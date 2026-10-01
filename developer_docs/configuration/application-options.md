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
| `Show Mark Foreigner and Mark Local Buttons in Billing`         | Boolean   | `true`  | Controls visibility of the Mark Foreigner / Mark Local buttons across OPD Billing, Channel Booking, Clinic Sessions, and Inward Service Bill screens. Independent of `Save the Patient with Patient Status` (which only controls the read-only status badge/toggle shown during patient registration). See issue #22311. |

## Inward Discharge

| Key                                                              | Type      | Default | Description                                                                                             |
| ---------------------------------------------------------------- | --------- | ------- | ------------------------------------------------------------------------------------------------------- |
| `Inward Administrative Discharge - Require Nursing Discharge`   | Boolean   | `true`  | Controls whether `BhtSummeryController.checkDischargeTime()` requires `PatientEncounter.isNursingDischarged()` before allowing administrative discharge (the "Discharge" action on the Interim Bill) for room-charged admissions. Some hospitals do not use the nursing discharge workflow; setting this to `false` for those institutions lets administrative discharge proceed without it. See issue #22607. |

## Pharmacy Procurement

| Key                                                              | Type      | Default | Description                                                                                             |
| ---------------------------------------------------------------- | --------- | ------- | ------------------------------------------------------------------------------------------------------- |
| `Pharmacy - Allow Cross-Department PO Receiving`                | Boolean   | `false` | Institution-wide toggle. When `true`, the Purchase Orders for Receiving list (and its wholesale/with-approval/DTO variants) drops the same-department restriction, so a PO created in one department (e.g. Pharmacy) can be received/GRN'd from any other department in the same institution (e.g. Store). Institution isolation is unaffected — POs from a different institution never appear. Added for RMH Hambantota, which creates POs in Pharmacy but receives into Store. See issue #21848. |
| `Pharmacy - List Packs (AMPPs) in Item Selection`               | Boolean   | `true`  | When `false`, Packs (AMPPs) are excluded from every pharmacy item autocomplete that offers them — Purchase Order Request (JPA and native), Direct Purchase, Purchase, Donation, Batch Create and Transfer Request — so only AMPs (plus VMPs/VMPPs on Transfer Request) can be chosen. On the Purchase Order page the supplier-item dropdown and the *Add All Supplier Items* / *Below ROL* buttons also drop AMPPs from the supplier's item list (`ItemController.removeAmppsIfNotListed()`). GRN, Issue and Transfer Issue have no AMPP picker of their own (they take items from the PO, the request or stock), so they follow automatically. Bills that already contain AMPPs are unaffected. Implemented in `ItemController.isAmppListedInItemSelection()`. See issue #24164. |

### GRN Item Table Columns (native GRN page)

Columns of the added-items table on `pharmacy/pharmacy_grn_costing_native.xhtml` (Pharmacy → Procurement → Create GRN from PO). Every option defaults to `true`, so hospitals that set nothing see every column. Hiding a column only stops it rendering; the line keeps the value it was created with (see each row) and nothing else about saving changes. Item Name, Receiving Qty, Purchase Rate, Retail Rate, Date Of Expiry, Batch No and Actions are always shown because a GRN cannot be completed correctly without them. See issue #24171.

| Key                                                              | Type      | Default | Description                                                                                             |
| ---------------------------------------------------------------- | --------- | ------- | ------------------------------------------------------------------------------------------------------- |
| `Medicine Identification Codes Used`                            | Boolean   | `true`  | Shows the Code column (shared with other pharmacy pages). |
| `GRN - Show Ordered Qty Column`                                 | Boolean   | `true`  | Shows the Ordered Qty column. |
| `GRN - Show Ordered Free Qty Column`                            | Boolean   | `true`  | Shows the Ordered Free Qty column. |
| `GRN - Show Received Free Qty Column`                           | Boolean   | `true`  | Shows the Received Free Qty input. When hidden, the line receives the free quantity still outstanding on the PO. |
| `GRN - Show Discount Rate Column`                               | Boolean   | `true`  | Shows the Discount Rate input. When hidden, no line discount is applied (a GRN line starts at 0). |
| `GRN - Show Wholesale Rate Column`                              | Boolean   | `true`  | Shows the Wholesale Rate input. When hidden, the wholesale rate stays 0 (a GRN line starts at 0). |
| `GRN - Show Line Net Total Column`                              | Boolean   | `true`  | Shows the Line Net Total column. |
| `GRN - Show Comments Column`                                    | Boolean   | `true`  | Shows the Comments input. |
| `Show Profit Percentage in GRN`                                 | Boolean   | `true`  | Shows the Profit % column. |

## Inventory Reports

| Key                                                              | Type      | Default | Description                                                                                             |
| ---------------------------------------------------------------- | --------- | ------- | ------------------------------------------------------------------------------------------------------- |
| `Cost of Goods Sold Report - Display Stock Correction Section`  | Boolean   | `true`  | Controls whether the Stock Correction section is displayed and calculated in the Cost of Goods Sold report. |

## Collecting Centre

| Key                                                              | Type      | Default | Description                                                                                             |
| ----------------------------------------------------------------  | --------- | ------- | ------------------------------------------------------------------------------------------------------- |
| `Collecting Centre Agent Payment - Skip Payment Record`         | Boolean   | `true`  | When true, `CollectingCentrePaymentController.createPayment()` does not create a `Payment` record for Collecting Centre Agent Payment / Cancellation bills (`CC_AGENT_PAYMENT`, `CC_AGENT_PAYMENT_CANCELLATION`). These are agent/collecting-centre commission payouts, not cashier cash collections, and should not appear in cashier reports (All Cashier Summary, Cashier Summary, Cashier Details). The `Bill` itself is still created for agent-balance history and printing. See issue #21840. |

