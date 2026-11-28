#!/usr/bin/env python3
"""PreToolUse hook (Bash): refuse a few dangerous commands in Claude sessions.

Refused:
  1. gh pr merge, or gh api .../pulls/N/merge and .../merges, when the PR base is
     master, staging, production, or ends in -production / -staging (-prod, -stg).
     The base comes from `gh pr view --json baseRefName` (gh api for the API path).
     If the base cannot be read (network, auth, parse), the merge is refused too
     (fail closed). A graphql mergePullRequest / auto-merge mutation is refused.
  2. git add -A, --all, . (also with git -C, inside && ; | $( ) and bash -c).
  3. git push --force / -f / --force-with-lease / --force-if-includes / +refspec.
     Always refused.
  4. ssh / scp / sftp / rsync commands that read the Payara DB pool settings:
     asadmin get/list-jdbc* on property/password/jdbc-connection-pool, or any
     use of domain.xml. Ordinary ssh is not touched.

Deny = exit 2 with the reason on stderr (same as public-guard.py). The message
never echoes the command. A failed merge-base lookup is refused (fail closed).

Applies to Claude Code Bash and PowerShell calls.
Env for tests: HMIS_SESSION_GUARD_GH = gh binary or command line (default gh).
Tests: .claude/hooks/test_session_guard.py. Policy: developer_docs/rules/git.md
"""
import json
import os
import re
import shlex
import shutil
import subprocess
import sys

DEPLOY_BASE = re.compile(r"^(?:master|staging|production|.+-(?:production|staging|prod|stg))$")
WRAPPERS = {"sudo", "env", "command", "exec", "time", "nice", "nohup", "builtin", "xargs"}
WRAPPER_ARG_OPTS = {"sudo": {"-u", "-g", "-C", "-h", "-p", "-r", "-t", "-U"}, "env": {"-u", "-C"},
                    "nice": {"-n"}, "xargs": {"-I", "-n", "-P", "-L", "-d", "-s"}}
SHELLS = {"bash", "sh", "zsh", "dash", "ksh"}
PS_SHELLS = {"pwsh", "powershell"}
EVALS = {"eval", "iex", "invoke-expression"}
REMOTE = {"ssh", "scp", "sftp", "rsync"}
GIT_OPT_ARG = {"-C", "-c", "--git-dir", "--work-tree", "--namespace", "--exec-path", "--super-prefix"}
PUNCT = set(";&|()<>")
ASSIGN = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*=")

MSG_MERGE = ("Merges to deploy branches (master, *-production, *-staging) are the user's decision. "
             "Do not merge them from a session; ask the user, or use the scheduled-merge skill. "
             "Merges into development follow the merge gate.")
MSG_BASE = ("session-guard hook: could not check the base branch of this merge (lookup failed), "
            "so it is refused. Retry the command. If it keeps failing, check gh auth and network.")
MSG_NO_GH = "session-guard hook: gh not found; merges cannot be checked, so this merge is refused. Install gh or fix PATH."
MSG_ADD = ("session-guard hook: `git add -A`, `--all` and `.` are refused. "
           "Stage explicit paths: `git add path/one path/two`. See developer_docs/rules/git.md.")
MSG_ALIAS = "session-guard hook: git -c alias.* is refused. Use the git command itself."
MSG_FORCE = ("session-guard hook: force push is refused. Push normally, or ask the user.")
MSG_SSH = ("session-guard hook: reading app-server DB pool settings over ssh is refused. "
           "Do not read domain.xml or asadmin pool/property/password values. Ask the user.")


class Deny(Exception):
    pass


def tokenize(cmd, ps=False):
    """Return a list of segments, each a list of tokens. Raises ValueError if unparsable.
    ps=True reads PowerShell syntax: the backtick is an escape, not command substitution."""
    cmd = cmd.replace("\\\n", " ")
    if ps:
        cmd = re.sub(r"`(.)", r"\1", cmd, flags=re.S)
        cmd = cmd.replace("$(", " ; ( ")
    else:
        cmd = cmd.replace("$(", " ; ( ").replace("`", " ; ").replace("<(", " ; ( ")
    lex = shlex.shlex(cmd, posix=True, punctuation_chars=True)
    lex.whitespace_split = True
    lex.commenters = ""
    segs, cur = [], []
    for tok in lex:
        if tok and set(tok) <= PUNCT:
            if cur:
                segs.append(cur)
            cur = []
        else:
            cur.append(tok)
    if cur:
        segs.append(cur)
    return segs


