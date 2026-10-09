#!/usr/bin/env python3
"""
Config semantics guard.

Fails when a change silently alters what an existing boolean configuration
option means:

  1. A changed line reads the same key as the line it replaced, but with the
     negation flipped (`!` / `not` added or removed) or a different default
     (`true` <-> `false`).
  2. The change makes a key read with both `true` and `false` defaults when it
     was not before (the stored value would then depend on which screen read
     the key first).

Hospitals keep a stored value per key, so either change makes the same stored
value do something different.

Usage:  check_config_semantics.py <base-ref> [--head <ref>] [--allow]
  --allow  report findings as warnings and exit 0 (PR labelled
           'config-meaning-change').
"""
import re
import subprocess
import sys
from collections import Counter, defaultdict

CALL = re.compile(
    r"(?P<neg>!\s*|\bnot\s+)?"                      # optional negation right before the call
    r"(?:[\w.]+\.)?getBooleanValueByKey\(\s*"
    r"(?P<q>[\"'])(?P<key>(?:(?!(?P=q)).)+)(?P=q)"  # key in single or double quotes
    r"(?:\s*,\s*(?P<default>true|false)\b)?"        # optional literal default
)
SOURCE = re.compile(r"\.(java|xhtml)$")


def git(*args, ok=(0,)):
    r = subprocess.run(["git", *args], capture_output=True, encoding="utf-8", errors="replace")
    if r.returncode not in ok:
        raise subprocess.CalledProcessError(r.returncode, r.args, r.stdout, r.stderr)
    return r.stdout


def reads(line):
    """(key, negated, default) for every boolean config read on the line."""
    out = []
    for m in CALL.finditer(line):
        out.append((m.group("key"), bool(m.group("neg")), m.group("default") or "-"))
    return out


def defaults_by_key(text):
    found = defaultdict(set)
    for line in text.splitlines():
        for key, _neg, default in reads(line):
            if default != "-":
                found[key].add(default)
    return found


def tree_defaults(ref):
    """Defaults used for each key anywhere in the tree at ref (one git grep)."""
    out = git("grep", "-h", "-E", "getBooleanValueByKey", ref, "--", "*.java", "*.xhtml", ok=(0, 1))
    return defaults_by_key(out)


def changed_hunks(base, head):
    """Yield (file, new_start_line, removed_lines, added_lines) per hunk."""
    diff = git("diff", "--unified=0", f"{base}...{head}", "--", "*.java", "*.xhtml")
    file = None
    hunk = None
    for line in diff.splitlines():
        if line.startswith("+++ "):
            file = line[6:] if line.startswith("+++ b/") else None
            continue
        if line.startswith("@@"):
            if hunk:
                yield hunk
            m = re.search(r"\+(\d+)", line)
            hunk = (file, int(m.group(1)) if m else 0, [], [])
            continue
        if hunk is None or file is None:
            continue
        if line.startswith("-") and not line.startswith("---"):
            hunk[2].append(line[1:])
        elif line.startswith("+") and not line.startswith("+++"):
            hunk[3].append(line[1:])
    if hunk:
        yield hunk


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    base = sys.argv[1]
    allow = "--allow" in sys.argv[2:]
    head = sys.argv[sys.argv.index("--head") + 1] if "--head" in sys.argv else "HEAD"
    findings = []

    # Rule 1: same key on removed and added lines of one hunk, semantics differ.
    for file, start, removed, added in changed_hunks(base, head):
        before = Counter(r for line in removed for r in reads(line))
        after = Counter(r for line in added for r in reads(line))
        keys = {k for k, _, _ in before} & {k for k, _, _ in after}
        for key in sorted(keys):
            b = Counter({(n, d): c for (k, n, d), c in before.items() if k == key})
            a = Counter({(n, d): c for (k, n, d), c in after.items() if k == key})
            if set(b) != set(a):
                def fmt(c):
                    return ", ".join(f"{'!' if n else ''}read(default={d})" for (n, d) in sorted(c))
                findings.append((file, start,
                                 f'Config key "{key}" changed meaning: before [{fmt(b)}] -> after [{fmt(a)}]. '
                                 "Do not flip the negation or default of an existing key; create a new, clearly named key instead."))

    # Rule 2: a key newly read with both true and false defaults.
    head_defaults = tree_defaults(head)
    base_defaults = tree_defaults(base)
    for key, vals in sorted(head_defaults.items()):
        if len(vals) > 1 and len(base_defaults.get(key, set())) <= 1:
            findings.append((None, None,
                             f'Config key "{key}" is now read with conflicting defaults {sorted(vals)}. '
                             "Use one default everywhere."))

    if not findings:
        print("Config semantics guard: no changes to the meaning of existing config keys.")
        return 0

    level = "warning" if allow else "error"
    for file, line, msg in findings:
        loc = f" file={file},line={line}" if file else ""
        print(f"::{level}{loc}::{msg}")
    if allow:
        print("Label 'config-meaning-change' present: reported as warnings. "
              "Explain the change and the impact on every hospital in the PR description.")
        return 0
    print("\nIf this change of meaning is really intended, add the PR label 'config-meaning-change' "
          "and explain in the PR description how existing hospitals are affected.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
