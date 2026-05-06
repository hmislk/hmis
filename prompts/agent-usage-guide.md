# HMIS Agent Usage Guide

## Overview

This guide explains how to effectively use AI agents in the HMIS project for common development tasks. Agents are specialized assistants that handle specific types of work - from UI development to performance optimization.

## Available Agents

### Core Development Agents

1. **jsf-frontend-dev** - JSF/PrimeFaces UI development
2. **java-backend-developer** - Backend Java development (entities, DTOs, controllers, services)
3. **java-health-code-reviewer** - Healthcare-specific code review
4. **performance-optimizer** - JPQL query and database optimization
5. **java-ee-code-analyzer** - Understanding complex codebases and tracing logic

### Specialized Agents

6. **health-informatics-architect** - Healthcare standards, enterprise architecture, TOGAF
7. **quickbooks-healthcare-finance** - Accounting integrations, financial reporting
8. **devops-troubleshooter** - Deployment issues, CI/CD, infrastructure
9. **Explore** - Fast codebase exploration
10. **Plan** - Implementation planning

---

## Common Task Workflows

### Task 1: Adding a New Feature to Pharmacy Module

**Scenario:** "Add a feature to track medicine batch expiry alerts"

**Agent Workflow:**

#### Step 1: Planning
- **Agent:** Plan agent
- **How to trigger:** Let Claude auto-trigger when you describe the feature
- **What it does:** Explores codebase, understands existing patterns, creates implementation plan

#### Step 2: Backend Implementation
- **Agent:** java-backend-developer
- **Triggered for:** Creating entities, DTOs, controllers, services
- **Example request:** "Create PharmaceuticalBatchExpiry entity and ExpiryAlertController"

#### Step 3: UI Development
- **Agent:** jsf-frontend-dev
- **Triggered for:** Creating XHTML views, PrimeFaces components
- **Example request:** "Create expiry alert dashboard with dataTable"

#### Step 4: Code Review
- **Agent:** java-health-code-reviewer
- **Triggered for:** Reviewing completed code before commit
- **Example request:** "Review this expiry alert feature"

---

### Task 2: Fixing Performance Issues

**Scenario:** "Patient search is slow when filtering by date range"

**Agent Workflow:**

#### Step 1: Investigation
- **Agent:** java-ee-code-analyzer
- **Triggered for:** Understanding existing code, tracing queries
- **Example request:** "Analyze how patient search filtering works"

#### Step 2: Optimization
- **Agent:** performance-optimizer
- **Triggered for:** JPQL query optimization, identifying N+1 queries
- **Example request:** "Optimize this patient search JPQL query"

#### Step 3: Code Review
- **Agent:** java-health-code-reviewer
- **Triggered for:** Ensure optimization doesn't break healthcare logic
- **Example request:** "Review these performance changes"

---

### Task 3: UI Improvements

**Scenario:** "Make the billing screen more user-friendly"

**Agent Workflow:**

#### Step 1: UI-Only Changes
- **Agent:** jsf-frontend-dev (EXCLUSIVELY)
- **Triggered for:** XHTML styling, PrimeFaces component improvements
- **Example request:** "Improve billing screen layout with better spacing and icons"
- **IMPORTANT:** This agent won't add backend code (follows UI-ONLY rule from CLAUDE.md)

---

### Task 4: Understanding Unfamiliar Module

**Scenario:** "How does the substitute item functionality work in inventory?"

**Agent Workflow:**

#### Step 1: Code Exploration
- **Agent:** java-ee-code-analyzer
- **Triggered for:** Tracing entity relationships, business logic flow
- **Example request:** "Explain how substitute item feature works across inventory and pharmacy"
- **What it does:** Traces code flow, entity relationships, JPA mappings

---

### Task 5: Financial/Accounting Feature

**Scenario:** "Export pharmacy sales to QuickBooks format"

**Agent Workflow:**

#### Step 1: Financial Design
- **Agent:** quickbooks-healthcare-finance
- **Triggered for:** Accounting integration, financial data handling
- **Example request:** "Design pharmacy sales export to QuickBooks"

#### Step 2: Backend Implementation
- **Agent:** java-backend-developer
- **Triggered for:** Implementing the export logic

#### Step 3: Code Review
- **Agent:** java-health-code-reviewer
- **Triggered for:** Review accounting logic and healthcare compliance

---

## Agent Trigger Rules

### Auto-Trigger Keywords

