# PVC Card Print — Dynamic Front/Back Configuration

## Origin

Requirement from RMH: the patient PVC card (currently blank cards, printed on-site)
needs front and back printed separately, with card size, margins, text position,
background image, and barcode (type/length/height) all configurable per deployment
— without code changes. After deployment, layout tuning happens by printing a test
card, reviewing it, and adjusting configuration (expected to be done in a Claude
Code session, driven by the physical print output).

## Current state (as of this design)

- `opd/patient.xhtml` has a single "Print Card" button using PrimeFaces's
  client-side `<p:printer target="groupPatientCard"/>` — no backing bean method,
  no server-side print logic.
- The printed panel is an inline `h:panelGroup` in the same page: a
  `<p:barcode type="code128" format="svg">` of the PHN, plus hardcoded
  `font-size:8px` text for PHN/name/sex/DOB/NIC/phone. Fixed `10cm x 5cm` wrapper
  with a `scale(2.0)` CSS transform. No front/back split, nothing configurable.
- A separate, disabled legacy flow (`clinical/clinical_print_barcode.xhtml` +
  `BarcodeController`, Barbecue-library-based) models a real ID-card layout
  (`.patientCard` CSS class, 8.56cm × 5.398cm) but its launch button is commented
  out and it is not reachable from the UI today.
- Three barcode mechanisms exist in the codebase: PrimeFaces `<p:barcode>`
  (backed by Barcode4J, already in use), Barbecue (used only by the disabled
  legacy flow), and an unused ZXing dependency with no code referencing it.
- `ConfigOptionApplicationController` is the established, app-wide (not
  department-scoped) mechanism for configurable print settings (see
  `developer_docs/configuration/printer-configuration-system.md`), but no
  card-specific keys exist yet.
- The `Upload` entity (`core/entity/Upload.java`) already supports storing an
  image either as a DB BLOB (`baImage`) or as an external URL (`fileUrl`),
  discriminated by a `UploadType` enum (`core/data/UploadType.java`) describing
  the image's purpose (`User_Signature`, `Report_background_image`, etc.).
  Rendering code picks blob vs. URL based on which is non-empty — no separate
  mode flag. This is reused rather than building new storage.
- Deploys run `asadmin undeploy` + `asadmin deploy --force` every time (see
  `developer_docs/deployment/ci-cd-pipeline-overview.md`), wiping the exploded
  webapp directory. Nothing on the application server filesystem survives a
  redeploy or a migration to a new server — confirmed by checking the one place
  in the codebase that writes images to a webapp-relative path
  (`PhotoCamBean`), which loses its files on every redeploy. This is why
  background images must be DB-stored (via `Upload`), not filesystem paths.

## Goals

