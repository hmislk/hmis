# User Management API

## Overview

The User Management API provides REST endpoints for administering HMIS users, roles, privileges, department assignments, and login history. It is the preferred way to perform user admin operations — direct database manipulation should be avoided.

The API is split into three resource groups:

| Group | Base Path | Purpose |
|---|---|---|
| User API | `/api/users` | User CRUD, passwords, privilege assignment, department assignment |
| Role API | `/api/user-roles` | Role CRUD and role privilege assignment |
| Login History API | `/api/logins` | Query login history by user, department, or date range |

All endpoints use the standard HMIS response envelope:

```json
{ "status": "success", "code": 200, "data": {} }
{ "status": "error",   "code": 400, "message": "error details" }
```

---

## Base Configuration

| Setting | Value |
|---|---|
| Base URL | `http://<host>/<context>` (e.g. `http://localhost:9090/rh`) |
| Authentication header | `Finance: <api_key>` |
| Content-Type | `application/json` |
| Date format | `yyyy-MM-dd HH:mm:ss` |

---

## Authentication

All endpoints require a valid API key passed in the `Finance` header. A key is valid when:
- It exists and is not retired
- It is linked to an activated, non-retired web user
- Its expiry date is in the future

Failed auth returns **401** `Not a valid key`.

---

## Admin Privilege Requirement

Write operations (create, update, retire, assign) require the calling user to hold the `Admin` privilege. The User API checks both direct user privileges and role-based privileges. The Role API checks direct user privileges only.

Failed privilege check returns **403** `Insufficient privileges`.

---

## A. User API (`/api/users`)

### 1. List / Search Users

```
GET /api/users?query={text}&departmentId={id}&page={n}&size={n}
GET /api/users/search?query={text}&departmentId={id}&page={n}&size={n}
```

| Parameter | Type | Default | Description |
|---|---|---|---|
| `query` | String | — | Case-insensitive match on user name, code, or person name |
| `departmentId` | Long | — | Filter to users assigned to this loggable department |
| `page` | Int | `0` | Zero-based page number |
| `size` | Int | `20` | Page size, min 1, max 100 |

When `departmentId` is supplied, results are restricted to users who have that department in their `WebUserDepartment` (loggable department) list. `query` can be combined with `departmentId`.

**Example — all users in OPD:**
```bash
curl "http://localhost:9090/rh/api/users?departmentId=481" \
  -H "Finance: YOUR_API_KEY"
```

**Example — search by name within a department:**
```bash
curl "http://localhost:9090/rh/api/users?query=tharindi&departmentId=481" \
  -H "Finance: YOUR_API_KEY"
```

**Response items:** `id`, `name`, `userName`, `code`, `staffNameWithTitle`

---

### 2. Get User by ID

```
GET /api/users/{id}
```

**Response fields:** `id`, `name`, `code`, `email`, `telNo`, `activated`, `retired`, `institutionId`, `departmentId`, `siteId`, `roleId`, `personName`

---

### 3. Create User

```
POST /api/users
```

**Required fields:** `name`, `password`

```json
{
  "name": "api_user_01",
  "password": "StrongPassword#123",
  "code": "API001",
  "email": "user@hospital.lk",
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

Rejects duplicate active user names with `400 User name already exists`.

---

### 4. Update User

```
PUT /api/users/{id}
```

Partial update — only fields present in the body are changed.

---

### 5. Retire User

```
DELETE /api/users/{id}?retireComments={reason}
```

Soft-retires the user (sets `retired=true`). Irreversible via API.

---

### 6. Reset Password (Admin)

```
POST /api/users/{id}/reset-password
```

```json
{ "newPassword": "NewPassword#456" }
```

Sets `needToResetPassword=true` so the user is prompted to change on next login.

---

### 7. Change Own Password

```
POST /api/users/{id}/change-password
```

```json
{
  "currentPassword": "CurrentPassword#123",
  "newPassword": "NewPassword#789"
}
```

- Users changing their own password must supply `currentPassword`.
- Admins can change any user's password without `currentPassword`.

---

### 8. List Available Privilege Names

```
GET /api/users/privileges/available
```

Returns the full list of valid privilege enum names from `Privileges.java`. Use this to discover valid values before calling assign endpoints.

```bash
curl "http://localhost:9090/rh/api/users/privileges/available" \
  -H "Finance: YOUR_API_KEY"
