# Patient Deposit Management
## Administrator Guide for HMIS

---

## 📋 Overview

This guide provides hospital administrators with information about patient deposit management in the HMIS system. Patient deposits operate with department-level isolation, providing proper financial controls and audit trails.

---

## ⚙️ System Configuration

### **Configuration Setting**
- **Configuration Name**: `"Patient Deposits are Department Specific"`
- **Status**: ✅ **ENABLED** (Automatically activated)
- **Effect**: All patient deposits are isolated by department
- **Access**: System enables this configuration automatically when accessing patient deposit functions

### **Department-Level Isolation Benefits**
- ✅ Each department manages only their own patient deposits
- ✅ Cross-department deposit access is prevented
- ✅ Financial reporting is department-specific
- ✅ Audit trails maintain department accountability
- ✅ Better financial controls and reconciliation

---

## 🏥 Patient Deposit Management Pages

### **1. Patient Deposit Management Dashboard**
**Navigation**: Menu > Payment > Patient Deposit Management

**Function**: Main navigation hub for all patient deposit operations
**Department Compliance**: ✅ **COMPLIANT** - Navigation only, no data filtering required
**Details**: Provides centralized access to all patient deposit functions with appropriate privilege controls

---

### **2. Accept Patient Deposits**
**Navigation**: Menu > Payment > Patient Deposit Management > Accept Patient Deposits

**Function**: Staff can accept and process patient deposits
**Department Compliance**: ✅ **FULLY COMPLIANT**
**Implementation**:
- Deposits are automatically associated with the logged-in user's department
- Deposit receipts show department information
- Each department has separate deposit accounts per patient

---

### **3. Search Patient Deposits**
**Navigation**: Menu > Payment > Patient Deposit Management > Search Patient Deposits

**Function**: Search and view existing patient deposits
**Department Compliance**: ✅ **FULLY COMPLIANT**
**Implementation**:
- Search results are filtered to show only deposits from the logged-in user's department
- Cross-department deposit visibility is prevented
- Department filtering is automatically applied in search queries

---

### **4. Return Patient Deposits**
**Navigation**: Menu > Payment > Patient Deposit Management > Return Patient Deposits

**Function**: Process patient deposit returns/refunds
**Department Compliance**: ✅ **FULLY COMPLIANT**
**Implementation**:
- Only deposits from the current department can be returned
- Return transactions are recorded with department information
- Refund bills maintain department association

---

### **5. Cancel Patient Deposits**
**Navigation**: Accessible through individual deposit records in search results

**Function**: Cancel patient deposit transactions
**Department Compliance**: ✅ **FULLY COMPLIANT**
**Implementation**:
- Only deposits created by the current department can be cancelled
- Cancellation maintains audit trail with department information
- Department restrictions prevent unauthorized cancellations

---

## 💊 Patient Deposit Utilization in Billing

### **6. Pharmacy Retail Sale**
**Navigation**: Menu > Pharmacy > Retail Sale

**Function**: Use patient deposits to pay for pharmacy retail sales
**Department Compliance**: ✅ **FULLY COMPLIANT**
**Implementation**:
- Only patient deposits from the current department are available for utilization
- Deposit balance shown is department-specific
- Payment utilization is restricted to logged-in user's department

---

### **7. Pharmacy Pre-Settlement**
**Navigation**: Menu > Pharmacy > Pre-Settlement

**Function**: Use patient deposits for pharmacy bill pre-settlement
**Department Compliance**: ✅ **FULLY COMPLIANT**
**Implementation**:
- Same department-level restrictions as pharmacy retail sale
- Pre-settlement bills respect department boundaries
- Deposit utilization is department-specific

---

### **8. OPD Billing**
**Navigation**: Menu > OPD > Billing > OPD Billing

**Function**: Use patient deposits to pay for OPD services
**Department Compliance**: ✅ **FULLY COMPLIANT**
**Implementation**:
- OPD bills can only utilize deposits from the same department
- Patient account balance displays department-specific amounts

---

## 📊 Financial Reporting

### **9. Daily Return Financial Report**
**Navigation**: Menu > Reports > Financial Reports > Daily Return

**Function**: Generate comprehensive daily financial reports including patient deposit data
**Department Compliance**: ✅ **FULLY COMPLIANT**
**Implementation**:
- **Patient Deposit Receipts**: Shows only deposits received by the selected department
- **Patient Deposit Utilization**: Shows only deposit usage by the selected department
- Department filter controls all patient deposit financial calculations
- Report totals reflect department-specific amounts

---

## 🔐 Security & Access Control

### **Department Isolation**
- ✅ **Physical Separation**: Each department has separate patient deposit accounts
- ✅ **Access Control**: Users can only access deposits from their logged-in department
- ✅ **Data Integrity**: Cross-department deposit modifications are prevented
- ✅ **Audit Compliance**: All transactions maintain department attribution

### **User Role Requirements**
- Users must have appropriate privileges (`Payment` privilege for deposit management)
- Users must be assigned to a specific department
- Department assignment determines accessible deposit accounts

---

## 🎯 Administrator Verification Checklist

To verify proper department-level isolation, administrators should:

### **✅ Deposit Creation Testing**
1. Login as User from Department A
2. Create a patient deposit
3. Verify deposit is associated with Department A
4. Login as User from Department B
5. Confirm Department A's deposit is not visible

### **✅ Deposit Utilization Testing**
1. Login as User from Department A with patient deposits
2. Attempt to use deposits in pharmacy/OPD billing
3. Verify only Department A deposits are available
4. Login as User from Department B
5. Confirm Department A's deposits cannot be accessed

### **✅ Search Functionality Testing**
1. Create deposits from different departments
2. Login as each department user
3. Verify search results show only respective department deposits
4. Confirm cross-department visibility is prevented

### **✅ Reporting Testing**
1. Access Daily Return report
2. Select specific department filter
3. Verify patient deposit sections show only that department's data
4. Confirm totals reflect department-specific amounts

---

## ⚠️ Important Notes for Administrators

### **Configuration Maintenance**
- The system automatically enables department-specific deposits
- No manual configuration changes are required
- Configuration changes should be tested in a development environment first

### **Data Migration**
- Existing patient deposits without department assignment are automatically associated with the user's current department when accessed
- No manual data migration is required
- Historical deposits integrate seamlessly with department restrictions

### **Backup & Recovery**
- Patient deposit department associations are stored in the database
- Standard backup procedures protect department-level data integrity
- Recovery procedures maintain department isolation

### **Training Requirements**
- Staff should understand that patient deposits are department-specific
- Cross-department deposit transfers require proper procedures if needed
- Financial reporting reflects department-level isolation

---

## 📞 Support & Troubleshooting

### **Common Issues**
1. **Cannot see patient deposits**: Verify user is assigned to the correct department
2. **Deposit not available for utilization**: Confirm deposit was created by the same department
3. **Report shows zero deposits**: Ensure correct department filter is selected

### **For Technical Support**
- Patient deposit department management is fully automated
- System configuration is handled automatically
- Contact system administrators for privilege or department assignment issues

---

**Document Version**: 1.0
**System**: HMIS Patient Deposit Management
**Compliance Status**: ✅ Fully Department-Level Compliant