# Pharmaceutical Management API Documentation for AI Agents

## Overview

This document provides comprehensive guidance for AI agents to interact with the Pharmaceutical Management API system. The API consists of two main components:

1. **Pharmaceutical Item APIs** - Create, update, retire/unretire, activate/deactivate, and search pharmaceutical items (VTM, ATM, VMP, AMP, VMPP, AMPP)
2. **Pharmaceutical Config APIs** - Manage pharmaceutical configuration entities (categories, dosage forms, measurement units)

## Base Configuration

- **Base URL**: `http://localhost:8080` (adjust for your environment)
- **API Base Paths**:
  - `/api/pharmaceutical_items` (Pharmaceutical item management)
  - `/api/pharmaceutical_config` (Pharmaceutical config management)
- **Authentication**: API Key via `Finance` header
- **Content Type**: `application/json`
- **Date Format**: `yyyy-MM-dd HH:mm:ss` for responses

## Authentication

All API calls require a valid API key in the request header:

```bash
-H "Finance: YOUR_API_KEY"
```

The API key must:
- Belong to an active, non-retired WebUser
- Have a valid expiry date (not null and not in the past)
- Be properly activated

## Pharmaceutical Item Types

The dm+d (dictionary of medicines and devices) hierarchy:

| Type | Path Segment | Description |
|------|-------------|-------------|
| VTM | `vtm` | Virtual Therapeutic Moiety - active ingredient |
| ATM | `atm` | Anatomical Therapeutic Material - classified active ingredient |
| VMP | `vmp` | Virtual Medicinal Product - generic product |
| AMP | `amp` | Actual Medicinal Product - branded product |
| VMPP | `vmpp` | Virtual Medicinal Product Pack - generic pack |
| AMPP | `ampp` | Actual Medicinal Product Package - branded pack |

## Department Types Reference

Available department types for filtering pharmaceutical items:

- `Pharmacy` - Pharmacy
- `Store` - Store
- `Lab` - Hospital Lab
- `Clinical` - Clinical
- `Inventry` - Inventory
- (All other DepartmentType enum values are valid)

---

## Pharmaceutical Item API Endpoints

### 1. Search Items

**GET** `/api/pharmaceutical_items/{type}/search?query={query}&departmentType={deptType}&limit={limit}`

Search pharmaceutical items by name or code.

**Parameters:**
- `{type}` (path) - Item type: `vtm`, `atm`, `vmp`, `amp`, `vmpp`, `ampp`
- `query` (required) - Search term (matches name or code, case-insensitive)
- `departmentType` (optional) - Filter by DepartmentType enum value
- `limit` (optional) - Max results (1-100, default 20)

**Example - Search VTMs:**
```bash
curl -X GET "http://localhost:8080/api/pharmaceutical_items/vtm/search?query=para&limit=10" \
  -H "Finance: YOUR_API_KEY"
```

**Example - Search AMPs with department filter:**
```bash
curl -X GET "http://localhost:8080/api/pharmaceutical_items/amp/search?query=amox&departmentType=Pharmacy&limit=20" \
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
      "name": "Paracetamol",
      "code": "paracetamol",
      "descreption": "Analgesic and antipyretic",
      "instructions": "Take with water",
      "retired": false,
      "inactive": false
    }
  ]
}
```

### 2. Get Item by ID

**GET** `/api/pharmaceutical_items/{type}/{id}`

**Example:**
```bash
curl -X GET "http://localhost:8080/api/pharmaceutical_items/amp/123" \
  -H "Finance: YOUR_API_KEY"
```

### 3. Create Item

**POST** `/api/pharmaceutical_items/{type}`

**VTM Request Body:**
```json
{
  "name": "Paracetamol",
  "code": "paracetamol",
  "descreption": "Analgesic and antipyretic",
  "departmentType": "Pharmacy",
  "instructions": "Take with food or water"
}
```

**ATM Request Body:**
```json
{
  "name": "Paracetamol 500mg",
  "code": "paracetamol_500",
  "descreption": "Paracetamol 500mg tablet",
  "departmentType": "Pharmacy",
  "vtmId": 123
}
```

**VMP Request Body:**
```json
{
  "name": "Paracetamol 500mg Tablet",
  "code": "paracetamol_500_tab",
  "descreption": "Generic paracetamol tablet",
  "departmentType": "Pharmacy",
  "vtmId": 123,
  "dosageFormId": 456
}
```

