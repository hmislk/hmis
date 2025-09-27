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

## Required Configuration Options

Add these configuration options to `ConfigOptionApplicationController.java`:

```java
// Bill Number Generation Strategy Configuration
public static final String BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_DEPT_INS_YEAR_COUNT = "Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count";
public static final String BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_INS_YEAR_COUNT = "Bill Number Generation Strategy for Department Id is Prefix Ins Year Count";
public static final String BILL_NUMBER_GENERATION_STRATEGY_FOR_INSTITUTION_ID_IS_PREFIX_INS_YEAR_COUNT = "Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count";

// Bill Number Suffix Configuration (per bill type)
// Format: "Bill Number Suffix for " + BillTypeAtomic.BILL_TYPE_NAME
// Examples:
// "Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_PRE -> "POR"
// "Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_APPROVAL -> "POA"
// "Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_CANCELLED -> "C-POR"
// "Bill Number Suffix for " + BillTypeAtomic.PHARMACY_GRN -> "GRN"
```

## Step-by-Step Implementation

### Step 1: Add Configuration Methods to Controller

Ensure your controller has access to `ConfigOptionApplicationController`:

```java
@Inject
ConfigOptionApplicationController configOptionApplicationController;

// Get configuration values
private boolean billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount() {
    return configOptionApplicationController.getBooleanValueByKey(
        "Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count", false);
}

private boolean billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount() {
    return configOptionApplicationController.getBooleanValueByKey(
        "Bill Number Generation Strategy for Department Id is Prefix Ins Year Count", false);
}

private boolean billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount() {
    return configOptionApplicationController.getBooleanValueByKey(
        "Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count", false);
}
```

### Step 2: Implement Independent Department and Institution ID Generation

**CRITICAL**: Department and Institution ID generation must be completely independent:

```java
// Handle Department ID generation (independent)
String deptId;
if (billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount()) {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
            sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
} else if (billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount()) {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
} else {
    // Use existing method for backward compatibility
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(
            sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
}

// Handle Institution ID generation (completely separate)
String insId;
if (billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount()) {
    insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
} else {
    // Smart fallback logic
    if (billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount() ||
        billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount()) {
        insId = deptId; // Use same number as department
    } else {
        // Use existing institution method for backward compatibility
        insId = getBillNumberBean().institutionBillNumberGenerator(
                getSessionController().getInstitution(),
                BillType.YourBillType,
                BillClassType.BilledBill,
                BillNumberSuffix.YOUR_SUFFIX);
    }
}
```

### Step 3: Implement Default Suffix Logic

Handle default suffix in the controller (NOT in the service):

```java
// Check if bill number suffix is configured, if not set default
String billSuffix = configOptionApplicationController.getLongTextValueByKey(
    "Bill Number Suffix for " + BillTypeAtomic.YOUR_BILL_TYPE, "");
if (billSuffix == null || billSuffix.trim().isEmpty()) {
    configOptionApplicationController.setLongTextValueByKey(
        "Bill Number Suffix for " + BillTypeAtomic.YOUR_BILL_TYPE, "YOUR_DEFAULT_SUFFIX");
}
```

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

### Example 1: Purchase Order Request (PurchaseOrderRequestController.java)

```java
// In saveBill() method
String billSuffix = configOptionApplicationController.getLongTextValueByKey(
    "Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_PRE, "");
if (billSuffix == null || billSuffix.trim().isEmpty()) {
    configOptionApplicationController.setLongTextValueByKey(
        "Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_PRE, "POR");
}

// Department ID generation
String deptId;
if (billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount()) {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
            sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
} else if (billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount()) {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
} else {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(
            sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
}

// Institution ID generation (independent)
String insId;
if (billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount()) {
    insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
} else {
    if (billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount() ||
        billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount()) {
        insId = deptId;
    } else {
        insId = getBillNumberBean().institutionBillNumberGenerator(
                getSessionController().getInstitution(),
                BillType.PharmacyOrderRequest,
                BillClassType.PreBill,
                BillNumberSuffix.PHAORD);
    }
}

getCurrentBill().setDeptId(deptId);
getCurrentBill().setInsId(insId);
```

### Example 2: GRN Costing (GrnCostingController.java)

```java
// In requestFinalizeWithSaveApprove() method
String billSuffix = configOptionApplicationController.getLongTextValueByKey(
    "Bill Number Suffix for " + BillTypeAtomic.PHARMACY_GRN, "");
if (billSuffix == null || billSuffix.trim().isEmpty()) {
    configOptionApplicationController.setLongTextValueByKey(
        "Bill Number Suffix for " + BillTypeAtomic.PHARMACY_GRN, "GRN");
}

// Similar independent department and institution ID generation...
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