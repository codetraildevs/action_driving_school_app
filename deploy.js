/**
 * Smart deploy for cPanel hosts — only rebuilds what changed.
 *
 * Checks file hashes before each step:
 *   - npm install: only if package.json or package-lock.json changed
 *   - prisma generate: only if prisma/schema.prisma changed
 *   - next build: only if source files changed
 *   - permissions: only if first run or root-owned dirs detected
 */
const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');

process.chdir(__dirname);

// --- Overlap lock ---
const LOCK = path.join(__dirname, '.deploy.lock');
function lockIsStale() {
  try {
    const pid = parseInt(fs.readFileSync(LOCK, 'utf8'), 10);
    if (!Number.isInteger(pid)) return true;
    process.kill(pid, 0);
    return false;
  } catch (_) { return true; }
}
if (fs.existsSync(LOCK) && !lockIsStale()) {
  console.log('Another deploy is still running — skipping.');
  process.exit(0);
}
fs.writeFileSync(LOCK, String(process.pid));
process.on('exit', () => { try { fs.unlinkSync(LOCK); } catch (_) {} });

const nodeBin = path.dirname(process.execPath);
const npm = path.join(nodeBin, 'npm');
const npx = path.join(nodeBin, 'npx');

// --- Hash cache for change detection ---
const HASH_FILE = path.join(__dirname, '.deploy-hashes.json');
function loadHashes() {
  try { return JSON.parse(fs.readFileSync(HASH_FILE, 'utf8')); }
  catch (_) { return {}; }
}
function saveHashes(h) { fs.writeFileSync(HASH_FILE, JSON.stringify(h, null, 2)); }

function fileHash(filePath) {
  try { return crypto.createHash('md5').update(fs.readFileSync(filePath)).digest('hex'); }
  catch (_) { return null; }
}

function dirHash(dir, patterns) {
  const files = [];
  function walk(d) {
    for (const f of fs.readdirSync(d, { withFileTypes: true })) {
      const full = path.join(d, f.name);
      if (f.isDirectory()) { if (f.name !== 'node_modules' && f.name !== '.next' && f.name !== 'tmp') walk(full); }
      else if (patterns.some(p => f.name.endsWith(p))) files.push(full);
    }
  }
  try { walk(dir); } catch (_) {}
  files.sort();
  const h = crypto.createHash('md5');
  for (const f of files) h.update(fileHash(f) || '');
  return h.digest('hex');
}

function run(cmd, label) {
  console.log('\n========================================');
  console.log('  ' + label);
  console.log('========================================');
  execSync(cmd, { stdio: 'inherit', shell: true });
}

// --- Kill stray processes ---
function killStray() {
  try {
    const out = execSync("ps -eo pid,ppid,cmd", { encoding: 'utf8', shell: true });
    const me = process.pid;
    const killed = [];
    for (const line of out.trim().split('\n')) {
      const p = line.trim().match(/^(\d+)\s+(\d+)\s+(.*)$/);
      if (!p) continue;
      const pid = parseInt(p[1], 10);
      const ppid = parseInt(p[2], 10);
      const cmd = p[3];
      if (pid === me || ppid === me) continue;
      if (cmd.includes('server.js') || cmd.includes('deploy.js')) continue;
      if (/grep|ps\s|sshd|bash|sh\s/.test(cmd)) continue;
      try { process.kill(pid, 'SIGKILL'); killed.push(pid); } catch (_) {}
    }
    if (killed.length) console.log('Killed: ' + killed.join(', '));
    else console.log('No stray processes.');
    try { execSync('sync', { shell: true }); } catch (_) {}
  } catch (e) { console.log('Process check skipped: ' + e.message); }
}

