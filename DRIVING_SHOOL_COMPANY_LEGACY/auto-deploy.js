// @ts-nocheck
/**
 * auto-deploy.js — One-click deploy for cPanel (no terminal needed)
 *
 * SETUP (one-time):
 *   1. Upload ALL files to /home/sxlvhdzo/project3/ via cPanel File Manager
 *   2. cPanel → Cron Jobs → Add New Cron Job:
 *      - Common Settings: "Every 5 minutes"
 *      - Command: /home/sxlvhdzo/nodevenv/project3/22/bin/node /home/sxlvhdzo/project3/auto-deploy.js
 *   3. Save — that's it!
 *
 * WHAT IT DOES:
 *   - Checks if .next/BUILD_ID exists (build is healthy)
 *   - If missing → runs full deploy (npm install → prisma generate → next build)
 *   - If exists → checks HTTP health, restarts if needed
 *   - Logs everything to auto-deploy.log
 *   - Won't overlap with running deploys
 *
 * TRIGGERS:
 *   - .next/BUILD_ID missing → full rebuild
 *   - HTTP health check fails → restart Passenger
 *   - Every 5 minutes via cron
 */
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const http = require('http');

// --- Configuration ---
const APP_DIR = __dirname;
const NODE_BIN = path.dirname(process.execPath);
const npm = path.join(NODE_BIN, 'npm');
const npx = path.join(NODE_BIN, 'npx');
const node = path.join(NODE_BIN, 'node');

const BUILD_ID_FILE = path.join(APP_DIR, '.next', 'BUILD_ID');
const LOCK_FILE = path.join(APP_DIR, '.deploy.lock');
const RESTART_FILE = path.join(APP_DIR, 'tmp', 'restart.txt');
const LOG_FILE = path.join(APP_DIR, 'auto-deploy.log');
const PORT = process.env.PORT || 3000;

// --- Logging ---
function log(msg) {
  const line = `[${new Date().toISOString()}] ${msg}`;
  console.log(line);
  try {
    // Rotate log if > 1MB
    if (fs.existsSync(LOG_FILE)) {
      const stat = fs.statSync(LOG_FILE);
      if (stat.size > 1048576) {
        fs.renameSync(LOG_FILE, LOG_FILE + '.old');
      }
    }
    fs.appendFileSync(LOG_FILE, line + '\n');
  } catch (_) {}
}

// --- Lock (prevent overlapping deploys) ---
function acquireLock() {
  try {
    if (fs.existsSync(LOCK_FILE)) {
      const pid = parseInt(fs.readFileSync(LOCK_FILE, 'utf8'), 10);
      if (!isNaN(pid)) {
        try {
          process.kill(pid, 0); // Check if process is alive
          return false; // Lock is held by live process
        } catch (_) {
          // Process is dead — lock is stale
          fs.unlinkSync(LOCK_FILE);
        }
      } else {
        fs.unlinkSync(LOCK_FILE);
      }
    }
    fs.writeFileSync(LOCK_FILE, String(process.pid));
    return true;
  } catch (e) {
    log('Lock error: ' + e.message);
    return true; // Proceed anyway
  }
}

function releaseLock() {
  try { fs.unlinkSync(LOCK_FILE); } catch (_) {}
}

// --- Build Check ---
function isBuildHealthy() {
  try {
    if (!fs.existsSync(BUILD_ID_FILE)) return false;
    const id = fs.readFileSync(BUILD_ID_FILE, 'utf8').trim();
    return id.length > 0;
  } catch (_) {
    return false;
  }
}

// --- HTTP Health Check ---
function checkHttp(timeout = 10000) {
  return new Promise((resolve) => {
    const req = http.get(`http://localhost:${PORT}/api/health`, { timeout }, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          resolve({ ok: json.status === 'ok', data: json });
        } catch (_) {
          resolve({ ok: false, reason: 'Invalid response' });
        }
      });
    });
    req.on('error', (e) => resolve({ ok: false, reason: e.message }));
    req.on('timeout', () => { req.destroy(); resolve({ ok: false, reason: 'timeout' }); });
  });
}

