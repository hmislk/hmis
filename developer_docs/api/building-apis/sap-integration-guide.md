# SAP S/4HANA Cloud Integration — Developer Guide

## Overview

The SAP integration provides bidirectional data exchange between HMIS and SAP S/4HANA Cloud:

- **Outbound (Billing)**: Push a finalized HMIS bill to SAP Finance (FI) as a journal entry.
- **Inbound (Billing)**: Receive a payment confirmation webhook from SAP and record it against the HMIS bill.
- **Inventory Sync**: Fetch SAP Materials Management (MM) goods-receipt documents and match them against the HMIS pharmacy item master for audit purposes.

All integration is controlled at runtime via `ConfigOption` keys. No credentials are hard-coded in source.

---

## Architecture

### Classes

| Class | Package | Role |
|---|---|---|
| `SapTokenService` | `com.divudi.service.sap` | `@Singleton` EJB — fetches and caches OAuth 2.0 client-credentials token |
| `SapOutboundService` | `com.divudi.service.sap` | `@Stateless` EJB — pushes bills to SAP FI, records confirmations |
| `SapInventorySyncService` | `com.divudi.service.sap` | `@Stateless` EJB — fetches SAP MM goods-receipt documents, matches to HMIS items |
| `SapBillingApi` | `com.divudi.ws.sap` | `@RequestScoped` JAX-RS — billing REST endpoints |
| `SapInventoryApi` | `com.divudi.ws.sap` | `@RequestScoped` JAX-RS — inventory sync REST endpoint |
| `SapIntegrationException` | `com.divudi.service.sap` | Checked exception for all SAP integration errors |

### DTOs

| DTO | Purpose |
|---|---|
| `SapJournalEntryDTO` | Request body for `POST /JournalEntry` (OData v2) |
| `SapJournalEntryResponseDTO` | Parsed response: `AccountingDocument`, `CompanyCode`, `FiscalYear` |
| `SapPaymentConfirmationDTO` | Inbound webhook body from SAP |
| `SapErrorResponseDTO` | SAP OData error envelope; `extractMessage()` drills into `error.message.value` |
| `SapMaterialDocumentDTO` | Single goods-receipt line from `A_MaterialDocItem` |
| `SapInventorySyncResultDTO` | Sync summary: counts, warnings, unmatched materials, watermark |

---

## Authentication

SAP S/4HANA Cloud uses OAuth 2.0 client credentials. `SapTokenService`:

1. Reads `SAP Integration - Token URL`, `SAP Integration - Client ID`, `SAP Integration - Client Secret` from `ConfigOption`.
2. POSTs `grant_type=client_credentials` with HTTP Basic auth (URL-encoded client ID + secret).
3. Caches the `access_token` until `expires_in - 30s`. All downstream services call `getBearerToken()`.
4. On HTTP 401 from a downstream API: caller invokes `tokenService.invalidate()`, then re-calls `getBearerToken()` for a fresh token (one retry only).

`SapTokenService.isEnabled()` returns `false` (and `getBearerToken()` returns `null`) when:
- Config key `SAP Integration - Enabled` is `false`, OR
- Any of Token URL / Client ID / Client Secret is blank.

---

## Configuration Keys

All keys are `ConfigOption` short-text or boolean values, managed via the admin UI or `POST /api/config/setBoolean|setLongText`.

### Core / Auth

| Key | Type | Description |
|---|---|---|
| `SAP Integration - Enabled` | Boolean | Master on/off switch. Must be `true` for any SAP call to proceed. |
| `SAP Integration - Base URL` | Short text | SAP tenant base URL, e.g. `https://{tenant}.s4hana.cloud.sap` |
| `SAP Integration - Token URL` | Short text | OAuth token endpoint, e.g. `https://{tenant}.authentication.sap.hana.ondemand.com/oauth/token` |
| `SAP Integration - Client ID` | Short text | OAuth client ID (from SAP BTP service binding) |
| `SAP Integration - Client Secret` | Short text | OAuth client secret (from SAP BTP service binding) |

### Billing

| Key | Type | Description |
|---|---|---|
| `SAP Integration - Company Code` | Short text | SAP company code, e.g. `1000` |
| `SAP Integration - AR Account` | Short text | G/L account for accounts-receivable debit line |
| `SAP Integration - Revenue Account` | Short text | G/L account for revenue credit lines |
| `SAP Integration - Currency` | Short text | ISO currency code, default `LKR` |

### Inventory Sync

| Key | Type | Description |
|---|---|---|
| `SAP Integration - Material Code Field` | Short text | HMIS `Item` field to match against SAP material number: `code` (default) or `barcode` |
| `SAP Integration - Inventory Last Sync` | Short text | ISO timestamp of last successful forward sync (e.g. `2024-03-15T23:59:59`). Auto-updated after each forward sync. |
| `SAP Integration - Inventory Sync From Days` | Short text | Fall-back look-back window in days when no prior sync exists. Default `7`. |

### Idempotency Records (auto-created, do not edit manually)

| Key pattern | Written by | Content |
|---|---|---|
| `SAP Bill Push - {billId}` | `SapOutboundService.recordPush()` | JSON: `sapDocNumber`, `fiscalYear`, `sentAt` |
| `SAP Bill Confirm - {billId}` | `SapOutboundService.confirmPayment()` | JSON: `sapDocNumber`, `amount`, `currency`, `postingDate`, `confirmedAt` |

---

## Billing Outbound — Journal Entry Mapping

When `POST /api/sap/billing/push/{billId}` is called:

1. **Guard checks** (in order):
   - SAP integration enabled (`isEnabled()`)
   - Idempotency: `SAP Bill Push - {billId}` ConfigOption already set → return `already_pushed`
   - Bill exists, not retired, not cancelled, `isCompleted() == true`

2. **Payload** built in `SapOutboundService.buildPayload()`:
   - One **debit** line: AR account, amount = `bill.getNetTotal()` (post-discount), indicator `S`
   - One **credit** line per non-retired `BillItem`: revenue account, amount = `bi.getNetValue()` (falls back to `bi.getGrossValue()` if net is 0), indicator `H`
   - `bill.getDeptId()` is the SAP `Reference` field (HMIS bill reference number)

3. **SAP OData path**: `POST {baseUrl}/sap/opu/odata/sap/API_JOURNALENTRYITEMBASIC/JournalEntry`

4. **Response**: SAP returns OData v2 `{"d": {"AccountingDocument": "...", "CompanyCode": "...", "FiscalYear": "..."}}`. The accounting document number is stored in the idempotency record.

5. **Amount format**: `String.format(Locale.ROOT, "%.2f", amount)` — locale-safe two decimal places.

---

## Billing Inbound — Payment Confirmation Webhook

`POST /api/sap/billing/confirm` receives:

```json
{
  "sapDocumentNumber": "0100000012",
  "hmsBillReference":  "HMIS-2024-001234",
  "amount":            15000.00,
  "currency":          "LKR",
  "postingDate":       "2024-03-15",
  "note":              "optional"
}
```

`hmsBillReference` matches `Bill.deptId`. The service looks up the bill by JPQL on `b.deptId`. Idempotency: if `SAP Bill Confirm - {billId}` already exists, returns `already_confirmed` without writing again. On DB failure, the exception propagates as a 5xx so SAP can retry.

---

## Inventory Sync Design

`GET /api/sap/inventory/sync?fromDate=yyyy-MM-dd&toDate=yyyy-MM-dd`

### SAP API

`GET {baseUrl}/sap/opu/odata/sap/API_MATERIAL_DOCUMENT_SRV/A_MaterialDocItem`

OData filter: `PostingDate ge datetime'{from}T00:00:00' and PostingDate le datetime'{to}T23:59:59'`

Selected fields: `MaterialDocument`, `MaterialDocumentItem`, `Material`, `Plant`, `StorageLocation`, `Quantity`, `BaseUnit`, `PostingDate`, `MaterialDocumentYear`

### Pagination

SAP returns server-driven pages. `SapInventorySyncService.fetchMaterialDocItems()` follows:
- OData v2: `{"d": {"results": [...], "__next": "..."}}` — follows `d.__next`
- OData v4: `{"value": [...], "@odata.nextLink": "..."}` — follows `@odata.nextLink`

Pages are fetched until `nextLink` is null.

### Item Matching

Each `SapMaterialDocumentDTO.Material` is matched against `Item.code` or `Item.barcode` (per `SAP Integration - Material Code Field`). Unmatched materials are accumulated in `SapInventorySyncResultDTO.unmatchedMaterials`. An unknown field name (not `code`/`barcode`) adds a warning to the result and falls back to `code`.

### Watermark Strategy

- **Forward sync** (no explicit `fromDate`): watermark advances to `resolvedTo + "T23:59:59"` (the effective SAP upper bound, not wall-clock time). Stored in `SAP Integration - Inventory Last Sync`.
- **Backfill** (explicit `fromDate` supplied): watermark is NOT advanced. Result includes a warning.
- `resolveFromDate()`: uses the date part (first 10 chars) of the stored watermark; falls back to `N` days ago.

### Read-Only Design

The sync is intentionally read-only (audit only). Actual GRN creation in HMIS requires pricing, supplier, and department context that is not present in SAP goods-receipt documents. Pharmacy GRN workflows handle real stock intake.

---

## REST Endpoints

### Billing API (`/api/sap/billing`)

| Method | Path | Description |
|---|---|---|
| `POST` | `/push/{billId}` | Push bill to SAP FI. Returns `sapDocumentNumber`, `companyCode`, `fiscalYear`. |
| `GET` | `/status/{billId}` | Return push idempotency record (or 404 if not yet pushed). |
| `POST` | `/confirm` | Receive SAP payment confirmation webhook. |
| `GET` | `/confirm/status/{billId}` | Return confirmation idempotency record (or 404 if not confirmed). |

### Inventory API (`/api/sap/inventory`)

| Method | Path | Description |
|---|---|---|
| `GET` | `/sync` | Trigger SAP MM sync. Query params: `fromDate`, `toDate` (both optional, `yyyy-MM-dd`). |

All endpoints authenticate via `Finance` header (HMIS API key).

---

## Error Handling

| Scenario | HTTP Status | Behaviour |
|---|---|---|
| SAP integration disabled | 200 | Returns `{"status":"skipped","reason":"SAP integration is disabled"}` |
| Bill already pushed | 200 | Returns `{"status":"already_pushed","pushRecord":"..."}` |
| Bill not found / retired | 400 | `SapIntegrationException` → 400 response |
| SAP returns 4xx | 502 | Error message extracted via `SapErrorResponseDTO.extractMessage()`, logged |
| SAP returns 401 | retry once | `invalidate()` + fresh token, then one retry |
| DB failure on `confirmPayment` | 500 | Exception propagates so SAP can retry the webhook |
| Inventory sync failure | 502 | Generic `"SAP sync failed"` returned; full exception logged server-side |

---

## Adding New Config Defaults

New config keys are registered in `ConfigOptionApplicationController.loadSapIntegrationConfigurationDefaults()`. This method is called on application start to seed empty defaults. Do not hard-code SAP credentials anywhere in the project.
