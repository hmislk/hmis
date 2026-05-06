# Institution, Department, and Site Management API Documentation for AI Agents

## Overview

This document provides comprehensive guidance for AI agents to interact with the Institution, Department, and Site Management API system. The API consists of four main components:

1. **Institution APIs** - Create, update, retire, and search institutions (hospitals, pharmacies, labs, etc.)
2. **Department APIs** - Manage departments within institutions (pharmacy, OPD, lab, store, etc.)
3. **Site APIs** - Manage sites (physical locations that are a special type of institution)
4. **Configuration APIs** - Manage department-specific configuration options

## Base Configuration

- **Base URL**: `http://localhost:8080` (adjust for your environment)
- **API Base Paths**:
  - `/api/institutions` (Institution management)
  - `/api/departments` (Department management)
  - `/api/sites` (Site management)
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

## Institution Types Reference

Available institution types for use in API calls:

- `Agency` - Agency
- `OnlineBookingAgent` - Channeling Online Booking Agent
- `Bank` - Bank
- `Site` - Site (physical location)
- `branch` - Branch
- `CollectingCentre` - Collecting Centre
- `Company` - Company
- `CreditCompany` - Credit Company
- `Dealer` - Dealer
- `Distributor` - Distributor
- `EducationalInstitute` - Educational Institute
- `Government` - Government
- `Hospital` - Hospital
- `Importer` - Importer
- `Lab` - Lab
- `Manufacturer` - Manufacturer
- `NonProfit` - Non-Profit
- `Other` - Other
- `Pharmacy` - Pharmacy
- `PrivatePractice` - Private Practice
- `StoreDealor` - Store Dealer
- `Wholesaler` - Wholesaler

## Department Types Reference

Available department types for use in API calls:

- `Clinical` - Clinical
- `NonClinical` - Non-Clinical
- `Pharmacy` - Pharmacy
- `Lab` - Hospital Lab
- `External_Lab` - Outsource Lab
- `Channelling` - Channelling
- `Opd` - Out Patient Department (OPD)
- `Inward` - Inward
- `Theatre` - Theatre
- `Etu` - Emergency Treatment Unit (ETU)
- `CollectingCentre` - Collecting Centre
- `Store` - Store
- `Inventry` - Inventory
- `Kitchen` - Kitchen
- `Optician` - Optician
- `Counter` - Counter
- `Cashier` - Cashier
- `Office` - Office
- `Ict` - Information and Communication Technology (ICT)
- `Other` - Other

## Search/Lookup APIs

### 1. Institution Search

**Purpose**: Find institutions by name or code with optional type filtering

**Endpoint**: `GET /api/institutions/search`

**Parameters**:
- `query` (required): Institution name or code search term
- `type` (optional): InstitutionType enum value (e.g., Hospital, Pharmacy, Lab)
- `limit` (optional): Result limit (default: 20, max: 100)

**Example Request**:
```bash
GET /api/institutions/search?query=Central%20Hospital&type=Hospital&limit=10
```

**Example Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "Central Hospital",
      "code": "CH001",
      "institutionType": "Hospital",
      "address": "123 Main Street, Colombo",
      "phone": "+94112345678",
      "email": "info@centralhospital.lk"
    },
    {
      "id": 2,
      "name": "Central District Hospital",
      "code": "CDH001",
      "institutionType": "Hospital",
      "address": "456 District Road, Kandy",
      "phone": "+94812345678",
      "email": "info@districthospital.lk"
    }
  ]
}
```

**Validation Rules**:
- `query`: Required, cannot be empty
- `type`: Optional, must be valid InstitutionType enum value
- `limit`: Optional, must be valid integer

### 2. Department Search

**Purpose**: Find departments by name or code with optional type and institution filtering

**Endpoint**: `GET /api/departments/search`

**Parameters**:
- `query` (required): Department name or code search term
- `type` (optional): DepartmentType enum value (e.g., Pharmacy, Lab, Opd)
- `institutionId` (optional): Filter by institution ID
- `limit` (optional): Result limit (default: 20, max: 100)

**Example Request**:
```bash
GET /api/departments/search?query=Pharmacy&type=Pharmacy&institutionId=1&limit=20
```

**Example Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 101,
      "name": "Main Pharmacy",
      "code": "PHARM01"
    },
    {
      "id": 102,
      "name": "Emergency Pharmacy",
      "code": "PHARM02"
    }
  ]
}
```

**Validation Rules**:
- `query`: Required, cannot be empty
- `type`: Optional, must be valid DepartmentType enum value
- `institutionId`: Optional, must be valid Long
- `limit`: Optional, must be valid integer

### 3. Site Search

**Purpose**: Find sites by name or code (sites are institutions with type Site)

**Endpoint**: `GET /api/sites/search`

**Parameters**:
- `query` (required): Site name or code search term
- `limit` (optional): Result limit (default: 20, max: 100)

**Example Request**:
```bash
GET /api/sites/search?query=Main&limit=10
```

**Example Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 501,
      "name": "Main Campus Site",
      "code": "SITE001"
    },
    {
      "id": 502,
      "name": "Main Building Site",
      "code": "SITE002"
    }
  ]
}
```

**Validation Rules**:
- `query`: Required, cannot be empty
- `limit`: Optional, must be valid integer

## CRUD APIs - Institution Management

### 1. Get Institution by ID

**Purpose**: Retrieve detailed information about a specific institution

**Endpoint**: `GET /api/institutions/{id}`

**Path Parameters**:
- `id` (required): Institution ID (Long)

**Example Request**:
```bash
GET /api/institutions/1
```

**Example Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 1,
    "name": "Central Hospital",
    "code": "CH001",
    "institutionType": "Hospital",
    "address": "123 Main Street, Colombo",
    "phone": "+94112345678",
    "mobile": "+94771234567",
    "email": "info@centralhospital.lk",
    "fax": "+94112345679",
    "web": "https://www.centralhospital.lk",
    "parentInstitutionId": null,
    "parentInstitutionName": null,
    "contactPersonName": "Dr. John Silva",
    "ownerName": "Hospital Board of Directors",
    "ownerEmail": "board@centralhospital.lk",
    "active": true,
    "createdAt": "2025-01-15 10:30:00",
    "message": "Institution retrieved successfully"
  }
}
```

**Error Responses**:
```json
{
  "status": "error",
  "code": 404,
  "message": "Institution with ID 999 not found"
}
```

### 2. Create Institution

**Purpose**: Create a new institution

**Endpoint**: `POST /api/institutions`

**Request Body**:
```json
{
  "name": "City Medical Center",
  "code": "CMC001",
  "institutionType": "Hospital",
  "address": "789 City Avenue, Galle",
  "phone": "+94912345678",
  "mobile": "+94771234568",
  "email": "info@citymedical.lk",
  "fax": "+94912345679",
  "web": "https://www.citymedical.lk",
  "parentInstitutionId": null,
  "contactPersonName": "Dr. Jane Perera",
  "ownerName": "City Medical Board",
  "ownerEmail": "board@citymedical.lk"
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 3,
    "name": "City Medical Center",
    "code": "CMC001",
    "institutionType": "Hospital",
    "address": "789 City Avenue, Galle",
    "phone": "+94912345678",
    "mobile": "+94771234568",
    "email": "info@citymedical.lk",
    "fax": "+94912345679",
    "web": "https://www.citymedical.lk",
    "parentInstitutionId": null,
    "parentInstitutionName": null,
    "contactPersonName": "Dr. Jane Perera",
    "ownerName": "City Medical Board",
    "ownerEmail": "board@citymedical.lk",
    "active": true,
    "createdAt": "2025-01-20 14:45:00",
    "message": "Institution created successfully"
  }
}
```

**Validation Rules**:
- `name`: Required, cannot be empty
- `institutionType`: Required, must be valid InstitutionType enum
- `code`: Optional - auto-generated if not provided
- All other fields: Optional

