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
5. **Uniformity Requirement**: Use bill-type-specific configuration keys for consistency and better maintainability

## Configuration Key Patterns for Uniformity

### Bill-Type-Specific Pattern (Recommended)

For consistency and better maintainability, always use bill-type-specific configuration keys:

**✅ Correct Pattern:**
```
Bill Number Generation Strategy for Pharmacy [BILL_TYPE_NAME] - Prefix + Department Code + Institution Code + Year + Yearly Number
Bill Number Generation Strategy for Pharmacy [BILL_TYPE_NAME] - Prefix + Institution Code + Year + Yearly Number
```

**Examples:**
- `"Bill Number Generation Strategy for Pharmacy Direct Purchase Refund - Prefix + Department Code + Institution Code + Year + Yearly Number"`
- `"Bill Number Generation Strategy for Pharmacy GRN Return - Prefix + Institution Code + Year + Yearly Number"`

### Generic Pattern (Legacy - Use Only for Backward Compatibility)

**❌ Avoid for New Implementations:**
```
Bill Number Generation Strategy for Department ID is Prefix Dept Ins Year Count
Bill Number Generation Strategy for Institution ID is Prefix Ins Year Count
```

These generic keys are maintained only for backward compatibility with existing implementations.

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
if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
            sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
} else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Institution Code + Year + Yearly Number", false)) {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
} else {
    // Use existing method for backward compatibility
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(
            sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
}

// Handle Institution ID generation (completely separate)
String insId;
if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Institution Code + Year + Yearly Number", false)) {
    insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
} else {
    // Smart fallback logic
    if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Department Code + Institution Code + Year + Yearly Number", false) ||
        configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Institution Code + Year + Yearly Number", false)) {
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
| Direct Purchase | `PHARMACY_DIRECT_PURCHASE` | `DP` | Direct Purchase |
| Direct Purchase Refund | `PHARMACY_DIRECT_PURCHASE_REFUND` | `DPR` | Direct Purchase Refund |

## Implementation Examples

### Example 1: Direct Purchase (PharmacyDirectPurchaseController.java)

```java
// In saveBill() method
// NOTE: No suffix configuration logic needed - handled by ConfigOptionApplicationController

// Handle Department ID generation (independent)
String deptId;
if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Direct Purchase - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
            sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
} else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Direct Purchase - Prefix + Institution Code + Year + Yearly Number", false)) {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
} else {
    deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
}

// Handle Institution ID generation (completely separate)
String insId;
if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Direct Purchase - Prefix + Institution Code + Year + Yearly Number", false)) {
    insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
} else {
    // Smart fallback logic
    if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Direct Purchase - Prefix + Department Code + Institution Code + Year + Yearly Number", false) ||
        configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Direct Purchase - Prefix + Institution Code + Year + Yearly Number", false)) {
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
    // Generic strategies (for backward compatibility - prefer bill-type-specific configurations)
    getBooleanValueByKey("Bill Number Generation Strategy for Department ID is Prefix Dept Ins Year Count", false);
    getBooleanValueByKey("Bill Number Generation Strategy for Department ID is Prefix Ins Year Count", false);
    getBooleanValueByKey("Bill Number Generation Strategy for Institution ID is Prefix Ins Year Count", false);

    // Bill-type-specific strategies (recommended approach for uniformity)
    getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Direct Purchase Refund - Prefix + Department Code + Institution Code + Year + Yearly Number", false);
    getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Direct Purchase Refund - Prefix + Institution Code + Year + Yearly Number", false);

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
// Test Case 1: Department + Institution format (Bill-Type-Specific)
"Bill Number Generation Strategy for Pharmacy Purchase Order Request - Prefix + Department Code + Institution Code + Year + Yearly Number" = true
Expected: POR/ICU/MPH/25/000001 (dept), POR/ICU/MPH/25/000001 (ins)

// Test Case 2: Institution-wide department format (Bill-Type-Specific)
"Bill Number Generation Strategy for Pharmacy Purchase Order Request - Prefix + Institution Code + Year + Yearly Number" = true
Expected: POR/MPH/25/000001 (dept), POR/MPH/25/000001 (ins)

// Test Case 3: Legacy format (No new strategies enabled)
"Bill Number Generation Strategy for Pharmacy Purchase Order Request - Prefix + Department Code + Institution Code + Year + Yearly Number" = false
"Bill Number Generation Strategy for Pharmacy Purchase Order Request - Prefix + Institution Code + Year + Yearly Number" = false
Expected: [legacy format] (dept), [legacy format] (ins)
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

## Complete Implementation Workflow

### When Given a Page Name: Finding the Implementation Points

When you're asked to add bill number generation to a specific page (e.g., `/pharmacy/pharmacy_cancel_purchase.xhtml`), follow this systematic discovery process:

#### Step 1: Identify the Button and Action Method

1. **Find the XHTML page**: Look for the page file in `src/main/webapp/`
2. **Locate the relevant button**: Look for buttons with actions like "Cancel", "Save", "Submit", etc.
3. **Extract the backend method**: From the button's `action` attribute, identify the controller and method

**Example from `/pharmacy/pharmacy_cancel_purchase.xhtml`:**
```xml
<h:commandButton value="Cancel" action="#{pharmacyBillSearch.pharmacyPurchaseCancel()}" >
</h:commandButton>
```

**Key Information Extracted:**
- **Controller**: `pharmacyBillSearch` → `PharmacyBillSearch.java`
- **Method**: `pharmacyPurchaseCancel()`
- **Button**: "Cancel" button

#### Step 2: Locate the Backend Method

1. **Find the controller class**: Search for the controller class (e.g., `PharmacyBillSearch.java`)
2. **Find the specific method**: Look for the method identified in Step 1
3. **Analyze the method flow**: Look for bill creation, number generation, or calls to other methods

**Example Method Discovery:**
```java
public void pharmacyPurchaseCancel() {
    // ... validation logic ...
    CancelledBill cb = pharmacyCreateCancelBill();  // <-- Bill creation method
    // ... existing bill number generation logic ...
}
```

#### Step 3: Find the Department ID Generation Logic

Look for one of these patterns in the method or sub-methods it calls:

**Pattern 1: Direct bill number generation in the main method**
```java
cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(...));
cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(...));
```

**Pattern 2: Bill number generation in a helper method**
```java
CancelledBill cb = createSomeCancelBill(); // <-- Check this method for bill number generation
```

**Pattern 3: Bill number generation in inherited/parent method**
```java
// Sometimes the generation is in parent class methods or shared utility methods
```

#### Step 4: Identify the Bill Type Atomic

1. **Find the BillTypeAtomic**: Look for lines like `cb.setBillTypeAtomic(BillTypeAtomic.SOMETHING)`
2. **Note the bill type**: This determines what suffix configuration key to use

**Example:**
```java
cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED);
```

### Complete Implementation Checklist

#### Phase 1: Discovery and Analysis
- [ ] **Page Analysis**: Found the XHTML page and identified the button
- [ ] **Controller Discovery**: Located the controller class and method
- [ ] **Method Analysis**: Found where bill creation and number generation occurs
- [ ] **Bill Type Identification**: Identified the BillTypeAtomic being used

#### Phase 2: Configuration Setup
- [ ] **Add Suffix Configuration**: Add suffix config to `ConfigOptionApplicationController.loadPharmacyConfigurationDefaults()`
```java
getShortTextValueByKey("Bill Number Suffix for YOUR_BILL_TYPE_ATOMIC", "YOUR_SUFFIX");
```

#### Phase 3: Implementation
- [ ] **Replace Bill Number Logic**: Implement the new configurable generation pattern
- [ ] **Preserve P1 Behavior**: Ensure fallback logic reuses deptId for insId when all strategies are disabled
- [ ] **Test Independence**: Verify department and institution strategies work independently

#### Phase 4: Validation
- [ ] **Compilation**: Code compiles without errors
- [ ] **Configuration Loading**: New suffix appears in admin configuration
- [ ] **Strategy Testing**: All three strategies work independently
- [ ] **Backward Compatibility**: Existing behavior preserved when strategies disabled

### Detailed Implementation Template

```java
public void yourMethodName() {
    // ... existing validation logic ...

    CancelledBill cb = createYourCancelBill();
    cb.setBillTypeAtomic(BillTypeAtomic.YOUR_BILL_TYPE_ATOMIC);

    // Handle Department ID generation (independent)
    String deptId;
    if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
        deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                getSessionController().getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE_ATOMIC);
    } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Institution Code + Year + Yearly Number", false)) {
        deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                getSessionController().getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE_ATOMIC);
    } else {
        // Use existing method for backward compatibility
        deptId = getBillNumberBean().institutionBillNumberGenerator(
                getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.YOUR_SUFFIX);
    }

    // Handle Institution ID generation (completely separate)
    String insId;
    if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Institution Code + Year + Yearly Number", false)) {
        insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                getSessionController().getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE_ATOMIC);
    } else {
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Department Code + Institution Code + Year + Yearly Number", false) ||
            configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = deptId; // Use same number as department
        } else {
            // Preserve old behavior: reuse deptId for insId to avoid consuming counter twice
            insId = deptId;
        }
    }

    cb.setDeptId(deptId);
    cb.setInsId(insId);

    // ... rest of your method logic ...
}
```

### Real-World Example: Direct Purchase Cancellation

**Given**: Page `/pharmacy/pharmacy_cancel_purchase.xhtml`

**Step 1: Button Discovery**
```xml
<h:commandButton value="Cancel" action="#{pharmacyBillSearch.pharmacyPurchaseCancel()}" >
```

**Step 2: Method Location**
- **File**: `PharmacyBillSearch.java`
- **Method**: `pharmacyPurchaseCancel()`

**Step 3: Department ID Generation Found**
```java
public void pharmacyPurchaseCancel() {
    // ... validation ...
    CancelledBill cb = pharmacyCreateCancelBill();
    // OLD CODE (replaced):
    // cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(...));
    // cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(...));

    // NEW CODE (implemented):
    // [The improved bill number generation logic from template above]
}
```

**Step 4: Bill Type Identified**
```java
cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED);
```

**Step 5: Configuration Added**
```java
// In ConfigOptionApplicationController.loadPharmacyConfigurationDefaults():
getShortTextValueByKey("Bill Number Suffix for PHARMACY_DIRECT_PURCHASE_CANCELLED", "C-DP");
```

### Common Scenarios and Variations

#### Scenario 1: Bill Number Generation in Main Method
- **Pattern**: Bill numbers are set directly in the action method
- **Implementation**: Replace the `setDeptId()` and `setInsId()` calls with new logic

#### Scenario 2: Bill Number Generation in Helper Method
- **Pattern**: Action method calls a helper like `createCancelBill()` that handles numbering
- **Implementation**: Modify the helper method to use new generation logic

#### Scenario 3: Multiple Bill Types in One Method
- **Pattern**: Method handles different bill types based on conditions
- **Implementation**: Use appropriate BillTypeAtomic for each branch

#### Scenario 4: Inherited Bill Number Logic
- **Pattern**: Bill numbering is handled in parent class or shared utility
- **Implementation**: May need to pass additional parameters or override behavior

### Troubleshooting Discovery Process

#### Issue: Can't find the backend method
**Solution**:
1. Check for typos in method name
2. Look in parent classes or interfaces
3. Search across entire codebase for the method name

#### Issue: Button action uses different controller
**Solution**:
1. Check if `#{controllerName}` matches expected controller
2. Look for `@Named` annotation on controller class
3. Check if action is in a different bean

