# Configuration Options System Guide

## Overview

The HMIS system uses a flexible configuration system that allows administrators to customize application behavior without code changes. This guide explains how to work with configuration options from both developer and end-user perspectives.

## For Developers

### Adding New Configuration Options

#### 1. Define Configuration Option in `ConfigOptionApplicationController`

Add your configuration option in the appropriate `load*ConfigurationDefaults()` method:

```java
private void loadPharmacyConfigurationDefaults() {
    // Boolean configuration
    getBooleanValueByKey("Your Config Option Name", false);

    // String configuration
    getShortTextValueByKey("Your String Config", "default value");

    // Integer configuration
    getIntegerValueByKey("Your Integer Config", 0);

    // Double configuration
    getDoubleValueByKey("Your Double Config", 0.0);

    // Long text configuration
    getLongTextValueByKey("Your Long Text Config", "");
}
```

**Configuration Value Types:**
- `BOOLEAN` - True/false values
- `SHORT_TEXT` - Short strings (< 255 characters)
- `LONG_TEXT` - Long strings (for HTML, CSS, etc.)
- `INTEGER` - Whole numbers
- `DOUBLE` - Decimal numbers
- `LONG` - Large whole numbers
- `COLOR` - Color codes
- `ENUM` - Enumeration values

#### 2. Use Configuration in Your Controller

Inject `ConfigOptionApplicationController`:

```java
@Inject
ConfigOptionApplicationController configOptionApplicationController;
```

Read the configuration value:

```java
// Boolean
boolean isRequired = configOptionApplicationController.getBooleanValueByKey(
    "Your Config Option Name",
    false  // default value if not found
);

// String
String value = configOptionApplicationController.getShortTextValueByKey(
    "Your String Config",
    "default"
);

// Integer
Integer count = configOptionApplicationController.getIntegerValueByKey(
    "Your Integer Config",
    0
);
```

#### 3. Apply Configuration Logic

Use the configuration value to control behavior:

```java
public void settleBill() {
    // Check configuration
    if (configOptionApplicationController.getBooleanValueByKey("Patient is required in Pharmacy Retail Sale", false)) {
        if (getPatient() == null) {
            JsfUtil.addErrorMessage("Patient is required.");
            return;
        }
    }

    // Rest of settlement logic
}
```

### Configuration Naming Conventions

**Best Practices:**
- Use descriptive, clear names
- Include context (module, feature, field)
- Use consistent patterns
- Avoid abbreviations unless widely understood

**Examples:**
```
✅ Good:
- "Patient is required in Pharmacy Retail Sale"
- "Pharmacy Transfer Issue Bill is POS Paper"
- "Show Profit Percentage in GRN"

❌ Bad:
- "PatReq"
- "TrIssPOS"
- "ShowProf"
```

**Common Patterns:**
- **Boolean validation**: `[Field] is required in [Module] [Feature]`
- **Boolean feature toggle**: `Show [Field] in [Module] [Feature]`
- **Display option**: `[Module] [Feature] - Display [Field]`
- **Bill format**: `[Module] [Bill Type] is [Format]`

### Example: Complete Implementation

**Step 1: Add to ConfigOptionApplicationController**
```java
private void loadPharmacyConfigurationDefaults() {
    // ... existing options ...

    // Pharmacy Sale Validation Configuration Options
    getBooleanValueByKey("Patient is required in Pharmacy Retail Sale", false);
    getBooleanValueByKey("Patient Name is required in Pharmacy Retail Sale", false);
    getBooleanValueByKey("Patient Phone is required in Pharmacy Retail Sale", false);
}
```

**Step 2: Use in Controller**
```java
@Named
@SessionScoped
public class PharmacySaleController {

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    public String settleBill() {
        // Validation using config
        boolean patientRequired = configOptionApplicationController.getBooleanValueByKey(
            "Patient is required in Pharmacy Retail Sale",
            false
        );

        if (patientRequired) {
            if (getPatient() == null || getPatient().getPerson() == null) {
                JsfUtil.addErrorMessage("Patient is required.");
                return null;
            }
        }

        // Continue with settlement
        return "success";
    }
}
```

