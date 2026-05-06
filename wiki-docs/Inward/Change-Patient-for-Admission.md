# Change Patient for Admission

## Overview

The **Change Patient for Admission** feature allows authorized users to reassign an admission (BHT) from one patient to another. This is useful in cases where an admission was mistakenly created under the wrong patient record.

### Important Information

⚠️ **Critical**: This action permanently changes which patient is associated with an admission record. All changes are recorded in the system's audit log for compliance and accountability.

---

## When to Use This Feature

Use this feature when:
- An admission (BHT) was accidentally created under the wrong patient
- Patient identification was corrected after admission was created
- Administrative errors need to be corrected

**Do NOT use this feature for:**
- Transferring billing records between patients
- Changing patient demographics (use Edit Admission Details instead)
- Room transfers (use Room Change feature instead)

---

## Prerequisites

### Required Permissions
- You must have the **InwardAdmissionsEditAdmission** privilege to access this feature

### Before You Begin
Make sure you have:
1. The correct BHT (admission) number that needs to be changed
2. The new patient's information (name, phone number, or PHN)
3. Verification that this is the correct action to take
4. Authorization from your supervisor (if required by your institution)

---

## How to Access

### Navigation Path
1. Log in to the HMIS system
2. Click on **Inward** in the main menu
3. Click on **Admissions**
4. Select **Change Patient for Admission**

### Menu Location
```
Main Menu → Inward → Admissions → Change Patient for Admission
```

---

## Step-by-Step Instructions

### Step 1: Search for the Admission

