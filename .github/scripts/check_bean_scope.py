#!/usr/bin/env python3
"""
Bean scope guard.

Fails when a change adds, removes or replaces a scope annotation on an
existing Java class (for example @SessionScoped -> @ViewScoped, or
javax.faces.bean.ViewScoped -> javax.faces.view.ViewScoped).

Controllers are session-scoped on purpose; moving them to view or request
scope is planned as one coordinated migration. Changing one bean on its own
breaks pages and other beans that rely on its state surviving navigation.
Never change a bean's scope without first discussing it with the lead
developer.

Scopes are compared per class (by class name), with each annotation resolved
to its fully qualified type through the file's imports. New classes are not
checked; removed classes are not checked. Moved files are matched by path
rename or, when Git reports a heavily edited move as delete + add, by file
name. Changes to the guard itself are also reported, so the check cannot be
switched off silently.

Usage:  check_bean_scope.py <base-ref> [--head <ref>] [--allow]
  --allow  report findings as warnings and exit 0 (PR labelled
           'scope-change-approved' by an approver listed in
           .github/scope-change-approvers).
"""
import os
import re
import subprocess
import sys

SCOPES = (
    "SessionScoped", "ViewScoped", "RequestScoped", "ApplicationScoped",
    "ConversationScoped", "Dependent", "FlowScoped", "Singleton",
    "Stateless", "Stateful",
    # legacy javax.faces.bean scopes
    "SessionScope", "ViewScope", "RequestScope", "ApplicationScope",
    "NoneScoped", "CustomScoped",
)
ANNOTATION = re.compile(r"@((?:[a-z_][\w]*\.)+)?(" + "|".join(SCOPES) + r")\b")
CLASS_DECL = re.compile(r"\b(?:class|interface|enum|record)\s+([A-Za-z_]\w*)")
IMPORT = re.compile(r"^\s*import\s+([\w.]+)\.(\w+)\s*;", re.M)
COMMENTS = re.compile(r"//[^\n]*|/\*.*?\*/", re.S)
STRINGS = re.compile(r'"(?:\\.|[^"\\])*"')
GUARD_FILES = (
    ".github/scripts/check_bean_scope.py",
    ".github/workflows/bean_scope_guard.yml",
    ".github/scope-change-approvers",
)


def git(*args, ok=(0,)):
    r = subprocess.run(["git", *args], capture_output=True, encoding="utf-8", errors="replace")
    if r.returncode not in ok:
        raise subprocess.CalledProcessError(r.returncode, r.args, r.stdout, r.stderr)
    return r.stdout


def class_scopes(text):
    """{class name: sorted fully qualified scope annotations} for Java source."""
    code = COMMENTS.sub("", STRINGS.sub('""', text))
    imports = {name: pkg + "." + name for pkg, name in IMPORT.findall(code)}
    result = {}
    pending = []
    pos = 0
    tokens = sorted(
        [(m.start(), "a", m) for m in ANNOTATION.finditer(code)]
        + [(m.start(), "c", m) for m in CLASS_DECL.finditer(code)],
        key=lambda t: t[0],
    )
    for _start, kind, m in tokens:
        if kind == "a":
            qualifier, name = m.group(1), m.group(2)
            pending.append(qualifier + name if qualifier else imports.get(name, name))
        else:
            result.setdefault(m.group(1), [])
            result[m.group(1)].extend(pending)
            pending = []
    return {k: sorted(v) for k, v in result.items()}


def show(ref, path):
    return git("show", f"{ref}:{path}", ok=(0, 128))


def changed(base, head):
    """((old_path, new_path) Java pairs to compare, changed guard files)."""
    out = git("diff", "--name-status", "-M", f"{base}...{head}")
    pairs, deleted, added, guard = [], {}, {}, []
    for line in out.splitlines():
        parts = line.split("\t")
        status, paths = parts[0], parts[1:]
        if any(p in GUARD_FILES for p in paths):
            guard.append(paths[-1])
        if not paths[-1].endswith(".java") and not paths[0].endswith(".java"):
            continue
        if status.startswith("M"):
            pairs.append((paths[0], paths[0]))
        elif status.startswith("R"):
            pairs.append((paths[0], paths[1]))
        elif status.startswith("D"):
            deleted[os.path.basename(paths[0])] = paths[0]
        elif status.startswith("A"):
            added[os.path.basename(paths[0])] = paths[0]
    for name, old in deleted.items():
        if name in added:
            pairs.append((old, added[name]))
    return pairs, guard


def fmt(scopes):
    return ", ".join("@" + s for s in scopes) or "(none)"


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

    pairs, guard = changed(base, head)
    findings = []
    for old, new in pairs:
        before = class_scopes(show(base, old))
        after = class_scopes(show(head, new))
        for cls in sorted(before.keys() & after.keys()):
            if before[cls] != after[cls]:
                findings.append((f"{new} ({cls})", before[cls], after[cls]))

    if not findings and not guard:
        print("Bean scope guard: no scope annotation changed on an existing class.")
        return 0

    level = "WARNING" if allow else "ERROR"
    if findings:
        print(f"Bean scope guard {level}: this PR changes the scope of existing beans.\n")
        for where, before, after in findings:
            print(f"  {where}\n    before: {fmt(before)}\n    after:  {fmt(after)}")
    if guard:
        print(f"\nBean scope guard {level}: this PR changes the guard itself:")
        for path in guard:
            print(f"  {path}")
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
