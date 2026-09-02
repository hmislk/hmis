# SAP S/4HANA Cloud Integration — Testing Guide

## Overview

This guide covers how to test the SAP integration against the SAP API Business Hub sandbox and against a local HMIS instance. No live SAP tenant is required for initial development.

---

## SAP API Business Hub Sandbox

The SAP API Business Hub provides free sandbox endpoints for both APIs used by this integration.

### APIs

| API | Sandbox base path |
|---|---|
| Journal Entry (FI) | `https://sandbox.api.sap.com/s4hanacloud/sap/opu/odata/sap/API_JOURNALENTRYITEMBASIC` |
| Material Document (MM) | `https://sandbox.api.sap.com/s4hanacloud/sap/opu/odata/sap/API_MATERIAL_DOCUMENT_SRV` |

### Getting an API Key

1. Sign up at https://api.sap.com (free SAP ID).
2. Go to **API Business Hub → Settings → Show API Key**.
3. Copy the key — it is sent as the `APIKey` header (not as a Bearer token).

> **Note**: The sandbox uses a static `APIKey` header, not OAuth. For sandbox testing, you can bypass `SapTokenService` and inject the sandbox key directly, or configure the Token URL to point to a mock token server.

---

## Local Config Setup (HMIS Admin UI)

Set the following `ConfigOption` values via the admin UI or `POST /api/config/setLongText/{key}/{value}`:

```
SAP Integration - Enabled               = true
SAP Integration - Base URL              = https://sandbox.api.sap.com/s4hanacloud
SAP Integration - Token URL             = <mock token URL or skip for sandbox>
SAP Integration - Client ID             = <any non-empty value for sandbox>
SAP Integration - Client Secret         = <any non-empty value for sandbox>
SAP Integration - Company Code          = 1710
SAP Integration - AR Account            = 11000
SAP Integration - Revenue Account       = 40000
SAP Integration - Currency              = USD
```

> **Security**: Never commit these values to any file in the project. Keep them in the HMIS database (ConfigOption) only, or in `C:\Credentials\` for local reference.

---

## Testing Billing Outbound (Push)

### 1. Check SAP is reachable

```bash
curl -X GET \
  "https://sandbox.api.sap.com/s4hanacloud/sap/opu/odata/sap/API_JOURNALENTRYITEMBASIC/$metadata" \
  -H "APIKey: <your-sandbox-api-key>" \
  -H "Accept: application/xml"
```

Expect HTTP 200 with OData metadata XML.

### 2. Push a bill

```bash
curl -X POST \
  "http://localhost:8080/hmis/api/sap/billing/push/12345" \
  -H "Finance: <hmis-api-key>"
```

Expected success response:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "billId": 12345,
    "billReference": "HMIS-2024-001234",
    "sapDocumentNumber": "0100000042",
    "companyCode": "1710",
    "fiscalYear": "2024"
  }
}
```

### 3. Check idempotency

Call the same push endpoint a second time. Expected response includes `"status":"already_pushed"`.

### 4. Check push status

```bash
curl -X GET \
  "http://localhost:8080/hmis/api/sap/billing/status/12345" \
  -H "Finance: <hmis-api-key>"
```

---

## Testing Billing Inbound (Confirm Webhook)

Simulate a SAP payment confirmation callback:

```bash
curl -X POST \
  "http://localhost:8080/hmis/api/sap/billing/confirm" \
  -H "Finance: <hmis-api-key>" \
  -H "Content-Type: application/json" \
  -d '{
    "sapDocumentNumber": "0100000042",
    "hmsBillReference":  "HMIS-2024-001234",
    "amount":            15000.00,
    "currency":          "LKR",
    "postingDate":       "2024-03-15",
    "note":              "Settled"
  }'
```

Expected success response:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "billId": 12345,
    "billReference": "HMIS-2024-001234",
    "sapDocumentNumber": "0100000042",
    "confirmedAt": "2024-03-15 14:32:00",
    "status": "confirmed"
  }
}
```

Call again to verify idempotency (`"status":"already_confirmed"`).

Check confirmation status:

```bash
curl -X GET \
  "http://localhost:8080/hmis/api/sap/billing/confirm/status/12345" \
  -H "Finance: <hmis-api-key>"
```

---

## Testing Inventory Sync

### 1. Check sandbox material documents

```bash
curl -X GET \
  "https://sandbox.api.sap.com/s4hanacloud/sap/opu/odata/sap/API_MATERIAL_DOCUMENT_SRV/A_MaterialDocItem?\$top=5&\$format=json" \
  -H "APIKey: <your-sandbox-api-key>"
```

Note the `Material` field values returned — these are the material numbers you will need to create matching `Item.code` values in HMIS.

### 2. Seed HMIS items

For each SAP material number returned, ensure there is an HMIS `Item` (or `PharmaceuticalItem`) with `code` equal to the SAP material number. Use the Pharmaceutical Items API:

```bash
curl -X POST \
  "http://localhost:8080/hmis/api/pharmaceutical_items/amp" \
  -H "Finance: <hmis-api-key>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Drug A", "code": "MED-001"}'
```

### 3. Trigger a sync

```bash
# Forward sync (uses stored watermark or 7-day fallback)
curl -X GET \
  "http://localhost:8080/hmis/api/sap/inventory/sync" \
  -H "Finance: <hmis-api-key>"

# Explicit date range (backfill — watermark not updated)
curl -X GET \
  "http://localhost:8080/hmis/api/sap/inventory/sync?fromDate=2024-01-01&toDate=2024-03-31" \
  -H "Finance: <hmis-api-key>"
```

Expected response:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "totalDocumentItems": 47,
    "matchedItems": 43,
    "unmatchedItems": 4,
    "syncFromDate": "2024-03-08",
    "syncToDate": "2024-03-15",
    "lastSyncTimestamp": "2024-03-15T23:59:59",
    "unmatchedMaterials": ["MAT-9001", "MAT-9002", "MAT-9003", "MAT-9004"],
    "warnings": []
  }
}
```

### 4. Verify watermark

After a forward sync, check the `SAP Integration - Inventory Last Sync` ConfigOption:

```bash
curl -X GET \
  "http://localhost:8080/hmis/api/config/search?keyword=Inventory+Last+Sync" \
  -H "Config: <config-api-key>"
```

For a backfill (explicit `fromDate`), the value should be unchanged.

---

## Integration Disabled Behaviour

Set `SAP Integration - Enabled = false` in config, then call any SAP endpoint. All operations return:

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "status": "skipped",
    "reason": "SAP integration is disabled"
  }
}
```

No HTTP calls to SAP are made.

---

## Common Errors

| Symptom | Likely cause |
|---|---|
| `SAP integration config key is not set: SAP Integration - Company Code` | Config key is blank — set via admin UI |
| `Bill not found: 12345` | Bill ID does not exist or is retired |
| `Bill 12345 is not yet completed` | Bill was not finalized before push |
| HTTP 401 from SAP | Token expired — `SapTokenService` will auto-refresh on next call |
| HTTP 502 from HMIS | SAP returned a non-2xx; check Payara server log for the full SAP error body |
| `unmatchedMaterials` non-empty | SAP material numbers have no corresponding `Item.code` in HMIS |
