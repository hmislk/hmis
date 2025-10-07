# UI Styling Guidelines for HMIS Project

## Button Styling

### ❌ Do NOT Use
- **Bootstrap button classes**: `btn btn-primary`, `btn btn-success`, `btn btn-warning`, etc.
- **Bootstrap utility classes for buttons**: These don't integrate well with PrimeFaces themes

### ✅ Use PrimeFaces Button Classes
```xhtml
<!-- Primary Actions -->
<p:commandButton class="ui-button-success" />

<!-- Secondary Actions -->  
<p:commandButton class="ui-button-info" />

<!-- Warning Actions -->
<p:commandButton class="ui-button-warning" />

<!-- Danger Actions -->
<p:commandButton class="ui-button-danger" />

<!-- Outlined Buttons -->
<p:commandButton class="ui-button-success ui-button-outlined" />
<p:commandButton class="ui-button-info ui-button-outlined" />

<!-- Disabled/Secondary -->
<p:commandButton class="ui-button-secondary" />
```

## Currency Display

### ❌ Avoid Currency-Specific Icons/Labels
```xhtml
<!-- Don't use these -->
<i class="fas fa-dollar-sign"></i>
<i class="fas fa-rupee-sign"></i>
<label>Requested Value (Rs)</label>
<label>Amount (USD)</label>
```

### ✅ Use Neutral Currency Icons/Labels
```xhtml
<!-- Use neutral money icons -->
<i class="fas fa-coins"></i>
<i class="fas fa-money-bill"></i>

<!-- Use neutral labels -->
<label>Requested Value</label>
<label>Total Amount</label>
<label>Net Value</label>
```

### Reason
- Different implementations may use different currencies (USD, Rs, EUR, etc.)
- Neutral icons and labels work for all currency types
- System-level currency configuration can handle display formatting

## Icon Usage

### Icon Priority and Sources
1. **PrimeFaces Icons (Primary)**: Always check PrimeFaces icon library first
2. **Font Awesome (Secondary)**: Use Font Awesome icons when suitable PrimeFaces icons are not available
3. **Consistency**: Once an icon is chosen for a data type/action, use it consistently across the application

### Financial/Currency Icons (Neutral)
- `fa-coins` - For monetary values, amounts
- `fa-money-bill` - For payment amounts, totals
- `fa-chart-line` - For financial reports, trends

### Action Icons
- `fa-edit` - For edit actions
- `fa-eye` - For view/preview actions
- `fa-check-circle` - For approval/finalize actions  
- `fa-check-double` - For completed/finalized status
- `fa-search` - For search actions
- `fa-times-circle` - For cancelled/blocked status
- `fa-undo` - For refund/reverse actions

### Data Type Icons  
- `fa-calendar-alt` - For date fields
- `fa-user` - For person/user fields
- `fa-building` - For institution/company fields
- `fa-hospital` - For department/medical units
- `fa-hashtag` - For ID/reference numbers
- `fa-list-ol` - For count/quantity fields

## Bootstrap Integration

### ✅ Safe to Use (Layout & Utility)
- **Grid system**: `row`, `col-*`, `col-lg-*`, etc.
- **Spacing utilities**: `mb-3`, `mt-2`, `p-2`, etc.
- **Flexbox utilities**: `d-flex`, `justify-content-center`, `align-items-center`
- **Text utilities**: `text-center`, `text-end`, `fw-bold`, `text-success`

### ⚠️ Use with Caution (Component Styling)  
- **Form classes**: `form-control`, `form-label` - okay for basic styling
- **Card/Panel classes**: May conflict with PrimeFaces panel styling

### ❌ Avoid (Component-Specific)
- **Button classes**: Use PrimeFaces button classes instead
- **Alert classes**: Use PrimeFaces messages/growl components
- **Modal classes**: Use PrimeFaces dialog components

## Theme and Styling Priority

### 1. PrimeFaces (Primary)
- **First choice**: Always try to use PrimeFaces themes, styles, and components
- **Headers/Footers**: Use PrimeFaces headers and footers instead of HTML heading tags
- **Color classes**: Prefer PrimeFaces color classes for consistent theming
- **Component styling**: Use PrimeFaces-native styling attributes

### 2. Bootstrap (Secondary) 
- **Layout utilities**: Use Bootstrap for layout (rows, cols, flexbox utilities)
- **Spacing utilities**: Use Bootstrap spacing classes (mb-3, mt-2, p-2, etc.)
- **Fallback styling**: Only when PrimeFaces classes cannot achieve the desired result
- **Grid system**: Primary choice for responsive layouts
- **Content alignment**: Mainly use Bootstrap for aligning contents like rows and columns

