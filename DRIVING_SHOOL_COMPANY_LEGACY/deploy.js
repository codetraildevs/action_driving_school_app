// @ts-nocheck
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

// --- Self-logging: tee ALL output to deploy.log so cron captures it -------
const LOG = path.join(__dirname, 'deploy.log');
const logStream = fs.createWriteStream(LOG, { flags: 'a' });
function ts() { return new Date().toISOString(); }
function log(msg) { const line = '[' + ts() + '] ' + msg; console.log(line); logStream.write(line + '\n'); }
function logErr(msg) { const line = '[' + ts() + '] ' + msg; console.error(line); logStream.write(line + '\n'); }
log('--- deploy.js started (PID ' + process.pid + ') ---');

// Flush the log on any exit so cron captures everything
function flushAndExit(code) { logStream.end(() => process.exit(code)); }
process.on('exit', (c) => { try { logStream.end(); } catch (_) {} });
process.on('uncaughtException', (err) => {
  logErr('UNCAUGHT: ' + (err && err.stack ? err.stack : err));
  flushAndExit(1);
});

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
  log('Another deploy is still running — skipping this run.');
  logStream.end();
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

// --- Kill ALL Node processes to free nproc slots ---------------------------
// Shared cPanel hosts enforce a very low RLIMIT_NPROC (process count limit).
// During the build we need 3-4 node processes (deploy.js + npm + next build +
// static-generation worker). Passenger workers, stale `next dev`, and leftover
// npm/npx processes all consume nproc slots — if the limit is hit, `next build`
// crashes with "spawn ... EAGAIN".  Killing everything here is safe because:
//   1. The build itself runs AFTER this function returns.
//   2. `tmp/restart.txt` at the end tells Passenger to respawn its workers.
function killStrayNodeProcesses() {
  try {
    const out = execSync("ps -eo pid,cmd | grep -iE 'node|npm|npx' | grep -v grep", { encoding: 'utf8', shell: true });
    const me = process.pid;
    const killed = [];
    for (const line of out.trim().split('\n')) {
      const m = line.trim().match(/^(\d+)\s+(.*)$/);
      if (!m) continue;
      const pid = parseInt(m[1], 10);
      if (pid === me) continue;
      try { process.kill(pid, 'SIGKILL'); killed.push(pid); } catch (_) { /* already gone */ }
    }
    if (killed.length) console.log('Freed nproc slots — killed ' + killed.length + ' processes: ' + killed.join(', '));
    else console.log('No stray node processes found.');
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
      console.log('  (components-fix.tar.gz or components-fix.zip) was found in: ' + __dirname);
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

  // Remove directories that cause webpack/EACCES errors on shared hosting
  const problematicDirs = ['public/images/New folder'];
  for (const d of problematicDirs) {
    const full = path.join(__dirname, d);
    if (fs.existsSync(full)) {
      console.log('Removing problematic directory: ' + d);
      fs.rmSync(full, { recursive: true, force: true });
    }
  }

  run(`"${npm}" install --legacy-peer-deps`, 'Step 1/3 — npm install --legacy-peer-deps (this can take a few minutes)');
  // Pin the Prisma major version: in production-mode installs the CLI is not
  // present in node_modules, and a bare `npx prisma` would fetch the latest
  // major (7.x) which is incompatible with this Prisma 6 project.
  run(`"${npx}" prisma@6 generate`, 'Step 2/3 — prisma generate');

  // Cap Node's heap so a shared-host memory limit cannot silently kill the
  // build (a killed process prints NOTHING to the log — exactly what happened
  // when the build died right after "Creating an optimized production build").
  const buildEnv = Object.assign({}, process.env, {
    NODE_OPTIONS: '--max-old-space-size=1024',
    SWC_THREAD_COUNT: '1',
    RAYON_NUM_THREADS: '1',
    UV_THREADPOOL_SIZE: '1',
    NEXT_TELEMETRY_DISABLED: '1',
  });
  log('\nBuild memory cap: NODE_OPTIONS=--max-old-space-size=1024');
  execSync(`"${npm}" run build`, { stdio: 'inherit', shell: true, env: buildEnv });

  // Restart the app so Passenger serves the new build (cPanel restart.txt mechanism).
  fs.mkdirSync(path.join(__dirname, 'tmp'), { recursive: true });
  fs.closeSync(fs.openSync(path.join(__dirname, 'tmp', 'restart.txt'), 'a')); // touch
  log('App restart triggered (tmp/restart.txt touched).');

  dbCheck()
    .then(() => checkMissingUploads())
    .finally(() => {
    log('\n========================================');
    log('  ✅ DEPLOY COMPLETE');
    log('========================================');
    log('Next: open https://console.amategekoyumuhanda.rw');
    logStream.end();
  });
} catch (err) {
  logErr('\n❌ DEPLOY FAILED at the step above.');
  // Real diagnostics so we never have to guess again:
  if (err && err.message) logErr('Error: ' + err.message);
  if (err && err.signal) {
    logErr('Process was killed by signal: ' + err.signal);
    logErr('(SIGKILL usually means the host (cPanel/LVE) killed it — typically out of memory or process limit)');
  }
  if (err && err.status) logErr('Exit code: ' + err.status);
  logErr('Scroll up for the full build output above this line.');
  logStream.end();
  process.exit(1);
}
