# Three-Level Inward Final Bill Discounts — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Record inward final-bill discounts separately at three levels — item/fee level (existing, auto-computed), inward-charge-type level (new, manual), and bill level (new, manual) — per the approved spec `developer_docs/inward/2026-07-19-three-level-final-bill-discounts-design.md` (issue #22241).

**Architecture:** Level 2 is stored in a new `BillItem.chargeTypeDiscount` column on the per-charge-type lines of temp/original/provisional/final bills; level 3 in the existing `BillFinanceDetails.billDiscount`; `Bill.discount` and `BillItem.discount` keep their current combined meanings so all existing reports/prints are untouched. `BhtSummeryController` computes `discount = Σ itemDiscounts + Σ chargeTypeDiscounts + billLevelDiscount` and no longer pushes charge-type discounts down onto fees.

**Tech Stack:** Java EE (JSF/PrimeFaces, JPA/EclipseLink), JUnit 5 (jupiter), Maven, MySQL, Playwright MCP for E2E.

## Global Constraints

- Branch: `22241-inward-three-level-final-bill-discounts` (already created from `origin/development`; PR must target `development`).
- 🚨 Never work in a git worktree; work in the main checkout.
- 🚨 Never modify existing constructors — only add.
- New plain columns go through DDL generation, NOT hand-written migration scripts.
- `Bill.discount` MUST remain the grand-total discount; `BillItem.discount` MUST remain the combined (item + charge-type) discount per line — backward compatibility for every existing report/print.
- Entry of new discounts happens on `inward_bill_final.xhtml` only; the interim page is display-only. No new privilege. VAT math unchanged.
- Commit messages: conventional style, include `Refs #22241`, end with `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.
- After every `git push`: restore `persistence.xml` placeholders to local JNDI (`${JDBC_DATASOURCE}` → `jdbc/coop`, `${JDBC_AUDIT_DATASOURCE}` → `jdbc/ruhunuAudit`), leave unstaged.
- JSF-only changes (Task 5) need no compilation.

---

### Task 1: `ChargeItemTotal` gains `chargeTypeDiscount` (TDD)

**Files:**
- Modify: `src/main/java/com/divudi/core/data/dataStructure/ChargeItemTotal.java`
- Test (create): `src/test/java/com/divudi/core/data/dataStructure/ChargeItemTotalTest.java`

**Interfaces:**
- Produces: `double getChargeTypeDiscount()` / `void setChargeTypeDiscount(double)`; `getNetTotal()` now returns `total − discount − chargeTypeDiscount`. Tasks 3–5 rely on these exact names.

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.core.data.dataStructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChargeItemTotalTest {

    @Test
    public void netTotalSubtractsItemAndChargeTypeDiscounts() {
        ChargeItemTotal cit = new ChargeItemTotal();
        cit.setTotal(1000.0);
        cit.setDiscount(100.0);            // level 1 aggregate
        cit.setChargeTypeDiscount(50.0);   // level 2 manual
        assertEquals(850.0, cit.getNetTotal(), 0.001);
    }

    @Test
    public void chargeTypeDiscountDefaultsToZeroKeepingOldBehaviour() {
        ChargeItemTotal cit = new ChargeItemTotal();
        cit.setTotal(1000.0);
        cit.setDiscount(100.0);
        assertEquals(0.0, cit.getChargeTypeDiscount(), 0.001);
        assertEquals(900.0, cit.getNetTotal(), 0.001);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ChargeItemTotalTest -q`
Expected: COMPILATION ERROR — `cannot find symbol: method setChargeTypeDiscount(double)`

- [ ] **Step 3: Implement**

In `ChargeItemTotal.java`, below `private double discount = 0;` add:

```java
    private double chargeTypeDiscount = 0;
```

Change `getNetTotal()` (currently `netTotal = total - discount;`) to:

```java
    public double getNetTotal() {
        netTotal = total - discount - chargeTypeDiscount;
        return netTotal;
    }
```

Below the `discount` getter/setter pair add:

