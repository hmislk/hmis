# API: Service Management

**Base URL:** `/api/services`
**Authentication:** `Finance` header (API key)
**Issue:** #19030

---

## Overview

This API manages OPD Services (`Service` DTYPE), Inward Services (`InwardService` DTYPE), their fee structures (`ItemFee`), and service categories (`ServiceCategory`). It enables AI agents to programmatically create and manage services that appear in the inpatient billing autocomplete (`inward_bill_service.xhtml`).

---

## Authentication

All endpoints require the `Finance` HTTP header containing a valid, non-expired API key.

```
Finance: <api-key>
```

---

## Endpoints

### A. Service CRUD

#### Search Services
```
GET /api/services/search
```

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | No | Name substring (case-insensitive) |
| `code` | string | No | Item code substring. Use this to make a bulk load idempotent — item code is the natural key a spreadsheet of services carries |
| `serviceType` | string | No | `OPD`, `Inward`, or omit for both |
| `categoryId` | long | No | Filter by ServiceCategory ID |
| `inactive` | boolean | No | `true` = inactive only, `false` = active only |
| `limit` | int | No | Max results (default 30, max 100) |

**Example:**
```bash
curl -H "Finance: <key>" \
  "https://host/hmis/api/services/search?query=ward&serviceType=Inward&limit=20"
```

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 101,
      "name": "Ward Procedure",
      "code": "WARDPROC",
      "printName": null,
      "fullName": null,
      "serviceType": "Inward",
      "total": 500.0,
      "totalForForeigner": 500.0,
      "inactive": false,
      "categoryId": 5,
      "categoryName": "Surgical",
      "financialCategoryId": 88,
      "financialCategoryName": "INCOME ACCOUNTS:Operation Theatre Charges",
      "inwardChargeType": "WardProcedures"
    }
  ]
}
```

Look a service up by its code before creating it:

```bash
curl -H "Finance: <key>" "https://host/hmis/api/services/search?code=SM-RH-0122&limit=5"
```

---

#### Get Service by ID
```
GET /api/services/{id}
```

Returns full service details including all associated fees.

**Example:**
```bash
curl -H "Finance: <key>" "https://host/hmis/api/services/101"
```

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 101,
    "name": "Ward Procedure",
    "code": "WARDPROC",
    "serviceType": "Inward",
    "total": 500.0,
    "totalForForeigner": 500.0,
    "inactive": false,
    "retired": false,
    "inwardChargeType": "WardProcedures",
    "categoryId": 5,
    "categoryName": "Surgical",
    "institutionId": null,
    "institutionName": null,
    "departmentId": null,
    "departmentName": null,
    "createdAt": "2026-03-01 10:00:00",
    "fees": [
      {
        "id": 201,
        "name": "Hospital Fee",
        "feeType": "OwnInstitution",
        "fee": 500.0,
        "ffee": 500.0,
        "discountAllowed": false,
        "retired": false,
        "institutionId": 1,
        "institutionName": "General Hospital",
        "forInstitutionId": null,
        "forInstitutionName": null,
        "forDepartmentId": null,
        "forDepartmentName": null,
        "forCategoryId": null,
        "forCategoryName": null
      }
    ],
    "message": "Service found successfully"
  }
}
```

---

