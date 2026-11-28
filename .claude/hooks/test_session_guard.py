#!/usr/bin/env python3
"""Tests for session-guard.py. Run: python3 .claude/hooks/test_session_guard.py
(also works with pytest). gh is replaced by a Python stub run with sys.executable, so no network is used.
"""
import json
import os
import subprocess
import sys
import tempfile
import unittest

HOOK = os.path.join(os.path.dirname(os.path.abspath(__file__)), "session-guard.py")
TMP = tempfile.mkdtemp(prefix="session-guard-test-")


FAKE = os.path.join(TMP, "fake_gh.py")
with open(FAKE, "w") as f:
    f.write("""import os, sys
mode = sys.argv[1]
if mode == "fail":
    sys.stderr.write("boom\\n")
    sys.exit(1)
if mode == "empty":
    sys.exit(0)
with open(os.environ["GH_LOG"], "a") as log:
    log.write(" ".join(sys.argv[2:]) + "\\n")
print(os.environ["BASE"])
""")


def fake_gh(mode):
    # A command line: the guard parses it and runs it without a shell.
    return '"%s" "%s" %s' % (sys.executable, FAKE, mode)


# Prints the base named in $BASE; logs its arguments to $GH_LOG.
GH_OK = fake_gh("ok")
GH_FAIL = fake_gh("fail")
GH_EMPTY = fake_gh("empty")


def run(cmd, gh=GH_OK, base="development", extra_env=None, tool="Bash"):
    env = dict(os.environ)
    env.update({"HMIS_SESSION_GUARD_GH": gh, "BASE": base,
                "GH_LOG": os.path.join(TMP, "gh.log")})
    env.update(extra_env or {})
    p = subprocess.run([sys.executable, HOOK], input=json.dumps(
        {"tool_name": tool, "tool_input": {"command": cmd}, "cwd": TMP}),
        capture_output=True, text=True, env=env, timeout=30)
    return p.returncode, p.stderr


class Base(unittest.TestCase):
    def deny(self, cmd, **kw):
        rc, err = run(cmd, **kw)
        self.assertEqual(rc, 2, "should deny: %s" % cmd)
        self.assertNotIn("s3cr3t", err)  # the message never echoes command text
        return err

    def allow(self, cmd, **kw):
        rc, err = run(cmd, **kw)
        self.assertEqual(rc, 0, "should allow: %s (%s)" % (cmd, err))


class PowerShellTests(Base):
    PS = {"tool": "PowerShell"}

    def test_merge(self):
        for c in ("gh pr merge 12", "gh.exe pr merge 12 --squash", "& gh pr merge 12",
                  "cd x; gh pr merge 12", "git status; & 'gh' pr merge 12",
                  'pwsh -Command "gh pr merge 12"', "powershell -c 'gh pr merge 12'",
                  'cmd /c "gh pr merge 12"', "iex 'gh pr merge 12'",
                  "gh api -X PUT repos/hmislk/hmis/pulls/12/merge"):
            self.deny(c, base="master", **self.PS)
            self.allow(c, base="development", **self.PS)
            self.deny(c, gh=GH_FAIL, **self.PS)

    def test_add(self):
        for c in ("git add -A", "git add .", "git.exe add --all", "& git add .", "git status; git add -A",
                  'pwsh -NoProfile -Command "git add -A"', 'cmd /c "git add ."', "git -C C:\\x add .",
                  "git add `\n-A", "git add *", "git add ':/'"):
            self.deny(c, **self.PS)
        for c in ("git add src\\x.java", "git add src/x.java", "git log --all", "echo `git add -A`"):
            self.allow(c, **self.PS)

    def test_push(self):
        for c in ("git push --force", "git push origin +branch", "& git push -f", "git status; git push --force-with-lease",
                  'pwsh -Command "git push -f"', 'cmd /c git push --force'):
            self.deny(c, **self.PS)
        for c in ("git push origin branch", "git push -u origin x"):
            self.allow(c, **self.PS)

    def test_ssh(self):
        for c in ("ssh app1 cat /opt/example/app/config/domain.xml",
                  "ssh.exe app1 'asadmin list-jdbc-connection-pools'", "& ssh app1 'cat domain.xml'",
                  "scp app1:/x/domain.xml .", 'pwsh -c "ssh app1 cat domain.xml"'):
            self.deny(c, **self.PS)
        for c in ("ssh app1 uptime", "ssh.exe app1 'df -h'"):
            self.allow(c, **self.PS)


