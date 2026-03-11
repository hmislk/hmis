# Claude Code Skills Guide

## Overview

Claude Code Skills are reusable instructions that extend Claude Code's capabilities within the HMIS project. Skills provide project-specific knowledge and automated workflows that help developers work more efficiently. They are loaded automatically when relevant to your task or invoked manually as slash commands.

## When to Use

- When starting a new development task with Claude Code
- When you need Claude to follow project-specific patterns and conventions
- When performing common workflows like deploying to QA or publishing documentation
- When reviewing code or preparing commits

## How Skills Work

Skills are stored in the `.claude/skills/` directory of the HMIS project. Each skill has a `SKILL.md` file that contains instructions Claude follows when the skill is active.

There are two types of skills:

### Automatic Skills (Reference)

These skills are loaded automatically by Claude when your conversation matches their purpose. You don't need to do anything special - just work normally and Claude will apply the relevant guidelines.

### Manual Skills (Slash Commands)

These skills are invoked by typing a slash command in Claude Code. Type `/` followed by the skill name and optional arguments.

## Available Skills

### Automatic Skills

These load automatically when you're working on related tasks:

| Skill | Activates When |
|-------|---------------|
| **UI Guidelines** | Editing XHTML pages, working with PrimeFaces components, page layout |
| **JSF AJAX** | Working with AJAX updates, `p:commandButton`, partial page rendering |
| **DTO Implementation** | Creating or modifying DTOs, JPQL constructor queries |
| **Database Guide** | Writing database queries, MySQL operations |
| **Security Privileges** | Adding privileges, access control, role-based rendering |
| **Pharmacy Dev** | Working on pharmacy module features |
| **Performance Optimization** | Optimizing slow queries, autocomplete, report generation |
| **App Configuration** | Working with feature toggles and configuration options |

### Slash Commands

Type these commands directly in Claude Code:

#### `/verify-persistence`

Checks `persistence.xml` for deployment readiness before git push.

**Example:**
```
/verify-persistence
```

**What it does:**
1. Reads the persistence.xml file
2. Checks that JNDI datasources use environment variables (not hardcoded names)
3. Checks for hardcoded DDL generation paths
4. Reports whether the file is deployment-ready

---

#### `/publish-wiki`

Publishes documentation from the `wiki-docs/` folder to the GitHub Wiki.

**Example:**
```
/publish-wiki
```

**What it does:**
1. Checks for documentation files in `wiki-docs/`
2. Clones or updates the sibling wiki repository
3. Copies documentation files preserving directory structure
4. Commits and pushes to the wiki

---

#### `/write-wiki [Module/Feature-Name]`

Creates end-user documentation following the project's wiki writing guidelines.

**Example:**
```
/write-wiki Pharmacy/Stock-Transfer
```

**What it does:**
1. Creates a documentation file in `wiki-docs/`
2. Follows the standard template with required sections
3. Writes for end users (not developers)
4. Suggests publishing with `/publish-wiki` when done

---

#### `/deploy-qa [qa1|qa2]`

Deploys code to QA testing environments.

**Example:**
```
/deploy-qa qa1
```

**What it does:**
1. Runs pre-deployment checks (persistence.xml verification)
2. Merges development branch into the QA branch
3. Pushes to trigger GitHub Actions deployment

---

#### `/review-code`

Reviews code changes against HMIS project standards.

**Example:**
```
/review-code
```

**What it does:**
1. Checks backward compatibility (no modified constructors, no renamed columns)
2. Verifies AI suggestion patterns
3. Validates persistence configuration
4. Checks JSF/XHTML rules, DTO patterns, and security

---

#### `/commit-code [issue-number]`

Commits staged changes with proper HMIS conventions.

**Example:**
```
/commit-code 18429
```

**What it does:**
1. Reviews staged changes
2. Creates a commit message with issue closing keyword (`Closes #18429`)
3. Adds co-author attribution
4. Checks for sensitive files before committing

## Best Practices

- **Let automatic skills work**: Don't try to manually invoke reference skills - they load when needed
- **Use slash commands for workflows**: Use `/verify-persistence` before every push
- **Combine commands**: After `/write-wiki`, use `/publish-wiki` to publish immediately
- **Check before deploying**: Always run `/verify-persistence` before `/deploy-qa`

## Troubleshooting

### Skill Not Loading

If a skill doesn't seem to be active:
1. Check that the `.claude/skills/` directory exists in the project
2. Verify the skill's `SKILL.md` file is present
3. Try explicitly mentioning the topic (e.g., "I'm working on AJAX updates")

### Slash Command Not Found

If a slash command doesn't work:
1. Type `/` and check the autocomplete menu for available skills
2. Verify you're in the HMIS project directory
3. Check that the skill file exists in `.claude/skills/<name>/SKILL.md`

### Too Many Skills Loading

If Claude seems to be applying too many rules:
1. Be specific about what you're working on
2. Skills with `disable-model-invocation: true` only load when you invoke them

## Related Features

- [Code Conventions](../Code-Conventions) - Project coding standards
- [Database Migration Development](Database-Migration-Development) - Database migration workflow
- [Deployment in a Development Environment](../Deployment-in-a-Development-Environment) - Setting up local development
