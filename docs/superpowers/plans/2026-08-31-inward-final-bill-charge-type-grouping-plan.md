# Inward Final Bill Charge-Type Grouping Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an admin configure, per `InwardChargeType`, a free-text "Final Bill Group" so charge types sharing the same group text print as one summed line on a new opt-in Final Bill print format ("Bundled Custom 1") — while every other hospital, every other print format, and the Inward Ledger Report are completely unaffected.

**Architecture:** A pure static grouping/summing algorithm (`BhtSummeryController.buildBundledRows`, unit-tested directly) is fed by a thin CDI-aware wrapper that reads the new per-charge-type `ConfigOption` ("Final Bill Group") via the existing `ConfigOptionApplicationController` pattern from #23340. A brand-new print composite (`finalBillBundledCustom1.xhtml`) renders the result; it is wired into the existing "Custom Bills" tab / per-format config-toggle convention already used by Custom Bill 2/3/4, so no existing print template is touched except to delete confirmed-dead code.

**Tech Stack:** Java EE (JSF/PrimeFaces, JAX-RS, EJB), JUnit 5, Maven, MySQL via `ConfigOption` entity.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-31-inward-final-bill-charge-type-grouping-design.md` — every task below traces to a section of it.
- Every new `ConfigOption` default must preserve current behavior for every hospital that never touches it (empty group = individual row; toggle off = format hidden).
- No new privileges. No DB schema/DDL changes — everything is `ConfigOption` rows (already-existing table).
- This codebase's established verification method is **compile after each Java change, then one consolidated build+deploy+Playwright+DB pass at the end** (see `developer_docs/testing/playwright-e2e-workflow.md`, `developer_docs/testing/maven-commands.md`) — not a JSF integration-test harness. Pure-logic Java (the static grouping algorithm, the DTO) gets real JUnit tests, following the existing precedent in `src/test/java/com/divudi/bean/inward/InwardProfessionalFeeSummaryControllerTest.java` (a `static` method on a `@SessionScoped` controller tested directly with no CDI container). Use `./detect-maven.sh test -Dtest=ClassName` (falls back to `mvn test -Dtest=ClassName` if the script doesn't recognize the machine) to run a single test class, and `./detect-maven.sh compile -q` to just check compilation.
- `CLAUDE.md` rule: after every `git push`, restore local JNDI in `persistence.xml` (not this plan's concern — a pre-existing local-only diff already sits on this branch).
- `CLAUDE.md` rule (added this session): never gate behavior on `applicationInstitution eq 'HospitalName'` — this plan is itself the reference example of doing it the right way (`ConfigOption`, per `developer_docs/configuration/institution-specific-behavior.md`).

---

### Task 1: `FinalBillPrintRowDTO` — the row data class

**Files:**
- Create: `src/main/java/com/divudi/core/data/dto/FinalBillPrintRowDTO.java`
- Test: `src/test/java/com/divudi/core/data/dto/FinalBillPrintRowDTOTest.java`

**Interfaces:**
- Produces: `FinalBillPrintRowDTO(String label, double amount, int order)` constructor; `getLabel()`, `getAmount()`, `getOrder()` — used by Task 2's algorithm and Task 6's `ui:repeat`.

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.core.data.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinalBillPrintRowDTOTest {

    @Test
    void constructor_setsAllThreeFields() {
        FinalBillPrintRowDTO row = new FinalBillPrintRowDTO("Room Charges", 4250.0, 30);

        assertEquals("Room Charges", row.getLabel());
        assertEquals(4250.0, row.getAmount());
        assertEquals(30, row.getOrder());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./detect-maven.sh test -Dtest=FinalBillPrintRowDTOTest`
Expected: FAIL — `FinalBillPrintRowDTO` does not exist (compile error).

- [ ] **Step 3: Write the implementation**

```java
package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * One printed line on the "Bundled Custom 1" Final Bill format (issue #23340
 * follow-up: configurable charge-type grouping). Represents either a single
 * ungrouped InwardChargeType's total, or the summed total of every charge
 * type an admin assigned to the same "Final Bill Group" text.
 */
public class FinalBillPrintRowDTO implements Serializable {

    private final String label;
    private final double amount;
    private final int order;

    public FinalBillPrintRowDTO(String label, double amount, int order) {
        this.label = label;
        this.amount = amount;
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    public double getAmount() {
        return amount;
    }

    public int getOrder() {
        return order;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./detect-maven.sh test -Dtest=FinalBillPrintRowDTOTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/data/dto/FinalBillPrintRowDTO.java src/test/java/com/divudi/core/data/dto/FinalBillPrintRowDTOTest.java
git commit -m "feat(inward): add FinalBillPrintRowDTO for bundled final bill rows

Part of the configurable charge-type grouping work (design doc
2026-08-31-inward-final-bill-charge-type-grouping-design.md).

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 2: `buildBundledRows` — the grouping/summing algorithm

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java` (add a `public static` method; insert after `getSummaryOfDoctorChargers`, i.e. immediately before `public String navigateToIntrimBill()`)
- Test: `src/test/java/com/divudi/bean/inward/BhtSummeryControllerBundledRowsTest.java`