class MergeTests(Base):
    def test_deploy_bases_denied(self):
        for b in ("master", "rh-production", "coop-staging", "staging", "x-prod", "x-stg"):
            self.deny("gh pr merge 12 --merge", base=b)

    def test_development_allowed(self):
        for b in ("development", "feature-x"):
            self.allow("gh pr merge 12 --merge --match-head-commit abc", base=b)

    def test_arg_forms_and_repo_flag(self):
        for c in ("gh pr merge 12 -R hmislk/hmis", "gh pr merge --repo hmislk/hmis 12",
                  "gh pr merge https://github.com/hmislk/hmis/pull/12", "gh pr merge my-branch",
                  "gh pr merge --squash", "gh pr merge 12 -b 'text' -t 'subject' --admin"):
            self.deny(c, base="master")
            self.allow(c, base="development")

    def test_repo_and_pr_passed_to_lookup(self):
        open(os.path.join(TMP, "gh.log"), "w").close()
        run("gh pr merge 12 -R hmislk/hmis --squash")
        with open(os.path.join(TMP, "gh.log")) as f:
            log = f.read()
        self.assertIn("pr view 12", log)
        self.assertIn("-R hmislk/hmis", log)

    def test_compound_and_wrappers(self):
        for c in ("cd x && gh pr merge 5", "true; gh pr merge 5", "echo a | gh pr merge 5",
                  "(gh pr merge 5)", 'bash -c "gh pr merge 5"', "sh -c 'cd x && gh pr merge 5'",
                  "FOO=1 gh pr merge 5", "sudo gh pr merge 5", "eval 'gh pr merge 5'",
                  "echo $(gh pr merge 5)", "/usr/bin/gh pr merge 5"):
            self.deny(c, base="master")

    def test_lookup_failure_fails_closed(self):
        for gh in (GH_FAIL, GH_EMPTY):
            err = self.deny("gh pr merge 12", gh=gh)
            self.assertIn("could not check", err)

    def test_gh_not_found_message(self):
        err = self.deny("gh pr merge 12", gh=os.path.join(TMP, "does-not-exist"))
        self.assertIn("gh not found; merges cannot be checked", err)

    def test_api_merge_path(self):
        for c in ("gh api -X PUT repos/hmislk/hmis/pulls/12/merge",
                  "gh api --method PUT /repos/hmislk/hmis/pulls/12/merge -f merge_method=squash",
                  "gh api -XPUT repos/hmislk/hmis/pulls/12/merge"):
            self.deny(c, base="master")
            self.allow(c, base="development")
            self.deny(c, gh=GH_FAIL)

    def test_api_merges_endpoint(self):
        self.deny("gh api repos/hmislk/hmis/merges -f base=rh-production -f head=development")
        self.deny("gh api repos/hmislk/hmis/merges -f head=development")  # no base: fail closed
        self.allow("gh api repos/hmislk/hmis/merges -f base=development -f head=x")

    def test_graphql_merge_denied(self):
        self.deny("gh api graphql -f query='mutation { mergePullRequest(input:{pullRequestId:\"x\"}) { clientMutationId } }'")

    def test_lookalikes_allowed(self):
        for c in ("gh pr view 12", "gh pr list --state all --search merge", "gh pr create --base development",
                  "gh api repos/hmislk/hmis/pulls/12", "gh api repos/hmislk/hmis/pulls/12/comments",
                  "echo gh pr merge 5", "git merge origin/development"):
            self.allow(c, base="master")


