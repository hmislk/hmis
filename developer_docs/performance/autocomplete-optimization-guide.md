# Autocomplete Performance Optimization Guide

## Overview

This guide documents the performance optimizations applied to the Pharmacy Retail Sale autocomplete feature. These patterns can be applied to other autocomplete components throughout HMIS to achieve similar performance improvements.

**Performance Improvement Achieved**: 85-90% faster (from ~639-820ms to ~50-100ms)

---

## Problem Statement

### Original Issue
When selecting an item from a PrimeFaces autocomplete component, there was a noticeable delay (500-800ms) before focus moved to the next field. This was caused by:

1. **Database queries** to fetch full entity objects even though data was already in the DTO
2. **JSF converter** losing DTO fields during serialization/deserialization
3. **Unnecessary discount calculations** even when no discount scheme was active
4. **Lazy loading** of item instructions on every selection

---

## Solution Architecture

### Core Principles

1. **Use DTOs throughout** - Avoid converting to entities until absolutely necessary
2. **Cache autocomplete results** - Preserve full DTO data for the converter
3. **Skip unnecessary operations** - Use configuration and conditional logic
4. **Defer expensive operations** - Move to background or later in the workflow

---

## Implementation Steps

### Step 1: Enhance the DTO with Required Fields

**File**: `StockDTO.java` (or your equivalent DTO)

**Add fields needed for lightweight entity creation:**

```java
public class StockDTO implements Serializable {
    private Long id;              // Entity ID
    private Long itemBatchId;     // Foreign key to ItemBatch
    private Long itemId;          // Foreign key to Item
    private String itemName;
    private String code;
    private String batchNo;
    private Double retailRate;
    private Double stockQty;
    private Date dateOfExpire;
    private Boolean discountAllowed;  // Prevents DB query during discount calculation

    // ... existing fields ...
}
```

**Add a constructor for the optimized query:**

```java
// Constructor for optimized autocomplete (includes all required IDs)
public StockDTO(Long id, Long itemBatchId, Long itemId,
                String itemName, String code, String genericName,
                String batchNo, Double retailRate, Double stockQty,
                Date dateOfExpire, Boolean discountAllowed) {
    this.id = id;
    this.itemBatchId = itemBatchId;
    this.itemId = itemId;
    this.itemName = itemName;
    this.code = code;
    this.genericName = genericName;
    this.batchNo = batchNo;
    this.retailRate = retailRate;
    this.stockQty = stockQty;
    this.dateOfExpire = dateOfExpire;
    this.discountAllowed = discountAllowed;
}
```

**Add getters and setters for new fields.**

---

### Step 2: Update the Autocomplete Query

**File**: Your controller (e.g., `PharmacySaleController.java`)

**Change from entity query to DTO query:**

**❌ Before (returns entities):**
```java
public List<Stock> completeAvailableStock(String qry) {
    StringBuilder sql = new StringBuilder("SELECT i FROM Stock i ")
        .append("WHERE i.stock > :stockMin ")
        .append("AND i.department = :department ");

    return stockFacade.findByJpql(sql.toString(), parameters, 20);
}
```

**✅ After (returns DTOs):**
```java
public List<StockDTO> completeAvailableStockOptimizedDto(String qry) {
    StringBuilder sql = new StringBuilder("SELECT NEW com.divudi.core.data.dto.StockDTO(")
        .append("i.id, i.itemBatch.id, i.itemBatch.item.id, ")
        .append("i.itemBatch.item.name, i.itemBatch.item.code, i.itemBatch.item.vmp.name, ")
        .append("i.itemBatch.batchNo, i.itemBatch.retailsaleRate, i.stock, ")
        .append("i.itemBatch.dateOfExpire, i.itemBatch.item.discountAllowed) ")
        .append("FROM Stock i ")
        .append("WHERE i.stock > :stockMin ")
        .append("AND i.department = :department ");

    List<StockDTO> results = (List<StockDTO>) stockFacade.findLightsByJpql(
        sql.toString(), parameters, TemporalType.TIMESTAMP, 20);

    // Cache results for converter
    this.lastAutocompleteResults = results;

    return results;
}
```