```java
    public double getChargeTypeDiscount() {
        return chargeTypeDiscount;
    }

    public void setChargeTypeDiscount(double chargeTypeDiscount) {
        this.chargeTypeDiscount = chargeTypeDiscount;
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=ChargeItemTotalTest -q`
Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/data/dataStructure/ChargeItemTotal.java src/test/java/com/divudi/core/data/dataStructure/ChargeItemTotalTest.java
git commit -m "feat(inward): add chargeTypeDiscount to ChargeItemTotal

- netTotal now subtracts both item-level and charge-type-level discounts
- Refs #22241

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: `BillItem.chargeTypeDiscount` entity column

**Files:**
- Modify: `src/main/java/com/divudi/core/entity/BillItem.java`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: `double getChargeTypeDiscount()` / `void setChargeTypeDiscount(double)` on `BillItem`. Task 4 and the DB verification in Task 6 rely on these; column `CHARGETYPEDISCOUNT` appears via DDL generation on deploy.

- [ ] **Step 1: Add the field**

In `BillItem.java`, directly below `double discount;` (around line 77) add:

```java
    // Manual discount entered at inward-charge-type level on the final bill
    // (level 2 of 3). `discount` on final-bill lines holds the combined
    // item-level + charge-type-level value.
    double chargeTypeDiscount;
```

Below the existing `setDiscount(...)` method (around line 574) add:

```java
    public double getChargeTypeDiscount() {
        return chargeTypeDiscount;
    }

    public void setChargeTypeDiscount(double chargeTypeDiscount) {
        this.chargeTypeDiscount = chargeTypeDiscount;
    }
```

- [ ] **Step 2: Wire into copy/invert/reset paths**

All in `BillItem.java`; add one line to each method, next to the existing `discount` line:

In `copy(BillItem billItem)` (line ~293, after `discount = billItem.getDiscount();`):
```java
        chargeTypeDiscount = billItem.getChargeTypeDiscount();
```

In `copyWithPharmaceuticalAndFinancialData(BillItem billItem)` (line ~340, after `discount = billItem.getDiscount();`):
```java
        chargeTypeDiscount = billItem.getChargeTypeDiscount();
```

In `resetValue()` (line ~406, after `discount = 0;`):
```java
        chargeTypeDiscount = 0;
```

In `invertValue(BillItem billItem)` (line ~440, after `discount = 0 - billItem.getDiscount();`):
```java
        chargeTypeDiscount = 0 - billItem.getChargeTypeDiscount();
```

In `invertValue()` (line ~465, after `discount = 0 - getDiscount();`):
```java
        chargeTypeDiscount = 0 - getChargeTypeDiscount();
```

- [ ] **Step 3: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS (no errors)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/core/entity/BillItem.java
git commit -m "feat(inward): add chargeTypeDiscount column to BillItem

- included in copy, invert, and reset paths so cancellation flows carry it
- plain new column; created by DDL generation, no migration script
- Refs #22241

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: `BhtSummeryController` — three-level totals math

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java`

**Interfaces:**
- Consumes: `ChargeItemTotal.getChargeTypeDiscount()/setChargeTypeDiscount()` (Task 1).
- Produces (used by Task 4 persistence and Task 5 XHTML): controller properties `billLevelDiscount`, `itemDiscountTotal`, `chargeTypeDiscountTotal` (each with public getter/setter, `double`); rewritten `changeDiscountListener(ChargeItemTotal)`; `listnerDiscontAmmountChanged()` keeps its existing name (bound in XHTML).

- [ ] **Step 1: Add fields and accessors**

Below `private double discount;` (line ~183) add:

```java
    private double billLevelDiscount = 0.0;
    private double itemDiscountTotal = 0.0;
    private double chargeTypeDiscountTotal = 0.0;
