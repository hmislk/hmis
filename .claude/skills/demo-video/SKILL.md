---
name: demo-video
description: >
  Use when asked to make a demo, training, how-to or tutorial video with sound or
  voice-over showing an HMIS function or configuration (e.g. "make a video showing
  how to enable X", "record a demo of Y for users", "Sinhala/Tamil narrated
  walkthrough"), to publish such a video to YouTube, or via `/demo-video`. Not for bug demonstrations (use
  demonstrate-issues) or plain E2E verification (use playwright-e2e).
---

# Demo Video

Narrated MP4 demos of real HMIS screens. Playwright drives the running app **through the menus only**, an injected overlay adds a cursor, click highlights and captions, edge-tts voices each step, and ffmpeg mixes it all. Everything comes from a script, so when the UI changes you re-run the video instead of re-recording it.

Works on both Windows and Linux (verified on Ubuntu). See the Linux notes under **Work dir** and **Credentials** below — the only differences are the one-time pip setup and how credential paths are supplied; the recording/build/upload steps themselves are identical.

Worked examples:
- `examples/pharmacy-issue-own-department/` — the first accepted video; normal case, login happens silently before narration starts.
- `examples/login-department-select/` — a video *about* logging in itself, using `manualLogin: true` (see Recorder API below).
- `examples/pvc-card-print/` — a video whose key moment is a **print button** (see Printing below).
- `examples/lab-two-page-reports/` — a long (about 5 min) configuration-plus-data-entry story. `setup()` resets a config option and a report format and bills a fresh test patient through the menus (one new `PT` name per take); the flow then enables the option, edits the format designer, and enters results. Shows `exact()` text locators, retrying accordion headers, and picking result boxes by screen order.
- `examples/inward-raw-text-paper-type/` — a button that **downloads a file** (`.prn`): catch it with `p.waitForEvent('download')`, then show its contents on screen. Also shows an unrecorded `setup()` and a `restore.js` that put a department setting back as found.

## Printing (print buttons, receipts, cards)

Chrome's print dialog is browser UI, not page content: Playwright's video never contains it, a headless recorder can't open it, and on this machine the shell can't screenshot the desktop either. Show *what gets printed* instead, with `scripts/printcap.js`:

1. `await pc.install(r.page)` at the start of `flow`. It makes `print()` and `document.execCommand('print')` (which PrimeFaces `p:printer` tries first) record the printed document instead of opening the dialog.
2. After clicking the print button: `html = await pc.waitForPrint(page, n)` (n = 1-based print count).
3. `pdf = await pc.toPdf(page.context(), HMIS_BASE, html, 'x.pdf')` renders that exact HTML with Chrome's print engine at the CSS `@page` size, then `{ png, pages } = pc.toPng(pdf)` (needs `pip install pymupdf`).
4. `pc.showPrinted(page, png, 'Printed front')` overlays it on camera; `pc.hidePrinted(page)` removes it.
5. Narrate the dialog settings (printer, Margins, Scale) in a caption-only step.

