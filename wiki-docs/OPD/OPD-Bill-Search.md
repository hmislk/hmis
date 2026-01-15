# OPD Bill Search

## Overview

The OPD Bill Search feature allows cashiers to find and view existing outpatient department bills for billing inquiries, cancellations, and patient service history. This search function displays both individual bills and batch bills, providing access to detailed billing information and administrative actions.

## When to Use

Use the OPD Bill Search when you need to:

- Look up a patient's previous OPD visits and billing history
- Find a specific bill for cancellation or refund
- View payment details for billing inquiries
- Access batch bills for multiple service cancellations
- Review billing information for patient service disputes
- Check payment status and methods used

## How to Use

### Accessing OPD Bill Search

1. Navigate to **Main Menu** → **OPD Sub Menu** → **Search** → **OPD Bill Search**
2. The bill search page will open with filter options

### Searching for Bills

#### Basic Search Options

1. **Patient Search**: Enter patient name, BHT number, or phone number
2. **Bill Number**: Enter specific bill number if known
3. **Date Range**: Select from and to dates to narrow search results
4. **Department**: Choose specific OPD departments if needed

#### Advanced Search

1. **Payment Status**: Filter by paid, unpaid, or partially paid bills
2. **Payment Method**: Filter by cash, card, multiple payment methods
3. **Doctor**: Select specific doctors to see their patient bills
4. **Bill Type**: Choose individual bills, batch bills, or both

### Understanding Search Results

The search results display two main sections:

#### Individual Bills List
- **Bill Number**: Unique identifier for each bill
- **Date & Time**: When the bill was created
- **Patient Name**: Patient details and BHT number
- **Services**: List of services provided
- **Amount**: Total bill amount and payment status
- **Action Buttons**:
  - **View**: Opens detailed bill information
  - **Print**: Generates bill receipt
  - **Cancel**: Initiates bill cancellation (if authorized)

#### Batch Bills List
- **Batch Bill Number**: Unique identifier for the batch
- **Date**: When the batch was created
- **Patient Name**: Main patient for the batch
- **Total Amount**: Combined amount for all services in batch
- **Items Count**: Number of individual bills in the batch
- **Action Buttons**:
  - **View Batch**: Opens batch bill details
  - **Cancel Batch**: Initiates batch cancellation (if authorized)

## Viewing Bill Details

### Individual Bill View

1. Click the **View** button next to any individual bill
2. The bill details page will show:
   - **Patient Information**: Name, BHT number, contact details
   - **Services Provided**: Detailed list of consultations, procedures, tests
   - **Payment Details**: Payment method, amount, reference numbers
   - **Doctor Information**: Consulting doctors and departments
   - **Billing History**: Previous bills and cancellations

### Batch Bill View

1. Click the **View Batch** button next to any batch bill
2. The batch details page will show:
   - **Batch Summary**: Total amount, payment status, creation date
   - **Individual Bills**: All bills included in this batch
   - **Payment Summary**: Combined payment methods and amounts
   - **Service Summary**: All services across the batch

## Accessing Cancellation Features

### Individual Bill Cancellation

1. Find the bill in search results
2. Click the **Cancel** button (if visible)
3. This opens the [Individual Bill Cancellation](Individual-Bill-Cancellation.md) process

> **Note**: If the Cancel button is not visible, contact your administrator to request cancellation privileges.

### Batch Bill Cancellation

1. Find the batch bill in search results
2. Click the **Cancel Batch** button (if visible)
3. This opens the [Batch Bill Cancellation](Batch-Bill-Cancellation.md) process

> **Note**: If the Cancel Batch button is not visible, contact your administrator to request batch cancellation privileges.

## Understanding Messages

### Success Messages
- **"Search completed successfully"**: Search results are displayed below
- **"Bill found"**: Specific bill has been located

### Information Messages
- **"No bills found for the selected criteria"**: Try expanding your search parameters or check if the patient has bills in the selected date range
- **"Multiple bills found for this patient"**: Review the list to select the correct bill

### Warning Messages
- **"Bill is already cancelled"**: This bill has been previously cancelled and cannot be cancelled again
- **"Bill payment is pending"**: Payment collection is required before cancellation

