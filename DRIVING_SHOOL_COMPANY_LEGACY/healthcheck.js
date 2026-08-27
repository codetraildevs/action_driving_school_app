// @ts-nocheck
/**
 * healthcheck.js — Standalone health check for the Next.js app.
 *
 * Can be called from:
 *   - Cron:  /home/sxlvhdzo/nodevenv/project3/22/bin/node healthcheck.js
 *   - External: curl https://console.amategekoyumuhanda.rw/api/health
 *   - Passenger: touch tmp/restart.txt if unhealthy
 *
 * Checks:
 *   1. .next/BUILD_ID exists and is non-empty
 *   2. Node process can start without crashing
 *   3. Database is reachable (optional)
 */
const fs = require('fs');
const path = require('path');
const http = require('http');

const APP_DIR = __dirname;
const BUILD_ID_FILE = path.join(APP_DIR, '.next', 'BUILD_ID');
const RESTART_FILE = path.join(APP_DIR, 'tmp', 'restart.txt');
const LOG_FILE = path.join(APP_DIR, 'healthcheck.log');
const PORT = process.env.PORT || 3000;

// --- Logging ---
function log(msg) {
  const line = `[${new Date().toISOString()}] ${msg}`;
  console.log(line);
  try { fs.appendFileSync(LOG_FILE, line + '\n'); } catch (_) {}
}

// --- Check 1: BUILD_ID exists ---
function checkBuildId() {
  try {
    if (!fs.existsSync(BUILD_ID_FILE)) {
      return { ok: false, reason: '.next/BUILD_ID is MISSING' };
    }
    const id = fs.readFileSync(BUILD_ID_FILE, 'utf8').trim();
    if (!id) {
      return { ok: false, reason: '.next/BUILD_ID is empty' };
    }
    return { ok: true, buildId: id };
  } catch (e) {
    return { ok: false, reason: `BUILD_ID read error: ${e.message}` };
  }
}

// --- Check 2: HTTP health (is the server responding?) ---
function checkHttp(timeout = 5000) {
  return new Promise((resolve) => {
    const req = http.get(`http://localhost:${PORT}/api/health`, { timeout }, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          resolve({ ok: json.status === 'ok', data: json });
        } catch (_) {
          resolve({ ok: false, reason: `Invalid JSON: ${data.substring(0, 100)}` });
        }
      });
    });
    req.on('error', (e) => resolve({ ok: false, reason: e.message }));
    req.on('timeout', () => { req.destroy(); resolve({ ok: false, reason: 'timeout' }); });
  });
}

// --- Auto-rebuild if build is missing ---
function triggerRebuild() {
  log('触发 Auto-rebuild: .next is missing');
  const { execSync } = require('child_process');
  const nodeBin = path.dirname(process.execPath);
  const deployJs = path.join(APP_DIR, 'deploy.js');

  try {
    execSync(`"${path.join(nodeBin, 'node')}" "${deployJs}"`, {
      stdio: 'inherit',
      shell: true,
      timeout: 600000, // 10 minutes max
    });
    log('✅ Auto-rebuild completed');
    return true;
  } catch (e) {
    log(`❌ Auto-rebuild failed: ${e.message}`);
    return false;
  }
}

// --- Main ---
async function main() {
  log('--- Health check started ---');

  // Check 1: Build
  const build = checkBuildId();
  if (!build.ok) {
    log(`⚠ Build check FAILED: ${build.reason}`);
    const rebuilt = triggerRebuild();
    if (!rebuilt) {
      log('❌ Health check FAILED — rebuild did not succeed');
      process.exit(1);
    }
  } else {
    log(`✅ Build OK (BUILD_ID: ${build.buildId})`);
  }

  // Check 2: HTTP
  const httpCheck = await checkHttp();
  if (httpCheck.ok) {
    log(`✅ HTTP OK — uptime: ${httpCheck.data.uptime}s, memory: ${httpCheck.data.memory?.rss}`);
  } else {
    log(`⚠ HTTP check FAILED: ${httpCheck.reason}`);
    // Touch restart.txt to force Passenger to restart workers
    try {
      fs.mkdirSync(path.join(APP_DIR, 'tmp'), { recursive: true });
      fs.closeSync(fs.openSync(RESTART_FILE, 'a'));
      log('Touched tmp/restart.txt — Passenger will restart workers');
    } catch (e) {
      log(`Could not restart: ${e.message}`);
    }
  }

  log('--- Health check complete ---');
}

main().catch((e) => {
  log(`❌ Health check crashed: ${e.message}`);
  process.exit(1);
});
