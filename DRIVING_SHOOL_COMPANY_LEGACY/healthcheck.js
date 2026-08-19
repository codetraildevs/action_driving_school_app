// @ts-nocheck
#!/usr/bin/env node
/**
 * Uptime / response-time healthcheck for the Driving School console.
 *
 * Add it as a cron job in cPanel → Cron Jobs (every 5 minutes):
 *
 *   /home/sxlvhdzo/nodevenv/project1/22/bin/node /home/sxlvhdzo/project1/healthcheck.js >> /home/sxlvhdzo/project1/healthcheck-cron.log 2>&1
 *
 * (use the "Every 5 minutes" preset in the cPanel UI for the schedule)
 *
 * What it does:
 *   1. Probes GET /api/health (app + DB) and GET / (landing) with a 15s timeout.
 *   2. Appends one line per run to healthcheck.log (auto-rotated).
 *   3. After 2 consecutive failures it emails an alert via the app's SMTP
 *      (EMAIL_HOST/EMAIL_USER/EMAIL_PASSWORD from .env) to ALERT_EMAIL
 *      (falls back to EMAIL_USER). Sends a "recovered" email when it's back.
 *
 * No dependencies beyond the project's own node_modules (nodemailer).
 * Requires Node 18+ (global fetch / https).
 */
const fs = require('fs');
const path = require('path');
const https = require('https');

process.chdir(__dirname);

const BASE_URL = process.env.CHECK_URL || 'https://console.amategekoyumuhanda.rw';
const LOG_FILE = path.join(__dirname, 'healthcheck.log');
const STATE_FILE = path.join(__dirname, 'healthcheck.state.json');
const ALERT_AFTER = 2; // consecutive failures before emailing
const TIMEOUT_MS = 15000;

// --- Minimal .env loader (no dotenv dependency) ---
// process.env wins over .env so cron/CLI overrides work.
function loadEnv() {
  const env = {};
  try {
    const txt = fs.readFileSync(path.join(__dirname, '.env'), 'utf8');
    for (const line of txt.split('\n')) {
      const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/);
      if (!m) continue;
      let v = m[2].trim();
      if ((v.startsWith('"') && v.endsWith('"')) || (v.startsWith("'") && v.endsWith("'"))) {
        v = v.slice(1, -1);
      }
      env[m[1]] = process.env[m[1]] !== undefined ? process.env[m[1]] : v;
    }
  } catch (_) { /* no .env — email alerts will be skipped */ }
  for (const k of ['EMAIL_HOST', 'EMAIL_PORT', 'EMAIL_USER', 'EMAIL_PASSWORD', 'EMAIL_FROM', 'ALERT_EMAIL', 'CHECK_URL']) {
    if (process.env[k] !== undefined && env[k] === undefined) env[k] = process.env[k];
  }
  return env;
}

// --- Single probe: { ok, code, ms } ---
function probe(url) {
  return new Promise((resolve) => {
    const started = Date.now();
    const req = https.get(url, { timeout: TIMEOUT_MS }, (res) => {
      res.resume(); // drain so the connection closes cleanly
      res.on('end', () => {
        resolve({
          ok: res.statusCode >= 200 && res.statusCode < 300,
          code: res.statusCode,
          ms: Date.now() - started,
        });
      });
    });
    req.on('timeout', () => {
      req.destroy(new Error('timeout'));
      resolve({ ok: false, code: 0, ms: TIMEOUT_MS, error: 'timeout' });
    });
    req.on('error', () => {
      resolve({ ok: false, code: 0, ms: Date.now() - started, error: 'network error' });
    });
  });
}

// --- Read / write state ---
function readState() {
  try {
    return JSON.parse(fs.readFileSync(STATE_FILE, 'utf8'));
  } catch (_) {
    return { consecutiveFailures: 0, alerted: false, lastFailureAt: null };
  }
}

function writeState(s) {
  try { fs.writeFileSync(STATE_FILE, JSON.stringify(s)); } catch (_) { /* non-fatal */ }
}

