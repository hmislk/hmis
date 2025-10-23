# Stock Ledger Report

## Overview

The Stock Ledger Report shows the complete transaction history for pharmaceutical items over a selected period. This report displays every stock movement (in and out) with running stock balances, helping pharmacy staff track inventory movements, investigate discrepancies, and maintain audit trails.

## When to Use This Report

Use the Stock Ledger Report when you need to:

- **Track item movements** - See all transactions for an item over time
- **Investigate stock discrepancies** - Trace where stock came from and where it went
- **Audit stock records** - Verify transaction accuracy and completeness
- **Reconcile stock levels** - Match physical stock with system records
- **Prepare audit documentation** - Generate detailed transaction trails
- **Monitor department activities** - Review stock transfers and usage patterns

## Navigation

To access the Stock Ledger Report:

1. Click on the **Reports / Analytics** icon in the main menu
2. Select **Reports** submenu
3. The Reports page will open
4. Navigate to **Inventory Reports** section
5. Click on **12. Stock Ledger Report**

## Report Types

The Stock Ledger Report offers two viewing options that determine how closing stock is calculated and displayed:

### By Batch Report

Shows transaction history with batch-level stock tracking.

**Best for:**
- Batch-specific investigations
- Tracking specific batch movements
- First-Expiry-First-Out (FEFO) verification
- Expiry date monitoring
- Detailed batch audits

**Displays:**
- Each transaction shows the batch involved
- Closing stock for that specific batch
- Batch number and expiry date
- Batch-level purchase and cost rates

**Closing Stock Calculation:**
- Shows quantity remaining in that specific batch
- Each row shows the batch stock after that transaction
- Useful for verifying batch-level accuracy

### By Item Report

Shows transaction history with item-level stock tracking (all batches combined).

**Best for:**
- Overall item movement analysis
- Department-wide stock reconciliation
- Quick stock verification
- Management reporting
- Financial valuation

**Displays:**
- Each transaction shows the item
- Closing stock for the item across all batches
- Aggregated quantities
- Average rates across batches

**Closing Stock Calculation:**
- Shows total quantity across all batches of that item
- Each row shows the total item stock after that transaction
- Useful for overall inventory management

## Understanding the Filters

### Date Range Filters

#### From Date
- **Purpose**: Start date for the transaction period
- **Format**: Date and time
- **Example**: "January 1, 2025 00:00"
- **Tip**: Include time to capture transactions from the start of the day

#### To Date
- **Purpose**: End date for the transaction period
- **Format**: Date and time
- **Example**: "January 31, 2025 23:59"
- **Tip**: Set to end of day to include all transactions

**Note**: The report shows all transactions that occurred between these dates.

### Document Type Filter

Filter transactions by specific document types:

- **All Document Types** - Shows all transactions (default)
- **OP Sale** - Outpatient pharmacy sales
- **IP Sale** - Inpatient pharmacy issues
- **Transfer Issue** - Stock transfers sent to other departments
- **Transfer Receive** - Stock transfers received from other departments
- **Consumption** - Internal department consumption
- **Purchase** - Direct purchases
- **GRN** - Goods Received Notes
- **Return Without Receipt** - Returns from patients/departments
- **Stock Adjustments** - Manual stock corrections
- **Rate Adjustments** - Price/rate corrections

**Use this filter to:**
- Focus on specific transaction types
- Investigate particular activities
- Reconcile specific document categories

### Institution Filter

- **Purpose**: Filter by institution
- **Options**: All Institutions or select specific institution
- **Leave blank**: Shows transactions from all institutions
- **Select institution**: Shows only transactions from that institution

**Impact on Closing Stock:**
- When institution is selected: Closing stock shows institution-level totals
- When blank: Closing stock shows system-wide totals

### Site Filter

- **Purpose**: Filter by site/location
- **Works with**: Institution filter for more specific results
- **Leave blank**: Shows all sites
- **Select site**: Shows only transactions at that site

**Impact on Closing Stock:**
- Narrows down the institution filter
- Helps focus on specific locations

### Department Filter

- **Purpose**: Filter by specific department/store
- **This is the most commonly used filter**
- **Dynamic**: Options change based on institution and site selection
- **Leave blank**: Shows all departments
- **Select department**: Shows only that department's transactions

**Impact on Closing Stock:**
- **When department is selected**: Closing stock shows department-level stock only
  - Badge displays: "Dept"
  - Most precise stock level
  - Use for: Daily operations, department audits

