# Ward Pharmacy BHT Substitute Functionality

## Overview

The Ward Pharmacy BHT Issue system allows pharmacy staff to substitute requested medications with available alternatives when the original items are out of stock. This guide explains how to use the substitute functionality effectively.

## When to Use Substitutes

Use the substitute functionality when:
- The requested medication is out of stock
- The requested medication has expired
- An equivalent medication within the same therapeutic category (VMP group) with better pricing is available
- A medication with a longer expiry date is preferred

Note: Substitutions are currently restricted to items with the same unit/pack (e.g., Amp→Amp). Pack-to-pack substitutions (e.g., Amp→Ampp) are not supported.
## How to Substitute Items

### Step 1: Access the Ward Pharmacy BHT Issue Page
1. Navigate to **Ward** → **Ward Pharmacy BHT Issue**
2. Select the patient's BHT request
3. View the list of requested items

### Step 2: Select a Substitute
1. In the **Requested Item** column, find the item you want to substitute
2. Click the **🔄 (refresh/substitute)** button next to the item name
3. A **"Select a Substitute Stock"** dialog will open

### Step 3: Choose from Available Substitutes
The dialog will show available substitute stocks with the following information:
- **Item Name**: Alternative medication name
- **Batch No**: Batch number for tracking
- **Expiry Date**: When the stock expires
- **Available Qty**: How much stock is available
- **Purchase Rate**: Cost price
- **Retail Rate**: Selling price

### Step 4: Replace the Item
1. Review the substitute options
2. Click **"Replace"** button next to your chosen substitute
3. The dialog will close automatically
4. The main screen will update to show:
   - New item name
   - Updated batch number
   - New rates and pricing
   - Recalculated bill totals

## Understanding Messages

### Success Messages
- **"Stock replaced successfully"**: The substitution was completed

### Warning Messages
- **"No substitute stocks available for [Item Name]"**: No alternatives are currently in stock
  - **Action**: Check with pharmacy department or try a different item

### Error Messages
- **"Insufficient stock available. Required: X, Available: Y"**: Not enough stock for the requested quantity
  - **Action**: Select a different substitute or adjust the quantity

- **"Sorry, another user is currently billing this substitute stock"**: Another user is using the same stock
  - **Action**: Wait a moment and try again, or select a different substitute

- **"Cannot use expired stock"**: The selected substitute has expired
  - **Action**: Choose a different substitute with a valid expiry date

## Best Practices

### Before Substituting
1. **Check Expiry Dates**: Always verify the substitute has adequate shelf life
2. **Verify Quantities**: Ensure sufficient stock is available for the full requirement
3. **Review Pricing**: Compare rates to ensure cost-effectiveness
4. **Confirm Equivalency**: Verify the substitute is medically appropriate

### During Substitution
1. **Single Selection**: Choose one substitute at a time
2. **Immediate Action**: Complete the substitution promptly to avoid conflicts with other users
3. **Verify Updates**: Confirm all details update correctly after replacement

### After Substitution
1. **Review Totals**: Check that bill totals have recalculated properly
2. **Document Changes**: Note any significant changes for the requesting department
3. **Continue Processing**: Proceed with issuing the medications to the ward

## Troubleshooting

### No Substitutes Available
If you see "No substitute stocks available":
1. Contact the pharmacy department to check if alternatives can be ordered
2. Coordinate with the requesting ward about possible alternatives
3. Check if the item is available in other pharmacy locations

### Unable to Replace Item
If the Replace button doesn't work:
1. Ensure you have selected a substitute stock
2. Check that the stock hasn't been reserved by another user
3. Verify the substitute has sufficient quantity
4. Try refreshing the page and attempting again

### Updated Information Not Showing
If changes don't appear after replacement:
1. Wait a few seconds for the system to update
2. Check for any error messages
3. Refresh the browser page if necessary
4. Contact IT support if the issue persists

## Configuration Options

Administrators can configure:

### Adding New Items
- **Option**: "Adding new items for inpatient requests are allowed"
- **Purpose**: Controls whether new items can be added to requests
- **Impact**: When disabled, only substitutions are allowed

### Paper Type
- **Option**: "Pharmacy Request Issue Bill is PosHeaderPaper" 
- **Purpose**: Controls the format of printed bills
- **Impact**: Affects how issued medications are documented

### Price Calculations
- Price matrix settings determine how rates are calculated
- Different departments may have different pricing rules
- Contact your system administrator for pricing configuration

## Frequently Asked Questions

### Q: Can I substitute any medication with any other?
A: No, substitutes are limited to medications in the same therapeutic category (VMP group). The system only shows appropriate alternatives.

### Q: What if I make a mistake during substitution?
A: You can repeat the substitution process to select a different alternative. Contact your supervisor if you need to reverse a completed transaction.

### Q: Why can't I see some items as substitutes?
A: Items may not appear if they are:
- Out of stock
- Expired
- Not in the same therapeutic category
- Reserved by another user

### Q: Can multiple users substitute the same item simultaneously?
A: No, the system prevents conflicts. If another user is working with the same stock, you'll receive a message to try again later.

---

## Related Documentation

- [Pharmacy Issue Configuration](../Pharmacy-Issue-Configuration.md)
- [Pharmacy Issue](../Pharmacy-Issue.md) 
- [Ward Pharmacy Management](Ward-Pharmacy-Management.md)

---

**Note**: This functionality requires appropriate user permissions. Contact your system administrator if you cannot access the substitute features.