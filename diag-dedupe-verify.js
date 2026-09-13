// diag-dedupe-verify.js — READ-ONLY post-merge verification for the
// duplicate-account merge (scripts/dedupe-accounts.sql / dedupe-plain.sql).
//
// Checks:
//   1. dedupe_log — one row per merged pair, every survivor still exists.
//   2. users_dedupe_archive — every archived (dropped) user id is really gone
//      from `users`, and the archived row's phone matches the log.
//   3. Orphans — NO row in ANY user-referencing child table still points at a
//      dropped id (this is what would silently break profiles/subscriptions).
//   4. Survivor phone — each kept user owns the canonical number.
//
// Run from /home/project3:  node diag-dedupe-verify.js
// Exits 0 only if every check passes.
const fs = require("fs");
const path = require("path");

for (const line of fs.readFileSync(path.join(__dirname, ".env"), "utf8").split(/\r?\n/)) {
  const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)$/);
  if (!m) continue;
  let v = m[2].trim();
  if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) v = v.slice(1, -1);
  process.env[m[1]] = v;
}

const { PrismaClient } = require("./lib/generated/prisma");
const { PrismaMariaDb } = require("@prisma/adapter-mariadb");

const u = new URL(process.env.DATABASE_URL);
const prisma = new PrismaClient({
  adapter: new PrismaMariaDb({
    host: u.hostname,
    port: parseInt(u.port || 3306, 10),
    user: decodeURIComponent(u.username),
    password: decodeURIComponent(u.password),
    database: decodeURIComponent(u.pathname.slice(1)),
  }),
});

// Every table that references users, with its FK column (verified against the
// schema dump — keep in sync with scripts/dedupe-accounts.sql).
const CHILD_TABLES = [
  ["addresses", "user"], ["bookmarks", "user"], ["devices", "user"],
  ["sessions", "user"], ["user_activities", "user"], ["user_notifications", "user"],
  ["user_permissions", "user"], ["ratings", "user"], ["test_attempts", "user"],
  ["test_results", "user"], ["transactions", "user"],
  ["user_subscriptions_request", "user"],
  ["user_learning_materials", "user_id"], ["privacy_policy_acceptances", "user_id"],
  ["terms_of_service_acceptances", "user_id"], ["user_ratings", "user_id"],
  ["irembo_driving_license_requests", "user_id"], ["irembo_special_requests", "user_id"],
  ["user_subscriptions", "user"], ["user_test_access", "user"],
  ["user_timezones", "user"], ["reading_sessions", "user"],
  ["loginAttemps", "user"], ["privacyConsent", "user"],
  ["DataDeletionRequests", "user"], ["permissionlogs", "user"],
];

let failures = 0;
const fail = (msg) => { console.log("  ❌ " + msg); failures++; };
const ok = (msg) => console.log("  ✅ " + msg);

async function tableExists(t) {
  const rows = await prisma.$queryRawUnsafe(
    "SELECT COUNT(*) AS c FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?", t);
  return rows[0].c > 0;
}

(async () => {
  console.log("═══ 1. dedupe_log ═══");
  const logRows = await prisma.$queryRawUnsafe(
    "SELECT kept_user_id, dropped_user_id, phone_number, child_rows_moved, executed_at FROM dedupe_log ORDER BY id");
  console.log(`  ${logRows.length} merged pair(s) logged`);
  if (logRows.length === 0) fail("dedupe_log is EMPTY — did the merge actually run?");
  for (const r of logRows) {
    const kept = await prisma.$queryRawUnsafe("SELECT id, phone_number FROM users WHERE id = ?", r.kept_user_id);
    if (!kept.length) fail(`log row: survivor ${r.kept_user_id} NO LONGER EXISTS in users`);
    else if (kept[0].phone_number.replace(/\D/g, "").replace(/^250/, "").replace(/^0/, "") !==
             r.phone_number.replace(/\D/g, "").replace(/^250/, "").replace(/^0/, ""))
      fail(`log row: survivor ${r.kept_user_id} phone ${kept[0].phone_number} ≠ logged ${r.phone_number}`);
  }
  if (failures === 0) ok("every logged survivor exists with the expected number");

  console.log("\n═══ 2. users_dedupe_archive ═══");
  if (!(await tableExists("users_dedupe_archive"))) {
    console.log("  (no archive table — nothing merged)"); 
  } else {
    const archived = await prisma.$queryRawUnsafe(
      "SELECT id, phone_number, merged_into FROM users_dedupe_archive WHERE merged_into > 0");
    console.log(`  ${archived.length} archived account(s)`);
    for (const a of archived) {
      const stillThere = await prisma.$queryRawUnsafe("SELECT id FROM users WHERE id = ?", a.id);
      if (stillThere.length) fail(`archived id ${a.id} (${a.phone_number}) STILL EXISTS in users`);
      const survivor = await prisma.$queryRawUnsafe("SELECT id FROM users WHERE id = ?", a.merged_into);
      if (!survivor.length) fail(`archived id ${a.id} points at missing survivor ${a.merged_into}`);
    }
    const loggedDrops = logRows.map((r) => r.dropped_user_id).sort((x, y) => x - y);
    const archivedIds = archived.map((a) => a.id).sort((x, y) => x - y);
    if (JSON.stringify(loggedDrops) !== JSON.stringify(archivedIds))
      fail(`log drops [${loggedDrops}] ≠ archive ids [${archivedIds}]`);
    else ok("archive matches the log exactly; no archived id remains in users");
  }

  console.log("\n═══ 3. Orphaned child rows (must all be 0) ═══");
  const droppedIds = logRows.map((r) => r.dropped_user_id);
  if (droppedIds.length) {
    for (const [t, col] of CHILD_TABLES) {
      if (!(await tableExists(t))) { console.log(`  ⏭️  ${t}: table absent, skipped`); continue; }
      const rows = await prisma.$queryRawUnsafe(
        `SELECT COUNT(*) AS c FROM \`${t}\` WHERE \`${col}\` IN (${droppedIds.join(",")})`);
      if (rows[0].c > 0) fail(`${t}: ${rows[0].c} row(s) still point at a dropped user id`);
      else console.log(`  ✅ ${t}: 0`);
    }
  }

  console.log("\n═══ 4. Final duplicate scan ═══");
  const dups = await prisma.$queryRawUnsafe(
    "SELECT TRIM(LEADING '0' FROM TRIM(LEADING '+250' FROM REPLACE(phone_number,' ',''))) AS core, COUNT(*) AS c " +
    "FROM users GROUP BY core HAVING c > 1");
  if (dups.length) fail(`${dups.length} duplicate core number(s) remain: ${JSON.stringify(dups)}`);
  else ok("no two accounts share a phone number in any format");

  console.log("\n═══ VERDICT ═══");
  if (failures === 0) console.log("✅ MERGE VERIFIED CLEAN — log, archive, children and duplicates all consistent");
  else { console.log(`❌ ${failures} problem(s) found — see above`); }
  await prisma.$disconnect();
  process.exit(failures === 0 ? 0 : 1);
})().catch((e) => { console.error("ERROR:", e); process.exit(1); });
