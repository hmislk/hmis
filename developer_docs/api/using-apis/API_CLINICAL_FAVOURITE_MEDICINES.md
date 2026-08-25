# Clinical Favourite Medicines API Documentation for AI Agents

## Overview

This document provides comprehensive guidance for AI agents to interact with the Clinical Favourite Medicines API. This module manages prescription templates ("favourite medicines") — pre-configured medicine dosage configurations keyed by patient age, sex, and setting (indoor/outdoor).

**AI-Optimised Endpoints**: `/validate`, `/suggest`, and `/parse` are specifically designed for AI agent workflows, enabling bulk entity validation, auto-suggestion, and natural language parsing of medicine instructions.

## Base Configuration

- **Base URL**: `http://localhost:8080` (adjust for your environment)
- **API Base Path**: `/api/clinical/favourite_medicines`
- **Authentication**: API Key via `Finance` header
- **Content Type**: `application/json`
- **Date Format**: `yyyy-MM-dd HH:mm:ss`

## Authentication

All API calls require a valid API key in the request header:

```bash
-H "Finance: YOUR_API_KEY"
```

---

## Core CRUD Operations

### 1. Create Favourite Medicine

**POST** `/api/clinical/favourite_medicines`

Creates a new prescription template.

**Request Body:**
```json
{
  "itemId": 123,
  "fromYears": 2.0,
  "toYears": 12.0,
  "categoryId": 45,
  "dose": 250.0,
  "doseUnitId": 10,
  "frequencyUnitId": 11,
  "duration": 5,
  "durationUnitId": 12,
  "issue": 30.0,
  "issueUnitId": 13,
  "orderNo": 1,
  "indoor": false,
  "sex": "Any"
}
```

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 456,
    "itemId": 123,
    "itemName": "Amoxicillin 250mg Suspension",
    "fromYears": 2.0,
    "toYears": 12.0,
    "categoryName": "Antibiotic",
    "dose": 250.0,
    "doseUnitName": "mg",
    "frequencyUnitName": "8 Hourly",
    "duration": 5,
    "durationUnitName": "Days",
    "issue": 30.0,
    "issueUnitName": "ml",
    "orderNo": 1,
    "indoor": false,
    "sex": "Any"
  }
}
```

### 2. Search Favourite Medicines

**GET** `/api/clinical/favourite_medicines`

Search and list prescription templates with optional filters.

**Query Parameters:**
- `query` (optional) — Search by item name
- `itemType` (optional) — Filter by item type (e.g., `AMP`, `VMP`)
- `categoryName` (optional) — Filter by category name
- `ageYears` (optional) — Filter by a specific patient age (in years)
- `fromYears` (optional) — Filter by minimum age range
- `toYears` (optional) — Filter by maximum age range
- `sex` (optional) — Filter by sex (`Male`, `Female`, `Any`)
- `indoor` (optional) — Filter by setting (`true` for inpatient, `false` for outpatient)
- `forItemName` (optional) — Filter by the "for item" association
- `limit` (optional) — Maximum results (default 20)
- `offset` (optional) — Pagination offset
- `orderBy` (optional) — Sort field
- `orderDirection` (optional) — `ASC` or `DESC`

**Example:**
```bash
curl -X GET "http://localhost:8080/api/clinical/favourite_medicines?query=amoxicillin&ageYears=5" \
  -H "Finance: YOUR_API_KEY"
```

### 3. Get Favourite Medicine by ID

**GET** `/api/clinical/favourite_medicines/{id}`

**Example:**
```bash
curl -X GET "http://localhost:8080/api/clinical/favourite_medicines/456" \
  -H "Finance: YOUR_API_KEY"
```

### 4. Update Favourite Medicine

**PUT** `/api/clinical/favourite_medicines/{id}`

Only provided fields are updated.

**Example:**
```bash
curl -X PUT "http://localhost:8080/api/clinical/favourite_medicines/456" \
  -H "Finance: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"dose": 500.0, "duration": 7}'
```

### 5. Delete Favourite Medicine

**DELETE** `/api/clinical/favourite_medicines/{id}`

Soft-deletes the prescription template.

**Example:**
```bash
curl -X DELETE "http://localhost:8080/api/clinical/favourite_medicines/456" \
  -H "Finance: YOUR_API_KEY"
```

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 456,
    "deleted": true,
    "message": "Favourite medicine deleted successfully"
  }
}
```

---

## Entity Management Operations