```

Next to `getDiscount()/setDiscount()` (line ~3679) add:

```java
    public double getBillLevelDiscount() {
        return billLevelDiscount;
    }

    public void setBillLevelDiscount(double billLevelDiscount) {
        this.billLevelDiscount = billLevelDiscount;
    }

    public double getItemDiscountTotal() {
        return itemDiscountTotal;
    }

    public void setItemDiscountTotal(double itemDiscountTotal) {
        this.itemDiscountTotal = itemDiscountTotal;
    }

    public double getChargeTypeDiscountTotal() {
        return chargeTypeDiscountTotal;
    }

    public void setChargeTypeDiscountTotal(double chargeTypeDiscountTotal) {
        this.chargeTypeDiscountTotal = chargeTypeDiscountTotal;
    }
```

- [ ] **Step 2: Rewrite `calFinalValue()` (line ~3649)**

Replace the whole method with:

```java
    public void calFinalValue() {
        grantTotal = 0;
        itemDiscountTotal = 0;
        chargeTypeDiscountTotal = 0;
        adjustedTotal = 0;
        grossTotal = 0;
        marginTotal = 0;
        vatTotal = 0;
        for (ChargeItemTotal c : getChargeItemTotals()) {
            grantTotal += c.getTotal();
            itemDiscountTotal += c.getDiscount();
            chargeTypeDiscountTotal += c.getChargeTypeDiscount();
            adjustedTotal += c.getAdjustedTotal();
            grossTotal += c.getGross();
            marginTotal += c.getMargin();
            vatTotal += c.getVat();
        }
        discount = itemDiscountTotal + chargeTypeDiscountTotal + billLevelDiscount;
    }
```

- [ ] **Step 3: Rewrite `changeDiscountListener(ChargeItemTotal)` (line ~493)**

The old body converts the entered amount to a percentage and pushes it down onto item fees (`updateIssueBillFees` / `discountSet`). Replace the whole method with:

```java
    public void changeDiscountListener(ChargeItemTotal cit) {
        if (cit.getChargeTypeDiscount() < 0) {
            cit.setChargeTypeDiscount(0);
            JsfUtil.addErrorMessage("Charge type discount cannot be negative");
        }
        double maxAllowed = cit.getTotal() - cit.getDiscount();
        if (cit.getChargeTypeDiscount() > maxAllowed) {
            cit.setChargeTypeDiscount(0);
            JsfUtil.addErrorMessage("Charge type discount cannot exceed the remaining net for this charge type");
        }
        updateTotal();
    }
```

Do NOT delete `updateIssueBillFees`, `discountSet(ChargeItemTotal, double)`, or `updateServiceBillFees` — `calculateDiscount()` (auto item-level path) still uses some of them; leave any newly-unused private helpers in place for this PR to keep the diff reviewable.

- [ ] **Step 4: Rewrite `listnerDiscontAmmountChanged()` (line ~3930)**

Replace the whole method (old body: `due = (grantTotal - discount) - paid;`) with:

```java
    public void listnerDiscontAmmountChanged() {
        if (billLevelDiscount < 0) {
            billLevelDiscount = 0;
            JsfUtil.addErrorMessage("Bill level discount cannot be negative");
        }
        discount = itemDiscountTotal + chargeTypeDiscountTotal + billLevelDiscount;
        if (discount > grantTotal) {
            billLevelDiscount = 0;
            discount = itemDiscountTotal + chargeTypeDiscountTotal;
            JsfUtil.addErrorMessage("Total discount cannot exceed total charges");
        }
        due = (grantTotal - discount) - paid;
    }
```

- [ ] **Step 5: Guard in `errorCheck()` (line ~2306)**

Add before the final `return false;`:

```java
        if (discount > grantTotal) {
            JsfUtil.addErrorMessage("Total discount (" + discount + ") exceeds total charges (" + grantTotal + ")");
            return true;
        }
