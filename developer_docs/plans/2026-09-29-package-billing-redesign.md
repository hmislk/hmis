# Inpatient Package Billing Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the `fromPackage` billing-lock mechanism entirely (admissions under a package bill exactly like normal admissions during the stay), and make the final bill compare real usage against the package's price — showing the package's allocated value per covered category, bucketing any excess under a new `PackageExcessCharges` charge type, and letting staff use the existing charge-type discount when real usage came in under the package price.

**Architecture:** Deleting a `boolean fromPackage` field (and one `sourcePackageItem` reference is *kept*, non-blocking, for traceability) from `BillItem`/`BillFee`/`PatientRoom`, and every guard that reads it. A separate, unrelated "package rate override" mechanism in the pharmacy-issue controllers (which silently substitutes a package-negotiated per-unit rate when dispensing a medicine, and blocks substitution) is also removed for the same reason — it is a second, independently-discovered form of "locking" pharmacy issues to a frozen package rate. In its place, `BhtSummeryController.createChargeItemTotals()` — the method that already builds the final bill's per-charge-type totals from scratch every time — gets one new branch that runs only when the admission has a package.

**Tech Stack:** Java EE (CDI/EJB beans, JSF/PrimeFaces), JPA/EclipseLink, JUnit 5.

**Spec:** `developer_docs/specs/2026-09-29-package-billing-redesign-design.md`

## Global Constraints

- **Non-package admissions must be byte-for-byte unaffected.** Every new branch in this plan is gated on `admission.getInpatientPackage() != null`; nothing changes for a `null` package.
- Branch from `origin/development`, target `development` in the PR (CLAUDE.md).
- JSF-only file changes (`.xhtml`) do not require a Maven build/compile step; Java changes do.
- `AuditService.logEncounterAudit(...)` already wraps its body in try/catch — no new audit code needed here (nothing in this plan changes the audit trail).
- No new persisted entity *fields* beyond the two deletions already covered — the only schema change in this plan is `PackageExcessCharges` becoming a valid `InwardChargeType` value on already-existing `BillItem.inwardChargeType` columns (a plain enum, no DDL).
- Never navigate to a page by typing its URL during manual/Playwright verification — reach every page through the menus (CLAUDE.md).

---

## File Structure

| File | Responsibility |
|---|---|
| `src/main/java/com/divudi/core/data/inward/InwardChargeType.java` | Add `PackageExcessCharges` constant |
| `src/main/java/com/divudi/core/util/InpatientPackagePricing.java` | Add two pure static methods: per-category allocation map, excess calculation |
| `src/test/java/com/divudi/core/util/InpatientPackagePricingTest.java` | Unit tests for the two new methods |
| `src/main/java/com/divudi/ejb/InpatientPackageApplicationBean.java` | Remove room locking; drop `fromPackage=true` from component creation; pre-set `professionalFeeCategory` |
| `src/main/java/com/divudi/core/entity/BillItem.java`, `BillFee.java`, `src/main/java/com/divudi/core/entity/inward/PatientRoom.java` | Remove `fromPackage` field/accessors |
| `src/main/java/com/divudi/bean/inward/BhtSummeryController.java` | Remove guards + room-variance helpers; add the new package branch in `createChargeItemTotals()` |
| `src/main/java/com/divudi/bean/inward/InwardBeanController.java` | Remove `fromPackage=false` JPQL filters |
| `src/main/java/com/divudi/bean/inward/InwardTimedItemController.java`, `src/main/webapp/inward/inward_timed_service_consume.xhtml` | Remove guards + matching client-side bindings |
| `src/main/java/com/divudi/bean/inward/InwardProfessionalBillController.java` | Remove guards; adjust staff-assignment guard to use `sourcePackageItem` |
| `src/main/java/com/divudi/bean/inward/SurgeryChargeSummaryController.java`, `src/main/webapp/theater/patient_surgery.xhtml` | Remove package-locked room-charge branching |
| `src/main/java/com/divudi/bean/pharmacy/InpatientDirectIssueNativeSqlController.java`, `src/main/java/com/divudi/core/data/dto/BillItemData.java`, `src/main/java/com/divudi/service/pharmacy/InpatientDirectIssueNativeSqlService.java` | Remove the package-rate-override mechanism (native-SQL issue path) |
| `src/main/java/com/divudi/bean/pharmacy/PharmacyRequestForBhtController.java`, `src/main/java/com/divudi/bean/pharmacy/PharmacySaleBhtController.java` | Remove the package-rate-override mechanism (JPA issue paths) |

---

### Task 1: New `PackageExcessCharges` enum constant + pure package-math utilities

**Files:**
- Modify: `src/main/java/com/divudi/core/data/inward/InwardChargeType.java`
- Modify: `src/main/java/com/divudi/core/util/InpatientPackagePricing.java`
- Modify: `src/test/java/com/divudi/core/util/InpatientPackagePricingTest.java`

**Interfaces:**
- Produces: `InwardChargeType.PackageExcessCharges` (enum constant, `allowToSetItems=false`); `InpatientPackagePricing.calculateChargeTypeAllocations(Map<String, Double> chargeTypeAmounts, Map<InwardChargeType, Double> componentAllocations)` returning `Map<InwardChargeType, Double>`; `InpatientPackagePricing.calculatePackageExcess(Map<InwardChargeType, Double> perCategoryAllocations, Map<InwardChargeType, Double> actualTotalsByType, double packageTotal)` returning `double`. Both consumed by Task 5.

- [ ] **Step 1: Add the enum constant**

