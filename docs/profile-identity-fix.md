# Cross-User Profile Leak — Root-Cause Trace & Fixes

Bug: login as **0732657993** → Profile page shows another user (e.g. **0782457226**).

Identity chain traced end-to-end:

```
Login input → POST /api/auth/login → users lookup → JWT (userId) → app stores token
→ GET /api/users/profile (Bearer) → middleware verifyTokenEdge → verifyToken → prisma.user.findUnique({id: token.userId})
→ response → app Room cache → Profile UI
```

## Where identity can change (found defects)

### A. Backend — duplicate phone-format accounts + pre-credential row picking (fixed here)

**Production DB audit found the smoking gun: 21 duplicate account pairs** — the
same person registered twice, once as `07…` (legacy) and once as `+250…`
(newer). The old login route picked ONE row (`IN variants` + `ORDER BY id asc`)
*before* verifying credentials, so it could authenticate the sibling duplicate
and issue that account's token — profile then "correctly" shows the other user.

Fix in `app/api/auth/login/route.ts`:
- Collect **all** candidate accounts across typed-format variants (exact →
  `+250…` → `250…`), then run device + password checks against each; the first
  candidate whose credentials pass wins. Identity is only trusted after the
  credential checks.
- Device binding now matches ANY registered device (was `devices[0]` only).
- Added `[AUTH-DEBUG]` logs: requested identifier, candidate userIds, token
  userId, and every failure path. Removed `console.log(body)` (leaked
  passwords into pm2 logs).
- Follow-up (ops): merge/remove the 21 duplicate pairs listed by
  `diag-phone-audit.js` so each number maps to exactly one active account.

### B. Backend — per-user responses were cacheable + missing PUT (fixed here)

`GET /api/users/profile`, `GET /api/subscriptions/user` and
`POST /api/auth/refresh` set no `Cache-Control`. Any intermediary (or browser)
that caches a profile response could replay user A's body to user B.
Now: `Cache-Control: no-store, private` + `[AUTH-DEBUG]` logs proving
`token.userId == response.userId` on every profile/subscription call.

Also: the Android app calls `PUT users/profile` but the backend had **no PUT
handler** (profile editing from the app could never succeed). Added a
token-derived PUT that only edits the caller's own record via a strict field
whitelist (no id/role/phone/password edits possible).

### C. Web console — refresh token survived logout (fixed here)

`lib/auth/auth-context.tsx` logout removed `admin_token`/`admin_user` but NOT
`admin_refresh_token`. Any 401 after logout made `lib/api-client.ts` mint a
fresh access token for the **previous** user from localStorage — the next
logged-in account could then see the previous account's data.
Now `admin_refresh_token` is removed on logout too.

### D. Android app — Room caches are cross-account (patch needed on `feature/role-based-app`)

The app already derives profile identity from the token and persists
`user_id` (TokenManager) — those hardenings exist. Remaining holes:

1. **`UserRepository.getProfile()` falls back to `userDao.getUser()`
   (`SELECT * FROM users LIMIT 1`) when `tokenManager.getUserId() == 0`.**
   `LIMIT 1` without ORDER BY is nondeterministic — after an account switch
   (or when any stale row remains) it can return the PREVIOUS user's row and
   render it while the network fetch is in flight (and permanently on error).
2. **`performLocalLogout()` clears `users` but NOT `user_subscription`.**
   User A's subscription row is served to user B by
   `SubscriptionRepository.getUserSubscription()` (also `LIMIT 1`) and
   `UserSubscriptionDao.getUserSubscription()`.
3. **`MaterialsFragment` observes `userDao.getUser()`** — same `LIMIT 1`
   problem for the cached-user reference it passes to its adapter.

Patch (apply on `feature/role-based-app`, `app/src/main/java/com/drivingschoolrwandaapp/`):

**1. `repository/UserRepository.java` — never fall back to LIMIT 1; clear subscription cache on logout:**

```java
// In getProfile(): replace the loadFromDb() body
@NonNull
@Override
protected LiveData<com.drivingschoolrwandaapp.database.entities.User> loadFromDb() {
    // Never fall back to userDao.getUser() (SELECT ... LIMIT 1): with no
    // ORDER BY it can return a STALE row from a previous account. If no
    // user id is persisted we return an empty live data — the network
    // fetch repopulates Room for the correct user.
    int userId = tokenManager.getUserId();
    if (userId > 0) {
        return userDao.getUserById(userId);
    }
    MediatorLiveData<com.drivingschoolrwandaapp.database.entities.User> empty = new MediatorLiveData<>();
    empty.setValue(null);
    return empty;
}
```

```java
// In performLocalLogout(): also clear the per-user subscription cache
private void performLocalLogout() {
    executeSafely(() -> {
        userDao.deleteAll();
        subscriptionDao.delete();   // inject UserSubscriptionDao into UserRepository (see RepositoryModule)
        tokenManager.clearTokens();
        navigateToLogin();
    });
}
```
Constructor + `di/RepositoryModule.java`: add `UserSubscriptionDao subscriptionDao`
parameter and pass `appDatabase.userSubscriptionDao()`.

**2. `database/dao/UserSubscriptionDao.java`** — already has `delete()`; no change needed.

**3. `ui/fragments/MaterialsFragment.java`:**

```java
// observeUser(): replace userDao.getUser() with the id-scoped query
int userId = tokenManager.getUserId(); // add a TokenManager field (injected or new TokenManager(requireContext()))
if (userId > 0) {
    userDao.getUserById(userId).observe(getViewLifecycleOwner(), user -> { ... });
}
```

**4. Debug logs for the required ID match test (add in `LoginActivity` success
branch and `ProfileFragment.updateUserProfile`):**

```java
Log.d("AUTH-DEBUG", "login: phone=" + phone + " userId=" + userId + " tokenUserId(from JWT payload if logged)=" + userId);
Log.d("AUTH-DEBUG", "profile: tokenUserId=" + tokenManager.getUserId() + " responseUserId=" + user.getId() + " phone=" + user.getPhoneNumber());
```
The four IDs (login userId, token userId, /profile request identity, profile
response userId) must match for the same account.

## Verification checklist (2 accounts)

- Login **0732657993** → profile shows only that account
  (`[AUTH-DEBUG] login token issued: token.userId=X` ==
  `[AUTH-DEBUG] /profile response: ... response.userId=X`).
- Login **0782457226** → same check.
- Logout A → login B → no A data (Room `users` + `user_subscription` empty at login; profile fetch repopulates).
- Kill + relaunch app while logged in → Splash routes by token; profile loads correct user.
- Expired token → 401 → TokenAuthenticator/logout → LoginActivity (no cached UI).
- Run `node diag-phone-audit.js` (read-only) to confirm no duplicate/variant-colliding phone rows in prod DB.

## Files changed on `backend-deploy`

- `app/api/auth/login/route.ts` — credential-aware multi-candidate lookup (defeats duplicate-format rows), device-match fix, identity logs, password log removed
- `app/api/users/profile/route.ts` — identity logs, `no-store, private`, full-profile log removed, NEW PUT handler (own-record, whitelisted fields)
- `app/api/auth/refresh/route.ts` — identity log, `no-store, private`
- `app/api/subscriptions/user/route.ts` — identity log, `no-store, private` (GET)
- `lib/auth/auth-context.tsx` — logout clears `admin_refresh_token`
- `diag-phone-audit.js` — read-only duplicate/variant-collision audit
