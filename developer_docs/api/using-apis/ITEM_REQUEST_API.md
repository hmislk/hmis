# Item Request API (Issue #21793)

External systems (e.g. Ruhunu Hospital's request tooling) submit **item/service
requests** — meals such as Breakfast/Lunch/Dinner (`InwardService` items) and
stock items such as Water Bottle/Tea/Milk/Sugar — against a patient's active
BHT. Requests start **Pending** (no charge, no stock movement) and are routed
to a **target department's** in-app approval queue. A department user
**approves** (charges the BHT and, for stock items, deducts stock atomically
— the whole approval fails if any line has insufficient stock) or **rejects**
(records a reason, no charge/stock change) the request via the JSF page at
`/inward/item_request_pending_list.xhtml`.

**Approval and rejection are in-app only — there is no API for them.**
External systems poll `GET /api/itemrequests/{id}` to learn the outcome.

## Authentication

Same as all other HMIS REST APIs: send the API key in the `Finance` HTTP
header. The key must belong to an active, non-retired `WebUser` and must not
be expired (`ApiKey.dateOfExpiary`).

## Response envelope

```json
{"status": "success", "code": 200, "data": { ... }}
{"status": "error", "code": 400, "message": "..."}
```

## Endpoints

### POST /api/itemrequests

Submit a new item/service request.

Request body:
```json
{
  "bhtNo": "RH/2026/001234",
  "targetDepartmentId": 42,
  "comments": "Patient requested via mobile app",
  "lines": [
    {"itemId": 1001, "qty": 1},
    {"itemId": 2002, "qty": 2}
  ]
}
```

Response (201): an `ItemRequestResponseDTO` with `status: "PENDING"`.

Validation:
- `bhtNo` must resolve to a non-retired, non-discharged, not-yet-payment-finalized `PatientEncounter`.
- `targetDepartmentId` must resolve to a non-retired `Department`.
- At least one line is required; every `itemId` must resolve to a non-retired `Item`; every `qty` must be `> 0`.

### GET /api/itemrequests/{id}

Fetch a single request's status and line detail (for polling).

Response (200): an `ItemRequestResponseDTO`:
```json
{
  "id": 98765,
  "requestNo": "IR/2026/000123",
  "bhtNo": "RH/2026/001234",
  "targetDepartmentId": 42,
  "targetDepartmentName": "Ward Kitchen Store",
  "status": "PENDING",
  "comments": "Patient requested via mobile app",
  "rejectionReason": null,
  "createdAt": "2026-07-02 09:15:00",
  "createdBy": "api-user",
  "approvalBillId": null,
  "decidedAt": null,
  "decidedBy": null,
  "lines": [
    {"billItemId": 111, "itemId": 1001, "itemName": "Breakfast", "itemType": "SERVICE", "qty": 1.0, "netValue": 0.0},
    {"billItemId": 112, "itemId": 2002, "itemName": "Water Bottle", "itemType": "INVENTORY", "qty": 2.0, "netValue": 0.0}
  ]
}
```
404 if the id doesn't resolve to an item request.

### GET /api/itemrequests

List/search requests. Query parameters (all optional):

| Param | Format | Notes |
|---|---|---|
| `targetDepartmentId` | number | filter by target department |
| `status` | `PENDING`\|`APPROVED`\|`REJECTED`\|`CANCELLED` | filter by derived status |
| `fromDate` / `toDate` | `yyyy-MM-dd` | filter by submission date |
| `limit` | number | max rows returned |

Response (200): a list of lightweight `ItemRequestSearchResultDTO` rows
(`id`, `requestNo`, `bhtNo`, `targetDepartmentName`, `status`, `createdAt`).

### PUT /api/itemrequests/{id}/cancel

Withdraw a request while it is still **PENDING** (for the submitting system
to cancel its own request before a department user has acted on it).

Request body:
```json
{"reason": "Duplicate submission"}
```

Response (200): the updated `ItemRequestResponseDTO` with `status: "CANCELLED"`.
409 if the request has already been approved or rejected.

## Status lifecycle

```
PENDING --(department approves)--> APPROVED
PENDING --(department rejects)-->   REJECTED
PENDING --(requester cancels)-->    CANCELLED
```

Status is derived, not stored directly:
- `Bill.cancelled = true` + `billTypeAtomic = INWARD_SERVICE_ITEM_REJECTION` → `REJECTED`
- `Bill.cancelled = true` + `billTypeAtomic = INWARD_SERVICE_ITEM_REQUEST_CANCELLATION` → `CANCELLED`
- A non-cancelled `Bill` exists with `referenceBill` = this request and
  `billTypeAtomic = INWARD_SERVICE_ITEM_APPROVAL` → `APPROVED`
- Otherwise → `PENDING`

## Error codes

| Code | Meaning |
|---|---|
| 400 | Validation error (unknown/retired item, non-positive qty, invalid/discharged/finalized BHT, unknown department, malformed JSON) |
| 401 | Missing or invalid `Finance` API key |
| 404 | Unknown request id |
| 409 | Attempting to cancel a request that's already been approved/rejected/cancelled |
| 500 | Unexpected server error |

## Master-data prerequisites (setup, not code)

Before requests can be submitted for a given hospital, make sure the
requestable items exist:

1. **Meal/service items** (Breakfast, Lunch, Dinner) — create as `InwardService`
   items via the existing `POST /api/services` endpoint (`ServiceApi`). Each
   item needs at least one `ItemFee` configured — approving a service item
   with zero `ItemFee` rows fails with "No fee configured for service item"
   (silently charging zero would under-bill the BHT).
2. **Inventory items** (Water Bottle, Tea, Milk, Sugar) — provision as normal
   stock-tracked items (VTM/VMP/AMP as applicable) via the existing
   pharmaceutical item management API/flow, then stock them for the target
   department via the normal GRN or stock-adjustment flow. Approval deducts
   from whichever `Stock` row (for the target department) has sufficient
   quantity, earliest-expiry first, and charges the BHT at that batch's
   **retail sale rate** (`ItemBatch.retailsaleRate` × qty) on the approval
   bill line, mirroring the ward BHT-issue pricing.

## AI chat tool

The `manage_item_requests` tool (see `AnthropicApiService`) exposes the same
4 endpoints to the in-app AI assistant for GET/POST/PUT — approval/rejection
remain in-app only, not exposed via the tool either.
