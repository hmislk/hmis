# Pharmacy Purchase Orders API Documentation

## Overview
Read the status of a pharmacy Purchase Order request (its approval history and
any GRNs raised against the live approval), and cancel an approved PO
(`PHARMACY_ORDER_APPROVAL`) without SQL or an app restart. Backs issue #23944 —
this is the API-side equivalent of the UI's Cancel button on the PO print
pages (issue #23941), and shares the same underlying service
(`PharmacyPurchaseOrderApprovalCancellationService`), so behaviour and error
reasons match exactly.

## Authentication
All endpoints require API Key-based authentication using the `Finance` header.

**Header:**
```
Finance: <your-api-key>
```

## Base URL
```
/api/pharmacy_purchase_orders
```

## Endpoints

### 1. Get PO Status
Resolves a PO **request** bill (by number or id) and reports its approval
history — including cancelled approvals — plus any GRNs raised against the
currently-live approval.

**Endpoint:** `GET /api/pharmacy_purchase_orders/status?number={requestBillNumber}`

Also accepts `?billId={id}` in place of `number` (per
[API_BILL_NUMBER_SOLUTION.md](API_BILL_NUMBER_SOLUTION.md), `?number=` is the
right form for bill numbers containing `/`).

**Query Parameters:**
- `number` (string, optional): the request bill's number (deptId)
- `billId` (number, optional): the request bill's numeric id — use one or the other

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "request": {
      "id": 14077854,
      "billNumber": "POR/COOP/MP/26/001707",
      "finalized": false,
      "cancelled": false
    },
    "approvals": [
      {
        "id": 14079179,
        "billNumber": "MP/PO/26/050354",
        "cancelled": true,
        "cancelledBillId": 14080625,
        "cancelledBillNumber": "MPPOCAN/4"
      }
    ],
    "grns": [
      { "id": 1560210, "billNumber": "MP/GRN/25/012969", "cancelled": false }
    ]
  }
}
```

- `approvals` lists **every** approval ever made against this request,
  including cancelled ones — a request can have more than one on record (an
  earlier, superseded approval alongside the current live one).
- `grns` only lists GRNs raised against the request's *currently live*
  (non-cancelled) approval, if one exists.

### 2. Cancel an Approval
Cancels a `PHARMACY_ORDER_APPROVAL` bill via the same shared service the JSF
UI's Cancel button calls.

**Endpoint:** `POST /api/pharmacy_purchase_orders/approvals/{approvalBillId}/cancel`

**Path Parameters:**
- `approvalBillId` (number, required): the approval bill's id

**Request Body:**
```json
{
  "auditComment": "Approved by mistake - reverting to pending approval",
  "approvedBy": "Jane Doe"
}
```
Both fields are mandatory.

**Authorization:** the API key's WebUser must hold the `PharmacyOrderCancellation`
privilege (checked without a department filter, since an API caller has no
"currently selected department").

**Business rules** (identical to the UI):
- Refused if the approval is already cancelled.
- Refused if a live GRN (or GRN import) has been raised against it.
- Refused if the config option **Pharmacy Purchase Order Bill can be Cancelled**
  is switched off.
- On success, writes each cancelled line **once** (not duplicated), stamps the
  contra bill `PHARMACY_ORDER_APPROVAL_CANCELLED`, and clears the request's
  link back to this approval **only if this was the request's currently live
  approval** — cancelling a stale/superseded approval never disturbs a
  different, still-live one.

**Response (Success):**
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "cancelledBillId": 14080625,
    "cancelledBillNumber": "MPPOCAN/4",
    "requestBillId": 14077854,
    "pendingApproval": true
  }
}
```
`pendingApproval` is `true` only when this cancellation freed the request for
re-approval (i.e. the cancelled approval was the request's live one).

**Response (Error, e.g. GRN exists):**
```json
{
  "status": "error",
  "code": 409,
  "message": "Grn already head been Come u can't bill "
}
```

## Error Responses

| Code | Meaning | Example message |
|---|---|---|
| 400 | Missing/invalid request body | `auditComment is required` |
| 401 | Invalid or missing API key | `Not a valid key` |
| 403 | API user lacks `PharmacyOrderCancellation` | `API user does not have the PharmacyOrderCancellation privilege` |
| 404 | Request bill or approval bill not found | `No Pharmacy Purchase Order request bill found` / `No Bill to cancel` |
| 409 | Already cancelled / GRN exists / config disabled / not an approval bill | `Already Cancelled. Can not cancel again` |
| 500 | Unexpected server error | `An error occurred: ...` |

## Example cURL

```bash
# Status
curl -s -X GET "https://<host>/api/pharmacy_purchase_orders/status?billId=14077854" \
  -H "Finance: <your-api-key>"

# Cancel
curl -s -X POST "https://<host>/api/pharmacy_purchase_orders/approvals/14079179/cancel" \
  -H "Finance: <your-api-key>" -H "Content-Type: application/json" \
  -d '{"auditComment":"Approved by mistake","approvedBy":"Jane Doe"}'
```

## Related
- [API_COSTING_DATA.md](API_COSTING_DATA.md) — the `referenceBillId`,
  `cancelled`, `billedBillId` etc. fields these endpoints rely on are also
  exposed on `BillDetailsDTO` there.
- [API_BILL_DATA_CORRECTION.md](API_BILL_DATA_CORRECTION.md) — `BILL_ITEM`
  `retired` correction, for cleaning up a duplicate line without SQL.
