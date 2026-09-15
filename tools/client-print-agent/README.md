# Client Print Agent

A small Windows background agent for client PCs that print raw ESC/P dot-matrix
output (e.g. deposit slips, bill reprints) triggered by a "Print (Row)"-style
button in the HIMS web UI.

## How it works

1. A page in the web app (e.g. `inward_reprint_bill_deposit.xhtml`) has a
   button that downloads a `.prn` file (raw printer bytes) into a watched
   folder on the client PC, instead of opening a normal browser print dialog.
2. `raw-text-print-agent.ps1` polls that folder, sends each matching file's
   raw bytes straight to a locally installed printer via the Windows Print
   Spooler API (bypassing GDI rendering), then removes the file.
3. `start-agent-hidden.vbs` launches the PowerShell agent with no visible
   window, and is placed in the current user's Startup folder so it runs
   automatically at logon.

## Files

- `raw-text-print-agent.ps1` — the watcher/print loop.
- `RawPrinterHelper.ps1` — P/Invoke wrapper around `winspool.drv`
  (`OpenPrinter`/`StartDocPrinter`/`WritePrinter`) used to send raw bytes.
- `start-agent-hidden.vbs` — hidden launcher for Startup.
- `print-agent-config.example.json` — copy to `print-agent-config.json`
  next to the script and adjust for the site's printer name/folder.

## Setup on a client PC

1. Copy this folder's contents to a folder on the client PC, e.g.
   `C:\hims-print\`.
2. Copy `print-agent-config.example.json` to `print-agent-config.json` in
   the same folder and set:
   - `WatchFolder` — where the browser downloads `.prn` files to.
   - `PrinterPath` — the exact name of the installed Windows printer to
     print to (`Settings > Printers & scanners`).
   - `FileGlob` — filename pattern to watch for.
   - `PollSeconds` — polling interval.
3. Create a shortcut to `start-agent-hidden.vbs` in
   `%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup` so it starts
   automatically at logon. No admin rights are required.
4. Log off/on (or run the shortcut once) and confirm `agent.log` in the
   watch folder shows a `watching ... -> <printer>` line.

## Design notes / known pitfalls this agent avoids

- **Never retries a print after it already succeeded.** Sending to the
  printer and deleting the source file are independent steps — a delete
  failure (e.g. AV briefly locking the file) never causes a reprint.
- **Files are claimed before printing** by atomically renaming them
  (`file.prn` → `file.prn.processing`). This means a file can never be
  picked up twice, even by two agent instances racing the same folder.
- **Single-instance guard** via a named mutex — if the agent is started
  twice (e.g. run manually while it's already running from Startup), the
  second instance exits immediately instead of double-printing every file.
- **No infinite retry storm.** If the printer is unavailable, a job is
  retried a few times and then moved to a `failed/` subfolder instead of
  being retried forever every poll cycle — this is what previously caused
  large numbers of duplicate jobs to build up once a printer connection was
  restored.
