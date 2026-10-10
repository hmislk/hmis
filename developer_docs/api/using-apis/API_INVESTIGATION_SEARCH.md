# Investigation Search API

Search and list laboratory investigations (the `Investigation` item master).

> **Scope of this file:** only `GET /api/investigations/search`. The other `/api/investigations/*`
> endpoints (create, update, activate/deactivate, fees, format) are implemented in
> `com.divudi.ws.investigation.InvestigationApi` and its sibling classes in the same package;
> this file does not document them.

**Base path:** `/api/investigations`

## Authentication

Requires the `Finance` HTTP header containing a valid, non-expired API key.

```
Finance: <api-key>
```

---

## Search Investigations

```
GET /api/investigations/search
```

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `query` | string | No | Case-insensitive substring matched against name, code **or** print name |
| `inactive` | boolean | No | `true` = inactive only, `false` = active only, omit for both |
| `limit` | int | No | Page size (default 20, max 100; larger values are clamped to 100) |
| `offset` | int | No | Rows to skip, for paging (default 0; negative is treated as 0; non-numeric → 400) |
| `includeTotal` | boolean | No | `true` adds `totalCount`, `offset` and `limit` beside `data` (see [Listing the whole master](#listing-the-whole-master)). `false`/omitted = response unchanged. Anything else → 400 |

Retired investigations are never returned. Results are always ordered by `name`, then `id`. The
`id` tiebreaker keeps pages stable when two investigations share a name, so paging never repeats or
skips a row.

**Example:**
```bash
curl -H "Finance: <key>" \
  "https://host/hmis/api/investigations/search?query=blood&limit=20"
```

**Response:**
```json
{
  "status": "success",
  "code": 200,
  "timestamp": "2026-10-01 10:15:00",
  "data": [
    {
      "id": 4021,
      "name": "Blood Picture",
      "code": "BP",
      "printName": "Blood Picture",
      "inactive": false,
      "reportType": "PathologyOrHaematology",
      "bypassSampleWorkflow": false,
      "vatable": false,
      "vatPercentage": 0.0,
      "categoryId": 12,
      "categoryName": "Haematology",
      "sampleId": 3,
      "sampleName": "Whole Blood",
      "containerId": 2,
      "containerName": "EDTA Tube",
      "discountAllowed": true
    }
  ]
}
```

Fields without a value are omitted from the JSON — here `analyzerId`/`analyzerName` are absent
because no analyzer is linked — so treat a missing key as "not set".

---

## Listing the whole master

One request returns at most 100 rows, so list the full investigation master by paging with
`offset` instead of firing many substring queries. Add `includeTotal=true` to learn how many rows
match:

```bash
curl -H "Finance: <key>" \
  "https://host/hmis/api/investigations/search?limit=100&offset=0&includeTotal=true"
```

```json
{
  "status": "success",
  "code": 200,
  "timestamp": "2026-10-01 10:15:00",
  "data": [ { "id": 4021, "name": "Blood Picture", "...": "..." } ],
  "totalCount": 812,
  "offset": 0,
  "limit": 100
}
```

- `data` is still a plain array, so existing callers are unaffected. `totalCount` is the number of
  rows matching the filters, ignoring `limit` and `offset`.
- `limit` in the response is the **effective** page size after clamping to 100. Advance `offset`
  by the number of rows you actually received (`data.length`), never by the `limit` you asked for.
- Stop at the first page that returns fewer rows than the `limit` you sent (an empty page is the
  limiting case). A failed query comes back as an HTTP error, never as an empty page, so a short
  page really is the end of the list. `totalCount` is for progress and for cross-checking that you
  collected everything; callers that do not ask for it pay nothing for it.
- Paging combines with `query` and `inactive`.
- Without `includeTotal=true` no count query runs and the response is exactly what it was before
  paging existed.

```bash
# Walk the whole investigation master, 100 at a time
offset=0; limit=100
while :; do
  page=$(curl -sf -H "Finance: <key>" \
    "https://host/hmis/api/investigations/search?limit=$limit&offset=$offset") || { echo "request failed" >&2; exit 1; }
  count=$(echo "$page" | jq '.data | length')
  echo "$page" | jq -c '.data[] | {id, name}'
  offset=$((offset + count))
  [ "$count" -lt "$limit" ] && break
done
```

The same recipe lists the service master via `GET /api/services/search` — see
[API_SERVICE_MANAGEMENT.md](API_SERVICE_MANAGEMENT.md#listing-the-whole-master). Together they give
every item ID needed by `POST /api/item-mappings/bulk` ([API_ITEM_MAPPINGS.md](API_ITEM_MAPPINGS.md)).

## Error Responses

| Status | Cause |
|--------|-------|
| 400 | `offset` is not an integer, or `includeTotal` is not `true`/`false` |
| 401 | Missing, unknown, retired or expired `Finance` key |
| 500 | Unexpected server error (message in `message`) |
