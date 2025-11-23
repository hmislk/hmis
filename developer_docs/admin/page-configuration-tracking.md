# Page Configuration Tracking - Developer Guide

## Overview

The Page Configuration Tracking system provides administrators with a comprehensive view of configuration options and privileges used on each page. This helps with:

- Understanding what configuration options affect a page
- Knowing which privileges control access to features
- Documenting page functionality
- Troubleshooting configuration issues
- Onboarding new administrators

## Architecture

### Components

1. **Data Classes** (`com.divudi.core.data.admin`)
   - `PageMetadata`: Contains all metadata about a page
   - `ConfigOptionInfo`: Information about a configuration option
   - `PrivilegeInfo`: Information about a privilege

2. **Registry** (`com.divudi.bean.common.PageMetadataRegistry`)
   - Application-scoped bean
   - Stores metadata for all registered pages
   - Provides lookup and retrieval methods

3. **Admin Controller** (`com.divudi.bean.common.PageAdminController`)
   - Session-scoped bean
   - Manages navigation to admin interface
   - Loads page metadata for display

4. **Admin UI** (`/admin/page_configuration_view.xhtml`)
   - Displays page metadata
   - Shows configuration options and current values
   - Lists privileges and their usage

## Registering a New Page

### Step 1: Add Imports to Controller

```java
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import javax.annotation.PostConstruct;
```

### Step 2: Inject PageMetadataRegistry

```java
@Inject
PageMetadataRegistry pageMetadataRegistry;
```

### Step 3: Create @PostConstruct Method

If your controller doesn't have a `@PostConstruct` method:

```java
@PostConstruct
public void init() {
    registerPageMetadata();
}
```

If your controller already has a `@PostConstruct` method, add the call to `registerPageMetadata()`:

```java
@PostConstruct
public void init() {
    // Existing initialization code
    // ...

    registerPageMetadata();
}
```

### Step 4: Implement registerPageMetadata() Method

🚨 **CRITICAL: ConfigOptionInfo Constructor Options** 🚨

The `ConfigOptionInfo` class provides **TWO constructor options**. Choose the one that fits your needs:

#### Option 1: 4-Parameter Constructor (Detailed Documentation)
```java
new ConfigOptionInfo(
    "Configuration Key Name",           // Config key (exact match required)
    "Description of what this config does",  // Human-readable description
    "Line XXX: Where it's used in XHTML",   // Usage location for developers
    OptionScope.APPLICATION             // Scope (APPLICATION/DEPARTMENT/USER)
)
```

#### Option 2: 3-Parameter Constructor (Convenience - No Usage Location)
```java
new ConfigOptionInfo(
    "Configuration Key Name",           // Config key (exact match required)
    "Description of what this config does",  // Human-readable description
    OptionScope.APPLICATION             // Scope (APPLICATION/DEPARTMENT/USER)
)
```

⚠️ **NEVER create your own ConfigOptionInfo class!** Always use `com.divudi.core.data.admin.ConfigOptionInfo`

#### Complete Implementation Example

```java
/**
 * Register page metadata for the admin interface
 */
private void registerPageMetadata() {
    if (pageMetadataRegistry == null) {
        return;
    }

    PageMetadata metadata = new PageMetadata();
    metadata.setPagePath("your/page/path");  // e.g., "inward/pharmacy_bill_issue_bht"
    metadata.setPageName("Your Page Name");
    metadata.setDescription("Description of what this page does");
    metadata.setControllerClass("YourControllerClassName");

    // Register configuration options - USE 4-PARAMETER FOR DETAILED TRACKING
    metadata.addConfigOption(new ConfigOptionInfo(
        "Configuration Key Name",
        "Description of what this configuration does",
        "Line XXX: Where it's used in the XHTML",
        OptionScope.APPLICATION  // or OptionScope.DEPARTMENT or OptionScope.USER
    ));

    // OR USE 3-PARAMETER FOR SIMPLER IMPLEMENTATION
    metadata.addConfigOption(new ConfigOptionInfo(
        "Another Config Key",
        "Description of what this configuration does",
        OptionScope.APPLICATION
    ));

    // Register privileges - PrivilegeInfo uses different constructor pattern
    metadata.addPrivilege(new PrivilegeInfo(
        "PrivilegeName",
        "Description of what this privilege controls",
        "Lines XXX, YYY: Where it's used in the XHTML"
    ));

    // Add more privileges as needed
    metadata.addPrivilege(new PrivilegeInfo(
        "AnotherPrivilege",
        "Description",
        "Line ZZZ: Usage location"
    ));

    // Register the page metadata
    pageMetadataRegistry.registerPage(metadata);
}
```

