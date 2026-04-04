# Consultant Management API

## Overview

Provides REST endpoints to **create** and **update** consultant (doctor) records in HMIS.

- Base path: `/api/channel/consultant`
- Authentication: `Token` header (channel API key — **not** the `Finance` header used by other modules)
- Content-Type: `application/json`

## Authentication

```
Token: <your-channel-api-key>
```

## Endpoints

---

### POST `/api/channel/consultant` — Create a new Consultant

Creates a new `Consultant` entity and its associated `Person` record.

**Required fields:** `name`  
**Optional fields:** all others

#### Request Body

```json
{
  "name"          : "NURADH JOSEPH",
  "title"         : "Dr",
  "mobile"        : "0771234567",
  "phone"         : "",
  "fax"           : "",
  "address"       : "",
  "code"          : "NURADH JOSEPH",
  "serialNo"      : 1,
  "specialityId"  : 12,
  "institutionId" : 5,
  "registration"  : "SLMC/12345",
  "qualification" : "MBBS, MD",
  "description"   : "ONCOLOGIST"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | ✅ | Full name of the doctor (stored in Person.name) |
| `title` | string | ❌ | Title enum value. Valid values: `Dr`, `DrMiss`, `DrMs`, `DrMrs`, `Mr`, `Mrs`, `Miss`, `Ms`. Defaults to `Dr`. |
| `mobile` | string | ❌ | Mobile phone number |
| `phone` | string | ❌ | Landline phone number |
| `fax` | string | ❌ | Fax number |
| `address` | string | ❌ | Physical address |
| `code` | string | ❌ | Consultant code / short identifier |
| `serialNo` | integer | ❌ | Consultant serial/ordering number (`codeInterger`) |
| `specialityId` | long | ❌ | ID of the Speciality entity to link |
| `institutionId` | long | ❌ | ID of the Institution entity to link |
| `registration` | string | ❌ | Medical registration number (e.g. SLMC number) |
| `qualification` | string | ❌ | Academic/professional qualifications |
| `description` | string | ❌ | Free-text description, often used for specialty label |

#### Response — 201 Created

```json
{
  "code": "201",
  "message": "Created",
  "detailMessage": "Consultant created successfully",
  "data": {
    "id": 12260013,
    "name": "NURADH JOSEPH",
    "title": "Dr"
  }
}
```

#### Error Responses

| Status | Reason |
|--------|--------|
| 400 Bad Request | `name` is missing or blank |
| 401 Unauthorized | Missing, expired, or invalid Token |

#### Example — curl

```bash
curl -X POST http://localhost:8080/rh/api/channel/consultant \
  -H "Token: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "NURADH JOSEPH",
    "title": "Dr",
    "description": "ONCOLOGIST"
  }'
```

---

### PUT `/api/channel/consultant/{id}` — Update an Existing Consultant

Updates a consultant's personal details, code, speciality, or institution. Only fields present in the request body are changed (partial update).

#### Path Parameter

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | long | The internal STAFF.ID of the consultant |

#### Request Body

Same optional fields as the POST endpoint. Only include the fields you want to change.

```json
{
  "mobile"       : "0771234567",
  "specialityId" : 12,
  "description"  : "ONCOLOGIST"
}
```

#### Response — 202 Accepted

```json
{
  "code": "202",
  "message": "Accepted",
  "detailMessage": "Consultant updated successfully",
  "data": {
    "id": 12260013,
    "name": "NURADH JOSEPH",
    "title": "Dr"
  }
}
```

#### Error Responses

| Status | Reason |
|--------|--------|
| 401 Unauthorized | Missing, expired, or invalid Token |
| 404 Not Found | No active consultant exists with the given ID |

#### Example — curl

```bash
curl -X PUT http://localhost:8080/rh/api/channel/consultant/12260013 \
  -H "Token: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "mobile": "0771234567",
    "description": "ONCOLOGIST"
  }'
```

---

## Finding Speciality and Institution IDs

Before creating a consultant you may need the `specialityId` or `institutionId`.

### Search Institutions

```bash
GET /api/institutions/search?query=General&limit=10
Header: Finance: YOUR_API_KEY
```

### Search Departments / Institutions

```bash
GET /api/departments/search?query=Oncology
Header: Finance: YOUR_API_KEY
```

> **Note:** The Consultant endpoints use the `Token` header (channel key), while Institution/Department search use the `Finance` header. These may be the same key or different keys depending on your system configuration.
