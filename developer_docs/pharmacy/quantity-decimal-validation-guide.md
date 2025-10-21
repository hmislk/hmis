# Quantity Decimal Validation Implementation Guide

## Overview

This guide provides step-by-step instructions for implementing department-specific decimal validation for quantity fields in pharmacy pages. This prevents users from entering decimal quantities when the configuration requires integer-only values.

## When to Use

Implement this validation when:
- Users can enter quantity values (e.g., medicine quantities, item counts)
- The page should support department-specific rules for allowing/disallowing decimals
- Both initial entry (Add button) and editing (table inline editing) need validation

## Configuration Key

The validation uses this configuration option:
```
"Pharmacy Direct Issue to BHT - Quantity Must Be Integer"
```

**Note**: You can use a different configuration key for other pages. Just replace this key with your page-specific key.

## Implementation Steps

### Step 1: Add ConfigOptionController Injection

In your controller class, inject the `ConfigOptionController`:

```java
@Inject
ConfigOptionController configOptionController;
```

**Example Location**: After other injections, before the field declarations
```java
@Inject
SessionController sessionController;
@Inject
ConfigOptionApplicationController configOptionApplicationController;
@Inject
ConfigOptionController configOptionController;  // Add this line
/////////////////////////
```

### Step 2: Add Validation to "Add" Method

In the method that adds new items (usually triggered by an "Add" button), add this validation **after** checking for null/zero quantities but **before** processing the item:

```java
// Validate integer-only quantity if configuration is enabled
if (configOptionController.getBooleanValueByKey("Your Config Key Here", true)) {
    if (getQty() % 1 != 0) {
        errorMessage = "Please enter only whole numbers (integers). Decimal values are not allowed.";
        JsfUtil.addErrorMessage("Please enter only whole numbers (integers). Decimal values are not allowed.");
        return;
    }
}
```

**Complete Example**:
```java
public void addBillItem() {
    if (getStock() == null) {
        JsfUtil.addErrorMessage("No Stock");
        return;
    }
    if (getQty() == null) {
        JsfUtil.addErrorMessage("Please enter a Quantity?");
        return;
    }
    if (getQty() <= 0.0) {
        JsfUtil.addErrorMessage("Please enter a Quantity?");
        return;
    }

    // ADD VALIDATION HERE
    if (configOptionController.getBooleanValueByKey("Pharmacy Direct Issue to BHT - Quantity Must Be Integer", true)) {
        if (getQty() % 1 != 0) {
            errorMessage = "Please enter only whole numbers (integers). Decimal values are not allowed.";
            JsfUtil.addErrorMessage("Please enter only whole numbers (integers). Decimal values are not allowed.");
            return;
        }
    }

    // Continue with normal processing...
}
```

### Step 3: Add Validation to "Edit" Method

In the method that handles inline editing (usually `onEdit(BillItem tmp)`), add this validation **after** null/zero checks but **before** stock availability checks:

```java
// Validate integer-only quantity if configuration is enabled
if (configOptionController.getBooleanValueByKey("Your Config Key Here", true)) {
    if (tmp.getQty() % 1 != 0) {
        setZeroToQty(tmp);  // Reset to valid state
        onEditCalculation(tmp);  // Recalculate totals
        JsfUtil.addErrorMessage("Please enter only whole numbers (integers). Decimal values are not allowed.");
        return true;
    }
}
```

**Complete Example**:
```java
public boolean onEdit(BillItem tmp) {
    // Checking Minus Value && Null
    if (tmp.getQty() <= 0 || tmp.getQty() == null) {
        setZeroToQty(tmp);
        onEditCalculation(tmp);
        JsfUtil.addErrorMessage("Can not enter a minus value");
        return true;
    }

    // ADD VALIDATION HERE
    if (configOptionController.getBooleanValueByKey("Pharmacy Direct Issue to BHT - Quantity Must Be Integer", true)) {
        if (tmp.getQty() % 1 != 0) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);
            JsfUtil.addErrorMessage("Please enter only whole numbers (integers). Decimal values are not allowed.");
            return true;
        }
    }

    // Continue with stock checks...
    Stock fetchedStock = getStockFacade().find(tmp.getPharmaceuticalBillItem().getStock().getId());
    // ... rest of validation
}
```

## Configuration Setup

### Application-Level (All Departments)

