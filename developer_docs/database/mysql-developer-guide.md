# MySQL Developer Guide for HMIS Project

## Overview

This guide covers MySQL database development, debugging, and maintenance practices for the HMIS project. It includes credential management, debugging techniques, and common database operations.

## Credentials Management

### Security Policy
- **🚨 CRITICAL**: MySQL credentials MUST NEVER be committed to git
- Credentials are stored in a separate folder outside the project directory
- Each development environment has its own credential location
- Credentials folder is explicitly excluded from all git operations

### Environment-Specific Credential Locations

#### Windows Development Environments
- **Primary Location**: `C:\Credentials\credentials.txt`
- **Backup Location**: `C:\Credentials\mysql-credentials.txt`
- **Format**:
  ```
  JNDI Name: jdbc/coop
  URL - jdbc:mysql://localhost:3306/coop?zeroDateTimeBehavior=CONVERT_TO_NULL
  User - root
  Password - [password]
  ```

#### Linux/Mac Development Environments
- **Primary Location**: `~/.config/hmis/credentials.txt`
- **Backup Location**: `~/.credentials/mysql-credentials.txt`
- **Format**: Same as Windows

#### Server Environments
- **Location**: Environment-specific secure storage
- **Access**: Through environment variables or secure credential management systems

### Setting Up Credentials

1. **Create credentials directory** (outside project folder):
   ```bash
   # Windows
   mkdir C:\Credentials
   
   # Linux/Mac
   mkdir ~/.config/hmis
   ```

2. **Create credentials file**:
   ```bash
   # Windows
   notepad C:\Credentials\credentials.txt
   
   # Linux/Mac
   nano ~/.config/hmis/credentials.txt
   ```

3. **Set appropriate permissions**:
   ```bash
   # Linux/Mac only
   chmod 600 ~/.config/hmis/credentials.txt
   ```

## MySQL Command Line Operations

### Basic Connection
```bash
# Using credentials from file
mysql -u [username] -p[password] -h [host] [database]

# Example with HMIS database
mysql -u root -p -h localhost coop
# You will be prompted for the password. Alternatively:
# mysql --user=root --password --host=localhost coop
```

### Essential Commands for HMIS

#### Database Structure Investigation
```sql
-- Show all tables
SHOW TABLES;

-- Describe table structure
DESCRIBE bill;
DESCRIBE pharmaceuticalbillitem;
DESCRIBE billitem;

-- Find columns by pattern
SHOW COLUMNS FROM billitem LIKE '%pharmaceutical%';

-- Use information schema for complex queries
SELECT COLUMN_NAME, DATA_TYPE 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'bill' 
AND TABLE_SCHEMA = 'coop' 
AND COLUMN_NAME LIKE '%completed%';
```

#### Bill and Order Analysis
```sql
-- Check Purchase Order completion status
SELECT b.id, b.deptId, b.createdAt, b.completed, b.completedAt
FROM bill b 
WHERE b.billTypeAtomic = 'PHARMACY_ORDER_APPROVAL' 
AND b.retired = FALSE 
ORDER BY b.createdAt DESC 
LIMIT 10;

-- Find GRNs for a specific Purchase Order
SELECT bi.id, bi.qty, pbi.qty as received_qty, pbi.freeqty as received_free 
FROM billitem bi 
JOIN pharmaceuticalbillitem pbi ON pbi.billitem_id = bi.id 
JOIN bill b ON bi.bill_id = b.id 
WHERE b.referenceBill_id = [PO_ID] 
AND b.billTypeAtomic = 'PHARMACY_GRN' 
AND bi.retired = FALSE;

-- Check if Purchase Order should be completed
SELECT 
    po.id as po_id,
    po.deptId as po_number,
    po.completed,
    COUNT(poi.id) as total_items,
    SUM(CASE WHEN poi.qty <= COALESCE(grn_summary.received_qty, 0) THEN 1 ELSE 0 END) as fully_received_items
FROM bill po
JOIN billitem poi ON poi.bill_id = po.id
LEFT JOIN (
    SELECT 
        poi_ref.id as po_item_id,
        SUM(ABS(grn_pbi.qty)) as received_qty
    FROM billitem poi_ref
    JOIN billitem grn_bi ON grn_bi.referanceBillItem_id = poi_ref.id
    JOIN pharmaceuticalbillitem grn_pbi ON grn_pbi.billitem_id = grn_bi.id
    JOIN bill grn_b ON grn_bi.bill_id = grn_b.id
    WHERE grn_b.billTypeAtomic = 'PHARMACY_GRN'
    AND grn_b.retired = FALSE
    AND grn_bi.retired = FALSE
    GROUP BY poi_ref.id
) grn_summary ON grn_summary.po_item_id = poi.id
WHERE po.billTypeAtomic = 'PHARMACY_ORDER_APPROVAL'
AND po.id = [PO_ID]
AND po.retired = FALSE
AND poi.retired = FALSE
GROUP BY po.id, po.deptId, po.completed;
```