### 3. Custom Styles (Last Resort)
- **Minimize usage**: Avoid custom styles as much as possible
- **Location**: Use `\src\main\webapp\resources\css\ohmis.css` for custom classes when absolutely necessary
- **Avoid inline styles**: Minimize use of inline styles - prefer CSS classes
- **Theme-compatible**: Ensure custom styles work with PrimeFaces theme changes

### 4. Color Guidelines
- **Default colors first**: Try to use default PrimeFaces theme colors
- **Bootstrap fallback**: If PrimeFaces colors don't work, use Bootstrap color utilities
- **Theme-based approach**: Since we use PrimeFaces themes, colors should be theme-compatible
- **Consistency**: Maintain color consistency across the application

## Typography Guidelines (ERP-Focused)

### Font Sizing Philosophy
- **Avoid multiple font sizes**: This is a web application, not a website
- **Visual appeal**: Multiple size fonts are not visually appealing in ERP systems
- **Uniform appearance**: Maintain consistent text sizing for professional look
- **Business focus**: Text should support functionality, not dominate the interface

### Header Alternatives
- **No HTML headers**: Avoid h1, h2, h3, h4, h5, h6 tags
- **PrimeFaces headers**: Use PrimeFaces panel headers and component headers
- **h:outputText approach**: Use `<h:outputText>` with appropriate CSS classes for headings
- **Consistent styling**: Maintain uniform text appearance across modules

## Best Practices

1. **Consistency**: Use the same icon for the same data type across all pages
2. **Neutrality**: Avoid region/currency-specific icons and labels  
3. **PrimeFaces First**: Prefer PrimeFaces components and classes over Bootstrap alternatives
4. **Theme Compatibility**: Ensure styling works with different PrimeFaces themes
5. **Responsive Design**: Use Bootstrap grid system for responsive layouts
6. **Minimize Custom CSS**: Use existing PrimeFaces/Bootstrap classes before creating custom styles
7. **Theme-Based Colors**: Stick to theme-compatible colors for consistent appearance
8. **Professional Typography**: Avoid website-style typography in favor of business application standards

## Common Patterns

### Search Form Layout
```xhtml
<div class="row">
    <div class="col-lg-3 col-md-4">
        <p:panel header="Search Filters" styleClass="shadow-sm">
            <!-- Primary filters visible -->
            <p:commandButton class="ui-button-warning w-100" />
            
            <!-- Advanced filters in collapsible panel -->
            <p:panel header="Advanced Filters" toggleable="true" collapsed="true">
                <!-- Secondary filters -->
            </p:panel>
        </p:panel>
    </div>
    <div class="col-lg-9 col-md-8">
        <!-- Data table -->
    </div>
</div>
```

### Data Table with Actions
```xhtml
<p:dataTable styleClass="table-striped" sortMode="multiple">
    <p:column headerText="Actions" styleClass="text-center" exportable="false">
        <p:commandButton 
            class="ui-button-success"
            disabled="#{condition}"
            icon="fas fa-edit" />
    </p:column>
</p:dataTable>
```

### Vertical Action Button Groups
```xhtml
<p:column id="colActions" headerText="Actions">
    <div class="btn-group-vertical" role="group" style="width: 100%;">
        <p:commandButton 
            value="Pre Bill" 
            action="#{controller.action}"
            styleClass="ui-button-success btn-sm"
            icon="fa fa-eye"
            style="margin-bottom: 2px;">
        </p:commandButton>
        
        <p:commandButton 
            value="Paid Bill" 
            rendered="#{condition}"
            action="#{controller.action}"
            styleClass="ui-button-info btn-sm"
            icon="fa fa-check-circle"
            style="margin-bottom: 2px;">
        </p:commandButton>

        <p:commandButton 
            value="Cancelled" 
            action="#{controller.action}" 
            styleClass="ui-button-danger btn-sm"
            icon="fa fa-times-circle"
            rendered="#{condition}"
            style="margin-bottom: 2px;">
        </p:commandButton>

        <p:commandButton 
            value="Refunded" 
            action="#{controller.action}" 
            styleClass="ui-button-warning btn-sm"
            icon="fa fa-undo"
            rendered="#{condition}"
            style="margin-bottom: 2px;">
        </p:commandButton>
    </div>
</p:column>
```