**AMP Request Body:**
```json
{
  "name": "Panadol 500mg Tablet",
  "code": "panadol_500",
  "descreption": "Branded paracetamol",
  "departmentType": "Pharmacy",
  "vmpId": 789,
  "atmId": 101,
  "categoryId": 202,
  "barcode": "1234567890123",
  "discountAllowed": true,
  "allowFractions": false,
  "consumptionAllowed": true,
  "refundsAllowed": true
}
```

**VMPP Request Body:**
```json
{
  "name": "Paracetamol 500mg Tablet x 100",
  "code": "paracetamol_500_tab_100",
  "departmentType": "Pharmacy",
  "vmpId": 789,
  "dblValue": 100.0,
  "packUnitId": 303
}
```

**AMPP Request Body:**
```json
{
  "name": "Panadol 500mg Tablet x 100",
  "code": "panadol_500_tab_100",
  "departmentType": "Pharmacy",
  "ampId": 404,
  "dblValue": 100.0,
  "packUnitId": 303
}
```

**Example - Create VTM:**
```bash
curl -X POST "http://localhost:8080/api/pharmaceutical_items/vtm" \
  -H "Finance: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"name": "Paracetamol", "descreption": "Analgesic", "departmentType": "Pharmacy"}'
```

### 4. Update Item

**PUT** `/api/pharmaceutical_items/{type}/{id}`

Only provided fields are updated (null fields are ignored).

**Example:**
```bash
curl -X PUT "http://localhost:8080/api/pharmaceutical_items/vtm/123" \
  -H "Finance: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"name": "Paracetamol Updated", "instructions": "Updated instructions"}'
```

### 5. Retire Item (Soft Delete)

**DELETE** `/api/pharmaceutical_items/{type}/{id}?retireComments={reason}`

**Example:**
```bash
curl -X DELETE "http://localhost:8080/api/pharmaceutical_items/vtm/123?retireComments=Duplicate%20entry" \
  -H "Finance: YOUR_API_KEY"
```

### 6. Restore Item (Unretire)

**PUT** `/api/pharmaceutical_items/{type}/{id}/restore`

**Example:**
```bash
curl -X PUT "http://localhost:8080/api/pharmaceutical_items/vtm/123/restore" \
  -H "Finance: YOUR_API_KEY"
```

### 7. Activate/Deactivate Item

**PUT** `/api/pharmaceutical_items/{type}/{id}/status?active={true|false}`

**Example - Deactivate:**
```bash
curl -X PUT "http://localhost:8080/api/pharmaceutical_items/amp/123/status?active=false" \
  -H "Finance: YOUR_API_KEY"
```

**Example - Activate:**
```bash
curl -X PUT "http://localhost:8080/api/pharmaceutical_items/amp/123/status?active=true" \
  -H "Finance: YOUR_API_KEY"
```

---

## Pharmaceutical Config API Endpoints

### Config Types

| Type | Path Segment | Description |
|------|-------------|-------------|
| Categories | `categories` | PharmaceuticalItemCategory - item categorization |
| Dosage Forms | `dosage_forms` | DosageForm - tablet, capsule, liquid, etc. |
| Units | `units` | MeasurementUnit - mg, ml, tablet, strip, etc. |

### 1. Search Configs

**GET** `/api/pharmaceutical_config/{type}/search?query={query}&limit={limit}`

**Example:**
```bash
curl -X GET "http://localhost:8080/api/pharmaceutical_config/dosage_forms/search?query=tab" \
  -H "Finance: YOUR_API_KEY"
```

### 2. Get Config by ID

**GET** `/api/pharmaceutical_config/{type}/{id}`

### 3. Create Config

**POST** `/api/pharmaceutical_config/{type}`

**Category / Dosage Form Request:**
```json
{
  "name": "Tablet",
  "code": "tablet",
  "description": "Solid oral dosage form"
}
```

**Measurement Unit Request (includes unit flags):**
```json
{
  "name": "Milligram",
  "code": "mg",
  "description": "Metric unit of mass",
  "strengthUnit": true,
  "packUnit": false,
  "issueUnit": false,
  "durationUnit": false,
  "frequencyUnit": false,
  "durationInHours": null,
  "frequencyInHours": null
}
```

### 4. Update Config

**PUT** `/api/pharmaceutical_config/{type}/{id}`

### 5. Retire Config