#### Transfer Request and Issue Analysis
```sql
-- Check Transfer Request completion
SELECT b.id, b.deptId, b.completed, b.createdAt
FROM bill b 
WHERE b.billTypeAtomic = 'PHARMACY_TRANSFER_REQUEST' 
AND b.retired = FALSE 
ORDER BY b.createdAt DESC 
LIMIT 10;

-- Find Transfer Issues for a Request
SELECT b.id, b.deptId, b.createdAt, b.completed
FROM bill b 
WHERE b.backwardReferenceBill_id = [REQUEST_ID]
AND b.billTypeAtomic = 'PHARMACY_ISSUE'
AND b.retired = FALSE;

-- Check Transfer Issue completion (receiving side)
SELECT b.id, b.deptId, b.completed, b.createdAt
FROM bill b 
WHERE b.billTypeAtomic = 'PHARMACY_ISSUE' 
AND b.toDepartment_id = [DEPARTMENT_ID]
AND b.retired = FALSE 
ORDER BY b.createdAt DESC;
```

## Debugging Techniques

### Performance Issue Investigation

#### Identify Slow Queries
```sql
-- Enable slow query log
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';
SET GLOBAL long_query_time = 2;

-- Check current performance settings
SHOW VARIABLES LIKE 'slow_query_log%';
SHOW VARIABLES LIKE 'long_query_time';
```

#### Analyze Query Performance
```sql
-- Use EXPLAIN to understand query execution
EXPLAIN SELECT b.id, b.deptId, b.completed 
FROM bill b 
WHERE b.billTypeAtomic = 'PHARMACY_ORDER_APPROVAL' 
AND b.retired = FALSE;

-- Check index usage
SHOW INDEX FROM bill;
SHOW INDEX FROM billitem;
```

### Data Consistency Checks

#### Orphaned Records
```sql
-- Find billitems without valid bills
SELECT bi.id, bi.bill_id 
FROM billitem bi 
LEFT JOIN bill b ON bi.bill_id = b.id 
WHERE b.id IS NULL 
LIMIT 10;

-- Find pharmaceuticalbillitems without valid billitems
SELECT pbi.id, pbi.billitem_id 
FROM pharmaceuticalbillitem pbi 
LEFT JOIN billitem bi ON pbi.billitem_id = bi.id 
WHERE bi.id IS NULL 
LIMIT 10;
```

#### Bill Status Consistency
```sql
-- Find completed bills without completion timestamp
SELECT id, deptId, completed, completedAt 
FROM bill 
WHERE completed = TRUE 
AND completedAt IS NULL;

-- Find bills with completion timestamp but not marked completed
SELECT id, deptId, completed, completedAt 
FROM bill 
WHERE completed = FALSE 
AND completedAt IS NOT NULL;
```

### Common Debugging Scenarios

#### Purchase Order Not Showing as Completed
1. **Check PO status**:
   ```sql
   SELECT id, deptId, completed, completedAt FROM bill WHERE id = [PO_ID];
   ```

2. **Check PO items and received quantities**:
   ```sql
   SELECT 
       poi.id as po_item_id,
       poi.qty as ordered_qty,
       po_pbi.freeqty as ordered_free,
       COALESCE(SUM(ABS(grn_pbi.qty)), 0) as received_qty,
       COALESCE(SUM(ABS(grn_pbi.freeqty)), 0) as received_free
   FROM billitem poi
   JOIN pharmaceuticalbillitem po_pbi ON po_pbi.billitem_id = poi.id
   LEFT JOIN billitem grn_bi ON grn_bi.referanceBillItem_id = poi.id
   LEFT JOIN pharmaceuticalbillitem grn_pbi ON grn_pbi.billitem_id = grn_bi.id
   LEFT JOIN bill grn_b ON grn_bi.bill_id = grn_b.id AND grn_b.billTypeAtomic = 'PHARMACY_GRN'
   WHERE poi.bill_id = [PO_ID] AND poi.retired = FALSE
   GROUP BY poi.id, poi.qty, po_pbi.freeqty;
   ```

3. **Manual completion update** (if needed):
   ```sql
   UPDATE bill 
   SET completed = TRUE, 
       completedAt = NOW(), 
       completedBy_id = (SELECT id FROM webuser WHERE name = 'Admin' LIMIT 1)
   WHERE id = [PO_ID];
   ```

#### Transfer Request/Issue Debugging
Similar patterns apply for transfer requests and issues, using their respective BillTypeAtomic values.

## Database Schema Understanding

### Key Tables for Pharmacy Operations

#### bill
- **Purpose**: Main bill/transaction table
- **Key Fields**: 
  - `billTypeAtomic`: Defines the type (PHARMACY_ORDER_APPROVAL, PHARMACY_GRN, etc.)
  - `completed`: Boolean flag for completion status
  - `completedAt`, `completedBy_id`: Completion audit fields
  - `referenceBill_id`: Links GRNs to POs
  - `backwardReferenceBill_id`: Links issues to requests

#### billitem
- **Purpose**: Line items for bills
- **Key Fields**:
  - `bill_id`: References parent bill
  - `qty`: Item quantity
  - `referanceBillItem_id`: Links GRN items to PO items
  - `rate`: Item rate
  - `netRate`: Net rate after discounts
  - `netValue`: Net value (netRate × qty)
  - `grossValue`: Gross value