**Code Auto-Generation**:
- If `code` is null or empty, system generates code from name
- Converts to lowercase, replaces spaces with underscores
- Example: "City Medical Center" becomes "city_medical_center"

### 3. Update Institution

**Purpose**: Update existing institution details

**Endpoint**: `PUT /api/institutions/{id}`

**Path Parameters**:
- `id` (required): Institution ID to update

**Request Body**:
```json
{
  "name": "City Medical Center - Updated",
  "code": "CMC001",
  "institutionType": "Hospital",
  "address": "789 City Avenue, Galle - New Wing",
  "phone": "+94912345680",
  "mobile": "+94771234569",
  "email": "contact@citymedical.lk",
  "fax": "+94912345681",
  "web": "https://www.citymedical.lk",
  "contactPersonName": "Dr. Jane Perera Silva",
  "ownerName": "City Medical Board of Directors",
  "ownerEmail": "directors@citymedical.lk"
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 3,
    "name": "City Medical Center - Updated",
    "code": "CMC001",
    "institutionType": "Hospital",
    "address": "789 City Avenue, Galle - New Wing",
    "phone": "+94912345680",
    "mobile": "+94771234569",
    "email": "contact@citymedical.lk",
    "fax": "+94912345681",
    "web": "https://www.citymedical.lk",
    "parentInstitutionId": null,
    "parentInstitutionName": null,
    "contactPersonName": "Dr. Jane Perera Silva",
    "ownerName": "City Medical Board of Directors",
    "ownerEmail": "directors@citymedical.lk",
    "active": true,
    "createdAt": "2025-01-20 14:45:00",
    "message": "Institution updated successfully"
  }
}
```

**Validation Rules**:
- `name`: Optional, but cannot be empty if provided
- `institutionType`: Optional, must be valid InstitutionType enum if provided
- All fields are optional - only provided fields will be updated
- Cannot update parent institution using this endpoint (use relationship endpoint)

### 4. Retire Institution (Soft Delete)

**Purpose**: Retire an institution (soft delete - marks as inactive)

**Endpoint**: `DELETE /api/institutions/{id}`

**Path Parameters**:
- `id` (required): Institution ID to retire

**Query Parameters**:
- `retireComments` (optional): Reason for retiring the institution

**Example Request**:
```bash
DELETE /api/institutions/3?retireComments=Merged%20with%20another%20hospital
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 3,
    "name": "City Medical Center - Updated",
    "code": "CMC001",
    "institutionType": "Hospital",
    "address": "789 City Avenue, Galle - New Wing",
    "phone": "+94912345680",
    "mobile": "+94771234569",
    "email": "contact@citymedical.lk",
    "fax": "+94912345681",
    "web": "https://www.citymedical.lk",
    "parentInstitutionId": null,
    "parentInstitutionName": null,
    "contactPersonName": "Dr. Jane Perera Silva",
    "ownerName": "City Medical Board of Directors",
    "ownerEmail": "directors@citymedical.lk",
    "active": false,
    "createdAt": "2025-01-20 14:45:00",
    "message": "Institution retired successfully"
  }
}
```

**Important Notes**:
- This is a SOFT DELETE - institution is marked as retired, not physically deleted
- Retired institutions are excluded from search results
- Retire comments are stored in the institution's retireComments field
- Operation cannot be reversed via API (requires database administrator)

## CRUD APIs - Department Management

### 1. Get Department by ID

**Purpose**: Retrieve detailed information about a specific department

**Endpoint**: `GET /api/departments/{id}`

**Path Parameters**:
- `id` (required): Department ID (Long)

**Example Request**:
```bash
GET /api/departments/101
```

**Example Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 101,
    "name": "Main Pharmacy",
    "code": "PHARM01",
    "departmentType": "Pharmacy",
    "institutionId": 1,
    "institutionName": "Central Hospital",
    "siteId": 501,
    "siteName": "Main Campus Site",
    "description": "Central pharmacy department handling all prescriptions",
    "printingName": "Main Pharmacy - Central Hospital",
    "address": "Ground Floor, Building A",
    "telephone1": "+94112345601",
    "telephone2": "+94112345602",
    "fax": "+94112345603",
    "email": "pharmacy@centralhospital.lk",
    "superDepartmentId": null,
    "superDepartmentName": null,
    "margin": 15.5,
    "pharmacyMarginFromPurchaseRate": 20.0,
    "active": true,
    "createdAt": "2025-01-10 09:00:00",
    "message": "Department retrieved successfully"
  }
}
```

**Error Responses**:
```json
{
  "status": "error",
  "code": 404,
  "message": "Department with ID 999 not found"
}
```

### 2. Create Department

**Purpose**: Create a new department within an institution

**Endpoint**: `POST /api/departments`

**Request Body**:
```json
{
  "name": "Emergency Pharmacy",
  "code": "PHARM02",
  "departmentType": "Pharmacy",
  "institutionId": 1,
  "siteId": 501,
  "description": "24/7 emergency pharmacy services",
  "printingName": "Emergency Pharmacy - Central Hospital",
  "address": "First Floor, Emergency Wing",
  "telephone1": "+94112345610",
  "telephone2": "+94112345611",
  "fax": "+94112345612",
  "email": "emergency.pharmacy@centralhospital.lk",
  "superDepartmentId": 101,
  "margin": 15.5,
  "pharmacyMarginFromPurchaseRate": 20.0
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 102,
    "name": "Emergency Pharmacy",
    "code": "PHARM02",
    "departmentType": "Pharmacy",
    "institutionId": 1,
    "institutionName": "Central Hospital",
    "siteId": 501,
    "siteName": "Main Campus Site",
    "description": "24/7 emergency pharmacy services",
    "printingName": "Emergency Pharmacy - Central Hospital",
    "address": "First Floor, Emergency Wing",
    "telephone1": "+94112345610",
    "telephone2": "+94112345611",
    "fax": "+94112345612",
    "email": "emergency.pharmacy@centralhospital.lk",
    "superDepartmentId": 101,
    "superDepartmentName": "Main Pharmacy",
    "margin": 15.5,
    "pharmacyMarginFromPurchaseRate": 20.0,
    "active": true,
    "createdAt": "2025-01-22 11:30:00",
    "message": "Department created successfully"
  }
}
```

**Validation Rules**:
- `name`: Required, cannot be empty
- `departmentType`: Required, must be valid DepartmentType enum
- `institutionId`: Required, must be valid institution ID
- `code`: Optional - auto-generated if not provided
- `siteId`: Optional, must be valid site (institution with type Site) if provided
- `superDepartmentId`: Optional, must be valid department ID if provided
- All other fields: Optional

**Code Auto-Generation**:
- If `code` is null or empty, system generates code from name
- Converts to lowercase, replaces spaces with underscores
- Example: "Emergency Pharmacy" becomes "emergency_pharmacy"

### 3. Update Department

**Purpose**: Update existing department details

**Endpoint**: `PUT /api/departments/{id}`

**Path Parameters**:
- `id` (required): Department ID to update

**Request Body**:
```json
{
  "name": "Emergency Pharmacy - 24/7",
  "code": "PHARM02",
  "departmentType": "Pharmacy",
  "description": "Round-the-clock emergency pharmacy services",
  "printingName": "Emergency Pharmacy - Central Hospital (24/7)",
  "address": "First Floor, Emergency Wing - Extended",
  "telephone1": "+94112345615",
  "telephone2": "+94112345616",
  "fax": "+94112345617",
  "email": "emergency24@centralhospital.lk",
  "margin": 16.0,
  "pharmacyMarginFromPurchaseRate": 22.0
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 102,
    "name": "Emergency Pharmacy - 24/7",
    "code": "PHARM02",
    "departmentType": "Pharmacy",
    "institutionId": 1,
    "institutionName": "Central Hospital",
    "siteId": 501,
    "siteName": "Main Campus Site",
    "description": "Round-the-clock emergency pharmacy services",
    "printingName": "Emergency Pharmacy - Central Hospital (24/7)",
    "address": "First Floor, Emergency Wing - Extended",
    "telephone1": "+94112345615",
    "telephone2": "+94112345616",
    "fax": "+94112345617",
    "email": "emergency24@centralhospital.lk",
    "superDepartmentId": 101,
    "superDepartmentName": "Main Pharmacy",
    "margin": 16.0,
    "pharmacyMarginFromPurchaseRate": 22.0,
    "active": true,
    "createdAt": "2025-01-22 11:30:00",
    "message": "Department updated successfully"
  }
}
```

**Validation Rules**:
- All fields are optional - only provided fields will be updated
- `name`: Cannot be empty if provided
- `departmentType`: Must be valid DepartmentType enum if provided
- Cannot update relationships (institution, site, super department) using this endpoint (use relationship endpoint)

### 4. Retire Department (Soft Delete)

**Purpose**: Retire a department (soft delete - marks as inactive)

**Endpoint**: `DELETE /api/departments/{id}`

**Path Parameters**:
- `id` (required): Department ID to retire

**Query Parameters**:
- `retireComments` (optional): Reason for retiring the department

**Example Request**:
```bash
DELETE /api/departments/102?retireComments=Services%20moved%20to%20main%20pharmacy
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 102,
    "name": "Emergency Pharmacy - 24/7",
    "code": "PHARM02",
    "departmentType": "Pharmacy",
    "institutionId": 1,
    "institutionName": "Central Hospital",
    "siteId": 501,
    "siteName": "Main Campus Site",
    "description": "Round-the-clock emergency pharmacy services",
    "printingName": "Emergency Pharmacy - Central Hospital (24/7)",
    "address": "First Floor, Emergency Wing - Extended",
    "telephone1": "+94112345615",
    "telephone2": "+94112345616",
    "fax": "+94112345617",
    "email": "emergency24@centralhospital.lk",
    "superDepartmentId": 101,
    "superDepartmentName": "Main Pharmacy",
    "margin": 16.0,
    "pharmacyMarginFromPurchaseRate": 22.0,
    "active": false,
    "createdAt": "2025-01-22 11:30:00",
    "message": "Department retired successfully"
  }
}
```

**Important Notes**:
- This is a SOFT DELETE - department is marked as retired, not physically deleted
- Retired departments are excluded from search results
- Retire comments are stored in the department's retireComments field
- Operation cannot be reversed via API (requires database administrator)

## CRUD APIs - Site Management

### 1. Get Site by ID

**Purpose**: Retrieve detailed information about a specific site

**Endpoint**: `GET /api/sites/{id}`

**Path Parameters**:
- `id` (required): Site ID (Long)

**Example Request**:
```bash
GET /api/sites/501
```

**Example Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 501,
    "name": "Main Campus Site",
    "code": "SITE001",
    "address": "123 Main Street, Colombo",
    "phone": "+94112345678",
    "mobile": "+94771234567",
    "email": "mainsite@centralhospital.lk",
    "fax": "+94112345679",
    "web": "https://www.centralhospital.lk/mainsite",
    "active": true,
    "createdAt": "2025-01-05 08:00:00",
    "message": "Site retrieved successfully"
  }
}
```

