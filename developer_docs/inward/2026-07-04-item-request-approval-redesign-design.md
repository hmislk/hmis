# Item/Service Request Approval Redesign (issue #21793 follow-up)

## Background

Issue [#21793](https://github.com/hmislk/hmis/issues/21793) added a REST API + department approval workflow for external item/service requests against an inpatient BHT (meals, and stock items like Water Bottle/Tea/Milk/Sugar). It was implemented and merged (`36e7587329`, `45d291ce55`, PR #21819) but **never deployed to production**.

The current "Approve" action is a single server-side transaction (`ItemRequestApiService.approveRequest()`) that hand-rolls its own bill: a bespoke `BillTypeAtomic.INWARD_SERVICE_ITEM_APPROVAL` bill, with manually-built `BillItem`/`BillFee`/`PharmaceuticalBillItem` rows and a direct call to `DirectIssueBatchService.batchStockDeduction()`. This duplicates (rather than reuses) the logic behind the real "Inpatient Direct Issue" and "Add Services" flows. Consequences already observed:
- The new bill type had to be special-cased into the Cost-of-Goods-Sold report (`PharmacyReportController.calculateBhtIssueValue()`) after the fact to avoid a variance (tracked as #21266).
- A second report (`processBhtIssue()`, the BHT Issue Report export) was never patched and still misses these bills.
- The bespoke bill never calls `BillService.createBillFinancialDetailsForInpatientDirectIssueBill()`, so any other reporting keyed off `BillFinancialDetails` also misses it.

Since this feature never reached production, there is no legacy data to preserve — the bespoke bill type can be deleted outright rather than kept for backward compatibility.

## Goals

1. Approval must never itself perform stock deduction or create charges by directly duplicating billing logic.
2. Approved requests are turned into **real, ordinary bills** created through the same, unmodified pages hospital staff already use manually:
   - Inventory lines → **Direct Issue to BHTs (Native SQL)** (`InpatientDirectIssueNativeSqlController`, `BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE`).
   - Service/meal lines and investigation lines → **Add Services & Investigations** (`BillBhtController`, `BillTypeAtomic.INWARD_SERVICE_BILL`).
3. Every bill created this way carries a reference back to the original request bill, and each fulfilled request line is traceable to the specific `BillItem` that fulfilled it.
4. A request with mixed line types (some inventory, some service) can be processed across two separate visits to the two different pages, with the pending-queue reflecting partial completion.

## Non-goals

- No new stateless "settle" service is extracted for Add Services — the existing session-scoped `BillBhtController` is reused directly (see Mechanism below), not refactored into a new service layer.
- No change to `POST /api/itemrequests` (submission) or the request DTO shape.
- No new privileges — the two existing privileges (`InwardServiceItemRequestApproval`, `InwardServiceItemRequestRejection`) are reused with adjusted meaning.

## Workflow

1. External system submits a request (unchanged): `POST /api/itemrequests` creates a `Bill` (`BillType.InwardServiceItemRequest`), Pending, with one `BillItem` per line — no charge, no stock movement.
2. Department user opens the pending-requests queue (`ItemRequestApprovalController` / `item_request_pending_list.xhtml`), filtered to their department, as today.
3. Instead of one "Approve" button, each request shows up to two buttons depending on what remains unfulfilled:
   - **Process Services/Investigations** — shown if the request has any service or investigation line not yet fulfilled.
   - **Process Inventory Items** — shown if the request has any inventory line not yet fulfilled.
4. Clicking a button navigates to the corresponding existing page (`inward_bill_service.xhtml` or `pharmacy_bill_issue_bht_native.xhtml`) with:
   - The patient/BHT already selected (from the request's `PatientEncounter`).
   - The remaining lines of that type already added to the page's cart, at the requested quantities — exactly as if the user had manually searched for and added each one.
5. The user is free to edit quantities, remove lines, or add extra items before clicking that page's own, completely unmodified Save/Settle button. Nothing happens automatically.
6. On save, the resulting real bill (Direct Issue or Add Services) sets `referenceBill` = the original request bill, and each of its `BillItem`s that fulfills a request line sets `referanceBillItem` = that request line's `BillItem`.
7. The user returns to the pending queue; it recomputes remaining lines from scratch (any request line with no `BillItem` elsewhere pointing back to it via `referanceBillItem` is still pending) and shows whichever "Process…" buttons still apply.
8. **Reject** cancels only the still-pending lines of a request (records a reason); lines already fulfilled by a real bill are untouched. A fully-fulfilled request has nothing left to reject.

## Technical design

### Line classification (unchanged mechanism, same as today)
- `item instanceof Service` (excluding `Investigation`) → service line.
- `item instanceof Investigation` → investigation line, billed the same way as a service line via Add Services; requires a `billTime`, defaulted to the moment of processing (`new Date()`), matching `BillBhtController`'s own auto-fill behavior.
- Anything else → inventory line, handled via Direct Issue.

### Controller additions

**`BillBhtController`** (Add Services): new method `navigateToAddServicesFromItemRequest(Bill itemRequest, List<BillItem> remainingLines)`:
- Sets `patientEncounter` from `itemRequest.getPatientEncounter()`.
- For each remaining line, adds a cart entry via the controller's existing add-item path (same validation/pricing/margin logic as manual entry), tagged with a reference to the originating request `BillItem`.
- Returns the existing `/inward/inward_bill_service` outcome — the page itself is unchanged.
- Minimal addition to the existing save path: when persisting each `BillItem`, if its cart entry carries a source-request-line tag, set `billItem.setReferanceBillItem(sourceRequestLine)` and (once, on the bill) `bill.setReferenceBill(itemRequest)`.

**`InpatientDirectIssueNativeSqlController`** (Direct Issue Native SQL): equivalent new method `navigateToDirectIssueFromItemRequest(Bill itemRequest, List<BillItem> remainingLines)`, same pattern — pre-populate the controller's item/qty cart, tag each line, and thread the same two linkage writes through `InpatientDirectIssueNativeSqlService.settle(...)`'s bill/item construction.

**`ItemRequestApprovalController`**: replaces `approve()`/`reject()` semantics —
- `getRemainingServiceLines(Bill request)` / `getRemainingInventoryLines(Bill request)` — request lines with no `BillItem` elsewhere referencing them via `referanceBillItem`.
- `processServices(Bill request)` → calls `billBhtController.navigateToAddServicesFromItemRequest(...)` and returns its navigation outcome.
- `processInventory(Bill request)` → calls the Direct Issue controller's equivalent method.
- `reject(Bill request, String reason)` — cancels only the remaining (unfulfilled) request lines; if all lines are already fulfilled, this is a no-op / hidden action.

**`ItemRequestApiService`**: `approveRequest()` is deleted. `rejectRequest()` is adjusted to cancel only remaining lines. Status/response building is adjusted (see below).

### Status derivation & API response

`GET /api/itemrequests/{id}` status becomes per-line:
- Each line reports `PENDING` or `FULFILLED` (with the fulfilling bill's id, type, and the department user who saved it).
- Overall request status: `PENDING` (no lines fulfilled), `PARTIALLY_FULFILLED` (some but not all), `FULFILLED` (all lines fulfilled), `REJECTED` (remaining lines were rejected and none were ever fulfilled), or a mix — a request can be `PARTIALLY_FULFILLED_AND_REJECTED` if some lines were fulfilled and the rest rejected. (Exact enum-vs-string representation to be finalized in the implementation plan.)
- `ItemRequestResponseDTO.approvalBillId` (singular) is replaced with a list of `{billId, billType, lineIds[]}` entries, since a request can now produce more than one bill over time (one per visit per page).

### Cleanup

- Delete `BillTypeAtomic.INWARD_SERVICE_ITEM_APPROVAL`, `INWARD_SERVICE_ITEM_APPROVAL_CANCELLATION`, and `BillType.InwardServiceItemApproval` entirely (no production data exists under these values).
- Remove the special-case entry for `INWARD_SERVICE_ITEM_APPROVAL` from `PharmacyReportController.calculateBhtIssueValue()` (added for #21266) — no longer needed since real `DIRECT_ISSUE_INWARD_MEDICINE` / `INWARD_SERVICE_BILL` bills are already covered by existing COGS logic.
- Remove the now-dead stock/fee-building code in `ItemRequestApiService` (`saveServiceLineFees`, the inventory stock-deduction block in the old `approveRequest()`, etc.), keeping only the FIFO stock-lookup helper if it's still needed to compute "suggested" quantities/availability shown when pre-loading the Direct Issue page.

### Privileges

No new privileges. `InwardServiceItemRequestApproval` gates visibility of the pending queue and the two "Process…" buttons (in addition to the normal Add Services / Direct Issue page privileges, which still apply once there). `InwardServiceItemRequestRejection` still gates Reject.

## Testing plan

- Playwright: submit a mixed request via API → approve queue shows both buttons → click "Process Services/Investigations" → verify BHT/items pre-loaded in `inward_bill_service.xhtml` → save → verify a real `INWARD_SERVICE_BILL` bill created with `referenceBill` set and `referanceBillItem` set on its line → return to queue → verify only "Process Inventory Items" remains → click it → verify pre-loaded Direct Issue Native SQL page → save → verify `DIRECT_ISSUE_INWARD_MEDICINE` bill + stock deduction → verify queue now shows fully fulfilled.
- Verify COGS report picks up both bills without any special-casing.
- Verify Reject cancels only remaining lines when some lines are already fulfilled.
- Verify `GET /api/itemrequests/{id}` reflects per-line and overall status correctly at each stage.
