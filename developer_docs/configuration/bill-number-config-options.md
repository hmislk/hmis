# Bill Number Generation Configuration Options

## Overview

This document details the specific configuration options that need to be added to `ConfigOptionApplicationController.java` to support the improved bill number generation strategy.

## Required Configuration Constants

Add these constants to `ConfigOptionApplicationController.java`:

```java
// Bill Number Generation Strategy Configuration
public static final String BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_DEPT_INS_YEAR_COUNT =
    "Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count";

public static final String BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_INS_YEAR_COUNT =
    "Bill Number Generation Strategy for Department Id is Prefix Ins Year Count";

public static final String BILL_NUMBER_GENERATION_STRATEGY_FOR_INSTITUTION_ID_IS_PREFIX_INS_YEAR_COUNT =
    "Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count";
```

## Configuration Option Details

### 1. Department ID Strategy - Full Format

**Key**: `Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count`
**Type**: Boolean
**Default**: `false`
**Description**: When enabled, generates department bill numbers in format `PREFIX/DEPT_CODE/INS_CODE/YEAR/NUMBER`

**Example Output**: `POR/ICU/MPH/25/000001`

### 2. Department ID Strategy - Institution-wide Format

**Key**: `Bill Number Generation Strategy for Department Id is Prefix Ins Year Count`
**Type**: Boolean
**Default**: `false`
**Description**: When enabled, generates department bill numbers in format `PREFIX/INS_CODE/YEAR/NUMBER` with institution-wide counting for department bills only

**Example Output**: `POR/MPH/25/000001`

### 3. Institution ID Strategy - Institution-wide Format

**Key**: `Bill Number Generation Strategy for Institution Id is Prefix Ins Year Count`
**Type**: Boolean
**Default**: `false`
**Description**: When enabled, generates institution bill numbers in format `PREFIX/INS_CODE/YEAR/NUMBER` with institution-wide counting for all bills

**Example Output**: `POR/MPH/25/000001`

## Bill Type Suffix Configuration

### Dynamic Configuration Keys

For each bill type, a suffix configuration is created dynamically:

**Pattern**: `"Bill Number Suffix for " + BillTypeAtomic.BILL_TYPE_NAME`
**Type**: String (Long Text)
**Description**: Defines the prefix for bill numbers of specific types

### Standard Bill Type Suffixes

| Bill Type | Configuration Key | Default Suffix |
|-----------|------------------|----------------|
| Purchase Order Request | `Bill Number Suffix for PHARMACY_ORDER_PRE` | `POR` |
| Purchase Order Approval | `Bill Number Suffix for PHARMACY_ORDER_APPROVAL` | `POA` |
| Purchase Order Cancellation | `Bill Number Suffix for PHARMACY_ORDER_CANCELLED` | `C-POR` |
| Pharmacy GRN | `Bill Number Suffix for PHARMACY_GRN` | `GRN` |
| Pharmacy Purchase | `Bill Number Suffix for PHARMACY_PURCHASE` | `PP` |
| Pharmacy Issue | `Bill Number Suffix for PHARMACY_ISSUE` | `PI` |
| Pharmacy Adjustment | `Bill Number Suffix for PHARMACY_ADJUSTMENT` | `PA` |
| Pharmacy Transfer | `Bill Number Suffix for PHARMACY_TRANSFER` | `PT` |

## Implementation in Controller

### Helper Methods

Add these helper methods to controllers that need bill number generation:

```java
private boolean billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount() {
    return configOptionApplicationController.getBooleanValueByKey(
        ConfigOptionApplicationController.BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_DEPT_INS_YEAR_COUNT,
        false);
}

private boolean billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount() {
    return configOptionApplicationController.getBooleanValueByKey(
        ConfigOptionApplicationController.BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_INS_YEAR_COUNT,
        false);
}

private boolean billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount() {
    return configOptionApplicationController.getBooleanValueByKey(
        ConfigOptionApplicationController.BILL_NUMBER_GENERATION_STRATEGY_FOR_INSTITUTION_ID_IS_PREFIX_INS_YEAR_COUNT,
        false);
}
```

### Suffix Configuration Method

Add this method to handle default suffix configuration:

```java
private void ensureBillNumberSuffix(BillTypeAtomic billTypeAtomic, String defaultSuffix) {
    String configKey = "Bill Number Suffix for " + billTypeAtomic;
    String billSuffix = configOptionApplicationController.getLongTextValueByKey(configKey, "");
    if (billSuffix == null || billSuffix.trim().isEmpty()) {
        configOptionApplicationController.setLongTextValueByKey(configKey, defaultSuffix);
    }
}
```

## Configuration Scenarios

### Scenario 1: Traditional Bill Numbers (Default)

```
BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_DEPT_INS_YEAR_COUNT = false
BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_INS_YEAR_COUNT = false
BILL_NUMBER_GENERATION_STRATEGY_FOR_INSTITUTION_ID_IS_PREFIX_INS_YEAR_COUNT = false
```

**Result**: Uses existing legacy bill number generation methods
**Department Bill**: `000001/2025` (legacy format)
**Institution Bill**: `000001/2025` (legacy format)

