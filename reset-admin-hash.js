// One-off helper: generates a strong admin password + bcrypt hash, applies the
// hash to the LOCAL DB copy (id=2), and prints the prod SQL. Run once:
// node reset-admin-hash.js
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

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

const bcrypt = require("bcryptjs");
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

// Strong but typeable: 4 groups of 4 alphanumeric chars + 2 symbols.
function generatePassword() {
  const alphabet = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const group = () =>
    Array.from(crypto.randomBytes(4))
      .map((b) => alphabet[b % alphabet.length])
      .join("");
  const symbols = ["!", "@", "#", "%", "&", "*"];
  const sym1 = symbols[crypto.randomBytes(1)[0] % symbols.length];
  const sym2 = symbols[crypto.randomBytes(1)[0] % symbols.length];
  return `${group()}-${sym1}${group()}-${group()}${sym2}`;
}

async function main() {
  const password = generatePassword();
  const hash = await bcrypt.hash(password, 10);

  const adapter = new PrismaMariaDb(parseDatabaseUrl(process.env.DATABASE_URL));
  const prisma = new PrismaClient({ adapter });

  const admin = await prisma.user.findUnique({
    where: { id: 2 },
    select: { id: true, phoneNumber: true, role: { select: { roleName: true } } },
  });
  if (!admin) {
    console.error("Admin user id=2 not found in local DB copy — aborting.");
    process.exit(1);
  }

  await prisma.user.update({ where: { id: 2 }, data: { password: hash } });

  // Verify the row we just wrote.
  const updated = await prisma.user.findUnique({
    where: { id: 2 },
    select: { password: true },
  });
  const ok = await bcrypt.compare(password, updated.password);
  console.log(`LOCAL DB UPDATED: id=2 phone=${admin.phoneNumber} role=${admin.role?.roleName}`);
  console.log(`VERIFY (bcrypt.compare): ${ok ? "PASS ✅" : "FAIL ❌"}`);

  if (ok) {
    console.log("\n──────────────────────────────────────────────");
    console.log("NEW ADMIN PASSWORD (shown once, store it now):");
    console.log("\n  " + password + "\n");
    console.log("Run this SQL on the PRODUCTION database:");
    console.log("──────────────────────────────────────────────");
    console.log(
      `UPDATE users SET password = '${hash}' WHERE id = 2 AND phone_number = '${admin.phoneNumber}';`
    );
    console.log("──────────────────────────────────────────────");
    console.log(
      "\nThen verify on prod: node diag-password-check.js 0732657995 '<password>'"
    );
  }

  await prisma.$disconnect();
}

main().then(
  () => process.exit(0),
  (e) => {
    console.error("RESET ERROR:", e);
    process.exit(1);
  }
);
