# OPD Navigation Guide

This document lists all pages reachable from the **OPD** menu. Each entry shows how to navigate from the main screen, the target page path, required user privilege and any relevant configuration keys.

| Menu steps | Page path | Privilege | Configuration notes |
|------------|-----------|-----------|---------------------|
| **OPD → Patient Lookup & Registration** | `/faces/opd/patient_search.xhtml` | `Opd` | |
| **OPD → OPD Token** | `/faces/opd/token/opd_token.xhtml` | `Opd` | |
| **OPD → OPD Queue** | `/faces/opd/token/opd_queue.xhtml` | `Opd` | |
| **OPD → Billing → OPD Billing** | `/faces/opd/opd_bill.xhtml` (`opd_bill_ac.xhtml` when `OPD Bill Item Search By Autocomplete` is true) | `OpdBilling` | Requires shift start when `applicationPreference.opdBillingAftershiftStart` is enabled |
| **OPD → Billing → OPD Ordering** | `/faces/opd/opd_bill.xhtml` (`opd_bill_ac.xhtml` when `OPD Bill Item Search By Autocomplete` is true) | `OpdOrdering` | Same conditions as OPD Billing |
| **OPD → Billing → Billing for Cashier** | `/faces/opd/opd_pre_bill.xhtml` | `OpdPreBilling` | |
| **OPD → Billing → Collecting Centre Billing** | `/faces/collecting_centre/bill.xhtml` | `OpdCollectingCentreBilling` | |
| **OPD → Billing → Package Billing** | `/faces/opd/opd_bill_package.xhtml` | `OpdBilling` | |
| **OPD → Cashier → Scan Bills** | `/faces/cashier/scan_bill_by_barcode_scanner.xhtml` | `ScanBillsFromCashier` | |
| **OPD → Cashier → Accept Payments for OPD Batch Bills** | `/faces/opd/opd_search_pre_bill.xhtml` | `AcceptPaymentForOpdBatchBills` | |
| **OPD → Cashier → Accept Payments for Pharmacy Bills** | `/faces/pharmacy/pharmacy_search_pre_bill.xhtml` | `AcceptPaymentForPharmacyBills` | |
| **OPD → Cashier → Refunds for OPD Bills**    | `/faces/opd/opd_search_pre_refund_bill_for_return_cash.xhtml`    | `RefundOpdBillsFromCashier`    | |
| **OPD → Cashier → Refunds for Pharmacy Bills** | `/faces/pharmacy/pharmacy_search_pre_refund_bill_for_return_cash.xhtml` | `RefundPharmacyBillsFromCashier` | |
| **OPD → Search → OPD Bill Search** | `/faces/opd/opd_bill_search.xhtml` | `OpdBillSearch` | |
| **OPD → Search → OPD Bill Item Search** | `/faces/opd_search_billitem_own.xhtml` | `OpdBillItemSearch` | |
| **OPD → Search → OPD Payment Search** | `/faces/opd_search_bill_fee_payment.xhtml` | `Opd` | |
| **OPD → Search → OPD Bill Package Search** | `/faces/opd/opd_package_bill_search.xhtml` | `OpdBillSearch` | |
| **OPD → Search → Collecting Centre Bill Search** | `/faces/collecting_centre/collecting_centre_search_bill_own.xhtml` | `OpdCollectingCentreBilling` | |
| **OPD → Search → Bills To Pay Search** | `/faces/opd_search_bill_to_pay.xhtml` | `OpdBillSearch` | |
| **OPD → Search → Credit Paid Bill Search** | `/faces/opd_search_bill_full_paid.xhtml` | `OpdBillSearch` | |
| **OPD → Search → Credit Paid Bills with OPD Bill Search** | `/faces/opd_search_bill_full_paid_bills.xhtml` | `OpdBillSearch` | |
| **OPD → Doctors → Mark In** | `/faces/opd/markIn.xhtml` | `Opd` | |
| **OPD → Doctors → Mark Out** | `/faces/opd/marked_ins_current.xhtml` | `Opd` | |
| **OPD → Doctors → Working Times** | `/faces/opd/workTimes.xhtml` | `Opd` | |
| **OPD → Doctors → Pay** | `/faces/opd/pay_doctor.xhtml` | `Opd` | |
| **OPD → Lab Report Print** | `/faces/lab/report_for_opd_print.xhtml` | `OpdLabReportSearch` | |
| **OPD → OPD Analytics** | `/faces/opd/analytics/index.xhtml` | `Opd` | |
| **OPD → Financial Transaction Manager** | `/faces/cashier/index.xhtml` | `Opd` | Shown only when `applicationPreference.opdBillingAftershiftStart` is enabled |
