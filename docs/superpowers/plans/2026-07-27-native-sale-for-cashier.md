# Native-SQL Sale for Cashier — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the entity-based "Sale for Cashier" pharmacy page with a native-SQL
implementation that avoids the EAGER `Stock → ItemBatch → Item` cascade load, at 100%
functional parity.

**Architecture:** Clone `RetailSaleNativeSqlController` / `RetailSaleNativeSqlService`, collapse
the two-bill (PreBill + BilledBill + Payment) settle into the cashier's single-bill
(`PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER`, no Payment rows) settle, then re-apply the
six cashier-specific deltas identified by the inventory diff in the spec.

**Tech Stack:** Java EE (CDI `@SessionScoped`, EJB `@Stateless`), JPA/EclipseLink with native
SQL via `EntityManager.createNativeQuery`, JSF 2.x + PrimeFaces, Maven, MySQL, Payara.

**Spec:** `docs/superpowers/specs/2026-07-27-native-sale-for-cashier-design.md`
**Issue:** #20261 — **Master:** #22442 (Phase 2) — **Branch:** `20261-native-sale-for-cashier`

## Global Constraints

- **No unit-test harness exists for these controllers.** Verification per task is
  `mvn -q compile` (must exit 0); functional verification is the DB-backed E2E in Task 9.
  Do not invent test files — there is no test source tree for this layer.
- **JPQL first, native SQL last** (CLAUDE.md). Native SQL here is pre-authorised: it is the
  entire point of the migration and mirrors `RetailSaleNativeSqlService`.
- **Never modify existing constructors.** DTO changes are additive fields + accessors only.
- **Never rename or "fix" existing typos** (e.g. `descreption`, `purcahseRate`) — DB column
  compatibility depends on them.
- **Never use `ui:fragment`** — use `h:panelGroup rendered="..."`. `p:repeat` does not exist;
  use `ui:repeat`.
- **`persistence.xml` stays untouched.** It carries local JNDI (`jdbc/coop`,
  `jdbc/ruhunuAudit`) as an unstaged modification. Never stage or commit it.
- **Bill type:** `BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER`,
  `BillType.PharmacyPre`. Bill-number suffix `SCPB`.
- Commit messages end with `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>` and
  reference `#20261`.

---

## File Structure

| File | Responsibility |
|---|---|
| `src/main/java/com/divudi/core/data/dto/PrintBillData.java` | **Modify.** Add `billIdStr`, `cancelled` — needed by token composites (barcode, cancelled marker). |
| `src/main/java/com/divudi/service/pharmacy/RetailSaleForCashierNativeSqlService.java` | **Create.** Single-bill native settle: BillItem + fully-populated PBI, stock deduct, StockHistory, BFD/BIFD, totals. No BilledBill, no Payment. |
| `src/main/java/com/divudi/bean/pharmacy/RetailSaleForCashierNativeSqlController.java` | **Create.** `@Named @SessionScoped` page controller: cart, validation, token integration, qty helpers, print-data assembly. |
| `src/main/webapp/resources/pharmacy/*_native.xhtml` (6) | **Create.** Native token + cashier bill print composites (`phi:` namespace). |
| `src/main/webapp/resources/pharmacy/print/*_native.xhtml` (3) | **Create.** Native cashier custom bill formats (`pp:` namespace). |
| `src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier_native.xhtml` | **Create.** The page: cart entry, token panel, qty helpers, inline print panel, printer-config dialog. |
| `src/main/webapp/resources/ezcomp/menu.xhtml` | **Modify.** Retarget "Sale for cashier"; add "Sale for cashier (Legacy)". |

---

## Task 1: Extend `PrintBillData` for token composites

**Files:**
- Modify: `src/main/java/com/divudi/core/data/dto/PrintBillData.java`

**Interfaces:**
- Consumes: nothing.
- Produces: `PrintBillData.getBillIdStr()` / `setBillIdStr(String)`,
  `PrintBillData.isCancelled()` / `setCancelled(boolean)`. Tasks 2, 3, 4 rely on these.

**Why:** `saleBillToken.xhtml:132` renders `<p:barcode value="#{cc.attrs.bill.idStr}">` and
line 33 renders `**Cancelled**` on `#{cc.attrs.bill.cancelled}`. `PrintBillData` has neither,
so the native token composites cannot be written without them.

- [ ] **Step 1: Add the fields**

In the "Bill identity" block (after `private String creatorName;`, currently line 25):

```java
    private String billIdStr;
    private boolean cancelled;
```

- [ ] **Step 2: Add the accessors**

After the `creatorName` accessors:

```java
    public String getBillIdStr() { return billIdStr; }
    public void setBillIdStr(String billIdStr) { this.billIdStr = billIdStr; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
```

Do **not** touch any existing constructor (CLAUDE.md rule) — `PrintBillData` uses the
implicit no-arg constructor plus setters, so nothing else changes.

- [ ] **Step 3: Compile**

Run: `mvn -q compile`
Expected: exit code 0, no output.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/core/data/dto/PrintBillData.java
git commit -m "feat(pharmacy): add billIdStr and cancelled to PrintBillData

Needed by the native Sale for Cashier token print composites, which render
a Code128 barcode from bill.idStr and a Cancelled marker from bill.cancelled.
Additive fields only; no constructor changed.

Refs #20261

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 2: `RetailSaleForCashierNativeSqlService`

**Files:**
- Create: `src/main/java/com/divudi/service/pharmacy/RetailSaleForCashierNativeSqlService.java`
- Reference (do not modify): `src/main/java/com/divudi/service/pharmacy/RetailSaleNativeSqlService.java`

**Interfaces:**
- Consumes: `PrintBillData` (Task 1), `BillItemData`, `PreBill`, `PaymentMethod`,
  `PaymentMethodData`, `PaymentScheme`.
- Produces:
  ```java
  public void settle(PreBill preBill, List<BillItemData> items,
                     PaymentMethod paymentMethod, PaymentMethodData paymentMethodData,
                     PaymentScheme paymentScheme)

  public Object[] loadViewDataByBillId(long billId)   // [PrintBillData, List<BillItemData>]
  ```
  Task 3 calls both.

**Method:** copy `RetailSaleNativeSqlService.java` verbatim, then apply the deletions and
edits below. Copying first preserves the table-name resolution helpers
(`resolveTable`/`stockTable()`/…), `deductStock`, `fetchStockQty`, `computeAggregates`,
`insertStockHistory`, `insertFinanceDetails` and `safeStr` — all of which carry
production fixes and must not be retyped.

- [ ] **Step 1: Copy the source file**

```bash
cp src/main/java/com/divudi/service/pharmacy/RetailSaleNativeSqlService.java \
   src/main/java/com/divudi/service/pharmacy/RetailSaleForCashierNativeSqlService.java
```

- [ ] **Step 2: Rename the class and logger**