```

- [ ] **Step 6: Preserve manual values across Recalculate and reload**

In `createChargeItemTotals()` (line ~3748): the method rebuilds `chargeItemTotals` from scratch, which would wipe manual level-2 entries on every Recalculate. At the very top of the method (before `chargeItemTotals = new ArrayList<>();`) add:

```java
        Map<InwardChargeType, Double> previousTypeDiscounts = new HashMap<>();
        if (chargeItemTotals != null) {
            for (ChargeItemTotal old : chargeItemTotals) {
                if (old.getChargeTypeDiscount() != 0) {
                    previousTypeDiscounts.put(old.getInwardChargeType(), old.getChargeTypeDiscount());
                }
            }
        }
```

At the very bottom of the method (after `restoreChargeItemComments();`) add:

```java
        for (ChargeItemTotal cit : chargeItemTotals) {
            Double previous = previousTypeDiscounts.get(cit.getInwardChargeType());
            if (previous != null) {
                cit.setChargeTypeDiscount(previous);
            }
        }
```

In `restoreChargeItemComments()` (line ~3777) — which already restores per-type comments from the persisted final bill when re-opening a finalized BHT — add one line inside the matching-type block, after `cit.setComments(existing.getDescreption());`:

```java
                    cit.setChargeTypeDiscount(existing.getChargeTypeDiscount());
```

In the same finalized-reload path, restore the bill-level value: in `restoreChargeItemComments()`, before the `for` loop over bill items, add:

```java
        if (getPatientEncounter().getFinalBill().getBillFinanceDetails() != null
                && getPatientEncounter().getFinalBill().getBillFinanceDetails().getBillDiscount() != null) {
            billLevelDiscount = getPatientEncounter().getFinalBill().getBillFinanceDetails().getBillDiscount().doubleValue();
        }
```

(Note: `getBillFinanceDetails()` lazily creates an instance, so the null check on `getBillDiscount()` is what actually protects against garbage; keep both checks as written.)

In `makeNull()` (line ~3069), next to `chargeItemTotals = null;` add:

```java
        billLevelDiscount = 0;
        itemDiscountTotal = 0;
        chargeTypeDiscountTotal = 0;
```

Check imports: `java.util.Map` and `java.util.HashMap` are already imported in this controller (used by existing JPQL parameter maps); `com.divudi.core.data.inward.InwardChargeType` is already imported.

- [ ] **Step 7: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/BhtSummeryController.java
git commit -m "feat(inward): compute final bill discount from three independent levels

- discount = item-level aggregate + charge-type manual + bill-level manual
- changeDiscountListener no longer pushes charge-type discounts onto fees
- manual entries survive Recalculate and are restored on finalized reload
- validation: per-type discount <= remaining net, total discount <= total
- Refs #22241

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: Persist the breakdown at settle

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java` (methods `saveBillItem` ~2686, `saveTempBillItem` ~2734, `saveOriginalBillItem` ~2772, `saveBill` ~2525, `saveTempBill` ~2559, `saveOriginalBill` ~2585)

**Interfaces:**
- Consumes: `BillItem.setChargeTypeDiscount(double)` (Task 2); controller fields `billLevelDiscount`, `itemDiscountTotal`, `chargeTypeDiscountTotal` (Task 3); `Bill.getBillFinanceDetails()` (existing, lazily creates; cascade ALL persists it with the bill).
- Produces: persisted `BillItem.chargeTypeDiscount` per charge-type line; `BillFinanceDetails.billDiscount/lineDiscount/totalDiscount` on temp/original/final bills. Task 6 verifies these in MySQL.

- [ ] **Step 1: Per-line persistence in all three `save*BillItem` methods**

In each of `saveBillItem()`, `saveTempBillItem()`, `saveOriginalBillItem()`, the loop currently contains:

```java
            temBi.setGrossValue(cit.getTotal());
            temBi.setDiscount(cit.getDiscount());
            temBi.setNetValue(cit.getNetTotal());
```

Replace those three lines (in each of the three methods) with:

```java
            temBi.setGrossValue(cit.getTotal());
            temBi.setDiscount(cit.getDiscount() + cit.getChargeTypeDiscount());
            temBi.setChargeTypeDiscount(cit.getChargeTypeDiscount());
            temBi.setNetValue(cit.getNetTotal());
```

