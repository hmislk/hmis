# Inward Deposit & Payment Receipts — Dot-Matrix / Raw-Text Print Design

- **Date:** 2026-09-10
- **Status:** Approved (brainstorming) — pending implementation plan
- **Scope:** Inward Deposit Receipt and Inward Payment Receipt, 5×5 paper, on Epson LQ-310 impact printers
- **Driver hospital:** Galle Co-operative Hospital (Coop)

## 1. Problem

Coop prints inward **deposit** and **payment** receipts on 5×5 continuous stationery
(pre-printed letterhead) using an **Epson LQ-310** impact printer. Output is
illegible — even large text cannot be read (see `tmp/coop/poor bill.jpeg`). A
competitor's standalone application prints the same class of receipt crisply on
the same hardware. Coop is not happy.

### Root cause

The current 5×5 templates are screen documents that happen to be printed:

| # | Current code | Effect on an LQ-310 |
|---|---|---|
| 1 | `font-family: sans-serif / Verdana / Arial` throughout (`opd_five_five.css`), with `monospace` mixed in only for the header | Browser rasterises the proportional TrueType font to a **graphics bitmap** and sends that to the impact head. Rasterised proportional glyphs print mushy and drop strokes. This is the photo. |
| 2 | Sizes in `px` and `%` (`font-size: 9px`, `font-size: 80%`, `.billDetailsFiveFive { font-size: 14px }`) | Below the head's effective dot pitch at small sizes; `%` scaling compounds it. |
| 3 | `text-transform: capitalize` on the whole bill | Forces the browser off any printer-native font path (it must re-case, so it rasterises). |
| 4 | `<hr/>` rules and `text-decoration: overline` | Thin graphic lines — missed or smeared by the impact head. |
| 5 | App renders its own letterhead block **on top of** the pre-printed letterhead; no way to suppress it | Double header; app text overlaps the pre-printed logo (visible in the photo). |
| 6 | Fixed `width: 11cm; height: 13.5cm` box + `margin-top: -2cm` | Fights tractor-feed form length; contributes to vertical misalignment. |
| 7 | Competitor uses a **standalone app** | Almost certainly prints **raw ESC/P text**, not an HTML rasteriser — hence crisp. |

### Research (industry consensus)

- Browsers rasterise the whole page to a bitmap; proportional fonts print badly
  on impact heads. Fix on the HTML side: **single fixed-width font**, size in
  **pt**, no `text-transform`, **character rules** (`-` / `=`) instead of `<hr>`,
  fixed column count, and set the printer driver to its **native/Draft font**
  rather than "Graphics" / "Print text as graphics".
- True standalone-app crispness requires **bypassing the browser rasteriser**:
  send **raw ESC/P text** to the printer. Chrome cannot raw-print; it needs a
  tiny local helper (watched-folder agent) or a text-only print queue.

Sources: escpos-php issue #1381; Printer Forums "HTML to dot-matrix in text
mode"; "Dot Matrix Printer Fonts and Plain Text Printing" guide;
`wagnerc4/dot-matrix-chrome-browser`; ESC/P (Wikipedia); Report Manager
dot-matrix output guide.

## 2. Strategy — two new print buttons, tested by Coop

Both the **Deposit** page and the **Payment** page (live + reprint) get **two
new print buttons** beside the existing one, so Coop can compare on real
stationery:

| Button | Track | Mechanism | Change size |
|---|---|---|---|
| **Print (Dot-Matrix)** | **A** | New JSF component + new stylesheet: pure monospace, pt-sized, character rules, no `capitalize`, no rasterised barcode. Prints through the normal browser dialog. | JSF + ConfigOptions |
| **Print (Raw Text)** | **B** | Server streams the same receipt as a `.prn` byte file (plain text, optional ESC/P codes). A small agent on the cashier PC raw-copies it to the LQ-310. No browser rasterisation at all. | Small controller method + text builder + wiki agent |

Track A ships and Coop tests. If A on real stationery is still not crisp enough,
B is already wired next to it for immediate comparison. If neither suffices, a
full local print-agent service is the next escalation — out of scope here.

The existing **Print** button and all other paper formats (POS, A4, 5×5 Custom
3) are untouched.

## 3. Scope

### In scope

- `src/main/webapp/inward/inward_bill_deposit.xhtml` — add 2 buttons + Dot-Matrix preview group
- `src/main/webapp/inward/inward_bill_payment.xhtml` — add 2 buttons + Dot-Matrix preview group
- `src/main/webapp/inward/inward_reprint_bill_deposit.xhtml` — add 2 buttons (renders `FiveFivePaymentBill` today)
- `src/main/webapp/inward/inward_reprint_bill_payment.xhtml` — add 2 buttons

### Out of scope (revisit after Coop's test)

Post-final-payment receipt, cancellation receipt, OPD / pharmacy 5×5, A4, POS.
No changes to `Bill`, existing components, `opd_five_five.css`, `PaperType`, DTOs.

