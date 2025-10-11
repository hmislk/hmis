# Printer Configuration System

## Overview

The Printer Configuration System provides a unified interface for managing paper formats and printer settings across various operations. This system centralizes printer configurations for receipts, bills, and reports, making it easier for administrators to control paper types without code changes.

## Architecture

### Core Components

1. **PharmacyRetailConfigController** (`src/main/java/com/divudi/bean/pharmacy/PharmacyConfigController.java`)
   - Manages all pharmacy printer configuration settings
   - Handles loading and saving of configuration options
   - Provides getter/setter methods for UI binding

2. **ConfigOptionController** (Department-specific configurations)
   - Used for printer-related configurations that vary by department
   - Provides department-specific configuration values
   - Used in rendering conditions: `#{configOptionController.getBooleanValueByKey('key', defaultValue)}`

3. **ConfigOptionApplicationController** (Application-wide configurations)
   - Used for application-wide preferences that don't change by department
   - Used for system-wide settings and business rules
   - Used in rendering conditions: `#{configOptionApplicationController.getBooleanValueByKey('key', defaultValue)}`

4. **Configuration Dialog System**
   - Modal dialogs with standardized UI components
   - Real-time configuration updates
   - Consistent user experience across modules

5. **Settings Button Integration**
   - Permission-based access control
   - Uniform settings button placement at the left
   - Standard icon and styling

## Samples

### Transfer Issue Configuration

**Pages:**
- `pharmacy_transfer_issue.xhtml` - Main transfer issue page
- `pharmacy_reprint_transfer_isssue.xhtml` - Reprint transfer issue page

**Configuration Options:**
- A4 Paper Format (transferIssue component)
- A4 Paper Format Detailed (transferIssue_detailed component)
- POS Paper Format (Pharmacy_department_Issue_pos_bill component)
- POS Paper with Header (saleBill_Header_Transfer component)
- Template Format (pharmacy_transfer_issue_receipt component)

**Configuration Keys (Department-specific via ConfigOptionController):**
- `Pharmacy Transfer Issue A4 Paper` (default: true)
- `Pharmacy Transfer Issue A4 Paper Detailed` (default: false)
- `Pharmacy Transfer Issue POS Paper` (default: false)
- `Pharmacy Transfer Issue Bill is PosHeaderPaper` (default: false)
- `Pharmacy Transfer Issue Bill is Template` (default: false)

**Rendering Example:**
```xhtml
<h:panelGroup rendered="#{configOptionController.getBooleanValueByKey('Pharmacy Transfer Issue A4 Paper', true)}">
    <ph:transferIssue bill="#{transferIssueController.issuedBill}"/>
</h:panelGroup>
```

### GRN Configuration

**Configuration Options:**
- A4 Paper Format
- Custom Format 1
- Custom Format 2

**Configuration Keys:**
- `GRN Receipt Paper is A4`
- `GRN Receipt Paper is Custom 1`
- `GRN Receipt Paper is Custom 2`

### Transfer Receive Configuration

**Configuration Options:**
- A4 Paper Format
- Template Format
- A4 Detailed Format
- A4 Custom 1 Format
- A4 Custom 2 Format

**Configuration Keys:**
- `Pharmacy Transfer Receive Receipt is A4`
- `Pharmacy Transfer Receive Bill is Template`
- `Pharmacy Transfer Receive Receipt is A4 Detailed`
- `Pharmacy Transfer Receive Receipt is A4 Custom 1`
- `Pharmacy Transfer Receive Receipt is A4 Custom 2`


## Implementation Guide

### Adding Settings to a New Page

#### 1. Frontend Implementation (XHTML)

**Step 1: Add Settings Button**

⚠️ **Important**: Settings buttons should be placed in the **printing/preview section** where `printPreview = true`, not in the main billing interface where `printPreview = false`. This ensures settings are accessible when users are reviewing print formats.

