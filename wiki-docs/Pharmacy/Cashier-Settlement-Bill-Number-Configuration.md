# Pharmacy Cashier Settlement Bill Number Configuration

## Overview

This feature enables separate bill number generation for cashier settlement operations in the pharmacy billing workflow. Previously, when a pharmacist prepared a bill and a cashier settled it, both the PreBill and the final BilledBill shared the same bill number. This new implementation allows institutions to configure separate numbering strategies for cashier settlement bills while maintaining backward compatibility.

## Workflow Description

### Traditional Flow (Before Enhancement)
1. **Pharmacist Stage**: Creates PreBill with bill number (e.g., `PH/PHARM/INS001/24/000123`)
2. **Cashier Stage**: Creates BilledBill with **same bill number** (`PH/PHARM/INS001/24/000123`)

### New Configurable Flow (After Enhancement)
1. **Pharmacist Stage**: Creates PreBill with bill number (e.g., `PH/PHARM/INS001/24/000123`)
2. **Cashier Stage**: Creates BilledBill with **separate bill number** (e.g., `CSB/PHARM/INS001/24/000456`)

## Configuration Options

### Primary Control Configuration

| Configuration Key | Type | Default Value | Description |
|------------------|------|---------------|-------------|
| `Generate Separate Bill Numbers for Cashier Settlement - Pharmacy` | Boolean | `false` | Master switch to enable separate bill number generation for cashier settlements |

### Bill Number Generation Strategies

When the primary control is enabled, you can choose from these strategies:

| Configuration Key | Format Example | Description |
|------------------|----------------|-------------|
| `Cashier Settlement Bill Number Strategy - Prefix + Department Code + Institution Code + Year + Yearly Number` | `CSB/DEPT/INS/24/000001` | Department-specific numbering with institution code |
| `Cashier Settlement Bill Number Strategy - Prefix + Institution Code + Department Code + Year + Yearly Number` | `CSB/INS/DEPT/24/000001` | Institution-first formatting |
| `Cashier Settlement Bill Number Strategy - Prefix + Institution Code + Year + Yearly Number` | `CSB/INS/24/000001` | Institution-wide numbering (ignores departments) |

### Additional Configurations

| Configuration Key | Type | Default Value | Description |
|------------------|------|---------------|-------------|
| `Cashier Settlement Bill Number Custom Prefix` | Text | `CS` | Custom prefix for cashier settlement bills |
| `Bill Number Suffix for PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER` | Text | `CSB` | Bill type suffix for settled bills |

## Configuration Steps

### Step 1: Access Configuration
1. Navigate to **Admin → Configuration → Application Options**
2. Search for "Cashier Settlement" or "Bill Number Generation"

### Step 2: Enable the Feature
1. Set `Generate Separate Bill Numbers for Cashier Settlement - Pharmacy` to `true`

### Step 3: Choose Numbering Strategy
1. Enable **one** of the three numbering strategies:
   - Department + Institution + Year strategy
   - Institution + Department + Year strategy
   - Institution-wide strategy

### Step 4: Customize Prefix (Optional)
1. Modify `Cashier Settlement Bill Number Custom Prefix` if needed (default: "CS")
2. Modify `Bill Number Suffix for PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER` if needed (default: "CSB")

## Implementation Details

### File Modifications

#### 1. ConfigOptionApplicationController.java
**Location**: `src/main/java/com/divudi/bean/common/ConfigOptionApplicationController.java`

**Changes**: Added new configuration options in `loadPharmacyConfigurationDefaults()` method:
- Primary control configuration
- Three numbering strategy configurations
- Custom prefix configuration
- Bill suffix configuration

#### 2. BillNumberGenerator.java
**Location**: `src/main/java/com/divudi/ejb/BillNumberGenerator.java`

**Changes**: Added new methods for cashier settlement bill number generation:
- `cashierSettlementBillNumberGenerator(Bill bill)` - Main generator method
- `cashierSettlementInsIdGenerator(Bill bill)` - Institution ID generator
- Three private strategy methods for different numbering formats
- Thread-safe bill number generation with proper locking

#### 3. PharmacyPreSettleController.java
**Location**: `src/main/java/com/divudi/bean/pharmacy/PharmacyPreSettleController.java`

**Changes**: Modified `saveSaleBill()` method:
- Added configuration check for separate bill number generation
- Conditional logic for bill number generation vs. copying
- Comprehensive error handling and validation
- Detailed audit logging for tracking

### Bill Number Format Details

#### Format Components
- **Prefix/Suffix**: Configurable identifier (default: "CSB" for Cashier Settlement Bill)
- **Department Code**: Department identifier (e.g., "PHARM")
- **Institution Code**: Institution identifier (e.g., "INS001")
- **Year**: Last two digits of current year (e.g., "24" for 2024)
- **Number**: 6-digit sequential counter (e.g., "000001")
- **Delimiter**: Configurable separator (default: "/")

#### Strategy Examples

**Strategy 1**: Department + Institution + Year
```
CSB/PHARM/INS001/24/000001
CSB/PHARM/INS001/24/000002
```

**Strategy 2**: Institution + Department + Year
```
CSB/INS001/PHARM/24/000001
CSB/INS001/PHARM/24/000002
```

**Strategy 3**: Institution-wide
```
CSB/INS001/24/000001
CSB/INS001/24/000002
```

## Database Impact

### No Schema Changes Required
- Utilizes existing `Bill.deptId` and `Bill.insId` fields
- Uses existing `BillNumber` entity for sequence management
- Maintains referential integrity through `Bill.referenceBill`

