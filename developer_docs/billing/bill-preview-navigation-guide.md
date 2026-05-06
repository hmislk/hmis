# Bill Preview and Navigation Implementation Guide

## Overview

This guide explains how to implement bill preview/reprint functionality in HMIS using the atomic bill type routing system. The system automatically routes users to the correct bill preview page based on the bill's atomic type, ensuring consistent navigation across all modules.

## Core Concepts

### 1. Atomic Bill Type Routing

The HMIS system uses `BillTypeAtomic` enum values to determine the correct preview/reprint page for each bill type. This approach:

- **Centralizes navigation logic** in `BillSearch.navigateToViewBillByAtomicBillType()`
- **Ensures consistency** across all modules
- **Simplifies maintenance** by having a single source of truth
- **Supports all bill types** (pharmacy, OPD, channeling, etc.)

### 2. Navigation Flow

```
User clicks "View Bill" button
    ↓
Pass Bill ID to navigation method
    ↓
Fetch Bill from database
    ↓
Identify BillTypeAtomic
    ↓
Route to appropriate reprint page
    ↓
Display bill with print options
```

## Implementation Pattern

### Frontend (XHTML)

#### Basic Implementation

```xhtml
<!-- In a data table action column -->
<p:column headerText="Action">
    <p:commandButton
        id="btnViewBill"
        title="View Bill"
        ajax="false"
        class="mx-1"
        icon="pi pi-eye"
        action="#{billSearch.navigateToViewBillByAtomicBillTypeByBillId(row.bill.id)}" />
</p:column>
```

#### Using Bill Item ID

When your report displays bill items instead of bills:

```xhtml
<p:commandButton
    id="btnViewBill"
    title="View Bill"
    ajax="false"
    icon="pi pi-eye"
    action="#{billSearch.navigateToViewBillByAtomicBillTypeByBillItemId(row.billItem.id)}" />
```


### Backend (Java)

The navigation system is already implemented in `BillSearch.java`. You don't need to write backend code unless you're:
1. Adding a new bill type
2. Creating a new reprint page

#### Entry Point Methods

**Method 1: Navigate by Bill ID**
```java
// Location: BillSearch.java:3870
public String navigateToViewBillByAtomicBillTypeByBillId(Long BillId) {
    if (BillId == null) {
        JsfUtil.addErrorMessage("Bill ID is required");
        return null;
    }

    Bill foundBill = billFacade.find(BillId);
    if (foundBill == null) {
        JsfUtil.addErrorMessage("Bill not found");
        return null;
    }

    this.bill = foundBill;
    return navigateToViewBillByAtomicBillType();
}
```

**Method 2: Navigate by Bill Item ID**
```java
// Location: BillSearch.java:3886
public String navigateToViewBillByAtomicBillTypeByBillItemId(Long BillItemId) {
    if (BillItemId == null) {
        JsfUtil.addErrorMessage("Bill Item ID is required");
        return null;
    }

    BillItem foundBillItem = billItemFacede.find(BillItemId);
    if (foundBillItem == null) {
        JsfUtil.addErrorMessage("Bill Item not found");
        return null;
    }

    if (foundBillItem.getBill() == null) {
        JsfUtil.addErrorMessage("Associated Bill not found");
        return null;
    }

    this.bill = foundBillItem.getBill();
    return navigateToViewBillByAtomicBillType();
}
```


#### Core Routing Logic

```java
// Location: BillSearch.java:3907
public String navigateToViewBillByAtomicBillType() {
    if (bill == null) {
        JsfUtil.addErrorMessage("No Bill is Selected");
        return null;
    }
    if (bill.getBillTypeAtomic() == null) {
        JsfUtil.addErrorMessage("No Bill type");
        return null;
    }

    BillTypeAtomic billTypeAtomic = bill.getBillTypeAtomic();
    loadBillDetails(bill);

    switch (billTypeAtomic) {
        case PHARMACY_RETAIL_SALE:
            pharmacyBillSearch.setBill(bill);
            return pharmacyBillSearch.navigatePharmacyReprintRetailBill();

        case PHARMACY_RETAIL_SALE_CANCELLED:
            pharmacyBillSearch.setBill(bill);
            return pharmacyBillSearch.navigateToViewPharmacyRetailCancellationBill();

        case PHARMACY_ISSUE:
        case PHARMACY_DISPOSAL_ISSUE:
            return navigateToPharmacyIssue();

        case PHARMACY_RECEIVE:
        case PHARMACY_RECEIVE_CANCELLED:
            return navigateToPharmayReceive();

        case PHARMACY_DIRECT_PURCHASE:
            return navigateToDirectPurchaseBillView();

        // ... more cases for different bill types

        default:
            JsfUtil.addErrorMessage("Unknown Bill Type");
            return null;
    }
}
```