```

**Response:** `["Admin", "Opd", "Inward", "ClinicalPatientEdit", ...]`

---

### 9. List User's Privileges

```
GET /api/users/{id}/privileges
```

**Response items:** `id`, `privilege`, `departmentId`

---

### 10. Assign Privileges to a Single User

```
POST /api/users/{id}/privileges
```

```json
{
  "privileges": ["ClinicalPatientEdit", "OpdEditPatientDetails"],
  "departmentId": 481
}
```

- `departmentId` is required (scopes the privilege to a department).
- Existing active assignments are skipped (no duplicates).
- Invalid privilege names return **400**.
- Returns the full updated privilege list for the user.

---

### 11. Bulk Assign Privileges to Multiple Users

```
POST /api/users/bulk-privileges
```

Assigns a set of privileges to multiple users in a single call. This is the recommended approach for onboarding a department or rolling out a new privilege set.

```json
{
  "userIds": [20701, 547569, 44338],
  "privileges": ["ClinicalPatientEdit", "OpdEditPatientDetails", "ClinicalPatientNameChange"],
  "departmentId": 481
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `userIds` | List\<Long\> | Yes | Users to receive the privileges |
| `privileges` | List\<String\> | Yes | Privilege enum names to assign |
| `departmentId` | Long | No | If supplied, assigns to this department only. If omitted, assigns across **each user's own loggable departments** |

**Behaviour when `departmentId` is omitted:** for each user, the API iterates all their active `WebUserDepartment` entries and assigns the privileges to each one. Existing assignments are silently skipped.

**Response:** object with `processed` (per-user summary) and `skippedUsers` (IDs that could not be processed):

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "processed": [
      { "userId": 20701, "userName": "tharindi", "privilegesAdded": 13, "privilegesSkipped": 1 },
      { "userId": 547569, "userName": "binosha",  "privilegesAdded": 14, "privilegesSkipped": 0 }
    ],
    "skippedUsers": [
      { "userId": 99999, "reason": "not_found" },
      { "userId": 88888, "reason": "retired" }
    ]
  }
}
```

**Example — grant patient editing to all users in a department across their own departments:**
```bash
curl -X POST "http://localhost:9090/rh/api/users/bulk-privileges" \
  -H "Finance: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "userIds": [20701, 547569, 44338],
    "privileges": [
      "ClinicalPatientEdit", "ClinicalPatientAdd", "ClinicalPatientDetails",
      "ClinicalPatientNameChange", "ClinicalPatientPhoneNumberEdit",
      "OpdEditPatientDetails", "LabEditPatient"
    ]
  }'
```

---

### 12. Revoke User Privilege

```
DELETE /api/users/{id}/privileges/{privilegeId}
```

Soft-retires a single privilege assignment. Returns `Privilege revoked`.

---

### 13. List User's Loggable Departments

```
GET /api/users/{id}/departments
```

**Response items:** `id`, `departmentId`, `departmentName`

---

### 14. Assign Loggable Departments to User

```
POST /api/users/{id}/departments
```

```json
{ "departmentIds": [481, 86405, 1048772] }
```

- Existing active assignments are skipped.
- Non-existent department IDs return **400**.
- Returns the updated department list.

---

## B. Role API (`/api/user-roles`)

### 1. List Roles

```
GET /api/user-roles
```

**Response items:** `id`, `name`, `description`, `retired`

### 2. Get Role by ID

```
GET /api/user-roles/{id}
```

### 3. Create Role

```
POST /api/user-roles
```

```json
{ "name": "PharmacySupervisor", "description": "Pharmacy supervision role" }
```

### 4. Update Role

```
PUT /api/user-roles/{id}
```

### 5. Retire Role

```
DELETE /api/user-roles/{id}
```

### 6. List Role Privileges

```
GET /api/user-roles/{id}/privileges
```

**Response items:** `id`, `privilege`, `departmentId`

### 7. Assign Role Privileges

```
POST /api/user-roles/{id}/privileges
```

```json
{
  "privileges": ["Pharmacy", "PharmacyIssueBill"],
  "departmentId": 485
}
```

Duplicates are skipped. Returns updated role privilege list.

---

## C. Login History API (`/api/logins`)

Useful for finding which users have recently accessed a department — e.g. before bulk-assigning a new privilege.

### 1. List Logins

```
GET /api/logins?departmentId={id}&userId={id}&fromDate={date}&toDate={date}&days={n}&page={n}&size={n}
```

| Parameter | Type | Description |
|---|---|---|
| `departmentId` | Long | Filter by department |
| `userId` | Long | Filter by user |
| `days` | Int | Logins from the last N days (takes precedence over `fromDate`/`toDate`) |
| `fromDate` | String | Start of range, format `yyyy-MM-dd` (inclusive) |
| `toDate` | String | End of range, format `yyyy-MM-dd` (inclusive, end of day) |
| `page` | Int | Default `0` |
| `size` | Int | Default `20`, max `100` |

Results are ordered most recent first.

**Response items:** `id`, `userId`, `userName`, `departmentId`, `departmentName`, `institutionId`, `logedAt`, `logoutAt`, `ipAddress`, `browser`

**Examples:**
```bash
# Last 10 days in OPD
curl "http://localhost:9090/rh/api/logins?departmentId=481&days=10&size=50" \
  -H "Finance: YOUR_API_KEY"