(`cit.getNetTotal()` already subtracts both components after Task 1. `BillItem.discount` = combined per line, per Global Constraints.)

- [ ] **Step 2: Finance details in all three `save*Bill` methods**

Add a private helper next to `saveBill()`:

```java
    private void writeDiscountBreakdownToFinanceDetails(Bill bill) {
        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        bfd.setBillDiscount(BigDecimal.valueOf(billLevelDiscount));
        bfd.setLineDiscount(BigDecimal.valueOf(itemDiscountTotal + chargeTypeDiscountTotal));
        bfd.setTotalDiscount(BigDecimal.valueOf(discount));
    }
```

Add import if missing: `import java.math.BigDecimal;` and `import com.divudi.core.entity.BillFinanceDetails;`.

Call it in each save method immediately before the `create/edit` call:
- `saveBill()`: before `if (getCurrent().getId() == null) {` add `writeDiscountBreakdownToFinanceDetails(getCurrent());`
- `saveOriginalBill()`: before `if (getOriginalBill().getId() == null) {` add `writeDiscountBreakdownToFinanceDetails(getOriginalBill());`
- `saveTempBill()`: at the end of the method add `writeDiscountBreakdownToFinanceDetails(getTempBill());` (temp bill is persisted by its caller)

- [ ] **Step 3: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/BhtSummeryController.java
git commit -m "feat(inward): persist three-level discount breakdown at settle

- BillItem.chargeTypeDiscount saved per charge-type line; discount stays combined
- BillFinanceDetails billDiscount/lineDiscount/totalDiscount populated on
  temp, original, and final bills; Bill.discount stays the grand total
- Refs #22241

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: XHTML — final bill page and interim page

**Files:**
- Modify: `src/main/webapp/inward/inward_bill_final.xhtml`
- Modify: `src/main/webapp/inward/inward_bill_intrim.xhtml`

**Interfaces:**
- Consumes: `bhtSummeryController.billLevelDiscount / itemDiscountTotal / chargeTypeDiscountTotal / discount`; `c.chargeTypeDiscount` on `ChargeItemTotal`; listeners `changeDiscountListener(c)` and `listnerDiscontAmmountChanged()`.
- JSF-only task: no compilation needed. Follow project JSF rules (no `ui:fragment`; `h:panelGroup` for conditionals).

- [ ] **Step 1: Charge Types table on `inward_bill_final.xhtml` (~line 582)**

Replace the existing editable "Discount" column (the whole `<p:column headerText="Discount" ...>` block, lines ~582–607) with a read-only Item Discounts column plus a new editable Type Discount column:

```xml
                            <p:column headerText="Item Discounts" class="text-end" width="12em;">
                                <h:outputLabel value="#{c.discount}" class="w-100 text-end">
                                    <f:convertNumber pattern="#,##0.00"/>
                                </h:outputLabel>
                            </p:column>

                            <p:column headerText="Type Discount" class="text-end" width="12em;">
                                <p:inputText
                                    autocomplete="off"
                                    id="catTypeDiscount"
                                    class="w-100 text-end"
                                    title="Charge type discount for #{c.inwardChargeType.name}"
                                    value="#{c.chargeTypeDiscount}"
                                    disabled="#{c.total eq 0}">
                                    <f:convertNumber pattern="#,##0.00"/>
                                    <p:ajax process="@this"
                                            update="@this catNetTotal :#{p:resolveFirstComponentWithId('tot',view).clientId}"
                                            event="blur"
                                            listener="#{bhtSummeryController.changeDiscountListener(c)}"/>
                                    <p:ajax process="@this" event="keyup"
                                            update=":#{p:resolveFirstComponentWithId('settle',view).clientId} :#{p:resolveFirstComponentWithId('saveProvisional',view).clientId}"
                                            listener="#{bhtSummeryController.changeIsMade()}"/>
                                </p:inputText>
                            </p:column>
```