## Finding Existing Reprint Pages

Before creating a new reprint page, check if one already exists for your bill type:

### Step 1: Find the Bill Type Atomic Value

Check your bill's `BillTypeAtomic` enum value. Common values include:
- `PHARMACY_RETAIL_SALE`
- `PHARMACY_ISSUE`
- `PHARMACY_RECEIVE`
- `PHARMACY_DIRECT_PURCHASE`
- `PHARMACY_GRN`
- `OPD_BILL_WITH_PAYMENT`
- etc.

### Step 2: Search the Switch Statement

Look in `BillSearch.navigateToViewBillByAtomicBillType()` for your bill type:

```java
case PHARMACY_RETAIL_SALE:
    pharmacyBillSearch.setBill(bill);
    return pharmacyBillSearch.navigatePharmacyReprintRetailBill();
```

### Step 3: Trace the Navigation Method

Follow the navigation method to find the page path:

```java
// In PharmacyBillSearch.java:308
public String navigatePharmacyReprintRetailBill() {
    if (bill == null) {
        JsfUtil.addErrorMessage("No Bill Selected");
    }
    return "/pharmacy/pharmacy_reprint_bill_sale?faces-redirect=true";
}
```

This tells you the reprint page is: `/pharmacy/pharmacy_reprint_bill_sale.xhtml`

## Common Navigation Patterns

### Pattern 1: Direct Navigation

For simple bill types, navigation is direct:

```java
case PHARMACY_DIRECT_PURCHASE:
    return navigateToDirectPurchaseBillView();

// Method implementation
public String navigateToDirectPurchaseBillView() {
    if (bill == null) {
        JsfUtil.addErrorMessage("No Bill is Selected");
        return null;
    }
    loadBillDetails(bill);
    directPurchaseController.setPrintPreview(true);
    directPurchaseController.setPrintBill(bill);
    return "/pharmacy/direct_purchase";
}
```

**Key Points:**
- Set `printPreview = true` on the controller
- Set the bill on the controller (`setPrintBill()`, `setIssuedBill()`, etc.)
- Return the page path without `.xhtml` extension
- No redirect needed when returning to same page

### Pattern 2: Module-Specific Controller Navigation

For complex bill types with module-specific logic:

```java
case PHARMACY_RETAIL_SALE:
    pharmacyBillSearch.setBill(bill);
    return pharmacyBillSearch.navigatePharmacyReprintRetailBill();

// In PharmacyBillSearch.java
public String navigatePharmacyReprintRetailBill() {
    if (bill == null) {
        JsfUtil.addErrorMessage("No Bill Selected");
    }
    return "/pharmacy/pharmacy_reprint_bill_sale?faces-redirect=true";
}
```

**Key Points:**
- Delegate to module-specific controller (`pharmacyBillSearch`, `pharmacySaleController`, etc.)
- Use `faces-redirect=true` for clean URLs
- Module controller handles additional business logic

### Pattern 3: Conditional Navigation Based on Bill Type

Some bills require different pages based on additional criteria:

```java
public String navigateToPharmacyIssue() {
    if (bill == null) {
        JsfUtil.addErrorMessage("No Bill is Selected");
        return null;
    }
    loadBillDetails(bill);

    if (bill.getBillType() == BillType.PharmacyTransferIssue) {
        transferIssueController.setPrintPreview(true);
        transferIssueController.setIssuedBill(bill);
        return "/pharmacy/pharmacy_transfer_issue";
    } else {
        pharmacyIssueController.setBillPreview(true);
        pharmacyIssueController.setPrintBill(bill);
        return "/pharmacy/pharmacy_issue";
    }
}
```

**Key Points:**
- Check additional bill properties if needed (`billType`, status, etc.)
- Route to different pages based on business logic
- Set appropriate flags on controllers

## Creating New Reprint Pages

If a reprint page doesn't exist for your bill type, follow these steps:

### Step 1: Create the Reprint Page

**File:** `/pharmacy/pharmacy_reprint_[billtype].xhtml`

