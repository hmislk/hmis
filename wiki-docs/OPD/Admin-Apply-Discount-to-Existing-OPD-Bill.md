# Admin: Apply Discount to Existing OPD Bill

## Overview

This feature allows system administrators to apply or change discount schemes on OPD bills that have already been created and settled. This is typically used when a discount scheme was incorrectly applied or not applied at the time of billing, and there is a formal written request from hospital management to make the correction.

## Important Notice

**⚠️ WARNING**: This feature modifies financial data that has already been recorded in the system. This action is **IRREVERSIBLE** and should only be used with proper written authorization from hospital management.

## Who Can Use This Feature

- **System Administrators** with proper privileges
- **Finance Administrators** with written authorization

## When to Use This Feature

This feature should be used only in the following situations:

1. A patient was eligible for a staff discount, but it was not applied at the time of billing
2. Wrong discount scheme was applied during billing
3. A patient's membership status changed retroactively with proper documentation
4. Written request from authorized hospital management to correct discount application

## Prerequisites

Before using this feature, you must have:

1. ✓ Written authorization from hospital management
2. ✓ Request reference number and date
3. ✓ Clear reason for the discount change
4. ✓ Approval from the Finance Department
5. ✓ The bill must be of type "OPD Bill with Payment"
6. ✓ The bill must not be cancelled or refunded

## Step-by-Step Guide

### Step 1: Navigate to Bill Administration

1. Go to **OPD** → **View/Search** → **Bill Search**
2. Search for and select the bill that needs discount adjustment
3. Click the **Admin** button for the selected bill
4. This will open the **Bill Administration** page

### Step 2: Access the Apply Discount Feature

1. In the **Bill Administration** page, go to the **Summary** tab
2. You will see an **Apply Discount** button (visible only for OPD Bills with Payment)
3. Click the **Apply Discount** button

### Step 3: Review Current Bill Information

The system will display:

- **Bill Number** and **Bill Date**
- **Patient Name**
- **Current Payment Scheme** (if any)
- **Current Bill Values**:
  - Total
  - Discount
  - Net Total
- **Current Batch Bill Values** (if applicable):
  - Total
  - Discount
  - Net Total

### Step 4: Select New Discount Scheme

1. From the **Select Discount Scheme** dropdown, choose the appropriate discount scheme
2. Click the **Calculate Preview** button

### Step 5: Review Preview

After clicking Calculate Preview, the system will show:

- **New Bill Values** after applying the discount:
  - Total
  - Discount
  - Net Total
  - **Difference** from current values

- **New Batch Bill Values** (if applicable):
  - Total
  - Discount
  - Net Total
  - **Difference** from current values

**Important**: Carefully review these values to ensure they are correct before proceeding.

### Step 6: Provide Authorization Details

In the **Authorization Details** section:

1. Enter the mandatory comment with the following information:
   - Request reference number
   - Date of request
   - Name and designation of the authorizing person
   - Reason for the discount change

   **Example**:
   ```
   Request No: REQ-2024-001
   Date: 05-Nov-2024
   Authorized by: Hospital Director Dr. John Smith
   Reason: Patient is a staff member and was eligible for staff discount
   (50% discount scheme) but it was not applied at the time of billing
   due to system error. Patient has provided staff ID card as proof.
   Finance Department approval obtained on 04-Nov-2024.
   ```

### Step 7: Apply the Discount

1. Click the **Apply Discount Scheme** button
2. A confirmation dialog will appear asking you to confirm
3. Click **OK** to confirm and apply the discount
4. The system will:
   - Update all bill fees with the new discount scheme
   - Recalculate bill item totals
   - Update bill totals
   - Update batch bill totals (if applicable)
   - Create an audit log entry
   - Add the comment to the bill

### Step 8: Verify the Changes

After successful application:

1. You will be redirected back to the **Bill Administration** page
2. Verify that the new totals are displayed correctly
3. The bill's comments section will include your authorization details

## Financial Impact

When you apply a discount scheme to an existing bill, the following values will change:

### At Bill Level:
- Total (Gross Value)
- Discount Amount
- Net Total

### At Bill Item Level:
- Gross Value
- Discount
- Net Value

### At Batch Bill Level:
- Total (Gross Value)
- Discount Amount
- Net Total

## Audit Trail

All discount changes are automatically logged in the system audit trail. To view the audit logs:

1. Go to **Data Admin** → **Audit** → **Audit Event History**
2. Select the date range when the discount was applied
3. Click **Search**
4. Look for event: **"Admin Retroactive Discount Application"**
5. The audit log will show:
   - User who made the change
   - Date and time
   - Bill number
   - Original values
   - New values
   - Authorization comment

## Important Warnings and Limitations

### ⚠️ Critical Warnings:

1. **IRREVERSIBLE ACTION**: Once applied, the discount cannot be automatically reversed. You would need to manually create correcting entries.

2. **FINANCIAL IMPACT**: This changes recorded financial data that may already be included in financial reports.

3. **BATCH BILL IMPACT**: Changes to individual OPD bills will also affect the associated batch bill totals.

4. **NO AUTOMATIC REFUNDS**: If the new discount results in a lower net total, this feature does NOT automatically process refunds. Refunds must be handled separately through the standard refund process.

5. **NO AUTOMATIC COLLECTION**: If the new discount results in a higher net total, this feature does NOT automatically collect the additional amount. Additional collection must be handled separately.

### Limitations:

- Only works with bills of type "OPD Bill with Payment"
- Cannot be used on cancelled bills
- Cannot be used on refunded bills
- Requires mandatory authorization comment
- Must calculate preview before applying

## Troubleshooting

### Issue: Apply Discount button is not visible

**Solution**:
- Check that the bill type is "OPD Bill with Payment"
- Check that you have administrator privileges
- Ensure the bill is not cancelled or refunded

### Issue: Cannot apply discount - "Please calculate preview first"

**Solution**:
- Select a discount scheme
- Click the "Calculate Preview" button
- Review the preview values
- Only then click "Apply Discount Scheme"

### Issue: Error when applying discount

**Solution**:
- Ensure authorization comment is filled
- Check that all required fields have valid data
- Contact system administrator if the problem persists

## Best Practices

1. **Always obtain written authorization** before making any discount changes
2. **Keep copies of authorization documents** linked to the request number in your comment
3. **Review the preview carefully** before applying the discount
4. **Document the reason clearly** in the authorization comment
5. **Inform the Finance Department** after making discount changes
6. **Inform the patient** if the change affects their payment

## Related Topics

- [OPD Billing Process](OPD-Billing-Process.md)
- [Discount Schemes Management](Discount-Schemes-Management.md)
- [Audit Trail Review](../Admin/Audit-Trail-Review.md)
- [Bill Cancellation and Refund](Bill-Cancellation-and-Refund.md)

## Support

If you have questions or issues with this feature:

1. Contact your System Administrator
2. Contact the HMIS Support Team
3. Check the [HMIS User Guide](../index.md)

---

**Document Version**: 1.0
**Last Updated**: November 2024
**Feature**: Admin Apply Discount to Existing OPD Bill
