# DTO Implementation Guidelines

## CRITICAL RULES: Avoid Breaking Changes

When implementing DTOs to replace entity objects in UI/display components, follow these strict rules to prevent compilation errors and maintain backward compatibility:

### 1. NEVER Modify Existing Constructors or Attributes
- **❌ DO NOT** change parameters of existing constructors
- **❌ DO NOT** remove existing constructors  
- **❌ DO NOT** modify existing class attributes/fields
- **❌ DO NOT** change method signatures that other code depends on
- **✅ ONLY ADD** new constructors, new attributes, new methods

### 2. Use Direct DTO Queries - No Entity Conversion
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
String jpql = "SELECT new com.divudi.core.data.dto.StockDTO("
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

// 🚨 CRITICAL: Use findLightsByJpql() with cast for DTO constructor queries
List<StockDTO> dtos = (List<StockDTO>) facade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
```

### 2a. Navigation Pattern: Use IDs and Names Instead of Entity References

**🚨 CRITICAL PATTERN for Navigation Support:**

When DTOs need to support navigation (e.g., clicking on a row to view details), use **IDs and String names** instead of full entity references.

**❌ WRONG - Including entity objects in DTOs:**
```java
public class OpdSaleSummaryDTO {
    private Category category;  // Don't do this - defeats DTO purpose
    private Item item;          // Don't do this - loads entity graph
    private String itemName;
    private Double total;
}
```

**✅ CORRECT - Use IDs and names for navigation:**
```java
public class OpdSaleSummaryDTO {
    private Long categoryId;      // For navigation
    private String categoryName;  // For display
    private Long itemId;          // For navigation
    private String itemName;      // For display
    private Double total;

    // Constructor for JPQL query
    public OpdSaleSummaryDTO(Long categoryId, String categoryName,
                              Long itemId, String itemName, Double total) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.itemId = itemId;
        this.itemName = itemName;
        this.total = total;
    }
}
```

**JPQL Query Pattern:**
```java
String jpql = "SELECT new com.divudi.core.data.dto.OpdSaleSummaryDTO("
    + "bi.item.category.id, "           // Category ID for navigation
    + "bi.item.category.name, "         // Category name for display
    + "bi.item.id, "                    // Item ID for navigation
    + "bi.item.name, "                  // Item name for display
    + "sum(bi.netValue)) "              // Aggregated data
    + "FROM BillItem bi "
    + "WHERE ... "
    + "GROUP BY bi.item.category.id, bi.item.category.name, bi.item.id, bi.item.name";

List<OpdSaleSummaryDTO> dtos = (List<OpdSaleSummaryDTO>) facade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
```

**Navigation Controller Pattern:**
```java
// In controller - load full entity only when navigating
public String navigateToDetails(OpdSaleSummaryDTO dto) {
    // Load full entities only when needed for detail page
    if (dto.getCategoryId() != null) {
        this.category = categoryFacade.find(dto.getCategoryId());
    }
    if (dto.getItemId() != null) {
        this.item = itemFacade.find(dto.getItemId());
    }
    return "/detail_page?faces-redirect=true";
}
```

**Benefits:**
- ✅ DTOs remain lightweight (no entity graph loading)
- ✅ Navigation still works (using IDs to load entities on demand)
- ✅ Display names available without entity access
- ✅ Database aggregation stays efficient
- ✅ Memory footprint minimized

### 3. Safe Entity Property Changes
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

### 4. XHTML Selection Binding Pattern
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

### 5. Constructor Addition Guidelines

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

### 6. Reference Implementation Pattern

**Follow this pattern for efficient DTO implementation:**

1. **Identify the display use case** - what data does the UI actually need?
2. **Add new fields to DTO** (never remove existing ones)
3. **Add new constructor** with required fields for the use case
4. **Create direct JPQL query** using the new constructor
5. **Add new controller properties** for DTO selection (keep existing entity properties)
6. **Update XHTML** to use DTO properties for display
7. **Maintain entity properties** for business logic operations

### 7. Performance Benefits
Direct DTO queries provide:
- **Memory efficiency**: Only loads required display fields
- **Database efficiency**: Single optimized query instead of entity + conversion
- **Network efficiency**: Reduced data transfer
- **Compilation safety**: No breaking changes to existing code

### 8. Example: StockSearchService Reference
See `StockSearchService.findStockDtos()` method for the correct pattern of direct DTO querying.

### 9. CRITICAL: Correct Facade Method for DTO Constructor Queries

**🚨 ALWAYS use `findLightsByJpql()` with explicit cast for DTO constructor queries:**

```java
// ✅ CORRECT - DTO constructor query
String jpql = "SELECT new com.divudi.core.data.dto.PharmacySaleByBillTypeDTO("
    + "i.bill.billTypeAtomic.label, "
    + "sum(i.pharmaceuticalBillItem.qty)) "
    + "FROM BillItem i "
    + "WHERE ... "
    + "GROUP BY i.bill.billTypeAtomic.label";