#### billitemfinancedetails
- **Purpose**: Detailed financial information for bill items
- **Key Fields**:
  - `billItem_id`: References billitem
  - `quantity`: User-entered quantity (AMP: units, AMPP: packs)
  - `quantityByUnits`: Always in units (calculated for AMPP)
  - `lineGrossRate`: Gross rate per user-entered unit
  - `lineNetRate`: Net rate per user-entered unit
  - `valueatPurchaseRate`: Total value at purchase rate
  - `valueatRetailRate`: Total value at retail rate

**🚨 CRITICAL BUSINESS RULES for BillItemFinanceDetails**:
1. **User-entered quantities**: 
   - **AMP items**: `quantity` = units (same as `quantityByUnits`)
   - **AMPP items**: `quantity` = packs, `quantityByUnits` = packs × unitsPerPack
2. **Rates match user input**: 
   - **AMP**: Rate per unit
   - **AMPP**: Rate per pack
3. **Calculated values**: Always populate `valueatPurchaseRate`, `valueatRetailRate`, etc.

#### pharmaceuticalbillitem
- **Purpose**: Pharmacy-specific item data
- **Key Fields**:
  - `billitem_id`: References billitem
  - `qty`: Pharmaceutical quantity (ALWAYS in units)
  - `freeqty`: Free quantity (ALWAYS in units)
  - `purchaseRate`: Purchase price (ALWAYS positive)
  - `retailRate`: Retail price (ALWAYS positive)
  - `purchaseValue`: Calculated value (qty × purchaseRate)
  - `retailValue`: Calculated value (qty × retailRate)

**🚨 CRITICAL BUSINESS RULES for PharmaceuticalBillItem**:
1. **Quantities ALWAYS in units**: Both `qty` and `freeqty` are stored in units, never in packs
2. **Stock flow direction**: 
   - **Incoming stock** (Purchase, GRN, Receive): Positive quantities (+)
   - **Outgoing stock** (Issue, Sale, Return): Negative quantities (-)
3. **Rates ALWAYS positive**: All rate fields (`purchaseRate`, `retailRate`, etc.) are always positive values
4. **Values calculated**: `purchaseValue` = `qty` × `purchaseRate`, `retailValue` = `qty` × `retailRate`

### BillTypeAtomic Values for Pharmacy
- `PHARMACY_ORDER_APPROVAL`: Approved Purchase Orders
- `PHARMACY_GRN`: Goods Received Notes
- `PHARMACY_TRANSFER_REQUEST`: Transfer Requests
- `PHARMACY_ISSUE`: Transfer Issues
- `PHARMACY_RECEIVE`: Transfer Receives (deprecated in favor of PHARMACY_ISSUE)

## Best Practices

### Query Performance
1. **Always use indexes** on frequently queried columns
2. **Limit result sets** in development queries
3. **Use EXPLAIN** to understand query execution plans
4. **Avoid SELECT *** in production code

### Data Integrity
1. **Check foreign key constraints** before data manipulation
2. **Use transactions** for multi-table operations
3. **Validate data consistency** after bulk operations
4. **Backup before major data changes**

### Security
1. **Never log passwords** in application logs
2. **Use parameterized queries** to prevent SQL injection
3. **Rotate database passwords** regularly
4. **Limit database user privileges** to necessary operations only

## Integration with Application Code

### Entity Relationships
- Bill ↔ BillItem (one-to-many)
- BillItem ↔ PharmaceuticalBillItem (one-to-one)
- Purchase Order ↔ GRN (one-to-many via referenceBill_id)
- Transfer Request ↔ Transfer Issue (one-to-many via backwardReferenceBill_id)

### Completion Logic Flow
1. **Purchase Orders**: Completed when all items fully received via GRN
2. **Transfer Requests**: Completed when all items fully issued
3. **Transfer Issues**: Completed when all items fully received at destination

## Troubleshooting Common Issues

### Issue: "Button not disabled after full quantity received"
**Diagnosis**: Check if completion logic is implemented in all relevant controllers
**Solution**: Ensure completion logic exists in both GrnController and GrnCostingController

### Issue: "Completion status not updating in database"
**Diagnosis**: Check if the settle/completion method is being called
**Solution**: Verify controller method execution and database transaction success

### Issue: "Performance degradation in bill queries"
**Diagnosis**: Check for missing indexes or N+1 query problems
**Solution**: Add appropriate indexes and optimize query patterns

## References

- [CLAUDE.md](../CLAUDE.md) - Main project configuration and rules
- [DTO Implementation Guidelines](../dto/implementation-guidelines.md)
- [Persistence Workflow](../persistence/persistence-workflow.md)
- [QA Deployment Guide](../deployment/qa-deployment-guide.md)

## Security Notice

**⚠️ WARNING**: This document contains references to database operations that can modify production data. Always:
1. Use development/staging environments for testing
2. Create backups before data modifications
3. Review all SQL statements before execution
4. Never commit actual credentials to version control