#### Create Service
```
POST /api/services
Content-Type: application/json
```

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `serviceType` | string | **Yes** | `"OPD"` or `"Inward"` |
| `name` | string | **Yes** | Service name |
| `code` | string | No | Auto-generated from name if omitted |
| `printName` | string | No | Display name for printing |
| `fullName` | string | No | Full descriptive name |
| `categoryId` | long | No | Category ID (any Category subtype, not only `ServiceCategory`) |
| `financialCategoryId` | long | No | Financial category / income account — a `Category` whose `categoryType` is `FINANCIAL_CATEGORY`. Look one up with [`/item-categories/search`](#search-any-category) |
| `institutionId` | long | No | Institution ID |
| `departmentId` | long | No | Department ID |
| `inwardChargeType` | string | Req. for Inward | Enum value from [InwardChargeType reference](#inwardchargetype-reference) |
| `inactive` | boolean | No | Default `false` |
| `discountAllowed` | boolean | No | Default `false` |
| `userChangable` | boolean | No | Default `false` |
| `chargesVisibleForInward` | boolean | No | Default `false` |
| `marginNotAllowed` | boolean | No | Default `false` |
| `requestForQuentity` | boolean | No | Default `false` |
| `patientNotRequired` | boolean | No | Default `false` |

**Example (Inward Service):**
```bash
curl -X POST \
  -H "Finance: <key>" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceType": "Inward",
    "name": "Ward Dressing",
    "inwardChargeType": "DressingCharges",
    "categoryId": 5,
    "discountAllowed": true
  }' \
  "https://host/hmis/api/services"
```

**Example (OPD Service):**
```bash
curl -X POST \
  -H "Finance: <key>" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceType": "OPD",
    "name": "Consultation Fee",
    "categoryId": 2,
    "discountAllowed": true
  }' \
  "https://host/hmis/api/services"
```

**Response:** HTTP 201 with full `ServiceResponseDTO`.

---

#### Update Service
```
PUT /api/services/{id}
Content-Type: application/json
```

Only provided (non-null) fields are updated. Accepts the same fields as create, including
`financialCategoryId`.

**Example:**
```bash
curl -X PUT \
  -H "Finance: <key>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Updated Name", "discountAllowed": true}' \
  "https://host/hmis/api/services/101"
```

---

#### Retire Service (Permanent Soft Delete)
```
DELETE /api/services/{id}?retireComments=reason
```

Sets `retired=true`. Item is excluded from all future queries. This is **irreversible** from the UI.

```bash
curl -X DELETE \
  -H "Finance: <key>" \
  "https://host/hmis/api/services/101?retireComments=Duplicate+entry"
```

---

#### Activate Service
```
PATCH /api/services/{id}/activate
```

Sets `inactive=false`. Service becomes visible in day-to-day use.

---

#### Deactivate Service
```
PATCH /api/services/{id}/deactivate
```

Sets `inactive=true`. Service is hidden from day-to-day use but remains in the system.

---

### B. Fee Management

#### List Fees
```
GET /api/services/{id}/fees
```

Returns all non-retired `ItemFee` records for the service.

---

#### Add Fee
```
POST /api/services/{id}/fees
Content-Type: application/json
```

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | **Yes** | Fee name (e.g., "Hospital Fee") |
| `feeType` | string | **Yes** | Enum value from [FeeType reference](#feetype-reference) |
| `fee` | double | **Yes** | Fee amount for locals |
| `ffee` | double | No | Fee amount for foreigners (defaults to `fee` if 0) |
| `discountAllowed` | boolean | No | Default `false` |
| `institutionId` | long | Cond. | Required for `OwnInstitution`, `OtherInstitution`, `Referral`, `CollectingCentre` |
| `departmentId` | long | No | Department association |
| `specialityId` | long | No | Speciality (for `Staff` fee type) |
| `staffId` | long | No | Staff member (for `Staff` fee type) |

After adding a fee, the service's `total` and `totalForForeigner` are automatically recalculated.

Every fee in a response also carries `forInstitutionId`/`forInstitutionName`,
`forDepartmentId`/`forDepartmentName` and `forCategoryId`/`forCategoryName`. These are the
scoping keys: a **base** fee has all three null, a **site or collecting-centre** fee carries
`forInstitution`, and a **category-specific** fee carries `forCategory`. Note that `institutionId`
is a different field — it is who the fee is payable to, not what scopes it — so without the `for*`
keys a base fee cannot be told apart from a scoped one.

**Example:**
```bash
curl -X POST \
  -H "Finance: <key>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Hospital Fee",
    "feeType": "OwnInstitution",
    "fee": 500.0,
    "ffee": 750.0,
    "institutionId": 1,
    "discountAllowed": false
  }' \
  "https://host/hmis/api/services/101/fees"
```

**Response:** HTTP 201 with full `ServiceResponseDTO` including updated fees and totals.

---

#### Update Fee
```
PUT /api/services/{id}/fees/{feeId}
Content-Type: application/json
```

Only provided (non-null) fields are updated. Service totals are recalculated.

---

#### Remove Fee
```
DELETE /api/services/{id}/fees/{feeId}
```

Soft-deletes the fee (sets `retired=true`). Service totals are recalculated.

---

#### Recalculate Totals
```
POST /api/services/{id}/recalculate-totals
```

Recomputes the item's `total` and `totalForForeigner` by summing its current non-retired fees,
and persists them.

These two fields are denormalised. They go stale whenever fees are written outside this API
(bulk fee uploads, direct edits), and several list screens and reports read them rather than
summing fees — so a stale `0.00` reads as "this service has no charge" even though its fees are
correct. Until this endpoint existed the recalculation only ran as a side effect of a fee
create/update/delete, so the only way to repair a total was to pointlessly rewrite a fee.

Works for any `Item` subtype, not only services.

```bash
curl -X POST -H "Finance: <key>"   "https://host/hmis/api/services/101/recalculate-totals"
```

**Response:** the full `ServiceResponseDTO` with the refreshed totals.

> **Caution:** the recalculation sums *every* non-retired fee on the item, including
> site-, department- and collecting-centre-specific ones. If an item carries duplicate fee rows
> (the same charge recorded once as a base fee and once scoped to an institution), the recalculated
> total will be the sum of both. Check `forInstitution`/`forDepartment`/`forCategory` on
> `GET /api/services/{id}/fees` first.

---

### C. Category Lookup

#### Search Any Category
```
GET /api/services/item-categories/search?query=theatre&categoryType=FINANCIAL_CATEGORY&limit=20
```

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | No | Name substring |
| `categoryType` | string | No | A `CategoryType` name, e.g. `FINANCIAL_CATEGORY`, `SERVICE_CATEGORY` |
| `limit` | int | No | Max results (default 30, max 100) |

`/categories/search` (below) only sees rows whose DTYPE is `ServiceCategory`, yet a service's
`categoryId` may point at any `Category` subtype and its `financialCategoryId` is a plain
`Category` of type `FINANCIAL_CATEGORY`. Without this endpoint a caller can set a `categoryId`
it has no way to look up.

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 88,
      "name": "INCOME ACCOUNTS:Operation Theatre Charges",
      "code": "income_accounts_operation_theatre_charges",
      "categoryType": "FINANCIAL_CATEGORY",
      "retired": false
    }
  ]
}
```

An invalid `categoryType` returns `400`. Rows created before the `categoryType` column was
populated return `"categoryType": null` and are only reachable without the filter.

---

#### ServiceCategory CRUD

#### Search Categories
```
GET /api/services/categories/search?query=surgical&limit=20
```

#### Get Category by ID
```
GET /api/services/categories/{id}
```

#### Create Category
```
POST /api/services/categories
Content-Type: application/json