- Split "Print Card" into independent **Print Front** / **Print Back** actions.
- Make the following configurable per deployment, without a code change:
  - Card width, height, margin.
  - Per-field visibility and position (x, y, font size, font color) for: name,
    date of birth, phone number, gender, address, institution name, department
    name.
  - Barcode type, width, height, position (barcode content is always the
    patient's PHN — not itself configurable).
  - Background image per side, either uploaded (stored in DB) or an external
    URL.
- Provide an admin page to edit all of the above with a live preview, so future
  tuning (including a future Claude Code session working from a photographed
  printout) only touches configuration, never code.

## Non-goals

- No change to *what* barcode content is encoded (always PHN).
- No department-level override — one layout per deployment, consistent with
  `ConfigOptionApplicationController` being app-wide.
- No new JPA entity and no schema migration. All new structured data reuses the
  existing `Upload` entity (plus two new `UploadType` enum constants) and the
  existing `ConfigOption`/`ConfigOptionApplicationController` table (two new
  LONG_TEXT keys).

## Data model

### UploadType (enum addition only — `core/data/UploadType.java`)

Two new constants:
- `PVC_Card_Front_Background`
- `PVC_Card_Back_Background`

Each has at most one live (non-retired) `Upload` row at a time, app-wide (no
parent entity association needed — single-tenant-per-deployment). Reuses the
existing blob-or-URL convention: if `fileUrl` is set, render via URL; otherwise
stream `baImage` via a `UploadViewController`-style method.

### ConfigOptionApplicationController keys (LONG_TEXT, new)

- `"PVC Card Front Layout"`
- `"PVC Card Back Layout"`

Each holds a JSON object:

```json
{
  "widthMm": 85.6,
  "heightMm": 54.0,
  "marginMm": 2.0,
  "slots": {
    "name":           { "visible": true,  "leftMm": 5,  "topMm": 20, "fontSizePt": 9,  "fontColor": "#000000" },
    "dob":             { "visible": true,  "leftMm": 5,  "topMm": 26, "fontSizePt": 8,  "fontColor": "#000000" },
    "phone":           { "visible": true,  "leftMm": 5,  "topMm": 32, "fontSizePt": 8,  "fontColor": "#000000" },
    "gender":          { "visible": true,  "leftMm": 5,  "topMm": 38, "fontSizePt": 8,  "fontColor": "#000000" },
    "address":         { "visible": false, "leftMm": 5,  "topMm": 44, "fontSizePt": 7,  "fontColor": "#000000" },
    "institutionName": { "visible": true,  "leftMm": 5,  "topMm": 4,  "fontSizePt": 11, "fontColor": "#000000" },
    "departmentName":  { "visible": false, "leftMm": 5,  "topMm": 10, "fontSizePt": 9,  "fontColor": "#000000" },
    "barcode":         { "visible": true,  "leftMm": 5,  "topMm": 48, "widthMm": 40, "heightMm": 10, "type": "code128" },
    "phn":             { "visible": true,  "leftMm": 5,  "topMm": 59, "fontSizePt": 7,  "fontColor": "#000000" }
  }
}
```

The JSON stores **position/size/visibility/style only** — never the actual
field values. The real values (patient name, DOB, phone, gender, address,
institution name, department name) are bound live at render time via the
existing session/patient managed beans, exactly as today's inline card does.
This keeps the config schema stable regardless of which patient/institution is
being printed.

**Post-design addition:** a `phn` text slot was added after the original design
(this doc's slot list above predates it) so the readable PHN that the
pre-existing inline card showed as plain text isn't lost now that the barcode
is the only PHN representation — it follows the exact same slot mechanism as
`name`/`dob`/etc., just bound to `patientController.current.phn`.

If a key is missing or fails to parse, the controller falls back to built-in
default values (shown above) rather than rendering a blank/broken card.

## Components

### `PvcCardLayoutController` (new, `@Named @ViewScoped`)

- Reads both JSON config keys on init, parses into a small view-model (`front`,
  `back`), each exposing `widthMm`/`heightMm`/`marginMm` and a `slots` map
  keyed by slot name (`name`, `dob`, `phone`, `gender`, `address`,
  `institutionName`, `departmentName`, `barcode`) → an object with
  `visible`/`leftMm`/`topMm`/`fontSizePt`/`fontColor` (barcode slot additionally has
  `widthMm`/`heightMm`/`type`).
- Falls back to defaults on missing/invalid JSON (see above).
- Used both by the print panels on `opd/patient.xhtml` and by the admin page's
  live preview (shared rendering fragment, see below).
- Exposes background image source per side (delegates to a small read method
  resolving the matching `Upload` row's URL-or-streamed-blob).

### Shared print-panel fragment (new, e.g.
`resources/ezcomp/pvc_card_panel.xhtml` composite component)

Takes `side` (front/back) and renders:
- Outer `div`/`h:panelGroup` sized by `widthMm`/`heightMm`, with
  `background-image` CSS pointing at the resolved background source.
- One `h:panelGroup` per slot: `rendered` on `visible`, absolutely positioned
  (`left`/`top` from `leftMm`/`topMm`, `font-size` from `fontSizePt`, `color` from
  `fontColor`), content bound to the real data source for that slot.
- Barcode slot renders `<p:barcode value="#{patientController.current.phn}"
  type="#{...slot.type}" .../>` sized from the slot's width/height.

Reused both inside `opd/patient.xhtml` (for actual printing) and inside the new
admin page (for live preview) — single source of truth for rendering so the
preview can't drift from the real print output.

### `opd/patient.xhtml` changes

- Remove the existing inline card markup, the `Libre Barcode 128 Text`
  stylesheet include, and the `scale(2.0)` wrapper.
- Replace the single "Print Card" button with two buttons: **Print Front**
  (`<p:printer target="groupCardFront"/>`) and **Print Back**
  (`<p:printer target="groupCardBack"/>`).
- Two hidden panels (`groupCardFront`, `groupCardBack`), each rendering the
  shared print-panel fragment for that side.

### New admin page: `admin/institutions/pvc_card_layout.xhtml`
(backing bean: `PvcCardLayoutController` — the same bean used for rendering on
`opd/patient.xhtml`, extended with edit/save methods, so the admin preview and
the real print path can never drift apart)

- Front/Back tabs, each editing its own JSON independently.
- Card width/height/margin numeric inputs.
- Background image: `p:fileUpload` writing to the matching `Upload` row, plus a
  plain URL text input as the alternative (uploading clears the URL field and
  vice versa, mirroring `ReportFormatController.removeUploadedFile()` /
  `removeUrlFile()`).
- One row per slot: visible checkbox, X/Y/font-size inputs, color picker;
  barcode row additionally gets a type dropdown (Barcode4J-supported types)
  and width/height inputs.
- Live preview using the shared print-panel fragment bound to the in-memory
  (unsaved) form state via `p:ajax` on each input.
- Save button serializes form state to JSON and writes it to the corresponding
  `ConfigOptionApplicationController` key.
- On load, reads existing JSON (or defaults if missing) to populate the form.

## Error handling

- Malformed/missing layout JSON → fall back to built-in defaults; card always
  renders something sensible.
- Missing background image (no `Upload` row, empty blob, empty URL) → card
  renders without a background, text/barcode still positioned correctly.
- Admin page validates on save (dimensions > 0, positions within card bounds,
  font size > 0); invalid input blocks save with an inline PrimeFaces message.

## Testing

- Playwright E2E on `opd/patient.xhtml`: open a patient (via menu navigation,
  department selected first per project convention), verify both print buttons
  exist and each panel renders the configured slots bound to real patient data.
- Playwright E2E on the new admin page: change a slot's position/visibility,
  verify the live preview updates, save, reload, verify the change persisted.
- Manual: an actual printed front/back card compared against the configured mm
  dimensions — done by the user post-deployment, not automatable here.

## Rollout

1. File a GitHub issue in `hmislk/hmis` describing this feature.
2. Branch from `origin/development`, implement, open a PR targeting
   `development`.
3. After merge, create `-hotfix` branches targeting the RMH production and RMH
   staging branches, cherry-picking the merged commits, one PR per target
   branch (checking for an existing open PR per branch first per project
   convention). Because these are cherry-picks of commits already merged to
   `development`, no separate "mirror into development" step is needed.

## Amendments after first release (#24305, PR #24306)

Found while recording the user videos; all four are in `development`.

| Area | Was | Now |
|---|---|---|
| Patient bound to the card | `pvc_card_panel` read `patientController.current`, so the admin preview printed `null` and had no barcode | The component takes a required `patient` attribute. `opd/patient.xhtml` passes `patientController.current`. The admin page passes `pvcCardLayoutController.previewPatient`, a transient (never persisted) sample patient |
| Barcode size | `p:barcode format="svg"` keeps the SVG's own aspect ratio and centres it in the `<img>`, so `widthMm` never stretched the bars (40 × 8 mm printed about 16 mm wide, centred) | Linear types (`code128`, `code39`, `int2of5`, `codabar`, `ean13`, `ean8`, `upca`, `upce`) are drawn by `PvcCardBarcodeSvg` (Barcode4J) as inline SVG with `preserveAspectRatio="none"`, filling exactly `widthMm` × `heightMm`. Other types (e.g. `qr`), or values a type can't encode, fall back to `p:barcode` |
| Margin and page count | The `@page` was card-sized with `marginMm`, but the card box was also full card size, and the print iframe's body kept its default margin. Each side overflowed onto a second (Letter) page | `marginMm` is the strip the printer leaves blank. The card box is the printable area (card minus margins) and clips; a full-card layer inside it is shifted by `-marginMm`, so slot positions and the background stay measured from the physical card edge. A print-only rule resets `html, body` margin/padding. Each side prints exactly one card-sized page. `0` = edge to edge. Save rejects a margin that leaves no printable area |
| Admin page | Every field change re-rendered `@form` and the tab view snapped back to **Front**; the preview scrolled out of view below the field table | `p:tabView activeIndex` is bound to the view-scoped controller (updated on `tabChange`); the preview is `position: sticky` at 1.5× zoom with a dashed card outline padded by the margin |

### Setting the layout without the UI (Config API)

The two layouts are ordinary app-wide `LONG_TEXT` config options, so a deployment can be set up through the Config API (`developer_docs/api/using-apis/API_CONFIG.md`). That needs a key of type **Config**; Finance/Token/FHIR keys get HTTP 401.

```
POST {base}/api/config/setLongText/PVC%20Card%20Front%20Layout/{url-encoded JSON}
POST {base}/api/config/setLongText/PVC%20Card%20Back%20Layout/{url-encoded JSON}
GET  {base}/api/config/search?keyword=PVC%20Card     # read back and compare
Header: Config: <Config-type key>
```

JSON shape (`PvcCardLayout.toJson()`; missing slots or fields fall back to defaults):

```json
{"widthMm":85.6,"heightMm":54,"marginMm":0,
 "slots":{"name":{"visible":true,"leftMm":5,"topMm":19,"fontSizePt":10,"fontColor":"#000000"},
          "barcode":{"visible":true,"leftMm":22.8,"topMm":34,"fontSizePt":8,"fontColor":"#000000",
                     "widthMm":40,"heightMm":10,"type":"code128"}}}
```

Slot keys: `institutionName`, `departmentName`, `name`, `dob`, `phone`, `gender`, `address`, `barcode`, `phn`.

Background images are `Upload` rows (`PVC_Card_Front_Background` / `PVC_Card_Back_Background`) and have no API. Upload them on the PVC Card Layout page.

### User videos

- Set up the layout: https://youtu.be/_3i6au-1Or0
- Print a card: https://youtu.be/xvFcELFmhzM

## Printing on a larger page (#24330) — disc/ID card tray

Epson ink-tank printers with a disc/ID card tray (e.g. L8050) treat a card job as an **A4 page**: the driver fixes Document Size at A4, and each card slot is a window at a fixed spot on that page, with the card's long edge along the page's long edge. A bare 85.6 × 54 mm landscape page therefore prints blank, clipped or in the wrong place.

Per side, the layout JSON accepts four optional keys (all default to 0, which keeps the old card-only behaviour):

| Key | Meaning |
|---|---|
| `pageWidthMm`, `pageHeightMm` | Page the card is placed on (A4 = 210 × 297). Both 0 = the page is the card |
| `rotationDeg` | 0, 90, 180 or 270, clockwise, about the card centre. Anything else is read as 0 |
| `cardLeftMm`, `cardTopMm` | Top-left of the **rotated** card's bounding box on the page |

`pvc_card_panel` always renders page box → holder (the rotated card's bounding box) → card (centred, rotated) → printable area (card minus `marginMm`). With no page it reduces to the earlier output. `@page` is the page size with margin 0 when a page is set.

Tray printing from Chrome: use the browser's own print dialog (not "Print using system dialog"), destination = the card printer, Paper size A4, Margins None, Scale 100 / Default, Background graphics on. The printer's default preferences should already be Paper Source = Disc/ID Card Tray.

Finding the numbers: print an A4 millimetre grid through the tray onto a spare card, photograph it, and read which page coordinates land on the card. Two cards in the tray are about 74 mm apart; use one slot only. A card sits loosely in its slot (about 2–4 mm), so keep important content a few mm inside the edge. After flipping the card for the back side, the back may need a different `rotationDeg`.
