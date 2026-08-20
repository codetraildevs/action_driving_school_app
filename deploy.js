/**
 * One-click deploy for cPanel hosts WITHOUT shell/terminal access.
 *
 * Run from: cPanel → Setup Node.js App → "Run JS script" → enter `deploy.js` → Run
 *          or from a cron job:
 *            /home/sxlvhdzo/nodevenv/project1/22/bin/node /home/sxlvhdzo/project1/deploy.js
 *
 * This performs the three commands you would normally type in a terminal:
 *   1. npm install --legacy-peer-deps
 *   2. npx prisma generate
 *   3. npm run build
 * then restarts the app (touch tmp/restart.txt) so Passenger serves the new build.
 *
 * It resolves npm/npx from the same folder as the app's node binary, so it works
 * even when the PATH does not include the virtual environment.
 *
 * NOTE: must be launched with the nodevenv node binary (see cron line above) —
 * if launched with a different node, npm/npx cannot be resolved.
 */
const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

// Always operate from this script's folder (= the application root).
process.chdir(__dirname);

// --- Overlap lock: skip if a previous deploy is still running --------------
const LOCK = path.join(__dirname, '.deploy.lock');
function lockIsStale() {
  try {
    const pid = parseInt(fs.readFileSync(LOCK, 'utf8'), 10);
    if (!Number.isInteger(pid)) return true; // corrupted -> stale
    process.kill(pid, 0); // throws if the process is dead
    return false; // process alive -> not stale
  } catch (_) {
    return true; // dead pid or missing file -> stale
  }
}
if (fs.existsSync(LOCK) && !lockIsStale()) {
  console.log('Another deploy is still running — skipping this run.');
  process.exit(0);
}
fs.writeFileSync(LOCK, String(process.pid));
const unlock = () => {
  try { fs.unlinkSync(LOCK); } catch (_) { /* already gone */ }
};
process.on('exit', unlock);

// On cPanel the app's node lives in e.g. /home/sxlvhdzo/nodevenv/project1/22/bin,
// and npm/npx live right next to it.
const nodeBin = path.dirname(process.execPath);
const npm = path.join(nodeBin, 'npm');
const npx = path.join(nodeBin, 'npx');

function run(cmd, label) {
  console.log('\n========================================');
  console.log('  ' + label);
  console.log('========================================');
  execSync(cmd, { stdio: 'inherit', shell: true });
}

// --- Kill stray Node daemons -----------------------------------------------
// A `next dev` / `next start` started from the panel's script dropdown keeps
// running (the UI warns it cannot be stopped) and eats the account's
// process/thread quota — the cause of spawn EAGAIN and Prisma "timer has gone
// away" panics on this host. Kill every node/npm process except this script
// and the Passenger-served app (cmdline contains server.js).
function killStrayNodeProcesses() {
  try {
    // Kill ALL node/npm processes except this script and the Passenger app
    const out = execSync("ps -eo pid,ppid,cmd", { encoding: 'utf8', shell: true });
    const me = process.pid;
    const ppid = process.ppid;
    const killed = [];
    for (const line of out.trim().split('\n')) {
      const parts = line.trim().match(/^(\d+)\s+(\d+)\s+(.*)$/);
      if (!parts) continue;
      const pid = parseInt(parts[1], 10);
      const parentPid = parseInt(parts[2], 10);
      const cmd = parts[3];
      if (pid === me || parentPid === me) continue; // skip self and children
      if (cmd.includes('server.js')) continue; // keep Passenger's app
      if (cmd.includes('deploy.js')) continue; // keep this script
      if (/grep|ps\s/.test(cmd)) continue; // skip system tools
      if (/sshd|bash|sh\s/.test(cmd)) continue; // keep shell sessions
      try { process.kill(pid, 'SIGKILL'); killed.push(pid); } catch (_) { /* already gone */ }
    }
    if (killed.length) console.log('Killed stray processes: ' + killed.join(', '));
    else console.log('No stray processes found.');
    // Also try to free memory
    try { execSync('sync', { shell: true }); } catch (_) {}
  } catch (e) {
    console.log('Stray-process check skipped: ' + e.message);
  }
}

