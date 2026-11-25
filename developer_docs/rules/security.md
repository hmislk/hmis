# Security rules (detail)

Moved from the root CLAUDE.md; each rule there links here. The rules are unchanged.

- Credentials: never write passwords, API keys, DB users, IPs, hostnames or SSH strings into any file in the project folder, tracked or not. Point to the external credentials file instead. Never print a credentials file to output, even "masked" (a failed mask prints it): pass the one value straight into the command.
- Secret file names are secret too: never `ls` or list the credentials folder, and never try other lines of a credentials file. A sub-agent never prints credentials or memory notes that hold them.
- No DB login in a QA brief: stop and report BLOCKED. For production reads, use one read-only loader per DB that never echoes secrets.
- Live vulnerabilities: no detail on ANY public GitHub surface until the fix is deployed everywhere: comments, PR bodies and titles, commit messages, close reasons. Write only "hardening, tracked under #N". GitHub keeps body edit-history and force-pushed commits by SHA, so draft security-PR text privately first.
- Threat-model note: green CI and e2e are not enough. State whether the change closes the whole class: forgery AND replay AND cross-purpose use.
- DB dumps and backups NEVER go inside the repo tree, even in a gitignored subdirectory. Resolve the root with `git rev-parse --show-toplevel` and refuse any dump path under it. Each machine picks its own out-of-repo directory.