{"name": "Surgical Procedures", "code": "SURG", "description": "..."}
```

#### Update Category
```
PUT /api/services/categories/{id}
Content-Type: application/json

{"name": "Updated Name"}
```

#### Retire Category
```
DELETE /api/services/categories/{id}?retireComments=reason
```

---

## FeeType Reference

| Value | Description |
|-------|-------------|
| `Staff` | Staff/doctor fee |
| `Member` | Member fee |
| `Outpatient` | Outpatient fee |
| `OwnInstitution` | Hospital fee (own institution) |
| `OtherInstitution` | Outside institution fee |
| `Chemical` | Reagent/chemical fee |
| `Department` | Department fee |
| `Tax` | Tax |
| `Issue` | Issue fee |
| `Additional` | Additional fee |
| `Service` | Service fee |
| `CollectingCentre` | Collecting centre fee |
| `Referral` | Referral fee |

---

## InwardChargeType Reference

Key values (full list in `com.divudi.core.data.inward.InwardChargeType`):

| Value | Label |
|-------|-------|
| `AdmissionFee` | Admission Fee |
| `AmbulanceCharges` | Ambulance Charges |
| `CT` | CT Scan |
| `DressingCharges` | Dressing Charges |
| `ECG_EEG` | ECG/EEG |
| `Equipment` | Equipment Charges |
| `ETUCharges` | ETU Charges |
| `GeneralIssuing` | General Issuing |
| `IntensiveCareManagement` | Intensive Care Management |
| `Laboratory` | Laboratory Charges |
| `MealCharges` | Meal Charges |
| `MedicalServices` | Medical Services |
| `Medicine` | Medicine |
| `MedicinesAndSurgicalSupplies` | Medicines and Surgical Supplies |
| `NursingCharges` | Nursing Care |
| `OtherCharges` | Other Charges |
| `OperationTheatreCharges` | Operation Theatre Charges |
| `ProfessionalCharge` | Professional Charge |
| `RoomCharges` | Room Charges |
| `Scanning` | Scanning Charges |
| `TreatmentCharges` | Treatment Charges |
| `WardProcedures` | Surgical Procedures |
| `X_Ray` | X-Ray |
| `physiotherapy` | Physiotherapy Charges |
| `Echo` | Echo |
| `Nebulisation` | Nebulisation |
| `VAT` | VAT (18%) |
| `BloodTransfusioncharges` | Blood Transfusion Charges |
| `LarbourRoomCharges` | Larbour Room Charges |
| `LinenCharges` | Linen Charges |
| `Dialysis` | Dialysis |
| `ECG` | ECG |
| `EEG` | EEG |
| `Radiology` | Radiology |
| `MRIScan` | MRI Scan |
| `UltrasoundScan` | Ultrasound Scan |
| `Colonoscopy` | Colonoscopy |
| `Laparoscopy` | Laparoscopy |
| `DopplerScan` | Doppler Scan |
| `BabyWarmerUse` | Baby Warmer Charges |
| `PhototherapyPerHour` | Phototherapy/hr |
| `SpeechTherapy` | Speech Therapy |

> See `InwardChargeType.java` for the complete list of ~100+ values.

---

## AI Agent Workflow Examples

### Scenario 1: Create a new Inward service and add fees

```bash
# Step 1: Find the category
curl -H "Finance: <key>" \
  "https://host/hmis/api/services/categories/search?query=surgical"