**Error Responses**:
```json
{
  "status": "error",
  "code": 404,
  "message": "Site with ID 999 not found"
}
```

```json
{
  "status": "error",
  "code": 404,
  "message": "Institution with ID 123 is not a site"
}
```

### 2. Create Site

**Purpose**: Create a new site (institution with type Site)

**Endpoint**: `POST /api/sites`

**Request Body**:
```json
{
  "name": "Branch Campus Site",
  "code": "SITE002",
  "address": "456 Branch Road, Kandy",
  "phone": "+94812345678",
  "mobile": "+94771234568",
  "email": "branch@centralhospital.lk",
  "fax": "+94812345679",
  "web": "https://www.centralhospital.lk/branch"
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 502,
    "name": "Branch Campus Site",
    "code": "SITE002",
    "address": "456 Branch Road, Kandy",
    "phone": "+94812345678",
    "mobile": "+94771234568",
    "email": "branch@centralhospital.lk",
    "fax": "+94812345679",
    "web": "https://www.centralhospital.lk/branch",
    "active": true,
    "createdAt": "2025-01-23 10:15:00",
    "message": "Site created successfully"
  }
}
```

**Validation Rules**:
- `name`: Required, cannot be empty
- `code`: Optional - auto-generated if not provided
- All other fields: Optional
- InstitutionType is automatically set to `Site`

**Code Auto-Generation**:
- If `code` is null or empty, system generates code from name
- Converts to lowercase, replaces spaces with underscores
- Example: "Branch Campus Site" becomes "branch_campus_site"

### 3. Update Site

**Purpose**: Update existing site details

**Endpoint**: `PUT /api/sites/{id}`

**Path Parameters**:
- `id` (required): Site ID to update

**Request Body**:
```json
{
  "name": "Branch Campus Site - Expanded",
  "code": "SITE002",
  "address": "456 Branch Road, Kandy - New Building",
  "phone": "+94812345680",
  "mobile": "+94771234569",
  "email": "branch.expanded@centralhospital.lk",
  "fax": "+94812345681",
  "web": "https://www.centralhospital.lk/branch-expanded"
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 502,
    "name": "Branch Campus Site - Expanded",
    "code": "SITE002",
    "address": "456 Branch Road, Kandy - New Building",
    "phone": "+94812345680",
    "mobile": "+94771234569",
    "email": "branch.expanded@centralhospital.lk",
    "fax": "+94812345681",
    "web": "https://www.centralhospital.lk/branch-expanded",
    "active": true,
    "createdAt": "2025-01-23 10:15:00",
    "message": "Site updated successfully"
  }
}
```

**Validation Rules**:
- All fields are optional - only provided fields will be updated
- `name`: Cannot be empty if provided

### 4. Retire Site (Soft Delete)

**Purpose**: Retire a site (soft delete - marks as inactive)

**Endpoint**: `DELETE /api/sites/{id}`

**Path Parameters**:
- `id` (required): Site ID to retire

**Query Parameters**:
- `retireComments` (optional): Reason for retiring the site

**Example Request**:
```bash
DELETE /api/sites/502?retireComments=Relocated%20to%20main%20campus
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 502,
    "name": "Branch Campus Site - Expanded",
    "code": "SITE002",
    "address": "456 Branch Road, Kandy - New Building",
    "phone": "+94812345680",
    "mobile": "+94771234569",
    "email": "branch.expanded@centralhospital.lk",
    "fax": "+94812345681",
    "web": "https://www.centralhospital.lk/branch-expanded",
    "active": false,
    "createdAt": "2025-01-23 10:15:00",
    "message": "Site retired successfully"
  }
}
```

**Important Notes**:
- This is a SOFT DELETE - site is marked as retired, not physically deleted
- Retired sites are excluded from search results
- Retire comments are stored in the site's retireComments field
- Operation cannot be reversed via API (requires database administrator)

## Relationship Management APIs

### 1. Change Parent Institution

**Purpose**: Update the parent institution relationship for an institution (e.g., assign a branch to its parent hospital)

**Endpoint**: `PUT /api/institutions/{id}/relationship`

**Path Parameters**:
- `id` (required): Institution ID whose parent will be changed

**Request Body**:
```json
{
  "newParentInstitutionId": 1,
  "comment": "Assigning branch to parent hospital"
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 3,
    "name": "City Medical Center Branch",
    "code": "CMCB001",
    "institutionType": "branch",
    "address": "789 Branch Avenue, Galle",
    "phone": "+94912345678",
    "mobile": "+94771234568",
    "email": "branch@citymedical.lk",
    "fax": "+94912345679",
    "web": "https://www.citymedical.lk/branch",
    "parentInstitutionId": 1,
    "parentInstitutionName": "Central Hospital",
    "contactPersonName": "Dr. Jane Perera",
    "ownerName": "City Medical Board",
    "ownerEmail": "board@citymedical.lk",
    "active": true,
    "createdAt": "2025-01-20 14:45:00",
    "message": "Parent institution updated successfully"
  }
}
```

