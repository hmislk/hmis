# Native SQL Print Page Development Guide

**Reference implementation**: Purchase Order print page (Issue #20923)  
**Last updated**: 2026-05-21

---

## Why Native SQL for Print Pages?

The standard entity-based print flow loads a full JPA entity graph on every view:
`bill → referenceBill → creater → institution / department`, `billItems → item → billItemFinanceDetails`, etc.

For a read-only print page this is wasteful. Native SQL replaces all of that with two queries
(header + items), each returning flat rows into a DTO. The JSF page binds to the DTO — no
lazy-loading, no L2 cache involvement.

---

## Step 0 — Identify the Existing Page and Print Formats

1. Find the current print/view XHTML page (e.g. `pharmacy_reprint_po.xhtml`).
2. Note whether it uses an **old dropdown paper-size selector** or the modern
   **config-button / gear-icon pattern**.
   - If it uses a dropdown: add a print config button first following
     `developer_docs/admin/config-button-implementation-guide.md`, then continue.
3. List all paper format composite components rendered on the page and read each one fully.
   Record every entity EL expression used.

---

## Step 1 — Catalogue All Entity References

Go through every composite template and the host XHTML page. For each EL expression, note:
- Which entity it comes from (`bill`, `bill.referenceBill`, `bill.creater`, etc.)
- Which field is accessed
- Whether the FK could be null in production data

Typical sources for pharmacy bills:

| Source | Example fields |
|---|---|
| `bill` (approval/main) | `deptId`, `paymentMethod`, `creditDuration`, `createdAt`, `consignment`, `comments`, `netTotal`, `approveAt`, `cancelled` |
| `bill.referenceBill` (pre-bill) | `createdAt`, `comments`, `checkeAt` |
| `bill.creater` (WebUser → Person) | `webUserPerson.name` |
| `bill.referenceBill.checkedBy` (WebUser → Person) | `webUserPerson.name` |
| `bill.referenceBill.creater` (WebUser → Department → Institution) | `institution.name`, `institution.phone`, `institution.fax`, `department.telephone1`, `department.email` |
| `bill.department` | `name`, `address`, `telephone1`, `telephone2`, `fax`, `site.name` |
| `bill.institution` | `name`, `email` |
| `bill.toInstitution` (supplier) | `name`, `code` |
| `bill.billItems[]` | `item.name`, `item.code`, `item.issueUnit.name`, `retired` |
| `billItem.billItemFinanceDetails` | `lineGrossRate`, `quantity`, `freeQuantity`, `lineGrossTotal` |
| Stock / ItemBatch / StockHistory | Only for bills that affect stock (GRN, retail sale, transfers) |

---

## Step 2 — Create DTOs

Create two DTO classes in `com.divudi.core.data.dto.pharmacy`:

- **`<Module>PrintDto`** — header-level fields (one per bill)
- **`<Module>ItemPrintDto`** — line-item fields (one per bill item)

Rules:
- Use primitive types (`double`, `boolean`, `int`) rather than boxed types for numeric fields.
- Use `String` (never null from the service — default to `""`) for text fields.
- Use `Date` for timestamps.
- Add a `List<ItemDto> items = new ArrayList<>()` field on the header DTO.
- No JPA annotations. No `@Entity`. Plain Java beans with getters/setters only.

Reference: `PurchaseOrderPrintDto` + `PurchaseOrderItemPrintDto`.

---

## Step 3 — Create the Native SQL Service

Create `<Module>NativeSqlService` in `com.divudi.service.pharmacy`:
- Annotate `@Stateless`.
- Inject `@PersistenceContext(unitName = "hmisPU") EntityManager em`.
- One public method: `<ModulePrintDto> loadPrintDtoByBillId(long billId)`.
- Wrap the body in a try/catch — return `null` on any exception (caller shows error message).

### Query design rules

**Always use LEFT JOIN** for every FK — never INNER JOIN. Any null FK (e.g. a bill with no
`checkedBy` user, or a bill item with no `billItemFinanceDetails`) must **not** drop the row.

**Two-query pattern**:
1. Header query — one row joining bill → referenceBill → users → persons → departments → institutions.
2. Item query — one row per non-retired bill item, joining billitem → item → issueUnit → billitemfinancedetails.

**Null safety in Java**:
- Use a `str(Object o)` helper: `return o != null ? o.toString().trim() : "";`
- Use a `toDate(Object o)` helper that handles both `Timestamp` and `java.util.Date`.
- For numbers: `r[col] != null ? ((Number) r[col]).doubleValue() : 0.0`.
- For booleans: `r[col] != null && ((Number) r[col]).intValue() != 0`.

**Column ordering**: list columns in the SELECT in the same order as you read them in Java
(sequential `col++` index). Keep them in sync — a shifted column is a silent bug.

**Bill resolution** — some BillTypeAtomics come in pairs (request/approval). The service must
resolve which bill is which before querying, e.g.:
```java
String atomicSql = "SELECT billTypeAtomic, referenceBill_ID FROM bill WHERE ID = ?1 AND retired = 0 LIMIT 1";
em.createNativeQuery(atomicSql).setParameter(1, billId).getResultList();
```
Then branch on the atomic value to get the approval bill ID before the main query.

Reference: `PurchaseOrderNativeSqlService.java`.

---

## Step 4 — Create the Controller

Create `<Module>NativeSqlController` in `com.divudi.bean.pharmacy`:
- Annotate `@Named @SessionScoped`.
- Fields: `private <ModulePrintDto> printDto` and `private boolean printPreview`.
- Inject `SessionController` and `@EJB <Module>NativeSqlService`.
- One public navigation method:

```java
public String viewByBillId(Long billId) {
    if (billId == null) { JsfUtil.addErrorMessage("No Bill Selected"); return null; }
    makeNull();
    printDto = service.loadPrintDtoByBillId(billId);
    if (printDto == null) { JsfUtil.addErrorMessage("Bill not found"); return null; }
    printPreview = true;
    return "/pharmacy/pharmacy_reprint_<module>_native?faces-redirect=true";
}

public void makeNull() { printDto = null; printPreview = false; }
```

Reference: `PurchaseOrderNativeSqlController.java`.

---

## Step 5 — Create the Native Print Page

Copy the original XHTML page to a new file named `pharmacy_reprint_<module>_native.xhtml`.

Changes to make:
1. **Remove all composite component includes** (`xmlns:ph=...`, `<ph:xxx .../>`).
2. **Inline each format section** as a `<h:panelGroup rendered="#{configOptionController.getBooleanValueByKey(...)}">` block.
3. **Replace every entity EL** `#{pharmacyBillSearch.bill.someField}` with the flat DTO field
   `#{<module>NativeSqlController.printDto.someField}`.
4. **Keep the config dialog** — reuse the same `purchaseOrderConfigController` (or equivalent)
   and the same config keys. Use `h:selectBooleanCheckbox` (not `p:selectBooleanCheckbox`) to
   avoid dialog initialisation issues.
5. **Add a small Legacy View button** pointing back to the original page:

```xhtml
<p:commandButton
    ajax="false"
    value="Legacy View"
    title="Open original entity-based print page"
    action="#{pharmacyBillSearch.navigatePharmacyReprintPo}"
    class="ui-button-secondary ms-3"
    icon="fas fa-history"
    style="font-size: 0.75rem; padding: 0.2rem 0.5rem;"/>
```

6. **Keep the original page untouched** — do not modify it.

---

## Step 6 — Review Agent Check

After creating the native page, run a review to verify no entity references remain.
Look for:
- `#{pharmacyBillSearch.bill.` or any `#{<controller>.bill.` (entity EL chain)
- Any `ui:repeat value="#{...bill.billItems}"` (iterating entity collection)
- Any `#{bip.item.name}` style access (entity traversal inside the loop)

Also check whether any controller method called from the page internally loads a JPA entity
and exposes fields via getters. If so, those fields must also move to the DTO.

---

## Step 7 — Wire Navigation in BillSearch

In `BillSearch.java`:

1. Add an import: `import com.divudi.bean.pharmacy.<Module>NativeSqlController;`
2. Add an `@Inject` field near the other native controllers (~line 338).
3. In `navigateToViewBillByAtomicBillTypeByBillId()` (the native fast-path switch at ~line 4786),
   add cases for the active BillTypeAtomics **before** the `default:` fallthrough:

```java
case PHARMACY_ORDER:
case PHARMACY_ORDER_APPROVAL:
    return purchaseOrderNativeSqlController.viewByBillId(BillId);
```

4. In `navigateToViewBillByAtomicBillTypeByBillIdEntityBased()` (the entity-based fallback at
   ~line 5134), replace the unified block with split cases:
   - Active atomics → delegate to native controller
   - Cancelled/pre atomics → keep existing entity path

**Do not** add cases to `navigateToManageBillByAtomicBillType()` or
`navigateToAdminBillByAtomicBillType()` — those flows still use entities and are out of scope.

---

## Step 8 — QA Checklist

- [ ] All enabled paper formats render correctly.
- [ ] Null FKs (e.g. bill with no `checkedBy`) do not cause NPE or blank page — fields show empty string.
- [ ] `preparedByName` and `approvedByName` display correct names.
- [ ] PO Date comes from the request pre-bill; PO Time from the approval bill.
- [ ] Item list shows all non-retired items with correct quantities and rates.
- [ ] Net total matches the bill total in the DB.
- [ ] Legacy View button navigates to the old entity-based page.
- [ ] Cancelled POs (`PHARMACY_ORDER_CANCELLED`, `PHARMACY_ORDER_APPROVAL_CANCELLED`) still
      route to the old page.
- [ ] Settings dialog saves and reloads config correctly.

---

## Common Mistakes

### 1. INNER JOIN drops bills with null FKs

```sql
-- WRONG: any bill with no checkedBy user silently disappears
JOIN webuser checkedByWU ON checkedByWU.ID = rb.checkedBy_ID

-- CORRECT
LEFT JOIN webuser checkedByWU ON checkedByWU.ID = rb.checkedBy_ID
```

### 2. Column index off-by-one

If the Java column reader skips or double-reads a column (sequential `col++` drift), all
subsequent values are wrong. Keep SELECT column order and Java read order perfectly in sync.

### 3. Iterating entity collection in the native page

`<ui:repeat value="#{controller.printDto.items}">` is correct.  
`<ui:repeat value="#{pharmacyBillSearch.bill.billItems}">` loads the JPA entity — remove it.

### 4. p:selectBooleanCheckbox in config dialog

Use `h:selectBooleanCheckbox` not `p:selectBooleanCheckbox` in dialogs. PrimeFaces checkboxes
load incorrectly initially when rendered inside a dialog. See `jsf-ajax` skill.

### 5. Native controller is SessionScoped but printDto from a prior navigation bleeds in

Always call `makeNull()` at the start of `viewByBillId()` to clear stale state.

### 6. Named parameters in native queries cause a SQL syntax error

EclipseLink's `createNativeQuery` does **not** support named parameters (`:name`). Use positional
parameters `?1`, `?2`, ... and set them with `.setParameter(1, value)`.

```java
// WRONG — EclipseLink throws SQLSyntaxErrorException at runtime
em.createNativeQuery("SELECT ... WHERE ID = :id").setParameter("id", billId)

// CORRECT
em.createNativeQuery("SELECT ... WHERE ID = ?1").setParameter(1, billId)
```

---

## Reference Files

| File | Purpose |
|---|---|
| `com/divudi/core/data/dto/pharmacy/PurchaseOrderPrintDto.java` | Reference header DTO |
| `com/divudi/core/data/dto/pharmacy/PurchaseOrderItemPrintDto.java` | Reference item DTO |
| `com/divudi/service/pharmacy/PurchaseOrderNativeSqlService.java` | Reference native service |
| `com/divudi/bean/pharmacy/PurchaseOrderNativeSqlController.java` | Reference controller |
| `src/main/webapp/pharmacy/pharmacy_reprint_po_native.xhtml` | Reference native page |
| `developer_docs/billing/bill-preview-navigation-guide.md` | BillSearch routing overview |
| `developer_docs/pharmacy/native-sql-bill-migration-guide.md` | Settlement (write) side native SQL guide |
| `developer_docs/admin/config-button-implementation-guide.md` | Config button / gear icon pattern |