# Step 2: Create the inward service
curl -X POST -H "Finance: <key>" -H "Content-Type: application/json" \
  -d '{"serviceType":"Inward","name":"Endoscopy Procedure","inwardChargeType":"EndoscopyCharges","categoryId":5}' \
  "https://host/hmis/api/services"
# Note the returned id, e.g. 205

# Step 3: Add hospital fee
curl -X POST -H "Finance: <key>" -H "Content-Type: application/json" \
  -d '{"name":"Hospital Fee","feeType":"OwnInstitution","fee":2500.0,"institutionId":1}' \
  "https://host/hmis/api/services/205/fees"

# Step 4: Add staff fee
curl -X POST -H "Finance: <key>" -H "Content-Type: application/json" \
  -d '{"name":"Specialist Fee","feeType":"Staff","fee":1500.0,"specialityId":3}' \
  "https://host/hmis/api/services/205/fees"
```

### Scenario 2: Deactivate an unused service

```bash
curl -X PATCH -H "Finance: <key>" \
  "https://host/hmis/api/services/101/deactivate"
```

### Scenario 3: Search and verify service appears in inward billing

```bash
# Search for the service
curl -H "Finance: <key>" \
  "https://host/hmis/api/services/search?query=endoscopy&serviceType=Inward"

# The service should have inactive=false and appear in inward_bill_service.xhtml autocomplete
```

---

## Implementation Notes

- **Single-table inheritance**: `Item` uses DTYPE column. `Service` and `InwardService` both map to the same `Item` table.
- **Retire vs Deactivate**: `retired=true` is permanent and excludes the item from all queries. `inactive=true` is temporary and hides it from daily use but the item remains searchable in management pages.
- **Fee totals**: After any fee create/update/delete, the service's `total` and `totalForForeigner` fields are automatically recalculated by summing non-retired fees.
- **billedAs / reportedAs**: On creation, these self-reference fields are set to the service itself (mirrors UI controller behavior).
- **Code auto-generation**: If `code` is not provided, it is derived from the `name` via `CommonFunctions.nameToCode()`.
