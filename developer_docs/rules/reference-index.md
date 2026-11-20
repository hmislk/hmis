# Reference index

Moved from the root CLAUDE.md; each rule there links here. The rules are unchanged.

- Persistence/deployment: [JNDI dev vs prod](../deployment/persistence-verification.md) · [Windows remote access](../deployment/windows-remote-access-tips.md) · [Migrating a stale hospital DB](../deployment/migrating-a-stale-hospital-to-development.md) (always E2E-test a real OPD bill settle)
- Migrations must detect table-name case: [Migration Guide § case sensitivity](../database/migration-development-guide.md#cross-deployment-case-sensitivity-must) · Excel export of HTML tables: [guide](../feature/excel-export-html-table.md)
- Inward: [navigation](../navigation/inward_navigation.md) · [CC settlement](../billing/inward-cc-settlement-tracking.md)
- Subagents (`.claude/agents/`): [Agent authoring](../process/agent-authoring.md); CI `agent-lint` checks frontmatter.