To set for all departments, add this configuration:
```
Key: "Pharmacy Direct Issue to BHT - Quantity Must Be Integer"
Value: true/false
Scope: APPLICATION
```

### Department-Level (Specific Department)

To override for a specific department, add this configuration:
```
Key: "Department Name - Pharmacy Direct Issue to BHT - Quantity Must Be Integer"
Example: "Pharmacy - Pharmacy Direct Issue to BHT - Quantity Must Be Integer"
Value: true/false
Scope: APPLICATION
```

The `ConfigOptionController` automatically handles the department lookup using `sessionController.getDepartment()`.

## How It Works

1. **Department Resolution**:
   - `ConfigOptionController.getBooleanValueByKey()` checks if user has a department in session
   - If department exists, it looks for "Department Name - Your Key"
   - If not found, falls back to application-level "Your Key"

2. **Validation Logic**:
   - Uses modulo operator (`% 1`) to check if number has decimal places
   - Example: `5.5 % 1 = 0.5` (has decimals) ❌
   - Example: `5.0 % 1 = 0` (no decimals) ✅

3. **Error Handling**:
   - For "Add" operations: Shows error and prevents adding
   - For "Edit" operations: Resets quantity to zero, recalculates, shows error

## Testing Checklist

- [ ] Configuration is created at application level
- [ ] Configuration can be overridden at department level
- [ ] Adding items with decimals shows error message
- [ ] Adding items with integers works correctly
- [ ] Editing items with decimals shows error and resets quantity
- [ ] Editing items with integers works correctly
- [ ] Error messages are clear and user-friendly
- [ ] When validation is disabled (false), decimals are allowed

## Example Implementation Reference

See: `PharmacySaleBhtController.java`
- File: `src/main/java/com/divudi/bean/pharmacy/PharmacySaleBhtController.java`
- Add Method: Line 1258 (in `addBillItem()`)
- Edit Method: Line 304 (in `onEdit(BillItem tmp)`)
- Page: `src/main/webapp/inward/pharmacy_bill_issue_bht.xhtml`

## Common Patterns

### Pattern 1: Using with Different Quantity Variable Names

If your quantity is stored in a different variable:
```java
// For direct qty field
if (qty % 1 != 0) { ... }

// For BillItem qty
if (tmp.getQty() % 1 != 0) { ... }

// For custom object
if (myObject.getQuantity() % 1 != 0) { ... }
```

### Pattern 2: Custom Error Messages

Customize the error message for your context:
```java
JsfUtil.addErrorMessage("Stock transfer quantities must be whole numbers.");
JsfUtil.addErrorMessage("Please enter integer values only for item count.");
```

### Pattern 3: Using Different Configuration Keys

For different pages/modules:
```java
// For purchase orders
configOptionController.getBooleanValueByKey("Purchase Order - Quantity Must Be Integer", true)

// For stock transfers
configOptionController.getBooleanValueByKey("Stock Transfer - Quantity Must Be Integer", true)

// For general issue
configOptionController.getBooleanValueByKey("Pharmacy Issue - Quantity Must Be Integer", true)
```

## Troubleshooting

### Issue: Validation Not Working

**Check**:
1. Is `ConfigOptionController` properly injected?
2. Is the configuration key spelled exactly right?
3. Is the configuration created in the database?
4. Is the department name in the key matching exactly?

### Issue: Validation Always Returns Same Value

**Check**:
1. Verify `sessionController.getDepartment()` returns correct department
2. Check if department-specific config exists and is not retired
3. Verify fallback to application-level config works

### Issue: Decimals Still Allowed

**Check**:
1. Ensure validation is placed **before** processing logic
2. Verify configuration value is `true` not `false`
3. Check if there are multiple code paths that bypass validation

## Best Practices

1. **Consistent Placement**: Always add validation after null/zero checks but before business logic
2. **Clear Messages**: Use user-friendly error messages that explain what's wrong
3. **Reset State**: In edit operations, always reset to valid state (zero or previous value)
4. **Recalculate**: After resetting, always recalculate totals/values
5. **Configuration Naming**: Use descriptive configuration keys that clearly indicate the feature
6. **Default Value**: Use sensible defaults (usually `true` for strict validation)

## Related Documentation

- Configuration Management: `developer_docs/configuration/`
- JSF Validation: `developer_docs/jsf/`
- Pharmacy Module: `developer_docs/pharmacy/`

---

**Last Updated**: 2025-01-20
**Author**: HMIS Development Team
