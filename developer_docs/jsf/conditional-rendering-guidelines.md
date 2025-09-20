# JSF Conditional Rendering Guidelines

## Critical Rule: Components with IDs and AJAX Updates

### ⚠️ THE GOLDEN RULE
**NEVER put a `rendered` attribute on a component that has an `id` and will be updated via AJAX.**

### Why This Matters
When a JSF component with an `id` is not rendered initially (`rendered="false"`), JSF will not include it in the component tree. Later AJAX updates that try to update this component **will silently fail** because JSF cannot find the component to update.

## ❌ WRONG Examples

### Wrong Example 1: ID on conditionally rendered component
```xml
<!-- BAD: This will break AJAX updates -->
<h:panelGroup id="staffDetails" rendered="#{bean.staff != null}">
    <h:outputText value="#{bean.staff.name}" />
</h:panelGroup>

<p:ajax update="staffDetails" /> <!-- This will fail silently -->
```

### Wrong Example 2: Nested conditional rendering with ID
```xml
<!-- BAD: Inner component has ID but may not be rendered -->
<h:panelGroup rendered="#{bean.paymentMethod eq 'Credit'}">
    <h:panelGroup id="creditDetails" rendered="#{bean.creditCard != null}">
        <h:outputText value="#{bean.creditCard.number}" />
    </h:panelGroup>
</h:panelGroup>
```

## ✅ CORRECT Examples

### Correct Example 1: ID always rendered, content conditional
```xml
<!-- GOOD: Component with ID is always rendered -->
<h:panelGroup id="staffDetails">
    <h:panelGroup rendered="#{bean.staff != null}">
        <h:outputText value="#{bean.staff.name}" />
    </h:panelGroup>
</h:panelGroup>

<p:ajax update="staffDetails" /> <!-- This works reliably -->
```

### Correct Example 2: Multiple conditions
```xml
<!-- GOOD: Outer wrapper always rendered, inner content conditional -->
<h:panelGroup id="paymentDetails">
    <h:panelGroup rendered="#{bean.paymentMethod eq 'Credit'}">
        <h:panelGroup rendered="#{bean.creditCard != null}">
            <h:outputText value="Credit Card: #{bean.creditCard.number}" />
        </h:panelGroup>
        <h:panelGroup rendered="#{bean.creditCard == null}">
            <h:outputText value="Please select a credit card" />
        </h:panelGroup>
    </h:panelGroup>

    <h:panelGroup rendered="#{bean.paymentMethod eq 'Cash'}">
        <h:outputText value="Cash payment selected" />
    </h:panelGroup>

    <h:panelGroup rendered="#{bean.paymentMethod eq 'Staff_Welfare'}">
        <h:panelGroup rendered="#{bean.staff != null}">
            <h:outputText value="Staff: #{bean.staff.name}" />
        </h:panelGroup>
    </h:panelGroup>
</h:panelGroup>
```

### Correct Example 3: Complex form sections
```xml
<!-- GOOD: Container always exists for AJAX updates -->
<p:panel id="billingDetailsPanel">
    <h:panelGroup rendered="#{bean.paymentMethod == 'Staff_Welfare'}">
        <f:facet name="header">Staff Welfare Details</f:facet>

        <!-- Staff selection always available -->
        <p:autoComplete value="#{bean.staff}" ... />

        <!-- Details shown after selection -->
        <h:panelGroup rendered="#{bean.staff != null}">
            <h:outputText value="Entitled: #{bean.staff.welfareEntitled}" />
            <h:outputText value="Utilized: #{bean.staff.welfareUtilized}" />
        </h:panelGroup>
    </h:panelGroup>
</p:panel>

<p:ajax update="billingDetailsPanel" /> <!-- Always works -->
```

## Best Practices

### 1. Structure Your Components Properly
- **Outer wrapper**: Always rendered, has the ID for AJAX updates
- **Inner content**: Conditionally rendered based on business logic

