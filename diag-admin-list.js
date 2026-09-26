// READ-ONLY diagnostic: lists console (admin/super_admin) accounts.
// Prints ids, phone format, email, isActive, lastLogin — nothing sensitive.
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

async function main() {
  const adapter = new PrismaMariaDb(parseDatabaseUrl(process.env.DATABASE_URL));
  const prisma = new PrismaClient({ adapter });

  const admins = await prisma.user.findMany({
    where: { role: { roleName: { in: ["admin", "super_admin", "Admin", "Super Admin", "Super_Admin"] } } },
    select: {
      id: true,
      phoneNumber: true,
      email: true,
      isActive: true,
      lastLogin: true,
      role: { select: { roleName: true } },
    },
    orderBy: { id: "asc" },
  });

  console.log(`Console accounts found: ${admins.length}`);
  for (const a of admins) {
    console.log(
      `id=${a.id} phone=${a.phoneNumber} email=${a.email ?? "-"} role=${a.role?.roleName} isActive=${a.isActive} lastLogin=${a.lastLogin ?? "never"}`
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
