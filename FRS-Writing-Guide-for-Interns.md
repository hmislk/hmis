# Functional Requirements Specification (FRS) Writing Guide
## A Practical Guide for Software Engineering &amp; BA Interns

---

## Table of Contents

1. [Introduction to FRS](#introduction-to-frs)
2. [Why FRS Matters](#why-frs-matters)
3. [FRS Document Structure](#frs-document-structure)
4. [Writing Each Section](#writing-each-section)
5. [Real Examples from HMIS](#real-examples-from-hmis)
6. [From Requirements to Code](#from-requirements-to-code)
7. [Best Practices](#best-practices)
8. [Common Mistakes to Avoid](#common-mistakes-to-avoid)
9. [Practical Exercises](#practical-exercises)
10. [Resources &amp; Templates](#resources--templates)

---

## Introduction to FRS

### What is an FRS?

A **Functional Requirements Specification (FRS)** is a formal document that describes:
- **WHAT** the system should do (not HOW it does it)
- **WHO** will use the feature
- **WHEN** and **WHY** they need it
- **WHAT** the expected outcomes are

### FRS vs Other Documents

| Document Type | Focus | Audience |
|--------------|-------|----------|
| **FRS** | Functional behavior | Developers, Testers, BAs |
| **User Documentation** | How to use features | End users |
| **Technical Specification** | Implementation details | Developers |
| **System Design Document** | Architecture &amp; structure | Architects, Senior Developers |

### Key Principle

> **An FRS should be complete enough that a developer who has never seen the feature can implement it correctly.**

---

## Why FRS Matters

### For the Business
- ✅ Ensures stakeholder needs are understood
- ✅ Prevents feature creep and scope issues
- ✅ Provides basis for acceptance testing
- ✅ Creates audit trail for regulatory compliance

### For Development Team
- ✅ Clear roadmap for implementation
- ✅ Reduces back-and-forth questions
- ✅ Helps estimate effort accurately
- ✅ Serves as single source of truth

### For Quality Assurance
- ✅ Foundation for test case creation
- ✅ Clear acceptance criteria
- ✅ Validates expected vs actual behavior

### Real Impact: A Case Study

**Scenario**: Adding discount functionality to OPD bills

**Without FRS**:
- Developer assumes discounts can only be applied at billing time
- Implements simple percentage discount
- Business later says they need retroactive discounts with authorization
- Major rework required (3 weeks wasted)

**With FRS**:
- Requirements clearly state need for retroactive discount application
- Authorization workflow specified upfront
- Audit trail requirements documented
- Feature implemented correctly first time

---

## FRS Document Structure

### Essential Sections

```
1. Overview
   └─ Brief description of the feature

2. Business Context
   ├─ Problem Statement
   ├─ Business Value
   └─ Stakeholders

3. Functional Requirements
   ├─ User Roles &amp; Permissions
   ├─ Use Cases
   ├─ Business Rules
   ├─ Data Requirements
   └─ User Interface Requirements

4. Non-Functional Requirements
   ├─ Performance
   ├─ Security
   ├─ Usability
   └─ Compliance

5. User Workflows
   └─ Step-by-step user journeys

6. Acceptance Criteria
   └─ How to validate the feature works

7. Dependencies &amp; Constraints
   ├─ System dependencies
   ├─ Integration points
   └─ Limitations

8. Open Questions &amp; Assumptions
   └─ Things that need clarification
```

---

## Writing Each Section

### 1. Overview Section

**Purpose**: Give readers a quick understanding in 2-3 sentences

**Template**:
```markdown
## Overview

[Feature Name] allows [User Role] to [Primary Action] in order to [Business Goal].
This feature addresses [Problem] by providing [Solution].
```

**Example from HMIS**:
```markdown
## Overview

The **Closing Stock Report** shows the stock position of pharmaceutical items
at the end of a selected date. This report helps pharmacy staff track inventory
levels, calculate stock values for financial reporting, and identify items
nearing expiry.
```

**What Makes This Good**:
- ✅ States WHO uses it (pharmacy staff)
- ✅ States WHAT it does (shows stock position)
- ✅ States WHY it's needed (track inventory, financial reporting, expiry tracking)

---

### 2. Business Context

**Purpose**: Explain the "why" behind the feature

**Key Questions to Answer**:
1. What problem does this solve?
2. Who requested this feature?
3. What's the business impact if we don't build it?
4. How does it fit into the larger system?

**Example**:

```markdown
## Business Context

### Problem Statement
Hospital administrators need to retrospectively apply staff discounts to OPD bills
when the discount was mistakenly not applied at the time of billing. Currently,
there's no way to do this without canceling and recreating the entire bill, which
disrupts financial reporting.

### Business Value
- Reduces billing errors by 40% (based on 6-month audit)
- Eliminates need for bill cancellation (saves 15 min per correction)
- Improves staff satisfaction (resolves discount issues faster)
- Maintains financial audit trail compliance

### Stakeholders
- **Primary**: Hospital Finance Administrators
- **Secondary**: Hospital Directors, Audit Teams
- **Impacted**: OPD Billing Staff, Patients receiving corrections
```

---

### 3. Functional Requirements

#### 3.1 User Roles &amp; Permissions

**Template**:
```markdown
## User Roles &amp; Permissions

| Role | Permission | Can Do | Cannot Do |
|------|------------|--------|-----------|
| [Role Name] | [Permission Code] | [Actions allowed] | [Actions restricted] |
```

**Example**:
```markdown
## User Roles &amp; Permissions

| Role | Permission | Can Do | Cannot Do |
|------|------------|--------|-----------|
| System Administrator | `AdminRetroactiveDiscountApplication` | Apply/change discounts on settled bills | Apply discounts without authorization comment |
| Finance Administrator | `AdminRetroactiveDiscountApplication` | Apply/change discounts with written auth | Apply discounts to cancelled bills |
| Regular Cashier | None | View bills only | Apply retroactive discounts |
```

---

#### 3.2 Use Cases

**Template**:
```markdown
### Use Case: [Use Case Name]

**Actor**: [Who performs this action]
**Preconditions**:
- [What must be true before this can happen]

**Main Flow**:
1. [Step 1]
2. [Step 2]
3. [System does X]
4. [User does Y]

**Alternative Flows**:
- **3a**: If [condition], then [what happens]

**Postconditions**:
- [What's true after success]

**Exceptions**:
- [What can go wrong and how it's handled]
```

**Real Example from HMIS**:

```markdown
### Use Case: Apply Staff Discount Retroactively

**Actor**: Finance Administrator

**Preconditions**:
- Bill exists and is of type "OPD Bill with Payment"
- Bill is not cancelled or refunded
- Written authorization from Hospital Director exists

**Main Flow**:
1. Administrator searches for bill by bill number
2. System displays bill details with current discount
3. Administrator clicks "Apply Discount" button
4. System displays current bill values and discount options
5. Administrator selects new discount scheme (e.g., "Staff 50% Discount")
6. Administrator clicks "Calculate Preview"
7. System calculates and displays:
   - Current totals (Total, Discount, Net)
   - New totals with selected discount
   - Difference amounts
8. Administrator reviews preview values
9. Administrator enters authorization details:
   - Request reference number
   - Authorization date
   - Authorizing person's name
   - Reason for change
10. Administrator clicks "Apply Discount Scheme"
11. System shows confirmation dialog
12. Administrator confirms
13. System:
    - Updates all bill fees with new discount
    - Recalculates bill totals
    - Adds authorization comment to bill
    - Creates audit log entry
    - Shows success message

**Alternative Flows**:
- **5a**: If no discount scheme selected:
  - System shows error "Please select a discount scheme"
  - Return to step 5
- **9a**: If authorization comment is empty:
  - System shows error "Authorization details are mandatory"
  - Return to step 9

**Postconditions**:
- Bill totals updated with new discount
- Audit trail created
- Authorization comment recorded

**Exceptions**:
- Bill is cancelled → System shows error, process stops
- Bill is refunded → System shows error, process stops
- User lacks permission → Button not visible
```

---

#### 3.3 Business Rules

**Purpose**: Define the logic that governs the feature

**Template**:
```markdown
## Business Rules

### Rule ID: BR-[Number]
**Rule**: [Statement of the rule]
**Rationale**: [Why this rule exists]
**Examples**:
- ✅ Valid: [Example of valid scenario]
- ❌ Invalid: [Example of invalid scenario]
```

**Example**:

```markdown
## Business Rules

### Rule ID: BR-001
**Rule**: Retroactive discounts can only be applied to bills of type "OPD Bill with Payment"
**Rationale**: Only completed, paid bills should be modified retroactively. Unpaid bills
can have discounts applied during normal payment process.
**Examples**:
- ✅ Valid: OPD Bill #12345 with payment status "Paid" → Allow discount change
- ❌ Invalid: OPD Bill #12346 with payment status "Pending" → Block discount change

### Rule ID: BR-002
**Rule**: Authorization comment must include: Request number, Date, Authorizing person, Reason
**Rationale**: Regulatory compliance requires full audit trail for all financial modifications
**Examples**:
- ✅ Valid: "Req: FIN-2024-056, Date: 15-Nov-2024, Auth: Dr. Smith (Director),
            Reason: Staff discount not applied due to system error"
- ❌ Invalid: "Apply staff discount"

### Rule ID: BR-003
**Rule**: When discount changes, system must recalculate: Bill Total, Discount, Net Total,
         AND Batch Bill Totals
**Rationale**: Individual bills roll up to batch bills. Both must stay synchronized.
**Calculation**:
- New Discount = Item Total × Discount Percentage
- New Net Total = Item Total - New Discount
- Batch Bill Total = Sum of all bills in batch
```

---

#### 3.4 Data Requirements

**Purpose**: Define what data the feature needs

**Template**:
```markdown
## Data Requirements

### Input Data
| Field | Type | Required | Validation | Source |
|-------|------|----------|------------|--------|

### Output Data
| Field | Type | Calculation | Display Location |
|-------|------|-------------|------------------|

### Data to Persist
| Entity | Attributes | Relationships |
|--------|------------|---------------|
```

**Example**:

```markdown
## Data Requirements

### Input Data
| Field | Type | Required | Validation | Source |
|-------|------|----------|------------|--------|
| Bill Number | String | Yes | Must exist in database | User input |
| Discount Scheme | Entity (PaymentScheme) | Yes | Must be active scheme | Dropdown selection |
| Authorization Comment | Text | Yes | Min 50 characters | User input |
| Request Reference | String | Yes | Extracted from comment | User input |

### Output Data
| Field | Type | Calculation | Display Location |
|-------|------|-------------|------------------|
| Current Total | Decimal | Sum(BillFee.grossValue) | Preview panel |
| Current Discount | Decimal | Sum(BillFee.discount) | Preview panel |
| New Total | Decimal | Current Total (unchanged) | Preview panel |
| New Discount | Decimal | Sum(Item × Scheme.discount%) | Preview panel |
| New Net Total | Decimal | New Total - New Discount | Preview panel |
| Difference | Decimal | New Net Total - Current Net Total | Preview panel |

### Data to Persist
| Entity | Attributes Modified | Audit Required |
|--------|-------------------|----------------|
| Bill | total, discount, netTotal, comments | Yes |
| BillFee | grossValue, discount, netValue | Yes |
| BHT (Batch Bill) | total, discount, netTotal | Yes |
| AuditEvent | eventType, oldValue, newValue, user, timestamp | Yes |
```

---

#### 3.5 User Interface Requirements

**Purpose**: Describe the UI without designing it pixel-by-pixel

**Template**:
```markdown
## UI Requirements

### Screen: [Screen Name]

**Layout**:
- [High-level layout description]

**Components**:
1. **[Component Name]**
   - **Type**: [Button/Input/Table/etc]
   - **Label**: "[Label text]"
   - **Behavior**: [What happens when interacted with]
   - **Validation**: [Any validation rules]

**Visibility Rules**:
- [Component X] visible only if [condition]

**User Feedback**:
- Success: [What user sees on success]
- Error: [What user sees on error]
```

**Example**:

```markdown
## UI Requirements

### Screen: Apply Discount to Bill

**Layout**:
- Top section: Bill search and load
- Left panel: Current bill information (read-only)
- Center panel: Discount selection and preview
- Right panel: Authorization and confirmation

**Components**:

1. **Bill Search Field**
   - **Type**: Autocomplete input
   - **Label**: "Search Bill"
   - **Behavior**: Shows suggestions as user types (min 3 characters)
   - **Displays**: Bill Number, Patient Name, Date, Current Total
   - **Validation**: Must select from dropdown (not free text)

2. **Load Admission Button**
   - **Type**: Primary action button
   - **Label**: "Load Admission"
   - **Behavior**: Loads selected bill details into all panels
   - **Enabled**: Only when bill is selected

3. **Current Bill Details Panel**
   - **Type**: Read-only display panel
   - **Shows**:
     - Bill Number (large, prominent)
     - Patient Name
     - Current Payment Scheme (highlighted if exists)
     - Current Total, Discount, Net Total (in table format)
   - **Style**: Light gray background (indicates read-only)

4. **Discount Scheme Dropdown**
   - **Type**: Dropdown select
   - **Label**: "Select Discount Scheme"
   - **Options**: All active PaymentScheme entities of type "Discount"
   - **Display Format**: "[Scheme Name] - [Discount %]"
   - **Validation**: Required before calculating preview

5. **Calculate Preview Button**
   - **Type**: Secondary action button
   - **Label**: "Calculate Preview"
   - **Behavior**: Calculates new totals without saving
   - **Enabled**: Only when discount scheme is selected

6. **Preview Panel**
   - **Type**: Data display panel with comparison table
   - **Visibility**: Hidden until "Calculate Preview" is clicked
   - **Shows**:
     ```
     | Item          | Current Value | New Value | Difference |
     |---------------|---------------|-----------|------------|
     | Total         | $1000         | $1000     | $0         |
     | Discount      | $100          | $500      | +$400      |
     | Net Total     | $900          | $500      | -$400      |
     ```
   - **Color Coding**:
     - Green for decreased amounts (patient benefit)
     - Red for increased amounts (patient owes more)

7. **Authorization Comment Field**
   - **Type**: Multi-line text area
   - **Label**: "Authorization Details (Mandatory)"
   - **Placeholder**: Shows example format
   - **Validation**:
     - Minimum 50 characters
     - Cannot be empty
   - **Size**: 5 rows × full width

8. **Apply Discount Scheme Button**
   - **Type**: Primary action button (Warning style - Orange/Red)
   - **Label**: "Apply Discount Scheme"
   - **Behavior**:
     - Shows JavaScript confirmation dialog
     - On confirm: Saves changes and shows success message
   - **Enabled**: Only after preview is calculated AND comment is filled
   - **Icon**: Warning icon (⚠️)

**Visibility Rules**:
- "Apply Discount" button in Bill Administration page visible only if:
  - Bill type = "OPD Bill with Payment"
  - Bill not cancelled
  - Bill not refunded
  - User has privilege `AdminRetroactiveDiscountApplication`

**User Feedback**:

Success:
```
✅ Success!
Discount scheme applied successfully to Bill #12345
Patient: John Doe
New Net Total: $500.00
```

Error Examples:
```
❌ Error: Please select a discount scheme before calculating preview
❌ Error: Authorization details are mandatory
❌ Error: Please calculate preview before applying discount
❌ Error: Cannot apply discount to cancelled bills
```

**Accessibility**:
- All form fields must have labels
- Error messages must be associated with fields
- Confirmation dialog must be keyboard-accessible
- Color coding must also use icons (not color alone)
```

---

### 4. Non-Functional Requirements

**Purpose**: Define quality attributes, not features

```markdown
## Non-Functional Requirements

### Performance
- **NFR-001**: Bill search autocomplete must return results within 500ms for databases with up to 1M bills
- **NFR-002**: Preview calculation must complete within 2 seconds for bills with up to 100 line items
- **NFR-003**: Discount application must complete within 5 seconds

### Security
- **NFR-004**: All discount changes must create audit log entries
- **NFR-005**: Audit logs must be immutable (no deletion/editing)
- **NFR-006**: Feature access must require explicit permission grant
- **NFR-007**: Authorization comments must be stored encrypted at rest

### Usability
- **NFR-008**: Preview must show clear before/after comparison
- **NFR-009**: Confirmation dialog must prevent accidental clicks (double confirmation)
- **NFR-010**: Error messages must suggest corrective action

### Compliance
- **NFR-011**: Must comply with financial audit requirements (SOX, local regulations)
- **NFR-012**: All financial modifications must have authorization trail
- **NFR-013**: System must support financial period freeze (prevent changes to closed periods)

### Reliability
- **NFR-014**: Failed discount applications must not corrupt bill data
- **NFR-015**: System must use database transactions (all-or-nothing updates)
- **NFR-016**: Must handle concurrent bill updates gracefully
```

---

### 5. User Workflows

**Purpose**: Show the complete user journey with screenshots/mockups

**Template**:
```markdown
## User Workflow: [Workflow Name]

### Step 1: [Step Name]
[Description of what user does]

**What User Sees**:
- [Visual elements]

**What User Does**:
1. [Action 1]
2. [Action 2]

**System Response**:
- [What system does]

[Screenshot or mockup if available]

---

### Step 2: [Next Step]
...
```

**Example**:

```markdown
## User Workflow: Applying Staff Discount Retroactively

### Step 1: Navigate to Bill Administration
User needs to correct a billing error where staff discount wasn't applied.

**What User Sees**:
- OPD module main menu
- "View/Search" submenu with "Bill Search" option

**What User Does**:
1. Clicks "OPD" in main navigation
2. Hovers over "View/Search"
3. Clicks "Bill Search"

**System Response**:
- Bill search page loads
- Empty search field ready for input
- Table showing recent bills

---

### Step 2: Find the Bill
User searches for the specific bill that needs correction.

**What User Sees**:
- Search field with placeholder "Enter Bill Number, Patient Name, or Phone"
- List of recent OPD bills

**What User Does**:
1. Types bill number "OPD-12345" or patient name "John Doe"
2. System shows matching bills in dropdown
3. Clicks on correct bill from suggestions

**System Response**:
- Autocomplete dropdown appears with matches
- Each suggestion shows:
  - Bill Number
  - Patient Name
  - Date
  - Net Total
  - Status

---

### Step 3: Access Bill Administration
User opens the bill administration panel.

**What User Sees**:
- Bill details displayed
- "Admin" button visible (only for authorized users)

**What User Does**:
1. Reviews bill to confirm it's the correct one
2. Clicks "Admin" button

**System Response**:
- Bill Administration page opens
- Multiple tabs appear: Summary, Items, Payments, Comments, Audit
- "Apply Discount" button visible in Summary tab

---

### Step 4: Review Current Bill Information
User verifies current bill state before making changes.

**What User Sees**:
- **Bill Details Panel** (left):
  - Bill Number: OPD-12345
  - Patient: John Doe
  - Date: 2024-11-10
  - Current Scheme: None
- **Current Values**:
  - Total: $1,000.00
  - Discount: $0.00
  - Net Total: $1,000.00

**What User Does**:
1. Verifies this is the correct bill
2. Notes current discount is $0 (none applied)
3. Clicks "Apply Discount" button

**System Response**:
- Apply Discount page loads
- Current bill information displayed (read-only)
- Discount selection panel appears

---

### Step 5: Select and Preview New Discount
User selects appropriate discount and previews the impact.

**What User Sees**:
- Discount Scheme dropdown with options:
  - Staff Discount - 50%
  - Senior Citizen - 10%
  - Student - 15%
- "Calculate Preview" button (disabled until selection)

**What User Does**:
1. Clicks discount dropdown
2. Selects "Staff Discount - 50%"
3. Clicks "Calculate Preview"

**System Response**:
- System calculates new values
- Preview panel appears showing:

```
Current Bill Values:
- Total:        $1,000.00
- Discount:         $0.00
- Net Total:    $1,000.00

New Bill Values:
- Total:        $1,000.00 (unchanged)
- Discount:       $500.00 (+$500.00) 🔴
- Net Total:      $500.00 (-$500.00) 🟢

Impact: Patient will receive $500.00 refund credit
```

---

### Step 6: Provide Authorization
User documents the reason and authorization for this change.

**What User Sees**:
- Authorization Details section
- Multi-line text box with example format shown in placeholder
- Character counter (50 character minimum)

**What User Does**:
1. Types authorization details:
```
Request No: FIN-2024-056
Date: 15-Nov-2024
Authorized by: Dr. Sarah Smith, Hospital Director
Reason: Patient John Doe is a hospital staff member (Employee ID: EMP-1234).
Staff discount (50%) was not applied at time of billing due to employee ID
not being available. Employee has now provided ID card as verification.
Finance Department approval obtained on 14-Nov-2024 (Approval: FD-2024-1123).
```

**System Response**:
- Character counter updates
- "Apply Discount Scheme" button becomes enabled
- Warning icon (⚠️) appears next to button

---

### Step 7: Confirm and Apply
User makes final confirmation and applies the discount.

**What User Sees**:
- Enabled "Apply Discount Scheme" button (orange/warning color)
- Summary of all changes about to be made

**What User Does**:
1. Reviews preview one final time
2. Clicks "Apply Discount Scheme" button
3. Confirmation dialog appears:
   ```
   ⚠️ WARNING

   Are you absolutely sure you want to change the discount for this bill?

   Bill: OPD-12345
   Patient: John Doe
   Current Net Total: $1,000.00
   New Net Total: $500.00

   This action will be permanently recorded in the audit log.

   [Cancel] [Confirm]
   ```
4. Clicks "Confirm"

**System Response**:
- Progress indicator appears briefly
- System updates database:
  - Updates all 15 bill line items with new discount
  - Recalculates bill totals
  - Updates batch bill totals
  - Creates audit log entry
  - Saves authorization comment
- Success message displays:
  ```
  ✅ Success!

  Discount scheme applied successfully to Bill #OPD-12345
  Patient: John Doe
  Previous Net Total: $1,000.00
  New Net Total: $500.00
  Difference: -$500.00 (Patient Refund Credit)

  Audit log entry created.

  [View Bill] [Apply to Another Bill]
  ```

---

### Step 8: Verify Changes
User confirms the changes are correctly applied.

**What User Does**:
1. Clicks "View Bill" button
2. Reviews updated bill

**What User Sees**:
- Bill Administration page with updated values:
  - Total: $1,000.00
  - Discount: $500.00
  - Net Total: $500.00
- Comments section shows authorization details
- Audit tab shows new entry:
  ```
  Event: Admin Retroactive Discount Application
  Date: 2024-11-15 14:23:45
  User: admin@hospital.com
  Old Net Total: $1,000.00
  New Net Total: $500.00
  Authorization: [Full comment text]
  ```

**System Response**:
- Bill now shows "Staff Discount - 50%" as payment scheme
- All financial reports will reflect new totals
- Patient account shows $500 credit balance

---

### Alternative Flow: Error - No Preview Calculated

**Trigger**: User tries to apply discount without clicking "Calculate Preview"

**What Happens**:
1. User selects discount scheme
2. User fills authorization comment
3. User clicks "Apply Discount Scheme" (skipping preview)
4. System shows error:
   ```
   ❌ Error

   Please calculate preview before applying discount.

   Click "Calculate Preview" to see the impact of the discount
   before applying it.

   [OK]
   ```
5. User clicks OK
6. User clicks "Calculate Preview" button
7. Reviews preview
8. Then clicks "Apply Discount Scheme"

---

### Alternative Flow: Error - Missing Authorization

**Trigger**: User tries to apply discount with empty authorization comment

**What Happens**:
1. User selects discount and calculates preview
2. User clicks "Apply Discount Scheme" without filling comment
3. System shows error:
   ```
   ❌ Error

   Authorization details are mandatory.

   Please provide:
   - Request reference number
   - Authorization date
   - Name and designation of authorizing person
   - Detailed reason for discount change

   Minimum 50 characters required.

   [OK]
   ```
4. Authorization field highlighted in red
5. User fills in comment
6. User clicks "Apply Discount Scheme" again
7. Process continues normally
```

---

### 6. Acceptance Criteria

**Purpose**: Define DONE - how to verify the feature works

**Template**:
```markdown
## Acceptance Criteria

### Scenario: [Scenario Name]
**Given** [Initial context]
**When** [Action taken]
**Then** [Expected outcome]
**And** [Additional expected outcome]
```

**Example**:

```markdown
## Acceptance Criteria

### Scenario 1: Successfully Apply Staff Discount
**Given** I am a Finance Administrator
**And** I am on the Bill Administration page for bill "OPD-12345"
**And** The bill is of type "OPD Bill with Payment"
**And** The bill has no discount applied (discount = $0)
**When** I click "Apply Discount" button
**And** I select "Staff Discount - 50%"
**And** I click "Calculate Preview"
**And** I see new net total is $500 (50% of $1000)
**And** I enter authorization comment with required details
**And** I click "Apply Discount Scheme"
**And** I confirm in the dialog
**Then** I see success message "Discount scheme applied successfully"
**And** Bill shows new discount of $500
**And** Bill shows new net total of $500
**And** Batch bill totals are updated accordingly
**And** Audit log contains entry with old and new values
**And** Authorization comment is saved with the bill

### Scenario 2: Prevent Discount on Cancelled Bill
**Given** I am a Finance Administrator
**And** I am viewing a bill that is cancelled
**When** I open the Bill Administration page
**Then** I do not see "Apply Discount" button
**And** I cannot apply retroactive discount

### Scenario 3: Require Authorization Comment
**Given** I am on Apply Discount page
**And** I have selected a discount scheme
**And** I have calculated preview
**When** I click "Apply Discount Scheme" with empty comment
**Then** I see error "Authorization details are mandatory"
**And** The discount is NOT applied
**And** The comment field is highlighted

### Scenario 4: Audit Trail Created
**Given** I successfully applied a retroactive discount
**When** I navigate to "Data Admin" → "Audit" → "Audit Event History"
**And** I search for today's date
**Then** I see audit event "Admin Retroactive Discount Application"
**And** Event shows my username
**And** Event shows bill number "OPD-12345"
**And** Event shows old net total "$1000.00"
**And** Event shows new net total "$500.00"
**And** Event shows authorization comment
**And** Event timestamp matches when I applied discount

### Scenario 5: User Without Permission Cannot Access
**Given** I am logged in as "Regular Cashier" (no admin permission)
**When** I view any OPD bill
**Then** I do not see "Apply Discount" button
**And** I cannot access Apply Discount page via URL
**And** System shows "Access Denied" if I try direct URL

### Scenario 6: Preview Must Be Calculated First
**Given** I am on Apply Discount page
**And** I selected a discount scheme
**When** I click "Apply Discount Scheme" without clicking "Calculate Preview"
**Then** I see error "Please calculate preview before applying discount"
**And** Discount is NOT applied
**And** "Calculate Preview" button is highlighted

### Scenario 7: Concurrent Update Prevention
**Given** User A and User B both load bill "OPD-12345"
**When** User A applies 50% staff discount at 2:00 PM
**And** User B tries to apply 10% senior citizen discount at 2:01 PM
**Then** User B sees error "Bill has been modified by another user"
**And** User B is asked to reload and try again
**And** Only User A's change is saved
**And** Bill has 50% discount (not 10%)
```

---

## Real Examples from HMIS

### Example 1: Admin Feature - Apply Discount

Let's trace how the "Apply Discount to Existing OPD Bill" feature maps to FRS concepts:

#### From User Documentation to FRS

**User Documentation** (wiki-docs/OPD/Admin-Apply-Discount-to-Existing-OPD-Bill.md):
- Written for END USERS (finance administrators)
- Focus: HOW to use the feature
- Step-by-step instructions with screenshots

**What FRS Would Contain** (for developers):
```markdown
## Functional Requirement: FR-OPD-DISC-001

### Requirement
System shall allow authorized users to retroactively apply discount schemes
to OPD bills that have already been settled.

### Business Rules
- BR-001: Only bills of type "OPD Bill with Payment" can be modified
- BR-002: Cancelled and refunded bills cannot be modified
- BR-003: Authorization comment is mandatory (min 50 characters)
- BR-004: All changes must create audit trail
- BR-005: Preview must be calculated before application

### Data Model Changes
```java
@Entity
@Table(name = "bill")
public class Bill {
    // Existing fields...

    @Lob
    @Column(name = "discount_change_comments")
    private String discountChangeComments; // NEW: Authorization trail

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "discount_last_modified")
    private Date discountLastModified; // NEW: Audit timestamp

    @ManyToOne
    @JoinColumn(name = "discount_modified_by")
    private WebUser discountModifiedBy; // NEW: Audit user
}
```

### Calculations
```
For each BillFee in Bill:
  newDiscount = billFee.grossValue × paymentScheme.discountPercent
  newNetValue = billFee.grossValue - newDiscount

Bill level:
  bill.discount = SUM(billFee.discount)
  bill.netTotal = SUM(billFee.netValue)

Batch Bill level (if exists):
  batchBill.discount = SUM(allBills.discount)
  batchBill.netTotal = SUM(allBills.netTotal)
```

### API Specification
```
Endpoint: POST /api/opd/bill/admin/apply-discount

Request Body:
{
  "billId": 12345,
  "paymentSchemeId": 67,
  "authorizationComment": "Request No: FIN-2024-056..."
}

Response Success (200):
{
  "success": true,
  "message": "Discount applied successfully",
  "bill": {
    "id": 12345,
    "billNumber": "OPD-12345",
    "oldNetTotal": 1000.00,
    "newNetTotal": 500.00,
    "difference": -500.00
  }
}

Response Error (400):
{
  "success": false,
  "errorCode": "PREVIEW_NOT_CALCULATED",
  "message": "Please calculate preview before applying discount"
}
```
```

---

### Example 2: Report Feature - Closing Stock

#### From User Docs to FRS

**User Documentation** shows:
- How to generate report
- What filters to use
- How to interpret results

**FRS Would Specify**:

```markdown
## Functional Requirement: FR-PHARM-STOCK-001

### Requirement
System shall provide historical stock position reports as of any selected date,
with aggregation at item or batch level.

### Report Types
1. **Item-Wise Closing Stock**
   - Aggregates all batches for each item
   - Shows total quantity and values

2. **Batch-Wise Closing Stock**
   - Shows individual batch records
   - Includes expiry dates and batch numbers

### Data Retrieval Logic
```sql
-- For Closing Stock as of 2024-11-15:

SELECT
  item.name,
  batch.batchNo,
  batch.expiryDate,
  stock.quantity,
  stock.purchaseRate,
  stock.saleRate
FROM stock
WHERE stock.item = :selectedItem
  AND stock.createdAt = (
    SELECT MAX(s2.createdAt)
    FROM stock s2
    WHERE s2.item = :selectedItem
      AND s2.department = :selectedDepartment
      AND s2.createdAt &lt;= :selectedDate
  )
```

### Scope Levels
| Scope | Badge | Data Source | Calculation |
|-------|-------|-------------|-------------|
| Department | Dept | Single department_stock table | Direct query |
| Institution | Ins | All departments in institution | SUM(department stocks) |
| Total | Tot | All institutions | SUM(all department stocks) |

### Value Calculations
```
Purchase Value = Quantity × Purchase Rate
Sale Value     = Quantity × Retail Rate
Cost Value     = Quantity × Cost Rate
```

### Filtering Rules
- If institution selected: Show only that institution's stock
- If department selected: Show only that department (overrides institution)
- If category selected: Filter items by category
- If item selected: Show only that item
- Consignment checkbox: Include/exclude negative stock items

### Performance Requirements
- Report must load within 10 seconds for 10,000 items
- Export to Excel must complete within 30 seconds
- Database query must use indexes on (item_id, department_id, created_at)
```

---

### Example 3: Administrative Feature - Change Patient

#### From User Docs to FRS

**What Users See**: Step-by-step guide to change patient

**What Developers Need**: Complete technical specification

```markdown
## Functional Requirement: FR-INWARD-ADM-002

### Requirement
System shall allow authorized users to reassign an admission (BHT) from one
patient to another, with complete audit trail.

### Use Case
**Actor**: Inward Administrator
**Goal**: Correct mistaken patient assignment

**Preconditions**:
- User has privilege "InwardAdmissionsEditAdmission"
- BHT exists
- New patient exists (or will be created)

**Main Flow**:
1. User searches for BHT by number/patient
2. System displays current patient details
3. User searches for new patient by phone
4. System shows matching patients
5. User selects correct patient
6. User clicks "Prepare to Change Patient"
7. System shows side-by-side comparison
8. User clicks "Confirm &amp; Change Patient"
9. System shows JavaScript confirmation
10. User confirms
11. System updates BHT.patient_id
12. System creates audit event
13. System shows success message

**Business Rules**:
- BR-001: New patient cannot be same as current patient
- BR-002: BHT must exist
- BR-003: All changes must create audit event
- BR-004: Original patient's bills remain unchanged

### Data Changes
```java
@Entity
@Table(name = "patient_encounter")
public class BHT {
    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient; // This gets updated

    // All other fields remain unchanged:
    // - bhtNo
    // - admittedDate
    // - room
    // - consultant
    // etc.
}
```

### What Changes:
- ✅ BHT.patient → Points to new patient
- ✅ Audit log → New entry created

### What Does NOT Change:
- ❌ Past bills (remain with original patient)
- ❌ BHT number
- ❌ Admission date
- ❌ Room assignment
- ❌ Consultant

### Audit Trail Specification
```java
@Entity
public class AuditEvent {
    private String eventType = "PATIENT_CHANGE_FOR_ADMISSION";
    private String bhtNumber;
    private Long oldPatientId;
    private String oldPatientName;
    private Long newPatientId;
    private String newPatientName;
    private WebUser performedBy;
    private Date performedAt;
}
```

### UI Behavior
```markdown
Confirmation Dialog Text:
"Are you absolutely sure you want to change the patient for this admission?
This action will be permanently recorded in the audit log."

Success Message:
"✅ Patient changed successfully for BHT #{bhtNumber}.
New patient: {newPatientName}"

Error Messages:
- "No admission to edit" → When no BHT loaded
- "Please select a new patient" → When new patient not selected
- "The selected patient is the same as the current patient" → BR-001 violation
```

---

## From Requirements to Code

### How Developers Use FRS

Let's see how a developer translates FRS to implementation:

#### Step 1: Read the FRS

Developer reads:
```markdown
## FR-OPD-DISC-001: Apply Retroactive Discount

### Business Rule BR-003
Authorization comment is mandatory (minimum 50 characters)
```

#### Step 2: Create Validation Method

```java
public class BillController {

    public String applyDiscountScheme() {
        // Validation based on BR-003
        if (authorizationComment == null ||
            authorizationComment.trim().length() &lt; 50) {
            JsfUtil.addErrorMessage("Authorization details are mandatory. "
                + "Please provide at least 50 characters including "
                + "request number, date, authorizing person, and reason.");
            return null;
        }

        // Rest of implementation...
    }
}
```

#### Step 3: Read Data Requirements

FRS says:
```markdown
### Data to Persist
| Entity | Attributes Modified |
|--------|-------------------|
| Bill | total, discount, netTotal, comments |
| BillFee | grossValue, discount, netValue |
```

Developer implements:
```java
public void updateBillWithNewDiscount(Bill bill, PaymentScheme scheme) {
    // Update each bill fee (per data requirements)
    for (BillFee fee : bill.getBillFees()) {
        double newDiscount = fee.getGrossValue() *
                           (scheme.getDiscountPercent() / 100.0);
        fee.setDiscount(newDiscount);
        fee.setNetValue(fee.getGrossValue() - newDiscount);
        billFeeFacade.edit(fee); // Persist
    }

    // Update bill totals (per data requirements)
    bill.setDiscount(calculateTotalDiscount(bill));
    bill.setNetTotal(bill.getTotal() - bill.getDiscount());
    bill.setComments(authorizationComment); // Per requirement

    billFacade.edit(bill); // Persist
}
```

#### Step 4: Read Non-Functional Requirements

FRS says:
```markdown
### NFR-015: System must use database transactions
```

Developer adds:
```java
@Transactional // Ensures all-or-nothing
public void applyDiscount(Bill bill, PaymentScheme scheme) {
    try {
        updateBillFees(bill, scheme);
        updateBill(bill);
        updateBatchBill(bill);
        createAuditLog(bill, scheme);

        // If any step fails, entire transaction rolls back
    } catch (Exception e) {
        // Transaction automatically rolled back
        throw new RuntimeException("Failed to apply discount", e);
    }
}
```

#### Step 5: Read Acceptance Criteria

FRS says:
```markdown
### Scenario 3: Require Authorization Comment
**When** I click "Apply Discount Scheme" with empty comment
**Then** I see error "Authorization details are mandatory"
```

QA Engineer writes test:
```java
@Test
public void testRequireAuthorizationComment() {
    // Setup
    billController.setBill(testBill);
    billController.setSelectedScheme(staffDiscount);
    billController.setAuthorizationComment(""); // Empty!

    // Execute
    String result = billController.applyDiscountScheme();

    // Verify (based on acceptance criteria)
    assertNull(result); // Should not navigate away
    assertTrue(FacesContext.getCurrentInstance()
        .getMessageList()
        .stream()
        .anyMatch(m -&gt; m.getSummary()
            .contains("Authorization details are mandatory")));

    // Verify discount NOT applied
    assertEquals(0.0, testBill.getDiscount(), 0.01);
}
```

---

### FRS as Communication Tool

**Scenario**: Junior developer has question about discount calculation

**Without FRS**:
1. Developer interrupts senior developer
2. Senior explains verbally
3. Developer might misunderstand
4. Implementation might be inconsistent

**With FRS**:
1. Developer reads FRS Calculations section:
```markdown
### Calculations
For each BillFee in Bill:
  newDiscount = billFee.grossValue × paymentScheme.discountPercent
  newNetValue = billFee.grossValue - newDiscount
```
2. Developer implements correctly
3. Senior developer reviews against FRS
4. Consistent implementation across team

---

## Best Practices

### 1. Write for Your Audience

**Bad** (Too technical for stakeholders):
```markdown
The system shall implement a RESTful API endpoint using JAX-RS annotations
to expose bill discount functionality via HTTP POST with JSON payload...
```

**Good** (Clear for everyone):
```markdown
The system shall allow administrators to apply discount schemes to existing
bills through a web interface, with preview before applying.
```

### 2. Be Specific, Not Vague

**Bad**:
```markdown
The system should provide reports.
```

**Good**:
```markdown
The system shall provide two types of stock reports:
1. Item-Wise: Shows aggregated quantity for each item
2. Batch-Wise: Shows individual batches with expiry dates

Reports shall include filters for:
- Date range (required)
- Department (optional)
- Item category (optional)

Reports shall support export to Excel and PDF formats.
```

### 3. Use Examples Liberally

**Bad**:
```markdown
Authorization comment must include all required information.
```

**Good**:
```markdown
Authorization comment must include:
- Request reference number
- Authorization date
- Authorizing person's name and designation
- Detailed reason

Example:
"Request No: FIN-2024-056, Date: 15-Nov-2024,
Authorized by: Dr. Sarah Smith (Hospital Director),
Reason: Staff discount not applied due to employee ID verification delay."
```

### 4. Define Edge Cases

**Bad**:
```markdown
System calculates discount.
```

**Good**:
```markdown
System calculates discount as:
  discount = grossValue × (discountPercent / 100)

Edge cases:
- If grossValue is 0 → discount = 0
- If discountPercent &gt; 100 → show error "Invalid discount percentage"
- If discountPercent &lt; 0 → show error "Discount cannot be negative"
- Round discount to 2 decimal places
- If grossValue is negative → show error "Cannot apply discount to negative amounts"
```

### 5. Link Requirements to Business Value

**Bad**:
```markdown
System shall create audit logs.
```

**Good**:
```markdown
System shall create audit logs for all discount changes.

Business Value:
- Enables regulatory compliance (SOX, local healthcare regulations)
- Provides evidence for internal audits
- Supports dispute resolution
- Tracks user accountability

Audit logs shall be immutable and include:
- User who made change
- Timestamp
- Old and new values
- Authorization details
```

### 6. Use Visual Aids

Include in FRS:
- ✅ Workflow diagrams
- ✅ State machine diagrams
- ✅ Mockups/wireframes
- ✅ Data model diagrams
- ✅ Sequence diagrams for complex interactions

**Example**: State Machine for Bill Discount

```
┌─────────┐
│  Draft  │
└────┬────┘
     │ apply discount
     ▼
┌─────────────┐
│ Preview     │
│ Calculated  │
└────┬────────┘
     │ confirm
     ▼
┌─────────────┐
│  Applied    │ ◄── (Cannot undo, audit logged)
└─────────────┘
```

### 7. Version Your Requirements

```markdown
## Change History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-11-01 | John Doe | Initial version |
| 1.1 | 2024-11-15 | Jane Smith | Added batch bill update requirement |
| 1.2 | 2024-11-20 | John Doe | Clarified authorization comment format |
```

### 8. Mark Priorities

```markdown
## Requirements Priority

### Must Have (P0)
- FR-001: Apply discount to individual bills
- FR-002: Create audit trail
- FR-003: Require authorization

### Should Have (P1)
- FR-004: Update batch bill totals
- FR-005: Prevent concurrent updates

### Nice to Have (P2)
- FR-006: Bulk discount application
- FR-007: Discount reversal workflow
```

### 9. Separate WHAT from HOW

**Bad** (Mixing WHAT with HOW):
```markdown
System shall use a JavaScript confirmation dialog implemented with
PrimeFaces p:confirmDialog component to verify user intent before
saving discount changes to the MySQL database using JPA EntityManager.
```

**Good** (Focus on WHAT):
```markdown
System shall require user confirmation before applying discount changes.
Confirmation dialog shall:
- Display current and new values
- Require explicit "Confirm" action
- Allow user to cancel
- Prevent accidental clicks (no single-click apply)
```

### 10. Define Success Metrics

```markdown
## Success Criteria

This feature will be considered successful when:

1. **Functional Success**:
   - ✅ Finance administrators can apply retroactive discounts
   - ✅ All changes create audit trail
   - ✅ 100% of test scenarios pass

2. **Business Success**:
   - ✅ Reduce billing corrections from 2 hours to 15 minutes (87% improvement)
   - ✅ Eliminate need for bill cancellation (reduce cancellations by 30%)
   - ✅ 100% audit compliance (no missing authorization trails)

3. **User Success**:
   - ✅ Finance staff can use feature without training
   - ✅ Average task completion time &lt; 3 minutes
   - ✅ User satisfaction score &gt; 4/5
```

---

## Common Mistakes to Avoid

### ❌ Mistake 1: Writing Implementation, Not Requirements

**Wrong**:
```markdown
Use PrimeFaces p:autoComplete component with completeMethod pointing to
BillController.searchBills() method that queries database using JPA Criteria API.
```

**Right**:
```markdown
System shall provide autocomplete search for bills.
Search shall accept: Bill number, Patient name, Phone number
Search shall show suggestions after 3 characters typed.
Each suggestion shall display: Bill number, Patient name, Date, Total.
```

### ❌ Mistake 2: Ambiguous Language

**Wrong**:
```markdown
System should probably show some kind of error if the discount is wrong.
```

**Right**:
```markdown
System shall validate discount percentage:
- If &lt; 0: Show error "Discount cannot be negative"
- If &gt; 100: Show error "Discount cannot exceed 100%"
- If not a number: Show error "Please enter a valid discount percentage"
```

### ❌ Mistake 3: Missing Edge Cases

**Wrong**:
```markdown
Calculate discount by multiplying amount by percentage.
```

**Right**:
```markdown
Calculate discount as: amount × (percentage / 100)

Edge cases:
- If amount = 0 → discount = 0
- If percentage = 0 → discount = 0
- If amount is negative → reject with error
- Round result to 2 decimal places
- If calculation results in &gt; amount → cap at amount
```

### ❌ Mistake 4: No Acceptance Criteria

**Wrong**:
```markdown
System shall provide discount functionality.
[End of requirement]
```

**Right**:
```markdown
System shall provide discount functionality.

Acceptance Criteria:
1. Given bill with $100 total, when I apply 50% discount,
   then net total becomes $50
2. Given cancelled bill, when I try to apply discount,
   then system shows error "Cannot modify cancelled bills"
3. Given successful discount application, when I check audit log,
   then I see entry with old and new values
```

### ❌ Mistake 5: Forgetting the "Why"

**Wrong**:
```markdown
Authorization comment is required.
```

**Right**:
```markdown
Authorization comment is required to ensure regulatory compliance.

Healthcare regulations require documented approval for all financial
modifications. The authorization comment provides:
- Legal evidence of approval
- Audit trail for investigators
- Accountability for changes
- Justification for exceptions
```

### ❌ Mistake 6: No Error Scenarios

**Wrong**:
```markdown
User applies discount and sees success message.
```

**Right**:
```markdown
Success Scenario:
User applies valid discount and sees: "✅ Discount applied successfully"

Error Scenarios:
1. Missing authorization → "❌ Authorization details are mandatory"
2. Cancelled bill → "❌ Cannot apply discount to cancelled bills"
3. No preview → "❌ Please calculate preview before applying"
4. Concurrent update → "❌ Bill modified by another user. Please reload."
5. System error → "❌ Failed to apply discount. Please try again."
```

### ❌ Mistake 7: Assuming Knowledge

**Wrong**:
```markdown
Follow the usual audit trail pattern.
```

**Right**:
```markdown
Create audit log entry with:
- Event type: "RETROACTIVE_DISCOUNT_APPLICATION"
- Timestamp: Current date and time (UTC)
- User: Currently logged-in user ID and name
- Bill ID: ID of the bill being modified
- Old values: Previous total, discount, net total
- New values: Updated total, discount, net total
- Comment: Full authorization comment text
- IP Address: User's IP address
- Session ID: User's session ID
```

### ❌ Mistake 8: Inconsistent Terminology

**Wrong**:
```markdown
In section 1: "Users can add discounts..."
In section 2: "The system allows rebates to be applied..."
In section 3: "Price reductions are calculated..."
```

**Right**:
```markdown
Consistently use: "discount" throughout
Include glossary:

### Glossary
- **Discount**: Percentage or fixed amount reduction from gross price
- **Discount Scheme**: Predefined discount configuration (e.g., "Staff 50%")
- **Net Total**: Final amount after discount applied
- **Gross Total**: Original amount before discount
```

### ❌ Mistake 9: No Dependencies Listed

**Wrong**:
```markdown
System applies discounts to bills.
```

**Right**:
```markdown
System applies discounts to bills.

Dependencies:
- Bill must exist (BillController.selectedBill != null)
- Payment scheme must be defined (PaymentSchemeFacade)
- User must have permission (Privilege.AdminRetroactiveDiscountApplication)
- Audit system must be operational (AuditEventFacade)
- Database transaction support must be available

Integration Points:
- Uses BillFacade for bill persistence
- Uses PaymentSchemeFacade for discount configuration
- Uses AuditEventFacade for logging
- Uses WebUserController for current user info
```

### ❌ Mistake 10: No Validation Rules

**Wrong**:
```markdown
User enters authorization comment.
```

**Right**:
```markdown
User enters authorization comment.

Validation Rules:
- Required: Yes (cannot be null or empty)
- Minimum length: 50 characters
- Maximum length: 5000 characters
- Allowed characters: Alphanumeric, spaces, punctuation
- Prohibited content: Script tags, SQL injection patterns
- Format recommendation: Include "Request No:", "Date:", "Authorized by:", "Reason:"
- Validation timing: On blur and on submit
- Error message location: Below input field (red text)
```

---

## Practical Exercises

### Exercise 1: Write an FRS Section

**Scenario**: The hospital wants to add a feature where doctors can view patient lab test history.

**Your Task**: Write the "Use Case" and "Acceptance Criteria" sections.

**Hints**:
- Who is the actor?
- What data do they need to see?
- What filters might they need?
- What are edge cases (patient with no tests, etc.)?

**Template to Fill**:
```markdown
### Use Case: View Patient Lab Test History

**Actor**: ___________

**Preconditions**:
-
-

**Main Flow**:
1.
2.
3.

**Alternative Flows**:
-

**Postconditions**:
-

---

## Acceptance Criteria

### Scenario 1: View Tests for Patient with History
**Given** ___________
**When** ___________
**Then** ___________
**And** ___________
```

---

### Exercise 2: Identify Problems in Requirements

**Bad Requirement**:
```markdown
The system should maybe show a popup or something when the user
does stuff with the bill and it has errors or whatever.
```

**Questions**:
1. What's wrong with this requirement?
2. Rewrite it to be clear and testable

---

### Exercise 3: Convert User Story to FRS

**User Story**:
```
As a pharmacy manager
I want to see which items are expiring soon
So that I can plan discounts or returns
```

**Your Task**: Expand this into a full FRS section including:
- Business context
- Data requirements
- Acceptance criteria

---

### Exercise 4: Spot Missing Edge Cases

**Requirement**:
```markdown
System calculates patient age from date of birth.
Age = Current Year - Birth Year
```

**Question**: What edge cases are missing? List at least 5.

---

### Exercise 5: Write Validation Rules

**Feature**: User registration form for new hospital staff

**Fields**:
- Employee ID
- Email
- Phone Number
- Date of Birth

**Your Task**: Write complete validation rules for each field.

---

## Resources &amp; Templates

### FRS Document Template

```markdown
# Functional Requirements Specification
## [Feature Name]

**Document Version**: 1.0
**Date**: YYYY-MM-DD
**Author**: [Your Name]
**Stakeholders**: [List]
**Status**: Draft / Review / Approved

---

## 1. Overview
[2-3 sentence summary]

---

## 2. Business Context

### 2.1 Problem Statement
[What problem does this solve?]

### 2.2 Business Value
[Why build this?]
- Metric 1: [e.g., Reduce time by X%]
- Metric 2: [e.g., Improve accuracy by Y%]

### 2.3 Stakeholders
- **Primary**: [Main users]
- **Secondary**: [Indirectly impacted]

---

## 3. Functional Requirements

### 3.1 User Roles &amp; Permissions
[Table of roles and permissions]

### 3.2 Use Cases
[Detailed use cases with main and alternative flows]

### 3.3 Business Rules
[Numbered business rules with rationale]

### 3.4 Data Requirements
[Input/Output/Persistence requirements]

### 3.5 UI Requirements
[Screen-by-screen descriptions]

---

## 4. Non-Functional Requirements

### 4.1 Performance
[Response times, throughput]

### 4.2 Security
[Authentication, authorization, audit]

### 4.3 Usability
[User experience requirements]

### 4.4 Compliance
[Regulatory requirements]

---

## 5. User Workflows
[Step-by-step user journeys]

---

## 6. Acceptance Criteria
[Given/When/Then scenarios]

---

## 7. Dependencies &amp; Constraints
[System dependencies, limitations]

---

## 8. Open Questions
[Items needing clarification]

---

## 9. Appendices

### 9.1 Glossary
[Define terms]

### 9.2 References
[Related documents]

---

## Approval

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Business Analyst | | | |
| Product Owner | | | |
| Tech Lead | | | |
```

---

### Quick Reference Checklist

Before submitting your FRS, verify:

**Completeness**:
- ☐ Overview clearly states WHAT, WHO, WHY
- ☐ All user roles defined
- ☐ Main and alternative flows documented
- ☐ Business rules numbered and explained
- ☐ Data requirements specified
- ☐ Acceptance criteria written
- ☐ Error scenarios covered
- ☐ Dependencies listed

**Clarity**:
- ☐ No ambiguous words (maybe, probably, should)
- ☐ Specific numbers (not "fast" but "&lt; 2 seconds")
- ☐ Examples provided for complex rules
- ☐ Glossary defines technical terms

**Testability**:
- ☐ Every requirement has acceptance criteria
- ☐ Success is measurable
- ☐ Edge cases identified

**Consistency**:
- ☐ Same terminology throughout
- ☐ No contradictions
- ☐ Formatting consistent

---

### Useful Phrases

**For Requirements**:
- "The system **shall**..." (mandatory)
- "The system **should**..." (recommended)
- "The system **may**..." (optional)

**For Clarity**:
- "For example, ..."
- "Specifically, ..."
- "In other words, ..."

**For Edge Cases**:
- "If [condition], then [outcome]"
- "When [scenario], the system shall [action]"
- "In the case of [exception], ..."

**For Validation**:
- "Required / Optional / Conditional"
- "Minimum / Maximum"
- "Must / Must not"
- "Allowed values: ..."

---

## Learning Path for Interns

### Week 1: Reading Requirements
- Read 5 existing FRS documents
- Identify patterns
- Note good and bad examples

### Week 2: Small Sections
- Write "Overview" sections
- Write "Business Rules"
- Get feedback from mentor

### Week 3: Complete Use Cases
- Write full use cases with flows
- Include edge cases
- Practice acceptance criteria

### Week 4: Full FRS
- Write complete FRS for small feature
- Review with team
- Iterate based on feedback

### Ongoing:
- Attend requirements review meetings
- Ask questions when requirements are unclear
- Learn from developer questions (those expose gaps in FRS)

---

## Questions to Ask Yourself

When writing any requirement, ask:

1. **Can a developer implement this without asking questions?**
   - If no → Add more details

2. **Can QA test this?**
   - If no → Add acceptance criteria

3. **What could go wrong?**
   - If you can think of edge cases → Document them

4. **Why are we building this?**
   - If unclear → Clarify business value

5. **What happens if [extreme scenario]?**
   - If uncertain → List as open question

---

## Final Tips

### For Business Analysts

- **Listen to users**: They know the problem better than you
- **Question assumptions**: "Is this always true?"
- **Think edge cases**: "What if the patient has no bills?"
- **Validate with stakeholders**: Show them your FRS

### For Software Engineers

- **Read the whole FRS**: Don't just skim
- **Ask questions early**: Don't assume
- **Propose improvements**: You understand technical constraints
- **Update FRS if things change**: Keep it current

### For Everyone

- **FRS is a living document**: Update it as you learn
- **Perfect is the enemy of good**: Start with draft, iterate
- **Collaborate**: Best FRS comes from team input
- **Learn from production**: When bugs occur, was it in the FRS?

---

## Conclusion

A good FRS:
- ✅ Saves development time
- ✅ Reduces defects
- ✅ Improves communication
- ✅ Serves as documentation

A bad FRS:
- ❌ Causes confusion
- ❌ Leads to rework
- ❌ Frustrates everyone
- ❌ Gets ignored

**Your goal**: Write FRS that developers thank you for, not curse at.

---

## Additional Resources

- **HMIS Wiki**: https://github.com/hmislk/hmis/wiki (647 pages of examples)
- **HMIS Source Code**: C:\Development\hmis (see real implementations)
- **HMIS Issues**: https://github.com/hmislk/hmis/issues (see how requirements evolve)

---

**Document Status**: Training Material
**Target Audience**: New Software Engineering &amp; BA Interns
**Last Updated**: December 2025
**Version**: 1.0

---

*Good luck with your FRS writing! Remember: Clarity is kindness to future you.*
