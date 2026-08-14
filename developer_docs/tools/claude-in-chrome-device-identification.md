# Claude-in-Chrome: Identifying Connected Devices

## Problem

When multiple Chrome instances (laptops, desktops, remote servers) are
connected to the Claude-in-Chrome extension under one Claude account,
there's no reliable built-in way to tell them apart:

- `list_connected_browsers` returns only generic placeholder names
  ("Browser 1", "Browser 2", ...) for every connected device, and the
  label/order is not stable across calls — the same physical device can
  show up differently on a later call.
- `switch_browser` broadcasts a pairing/confirmation prompt to *every*
  connected extension and resolves on whichever device the user clicks
  "Connect" on first, returning a user-supplied name at that point. That
  name is **not** reflected back into subsequent `list_connected_browsers`
  calls, which keep returning the same static generic list. The two tools
  cannot currently be cross-referenced to build a stable `deviceId -> name`
  map.
- Neither tool alone gives enough signal (`deviceId` is an opaque GUID;
  `osPlatform`/`isLocal` aren't enough) to let a user confidently say
  "yes, that's my laptop."
- `list_connected_browsers` is scoped to whichever Claude account is
  currently driving the session — devices connected under a different
  Claude account, on the same or a different machine, are invisible to it.
  Anyone with multiple Claude accounts needs a separate identification
  pass per account.

See [issue #22925](https://github.com/hmislk/hmis/issues/22925).

## Why this can't be fixed in this repo

`list_connected_browsers` and `switch_browser` are implemented inside the
Claude-in-Chrome browser extension itself, not in this codebase. Persisting
a `switch_browser`-assigned name and surfacing it back through
`list_connected_browsers` would require a change to that extension. If you
want this pursued, file feedback with Anthropic directly (e.g. through the
extension's own feedback channel) — it isn't something a change here can
resolve.

What *is* fixable from this repo is automating the manual disambiguation
workaround below, and standardizing where the resulting map is stored. That
is what the `identify-chrome-devices` skill does.

## Policy: prefer Playwright

For ordinary browser automation and UI testing (exercising HMIS pages,
verifying a fix, taking screenshots), always default to **Playwright** —
see the `playwright-e2e` skill. Playwright is cheaper on tokens, fully
scriptable, and doesn't depend on any particular device being connected.

Claude-in-Chrome tools (`list_connected_browsers`, `select_browser`,
`javascript_tool` against a live tab) should only be used
when the task specifically requires interacting with the user's own
already-open, already-paired physical browser session. Device
identification is that exception by nature: Playwright launches its own
separate, throwaway browser instance — it has no visibility into, and
cannot select among, a user's existing Claude-in-Chrome-paired devices.
Outside of that kind of case, reach for Claude-in-Chrome only when the user
explicitly asks for it.

For the full pros/cons comparison, safety notes (including why Claude in
Chrome must never be pointed at real patient data), and how-to guidance for
both tools, see
[claude-in-chrome-vs-playwright.md](claude-in-chrome-vs-playwright.md).

## Workflow

Run the `identify-chrome-devices` skill (`.claude/skills/identify-chrome-devices/`)
from a session that has the Claude-in-Chrome extension connected. It
automates the loop that used to be done by hand:

1. Load the existing local mapping file, if one exists, so already-named
   devices aren't re-prompted.
2. Call `list_connected_browsers` and diff against the known map to find
   new/unidentified `deviceId`s.
3. For each unidentified device:
   a. `select_browser` to make it active — not `switch_browser`, which
      broadcasts a pairing prompt to every connected device instead of
      targeting one by `deviceId`.
   b. Open a tab and inject a full-width banner via `javascript_tool`
      showing the raw `deviceId` as plain text on the page.
   c. Ask the user to physically check that screen and confirm which real
      device it is and what name to give it.
   d. Record `{deviceId, name, osPlatform, isLocal, identifiedUnderAccount,
      firstIdentified, lastConfirmed}` into the mapping.
4. Write the updated mapping back to the local file via a temp file +
   atomic rename (see below), re-reading first in case another run added
   entries since step 1.
5. Print a summary table of `deviceId -> name` to the user.

Already-identified devices are skipped by default on later runs; the user
can ask to re-verify a device (or all of them) to refresh `lastConfirmed`.

Repeat the whole pass once per Claude account — `list_connected_browsers`
only sees devices connected under the account driving the current session.

## Storage format

The resulting `deviceId -> name` map is written to a **single local JSON
file outside this repository** — never commit it, and never paste real
`deviceId`s, device names, or hostnames into any file inside this project
(per CLAUDE.md's credentials rule). This mirrors the existing convention
used for MySQL credentials in the `database-guide` skill.

**Path:**
- Windows: `C:\Credentials\claude-chrome-devices.json`
- Linux/Mac: `~/.config/hmis/claude-chrome-devices.json`

**Schema** (example uses placeholder values only):

```json
{
  "devices": [
    {
      "deviceId": "abc-123-placeholder",
      "name": "Example Laptop",
      "osPlatform": "windows",
      "isLocal": true,
      "identifiedUnderAccount": "you@example.com",
      "firstIdentified": "2026-08-13",
      "lastConfirmed": "2026-08-13"
    }
  ]
}
```

`identifiedUnderAccount` records which Claude account was active when the
device was identified. Anthropic's docs don't state whether `deviceId` is
guaranteed globally unique across different Claude accounts (only that
`list_connected_browsers` visibility is account-scoped) — if the same
`deviceId` ever turns up under two accounts with different names, treat
that as a signal to double-check before trusting the entry, rather than
assuming it's a safe collision-free key.

No Google Doc mirror or auto-memory pointer entry is required — the single
file is the source of truth. If a developer wants a personal backup copy
elsewhere, that's their own choice and stays outside the repo either way.