### 2. Use Meaningful Container Components
```xml
<!-- Use semantic containers -->
<h:panelGroup id="paymentMethodDetails" class="payment-section">
    <!-- Conditional content here -->
</h:panelGroup>

<!-- Or use actual panels for better styling -->
<p:panel id="staffWelfareSection" rendered="#{bean.showStaffWelfare}">
    <h:panelGroup id="staffWelfareContent">
        <!-- Always updateable content -->
    </h:panelGroup>
</p:panel>
```

### 3. AJAX Update Patterns
```xml
<!-- Update specific sections -->
<p:ajax update="staffWelfareContent billingDetails" />

<!-- Use component resolution for deeply nested updates -->
<p:ajax update=":#{p:resolveFirstComponentWithId('staffDetails',view).clientId}" />
```

### 4. Testing Conditional Rendering
Always test these scenarios:
1. Initial page load with condition false
2. AJAX action that should make condition true
3. Multiple back-and-forth state changes
4. Page refresh in different states

### 5. Common Pitfalls to Avoid

#### Pitfall 1: Nested conditions with IDs
```xml
<!-- AVOID: Multiple levels of conditional rendering -->
<h:panelGroup rendered="#{condition1}">
    <h:panelGroup id="updateTarget" rendered="#{condition2}">
        Content
    </h:panelGroup>
</h:panelGroup>
```

#### Pitfall 2: Conditional modals/dialogs
```xml
<!-- BAD: Dialog may not exist when trying to show it -->
<p:dialog id="myDialog" rendered="#{bean.showDialog}">
    Content
</p:dialog>

<!-- GOOD: Dialog always exists, content is conditional -->
<p:dialog id="myDialog">
    <h:panelGroup rendered="#{bean.showDialog}">
        Content
    </h:panelGroup>
</p:dialog>
```

## Debugging Tips

### 1. Check Browser Console
When AJAX updates fail silently, check browser console for JSF errors like:
```
Cannot find component for expression "componentId" referenced from "source"
```

### 2. Use JSF Development Mode
Enable development mode to get better error messages:
```xml
<context-param>
    <param-name>javax.faces.PROJECT_STAGE</param-name>
    <param-value>Development</param-value>
</context-param>
```

### 3. Component Tree Analysis
Use browser developer tools to inspect the rendered HTML and verify components exist with expected IDs.

### 4. Temporary Always-Render Testing
Temporarily remove `rendered` attributes to verify AJAX updates work, then add proper conditional structure.

## Summary Checklist

- [ ] Components with IDs used in AJAX updates are always rendered
- [ ] Conditional logic is inside always-rendered containers
- [ ] AJAX update targets are predictably available
- [ ] Tested multiple state transitions
- [ ] No nested conditional rendering with IDs
- [ ] Browser console shows no JSF component resolution errors

## Real-world Example: Staff Welfare Implementation

```xml
<!-- Container always rendered for AJAX updates -->
<h:panelGroup id="welfareStaff">
    <!-- Payment method panel - conditionally shown -->
    <p:panel rendered="#{controller.paymentMethod eq 'Staff_Welfare'}">
        <f:facet name="header">Staff Welfare Selection</f:facet>

        <!-- Staff selection always available when panel shown -->
        <p:autoComplete value="#{controller.toStaff}"
                        update="staffWelfareDetails" ... />

        <!-- Details container always rendered for AJAX -->
        <h:panelGroup id="staffWelfareDetails">
            <!-- Actual details conditionally shown -->
            <h:panelGroup rendered="#{controller.toStaff != null}">
                <h:outputText value="Staff: #{controller.toStaff.name}" />
                <h:outputText value="Entitled: #{controller.toStaff.welfareEntitled}" />
                <h:outputText value="Utilized: #{controller.toStaff.welfareUtilized}" />
            </h:panelGroup>
        </h:panelGroup>
    </p:panel>
</h:panelGroup>
```

This structure ensures:
- `welfareStaff` container always exists for payment method changes
- `staffWelfareDetails` always exists for staff selection updates
- Content is conditionally displayed without breaking AJAX updates