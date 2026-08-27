# Auto-Deploy Setup Guide (cPanel — No Terminal Needed)

## Prerequisites
- cPanel access with **File Manager** and **Cron Jobs**
- Node.js app already set up in cPanel (project3)

## Step 1: Upload Files

1. Open **cPanel → File Manager**
2. Navigate to `/home/sxlvhdzo/project3/`
3. Upload these files (overwrite existing):
   - `auto-deploy.js`
   - `server.js`
   - `deploy.js`
   - `healthcheck.js`
4. Make sure `node_modules/` and `.env` are NOT overwritten

## Step 2: Set Up Cron Job

1. Open **cPanel → Cron Jobs**
2. Under "Add New Cron Job":
   - **Common Settings:** Select "Every 5 minutes"
   - **Command:** 
   ```
   /home/sxlvhdzo/nodevenv/project3/22/bin/node /home/sxlvhdzo/project3/auto-deploy.js >> /home/sxlvhdzo/project3/auto-deploy.log 2>&1
   ```
3. Click **Add New Cron Job**

## Step 3: Verify It's Working

After 5 minutes, check:
- **File Manager** → `auto-deploy.log` should show entries
- Open `https://console.amategekoyumuhanda.rw/api/health` — should return JSON

## What Happens Automatically

| Situation | Action |
|---|---|
| `.next` is missing | Full rebuild (npm install → prisma → next build) |
| `.next` exists, HTTP works | No action (logs "All healthy") |
| `.next` exists, HTTP fails | Restart Passenger, rebuild if still down |
| Deploy already running | Skip (prevents overlap) |

## Logs to Check

| Log File | Shows |
|---|---|
| `auto-deploy.log` | Auto-deploy decisions and results |
| `deploy.log` | Full deploy output |
| `healthcheck.log` | Health check results |

## Manual Deploy (if needed)

Upload `deploy.js` and run via cPanel → Setup Node.js Apps → Run JS Script.