**Key Points:**
- Use `SELECT NEW com.divudi.core.data.dto.StockDTO(...)` constructor expression
- Include ALL foreign key IDs (itemBatchId, itemId, etc.)
- Include fields needed for business logic (discountAllowed, batchNo, etc.)
- Use `findLightsByJpql()` for DTO queries, not `findByJpql()`
- Cache results in controller for converter to access

---

### Step 3: Cache Autocomplete Results

**File**: Your controller

**Add a cache field:**
```java
@Named
@SessionScoped
public class PharmacySaleController {

    private StockDTO stockDto;
    private List<StockDTO> lastAutocompleteResults; // Cache for converter

    // ... other fields ...
}
```

**Cache results in autocomplete method:**
```java
public List<StockDTO> completeAvailableStockOptimizedDto(String qry) {
    // ... build query ...

    List<StockDTO> results = (List<StockDTO>) stockFacade.findLightsByJpql(...);

    // Cache for converter
    this.lastAutocompleteResults = results;

    return results;
}
```

---

### Step 4: Fix the JSF Converter

**File**: Your controller (inner class or separate class)

**Problem**: Default converter creates new DTO with only ID, losing all other fields.

**❌ Before:**
```java
public static class StockDtoConverter implements Converter {
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        Long id = Long.valueOf(value);
        // Creates minimal DTO - LOSES itemBatchId, itemId, etc.
        StockDTO dto = new StockDTO();
        dto.setId(id);
        return dto;
    }
}
```

**✅ After:**
```java
public static class StockDtoConverter implements Converter {
    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        Long id = Long.valueOf(value);
        PharmacySaleController controller = (PharmacySaleController)
            context.getApplication().getELResolver()
                .getValue(context.getELContext(), null, "pharmacySaleController");

        if (controller != null) {
            // Search in cached autocomplete results - PRESERVES ALL FIELDS
            if (controller.lastAutocompleteResults != null) {
                for (StockDTO dto : controller.lastAutocompleteResults) {
                    if (id.equals(dto.getId())) {
                        return dto; // Return full DTO with all fields
                    }
                }
            }

            // Fallback: check currently selected
            if (controller.getStockDto() != null &&
                id.equals(controller.getStockDto().getId())) {
                return controller.getStockDto();
            }
        }

        // Last resort: minimal DTO (triggers database fetch)
        StockDTO dto = new StockDTO();
        dto.setId(id);
        return dto;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        if (value instanceof StockDTO) {
            return ((StockDTO) value).getId().toString();
        }
        return "";
    }
}
```

---

### Step 5: Create Lightweight Entities from DTO

**File**: Your controller

**Method to convert DTO to entity WITHOUT database query:**

```java
public Stock convertStockDtoToEntity(StockDTO stockDto) {
    if (stockDto == null || stockDto.getId() == null) {
        return null;
    }

    // Check if we have the necessary data for lightweight creation
    if (stockDto.getItemBatchId() != null && stockDto.getItemId() != null) {
        // Create lightweight Stock entity with data from DTO
        Stock stock = new Stock();
        stock.setId(stockDto.getId());
        stock.setStock(stockDto.getStockQty());

        // Create lightweight ItemBatch with data from DTO
        ItemBatch itemBatch = new ItemBatch();
        itemBatch.setId(stockDto.getItemBatchId());
        itemBatch.setRetailsaleRate(stockDto.getRetailRate());
        itemBatch.setDateOfExpire(stockDto.getDateOfExpire());
        itemBatch.setBatchNo(stockDto.getBatchNo());

        // Create lightweight Item (use concrete class like Amp)
        Item item = createMinimalItemFromDto(stockDto);

        // Link entities
        itemBatch.setItem(item);
        stock.setItemBatch(itemBatch);

        return stock; // NO DATABASE QUERY!
    }

    // Fallback: fetch from database if DTO lacks required data
    return stockFacade.find(stockDto.getId());
}

private Item createMinimalItemFromDto(StockDTO stockDto) {
    Amp item = new Amp(); // Use concrete class
    item.setId(stockDto.getItemId());
    item.setName(stockDto.getItemName());
    item.setCode(stockDto.getCode());
    item.setDiscountAllowed(stockDto.getDiscountAllowed() != null ?
        stockDto.getDiscountAllowed() : true);
    return item;
}
```