### Scenario 2: Full Department + Institution Format

```
BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_DEPT_INS_YEAR_COUNT = true
BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_INS_YEAR_COUNT = false
BILL_NUMBER_GENERATION_STRATEGY_FOR_INSTITUTION_ID_IS_PREFIX_INS_YEAR_COUNT = false
```

**Result**: Department bills include department and institution codes
**Department Bill**: `POR/ICU/MPH/25/000001`
**Institution Bill**: `POR/ICU/MPH/25/000001` (same as department)

### Scenario 3: Institution-wide Department Counting

```
BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_DEPT_INS_YEAR_COUNT = false
BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_INS_YEAR_COUNT = true
BILL_NUMBER_GENERATION_STRATEGY_FOR_INSTITUTION_ID_IS_PREFIX_INS_YEAR_COUNT = false
```

**Result**: Department bills counted institution-wide but only for department bills
**Department Bill**: `POR/MPH/25/000001`
**Institution Bill**: `POR/MPH/25/000001` (same as department)

### Scenario 4: Independent Institution Counting

```
BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_DEPT_INS_YEAR_COUNT = false
BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_INS_YEAR_COUNT = false
BILL_NUMBER_GENERATION_STRATEGY_FOR_INSTITUTION_ID_IS_PREFIX_INS_YEAR_COUNT = true
```

**Result**: Institution bills counted independently across all bill types
**Department Bill**: `000001/2025` (legacy format)
**Institution Bill**: `POR/MPH/25/000001` (new format, all bills counted)

### Scenario 5: Mixed Configuration

```
BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_DEPT_INS_YEAR_COUNT = false
BILL_NUMBER_GENERATION_STRATEGY_FOR_DEPARTMENT_ID_IS_PREFIX_INS_YEAR_COUNT = true
BILL_NUMBER_GENERATION_STRATEGY_FOR_INSTITUTION_ID_IS_PREFIX_INS_YEAR_COUNT = true
```

**Result**: Both department and institution use new format but with different counting
**Department Bill**: `POR/MPH/25/000001` (counts department bills only)
**Institution Bill**: `POR/MPH/25/000015` (counts all bills institution-wide)

## Database Configuration

### Adding Configuration through UI

1. Navigate to System Configuration → Application Configuration
2. Add new configuration option:
   - **Key**: Use the exact constant value
   - **Value Type**: Boolean for strategies, Long Text for suffixes
   - **Default Value**: `false` for strategies, appropriate suffix for bill types
   - **Description**: Brief description of what the option controls

### Direct Database Insert

```sql
-- Example: Enable department strategy with full format
INSERT INTO config_option_application (id, option_key, option_value, option_type, description)
VALUES (
    nextval('config_option_application_id_seq'),
    'Bill Number Generation Strategy for Department Id is Prefix Dept Ins Year Count',
    'true',
    'BOOLEAN',
    'Enables department bill number format: PREFIX/DEPT_CODE/INS_CODE/YEAR/NUMBER'
);

-- Example: Set POR suffix for Purchase Order Requests
INSERT INTO config_option_application (id, option_key, option_value, option_type, description)
VALUES (
    nextval('config_option_application_id_seq'),
    'Bill Number Suffix for PHARMACY_ORDER_PRE',
    'POR',
    'LONG_TEXT',
    'Bill number prefix for Purchase Order Requests'
);
```

## Migration Strategy

### Phase 1: Add Configuration Options
1. Add constants to `ConfigOptionApplicationController.java`
2. Deploy to test environment
3. Add configuration options through UI or database

### Phase 2: Test New Functionality
1. Enable one strategy at a time
2. Verify bill number formats
3. Test with different bill types

### Phase 3: Production Rollout
1. Deploy code changes
2. Keep all strategies disabled initially
3. Enable strategies per institution as requested

## Validation Rules

### Configuration Validation
- Only one department strategy should be enabled at a time
- Institution strategy is independent and can be enabled with any department strategy
- All suffixes should be 2-5 characters and alphanumeric

### Business Logic Validation
- Department strategies affect both department and institution bill numbers (unless institution strategy is separate)
- Institution strategy only affects institution bill numbers
- Legacy behavior is preserved when all strategies are disabled

## Troubleshooting Configuration Issues

### Issue: Bill numbers not changing after enabling strategy
**Solution**: Check that configuration is properly saved and controller is reading the correct value

### Issue: Numbers are not sequential
**Solution**: Verify the correct counting method is being used (institution-wide vs department-specific)

### Issue: Suffix not appearing
**Solution**: Check that suffix configuration is set and not empty/null

### Issue: Compilation errors
**Solution**: Ensure all configuration constants are properly defined and imported

## Best Practices

1. **Test Configuration Changes**: Always test in non-production environment first
2. **Document Custom Suffixes**: Keep track of institution-specific suffix preferences
3. **Monitor Performance**: New counting methods may have different performance characteristics
4. **Backup Before Changes**: Always backup configuration before making changes
5. **Gradual Rollout**: Enable one strategy at a time to isolate issues

This configuration system provides maximum flexibility while maintaining backward compatibility with existing bill number generation systems.