// MUST use findLightsByJpql() with cast
salesByBillType = (List<PharmacySaleByBillTypeDTO>) getBillItemFacade().findLightsByJpql(jpql, m, TemporalType.TIMESTAMP);
```

**❌ WRONG facade methods for DTO constructor queries:**
```java
// DON'T USE THESE for DTO constructor queries:
facade.findByJpql(jpql, params)           // Wrong return type
facade.findAggregates(jpql, params)       // For Object[] results only
facade.findLightsByJpql(jpql, params)     // Missing TemporalType when using Date parameters
```

**Why `findLightsByJpql()` is required:**
- Optimized for lightweight objects (DTOs)
- Handles constructor queries correctly
- Supports temporal parameters for Date/Timestamp filtering
- Returns properly typed collections

## Common Pitfalls to Avoid
1. **Changing existing constructor signatures** → Compilation errors in dependent code
2. **Converting entities to DTOs in loops** → Performance degradation
3. **Removing entity properties used by business logic** → Runtime failures
4. **Using wrong facade method for DTO queries** → `findByJpql()` instead of `findLightsByJpql()`
5. **Missing explicit cast** → Type safety issues with DTO constructor queries
6. **Forgetting to handle null entity relationships** → NullPointerExceptions in queries
7. **Using `Object` type for primitive boolean parameters** → Silent query failures (see Type Mismatch section below)

## 🚨 CRITICAL: Type Mismatch in DTO Constructor Queries

### The Problem: Primitive Boolean Auto-Boxing Limitation

**JPQL cannot auto-box primitive `boolean` to `Object` in DTO constructor expressions.**

This causes **silent failures** where:
- COUNT query returns results (no DTO construction needed)
- DTO query returns 0 results (constructor match fails silently)
- No exception is thrown

### Example of the Problem

**Entity with primitive booleans:**
```java
@Entity
public class Bill {
    private boolean cancelled;      // primitive boolean
    private boolean billClosed;     // primitive boolean
    private boolean fullyIssued;    // primitive boolean
}
```

**❌ WRONG - DTO Constructor using Object:**
```java
public PharmacyPurchaseOrderDTO(
        Long billId,
        String deptId,
        Object cancelled,        // ❌ WRONG - Object cannot receive primitive boolean from JPQL
        Object billClosed,       // ❌ WRONG
        Object fullyIssued) {    // ❌ WRONG
    this.cancelled = cancelled != null ? (Boolean) cancelled : false;
    // ...
}
```

**JPQL Query:**
```java
String jpql = "SELECT new com.divudi.core.data.dto.PharmacyPurchaseOrderDTO("
    + "b.id, "
    + "b.deptId, "
    + "b.cancelled, "        // primitive boolean from entity
    + "b.billClosed, "       // primitive boolean from entity
    + "b.fullyIssued) "      // primitive boolean from entity
    + "FROM Bill b WHERE ...";
