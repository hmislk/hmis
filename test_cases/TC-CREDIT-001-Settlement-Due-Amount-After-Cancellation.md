## 📋 **QA Test Case: Credit Settlement Due Amount Calculation After Cancellation**

### **Test Case ID:** TC-CREDIT-001
### **Priority:** High (Financial Bug)
### **Module:** Credit Settlement / OPD Billing

---

### **📊 Test Summary**
Verify that after cancelling a credit settlement for OPD batch bills that have existing refunds, the due amount calculation is accurate in both autocomplete search and bill details panel.

---

### **🔧 Preconditions**
- User has access to Credit Settlement page: `http://localhost:8080/rh/faces/credit/credit_company_bill_opd_combined.xhtml`
- Test data is prepared according to setup guide

---

### **🎯 Test Data Requirements**
**Required Test Bill Characteristics:**
- **Bill Type:** OPD Batch Bill
- **Payment Method:** Credit
- **Original Amount:** Any amount (e.g., 1,000.00 or higher)
- **Must have:** At least one processed refund
- **Must have:** Previous credit settlement that was cancelled

**Example Values for Testing:**
- **Original Billed Amount:** [ORIGINAL_AMOUNT]
- **Refund Amount:** [REFUND_AMOUNT]
- **Expected Due Amount:** [ORIGINAL_AMOUNT - REFUND_AMOUNT]

---

### **📋 Test Steps**

#### **Step 1: Verify Test Bill Setup**
1. Navigate to credit settlement: `http://localhost:8080/rh/faces/credit/credit_company_bill_opd_combined.xhtml`
2. Locate your test bill: `[TEST_BILL_NUMBER]`
3. Verify bill has:
   - **Billed Amount:** [ORIGINAL_AMOUNT]
   - **Payment Method:** Credit
   - **Refund processed:** [REFUND_AMOUNT]
   - **Previous settlement:** Cancelled

#### **Step 2: Autocomplete Due Amount Test**
1. In the "Select Bill" autocomplete field, type: `[TEST_BILL_NUMBER]`
2. Observe the "Due Amount" column in search results
3. **Expected Result:** Due Amount shows `[ORIGINAL_AMOUNT - REFUND_AMOUNT]`

#### **Step 3: Bill Details Panel Test**
1. Select the test bill from autocomplete
2. Review "Selected Bill Details" panel
3. **Expected Results:**
   - Due Amount: `[ORIGINAL_AMOUNT - REFUND_AMOUNT]`
   - Refund Amount: `[REFUND_AMOUNT]`

#### **Step 4: Settlement Amount Validation**
1. Try to enter payment amount: `[ORIGINAL_AMOUNT]` (full original amount)
2. Click "Add to Bill"
3. **Expected Result:** System should prevent entry or show validation error

#### **Step 5: Correct Settlement Test**
1. Clear previous entries
2. Enter correct amount: `[ORIGINAL_AMOUNT - REFUND_AMOUNT]`
3. Click "Add to Bill"
4. **Expected Result:** System accepts the correct amount
5. Complete settlement process
6. **Expected Result:** Bill shows as fully settled after settlement

---

### **✅ Acceptance Criteria**

| **Criteria** | **Expected Result** |
|--------------|-------------------|
| **Autocomplete Due Amount** | Shows (Original - Refund) amount |
| **Bill Details Due Amount** | Shows (Original - Refund) amount |
| **Refund Amount Display** | Correctly shows actual refund amount |
| **Settlement Validation** | Prevents overpayment |
| **Financial Accuracy** | No overpayment possible |

---

### **🔍 Additional Test Scenarios**

#### **Test Scenario A: Multiple Refunds**
1. Use bill with multiple partial refunds
2. Follow same test steps
3. **Verify:** Due amount = Original Amount - Sum of All Refunds

#### **Test Scenario B: Different Credit Companies**
1. Test with bills from different credit companies
2. **Verify:** Calculation accuracy regardless of credit company

#### **Test Scenario C: Zero Due Amount**
1. Find bill where refunds equal or exceed original amount
2. **Verify:** Due amount shows as 0.00
3. **Verify:** Cannot create new settlement

---

### **⚠️ Risk Areas to Test**
1. **Financial Reconciliation:** Ensure no overpayments are possible
2. **Different Bill Types:** Test with OPD batch bills and package bills
3. **Multiple Refund Scenarios:** Bills with several partial refunds

---

### **🔄 Regression Testing**
Verify the fix doesn't break existing functionality:
1. **Bills without refunds:** Due amount calculation remains accurate
2. **Bills with partial settlements:** Remaining due amount is correct
3. **Different payment methods:** Cash/card settlements work correctly

---

### **📊 Test Data Template**
```
Test Bill 1:
- Bill Number: _______
- Original Amount: _______
- Refund Amount: _______
- Expected Due: _______
- Credit Company: _______

Test Bill 2 (Multiple Refunds):
- Bill Number: _______
- Original Amount: _______
- Refund Amount 1: _______
- Refund Amount 2: _______
- Expected Due: _______
- Credit Company: _______

Test Bill 3 (Control - No Refund):
- Bill Number: _______
- Original Amount: _______
- Expected Due: _______
- Credit Company: _______
```

---

### **📝 Test Results**
```
Test Execution Date: _______
Tester: _______

Test Bill: _______
Original Amount: _______
Refund Amount: _______
Expected Due Amount: _______
Actual Due Amount Displayed: _______

Autocomplete Test: PASS / FAIL
Bill Details Test: PASS / FAIL
Settlement Validation Test: PASS / FAIL
Overall Result: PASS / FAIL

Comments: _________________________
```

---

**Test Case Created:** 2026-01-08
**Module:** Credit Settlement
**Related Pages:**
- Credit Settlement: `/credit/credit_company_bill_opd_combined.xhtml`
- Bill Cancellation: `/credit/credit_company_bill_cancel.xhtml`