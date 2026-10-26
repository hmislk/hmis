#!/usr/bin/env python3
"""
Bean scope guard.

Fails when a change adds, removes or replaces a scope annotation on an
existing Java class (for example @SessionScoped -> @ViewScoped).

Controllers are session-scoped on purpose; moving them to view or request
scope is planned as one coordinated migration. Changing one bean on its own
breaks pages and other beans that rely on its state surviving navigation.
Never change a bean's scope without first discussing it with the lead
developer.

New files are not checked (a new bean may choose any scope). Deleted files
are not checked. A renamed file is compared with its old path.

Usage:  check_bean_scope.py <base-ref> [--head <ref>] [--allow]
  --allow  report findings as warnings and exit 0 (PR labelled
           'scope-change-approved' by the lead developer).
"""
import re
import subprocess
import sys
from collections import Counter

SCOPES = (
    "SessionScoped", "ViewScoped", "RequestScoped", "ApplicationScoped",
    "ConversationScoped", "Dependent", "FlowScoped", "Singleton",
    "Stateless", "Stateful",
    # legacy javax.faces.bean scopes
    "SessionScope", "ViewScope", "RequestScope", "ApplicationScope",
    "NoneScoped", "CustomScoped",
)
ANNOTATION = re.compile(r"@(?:[\w.]+\.)?(" + "|".join(SCOPES) + r")\b")
COMMENTS = re.compile(r"//[^\n]*|/\*.*?\*/", re.S)
STRINGS = re.compile(r'"(?:\\.|[^"\\])*"')


def git(*args, ok=(0,)):
    r = subprocess.run(["git", *args], capture_output=True, encoding="utf-8", errors="replace")
    if r.returncode not in ok:
        raise subprocess.CalledProcessError(r.returncode, r.args, r.stdout, r.stderr)
    return r.stdout


def scopes(text):
    """Multiset of scope annotations in Java source, ignoring comments and strings."""
    code = COMMENTS.sub("", STRINGS.sub('""', text))
    return Counter(m.group(1) for m in ANNOTATION.finditer(code))


def show(ref, path):
    return git("show", f"{ref}:{path}", ok=(0, 128))


def changed_files(base, head):
    """(old_path, new_path) for modified and renamed .java files."""
    out = git("diff", "--name-status", "-M", f"{base}...{head}", "--", "*.java")
    pairs = []
    for line in out.splitlines():
        parts = line.split("\t")
        status = parts[0]
        if status.startswith("M"):
            pairs.append((parts[1], parts[1]))
        elif status.startswith("R"):
            pairs.append((parts[1], parts[2]))
    return pairs


def fmt(counter):
    return ", ".join(f"@{k}" + (f" x{v}" if v > 1 else "") for k, v in sorted(counter.items())) or "(none)"


def main(argv):
    args = [a for a in argv if not a.startswith("--")]
    allow = "--allow" in argv
    head = "HEAD"
    if "--head" in argv:
        head = argv[argv.index("--head") + 1]
        args.remove(head)
    if not args:
        print(__doc__)
        return 2
    base = args[0]

    findings = []
    for old, new in changed_files(base, head):
        before = scopes(show(base, old))
        after = scopes(show(head, new))
        if before != after:
            findings.append((new, before, after))

    if not findings:
        print("Bean scope guard: no scope annotation changed on an existing class.")
        return 0

    level = "WARNING" if allow else "ERROR"
    print(f"Bean scope guard {level}: this PR changes the scope of existing beans.\n")
    for path, before, after in findings:
        print(f"  {path}\n    before: {fmt(before)}\n    after:  {fmt(after)}")
    print(
        "\nNever change a bean's scope without discussing it with the lead developer first."
        "\nControllers are session-scoped on purpose; moving them is planned as one coordinated"
        "\nmigration, and changing one bean breaks pages and beans that rely on its state."
        "\nIf the change is not agreed, revert the scope annotation and fix the issue another way."
        "\nIf the lead developer has agreed, they add the PR label 'scope-change-approved'."
        "\nSee developer_docs/jsf/bean-scope-policy.md"
    )
    return 0 if allow else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
