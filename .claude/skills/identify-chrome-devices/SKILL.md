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

Confirm `list_connected_browsers`, `select_browser`, and `javascript_tool`
are actually available in this session (check the active tool list /
deferred-tool search). If they aren't, this session isn't connected to the
Claude-in-Chrome extension — tell the user this skill must be run from a
session that has it connected, and stop here.

Use `select_browser`, not `switch_browser`, throughout this skill:
`select_browser(deviceId)` targets a known device directly.
`switch_browser` is a different tool — it broadcasts a pairing prompt to
*every* connected device and waits for the user to manually click
"Connect", and does not accept a `deviceId` to target one device. It has
no role in this automated loop.

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
- Known (already has a `name` entry) → skip by default, no need to
  re-prompt. **Exception**: if the user asked to re-verify a specific
  device, or to re-verify everything (e.g. "re-check my devices"), include
  already-known devices in the work list too — that's the only path that
  reaches the `lastConfirmed` update in Step 3.
- Unknown → always add to the work list for Step 3.

If the work list is empty, report the existing mapping to the user (Step 5)
and stop — nothing new to identify or re-verify.

## Step 3 — Identify each unknown device

For each `deviceId` in the work list, in turn:

1. Call `select_browser` with that `deviceId` to make it the active device.
2. Open a tab and use `javascript_tool` to inject a full-viewport banner
   showing the raw `deviceId` as large, plain text on the page — nothing
   else needed on the page for this to work.
3. Use `AskUserQuestion` to ask the user to physically check that device's
   screen and confirm: is this the device they expect, and what name
   should it be given (e.g. "Home laptop", "Office desktop", "DC server 2")?
   Let them decline/skip a device if they don't recognize it right now.
4. If the user provided a name:
   - **New device** (wasn't in the loaded map): add an entry:
     ```json
     {
       "deviceId": "<the deviceId>",
       "name": "<user-supplied name>",
       "osPlatform": "<from list_connected_browsers>",
       "isLocal": <from list_connected_browsers>,
       "identifiedUnderAccount": "<current Claude account, e.g. email>",
       "firstIdentified": "<today's date>",
       "lastConfirmed": "<today's date>"
     }
     ```
   - **Already-known device being re-verified**: update that entry's
     `lastConfirmed` (and `name`, if the user gave a different one) in
     place — never create a duplicate entry for a `deviceId` already in
     the map.

## Step 4 — Persist the mapping

Before writing, re-read the local file — if it changed since Step 1 (e.g.
another session identified devices in the meantime), merge those entries
into the in-memory map first rather than silently discarding them.

Write the full updated `{"devices": [...]}` map as JSON to a temporary file
in the same directory (creating the directory first if it doesn't exist),
confirm it parses back as valid JSON, then rename it over the target file.
This avoids leaving an unreadable half-written file if the write is
interrupted.

## Step 5 — Summarize

Print a `deviceId -> name` table for every device now in the mapping
(known + newly identified this run) so the user has a single confirmation
of the current state. Remind them the file lives at the OS-specific path
above, outside the repo, and is never committed.

## Note on multiple Claude accounts

`list_connected_browsers` only sees devices connected under the Claude
account driving the current session. If the user has devices under more
than one account, this skill needs to be run once per account — the same
local mapping file accumulates entries across runs, with each entry's
`identifiedUnderAccount` recording which account identified it. Anthropic's
docs don't confirm `deviceId` is globally unique across accounts, so if the
same `deviceId` shows up under two accounts with different names, flag it
to the user rather than silently overwriting one with the other.
