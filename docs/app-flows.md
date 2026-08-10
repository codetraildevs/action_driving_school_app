# App Flows — User &amp; Admin

Interactive HTML version: [app-flows.html](./app-flows.html)

**Roles:** regular user = any role ≠ 1/2 (student 5, premium 6, free 7, …) · admin = **super_admin (1)** or **admin (2)**.

## User flow

```mermaid
flowchart TD
    A[Launch: SplashActivity] --> B{Logged in?<br/>valid token in TokenManager}
    B -- no --> C[WelcomeActivity]
    C --> D[Login / Register]
    B -- yes --> E{Role is admin?<br/>RoleUtils.isAdminRole 1-2}
    E -- no --> F[App - Main Activity<br/>Home · Exams · Materials · Irembo · Profile]
    E -- yes --> G[AdminActivity - Admin Console]
    D --> H{Backend: POST /api/auth/login<br/>valid creds + isActive}
    H -- 200 --> I[Save accessToken + role]
    I --> E
    H -- 401/403 --> D
    F --> J[Profile tab: role badge]
```

**Key points**
- **One shared login, role-based landing** — there is exactly one login (user + admin use the same `LoginActivity`); after login the user is routed to the app that matches their role. There is **no in-app switching** between the user app and the admin console: the drawer has no “Admin Console” entry and the admin console has no “Open User App” link. To use the other experience, log out and log back in.
- **Registration is student-only** — `POST /api/auth/register` always creates role 5 (`STUDENT_ROLE_ID`); a client-supplied `role` is ignored. There is no admin registration path.
- The role is persisted via `TokenManager.saveRole` and refreshed on every profile load (`UserRepository.mapUser`), so a server-side role change applies without re-login (the next launch lands in the right app).

## Admin flow

```mermaid
flowchart TD
    A[Launch: SplashActivity] --> B{Logged in?}
    B -- yes --> C{Role 1 or 2?<br/>persisted in TokenManager}
    C -- yes --> D[AdminActivity]
    C -- no --> E[App - Main Activity]
    B -- no --> F[WelcomeActivity]
    D --> G{onCreate re-check:<br/>isAdminRole? defense in depth}
    G -- no --> E
    G -- yes --> H[Admin Console<br/>Dashboard · Users · Requests · Settings]
    H --> I[Admin API calls<br/>Authorization: Bearer token]
    I --> J{Backend guard<br/>verifyToken + role check}
    J -- 200 --> K[Dashboard stats · users · requests · user detail]
    J -- 401 --> L[Missing / invalid / expired token]
    J -- 403 --> M[Authenticated but not admin]
    H --> N[Settings: Web Console · Logout]
```

## Backend guard layer — `/api/admin/*`

```mermaid
flowchart LR
    A[AuthInterceptor<br/>Bearer token] --> B[verifyToken JWT<br/>valid + userId]
    B --> C[Role check<br/>isAdminRoleName<br/>admin / super_admin]
    C -- ok --> D[200 - data]
    C -- fail --> E[403 Forbidden]
    B -- fail --> F[401 Unauthorized]
```

## Entry points &amp; guards summary

| Entry point | What it does | Guard |
|---|---|---|
| `SplashActivity` | Routes by login state + role | `TokenManager.isLoggedIn`, `RoleUtils.isAdminRole` |
| `LoginActivity` | Login → saves token + role → routes by role | Backend 401/403; admin → AdminActivity |
| `RegisterActivity` | Register → always lands in user App | Backend always creates Student (role 5) |
| `App` (user app) | 5 tabs + drawer; no admin-console entry (role switch requires logout+login) | — |
| `AdminActivity` | 4-tab console; no “Open User App” link (role switch requires logout+login) | onCreate re-check bounces non-admins to App |
| Backend `/api/admin/*` | Dashboard, users, requests, user detail | `verifyToken` + `isAdminRoleName` → 401/403 |