**Validation Rules**:
- `newParentInstitutionId`: Optional - set to null to remove parent relationship
- If provided, must be a valid institution ID
- Cannot set an institution as its own parent
- `comment`: Optional, stored in audit trail

**Use Cases**:
- Assign branch institutions to parent hospitals
- Create institution hierarchies
- Remove parent relationships by setting `newParentInstitutionId` to null

### 2. Change Department Relationships

**Purpose**: Update department relationships (institution, site, super department)

**Endpoint**: `PUT /api/departments/{id}/relationship`

**Path Parameters**:
- `id` (required): Department ID whose relationships will be changed

**Request Body**:
```json
{
  "newInstitutionId": 2,
  "newSiteId": 503,
  "newSuperDepartmentId": 105,
  "comment": "Restructuring department hierarchy"
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 102,
    "name": "Emergency Pharmacy",
    "code": "PHARM02",
    "departmentType": "Pharmacy",
    "institutionId": 2,
    "institutionName": "District Hospital",
    "siteId": 503,
    "siteName": "District Campus Site",
    "description": "24/7 emergency pharmacy services",
    "printingName": "Emergency Pharmacy - District Hospital",
    "address": "First Floor, Emergency Wing",
    "telephone1": "+94112345610",
    "telephone2": "+94112345611",
    "fax": "+94112345612",
    "email": "emergency.pharmacy@districthospital.lk",
    "superDepartmentId": 105,
    "superDepartmentName": "Central Pharmacy",
    "margin": 15.5,
    "pharmacyMarginFromPurchaseRate": 20.0,
    "active": true,
    "createdAt": "2025-01-22 11:30:00",
    "message": "Department relationships updated successfully"
  }
}
```

**Validation Rules**:
- All fields are optional - only provided fields will be updated
- `newInstitutionId`: Optional, must be valid institution ID if provided
- `newSiteId`: Optional, must be valid site ID (institution with type Site) if provided
- `newSuperDepartmentId`: Optional, must be valid department ID if provided, set to null to remove
- Cannot set a department as its own super department
- `comment`: Optional, stored in audit trail

**Use Cases**:
- Move department to different institution
- Assign department to a site
- Create department hierarchies (parent-child relationships)
- Remove relationships by setting IDs to null

## Configuration Management APIs

### 1. Get Department Configuration Options

**Purpose**: Retrieve all configuration options for a department

**Endpoint**: `GET /api/departments/{id}/config`

**Path Parameters**:
- `id` (required): Department ID

**Example Request**:
```bash
GET /api/departments/101/config
```

**Example Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 1001,
      "departmentId": 101,
      "departmentName": "Main Pharmacy",
      "configKey": "allow_negative_stock",
      "configValue": "false",
      "configDescription": "Allow negative stock levels"
    },
    {
      "id": 1002,
      "departmentId": 101,
      "departmentName": "Main Pharmacy",
      "configKey": "auto_approve_grn",
      "configValue": "true",
      "configDescription": "Automatically approve GRN transactions"
    },
    {
      "id": 1003,
      "departmentId": 101,
      "departmentName": "Main Pharmacy",
      "configKey": "default_payment_method",
      "configValue": "Cash",
      "configDescription": "Default payment method for sales"
    }
  ]
}
```

**Error Responses**:
```json
{
  "status": "error",
  "code": 404,
  "message": "Department with ID 999 not found"
}
```

**Common Configuration Keys**:
- `allow_negative_stock` - Allow negative stock levels (true/false)
- `auto_approve_grn` - Automatically approve GRN transactions (true/false)
- `default_payment_method` - Default payment method (Cash, Card, Credit, etc.)
- `require_batch_selection` - Require batch selection for sales (true/false)
- `enable_expiry_warning` - Enable expiry date warnings (true/false)
- `expiry_warning_days` - Days before expiry to show warning (integer)

### 2. Update Department Configuration Option

**Purpose**: Update a specific configuration option value for a department

**Endpoint**: `PUT /api/departments/{id}/config`

**Path Parameters**:
- `id` (required): Department ID

**Request Body**:
```json
{
  "configKey": "allow_negative_stock",
  "configValue": "true"
}
```

**Response**:
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "id": 1001,
    "departmentId": 101,
    "departmentName": "Main Pharmacy",
    "configKey": "allow_negative_stock",
    "configValue": "true",
    "configDescription": "Allow negative stock levels"
  }
}
```

**Validation Rules**:
- `configKey`: Required, cannot be empty
- `configValue`: Required, cannot be null
- If configKey doesn't exist for the department, a new config option is created
- If configKey exists, its value is updated

**Important Notes**:
- Configuration options are department-specific
- Creating new config keys is allowed - useful for custom configurations
- Config values are stored as strings - application handles type conversion
- Some config keys may affect critical business logic - use with caution

## Complete Workflow Examples

### Example 1: Create New Hospital with Pharmacy Department

**Scenario**: Set up a new hospital with a pharmacy department and configure it

```bash
# Step 1: Create the hospital institution
curl -X POST "http://localhost:8080/api/institutions" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "name": "Green Valley Hospital",
    "institutionType": "Hospital",
    "address": "123 Valley Road, Matara",
    "phone": "+94412345678",
    "email": "info@greenvalley.lk"
  }'

# Response: { "id": 10, "name": "Green Valley Hospital", ... }

# Step 2: Create a site for the hospital
curl -X POST "http://localhost:8080/api/sites" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "name": "Green Valley Main Campus",
    "address": "123 Valley Road, Matara",
    "phone": "+94412345678"
  }'

# Response: { "id": 510, "name": "Green Valley Main Campus", ... }

# Step 3: Create pharmacy department in the hospital
curl -X POST "http://localhost:8080/api/departments" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "name": "Main Pharmacy",
    "departmentType": "Pharmacy",
    "institutionId": 10,
    "siteId": 510,
    "description": "Primary pharmacy for outpatient services",
    "telephone1": "+94412345680",
    "email": "pharmacy@greenvalley.lk",
    "pharmacyMarginFromPurchaseRate": 25.0
  }'

# Response: { "id": 110, "name": "Main Pharmacy", ... }

# Step 4: Configure the pharmacy department
curl -X PUT "http://localhost:8080/api/departments/110/config" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "configKey": "allow_negative_stock",
    "configValue": "false"
  }'

# Step 5: Add another config option
curl -X PUT "http://localhost:8080/api/departments/110/config" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "configKey": "auto_approve_grn",
    "configValue": "true"
  }'
```

### Example 2: Create Hospital Branch and Link to Parent

**Scenario**: Create a branch hospital and link it to the parent institution

```bash
# Step 1: Search for parent hospital
curl -X GET "http://localhost:8080/api/institutions/search?query=Green%20Valley%20Hospital" \
  -H "Finance: YOUR_API_KEY"

# Response: { "data": [{ "id": 10, "name": "Green Valley Hospital", ... }] }

# Step 2: Create branch institution
curl -X POST "http://localhost:8080/api/institutions" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "name": "Green Valley Hospital - Galle Branch",
    "institutionType": "branch",
    "address": "456 Beach Road, Galle",
    "phone": "+94912345678",
    "email": "galle@greenvalley.lk",
    "parentInstitutionId": 10
  }'

# Response: { "id": 11, "name": "Green Valley Hospital - Galle Branch", "parentInstitutionId": 10, ... }

# Step 3: Create pharmacy for the branch
curl -X POST "http://localhost:8080/api/departments" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "name": "Galle Branch Pharmacy",
    "departmentType": "Pharmacy",
    "institutionId": 11,
    "description": "Pharmacy for Galle branch",
    "telephone1": "+94912345680",
    "email": "pharmacy.galle@greenvalley.lk",
    "superDepartmentId": 110
  }'

# Response: { "id": 111, "name": "Galle Branch Pharmacy", "superDepartmentId": 110, ... }
```

