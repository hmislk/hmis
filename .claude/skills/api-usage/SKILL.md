---
name: api-usage
description: >
  Reference for calling existing HMIS REST APIs. Use when consuming/invoking an HMIS endpoint —
  verifying data via API during issue investigation, writing a debug curl/script call, or
  checking an endpoint's request/response shape before writing a caller. Covers auth header
  conventions, the standard response envelope, and the developer_docs/api/using-apis/ module
  index. For building or extending an endpoint, use api-development instead.
user-invocable: true
---

# HMIS API Usage Guide

## When This Applies

Any time you need to **call** an HMIS REST endpoint that already exists — not create one:

- Verifying a bill/payment/balance via the read-only APIs while investigating an issue
- Writing a one-off curl/script call to check data during debugging
- Confirming an endpoint's request/response shape before writing a caller against it
- Wiring an API call into the in-app AI Chat tool-calling flow (`AnthropicApiService`)

If you're instead **building or extending** an endpoint (new `@Path` class or new method on one),
use the `api-development` skill — this one has no implementation detail.

---

## Auth Headers

Every request needs an auth header. Which one depends on the module — check the table below
before guessing:

| Header | Used by |
|---|---|
| `Finance` | Default for most modules (bills, balances, pharmacy, inward, users, etc.) |
| `Token` | Channel/booking appointments, consultant management |
| `Config` | Reading/updating `ConfigOption` application settings |
| `FHIR` | FHIR Patient resource endpoints |
| Custom (URL/JSON/Basic) | LIMS lab middleware |
| none | Membership scheme registration/payment (public) |

Send the key as the exact header name above, e.g. `Finance: <apiKey>`. A `401 Not a valid key`
response means the key is invalid, retired, or expired (`ApiKey.dateOfExpiary`).

## Response Envelope

Standard endpoints wrap responses the same way regardless of module:

```json
{"status":"success","code":200,"data":{...}}
{"status":"error","code":400,"message":"..."}
```

POST duplicate-detection endpoints return a bare (non-wrapped) shape instead:

```json
{"status":"already_exists","id":123,"name":"..."}
```

Always check `status` first, not just HTTP status code — some error conditions still return
HTTP 200 with `"status":"error"` in the body.

## Module Index

`developer_docs/api/using-apis/` has one self-contained reference file per module: base path,
auth header, request/response shapes, error codes. **Load only the file for the module you
need** — don't read the whole directory. Start from
[`developer_docs/api/using-apis/README.md`](../../../developer_docs/api/using-apis/README.md)
for the full, current table (module, file, auth header); it's kept up to date as new API docs
are added, so treat it as the source of truth rather than duplicating the list here.

If you're unsure which module owns the data you need, check `README.md`'s table first — several
files disambiguate related-sounding endpoints (e.g. `API_ADMISSION_DETAILS.md` is a consolidated
index of which admission-related API to call).

## Verifying Payments and Balances

A common task during issue investigation: confirm a bill's payments produced the correct balance
updates. Don't hand-roll this — follow
[`API_TESTING_WORKFLOWS.md`](../../../developer_docs/api/using-apis/API_TESTING_WORKFLOWS.md):
fetch the bill (`/api/costing_data/...`), pull the matching balance-history endpoint per payment
method (drawer/deposit/staff-welfare/agent), and assert `after == before + transactionValue`
(with float tolerance, since credit-company payments have no history endpoint and refunds/
cancellations use negative or reversing entries). This same file also covers batch verification
and common troubleshooting symptoms (case-sensitive bill numbers, empty `payments[]` on
transfer/issue bills, etc.).

## General Troubleshooting

| Symptom | Cause / Fix |
|---|---|
| `401 Not a valid key` | Auth key invalid, retired user, or expired — check `ApiKey.dateOfExpiary` |
| `404` / entity not found | IDs and bill numbers are often case-sensitive; confirm exact format |
| `200` with `"status":"error"` | Check `message`, not just HTTP status — see Response Envelope above |
| Numeric fields don't match exactly | Use tolerance (`abs(a - b) < 0.01`) for currency comparisons, not `==` |
