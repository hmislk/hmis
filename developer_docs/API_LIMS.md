# LIMS REST API Documentation

## Overview

The LIMS (Laboratory Information Management System) REST API provides integration endpoints for laboratory middleware, analyzers, and sample management. It consists of three resource paths:

| Resource | Path | Purpose |
|----------|------|---------|
| `Lims` | `/api/lims` | Sample barcode generation and legacy lab queries |
| `MiddlewareController` | `/api/middleware` | Analyzer middleware – test orders and results |
| `LimsMiddlewareController` | `/api/limsmw` | HL7 / analyzer message processing |

> **IMPORTANT – Write Safety**: The `/middleware/test_results`, `/limsmw/observation`, `/limsmw/sysmex`, and `/limsmw/limsProcessAnalyzerMessage` endpoints write lab results into the HMIS database. Never call these unless you have verified the correct sample ID and are responding to real analyzer output. Accidental calls may create spurious result records that affect patient care.

---

## Authentication

### Credential-in-URL (legacy `/lims` endpoints)
Username and password are embedded in the URL path. Used only by legacy barcode scanners and middleware clients.

### JSON body credentials
Some endpoints accept a `LoginRequest` JSON object:
```json
{ "username": "labuser", "password": "secret" }
```

### HTTP Basic Auth
`/limsmw/sysmex` and `/limsmw/limsProcessAnalyzerMessage` require the `Authorization` header with HTTP Basic credentials:
```
Authorization: Basic base64(username:password)
```

---

## Resource 1 – `/api/lims`

### 1.1 Login (POST body)

**POST** `/api/lims/login/mw`

Authenticate a middleware client.

**Request body:**
```json
{ "username": "labuser", "password": "secret" }
```

**Responses:**
- `200 OK` – credentials valid; body contains FHIR `OperationOutcome` with `"Logged Successfully"`
- `401 Unauthorized` – invalid credentials

---

### 1.2 Login (URL params, legacy)

**GET** `/api/lims/samples/login/{username}/{password}`

Legacy login check. Returns JSON:
```json
{ "result": "success", "error": false, "successMessage": "Successfully Logged.", "successCode": -1 }
```
or an error object with `"result": "error"`.

---

### 1.3 Get Patient Sample Barcodes from Bill

**GET** `/api/lims/samples/{billId}/{username}/{password}`

Returns barcode data for all patient samples linked to the given bill.

**Path parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `billId` | String | Bill ID or bill number |
| `username` | String | Authenticating user |
| `password` | String | Authenticating password |

**Response (200):**
```json
{
  "Barcodes": [
    {
      "barcode": "12345",
      "id": "12345",
      "name": "John Doe",
      "age": "35Y",
      "sex": "Male",
      "insid": "INS/2024/001",
      "deptid": "LAB/2024/001",
      "billDate": "04 Apr 24",
      "tests": "CBC, Urine RE"
    }
  ]
}
```

---

### 1.4 Get Patient Sample Barcodes (Enhanced)

**GET** `/api/lims/samples1/{billId}/{username}/{password}`

Same as 1.3 but includes additional input validation. Preferred over 1.3 for new integrations.

**Error response:**
```json
{ "error": true, "errorMessage": "Bill Not Found. Please reenter.", "errorCode": 1, "ErrorBillId": "12345" }
```

---

### 1.5 Get Optician PO Bill Barcodes

**GET** `/api/lims/opticianPoBillBarcodes/{billid}/{username}/{password}`

Returns item barcodes for optician purchase-order bills.

**Response (200):**
```json
{ "Barcodes": [ { ... } ] }
```

---

### 1.6 Request LIMS Response for Analyzer (Middleware Query)

**GET** `/api/lims/middleware/{machine}/{message}/{username}/{password}`

Sends a raw analyzer message to the LIMS middleware and returns the response.

**Path parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `machine` | String | Analyzer/machine identifier |
| `message` | String | Raw message string from the analyzer |
| `username` | String | Authenticating user |
| `password` | String | Authenticating password |

**Response:** Plain text or JSON depending on analyzer type.

---

## Resource 2 – `/api/middleware`

This resource is used exclusively by laboratory middleware software to push/pull test orders and results. Do **not** call write endpoints manually unless you are certain you are acting on behalf of real analyzer output.

### 2.1 Health Check

**GET** `/api/middleware`

Returns `200 OK` with plain text `"Middleware service is working"`.

---

### 2.2 Health Check (Test Path)

**GET** `/api/middleware/test`

Same as 2.1. Returns `200 OK` with `"Middleware service is working"`.

---

### 2.3 Get Test Orders for Sample Requests

**POST** `/api/middleware/test_orders_for_sample_requests`

Analyzer queries HMIS for the list of tests ordered for a given sample ID.

**Request body** (`DataBundle` JSON):
```json
{
  "middlewareSettings": {
    "analyzerDetails": { "analyzerName": "Gallery_Indiko" },
    "limsSettings": { "username": "labuser", "password": "secret" }
  },
  "queryRecords": [
    { "sampleId": "12345" }
  ]
}
```

**Response (200)** – `PatientDataBundle` JSON with patient demographics and ordered tests:
```json
{
  "patientRecord": {
    "patientId": "12345",
    "patientName": "John Doe",
    "sex": "Male",
    "address": "123 Main St",
    "phone": "0771234567",
    "referringDoctor": "Dr. Smith"
  },
  "orderRecords": [
    { "sampleId": "12345", "testNames": ["HGB", "WBC", "PLT"], "sampleType": "S" }
  ]
}
```

