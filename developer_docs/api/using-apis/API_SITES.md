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

---

## Bed-board SVG (issue #21592)

A site stores two SVG drawings used by the Inpatient Bed Board page, on a shared
`viewBox="0 0 1000 600"` grid:

- **`svgParentView`** — the site's own empty floor-plan canvas, shown when you
  navigate *into* it.
- **`svgChildView`** — the small shape showing how the site looks as a tile
  *inside its parent's* canvas.

Both fields are accepted on the normal `POST`/`PUT` bodies and returned by `GET`.
SVG is stored **verbatim** (create/update → read back identical); it is sanitised
at render time on the bed board. See the wiki page
[Inpatient — Bed Board](https://github.com/hmislk/hmis/wiki/Inpatient-Bed-Board)
for authoring guidance and copy-paste examples.

### GET `/api/sites/{id}/svg` — Read just the drawings

```json
{ "status": "success", "code": 200,
  "data": { "id": 5, "name": "Karapitiya", "svgParentView": "<svg ...>", "svgChildView": "<svg ...>" } }
```

### PUT `/api/sites/{id}/svg` — Set just the drawings

Only the fields present in the body are changed; pass an empty string to clear a
drawing.

```json
{ "svgParentView": "<svg viewBox=\"0 0 1000 600\">...</svg>",
  "svgChildView":  "<svg viewBox=\"0 0 1000 600\"><ellipse cx=\"500\" cy=\"300\" rx=\"400\" ry=\"200\"/></svg>" }
```

```bash
curl -s -H "Finance: $KEY" -H "Content-Type: application/json" \
  -X PUT "$BASE/api/sites/5/svg" \
  -d '{"svgChildView":"<svg viewBox=\"0 0 1000 600\"><rect width=\"1000\" height=\"600\"/></svg>"}'
```
