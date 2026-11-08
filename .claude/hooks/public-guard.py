#!/usr/bin/env python3
"""PreToolUse hook: regex scan of text about to be posted to the public repo.

Runs on Bash commands that can carry public text:
  gh pr     create|edit|comment|review|close|merge|reopen   (-b/--body, -c/--comment,
            -t/--title/--subject, --body-file/-F)
  gh issue  create|edit|comment|close|reopen
  gh release create|edit (-n/--notes, -F/--notes-file), gh gist create|edit (file args)
  gh label|repo create|edit (--description)
  gh api    with -f/-F/--field/--raw-field/--input (comment, reply, review, issue and
            pull bodies, graphql mutations)
  git commit|tag|notes|merge (-m/--message, -F/--file), with git -C/-c options
Short, long and --flag=value forms all match, since the whole command is scanned.
It also scans the files named by --body-file, --notes-file, --file, --input, -F
(gh api key=@file) and gh gist file arguments. A high-confidence hit blocks the
command (exit 2) and asks for the hmis-public-guard subagent. The regexes are a
backstop, not the judge.

It fails open: if no python runs, or the input is not hook JSON, the command
goes through. Tests: .claude/hooks/test_public_guard.py.

Bypass for one command, after hmis-public-guard returned PASS or a human checked
the text: prefix the command with HMIS_PUBLIC_GUARD_BYPASS=1. It counts only as
an env assignment in front of a command, not inside quoted text. Anyone can type
it, so it is a speed bump, not enforcement.

Machine-specific patterns (real hostnames, which must never be committed) may be
added one regex per line in the file named by HMIS_PUBLIC_GUARD_EXTRA, default
~/.config/hmis-public-guard/patterns.txt. Policy:
developer_docs/git/github-public-content-policy.md
"""
import json
import os
import re
import shlex
import sys

BYPASS = "HMIS_PUBLIC_GUARD_BYPASS"

GIT = r"\bgit(?:\s+(?:-[Cc]\s+\S+|--[\w-]+(?:=\S+)?))*\s+"
POSTING = [
    re.compile(r"\bgh\s+pr\s+(?:create|edit|comment|review|close|merge|reopen)\b"),
    re.compile(r"\bgh\s+issue\s+(?:create|edit|comment|close|reopen)\b"),
    re.compile(r"\bgh\s+(?:release|gist|label|repo)\s+(?:create|edit)\b"),
    re.compile(r"\bgh\s+api\b[^\n;|&]*\s(?:-[fF]|--field|--raw-field|--input)(?![\w-])"),
    re.compile(GIT + r"(?:commit|tag|notes|merge)\b"),
]

# File arguments whose content is the posted text.
FILE_ARGS = re.compile(
    r"(?<![\w-])(?:--body-file|--notes-file|--file|--input|-[a-zA-Z]*F)(?:\s+|=)"
    r"(?:body=)?@?(\"[^\"]+\"|'[^']+'|[^\s;|&]+)")
GIST = re.compile(r"\bgh\s+gist\s+(?:create|edit)\b")
SHELL_OPS = set("();<>|&")

OCTET = r"(?:25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)"
IPV4 = re.compile(r"(?<![\w.])(" + OCTET + r"(?:\." + OCTET + r"){3})(?![\w.])")
IP_ALLOWED = re.compile(r"^(127\.|0\.0\.0\.0$|255\.|192\.0\.2\.|198\.51\.100\.|203\.0\.113\.)")

HOSPITALS = r"(?:ruhunu|coop|rmh|sethma|southern\s*lanka|southernlanka)"
COUNTED = (r"\d{1,3}(?:,\d{3})+|\d{2,}")
NOUNS = (r"(?:bills?|patients?|records?|rows?|admissions?|bhts?|invoices?|"
         r"grns?|staff|users?|visits?|sales|transactions?)")

