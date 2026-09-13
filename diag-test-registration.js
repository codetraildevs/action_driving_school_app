// diag-test-registration.js — Prove registration normalizes phone numbers so
// the same number can never exist in two formats again.
//
// What it does (against the RUNNING app — run AFTER ./deploy-vps.sh so the
// new build with normalizeRwandaPhone is live):
//   1. Registers a unique number as 0799XXXXXX        → expect 201
//   2. Registers the SAME number as +250799XXXXXX     → expect 409
//   3. Registers the SAME number as 250799XXXXXX      → expect 409
//   4. Registers the SAME number as "+250 799 …"      → expect 409
//   5. Registers an invalid number (0799123)          → expect 400
//   6. Verifies the DB holds EXACTLY ONE row for the core number
//   7. Deletes the test account (self-cleaning, including child rows)
//
// Run from /home/project3:  node diag-test-registration.js
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

const BASE = process.env.TEST_BASE_URL || "http://127.0.0.1:3000";

// Unique but valid Rwandan test number: 0799 + 6 digits (0799123456 shape).
const core = "0799" + String(Date.now() % 1000000).padStart(6, "0");
const FORMATS = [core, "+250" + core.slice(1), "250" + core.slice(1), "+250 " + core.slice(1, 4) + " " + core.slice(4, 7) + " " + core.slice(7)];

let failures = 0;
const fail = (msg) => { console.log("  ❌ " + msg); failures++; };
const pass = (msg) => console.log("  ✅ " + msg);

async function register(phone) {
  const r = await fetch(BASE + "/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      firstName: "Dedupe", lastName: "Selftest",
      phoneNumber: phone,
      password: "selftest-pw-123",
      language: "en",
      timezone: "Africa/Kigali",
    }),
  });
  return { status: r.status, json: await r.json().catch(() => ({})) };
}

async function cleanup() {
  // Remove every trace of the test number in ANY stored format.
  const rows = await prisma.user.findMany({
    where: { phoneNumber: { contains: core.slice(1) } }, // matches 07…/+250…/250…
    select: { id: true },
  });
  for (const row of rows) {
    await prisma.userTestAccess.deleteMany({ where: { userId: row.id } });
    await prisma.userTimezone.deleteMany({ where: { userId: row.id } });
    await prisma.device.deleteMany({ where: { userId: row.id } });
    await prisma.address.deleteMany({ where: { userId: row.id } });
    await prisma.user.delete({ where: { id: row.id } }).catch(() => {});
  }
  return rows.length;
}

(async () => {
  console.log(`Test number (core): ${core}\n`);

  console.log("1. Register local format " + FORMATS[0]);
  const r1 = await register(FORMATS[0]);
  r1.status === 201 && r1.json.success
    ? pass("201 created")
    : fail(`expected 201, got ${r1.status}: ${JSON.stringify(r1.json).slice(0, 200)}`);

  for (const [i, fmt] of FORMATS.slice(1).entries()) {
    console.log(`${i + 2}. Register SAME number as "${fmt}"`);
    const r = await register(fmt);
    r.status === 409
      ? pass("409 rejected — duplicate detected across formats")
      : fail(`expected 409, got ${r.status}: ${JSON.stringify(r.json).slice(0, 200)}`);
  }

  console.log("5. Register invalid number 0799123");
  const rInv = await register("0799123");
  rInv.status === 400
    ? pass("400 rejected — invalid Rwandan number")
    : fail(`expected 400, got ${rInv.status}: ${JSON.stringify(rInv.json).slice(0, 200)} ` +
           `(402/201 means the app is still the OLD build — run ./deploy-vps.sh first)`);

  console.log("6. DB holds exactly ONE row for the core number");
  const dbRows = await prisma.user.findMany({
    where: { phoneNumber: { contains: core.slice(1) } },
    select: { id: true, phoneNumber: true },
  });
  dbRows.length === 1
    ? pass(`exactly 1 row: id=${dbRows[0].id} phone=${dbRows[0].phoneNumber}`)
    : fail(`found ${dbRows.length} rows: ${JSON.stringify(dbRows)}`);

  console.log("7. Cleaning up test account…");
  const n = await cleanup();
  console.log(`  🧹 removed ${n} test user row(s)`);

  console.log("\n═══ VERDICT ═══");
  if (failures === 0) console.log("✅ REGISTRATION NORMALIZATION VERIFIED — same number cannot register twice in any format");
  else console.log(`❌ ${failures} check(s) failed — see above`);
  await prisma.$disconnect();
  process.exit(failures === 0 ? 0 : 1);
})().catch((e) => { console.error("ERROR:", e); process.exit(1); });
