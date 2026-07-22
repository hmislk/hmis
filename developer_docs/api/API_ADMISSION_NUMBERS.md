# Admission Number Counters API

REST endpoint for viewing and resetting the BHT/OPD-card admission-number sequence counter (`AdmissionNumber`) for a given admission type (and institution, when institution-based numbering is enabled).

**Purpose:** Staff sometimes type their own override into the BHT/OPD-card number field at admission time instead of accepting the auto-generated number. When that happens the system's counter is intentionally left untouched (issue #22336), so the next auto-generated number may fall behind the highest number actually printed/used. This endpoint lets an administrator (or the AI chat assistant, on explicit instruction) check the counter and, once the correct next number is known, realign the counter so future admissions continue from the right point.

**Authentication:** `Finance` header (API key)

---

## GET — View the current counter

```
GET /api/admission-numbers?admissionTypeId=1192&institutionId=
```

| Param | Required | Description |
|-------|----------|-------------|
| admissionTypeId | Yes | Numeric `AdmissionType` ID (e.g. `1192` for OPD Card) |
| institutionId | No | Numeric `Institution` ID. Only relevant when "Generate Separate BHT Number Series for Each Institution" is enabled; otherwise ignored |

**Response 200:**
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "admissionTypeId": 1192,
    "admissionTypeName": "OPD Card",
    "institutionId": null,
    "institutionBased": false,
    "lastAdmissionNumber": 48213,
    "nextAdmissionNumber": 48214
  }
}
```

`lastAdmissionNumber` is the last number actually issued by the counter; `nextAdmissionNumber` is what the next call to the generator would hand out. If no counter row exists yet, both values are computed from the current admission count for that series (the same self-initialization logic the generator itself uses) without creating a row.

---

## PUT — Reset the counter

```
PUT /api/admission-numbers?admissionTypeId=1192&institutionId=
Content-Type: application/json
```

```json
{ "lastAdmissionNumber": 48250 }
```

| Field | Required | Description |
|-------|----------|-------------|
| lastAdmissionNumber | Yes | The corrected **last-used** number, not the next number. The next auto-generated number will be this value + 1 |

**Response 200:**
```json
{
  "status": "success",
  "code": 200,
  "data": {
    "admissionTypeId": 1192,
    "admissionTypeName": "OPD Card",
    "institutionId": null,
    "institutionBased": false,
    "previousLastAdmissionNumber": 48213,
    "lastAdmissionNumber": 48250,
    "nextAdmissionNumber": 48251
  }
}
```

If no counter row exists yet for the series, one is created with `lastAdmissionNumber` set to the value supplied.

**Errors:**
- `400` — missing `admissionTypeId` query param, missing/invalid JSON body, or missing `lastAdmissionNumber` in the body
- `401` — invalid or missing `Finance` API key
- `404` — `admissionTypeId` or `institutionId` does not resolve to an existing record
- `500` — unexpected server error

Every successful reset is written to the audit log (`AdmissionNumber`, event `Admission Number Counter Reset`) recording the previous and new `lastAdmissionNumber` along with the acting user.

---

## Notes

- Uses the same in-JVM lock, cache-bypass read, and immediate-flush write as the internal number generator (`BillNumberGenerator`), so a reset is visible to the very next admission on any app instance.
- This endpoint does not itself change any `Admission` record — it only affects future number generation.
- Related: `AdmissionController.saveSelected()` no longer draws (and burns) a counter value when staff have overridden the suggested BHT/OPD-card text at admission time.
