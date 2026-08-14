# Browser Automation: Playwright vs Claude in Chrome

This project's default for browser automation and UI testing is **Playwright**
(see the `playwright-e2e` skill and
[`developer_docs/tools/playwright-mcp-guide.md`](playwright-mcp-guide.md) for
setup and tool patterns). **Claude in Chrome** is a separate, narrower tool —
this doc explains what it actually is, how it compares to Playwright, and the
small set of cases where it's the right choice instead.

## What each tool is

- **Playwright (MCP)** — Claude Code drives a browser it launches itself
  (Chromium, Firefox, or WebKit) through the Playwright API: navigate, click,
  fill forms, read the accessibility tree, take screenshots. It's a clean,
  disposable browser instance every time.
- **Claude in Chrome** — a Chrome/Edge browser extension that lets Claude act
  inside your **actual, already-open browser** — your real tabs, logins,
  cookies, and session state — via `claude --chrome` or the VS Code
  extension. See Anthropic's own docs:
  [Use Claude Code with Chrome](https://code.claude.com/docs/en/chrome) and
  [Use Claude in Chrome safely](https://support.claude.com/en/articles/12902428-use-claude-in-chrome-safely).

## Pros and cons

| | Playwright | Claude in Chrome |
|---|---|---|
| **Browser state** | Fresh, clean instance every run | Your real browser: existing logins, cookies, open tabs |
| **Reproducibility** | Same result on repeat runs | Ephemeral — state changes as you use your browser day to day |
| **CI/CD** | Runs in pipelines, on schedules, in parallel | Not usable in CI — needs a real, visible browser window with you present |
| **Setup for logged-in flows** | Needs explicit auth (cookies/storage state, or a login step in the script) | Inherits whatever you're already signed into — no setup |
| **Token/context cost** | Lower — structured DOM/accessibility-tree access (`browser_snapshot`) rather than raw screenshots | Higher — more screenshot- and page-content-heavy per step |
| **Browser coverage** | Chromium, Firefox, WebKit | Chrome, Edge, and other Chromium-based browsers (Brave, Arc, Vivaldi, Opera) only |
| **Cost** | Free, open source | Requires a paid Claude plan; not available via third-party providers (Bedrock, Vertex, Foundry) |
| **Failure mode when blocked** | Script just fails/retries | Pauses and asks you to handle logins/CAPTCHAs manually |

This lines up with independent comparisons too — Playwright MCP is generally
described as faster and more reliable for scripted, repeatable automation via
structured DOM access, while Claude in Chrome's value is being inside your
real, authenticated session ("tasks you would do yourself, with your own eyes
on it"). See sources at the bottom.

## When to use which (in this project)

**Default to Playwright** for anything that fits its model — which is nearly
everything HMIS-related:
- Testing a feature end-to-end (`playwright-e2e` skill) against local Payara
  with local/test data.
- Any repeatable verification you might want to run again, or that a
  reviewer should be able to reproduce.
- Anything that could plausibly run unattended or in CI later.

**Reach for Claude in Chrome only when:**
- The task genuinely needs your own already-open, already-authenticated
  browser session and reproducing that login/state in Playwright isn't
  practical (e.g. an SSO-gated internal tool, your personal Google Docs,
  your Claude-in-Chrome device list itself).
- The task is inherently about your real paired devices, like identifying
  which physical machine a `deviceId` belongs to — see the
  `identify-chrome-devices` skill and
  [claude-in-chrome-device-identification.md](claude-in-chrome-device-identification.md).
  Playwright cannot do this: it always launches its own separate browser, so
  it has no visibility into your existing paired Claude-in-Chrome devices.
- You explicitly ask for Claude in Chrome by name.

### 🚨 Do not use Claude in Chrome on real patient data

Per Anthropic's own guidance, **Claude in Chrome is unavailable for
HIPAA-covered organizations and should not be used on pages containing
regulated health data** — it screenshots the visible tab into the
conversation and has no way to filter out sensitive content. HMIS pages
routinely display real patient records. If Claude in Chrome is ever used
against this application, it must only be pointed at local/test instances
with synthetic data — never a production system or any page showing a real
patient's information. Playwright, run against local/test data, is the safe
default for exactly this reason.

## How to use each

**Playwright** — see the `playwright-e2e` skill for the project's standard
workflow, and
[`playwright-mcp-guide.md`](playwright-mcp-guide.md) for tool-by-tool
patterns (snapshots vs screenshots, dropdowns, file upload, common errors).

**Claude in Chrome** — from the CLI, `claude --chrome` (or enable it by
default via `/chrome` → "Enabled by default"); it's available automatically
in the VS Code extension once the extension is installed. Use `/chrome` at
any time to check connection status, manage site permissions, reconnect, or
pick which connected browser to use. A few practical points:
- The extension asks permission per site — start with a restrictive
  allowlist and only grant access to sites you actually need.
- Prefer **Manually Approve** permission mode over the default
  auto-approve when doing anything beyond trivial, read-only browsing.
- Consider a separate Chrome profile with no sensitive-account logins for
  any exploratory or first-time use.
- In plan mode, read-only browser calls (reading the page, screenshots) run
  without a prompt; state-changing calls (clicks, typing, navigation) still
  ask for approval.

## Sources

- [Use Claude Code with Chrome — Claude Code Docs](https://code.claude.com/docs/en/chrome)
- [Use Claude in Chrome safely — Claude Help Center](https://support.claude.com/en/articles/12902428-use-claude-in-chrome-safely)
- [Chrome MCP vs Playwright MCP: Which to Use (2026) — test-lab.ai](https://www.test-lab.ai/blog/chrome-mcp-vs-playwright-mcp)
- [Runtime Tools Compared: Playwright MCP, Chrome DevTools MCP, and Claude in Chrome — Steve Kinney](https://stevekinney.com/courses/self-testing-ai-agents/runtime-tools-compared)
- [Playwright CLI vs agent-browser vs Claude in Chrome — AI browser automation token benchmark (2026) — ytyng.com](https://www.ytyng.com/en/blog/ai-browser-automation-tools-comparison-2026)
