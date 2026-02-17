# Closing Stock Report

## Overview

The Closing Stock Report shows the stock position of pharmaceutical items at the end of a selected date. This report helps pharmacy staff track inventory levels, calculate stock values for financial reporting, and identify items nearing expiry.

## When to Use This Report

Use the Closing Stock Report when you need to:

- **View historical stock positions** - See what stock you had on any past date
- **Generate financial reports** - Calculate inventory value for accounting purposes
- **Audit stock movements** - Verify stock levels at specific points in time
- **Plan stock ordering** - Review stock trends over time
- **Prepare period-end reports** - Generate month-end or year-end inventory summaries

## Report Types

The Closing Stock Report offers two viewing options:

### Item-Wise Report

Shows aggregated stock for each item (all batches combined).

**Best for:**
- Quick overview of total item quantities
- Financial reporting and valuation
- Stock ordering decisions
- Management reports

**Displays:**
- Item name, code, category
- Total quantity across all batches
- Total values (purchase, sale, cost)
- No batch numbers or expiry dates

### Batch-Wise Report

Shows individual batches with detailed information.

**Best for:**
- Expiry date tracking
- Batch-specific investigations
- Detailed inventory audits
- First-Expiry-First-Out (FEFO) management

**Displays:**
- Item name, code, category
- Batch number
- Expiry date
- Batch quantity
- Batch-specific rates and values

## How to Generate the Report

### Step 1: Access the Report

1. Navigate to **Reports** → **Inventory Reports** → **Closing Stock Report**
2. Select either **Opening Stock** or **Closing Stock** template

### Step 2: Select Date

- **For Closing Stock**: Select the date for which you want to see end-of-day stock
- **For Opening Stock**: Select the date for which you want to see start-of-day stock

**Example**:
- Selecting "January 31, 2025" for Closing Stock shows stock at the end of January 31
- Selecting "February 1, 2025" for Opening Stock shows stock at the start of February 1

### Step 3: Choose Report Type

Select either:
- **Item Wise** - Aggregated view (all batches combined)
- **Batch Wise** - Detailed view (individual batches)

### Step 4: Apply Filters (Optional)

Filter the report by:

#### Institution Filter
- Select a specific institution to see only that institution's stock
- Leave blank to see total stock across all institutions

#### Site Filter
- Select a site to see stock for departments at that site
- Works with institution filter for more specific results

#### Department Filter
- Select a specific department to see only that department's stock
- This is the most common filter for day-to-day stock checking

#### Category Filter
- Filter by item category (e.g., "Antibiotics", "Analgesics")
- Useful for reviewing specific drug groups

#### Item Filter
- Search for a specific item by name
- Type at least 3 characters to see suggestions

#### Consignment Item Checkbox
- Check this to see only consignment items (items with negative stock)
- Leave unchecked for regular stock items

### Step 5: Process the Report

Click the **Process** button to generate the report.

The system will:
1. Find the last stock record before your selected date for each item/batch
2. Calculate quantities and values based on your filters
3. Display results in a table

### Step 6: View Results

The report displays with scope indicators:

- **Dept** badge - Shows stock for the selected department only
- **Ins** badge - Shows aggregated stock for the selected institution
- **Tot** badge - Shows total stock across all institutions

### Step 7: Export Results

Choose from:
- **Print All** - Print the entire report
- **Download (Excel)** - Export to Excel spreadsheet
- **PDF** - Generate PDF document

## Understanding the Report Columns

### Item-Wise Report Columns

| Column | Description |
|--------|-------------|
| S.No | Serial number |
| Item Category | Category the item belongs to |
| Item Code | Unique code for the item |
| Item Name | Name of the pharmaceutical item |
| UOM | Unit of Measurement (e.g., tablets, bottles) |
| Closing Stock | Total quantity available |
| Purchase Value | Total value at purchase rate |
| Cost Value | Total value at cost rate |
| Sale Value | Total value at retail/sale rate |

### Batch-Wise Report Columns

| Column | Description |
|--------|-------------|
| S.No | Serial number |
| Item Category | Category the item belongs to |
| Item Code | Unique code for the item |
| Item Name | Name of the pharmaceutical item |
| UOM | Unit of Measurement |
| Expiry | Batch expiry date |
| Batch No | Batch number |
| Qty | Batch quantity |
| Purchase Rate | Purchase price per unit |
| Purchase Value | Batch quantity × Purchase rate |
| Cost Rate | Cost price per unit |
| Cost Value | Batch quantity × Cost rate |
| Sale Rate | Retail/sale price per unit |
| Sale Value | Batch quantity × Sale rate |

## Understanding Values

The report shows three types of values:

### Purchase Value
- **Calculation**: Quantity × Purchase Rate
- **Use**: Track inventory at purchase cost
- **Best for**: Cost of Goods Sold (COGS) calculations

### Sale Value
- **Calculation**: Quantity × Retail/Sale Rate
- **Use**: Estimate potential revenue from stock
- **Best for**: Financial projections

### Cost Value
- **Calculation**: Quantity × Cost Rate
- **Use**: Track true cost including overheads
- **Best for**: Profitability analysis

## Understanding Scope Levels

The report can show stock at three different scopes:

### Department Level (Dept Badge)
- Shows stock in ONE specific department
- Most detailed level
- Use for: Daily stock checks, department-specific reports

### Institution Level (Ins Badge)
- Shows stock across ALL departments in ONE institution
- Aggregated from all departments
- Use for: Institution-wide inventory valuation

### Total Level (Tot Badge)
- Shows stock across ALL institutions
- System-wide total
- Use for: Corporate-level reporting, overall inventory

## Common Use Cases

### Monthly Stock Valuation

1. Select last day of the month
2. Choose "Item Wise" report
3. Leave all filters blank (to get total)
4. Export to Excel
5. Use purchase values for accounting

### Expiry Tracking

1. Choose current date
2. Select "Batch Wise" report
3. Filter by department if needed
4. Sort by expiry date
5. Identify batches expiring soon

### Department Stock Check

1. Select current date or yesterday
2. Choose "Item Wise" for overview or "Batch Wise" for details
3. Filter by specific department
4. Review stock levels
5. Identify items needing reorder

### Year-End Inventory Report

1. Select December 31st
2. Choose "Item Wise" report
3. Leave filters blank for system-wide total
4. Export to PDF
5. Use for financial statements

## Troubleshooting

### "No records found"

**Possible causes:**
- No stock existed at the selected date
- Filters are too restrictive
- Selected date is before stock recording began

**Solutions:**
- Try a more recent date
- Remove some filters
- Check if department/institution had stock at that time

### Incorrect Quantities Showing

**Possible causes:**
- Looking at wrong scope level (check badge)
- Consignment item filter is checked when it shouldn't be
- Date selected is incorrect

**Solutions:**
- Verify the scope badge matches your expectation
- Uncheck consignment item filter
- Double-check the selected date

### Batch Not Appearing in Batch-Wise Report

**Possible causes:**
- Batch was created AFTER the selected date
- Batch has zero stock
- Filters exclude the batch

**Solutions:**
- Select a later date
- Include zero-stock items if needed
- Review active filters

## Best Practices

1. **Use Item-Wise for speed** - Faster to load than batch-wise for large inventories

2. **Use Batch-Wise for expiry tracking** - Essential for FEFO management

3. **Export regularly** - Keep monthly Excel exports for audit trails

4. **Verify scope badges** - Always check the badge to confirm you're viewing the right scope

5. **Cross-check totals** - Department totals should match institution totals when all departments are included

6. **Historical accuracy** - Remember this shows stock AT the selected date, not current stock

7. **Filter strategically** - Start with broad filters, then narrow down for specific needs

## Frequently Asked Questions

### Q: Why does the total differ from current stock?
**A:** This is a historical report. It shows stock at the END of the selected date, not current stock. For current stock, use the Current Stock Report.

### Q: Can I see stock for multiple departments?
**A:** Yes, select the Institution filter instead of Department. This shows aggregated stock for all departments in that institution.

### Q: What's the difference between Opening Stock and Closing Stock?
**A:** Opening Stock shows stock at the START of the day. Closing Stock shows stock at the END of the day. They differ by the day's transactions.

### Q: Why are some batches missing in the report?
**A:** The report only shows batches that existed at the selected date. Batches created after that date won't appear.

### Q: Can I see stock for a specific item across all departments?
**A:** Yes, use the Item filter to select the specific item and leave the Department filter blank. Select Institution scope to see aggregated stock.

### Q: What does the "Consignment Item" checkbox do?
**A:** It filters for consignment items (items with negative stock). These are items owned by suppliers but held in your pharmacy.

### Q: How often is this report updated?
**A:** The report reflects stock history as it was recorded. It's not "updated" - it shows a snapshot at your selected date.

### Q: Can I export to Excel for further analysis?
**A:** Yes, click the "Download" button to export the current view to Excel format.

## Related Reports

- **Current Stock Report** - For current (live) stock levels
- **Stock Ledger Report** - For detailed transaction history
- **Expiry Report** - For upcoming expiries
- **Reorder Level Report** - For items needing replenishment

## Support

If you need help with this report:
- Contact your system administrator
- Refer to the HMIS User Manual
- Report issues at: <https://github.com/hmislk/hmis/issues>
