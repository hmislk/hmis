# Getting Admission Details — Consolidated Guide

There is **no single endpoint** that returns one admission's full detail (demographics + clinical + billing) by ID. Admission-related data is split across three separate APIs, each covering a different slice. This guide indexes all three with worked examples so you can pick the right one instead of guessing.

| Need | Endpoint | Doc |
|---|---|---|
| List all currently active (not-discharged) admissions, or search past/current admissions by BHT/name/MRN/phone/NIC | `GET /api/inward/admissions` | below |
| Which admissions still owe money (financial worklist) | `GET /api/apiInward/admissions` | below |
| Find a patient's open/unpaid admission by phone | `GET /api/apiInward/admissions/byPhone/{phone}` | below |
| Confirm a BHT number + phone combo is valid before taking payment | `GET /api/apiInward/validateAdmission/{bht_no}/{phone}` | [API_INWARD.md](API_INWARD.md) |
| Take an online payment against a BHT | `POST /api/apiInward/payment` | [API_INWARD.md](API_INWARD.md) |
| Bank list for the payment form | `GET /api/apiInward/banks` | [API_INWARD.md](API_INWARD.md) |
| View/reset the BHT/OPD-card number sequence counter | `GET`/`PUT /api/admission-numbers` | [API_ADMISSION_NUMBERS.md](API_ADMISSION_NUMBERS.md) |
| Clinical form entries recorded against an admission | `GET /api/forms/entries/{admissionId}` | below |

All endpoints use the `Finance` header for authentication unless noted.

---

## 1. General-purpose admission search — `AdmissionSearchApi`

`GET /api/inward/admissions` mirrors the staff-facing search page (`/inward/inpatient_search.xhtml`, `AdmissionController.searchAdmissions()`) — unlike `ApiInward`'s worklist endpoints below, it is **not** scoped to unpaid/open admissions and has no row cap (paginated instead).

All query params are optional:

| Param | Description |
|---|---|
| `status` | `ADMITTED_BUT_NOT_DISCHARGED` (default), `DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED`, `DISCHARGED_AND_FINAL_BILL_COMPLETED`, `ANY_STATUS` |
| `bhtNo` | Bed Head Ticket number (partial match) |
| `patientName` | Patient name (partial match) |
| `mrn` | Patient MRN/PHN or patient code (exact match) |
| `phone` | Patient or guardian phone/mobile (exact match) |
| `nic` | Patient NIC/passport (exact match) |
| `admissionTypeId` | Numeric `AdmissionType` ID |
| `institutionId` | Numeric `Institution` ID |
| `departmentId` | Numeric `Department` ID |
| `fromDate` / `toDate` | Admission date range, `yyyy-MM-dd HH:mm:ss` — must be supplied together |
| `page` | Default 1 |
| `size` | Default 50, max 200 |

### List currently active (not-discharged) admissions

```bash
curl -H "Finance: YOUR_API_KEY" \
  https://hmis.example.com/api/inward/admissions
```

### Search past or current admissions by phone or MRN

```bash
curl -H "Finance: YOUR_API_KEY" \
  "https://hmis.example.com/api/inward/admissions?status=ANY_STATUS&mrn=P00012345"

curl -H "Finance: YOUR_API_KEY" \
  "https://hmis.example.com/api/inward/admissions?status=ANY_STATUS&phone=0771234567"
```