**Step 3: Document in Wiki**
Create end-user documentation in `docs/wiki/` with navigation instructions (see below).

## For End Users (Administrators)

### Accessing Configuration Options

**Navigation Path:**
```
Menu → Administration → Manage Institutions → Preferences → Configuration Options
```

**Step-by-Step:**
1. Click **Menu** in the top navigation
2. Select **Administration**
3. Click **Manage Institutions**
4. Go to the **Preferences** tab
5. Click **Configuration Options**
6. Use the search box to find the configuration option by key name

### Searching for Configuration Options

**Search Tips:**
- Configuration option keys are **case-sensitive**
- Search by partial key name (e.g., "Patient is required")
- Search by module (e.g., "Pharmacy Retail Sale")
- Search by feature (e.g., "Transfer Issue")

**Examples:**

| Search Term | Finds |
|-------------|-------|
| `Patient is required` | All patient requirement options |
| `Pharmacy Retail Sale` | All pharmacy retail sale options |
| `Transfer Issue` | All transfer issue options |
| `Display` | All display/visibility options |
| `Bill is POS` | All POS paper bill options |

### Changing Configuration Values

1. **Find the option** using the search box
2. **Click on the option** to edit
3. **Change the value:**
   - Boolean: Toggle checkbox (checked = true, unchecked = false)
   - Text: Enter new value in text box
   - Number: Enter numeric value
4. **Click Save** to apply changes
5. **Changes take effect immediately** (no restart required)

### Understanding Configuration Option Types

| Type | Example | How to Edit |
|------|---------|-------------|
| Boolean | `Patient is required in Pharmacy Retail Sale` | Checkbox (checked/unchecked) |
| Short Text | `Bill Number Delimiter` | Text input (max 255 chars) |
| Long Text | `Pharmacy Common Bill CSS` | Text area (unlimited) |
| Integer | `Email Gateway - SMTP Port` | Number input (whole numbers) |
| Double | `Wholesale Rate Factor` | Number input (decimals) |

## Common Configuration Scenarios

### Scenario 1: Making a Field Mandatory

**Requirement:** Make patient phone mandatory in pharmacy sales

**Steps:**
1. Navigate to Configuration Options
2. Search for: `Patient Phone is required`
3. Find: `Patient Phone is required in Pharmacy Retail Sale`
4. Check the checkbox (enable)
5. Click Save

**Result:** Users cannot complete pharmacy sales without entering patient phone number

### Scenario 2: Customizing Bill Format

**Requirement:** Use POS paper for transfer issue bills

**Steps:**
1. Navigate to Configuration Options
2. Search for: `Transfer Issue Bill is POS`
3. Find: `Pharmacy Transfer Issue Bill is POS Paper`
4. Check the checkbox (enable)
5. Uncheck other format options if needed
6. Click Save

**Result:** Transfer issue bills print in POS paper format

### Scenario 3: Adjusting Calculation Factors

**Requirement:** Change wholesale rate factor from 1.08 to 1.10

**Steps:**
1. Navigate to Configuration Options
2. Search for: `Wholesale Rate Factor`
3. Find the option
4. Change value from `1.08` to `1.10`
5. Click Save

**Result:** Wholesale rates calculated with new factor

## Writing Wiki Documentation

When creating user documentation that references configuration options, use this standard format:

### Template for Wiki Articles

```markdown
## How to Configure

### Accessing Configuration

1. Navigate to **Menu** → **Administration** → **Manage Institutions** → **Preferences** → **Configuration Options**
2. Use the search box to find configuration options by their key name
3. Each validation option can be enabled or disabled independently
4. Click the **Save** button after making changes

### Configuration Option Keys

When searching for configuration options, use these exact key names:

| Configuration Option Key | Purpose |
|-------------------------|---------|
| `Your Config Option Key Name` | Description of what it does |
| `Another Config Option` | Description |

**Quick Search Tips:**
- Search for "keyword" to find related options
- Configuration options are case-sensitive when searching

### Configuration Details

#### Option Name

**Configuration Key:** `Your Config Option Key Name`

**What it does:** Clear description

**When enabled:**
- Effect 1
- Effect 2

**When disabled:**
- Effect 1
- Effect 2

**Error message if violated:** "Exact error message text"
```