def split_prefix(seg):
    """Strip env assignments and wrappers. Return (env dict, tokens)."""
    env, i = {}, 0
    while i < len(seg):
        t = seg[i]
        if ASSIGN.match(t):
            k, _, v = t.partition("=")
            env[k] = v
            i += 1
        elif os.path.basename(t) in WRAPPERS:
            w = os.path.basename(t)
            i += 1
            while i < len(seg) and seg[i].startswith("-"):
                i += 2 if seg[i] in WRAPPER_ARG_OPTS.get(w, ()) else 1
            while w == "env" and i < len(seg) and ASSIGN.match(seg[i]):
                k, _, v = seg[i].partition("=")
                env[k] = v
                i += 1
        else:
            break
    return env, seg[i:]


def git_parts(args):
    """args after `git`. Return (subcommand, rest)."""
    i = 0
    while i < len(args) and args[i].startswith("-"):
        if args[i] == "-c" and i + 1 < len(args) and args[i + 1].lower().startswith("alias."):
            raise Deny(MSG_ALIAS)
        if args[i].lower().startswith("-calias."):
            raise Deny(MSG_ALIAS)
        i += 2 if args[i] in GIT_OPT_ARG else 1
    if i >= len(args):
        return None, []
    return args[i], args[i + 1:]


def check_add(rest):
    for a in rest:
        if a in ("--all", ".", "./", "-A", "*", "./*", ":/", ":/*", ":/."):
            raise Deny(MSG_ADD)
        if a.startswith("-") and not a.startswith("--") and "A" in a:
            raise Deny(MSG_ADD)


def check_push(rest):
    positional = []
    for a in rest:
        if a in ("-f", "--force", "--force-if-includes") or a.startswith("--force-with-lease"):
            raise Deny(MSG_FORCE)
        if a.startswith("-") and not a.startswith("--") and re.match(r"^-[A-Za-z]+$", a) and "f" in a[1:]:
            raise Deny(MSG_FORCE)
        if not a.startswith("-"):
            positional.append(a)
    # positional[0] is the remote; the rest are refspecs
    if any(r.startswith("+") for r in positional[1:]):
        raise Deny(MSG_FORCE)


def gh_command():
    """argv prefix for gh. The env value may be a command line (tests use it).
    shutil.which honours PATHEXT on Windows, so gh.exe / gh.cmd are found."""
    raw = os.environ.get("HMIS_SESSION_GUARD_GH", "gh")
    try:
        parts = shlex.split(raw, posix=(os.name != "nt"))
    except ValueError:
        parts = []
    parts = [p[1:-1] if len(p) > 1 and p[0] == p[-1] and p[0] in "\"'" else p for p in parts]
    if not parts:
        return None
    exe = shutil.which(parts[0])
    if exe is None:
        return None
    return [exe] + parts[1:]


def run_gh(args, env, cwd):
    cmd = gh_command()
    if cmd is None:
        raise Deny(MSG_NO_GH)
    full = dict(os.environ)
    full.update(env)
    try:
        out = subprocess.run(cmd + args, capture_output=True, text=True, timeout=12, env=full, cwd=cwd)
    except Exception:
        return None
    if out.returncode != 0:
        return None
    val = out.stdout.strip()
    return val or None


def check_base(base):
    if base is None:
        raise Deny(MSG_BASE)
    if DEPLOY_BASE.match(base.strip()):
        raise Deny(MSG_MERGE)


def check_pr_merge(rest, env, cwd):
    with_val = {"-R", "--repo", "-b", "--body", "-t", "--subject", "-F", "--body-file", "-A",
                "--author-email", "--match-head-commit"}
    repo, pr, i = None, None, 0
    while i < len(rest):
        a = rest[i]
        if a in ("-R", "--repo") and i + 1 < len(rest):
            repo = rest[i + 1]
            i += 2
        elif a.startswith("--repo="):
            repo = a.split("=", 1)[1]
            i += 1
        elif a.startswith("-R") and len(a) > 2:
            repo = a[2:]
            i += 1
        elif a in with_val:
            i += 2
        elif a.startswith("-"):
            i += 1
        else:
            pr = pr or a
            i += 1
    args = ["pr", "view"] + ([pr] if pr else []) + ["--json", "baseRefName", "-q", ".baseRefName"]
    if repo:
        args += ["-R", repo]
    check_base(run_gh(args, env, cwd))


def api_field(rest, name):
    for i, a in enumerate(rest):
        for pre in ("-f", "-F", "--field", "--raw-field"):
            val = None
            if a == pre and i + 1 < len(rest):
                val = rest[i + 1]
            elif a.startswith(pre + "="):
                val = a.split("=", 1)[1]
            elif pre in ("-f", "-F") and a.startswith(pre) and len(a) > 2 and a[2] != "-":
                val = a[2:]
            if val and val.startswith(name + "="):
                return val.split("=", 1)[1]
    return None


