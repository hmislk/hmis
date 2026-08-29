# Login History API

Base path: `/api/logins`
Authentication: `Finance` header
Content-Type: `application/json`

## Endpoints

### GET `/api/logins` — Query login history

All parameters are optional query strings.

| Parameter | Type | Description |
|-----------|------|-------------|
| `departmentId` | long | Filter by department ID |
| `userId` | long | Filter by specific user ID |
| `days` | int | Logins from last N days (overrides fromDate/toDate) |
| `fromDate` | string | Start date `yyyy-MM-dd` |
| `toDate` | string | End date `yyyy-MM-dd` (inclusive) |
| `page` | int | Page number, 0-based (default 0) |
| `size` | int | Page size 1-100 (default 20) |

```bash
# All logins in Main Pharmacy in the last 7 days
GET /api/logins?departmentId=485&days=7
Header: Finance: YOUR_API_KEY

# Logins for a specific user
GET /api/logins?userId=60103&fromDate=2026-03-01&toDate=2026-03-31
```

Response:
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 1234,
      "userId": 60103,
      "userName": "jdoe",
      "departmentId": 485,
      "departmentName": "Main Pharmacy",
      "institutionId": 1,
      "logedAt": "2026-03-15 08:32:11",
      "logoutAt": "2026-03-15 17:04:55",
      "ipAddress": "192.168.1.10",
      "browser": "Chrome"
    }
  ]
}
```

---

### GET `/api/logins/last-per-user` — Last login per unique user

Returns the most recent login record per unique user. Useful for finding who is currently active in a department.

| Parameter | Type | Description |
|-----------|------|-------------|
| `departmentId` | long | Filter by department (recommended) |
| `size` | int | Max users to return (default 20, max 100) |

```bash
GET /api/logins/last-per-user?departmentId=485&size=50
Header: Finance: YOUR_API_KEY
```

Response:
```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "userId": 60103,
      "userName": "jdoe",
      "departmentId": 485,
      "departmentName": "Main Pharmacy",
      "lastLogin": "2026-04-04 08:15:00"
    }
  ]
}
```
