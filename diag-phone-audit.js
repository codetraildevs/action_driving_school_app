// READ-ONLY audit: finds phone numbers that could collide under the login
// route's phoneVariants() matching. Two accounts whose numbers normalize to
// the same variants are the one way login can resolve to the WRONG account.
// Run: node diag-phone-audit.js
// Prints only ids, phone formats, and counts — no personal data.
const fs = require("fs");
const path = require("path");

// Load .env manually (dotenv is not a direct dep)
const envTxt = fs.readFileSync(path.join(__dirname, ".env"), "utf8");
for (const line of envTxt.split(/\r?\n/)) {
  const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)$/);
  if (!m) continue;
  let val = m[2].trim();
  if ((val.startsWith('"') && val.endsWith('"')) || (val.startsWith("'") && val.endsWith("'"))) {
    val = val.slice(1, -1);
  }
  process.env[m[1]] = val;
}

const { PrismaClient } = require("./lib/generated/prisma");
const { PrismaMariaDb } = require("@prisma/adapter-mariadb");

function parseDatabaseUrl(url) {
  const u = new URL(url);
  return {
    host: u.hostname,
    port: u.port ? parseInt(u.port, 10) : 3306,
    user: decodeURIComponent(u.username),
    password: decodeURIComponent(u.password),
    database: u.pathname ? decodeURIComponent(u.pathname.slice(1)) : undefined,
  };
}

// Mirror of phoneVariants() in app/api/auth/login/route.ts — keep in sync.
function phoneVariants(raw) {
  const digits = raw.replace(/\D/g, "");
  const set = new Set();
  if (raw) set.add(raw);
  if (digits) {
    if (digits.startsWith("250")) {
      set.add("+" + digits);
      set.add("0" + digits.slice(3));
    } else if (digits.startsWith("0")) {
      set.add("+" + "250" + digits.slice(1));
      set.add("250" + digits.slice(1));
    } else {
      set.add("+" + digits);
    }
  }
  return [...set];
}

async function main() {
  const adapter = new PrismaMariaDb(parseDatabaseUrl(process.env.DATABASE_URL));
  const prisma = new PrismaClient({ adapter });

  const users = await prisma.user.findMany({
    select: { id: true, phoneNumber: true, isActive: true },
  });
  console.log(`Total users: ${users.length}`);

  // 1. Exact-duplicate phone numbers (impossible if the UNIQUE index exists)
  const byPhone = new Map();
  for (const u of users) {
    if (!byPhone.has(u.phoneNumber)) byPhone.set(u.phoneNumber, []);
    byPhone.get(u.phoneNumber).push(u);
  }
  const dupes = [...byPhone.entries()].filter(([, rows]) => rows.length > 1);
  if (dupes.length) {
    console.log("\n❌ DUPLICATE phone_number rows (UNIQUE index missing?):");
    for (const [phone, rows] of dupes) {
      console.log(`  ${phone}: ids=${rows.map((r) => r.id).join(",")}`);
    }
  } else {
    console.log("✅ No duplicate phone_number rows.");
  }

  // 2. Variant collisions: different accounts whose numbers share a variant,
  //    so login typed as one format could resolve to the other account.
  //    Key by normalized 9-digit local part (e.g. 732657995).
  const normalizeCore = (phone) => {
    const d = phone.replace(/\D/g, "");
    if (d.startsWith("250")) return d.slice(3);
    if (d.startsWith("0")) return d.slice(1);
    return d;
  };
  const byCore = new Map();
  for (const u of users) {
    const core = normalizeCore(u.phoneNumber);
    if (!byCore.has(core)) byCore.set(core, []);
    byCore.get(core).push(u);
  }
  const collisions = [...byCore.entries()].filter(([, rows]) => rows.length > 1);
  if (collisions.length) {
    console.log("\n❌ VARIANT COLLISIONS (different formats map to the same number):");
    for (const [core, rows] of collisions) {
      console.log(
        `  core=${core}: ` +
          rows.map((r) => `id=${r.id} phone=${r.phone ?? r.phoneNumber}`).join(" | ")
      );
    }
    console.log(
      "\nThese accounts are ambiguous at login. Keep only the correct row " +
        "(or align the format) so each core number maps to exactly one account."
    );
  } else {
    console.log("✅ No variant collisions across accounts.");
  }

  // 3. Same-number cross-format rows (07x vs +250x as two different ids) is
  //    the same as #2, but double-check with the login variants directly.
  const variantOwner = new Map(); // variant -> [userId]
  for (const u of users) {
    for (const v of phoneVariants(u.phoneNumber)) {
      if (!variantOwner.has(v)) variantOwner.set(v, new Set());
      variantOwner.get(v).add(u.id);
    }
  }
  const multiOwner = [...variantOwner.entries()].filter(([, ids]) => ids.size > 1);
  if (multiOwner.length) {
    console.log("\n❌ SHARED VARIANTS (one format string maps to MULTIPLE accounts):");
    for (const [variant, ids] of multiOwner) {
      console.log(`  "${variant}" → ids=${[...ids].join(",")}`);
    }
  } else {
    console.log("✅ Every variant string maps to at most one account.");
  }

  await prisma.$disconnect();
}

main().then(
  () => process.exit(0),
  (e) => {
    console.error("AUDIT ERROR:", e);
    process.exit(1);
  }
);