```bash
sed -i 's/\bRetailSaleNativeSqlService\b/RetailSaleForCashierNativeSqlService/g' \
   src/main/java/com/divudi/service/pharmacy/RetailSaleForCashierNativeSqlService.java
```

This is safe to run unanchored here — the file contains no other numbered or
similarly-prefixed identifier. (Contrast the page copies in Phase 4, which need the guarded
replace from the multi-window guide.)

- [ ] **Step 3: Replace the class javadoc**

Replace the existing class-level comment with:

```java
/**
 * Native-SQL settle path for the pharmacy "Sale for Cashier" page.
 *
 * Unlike RetailSaleNativeSqlService this writes a SINGLE bill — a PreBill of
 * BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER. There is no BilledBill
 * and there are no Payment rows: that atomic type is NO_PAYMENT / NO_FINANCE_TRANSACTIONS,
 * and the cashier settles payment later via PharmacyPreSettleController.
 *
 * Stock is still deducted here, at pharmacy time, exactly as the legacy
 * PharmacySaleForCashierController did.
 *
 * Deviation from legacy: BillFinanceDetails / BillItemFinanceDetails ARE written at settle.
 * Legacy wrote none, which is why DataAdministrationController
 * .backfillBfdForPreToSettleAtCashierBills() exists — without BFD the F15 report reads
 * totalRetailSaleValue as 0. Bills created here never need that backfill.
 *
 * Issue: #20261
 */
```

- [ ] **Step 4: Change the `settle` signature**

Replace:

```java
    public List<Payment> settle(PreBill preBill, BilledBill saleBill, List<BillItemData> items,
                          PaymentMethod paymentMethod, PaymentMethodData paymentMethodData,
                          PaymentScheme paymentScheme) {
```

with:

```java
    public void settle(PreBill preBill, List<BillItemData> items,
                       PaymentMethod paymentMethod, PaymentMethodData paymentMethodData,
                       PaymentScheme paymentScheme) {
```

`paymentMethod` / `paymentMethodData` / `paymentScheme` stay in the signature even though no
Payment rows are written — they are retained so the method shape matches its sibling and so a
future change (e.g. recording method on the bill inside the service) needs no call-site churn.
Mark them so no reader thinks they were forgotten by adding, immediately after the guard clause:

```java
        // paymentMethod / paymentMethodData / paymentScheme are intentionally unused here:
        // PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER is a NO_PAYMENT bill type. The
        // controller stamps them onto the bill's own columns via billBean.setPaymentMethodData
        // before calling settle(). No Payment entity is created.
```

- [ ] **Step 5: Delete the BilledBill persist and cross-link (Steps 1b and 1c)**

Delete this whole block:

```java
        // Step 1b: Persist BilledBill (payment will reference its ID).
        saleBill.setBillItems(null);
        saleBill.setReferenceBill(preBill);
        em.persist(saleBill);
        em.flush();
        long billId = saleBill.getId();

        // Step 1c: Cross-link PreBill → BilledBill via JPA so the L2 cache stays coherent.
        preBill.setReferenceBill(saleBill);
        em.merge(preBill);
        em.flush();
```

Change the following log line to:

```java
        LOGGER.log(Level.INFO, "[CashierNativeSettle] PreBill id={0} ms={1}",
                new Object[]{preBillId, System.currentTimeMillis() - t0});
```

- [ ] **Step 6: Make the single PBI fully populated (Step 2a)**

In the Step 2a loop, the PreBill PBI insert currently zeroes the value columns. With no
BilledBill to carry them, this single PBI is the only source of cost/retail/purchase values
for costing and F15 — so it must be populated. Replace the Step 2a PBI insert with:

```java
            // Single-bill flow: this PBI is the ONLY one, so it carries the real
            // cost/retail/purchase values. (The two-bill retail flow zeroes them here
            // because its BilledBill PBI carries them instead.)
            em.createNativeQuery(
                "INSERT INTO " + pharmBillItemTable()
                + " (billItem_ID, itemBatch_ID, stock_ID, qty, stringValue,"
                + " costRate, purchaseRate, retailRate, wholesaleRate, doe, description)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?)")
                .setParameter(1, biPreIds[i])
                .setParameter(2, d.getItemBatchId())
                .setParameter(3, d.getStockId())
                .setParameter(4, d.getPbiQty())
                .setParameter(5, d.getStringValue())
                .setParameter(6, d.getCostRate())
                .setParameter(7, d.getPurchaseRate())
                .setParameter(8, d.getRetailRate())
                .setParameter(9, d.getWholesaleRate())
                .setParameter(10, d.getDoe() != null ? new Timestamp(d.getDoe().getTime()) : null)
                .setParameter(11, d.getDescription())
                .executeUpdate();
            pbPreIds[i] = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
```

Also change the comment above the Step 2a `BillItem` insert from
`// Step 2a: Native INSERT BillItem + bare PBI on PreBill ...` to
`// Step 2: Native INSERT BillItem + fully-populated PBI on the single PreBill`.

- [ ] **Step 7: Delete the duplicate BillItem/PBI insert (Step 2b)**

Delete the entire `// Step 2b: ...` block — the `long[] biIds` / `long[] pbIds` declarations
and the loop that inserts into `billItemTable()` and `pharmBillItemTable()` against `billId`.

- [ ] **Step 8: Trim the L2 cache eviction**

Delete the `cache.evict(BilledBill.class);` line. Keep the `StockHistory`, `Stock`,
`BillItem`, `Bill` and `PreBill` evictions.

- [ ] **Step 9: Point finance details at the PreBill (Step 4)**

Replace:

```java
        double[] billTotals = insertFinanceDetails(billId, biIds, pbIds, items);
```

with:

```java
        // Deviation from legacy (which wrote no finance details for cashier bills):
        // BFD/BIFD are written against the single PreBill so F15's totalRetailSaleValue
        // is correct without backfillBfdForPreToSettleAtCashierBills. Issue #20261.
        double[] billTotals = insertFinanceDetails(preBillId, biPreIds, pbPreIds, items);
```

- [ ] **Step 10: Update the totals statement (Step 5) for one bill**

Replace:

```java
        em.createNativeQuery(
                "UPDATE " + billTable() + " SET total=?, netTotal=?, DISCOUNT=? WHERE ID=? OR ID=?")
                .setParameter(1, billTotals[0])
                .setParameter(2, billTotals[1])
                .setParameter(3, billTotals[2])
                .setParameter(4, preBillId)
                .setParameter(5, billId)
                .executeUpdate();
```

with:

```java
        em.createNativeQuery(
                "UPDATE " + billTable() + " SET total=?, netTotal=?, DISCOUNT=? WHERE ID=?")
                .setParameter(1, billTotals[0])
                .setParameter(2, billTotals[1])
                .setParameter(3, billTotals[2])
                .setParameter(4, preBillId)
                .executeUpdate();
```

- [ ] **Step 11: Delete the Payment insert (Step 6) and its helpers**

