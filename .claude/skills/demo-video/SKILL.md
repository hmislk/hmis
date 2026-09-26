---
name: demo-video
description: >
  Use when asked to make a demo, training, how-to or tutorial video with sound or
  voice-over showing an HMIS function or configuration (e.g. "make a video showing
  how to enable X", "record a demo of Y for users", "Sinhala/Tamil narrated
  walkthrough"), or via `/demo-video`. Not for bug demonstrations (use
  demonstrate-issues) or plain E2E verification (use playwright-e2e).
---

# Demo Video

Narrated MP4 demos of real HMIS screens. Playwright drives the running app **through the menus only**, an injected overlay adds a cursor, click highlights and captions, edge-tts voices each step, and ffmpeg mixes it all. Everything comes from a script, so when the UI changes you re-run the video instead of re-recording it.

Worked example (the first accepted video): `examples/pharmacy-issue-own-department/`.

## Rules

- **Only open the application root URL.** Reach every page through the menus, exactly as a user would. If a page has no menu path, stop: the missing menu path is the finding. File an issue and fix it before recording (see CLAUDE.md Testing rules).
- **Record on local or QA, never production.** Everything on screen is published. Use test departments and test patients only.
- **Every step of the storyline must really happen.** If the flow shows "blocked by default", make `setup()` put the configuration back to its default in an unrecorded browser session first. Never fake a screen state.
- **Look at the output before handing it over** (`check_frames.py`). Green scripts are not proof that the video shows the right thing.

## Workflow

1. **Storyline.** Agree the before → change → after story and the menu path with the user. Then walk the path once with plain Playwright (dump element ids, take screenshots) to find selectors and confirm every step is reachable.
2. **Work dir.** `tmp/demo-videos/<slug>/` (gitignored). One-time setup in `tmp/demo-videos/`:
   `npm init -y && npm i playwright`, then `NODE_OPTIONS=--dns-result-order=ipv4first npx playwright install ffmpeg`, then `python -m pip install edge-tts imageio-ffmpeg`.
   The recorder launches the **installed Google Chrome** (`channel: 'chrome'`), because Playwright's own Chromium download fails on this network. Chrome must be installed; there is no `npx playwright install chromium` step.
3. **`script.json`.** Write one `{id, say}` per step, one or two short sentences each. Name menus and buttons exactly as they are labelled on screen. For a Sinhala or Tamil version, reuse the same ids and `flow.js`, translate only the `say` text, and keep the on-screen labels in English exactly as the app shows them ("Pharmacy මෙනුව විවෘත කර, Consumption ක්ලික් කරන්න"). Ask for a native speaker's review before the video goes to users.
4. **Voice.** Run `python <skill>/scripts/tts.py [voice]`. The default voice is `en-GB-SoniaNeural`. For Sinhala use `si-LK-ThiliniNeural`; for Tamil use `ta-LK-SaranyaNeural`.
5. **`flow.js`.** Copy the example and edit it. Each step is `r.step(id)` → actions → `r.done()`, and `r.done()` waits until that step's narration has finished. Add a `throw` wherever the result must be true (e.g. the block message appears, the screen opens), so a wrong recording fails loudly.
6. **Record.** Run `HMIS_BASE=http://localhost:8080/rh/ DEPT="<login dept>" node flow.js`. It exits non-zero if a `throw` fires. Don't pipe it (e.g. `| tail`), because the pipe hides the failure and `build.py` would then build a broken video.
7. **Build.** Run `python <skill>/scripts/build.py <slug>.mp4`.
8. **Check.** Run `python <skill>/scripts/check_frames.py <slug>.mp4` and read `frames/sheet.png`. Every caption must match what is on screen at that moment. Fix the flow and repeat steps 6–8 until it does.
9. **Deliver.** Save the MP4 in `tmp/demo-videos/` and report its path. Say which environment and test data appear in it.

## Recorder API (`r` inside `flow`)

| Call | Does |
|---|---|
| `step(id, {keep})` / `done(extraMs)` | show the caption and start the clip (clears a pinned growl unless `keep:true`) / hold until the clip ends |
| `menu(top, item)` | hover a top menu, highlight the item, click it. `top` is `'Administration'` or an icon menu id suffix: `smPharmacy`, `smInpatient`, `smOpd`, `smLab`, `smStore`, `smReports`, `smSettings` |
| `click(loc, {nav})` | move the cursor, highlight, click. Use `nav:true` for `ajax="false"` buttons |
| `point(loc, ms)` | move the cursor and highlight without clicking, to draw attention |
| `type(loc, text)` / `autocomplete(text, pick)` | visible typing / pick from the first PrimeFaces autocomplete |
| `pinGrowl()` / `unpin()` | pin a styled copy of the growl; returns its text for asserting. To keep it through the explaining step, open that step with `step(id, {keep:true})` |

Environment variables: `VIEW` sets the window size (default `1600x900`; the user asked for this zoomed-out look, and `1366x768` is too cramped). Credentials come from `HMIS_USER`/`HMIS_PASS`, or otherwise from `C:/Credentials/hmis_web_login.txt`.

## Common Mistakes

| Symptom | Fix |
|---|---|
| `npx playwright install ffmpeg` fails with "Download failure" | Prefix `NODE_OPTIONS=--dns-result-order=ipv4first`. IPv6 to Azure is black-holed on this machine |
| An error message is gone before the narration explains it | Call `pinGrowl()` right after the action that raises it, before `done()` |
| A datatable column filter finds nothing | PrimeFaces filters match from the start of the text. Type the beginning of the value |
| Department picker click times out | Its items are table rows (`tr`), not `li`. `login()` already handles this |
| Hover on `Administration` times out | Several menu items are labelled Administration. `topMenu()` targets the top-level entry |
| Edited XHTML not showing on local Payara | Production stage caches Facelets. Run `asadmin disable <app>` then `asadmin enable <app>` |
| Bash heredoc "unexpected EOF" when writing JS | Write files with the Write tool, not heredocs; the caption quotes break the shell |
| Red "Database Migration Pending" bar visible | The overlay hides it. If it still shows, it only appears on dev builds, not to users |
| Screen and voice drift apart | Never use fixed sleeps for pacing. Let `done()` pace each step from the clip duration |