// cPanel's File Manager extractor can silently drop files from large archives
// (that is what left the server without components/ui/*). Instead of relying on
// it, if a fix archive (components-fix.tar.gz / .zip) is sitting in the app
// root, extract it here with the server's own tar/unzip — guaranteed to work.
// The archive is renamed to *.done after a successful extraction so it is not
// re-extracted on every cron run.
function extractFixArchives() {
  const candidates = [
    { file: 'components-fix.tar.gz', extract: (f) => `tar -xzf "${f}" -C .`, rename: true },
    { file: 'components-fix.zip', extract: (f) => `unzip -o "${f}" -d .`, rename: true },
    // Restores the server's public/uploads/ folder (files/learning-materials/
    // thumbnails) that a previous deploy wiped. Extracted with the server's own
    // unzip (File Manager's extractor silently drops files from large archives),
    // so every missing upload comes back. The gazette PDFs uploaded Jan 2026 are
    // NOT in this archive — they must be re-uploaded via the admin console.
    { file: 'uploads-restore.zip', extract: (f) => `unzip -o "${f}" -d .`, rename: true },
    // The 3 gazette PDFs restored from the owner's local copies, named exactly
    // as the DB expects (1768631656325-5wd1wt0xhu3.pdf etc.) so downloads work
    // with zero database changes. Restored from the machine; sizes differ from
    // the originals (older versions of the same documents).
    { file: 'gazette-pdfs.zip', extract: (f) => `unzip -o "${f}" -d .`, rename: true },
  ];
  // Fallback: if components/ui is still missing, pull just ./components out of
  // the full deploy archive (in case File Manager's extractor dropped it).
  const cardTsx = path.join(__dirname, 'components', 'ui', 'card.tsx');
  if (!fs.existsSync(cardTsx)) {
    candidates.push({
      file: 'backend-deploy.tar.gz',
      extract: (f) => `tar -xzf "${f}" -C . ./components`,
      rename: false,
    });
  }
  let extracted = false;
  for (const c of candidates) {
    const f = path.join(__dirname, c.file);
    if (fs.existsSync(f)) {
      console.log('\nFound ' + c.file + ' — extracting it now...');
      execSync(c.extract(f), { stdio: 'inherit', shell: true });
      if (c.rename) fs.renameSync(f, f + '.done');
      extracted = true;
      console.log('Extracted ' + c.file + (c.rename ? ' ✓ (renamed to ' + c.file + '.done)' : ' ✓'));
    }
  }
  if (extracted) {
    const ui = path.join(__dirname, 'components', 'ui', 'card.tsx');
    console.log(fs.existsSync(ui)
      ? 'Sanity check: components/ui/card.tsx is now present ✓'
      : '⚠ WARNING: components/ui/card.tsx still missing after extraction!');
  } else {
    const ui = path.join(__dirname, 'components', 'ui', 'card.tsx');
    if (!fs.existsSync(ui)) {
      console.log('\n⚠ components/ui/card.tsx is MISSING on this server and no fix archive');
      console.log('  (components-fix.tar.gz / components-fix.zip) was found in: ' + __dirname);
      console.log('  → Upload components-fix.tar.gz to ' + __dirname + ' and re-run.');
    }
  }
  return extracted;
}

// Parse a mysql:// URL into the config object expected by the mariadb driver.
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

// --- Missing uploads check --------------------------------------------------
// Every deploy should end with all DB-referenced uploads present on disk.
// If a deploy ever wipes public/uploads (or a file was never uploaded to the
// server), downloads break with a 404 "File not found on server" while the
// database still lists the file. This check reports exactly which stored
// paths are missing so the problem can't go unnoticed again.
// Purely informational: it never fails the deploy.
function checkMissingUploads() {
  return new Promise((resolve) => {
    try {
      const envFile = path.join(__dirname, '.env');
      const env = fs.readFileSync(envFile, 'utf8');
      const m = env.match(/^DATABASE_URL\s*=\s*"?([^"\n]+)"?/m);
      if (!m) { resolve(); return; }
      const { PrismaClient } = require(path.join(__dirname, 'lib', 'generated', 'prisma'));
      const { PrismaMariaDb } = require(path.join(__dirname, 'node_modules', '@prisma', 'adapter-mariadb'));
      const adapter = new PrismaMariaDb(parseDatabaseUrl(m[1]));
      const p = new PrismaClient({ adapter });
      p.file.findMany({ select: { id: true, name: true, filePath: true } })
        .then((files) => {
          const missing = (files || []).filter((f) => {
            if (!f.filePath) return false;
            const diskPath = path.join(process.cwd(), 'public', f.filePath);
            return !fs.existsSync(diskPath);
          });
          if (missing.length) {
            console.log('\n⚠ MISSING UPLOAD FILES (' + missing.length + '):');
            for (const f of missing) {
              console.log('  - file#' + f.id + ' "' + f.name + '" → ' + f.filePath + ' NOT on disk');
            }
            console.log('  Downloads for these will fail with 404. Restore public/' + ' or re-upload via the admin console.');
          } else {
            console.log('UPLOADS CHECK: all ' + (files || []).length + ' DB-referenced files present on disk ✓');
          }
        })
        .catch((e) => { console.log('UPLOADS CHECK: could not run — ' + String((e && e.message) || e).split('\n')[0]); })
        .finally(() => p.$disconnect().catch(() => {}).finally(resolve));
    } catch (e) {
      console.log('UPLOADS CHECK: could not run — ' + String((e && e.message) || e).split('\n')[0]);
      resolve();
    }
  });
}

