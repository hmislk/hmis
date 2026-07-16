# Membership API Documentation for AI Agents

## Overview

This document provides comprehensive guidance for AI agents to interact with the Membership API. The API manages membership schemes, patient registration under a membership scheme, and membership billing/payment processing.

The API exposes five endpoints:

1. **Get Banks** – Retrieve the list of available bank institutions for payment
2. **Save Patient** – Register a new patient under a membership scheme
3. **Get Patient** – Look up a patient by their internal ID
4. **Get Service Value** – Retrieve the membership service fee and VAT details
5. **Pay for Membership** – Process a credit-card membership payment and activate the patient

## Base Configuration

- **Base URL**: `http://localhost:8080` (adjust for your environment)
- **API Base Path**: `/api/apiMembership`
- **Authentication**: None required (these endpoints are publicly accessible)
- **Content Type**: `application/json`
- **Date Format**: `yyyy-MM-dd` for date path parameters

## Reference Enums

### Title Values

Use exact enum name as the path parameter value:

| Enum Value | Display |
|-----------|---------|
| `Mr`       | Mr      |
| `Mrs`      | Mrs     |
| `Miss`     | Miss    |
| `Ms`       | Ms      |
| `Master`   | Master  |
| `Baby`     | Baby    |
| `Rev`      | Rev     |
| `RtRev`    | Rt. Rev.|
| `Hon`      | Hon.    |
| `RtHon`    | Rt. Hon.|
| `Dr`       | Dr      |
| `DrMrs`    | Dr. Mrs.|
| `DrMs`     | Dr. Ms. |
| `DrMiss`   | Dr. Miss|
| `Prof`     | Prof    |
| `ProfMrs`  | Prof. Mrs.|
| `Other`    | Other   |
| `Baby_Of`  | Baby of |

### Sex Values

| Enum Value | Description |
|-----------|-------------|
| `Male`    | Male        |
| `Female`  | Female      |
| `Unknown` | Unknown     |
| `Other`   | Other       |

---

## Endpoints

---

### 1. Get Banks

Returns the list of bank institutions available for credit-card payment.

**Method**: `GET`  
**Path**: `/api/apiMembership/banks`

**Parameters**: None

**Example Request**:
```bash
curl -X GET "http://localhost:8080/api/apiMembership/banks"
```

**Success Response** (`error: "0"`):
```json
{
  "bank": [
    { "bank_id": 16664599, "bank_name": "Amana Bank" },
    { "bank_id": 16664600, "bank_name": "Commercial Bank" }
  ],
  "error": "0",
  "error_description": ""
}
```

**Error Response** (no banks found):
```json
{
  "bank": "",
  "error": "1",
  "error_description": "No Data."
}
```

**Notes**:
- Returns only active (non-retired) institutions of type `Bank`.
- Use `bank_id` values from this response in the `payForMembership` endpoint.

---

### 2. Save Patient

Registers a new patient under the default membership scheme. The patient is created in a **de-active** (pending) state until payment is confirmed via `payForMembership`.

**Method**: `GET`  
**Path**: `/api/apiMembership/savePatient/{title}/{name}/{sex}/{dob}/{address}/{phone}/{nic}`

**Path Parameters**:

| Parameter | Type   | Required | Description |
|-----------|--------|----------|-------------|
| `title`   | String | Yes      | Patient title. Must be one of the `Title` enum values (e.g. `Mr`, `Mrs`, `Dr`) |
| `name`    | String | Yes      | Full name of the patient. Use `+` to encode spaces in the URL (e.g. `Dushan+Maduranga`) |
| `sex`     | String | Yes      | Patient sex. Must be one of the `Sex` enum values: `Male`, `Female`, `Unknown`, `Other` |
| `dob`     | String | Yes      | Date of birth in `yyyy-MM-dd` format (e.g. `1990-10-17`) |
| `address` | String | Yes      | Patient address. Use `+` for spaces and `,` for commas in the URL |
| `phone`   | String | Yes      | Phone number — exactly 10 digits, no dashes (e.g. `0788044212`) |
| `nic`     | String | Yes      | National Identity Card number (e.g. `13456789v`) |