Delete the `// Step 6: ...` block and the `return payments;` statement. Replace the trailing
log with:

```java
        LOGGER.log(Level.INFO, "[CashierNativeSettle] DONE items={0} ms={1}",
                new Object[]{items.size(), System.currentTimeMillis() - t0});
    }
```

Then delete the now-unreachable private methods `insertPayment(...)` and
`insertMultiplePayments(...)`, and delete the `buildPrintPaymentLinesFromPersisted(Bill)`
helper only if `loadViewDataByBillId` does not call it — check first:

```bash
grep -n "buildPrintPaymentLinesFromPersisted" \
  src/main/java/com/divudi/service/pharmacy/RetailSaleForCashierNativeSqlService.java
```

If `loadViewDataByBillId` calls it, keep it: reprinting a persisted cashier bill still needs
to show the payment breakdown stored on the bill's own columns.

- [ ] **Step 12: Remove now-unused imports**

Remove imports that no longer resolve to a use — at minimum `BilledBill` and, if Step 11
removed every reference, `Payment`. Verify with:

```bash
grep -n "BilledBill\|Payment" \
  src/main/java/com/divudi/service/pharmacy/RetailSaleForCashierNativeSqlService.java
```

- [ ] **Step 13: Compile**

Run: `mvn -q compile`
Expected: exit code 0. If it fails on an unused-variable or missing-symbol error, the most
likely cause is a leftover `billId` / `biIds` / `pbIds` reference from an incompletely deleted
Step 2b or Step 6 block.

- [ ] **Step 14: Commit**

```bash
git add src/main/java/com/divudi/service/pharmacy/RetailSaleForCashierNativeSqlService.java
git commit -m "feat(pharmacy): native-SQL settle service for Sale for Cashier

Single-bill settle: one PreBill of PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER,
no BilledBill and no Payment rows (NO_PAYMENT bill type). Stock is still
deducted at pharmacy time.

- PBI fully populated: it is the only one, so it carries cost/retail/purchase
- BFD/BIFD written at settle, removing the need for the F15 backfill
- Payment insert and multiple-payment helpers dropped

Refs #20261

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 3: `RetailSaleForCashierNativeSqlController`

**Files:**
- Create: `src/main/java/com/divudi/bean/pharmacy/RetailSaleForCashierNativeSqlController.java`
- Reference: `src/main/java/com/divudi/bean/pharmacy/RetailSaleNativeSqlController.java`,
  `src/main/java/com/divudi/bean/pharmacy/PharmacySaleForCashierController.java`

**Interfaces:**
- Consumes: `RetailSaleForCashierNativeSqlService.settle(...)` (Task 2),
  `PrintBillData.setBillIdStr/setCancelled` (Task 1).
- Produces (EL-visible on bean name `retailSaleForCashierNativeSqlController`):
  ```java
  String navigateToPharmacyBillForCashierNativeFromMenu()
  String settleBillWithPay()                 // returns null; renders inline print panel
  void   addBillItem()
  void   removeBillItem(BillItemData)
  void   removeSelectedBillItems()
  void   quantityInTableChangeEvent(RowEditEvent)
  void   multiplyQuantityByTwo(BillItemData)
  void   divideQuantityByHalf(BillItemData)
  void   multiplyAllQuantitiesByTwo()
  void   divideAllQuantitiesByHalf()
  List<StockDTO> completeAvailableStockOptimizedDtoFilteredByDepartmentType(String)
  void   resetAll()
  PrintBillData getPrintBill()
  List<BillItemData> getPrintBillItems()
  Token  getCurrentToken()
  ```
  Task 7 (page) binds these.

- [ ] **Step 1: Copy the source file**

```bash
cp src/main/java/com/divudi/bean/pharmacy/RetailSaleNativeSqlController.java \
   src/main/java/com/divudi/bean/pharmacy/RetailSaleForCashierNativeSqlController.java
sed -i 's/\bRetailSaleNativeSqlController\b/RetailSaleForCashierNativeSqlController/g; s/\bRetailSaleNativeSqlService\b/RetailSaleForCashierNativeSqlService/g' \
   src/main/java/com/divudi/bean/pharmacy/RetailSaleForCashierNativeSqlController.java
```

The bean name is derived from the class name (the class uses a bare `@Named`), giving
`retailSaleForCashierNativeSqlController`. Do not add an explicit `@Named("...")` value —
the sibling controllers rely on derivation and Phase 4's numbered copies depend on it.

- [ ] **Step 2: Verify the bean name is unique**

```bash
grep -rn "retailSaleForCashierNativeSqlController" src/main/java/ src/main/webapp/
```
Expected at this point: only the class file (no EL references yet). No other Java class may
declare the same derived name.

- [ ] **Step 3: Switch the bill types**

In `buildPreBill()`:

```java
        pb.setBillType(BillType.PharmacyPre);
        pb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
```

- [ ] **Step 4: Delete `buildSaleBill(PreBill)` entirely**

It builds the `BilledBill` that no longer exists. Remove the whole method and the
`BilledBill` import.

- [ ] **Step 5: Replace the bill-number strategies**

Replace the body of `generateBillNumber()` with the cashier's four strategies, taken from
`PharmacySaleForCashierController.savePreBillFinallyForRetailSaleForCashier()` (line 2999):

```java
    private String generateBillNumber() {
        if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            return billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            return billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        } else if (configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Pharmacy Sale Cashier Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            return billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        }
        return billNumberGenerator.departmentBillNumberGenerator(
                sessionController.getDepartment(), BillType.PharmacyPre,
                BillClassType.PreBill, BillNumberSuffix.SALE);
    }
```

Add imports `com.divudi.core.data.BillClassType` and `com.divudi.core.data.BillNumberSuffix`.
Before compiling, confirm the exact `departmentBillNumberGenerator` overload:

```bash
grep -n "departmentBillNumberGenerator\b" src/main/java/com/divudi/ejb/BillNumberGenerator.java
```

- [ ] **Step 6: Rewrite the settle call**

In `settleBillWithPay()`, replace from `PreBill preBillEntity = buildPreBill();` through the
end of the `try` block with:

```java
        PreBill preBillEntity = buildPreBill();

        // Stamp the multiple-payment breakdown onto the bill's own columns. No Payment
        // entity is created for a NO_PAYMENT bill type; this is what the cashier-side
        // settle page later reads. Mirrors legacy savePreBillFinallyForRetailSaleForCashier.
        preBillEntity.setCashPaid(cashPaid);
        billBean.setPaymentMethodData(preBillEntity, paymentMethod, getPaymentMethodData());

        // Stamp dept/institution IDs on each item (needed by the native service for
        // StockHistory aggregates).
        long deptId = sessionController.getLoggedUser().getDepartment().getId();
        long instId = sessionController.getLoggedUser().getDepartment().getInstitution().getId();
        for (BillItemData bid : billItemDataList) {
            bid.setDepartmentId(deptId);
            bid.setInstitutionId(instId);
        }

        try {
            nativeSqlService.settle(preBillEntity, billItemDataList,
                    paymentMethod, getPaymentMethodData(), paymentScheme);

            settleTokenIfEnabled(preBillEntity);

            buildPrintBill(preBillEntity);
            clearBill();
            clearBillItem();
            billPreview = true;
            billSettlingStarted = false;
            JsfUtil.addSuccessMessage("Bill settled successfully.");
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Native sale-for-cashier settle failed", e);
            billSettlingStarted = false;
            JsfUtil.addErrorMessage("Failed to settle bill: " + e.getMessage());
        }
        return null;