class AddTests(Base):
    def test_denied(self):
        self.deny("git add -A s3cr3t_marker/.")
        for c in ("git add -A", "git add --all", "git add .", "git add -- .", "git -C /x add -A",
                  "git -c core.x=1 add .", "git add -Av", "git add -vA", "git add src/a.java .",
                  "cd x && git add -A && git commit -m x", "git status; git add .",
                  "echo a | git add -A", "(git add .)", 'bash -c "git add -A"', "sh -c 'git add .'",
                  "echo $(git add -A)", "/usr/bin/git add .", "sudo git add -A", "git add ./"):
            self.deny(c)

    def test_all_files_pathspecs_and_alias(self):
        for c in ("git add :/", "git add '*'", "git add *", "git add ':/*'", "git -c alias.x=push x --force",
                  "git -c alias.p=add p .", "git -calias.x=status x", "git -c ALIAS.x=status x"):
            self.deny(c)
        for c in ("git add src/*.java", "git -c core.x=1 status", "git -c user.name=a commit -m x"):
            self.allow(c)

    def test_allowed(self):
        for c in ("git add src/x.java", "git add src/a.java src/b.java", "git add -- src/x.java",
                  "git add -p src/x.java", "git add .claude/hooks/x.py", "git add ./src/x.java",
                  "git log --all", "git status --all", "git commit -m 'do not git add -A'",
                  "git -C /x add src/y", "echo 'git add .'", "git stash --all"):
            self.allow(c)


class PushTests(Base):
    def test_denied(self):
        for c in ("git push --force", "git push -f", "git push origin branch -f", "git push --force-with-lease",
                  "git push --force-with-lease=branch:abc origin branch", "git push --force-if-includes",
                  "git push origin +branch", "git push origin +HEAD:x", "git push -uf origin x",
                  "git -C /x push --force", "cd x && git push -f", 'bash -c "git push origin +x"',
                  "git push origin 'HEAD:x' --force"):
            self.deny(c)

    def test_allowed(self):
        for c in ("git push", "git push origin branch", "git push -u origin branch", "git push origin HEAD:branch",
                  "git push --set-upstream origin feat-fix", "git push origin --tags", "git pull --force",
                  "git fetch -f origin", "git push origin feature+x"):
            self.allow(c)


    def test_no_exception_route(self):
        self.deny("git push --force origin home-qa-1")


class SshTests(Base):
    def test_denied(self):
        for c in ("ssh app1 'asadmin get server.resources.jdbc-connection-pool.x.property.*'",
                  "ssh app1 /opt/example/bin/asadmin list-jdbc-connection-pools",
                  "ssh -i k u@h '/opt/p/bin/asadmin get --passwordfile x server.resources'  ",
                  "ssh app1 'asadmin list-password-aliases'",
                  "ssh app1 cat /opt/example/app/config/domain.xml",
                  "ssh app1 'grep -i password /opt/example/app/config/domain.xml'",
                  "ssh app1 cat /x/domain.xml | grep user",
                  "scp app1:/opt/example/app/config/domain.xml .",
                  "rsync -e ssh app1:/opt/p/config/domain.xml /tmp/x", "sftp app1:/x/domain.xml",
                  "cd x && ssh app1 'cat domain.xml'", 'bash -c "ssh app1 cat domain.xml"'):
            self.deny(c)

    def test_allowed(self):
        for c in ("ssh app1 uptime", "ssh -i key user@host 'df -h'", "ssh app1 'asadmin list-applications'",
                  "ssh app1 'asadmin list-domains'", "ssh app1 'tail -n 50 /opt/example/app/logs/server.log'",
                  "scp build.war app1:/tmp/", "rsync -av ./out/ app1:/tmp/out/",
                  "cat domain.xml", "grep password notes.txt", "asadmin get x.y.property.z"):
            self.allow(c)


class GeneralTests(Base):
    def test_non_bash_and_bad_input_pass(self):
        env = dict(os.environ)
        p = subprocess.run([sys.executable, HOOK], input="not json", capture_output=True, text=True, env=env)
        self.assertEqual(p.returncode, 0)
        p = subprocess.run([sys.executable, HOOK], capture_output=True, text=True, env=env,
                           input=json.dumps({"tool_name": "Read", "tool_input": {"command": "git add -A"}}))
        self.assertEqual(p.returncode, 0)

    def test_unparsable_quotes_use_fallback(self):
        self.deny("git add -A 'unclosed")
        self.deny("git push --force 'unclosed")
        self.allow("echo 'unclosed")

    def test_other_commands_pass(self):
        for c in ("ls -la", "mvn -q compile", "echo hello && pwd", ""):
            self.allow(c)


if __name__ == "__main__":
    unittest.main(verbosity=1)
