#!/usr/bin/env python3
"""PreToolUse hook: regex scan of text about to be posted to the public repo.

Runs on Bash commands that post text to GitHub (gh pr/issue create|edit|comment|
review, gh api .../comments|replies|reviews) or write a commit message
(git commit -m/-F). Scans the command text plus any --body-file / -F file it
names. A high-confidence hit blocks the command (exit 2) and asks for the
hmis-public-guard subagent. The regexes are a backstop, not the judge.

Bypass for one command, after hmis-public-guard returned PASS or a human
checked the text: prefix it with HMIS_PUBLIC_GUARD_BYPASS=1.

Machine-specific patterns (real hostnames, which must never be committed) may be
added one regex per line in the file named by HMIS_PUBLIC_GUARD_EXTRA, default
~/.config/hmis-public-guard/patterns.txt. Policy:
developer_docs/git/github-public-content-policy.md
"""
import json
import os
import re
import sys

BYPASS = "HMIS_PUBLIC_GUARD_BYPASS"

POSTING = [
    re.compile(r"\bgh\s+pr\s+(create|edit|comment|review)\b"),
    re.compile(r"\bgh\s+issue\s+(create|comment|edit)\b"),
    re.compile(r"\bgh\s+api\b[^\n;|&]*/(comments|replies|reviews)\b"),
    re.compile(r"\bgit\s+commit\b[^\n;|&]*\s(-[a-zA-Z]*[mF]\b|--message\b|--file\b)"),
]

# File arguments whose content is the posted text.
FILE_ARGS = re.compile(
    r"(?:--body-file|--file|-F)(?:\s+|=)(?:body=)?@?(\"[^\"]+\"|'[^']+'|[^\s;|&]+)")

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


def posted_text(command, cwd):
    parts = [command]
    for m in FILE_ARGS.finditer(command):
        name = m.group(1).strip("\"'")
        field = re.match(r"^\w+=(@?)(.*)$", name)
        if field:  # gh api -F key=value: only key=@file names a file
            if not field.group(1):
                continue
            name = field.group(2)
        name = name.lstrip("@")
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
    if not any(rx.search(command) for rx in POSTING):
        return 0
    if os.environ.get(BYPASS) == "1" or re.search(r"(?:^|[\s;&(])" + BYPASS + r"=1\b", command):
        return 0
    hits = scan(posted_text(command, data.get("cwd") or os.getcwd()))
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
