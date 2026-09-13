// diag-pick-test-accounts.js — Suggest real accounts for identity testing.
// Lists recently-active users with at least one registered device (so the
// device-id login path in diag-two-accounts.js can work), plus the admin
// account as a zero-setup fallback.
//
// Run from /home/project3:  node diag-pick-test-accounts.js [count]
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

(async () => {
  const count = parseInt(process.argv[2] || "6", 10);

  const admin = await prisma.user.findFirst({
    where: { role: { roleName: { in: ["admin", "super_admin"] } } },
    select: { id: true, phoneNumber: true, isActive: true, role: { select: { roleName: true } } },
    orderBy: { id: "asc" },
  });
  if (admin) {
    console.log("Admin (phone-only login, no device id needed):");
    console.log(`  id=${admin.id} phone=${admin.phoneNumber} role=${admin.role.role_name}`);
  }

  const users = await prisma.user.findMany({
    where: { isActive: true, devices: { some: {} } },
    select: {
      id: true, phoneNumber: true, lastLogin: true,
      devices: { select: { physicalAddress: true } },
    },
    orderBy: { lastLogin: "desc" },
    take: count,
  });

  console.log("\nRegular users (password = a registered device id, shown truncated):");
  for (const usr of users) {
    const dev = usr.devices[0].physicalAddress;
    console.log(
      `  id=${usr.id} phone=${usr.phoneNumber} lastLogin=${usr.lastLogin ? usr.lastLogin.toISOString().slice(0, 16) : "never"} ` +
      `deviceId=${dev.slice(0, 10)}…`
    );
  }

  console.log("\nThen test (server-side, app running):");
  console.log("  node diag-two-accounts.js <phone1> <phone2>");
  console.log("or with the full device id as the password argument via test-identity.sh.");
  await prisma.$disconnect();
})().catch((e) => { console.error("ERROR:", e); process.exit(1); });