```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:p="http://primefaces.org/ui"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:ph="http://xmlns.jcp.org/jsf/composite/ezcomp/pharmacy">

    <h:body>
        <ui:composition template="/pharmacy/index.xhtml">
            <ui:define name="subcontent">
                <h:form>
                    <!-- Header with action buttons -->
                    <p:panel>
                        <f:facet name="header">
                            <div class="d-flex align-items-center justify-content-between">
                                <div>
                                    <h:outputText value="Bill Preview - #{yourController.bill.deptId}" />
                                </div>
                                <div>
                                    <p:commandButton
                                        value="Print"
                                        ajax="false"
                                        icon="fas fa-print"
                                        class="ui-button-info mx-2">
                                        <p:printer target="printArea" />
                                    </p:commandButton>

                                    <!-- Settings button for printer configuration -->
                                    <p:commandButton
                                        rendered="#{webUserController.hasPrivilege('ChangeReceiptPrintingPaperTypes')}"
                                        value="Settings"
                                        icon="fas fa-cog"
                                        class="ui-button-secondary mx-1"
                                        type="button"
                                        onclick="PF('billTypeConfigDialog').show();" />

                                    <p:commandButton
                                        value="Back"
                                        ajax="false"
                                        icon="fas fa-arrow-left"
                                        action="#{yourController.navigateToList}"
                                        class="ui-button-secondary mx-2" />
                                </div>
                            </div>
                        </f:facet>

                        <!-- Print area with conditional rendering based on paper type settings -->
                        <h:panelGroup id="printArea">
                            <!-- A4 Format -->
                            <h:panelGroup rendered="#{configOptionController.getBooleanValueByKey('Your Bill Type A4 Paper', true)}">
                                <ph:yourBillComposite bill="#{yourController.bill}"/>
                            </h:panelGroup>

                            <!-- POS Format -->
                            <h:panelGroup rendered="#{configOptionController.getBooleanValueByKey('Your Bill Type POS Paper', false)}">
                                <ph:yourBillComposite_pos bill="#{yourController.bill}"/>
                            </h:panelGroup>

                            <!-- Custom Format -->
                            <h:panelGroup rendered="#{configOptionController.getBooleanValueByKey('Your Bill Type Custom Format', false)}">
                                <ph:yourBillComposite_custom bill="#{yourController.bill}"/>
                            </h:panelGroup>
                        </h:panelGroup>
                    </p:panel>
                </h:form>

                <!-- Include printer configuration dialog if needed -->
                <!-- See: developer_docs/configuration/printer-configuration-system.md -->

            </ui:define>
        </ui:composition>
    </h:body>
</html>
```

### Step 2: Reuse Existing Composites

**IMPORTANT:** Always reuse existing composite components for bill display. Do NOT create duplicate composites.

**Example:** If you're creating a reprint page for pharmacy transfer issue:
- Look for existing composite: `resources/pharmacy/transferIssue.xhtml`
- Use the same composite in your reprint page
- Only the page structure and navigation differs

**Benefits of Reusing Composites:**
- Consistent bill formatting across create/edit/reprint pages
- Single source of truth for bill display logic
- Automatic updates when composite is improved
- Printer settings work across all pages

### Step 3: Add Printer Configuration Settings

Follow the printer configuration system to allow users to select paper formats:

```xhtml
<!-- Settings button -->
<p:commandButton
    rendered="#{webUserController.hasPrivilege('ChangeReceiptPrintingPaperTypes')}"
    value="Settings"
    icon="fas fa-cog"
    class="ui-button-secondary mx-1"
    type="button"
    onclick="PF('yourBillTypeConfigDialog').show();" />

<!-- Configuration dialog -->
<p:dialog id="yourBillTypeConfigDialog"
          header="Bill Type Printer Configuration"
          widgetVar="yourBillTypeConfigDialog"
          modal="true"
          width="600">
    <!-- See: developer_docs/configuration/printer-configuration-system.md -->
</p:dialog>
```

**Reference:** See [Printer Configuration System](../configuration/printer-configuration-system.md) for complete implementation details.

### Step 4: Add Navigation Method

Add a navigation method in the appropriate controller:

```java
public String navigateToYourBillTypeReprint() {
    if (bill == null) {
        JsfUtil.addErrorMessage("No Bill is Selected");
        return null;
    }
    loadBillDetails(bill);
    yourController.setPrintPreview(true);
    yourController.setBill(bill);
    return "/pharmacy/pharmacy_reprint_your_bill_type?faces-redirect=true";
}
```

### Step 5: Add Case to Switch Statement

In `BillSearch.navigateToViewBillByAtomicBillType()`, add your bill type:

```java
case YOUR_BILL_TYPE_ATOMIC:
    return navigateToYourBillTypeReprint();
```

## Best Practices

### 1. Always Use Atomic Bill Type Routing