```

Note what is deliberately **removed**: `paymentService.updateBalances(payments)` and
`drawerController.updateDrawerForIns(payments)`. No money changes hands on this page — the
cashier's drawer is updated later by `PharmacyPreSettleController`. Calling them here would
double-count. Remove the `DrawerController` and `PaymentService` injections and the `Payment`
import if nothing else uses them.

Add the injection needed by `setPaymentMethodData`:

```java
    @EJB
    private com.divudi.ejb.BillBeanController billBean;
```

Confirm the correct type and method signature first:

```bash
grep -rn "setPaymentMethodData" src/main/java/com/divudi/ejb/BillBeanController.java \
  src/main/java/com/divudi/bean/common/BillBeanController.java 2>/dev/null | head
```

Use whichever class actually declares it, matching how `PharmacySaleForCashierController`
injects it (`getBillBean()` there).

- [ ] **Step 7: Add the token integration**

Port from `PharmacySaleForCashierController` lines 3852-3872 and `settlePharmacyToken`
(line 3530). Add the field, accessors and this method:

```java
    private Token currentToken;

    /**
     * Cashier-page token integration, gated on config
     * "Enable token system in sale for cashier". Ported from
     * PharmacySaleForCashierController.settlePreBillAndNavigateToPrint().
     */
    private void settleTokenIfEnabled(Bill settledBill) {
        if (!configOptionController.getBooleanValueByKey("Enable token system in sale for cashier", false)) {
            return;
        }
        if (patient == null) {
            return;
        }
        Token existing = tokenController.findPharmacyTokens(settledBill);
        if (existing == null) {
            Token saleForCashierToken = tokenController.findPharmacyTokenSaleForCashier(
                    settledBill, TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER);
            if (saleForCashierToken == null) {
                settlePharmacyToken(TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER, settledBill);
            }
            markInprogress();
        } else {
            markToken();
        }
        if (currentToken != null) {
            currentToken.setBill(settledBill);
            tokenFacade.edit(currentToken);
        }
    }
```

Copy `settlePharmacyToken`, `markToken` and `markInprogress` from
`PharmacySaleForCashierController` verbatim, adding the `Bill` parameter to
`settlePharmacyToken` so it does not depend on a `getPreBill()` field that this controller
uses differently. Add injections `TokenController tokenController` and `TokenFacade
tokenFacade`, and imports for `Token`, `TokenType`.

- [ ] **Step 8: Add the four qty helpers**

Ported from `PharmacySaleForCashierController`, adapted to `BillItemData` (the native cart
row) instead of `BillItem`:

```java
    public void multiplyQuantityByTwo(BillItemData bid) {
        if (bid == null) {
            return;
        }
        bid.setQty(bid.getQty() * 2);
        recalculateRow(bid);
        calTotal();
    }

    public void divideQuantityByHalf(BillItemData bid) {
        if (bid == null) {
            return;
        }
        bid.setQty(bid.getQty() / 2);
        recalculateRow(bid);
        calTotal();
    }

    public void multiplyAllQuantitiesByTwo() {
        if (billItemDataList == null) {
            return;
        }
        for (BillItemData bid : billItemDataList) {
            bid.setQty(bid.getQty() * 2);
            recalculateRow(bid);
        }
        calTotal();
    }

    public void divideAllQuantitiesByHalf() {
        if (billItemDataList == null) {
            return;
        }
        for (BillItemData bid : billItemDataList) {
            bid.setQty(bid.getQty() / 2);
            recalculateRow(bid);
        }
        calTotal();
    }
```

`recalculateRow(BillItemData)` must be the controller's existing per-row recalculation. Find
its real name before writing these — the native controller recalculates rows inside
`quantityInTableChangeEvent`:

```bash
grep -n "quantityInTableChangeEvent" -A 25 \
  src/main/java/com/divudi/bean/pharmacy/RetailSaleForCashierNativeSqlController.java
```

Extract that per-row logic into a private `recalculateRow(BillItemData)` and call it from both
`quantityInTableChangeEvent` and the four helpers, so quantity maths lives in exactly one
place. Also enforce the same stock-availability and qty > 0 checks the row editor applies —
doubling a quantity must not be able to exceed available stock.

- [ ] **Step 9: Add the departmentType-filtered autocomplete**

Copy `completeAvailableStockOptimizedDtoFilteredByDepartmentType(String)` from
`PharmacySaleForCashierController`, but keep this controller's `StockDTO` projection —
port the **`departmentType` predicate only**, not the legacy entity query. Start from the
existing `completeAvailableStockOptimizedDto` in this file and add the predicate:

```bash
grep -n "completeAvailableStockOptimizedDto" -A 45 \
  src/main/java/com/divudi/bean/pharmacy/RetailSaleForCashierNativeSqlController.java
grep -n "completeAvailableStockOptimizedDtoFilteredByDepartmentType" -A 45 \
  src/main/java/com/divudi/bean/pharmacy/PharmacySaleForCashierController.java
```

- [ ] **Step 10: Replace the navigation methods**

```java
    public String navigateToPharmacyBillForCashierNativeFromMenu() {
        resetAll();
        billSettlingStarted = false;
        return "/pharmacy/pharmacy_bill_retail_sale_for_cashier_native?faces-redirect=true";
    }
```

Delete `pharmacyRetailSaleNative()`. Retarget `switchToThisSaleWindow()` and
`viewByBillId(Long)` to `/pharmacy/pharmacy_bill_retail_sale_for_cashier_native`.
Keep `switchToThisSaleWindow()` — Phase 4 (#22444) needs it, and its no-reset semantics are
already correct.

- [ ] **Step 11: Populate the new print fields**

In `buildPrintBill(Bill bill)`, after `pbd.setBillNo(bill.getDeptId());`:

```java
        pbd.setBillIdStr(bill.getIdStr());
        pbd.setCancelled(bill.isCancelled());
```

Confirm `getIdStr()` and `isCancelled()` exist on `Bill` before relying on them — an
automated-tool-style guess at a boolean getter name is the exact failure CLAUDE.md warns
about:

```bash
grep -n "public String getIdStr\|public boolean isCancelled\|public Boolean getCancelled" \
  src/main/java/com/divudi/core/entity/Bill.java
