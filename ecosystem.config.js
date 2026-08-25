module.exports = {
  apps: [
    {
      name: 'driving-school',
      script: './server.js',
      cwd: '/home/project3',

      // ── Instances ──
      // Single instance to save memory on 4GB VPS
      instances: 1,
      exec_mode: 'fork',

      // ── Environment ──
      env: {
        NODE_ENV: 'production',
        PORT: 3000,
      },

      // ── Memory Management ──
      // Restart if a single worker exceeds 512MB (leak protection).
      // With 4GB RAM and 2 workers, each gets ~512MB max.
      max_memory_restart: '512M',

      // ── Restart Policy ──
      // PM2 will restart the app if it crashes.
      // On a VPS with systemd, PM2 itself auto-starts on boot.
      restart_delay: 3000,         // Wait 3s between restarts
      max_restarts: 15,            // Max restarts before giving up (prevents crash loops)
      min_uptime: '10s',           // App must run 10s to be considered "started"

      // ── Logs ──
      log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
      error_file: '/home/logs/driving-school-error.log',
      out_file: '/home/logs/driving-school-out.log',
      merge_logs: true,            // Combine cluster worker logs into one file
      log_type: 'json',            // Structured logs for easy parsing

      // ── Watch (disabled in production) ──
      watch: false,

      // ── Graceful Shutdown ──
      kill_timeout: 5000,          // 5s to finish pending requests before SIGKILL
      listen_timeout: 10000,       // 10s for app to signal "ready"

      // ── Advanced ──
      // Prevent false restarts from slow startups
      exp_backoff_restart_delay: 100, // Exponential backoff: 100ms, 200ms, 400ms...
    },
  ],
};