```xhtml
<!-- Place in the print preview section header -->
<h:panelGroup rendered="#{yourController.printPreview}">
    <p:panel>
        <f:facet name="header">
            <div class="d-flex align-items-center justify-content-between">
                <div>
                    <!-- Print preview title -->
                </div>
                <div>
                    <p:commandButton
                        value="Print"
                        ajax="false"
                        action="#"
                        class="ui-button-info mx-2"
                        icon="fas fa-print">
                        <p:printer target="printTarget" />
                    </p:commandButton>
                    <p:commandButton
                        rendered="#{webUserController.hasPrivilege('ChangeReceiptPrintingPaperTypes')}"
                        value="Settings"
                        icon="fas fa-cog"
                        class="ui-button-secondary mx-1"
                        type="button"
                        onclick="PF('yourModuleConfigDialog').show();" />
                    <!-- Other action buttons -->
                </div>
            </div>
        </f:facet>
        <!-- Print preview content -->
    </p:panel>
</h:panelGroup>
```

**Step 2: Add Configuration Dialog**
```xhtml
<!-- Your Module Configuration Dialog -->
<p:dialog id="yourModuleConfigDialog"
          header="Your Module Printer Configuration"
          widgetVar="yourModuleConfigDialog"
          modal="true"
          width="600"
          height="500"
          resizable="true"
          maximizable="true"
          closeOnEscape="true">

    <p:ajax event="open" listener="#{pharmacyRetailConfigController.loadCurrentConfig}" update="yourModuleConfigForm" />

    <h:form id="yourModuleConfigForm">
        <div class="row">
            <div class="col-12">
                <div class="card config-section">
                    <div class="card-header">
                        <h6><i class="fas fa-file-alt"></i> Your Module Paper Settings</h6>
                    </div>
                    <div class="card-body">
                        <div class="mb-3">
                            <p:selectBooleanCheckbox
                                id="yourOptionId"
                                value="#{pharmacyRetailConfigController.yourProperty}" />
                            <p:outputLabel for="yourOptionId" value="Your Option Name" class="ms-2" />
                            <small class="form-text text-muted d-block">Description of what this option does</small>
                        </div>
                        <!-- Add more options as needed -->
                    </div>
                </div>
            </div>
        </div>

        <div class="row mt-4">
            <div class="col-12">
                <div class="card">
                    <div class="card-body">
                        <p:messages id="yourModuleConfigMessages" showDetail="true" closable="true" />
                        <div class="d-flex justify-content-between align-items-center">
                            <div class="d-flex gap-2">
                                <p:commandButton
                                    value="Apply &amp; Close"
                                    icon="fas fa-save"
                                    class="ui-button-success"
                                    ajax="false"
                                    action="#{pharmacyRetailConfigController.saveYourModuleConfig}" />
                                <p:commandButton
                                    value="Cancel"
                                    icon="fas fa-times"
                                    class="ui-button-secondary"
                                    onclick="PF('yourModuleConfigDialog').hide(); return false;"
                                    type="button" />
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </h:form>
</p:dialog>
```

#### 2. Backend Implementation (Java)

**Step 1: Add Properties to PharmacyConfigController**
```java
// Your Module Settings
private boolean yourModuleOption1;
private boolean yourModuleOption2;
// Add more as needed
```

**Step 2: Add Loading Logic**
```java
// Add to loadCurrentConfig() method
// Your Module Settings
yourModuleOption1 = configOptionController.getBooleanValueByKey("Your Config Key 1", true);
yourModuleOption2 = configOptionController.getBooleanValueByKey("Your Config Key 2", false);
```

**Step 3: Add Saving Logic**
```java
// Add to saveConfig() method
// Your Module Settings
configOptionController.setBooleanValueByKey("Your Config Key 1", yourModuleOption1);
configOptionController.setBooleanValueByKey("Your Config Key 2", yourModuleOption2);

// Add specific save method
public void saveYourModuleConfig() {
    try {
        // Your Module Settings
        configOptionController.setBooleanValueByKey("Your Config Key 1", yourModuleOption1);
        configOptionController.setBooleanValueByKey("Your Config Key 2", yourModuleOption2);

        JsfUtil.addSuccessMessage("Your Module configuration saved successfully");
        loadCurrentConfig();
    } catch (Exception e) {
        JsfUtil.addErrorMessage("Error saving Your Module configuration: " + e.getMessage());
    }
}
```

