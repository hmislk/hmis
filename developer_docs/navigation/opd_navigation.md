# OPD Navigation Guide

This guide summarises how to reach each OPD-related page from the main menu and lists the privileges and configuration options controlling their visibility.

## Accessing the OPD Menu
1. Log in to the application.
2. On `home.xhtml` select the **OPD** icon to open the module menu (`resources/ezcomp/menu.xhtml`).

## Token and Queue
- **OPD Token** (`#{opdTokenController.navigateToCreateNewOpdToken()}`)
  - Path: `/opd/token/opd_token`
  - Privilege: none
- **OPD Queue** (`#{opdTokenController.navigateToOpdQueue()}`)
  - Path: `/opd/token/opd_queue`
  - Privilege: none

## Billing Submenu
- **OPD Billing** (`#{opdBillController.navigateToNewOpdBill()}`)
  - Path: `/opd/opd_bill`
  - Privilege: `Privileges.OpdBilling`
  - Configuration: `OPD Bill Item Search By Autocomplete` chooses `/opd/opd_bill_ac` instead.
- **OPD Ordering** (`#{opdOrderController.navigateToNewOpdOrder()}`)
  - Path: `/opd/opd_bill`
  - Privilege: `Privileges.OpdOrdering`
  - Configuration: `OPD Bill Item Search By Autocomplete` affects the page as above.
- **Billing for Cashier** (`#{opdPreBillController.navigateToBillingForCashierFromMenu()}`)
  - Path: `/opd/opd_pre_bill`
  - Privilege: `Privileges.OpdPreBilling`
- **Collecting Centre Billing** (`#{collectingCentreBillController.navigateToCollectingCenterBillingromMenu()}`)
  - Path: `/collecting_centre/bill`
  - Privilege: `Privileges.OpdCollectingCentreBilling`
- **Package Billing**
  - Path: `/opd/opd_bill_package`
  - Privilege: `Privileges.OpdBilling`

## Cashier Submenu
- **Scan Bills** (`#{opdPreSettleController.navigateToScanBills()}`)
  - Path: `/cashier/scan_bill_by_barcode_scanner`
  - Privilege: `Privileges.ScanBillsFromCashier`
- **Accept Payments for OPD Batch Bills** (`#{opdPreSettleController.navigateToSettleOpdPreBills()}`)
  - Path: `/opd/opd_search_pre_bill`
  - Privilege: `Privileges.AcceptPaymentForOpdBatchBills`
- **Accept Payments for Pharmacy Bills**
  - Path: `/pharmacy/pharmacy_search_pre_bill`
  - Privilege: `Privileges.AcceptPaymentForPharmacyBills`
- **Refunds**
  - Path: `/pharmacy/pharmacy_search_pre_refund_bill_for_return_cash`
  - Privilege: `Privileges.RefundFromCashier`
- **Refunds for OPD Bills**
  - Path: `/opd/opd_search_pre_refund_bill_for_return_cash`
  - Privilege: `Privileges.RefundOpdBillsFromCashier`
- **Refunds for Pharmacy Bills**
  - Path: `/pharmacy/pharmacy_search_pre_refund_bill_for_return_cash`
  - Privilege: `Privileges.RefundPharmacyBillsFromCashier`

## Search Submenu
- **OPD Bill Search** (`#{opdBillController.navigateToSearchOpdBills()}`)
  - Path: `/opd/opd_bill_search`
  - Privilege: `Privileges.OpdBillSearch`
- **OPD Bill Item Search**
  - Path: `/opd_search_billitem_own`
  - Privilege: `Privileges.OpdBillItemSearch`
- **OPD Payment Search**
  - Path: `/opd_search_bill_fee_payment`
- **OPD Bill Package Search** (`#{billPackageController.navigateToSearchOpdPackageBills()}`)
  - Path: `/opd/opd_package_bill_search`
  - Privilege: `Privileges.OpdBillSearch`
- **Collecting Centre Bill Search**
  - Path: `/collecting_centre/collecting_centre_search_bill_own`
  - Privilege: `Privileges.OpdCollectingCentreBilling`
- **Bills To Pay Search**
  - Path: `/opd_search_bill_to_pay`
  - Privilege: `Privileges.OpdBillSearch`
- **Credit Paid Bill Search**
  - Path: `/opd_search_bill_full_paid`
  - Privilege: `Privileges.OpdBillSearch`
- **Credit Paid Bills with OPD Bill Search**
  - Path: `/opd_search_bill_full_paid_bills`
  - Privilege: `Privileges.OpdBillSearch`

## Doctors Submenu
- **Mark In / Mark Out / Working Times / Pay**
  - Paths: controlled by `WorkingTimeController` (e.g., `/hr/work_time`)
  - Privilege: `Privileges.Opd`

## Other Options
- **Lab Report Print** (`#{reportController.navigatetoOPDLabReportByMenu()}`)
  - Path: `/lab/report_for_opd_print`
  - Privilege: `Privileges.OpdLabReportSearch`
- **OPD Analytics** (`#{cashierReportController.navigateToOpdAnalytics()}`)
  - Path: `/opd/analytics/index`
- **Financial Transaction Manager**
  - Path: `/financial/transaction_index`
  - Privilege: `Privileges.Opd`
  - Configuration: visible only when `applicationPreference.opdBillingAftershiftStart` is `true`.
*Updated for issue #14188.*