In `src/main/java/com/divudi/core/data/inward/InwardChargeType.java`, find the existing deprecated constant:
```java
    @Deprecated
    PackageFee("PackageFee", true),
```
(if this exact line is not found, search for `PackageFee` to locate the right spot — add the new constant near it since they're topically related, but do **not** touch the deprecated `PackageFee` itself). Add immediately after it:
```java
    PackageFee("PackageFee", true),

    /**
     * Computed at final-bill time only: the amount a package admission's real usage exceeds the package's
     * total price. Never assigned to an Item, never entered by an admin —
     * allowToSetItems=false keeps it out of both the Inpatient Package
     * "Charge Type Amounts" admin screen and the automatic
     * getInwardChargeTypesForSetting() seeding loop that final-bill totals
     * are built from; BhtSummeryController.applyPackagePricingIfApplicable()
     * appends it to chargeItemTotals directly when needed, the same way the
     * existing CancelledReturnedMedicine value is computed outside that loop.
     */
    PackageExcessCharges("Package Excess Charges", false),
```

- [ ] **Step 2: Write the failing tests for the two new pure methods**

Add to `src/test/java/com/divudi/core/util/InpatientPackagePricingTest.java` (same file/class as the existing tests — do not create a new file):
```java
    @Test
    void chargeTypeAllocationsSumsMapAndComponentsPerCategory() {
        Map<String, Double> chargeTypeAmounts = new LinkedHashMap<>();
        chargeTypeAmounts.put("RoomCharges", 30000.0);
        chargeTypeAmounts.put("NursingCharges", 15000.0);

        Map<InwardChargeType, Double> componentAllocations = new LinkedHashMap<>();
        componentAllocations.put(InwardChargeType.RoomCharges, 5000.0); // same category as chargeTypeAmounts
        componentAllocations.put(InwardChargeType.ProfessionalCharge, 8000.0); // new category

        Map<InwardChargeType, Double> result = InpatientPackagePricing.calculateChargeTypeAllocations(chargeTypeAmounts, componentAllocations);

        assertEquals(35000.0, result.get(InwardChargeType.RoomCharges), 0.001);
        assertEquals(15000.0, result.get(InwardChargeType.NursingCharges), 0.001);
        assertEquals(8000.0, result.get(InwardChargeType.ProfessionalCharge), 0.001);
    }

    @Test
    void chargeTypeAllocationsSkipsUnknownKeysAndNulls() {
        Map<String, Double> chargeTypeAmounts = new LinkedHashMap<>();
        chargeTypeAmounts.put("RoomCharges", 30000.0);
        chargeTypeAmounts.put("NotARealEnumName", 999.0);
        chargeTypeAmounts.put("NursingCharges", null);

        Map<InwardChargeType, Double> result = InpatientPackagePricing.calculateChargeTypeAllocations(chargeTypeAmounts, null);

        assertEquals(1, result.size());
        assertEquals(30000.0, result.get(InwardChargeType.RoomCharges), 0.001);
    }

    @Test
    void chargeTypeAllocationsHandlesBothNullInputs() {
        Map<InwardChargeType, Double> result = InpatientPackagePricing.calculateChargeTypeAllocations(null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void packageExcessIsZeroWhenActualWithinPackageTotal() {
        Map<InwardChargeType, Double> allocations = new LinkedHashMap<>();
        allocations.put(InwardChargeType.RoomCharges, 30000.0);
        allocations.put(InwardChargeType.NursingCharges, 15000.0);

        Map<InwardChargeType, Double> actual = new LinkedHashMap<>();
        actual.put(InwardChargeType.RoomCharges, 20000.0);
        actual.put(InwardChargeType.NursingCharges, 10000.0);

        double excess = InpatientPackagePricing.calculatePackageExcess(allocations, actual, 45000.0);

        assertEquals(0.0, excess, 0.001);
    }

    @Test
    void packageExcessIsWholePackageNetOverage() {
        Map<InwardChargeType, Double> allocations = new LinkedHashMap<>();
        allocations.put(InwardChargeType.RoomCharges, 30000.0);
        allocations.put(InwardChargeType.NursingCharges, 15000.0);

        Map<InwardChargeType, Double> actual = new LinkedHashMap<>();
        actual.put(InwardChargeType.RoomCharges, 40000.0); // 10,000 over its own allocation
        actual.put(InwardChargeType.NursingCharges, 5000.0); // 10,000 under its own allocation

        double excess = InpatientPackagePricing.calculatePackageExcess(allocations, actual, 45000.0);

        // Whole-package netting: 40000+5000=45000 actual vs 45000 package total -> no excess,
        // even though Room Charges alone overspent by 10,000.
        assertEquals(0.0, excess, 0.001);
    }

    @Test
    void packageExcessNetsAcrossCategoriesBeforeReportingOverage() {
        Map<InwardChargeType, Double> allocations = new LinkedHashMap<>();
        allocations.put(InwardChargeType.RoomCharges, 30000.0);
        allocations.put(InwardChargeType.NursingCharges, 15000.0);

        Map<InwardChargeType, Double> actual = new LinkedHashMap<>();
        actual.put(InwardChargeType.RoomCharges, 50000.0);
        actual.put(InwardChargeType.NursingCharges, 5000.0);

        double excess = InpatientPackagePricing.calculatePackageExcess(allocations, actual, 45000.0);

        // 50000+5000=55000 actual vs 45000 package total -> 10,000 net excess.
        assertEquals(10000.0, excess, 0.001);
    }

    @Test
    void packageExcessIgnoresActualCategoriesThePackageDoesNotCover() {
        Map<InwardChargeType, Double> allocations = new LinkedHashMap<>();
        allocations.put(InwardChargeType.RoomCharges, 30000.0);

        Map<InwardChargeType, Double> actual = new LinkedHashMap<>();
        actual.put(InwardChargeType.RoomCharges, 30000.0);
        actual.put(InwardChargeType.Medicine, 999999.0); // not package-covered, must not count

        double excess = InpatientPackagePricing.calculatePackageExcess(allocations, actual, 30000.0);

        assertEquals(0.0, excess, 0.001);
    }
```
Add these imports to the test file if not already present: `import com.divudi.core.data.inward.InwardChargeType;`, `import static org.junit.jupiter.api.Assertions.assertTrue;`. (`LinkedHashMap`, `Map` are already imported per the existing file.)

- [ ] **Step 2: Run the tests to verify they fail**

Run: `mvn -q test -Dtest=InpatientPackagePricingTest`
Expected: compilation failure — `calculateChargeTypeAllocations`/`calculatePackageExcess` do not exist yet.

- [ ] **Step 3: Implement the two methods**

In `src/main/java/com/divudi/core/util/InpatientPackagePricing.java`, add the import and the two methods:
```java
import com.divudi.core.data.inward.InwardChargeType;
import java.util.HashMap;
```
```java
    /**
     * The package's price broken down per InwardChargeType: chargeTypeAmounts'
     * entries (converted from their String enum-name keys) plus each
     * component's contribution under its own resolved charge type, summed
     * together when a category appears in both. Unknown/unparseable
     * chargeTypeAmounts keys and null values are skipped rather than thrown.
     */
    public static Map<InwardChargeType, Double> calculateChargeTypeAllocations(
            Map<String, Double> chargeTypeAmounts, Map<InwardChargeType, Double> componentAllocations) {
        Map<InwardChargeType, Double> result = new HashMap<>();
        if (chargeTypeAmounts != null) {
            for (Map.Entry<String, Double> e : chargeTypeAmounts.entrySet()) {
                if (e.getValue() == null) {
                    continue;
                }
                InwardChargeType type;
                try {
                    type = InwardChargeType.valueOf(e.getKey());
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                result.merge(type, e.getValue(), Double::sum);
            }
        }
        if (componentAllocations != null) {
            for (Map.Entry<InwardChargeType, Double> e : componentAllocations.entrySet()) {
                if (e.getValue() == null) {
                    continue;
                }
                result.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }
        return result;
    }

    /**
     * Whole-package netting: sums the admission's real, actual totals across
     * only the charge types the package covers (present in
     * perCategoryAllocations), and returns how much that sum exceeds
     * packageTotal — or 0.0 if it doesn't exceed it. A category's own
     * overspend can be absorbed by another category's underspend, since only
     * the combined total is compared against the package's combined total.
     */
    public static double calculatePackageExcess(
            Map<InwardChargeType, Double> perCategoryAllocations,
            Map<InwardChargeType, Double> actualTotalsByType,
            double packageTotal) {
        double actualCovered = 0.0;
        if (perCategoryAllocations != null && actualTotalsByType != null) {
            for (InwardChargeType type : perCategoryAllocations.keySet()) {
                Double actual = actualTotalsByType.get(type);
                if (actual != null) {
                    actualCovered += actual;
                }
            }
        }
        double excess = actualCovered - packageTotal;
        return excess > 0.0 ? excess : 0.0;
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `mvn -q test -Dtest=InpatientPackagePricingTest`
Expected: `Tests run: 13, Failures: 0, Errors: 0` (6 pre-existing + 7 new).

- [ ] **Step 5: Compile the full project**

Run: `mvn -q compile -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/divudi/core/data/inward/InwardChargeType.java src/main/java/com/divudi/core/util/InpatientPackagePricing.java src/test/java/com/divudi/core/util/InpatientPackagePricingTest.java
git commit -m "feat(inward): add PackageExcessCharges charge type and package-math utilities"
```

---

### Task 2: Remove `fromPackage` from `BillItem`, `BillFee`, `PatientRoom`

**Files:**
- Modify: `src/main/java/com/divudi/core/entity/BillItem.java`
- Modify: `src/main/java/com/divudi/core/entity/BillFee.java`
- Modify: `src/main/java/com/divudi/core/entity/inward/PatientRoom.java`

**Interfaces:**
- Removes: `BillItem.isFromPackage()`/`setFromPackage(boolean)`, `BillFee.isFromPackage()`/`setFromPackage(boolean)`, `PatientRoom.isFromPackage()`/`setFromPackage(boolean)`. Every caller of these is removed in Tasks 3–9 — **do this task last among the entity/guard-removal tasks would break the build; instead do this task FIRST among Tasks 2–9 is also fine as long as Tasks 3–9 are completed in the same PR before building** — since the plan is executed task-by-task with a build check at the end of each task, expect `mvn compile` to fail after this task alone until Tasks 3–9 remove all callers. This is intentional: run the full sequence through Task 9 before the next `mvn compile` checkpoint (Task 9's own build step covers it).
- Kept: `BillItem.getSourcePackageItem()`/`setSourcePackageItem(...)`, `BillFee.getSourcePackageItem()`/`setSourcePackageItem(...)` — unchanged, still used by Task 3.

- [ ] **Step 1: Remove from `BillItem.java`**

Delete the field (currently around line 155-157, exact block):
```java
    private boolean fromPackage;

    @ManyToOne
    private InpatientPackageItem sourcePackageItem;
```
Replace with (keep `sourcePackageItem` only):
```java
    @ManyToOne
    private InpatientPackageItem sourcePackageItem;
```

In the three copy-constructor blocks (currently around lines 321-322, 369-370, 404-405), each looks like:
```java
        fromPackage = billItem.isFromPackage();
        sourcePackageItem = billItem.getSourcePackageItem();
```
Replace each with:
```java
        sourcePackageItem = billItem.getSourcePackageItem();
```
(There are 3 occurrences of this exact two-line pattern — replace all 3.)

Delete the accessor block (currently around lines 844-849):
```java
    public boolean isFromPackage() {
        return fromPackage;
    }

    public void setFromPackage(boolean fromPackage) {
        this.fromPackage = fromPackage;
    }

```
(Leave the `getSourcePackageItem()`/`setSourcePackageItem(...)` pair immediately after this block untouched.)

- [ ] **Step 2: Remove from `BillFee.java`**

Same pattern. Delete the field (currently around line 149):
```java
    private boolean fromPackage;

    @ManyToOne
    private InpatientPackageItem sourcePackageItem;
```
→
```java
    @ManyToOne
    private InpatientPackageItem sourcePackageItem;
```

Delete the accessor block (currently around lines 209-214):
```java
    public boolean isFromPackage() {
        return fromPackage;
    }

    public void setFromPackage(boolean fromPackage) {
        this.fromPackage = fromPackage;
    }

```

In the two copy-constructor blocks (currently around lines 259-260, 274-275):
```java
        fromPackage = billFee.isFromPackage();
        sourcePackageItem = billFee.getSourcePackageItem();
```
→
```java
        sourcePackageItem = billFee.getSourcePackageItem();
```
(2 occurrences.)

- [ ] **Step 3: Remove from `PatientRoom.java`**

`PatientRoom` has no `sourcePackageItem` — delete the field entirely (currently around line 105):
```java
    private boolean fromPackage;
```
Delete the accessor block (currently around lines 783-788):
```java
    public boolean isFromPackage() {
        return fromPackage;
    }

    public void setFromPackage(boolean fromPackage) {
        this.fromPackage = fromPackage;
    }

```

- [ ] **Step 4: Commit** (build check deferred to Task 9 per the note above)

```bash
git add src/main/java/com/divudi/core/entity/BillItem.java src/main/java/com/divudi/core/entity/BillFee.java src/main/java/com/divudi/core/entity/inward/PatientRoom.java
git commit -m "feat(inward): remove fromPackage lock field from BillItem, BillFee, PatientRoom"
```

---

### Task 3: Redesign `InpatientPackageApplicationBean`

**Files:**
- Modify: `src/main/java/com/divudi/ejb/InpatientPackageApplicationBean.java`

**Interfaces:**
- Consumes: `professionalFeeClassificationService.defaultCategoryFor(Staff staff, Speciality selectedSpeciality)` (existing, `staff` may be `null` — already documented and tested to fall back to `TechnicianAndParamedicalCharge` when both are unconfigured). Needs a new `@EJB private com.divudi.service.inward.InwardProfessionalFeeClassificationService professionalFeeClassificationService;` injection (not currently present in this bean).
- Removes: `lockRoomCharge(...)` entirely.
- Renames (drops "Locked" from the name, since rows are no longer locked): `createLockedServiceBillItem`→`createServiceBillItemFromComponent`, `createLockedTimedItem`→`createTimedItemFromComponent`, `createLockedProfessionalFee`→`createProfessionalFeeFromComponent`, `createLockedOutsideCharge`→`createOutsideChargeFromComponent`, `createLockedBill`→`createBillForComponent`.

- [ ] **Step 1: Remove the `lockRoomCharge` call and method**

Replace:
```java
    public void applyPackageToAdmission(Admission admission, PatientRoom patientRoom, WebUser loggedUser) {
        InpatientPackage inpatientPackage = admission.getInpatientPackage();
        if (inpatientPackage == null) {
            return;
        }

        lockRoomCharge(inpatientPackage, patientRoom);

        Map<String, Object> params = new HashMap<>();
        params.put("pkg", inpatientPackage);
        List<InpatientPackageItem> components = inpatientPackageItemFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i WHERE i.retired = false AND i.inpatientPackage = :pkg",
                params);

        for (InpatientPackageItem component : components) {
            switch (component.getComponentType()) {
                case SERVICE:
                    createLockedServiceBillItem(admission, component, loggedUser);
                    break;
                case TIMED_ITEM:
                    createLockedTimedItem(admission, component, loggedUser);
                    break;
                case PROFESSIONAL_FEE_ROLE:
                    createLockedProfessionalFee(admission, component, loggedUser);
                    break;
                case OUTSIDE_CHARGE:
                    createLockedOutsideCharge(admission, component, loggedUser);
                    break;
                case PHARMACY_ITEM:
                    // Not pre-created — consumed progressively via pharmacy issue, see Task 16.
                    break;
                default:
                    break;
            }
        }
    }

    private void lockRoomCharge(InpatientPackage inpatientPackage, PatientRoom patientRoom) {
        if (patientRoom == null || patientRoom.getId() == null) {
            return;
        }
        patientRoom.setFromPackage(true);
        patientRoom.setIncludedRoomDurationHours(inpatientPackage.getIncludedRoomDurationHours());
        patientRoom.setCurrentRoomCharge(inpatientPackage.getFixedRoomCharge() != null ? inpatientPackage.getFixedRoomCharge() : 0.0);
        patientRoom.setCurrentMaintananceCharge(0.0);
        patientRoom.setCurrentNursingCharge(0.0);
        patientRoom.setCurrentMoCharge(0.0);
        patientRoom.setCurrentMoChargeForAfterDuration(0.0);
        patientRoom.setCurrentLinenCharge(0.0);
        patientRoom.setCurrentAdministrationCharge(0.0);
        patientRoom.setCurrentMedicalCareCharge(0.0);
        patientRoomFacade.edit(patientRoom);
    }