✅ **DO:**
```xhtml
<p:commandButton
    action="#{billSearch.navigateToViewBillByAtomicBillTypeByBillId(row.bill.id)}" />
```

❌ **DON'T:**
```xhtml
<p:commandButton
    action="/pharmacy/pharmacy_reprint_bill_sale?faces-redirect=true" />
```

**Why:** Atomic routing ensures you navigate to the correct page even if bill types change or business logic evolves.

### 2. Check for Existing Reprint Pages

Before creating a new page:
1. Search the switch statement in `BillSearch.navigateToViewBillByAtomicBillType()`
2. Check if your bill type is already handled
3. Verify the target page exists and works correctly
4. Reuse existing pages when possible

### 3. Reuse Existing Composites

✅ **DO:**
```xhtml
<!-- Reuse existing composite -->
<ph:transferIssue bill="#{transferIssueController.issuedBill}"/>
```

❌ **DON'T:**
```xhtml
<!-- Create duplicate composite -->
<ph:transferIssue_reprint bill="#{transferIssueController.issuedBill}"/>
```

**Why:** Duplicating composites creates maintenance burden and inconsistencies.

### 4. Set Print Preview Flags

Always set the `printPreview` flag when navigating to reprint pages:

```java
yourController.setPrintPreview(true);
```

This flag tells the page to:
- Show print buttons instead of save buttons
- Hide editing functionality
- Display the bill in read-only mode
- Show printer configuration settings

### 5. Include Printer Settings

For all pharmacy bill reprint pages, include printer configuration:

```xhtml
<p:commandButton
    rendered="#{webUserController.hasPrivilege('ChangeReceiptPrintingPaperTypes')}"
    value="Settings"
    icon="fas fa-cog"
    class="ui-button-secondary mx-1"
    type="button"
    onclick="PF('configDialog').show();" />
```

**Reference:** [Printer Configuration System](../configuration/printer-configuration-system.md)

### 6. Handle Navigation Errors Gracefully

```java
if (bill == null) {
    JsfUtil.addErrorMessage("No Bill is Selected");
    return null; // Stay on current page
}

if (bill.getBillTypeAtomic() == null) {
    JsfUtil.addErrorMessage("Bill type not found");
    return null;
}
```

### 7. Use Faces Redirect for Clean URLs

```java
return "/pharmacy/pharmacy_reprint_bill_sale?faces-redirect=true";
```

**Why:**
- Creates bookmarkable URLs
- Prevents duplicate form submissions
- Provides better user experience

## Real-World Examples

### Example 1: Goods in Transit Report

**File:** `reports/inventoryReports/good_in_transit.xhtml:487`

```xhtml
<p:column headerText="Action">
    <p:commandButton
        id="btnViewBill"
        title="View"
        ajax="false"
        class="mx-1"
        icon="pi pi-eye"
        action="#{billSearch.navigateToViewBillByAtomicBillTypeByBillId(bi.billItem.bill.id)}" >
    </p:commandButton>
</p:column>
```

**What happens:**
1. User clicks "View" button for a row
2. System gets the bill ID from `bi.billItem.bill.id`
3. `BillSearch` loads the bill and checks its atomic type
4. If bill type is `PHARMACY_ISSUE`, navigates to `/pharmacy/pharmacy_transfer_issue`
5. If bill type is `PHARMACY_RECEIVE`, navigates to `/pharmacy/pharmacy_transfer_receive`
6. Page displays with `printPreview = true` flag
7. User can print using configured paper format

### Example 2: Pharmacy Transfer Issue

**Navigation Method:** `BillSearch.navigateToPharmacyIssue():4492`

```java
public String navigateToPharmacyIssue() {
    if (bill == null) {
        JsfUtil.addErrorMessage("No Bill is Selected");
        return null;
    }
    loadBillDetails(bill);

    if (bill.getBillType() == BillType.PharmacyTransferIssue) {
        transferIssueController.setPrintPreview(true);
        transferIssueController.setIssuedBill(bill);
        return "/pharmacy/pharmacy_transfer_issue";
    } else {
        pharmacyIssueController.setBillPreview(true);
        pharmacyIssueController.setPrintBill(bill);
        return "/pharmacy/pharmacy_issue";
    }
}
```

**Key Points:**
- Checks bill type to determine correct page
- Sets `printPreview = true` flag
- Loads bill into appropriate controller
- Returns to the same page used for creating bills (just in view mode)

### Example 3: Pharmacy Retail Sale Reprint

**Navigation Method:** `PharmacyBillSearch.navigatePharmacyReprintRetailBill():308`

