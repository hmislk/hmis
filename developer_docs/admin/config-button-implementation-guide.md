# Config Button Implementation Guide

## Overview

This guide provides step-by-step instructions for implementing a "Config" button on HMIS pages that allows administrators to navigate to the admin configuration interface where they can modify configuration options and privileges used by that specific page.

## Objectives

1. **Deep Analysis**: Systematically find ALL configuration options and privileges used in a page
2. **Config Button**: Add a subtle admin-only Config button to navigate to admin interface
3. **Page Registration**: Register page metadata for the admin configuration system
4. **Documentation**: Create complete documentation of page configurations

## Example Reference Page

**Target Page**: `http://localhost:8080/rh/faces/inward/pharmacy_bill_issue_bht.xhtml`
**Controller**: `PharmacySaleBhtController.java`
**Admin Interface**: `http://localhost:8080/rh/faces/admin/page_configuration_view.xhtml`

## Step 1: Deep Analysis Methodology

### 1.1 Analyze XHTML File

**Search Patterns in XHTML:**

```bash
# Configuration Options (search for these patterns)
configOptionApplicationController.getBooleanValueByKey(
configOptionApplicationController.getShortTextValueByKey(
configOptionApplicationController.getIntegerValueByKey(
configOptionApplicationController.getDoubleValueByKey(
configOptionApplicationController.getLongTextValueByKey(

# Privileges (search for these patterns)
webUserController.hasPrivilege(
webUserController.checkPrivilege(
```

**Example Analysis Process:**

1. Open the XHTML file
2. Search for each pattern above
3. For each match found, record:
   - Line number
   - Configuration key or privilege name
   - What UI element it controls
   - Method parameters and default values

**Actual Findings from `pharmacy_bill_issue_bht.xhtml`:**

```xml
<!-- Line 25: Nursing workbench navigation -->
<h:panelGroup rendered="#{webUserController.hasPrivilege('NursingWorkBench')}">

<!-- Line 121: Admin config button -->
<h:panelGroup rendered="#{webUserController.hasPrivilege('Admin')}">

<!-- Line 250: Medicine identification codes -->
rendered="#{configOptionApplicationController.getBooleanValueByKey('Medicine Identification Codes Used',true)}"

<!-- Line 258: Drug charges visibility -->
rendered="#{webUserController.hasPrivilege('ShowDrugCharges')}"

<!-- Line 393: Rate column -->
<p:column headerText="Rate" rendered="#{webUserController.hasPrivilege('ShowDrugCharges')}">

<!-- Line 398: Value column -->
<p:column headerText="Value" rendered="#{webUserController.hasPrivilege('ShowDrugCharges')}">

<!-- Line 444: Native printer support -->
rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Bill Support for Native Printers')}"

<!-- Line 550: Bill format FiveFiveCustom3 -->
rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Inward Direct Issue Bill is FiveFiveCustom3',true)}"

<!-- Line 556: Bill format POS header -->
rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Inward Direct Issue Bill is PosHeaderPaper',true)}"
```

### 1.2 Analyze Controller Methods

**Deep-Dive Process:**

1. **Identify Main Methods**: Start with public methods and action methods
2. **Follow Method Calls**: For each method, check what other methods it calls
3. **Search Configuration Usage**: Look for configuration option usage in each method
4. **Document Findings**: Record all configuration options and privileges found

**Search Patterns in Java Controller:**

```java
// Configuration Options
configOptionApplicationController.getBooleanValueByKey(
configOptionApplicationController.getShortTextValueByKey(
configOptionApplicationController.getIntegerValueByKey(

// Privileges
webUserController.hasPrivilege(
Privileges.PRIVILEGE_NAME
```

**Actual Controller Analysis from `PharmacySaleBhtController.java`:**