```

`buildPrintPaymentLines()` needs **no change** — it already builds from the in-memory
`PaymentMethodData`, not from `Payment` entities.

- [ ] **Step 12: Update the class javadoc**

```java
/**
 * Controller for the native-SQL pharmacy "Sale for Cashier" page.
 *
 * Settles through RetailSaleForCashierNativeSqlService, writing a SINGLE PreBill of
 * BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER with no Payment rows.
 * Stock is deducted here; the cashier takes payment later via PharmacyPreSettleController.
 *
 * Derived from RetailSaleNativeSqlController (#20260) with the cashier deltas re-applied:
 * token system, qty helpers, departmentType-filtered autocomplete, cashier bill numbering.
 *
 * Issue: #20261
 */
```

- [ ] **Step 13: Compile**

Run: `mvn -q compile`
Expected: exit code 0.

- [ ] **Step 14: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/RetailSaleForCashierNativeSqlController.java
git commit -m "feat(pharmacy): native-SQL controller for Sale for Cashier

Derived from RetailSaleNativeSqlController with the cashier deltas re-applied:
token system, four qty helpers, departmentType-filtered autocomplete, and the
four cashier bill-number strategies.

Single-bill settle; drawer and balance updates deliberately omitted since no
money changes hands on this page (the cashier-side settle does that).

Refs #20261

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 4: Native token print composites (3)

**Files:**
- Create: `src/main/webapp/resources/pharmacy/saleBill_five_five_token_native.xhtml`
- Create: `src/main/webapp/resources/pharmacy/saleBillToken_native.xhtml`
- Create: `src/main/webapp/resources/pharmacy/saleBillToken_Custom_1_native.xhtml`
- Reference: the three non-`_native` files of the same names.

**Interfaces:**
- Consumes: `PrintBillData` incl. `billIdStr`/`cancelled` (Task 1), `List<BillItemData>`.
- Produces: composites `phi:saleBill_five_five_token_native`, `phi:saleBillToken_native`,
  `phi:saleBillToken_Custom_1_native`. Task 7 uses them.

**Transform recipe** — apply to each file. These are mechanical ports of existing markup;
transcribing all three verbatim into this plan would invite drift from the originals, so
work from the originals with this mapping.

- [ ] **Step 1: Copy each file to its `_native` name**

```bash
cd src/main/webapp/resources/pharmacy
for f in saleBill_five_five_token saleBillToken saleBillToken_Custom_1; do
  cp "$f.xhtml" "${f}_native.xhtml"
done
cd -
```

- [ ] **Step 2: Change the interface block in each**

Replace:

```xml
        <cc:attribute name="bill" type="com.divudi.core.entity.Bill"/>
```

with:

```xml
        <cc:attribute name="bill"      required="true"  type="com.divudi.core.data.dto.PrintBillData" />
        <cc:attribute name="items"     required="true"  type="java.util.List" />
```

Keep any existing `duplicate` attribute.

- [ ] **Step 3: Rewrite every `#{cc.attrs.bill.*}` expression**

`PrintBillData` is flat — entity navigation must collapse to a single property:

| Entity expression | `PrintBillData` expression |
|---|---|
| `bill.department.printingName` | `bill.departmentPrintingName` |
| `bill.department.name` | `bill.departmentName` |
| `bill.department.telephone1` | `bill.departmentTelephone1` |
| `bill.department.address` | `bill.departmentAddress` |
| `bill.institution.name` | `bill.institutionName` |
| `bill.deptId` / `bill.insId` | `bill.billNo` |
| `bill.createdAt` | `bill.createdAt` (unchanged) |
| `bill.creater.staff.name` / `bill.creater.name` | `bill.creatorName` |
| `bill.patient.person.nameWithTitle` | `bill.patientName` |
| `bill.patient.person.phone` | `bill.patientPhone` |
| `bill.patient.phn` | `bill.patientPhn` |
| `bill.netTotal` / `bill.total` / `bill.discount` | same names (unchanged) |
| `bill.cashPaid` / `bill.balance` | same names (unchanged) |
| `bill.paymentMethod.label` | `bill.paymentMethodLabel` |
| `bill.paymentScheme.printingName` | `bill.paymentSchemePrintingName` |
| `bill.comments` | `bill.comment` |
| `bill.idStr` | `bill.billIdStr` |
| `bill.cancelled` | `bill.cancelled` (unchanged) |
| `bill.toStaff.person.nameWithTitle` | `bill.toStaffName` |

Any `rendered="#{cc.attrs.bill.patient ne null}"` becomes
`rendered="#{not empty cc.attrs.bill.patientName}"`.

- [ ] **Step 4: Rewrite item iteration**

Where the original iterates `#{cc.attrs.bill.billItems}` with `var="bi"` and reads
`bi.item.name`, `bi.qty`, `bi.rate`, `bi.netValue`, iterate `#{cc.attrs.items}` instead and
read the `BillItemData` properties. Confirm the exact property names first:

```bash
grep -n "private \|public .* get" src/main/java/com/divudi/core/data/dto/BillItemData.java | head -40
```

Cross-check against a working example — `saleBill_native.xhtml` already does exactly this
iteration and is the reference implementation:

```bash
grep -n "ui:repeat\|cc.attrs.items" src/main/webapp/resources/pharmacy/saleBill_native.xhtml
```

Use `ui:repeat` — `p:repeat` does not exist in this PrimeFaces version.

- [ ] **Step 5: Verify no entity expressions survive**

```bash
grep -nE 'cc\.attrs\.bill\.(department|institution|patient|creater|paymentMethod|paymentScheme|billItems|toStaff|deptId|insId|comments|idStr)\b' \
  src/main/webapp/resources/pharmacy/*_native.xhtml
```
Expected: **no output.** Any hit is an unconverted entity navigation that will throw
`PropertyNotFound` at render time.

- [ ] **Step 6: Commit**