// --- DB connectivity check --------------------------------------------------
// Mirrors the app's driver-adapter setup so deploy.log shows whether the
// database is reachable. Never fails the deploy — purely informational.
function dbCheck() {
  try {
    const envFile = path.join(__dirname, '.env');
    const env = fs.readFileSync(envFile, 'utf8');
    const m = env.match(/^DATABASE_URL\s*=\s*"?([^"\n]+)"?/m);
    if (!m) {
      console.log('DB CHECK: could not run — DATABASE_URL not found in .env');
      return Promise.resolve();
    }
    const url = m[1];
    process.env.DATABASE_URL = url;
    console.log('DB CHECK — host: ' + url.replace(/:\/\/[^@]+@/, '://***@'));
    const { PrismaClient } = require(path.join(__dirname, 'lib', 'generated', 'prisma'));
    const { PrismaMariaDb } = require(path.join(__dirname, 'node_modules', '@prisma', 'adapter-mariadb'));
    const adapter = new PrismaMariaDb(parseDatabaseUrl(url));
    const p = new PrismaClient({ adapter });
    return p.$queryRaw`SELECT 1 as ok`
      .then((r) => { console.log('DB CHECK: OK — ' + JSON.stringify(r)); })
      .catch((e) => { console.log('DB CHECK: FAILED — ' + String((e && e.message) || e).split('\n')[0]); })
      .finally(() => p.$disconnect().catch(() => {}));
  } catch (e) {
    console.log('DB CHECK: could not run — ' + (e && e.message ? e.message : e));
    return Promise.resolve();
  }
}

