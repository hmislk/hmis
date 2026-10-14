# Playwright E2E — Screenshots, printing, exports, styling checks

Part of the [Playwright E2E Workflow](../playwright-e2e-workflow.md). Read only the section you need.

- [43. Clicking a `p:printer` button hangs the whole browser session — verify with print-media emulation instead](#43-clicking-a-pprinter-button-hangs-the-whole-browser-session--verify-with-print-media-emulation-instead)
- [83. A page-local `<style>` rule can silently lose to the PrimeFaces theme — verify with a computed-style probe, not a screenshot](#83-a-page-local-style-rule-can-silently-lose-to-the-primefaces-theme--verify-with-a-computed-style-probe-not-a-screenshot)
- [84. A markup-less PrimeFaces component (`p:defaultCommand`, `p:focus`, …) cannot be confirmed by searching the rendered HTML — look for its event handler instead](#84-a-markup-less-primefaces-component-pdefaultcommand-pfocus--cannot-be-confirmed-by-searching-the-rendered-html--look-for-its-event-handler-instead)
- [110. A print receipt rendering completely blank can mean the department's paper-type preference isn't one the page checks — not a broken query](#110-a-print-receipt-rendering-completely-blank-can-mean-the-departments-paper-type-preference-isnt-one-the-page-checks--not-a-broken-query)
- [115. Element screenshots land on the wrong region — crop the viewport shot instead](#115-element-screenshots-land-on-the-wrong-region--crop-the-viewport-shot-instead)
- [120. Verifying a `p:fileDownload` export: POST the form with `fetch` and decode locally](#120-verifying-a-pfiledownload-export-post-the-form-with-fetch-and-decode-locally)
- [133. Redacted print evidence on a restored-production DB: clone only the rows you need into a fixed overlay, then screenshot that](#133-redacted-print-evidence-on-a-restored-production-db-clone-only-the-rows-you-need-into-a-fixed-overlay-then-screenshot-that)

---

## 43. Clicking a `p:printer` button hangs the whole browser session — verify with print-media emulation instead

`p:printer` calls `window.print()`, which opens a real native OS print dialog.
In a Playwright-driven session this dialog blocks not just the click (which
times out and gets moved to a background task) but **every subsequent tool
call on that browser** — `browser_tabs list/new/close` all hang too, because
the dialog is modal at the OS/browser-process level, not a JS `confirm()`
that `browser_handle_dialog` can intercept. The only recovery is asking the
human operator to manually dismiss the dialog in the actual browser window.

**Don't click the Print button to verify print CSS.** Instead, emulate print
media on the existing page and screenshot that — `p:printer` clones the
current document's `<head>` (including inline `<style>` blocks and linked
stylesheets) into its print iframe, so `@media print` rules apply identically
whether triggered by the real dialog or by emulation:

```js
async (page) => { await page.emulateMedia({ media: 'print' }); }
```

Then `browser_take_screenshot` — this shows exactly what would print (hidden
`.noPrintButton` elements, `.printOnlyReport` toggled visible, etc.) without
ever touching `window.print()`. Verified while fixing issue #22316 (Time
Service Report print truncation).

**Scope of this check**: this only proves `@media print` visibility/layout
rules apply correctly — it does not verify pagination, page-fit, or page
breaks across multiple printed pages. For reports where those matter, follow
up with an actual PDF export or a manual print-preview pass.


## 83. A page-local `<style>` rule can silently lose to the PrimeFaces theme — verify with a computed-style probe, not a screenshot

A page-local `<style>` block that targets a PrimeFaces sub-part (title bar, row,
header cell) can look plausible in review and still never apply, because the
Material theme qualifies the same part with a leading element selector:

```css
/* theme.css — specificity (0,2,1): two classes PLUS the `body` element */
body .ui-panel .ui-panel-titlebar { background: #f8f9fa; }

/* page-local — specificity (0,2,0): loses, even though it comes later */
.my-highlight > .ui-panel-titlebar { background: rgba(13,110,253,.10); }
```

Later-in-document does **not** save you here: the theme wins on specificity, so
the rule is simply discarded. Adding the same `body` + owning-class prefix
restores the win:

```css
body .ui-panel.my-highlight > .ui-panel-titlebar { background: rgba(13,110,253,.10); }
```

The trap is that a *partial* application looks like success — an outer `border`
on the panel itself can land (nothing in the theme sets it) while the title-bar
`background` on the very next line is dropped, so the screenshot shows "some
highlight" and the defect passes review.

Don't judge this from a screenshot. Read the computed style, and — when the rule
is print-scoped — read it under emulated print media (see §43; never click the
real `p:printer` button):

```js
async (page) => {
  const probe = async () => await page.evaluate(() => {
    const bar = document.querySelector('body .ui-panel.my-highlight > .ui-panel-titlebar');
    return getComputedStyle(bar).backgroundColor;
  });
  await page.emulateMedia({ media: 'screen' }); const onScreen = await probe();
  await page.emulateMedia({ media: 'print'  }); const onPrint  = await probe();
  await page.emulateMedia({ media: 'screen' });
  return { onScreen, onPrint };
}
```

To find *which* rule actually won, enumerate `document.styleSheets` for rules the
element `matches()` and print each one's `selectorText` plus originating
stylesheet — that names the offending theme selector directly, instead of
guessing at specificity.

Found on `inward/pharmacy_bill_return_bht_issue.xhtml` while adding the Return
Bill Preview highlight (issue #23338).


## 84. A markup-less PrimeFaces component (`p:defaultCommand`, `p:focus`, …) cannot be confirmed by searching the rendered HTML — look for its event handler instead

These components emit no DOM element at all, only a `PrimeFaces.cw(...)` init
script, and PrimeFaces removes inline scripts from the document once they have
run. So `document.documentElement.innerHTML.includes('DefaultCommand')` returns
`false` on a page where the component is present and working — an easy false
negative that looks like "my fix didn't deploy".

Confirm it the way the widget actually manifests:

```js
// the widget object (its key is widget_<clientId>, name mangled by minification)
Object.keys(PrimeFaces.widgets);
// what p:defaultCommand really does: a namespaced keydown handler on the form
jQuery._data(document.getElementById('form'), 'events').keydown.map(h => h.namespace);
// -> ["form:j_idt579"]  ← the defaultCommand's own client id
```

Then assert the *behaviour* (press Enter, check the URL didn't change and the
intended action ran), which is the only proof that matters anyway. Found while
verifying issue #23342.


## 110. A print receipt rendering completely blank can mean the department's paper-type preference isn't one the page checks — not a broken query

While verifying issue #23571's new Appointment Deposit refund receipt on
`inward_view_appointment_bill_receipt.xhtml`, the `billTypeAtomic`-keyed
`h:panelGroup` branch matched correctly (confirmed with a temporary debug
`h:outputText` dumping the DTO fields) and the DB row was correct, but the
receipt panel rendered as a completely empty `<span>` — no error, no
exception in `server.log`.

The cause: this page's three paper-type branches only check
`'Inward Payment Bill Five Five Paper'`, `'... A4 Paper'`, and
`'... POS Paper'`. The department's actual `ChangeReceiptPrintingPaperTypes`
Settings dialog (opened via the page's own "Settings" button) showed a
**fourth** option, "5×5 Custom 3 Paper", was the one actually enabled for
that department — a paper type this particular page's code has never
checked. All three of the page's `getBooleanValueByKey` calls legitimately
evaluated `false`, so nothing rendered — this is not a bug in the routing or
DTO logic, it is a pre-existing gap between the Settings dialog's options and
the page's own `rendered` conditions.

**Before concluding a receipt panel is broken because it renders blank**,
open the page's own "Settings" button/dialog (per §26, never raw SQL) and
check which paper type is actually enabled for the current department
against the exact set of paper-type checks the page's `rendered` attributes
test — a Settings dialog can offer an option the page doesn't (yet) handle.
For local verification of the remaining receipt rendering, enable one of the
paper types the page checks, such as "POS Paper". This does not validate
`5×5 Custom 3 Paper`; test or fix that unsupported configuration separately.

Found while fixing issue #23571.


## 115. Element screenshots land on the wrong region — crop the viewport shot instead

`browser_take_screenshot` with an `element`/`target` repeatedly captured the
wrong band of the page on inward billing screens (blank, or the footer instead
of the table). The pages have a sticky header and the browser runs at a device
pixel ratio > 1, and the element-clip path does not agree with the rendered
offsets.

What works reliably: size the viewport wide enough for the whole table, scroll
the target into view, take a plain **viewport** screenshot, then crop it:

```python
from PIL import Image
im = Image.open('tmp/<issue>/_full.png')
im.crop((0, top, im.size[0], bottom)).save('tmp/<issue>/<name>.png')
```

Cropping is also how you strip patient identifiers before anything reaches the
wiki — a full-page inward screenshot carries name, DOB, phone, NIC and
consultant in the Patient Details panel, none of which may be published.


## 120. Verifying a `p:fileDownload` export: POST the form with `fetch` and decode locally

Clicking a `p:fileDownload` button in Playwright kills the MCP session — the browser
starts a native download the driver never returns from. So an Excel/PDF export can't be
verified by clicking it, and "the numbers are right on screen" is not evidence the
export is right: the on-screen footer and the export read different getters, which is
exactly how issue #23676 shipped (the screen had no Gross/WHT footer at all while
`ExcelController` null-guarded `bundle.getGrossTotal()` to `0.0` and `PdfController`
printed the literal `null`).

Submit the same POST the button would, from inside the page, and hand the bytes back as
base64. Generate the report first — these exports read the `bundle` already in session:

```js
async () => {
  const form = document.getElementById('<formId>');
  const fd = new FormData(form);
  fd.append('<buttonId>', '<buttonId>');          // the p:commandButton's own name/value
  const res = await fetch(form.action, { method: 'POST', body: fd, credentials: 'same-origin' });
  const bytes = new Uint8Array(await res.arrayBuffer());
  let bin = ''; for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]);
  return JSON.stringify({ status: res.status, ct: res.headers.get('content-type'),
                          cd: res.headers.get('content-disposition'), len: bytes.length,
                          b64: btoa(bin) });
}
```

Pass `filename:` to `browser_evaluate` so the base64 goes to a file instead of the
transcript, then decode it:

```powershell
$o = Get-Content <result>.json -Raw | ConvertFrom-Json
[IO.File]::WriteAllBytes("tmp\export.xlsx", [Convert]::FromBase64String($o.b64))
```

`Content-Disposition` also proves the filename logic (date range, report name) that no
screenshot shows.

**Reading the file back:**

- **`.xlsx`** — `Expand-Archive` refuses the extension, so copy to `.zip` first, then
  read `xl/worksheets/sheet1.xml`. Numeric cells hold raw values, so the totals row is
  directly assertable without resolving `sharedStrings.xml`:
  ```powershell
  Copy-Item export.xlsx export.zip; Expand-Archive export.zip -DestinationPath ex -Force
  [regex]::Matches((Get-Content ex\xl\worksheets\sheet1.xml -Raw), '<row[^>]*>.*?</row>') |
    Select-Object -Last 1 | ForEach-Object { $_.Value }
  ```
- **`.pdf`** — `Read` needs poppler, which the dev box doesn't have. iText streams are
  zlib-deflated: walk each `stream`…`endstream`, skip the 2-byte zlib header, run it
  through `DeflateStream`, and pull the text out of the `(…)` literals. That recovers
  the whole table including the totals row, which is enough to assert on.

Both are read-only and touch no local state, so they're safe to repeat.


## 133. Redacted print evidence on a restored-production DB: clone only the rows you need into a fixed overlay, then screenshot that

Local databases restored from production (e.g. the local `coop` copy) hold **real** patient and doctor names,
so a full-page screenshot of a final-bill print can never be published as-is (see §8 and the
GitHub public-content policy). Element screenshots of the print table are also unreliable on these pages:
the print area sits inside a scrolled container and the page runs at `devicePixelRatio` 0.75, so
`getBoundingClientRect()` coordinates do not map onto the captured image and crops land on the wrong rows.

What works (issue #24085): in `browser_evaluate`, clone just the rows you need (e.g. *Description* through
*Total*) into a new `position:fixed; top:0; left:0; z-index:99999` container, replace any names in the
clone's text nodes with `[Doctor name redacted]`, then `browser_take_screenshot` with `target` set to that
container. The live page is untouched server-side — the clone is client-only and vanishes on the next
navigation. Trim any page bleed around the clone afterwards (a PIL crop to the print's background colour is
enough).

This is not specific to print pages. On the Inpatient Dashboard (`admission_profile.xhtml`), too, element
screenshots (`browser_take_screenshot` with `target`) of a banner/alert land on the wrong region at
`devicePixelRatio` 0.75, and each miss captured the **Patient Details** panel with a real patient name
(issue #24150). Manual crops of a viewport screenshot using `getBoundingClientRect()` coordinates missed too.
Use the fixed-overlay clone above for *any* element you want as evidence on these pages, and check each
image before it leaves `tmp/`.

Related: a PrimeFaces `p:toggleSwitch` (e.g. the **FOC** switch on *Add Professional Fee*) ignores clicks on
its hidden `<input>`; click its `.ui-toggleswitch-slider` child instead, then confirm with
`document.getElementById('<id>_input').checked`.