```bash
git add src/main/webapp/resources/pharmacy/saleBill_five_five_token_native.xhtml \
        src/main/webapp/resources/pharmacy/saleBillToken_native.xhtml \
        src/main/webapp/resources/pharmacy/saleBillToken_Custom_1_native.xhtml
git commit -m "feat(pharmacy): native token print composites for Sale for Cashier

PrintBillData/BillItemData versions of the three token formats, so the native
page prints without reloading the Bill entity after settle.

Refs #20261

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 5: Native cashier bill print composites (6)

**Files:**
- Create in `src/main/webapp/resources/pharmacy/`:
  `saleBill_for_Cashier_native.xhtml`,
  `saleBill_for_Cashier_Pos_paper_Custom_1_native.xhtml`,
  `saleBill_five_five_for_Cashier_native.xhtml`
- Create in `src/main/webapp/resources/pharmacy/print/`:
  `retail_sale_for_cashier_custom_1_native.xhtml`,
  `retail_sale_for_cashier_custom_2_native.xhtml`,
  `retail_sale_for_cashier_custom_3_native.xhtml`

**Interfaces:**
- Consumes: `PrintBillData` (Task 1), `List<BillItemData>`.
- Produces: composites `phi:saleBill_for_Cashier_native`,
  `phi:saleBill_for_Cashier_Pos_paper_Custom_1_native`,
  `phi:saleBill_five_five_for_Cashier_native`,
  `pp:retail_sale_for_cashier_custom_1_native`, `..._2_native`, `..._3_native`.

`phi:` resolves to `resources/pharmacy/`, `pp:` to `resources/pharmacy/print/`.

**Not ported:** `phi:saleBill_Header` — the native retail page already uses
`phi:saleBill_Header_Inward_native` under the same config key
(`Pharmacy Retail Sale Bill Paper is POS paper with header`). Reuse it as-is.

- [ ] **Step 1: Copy the six files**

```bash
cd src/main/webapp/resources/pharmacy
for f in saleBill_for_Cashier saleBill_for_Cashier_Pos_paper_Custom_1 saleBill_five_five_for_Cashier; do
  cp "$f.xhtml" "${f}_native.xhtml"
done
cd print
for f in retail_sale_for_cashier_custom_1 retail_sale_for_cashier_custom_2 retail_sale_for_cashier_custom_3; do
  cp "$f.xhtml" "${f}_native.xhtml"
