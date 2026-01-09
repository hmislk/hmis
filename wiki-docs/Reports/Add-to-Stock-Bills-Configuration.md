# Add to Stock Bills Report - Configuration Guide

## Overview

The Add to Stock Bills Report provides a comprehensive view of pharmacy bills that add stock to the system, displaying detailed cost information including cost rates, purchase values, and financial totals. This report is essential for inventory management and financial reconciliation.

**Location**: Reports → Inventory Reports → Add to Stock Bills

## Report Features

### Available Columns
- **Bill Information**: Date & Time, Bill Number, Reference Bill details
- **Item Details**: Name, Code, Batch Number, Quantity
- **Cost Data**:
  - Cost Rate (per unit)
  - Cost Value (quantity × cost rate)
  - Purchase Rate and Purchase Value
  - MRP (Maximum Retail Price) and MRP Value
- **Financial Summary**: Net Total, Payment Methods, Discounts
- **Footer Totals**: Automatic calculation of cost values and net totals

### Filtering Options
- **Date Range**: From Date and To Date filters
- **Institution Filter**: Filter by specific institutions
- **Site Filter**: Filter by institution sites
- **Department Filter**: Conditional department selection based on institution/site

## Page Configuration Management

### Accessing Configuration
1. Navigate to **Reports → Inventory Reports → Add to Stock Bills**
2. Look for the **Config** button in the page header (visible only to administrators)
3. Click **Config** to open the Page Configuration Management interface
4. Modify configuration options as needed
5. Save changes - they take effect immediately

### Available Configuration Options

#### Date Formatting
**Configuration Key**: `Short Date Format`
**Description**: Controls the date formatting pattern used in report generation and PDF exports
**Default**: System default date format
**Effect**: Changes how dates appear in exported reports and PDF documents

### Required Privileges

#### Admin Privilege
**Privilege Name**: `Admin`
**Description**: Administrative access to system configuration and page management
**Required For**:
- Viewing the Config button
- Accessing page configuration interface
- Modifying configuration options

## Data Sources

### Cost Rate Information
The report displays cost rate data from the following sources:
- **Primary Source**: ItemBatch.costRate field
- **Fallback Logic**: If costRate is null, falls back to ItemBatch.purchaseRate
- **Calculation**: Cost Value = Quantity × Cost Rate

### Bill Data
- **Bill Type**: `PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER`
- **Data Flow**: Database → BillItemDTO → Report Display
- **Financial Totals**: Automatically calculated and displayed in footer

## Export Options

### Available Export Formats
1. **Print All**: Browser-based printing of the entire report
2. **Download (Excel)**: Export to Excel format with all data and calculations
3. **PDF Export**: Generate PDF version with formatted layout

### Export Configuration
- Date formatting in exports is controlled by the "Short Date Format" configuration
- All financial totals are included in exported versions
- Pagination settings are preserved in exports

## Troubleshooting

### Cost Rate Columns Not Displaying Data
If Cost Rate and Cost Value columns show zeros or are empty:

1. **Check Data Source**: Verify that ItemBatch records have populated costRate values
2. **Bill Type Verification**: Ensure bills are of the correct type for stock addition
3. **Batch Association**: Confirm pharmaceutical bill items are properly linked to item batches
4. **Configuration Check**: Review any configuration options that might affect display

### Config Button Not Visible
If the Config button doesn't appear:

1. **Privilege Check**: Verify user has 'Admin' privilege assigned
2. **User Role**: Confirm user is logged in with administrative rights
3. **Page Registration**: Ensure page metadata is properly registered in the system

## Administrative Notes

### Configuration Best Practices
- **Date Format Changes**: Test date format changes with small report extracts first
- **Backup Configuration**: Document current configuration values before making changes
- **User Communication**: Inform users when date formats or display options change

### System Impact
- Configuration changes take effect immediately without requiring server restart
- Date format changes affect all future report generations
- No impact on historical data or existing exported reports

## Related Documentation
- [Pharmacy Inventory Management](../Pharmacy/Inventory-Management.md)
- [Report Configuration System](../Admin/Report-Configuration.md)
- [User Privilege Management](../Admin/User-Privileges.md)

## Technical Information

**Report Controller**: `PharmacyReportController`
**Processing Method**: `processAddToStockBills()`
**Data Retrieval**: `retrieveBillWithoutReference()`
**Page Path**: `reports/inventoryReports/add_to_stock_bills`

For technical implementation details, refer to the developer documentation in the repository.