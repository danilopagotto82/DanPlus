"""Read public upstream metadata. No checkout, code execution, or writes to GitHub."""

import argparse
import json
import re
import subprocess
from pathlib import Path

REPOSITORIES = ("NuvioMedia/NuvioTV", "ysosrs123/NuvioTV-Fork", "abbaskip/Nuvio-ReSeq")
RISK_PATHS = {
    "player": r"player|media3|mpv|ffmpeg|debrid",
    "security": r"auth|token|secret|keystore|signing|supabase",
    "build": r"gradle|workflow|manifest|jni|cmake",
    "data-sync": r"repository|datastore|backup|sync|tracking",
    "interface": r"/ui/|/res/",
}


def github(path, paginate=False):
    command = ["gh", "api", path]
    if paginate:
        command.extend(("--paginate", "--slurp"))
    return json.loads(subprocess.check_output(command, timeout=120))


def risk_areas(paths):
    return sorted(name for name, pattern in RISK_PATHS.items()
                  if any(re.search(pattern, path, re.IGNORECASE) for path in paths))


def inventory(repo, reviewed, api=github):
    head = api(f"repos/{repo}/commits/HEAD")["sha"]
    base = reviewed["commit"]
    if not re.fullmatch(r"[0-9a-f]{40}", base):
        raise ValueError(f"{repo}: reviewed commit must be a full SHA")
    commits, files = {}, {}
    complete = True
    if base != head:
        pages = api(f"repos/{repo}/compare/{base}...{head}?per_page=100", paginate=True)
        first = pages[0]
        if first["status"] != "ahead":
            raise ValueError(f"{repo}: reviewed base is no longer an ancestor")
        for page in pages:
            for commit in page["commits"]:
                commits[commit["sha"]] = commit["commit"]["message"].splitlines()[0]
        if len(commits) != first["total_commits"]:
            raise ValueError(f"{repo}: incomplete commit range")
        for entry in first.get("files", []):
            files[entry["filename"]] = entry["status"]
        complete = len(files) < 300
    reviews = set(reviewed["releases"])
    releases = {}
    for page in api(f"repos/{repo}/releases?per_page=100", paginate=True):
        for item in page:
            if not item["draft"] and item["id"] not in reviews:
                releases[item["id"]] = {"tag": item["tag_name"], "prerelease": item["prerelease"]}
    return {
        "reviewed": base, "candidate_head": head, "commits": commits, "releases": releases,
        "files": files, "file_list_may_be_truncated": not complete, "risk_areas": risk_areas(files),
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("ledger", type=Path)
    parser.add_argument("--include-reseq", action="store_true")
    args = parser.parse_args()
    ledger = json.loads(args.ledger.read_text(encoding="utf-8"))
    repositories = REPOSITORIES if args.include_reseq else REPOSITORIES[:2]
    print(json.dumps({repo: inventory(repo, ledger[repo]) for repo in repositories}, indent=2))