done
cd -
```

- [ ] **Step 2: Apply the same transform as Task 4**

Identical recipe: swap the `cc:attribute` interface to
`PrintBillData` + `items`, apply the expression mapping table from Task 4 Step 3, and convert
item iteration per Task 4 Step 4.

These are bill formats, so they use more of the totals block than the token formats do. Pay
particular attention to `bill.total`, `bill.discount`, `bill.netTotal`, `bill.cashPaid`,
`bill.balance` — all present on `PrintBillData` under the same names — and to
`bill.discountPercentPharmacy` where a discount percentage is shown.

For any expression with no `PrintBillData` equivalent, **stop and report it** rather than
inventing a field. Adding a field is fine, but it must be a deliberate decision, and every
addition needs the same treatment in `loadViewDataByBillId` so reprints match fresh prints.

- [ ] **Step 3: Verify no entity expressions survive**

```bash
grep -nE 'cc\.attrs\.bill\.(department|institution|patient|creater|paymentMethod|paymentScheme|billItems|toStaff|deptId|insId|comments|idStr)\b' \
  src/main/webapp/resources/pharmacy/*_native.xhtml \
  src/main/webapp/resources/pharmacy/print/*_native.xhtml
```
Expected: no output.

- [ ] **Step 4: Commit**

```bash
git add src/main/webapp/resources/pharmacy/saleBill_for_Cashier_native.xhtml \
        src/main/webapp/resources/pharmacy/saleBill_for_Cashier_Pos_paper_Custom_1_native.xhtml \
        src/main/webapp/resources/pharmacy/saleBill_five_five_for_Cashier_native.xhtml \
        src/main/webapp/resources/pharmacy/print/retail_sale_for_cashier_custom_1_native.xhtml \
        src/main/webapp/resources/pharmacy/print/retail_sale_for_cashier_custom_2_native.xhtml \
        src/main/webapp/resources/pharmacy/print/retail_sale_for_cashier_custom_3_native.xhtml
git commit -m "feat(pharmacy): native bill print composites for Sale for Cashier

PrintBillData/BillItemData versions of the six cashier bill formats.
saleBill_Header is not ported: saleBill_Header_Inward_native already covers
the same config key.

Refs #20261

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 6: The page

**Files:**
- Create: `src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier_native.xhtml`
- Reference: `src/main/webapp/pharmacy/pharmacy_bill_retail_sale_native.xhtml` (structure),
  `src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier.xhtml` (cashier extras),
  `src/main/webapp/pharmacy/printing/retail_sale_for_cashier.xhtml` (print panel layout)

**Interfaces:**
- Consumes: everything in Task 3's Produces block, plus the 9 composites from Tasks 4-5.
- Produces: the view id `/pharmacy/pharmacy_bill_retail_sale_for_cashier_native.xhtml`,
  referenced by Task 7's menu wiring and by Task 3's navigation methods.

- [ ] **Step 1: Copy the native retail page as the base**

```bash
cp src/main/webapp/pharmacy/pharmacy_bill_retail_sale_native.xhtml \
   src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier_native.xhtml
sed -i 's/\bretailSaleNativeSqlController\b/retailSaleForCashierNativeSqlController/g' \
   src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier_native.xhtml
```

The unanchored `sed` is safe **only** because this page has no numbered controller
references. Phase 4's numbered copies must use the guarded three-step replace in
`developer_docs/pharmacy/PHARMACY_RETAIL_SALE_MULTI_WINDOW_GUIDE.md.md` §3.2 — an unanchored
replace there is what caused #15845.

- [ ] **Step 2: Set the page title**

Change the page heading to `Pharmacy Sale for Cashier (Native)`.

- [ ] **Step 3: Add the token header block**

Port from `pharmacy_bill_retail_sale_for_cashier.xhtml` lines 40-100: the "Back to token
management" button and the token panel, both gated on
`#{configOptionApplicationController.getBooleanValueByKey('Enable token system in sale for cashier', true)}`.
Use `h:panelGroup rendered="..."` — never `ui:fragment`.

- [ ] **Step 4: Add the four qty-helper buttons**

In the cart datatable's row-action column, port the four buttons from
`pharmacy_bill_retail_sale_for_cashier.xhtml`, binding to the Task 3 methods. Per the project's
accessibility rule, give each a `title` including the row identifier so Playwright can target
a specific row, e.g.:

```xml
<p:commandButton id="btnQtyX2"
                 icon="fas fa-times"
                 title="Double quantity #{bid.description}"
                 actionListener="#{retailSaleForCashierNativeSqlController.multiplyQuantityByTwo(bid)}"
                 update="@form" />
```

Set `rowKey`, `id` and `widgetVar` on the datatable if the copied page lacks them.

- [ ] **Step 5: Replace the print panel**

Replace the retail print panel (the `gpBillPreview` block, around line 904 of the source page)
with the cashier's formats, laid out as in `printing/retail_sale_for_cashier.xhtml`: a token
group with `page-break-after: always`, then the bill group. Each entry follows this shape,
using the **same config keys** as the legacy page so existing site settings keep working:

```xml
<h:panelGroup rendered="#{configOptionController.getBooleanValueByKey('Pharmacy Retail Sale Bill Paper is POS Paper', true)}">
    <phi:saleBill_for_Cashier_native
        bill="#{retailSaleForCashierNativeSqlController.printBill}"
        items="#{retailSaleForCashierNativeSqlController.printBillItems}" />
</h:panelGroup>
```

The full key → composite mapping is the table in spec §7.2. Declare both namespaces in the
page root:

```xml
xmlns:phi="http://xmlns.jcp.org/jsf/composite/pharmacy"
xmlns:pp="http://xmlns.jcp.org/jsf/composite/pharmacy/print"
```

- [ ] **Step 6: Retarget the printer-config dialog**

Rename the dialog id and header to the cashier page (e.g.
`saleForCashierNativeConfigDialog`, "Sale for Cashier (Native) Printer Configuration"), keeping
`rendered="#{webUserController.hasPrivilege('ChangeReceiptPrintingPaperTypes')}"` and pointing
its toggles at the same config keys used in Step 5.

- [ ] **Step 7: Verify every referenced bean exists**

This is the #15845 guard, adapted from the multi-window guide:

```bash
for b in $(grep -oE '\bretailSaleForCashierNativeSqlController[0-9]*\b' \
             src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier_native.xhtml | sort -u); do
  n=$(grep -rl "class ${b^}" src/main/java/ | wc -l)
  [ "$n" -eq 0 ] && echo "PHANTOM BEAN: $b"
done
```
Expected: **no output.**

- [ ] **Step 8: Verify every referenced composite exists**

```bash
for c in $(grep -oE '<(phi|pp):[a-zA-Z0-9_]+' \
             src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier_native.xhtml \
           | sed 's/<//' | sort -u); do
  ns="${c%%:*}"; name="${c#*:}"
  case "$ns" in
    phi) d=src/main/webapp/resources/pharmacy ;;
    pp)  d=src/main/webapp/resources/pharmacy/print ;;
  esac
  [ -f "$d/$name.xhtml" ] || echo "MISSING COMPOSITE: $ns:$name -> $d/$name.xhtml"
done
```
Expected: **no output.** A missing composite renders as silently empty markup, not an error —
this check is the only thing that catches it before E2E.

- [ ] **Step 9: Commit**

```bash
git add src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier_native.xhtml
git commit -m "feat(pharmacy): native Sale for Cashier page

Built from pharmacy_bill_retail_sale_native.xhtml with the token panel, the
four qty helpers and the cashier print formats. Printing is inline from
PrintBillData, so there is no entity reload after settle.

Refs #20261

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 7: Menu wiring

**Files:**
- Modify: `src/main/webapp/resources/ezcomp/menu.xhtml` (around line 1116)

**Interfaces:**
- Consumes: `retailSaleForCashierNativeSqlController
  .navigateToPharmacyBillForCashierNativeFromMenu()` (Task 3), the page (Task 6).
- Produces: nothing downstream.

Mirrors the "Sale" / "Sale (Legacy)" pattern already at line ~1084.

- [ ] **Step 1: Retarget the existing item and add the legacy entry**

Replace:

```xml
                        <p:menuitem
                            ajax="false"
                            action="#{pharmacySaleForCashierController.navigateToPharmacyBillForCashierFromMenu()}"
                            value="Sale for cashier"
                            icon="fas fa-cash-register"
                            rendered="#{webUserController.hasPrivilege('PharmacySaleForCashier')}" ></p:menuitem>
```

with:

```xml
                        <p:menuitem
                            ajax="false"
                            action="#{retailSaleForCashierNativeSqlController.navigateToPharmacyBillForCashierNativeFromMenu()}"
                            value="Sale for cashier"
                            icon="fas fa-cash-register"
                            styleClass="menu-item-optimised"
                            rendered="#{webUserController.hasPrivilege('PharmacySaleForCashier')}" ></p:menuitem>
                        <p:menuitem
                            ajax="false"
                            action="#{pharmacySaleForCashierController.navigateToPharmacyBillForCashierFromMenu()}"
                            value="Sale for cashier (Legacy)"
                            icon="fas fa-cash-register"
                            rendered="#{webUserController.hasPrivilege('PharmacySaleForCashier') and configOptionApplicationController.getBooleanValueByKey('Legacy Functions Allowed', false)}" ></p:menuitem>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/webapp/resources/ezcomp/menu.xhtml
git commit -m "feat(pharmacy): point Sale for cashier menu at the native page

Mirrors the Sale / Sale (Legacy) switchover: the native page takes the menu
item, legacy moves behind Legacy Functions Allowed.

Refs #20261

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

## Task 8: Build and deploy locally

**Files:** none modified.

- [ ] **Step 1: Full build**

Run: `mvn -q clean package -DskipTests`
Expected: exit code 0, WAR produced under `target/`.

- [ ] **Step 2: Confirm `persistence.xml` still holds local JNDI**

```bash
grep -n "jta-data-source" src/main/resources/META-INF/persistence.xml
```
Expected: `jdbc/coop` and `jdbc/ruhunuAudit` — **not** `${JDBC_DATASOURCE}`. It must remain
an unstaged modification; never commit it.

- [ ] **Step 3: Deploy to the local Payara domain**

Always pass `--port 9048`. Bare `asadmin` targets port 4848, which is a **different Payara
instance** on this machine and has previously caused a wrong-domain undeploy.

```bash
~/payara/bin/asadmin --port 9048 undeploy rh || true
~/payara/bin/asadmin --port 9048 deploy --name rh --force=true target/rh.war
```

Confirm the WAR name from `target/` first — do not assume `rh.war`.

- [ ] **Step 4: Check the log for deployment errors**

```bash
tail -n 100 ~/payara/glassfish/domains/rh/logs/server.log | grep -iE "SEVERE|Exception" || echo "clean"
```
Expected: `clean`, or no entries newer than the deploy.

---

## Task 9: E2E verification

**Files:** none modified. Use the `playwright-e2e` skill; app at `http://localhost:9080/rh/`.

**After login, click Select on the department screen before navigating anywhere** — inner
pages fail without a selected department. Use a pharmacy department (e.g. OPD Pharmacy).

- [ ] **Step 1: Page loads**

Navigate via menu → Pharmacy → Sale for cashier. Confirm: page renders, title reads
"Pharmacy Sale for Cashier (Native)", no `PropertyNotFound` in the console, no new `SEVERE`
in `server.log`.

- [ ] **Step 2: Settle a bill**

Add 2 items with different batches, set quantities, choose a payment method, settle. Note the
printed bill number.

- [ ] **Step 3: Verify the database — the core assertion**

```sql
SET @billno = 'PASTE_BILL_NUMBER_HERE';

-- Expect exactly ONE row, billTypeAtomic = PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER
SELECT id, deptId, billTypeAtomic, dtype, total, netTotal, discount, paymentMethod,
       cashPaid, balance, paidAmount
FROM Bill WHERE deptId = @billno;

-- Expect ZERO rows
SELECT COUNT(*) AS payment_rows FROM Payment p
JOIN Bill b ON p.bill_ID = b.id WHERE b.deptId = @billno;

-- Expect one BillItem and one PharmaceuticalBillItem per cart line, no duplicates
SELECT bi.id, bi.item_ID, bi.qty, bi.netValue,
       pbi.id AS pbi_id, pbi.stock_ID, pbi.costRate, pbi.retailRate, pbi.purchaseRate
FROM BillItem bi
JOIN Bill b ON bi.bill_ID = b.id
LEFT JOIN PharmaceuticalBillItem pbi ON pbi.billItem_ID = bi.id
WHERE b.deptId = @billno;
```

Assert: exactly one `Bill`; `dtype` is `PreBill`; **zero** `Payment` rows; one `BillItem` +
one `PharmaceuticalBillItem` per line; and **`costRate`/`retailRate`/`purchaseRate` are
non-zero** on every PBI — that is the Task 2 Step 6 change, and zeros there mean it was
missed.

- [ ] **Step 4: Verify stock and stock history**

Record `Stock.stock` for each batch before settling, then:

```sql
SELECT id, stock FROM Stock WHERE id IN (/* stock ids from Step 3 */);
SELECT id, pbItem_ID, stockQty, itemQty FROM StockHistory
WHERE pbItem_ID IN (/* pbi ids from Step 3 */);
```

Assert: `Stock.stock` decreased by exactly the sold qty per batch; one `StockHistory` row per
item.

- [ ] **Step 5: Verify finance details — the deliberate deviation**

```sql
SELECT bfd.* FROM BillFinanceDetails bfd
JOIN Bill b ON bfd.bill_ID = b.id WHERE b.deptId = @billno;

