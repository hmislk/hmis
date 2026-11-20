# Temp files policy

Moved from the root CLAUDE.md; each rule there links here. The rules are unchanged.

Two-tier temp policy. In-repo `<project-root>/tmp/` holds only small TEXT working files (md reports, short scripts); it is gitignored and swept regularly. Bulk, binary or one-off artifacts (screenshots, Playwright output, PDFs, DB dumps) go to an OUT-OF-REPO per-machine directory, never inside the repo root and never ad-hoc system `/tmp/`. The session scratchpad directory the harness provides (under the system temp dir) is allowed for throwaway files. Resolve the repo root with `git rev-parse --show-toplevel`. Confirm no machine's repo root sits inside a cloud-synced folder (OneDrive, Dropbox).