```

**Result:** Query returns 0 results even though COUNT shows 1 record exists!

### ✅ CORRECT Solutions

**Solution 1: Use Boolean Wrapper Type (Recommended)**
```java
// ✅ CORRECT - Use Boolean wrapper type
public PharmacyPurchaseOrderDTO(
        Long billId,
        String deptId,
        Boolean cancelled,      // ✅ JPQL can auto-box primitive boolean → Boolean
        Boolean billClosed,     // ✅ Works correctly
        Boolean fullyIssued) {  // ✅ Works correctly
    this.cancelled = cancelled != null ? cancelled : false;
    // ...
}
```

**Solution 2: Explicit CASE in JPQL (if you cannot change constructor)**
```java
String jpql = "SELECT new com.divudi.core.data.dto.PharmacyPurchaseOrderDTO("
    + "b.id, "
    + "b.deptId, "
    + "CASE WHEN b.cancelled = true THEN true ELSE false END, "    // Forces Boolean wrapper
    + "CASE WHEN b.billClosed = true THEN true ELSE false END, "
    + "CASE WHEN b.fullyIssued = true THEN true ELSE false END) "
    + "FROM Bill b WHERE ...";
```

### Type Compatibility Matrix

| Entity Type | DTO Constructor Parameter | JPQL Auto-Boxing | Result |
|-------------|---------------------------|------------------|---------|
| `boolean` (primitive) | `Boolean` (wrapper) | ✅ YES | Works |
| `boolean` (primitive) | `Object` | ❌ NO | Silent failure |
| `Boolean` (wrapper) | `Boolean` (wrapper) | ✅ YES | Works |
| `Boolean` (wrapper) | `Object` | ✅ YES | Works |
| `int` (primitive) | `Integer` (wrapper) | ✅ YES | Works |
| `int` (primitive) | `Object` | ❌ NO | Silent failure |

### Debugging Silent Failures

When COUNT returns results but DTO query returns 0:

1. **Check constructor parameter types** - Use wrapper types for primitives
2. **Verify parameter count** - Must match exactly (13 params in query = 13 in constructor)
3. **Check parameter order** - Must match query SELECT order exactly
4. **Verify type compatibility** - No primitive → Object conversions
5. **Test with simple query** - Remove LEFT JOINs temporarily to isolate issue

### LEFT JOIN Considerations for Null Safety

When accessing nested properties that may be null:

**❌ WRONG - Direct property access (NPE if null):**
```java
String jpql = "SELECT new DTO("
    + "b.cancelledBill.createdAt, "              // NPE if cancelledBill is null
    + "b.cancelledBill.creater.name) "           // NPE if cancelledBill or creater is null
    + "FROM Bill b WHERE ...";
```

**✅ CORRECT - Use LEFT JOIN with aliases:**
```java
String jpql = "SELECT new DTO("
    + "cb.createdAt, "                           // Safe - cb can be null from LEFT JOIN
    + "COALESCE(cancellerPerson.name, '')) "    // Safe - COALESCE handles null
    + "FROM Bill b "
    + "LEFT JOIN b.cancelledBill cb "
    + "LEFT JOIN cb.creater.webUserPerson cancellerPerson "
    + "WHERE ...";
