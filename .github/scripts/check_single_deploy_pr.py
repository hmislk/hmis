#!/usr/bin/env python3
"""
Single deploy PR guard.

Allows one open PR per deploy branch (`<hospital>-production` /
`<hospital>-staging`). Parallel PRs against the same deploy branch can
conflict, deploy in an unpredictable order, and each merge triggers its own
production deployment. A second fix belongs on the open PR's head branch.

The oldest open PR (lowest number) per base passes; every newer one fails.

Usage:
  check_single_deploy_pr.py --pr <number> --base <branch>
      Exit 1 when an older PR to <branch> is still open (PR check).
  check_single_deploy_pr.py --sweep [--dry-run]
      Set the 'single-deploy-pr' commit status on the head of every open PR
      to a deploy branch (scheduled sweep). Exit 0.

Needs GITHUB_REPOSITORY and GH_TOKEN (or GITHUB_TOKEN) in the environment.
"""
import argparse
import json
import os
import re
import sys
import urllib.request

DEPLOY_BRANCH = re.compile(r"-(production|staging)$")
CONTEXT = "single-deploy-pr"
API = "https://api.github.com"


def api(method, path, body=None):
    token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
    req = urllib.request.Request(API + path, method=method,
                                 data=json.dumps(body).encode() if body is not None else None)
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("X-GitHub-Api-Version", "2022-11-28")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read() or "null")


def open_prs(repo, base=None):
    prs, page = [], 1
    while True:
        query = "state=open&per_page=100&page=%d" % page
        if base:
            query += "&base=" + base
        batch = api("GET", "/repos/%s/pulls?%s" % (repo, query))
        prs += batch
        if len(batch) < 100:
            return prs
        page += 1


def describe(pr):
    return "#%d (%s, by %s)" % (pr["number"], pr["head"]["ref"], pr["user"]["login"])


def check_pr(repo, number, base):
    if not DEPLOY_BRANCH.search(base):
        print("%s is not a deploy branch; nothing to check." % base)
        return 0
    older = sorted((p for p in open_prs(repo, base) if p["number"] < number),
                   key=lambda p: p["number"])
    if not older:
        print("✅ #%d is the only open PR to %s." % (number, base))
        return 0
    first = older[0]
    print("::error::%s already has an open PR: %s. Only one PR per deploy branch may be open. "
          "Add your commits to %s's branch '%s' and update its title/body to list every fix, "
          "then close this PR (see the hotfix-deploy skill, Step 1a). "
          "If %s has since been merged or closed, re-run this check."
          % (base, describe(first), "#%d" % first["number"], first["head"]["ref"], "#%d" % first["number"]))
    for p in older[1:]:
        print("Also open on %s: %s" % (base, describe(p)))
    return 1


def current_state(repo, sha):
    statuses = api("GET", "/repos/%s/commits/%s/statuses?per_page=100" % (repo, sha))
    for s in statuses:  # newest first
        if s["context"] == CONTEXT:
            return s["state"], s["description"]
    return None, None


def sweep(repo, dry_run):
    by_base = {}
    for p in open_prs(repo):
        base = p["base"]["ref"]
        if DEPLOY_BRANCH.search(base):
            by_base.setdefault(base, []).append(p)
    # A status belongs to a commit, not a PR, and one head commit can be open
    # as PRs to several deploy branches. Work out one result per commit, with a
    # duplicate on any branch winning, so the PRs do not overwrite each other.
    # The context stays fixed so it can later be made a required check.
    by_sha = {}
    for base, prs in sorted(by_base.items()):
        prs.sort(key=lambda p: p["number"])
        first = prs[0]
        for p in prs:
            if p is first:
                result = ("success", "Only open PR to %s" % base, first)
            else:
                result = ("failure", "#%d is already open on %s - add these commits to its branch"
                          % (first["number"], base), first)
            sha = p["head"]["sha"]
            entry = by_sha.setdefault(sha, {"prs": [], "result": result, "bases": []})
            entry["prs"].append(p)
            if result[0] == "success":
                entry["bases"].append(base)
            if entry["result"][0] == "success" and result[0] == "failure":
                entry["result"] = result
    for entry in by_sha.values():
        if entry["result"][0] == "success":
            entry["result"] = ("success", "Only open PR to %s" % ", ".join(entry["bases"]), entry["result"][2])
    for sha, entry in by_sha.items():
        state, desc, target = entry["result"]
        desc = desc[:140]
        if current_state(repo, sha) == (state, desc):
            continue
        print("%s %s: %s" % (", ".join(describe(p) for p in entry["prs"]), state, desc))
        if not dry_run:
            api("POST", "/repos/%s/statuses/%s" % (repo, sha), {
                "state": state,
                "context": CONTEXT,
                "description": desc,
                "target_url": "https://github.com/%s/pull/%d" % (repo, target["number"]),
            })
    return 0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--pr", type=int)
    ap.add_argument("--base")
    ap.add_argument("--sweep", action="store_true")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    repo = os.environ["GITHUB_REPOSITORY"]
    if args.sweep:
        return sweep(repo, args.dry_run)
    if args.pr is None or not args.base:
        ap.error("--pr and --base are required unless --sweep is given")
    return check_pr(repo, args.pr, args.base)


if __name__ == "__main__":
    sys.exit(main())
