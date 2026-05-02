# Codex Code Configuration for HMIS Project

This file mirrors the repository-level Codex operating guidance and is intended to be maintained side-by-side with `CLAUDE.md`.

## Primary Instruction Source
- Follow `AGENTS.md` as the authoritative Codex instruction file for this repository.
- Keep Claude-specific instructions in `CLAUDE.md` unchanged unless explicitly requested.

## Codex Operating Summary
1. Use the main checkout directory (no worktree isolation).
2. Do not auto-run commit/build/push/deploy actions unless explicitly requested.
3. Prefer JPQL; use native SQL only when required by proven constraints.
4. Preserve backward compatibility (intentional typo fields, composite component names, existing constructor signatures).
5. Use `./detect-maven.sh test` when tests are needed; JSF-only changes do not require compile/tests.
6. Validate `persistence.xml` placeholders before push (`${JDBC_DATASOURCE}`, `${JDBC_AUDIT_DATASOURCE}`) and avoid hardcoded local paths.
7. Target `development` for normal PRs; production hotfix branches must end with `-hotfix`.
8. Include issue auto-close keywords (`Closes #N`) in commit/PR messages.

## Key References
- `AGENTS.md`
- `CLAUDE.md`
- `developer_docs/git/commit-conventions.md`
- `developer_docs/deployment/qa-deployment-guide.md`
- `developer_docs/dto/implementation-guidelines.md`

---
This behavior should persist across all Codex sessions for this project.