// --- Extract fix archives ---
function extractFixArchives() {
  const candidates = [
    { file: 'components-fix.tar.gz', cmd: (f) => `tar -xzf "${f}" -C .`, rename: true },
    { file: 'components-fix.zip', cmd: (f) => `unzip -o "${f}" -d .`, rename: true },
    { file: 'uploads-restore.zip', cmd: (f) => `unzip -o "${f}" -d .`, rename: true },
    { file: 'gazette-pdfs.zip', cmd: (f) => `unzip -o "${f}" -d .`, rename: true },
  ];
  for (const c of candidates) {
    const f = path.join(__dirname, c.file);
    if (fs.existsSync(f)) {
      console.log('Extracting ' + c.file + '...');
      execSync(c.cmd(f), { stdio: 'inherit', shell: true });
      if (c.rename) fs.renameSync(f, f + '.done');
    }
  }
}

function parseDatabaseUrl(url) {
  const u = new URL(url);
  return {
    host: u.hostname, port: u.port ? parseInt(u.port, 10) : 3306,
    user: decodeURIComponent(u.username), password: decodeURIComponent(u.password),
    database: u.pathname ? decodeURIComponent(u.pathname.slice(1)) : undefined,
  };
}

function rmrf(dirPath) {
  try { fs.rmSync(dirPath, { recursive: true, force: true }); return true; }
  catch (_) {
    try { execSync(`rm -rf "${dirPath}"`, { stdio: 'inherit', shell: true }); return true; }
    catch (e) { return false; }
  }
}

// --- Main deploy ---
const hashes = loadHashes();
const now = Date.now();
let changed = { install: false, prisma: false, build: false };

console.log('\n🔍 Checking for changes...');

// 1. Check npm install
const pkgHash = fileHash(path.join(__dirname, 'package.json'));
const lockHash = fileHash(path.join(__dirname, 'package-lock.json'));
const installKey = (pkgHash || '') + (lockHash || '');
if (hashes.install !== installKey) {
  console.log('📦 package.json or package-lock.json changed → npm install needed');
  changed.install = true;
} else {
  console.log('📦 No package changes → skipping npm install');
}

// 2. Check prisma
const schemaHash = fileHash(path.join(__dirname, 'prisma', 'schema.prisma'));
if (hashes.prisma !== schemaHash) {
  console.log('🗄️  schema.prisma changed → prisma generate needed');
  changed.prisma = true;
} else {
  console.log('🗄️  No schema changes → skipping prisma generate');
}

// 3. Check source files
const srcHash = dirHash(__dirname, ['.ts', '.tsx', '.js', '.mjs', '.css', '.json']);
const fullKey = srcHash + (pkgHash || '') + (schemaHash || '');
if (hashes.build !== fullKey) {
  console.log('🔨 Source files changed → build needed');
  changed.build = true;
} else {
  console.log('🔨 No source changes → skipping build');
}

const anythingChanged = changed.install || changed.prisma || changed.build;
if (!anythingChanged) {
  console.log('\n✅ Nothing changed — deploy skipped.');
  process.exit(0);
}

console.log('\n🚀 Starting deploy...\n');

killStray();
extractFixArchives();

// Step 1: npm install
if (changed.install) {
  run(`"${npm}" install --legacy-peer-deps`, 'npm install');
  hashes.install = installKey;
  saveHashes(hashes);
}

// Step 2: Fix Prisma permissions + generate
const prismaGenDir = path.join(__dirname, 'lib', 'generated', 'prisma');
if (changed.prisma) {
  console.log('\n--- Fixing Prisma permissions ---');
  if (fs.existsSync(prismaGenDir)) rmrf(prismaGenDir);
  fs.mkdirSync(path.join(__dirname, 'lib', 'generated'), { recursive: true });
  run(`"${npx}" prisma@6 generate`, 'prisma generate');
  hashes.prisma = schemaHash;
  saveHashes(hashes);
}

