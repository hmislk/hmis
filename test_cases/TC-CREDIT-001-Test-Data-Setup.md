# 🔧 **Test Data Setup Guide: TC-CREDIT-001**

## **Creating Test Data for Credit Settlement Bug Testing**

### **📋 Overview**
This guide helps QA create appropriate test data to verify the credit settlement due amount calculation fix.

### **🎯 Required Test Scenarios**

#### **Scenario 1: Single Refund (Primary Test)**
```
Purpose: Test basic refund scenario
Bill Type: OPD Batch Bill
Payment Method: Credit
Original Amount: 1,000.00
Refund: 300.00
Expected Due: 700.00
```

#### **Scenario 2: Multiple Refunds**
```
Purpose: Test cumulative refund calculation
Bill Type: OPD Batch Bill
Payment Method: Credit
Original Amount: 1,500.00
Refund 1: 200.00
Refund 2: 300.00
Expected Due: 1,000.00
```

#### **Scenario 3: Large Refund**
```
Purpose: Test when refund is majority of bill
Bill Type: OPD Batch Bill
Payment Method: Credit
Original Amount: 800.00
Refund: 650.00
Expected Due: 150.00
```

#### **Scenario 4: Control (No Refund)**
```
Purpose: Ensure fix doesn't break normal bills
Bill Type: OPD Batch Bill
Payment Method: Credit
Original Amount: 1,200.00
Refund: 0.00
Expected Due: 1,200.00
```

### **🔨 Step-by-Step Test Data Creation**

#### **Step 1: Create OPD Batch Bill**
1. Navigate to: `http://localhost:8080/rh/faces/opd/opd_bill_ac.xhtml`
2. Click "New OPD Bill"
3. Select patient or create new patient
4. Add services/investigations to reach desired amount
5. **Important:** Set Payment Method to "Credit"
6. **Important:** Select a Credit Company
7. Complete and save the bill
8. **Record the bill number generated**

#### **Step 2: Process Refund**
1. Navigate to: `http://localhost:8080/rh/faces/opd/bill_return.xhtml`
2. Search for the bill created in Step 1
3. Select items to refund (to match your test scenario amount)
4. Process the refund
5. **Verify refund amount matches your test scenario**

#### **Step 3: Create Initial Credit Settlement**
1. Navigate to: `http://localhost:8080/rh/faces/credit/credit_company_bill_opd_combined.xhtml`
2. Search for your test bill
3. **Verify it shows correct due amount** (Original - Refund)
4. Create a settlement for the due amount
5. Complete the settlement process

#### **Step 4: Cancel Credit Settlement**
1. Navigate to: `http://localhost:8080/rh/faces/credit/credit_compnay_bill_opd.xhtml`
2. Find the settlement bill created in Step 3
3. Click "To Cancel Bill"
4. Process the cancellation
5. **Your test bill is now ready for bug testing**

### **📊 Test Data Tracking Sheet**

```
Scenario 1 - Single Refund:
Bill Number: ____________________
Original Amount: ____________________
Refund Amount: ____________________
Expected Due: ____________________
Credit Company: ____________________
Status: Created / Pending / Ready

Scenario 2 - Multiple Refunds:
Bill Number: ____________________
Original Amount: ____________________
Refund 1: ____________________
Refund 2: ____________________
Total Refunds: ____________________
Expected Due: ____________________
Credit Company: ____________________
Status: Created / Pending / Ready

Scenario 3 - Large Refund:
Bill Number: ____________________
Original Amount: ____________________
Refund Amount: ____________________
Expected Due: ____________________
Credit Company: ____________________
Status: Created / Pending / Ready

Scenario 4 - Control:
Bill Number: ____________________
Original Amount: ____________________
Refund Amount: 0.00
Expected Due: ____________________
Credit Company: ____________________
Status: Created / Pending / Ready
```

### **⚠️ Important Notes**

1. **Credit Company Requirement:**
   - Ensure credit company is properly configured
   - Test with different credit companies if possible
   - Common credit companies: Insurance corporations, corporate accounts

2. **Patient Selection:**
   - Use existing test patients if available
   - Create new patients with proper demographics
   - Ensure patient has necessary information for billing

3. **Service Selection:**
   - Use common OPD services for realistic test data
   - Vary service types across test scenarios
   - Ensure services have proper pricing configured

4. **Timing Considerations:**
   - Allow some time between bill creation and refund processing
   - Process settlements and cancellations in realistic timeframes
   - Don't rush through the steps to avoid system timing issues

### **🔍 Verification Checklist**

Before proceeding with bug testing, verify each test bill:
- [ ] Bill is saved and has valid bill number
- [ ] Payment method is set to Credit
- [ ] Credit company is assigned
- [ ] Refund is processed and amount is correct
- [ ] Initial settlement was created and cancelled
- [ ] Bill appears in credit settlement search

### **🚨 Common Issues During Setup**

1. **Credit Company Not Selected:**
   - Bills without credit company won't appear in credit settlement
   - Verify credit company is properly assigned

2. **Refund Processing Errors:**
   - Ensure original bill is in correct status
   - Verify user has refund processing privileges
   - Check that refund amount doesn't exceed bill amount

3. **Settlement Creation Issues:**
   - Verify credit company matches between bill and settlement
   - Ensure user has settlement creation privileges
   - Check that due amount calculation is working in pre-fix state

4. **Cancellation Problems:**
   - Verify settlement bill is in correct status
   - Ensure user has cancellation privileges
   - Allow sufficient time for settlement to be processed before cancelling

### **📞 Support**
If you encounter issues during test data creation:
1. Check user privileges for all required operations
2. Verify system configuration for credit companies
3. Escalate to development team if setup steps fail
4. Document any errors encountered during setup

---
**Guide Version:** 1.0
**Last Updated:** 2026-01-08
**Related Test Case:** TC-CREDIT-001