### Step 5: Add Admin Button to XHTML

Add this button in the page header (adjust placement as needed):

```xml
<!-- Admin Configuration Button (only for administrators) -->
<h:panelGroup rendered="#{webUserController.hasPrivilege('Admin')}">
    <p:commandButton
        value="Config"
        icon="fa fa-cog"
        title="Page Configuration Management"
        action="#{pageAdminController.navigateToPageAdmin()}"
        ajax="false"
        class="ui-button-secondary mx-2">
    </p:commandButton>
</h:panelGroup>
```

## Finding Configuration Options and Privileges

### Finding Configuration Options

1. Open the XHTML file
2. Search for `configOptionApplicationController.getBooleanValueByKey(` or similar methods
3. Extract the configuration key name (first parameter)
4. Note the line number and what it controls

Example:
```xml
<!-- Line 221 -->
<p:column
    rendered="#{configOptionApplicationController.getBooleanValueByKey('Medicine Identification Codes Used',true)}"
    headerText="Code">
```

Becomes:
```java
metadata.addConfigOption(new ConfigOptionInfo(
    "Medicine Identification Codes Used",
    "Shows medicine identification codes in the autocomplete dropdown",
    "Line 221: Autocomplete column visibility",
    OptionScope.APPLICATION
));
```

### Finding Privileges

1. Open the XHTML file
2. Search for `webUserController.hasPrivilege(`
3. Extract the privilege name (parameter)
4. Note the line number and what it controls

Example:
```xml
<!-- Line 229 -->
<p:column
    rendered="#{webUserController.hasPrivilege('ShowDrugCharges')}"
    headerText="Rate">
```

Becomes:
```java
metadata.addPrivilege(new PrivilegeInfo(
    "ShowDrugCharges",
    "View drug prices and financial charges in the billing interface",
    "Lines 229, 285, 293, 364, 369: Rate and value columns"
));
```

## Configuration Scopes

### OptionScope.APPLICATION
- Applies to the entire application
- Same value for all users and departments
- Most common scope

### OptionScope.DEPARTMENT
- Can be different for each department
- Allows department-specific customization

### OptionScope.USER
- Can be different for each user
- Allows user-specific preferences

## Best Practices

### 1. Complete Registration
Register ALL configuration options and privileges used on the page, not just the main ones.

### 2. Clear Descriptions
Write descriptions that explain:
- What the configuration/privilege does
- What happens when enabled/disabled
- What features it affects

### 3. Accurate Line References
Keep line numbers updated when modifying XHTML files. If lines change significantly, use description of location instead:
- Good: "Patient details section: Shows PHN field"
- Acceptable: "Line 123: PHN field visibility"

### 4. Group Related Items
If a configuration option is used in multiple places, list all of them:
```java
metadata.addConfigOption(new ConfigOptionInfo(
    "Show Patient Photos",
    "Displays patient photos throughout the application",
    "Lines 45, 89, 234, 567: Patient photo display",
    OptionScope.APPLICATION
));
```

### 5. Update When Modifying Pages
When you add a new configuration option or privilege check to a page:
1. Add it to the XHTML
2. Update the `registerPageMetadata()` method
3. Keep the documentation in sync

## Example: Complete Controller Implementation