```java
// Line 394: Decimal quantity validation
boolean allowDecimalsUniversally = configOptionApplicationController.getBooleanValueByKey(
    "Allow Quantity in Decimals Universally for all the items", false);

// Line 407: Integer-only quantity validation
boolean mustBeInteger = configOptionApplicationController.getBooleanValueByKey(
    "Enforce Integer Value Quantity Only for " + getPharmacyItem().getName(), false);

// Line 965: Price matrix calculation (admission department)
matrixByAdmissionDepartment = configOptionApplicationController.getBooleanValueByKey(
    "Price Matrix is calculated from Inpatient Department for " + sessionController.getDepartment().getName(), true);

// Line 966: Price matrix calculation (issuing department)
matrixByIssuingDepartment = configOptionApplicationController.getBooleanValueByKey(
    "Price Matrix is calculated from Issuing Department for " + sessionController.getDepartment().getName(), true);

// Line 1139: Allergy checking during dispensing
if (!configOptionApplicationController.getBooleanValueByKey("Check for Allergies during Dispensing")) {
    // Skip allergy check logic
}

// Line 1395-1397: Transfer pricing options
boolean pharmacyTransferIsByPurchaseRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Purchase Rate", false);
boolean pharmacyTransferIsByCostRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Cost Rate", false);
boolean pharmacyTransferIsByRetailRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Retail Rate", true);

// Line 2545: Item search configuration
boolean searchByItemCode = configOptionApplicationController.getBooleanValueByKey(
    "Search Item by Code in " + sessionController.getDepartment().getName(), true);
```

### 1.3 Find Navigation Methods

**Search for methods that navigate to the target page:**

```java
// Search patterns
return "pharmacy_bill_issue_bht"
return "/inward/pharmacy_bill_issue_bht"
"pharmacy_bill_issue_bht.xhtml"
```

**Example Navigation Methods:**

```java
// In PharmacyController.java
public String navigateToPharmacyBhtIssue() {
    // Privilege check
    if (!webUserController.hasPrivilege(Privileges.PharmacyBhtIssue)) {
        return "access_denied";
    }
    return "/inward/pharmacy_bill_issue_bht?faces-redirect=true";
}
```

## Step 2: Configuration Documentation Template

### 2.1 Create Analysis Document

Create a comprehensive list of all findings:

```markdown
## Configuration Options Found (Actual Analysis)

| Option Key | Default Value | Line/Location | Description | Scope |
|------------|---------------|---------------|-------------|-------|
| "Medicine Identification Codes Used" | true | Line 250 (XHTML) | Shows medicine identification codes in autocomplete | APPLICATION |
| "Pharmacy Bill Support for Native Printers" | varies | Line 444 (XHTML) | Enables native printer support for bills | APPLICATION |
| "Pharmacy Inward Direct Issue Bill is FiveFiveCustom3" | true | Line 550 (XHTML) | Uses FiveFiveCustom3 bill format | APPLICATION |
| "Pharmacy Inward Direct Issue Bill is PosHeaderPaper" | true | Line 556 (XHTML) | Uses POS header paper format | APPLICATION |
| "Allow Quantity in Decimals Universally for all the items" | false | Line 394 (Controller) | Allows decimal quantities for all items | APPLICATION |
| "Enforce Integer Value Quantity Only for [Item Name]" | false | Line 407 (Controller) | Forces integer quantities for specific items | APPLICATION |
| "Price Matrix is calculated from Inpatient Department for [Dept]" | true | Line 965 (Controller) | Price calculation based on admission dept | DEPARTMENT |
| "Price Matrix is calculated from Issuing Department for [Dept]" | true | Line 966 (Controller) | Price calculation based on issuing dept | DEPARTMENT |
| "Check for Allergies during Dispensing" | varies | Line 1139 (Controller) | Enables allergy checking during dispensing | APPLICATION |
| "Pharmacy Transfer is by Purchase Rate" | false | Line 1395 (Controller) | Uses purchase rate for transfers | APPLICATION |
| "Pharmacy Transfer is by Cost Rate" | false | Line 1396 (Controller) | Uses cost rate for transfers | APPLICATION |
| "Pharmacy Transfer is by Retail Rate" | true | Line 1397 (Controller) | Uses retail rate for transfers | APPLICATION |
| "Search Item by Code in [Department]" | true | Line 2545 (Controller) | Enables item code search by department | DEPARTMENT |

## Privileges Found (Actual Analysis)

| Privilege Name | Line/Location | Description | UI Element Affected |
|----------------|---------------|-------------|---------------------|
| Admin | Line 121 (XHTML) | Administrative access | Config button visibility |
| NursingWorkBench | Lines 25, 132, 159, 469, 503 (XHTML) | Nursing workbench access | Workbench navigation buttons |
| ShowDrugCharges | Lines 258, 314, 322, 330, 338, 393, 398 (XHTML) | View drug prices and charges | Rate and value columns, price fields |

## Navigation Methods (To Be Identified)

| Method Name | Controller | Description |
|-------------|-----------|-------------|
| action="pharmacy_bill_issue_bht" | Multiple controllers | Direct navigation to page |
| pageAdminController.navigateToPageAdmin() | PageAdminController | Navigate to admin config interface |
```

