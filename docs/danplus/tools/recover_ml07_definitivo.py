"""Verify the ML07 bundle and restore it in a fresh bare repository and optional checkout."""

import argparse
import hashlib
from pathlib import Path
import subprocess

BASE = "5cb4f2d9f9af6ffcee077d0385cf7bd9f9909d7d"
BRANCH = "refs/heads/feat/danplus-2.1.2-homologacao-20261127"
RECOVERED = "refs/heads/recuperado"


def git(*args, repo=None, **kwargs):
    command = ["git", "-C", str(repo)] if repo else ["git"]
    return subprocess.run(command + [str(arg) for arg in args], check=True, **kwargs)


def resolve(repo, revision):
    return git("rev-parse", revision, repo=repo, capture_output=True, text=True).stdout.strip()


def recover(bundle, checksum, source, target, checkout):
    with bundle.open("rb") as file:
        actual = hashlib.file_digest(file, "sha256").hexdigest()
    expected = checksum.read_text(encoding="utf-8-sig").split()[0].casefold()
    if actual != expected:
        raise ValueError("Bundle SHA-256 mismatch")
    for destination in (target, checkout):
        if destination and (destination.exists() or destination.is_symlink()):
            raise ValueError(f"Destination already exists: {destination}")
    git("cat-file", "-e", f"{BASE}^{{tree}}", repo=source)
    git("init", "--bare", target)
    git("pack-objects", "--revs", target / "objects/pack/base", repo=source, input=(BASE + "\n").encode())
    (target / "shallow").write_bytes((BASE + "\n").encode())
    git("update-ref", "refs/heads/base", BASE, repo=target)
    git("bundle", "verify", bundle, repo=target)
    git("fetch", bundle, f"{BRANCH}:{RECOVERED}", repo=target)
    git("symbolic-ref", "HEAD", RECOVERED, repo=target)
    git("fsck", "--full", "--no-reflogs", repo=target)
    print("HEAD", resolve(target, "HEAD"))
    print("TREE", resolve(target, "HEAD^{tree}"))
    if checkout:
        git("worktree", "add", "--detach", checkout, "HEAD", repo=target)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--bundle", type=Path, required=True)
    parser.add_argument("--checksum", type=Path, required=True)
    parser.add_argument("--base-repo", type=Path, required=True, help="Validated ML07 recovery repo containing the base")
    parser.add_argument("--new-bare", type=Path, required=True)
    parser.add_argument("--checkout", type=Path)
    args = parser.parse_args()
    recover(args.bundle.resolve(), args.checksum.resolve(), args.base_repo.resolve(),
            args.new_bare.absolute(), args.checkout.absolute() if args.checkout else None)
