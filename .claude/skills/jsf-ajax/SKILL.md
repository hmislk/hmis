---
name: jsf-ajax
description: >
  JSF AJAX update rules for the HMIS project. Use when working on AJAX updates,
  p:commandButton update attributes, PrimeFaces AJAX callbacks, partial page rendering,
  or debugging AJAX update failures. Also covers JSF navigation patterns: why
  f:viewAction must not be used on @SessionScoped beans, and how initialization
  belongs in navigation methods. Critical rules to prevent silent AJAX failures
  and refresh/back-button state corruption.
user-invocable: true
---

# JSF AJAX Update Guidelines

## Critical Rules

1. **AJAX UPDATE RULE**: NEVER use plain HTML elements (div, span) with id attributes for AJAX updates - use JSF components (`h:panelGroup`, `p:outputPanel`) instead
2. **RENDERED ATTRIBUTE RULE**: NEVER use `rendered` on plain HTML elements - JSF ignores it; use `h:panelGroup layout="block"` instead
3. **COMPONENT REFERENCES**: Use `p:resolveFirstComponentWithId` for updates: `update=":#{p:resolveFirstComponentWithId('componentId',view).clientId}"`
4. **NO CSS/jQuery SELECTORS**: NEVER use `@(.class)`, `@(#id)`, `@parent` in `update` or `process` attributes. Use `@this`, `@form`, explicit IDs, or `:#{p:resolveFirstComponentWithId(...)}`

## Wrong vs Correct

```xhtml
<!-- WRONG - Plain HTML, AJAX silently fails -->
<div id="stockSelection">
    <!-- content -->
</div>
<p:commandButton update="stockSelection" />

<!-- CORRECT - JSF component, AJAX works -->
<h:panelGroup id="stockSelection">
    <div><!-- content --></div>
</h:panelGroup>
<p:commandButton update="stockSelection" />
```

## Updating Growl/Messages

The growl component is in `template.xhtml` outside forms. Use absolute ID with colon prefix:

```xhtml
<!-- CORRECT -->
<p:commandButton action="#{bean.save}" update="myTable :growl" />

<!-- WRONG - Do NOT use CSS selectors -->
<p:commandButton update="@(.ui-growl)" />
```

## JSF Components for AJAX Updates

- `h:panelGroup` - Lightweight wrapper, no HTML output
- `p:outputPanel` - PrimeFaces panel, renders as `<span>` or `<div>`
- `h:div` - Renders as HTML `<div>`
- `h:form` - For updating entire form sections
- `p:panel` - Full-featured panel with header/footer

## Debugging

1. Check browser console for JavaScript errors
2. Verify target element is a JSF component (not plain HTML)
3. Use browser dev tools to confirm JSF-generated id
4. Test with `h:panelGroup` wrapper if updates fail
5. Check component hierarchy - nested components affect id resolution

For complete reference, read [developer_docs/jsf/ajax-update-guidelines.md](../../developer_docs/jsf/ajax-update-guidelines.md).

---

## Navigation Pattern: Never Use f:viewAction on @SessionScoped Beans

**🚨 Most controllers in this project are `@SessionScoped`. Never use `f:viewAction` or `f:event type="preRenderView"` to initialize state on `@SessionScoped` beans.**

`f:viewAction` fires on every GET — including browser refresh and back-button — silently resetting in-progress state. All initialization belongs in the navigation method that redirects to the page.

### Correct pattern

```java
// Navigation method — initialize here
public String navigateToFundTransferBill() {
    resetClassVariables();
    prepareToAddNewFundTransferBill();
    currentBillPayments = new ArrayList<>();
    return "/cashier/fund_transfer_bill?faces-redirect=true";
}
```

```xhtml
<!-- XHTML — no f:metadata needed -->
<ui:define name="subcontent">
    <h:form>...</h:form>
</ui:define>
```

### The two legitimate uses of f:viewAction

1. **URL parameter ingestion** — page is reached via external URL with `f:viewParam` query params (lab result links, mobile API, patient portal). No navigation method exists; the URL is the entry point. **Signal: `f:metadata` contains `f:viewParam` elements.**

2. **`@ViewScoped` beans** — bean is created fresh on each page load, so there is no prior navigation method. (Rare in this project — most controllers are `@SessionScoped`.)

If you see `f:viewAction` without any `f:viewParam`, it is almost certainly wrong.

For complete reference, read [developer_docs/jsf/navigation-patterns.md](../../developer_docs/jsf/navigation-patterns.md).
