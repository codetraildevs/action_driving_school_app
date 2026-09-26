// diag-two-accounts.js — End-to-end identity test for REGULAR (device-bound)
// accounts. Looks up each account's registered device(s) in the DB, uses the
// device id as the password (that is how regular users log in), then verifies:
//   login.userId == token.userId == /profile.userId == refresh.userId
//
// Run from /home/project3 (so ./lib/generated/prisma resolves):
//   node diag-two-accounts.js                    # tests the default two phones
//   node diag-two-accounts.js 0732657993 0782457226 [more...]
//
// READ-ONLY on the DB; the HTTP logins are real (they create a Session row
// and update lastLogin, same as the mobile app would).
const fs = require("fs");
const path = require("path");

// Load .env manually (dotenv is not a direct dep)
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

// Login against the local app process — avoids TLS/public-URL dependencies.
const BASE = process.env.TEST_BASE_URL || "http://127.0.0.1:3000";
const PHONES = process.argv.slice(2);
if (PHONES.length === 0) {
  console.log("usage: node diag-two-accounts.js <phone1> <phone2> [more...]");
  console.log("(tip: node diag-pick-test-accounts.js lists recently-active candidates)");
  process.exit(1);
}

// Mirror of phoneVariants() in app/api/auth/login/route.ts — keep in sync.
function phoneVariants(raw) {
  const digits = raw.replace(/\D/g, "");
  const set = new Set();
  if (raw) set.add(raw);
  if (digits) {
    if (digits.startsWith("250")) { set.add("+" + digits); set.add("0" + digits.slice(3)); }
    else if (digits.startsWith("0")) { set.add("+" + "250" + digits.slice(1)); set.add("250" + digits.slice(1)); }
    else { set.add("+" + digits); }
  }
  return [...set];
}

function decodeJwtPayload(token) {
  const b = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
  return JSON.parse(Buffer.from(b, "base64").toString("utf8"));
}

async function jsonPost(pathName, body) {
  const r = await fetch(BASE + pathName, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  return { status: r.status, json: await r.json().catch(() => ({})) };
}

async function testPhone(phone) {
  console.log(`\n════ ${phone} ════`);
  const users = await prisma.user.findMany({
    where: { phoneNumber: { in: phoneVariants(phone) } },
    select: {
      id: true, phoneNumber: true, isActive: true,
      devices: { select: { physicalAddress: true } },
    },
    orderBy: { id: "asc" },
  });

  if (users.length === 0) {
    console.log("  ❌ NOT FOUND in DB under any format — cannot test this number");
    return false;
  }
  if (users.length > 1) {
    console.log(`  ⚠️  DUPLICATE PAIR in DB: ids=${users.map((x) => x.id).join(",")} phones=${users.map((x) => x.phoneNumber).join(",")}`);
    console.log("      (each row is tested separately below — the credential checks must pick the right one)");
  }

  let allOk = true;
  for (const user of users) {
    const label = `userId=${user.id} phone=${user.phoneNumber}`;
    if (!user.isActive) {
      console.log(`  ⏭️  ${label}: inactive — skipped`);
      continue;
    }
    if (!user.devices.length) {
      console.log(`  ⏭️  ${label}: no registered device → cannot derive the device-id password`);
      allOk = false;
      continue;
    }

    // Try each registered device until one logs in (password == device id).
    let login = null;
    for (const d of user.devices) {
      const res = await jsonPost("/api/auth/login", {
        identifier: phone,
        password: d.physicalAddress,
        deviceId: d.physicalAddress,
        clientType: "android_app",
      });
      if (res.json.success) { login = res.json; break; }
      console.log(`  ↩︎  device ${d.physicalAddress.slice(0, 8)}… rejected: ${res.json.message || res.json.error || res.status}`);
    }
    if (!login) {
      console.log(`  ❌ ${label}: login FAILED with every registered device`);
      allOk = false;
      continue;
    }

    const loginUserId = login.user.id;
    const loginUserPhone = login.user.phoneNumber;
    const payload = decodeJwtPayload(login.accessToken);
    const tokenUserId = payload.userId;

    const pr = await fetch(BASE + "/api/users/profile", {
      headers: { Authorization: `Bearer ${login.accessToken}` },
    });
    const pj = await pr.json().catch(() => ({}));
    const profileUser = pj.data;
    const profileUserId = profileUser ? profileUser.id : null;
    const profileUserPhone = profileUser ? profileUser.phoneNumber : null;

    const chainOk =
      loginUserId === tokenUserId &&
      profileUserId === loginUserId &&
      profileUserPhone === loginUserPhone;

    console.log(`  login returned : userId=${loginUserId} phone=${loginUserPhone}`);
    console.log(`  token claims   : userId=${tokenUserId}`);
    console.log(`  profile said   : userId=${profileUserId} phone=${profileUserPhone}`);
    console.log(chainOk
      ? `  ✅ ${label}: IDENTITY CHAIN OK`
      : `  ❌ ${label}: IDENTITY MISMATCH — cross-user leak!`);

    // Refresh must keep the same identity.
    const rr = await jsonPost("/api/auth/refresh", { refreshToken: login.refreshToken });
    let refreshOk = false;
    if (rr.json.success) {
      refreshOk = decodeJwtPayload(rr.json.accessToken).userId === loginUserId;
      console.log(refreshOk
        ? `  ✅ refresh keeps userId=${loginUserId}`
        : `  ❌ refresh switched user!`);
    } else {
      console.log(`  ⚠️  refresh failed: ${rr.json.message || rr.json.error || rr.status}`);
    }
    allOk = allOk && chainOk && refreshOk;
  }
  return allOk;
}

(async () => {
  let ok = true;
  for (const phone of PHONES) {
    ok = (await testPhone(phone)) && ok;
  }
  console.log("\n════ SUMMARY ════");
  console.log(ok ? "✅ ALL TESTED ACCOUNTS: identity chain OK" : "❌ FAILURES ABOVE — investigate before proceeding");
  await prisma.$disconnect();
  process.exit(ok ? 0 : 1);
})().catch((e) => { console.error("ERROR:", e); process.exit(1); });