These endpoints allow AI agents to resolve medicine entity references before creating or updating prescription templates.

### 6. Search VTMs (Virtual Therapeutic Moieties)

**GET** `/api/clinical/favourite_medicines/entities/vtms?query={query}&limit={limit}`

Search active ingredients by name.

**Parameters:**
- `query` (required) — Search term
- `limit` (optional) — Max results (default 20)

**Example:**
```bash
curl -X GET "http://localhost:8080/api/clinical/favourite_medicines/entities/vtms?query=amoxicillin&limit=10" \
  -H "Finance: YOUR_API_KEY"
```

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 123,
      "name": "Amoxicillin",
      "code": "amoxicillin",
      "description": "Broad-spectrum antibiotic",
      "itemType": "VTM",
      "retired": false
    }
  ]
}
```

### 7. Search VMPs (Virtual Medicinal Products)

**GET** `/api/clinical/favourite_medicines/entities/vmps?query={query}&limit={limit}`

Search generic medicine products by name.

**Example:**
```bash
curl -X GET "http://localhost:8080/api/clinical/favourite_medicines/entities/vmps?query=amoxicillin+suspension&limit=10" \
  -H "Finance: YOUR_API_KEY"
```

### 8. Search AMPs (Actual Medicinal Products)

**GET** `/api/clinical/favourite_medicines/entities/amps?query={query}&limit={limit}`

Search branded medicine products by name.

**Example:**
```bash
curl -X GET "http://localhost:8080/api/clinical/favourite_medicines/entities/amps?query=amoxil&limit=10" \
  -H "Finance: YOUR_API_KEY"
```

### 9. Search Measurement Units

**GET** `/api/clinical/favourite_medicines/entities/units?query={query}&unitType={type}&limit={limit}`

Search measurement units for dose, frequency, duration, or issue.

**Parameters:**
- `query` (required) — Search term (e.g., `mg`, `ml`, `days`)
- `unitType` (optional) — Filter by type: `DoseUnit`, `FrequencyUnit`, `DurationUnit`, `IssueUnit`, `PackUnit`
- `limit` (optional) — Max results

**Example:**
```bash
curl -X GET "http://localhost:8080/api/clinical/favourite_medicines/entities/units?query=hourly&unitType=FrequencyUnit" \
  -H "Finance: YOUR_API_KEY"
```

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 11,
      "name": "8 Hourly",
      "code": "8hourly",
      "unitType": "FrequencyUnit",
      "retired": false
    }
  ]
}
```

### 10. Search Categories

**GET** `/api/clinical/favourite_medicines/entities/categories?query={query}&limit={limit}`

Search medicine dosage form / route categories (e.g., suspension, tablet, IV).

**Example:**
```bash
curl -X GET "http://localhost:8080/api/clinical/favourite_medicines/entities/categories?query=suspension" \
  -H "Finance: YOUR_API_KEY"
```

---

## AI-Optimised Operations

These endpoints are specifically designed for AI agent workflows.

### 11. Validate Entities (AI-Optimised)

**POST** `/api/clinical/favourite_medicines/validate`

Bulk-validate a list of entity name/type pairs before creating a prescription template. Returns which entities exist, their IDs, and any that need to be created.