```

### Best Practices Summary

1. **Always use wrapper types** (`Boolean`, `Integer`, `Long`) for DTO constructor parameters
2. **Use LEFT JOIN** for nullable relationships to prevent cartesian products
3. **Use COALESCE** for nullable String fields to provide default values
4. **Test COUNT separately** to verify data exists before troubleshooting DTO construction
5. **Add debug logging** when implementing new DTO queries to catch silent failures early
6. **Match parameter types exactly** - don't rely on implicit conversions with `Object`

## Navigation-Level DTO/Entity Selection
When implementing dual DTO/Entity approach:

### Navigation Structure
1. **Separate navigation entries** for Entity and DTO versions
   - Example: "Transfer Reports (Entity)" and "Transfer Reports (DTO)" 
   - Double the navigation buttons, but clearer separation of concerns
2. **Each page has single purpose** - either Entity OR DTO, not both
3. **Simple Fill button** on each page - no switching within page

### Page Design Principles
1. **Entity page**: Contains only entity-based Fill button and entity-specific actions
2. **DTO page**: Contains only DTO-based Fill button and DTO-specific actions  
3. **No cross-navigation buttons** within pages - navigation choice made at menu level
4. **Clear page headers** indicating which approach (Entity vs DTO)

### Recommended Naming Convention
- Original page: `feature_name.xhtml` (Entity-based for backward compatibility)
- DTO page: `feature_name_dto.xhtml` (DTO-based, optimized)
- Navigation labels: "Feature Name" and "Feature Name (DTO - Recommended)"

### Configuration
1. **DTO approach should be the default** where applicable
2. **Label DTO version clearly** in navigation to indicate it's the recommended approach
3. **Maintain entity version** for backward compatibility and business logic needs

### Implementation Example

**Navigation Configuration (pharmacy_analytics.xhtml):**
```xml
<!-- Entity Version - Traditional -->
<p:commandButton rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Analytics - Show Transfer Issue by Bill')}" 
                 value="Transfer Issue by Bill (Entity)" 
                 action="#{reportsTransfer.navigateToTransferIssueByBill}" 
                 ajax="false" 
                 icon="fa fa-file-export" 
                 class="w-100"/>

<!-- DTO Version - Recommended (defaults to enabled) -->
<p:commandButton rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Analytics - Show Transfer Issue by Bill (DTO)', true)}" 
                 value="Transfer Issue by Bill (DTO - Fast)" 
                 action="/pharmacy/reports/disbursement_reports/pharmacy_report_transfer_issue_bill_dto?faces-redirect=true" 
                 ajax="false" 
                 icon="fa fa-rocket" 
                 class="w-100 ui-button-success"
                 title="High-performance DTO-based report - Recommended"/>
```

**Configuration Key Pattern:**
- **Entity version**: `'Feature Name'` (existing configuration)
- **DTO version**: `'Feature Name (DTO)'` with `true` as default value
- This allows administrators to disable DTO versions if needed while defaulting to enabled

**Resulting Navigation Menu Structure:**
```
Pharmacy Analytics → Disbursement Reports
├── Transfer Issue by Bill (Entity)           → pharmacy_report_transfer_issue_bill.xhtml
└── Transfer Issue by Bill (DTO - Fast)       → pharmacy_report_transfer_issue_bill_dto.xhtml
```

**Entity Page Content:**
- Single "Fill" button → `fillDepartmentTransfersIssueByBillEntity()`
- Excel/Print buttons specific to entity data
- Uses `#{reportsTransfer.transferBills}` for data binding

**DTO Page Content:**
- Single "Fill" button → `fillDepartmentTransfersIssueByBillDto()`  
- Excel/Print buttons specific to DTO data
- Uses `#{reportsTransfer.transferIssueDtos}` for data binding

**Controller Structure:**
```java
// Keep both properties for backward compatibility
private List<Bill> transferBills;              // For entity approach
private List<PharmacyTransferIssueDTO> transferIssueDtos; // For DTO approach

// Separate methods for each approach
public void fillDepartmentTransfersIssueByBillEntity() { ... }
public void fillDepartmentTransfersIssueByBillDto() { ... }

// Navigation control method
public boolean isTransferIssueDtoEnabled() { return true; }
```

**Benefits of Navigation-Level Selection:**
1. **Clear user choice** before entering report
2. **No switching confusion** within pages
3. **Easy configuration control** via controller methods
4. **Gradual migration path** - can disable DTO option if needed
5. **Performance awareness** - users can choose fast DTO version consciously

## Testing DTO Changes
Before committing DTO changes:
1. **Compile the entire project** to check for breaking changes
2. **Test the specific feature** that uses the new DTOs
3. **Verify existing functionality** still works (entities for business logic)
4. **Check performance improvements** compared to entity approach
5. **Test both DTO and Entity versions** if dual approach is implemented
