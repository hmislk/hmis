---
name: app-configuration
description: >
  Application configuration options reference for the HMIS project. Use when working with
  feature toggles, configuration keys, configOptionApplicationController, bill number
  configuration, printer configuration, or any configurable application behavior.
user-invocable: true
---

# Application Configuration Guide

## Configuration System

HMIS uses `configOptionApplicationController` for feature toggles and runtime configuration.

### Usage Pattern

```xhtml
<!-- Boolean toggle with default false -->
rendered="#{configOptionApplicationController.getBooleanValueByKey('Feature Key')}"

<!-- Boolean toggle with explicit default true -->
rendered="#{configOptionApplicationController.getBooleanValueByKey('Feature Key', true)}"
```

### Key Naming Convention

- Use descriptive, hierarchical names
- Module prefix: `Pharmacy Transfer Issue - Show Rate and Value`
- Report columns: `Pharmacy Disbursement Reports - Display Serial Number`

## Configuration Categories

### Pharmacy Transfer
- Rate display toggles (Purchase/Cost/Retail Rate)
- Bill format options (POS, Template)
- Footer customization (CSS and text)

### Report Display
- Column visibility toggles per report type
- Font size settings
- Serial number display

### Bill Number
- Prefix and year counting strategies
- Department-specific number generation
- **🚨 Inward deposit/payment series:** with `Unique Serial Per Admission Type for Inward Payments` off (and `Omit Year` off or not deployed), their serial follows the general `Bill Number Generation Strategy - ...` counter. That counter is institution-wide, shared by all bills, when none of those options is on. See option 4 in the Bill Number Config doc.

### 🚨 Applying config to a live hospital
1. **Apply the agreed values exactly.** Use the recorded plan (memory, tracker, issue). If you think a value should differ, ask before setting it. Never "simplify" a value on your own reading of the requirement.
2. **Check the deployed branch has the code.** `git grep "<key>" origin/<deployed-branch>`. A key that exists in the DB but not in that branch's code does nothing.
3. **Verify the output, not the config row.** After the change, read a record created after it, e.g. the newest bill's `deptId`/`insId`. Check that it matches the requirement: format **and** serial order. Report those real values.

### Printer
- Printing profiles and templates
- Per-department printer assignment
- **🚨 `configOptionController` keys are department-scoped when a department is selected.** With a department selected in the session, it reads `<Department name> - <key>` first and falls back to the global `<key>`, and its `setBooleanValueByKey` writes only the department copy. With no department selected, it reads and writes the global `<key>`. The receipt pages' **Settings** dialogs (e.g. `Inward Payment Bill ... Paper`) use it, so check the row for the session's department (`optionKey like '<Dept> - %'`), not just the global one.

### Pharmacy Procurement
- Cross-department PO receiving (institution-wide, opt-in)

For complete reference, read:
- [Application Options](../../developer_docs/configuration/application-options.md)
- [Bill Number Config](../../developer_docs/configuration/bill-number-config-options.md)
- [Printer Configuration](../../developer_docs/configuration/printer-configuration-system.md)