Assert `pages === 1` (or whatever the paper needs): the PDF is real verification, not decoration. The first take of the PVC card video exposed that every card side printed as 2 pages (#24305). If the user wants the real dialog in the video, they must screenshot it themselves (PrtScn) and you show the image as a still the same way.

## Rules

- **Only open the application root URL.** Reach every page through the menus, exactly as a user would. If a page has no menu path, stop: the missing menu path is the finding. File an issue and fix it before recording (see CLAUDE.md Testing rules).
- **Record on local or QA, never production.** Everything on screen is published. Use test departments and test patients only.
- **A named test environment needs no questions.** When the user names local, QA or a staging box (`rh-local-staging` = Ruhunu's own server, `home-qa*`), go straight there. Do not ask which environment, whether you may create fictional test data (patient, admission, bill, template, insurer contact) or edit the config the story needs, or whether to blur names. Just do it, blur real names, and list what you created in the delivery message. Ask only for what you cannot find: the URL and login. Look in `C:\Credentials\` first; if the user pastes a login, save it to a file there (never into the repo) and use it from the flow via `process.env.HMIS_USER`/`HMIS_PASS`. Stop and ask only if the target looks like production.
- **Create missing test data through the menus.** If the environment has no suitable record (for example no finalised insurer admission), build one yourself, following the recipe in [Playwright E2E §143](../../../developer_docs/testing/playwright-e2e/module-pages.md#143-creating-an-inward-test-admission-with-a-finalised-insurer-bill-on-a-staging-box). Only publishing to YouTube waits for the user's confirmation of the dry-run metadata.
- **Every step of the storyline must really happen.** If the flow shows "blocked by default", make `setup()` put the configuration back to its default in an unrecorded browser session first. Never fake a screen state.
- **Look at the output before handing it over** (`check_frames.py`). Green scripts are not proof that the video shows the right thing.

## Workflow

1. **Storyline.** Agree the before → change → after story and the menu path with the user. Then walk the path once with plain Playwright (dump element ids, take screenshots) to find selectors and confirm every step is reachable.
2. **Work dir.** `tmp/demo-videos/<slug>/` (gitignored). One-time setup in `tmp/demo-videos/`:
   `npm init -y && npm i playwright`, then `NODE_OPTIONS=--dns-result-order=ipv4first npx playwright install ffmpeg`, then `python -m pip install edge-tts imageio-ffmpeg`.
   The recorder launches the **installed Google Chrome** (`channel: 'chrome'`), because Playwright's own Chromium download fails on this network. Chrome must be installed; there is no `npx playwright install chromium` step.
   **On Debian/Ubuntu**, the plain `pip install` fails with `externally-managed-environment` (PEP 668). Use `python -m pip install --user --break-system-packages edge-tts imageio-ffmpeg` instead (a venv also works, but needs `python3-venv` installed first, which most boxes don't have by default).
3. **`script.json`.** Write one `{id, say}` per step, one or two short sentences each. Name menus and buttons exactly as they are labelled on screen. For a Sinhala or Tamil version, reuse the same ids and `flow.js`, translate only the `say` text, and keep the on-screen labels in English exactly as the app shows them ("Pharmacy මෙනුව විවෘත කර, Consumption ක්ලික් කරන්න"). Ask for a native speaker's review before the video goes to users.
4. **Voice.** Run `python <skill>/scripts/tts.py [voice]`. The default voice is `en-GB-SoniaNeural`. For Sinhala use `si-LK-ThiliniNeural`; for Tamil use `ta-LK-SaranyaNeural`.
5. **`flow.js`.** Copy the example and edit it. Each step is `r.step(id)` → actions → `r.done()`, and `r.done()` waits until that step's narration has finished. Add a `throw` wherever the result must be true (e.g. the block message appears, the screen opens), so a wrong recording fails loudly.
6. **Record.** Run `HMIS_BASE=http://localhost:8080/rh/ DEPT="<login dept>" node flow.js`. It exits non-zero if a `throw` fires. Don't pipe it (e.g. `| tail`), because the pipe hides the failure and `build.py` would then build a broken video.
7. **Build.** Run `python <skill>/scripts/build.py <slug>.mp4`.
8. **Check.** Run `python <skill>/scripts/check_frames.py <slug>.mp4` and read `frames/sheet.png`. Every caption must match what is on screen at that moment. Fix the flow and repeat steps 6–8 until it does.
9. **Deliver.** Save the MP4 in `tmp/demo-videos/` and report its path. Say which environment and test data appear in it.
10. **Publish to YouTube (optional, only when asked).** HMIS is open source and these videos go out **public** on the channel the token belongs to. Write `youtube.json` in the work dir (`title` of at most 100 characters, plus optional `tags`, `privacy`, `language` such as `si`/`ta`, `playlistId` and `description`, which defaults to the `script.json` narration). Run `python <skill>/scripts/upload.py <slug>.mp4 --dry-run`, show the user the metadata, and upload **only after they confirm**: `python <skill>/scripts/upload.py <slug>.mp4`. The script writes `youtube-upload.json`, which blocks a second upload of the same video unless you pass `--force`. Report the `youtu.be` link. The one-time setup is below.

11. **Update the wiki Video Library (whenever a video is uploaded or replaced).** The wiki keeps one page, [Video Library](https://github.com/hmislk/hmis/wiki/Video-Library), with every video grouped by module (same order as the Wiki Article Index). A new recording of a topic **replaces** its old link; it never adds a second row for the same topic and language. From the repo root run:
    `python .claude/skills/demo-video/scripts/wiki_video_link.py --module "<Index heading, e.g. Laboratory (LIMS)>" --topic "<video topic>" --url <youtu.be link> --lang English --audience "<who>" --length "about 3 min" --article <Wiki-Article-Name>`
    It replaces the row with the same topic and language (new link, new Updated date) or adds one under the module heading, and swaps the old video link in the written article for the new one, or adds a **Video:** line under the article's title when it has none. Use `--dry-run` first to see what it will do. Then commit and push `../hmis.wiki` in one commit and put the link in the delivery message. For a Sinhala or Tamil version, run it again with `--lang Sinhala` (a separate row) and leave out `--article` when the English article is reused; pass `--article` only for a distinct translated article. Without a YouTube upload (a file-only delivery), skip this step.

## YouTube publishing setup (one time)

Credentials live outside the repo in `C:/Credentials/youtube/` on Windows (override with `YT_CREDENTIALS_DIR`). **On Linux there is no default path** — always set `YT_CREDENTIALS_DIR` (e.g. `/home/<user>/Credentials/youtube`). Use `pip install google-api-python-client google-auth-oauthlib` (add `--user --break-system-packages` on Debian/Ubuntu).

1. In Google Cloud Console (as the Google account that owns or manages the channel), create a project and enable **YouTube Data API v3**.
2. Under **Google Auth Platform**, create the branding: app name, support email, audience **External**, contact email. Accept the User Data Policy (ask the user to do this).
3. Under **Audience → Test users**, add the Google account. **Re-open the Audience page and confirm "1 user (1 test)"**: the side-panel Save can silently fail to stick.
4. Under **Clients → Create client → Desktop app**, click **Download JSON** and save it as `client_secret.json`. The secret cannot be shown again once the dialog closes, so find the file first. Chrome may save it to a non-default Downloads folder.
5. The first `upload.py` run opens the consent page. Pick the Google account; for a Brand Account channel such as CareCode, pick it on the channel chooser that follows. On "Google hasn't verified this app", click **Continue**. The script saves `token.json`. While the app is in Testing mode that token lasts about 7 days, after which the consent page simply reappears.

## Recorder API (`r` inside `flow`)

| Call | Does |
|---|---|
| `step(id, {keep})` / `done(extraMs)` | show the caption and start the clip (clears a pinned growl unless `keep:true`) / hold until the clip ends |
| `menu(top, item)` | hover a top menu, highlight the item, click it. `top` is `'Administration'` or an icon menu id suffix: `smPharmacy`, `smInpatient`, `smOpd`, `smLab`, `smStore`, `smReports`, `smSettings` |
| `click(loc, {nav})` | move the cursor, highlight, click. Use `nav:true` for `ajax="false"` buttons |
| `point(loc, ms)` | move the cursor and highlight without clicking, to draw attention |
| `highlight(loc, ms)` | red outline only, cursor stays put. Use it for the menu bar or anything that opens on hover |
| `type(loc, text)` / `autocomplete(text, pick)` | visible typing / pick from the first PrimeFaces autocomplete |
| `pinGrowl()` / `unpin()` | pin a styled copy of the growl; returns its text for asserting. To keep it through the explaining step, open that step with `step(id, {keep:true})` |

Environment variables: `VIEW` sets the window size. The default `1920x1080` is the zoom level users work at, where the whole top menu fits on one line; below about 1760 px wide it wraps to two lines. Captions scale with the width. Credentials come from `HMIS_USER`/`HMIS_PASS`, or otherwise from `C:/Credentials/hmis_web_login.txt` (Windows default). **On Linux there is no default path** — always set `HMIS_USER`/`HMIS_PASS` explicitly.

`run(chromium, { setup, flow, manualLogin })`: by default (`manualLogin` unset/`false`) the recorder logs in and picks `DEPT` silently before recording narration, and that portion is trimmed from the final video — this is right for a video *about* some other feature. Set `manualLogin: true` to skip that automatic step and have `flow` perform (and narrate) the login and department-selection itself, e.g. for a video that teaches login — see `examples/login-department-select/`. Read credentials inside `flow` with `creds()` (also exported from `recorder.js`), which resolves `HMIS_USER`/`HMIS_PASS` the same way the automatic login does.

## Common Mistakes

| Symptom | Fix |
|---|---|
| `npx playwright install ffmpeg` fails with "Download failure" | Prefix `NODE_OPTIONS=--dns-result-order=ipv4first`. IPv6 to Azure is black-holed on this machine |
| `pip install` fails with `externally-managed-environment` | Debian/Ubuntu (PEP 668). Add `--user --break-system-packages` |
| An error message is gone before the narration explains it | Call `pinGrowl()` right after the action that raises it, before `done()` |
| A datatable column filter finds nothing | PrimeFaces filters match from the start of the text. Type the beginning of the value |
| Department picker click times out | Its items are table rows (`tr`), not `li`. `login()` already handles this |
| Hover on `Administration` times out | Several menu items are labelled Administration. `topMenu()` targets the top-level entry |
| Edited XHTML not showing on local Payara | Production stage caches Facelets. Run `asadmin disable <app>` then `asadmin enable <app>` |
| Bash heredoc "unexpected EOF" when writing JS | Write files with the Write tool, not heredocs; the caption quotes break the shell |
| Red "Database Migration Pending" bar visible | The overlay hides it. If it still shows, it only appears on dev builds, not to users |
| Screen and voice drift apart | Never use fixed sleeps for pacing. Let `done()` pace each step from the clip duration |
| Consent says "Access blocked: … has not completed the Google verification process" (403 access_denied) | The account is not a saved test user. Add it under Audience → Test users and check the count shows it |
| Consent says "Something went wrong" after picking a Brand Account | Sign in as the managing Google account (the test user), then pick the channel from the chooser |
| Upload hangs, the log shows `WinError 10060` retries | IPv6 is black-holed. `upload.py` forces IPv4 by default; don't set `YT_ALLOW_IPV6` on this network |
| Consent page closed or lost while the script waits | The full URL is in the script's output. Open it again; the local listener keeps waiting |
| `quotaExceeded` | About 6 uploads per day on the default 10,000-unit quota. Retry after midnight Pacific time |
| Upload lands **private** although public was asked | Unaudited API projects may be locked to private. Change the visibility in Studio, or file the API audit form (link in the script output) |
| `r.click()` on a page accordion/tab header silently has no effect the first time (`aria-expanded` stays `false`, next step's target is hidden) | Read `aria-expanded` back after the click and click again if it didn't flip — some PrimeFaces accordion/tab headers on this app need two clicks right after page load |
| A nested flyout item (hover a top icon, then hover a second-level category, *then* click a third-level item) times out as "not visible" even though a plain `.isVisible()` check just said true | The default `r.click()`/`r.point()` mouse path travels from wherever the cursor currently is, and crossing other menu items on the way closes the flyout before the click lands. After the hover opens it, move the mouse in two short steps straight down the submenu column first (to just below the hovered header, then to the target's row — see `r.menu()`'s internal path in `recorder.js`) and only then call `r.click()` |
| `button:has-text("X")`/`:text-is("X")` resolves but the action times out "element is not visible" | On a busy admin page, several *other* hidden buttons (other accordion tabs, conditionally-rendered sections) can also match that text and sort earlier in DOM order than the one you want — `.first()` then grabs the wrong one. Dump all matches with `page.evaluate` (text + `offsetParent!==null` + `id`) to find the real one, then target it by its own id instead of by text |
| `has-text("Select All")` clicks "Unselect All"; `has-text("Handover")` hits "Select Payments to Handover" | `has-text` ignores case and matches any substring. Use an exact regex (`hasText: /^\s*Handover\s*$/`), a class (`.ui-button-success`), or scope it to the container (`td button`) |
| `tts.py` fails on every step with HTTP 403 | The voice service rejected an old `edge-tts`. Upgrade it: `python -m pip install -U edge-tts` (7.2.8 failed, 7.3.1 works) |
| A cashier/handover demo is blocked: "All collections must be selected" | The environment does not allow partial handovers (`Allow Handover with Unmatched Balance` is off on rh-local-staging). Use a new test user with a new shift so the handover holds only the demo payments. See [Playwright E2E §144](../../../developer_docs/testing/playwright-e2e/module-pages.md#144-setting-up-a-non-cash-settlement-test-payments-must-come-through-an-accepted-handover) |
| `button[title="…"]` locator works once, then times out | PrimeFaces tooltip strips `title` on hover. Locate by icon class (`button:has(.fa-pills)`) or `id` |
| `pinGrowl()` returns `''` although the growl is on screen | PrimeFaces adds the growl container before the text fades in. Fixed in `recorder.js` (waits for `.ui-growl-message`); if a save is slow, give the step `r.sleep(2500)` after `pinGrowl()` and open the next step with `{keep:true}` so the confirmation is readable |
| A captured print renders as 2+ PDF pages only while recording | The recorder overlay (`#__cap`, `#__cur`) is injected into every page of the recorded context, including the one `toPdf` opens. `printcap.toPdf` strips it; don't render captured HTML in a page without that step |
| The page's live preview scrolls out of view while editing lower fields | That is a UX defect for real users too — fix the page (e.g. `position:sticky` preview), don't film around it |
| Receipt/print page records as blank white | It calls `window.print()`. Before navigating: `await p.context().addInitScript(() => { window.print = () => {}; })` |
| Re-record fails "Insufficient stock" | Earlier takes drained the first batch. Pass its expiry as `pick`: `r.autocomplete('Panadeine', '31 May 28')` |
| Failed takes clutter the next take's lists | Never clean up. Use per-take names via env vars; for per-day lists (tokens), record in a department with none today |
| `tts.py` fails on one step with `CalledProcessError`, although the same text works on its own | The step id is a Windows reserved file name (`prn`, `con`, `aux`, `nul`, `com1`, `lpt1`), so `audio/<id>.mp3` can't be created. Rename the id |
| A button with `onclick="confirm(...)"` (e.g. Pay) does nothing | Playwright dismisses dialogs by default, so `confirm()` returns false. Add `p.on('dialog', d => d.accept())` at the start of `flow` |
| The real patient/doctor name shows on a test admission (consultant, referring doctor) | Point that field of the test record at a test staff member before recording, then `asadmin disable/enable` the app so the cache picks it up. Check every panel of every page in the flow |
| `r.type()` into a field that already has a value doubles the text on a re-take | `r.type` appends. `fill('')` the field first. Earlier takes persist (the insurer contact, for example), so every take must start from the same state |
| A rich-text (Quill) template saves its default text although `.ql-editor.innerHTML` was set | Quill ignores programmatic HTML. Click the editor, `Control+A`, `Delete`, then `keyboard.insertText(line)` and `Enter` per line |
| A `confirm()` button does nothing under the Playwright MCP although a dialog handler was registered | Run `window.confirm = () => true` in the page first, then read the real error message. A silently dismissed confirm hid three separate blockers in one run |
| The Search button, or other buttons, are missing on a page in one department | Privileges are per department. Log in again to a department with the full menu (on Ruhunu staging, "Third Floor") |
| A save shows no growl on a staging box | Do not assert on the growl. Assert on the downstream result (here, the generated letter) |
