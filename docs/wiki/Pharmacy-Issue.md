# Pharmacy Issue (Disposal)

The Pharmacy Issue module allows pharmacists to dispense medications and items to patients with configurable rate displays and comprehensive financial tracking.

## Overview

Pharmacy Issue is a core dispensing functionality that enables:
- Item dispensing with configurable rate visibility
- Real-time financial calculations
- Bill-level value summaries
- Configuration-based display options

## Key Features

### 1. **Configurable Rate Display**
Administrators can control which rates are displayed during dispensing:

- **Purchase Rate**: Shows item purchase cost
- **Cost Rate**: Shows calculated cost rate including expenses
- **Retail Rate**: Shows standard retail selling price
- **Issue Rate**: Shows actual dispensing rate (always visible)

### 2. **Value Calculations**
The system displays both item-level and bill-level values:

**Item-Level Values:**
- Purchase Value: Quantity × Purchase Rate  
- Cost Value: Quantity × Cost Rate
- Retail Value: Quantity × Retail Rate
- Transfer Value: Quantity × Issue Rate (net dispensing value)

**Bill-Level Totals:**
- Total Purchase Value: Sum of all item purchase values
- Total Cost Value: Sum of all item cost values  
- Total Retail Value: Sum of all item retail values
- Total Transfer Value: Net dispensing amount

### 3. **Real-Time Updates**
All financial values automatically update when:
- Items are added to the bill
- Quantities are changed
- Items are selected from stock

## How to Use Pharmacy Issue

### Basic Dispensing Process

1. **Select Department/Location**
   - Choose the dispensing department from the dropdown

2. **Add Items to Bill**
   - Search and select items using the autocomplete field
   - Enter the dispensing quantity
   - Review displayed rates and values (based on configuration)
   - Click "Add" to add the item to the bill

3. **Review Bill Details**
   - View item-wise breakdown in the bill items table
   - Check bill-level totals in the Bill Details panel
   - Verify all calculations are correct

4. **Complete Transaction**
   - Process payment if required
   - Print dispensing documents
   - Complete the transaction

### Item Selection Features

- **Auto-complete Search**: Type item name, code, or barcode to search
- **Stock Availability**: View current stock levels for each batch
- **Expiry Warnings**: Color-coded warnings for expiring items
  - Red: Expired items
  - Yellow: Items expiring within 3 months
- **Batch Selection**: Choose from available batches with different rates

### Configuration Options

The display of rates and values can be controlled through configuration settings. See [Pharmacy Issue Configuration](https://github.com/hmislk/hmis/wiki/Pharmacy-Issue-Configuration) for detailed setup instructions.

## Access Requirements

**User Privileges Required:**
- Pharmacy Issue privileges
- Access to specific pharmacy departments
- Item dispensing permissions

**Typical User Roles:**
- Pharmacists
- Pharmacy Technicians
- Authorized dispensing staff

## Integration Points

### With Other Modules
- **Inward Management**: Dispensing to admitted patients
- **OPD Services**: Outpatient medication dispensing
- **Stock Management**: Real-time stock deduction
- **Financial Management**: Revenue and cost tracking

### External Systems
- **Barcode Scanners**: For quick item selection
- **Receipt Printers**: For dispensing receipts
- **Label Printers**: For medication labels

## Reports and Analytics

**Available Reports:**
- Dispensing summaries by date/period
- Item-wise dispensing reports
- Financial summaries with multiple rate views
- Stock movement reports
- User activity reports

## Best Practices

### For Pharmacists
1. **Verify Patient Information**: Ensure correct patient before dispensing
2. **Check Expiry Dates**: Always use FEFO (First Expired, First Out) principle
3. **Quantity Verification**: Double-check dispensing quantities
4. **Rate Review**: Verify appropriate rates are being charged

### For Administrators  
1. **Configure Display Options**: Set appropriate rate visibility for users
2. **Monitor Financial Reports**: Regular review of dispensing patterns
3. **Stock Level Monitoring**: Maintain adequate stock levels
4. **User Access Control**: Ensure proper privilege assignment

## Troubleshooting

### Common Issues
1. **Items Not Found**: Check item master data and ensure items are marked as active
2. **Rate Calculation Issues**: Verify configuration settings and item pricing
3. **Stock Availability**: Ensure items have positive stock in the selected store
4. **Permission Errors**: Check user privileges and department access rights

### Performance Tips
1. **Regular Data Cleanup**: Archive old transaction data
2. **Index Maintenance**: Ensure database indexes are optimized
3. **Cache Management**: Clear application cache if performance degrades

---

**Related Topics:**
- [Pharmacy Issue Configuration](https://github.com/hmislk/hmis/wiki/Pharmacy-Issue-Configuration)
- [Pharmacy Administration](https://github.com/hmislk/hmis/wiki/Pharmacy-Administration)
- [Stock Management](https://github.com/hmislk/hmis/wiki/Add-To-Stock)

[Back to Pharmacy](https://github.com/hmislk/hmis/wiki/Pharmacy)