"""Create a private local .env once; never replace existing configuration."""

import os
import secrets
from pathlib import Path

root = Path(__file__).resolve().parent.parent
path = root / ".env"
content = (root / ".env.example").read_text()
for placeholder in [
    "change-me-run-python3-scripts-setup-env-py-before-docker",
    "change-me-generated-by-setup-env",
]:
    # Each password receives its own random value.
    while placeholder in content:
        content = content.replace(placeholder, secrets.token_urlsafe(48), 1)
try:
    with open(path, "x", opener=lambda name, flags: os.open(name, flags, 0o600)) as output:
        output.write(content)
except FileExistsError:
    raise SystemExit(".env already exists; left it unchanged.")
print("Created a private .env with unique local secrets. It is excluded from Git.")
