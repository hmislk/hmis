# Bill Number Generation Strategy Implementation Guide

## Overview

This guide explains how to implement improved bill number generation strategies for any bill type in the HMIS system without affecting existing flows. The new strategy provides configurable bill number formats with proper separators and institution-wide counting capabilities.

## Core Concepts

### Bill Number Formats

The new system supports three main formats:

1. **Department + Institution Format**: `PREFIX/DEPT_CODE/INS_CODE/YEAR/NUMBER`
   - Example: `POR/ICU/MPH/25/000001`

2. **Institution-wide Department Format**: `PREFIX/INS_CODE/YEAR/NUMBER` (counting department bills institution-wide)
   - Example: `POR/MPH/25/000001`

3. **Institution-only Format**: `PREFIX/INS_CODE/YEAR/NUMBER` (counting all bills institution-wide)
   - Example: `POR/MPH/25/000001`

### Key Principles

1. **Backward Compatibility**: Never modify existing methods - create new ones and deprecate old ones
2. **Independent Generation**: Department ID and Institution ID generation must be completely separate
3. **Configuration-Driven**: All strategies controlled through ConfigOptionApplicationController
4. **Thread-Safe**: All generation methods use ReentrantLock for concurrency safety

## Required Configuration Setup

### Step 1: Add Configuration Defaults to ConfigOptionApplicationController

The configuration defaults are centrally managed in `ConfigOptionApplicationController.java` within the `loadPharmacyConfigurationDefaults()` method. This ensures all configuration options have proper defaults and are loaded at application startup.

**CRITICAL**: All bill number generation configuration must be added to `loadPharmacyConfigurationDefaults()` method, NOT in individual controllers.

#### Bill Number Generation Strategy Defaults

These are already present in `loadPharmacyConfigurationDefaults()`:

```java
private void loadPharmacyConfigurationDefaults() {
    // ... other pharmacy configurations ...

    // Bill Numbering Configuration Options - Added for improved bill numbering functionality
    // Generic bill numbering strategies (for backward compatibility)
    getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false);
    getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false);
    getBooleanValueByKey("Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count", false);

    // ... other bill-type-specific strategies ...
}
```

#### Adding Bill Number Suffix for New Bill Types

When adding support for a new bill type, add the suffix configuration to the same method:

```java
private void loadPharmacyConfigurationDefaults() {
    // ... existing configurations ...

    // Bill Number Suffix Configuration Options - Default suffixes for different bill types
    getShortTextValueByKey("Bill Number Suffix for Purchase Order Request", "POR");
    getShortTextValueByKey("Bill Number Suffix for Purchase Order Approval", "POA");
    getShortTextValueByKey("Bill Number Suffix for PHARMACY_DIRECT_PURCHASE", "DP");
    // Add your new bill type suffix here:
    getShortTextValueByKey("Bill Number Suffix for YOUR_BILL_TYPE_ATOMIC", "YOUR_SUFFIX");
}
```

**Example for Direct Purchase**:
```java
getShortTextValueByKey("Bill Number Suffix for PHARMACY_DIRECT_PURCHASE", "DP");
```

## Step-by-Step Implementation

### Step 1: Ensure Controller Access to ConfigOptionApplicationController

Ensure your controller has access to `ConfigOptionApplicationController` (most controllers already have this):

```java
@Inject
ConfigOptionApplicationController configOptionApplicationController;
```

**IMPORTANT**: Do NOT create helper methods in controllers. Use the configuration service directly.

### Step 2: Implement Independent Department and Institution ID Generation

**CRITICAL**: Department and Institution ID generation must be completely independent. Use direct calls to `configOptionApplicationController` without helper methods:

```java
// Handle Department ID generation (independent)
String deptId;
if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false)) {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
            sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
} else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false)) {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
} else {
    // Use existing method for backward compatibility
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(
            sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
}

// Handle Institution ID generation (completely separate)
String insId;
if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count", false)) {
    insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
} else {
    // Smart fallback logic
    if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false) ||
        configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false)) {
        insId = deptId; // Use same number as department
    } else {
        // Use existing institution method for backward compatibility
        insId = getBillNumberBean().departmentBillNumberGeneratorYearly(
                sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
    }
}
```

### Step 3: Configuration Defaults Handle Suffix Logic

**IMPORTANT**: Do NOT implement suffix logic in controllers. The configuration defaults in `ConfigOptionApplicationController.loadPharmacyConfigurationDefaults()` handle this automatically.

