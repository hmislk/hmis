# Native Purchase Order Request Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the JPA-entity write path of the Purchase Order Request page
(create → repeated draft save → item CRUD → finalize → email) with a native-SQL
implementation, with 100% functional parity with `PurchaseOrderRequestController`.

**Architecture:** New `PurchaseOrderRequestNativeSqlService` (`@Stateless`) does native
`INSERT`/`UPDATE` for `bill`, `billitem`, `pharmaceuticalbillitem` (all `IDENTITY`-PK
tables, matching `RetailSaleNativeSqlService`'s `INSERT` + `SELECT LAST_INSERT_ID()`
pattern). `BillItemFinanceDetails` (also `IDENTITY` PK but carrying business-rule-heavy
calculation logic) stays JPA `persist`/`merge`, per the retail-sale precedent. A new
`PurchaseOrderRequestNativeSqlController` (`@Named @SessionScoped`, matching the scope
of every other controller in this bill family) owns validation, item-list state, rate
lookups (reused verbatim — already JPQL, not raw entity walking), and email — all
unchanged from the legacy controller, since none of that is the EAGER-cascade cost
this migration targets.

**Tech Stack:** Java 11, JPA/EclipseLink (`hmisPU` persistence unit), JSF 2.x +
PrimeFaces, MySQL (via native SQL for the hot write path).

## Global Constraints

- **NO MOCK DATA** — every task operates against real entities/tables.
- **NEVER MODIFY EXISTING CONSTRUCTORS** — only add new ones; the legacy
  `PurchaseOrderRequestController` and its collaborators (`BillFacade`,
  `BillItemFacade`, `PharmaceuticalBillItemFacade`) must not change signatures.
- **JPQL FIRST, NATIVE SQL LAST** — native SQL is used only for the `bill`/`billitem`/
  `pharmaceuticalbillitem` write path per the approved design spec's performance
  rationale; every other query (rate lookups, item search, email) stays JPQL/JPA.
- **`findLongByJpql` for COUNT queries** — n/a to this plan (no new COUNT queries), but
  any added later must follow this rule.
- Table names must be resolved case-insensitively via `INFORMATION_SCHEMA.TABLES`
  (`resolveTable()` pattern from `RetailSaleNativeSqlService`), never hardcoded upper
  or lower case — per `developer_docs/database/migration-development-guide.md` and
  this codebase's existing native services.
- After every native `INSERT`/`UPDATE`, evict `Bill`, `BillItem`,
  `PharmaceuticalBillItem` from the EclipseLink L2 cache.
- Persistence.xml JNDI: leave local (`jdbc/coop`/`jdbc/ruhunuAudit`) during
  development; restore CI/CD placeholders only immediately after `git push` (handled
  outside this plan, per CLAUDE.md).
- Design spec: `docs/superpowers/specs/2026-08-07-po-request-native-design.md` —
  every task below implements a piece of it; do not deviate from its scope boundary
  (only `bill`/`billitem`/`pharmaceuticalbillitem` are touched; no BIFD/BFD-table
  native writes, no stock/GRN involvement at this phase).

---

## File Structure

| File | Responsibility |
|---|---|
| `src/main/java/com/divudi/core/data/dto/pharmacy/PurchaseOrderRequestLineData.java` (new) | Plain input DTO carrying one line's data from controller to service (item id, qty, free qty, purchase rate, retail rate, units-per-pack, existing `BillItem`/`PharmaceuticalBillItem` id if editing) |
| `src/main/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlService.java` (new) | `@Stateless` — native SQL create/update draft bill, native SQL insert/update line items, in-place finalize promotion, zero-qty line retirement |
| `src/main/java/com/divudi/bean/pharmacy/PurchaseOrderRequestNativeSqlController.java` (new) | `@Named @SessionScoped` — page-backing bean: item list state, add/remove/edit, rate lookups (ported verbatim from legacy), bulk-add, email, navigation |
| `src/main/webapp/pharmacy/pharmacy_purhcase_order_request_native.xhtml` (new) | Copy of legacy page layout, EL bindings repointed to the new controller |
| `src/main/webapp/pharmacy/pharmacy_purhcase_order_list_to_finalize.xhtml` (modify) | Repoint "Edit" action from `purchaseOrderRequestController.navigateToUpdatePurchaseOrder()` to the new controller's equivalent |
| `src/test/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlServiceTest.java` (new) | Unit/integration tests for the native service |

---

## Task 1: `PurchaseOrderRequestLineData` DTO

**Files:**
- Create: `src/main/java/com/divudi/core/data/dto/pharmacy/PurchaseOrderRequestLineData.java`
- Test: `src/test/java/com/divudi/core/data/dto/pharmacy/PurchaseOrderRequestLineDataTest.java`

**Interfaces:**
- Consumes: nothing (plain data holder)
- Produces: `PurchaseOrderRequestLineData` with getters/setters for:
  `Long billItemId` (null = new line), `Long pharmaceuticalBillItemId` (null = new),
  `Long itemId`, `boolean isAmpp`, `BigDecimal quantity`, `BigDecimal freeQuantity`,
  `BigDecimal purchaseRate`, `BigDecimal retailRate`, `BigDecimal unitsPerPack`,
  `int serialNo`, `Long createrId`. Later tasks (2, 3) consume these exact field names.

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.core.data.dto.pharmacy;

import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.assertEquals;

