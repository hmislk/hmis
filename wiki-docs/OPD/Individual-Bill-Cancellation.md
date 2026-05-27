# Individual Bill Cancellation

## Overview

Individual bill cancellation allows cashiers to cancel single OPD bills when services need to be reversed, refunded, or corrected. This process handles the cancellation of one specific bill and processes any necessary refunds based on the original payment method used.

## When to Use

Use individual bill cancellation when you need to:

- Cancel a bill for services that were not provided
- Process refunds for cancelled consultations or procedures
- Correct billing errors for individual services
- Handle patient complaints about incorrect charges
- Cancel duplicate bills created by mistake
- Process refunds when patients cannot receive scheduled services

## Prerequisites

Before cancelling an individual bill, ensure:

1. ✓ You have cancellation privileges (Cancel button is visible)
2. ✓ Patient has provided valid reason for cancellation
3. ✓ Bill is not already cancelled or partially refunded
4. ✓ You have proper authorization if required by hospital policy
5. ✓ Bill payment status is confirmed (paid bills require refund processing)

## How to Use

### Step 1: Find the Bill to Cancel

1. Navigate to **Main Menu** → **OPD Sub Menu** → **Search** → **OPD Bill Search**
2. Search for the patient's bill using:
   - Patient name, BHT number, or phone number
   - Bill number if available
   - Date range when service was provided
3. Locate the specific bill in the search results
4. Verify the bill details match the patient's request

### Step 2: Access Bill Cancellation

1. Click the **Cancel** button next to the bill in search results
2. The **Individual Bill Cancellation** page will open
3. Review the bill information displayed:
   - **Patient Details**: Name, BHT number, contact information
   - **Service Details**: List of services, fees, and doctors
   - **Payment Information**: Payment method, amount, transaction details
   - **Bill Status**: Current status and any previous modifications

### Step 3: Review Payment Details

The system will display the original payment information:

#### For Single Payment Method Bills
- **Payment Method**: Cash, Card, Cheque, eWallet, etc.
- **Amount**: Total amount paid
- **Transaction Details**: Reference numbers, bank details, dates
- **Refund Amount**: Amount to be refunded (usually equals paid amount)

#### For Cash Payments
- **Amount Paid**: Total cash received
- **Refund Due**: Cash amount to return to patient

#### For Card Payments
- **Bank**: Issuing bank name
- **Reference Number**: Card transaction reference
- **Amount**: Card transaction amount
- **Refund Process**: Card refund will be processed through the bank

#### For Cheque Payments
- **Bank**: Bank that issued the cheque
- **Cheque Number**: Cheque reference number
- **Date**: Cheque date
- **Amount**: Cheque amount
- **Refund Process**: Cheque refund requires bank coordination

#### For Other Payment Methods
- **eWallet**: Digital wallet transaction details
- **Staff Credit**: Staff account credit details
- **Patient Deposit**: Patient advance payment details

### Step 4: Enter Cancellation Details

1. **Cancellation Reason**: Select from dropdown options:
   - Patient Request
   - Service Not Provided
   - Billing Error
   - Duplicate Bill
   - Doctor Unavailable
   - Medical Emergency
   - Other (requires explanation)

2. **Comments**: Enter detailed explanation including:
   - Specific reason for cancellation
   - Patient request reference or authorization
   - Any special circumstances
   - Date and time of patient request

   **Example**:
   ```
   Patient requested cancellation due to sudden family emergency.
   Unable to attend scheduled consultation with Dr. Smith on 15-Jan-2025.
   Patient provided written request on 15-Jan-2025 at 2:30 PM.
   Refund to be processed back to original card ending in 1234.
   ```

### Step 5: Process the Cancellation

1. Review all details carefully
2. Confirm the refund amount and method
3. Click **Process Cancellation**
4. A confirmation dialog will appear asking you to confirm
5. Click **Confirm** to proceed with the cancellation

### Step 6: Handle Refund Processing

After successful cancellation, the system will:

#### Immediate Actions
- Mark the bill as **CANCELLED**
- Generate a cancellation receipt
- Create an audit log entry
- Update patient billing history

#### Refund Processing by Payment Method

**Cash Refunds**:
- Prepare exact cash amount for immediate refund
- Print cash refund receipt
- Have patient sign the refund acknowledgment
- Document cash refund in cash register

**Card Refunds**:
- System initiates automatic refund to original card
- Refund may take 3-7 business days to appear on patient's card
- Provide patient with refund reference number
- Keep refund receipt for your records

**Cheque Refunds**:
- Contact finance department for cheque refund process
- Provide patient with refund reference number
- Refund timeline depends on bank processing

**Other Payment Method Refunds**:
- eWallet refunds are processed automatically
- Staff credit refunds are added back to staff account
- Patient deposit refunds are credited back to patient deposit account

### Step 7: Complete Documentation

1. **Print Receipts**:
   - Original bill cancellation receipt
   - Refund receipt (if applicable)
   - Patient acknowledgment form

2. **Patient Communication**:
   - Explain the cancellation process
   - Provide refund timeline information
   - Give patient the cancellation receipt
   - Note any follow-up required

## Understanding Messages

### Success Messages
- **"Bill cancelled successfully"**: Cancellation completed and refund processed
- **"Cancellation receipt generated"**: Documentation ready for printing
- **"Refund initiated"**: Payment refund process has started

### Information Messages
- **"Cancellation requires supervisor approval"**: Contact your supervisor before proceeding
- **"Card refund may take 3-7 business days"**: Patient should expect delayed refund
- **"Patient deposit refund added to account"**: Credit added to patient's deposit balance