**DELETE** `/api/pharmaceutical_config/{type}/{id}?retireComments={reason}`

### 6. Restore Config

**PUT** `/api/pharmaceutical_config/{type}/{id}/restore`

---

## Response Format

All responses follow this standard format:

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
| 200 | Success |
| 400 | Bad request (validation error, invalid type, missing required field) |
| 401 | Unauthorized (invalid or missing API key) |
| 404 | Not found |
| 409 | Conflict (already retired / not retired) |
| 500 | Internal server error |

## Response DTO Fields by Type

### VTM Response
- `id`, `name`, `code`, `descreption`, `instructions`, `retired`, `inactive`

### ATM Response
- `id`, `name`, `code`, `descreption`, `retired`, `inactive`

### VMP Response
- `id`, `name`, `code`, `descreption`, `retired`, `inactive`, `vtmId`, `vtmName`, `dosageFormId`, `dosageFormName`

### AMP Response
- `id`, `name`, `code`, `barcode`, `inactive`, `vmpId`, `vmpName`, `categoryId`, `categoryName`, `dosageFormId`, `dosageFormName`

### VMPP Response
- `id`, `name`, `code`, `retired`, `inactive`, `vmpId`, `vmpName`

### AMPP Response
- `id`, `name`, `code`, `retired`, `inactive`, `dblValue`, `packUnitName`, `ampId`, `ampName`

### Category / Dosage Form Response
- `id`, `name`, `code`, `description`, `retired`

### Measurement Unit Response
- `id`, `name`, `code`, `description`, `retired`, `strengthUnit`, `packUnit`, `issueUnit`, `durationUnit`, `frequencyUnit`, `durationInHours`, `frequencyInHours`

## Retired vs Inactive

- **Retired** (`retired` field): Soft delete. Retired items are excluded from search results. Use DELETE to retire, PUT `/restore` to unretire.
- **Inactive** (`inactive` field): Temporary deactivation. Inactive items still appear in search but are marked as inactive. Use PUT `/status?active=false` to deactivate, `?active=true` to reactivate.

## Python Example for AI Agents

```python
import requests

BASE_URL = "http://localhost:8080/api"
HEADERS = {
    "Finance": "YOUR_API_KEY",
    "Content-Type": "application/json"
}

# Search for VTMs
response = requests.get(
    f"{BASE_URL}/pharmaceutical_items/vtm/search",
    params={"query": "paracetamol", "departmentType": "Pharmacy"},
    headers=HEADERS
)
vtms = response.json()["data"]

# Create a new AMP
amp_data = {
    "name": "Panadol 500mg Tablet",
    "departmentType": "Pharmacy",
    "vmpId": vtms[0]["id"] if vtms else None,
    "discountAllowed": True,
    "consumptionAllowed": True
}
response = requests.post(
    f"{BASE_URL}/pharmaceutical_items/amp",
    json=amp_data,
    headers=HEADERS
)
new_amp = response.json()["data"]

# Deactivate the AMP
requests.put(
    f"{BASE_URL}/pharmaceutical_items/amp/{new_amp['id']}/status",
    params={"active": "false"},
    headers=HEADERS
)

# Retire the AMP
requests.delete(
    f"{BASE_URL}/pharmaceutical_items/amp/{new_amp['id']}",
    params={"retireComments": "No longer needed"},
    headers=HEADERS
)

# Restore the AMP
requests.put(
    f"{BASE_URL}/pharmaceutical_items/amp/{new_amp['id']}/restore",
    headers=HEADERS
)
```

## Related APIs

- **Institution Management API** - `/api/institutions` - See `API_INSTITUTION_DEPARTMENT_MANAGEMENT.md`
- **Pharmacy Stock APIs** - `/api/pharmacy_search`, `/api/pharmacy_adjustments`, `/api/pharmacy_batches`

## Source Files

- REST Controllers: `src/main/java/com/divudi/ws/pharmacy/PharmaceuticalItemApi.java`, `PharmaceuticalConfigApi.java`
- Services: `src/main/java/com/divudi/service/pharmacy/PharmaceuticalItemApiService.java`, `PharmaceuticalConfigApiService.java`
- Request DTOs: `src/main/java/com/divudi/core/data/dto/pharmaceutical/`
- Response DTOs: `src/main/java/com/divudi/core/data/dto/` (VtmDto, AtmDto, VmpDto, AmpDto, VmppDto, AmppDto)
