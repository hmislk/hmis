# JSF AJAX Update Guidelines

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

## Related JSF Concepts

- **Component Tree**: Only JSF components are part of the component tree
- **Partial Response**: AJAX updates work through JSF's partial response mechanism  
- **Client IDs**: JSF generates client-side IDs that may differ from component IDs
- **Naming Containers**: Forms and other containers affect final client IDs