## 4. Track A — `Print (Dot-Matrix)`

### New component: `resources/inward/bill/payment/FiveFiveDotMatrixPaymentBill.xhtml`

`cc:interface`:

- `bill : com.divudi.core.entity.Bill`
- `duplicate : java.lang.Boolean`
- `heading : java.lang.String` — default `"Deposit Receipt"`; Payment page passes `"Payment Receipt"`

Rendering rules:

- **One font only:** `font-family: "Courier New", "Courier", monospace;` — no
  Arial/Verdana/sans-serif anywhere, no per-element font override.
- **Sizes in pt:** body `12pt`, heading `13pt` bold. `line-height: 1`.
  No `%`, no `px`.
- **No `text-transform`.** Emit data in its stored case.
- **Rules are literal character runs** in the markup
  (`<div class="dmx-rule">========================================</div>`),
  width-matched to the column count — not `<hr>`, not CSS `::before content`.
- **Fixed 40-column monospace grid** (~4 in at 10 CPI, inside the 5-in form).
  Two-column label/value rows aligned on character boundaries using `ch` units
  (or a single `<pre>`-style block). No nested `<table>` with `px` spacer cells.
- **Fields:** Admission Type, Name, Age, Gender, Address, Phone, BHT No, Bill No,
  Bill Date, Bill Time, Payment method; `Paying Amount` line; MultiplePayment
  sub-rows as monospace lines (no bordered cells); Comment line (same
  `Show Comment on Inward Deposit Bill` guard as today); `Cashier : <name>` as
  plain text (no `overline`); footer from
  `sessionController.userPreference.pharmacyBillFooter`.
- **Barcode:** omitted by default. Rendered only if ConfigOption
  `Print Barcode on Inward Dot Matrix Receipt` (bool, default `false`) is true.

### New stylesheet: `resources/css/five_five_dotmatrix.css`

- Both `@media print` and `@media screen` blocks. Screen preview ~4 in wide,
  light border.
- All selectors prefixed `dmx-` / `.dmxbill` so nothing leaks into other 5×5
  bills.

### ConfigOptions (Track A)

Resolved **per-department-first**, same as everywhere else in HMIS.

| Key | Type | Default | Purpose |
|---|---|---|---|
| `Inward Payment Bill Dot Matrix Paper` | boolean | `false` | If true, the Dot-Matrix preview group is the default preview on the page (button still available regardless). |
| `Inward Dot Matrix Receipt Preprinted Stationery` | boolean | `false` | `false` → component prints its own text header (`department.printingName`, address, `telephone1/2`, fax) for blank continuous paper. `true` → header suppressed; emit N blank leading lines so the body prints **below** the pre-printed letterhead. |
| `Inward Dot Matrix Receipt Top Margin Lines` | integer | `8` | N blank leading lines when `Preprinted Stationery` is true. |
| `Print Barcode on Inward Dot Matrix Receipt` | boolean | `false` | Render the (rasterised) barcode. Off for LQ sites. |

### Page wiring

- New `h:panelGroup id="gpBillPreviewDmx"` inside the existing preview area,
  containing `FiveFiveDotMatrixPaymentBill`. Hidden (`display:none`) unless it is
  the configured default, but always present in the DOM.
- New button **Print (Dot-Matrix)**: `ajax="false"`, `action="#"`, with
  `<p:printer target="gpBillPreviewDmx" />` — mirrors the existing Print button's
  `p:printer target="gpBillPreview"` pattern.
- Deposit page passes `heading="Deposit Receipt"`; Payment page passes
  `heading="Payment Receipt"`; reprint pages pass `duplicate="true"`.

### Driver configuration (goes in the wiki, Track A section)

- Install the LQ-310 with the **Epson LQ-310 ESC/P2** driver (not a generic
  text driver).
- Printer Properties → Preferences → Advanced → **Print Quality = "Draft"** or
  **"LQ (NLQ)"**, not "Photo/Best".