| Keywords in Request | Agent Triggered | Example Request |
|---------------------|-----------------|-----------------|
| "UI", "screen", "layout", "styling" (no backend) | jsf-frontend-dev | "Make pharmacy screen prettier" |
| "XHTML", "PrimeFaces", "dataTable", "AJAX update" | jsf-frontend-dev | "Fix AJAX update on patient form" |
| "entity", "DTO", "controller", "service" | java-backend-developer | "Create BatchExpiry entity" |
| "review", "check implementation" | java-health-code-reviewer | "Review my billing code" |
| "slow", "performance", "optimize query", "LazyInitialization" | performance-optimizer | "Patient search is slow" |
| "how does X work", "where is X", "understand" | java-ee-code-analyzer | "How does substitute item work?" |
| "accounting", "financial report", "QuickBooks" | quickbooks-healthcare-finance | "Export to QuickBooks" |
| "deployment", "build failing", "CI/CD" | devops-troubleshooter | "Build failing on QA" |

---

## Best Practices

### 1. Be Explicit When You Want Specific Agent

**Good Examples:**
```
✅ "Use jsf-frontend-dev to redesign this form"
✅ "Use performance-optimizer to fix this slow query"
✅ "Use java-ee-code-analyzer to trace how billing works"
```

### 2. Run Agents in Parallel for Independent Tasks

**Good Example:**
```
✅ "Review the backend code AND optimize the queries"
   → Triggers java-health-code-reviewer + performance-optimizer in parallel
```

### 3. Chain Agents for Complex Multi-Step Tasks

**Good Example:**
```
Step 1: "Understand how billing works"
        → java-ee-code-analyzer

Step 2: "Add discount feature to billing"
        → java-backend-developer

Step 3: "Create UI for discount selection"
        → jsf-frontend-dev

Step 4: "Review everything before commit"
        → java-health-code-reviewer
```

### 4. Separate UI and Backend Requests

**Bad Example:**
```
❌ "Add search feature" (ambiguous - UI + backend?)
```

**Good Examples:**
```
✅ "Add search backend logic" → java-backend-developer
✅ "Add search UI form" → jsf-frontend-dev
```

### 5. Use Correct Agent for Task Type

| Task Type | Correct Agent | Wrong Agent |
|-----------|---------------|-------------|
| Styling XHTML | jsf-frontend-dev | java-backend-developer |
| Creating entities | java-backend-developer | jsf-frontend-dev |
| Optimizing JPQL | performance-optimizer | java-backend-developer |
| Understanding code flow | java-ee-code-analyzer | Explore |
| Healthcare compliance review | java-health-code-reviewer | general review |

---

## Common Mistakes to Avoid

### Mistake 1: Mixing UI and Backend in Single Request

**Problem:** "Add patient search feature" triggers both UI and backend changes

**Solution:**
```
Step 1: "Add patient search backend with JPQL query"
Step 2: "Add patient search UI form"
```

### Mistake 2: Not Using Performance Agent for Slow Queries

**Problem:** General agent might fix slow query without optimization expertise

**Solution:**
```
✅ "Use performance-optimizer to fix this slow patient search query"
```

### Mistake 3: Using Wrong Agent for Code Understanding

**Problem:** Using Explore agent for deep code analysis

**Solution:**
```
❌ "Explore how billing discount works" (too shallow)
✅ "Use java-ee-code-analyzer to trace billing discount logic across entities and controllers"
```

---

## Quick Reference Card

**Save this for daily use:**

```
📋 HMIS AGENT QUICK REFERENCE

🎨 UI Work Only          → jsf-frontend-dev
⚙️ Backend Code          → java-backend-developer
🔍 Understand Code       → java-ee-code-analyzer
⚡ Slow/Performance      → performance-optimizer
✅ Code Review           → java-health-code-reviewer
💰 Accounting/Finance    → quickbooks-healthcare-finance
🚀 Build/Deploy Issues   → devops-troubleshooter
📐 Strategic Planning    → health-informatics-architect
🔎 Quick File Search     → Explore
📝 Implementation Plans  → Plan
```

---

## Real-World Examples

### Example 1: Complete Feature Implementation

**Request:** "Add medicine batch expiry warning feature"

**Claude's Response:**
```
I'll break this into steps using specialized agents:

1. Using Plan agent to design the feature...
   ✓ Planned: Entity, DTO, Controller, UI components

2. Using java-backend-developer to create backend...
   ✓ Created: BatchExpiry entity, BatchExpiryDTO, BatchExpiryController

3. Using jsf-frontend-dev to create UI...
   ✓ Created: batch_expiry_alerts.xhtml with PrimeFaces dataTable

4. Using java-health-code-reviewer to review...
   ✓ Reviewed: Security, healthcare compliance, backward compatibility
```