## Page Structure Guidelines

### Header Spacing Optimization (ERP Systems)
- **No HTML heading tags**: Do NOT use h1, h2, h3, h4, h5, h6 tags - this is an ERP system, not a website
- **Use h:outputText**: Use `<h:outputText>` with appropriate CSS classes for all headings and labels
- **Compact design**: Keep text compact and business-focused
- **Efficient use of space**: Text should be informative but not dominate the page layout

### Page Type Selection
- **Use HTML structure WITH ui:composition and template**: XHTML pages should use proper HTML document structure that contains ui:composition with template
- **HTML + Template combination**: Use complete HTML document structure with proper DOCTYPE and html root element, then use ui:composition inside h:body
- **Best of both worlds**: HTML structure provides flexibility while ui:composition with template ensures consistent layout and navigation

### Layout Structure Optimization
- **Consider p:panelGrid over nested panels**: When you have a simple container with just content inside, `p:panelGrid` can be more efficient than wrapping `p:panel`
- **Avoid unnecessary nesting**: Don't use `p:panel` when a simple `div` or `p:panelGrid` would suffice
- **Container efficiency**: Use the most appropriate container component for your content structure

### XML Entity Handling
- **Escape ampersands**: Always use `&amp;` instead of `&` in XHTML attribute values and content
- **Proper entity references**: The entity name must immediately follow the '&' in entity references
- **Common entities**: 
  - `&amp;` for &
  - `&lt;` for <
  - `&gt;` for >
  - `&quot;` for "
  - `&apos;` for '

## Efficient Layouts

### When to Use p:panelGrid vs p:panel
```xhtml
<!-- ❌ Unnecessary wrapping when only containing content -->
<p:panel>
    <div class="content">Simple content here</div>
</p:panel>

<!-- ✅ More efficient for simple layouts -->
<p:panelGrid columns="1" styleClass="w-100">
    <div class="content">Simple content here</div>
</p:panelGrid>

<!-- ✅ Use p:panel when you need header/footer facets -->
<p:panel>
    <f:facet name="header">Header Content</f:facet>
    <div class="content">Main content</div>
</p:panel>
```

### Page Structure Examples

#### ✅ Correct: HTML Structure WITH ui:composition and Template (PREFERRED)
```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:p="http://primefaces.org/ui"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets">

<h:head>
    <title>Purchase Orders - Finalization</title>
    <!-- Additional head elements if needed -->
</h:head>
<h:body>
    <ui:composition template="/resources/template/template.xhtml">
        <ui:define name="content">
            <!-- Page content here -->
            <h:form>
                <!-- Form content -->
            </h:form>
        </ui:define>
    </ui:composition>
</h:body>
</html>
```

#### ❌ Wrong: HTML Structure WITHOUT Template
```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<h:head>
    <title>Page Title</title>
</h:head>
<h:body>
    <!-- Page content here - loses template benefits -->
</h:body>
</html>
```

#### ❌ Wrong: ui:composition WITHOUT HTML Structure
```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE composition PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<ui:composition xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
                template="/resources/template/template.xhtml">
    <ui:define name="content">
        <!-- Content - lacks HTML document structure -->
    </ui:define>
</ui:composition>
```

### Why This Structure is Best

#### Advantages of HTML + ui:composition + Template:
1. **Proper Document Structure**: HTML DOCTYPE and root element provide proper document foundation
2. **Template Benefits**: Consistent navigation, layout, and styling from template system  
3. **Flexibility**: Can add custom head elements, scripts, or styles when needed
4. **SEO Friendly**: Proper HTML structure with meaningful titles
5. **Standards Compliant**: Follows web standards while leveraging JSF templates
6. **Maintenance**: Template changes propagate to all pages automatically

#### Key Requirements:
- **HTML DOCTYPE**: Must use `<!DOCTYPE html>` not `<!DOCTYPE composition>`
- **HTML Root Element**: Must use `<html>` with proper namespaces including `xmlns:ui`
- **Template Inside Body**: ui:composition with template goes inside `<h:body>`
- **Content Definition**: Page content goes inside `<ui:define name="content">`

