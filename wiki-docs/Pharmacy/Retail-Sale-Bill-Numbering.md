# Pharmacy Retail Sale Bill Numbering

## Overview

The pharmacy retail sale system uses a three-tier bill numbering approach with a structured format to accommodate different operational workflows and ensure proper tracking of transactions and inventory movements across multiple institutions and departments.

## Bill Number Format

The system uses the following standardized format:
```
[Institution Code]/[Department Code]/[Suffix]/[Year]/[Sequential Number]
```

**Example**: `RH/MP/RSD/25/000045`

Where:
- **Institution Code**: 2-3 character configurable code (e.g., "RH")
- **Department Code**: 2-3 character pharmacy department code (e.g., "MP")
- **Suffix**: Bill type identifier (e.g., "RSD" for retail sale)
- **Year**: Last 2 digits of current year (e.g., "25" for 2025)
- **Sequential Number**: 6-digit zero-padded counter (e.g., "000045")
- **Delimiter**: Configurable separator (default: "/")

## Bill Types and Suffixes

### 1. Retail Sale with Payment (Complete Transaction)
- **Bill Type**: `PHARMACY_RETAIL_SALE`
- **Suffix**: Configurable (typically "S" or "RSD")
- **Description**: Bills generated when pharmacist issues medicines and accepts payment in a single transaction
- **Workflow**: Medicine dispensing → Payment collection → Bill generation
- **Example Number**: `RH/MP/RSD/25/000045`
- **Use Case**: Standard retail sales where customer pays immediately

### 2. Medicine Issue Only (No Payment)
- **Bill Type**: `PHARMACY_ISSUE`
- **Suffix**: Configurable (typically "ISS" or "MI")
- **Description**: Bills generated when pharmacist issues medicines without collecting payment
- **Workflow**: Medicine dispensing → Bill generation (payment pending)
- **Example Number**: `RH/MP/ISS/25/000023`
- **Use Case**: Credit sales, insurance claims, or deferred payment scenarios

### 3. Payment Collection Only (For Previously Issued Medicines)
- **Bill Type**: `PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS`
- **Suffix**: Configurable (typically "PC" or "PAY")
- **Description**: Bills generated when cashier collects payment for previously issued medicines
- **Workflow**: Payment collection → Bill generation (references medicine issue bill)
- **Example Number**: `RH/MP/PC/25/000012`
- **Use Case**: Payment for medicines previously issued by pharmacists

## Numbering System Rationale

### Why Three Separate Number Series?

#### Option 1: Single Sequential Numbering (Not Recommended)
- **Approach**: One continuous number sequence for all three bill types
- **Problems**:
  - Returns processing becomes complex and confusing
  - Difficult to distinguish between complete sales and partial transactions
  - Challenging to match medicine issues with corresponding payments
  - Audit trail complications

#### Option 2: Two Number Series (Considered but Not Optimal)
- **Approach**: One series for money transactions, another for medicine issues
- **Problems**:
  - Still creates confusion during returns
  - Doesn't clearly separate complete transactions from partial ones

#### Option 3: Three Number Series (Current Implementation) ✅
- **Approach**: Separate numbering for each bill type
- **Advantages**:
  - Clear transaction identification
  - Simplified returns processing
  - Better audit trail
  - Easy matching of issue and payment bills
  - Reduced confusion for staff and customers

## Implementation Benefits

### For Pharmacists
- Clear distinction between different transaction types
- Easy identification of pending payments
- Simplified medicine return processing

### For Cashiers
- Clear reference to medicine issue bills when collecting payments
- Better cash flow tracking
- Reduced confusion during reconciliation

### For Management
- Better financial reporting
- Clear audit trails
- Simplified inventory tracking
- Enhanced control over credit sales

### For Returns Processing
- Easy identification of original transaction type
- Simplified refund processing
- Clear tracking of returned medicines
- Better inventory adjustment management

## Configuration Options

### Institution and Department Codes
- **Institution Code**: Configured in Institution settings (e.g., "RH" for Regional Hospital)
- **Department Code**: Configured in Department settings (e.g., "MP" for Main Pharmacy)
- **Configurable per hospital**: Each institution can customize their codes

### Bill Type Suffixes
The system allows configuration of suffixes for each bill type:

| Bill Type | Default Suffix | Configurable Options | Description |
|-----------|----------------|---------------------|-------------|
| Retail Sale with Payment | "S" or "RSD" | Any 2-4 characters | Complete transaction |
| Medicine Issue Only | "ISS" or "MI" | Any 2-4 characters | Issue without payment |
| Payment Collection Only | "PC" or "PAY" | Any 2-4 characters | Payment for issued medicines |
| Sale Cancellation | "SC" | Any 2-4 characters | Cancelled retail sale |
| Return with Items | "RIT" | Any 2-4 characters | Return items only |
| Return with Payment | "RIP" | Any 2-4 characters | Return with payment adjustment |

### Number Generation Strategies
The system supports multiple numbering strategies:

1. **Separate for All** (Recommended): Institution + Department + Bill Type specific numbering
2. **Institution Only**: Separate numbering per institution
3. **Department Only**: Separate numbering per department
4. **Bill Type Only**: Separate numbering per bill type
5. **Common Numbering**: Single sequence for all bills (not recommended)

### Configuration File Settings
```properties
# Bill Number Configuration
bill.number.include.institution.code=true
bill.number.delimiter=/
bill.number.suffix.pharmacy.retail.sale=RSD
bill.number.suffix.pharmacy.issue=ISS
bill.number.suffix.pharmacy.payment=PC
```

## Workflow Integration

### Standard Sale Process
1. Customer requests medicine
2. Pharmacist dispenses medicine and collects payment
3. System generates **Retail Sale Bill** with format: `RH/MP/RSD/25/000045`

### Credit Sale Process
1. Customer requests medicine (credit approved)
2. Pharmacist dispenses medicine
3. System generates **Medicine Issue Bill** with format: `RH/MP/ISS/25/000023`
4. Later: Customer/cashier processes payment
5. System generates **Payment Collection Bill** with format: `RH/MP/PC/25/000012` (references original issue bill)

### Return Process
- Returns reference original bill number and type
- **Item Return Only**: Format `RH/MP/RIT/25/000008`
- **Payment Return Only**: Format `RH/MP/RIP/25/000009`
- **Complete Return**: Format `RH/MP/RTN/25/000010`
- System can easily identify transaction history through bill number structure
- Inventory adjustments are properly tracked with bill type-specific numbering

### Bill Number Lookup and Tracking
- Each component of the bill number provides specific information:
  - **Institution identification** through institution code
  - **Department identification** through department code
  - **Transaction type** through suffix code
  - **Temporal grouping** through year
  - **Sequential tracking** through counter
- Cross-referencing between related bills (issue → payment) is simplified
- Audit trails are maintained through structured numbering

## Conclusion

The three-tier bill numbering system provides clarity, reduces confusion, and ensures proper tracking of all pharmacy retail transactions while maintaining simple and efficient returns processing.