Example response:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "admissions": [
      {
        "patientEncounterId": 384021,
        "bhtNo": "BHT2026001",
        "patientMrn": "P00012345",
        "patientCode": "PC00123",
        "patientName": "K. G. Perera",
        "patientPhone": "0112345678",
        "patientMobile": "0771234567",
        "patientNic": "901234567V",
        "patientAddress": "12 Main St, Galle",
        "patientArea": "Galle",
        "referringDoctor": "Dr. J. Silva",
        "admissionType": "Ward Admission",
        "institution": "Ruhunu Hospital",
        "department": "Medical Ward 1",
        "currentRoom": "Ward 1 - Bed 4",
        "dateOfAdmission": "2026-08-01 10:15:00",
        "dateOfDischarge": null,
        "discharged": false,
        "paymentFinalized": false,
        "netTotal": 45000.0,
        "paidAmount": 20000.0,
        "balance": 25000.0
      }
    ],
    "page": 1,
    "size": 50,
    "totalCount": 128
  }
}
```

`netTotal`/`paidAmount`/`balance` are only present once a final bill exists for the admission, matching `ApiInward`'s behavior. `totalCount` reflects the full matching set independent of pagination.

---

## 2. Financial worklist — `ApiInward`

Full reference: [API_INWARD.md](API_INWARD.md). These two endpoints don't return *all* admissions — they return admissions that still need attention: not-yet-finalized/discharged admissions, plus finalized-and-discharged ones with an outstanding balance greater than 0.1 (capped at 20 of each kind, newest first).

### List admissions with money owed

```bash
curl -H "Finance: YOUR_API_KEY" \
  https://hmis.example.com/api/apiInward/admissions
```

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

### Find by patient/guardian phone

```bash
curl -H "Finance: YOUR_API_KEY" \
  https://hmis.example.com/api/apiInward/admissions/byPhone/0771234567
```

Same field shape, filtered to admissions where the patient's or guardian's mobile/home phone matches. Returns `{"admission": "", "error": "1", "error_description": "No Data."}` when nothing matches.

---

## 3. BHT number sequence — `AdmissionNumberApi`

Full reference: [API_ADMISSION_NUMBERS.md](API_ADMISSION_NUMBERS.md). This is **not** admission content — it manages the counter that generates the next BHT/OPD-card number for a given admission type.

```bash
curl -H "Finance: YOUR_API_KEY" \
  "https://hmis.example.com/api/admission-numbers?admissionTypeId=1192"
```

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "admissionTypeId": 1192,
    "admissionTypeName": "OPD Card",
    "institutionId": null,
    "institutionBased": false,
    "lastAdmissionNumber": 48213,
    "nextAdmissionNumber": 48214
  }
}
```

Resetting the counter (`PUT`) requires an `expectedLastAdmissionNumber` compare-and-set guard — see the full doc for the request body and error codes.

---

## 4. Clinical form entries — `FormApi`

Full reference: [`developer_docs/forms/form-api-guide.md`](../../forms/form-api-guide.md). Returns the filled-in clinical forms (ward assessments, nursing notes, etc.) recorded against an admission — not demographic or billing data.

```bash
curl -H "Finance: YOUR_API_KEY" \
  https://hmis.example.com/api/forms/entries/384021
```

`384021` is the `PatientEncounter`/`Admission` primary key (same `patient_encounter_id` returned by `/api/apiInward/admissions`).

```json
[
  {
    "id": 9021,
    "formTemplateId": 12,
    "formTemplateName": "Ward Assessment",
    "comments": "Stable, no complaints",
    "createdAt": "2026-08-05T09:30:00.000+0000",
    "createdBy": "nurse.silva"
  }
]
```

To get the actual field values captured on a specific entry, follow up with:

```bash
curl -H "Finance: YOUR_API_KEY" \
  https://hmis.example.com/api/forms/entries/9021/values
```

---

## Getting full admission demographics/clinical fields

`/api/inward/admissions` (section 1) now covers the common demographic/status/location fields a caller typically needs (patient identity, admission type, institution/department, current room, discharge status, financials). If you need fields it still doesn't expose — e.g. detailed vitals (`weight`, `sbp`, `dbp`, `bmi`, etc.), discharge condition/instructions, guardian relationship — these live on the `PatientEncounter` entity (`src/main/java/com/divudi/core/entity/PatientEncounter.java`) but are **not currently exposed via REST**. If a caller needs one of these fields, file a GitHub issue describing the specific fields required rather than reaching for direct DB/entity access — see [api-development](../building-apis/) for the pattern to follow when adding a new endpoint.

## History

[hmislk/hmis#22754](https://github.com/hmislk/hmis/issues/22754) tracked the original gap — no endpoint could list all currently active admissions or search past/current admissions by phone or MRN, only the `ApiInward` financial worklist existed. `AdmissionSearchApi` (`GET /api/inward/admissions`, section 1) closed this gap.
