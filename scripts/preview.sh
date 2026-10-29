#!/bin/sh
set -eu
# Database, session and temporary files belong only to the current OS account.
umask 077
cd "$(dirname "$0")/.."
if command -v lsof >/dev/null 2>&1 && lsof -t -iTCP:"${PORT:-8082}" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Stop the existing server on port ${PORT:-8082} before starting another preview." >&2
  exit 1
fi
mkdir -p .local
chmod 700 .local
find .local -type f -exec chmod 600 {} \;
npm ci --ignore-scripts
npm run build
if [ -n "${MAVEN_REPO_LOCAL:-}" ]; then
  ./mvnw --batch-mode -q "-Dmaven.repo.local=$MAVEN_REPO_LOCAL" -DskipTests package
else
  ./mvnw --batch-mode -q -DskipTests package
fi
cp target/fieldkit.jar .local/fieldkit-preview.jar
exec java -Xms64m -Xmx256m -jar .local/fieldkit-preview.jar --spring.profiles.active=preview --debug=false
