# Pharmacy Issue (Disposal)

The Pharmacy Issue module allows pharmacists to dispense medications and items for disposal or transfer between departments with configurable rate displays and comprehensive financial tracking.

## Overview

Pharmacy Issue is a core disposal functionality that enables:
- Item disposal issue to other departments
- Configurable rate visibility and value calculations
- Real-time financial tracking across multiple rate types
- Bill-level value summaries with configurable display options
- Inter-department transfer tracking with detailed audit trail

## Key Features

### 1. **Configurable Rate Display**
Administrators can control which rates are displayed during disposal issue operations:

- **Purchase Rate**: Shows original item purchase cost for reference
- **Cost Rate**: Shows calculated cost rate including expenses and overheads
- **Retail Rate**: Shows standard retail selling price for value comparison
- **Issue Rate**: Shows actual disposal issue rate used for transfer value calculation (always visible)

### 2. **Value Calculations**
The system calculates and displays multiple value types at both item and bill levels:

**Item-Level Values:**
- **Purchase Value**: Quantity × Purchase Rate (original procurement cost)
- **Cost Value**: Quantity × Cost Rate (cost including overheads)  
- **Retail Value**: Quantity × Retail Rate (market value reference)
- **Issue Value**: Quantity × Issue Rate (actual transfer value used in disposal)

**Bill-Level Totals:**
- **Total Purchase Value**: Sum of all item purchase values for cost accounting
- **Total Cost Value**: Sum of all item cost values for internal costing
- **Total Retail Value**: Sum of all item retail values for value assessment
- **Total Issue Value**: Net disposal value transferred to receiving department

### 3. **Real-Time Updates**
All financial values automatically update when:
- Items are added to the disposal issue bill
- Quantities are modified
- Items are selected from stock with different batch rates

### 4. **Department Transfer Management**
The system tracks transfers between departments with:
- **From Department**: Automatically set to logged user's department
- **To Department**: Selectable destination department for disposal items
- **Request Number**: Reference tracking for internal requests
- **Comments**: Detailed notes about the disposal/transfer reason

## How to Use Pharmacy Disposal Issue

### Basic Disposal Issue Process

1. **Select Receiving Department**
   - Choose the destination department for disposal items
   - Enter request number if applicable
   - Add comments explaining the disposal reason

2. **Add Items to Disposal Bill**
   - Search and select items using the autocomplete field
   - Enter the disposal quantity
   - Review displayed rates and values (based on configuration settings)
   - Click "Add" to add the item to the disposal bill

3. **Review Disposal Bill Details**
   - View item-wise breakdown in the bill items table
   - Check bill-level totals across all configured value types
   - Verify all calculations and transfer values are correct

4. **Complete Disposal Transaction**
   - Review all details for accuracy
   - Click "Settle Issue" to complete the disposal
   - Print disposal receipt for record-keeping

### Item Selection Features

- **Auto-complete Search**: Type item name, code, or barcode to search
- **Stock Availability**: View current stock levels for each batch
- **Expiry Warnings**: Color-coded warnings for expiring items
  - Red: Expired items
  - Yellow: Items expiring within 3 months
- **Batch Selection**: Choose from available batches with different rates

### Configuration Options

The display of rates and values can be controlled through configuration settings. See [Pharmacy Issue Configuration](https://github.com/hmislk/hmis/wiki/Pharmacy-Issue-Configuration) for detailed setup instructions.

## Technical Implementation

### Controller: PharmacyIssueController

The disposal issue functionality is implemented in `PharmacyIssueController.java` with key methods:

**Core Methods:**
- `settleDisposalIssueBill()`: Main method to complete disposal transactions
- `addBillItem()`: Adds items to disposal bill with validation
- `calTotal()`: Recalculates all financial values using PharmacyCostingService
- `updateFinancialsForIssue()`: Updates item-level financial calculations

**Rate Determination Logic:**
```java
private BigDecimal determineIssueRate(ItemBatch itemBatch) {
    boolean issueByPurchase = configOptionApplicationController.getBooleanValueByKey("Pharmacy Issue is by Purchase Rate", true);
    boolean issueByCost = configOptionApplicationController.getBooleanValueByKey("Pharmacy Issue is by Cost Rate", false);
    boolean issueByRetail = configOptionApplicationController.getBooleanValueByKey("Pharmacy Issue is by Retail Rate", false);
    
    if (issueByPurchase) return BigDecimal.valueOf(itemBatch.getPurcahseRate());
    else if (issueByCost) return BigDecimal.valueOf(itemBatch.getCostRate());
    else if (issueByRetail) return BigDecimal.valueOf(itemBatch.getRetailsaleRate());
    else return BigDecimal.valueOf(itemBatch.getPurcahseRate()); // fallback
}
```

### JSF Page: pharmacy_issue.xhtml

Located at `src/main/webapp/pharmacy/pharmacy_issue.xhtml`, this page implements:

**Key UI Components:**
- Item selection autocomplete with multi-column rate display
- Real-time rate and value calculation inputs
- Bill items table with configurable column visibility
- Bill details panel with multiple total types
- Receipt component for printing disposal documentation

**Conditional Display Logic:**
All rate and value displays are controlled by configuration keys:
```xhtml
rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Dispotals - Display Purchase Rate',true)}"
```

### Receipt Component: pharmacy_issue_receipt.xhtml

Located at `src/main/webapp/resources/pharmacy/pharmacy_issue_receipt.xhtml`, this composite component provides:

**Features:**
- Professional disposal issue receipt formatting
- Configurable rate and value column visibility
- Department transfer information display
- Audit trail with created by/at information
- CSS customization through configuration options

**Key Display Elements:**
- Bill metadata (date, receipt number, request number)
- Department transfer details (from/to departments)
- Item details with configurable rate columns
- Multiple total types based on configuration
- Staff signatures section for approval workflow

### PharmacyCostingService Integration

The `calculateBillTotalsFromItemsForDisposalIssue()` method handles:
- Multiple rate type calculations (purchase, cost, retail, issue)
- Bill-level total aggregation across all value types
- Financial detail population for reporting and audit
- Proper negative quantity handling for disposal transactions

## Access Requirements

**User Privileges Required:**
- Pharmacy Disposal Issue privileges
- Access to specific pharmacy departments
- Item disposal/transfer permissions

**Typical User Roles:**
- Pharmacists
- Pharmacy Technicians  
- Department Store Managers
- Authorized disposal staff

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