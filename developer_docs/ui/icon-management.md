# Icon Management Guidelines

Consistent icon usage keeps the HMIS ERP interface predictable and accessible. Use this guide to choose icons, colors, and supporting labels without re-reading prior implementations.

## Icon Sources and Sizing
- **Primary library**: PrimeFaces (`pi` icons). Use Font Awesome only when a matching PrimeFaces icon is unavailable.
- **Format**: SVG-based icons (PrimeFaces or Font Awesome) sized to 80x80 when designing reusable SVG assets; rely on CSS sizing for component-level icons.
- **Color**: In shared SVG assets use `currentColor` so icons automatically inherit the context color.
- **Consistency**: When an icon is selected for a workflow or action, reuse it everywhere in that workflow.

## Command Button Standards
Use the following pairings for command buttons across reporting and transactional pages.

| Action | PrimeFaces Icon | Button Class | Label | Tooltip Template |
| ------ | --------------- | ------------ | ----- | ---------------- |
| Process / Generate Report | `pi pi-play` | `ui-button-warning` | `Process` | `Generate report with current filters` |
| Export to Excel | `pi pi-file-excel` | `ui-button-success` | `Export to Excel` | `Download results as Excel` |
| Print | `pi pi-print` | `ui-button-info` | `Print Report` | `Print the current report` |
| Approve / Complete | `pi pi-check` | `ui-button-success` | `Approve` | `Approve the selected record` |
| Revert / Reset | `pi pi-undo` | `ui-button-secondary` | `Reset` | `Reset filters to defaults` |

**Implementation tips**
- Always include a `title` attribute for accessibility.
- Group related buttons with flex utilities, e.g. `class="d-flex flex-wrap gap-2 my-3"`.
- Prefer non-AJAX submissions (`ajax="false"`) for heavy report generation that triggers downloads.

## Terminology Alignment
- Use **“Process”** for generating reports instead of “Refresh” or “Fill”.
- Use **“Export to Excel”** rather than “Download” or “Export”.
- Use **“Print Report”** instead of “Print”.
- Keep button values, icons, and tooltips aligned with the table above.

## Data and Status Icon Patterns
Choose neutral icons that work across currencies and business units.

| Use Case | Recommended Icon |
| -------- | ---------------- |
| Monetary values | `fa-coins`, `fa-money-bill` |
| Date inputs | `fa-calendar-alt` |
| User / staff references | `fa-user` |
| Institution / hospital | `fa-hospital`, `fa-building` |
| Numeric references / IDs | `fa-hashtag`, `fa-list-ol` |
| Approval state | `fa-check-circle`, `fa-check-double` |
| Search / filters | `fa-search` |
| Cancellation | `fa-times-circle` |
| Refund / reverse | `fa-undo` |

Ensure all Font Awesome icons are loaded through the existing theme; never add ad-hoc icon bundles.

## Accessibility and Contrast
- Provide descriptive `title` attributes for icons that have interactive roles.
- Avoid using icons alone to convey status—pair with `h:outputText` labels.
- Respect ERP theming: use PrimeFaces button classes for color meaning (warning/process, success/export, info/print, danger/remove).

## Maintenance Checklist
- Before introducing a new icon, confirm the action is not already covered in this guide.
- When updating icons, verify linked documentation (`CLAUDE.md`, `AGENTS.md`, `gemini.md`) references this guide.
- Keep icon usage consistent between JSF pages and documentation screenshots.
