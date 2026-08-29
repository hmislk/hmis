# Bill Navigation Guide — View, Manage, Admin

## Overview

`BillSearch` (bean `billSearch`) provides three families of navigation methods
that route to the correct page for any bill, based on its `BillTypeAtomic` value.
Always use these methods — never hardcode a page URL or load the bill yourself in
the calling page.

---

## Three Navigation Modes

| Mode | XHTML method | Purpose |
|------|-------------|---------|
| **View** | `billSearch.navigateToViewBillByAtomicBillTypeByBillId(id)` | Read-only / print |
| **Manage** | `billSearch.navigateToManageBillByAtomicBillTypeByBillId(id)` | Edit / cancel / reprint |
| **Admin** | `billSearch.navigateToAdminBillByAtomicBillTypeByBillId(id)` | Admin-level corrections |

All three accept a `Long` bill ID, look up the `BillTypeAtomic` and route accordingly.

---

## Standard XHTML Button Pattern

```xhtml
<!-- View (read-only / print) -->
<p:commandButton
    ajax="false"
    icon="fa fa-eye"
    title="View Bill"
    action="#{billSearch.navigateToViewBillByAtomicBillTypeByBillId(row.id)}" />

<!-- Manage (edit / cancel) -->
<p:commandButton
    ajax="false"
    icon="fa fa-file-invoice"
    title="Manage Bill"
    action="#{billSearch.navigateToManageBillByAtomicBillTypeByBillId(row.id)}" />

<!-- Admin -->
<p:commandButton
    ajax="false"
    icon="fa fa-tools"
    title="Admin"
    action="#{billSearch.navigateToAdminBillByAtomicBillTypeByBillId(row.id)}" />
```

Pass whatever holds the bill ID in your DTO — `row.id`, `row.billId`,
`row.referenceBillId`, etc.

**Never** create a custom navigation method just to reach a specific page — check
the switch statement first (see below).

---

## How `navigateToViewBillByAtomicBillTypeByBillId` Works

1. Fetches **only** `BILLTYPEATOMIC` from the `bill` table via a single native
   scalar query (no entity load).
2. Dispatches to the appropriate native-SQL controller or falls back to the
   entity-based path for bill types not yet migrated.

```java
// BillSearch.java ~4756
public String navigateToViewBillByAtomicBillTypeByBillId(Long BillId) {
    BillTypeAtomic bta = fetchBillTypeAtomicByNativeSql(BillId); // single scalar query
    switch (bta) {
        case PHARMACY_ISSUE:              return transferIssueNativeSqlController.viewByBillId(BillId);
        case PHARMACY_RECEIVE:            return transferReceiveNativeSqlController.viewByBillId(BillId);
        case DIRECT_ISSUE_INWARD_MEDICINE: return inpatientDirectIssueNativeSqlController.viewByBillId(BillId);
        case PHARMACY_RETAIL_SALE:        return retailSaleNativeSqlController.viewByBillId(BillId);
        case PHARMACY_TRANSFER_REQUEST_PRE:
        case PHARMACY_TRANSFER_REQUEST:   return pharmacyBillSearch.viewRequestByBillId(BillId);
        default:                          return navigateToViewBillByAtomicBillTypeByBillIdEntityBased(BillId);
    }
}
```

### Native-SQL-backed bill types (fast path)

These six types load data via DTO / native SQL — no JPA entity graph, no L2
cache involvement:

| `BillTypeAtomic` | Controller | Target page |
|-----------------|-----------|-------------|
| `PHARMACY_RETAIL_SALE` | `RetailSaleNativeSqlController` | `pharmacy_bill_retail_sale_native` |
| `PHARMACY_ISSUE` | `TransferIssueNativeSqlController` | `pharmacy_bill_transfer_issue_native` |
| `PHARMACY_RECEIVE` | `TransferReceiveNativeSqlController` | `pharmacy_bill_transfer_receive_native` |
| `DIRECT_ISSUE_INWARD_MEDICINE` | `InpatientDirectIssueNativeSqlController` | `pharmacy_bill_direct_issue_native` |
| `PHARMACY_TRANSFER_REQUEST_PRE` | `PharmacyBillSearch` | `pharmacy_reprint_transfer_request` |
| `PHARMACY_TRANSFER_REQUEST` | `PharmacyBillSearch` | `pharmacy_reprint_transfer_request` |

All other bill types fall through to the entity-based path
(`navigateToViewBillByAtomicBillTypeByBillIdEntityBased`), which loads the full
`Bill` entity and calls `navigateToViewBillByAtomicBillType()`.

### Fallback: entity-based path

For bill types not yet on the native path, `BillSearch` loads the full entity
and routes via a large switch in `navigateToViewBillByAtomicBillType()`. This
covers OPD bills, channelling, GRN, direct purchase, inward professional bills,
etc.

---

## Manage and Admin Paths

`navigateToManageBillByAtomicBillType()` handles a different set of types — OPD
bills, package bills, credit company bills, etc. It currently has no native-SQL
fast path; it always loads the full entity first.

`navigateToAdminBillByAtomicBillType()` delegates entirely to `navigateToAdminBill()`
after loading the entity — it has no type-specific switch yet.

---

## Adding Support for a New Bill Type

### View (fast path preferred)

1. Create a `viewByBillId(Long billId)` method in the relevant native controller
   that loads print data via DTO/native SQL and returns the target page with
   `faces-redirect=true`.
2. Add a `case YOUR_TYPE: return yourNativeSqlController.viewByBillId(BillId);`
   to the switch in `navigateToViewBillByAtomicBillTypeByBillId`.

### View (entity fallback)

If native SQL is not yet needed, add a case to
`navigateToViewBillByAtomicBillType()`:

```java
case YOUR_TYPE:
    yourController.setPrintPreview(true);
    yourController.setBill(bill);
    return "/your/module/your_page?faces-redirect=true";
```

### Manage / Admin

Add a case to `navigateToManageBillByAtomicBillType()` or `navigateToAdminBill()`
as appropriate.

---

## What NOT to Do

```xhtml
<!-- BAD: hardcoded page -->
<p:commandButton action="/pharmacy/pharmacy_reprint_bill_sale?faces-redirect=true" />

<!-- BAD: custom entity load + direct navigation -->
<p:commandButton action="pharmacy_reprint_bill_sale">
    <f:setPropertyActionListener value="#{searchController.loadBillById(bill.id)}"
                                 target="#{pharmacyBillSearch.bill}"/>
</p:commandButton>
```

Both patterns bypass the routing layer, break when bill types evolve, and in the
case of entity load may trigger L2 cache issues. Use the `billSearch` methods
instead.

---

## Related

- `BillSearch.java` — all routing logic
- `RetailSaleNativeSqlController.java`, `TransferIssueNativeSqlController.java`, etc. — native controllers
- [Printer Configuration System](../configuration/printer-configuration-system.md)
- [DTO Implementation Guidelines](../dto/implementation-guidelines.md)