#### Issue: Bill number generation not in main method
**Solution**:
1. Look for method calls that create bills
2. Check helper methods like `createCancelBill()`, `saveBill()`, etc.
3. Search for `setDeptId` calls in the controller

#### Issue: Multiple bill types handled
**Solution**:
1. Identify all BillTypeAtomic values used
2. Add suffix configurations for each bill type
3. Implement appropriate logic for each type

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
    if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
        deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
    } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Institution Code + Year + Yearly Number", false)) {
        deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
    } else {
        deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(
                sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
    }

    // Institution ID generation (completely separate)
    String insId;
    if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Institution Code + Year + Yearly Number", false)) {
        insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                sessionController.getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
    } else {
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Department Code + Institution Code + Year + Yearly Number", false) ||
            configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Institution Code + Year + Yearly Number", false)) {
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
- **Preserve old behavior when all strategies are disabled (reuse deptId for insId)**

#### Don'ts ❌

- **❌ Do NOT create helper methods in controllers**
- **❌ Do NOT add suffix configuration logic in controllers**
- **❌ Do NOT modify existing deprecated methods**
- **❌ Do NOT combine department and institution strategies into single if/else**
- **❌ Do NOT skip adding configuration defaults**
- **❌ Do NOT call bill number generator twice when all strategies are disabled (causes sequence gaps)**

### Troubleshooting Configuration Issues

#### Issue: Configuration option not appearing in admin UI
**Solution**: Ensure `loadPharmacyConfigurationDefaults()` contains the configuration and restart application

#### Issue: Default suffix not working
**Solution**: Check that `getShortTextValueByKey("Bill Number Suffix for BILL_TYPE", "DEFAULT")` is in `loadPharmacyConfigurationDefaults()`

#### Issue: Bill numbers not following new format
**Solution**: Verify configuration strategies are enabled in admin UI and check BillTypeAtomic parameter is correct

#### Issue: Different numbers for department and institution when they should be same
**Solution**: Check that institution ID generation logic has proper fallback to use `deptId` when appropriate

### Critical P1 Issue: Preserving Old Behavior When No Strategies Are Enabled

#### **Issue Description**
When all three configuration flags are left at their defaults (false), the fallback logic must preserve the original behavior exactly. For some bill types (like Direct Purchase bills), the original behavior was `deptId == insId`, meaning both identifiers had the same value and only one sequence number was consumed.

#### **Problem Pattern**
**❌ INCORRECT (consumes counter twice):**
```java
// When all strategies are false - this is WRONG
String deptId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), ...);
String insId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), ...);
```

**✅ CORRECT (preserves old behavior):**
```java
// When all strategies are false - this preserves original behavior
String deptId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), ...);
String insId = deptId; // Reuse same number
```

#### **Correct Implementation Pattern**
```java
// Handle Institution ID generation (completely separate)
String insId;
if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Institution Code + Year + Yearly Number", false)) {
    insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
            getSessionController().getDepartment(), BillTypeAtomic.YOUR_BILL_TYPE);
} else {
    if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Department Code + Institution Code + Year + Yearly Number", false) ||
        configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy YOUR_BILL_TYPE_NAME - Prefix + Institution Code + Year + Yearly Number", false)) {
        insId = deptId; // Use same number as department
    } else {
        // Preserve old behavior: reuse deptId for insId to avoid consuming counter twice
        insId = deptId;
    }
}
```

#### **Why This Matters**
1. **Sequence Integrity**: Prevents gaps in bill number sequences
2. **Backward Compatibility**: Maintains expected behavior for existing institutions
3. **Database Consistency**: Preserves assumptions in downstream logic that relies on `deptId == insId`
4. **Performance**: Avoids unnecessary database calls to increment counters

#### **Bills Affected**
This pattern applies specifically to bill types where the original behavior was `deptId == insId`:
- **Direct Purchase Cancellation** (`PHARMACY_DIRECT_PURCHASE_CANCELLED`)
- **Purchase Order Request Cancellation** (`PHARMACY_ORDER_CANCELLED`)
- Other bill types may have different original behaviors (some may have always used different numbers)

#### **Testing Verification**
When all strategies are disabled, verify:
1. `bill.getDeptId() == bill.getInsId()` (same value)
2. Only one sequence number is consumed per bill
3. No gaps appear in bill number sequences
4. Existing institution behavior remains unchanged