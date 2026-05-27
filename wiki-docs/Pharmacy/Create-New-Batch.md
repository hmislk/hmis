# Create New Batch

This guide explains how to manually create a new item batch in the pharmacy module.

## Required Privilege

Users must have the **Pharmacy Adjustment Create Batch** privilege to access this feature.

To grant this privilege:
1. Navigate to **Administration** > **Users** > **User Privileges**
2. Select the user or department
3. Expand **Pharmacy** > **Pharmacy Adjustment**
4. Check **Pharmacy Adjustment Create Batch**
5. Click **Save**

## Navigation

**Pharmacy** > **Adjustments** > **Create New Batch**

Or directly: `/pharmacy/adjustments/pharmacy_adjustment_batch_create.xhtml`

## Steps to Create a New Batch

1. Click the **Create New Batch** button from the Pharmacy Adjustments page
2. Fill in the required fields:
   - **Select Item** - Search and select the item (AMP or AMPP)
   - **Batch Number** - Enter the batch number from the product label
   - **Date of Expiry** - Select the expiry date
   - **Department** - Defaults to your logged-in department; can be changed
   - **Purchase Rate** - Auto-populated from last purchase; can be modified
   - **Retail Rate** - Auto-populated from last purchase; can be modified
3. Optionally fill in:
   - **Cost Rate**
   - **Wholesale Rate**
4. Click **Save** to create the batch and return to the adjustments page
   - Or click **Save and Add Another** to create the batch and continue adding more

## Notes

- The batch is created with zero stock quantity
- To add stock quantity, use the **Quantity Adjustment** feature
- Duplicate batches (same item, batch number, and expiry date) are not allowed