**Example Request**:
```bash
curl -X GET "http://localhost:8080/api/apiMembership/savePatient/Mr/Dushan+Maduranga/Male/1990-10-17/Karapitiya,Galle/0788044212/13456789v"
```

**Success Response** (`error: "0"`):
```json
{
  "save_patient": {
    "save_patient_id": 123456,
    "save_patient_temp_code": "LM-0001",
    "save_patient_title": "Mr",
    "save_patient_name": "Dushan Maduranga",
    "save_patient_sex": "Male",
    "save_patient_dob": "1990-10-17T00:00:00.000+0000",
    "save_patient_address": "Karapitiya,Galle",
    "save_patient_phone": "078-8044212",
    "save_patient_nic": "13456789v",
    "save_patient_status": "De-Active"
  },
  "error": "0",
  "error_description": ""
}
```

**Error Response**:
```json
{
  "save_patient": "Please Enter Ten Numbers",
  "error": "1",
  "error_description": "No Data."
}
```

**Notes**:
- The returned `save_patient_id` is the patient's internal database ID — store this value to use in subsequent calls.
- The `save_patient_temp_code` is a temporary code; the permanent `save_patient_code` is assigned after successful payment.
- `save_patient_status` will be `De-Active` until `payForMembership` is called.
- Phone number must be exactly 10 digits; it is stored in the format `078-8044212` (first 3 digits, dash, next 7 digits).
- Validation errors returned in `save_patient` field: `"Please Enter title"`, `"Please Enter Name"`, `"Please Enter sex"`, `"Please Enter dob"`, `"Please Enter address"`, `"Please Enter phone"`, `"Please Enter Ten Numbers"`, `"Please Enter Nic"`.

---

### 3. Get Patient

Retrieves a patient record by their internal database ID.

**Method**: `GET`  
**Path**: `/api/apiMembership/patient/{patient_id}`

**Path Parameters**:

| Parameter    | Type   | Required | Description |
|-------------|--------|----------|-------------|
| `patient_id` | Long   | Yes      | The internal patient ID (as returned by `savePatient`) |

**Example Request**:
```bash
curl -X GET "http://localhost:8080/api/apiMembership/patient/123456"
```

**Success Response – Active patient** (`error: "0"`):
```json
{
  "patient": {
    "save_patient_id": 123456,
    "save_patient_title": "Mr",
    "save_patient_name": "Dushan Maduranga",
    "save_patient_sex": "Male",
    "save_patient_dob": "1990-10-17T00:00:00.000+0000",
    "save_patient_address": "Karapitiya,Galle",
    "save_patient_phone": "078-8044212",
    "save_patient_nic": "13456789v",
    "save_patient_code": "LM-0001",
    "save_patient_status": "Active"
  },
  "error": "0",
  "error_description": ""
}
```

**Success Response – De-Active patient** (`error: "0"`):
```json
{
  "patient": {
    "save_patient_id": 123456,
    "save_patient_title": "Mr",
    "save_patient_name": "Dushan Maduranga",
    "save_patient_sex": "Male",
    "save_patient_dob": "1990-10-17T00:00:00.000+0000",
    "save_patient_address": "Karapitiya,Galle",
    "save_patient_phone": "078-8044212",
    "save_patient_nic": "13456789v",
    "save_patient_temp_code": "LM-TEMP-0001",
    "save_patient_status": "De-Active"
  },
  "error": "0",
  "error_description": ""
}
```

**Error Response** (patient not found):
```json
{
  "patient": "",
  "error": "1",
  "error_description": "No Patient"
}
```

**Notes**:
- Active patients have `save_patient_code`; de-active patients have `save_patient_temp_code`.
- Use this endpoint to verify patient registration before processing payment.

---

### 4. Get Service Value

Returns the membership service item details including the fee, VAT, and total amount payable.

**Method**: `GET`  
**Path**: `/api/apiMembership/serviceValue`

**Parameters**: None

**Example Request**:
```bash
curl -X GET "http://localhost:8080/api/apiMembership/serviceValue"
```