- **"Print Text as Graphics" = OFF** (driver-dependent label: "Send TrueType as
  Bitmap = No" / "Print Mode = Native").
- Paper size = custom **5 in × 5 in** (or the site's real form length);
  Source = **Tractor / Continuous**.
- Chrome print dialog: **Margins = None**, **Scale = 100 (Default)**,
  **Headers and footers = OFF**, **Background graphics = OFF**.
- Save these as the printer's defaults and pin it as a named Chrome destination
  so cashiers do not re-pick per print.

## 5. Track B — `Print (Raw Text)`

### Server side

No new REST resource, no `ApplicationConfig` change. Add a method to
`InwardDepositController` and `InwardPaymentController` (or one shared
`@Named` helper `InwardReceiptTextController`) that:

1. Takes the current `Bill`.
2. Builds a **40-column plain-text** rendering (same fields as Track A).
3. If `Inward Raw Text Receipt Emit ESC/P Codes` (bool, default `true`):
   wrap with `ESC @` (init), `ESC x 0` (draft) / `ESC x 1` (LQ), `ESC P`
   (10 CPI), trailing form feed `\f`. When `Inward Raw Text Receipt
   Preprinted Stationery` is `false`, include the text header; when `true`,
   emit `Inward Raw Text Receipt Top Margin Lines` blank lines first.
4. Streams via `FacesContext` → `HttpServletResponse`:
   - `Content-Type: application/octet-stream`
   - `Content-Disposition: attachment; filename="inward-deposit-<billNo>.prn"`
     (`inward-payment-<billNo>.prn` on the Payment page)
   - body bytes, then `facesContext.responseComplete()`.

   Reuses the streaming pattern already in `ExcelController` / `PdfController`.

### ConfigOptions (Track B)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `Inward Raw Text Receipt Preprinted Stationery` | boolean | `false` | Mirror of the Track A stationery option. |
| `Inward Raw Text Receipt Top Margin Lines` | integer | `8` | Blank leading lines for pre-printed stationery. |
| `Inward Raw Text Receipt Emit ESC/P Codes` | boolean | `true` | If a site's transport mangles 8-bit control bytes, set `false` for pure ASCII. |

### Button

**Print (Raw Text)** beside the other two. `ajax="false"`, `action` returns
`null`, calls the stream method. Browser downloads `inward-deposit-<n>.prn`.

### Client machine — raw-print agent (delivered to the on-site person)

The `.prn` must reach the LQ-310 as **raw bytes**. Wiki documents three options,
simplest first:

1. **Manual (proof of concept).**
   - Map the printer to a port: `net use LPT1: \\localhost\LQ310 /persistent:yes`
     (or use a real LPT1).
   - `COPY /B "%USERPROFILE%\Downloads\inward-deposit-123.prn" LPT1:` — prints
     immediately, crisp, no rasterisation. Confirms B's quality before automating.

2. **Watched-folder agent (recommended for rollout).** PowerShell script on the
   cashier PC:
   - `FileSystemWatcher` on a folder (`C:\hmis-print\` or Chrome's download dir).
   - On `Created` for `inward-*.prn`: `cmd /c copy /b "<file>" \\localhost\LQ310`
     (or write to `\\.\LPT1`), then delete the file.
   - Config block at top: watch folder, printer share / LPT port, filename glob.
   - Runs via Task Scheduler, trigger "At log on".
   - Shipped verbatim in the wiki **and** committed to the PR as
     `developer_docs/printing/raw-text-print-agent.ps1`.

3. **Hands-off:** set Chrome `Settings → Downloads → Location` to the watched
   folder and disable "Ask where to save each file". Then option 2 is fully
   automatic: click → file lands → agent prints → file deleted.

Wiki troubleshooting: nothing prints; prints half a page then form-feeds;
control codes printed literally (→ set `Emit ESC/P Codes = false`); how to find
the printer share name; Task Scheduler setup.

## 6. Documentation

New wiki page `hmis.wiki/Dot-Matrix-Printing-for-Inward-Deposit-and-Payment-Receipts.md`:

- What the two new buttons are and when to use each.
- Track A: driver + Chrome settings (§4 above).
- Track B: all three client-machine agent options, the PowerShell script inline,
  Task Scheduler steps, troubleshooting.
- Linked from the PR description.

`developer_docs/printing/raw-text-print-agent.ps1` committed alongside the code.

## 7. Testing

- **JSF/CSS (Track A):** visual check of the Dot-Matrix preview group for
  Deposit and Payment (live + reprint); confirm monospace, character rules, no
  `capitalize`, header suppressed when `Preprinted Stationery = true` with N
  blank lines. No compilation needed for the pure-XHTML parts.
- **Track B (Java):** compile; unit-test the text builder against a sample `Bill`
  (column width = 40, ESC/P prologue present/absent per ConfigOption, header
  present/absent per stationery option); verify the servlet stream sets
  `application/octet-stream` + `Content-Disposition` and calls
  `responseComplete()`.
- **End-to-end (Playwright, per playwright-e2e skill):** log in → select
  Department → menu to Inward → Deposit; create a deposit; click each of the
  three print buttons; for Track A confirm the print preview renders the
  monospace receipt; for Track B confirm a `.prn` downloads and its bytes match
  the expected layout. Repeat on the Payment page. Record the menu path in the
  PR.
- **Coop acceptance:** Coop prints real deposits + payments on their LQ-310 with
  their pre-printed stationery using both new buttons and reports which is
  acceptable.

## 8. Rollout

- All new ConfigOptions default to today's behaviour (`false` / off), so no
  hospital changes until an admin opts in per department.
- Coop enables `Inward Dot Matrix Receipt Preprinted Stationery = true` and tunes
  `Top Margin Lines` for their form.
- No `master` involvement; branch from `origin/development`; PR targets
  `development`.