---

### Step 6: Skip Unnecessary Calculations

#### 6.1 Skip Discount Calculation When No Scheme Active

**File**: Your controller

```java
public void calculateRatesOfSelectedBillItem(BillItem bi) {
    // Set retail rate from DTO data
    bi.setRate(itemBatch.getRetailsaleRate());

    // Performance optimization: Skip discount calculation if no discount scheme
    boolean hasDiscountScheme = getPaymentScheme() != null ||
                               getPaymentMethod() != null ||
                               (getPaymentMethod() == PaymentMethod.Credit && toInstitution != null);

    if (hasDiscountScheme) {
        bi.setDiscountRate(calculateBillItemDiscountRate(bi));
    } else {
        bi.setDiscountRate(0.0); // No calculation needed
    }

    // Calculate net rate
    bi.setNetRate(bi.getRate() - bi.getDiscountRate());
    bi.setGrossValue(bi.getRate() * bi.getQty());
    bi.setDiscount(bi.getDiscountRate() * bi.getQty());
    bi.setNetValue(bi.getGrossValue() - bi.getDiscount());
}
```

#### 6.2 Make Expensive Operations Configurable

**File**: `ConfigOptionApplicationController.java`

```java
private void loadPharmacyConfigurationDefaults() {
    // ... existing options ...

    // Performance Configuration Options
    getBooleanValueByKey("Load Item Instructions in Pharmacy Retail Sale", false);
}
```

**File**: Your controller

```java
public void handleSelectAction() {
    // ... set stock and calculate rates ...

    // Load instructions only if configured (default: disabled for performance)
    if (configOptionApplicationController.getBooleanValueByKey(
            "Load Item Instructions in Pharmacy Retail Sale", false)) {
        pharmacyService.addBillItemInstructions(billItem);
    }
}
```

---

## Complete Example: Handle Item Selection

**File**: Your controller

```java
public void handleSelectAction() {
    if (stockDto == null) {
        return;
    }

    // Convert DTO to minimal entity (NO database query)
    this.stock = convertStockDtoToEntity(stockDto);

    if (stock == null) {
        return;
    }

    // Set stock on bill item
    getBillItem().getPharmaceuticalBillItem().setStock(stock);

    // Calculate rates (with optimized discount logic)
    calculateRatesOfSelectedBillItemBeforeAddingToTheList(billItem);

    // Load instructions only if configured (optional for performance)
    if (configOptionApplicationController.getBooleanValueByKey(
            "Load Item Instructions in Pharmacy Retail Sale", false)) {
        pharmacyService.addBillItemInstructions(billItem);
    }
}
```

---

## Update XHTML (If Needed)

**File**: Your XHTML view

Ensure the autocomplete uses the optimized method and DTO converter:

```xml
<p:autoComplete
    id="acStock"
    value="#{yourController.stockDto}"
    completeMethod="#{yourController.completeAvailableStockOptimizedDto}"
    converter="#{yourController.stockDtoConverter}"
    var="i"
    itemLabel="#{i.itemName}"
    itemValue="#{i}">

    <p:ajax event="itemSelect"
            listener="#{yourController.handleSelect}"
            update="nextField" />
</p:autoComplete>
```

---

## Checklist for Applying to Other Controllers