public class PurchaseOrderRequestLineDataTest {
    @Test
    public void gettersReturnValuesSetByConstructorArgs() {
        PurchaseOrderRequestLineData d = new PurchaseOrderRequestLineData();
        d.setBillItemId(5L);
        d.setPharmaceuticalBillItemId(7L);
        d.setItemId(100L);
        d.setAmpp(true);
        d.setQuantity(BigDecimal.TEN);
        d.setFreeQuantity(BigDecimal.ONE);
        d.setPurchaseRate(BigDecimal.valueOf(12.5));
        d.setRetailRate(BigDecimal.valueOf(15.0));
        d.setUnitsPerPack(BigDecimal.valueOf(10));
        d.setSerialNo(2);
        d.setCreaterId(1L);

        assertEquals(Long.valueOf(5L), d.getBillItemId());
        assertEquals(Long.valueOf(7L), d.getPharmaceuticalBillItemId());
        assertEquals(Long.valueOf(100L), d.getItemId());
        assertEquals(true, d.isAmpp());
        assertEquals(BigDecimal.TEN, d.getQuantity());
        assertEquals(BigDecimal.ONE, d.getFreeQuantity());
        assertEquals(BigDecimal.valueOf(12.5), d.getPurchaseRate());
        assertEquals(BigDecimal.valueOf(15.0), d.getRetailRate());
        assertEquals(BigDecimal.valueOf(10), d.getUnitsPerPack());
        assertEquals(2, d.getSerialNo());
        assertEquals(Long.valueOf(1L), d.getCreaterId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=PurchaseOrderRequestLineDataTest test`
Expected: FAIL — compile error, class does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;
import java.math.BigDecimal;

public class PurchaseOrderRequestLineData implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long billItemId;
    private Long pharmaceuticalBillItemId;
    private Long itemId;
    private boolean ampp;
    private BigDecimal quantity;
    private BigDecimal freeQuantity;
    private BigDecimal purchaseRate;
    private BigDecimal retailRate;
    private BigDecimal unitsPerPack;
    private int serialNo;
    private Long createrId;

    public Long getBillItemId() { return billItemId; }
    public void setBillItemId(Long billItemId) { this.billItemId = billItemId; }

    public Long getPharmaceuticalBillItemId() { return pharmaceuticalBillItemId; }
    public void setPharmaceuticalBillItemId(Long pharmaceuticalBillItemId) { this.pharmaceuticalBillItemId = pharmaceuticalBillItemId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public boolean isAmpp() { return ampp; }
    public void setAmpp(boolean ampp) { this.ampp = ampp; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getFreeQuantity() { return freeQuantity; }
    public void setFreeQuantity(BigDecimal freeQuantity) { this.freeQuantity = freeQuantity; }

    public BigDecimal getPurchaseRate() { return purchaseRate; }
    public void setPurchaseRate(BigDecimal purchaseRate) { this.purchaseRate = purchaseRate; }

    public BigDecimal getRetailRate() { return retailRate; }
    public void setRetailRate(BigDecimal retailRate) { this.retailRate = retailRate; }

    public BigDecimal getUnitsPerPack() { return unitsPerPack; }
    public void setUnitsPerPack(BigDecimal unitsPerPack) { this.unitsPerPack = unitsPerPack; }

    public int getSerialNo() { return serialNo; }
    public void setSerialNo(int serialNo) { this.serialNo = serialNo; }

    public Long getCreaterId() { return createrId; }
    public void setCreaterId(Long createrId) { this.createrId = createrId; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=PurchaseOrderRequestLineDataTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/data/dto/pharmacy/PurchaseOrderRequestLineData.java src/test/java/com/divudi/core/data/dto/pharmacy/PurchaseOrderRequestLineDataTest.java
git commit -m "feat(pharmacy): add PurchaseOrderRequestLineData DTO for native PO request writes"
```

---

## Task 2: `PurchaseOrderRequestNativeSqlService` — draft bill create/update

**Files:**
- Create: `src/main/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlService.java`

**Interfaces:**
- Consumes: `PurchaseOrderRequestLineData` (Task 1)
- Produces:
  - `long createDraftBill(long departmentId, long institutionId, long createrId, String deptId, String insId)` →
    returns new bill id, `BILLTYPEATOMIC='PHARMACY_ORDER_PRE'`, `checked=0`
  - `void updateDraftBillHeader(long billId, Long toInstitutionId, PaymentMethod paymentMethod, int creditDuration, boolean consignment, DepartmentType departmentType, Long editorId)`
  - `boolean isBillChecked(long billId)` — used by controller guards
  - Later tasks (3, 4) call these exact method names/signatures.

**Codebase testing convention for native SQL services (verified against
`BhtIssueRequestNativeSqlServiceTest`/`BhtIssueRequestNativeSqlService`, the only
existing test precedent for a `@Stateless` native SQL service in this package):
there is no EntityManager/database test harness for these services anywhere in
the codebase. Only package-private, EntityManager-free pure logic gets a JUnit
test. Methods that call `em.createNativeQuery(...)` are NOT unit tested — they
are verified via Task 11's Playwright + manual DB-query pass instead.** Do not
invent a test container or in-memory persistence unit; none exists here.

This task's `createDraftBill`/`updateDraftBillHeader`/`isBillChecked` all touch
`em` directly and have no extractable pure logic (no branching on plain
values) — per the convention above, they get no JUnit test in this task. The
package-private `resolveTable()` table-name-resolution helper also touches
`em` and is not unit tested for the same reason (matches
`BhtIssueRequestNativeSqlService`, whose own `resolveTable`-equivalent has no
test — only its return-nothing pure helpers like `str`/`toBool`/`toDate` do).

- [ ] **Step 1: Write the implementation directly (no test-first step — see
  convention note above; this task has no pure logic to isolate)**

```java
package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Native SQL write path for the Purchase Order Request draft bill.
 * Only bill / billitem / pharmaceuticalbillitem are touched here —
 * BillItemFinanceDetails stays JPA (IDENTITY PK, calculation-heavy),
 * matching RetailSaleNativeSqlService's split.
 * Related issue: #22727
 */
@Stateless
public class PurchaseOrderRequestNativeSqlService {

    private static final Logger LOGGER = Logger.getLogger(PurchaseOrderRequestNativeSqlService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    private String tBill;
    private String tBillItem;
    private String tPharmBillItem;

    public long createDraftBill(long departmentId, long institutionId, long createrId, String deptId, String insId) {
        Date now = new Date();
        em.createNativeQuery(
            "INSERT INTO " + billTable()
            + " (BILLTYPEATOMIC, billType, department_ID, institution_ID, fromDepartment_ID, fromInstitution_ID,"
            + " creater_ID, createdAt, checked, retired, cancelled, deptId, insId, netTotal, total)"
            + " VALUES (?,?,?,?,?,?,?,?,0,0,0,?,?,0,0)")
            .setParameter(1, BillTypeAtomic.PHARMACY_ORDER_PRE.toString())
            .setParameter(2, BillType.PharmacyOrder.toString())
            .setParameter(3, departmentId)
            .setParameter(4, institutionId)
            .setParameter(5, departmentId)
            .setParameter(6, institutionId)
            .setParameter(7, createrId)
            .setParameter(8, new Timestamp(now.getTime()))
            .setParameter(9, deptId)
            .setParameter(10, insId)
            .executeUpdate();
        long billId = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        evictCache();
        return billId;
    }

    public void updateDraftBillHeader(long billId, Long toInstitutionId, PaymentMethod paymentMethod,
                                       int creditDuration, boolean consignment, DepartmentType departmentType,
                                       Long editorId) {
        em.createNativeQuery(
            "UPDATE " + billTable()
            + " SET toInstitution_ID=?, paymentMethod=?, creditDuration=?, consignment=?,"
            + " departmentType=?, editor_ID=?, editedAt=?"
            + " WHERE ID=?")
            .setParameter(1, toInstitutionId)
            .setParameter(2, paymentMethod != null ? paymentMethod.toString() : null)
            .setParameter(3, creditDuration)
            .setParameter(4, consignment ? 1 : 0)
            .setParameter(5, departmentType != null ? departmentType.toString() : null)
            .setParameter(6, editorId)
            .setParameter(7, new Timestamp(new Date().getTime()))
            .setParameter(8, billId)
            .executeUpdate();
        evictCache();
    }

    public boolean isBillChecked(long billId) {
        Object result = em.createNativeQuery(
            "SELECT checked FROM " + billTable() + " WHERE ID=?")
            .setParameter(1, billId)
            .getSingleResult();
        if (result instanceof Boolean) return (Boolean) result;
        if (result instanceof Number) return ((Number) result).intValue() != 0;
        return false;
    }

    private void evictCache() {
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(com.divudi.core.entity.Bill.class);
    }

    private String resolveTable(String upperName) {
        Object name = em.createNativeQuery(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
            + "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = ? LIMIT 1")
            .setParameter(1, upperName)
            .getSingleResult();
        return name.toString();
    }

    private String billTable() {
        if (tBill == null) tBill = resolveTable("BILL");
        return tBill;
    }

    private String billItemTable() {
        if (tBillItem == null) tBillItem = resolveTable("BILLITEM");
        return tBillItem;
    }

    private String pharmBillItemTable() {
        if (tPharmBillItem == null) tPharmBillItem = resolveTable("PHARMACEUTICALBILLITEM");
        return tPharmBillItem;
    }
}
```

- [ ] **Step 2: Compile check**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlService.java
git commit -m "feat(pharmacy): native SQL draft-bill create/update for PO Request"
```

---

## Task 3: `PurchaseOrderRequestNativeSqlService` — line item insert/update + BIFD

**Files:**
- Modify: `src/main/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlService.java`
- Create: `src/test/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlServiceTest.java`

**Interfaces:**
- Consumes: `PurchaseOrderRequestLineData` (Task 1), `billItemTable`/`pharmBillItemTable` (Task 2)
- Produces:
  - `long saveLine(long billId, PurchaseOrderRequestLineData line)` — inserts or
    updates `billitem` + `pharmaceuticalbillitem` natively, persists/merges
    `BillItemFinanceDetails` via JPA, returns the `billitem` id. Task 4 (finalize) and
    the controller (Task 5) call this for every line on every save.
  - `LineValues computeLineValues(PurchaseOrderRequestLineData line)` — package-private,
    pure (no `em` access), extracts the calculation math from legacy
    `calculateLineValues()` into a testable unit. `LineValues` is a package-private
    static nested class on the service exposing (all `BigDecimal`, all
    package-private final fields with a package-private constructor, matching
    `BhtIssueRequestNativeSqlServiceTest`'s convention of testing package-private
    logic directly, same package, no getters needed): `qty, freeQty, purchaseRate,
    retailRate, unitsPerPack, grossValue, netValue, purchaseValue, retailValue,
    netRate, pbiQty (double), pbiFreeQty (double), pbiPurchaseRate (double),
    pbiRetailRate (double)`. `saveLine()` calls `computeLineValues()` first, then
    uses the returned values for both the native SQL and the BIFD JPA write —
    no duplicated math between the two.

**Codebase testing convention (see Task 2's note):** only pure logic gets a
JUnit test. `saveLine()` itself touches `em` and is not unit tested — verified
via Task 11's Playwright + DB pass. `computeLineValues()` has no `em` access and
mirrors legacy `calculateLineValues()`'s branching (AMPP vs non-AMPP, zero-qty
netRate guard) — it gets a real test here, following
`BhtIssueRequestNativeSqlServiceTest`'s pattern of testing package-private pure
methods directly via `new PurchaseOrderRequestNativeSqlService()` with no mocks.

- [ ] **Step 1: Write the failing test**

```java
package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurchaseOrderRequestNativeSqlServiceTest {

    private final PurchaseOrderRequestNativeSqlService service = new PurchaseOrderRequestNativeSqlService();

    @Test
    void computeLineValues_nonAmpp_purchaseRateTimesQtyIsGrossAndNetValue() {
        PurchaseOrderRequestLineData line = new PurchaseOrderRequestLineData();
        line.setAmpp(false);
        line.setQuantity(new BigDecimal("10"));
        line.setFreeQuantity(new BigDecimal("1"));
        line.setPurchaseRate(new BigDecimal("25.50"));
        line.setRetailRate(new BigDecimal("30.00"));
        line.setUnitsPerPack(BigDecimal.ONE);

        PurchaseOrderRequestNativeSqlService.LineValues v = service.computeLineValues(line);

        assertEquals(new BigDecimal("255.00"), v.grossValue.setScale(2));
        assertEquals(new BigDecimal("255.00"), v.netValue.setScale(2));
        assertEquals(new BigDecimal("280.50"), v.purchaseValue.setScale(2)); // 25.50 * (10+1)
        assertEquals(new BigDecimal("330.00"), v.retailValue.setScale(2));  // 30.00 * (10+1)
        assertEquals(25.50, v.pbiPurchaseRate, 0.0001); // non-AMPP: no pack conversion
        assertEquals(10.0, v.pbiQty, 0.0001);
    }

    @Test
    void computeLineValues_ampp_convertsQtyAndRateByUnitsPerPack() {
        PurchaseOrderRequestLineData line = new PurchaseOrderRequestLineData();
        line.setAmpp(true);
        line.setQuantity(new BigDecimal("2")); // 2 packs
        line.setFreeQuantity(BigDecimal.ZERO);
        line.setPurchaseRate(new BigDecimal("100")); // rate per pack
        line.setRetailRate(new BigDecimal("120"));
        line.setUnitsPerPack(new BigDecimal("10")); // 10 units per pack

        PurchaseOrderRequestNativeSqlService.LineValues v = service.computeLineValues(line);

        assertEquals(20.0, v.pbiQty, 0.0001); // 2 packs * 10 units/pack
        assertEquals(10.0, v.pbiPurchaseRate, 0.0001); // 100 / 10 units per pack
        assertEquals(12.0, v.pbiRetailRate, 0.0001); // 120 / 10
    }

    @Test
    void computeLineValues_zeroQuantity_netRateIsZeroNotDivideByZero() {
        PurchaseOrderRequestLineData line = new PurchaseOrderRequestLineData();
        line.setAmpp(false);
        line.setQuantity(BigDecimal.ZERO);
        line.setFreeQuantity(BigDecimal.ZERO);
        line.setPurchaseRate(new BigDecimal("50"));
        line.setRetailRate(new BigDecimal("60"));
        line.setUnitsPerPack(BigDecimal.ONE);

        PurchaseOrderRequestNativeSqlService.LineValues v = service.computeLineValues(line);

        assertEquals(BigDecimal.ZERO.setScale(2), v.netRate.setScale(2));
    }

    @Test
    void computeLineValues_nullFields_defaultToZeroOrOne() {
        PurchaseOrderRequestLineData line = new PurchaseOrderRequestLineData();
        // quantity, freeQuantity, purchaseRate, retailRate, unitsPerPack all left null

        PurchaseOrderRequestNativeSqlService.LineValues v = service.computeLineValues(line);

        assertEquals(BigDecimal.ZERO.setScale(2), v.grossValue.setScale(2));
        assertEquals(BigDecimal.ONE, v.unitsPerPack);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=PurchaseOrderRequestNativeSqlServiceTest test`
Expected: FAIL — `computeLineValues`/`LineValues` do not exist.

- [ ] **Step 3: Write minimal implementation**

Add to `PurchaseOrderRequestNativeSqlService`:

```java
import com.divudi.core.entity.BillItemFinanceDetails;
import java.math.BigDecimal;

// ... inside the class ...

/**
 * Pure calculation extracted from legacy calculateLineValues() — no em access,
 * unit tested directly. AMPP items store rates per-pack on BillItemFinanceDetails
 * but per-unit on PharmaceuticalBillItem; non-AMPP items use the same rate for both.
 */
static final class LineValues {
    final BigDecimal qty, freeQty, purchaseRate, retailRate, unitsPerPack;
    final BigDecimal grossValue, netValue, purchaseValue, retailValue, netRate;
    final double pbiQty, pbiFreeQty, pbiPurchaseRate, pbiRetailRate;

    LineValues(BigDecimal qty, BigDecimal freeQty, BigDecimal purchaseRate, BigDecimal retailRate,
               BigDecimal unitsPerPack, BigDecimal grossValue, BigDecimal netValue,
               BigDecimal purchaseValue, BigDecimal retailValue, BigDecimal netRate,
               double pbiQty, double pbiFreeQty, double pbiPurchaseRate, double pbiRetailRate) {
        this.qty = qty; this.freeQty = freeQty; this.purchaseRate = purchaseRate; this.retailRate = retailRate;
        this.unitsPerPack = unitsPerPack; this.grossValue = grossValue; this.netValue = netValue;
        this.purchaseValue = purchaseValue; this.retailValue = retailValue; this.netRate = netRate;
        this.pbiQty = pbiQty; this.pbiFreeQty = pbiFreeQty;
        this.pbiPurchaseRate = pbiPurchaseRate; this.pbiRetailRate = pbiRetailRate;
    }
}

LineValues computeLineValues(com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData line) {
    BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
    BigDecimal freeQty = line.getFreeQuantity() != null ? line.getFreeQuantity() : BigDecimal.ZERO;
    BigDecimal purchaseRate = line.getPurchaseRate() != null ? line.getPurchaseRate() : BigDecimal.ZERO;
    BigDecimal retailRate = line.getRetailRate() != null ? line.getRetailRate() : BigDecimal.ZERO;
    BigDecimal unitsPerPack = (line.getUnitsPerPack() != null && line.getUnitsPerPack().doubleValue() > 0)
            ? line.getUnitsPerPack() : BigDecimal.ONE;

    BigDecimal grossValue = purchaseRate.multiply(qty);
    BigDecimal netValue = grossValue;
    BigDecimal purchaseValue = purchaseRate.multiply(qty.add(freeQty));
    BigDecimal retailValue = retailRate.multiply(qty.add(freeQty));
    BigDecimal netRate = qty.doubleValue() > 0
            ? netValue.divide(qty, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

    double pbiQty = line.isAmpp() ? qty.doubleValue() * unitsPerPack.doubleValue() : qty.doubleValue();
    double pbiFreeQty = line.isAmpp() ? freeQty.doubleValue() * unitsPerPack.doubleValue() : freeQty.doubleValue();
    double pbiPurchaseRate = line.isAmpp() ? purchaseRate.doubleValue() / unitsPerPack.doubleValue() : purchaseRate.doubleValue();
    double pbiRetailRate = line.isAmpp() ? retailRate.doubleValue() / unitsPerPack.doubleValue() : retailRate.doubleValue();

    return new LineValues(qty, freeQty, purchaseRate, retailRate, unitsPerPack, grossValue, netValue,
            purchaseValue, retailValue, netRate, pbiQty, pbiFreeQty, pbiPurchaseRate, pbiRetailRate);
}

public long saveLine(long billId, com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData line) {
    LineValues v = computeLineValues(line);
    BigDecimal qty = v.qty, freeQty = v.freeQty, purchaseRate = v.purchaseRate, retailRate = v.retailRate;
    BigDecimal grossValue = v.grossValue, netValue = v.netValue, netRate = v.netRate;
    BigDecimal purchaseValue = v.purchaseValue, retailValue = v.retailValue, unitsPerPack = v.unitsPerPack;

    long billItemId;
    if (line.getBillItemId() == null) {
        em.createNativeQuery(
            "INSERT INTO " + billItemTable()
            + " (bill_ID, item_ID, qty, netValue, grossValue, Rate, netRate,"
            + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
            + " consideredForCosting, inwardChargeType, searialNo)"
            + " VALUES (?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine',?)")
            .setParameter(1, billId)
            .setParameter(2, line.getItemId())
            .setParameter(3, qty.doubleValue())
            .setParameter(4, netValue.doubleValue())
            .setParameter(5, grossValue.doubleValue())
            .setParameter(6, purchaseRate.doubleValue())
            .setParameter(7, netRate.doubleValue())
            .setParameter(8, new Timestamp(new Date().getTime()))
            .setParameter(9, line.getCreaterId())
            .setParameter(10, line.getSerialNo())
            .executeUpdate();
        billItemId = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    } else {
        billItemId = line.getBillItemId();
        em.createNativeQuery(
            "UPDATE " + billItemTable()
            + " SET qty=?, netValue=?, grossValue=?, Rate=?, netRate=? WHERE ID=?")
            .setParameter(1, qty.doubleValue())
            .setParameter(2, netValue.doubleValue())
            .setParameter(3, grossValue.doubleValue())
            .setParameter(4, purchaseRate.doubleValue())
            .setParameter(5, netRate.doubleValue())
            .setParameter(6, billItemId)
            .executeUpdate();
    }

    double pbiQty = v.pbiQty, pbiFreeQty = v.pbiFreeQty, pbiPurchaseRate = v.pbiPurchaseRate, pbiRetailRate = v.pbiRetailRate;

    if (line.getPharmaceuticalBillItemId() == null) {
        em.createNativeQuery(
            "INSERT INTO " + pharmBillItemTable()
            + " (billItem_ID, qty, freeQty, purchaseRate, purchaseRatePack, purchaseValue,"
            + " retailRate, retailRatePack, retailRateInUnit, retailValue, costRate, costRatePack, costValue,"
            + " createdAt, creater_ID)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,0,0,0,?,?)")
            .setParameter(1, billItemId)
            .setParameter(2, pbiQty)
            .setParameter(3, pbiFreeQty)
            .setParameter(4, pbiPurchaseRate)
            .setParameter(5, purchaseRate.doubleValue())
            .setParameter(6, purchaseValue.doubleValue())
            .setParameter(7, pbiRetailRate)
            .setParameter(8, retailRate.doubleValue())
            .setParameter(9, pbiRetailRate)
            .setParameter(10, retailValue.doubleValue())
            .setParameter(11, new Timestamp(new Date().getTime()))
            .setParameter(12, line.getCreaterId())
            .executeUpdate();
    } else {
        em.createNativeQuery(
            "UPDATE " + pharmBillItemTable()
            + " SET qty=?, freeQty=?, purchaseRate=?, purchaseRatePack=?, purchaseValue=?,"
            + " retailRate=?, retailRatePack=?, retailRateInUnit=?, retailValue=? WHERE billItem_ID=?")
            .setParameter(1, pbiQty)
            .setParameter(2, pbiFreeQty)
            .setParameter(3, pbiPurchaseRate)
            .setParameter(4, purchaseRate.doubleValue())
            .setParameter(5, purchaseValue.doubleValue())
            .setParameter(6, pbiRetailRate)
            .setParameter(7, retailRate.doubleValue())
            .setParameter(8, pbiRetailRate)
            .setParameter(9, retailValue.doubleValue())
            .setParameter(10, billItemId)
            .executeUpdate();
    }

    saveBillItemFinanceDetails(billItemId, line, qty, freeQty, purchaseRate, retailRate, netValue, grossValue,
            netRate, unitsPerPack, pbiQty, pbiFreeQty);

    evictLineCache();
    return billItemId;
}

private void saveBillItemFinanceDetails(long billItemId, com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData line,
        BigDecimal qty, BigDecimal freeQty, BigDecimal purchaseRate, BigDecimal retailRate,
        BigDecimal netValue, BigDecimal grossValue, BigDecimal netRate, BigDecimal unitsPerPack,
        double quantityByUnits, double freeQuantityByUnits) {
    BillItemFinanceDetails bifd;
    if (line.getBillItemId() != null) {
        String jpql = "SELECT bi.billItemFinanceDetails FROM BillItem bi WHERE bi.id = :id";
        java.util.List<BillItemFinanceDetails> existing = em.createQuery(jpql, BillItemFinanceDetails.class)
                .setParameter("id", billItemId)
                .getResultList();
        bifd = (existing != null && !existing.isEmpty() && existing.get(0) != null) ? existing.get(0) : new BillItemFinanceDetails();
    } else {
        bifd = new BillItemFinanceDetails();
    }

    bifd.setLineNetRate(purchaseRate);
    bifd.setLineGrossRate(purchaseRate);
    bifd.setGrossRate(purchaseRate);
    bifd.setLineNetTotal(netValue);
    bifd.setLineGrossTotal(grossValue);
    bifd.setGrossTotal(grossValue);
    bifd.setQuantity(qty);
    bifd.setFreeQuantity(freeQty);
    bifd.setQuantityByUnits(BigDecimal.valueOf(quantityByUnits));
    bifd.setFreeQuantityByUnits(BigDecimal.valueOf(freeQuantityByUnits));
    bifd.setRetailSaleRate(retailRate);
    bifd.setUnitsPerPack(unitsPerPack);
    bifd.setValueAtPurchaseRate(purchaseRate.multiply(qty.add(freeQty)));
    bifd.setValueAtRetailRate(retailRate.multiply(qty.add(freeQty)));
    bifd.setLineCost(BigDecimal.ZERO);
    bifd.setLineCostRate(BigDecimal.ZERO);
    bifd.setValueAtCostRate(BigDecimal.ZERO);

    if (bifd.getId() == null) {
        bifd.setCreatedAt(new Date());
        em.persist(bifd);
        em.flush();
        em.createNativeQuery("UPDATE " + billItemTable() + " SET BILLITEMFINANCEDETAILS_ID=? WHERE ID=?")
                .setParameter(1, bifd.getId())
                .setParameter(2, billItemId)
                .executeUpdate();
    } else {
        em.merge(bifd);
    }
}

private void evictLineCache() {
    javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
    cache.evict(com.divudi.core.entity.BillItem.class);
    cache.evict(com.divudi.core.entity.pharmacy.PharmaceuticalBillItem.class);
    cache.evict(BillItemFinanceDetails.class);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=PurchaseOrderRequestNativeSqlServiceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlService.java src/test/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlServiceTest.java
git commit -m "feat(pharmacy): native SQL line-item insert/update for PO Request"
```

---

## Task 4: `PurchaseOrderRequestNativeSqlService` — finalize + zero-qty retirement

**Files:**
- Modify: `src/main/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlService.java`
- Modify: `src/test/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlServiceTest.java`

**Interfaces:**
- Consumes: `billTable()`, `billItemTable()`, `pharmBillItemTable()` (Task 2)
- Produces:
  - `void finalizeBill(long billId, long editorId)` — promotes
    `BILLTYPEATOMIC` to `PHARMACY_ORDER` and sets `checked=1, checkeAt=now,
    checkedBy_ID=editorId`
  - `int retireZeroQtyLines(long billId, long retirerId)` —
    retires (native `UPDATE billitem/pharmaceuticalbillitem SET retired=1,...`) any
    line whose `qty+freeQty <= 0`, sets `remainingQty`/`remainingFreeQty` on surviving
    lines, returns count of surviving (non-retired) lines with qty > 0 — mirrors legacy
    `finalizeBillComponent()`'s `totalBillItemsCount` accumulation. Controller (Task 5)
    calls both in sequence for the Finalize button.
  - `boolean isZeroQtyLine(double qty, double freeQty)` — package-private, pure
    predicate extracted from legacy `finalizeBillComponent()`'s `totalUnits.compareTo(BigDecimal.ZERO) <= 0`
    check, called by `retireZeroQtyLines` for each row.

**Codebase testing convention (see Task 2/3's notes):** `finalizeBill` and
`retireZeroQtyLines` both touch `em` directly (native UPDATE/SELECT) and are not
unit tested — verified via Task 11's Playwright + DB pass. `isZeroQtyLine` has no
`em` access and gets a real test, same pattern as Task 3's `computeLineValues`.

- [ ] **Step 1: Write the failing test**

```java
@Test
void isZeroQtyLine_trueWhenQtyAndFreeQtyBothZero() {
    assertTrue(service.isZeroQtyLine(0.0, 0.0));
}

@Test
void isZeroQtyLine_falseWhenQtyPositive() {
    assertFalse(service.isZeroQtyLine(5.0, 0.0));
}

@Test
void isZeroQtyLine_falseWhenOnlyFreeQtyPositive() {
    assertFalse(service.isZeroQtyLine(0.0, 3.0));
}

@Test
void isZeroQtyLine_trueWhenNegativeTotal() {
    // Defensive: legacy compares totalUnits <= 0, so a negative sum (shouldn't
    // happen in practice but the guard must match) also counts as zero-qty.
    assertTrue(service.isZeroQtyLine(-1.0, 0.5));
}
```

(Add these four `@Test` methods to `PurchaseOrderRequestNativeSqlServiceTest`,
same class as Task 3's tests — import `assertTrue`/`assertFalse` from
`org.junit.jupiter.api.Assertions` alongside the existing `assertEquals` import.)

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=PurchaseOrderRequestNativeSqlServiceTest test`
Expected: FAIL — `isZeroQtyLine` does not exist.

- [ ] **Step 3: Write minimal implementation**

Add to `PurchaseOrderRequestNativeSqlService`:

```java
boolean isZeroQtyLine(double qty, double freeQty) {
    return (qty + freeQty) <= 0;
}

public void finalizeBill(long billId, long editorId) {
    em.createNativeQuery(
        "UPDATE " + billTable()
        + " SET BILLTYPEATOMIC=?, checked=1, checkeAt=?, checkedBy_ID=?, editor_ID=?, editedAt=? WHERE ID=?")
        .setParameter(1, BillTypeAtomic.PHARMACY_ORDER.toString())
        .setParameter(2, new Timestamp(new Date().getTime()))
        .setParameter(3, editorId)
        .setParameter(4, editorId)
        .setParameter(5, new Timestamp(new Date().getTime()))
        .setParameter(6, billId)
        .executeUpdate();
    evictCache();
}

@SuppressWarnings("unchecked")
public int retireZeroQtyLines(long billId, long retirerId) {
    java.util.List<Object[]> rows = em.createNativeQuery(
        "SELECT bi.ID, pbi.qty, pbi.freeQty FROM " + billItemTable() + " bi "
        + "JOIN " + pharmBillItemTable() + " pbi ON pbi.billItem_ID = bi.ID "
        + "WHERE bi.bill_ID = ? AND bi.retired = 0")
        .setParameter(1, billId)
        .getResultList();

    int survivingCount = 0;
    Timestamp now = new Timestamp(new Date().getTime());
    for (Object[] row : rows) {
        long billItemId = ((Number) row[0]).longValue();
        double qty = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        double freeQty = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

        if (isZeroQtyLine(qty, freeQty)) {
            em.createNativeQuery(
                "UPDATE " + billItemTable() + " SET retired=1, retirer_ID=?, retiredAt=?, retireComments=? WHERE ID=?")
                .setParameter(1, retirerId)
                .setParameter(2, now)
                .setParameter(3, "Retired at Finalising PO")
                .setParameter(4, billItemId)
                .executeUpdate();
            em.createNativeQuery(
                "UPDATE " + pharmBillItemTable() + " SET retired=1, retirer_ID=?, retiredAt=? WHERE billItem_ID=?")
                .setParameter(1, retirerId)
                .setParameter(2, now)
                .setParameter(3, billItemId)
                .executeUpdate();
        } else {
            em.createNativeQuery(
                "UPDATE " + pharmBillItemTable() + " SET remainingQty=qty, remainingFreeQty=freeQty WHERE billItem_ID=?")
                .setParameter(1, billItemId)
                .executeUpdate();
            survivingCount++;
        }
    }
    evictLineCache();
    return survivingCount;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=PurchaseOrderRequestNativeSqlServiceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlService.java src/test/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlServiceTest.java
git commit -m "feat(pharmacy): native SQL finalize + zero-qty line retirement for PO Request"
```

---

## Task 5: `PurchaseOrderRequestNativeSqlController` — scaffolding, navigation, guards

**Files:**
- Create: `src/main/java/com/divudi/bean/pharmacy/PurchaseOrderRequestNativeSqlController.java`

**Interfaces:**
- Consumes: `PurchaseOrderRequestNativeSqlService` (Tasks 2–4),
  `SessionController.getLoggedUser()`, `WebUserController.hasPrivilege(String)`,
  `BillNumberGenerator` (unchanged JPA-backed sequence logic — reused, not
  reimplemented, per the design spec's rationale)
- Produces: `String navigateToCreateNewPurchaseOrder()`,
  `String navigateToUpdatePurchaseOrder(Long billId)` — Task 9 (XHTML) and the
  List-to-Finalize page (Task 10) call these by name.

This task has no independent unit test — it is thin JSF scaffolding wired to the
already-tested service. Verify it manually via the Playwright pass in Task 11. Copy
the following pieces from `PurchaseOrderRequestController` **verbatim, unchanged**
(these are pure JPQL/validation logic, not part of the native-SQL migration surface,
per the design spec §2 and §4 function inventory):

- `isAuthorized(String action, String requiredPrivilege)` — copy body exactly
- `getDealorItems()`, `filterItemsByDepartmentType()`,
  `completeItemForSelectedDepartmentType()` — copy exactly
- `applyLastRatesToBillItem()` (both overloads), `getUnitsPerPack()`,
  `fetchLastPurchaseRatesForItems()`, `fetchLastRetailRatesForItems()`,
  `fetchLastRatesForItems()`, and every private helper it calls down to
  `getRateByItemId()` — copy exactly, unchanged; these are read-only JPQL
  queries, not the EAGER-cascade cost this migration targets
- `createAndAssignBillNumber()` — copy exactly, delegates to `BillNumberGenerator`
- `prepareEmailDialog()`, `sendPurchaseOrderEmail()`, `generatePurchaseOrderHtml()`
  — copy exactly; these read `currentBill` (kept as a live JPA entity for display/
  email — see below), not part of the native write path

- [ ] **Step 1: Write the controller skeleton**

```java
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.service.pharmacy.PurchaseOrderRequestNativeSqlService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * SessionScoped controller for the native-SQL Purchase Order Request page.
 * Native SQL: bill create/update, billitem+PBI writes (via the service).
 * JPA (unchanged): rate lookups, bill-number generation, email — see
 * docs/superpowers/specs/2026-08-07-po-request-native-design.md §3.
 * Related issue: #22727
 */
@Named
@SessionScoped
public class PurchaseOrderRequestNativeSqlController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ConfigOptionController configOptionController;

    @EJB
    private PurchaseOrderRequestNativeSqlService purchaseOrderRequestNativeSqlService;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;

    private Bill currentBill;
    private BillItem currentBillItem;
    private List<BillItem> billItems;
    private List<BillItem> selectedBillItems;
    private boolean printPreview;
    private boolean itemHistoryVisible;
    private Long billId;
    private String emailRecipient;

    public String navigateToCreateNewPurchaseOrder() {
        resetBillValues();
        currentBill = new Bill();
        return "/pharmacy/pharmacy_purhcase_order_request_native?faces-redirect=true";
    }

    public String navigateToUpdatePurchaseOrder(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        resetBillValues();
        this.billId = billId;
        currentBill = billFacade.find(billId);
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return "";
        }
        billItems = loadBillItems(currentBill);
        return "/pharmacy/pharmacy_purhcase_order_request_native?faces-redirect=true";
    }

    private List<BillItem> loadBillItems(Bill bill) {
        String jpql = "select bi from BillItem bi where bi.retired=:ret and bi.bill=:bill order by bi.searialNo";
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("ret", false);
        m.put("bill", bill);
        return billFacade.findByJpql(jpql, m) instanceof List ? (List<BillItem>) (List<?>) billFacade.findByJpql(jpql, m) : new ArrayList<>();
    }

    private void resetBillValues() {
        currentBill = null;
        currentBillItem = new BillItem();
        billItems = new ArrayList<>();
        selectedBillItems = null;
        printPreview = false;
        itemHistoryVisible = false;
        billId = null;
    }

    private boolean isAuthorized(String action, String requiredPrivilege) {
        if (!webUserController.hasPrivilege(requiredPrivilege)) {
            JsfUtil.addErrorMessage("You don't have permission to " + action.toLowerCase() + " purchase orders.");
            return false;
        }
        return true;
    }

    // Getters/setters
    public Bill getCurrentBill() { return currentBill; }
    public void setCurrentBill(Bill currentBill) { this.currentBill = currentBill; }
    public BillItem getCurrentBillItem() { return currentBillItem; }
    public void setCurrentBillItem(BillItem currentBillItem) { this.currentBillItem = currentBillItem; }
    public List<BillItem> getBillItems() { return billItems; }
    public void setBillItems(List<BillItem> billItems) { this.billItems = billItems; }
    public List<BillItem> getSelectedBillItems() { return selectedBillItems; }
    public void setSelectedBillItems(List<BillItem> selectedBillItems) { this.selectedBillItems = selectedBillItems; }
    public boolean isPrintPreview() { return printPreview; }
    public void setPrintPreview(boolean printPreview) { this.printPreview = printPreview; }
    public boolean isItemHistoryVisible() { return itemHistoryVisible; }
    public void setItemHistoryVisible(boolean itemHistoryVisible) { this.itemHistoryVisible = itemHistoryVisible; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getEmailRecipient() { return emailRecipient; }
    public void setEmailRecipient(String emailRecipient) { this.emailRecipient = emailRecipient; }
}
```

- [ ] **Step 2: Compile check**

Run: `mvn -q compile`
Expected: BUILD SUCCESS (no tests yet — this is JSF scaffolding, verified via
Playwright in Task 11)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/PurchaseOrderRequestNativeSqlController.java
git commit -m "feat(pharmacy): scaffold PurchaseOrderRequestNativeSqlController"
```

---

## Task 6: Controller — `addItem` / `removeItem` / `onEdit` wired to native service

**Files:**
- Modify: `src/main/java/com/divudi/bean/pharmacy/PurchaseOrderRequestNativeSqlController.java`

**Interfaces:**
- Consumes: `PurchaseOrderRequestNativeSqlService.saveLine()` (Task 3)
- Produces: `void addItem()`, `void removeItem(BillItem bi)`, `void onEdit(BillItem bi)`
  — same method names/signatures as the legacy controller so the copied XHTML
  (Task 9) needs zero EL renaming beyond the bean name prefix.

Port the **exact guard logic** from legacy `addItem()` (department-type matching,
duplicate-item check gated on `Prevent Duplicate Items in Purchase Orders`,
auto-department-type-assignment) and `onEdit()` (integer-only qty gated on
`Pharmacy Purchase - Quantity Must Be Integer`) — see design spec §2. Only the
persistence call changes: instead of `billItemFacade.create()/.edit()`, build a
`PurchaseOrderRequestLineData` from the in-memory `BillItem`/`PharmaceuticalBillItem`
and call `purchaseOrderRequestNativeSqlService.saveLine()`.

- [ ] **Step 1: Implement `addItem`, `removeItem`, `onEdit`**

```java
public void addItem() {
    if (currentBillItem.getItem() == null) {
        JsfUtil.addErrorMessage("Please select and item from the list");
        return;
    }

    if (currentBill.getDepartmentType() == null) {
        currentBill.setDepartmentType(currentBillItem.getItem().getDepartmentType() != null
                ? currentBillItem.getItem().getDepartmentType() : DepartmentType.Pharmacy);
    }

    DepartmentType itemDepartmentType = currentBillItem.getItem().getDepartmentType();
    if (itemDepartmentType != null && !itemDepartmentType.equals(currentBill.getDepartmentType())) {
        JsfUtil.addErrorMessage("Cannot add items from different department types. "
                + "Bill is set for " + currentBill.getDepartmentType().getLabel()
                + " items, but you are trying to add a " + itemDepartmentType.getLabel() + " item.");
        return;
    }

    if (configOptionApplicationController.getBooleanValueByKey("Prevent Duplicate Items in Purchase Orders", false)) {
        for (BillItem existing : billItems) {
            if (existing != null && !existing.isRetired() && existing.getItem() != null
                    && existing.getItem().equals(currentBillItem.getItem())) {
                JsfUtil.addErrorMessage("This item has already been added to the purchase order. Please update the quantity of the existing item instead of adding it again.");
                return;
            }
        }
    }

    currentBillItem.setSearialNo(billItems.size());
    applyLastRatesToBillItem(currentBillItem);
    billItems.add(currentBillItem);
    calculateBillTotals();
    currentBillItem = new BillItem();
}

public void removeItem(BillItem bi) {
    if (!isAuthorized("SAVE", "PurchaseOrderSave")) {
        return;
    }
    if (currentBill == null || bi == null) {
        return;
    }
    bi.setRetired(true);
    billItems.remove(bi);
    calculateBillTotals();
    itemHistoryVisible = false;
}

public void onEdit(BillItem bi) {
    if (configOptionController.getBooleanValueByKey("Pharmacy Purchase - Quantity Must Be Integer", true)) {
        java.math.BigDecimal qty = bi.getBillItemFinanceDetails().getQuantity();
        java.math.BigDecimal freeQty = bi.getBillItemFinanceDetails().getFreeQuantity();
        if (qty != null && qty.remainder(java.math.BigDecimal.ONE).compareTo(java.math.BigDecimal.ZERO) != 0) {
            bi.getBillItemFinanceDetails().setQuantity(java.math.BigDecimal.ZERO);
            JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for quantity. Decimal values are not allowed.");
            calculateBillTotals();
            return;
        }
        if (freeQty != null && freeQty.remainder(java.math.BigDecimal.ONE).compareTo(java.math.BigDecimal.ZERO) != 0) {
            bi.getBillItemFinanceDetails().setFreeQuantity(java.math.BigDecimal.ZERO);
            JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for free quantity. Decimal values are not allowed.");
            calculateBillTotals();
            return;
        }
    }
    calculateBillTotals();
}

private void calculateBillTotals() {
    double total = 0.0;
    for (BillItem bi : billItems) {
        if (bi != null && !bi.isRetired()) {
            total += bi.getNetValue();
        }
    }
    currentBill.setNetTotal(total);
    currentBill.setTotal(total);
}
```

Note: `applyLastRatesToBillItem` is the method copied verbatim per Task 5's
instructions — it populates `BillItemFinanceDetails`/`PharmaceuticalBillItem` rate
fields on the in-memory entity before it's ever sent to `saveLine()`.

- [ ] **Step 2: Compile check**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/PurchaseOrderRequestNativeSqlController.java
git commit -m "feat(pharmacy): wire addItem/removeItem/onEdit to native controller"
```

---

## Task 7: Controller — save draft / finalize wired to native service

**Files:**
- Modify: `src/main/java/com/divudi/bean/pharmacy/PurchaseOrderRequestNativeSqlController.java`

**Interfaces:**
- Consumes: `PurchaseOrderRequestNativeSqlService.createDraftBill/updateDraftBillHeader/
  saveLine/finalizeBill/retireZeroQtyLines` (Tasks 2–4)
- Produces: `synchronized void saveRequest()`, `synchronized void finalizeRequest()` —
  same names as legacy, called by the copied XHTML's Save/Finalize buttons.

Port the exact validation order from legacy `saveRequestWithoutMessage()` and
`finalizeRequest()` (design spec §2's guard list) — only the persistence calls
change target from `billFacade`/`billItemFacade` to
`purchaseOrderRequestNativeSqlService`. Keep the `synchronized` modifier — this is
the issue #21417 double-submit guard and must not be dropped (design spec §2,
"Concurrency guards").

- [ ] **Step 1: Implement `saveRequest` and `finalizeRequest`**

```java
public synchronized void saveRequest() {
    if (!isAuthorized("SAVE", "PurchaseOrderSave")) {
        return;
    }
    boolean saved = saveRequestWithoutMessage();
    if (saved) {
        JsfUtil.addSuccessMessage("Request Saved");
    }
}

private boolean saveRequestWithoutMessage() {
    if (currentBill.isChecked()) {
        JsfUtil.addErrorMessage("Cannot save a finalized bill");
        return false;
    }
    if (currentBill.getToInstitution() == null) {
        JsfUtil.addErrorMessage("Please select a supplier");
        return false;
    }

    if (currentBill.getId() == null) {
        String[] billNumbers = createAndAssignBillNumber();
        long newBillId = purchaseOrderRequestNativeSqlService.createDraftBill(
                sessionController.getLoggedUser().getDepartment().getId(),
                sessionController.getLoggedUser().getDepartment().getInstitution().getId(),
                sessionController.getLoggedUser().getId(),
                billNumbers[0],
                billNumbers[1]);
        currentBill = billFacade.find(newBillId);
    }

    purchaseOrderRequestNativeSqlService.updateDraftBillHeader(
            currentBill.getId(),
            currentBill.getToInstitution().getId(),
            currentBill.getPaymentMethod(),
            currentBill.getCreditDuration(),
            currentBill.isConsignment(),
            currentBill.getDepartmentType(),
            sessionController.getLoggedUser().getId());

    for (BillItem bi : billItems) {
        PurchaseOrderRequestLineData line = toLineData(bi);
        long billItemId = purchaseOrderRequestNativeSqlService.saveLine(currentBill.getId(), line);
        bi.setId(billItemId);
    }

    return true;
}

public synchronized void finalizeRequest() {
    if (!isAuthorized("FINALIZE", "PurchaseOrderFinalize")) {
        return;
    }
    if (currentBill == null) {
        JsfUtil.addErrorMessage("No Bill");
        return;
    }
    if (currentBill.getToInstitution() == null) {
        JsfUtil.addErrorMessage("Please selectr a supplier");
        return;
    }
    if (currentBill.isChecked()) {
        JsfUtil.addErrorMessage("Cannot finalize an already finalized bill");
        return;
    }
    if (currentBill.getPaymentMethod() == null) {
        JsfUtil.addErrorMessage("Please select a payment method.");
        return;
    }
    if (billItems == null || billItems.isEmpty()) {
        JsfUtil.addErrorMessage("Please add bill items.");
        return;
    }
    saveRequestWithoutMessage();

    purchaseOrderRequestNativeSqlService.finalizeBill(currentBill.getId(), sessionController.getLoggedUser().getId());
    int survivingCount = purchaseOrderRequestNativeSqlService.retireZeroQtyLines(currentBill.getId(), sessionController.getLoggedUser().getId());
    if (survivingCount == 0) {
        JsfUtil.addErrorMessage("Please enter item quantities for the bill.");
        return;
    }

    currentBill = billFacade.find(currentBill.getId());
    billItems = loadBillItems(currentBill);
    JsfUtil.addSuccessMessage("Request successfully finalized.");
    printPreview = true;
}

private PurchaseOrderRequestLineData toLineData(BillItem bi) {
    PurchaseOrderRequestLineData line = new PurchaseOrderRequestLineData();
    line.setBillItemId(bi.getId());
    line.setPharmaceuticalBillItemId(bi.getPharmaceuticalBillItem() != null ? bi.getPharmaceuticalBillItem().getId() : null);
    line.setItemId(bi.getItem().getId());
    line.setAmpp(bi.getItem() instanceof com.divudi.core.entity.pharmacy.Ampp);
    line.setQuantity(bi.getBillItemFinanceDetails().getQuantity());
    line.setFreeQuantity(bi.getBillItemFinanceDetails().getFreeQuantity());
    line.setPurchaseRate(bi.getBillItemFinanceDetails().getLineGrossRate());
    line.setRetailRate(bi.getBillItemFinanceDetails().getRetailSaleRate());
    line.setUnitsPerPack(bi.getBillItemFinanceDetails().getUnitsPerPack());
    line.setSerialNo(bi.getSearialNo());
    line.setCreaterId(sessionController.getLoggedUser().getId());
    return line;
}
```

**`createAndAssignBillNumber()` must return `String[2]` = `{deptId, insId}` from a
SINGLE call — never call it twice.** This is a correction to an earlier draft of
this plan that called it twice (once "for deptId", once "for insId"); that is
wrong and would generate two independent sequence numbers. Verified against the
actual legacy method (`PurchaseOrderRequestController.java:830-881`): legacy
computes `deptId` exactly once via one of 4 configured strategies (lines
845-866), THEN computes `insId` either via its own separate strategy (line
868-872) OR — in the default/most common case — by reusing the just-computed
`deptId` value as `insId`'s fallback (lines 873-879: `if (billId != null &&
!billId.trim().isEmpty()) { getCurrentBill().setInsId(billId); }`). `deptId` and
`insId` are NOT independent draws from the sequence generator; `insId` depends
on the `deptId` computed in the same invocation.

Implement `createAndAssignBillNumber()` in this task as:

```java
private String[] createAndAssignBillNumber() {
    String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_PRE, "");
    if (billSuffix == null || billSuffix.trim().isEmpty()) {
        configOptionApplicationController.setLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_PRE, "POR");
    }

    boolean stratDeptInsYear = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Requests - Prefix + Department Code + Institution Code + Year + Yearly Number and Yearly Number", false);
    boolean stratInsDeptYear = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Requests - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number", false);
    boolean stratInsYear = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Requests - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);
    boolean stratInsIdInsYear = configOptionApplicationController.getBooleanValueByKey("Institution Number Generation Strategy for Purchase Order Requests - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);

    String deptId;
    if (stratDeptInsYear) {
        deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
    } else if (stratInsDeptYear) {
        deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
    } else if (stratInsYear) {
        deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
    } else {
        deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
    }

    String insId;
    if (stratInsIdInsYear) {
        insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
    } else {
        insId = (deptId != null && !deptId.trim().isEmpty()) ? deptId : null;
    }

    return new String[]{deptId, insId};
}
```

This adapts legacy's mutate-`currentBill`-directly style (there is no `currentBill`
row yet at this point in the native flow — `createDraftBill()` hasn't been called
yet) into a pure return, but preserves the exact same strategy branching and the
exact same deptId→insId fallback relationship.

**Task 5 already added a `private void createAndAssignBillNumber()` to this
controller** (copied byte-for-byte from legacy per Task 5's "copy verbatim"
instruction, at the time correctly matching legacy's own signature). That
version calls `getCurrentBill().setDeptId(...)`/`setInsId(...)` directly — but
at the point Task 7 needs to call it (before `createDraftBill()` has run), there
is no bill row yet for `getCurrentBill()` to mutate. **Task 7 must REPLACE that
existing method** with the `private String[] createAndAssignBillNumber()`
version shown above (same name, changed signature and return type) — do not
leave both versions in the file, and do not call the old void version anywhere.

- [ ] **Step 2: Compile check**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/PurchaseOrderRequestNativeSqlController.java
git commit -m "feat(pharmacy): wire saveRequest/finalizeRequest to native service"
```

---

## Task 8: Controller — bulk-add item methods (ported verbatim)

**Correction to an earlier draft of this task:** the rate-lookup chain
(`applyLastRatesToBillItem` both overloads, `fetchLastPurchaseRatesForItems`,
`fetchLastRetailRatesForItems`, and all their private helpers) and the email
methods (`prepareEmailDialog`, `sendPurchaseOrderEmail`, `generatePurchaseOrderHtml`)
were **already copied into `PurchaseOrderRequestNativeSqlController` by Task 5**
(confirmed present in the file — Task 5's own review verified them verbatim
against legacy). Only the bulk-add and item-history-display methods below are
still missing. Do NOT copy the rate-lookup or email methods again — they already
exist in the file; re-adding them would either duplicate a method (compile error)
or silently diverge from the Task-5-verified copy.

**Files:**
- Modify: `src/main/java/com/divudi/bean/pharmacy/PurchaseOrderRequestNativeSqlController.java`

**Interfaces:**
- Consumes: `applyLastRatesToBillItem`/`fetchLastPurchaseRatesForItems`/
  `fetchLastRetailRatesForItems` (already present from Task 5 — call them, don't
  recreate them), `getUnitsPerPack` (already present from Task 5)
- Produces: `generateBillComponentsForAllSupplierItems(List<Item>)`,
  `addAllSupplierItems()`, `addAllSupplierItemsBelowRol()`, `displayItemDetails(BillItem)`,
  `closeItemHistory()` — same names as legacy for the copied XHTML.

Copy these methods from `PurchaseOrderRequestController` **verbatim** — read the
legacy file first to find their current exact line numbers rather than trusting
any line-number reference in this plan (line numbers drift). Search for
`generateBillComponentsForAllSupplierItems`, `addAllSupplierItems`,
`addAllSupplierItemsBelowRol`, `displayItemDetails`, `closeItemHistory` in
`PurchaseOrderRequestController.java` and copy each method body verbatim. Only
adapt field/method access to this controller's field names (`billItems`,
`currentBill`, `sessionController`, `configOptionApplicationController`,
`pharmacyBean` — inject `@EJB private PharmacyBean pharmacyBean;` if not already
present) — do not alter the logic itself.

- [ ] **Step 1: Find and copy the 5 methods listed above from
  `PurchaseOrderRequestController` verbatim**, adjusting only field references to
  match this controller. Do NOT touch or re-copy any rate-lookup or email method —
  verify each of the 5 target methods doesn't already exist in the new controller
  before adding it (a quick grep for the method name), since this brief itself was
  corrected once already for over-claiming what was missing.

- [ ] **Step 2: Compile check**

Run: `mvn -q compile`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/PurchaseOrderRequestNativeSqlController.java
git commit -m "feat(pharmacy): port bulk-add, rate lookup, and email logic to native controller"
```

---

## Task 9: Native XHTML page

**Files:**
- Create: `src/main/webapp/pharmacy/pharmacy_purhcase_order_request_native.xhtml`

**Interfaces:**
- Consumes: every public method/getter on `PurchaseOrderRequestNativeSqlController`
  (Tasks 5–8)
- Produces: the page itself — Task 10 (List-to-Finalize) links to it.

Copy `src/main/webapp/pharmacy/pharmacy_purhcase_order_request.xhtml` in full,
then do a global find-and-replace of `purchaseOrderRequestController` →
`purchaseOrderRequestNativeSqlController` (the new bean's `@Named` default name).
**Verify no other bean references need renaming** — this page does not use the
numbered-controller pattern that caused the #15845 defect (single window only, per
design spec §8), so a plain unanchored replace of this one bean name is safe here.

Add a small "Legacy View" link near the page header (same visual treatment as
`pharmacy_reprint_po_native.xhtml`'s Legacy View button, per the print-page guide
pattern) pointing back to `pharmacy_purhcase_order_request.xhtml`, for use during
the review window before the legacy page is retired.

- [ ] **Step 1: Copy and rename**

```bash
cp "src/main/webapp/pharmacy/pharmacy_purhcase_order_request.xhtml" "src/main/webapp/pharmacy/pharmacy_purhcase_order_request_native.xhtml"
```

Then edit the new file: replace every `purchaseOrderRequestController` with
`purchaseOrderRequestNativeSqlController`, and add the Legacy View link.

- [ ] **Step 2: Compile / deploy check**

Run the project's local build+deploy per `developer_docs/deployment/` (or the `run`
skill) and confirm the page loads without a JSF EL resolution error in
`glassfish/domains/domain1/logs/server.log`.

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/pharmacy/pharmacy_purhcase_order_request_native.xhtml
git commit -m "feat(pharmacy): add native Purchase Order Request page"
```

---

## Task 10: Wire List-to-Finalize picker to the new controller

**Files:**
- Modify: `src/main/webapp/pharmacy/pharmacy_purhcase_order_list_to_finalize.xhtml:284-287`

**Interfaces:**
- Consumes: `PurchaseOrderRequestNativeSqlController.navigateToUpdatePurchaseOrder(Long)`
  (Task 5)

- [ ] **Step 1: Repoint the Edit action**

Change:
```xml
action="#{purchaseOrderRequestController.navigateToUpdatePurchaseOrder()}"
...
<f:setPropertyActionListener target="#{purchaseOrderRequestController.billId}" value="#{b.billId}"/>
```
to:
```xml
action="#{purchaseOrderRequestNativeSqlController.navigateToUpdatePurchaseOrder(b.billId)}"
```

(Passing `b.billId` directly as a method parameter replaces the
`setPropertyActionListener` + no-arg-method pattern, since the new controller's
`navigateToUpdatePurchaseOrder` takes `Long billId` directly — see Task 5's
signature.)

- [ ] **Step 2: Manual verify**

Open `pharmacy_purhcase_order_list_to_finalize.xhtml` in the running app, click Edit
on a draft PO, confirm it opens `pharmacy_purhcase_order_request_native.xhtml` with
the correct bill loaded.

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/pharmacy/pharmacy_purhcase_order_list_to_finalize.xhtml
git commit -m "feat(pharmacy): route List-to-Finalize Edit action to native PO Request page"
```

---

## Task 11: Playwright verification + second-agent review

**Files:** none (verification only)

**Interfaces:** none

- [ ] **Step 1: Playwright pass**

Using the `playwright-e2e` skill, drive the running app through: create new PO
request → add item (single, via autocomplete) → add item (bulk, "Add All Supplier
Items") → add item (bulk, "Add All Supplier Items Below ROL") → edit a line's
qty/rate → remove a line → save draft (click Save twice, confirm the bill row is
not duplicated in the DB) → set payment method + supplier → finalize → confirm
zero-qty lines were retired → open email dialog → send test email → confirm
List-to-Finalize picker still opens this bill for further edits before finalize
(N/A after finalize, since `isChecked()` blocks further edits, matching legacy).

- [ ] **Step 2: DB verification**

Run the queries from `developer_docs/pharmacy/native-sql-bill-migration-guide.md`
§"Database Verification Checklist", adapted to this bill's tables (`bill`,
`billitem`, `pharmaceuticalbillitem` only — skip the BIFD/BFD/stock/payment steps,
which don't apply at this phase per design spec §5). Confirm: `BILLTYPEATOMIC`
transitions `PHARMACY_ORDER_PRE` → `PHARMACY_ORDER`; no duplicate `billitem` rows
from the double-save test; `remainingQty`/`remainingFreeQty` set correctly on
finalize.

- [ ] **Step 3: Second-agent review**

Dispatch a fresh agent (or use `/code-review`) with the design spec's §4 function
inventory table as the checklist: confirm every legacy method has a native
equivalent present and wired, and that no button visible on
`pharmacy_purhcase_order_request.xhtml` is missing from
`pharmacy_purhcase_order_request_native.xhtml`.

- [ ] **Step 4: Update GitHub issue and memory**

Check off completed items on issue #22727, comment with the Playwright/DB
verification results, and update the memory file
`project_po_native_sql_migration_umbrella_22726.md` with Phase 1's completion status
before starting Phase 2 (Approving page).

---

## Plan Self-Review Notes

- **Spec coverage:** Every function in the design spec's §4 inventory table has a
  corresponding task (Tasks 6–8) or is explicitly marked as copied verbatim.
  Bill-number generation (§2) is handled in Task 7 via the adapted
  `createAndAssignBillNumber()`. Email (§2) is Task 8. The two-bill cross-link is
  explicitly out of scope for this phase (Phase 2's concern) — confirmed no task
  here writes `referenceBill`.
- **Type consistency:** `PurchaseOrderRequestLineData` field names introduced in
  Task 1 are used identically in Tasks 3, 4, and 7 (`saveLine`, `toLineData`).
  `PurchaseOrderRequestNativeSqlService` method signatures introduced in Tasks 2–4
  are called with matching argument order/types in Task 7.
- **Placeholder scan:** No TBD/TODO markers. Task 5's copy-instructions for §"getDealorItems"
  etc. name exact legacy line ranges (Task 8) rather than saying "similar to legacy."
- **Scope check:** This plan implements Phase 1 only (#22727). Phase 2 (Approving
  page, #22726) is a separate plan, written after this phase's Playwright + review
  gate (Task 11) passes, per the master issue's sequencing decision.