### Example 3: Search and Update Department Configuration

**Scenario**: Find a pharmacy department and update its configuration

```bash
# Step 1: Search for pharmacy departments
curl -X GET "http://localhost:8080/api/departments/search?query=Pharmacy&type=Pharmacy&limit=20" \
  -H "Finance: YOUR_API_KEY"

# Response: { "data": [{ "id": 110, "name": "Main Pharmacy", "code": "PHARM01" }, ...] }

# Step 2: Get department details
curl -X GET "http://localhost:8080/api/departments/110" \
  -H "Finance: YOUR_API_KEY"

# Response: Full department details

# Step 3: Get current configuration
curl -X GET "http://localhost:8080/api/departments/110/config" \
  -H "Finance: YOUR_API_KEY"

# Response: List of all config options

# Step 4: Update configuration
curl -X PUT "http://localhost:8080/api/departments/110/config" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "configKey": "expiry_warning_days",
    "configValue": "90"
  }'

# Response: { "configKey": "expiry_warning_days", "configValue": "90", ... }
```

### Example 4: Reorganize Department Structure

**Scenario**: Move a department to different institution and change its parent department

```bash
# Step 1: Search for target institution
curl -X GET "http://localhost:8080/api/institutions/search?query=District%20Hospital" \
  -H "Finance: YOUR_API_KEY"

# Response: { "data": [{ "id": 5, "name": "District Hospital", ... }] }

# Step 2: Search for new parent department
curl -X GET "http://localhost:8080/api/departments/search?query=Central%20Pharmacy&institutionId=5" \
  -H "Finance: YOUR_API_KEY"

# Response: { "data": [{ "id": 105, "name": "Central Pharmacy", ... }] }

# Step 3: Update department relationships
curl -X PUT "http://localhost:8080/api/departments/111/relationship" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "newInstitutionId": 5,
    "newSuperDepartmentId": 105,
    "comment": "Reorganizing department structure"
  }'

# Response: Updated department with new relationships
```

### Example 5: Retire Old Department and Create Replacement

**Scenario**: Retire an old department and create a new one to replace it

```bash
# Step 1: Search for department to retire
curl -X GET "http://localhost:8080/api/departments/search?query=Old%20Pharmacy" \
  -H "Finance: YOUR_API_KEY"

# Response: { "data": [{ "id": 95, "name": "Old Pharmacy Department", ... }] }

# Step 2: Get department details to copy information
curl -X GET "http://localhost:8080/api/departments/95" \
  -H "Finance: YOUR_API_KEY"

# Step 3: Create new department with updated information
curl -X POST "http://localhost:8080/api/departments" \
  -H "Content-Type: application/json" \
  -H "Finance: YOUR_API_KEY" \
  -d '{
    "name": "Modern Pharmacy Department",
    "departmentType": "Pharmacy",
    "institutionId": 1,
    "description": "Upgraded pharmacy with modern facilities",
    "telephone1": "+94112345700",
    "email": "modern.pharmacy@hospital.lk"
  }'

# Response: { "id": 120, "name": "Modern Pharmacy Department", ... }

# Step 4: Retire old department
curl -X DELETE "http://localhost:8080/api/departments/95?retireComments=Replaced%20by%20Modern%20Pharmacy%20Department" \
  -H "Finance: YOUR_API_KEY"

# Response: { "id": 95, "active": false, "message": "Department retired successfully" }
```

## AI Agent Implementation Guidelines

### 1. Error Handling

**Common Error Responses**:
```json
{
  "status": "error",
  "code": 400,
  "message": "Query parameter is required"
}
```

```json
{
  "status": "error",
  "code": 401,
  "message": "Not a valid key"
}
```

```json
{
  "status": "error",
  "code": 404,
  "message": "Institution with ID 999 not found"
}
```

**Error Codes**:
- `400`: Bad request (invalid parameters, missing required fields, validation errors)
- `401`: Unauthorized (invalid API key, expired key, inactive user)
- `404`: Not found (no results, invalid institution/department/site ID)
- `500`: Server error (database issues, system errors, unexpected exceptions)

**AI Error Recovery Strategy**:
1. **401 Errors**: Validate API key is correct and not expired
2. **400 Errors**: Check required fields, validate enum values, verify data types
3. **404 Errors**: Verify IDs exist using search endpoints before operations
4. **500 Errors**: Implement exponential backoff, log errors, notify administrators
5. **Retry Logic**: Implement for transient failures, but not for validation errors

### 2. Search Strategy

**Multi-Step Search Approach**:
1. Start with specific search terms
2. Broaden search if no results found
3. Filter results using optional parameters
4. Present multiple matches to user for selection

**Example Search Evolution**:
```bash
# Try 1: Exact name with type filter
GET /api/departments/search?query=Emergency%20Pharmacy&type=Pharmacy

# Try 2: Partial name
GET /api/departments/search?query=Emergency

# Try 3: Code-based search
GET /api/departments/search?query=PHARM02

# Try 4: Institution-specific search
GET /api/departments/search?query=Pharmacy&institutionId=1
```

### 3. Validation Before Operations

**Always Validate IDs Before Using**:
```python
def safe_create_department(institution_id, site_id, super_dept_id, dept_data):
    """Validate all IDs before creating department"""

    # Validate institution exists
    inst_result = get_institution_by_id(institution_id)
    if not inst_result["success"]:
        return {"error": f"Institution ID {institution_id} not found"}

    # Validate site if provided
    if site_id:
        site_result = get_site_by_id(site_id)
        if not site_result["success"]:
            return {"error": f"Site ID {site_id} not found"}

    # Validate super department if provided
    if super_dept_id:
        super_result = get_department_by_id(super_dept_id)
        if not super_result["success"]:
            return {"error": f"Super department ID {super_dept_id} not found"}

    # All validations passed - proceed with creation
    return create_department(dept_data)
```

### 4. Relationship Management Best Practices

**Prevent Circular References**:
```python
def validate_department_hierarchy(dept_id, new_super_dept_id):
    """Prevent circular department hierarchies"""

    # Cannot be own parent
    if dept_id == new_super_dept_id:
        return False

    # Check if new_super_dept has dept_id in its parent chain
    current = new_super_dept_id
    visited = set()

    while current:
        if current == dept_id:
            return False  # Circular reference detected

        if current in visited:
            return False  # Circular reference in parent chain

        visited.add(current)
        dept = get_department_by_id(current)
        current = dept["data"]["superDepartmentId"]

    return True
```

### 5. Configuration Management

**Type-Safe Configuration Updates**:
```python
def update_department_config_typed(dept_id, config_key, config_value, value_type):
    """Update config with type validation"""

    # Validate value type
    if value_type == "boolean":
        if config_value.lower() not in ["true", "false"]:
            return {"error": f"Invalid boolean value: {config_value}"}
        config_value = config_value.lower()

    elif value_type == "integer":
        try:
            int(config_value)
        except ValueError:
            return {"error": f"Invalid integer value: {config_value}"}

    elif value_type == "decimal":
        try:
            float(config_value)
        except ValueError:
            return {"error": f"Invalid decimal value: {config_value}"}

    # Update configuration
    return update_department_config(dept_id, config_key, config_value)
```

### 6. Batch Operations

**Efficient Bulk Processing**:
```python
def bulk_create_departments(departments_list):
    """Create multiple departments efficiently"""
    results = []

    for dept_spec in departments_list:
        print(f"Creating department: {dept_spec['name']}")

        # Validate institution first
        inst_result = get_institution_by_id(dept_spec["institutionId"])
        if not inst_result["success"]:
            results.append({
                "name": dept_spec["name"],
                "status": "failed",
                "error": f"Institution {dept_spec['institutionId']} not found"
            })
            continue

        # Create department
        result = create_department(dept_spec)

        if result["success"]:
            results.append({
                "name": dept_spec["name"],
                "status": "success",
                "id": result["data"]["id"]
            })
        else:
            results.append({
                "name": dept_spec["name"],
                "status": "failed",
                "error": result.get("error", "Unknown error")
            })

    return results
```

