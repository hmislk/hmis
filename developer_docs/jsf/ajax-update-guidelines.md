# JSF AJAX Update Guidelines

## Critical Rules for Claude Code

**🚨 These rules MUST be followed when working on JSF/XHTML:**

1. **AJAX UPDATE RULE**: NEVER use plain HTML elements (div, span, etc.) with id attributes for AJAX updates - use JSF components (h:panelGroup, p:outputPanel, etc.) instead
2. **RENDERED ATTRIBUTE RULE**: NEVER use `rendered` attribute on plain HTML elements - JSF ignores it; use JSF components like `h:panelGroup` with `layout="block"` instead
3. **PRIMEFACES COMPONENT REFERENCES**: Use PrimeFaces `p:resolveFirstComponentWithId` function for component updates: `update=":#{p:resolveFirstComponentWithId('componentId',view).clientId}"`
4. **AJAX SELECTORS**: NEVER use PrimeFaces CSS/jQuery selectors like `@(.class)`, `@(#id)`, `@parent`, etc. in `update` or `process` attributes. Use `@this`, `@form`, explicit component IDs, or `:#{p:resolveFirstComponentWithId('id',view).clientId}`

---

## Critical Rule: AJAX Updates Require JSF Components

### ❌ WRONG - Plain HTML with id attribute
```xhtml
<div id="stockSelection">
    <!-- content -->
</div>

<p:commandButton update="stockSelection" />
```

### ✅ CORRECT - JSF component with id attribute
```xhtml
<h:panelGroup id="stockSelection">
    <div>
        <!-- content -->
    </div>
</h:panelGroup>

<p:commandButton update="stockSelection" />
```

## The Problem

When using JSF AJAX updates (like `update="someId"` in PrimeFaces components), the target element **MUST** be a JSF component, not a plain HTML element. Plain HTML elements with `id` attributes cannot be targeted by JSF AJAX updates.

## What Happens

- **Plain HTML elements**: AJAX update silently fails, no error thrown
- **JSF components**: AJAX update works correctly, DOM is updated

## Solutions

### 1. Use h:panelGroup (Recommended)
```xhtml
<h:panelGroup id="targetArea">
    <div class="row">
        <!-- your content -->
    </div>
</h:panelGroup>
```

### 2. Use p:outputPanel
```xhtml
<p:outputPanel id="targetArea">
    <div class="row">
        <!-- your content -->
    </div>
</p:outputPanel>
```

### 3. Use h:div (JSF 2.2+)
```xhtml
<h:div id="targetArea" styleClass="row">
    <!-- your content -->
</h:div>
```

## Common JSF Components for AJAX Updates

- `h:panelGroup` - Lightweight wrapper, no HTML output by default
- `p:outputPanel` - PrimeFaces panel, renders as `<span>` or `<div>`
- `h:div` - Renders as HTML `<div>` element
- `h:form` - For updating entire form sections
- `p:panel` - Full-featured panel with header/footer support

## Best Practices

1. **Always use JSF components** for elements that need AJAX updates
2. **Use h:panelGroup** when you just need a lightweight wrapper
3. **Use p:outputPanel** when you need block-level updates
4. **Test AJAX updates** during development to ensure they work
5. **Avoid mixing plain HTML ids** with JSF AJAX update attributes

## Real-World Example

### Before (Broken)
```xhtml
<div class="row" id="stockSelection">
    <p:dataTable rendered="#{bean.showTable}" />
</div>

<p:commandButton action="#{bean.loadData}" update="stockSelection" />
```

### After (Working)
```xhtml
<h:panelGroup id="stockSelection">
    <div class="row">
        <p:dataTable rendered="#{bean.showTable}" />
    </div>
</h:panelGroup>

<p:commandButton action="#{bean.loadData}" update="stockSelection" />
```

## Debugging AJAX Update Issues

1. **Check browser console** for JavaScript errors
2. **Verify target element** is a JSF component
3. **Use browser dev tools** to confirm element has proper JSF-generated id
4. **Test with simple h:panelGroup** wrapper if updates aren't working
5. **Check component hierarchy** - nested components may affect id resolution

## Updating Growl/Messages Component

The growl component is defined in the main template (`template.xhtml`) outside of forms. To update it from AJAX actions:

### ✅ CORRECT - Use absolute ID with colon prefix
```xhtml
<p:commandButton
    action="#{bean.save}"
    update="myTable :growl" />
```

### ❌ WRONG - CSS/jQuery selector patterns (not used in this project)
```xhtml
<!-- DO NOT USE - These patterns are not supported in this project -->
<p:commandButton update="@(.ui-growl)" />
<p:commandButton update="@(#growl)" />
<p:commandButton update="@parent" />
```

**Note**: `@this` and `@form` are allowed and work correctly.

### Why `:growl` Works
- The colon prefix (`:`) indicates an absolute client ID from the view root
- The growl component has `id="growl"` in template.xhtml and is outside any form
- This pattern is used consistently throughout the codebase

### Examples from Codebase
```xhtml
<!-- From channel_booking.xhtml -->
update="dialogContentForCancellation growl"

<!-- From user_roles.xhtml -->
update="lstItems focusForList actions growl"

<!-- From ward_pharmacy_bht_issue_request_bill.xhtml -->
update="growl :#{p:resolveFirstComponentWithId('tblBillItem',view).clientId}"
```

## Related JSF Concepts

- **Component Tree**: Only JSF components are part of the component tree
- **Partial Response**: AJAX updates work through JSF's partial response mechanism
- **Client IDs**: JSF generates client-side IDs that may differ from component IDs
- **Naming Containers**: Forms and other containers affect final client IDs