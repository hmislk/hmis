# Playwright MCP — Browser Automation with Claude Code

The Playwright MCP server lets Claude Code control a real browser: navigate pages, click
elements, fill forms, upload files, and take screenshots. This guide covers setup and
practical patterns discovered through real-world use.

---

## Setup

### Desktop App (no config needed)

The Playwright MCP plugin is built into the Claude Code **desktop app** and available in
every session automatically. Open any project folder in the desktop app and the tools are
ready.

### CLI (`claude` in terminal)

The CLI does **not** include Playwright by default. Add it once at user scope so it works
in every project folder:

```powershell
claude mcp add playwright npx @playwright/mcp@latest --scope user
```

Then restart your terminal and reopen `claude`. The tools will appear as `mcp__playwright__*`.

> **Note:** Adding `mcpServers` directly to `settings.json` does **not** work — Claude Code
> rejects it. Always use the `claude mcp add` CLI command.

### Verify it works

```text
Use mcp__playwright__browser_navigate to open https://example.com
```

---

## Tool Reference

| Tool | What it does |
|---|---|
| `browser_navigate` | Go to a URL |
| `browser_snapshot` | Get accessibility tree with element refs |
| `browser_take_screenshot` | Capture a screenshot |
| `browser_click` | Click an element by ref |
| `browser_type` | Type text into a field |
| `browser_fill_form` | Fill multiple fields at once |
| `browser_press_key` | Press a key (Enter, Tab, Escape, etc.) |
| `browser_select_option` | Select a `<select>` dropdown option |
| `browser_hover` | Hover over an element |
| `browser_navigate_back` | Browser back button |
| `browser_wait_for` | Wait for element or condition |
| `browser_evaluate` | Run JavaScript on the page |
| `browser_file_upload` | Upload a file via file chooser |
| `browser_handle_dialog` | Handle alert/confirm/prompt dialogs |
| `browser_network_requests` | List recent network requests |
| `browser_tabs` | List open tabs |
| `browser_close` | Close the browser |
| `browser_console_messages` | Read browser console output |
| `browser_resize` | Resize the browser window |

---

## Core Workflow

```text
1. browser_navigate      → open the page
2. browser_snapshot      → inspect accessibility tree, get element refs
3. browser_click         → click a button or link by ref
4. browser_type          → type into a field by ref
5. browser_take_screenshot → visually confirm result
```

---

## Snapshots vs Screenshots

**Use `browser_snapshot`** to find elements. It returns the accessibility tree with
`[ref=eXXX]` identifiers you can pass to `browser_click`, `browser_type`, etc.

```bash
browser_snapshot depth=4   ← limit depth to keep output small
```

**Use `browser_take_screenshot`** to visually confirm the page state, especially after
complex interactions where you are not sure what happened.

### Stale refs

Snapshot refs (`eXXX`) **change after every page interaction**. If you get a "ref not found"
error, re-run `browser_snapshot` to get fresh refs before retrying.

---

## Clicking Patterns

### Standard click
```bash
browser_click ref="e123"
```

### Label intercepts the click (hidden radio/checkbox)
When clicking a radio button gives "label intercepts pointer events", click the **label**
element instead of the input:
```bash
browser_snapshot   ← find the <label> ref, not the <input> ref
browser_click ref="e456"   ← click the label
```

### Overlay blocks click (modal/dropdown still open)
After dismissing a dropdown, an invisible overlay can block the next click. Fix: press Tab
to move focus away, then click:
```bash
browser_press_key key="Tab"
browser_click ref="e789"
```

Or bypass entirely with JavaScript:
```js
browser_evaluate script="() => {
  const btns = [...document.querySelectorAll('button')];
  const b = btns.find(b => b.textContent.trim() === 'Save' && b.closest('.modal'));
  if (b) b.click();
}"
```

---

## Dropdown Patterns

### Native `<select>`
```bash
browser_select_option target="eXXX" values=["option-value"]
```

### Semantic UI / custom combobox (e.g. InvenioRDM, Zenodo)
```text
1. browser_click ref="<combobox container ref>"
2. browser_type  ref="<input ref>" text="search term"
3. browser_snapshot   ← find the option ref in the dropdown list
4. browser_click ref="<option ref>"
5. browser_press_key key="Tab"   ← dismiss overlay before next action
```

---

## File Upload
```text
1. browser_click ref="<Upload button ref>"   ← opens OS file chooser
2. browser_file_upload paths=["C:\\absolute\\path\\to\\file.pdf"]
```

Use absolute paths. Relative paths may not resolve correctly.

---

## Rich Text / CKEditor Fields

CKEditor renders inside an `<iframe>`. Standard `browser_type` on the outer element will
not work. Use `browser_evaluate` to set content directly:
```js
browser_evaluate script="() => {
  const editor = document.querySelector('.ck-editor__editable');
  if (editor && editor.ckeditorInstance) {
    editor.ckeditorInstance.setData('<p>Your content here</p>');
  }
}"
```

---

## Login and Session State

- Navigating to a protected URL redirects to the login page — check the resulting URL after
  `browser_navigate` to confirm you are actually on the intended page.
- Navigating away from an unsaved form loses all data. Always save a draft before navigating.
- The browser session persists for the whole conversation. You stay logged in between tool calls.

---

## Deferred Tool Schemas

The Playwright tools are **deferred** — their schemas are not loaded until first use.
Claude loads them automatically via `ToolSearch` when you ask to use them. You do not need
to do anything special; just ask Claude to use a Playwright tool and it will fetch the schema.

If Claude seems stuck, you can prompt it explicitly:
```text
Use ToolSearch to load the mcp__playwright__browser_navigate schema, then navigate to X.
```

---

## Common Errors

| Error | Cause | Fix |
|---|---|---|
| "label intercepts pointer events" | Clicking hidden `<input>` | Click the `<label>` element instead |
| "Ref eXXX not found" | Snapshot is stale after interaction | Re-run `browser_snapshot` for fresh refs |
| "strict mode violation" | Selector matches multiple elements | Use `ref=` from snapshot, or more specific selector |
| Dialog closes without saving | Escape / overlay dismisses modal | Tab to dismiss dropdowns; use JS `.click()` for modal buttons |
| Form data lost | Navigated away before saving | Save draft before any navigation |
| `mcpServers` rejected in settings.json | Not a valid Claude Code field | Use `claude mcp add ... --scope user` instead |
| Tools not available in terminal | CLI session without MCP config | Run `claude mcp add playwright npx @playwright/mcp@latest --scope user` |
