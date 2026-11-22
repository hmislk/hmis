# Maintenance Windows API

Schedule maintenance windows that show a banner to users. The banner appears on every page for users of the
affected institutions or departments, from `showFromAt` until `endAt`.

Base path: `/api/maintenance_windows`

## Authentication and who may write

Send the API key in the `Finance` header.

| Method | Needs |
|--------|-------|
| GET | Any valid API key |
| POST, DELETE | A valid key **and** its owner holds the `ManageMaintenanceWindows` privilege (granted to the user in any department under Administration, Manage Users, Privileges) |

A key whose owner lacks the privilege gets `403`. Roles are not consulted; the privilege must be on the user.

## Dates

`yyyy-MM-dd HH:mm:ss`, in the server's local time.

## Endpoints

### GET /api/maintenance_windows
Lists active and upcoming (not ended, not cancelled) windows, soonest start first.

### POST /api/maintenance_windows
Body:

| Field | Required | Notes |
|-------|----------|-------|
| `message` | yes | Shown in the banner |
| `startAt` | yes | Maintenance start |
| `endAt` | yes | Must be after `startAt` |
| `title` | no | |
| `showFromAt` | no | Default `startAt` minus 24 hours; must not be after `startAt` |
| `institutionIds` | no | Empty or missing = all institutions |
| `departmentIds` | no | Empty or missing = all departments |
| `reference` | no | For example a PR number or a note |

A window with institutions or departments set shows to users whose logged institution or department is in the lists.
Unknown ids return `400`.

### DELETE /api/maintenance_windows/{id}
Cancels (soft-retires) the window. `404` when it does not exist or is already cancelled.

## Response

```json
{"status":"success","code":200,"data":{"id":1,"title":"Upgrade","message":"System upgrade","startAt":"2026-12-01 22:00:00","endAt":"2026-12-02 02:00:00","showFromAt":"2026-11-30 22:00:00","institutionIds":[],"departmentIds":[],"source":"api","reference":"PR 123","retired":false}}
{"status":"error","code":400,"message":"End time must be after the start time"}
```

## Examples

```bash
# List
curl -s -H "Finance: <API_KEY>" "http://localhost:8080/rh/api/maintenance_windows"

# Create
curl -s -H "Finance: <API_KEY>" -H "Content-Type: application/json" \
  -X POST "http://localhost:8080/rh/api/maintenance_windows" \
  -d '{"title":"Upgrade","message":"The system will be unavailable for an upgrade.","startAt":"2026-12-01 22:00:00","endAt":"2026-12-02 02:00:00","reference":"PR 123"}'

# Cancel
curl -s -H "Finance: <API_KEY>" -X DELETE "http://localhost:8080/rh/api/maintenance_windows/1"
```

## Notes

- The banner list is cached for about 45 seconds. A create or cancel through the page or this API clears the cache on
  the server that handled it, so on a multi-server setup the other servers show it within the cache time.
- Banner only: no login blocking, no SMS or email.
