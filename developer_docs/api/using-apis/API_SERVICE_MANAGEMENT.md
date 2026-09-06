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
      "inwardChargeType": "WardProcedures"
    }
  ]
}
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
        "institutionName": "General Hospital"
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
| `categoryId` | long | No | ServiceCategory ID |
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

Only provided (non-null) fields are updated.

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

### C. Service Category CRUD

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
