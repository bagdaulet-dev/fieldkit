# Verification record

Verified locally on **2026-10-28** using JDK 26.0.2.1 with a Java 21 compilation target, Node 24.21.0, Chrome, Docker, Java 21 in the container and MySQL 8.4.

| Check                               | Result                                                                      |
| ----------------------------------- | --------------------------------------------------------------------------- |
| Preview API and persistence suite   | 71 TestNG cases passed after security hardening                             |
| MySQL API and concurrency suite     | 47 TestNG cases passed before the security-only changes                     |
| Combined backend line coverage      | 92.0% after hardening; verification gate 85%                                |
| Real SQL insert failure             | Stock decrement rolled back; no loan inserted                               |
| Simultaneous last-unit checkouts    | One success, one stock conflict                                             |
| Simultaneous returns                | One success, one conflict; stock restored once                              |
| H2 file-database restart            | Inventory and the same browser session survived                             |
| MySQL application-container restart | Inventory, session, CSRF token and subsequent writes survived               |
| TypeScript / Vite production build  | Passed                                                                      |
| Java formatting / Prettier          | Passed                                                                      |
| OpenAPI 3.0 schema validation       | Passed against the official schema                                          |
| Maven dependency advisory scan      | 74 resolved packages checked; no known advisories returned                  |
| npm audit                           | 0 known vulnerabilities                                                     |
| Docker build, startup and health    | Passed with MySQL and the non-root runtime                                  |
| Native preview browser journey      | Passed                                                                      |
| Docker/MySQL browser journey        | Passed                                                                      |
| Responsive layouts                  | No horizontal overflow at 320, 390 and 768 pixels; desktop reviewed at 1440 |

The API tests cover owner isolation, CSRF, strict JSON types, unknown fields, query shapes, text/date/quantity validation, safe errors, stock edits, search/pagination, owned references, whole returns, private demos, export, deletion and relational history rules. The forced SQL failure intentionally produces a generic 503 and a sanitized exception-class log entry.

The browser journey creates equipment and a person, checks out the whole quantity, verifies zero availability, reads loan details, cancels and confirms a return, checks restored stock, edits quantity, searches, reloads, exports data and checks a separate empty workspace. It also verifies confirmed deletion of a disposable demo workspace.

The interface was reviewed from screenshots, including mobile illustration bounds, stable navigation names, modal focus restoration and reduced-motion rendering. Artwork uses native SVG; fonts are local assets with bundled licenses.

## Screenshots

- [Desktop overview](screenshots/overview-desktop.png)
- [Gear library](screenshots/library-desktop.png)
- [Loan detail](screenshots/loan-details.png)
- [Mobile overview](screenshots/overview-mobile.png)

## Limits

The source repository is [bagdaulet-dev/fieldkit](https://github.com/bagdaulet-dev/fieldkit); see its [verification workflow](https://github.com/bagdaulet-dev/fieldkit/actions/workflows/ci.yml) for remote results. Manual image publication remains opt-in. No public application deployment is configured. Advisory checks cover known records in the queried sources; they are not a complete security audit. Cleanup runs only while the application is running.

## Security hardening verification

The native suite now includes cross-workspace CSRF misuse, query-string session identifiers, blocked private-file/console URLs, no cross-origin response sharing, no-store on private responses, unsafe production settings and early rejection before application beans are created. HTTPS is exercised with a temporary trusted certificate: the session cookie is Secure/HttpOnly/SameSite=Strict and HSTS is present. Plain HTTP is rejected in secure mode, even with spoofed proxy headers, without creating a session. A mutation check inverted the HTTPS condition; both transport regressions failed, then the correct source was restored and the full suite passed.

The local Gitleaks scan found no secrets in publication candidates or the built client. A separate temporary Git repository verified that scanning works before the first commit, detects a synthetic token present only in the staged index, rejects a forcibly staged .env and redacts the canary from logs. The same scanner protects publication of this repository. .local permissions were checked as 0700; local data and .env files are 0600. Dependency checks again returned zero npm vulnerabilities and no advisories for 74 Maven packages.

Docker/MySQL were not restarted for this security-only pass; the earlier 47-case MySQL run remains the database-engine verification. The hardened build is exercised in the native preview. Public hosting, host/backup encryption, proxy configuration and staff-account authentication require separate deployment work as described in security.md.
