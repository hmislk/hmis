# User Management API

Base path: `/api/users`
Authentication: `Finance` header
Content-Type: `application/json`

## Endpoints

### GET `/api/users` — List users
Query params: `query` (name search), `departmentId`, `page` (0-based), `size` (1-100, default 20)

```bash
GET /api/users?query=Buddhika&departmentId=485&page=0&size=20
Header: Finance: YOUR_API_KEY
```

Response:
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 101,
      "name": "John Doe",
      "username": "jdoe",
      "email": "jdoe@hospital.lk",
      "activated": true,
      "retired": false
    }
  ]
}
```

---

### POST `/api/users` — Create user

```json
{ "username": "jdoe", "name": "John Doe", "email": "jdoe@hospital.lk", "password": "TempPass1!", "departmentId": 485 }
```

---

### GET `/api/users/{id}` — Get user by ID

---

### PUT `/api/users/{id}` — Update user

```json
{ "name": "John Doe Updated", "email": "new@hospital.lk", "activated": true }
```

---

### DELETE `/api/users/{id}` — Retire (soft-delete) user

---

### POST `/api/users/{id}/reset-password` — Reset password (admin)

```json
{ "newPassword": "NewPass1!" }
```

---

### POST `/api/users/{id}/change-password` — Change own password

```json
{ "currentPassword": "OldPass1!", "newPassword": "NewPass1!" }
```

---

### GET `/api/users/{id}/privileges` — List user's privileges

---

### POST `/api/users/{id}/privileges` — Assign privilege to user

```json
{ "privilege": "OpdBilling", "departmentId": 485 }
```

---

### DELETE `/api/users/{id}/privileges` — Remove privilege from user

```json
{ "privilege": "OpdBilling", "departmentId": 485 }
```

---

### GET `/api/users/{id}/departments` — List user's loggable departments

---

### POST `/api/users/{id}/departments` — Assign department to user

```json
{ "departmentId": 485 }
```

---

### GET `/api/users/privileges/available` — List all valid privilege names

Returns the complete enum list from `Privileges.java`. Use this before assigning privileges to discover valid names.

---

### POST `/api/users/bulk-privileges` — Assign privileges to multiple users at once

```json
{
  "userIds": [101, 102, 103],
  "privileges": ["OpdBilling", "PharmacyRetailSale"],
  "departmentId": 485
}
```

Returns per-user summary of `privilegesAdded` and `privilegesSkipped`.

---

## User Roles

Base path: `/api/user-roles`

### GET `/api/user-roles` — List all roles

### POST `/api/user-roles` — Create role

```json
{ "name": "Pharmacy Staff", "description": "" }
```

### GET `/api/user-roles/{id}` — Get role by ID

### PUT `/api/user-roles/{id}` — Update role

### DELETE `/api/user-roles/{id}` — Retire role

### GET `/api/user-roles/{id}/privileges` — List role's privileges

### POST `/api/user-roles/{id}/privileges` — Assign privilege to role

```json
{ "privilege": "OpdBilling", "departmentId": 485 }
```
