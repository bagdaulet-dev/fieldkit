# Operations

## Native preview

Use JDK 21 or later, Node 24 and npm. The Maven wrapper pins Maven 3.9.16 and verifies its ZIP checksum; `unzip` is required. On this Mac, JDK 26 also successfully builds the Java 21 target. `scripts/preview.sh` builds the frontend and Java archive, then starts port 8082 with a bounded heap.

The launcher runs a snapshot at `.local/fieldkit-preview.jar` so later Maven builds do not replace the running archive. Preview data is in `.local/preview.mv.db`. The earlier development database, if present, is separate. Do not commit `.local/` or remove its files while the preview is running. Stop the server before starting another preview on the same database.

Set `MAVEN_REPO_LOCAL` only when you want an alternate Maven cache. The default uses Maven’s normal cache. Preview and MySQL data are independent; there is no automatic data transfer between them.

## MySQL stack

`python3 scripts/setup-env.py` writes a private `.env` with distinct random database passwords. It leaves an existing file unchanged. The example file contains placeholders. Compose reads values as configuration; no script sources them as shell code.

| Setting                                      | Purpose                                                                                |
| -------------------------------------------- | -------------------------------------------------------------------------------------- |
| APP_PORT                                     | Local Compose port, default 8082                                                       |
| MYSQL_DATABASE / MYSQL_USER / MYSQL_PASSWORD | Dedicated application database access                                                  |
| MYSQL_ROOT_PASSWORD                          | Database initialization only                                                           |
| MYSQL_SSL_MODE                               | DISABLED for the private local Docker network; use verified TLS for external databases |
| SECURE_COOKIES                               | False for local HTTP; true for public HTTPS                                            |
| DEMO_ENABLED                                 | Enable fictional data in an empty workspace                                            |
| APP_TIME_ZONE                                | Calendar-date rules, default Asia/Almaty                                               |

The app profile is explicitly MySQL in Compose. Liquibase applies versioned schema changes at startup, including vendor-specific Spring Session schemas. Startup migrations fit this single-instance deployment. Multiple replicas need a controlled release/migration job and shared edge rate limits.

The image uses a multi-stage frontend/Maven build and a Java 21 runtime. A small compiled Java readiness probe checks `/api/health`; no shell HTTP client is required in the runtime. The app and database each have a 512 MB container limit. Only temporary Tomcat files are writable in the app container.

## Disposable MySQL tests

`compose.test.yaml` has a separate project name, port 3311 and a temporary database directory. Its public credentials are for tests only. Use `TEST_STORAGE=mysql` and the exact test database settings from the README; do not run the test suite against a database with real records. Tests add and remove a temporary database constraint to verify rollback.

## Stop and preserve data

```sh
docker compose stop
docker compose -f compose.test.yaml down
```

The regular stack keeps its named database volume. The test database is temporary and disappears when its container is removed. Quit Docker Desktop when you want to release its VM resources. Avoid `docker compose down --volumes` unless erasing the regular database is intentional.

## CI and delivery

CI checks Java formatting, preview/MySQL API tests, file-database restart, frontend build, dependency audits, browser journeys and the container build. Actions are pinned to official release commits. Playwright downloads its pinned Chromium without repeating hosted Ubuntu’s system-package installation.

The separate manual workflow reruns verification and then publishes commit-tagged and `latest` images to GitHub Container Registry. It contains no server credentials and performs no remote deployment. The repository is [bagdaulet-dev/fieldkit](https://github.com/bagdaulet-dev/fieldkit). CI runs on pushes to main and pull requests. Image publication requires the separate manual workflow; no image or public website is deployed automatically.

## Troubleshooting

- Port 8082 occupied: stop the current native preview or set `APP_PORT=8083` for Docker.
- Browser cannot connect: inspect `/api/health` and the server/container startup log.
- CSRF rejected: reload to get the current token with the same cookie jar; a deleted/expired workspace starts a new session.
- Database passwords changed in `.env`: MySQL initialization variables affect a new volume only. Rotate existing users explicitly rather than deleting data.
- Record deletion returns 409: equipment/people with loan history are intentionally retained.
- Timeout while saving: refresh and check whether the write completed before retrying.

## Hardened deployment

`SPRING_PROFILES_ACTIVE=mysql,production` enables required secure cookies and disables demo seeding. Supply MYSQL_HOST, MYSQL_DATABASE, MYSQL_USER and MYSQL_PASSWORD through the host's secret manager or protected environment. The production profile requires verified MySQL TLS and a certificate matching MYSQL_HOST. It rejects unsafe configuration before creating application/database beans.

HTTPS must reach the application directly or through a trusted proxy configured as described in [Security](security.md). The local Compose file is intentionally loopback-only development configuration, not a public deployment manifest. Do not expose its HTTP port publicly. Secret scanning runs with `python3 scripts/check-secrets.py`; no Docker is needed.

The preview launcher restricts .local to 0700 and existing regular files to 0600 and starts Java under umask 077. These permissions protect data from other OS accounts; a stolen or unlocked OS/browser account needs separate protection. Keep FileVault or equivalent host encryption enabled and encrypt any backups.