The suffix configuration is already handled by:
```java
getShortTextValueByKey("Bill Number Suffix for YOUR_BILL_TYPE_ATOMIC", "DEFAULT_SUFFIX");
```

This ensures the suffix is available when needed without any controller logic.

### Step 4: Apply Bill Numbers to Entity

```java
// Set the generated bill numbers
bill.setDeptId(deptId);
bill.setInsId(insId);
```

## Available BillNumberGenerator Methods

### New Methods (Use These)

```java
// Format: PREFIX/DEPT_CODE/INS_CODE/YEAR/NUMBER
public String departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
    Department department, BillTypeAtomic billTypeAtomic)

// Format: PREFIX/INS_CODE/YEAR/NUMBER (department bills counted institution-wide)
public String departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
    Department department, BillTypeAtomic billTypeAtomic)

// Format: PREFIX/INS_CODE/YEAR/NUMBER (all bills counted institution-wide)
public String institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
    Department department, BillTypeAtomic billTypeAtomic)
```

### Deprecated Methods (Don't Modify)

```java
// These are deprecated but still used by existing institutions
public String departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCountDeprecated(...)
public String departmentBillNumberGeneratorYearlyWithPrefixInsYearCountDeprecated(...)
public String institutionBillNumberGeneratorYearlyWithPrefixInsYearCountDeprecated(...)
```

## Common Bill Types and Default Suffixes

| Bill Type | BillTypeAtomic | Default Suffix | Description |
|-----------|----------------|----------------|-------------|
| Purchase Order Request | `PHARMACY_ORDER_PRE` | `POR` | Purchase Order Request |
| Purchase Order Approval | `PHARMACY_ORDER_APPROVAL` | `POA` | Purchase Order Approval |
| Purchase Order Cancellation | `PHARMACY_ORDER_CANCELLED` | `C-POR` | Cancelled Purchase Order Request |
| Pharmacy GRN | `PHARMACY_GRN` | `GRN` | Goods Received Note |
| Pharmacy Purchase | `PHARMACY_PURCHASE` | `PP` | Pharmacy Purchase |
| Pharmacy Issue | `PHARMACY_ISSUE` | `PI` | Pharmacy Issue |

## Implementation Examples

### Example 1: Direct Purchase (PharmacyDirectPurchaseController.java)

```java
// In saveBill() method
// NOTE: No suffix configuration logic needed - handled by ConfigOptionApplicationController

// Handle Department ID generation (independent)
String deptId;
if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false)) {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
            sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
} else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false)) {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
} else {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
}

// Handle Institution ID generation (completely separate)
String insId;
if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count", false)) {
    insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
} else {
    // Smart fallback logic
    if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false) ||
        configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false)) {
        insId = deptId; // Use same number as department
    } else {
        // Use existing institution method for backward compatibility
        insId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
    }
}

getBill().setDeptId(deptId);
getBill().setInsId(insId);
```

### Example 2: Configuration Setup in ConfigOptionApplicationController

```java
private void loadPharmacyConfigurationDefaults() {
    // ... other pharmacy configurations ...

    // Bill Number Generation Strategy Defaults
    getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false);
    getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false);
    getBooleanValueByKey("Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count", false);

    // Bill Number Suffix Defaults
    getShortTextValueByKey("Bill Number Suffix for Purchase Order Request", "POR");
    getShortTextValueByKey("Bill Number Suffix for PHARMACY_DIRECT_PURCHASE", "DP");
    getShortTextValueByKey("Bill Number Suffix for PHARMACY_GRN", "GRN");
    // Add new bill types here
}
```

## Testing Strategy

### Configuration Testing

1. **Test Default Behavior**: Ensure existing institutions work unchanged
2. **Test New Formats**: Enable new strategies and verify format output
3. **Test Independence**: Verify department and institution strategies work independently
4. **Test Counting Logic**: Verify institution-wide vs department-specific counting

### Sample Test Configurations

```java
// Test Case 1: Department + Institution format
"Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count" = true
"Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count" = false
Expected: POR/ICU/MPH/25/000001 (dept), POR/ICU/MPH/25/000001 (ins)

// Test Case 2: Institution-wide department format
"Bill Number Generation Strategy for Department Id is Prefix Ins Year Count" = true
"Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count" = false
Expected: POR/MPH/25/000001 (dept), POR/MPH/25/000001 (ins)

// Test Case 3: Independent institution format
"Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count" = false
"Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count" = true
Expected: [legacy format] (dept), POR/MPH/25/000001 (ins)
```

## Migration Guidelines

### For Existing Institutions

