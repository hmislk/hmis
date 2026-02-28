# User Management API Documentation for Developers

## Overview

This document describes the User Management REST API introduced for administrative user operations and role/privilege administration.

The API is split into two resource groups:

1. **User APIs** (`/api/users`) for user CRUD, password operations, user privilege assignment, and user department assignment.
2. **Role APIs** (`/api/user-roles`) for role CRUD and role privilege assignment.

These APIs use the same response envelope pattern used across HMIS APIs:

- Success:

```json
{
  "status": "success",
  "code": 200,
  "data": {}
}
```

- Error:

```json
{
  "status": "error",
  "code": 400,
  "message": "error details"
}
```

## Base Configuration

- **Base URL**: `http://localhost:8080` (adjust per environment)
- **API Base Paths**:
  - `/api/users`
  - `/api/user-roles`
- **Authentication**: API key in `Finance` header
- **Content Type**: `application/json`
- **Date Format**: `yyyy-MM-dd HH:mm:ss`

---

## Authentication

All endpoints require a valid API key:

```bash
-H "Finance: YOUR_API_KEY"
```

A key is considered valid only when:
- key exists;
- key has a linked web user;
- key has a non-null expiry date;
- key is not expired;
- linked user is activated and not retired.

If validation fails, API returns:
- **401** with message: `Not a valid key`

---

## Security & Privilege Model

### Read access
Any valid API key can call read/list endpoints.

### Admin-required operations
Create/update/retire and assignment operations require **Admin** privilege.

- User API admin check supports:
  - direct user privilege (`WebUserPrivilege`), or
  - role-based admin privilege (`WebUserRolePrivilege`).
- Role API admin check supports:
  - direct user privilege only.

If privilege check fails, API returns:
- **403** with message: `Insufficient privileges`

---

## Endpoints Reference

## A. User APIs (`/api/users`)

### 1) List/Search Users

**GET** `/api/users?query={query}&page={page}&size={size}`
**GET** `/api/users/search?query={query}&page={page}&size={size}` (alias)

- `query` (optional): matches user name/code/person name (case-insensitive)
- `page` (optional): default `0`
- `size` (optional): default `20`, min `1`, max `100`

**Example**
```bash
curl -X GET "http://localhost:8080/api/users?query=admin&page=0&size=20" \
  -H "Finance: YOUR_API_KEY"
```

### 2) Get User by ID

**GET** `/api/users/{id}`

Returns user details map.

### 3) Create User

**POST** `/api/users`

**Required**: `name`, `password`

**Example body**
```json
{
  "name": "api_user_01",
  "password": "StrongPassword#123",
  "code": "API001",
  "email": "api.user@hospital.lk",
  "telNo": "+94112223344",
  "personName": "API User",
  "personMobile": "+94771234567",
  "institutionId": 1,
  "siteId": 2,
  "departmentId": 10,
  "roleId": 3,
  "activated": true
}
```

**Validation notes**
- duplicate active user name is rejected (`User name already exists`)
- `name` and `password` are mandatory (`name and password are required`)

### 4) Update User

**PUT** `/api/users/{id}`

Partial updates are supported (only provided fields are updated).

### 5) Retire User (soft delete)

**DELETE** `/api/users/{id}?retireComments={reason}`

Marks user as retired and records retire metadata.

### 6) Reset User Password (Admin)

**POST** `/api/users/{id}/reset-password`

**Body**
```json
{
  "newPassword": "NewPassword#456"
}
```

Returns: `Password reset successful`.

### 7) Change Password (Self or Admin)

**POST** `/api/users/{id}/change-password`

**Body (self-service)**
```json
{
  "currentPassword": "CurrentPassword#123",
  "newPassword": "NewPassword#789"
}
```

Rules:
- User can change own password if `currentPassword` matches.
- Admin can change any user password without current password.

Returns: `Password changed successfully`.

### 8) List User Privileges

**GET** `/api/users/{id}/privileges`

Response items:
- `id`
- `privilege`
- `departmentId`

### 9) Assign User Privileges

**POST** `/api/users/{id}/privileges`

**Body**
```json
{
  "privileges": ["Admin", "PharmacyIssueBill"],
  "departmentId": 10
}
```

Notes:
- duplicates are ignored;
- invalid privilege names return 400 (enum parsing failure message);
- returns full updated privilege list.

### 10) Revoke User Privilege

**DELETE** `/api/users/{id}/privileges/{privilegeId}`

Soft-retires the specific privilege assignment.

Returns: `Privilege revoked`.

### 11) List User Departments

**GET** `/api/users/{id}/departments`

Response items:
- `id`
- `departmentId`
- `departmentName`

### 12) Assign User Departments

**POST** `/api/users/{id}/departments`

**Body**
```json
{
  "departmentIds": [10, 11, 12]
}
```

Notes:
- non-existing department IDs are skipped;
- existing active assignments are not duplicated;
- returns updated assigned department list.

---

## B. User Role APIs (`/api/user-roles`)

### 1) List Roles

**GET** `/api/user-roles`

### 2) Get Role by ID