### Example 2: Performance Issue Investigation

**Request:** "Patient admission list is very slow to load"

**Claude's Response:**
```
I'll investigate this performance issue:

1. Using java-ee-code-analyzer to understand current implementation...
   ✓ Found: PatientController.loadAdmissions() with N+1 query problem

2. Using performance-optimizer to fix the query...
   ✓ Optimized: Added JOIN FETCH to JPQL query
   ✓ Reduced: 500 queries → 1 query

3. Using java-health-code-reviewer to verify changes...
   ✓ Verified: No business logic broken, audit trail intact
```

### Example 3: UI-Only Improvement

**Request:** "Make the pharmacy dashboard more modern-looking"

**Claude's Response:**
```
I'll use jsf-frontend-dev for UI-only improvements:

✓ Updated: PrimeFaces panel styling with modern theme
✓ Added: Icon buttons with proper sizing
✓ Improved: Responsive grid layout
✓ Enhanced: Color scheme and spacing

No backend changes made (following UI-ONLY rule).
```

---

## Troubleshooting Agent Issues

### Issue: Agent Not Triggering

**Symptoms:** General response instead of specialized agent

**Solutions:**
1. Be more explicit: "Use [agent-name] to..."
2. Include trigger keywords from table above
3. Ask: "Which agent should handle this task?"

### Issue: Wrong Agent Triggered

**Symptoms:** Backend agent triggered for UI task

**Solutions:**
1. Add "UI only" or "backend only" to request
2. Reference specific technology: "XHTML changes only"
3. Override: "Use jsf-frontend-dev instead"

### Issue: Multiple Agents Needed

**Symptoms:** Task requires both UI and backend work

**Solutions:**
1. Break into steps: "First backend, then UI"
2. Request parallel: "Use java-backend-developer for logic AND jsf-frontend-dev for UI"
3. Chain sequentially: "After backend is done, create the UI"

---

## Advanced Tips

### Tip 1: Use Agents for Code Reviews Before Commits

**Always run:**
```
"Use java-health-code-reviewer to review all changes before I commit"
```

### Tip 2: Combine Agents for Comprehensive Analysis

**Example:**
```
"Use java-ee-code-analyzer to understand the billing flow,
then use performance-optimizer to find bottlenecks,
then use java-health-code-reviewer to check for security issues"
```

### Tip 3: Leverage Domain-Specific Agents

**For healthcare-specific questions:**
```
✅ "Use health-informatics-architect to design HL7 integration"
✅ "Use quickbooks-healthcare-finance for revenue recognition rules"
```

### Tip 4: Use Plan Agent for Unfamiliar Features

**When starting new complex work:**
```
✅ "Use Plan agent to design the patient transfer workflow"
    → Creates detailed implementation plan
    → Identifies all files to modify
    → Proposes architecture approach
```

---

## Integration with HMIS Workflows

### Workflow: Pre-Commit Checklist

1. **Code Review:** `java-health-code-reviewer` - Check security, compliance, backward compatibility
2. **Performance Check:** `performance-optimizer` - Verify no N+1 queries introduced
3. **Build Test:** `devops-troubleshooter` - Ensure build passes (if Java changes)

### Workflow: Bug Fix

1. **Investigation:** `java-ee-code-analyzer` - Understand root cause
2. **Fix:** Appropriate agent (backend or frontend)
3. **Review:** `java-health-code-reviewer` - Verify fix doesn't break other features

### Workflow: New Module Development

1. **Planning:** `Plan` - Design architecture
2. **Backend:** `java-backend-developer` - Entities, DTOs, controllers
3. **UI:** `jsf-frontend-dev` - XHTML views
4. **Optimization:** `performance-optimizer` - Optimize queries
5. **Review:** `java-health-code-reviewer` - Final review

---

## Conclusion

Using the right agent for each task ensures:
- ✅ Faster development (specialized expertise)
- ✅ Better code quality (domain-specific reviews)
- ✅ Fewer mistakes (appropriate guardrails)
- ✅ Consistent patterns (agent follows project rules)

**Remember:** When in doubt, ask Claude "Which agent should handle this?" or explicitly specify the agent you want to use.

---

## Additional Resources

- [CLAUDE.md](../CLAUDE.md) - Project-wide Claude Code configuration
- [Developer Documentation](../developer_docs/) - Technical implementation guides
- [Wiki Publishing Guide](../developer_docs/github/wiki-publishing.md) - How to publish this as a wiki article

---

**Last Updated:** 2025-12-25
**Maintainer:** HMIS Development Team
