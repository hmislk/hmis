# Custom Form Layout Guide — C3 Hybrid Pattern

## Overview

The dynamic form system (`DesignComponent` / `CaptureComponent`) supports a **C3 hybrid layout pattern** that separates visual presentation from JSF component logic. An AI assistant or a developer can author per-field HTML wrappers while the input widgets remain code-managed PrimeFaces components.

---

## The Three Concerns

| Concern | Owner | Where stored |
|---|---|---|
| Field **layout** — column span, label style, decorators, help text | AI-generated HTML wrappers | `editHtml` / `viewHtml` on `DesignComponent` (copied to `CaptureComponent` at form-fill time) |
| Field **component** — input widget, value binding, conversion, validation | `componentPresentationType` switch in `admission_forms.xhtml` | Code-managed |
| Form-level **grid wrapper** | CSS class on the containing row div | `formCssClass` on `DesignComponent` (the form, not the field) |

---

## Placeholder Tokens

### Edit mode (`editHtml`)

| Token | Replaced with |
|---|---|
| `{{LABEL}}` | The field label (`cc.name`), HTML-escaped |
| `{{INPUT}}` | The rendered PrimeFaces input component for the field type |

The template is **split at `{{INPUT}}`**: everything before becomes the opening wrapper, everything after becomes the closing wrapper. The JSF component is inserted between them at render time.

**Example:**
```html
<div class="col-12 col-md-4 mb-3 border-start border-primary ps-3">
  <label class="form-label fw-semibold text-primary">{{LABEL}}</label>
  <div class="text-muted small mb-1">Enter the admission date</div>
  {{INPUT}}
</div>
```

### View mode (`viewHtml`)

| Token | Replaced with |
|---|---|
| `{{LABEL}}` | The field label, HTML-escaped |
| `{{VALUE}}` | The formatted stored value (see Value Formatting below) |

**Example:**
```html
<div class="col-12 col-md-4 mb-3 border-start border-secondary ps-3">
  <div class="text-muted small">{{LABEL}}</div>
  <div class="fw-semibold fs-5">{{VALUE}}</div>
</div>
```

---

## Value Formatting (`resolveViewValue`)

`InwardFormController.resolveViewValue(CaptureComponent)` converts the stored value to a display string:

| Component type(s) | Storage field | Display |
|---|---|---|
| Calendar | `dateValue` | `dd MMM yyyy` |
| SelectBooleanCheckBox, SelectBooleanButton, ToggleSwitch, TriStateCheckBox | `booleanValue` | `Yes` / `No` |
| SelectCheckBoxMenu, SelectManyButton, MultiSelectListBox | `longTextValue` (pipe-delimited) | Comma-separated choice **labels** (labels resolved from `DesignComponentChoice`) |
| Input_Number, Spinner, Slider | `doubleValue` / `intValue` | String representation of the number |
| Rating | `ratingIntValue` | Number |
| Input_text, SelectOneMenu, SelectOneRadio, SelectOneListBox, AutoComplete | `shortTextValue` | Raw value |
| Input_text_Area, TextEditor | `longTextValue` | Raw text (may contain HTML for TextEditor) |
| Signature | `longTextValue` (Base64) | `[Signature captured]` |

---

## Form-Level CSS (`formCssClass`)

Set on the `DesignComponent` that represents the **form** (not the field). Controls the Bootstrap row wrapper class on `admission_forms.xhtml`.

| Value | Effect |
|---|---|
| *(blank)* | Default: `row` |
| `row row-cols-1 row-cols-md-3 g-3` | Three columns on medium+, one on mobile |
| `row row-cols-1 g-2` | Always single-column |

---

## Fallback Behaviour

- If `editHtml` is null or blank → the default `col-12 col-md-6 mb-2` wrapper with a `form-label` is used.
- If `viewHtml` is null or blank → a simple label + bold-value div is used.
- If `formCssClass` is null or blank → `row` is used.

All existing forms created before C3 support continue to work unchanged.

---

## Security

- HTML wrappers are authored by clinical administrators in the form designer, not by patients or external users.
- Only `{{LABEL}}`, `{{INPUT}}`, and `{{VALUE}}` tokens are substituted — arbitrary EL expressions are not evaluated.
- `{{LABEL}}` and `{{VALUE}}` are passed through `escapeHtml()` before substitution to prevent stored XSS via field names or stored values.
- `{{INPUT}}` is JSF-rendered HTML, which is safe by construction.
- `escape="false"` on `h:outputText` is only used on admin-authored content — not on user-submitted form data.

---

## AI Prompt Template

To ask Claude (or another AI) to generate wrappers for a form:

```
Generate editHtml and viewHtml wrappers for each of these fields in a Bootstrap 5 + PrimeFaces layout.
Use {{LABEL}} for the field label, {{INPUT}} for the input widget (editHtml only), and {{VALUE}} for the display value (viewHtml only).
All wrappers should be col-based (the parent row uses class="row").

Fields:
- Admission Date (Calendar)
- Ward (SelectOneMenu, choices: Medical, Surgical, ICU)
- Diagnosis (Input_text_Area)
- Attending Doctor (Input_text)

Design goal: clinical card-style, left border accent, compact spacing.
```

---

## Where to Configure

### Form designer (admin)

- **Form level**: `forms/data_entry_form.xhtml` — "Form CSS Class" field
- **Field level**: `forms/data_entry_item.xhtml` — "Edit Wrapper HTML" and "View Wrapper HTML" textareas at the bottom of the field editor

### Runtime (patient forms)

- `inward/admission_forms.xhtml` — View/Edit toggle button switches between view and edit mode for the loaded form

---

## Common Layout Examples

### Two columns (default-like)
```html
<div class="col-12 col-md-6 mb-2">
  <label class="form-label d-block">{{LABEL}}</label>
  {{INPUT}}
</div>
```

### Three columns, accent border
```html
<div class="col-12 col-md-4 mb-3 border-start border-3 border-primary ps-3">
  <label class="form-label fw-semibold text-primary small text-uppercase">{{LABEL}}</label>
  {{INPUT}}
</div>
```

### Full-width with help text
```html
<div class="col-12 mb-3">
  <label class="form-label fw-semibold">{{LABEL}}</label>
  <div class="text-muted small mb-1">Provide a detailed description</div>
  {{INPUT}}
</div>
```

### View — card style
```html
<div class="col-12 col-md-4 mb-3">
  <div class="card h-100">
    <div class="card-body py-2 px-3">
      <div class="card-subtitle text-muted small">{{LABEL}}</div>
      <div class="card-title mb-0 fw-semibold">{{VALUE}}</div>
    </div>
  </div>
</div>
```
