# Inward (Admission) API

Base path: `/api/apiInward`
Authentication: `Finance` header
Content-Type: `application/json` (POST), `application/json` (GET responses)

Provides access to inpatient admission data and payment processing for admitted patients.

## Endpoints

### GET `/api/apiInward/admissions` — List active admissions

Returns current inpatient admissions.

```bash
GET /api/apiInward/admissions
Header: Finance: YOUR_API_KEY
```

Response fields per admission: `bhtNo`, `patientName`, `patientPhone`, `admittedAt`, `ward`, `bed`, `doctor`, `diagnosis`

---

### GET `/api/apiInward/admissions/byPhone/{phone}` — Find admission by patient phone

```bash
GET /api/apiInward/admissions/byPhone/0771234567
Header: Finance: YOUR_API_KEY
```

Returns the active admission(s) for the patient with this mobile number.

---

### GET `/api/apiInward/banks` — List available banks/payment institutions

Returns the list of bank institutions available for credit card / bank payments.

```bash
GET /api/apiInward/banks
Header: Finance: YOUR_API_KEY
```

---

### GET `/api/apiInward/validateAdmission/{bht_no}/{phone}` — Validate patient admission

Checks whether a BHT number and phone number combination is valid for payment processing.

```bash
GET /api/apiInward/validateAdmission/BHT2026001/0771234567
Header: Finance: YOUR_API_KEY
```

Returns `{ "valid": true/false, "patientName": "...", "balance": 0.0 }`

---

### POST `/api/apiInward/payment` — Process an inward payment

Creates an online settlement payment for an admitted patient. Requires a bank institution ID (from `/api/apiInward/banks`).

```json
{
  "bht_no": "BHT2026001",
  "bank_id": 5,
  "reference_no": "REF123456",
  "amount": 5000.00,
  "payment_date": "2026-04-04 14:30:00"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `bht_no` | string | ✅ | Bed Head Ticket number |
| `bank_id` | long | ✅ | Bank institution ID from `/api/apiInward/banks` |
| `reference_no` | string | ✅ | Payment reference/transaction number |
| `amount` | double | ✅ | Amount to collect (must be > 0) |
| `payment_date` | string | ❌ | Date/time of payment `yyyy-MM-dd HH:mm:ss` (defaults to now) |

Response:
```json
{
  "bill": {
    "bill_no": "RH/2026/00123",
    "bht_no": "BHT2026001",
    "amount": 5000.0,
    "reference_no": "REF123456"
  },
  "error": "0",
  "error_description": ""
}
```

---

### GET `/api/apiInward/payment/{bht_no}/{bank_id}/{credit_card_ref}/{amount}` — Process payment (GET form)

Legacy GET-based payment endpoint for integrations that cannot POST.

```bash
GET /api/apiInward/payment/BHT2026001/5/REF123456/5000.00
Header: Finance: YOUR_API_KEY
```

---

## Notes

- `bht_no` is the Bed Head Ticket number assigned at admission
- Payment is processed as an **online settlement** — use `/api/apiInward/banks` to get valid `bank_id` values
- Always call `/api/apiInward/validateAdmission` first before processing payment
