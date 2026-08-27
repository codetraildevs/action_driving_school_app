const { createServer } = require('http');
const { parse } = require('url');
const next = require('next');
const fs = require('fs');
const path = require('path');

const dev = process.env.NODE_ENV !== 'production';
const hostname = 'localhost';
const port = process.env.PORT || 3000;

// --- Verify .next build exists before starting --------------------------------
// Shared hosting (cPanel/Passenger) can leave .next in a corrupt state if a
// build was killed mid-way. This check prevents workers from crashing on start.
const nextBuildDir = path.join(__dirname, '.next');
const buildIdFile = path.join(nextBuildDir, 'BUILD_ID');

function verifyBuild() {
  try {
    if (!fs.existsSync(nextBuildDir)) {
      console.error('[SERVER] .next directory is MISSING');
      return false;
    }
    if (!fs.existsSync(buildIdFile)) {
      console.error('[SERVER] .next/BUILD_ID is MISSING — build may be corrupt');
      return false;
    }
    const buildId = fs.readFileSync(buildIdFile, 'utf8').trim();
    console.log('[SERVER] Build verified — BUILD_ID:', buildId);
    return true;
  } catch (e) {
    console.error('[SERVER] Build verification failed:', e.message);
    return false;
  }
}

// If build is missing, log clear instructions and exit
if (!verifyBuild()) {
  console.error('');
  console.error('═══════════════════════════════════════════════════════════════');
  console.error('  .next BUILD IS MISSING — the app cannot start.');
  console.error('');
  console.error('  To fix this, run deploy.js on the server:');
  console.error('    1. cPanel → Setup Node.js Apps → Run JS Script');
  console.error('    2. Enter: deploy.js');
  console.error('    3. Click Run');
  console.error('');
  console.error('  Or via cPanel terminal:');
  console.error('    cd /home/sxlvhdzo/project3');
  console.error('    /home/sxlvhdzo/nodevenv/project3/22/bin/node deploy.js');
  console.error('═══════════════════════════════════════════════════════════════');
  console.error('');
  // Exit with error so Passenger knows this worker is unhealthy
  // and doesn't route traffic to it.
  process.exit(1);
}

const app = next({ dev, hostname, port });
const handle = app.getRequestHandler();

app.prepare().then(() => {
  createServer(async (req, res) => {
    try {
      const parsedUrl = parse(req.url, true);
      const { pathname, query } = parsedUrl;

      // Health check endpoint — returns server status without going through Next.js
      if (pathname === '/api/health') {
        const uptime = process.uptime();
        const mem = process.memoryUsage();
        const buildId = fs.existsSync(buildIdFile)
          ? fs.readFileSync(buildIdFile, 'utf8').trim()
          : 'MISSING';
        res.setHeader('Content-Type', 'application/json');
        res.end(JSON.stringify({
          status: 'ok',
          buildId,
          uptime: Math.floor(uptime),
          memory: {
            rss: Math.floor(mem.rss / 1024 / 1024) + 'MB',
            heap: Math.floor(mem.heapUsed / 1024 / 1024) + 'MB',
          },
          timestamp: new Date().toISOString(),
        }));
        return;
      }

      await handle(req, res, parsedUrl);
    } catch (err) {
      console.error('Error occurred handling', req.url, err);
      res.statusCode = 500;
      res.end('Internal server error');
    }
  })
    .once('error', (err) => {
      console.error(err);
      process.exit(1);
    })
    .listen(port, () => {
      console.log(`> Ready on http://${hostname}:${port}`);
    });
});