# Specific date range
curl "http://localhost:9090/rh/api/logins?departmentId=481&fromDate=2026-03-01&toDate=2026-03-31" \
  -H "Finance: YOUR_API_KEY"
```

---

### 2. Last Login Per User

```
GET /api/logins/last-per-user?departmentId={id}&size={n}
```

Returns one record per unique user (the most recent login), deduplicated in application memory. Ideal for finding the active user set for a department.

| Parameter | Type | Description |
|---|---|---|
| `departmentId` | Long | Filter by department |
| `size` | Int | Max distinct users to return, default `20`, max `100` |

**Response items:** `userId`, `userName`, `departmentId`, `departmentName`, `lastLogin`

```bash
curl "http://localhost:9090/rh/api/logins/last-per-user?departmentId=481&size=50" \
  -H "Finance: YOUR_API_KEY"
```

---

## Common Workflows

### Workflow A: Grant a privilege set to all recent users of a department

```bash
# Step 1: Find department ID
curl "http://localhost:9090/rh/api/departments/search?query=MRI" \
  -H "Finance: YOUR_API_KEY"
# → departmentId = 1048772

# Step 2: Find users who logged in during the last 10 days
curl "http://localhost:9090/rh/api/logins/last-per-user?departmentId=1048772&size=50" \
  -H "Finance: YOUR_API_KEY"
# → userIds = [20701, 547569, ...]

# Step 3: Discover valid privilege names
curl "http://localhost:9090/rh/api/users/privileges/available" \
  -H "Finance: YOUR_API_KEY"

# Step 4: Bulk-assign across each user's own loggable departments
curl -X POST "http://localhost:9090/rh/api/users/bulk-privileges" \
  -H "Finance: YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "userIds": [20701, 547569],
    "privileges": ["ClinicalPatientEdit", "OpdEditPatientDetails", "LabEditPatient"]
  }'
```

---

### Workflow B: Onboard a new user with role and departments

```bash
# Create user
curl -X POST "http://localhost:9090/rh/api/users" \
  -H "Finance: YOUR_API_KEY" -H "Content-Type: application/json" \
  -d '{"name":"new.user","password":"Welcome#2026","personName":"New User","activated":true}'
# → userId = 99999

# Assign loggable departments
curl -X POST "http://localhost:9090/rh/api/users/99999/departments" \
  -H "Finance: YOUR_API_KEY" -H "Content-Type: application/json" \
  -d '{"departmentIds":[481, 86405]}'

# Assign privileges per department
curl -X POST "http://localhost:9090/rh/api/users/99999/privileges" \
  -H "Finance: YOUR_API_KEY" -H "Content-Type: application/json" \
  -d '{"privileges":["Opd","OpdBilling","Search"],"departmentId":481}'
```

---

### Workflow C: Audit who has a specific privilege in a department

```bash
# List all users in a department
curl "http://localhost:9090/rh/api/users?departmentId=481&size=100" \
  -H "Finance: YOUR_API_KEY"

# For each userId, check their privileges
curl "http://localhost:9090/rh/api/users/20701/privileges" \
  -H "Finance: YOUR_API_KEY"
