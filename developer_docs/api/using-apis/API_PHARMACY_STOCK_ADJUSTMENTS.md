# Pharmacy Stock Adjustment & Batch Creation API

Covers the `/api/pharmacy_adjustments` and `/api/pharmacy_batches` endpoint groups: searching
stocks/departments/items, adjusting quantity/rates/expiry on an existing stock batch, and
creating new AMPs/batches. For pharmaceutical item master CRUD (VTM/ATM/VMP/AMP/VMPP/AMPP) and
backfill operations, see [API_PHARMACEUTICAL_MANAGEMENT.md](API_PHARMACEUTICAL_MANAGEMENT.md).

## Base Configuration

- **Base Paths**: `/api/pharmacy_adjustments`, `/api/pharmacy_batches`
- **Authentication**: `Finance` header (API key)
- **Content Type**: `application/json`
- **Date Format**: `yyyy-MM-dd` request, `yyyy-MM-dd HH:mm:ss` response

## Search APIs

### `GET /pharmacy_adjustments/search/departments`
Params: `query` (required, name search), `limit` (default 20, max 50).

```json
{"status":"success","code":200,"data":[{"id":1,"name":"Main Pharmacy","code":"PHARM01"}]}
```

### `GET /pharmacy_adjustments/search/stocks`
Params: `query` + `department` (both required, exact department match). Optional:
`minQuantity`/`maxQuantity`, `minRetailRate`/`maxRetailRate`, `minCostRate`/`maxCostRate`,
`minPurchaseRate`/`maxPurchaseRate`, `expiryAfter`/`expiryBefore` (`yyyy-MM-dd`), `batchNo`,
`includeZeroStock` (default false), `limit` (default 30, max 100).

```json
{"status":"success","code":200,"data":[{
  "id":123,"stockId":456,"itemBatchId":789,"itemName":"Paracetamol 500mg","code":"PAR001",
  "batchNo":"B001234","retailRate":25.0,"stockQty":150.0,"dateOfExpire":"2025-12-31",
  "purchaseRate":20.0,"wholesaleRate":22.5,"costRate":21.0,"allowFractions":false
}]}
```

### `GET /pharmacy_adjustments/search/items`
Params: `query` (required — name/code/barcode), `limit` (default 30, max 50).

```json
{"status":"success","code":200,"data":[{"id":1,"name":"Paracetamol 500mg","code":"PAR001","barcode":"1234567890123","genericName":"Paracetamol"}]}
```

## Adjustment APIs

All four take `stockId`, `departmentId`, `comment` (required, non-empty) plus the field being
changed, and return a `PHARMACY_STOCK_ADJUSTMENT`-family bill.

| Endpoint | Field | Notes |
|---|---|---|
| `POST /pharmacy_adjustments/stock_quantity` | `newQuantity` | Physical count corrections |
| `POST /pharmacy_adjustments/retail_rate` | `newRetailRate` | ≥ 0 |
| `POST /pharmacy_adjustments/purchase_rate` | `newPurchaseRate` | ≥ 0 |
| `POST /pharmacy_adjustments/expiry_date` | `newExpiryDate` (`yyyy-MM-dd`) | Supplier data corrections |

Request:
```json
{"stockId":456,"newQuantity":150.0,"comment":"Physical count adjustment","departmentId":1}
```

Response:
```json
{"status":"success","code":200,"data":{
  "billId":54321,"billNumber":"ADJ/2025/000123","stockId":456,"stockType":"QUANTITY",
  "beforeValue":100.0,"afterValue":150.0,"comment":"Physical count adjustment",
  "adjustmentDate":"2025-01-03 14:30:00"
}}
```

## Batch Creation APIs

### `POST /pharmacy_batches/amp/search_or_create`
Finds an AMP by name; creates one if not found. Body: `name` (required), `genericName`,
`categoryId`, `dosageFormId` (all optional — uses any available VMP if `genericName` omitted).
Auto-generates `code` from `name` (lowercase, underscored). Response `data.created` is `true`
if newly created.

### `POST /pharmacy_batches/create`
Creates an `ItemBatch` + `Stock` for a department.

```json
{"itemId":1234,"batchNo":"BATCH001","expiryDate":"2025-12-31","retailRate":100.0,
 "purchaseRate":85.0,"costRate":null,"wholesaleRate":90.0,"departmentId":456,
 "comment":"Initial stock creation"}
```

- `purchaseRate` defaults to 85% of `retailRate` if null; `costRate` defaults to `purchaseRate`.
- `batchNo` auto-generates as `"B" + timestamp` if omitted.
- Duplicate `(itemId, batchNo, expiryDate)` reuses the existing batch and just creates a new
  `Stock` row for the given department — response `message` says which happened.

### `GET /pharmacy_batches/amp/search`
Params: `name` (required), `limit` (default 30, max 50). Search-only, never creates.

## Typical Workflow

1. `search/departments` → get `id` → 2. `search/stocks?department=<name>&query=<item>` → get
`stockId` → 3. If multiple matches, present them to the user (name, batch, qty, expiry) for
selection → 4. Call the relevant adjustment endpoint with the chosen `stockId`.

For a brand-new item: `amp/search_or_create` → `pharmacy_batches/create` with the returned
`itemId`.

## Error Handling

Standard envelope on failure: `{"status":"error","code":<400|401|404|500>,"message":"..."}`.
- `400` invalid/missing params · `401` bad API key · `404` no matching stock/department ·
  `500` server error.
- No search results: retry with a shorter/partial `query` before giving up, or broaden the
  department filter.
