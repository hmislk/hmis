# Form API Guide

REST API for designing, populating, and querying dynamic clinical forms.

## Authentication

All endpoints use the `Finance` header with an HMIS API key.

## Base Path

`/api/forms`

---

## Registration Checklist (4 steps)

1. `FormApi.java` — `@Path("forms")`, `@RequestScoped` — **done**
2. `ApplicationConfig.addRestResourceClasses()` — add `FormApi.class` — **done**
3. `CapabilityStatementResource.buildResources()` — add "Dynamic Forms" resource — **done**
4. `AnthropicApiService`:
   - `buildToolsArray()` — add `manage_forms` tool — **done**
   - `executeToolCall()` — add `case "manage_forms"` — **done**
   - `buildSystemPrompt()` — add tool description + module listing — **done**

---

## Endpoints

### Form Templates

| Method | Path | Description |
|---|---|---|
| GET | `/forms/templates` | List all non-retired form templates |
| GET | `/forms/templates/{id}` | Get one form template |
| POST | `/forms/templates` | Create a form template |
| PUT | `/forms/templates/{id}` | Update a form template |
| DELETE | `/forms/templates/{id}` | Retire (soft-delete) a form template |

**POST body:**
```json
{ "name": "Admission Form", "description": "...", "formCssClass": "row row-cols-1 row-cols-md-3 g-3" }
```

**Response (single template):**
```json
{ "status": "success", "code": 200, "data": { "id": 12, "name": "Admission Form", "description": "...", "formCssClass": "...", "fieldCount": 5 } }
```

### Fields

| Method | Path | Description |
|---|---|---|
| GET | `/forms/templates/{id}/fields` | List fields for a form (ordered by orderNo) |
| POST | `/forms/templates/{id}/fields` | Add a field |
| PUT | `/forms/fields/{id}` | Update a field |
| DELETE | `/forms/fields/{id}` | Retire a field |

**POST body:**
```json
{
  "name": "Admission Date",
  "componentPresentationType": "Calendar",
  "componentDataType": "Date",
  "orderNo": 1,
  "required": true,
  "editHtml": "<div class=\"col-12 col-md-6 mb-3\"><label class=\"form-label fw-semibold\">{{LABEL}}</label>{{INPUT}}</div>",
  "viewHtml": "<div class=\"col-12 col-md-6 mb-3\"><div class=\"text-muted small\">{{LABEL}}</div><div class=\"fw-semibold\">{{VALUE}}</div></div>"
}
```

**Supported `componentPresentationType` values:**
`Input_text`, `Input_text_Area`, `TextEditor`, `Input_Number`, `Spinner`, `Slider`, `Rating`, `Calendar`,
`SelectBooleanCheckBox`, `SelectBooleanButton`, `ToggleSwitch`, `TriStateCheckBox`,
`SelectOneMenu`, `SelectOneRadio`, `SelectOneListBox`, `SelectCheckBoxMenu`, `SelectManyButton`, `MultiSelectListBox`,
`AutoComplete`, `Signature`

### Choices

| Method | Path | Description |
|---|---|---|
| GET | `/forms/fields/{id}/choices` | List choices for a choice-type field |
| POST | `/forms/fields/{id}/choices` | Add a choice |
| PUT | `/forms/choices/{id}` | Update a choice |
| DELETE | `/forms/choices/{id}` | Retire a choice |

**POST body:**
```json
{ "label": "ICU", "value": "ICU", "orderNo": 3 }
```

### Entries and Values

| Method | Path | Description |
|---|---|---|
| GET | `/forms/entries/{admissionId}` | List all filled form entries for an admission (PatientEncounter ID) |
| GET | `/forms/entries/{entryId}/values` | List all captured field values for a filled form entry |

---

## C3 Layout Token Reference

| Token | Used in | Replaced with |
|---|---|---|
| `{{LABEL}}` | `editHtml`, `viewHtml` | Field label text (HTML-escaped) |
| `{{INPUT}}` | `editHtml` only | The rendered PrimeFaces input widget |
| `{{VALUE}}` | `viewHtml` only | The formatted stored value |

See `developer_docs/forms/custom-layout-guide.md` for full details and examples.

---

## Example Curl Calls

```bash
# List all form templates
curl -H "Finance: <api-key>" https://hmis.example.com/api/forms/templates

# Create a form
curl -X POST -H "Finance: <api-key>" -H "Content-Type: application/json" \
  -d '{"name":"Ward Assessment","formCssClass":"row row-cols-1 row-cols-md-2 g-3"}' \
  https://hmis.example.com/api/forms/templates

# Add a field
curl -X POST -H "Finance: <api-key>" -H "Content-Type: application/json" \
  -d '{"name":"Chief Complaint","componentPresentationType":"Input_text_Area","orderNo":1,"required":true}' \
  https://hmis.example.com/api/forms/templates/12/fields

# Add a choice to a SelectOneMenu field
curl -X POST -H "Finance: <api-key>" -H "Content-Type: application/json" \
  -d '{"label":"Medical","value":"Medical","orderNo":1}' \
  https://hmis.example.com/api/forms/fields/45/choices

# Get filled form entries for admission 1001
curl -H "Finance: <api-key>" https://hmis.example.com/api/forms/entries/1001
```
