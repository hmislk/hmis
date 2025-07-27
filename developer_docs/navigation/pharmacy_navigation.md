# Pharmacy Navigation Guide

This document lists key pages reachable from the **Pharmacy** menu. Each entry shows the main menu path, the page URL, required user privilege and any configuration notes.

| Menu steps | Page path | Privilege | Configuration notes |
|------------|-----------|-----------|---------------------|
| **Pharmacy → Retail → Fast Sale 1** | `/faces/pharmacy/pharmacy_fast_retail_sale.xhtml` | `PharmacySale` | |
| **Pharmacy → Retail → Fast Sale 2** | `/faces/pharmacy/pharmacy_fast_retail_sale_1.xhtml` | `PharmacySale` | |
| **Pharmacy → Retail → Fast Sale 3** | `/faces/pharmacy/pharmacy_fast_retail_sale_2.xhtml` | `PharmacySale` | |
| **Pharmacy → Retail → Fast Sale 4** | `/faces/pharmacy/pharmacy_fast_retail_sale_3.xhtml` | `PharmacySale` | |
| **Pharmacy → Cashier → Sale for Cashier** | `/faces/pharmacy/pharmacy_bill_retail_sale_for_cashier.xhtml` | `PharmacySale` | Requires active cashier shift when `applicationPreference.pharmacyBillingAfterShiftStart` is enabled |
| **Pharmacy → Purchase → Direct Purchase** | `/faces/pharmacy/direct_purchase.xhtml` | `PharmacyPurchase` | |
| **Pharmacy → Purchase → GRN** | `/faces/pharmacy/pharmacy_grn.xhtml` | `PharmacyGoodReceive` | |
| **Pharmacy → Purchase → Purchase Order** | `/faces/pharmacy/pharmacy_purhcase_order_list.xhtml` | `PharmacyOrderCreation` | |
| **Pharmacy → Stock → Transfer Request** | `/faces/pharmacy/pharmacy_transfer_request.xhtml` | `PharmacyTransferRequest` | |
| **Pharmacy → Stock → Issue Stock** | `/faces/pharmacy/pharmacy_transfer_issue.xhtml` | `PharmacyTransferIssue` | |
| **Pharmacy → Stock → Receive Stock** | `/faces/pharmacy/pharmacy_transfer_receive.xhtml` | `PharmacyTransferRecive` | |
| **Pharmacy → Stock → Bin Card** | `/faces/pharmacy/bin_card.xhtml` | `Pharmacy` | |
| **Pharmacy → Reports → Sale by Date Summary** | `/faces/pharmacy/pharmacy_report_sale_by_date_summery.xhtml` | `ReportsSearchCashCardOwn` | |
| **Pharmacy → Reports → Stock With Supplier** | `/faces/pharmacy/pharmacy_report_stock_report_with_supplier.xhtml` | `ReportsItemOwn` | |
| **Pharmacy → Reports → BHT Issue Items With Margin** | `/faces/pharmacy/pharmacy_report_bht_issue_item_with_margin.xhtml` | `ReportsItemOwn` | |
| **Pharmacy → Settings → Administration** | `/faces/pharmacy/admin/index.xhtml` | `Pharmacy` | |
| **Pharmacy → Settings → Importer Categories** | `/faces/pharmacy/pharmacy_importer_category.xhtml` | `Pharmacy` | |
