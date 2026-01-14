# 📝 **Quick Test Checklist: TC-CREDIT-001**

## **Credit Settlement Due Amount Bug - QA Verification**

### **🔧 Pre-Test Setup**
- [ ] Identify OPD batch bill with credit payment + existing refund
- [ ] Record bill number: `___________________`
- [ ] Record original amount: `___________________`
- [ ] Record refund amount: `___________________`
- [ ] Calculate expected due: `___________________`

### **⚡ Quick Validation Steps**

#### **1. Autocomplete Test**
- [ ] Go to: `/credit/credit_company_bill_opd_combined.xhtml`
- [ ] Search for test bill in autocomplete
- [ ] **Check:** Due Amount = (Original - Refund) ✅

#### **2. Bill Details Test**
- [ ] Select bill from autocomplete
- [ ] **Check:** Due Amount in details panel = (Original - Refund) ✅
- [ ] **Check:** Refund Amount displays correctly ✅

#### **3. Settlement Limit Test**
- [ ] Try to enter full original amount
- [ ] **Check:** System prevents overpayment ✅
- [ ] Enter correct due amount
- [ ] **Check:** Settlement processes successfully ✅

### **❌ Bug Symptoms (Before Fix)**
- Due Amount shows original amount (ignoring refunds)
- System allows settlement of more than actually due
- Potential financial overpayment

### **✅ Success Criteria (After Fix)**
- Due Amount accurately reflects original minus refunds
- Settlement limited to actual due amount
- No financial discrepancies possible

### **📊 Test Results**
```
Test Bill: ___________________
Original: ___________________
Refund: ___________________
Expected Due: ___________________
Actual Due Shown: ___________________
Result: PASS / FAIL
```

### **🚨 Escalate If:**
- Due amount still shows original amount
- System allows overpayment
- Any financial calculations appear incorrect
- Database balance field doesn't match expected values

---
**Quick Reference for:** TC-CREDIT-001-Settlement-Due-Amount-After-Cancellation