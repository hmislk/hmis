# Batch Bill Cancellation

## Overview

Batch bill cancellation allows cashiers to cancel entire batch bills that contain multiple individual OPD services for a single patient visit. This process handles complex payment scenarios where patients paid for multiple services using various payment methods, and cancels all services together while processing appropriate refunds.

## When to Use

Use batch bill cancellation when you need to:

- Cancel an entire patient visit with multiple services (consultation, procedures, tests)
- Process refunds for cancelled comprehensive treatment plans
- Handle patient emergencies requiring full visit cancellation
- Correct billing errors affecting multiple services in one visit
- Cancel duplicate batch bills created by mistake
- Process refunds when patients cannot receive any of the scheduled services

## Prerequisites

Before cancelling a batch bill, ensure:

1. ✓ You have batch cancellation privileges (Cancel Batch button is visible)
2. ✓ Patient has provided valid reason for cancelling all services
3. ✓ Batch bill is not already cancelled or partially refunded
4. ✓ You have proper authorization as required by hospital policy
5. ✓ All individual services in the batch can be legitimately cancelled
6. ✓ Patient understands that all services in the batch will be cancelled

## How to Use

### Step 1: Find the Batch Bill to Cancel

1. Navigate to **Main Menu** → **OPD Sub Menu** → **Search** → **OPD Bill Search**
2. Search for the patient's batch bill using:
   - Patient name, BHT number, or phone number
   - Batch bill number if available
   - Date range when services were provided
3. Look in the **Batch Bills List** section (not individual bills)
4. Locate the specific batch bill in the search results
5. Verify the batch bill details match the patient's request

### Step 2: Access Batch Bill Cancellation

1. Click the **Cancel Batch** button next to the batch bill in search results
2. The **Batch Bill Cancellation** page will open
3. Review the comprehensive information displayed:
   - **Patient Details**: Name, BHT number, contact information
   - **Batch Summary**: Total amount, number of individual bills, creation date
   - **Service Details**: Complete list of all services across the batch
   - **Payment Information**: All payment methods used for this batch

### Step 3: Review Payment Details

The system will display all payment information used for the batch bill. Batch bills often use **multiple payment methods**, so you'll see detailed breakdown:

#### Multiple Payment Methods Display
The payment details are shown in **view-only mode** to ensure accuracy:

**Cash Payments**:
- **Amount**: Cash portion of the total payment
- Displayed as read-only to prevent accidental changes

**Card Payments**:
- **Bank**: Name of the issuing bank (automatically populated)
- **Amount**: Card transaction amount
- **Reference Number**: Card transaction reference
- All details shown as view-only for data integrity

**Cheque Payments**:
- **Bank**: Issuing bank name
- **Cheque Number**: Cheque reference number
- **Date**: Cheque date
- **Amount**: Cheque amount
- All displayed as read-only

**Other Payment Methods**:
- **eWallet**: Digital wallet transaction details
- **Staff Credit**: Staff account payment details
- **Patient Deposit**: Patient advance payment amounts
- **Online Settlement**: Online payment transaction details

> **Important**: All payment details are displayed in **view-only mode** during batch cancellation. This ensures the original payment information is preserved and prevents accidental modification during the cancellation process.

### Step 4: Review Individual Bills in Batch

The cancellation page shows all individual bills included in the batch:

1. **Service List**: Each individual service with details:
   - Service name and department
   - Doctor assigned
   - Individual bill amount
   - Service date and time

2. **Payment Allocation**: How the batch payment was distributed across services

3. **Cancellation Impact**: Total refund amount and method breakdown

### Step 5: Enter Cancellation Details

1. **Cancellation Reason**: Select from dropdown options:
   - Patient Emergency
   - Complete Visit Cancellation
   - Medical Emergency
   - Patient Unable to Continue Treatment
   - Billing Error - Multiple Services
   - Duplicate Batch Bill
   - Patient Request - Full Cancellation
   - Other (requires detailed explanation)

2. **Comments**: Enter comprehensive explanation including:
   - Specific reason for batch cancellation
   - Patient request authorization or reference
   - Impact on all services in the batch
   - Any special refund considerations
   - Date and time of patient request

   **Example**:
   ```
   Patient had medical emergency and cannot continue with scheduled treatment plan.
   All services cancelled: Dr. Smith consultation, ECG test, blood work, X-ray.
   Patient provided written request on 15-Jan-2025 at 3:15 PM.
   Emergency admission to another hospital - unable to complete OPD services.
   Refund: Rs. 5,000 to card ****1234, Rs. 2,500 cash, Rs. 1,500 from patient deposit.
   Authorized by Head Nurse Jane Doe.
   ```

### Step 6: Process the Batch Cancellation