Key differences from the old column: value binds `c.chargeTypeDiscount` (not `c.discount`), and the long `disabled` list of room/professional charge types is gone — only `c.total eq 0` disables (all charge types are now discountable at this level).

- [ ] **Step 2: Discount Details panel on `inward_bill_final.xhtml` (~line 157)**

Change the label and binding (old: `value="#{bhtSummeryController.discount}"`):

```xml
                                    <h:panelGrid columns="3" class="w-100">
                                        <h:outputLabel value="Bill Level Discount        "/>
                                        <h:outputLabel value=" :     "/>
                                        <p:inputText id="billLevelDiscountInput" value="#{bhtSummeryController.billLevelDiscount}">
                                            <p:ajax process="@this" update="@all" event="change" listener="#{bhtSummeryController.listnerDiscontAmmountChanged()}"/>
                                        </p:inputText>
                                    </h:panelGrid>
```

- [ ] **Step 3: Charges Overview panel on `inward_bill_final.xhtml` (~line 186)**

Replace the single Discount row:

```xml
                                        <h:outputLabel value="Discount" style="font-weight: bold"/>
                                        <h:outputLabel value="#{bhtSummeryController.discount}">
                                            <f:convertNumber pattern="#,##0.00"/>
                                        </h:outputLabel>
```

with four rows:

```xml
                                        <h:outputLabel value="Item Discounts" style="font-weight: bold"/>
                                        <h:outputLabel value="#{bhtSummeryController.itemDiscountTotal}">
                                            <f:convertNumber pattern="#,##0.00"/>
                                        </h:outputLabel>
                                        <h:outputLabel value="Charge Type Discounts" style="font-weight: bold"/>
                                        <h:outputLabel value="#{bhtSummeryController.chargeTypeDiscountTotal}">
                                            <f:convertNumber pattern="#,##0.00"/>
                                        </h:outputLabel>
                                        <h:outputLabel value="Bill Level Discount" style="font-weight: bold"/>
                                        <h:outputLabel value="#{bhtSummeryController.billLevelDiscount}">
                                            <f:convertNumber pattern="#,##0.00"/>
                                        </h:outputLabel>
                                        <h:outputLabel value="Total Discount" style="font-weight: bold"/>
                                        <h:outputLabel value="#{bhtSummeryController.discount}">
                                            <f:convertNumber pattern="#,##0.00"/>
                                        </h:outputLabel>
```

- [ ] **Step 4: Interim page summary on `inward_bill_intrim.xhtml` (~line 293)**

Replace the misleadingly-labelled row:

```xml
                                                <h:outputLabel value="Bill Level Discount"/>
                                                <h:outputLabel value=":" class="mx-2"/>
                                                <h:outputLabel value="#{bhtSummeryController.discount}">
                                                    <f:convertNumber pattern="#,##0.00"/>
                                                </h:outputLabel>
```

with display-only rows for all levels:

```xml
                                                <h:outputLabel value="Item Discounts"/>
                                                <h:outputLabel value=":" class="mx-2"/>
                                                <h:outputLabel value="#{bhtSummeryController.itemDiscountTotal}">
                                                    <f:convertNumber pattern="#,##0.00"/>
                                                </h:outputLabel>

                                                <h:outputLabel value="Charge Type Discounts"/>
                                                <h:outputLabel value=":" class="mx-2"/>
                                                <h:outputLabel value="#{bhtSummeryController.chargeTypeDiscountTotal}">
                                                    <f:convertNumber pattern="#,##0.00"/>
                                                </h:outputLabel>

                                                <h:outputLabel value="Bill Level Discount"/>
                                                <h:outputLabel value=":" class="mx-2"/>
                                                <h:outputLabel value="#{bhtSummeryController.billLevelDiscount}">
                                                    <f:convertNumber pattern="#,##0.00"/>
                                                </h:outputLabel>

                                                <h:outputLabel value="Total Discount"/>
                                                <h:outputLabel value=":" class="mx-2"/>
                                                <h:outputLabel value="#{bhtSummeryController.discount}">
                                                    <f:convertNumber pattern="#,##0.00"/>
                                                </h:outputLabel>
```

