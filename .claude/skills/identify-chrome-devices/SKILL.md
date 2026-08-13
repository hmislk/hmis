---
name: identify-chrome-devices
description: >
  Identify and name Chrome instances connected to the Claude-in-Chrome
  extension under the current Claude account, and persist a stable
  deviceId -> name mapping to a local file outside the repo. Use when the
  user asks to "identify my chrome devices", "name my browsers", wants to
  disambiguate list_connected_browsers output, or invokes
  /identify-chrome-devices. Requires a session with the Claude-in-Chrome
  extension actually connected — see
  developer_docs/tools/claude-in-chrome-device-identification.md for
  background and the policy on preferring Playwright for ordinary browser
  automation.
user-invocable: true
---

# Identify Chrome Devices

Automates the manual workaround from
[developer_docs/tools/claude-in-chrome-device-identification.md](../../../developer_docs/tools/claude-in-chrome-device-identification.md):
loop through every `deviceId` the current Claude account can see, help the
user confirm which physical device each one is, and record the result to a
local mapping file — so this doesn't have to be done by hand in-chat every
time a new device connects.

**Before running this**: confirm the task genuinely needs Claude-in-Chrome.
For ordinary browser automation/testing, use Playwright (`playwright-e2e`
skill) instead — it's cheaper and more flexible. This skill exists only
because identifying a user's *already-paired* physical devices is something
Playwright cannot do (it launches its own separate browser, with no
visibility into paired Claude-in-Chrome devices).

## Step 0 — Precondition check

Confirm `list_connected_browsers`, `select_browser` (or `switch_browser`),
and `javascript_tool` are actually available in this session (check the
active tool list / deferred-tool search). If they aren't, this session
isn't connected to the Claude-in-Chrome extension — tell the user this
skill must be run from a session that has it connected, and stop here.

## Step 1 — Load the existing mapping

Determine the local mapping file path for the current OS:
- Windows: `C:\Credentials\claude-chrome-devices.json`
- Linux/Mac: `~/.config/hmis/claude-chrome-devices.json`

If it exists, read and parse it. If it doesn't exist yet, treat the map as
empty — it will be created in Step 4. Never look for or write this file
anywhere inside the project repo.

## Step 2 — Enumerate connected devices

Call `list_connected_browsers`. For each returned `deviceId`, check it
against the loaded map:
- Known (already has a `name` entry) → skip, no need to re-prompt.
- Unknown → add to the work list for Step 3.

If the work list is empty, report the existing mapping to the user (Step 5)
and stop — nothing new to identify.

## Step 3 — Identify each unknown device

For each unknown `deviceId`, in turn:

1. Call `select_browser` (or `switch_browser`) with that `deviceId` to make
   it the active device.
2. Open a tab and use `javascript_tool` to inject a full-viewport banner
   showing the raw `deviceId` as large, plain text on the page — nothing
   else needed on the page for this to work.
3. Use `AskUserQuestion` to ask the user to physically check that device's
   screen and confirm: is this the device they expect, and what name
   should it be given (e.g. "Home laptop", "Office desktop", "DC server 2")?
   Let them decline/skip a device if they don't recognize it right now.
4. If the user provided a name, record an entry:
   ```json
   {
     "deviceId": "<the deviceId>",
     "name": "<user-supplied name>",
     "osPlatform": "<from list_connected_browsers>",
     "isLocal": <from list_connected_browsers>,
     "firstIdentified": "<today's date>",
     "lastConfirmed": "<today's date>"
   }
   ```
   For a device that was already in the map and the user just re-confirmed
   it, update `lastConfirmed` instead of creating a duplicate entry.

## Step 4 — Persist the mapping

Write the full updated `{"devices": [...]}` array back to the local file
from Step 1, creating the parent directory first if it doesn't exist.
Overwrite the whole file — the in-memory map from Step 1 plus this
session's updates is always the full, current source of truth.

## Step 5 — Summarize

Print a `deviceId -> name` table for every device now in the mapping
(known + newly identified this run) so the user has a single confirmation
of the current state. Remind them the file lives at the OS-specific path
above, outside the repo, and is never committed.

## Note on multiple Claude accounts

`list_connected_browsers` only sees devices connected under the Claude
account driving the current session. If the user has devices under more
than one account, this skill needs to be run once per account — the same
local mapping file accumulates entries across runs regardless of which
account identified them.