1. On the **Change Patient for Admission** page, you'll see a search field
2. Type one of the following:
   - **BHT Number** (e.g., BHT001234)
   - **Patient Name** (current patient's name)
   - **PHN** (Patient Health Number)

3. The system will show matching admissions with:
   - PHN
   - BHT Number
   - Current Patient Name
   - Room
   - Status (Active or Discharged)

4. Click on the correct admission from the dropdown list
5. Click the **Load Admission** button

### Step 2: Review Current Patient Details

Once the admission is loaded, you'll see three panels:

#### Admission Details Panel
- **BHT Number**: The admission number
- **Admission Date**: When the patient was admitted
- **Consultant**: The assigned consultant

#### Current Patient Panel (Read-Only)
This shows the patient currently assigned to the admission:
- Name
- PHN (Patient Health Number)
- Gender
- Date of Birth
- Age
- Mobile Number
- NIC (National ID)
- Address

⚠️ **Verify**: Make sure this is indeed the wrong patient before proceeding.

#### Information Panel
Read the instructions and warnings carefully before proceeding.

### Step 3: Select the New Patient

1. In the **Select New Patient** panel, you have two options:

   **Option A: Quick Search by Phone Number**
   - Enter the patient's phone number in the search field
   - Click the search icon (🔍)
   - If multiple patients are found, select the correct one from the list
   - Click **Select**

   **Option B: Add New Patient**
   - Click the **Add New Patient** button (➕)
   - Fill in the new patient's details
   - Click **Save** to create the patient record
   - The new patient will be automatically selected

2. Review the selected patient's details carefully

3. Click **Prepare to Change Patient** button

### Step 4: Confirm the Change

A confirmation panel will appear on the right side showing:

#### Warning Message
⚠️ **Warning**: You are about to change the patient associated with this admission. This action will be recorded in the audit log.

#### Change Summary

**Current Patient (Will be removed)**
- Shows the patient currently assigned to the admission
- Highlighted in red

**New Patient (Will be assigned)**
- Shows the patient who will be assigned to the admission
- Highlighted in green

#### Review Carefully:
- ✓ BHT Number is correct
- ✓ Current patient is the one you want to remove
- ✓ New patient is the one you want to assign
- ✓ Patient IDs and PHNs match your records

### Step 5: Final Confirmation

1. Read all the information in the confirmation panel
2. If everything is correct, click **Confirm & Change Patient**
3. A JavaScript confirmation dialog will appear asking:
   > "Are you absolutely sure you want to change the patient for this admission? This action will be permanently recorded in the audit log."

4. Click **OK** to proceed or **Cancel** to abort

### Step 6: Success

- A success message will appear confirming the change
- The message will show:
  - BHT Number
  - New patient's name
- You can now search for another admission or exit the page

---

## Important Notes

### What Happens After Changing the Patient

✓ **The admission record is reassigned** to the new patient
✓ **All future billing** will be associated with the new patient
✓ **The change is logged** in the audit trail
✓ **The original patient** is no longer linked to this admission

⚠️ **What Does NOT Change:**
- Past billing records remain with the original patient
- Medical records stay with the original patient
- Room assignments are not affected
- Consultant assignments remain the same

### Audit Trail

Every patient change is recorded with:
- Date and time of change
- User who made the change
- Original patient details
- New patient details
- BHT number

This information can be viewed by authorized users in the **Audit Events** section.

---

## Troubleshooting

### Problem: "No admission to edit" error

**Solution**:
- Make sure you selected an admission from the dropdown
- Click the **Load Admission** button after selecting

### Problem: "Please select a new patient" error

**Solution**:
- Use the quick search to find a patient
- Make sure you clicked **Select** after finding the patient
- Or create a new patient using the **Add New Patient** button

### Problem: "The selected patient is the same as the current patient"

**Solution**:
- You cannot change a patient to the same patient
- Verify you selected the correct new patient
- If you need to edit the current patient's details, use **Edit Admission Details** instead

### Problem: Menu item is not visible

**Solution**:
- Contact your system administrator
- You may not have the required privilege (**InwardAdmissionsEditAdmission**)
- Request access if needed for your role

### Problem: Cannot find the new patient

**Solution**:
- Verify the phone number is correct
- Try searching with a different phone number
- Use **Add New Patient** to create a new patient record
- Check if the patient exists in **Patient Lookup & Registration**

---

## Best Practices

### Before Making Changes

1. ✓ **Verify the mistake**: Confirm with your supervisor that the patient needs to be changed
2. ✓ **Document the reason**: Keep a note of why the change was necessary
3. ✓ **Check billing**: Review if any bills have been generated
4. ✓ **Notify stakeholders**: Inform the nursing staff, billing department, and consultants

### After Making Changes

1. ✓ **Verify the change**: Check the admission in **Edit Admission Details** to confirm
2. ✓ **Update records**: Inform relevant departments of the change
3. ✓ **Document**: Record the change in your department's log book
4. ✓ **Follow up**: Ensure billing and medical records are handled appropriately

### Security

- 🔒 Only use this feature when absolutely necessary
- 🔒 Never change patients to hide mistakes
- 🔒 All changes are permanently logged and auditable
- 🔒 Misuse may result in disciplinary action

---

## Frequently Asked Questions (FAQ)

### Q1: Can I change the patient for a discharged admission?

**A**: Yes, the system allows changing patients for discharged admissions. However, consult with your billing department first as this may affect final bills.

### Q2: Will the old patient's bills transfer to the new patient?

**A**: No. Existing bills remain with the original patient. Only the admission record and future billing will be associated with the new patient.

### Q3: Can I undo a patient change?

**A**: Yes, you can use the same feature to change the patient back. However, both changes will be recorded in the audit log.

### Q4: What if I select the wrong new patient by mistake?

**A**: Use the feature again to change back to the correct patient. Both actions will be logged.

### Q5: How long does the change take to reflect in the system?

**A**: The change is immediate. As soon as you click **Confirm & Change Patient**, the admission is reassigned.

### Q6: Who can see the audit log for patient changes?

**A**: Only users with audit viewing privileges can access the audit events. Contact your system administrator for access.

### Q7: Can I change patients for multiple admissions at once?

**A**: No. Each admission must be changed individually to ensure accuracy and proper verification.

### Q8: What happens to the room assignment when I change the patient?

**A**: Room assignments remain unchanged. The new patient is now in the same room as the old patient was assigned to.

---

## Related Features

- **Edit Admission Details**: For editing admission information, dates, consultants, etc.
- **Patient Lookup & Registration**: For finding or creating patient records
- **Room Change**: For transferring patients between rooms
- **Audit Events**: For viewing the history of changes

---

## Support

If you encounter any issues or have questions:

1. **Contact your department supervisor** for guidance on when to use this feature
2. **Contact your IT Help Desk** for technical issues
3. **Report bugs** through your institution's issue tracking system

---

## Document Information

- **Feature Name**: Change Patient for Admission
- **Version**: 1.0
- **Last Updated**: November 2025
- **Applies To**: HMIS Inward Module
- **Related Issue**: #16638

---

**Document Status**: Active
**Review Date**: To be determined by your institution

---

*This documentation is provided as-is and should be customized according to your institution's specific policies and procedures.*