## Step 3: Implementation

### 3.1 Add Config Button to XHTML

Add this button to the page header (adjust placement for subtle positioning):

```xml
<!-- Config Button (Admin Only) - Place in page header -->
<div class="d-flex justify-content-start mb-2">
    <h:panelGroup rendered="#{webUserController.hasPrivilege('Admin')}">
        <p:commandButton
            value="Config"
            icon="fa fa-cog"
            title="Page Configuration Management"
            action="#{pageAdminController.navigateToPageAdmin('inward/pharmacy_bill_issue_bht')}"
            ajax="false"
            styleClass="ui-button-secondary ui-button-sm p-button-outlined"
            style="font-size: 0.8rem; padding: 0.25rem 0.5rem;">
        </p:commandButton>
    </h:panelGroup>
</div>

<!-- Rest of page content -->
<h:form id="PharmacySaleBhtForm">
    <!-- Existing page content -->
</h:form>
```

**Button Styling Options (choose one):**

```xml
<!-- Option 1: Small outlined button (recommended) -->
styleClass="ui-button-secondary ui-button-sm p-button-outlined"

<!-- Option 2: Text-only link style -->
styleClass="ui-button-link"

<!-- Option 3: Icon-only minimal -->
value=""
styleClass="ui-button-secondary ui-button-sm"
```

### 3.2 Register Page Metadata in Controller

Add these imports to your controller:

```java
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import javax.annotation.PostConstruct;
```

Add injection:

```java
@Inject
PageMetadataRegistry pageMetadataRegistry;
```

Add or modify @PostConstruct method:

```java
@PostConstruct
public void init() {
    // Existing initialization code (if any)

    registerPageMetadata();
}

/**
 * Register page metadata for the admin configuration interface
 */
private void registerPageMetadata() {
    if (pageMetadataRegistry == null) {
        return;
    }

    PageMetadata metadata = new PageMetadata();
    metadata.setPagePath("inward/pharmacy_bill_issue_bht");
    metadata.setPageName("Pharmacy BHT Direct Issue");
    metadata.setDescription("Direct issue of pharmacy items to BHT patients");
    metadata.setControllerClass("PharmacySaleBhtController");

    // Configuration Options (actual findings from analysis)
    metadata.addConfigOption(new ConfigOptionInfo(
        "Medicine Identification Codes Used",
        "Shows medicine identification codes in autocomplete dropdown",
        "Line 250 (XHTML): Autocomplete column visibility",
        OptionScope.APPLICATION
    ));

    metadata.addConfigOption(new ConfigOptionInfo(
        "Pharmacy Bill Support for Native Printers",
        "Enables native printer support for bill printing",
        "Line 444 (XHTML): Print button rendering condition",
        OptionScope.APPLICATION
    ));

    metadata.addConfigOption(new ConfigOptionInfo(
        "Pharmacy Inward Direct Issue Bill is FiveFiveCustom3",
        "Uses FiveFiveCustom3 format for inward direct issue bills",
        "Line 550 (XHTML): Bill preview format selection",
        OptionScope.APPLICATION
    ));

    metadata.addConfigOption(new ConfigOptionInfo(
        "Pharmacy Inward Direct Issue Bill is PosHeaderPaper",
        "Uses POS header paper format for inward direct issue bills",
        "Line 556 (XHTML): Bill preview format selection",
        OptionScope.APPLICATION
    ));

    metadata.addConfigOption(new ConfigOptionInfo(
        "Allow Quantity in Decimals Universally for all the items",
        "Allows decimal quantities for all pharmacy items globally",
        "Line 394 (Controller): Quantity validation logic",
        OptionScope.APPLICATION
    ));

    metadata.addConfigOption(new ConfigOptionInfo(
        "Check for Allergies during Dispensing",
        "Enables allergy checking when dispensing medications",
        "Line 1139 (Controller): Allergy validation in dispensing process",
        OptionScope.APPLICATION
    ));

    metadata.addConfigOption(new ConfigOptionInfo(
        "Pharmacy Transfer is by Purchase Rate",
        "Uses purchase rate for pharmacy transfer calculations",
        "Line 1395 (Controller): Transfer rate calculation method",
        OptionScope.APPLICATION
    ));

    metadata.addConfigOption(new ConfigOptionInfo(
        "Pharmacy Transfer is by Cost Rate",
        "Uses cost rate for pharmacy transfer calculations",
        "Line 1396 (Controller): Transfer rate calculation method",
        OptionScope.APPLICATION
    ));

    metadata.addConfigOption(new ConfigOptionInfo(
        "Pharmacy Transfer is by Retail Rate",
        "Uses retail rate for pharmacy transfer calculations",
        "Line 1397 (Controller): Transfer rate calculation method",
        OptionScope.APPLICATION
    ));

    // Privileges (actual findings from analysis)
    metadata.addPrivilege(new PrivilegeInfo(
        "Admin",
        "Administrative access to system configuration",
        "Line 121 (XHTML): Config button visibility"
    ));

    metadata.addPrivilege(new PrivilegeInfo(
        "NursingWorkBench",
        "Access to nursing workbench functionality and navigation",
        "Lines 25, 132, 159, 469, 503 (XHTML): Workbench navigation buttons"
    ));

    metadata.addPrivilege(new PrivilegeInfo(
        "ShowDrugCharges",
        "View drug prices, rates, and financial charges in pharmacy interfaces",
        "Lines 258, 314, 322, 330, 338, 393, 398 (XHTML): Rate and value fields visibility"
    ));

    // Register the metadata
    pageMetadataRegistry.registerPage(metadata);
}
```

### 3.3 PageAdminController Method

Ensure this method exists in `PageAdminController`:

```java
public String navigateToPageAdmin(String pagePath) {
    // Store the current page path for the admin interface
    this.currentPagePath = pagePath;

    // Navigate to admin configuration view
    return "/admin/page_configuration_view?faces-redirect=true";
}
```

## Step 4: Testing Checklist

### 4.1 Functional Testing

1. **Admin Access**:
   - [ ] Log in as admin user
   - [ ] Verify Config button is visible
   - [ ] Click Config button
   - [ ] Verify navigation to admin interface

2. **Non-Admin Access**:
   - [ ] Log in as non-admin user
   - [ ] Verify Config button is NOT visible

3. **Configuration Display**:
   - [ ] In admin interface, verify all configuration options are listed
   - [ ] Verify all privileges are listed
   - [ ] Verify current values are displayed correctly

### 4.2 Validation Testing

1. **Button Placement**:
   - [ ] Button is subtle and not prominently placed
   - [ ] Button doesn't interfere with main page functionality
   - [ ] Button styling matches application theme

2. **Admin Interface**:
   - [ ] All configuration options can be modified
   - [ ] Changes are saved correctly
   - [ ] Changes take effect on the source page

## Step 5: Documentation Template

### 5.1 Wiki Documentation

Create user documentation in `wiki-docs/` directory:

