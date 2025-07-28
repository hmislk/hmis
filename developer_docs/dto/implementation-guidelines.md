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

## Common Pitfalls to Avoid
1. **Changing existing constructor signatures** → Compilation errors in dependent code
2. **Converting entities to DTOs in loops** → Performance degradation
3. **Removing entity properties used by business logic** → Runtime failures  
4. **Not using `findLightsByJpql()`** → Missing DTO optimization
5. **Forgetting to handle null entity relationships** → NullPointerExceptions in queries

## Testing DTO Changes
Before committing DTO changes:
1. **Compile the entire project** to check for breaking changes
2. **Test the specific feature** that uses the new DTOs
3. **Verify existing functionality** still works (entities for business logic)
4. **Check performance improvements** compared to entity approach