// Step 3: Fix permissions (always — first deploy or root-owned dirs)
console.log('\n--- Fixing permissions ---');
try {
  execSync(`find "${__dirname}" -type f -not -path "*/node_modules/*" -not -path "*/.next/*" -exec chmod 644 {} +`, { stdio: 'inherit', shell: true });
  execSync(`find "${__dirname}" -type d -not -path "*/node_modules/*" -not -path "*/.next/*" -exec chmod 755 {} +`, { stdio: 'inherit', shell: true });
  console.log('Permissions fixed ✓');
} catch (_) {}

// Step 4: Build
if (changed.build) {
  const nextDir = path.join(__dirname, '.next');
  if (fs.existsSync(nextDir)) {
    console.log('Removing old .next...');
    rmrf(nextDir);
  }
  const buildEnv = Object.assign({}, process.env, {
    NODE_OPTIONS: '--max-old-space-size=1024',
    NEXT_TELEMETRY_DISABLED: '1',
    NEXT_WORKER_COUNT: '1',
    RAYON_NUM_THREADS: '1',
  });
  run(`"${npm}" run build`, 'next build');
  hashes.build = fullKey;
  saveHashes(hashes);
}

// Step 5: Restart
fs.mkdirSync(path.join(__dirname, 'tmp'), { recursive: true });
fs.closeSync(fs.openSync(path.join(__dirname, 'tmp', 'restart.txt'), 'a'));
console.log('App restart triggered ✓');

// Step 6: Health checks
function dbCheck() {
  try {
    const env = fs.readFileSync(path.join(__dirname, '.env'), 'utf8');
    const m = env.match(/^DATABASE_URL\s*=\s*"?([^"\n]+)"?/m);
    if (!m) return Promise.resolve();
    process.env.DATABASE_URL = m[1];
    console.log('DB CHECK — host: ' + m[1].replace(/:\/\/[^@]+@/, '://***@'));
    const { PrismaClient } = require(path.join(__dirname, 'lib', 'generated', 'prisma'));
    const { PrismaMariaDb } = require(path.join(__dirname, 'node_modules', '@prisma', 'adapter-mariadb'));
    const adapter = new PrismaMariaDb(parseDatabaseUrl(m[1]));
    const p = new PrismaClient({ adapter });
    return p.$queryRaw`SELECT 1 as ok`
      .then((r) => console.log('DB CHECK: OK'))
      .catch((e) => console.log('DB CHECK: FAILED — ' + String(e.message || e).split('\n')[0]))
      .finally(() => p.$disconnect().catch(() => {}));
  } catch (_) { return Promise.resolve(); }
}

function checkUploads() {
  return new Promise((resolve) => {
    try {
      const env = fs.readFileSync(path.join(__dirname, '.env'), 'utf8');
      const m = env.match(/^DATABASE_URL\s*=\s*"?([^"\n]+)"?/m);
      if (!m) { resolve(); return; }
      const { PrismaClient } = require(path.join(__dirname, 'lib', 'generated', 'prisma'));
      const { PrismaMariaDb } = require(path.join(__dirname, 'node_modules', '@prisma', 'adapter-mariadb'));
      const adapter = new PrismaMariaDb(parseDatabaseUrl(m[1]));
      const p = new PrismaClient({ adapter });
      p.file.findMany({ select: { id: true, name: true, filePath: true } })
        .then((files) => {
          const missing = (files || []).filter(f => f.filePath && !fs.existsSync(path.join(process.cwd(), 'public', f.filePath)));
          if (missing.length) console.log('⚠ MISSING FILES: ' + missing.length);
          else console.log('UPLOADS: all ' + (files || []).length + ' files present ✓');
        })
        .catch(() => {})
        .finally(() => p.$disconnect().catch(() => {}).finally(resolve));
    } catch (_) { resolve(); }
  });
}

dbCheck()
  .then(() => checkUploads())
  .finally(() => {
    const elapsed = ((Date.now() - now) / 1000).toFixed(1);
    console.log('\n========================================');
    console.log('  ✅ DEPLOY COMPLETE (' + elapsed + 's)');
    console.log('========================================');
  });