## Python Implementation Template

```python
import requests
import json
from datetime import datetime
from typing import Optional, Dict, List, Any

class HMISInstitutionManager:
    """
    Comprehensive manager for HMIS Institution, Department, and Site APIs
    Provides methods for all CRUD operations and relationship management
    """

    def __init__(self, base_url: str, api_key: str):
        """
        Initialize the manager with base URL and API key

        Args:
            base_url: Base URL of HMIS API (e.g., http://localhost:8080)
            api_key: Valid API key for authentication
        """
        self.base_url = base_url.rstrip('/')
        self.headers = {
            "Finance": api_key,
            "Content-Type": "application/json"
        }

    # ========== Institution Methods ==========

    def search_institutions(self, query: str, institution_type: Optional[str] = None,
                           limit: Optional[int] = None) -> Dict[str, Any]:
        """
        Search for institutions by name or code

        Args:
            query: Search term for institution name or code
            institution_type: Optional InstitutionType enum value
            limit: Optional result limit

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/institutions/search"
        params = {"query": query}

        if institution_type:
            params["type"] = institution_type
        if limit:
            params["limit"] = limit

        response = requests.get(url, headers=self.headers, params=params)
        return self._process_response(response)

    def get_institution_by_id(self, institution_id: int) -> Dict[str, Any]:
        """
        Get institution details by ID

        Args:
            institution_id: Institution ID

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/institutions/{institution_id}"
        response = requests.get(url, headers=self.headers)
        return self._process_response(response)

    def create_institution(self, institution_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Create a new institution

        Args:
            institution_data: Dictionary containing institution details
                Required: name, institutionType
                Optional: code, address, phone, mobile, email, fax, web,
                         parentInstitutionId, contactPersonName, ownerName, ownerEmail

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/institutions"
        response = requests.post(url, headers=self.headers, json=institution_data)
        return self._process_response(response)

    def update_institution(self, institution_id: int,
                          institution_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Update an existing institution

        Args:
            institution_id: Institution ID to update
            institution_data: Dictionary containing fields to update

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/institutions/{institution_id}"
        response = requests.put(url, headers=self.headers, json=institution_data)
        return self._process_response(response)

    def retire_institution(self, institution_id: int,
                          retire_comments: Optional[str] = None) -> Dict[str, Any]:
        """
        Retire an institution (soft delete)

        Args:
            institution_id: Institution ID to retire
            retire_comments: Optional reason for retiring

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/institutions/{institution_id}"
        params = {}
        if retire_comments:
            params["retireComments"] = retire_comments

        response = requests.delete(url, headers=self.headers, params=params)
        return self._process_response(response)

    def change_parent_institution(self, institution_id: int,
                                 new_parent_id: Optional[int],
                                 comment: Optional[str] = None) -> Dict[str, Any]:
        """
        Change parent institution relationship

        Args:
            institution_id: Institution ID to update
            new_parent_id: New parent institution ID (None to remove parent)
            comment: Optional comment about the change

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/institutions/{institution_id}/relationship"
        payload = {"newParentInstitutionId": new_parent_id}
        if comment:
            payload["comment"] = comment

        response = requests.put(url, headers=self.headers, json=payload)
        return self._process_response(response)

    # ========== Department Methods ==========

    def search_departments(self, query: str, department_type: Optional[str] = None,
                          institution_id: Optional[int] = None,
                          limit: Optional[int] = None) -> Dict[str, Any]:
        """
        Search for departments by name or code

        Args:
            query: Search term for department name or code
            department_type: Optional DepartmentType enum value
            institution_id: Optional institution ID filter
            limit: Optional result limit

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/departments/search"
        params = {"query": query}

        if department_type:
            params["type"] = department_type
        if institution_id:
            params["institutionId"] = institution_id
        if limit:
            params["limit"] = limit

        response = requests.get(url, headers=self.headers, params=params)
        return self._process_response(response)

    def get_department_by_id(self, department_id: int) -> Dict[str, Any]:
        """
        Get department details by ID

        Args:
            department_id: Department ID

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/departments/{department_id}"
        response = requests.get(url, headers=self.headers)
        return self._process_response(response)

    def create_department(self, department_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Create a new department

        Args:
            department_data: Dictionary containing department details
                Required: name, departmentType, institutionId
                Optional: code, siteId, description, printingName, address,
                         telephone1, telephone2, fax, email, superDepartmentId,
                         margin, pharmacyMarginFromPurchaseRate

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/departments"
        response = requests.post(url, headers=self.headers, json=department_data)
        return self._process_response(response)

    def update_department(self, department_id: int,
                         department_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Update an existing department

        Args:
            department_id: Department ID to update
            department_data: Dictionary containing fields to update

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/departments/{department_id}"
        response = requests.put(url, headers=self.headers, json=department_data)
        return self._process_response(response)

    def retire_department(self, department_id: int,
                         retire_comments: Optional[str] = None) -> Dict[str, Any]:
        """
        Retire a department (soft delete)

        Args:
            department_id: Department ID to retire
            retire_comments: Optional reason for retiring

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/departments/{department_id}"
        params = {}
        if retire_comments:
            params["retireComments"] = retire_comments

        response = requests.delete(url, headers=self.headers, params=params)
        return self._process_response(response)

    def change_department_relationships(self, department_id: int,
                                       new_institution_id: Optional[int] = None,
                                       new_site_id: Optional[int] = None,
                                       new_super_department_id: Optional[int] = None,
                                       comment: Optional[str] = None) -> Dict[str, Any]:
        """
        Change department relationships

        Args:
            department_id: Department ID to update
            new_institution_id: New institution ID (optional)
            new_site_id: New site ID (optional)
            new_super_department_id: New super department ID (optional, None to remove)
            comment: Optional comment about the change

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/departments/{department_id}/relationship"
        payload = {}

        if new_institution_id is not None:
            payload["newInstitutionId"] = new_institution_id
        if new_site_id is not None:
            payload["newSiteId"] = new_site_id
        if new_super_department_id is not None:
            payload["newSuperDepartmentId"] = new_super_department_id
        if comment:
            payload["comment"] = comment

        response = requests.put(url, headers=self.headers, json=payload)
        return self._process_response(response)

    # ========== Site Methods ==========

    def search_sites(self, query: str, limit: Optional[int] = None) -> Dict[str, Any]:
        """
        Search for sites by name or code

        Args:
            query: Search term for site name or code
            limit: Optional result limit

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/sites/search"
        params = {"query": query}

        if limit:
            params["limit"] = limit

        response = requests.get(url, headers=self.headers, params=params)
        return self._process_response(response)

    def get_site_by_id(self, site_id: int) -> Dict[str, Any]:
        """
        Get site details by ID

        Args:
            site_id: Site ID

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/sites/{site_id}"
        response = requests.get(url, headers=self.headers)
        return self._process_response(response)

    def create_site(self, site_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Create a new site

        Args:
            site_data: Dictionary containing site details
                Required: name
                Optional: code, address, phone, mobile, email, fax, web

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/sites"
        response = requests.post(url, headers=self.headers, json=site_data)
        return self._process_response(response)

    def update_site(self, site_id: int, site_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Update an existing site

        Args:
            site_id: Site ID to update
            site_data: Dictionary containing fields to update

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/sites/{site_id}"
        response = requests.put(url, headers=self.headers, json=site_data)
        return self._process_response(response)

    def retire_site(self, site_id: int,
                   retire_comments: Optional[str] = None) -> Dict[str, Any]:
        """
        Retire a site (soft delete)

        Args:
            site_id: Site ID to retire
            retire_comments: Optional reason for retiring

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/sites/{site_id}"
        params = {}
        if retire_comments:
            params["retireComments"] = retire_comments

        response = requests.delete(url, headers=self.headers, params=params)
        return self._process_response(response)

    # ========== Configuration Methods ==========

    def get_department_config(self, department_id: int) -> Dict[str, Any]:
        """
        Get all configuration options for a department

        Args:
            department_id: Department ID

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/departments/{department_id}/config"
        response = requests.get(url, headers=self.headers)
        return self._process_response(response)

    def update_department_config(self, department_id: int, config_key: str,
                                config_value: str) -> Dict[str, Any]:
        """
        Update a department configuration option

        Args:
            department_id: Department ID
            config_key: Configuration key name
            config_value: Configuration value (as string)

        Returns:
            Dictionary with success status and data/error
        """
        url = f"{self.base_url}/api/departments/{department_id}/config"
        payload = {
            "configKey": config_key,
            "configValue": config_value
        }

        response = requests.put(url, headers=self.headers, json=payload)
        return self._process_response(response)

    # ========== Helper Methods ==========

    def _process_response(self, response: requests.Response) -> Dict[str, Any]:
        """
        Process API response and return standardized result

        Args:
            response: requests.Response object

        Returns:
            Dictionary with success status and data/error
        """
        try:
            data = response.json()
            if response.status_code == 200 and data.get("status") == "success":
                return {"success": True, "data": data["data"]}
            else:
                return {
                    "success": False,
                    "error": data.get("message", "Unknown error"),
                    "code": response.status_code
                }
        except json.JSONDecodeError:
            return {
                "success": False,
                "error": f"Invalid JSON response: {response.text}",
                "code": response.status_code
            }

    # ========== High-Level Workflow Methods ==========

    def create_hospital_with_departments(self, hospital_data: Dict[str, Any],
                                        departments: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        Create a hospital and multiple departments in one workflow

        Args:
            hospital_data: Hospital institution data
            departments: List of department data dictionaries

        Returns:
            Dictionary with hospital and department creation results
        """
        # Create hospital
        hospital_result = self.create_institution(hospital_data)
        if not hospital_result["success"]:
            return {"error": f"Failed to create hospital: {hospital_result['error']}"}

        hospital = hospital_result["data"]
        hospital_id = hospital["id"]

        print(f"Created hospital: {hospital['name']} (ID: {hospital_id})")

        # Create departments
        dept_results = []
        for dept_data in departments:
            dept_data["institutionId"] = hospital_id

            dept_result = self.create_department(dept_data)
            if dept_result["success"]:
                dept = dept_result["data"]
                print(f"  Created department: {dept['name']} (ID: {dept['id']})")
                dept_results.append({
                    "success": True,
                    "name": dept["name"],
                    "id": dept["id"]
                })
            else:
                print(f"  Failed to create department: {dept_data['name']}")
                dept_results.append({
                    "success": False,
                    "name": dept_data["name"],
                    "error": dept_result.get("error")
                })

        return {
            "success": True,
            "hospital": {
                "id": hospital_id,
                "name": hospital["name"],
                "code": hospital["code"]
            },
            "departments": dept_results
        }

    def find_and_configure_department(self, search_query: str, config_options: Dict[str, str],
                                     department_type: Optional[str] = None) -> Dict[str, Any]:
        """
        Search for a department and configure it

        Args:
            search_query: Department search query
            config_options: Dictionary of config_key: config_value pairs
            department_type: Optional department type filter

        Returns:
            Dictionary with search and configuration results
        """
        # Search for department
        search_result = self.search_departments(search_query, department_type=department_type)
        if not search_result["success"] or not search_result["data"]:
            return {"error": "Department not found"}

        # Handle multiple matches
        if len(search_result["data"]) > 1:
            print(f"Found {len(search_result['data'])} departments:")
            for i, dept in enumerate(search_result["data"], 1):
                print(f"  {i}. {dept['name']} (ID: {dept['id']}, Code: {dept['code']})")

            choice = int(input("Select department number: ")) - 1
            if choice < 0 or choice >= len(search_result["data"]):
                return {"error": "Invalid selection"}

            department = search_result["data"][choice]
        else:
            department = search_result["data"][0]

        dept_id = department["id"]
        print(f"Configuring department: {department['name']} (ID: {dept_id})")

        # Apply configuration options
        config_results = []
        for config_key, config_value in config_options.items():
            config_result = self.update_department_config(dept_id, config_key, config_value)
            if config_result["success"]:
                print(f"  Set {config_key} = {config_value}")
                config_results.append({
                    "success": True,
                    "key": config_key,
                    "value": config_value
                })
            else:
                print(f"  Failed to set {config_key}: {config_result.get('error')}")
                config_results.append({
                    "success": False,
                    "key": config_key,
                    "error": config_result.get("error")
                })

        return {
            "success": True,
            "department": {
                "id": dept_id,
                "name": department["name"],
                "code": department["code"]
            },
            "configurations": config_results
        }


# ========== Example Usage ==========

if __name__ == "__main__":
    # Initialize manager
    manager = HMISInstitutionManager("http://localhost:8080", "YOUR_API_KEY")

    # Example 1: Create hospital with departments
    print("\n=== Example 1: Create Hospital with Departments ===")
    hospital_result = manager.create_hospital_with_departments(
        hospital_data={
            "name": "Sunrise Medical Center",
            "institutionType": "Hospital",
            "address": "123 Health Street, Colombo",
            "phone": "+94112223344",
            "email": "info@sunrisemedical.lk"
        },
        departments=[
            {
                "name": "Main Pharmacy",
                "departmentType": "Pharmacy",
                "description": "Primary pharmacy department",
                "pharmacyMarginFromPurchaseRate": 25.0
            },
            {
                "name": "Laboratory",
                "departmentType": "Lab",
                "description": "Diagnostic laboratory services"
            },
            {
                "name": "OPD",
                "departmentType": "Opd",
                "description": "Outpatient department"
            }
        ]
    )

    if hospital_result.get("success"):
        print(f"\nSuccessfully created hospital and {len(hospital_result['departments'])} departments")

    # Example 2: Search and configure pharmacy
    print("\n=== Example 2: Search and Configure Pharmacy ===")
    config_result = manager.find_and_configure_department(
        search_query="Pharmacy",
        department_type="Pharmacy",
        config_options={
            "allow_negative_stock": "false",
            "auto_approve_grn": "true",
            "expiry_warning_days": "90",
            "default_payment_method": "Cash"
        }
    )

    # Example 3: Create site and link department
    print("\n=== Example 3: Create Site and Link Department ===")

    # Create site
    site_result = manager.create_site({
        "name": "Sunrise Main Campus",
        "address": "123 Health Street, Colombo",
        "phone": "+94112223344"
    })

    if site_result["success"]:
        site_id = site_result["data"]["id"]
        print(f"Created site: {site_result['data']['name']} (ID: {site_id})")

        # Link pharmacy to site
        if hospital_result.get("success"):
            pharmacy_id = None
            for dept in hospital_result["departments"]:
                if "Pharmacy" in dept.get("name", ""):
                    pharmacy_id = dept["id"]
                    break

            if pharmacy_id:
                link_result = manager.change_department_relationships(
                    department_id=pharmacy_id,
                    new_site_id=site_id,
                    comment="Linking pharmacy to main campus site"
                )
                if link_result["success"]:
                    print(f"Linked pharmacy to site successfully")

    # Example 4: Search institutions by type
    print("\n=== Example 4: Search Institutions by Type ===")
    hospitals = manager.search_institutions("", institution_type="Hospital", limit=5)
    if hospitals["success"]:
        print(f"Found {len(hospitals['data'])} hospitals:")
        for hospital in hospitals["data"]:
            print(f"  - {hospital['name']} (ID: {hospital['id']})")

    # Example 5: Create branch and link to parent
    print("\n=== Example 5: Create Branch and Link to Parent ===")
    if hospital_result.get("success"):
        parent_id = hospital_result["hospital"]["id"]

        branch_result = manager.create_institution({
            "name": "Sunrise Medical Center - Kandy Branch",
            "institutionType": "branch",
            "address": "456 Branch Road, Kandy",
            "phone": "+94812223344",
            "email": "kandy@sunrisemedical.lk",
            "parentInstitutionId": parent_id
        })

        if branch_result["success"]:
            print(f"Created branch: {branch_result['data']['name']}")
            print(f"Parent: {branch_result['data']['parentInstitutionName']}")
```