**Step 4: Add Getters and Setters**
```java
// Your Module Getters and Setters
public boolean isYourModuleOption1() {
    return yourModuleOption1;
}

public void setYourModuleOption1(boolean yourModuleOption1) {
    this.yourModuleOption1 = yourModuleOption1;
}

public boolean isYourModuleOption2() {
    return yourModuleOption2;
}

public void setYourModuleOption2(boolean yourModuleOption2) {
    this.yourModuleOption2 = yourModuleOption2;
}
```

### Configuration Key Naming Convention

Follow this naming pattern for configuration keys:
- `Pharmacy [Module] [Receipt/Bill] [is/Paper] [Format Type]`

Examples:
- `Pharmacy Transfer Issue A4 Paper`
- `Pharmacy Transfer Receive Bill is Template`
- `GRN Receipt Paper is A4`

### Configuration Controller Usage Guidelines

**Use ConfigOptionController for:**
- Printer-related configurations (paper types, formats)
- Department-specific settings that vary by user's department
- Print format selections that departments can customize independently

**Use ConfigOptionApplicationController for:**
- Application-wide business rules and preferences
- System-wide settings that don't change by department
- Feature enable/disable flags that apply to all departments

**Examples:**
```xhtml
<!-- Department-specific printer configuration -->
<h:panelGroup rendered="#{configOptionController.getBooleanValueByKey('Pharmacy Transfer Issue A4 Paper', true)}">

<!-- Application-wide business rule -->
<h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Pharmacy Transfer Issues can be Cancelled', true)}">
```

### Permission Requirements

All settings buttons require the `ChangeReceiptPrintingPaperTypes` privilege:
```xhtml
rendered="#{webUserController.hasPrivilege('ChangeReceiptPrintingPaperTypes')}"
```

## Best Practices

### 1. UI Consistency
- Use standard dialog structure with card layouts
- Include descriptive help text for each option
- Use consistent button styling (`ui-button-secondary` for settings, at the left of the page)
- Place settings buttons in the right button area

### 2. Backend Consistency
- Group related properties together with comments
- Use descriptive property names matching the UI labels
- Implement both general and specific save methods
- Always reload configuration after saving

### 3. Error Handling
- Include try-catch blocks in save methods
- Use `JsfUtil` for user feedback messages
- Provide clear error messages

### 4. Configuration Validation
- Set appropriate default values
- Use boolean flags for enable/disable options
- Ensure configuration keys are unique and descriptive

## Troubleshooting

### Common Issues

1. **Settings Button Not Visible**
   - Check user privileges (`ChangeReceiptPrintingPaperTypes`)
   - Verify button permission rendering condition

2. **Dialog Not Opening**
   - Check `widgetVar` matches `onclick` function call
   - Ensure dialog ID is unique on the page

3. **Configuration Not Saving**
   - Verify configuration keys match between load and save methods
   - Check for proper form submission (`ajax="false"`)
   - Review error messages in browser console

4. **Settings Not Applied**
   - Ensure configuration keys match the ones used in rendering conditions
   - Verify you're using the correct controller (`configOptionController` vs `configOptionApplicationController`)
   - Check if page caching is affecting configuration display
   - Verify `loadCurrentConfig()` is called after saving
   - Remember: `configOptionController` provides department-specific values

### Debug Tips

1. Check configuration values in database:
   ```sql
   SELECT * FROM config_option WHERE option_key LIKE '%Transfer Issue%';
   ```

2. Enable debug logging for configuration changes
3. Use browser developer tools to inspect AJAX calls
4. Verify EL expressions in XHTML are correctly referencing controller properties

## Related Documentation

- [Application Options Configuration](application-options.md)
- [UI Styling Guidelines](../ui/ui-styling-guidelines.md)
- [JSF Best Practices](../ui/best-practices.md)
- [Security Privileges](../security/privilege-system.md)