CHECKS = [
    ("private key", re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----")),
    ("GitHub token", re.compile(r"\b(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{30,})")),
    ("Anthropic/OpenAI key", re.compile(r"\bsk-(?:ant-)?[A-Za-z0-9_-]{24,}")),
    ("AWS key", re.compile(r"\bAKIA[0-9A-Z]{16}\b")),
    ("Slack token", re.compile(r"\bxox[abprs]-[A-Za-z0-9-]{10,}")),
    ("password assignment", re.compile(
        r"(?i)\b(?:password|passwd|pwd)\s*[:=]\s*['\"]?(?![$<{*.\s'\"]|xxx|\[)[^\s'\"<>]{4,}")),
    ("secret/api key assignment", re.compile(
        r"(?i)\b(?:api[_-]?key|secret|access[_-]?token|auth[_-]?token)\s*[:=]\s*['\"]?"
        r"(?=[A-Za-z0-9+/_=-]*\d)(?=[A-Za-z0-9+/_=-]*[A-Za-z])[A-Za-z0-9+/_=-]{20,}")),
    ("mysql inline password", re.compile(r"\bmysql\b[^\n]*\s-p(?!\s)[^\s'\"]+")),
    ("JDBC URL with host", re.compile(r"(?i)jdbc:mysql://(?!localhost\b|127\.0\.0\.1\b|<|\$\{)[\w.-]+")),
    ("SSH user@host", re.compile(r"\bssh\s+(?:-\S+\s+)*[\w.-]+@[\w.-]+")),
    ("private hostname", re.compile(
        r"(?i)\b[\w-]+\.(?:internal|lan|corp|intranet)\b|\b[\w-]+\.cloudapp\.azure\.com\b")),
    ("BHT number", re.compile(r"\bBHT\s*(?:/|#|no\.?\s*|number\s*)[:#]?\s*\d{3,}")),
    ("PHN number", re.compile(r"\bPHN\s*(?:/|#|no\.?\s*|number\s*)?[:#]?\s*\d{5,}")),
    ("bill-number-like code", re.compile(r"\b[A-Z]{2,6}/[A-Z]{2,10}/\d{4,}\b|\b[A-Z]{2,6}/\d{2}/\d{4,}\b")),
    ("count tied to a hospital", re.compile(
        r"(?i)\b" + HOSPITALS + r"\b[^\n.]{0,60}?\b(?:" + COUNTED + r")\s+" + NOUNS + r"\b"
        r"|\b(?:" + COUNTED + r")\s+" + NOUNS + r"\b[^\n.]{0,40}?\b(?:at|in|for|from)\s+" + HOSPITALS + r"\b")),
]


def extra_checks():
    path = os.environ.get("HMIS_PUBLIC_GUARD_EXTRA") or os.path.expanduser(
        "~/.config/hmis-public-guard/patterns.txt")
    out = []
    try:
        with open(path, encoding="utf-8") as fh:
            for line in fh:
                line = line.strip()
                if line and not line.startswith("#"):
                    try:
                        out.append(("local pattern", re.compile(line, re.I)))
                    except re.error:
                        pass
    except OSError:
        pass
    return out


def file_names(command):
    for m in FILE_ARGS.finditer(command):
        name = m.group(1).strip("\"'")
        field = re.match(r"^\w+=(@?)(.*)$", name)
        if field:  # gh api -F key=value: only key=@file names a file
            if not field.group(1):
                continue
            name = field.group(2)
        yield name.lstrip("@")
    if GIST.search(command):
        yield from gist_files(command)


def posted_text(command, cwd):
    parts = [command]
    for name in file_names(command):
        if name in ("-", ""):
            continue
        path = os.path.expanduser(name)
        if not os.path.isabs(path):
            path = os.path.join(cwd, path)
        try:
            if os.path.getsize(path) < 2_000_000:
                with open(path, encoding="utf-8", errors="replace") as fh:
                    parts.append(fh.read())
        except OSError:
            pass
    return "\n".join(parts)


def shell_words(command):
    """shlex tokens with operators split out; None if the quoting is unbalanced."""
    try:
        lex = shlex.shlex(command, posix=True, punctuation_chars=True)
        lex.whitespace_split = True
        return list(lex)
    except ValueError:
        return None


def gist_files(command):
    """Non-flag words after gh gist create|edit, up to the next shell operator."""
    words = shell_words(command) or []
    out = []
    for i in range(len(words) - 2):
        if words[i:i + 2] == ["gh", "gist"] and words[i + 2] in ("create", "edit"):
            for w in words[i + 3:]:
                if w and all(c in SHELL_OPS for c in w):
                    break
                if not w.startswith("-"):
                    out.append(w)
    return out


def bypass_requested(command):
    if os.environ.get(BYPASS) == "1":
        return True
    words = shell_words(command)
    if not words:
        return False
    at_start = True
    for w in words:
        if w and all(c in SHELL_OPS for c in w):
            at_start = True
        elif at_start:
            if w == BYPASS + "=1":
                return True
            at_start = bool(re.match(r"^[A-Za-z_]\w*=", w))
    return False


def scan(text):
    hits = []
    for m in IPV4.finditer(text):
        if not IP_ALLOWED.match(m.group(1)):
            hits.append(("IPv4 address", m.group(1)))
    for label, rx in CHECKS + extra_checks():
        m = rx.search(text)
        if m:
            hits.append((label, m.group(0)))
    return hits


def redact(s):
    s = s.strip().replace("\n", " ")
    return s[:4] + "…" if len(s) > 6 else s[:2] + "…"


def main():
    try:
        data = json.load(sys.stdin)
    except ValueError:
        return 0
    if data.get("tool_name") != "Bash":
        return 0
    command = (data.get("tool_input") or {}).get("command") or ""
    joined = command.replace("\\\n", " ")  # backslash line continuations
    if not any(rx.search(joined) for rx in POSTING):
        return 0
    if bypass_requested(command):
        return 0
    hits = scan(posted_text(joined, data.get("cwd") or os.getcwd()))
    if not hits:
        return 0
    lines = ["public-guard hook: this text is going to the PUBLIC hmislk/hmis repo and matched:"]
    for label, frag in hits[:8]:
        lines.append("  - %s (starts %r)" % (label, redact(frag)))
    lines += [
        "Remove the data (see developer_docs/git/github-public-content-policy.md), or run the",
        "hmis-public-guard subagent on the text. If it returns PASS, or this is a false positive",
        "a human checked, re-run the command prefixed with HMIS_PUBLIC_GUARD_BYPASS=1.",
    ]
    sys.stderr.write("\n".join(lines) + "\n")
    return 2


if __name__ == "__main__":
    sys.exit(main())