1. **No Action Required**: Existing behavior is preserved by default
2. **Optional Upgrade**: Can enable new strategies through configuration
3. **Gradual Migration**: Can enable strategies independently

### For New Institutions

1. **Configure Strategies**: Set desired bill number generation strategies
2. **Set Suffixes**: Configure appropriate bill number suffixes
3. **Test Thoroughly**: Verify bill number formats meet requirements

## Troubleshooting

### Common Issues

1. **Numbers Not Sequential**: Check if institution-wide counting is needed
2. **Wrong Format**: Verify configuration options are set correctly
3. **Compilation Errors**: Ensure all new methods are used correctly
4. **Legacy Behavior Changed**: Check if deprecated methods were modified

### Debug Steps

1. **Check Configuration**: Verify boolean configuration values
2. **Check Suffix Configuration**: Verify suffix is set properly
3. **Check Method Calls**: Ensure correct new methods are called
4. **Check Independence**: Verify department and institution logic is separate

## Future Enhancements

1. **Additional Formats**: New bill number formats can be added by creating new methods
2. **Custom Separators**: Currently uses "/" but can be made configurable
3. **Multi-Year Support**: Can extend to support financial year vs calendar year
4. **Audit Trail**: Can add logging for bill number generation events

## Notes

- **NEVER** modify deprecated methods - they are used by existing institutions
- **ALWAYS** maintain backward compatibility
- **ALWAYS** keep department and institution ID generation independent
- **ALWAYS** handle default suffixes in controller, not service
- **ALWAYS** use appropriate BillTypeAtomic for each bill type
- **ALWAYS** test with different configuration combinations

This pattern can be applied to any bill type in the HMIS system while maintaining complete backward compatibility.

## Configuration Change Workflow

### Adding Support for New Bill Types

Follow this systematic approach when adding bill number generation support for new bill types:

#### 1. Add Configuration Defaults First

**File**: `ConfigOptionApplicationController.java`
**Method**: `loadPharmacyConfigurationDefaults()`

```java
// Add the suffix configuration for your new bill type
getShortTextValueByKey("Bill Number Suffix for YOUR_BILL_TYPE_ATOMIC", "YOUR_DEFAULT_SUFFIX");
```

#### 2. Implement Controller Logic

**File**: Your controller's save/finalize method

```java
public void yourSaveMethod() {
    // Department ID generation (independent)
    String deptId;
    if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false)) {
        deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
    } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false)) {
        deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
    } else {
        deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(
                sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
    }

    // Institution ID generation (completely separate)
    String insId;
    if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count", false)) {
        insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
    } else {
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false) ||
            configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false)) {
            insId = deptId;
        } else {
            insId = getBillNumberBean().departmentBillNumberGeneratorYearly(
                    sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
        }
    }

    yourBill.setDeptId(deptId);
    yourBill.setInsId(insId);

    // ... rest of your save logic
}
```

#### 3. Testing Checklist

- [ ] Configuration defaults are loaded at application startup
- [ ] Default suffix appears in admin configuration UI
- [ ] Bill numbers generate correctly with default suffix
- [ ] All three generation strategies work independently
- [ ] Backward compatibility preserved for existing institutions
- [ ] No compilation errors
- [ ] No null pointer exceptions

### Configuration Management Best Practices

#### Do's ✅

- **Add all configuration defaults to `loadPharmacyConfigurationDefaults()`**
- **Use direct calls to `configOptionApplicationController.getBooleanValueByKey()`**
- **Keep department and institution ID generation completely independent**
- **Test all configuration scenarios**
- **Follow the exact pattern shown in examples**

#### Don'ts ❌

- **❌ Do NOT create helper methods in controllers**
- **❌ Do NOT add suffix configuration logic in controllers**
- **❌ Do NOT modify existing deprecated methods**
- **❌ Do NOT combine department and institution strategies into single if/else**
- **❌ Do NOT skip adding configuration defaults**

### Troubleshooting Configuration Issues

#### Issue: Configuration option not appearing in admin UI
**Solution**: Ensure `loadPharmacyConfigurationDefaults()` contains the configuration and restart application

#### Issue: Default suffix not working
**Solution**: Check that `getShortTextValueByKey("Bill Number Suffix for BILL_TYPE", "DEFAULT")` is in `loadPharmacyConfigurationDefaults()`

#### Issue: Bill numbers not following new format
**Solution**: Verify configuration strategies are enabled in admin UI and check BillTypeAtomic parameter is correct

#### Issue: Different numbers for department and institution when they should be same
**Solution**: Check that institution ID generation logic has proper fallback to use `deptId` when appropriate