# JSF Navigation Patterns

## Core Rule: Initialize in Navigation Methods, Not in viewAction

**🚨 Never use `f:viewAction` or `f:event type="preRenderView"` to initialize state on `@SessionScoped` beans.**

All controllers in this project are `@SessionScoped`. Navigation is always performed via explicit Java methods that return `"/<page>?faces-redirect=true"`. That navigation method is the correct and only place to set up state for the destination page.

### Why `f:viewAction` is harmful on `@SessionScoped` beans

`f:viewAction` fires on **every GET request** to the page:

| Trigger | Effect with `f:viewAction` |
|---------|---------------------------|
| Normal navigation (nav method ran) | Runs initializer a second time — redundant at best, destructive if it resets in-progress state |
| Browser refresh | Silently resets a bill or list the user was mid-way through filling in |
| Back button | Reinitializes state the user just left, losing their work |
| Tab duplication | Resets the shared session-scoped state |

With a `@SessionScoped` bean the navigation method already ran and left the bean in exactly the right state. A `f:viewAction` firing afterward is either a no-op or a bug.

### The correct pattern

```java
// Navigation method — the only place initialization belongs
public String navigateToFundTransferBill() {
    resetClassVariables();
    prepareToAddNewFundTransferBill();
    floatTransferStarted = false;
    currentBillPayments = new ArrayList<>();
    return "/cashier/fund_transfer_bill?faces-redirect=true";
}
```

```xhtml
<!-- XHTML page — NO f:metadata / f:viewAction needed -->
<ui:define name="subcontent">
    <h:form>
        ...
    </h:form>
</ui:define>
```

### ❌ Wrong — redundant and harmful

```xhtml
<ui:define name="subcontent">
    <f:metadata>
        <f:viewAction action="#{financialTransactionController.ensureFundTransferBillInitialized()}" />
    </f:metadata>
    <h:form>
        ...
    </h:form>
</ui:define>
```

---

## When `f:viewAction` / `f:metadata` IS legitimate

There are two real use cases where `f:viewAction` is necessary:

### 1. URL parameter ingestion (`f:viewParam`)

When a page is reached via an external URL with query parameters (lab result links, patient portal, mobile API callbacks), there is no navigation method — the URL **is** the entry point. `f:viewParam` binds the URL parameter to the bean, and `f:viewAction` processes it.

```xhtml
<!-- requests/report.xhtml — reached via external URL -->
<f:metadata>
    <f:viewParam name="id" value="#{patientReportController.encryptedPatientReportId}" />
    <f:viewParam name="user" value="#{patientReportController.encryptedExpiary}" />
    <f:viewAction action="#{patientReportController.preparePatientReportByIdForRequests()}" />
</f:metadata>
```

**Signal:** the `f:metadata` block contains `f:viewParam`. If there are no `f:viewParam` elements, the `f:viewAction` is almost certainly wrong.

### 2. `@ViewScoped` beans

`@ViewScoped` beans are created fresh on each page load — there is no navigation method that ran beforehand. A `f:viewAction` or `f:event type="preRenderView"` is the correct initialization hook. (Note: most controllers in this project are `@SessionScoped`, not `@ViewScoped`.)

---

## `f:event type="preRenderView"` — same rules apply

`f:event type="preRenderView"` fires on **every render**, including postbacks (form submissions), not just on initial GET. This makes it even more aggressive than `f:viewAction`. It is only legitimate in the same two cases above, and even then `f:viewAction` is preferred as it fires only on GET by default.

---

## Known misuses to fix (tracked as issues)

The following pages use `f:viewAction` on `@SessionScoped` beans without `f:viewParam` and should be refactored to move initialization into their navigation methods:

| Page | viewAction method | Issue |
|------|-------------------|-------|
| `opd/analytics/summary_reports/bill_item_report.xhtml` | `opdReportController.generateBillItemReport()` | hmislk/hmis#19607 |
| `pharmacy/admin/lab_ampp_audit_events.xhtml` | `labAmppController.fillAmppAuditEvents` | hmislk/hmis#19608 |
| `pharmacy/admin/store_ampp_audit_events.xhtml` | `storeAmppController.fillAmppAuditEvents` | hmislk/hmis#19609 |
| `reports/inventoryReports/expiry_item.xhtml` | `pharmacyReportController.initExpiryReportDefaults()` | hmislk/hmis#19610 |
| `pharmacy/pharmacy_search_issue_bill_for_return.xhtml` | `searchController.createTableByKeywordForPharmacyDisposalIssue` | hmislk/hmis#19611 |
| `pharmacy/pharmacy_fast_retail_sale*.xhtml` (5 files) | `pharmacyFastRetailSaleController.initIfNeeded()` | hmislk/hmis#19612 |

Already fixed: `cashier/fund_transfer_bill.xhtml`, `cashier/fund_transfer_request_bill.xhtml`, `cashier/fund_transfer_request_bills_for_me.xhtml` (PR #19604).