SELECT bifd.* FROM BillItemFinanceDetails bifd
JOIN BillItem bi ON bifd.billItem_ID = bi.id
JOIN Bill b ON bi.bill_ID = b.id WHERE b.deptId = @billno;
```

Assert: BFD present with non-zero `totalRetailSaleValue`; one BIFD per item. Confirm the real
table/column names from the entities first — these differ from the class names in places:

```bash
grep -n "@Table\|@Column" src/main/java/com/divudi/core/entity/BillFinanceDetails.java | head
```

- [ ] **Step 6: Downstream cross-check — the most important test**

Log in as a cashier, open the cashier-side settle page (Pharmacy → Search Sale for Cashier
Bills, or `PharmacyPreSettleController`'s entry point), find the bill from Step 2 and settle
it. Assert: the bill is found, its items and totals display correctly, settling produces the
`PHARMACY_RETAIL_SALE` bill and `Payment` rows as it does for a legacy-created bill, and stock
is **not** deducted a second time.

A native bill must be indistinguishable to this page. If it is not, stop and report — this
gates everything else.

- [ ] **Step 7: Token path**

Set `Enable token system in sale for cashier` to `true`, settle another bill, confirm the
token prints and a `Token` row is created linked to the bill. Set it back to `false`, settle
again, confirm no token is created and no error appears.

- [ ] **Step 8: Multiple payment methods**

Settle with `MultiplePaymentMethods` split across two components. Assert: still **zero**
`Payment` rows; the breakdown is on the bill's own columns (`creditCardRefNo`, `chequeRefNo`,
etc. — confirm which via `billBean.setPaymentMethodData`); the printed bill shows both
component lines.

- [ ] **Step 9: Discount path**

Settle with a discount-bearing payment scheme. Assert `total`, `discount` and `netTotal` on
the bill are consistent (`total - discount = netTotal`) and match the printed values.

- [ ] **Step 10: Qty helpers**

Add an item, use ×2 and ÷2 on the row and the all-rows variants. Assert quantities and the
net total update correctly, and that ×2 cannot push a quantity beyond available stock.

- [ ] **Step 11: Print formats**

Enable each of the 10 formats in turn via the printer-config dialog and confirm each renders
with correct values, the token barcode included. A blank render means a missing composite —
re-run Task 6 Step 8.

- [ ] **Step 12: Record the results**

Append the outcome (bill numbers, stock deltas, row counts) to
`tmp/retail-sale-native-migration-master-plan.md` under Phase 2, matching how Phase 1's E2E
result was recorded.

---

## Task 10: Push and open the PR

- [ ] **Step 1: Confirm nothing unwanted is staged**

```bash
git status --short
```
Expected: only `M src/main/resources/META-INF/persistence.xml`, unstaged.

- [ ] **Step 2: Push**

```bash
git push -u origin 20261-native-sale-for-cashier
```

- [ ] **Step 3: Restore local JNDI immediately after pushing**

Required by CLAUDE.md after **every** push. If CI placeholders ended up in the working tree,
put `jdbc/coop` and `jdbc/ruhunuAudit` back and leave the change unstaged.

- [ ] **Step 4: Open the PR against `development`**

Never target `master`. Body must include `Closes #20261`, a summary of the single-bill
structure, the BFD/BIFD deviation, and the E2E evidence from Task 9. End with:

```
🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

- [ ] **Step 5: Update the tracker**

Mark Phase 2 `[x]` in `tmp/retail-sale-native-migration-master-plan.md` with the PR number,
and note that Phase 3 (production testing gate) is now the blocker for Phase 4.

---

## Self-Review

**Spec coverage:**

| Spec section | Task |
|---|---|
| §5.1 new files | 2, 3, 4, 5, 6 |
| §5.2 service, single bill, PBI population, BFD deviation | 2 (Steps 5-10) |
| §5.3 controller, bill types, deltas | 3 (Steps 3-11) |
| §5.4 `PrintBillData` fields | 1 |
| §6.1 cart building, deptType autocomplete, qty helpers | 3 (Steps 8-9) |
| §6.2 settle sequence | 3 (Steps 5-7) |
| §6.2 stock locking not carried over | 2 (inherited `deductStock`) |
| §7.1 page, inline print | 6 |
| §7.2 nine composites | 4, 5 |
| §7.3 menu | 7 |
| §8 error handling | 3 (Step 6 try/catch, inherited guards) |
| §9 verification | 9 |
| §10 out of scope | not implemented, cross-checked in 9 Step 6 |

**Known gaps, deliberate:**

- `showItemDetailsForSelectedStock` (spec §4) has no dedicated step. It is a UI convenience
  bound from the legacy page; port it in Task 3 only if Task 6 Step 3/4 turns out to reference
  it. Flagged rather than silently dropped.
- Tasks 4 and 5 give a transform recipe plus verification greps instead of full markup for
  nine files. Transcribing ~2,000 lines of existing markup into a plan would introduce drift
  from the originals; the recipe plus the two `grep` gates is more reliable and is how the
  existing `*_native` composites were produced.

**Type consistency:** `settle(...)` is `void` and 5-arg in Task 2's Produces block, Task 3's
call site, and the spec. `buildPrintBill(Bill)` keeps its one-arg entity signature (Task 3
Steps 6, 11). `recalculateRow(BillItemData)` is introduced in Task 3 Step 8 and used only
there. `navigateToPharmacyBillForCashierNativeFromMenu()` is defined in Task 3 Step 10 and
consumed in Task 7 Step 1 — names match.
