# Security and data retention

## Workspace boundary

A browser’s server-side session selects its private workspace. This version has no staff accounts, borrower authentication or cross-device recovery. Another browser starts empty. Anyone holding a valid session cookie can use that workspace, so use a trusted device and export records you need to retain.

Every equipment, borrower and loan lookup is scoped to the workspace. Exports, search and summaries use the same boundary. Referencing another workspace’s UUID returns 404 and changes no stock. UUIDs alone are not the authorization check.

The cookie is HttpOnly, SameSite=Strict and has a seven-day lifetime. Spring Session JDBC persists server-side sessions in both database modes. CSRF is required on every state-changing route, including demo creation and workspace deletion. The token is held in frontend memory, never localStorage. Secure cookies are configurable and required for public HTTPS hosting.

## Input, browser and database controls

- JSON bodies are bounded to 32 KB before parsing; unknown fields and numeric/boolean coercion into text are rejected.
- Lengths, categories, quantity ranges, UUIDs, dates, pages and filter shapes are validated. Repeated query keys are rejected.
- Text is rendered through React escaping. Notes and descriptions are plain text.
- SQL values use bound parameters; stock and owned references have database constraints.
- A strict same-origin CSP, denied framing, nosniff, referrer and permissions policies apply. API responses use no-store caching.
- No uploads, analytics, external fonts, image requests, email delivery or server-side URL fetching are used.
- Unexpected API failures expose generic messages and log the exception class without request bodies or borrower notes.
- Approximate process-local counters target 300 API requests/minute per remote address and cap stored counters. They reset on restart and are not a shared distributed limiter. Public hosting needs an edge limiter.

## Inventory integrity

Workspace/record row locks, database transactions and SQL constraints protect checkout, return and stock edits. Two requests cannot borrow the same last unit; two returns cannot restore stock twice. A test forces the final insert to fail after the decrement and verifies that the decrement is rolled back.

## Retention

Names, team labels and loan notes are stored only for managing loans; no contact information is required. Normal and demo workspaces have the same scope and limits.

The cookie lasts up to seven days. Inactive workspaces are eligible for deletion after seven days. A cleanup task starts one minute after startup, then hourly, removing up to 100 expired workspaces per pass. It rechecks expiry while locked. Spring Session separately removes expired session rows. A stopped server cannot clean data until it runs again.

Confirmed workspace deletion removes loans first, then people, equipment and the workspace, in one transaction, and invalidates the current session. Export includes all owned records and omits internal workspace/session material. Backups, filesystem copies and operator logs have separate retention policies.

## Container and hosting

The app runs as a non-root user with a read-only filesystem, bounded temporary storage, dropped capabilities and no new privileges. Compose exposes only localhost. Its MySQL account is dedicated, and the root password is not passed to the app container. Local MySQL traffic uses the private Docker network; use verified database TLS for external database connections.

For public hosting, configure HTTPS, secure cookies, a controlled reverse proxy, edge limits, monitoring and tested backups. Keep the session-bound workspace model clear to users. The project has no independent security certification or legal compliance guarantee. Dependency audits detect known advisories and do not establish absence of vulnerabilities.

## Security review: 2026-10-28

Assets are borrower names/teams/notes, inventory and loan integrity, database credentials, and session cookies/CSRF material. The trust boundaries are browser → HTTP API, session → workspace ownership, API → SQL database, local disk → OS users, and source/build → GitHub/container publication.

| Abuse case                                           | Enforced control                                                                                                     |
| ---------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| Another workspace's UUID or CSRF token is supplied   | Ownership filters, composite foreign keys, per-session CSRF; no stock changes on rejection                           |
| SQL or HTML is placed in a field                     | Bound SQL values, strict JSON/field validation and React text escaping                                               |
| A client pretends HTTP arrived over HTTPS            | Forwarded headers ignored by default; secure-cookie mode rejects plaintext access with 426                           |
| Production starts with weak settings                 | Early configuration check before connections/migrations; dedicated DB user, secure cookies, no demo, verified DB TLS |
| Private files or credentials enter a publication     | Git/Docker exclusions, publishable-file checks and redacted Gitleaks scan in CI                                      |
| Another OS account opens the local preview database  | Private .local directory (0700), files (0600), launcher umask 077                                                    |
| Accidental diagnostic output leaks submitted records | Generic errors, no request/body/access logging, no H2 console                                                        |
| Loan writes race or fail midway                      | Row locks, transactions, rollback tests and database constraints                                                     |

### Keys and publication

This version has no third-party API integration and needs no external API key. MySQL passwords are read only by the backend/container, never sent to React. Never put a secret in a VITE_ variable: Vite exposes those values in the browser bundle. API URLs and the OpenAPI contract are public interfaces; hiding URLs is not access control.

The .env template contains placeholders. Generated .env credentials are mode 0600 and ignored. Private key/certificate stores, database copies and local state are excluded from Git and Docker. Exclusions do not undo an already tracked file; the secrets check explicitly rejects private paths among publication candidates, including tracked files. Never use git add --force for these files.

Run `python3 scripts/check-secrets.py` before publishing. It downloads Gitleaks 8.30.1 from the official release, verifies a pinned SHA-256, scans a temporary copy of publishable files, the staged Git index and any compiled client, and scans all local Git history when this folder is a repository. Matches are redacted; source and secrets are not uploaded. Scanners detect patterns, not every possible credential. The repository history is scanned as part of publication and CI. When Git is initialized, `git config core.hooksPath .githooks` enables the included local commit check. CI repeats the scan with full history and no persisted checkout credentials.

If a secret ever reaches GitHub, revoke/rotate it first, then remove it from files and history. Deleting the file alone cannot make that credential safe. The scanner downloads a public binary; advisory audits submit only public package names/versions.

### Production mode and remaining operator responsibilities

Use `SPRING_PROFILES_ACTIVE=mysql,production` for a deployed installation. Startup refuses root/blank database users, blank/placeholder passwords, insecure cookies, demo seeding, preview storage and database URLs without exactly one sslMode=VERIFY_IDENTITY. Local preview and private local Compose remain HTTP-only on loopback for development.

With secure cookies enabled, plaintext requests receive 426 before creating a session; only GET/HEAD /api/health may pass for the container probe. HTTPS responses receive HSTS; all responses receive defensive browser headers. Sessions use cookies only, fixed path /, HttpOnly and SameSite=Strict. Session values in query strings cannot select a workspace.

Terminate HTTPS at a controlled reverse proxy or configure server TLS. Keep the application reachable only by that proxy. If the proxy terminates TLS, explicitly configure forwarded-header handling only after the proxy strips/replaces both Forwarded and X-Forwarded-* supplied by clients. Without that setup, the HTTPS guard deliberately refuses forwarded plaintext requests. See the [Spring proxy guidance](https://docs.spring.io/spring-security/reference/features/exploits/http.html).

Disk encryption, encrypted backups, database privileges/patching, trusted DB certificates, monitoring and edge rate limits must be configured on the deployment host. Local file permissions are not database encryption. Exported JSON is also private and is controlled by whoever downloads it. Avoid real sensitive records in this portfolio demonstration. Anyone who can use the same browser profile or steal its cookie can access its workspace; authenticated staff accounts and account recovery are outside this version's scope.