### Warning Messages
- **"Bill has partial payments"**: Review payment history before cancelling
- **"Cancellation will affect batch bill"**: This bill is part of a larger batch bill
- **"Patient has unpaid bills"**: Consider payment history before processing refund

### Error Messages
- **"Bill is already cancelled"**: This bill was previously cancelled
- **"Insufficient cancellation privileges"**: Contact administrator for access
- **"Payment details not found"**: Contact system administrator for support
- **"Cancellation reason is required"**: Select a reason from the dropdown

## Financial Impact

### Accounting Effects
When you cancel an individual bill:
- **Revenue Reduction**: The cancelled amount reduces daily revenue
- **Refund Processing**: Creates a refund transaction in the system
- **Audit Trail**: All changes are logged for financial reporting
- **Batch Bill Impact**: If the bill is part of a batch, batch totals are updated

### Cash Flow Impact
- **Immediate**: Cash refunds reduce cash drawer balance
- **Short-term**: Card refunds create pending refund obligations
- **Reporting**: Cancelled bills appear in daily cancellation reports

## Best Practices

### Before Cancellation
1. **Verify Patient Identity**: Confirm patient identity before processing
2. **Document Authorization**: Get written request when required
3. **Review Payment History**: Check for any previous cancellations or refunds
4. **Confirm Service Status**: Verify services were not already provided

### During Cancellation
1. **Double-check Details**: Verify bill number, patient name, and amounts
2. **Clear Communication**: Explain the process and timeline to patient
3. **Print Documentation**: Generate all required receipts and forms
4. **Follow Procedures**: Adhere to hospital refund policies

### After Cancellation
1. **File Documentation**: Keep cancellation receipts and patient acknowledgments
2. **Update Patient**: Inform patient about refund status and timeline
3. **Record in Register**: Document cash refunds in your cash register
4. **Report Issues**: Notify supervisor of any unusual cancellations

## Troubleshooting

### Problem: Cancel button not visible
**Symptoms**: Cannot see Cancel button in search results
**Cause**: User lacks cancellation privileges
**Solution**: Contact your administrator to request individual bill cancellation permissions

### Problem: Cannot select cancellation reason
**Symptoms**: Dropdown is empty or disabled
**Cause**: System configuration issue
**Solution**:
1. Refresh the page and try again
2. Contact system administrator if problem persists

### Problem: Payment details not showing
**Symptoms**: Payment information appears blank
**Cause**: Bill may not be properly paid or data issue
**Solution**:
1. Verify bill payment status in bill search
2. Contact supervisor if payment details are missing
3. Do not proceed with cancellation until payment details are confirmed

### Problem: Error during cancellation processing
**Symptoms**: "Error processing cancellation" message appears
**Cause**: System connectivity or database issue
**Solution**:
1. Do not attempt cancellation again immediately
2. Contact system administrator
3. Document the attempt for follow-up

### Problem: Card refund failed
**Symptoms**: "Card refund could not be processed" message
**Cause**: Bank connectivity or card account issue
**Solution**:
1. Note the error for finance department
2. Process manual refund through finance department
3. Provide patient with incident reference number

## Configuration (Admin)

### Cancellation Settings
Administrators can configure:
- **User Permissions**: Who can cancel individual bills
- **Approval Requirements**: Whether supervisor approval is needed
- **Reason Categories**: Available cancellation reasons
- **Refund Methods**: Supported refund processing options

### Financial Controls
- **Daily Limits**: Maximum cancellation amounts per user per day
- **Audit Requirements**: Additional documentation requirements
- **Approval Workflows**: Multi-step approval for large refunds
- **Reporting**: Automatic generation of cancellation reports

## FAQ

**Q: Can I cancel a bill that was paid several days ago?**
A: Yes, you can cancel bills from previous days, but refund processing times may vary based on the payment method used.

**Q: What happens if a patient paid with multiple payment methods?**
A: For bills with multiple payment methods, the individual bill shows only one payment method. Multiple payment method bills are typically part of batch bills - use [Batch Bill Cancellation](Batch-Bill-Cancellation.md) instead.

**Q: Can I partially cancel a bill for only some services?**
A: No, individual bill cancellation cancels the entire bill. For partial cancellations, contact your supervisor or administrator for guidance.

**Q: How long does a card refund take?**
A: Card refunds typically take 3-7 business days to appear on the patient's statement, depending on their bank's processing time.

**Q: What if the patient lost their receipt?**
A: You can still cancel the bill if you can verify the patient's identity and locate the bill in the system. The patient's receipt is helpful but not mandatory.

**Q: Can I cancel a bill for a patient who is not present?**
A: This depends on hospital policy. Generally, the patient or their authorized representative should be present. Check with your supervisor for specific guidelines.

**Q: What if I made a mistake and cancelled the wrong bill?**
A: Contact your supervisor immediately. Cancellations cannot be easily reversed, and a new bill may need to be created for the correct services.

## Related Features
- [OPD Bill Search](https://github.com/hmislk/hmis/wiki/OPD-Bill-Search)
- [Batch Bill Cancellation](https://github.com/hmislk/hmis/wiki/Batch-Bill-Cancellation)

## Support

If you have questions or issues with individual bill cancellation:

1. Contact your immediate supervisor or senior cashier
2. Contact your System Administrator
3. Contact the Finance Department for refund issues
4. Contact the HMIS Support Team
5. Check the [HMIS User Guide](../index.md)

---

**Document Version**: 1.0
**Last Updated**: January 2025
**Feature**: Individual Bill Cancellation