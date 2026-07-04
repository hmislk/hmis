# Item/Service Request Approval Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the bespoke, stock/billing-duplicating "Approve" transaction in the Item/Service Request workflow (issue #21793) with navigation into the real, unmodified "Add Services & Investigations" and "Direct Issue to BHTs (Native SQL)" pages, pre-loaded with the request's details, so approved requests become ordinary bills traceable back to the request.

**Architecture:** No new stateless "settle" service is introduced. Two existing session-scoped controllers (`BillBhtController`, `InpatientDirectIssueNativeSqlController`) get a small new entry point each that seeds their existing cart state from a request's remaining lines and tags each seeded line with the originating request `BillItem`. Their existing, unmodified save/settle methods get one small addition each: propagate that tag onto the persisted `BillItem.referanceBillItem` and the bill's `referenceBill`. `ItemRequestApprovalController`/`ItemRequestApiService` are rewritten to compute "remaining lines" per request (lines with no other `BillItem` referencing them) instead of running a single all-in-one approval transaction.

**Tech Stack:** Java EE (JSF/PrimeFaces, JAX-RS, JPA/EclipseLink), Payara, MySQL, Maven.

## Global Constraints

- JPQL first, native SQL last (CLAUDE.md rule 9): the one native-SQL touch point in this plan (Task 3) extends an *existing*, already performance-justified native-SQL path (`InpatientDirectIssueNativeSqlService`) by one column — it does not introduce a new native query. All new lookups added in this plan (Task 3's stock-by-item query) use JPQL.
- Never modify existing constructors — only add new ones (CLAUDE.md rule 8). Not triggered by this plan (no entity constructors touched), but any DTO constructor added must delegate via `this(...)` if an existing one exists.
- `findLongByJpql` (not `findDoubleByJpql`) for any `COUNT(...)` JPQL (CLAUDE.md rule 9a). Not directly triggered, but keep in mind if adding count queries for the pending-list UI.
- No new privileges — reuse `InwardServiceItemRequestApproval` and `InwardServiceItemRequestRejection` as-is (see spec §"Privileges").
- This feature never reached production — no backward-compatible data migration is needed. `BillTypeAtomic.INWARD_SERVICE_ITEM_APPROVAL`/`_CANCELLATION` and `BillType.InwardServiceItemApproval` are deleted outright (Task 8).
- Verification in this codebase is manual build/redeploy + Playwright + DB checks (no JUnit suite exists for this module) — every task's "test" step follows that pattern, per `developer_docs/testing/playwright-e2e-workflow.md` and this project's established iterative dev/test loop.
- Persistence.xml stays in local JNDI mode (`jdbc/coop`) throughout — do not touch it as part of this work.

---

## File structure

| File | Responsibility |
|---|---|
| `src/main/java/com/divudi/core/entity/BillEntry.java` | **Modify.** Add transient `sourceRequestBillItem` tag field. |
| `src/main/java/com/divudi/core/data/dto/BillItemData.java` | **Modify.** Add `sourceRequestBillItemId` field. |
| `src/main/java/com/divudi/bean/inward/BillBhtController.java` | **Modify.** New `navigateToAddServicesFromItemRequest(...)`; thread the request-line tag through `settleBill()`/`saveBill()`/`saveBillItems()`. |
| `src/main/java/com/divudi/bean/pharmacy/InpatientDirectIssueNativeSqlController.java` | **Modify.** New `navigateToDirectIssueFromItemRequest(...)`; new JPQL stock lookup by item id; thread the tag onto `BillItemData`. |
| `src/main/java/com/divudi/service/pharmacy/InpatientDirectIssueNativeSqlService.java` | **Modify.** `settle()`'s native `BillItem` INSERT gains a `referanceBillItem_ID` column. |
| `src/main/java/com/divudi/bean/inward/ItemRequestApprovalController.java` | **Modify.** Replace `approve()` with `getRemainingServiceLines`/`getRemainingInventoryLines`/`processServices`/`processInventory`; `reject()` cancels only remaining lines. |
| `src/main/java/com/divudi/service/inward/ItemRequestApiService.java` | **Modify.** Delete `approveRequest()` and its now-dead helpers; `rejectRequest()` cancels only remaining lines; status/response building becomes per-line. |
| `src/main/java/com/divudi/core/data/dto/itemrequest/ItemRequestResponseDTO.java` | **Modify.** Replace `approvalBillId` with a list of fulfilling-bill references; add per-line status. |
| `src/main/java/com/divudi/core/data/dto/itemrequest/ItemRequestLineResponseDTO.java` | **Modify.** Add per-line `status`, `fulfillingBillId`, `fulfillingBillType`. |
| `src/main/webapp/inward/item_request_pending_list.xhtml` | **Modify.** Replace the single "Approve" button with two conditional "Process…" buttons; reject applies to remaining lines only. |
| `src/main/java/com/divudi/core/data/BillTypeAtomic.java` | **Modify.** Delete `INWARD_SERVICE_ITEM_APPROVAL` and `INWARD_SERVICE_ITEM_APPROVAL_CANCELLATION`. |
| `src/main/java/com/divudi/core/data/BillType.java` | **Modify.** Delete `InwardServiceItemApproval`. |
| `src/main/java/com/divudi/bean/report/PharmacyReportController.java` | **Modify.** Remove the `INWARD_SERVICE_ITEM_APPROVAL` special-case in `calculateBhtIssueValue()` (#21266 patch, no longer needed). |

---

## Task 1: Linkage fields — `BillEntry` and `BillItemData`

**Files:**
- Modify: `src/main/java/com/divudi/core/entity/BillEntry.java`
- Modify: `src/main/java/com/divudi/core/data/dto/BillItemData.java`

**Interfaces:**
- Produces: `BillEntry.getSourceRequestBillItem()/setSourceRequestBillItem(BillItem)` — transient, not persisted, used by Task 2.
- Produces: `BillItemData.getSourceRequestBillItemId()/setSourceRequestBillItemId(Long)` — used by Task 3.

- [ ] **Step 1: Add the transient tag field to `BillEntry`**

In `src/main/java/com/divudi/core/entity/BillEntry.java`, add near the other `@Transient` fields (after line 38, `lsyBillItems`):

```java
    @Transient
    BillItem sourceRequestBillItem;
```

And add the accessor pair anywhere in the getter/setter block (e.g. after `getLsyBillItems()`/`setLsyBillItems()`, around line 104):

```java
    public BillItem getSourceRequestBillItem() {
        return sourceRequestBillItem;
    }

    public void setSourceRequestBillItem(BillItem sourceRequestBillItem) {
        this.sourceRequestBillItem = sourceRequestBillItem;
    }
```

- [ ] **Step 2: Add the id field to `BillItemData`**

Open `src/main/java/com/divudi/core/data/dto/BillItemData.java`. After the existing `institutionId` field (the last field in the "Institution/Department IDs" block), add:

```java
    // ---- Linkage back to an originating ItemRequest line (issue #21793 redesign) ----
    private Long sourceRequestBillItemId;
```

And add the accessor pair alongside the other getters/setters:

```java
    public Long getSourceRequestBillItemId() {
        return sourceRequestBillItemId;
    }

    public void setSourceRequestBillItemId(Long sourceRequestBillItemId) {
        this.sourceRequestBillItemId = sourceRequestBillItemId;
    }
```

- [ ] **Step 3: Compile**

Run: `cd /home/buddhika/development/rh && mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`, no errors referencing `BillEntry` or `BillItemData`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/core/entity/BillEntry.java src/main/java/com/divudi/core/data/dto/BillItemData.java
git commit -m "feat(inward): add request-line linkage fields to BillEntry/BillItemData

Part of the #21793 approval redesign — carries a tag from a pre-loaded
cart line back to the originating item-request BillItem so the save
path can set BillItem.referanceBillItem."
```

---

## Task 2: `BillBhtController` — pre-load from a request and thread the linkage through save

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/BillBhtController.java`

**Interfaces:**
- Consumes: `BillEntry.getSourceRequestBillItem()/setSourceRequestBillItem(BillItem)` (Task 1).
- Produces: `BillBhtController.navigateToAddServicesFromItemRequest(Bill itemRequest, List<BillItem> remainingLines)` → `String` (navigation outcome) — called by Task 4.
- Consumes/produces: existing `patientEncounter`, `currentBillItem`, `currentBillItemQty`, `lstBillEntries`, `addToBill()`, `settleBill()`, `saveBill(...)`, `saveBillItems(...)` — unchanged signatures, behavior extended.

- [ ] **Step 1: Add the pre-load navigation method**

In `BillBhtController.java`, add this method right after `navigateToAddServicesFromAdmissionProfile()` (after line 344, the closing brace of that method):

```java
    /**
     * Entry point from the Item/Service Request pending queue (issue #21793
     * redesign): seeds this controller's cart with the request's still-unfulfilled
     * lines, exactly as if the user had searched for and added each one manually,
     * then navigates to the same Add Services page. The user can edit/add/remove
     * before clicking the page's own Save button.
     */
    public String navigateToAddServicesFromItemRequest(Bill itemRequest, List<BillItem> remainingLines) {
        navigateToAddServicesFromAdmissionProfile();
        setPatientEncounter(itemRequest.getPatientEncounter());
        for (BillItem requestLine : remainingLines) {
            BillItem seed = new BillItem();
            seed.setItem(requestLine.getItem());
            setCurrentBillItem(seed);
            setCurrentBillItemQty(requestLine.getQty());
            if (requestLine.getItem() != null && requestLine.getItem().getClass() == Investigation.class) {
                seed.setBillTime(new Date());
            }
            int sizeBefore = lstBillEntries.size();
            addToBill();
            if (lstBillEntries.size() > sizeBefore) {
                lstBillEntries.get(lstBillEntries.size() - 1).setSourceRequestBillItem(requestLine);
            }
        }
        return "/inward/inward_bill_service?faces-redirect=true";
    }
```

- [ ] **Step 2: Thread the tag through `saveBill(...)` so the created `Bill` gets `referenceBill`**

In `saveBill(Department bt, BilledBill temp, Department matrixDepartment)` (starts at line 654), the method currently has no knowledge of `lstBillEntries`. Change `putToBills(...)` and `settleBill(Department, PaymentMethod)` to set `referenceBill` once, right after each `Bill` is created, based on whether any of its `BillEntry`s carry a tag. Replace the body of `putToBills` (lines 428-452) with:

```java
    public void putToBills(Department matrixDepartment, PaymentMethod paymentMethod) {

        Set<Department> billDepts = new HashSet<>();
        for (BillEntry e : lstBillEntries) {
            billDepts.add(e.getBillItem().getItem().getDepartment());
        }
        for (Department d : billDepts) {
            BilledBill myBill = new BilledBill();
            saveBill(d, myBill, matrixDepartment);
            List<BillEntry> tmp = new ArrayList<>();
            for (BillEntry e : lstBillEntries) {
                if (e.getBillItem().getItem().getDepartment().equals(d)) {
                    tmp.add(e);
                }
            }
            applyItemRequestReference(myBill, tmp);
            List<BillItem> tmpBis = saveBillItems(myBill, tmp, getSessionController().getLoggedUser(), matrixDepartment, paymentMethod);
            for (int i = 0; i < tmpBis.size(); i++) {
                tmpBis.get(i).setSearialNo(i);
            }
            getBillBean().calculateBillItems(myBill, tmp);
            myBill.setBillItems(tmpBis);
            getBills().add(myBill);
        }

    }

    /**
     * If any of the entries being saved onto this bill originated from an
     * Item/Service Request line (issue #21793 redesign), set the bill's
     * referenceBill so the request stays traceable to the bill it produced.
     * Each such entry's originating request BillItem is threaded onto the
     * real BillItem in {@link #saveBillItems(Bill, BillItem, BillEntry, List, WebUser, Department)}.
     */
    private void applyItemRequestReference(Bill bill, List<BillEntry> entries) {
        for (BillEntry e : entries) {
            if (e.getSourceRequestBillItem() != null && e.getSourceRequestBillItem().getBill() != null) {
                bill.setReferenceBill(e.getSourceRequestBillItem().getBill());
                return;
            }
        }
    }
```

Then update `settleBill(Department matrixDepartment, PaymentMethod paymentMethod)` (lines 561-592) — the single-bill branch (`if (getBillBean().calculateNumberOfBillsPerOrder(...) == 1)`) needs the same call. Change:

```java
            Bill b = saveBill(lstBillEntries.get(0).getBillItem().getItem().getDepartment(), temp, matrixDepartment);

            List<BillItem> list = saveBillItems(b, getLstBillEntries(), getSessionController().getLoggedUser(), matrixDepartment, paymentMethod);
```

to:

```java
            Bill b = saveBill(lstBillEntries.get(0).getBillItem().getItem().getDepartment(), temp, matrixDepartment);
            applyItemRequestReference(b, getLstBillEntries());

            List<BillItem> list = saveBillItems(b, getLstBillEntries(), getSessionController().getLoggedUser(), matrixDepartment, paymentMethod);
```

- [ ] **Step 3: Thread the tag onto the persisted `BillItem.referanceBillItem`**

In the single-`BillItem` overload of `saveBillItems` (lines 454-479), which every entry passes through, add the linkage right after the item is created (after line 467, the `if (billItem.getId() == null) { getBillItemFacade().create(billItem); }` block):

```java
    public BillItem saveBillItems(Bill bill, BillItem billItem, BillEntry billEntry, List<BillFee> billFees, WebUser wu, Department matrixDepartment) {

        billItem.setCreatedAt(new Date());
        billItem.setCreater(wu);
        billItem.setBill(bill);

        if (billItem.getInwardChargeType() == null && billItem.getItem() != null
                && billItem.getItem().getInwardChargeType() != null) {
            billItem.setInwardChargeType(billItem.getItem().getInwardChargeType());
        }

        if (billEntry != null && billEntry.getSourceRequestBillItem() != null) {
            billItem.setReferanceBillItem(billEntry.getSourceRequestBillItem());
        }

        if (billItem.getId() == null) {
            getBillItemFacade().create(billItem);
        }

        getBillBean().saveBillComponent(billEntry, bill, wu);

        for (BillFee bf : billFees) {
            getInwardBean().saveBillFee(bf, billItem, bill, wu);
            billItem.getBillFees().add(bf);
        }

        getBillBean().updateBillItemByBillFee(billItem);

        return billItem;
    }
```

- [ ] **Step 4: Import `Investigation` and `Bill` if not already imported**

`Investigation` is already imported (`com.divudi.core.entity.lab.Investigation`, used at line 839 in the existing `errorCheckForAdding()`). `Bill` is already imported. No new imports needed.

- [ ] **Step 5: Compile**

Run: `cd /home/buddhika/development/rh && mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/BillBhtController.java
git commit -m "feat(inward): pre-load Add Services page from an item request

Adds navigateToAddServicesFromItemRequest(...) which seeds the cart
from the request's remaining lines via the existing addToBill() path,
and threads referenceBill/referanceBillItem through the unmodified
save flow so the resulting bill stays linked to the originating
request. Part of the #21793 approval redesign."
```

---

## Task 3: `InpatientDirectIssueNativeSqlController`/`Service` — pre-load and thread the linkage

**Files:**
- Modify: `src/main/java/com/divudi/bean/pharmacy/InpatientDirectIssueNativeSqlController.java`
- Modify: `src/main/java/com/divudi/service/pharmacy/InpatientDirectIssueNativeSqlService.java`

**Interfaces:**
- Consumes: `BillItemData.getSourceRequestBillItemId()/setSourceRequestBillItemId(Long)` (Task 1).
- Produces: `InpatientDirectIssueNativeSqlController.navigateToDirectIssueFromItemRequest(Bill itemRequest, List<BillItem> remainingLines)` → `String` — called by Task 4.

- [ ] **Step 1: Verify the actual DB column name for `BillItem.referanceBillItem` before writing native SQL**

This entity field has no explicit `@JoinColumn`, so EclipseLink derives the column name — the existing native INSERTs in this same file already assume the `<fieldName>_ID` convention (e.g. `bill_ID`, `item_ID`, `creater_ID`), so the expected name is `referanceBillItem_ID`, but confirm against the actual schema before hardcoding it:

Run (see `developer_docs/database/mysql-developer-guide.md` for the local connection):
```bash
mysql -u buddhika -p -e "SHOW COLUMNS FROM billitem LIKE '%referance%';" coop
```
Expected: one row showing the FK column name (e.g. `referanceBillItem_ID`). If the actual name differs, use that exact name in Step 2 below instead.

- [ ] **Step 2: Add the column to the native `BillItem` INSERT**

In `src/main/java/com/divudi/service/pharmacy/InpatientDirectIssueNativeSqlService.java`, the `BillItem` INSERT is at lines 102-117. Replace it with (column name assumed `referanceBillItem_ID` per Step 1 — adjust if the verification step found a different name):

```java
            em.createNativeQuery(
                "INSERT INTO " + billItemTable()
                + " (bill_ID, item_ID, qty, descreption, netValue, grossValue, netRate,"
                + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType, referanceBillItem_ID)"
                + " VALUES (?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine',?)")
                .setParameter(1, billId)
                .setParameter(2, d.getItemId())
                .setParameter(3, absQty)
                .setParameter(4, d.getDescription())
                .setParameter(5, absNetValue)
                .setParameter(6, absGrossValue)
                .setParameter(7, netRate)
                .setParameter(8, new Timestamp(createdAt.getTime()))
                .setParameter(9, d.getCreaterId())
                .setParameter(10, d.getSourceRequestBillItemId())
                .executeUpdate();
            biIds[i] = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
```

- [ ] **Step 3: Add a JPQL stock lookup by item id (FIFO earliest expiry) to the controller**

`completeAvailableStockOptimizedDto(String qry)` (lines 493-516) only supports a name-search autocomplete. Add a new method right after it (after line 516) that finds the best batch for a known item id, reusing the same `StockDTO` projection and department scope:

```java
    /**
     * FIFO earliest-expiry stock lookup by item id, for pre-loading suggested
     * quantities from an Item/Service Request line (issue #21793 redesign) —
     * unlike completeAvailableStockOptimizedDto(), this looks up by exact item
     * id rather than a name search.
     */
    public StockDTO findEarliestExpiryStockForItem(Long itemId, double qty) {
        if (itemId == null) {
            return null;
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("department", sessionController.getLoggedUser().getDepartment());
        parameters.put("itemId", itemId);
        parameters.put("stockMin", qty);

        String sql = "SELECT NEW com.divudi.core.data.dto.StockDTO("
                + "i.id, i.itemBatch.id, i.itemBatch.item.id, i.itemBatch.item.name, i.itemBatch.item.code, "
                + "i.itemBatch.item.name, i.itemBatch.retailsaleRate, i.stock, i.itemBatch.dateOfExpire) "
                + "FROM Stock i "
                + "WHERE i.stock >= :stockMin "
                + "AND i.department = :department "
                + "AND i.itemBatch.item.id = :itemId "
                + "ORDER BY i.itemBatch.dateOfExpire";

        List<StockDTO> results = stockFacade.findLightsByJpql(sql, parameters, TemporalType.TIMESTAMP, 1);
        return results != null && !results.isEmpty() ? results.get(0) : null;
    }
```

- [ ] **Step 4: Add the pre-load navigation method**

Add this method after `findEarliestExpiryStockForItem(...)`:

```java
    /**
     * Entry point from the Item/Service Request pending queue (issue #21793
     * redesign): seeds this controller's cart with a suggested stock batch per
     * remaining inventory line, then hands control to the normal Direct Issue
     * page — the user reviews/edits and clicks the page's own Settle button.
     * Lines with no available stock are skipped (left remaining, reported back
     * to the queue) rather than blocking the whole navigation.
     */
    public String navigateToDirectIssueFromItemRequest(Bill itemRequest, List<BillItem> remainingLines) {
        resetAll();
        setPatientEncounter(itemRequest.getPatientEncounter());
        for (BillItem requestLine : remainingLines) {
            StockDTO stockDto = findEarliestExpiryStockForItem(
                    requestLine.getItem() != null ? requestLine.getItem().getId() : null,
                    requestLine.getQty());
            if (stockDto == null) {
                continue;
            }
            selectedStockDto = stockDto;
            selectedStockId = stockDto.getId();
            qty = requestLine.getQty();
            int sizeBefore = billItemDataList != null ? billItemDataList.size() : 0;
            addBillItem();
            if (billItemDataList != null && billItemDataList.size() > sizeBefore) {
                billItemDataList.get(billItemDataList.size() - 1).setSourceRequestBillItemId(requestLine.getId());
            }
        }
        return "/inward/pharmacy_bill_issue_bht?faces-redirect=true";
    }
```

- [ ] **Step 5: Thread `referenceBill` onto the created `Bill` header**

`buildBillHeader(Department matrixDept)` (lines 253-282) builds the `Bill` passed into `settle()`. Since all lines in one Direct-Issue visit share the same `patientEncounter`, and this method is only reached via the normal flow or the new pre-load entry point, add the request reference by tracking which request (if any) originated the current cart. Add a new field near the other working-state fields (after `private double marginTotal = 0.0;`, around line 101):

```java
    private Bill sourceItemRequest;
```

In `navigateToDirectIssueFromItemRequest(...)` (Step 4 above), set it right after `setPatientEncounter(...)`:

```java
        this.sourceItemRequest = itemRequest;
```

And in `buildBillHeader(Department matrixDept)`, right before the `return b;` (end of the method, after `b.setGrantTotal(0.0);`), add:

```java
        if (sourceItemRequest != null) {
            b.setReferenceBill(sourceItemRequest);
        }
```

Also add `sourceItemRequest = null;` inside `resetAll()` and `clearBill()` so a normal (non-request) Direct Issue visit afterward doesn't inherit a stale reference — check both methods (`resetAll()` at line 560, `clearBill()` referenced at line 195) and add the reset line in each.

- [ ] **Step 6: Compile**

Run: `cd /home/buddhika/development/rh && mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/InpatientDirectIssueNativeSqlController.java src/main/java/com/divudi/service/pharmacy/InpatientDirectIssueNativeSqlService.java
git commit -m "feat(pharmacy): pre-load Direct Issue (Native SQL) page from an item request

Adds navigateToDirectIssueFromItemRequest(...), a FIFO stock lookup by
item id, and threads referenceBill/referanceBillItem_ID through the
existing settle() native-SQL path. Part of the #21793 approval
redesign."
```

---

## Task 4: `ItemRequestApprovalController` — remaining-lines tracking and the two "Process…" actions

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/ItemRequestApprovalController.java`

**Interfaces:**
- Consumes: `BillBhtController.navigateToAddServicesFromItemRequest(Bill, List<BillItem>)` (Task 2), `InpatientDirectIssueNativeSqlController.navigateToDirectIssueFromItemRequest(Bill, List<BillItem>)` (Task 3).
- Produces: `getRemainingServiceLines(Bill)`, `getRemainingInventoryLines(Bill)` → `List<BillItem>`; `processServices(Bill)`, `processInventory(Bill)` → `String` (navigation outcomes) — called from Task 7's XHTML.
- Produces: `reject(Bill request, String reason)` — same signature as today, now cancels only remaining lines.

- [ ] **Step 1: Inject the two target controllers**

In `ItemRequestApprovalController.java`, add alongside the existing `@Inject private ItemRequestApiService itemRequestApiService;` (line 48-49):

```java
    @Inject
    private com.divudi.bean.inward.BillBhtController billBhtController;

    @Inject
    private com.divudi.bean.pharmacy.InpatientDirectIssueNativeSqlController inpatientDirectIssueNativeSqlController;
```

- [ ] **Step 2: Add line-classification and remaining-lines helpers**

Add these methods after `getRequestLines(Bill request)` (after line 164):

```java
    /**
     * A request line counts as fulfilled once some other BillItem references it
     * via referanceBillItem — set by BillBhtController/InpatientDirectIssueNativeSqlController's
     * save paths once the user completes the corresponding page (issue #21793 redesign).
     */
    private boolean isLineFulfilled(BillItem requestLine) {
        Map<String, Object> params = new HashMap<>();
        params.put("line", requestLine);
        Long count = billItemFacade.findLongByJpql(
                "select count(bi) from BillItem bi where bi.retired=false and bi.referanceBillItem=:line",
                params);
        return count != null && count > 0;
    }

    private boolean isServiceOrInvestigationItem(com.divudi.core.entity.Item item) {
        return item instanceof com.divudi.core.entity.Service
                || item instanceof com.divudi.core.entity.lab.Investigation;
    }

    public List<BillItem> getRemainingServiceLines(Bill request) {
        List<BillItem> remaining = new ArrayList<>();
        for (BillItem line : getRequestLines(request)) {
            if (isServiceOrInvestigationItem(line.getItem()) && !isLineFulfilled(line)) {
                remaining.add(line);
            }
        }
        return remaining;
    }

    public List<BillItem> getRemainingInventoryLines(Bill request) {
        List<BillItem> remaining = new ArrayList<>();
        for (BillItem line : getRequestLines(request)) {
            if (!isServiceOrInvestigationItem(line.getItem()) && !isLineFulfilled(line)) {
                remaining.add(line);
            }
        }
        return remaining;
    }
```

- [ ] **Step 3: Replace `approve()` with `processServices(...)`/`processInventory(...)`**

Delete the existing `approve(Bill request)` method (lines 85-108) entirely, and replace it with:

```java
    public String processServices(Bill request) {
        if (!webUserController.hasPrivilege("InwardServiceItemRequestApproval")) {
            JsfUtil.addErrorMessage("You do not have privileges to process item/service requests.");
            return null;
        }
        if (request == null || !belongsToCurrentDepartment(request)) {
            JsfUtil.addErrorMessage("This request belongs to another department's queue.");
            loadPendingRequests();
            return null;
        }
        List<BillItem> remaining = getRemainingServiceLines(request);
        if (remaining.isEmpty()) {
            JsfUtil.addErrorMessage("No remaining service/investigation lines on this request.");
            return null;
        }
        return billBhtController.navigateToAddServicesFromItemRequest(request, remaining);
    }

    public String processInventory(Bill request) {
        if (!webUserController.hasPrivilege("InwardServiceItemRequestApproval")) {
            JsfUtil.addErrorMessage("You do not have privileges to process item/service requests.");
            return null;
        }
        if (request == null || !belongsToCurrentDepartment(request)) {
            JsfUtil.addErrorMessage("This request belongs to another department's queue.");
            loadPendingRequests();
            return null;
        }
        List<BillItem> remaining = getRemainingInventoryLines(request);
        if (remaining.isEmpty()) {
            JsfUtil.addErrorMessage("No remaining inventory lines on this request.");
            return null;
        }
        return inpatientDirectIssueNativeSqlController.navigateToDirectIssueFromItemRequest(request, remaining);
    }
```

- [ ] **Step 4: Update `reject(...)` to cancel only remaining lines**

Replace the body of `reject(Bill request, String reason)` (lines 110-138) — instead of delegating the whole request to `itemRequestApiService.rejectRequest(...)`, pass the remaining-line ids so the service only cancels those:

```java
    public void reject(Bill request, String reason) {
        if (!webUserController.hasPrivilege("InwardServiceItemRequestRejection")) {
            JsfUtil.addErrorMessage("You do not have privileges to reject item/service requests.");
            return;
        }
        if (request == null || request.getId() == null) {
            JsfUtil.addErrorMessage("No request selected.");
            return;
        }
        if (reason == null || reason.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Rejection reason is required");
            return;
        }
        if (!belongsToCurrentDepartment(request)) {
            JsfUtil.addErrorMessage("This request belongs to another department's queue.");
            loadPendingRequests();
            return;
        }
        List<BillItem> remaining = new ArrayList<>();
        remaining.addAll(getRemainingServiceLines(request));
        remaining.addAll(getRemainingInventoryLines(request));
        if (remaining.isEmpty()) {
            JsfUtil.addErrorMessage("This request has no remaining lines to reject.");
            return;
        }
        try {
            itemRequestApiService.rejectRemainingLines(request.getId(), remaining, reason, sessionController.getLoggedUser(), sessionController.getDepartment());
            JsfUtil.addSuccessMessage("Remaining lines rejected.");
        } catch (IllegalStateException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Rejection failed: " + e.getMessage());
        }
        setRejectionReason(null);
        loadPendingRequests();
    }
```

- [ ] **Step 5: Update `loadPendingRequests()` to include partially-fulfilled requests**

The current JPQL (lines 73-79) excludes any request that has an `INWARD_SERVICE_ITEM_APPROVAL` bill referencing it — that whole-request exclusion no longer makes sense once partial fulfillment is possible. Replace `loadPendingRequests()` (lines 71-83) with:

```java
    public void loadPendingRequests() {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select b from Bill b where b.retired=false and b.toDepartment=:toDep "
                + "and b.billType=:bTp and b.cancelled=false "
                + "order by b.createdAt desc";
        params.put("toDep", sessionController.getDepartment());
        params.put("bTp", BillType.InwardServiceItemRequest);

        List<Bill> results = billFacade.findByJpql(jpql, params);
        List<Bill> withRemainingLines = new ArrayList<>();
        for (Bill candidate : results != null ? results : new ArrayList<Bill>()) {
            if (!getRemainingServiceLines(candidate).isEmpty() || !getRemainingInventoryLines(candidate).isEmpty()) {
                withRemainingLines.add(candidate);
            }
        }
        pendingRequests = withRemainingLines;
    }
```

Note: `BillTypeAtomic` import may now be unused in this file — leave it for Task 8 to remove (it deletes the enum value this import was for anyway; re-check unused imports once Task 8 lands).

- [ ] **Step 6: Compile**

Run: `cd /home/buddhika/development/rh && mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS` (Task 5's `rejectRemainingLines(...)` doesn't exist yet — this step will fail until Task 5 is done; if executing tasks in strict order, do Task 5 before compiling, or compile after both).

- [ ] **Step 7: Commit** (combine with Task 5's commit if compiling together)

---

## Task 5: `ItemRequestApiService` — delete `approveRequest()`, add `rejectRemainingLines(...)`, per-line status

**Files:**
- Modify: `src/main/java/com/divudi/service/inward/ItemRequestApiService.java`

**Interfaces:**
- Consumes: nothing new.
- Produces: `rejectRemainingLines(Long requestBillId, List<BillItem> linesToReject, String reason, WebUser rejectingUser, Department rejectingDepartment)` — called by Task 4.
- Produces: `getRequestById(Long)`/`listRequests(...)` now report per-line status (used by Task 6's DTOs).

- [ ] **Step 1: Delete `approveRequest()` and its now-dead private helpers**

Delete the entire `approveRequest(Long requestBillId, WebUser approvingUser, Department approvingDepartment)` method (lines 298-428 in the current file) and the two private helpers it alone used: `saveServiceLineFees(...)` (lines 594-626) and `findAvailableStockForItem(...)` (lines 628-647) — both become dead code once `approveRequest()` is gone. Also remove the now-unused `@Inject private BillBeanController billBeanController;` (line 91-92) and `@Inject private DirectIssueBatchService directIssueBatchService;` (line 94-95) and their imports (`com.divudi.bean.common.BillBeanController`, `com.divudi.service.pharmacy.DirectIssueBatchService`) — nothing else in this file uses them after the deletion.

Also remove the now-unused `@EJB private StockFacade stockFacade;` and `@EJB private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;` if nothing else in the file references them after deletion (check with `grep -n "stockFacade\|pharmaceuticalBillItemFacade" src/main/java/com/divudi/service/inward/ItemRequestApiService.java` — if only the deleted method used them, remove the fields and their imports too).

- [ ] **Step 2: Replace `deriveStatus`/`findApprovalBill` with per-line status derivation**

Replace `deriveStatus(Bill requestBill)` (lines 489-501) and `findApprovalBill(Bill requestBill)` (lines 503-510) with:

```java
    /**
     * A request line is FULFILLED once some BillItem elsewhere references it via
     * referanceBillItem (set by BillBhtController/InpatientDirectIssueNativeSqlController's
     * save paths — issue #21793 redesign). Overall status is derived from the mix
     * of fulfilled / rejected / pending lines.
     */
    private boolean isLineFulfilled(BillItem requestLine) {
        Map<String, Object> params = new HashMap<>();
        params.put("line", requestLine);
        Long count = billItemFacade.findLongByJpql(
                "select count(bi) from BillItem bi where bi.retired=false and bi.referanceBillItem=:line",
                params);
        return count != null && count > 0;
    }

    private BillItem findFulfillingBillItem(BillItem requestLine) {
        Map<String, Object> params = new HashMap<>();
        params.put("line", requestLine);
        return billItemFacade.findFirstByJpql(
                "select bi from BillItem bi where bi.retired=false and bi.referanceBillItem=:line",
                params);
    }

    private String deriveStatus(Bill requestBill) {
        if (requestBill.isCancelled()) {
            return "CANCELLED";
        }
        List<BillItem> lines = billItemFacade.findByJpql(
                "select bi from BillItem bi where bi.retired=false and bi.bill=:b",
                singleParam("b", requestBill));
        boolean anyFulfilled = false;
        boolean anyPending = false;
        boolean anyRejected = false;
        for (BillItem line : lines) {
            if (isLineFulfilled(line)) {
                anyFulfilled = true;
            } else if (Boolean.TRUE.equals(line.isRetired())) {
                anyRejected = true;
            } else {
                anyPending = true;
            }
        }
        if (anyFulfilled && anyRejected && !anyPending) {
            return "PARTIALLY_FULFILLED_AND_REJECTED";
        }
        if (anyFulfilled && anyPending) {
            return "PARTIALLY_FULFILLED";
        }
        if (anyFulfilled) {
            return "FULFILLED";
        }
        if (anyRejected && !anyPending) {
            return "REJECTED";
        }
        return "PENDING";
    }
```

Note: this uses `line.isRetired()` on the request's own `BillItem` lines as the "this specific line was rejected" marker — Task 5 Step 3 below wires `rejectRemainingLines(...)` to retire exactly those `BillItem` rows (not the whole `Bill`), which is a new, more granular rejection mechanism than the old whole-bill `cancelled` flag.

- [ ] **Step 3: Replace `rejectRequest(...)` with `rejectRemainingLines(...)`**

Delete `rejectRequest(Long requestBillId, String reason, WebUser rejectingUser, Department rejectingDepartment)` (lines 434-450) and replace with:

```java
    /**
     * Rejects only the given still-pending request lines (retires their
     * BillItem rows with a reason), leaving any already-fulfilled lines and
     * their real bills untouched. Issue #21793 redesign — rejection is no
     * longer whole-request once partial fulfillment is possible.
     */
    public Bill rejectRemainingLines(Long requestBillId, List<BillItem> linesToReject, String reason, WebUser rejectingUser, Department rejectingDepartment) {
        Bill requestBill = fetchRequestBillOrThrow(requestBillId);
        assertRequestBelongsToDepartment(requestBill, rejectingDepartment, "reject");

        for (BillItem line : linesToReject) {
            if (isLineFulfilled(line)) {
                continue;
            }
            line.setRetired(true);
            line.setRetirer(rejectingUser);
            line.setRetiredAt(new Date());
            line.setRetireComments(reason);
            billItemFacade.edit(line);
        }

        return requestBill;
    }
```

- [ ] **Step 4: Update `buildResponseForRequestBill(...)` for per-line status**

Replace `buildResponseForRequestBill(Bill requestBill)` (lines 512-535) — it previously looked for a single `approvalBill`; now it builds per-line fulfillment info:

```java
    private ItemRequestResponseDTO buildResponseForRequestBill(Bill requestBill) {
        List<BillItem> lines = billItemFacade.findByJpql(
                "select bi from BillItem bi where bi.retired=false and bi.bill=:b",
                singleParam("b", requestBill));
        // Include retired (rejected) lines too, so the API can report their status.
        List<BillItem> retiredLines = billItemFacade.findByJpql(
                "select bi from BillItem bi where bi.retired=true and bi.bill=:b",
                singleParam("b", requestBill));

        List<BillItem> allLines = new ArrayList<>();
        allLines.addAll(lines);
        allLines.addAll(retiredLines);

        String status = deriveStatus(requestBill);
        return toResponseDTO(requestBill, allLines, status);
    }
```

- [ ] **Step 5: Rewrite `toResponseDTO(...)` for the new per-line/per-bill shape**

Replace both overloads of `toResponseDTO(...)` (lines 537-574) with a single version matching the new `ItemRequestResponseDTO` shape from Task 6:

```java
    private ItemRequestResponseDTO toResponseDTO(Bill requestBill, List<BillItem> lines, String status) {
        ItemRequestResponseDTO dto = new ItemRequestResponseDTO();
        dto.setId(requestBill.getId());
        dto.setRequestNo(requestBill.getDeptId());
        dto.setBhtNo(requestBill.getPatientEncounter() != null ? requestBill.getPatientEncounter().getBhtNo() : null);
        dto.setTargetDepartmentId(requestBill.getToDepartment() != null ? requestBill.getToDepartment().getId() : null);
        dto.setTargetDepartmentName(requestBill.getToDepartment() != null ? requestBill.getToDepartment().getName() : null);
        dto.setStatus(status);
        dto.setComments(requestBill.getComments());
        dto.setCreatedAt(requestBill.getCreatedAt());
        dto.setCreatedBy(requestBill.getCreater() != null ? requestBill.getCreater().getName() : null);

        List<ItemRequestLineResponseDTO> lineDtos = new ArrayList<>();
        List<Long> fulfillingBillIds = new ArrayList<>();
        for (BillItem bi : lines) {
            ItemRequestLineResponseDTO lineDto = new ItemRequestLineResponseDTO();
            lineDto.setBillItemId(bi.getId());
            lineDto.setItemId(bi.getItem() != null ? bi.getItem().getId() : null);
            lineDto.setItemName(bi.getItem() != null ? bi.getItem().getName() : null);
            lineDto.setItemType(isServiceItem(bi.getItem()) ? "SERVICE" : "INVENTORY");
            lineDto.setQty(bi.getQty() != null ? bi.getQty() : 0.0);

            if (Boolean.TRUE.equals(bi.isRetired())) {
                lineDto.setStatus("REJECTED");
                lineDto.setRejectionReason(bi.getRetireComments());
            } else {
                BillItem fulfillingLine = findFulfillingBillItem(bi);
                if (fulfillingLine != null) {
                    lineDto.setStatus("FULFILLED");
                    lineDto.setFulfillingBillId(fulfillingLine.getBill().getId());
                    lineDto.setFulfillingBillType(fulfillingLine.getBill().getBillType().name());
                    if (!fulfillingBillIds.contains(fulfillingLine.getBill().getId())) {
                        fulfillingBillIds.add(fulfillingLine.getBill().getId());
                    }
                } else {
                    lineDto.setStatus("PENDING");
                }
            }
            lineDtos.add(lineDto);
        }
        dto.setLines(lineDtos);
        dto.setFulfillingBillIds(fulfillingBillIds);

        return dto;
    }
```

- [ ] **Step 6: Update `listRequests(...)`'s status filter call site**

`listRequests(...)` (lines 218-259) calls `deriveStatus(b)` — signature unchanged, no edit needed there. Double check the status string comparison (`status.trim().equalsIgnoreCase(derivedStatus)`) still works with the new status vocabulary (`PENDING`, `PARTIALLY_FULFILLED`, `FULFILLED`, `REJECTED`, `PARTIALLY_FULFILLED_AND_REJECTED`, `CANCELLED`) — no code change required, just confirm callers/docs use the new vocabulary (Task 9 covers the API doc update).

- [ ] **Step 7: Compile**

Run: `cd /home/buddhika/development/rh && mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`. This also resolves Task 4 Step 6's pending compile (both files depend on each other's new methods).

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/divudi/service/inward/ItemRequestApiService.java src/main/java/com/divudi/bean/inward/ItemRequestApprovalController.java
git commit -m "refactor(inward): replace approveRequest() with per-line remaining/reject tracking

Deletes the bespoke approval transaction entirely. Status is now
derived per-line from BillItem.referanceBillItem, and rejection only
retires still-pending lines. Part of the #21793 approval redesign."
```

---

## Task 6: DTO updates — per-line status and multi-bill fulfillment

**Files:**
- Modify: `src/main/java/com/divudi/core/data/dto/itemrequest/ItemRequestResponseDTO.java`
- Modify: `src/main/java/com/divudi/core/data/dto/itemrequest/ItemRequestLineResponseDTO.java`

**Interfaces:**
- Consumes: nothing (leaf DTOs).
- Produces: fields consumed by Task 5's `toResponseDTO(...)`.

- [ ] **Step 1: Update `ItemRequestResponseDTO`**

In `ItemRequestResponseDTO.java`, remove `approvalBillId`, `decidedAt`, `decidedBy`, `rejectionReason` (request-level — rejection is now per-line, reported on each line instead) and add `fulfillingBillIds`:

Replace:
```java
    private Long approvalBillId;
    private Date decidedAt;
    private String decidedBy;
```
with:
```java
    private List<Long> fulfillingBillIds;
```

Remove the `rejectionReason` field and its getter/setter (moved to line level).

Replace the `getApprovalBillId()/setApprovalBillId(...)`, `getDecidedAt()/setDecidedAt(...)`, `getDecidedBy()/setDecidedBy(...)` accessor blocks with:
```java
    public List<Long> getFulfillingBillIds() {
        return fulfillingBillIds;
    }

    public void setFulfillingBillIds(List<Long> fulfillingBillIds) {
        this.fulfillingBillIds = fulfillingBillIds;
    }
```

Remove `getRejectionReason()/setRejectionReason(...)`.

- [ ] **Step 2: Update `ItemRequestLineResponseDTO`**

Read the file first to get its exact current shape, then add per-line status fields:

```bash
cat src/main/java/com/divudi/core/data/dto/itemrequest/ItemRequestLineResponseDTO.java
```

Add these fields (alongside `billItemId`, `itemId`, `itemName`, `itemType`, `qty`, `netValue`):

```java
    private String status;
    private Long fulfillingBillId;
    private String fulfillingBillType;
    private String rejectionReason;
```

With accessors:

```java
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getFulfillingBillId() {
        return fulfillingBillId;
    }

    public void setFulfillingBillId(Long fulfillingBillId) {
        this.fulfillingBillId = fulfillingBillId;
    }

    public String getFulfillingBillType() {
        return fulfillingBillType;
    }

    public void setFulfillingBillType(String fulfillingBillType) {
        this.fulfillingBillType = fulfillingBillType;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
```

- [ ] **Step 3: Compile**

Run: `cd /home/buddhika/development/rh && mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/core/data/dto/itemrequest/ItemRequestResponseDTO.java src/main/java/com/divudi/core/data/dto/itemrequest/ItemRequestLineResponseDTO.java
git commit -m "feat(api): per-line status and multi-bill fulfillment in item request DTOs

approvalBillId (singular) is replaced by fulfillingBillIds (list) since
a request can now be fulfilled by more than one bill over time.
Rejection reason moves from request-level to line-level. Part of the
#21793 approval redesign."
```

---

## Task 7: Pending-queue UI — two "Process…" buttons, remaining-lines display

**Files:**
- Modify: `src/main/webapp/inward/item_request_pending_list.xhtml`

**Interfaces:**
- Consumes: `itemRequestApprovalController.getRemainingServiceLines(Bill)`, `getRemainingInventoryLines(Bill)`, `processServices(Bill)`, `processInventory(Bill)`, `reject(Bill, String)` (Task 4).

- [ ] **Step 1: Replace the single "Requested Items" column + "Approve" button with per-type remaining lines and two buttons**

Replace the "Requested Items" column (lines 54-63) and the "Actions" column's Approve button (lines 65-74) with:

```xml
                    <p:column headerText="Remaining Services/Investigations" style="width: 20%;">
                        <p:dataTable var="li" value="#{itemRequestApprovalController.getRemainingServiceLines(r)}" emptyMessage="None remaining">
                            <p:column headerText="Item">
                                <h:outputLabel value="#{li.item.name}"/>
                            </p:column>
                            <p:column headerText="Qty">
                                <h:outputLabel value="#{li.qty}"/>
                            </p:column>
                        </p:dataTable>
                    </p:column>

                    <p:column headerText="Remaining Inventory Items" style="width: 20%;">
                        <p:dataTable var="li" value="#{itemRequestApprovalController.getRemainingInventoryLines(r)}" emptyMessage="None remaining">
                            <p:column headerText="Item">
                                <h:outputLabel value="#{li.item.name}"/>
                            </p:column>
                            <p:column headerText="Qty">
                                <h:outputLabel value="#{li.qty}"/>
                            </p:column>
                        </p:dataTable>
                    </p:column>

                    <p:column headerText="Actions">
                        <p:commandButton
                            id="btnProcessServices#{r.id}"
                            title="Process Services/Investigations"
                            value="Process Services/Investigations"
                            class="ui-button-success m-1"
                            ajax="false"
                            icon="fa fa-utensils"
                            action="#{itemRequestApprovalController.processServices(r)}"
                            rendered="#{webUserController.hasPrivilege('InwardServiceItemRequestApproval') and not empty itemRequestApprovalController.getRemainingServiceLines(r)}"/>

                        <p:commandButton
                            id="btnProcessInventory#{r.id}"
                            title="Process Inventory Items"
                            value="Process Inventory Items"
                            class="ui-button-success m-1"
                            ajax="false"
                            icon="fa fa-box"
                            action="#{itemRequestApprovalController.processInventory(r)}"
                            rendered="#{webUserController.hasPrivilege('InwardServiceItemRequestApproval') and not empty itemRequestApprovalController.getRemainingInventoryLines(r)}"/>

                        <p:commandButton
                            id="btnOpenReject#{r.id}"
                            title="Reject Remaining Lines"
                            class="ui-button-danger m-1"
                            icon="fa fa-times"
                            process="@this"
                            update=":itemRequestPendingListForm:rejectDialogPanel"
                            oncomplete="PF('rejectRequestDialog').show()"
                            rendered="#{webUserController.hasPrivilege('InwardServiceItemRequestRejection')}">
                            <f:setPropertyActionListener value="#{r}" target="#{itemRequestApprovalController.selectedRequest}"/>
                            <f:setPropertyActionListener value="#{null}" target="#{itemRequestApprovalController.rejectionReason}"/>
                        </p:commandButton>
                    </p:column>
```

- [ ] **Step 2: Update the reject dialog confirmation text and button label**

In the `p:dialog` block (lines 93-117), change `header="Reject Request"` to `header="Reject Remaining Lines"`, and in `btnConfirmReject`'s `onclick` (line 113), change the confirm text from `'Reject this request?'` to `'Reject the remaining lines of this request?'`.

- [ ] **Step 3: Deploy and manually verify the page renders**

Follow `developer_docs/testing/playwright-e2e-workflow.md` to rebuild/redeploy:
```bash
cd /home/buddhika/development/rh && mvn -q -DskipTests package
```
Then redeploy to local Payara (per this project's established redeploy steps) and load `/inward/item_request_pending_list.xhtml` as a department user with a request pending — confirm the two remaining-lines tables and the "Process…" buttons render without JSF errors (check server log for `ELResolver`/expression errors).

- [ ] **Step 4: Commit**

```bash
git add src/main/webapp/inward/item_request_pending_list.xhtml
git commit -m "feat(inward): two-button pending-request UI (Process Services/Inventory)

Replaces the single Approve button with per-type remaining-lines
tables and buttons that navigate into the real Add Services / Direct
Issue pages. Part of the #21793 approval redesign."
```

---

## Task 8: Delete the retired bill type and its COGS special-case

**Files:**
- Modify: `src/main/java/com/divudi/core/data/BillTypeAtomic.java`
- Modify: `src/main/java/com/divudi/core/data/BillType.java`
- Modify: `src/main/java/com/divudi/bean/report/PharmacyReportController.java`

**Interfaces:** none (leaf cleanup).

- [ ] **Step 1: Confirm nothing else references the values being deleted**

```bash
grep -rn "INWARD_SERVICE_ITEM_APPROVAL\b\|InwardServiceItemApproval\b" src/main/java src/main/webapp
```
Expected after Tasks 1-7: only the two enum declarations themselves and the one `PharmacyReportController` special-case line remain. If anything else shows up, resolve it before continuing (most likely stale imports left over from Task 4/5's deletions — remove them).

- [ ] **Step 2: Delete the two `BillTypeAtomic` entries**

In `src/main/java/com/divudi/core/data/BillTypeAtomic.java`, delete lines 78-79:
```java
    INWARD_SERVICE_ITEM_APPROVAL("Approve Inward Service/Item Request", BillCategory.BILL, ServiceType.INWARD_SERVICE, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD, PaymentCategory.NO_PAYMENT, BillType.InwardServiceItemApproval),
    INWARD_SERVICE_ITEM_APPROVAL_CANCELLATION("Cancel Inward Service/Item Approval", BillCategory.CANCELLATION, ServiceType.INWARD_SERVICE, BillFinanceType.NO_FINANCE_TRANSACTIONS, CountedServiceType.INWARD, PaymentCategory.NO_PAYMENT, BillType.InwardServiceItemApproval),
```
(Leave `INWARD_SERVICE_ITEM_REQUEST`, `INWARD_SERVICE_ITEM_REQUEST_CANCELLATION`, and `INWARD_SERVICE_ITEM_REJECTION` — still used by the request bill itself.)

- [ ] **Step 3: Delete the `BillType` entry**

In `src/main/java/com/divudi/core/data/BillType.java`, delete line 76: `    InwardServiceItemApproval,`.

- [ ] **Step 4: Remove the COGS special-case in `PharmacyReportController`**

In `calculateBhtIssueValue()` (around line 13715), remove the comment and enum reference added for #21266:
```java
            List<BillTypeAtomic> billTypes = Arrays.asList(
                    BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD,
                    BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
                    BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE
            );
```
(drop the `INWARD_SERVICE_ITEM_APPROVAL` line and its preceding comment — real `DIRECT_ISSUE_INWARD_MEDICINE` bills created by Task 3 are already covered by the existing entry).

- [ ] **Step 5: Compile**

Run: `cd /home/buddhika/development/rh && mvn -q -DskipTests compile`
Expected: `BUILD SUCCESS` — a compile error here means Step 1's grep missed a reference; find and fix it.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/divudi/core/data/BillTypeAtomic.java src/main/java/com/divudi/core/data/BillType.java src/main/java/com/divudi/bean/report/PharmacyReportController.java
git commit -m "refactor(inward): delete retired InwardServiceItemApproval bill type

Never reached production, so no data migration is needed. Removes the
#21266 COGS special-case along with it, since real DIRECT_ISSUE_INWARD_MEDICINE
bills are already covered without special-casing. Completes the #21793
approval redesign."
```

---

## Task 9: End-to-end verification

**Files:** none (verification only).

- [ ] **Step 1: Rebuild and redeploy**

```bash
cd /home/buddhika/development/rh && mvn -q -DskipTests package
```
Redeploy the resulting WAR to local Payara per this project's established redeploy process (see `developer_docs/deployment/persistence-verification.md`), confirming `persistence.xml` is still in local JNDI mode first.

- [ ] **Step 2: Submit a mixed request via the API**

```bash
curl -s -X POST http://localhost:8080/rh/api/itemrequests \
  -H "Finance: <local-test-api-key>" -H "Content-Type: application/json" \
  -d '{"bhtNo":"<a real active BHT no>","targetDepartmentId":<a real department id>,"lines":[{"itemId":<a Service item id, e.g. Breakfast>,"qty":1},{"itemId":<a stock Item id, e.g. Water Bottle>,"qty":3}]}'
```
Expected: `201`, `data.status = "PENDING"`, both lines present with `status: "PENDING"`.

- [ ] **Step 3: Playwright — process the service line**

Log in as a department user for the target department (see `developer_docs/testing/playwright-e2e-workflow.md` for login + department selection), open `/inward/item_request_pending_list.xhtml`, confirm the new request row shows the meal line under "Remaining Services/Investigations" and the stock line under "Remaining Inventory Items". Click "Process Services/Investigations".
Expected: navigates to `/inward/inward_bill_service.xhtml` with the patient/BHT already selected and the meal item already in the cart at qty 1.

- [ ] **Step 4: Save the Add Services page and verify the bill + linkage in the DB**

Click the page's own Save button. Then query the DB:
```bash
mysql -u buddhika -p coop -e "select b.ID, b.BILLTYPEATOMIC, b.REFERENCEBILL_ID from bill b where b.BILLTYPEATOMIC='INWARD_SERVICE_BILL' order by b.ID desc limit 1;"
```
Expected: one row, `REFERENCEBILL_ID` equal to the request bill's id from Step 2.
```bash
mysql -u buddhika -p coop -e "select ID, REFERANCEBILLITEM_ID from billitem where BILL_ID=(select ID from bill where BILLTYPEATOMIC='INWARD_SERVICE_BILL' order by ID desc limit 1);"
```
Expected: `REFERANCEBILLITEM_ID` (exact column name per Task 3 Step 1's verification) equal to the request's meal-line `BillItem` id.

- [ ] **Step 5: Return to the queue and process the inventory line**

Navigate back to `/inward/item_request_pending_list.xhtml`. Confirm the same request row now shows "None remaining" under Services/Investigations but still lists the Water Bottle line under Remaining Inventory Items, and only "Process Inventory Items" is rendered. Click it.
Expected: navigates to `/inward/pharmacy_bill_issue_bht.xhtml` with the BHT selected and the Water Bottle line pre-added at qty 3 (assuming stock ≥ 3 exists for that department).

- [ ] **Step 6: Save and verify stock deduction + linkage + COGS**

Click Settle. Verify stock deducted by 3 for that item/department/batch, and:
```bash
mysql -u buddhika -p coop -e "select ID, BILLTYPEATOMIC, REFERENCEBILL_ID from bill where BILLTYPEATOMIC='DIRECT_ISSUE_INWARD_MEDICINE' order by ID desc limit 1;"
```
Expected: `REFERENCEBILL_ID` equal to the request bill id.

Then open the COGS report (`processCostOfGoodSoldReport()` / the Pharmacy → Cost of Goods Sold page) for today's date range and confirm the "BHT Issue" row includes this bill's value with no special-casing needed (it's a real `DIRECT_ISSUE_INWARD_MEDICINE` bill, already covered).

- [ ] **Step 7: Verify the request now shows fully fulfilled**

Return to `/inward/item_request_pending_list.xhtml` — the request row should have disappeared from the pending queue entirely (both remaining-lines lists empty). Then:
```bash
curl -s http://localhost:8080/rh/api/itemrequests/<request id> -H "Finance: <local-test-api-key>"
```
Expected: `data.status = "FULFILLED"`, both lines report `status: "FULFILLED"` with their respective `fulfillingBillId`/`fulfillingBillType`, and `data.fulfillingBillIds` contains both bill ids.

- [ ] **Step 8: Verify partial-reject behavior**

Submit a second two-line request, process only one line (either type), then click "Reject Remaining Lines" and confirm. Verify via `GET /api/itemrequests/{id}` that the processed line still reports `FULFILLED` (with its bill untouched) and the other reports `REJECTED` with the given reason, and overall `status = "PARTIALLY_FULFILLED_AND_REJECTED"`.

- [ ] **Step 9: Final commit (if any fixes were needed during verification)**

If verification uncovered issues, fix them in the relevant task's files and commit with a message describing the specific fix (e.g. `fix(inward): correct REFERANCEBILLITEM_ID column name in native INSERT`), referencing which task it corrects.
