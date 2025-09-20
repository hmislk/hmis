# Pharmacy Issue (Disposal)

The Pharmacy Issue module allows pharmacists to dispose of medications and items internally within the hospital with configurable rate displays and comprehensive financial tracking.

## Overview

Pharmacy Issue is a core disposal functionality that enables:
- Item disposal with configurable rate visibility
- Real-time financial calculations
- Bill-level value summaries
- Configuration-based display options

## Key Features

### 1. **Configurable Rate Display**
Administrators can control which rates are displayed during disposal:

- **Purchase Rate**: Shows item purchase cost
- **Cost Rate**: Shows calculated cost rate including expenses
- **Retail Rate**: Shows standard retail selling price
- **Issue Rate**: Shows actual disposal rate (always visible)

### 2. **Value Calculations**
The system displays both item-level and bill-level values:

**Item-Level Values:**
- Purchase Value: Quantity × Purchase Rate  
- Cost Value: Quantity × Cost Rate
- Retail Value: Quantity × Retail Rate
- Transfer Value: Quantity × Issue Rate (net disposal value)

**Bill-Level Totals:**
- Total Purchase Value: Sum of all item purchase values
- Total Cost Value: Sum of all item cost values  
- Total Retail Value: Sum of all item retail values
- Total Transfer Value: Net disposal amount

### 3. **Real-Time Updates**
All financial values automatically update when:
- Items are added to the bill
- Quantities are changed
- Items are selected from stock

## Navigation and Usage

### How to Navigate to Pharmacy Issue

**Path:** Pharmacy → Disposal → Issue

### Basic Disposal Process

1. **Select Department/Location**
   - Choose the issuing department from the dropdown

2. **Add Items to Disposal**
   - Search and select items using the autocomplete field
   - Enter the disposal quantity
   - Review displayed rates and values (based on configuration)
   - Click "Add" to add the item to the disposal

3. **Review Disposal Details**
   - View item-wise breakdown in the disposal items table
   - Check disposal-level totals in the Bill Details panel
   - Verify all calculations are correct

4. **Complete Disposal**
   - Enter request number and comments
   - Print disposal documents
   - Click "Issue" to complete the disposal

### Item Selection Features

- **Auto-complete Search**: Type item name, code, or barcode to search
- **Stock Availability**: View current stock levels for each batch
- **Expiry Warnings**: Color-coded warnings for expiring items
  - Red: Expired items
  - Yellow: Items expiring within 3 months
- **Batch Selection**: Choose from available batches with different rates

## Access Requirements

**User Privileges Required:**
- Pharmacy Issue privileges
- Access to specific pharmacy departments
- Item dispensing permissions

**Typical User Roles:**
- Pharmacists
- Pharmacy Technicians
- Authorized disposal staff


## Reports and Analytics

**Available Reports:**
- Disposal summaries by date/period
- Item-wise disposal reports
- Financial summaries with multiple rate views
- Stock movement reports

## Best Practices

### For Pharmacists
1. **Check Expiry Dates**: Always use FEFO (First Expired, First Out) principle
2. **Quantity Verification**: Double-check disposal quantities
3. **Rate Review**: Verify appropriate rates are recorded
4. **Documentation**: Enter proper request numbers and comments for disposal tracking

## Troubleshooting

### Common Issues
1. **Items Not Found**: Check item master data and ensure items are marked as active
2. **Rate Calculation Issues**: Verify configuration settings and item pricing
3. **Stock Availability**: Ensure items have positive stock in the selected store
4. **Permission Errors**: Check user privileges and department access rights


---

**Related Topics:**
- [Pharmacy Administration](https://github.com/hmislk/hmis/wiki/Pharmacy-Administration)
- [Managing Expiring Items](https://github.com/hmislk/hmis/wiki/Managing-Expiring-Items-in-Pharmacy-Inventory)
- [Discard Categories](https://github.com/hmislk/hmis/wiki/Discard-Categories)

[Back to Pharmacy](https://github.com/hmislk/hmis/wiki/Pharmacy)