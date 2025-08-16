# Lab Navigation Guide

This document lists all pages reachable from the **Lab** menu. Each entry shows how to navigate from the main screen, the target page path, required user privilege and any relevant configuration keys.

| Menu steps | Page path | Privilege | Configuration notes |
|------------|-----------|-----------|---------------------|
| **Lab → Billing → Lab Billing** | `/faces/opd/opd_bill.xhtml` (`opd_bill_ac.xhtml` when `OPD Bill Item Search By Autocomplete` is true) | `OpdBilling` | Requires shift start when `applicationPreference.opdBillingAftershiftStart` is enabled |
| **Lab → Billing → Billing for Cashier** | `/faces/opd/opd_pre_bill.xhtml` | `OpdPreBilling` | |
| **Lab → Billing → Package Billing** | `/faces/opd/opd_bill_package.xhtml` | `OpdBilling` | |
| **Lab → Billing → Inpatient Billing** | `/faces/inward/inward_bill_service.xhtml` | `InwardServicesAndItemsAddServices` | |
| **Lab → Billing Search → Lab Bill Search** | `/faces/opd/opd_bill_search.xhtml` | `OpdBillSearch` | |
| **Lab → Billing Search → Lab Bill Item Search** | `/faces/opd_search_billitem_own.xhtml` | `OpdBillItemSearch` | |
| **Lab → Billing Search → Search In-patient Bills** | `/faces/lab/inward_search_service.xhtml` | `LabInwardSearchServiceBill` | |
| **Lab → Sample Management** | `/faces/lab/generate_barcode_p.xhtml` | `LabSampleCollecting` or `LabSampleReceiving` | Hidden when `The system uses the Laboratory Dashboard as its default interface` is true |
| **Lab → Laboratory Dashboard** | `/faces/lab/laboratory_management_dashboard.xhtml` | `DashBoardMenu` | Visible when `The system uses the Laboratory Dashboard as its default interface` is true |
| **Lab → Worksheets** | `/faces/lab/receive.xhtml` | `LabSampleReceiving` | |
| **Lab → Report Management** | `/faces/lab/search_for_reporting_ondemand.xhtml` | `LabReportSearch` | |
| **Lab → Report Search by Billing Department** | `/faces/lab/patient_reports_search.xhtml` | `LabReportPrint` | |
| **Lab → Lab Analytics** | `/faces/reportLab/lab_summeries_index.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Performance → Investigation List** | `/faces/reportLab/investigation_counts.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Performance → Bill List** | `/faces/reportLab/bill_list.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Performance → Client List** | `/faces/reportLab/client_list.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Performance → Sample List** | `/faces/lab/patient_sample_list.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Performance → Average Turn Around Time** | `/faces/reportLab/turn_over_time.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Performance → Bill-vice turn-around time** | `/faces/reportLab/turn_over_time_bills.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Performance → By Billed Institution** | `/faces/reportLab/report_lab_by_billed_institution.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Performance → By Billed Department** | `/faces/reportLab/report_lab_by_billed_department.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Performance → By Reported Institution** | `/faces/reportLab/report_lab_by_reported_institution.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Performance → By Reported Department** | `/faces/reportLab/report_lab_by_reported_department.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Performance → OPD Bill Items For Credit Companies** | `/faces/reportLab/credit_company_bill_item_list.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Institutions → By Ordering Institution** | `/faces/reportLab/ix_count_by_billed_institution.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Collecting Centres → Report by Collection Centre(Detail)** | `/faces/reportLab/report_lab_collection_centre.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Collecting Centres → Report by Collection Centre(Summary)** | `/faces/reportLab/report_lab_collection_centre_summery.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Collecting Centres → Report by Collection Centre Count** | `/faces/reportLab/report_lab_by_collection_centre_investigation_count.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Collecting Centres → Report by Collection Centre Count(Summary)** | `/faces/reportLab/report_lab_by_collection_centre_investigation_count_summery.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Referring Doctors → Report by Referring Doctor(Details)** | `/faces/reportLab/report_lab_by_refering_doctor.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Referring Doctors → Report by Referring Doctor(Summary)** | `/faces/reportLab/report_lab_by_refering_doctor_summery.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Inward → Inward Lab Summary by Added Date** | `/faces/reportLab/report_inward_service_detail_added_lab.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Inward → Inward Lab Summary by Added Date With Margin** | `/faces/reportLab/report_inward_service_detail_added_lab_feeMargin.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Inward → Investigation Summary Inward** | `/faces/reportLab/report_investigation_summery_by_inward.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Inward → Investigation Summary Inward by Date** | `/faces/reportLab/report_lab_by_date_summery_inward.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Income Summary** | `/faces/reportLab/income_summery.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Report Summary Department** | `/faces/reportLab/report_cashier_detailed_by_department.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Report Summary by day** | `/faces/reportLab/report_lab_by_date_summery.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Investigation Summary Fee Type** | `/faces/reportLab/report_investigation_summery_by_feetype.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Investigation Summary Regent Fee** | `/faces/reportLab/report_investigation_summery_by_regent_fee.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Investigation Summary Fee Type With Credit** | `/faces/reportLab/report_investigation_summery_by_feetype_with_credit.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Investigation Summary Regent Fee With Credit** | `/faces/reportLab/report_investigation_summery_by_regent_fee_with_credit.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Investigation Summary Regent Fee By Payment Method** | `/faces/reportLab/report_investigation_summery_by_regent_fee_by_pay_method.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Daily Lab Summary By Department** | `/faces/reportLab/lab_daily_summary_by_department.xhtml` | `LabSummeries` | Visible when `Lab Daily Summary Report - Legacy Method` is true |
| **Lab → Lab Analytics → Income → Daily Lab Summary By Department (DTO)** | `/faces/reportLab/lab_daily_summary_by_department_dto.xhtml` | `LabSummeries` | Visible when `Lab Daily Summary Report - Optimized Method` is true |
| **Lab → Lab Analytics → Income → Laboratory Card Income Report** | `/faces/reportLab/laboratary_card_income_report.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Daily OPD Fee Summary** | `/faces/reportLab/report_lab_by_date_summery_cash_credit.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Daily OPD Fee Summary with Counts** | `/faces/reportLab/report_lab_by_date_summery_cash_credit_counts.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Daily Inward Fee Summary** | `/faces/reportLab/daily_inward_fee_summery.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Daily Inward Fee Summary with Counts** | `/faces/reportLab/daily_inward_fee_summery_counts.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Income → Report Summary by Month With Cash and Credit** | `/faces/reportLab/report_lab_by_date_summery_cash_credit_only_total.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Lab Summary → Test Wise Count Report** | `/faces/reportLab/test_wise_count.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Lab Summary → Laboratory Income Report** | `/faces/reportLab/laboratary_income_report.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Lab Summary → Laboratory Order Report** | `/faces/reportLab/lab_inward_order_report.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Lab Summary → Laboratory Summary** | `/faces/reportLab/laboratary_summary.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Lab Summary → Daily Summary By Bill Types** | `/faces/reportLab/report_opd_service_summery_by_bill_types.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Lab Summary → Daily Summary** | `/faces/reportLab/report_opd_service_summery.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Lab Summary → Daily Summary Inward and Opd** | `/faces/reportLab/report_investigation_summery_by_inward_opd.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Lab Summary → Daily Summary Inward and Opd by Date** | `/faces/reportLab/report_investigation_summery_by_date_inward_opd.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Lab Summary → Daily Summary Inward and Opd Count** | `/faces/reportLab/report_investigation_summery_by_inward_opd_count.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Auditing → Bills Cancelled after Approving Reports** | `/faces/lab/report_cancelled_bill_search.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Clinical → Test Results - Single** | `/faces/reportLab/test_results_single.xhtml` | `LabSummeries` | |
| **Lab → Lab Analytics → Clinical → Test Results** | `/faces/lab/clinical_result_values.xhtml` | `LabSummeries` | |
