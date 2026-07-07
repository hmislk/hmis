# Application Options

This document lists the configuration options used in the application and their purpose.

## Pharmacy Transfer Issue

| Key                                                              | Type      | Default | Description                                                                                             |
| ---------------------------------------------------------------- | --------- | ------- | ------------------------------------------------------------------------------------------------------- |
| `Stock Transaction - Show Rate and Value`                          | Boolean   | `false` | Controls visibility of rate and value in stock transactions.                                            |
| `Pharmacy Transfer Issue Bill is PosHeaderPaper`                 | Boolean   | `true`  | Controls visibility of the main print button.                                                           |
| `Pharmacy Transfer Issue Bill is POS Paper without details`        | Boolean   | `false` | Renders the POS paper bill without details.                                                             |
| `Pharmacy Transfer Issue Bill is POS Paper with details`         | Boolean   | `false` | Renders the POS paper bill with details.                                                                |
| `Pharmacy Transfer Issue Bill is POS Paper with header`          | Boolean   | `false` | Renders the POS paper bill with a header.                                                               |
| `Pharmacy Transfer Issue Bill is Template`                       | Boolean   | `false` | Renders the bill using a template.                                                                      |
| `Pharmacy Transfer is by Purchase Rate`                          | Boolean   | `false` | Determines if the transfer rate is based on the purchase rate.                                            |
| `Pharmacy Transfer is by Cost Rate`                              | Boolean   | `false` | Determines if the transfer rate is based on the cost rate.                                                |
| `Pharmacy Transfer is by Retail Rate`                            | Boolean   | `true`  | Determines if the transfer rate is based on the retail rate.                                              |
| `DepNumGenFromToDepartment`                                      | Boolean   | `false` | Determines how the department bill number is generated.                                                   |
| `Display Colours for Stock Autocomplete Items`                   | Boolean   | `true`  | Controls whether to display colors for stock autocomplete items based on expiry dates.                    |
| `Report Font Size of Item List in Pharmacy Disbursement Reports` | String    | `10pt`  | Sets the font size for the item list in the report.                                                       |
| `Pharmacy Disbursement Reports - Display Serial Number`          | Boolean   | `true`  | Controls the visibility of the serial number column.                                                      |
| `Pharmacy Disbursement Reports - Display Code`                   | Boolean   | `true`  | Controls the visibility of the item code column.                                                          |
| `Pharmacy Disbursement Reports - Display Batch Number`           | Boolean   | `false` | Controls the visibility of the batch number column.                                                       |
| `Pharmacy Disbursement Reports - Display Date of Expiary`        | Boolean   | `true`  | Controls the visibility of the expiry date column.                                                        |
| `Pharmacy Disbursement Reports - Display Purchase Rate`          | Boolean   | `true`  | Controls the visibility of the purchase rate column.                                                      |
| `Pharmacy Disbursement Reports - Display Purchase Value`         | Boolean   | `false` | Controls the visibility of the purchase value column.                                                     |
| `Pharmacy Disbursement Reports - Display Retail Sale Rate`       | Boolean   | `false` | Controls the visibility of the retail sale rate column.                                                     |
| `Pharmacy Disbursement Reports - Display Retail Sale Value`      | Boolean   | `false` | Controls the visibility of the retail sale value column.                                                    |
| `Pharmacy Disbursement Reports - Display Transfer Rate`          | Boolean   | `false` | Controls the visibility of the transfer rate column.                                                      |
| `Pharmacy Disbursement Reports - Display Transfer Value`         | Boolean   | `false` | Controls the visibility of the transfer value column.                                                     |
| `Pharmacy Transfer Issue - Show Rate and Value`                  | Boolean   | `false` | Used in combination with `PharmacyTransferViewRates` to control visibility of rate and value columns. |
| `Pharmacy Transfer Issue Bill Footer CSS`                        | String    | `''`    | CSS for the footer of the transfer issue bill.                                                            |
| `Pharmacy Transfer Issue Bill Footer Text`                       | String    | `''`    | Text for the footer of the transfer issue bill.                                                             |

## Pharmacy Retail Sale

| Key                                                              | Type      | Default | Description                                                                                             |
| ---------------------------------------------------------------- | --------- | ------- | ------------------------------------------------------------------------------------------------------- |
| `Show alternative medicines available during retail sale`        | Boolean   | `true`  | Gates the on-demand "Alternatives" (substitute medicines) UI on the Retail Sale, Fast Sale and Sale for Cashier pages. When `true`, each bill-item row shows a Substitute button that opens a dialog listing in-stock, non-expired substitute stocks in the current department (FEFO order) and lets the cashier swap one in. When `false`, no Substitute control appears and no alternatives query runs. See issue #21697. |

## OPD Billing

| Key                                                              | Type      | Default | Description                                                                                             |
| ---------------------------------------------------------------- | --------- | ------- | ------------------------------------------------------------------------------------------------------- |
| `OPD Billing - Clear Referring Doctor on New Bill`              | Boolean   | `true`  | When true, clears the referring doctor and referring institution when starting a new OPD bill. When false, the values are preserved across consecutive bills. |

## Pharmacy Procurement

| Key                                                              | Type      | Default | Description                                                                                             |
| ---------------------------------------------------------------- | --------- | ------- | ------------------------------------------------------------------------------------------------------- |
| `Pharmacy - Allow Cross-Department PO Receiving`                | Boolean   | `false` | Institution-wide toggle. When `true`, the Purchase Orders for Receiving list (and its wholesale/with-approval/DTO variants) drops the same-department restriction, so a PO created in one department (e.g. Pharmacy) can be received/GRN'd from any other department in the same institution (e.g. Store). Institution isolation is unaffected — POs from a different institution never appear. Added for RMH Hambantota, which creates POs in Pharmacy but receives into Store. See issue #21848. |

## Inventory Reports

| Key                                                              | Type      | Default | Description                                                                                             |
| ---------------------------------------------------------------- | --------- | ------- | ------------------------------------------------------------------------------------------------------- |
| `Cost of Goods Sold Report - Display Stock Correction Section`  | Boolean   | `true`  | Controls whether the Stock Correction section is displayed and calculated in the Cost of Goods Sold report. |

## Collecting Centre

| Key                                                              | Type      | Default | Description                                                                                             |
| ----------------------------------------------------------------  | --------- | ------- | ------------------------------------------------------------------------------------------------------- |
| `Collecting Centre Agent Payment - Skip Payment Record`         | Boolean   | `true`  | When true, `CollectingCentrePaymentController.createPayment()` does not create a `Payment` record for Collecting Centre Agent Payment / Cancellation bills (`CC_AGENT_PAYMENT`, `CC_AGENT_PAYMENT_CANCELLATION`). These are agent/collecting-centre commission payouts, not cashier cash collections, and should not appear in cashier reports (All Cashier Summary, Cashier Summary, Cashier Details). The `Bill` itself is still created for agent-balance history and printing. See issue #21840. |