- **When department is blank but institution is selected**: Closing stock shows institution-level totals
  - Badge displays: "Ins"
  - Aggregates all departments in that institution
  - Use for: Institution-wide reporting

- **When both department and institution are blank**: Closing stock shows system-wide totals
  - Badge displays: "Tot"
  - Total across entire system
  - Use for: Corporate reporting, overall inventory

### Item Name Filter

- **Purpose**: Search for specific item
- **Type**: Autocomplete search
- **Minimum characters**: 3
- **Maximum results**: 20 suggestions
- **Leave blank**: Shows all items
- **Select item**: Shows only transactions for that item

**Tip**: Type the item name or code to see suggestions

### Report Type Filter

- **Purpose**: Choose between batch-level or item-level stock tracking
- **Options**:
  - **By Batch** - Shows batch-specific stock (default)
  - **By Item** - Shows aggregated item stock

**This is a critical filter** - see "Report Types" section above for details

## How to Generate the Report

### Step 1: Access the Report
Follow the navigation path described above to reach the Stock Ledger Report page.

### Step 2: Set Date Range
1. Select **From Date** - Start of the period you want to review
2. Select **To Date** - End of the period you want to review

**Example**: For January transactions, set From Date to "January 1, 2025 00:00" and To Date to "January 31, 2025 23:59"

### Step 3: Choose Report Type
Select either:
- **By Batch** - For batch-level stock tracking (default)
- **By Item** - For item-level stock tracking

### Step 4: Apply Filters (Optional but Recommended)

**For Department-Specific Review:**
1. Leave Institution and Site as "All"
2. Select specific Department
3. Leave Item blank to see all items
4. Leave Document Type as "All"

**For Specific Item Investigation:**
1. Select Department (optional)
2. Type item name in Item filter
3. Leave Document Type as "All"

**For Specific Transaction Type:**
1. Select Department
2. Select Document Type (e.g., "Transfer Issue")
3. Leave Item blank

### Step 5: Process the Report
Click the **Process** button to generate the report.

### Step 6: Review Results
The report will display all matching transactions in chronological order.

### Step 7: Export Results
Choose from:
- **Print All** - Print the entire report
- **Download (Excel)** - Export to Excel spreadsheet
- **PDF** - Generate PDF document (uses dedicated export method)

## Understanding the Report Columns

| Column | Description |
|--------|-------------|
| Department | Department where transaction occurred |
| Category | Item category (e.g., Antibiotics, Analgesics) |
| Item | Item code |
| Name | Full item name |
| UOM | Unit of Measurement (tablets, bottles, etc.) |
| Transaction Type | STOCK IN (green badge) or STOCK OUT (red badge) |
| Bill Number | Reference number for the transaction (clickable link) |
| Transaction At | Date and time of transaction |
| Reference Bill Number | Original bill being referenced (for returns, etc.) |
| Ref Transaction at | Date and time of reference transaction |
| From Store | Source department (for transfers) |
| To Store | Destination department (for transfers, sales) |
| Consumption Department | Department consuming the stock |
| Document Type | Type of transaction document |
| Stock In Qty in units | Quantity added to stock (if stock in) |
| Stock Out Qty in units | Quantity removed from stock (if stock out) |
| **Closing Stock** | **Running balance after this transaction** |
| Rate | Purchase rate per unit |
| Cost Rate | Cost rate per unit |
| Closing Stock Value at Purchase Rate | Total value at purchase rate |
| Closing Stock Value at Cost Rate | Total value at cost rate |
| Closing Stock Value at Retail Rate | Total value at retail rate |
| Batch Code | Batch number |
| MRP | Maximum Retail Price |
| Expiry Date | Batch expiry date |
| User | User who created the transaction |

## Understanding Closing Stock Column

The **Closing Stock** column is dynamic and changes based on your filters:

### Closing Stock Badge Indicators

The column header shows a badge indicating the scope level:

#### "Batch" Badge
- Appears when: Report Type is "By Batch"
- Shows: Stock quantity for that specific batch
- Calculation varies by filter:
  - **Dept selected**: Batch stock in that department only
  - **Ins selected**: Batch stock across all departments in that institution
  - **Neither selected**: Batch stock across entire system

#### "Item" Badge
- Appears when: Report Type is "By Item"
- Shows: Total stock for that item across all batches
- Calculation varies by filter:
  - **Dept selected**: Item stock in that department only (all batches combined)
  - **Ins selected**: Item stock across all departments in that institution (all batches combined)
  - **Neither selected**: Item stock across entire system (all batches combined)

