# Code Conventions

## Overview

This document outlines the coding standards, conventions, and best practices for the HMIS (Hospital Management Information System) project. Following these conventions ensures code consistency, maintainability, and prevents common pitfalls.

**Target Audience:** Developers contributing to the HMIS codebase

**Last Updated:** 2025-01-23

---

## Table of Contents

1. [General Principles](#general-principles)
2. [Java Backend Conventions](#java-backend-conventions)
3. [JSF Frontend Conventions](#jsf-frontend-conventions)
4. [Database Conventions](#database-conventions)
5. [Git & Version Control](#git--version-control)
6. [Testing & Build](#testing--build)
7. [Deployment & Configuration](#deployment--configuration)
8. [Security & Privileges](#security--privileges)
9. [Performance & Optimization](#performance--optimization)

---

## General Principles

### Do NOT Auto-Execute Commands

**🚨 CRITICAL:** Never commit, build, run, or push code unless explicitly requested by the user.

- Wait for user confirmation before executing Git operations
- Wait for user confirmation before running Maven builds
- Do not proactively run deployment commands

### Avoid Over-Engineering

- Only make changes that are directly requested or clearly necessary
- Keep solutions simple and focused
- Don't add features, refactor code, or make "improvements" beyond what was asked
- A bug fix doesn't need surrounding code cleaned up
- Don't add docstrings, comments, or type annotations to code you didn't change
- Don't add error handling, fallbacks, or validation for scenarios that can't happen
- Don't create helpers, utilities, or abstractions for one-time operations

### Backward Compatibility

**🚨 CRITICAL:** Maintain backward compatibility at all times.

- Never "fix" intentional typos (e.g., `purcahseRate`) - these exist for database compatibility
- Never rename composite components without checking ALL usage
- Never modify existing constructors - only add new ones
- Never change method signatures that other code depends on

---

## Java Backend Conventions

### DTO (Data Transfer Object) Implementation

**Purpose:** DTOs improve performance by loading only necessary data instead of full entity graphs.

#### Rule 1: Never Modify Existing Constructors

```java
// ❌ WRONG - Modifying existing constructor
public StockDTO(Long id, String itemName) {
    // Changed parameters - BREAKS COMPILATION!
}

// ✅ CORRECT - Add new constructor, keep existing
public StockDTO(Long id, String itemName) {
    // Original constructor - NEVER CHANGE
}

public StockDTO(Long id, String itemName, Double stockQty, Date expiry) {
    // New constructor with additional fields
}
```

#### Rule 2: Use Direct DTO Queries

**Always use JPQL constructor queries instead of entity-to-DTO conversion loops.**

```java
// ❌ WRONG - Entity conversion loop (inefficient)
List<Stock> stocks = stockFacade.findByJpql(sql, params);
List<StockDTO> dtos = new ArrayList<>();
for (Stock stock : stocks) {
    StockDTO dto = new StockDTO(stock.getField1(), stock.getField2());
    dtos.add(dto);
}

// ✅ CORRECT - Direct DTO query
String jpql = "SELECT new com.divudi.core.data.dto.StockDTO("
    + "s.id, "
    + "s.itemBatch.item.name, "
    + "s.itemBatch.item.code, "
    + "s.stock) "
    + "FROM Stock s WHERE ...";

List<StockDTO> dtos = (List<StockDTO>) facade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
```

#### Rule 3: Use IDs for Navigation

**Use IDs and names instead of entity references in DTOs for navigation support.**

```java
// ❌ WRONG - Entity objects in DTOs
public class OpdSaleSummaryDTO {
    private Category category;  // Defeats DTO purpose
    private Item item;          // Loads entity graph
}

// ✅ CORRECT - IDs and names
public class OpdSaleSummaryDTO {
    private Long categoryId;      // For navigation
    private String categoryName;  // For display
    private Long itemId;          // For navigation
    private String itemName;      // For display
}
```

#### Rule 4: Dual Property Pattern

**Maintain both DTO and Entity properties for backward compatibility.**

```java
@Named
@SessionScoped
public class PharmacySaleController {

    private Stock stock;              // Keep for business logic
    private StockDTO selectedStockDto; // Add for UI display

    public void setSelectedStockDto(StockDTO dto) {
        this.selectedStockDto = dto;
        // Automatically convert DTO to entity
        if (dto != null) {
            this.stock = stockFacade.find(dto.getId());
        }
    }
}
```

### Autocomplete Components

**Always use DTOs for autocomplete components to improve performance.**

See: [DTO Autocomplete Pattern](developer_docs/dto/autocomplete-dto-pattern.md)

Key points:
- Create DTO-returning methods in controllers
- Use JSF converters for DTO serialization
- Limit result sets (typically 20-50 items)
- Calculate aggregates only for small result sets

---

## JSF Frontend Conventions

### Page Structure

**Use full HTML documents with proper DOCTYPE and namespaces.**

```xml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:p="http://primefaces.org/ui"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets">
<h:head>
    <title>Page Title</title>
</h:head>
<h:body>
    <ui:composition template="/resources/template/template.xhtml">
        <ui:define name="content">
            <h:form>
                <!-- Page content -->
            </h:form>
        </ui:define>
    </ui:composition>
</h:body>
</html>
```

**Key Rules:**
- One logical form per page
- Escape XML entities (`&amp;`, `&lt;`, `&gt;`)
- Template inside `h:body`
- Use `h:outputText` instead of HTML headings (h1-h6)

### AJAX Updates

**🚨 CRITICAL:** AJAX update targets MUST be JSF components, not plain HTML elements.

```xml
<!-- ❌ WRONG - Plain HTML with id -->
<div id="stockSelection">
    <!-- content -->
</div>
<p:commandButton update="stockSelection" />

<!-- ✅ CORRECT - JSF component with id -->
<h:panelGroup id="stockSelection">
    <div>
        <!-- content -->
    </div>
</h:panelGroup>
<p:commandButton update="stockSelection" />
```

**Acceptable JSF Components for AJAX:**
- `h:panelGroup` - Lightweight wrapper (recommended)
- `p:outputPanel` - PrimeFaces panel
- `h:div` - HTML div as JSF component
- `h:form` - For entire form sections
- `p:panel` - Full-featured panel

### Conditional Rendering

**🚨 GOLDEN RULE:** NEVER put a `rendered` attribute on a component that has an `id` and will be updated via AJAX.

```xml
<!-- ❌ WRONG - ID on conditionally rendered component -->
<h:panelGroup id="staffDetails" rendered="#{bean.staff != null}">
    <h:outputText value="#{bean.staff.name}" />
</h:panelGroup>
<p:ajax update="staffDetails" /> <!-- Fails silently -->

<!-- ✅ CORRECT - ID always rendered, content conditional -->
<h:panelGroup id="staffDetails">
    <h:panelGroup rendered="#{bean.staff != null}">
        <h:outputText value="#{bean.staff.name}" />
    </h:panelGroup>
</h:panelGroup>
<p:ajax update="staffDetails" /> <!-- Works reliably -->
```

### UI Development

**When UI improvements are requested, make ONLY frontend changes.**

- ❌ Do NOT add new controller properties or methods
- ❌ Do NOT add backend dependencies
- ❌ Do NOT introduce filteredValues or globalFilter
- ✅ Use existing controller properties and methods
- ✅ Stick to HTML/CSS styling
- ✅ Modify PrimeFaces component attributes
- ✅ Improve layout and visual design

### Button Conventions

**Use consistent button styles and icons across the application.**

| Action | Style Class | Icon | Label |
|--------|-------------|------|-------|
| Process/Generate | `ui-button-warning` | `pi pi-play` | Process |
| Export Excel | `ui-button-success` | `pi pi-file-excel` | Export to Excel |
| Print | `ui-button-info` | `pi pi-print` | Print Report |
| Approve/Complete | `ui-button-success` | `pi pi-check` | Approve |
| Cancel/Close | `ui-button-danger ui-button-outlined` | `fas fa-times` | Cancel |
| Navigation | `ui-button-info ui-button-outlined` | Workflow-specific | Descriptive |

**Icon Guidelines:**
- Primary library: PrimeFaces `pi` icons
- Use Font Awesome `fas` only when no `pi` equivalent exists
- Always pair icons with text labels
- Provide `title` attributes for accessibility

### Forms and Input

- Align labels with `p:outputLabel` + `for` attributes
- Reuse controller state - avoid duplicating filters
- Heavy operations (reports, exports) use `ajax="false"`
- Always include descriptive `title` attributes

---

## Database Conventions

### MySQL Credentials

**🚨 CRITICAL:** MySQL credentials MUST NEVER be committed to git.

**Storage Locations:**
- **Windows:** `C:\Credentials\credentials.txt`
- **Linux/Mac:** `~/.config/hmis/credentials.txt`

**Format:**
```
JNDI Name: jdbc/coop
URL - jdbc:mysql://localhost:3306/coop?zeroDateTimeBehavior=CONVERT_TO_NULL
User - root
Password - [password]
```

### Sign Conventions for Pharmacy Transfers

**🚨 CRITICAL:** For pharmacy transfer ISSUE transactions, cost flows in the SAME direction as stock.

#### Quick Reference

| Field | Sign for Transfer ISSUE | Example | Rationale |
|-------|-------------------------|---------|-----------|
| quantity | NEGATIVE | -10 packs | Stock leaves issuing dept |
| lineCostRate | POSITIVE | 2.7273 | Rate is intrinsic property |
| lineCost | NEGATIVE | -27.273 | Cost burden relieved |
| totalCost | NEGATIVE | -27.273 | Cost burden relieved |
| Bill.netTotal | POSITIVE | 100.0 | Revenue received |
| Margin | POSITIVE | 127.273 | Revenue - Cost |

#### Core Rules

1. **Rates are always positive** (unsigned intrinsic properties)
2. **Costs mirror quantity sign** (negative for issue, positive for receive)
3. **Revenue has opposite sign of quantity** (positive for issue, negative for billing)
4. **Use idempotent sign normalization** (always `.abs()` before applying sign)

```java
// ✅ CORRECT - Idempotent pattern
BigDecimal absQtyInUnits = qtyInUnits.abs();
f.setLineCost(BigDecimal.ZERO.subtract(costRate.multiply(absQtyInUnits)));

// ❌ WRONG - Not idempotent
f.setLineCost(BigDecimal.ZERO.subtract(costRate.multiply(qtyInUnits)));
```

### Database Tables

**Key Tables:**
- `bill` - Main transaction table
- `billitem` - Line items
- `billitemfinancedetails` - Financial details (quantities, costs, values)
- `pharmaceuticalbillitem` - Pharmacy-specific data

**Important Fields:**
- `BillItemFinanceDetails.quantity` - Quantity in packs (signed)
- `BillItemFinanceDetails.quantityByUnits` - Quantity in units (signed)
- `PharmaceuticalBillItem.qty` - Always in units, NEVER packs
- `PharmaceuticalBillItem.purchaseRate` - Always positive
- `PharmaceuticalBillItem.retailRate` - Always positive

---

## Git & Version Control

### Commit Conventions

**Always include issue closing keywords in commit messages:**
- `Closes #issueNumber` - for general issue resolution
- `Fixes #issueNumber` - for bug fixes
- `Resolves #issueNumber` - alternative to closes

**Example:**
```
Add flexible persistence.xml configuration workflow

Closes #14011

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

### Branch Management

- Feature branches start with `claude/` prefix
- Branch names end with session ID
- Always push to designated feature branch
- Never force push to main/master

### Pre-Push Checklist

**🚨 CRITICAL:** Before any push, verify `persistence.xml` configuration.

1. Check if `persistence.xml` was modified:
   ```bash
   git status | grep persistence.xml
   ```

2. Verify datasource configuration:
   ```bash
   grep "jta-data-source" src/main/resources/META-INF/persistence.xml
   ```

3. Ensure environment variables are used:
   ```xml
   <!-- ✅ CORRECT -->
   <jta-data-source>${JDBC_DATASOURCE}</jta-data-source>
   <jta-data-source>${JDBC_AUDIT_DATASOURCE}</jta-data-source>

   <!-- ❌ WRONG -->
   <jta-data-source>jdbc/coop</jta-data-source>
   <jta-data-source>jdbc/ruhunuAudit</jta-data-source>
   ```

---

## Testing & Build

### Maven Commands

**Preferred approach:** Use auto-detection script
```bash
./detect-maven.sh test
```

**Fallback:** Try these in order:
1. Standard Maven (if in PATH): `mvn test`
2. NetBeans bundled Maven (machine-specific)
3. Maven wrapper: `./mvnw test` (Linux/Mac) or `./mvnw.cmd test` (Windows)

### Testing Requirements

- **Run tests before committing** when making Java changes
- **JSF-only changes** (XHTML files) do not require compilation or testing
- **Never run compile** unless explicitly requested by user

**When to Test:**
- ✅ Modified Java classes
- ✅ Modified entity relationships
- ✅ Modified business logic
- ❌ Modified only XHTML/JSF files
- ❌ Modified only CSS/styling

---

## Deployment & Configuration

### Persistence Configuration

**File:** `src/main/resources/META-INF/persistence.xml`

**🚨 CRITICAL Rules:**
1. ALWAYS use environment variables: `${JDBC_DATASOURCE}`, `${JDBC_AUDIT_DATASOURCE}`
2. NEVER commit hardcoded JNDI names: `jdbc/coop`, `jdbc/ruhunuAudit`
3. Verify before EVERY push to GitHub
4. Comment out or remove hardcoded DDL generation paths

**Why This Matters:**
- Hardcoded values break QA deployments
- Automated deployment cannot find hardcoded datasources
- Application won't start on QA/production servers
- Requires manual intervention to fix

### Environment Variables

**Local Development:**
- Configure local Payara server JNDI resources
- Or use local `.gitignore`d copy for development

**QA/Production:**
- Managed by deployment automation
- Set via environment variables

---

## Security & Privileges

### Privilege System

**Location:** `Privileges.java` enum

**Core Principles:**
1. Declare privileges in enum without reordering
2. Check privileges via `webUserController.hasPrivilege(...)`
3. Assign privileges through admin interface (never directly in database)
4. Name privileges descriptively

**Common Privileges:**
- `StockTransactionViewRates` - View rates/values in stock transactions
- `PharmacyTransferViewRates` - View rates/values in transfer reports
- `Developers` - Toggle all bill formats for QA

### Implementation

```java
// Controller
if (webUserController.hasPrivilege(Privileges.StockTransactionViewRates)) {
    // Show rate information
}
```

```xml
<!-- XHTML -->
<p:column headerText="Rate"
          rendered="#{webUserController.hasPrivilege('StockTransactionViewRates')}">
    <h:outputText value="#{item.rate}" />
</p:column>
```

### Adding New Privileges

1. Check if existing privilege matches behavior (reuse if possible)
2. Add to appropriate section in `Privileges.java` enum
3. Update admin privilege maintenance page
4. Extend `UserPrivilageController.createPrivilegeHolderTreeNodes()`
5. Document in release notes

---

## Performance & Optimization

### DTO Usage

**Use DTOs for:**
- All autocomplete components
- Large data tables/reports
- Any query with 3+ entity relationships
- Heavy entities (Stock, Bill, Patient)

**Performance Benefits:**
- 50-80% faster query execution
- 60-90% less memory usage
- Eliminates N+1 queries
- Better user experience

### Query Optimization

**Best Practices:**
1. Always use indexes on frequently queried columns
2. Limit result sets in development queries
3. Use EXPLAIN to understand query execution plans
4. Avoid SELECT * in production code
5. Use `findLightsByJpql()` for DTO constructor queries

### JPQL Patterns

```java
// ✅ CORRECT - Direct DTO construction
String jpql = "SELECT new com.divudi.core.data.dto.StockDTO("
    + "s.id, s.itemBatch.item.name, s.stock) "
    + "FROM Stock s WHERE s.department = :dept";

List<StockDTO> dtos = (List<StockDTO>) facade.findLightsByJpql(
    jpql, params, TemporalType.TIMESTAMP, 20
);

// ❌ WRONG - Entity query with conversion
List<Stock> stocks = facade.findByJpql(jpql, params);
List<StockDTO> dtos = convertToDTOs(stocks);
```

---

## Common Pitfalls to Avoid

### 1. Breaking Changes to DTOs

❌ **DON'T** modify existing constructors
❌ **DON'T** remove existing attributes
❌ **DON'T** change method signatures

✅ **DO** add new constructors
✅ **DO** add new attributes
✅ **DO** add new methods

### 2. AJAX Update Failures

❌ **DON'T** use plain HTML elements for AJAX targets
❌ **DON'T** put `rendered` on components with IDs used in AJAX

✅ **DO** use JSF components (h:panelGroup, p:outputPanel)
✅ **DO** wrap conditional content inside always-rendered containers

### 3. Hardcoded Configuration

❌ **DON'T** commit hardcoded JNDI datasource names
❌ **DON'T** commit database credentials
❌ **DON'T** commit hardcoded file paths

✅ **DO** use environment variables
✅ **DO** store credentials outside repository
✅ **DO** verify persistence.xml before every push

### 4. Sign Convention Violations

❌ **DON'T** negate rates (rates are always positive)
❌ **DON'T** forget to use absolute value before applying sign
❌ **DON'T** use negative quantity directly for revenue calculation

✅ **DO** keep rates positive (intrinsic properties)
✅ **DO** use idempotent sign normalization
✅ **DO** use absolute value for revenue (always positive)

---

## Wiki Publishing

**🚨 CRITICAL:** When creating user documentation, ALWAYS publish to GitHub Wiki immediately.

**Workflow:**
1. Create markdown in `wiki-docs/` directory
2. Commit to feature branch
3. **IMMEDIATELY** publish to GitHub Wiki (don't wait for PR merge)

**Why Immediate Publication:**
- QA testers need documentation during testing
- Stakeholders can review features immediately
- Documentation available during PR review

See: [Wiki Publishing Guide](developer_docs/github/wiki-publishing.md)

---

## References

### Documentation
- [DTO Implementation Guidelines](developer_docs/dto/implementation-guidelines.md)
- [JSF AJAX Update Guidelines](developer_docs/jsf/ajax-update-guidelines.md)
- [UI Development Handbook](developer_docs/ui/comprehensive-ui-guidelines.md)
- [MySQL Developer Guide](developer_docs/database/mysql-developer-guide.md)
- [Git Commit Conventions](developer_docs/git/commit-conventions.md)
- [Persistence Verification](developer_docs/deployment/persistence-verification.md)

### Architecture
- [Cost Accounting Sign Conventions](developer_docs/pharmacy/cost-accounting-sign-conventions.md)
- [Privilege System](developer_docs/security/privilege-system.md)
- [Icon Management](developer_docs/ui/icon-management.md)

---

## Document History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-01-23 | Initial comprehensive code conventions document |

---

**Maintainer:** HMIS Development Team
**Contact:** https://github.com/hmislk/hmis/issues

---

*This document is part of the HMIS project documentation. For the latest version, see: https://github.com/hmislk/hmis/wiki/Code-Conventions*