**Request Body:**
```json
{
  "entities": [
    {"name": "amoxicillin", "type": "VTM"},
    {"name": "suspension", "type": "CATEGORY"},
    {"name": "ml", "type": "UNIT"},
    {"name": "8 Hourly", "type": "UNIT"}
  ]
}
```

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "validated": [
      {"name": "amoxicillin", "type": "VTM", "found": true, "id": 123},
      {"name": "suspension", "type": "CATEGORY", "found": true, "id": 45},
      {"name": "ml", "type": "UNIT", "found": true, "id": 10},
      {"name": "8 Hourly", "type": "UNIT", "found": false, "id": null}
    ],
    "allFound": false,
    "missingEntities": [
      {"name": "8 Hourly", "type": "UNIT"}
    ]
  }
}
```

### 12. Suggest Similar Entities (AI-Optimised)

**POST** `/api/clinical/favourite_medicines/suggest`

Auto-suggest similar entities when an exact match is not found. Useful for fuzzy matching in AI workflows.

> **Note**: This endpoint is currently under development (`501 Not Implemented`). Check back for updates.

### 13. Parse Natural Language Instructions (AI-Optimised)

**POST** `/api/clinical/favourite_medicines/parse`

Takes a natural language medicine instruction string and returns structured medicine data suitable for creating a prescription template.

> **Note**: This endpoint is currently under development (`501 Not Implemented`). When available, it will accept strings like `"Amoxicillin 250mg suspension 5ml three times a day for 5 days for children aged 2-12"` and return a structured prescription template payload.

---

## Response Format

**Success:**
```json
{
  "status": "success",
  "code": 200,
  "data": { ... }
}
```

**Error:**
```json
{
  "status": "error",
  "code": 400,
  "message": "Description of the error"
}
```

## HTTP Status Codes

| Code | Description |
|------|-------------|
| 200  | Success |
| 400  | Bad request (validation error, missing required field) |
| 401  | Unauthorized (invalid or missing API key) |
| 404  | Not found |
| 500  | Internal server error |
| 501  | Not yet implemented |

## Favourite Medicine Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Unique identifier |
| `itemId` | Long | ID of the medicine item |
| `itemName` | String | Name of the medicine |
| `itemType` | String | Type: `VTM`, `VMP`, `AMP`, etc. |
| `fromYears` | Double | Minimum patient age (years) |
| `toYears` | Double | Maximum patient age (years) |
| `categoryId` | Long | Dosage form category ID |
| `categoryName` | String | Dosage form category name |
| `dose` | Double | Dose amount |
| `doseUnitId` | Long | ID of the dose unit |
| `doseUnitName` | String | Dose unit name (e.g., `mg`, `ml`) |
| `frequencyUnitId` | Long | ID of the frequency unit |
| `frequencyUnitName` | String | Frequency (e.g., `8 Hourly`, `Twice Daily`) |
| `duration` | Integer | Duration count |
| `durationUnitId` | Long | ID of the duration unit |
| `durationUnitName` | String | Duration unit (e.g., `Days`, `Weeks`) |
| `issue` | Double | Total issue quantity |
| `issueUnitId` | Long | ID of the issue unit |
| `issueUnitName` | String | Issue unit (e.g., `ml`, `Tablets`) |
| `orderNo` | Integer | Display ordering within a template group |
| `indoor` | Boolean | `true` = inpatient, `false` = outpatient |
| `sex` | String | `Male`, `Female`, or `Any` |
| `createdBy` | String | Name of the user who created the template |
| `createdAt` | String | Creation timestamp |

## AI Agent Workflow Example

```python
import requests

BASE_URL = "http://localhost:8080/api"
HEADERS = {
    "Finance": "YOUR_API_KEY",
    "Content-Type": "application/json"
}

# Step 1: Validate all entities exist
validation = requests.post(
    f"{BASE_URL}/clinical/favourite_medicines/validate",
    json={
        "entities": [
            {"name": "amoxicillin", "type": "VTM"},
            {"name": "suspension", "type": "CATEGORY"},
            {"name": "ml", "type": "UNIT"},
            {"name": "8 Hourly", "type": "UNIT"}
        ]
    },
    headers=HEADERS
).json()["data"]

if not validation["allFound"]:
    print("Missing entities:", validation["missingEntities"])
    # Search for alternatives or create missing entities
else:
    # Step 2: Build the IDs map
    id_map = {e["name"]: e["id"] for e in validation["validated"]}

    # Step 3: Create the favourite medicine template
    response = requests.post(
        f"{BASE_URL}/clinical/favourite_medicines",
        json={
            "itemId": id_map["amoxicillin"],
            "categoryId": id_map["suspension"],
            "doseUnitId": id_map["ml"],
            "frequencyUnitId": id_map["8 Hourly"],
            "dose": 5.0,
            "duration": 5,
            "issue": 75.0,
            "fromYears": 2.0,
            "toYears": 12.0,
            "indoor": False,
            "sex": "Any"
        },
        headers=HEADERS
    )
    print("Created:", response.json()["data"])
```

## Related APIs

- **Pharmaceutical Items API** — `/api/pharmaceutical_items` — Manage VTM, VMP, AMP master data. See `API_PHARMACEUTICAL_MANAGEMENT.md`
- **Pharmaceutical Config API** — `/api/pharmaceutical_config` — Manage dosage forms, categories, and units. See `API_PHARMACEUTICAL_MANAGEMENT.md`

## Source Files

- REST Controller: `src/main/java/com/divudi/ws/clinical/FavouriteMedicineApi.java`
- Service: `src/main/java/com/divudi/service/clinical/FavouriteMedicineApiService.java`
- DTOs: `src/main/java/com/divudi/core/data/dto/clinical/`
