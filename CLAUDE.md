# Claude Code Configuration for HMIS Project

## Persistence.xml Database Configuration Workflow

### IMPORTANT: Automatic Git Push Behavior

When asked to push changes to GitHub, Claude should AUTOMATICALLY:

1. **Before pushing:**
   - Change current JNDI names → `${JDBC_DATASOURCE}` in persistence.xml
   - Change current audit JNDI names → `${JDBC_AUDIT_DATASOURCE}` in persistence.xml

2. **Push changes** to GitHub

3. **After pushing:**
   - Revert `${JDBC_DATASOURCE}` → back to the original JNDI name that was there before
   - Revert `${JDBC_AUDIT_DATASOURCE}` → back to the original audit JNDI name that was there before

### File Location
`src/main/resources/META-INF/persistence.xml`

### Variable JNDI Names
The JNDI names change based on environment and will be manually updated:
- Examples: `jdbc/asiri`, `jdbc/ruhunu`, `jdbc/coop`, etc.
- Always preserve whatever the current local configuration is

### Security Note
**NEVER commit actual JNDI names to the repository - always use environment variables in pushed commits**

### Key Principle
Use the **last choice in history** - whatever JNDI names are currently in the file should be restored after pushing.

## Git Commit Message Conventions

### Issue Closing Keywords
When pushing commits that resolve GitHub issues, include one of these keywords in the commit message:
- `Closes #issueNumber` - for general issue resolution
- `Fixes #issueNumber` - for bug fixes
- `Resolves #issueNumber` - alternative to closes

### Example Commit Messages
```
Add flexible persistence.xml configuration workflow

Closes #14011

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

### Auto-Close Behavior
When Claude pushes commits that complete an issue, automatically include the appropriate closing keyword in the commit message.

## GitHub Project Board Integration

### Project Information
- **Project**: "CareCode: HMIS Board" (Project #11)
- **Organization**: hmislk
- **URL**: https://github.com/orgs/hmislk/projects/11

### Required Authentication
Ensure GitHub CLI has project scope:
```bash
gh auth refresh --hostname github.com -s project
```

### Project Status Workflow
When working on issues, update their status in the project board:

#### 1. Starting Development
When beginning work on an issue:
```bash
# Add issue to project (if not already added)
gh project item-add 11 --owner hmislk --url https://github.com/hmislk/hmis/issues/ISSUE_NUMBER

# Move to "In Progress" status
gh project item-edit --project-id PVT_kwDOAHw-zs4ApMln --id PROJECT_ITEM_ID --field-id PVTSSF_lADOAHw-zs4ApMlnzggpN3k --single-select-option-id 47fc9ee4
```

#### 2. After Creating PR
When a pull request is created:
```bash
# Move to "Reviewing & Merging" status
gh project item-edit --project-id PVT_kwDOAHw-zs4ApMln --id PROJECT_ITEM_ID --field-id PVTSSF_lADOAHw-zs4ApMlnzggpN3k --single-select-option-id a087e083
```

#### 3. After Merge/Completion
When the issue is fully resolved:
```bash
# Move to "Done" status
gh project item-edit --project-id PVT_kwDOAHw-zs4ApMln --id PROJECT_ITEM_ID --field-id PVTSSF_lADOAHw-zs4ApMlnzggpN3k --single-select-option-id 1bab9178
```

### Status Option IDs
- **Backlog**: `8a5edcf1`
- **Current Sprint**: `1cbc1930`
- **To Do**: `fbaa9d0f`
- **In Progress**: `47fc9ee4`
- **Reviewing & Merging**: `a087e083`
- **Dev Completed**: `85d4d4c9`
- **Ready For Testing**: `f9564514`
- **Testing In Progress**: `63aa4183`
- **Done**: `1bab9178`

### Project Constants
- **Project ID**: `PVT_kwDOAHw-zs4ApMln`
- **Status Field ID**: `PVTSSF_lADOAHw-zs4ApMlnzggpN3k`

### Finding Project Item ID
To get the project item ID for an issue:
```bash
gh api graphql -f query='
{
  search(query: "repo:hmislk/hmis is:issue ISSUE_NUMBER", type: ISSUE, first: 1) {
    nodes {
      ... on Issue {
        number
        projectItems(first: 10) {
          nodes {
            id
            project {
              number
            }
          }
        }
      }
    }
  }
}'
```

### Automatic Workflow
Claude should automatically:
1. Add issues to the project when starting development
2. Move issues to "In Progress" when development begins
3. Move issues to "Reviewing & Merging" when PRs are created
4. Include comprehensive test plans in PR descriptions for QA team

## Maven Testing Commands

### Environment-Specific Maven Locations
Different development machines have Maven installed in different locations.

**Machine Detection**: Use `hostname` and `whoami` commands to identify the current machine.

#### Known Configurations:
- **cclap** (Computer: `<HOSTNAME>`, User: `<USERNAME>`):
  - Maven: `<NB_MAVEN_PATH>`
  - Payara Server: `<PAYARA_SERVER_PATH>` (Domain 1)
- **hiulap** (Computer: `_[TBD]_`, User: `_[TBD]_`): _[To be documented]_
- **hiud** (Computer: `_[TBD]_`, User: `_[TBD]_`): _[To be documented]_ 
- **ccd** (Computer: `_[TBD]_`, User: `_[TBD]_`): _[To be documented]_

### Testing Commands to Try (in order of preference)
When running tests, try these commands in order until one works:

1. **Standard Maven** (if in PATH):
   ```bash
   mvn test
   ```

2. **NetBeans Bundled Maven** (cclap):
   ```bash
   "<NB_MAVEN_PATH>" test
   ```

3. **Maven Wrapper** (if available):
   ```bash
   ./mvnw test        # Linux/Mac
   ./mvnw.cmd test    # Windows
   ```

4. **Specific Test Classes**:
   ```shell
   mvn test -Dtest="*BigDecimal*Test"
   ```

### Note for Claude
**PREFERRED APPROACH**: Use the auto-detection script:
1. **First try**: `./detect-maven.sh test` (automatically detects machine and uses correct Maven)
2. **If script fails**: Fall back to manual detection:
   - Run `hostname` and `whoami` to identify machine
   - Try standard `mvn test`
   - If Maven not found, use machine-specific path based on hostname
   - For `<HOSTNAME>` (cclap): Use `"<NB_MAVEN_PATH>" test`

**The detect-maven.sh script handles all this automatically and should be the first choice.**

## DTO Implementation Guidelines

### CRITICAL RULES: Avoid Breaking Changes

When implementing DTOs to replace entity objects in UI/display components, follow these strict rules to prevent compilation errors and maintain backward compatibility:

#### 1. NEVER Modify Existing Constructors or Attributes
- **❌ DO NOT** change parameters of existing constructors
- **❌ DO NOT** remove existing constructors  
- **❌ DO NOT** modify existing class attributes/fields
- **❌ DO NOT** change method signatures that other code depends on
- **✅ ONLY ADD** new constructors, new attributes, new methods

#### 2. Use Direct DTO Queries - No Entity Conversion
When replacing entities with DTOs in controllers:

**❌ WRONG APPROACH:**
```java
// DON'T DO THIS - Inefficient and resource-intensive
List<Stock> stocks = stockFacade.findByJpql(sql, params);
List<StockDTO> dtos = new ArrayList<>();
for (Stock stock : stocks) {
    StockDTO dto = new StockDTO(stock.getField1(), stock.getField2(), ...);
    dtos.add(dto);
}
```

**✅ CORRECT APPROACH:**
```java
// DO THIS - Direct DTO query from database
String sql = "SELECT new com.divudi.core.data.dto.StockDTO("
    + "s.id, "
    + "s.itemBatch.item.name, "
    + "s.itemBatch.item.code, "
    + "s.itemBatch.retailsaleRate, "
    + "s.stock, "
    + "s.itemBatch.dateOfExpire, "
    + "s.itemBatch.batchNo, "
    + "s.itemBatch.purcahseRate, "
    + "s.itemBatch.wholesaleRate) "
    + "FROM Stock s WHERE ...";