### New Bill Number Sequences
When separate numbering is enabled, the system creates new `BillNumber` entities for:
- `BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER`
- Scoped by Institution, Department (where applicable), and Year

## Backward Compatibility

### Default Behavior Preserved
- **Default configuration**: `Generate Separate Bill Numbers for Cashier Settlement - Pharmacy = false`
- **Existing behavior**: Bill numbers are copied from PreBill to BilledBill
- **No impact**: on existing installations until explicitly enabled

### Migration Strategy
1. **Phase 1**: Deploy with default configuration (disabled)
2. **Phase 2**: Enable per institution/department as needed
3. **Phase 3**: Monitor and adjust based on requirements

## Audit Trail and Logging

### Log Messages
The system generates detailed log messages for tracking:

#### When Separate Bill Numbers are Generated:
```
CASHIER_SETTLEMENT_BILL_NUMBER_GENERATION: User=John Doe, Department=Pharmacy, Institution=General Hospital, OriginalBillNumber=PH/PHARM/INS001/24/000123, NewDeptId=CSB/PHARM/INS001/24/000456, NewInsId=CSB/PHARM/INS001/24/000456, PaymentMethod=Cash, NetTotal=1500.00, Timestamp=2024-11-30 14:30:25
```

#### When Bill Numbers are Copied (Default Behavior):
```
CASHIER_SETTLEMENT_BILL_NUMBER_COPY: OriginalBillNumber=PH/PHARM/INS001/24/000123, CopiedDeptId=PH/PHARM/INS001/24/000123, CopiedInsId=PH/PHARM/INS001/24/000123, Timestamp=2024-11-30 14:30:25
```

### Error Handling
- **Graceful fallback**: If bill number generation fails, system copies original numbers
- **User notification**: Error messages displayed to cashier if generation fails
- **Detailed logging**: Complete error stack traces logged for troubleshooting

## Technical Specifications

### Thread Safety
- Uses `ReentrantLock` for concurrent bill number generation
- Lock scope: Institution + Department + BillTypeAtomic
- Prevents duplicate bill number generation under high load

### Performance Considerations
- **Minimal overhead**: Configuration checks are lightweight
- **Database calls**: One additional query per bill when enabled
- **Caching**: Configuration values are cached at application level

### Entity Relationships
- **PreBill**: Original bill created by pharmacist
- **BilledBill**: Final bill created by cashier
- **Relationship**: `BilledBill.referenceBill` points to `PreBill`
- **Bi-directional**: `PreBill.referenceBill` updated to point to `BilledBill`

## Testing Scenarios

### Test Case 1: Default Behavior (Disabled)
1. Set configuration to `false`
2. Create pharmacy bill at pharmacist stage
3. Settle bill at cashier stage
4. **Verify**: Both bills have same bill number
5. **Verify**: Log shows "CASHIER_SETTLEMENT_BILL_NUMBER_COPY"

### Test Case 2: Separate Numbering (Enabled)
1. Set configuration to `true`
2. Enable one numbering strategy
3. Create pharmacy bill at pharmacist stage
4. Settle bill at cashier stage
5. **Verify**: Bills have different bill numbers
6. **Verify**: Log shows "CASHIER_SETTLEMENT_BILL_NUMBER_GENERATION"
7. **Verify**: Bill number format matches selected strategy

### Test Case 3: Error Handling
1. Enable separate numbering
2. Simulate database connection failure during bill number generation
3. **Verify**: System falls back to copying original bill numbers
4. **Verify**: User sees error message
5. **Verify**: Transaction completes successfully

### Test Case 4: Concurrent Access
1. Enable separate numbering
2. Simulate multiple cashiers settling bills simultaneously
3. **Verify**: No duplicate bill numbers generated
4. **Verify**: All bills have sequential, unique numbers

## Troubleshooting

### Common Issues

#### Issue: Bills Still Use Same Numbers After Enabling
**Solution**:
- Verify primary configuration is set to `true`
- Ensure at least one strategy is enabled
- Check system logs for configuration loading

#### Issue: Bill Number Generation Fails
**Solution**:
- Check database connectivity
- Verify `BillNumber` table has proper permissions
- Review application logs for detailed error messages

#### Issue: Numbers Not Sequential
**Explanation**:
- Normal behavior when multiple strategies are used
- Each strategy maintains separate counters
- Institution-wide vs department-specific counters are independent

### Log Analysis
Monitor these log patterns:
- `CASHIER_SETTLEMENT_BILL_NUMBER_*`: Track bill number operations
- `WARNING: Failed to generate*`: Identify generation failures
- `ERROR: Exception occurred during*`: Critical errors requiring investigation

## Support and Maintenance

### Configuration Backup
Before making changes:
1. Export current configuration options
2. Document current bill numbering patterns
3. Plan rollback strategy if needed

### Monitoring
Regular checks:
- Bill number sequence continuity
- Log file sizes and rotation
- Database performance impact

### Future Enhancements
Potential improvements:
- Additional numbering strategies
- Custom bill number formats via templates
- Integration with external numbering systems
- Department-specific prefix configuration

---

## Quick Reference

### Enable Feature
```
Generate Separate Bill Numbers for Cashier Settlement - Pharmacy = true
```

### Choose Strategy (pick one)
```
Cashier Settlement Bill Number Strategy - Prefix + Department Code + Institution Code + Year + Yearly Number = true
```

### Customize Prefix
```
Cashier Settlement Bill Number Custom Prefix = "CS"
Bill Number Suffix for PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER = "CSB"
```

### Monitor Logs
```bash
tail -f logs/application.log | grep "CASHIER_SETTLEMENT_BILL_NUMBER"
```