### Error Messages
- **"Please enter search criteria"**: At least one search field must be filled
- **"Invalid date range"**: The 'From' date cannot be after the 'To' date
- **"Access denied"**: Contact your administrator for search permissions

## Best Practices

### Efficient Searching
- Use **specific date ranges** to reduce search results and improve performance
- Search by **BHT number** when available for fastest results
- Use **bill number** for direct access to specific bills
- Combine **patient name and date range** for comprehensive patient history

### Bill Review Before Action
- **Always verify patient details** before proceeding with cancellations
- **Check payment status** to understand refund implications
- **Review service details** to ensure correct bill selection
- **Note payment methods** especially for multiple payment scenarios

### Record Keeping
- **Print bills** before cancellation for your records
- **Note the reason** for any cancellation requests
- **Verify patient authorization** for cancellation requests
- **Document any special circumstances** for audit purposes

## Troubleshooting

### Problem: No search results appear
**Symptoms**: Search returns empty results despite knowing bills exist
**Cause**: Search criteria may be too restrictive or dates incorrect
**Solution**:
1. Expand the date range to cover a longer period
2. Try searching with just the patient name without other filters
3. Check if the patient name is spelled correctly
4. Verify the BHT number is accurate

### Problem: Too many search results
**Symptoms**: Search returns hundreds of bills making it hard to find the right one
**Cause**: Search criteria is too broad
**Solution**:
1. Add a specific date range for the service period
2. Include department or doctor filters
3. Use more specific patient information like BHT number
4. Filter by payment status if relevant

### Problem: Cannot see cancellation buttons
**Symptoms**: Cancel buttons are missing from search results
**Cause**: User account lacks cancellation privileges
**Solution**: Contact your system administrator to request bill cancellation permissions

### Problem: Bill details not loading
**Symptoms**: Clicking View button shows error or empty page
**Cause**: System connectivity or temporary loading issue
**Solution**:
1. Refresh the page and try again
2. Go back to search and re-run the search
3. Contact system administrator if problem persists

## Configuration (Admin)

### User Permissions
Administrators can configure the following settings that affect bill search:
- **Search Access**: Who can search for bills
- **Cancellation Privileges**: Who can see and use cancellation buttons
- **Department Restrictions**: Limiting search to specific OPD departments
- **Date Range Limits**: Maximum date range allowed for searches

### Search Performance
- **Result Limits**: Maximum number of bills shown in search results
- **Auto-complete Settings**: Patient name and BHT number suggestions
- **Cache Settings**: How long search results are cached for repeat searches

## FAQ

**Q: Can I search for bills from other departments like Pharmacy or Lab?**
A: No, this search is specifically for OPD bills only. Use the respective department search features for other bill types.

**Q: Why don't I see some patients' bills even though I know they visited?**
A: You might not have permission to view bills from certain departments, or the bills might be from a different billing category. Contact your administrator to verify your access permissions.

**Q: Can I search for cancelled bills?**
A: Yes, cancelled bills appear in search results but are clearly marked as "CANCELLED" with the cancellation date and reason.

**Q: What's the difference between individual bills and batch bills?**
A: Individual bills are for single services or consultations. Batch bills combine multiple services for the same patient visit into one consolidated bill for easier payment processing.

**Q: How far back can I search for bills?**
A: This depends on your system configuration. Generally, you can search for bills from the past several years, but very old bills might be archived. Contact your administrator for specific date limits.

**Q: Can I export search results to Excel?**
A: Export functionality depends on your user permissions and system configuration. Look for an "Export" button in the search results area, or contact your administrator about export options.

## Related Features
- [Individual Bill Cancellation](Individual-Bill-Cancellation.md)
- [Batch Bill Cancellation](Batch-Bill-Cancellation.md)
- [OPD Billing Process](OPD-Billing-Process.md)
- [Payment Methods and Processing](../Cashier/Payment-Methods-Processing.md)

## Support

If you have questions or issues with bill search:

1. Contact your immediate supervisor or senior cashier
2. Contact your System Administrator
3. Contact the HMIS Support Team
4. Check the [HMIS User Guide](../index.md)

---

**Document Version**: 1.0
**Last Updated**: January 2025
**Feature**: OPD Bill Search