```java
package com.divudi.bean.example;

import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class ExampleController implements Serializable {

    @Inject
    PageMetadataRegistry pageMetadataRegistry;

    public ExampleController() {
    }

    @PostConstruct
    public void init() {
        registerPageMetadata();
    }

    private void registerPageMetadata() {
        if (pageMetadataRegistry == null) {
            return;
        }

        PageMetadata metadata = new PageMetadata();
        metadata.setPagePath("module/example_page");
        metadata.setPageName("Example Page");
        metadata.setDescription("Example page for demonstration purposes");
        metadata.setControllerClass("ExampleController");

        // Configuration options
        metadata.addConfigOption(new ConfigOptionInfo(
            "Enable Advanced Features",
            "Enables advanced features for power users",
            "Line 45: Advanced features panel visibility",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Show Detailed Reports",
            "Displays detailed report options",
            "Lines 78, 123: Report selection dropdown",
            OptionScope.DEPARTMENT
        ));

        // Privileges
        metadata.addPrivilege(new PrivilegeInfo(
            "ExamplePageAccess",
            "Basic access to the example page",
            "Line 12: Page visibility"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
            "ExamplePageEdit",
            "Ability to edit data on the example page",
            "Lines 56, 89, 234: Edit buttons and forms"
        ));

        pageMetadataRegistry.registerPage(metadata);
    }

    // Rest of controller code...
}
```

## Accessing the Admin Interface

### For Administrators

1. Navigate to any registered page (e.g., Pharmacy BHT Direct Issue)
2. Look for the "Config" button in the page header (visible only to admins)
3. Click the button to view page configuration and privileges

### Programmatic Access

```java
// Inject the registry
@Inject
PageMetadataRegistry pageMetadataRegistry;

// Get metadata for a specific page
PageMetadata metadata = pageMetadataRegistry.getMetadata("inward/pharmacy_bill_issue_bht");

// Get all registered pages
List<PageMetadata> allPages = pageMetadataRegistry.getAllPages();

// Check if a page is registered
boolean isRegistered = pageMetadataRegistry.isPageRegistered("module/example_page");
```

## 🚨 CRITICAL ERROR PREVENTION & TROUBLESHOOTING 🚨

### 🛑 COMPILATION ERRORS - MOST COMMON ISSUES

#### Error: "constructor ConfigOptionInfo cannot be applied to given types"

**CAUSE**: Using wrong number of parameters or creating local ConfigOptionInfo class

**SOLUTIONS**:
1. **✅ Use the correct core class**: Always import `com.divudi.core.data.admin.ConfigOptionInfo`
2. **✅ Choose the right constructor**:
   - 3-parameter: `new ConfigOptionInfo(key, description, scope)`
   - 4-parameter: `new ConfigOptionInfo(key, description, usageLocation, scope)`