**Interfaces:**
- Consumes: `FinalBillPrintRowDTO` (Task 1); `BillItem.getInwardChargeType()`, `BillItem.getAdjustedValue()` (existing entity methods); `InwardChargeType` enum (existing).
- Produces: `public static List<FinalBillPrintRowDTO> buildBundledRows(List<BillItem> billItems, Map<InwardChargeType,String> groupByType, Map<InwardChargeType,Integer> orderByType, Map<InwardChargeType,String> labelByType)` — consumed by Task 5's `getBundledFinalBillRows(Bill)` instance wrapper.
  - `groupByType`/`orderByType`/`labelByType` **must share the same key set** — a charge type absent from `groupByType` is treated as *not part of this bundled view at all* (its bill items are silently skipped), which is how Task 5 excludes `ProfessionalCharge`/`DoctorAndNurses` (those are rendered separately with their per-staff breakdown; see Task 6).
  - A present-but-empty (after trim) group value means "print this charge type on its own line."
  - Rows with a final summed amount of exactly `0.0` are dropped (matches the existing `!= 0` guard used throughout `finalBill.xhtml`).
  - Result is sorted ascending by `order` (a grouped row's order is the **minimum** `orderByType` value among its members); ties keep the relative order the inputs were encountered in (stable sort).

- [ ] **Step 1: Write the failing tests**

```java
package com.divudi.bean.inward;

import com.divudi.core.data.dto.FinalBillPrintRowDTO;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.BillItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BhtSummeryControllerBundledRowsTest {

    private static BillItem chargeItem(InwardChargeType type, double adjustedValue) {
        BillItem bi = new BillItem();
        bi.setInwardChargeType(type);
        bi.setAdjustedValue(adjustedValue);
        return bi;
    }

    /** Every charge type present in groupByType with an empty group, own order. */
    private static Map<InwardChargeType, String> emptyGroups(InwardChargeType... types) {
        Map<InwardChargeType, String> m = new EnumMap<>(InwardChargeType.class);
        for (InwardChargeType t : types) {
            m.put(t, "");
        }
        return m;
    }

    private static Map<InwardChargeType, Integer> sequentialOrders(InwardChargeType... types) {
        Map<InwardChargeType, Integer> m = new EnumMap<>(InwardChargeType.class);
        int order = 10;
        for (InwardChargeType t : types) {
            m.put(t, order);
            order += 10;
        }
        return m;
    }

    private static Map<InwardChargeType, String> defaultLabels(InwardChargeType... types) {
        Map<InwardChargeType, String> m = new EnumMap<>(InwardChargeType.class);
        for (InwardChargeType t : types) {
            m.put(t, t.getLabel());
        }
        return m;
    }

    @Test
    void groupsMatchingChargeTypesIntoOneSummedRow() {
        List<BillItem> items = new ArrayList<>();
        items.add(chargeItem(InwardChargeType.RoomCharges, 3000.0));
        items.add(chargeItem(InwardChargeType.MealCharges, 750.0));
        items.add(chargeItem(InwardChargeType.CT, 500.0));

        Map<InwardChargeType, String> groups = emptyGroups(InwardChargeType.RoomCharges, InwardChargeType.MealCharges, InwardChargeType.CT);
        groups.put(InwardChargeType.RoomCharges, "Room Charges");
        groups.put(InwardChargeType.MealCharges, "Room Charges");
        groups.put(InwardChargeType.CT, "Room Charges");
        Map<InwardChargeType, Integer> orders = sequentialOrders(InwardChargeType.RoomCharges, InwardChargeType.MealCharges, InwardChargeType.CT);
        Map<InwardChargeType, String> labels = defaultLabels(InwardChargeType.RoomCharges, InwardChargeType.MealCharges, InwardChargeType.CT);

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(items, groups, orders, labels);

        assertEquals(1, rows.size());
        assertEquals("Room Charges", rows.get(0).getLabel());
        assertEquals(4250.0, rows.get(0).getAmount());
    }

    @Test
    void leavesUngroupedChargeTypeAsIndividualRowWithResolvedLabel() {
        List<BillItem> items = new ArrayList<>();
        items.add(chargeItem(InwardChargeType.Laboratory, 1200.0));

        Map<InwardChargeType, String> groups = emptyGroups(InwardChargeType.Laboratory);
        Map<InwardChargeType, Integer> orders = sequentialOrders(InwardChargeType.Laboratory);
        Map<InwardChargeType, String> labels = new EnumMap<>(InwardChargeType.class);
        labels.put(InwardChargeType.Laboratory, "Lab Charges");

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(items, groups, orders, labels);

        assertEquals(1, rows.size());
        assertEquals("Lab Charges", rows.get(0).getLabel());
        assertEquals(1200.0, rows.get(0).getAmount());
    }

    @Test
    void skipsChargeTypeNotPresentInGroupMap() {
        List<BillItem> items = new ArrayList<>();
        items.add(chargeItem(InwardChargeType.ProfessionalCharge, 26000.0));
        items.add(chargeItem(InwardChargeType.Laboratory, 1200.0));

        // ProfessionalCharge deliberately absent — mirrors how Task 5 excludes it.
        Map<InwardChargeType, String> groups = emptyGroups(InwardChargeType.Laboratory);
        Map<InwardChargeType, Integer> orders = sequentialOrders(InwardChargeType.Laboratory);
        Map<InwardChargeType, String> labels = defaultLabels(InwardChargeType.Laboratory);

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(items, groups, orders, labels);

        assertEquals(1, rows.size());
        assertEquals("Laboratory Charges", rows.get(0).getLabel());
    }

    @Test
    void filtersOutRowsThatSumToZero() {
        List<BillItem> items = new ArrayList<>();
        items.add(chargeItem(InwardChargeType.OxygenCharges, 0.0));

        Map<InwardChargeType, String> groups = emptyGroups(InwardChargeType.OxygenCharges);
        Map<InwardChargeType, Integer> orders = sequentialOrders(InwardChargeType.OxygenCharges);
        Map<InwardChargeType, String> labels = defaultLabels(InwardChargeType.OxygenCharges);

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(items, groups, orders, labels);

        assertTrue(rows.isEmpty());
    }

    @Test
    void sortsByOrder_groupedRowUsesMinimumOrderAmongMembers() {
        List<BillItem> items = new ArrayList<>();
        items.add(chargeItem(InwardChargeType.Laboratory, 1200.0));   // order 10, ungrouped
        items.add(chargeItem(InwardChargeType.RoomCharges, 3000.0));  // order 20, grouped
        items.add(chargeItem(InwardChargeType.MealCharges, 750.0));   // order 999, grouped (higher than RoomCharges)

        Map<InwardChargeType, String> groups = emptyGroups(InwardChargeType.Laboratory, InwardChargeType.RoomCharges, InwardChargeType.MealCharges);
        groups.put(InwardChargeType.RoomCharges, "Room Charges");
        groups.put(InwardChargeType.MealCharges, "Room Charges");

        Map<InwardChargeType, Integer> orders = new EnumMap<>(InwardChargeType.class);
        orders.put(InwardChargeType.Laboratory, 10);
        orders.put(InwardChargeType.RoomCharges, 20);
        orders.put(InwardChargeType.MealCharges, 999);

        Map<InwardChargeType, String> labels = defaultLabels(InwardChargeType.Laboratory, InwardChargeType.RoomCharges, InwardChargeType.MealCharges);

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(items, groups, orders, labels);

        // Laboratory (order 10) first, then Room Charges group (min(20, 999) = 20)
        assertEquals(2, rows.size());
        assertEquals("Laboratory Charges", rows.get(0).getLabel());
        assertEquals("Room Charges", rows.get(1).getLabel());
        assertEquals(3750.0, rows.get(1).getAmount());
    }

    @Test
    void treatsBlankGroupTextAsUngrouped() {
        List<BillItem> items = new ArrayList<>();
        items.add(chargeItem(InwardChargeType.Laboratory, 1200.0));

        Map<InwardChargeType, String> groups = new EnumMap<>(InwardChargeType.class);
        groups.put(InwardChargeType.Laboratory, "   "); // whitespace-only, must trim to empty
        Map<InwardChargeType, Integer> orders = sequentialOrders(InwardChargeType.Laboratory);
        Map<InwardChargeType, String> labels = defaultLabels(InwardChargeType.Laboratory);

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(items, groups, orders, labels);

        assertEquals(1, rows.size());
        assertEquals("Laboratory Charges", rows.get(0).getLabel());
    }

    @Test
    void returnsEmptyList_whenBillItemsIsNull() {
        Map<InwardChargeType, String> groups = emptyGroups(InwardChargeType.Laboratory);
        Map<InwardChargeType, Integer> orders = sequentialOrders(InwardChargeType.Laboratory);
        Map<InwardChargeType, String> labels = defaultLabels(InwardChargeType.Laboratory);

        List<FinalBillPrintRowDTO> rows = BhtSummeryController.buildBundledRows(null, groups, orders, labels);

        assertTrue(rows.isEmpty());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./detect-maven.sh test -Dtest=BhtSummeryControllerBundledRowsTest`
Expected: FAIL — `buildBundledRows` does not exist on `BhtSummeryController` (compile error).

- [ ] **Step 3: Write the implementation**

Open `src/main/java/com/divudi/bean/inward/BhtSummeryController.java`. Confirm these imports already exist near the top (they do, as of this branch — `java.util.ArrayList`, `java.util.Comparator`, `java.util.LinkedHashMap`, `java.util.List`, `java.util.Map`); if any are missing, add them. Add one new import:

```java
import com.divudi.core.data.dto.FinalBillPrintRowDTO;
```

Find this exact text (the end of `getSummaryOfDoctorChargers`, immediately followed by `navigateToIntrimBill`):

```java
        List<BillFee> proFees = new ArrayList<>(staffFeeMap.values());

        if (!proFees.isEmpty()) {
            BillItem newBillItem = new BillItem();
            newBillItem.setInwardChargeType(InwardChargeType.ProfessionalCharge);
            newBillItem.setProFees(proFees);
            newBillItem.setAdjustedValue(totalFee);
            newBillItems.add(newBillItem);
        }

        return newBillItems;
    }

    public String navigateToIntrimBill() {
```

Replace it with (inserting the new method between the two):

```java
        List<BillFee> proFees = new ArrayList<>(staffFeeMap.values());

        if (!proFees.isEmpty()) {
            BillItem newBillItem = new BillItem();
            newBillItem.setInwardChargeType(InwardChargeType.ProfessionalCharge);
            newBillItem.setProFees(proFees);
            newBillItem.setAdjustedValue(totalFee);
            newBillItems.add(newBillItem);
        }

        return newBillItems;
    }

    /**
     * Pure grouping/summing algorithm behind the "Bundled Custom 1" Final Bill
     * format (configurable charge-type grouping, #23340 follow-up). Kept
     * static and free of CDI-injected fields so it is directly unit-testable
     * (see BhtSummeryControllerBundledRowsTest), following the same pattern as
     * InwardProfessionalFeeSummaryController's static summary methods.
     *
     * <p>{@code groupByType}, {@code orderByType}, and {@code labelByType}
     * must share the same key set. A charge type <b>absent</b> from
     * {@code groupByType} is excluded from this bundled view entirely — its
     * BillItems are silently skipped (this is how callers keep charge types
     * with their own special rendering, like ProfessionalCharge, out of the
     * generic row list). A charge type <b>present</b> with a blank (after
     * trim) group value prints as its own row, labeled via
     * {@code labelByType}. Charge types sharing the same non-blank group text
     * are summed into one row labeled with that group text.
     *
     * <p>Rows whose final amount is exactly {@code 0.0} are dropped. The
     * result is sorted ascending by order; a grouped row's order is the
     * minimum {@code orderByType} value among its members.
     */
    public static List<FinalBillPrintRowDTO> buildBundledRows(
            List<BillItem> billItems,
            Map<InwardChargeType, String> groupByType,
            Map<InwardChargeType, Integer> orderByType,
            Map<InwardChargeType, String> labelByType) {

        Map<InwardChargeType, Double> totalsByType = new java.util.EnumMap<>(InwardChargeType.class);
        if (billItems != null) {
            for (BillItem bi : billItems) {
                InwardChargeType type = bi == null ? null : bi.getInwardChargeType();
                if (type == null || !groupByType.containsKey(type)) {
                    continue;
                }
                totalsByType.merge(type, bi.getAdjustedValue(), Double::sum);
            }
        }

        List<FinalBillPrintRowDTO> individualRows = new ArrayList<>();
        Map<String, Double> groupedTotals = new LinkedHashMap<>();
        Map<String, Integer> groupedOrder = new LinkedHashMap<>();

        for (Map.Entry<InwardChargeType, Double> entry : totalsByType.entrySet()) {
            InwardChargeType type = entry.getKey();
            double amount = entry.getValue();
            String rawGroup = groupByType.get(type);
            String group = rawGroup == null ? "" : rawGroup.trim();
            int order = orderByType.getOrDefault(type, Integer.MAX_VALUE);

            if (group.isEmpty()) {
                String label = labelByType.getOrDefault(type, type.getLabel());
                individualRows.add(new FinalBillPrintRowDTO(label, amount, order));
            } else {
                groupedTotals.merge(group, amount, Double::sum);
                groupedOrder.merge(group, order, Math::min);
            }
        }

        List<FinalBillPrintRowDTO> rows = new ArrayList<>(individualRows);
        for (Map.Entry<String, Double> entry : groupedTotals.entrySet()) {
            String group = entry.getKey();
            rows.add(new FinalBillPrintRowDTO(group, entry.getValue(), groupedOrder.get(group)));
        }

        rows.removeIf(row -> row.getAmount() == 0.0);
        rows.sort(Comparator.comparingInt(FinalBillPrintRowDTO::getOrder));
        return rows;
    }

    public String navigateToIntrimBill() {
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./detect-maven.sh test -Dtest=BhtSummeryControllerBundledRowsTest`
Expected: PASS (7 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/BhtSummeryController.java src/test/java/com/divudi/bean/inward/BhtSummeryControllerBundledRowsTest.java
git commit -m "feat(inward): add buildBundledRows charge-type grouping algorithm

Pure static method, unit-tested directly (no CDI container needed),
following the InwardProfessionalFeeSummaryController precedent. This is
the core logic behind the configurable Final Bill charge-type grouping
(design doc 2026-08-31-inward-final-bill-charge-type-grouping-design.md).

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 3: Admin-configurable "Final Bill Group" setting

**Files:**
- Modify: `src/main/java/com/divudi/bean/common/ConfigOptionApplicationController.java:1418-1426` (immediately after the existing `getInwardChargeTypeFinalBillOrder`/`saveInwardChargeTypeFinalBillOrder` pair)
- Modify: `src/main/java/com/divudi/bean/common/ConfigOptionController.java` (new prefix constant near line 72; extend `isInwardChargeTypeLabelKey` at line 595-600)
- Modify: `src/main/java/com/divudi/bean/inward/InwardChargeTypeLabelController.java` (new `groupMap` field, wired into `init()`, `saveAll()`, `saveOne()`, plus getter/setter)
- Modify: `src/main/webapp/inward/inward_charge_type_labels.xhtml` (new "Final Bill Group" column)

**Interfaces:**
- Produces: `ConfigOptionApplicationController.getInwardChargeTypeFinalBillGroup(InwardChargeType type)` → `String` (default `""`); `saveInwardChargeTypeFinalBillGroup(InwardChargeType type, String group)` — consumed by Task 5.

- [ ] **Step 1: Add the get/save methods to `ConfigOptionApplicationController`**

Find this exact text:

```java
    public int getInwardChargeTypeFinalBillOrder(InwardChargeType type) {
        String key = "Inward Charge Type Final Bill Order - " + type.name();
        Integer v = getIntegerValueByKey(key, (type.ordinal() + 1) * 10);
        return v == null ? (type.ordinal() + 1) * 10 : v;
    }

    public void saveInwardChargeTypeFinalBillOrder(InwardChargeType type, int order) {
        setIntegerValueByKey("Inward Charge Type Final Bill Order - " + type.name(), order);
    }
```

Replace with:

```java
    public int getInwardChargeTypeFinalBillOrder(InwardChargeType type) {
        String key = "Inward Charge Type Final Bill Order - " + type.name();
        Integer v = getIntegerValueByKey(key, (type.ordinal() + 1) * 10);
        return v == null ? (type.ordinal() + 1) * 10 : v;
    }

    public void saveInwardChargeTypeFinalBillOrder(InwardChargeType type, int order) {
        setIntegerValueByKey("Inward Charge Type Final Bill Order - " + type.name(), order);
    }

    /**
     * Free-text grouping key for the "Bundled Custom 1" Final Bill print
     * format: charge types sharing the same non-blank group text print as
     * one summed line (see BhtSummeryController#buildBundledRows). Default
     * empty — every charge type prints on its own line until an admin sets
     * this, so no hospital is affected until it opts in.
     */
    public String getInwardChargeTypeFinalBillGroup(InwardChargeType type) {
        String key = "Inward Charge Type Final Bill Group - " + type.name();
        return getShortTextValueByKey(key, "");
    }

    public void saveInwardChargeTypeFinalBillGroup(InwardChargeType type, String group) {
        String key = "Inward Charge Type Final Bill Group - " + type.name();
        saveShortTextOption(key, group == null ? "" : group.trim());
    }
```

- [ ] **Step 2: Extend the Application Options admin-page guard in `ConfigOptionController`**

Find this exact text (the three prefix constants):

```java
    private static final String INWARD_CHARGE_TYPE_REPORT_ORDER_KEY_PREFIX = "Inward Charge Type Report Order - ";

    /**
     * Prefix shared by every Inward Charge Type Final Bill Order ConfigOption
     * key (e.g. "Inward Charge Type Final Bill Order - ROOM_CHARGE"). Same
     * dedicated editor as {@link #INWARD_CHARGE_TYPE_LABEL_KEY_PREFIX} (issue
     * #23340).
     */
    private static final String INWARD_CHARGE_TYPE_FINAL_BILL_ORDER_KEY_PREFIX = "Inward Charge Type Final Bill Order - ";
```

Replace with:

```java
    private static final String INWARD_CHARGE_TYPE_REPORT_ORDER_KEY_PREFIX = "Inward Charge Type Report Order - ";

    /**
     * Prefix shared by every Inward Charge Type Final Bill Order ConfigOption
     * key (e.g. "Inward Charge Type Final Bill Order - ROOM_CHARGE"). Same
     * dedicated editor as {@link #INWARD_CHARGE_TYPE_LABEL_KEY_PREFIX} (issue
     * #23340).
     */
    private static final String INWARD_CHARGE_TYPE_FINAL_BILL_ORDER_KEY_PREFIX = "Inward Charge Type Final Bill Order - ";

    /**
     * Prefix shared by every Inward Charge Type Final Bill Group ConfigOption
     * key (e.g. "Inward Charge Type Final Bill Group - ROOM_CHARGE"). Same
     * dedicated editor as {@link #INWARD_CHARGE_TYPE_LABEL_KEY_PREFIX}
     * (configurable Final Bill charge-type grouping, #23340 follow-up).
     */
    private static final String INWARD_CHARGE_TYPE_FINAL_BILL_GROUP_KEY_PREFIX = "Inward Charge Type Final Bill Group - ";
```

Then find this exact text:

```java
    public boolean isInwardChargeTypeLabelKey(String optionKey) {
        return optionKey != null
                && (optionKey.startsWith(INWARD_CHARGE_TYPE_LABEL_KEY_PREFIX)
                || optionKey.startsWith(INWARD_CHARGE_TYPE_REPORT_ORDER_KEY_PREFIX)
                || optionKey.startsWith(INWARD_CHARGE_TYPE_FINAL_BILL_ORDER_KEY_PREFIX));
    }
```

Replace with:

```java
    public boolean isInwardChargeTypeLabelKey(String optionKey) {
        return optionKey != null
                && (optionKey.startsWith(INWARD_CHARGE_TYPE_LABEL_KEY_PREFIX)
                || optionKey.startsWith(INWARD_CHARGE_TYPE_REPORT_ORDER_KEY_PREFIX)
                || optionKey.startsWith(INWARD_CHARGE_TYPE_FINAL_BILL_ORDER_KEY_PREFIX)
                || optionKey.startsWith(INWARD_CHARGE_TYPE_FINAL_BILL_GROUP_KEY_PREFIX));
    }
```

- [ ] **Step 3: Run compile check**

Run: `./detect-maven.sh compile -q`
Expected: no output, exit code 0 (compiles clean).

- [ ] **Step 4: Wire `groupMap` into `InwardChargeTypeLabelController`**

Open `src/main/java/com/divudi/bean/inward/InwardChargeTypeLabelController.java`. Find:

```java
    private List<InwardChargeType> chargeTypes;
    private Map<String, String> labelMap;
    private Map<String, String> reportOrderMap;
    private Map<String, String> finalBillOrderMap;

    @PostConstruct
    public void init() {
        chargeTypes = Arrays.asList(InwardChargeType.values());
        labelMap = new HashMap<>();
        reportOrderMap = new HashMap<>();
        finalBillOrderMap = new HashMap<>();
        for (InwardChargeType type : chargeTypes) {
            String custom = configOptionApplicationController.getShortTextValueByKey(
                    "Inward Charge Type Label - " + type.name(), "");
            labelMap.put(type.name(), custom == null ? "" : custom);
            // Kept as String (not Integer) the same way labelMap is: a p:inputText
            // bound to a Map<String, Integer> entry submits a raw String, since
            // MapELResolver.setValue() puts the value as-is without consulting the
            // map's erased generic type — an Integer-typed map caused a
            // ClassCastException on save (issue #23340 QA). Parsed back to int only
            // where actually needed, in orderOrDefault().
            reportOrderMap.put(type.name(), String.valueOf(configOptionApplicationController.getInwardChargeTypeReportOrder(type)));
            finalBillOrderMap.put(type.name(), String.valueOf(configOptionApplicationController.getInwardChargeTypeFinalBillOrder(type)));
        }
    }

    public void saveAll() {
        for (InwardChargeType type : chargeTypes) {
            String custom = labelMap.get(type.name());
            configOptionApplicationController.saveInwardChargeTypeLabel(type, custom);
            configOptionApplicationController.saveInwardChargeTypeReportOrder(type, orderOrDefault(reportOrderMap, type));
            configOptionApplicationController.saveInwardChargeTypeFinalBillOrder(type, orderOrDefault(finalBillOrderMap, type));
        }
    }

    public void saveOne(InwardChargeType type) {
        String custom = labelMap.get(type.name());
        configOptionApplicationController.saveInwardChargeTypeLabel(type, custom);
        configOptionApplicationController.saveInwardChargeTypeReportOrder(type, orderOrDefault(reportOrderMap, type));
        configOptionApplicationController.saveInwardChargeTypeFinalBillOrder(type, orderOrDefault(finalBillOrderMap, type));
    }
```

Replace with:

```java
    private List<InwardChargeType> chargeTypes;
    private Map<String, String> labelMap;
    private Map<String, String> reportOrderMap;
    private Map<String, String> finalBillOrderMap;
    private Map<String, String> finalBillGroupMap;

    @PostConstruct
    public void init() {
        chargeTypes = Arrays.asList(InwardChargeType.values());
        labelMap = new HashMap<>();
        reportOrderMap = new HashMap<>();
        finalBillOrderMap = new HashMap<>();
        finalBillGroupMap = new HashMap<>();
        for (InwardChargeType type : chargeTypes) {
            String custom = configOptionApplicationController.getShortTextValueByKey(
                    "Inward Charge Type Label - " + type.name(), "");
            labelMap.put(type.name(), custom == null ? "" : custom);
            // Kept as String (not Integer) the same way labelMap is: a p:inputText
            // bound to a Map<String, Integer> entry submits a raw String, since
            // MapELResolver.setValue() puts the value as-is without consulting the
            // map's erased generic type — an Integer-typed map caused a
            // ClassCastException on save (issue #23340 QA). Parsed back to int only
            // where actually needed, in orderOrDefault().
            reportOrderMap.put(type.name(), String.valueOf(configOptionApplicationController.getInwardChargeTypeReportOrder(type)));
            finalBillOrderMap.put(type.name(), String.valueOf(configOptionApplicationController.getInwardChargeTypeFinalBillOrder(type)));
            finalBillGroupMap.put(type.name(), configOptionApplicationController.getInwardChargeTypeFinalBillGroup(type));
        }
    }

    public void saveAll() {
        for (InwardChargeType type : chargeTypes) {
            String custom = labelMap.get(type.name());
            configOptionApplicationController.saveInwardChargeTypeLabel(type, custom);
            configOptionApplicationController.saveInwardChargeTypeReportOrder(type, orderOrDefault(reportOrderMap, type));
            configOptionApplicationController.saveInwardChargeTypeFinalBillOrder(type, orderOrDefault(finalBillOrderMap, type));
            configOptionApplicationController.saveInwardChargeTypeFinalBillGroup(type, finalBillGroupMap.get(type.name()));
        }
    }

    public void saveOne(InwardChargeType type) {
        String custom = labelMap.get(type.name());
        configOptionApplicationController.saveInwardChargeTypeLabel(type, custom);
        configOptionApplicationController.saveInwardChargeTypeReportOrder(type, orderOrDefault(reportOrderMap, type));
        configOptionApplicationController.saveInwardChargeTypeFinalBillOrder(type, orderOrDefault(finalBillOrderMap, type));
        configOptionApplicationController.saveInwardChargeTypeFinalBillGroup(type, finalBillGroupMap.get(type.name()));
    }
```

Then find:

```java
    public Map<String, String> getFinalBillOrderMap() {
        return finalBillOrderMap;
    }

    public void setFinalBillOrderMap(Map<String, String> finalBillOrderMap) {
        this.finalBillOrderMap = finalBillOrderMap;
    }
}
```

Replace with:

```java
    public Map<String, String> getFinalBillOrderMap() {
        return finalBillOrderMap;
    }

    public void setFinalBillOrderMap(Map<String, String> finalBillOrderMap) {
        this.finalBillOrderMap = finalBillOrderMap;
    }

    public Map<String, String> getFinalBillGroupMap() {
        return finalBillGroupMap;
    }

    public void setFinalBillGroupMap(Map<String, String> finalBillGroupMap) {
        this.finalBillGroupMap = finalBillGroupMap;
    }
}
```

- [ ] **Step 5: Add the "Final Bill Group" column to the admin page**

Open `src/main/webapp/inward/inward_charge_type_labels.xhtml`. Find:

```xhtml
                            <p:column
                                headerText="Final Bill Order"
                                style="width: 13%; padding: 7px;">
                                <p:inputText
                                    style="width: 100%; text-align: right;"
                                    value="#{inwardChargeTypeLabelController.finalBillOrderMap[chargeType.name()]}"
                                    title="Final bill order for #{chargeType.name()}">
                                    <f:validateRegex pattern="^[0-9]{1,6}$"/>
                                </p:inputText>
                            </p:column>

                            <p:column
                                headerText="Save"
                                style="width: 8%; padding: 7px; text-align: center;">
```

Replace with:

```xhtml
                            <p:column
                                headerText="Final Bill Order"
                                style="width: 11%; padding: 7px;">
                                <p:inputText
                                    style="width: 100%; text-align: right;"
                                    value="#{inwardChargeTypeLabelController.finalBillOrderMap[chargeType.name()]}"
                                    title="Final bill order for #{chargeType.name()}">
                                    <f:validateRegex pattern="^[0-9]{1,6}$"/>
                                </p:inputText>
                            </p:column>

                            <p:column
                                headerText="Final Bill Group"
                                style="width: 14%; padding: 7px;">
                                <p:inputText
                                    style="width: 100%;"
                                    value="#{inwardChargeTypeLabelController.finalBillGroupMap[chargeType.name()]}"
                                    placeholder="Leave blank to print alone"
                                    title="Charge types sharing the same text here print as one summed line on the Bundled Custom 1 Final Bill format"/>
                            </p:column>

                            <p:column
                                headerText="Save"
                                style="width: 8%; padding: 7px; text-align: center;">
```

Also adjust the `Custom Label` column's width from `25%` to `22%` and `Report Order` from `12%` to `11%` so the row still totals ~100% (cosmetic only — PrimeFaces doesn't hard-fail on a slight overflow, but keep it tidy):

Find:
```xhtml
                            <p:column
                                headerText="Custom Label"
                                style="width: 25%; padding: 7px;">
```
Replace with:
```xhtml
                            <p:column
                                headerText="Custom Label"
                                style="width: 22%; padding: 7px;">
```

Find:
```xhtml
                            <p:column
                                headerText="Report Order"
                                style="width: 12%; padding: 7px;">
```
Replace with:
```xhtml
                            <p:column
                                headerText="Report Order"
                                style="width: 11%; padding: 7px;">
```

- [ ] **Step 6: Compile check**

Run: `./detect-maven.sh compile -q`
Expected: no output, exit code 0.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/divudi/bean/common/ConfigOptionApplicationController.java src/main/java/com/divudi/bean/common/ConfigOptionController.java src/main/java/com/divudi/bean/inward/InwardChargeTypeLabelController.java src/main/webapp/inward/inward_charge_type_labels.xhtml
git commit -m "feat(inward): admin-configurable Final Bill Group per charge type

Fourth per-InwardChargeType setting on the existing Inward Charge Type
Labels admin page, alongside Custom Label / Report Order / Final Bill
Order (#23340). Default empty — every charge type prints on its own
line until an admin sets this. Also extends the existing Application
Options admin-page guard (issue #23257) to lock the new key prefix,
same as the other three.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 4: Expose the new field on the discovery API

**Files:**
- Modify: `src/main/java/com/divudi/ws/common/ConfigResource.java:207-223`

**Interfaces:**
- Consumes: `ConfigOptionApplicationController.getInwardChargeTypeFinalBillGroup(type)` (Task 3).

- [ ] **Step 1: Add the field to the JSON response**

Find this exact text:

```java
    @GET
    @Path("inward-charge-types")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listInwardChargeTypes(@Context HttpHeaders headers) {
        if (validateConfigKey(headers) == null) {
            return unauthorizedResponse();
        }

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (InwardChargeType type : InwardChargeType.values()) {
            JsonObjectBuilder obj = Json.createObjectBuilder()
                    .add("name", type.name())
                    .add("defaultLabel", type.getLabel() != null ? type.getLabel() : "")
                    .add("label", configOptionApplicationController.getInwardChargeTypeLabel(type))
                    .add("reportOrder", configOptionApplicationController.getInwardChargeTypeReportOrder(type))
                    .add("finalBillOrder", configOptionApplicationController.getInwardChargeTypeFinalBillOrder(type));
            arrayBuilder.add(obj);
        }
        return Response.ok(arrayBuilder.build().toString()).build();
    }
```

Replace with:

```java
    @GET
    @Path("inward-charge-types")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listInwardChargeTypes(@Context HttpHeaders headers) {
        if (validateConfigKey(headers) == null) {
            return unauthorizedResponse();
        }

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (InwardChargeType type : InwardChargeType.values()) {
            JsonObjectBuilder obj = Json.createObjectBuilder()
                    .add("name", type.name())
                    .add("defaultLabel", type.getLabel() != null ? type.getLabel() : "")
                    .add("label", configOptionApplicationController.getInwardChargeTypeLabel(type))
                    .add("reportOrder", configOptionApplicationController.getInwardChargeTypeReportOrder(type))
                    .add("finalBillOrder", configOptionApplicationController.getInwardChargeTypeFinalBillOrder(type))
                    .add("finalBillGroup", configOptionApplicationController.getInwardChargeTypeFinalBillGroup(type));
            arrayBuilder.add(obj);
        }
        return Response.ok(arrayBuilder.build().toString()).build();
    }
```

Also update the method's Javadoc (the comment block directly above `public Response listInwardChargeTypes`) to mention the new field — find:

```java
     * Discovery endpoint for the InwardChargeType enum: for every value,
     * returns its default/custom label plus the two admin-configurable
     * ordering numbers (Report Order, Final Bill Order) introduced in issue
     * #23340. Reading each value here also lazily seeds any of their
```

Replace with:

```java
     * Discovery endpoint for the InwardChargeType enum: for every value,
     * returns its default/custom label, the two admin-configurable ordering
     * numbers (Report Order, Final Bill Order) introduced in issue #23340,
     * and the Final Bill Group text (configurable charge-type grouping,
     * #23340 follow-up). Reading each value here also lazily seeds any of their
```

- [ ] **Step 2: Compile check**

Run: `./detect-maven.sh compile -q`
Expected: no output, exit code 0.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/ws/common/ConfigResource.java
git commit -m "feat(api): expose Final Bill Group on inward-charge-types discovery endpoint

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 5: `getBundledFinalBillRows(Bill)` — the CDI-aware wrapper

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java` (add instance method right after the new `buildBundledRows` static method from Task 2)

**Interfaces:**
- Consumes: `buildBundledRows(...)` (Task 2); `configOptionApplicationController.getInwardChargeTypeFinalBillGroup/FinalBillOrder/Label(type)` (Task 3, pre-existing).
- Produces: `public List<FinalBillPrintRowDTO> getBundledFinalBillRows(Bill bill)` — consumed by Task 6's `finalBillBundledCustom1.xhtml` via `#{bhtSummeryController.getBundledFinalBillRows(cc.attrs.bill)}`.

- [ ] **Step 1: Add the wrapper method**

Find this exact text (the closing of `buildBundledRows`, immediately followed by `navigateToIntrimBill` — this is the end of what Task 2 inserted):

```java
        rows.removeIf(row -> row.getAmount() == 0.0);
        rows.sort(Comparator.comparingInt(FinalBillPrintRowDTO::getOrder));
        return rows;
    }

    public String navigateToIntrimBill() {
```

Replace with:

```java
        rows.removeIf(row -> row.getAmount() == 0.0);
        rows.sort(Comparator.comparingInt(FinalBillPrintRowDTO::getOrder));
        return rows;
    }

    /**
     * CDI-aware wrapper around {@link #buildBundledRows}, used by
     * finalBillBundledCustom1.xhtml. Deliberately excludes ProfessionalCharge
     * and DoctorAndNurses from the charge-type universe passed to
     * buildBundledRows — those two are always printed separately with their
     * per-staff fee breakdown (see the composite), never folded into a
     * generic summed row, so their BillItems must not double-count here.
     */
    public List<FinalBillPrintRowDTO> getBundledFinalBillRows(Bill bill) {
        List<BillItem> items = bill == null ? new ArrayList<>() : bill.getBillItems();

        Map<InwardChargeType, String> groupByType = new java.util.EnumMap<>(InwardChargeType.class);
        Map<InwardChargeType, Integer> orderByType = new java.util.EnumMap<>(InwardChargeType.class);
        Map<InwardChargeType, String> labelByType = new java.util.EnumMap<>(InwardChargeType.class);

        for (InwardChargeType type : InwardChargeType.values()) {
            if (type == InwardChargeType.ProfessionalCharge || type == InwardChargeType.DoctorAndNurses) {
                continue;
            }
            groupByType.put(type, configOptionApplicationController.getInwardChargeTypeFinalBillGroup(type));
            orderByType.put(type, configOptionApplicationController.getInwardChargeTypeFinalBillOrder(type));
            labelByType.put(type, configOptionApplicationController.getInwardChargeTypeLabel(type));
        }

        return buildBundledRows(items, groupByType, orderByType, labelByType);
    }

    public String navigateToIntrimBill() {
```

- [ ] **Step 2: Compile check**

Run: `./detect-maven.sh compile -q`
Expected: no output, exit code 0.

(No new unit test here: this method's only job is wiring real `ConfigOption` reads into `buildBundledRows`, which is already fully covered by Task 2's tests. Its actual behavior is verified end-to-end in Task 9's Playwright/DB pass, matching how every other `BhtSummeryController` CDI-dependent method in this codebase is verified.)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/BhtSummeryController.java
git commit -m "feat(inward): add getBundledFinalBillRows(Bill) wrapper

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 6: New print composite `finalBillBundledCustom1.xhtml`

**Files:**
- Create: `src/main/webapp/resources/inward/bill/finalBillBundledCustom1.xhtml`

**Interfaces:**
- Consumes: `bhtSummeryController.getBundledFinalBillRows(Bill)` (Task 5). Composite interface mirrors `finalBill.xhtml`: `bill` (Bill), `duplicate` (Boolean), `hosCopy`, `showProfessional` (Boolean, default true).
- Produces: available automatically as `<bi:finalBillBundledCustom1 .../>` (the `bi` namespace `http://xmlns.jcp.org/jsf/composite/inward/bill` already maps to this directory — no registration needed, same as `finalBillCustom4.xhtml`) — consumed by Task 7.

- [ ] **Step 1: Create the file**

```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:cc="http://xmlns.jcp.org/jsf/composite"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:p="http://primefaces.org/ui"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets">

    <!--
        "Bundled Custom 1" Final Bill format — configurable charge-type
        grouping (design doc 2026-08-31-inward-final-bill-charge-type-
        grouping-design.md). Header / patient-info / payment / signature
        blocks below are copied verbatim from finalBill.xhtml so this format
        looks identical to the default one except for the charge-lines table,
        which is driven by bhtSummeryController.getBundledFinalBillRows(bill)
        instead of a per-charge-type ui:repeat chain.

        Opt-in only: this file is never touched by any existing hospital's
        print flow. It is only reachable via the "Bundled Custom 1" panel in
        the Custom Bills tab, itself gated by the
        'Inward Final Bill - Show Bundled Custom 1 Format' ConfigOption
        (default false). See inward_reprint_bill_final.xhtml.
    -->

    <!-- INTERFACE -->
    <cc:interface>
        <cc:attribute name="bill" type="com.divudi.core.entity.Bill" />
        <cc:attribute name="duplicate" type="java.lang.Boolean"/>
        <cc:attribute name="hosCopy" />
        <cc:attribute name="showProfessional" type="java.lang.Boolean" default="true"/>
    </cc:interface>

    <!-- IMPLEMENTATION -->
    <cc:implementation>
        <h:outputStylesheet library="css" name="printing.css"/>

        <h:panelGroup id="gpBillPreview" >
            <div  class="container-fluid" style="width: 214mm!important; margin-bottom: 2.5cm!important">
                <div  style="margin-top: #{configOptionApplicationController.getLongTextValueByKey('Inward Final Bill Header Margin-Top Space','4cm')}"></div>

                <div style="font-family: verdana ">

                    <h:panelGroup rendered="#{configOptionApplicationController.getLongTextValueByKey('Inward Final Bill Header Template') ne ''}">
                        <div class="row">
                            <div class="col-12">
                                <h:outputText value="#{inwardSearch.fillDataForInpatientsFinalBillHeader(configOptionApplicationController.getLongTextValueByKey('Inward Final Bill Header Template').toString() , cc.attrs.bill)}"
                                              escape="false"
                                              >
                                </h:outputText>
                            </div>
                        </div>
                    </h:panelGroup>

                    <h:panelGroup rendered="#{configOptionApplicationController.getLongTextValueByKey('Inward Final Bill Header Template') eq ''}">
                        <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Show Inward Final bill header with institution details',false)}">
                            <div>
                                <div class="col-12 text-center">
                                    <h1 class=""><h:outputLabel value="#{cc.attrs.bill.institution.name}" /></h1>
                                    <h2><h:outputLabel value="#{cc.attrs.bill.institution.address}" /></h2>
                                    <h3>#{cc.attrs.bill.institution.phone}</h3>
                                    <h4>#{cc.attrs.bill.institution.fax}</h4>
                                    <h4>#{cc.attrs.bill.institution.email}</h4>
                                    <h4>#{cc.attrs.bill.institution.web}</h4>
                                </div>
                            </div>
                        </h:panelGroup>
                    </h:panelGroup>

                    <table class="headingPrinting" >
                        <h:panelGroup rendered="#{not empty configOptionApplicationController.getLongTextValueByKey('Inaptient Final Bill Header Image URL')}" >
                            <tr >
                                <td colspan="4">
                                    <h:graphicImage
                                        style="width: 100%;"
                                        class="mb-4"
                                        url="#{configOptionApplicationController.getLongTextValueByKey('Inaptient Final Bill Header Image URL')}"
                                        >
                                    </h:graphicImage>
                                </td>
                            </tr>
                        </h:panelGroup>

                        <tr>
                            <td colspan="4">
                                <h:outputLabel class="mark" style="font-size: 18px;" value="**Hospital Bill**" rendered="#{cc.attrs.hosCopy eq true }"/>
                                <h:outputLabel class="mark" style="font-size: 18px;" value="**Original Bill**" rendered="#{cc.attrs.duplicate ne true }"/>
                                <h:outputLabel class="mark" style="font-size: 18px;" value="**Duplicate Bill**" rendered="#{cc.attrs.duplicate eq true}"/>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="4">
                                <h:outputLabel style="font-size: 17px;" value="#{configOptionApplicationController.getLongTextValueByKey('Final Bill Title','Tax Invoice')}"/>
                            </td>
                        </tr>

                    </table>


                    <!-- ===== A4 Bill Header (BHT, Name, ...) - auto-wrapping 2-column grid ===== -->
                    <div style="font-size: 13px; width: 90%; margin-left: 5%; margin-right: 5%" class="mt-4 mb-4">
                        <div class="row gx-0">

                            <!-- BHT No -->
                            <div class="col-6 d-flex mb-1">
                                <span style="width: 130px;">BHT No</span>
                                <span style="width: 12px; text-align: center;">:</span>
                                <span class="flex-fill ps-2">
                                    <h:outputLabel value="#{cc.attrs.bill.patientEncounter.bhtNo}" />
                                </span>
                            </div>

                            <!-- Bill No -->
                            <div class="col-6 d-flex mb-1">
                                <span style="width: 130px;">Bill No</span>
                                <span style="width: 12px; text-align: center;">:</span>
                                <span class="flex-fill ps-2">
                                    <h:outputLabel value="#{cc.attrs.bill.deptId}" />
                                </span>
                            </div>

                            <!-- Patient Name -->
                            <div class="col-6 d-flex mb-1">
                                <span style="width: 130px;">Patient Name</span>
                                <span style="width: 12px; text-align: center;">:</span>
                                <span class="flex-fill ps-2">
                                    <h:outputLabel value="#{cc.attrs.bill.patientEncounter.patient.person.nameWithTitle}"/>
                                </span>
                            </div>

                            <!-- Address -->
                            <h:panelGroup layout="block" styleClass="col-6 d-flex mb-1"
                                          rendered="#{configOptionApplicationController.getBooleanValueByKey('Print Patient Address on Final Bill',true)}">
                                <span style="width: 130px;">Address</span>
                                <span style="width: 12px; text-align: center;">:</span>
                                <span class="flex-fill ps-2">
                                    <h:outputLabel value="#{cc.attrs.bill.patientEncounter.patient.person.address}"/>
                                </span>
                            </h:panelGroup>

                            <!-- Patient Age/Sex -->
                            <h:panelGroup layout="block" styleClass="col-6 d-flex mb-1"
                                          rendered="#{configOptionApplicationController.getBooleanValueByKey('Print Patient Gender/Age on Final Bill',false)}">
                                <span style="width: 130px;">Patient Age/Sex</span>
                                <span style="width: 12px; text-align: center;">:</span>
                                <span class="flex-fill ps-2">
                                    <h:outputLabel value="#{cc.attrs.bill.patientEncounter.patient.person.ageAsShortString} / #{cc.attrs.bill.patientEncounter.patient.person.sex}"/>
                                </span>
                            </h:panelGroup>

                            <!-- NIC No -->
                            <h:panelGroup layout="block" styleClass="col-6 d-flex mb-1"
                                          rendered="#{configOptionApplicationController.getBooleanValueByKey('Print Patient NIC on Final Bill',false) or (cc.attrs.bill.patientEncounter.patient.person.nic ne null and !cc.attrs.bill.patientEncounter.patient.person.nic.equals(''))}">
                                <span style="width: 130px;">NIC No</span>
                                <span style="width: 12px; text-align: center;">:</span>
                                <span class="flex-fill ps-2">
                                    <h:outputLabel value="#{cc.attrs.bill.patientEncounter.patient.person.nic ne null and !cc.attrs.bill.patientEncounter.patient.person.nic.equals('') ? cc.attrs.bill.patientEncounter.patient.person.nic : 'N/A'}"/>
                                </span>
                            </h:panelGroup>

                            <!-- Phone No -->
                            <h:panelGroup layout="block" styleClass="col-6 d-flex mb-1"
                                          rendered="#{configOptionApplicationController.getBooleanValueByKey('Print Patient Phone Number on Final Bill',false)}">
                                <span style="width: 130px;">Phone No</span>
                                <span style="width: 12px; text-align: center;">:</span>
                                <span class="flex-fill ps-2">
                                    <h:outputLabel value="#{cc.attrs.bill.patientEncounter.patient.person.phone}"/>
                                </span>
                            </h:panelGroup>

                            <!-- Admission At -->
                            <div class="col-6 d-flex mb-1">
                                <span style="width: 130px;">Admission At</span>
                                <span style="width: 12px; text-align: center;">:</span>
                                <span class="flex-fill ps-2">
                                    <h:outputLabel value="#{cc.attrs.bill.patientEncounter.printingAdmissionTime ne null and !cc.attrs.bill.patientEncounter.printingAdmissionTime.equals('') ? cc.attrs.bill.patientEncounter.printingAdmissionTime : cc.attrs.bill.patientEncounter.dateOfAdmission}">
                                        <f:convertDateTime pattern="#{sessionController.applicationPreference.longDateTimeFormat}" />
                                    </h:outputLabel>
                                </span>
                            </div>

                            <!-- Discharged At -->
                            <div class="col-6 d-flex mb-1">
                                <span style="width: 130px;">Discharged At</span>
                                <span style="width: 12px; text-align: center;">:</span>
                                <span class="flex-fill ps-2">
                                    <h:outputLabel value="#{cc.attrs.bill.patientEncounter.printingDischargeTime ne null and !cc.attrs.bill.patientEncounter.printingDischargeTime.equals('') ? cc.attrs.bill.patientEncounter.printingDischargeTime : cc.attrs.bill.patientEncounter.dateOfDischarge}">
                                        <f:convertDateTime pattern="#{sessionController.applicationPreference.longDateTimeFormat}" />
                                    </h:outputLabel>
                                </span>
                            </div>

                            <!-- Guardian -->
                            <h:panelGroup layout="block" styleClass="col-6 d-flex mb-1"
                                          rendered="#{configOptionApplicationController.getBooleanValueByKey('Print Guardian on Final Bill',true)}">
                                <span style="width: 130px;">Guardian</span>
                                <span style="width: 12px; text-align: center;">:</span>
                                <span class="flex-fill ps-2">
                                    <h:outputLabel value="#{cc.attrs.bill.patientEncounter.guardian.nameWithTitle}" />
                                </span>
                            </h:panelGroup>

                            <!-- Room -->
                            <div class="col-6 d-flex mb-1">
                                <span style="width: 130px;">Room</span>
                                <span style="width: 12px; text-align: center;">:</span>
                                <span class="flex-fill ps-2">
                                    <h:outputLabel value="#{cc.attrs.bill.patientEncounter.currentPatientRoom.roomFacilityCharge.name}" />
                                </span>
                            </div>

                            <!-- Credit Company -->
                            <h:panelGroup layout="block" styleClass="col-6 d-flex mb-1"
                                          rendered="#{cc.attrs.bill.creditCompany ne null and cc.attrs.bill.paymentMethod eq 'Credit'}">
                                <span style="width: 130px;">Credit Company</span>
                                <span style="width: 12px; text-align: center;">:</span>
                                <span class="flex-fill ps-2">
                                    <h:outputLabel value="#{cc.attrs.bill.creditCompany.name}" />
                                </span>
                            </h:panelGroup>

                        </div>
                    </div>

                    <div style="width: 90%; margin-left: 5%; margin-right: 5%">
                        <table class=" w-100"  >
                            <tr style="t">
                                <td style="text-align: left; font-weight: bold; font-size: 14px;">
                                    <h:outputLabel value="Description" />
                                </td>
                                <td>
                                    <h:outputLabel value="" />
                                </td>
                                <td style="text-align: right; font-weight: bold; font-size: 14px;">
                                    <h:outputLabel value="Charge (Rs.)" />
                                </td>
                            </tr>

                            <!-- Every non-Professional/DoctorAndNurses charge type, individually or
                                 summed into its configured group — see
                                 BhtSummeryController#getBundledFinalBillRows. -->
                            <ui:repeat value="#{bhtSummeryController.getBundledFinalBillRows(cc.attrs.bill)}" var="row">
                                <tr style="width: 100%; font-weight: 400; line-height: 18px;">
                                    <td style="text-align: left;font-size: 13px!important; margin-top: -10px;">
                                        <h:outputLabel value="#{row.label}" />
                                    </td>
                                    <td style="margin-top: -10px;">
                                        &nbsp;
                                    </td>
                                    <td  style="width: 30%;text-align: right;font-size: 13px!important;">
                                        <h:outputLabel  value="#{row.amount}">
                                            <f:convertNumber pattern="#,##0.00" />
                                        </h:outputLabel>
                                    </td>
                                </tr>
                            </ui:repeat>

                            <!-- Doctor and Nurse assisting charges: always shown individually with
                                 their per-staff fee breakdown, never folded into a group. -->
                            <ui:repeat value="#{cc.attrs.bill.billItems}" var="bip">
                                <h:panelGroup rendered="#{bip.inwardChargeType eq 'DoctorAndNurses' and bip.adjustedValue != 0}" style="font-size: 12px;">
                                    <tr style="width: 100%; font-weight: 400; line-height: 18px;">
                                        <td style="text-align: left; font-size: 13px!important; margin-top: -10px;">
                                            <h:outputLabel value="#{configOptionApplicationController.getInwardChargeTypeLabel(bip.inwardChargeType)}" />
                                        </td>
                                        <td style="margin-top: -10px;">
                                            <table>
                                                <ui:repeat value="#{bip.billFees}" var="fe" rendered="#{!configOptionApplicationController.getBooleanValueByKey('Hide Doctor and Nurse Charges Details in Final Bill', false)}">
                                                    <h:panelGroup>
                                                        <tr>
                                                            <td>
                                                                <h:outputLabel value="#{fe.referencePatientRoom.name}" style="text-align: center; font-size: 9px!important;" />
                                                            </td>
                                                        </tr>
                                                    </h:panelGroup>
                                                </ui:repeat>
                                            </table>
                                        </td>
                                        <td style="width: 30%; text-align: right; font-size: 13px!important;">
                                            <h:outputLabel value="#{bip.adjustedValue}">
                                                <f:convertNumber pattern="#,##0.00" />
                                            </h:outputLabel>
                                        </td>
                                    </tr>
                                </h:panelGroup>
                            </ui:repeat>

                            <!-- Professional Charge: same, per-staff breakdown, only when
                                 showProfessional is true (Hospital Copy without professional
                                 fees passes showProfessional=false). -->
                            <h:panelGroup rendered="#{cc.attrs.showProfessional}">
                                <ui:repeat value="#{cc.attrs.bill.billItems}" var="bip">
                                    <h:panelGroup rendered="#{bip.inwardChargeType eq 'ProfessionalCharge' and bip.adjustedValue != 0}" style="font-size: 12px;">
                                        <tr style="width: 100%; font-weight: 400; line-height: 18px;">
                                            <td style="text-align: left; font-size: 13px!important; margin-top: -10px;">
                                                <h:outputLabel value="#{configOptionApplicationController.getInwardChargeTypeLabel(bip.inwardChargeType)}" />
                                            </td>
                                            <td style="margin-top: -10px;">
                                                <table>
                                                    <ui:repeat value="#{bip.proFees}" var="fe">
                                                        <h:panelGroup rendered="#{fe.feeAdjusted ne 0 and fe.bill.cancelled eq false and fe.bill.billClass eq 'class com.divudi.core.entity.BilledBill'}">
                                                            <tr>
                                                                <td style="text-align: left; font-size: 10px!important;">
                                                                    <h:panelGroup>#{fe.staff.person.nameWithTitle}<h:outputText value=" (#{fe.staff.speciality.name})" rendered="#{fe.staff.speciality ne null}"/></h:panelGroup>
                                                                </td>
                                                                <td style="text-align: right; font-size: 10px!important; padding-left: 25px;">
                                                                    <h:outputLabel value="#{fe.feeAdjusted}">
                                                                        <f:convertNumber pattern="#,##0.00" />
                                                                    </h:outputLabel>
                                                                </td>
                                                            </tr>
                                                        </h:panelGroup>
                                                    </ui:repeat>
                                                </table>
                                            </td>
                                            <td style="width: 30%; text-align: right; font-size: 13px!important;">
                                                <h:outputLabel value="#{bip.adjustedValue}">
                                                    <f:convertNumber pattern="#,##0.00" />
                                                </h:outputLabel>
                                            </td>
                                        </tr>
                                    </h:panelGroup>
                                </ui:repeat>
                            </h:panelGroup>

                            <h:panelGroup>
                                <!--GRANT TOTAL-->
                                <tr style="margin-top: -10px;">
                                    <td>&nbsp;</td>
                                    <td>&nbsp;</td>
                                    <td style="text-align: right;"><h:outputLabel value="-------------------" /></td>
                                </tr>


                                <h:panelGroup >
                                    <tr style="width: 100%;">
                                        <td style="text-align: left; font-size: 13px!important;font-weight: bold!important;">
                                            <h:outputLabel value="Total"/>
                                        </td>
                                        <td>&nbsp;</td>
                                        <td style="text-align: right;font-size: 13px!important;font-weight: bold!important;">

                                            <h:outputLabel rendered="#{cc.attrs.showProfessional}" value="#{cc.attrs.bill.grantTotal}" >
                                                <f:convertNumber pattern="#,##0.00" />
                                            </h:outputLabel>

                                            <h:outputLabel rendered="#{!cc.attrs.showProfessional}" value="#{cc.attrs.bill.hospitalFee}" >
                                                <f:convertNumber pattern="#,##0.00" />
                                            </h:outputLabel>

                                        </td>
                                    </tr>
                                </h:panelGroup>

                            </h:panelGroup>

                            <!--DISCOUNT-->
                            <h:panelGroup rendered="#{cc.attrs.bill.discount ne 0.0}">
                                <tr>
                                    <td style="text-align: left;font-size: 13px!important;font-weight: bold!important;">
                                        <h:outputLabel value="Discount"/>
                                    </td>
                                    <td>&nbsp;</td>
                                    <td style="text-align: right;font-size: 13px!important;font-weight: bold!important;">
                                        <h:outputLabel  value="#{cc.attrs.bill.discount}" >
                                            <f:convertNumber pattern="#,##0.00" />
                                        </h:outputLabel>
                                    </td>
                                </tr>
                            </h:panelGroup>

                            <h:panelGroup >
                                <tr>
                                    <td>&nbsp;</td>
                                    <td>&nbsp;</td>
                                    <td style="text-align: right;"><h:outputLabel value="-------------------" /></td>
                                </tr>
                                <tr>
                                    <td style="text-align: left;font-size: 13px!important;font-weight: bold!important;">
                                        <h:outputLabel  value="Net Total"/>
                                    </td>
                                    <td>&nbsp;</td>
                                    <td style="text-align: right;font-size: 13px!important;font-weight: bold!important;">
                                        <h:outputLabel rendered="#{cc.attrs.showProfessional}" value="#{cc.attrs.bill.netTotal}" >
                                            <f:convertNumber pattern="#,##0.00" />
                                        </h:outputLabel>

                                        <h:outputLabel rendered="#{!cc.attrs.showProfessional}" value="#{cc.attrs.bill.hospitalFee-cc.attrs.bill.discount}" >
                                            <f:convertNumber pattern="#,##0.00" />
                                        </h:outputLabel>

                                    </td>
                                </tr>

                                <tr>
                                    <td>&nbsp;</td>
                                    <td>&nbsp;</td>
                                    <td style="text-align: right;"><h:outputLabel value="============"/></td>
                                </tr>
                            </h:panelGroup>

                            <h:panelGroup rendered="#{cc.attrs.showProfessional}">
                                <h:panelGroup
                                    rendered="#{cc.attrs.bill.paidAmount !=0 and cc.attrs.bill.settledAmountByPatient !=0 }">

                                    <tr>
                                        <td style="text-align: left;font-size: 13px!important;font-weight: bold!important;">
                                            <h:outputLabel value="Paid By Patient "
                                                           rendered="#{cc.attrs.bill.paidAmount !=0 and cc.attrs.bill.settledAmountByPatient !=0 }"/>
                                        </td>
                                        <td>
                                            <table>
                                                <ui:repeat value="#{cc.attrs.bill.patientEncounter.finalBill ne null ? cc.attrs.bill.patientEncounter.finalBill.backwardReferenceBills : cc.attrs.bill.backwardReferenceBills}" var="b">
                                                    <h:panelGroup rendered="#{(b.netTotal ne 0 )
                                                                              and
                                                                              ((b.cancelled eq false
                                                                              and b.billClass eq 'class com.divudi.core.entity.BilledBill')
                                                                              or
                                                                              (b.cancelled eq false
                                                                              and b.refundedBill eq null
                                                                              and b.billClass eq 'class com.divudi.core.entity.RefundBill')) and b.billTypeAtomic ne 'INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED'}">
                                                        <div class="d-flex gap-3" style="font-size: 11px;">
                                                            <h:outputLabel value="#{b.deptId}"/>
                                                            <h:outputLabel value="#{b.paymentMethod}" rendered="#{configOptionApplicationController.getBooleanValueByKey('Show Payment Method on Payments on Final Bill On Final Bill Print',false)}"/>
                                                            <h:outputLabel value="#{b.netTotal}"
                                                                           style="text-align: right;">
                                                                <f:convertNumber pattern="#,##0.00"/>
                                                            </h:outputLabel>
                                                            <h:outputLabel value="(#{b.comments})" rendered="#{configOptionApplicationController.getBooleanValueByKey('Show Comments of Payments on Final Bill On Final Bill Print',false) and b.comments ne null and b.comments ne ''}"/>

                                                        </div>

                                                    </h:panelGroup>
                                                </ui:repeat>
                                            </table>
                                        </td>
                                        <td style="text-align: right;font-size: 13px!important;font-weight: bold!important;">
                                            <h:outputLabel value="#{cc.attrs.bill.settledAmountByPatient}"
                                                           rendered="#{cc.attrs.bill.paidAmount !=0}">
                                                <f:convertNumber pattern="#,##0.00"/>
                                            </h:outputLabel>
                                        </td>
                                    </tr>

                                </h:panelGroup>

                                <h:panelGroup
                                    rendered="#{cc.attrs.bill.paidAmount !=0 and cc.attrs.bill.settledAmountBySponsor !=0 }">
                                    <tr>
                                        <td style="text-align: left;font-size: 13px!important;font-weight: bold!important;">
                                            <h:outputLabel value="Paid By Company "
                                                           rendered="#{cc.attrs.bill.paidAmount !=0 and cc.attrs.bill.settledAmountBySponsor !=0 }"/>
                                        </td>
                                        <td>
                                            <table>
                                                <ui:repeat value="#{cc.attrs.bill.patientEncounter.finalBill ne null ? cc.attrs.bill.patientEncounter.finalBill.backwardReferenceBills : cc.attrs.bill.backwardReferenceBills}" var="b">
                                                    <h:panelGroup rendered="#{(b.netTotal ne 0 )
                                                                              and
                                                                              ((b.cancelled eq false
                                                                              and b.billClass eq 'class com.divudi.core.entity.BilledBill')
                                                                              or
                                                                              (b.cancelled eq false
                                                                              and b.refundedBill eq null
                                                                              and b.billClass eq 'class com.divudi.core.entity.RefundBill')) and b.billTypeAtomic eq 'INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED'}">
                                                        <div class="d-flex gap-3" style="font-size: 11px;">
                                                            <h:outputLabel  value="#{b.deptId}"/>
                                                            <h:outputLabel value="#{b.netTotal}" style="width: 30px; text-align: right;">
                                                                <f:convertNumber pattern="#,##0.00" />
                                                            </h:outputLabel>
                                                        </div>

                                                    </h:panelGroup>
                                                </ui:repeat>
                                            </table>
                                        </td>
                                        <td style="text-align: right;font-size: 13px!important;font-weight: bold!important;">
                                            <h:outputLabel value="#{cc.attrs.bill.settledAmountBySponsor}"
                                                           rendered="#{cc.attrs.bill.paidAmount !=0 and cc.attrs.bill.settledAmountBySponsor !=0}">
                                                <f:convertNumber pattern="#,##0.00"/>
                                            </h:outputLabel>
                                        </td>
                                    </tr>

                                </h:panelGroup>

                                <h:panelGroup>
                                    <tr>
                                        <td>&nbsp;</td>
                                        <td>&nbsp;</td>
                                        <td style="text-align: right;"><h:outputLabel value="-------------------"/></td>
                                    </tr>

                                </h:panelGroup>

                                <h:panelGroup>
                                    <tr>
                                        <td style="text-align: left;font-size: 13px!important;font-weight: bold!important;">
                                            <h:outputLabel value="Total Paid "
                                                           rendered="#{cc.attrs.bill.paidAmount !=0}"/>
                                        </td>
                                        <td></td>
                                        <td style="text-align: right;font-size: 13px!important;font-weight: bold!important;">
                                            <h:outputLabel value="#{cc.attrs.bill.paidAmount}"
                                                           rendered="#{cc.attrs.bill.paidAmount !=0}">
                                                <f:convertNumber pattern="#,##0.00" />
                                            </h:outputLabel>
                                        </td>
                                    </tr>

                                </h:panelGroup>

                                <h:panelGroup >
                                    <tr>
                                        <td style="text-align: left;font-size: 13px!important;font-weight: bold!important;">
                                            <h:outputLabel   value="Due Amount" />
                                        </td>
                                        <td>
                                            &nbsp;
                                        </td>
                                        <td style="text-align: right;font-size: 13px!important;font-weight: bold!important;">
                                            <h:outputLabel value="#{cc.attrs.bill.netTotal-(cc.attrs.bill.paidAmount)}">
                                                <f:convertNumber pattern="#,##0.00" />
                                            </h:outputLabel>
                                        </td>
                                    </tr>


                                </h:panelGroup>
                            </h:panelGroup>

                        </table>
                    </div>

                    <p:spacer></p:spacer>

                    <div class="row mt-4" style="font-size: 12px;">
                        <div class="col-4">
                            <div class="d-grid justify-content-center">
                                <h:outputLabel value="-----------------------------" />
                                <h:outputLabel value="Patient/Guardian" class="w-100 d-flex justify-content-center"/>
                            </div>
                        </div>
                        <div class="col-4">
                            <div class="d-grid justify-content-center">
                                <h:outputLabel value="-----------------------------" />
                                <h:outputLabel value="Prepared by : #{cc.attrs.bill.creater.name}"  class="w-100 d-flex justify-content-center"/>
                            </div>
                        </div>
                        <div class="col-4">
                            <div class="d-grid justify-content-center">
                                <h:outputLabel value="-----------------------------" />
                                <h:outputLabel value="Cashier"  class="w-100 d-flex justify-content-center"/>
                            </div>
                        </div>
                    </div>

                </div>

            </div>
        </h:panelGroup>

    </cc:implementation>
</html>
```

- [ ] **Step 2: Compile check**

Run: `./detect-maven.sh compile -q`
Expected: no output, exit code 0 (this is a webapp resource, not compiled Java — this step just re-confirms Task 5's Java still compiles after the file-system add; the real check for this file happens in Task 9's Playwright pass).

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/resources/inward/bill/finalBillBundledCustom1.xhtml
git commit -m "feat(inward): add finalBillBundledCustom1 print composite

New opt-in Final Bill format driven by
bhtSummeryController.getBundledFinalBillRows(bill). Header/patient-info/
payment/signature blocks copied verbatim from finalBill.xhtml; only the
charge-lines table differs. Not wired into any page yet (Task 7).

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 7: Wire the opt-in toggle into the Custom Bills tab

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java` (new `showBundledCustom1Format` field, getter/setter, wired into `loadCustomBillFormatVisibility()` / `persistCustomBillFormatVisibility()`)
- Modify: `src/main/webapp/inward/inward_reprint_bill_final.xhtml` (new panel in the "Custom Bills" tab + new checkbox in the settings dialog)

**Interfaces:**
- Consumes: `bi:finalBillBundledCustom1` (Task 6); existing `configOptionController.getBooleanValueByKeyReadOnly/setBooleanValueByKey` pattern.
- Produces: `BhtSummeryController.isShowBundledCustom1Format()/setShowBundledCustom1Format(boolean)` — bound from the settings checkbox; `'Inward Final Bill - Show Bundled Custom 1 Format'` ConfigOption (default `false`) gates the new panel.

- [ ] **Step 1: Add the field + wire it into the existing load/save centralizers**

Find:

```java
    private boolean showCustomBill2Format;
    private boolean showCustomBill3Format;
    private boolean showCustomBill4Format;
```

Replace with:

```java
    private boolean showCustomBill2Format;
    private boolean showCustomBill3Format;
    private boolean showCustomBill4Format;
    private boolean showBundledCustom1Format;
```

Find:

```java
    private void loadCustomBillFormatVisibility() {
        showCustomBill2Format = configOptionController.getBooleanValueByKeyReadOnly("Inward Final Bill - Show Custom Bill 2 Format", true);
        showCustomBill3Format = configOptionController.getBooleanValueByKeyReadOnly("Inward Final Bill - Show Custom Bill 3 Format", false);
        showCustomBill4Format = configOptionController.getBooleanValueByKeyReadOnly("Inward Final Bill - Show Custom Bill 4 Format", false);
    }

    private void persistCustomBillFormatVisibility() {
        configOptionController.setBooleanValueByKey("Inward Final Bill - Show Custom Bill 2 Format", showCustomBill2Format);
        configOptionController.setBooleanValueByKey("Inward Final Bill - Show Custom Bill 3 Format", showCustomBill3Format);
        configOptionController.setBooleanValueByKey("Inward Final Bill - Show Custom Bill 4 Format", showCustomBill4Format);
    }
```

Replace with:

```java
    private void loadCustomBillFormatVisibility() {
        showCustomBill2Format = configOptionController.getBooleanValueByKeyReadOnly("Inward Final Bill - Show Custom Bill 2 Format", true);
        showCustomBill3Format = configOptionController.getBooleanValueByKeyReadOnly("Inward Final Bill - Show Custom Bill 3 Format", false);
        showCustomBill4Format = configOptionController.getBooleanValueByKeyReadOnly("Inward Final Bill - Show Custom Bill 4 Format", false);
        showBundledCustom1Format = configOptionController.getBooleanValueByKeyReadOnly("Inward Final Bill - Show Bundled Custom 1 Format", false);
    }

    private void persistCustomBillFormatVisibility() {
        configOptionController.setBooleanValueByKey("Inward Final Bill - Show Custom Bill 2 Format", showCustomBill2Format);
        configOptionController.setBooleanValueByKey("Inward Final Bill - Show Custom Bill 3 Format", showCustomBill3Format);
        configOptionController.setBooleanValueByKey("Inward Final Bill - Show Custom Bill 4 Format", showCustomBill4Format);
        configOptionController.setBooleanValueByKey("Inward Final Bill - Show Bundled Custom 1 Format", showBundledCustom1Format);
    }
```

Find:

```java
    public boolean isShowCustomBill4Format() {
        return showCustomBill4Format;
    }

    public void setShowCustomBill4Format(boolean showCustomBill4Format) {
        this.showCustomBill4Format = showCustomBill4Format;
    }
    // </editor-fold>
```

Replace with:

```java
    public boolean isShowCustomBill4Format() {
        return showCustomBill4Format;
    }

    public void setShowCustomBill4Format(boolean showCustomBill4Format) {
        this.showCustomBill4Format = showCustomBill4Format;
    }

    public boolean isShowBundledCustom1Format() {
        return showBundledCustom1Format;
    }

    public void setShowBundledCustom1Format(boolean showBundledCustom1Format) {
        this.showBundledCustom1Format = showBundledCustom1Format;
    }
    // </editor-fold>
```

- [ ] **Step 2: Compile check**

Run: `./detect-maven.sh compile -q`
Expected: no output, exit code 0.

- [ ] **Step 3: Add the new panel to the Custom Bills tab**

Open `src/main/webapp/inward/inward_reprint_bill_final.xhtml`. Find this exact text (the closing of the Custom Bill 4 panel, immediately followed by the settings dialog):

```xhtml
                                                <h:panelGroup id="duplicateCustom4BillPriview">
                                                    <bi:finalBillCustom4 bill="#{inwardSearch.showOrginalBill ? inwardSearch.bill.referenceBill : inwardSearch.bill}" duplicate="true" showProfessional="#{inwardSearch.withProfessionalFee}"/>
                                                </h:panelGroup>

                                            </p:panel>
                                        </div>
                                    </div>
                                    </h:panelGroup>

                                    <p:dialog id="custom2ConfigDialog"
```

Replace with:

```xhtml
                                                <h:panelGroup id="duplicateCustom4BillPriview">
                                                    <bi:finalBillCustom4 bill="#{inwardSearch.showOrginalBill ? inwardSearch.bill.referenceBill : inwardSearch.bill}" duplicate="true" showProfessional="#{inwardSearch.withProfessionalFee}"/>
                                                </h:panelGroup>

                                            </p:panel>
                                        </div>
                                    </div>
                                    </h:panelGroup>

                                    <h:panelGroup layout="block" rendered="#{configOptionController.getBooleanValueByKeyReadOnly('Inward Final Bill - Show Bundled Custom 1 Format', false)}">
                                    <div class="row mt-3">
                                        <div class="col-lg-6">
                                            <p:panel>
                                                <f:facet name="header">
                                                    <div class="d-flex justify-content-between">
                                                        <div><h:outputLabel value="Original" class="mt-2"/></div>
                                                        <div>
                                                            <p:commandButton
                                                                icon="fa fa-print"
                                                                value="Bundled Custom 1"
                                                                class="ui-button-info"
                                                                ajax="false">
                                                                <p:printer target="originalBundledCustom1BillPriview" ></p:printer>
                                                            </p:commandButton>
                                                        </div>
                                                    </div>
                                                </f:facet>

                                                <h:panelGroup id="originalBundledCustom1BillPriview">
                                                    <bi:finalBillBundledCustom1 bill="#{inwardSearch.showOrginalBill ? inwardSearch.bill.referenceBill : inwardSearch.bill}" duplicate="false" showProfessional="#{inwardSearch.withProfessionalFee}"/>
                                                </h:panelGroup>

                                            </p:panel>
                                        </div>
                                        <div class="col-lg-6">
                                            <p:panel>
                                                <f:facet name="header">
                                                    <div class="d-flex justify-content-between">
                                                        <div><h:outputLabel value="Duplicate" class="mt-2"/></div>
                                                        <div>
                                                            <p:commandButton
                                                                icon="fa fa-print"
                                                                value="Bundled Custom 1"
                                                                class="ui-button-info"
                                                                ajax="false">
                                                                <p:printer target="duplicateBundledCustom1BillPriview" ></p:printer>
                                                            </p:commandButton>
                                                        </div>
                                                    </div>
                                                </f:facet>

                                                <h:panelGroup id="duplicateBundledCustom1BillPriview">
                                                    <bi:finalBillBundledCustom1 bill="#{inwardSearch.showOrginalBill ? inwardSearch.bill.referenceBill : inwardSearch.bill}" duplicate="true" showProfessional="#{inwardSearch.withProfessionalFee}"/>
                                                </h:panelGroup>

                                            </p:panel>
                                        </div>
                                    </div>
                                    </h:panelGroup>

                                    <p:dialog id="custom2ConfigDialog"
```

- [ ] **Step 4: Add the settings checkbox**

Find:

```xhtml
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="showCustomBill4Format" value="#{bhtSummeryController.showCustomBill4Format}" />
                                                    <h:outputLabel for="showCustomBill4Format" value="Show &quot;Custom Bill 3 (Letterhead)&quot; format" class="ms-2" />
                                                </div>
                                                <hr/>
```

Replace with:

```xhtml
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="showCustomBill4Format" value="#{bhtSummeryController.showCustomBill4Format}" />
                                                    <h:outputLabel for="showCustomBill4Format" value="Show &quot;Custom Bill 3 (Letterhead)&quot; format" class="ms-2" />
                                                </div>
                                                <div class="mb-3">
                                                    <h:selectBooleanCheckbox id="showBundledCustom1Format" value="#{bhtSummeryController.showBundledCustom1Format}" />
                                                    <h:outputLabel for="showBundledCustom1Format" value="Show &quot;Bundled Custom 1&quot; format" class="ms-2" />
                                                </div>
                                                <hr/>
```

- [ ] **Step 5: Compile check**

Run: `./detect-maven.sh compile -q`
Expected: no output, exit code 0.

(Full render correctness of this XHTML wiring is verified in Task 9's Playwright pass — per this project's convention, JSF errors don't surface at `mvn compile` time.)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/BhtSummeryController.java src/main/webapp/inward/inward_reprint_bill_final.xhtml
git commit -m "feat(inward): wire Bundled Custom 1 into the Custom Bills tab

Gated by 'Inward Final Bill - Show Bundled Custom 1 Format'
(default false), following the exact existing convention for
Custom Bill 2/3/4 — a settings-dialog checkbox toggles visibility,
no hospital sees this panel until an admin opts in.

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 8: Remove the dead "Group Bed Charges in Bills" code

**Files:**
- Modify: `src/main/webapp/resources/inward/bill/finalBill.xhtml`
- Modify: `src/main/webapp/resources/inward/bill/finalBillCustom4.xhtml`
- Modify: `src/main/webapp/resources/inward/bill/intrimBill.xhtml`

**Interfaces:** none — pure deletion of unreachable, broken markup. `bhtSummeryController.getBedChargesTotal(...)` does not exist anywhere in `BhtSummeryController`; the config defaults to `false` everywhere, so this branch has never executed in production. Superseded by the general mechanism (Tasks 1-7).

- [ ] **Step 1: Clean up `finalBill.xhtml`**

Find:

```xhtml
                                <!-- Bed Charges - Display individually if option is off -->
                                <h:panelGroup rendered="#{!configOptionApplicationController.getBooleanValueByKey('Group Bed Charges in Bills', false)}">
```

Replace with:

```xhtml
                                <!-- Bed Charges - always displayed individually (the broken
                                     'Group Bed Charges in Bills' mechanism that used to gate this
                                     was removed — it called a nonexistent method and was never
                                     actually usable; see finalBillBundledCustom1.xhtml for the
                                     real, configurable replacement). -->
                                <h:panelGroup>
```

Then find (the entire broken grouped block, from its comment through its closing `</h:panelGroup>` pair):

```xhtml
                                <!-- Grouped Bed Charges - Display as single line if option is on -->
                                <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Group Bed Charges in Bills', false)}">
                                    <h:panelGroup rendered="#{bhtSummeryController.getBedChargesTotal(cc.attrs.bill.billItems) > 0}" style="font-size: 12px;">
                                        <tr style="width: 100%; font-weight: 400; line-height: 18px;">
                                            <td style="text-align: left; font-size: 13px!important; margin-top: -10px;">
                                                <h:outputLabel value="#{configOptionApplicationController.getLongTextValueByKey('Bed Charges Label', 'Bed Charges')}" />
                                            </td>
                                            <td style="margin-top: -10px;">
                                            </td>
                                            <td style="width: 30%; text-align: right; font-size: 13px!important;">
                                                <h:outputLabel value="#{bhtSummeryController.getBedChargesTotal(cc.attrs.bill.billItems)}">
                                                    <f:convertNumber pattern="#,##0.00" />
                                                </h:outputLabel>
                                            </td>
                                        </tr>
                                    </h:panelGroup>
                                </h:panelGroup>

                                <!-- Loop through all other items excluding RoomCharges and ProfessionalCharge -->
```

Replace with:

```xhtml
                                <!-- Loop through all other items excluding RoomCharges and ProfessionalCharge -->
```

- [ ] **Step 2: Clean up `finalBillCustom4.xhtml`** (identical broken block)

Find:

```xhtml
                                <!-- Bed Charges - Display individually if option is off -->
                                <h:panelGroup rendered="#{!configOptionApplicationController.getBooleanValueByKey('Group Bed Charges in Bills', false)}">
```

Replace with:

```xhtml
                                <!-- Bed Charges - always displayed individually (the broken
                                     'Group Bed Charges in Bills' mechanism that used to gate this
                                     was removed — it called a nonexistent method and was never
                                     actually usable; see finalBillBundledCustom1.xhtml for the
                                     real, configurable replacement). -->
                                <h:panelGroup>
```

Find:

```xhtml
                                <!-- Grouped Bed Charges - Display as single line if option is on -->
                                <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Group Bed Charges in Bills', false)}">
                                    <h:panelGroup rendered="#{bhtSummeryController.getBedChargesTotal(cc.attrs.bill.billItems) > 0}" style="font-size: 12px;">
                                        <tr style="width: 100%; font-weight: 400; line-height: 18px;">
                                            <td style="text-align: left; font-size: 13px!important; margin-top: -10px;">
                                                <h:outputLabel value="#{configOptionApplicationController.getLongTextValueByKey('Bed Charges Label', 'Bed Charges')}" />
                                            </td>
                                            <td style="margin-top: -10px;">
                                            </td>
                                            <td style="width: 30%; text-align: right; font-size: 13px!important;">
                                                <h:outputLabel value="#{bhtSummeryController.getBedChargesTotal(cc.attrs.bill.billItems)}">
                                                    <f:convertNumber pattern="#,##0.00" />
                                                </h:outputLabel>
                                            </td>
                                        </tr>
                                    </h:panelGroup>
                                </h:panelGroup>

                                <!-- Loop through all other items excluding RoomCharges and ProfessionalCharge -->
```

Replace with:

```xhtml
                                <!-- Loop through all other items excluding RoomCharges and ProfessionalCharge -->
```

- [ ] **Step 3: Clean up `intrimBill.xhtml`** (different, simpler structure — see spec's "Existing debt" note re: the `#22989` named-item exception, which only ever mattered inside this now-removed dead branch)

Find:

```xhtml
                        <table class="tbl"  >
                            <h:panelGroup rendered="#{!configOptionApplicationController.getBooleanValueByKey('Group Bed Charges in Bills', false)}">
                                <ui:repeat value="#{cc.attrs.bill.billItems}" var="bip">
                                    <h:panelGroup rendered="#{bip.adjustedValue !=0}">
                                        <tr>
                                            <td style="text-align: left;" >
                                                <h:outputLabel value="#{not empty bip.descreption ? bip.descreption : bip.inwardChargeType.name}" />
                                            </td>
                                            <td style="text-align: right;" >
                                                <h:outputLabel value="#{bip.adjustedValue}">
                                                    <f:convertNumber pattern="#,##0.00" />
                                                </h:outputLabel>  
                                            </td>
                                        </tr>
                                    </h:panelGroup>
                                </ui:repeat>
                            </h:panelGroup>

                            <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Group Bed Charges in Bills', false)}">
                                <ui:repeat value="#{cc.attrs.bill.billItems}" var="bip">
                                    <!-- A named Outside Charge item row (bip.descreption set) is always
                                         shown on its own line regardless of charge type, even when that
                                         type is one of the bed-charge categories folded into the grouped
                                         row below — otherwise the item would silently disappear (issue
                                         #22989) while its value still counts toward the grouped total. -->
                                    <h:panelGroup rendered="#{bip.adjustedValue != 0 and (not empty bip.descreption or (bip.inwardChargeType ne 'LinenCharges' and bip.inwardChargeType ne 'MOCharges' and bip.inwardChargeType ne 'NursingCharges' and bip.inwardChargeType ne 'MaintainCharges' and bip.inwardChargeType ne 'MedicalCareICU' and bip.inwardChargeType ne 'AdministrationCharge'))}">
                                        <tr>
                                            <td style="text-align: left;" >
                                                <h:outputLabel value="#{not empty bip.descreption ? bip.descreption : bip.inwardChargeType.name}" />
                                            </td>
                                            <td style="text-align: right;" >
                                                <h:outputLabel value="#{bip.adjustedValue}">
                                                    <f:convertNumber pattern="#,##0.00" />
                                                </h:outputLabel>  
                                            </td>
                                        </tr>
                                    </h:panelGroup>
                                </ui:repeat>

                                <!-- Grouped Bed Charges -->
                                <h:panelGroup rendered="#{bhtSummeryController.getBedChargesTotal(cc.attrs.bill.billItems) > 0}">
                                    <tr>
                                        <td style="text-align: left;" >
                                            <h:outputLabel value="#{configOptionApplicationController.getLongTextValueByKey('Bed Charges Label', 'Bed Charges')}" />
                                        </td>                                   
                                        <td style="text-align: right;" >
                                            <h:outputLabel value="#{bhtSummeryController.getBedChargesTotal(cc.attrs.bill.billItems)}">
                                                <f:convertNumber pattern="#,##0.00" />
                                            </h:outputLabel>
                                        </td>
                                    </tr>
                                </h:panelGroup>
                            </h:panelGroup>
```

Replace with:

```xhtml
                        <table class="tbl"  >
                            <!-- The broken 'Group Bed Charges in Bills' alternate branch that used
                                 to sit here (calling a nonexistent
                                 bhtSummeryController.getBedChargesTotal(...)) has been removed —
                                 it was unreachable (config defaults false) and never actually
                                 worked. This is the Interim Bill; charge-type grouping was not
                                 requested here, only the dead code. -->
                            <h:panelGroup>
                                <ui:repeat value="#{cc.attrs.bill.billItems}" var="bip">
                                    <h:panelGroup rendered="#{bip.adjustedValue !=0}">
                                        <tr>
                                            <td style="text-align: left;" >
                                                <h:outputLabel value="#{not empty bip.descreption ? bip.descreption : bip.inwardChargeType.name}" />
                                            </td>
                                            <td style="text-align: right;" >
                                                <h:outputLabel value="#{bip.adjustedValue}">
                                                    <f:convertNumber pattern="#,##0.00" />
                                                </h:outputLabel>  
                                            </td>
                                        </tr>
                                    </h:panelGroup>
                                </ui:repeat>
                            </h:panelGroup>
```

- [ ] **Step 4: Compile check**

Run: `./detect-maven.sh compile -q`
Expected: no output, exit code 0 (these are `.xhtml` resources; nothing here changes Java compilation — this step just confirms nothing else broke).

- [ ] **Step 5: Commit**

```bash
git add src/main/webapp/resources/inward/bill/finalBill.xhtml src/main/webapp/resources/inward/bill/finalBillCustom4.xhtml src/main/webapp/resources/inward/bill/intrimBill.xhtml
git commit -m "fix(inward): remove dead 'Group Bed Charges in Bills' branches

Calls bhtSummeryController.getBedChargesTotal(...), a method that does
not exist anywhere in BhtSummeryController. The config defaults to
false everywhere, so this branch has never actually executed in
production — turning it on would have thrown a JSF
PropertyNotFoundException. Superseded by the general, working
Final Bill Group mechanism (finalBillBundledCustom1.xhtml).

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

### Task 9: Build, deploy, and verify end-to-end

**Files:** none (verification only)

- [ ] **Step 1: Full build**

Run: `./detect-maven.sh clean package -q -DskipTests=false`
Expected: `BUILD SUCCESS`, all unit tests (including the new ones from Tasks 1-2) pass, WAR produced.

- [ ] **Step 2: Load the `run` / `playwright-e2e` skill and redeploy locally**

Use the project's established local redeploy process (`developer_docs/testing/playwright-e2e-workflow.md`) to get the new WAR onto the local Payara domain `rh`.

- [ ] **Step 3: Configure a test department for the new format via the API**

Using an existing valid Config API key (see `developer_docs/api/using-apis/`), for two or three real `InwardChargeType` values used by an existing test BHT (e.g. `RoomCharges`, `MealCharges`, and one more the test data actually has charges under):

```bash
curl -s -X PUT http://localhost:9080/rh/api/config/Inward%20Charge%20Type%20Final%20Bill%20Group%20-%20RoomCharges \
  -H "Config: <api-key>" -H "Content-Type: application/json" \
  -d '{"value":"Room Charges"}'

curl -s -X PUT http://localhost:9080/rh/api/config/Inward%20Charge%20Type%20Final%20Bill%20Group%20-%20MealCharges \
  -H "Config: <api-key>" -H "Content-Type: application/json" \
  -d '{"value":"Room Charges"}'

curl -s -X POST http://localhost:9080/rh/api/config/setBoolean/Inward%20Final%20Bill%20-%20Show%20Bundled%20Custom%201%20Format/true \
  -H "Config: <api-key>"
```

(The `PUT /api/config/{key}` endpoint requires the key to already exist — if it 404s, first open `inward/inward_charge_type_labels.xhtml` once in the browser, or call `GET /api/config/inward-charge-types` which seeds every row, then retry.)

- [ ] **Step 4: Playwright — verify the new format**

Following `playwright-e2e` conventions (login, select department first per `feedback_playwright_select_department`): navigate to a discharged test BHT's Inward Final Bill reprint page, open the "Custom Bills" tab, confirm a "Bundled Custom 1" panel now appears (it did not before Step 3), and its "Room Charges" line shows the correct sum of that BHT's actual Room + Meal charges (cross-check the numbers against the BHT's Interim/Final bill breakdown or the DB directly).

- [ ] **Step 5: Confirm the Inward Ledger Report is unaffected**

For the same test BHT, open the Inward Ledger / Charge Type Breakdown report and confirm Room Charges and Meal Charges still appear as two separate rows with their original individual values — proving the grouping config has zero effect on that reporting path.

- [ ] **Step 6: Confirm default (off) behavior for a different department**

Repeat Step 4 for a BHT in a department that never had the new config set — confirm the Custom Bills tab looks exactly as it did before this branch (no "Bundled Custom 1" panel), and that `finalBill.xhtml`/`finalBillCustom4.xhtml`/`intrimBill.xhtml` render identically to their pre-Task-8 output (the dead-code removal changed nothing observable).

- [ ] **Step 7: Record any new testing gotchas**

If anything unexpected turns up (e.g. a `p:printer target` id collision, an EL resolution surprise), add it to `developer_docs/testing/playwright-e2e-workflow.md` as a new numbered gotcha, per existing convention (see `§86` from #23340).

- [ ] **Step 8: Restore local JNDI and final commit (if any docs were updated in Step 7)**

Per `CLAUDE.md`, after any `git push` in this session, restore `persistence.xml`'s local JNDI names (leave unstaged). If Step 7 added a testing-workflow note:

```bash
git add developer_docs/testing/playwright-e2e-workflow.md
git commit -m "docs(testing): record gotcha found verifying Bundled Custom 1 final bill

Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>"
```

---

## Self-Review

**Spec coverage:**
- Section 1 (config data model) → Task 3, Task 4.
- Section 2 (rendering mechanism) → Task 1, Task 2, Task 5, Task 6.
- Section 3 (wiring + cleanup) → Task 7, Task 8.
- Testing section of the spec → Task 9 (Playwright/DB) plus the JUnit coverage in Tasks 1-2.
- Non-goals (ledger untouched, no schema change, no new privilege) → verified explicitly in Task 9 Step 5, and no task in this plan touches `InwardChargeTypeBreakdownController`, `InwardInvoiceJournalController`, `inward_bill_final_break_down.xhtml`, privileges, or `persistence.xml`/DDL.

**Placeholder scan:** no TBD/TODO; every step has complete, copy-pasteable code or an exact find/replace pair with real surrounding context lines taken directly from the current file contents on this branch.

**Type consistency:** `FinalBillPrintRowDTO(String, double, int)` (Task 1) matches every call site in Task 2 and every `#{row.label}/#{row.amount}` EL reference in Task 6. `buildBundledRows(List<BillItem>, Map<InwardChargeType,String>, Map<InwardChargeType,Integer>, Map<InwardChargeType,String>)` (Task 2) matches the call in `getBundledFinalBillRows` (Task 5) exactly (group, order, label maps in that order). `getBundledFinalBillRows(Bill)` (Task 5) matches the EL call `#{bhtSummeryController.getBundledFinalBillRows(cc.attrs.bill)}` (Task 6).