- [ ] **DTO Enhancement**
  - [ ] Add foreign key ID fields (e.g., itemBatchId, itemId)
  - [ ] Add fields needed for business logic (e.g., discountAllowed)
  - [ ] Add optimized constructor with all required fields
  - [ ] Add getters/setters for new fields

- [ ] **Autocomplete Query**
  - [ ] Change from `SELECT entity` to `SELECT NEW DTO(...)`
  - [ ] Include all foreign key IDs in constructor
  - [ ] Include business logic fields
  - [ ] Use `findLightsByJpql()` instead of `findByJpql()`
  - [ ] Cache results in controller field

- [ ] **Converter Fix**
  - [ ] Search cached autocomplete results first
  - [ ] Return full DTO with all fields preserved
  - [ ] Add fallback to database fetch (backward compatibility)

- [ ] **Entity Conversion**
  - [ ] Create `convertDtoToEntity()` method
  - [ ] Build lightweight entities from DTO data
  - [ ] Avoid database queries when DTO has all fields
  - [ ] Add fallback to database fetch if DTO incomplete

- [ ] **Skip Unnecessary Operations**
  - [ ] Add conditional logic to skip discount calculations
  - [ ] Make expensive operations configurable
  - [ ] Add configuration options with appropriate defaults

- [ ] **Testing**
  - [ ] Test autocomplete selection speed
  - [ ] Verify all calculations are correct
  - [ ] Test with and without discount schemes
  - [ ] Test with configuration enabled/disabled
  - [ ] Check database query count (should be 0 on selection)

---

## Performance Metrics

### Before Optimization
- Item selection time: **639-820ms**
- Database queries: **1-2 per selection**
- Breakdown:
  - Database fetch: ~10ms
  - Discount calculation: ~345ms (always)
  - Instructions loading: ~294ms (always)

### After Optimization
- Item selection time: **50-100ms**
- Database queries: **0 per selection**
- Breakdown:
  - Entity creation from DTO: ~0ms (in-memory)
  - Discount calculation: ~0ms (skipped when no scheme)
  - Instructions loading: ~0ms (skipped by default)

**Performance Improvement: 85-90% faster** 🚀

---

## Common Pitfalls to Avoid

### 1. Forgetting to Cache Autocomplete Results
**Problem**: Converter can't find full DTO, creates minimal one
**Solution**: Always cache results in controller field

### 2. Using Wrong Query Method
**Problem**: Using `findByJpql()` instead of `findLightsByJpql()`
**Solution**: DTO queries must use `findLightsByJpql()`

### 3. Missing Required Fields in DTO Constructor
**Problem**: Lightweight entity creation falls back to database
**Solution**: Include ALL foreign key IDs in DTO constructor

### 4. Not Handling Abstract Entity Classes
**Problem**: Can't instantiate abstract Item class
**Solution**: Use concrete subclass (e.g., Amp, Ampp) or entity reference

### 5. Forgetting to Update Return Type
**Problem**: Compilation error when changing from `List<Entity>` to `List<DTO>`
**Solution**: Update method signature and all callers

### 6. Always Running Expensive Operations
**Problem**: Performance not improved even with DTO optimization
**Solution**: Add conditional logic and configuration options

---

## Related Documentation

- [Configuration Options Guide](../configuration/configuration-options-guide.md)
- [DTO Implementation Guidelines](../dto/implementation-guidelines.md)
- [JSF AJAX Update Guidelines](../jsf/ajax-update-guidelines.md)

---

## Maintenance Notes

- When adding new fields to entities, update corresponding DTO constructor
- When adding new discount schemes, update skip conditions
- Keep cached results synchronized with autocomplete results
- Monitor performance metrics after deployment
- Consider adding similar patterns to other autocomplete components

---

**Last Updated**: 2025-12-02
**Applied To**: Pharmacy Retail Sale (PharmacySaleController)
**Performance Gain**: 85-90% faster item selection