1. Carefully review all batch details and payment information
2. Confirm the total refund amount matches patient payment
3. Verify all individual services will be cancelled
4. Click **Process Batch Cancellation**
5. A detailed confirmation dialog will appear showing:
   - Total refund amount
   - Breakdown by payment method
   - Number of individual bills to be cancelled
6. Click **Confirm** to proceed with the batch cancellation

### Step 7: Handle Refund Processing

After successful batch cancellation, the system will:

#### Immediate Actions
- Mark the entire batch bill as **CANCELLED**
- Cancel all individual bills within the batch
- Generate comprehensive cancellation receipt
- Create detailed audit log entries
- Update patient billing history

#### Refund Processing by Payment Method

Since batch bills often involve **multiple payment methods**, refunds will be processed according to each method:

**Cash Refunds**:
- Prepare exact cash amount for immediate refund
- Print detailed cash refund receipt showing cash portion
- Have patient sign acknowledgment for cash refund amount
- Update your cash drawer with the cash refund amount

**Card Refunds**:
- System automatically initiates refund to original card(s)
- Each card transaction is refunded separately
- Refunds may take 3-7 business days per card
- Provide patient with separate reference numbers for each card refund
- Print card refund receipts for patient records

**Cheque Refunds**:
- Contact finance department for cheque refund processing
- Provide patient with refund reference numbers
- Processing timeline varies based on issuing bank
- Finance team will coordinate with patient on refund method

**Multiple Payment Method Coordination**:
- Patient receives separate refund receipts for each payment method
- Different refund timelines explained clearly to patient
- Total refund amount verified against original batch payment
- Patient informed of expected refund schedule for each method

### Step 8: Complete Documentation

1. **Print All Receipts**:
   - Batch cancellation summary receipt
   - Individual refund receipts for each payment method
   - Patient acknowledgment forms
   - Detailed service cancellation list

2. **Patient Communication**:
   - Explain cancellation of all services in the batch
   - Provide detailed refund timeline for each payment method
   - Give patient all cancellation and refund receipts
   - Schedule any necessary follow-up appointments if appropriate

## Understanding Messages

### Success Messages
- **"Batch bill cancelled successfully"**: All services cancelled and refunds initiated
- **"Multiple refunds processed"**: Refunds initiated for all payment methods
- **"Batch cancellation receipt generated"**: Complete documentation ready

### Information Messages
- **"Batch contains X individual bills"**: Confirms scope of cancellation
- **"Multiple payment methods detected"**: System recognizes complex payment structure
- **"Payment details preserved for audit"**: Original payment information maintained

### Warning Messages
- **"Batch cancellation affects multiple services"**: Confirms comprehensive cancellation scope
- **"Some refunds may take several business days"**: Alerts about varying refund timelines
- **"Patient has other pending bills"**: Consider complete patient billing status

### Error Messages
- **"Batch is already cancelled"**: This batch was previously cancelled
- **"Payment details could not be loaded"**: Contact system administrator
- **"Insufficient batch cancellation privileges"**: Contact administrator for access
- **"Batch cancellation reason is required"**: Select appropriate reason from dropdown

## Financial Impact

### Accounting Effects
Batch bill cancellation has significant financial impact:
- **Revenue Reduction**: Large cancellation amount affects daily revenue
- **Multiple Refund Processing**: Creates several refund transactions
- **Audit Trail Complexity**: Multiple entries across payment methods
- **Department Impact**: Affects revenue across multiple OPD departments

### Cash Flow Impact
- **Immediate**: Cash portions reduce drawer balance significantly
- **Short-term**: Multiple card refunds create larger pending obligations
- **Complex Reporting**: Batch cancellations require detailed breakdown in reports

## Best Practices

### Before Batch Cancellation
1. **Verify Complete Authorization**: Ensure patient wants ALL services cancelled
2. **Review Individual Services**: Confirm each service can be legitimately cancelled
3. **Document Comprehensive Reason**: Note why entire batch needs cancellation
4. **Check Service Status**: Verify no services were already provided

### During Batch Cancellation
1. **Review Payment Accuracy**: Verify all payment methods and amounts are correct
2. **Confirm View-Only Display**: Payment details should be displayed as read-only
3. **Clear Patient Communication**: Explain that ALL services will be cancelled
4. **Print Complete Documentation**: Generate all required receipts

### After Batch Cancellation
1. **Organize Multiple Receipts**: Keep all refund receipts organized for patient
2. **Follow Up on Complex Refunds**: Monitor card and cheque refund progress
3. **Update Patient Records**: Note comprehensive service cancellation
4. **Report to Departments**: Notify affected departments of service cancellations

