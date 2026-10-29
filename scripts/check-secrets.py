#!/usr/bin/env python3
"""Scan publishable files, the built client and Git history without disclosing matches.

Downloads only the pinned official Gitleaks binary and checks its SHA-256 before
execution. Scanning is local: no source, credentials or records are uploaded.
"""
import fnmatch
import hashlib
from pathlib import Path
import platform
import shutil
import subprocess
import tarfile
import tempfile
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
VERSION = "8.30.1"
CHECKSUMS = {
    ("Darwin", "arm64"): "b40ab0ae55c505963e365f271a8d3846efbc170aa17f2607f13df610a9aeb6a5",
    ("Darwin", "x64"): "dfe101a4db2255fc85120ac7f3d25e4342c3c20cf749f2c20a18081af1952709",
    ("Linux", "arm64"): "e4a487ee7ccd7d3a7f7ec08657610aa3606637dab924210b3aee62570fb4b080",
    ("Linux", "x64"): "551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb",
}
PRIVATE = ("*.pem", "*.key", "*.p12", "*.pfx", "*.jks", "*.keystore", "*.mv.db",
           "*.trace.db", "*.sqlite", "*.sqlite3", "*.dump", "*.bak", "*.sql.gz", "*.sql.zip", "credentials.json",
           "secrets.json", "id_rsa*", "id_ed25519*")


def git(*args):
    return subprocess.run(["git", "-C", str(ROOT), *args], capture_output=True, check=True).stdout


def private(path):
    parts = path.parts
    return any(part in {".local", ".aws", ".ssh", "backups", "exports"} for part in parts) or any(
        (part.startswith(".env") and part != ".env.example")
        or any(fnmatch.fnmatch(part.lower(), pattern) for pattern in PRIVATE) for part in parts)


def main():
    machine = {"aarch64": "arm64", "arm64": "arm64", "x86_64": "x64", "AMD64": "x64"}.get(platform.machine())
    system = platform.system()
    checksum = CHECKSUMS.get((system, machine))
    if not checksum:
        raise SystemExit("Scanner supports macOS/Linux on arm64/x64; use official Gitleaks on other platforms.")
    with tempfile.TemporaryDirectory(prefix="fieldkit-secrets-") as folder:
        temp = Path(folder)
        try:
            own_repo = Path(git("rev-parse", "--show-toplevel").decode().strip()).resolve() == ROOT
        except subprocess.CalledProcessError:
            own_repo = False
        if own_repo:
            files = git("ls-files", "--cached", "--others", "--exclude-standard", "-z")
        else:
            repo = temp / "repository"
            subprocess.run(["git", "init", "-q", str(repo)], check=True)
            files = subprocess.check_output(["git", "--git-dir=" + str(repo / ".git"),
                "--work-tree=" + str(ROOT), "ls-files", "--others", "--exclude-standard", "-z"])
        staged = set(git("ls-files", "-z").decode().split("\0")) if own_repo else set()
        has_history = own_repo and subprocess.run(["git", "-C", str(ROOT), "rev-parse", "--verify", "HEAD"],
            capture_output=True).returncode == 0
        snapshot = temp / "publishable"
        snapshot.mkdir()
        for name in files.decode().split("\0"):
            if not name:
                continue
            relative = Path(name)
            if private(relative):
                raise SystemExit("Private file would be published: " + name)
            if name in staged:
                staged_target = snapshot / "staged-index" / relative
                staged_target.parent.mkdir(parents=True, exist_ok=True)
                staged_target.write_bytes(git("show", ":" + name))
            source = ROOT / relative
            if not source.exists():
                continue  # A tracked deletion is safe.
            if source.is_symlink() or not source.is_file() or not source.resolve().is_relative_to(ROOT):
                raise SystemExit("Review unsupported publishable path: " + name)
            target = snapshot / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
        if (ROOT / "frontend/dist").is_dir():
            shutil.copytree(ROOT / "frontend/dist", snapshot / "compiled-client")
        asset = f"gitleaks_{VERSION}_{system.lower()}_{machine}.tar.gz"
        url = f"https://github.com/gitleaks/gitleaks/releases/download/v{VERSION}/{asset}"
        archive = temp / asset
        with urllib.request.urlopen(url, timeout=45) as response:
            archive.write_bytes(response.read())
        if hashlib.sha256(archive.read_bytes()).hexdigest() != checksum:
            raise SystemExit("Scanner checksum mismatch; refused to execute.")
        binary = temp / "gitleaks"
        with tarfile.open(archive) as package:
            member = package.getmember("gitleaks")
            if not member.isfile():
                raise SystemExit("Invalid scanner archive.")
            with package.extractfile(member) as source:
                binary.write_bytes(source.read())
        binary.chmod(0o700)
        flags = ["--redact", "--no-banner", "--no-color", "--ignore-gitleaks-allow",
                 "--config", str(ROOT / ".gitleaks.toml")]
        subprocess.run([str(binary), "dir", *flags, str(snapshot)], check=True)
        if has_history:
            subprocess.run([str(binary), "git", *flags, "--log-opts=--all", str(ROOT)], check=True)
        print("Secret checks passed: publishable files and built client" + (", staged index and Git history." if has_history else ". No Git history exists yet."))


if __name__ == "__main__":
    main()
