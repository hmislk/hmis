# FHIR API Documentation

## Overview

The HMIS exposes two FHIR-related API groups:

1. **FHIR Financial Data** (`/api/fhir`) — HL7 FHIR R5-compliant access to invoices, GRN records, and payments. Uses the `Finance` header for authentication.
2. **FHIR Patient** (`/api/fhir/Patient`) — FHIR R5 Patient resource operations (search, read, create, update). Uses the `FHIR` header for authentication.

> **Important**: The two groups use **different authentication headers**. Do **not** mix them.

---

## 1. FHIR Financial Data (`/api/fhir`)

### Authentication

```
Finance: <your-api-key>
```

### Base URL

```
/api/fhir
```

### Content Type

`application/json`

### Endpoints

#### GET `/api/fhir/cash_invoice/{institution_code}/{last_invoice_id}`

Returns cash invoices (pharmacy retail sale bills) for the given institution that have an ID greater than `last_invoice_id`. Use `0` for `last_invoice_id` to retrieve all invoices.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| institution_code | String | Institution code |
| last_invoice_id | Long | Return invoices with ID > this value (use 0 for all) |

---

#### GET `/api/fhir/credit_invoice/{institution_code}/{last_invoice_id}`

Returns credit invoices for the given institution.

**Path Parameters:** same as `cash_invoice`.

---

#### GET `/api/fhir/invoicereturn/{institution_code}/{last_return_invoice_id}`

Returns invoice return records for the given institution.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| institution_code | String | Institution code |
| last_return_invoice_id | Long | Return records with ID > this value |

---

#### GET `/api/fhir/grn/{institution_code}/{last_grn_id}`

Returns Goods Receipt Note (GRN) records for the given institution.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| institution_code | String | Institution code |
| last_grn_id | Long | Return GRNs with ID > this value |

---

#### GET `/api/fhir/grnreturn/{institution_code}/{last_return_grn_id}`

Returns GRN return records for the given institution.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| institution_code | String | Institution code |
| last_return_grn_id | Long | Return GRN returns with ID > this value |

---

#### GET `/api/fhir/payment/{institution_code}/{last_payment_id}`

Returns payment records for the given institution.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| institution_code | String | Institution code |
| last_payment_id | Long | Return payments with ID > this value |

---

#### GET `/api/fhir/paymentreturn/{institution_code}/{last_return_payment_id}`

Returns payment return records for the given institution.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| institution_code | String | Institution code |
| last_return_payment_id | Long | Return payment returns with ID > this value |

---

## 2. FHIR Patient (`/api/fhir/Patient`)

### Authentication

```
FHIR: <your-api-key>
```

> **Note**: This endpoint uses the `FHIR` header, **not** the `Finance` header used by most other HMIS APIs.

### Base URL

```
/api/fhir/Patient
```

### Content Type

`application/fhir+json`

### Identifier Systems

| System URI | Meaning |
|------------|---------|
| `urn:hmis:mrn` | HMIS internal Medical Record Number |
| `urn:lk:phn` | Sri Lanka Personal Health Number |
| `urn:lk:nic` | Sri Lanka National Identity Card |

### Endpoints

#### GET `/api/fhir/Patient/{id}`

Retrieve a single patient by the HMIS internal database ID. Returns a FHIR R5 `Patient` resource.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | Long | HMIS internal patient ID |

**Example:**

```bash
curl -H "FHIR: <your-api-key>" \
     -H "Accept: application/fhir+json" \
     http://localhost:8080/api/fhir/Patient/12345
```

---

#### GET `/api/fhir/Patient?name=&phone=&identifier=`

Search for patients. At least one query parameter must be provided. Returns a FHIR R5 `Bundle` (searchset) containing matching `Patient` resources (max 50 results).

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| name | String | No* | Patient name (partial match) |
| phone | String | No* | Phone number (partial match) |
| identifier | String | No* | Identifier value (MRN, PHN, or NIC) |

\* At least one parameter must be supplied.

**Example:**

```bash
curl -H "FHIR: <your-api-key>" \
     -H "Accept: application/fhir+json" \
     "http://localhost:8080/api/fhir/Patient?name=Perera"
```

---

#### POST `/api/fhir/Patient`

Create a new patient from a FHIR R5 `Patient` resource. Returns the created patient as a FHIR `Patient` resource with HTTP 201 Created.

**Request Body:** FHIR R5 `Patient` JSON (`application/fhir+json`)

Mapped fields:
- `name[0].given` → first name, `name[0].family` → last name
- `gender` → sex
- `birthDate` → date of birth
- `telecom[system=phone].value` → phone number
- `address[0].text` → address
- `identifier[system=urn:hmis:mrn].value` → MRN
- `identifier[system=urn:lk:phn].value` → PHN
- `identifier[system=urn:lk:nic].value` → NIC

**Example:**

```bash
curl -X POST \
     -H "FHIR: <your-api-key>" \
     -H "Content-Type: application/fhir+json" \
     -d '{"resourceType":"Patient","name":[{"family":"Silva","given":["Nimal"]}],"gender":"male","birthDate":"1985-06-15"}' \
     http://localhost:8080/api/fhir/Patient
```

---

#### PUT `/api/fhir/Patient/{id}`

Partially update an existing patient. Only fields present in the request body are updated. Returns the updated patient as a FHIR `Patient` resource.

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| id | Long | HMIS internal patient ID |

**Request Body:** FHIR R5 `Patient` JSON (`application/fhir+json`) — only include fields to update.

**Example:**

```bash
curl -X PUT \
     -H "FHIR: <your-api-key>" \
     -H "Content-Type: application/fhir+json" \
     -d '{"resourceType":"Patient","telecom":[{"system":"phone","value":"0771234567"}]}' \
     http://localhost:8080/api/fhir/Patient/12345
```

---

## Error Responses

All endpoints return JSON error objects on failure:

```json
{
  "status": "error",
  "code": 401,
  "message": "Invalid or missing FHIR API key"
}
```

Common HTTP status codes:
- `200 OK` — success
- `201 Created` — patient created (POST)
- `400 Bad Request` — invalid parameters or malformed FHIR JSON
- `401 Unauthorized` — missing or invalid API key
- `404 Not Found` — patient not found
