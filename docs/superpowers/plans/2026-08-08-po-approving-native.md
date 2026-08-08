# Native Purchase Order Approving Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the write path of the Purchase Order Approving page
(`pharmacy_purhcase_order_approving.xhtml` / `PurchaseOrderController`) with a
native-SQL implementation, per
`docs/superpowers/specs/2026-08-08-po-approving-native-design.md`.

**Architecture:** New `PurchaseOrderApprovingNativeSqlService` (`@Stateless`)
owns native INSERT/UPDATE for the approved bill's own `bill`/`billitem`/
`pharmaceuticalbillitem` rows, reusing Phase 1's `PurchaseOrderRequestNativeSqlService`
for line-value math and `PurchaseOrderRequestLineData` as the shared DTO shape.
New `PurchaseOrderApprovingNativeSqlController` (`@Named @SessionScoped`) owns
page state, guards, and (as revised during PR review — see note below) the
native cross-link write. New page
`pharmacy_purhcase_order_approving_native.xhtml` is a 1:1 layout port.

**Tech Stack:** JPA (EclipseLink) native queries via `EntityManager`, JSF/PrimeFaces, JUnit 5.

## Global Constraints

- **Requirement: 100% functional replication.** Every button, validation
  message, and guard on the legacy page carries over.
- **`requestedBill.referenceBill = approvedBill` cross-link write: originally
  JPA merge only (L2-cache-coherence rule from master issue #22726),
  superseded during PR review.** Both cross-link directions
  (`approvedBill.referenceBill` and `requestedBill.referenceBill`) are now
  native `UPDATE` writes — see the design spec §2/§7 for why: a
  `billFacade.edit()` merge on a bill whose lines were written by native SQL
  through a sibling EJB risks EclipseLink orphan-removal deleting those
  lines, and this applies to `requestedBill`'s merge exactly as it did to
  `approvedBill`'s. Confirmed working live via Playwright + direct DB query.
- **`requestedBill` entity itself stays read-only via JPA for guard checks
  and header seeding** — only its persisted cross-link write moved to native
  SQL; it is never merged via `billFacade.edit()`.
- **Recompute line values from 5 raw inputs, never copy legacy's ~25 BIFD
  fields verbatim.** Reuse `PurchaseOrderRequestNativeSqlService.computeLineValues()`
  and `LineValues` directly (both package-private in `com.divudi.service.pharmacy`,
  visible to the new service in the same package) rather than duplicating the logic.
- **Reuse `PurchaseOrderRequestLineData`** as both the write-side line DTO
  (as Phase 1 uses it) and the read-side projection returned by
  `loadRequestedLines()` — do not create a second, near-identical DTO class.
- Both `navigateToPurchaseOrderApproval()` and `approve()` must be
  `synchronized`, with an already-approved re-check inside `approve()` even
  though `navigateToPurchaseOrderApproval()` already checked it — this is the
  fix for a real production incident (GRN item duplication,
  PO/RH/GSK/26/01093), not defensive boilerplate to be simplified away.
- Evict `Bill`, `BillItem`, `PharmaceuticalBillItem`, `BillItemFinanceDetails`
  from L2 cache after every native INSERT/UPDATE.
- HTML-escape every free-text field in `generatePurchaseOrderHtml()` from the
  start (institution/supplier name, address, phone, item code/name, person
  names) — Phase 1 had to fix this after review; do it correctly here first.
- `navigateToPurchaseOrderApproval(Long requestedBillId)` must guard the
  loaded bill: reject if not `BillTypeAtomic.PHARMACY_ORDER`, retired,
  cancelled, or not owned by the logged-in user's department — same fix
  Phase 1 needed after review, applied here from the start.
- Apache Commons Text (`org.apache.commons.text.StringEscapeUtils`) is
  already a project dependency (used in Phase 1) — reuse it, don't add a new
  escaping utility.

---

### Task 1: `PurchaseOrderApprovingNativeSqlService` — bill and line persistence

**Files:**
- Create: `src/main/java/com/divudi/service/pharmacy/PurchaseOrderApprovingNativeSqlService.java`
- Test: `src/test/java/com/divudi/service/pharmacy/PurchaseOrderApprovingNativeSqlServiceTest.java`

**Interfaces:**
- Consumes: `PurchaseOrderRequestNativeSqlService.computeLineValues(PurchaseOrderRequestLineData)`
  → `PurchaseOrderRequestNativeSqlService.LineValues` (package-private, same
  package `com.divudi.service.pharmacy`); `PurchaseOrderRequestNativeSqlService.isZeroQtyLine(double, double)`
  (package-private). `PurchaseOrderRequestLineData` (`com.divudi.core.data.dto.pharmacy`)
  as both the line-write DTO and the `loadRequestedLines()` return type.
- Produces: `long createApprovedBill(long requestedBillId, long departmentId, long institutionId, long fromDepartmentId, long fromInstitutionId, long createrId, String deptId, String insId)`,
  `void updateApprovedBillHeader(long approvedBillId, Long toInstitutionId, PaymentMethod paymentMethod, int creditDuration, boolean consignment, DepartmentType departmentType, String comments, Long editorId)`,
  `long saveApprovedLine(long approvedBillId, PurchaseOrderRequestLineData line)`,
  `int retireZeroQtyApprovedLines(long approvedBillId, long retirerId)`,
  `List<PurchaseOrderRequestLineData> loadRequestedLines(long requestedBillId)`
  — used by later tasks (Task 3 controller).

This service owns only the **approved** bill's own rows. It never reads or
writes the requested bill (that stays JPA, owned by the controller in Task 3).

- [ ] **Step 1: Scaffold the service class and table-name resolution**

Mirror `PurchaseOrderRequestNativeSqlService`'s `resolveTable`/`billTable`/
`billItemTable`/`pharmBillItemTable` pattern exactly (case-insensitive table
name resolution via `INFORMATION_SCHEMA`, cached in instance fields) — this
project's migration guide requires it for cross-deployment case sensitivity.

```java
package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData;
import com.divudi.core.entity.BillItemFinanceDetails;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Native SQL write path for the Purchase Order Approving bill.
 * Only the APPROVED bill's own bill / billitem / pharmaceuticalbillitem rows
 * are touched here -- the requested bill stays JPA (see
 * PurchaseOrderApprovingNativeSqlController). BillItemFinanceDetails stays
 * JPA (IDENTITY PK, calculation-heavy), matching
 * PurchaseOrderRequestNativeSqlService's split.
 * Related issue: #22738
 */
@Stateless
public class PurchaseOrderApprovingNativeSqlService {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @EJB
    private PurchaseOrderRequestNativeSqlService purchaseOrderRequestNativeSqlService;

    private String tBill;
    private String tBillItem;
    private String tPharmBillItem;

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

    private void evictCache() {
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(com.divudi.core.entity.Bill.class);
    }

    private void evictLineCache() {
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(com.divudi.core.entity.Bill.class);
        cache.evict(com.divudi.core.entity.BillItem.class);
        cache.evict(com.divudi.core.entity.pharmacy.PharmaceuticalBillItem.class);
        cache.evict(BillItemFinanceDetails.class);
    }
}
```

- [ ] **Step 2: `createApprovedBill` — native INSERT for the approved bill row**

Sets the cross-link FKs (`referenceBill_ID` on this row points nowhere yet —
the *requested* bill's `referenceBill_ID` points here, written later in JPA
by the controller; this row's own `backwardReferenceBill_ID` points at the
requested bill, matching legacy's `setBackwardReferenceBill(getRequestedBill())`).
`billClassType` is `BilledBill` per legacy's `new BilledBill()` — write it as
a literal column too, matching how `BillTypeAtomic`/`BillType` are written as
`.toString()` elsewhere in Phase 1's service.

```java
public long createApprovedBill(long requestedBillId, long departmentId, long institutionId,
        long fromDepartmentId, long fromInstitutionId, long createrId, String deptId, String insId) {
    Date now = new Date();
    em.createNativeQuery(
        "INSERT INTO " + billTable()
        + " (BILLTYPEATOMIC, billType, billClassType, department_ID, institution_ID,"
        + " fromDepartment_ID, fromInstitution_ID, backwardReferenceBill_ID,"
        + " creater_ID, createdAt, checked, retired, cancelled, deptId, insId, netTotal, total)"
        + " VALUES (?,?,?,?,?,?,?,?,?,?,0,0,0,?,?,0,0)")
        .setParameter(1, BillTypeAtomic.PHARMACY_ORDER_APPROVAL.toString())
        .setParameter(2, BillType.PharmacyOrderApprove.toString())
        .setParameter(3, com.divudi.core.data.BillClassType.BilledBill.toString())
        .setParameter(4, departmentId)
        .setParameter(5, institutionId)
        .setParameter(6, fromDepartmentId)
        .setParameter(7, fromInstitutionId)
        .setParameter(8, requestedBillId)
        .setParameter(9, createrId)
        .setParameter(10, new Timestamp(now.getTime()))
        .setParameter(11, deptId)
        .setParameter(12, insId)
        .executeUpdate();
    long billId = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
    evictCache();
    return billId;
}
```

Note: verify `BillClassType` is a plain Java `enum` (not a JPA-only
converter-backed type) before writing `.toString()` — check
`src/main/java/com/divudi/core/data/BillClassType.java` (or wherever it
lives; grep first) the same way `BillTypeAtomic`/`BillType`/`PaymentMethod`
were confirmed as plain enums in Phase 1. If `billClassType` turns out to be
set automatically by the `BilledBill` JPA subclass discriminator rather than
a plain column, drop it from this INSERT and rely on `BILLTYPEATOMIC`/
`billType` alone, matching whichever pattern the existing `Bill`/`BilledBill`
mapping actually uses.

- [ ] **Step 3: `updateApprovedBillHeader` — native UPDATE for payment method / supplier / etc.**

Mirrors `PurchaseOrderRequestNativeSqlService.updateDraftBillHeader` exactly
(including the `comments` column, present in that method after Phase 1's
post-merge review fix — do not omit it here).

```java
public void updateApprovedBillHeader(long approvedBillId, Long toInstitutionId, PaymentMethod paymentMethod,
        int creditDuration, boolean consignment, DepartmentType departmentType, String comments, Long editorId) {
    em.createNativeQuery(
        "UPDATE " + billTable()
        + " SET toInstitution_ID=?, paymentMethod=?, creditDuration=?, consignment=?,"
        + " departmentType=?, comments=?, editor_ID=?, editedAt=?"
        + " WHERE ID=?")
        .setParameter(1, toInstitutionId)
        .setParameter(2, paymentMethod != null ? paymentMethod.toString() : null)
        .setParameter(3, creditDuration)
        .setParameter(4, consignment ? 1 : 0)
        .setParameter(5, departmentType != null ? departmentType.toString() : null)
        .setParameter(6, comments)
        .setParameter(7, editorId)
        .setParameter(8, new Timestamp(new Date().getTime()))
        .setParameter(9, approvedBillId)
        .executeUpdate();
    evictCache();
}
```

- [ ] **Step 4: `saveApprovedLine` — native INSERT/UPDATE for billitem + pharmaceuticalbillitem**

Copy `PurchaseOrderRequestNativeSqlService.saveLine()` verbatim, including
its post-review upsert-by-existence fix for the PBI row (UPDATE first,
INSERT only if 0 rows affected — never branch on
`line.getPharmaceuticalBillItemId()`), and its `saveBillItemFinanceDetails`
helper for the JPA-persisted `BillItemFinanceDetails`. The only difference
from Phase 1: `billItemId`'s owning `bill_ID` is the **approved** bill, and
the `computeLineValues()` call delegates to
`purchaseOrderRequestNativeSqlService.computeLineValues(line)` (injected
`@EJB`) instead of a local copy — do not duplicate that method's logic here.

```java
public long saveApprovedLine(long approvedBillId, PurchaseOrderRequestLineData line) {
    PurchaseOrderRequestNativeSqlService.LineValues v = purchaseOrderRequestNativeSqlService.computeLineValues(line);
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
            .setParameter(1, approvedBillId)
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

    int updated = em.createNativeQuery(
            "UPDATE " + pharmBillItemTable()
            + " SET qty=?, freeQty=?, purchaseRate=?, purchaseRatePack=?, purchaseValue=?,"
            + " retailRate=?, retailRatePack=?, retailRateInUnit=?, retailValue=?,"
            + " remainingQty=?, remainingFreeQty=? WHERE billItem_ID=?")
            .setParameter(1, pbiQty)
            .setParameter(2, pbiFreeQty)
            .setParameter(3, pbiPurchaseRate)
            .setParameter(4, purchaseRate.doubleValue())
            .setParameter(5, purchaseValue.doubleValue())
            .setParameter(6, pbiRetailRate)
            .setParameter(7, retailRate.doubleValue())
            .setParameter(8, pbiRetailRate)
            .setParameter(9, retailValue.doubleValue())
            .setParameter(10, pbiQty)
            .setParameter(11, pbiFreeQty)
            .setParameter(12, billItemId)
            .executeUpdate();

    if (updated == 0) {
        em.createNativeQuery(
            "INSERT INTO " + pharmBillItemTable()
            + " (billItem_ID, qty, freeQty, purchaseRate, purchaseRatePack, purchaseValue,"
            + " retailRate, retailRatePack, retailRateInUnit, retailValue, remainingQty, remainingFreeQty,"
            + " costRate, costRatePack, costValue, createdAt, creater_ID)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,0,0,0,?,?)")
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
            .setParameter(11, pbiQty)
            .setParameter(12, pbiFreeQty)
            .setParameter(13, new Timestamp(new Date().getTime()))
            .setParameter(14, line.getCreaterId())
            .executeUpdate();
    }

    saveBillItemFinanceDetails(billItemId, line, qty, freeQty, purchaseRate, retailRate, netValue, grossValue,
            netRate, unitsPerPack, pbiQty, pbiFreeQty);

    evictLineCache();
    return billItemId;
}

private void saveBillItemFinanceDetails(long billItemId, PurchaseOrderRequestLineData line,
        BigDecimal qty, BigDecimal freeQty, BigDecimal purchaseRate, BigDecimal retailRate,
        BigDecimal netValue, BigDecimal grossValue, BigDecimal netRate, BigDecimal unitsPerPack,
        double quantityByUnits, double freeQuantityByUnits) {
    BillItemFinanceDetails bifd;
    if (line.getBillItemId() != null) {
        String jpql = "SELECT bi.billItemFinanceDetails FROM BillItem bi WHERE bi.id = :id";
        List<BillItemFinanceDetails> existing = em.createQuery(jpql, BillItemFinanceDetails.class)
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
```

Note the two differences from Phase 1's `saveLine`, both intentional and
required by this spec (§2 "remainingQty/remainingFreeQty" note): the PBI
UPDATE/INSERT here also sets `remainingQty=pbiQty, remainingFreeQty=pbiFreeQty`
inline (legacy's `saveBillComponent()` sets these explicitly on approve;
Phase 1's Request-side `saveLine` never needed them because a request line
has no "remaining" concept until it's approved).

- [ ] **Step 5: `retireZeroQtyApprovedLines` — copy verbatim from Phase 1, adjusted retire comment**

Copy `PurchaseOrderRequestNativeSqlService.retireZeroQtyLines()` exactly,
delegating the zero-qty predicate to
`purchaseOrderRequestNativeSqlService.isZeroQtyLine(qty, freeQty)` instead of
a local copy, and changing the retire comment string to `"Retired at
Approving PO"` (matching legacy's `saveBillComponent()` comment, distinct
from the Request page's `"Retired at Finalising PO"`).

```java
@SuppressWarnings("unchecked")
public int retireZeroQtyApprovedLines(long approvedBillId, long retirerId) {
    List<Object[]> rows = em.createNativeQuery(
        "SELECT bi.ID, pbi.qty, pbi.freeQty FROM " + billItemTable() + " bi "
        + "JOIN " + pharmBillItemTable() + " pbi ON pbi.billItem_ID = bi.ID "
        + "WHERE bi.bill_ID = ? AND bi.retired = 0")
        .setParameter(1, approvedBillId)
        .getResultList();

    int survivingCount = 0;
    Timestamp now = new Timestamp(new Date().getTime());
    for (Object[] row : rows) {
        long billItemId = ((Number) row[0]).longValue();
        double qty = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
        double freeQty = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

        if (purchaseOrderRequestNativeSqlService.isZeroQtyLine(qty, freeQty)) {
            em.createNativeQuery(
                "UPDATE " + billItemTable() + " SET retired=1, retirer_ID=?, retiredAt=?, retireComments=? WHERE ID=?")
                .setParameter(1, retirerId)
                .setParameter(2, now)
                .setParameter(3, "Retired at Approving PO")
                .setParameter(4, billItemId)
                .executeUpdate();
            em.createNativeQuery(
                "UPDATE " + pharmBillItemTable() + " SET retired=1, retirer_ID=?, retiredAt=? WHERE billItem_ID=?")
                .setParameter(1, retirerId)
                .setParameter(2, now)
                .setParameter(3, billItemId)
                .executeUpdate();
        } else {
            survivingCount++;
        }
    }
    evictLineCache();
    return survivingCount;
}
```

Note: unlike Phase 1's `retireZeroQtyLines`, the surviving branch here does
**not** re-set `remainingQty=qty, remainingFreeQty=freeQty` — Step 4 already
wrote those at save time (see the note above), so there is nothing left to
do for survivors here.

- [ ] **Step 6: `loadRequestedLines` — JPQL projection reading the requested bill's lines**

Returns `PurchaseOrderRequestLineData` per non-retired
`PharmaceuticalBillItem` on the requested bill. The `ampp` flag needs
`TYPE(bi.item)` — use a `CASE WHEN` JPQL expression (EclipseLink supports
this in a `SELECT NEW` constructor argument).

```java
public List<PurchaseOrderRequestLineData> loadRequestedLines(long requestedBillId) {
    String jpql = "SELECT NEW com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData("
            + "bi.item.id, "
            + "CASE WHEN TYPE(bi.item) = com.divudi.core.entity.pharmacy.Ampp THEN true ELSE false END, "
            + "bi.billItemFinanceDetails.quantity, "
            + "bi.billItemFinanceDetails.freeQuantity, "
            + "bi.billItemFinanceDetails.lineGrossRate, "
            + "bi.billItemFinanceDetails.retailSaleRate, "
            + "bi.billItemFinanceDetails.unitsPerPack, "
            + "bi.searialNo) "
            + "FROM PharmaceuticalBillItem p JOIN p.billItem bi "
            + "WHERE bi.bill.id = :billId AND bi.retired = false "
            + "ORDER BY bi.searialNo";
    return em.createQuery(jpql, PurchaseOrderRequestLineData.class)
            .setParameter("billId", requestedBillId)
            .getResultList();
}
```

This requires a matching constructor on `PurchaseOrderRequestLineData` —
Task 2 adds it (Phase 1's existing DTO only has a no-arg constructor +
setters; per CLAUDE.md, add a new constructor, never modify the existing
no-arg one).

If `CASE WHEN TYPE(...) = X` in a `SELECT NEW` constructor argument turns
out not to be supported by the EclipseLink version this project pins (verify
by running Step 8's test against a real DB before assuming it works), fall
back to: project `bi.item.id` only (drop the `ampp` boolean from the
`SELECT NEW`), then resolve `ampp` in the controller via `itemFacade.find(itemId) instanceof Ampp`
for each returned line before calling `saveApprovedLine`. Note whichever
approach is used in the task's completion report so Task 3 knows which
constructor shape to call.

- [ ] **Step 7: Compile**

Run: `mvn -q -o compile` (see build tool path notes in project memory /
`developer_docs/deployment/persistence-verification.md` if `mvn` is not on
PATH)
Expected: no errors. If Step 6's `CASE WHEN TYPE(...)` fails to compile/run
against a real query, apply the fallback noted in Step 6 and re-verify.

- [ ] **Step 8: Unit tests for the parts with no `EntityManager` dependency**

`saveApprovedLine`'s math is entirely covered by
`purchaseOrderRequestNativeSqlService.computeLineValues()`, already tested
in Phase 1 (`PurchaseOrderRequestNativeSqlServiceTest`) — do not re-test that
math here. This class's own testable surface is thin (everything else needs
a live `EntityManager`). Write one test confirming the zero-qty retire
delegation is wired correctly:

```java
package com.divudi.service.pharmacy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PurchaseOrderApprovingNativeSqlServiceTest {

    private final PurchaseOrderRequestNativeSqlService requestService = new PurchaseOrderRequestNativeSqlService();

    @Test
    void isZeroQtyLine_deferredToRequestService_trueWhenBothZero() {
        assertTrue(requestService.isZeroQtyLine(0.0, 0.0));
    }

    @Test
    void isZeroQtyLine_deferredToRequestService_falseWhenQtyPositive() {
        assertFalse(requestService.isZeroQtyLine(5.0, 0.0));
    }
}
```

Run: `mvn -q -o test "-Dtest=PurchaseOrderApprovingNativeSqlServiceTest" -DfailIfNoTests=false`
Expected: PASS. (This confirms the delegation target's behavior is stable;
`PurchaseOrderApprovingNativeSqlService` itself has no pure-logic surface
worth mocking an `EntityManager` for — its correctness is verified end-to-end
in Task 5's Playwright pass, matching Phase 1's own test-coverage shape.)

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/divudi/service/pharmacy/PurchaseOrderApprovingNativeSqlService.java src/test/java/com/divudi/service/pharmacy/PurchaseOrderApprovingNativeSqlServiceTest.java
git commit -m "feat(pharmacy): native SQL service for PO Approving bill/line writes"
```

---

### Task 2: `PurchaseOrderRequestLineData` — add the read-side constructor

**Files:**
- Modify: `src/main/java/com/divudi/core/data/dto/pharmacy/PurchaseOrderRequestLineData.java`
- Test: `src/test/java/com/divudi/core/data/dto/pharmacy/PurchaseOrderRequestLineDataTest.java`

**Interfaces:**
- Consumes: nothing new.
- Produces: `PurchaseOrderRequestLineData(Long itemId, boolean ampp, BigDecimal quantity, BigDecimal freeQuantity, BigDecimal purchaseRate, BigDecimal retailRate, BigDecimal unitsPerPack, int serialNo)`
  — used by Task 1 Step 6's JPQL `SELECT NEW`.

Per CLAUDE.md: **never modify the existing no-arg constructor** — this class
currently has only the implicit default constructor (all fields set via
setters). Add a new constructor; do not touch existing getters/setters.

- [ ] **Step 1: Add the new constructor**

```java
public PurchaseOrderRequestLineData(Long itemId, boolean ampp, BigDecimal quantity, BigDecimal freeQuantity,
        BigDecimal purchaseRate, BigDecimal retailRate, BigDecimal unitsPerPack, int serialNo) {
    this.itemId = itemId;
    this.ampp = ampp;
    this.quantity = quantity;
    this.freeQuantity = freeQuantity;
    this.purchaseRate = purchaseRate;
    this.retailRate = retailRate;
    this.unitsPerPack = unitsPerPack;
    this.serialNo = serialNo;
}
```

Place it immediately after the field declarations, before the getters —
matching the class's existing layout.

- [ ] **Step 2: Add a test for the new constructor**

Append to the existing test class (do not remove or modify
`gettersReturnValuesSetByConstructorArgs`, which tests the setter path):

```java
@Test
void projectionConstructorSetsOnlyProjectedFields() {
    var d = new PurchaseOrderRequestLineData(100L, true, BigDecimal.TEN, BigDecimal.ONE,
            BigDecimal.valueOf(12.5), BigDecimal.valueOf(15.0), BigDecimal.valueOf(10), 3);

    assertEquals(Long.valueOf(100L), d.getItemId());
    assertTrue(d.isAmpp());
    assertEquals(BigDecimal.TEN, d.getQuantity());
    assertEquals(BigDecimal.ONE, d.getFreeQuantity());
    assertEquals(BigDecimal.valueOf(12.5), d.getPurchaseRate());
    assertEquals(BigDecimal.valueOf(15.0), d.getRetailRate());
    assertEquals(BigDecimal.valueOf(10), d.getUnitsPerPack());
    assertEquals(3, d.getSerialNo());
    assertNull(d.getBillItemId());
    assertNull(d.getPharmaceuticalBillItemId());
    assertNull(d.getCreaterId());
}
```

Add `import static org.junit.jupiter.api.Assertions.assertTrue;` and
`import static org.junit.jupiter.api.Assertions.assertNull;` to the test
file's existing import block if not already present.

- [ ] **Step 3: Run the test**

Run: `mvn -q -o test "-Dtest=PurchaseOrderRequestLineDataTest" -DfailIfNoTests=false`
Expected: PASS (both the pre-existing test and the new one).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/core/data/dto/pharmacy/PurchaseOrderRequestLineData.java src/test/java/com/divudi/core/data/dto/pharmacy/PurchaseOrderRequestLineDataTest.java
git commit -m "feat(pharmacy): add projection constructor to PurchaseOrderRequestLineData"
```

---

### Task 3: `PurchaseOrderApprovingNativeSqlController` — page state, guards, approve flow

**Files:**
- Create: `src/main/java/com/divudi/bean/pharmacy/PurchaseOrderApprovingNativeSqlController.java`

**Interfaces:**
- Consumes: `PurchaseOrderApprovingNativeSqlService` (Task 1) — all its public
  methods. `PurchaseOrderRequestLineData` (Task 2) — both constructors.
  `BillFacade.find(Long)` / `BillFacade.edit(Bill)` for the requested-bill
  JPA read and the cross-link write. `BillNumberGenerator` (existing,
  unchanged) for approval bill-number generation. `WebUserController.hasPrivilege(String)`
  for the `PurchaseOrdersApprovel` privilege check.
- Produces: `String navigateToPurchaseOrderApproval(Long requestedBillId)`,
  `void removeItem(BillItem)`, `void removeSelected()`, `void onEdit(BillItem)`,
  `void approve()`, `void prepareEmailDialog()`, `void sendPurchaseOrderEmail()`,
  plus getters/setters for `requestedBill`, `approvedBill`, `billItems`,
  `selectedItems`, `printPreview`, `emailRecipient` — all bound by Task 4's page.

This class never calls the native service for anything touching
`requestedBill` — only `approvedBill`.

- [ ] **Step 1: Scaffold the controller class, fields, and authorization helper**

Copy the `isAuthorized(String action, String requiredPrivilege)` helper
pattern verbatim from `PurchaseOrderRequestNativeSqlController` (privilege
check + audit logging via `LOGGER.log(Level.WARNING, ...)` on denial), scoped
to the `PurchaseOrdersApprovel` privilege for the `APPROVE` action.

```java
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.data.MessageType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.EmailFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.EmailManagerEjb;
import com.divudi.service.pharmacy.PurchaseOrderApprovingNativeSqlService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * SessionScoped controller for the native-SQL Purchase Order Approving page.
 * Native SQL: the APPROVED bill's own create/update, billitem+PBI writes
 * (via the service). JPA (unchanged): the requested bill (read-only except
 * for the referenceBill cross-link write), rate/email infra.
 * Related issue: #22738
 */
@Named
@SessionScoped
public class PurchaseOrderApprovingNativeSqlController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(PurchaseOrderApprovingNativeSqlController.class.getName());

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ConfigOptionController configOptionController;
    @Inject
    private PharmacyController pharmacyController;

    @EJB
    private PurchaseOrderApprovingNativeSqlService purchaseOrderApprovingNativeSqlService;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private EmailFacade emailFacade;
    @EJB
    private EmailManagerEjb emailManagerEjb;

    private Bill requestedBill;
    private Bill approvedBill;
    private List<BillItem> billItems;
    private List<BillItem> selectedItems;
    private boolean printPreview;
    private String emailRecipient;

    private boolean isAuthorized(String action, String requiredPrivilege) {
        if (webUserController == null || sessionController == null) {
            LOGGER.log(Level.SEVERE, "Authorization failed - missing controllers: action={0}, userId=null, billId={1}",
                    new Object[]{action, requestedBill != null ? requestedBill.getId() : "null"});
            return false;
        }
        if (!webUserController.hasPrivilege(requiredPrivilege)) {
            Long userId = sessionController.getLoggedUser() != null ? sessionController.getLoggedUser().getId() : null;
            Long billId = requestedBill != null ? requestedBill.getId() : null;
            LOGGER.log(Level.WARNING, "SECURITY: Unauthorized Purchase Order Approving access attempt - action={0}, userId={1}, billId={2}, requiredPrivilege={3}",
                    new Object[]{action, userId, billId, requiredPrivilege});
            JsfUtil.addErrorMessage("You don't have permission to " + action.toLowerCase() + " purchase orders.");
            return false;
        }
        return true;
    }
}
```

- [ ] **Step 2: `navigateToPurchaseOrderApproval` with scope guard, `getApprovedBill`, `resetBillValues`**

The scope guard mirrors Phase 1's post-review fix on
`navigateToUpdatePurchaseOrder`: reject if not `BillTypeAtomic.PHARMACY_ORDER`,
retired, cancelled, or not owned by the caller's department. The
already-approved check (`referenceBill != null`) and `synchronized` come
directly from legacy — do not drop either.

```java
public void resetBillValues() {
    requestedBill = null;
    approvedBill = null;
    billItems = new ArrayList<>();
    selectedItems = null;
    printPreview = false;
}

public Bill getApprovedBill() {
    if (approvedBill == null) {
        approvedBill = new BilledBill();
        approvedBill.setBillType(BillType.PharmacyOrderApprove);
        approvedBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
        if (requestedBill != null) {
            approvedBill.setConsignment(requestedBill.isConsignment());
            approvedBill.setDepartmentType(requestedBill.getDepartmentType());
        }
    }
    return approvedBill;
}

// synchronized: see Task 3 doc comment on approve() for the production
// incident (GRN item duplication, PO/RH/GSK/26/01093) this guards against.
public synchronized String navigateToPurchaseOrderApproval(Long requestedBillId) {
    if (requestedBillId == null) {
        JsfUtil.addErrorMessage("No Bill");
        return "";
    }
    Bill bill = billFacade.find(requestedBillId);
    if (bill == null) {
        JsfUtil.addErrorMessage("Bill not found");
        return "";
    }
    if (bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_ORDER) {
        JsfUtil.addErrorMessage("Bill is not a finalized purchase order request");
        return "";
    }
    if (bill.isRetired() || bill.isCancelled()) {
        JsfUtil.addErrorMessage("Bill is retired or cancelled");
        return "";
    }
    if (bill.getDepartment() == null || sessionController.getLoggedUser() == null
            || !bill.getDepartment().equals(sessionController.getDepartment())) {
        JsfUtil.addErrorMessage("You are not authorized to view this purchase order");
        return "";
    }
    if (bill.getReferenceBill() != null) {
        JsfUtil.addErrorMessage("This purchase order is already approved");
        return "";
    }

    resetBillValues();
    requestedBill = bill;
    getApprovedBill().setPaymentMethod(requestedBill.getPaymentMethod());
    getApprovedBill().setToInstitution(requestedBill.getToInstitution());
    getApprovedBill().setCreditDuration(requestedBill.getCreditDuration());
    generateBillComponent();
    printPreview = false;
    return "/pharmacy/pharmacy_purhcase_order_approving_native?faces-redirect=true";
}
```

- [ ] **Step 3: `generateBillComponent` — seed billItems from the requested bill's lines**

Calls `purchaseOrderApprovingNativeSqlService.loadRequestedLines(requestedBill.getId())`
and builds in-memory `BillItem`/`PharmaceuticalBillItem`/`BillItemFinanceDetails`
from each returned `PurchaseOrderRequestLineData`. This step only seeds raw
inputs into fresh entities for display — it does not need
`computeLineValues()`; `saveApprovedLine` (Task 1 Step 4) does the actual
computation when `approve()` persists. Resolve `Item` by id via `itemFacade`
(inject `ItemFacade` alongside the other EJBs from Step 1).

```java
@EJB
private com.divudi.core.facade.ItemFacade itemFacade;

public void generateBillComponent() {
    billItems = new ArrayList<>();
    if (requestedBill == null) {
        return;
    }
    List<PurchaseOrderRequestLineData> lines = purchaseOrderApprovingNativeSqlService.loadRequestedLines(requestedBill.getId());
    for (PurchaseOrderRequestLineData line : lines) {
        Item item = itemFacade.find(line.getItemId());
        if (item == null) {
            continue;
        }
        BillItem bi = new BillItem();
        bi.setItem(item);
        bi.setSearialNo(line.getSerialNo());

        PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
        pbi.setBillItem(bi);
        pbi.setCreatedAt(new Date());
        pbi.setCreater(sessionController.getLoggedUser());
        bi.setPharmaceuticalBillItem(pbi);

        bi.getBillItemFinanceDetails().setQuantity(line.getQuantity());
        bi.getBillItemFinanceDetails().setFreeQuantity(line.getFreeQuantity());
        bi.getBillItemFinanceDetails().setLineGrossRate(line.getPurchaseRate());
        bi.getBillItemFinanceDetails().setRetailSaleRate(line.getRetailRate());
        bi.getBillItemFinanceDetails().setUnitsPerPack(line.getUnitsPerPack());

        recalculateLineValues(bi);
        billItems.add(bi);
    }
    calculateBillTotals();
}
```

- [ ] **Step 4: `onEdit`, `recalculateLineValues`, `calculateBillTotals` — line editing**

Mirrors Phase 1's `PurchaseOrderRequestNativeSqlController.onEdit()`/
`recalculateLineValues()`/`calculateBillTotals()` exactly (integer-qty gate
on the same config key, null-safe `getBillItemFinanceDetails()` guard per
Phase 1's post-review fix, serial renumbering). Per the design spec's §3
decision, there is **no** `updateCalculatedValues()` lighter path — always
call the single recompute helper.

```java
public void onEdit(BillItem bi) {
    if (bi.getBillItemFinanceDetails() != null
            && configOptionController.getBooleanValueByKey("Pharmacy Purchase - Quantity Must Be Integer", true)) {
        BigDecimal qty = bi.getBillItemFinanceDetails().getQuantity();
        BigDecimal freeQty = bi.getBillItemFinanceDetails().getFreeQuantity();
        if (qty != null && qty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            bi.getBillItemFinanceDetails().setQuantity(BigDecimal.ZERO);
            recalculateLineValues(bi);
            JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for quantity. Decimal values are not allowed.");
            calculateBillTotals();
            return;
        }
        if (freeQty != null && freeQty.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            bi.getBillItemFinanceDetails().setFreeQuantity(BigDecimal.ZERO);
            recalculateLineValues(bi);
            JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for free quantity. Decimal values are not allowed.");
            calculateBillTotals();
            return;
        }
    }
    recalculateLineValues(bi);
    calculateBillTotals();
}

private void recalculateLineValues(BillItem bi) {
    if (bi == null || bi.getBillItemFinanceDetails() == null) {
        return;
    }
    BigDecimal qty = bi.getBillItemFinanceDetails().getQuantity();
    BigDecimal purchaseRate = bi.getBillItemFinanceDetails().getLineGrossRate();
    if (qty == null) qty = BigDecimal.ZERO;
    if (purchaseRate == null) purchaseRate = BigDecimal.ZERO;

    BigDecimal grossValue = purchaseRate.multiply(qty);
    bi.setRate(purchaseRate.doubleValue());
    bi.setNetRate(purchaseRate.doubleValue());
    bi.setGrossValue(grossValue.doubleValue());
    bi.setNetValue(grossValue.doubleValue());
    bi.getBillItemFinanceDetails().setLineGrossTotal(grossValue);
    bi.getBillItemFinanceDetails().setLineNetTotal(grossValue);
}

private void calculateBillTotals() {
    double total = 0.0;
    int serialNo = 0;
    for (BillItem bi : billItems) {
        if (bi == null || bi.isRetired()) {
            continue;
        }
        bi.setSearialNo(serialNo++);
        total += bi.getNetValue();
    }
    getApprovedBill().setNetTotal(total);
    getApprovedBill().setTotal(total);
}
```

- [ ] **Step 5: `removeItem`, `removeSelected` — in-memory only**

Unlike Phase 1's Request page, there is no persisted intermediate state to
keep in sync (per the design spec §2/§3) — these mutate only the in-memory
`billItems` list, matching legacy exactly.

```java
public void removeItem(BillItem billItem) {
    if (billItem == null || !billItems.contains(billItem)) {
        JsfUtil.addErrorMessage("Item not found or already removed");
        return;
    }
    billItems.remove(billItem);
    calculateBillTotals();
}

public void removeSelected() {
    if (selectedItems == null || selectedItems.isEmpty()) {
        JsfUtil.addErrorMessage("No items selected to remove");
        return;
    }
    billItems.removeAll(selectedItems);
    calculateBillTotals();
    selectedItems = null;
}
```

- [ ] **Step 6: `approve` — validation, native bill+line writes, JPA cross-link**

This is the task's central method. Validation happens entirely in memory
before any write (matching the ordering-safety lesson from Phase 1's
review). The cross-link write (`requestedBill.setReferenceBill(approvedBill); billFacade.edit(requestedBill)`)
is the **only** JPA write to a native-owned bill row's FK in this whole
phase — do this exactly as shown, do not attempt to fold it into the native
service.

```java
// synchronized: a double-submit on the Approve button (no confirm-then-review
// gap, or a resubmitted ajax="false" postback) let two requests race through
// the same in-memory billItems list before either had persisted -- both saw
// BillItem.id == null and created every line twice, duplicating every GRN
// item (Ruhunu PO/RH/GSK/26/01093, same bug class as Phase 1's #21417 guard).
public synchronized void approve() {
    if (!isAuthorized("APPROVE", "PurchaseOrdersApprovel")) {
        return;
    }
    if (requestedBill == null) {
        JsfUtil.addErrorMessage("No Bill");
        return;
    }
    if (requestedBill.getReferenceBill() != null) {
        JsfUtil.addErrorMessage("This purchase order is already approved");
        return;
    }
    if (getApprovedBill().getPaymentMethod() == null) {
        JsfUtil.addErrorMessage("Select Paymentmethod");
        return;
    }
    if (billItems == null || billItems.isEmpty()) {
        JsfUtil.addErrorMessage("Please add bill items");
        return;
    }
    for (BillItem bi : billItems) {
        PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
        if (pbi == null) {
            JsfUtil.addErrorMessage("Missing pharmaceutical details for item: " + bi.getItem().getName());
            return;
        }
        double totalQty = bi.getBillItemFinanceDetails().getQuantity().doubleValue()
                + bi.getBillItemFinanceDetails().getFreeQuantity().doubleValue();
        if (totalQty <= 0) {
            JsfUtil.addErrorMessage("Item '" + bi.getItem().getName() + "' has zero quantity and free quantity");
            return;
        }
        if (bi.getBillItemFinanceDetails().getLineGrossRate() == null
                || bi.getBillItemFinanceDetails().getLineGrossRate().doubleValue() <= 0) {
            JsfUtil.addErrorMessage("Item '" + bi.getItem().getName() + "' has invalid purchase price");
            return;
        }
    }

    calculateBillTotals();

    String[] billNumbers = createAndAssignBillNumber();
    long approvedBillId = purchaseOrderApprovingNativeSqlService.createApprovedBill(
            requestedBill.getId(),
            sessionController.getLoggedUser().getDepartment().getId(),
            sessionController.getLoggedUser().getDepartment().getInstitution().getId(),
            requestedBill.getDepartment().getId(),
            requestedBill.getInstitution().getId(),
            sessionController.getLoggedUser().getId(),
            billNumbers[0],
            billNumbers[1]);
    approvedBill = billFacade.find(approvedBillId);
    approvedBill.setPaymentMethod(getApprovedBill().getPaymentMethod());
    approvedBill.setToInstitution(getApprovedBill().getToInstitution());
    approvedBill.setCreditDuration(getApprovedBill().getCreditDuration());
    approvedBill.setConsignment(getApprovedBill().isConsignment());
    approvedBill.setDepartmentType(getApprovedBill().getDepartmentType());

    purchaseOrderApprovingNativeSqlService.updateApprovedBillHeader(
            approvedBillId,
            approvedBill.getToInstitution() != null ? approvedBill.getToInstitution().getId() : null,
            approvedBill.getPaymentMethod(),
            approvedBill.getCreditDuration(),
            approvedBill.isConsignment(),
            approvedBill.getDepartmentType(),
            approvedBill.getComments(),
            sessionController.getLoggedUser().getId());

    for (BillItem bi : billItems) {
        PurchaseOrderRequestLineData line = new PurchaseOrderRequestLineData();
        line.setItemId(bi.getItem().getId());
        line.setAmpp(bi.getItem() instanceof Ampp);
        line.setQuantity(bi.getBillItemFinanceDetails().getQuantity());
        line.setFreeQuantity(bi.getBillItemFinanceDetails().getFreeQuantity());
        line.setPurchaseRate(bi.getBillItemFinanceDetails().getLineGrossRate());
        line.setRetailRate(bi.getBillItemFinanceDetails().getRetailSaleRate());
        line.setUnitsPerPack(bi.getBillItemFinanceDetails().getUnitsPerPack());
        line.setSerialNo(bi.getSearialNo());
        line.setCreaterId(sessionController.getLoggedUser().getId());
        purchaseOrderApprovingNativeSqlService.saveApprovedLine(approvedBillId, line);
    }

    purchaseOrderApprovingNativeSqlService.retireZeroQtyApprovedLines(approvedBillId, sessionController.getLoggedUser().getId());

    approvedBill = billFacade.find(approvedBillId);
    approvedBill.setApproveAt(new Date());
    approvedBill.setApproveUser(sessionController.getLoggedUser());
    billFacade.edit(approvedBill);

    // The one JPA write in this phase that touches a bill this controller
    // does not own the writes for -- required to stay JPA merge, never
    // native SQL, per the master issue's L2-cache-coherence rule.
    requestedBill.setReferenceBill(approvedBill);
    billFacade.edit(requestedBill);

    printPreview = true;
    JsfUtil.addSuccessMessage("Purchase order approved successfully.");
}

private String[] createAndAssignBillNumber() {
    String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_APPROVAL, "");
    if (billSuffix == null || billSuffix.trim().isEmpty()) {
        configOptionApplicationController.setLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_APPROVAL, "POA");
    }

    boolean stratInsDeptYear = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Approvals - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number", false);
    boolean stratInsYear = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Approvals - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);
    boolean stratInsIdInsYear = configOptionApplicationController.getBooleanValueByKey("Institution Number Generation Strategy for Purchase Order Approvals - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);

    String deptId;
    if (stratInsDeptYear) {
        deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
    } else if (stratInsYear) {
        deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
    } else {
        deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
    }

    String insId;
    if (stratInsIdInsYear) {
        insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
    } else {
        insId = deptId;
    }

    return new String[]{deptId, insId};
}
```

Note on `approveAt`/`approveUser`: legacy sets these directly on the entity
before a single `billFacade.edit(aprovedBill)` call (no native column for
them was written in Task 1's `createApprovedBill`/`updateApprovedBillHeader`
— intentionally, since this is a low-frequency, non-hot-path write happening
exactly once per approve). This is the same pragmatic pattern Phase 1 used
for fields not on the hot native-write path; do not add
`approveAt`/`approveUser` columns to the native INSERT/UPDATE in Task 1 to
"complete" the native conversion — that would be scope creep against the
spec's own reasoning.

- [ ] **Step 7: Email — `prepareEmailDialog`, `sendPurchaseOrderEmail`, `generatePurchaseOrderHtml`**

Copy Phase 1's final (post-review) `generatePurchaseOrderHtml()` +
`esc()` helper structure exactly, retargeted to `approvedBill` instead of
`currentBill`, with subject `"Purchase Order"` (matching legacy's
`PurchaseOrderController`, not `"Purchase Order Request"`).

```java
private static String esc(String value) {
    return value != null ? org.apache.commons.text.StringEscapeUtils.escapeHtml4(value) : "";
}

public void prepareEmailDialog() {
    if (approvedBill == null) {
        JsfUtil.addErrorMessage("No Bill");
        return;
    }
    if (approvedBill.getToInstitution() != null && approvedBill.getToInstitution().getEmail() != null) {
        emailRecipient = approvedBill.getToInstitution().getEmail();
    } else {
        emailRecipient = "";
    }
}

public void sendPurchaseOrderEmail() {
    if (approvedBill == null) {
        JsfUtil.addErrorMessage("No Bill");
        return;
    }
    if (emailRecipient == null || emailRecipient.trim().isEmpty()) {
        JsfUtil.addErrorMessage("Please enter recipient email");
        return;
    }
    String recipient = emailRecipient.trim();
    if (!CommonFunctions.isValidEmail(recipient)) {
        JsfUtil.addErrorMessage("Please enter a valid email address");
        return;
    }
    String body = generatePurchaseOrderHtml();
    if (body == null) {
        JsfUtil.addErrorMessage("Could not generate email body");
        return;
    }

    AppEmail email = new AppEmail();
    email.setCreatedAt(new Date());
    email.setCreater(sessionController.getLoggedUser());
    email.setReceipientEmail(recipient);
    email.setMessageSubject("Purchase Order");
    email.setMessageBody(body);
    email.setDepartment(sessionController.getLoggedUser().getDepartment());
    email.setInstitution(sessionController.getLoggedUser().getInstitution());
    email.setBill(approvedBill);
    email.setMessageType(MessageType.Marketing);
    email.setSentSuccessfully(false);
    email.setPending(true);
    emailFacade.create(email);

    try {
        boolean success = emailManagerEjb.sendEmail(
                java.util.Collections.singletonList(recipient), body, "Purchase Order", true);
        email.setSentSuccessfully(success);
        email.setPending(!success);
        if (success) {
            email.setSentAt(new Date());
            JsfUtil.addSuccessMessage("Email Sent Successfully");
        } else {
            JsfUtil.addErrorMessage("Sending Email Failed");
        }
        emailFacade.edit(email);
    } catch (Exception ex) {
        JsfUtil.addErrorMessage("Sending Email Failed");
    }
}

private String generatePurchaseOrderHtml() {
    try {
        if (approvedBill == null) {
            return null;
        }
        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Purchase Order</title></head><body>");
        html.append("<div style='font-family: Arial, sans-serif; padding: 20px;'>");

        if (approvedBill.getCreater() != null && approvedBill.getCreater().getInstitution() != null) {
            html.append("<div style='text-align: center; margin-bottom: 20px;'>");
            html.append("<h2>").append(esc(approvedBill.getCreater().getInstitution().getName())).append("</h2>");
            if (approvedBill.getCreater().getInstitution().getAddress() != null) {
                html.append("<p>").append(esc(approvedBill.getCreater().getInstitution().getAddress())).append("</p>");
            }
            if (approvedBill.getCreater().getInstitution().getPhone() != null) {
                html.append("<p>Phone: ").append(esc(approvedBill.getCreater().getInstitution().getPhone())).append("</p>");
            }
            html.append("</div>");
        }

        html.append("<h3 style='text-align: center; text-decoration: underline;'>Purchase Order</h3>");
        html.append("<table style='width: 100%; margin-bottom: 20px;'>");
        html.append("<tr><td><strong>Order No:</strong></td><td>").append(esc(approvedBill.getDeptId())).append("</td></tr>");
        if (approvedBill.getDepartment() != null) {
            html.append("<tr><td><strong>Order Department:</strong></td><td>").append(esc(approvedBill.getDepartment().getName())).append("</td></tr>");
        }
        if (approvedBill.getToInstitution() != null) {
            html.append("<tr><td><strong>Supplier:</strong></td><td>").append(esc(approvedBill.getToInstitution().getName())).append("</td></tr>");
            html.append("<tr><td><strong>Supplier Code:</strong></td><td>").append(esc(approvedBill.getToInstitution().getCode())).append("</td></tr>");
            if (approvedBill.getToInstitution().getPhone() != null) {
                html.append("<tr><td><strong>Supplier Phone:</strong></td><td>").append(esc(approvedBill.getToInstitution().getPhone())).append("</td></tr>");
            }
            if (approvedBill.getToInstitution().getAddress() != null) {
                html.append("<tr><td><strong>Supplier Address:</strong></td><td>").append(esc(approvedBill.getToInstitution().getAddress())).append("</td></tr>");
            }
        }
        html.append("<tr><td><strong>Payment Method:</strong></td><td>").append(approvedBill.getPaymentMethod() != null ? approvedBill.getPaymentMethod().toString() : "").append("</td></tr>");
        html.append("<tr><td><strong>Consignment:</strong></td><td>").append(approvedBill.isConsignment() ? "Yes" : "No").append("</td></tr>");
        html.append("</table>");

        html.append("<table border='1' style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
        html.append("<thead style='background-color: #f0f0f0;'>");
        html.append("<tr><th style='padding: 8px;'>Item Code</th><th style='padding: 8px;'>Item Name</th>");
        html.append("<th style='padding: 8px;'>Qty</th><th style='padding: 8px;'>Free Qty</th>");
        html.append("<th style='padding: 8px;'>Purchase Rate</th><th style='padding: 8px;'>Purchase Value</th></tr></thead><tbody>");

        if (billItems != null) {
            for (BillItem bi : billItems) {
                if (bi != null && !bi.isRetired() && bi.getItem() != null) {
                    html.append("<tr>");
                    html.append("<td style='padding: 8px;'>").append(esc(bi.getItem().getCode())).append("</td>");
                    html.append("<td style='padding: 8px;'>").append(esc(bi.getItem().getName())).append("</td>");
                    html.append("<td style='padding: 8px; text-align: right;'>");
                    if (bi.getPharmaceuticalBillItem() != null) {
                        html.append(String.format("%,.0f", bi.getPharmaceuticalBillItem().getQty()));
                    }
                    html.append("</td><td style='padding: 8px; text-align: right;'>");
                    if (bi.getPharmaceuticalBillItem() != null) {
                        html.append(String.format("%,.0f", bi.getPharmaceuticalBillItem().getFreeQty()));
                    }
                    html.append("</td><td style='padding: 8px; text-align: right;'>");
                    if (bi.getPharmaceuticalBillItem() != null) {
                        html.append(String.format("%,.2f", bi.getPharmaceuticalBillItem().getPurchaseRate()));
                    }
                    html.append("</td><td style='padding: 8px; text-align: right;'>").append(String.format("%,.2f", bi.getNetValue())).append("</td>");
                    html.append("</tr>");
                }
            }
        }

        html.append("</tbody><tfoot style='font-weight: bold;'><tr>");
        html.append("<td colspan='5' style='padding: 8px; text-align: right;'>Net Total:</td>");
        html.append("<td style='padding: 8px; text-align: right;'>").append(String.format("%,.2f", approvedBill.getNetTotal())).append("</td>");
        html.append("</tr></tfoot></table>");

        html.append("<div style='margin-top: 20px;'>");
        if (approvedBill.getCreater() != null && approvedBill.getCreater().getWebUserPerson() != null) {
            html.append("<p><strong>Order Initiated By:</strong> ").append(esc(approvedBill.getCreater().getWebUserPerson().getName())).append("</p>");
        }
        if (approvedBill.getCheckedBy() != null) {
            html.append("<p><strong>Order Finalized By:</strong> ").append(esc(approvedBill.getCheckedBy().getName())).append("</p>");
        }
        if (approvedBill.getCheckeAt() != null) {
            html.append("<p><strong>Order Finalized At:</strong> ").append(CommonFunctions.formatDate(approvedBill.getCheckeAt(), "dd/MM/yyyy HH:mm:ss")).append("</p>");
        }
        html.append("<p><strong>Generated At:</strong> ").append(CommonFunctions.formatDate(new Date(), "dd/MM/yyyy HH:mm:ss")).append("</p>");
        html.append("<p><strong>Total:</strong> ").append(String.format("%,.2f", approvedBill.getNetTotal())).append("</p>");
        html.append("</div></div></body></html>");
        return html.toString();
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error generating purchase order HTML", e);
        return null;
    }
}
```

- [ ] **Step 8: `displayItemDetails`, `onFocus`, getters/setters**

```java
public void displayItemDetails(BillItem bi) {
    pharmacyController.fillItemDetails(bi.getItem());
}

public void onFocus(BillItem bi) {
    pharmacyController.setPharmacyItem(bi.getItem());
}

public Bill getRequestedBill() {
    return requestedBill;
}

public List<BillItem> getBillItems() {
    if (billItems == null) {
        billItems = new ArrayList<>();
    }
    return billItems;
}

public void setBillItems(List<BillItem> billItems) {
    this.billItems = billItems;
}

public List<BillItem> getSelectedItems() {
    return selectedItems;
}

public void setSelectedItems(List<BillItem> selectedItems) {
    this.selectedItems = selectedItems;
}

public boolean isPrintPreview() {
    return printPreview;
}

public void setPrintPreview(boolean printPreview) {
    this.printPreview = printPreview;
}

public String getEmailRecipient() {
    return emailRecipient;
}

public void setEmailRecipient(String emailRecipient) {
    this.emailRecipient = emailRecipient;
}
```

Note: `getPrintPreview()`/`printPreview` primitive boolean getter matches
legacy's `getPrintPreview()` naming (not `isPrintPreview()` as Phase 1's
Request controller uses) — the XHTML in Task 4 must call whichever name is
actually declared here; use `isPrintPreview()` per this project's
[[feedback_primitive_boolean_rendered_guards]] convention (`is` prefix for
primitive boolean getters), and reference it as such in Task 4's page.

- [ ] **Step 9: Compile**

Run: `mvn -q -o compile`
Expected: no errors.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/PurchaseOrderApprovingNativeSqlController.java
git commit -m "feat(pharmacy): native SQL controller for PO Approving page"
```

---

### Task 4: `pharmacy_purhcase_order_approving_native.xhtml` — page

**Files:**
- Create: `src/main/webapp/pharmacy/pharmacy_purhcase_order_approving_native.xhtml`

**Interfaces:**
- Consumes: every public method/getter/setter from Task 3's controller.

Copy `pharmacy_purhcase_order_approving.xhtml` (444 lines) as the starting
point, then:

- [ ] **Step 1: Copy the legacy file and do a global EL rebind**

```bash
cp src/main/webapp/pharmacy/pharmacy_purhcase_order_approving.xhtml src/main/webapp/pharmacy/pharmacy_purhcase_order_approving_native.xhtml
```

Then replace every `purchaseOrderController.` with
`purchaseOrderApprovingNativeSqlController.` and every
`purchaseOrderController.aprovedBill` with
`purchaseOrderApprovingNativeSqlController.approvedBill` (note: this plan's
controller field is spelled `approvedBill`, not legacy's `aprovedBill` typo —
per CLAUDE.md's backward-compatibility rule, that rule protects **database**
column/field names with existing data, not a page-local Java bean property
name being introduced fresh in this new controller; there is no compatibility
reason to carry the typo into new code).

- [ ] **Step 2: Add the "Legacy View" fallback button**

Insert immediately after the opening `<h:form id="form">`, matching Phase
1's exact pattern:

```xml
<div class="d-flex justify-content-end">
    <!-- Small non-prominent fallback to legacy entity-based page -->
    <p:commandButton
        ajax="false"
        value="Legacy View"
        title="Open original entity-based Purchase Order Approving page"
        action="pharmacy_purhcase_order_approving?faces-redirect=true"
        class="ui-button-secondary ms-3"
        icon="fas fa-history"
        style="font-size: 0.75rem; padding: 0.2rem 0.5rem;"/>
</div>
```

- [ ] **Step 3: Fix the `p:ajax` listener wiring for qty/freeQty (the two-argument-mismatch bug)**

Legacy's qty/freeQty columns bind `p:ajax`/`f:ajax` `listener="#{purchaseOrderController.onEdit(bi)}"`
with `process="@this price"` — this pattern is copied as-is (Phase 1's review
confirmed this same mixed `p:ajax`/`f:ajax` pattern is intentional legacy
behavior, not a bug, when it appeared on the Request page). No functional
change here beyond the EL rebind from Step 1.

- [ ] **Step 4: Update the `rendered`/`disabled` checks on Approve and Remove buttons**

Legacy's Approve button `disabled` check
(`!webUserController.hasPrivilege('PurchaseOrdersApprovel')`) stays as-is —
this is a UI-only convenience disable; the controller's own `isAuthorized()`
check is the actual enforcement (Phase 1's review flagged the equivalent
Remove buttons on the Request page as **missing** a `checked`-state disable;
this page has no equivalent "already approved" UI disable on Remove buttons
either — since navigating here at all is blocked once
`requestedBill.getReferenceBill() != null` per Task 3 Step 2's guard, there
is no reachable state where Remove is clickable on an approved order, so no
new disable condition is needed here, unlike Phase 1's `checked` mid-workflow
state).

- [ ] **Step 5: Verify print/email dialog and print-config dialog blocks are unchanged**

These reference `purchaseOrderController`/`purchaseOrderConfigController`
for print-format config — the `purchaseOrderConfigController` (config
storage for paper format checkboxes) is a **separate, unrelated** controller
not part of this migration; only its `bill="#{purchaseOrderApprovingNativeSqlController.approvedBill}"`
binding on the `ph:po_custom_*` composites needs the EL rebind from Step 1.
Do not modify `purchaseOrderConfigController`-related markup beyond the
mechanical rebind.

- [ ] **Step 6: Manual review pass — diff against the legacy file**

Run: `git diff --no-index src/main/webapp/pharmacy/pharmacy_purhcase_order_approving.xhtml src/main/webapp/pharmacy/pharmacy_purhcase_order_approving_native.xhtml`
Expected: every hunk is either (a) an EL rebind (`purchaseOrderController` →
`purchaseOrderApprovingNativeSqlController`, `aprovedBill` → `approvedBill`),
or (b) the new Legacy View button block from Step 2. No line of legacy
markup should be silently dropped — this file has no `- [ ]` sub-steps for
"copy XYZ block" precisely because the whole file is one copy-then-rebind
operation; use this diff as the actual verification.

- [ ] **Step 7: Commit**

```bash
git add src/main/webapp/pharmacy/pharmacy_purhcase_order_approving_native.xhtml
git commit -m "feat(pharmacy): native SQL Purchase Order Approving page"
```

---

### Task 5: List-to-Approve button rewiring + navigation route registration

**Files:**
- Modify: `src/main/webapp/pharmacy/pharmacy_purhcase_order_list_to_approve_dto.xhtml`

**Interfaces:**
- Consumes: `PurchaseOrderApprovingNativeSqlController.navigateToPurchaseOrderApproval(Long)` (Task 3).

- [ ] **Step 1: Rewire the Approve button**

Per the design spec §6, change:

```xml
action="#{purchaseOrderController.navigateToPurchaseOrderApproval}">
    <f:setPropertyActionListener target="#{purchaseOrderController.requestedBillId}" value="#{dto.billId}"/>
```

to:

```xml
action="#{purchaseOrderApprovingNativeSqlController.navigateToPurchaseOrderApproval(dto.billId)}">
```

Grep the file first to confirm the exact surrounding markup and attribute
order before editing (line numbers may differ from the design spec's §6
excerpt, which was based on this session's earlier read).

- [ ] **Step 2: Compile-equivalent check (JSF-only change)**

Per CLAUDE.md: "JSF-only changes (XHTML only, no Java) do not require
compilation or testing." Skip straight to Task 6's Playwright walkthrough,
which exercises this button directly.

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/pharmacy/pharmacy_purhcase_order_list_to_approve_dto.xhtml
git commit -m "feat(pharmacy): route List-to-Approve button to native PO Approving page"
```

---

### Task 6: Playwright walkthrough + whole-branch review

Not a code task — this is the design spec's §7 review gate, required by the
master issue (#22726) before this phase can be considered done.

- [ ] **Step 1: Rebuild and redeploy locally**

Follow `developer_docs/testing/playwright-e2e-workflow.md` §0a. This
invalidates the current session — redeploy before logging in for the
walkthrough.

- [ ] **Step 2: End-to-end walkthrough**

Using the `playwright-e2e` skill: finalize a PO Request (or use an existing
finalized one), navigate to List-to-Approve, click Approve, verify:
- The native Approving page loads with the request's lines pre-populated
  and correct (qty, free qty, purchase rate, retail rate all match what was
  finalized)
- Edit a line's qty/rate, confirm the integer-only gate and total
  recalculation both work
- Remove a line via both the row button and multi-select "Remove All"
- Attempt to approve with a missing payment method — confirm the error
- Attempt to re-approve an already-approved order (e.g. by revisiting the
  URL after a successful approve) — confirm the "already approved" guard
  fires
- Approve successfully — confirm: the approved bill is created, the
  requested bill's `referenceBill` is set (verify via DB query, not just UI),
  the print preview renders, "Send Email" works, "Legacy View" link works
- Verify in the database (per the workflow doc §6): `bill` row for the new
  approval exists with correct `BILLTYPEATOMIC`/`billType`/FKs; `billitem`/
  `pharmaceuticalbillitem` rows exist per line with correct
  `remainingQty`/`remainingFreeQty`; the requested bill's `referenceBill_ID`
  points at the new approved bill's `ID`

- [ ] **Step 2a: If DB lacks suitable test data, generate it through the app**

Per the workflow doc §15 — never fall back to "code looks correct" as
evidence. Use the native Request page (Phase 1) to create and finalize a
fresh PO Request if no unapproved finalized request exists in the local DB.

- [ ] **Step 3: Second-agent / whole-branch review**

Dispatch a review of the full branch diff (all 5 prior tasks together),
focused specifically on: any legacy button/function from
`PurchaseOrderController.java` / `pharmacy_purhcase_order_approving.xhtml`
with no native equivalent; the cross-link write's correctness; the
synchronized/already-approved guard placement; HTML-escaping completeness in
the email generator (per Phase 1's own review history, this is the finding
category most likely to recur if skipped early).

- [ ] **Step 4: Fix any findings, re-verify, then hand off per master issue process**

Same PR/CodeRabbit/reply workflow as Phase 1 (#22733) — open the PR against
`development`, address CodeRabbit findings inline with replies, push fix
commits, re-review, merge.