### Example Wiki Section

See the complete example in:
`docs/wiki/Pharmacy/Pharmacy-Sale-Mandatory-Fields-Configuration.md`

## Configuration Option Reference

### Location
All configuration defaults are defined in:
`com.divudi.bean.common.ConfigOptionApplicationController`

### Key Methods

| Method | Purpose |
|--------|---------|
| `loadApplicationOptions()` | Loads all configuration options at startup |
| `getBooleanValueByKey(key, default)` | Gets boolean configuration value |
| `getShortTextValueByKey(key, default)` | Gets short text value |
| `getLongTextValueByKey(key, default)` | Gets long text value |
| `getIntegerValueByKey(key, default)` | Gets integer value |
| `getDoubleValueByKey(key, default)` | Gets double value |
| `setBooleanValueByKey(key, value)` | Sets boolean value |
| `setIntegerValueByKey(key, value)` | Sets integer value |

### Database Storage

Configuration options are stored in the `config_option` table with these fields:
- `option_key` - The configuration key name
- `option_value` - The stored value
- `value_type` - The data type (BOOLEAN, SHORT_TEXT, etc.)
- `scope` - APPLICATION, DEPARTMENT, INSTITUTION, or USER
- `institution` - Link to institution (if scope is INSTITUTION)
- `department` - Link to department (if scope is DEPARTMENT)
- `web_user` - Link to user (if scope is USER)

## Best Practices

### For Developers

1. **Always provide defaults** - Don't assume config exists
2. **Use meaningful names** - Others should understand without code review
3. **Document in code** - Add comments explaining the option's purpose
4. **Group related options** - Keep similar configs together
5. **Test both states** - Ensure both enabled/disabled work correctly
6. **Update wiki** - Create end-user documentation

### For Administrators

1. **Document changes** - Keep track of what you changed and why
2. **Test before production** - Try config changes in test environment
3. **One change at a time** - Easier to troubleshoot issues
4. **Review periodically** - Adjust based on actual usage patterns
5. **Train users** - Explain why certain fields are mandatory

## Troubleshooting

### Configuration Not Taking Effect

**Possible causes:**
1. **Cache issue** - Try logging out and back in
2. **Wrong scope** - Check if option is set at correct level (application/department/institution)
3. **Typo in key** - Verify exact key name (case-sensitive)
4. **Code not checking config** - Developer may need to add the check

**Solution:**
1. Verify the configuration key is correct
2. Check the configuration value is saved
3. Clear browser cache
4. Restart application server (rare)

### Can't Find Configuration Option

**Possible causes:**
1. **Not yet added** - Developer hasn't created it yet
2. **Wrong search term** - Try different keywords
3. **Case sensitivity** - Check exact capitalization

**Solution:**
1. Search with partial key names
2. Check developer documentation for exact key
3. Contact system administrator or developer

### Configuration Value Not Saving

**Possible causes:**
1. **Permission issue** - User doesn't have admin rights
2. **Database connection** - Check database connectivity
3. **Validation error** - Invalid value for the type

**Solution:**
1. Verify user has administrator privileges
2. Check database connection
3. Ensure value matches expected type (boolean, number, text)

## Related Documentation

- **Application Options List**: [application-options.md](./application-options.md)
- **Bill Number Configuration**: [bill-number-config-options.md](./bill-number-config-options.md)
- **Printer Configuration**: [printer-configuration-system.md](./printer-configuration-system.md)
- **Wiki Writing Guidelines**: [../../CLAUDE.md#wiki-writing-guidelines](../../CLAUDE.md#wiki-writing-guidelines)
- **Wiki Publishing Guide**: [../github/wiki-publishing.md](../github/wiki-publishing.md)

---

**Last Updated:** 2025-10-05
**Maintained By:** Development Team
**For Questions:** Contact system administrator or development team
