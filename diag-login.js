// READ-ONLY diagnostic: reproduces the exact queries the login route runs.
// Prints only non-sensitive fields.
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

async function main() {
  const adapter = new PrismaMariaDb(parseDatabaseUrl(process.env.DATABASE_URL));
  const prisma = new PrismaClient({ adapter });

  const identifier = process.argv[2];
  if (!identifier) {
    console.log("usage: node diag-login.js <phone>");
    process.exit(1);
  }
  console.log("identifier:", identifier);

  // Exact match (same as login route step 1)
  const user = await prisma.user.findFirst({
    where: { phoneNumber: identifier },
    include: {
      role: true,
      language: true,
      userTimezone: { include: { timezone: true } },
      devices: true,
    },
  });

  if (!user) {
    console.log("NO EXACT MATCH — trying variants...");
    const variants = [identifier, "+250" + identifier.replace(/\D/g, "").replace(/^0?/, "")];
    const alt = await prisma.user.findFirst({
      where: { phoneNumber: { in: variants } },
      orderBy: { id: "asc" },
      include: {
        role: true,
        language: true,
        userTimezone: { include: { timezone: true } },
        devices: true,
      },
    });
    if (!alt) {
      console.log("NO USER FOUND AT ALL");
      return;
    }
    await dump(alt, prisma);
    return;
  }

  await dump(user, prisma);
}

async function dump(user, prisma) {
  console.log("=== USER FOUND ===");
  console.log("id:", user.id);
  console.log("phoneNumber:", user.phoneNumber);
  console.log("isActive:", user.isActive);
  console.log("role:", user.role ? JSON.stringify({ id: user.role.id, roleName: user.role.roleName }) : "NULL ❌ (login route would crash)");
  console.log("language:", user.language ? JSON.stringify({ id: user.language.id, languageCode: user.language.languageCode }) : "NULL ❌ (login route would crash)");
  console.log("userTimezone:", user.userTimezone ? "present" : "null (falls back to direct FK)");
  console.log("timezoneId:", user.timezoneId);
  console.log("password:", user.password ? `hash present (len ${user.password.length})` : "NULL ❌ (bcrypt would crash)");
  console.log("devices:", user.devices.length);
  console.log("email:", user.email);
  console.log("lastLogin:", user.lastLogin);
  console.log("=== counts used by dashboard route ===");
  try {
    console.log("users:", await prisma.user.count());
    console.log("userSubscriptions:", await prisma.userSubscription.count());
    console.log("tests:", await prisma.test.count());
    console.log("learningMaterials:", await prisma.learningMaterial.count());
    console.log("pdfFiles:", await prisma.pdfFile.count());
  } catch (e) {
    console.log("COUNT ERROR ❌:", e.message);
  }
}

main().then(() => process.exit(0)).catch((e) => {
  console.error("DIAG ERROR ❌:", e);
  process.exit(1);
});