## Security and Best Practices

### 1. API Key Management

**Secure Storage**:
```python
import os
from dotenv import load_dotenv

# Load API key from environment variable
load_dotenv()
API_KEY = os.getenv("HMIS_API_KEY")

if not API_KEY:
    raise ValueError("HMIS_API_KEY environment variable not set")

manager = HMISInstitutionManager("http://localhost:8080", API_KEY)
```

**Key Rotation**:
- Regularly rotate API keys
- Implement key expiry monitoring
- Handle authentication failures gracefully

### 2. Input Validation

**Validate Before Sending**:
```python
def validate_institution_data(data):
    """Validate institution data before API call"""
    errors = []

    if not data.get("name"):
        errors.append("Institution name is required")

    if not data.get("institutionType"):
        errors.append("Institution type is required")

    valid_types = [
        "Hospital", "Pharmacy", "Lab", "Site", "branch",
        "CollectingCentre", "Company", "Other"
    ]
    if data.get("institutionType") not in valid_types:
        errors.append(f"Invalid institution type: {data.get('institutionType')}")

    return errors

# Usage
institution_data = {...}
validation_errors = validate_institution_data(institution_data)
if validation_errors:
    print("Validation errors:", validation_errors)
else:
    result = manager.create_institution(institution_data)
```