### ERP-Appropriate Text Design
```xhtml
<!-- ❌ Wrong: HTML heading tags (website approach) -->
<h1 class="display-4 mb-4">Purchase Orders - Finalization Process</h1>
<h3>Search Filters</h3>
<h5 class="mb-0 text-primary fw-bold">Purchase Orders - Finalization</h5>

<!-- ✅ Correct: h:outputText (ERP approach) -->
<div class="d-flex align-items-center">
    <i class="fas fa-clipboard-check text-primary me-2"></i>
    <h:outputText value="Purchase Orders - Finalization" styleClass="text-primary fw-bold"/>
</div>

<!-- ✅ For labels and section titles -->
<h:outputText value="Search Filters" styleClass="fw-bold"/>

<!-- ✅ For messages and notifications -->
<h:outputText value="Access Restricted - You do not have the necessary permissions to access this page." 
              styleClass="fw-medium"/>
```

### Why h:outputText for ERP Systems
1. **Business Application**: ERP systems need compact, functional interfaces
2. **Screen Real Estate**: Maximize usable space for data and forms
3. **Consistency**: JSF components provide consistent rendering
4. **Flexibility**: Easy to style with CSS classes without semantic HTML overhead
5. **Performance**: Lighter rendering than semantic HTML headers

## Date and Time Formatting

### ❌ Do NOT Combine Separate Format Properties
```xhtml
<!-- Wrong: Don't concatenate date and time format properties -->
<f:convertDateTime pattern="#{sessionController.applicationPreference.shortDateFormat} #{sessionController.applicationPreference.shortTimeFormat}"/>
```

### ✅ Use Combined Format Properties
```xhtml
<!-- Correct: Use the combined format properties -->

<!-- For short date and time -->
<f:convertDateTime pattern="#{sessionController.applicationPreference.shortDateTimeFormat}"/>

<!-- For long date and time -->
<f:convertDateTime pattern="#{sessionController.applicationPreference.longDateTimeFormat}"/>

<!-- For date only (short) -->
<f:convertDateTime pattern="#{sessionController.applicationPreference.shortDateFormat}"/>

<!-- For date only (long) -->
<f:convertDateTime pattern="#{sessionController.applicationPreference.longDateFormat}"/>

<!-- For time only -->
<f:convertDateTime pattern="#{sessionController.applicationPreference.shortTimeFormat}"/>
```

### Available Date/Time Format Properties
- `shortDateTimeFormat` - Short date with time (e.g., "dd/MM/yyyy HH:mm")
- `longDateTimeFormat` - Long date with time (e.g., "dd MMMM yyyy HH:mm:ss")
- `shortDateFormat` - Short date only (e.g., "dd/MM/yyyy")
- `longDateFormat` - Long date only (e.g., "dd MMMM yyyy")
- `shortTimeFormat` - Time only (e.g., "HH:mm")

### Reason
- Application preferences already provide combined format patterns
- Combining separate properties manually can cause formatting issues
- Using the predefined combined formats ensures consistency across the application
- Format patterns are centrally managed and can be changed application-wide

## Data Table Alignment

### ❌ Do NOT Use Old Bootstrap Classes
```xhtml
<!-- Wrong: Old Bootstrap alignment classes -->
<p:column styleClass="text-right">
    <h:outputText value="#{item.amount}">
        <f:convertNumber pattern="#,##0.00"/>
    </h:outputText>
</p:column>
```

### ✅ Use Modern Bootstrap Classes
```xhtml
<!-- Correct: Use text-end for right alignment -->
<p:column headerText="Amount" styleClass="text-end" width="10em">
    <h:outputText value="#{item.amount}">
        <f:convertNumber pattern="#,##0.00"/>
    </h:outputText>
</p:column>

<!-- Other alignment classes -->
<p:column headerText="Status" styleClass="text-center" width="8em">
    <!-- Centered content -->
</p:column>

<p:column headerText="Name" styleClass="text-start" width="15em">
    <!-- Left aligned (default, usually not needed) -->
</p:column>
```

### Modern Bootstrap Alignment Classes
- `text-start` - Left align (default for LTR languages)
- `text-end` - Right align (use for numbers, amounts, quantities)
- `text-center` - Center align (use for status badges, icons)

### Best Practices for Data Tables
1. **Numeric columns**: Always use `text-end` for amounts, quantities, rates, totals
2. **Status columns**: Use `text-center` for badges, icons, status indicators
3. **Column widths**: Always specify width in `em` to prevent text wrapping (e.g., `width="10em"`)
4. **Number formatting**: Use `<f:convertNumber pattern="#,##0.00"/>` for consistent decimal display
5. **Date formatting**: Use application preference format patterns for consistency

---
This behavior should persist across all Claude Code sessions for this project.