### Payment Method Handling
1. **Cash**: Count exact amount for immediate patient refund
2. **Cards**: Explain 3-7 day processing time per card used
3. **Cheques**: Coordinate with finance for bank refund processing
4. **Mixed Methods**: Provide clear timeline for each refund type

## Troubleshooting

### Problem: Cancel Batch button not visible
**Symptoms**: Cannot see Cancel Batch button in search results
**Cause**: User lacks batch cancellation privileges
**Solution**: Contact your administrator to request batch bill cancellation permissions

### Problem: Payment details not displaying correctly
**Symptoms**: Payment information appears incomplete or incorrect
**Cause**: Complex payment data loading issue
**Solution**:
1. Refresh the page and search again
2. Verify the batch bill was properly paid
3. Contact system administrator if payment details remain incorrect
4. Do NOT proceed with cancellation until payment details are confirmed

### Problem: View-only payment fields showing validation errors
**Symptoms**: "Bank is required" or similar errors appear even though details are visible
**Cause**: System configuration or display mode issue
**Solution**:
1. This should not occur with properly configured view-only mode
2. Contact system administrator immediately
3. Do not attempt to modify any payment details
4. Document the error for technical support

### Problem: Partial batch cancellation needed
**Symptoms**: Patient wants to cancel only some services in the batch
**Cause**: Batch bills are all-or-nothing for cancellation
**Solution**:
1. Batch cancellation cancels ALL services in the batch
2. For partial cancellations, contact supervisor or administrator
3. May require individual bill handling or administrative intervention

### Problem: Refund processing errors
**Symptoms**: "Unable to process refund for payment method X"
**Cause**: Bank connectivity or payment processor issue
**Solution**:
1. Note which payment methods failed
2. Contact finance department for manual refund processing
3. Provide patient with incident reference numbers
4. Follow up to ensure all refunds are eventually processed

## Configuration (Admin)

### Batch Cancellation Settings
Administrators can configure:
- **User Permissions**: Who can cancel batch bills
- **Approval Workflows**: Required approvals for large batch cancellations
- **Payment Method Display**: Ensuring view-only mode for payment details
- **Reason Categories**: Available batch cancellation reasons

### Financial Controls
- **Daily Limits**: Maximum batch cancellation amounts per user
- **Multi-step Approval**: Required approvals for complex payment scenarios
- **Audit Requirements**: Enhanced documentation for batch cancellations
- **Department Notifications**: Automatic alerts to affected departments

## FAQ

**Q: Why are the payment details shown as view-only during batch cancellation?**
A: Payment details are displayed in view-only mode to preserve data integrity and prevent accidental modification during cancellation. This ensures the original payment information is maintained for accurate refund processing and audit trails.

**Q: Can I cancel just one service from a batch bill?**
A: No, batch bill cancellation cancels ALL individual bills within the batch. For partial cancellations, contact your supervisor for guidance on alternative processes.

**Q: What happens if the patient paid with multiple cards?**
A: Each card will receive a separate refund transaction. The patient will get individual refund receipts for each card, and each refund may take 3-7 business days to process.

**Q: How long do different refund types take for batch bills?**
A: Refund timing varies by method: Cash (immediate), Cards (3-7 business days each), Cheques (varies by bank), eWallet (usually immediate), Staff/Patient accounts (immediate credit).

**Q: What if some services in the batch were already provided?**
A: You should not cancel a batch bill if any services were already provided. Contact your supervisor for guidance on handling partially completed batch bills.

**Q: Can I process a batch cancellation without the patient present?**
A: This depends on hospital policy and the reason for cancellation. Generally, patient authorization is required. Check with your supervisor for specific guidelines.

**Q: What if the batch bill has an unusually large amount?**
A: Large batch cancellations may require supervisor approval before processing. Your system will alert you if additional approval is needed.

**Q: How do I handle cash drawer adjustments for large batch refunds?**
A: For significant cash refunds, coordinate with your supervisor to ensure adequate cash availability and proper cash drawer reconciliation.

## Related Features
- [OPD Bill Search](OPD-Bill-Search.md)
- [Individual Bill Cancellation](Individual-Bill-Cancellation.md)
- [Multiple Payment Methods Processing](../Cashier/Multiple-Payment-Methods-Processing.md)
- [Refund Processing](../Finance/Refund-Processing.md)
- [Cash Drawer Management](../Cashier/Cash-Drawer-Management.md)

## Support

If you have questions or issues with batch bill cancellation:

1. Contact your immediate supervisor or senior cashier
2. Contact your System Administrator
3. Contact the Finance Department for complex refund issues
4. Contact the HMIS Support Team
5. Check the [HMIS User Guide](../index.md)

---

**Document Version**: 1.0
**Last Updated**: January 2025
**Feature**: Batch Bill Cancellation