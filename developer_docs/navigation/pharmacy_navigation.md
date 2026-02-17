# Pharmacy Navigation Guide

This document lists key pages reachable from the **Pharmacy** menu. Each entry shows how to navigate from the main screen, the page path, required privilege and any relevant configuration keys.

| Menu steps | Page path | Privilege | Configuration notes |
|------------|-----------|-----------|---------------------|
| **Pharmacy → Wholesale → Retail Sale** | `/faces/pharmacy_wholesale/pharmacy_bill_retail_sale.xhtml` | `PharmacySaleWh` | |
| **Pharmacy → Wholesale → GRN** | `/faces/pharmacy_wholesale/pharmacy_grn.xhtml` | `PharmacyGoodReceiveWh` | |
| **Pharmacy → Wholesale → Purchase** | `/faces/pharmacy_wholesale/pharmacy_purchase.xhtml` | `PharmacyPurchaseWh` | |
| **Pharmacy → Wholesale → Credit Report** | `/faces/pharmacy_wholesale/pharmacy_report_credit.xhtml` | `PharmacyReports` | |
| **Pharmacy → Wholesale → Bill Search** | `/faces/pharmacy_wholesale/pharmacy_search_sale_bill.xhtml` | `PharmacyReports` | |
| **Pharmacy → Purchases → Purchase Entry** | `/faces/pharmacy/pharmacy_purchase.xhtml` | `PharmacyPurchase` | |
| **Pharmacy → Purchases → Purchase Opening Stock** | `/faces/pharmacy/pharmacy_purchase_openning_stock.xhtml` | `PharmacyPurchase` | |
| **Pharmacy → Purchases → Purchase Orders for GRN** | `/faces/pharmacy/pharmacy_purchase_order_list_for_recieve.xhtml` | `PurchaseOrdersApprovel` | |
| **Pharmacy → GRN → Goods Receipt** | `/faces/pharmacy/pharmacy_grn.xhtml` | `PharmacyGoodReceive` | |
| **Pharmacy → Stock Management → Department Stock** | `/faces/pharmacy/pharmacy_department_stock.xhtml` | `Pharmacy` | |
| **Pharmacy → Stock Management → Department Stock By Batch** | `/faces/pharmacy/pharmacy_department_stock_report_by_batch.xhtml` | `PharmacyReports` | Requires `Pharmacy Analytics - Show Department Stock By Batch` |
| **Pharmacy → Stock Management → Stock History** | `/faces/pharmacy/pharmacy_department_stock_history.xhtml` | `PharmacyReports` | |
| **Pharmacy → Reports → Pharmacy Bill Report** | `/faces/pharmacy/pharmacy_bill_report.xhtml` | `PharmacyReports` | |
| **Pharmacy → Reports → Purchase Detail** | `/faces/pharmacy/pharmacy_report_purchase_detail.xhtml` | `PharmacyReports` | |
| **Pharmacy → Reports → Sale Summary by Date** | `/faces/pharmacy/pharmacy_report_sale_by_date_summery.xhtml` | `PharmacyReports` | |
| **Pharmacy → Settings → Reorder Management** | `/faces/pharmacy/reorder_management.xhtml` | `PharmacyAdministration` | |
| **Pharmacy → Settings → Importer Categories** | `/faces/pharmacy/pharmacy_importer_category.xhtml` | `PharmacyAdministration` | |
| **Pharmacy → Settings → Set Reorder Levels** | `/faces/pharmacy/pharmacy_set_reorder_manage.xhtml` | `PharmacyAdministration` | |
| **Pharmacy → Settings → Frequency Units** | `/faces/pharmacy/pharmacy_frequency_unit.xhtml` | `PharmacyAdministration` | |
| **Pharmacy → Retail → Fast Sale 1** | `/faces/pharmacy/pharmacy_fast_retail_sale.xhtml` | `PharmacySale` | |
| **Pharmacy → Retail → Fast Sale 2** | `/faces/pharmacy/pharmacy_fast_retail_sale_1.xhtml` | `PharmacySale` | |
| **Pharmacy → Retail → Fast Sale 3** | `/faces/pharmacy/pharmacy_fast_retail_sale_2.xhtml` | `PharmacySale` | |
| **Pharmacy → Retail → Fast Sale 4** | `/faces/pharmacy/pharmacy_fast_retail_sale_3.xhtml` | `PharmacySale` | |
| **Pharmacy → Cashier → Sale for Cashier** | `/faces/pharmacy/pharmacy_bill_retail_sale_for_cashier.xhtml` | `PharmacySale` | Requires active cashier shift when `applicationPreference.pharmacyBillingAfterShiftStart` is enabled |
| **Pharmacy → Cashier → Sale for Cashier - By Item** | `/faces/pharmacy/pharmacy_fast_retail_sale_for_cashier.xhtml` | `PharmacySale` | Requires active cashier shift when `applicationPreference.pharmacyBillingAfterShiftStart` is enabled |
| **Pharmacy → Purchase → Direct Purchase** | `/faces/pharmacy/direct_purchase.xhtml` | `PharmacyPurchase` | |
| **Pharmacy → Purchase → GRN** | `/faces/pharmacy/pharmacy_grn.xhtml` | `PharmacyGoodReceive` | |
| **Pharmacy → Purchase → Purchase Order** | `/faces/pharmacy/pharmacy_purhcase_order_list.xhtml` | `PharmacyOrderCreation` | |
| **Pharmacy → Stock → Transfer Request** | `/faces/pharmacy/pharmacy_transfer_request.xhtml` | `PharmacyTransferRequest` | |
| **Pharmacy → Stock → Issue Stock** | `/faces/pharmacy/pharmacy_transfer_issue.xhtml` | `PharmacyTransferIssue` | Colour-coded autocomplete when `Display Colours for Stock Autocomplete Items` is true |
| **Pharmacy → Stock → Receive Stock** | `/faces/pharmacy/pharmacy_transfer_receive.xhtml` | `PharmacyTransferRecive` | |
| **Pharmacy → Stock → Bin Card** | `/faces/pharmacy/bin_card.xhtml` | `Pharmacy` | |
| **Pharmacy → Reports → Sale by Date Summary** | `/faces/pharmacy/pharmacy_report_sale_by_date_summery.xhtml` | `ReportsSearchCashCardOwn` | |
| **Pharmacy → Reports → Stock With Supplier** | `/faces/pharmacy/pharmacy_report_stock_report_with_supplier.xhtml` | `ReportsItemOwn` | |
| **Pharmacy → Reports → BHT Issue Items With Margin** | `/faces/pharmacy/pharmacy_report_bht_issue_item_with_margin.xhtml` | `ReportsItemOwn` | |
| **Pharmacy → Analytics → Procurement Reports → Item-wise Procurement** | `/faces/pharmacy/report_item_vice_purchase_and_good_receive.xhtml` | `PharmacyReports` | |
| **Pharmacy → Settings → Administration** | `/faces/pharmacy/admin/index.xhtml` | `Pharmacy` | |
| **Pharmacy → Settings → Importer Categories** | `/faces/pharmacy/pharmacy_importer_category.xhtml` | `Pharmacy` | |