```java
public String navigatePharmacyReprintRetailBill() {
    if (bill == null) {
        JsfUtil.addErrorMessage("No Bill Selected");
    }
    return "/pharmacy/pharmacy_reprint_bill_sale?faces-redirect=true";
}
```

**Reprint Page:** `pharmacy/pharmacy_reprint_bill_sale.xhtml`

Features:
- Multiple paper format options (POS, 5.5", Custom)
- Printer configuration dialog
- Print button with `<p:printer>` tag
- Reuses existing bill composites
- Settings button for paper type selection

## Troubleshooting

### Issue 1: "Bill not found" Error

**Symptom:** Error message when clicking view button

**Causes:**
- Incorrect bill ID being passed
- Bill was deleted from database
- Permission issues preventing access

**Solution:**
```xhtml
<!-- Verify correct property path -->
<p:commandButton
    action="#{billSearch.navigateToViewBillByAtomicBillTypeByBillId(row.billItem.bill.id)}" />

<!-- Check bill exists in debug -->
<h:outputText value="Bill ID: #{row.billItem.bill.id}" />
```

### Issue 2: "Unknown Bill Type" Error

**Symptom:** Error message after bill is loaded

**Causes:**
- Bill type atomic not set in database
- Bill type not handled in switch statement

**Solution:**
1. Check database: `SELECT bill_type_atomic FROM bill WHERE id = ?`
2. Add case to switch statement in `BillSearch.navigateToViewBillByAtomicBillType()`

```java
case YOUR_BILL_TYPE_ATOMIC:
    return navigateToYourBillTypeReprint();
```

### Issue 3: Navigation Returns to Wrong Page

**Symptom:** Clicking view navigates to incorrect page

**Causes:**
- Wrong bill type atomic value
- Incorrect case in switch statement
- Controller state not properly set

**Solution:**
1. Verify bill type atomic in database
2. Check switch statement mapping
3. Verify controller flags are set correctly:

```java
yourController.setPrintPreview(true); // Important!
yourController.setBill(bill);
```

### Issue 4: Bill Displays in Edit Mode Instead of View Mode

**Symptom:** Reprint page shows save/edit buttons instead of print buttons

**Causes:**
- `printPreview` flag not set
- Page doesn't check printPreview flag

**Solution:**
```java
// In navigation method
yourController.setPrintPreview(true);
```

```xhtml
<!-- In XHTML page -->
<p:commandButton
    rendered="#{yourController.printPreview}"
    value="Print"
    icon="fas fa-print">
    <p:printer target="printArea" />
</p:commandButton>

<p:commandButton
    rendered="#{not yourController.printPreview}"
    value="Save"
    action="#{yourController.save}" />
```

### Issue 5: Printer Settings Not Showing

**Symptom:** Settings button not visible on reprint page

**Causes:**
- User doesn't have required privilege
- Settings button not implemented
- Configuration dialog missing

**Solution:**
1. Verify user has privilege: `ChangeReceiptPrintingPaperTypes`
2. Add settings button to page header
3. Implement configuration dialog

**Reference:** [Printer Configuration System](../configuration/printer-configuration-system.md)

## Related Documentation

- [Printer Configuration System](../configuration/printer-configuration-system.md) - How to implement printer settings
- [Page Break Implementation](../ui/page-break-implementation-guide.md) - How to handle page breaks for printing
- [Bill Number Generation Strategy](bill-number-generation-strategy-guide.md) - Understanding bill numbering
- [UI Styling Guidelines](../ui/ui-styling-guidelines.md) - UI standards for pages

## Summary

**To implement bill preview functionality:**

1. **Check if reprint page exists** by searching `BillSearch.navigateToViewBillByAtomicBillType()`
2. **If exists:** Use the navigation methods with bill ID/item ID/dept ID
3. **If not exists:**
   - Create new reprint page
   - Reuse existing composites
   - Add printer configuration
   - Add navigation method
   - Update switch statement

**Key principles:**
- Always use atomic bill type routing
- Reuse existing composites and pages
- Set `printPreview = true` flag
- Include printer configuration settings
- Handle errors gracefully
- Use `faces-redirect=true` for clean URLs

**Example button implementation:**
```xhtml
<p:commandButton
    id="btnViewBill"
    title="View Bill"
    ajax="false"
    icon="pi pi-eye"
    action="#{billSearch.navigateToViewBillByAtomicBillTypeByBillId(row.bill.id)}" />
```

This pattern ensures consistent, maintainable bill preview functionality across the entire HMIS system.
