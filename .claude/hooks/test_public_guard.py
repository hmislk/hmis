#!/usr/bin/env python3
"""Table test for public-guard.py: every posting form x (clean -> 0, bad -> 2).

Run: python3 .claude/hooks/test_public_guard.py
The bad text is synthetic: a private-range address from RFC 1918.
"""
import json
import os
import subprocess
import sys
import tempfile

HOOK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "public-guard.py")
CLEAN = "Fixed the rounding in the pack rate"
BAD = "server at 10.20.30.40 is down"

# {T} is the inline text; {F} is a file holding it.
POSTING_FORMS = [
    'gh pr comment 1 --body "{T}"',
    'gh pr comment 1 -b "{T}"',
    'gh pr comment 1 --body="{T}"',
    'gh pr comment 1 --body-file {F}',
    'gh pr comment 1 -F {F}',
    'gh pr comment 1 --body-file={F}',
    'gh pr close 1 --comment "{T}"',
    'gh pr close 1 -c "{T}"',
    'gh pr close 1 --comment="{T}"',
    'gh pr reopen 1 --comment "{T}"',
    'gh pr merge 1 --merge --body "{T}"',
    'gh pr merge 1 --merge -b "{T}"',
    'gh pr merge 1 --merge --subject "{T}"',
    'gh pr merge 1 --merge -t "{T}"',
    'gh pr merge 1 --merge --body-file {F}',
    'gh pr review 1 --approve -b "{T}"',
    'gh pr review 1 --comment --body-file "{F}"',
    'gh pr create --base development --title "x" --body "{T}"',
    'gh pr create --title "x" --body-file {F}',
    'gh pr edit 1 --body "{T}"',
    'gh pr edit 1 --title "{T}"',
    'gh issue close 1 -c "{T}"',
    'gh issue close 1 --comment "{T}"',
    'gh issue close 1 --comment={T!r}',
    'gh issue comment 1 -b "{T}"',
    'gh issue comment 1 --body-file {F}',
    'gh issue create -t "x" -b "{T}"',
    'gh issue create --title "x" -F {F}',
    'gh issue edit 1 --body "{T}"',
    'gh issue reopen 1 -c "{T}"',
    'gh release create v1 --notes "{T}"',
    'gh release create v1 -n "{T}"',
    'gh release create v1 --notes-file {F}',
    'gh release create v1 -F {F}',
    'gh release edit v1 --notes="{T}"',
    'gh gist create {F}',
    'gh gist create -d "{T}" {F}',
    'gh label create x --description "{T}"',
    'gh repo edit --description "{T}"',
    'gh api repos/o/r/issues/1/comments -f body="{T}"',
    'gh api repos/o/r/issues/1/comments -F body=@{F}',
    'gh api repos/o/r/issues/1/comments --raw-field body="{T}"',
    'gh api repos/o/r/issues/1/comments --field body="{T}"',
    'gh api repos/o/r/pulls/1/comments/2/replies -f body="{T}"',
    'gh api repos/o/r/pulls/1/reviews --input {F}',
    'gh api -X PATCH repos/o/r/issues/1 -f body="{T}"',
    'gh api repos/o/r/pulls -f title=t -f body="{T}"',
    "gh api graphql -f query='mutation {{ addComment(input: {{body: \"{T}\"}}) {{ clientMutationId }} }}'",
    'gh api repos/o/r/issues/1/comments \\\n  -f body="{T}"',
    'gh api repos/o/r/issues/1/comments --input - <<EOF\n{{"body": "{T}"}}\nEOF',
    'git commit -m "{T}"',
    'git commit -am "{T}"',
    'git commit --message "{T}"',
    'git commit --message="{T}"',
    'git commit -F {F}',
    'git commit --file={F}',
    'git commit -m "$(cat <<\'EOF\'\n{T}\nEOF\n)"',
    'git -C /some/repo commit -m "{T}"',
    'cd /x && git add a.txt && git commit -m "{T}"',
    'git tag -a v1 -m "{T}"',
    'git tag v1 -F {F}',
    'git tag -s v1 --message="{T}"',
    'git notes add -m "{T}"',
    'git notes add -F {F}',
    'git merge --no-ff -m "{T}" topic',
]

# Commands that post nothing: pass even with bad text in them.
NON_POSTING = [
    'echo "{T}"',
    'gh pr view 1 --comments | grep "{T}"',
    'gh api repos/o/r/pulls/1/comments',
    'git log --grep "{T}"',
    'grep -F "{T}" README.md',
]

# Bypass: honoured only as an env prefix of a command.
BYPASS_CASES = [
    ('HMIS_PUBLIC_GUARD_BYPASS=1 gh pr comment 1 --body "{T}"', 0),
    ('cd /x && HMIS_PUBLIC_GUARD_BYPASS=1 gh pr close 1 -c "{T}"', 0),
    ('FOO=1 HMIS_PUBLIC_GUARD_BYPASS=1 git commit -m "{T}"', 0),
    ('gh pr comment 1 --body "{T} HMIS_PUBLIC_GUARD_BYPASS=1"', 2),
    ('gh pr comment 1 --body " HMIS_PUBLIC_GUARD_BYPASS=1 {T}"', 2),
    ('gh pr comment 1 --body HMIS_PUBLIC_GUARD_BYPASS=1 -c "{T}"', 2),
    ('gh pr comment 1 --body "{T}" ; echo HMIS_PUBLIC_GUARD_BYPASS=1', 2),
]


def run(command, cwd):
    env = dict(os.environ)
    env.pop("HMIS_PUBLIC_GUARD_BYPASS", None)
    env["HMIS_PUBLIC_GUARD_EXTRA"] = os.path.join(cwd, "no-such-patterns.txt")
    payload = json.dumps({"tool_name": "Bash", "cwd": cwd,
                          "tool_input": {"command": command}})
    return subprocess.run([sys.executable, HOOK], input=payload, text=True,
                          capture_output=True, env=env).returncode


def main():
    failures = 0
    total = 0
    with tempfile.TemporaryDirectory() as tmp:
        for text, label in ((CLEAN, "clean"), (BAD, "bad")):
            with open(os.path.join(tmp, label + ".md"), "w", encoding="utf-8") as fh:
                fh.write(text + "\n")

        def check(template, text, label, want):
            nonlocal failures, total
            cmd = template.replace("{T!r}", "'" + text + "'").replace(
                "{F}", label + ".md").replace("{T}", text).replace(
                "{{", "{").replace("}}", "}")
            got = run(cmd, tmp)
            total += 1
            if got != want:
                failures += 1
                print("FAIL want %d got %d: %s" % (want, got, cmd.replace("\n", "\\n")))

        for form in POSTING_FORMS:
            check(form, CLEAN, "clean", 0)
            check(form, BAD, "bad", 2)
        for form in NON_POSTING:
            check(form, BAD, "bad", 0)
        for form, want in BYPASS_CASES:
            check(form, BAD, "bad", want)
    print("%d/%d passed" % (total - failures, total))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
