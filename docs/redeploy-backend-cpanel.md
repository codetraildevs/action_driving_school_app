# Redeploy backend to cPanel (File Manager) — roles.ts fix

The fixed file is `DRIVING_SHOOL_COMPANY_LEGACY/lib/auth/roles.ts` (role-name check now
tolerates Title Case: `"Super Admin"` / `"super_admin"` / `"ADMIN"` are all treated as admin).

> ⚠️ You cannot upload a single `.ts` file — the server runs the **compiled build**, so the whole
> app must be re-uploaded and rebuilt.

## What's ready

- **`backend-deploy.tar.gz`** (12.7 MB) — the full backend source with the fix, excluding
  `node_modules`, `.next`, `.env`, `uploads/`, `lib/generated` (Prisma client is regenerated
  server-side), old `public/*.zip` backups, and git files.
- Rebuild any time with: `bash docs/build-deploy-archive.sh`

## Steps (cPanel File Manager)

1. **cPanel → File Manager** → open the folder where the backend app lives
   (e.g. `rwanda_app_backend` / `repositories/<app>` — wherever `package.json` + `.next` are).

2. Upload `backend-deploy.tar.gz` into that folder (File Manager → Upload, or drag & drop).

3. Select the file → **Extract** (File Manager extracts `.tar.gz` natively).

4. When asked about overwriting existing files, choose **Overwrite / Replace All**.

5. Open **cPanel → Terminal** in that folder and rebuild + restart:

   ```bash
   npm install
   npx prisma generate        # regenerates lib/generated (excluded from the archive)
   npm run build
   ```

6. **cPanel → Setup Node.js App** (or Passenger) → select the app → **Restart**.

> ⚠️ Do NOT touch `.env` on the server — it holds the working `DATABASE_URL`. The archive excludes
> it precisely so the upload can't clobber it.

## Verify

```bash
# login as the admin (from the DB: phone 0732657995, role 2)
curl -s -X POST https://console.amategekoyumuhanda.rw/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"identifier":"0732657995","password":"<ADMIN_PASSWORD>","deviceId":"any"}'
```

Expected: `success:true` and the `user` object **must include** `"role":2` and
`"roleName":"admin"`. Then:

```bash
# admin endpoint must return 200, not 403
curl -s -H "Authorization: Bearer <ACCESS_TOKEN>" \
  https://console.amategekoyumuhanda.rw/api/admin/users
```