### 3. Rate Limiting

**Implement Backoff Strategy**:
```python
import time
from typing import Callable

def retry_with_backoff(func: Callable, max_retries: int = 3,
                       initial_delay: float = 1.0) -> Any:
    """Retry function with exponential backoff"""
    for attempt in range(max_retries):
        try:
            result = func()
            if result.get("success"):
                return result

            # Don't retry validation errors (4xx)
            if result.get("code", 0) >= 400 and result.get("code", 0) < 500:
                return result

        except Exception as e:
            if attempt == max_retries - 1:
                raise

        # Exponential backoff
        delay = initial_delay * (2 ** attempt)
        print(f"Retry attempt {attempt + 1} after {delay}s")
        time.sleep(delay)

    return {"success": False, "error": "Max retries exceeded"}
```

### 4. Logging and Audit

**Comprehensive Logging**:
```python
import logging
from datetime import datetime

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('hmis_api.log'),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger('HMISInstitutionManager')

class HMISInstitutionManagerWithLogging(HMISInstitutionManager):
    """Extended manager with comprehensive logging"""

    def create_institution(self, institution_data):
        logger.info(f"Creating institution: {institution_data.get('name')}")
        result = super().create_institution(institution_data)

        if result["success"]:
            logger.info(f"Institution created successfully: ID {result['data']['id']}")
        else:
            logger.error(f"Institution creation failed: {result.get('error')}")

        return result
```

### 5. Transaction Safety

**Rollback on Partial Failure**:
```python
def create_hospital_with_rollback(manager, hospital_data, departments):
    """Create hospital and departments with rollback on failure"""
    created_entities = {
        "hospital_id": None,
        "department_ids": []
    }

    try:
        # Create hospital
        hospital_result = manager.create_institution(hospital_data)
        if not hospital_result["success"]:
            raise Exception(f"Hospital creation failed: {hospital_result['error']}")

        created_entities["hospital_id"] = hospital_result["data"]["id"]

        # Create departments
        for dept_data in departments:
            dept_data["institutionId"] = created_entities["hospital_id"]
            dept_result = manager.create_department(dept_data)

            if not dept_result["success"]:
                raise Exception(f"Department creation failed: {dept_result['error']}")

            created_entities["department_ids"].append(dept_result["data"]["id"])

        return {"success": True, "entities": created_entities}

    except Exception as e:
        # Rollback - retire created entities
        print(f"Error occurred: {e}. Rolling back...")

        for dept_id in created_entities["department_ids"]:
            manager.retire_department(dept_id, "Rollback due to creation error")

        if created_entities["hospital_id"]:
            manager.retire_institution(created_entities["hospital_id"],
                                      "Rollback due to creation error")

        return {"success": False, "error": str(e)}
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Authentication Errors (401)

**Problem**: "Not a valid key" error

**Solutions**:
- Verify API key is correct and not expired
- Check user account is activated and not retired
- Ensure API key expiry date is in the future
- Verify Finance header is set correctly

```python
# Debug authentication
def debug_auth(api_key):
    headers = {"Finance": api_key}
    response = requests.get(
        "http://localhost:8080/api/institutions/search?query=test",
        headers=headers
    )
    print(f"Status: {response.status_code}")
    print(f"Response: {response.text}")
```

#### 2. Invalid Institution/Department Type

**Problem**: "Invalid institution type" or "Invalid department type"

**Solution**: Use exact enum values from reference section

```python
# Valid institution types
VALID_INSTITUTION_TYPES = [
    "Hospital", "Pharmacy", "Lab", "Site", "branch",
    "CollectingCentre", "Company", "Other"
]

# Valid department types
VALID_DEPARTMENT_TYPES = [
    "Pharmacy", "Lab", "Opd", "Inward", "Theatre",
    "Etu", "Store", "Cashier", "Other"
]
```

#### 3. Circular Reference in Relationships

**Problem**: Setting relationships creates circular references

**Solution**: Validate hierarchy before updates

```python
def validate_no_circular_reference(manager, dept_id, new_super_dept_id):
    """Check for circular references in department hierarchy"""
    current = new_super_dept_id
    visited = set()

    while current:
        if current == dept_id:
            return False  # Circular reference

        if current in visited:
            return False  # Loop detected

        visited.add(current)

        dept = manager.get_department_by_id(current)
        if not dept["success"]:
            break

        current = dept["data"].get("superDepartmentId")

    return True
```

#### 4. Site Not Found (Institution is not a site)

**Problem**: Institution exists but "is not a site" error

**Solution**: Verify institution type is "Site"

```python
def verify_is_site(manager, institution_id):
    """Verify institution is a site"""
    result = manager.get_institution_by_id(institution_id)
    if not result["success"]:
        return False

    return result["data"]["institutionType"] == "Site"
```

#### 5. Department Configuration Not Taking Effect

**Problem**: Configuration updates succeed but don't affect behavior

**Solutions**:
- Verify config key spelling is correct
- Check application is reading from correct configuration
- Some configs may require application restart
- Verify config value format (true/false for booleans, numbers for integers)

```python
# Verify configuration was saved
def verify_config_update(manager, dept_id, config_key, expected_value):
    """Verify configuration was updated correctly"""
    result = manager.get_department_config(dept_id)
    if not result["success"]:
        return False

    for config in result["data"]:
        if config["configKey"] == config_key:
            return config["configValue"] == expected_value

    return False
```

## Summary

This API provides comprehensive institution, department, and site management capabilities for HMIS. Key features include:

- **CRUD Operations**: Full create, read, update, retire support for all entities
- **Search Capabilities**: Flexible search with filtering by type and other criteria
- **Relationship Management**: Manage hierarchies and relationships between entities
- **Configuration Management**: Department-specific configuration options
- **Soft Delete**: Retire entities without physical deletion
- **Audit Trail**: Track all changes with comments and timestamps

The API follows RESTful principles and returns consistent JSON responses with clear error messages. All operations require valid API key authentication and support comprehensive validation.

For AI agents, the Python implementation template provides production-ready code for all common workflows, with proper error handling, validation, and best practices built in.