def check_api(rest, env, cwd):
    joined = " ".join(rest)
    if re.search(r"mergePullRequest|enablePullRequestAutoMerge", joined):
        raise Deny(MSG_MERGE)
    paths = [a for a in rest if re.search(r"(?:^|/)repos/", a) or "pulls/" in a or a.endswith("/merges")]
    for p in paths:
        p = re.sub(r"^https?://[^/]+/(?:api/v3/)?", "", p).lstrip("/").split("?")[0]
        m = re.match(r"^(repos/[^/]+/[^/]+)/pulls/(\d+)/merge$", p)
        if m:
            args = ["api", "%s/pulls/%s" % (m.group(1), m.group(2)), "--jq", ".base.ref"]
            check_base(run_gh(args, env, cwd))
            return
        if re.match(r"^repos/[^/]+/[^/]+/merges$", p):
            base = api_field(rest, "base")
            if base is None and "--input" in rest:
                try:
                    f = rest[rest.index("--input") + 1]
                    base = json.load(sys.stdin if f == "-" else open(f)).get("base")
                except Exception:
                    base = None
            check_base(base)
            return


def check_remote(raw):
    low = raw
    if re.search(r"domain\.xml", low, re.I):
        raise Deny(MSG_SSH)
    if re.search(r"\basadmin\b", low) and \
            re.search(r"(?<![\w-])(?:get|list-jdbc[\w-]*|list-password[\w-]*|get-[\w-]*)(?![\w-])", low) and \
            re.search(r"propert|password|jdbc-connection-pool|jdbc-resource", low, re.I):
        raise Deny(MSG_SSH)


def check_segment(seg, raw, cwd, depth, ps=False):
    env, toks = split_prefix(seg)
    if not toks:
        return
    prog = re.sub(r"\.(?:exe|cmd|bat)$", "", os.path.basename(toks[0].replace("\\", "/")).lower())
    rest = toks[1:]
    if prog in SHELLS:
        for i, a in enumerate(rest):
            if re.match(r"^-[a-z]*c[a-z]*$", a) and i + 1 < len(rest):
                analyse(rest[i + 1], cwd, depth + 1)
                break
    elif prog in PS_SHELLS:
        for i, a in enumerate(rest):
            if re.match(r"^-(?:c|co|com|comm|comma|comman|command)$", a, re.I) and i + 1 < len(rest):
                analyse(" ".join(rest[i + 1:]), cwd, depth + 1, ps=True)
                break
    elif prog == "cmd":
        for i, a in enumerate(rest):
            if a.lower() in ("/c", "/k", "/r") and i + 1 < len(rest):
                analyse(" ".join(rest[i + 1:]), cwd, depth + 1)
                break
    elif prog in EVALS:
        analyse(" ".join(rest), cwd, depth + 1, ps=ps)
    elif prog == "git":
        sub, args = git_parts(rest)
        if sub == "add":
            check_add(args)
        elif sub == "push":
            check_push(args)
    elif prog == "gh":
        if rest[:2] == ["pr", "merge"]:
            check_pr_merge(rest[2:], env, cwd)
        elif rest[:1] == ["api"]:
            check_api(rest[1:], env, cwd)
    elif prog in REMOTE:
        check_remote(raw)


def raw_fallback(cmd):
    """Used only when shlex cannot parse the command."""
    if re.search(r"\bgit\b[^\n;&|]*\badd\s+(?:[^\n;&|]*\s)?(?:-[A-Za-z]*A[A-Za-z]*|--all|\./?)(?=\s|$|[;&|)\"'])", cmd):
        raise Deny(MSG_ADD)
    if re.search(r"\bgit\b[^\n;&|]*\bpush\b[^\n;&|]*(?:\s-[A-Za-z]*f|\s--force|\s\+\S)", cmd):
        raise Deny(MSG_FORCE)
    if re.search(r"\bgh\s+pr\s+merge\b", cmd) or re.search(r"\bgh\s+api\b[^\n;&|]*/merges?\b", cmd):
        raise Deny(MSG_BASE)
    if re.search(r"\b(?:ssh|scp|sftp|rsync)\b", cmd):
        check_remote(cmd)


def analyse(cmd, cwd, depth=0, ps=False):
    if depth > 4:
        return
    try:
        segs = tokenize(cmd, ps)
    except ValueError:
        raw_fallback(cmd)
        return
    for seg in segs:
        check_segment(seg, cmd, cwd, depth, ps)


def main():
    try:
        data = json.load(sys.stdin)
    except ValueError:
        return 0
    tool = data.get("tool_name")
    if tool not in ("Bash", "PowerShell"):
        return 0
    command = (data.get("tool_input") or {}).get("command") or ""
    cwd = data.get("cwd") or os.getcwd()
    if not re.search(r"\b(?:git|gh|ssh|scp|sftp|rsync|eval|iex|Invoke-Expression)\b", command):
        return 0
    try:
        analyse(command, cwd, ps=(tool == "PowerShell"))
    except Deny as d:
        sys.stderr.write(str(d) + "\n")
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
