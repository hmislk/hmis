# PHARMACY TRANSFER/DISBURSEMENT WORKFLOW DOCUMENTATION

**Project:** HMIS (Hospital Management Information System)
**Module:** Pharmacy Transfer/Disbursement
**Purpose:** Comprehensive documentation for pharmacy stock transfers between departments
**Related Reports:** Stock Transfer Report, Good In Transit Report

---

## TABLE OF CONTENTS

1. [Executive Summary](#executive-summary)
2. [Workflow Overview](#workflow-overview)
3. [Bill Type Atomics](#bill-type-atomics)
4. [Entity Structures](#entity-structures)
5. [Controllers & Methods](#controllers--methods)
6. [Quantity Tracking Mechanisms](#quantity-tracking-mechanisms)
7. [Stock Valuation & Financial Recording](#stock-valuation--financial-recording)
8. [Report Analysis](#report-analysis)
9. [Bill & BillItem Relationships](#bill--billitem-relationships)
10. [Technical Implementation Details](#technical-implementation-details)

---

## EXECUTIVE SUMMARY

The Pharmacy Transfer/Disbursement system manages the complete workflow of moving pharmaceutical stock between departments within a healthcare institution. The process involves seven distinct stages:

**TRANSFER REQUEST → FINALIZE → APPROVE → ISSUE → TRANSIT → RECEIVE → COMPLETE**

Each stage creates specific bills with unique BillTypeAtomic values, maintains quantity tracking with partial quantity support, and records financial valuations at multiple rate levels (purchase, cost, retail).

**Key Features:**
- Multi-department stock transfers with approval workflow
- Partial quantity issuing and receiving
- Staff-mediated transit with quantity loss tracking
- Multiple valuation methods (purchase/cost/retail rates)
- Complete audit trail through bill references
- Real-time stock movement tracking

---

## WORKFLOW OVERVIEW

### Stage Flow Diagram

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  1. REQUEST     │────→│  2. FINALIZE    │────→│  3. APPROVE     │
│  (Create)       │     │  (Editable)     │     │  (Lock & Init)  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
         │                        │                        │
         │                        │                        │
    Pre-Bill                 Pre-Bill               Approved Bill
  (Editable)              (Finalized)            (remainingQty set)
         │                        │                        │
         ▼                        ▼                        ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  4. ISSUE       │────→│  5. TRANSIT     │────→│  6. RECEIVE     │
│  (Settle)       │     │  (Staff Hold)   │     │  (Accept)       │
└─────────────────┘     └─────────────────┘     └─────────────────┘
         │                        │                        │
    Issue Bill              Stock w/ Staff            Receive Bill
  (Negative qty)           (Transit state)          (Positive qty)
         │                        │                        │
         ▼                        ▼                        ▼
    Stock moved           Staff responsible          Final stock move
    from source           for transport             to destination
```

### Detailed Stage Description

#### **Stage 1: Transfer Request Creation**
- **Page:** `pharmacy_transfer_request.xhtml`
- **Controller:** `TransferRequestController`
- **Action:** Pharmacist creates request for items from another department
- **Bill Type:** `PHARMACY_TRANSFER_REQUEST_PRE`
- **Key Properties:**
  - `transferRequestBillPre` - holds the pre-bill
  - `toDepartment` - destination department
  - Items and quantities added via `addItem()` method

#### **Stage 2: Request Finalization**
- **Page:** Same as Stage 1 (Finalize button)
- **Controller:** `TransferRequestController`
- **Action:** Request marked as finalized (still editable)
- **Method:** `finalizeTranserRequestPreBill()` at line 678
- **Changes:** Bill status updated, validation performed

#### **Stage 3: Request Approval**
- **Page:** `pharmacy_transfer_request_approval.xhtml`
- **Controller:** `TransferRequestController`
- **Action:** Supervisor approves the finalized request
- **Method:** `approveTransferRequestBill()` at line 295
- **Key Changes:**
  - Creates new bill with `BillTypeAtomic.PHARMACY_TRANSFER_REQUEST`
  - Initializes `remainingQty = qty` for each item (line 361)
  - Sets approval timestamps and user
  - Calculates bill totals via `pharmacyCostingService`

#### **Stage 4: Item Issue**
- **Page:** `pharmacy_transfer_issue.xhtml`
- **Controller:** `TransferIssueForRequestsController`
- **Action:** Source department issues items (full or partial)
- **Method:** `settle()` at line 406
- **Key Operations:**
  - Validates stock availability
  - Creates `PHARMACY_ISSUE` bill
  - Sets `PharmaceuticalBillItem.qty` to **negative** (line 459)
  - Calls `pharmacyBean.deductFromStock()` - removes from source department
  - Calls `pharmacyBean.addToStock()` - adds to issuing staff
  - Updates `issuedPhamaceuticalItemQty` on request items

#### **Stage 5: Transit State**
- **Responsibility:** Staff member carries items to destination
- **Stock Location:** Items held in staff's stock account
- **Tracking:** Issue bill contains staff reference (`toStaff`)
- **Status:** "Goods in Transit" - items physically moving between locations

#### **Stage 6: Item Receive**
- **Page:** `pharmacy_transfer_receive.xhtml`
- **Controller:** `TransferReceiveController`
- **Action:** Destination department receives items (full or partial)
- **Method:** `settle()` at line 435
- **Key Operations:**
  - User enters actual received quantities
  - Creates `PHARMACY_RECEIVE` bill
  - Calls `pharmacyBean.deductFromStock()` - removes from staff
  - Calls `pharmacyBean.addToStock()` - adds to destination department
  - Updates remaining quantities on issue items

#### **Stage 7: Process Complete**
- **Status:** Transfer completed
- **Records:** Complete audit trail through bill references
- **Stock:** Final quantities in destination department
- **Partial Handling:** Any undelivered quantities remain with staff for future processing

---

## BILL TYPE ATOMICS

The system uses specific BillTypeAtomic values to track each stage of the transfer process:

### Pre-Approval Stage

```java
PHARMACY_TRANSFER_REQUEST_PRE(
    "Pharmacy Transfer Request Pre",
    BillCategory.BILL,
    ServiceType.PHARMACY,
    BillFinanceType.NO_FINANCE_TRANSACTIONS,
    PaymentCategory.CREDIT_SPEND,
    BillType.PharmacyTransferRequest
)
```
- **Usage:** Initial request creation and finalization
- **Characteristics:** Editable, no financial impact, preparatory stage

### Approved Request Stage

```java
PHARMACY_TRANSFER_REQUEST(
    "Pharmacy Transfer Request",
    BillCategory.BILL,
    ServiceType.PHARMACY,
    BillFinanceType.NO_FINANCE_TRANSACTIONS,
    PaymentCategory.CREDIT_SPEND,
    BillType.PharmacyTransferRequest
)
```
- **Usage:** Approved and locked request, ready for issuing
- **Characteristics:** Read-only, `remainingQty` initialized

### Issue Stage

```java
PHARMACY_ISSUE(
    "Pharmacy Transfer Issue",
    BillCategory.BILL,
    ServiceType.PHARMACY,
    BillFinanceType.NO_FINANCE_TRANSACTIONS,
    PaymentCategory.NO_PAYMENT,
    BillType.PharmacyIssue
)
```
- **Usage:** Items issued from source department to staff
- **Characteristics:** Negative quantities, stock moved to staff

### Receive Stage

```java
PHARMACY_RECEIVE(
    "Pharmacy Receive",
    BillCategory.BILL,
    ServiceType.PHARMACY,
    BillFinanceType.NO_FINANCE_TRANSACTIONS,
    PaymentCategory.CREDIT_SPEND,
    BillType.PharmacyTransferReceive
)
```
- **Usage:** Items received from staff to destination department
- **Characteristics:** Positive quantities, final stock movement

### Cancellation Types

```java
PHARMACY_TRANSFER_REQUEST_CANCELLED
PHARMACY_ISSUE_CANCELLED
PHARMACY_RECEIVE_CANCELLED
```
- **Usage:** Reversal operations for each stage
- **Characteristics:** `BillCategory.CANCELLATION`

---

## ENTITY STRUCTURES

### Bill Entity
**File:** `/src/main/java/com/divudi/core/entity/Bill.java`

#### Transfer-Specific Properties
```java
// Basic Bill Information
private BillType billType;                    // PharmacyTransferRequest, PharmacyIssue, PharmacyTransferReceive
private BillTypeAtomic billTypeAtomic;        // Specific atomic type for each stage
private List<BillItem> billItems;             // Items being transferred
private String deptId;                        // Department-specific bill number

// Transfer Parties
private Department fromDepartment;            // Source department (issue stage)
private Department toDepartment;              // Destination department (request/receive)
private Staff toStaff;                        // Staff handling transit

// Workflow Tracking
private Date approveAt;                       // When request was approved
private WebUser approveUser;                  // Who approved the request
private Date releasedAt;                      // When items were released/issued

// Bill Relationships - CRITICAL for transfer workflow
private List<Bill> forwardReferenceBills;     // Links to next bills in workflow
private List<Bill> backwardReferenceBills;    // Links to previous bills in workflow
private Bill referenceBill;                   // Primary relationship link

// Financial Summary
private double netTotal;                      // Total transfer value
```

#### Bill Reference Chain
```
Transfer Request ←→ Issue Bill ←→ Receive Bill
      (parent)         (child)       (grandchild)
```

### BillItem Entity
**File:** `/src/main/java/com/divudi/core/entity/BillItem.java`

#### Transfer-Specific Properties
```java
// Quantity Management
private Double qty;                               // Quantity in packs (for AMPP/VMPP items)
private double remainingQty;                      // Quantity not yet issued (set on approval)

// Transfer Tracking - CRITICAL
@Transient
private double issuedPhamaceuticalItemQty;        // Quantity issued from original request (line 184)

// Item Information
private Item item;                                // The pharmaceutical item being transferred
private PharmaceuticalBillItem pharmaceuticalBillItem;  // Detailed pharmaceutical data
private BillItemFinanceDetails billItemFinanceDetails;  // Financial and rate information

// Workflow Relationships
private BillItem referanceBillItem;               // Links to source item in previous bill
private int searialNo;                            // Sequence number within bill
```

#### Key Quantity Properties
- **`qty`**: User-entered quantity in packs (display quantity)
- **`remainingQty`**: Tracks how much of the original request hasn't been issued yet
- **`issuedPhamaceuticalItemQty`**: Running total of issued quantities
- **`PharmaceuticalBillItem.qty`**: Actual quantity in units (calculated: qty × unitsPerPack)

### PharmaceuticalBillItem Entity
**File:** `/src/main/java/com/divudi/core/entity/pharmacy/PharmaceuticalBillItem.java`

#### Transfer-Specific Properties
```java
// Quantities in Units
private double qty;                          // Quantity in units (converted from packs)
private double qtyPacks;                     // Quantity in packs (for reference)

// Rate Information
private double purchaseRate;                 // Purchase rate per unit
private double retailRate;                   // Retail rate per unit
private double costRate;                     // Cost rate per unit

// Calculated Values
private double purchaseValue;                // Total purchase value (qty × purchaseRate)
private double retailValue;                  // Total retail value (qty × retailRate)
private double costValue;                    // Total cost value (qty × costRate)

// Batch Information
private Stock stock;                         // Source stock record
private ItemBatch itemBatch;                 // Batch details (expiry, rates, etc.)
```

#### Critical: Quantity Sign Convention
- **Issue Bills**: `qty` is set to **negative** (line 459 in TransferIssueForRequestsController)
- **Receive Bills**: `qty` is **positive**
- **Request Bills**: `qty` is **positive** (represents requested amount)

### BillItemFinanceDetails Entity
**File:** `/src/main/java/com/divudi/core/entity/BillItemFinanceDetails.java`

#### Rate Hierarchy (Multiple Valuation Methods)
```java
// Primary Rates
BigDecimal purchaseRate;          // Original purchase cost per unit
BigDecimal costRate;              // Internal costing rate per unit
BigDecimal retailSaleRate;        // Retail sale rate per unit
BigDecimal wholesaleRate;         // Wholesale rate per unit

// Line-Level Financial Information
BigDecimal lineGrossRate;         // Line gross rate (before adjustments)
BigDecimal lineNetRate;           // Line net rate (after adjustments)

// Quantities (User Input)
BigDecimal quantity;              // User-entered quantity in packs
BigDecimal freeQuantity;          // Free quantity included
BigDecimal totalQuantity;         // Total including free

// Calculated Values (Multiple Valuation Methods)
BigDecimal valueAtCostRate;       // Total value at cost rate
BigDecimal valueAtRetailRate;     // Total value at retail rate
BigDecimal valueAtPurchaseRate;   // Total value at purchase rate
BigDecimal valueAtWholesaleRate;  // Total value at wholesale rate

// Line Totals
BigDecimal lineGrossTotal;        // Gross value before adjustments
BigDecimal lineNetTotal;          // Net value after adjustments
BigDecimal netTotal;              // Final net value

// Pack Conversion
BigDecimal unitsPerPack;          // Units per pack for conversion
```

---

## CONTROLLERS & METHODS

### TransferRequestController
**File:** `/src/main/java/com/divudi/bean/pharmacy/TransferRequestController.java`

#### Core Properties
```java
private Bill transferRequestBillPre;      // Pre-bill (editable stage)
private Bill bill;                        // Final approved bill
private List<BillItem> billItems;         // Items in the request
private Department toDepartment;          // Destination department
private List<Department> recentToDepartments;  // Recently used destinations
```

#### Key Methods

##### Request Creation
```java
public void processTransferRequest()      // Initialize new request
public void addItem()                     // Add pharmaceutical items
public void removeItem(BillItem billItem) // Remove items from request
```

##### Request Workflow
```java
// Line 678: Finalize request (still editable)
public void finalizeTranserRequestPreBill()

// Line 295: Approve request (creates locked bill)
public void approveTransferRequestBill()

// Line 309: Creates approved bill with proper BillTypeAtomic
private void createNewApprovedTransferRequestBill()
```

##### Critical: Request Approval Process (Lines 295-380)
```java
public void approveTransferRequestBill() {
    // 1. Create new bill with PHARMACY_TRANSFER_REQUEST atomic type
    // 2. Copy all items from pre-bill to approved bill
    // 3. Initialize remainingQty = qty for each item (line 361)
    // 4. Set approval timestamps and user
    // 5. Calculate totals via pharmacyCostingService
    // 6. Persist approved bill
    // 7. Link pre-bill to approved bill via references
}
```

### TransferIssueForRequestsController
**File:** `/src/main/java/com/divudi/bean/pharmacy/TransferIssueForRequestsController.java`

#### Core Properties
```java
private Bill requestedBill;           // The approved transfer request
private Bill issuedBill;              // The issue bill being created
private List<BillItem> billItems;     // Items being issued
private List<Stock> substituteStocks; // Available substitute stocks
```

#### Key Methods

##### Issue Workflow
```java
// Line 129: Load approved request and prepare for issuing
public void navigateToPharmacyIssueForRequestsById()

// Create issue items from request items
public void createRequestIssueBillItems()

// Line 138: Check if request is already fully issued
public boolean isFullyIssued()

// Line 406: Finalize and settle the issue
public void settle()
```

##### Critical: Issue Settlement Process (Lines 406-510)
```java
public void settle() {
    // 1. Validate all items have sufficient stock
    // 2. Validate issued qty ≤ remaining requested qty
    // 3. Remove zero-quantity items
    // 4. For each bill item:
    //    - Check stock availability
    //    - Set PharmaceuticalBillItem.qty to NEGATIVE (line 459)
    //    - Call pharmacyBean.deductFromStock() (source dept)
    //    - Call pharmacyBean.addToStock() (staff member)
    //    - Update BillItem.qty
    // 5. Generate department-specific bill number
    // 6. Persist issue bill to database
    // 7. Update issued quantities on request items
}
```

##### Stock Movement in settle() (Lines 492-495)
```java
pharmacyBean.deductFromStock(stockSelected,
    Math.abs(billItemsInIssue.getPharmaceuticalBillItem().getQty()),
    billItemsInIssue.getPharmaceuticalBillItem().getItemBatch(),
    issuedBill.getFromDepartment());

pharmacyBean.addToStock(stockSelected,
    Math.abs(billItemsInIssue.getPharmaceuticalBillItem().getQty()),
    billItemsInIssue.getPharmaceuticalBillItem().getItemBatch(),
    issuedBill.getToStaff().getDepartment(),
    BillTypeAtomic.PHARMACY_ISSUE);
```

### TransferReceiveController
**File:** `/src/main/java/com/divudi/bean/pharmacy/TransferReceiveController.java`

#### Core Properties
```java
private Bill issuedBill;           // The issue bill being received
private Bill receivedBill;         // The receive bill being created
private List<BillItem> billItems;  // Items being received
```

#### Key Methods

##### Receive Workflow
```java
// Line 143: Initialize receive process from issue bill
public void navigateToRecieveRequest()

// Line 229: Create receive items from issued items
public void generateBillComponent()

// Line 334: Check if issue is already fully received
public boolean isAlreadyReceived()

// Line 361: Calculate remaining quantity available to receive
public double calculateRemainingQtyWithFreshData(BillItem issuedItem)

// Line 435: Finalize and settle the receive
public void settle()
```

##### Critical: Receive Settlement Process (Lines 435-530)
```java
public void settle() {
    // 1. Validate received items exist
    // 2. Check if already fully received
    // 3. Check for over-receiving conditions
    // 4. For each received item:
    //    - Get user-entered quantity from BillItemFinanceDetails
    //    - Convert packs to units based on item type
    //    - Update PharmaceuticalBillItem.qty (positive)
    //    - Update BillItem.qty
    //    - Call pharmacyBean.deductFromStock() (from staff)
    //    - Call pharmacyBean.addToStock() (to destination)
    // 5. Generate department-specific bill number
    // 6. Persist receive bill to database
}
```

##### Remaining Quantity Calculation (Lines 361-390)
```java
private double calculateRemainingQtyWithFreshData(BillItem issuedItem) {
    // 1. Get total issued quantity from PharmaceuticalBillItem
    // 2. Query database for all receive bills linked to this issue
    // 3. Sum all received quantities from those bills
    // 4. Return: totalIssued - totalReceived
    // This prevents over-receiving
}
```

---

## QUANTITY TRACKING MECHANISMS

### Multi-Level Quantity Tracking

The system maintains quantities at multiple levels to support partial transfers and accurate stock accounting:

#### **Level 1: Display Quantities (Packs)**
- **Property:** `BillItem.qty`
- **Purpose:** User-facing quantity in packs
- **Usage:** Display in UI, user input
- **Conversion:** Multiplied by `unitsPerPack` to get units

#### **Level 2: System Quantities (Units)**
- **Property:** `PharmaceuticalBillItem.qty`
- **Purpose:** Internal calculations and stock movements
- **Usage:** Stock deduction/addition operations
- **Sign Convention:** Negative for issues, positive for receives

#### **Level 3: Tracking Quantities**
- **Property:** `BillItem.remainingQty`
- **Purpose:** Track unfulfilled portions of original request
- **Initialization:** Set to `qty` when request is approved (line 361)
- **Updates:** Reduced when items are issued

#### **Level 4: Audit Quantities**
- **Property:** `BillItem.issuedPhamaceuticalItemQty`
- **Purpose:** Running total of issued quantities for audit
- **Scope:** @Transient (calculated field)
- **Usage:** Report generation and validation

### Partial Quantity Flow Example

**Original Request:** 100 units of Paracetamol
```
Request Bill: qty=100, remainingQty=100
```

**First Issue (60 units):**
```
Issue Bill: qty=-60 (negative), remainingQty=40 (on request)
Stock Movement: -60 from source dept, +60 to staff
```

**First Receive (50 units - 10 units lost):**
```
Receive Bill: qty=+50 (positive)
Stock Movement: -50 from staff, +50 to destination dept
Staff Still Holds: 10 units
```

**Second Receive (10 units):**
```
Receive Bill: qty=+10 (positive)
Stock Movement: -10 from staff, +10 to destination dept
Issue Fully Received: remainingQty=0
```

### Quantity Validation Rules

#### At Issue Stage
1. **Stock Availability Check:** `availableStock >= issuingQuantity`
2. **Request Limit Check:** `issuingQuantity <= remainingRequestedQuantity`
3. **Positive Quantity Rule:** No zero or negative quantities allowed
4. **Substitute Handling:** Alternative stock can be selected if primary unavailable

#### At Receive Stage
1. **Over-Receive Prevention:** `receivingQuantity <= issuedQuantity - alreadyReceivedQuantity`
2. **Loss Recording:** System accepts `receivingQuantity < issuedQuantity` (theft/damage)
3. **Multiple Receive Support:** Same issue can have multiple receive transactions
4. **Audit Trail:** All partial receives are recorded with timestamps

### Conversion Logic

#### Pack-to-Unit Conversion
```java
// For AMPP (Actual Medicinal Product Pack) items
double unitsPerPack = item.getDblValue();  // From item definition
double unitsQuantity = packsQuantity * unitsPerPack;

// For VMPP (Virtual Medicinal Product Pack) items
double unitsQuantity = packsQuantity * item.getParent().getDblValue();

// For other items (already in units)
double unitsQuantity = packsQuantity;  // No conversion needed
```

#### Display Logic in UI
```java
// Good in Transit report (lines 394-411)
if (item.class == 'class com.divudi.core.entity.pharmacy.Ampp') {
    // Show: "units (packs x units_per_pack)"
    // Example: "120 (12 packs x 10)"
    displayText = abs(qty * unitsPerPack) + " (" + abs(qty) + " pack x " + unitsPerPack + ")";
} else {
    // Show: "units"
    // Example: "120"
    displayText = abs(pharmaceuticalBillItem.qty);
}
```

---

## STOCK VALUATION & FINANCIAL RECORDING

### Multi-Rate Valuation System

The system supports multiple concurrent valuation methods to meet different financial reporting needs:

#### **Rate Types & Sources**

##### 1. Purchase Rate
- **Source:** `ItemBatch.purcahseRate` (original purchase cost)
- **Usage:** Inventory valuation, cost of goods
- **Property:** `BillItemFinanceDetails.purchaseRate`
- **Value:** `BillItemFinanceDetails.valueAtPurchaseRate`

##### 2. Cost Rate
- **Source:** `ItemBatch.costRate` (internal costing rate)
- **Usage:** Internal cost accounting, department charges
- **Property:** `BillItemFinanceDetails.costRate`
- **Value:** `BillItemFinanceDetails.valueAtCostRate`
- **Conditional Rendering:** Shown only if "Manage Costing" config is enabled

##### 3. Retail Rate
- **Source:** `ItemBatch.retailsaleRate` (patient billing rate)
- **Usage:** Patient charges, revenue calculations
- **Property:** `BillItemFinanceDetails.retailSaleRate`
- **Value:** `BillItemFinanceDetails.valueAtRetailRate`

##### 4. Wholesale Rate
- **Source:** `ItemBatch.wholesaleRate` (bulk sale rate)
- **Usage:** External transfers, bulk sales
- **Property:** `BillItemFinanceDetails.wholesaleRate`
- **Value:** `BillItemFinanceDetails.valueAtWholesaleRate`

#### **Rate Population Process**

When items are added to transfer requests, rates are populated through this hierarchy:

1. **ItemBatch Rates** (if batch is specified)
2. **Item Default Rates** (from item master data)
3. **Last Purchase Rate** (from recent purchase history)
4. **System Default** (zero if no rates found)

#### **Rate Correction During Receive (Lines 280-287)**
```java
// When receiving, rates are corrected from actual ItemBatch
if (itemBatch != null) {
    pbi.setPurchaseRate(itemBatch.getPurcahseRate());
    pbi.setPurchaseValue(itemBatch.getPurcahseRate() * remainingQty);
    pbi.setRetailRate(itemBatch.getRetailsaleRate());
    pbi.setRetailValue(itemBatch.getRetailsaleRate() * remainingQty);
    pbi.setCostRate(itemBatch.getCostRate());
    pbi.setCostValue(itemBatch.getCostRate() * remainingQty);
}
```

### Financial Impact by Transfer Stage

#### **Request & Approval Stages**
- **Financial Impact:** None (`BillFinanceType.NO_FINANCE_TRANSACTIONS`)
- **Purpose:** Planning and authorization only
- **Rates:** Populated for costing estimation

#### **Issue Stage**
- **Financial Impact:** None (internal transfer)
- **Stock Valuation:** Moves value from source department to staff
- **Rate Used:** Typically cost rate for internal transfers
- **Accounting:** No journal entries created

#### **Receive Stage**
- **Financial Impact:** None (internal transfer completion)
- **Stock Valuation:** Moves value from staff to destination department
- **Rate Used:** Same rates as issue (maintained through workflow)
- **Accounting:** No journal entries created

### Transfer Rate Configuration

Organizations can configure transfer rates through system settings:

#### **Option 1: Cost Rate Transfer**
- **Rate:** Internal costing rate
- **Usage:** Most common for internal departments
- **Advantage:** Reflects true internal cost

#### **Option 2: Purchase Rate Transfer**
- **Rate:** Original purchase cost
- **Usage:** When departments are cost centers
- **Advantage:** Actual acquisition cost

#### **Option 3: Retail Rate Transfer**
- **Rate:** Patient billing rate
- **Usage:** Revenue-generating departments
- **Advantage:** Includes markup for services

### Valuation in Reports

#### **Stock Transfer Report**
- **Purchase Value:** Sum of `valueAtPurchaseRate` for all items
- **Cost Value:** Sum of `valueAtCostRate` for all items (if costing enabled)
- **Retail Value:** Sum of `valueAtRetailRate` for all items

#### **Good In Transit Report**
- **Purpose:** Track value of items currently with staff
- **Calculation:** Items issued but not yet received
- **Values:** All three rate types displayed
- **Critical:** Represents potential loss exposure

---

## REPORT ANALYSIS

### Stock Transfer Report
**File:** `/src/main/webapp/reports/inventoryReports/stock_transfer_report.xhtml`
**Controller:** `PharmacyController.createStockTransferReport()`

#### Report Features
1. **Summary Report** - Department-level aggregated transfers
2. **Breakdown Summary** - Hierarchical department breakdown
3. **Detail Report** - Item-level transfer details
4. **Bill Report** - Bill-level transfer summary

#### Key Filter Options
- **Date Range:** From/To dates for transfer period
- **Issuing Filters:** Institution, Site, Department (source)
- **Receiving Filters:** Institution, Site, Department (destination)
- **Item Filter:** Specific pharmaceutical item
- **Transfer Type:** Issue vs Receive perspective
- **Report Type:** Summary/Breakdown/Detail/Bill level

#### Critical UI Elements

##### Multiple Department Selection Logic (Lines 92-255)
The report uses complex conditional rendering for department selection:
```xml
<!-- 4 different selectOneMenu components based on Institution/Site combinations -->
<p:selectOneMenu rendered="#{pharmacyController.fromInstitution eq null and pharmacyController.fromSite eq null}">
<p:selectOneMenu rendered="#{pharmacyController.fromInstitution eq null and pharmacyController.fromSite ne null}">
<p:selectOneMenu rendered="#{pharmacyController.fromInstitution ne null and pharmacyController.fromSite eq null}">
<p:selectOneMenu rendered="#{pharmacyController.fromInstitution ne null and pharmacyController.fromSite ne null}">
```

##### Summary Table with Expandable Rows (Lines 488-675)
- **Main Level:** Department summaries with totals
- **Expanded Level:** Breakdown by transfer categories
- **Pagination:** 10 rows default, configurable
- **Export:** Excel, PDF, Print options
- **Footer:** Grand totals calculation

##### Detail Report Structure (Lines 679-796)
- **Grouped By:** Department (`departmentWiseRows`)
- **Item Level:** Individual transfer item details
- **Links:** Bill number links to reprint pages
- **Reference Bills:** Shows source request bill information
- **Totals:** Department-level subtotals in footer

#### Identified Issues (Multiple Cost/Value Display)
1. **Column Header Inconsistency:** Some value columns missing proper headers
2. **Value Calculation:** Multiple rate values may be showing incorrectly
3. **Footer Totals:** Grand total calculations may be duplicated
4. **Export Functions:** Some export buttons have incorrect target tables

### Good In Transit Report
**File:** `/src/main/webapp/reports/inventoryReports/good_in_transit.xhtml`
**Controller:** `PharmacyReportController.processGoodInTransistReport()`

#### Report Purpose
Shows pharmaceutical items currently in transit between departments (issued but not yet received).

#### Key Filter Options
- **Date Range:** From/To dates for issue period
- **Issuing Filters:** Institution, Site, Department
- **Receiving Filters:** Institution, Site, Department
- **Item Filter:** Specific pharmaceutical item
- **Category Filter:** Pharmaceutical item category
- **Document Status:** Pending/Accepted/Cancelled
- **Transit Staff:** Specific staff member carrying items

#### Report Structure (Lines 361-511)
- **Main Table:** Single level with all transit items
- **Columns:**
  - Bill Number (issue bill)
  - Staff (person carrying items)
  - Item details (name, code)
  - Stock quantity (with pack conversion display)
  - Expiry date
  - From/To departments
  - Issue date
  - Values (retail, purchase, cost)
  - Comments
  - Action buttons (view bill)

#### Key Calculations

##### Stock Display Logic (Lines 387-412)
```xml
<!-- For AMPP items: show "units (packs x units_per_pack)" -->
<h:panelGroup rendered="#{bi.billItem.item.class eq 'class com.divudi.core.entity.pharmacy.Ampp'}">
    <h:outputText value="#{commonFunctionsProxy.abs(bi.billItem.qty * bi.billItem.billItemFinanceDetails.unitsPerPack)}"/>
    <h:outputText value=" ("/>
    <h:outputText value="#{commonFunctionsProxy.abs(bi.billItem.qty)}"/>
    <h:outputText value=" pack x "/>
    <h:outputText value="#{bi.billItem.billItemFinanceDetails.unitsPerPack}"/>
    <h:outputText value=")"/>
</h:panelGroup>

<!-- For non-AMPP items: show units only -->
<h:panelGroup rendered="#{bi.billItem.item.class ne 'class com.divudi.core.entity.pharmacy.Ampp'}">
    <h:outputText value="#{commonFunctionsProxy.abs(bi.billItem.pharmaceuticalBillItem.qty)}"/>
</h:panelGroup>
```

##### Value Calculations (Lines 449-490)
```xml
<!-- All values use abs() function to handle negative quantities in issue bills -->
<h:outputLabel value="#{commonFunctionsProxy.abs(bi.billItem.billItemFinanceDetails.valueAtRetailRate)}"/>
<h:outputLabel value="#{commonFunctionsProxy.abs(bi.billItem.billItemFinanceDetails.valueAtPurchaseRate)}"/>
<h:outputLabel value="#{commonFunctionsProxy.abs(bi.billItem.billItemFinanceDetails.valueAtCostRate)}"/>
```

#### Identified Issues (Multiple Cost/Value Display)
1. **Query Logic:** May be returning incorrect bill items or duplicates
2. **Status Filter:** Document status filter may not be working correctly
3. **Value Display:** Multiple cost/value columns may show same data
4. **Footer Totals:** Controller totals (`totalRetailValue`, `totalPurchaseValue`, `totalCostValue`) may be incorrect

---

## BILL & BILLITEM RELATIONSHIPS

### Reference Chain Structure

The transfer workflow creates a chain of related bills with specific reference relationships:

```
Transfer Request Bill
       ↓ (forwardReferenceBills)
Issue Bill 1 ←→ Issue Bill 2 ←→ Issue Bill N
       ↓ (forwardReferenceBills)
Receive Bill 1 ←→ Receive Bill 2 ←→ Receive Bill N
```

### Reference Properties

#### **Forward References (`forwardReferenceBills`)**
- **Direction:** Parent → Children
- **Usage:** Track what bills came after this one
- **Example:** Request → [Issue1, Issue2, Issue3]

#### **Backward References (`backwardReferenceBills`)**
- **Direction:** Children → Parent
- **Usage:** Track what bill led to this one
- **Example:** Issue1 → [Request]

#### **Primary Reference (`referenceBill`)**
- **Direction:** Single bill relationship
- **Usage:** Direct parent-child link
- **Example:** Issue1.referenceBill = Request

### BillItem Relationships

#### **`referanceBillItem` Property**
Links bill items across the workflow stages:

```java
// Request item
RequestBillItem {
    id: 100,
    qty: 50,
    referanceBillItem: null  // No parent
}

// Issue item (references request item)
IssueBillItem {
    id: 200,
    qty: -30,  // Negative for issue
    referanceBillItem: RequestBillItem(100)  // Links back to request
}

// Receive item (references issue item)
ReceiveBillItem {
    id: 300,
    qty: +25,  // Positive for receive (5 units lost)
    referanceBillItem: IssueBillItem(200)  // Links back to issue
}
```

### Relationship Usage in Reports

#### **Detail Report Bill Links (Lines 692-710)**
```xml
<!-- Main bill number -->
<p:commandLink value="#{i.billItem.bill.deptId}"
               action="/pharmacy/pharmacy_reprint_transfer_isssue">
    <f:setPropertyActionListener target="#{pharmacyBillSearch.bill}" value="#{i.billItem.bill}"/>
</p:commandLink>

<!-- Reference bill number -->
<p:commandLink value="#{i.billItem.bill.backwardReferenceBill.deptId}"
               action="/pharmacy/pharmacy_reprint_transfer_request">
    <f:setPropertyActionListener value="#{i.billItem.bill}" target="#{pharmacyBillSearch.bill}"/>
</p:commandLink>
```

#### **Timestamp Tracking**
```xml
<!-- Current bill date -->
<h:outputText value="#{i.billItem.bill.createdAt}">

<!-- Reference bill date -->
<h:outputText value="#{i.billItem.bill.backwardReferenceBill.createdAt}">
```

### Data Integrity Rules

#### **Referential Integrity**
1. **Issue bills** MUST have `backwardReferenceBills` pointing to request
2. **Receive bills** MUST have `backwardReferenceBills` pointing to issue
3. **Request bills** MUST have `forwardReferenceBills` populated when issues are created

#### **Quantity Consistency**
1. **Sum of issue quantities ≤ Request quantity**
2. **Sum of receive quantities ≤ Issue quantity**
3. **Remaining quantity = Request quantity - Sum of issue quantities**

#### **Orphan Prevention**
1. **No receive bills without corresponding issue bills**
2. **No issue bills without corresponding request bills**
3. **All bill items must have valid `referanceBillItem` except request items**

---

## TECHNICAL IMPLEMENTATION DETAILS

### Controller Architecture

#### **Separation of Concerns**
- **TransferRequestController:** Request creation, finalization, approval
- **TransferIssueForRequestsController:** Item issuing from approved requests
- **TransferReceiveController:** Item receiving from issues
- **PharmacyController:** Reporting for stock transfers
- **PharmacyReportController:** Specialized transit reporting

#### **Session Management**
All controllers are `@SessionScoped` to maintain state across page navigations during the transfer workflow.

### Database Query Patterns

#### **Request-to-Issue Linking**
```sql
-- Find approved requests ready for issuing
SELECT b FROM Bill b
WHERE b.billType = :bt
  AND b.billTypeAtomic = :bta
  AND b.toDepartment = :dept
  AND b.createdAt BETWEEN :from AND :to

-- Parameters:
-- bt = BillType.PharmacyTransferRequest
-- bta = BillTypeAtomic.PHARMACY_TRANSFER_REQUEST
```

#### **Transit Items Query (Good In Transit Report)**
```sql
-- Find items issued but not fully received
SELECT bi FROM BillItem bi
WHERE bi.bill.billType = :bt
  AND bi.bill.billTypeAtomic = :bta
  AND bi.bill.createdAt BETWEEN :from AND :to
  AND bi.bill.fromDepartment = :fromDept
  AND bi.bill.toDepartment = :toDept

-- Parameters:
-- bt = BillType.PharmacyIssue
-- bta = BillTypeAtomic.PHARMACY_ISSUE
```

#### **Remaining Quantity Calculation**
```sql
-- Calculate remaining quantity to receive
SELECT COALESCE(SUM(ABS(rbi.pharmaceuticalBillItem.qty)), 0)
FROM BillItem rbi
WHERE rbi.bill.billType = :receiveType
  AND rbi.referanceBillItem = :issuedItem

-- This sums all previous receives for the issued item
```

### Stock Movement Integration

#### **PharmacyBean Integration**
The transfer controllers integrate with `PharmacyBean` for actual stock movements:

```java
// Deduct from source (issue stage)
pharmacyBean.deductFromStock(
    stockSelected,                              // Stock record
    Math.abs(qty),                             // Quantity (positive)
    itemBatch,                                 // Batch information
    fromDepartment                             // Source department
);

// Add to destination (issue stage - to staff)
pharmacyBean.addToStock(
    stockSelected,                             // Stock record
    Math.abs(qty),                             // Quantity (positive)
    itemBatch,                                 // Batch information
    staffDepartment,                           // Staff's department
    BillTypeAtomic.PHARMACY_ISSUE              // Transaction type
);
```

### Error Handling & Validation

#### **Stock Validation (Issue Stage)**
1. **Availability Check:** Verify sufficient stock before issuing
2. **Request Limit Check:** Cannot issue more than remaining requested quantity
3. **Batch Expiry Check:** Warn if batch is near expiry
4. **Substitute Validation:** Ensure substitute items are equivalent

#### **Receive Validation**
1. **Over-Receive Prevention:** Cannot receive more than issued
2. **Duplicate Check:** Prevent duplicate receives of same issue
3. **Staff Verification:** Ensure staff actually holds the stock
4. **Department Authorization:** Verify receiving department permissions

### Performance Considerations

#### **Lazy Loading**
- **Bill Items:** Loaded only when needed for reports
- **Reference Bills:** Fetched on-demand for drill-down
- **Stock Details:** Loaded during item selection

#### **Caching Strategy**
- **Department Lists:** Cached per session
- **Item Master Data:** Cached with refresh triggers
- **Rate Information:** Cached per item batch

#### **Report Optimization**
- **Pagination:** All reports use pagination to limit initial load
- **Lazy DataTable:** PrimeFaces lazy loading for large datasets
- **Export Optimization:** Separate queries for export vs display

### Security & Audit

#### **Permission Checks**
- **Request Creation:** User must have pharmacy request permissions
- **Approval:** Requires supervisor-level permissions
- **Issue:** Must have issuing department permissions
- **Receive:** Must have receiving department permissions

#### **Audit Trail**
- **Creator Tracking:** Every bill records `creater` and `createdAt`
- **Modifier Tracking:** Updates recorded with `editer` and `editedAt`
- **Approval Tracking:** Separate `approveUser` and `approveAt` fields
- **Complete History:** Full workflow preserved through bill references

### Integration Points

#### **Inventory System**
- **Real-time Stock Updates:** Immediate stock level adjustments
- **Batch Tracking:** Full batch traceability through transfers
- **Expiry Management:** Expiry date tracking maintained

#### **Financial System**
- **No Direct Impact:** Transfers are internal (no journal entries)
- **Valuation Tracking:** Multiple rate types for different reports
- **Cost Center Allocation:** Department-level cost tracking

#### **Reporting System**
- **Real-time Data:** Reports show current state
- **Historical Views:** Audit trail enables historical reporting
- **Export Capabilities:** Excel, PDF, Print integration

---

## CONCLUSION

This comprehensive documentation covers the complete pharmacy transfer/disbursement workflow, providing the technical foundation needed to understand and maintain the system. The workflow's complexity arises from its support for:

1. **Multi-stage approval process** with complete audit trail
2. **Partial quantity handling** with accurate remaining quantity tracking
3. **Staff-mediated transit** with loss recording capabilities
4. **Multiple valuation methods** for different financial reporting needs
5. **Complex reporting** with drill-down capabilities and export options

Understanding these interconnected systems is crucial for maintaining data integrity and supporting the healthcare institution's pharmaceutical logistics needs.

**Key Files for Reference:**
- Controllers: `TransferRequestController.java`, `TransferIssueForRequestsController.java`, `TransferReceiveController.java`
- Entities: `Bill.java`, `BillItem.java`, `PharmaceuticalBillItem.java`, `BillItemFinanceDetails.java`
- Reports: `stock_transfer_report.xhtml`, `good_in_transit.xhtml`
- Enums: `BillTypeAtomic.java`, `BillType.java`

This documentation provides the foundation for fixing the identified "multiple cost/value display issues" in the two reports by understanding how quantities, rates, and relationships flow through the complete transfer workflow.