```

---

## Request / Response DTOs

### `UserUpsertRequestDTO`

| Field | Type | Create | Update | Description |
|---|---|---|---|---|
| `name` | String | Required | Optional | Login name (must be unique) |
| `password` | String | Required | Optional | Plaintext — API hashes before persisting |
| `code` | String | — | — | Short code |
| `email` | String | — | — | Email address |
| `telNo` | String | — | — | Telephone |
| `personName` | String | — | — | Display name |
| `personMobile` | String | — | — | Mobile number |
| `institutionId` | Long | — | — | Institution link |
| `siteId` | Long | — | — | Site link |
| `departmentId` | Long | — | — | Primary department |
| `roleId` | Long | — | — | Role reference |
| `activated` | Boolean | — | — | Activation state |

### `BulkPrivilegeAssignmentRequestDTO`

| Field | Type | Required | Description |
|---|---|---|---|
| `userIds` | List\<Long\> | Yes | Target users |
| `privileges` | List\<String\> | Yes | Privilege enum names (see `/privileges/available`) |
| `departmentId` | Long | No | If omitted, assigns across all of each user's loggable departments |

### `PrivilegeAssignmentRequestDTO`

| Field | Type | Required | Description |
|---|---|---|---|
| `privileges` | List\<String\> | Yes | Privilege enum names |
| `departmentId` | Long | No | Department scope |

### `DepartmentAssignmentRequestDTO`

| Field | Type | Required | Description |
|---|---|---|---|
| `departmentIds` | List\<Long\> | Yes | Department IDs to assign |

### `PasswordChangeRequestDTO`

| Field | Type | Required | Description |
|---|---|---|---|
| `currentPassword` | String | Self-change only | Validated against stored hash |
| `newPassword` | String | Yes | New plaintext password |

### `UserRoleUpsertRequestDTO`

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | String | Create: Yes | Role name |
| `description` | String | No | Role description |

### `RolePrivilegeAssignmentRequestDTO`

| Field | Type | Required | Description |
|---|---|---|---|
| `privileges` | List\<String\> | Yes | Privilege enum names |
| `departmentId` | Long | No | Department scope |

---

## Error Reference

| HTTP | Message | Cause |
|---|---|---|
| 401 | `Not a valid key` | Missing/expired/invalid API key |
| 403 | `Insufficient privileges` | Caller lacks `Admin` privilege |
| 404 | `User not found` | User ID not found or retired |
| 404 | `Role not found` | Role ID not found or retired |
| 404 | `Department not found` | Department ID invalid |
| 404 | `Privilege assignment not found` | Privilege ID not found or already retired |
| 400 | `name and password are required` | Create user missing fields |
| 400 | `User name already exists` | Duplicate login name |
| 400 | `privileges are required` | Empty privileges list |
| 400 | `userIds are required` | Empty userIds in bulk request |
| 400 | `departmentIds are required` | Empty department list |
| 400 | `newPassword is required` | Missing new password |
| 400 | `Current password is invalid` | Wrong current password on self-change |
| 400 | `Invalid privilege: {name}` | Privilege name not in `Privileges` enum |
| 400 | `Invalid departmentId` | Non-numeric or non-existent department ID |
| 400 | `Invalid fromDate format, expected yyyy-MM-dd` | Bad date string |
| 400 | `Invalid JSON format` | Malformed request body |
| 500 | `Internal server error` | Unexpected server-side error |

---

## Security Notes

1. **Least privilege**: avoid assigning `Admin` unless strictly necessary.
2. **Prefer roles**: use `WebUserRole` + role privileges rather than many direct user privilege rows.
3. **Rotate API keys**: enforce short expiry dates; rotate on staff changes.
4. **HTTPS only** in production — the `Finance` header is a bearer token.
5. **Audit password resets**: treat reset/change password endpoints as sensitive operations.
6. **Soft-retire only**: all deletes are soft (retired flag). Privilege rows are never hard-deleted.

---

## Related Documentation

- `developer_docs/API_INSTITUTION_DEPARTMENT_MANAGEMENT.md` — department lookup endpoints
- `developer_docs/API_Documentation_For_AI_Agents.md` — AI agent usage patterns
- `developer_docs/security/privilege-system.md` — how privileges work in the UI and code
