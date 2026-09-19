import { dirname } from "path";
import { fileURLToPath } from "url";
import { FlatCompat } from "@eslint/eslintrc";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const compat = new FlatCompat({
  baseDirectory: __dirname,
});

// ─────────────────────────────────────────────────────────────────────────────
// Rule: no-unguarded-nullable-relation
//
// Root cause of the 2026-09-13 admin-console crash
// (Uncaught TypeError: Cannot read properties of null (reading 'nativeName'),
// see docs/incident-report-2026-09.md): code read
// `req.user.Pendinglanguage.nativeName` while `Pendinglanguage` is a NULLABLE
// relation in prisma/schema.prisma (`Pendinglanguage Language?`). Hand-written
// interfaces declared it non-null, which hid the bug from strict TypeScript.
//
// This rule flags any direct property read off one of those relations
// (`x.Relation.prop`). Optional chaining (`x.Relation?.prop`) is allowed: in
// the AST the guarded outer MemberExpression has `optional: true`, the
// unguarded one does not.
//
// Maintenance: whenever prisma/schema.prisma gains a nullable relation (`?`)
// that the UI reads, add it to NULLABLE_RELATIONS. (Alias evasion —
// `const rel = user.Pendinglanguage; rel.nativeName` — is not detectable by an
// AST rule; the truthful `Language | null` interface types make `tsc` catch
// that path instead. Both defenses require the interfaces to stay truthful.)
// ─────────────────────────────────────────────────────────────────────────────
const NULLABLE_RELATIONS = [
  "Pendinglanguage",         // User.Pendinglanguage        (Language?)
  "userTestAccess",          // User.userTestAccess         (UserTestAccess?)
  "userSubscriptionsRequest",// User.userSubscriptionsRequest (nullable)
];

const noUnguardedNullableRelation = {
  meta: {
    type: "problem",
    docs: {
      description:
        "Forbid unguarded property reads on nullable Prisma relations (use ?. or a null check)",
    },
    schema: [],
    messages: {
      unguarded:
        "'{{relation}}' is a nullable Prisma relation (declared '?' in schema.prisma). Reading '{{prop}}' directly crashes at runtime when it is null (this caused the 2026-09 admin console outage). Use '{{fix}}' or null-check first.",
    },
  },

  create(context) {
    // True when `name` appears anywhere inside `expr` (tests only need to
    // mention the relation, e.g. `user.userTestAccess ? ...`,
    // `user?.userTestAccess && ...`, `Boolean(user.userTestAccess)`).
    // Heuristic: does not track aliasing (`const a = user.Rel; a ? ... : ...`)
    // — for that case the truthful `T | null` interface types make tsc catch it.
    function mentionsRelation(expr, name) {
      if (!expr || typeof expr.type !== "string") return false;
      switch (expr.type) {
        case "Identifier":
          return expr.name === name;
        case "MemberExpression":
          return (
            (expr.property && expr.property.type === "Identifier" && expr.property.name === name) ||
            mentionsRelation(expr.object, name)
          );
        case "ChainExpression":
          return mentionsRelation(expr.expression, name);
        case "BinaryExpression":
        case "LogicalExpression":
          return mentionsRelation(expr.left, name) || mentionsRelation(expr.right, name);
        case "UnaryExpression":
        case "AwaitExpression":
          return mentionsRelation(expr.argument, name);
        case "CallExpression":
          return (
            mentionsRelation(expr.callee, name) ||
            (expr.arguments || []).some((a) => mentionsRelation(a, name))
          );
        default:
          return false;
      }
    }

    // A direct read is acceptable when an enclosing conditional/short-circuit
    // already null-checks the relation:
    //   {user.userTestAccess ? (...user.userTestAccess.maxTest...) : ...}
    //   {user?.userTestAccess && (...user.userTestAccess.status...)}
    function guardedByAncestor(node, relationName) {
      for (let p = node.parent; p; p = p.parent) {
        if (p.type === "ConditionalExpression" && mentionsRelation(p.test, relationName)) {
          return true;
        }
        if (p.type === "LogicalExpression" && mentionsRelation(p.left, relationName)) {
          return true;
        }
      }
      return false;
    }

    return {
      MemberExpression(node) {
        const obj = node.object;
        if (
          // The read itself must be direct: `a.Rel?.prop` (optional: true)
          // is already safe, `a.Rel.prop` (optional: false) is the bug.
          node.optional ||
          obj.type !== "MemberExpression" ||
          obj.computed ||
          obj.property.type !== "Identifier" ||
          !NULLABLE_RELATIONS.includes(obj.property.name) ||
          node.property.type !== "Identifier"
        ) {
          return;
        }
        const relation = obj.property.name;
        if (guardedByAncestor(node, relation)) {
          return;
        }
        const prop = node.property.name;
        context.report({
          node,
          messageId: "unguarded",
          data: {
            relation,
            prop,
            fix: `${relation}?.${prop}`,
          },
        });
      },
    };
  },
};

const eslintConfig = [
  {
    ignores: [
      "node_modules/**",
      ".next/**",
      "out/**",
      "build/**",
      "next-env.d.ts",
      "deploy.js",
      "healthcheck.js",
      "prebuild.js",
    ],
  },
  ...compat.extends("next/core-web-vitals", "next/typescript"),
  {
    plugins: {
      local: { rules: { "no-unguarded-nullable-relation": noUnguardedNullableRelation } },
    },
    rules: {
      "local/no-unguarded-nullable-relation": "error",
    },
  },
];

export default eslintConfig;