**Error responses:**
- `400 Bad Request` – missing or invalid `queryRecord`
- `500 Internal Server Error` – sample/patient/bill not found

---

### 2.4 Receive Patient Results

**POST** `/api/middleware/test_results`

Analyzer pushes result data into HMIS. Credentials are validated from `middlewareSettings.limsSettings`.

**Request body** (`DataBundle` JSON):
```json
{
  "middlewareSettings": {
    "analyzerDetails": { "analyzerName": "Gallery_Indiko" },
    "limsSettings": { "username": "labuser", "password": "secret" }
  },
  "resultsRecords": [
    {
      "sampleId": "12345",
      "testCode": "HGB",
      "resultValueString": "13.5",
      "resultUnits": "g/dL"
    }
  ]
}
```

Supported analyzer names (value of `analyzerName`): `BioRadD10`, `Dimension_Clinical_Chemistry_System`, `Gallery_Indiko`, `Celltac_MEK`, `BA400`, `Sysmex_XS_Series`, `MaglumiX3HL7`, `MindrayBC5150`, `IndikoPlus`, `SmartLytePlus`, `SwelabLumi`, `HumaCount5D`, `HumaLyte`, `HumaStar600`, `XL_200`, `AIA_360`.

**Response (200)**:
```json
{
  "status": "Results Plus processed with details.",
  "details": [
    { "sampleId": "12345", "testCode": "HGB", "status": "Success" }
  ]
}
```

**Error responses:**
- `400 Bad Request` – malformed data bundle
- `401 Unauthorized` – invalid credentials
- `500 Internal Server Error` – processing error

---

## Resource 3 – `/api/limsmw`

Used by analyzer middleware that communicates via HL7, Sysmex ASTM, or JSON observation formats.

### 3.1 Health Check

**GET** `/api/limsmw/test`

Returns `200 OK` with plain text `"Hello, the path is correct!"`.

---

### 3.2 Receive Observation (JSON)

**POST** `/api/limsmw/observation`

Submit a single observation/result as a JSON object. Credentials are embedded in the JSON body.

**Request body:**
```json
{
  "sampleId": "12345",
  "analyzerName": "Gallery Indiko",
  "analyzerId": "1",
  "departmentId": "5",
  "departmentAnalyzerId": "3",
  "observationValueCodingSystem": "LOCAL",
  "observationValueCode": "HGB",
  "observationUnitCodingSystem": "UCUM",
  "observationUnitCode": "g/dL",
  "observationValue": "13.5",
  "issuedDate": "2024-04-04T08:30:00",
  "username": "labuser",
  "password": "secret"
}
```

**Response (201 Created):** `"Results Added Successfully for Sample ID : 12345"`

**Error responses:**
- `400 Bad Request` – invalid sample ID or observation value
- `401 Unauthorized` – invalid credentials
- `404 Not Found` – department, machine, or sample not found
- `417 Expectation Failed` – results could not be saved

---

### 3.3 Receive Sysmex Message (ASTM)

**POST** `/api/limsmw/sysmex`

Accepts a raw Sysmex ASTM-format message. Authentication via HTTP Basic Auth header.

**Headers:**
```
Authorization: Basic base64(username:password)
Content-Type: application/json
```

**Request body:** Raw Sysmex ASTM message string (plain text wrapped in JSON).

**Response (200):**
```
#{success=true|msg=Data Added to LIMS\nPatient = ...\nBill No = ...\nInvestigation = ...}
```

**Error response:**
```
#{success=false|msg=Wrong Sample ID. Please resent results 12345}
```

---

### 3.4 Process HL7 Analyzer Message

**POST** `/api/limsmw/limsProcessAnalyzerMessage`

Processes HL7 v2 messages from analyzers. The HL7 message must be Base64-encoded in the JSON body. Authentication via HTTP Basic Auth header.

**Headers:**
```
Authorization: Basic base64(username:password)
Content-Type: application/json
```

**Request body:**
```json
{ "message": "<base64-encoded-HL7-message>" }
```

**Supported HL7 message types:**
- `OUL^R22^OUL_R22` / `OUL^R22` – Unsolicited observation result (sends ACK)
- `QBP^Q11^QBP_Q11` – Query by parameter (returns RSP K11 with test orders)
- `ASTM` – Sysmex ASTM format

**Response (200):**
```json
{ "result": "<base64-encoded-response-message>" }
```

**Error responses:**
- `400 Bad Request` – malformed request or unhandled message type
- `401 Unauthorized` – invalid credentials

---

### 3.5 Login

**POST** `/api/limsmw/login`

Authenticate a middleware client.

**Request body:**
```json
{ "username": "labuser", "password": "secret" }
```

**Responses:**
- `200 OK` – credentials valid
- `401 Unauthorized` – invalid credentials

---

## Common Error Patterns

| HTTP Status | Meaning |
|-------------|---------|
| 200 | Success |
| 201 | Result saved successfully |
| 400 | Bad request / invalid input |
| 401 | Authentication failed |
| 404 | Resource not found (sample, department, machine) |
| 417 | Expectation failed – could not save result |
| 500 | Internal server error |

---

## Integration Notes

- All three resources are registered under the JAX-RS `ApplicationConfig` with base path `/api`.
- The `/middleware` resource relies on the `Analyzer` enum for routing results; the analyzer name in the request must exactly match an enum constant (spaces replaced with `_`).
- Sample IDs used across all endpoints refer to `PatientSample.id` or `PatientSample.sampleId` (the system tries both).
- The `/limsmw/limsProcessAnalyzerMessage` endpoint requires `loggedDepartment` and `loggedInstitution` to be set (via a successful login call) before processing `QBP^Q11` queries.
