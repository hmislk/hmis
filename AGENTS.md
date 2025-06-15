# Development Notes

## Branch Naming Convention

All development branches must be created based on the related GitHub issue when the issue name is available. The naming convention is as follows:

- Begin with the issue number.
- Follow with a hyphen `-` and the issue title in lowercase.
- Replace all spaces in the title with hyphens `-`.
- Use only hyphens and underscores; avoid other special characters.
### Examples

- `12875-implement-full-lab-workflow-history-tracking`
- `11964-implement-scheduled-historical-record-processing-and-recording-framework`
- `12881-refactor-calsalerte-method-in-pharmacydirectpurchasecontroller-for-better-maintainability`
- `12790-incorrect-retail-rate-and-sale-value-displayed-for-pack-purchases-in-direct-purchase-with-costing`
- `12888-need-to-display-consultant-name-and-credit-company-name`
- `12746-return-item-only-with-discount-accept-pharmacy-refund-updated-incorrect-in-pharmacy-income-report`

## Closing Issues with Pull Requests

After creating a branch and opening a pull request, include a closing phrase such as `Closes #<issue-number>` in the PR description or a follow-up comment. This ensures GitHub Actions will automatically move the issue through project workflows when the PR is merged.

## Adding User Icons

1. Add a new constant to `src/main/java/com/divudi/core/data/Icon.java`. The value should be the label displayed to users.
2. Create an SVG (or gvd) under `src/main/webapp/resources/images/home` with the graphic for the new feature.
3. Update `src/main/webapp/home.xhtml` to reference the new `Icon` constant and image. This home page change should be tracked as a separate issue when creating pull requests.



## Handling Privileges

- Add all privilege constants to `src/main/java/com/divudi/core/data/Privileges.java`.
- Use `WebUserController.hasPrivilege(String)` to verify a user's access for a department or feature.

## Menu Icons
* When creating menu items or command buttons, always specify an icon using
  PrimeFaces (`pi pi-*`) or Font Awesome (`fa`/`fas`) classes. This ensures the
  UI remains consistent across themes.


## Homepage Icon Management Guidelines

To ensure visual consistency and maintainability across all homepage icons:

### 1. Use of SVG Files Only
- All icons **must be SVG**.
- Discontinue use of PNG, JPG, and WebP formats.
- Replace existing bitmap icons with SVGs over time.

### 2. Style Consistency
- Use **simple line-based SVGs**.
- All SVG paths must use `fill="currentColor"` to allow CSS/theming control.
- Avoid multi-colour icons or complex illustrations.

### 3. Size Standardisation
- All icons must be displayed at `width="80"` and `height="80"`.
- SVGs should retain a clean `viewBox="0 0 48 48"` or similar.
- Remove padding/margin from SVGs to align visuals.

### 4. File Naming and Storage
- All icons must be placed in `/resources/images/home/`
- Use lowercase and hyphen-separated filenames:
  - ✅ `cashier-drawer.svg`
  - ❌ `CashierDrawer.PNG`

### 5. Rendering in Code
Use this format:
```xhtml
<p:graphicImage library="images" name="home/your-icon.svg"
                style="cursor: pointer;" width="80" height="80"/>
```
- Wrap with `img-thumbnail` for border and uniform styling.
- Use consistent tooltip and hidden label pattern for accessibility.

### 6. Tooltip and Labeling
- Use meaningful `value` attributes in `p:tooltip`.
- Hide text label with `display: none;` unless required.
- Maintain consistency across all icons.

### 7. Future Additions
All new icons must:
- Be SVG.
- Match visual style and dimensions.
- Use `currentColor` fill.
- Follow naming/storage conventions.

### 8. Theming and Accessibility
- Use `fill="currentColor"` for dynamic theming.
- Reference colours via `configOptionApplicationController`.
- Plan for light/dark mode support.

---

## Colour Configuration Options

Define the following colour config options via `configOptionApplicationController`:

| Config Key                         | Description                                | Default Value |
|------------------------------------|--------------------------------------------|---------------|
| `Home Icon Colour`                 | Fill colour for homepage icons             | `#1E88E5`     |
| `Home Icon Hover Colour`          | Hover state colour                         | `#1565C0`     |
| `Home Icon Background`            | Icon background colour                     | `#FFFFFF`     |
| `Homepage Tooltip Colour`         | Tooltip text colour                        | `#424242`     |
| `Homepage Section Header Colour`  | Colour for section headers                 | `#263238`     |
| `Critical Alert Colour`           | Colour for critical messages               | `#D32F2F`     |
| `Warning Alert Colour`            | Colour for warnings                        | `#FBC02D`     |
| `Success Highlight Colour`        | Colour for success states                  | `#388E3C`     |

### Sample Access in JSF
```xhtml
style="color: #{configOptionApplicationController.getShortTextValueByKey('Home Icon Colour', true)}"
```

## ConfigOptionApplicationController Usage

This controller is used to dynamically manage application-wide configuration values. These values can be accessed via Java code or JSF EL expressions.

### ✅ Java Usage

```java
if (configOptionApplicationController.getBooleanValueByKey("Automatically Load and Display the Refund Amount Upon Page Load")) {
    if (configOptionApplicationController.getBooleanValueByKey("Disable Hospital Fee Refunds")) {
        for (BillFee bf : bs.getBill().getBillFeesWIthoutZeroValue()) {
            if (!(bf.getFee().getFeeType() == FeeType.OwnInstitution)) {
                copyValue(bf);
                calRefundTotal();
                checkRefundTotal();
            }
        }
    } else {
        for (BillFee bf : bs.getBill().getBillFeesWIthoutZeroValue()) {
            copyValue(bf);
            calRefundTotal();
            checkRefundTotal();
        }
    }
}
```

### ✅ JSF Usage

In XHTML pages, configuration values can be checked or used directly for rendering logic or styling.

#### Example: Conditional Rendering

```xhtml
<h:panelGroup rendered="#{!configOptionApplicationController.getBooleanValueByKey('Remove Rate and Values From Printing on Cashier Sale Bill')}">
    <td><h:outputLabel value='RATE'/></td>
    <td><h:outputLabel value='VALUE'/></td>
</h:panelGroup>
```

#### Example: Dynamic Styling

```xhtml
<h:outputText style="color: #{configOptionApplicationController.getShortTextValueByKey('Home Icon Colour', true)}" value="Sample Text"/>
```

### ℹ️ Notes

- Always use the exact config key string as defined in the database.
- Use `.getBooleanValueByKey()` for true/false logic.
- Use `.getShortTextValueByKey(key, true)` for text values.
- Missing keys will be auto-created with default values if provided.


## Adding and Managing Privileges

The system uses an enum-based approach to define and control user privileges.

### 🔧 Step-by-Step: Adding a New Privilege

1. **Add a new enum value** to the `Privileges` enum:

```java
package com.divudi.core.data;

public enum Privileges {
    InwardAdmissionsInwardAppoinment, // Example
    ...
}
```

2. **Update the User Privileges UI**

Navigate to:  
`/admin/users/user_privileges.xhtml`

Ensure that the new enum value is available for assignment to users per department via this interface.

3. **Assign Privileges to Users**

For a user to access a specific function or page, the corresponding privilege **must** be assigned to them for at least one department.

4. **Render Menu Items or Features Conditionally**

Use the following check to conditionally render a UI component:

```xhtml
<h:panelGroup rendered="#{webUserController.hasPrivilege('InwardAdmissionsInwardAppoinment')}" layout="block" class="col-1 p-1">
    ...
</h:panelGroup>
```

### 📌 Notes

- Privileges are checked at runtime, per user and department.
- If a privilege is missing, related functions/pages will be hidden or blocked.
- All new privileges must be assigned manually via the User Privileges page.

These guidelines apply to the entire repository.
