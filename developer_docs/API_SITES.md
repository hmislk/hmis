# Sites API

Base path: `/api/sites`
Authentication: `Finance` header
Content-Type: `application/json`

A "Site" is an `Institution` with `institutionType = Site`. Sites represent physical locations such as collection points, sample drop-off sites, or satellite clinics.

## Endpoints

### GET `/api/sites/search` — Search sites

| Parameter | Type | Description |
|-----------|------|-------------|
| `query` | string | Name or code search term |
| `limit` | int | Max results (default 20) |

```bash
GET /api/sites/search?query=Colombo&limit=10
Header: Finance: YOUR_API_KEY
```

Response:
```json
{
  "status": "success",
  "code": 200,
  "data": [
    { "id": 12, "name": "Colombo Collection Point", "code": "COL01", "address": "123 Main St" }
  ]
}
```

---

### GET `/api/sites/{id}` — Get site by ID

```bash
GET /api/sites/12
Header: Finance: YOUR_API_KEY
```

---

### POST `/api/sites` — Create a new site

```json
{
  "name": "Galle Collection Point",
  "code": "GAL01",
  "address": "45 Harbour Rd, Galle",
  "phone": "0912234567",
  "email": "galle@hospital.lk"
}
```

Response: `201 Created` with the new site's `id`.

---

### PUT `/api/sites/{id}` — Update a site

Same fields as POST — only supplied fields are updated.

```json
{ "phone": "0912299999", "address": "New Address" }
```

---

### DELETE `/api/sites/{id}` — Retire (soft-delete) a site

Returns `200` on success, `404` if not found.
