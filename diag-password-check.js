// READ-ONLY diagnostic: compares a candidate password with the stored bcrypt
// hash for the account(s) matching a phone number. Never prints the hash.
// Run: node diag-password-check.js <phone> <password>
const fs = require("fs");
const path = require("path");

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
  const identifier = process.argv[2];
  const candidate = process.argv[3];
  if (!identifier || !candidate) {
    console.log("usage: node diag-password-check.js <phone> <password>");
    process.exit(1);
  }

  const adapter = new PrismaMariaDb(parseDatabaseUrl(process.env.DATABASE_URL));
  const prisma = new PrismaClient({ adapter });

  const rows = await prisma.user.findMany({
    where: { phoneNumber: { in: phoneVariants(identifier) } },
    select: { id: true, phoneNumber: true, isActive: true, password: true, role: { select: { roleName: true } }, lastLogin: true },
    orderBy: { id: "asc" },
  });

  if (rows.length === 0) {
    console.log("NO ACCOUNTS MATCH that phone number (in any format).");
    return;
  }

  for (const u of rows) {
    const hashLen = u.password ? u.password.length : 0;
    // Guard: bcrypt hashes are 60 chars starting with $2a$/$2b$/$2y$
    const looksLikeHash = /^\$2[aby]\$/.test(u.password || "");
    const match = looksLikeHash ? await bcrypt.compare(candidate, u.password) : false;
    console.log(
      `id=${u.id} phone=${u.phoneNumber} role=${u.role?.roleName} isActive=${u.isActive} ` +
      `hashLen=${hashLen} looksLikeBcrypt=${looksLikeHash} passwordMatch=${match} lastLogin=${u.lastLogin}`
    );
  }

  await prisma.$disconnect();
}

main().then(
  () => process.exit(0),
  (e) => {
    console.error("DIAG ERROR:", e);
    process.exit(1);
  }
);