```markdown
# Pharmacy BHT Direct Issue - Configuration Guide

## Overview
This page allows direct issuing of pharmacy items to BHT (inpatient) patients.

## Configuration Options

### Patient Details Display
**Key**: `Show Patient Details in Pharmacy BHT Issue`
**Default**: Enabled
**Description**: Controls whether patient information panel is displayed

### Rate Information Display
**Key**: `Show Rates in Pharmacy Bills`
**Default**: Enabled
**Description**: Shows rate and value columns for financial information

## Required Privileges

### ShowDrugCharges
**Description**: Required to view drug prices and charges
**Affects**: Rate and value columns visibility

### PharmacyBhtIssue
**Description**: Basic access to BHT issue functionality
**Affects**: Page access

## Accessing Configuration

1. Navigate to the Pharmacy BHT Direct Issue page
2. Click the "Config" button (visible only to administrators)
3. Modify configuration options as needed
4. Save changes - they take effect immediately
```

## Step 6: Advanced Analysis Techniques

### 6.1 Recursive Method Analysis

For complex controllers, use this systematic approach:

```java
// Start with public/action methods
public String settleBill() {
    // Method body
    validatePatient();        // Follow this method
    calculateTotals();        // Follow this method
    processPayment();         // Follow this method
}

private boolean validatePatient() {
    // Check for configuration options here
    if (configOptionApplicationController.getBooleanValueByKey("Patient Phone Required", false)) {
        // Validation logic
    }
    return checkPatientData(); // Follow this method too
}
```

### 6.2 Cross-Reference Analysis

Create a mapping of where each configuration is used:

```markdown
## Configuration Cross-Reference

### "Patient is required in Pharmacy Retail Sale"
- Controller Method: `settleBill()` line 145
- Controller Method: `validateBillData()` line 234
- XHTML Usage: None (controller-only validation)

### "Show Rates in Pharmacy Bills"
- XHTML: Line 123 (rate column)
- XHTML: Line 156 (value column)
- XHTML: Line 189 (total display)
- Controller: Not used
```

## Step 7: Quality Assurance

### 7.1 Code Review Checklist

- [ ] All configuration options documented
- [ ] All privileges documented
- [ ] Button placement is appropriate
- [ ] Admin-only access enforced
- [ ] Navigation works correctly
- [ ] Page metadata is complete
- [ ] Wiki documentation created

### 7.2 Performance Considerations

- [ ] @PostConstruct method executes quickly
- [ ] Page metadata registration doesn't impact page load time
- [ ] Button rendering doesn't affect page performance

## Step 8: Maintenance

### 8.1 When Adding New Configurations

1. Add configuration option to controller
2. Update `registerPageMetadata()` method
3. Update wiki documentation
4. Test admin interface display

### 8.2 When Modifying Existing Features

1. Review if new configuration options are needed
2. Check if privilege requirements changed
3. Update metadata if necessary
4. Update line number references in documentation

## Troubleshooting

### Config Button Not Visible
- Check user has 'Admin' privilege
- Verify XHTML button code is correct
- Check privilege check syntax

### Admin Interface Not Working
- Verify PageAdminController.navigateToPageAdmin() method exists
- Check page path matches exactly (case-sensitive)
- Ensure page metadata is registered correctly

### Configuration Options Not Displaying
- Check PageMetadataRegistry injection
- Verify registerPageMetadata() is called in @PostConstruct
- Check configuration option keys match exactly

## Example Implementation Files

### Required Files to Modify/Create
1. **Target XHTML Page**: Add Config button
2. **Target Controller**: Add page metadata registration
3. **Wiki Documentation**: Create user guide
4. **PageAdminController**: Ensure navigation method exists

### Dependencies
- PageMetadataRegistry bean
- PageAdminController bean
- Admin privilege system
- Configuration option system

---

**Implementation Time Estimate**: 2-4 hours per page
**Maintenance**: Update when page configurations change
**Prerequisites**: Admin privilege system, configuration option system