**Success Response** (`error: "0"`):
```json
{
  "service": {
    "save_service_id": 17860304,
    "save_service_name": "Membership Fee",
    "save_service_value": 5000.0,
    "save_service_vat": 750.0,
    "save_service_value_plus_vat": 5750.0
  },
  "error": "0",
  "error_description": ""
}
```

**Error Response** (service item not found):
```json
{
  "service": "",
  "error": "1",
  "error_description": "No Sevice(Invalid ID)"
}
```

**Notes**:
- This endpoint returns a fixed service item configured in the system.
- `save_service_value` — base fee before VAT.
- `save_service_vat` — VAT amount.
- `save_service_value_plus_vat` — total amount the patient must pay.
- Use this to display the membership fee to the patient before payment.

---

### 5. Pay for Membership

Processes a credit-card payment for a membership and activates the patient account. On success, the patient's status changes from `De-Active` to `Active` and a permanent membership code is assigned.

**Method**: `GET`  
**Path**: `/api/apiMembership/payForMembership/{patient_id}/{bank_id}/{credit_card_ref}/{memo}`

**Path Parameters**:

| Parameter        | Type   | Required | Description |
|-----------------|--------|----------|-------------|
| `patient_id`     | Long   | Yes      | The internal patient ID (from `savePatient`) |
| `bank_id`        | Long   | Yes      | The bank institution ID (from `getBanks`) |
| `credit_card_ref`| String | Yes      | Credit card or online payment reference number |
| `memo`           | String | Yes      | Memo or notes for this transaction |

**Example Request**:
```bash
curl -X GET "http://localhost:8080/api/apiMembership/payForMembership/123456/16664599/REF-2024-001/MembershipPayment"
```

**Success Response** (`error: "0"`):
```json
{
  "PayForMembership": {
    "bill_no_ins": "INS-2024-00001",
    "bill_no_dept": "DEPT-2024-00001",
    "bill_patient_id": 123456,
    "bill_patient_name": "Dushan Maduranga",
    "bill_patient_code": "LM-0001",
    "bill_amount": 5000.0,
    "bill_amount_vat": 750.0,
    "bill_amount_with_vat": 5750.0,
    "bill_bank": "Amana Bank",
    "bill_crad_ref_no": "REF-2024-001"
  },
  "error": "0",
  "error_description": ""
}
```

**Error Response** (payment failed):
```json
{
  "PayForMembership": "",
  "error": "1",
  "error_description": "Payment Not Done."
}
```

**Notes**:
- After successful payment, the patient's `retired` flag is set to `false` (Active) and a permanent membership code (`LM-XXXX`) is assigned.
- Both the `Patient` and `Person` records are activated.
- The bill is created as an `OpdBill` with payment method `OnlineSettlement`.
- `bill_crad_ref_no` note: the field name in the JSON response has a typo (`crad` instead of `card`) — this is intentional and preserved for backward compatibility.
- Validation errors: `"No Patient(Wrong Patient Id)"`, `"No Bank(Wrong Bank Id)"`, `"No Item(Wrong Item Id)"`.

---

## Typical Membership Workflow

1. **List available banks**: `GET /api/apiMembership/banks` → save `bank_id`
2. **Check membership cost**: `GET /api/apiMembership/serviceValue` → present fee to patient
3. **Register patient**: `GET /api/apiMembership/savePatient/{...}` → save `save_patient_id`
4. **Verify registration**: `GET /api/apiMembership/patient/{patient_id}` → confirm `De-Active` status
5. **Process payment**: `GET /api/apiMembership/payForMembership/{patient_id}/{bank_id}/{credit_card_ref}/{memo}` → patient becomes `Active`

## Error Handling

All endpoints return a JSON object with:

| Field               | Type   | Description |
|--------------------|--------|-------------|
| `error`             | String | `"0"` for success, `"1"` for failure |
| `error_description` | String | Human-readable error message (empty on success) |

When `error` is `"1"`, the primary data field (e.g. `bank`, `save_patient`, `patient`, `service`, `PayForMembership`) will be an empty string `""`.