// --- Rotate the log so it never grows forever ---
function appendLog(line) {
  try {
    fs.appendFileSync(LOG_FILE, line + '\n');
    const size = fs.statSync(LOG_FILE).size;
    if (size > 2 * 1024 * 1024) {
      fs.copyFileSync(LOG_FILE, LOG_FILE + '.old');
      fs.truncateSync(LOG_FILE, 0);
    }
  } catch (_) { /* non-fatal */ }
}

// --- Email alert via the app's SMTP config ---
function sendEmail(env, subject, text) {
  return new Promise((resolve) => {
    if (!env.EMAIL_HOST || !env.EMAIL_USER) {
      console.log('  (email alert skipped: EMAIL_HOST/EMAIL_USER not set in .env)');
      return resolve(false);
    }
    try {
      const nodemailer = require('nodemailer');
      const transporter = nodemailer.createTransport({
        host: env.EMAIL_HOST,
        port: parseInt(env.EMAIL_PORT || '587', 10),
        secure: false,
        auth: { user: env.EMAIL_USER, pass: env.EMAIL_PASSWORD },
      });
      const to = env.ALERT_EMAIL || env.EMAIL_USER;
      transporter.sendMail(
        {
          from: env.EMAIL_FROM || env.EMAIL_USER,
          to,
          subject,
          text,
        },
        (err) => {
          if (err) console.log('  (email send failed: ' + err.message + ')');
          resolve(!err);
        }
      );
    } catch (e) {
      console.log('  (email alert failed: ' + e.message + ')');
      resolve(false);
    }
  });
}

async function main() {
  const env = loadEnv();
  const stamp = new Date().toISOString();
  const health = await probe(BASE_URL + '/api/health');
  const home = await probe(BASE_URL + '/');

  const state = readState();
  let note = 'OK';

  if (health.ok) {
    if (state.alerted) {
      // Recovery after a confirmed outage
      const sent = await sendEmail(
        env,
        '✅ Console recovered: ' + BASE_URL,
        'The console is back up.\n\n' +
          'Last failure: ' + (state.lastFailureAt || 'unknown') + '\n' +
          'Health now: HTTP ' + health.code + ' in ' + health.ms + 'ms\n' +
          'Timestamp: ' + stamp
      );
      note = 'RECOVERED' + (sent ? ' - recovery email sent' : '');
      state.alerted = false;
      state.consecutiveFailures = 0;
    } else {
      state.consecutiveFailures = 0;
    }
    state.lastFailureAt = null;
  } else {
    state.consecutiveFailures = (state.consecutiveFailures || 0) + 1;
    if (!state.lastFailureAt) state.lastFailureAt = stamp;

    if (state.consecutiveFailures >= ALERT_AFTER && !state.alerted) {
      const sent = await sendEmail(
        env,
        '🚨 Console DOWN: ' + BASE_URL,
        'The console is DOWN or unhealthy.\n\n' +
          'Consecutive failed checks: ' + state.consecutiveFailures + '\n' +
          'Health probe: HTTP ' + health.code + (health.error ? ' (' + health.error + ')' : '') + ' in ' + health.ms + 'ms\n' +
          'Landing page: HTTP ' + home.code + ' in ' + home.ms + 'ms\n' +
          'First failure at: ' + state.lastFailureAt + '\n' +
          'Timestamp: ' + stamp
      );
      // Only mark alerted when the email actually went out; otherwise the
      // next run retries the alert instead of staying silent.
      state.alerted = sent;
      note = 'DOWN x' + state.consecutiveFailures + (sent ? ' - alert email sent' : ' - alert email FAILED, will retry');
    } else {
      note = 'DOWN x' + state.consecutiveFailures + ' (no alert yet)';
    }
  }
  writeState(state);

  const line =
    stamp +
    ' | health:' + health.code + ' ' + health.ms + 'ms' +
    ' | home:' + home.code + ' ' + home.ms + 'ms' +
    ' | ' + note;
  console.log(line);
  appendLog(line);
}

main().catch((e) => {
  console.log(new Date().toISOString() + ' | healthcheck error: ' + (e && e.message ? e.message : e));
});
