# Inward (Admission) API

Base path: `/api/apiInward`
Authentication: `Finance` header
Content-Type: `application/json` (POST), `application/json` (GET responses)

Provides access to inpatient admission data and payment processing for admitted patients.

## Endpoints

### GET `/api/apiInward/admissions` — Financial worklist: admissions needing billing attention

Returns admissions that need billing attention: admissions that are **not yet finalized/discharged** (open, regardless of balance), plus finalized-and-discharged admissions that still have an **outstanding balance > 0.1**. Not a full admission-detail record — it's a financial worklist (up to 20 of each kind, most recent first).

```bash
GET /api/apiInward/admissions
Header: Finance: YOUR_API_KEY
```

Response fields per admission:

| Field | Type | Description |
|-------|------|-------------|
| `patient_encounter_id` | long | `PatientEncounter`/`Admission` primary key |
| `bht_no` | string | Bed Head Ticket number |
| `patient_mrn` | string | Patient's PHN (medical record number) |
| `patient_name` | string | Patient's full name (or `"No Person Details"` if unavailable) |
| `patient_home_phone` | string | Patient's home phone |
| `patient_mobile` | string | Patient's mobile number |
| `patient_nic` | string | Patient's NIC |
| `net_total` | double | Final bill net total (only present once a final bill exists) |
| `paid_amount` | double | Amount paid so far, including credit payments |
| `balance` | double | `net_total - paid_amount` |

Example response:
```json
{
  "admission": [
    {
      "patient_encounter_id": 384021,
      "bht_no": "BHT2026001",
      "patient_mrn": "P00012345",
      "patient_name": "K. G. Perera",
      "patient_home_phone": "0112345678",
      "patient_mobile": "0771234567",
      "patient_nic": "901234567V",
      "net_total": 45000.0,
      "paid_amount": 20000.0,
      "balance": 25000.0
    }
  ],
  "error": "0",
  "error_description": ""
}
```

If there are no matching admissions:
```json
{ "admission": "", "error": "1", "error_description": "No Data." }
```

---

### GET `/api/apiInward/admissions/byPhone/{phone}` — Find admission by patient phone

Same field shape and filtering rules as `/admissions` above (not-yet-finalized, or finalized-with-balance), restricted to admissions where the patient's or guardian's mobile/phone matches `{phone}`.

```bash
GET /api/apiInward/admissions/byPhone/0771234567
Header: Finance: YOUR_API_KEY
```

Example response:
```json
{
  "admission": [
    {
      "patient_encounter_id": 384021,
      "bht_no": "BHT2026001",
      "patient_mrn": "P00012345",
      "patient_name": "K. G. Perera",
      "net_total": 45000.0,
      "paid_amount": 20000.0,
      "balance": 25000.0
    }
  ],
  "error": "0",
  "error_description": ""
}
```

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

Response fields:

| Field | Type | Description |
|-------|------|-------------|
| `error` | string | `"0"` = valid, `"1"` = not valid or error |
| `error_description` | string | `"Valid"`, `"Not Valid"`, or `"Invalid Argument."` |

Example (valid):
```json
{ "error": "0", "error_description": "Valid" }
```
Example (not valid):
```json
{ "error": "1", "error_description": "Not Valid" }
```

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
- It is recommended to call `/api/apiInward/validateAdmission` before processing payment as a best practice; however `POST /payment` does not enforce this as a hard precondition and will accept requests without a prior validate call
- `/admissions` and `/admissions/byPhone/{phone}` are financial worklists (unpaid/open admissions), not general admission search — `GET /api/inward/admissions` (see [API_ADMISSION_DETAILS.md](API_ADMISSION_DETAILS.md)) supports general admission list/search by BHT/name/MRN/phone/NIC/status, but it is not a full-detail lookup by admission ID either — it has no ID path parameter and does not expose every `PatientEncounter` field

## See also

- [Admission Search API](API_ADMISSION_DETAILS.md) — `GET /api/inward/admissions` lists all currently active admissions, or searches past/current admissions by BHT/name/MRN/phone/NIC, with no financial scoping or row cap
- [Admission Number Counters API](API_ADMISSION_NUMBERS.md) — view/reset the BHT/OPD-card number sequence (not admission content)
- [Forms API](../../forms/form-api-guide.md) — `GET /api/forms/entries/{admissionId}` returns clinical form entries recorded against an admission
