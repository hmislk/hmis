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

Creates a payment bill for an admitted patient.

```json
{
  "bhtNo": "BHT2026001",
  "amount": 5000.00,
  "paymentMethod": "Cash",
  "bankId": null,
  "creditCardRef": null,
  "institutionCode": "RH"
}
```

Response includes the generated bill number and payment confirmation.

---

### GET `/api/apiInward/payment/{bht_no}/{bank_id}/{credit_card_ref}/{amount}` — Process payment (GET form)

Alternative GET-based payment endpoint for integrations that cannot POST.

```bash
GET /api/apiInward/payment/BHT2026001/5/REF123456/5000.00
Header: Finance: YOUR_API_KEY
```

---

## Notes

- `bhtNo` is the Bed Head Ticket number assigned at admission
- Payment methods: `Cash`, `Card`, `Cheque`, `OnlineTransfer`
- Always validate the admission first before processing payment