### Examples of Closing Stock Behavior

#### Example 1: Department Selected, By Batch Report
```
Report Type: By Batch
Department: Main Pharmacy
Item: Paracetamol 500mg
Batch: B001

Closing Stock shows: Quantity of Batch B001 in Main Pharmacy only
Badge: "Batch"
```

#### Example 2: Department Selected, By Item Report
```
Report Type: By Item
Department: Main Pharmacy
Item: Paracetamol 500mg

Closing Stock shows: Total Paracetamol in Main Pharmacy (all batches combined)
Badge: "Item"
```

#### Example 3: Institution Selected, By Batch Report
```
Report Type: By Batch
Institution: City Hospital (no department selected)
Item: Paracetamol 500mg
Batch: B001

Closing Stock shows: Quantity of Batch B001 across all departments in City Hospital
Badge: "Batch"
```

#### Example 4: No Department/Institution, By Item Report
```
Report Type: By Item
Institution: All
Department: All
Item: Paracetamol 500mg

Closing Stock shows: Total Paracetamol across entire system (all batches, all departments)
Badge: "Item"
```

## Common Use Cases

### 1. Investigate Missing Stock

**Scenario**: Physical count shows less stock than system

**Steps**:
1. Select department
2. Select specific item
3. Choose "By Batch" report type
4. Set date range to cover recent period
5. Process report
6. Review each transaction to find discrepancies

### 2. Track Item Movement Over Month

**Scenario**: Need to see all movements for an item in January

**Steps**:
1. Set From Date: January 1, 2025 00:00
2. Set To Date: January 31, 2025 23:59
3. Select specific item
4. Choose "By Item" report type
5. Process report
6. Export to Excel for analysis

### 3. Verify Transfer Transactions

**Scenario**: Need to confirm transfers between departments

**Steps**:
1. Select department
2. Select Document Type: "Transfer Issue" or "Transfer Receive"
3. Set appropriate date range
4. Choose "By Batch" to see batch details
5. Process report
6. Verify quantities and reference bills

### 4. Department Stock Reconciliation

**Scenario**: Month-end stock audit for a department

**Steps**:
1. Select specific department
2. Leave item blank (to see all items)
3. Choose "By Item" report type
4. Set date range for the month
5. Process report
6. Export to Excel
7. Compare closing stock with physical count

### 5. Batch Movement Tracking

**Scenario**: Track a specific batch through the system

**Steps**:
1. Leave department blank
2. Select specific item
3. Choose "By Batch" report type
4. Set broad date range
5. Process report
6. Filter/search for specific batch in results
7. See all movements of that batch

### 6. Transaction Type Analysis

**Scenario**: Review all GRN transactions for a period

**Steps**:
1. Select department or leave blank
2. Select Document Type: "GRN"
3. Set date range
4. Choose "By Item" report type
5. Process report
6. Review all goods received

## Understanding Transaction Types

### Stock In Transactions (Green Badge)

Transactions that increase stock:
- **GRN** - Goods received from suppliers
- **Transfer Receive** - Stock received from other departments
- **Return Without Receipt** - Returns from patients/departments
- **Stock Adjustments** - Positive adjustments

### Stock Out Transactions (Red Badge)

Transactions that decrease stock:
- **OP Sale** - Sold to outpatients
- **IP Sale** - Issued to inpatients
- **Transfer Issue** - Sent to other departments
- **Consumption** - Used internally
- **Return** - Returned to suppliers
- **Stock Adjustments** - Negative adjustments

## Troubleshooting

### "No records found"

**Possible causes:**
- No transactions in selected date range
- Filters are too restrictive
- Department had no activity in that period
- Item filter doesn't match any items

**Solutions:**
- Expand date range
- Remove some filters
- Check if department/item exists
- Verify filter selections

### Closing Stock Doesn't Match Expectations

**Possible causes:**
- Wrong report type selected (Batch vs Item)
- Looking at wrong scope level (Dept/Ins/Tot)
- Transactions after date range not included
- Filter excluding some transactions

**Solutions:**
- Check the badge on Closing Stock column
- Verify Report Type selection
- Ensure date range includes all relevant transactions
- Review active filters

### Missing Transactions

**Possible causes:**
- Transaction outside date range
- Document Type filter excluding transaction
- Transaction in different department
- Transaction not yet finalized

**Solutions:**
- Expand date range
- Select "All Document Types"
- Check if wrong department selected
- Verify transaction was finalized