- [ ] **Step 5: Commit**

```bash
git add src/main/webapp/inward/inward_bill_final.xhtml src/main/webapp/inward/inward_bill_intrim.xhtml
git commit -m "feat(inward): three-level discount UI on final and interim bill pages

- charge-type table: read-only Item Discounts + editable Type Discount for
  all charge types (room/professional restrictions removed)
- Discount Details input now binds the separate bill-level discount
- overview panels show Item / Charge Type / Bill Level / Total discount rows
- Refs #22241

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: Build, deploy, E2E verify, PR

**Files:** none (verification and delivery)

- [ ] **Step 1: Full build**

Run: `mvn package -DskipTests -q`
Expected: BUILD SUCCESS, WAR produced under `target/`

- [ ] **Step 2: Deploy locally and E2E-test with the playwright-e2e skill**

Invoke the `playwright-e2e` skill. Scenario (remember: select department after login; use an admitted test BHT):

1. Open `/faces/inward/inward_bill_intrim.xhtml`, load a BHT with charges. Verify the Summary panel shows the four discount rows (Item Discounts = auto value, other two = 0.00).
2. Process to final bill (`toSettle` → `inward_bill_final.xhtml`). In the Charge Types table enter a Type Discount for (a) a service-type row and (b) a room-related or professional row (previously disabled — must now accept input). Verify NetTotal per row and the Charges Overview update.
3. Enter a Bill Level Discount in Discount Details. Verify Total Discount = item + type + bill level and Net Charges update.
4. Enter an oversized discount (greater than remaining net / total): verify the error message appears and the value resets.
5. Settle the final bill.
6. Re-open the final bill page for the same BHT: verify Type Discounts and Bill Level Discount display the persisted values.

- [ ] **Step 3: Verify in MySQL**

```sql
SELECT b.ID, b.DISCOUNT, b.NETTOTAL, bfd.BILLDISCOUNT, bfd.LINEDISCOUNT, bfd.TOTALDISCOUNT
FROM bill b LEFT JOIN billfinancedetails bfd ON b.BILLFINANCEDETAILS_ID = bfd.ID
WHERE b.BILLTYPEATOMIC = 'INWARD_FINAL_BILL'
ORDER BY b.ID DESC LIMIT 3;
```

Expected: newest row has `BILLDISCOUNT` = entered bill-level value, `LINEDISCOUNT` = item + type sums, `TOTALDISCOUNT` = `b.DISCOUNT`.

```sql
SELECT INWARDCHARGETYPE, GROSSVALUE, DISCOUNT, CHARGETYPEDISCOUNT, NETVALUE
FROM billitem WHERE BILL_ID = <final bill id from previous query>;
```

Expected: rows edited in the E2E run show `CHARGETYPEDISCOUNT` = entered value, `DISCOUNT` = item aggregate + `CHARGETYPEDISCOUNT`, `NETVALUE` = `GROSSVALUE − DISCOUNT`. (Table/column case may differ per MySQL settings; adjust to actual schema case.)

- [ ] **Step 4: Run the full test suite**

Run: `mvn test -q`
Expected: no new failures versus `origin/development` baseline.

- [ ] **Step 5: Push and open PR**

```bash
git push -u origin 22241-inward-three-level-final-bill-discounts
gh pr create --repo hmislk/hmis --base development \
  --title "feat(inward): record final bill discounts separately at item, charge type, and bill levels" \
  --body "Closes #22241. Design: developer_docs/inward/2026-07-19-three-level-final-bill-discounts-design.md

🤖 Generated with [Claude Code](https://claude.com/claude-code)"
```

Then restore `persistence.xml` local JNDI values (`${JDBC_DATASOURCE}` → `jdbc/coop`, `${JDBC_AUDIT_DATASOURCE}` → `jdbc/ruhunuAudit`), leave unstaged.