// --- Deploy Steps ---
function runStep(cmd, label) {
  log(`  → ${label}`);
  try {
    execSync(cmd, {
      stdio: 'pipe',
      shell: true,
      timeout: 300000, // 5 min max per step
      env: Object.assign({}, process.env, {
        NODE_OPTIONS: '--max-old-space-size=1024',
        SWC_THREAD_COUNT: '1',
        RAYON_NUM_THREADS: '1',
        UV_THREADPOOL_SIZE: '1',
        NEXT_TELEMETRY_DISABLED: '1',
      }),
    });
    log(`  ✓ ${label} — done`);
    return true;
  } catch (e) {
    log(`  ✗ ${label} — FAILED: ${e.message.split('\n')[0]}`);
    return false;
  }
}

function runDeploy() {
  log('═══════════════════════════════════════════════════════');
  log('  DEPLOY STARTED');
  log('═══════════════════════════════════════════════════════');

  // Step 1: npm install
  if (!runStep(`"${npm}" install --legacy-peer-deps`, 'npm install')) {
    log('Deploy failed at npm install');
    return false;
  }

  // Step 2: prisma generate
  if (!runStep(`"${npx}" prisma@6 generate`, 'prisma generate')) {
    log('Deploy failed at prisma generate');
    return false;
  }

  // Step 3: next build
  if (!runStep(`"${npm}" run build`, 'next build')) {
    log('Deploy failed at next build');
    return false;
  }

  // Step 4: Verify build
  if (!isBuildHealthy()) {
    log('Deploy failed — .next/BUILD_ID is missing after build');
    log('Build was likely killed by host (OOM/process limit)');
    return false;
  }

  const buildId = fs.readFileSync(BUILD_ID_FILE, 'utf8').trim();
  log(`Build verified — BUILD_ID: ${buildId}`);

  // Step 5: Restart Passenger
  try {
    fs.mkdirSync(path.join(APP_DIR, 'tmp'), { recursive: true });
    fs.closeSync(fs.openSync(RESTART_FILE, 'a'));
    log('Passenger restart triggered (tmp/restart.txt)');
  } catch (e) {
    log('Restart warning: ' + e.message);
  }

  log('═══════════════════════════════════════════════════════');
  log('  ✅ DEPLOY COMPLETE');
  log('═══════════════════════════════════════════════════════');
  return true;
}

// --- Main ---
async function main() {
  log('--- auto-deploy.js started ---');

  // Acquire lock
  if (!acquireLock()) {
    log('SKIP — another deploy is running');
    return;
  }

  try {
    // Check 1: Is build healthy?
    if (isBuildHealthy()) {
      const buildId = fs.readFileSync(BUILD_ID_FILE, 'utf8').trim();
      log(`Build OK (BUILD_ID: ${buildId})`);

      // Check 2: Is HTTP responding?
      const httpCheck = await checkHttp();
      if (httpCheck.ok) {
        log(`HTTP OK — uptime: ${httpCheck.data.uptime}s`);
        log('All healthy — no action needed');
        return;
      }

      // HTTP is down — restart Passenger
      log(`HTTP check failed: ${httpCheck.reason}`);
      log('Restarting Passenger...');
      try {
        fs.mkdirSync(path.join(APP_DIR, 'tmp'), { recursive: true });
        fs.closeSync(fs.openSync(RESTART_FILE, 'a'));
        log('Passenger restart triggered');
      } catch (e) {
        log('Restart failed: ' + e.message);
      }

      // Wait and recheck
      await new Promise(r => setTimeout(r, 5000));
      const recheck = await checkHttp();
      if (recheck.ok) {
        log('HTTP recovered after restart');
      } else {
        log('HTTP still down — full rebuild needed');
        runDeploy();
      }
    } else {
      // Build is missing — full deploy
      log('.next/BUILD_ID is MISSING — starting full deploy');
      runDeploy();
    }
  } finally {
    releaseLock();
    log('--- auto-deploy.js finished ---');
  }
}

main().catch((e) => {
  log('FATAL: ' + e.message);
  releaseLock();
  process.exit(1);
});
