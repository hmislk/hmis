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

## Inventory Reports

| Key                                                              | Type      | Default | Description                                                                                             |
| ---------------------------------------------------------------- | --------- | ------- | ------------------------------------------------------------------------------------------------------- |
| `Cost of Goods Sold Report - Display Stock Correction Section`  | Boolean   | `true`  | Controls whether the Stock Correction section is displayed and calculated in the Cost of Goods Sold report. |