3. **❌ NEVER**: Create your own inner ConfigOptionInfo class
4. **❌ NEVER**: Use 2-parameter constructor (doesn't exist in core class)

**Example of WRONG approach**:
```java
// ❌ WRONG - Don't create inner class
public static class ConfigOptionInfo {
    // This causes type conflicts!
}

// ❌ WRONG - 2-parameter constructor doesn't exist
new ConfigOptionInfo("key", "defaultValue")
```

**Example of CORRECT approach**:
```java
// ✅ CORRECT - Use core class with 3 parameters
import com.divudi.core.data.admin.ConfigOptionInfo;

new ConfigOptionInfo("key", "description", OptionScope.APPLICATION)

// ✅ CORRECT - Use core class with 4 parameters
new ConfigOptionInfo("key", "description", "usage location", OptionScope.APPLICATION)
```

#### Error: "incompatible types: cannot be converted to ConfigOptionInfo"

**CAUSE**: Type conflict between local class and core class

**SOLUTION**: Remove any local ConfigOptionInfo class and use only the core class

#### Error: "PageMetadataRegistry cannot be resolved"

**CAUSE**: Missing import or injection

**SOLUTION**:
```java
import com.divudi.bean.common.PageMetadataRegistry;

@Inject
PageMetadataRegistry pageMetadataRegistry;
```

### 🔧 RUNTIME ERRORS

#### Admin Button Not Showing

1. **Check privilege**: User must have 'Admin' privilege
2. **Check registration**: Verify page is registered in controller's @PostConstruct
3. **Check XHTML**: Ensure admin button code is added to the page
4. **Check imports**: Verify all required imports are present

#### Metadata Not Showing

1. **Check injection**: Ensure `PageMetadataRegistry` is properly injected with `@Inject`
2. **Check null check**: Verify the null check in `registerPageMetadata()`
3. **Check page path**: Ensure page path matches exactly (case-sensitive)
4. **Check @PostConstruct**: Ensure `registerPageMetadata()` is called during initialization

#### Configuration Values Not Displaying

1. **Check config key**: Ensure configuration key matches exactly (case-sensitive)
2. **Check scope**: Verify the correct scope is used (APPLICATION/DEPARTMENT/USER)
3. **Check application settings**: Verify configuration exists in database
4. **Check constructor parameters**: Verify using correct parameter order

### 🔍 VALIDATION CHECKLIST

Before implementing, verify:

- [ ] **Imports Complete**: All required imports added
  ```java
  import com.divudi.bean.common.PageMetadataRegistry;
  import com.divudi.core.data.OptionScope;
  import com.divudi.core.data.admin.ConfigOptionInfo;
  import com.divudi.core.data.admin.PageMetadata;
  import com.divudi.core.data.admin.PrivilegeInfo;
  import javax.annotation.PostConstruct;
  ```

- [ ] **Injection Correct**: PageMetadataRegistry properly injected
  ```java
  @Inject
  PageMetadataRegistry pageMetadataRegistry;
  ```

- [ ] **Constructor Choice**: Using correct ConfigOptionInfo constructor
  - 3-parameter: `(key, description, scope)`
  - 4-parameter: `(key, description, usageLocation, scope)`

- [ ] **No Local Classes**: No local ConfigOptionInfo class created

- [ ] **PostConstruct Present**: @PostConstruct method calls registerPageMetadata()

- [ ] **Null Check**: Proper null check for pageMetadataRegistry

- [ ] **Page Path Accurate**: Page path matches exactly (case-sensitive)

## Maintenance

### When Adding New Features

1. Add new configuration option or privilege to XHTML
2. Update `registerPageMetadata()` method immediately
3. Test admin interface to verify display

### When Refactoring

1. If page path changes, update `metadata.setPagePath()`
2. If configuration keys change, update all references
3. If privileges change, update privilege registrations

### When Removing Features

1. Remove from XHTML
2. Remove from `registerPageMetadata()` method
3. Consider cleanup of database configuration entries

## Integration with CLAUDE.md

This system is documented in the main CLAUDE.md file:

```markdown
## Admin Interface for easy configuration
- **Registry**: PageMetadataRegistry tracks page metadata
- **Registration**: Controllers register their pages in @PostConstruct
- **Admin UI**: /admin/page_configuration_view.xhtml displays configuration
- **Access**: Admin button visible only to users with 'Admin' privilege
- **Documentation**: See developer_docs/admin/page-configuration-tracking.md
```

## Future Enhancements

Potential improvements to consider:

1. **Automatic Discovery**: Auto-scan XHTML files for configuration and privilege usage
2. **Database Storage**: Persist metadata in database for historical tracking
3. **Change Detection**: Alert when configuration options or privileges change
4. **Documentation Links**: Link to wiki documentation for each configuration
5. **Dependency Tracking**: Show which configurations depend on others
6. **Usage Analytics**: Track which configurations are most commonly used

## Support

For questions or issues:

1. Check this documentation
2. Review example implementation in `PharmacySaleBhtController`
3. Examine admin UI at `/admin/page_configuration_view.xhtml`
4. Consult with the development team

---

**Last Updated**: 2025-11-13
**Version**: 1.0
**Author**: HMIS Development Team