```
with:
```java
    public void applyPackageToAdmission(Admission admission, PatientRoom patientRoom, WebUser loggedUser) {
        InpatientPackage inpatientPackage = admission.getInpatientPackage();
        if (inpatientPackage == null) {
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("pkg", inpatientPackage);
        List<InpatientPackageItem> components = inpatientPackageItemFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i WHERE i.retired = false AND i.inpatientPackage = :pkg",
                params);

        for (InpatientPackageItem component : components) {
            switch (component.getComponentType()) {
                case SERVICE:
                    createServiceBillItemFromComponent(admission, component, loggedUser);
                    break;
                case TIMED_ITEM:
                    createTimedItemFromComponent(admission, component, loggedUser);
                    break;
                case PROFESSIONAL_FEE_ROLE:
                    createProfessionalFeeFromComponent(admission, component, loggedUser);
                    break;
                case OUTSIDE_CHARGE:
                    createOutsideChargeFromComponent(admission, component, loggedUser);
                    break;
                case PHARMACY_ITEM:
                    // Not pre-created — consumed progressively via pharmacy issue.
                    break;
                default:
                    break;
            }
        }
    }
```
(`patientRoom` is now an unused parameter of `applyPackageToAdmission` — leave the parameter in place rather than changing the method signature, since `AdmissionController.saveSelected()` calls it with that argument and changing the signature is out of scope for this plan; an unused-parameter warning is acceptable here.)

- [ ] **Step 2: Rename the four creation methods and `createLockedBill`, dropping `fromPackage=true`**

Replace:
```java
    private BilledBill createLockedBill(Admission admission, BillType billType, BillTypeAtomic billTypeAtomic, BillNumberSuffix suffix, Department toDepartment, WebUser loggedUser) {
```
with:
```java
    private BilledBill createBillForComponent(Admission admission, BillType billType, BillTypeAtomic billTypeAtomic, BillNumberSuffix suffix, Department toDepartment, WebUser loggedUser) {
```
(body unchanged).

Replace:
```java
    private void createLockedServiceBillItem(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        Department toDepartment = component.getItem() != null ? component.getItem().getDepartment() : null;
        BilledBill bill = createLockedBill(admission, BillType.InwardBill, BillTypeAtomic.INWARD_SERVICE_BILL, BillNumberSuffix.INWSER, toDepartment, loggedUser);
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(component.getItem());
        billItem.setQty(component.getQty());
        billItem.setInwardChargeType(component.getItem() != null ? component.getItem().getInwardChargeType() : null);
        billItem.setPatientEncounter(admission);
        billItem.setGrossValue(component.getFixedPrice());
        billItem.setNetValue(component.getFixedPrice());
        billItem.setOverriddenRate(component.getFixedPrice());
        billItem.setFromPackage(true);
        billItem.setSourcePackageItem(component);
        billItem.setCreatedAt(new Date());
        billItem.setCreater(loggedUser);
        billItemFacade.create(billItem);
    }

    private void createLockedTimedItem(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        Department toDepartment = component.getItem() != null ? component.getItem().getDepartment() : null;
        BilledBill bill = createLockedBill(admission, BillType.InwardBill, BillTypeAtomic.INWARD_SERVICE_BILL, BillNumberSuffix.INWSER, toDepartment, loggedUser);
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(component.getItem());
        billItem.setQty(component.getQty());
        billItem.setInwardChargeType(component.getItem() != null ? component.getItem().getInwardChargeType() : null);
        billItem.setPatientEncounter(admission);
        billItem.setGrossValue(component.getFixedPrice());
        billItem.setNetValue(component.getFixedPrice());
        billItem.setOverriddenRate(component.getFixedPrice());
        billItem.setFromPackage(true);
        billItem.setSourcePackageItem(component);
        billItem.setCreatedAt(new Date());
        billItem.setCreater(loggedUser);
        billItemFacade.create(billItem);

        PatientItem patientItem = new PatientItem();
        patientItem.setPatient(admission.getPatient());
        patientItem.setPatientEncounter(admission);
        patientItem.setBill(bill);
        patientItem.setBillItem(billItem);
        patientItem.setItem(component.getItem());
        patientItem.setServiceValue(component.getFixedPrice());
        patientItem.setDiscount(0.0);
        patientItem.setCreatedAt(new Date());
        patientItem.setCreater(loggedUser);
        patientItemFacade.create(patientItem);
    }

    private void createLockedProfessionalFee(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        BilledBill bill = createLockedBill(admission, BillType.InwardProfessional, BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL, BillNumberSuffix.NONE, null, loggedUser);
        BillFee billFee = new BillFee();
        billFee.setBill(bill);
        billFee.setPatienEncounter(admission);
        billFee.setSpeciality(component.getSpeciality());
        billFee.setStaff(null); // Assigned later — see Task 15
        billFee.setFeeValue(component.getFixedPrice());
        billFee.setFeeGrossValue(component.getFixedPrice());
        billFee.setOverriddenRate(component.getFixedPrice());
        billFee.setFromPackage(true);
        billFee.setSourcePackageItem(component);
        billFee.setFeeAt(new Date());
        billFee.setCreatedAt(new Date());
        billFee.setCreater(loggedUser);
        billFeeFacade.create(billFee);
    }

    private void createLockedOutsideCharge(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        BilledBill bill = createLockedBill(admission, BillType.InwardOutSideBill, BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL, BillNumberSuffix.NONE, null, loggedUser);
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(component.getItem());
        billItem.setInwardChargeType(component.getItem() != null ? component.getItem().getInwardChargeType() : null);
        billItem.setPatientEncounter(admission);
        billItem.setDescreption(component.getItem() != null ? component.getItem().getName() : component.getRoleLabel());
        billItem.setGrossValue(component.getFixedPrice());
        billItem.setNetValue(component.getFixedPrice());
        billItem.setOverriddenRate(component.getFixedPrice());
        billItem.setFromPackage(true);
        billItem.setSourcePackageItem(component);
        billItem.setCreatedAt(new Date());
        billItem.setCreater(loggedUser);
        billItemFacade.create(billItem);
    }
```
with:
```java
    private void createServiceBillItemFromComponent(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        Department toDepartment = component.getItem() != null ? component.getItem().getDepartment() : null;
        BilledBill bill = createBillForComponent(admission, BillType.InwardBill, BillTypeAtomic.INWARD_SERVICE_BILL, BillNumberSuffix.INWSER, toDepartment, loggedUser);
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(component.getItem());
        billItem.setQty(component.getQty());
        billItem.setInwardChargeType(component.getItem() != null ? component.getItem().getInwardChargeType() : null);
        billItem.setPatientEncounter(admission);
        billItem.setGrossValue(component.getFixedPrice());
        billItem.setNetValue(component.getFixedPrice());
        billItem.setOverriddenRate(component.getFixedPrice());
        billItem.setSourcePackageItem(component);
        billItem.setCreatedAt(new Date());
        billItem.setCreater(loggedUser);
        billItemFacade.create(billItem);
    }

    private void createTimedItemFromComponent(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        Department toDepartment = component.getItem() != null ? component.getItem().getDepartment() : null;
        BilledBill bill = createBillForComponent(admission, BillType.InwardBill, BillTypeAtomic.INWARD_SERVICE_BILL, BillNumberSuffix.INWSER, toDepartment, loggedUser);
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(component.getItem());
        billItem.setQty(component.getQty());
        billItem.setInwardChargeType(component.getItem() != null ? component.getItem().getInwardChargeType() : null);
        billItem.setPatientEncounter(admission);
        billItem.setGrossValue(component.getFixedPrice());
        billItem.setNetValue(component.getFixedPrice());
        billItem.setOverriddenRate(component.getFixedPrice());
        billItem.setSourcePackageItem(component);
        billItem.setCreatedAt(new Date());
        billItem.setCreater(loggedUser);
        billItemFacade.create(billItem);

        PatientItem patientItem = new PatientItem();
        patientItem.setPatient(admission.getPatient());
        patientItem.setPatientEncounter(admission);
        patientItem.setBill(bill);
        patientItem.setBillItem(billItem);
        patientItem.setItem(component.getItem());
        patientItem.setServiceValue(component.getFixedPrice());
        patientItem.setDiscount(0.0);
        patientItem.setCreatedAt(new Date());
        patientItem.setCreater(loggedUser);
        patientItemFacade.create(patientItem);
    }

    private void createProfessionalFeeFromComponent(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        BilledBill bill = createBillForComponent(admission, BillType.InwardProfessional, BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL, BillNumberSuffix.NONE, null, loggedUser);
        BillFee billFee = new BillFee();
        billFee.setBill(bill);
        billFee.setPatienEncounter(admission);
        billFee.setSpeciality(component.getSpeciality());
        billFee.setStaff(null); // Assigned later via InwardProfessionalBillController.assignStaffToPackageFee
        billFee.setFeeValue(component.getFixedPrice());
        billFee.setFeeGrossValue(component.getFixedPrice());
        billFee.setOverriddenRate(component.getFixedPrice());
        billFee.setProfessionalFeeCategory(professionalFeeClassificationService.defaultCategoryFor(null, component.getSpeciality()));
        billFee.setSourcePackageItem(component);
        billFee.setFeeAt(new Date());
        billFee.setCreatedAt(new Date());
        billFee.setCreater(loggedUser);
        billFeeFacade.create(billFee);
    }

    private void createOutsideChargeFromComponent(Admission admission, InpatientPackageItem component, WebUser loggedUser) {
        BilledBill bill = createBillForComponent(admission, BillType.InwardOutSideBill, BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL, BillNumberSuffix.NONE, null, loggedUser);
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(component.getItem());
        billItem.setInwardChargeType(component.getItem() != null ? component.getItem().getInwardChargeType() : null);
        billItem.setPatientEncounter(admission);
        billItem.setDescreption(component.getItem() != null ? component.getItem().getName() : component.getRoleLabel());
        billItem.setGrossValue(component.getFixedPrice());
        billItem.setNetValue(component.getFixedPrice());
        billItem.setOverriddenRate(component.getFixedPrice());
        billItem.setSourcePackageItem(component);
        billItem.setCreatedAt(new Date());
        billItem.setCreater(loggedUser);
        billItemFacade.create(billItem);
    }
```

- [ ] **Step 3: Add the `professionalFeeClassificationService` injection**

Add alongside the other `@EJB` fields near the top of the class:
```java
    @EJB
    private com.divudi.service.inward.InwardProfessionalFeeClassificationService professionalFeeClassificationService;
```

- [ ] **Step 4: Update the class-level doc comment**

Replace:
```java
/**
 * Creates the locked, package-derived billing rows (service, timed item,
 * professional-fee role, outside charge) as soon as a package-linked
 * admission is saved, and locks the room charge on the newly created
 * PatientRoom. Pharmacy-item components are intentionally NOT created here
 * — they are consumed progressively via pharmacy issue (see Task 16).
```
with:
```java
/**
 * Creates ordinary, fully-editable billing rows (service, timed item,
 * professional-fee role, outside charge) from a package's components as
 * soon as a package-linked admission is saved. These rows carry no special
 * lock — they are ordered normally and can be edited/removed like any other
 * bill row; sourcePackageItem is kept purely for traceability. The room
 * charges normally too (no package-specific handling at all). Pharmacy-item
 * components are intentionally NOT created here — they are consumed
 * progressively via normal pharmacy issue, at normal rates.
```

- [ ] **Step 5: Commit** (build check deferred — `mvn compile` still fails until Task 9 removes all remaining callers of the deleted `fromPackage` accessors elsewhere)

```bash
git add src/main/java/com/divudi/ejb/InpatientPackageApplicationBean.java
git commit -m "feat(inward): stop locking package-derived billing rows; bill them normally"
```

---

### Task 4: `BhtSummeryController` — remove guards and room-variance helpers

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java`

**Interfaces:**
- Removes: `isPackageRoomDurationExceeded(PatientRoom)`, `getPackageRoomVarianceCharge(PatientRoom)` (public — confirmed to have zero callers anywhere in the codebase besides its own file; safe to delete outright).

- [ ] **Step 1: `updateChargesForRoom` — remove the package-skip branch**

Replace:
```java
    public void updateChargesForRoom(PatientRoom pr) {
        if (pr == null || pr.getRoomFacilityCharge() == null) {
            JsfUtil.addErrorMessage("Room facility charge not set");
            return;
        }
        if (pr.isFromPackage() && !isPackageRoomDurationExceeded(pr)) {
            // Package-locked charge stays as set by InpatientPackageApplicationBean,
            // but newly-linked timed items still need to be snapshotted so their
            // charges aren't silently dropped from the bill.
            getInwardBean().snapshotTimedItems(pr, pr.getRoomFacilityCharge());
            patientRooms = null;
            createTables();
            return;
        }
        RoomFacilityCharge rfc = pr.getRoomFacilityCharge();
```
with:
```java
    public void updateChargesForRoom(PatientRoom pr) {
        if (pr == null || pr.getRoomFacilityCharge() == null) {
            JsfUtil.addErrorMessage("Room facility charge not set");
            return;
        }
        RoomFacilityCharge rfc = pr.getRoomFacilityCharge();
```

- [ ] **Step 2: `updatePrintingPatientRoom` — remove the package-locked branch**

Replace:
```java
        if (patientRoom.isFromPackage() && !isPackageRoomDurationExceeded(patientRoom)) {
            // Package-locked room: currentRoomCharge already holds the package's fixed
            // total, not a per-block rate - do not overwrite it with the facility rate
            // while still within the included duration.
            patientRoom.setCalculatedRoomCharge(patientRoom.getCurrentRoomCharge() + patientRoom.getAddedRoomCharge());
        } else {
            patientRoom.setCurrentRoomCharge(patientRoom.getRoomFacilityCharge().getRoomCharge());
            calCulateRoomCharge(patientRoom);
        }
```
with:
```java
        patientRoom.setCurrentRoomCharge(patientRoom.getRoomFacilityCharge().getRoomCharge());
        calCulateRoomCharge(patientRoom);
```

- [ ] **Step 3: Delete `isPackageRoomDurationExceeded` and `getPackageRoomVarianceCharge`**

Delete both methods entirely (immediately follow each other in the file; confirmed zero callers of `getPackageRoomVarianceCharge` anywhere in `.java` or `.xhtml`, so it is safe to remove outright rather than just its guard):
```java
    private boolean isPackageRoomDurationExceeded(PatientRoom pr) {
        if (pr.getIncludedRoomDurationHours() == null) {
            return true;
        }
        Date to = pr.getDischargedAt() != null ? pr.getDischargedAt() : new Date();
        if (pr.getAdmittedAt() == null) {
            return false;
        }
        long stayedHours = java.time.Duration.between(
                pr.getAdmittedAt().toInstant(), to.toInstant()).toHours();
        return stayedHours > pr.getIncludedRoomDurationHours();
    }

    public double getPackageRoomVarianceCharge(PatientRoom pr) {
        if (pr == null || !pr.isFromPackage() || !isPackageRoomDurationExceeded(pr) || pr.getRoomFacilityCharge() == null) {
            return 0.0;
        }
        // currentRoomCharge holds the package's locked TOTAL for this room, not a
        // per-block rate, so it must not be used as the multiplicand here (that was
        // the bug: reusing calCulateRoomCharge(pr), which multiplies
        // pr.getCurrentRoomCharge() by elapsed blocks). Both sides of this variance
        // must be derived from the room's real per-block rate, RoomFacilityCharge.roomCharge.
        Double facilityRoomCharge = pr.getRoomFacilityCharge().getRoomCharge();
        if (facilityRoomCharge == null) {
            return 0.0;
        }
        TimedItemFee timedFee = pr.getRoomFacilityCharge().getTimedItemFee();
        double liveEquivalent = facilityRoomCharge * getInwardBean().calCount(timedFee, pr.getAdmittedAt(), pr.getDischargedAt());
        // RoomFacilityCharge.roomCharge is a rate per TimedItemFee block (see
        // InwardBeanController.calCount: charge = roomCharge * count, where count is the number
        // of blocks between admittedAt/dischargedAt). Room-charge TimedItemFee
        // configs are conventionally 24-hour ("per day") blocks, so we use the actual configured
        // block length here (falling back to 24.0 if unset) rather than hardcoding 24.
        // getDurationInHours() honours the configured duration unit, so a block defined in
        // minutes or days converts to hours instead of being read as a raw hour count.
        double blockHours = (timedFee != null && timedFee.getDurationInHours() > 0) ? timedFee.getDurationInHours() : 24.0;
        double includedEquivalent = facilityRoomCharge * (pr.getIncludedRoomDurationHours() / blockHours);
        return Math.max(0.0, liveEquivalent - includedEquivalent);
    }
```
Delete both in full (through the final `}` shown above), leaving the following method (`checkDischargeTime()`) untouched immediately after.

- [ ] **Step 4: `finalizeRunningTimedServices` — remove the package-skip**

Replace:
```java
        int closed = 0;
        for (PatientItem pi : running) {
            if (pi.getBillItem() != null && pi.getBillItem().isFromPackage()) {
                continue;
            }
            if (pi.getFromTime() != null && dischargeTime.before(pi.getFromTime())) {
```
with:
```java
        int closed = 0;
        for (PatientItem pi : running) {
            if (pi.getFromTime() != null && dischargeTime.before(pi.getFromTime())) {
```

- [ ] **Step 5: `syncTimedServiceCharge` — remove the short-circuit**

Replace:
```java
    private void syncTimedServiceCharge(PatientItem patientItem) {
        if (patientItem == null || patientItem.getBillItem() == null) {
            return;
        }
        BillItem bi = patientItem.getBillItem();
        if (bi.isFromPackage()) {
            return;
        }
        double discount = bi.getDiscount();
```
with:
```java
    private void syncTimedServiceCharge(PatientItem patientItem) {
        if (patientItem == null || patientItem.getBillItem() == null) {
            return;
        }
        BillItem bi = patientItem.getBillItem();
        double discount = bi.getDiscount();
```
Also update the method's doc comment immediately above it — replace the sentence "Package-locked items are skipped — their price is fixed by the package." with nothing (delete that sentence, keep the rest of the comment).

- [ ] **Step 6: `applyRoomChargeDiscounts` — remove the package-skip early return**

Replace:
```java
    private void applyRoomChargeDiscounts(PatientRoom p,
            double roomPct, double maintainPct, double linenPct, double nursingPct,
            double moPct, double adminPct, double medicalCarePct) {
        if (p.isFromPackage() && !isPackageRoomDurationExceeded(p)) {
            // Package-locked room: the price is fixed by the package, not subject
            // to PriceMatrix discount percentages while within the included duration.
            p.setDiscountRoomCharge(0.0);
            p.setDiscountMaintainCharge(0.0);
            p.setDiscountLinenCharge(0.0);
            p.setDiscountNursingCharge(0.0);
            p.setDiscountMoCharge(0.0);
            p.setDiscountAdministrationCharge(0.0);
            p.setDiscountMedicalCareCharge(0.0);
            p.setAdjustedRoomCharge(p.getCalculatedRoomCharge());
            p.setAdjustedMaintainCharge(p.getCalculatedMaintainCharge());
            p.setAjdustedLinenCharge(p.getCalculatedLinenCharge());
            p.setAjdustedNursingCharge(p.getCalculatedNursingCharge());
            p.setAdjustedMoCharge(p.getCalculatedMoCharge());
            p.setAjdustedAdministrationCharge(p.getCalculatedAdministrationCharge());
            p.setAjdustedMedicalCareCharge(p.getCalculatedMedicalCareCharge());
            return;
        }
        double roomDisc = (roomPct / 100.0) * p.getCalculatedRoomCharge();
```
with:
```java
    private void applyRoomChargeDiscounts(PatientRoom p,
            double roomPct, double maintainPct, double linenPct, double nursingPct,
            double moPct, double adminPct, double medicalCarePct) {
        double roomDisc = (roomPct / 100.0) * p.getCalculatedRoomCharge();
```

- [ ] **Step 7: `calculateRoomCharge` — remove the package-skip early return**

Replace:
```java
    private void calculateRoomCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentRoomCharge() == 0) {
            p.setCalculatedRoomCharge(0);
            p.setMarginRoomCharge(0.0);
            return;
        }

        if (p.isFromPackage() && !isPackageRoomDurationExceeded(p)) {
            // Package-locked room: currentRoomCharge already holds the package's
            // fixed total for the room, not a per-block rate — do not multiply
            // it by elapsed TimedItemFee blocks while within the included duration.
            p.setCalculatedRoomCharge(p.getCurrentRoomCharge() + p.getAddedRoomCharge());
            p.setMarginRoomCharge(0.0);
            return;
        }

        double roomCharge = p.getCurrentRoomCharge();
```
with:
```java
    private void calculateRoomCharge(PatientRoom p) {

        if (p.getRoomFacilityCharge() == null || p.getCurrentRoomCharge() == 0) {
            p.setCalculatedRoomCharge(0);
            p.setMarginRoomCharge(0.0);
            return;
        }

        double roomCharge = p.getCurrentRoomCharge();
```
(Note: this is a *different* `calculateRoomCharge` method than the earlier `calCulateRoomCharge` touched in Step 2 of `updatePrintingPatientRoom` — same-sounding names, different capitalization, different methods; do not conflate them.)

- [ ] **Step 8: `addRunningTimedServiceLiveTopUp` — remove the package-skip**

Replace:
```java
        for (PatientItem pi : running) {
            if (pi.getItem() == null || !(pi.getItem() instanceof TimedItem)) {
                continue;
            }
            // Package-locked services keep their fixed price - skip, same as the
            // discharge-time close (finalizeRunningTimedServices).
            if (pi.getBillItem() != null && pi.getBillItem().isFromPackage()) {
                continue;
            }
            // A start time in the future has not accrued anything yet.
            if (pi.getFromTime() == null || now.before(pi.getFromTime())) {
```
with:
```java
        for (PatientItem pi : running) {
            if (pi.getItem() == null || !(pi.getItem() instanceof TimedItem)) {
                continue;
            }
            // A start time in the future has not accrued anything yet.
            if (pi.getFromTime() == null || now.before(pi.getFromTime())) {
```

- [ ] **Step 9: Confirm no remaining `fromPackage`/`isPackageRoomDurationExceeded`/`getPackageRoomVarianceCharge` references in this file**

Run: `grep -n "fromPackage\|isPackageRoomDurationExceeded\|getPackageRoomVarianceCharge" src/main/java/com/divudi/bean/inward/BhtSummeryController.java`
Expected: no output.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/BhtSummeryController.java
git commit -m "feat(inward): remove package-lock guards from room/timed-service billing"
```

(Do not run `mvn compile` yet — Task 5 adds the new logic to this same file and Task 6-9 remove the remaining callers elsewhere; compile after Task 9.)

---

### Task 5: `BhtSummeryController` — add the package-aware final-bill branch

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java`

**Interfaces:**
- Consumes: `InpatientPackagePricing.calculateChargeTypeAllocations(...)`, `InpatientPackagePricing.calculatePackageExcess(...)` (Task 1); `professionalFeeClassificationService.defaultCategoryFor(Staff, Speciality)` (already injected in this class per earlier grep); `InpatientPackageItemFacade.findByJpql(String, Map)` (new injection needed).
- Produces: `BhtSummeryController.applyPackagePricingIfApplicable()` (private, called from `createChargeItemTotals()`), `resolveComponentChargeTypeAllocations(InpatientPackage)` (private helper it uses).

- [ ] **Step 1: Add imports**

Add to the existing import block in `src/main/java/com/divudi/bean/inward/BhtSummeryController.java`:
```java
import com.divudi.core.entity.inward.InpatientPackage;
import com.divudi.core.entity.inward.InpatientPackageItem;
import com.divudi.core.data.inward.InpatientPackageComponentType;
import com.divudi.core.facade.InpatientPackageItemFacade;
import com.divudi.core.util.InpatientPackagePricing;
```
(Check each is not already present before adding — `InwardChargeType`, `HashMap`, `Map` are already imported per earlier confirmation.)

- [ ] **Step 2: Add the `InpatientPackageItemFacade` injection**

Add alongside the existing `@EJB private com.divudi.service.inward.InwardProfessionalFeeClassificationService professionalFeeClassificationService;` field:
```java
    @EJB
    private InpatientPackageItemFacade inpatientPackageItemFacade;
```

- [ ] **Step 3: Wire the new branch into `createChargeItemTotals()`**

Replace:
```java
        if (getPatientEncounter() != null) {
            setKnownChargeTot();

            setServiceTotCategoryWise();

            setTimedServiceTotCategoryWise();

            Map<InwardChargeType, Double> additionalChargeTotals = setChargeValueFromAdditional();

            setGrossMarginVatBreakdown(additionalChargeTotals);

            addRunningTimedServiceLiveTopUp();

        }
```
with:
```java
        if (getPatientEncounter() != null) {
            setKnownChargeTot();

            setServiceTotCategoryWise();

            setTimedServiceTotCategoryWise();

            Map<InwardChargeType, Double> additionalChargeTotals = setChargeValueFromAdditional();

            setGrossMarginVatBreakdown(additionalChargeTotals);

            addRunningTimedServiceLiveTopUp();

            applyPackagePricingIfApplicable();

        }
```

- [ ] **Step 4: Add the two new private methods**

Add near `createChargeItemTotals()` (e.g. immediately after it):
```java
    /**
     * For an admission under an InpatientPackage: overrides each
     * package-covered category's total with the package's own allocated
     * amount, and appends a PackageExcessCharges row if the admission's real
     * usage across those categories (whole-package netting, not per-category)
     * exceeds the package's total price. No-op for a non-package admission.
     */
    private void applyPackagePricingIfApplicable() {
        InpatientPackage inpatientPackage = getPatientEncounter().getInpatientPackage();
        if (inpatientPackage == null) {
            return;
        }

        Map<InwardChargeType, Double> componentAllocations = resolveComponentChargeTypeAllocations(inpatientPackage);
        Map<InwardChargeType, Double> perCategoryAllocations = InpatientPackagePricing.calculateChargeTypeAllocations(
                inpatientPackage.getChargeTypeAmounts(), componentAllocations);

        Map<InwardChargeType, Double> actualTotalsByType = new HashMap<>();
        for (ChargeItemTotal cit : chargeItemTotals) {
            actualTotalsByType.put(cit.getInwardChargeType(), cit.getTotal());
        }

        double packageTotal = inpatientPackage.getTotalPrice() != null ? inpatientPackage.getTotalPrice() : 0.0;
        double excess = InpatientPackagePricing.calculatePackageExcess(perCategoryAllocations, actualTotalsByType, packageTotal);

        for (ChargeItemTotal cit : chargeItemTotals) {
            Double allocation = perCategoryAllocations.get(cit.getInwardChargeType());
            if (allocation != null) {
                cit.setTotal(allocation);
            }
        }

        if (excess > 0.0) {
            ChargeItemTotal excessRow = new ChargeItemTotal();
            excessRow.setInwardChargeType(InwardChargeType.PackageExcessCharges);
            excessRow.setTotal(excess);
            chargeItemTotals.add(excessRow);
        }
    }

    /**
     * Each active component's fixed price, keyed by the InwardChargeType it
     * resolves to: the component's Item's own inwardChargeType for
     * SERVICE/TIMED_ITEM/OUTSIDE_CHARGE/PHARMACY_ITEM, or the same
     * staff-independent default category a professional fee entry form would
     * preselect (professionalFeeClassificationService.defaultCategoryFor with
     * a null staff) for PROFESSIONAL_FEE_ROLE, whose real BillFee has no
     * category until a specific staff member is later assigned.
     */
    private Map<InwardChargeType, Double> resolveComponentChargeTypeAllocations(InpatientPackage inpatientPackage) {
        Map<InwardChargeType, Double> result = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        params.put("pkg", inpatientPackage);
        List<InpatientPackageItem> components = inpatientPackageItemFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i WHERE i.retired = false AND i.inpatientPackage = :pkg",
                params);
        for (InpatientPackageItem component : components) {
            InwardChargeType type;
            if (component.getComponentType() == InpatientPackageComponentType.PROFESSIONAL_FEE_ROLE) {
                type = professionalFeeClassificationService.defaultCategoryFor(null, component.getSpeciality());
            } else if (component.getItem() != null) {
                type = component.getItem().getInwardChargeType();
            } else {
                continue;
            }
            if (type == null || component.getFixedPrice() == null) {
                continue;
            }
            result.merge(type, component.getFixedPrice(), Double::sum);
        }
        return result;
    }
```
(`List` is already imported in this file per its extensive existing use — confirm during implementation; add `import java.util.List;` if for some reason it is not.)

- [ ] **Step 5: Commit** (build deferred to Task 9)

```bash
git add src/main/java/com/divudi/bean/inward/BhtSummeryController.java
git commit -m "feat(inward): compute package-vs-actual pricing on the final bill"
```

---

### Task 6: `InwardBeanController` — remove `fromPackage` JPQL filters

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/InwardBeanController.java`

- [ ] **Step 1: `fetchTimedServiceBillItemsByInwardChargeType`**

Replace:
```java
    public List<BillItem> fetchTimedServiceBillItemsByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "Select s From BillItem s"
                + " where s.retired=false "
                + " and s.bill.billType=:btp "
                + " and s.bill.patientEncounter=:pe"
                + " and type(s.item)=:cls "
                + " and s.fromPackage=false "
                + " and s.item.inwardChargeType=:inw ";
```
with:
```java
    public List<BillItem> fetchTimedServiceBillItemsByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "Select s From BillItem s"
                + " where s.retired=false "
                + " and s.bill.billType=:btp "
                + " and s.bill.patientEncounter=:pe"
                + " and type(s.item)=:cls "
                + " and s.item.inwardChargeType=:inw ";
```
Also update its doc comment immediately above (currently "Timed-service BillItems for one charge type, excluding package-locked ones (their price is fixed by the package and must not be discounted).") — replace with "Timed-service BillItems for one charge type."

- [ ] **Step 2: `bulkClearTimedServiceBillItemsWithOutMatrix`**

Replace:
```java
    public void bulkClearTimedServiceBillItemsWithOutMatrix(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "UPDATE BillItem s SET s.discount = 0.0, s.netValue = s.grossValue + s.marginValue"
                + " WHERE s.retired = false"
                + " AND s.bill.billType = :btp"
                + " AND s.bill.patientEncounter = :pe"
                + " AND type(s.item) = :cls"
                + " AND s.fromPackage = false"
                + " AND s.item.inwardChargeType = :inw";
```
with:
```java
    public void bulkClearTimedServiceBillItemsWithOutMatrix(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "UPDATE BillItem s SET s.discount = 0.0, s.netValue = s.grossValue + s.marginValue"
                + " WHERE s.retired = false"
                + " AND s.bill.billType = :btp"
                + " AND s.bill.patientEncounter = :pe"
                + " AND type(s.item) = :cls"
                + " AND s.item.inwardChargeType = :inw";
```
Also delete the trailing comment a few lines below it that explains the old `fromPackage` exemption for `PatientItem` clearing:
```java
        // Deliberately not filtered on billItem.fromPackage. A package item's
        // price is fixed and never discounted, so its mirrored discount is
        // already zero and clearing it changes nothing — and avoiding that
        // navigation keeps this a plain bulk update over PatientItem's own
        // columns, matching bulkClearPatientItemsWithOutMatrix.
```
(Read the full surrounding paragraph during implementation to remove exactly this now-stale explanation without disturbing whatever unrelated comment/code follows it.)

- [ ] **Step 3: Confirm no remaining references**

Run: `grep -n "fromPackage" src/main/java/com/divudi/bean/inward/InwardBeanController.java`
Expected: no output.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/InwardBeanController.java
git commit -m "feat(inward): remove fromPackage filters from timed-service bulk queries"
```

---

### Task 7: `InwardTimedItemController` + `inward_timed_service_consume.xhtml`

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/InwardTimedItemController.java`
- Modify: `src/main/webapp/inward/inward_timed_service_consume.xhtml`

- [ ] **Step 1: `removeTimedEncFromDbase` — remove the block**

Replace:
```java
        if (encounterComponent.getBillItem() != null && encounterComponent.getBillItem().isFromPackage()) {
            JsfUtil.addErrorMessage("This item is included in the admission's package and cannot be removed.");
            return;
        }

        retiredEncounterComponent(encounterComponent);
```
with:
```java
        retiredEncounterComponent(encounterComponent);
```
(this exact block appears once, inside `removeTimedEncFromDbase`).

- [ ] **Step 2: `removePatientItem` — remove the block**

Replace:
```java
    public void removePatientItem(PatientItem patientItem) {
        if (patientItem != null && patientItem.getBillItem() != null && patientItem.getBillItem().isFromPackage()) {
            JsfUtil.addErrorMessage("This item is included in the admission's package and cannot be removed.");
            return;
        }
        if (patientItem != null && isLockedForChanges(patientItem.getPatientEncounter())) {
```
with:
```java
    public void removePatientItem(PatientItem patientItem) {
        if (patientItem != null && isLockedForChanges(patientItem.getPatientEncounter())) {
```

- [ ] **Step 3: `retireTimedServiceBill` — remove the short-circuit**

Replace:
```java
    public void retireTimedServiceBill(PatientItem patientItem) {
        BillItem bi = patientItem.getBillItem();
        if (bi == null || bi.isFromPackage()) {
            return;
        }
        bi.setRetired(true);
```
with:
```java
    public void retireTimedServiceBill(PatientItem patientItem) {
        BillItem bi = patientItem.getBillItem();
        if (bi == null) {
            return;
        }
        bi.setRetired(true);
```

- [ ] **Step 4: `syncTimedServiceCharge` — remove the short-circuit and update its doc comment**

Replace:
```java
    /**
     * Pushes a recalculated timed-service charge onto its BillItem and Bill.
     * <p>
     * The BillItem is what the inward totals actually sum (see
     * {@code InwardBeanController.calServiceBillItemsTotalByInwardChargeTypeBulk}),
     * so it must never be left holding a stale duration. Package-locked items
     * are skipped — their price is fixed by the package.
     * <p>
     * The discount comes from the BillItem, which is the side the inward
     * discount routines clear when no price matrix applies; reading it from the
     * PatientItem would re-apply a discount that had just been removed. The
     * PatientItem is mirrored back so the breakdown screens still agree.
     */
    private void syncTimedServiceCharge(PatientItem patientItem) {
        if (patientItem == null || patientItem.getBillItem() == null) {
            return;
        }
        BillItem bi = patientItem.getBillItem();
        if (bi.isFromPackage()) {
            return;
        }
        double discount = bi.getDiscount();
```
with:
```java
    /**
     * Pushes a recalculated timed-service charge onto its BillItem and Bill.
     * <p>
     * The BillItem is what the inward totals actually sum (see
     * {@code InwardBeanController.calServiceBillItemsTotalByInwardChargeTypeBulk}),
     * so it must never be left holding a stale duration.
     * <p>
     * The discount comes from the BillItem, which is the side the inward
     * discount routines clear when no price matrix applies; reading it from the
     * PatientItem would re-apply a discount that had just been removed. The
     * PatientItem is mirrored back so the breakdown screens still agree.
     */
    private void syncTimedServiceCharge(PatientItem patientItem) {
        if (patientItem == null || patientItem.getBillItem() == null) {
            return;
        }
        BillItem bi = patientItem.getBillItem();
        double discount = bi.getDiscount();
```

- [ ] **Step 5: `finalizeService` — remove the block**

Replace:
```java
    public void finalizeService(PatientItem pic) {
        if (pic != null && pic.getBillItem() != null && pic.getBillItem().isFromPackage()) {
            JsfUtil.addErrorMessage("This item is included in the admission's package and its charge cannot be changed.");
            return;
        }
        if (pic != null && isLockedForChanges(pic.getPatientEncounter())) {
```
with:
```java
    public void finalizeService(PatientItem pic) {
        if (pic != null && isLockedForChanges(pic.getPatientEncounter())) {
```

- [ ] **Step 6: Confirm no remaining references**

Run: `grep -n "fromPackage" src/main/java/com/divudi/bean/inward/InwardTimedItemController.java`
Expected: no output.

- [ ] **Step 7: `inward_timed_service_consume.xhtml` — remove the four client-side bindings**

Replace both occurrences of:
```xml
                                disabled="#{ti.billItem.fromPackage or ti.bill.checkedBy ne null}"
```
with:
```xml
                                disabled="#{ti.bill.checkedBy ne null}"
```
(2 occurrences, on the Start Time and Stopped Time `p:datePicker`s.)

Replace both occurrences of:
```xml
                                    rendered="#{not ti.billItem.fromPackage}" >
```
with removal of the attribute entirely (the buttons become unconditionally rendered, subject only to their existing `disabled` attributes) — i.e. delete that line from both the "Update" and "Remove" `p:commandButton`s.

- [ ] **Step 8: Confirm no remaining references**

Run: `grep -n "fromPackage" src/main/webapp/inward/inward_timed_service_consume.xhtml`
Expected: no output.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/InwardTimedItemController.java src/main/webapp/inward/inward_timed_service_consume.xhtml
git commit -m "feat(inward): allow editing/removing timed services that came from a package"
```

---

### Task 8: `InwardProfessionalBillController`

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/InwardProfessionalBillController.java`

- [ ] **Step 1: Remove the encounter-component removal guard**

Replace:
```java
        if (encounterComponent.getBillFee().isFromPackage()) {
            JsfUtil.addErrorMessage("This fee is included in the admission's package and cannot be removed.");
            return;
        }

        retiredEncounterComponent(encounterComponent);
```
with:
```java
        retiredEncounterComponent(encounterComponent);
```

- [ ] **Step 2: Remove the `remove(BillFee)` guard**

Replace:
```java
    public void remove(BillFee bf) {
        if (bf.isFromPackage()) {
            JsfUtil.addErrorMessage("This fee is included in the admission's package and cannot be removed.");
            return;
        }
        bf.setRetiredAt(new Date());
```
with:
```java
    public void remove(BillFee bf) {
        bf.setRetiredAt(new Date());
```

- [ ] **Step 3: `assignStaffToPackageFee` — switch its guard from `fromPackage` to `sourcePackageItem`**

Replace:
```java
    public void assignStaffToPackageFee(BillFee billFee, Staff staff) {
        if (billFee == null || !billFee.isFromPackage()) {
            JsfUtil.addErrorMessage("This action is only for package-included professional fee roles.");
            return;
        }
```
with:
```java
    public void assignStaffToPackageFee(BillFee billFee, Staff staff) {
        if (billFee == null || billFee.getSourcePackageItem() == null) {
            JsfUtil.addErrorMessage("This action is only for package-included professional fee roles.");
            return;
        }
```
(This method's body is otherwise unchanged — it still sets `professionalFeeCategory` from the real assigned staff, which will now *update* the placeholder `defaultCategoryFor(null, speciality)` category Task 3 pre-set with the real `defaultCategoryFor(staff, speciality)` result once a specific person is chosen.)

- [ ] **Step 4: Confirm no remaining `isFromPackage`/`setFromPackage` references**

Run: `grep -n "isFromPackage\|setFromPackage" src/main/java/com/divudi/bean/inward/InwardProfessionalBillController.java`
Expected: no output (`getSourcePackageItem` references are fine and expected).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/InwardProfessionalBillController.java
git commit -m "feat(inward): allow removing package professional fees; key staff-assignment off sourcePackageItem"
```

---

### Task 9: `SurgeryChargeSummaryController` + `patient_surgery.xhtml`, and final compile

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/SurgeryChargeSummaryController.java`
- Modify: `src/main/webapp/theater/patient_surgery.xhtml`

- [ ] **Step 1: Simplify `createTheatreStayCharge`**

Replace:
```java
        double blocks = inwardBean.calCount(blockFee, room.getAdmittedAt(), to);
        // Mirrors BhtSummeryController.calculateRoomCharge: a package-locked
        // room within its included duration holds the package's fixed total in
        // currentRoomCharge, not a per-block rate.
        boolean packageLocked = room.isFromPackage() && !isPackageRoomDurationExceeded(room, to);
        double roomPart = packageLocked
                ? room.getCurrentRoomCharge()
                : room.getCurrentRoomCharge() * blocks;
        double perBlock = room.getCurrentMaintananceCharge()
                + room.getCurrentNursingCharge()
                + room.getCurrentAdministrationCharge()
                + room.getCurrentMedicalCareCharge();
        double added = room.getAddedRoomCharge()
                + room.getAddedMaintainCharge()
                + room.getAddedNursingCharge()
                + room.getAddedAdministrationCharge()
                + room.getAddedMedicalCareCharge();
        double discount = room.getDiscountRoomCharge()
                + room.getDiscountMaintainCharge()
                + room.getDiscountNursingCharge()
                + room.getDiscountAdministrationCharge()
                + room.getDiscountMedicalCareCharge();
        stay.setBlocks(blocks);
        stay.setRatePerBlock(packageLocked ? perBlock : perBlock + room.getCurrentRoomCharge());
        stay.setPackageLocked(packageLocked);
        stay.setTimeBasedCharge(roomPart + perBlock * blocks + added - discount);
```
with:
```java
        double blocks = inwardBean.calCount(blockFee, room.getAdmittedAt(), to);
        double roomPart = room.getCurrentRoomCharge() * blocks;
        double perBlock = room.getCurrentMaintananceCharge()
                + room.getCurrentNursingCharge()
                + room.getCurrentAdministrationCharge()
                + room.getCurrentMedicalCareCharge();
        double added = room.getAddedRoomCharge()
                + room.getAddedMaintainCharge()
                + room.getAddedNursingCharge()
                + room.getAddedAdministrationCharge()
                + room.getAddedMedicalCareCharge();
        double discount = room.getDiscountRoomCharge()
                + room.getDiscountMaintainCharge()
                + room.getDiscountNursingCharge()
                + room.getDiscountAdministrationCharge()
                + room.getDiscountMedicalCareCharge();
        stay.setBlocks(blocks);
        stay.setRatePerBlock(perBlock + room.getCurrentRoomCharge());
        stay.setTimeBasedCharge(roomPart + perBlock * blocks + added - discount);
```

- [ ] **Step 2: Delete the now-unused `isPackageRoomDurationExceeded(PatientRoom, Date)` private method**

Delete:
```java
    // Same rule as BhtSummeryController.isPackageRoomDurationExceeded.
    private boolean isPackageRoomDurationExceeded(PatientRoom room, Date to) {
        if (room.getIncludedRoomDurationHours() == null) {
            return true;
        }
        if (room.getAdmittedAt() == null) {
            return false;
        }
        long stayedHours = java.time.Duration.between(
                room.getAdmittedAt().toInstant(), to.toInstant()).toHours();
        return stayedHours > room.getIncludedRoomDurationHours();
    }
```

- [ ] **Step 3: Delete the `packageLocked` field and its accessors from the `TheatreStayCharge` inner class**

Find (inner class, currently around line 320):
```java
    private boolean packageLocked;
```
Delete this field declaration. Find its accessor pair (currently around lines 390-397):
```java
        public boolean isPackageLocked() {
            return packageLocked;
        }

        public void setPackageLocked(boolean packageLocked) {
            this.packageLocked = packageLocked;
        }
```
(exact getter name/indentation confirmed during implementation by reading the inner class body) — delete this pair.

- [ ] **Step 4: Confirm no remaining references**

Run: `grep -n "fromPackage\|packageLocked\|isPackageRoomDurationExceeded" src/main/java/com/divudi/bean/inward/SurgeryChargeSummaryController.java`
Expected: no output.

- [ ] **Step 5: `patient_surgery.xhtml` — remove the two conditional panels**

Replace:
```xml
                                                <h:panelGroup rendered="#{not ts.packageLocked}">
                                                    <h:outputText value="#{ts.blocks}">
                                                        <f:convertNumber pattern="#,##0.##"/>
                                                    </h:outputText>
                                                    <h:outputText value=" × "/>
                                                    <h:outputText value="#{ts.ratePerBlock}">
                                                        <f:convertNumber pattern="#,##0.00"/>
                                                    </h:outputText>
                                                    <h:outputText value=" (#{ts.blockUnit}) = " styleClass="text-muted"/>
                                                </h:panelGroup>
                                                <h:panelGroup rendered="#{ts.packageLocked}">
                                                    <p:tag value="Package" severity="info" styleClass="me-1"/>
                                                </h:panelGroup>
```
with:
```xml
                                                <h:outputText value="#{ts.blocks}">
                                                    <f:convertNumber pattern="#,##0.##"/>
                                                </h:outputText>
                                                <h:outputText value=" × "/>
                                                <h:outputText value="#{ts.ratePerBlock}">
                                                    <f:convertNumber pattern="#,##0.00"/>
                                                </h:outputText>
                                                <h:outputText value=" (#{ts.blockUnit}) = " styleClass="text-muted"/>
```

- [ ] **Step 6: Confirm no remaining references**

Run: `grep -n "packageLocked" src/main/webapp/theater/patient_surgery.xhtml`
Expected: no output.

- [ ] **Step 7: Full-project compile — this is the real checkpoint for Tasks 2–9**

Run: `mvn -q compile -DskipTests`
Expected: `BUILD SUCCESS`. If it fails, the error will name a remaining caller of a deleted `fromPackage`/`isFromPackage`/`setFromPackage`/`isPackageRoomDurationExceeded`/`getPackageRoomVarianceCharge`/`packageLocked` symbol somewhere Tasks 2–9 didn't reach — the pharmacy controllers in Tasks 10–11 are the only intentionally-still-remaining callers at this point, so a compile failure here should only ever point into `InpatientDirectIssueNativeSqlController.java`, `PharmacyRequestForBhtController.java`, or `PharmacySaleBhtController.java` (not yet touched) — anything else is a real miss to go back and fix.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/SurgeryChargeSummaryController.java src/main/webapp/theater/patient_surgery.xhtml
git commit -m "feat(inward): remove package-locked room-charge branching from theatre stay charges"
```

---

### Task 10: Remove the pharmacy package-rate override — native SQL issue path

**Files:**
- Modify: `src/main/java/com/divudi/bean/pharmacy/InpatientDirectIssueNativeSqlController.java`
- Modify: `src/main/java/com/divudi/core/data/dto/BillItemData.java`
- Modify: `src/main/java/com/divudi/service/pharmacy/InpatientDirectIssueNativeSqlService.java`

This is a second, independently-discovered "package lock" — pharmacy items dispensed under a package were silently priced at a frozen package rate and blocked from substitution. It is removed for the same reason as Tasks 2–9: pharmacy items now always issue at their normal computed rate, and their real cost is what the final-bill comparison in Task 5 uses.

- [ ] **Step 1: Delete `resolvePackageAllocation`**

Delete the entire method from `InpatientDirectIssueNativeSqlController.java`:
```java
    private com.divudi.core.entity.inward.InpatientPackageItem resolvePackageAllocation(Long itemId, double requestedQty) {
        if (patientEncounter == null || patientEncounter.getInpatientPackage() == null || itemId == null) {
            return null;
        }
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("pkg", patientEncounter.getInpatientPackage());
        m.put("itemId", itemId);
        m.put("type", com.divudi.core.data.inward.InpatientPackageComponentType.PHARMACY_ITEM);
        java.util.List<com.divudi.core.entity.inward.InpatientPackageItem> matches = inpatientPackageItemFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i"
                        + " WHERE i.retired = false"
                        + " AND i.inpatientPackage = :pkg"
                        + " AND i.item.id = :itemId"
                        + " AND i.componentType = :type",
                m);
        if (matches.isEmpty()) {
            return null;
        }
        com.divudi.core.entity.inward.InpatientPackageItem packageItem = matches.get(0);

        java.util.Map<String, Object> qm = new java.util.HashMap<>();
        qm.put("pe", patientEncounter);
        qm.put("itemId", itemId);
        Double alreadyIssued = billItemFacade.findDoubleByJpql(
                "SELECT SUM(bi.qty) FROM BillItem bi"
                        + " WHERE bi.retired = false"
                        + " AND bi.fromPackage = true"
                        + " AND bi.patientEncounter = :pe"
                        + " AND bi.item.id = :itemId",
                qm);
        double consumed = alreadyIssued != null ? alreadyIssued : 0.0;

        if (billItemDataList != null) {
            for (BillItemData existing : billItemDataList) {
                if (existing.isFromPackage() && itemId.equals(existing.getItemId())) {
                    consumed += existing.getQty();
                }
            }
        }

        if (consumed + requestedQty > packageItem.getQty()) {
            return null;
        }
        return packageItem;
    }
```
(If `inpatientPackageItemFacade` was injected in this class *only* to support this method, leave the injection in place unless a subsequent grep in Step 5 shows it's now unused — remove it then if so, not before, to avoid a premature guess.)

- [ ] **Step 2: Simplify the rate-calculation block that called it**

Replace:
```java
        // Rate / value for bill line — apply inward price matrix margin and discount
        com.divudi.core.entity.inward.InpatientPackageItem packageAllocation = resolvePackageAllocation(selectedStockDto.getItemId(), qty);
        boolean isPackageRate = packageAllocation != null;
        double packageRate = isPackageRate ? packageAllocation.getFixedPrice() / packageAllocation.getQty() : 0.0;
        double lineRetailRate = isPackageRate ? packageRate : (selectedStockDto.getRetailRate() != null ? selectedStockDto.getRetailRate() : 0.0);
        double absQty = Math.abs(qty);
        double grossValue = lineRetailRate * absQty;
        double marginRate = 0.0;
        double marginValue = 0.0;
        double discountPct = 0.0;
        double discountValue = 0.0;
        if (!isPackageRate) {
            long itemId = selectedStockDto.getItemId();
            double[] marginAndDiscount = computeMarginAndDiscount(itemId, lineRetailRate, absQty);
            marginRate = marginAndDiscount[0];
            marginValue = marginAndDiscount[1];
            discountPct = marginAndDiscount[2];
            discountValue = marginAndDiscount[3];
        }
        double netRate = lineRetailRate + marginRate - (absQty > 0 ? discountValue / absQty : 0.0);
        double netValue = grossValue + marginValue - discountValue;
        bid.setRate(lineRetailRate);
        bid.setNetRate(netRate);
        bid.setDiscountPercent(discountPct);
        bid.setDiscountValue(discountValue);
        bid.setMarginValue(marginValue);
        bid.setNetValue(-netValue);
        bid.setGrossValue(-grossValue);
        bid.setFromPackage(isPackageRate);
        if (isPackageRate) {
            bid.setOverriddenRate(packageRate);
            bid.setSourcePackageItemId(packageAllocation.getId());
        }
```
with:
```java
        // Rate / value for bill line — apply inward price matrix margin and discount
        double lineRetailRate = selectedStockDto.getRetailRate() != null ? selectedStockDto.getRetailRate() : 0.0;
        double absQty = Math.abs(qty);
        double grossValue = lineRetailRate * absQty;
        long itemId = selectedStockDto.getItemId();
        double[] marginAndDiscount = computeMarginAndDiscount(itemId, lineRetailRate, absQty);
        double marginRate = marginAndDiscount[0];
        double marginValue = marginAndDiscount[1];
        double discountPct = marginAndDiscount[2];
        double discountValue = marginAndDiscount[3];
        double netRate = lineRetailRate + marginRate - (absQty > 0 ? discountValue / absQty : 0.0);
        double netValue = grossValue + marginValue - discountValue;
        bid.setRate(lineRetailRate);
        bid.setNetRate(netRate);
        bid.setDiscountPercent(discountPct);
        bid.setDiscountValue(discountValue);
        bid.setMarginValue(marginValue);
        bid.setNetValue(-netValue);
        bid.setGrossValue(-grossValue);
```

- [ ] **Step 3: Remove the substitution-blocking guards**

Replace:
```java
        if (bid.isFromPackage()) {
            JsfUtil.addErrorMessage("Package-priced items cannot be substituted.");
            return;
        }
        Item item = itemFacade.find(bid.getItemId());
```
with:
```java
        Item item = itemFacade.find(bid.getItemId());
```
Replace:
```java
        if (itemDataForSubstitution.isFromPackage()) {
            JsfUtil.addErrorMessage("Package-priced items cannot be substituted.");
            return;
        }

        StockDTO sub = selectedSubstituteStock;
```
with:
```java
        StockDTO sub = selectedSubstituteStock;
```

- [ ] **Step 4: `BillItemData.java` — remove the package-rate fields**

Replace:
```java
    // ---- Package rate override (Task 16d — inpatient package pricing) ----
    private Double overriddenRate;
    private boolean fromPackage;
    private Long sourcePackageItemId;
```
with nothing (delete these 4 lines, including the comment).

Delete the matching accessor methods (`getOverriddenRate`/`setOverriddenRate`, `isFromPackage`/`setFromPackage`, `getSourcePackageItemId`/`setSourcePackageItemId`) — locate each via `grep -n "OverriddenRate\|FromPackage\|SourcePackageItemId" src/main/java/com/divudi/core/data/dto/BillItemData.java` during implementation and delete each getter/setter pair found.

- [ ] **Step 5: `InpatientDirectIssueNativeSqlService.java` — drop the 3 columns from the native INSERT**

Replace:
```java
            em.createNativeQuery(
                "INSERT INTO " + billItemTable()
                + " (bill_ID, item_ID, qty, descreption, netValue, grossValue, netRate,"
                + " rate, marginValue, discount, discountRate,"
                + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType, referanceBillItem_ID,"
                + " overriddenRate, fromPackage, sourcePackageItem_ID)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine',?,?,?,?)")
                .setParameter(1, billId)
                .setParameter(2, d.getItemId())
                .setParameter(3, absQty)
                .setParameter(4, d.getDescription())
                .setParameter(5, absNetValue)
                .setParameter(6, absGrossValue)
                .setParameter(7, netRate)
                .setParameter(8, rate)
                .setParameter(9, marginValue)
                .setParameter(10, discountValue)
                .setParameter(11, discountRate)
                .setParameter(12, new Timestamp(createdAt.getTime()))
                .setParameter(13, d.getCreaterId())
                .setParameter(14, d.getSourceRequestBillItemId())
                .setParameter(15, d.getOverriddenRate())
                .setParameter(16, d.isFromPackage() ? 1 : 0)
                .setParameter(17, d.getSourcePackageItemId())
                .executeUpdate();
```
with:
```java
            em.createNativeQuery(
                "INSERT INTO " + billItemTable()
                + " (bill_ID, item_ID, qty, descreption, netValue, grossValue, netRate,"
                + " rate, marginValue, discount, discountRate,"
                + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType, referanceBillItem_ID)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine',?)")
                .setParameter(1, billId)
                .setParameter(2, d.getItemId())
                .setParameter(3, absQty)
                .setParameter(4, d.getDescription())
                .setParameter(5, absNetValue)
                .setParameter(6, absGrossValue)
                .setParameter(7, netRate)
                .setParameter(8, rate)
                .setParameter(9, marginValue)
                .setParameter(10, discountValue)
                .setParameter(11, discountRate)
                .setParameter(12, new Timestamp(createdAt.getTime()))
                .setParameter(13, d.getCreaterId())
                .setParameter(14, d.getSourceRequestBillItemId())
                .executeUpdate();
```

- [ ] **Step 6: Confirm no remaining references in these three files**

Run: `grep -n "fromPackage\|FromPackage\|overriddenRate\|OverriddenRate\|sourcePackageItemId\|SourcePackageItemId\|resolvePackageAllocation" src/main/java/com/divudi/bean/pharmacy/InpatientDirectIssueNativeSqlController.java src/main/java/com/divudi/core/data/dto/BillItemData.java src/main/java/com/divudi/service/pharmacy/InpatientDirectIssueNativeSqlService.java`
Expected: no output.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/InpatientDirectIssueNativeSqlController.java src/main/java/com/divudi/core/data/dto/BillItemData.java src/main/java/com/divudi/service/pharmacy/InpatientDirectIssueNativeSqlService.java
git commit -m "feat(pharmacy): remove package-rate override from native-SQL BHT direct issue"
```

---

### Task 11: Remove the pharmacy package-rate override — JPA issue paths

**Files:**
- Modify: `src/main/java/com/divudi/bean/pharmacy/PharmacyRequestForBhtController.java`
- Modify: `src/main/java/com/divudi/bean/pharmacy/PharmacySaleBhtController.java`

- [ ] **Step 1: `PharmacyRequestForBhtController` — delete `resolvePackageOverrideRate`**

Delete the entire method:
```java
    private Double resolvePackageOverrideRate(com.divudi.core.entity.Item item, double requestedQty) {
        if (patientEncounter == null || patientEncounter.getInpatientPackage() == null || item == null) {
            return null;
        }
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("pkg", patientEncounter.getInpatientPackage());
        m.put("item", item);
        m.put("type", com.divudi.core.data.inward.InpatientPackageComponentType.PHARMACY_ITEM);
        java.util.List<com.divudi.core.entity.inward.InpatientPackageItem> matches = inpatientPackageItemFacade.findByJpql(
                "SELECT i FROM InpatientPackageItem i"
                        + " WHERE i.retired = false"
                        + " AND i.inpatientPackage = :pkg"
                        + " AND i.item = :item"
                        + " AND i.componentType = :type",
                m);
        if (matches.isEmpty()) {
            return null;
        }
        com.divudi.core.entity.inward.InpatientPackageItem packageItem = matches.get(0);

        java.util.Map<String, Object> qm = new java.util.HashMap<>();
        qm.put("pe", patientEncounter);
        qm.put("item", item);
        Double alreadyIssued = getBillItemFacade().findDoubleByJpql(
                "SELECT SUM(bi.qty) FROM BillItem bi"
                        + " WHERE bi.retired = false"
                        + " AND bi.fromPackage = true"
                        + " AND bi.patientEncounter = :pe"
                        + " AND bi.item = :item",
                qm);
        double consumed = alreadyIssued != null ? alreadyIssued : 0.0;

        if (getPreBill() != null && getPreBill().getBillItems() != null) {
            for (BillItem existing : getPreBill().getBillItems()) {
                if (existing.getId() == null && existing.isFromPackage() && item.equals(existing.getItem())) {
                    consumed += existing.getQty() != null ? existing.getQty() : 0.0;
                }
            }
        }

        if (consumed + requestedQty > packageItem.getQty()) {
            return null; // Beyond allocation — bill remaining/extra qty at live rate.
        }

        return packageItem.getFixedPrice() / packageItem.getQty();
    }
```

- [ ] **Step 2: Remove its call site**

Replace:
```java
        Double packageRate = resolvePackageOverrideRate(newBillItem.getItem(), getQty());
        if (packageRate != null) {
            newBillItem.setOverriddenRate(packageRate);
            newBillItem.setRate(packageRate);
            newBillItem.setFromPackage(true);
        }

        newBillItem.setSearialNo(getPreBill().getBillItems().size() + 1);
```
with:
```java
        newBillItem.setSearialNo(getPreBill().getBillItems().size() + 1);
```

- [ ] **Step 3: Confirm no remaining references**

Run: `grep -n "fromPackage\|resolvePackageOverrideRate" src/main/java/com/divudi/bean/pharmacy/PharmacyRequestForBhtController.java`
Expected: no output. (If `inpatientPackageItemFacade` is now unused in this file, leave its injection in place unless it causes an actual compile warning treated as an error — this codebase does not fail the build on unused-field warnings.)

- [ ] **Step 4: `PharmacySaleBhtController` — remove the package-rate branch in `calculateRates`**

Replace:
```java
    public void calculateRates(BillItem bi) {
        if (bi == null || bi.getPharmaceuticalBillItem() == null || bi.getItem() == null) {
            return;
        }

        if (bi.isFromPackage() && bi.getOverriddenRate() != null) {
            double packageRate = bi.getOverriddenRate();
            double quantity = bi.getQty() != null ? bi.getQty() : 0.0;
            bi.setRate(packageRate);
            bi.setGrossValue(packageRate * quantity);
            bi.setMarginValue(0.0);
            bi.setNetValue(packageRate * quantity);
            bi.setMarginRate(0.0);
            bi.setNetRate(packageRate);
```
Read the full body of this `if` block during implementation (only its opening lines are quoted here — the plan's earlier research did not capture its closing brace) and delete the entire `if (bi.isFromPackage() && bi.getOverriddenRate() != null) { ... }` block, leaving `calculateRates` to fall through directly to whatever normal-rate calculation logic follows it unconditionally.

- [ ] **Step 5: Remove the two `fromPackage` propagation sites**

Replace (first occurrence, in the return/reversal bill-item builder):
```java
                    billItem.setReferanceBillItem(i);
                    if (i.isFromPackage()) {
                        billItem.setFromPackage(true);
                        billItem.setOverriddenRate(i.getOverriddenRate());
                        billItem.setSourcePackageItem(i.getSourcePackageItem());
                    }
                    billItem.setSearialNo(getBillItems().size() + 1);
```
with:
```java
                    billItem.setReferanceBillItem(i);
                    billItem.setSearialNo(getBillItems().size() + 1);
```
Replace (second occurrence, in the partial-issue builder):
```java
                billItem.setReferanceBillItem(i);
                if (i.isFromPackage()) {
                    billItem.setFromPackage(true);
                    billItem.setOverriddenRate(i.getOverriddenRate());
                    billItem.setSourcePackageItem(i.getSourcePackageItem());
                }
                billItem.setSearialNo(getBillItems().size() + 1);
```
with:
```java
                billItem.setReferanceBillItem(i);
                billItem.setSearialNo(getBillItems().size() + 1);
```

- [ ] **Step 6: Confirm no remaining references**

Run: `grep -n "isFromPackage\|setFromPackage\|OverriddenRate" src/main/java/com/divudi/bean/pharmacy/PharmacySaleBhtController.java`
Expected: no output. (If this file used `getOverriddenRate`/`setOverriddenRate` for anything unrelated to package rates elsewhere, the grep will show it — only remove what's clearly part of the package-rate branch just deleted; if something unexpected shows up, stop and check it by hand rather than deleting blindly.)

- [ ] **Step 7: Full-project compile**

Run: `mvn -q compile -DskipTests`
Expected: `BUILD SUCCESS`. This is the real end-to-end compile checkpoint for the entire `fromPackage` removal across all 11 prior tasks.

- [ ] **Step 8: Run the full existing test suite**

Run: `mvn -q test`
Expected: all tests pass, including the 13 in `InpatientPackagePricingTest` and `PackageChangeControllerTest` (unaffected by this plan) from prior work.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/PharmacyRequestForBhtController.java src/main/java/com/divudi/bean/pharmacy/PharmacySaleBhtController.java
git commit -m "feat(pharmacy): remove package-rate override from BHT request and sale issue paths"
```

---

### Task 12: Build, redeploy, and end-to-end verification

**Files:** none (verification only)

- [ ] **Step 1: Full build**

Run: `mvn clean package -DskipTests`
Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Local redeploy**

Follow `playwright-e2e` skill §0a to redeploy the built WAR to local Payara.

- [ ] **Step 3: Package-config screen check (Playwright, never by URL)**

Navigate **Administration → Manage Inpatient Services → Packages → Manage Inpatient Packages**, open a package's "Charge Type Amounts" table, confirm `PackageExcessCharges` does **not** appear as a settable row.

- [ ] **Step 4: Over-budget package scenario**

Using the existing "Package Admission" entry point (Admissions submenu), admit a test patient under a package. During the stay, order real services/room time/professional fees whose combined total for the package's covered categories deliberately exceeds the package's `totalPrice`. Reach the final bill (record the menu path). Confirm: each package-covered category shows the package's allocated amount (not the real itemized total), and a `PackageExcessCharges` row appears with the correct excess (real combined total − package total).

- [ ] **Step 5: Under-budget package scenario + discount**

Repeat with real usage deliberately less than the package price. Confirm no `PackageExcessCharges` row appears, the covered categories still show their full package-allocated amounts, and entering a charge-type discount on one of those rows (existing UI, no new code) works exactly as it does for a non-package bill.

- [ ] **Step 6: Settle both bills, verify DB**

For each of Steps 4-5's admission, settle the final bill. Query the local DB: confirm a `BillItem` row exists with `inwardChargeType = 'PackageExcessCharges'` and `item IS NULL` for the over-budget case, and confirm `Bill.grantTotal` matches the on-screen total for both.

- [ ] **Step 7: Cancel one settled package bill**

Cancel the over-budget bill from Step 6. Verify in the DB that the contra-bill includes a correctly-inverted `PackageExcessCharges` `BillItem` alongside every other reversed row.

- [ ] **Step 8: Non-package regression**

Pick an existing non-package admission with real charges already on it (query the local DB for one, per this project's local-testing-environment convention — no need to ask). Settle its final bill; record `grantTotal` and every `ChargeItemTotal`/`BillItem` row. This is the "after" half of the regression check — since this admission never had a package, its "before" behavior is simply whatever `development` already produces for it today, which this plan's `if (inpatientPackage != null)` gating guarantees is unchanged. Confirm no `PackageExcessCharges` row appears and every other row/total is exactly what a manual read of the admission's real charges would predict.

- [ ] **Step 9: Component-still-auto-creates check**

Attach at least one component (a Service, a Professional Fee role) to a test package, admit a patient under it, and confirm on `inward_bill_intrim.xhtml`/the admission's service list that a bill row was auto-created for that component — and that it can be edited or removed like any other row (no "cannot be removed" error).

- [ ] **Step 10: Pharmacy — no package-rate override**

Configure a `PHARMACY_ITEM` component on a test package with a fixed price notably different from the medicine's real stock rate. Issue that medicine to the package admission via BHT direct issue. Confirm the issued `BillItem`'s rate is the medicine's normal computed rate (stock/margin/discount as usual), not the package's configured `fixedPrice` — and confirm the substitution action is available for it (not blocked).