**GET** `/api/user-roles/{id}`

### 3) Create Role

**POST** `/api/user-roles`

**Body**
```json
{
  "name": "DepartmentAdmin",
  "description": "Department-level admin operations"
}
```

### 4) Update Role

**PUT** `/api/user-roles/{id}`

**Body**
```json
{
  "name": "DepartmentAdministrator",
  "description": "Updated description"
}
```

### 5) Retire Role

**DELETE** `/api/user-roles/{id}`

### 6) List Role Privileges

**GET** `/api/user-roles/{id}/privileges`

Response items:
- `id`
- `privilege`
- `departmentId`

### 7) Assign Role Privileges

**POST** `/api/user-roles/{id}/privileges`

**Body**
```json
{
  "privileges": ["Admin", "CollectingCentreBillCreate"],
  "departmentId": 10
}
```

Notes:
- duplicates are ignored;
- invalid privilege names return 400;
- returns updated role privilege list.

---

## Request / Response DTOs

## 1) `UserUpsertRequestDTO`

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | String | Create: Yes | Login/user name |
| `password` | String | Create: Yes | Plain password; API hashes before persisting |
| `code` | String | No | Optional user code |
| `email` | String | No | Email |
| `telNo` | String | No | Telephone |
| `personName` | String | No | Person display name |
| `personMobile` | String | No | Person mobile |
| `institutionId` | Long | No | Institution link |
| `siteId` | Long | No | Site link |
| `departmentId` | Long | No | Primary department |
| `roleId` | Long | No | Role reference |
| `activated` | Boolean | No | Activation state |

## 2) `PasswordChangeRequestDTO`

| Field | Type | Required | Description |
|---|---|---|---|
| `currentPassword` | String | Required for self change | Existing password validation |
| `newPassword` | String | Yes | New password |

## 3) `PrivilegeAssignmentRequestDTO`

| Field | Type | Required | Description |
|---|---|---|---|
| `privileges` | List<String> | Yes | Privilege enum names |
| `departmentId` | Long | No | Optional department scope |

## 4) `DepartmentAssignmentRequestDTO`

| Field | Type | Required | Description |
|---|---|---|---|
| `departmentIds` | List<Long> | Yes | Department IDs to assign |

## 5) `UserRoleUpsertRequestDTO`

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | String | Create: Yes | Role name |
| `description` | String | No | Role description |

## 6) `RolePrivilegeAssignmentRequestDTO`

| Field | Type | Required | Description |
|---|---|---|---|
| `privileges` | List<String> | Yes | Privilege enum names |
| `departmentId` | Long | No | Optional department scope |

---

## Error Handling

Common API errors:

- `401 Not a valid key`
- `403 Insufficient privileges`
- `404 User not found`
- `404 Role not found`
- `404 Privilege assignment not found`
- `400 Invalid JSON format`
- `400 name and password are required`
- `400 name is required`
- `400 Request body is required`
- `400 privileges are required`
- `400 departmentIds are required`
- `400 newPassword is required`
- `400 Current password is invalid`
- `400 User name already exists`

For invalid privilege enum names, response is `400` with exception-derived message.

---

## cURL Examples for Common Workflows

### Create user

```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Finance: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "name":"integration.user",
    "password":"Secret#123",
    "personName":"Integration User",
    "departmentId":10,
    "roleId":3,
    "activated":true
  }'
```

### Reset password (admin)

```bash
curl -X POST "http://localhost:8080/api/users/123/reset-password" \
  -H "Finance: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"newPassword":"Welcome#2026"}'
```

### Assign privileges to user

```bash
curl -X POST "http://localhost:8080/api/users/123/privileges" \
  -H "Finance: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"privileges":["Admin","PharmacyIssueBill"],"departmentId":10}'
```

### Assign departments to user

```bash
curl -X POST "http://localhost:8080/api/users/123/departments" \
  -H "Finance: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"departmentIds":[10,11]}'
```

### Create role and assign privileges

```bash
curl -X POST "http://localhost:8080/api/user-roles" \
  -H "Finance: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"name":"PharmacySupervisor","description":"Role for pharmacy supervision"}'
```

```bash
curl -X POST "http://localhost:8080/api/user-roles/15/privileges" \
  -H "Finance: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"privileges":["PharmacyIssueBill","PharmacyReceiveGRN"],"departmentId":10}'
```

---

## Security Considerations

1. **Use least privilege**: Avoid assigning `Admin` unless absolutely required.
2. **Use role-based access where possible**: Prefer role privileges over many direct user privilege rows.
3. **Rotate API keys** regularly and keep expiry dates enforced.
4. **Always use HTTPS** in production deployments.
5. **Treat password endpoints as sensitive** and audit calls to reset/change password operations.
6. **Retire, don’t delete**: API intentionally performs soft retire for users/roles/privilege mappings.

---

## Related References

- `developer_docs/API_INSTITUTION_DEPARTMENT_MANAGEMENT.md`
- `developer_docs/API_PHARMACEUTICAL_MANAGEMENT.md`
- `developer_docs/API_Documentation_For_AI_Agents.md`
