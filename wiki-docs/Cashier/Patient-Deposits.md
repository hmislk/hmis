# Patient Deposits

## Overview

Patient Deposits allow patients to pre-pay money into their account before receiving services or products from the hospital. When patients use hospital services (consultations, pharmacy purchases, lab tests, etc.), charges can be deducted from their existing deposit balance. Patients can also request refunds for unused deposits, and staff can cancel deposit transactions if made in error. This system provides convenience for regular patients, ensures faster billing, and gives patients flexibility in managing their healthcare payments.

## When to Use

Use Patient Deposits when you need to:
- Accept advance payments from patients before they receive services
- Process payments for services using a patient's existing deposit balance
- Refund unused deposit amounts to patients
- Cancel deposit transactions made in error
- View deposit history and utilization records
- Check a patient's current deposit balance
- Generate reports on deposit collections and usage

## Accepting Patient Deposits

### Accessing the Feature

1. Navigate to **Payment** in the main menu
2. Select **Patient Deposit Management**
3. Click **Accept Patient Deposits**

### Processing a Deposit

1. Search for the patient by name, phone number, or patient ID
2. Select the patient from the search results
3. Enter the deposit amount in the **Amount** field
4. Select the payment method:
   - **Cash** - For cash payments
   - **Credit Card** - For card payments
   - **Cheque** - For cheque payments
5. Click **Accept Deposit** to process the transaction
6. Print the deposit receipt for the patient

> **Note:** Each deposit is recorded with the date, time, amount, payment method, and the staff member who processed it.

## Using Deposits for Payments

Patient deposits can be used to pay for hospital services across different departments.

### Paying with Patient Deposit

When billing a patient for services:

1. Search and select the patient
2. Add items/services to the bill
3. In the payment section, select **Patient Deposit** as the payment method
4. The system displays the patient's current deposit balance
5. If the balance is sufficient, click **Settle Bill**
6. The bill amount is automatically deducted from the deposit balance

### Partial Payments with Multiple Methods

When the deposit balance doesn't cover the full bill amount:

1. Select **Multiple Payment Methods** from the payment dropdown
2. Click **Add Payment** and select **Patient Deposit**
3. Enter the amount to use from the deposit (up to available balance)
4. Click **Add Payment** again for additional payment methods (Cash, Card, etc.)
5. Enter amounts for each payment method
6. Verify the total equals the bill amount (Balance Amount should be 0.00)
7. Click **Settle Bill** to process

### Where Deposits Can Be Used

- **OPD Billing** - Consultation fees and OPD services
- **Pharmacy** - Retail sales and prescription medications
- **Laboratory** - Lab tests and investigations
- **Radiology** - X-rays, scans, and imaging services
- **Other Departments** - Any department with billing capability

## Refunding Patient Deposits

### When to Refund

Process a refund when:
- Patient requests return of unused deposit amount
- Patient is discontinuing treatment
- Patient is transferring to another facility
- Excess deposit remains after treatment completion

### Processing a Refund

1. Navigate to **Payment** > **Patient Deposit Management** > **Return Patient Deposits**
2. Search for the patient
3. View the patient's current deposit balance
4. Enter the refund amount (cannot exceed available balance)
5. Select the refund method:
   - **Cash** - Return cash to patient
   - **Cheque** - Issue refund cheque
6. Click **Process Refund**
7. Print the refund receipt for the patient's records
8. Have the patient acknowledge receipt of the refund

> **Important:** Keep proper documentation of all refund transactions for audit purposes.

## Cancelling Patient Deposits

### When to Cancel

Cancel a deposit only when:
- The deposit was recorded in error (wrong patient, wrong amount)
- Duplicate entry was created
- Payment method was recorded incorrectly
- Transaction was made by mistake before completion

### Processing a Cancellation

1. Navigate to **Payment** > **Patient Deposit Management** > **Search Patient Deposits**
2. Search for the deposit transaction to cancel
3. Select the deposit record from the search results
4. Click **Cancel Deposit**
5. Enter the reason for cancellation (required for audit trail)
6. Confirm the cancellation
7. The deposit is marked as cancelled and balance is adjusted

> **Warning:** Cancellation cannot be undone. Verify the transaction details carefully before cancelling. For deposits that have already been partially used, consider processing a refund instead.

## Viewing Deposit History

### Accessing Deposit History

1. Navigate to **Payment** > **Patient Deposit Management** > **Search Patient Deposits**
2. Search by patient name, date range, or deposit number
3. View the list of all deposit transactions

### Information Available

The deposit history shows:
- **Date and Time** - When the deposit was made
- **Deposit Number** - Unique reference number
- **Patient Name** - Patient identification
- **Amount** - Deposit amount received
- **Payment Method** - Cash, card, or cheque
- **Status** - Active, Used, Refunded, or Cancelled
- **Received By** - Staff member who processed the deposit
- **Balance** - Remaining unused amount

### Viewing Utilization Details

To see how a deposit has been used:

1. Select a deposit record from the search results
2. Click **View Details** or expand the record
3. View the list of bills paid using this deposit
4. See dates, bill numbers, and amounts deducted

## Reports

### Available Reports

#### Patient Deposit Summary
- Shows total deposits received by date range
- Breaks down by payment method
- Shows department-wise collection

#### Deposit Utilization Report
- Lists all deposits used for payments
- Shows which bills were paid from deposits
- Tracks utilization percentage

#### Outstanding Deposits Report
- Lists all patients with unused deposit balances
- Shows deposit age (how long funds have been held)
- Helps identify deposits pending refund or utilization

#### Deposit Balance Report
- Current balance for each patient
- Historical balance changes
- Useful for patient inquiries

### Generating Reports

1. Navigate to **Reports** > **Financial Reports** or **Cashier Reports**
2. Select the desired report type
3. Enter filter criteria:
   - **Date Range** - From and To dates
   - **Department** - If applicable
   - **Patient** - For individual patient reports
4. Click **Generate Report**
5. Export to Excel or PDF if needed

## Understanding Messages

### Success Messages
- **"Deposit Accepted Successfully"** - Deposit has been recorded
- **"Balance Updated"** - Deposit balance has been modified after payment
- **"Refund Processed Successfully"** - Refund has been completed
- **"Deposit Cancelled"** - Cancellation is complete

### Warning Messages
- **"No Sufficient Patient Deposit"** - Available balance is less than required amount; use multiple payment methods or top up the deposit
- **"Deposit Already Used"** - Cannot cancel a deposit that has been partially or fully utilized

### Error Messages
- **"Patient has NO Patient Deposit"** - Patient doesn't have a deposit account; accept a deposit first
- **"No Patient Deposit"** - No deposit found for the selected patient
- **"Invalid Amount"** - Enter a valid positive amount
- **"Cancellation Failed"** - Check if the deposit has been used or already cancelled

## Best Practices

### Accepting Deposits
- Always verify patient identity before accepting deposits
- Issue printed receipts for all deposits
- Count cash in front of the patient
- Verify card payment approval before confirming

### Using Deposits for Payment
- Inform patients of their current balance before billing
- Suggest deposit top-up if balance is low
- Print receipts showing deposit deduction details

### Processing Refunds
- Verify patient identity before processing refunds
- Get patient signature acknowledging receipt
- Keep copies of refund documentation
- Process refunds promptly when requested

### Record Keeping
- Maintain accurate records of all transactions
- Reconcile deposits daily
- Report discrepancies immediately
- Keep audit trail documentation accessible

## Department-Specific Deposits

### Configuration Options

The system can be configured for either:

**Application-Wide Deposits** (Default for some installations)
- Patient's deposit balance is shared across all departments
- One balance can be used anywhere in the hospital

**Department-Specific Deposits**
- Each department maintains separate deposit balances
- Deposits accepted in OPD can only be used in OPD
- Provides better departmental financial control

> **Note:** Check with your system administrator for which mode is enabled at your facility.

## Troubleshooting

### Problem: Cannot Find Patient Deposit Record
**Cause:** Searching with incorrect criteria or deposit not in current department
**Solution:** Try different search parameters; verify you have access to the correct department

### Problem: Deposit Balance Shows Zero But Patient Claims They Deposited
**Cause:** Deposit may have been used, refunded, or made in different department
**Solution:** Check deposit history for utilization; verify department-specific settings

### Problem: Cannot Use Deposit for Payment
**Cause:** Insufficient balance or department mismatch
**Solution:** Check current balance; verify deposit department matches billing department

### Problem: Refund Button Not Available
**Cause:** No balance available or insufficient privileges
**Solution:** Check deposit has unused balance; verify your user privileges include refund processing

## FAQ

**Q: Can a patient deposit money in one department and use it in another?**
A: This depends on your system configuration. If "Department-Specific Deposits" is enabled, deposits can only be used in the department where they were accepted. Otherwise, deposits can be used anywhere.

**Q: What happens to unused deposits if a patient doesn't return?**
A: Unused deposits remain in the patient's account until they are used or refunded. The Outstanding Deposits Report helps track these balances. Follow your hospital's policy for handling dormant deposits.

**Q: Can I cancel a deposit that has been partially used?**
A: No, deposits that have been used (even partially) cannot be cancelled. Process a refund for any remaining balance instead.

**Q: Is there a limit on deposit amounts?**
A: There is typically no system-imposed limit, but your hospital may have policies on maximum deposit amounts. Check with your supervisor.

**Q: Can deposits be transferred between patients?**
A: No, deposits are linked to specific patients and cannot be transferred. If a deposit was recorded for the wrong patient, cancel it and create a new one for the correct patient.

## Related Features

- [Cashier Dashboard Configuration](Cashier-Dashboard-Configuration)
- [Fund Transfer Bills](Fund-Transfer-Bill-Configuration)
- [Shift End Handover](Shift-End-For-Handover-Configuration)
