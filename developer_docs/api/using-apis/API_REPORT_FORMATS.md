# Report Formats API — Common Report Template

Base path: `/api/report-formats`
Authentication: `Finance` header
Content-Type: `application/json`

A **report format** (`ReportFormat`, a `Category` subclass) owns a **common template**: the
`CommonReportItem` rows that print on *every* lab report produced in that format — the
patient-details block (name, age, gender, referring doctor, reference no, reported date,
specimen), the signature block, and the footer.

This is the half of the report layout that
[the Investigation Format API](#relationship-to-the-investigation-format-api) cannot reach.
It is also the half that has to move when a hospital prints onto **pre-printed stationery**
and the header block collides with a pre-printed band.

The same rows are edited by hand at **Menu → Admin → Lims → Report Template**
(`admin/lims/report_template.xhtml`).

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/report-formats` | List report formats |
| `GET` | `/api/report-formats/{categoryId}/items` | List the format's common-template rows |
| `GET` | `/api/report-formats/{categoryId}/items/{itemId}` | Read one row |
| `POST` | `/api/report-formats/{categoryId}/items` | Add a row |
| `PUT` | `/api/report-formats/{categoryId}/items/{itemId}` | Update a row |
| `DELETE` | `/api/report-formats/{categoryId}/items/{itemId}` | Retire (soft-delete) a row |

---

### GET `/api/report-formats` — List report formats

No parameters. Returns every non-retired `ReportFormat`, ordered by name, with the number of
common-template rows each one owns.

```bash
# Set BASE_URL and FINANCE_KEY from user input before running
curl -s -H "Finance: $FINANCE_KEY" "$BASE_URL/api/report-formats"
```

```json
{
  "status": "success",
  "code": 200,
  "data": [
    { "id": 1194, "name": "Main", "code": "main", "orderNo": 0, "itemCount": 29 },
    { "id": 1462, "name": "Culture", "code": "culture", "orderNo": 0, "itemCount": 26 }
  ]
}
```

---

### GET `/api/report-formats/{categoryId}/items` — List common-template rows

Non-retired rows only, ordered by name — the same order the Report Template screen lists them.

```json
{
  "status": "success",
  "code": 200,
  "data": [
    {
      "id": 84472,
      "categoryId": 1194,
      "categoryName": "Main",
      "name": "Name",
      "reportItemType": "PatientName",
      "ixItemType": "Value",
      "riTop": 13.6,
      "riLeft": 23.0,
      "riWidth": 71.0,
      "riHeight": 2.0,
      "riFontSize": 11.0,
      "cssTextAlign": "Left",
      "cssVerticalAlign": "Baseline",
      "cssFontStyle": "Normal",
      "cssFontWeight": "normal"
    }
  ]
}
```

---

### POST `/api/report-formats/{categoryId}/items` — Add a row

`name` is required; everything else is optional. `code` is derived from `name` when omitted.
Returns `201`.

```json
{
  "name": "Reference No",
  "reportItemType": "DepartmentBillNo",
  "ixItemType": "Value",
  "riTop": 15.8,
  "riLeft": 71,
  "riWidth": 23,
  "riHeight": 2,
  "riFontSize": 10.5,
  "cssTextAlign": "Left"
}
```

---

### PUT `/api/report-formats/{categoryId}/items/{itemId}` — Update a row

**Only the fields present in the body are applied.** Everything omitted is left exactly as it
was, so a single coordinate can be nudged on its own:

```bash
# Set BASE_URL and FINANCE_KEY from user input before running
curl -s -H "Finance: $FINANCE_KEY" -H "Content-Type: application/json" \
  -X PUT "$BASE_URL/api/report-formats/1194/items/84472" \
  -d '{"riTop": 16.1}'
```

A request body carrying no recognised field is rejected rather than reported as a no-op success.

---

### DELETE `/api/report-formats/{categoryId}/items/{itemId}` — Retire a row

Soft delete: sets `retired = true` and records the retiring user. The row stops printing but is
never removed from the database.

---

## Fields

| Field | Type | Notes |
|---|---|---|
| `name` | string | Row name on the Report Template screen. Required on POST. |
| `code` | string | Derived from `name` when omitted. |
| `description` | string | Free text. |
| `orderNo` | int | Display order number. |
| `pageNo` | int | Page the row prints on. |
| `reportItemType` | enum | What the row prints — `PatientName`, `PatientAge`, `PatientAgeOnBillDate`, `PatientSex`, `ReferringDoctor`, `DepartmentBillNo`, `BillNo`, `Speciman`, `BHT`, `MRN`, `SampledID`, `ApprovedAt`, `CollectedOn`, `ReceivedOn`, `AutherizedSignature`, `QrCodeLink`, … Omit for a static label. |
| `ixItemType` | enum | How it renders — `Label`, `Value`, `Css`, `Barcode`, `QrCode`, `Html`, `Image`, … Defaults to `Label`. |
| `ixItemValueType` | enum | `Varchar`, `Memo`, `Double`, `Integer`, `Long`, `Image`, `Line`, `Rectangle`, `Circle`, … |
| `htmltext` | string | Content of `Html`-type rows. |
| `formatPrefix` / `formatSuffix` | string | Text printed either side of the value. |
| `riTop` / `riLeft` | double | Position on the page, **in percent**. |
| `riWidth` / `riHeight` | double | Size, **in percent**. |
| `riFontSize` | double | Font size **in points**. |
| `htPix` / `wtPix` | double | Image height/width in pixels (`Image`-type rows). |
| `cssTextAlign` | enum | `Left`, `Right`, `Center`, `Justify`, `Inherit`. |
| `cssVerticalAlign` | enum | `Baseline`, `Sub`, `Super`, `Top`, `TextTop`, `Middle`, `Bottom`, `TextBottom`, `Inherit`. |
| `cssFontStyle` | enum | `Normal`, `Italic`, `Oblique`, `Inherit`. |
| `cssFontFamily` | string | Font family name. |
| `cssFontWeight` | string | e.g. `normal`, `bold`. |

For the enum-backed fields, sending an **empty string** clears the value; omitting the field
leaves it unchanged. An unrecognised value is rejected with the list of valid constants.

### Reads report the rendered value, not the stored one

`riWidth`, `riHeight` and `riFontSize` are read through the entity getters, which substitute
**30**, **2** and **12** respectively when the stored value is `0` — and `ixItemType`,
`cssTextAlign` and `cssFontStyle` likewise fall back to `Label`, `Left` and `Normal`. So a `GET`
reports the numbers the printed report actually uses, which is what you want when diagnosing a
layout, but it is not necessarily what is stored. See issue #23528.

## Scope: report formats only

`CommonReportItem` rows are also used for the HR/clinical **form** templates
(`FormFormatController`, `StaffController`) under ordinary `Category` rows. This API only accepts
a `ReportFormat` id, exactly like the Report Template screen, so a lab-facing call can never
rewrite a form layout by passing the wrong category id. A non-`ReportFormat` category id returns
*"An error occurred: Report format not found with ID: …"*.

## Relationship to the Investigation Format API

| | Investigation Format API | Report Formats API (this one) |
|---|---|---|
| Path | `/api/investigations/{id}/format` | `/api/report-formats/{categoryId}/items` |
| Entity | `InvestigationItem` | `CommonReportItem` |
| Keyed on | the investigation (`item_id`) | the report-format category (`category_id`) |
| Covers | the result table for one test | the header/signature/footer shared by every report of the format |

The two never overlap: `CommonReportItem` rows carry no `item`, so the investigation-scoped
queries can never return them. That gap is what issue #23529 fixed.

## Why write through the API instead of SQL

Layout tuning used to be done with `UPDATE` statements straight against the database, which then
needed an `asadmin disable/enable <app>` to clear the EclipseLink L2 cache before the change was
visible. Writing through this API goes through the facade, so the cache stays correct with no
redeploy and no downtime.

## Moving a whole block

There is no bulk endpoint — moving the patient-details block down by 2.5% is one `PUT` per row.
`GET .../items` first, then issue one `{"riTop": <old + 2.5>}` per row. Doing it row by row is
deliberate: it keeps every change reviewable and reversible.

## Errors

All errors use the standard envelope:

```json
{ "status": "error", "code": 500, "message": "An error occurred: Report format not found with ID: 999" }
```

Every failure except the auth one is a 500 carrying the literal prefix **`An error occurred: `**
before the message below — the same wrapper the sibling Investigation Format API uses. Match on a
substring, not on the start of the string.

| Condition | HTTP | Message (after the prefix) |
|---|---|---|
| Missing/invalid `Finance` header | 401 | `Not a valid key` — no prefix, this path does not go through the wrapper |
| Unknown or retired format | 500 | `Report format not found with ID: {id}` |
| Unknown or retired row | 500 | `Common report item not found with ID: {id}` |
| Row belongs to another format | 500 | `Item {itemId} does not belong to report format {categoryId}` |
| POST without a name | 500 | `Item name is required` |
| PUT with an empty body | 500 | `Valid update request is required` |
| PUT with a blank `name` | 500 | `Item name cannot be blank` |
| Bad enum value | 500 | `Invalid {field}: {value}. Valid values: [...]` |