try {
  killStrayNodeProcesses();
  extractFixArchives();
  run(`"${npm}" install --legacy-peer-deps`, 'Step 1/3 — npm install --legacy-peer-deps (this can take a few minutes)');

  // --- Fix root-owned Prisma generated files -------------------------------
  // On cPanel, if `prisma generate` was ever run from the panel's script
  // dropdown (runs as root), the output files become unreadable by the cPanel
  // user. chmod alone can't fix this if the *directory* is root-owned.
  // Strategy: try to delete the whole generated dir, then let prisma recreate
  // it with the correct ownership.
  const prismaGenDir = path.join(__dirname, 'lib', 'generated', 'prisma');
  const prismaGenParent = path.join(__dirname, 'lib', 'generated');
  console.log('\n--- Fixing Prisma generated file permissions ---');
  try {
    // Try deleting the generated prisma directory so prisma generate recreates it.
    execSync(`rm -rf "${prismaGenDir}"`, { stdio: 'inherit', shell: true });
    console.log('Deleted lib/generated/prisma/ — prisma generate will recreate it ✓');
  } catch (e1) {
    console.log('rm -rf failed: ' + (e1.message || e1));
    // Fallback: try chmod + chown
    try {
      execSync(`chmod -R 777 "${prismaGenDir}"`, { stdio: 'inherit', shell: true });
      console.log('chmod -R 777 on lib/generated/prisma/ ✓');
    } catch (e2) {
      console.log('chmod failed: ' + (e2.message || e2));
    }
    try {
      execSync(`chown -R ${process.getuid ? process.getuid() : 'sxlvhdzo'}:$(id -gn) "${prismaGenDir}"`, { stdio: 'inherit', shell: true });
      console.log('chown on lib/generated/prisma/ ✓');
    } catch (e3) {
      console.log('chown failed: ' + (e3.message || e3));
    }
  }
  // Also ensure the parent 'generated' directory is writable
  try {
    execSync(`chmod 755 "${prismaGenParent}"`, { stdio: 'inherit', shell: true });
  } catch (_) { /* best-effort */ }
  console.log('--- End permission fix ---\n');

  // Pin the Prisma major version: in production-mode installs the CLI is not
  // present in node_modules, and a bare `npx prisma` would fetch the latest
  // major (7.x) which is incompatible with this Prisma 6 project.
  try {
    run(`"${npx}" prisma@6 generate`, 'Step 2/3 — prisma generate');
  } catch (prismaErr) {
    console.error('\n⚠ prisma generate failed again. The generated files may be owned by root.');
    console.error('  MANUAL FIX: Go to cPanel → File Manager → navigate to:');
    console.error('    /home/sxlvhdzo/project3/lib/generated/');
    console.error('  DELETE the "prisma" folder entirely, then re-run this cron job.');
    throw prismaErr;
  }

  // --- Fix ALL root-owned directories in the project -----------------------
  // The cPanel File Manager can leave directories owned by root. This breaks
  // next build (EACCES scandir). Fix the whole project tree at once.
  console.log('\n--- Fixing project-wide file permissions ---');
  // chmod on files first (files are easier to fix than root-owned dirs)
  try {
    execSync(`find "${__dirname}" -type f -not -path "*/node_modules/*" -not -path "*/.next/*" -exec chmod 644 {} +`, { stdio: 'inherit', shell: true });
    console.log('chmod 644 on all project files ✓');
  } catch (e) {
    console.log('File chmod: some files skipped (root-owned)');
  }
  // Fix known root-owned directories individually
  const rootOwnedDirs = [
    'app/api/admin/analytics/dashboard',
    'app/api/admin/analytics/reports',
    'app/users/delete-account',
    'app/api/admin/analytics',
    'app/api/admin/privacy-policies',
    'app/api/admin/ratings',
    'app/api/admin/requests',
    'app/api/admin/subscriptions',
    'app/api/admin/system-settings',
    'app/api/admin/terms-of-service',
    'app/api/admin/user-activities',
    'app/api/admin/user-ratings',
    'app/api/admin/users',
    'app/api/admin/whatsapp-groups',
    'app/api/auth/change-password',
    'app/api/auth/forgot-password',
    'app/api/auth/login',
    'app/api/auth/logout',
    'app/api/auth/refresh',
    'app/api/auth/register',
    'app/api/auth/reset-password',
    'app/api/auth/verify-otp',
    'app/api/auth/[...nextauth]',
    'app/api/integrity/verify',
    'app/api/irembo/applications',
    'app/api/irembo/driving',
    'app/api/irembo/special',
    'app/api/users/delete',
    'app/api/users/profile',
    'app/api/users/[id]',
    'components/examples/profile-page',
  ];
  let fixedDirs = 0;
  for (const d of rootOwnedDirs) {
    const fullPath = path.join(__dirname, d);
    try {
      fs.chmodSync(fullPath, 0o755);
      fixedDirs++;
    } catch (_) {
      // Try via shell (sometimes works even when Node fs fails)
      try {
        execSync(`chmod 755 "${fullPath}"`, { shell: true });
        fixedDirs++;
      } catch (_) {}
    }
  }
  // Also try generic find for remaining dirs
  try {
    execSync(`find "${__dirname}" -type d -not -path "*/node_modules/*" -not -path "*/.next/*" -exec chmod 755 {} +`, { stdio: 'inherit', shell: true });
    console.log('chmod 755 on all project directories ✓');
  } catch (e) {
    console.log('Directory chmod: some dirs skipped (root-owned). Fixed ' + fixedDirs + ' known dirs manually.');
  }
  console.log('--- End project-wide permission fix ---\n');

  // --- Build (or use pre-built .next) ---------------------------------------
  // Shared hosting has too low a thread/process quota for `next build`.
  // The user builds locally and uploads the .next folder via File Manager.
  const nextDir = path.join(__dirname, '.next');
  if (fs.existsSync(nextDir)) {
    console.log('\n✅ Using pre-built .next folder (build locally, upload via File Manager)');
    console.log('   Skipping npm run build — the server does not have enough threads.');
  } else {
    console.log('\n⚠ .next folder not found. Building on server (may fail on shared hosts)...');
    const buildEnv = Object.assign({}, process.env, {
      NODE_OPTIONS: '--max-old-space-size=512',
      NEXT_TELEMETRY_DISABLED: '1',
      NEXT_WORKER_COUNT: '1',
      RAYON_NUM_THREADS: '1',
    });
    execSync(`"${npm}" run build`, { stdio: 'inherit', shell: true, env: buildEnv });
  }

  // Restart the app so Passenger serves the new build (cPanel restart.txt mechanism).
  fs.mkdirSync(path.join(__dirname, 'tmp'), { recursive: true });
  fs.closeSync(fs.openSync(path.join(__dirname, 'tmp', 'restart.txt'), 'a')); // touch
  console.log('App restart triggered (tmp/restart.txt touched).');

  dbCheck()
    .then(() => checkMissingUploads())
    .finally(() => {
    console.log('\n========================================');
    console.log('  ✅ DEPLOY COMPLETE');
    console.log('========================================');
    console.log('Next: open https://console.amategekoyumuhanda.rw');
  });
} catch (err) {
  console.error('\n❌ DEPLOY FAILED at the step above.');
  // Real diagnostics so we never have to guess again:
  if (err && err.message) console.error('Error:', err.message);
  if (err && err.signal) {
    console.error('Process was killed by signal: ' + err.signal);
    console.error('(SIGKILL usually means the host (cPanel/LVE) killed it — typically out of memory or process limit)');
  }
  if (err && err.status) console.error('Exit code:', err.status);
  console.error('Scroll up for the full build output above this line.');
  process.exit(1);
}