### Batch Not Showing in By Batch Report

**Possible causes:**
- No transactions for that batch in date range
- Batch belongs to different item
- Batch created after date range

**Solutions:**
- Expand date range
- Verify correct item selected
- Check batch creation date

## Best Practices

1. **Choose the right report type**
   - Use "By Batch" for expiry tracking and batch-specific issues
   - Use "By Item" for overall stock verification and faster loading

2. **Start with broad filters, then narrow down**
   - Begin with department and date range
   - Add item or document type filters as needed

3. **Export for detailed analysis**
   - Excel export allows sorting and filtering
   - PDF export is better for printing and archiving

4. **Verify scope badges**
   - Always check the badge to confirm stock calculation level
   - Understand the difference between Dept/Ins/Tot levels

5. **Use clickable bill numbers**
   - Click on bill numbers to view full transaction details
   - Use reference bill links to trace transaction chains

6. **Regular reconciliation**
   - Run monthly reports for audit trails
   - Compare closing stock with physical counts

7. **Investigate discrepancies immediately**
   - Use this report to trace stock movements
   - Review transaction sequences for accuracy

## Understanding Report Performance

### By Batch Report
- **Load time**: Slower for large date ranges
- **Data volume**: More rows (one per batch per transaction)
- **Use when**: Need batch-specific details
- **Tip**: Filter by item to improve performance

### By Item Report
- **Load time**: Faster than By Batch
- **Data volume**: Fewer rows (aggregated)
- **Use when**: Need overall item view
- **Tip**: Better for department-wide reviews

## Frequently Asked Questions

### Q: What's the difference between "By Batch" and "By Item" report types?
**A:** "By Batch" shows closing stock for each specific batch. "By Item" shows total closing stock across all batches of that item. Use "By Batch" for expiry tracking, "By Item" for overall stock verification.

### Q: Why does closing stock change based on department selection?
**A:** The closing stock calculation scope changes:
- Department selected = Stock in that department only
- Institution selected = Stock across all departments in that institution
- Neither selected = Total stock across entire system

Check the badge next to "Closing Stock" to see the current scope.

### Q: How do I see stock for a specific batch across all departments?
**A:**
1. Choose "By Batch" report type
2. Leave Department and Institution blank
3. Select the specific item
4. Process the report
5. Search for the batch number in the results

### Q: Can I see all transfers between two departments?
**A:**
1. Select the issuing department
2. Select Document Type: "Transfer Issue"
3. Process the report
4. Look at the "To Store" column to identify receiving department

### Q: What does the reference bill number represent?
**A:** It shows the original transaction being referenced. For example:
- Returns reference the original sale
- Transfer receives reference the transfer issue
- Cancellations reference the original bill

### Q: Why are some closing stock values different from current stock?
**A:** This is a historical report showing stock during the selected date range. Current stock may differ due to subsequent transactions. For current stock, use the Current Stock Report.

### Q: How can I export the report for Excel analysis?
**A:** Click the "Download" button with the Excel icon. The report will export to XLSX format preserving all columns and data.

### Q: What's the difference between "Rate" and "Cost Rate"?
**A:**
- **Rate** = Purchase rate (what you paid the supplier)
- **Cost Rate** = True cost including overheads and adjustments
- Cost rate is typically higher due to additional costs

### Q: Can I see stock values at different rates?
**A:** Yes, the report shows three value types:
- Closing Stock Value at Purchase Rate
- Closing Stock Value at Cost Rate
- Closing Stock Value at Retail Rate

Each shows the stock value using different pricing.

### Q: How do I investigate why stock decreased unexpectedly?
**A:**
1. Select the department and item
2. Choose "By Item" report type
3. Set date range around the decrease
4. Look for STOCK OUT transactions (red badge)
5. Check Document Type and Bill Number for details
6. Click bill number to view full transaction

### Q: What if I need stock history for multiple items?
**A:** Leave the Item filter blank and filter by category or department instead. Export to Excel and use Excel filters to analyze specific items.

## Related Reports

- **Current Stock Report** - For current (live) stock levels
- **Closing Stock Report** - For stock position at a specific date
- **Bin Card Report (Batch)** - Batch-level transaction history
- **Bin Card Report (Item)** - Item-level transaction history
- **Expiry Report** - For upcoming expiries
- **Stock Transfer Report** - Specialized transfer reporting

## Support

If you need help with this report:
- Contact your system administrator
- Refer to the HMIS User Manual
- Report issues at: <https://github.com/hmislk/hmis/issues>