List<StockDTO> dtos = (List<StockDTO>) facade.findLightsByJpql(sql, params);
```

#### 3. Safe Entity Property Changes
When changing controller properties from entities to DTOs:

**❌ WRONG - Breaking existing functionality:**
```java
// This breaks other code that depends on the Stock entity
Stock stock; // Changed to StockDTO - BREAKS OTHER CODE!
```

**✅ CORRECT - Add new property, keep existing:**
```java
Stock stock;              // Keep existing for business logic
StockDTO selectedStockDto; // Add new for UI display
```

#### 4. XHTML Selection Binding Pattern
When updating XHTML to use DTOs:

**For dataTable with DTO data source:**
```xhtml
<p:dataTable value="#{controller.stockDtoList}" var="i" 
             selection="#{controller.selectedStockDto}">
    <p:column headerText="Name">
        <h:outputText value="#{i.itemName}" />
    </p:column>
</p:dataTable>
```

**Sync DTO selection with entity if needed:**
```java
public void setSelectedStockDto(StockDTO dto) {
    this.selectedStockDto = dto;
    // Load full entity only if needed for business operations
    if (dto != null) {
        this.stock = stockFacade.find(dto.getId());
    }
}
```

#### 5. Constructor Addition Guidelines

**When adding new DTO constructors:**

```java
// ✅ KEEP existing constructor intact
public StockDTO(Long id, String itemName, String code, String genericName,
                Double retailRate, Double stockQty, Date dateOfExpire) {
    // Original constructor - NEVER CHANGE
}

// ✅ ADD new constructors for additional use cases
public StockDTO(Long id, String itemName, String code, Double retailRate, 
                Double stockQty, Date dateOfExpire, String batchNo, 
                Double purchaseRate, Double wholesaleRate) {
    // New constructor with additional fields
}
```

#### 6. Reference Implementation Pattern

**Follow this pattern for efficient DTO implementation:**

1. **Identify the display use case** - what data does the UI actually need?
2. **Add new fields to DTO** (never remove existing ones)
3. **Add new constructor** with required fields for the use case
4. **Create direct JPQL query** using the new constructor
5. **Add new controller properties** for DTO selection (keep existing entity properties)
6. **Update XHTML** to use DTO properties for display
7. **Maintain entity properties** for business logic operations

#### 7. Performance Benefits
Direct DTO queries provide:
- **Memory efficiency**: Only loads required display fields
- **Database efficiency**: Single optimized query instead of entity + conversion
- **Network efficiency**: Reduced data transfer
- **Compilation safety**: No breaking changes to existing code

#### 8. Example: StockSearchService Reference
See `StockSearchService.findStockDtos()` method for the correct pattern of direct DTO querying.

### Common Pitfalls to Avoid
1. **Changing existing constructor signatures** → Compilation errors in dependent code
2. **Converting entities to DTOs in loops** → Performance degradation
3. **Removing entity properties used by business logic** → Runtime failures  
4. **Not using `findLightsByJpql()`** → Missing DTO optimization
5. **Forgetting to handle null entity relationships** → NullPointerExceptions in queries

### Testing DTO Changes
Before committing DTO changes:
1. **Compile the entire project** to check for breaking changes
2. **Test the specific feature** that uses the new DTOs
3. **Verify existing functionality** still works (entities for business logic)
4. **Check performance improvements** compared to entity approach

---
This behavior should persist across all Claude Code sessions for this project.