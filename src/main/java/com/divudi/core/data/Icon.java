package com.divudi.core.data;

import static com.divudi.core.data.IconGroup.*;

/**
 * Enum for defining various icons with human-readable labels. Note: Image and
 * action paths are removed as per request.
 *
 * IMPORTANT: This enum is used in several places with EnumType.ORDINAL.
 * Therefore, do NOT insert new enum values in the middle or reorder existing
 * ones. Do NOT delete any values. Instead, deprecate unused ones if necessary.
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public enum Icon {

    //for OPD Section
    Patient_Lookup("Patient Lookup", PATIENT_MANAGEMENT),
    Patient_Add_New("Add New Patient", PATIENT_MANAGEMENT),
    Opd_Billing("OPD Billing", OPD_BILLING),
    Billing_For_Cashier("Billing for Cashier", OPD_BILLING),
    Collecting_Centre_Billing("Collecting Centre Billing", OPD_BILLING),
    Medical_Package_Billing("Medical Package Billing", OPD_BILLING),
    Scan_to_Pay("Scan to Pay", PAYMENTS_AND_REFUNDS),
    Accept_Payments("Accept Payments", PAYMENTS_AND_REFUNDS),
    Accept_Payments_For_OPD_Bills("Accept Payments for OPD Bills", PAYMENTS_AND_REFUNDS),
    Accept_Payments_For_OPD_Batch_Bills("Accept Payments for OPD Batch Bills", PAYMENTS_AND_REFUNDS),
    Accept_Payments_For_Pharmacy_Bills("Accept Payments for Pharmacy Bills", PAYMENTS_AND_REFUNDS),
    Refunds("Refunds", PAYMENTS_AND_REFUNDS),
    RefundsforOPDBills("Refunds for OPD Bills", PAYMENTS_AND_REFUNDS),
    RefundsforPharmacyBills("Refunds for Pharmacy Bills", PAYMENTS_AND_REFUNDS),
    Search_Opd_Bill("Search OPD Bill", OPD_BILLING),
    Search_Opd_Bill_Item("Search OPD Bill Item", OPD_BILLING),
    Search_Opd_Payment("Search OPD Payment", OPD_BILLING),
    Search_Collecting_Centre_Bill("Search Collecting Centre Bill", OPD_BILLING),
    Search_Bill_to_Pay("Search Bill to Pay", OPD_BILLING),
    Search_Credit_Paid_Bill("Search Credit Paid Bill", OPD_BILLING),
    Search_Credit_Paid_Bill_with_OPD_Bill("Search Credit Paid Bill with OPD Bill", OPD_BILLING),
    Doctor_Mark_in("Doctor Mark in", DOCTOR_AND_CHANNEL),
    Doctor_Mark_out("Doctor Mark out", DOCTOR_AND_CHANNEL),
    Doctor_Working_Times("Doctor Working Times", DOCTOR_AND_CHANNEL),
    Manage_Token("Manage Token", DOCTOR_AND_CHANNEL),
    Lab_Report_Print("Lab Report Print", LAB_AND_REPORTS),
    OPD_Summaries("OPD Summaries", OPD_SUMMARIES),
    OPD_Analytics("OPD Analytics", OPD_SUMMARIES),
    pharmacy_sale("Pharmacy - Sale", PHARMACY_SALES),
    pharmacy_sale_for_cashier("Pharmacy - Sale for cashier", PHARMACY_SALES),
    pharmacy_sale_without_stock("Pharmacy - Sale without Stock", PHARMACY_SALES),
    pharmacy_Search_sale_bill("Pharmacy - Search Sale Bill", PHARMACY_SALES),
    pharmacy_search_sale_pre_bill("Pharmacy - Search Sale Pre Bill", PHARMACY_SALES),
    pharmacy_search_sale_bill_item("Pharmacy - Search Sale Bill Item", PHARMACY_SALES),
    pharmacy_return_items_only("Pharmacy - Return Items Only", PHARMACY_RETURNS),
    pharmacy_return_items_and_payments("Pharmacy - Return Items and Payments", PHARMACY_RETURNS),
    pharmacy_search_return_bill("Pharmacy - Search Return Bill", PHARMACY_RETURNS),
    pharmacy_add_to_stock("Pharmacy - Add to Stock", PHARMACY_STOCK),
    wholesale_sale("Wholesale - Sale", WHOLESALE),
    wholesale_sale_for_cashier("Wholesale - Sale for cashier", WHOLESALE),
    wholesale_Search_sale_bill("Wholesale - Search Sale Bill", WHOLESALE),
    wholesale_Search_sale_bill_to_pay("Wholesale - Search Sale Bill to Pay", WHOLESALE),
    wholesale_search_sale_bill_item("Wholesale - Search Sale Bill Item", WHOLESALE),
    wholesale_return_items_only("Wholesale - Return (Items Only)", WHOLESALE),
    wholesale_return_items_and_payments("Wholesale - Return (Items and Payments)", WHOLESALE),
    wholesale_search_return_bill_item("Wholesale - Search Return Bill (Item)", WHOLESALE),
    wholesale_add_to_stock("Wholesale - Add to Stock", WHOLESALE),
    wholesale_purchase("Wholesale - Wholesale Purchase", WHOLESALE),
    direct_issue_to_BHTs("Direct Issue to BHTs", INPATIENT_DIRECT_ISSUES),
    direct_issue_to_theatre("Direct Issue to Theatre", INPATIENT_DIRECT_ISSUES),
    BHT_issue_requests("BHT Issue Request", INPATIENT_DIRECT_ISSUES),
    search_inpatint_direct_issue_by_bill("Search Inpatint Direct Issues by Bill", INPATIENT_DIRECT_ISSUES),
    search_inpatint_direct_issue_by_item("Search Inpatint Direct Issues by Item", INPATIENT_DIRECT_ISSUES),
    search_inpatint_direct_issue_returns_by_bill("Search Inpatint Direct Issues Returns by Bill", INPATIENT_DIRECT_ISSUES),
    search_inpatint_direct_issue_returns_by_item("Search Inpatint Direct Issues Returns by Bill", INPATIENT_DIRECT_ISSUES),
    Create_Purchase_Order("Create Purchase Order", PURCHASING),
    Auto_Order_P_Model("Auto Order (P Model)", PURCHASING),
    Auto_Order_Q_Model("Auto Order (Q Model)", PURCHASING),
    Direct_Purchase("Direct Purchase", PURCHASING),
    Purchase_Orders_Approval("Purchase Orders Approval", PURCHASING),
    Goods_Receipt("Goods Receipt", GOODS_RECEIPT),
    Return_Received_Goods("Return Received Goods", GOODS_RECEIPT),
    Return_without_Receipt("Return without Receipt", GOODS_RECEIPT),
    Multiple_Purchase_Order_Cancellation("Multiple Purchase Order Cancellation", PURCHASING),
    pharmacy_issue("Pharmacy - Issue", PHARMACY_STOCK),
    pharmacy_search_issue_bill("Pharmacy - Search Issue Bill", PHARMACY_STOCK),
    pharmacy_search_issue_bill_items("Pharmacy - Search Issue Bill Items", PHARMACY_STOCK),
    pharmacy_search_issue_return_bill("Pharmacy - Search Issue Return Bill", PHARMACY_STOCK),
    pharmacy_uint_issue_margin("Pharmacy - Uint Issue Margin", PHARMACY_STOCK),
    pharmacy_request("Pharmacy - Request", PHARMACY_STOCK),
    pharmacy_issue_for_request("Pharmacy - Issue for Request", PHARMACY_STOCK),
    pharmacy_direct_issue("Pharmacy - Direct Issue", PHARMACY_STOCK),
    pharmacy_receive("Pharmacy - Receive", PHARMACY_STOCK),
    pharmacy_disbursement_reports("Pharmacy - Disbursement Reports", PHARMACY_REPORTS),
    adjustments_Depaetment_stock("Adjustments - Depaetment Stock", STOCK_ADJUSTMENTS),
    adjustments_staff_stock("Adjustments - Staff Stock", STOCK_ADJUSTMENTS),
    adjustments_purchase_rate("Adjustments - Purchase Rate", STOCK_ADJUSTMENTS),
    adjustments_sale_rate("Adjustments - Sale Rate", STOCK_ADJUSTMENTS),
    adjustments_wholesale_rate("Adjustments - Wholesale Rate", STOCK_ADJUSTMENTS),
    adjustments_expiry_date("Adjustments - Expiry Date", STOCK_ADJUSTMENTS),
    adjustments_search_adjustment_bill("Adjustments - Search Adjustment Bill", STOCK_ADJUSTMENTS),
    adjustments_transfer_all_stock("Adjustments - Transfer All Stock", STOCK_ADJUSTMENTS),
    supplier_due_search("Supplier Due Search", SUPPLIER_PAYMENTS),
    supplier_payment_management("Supplier Payment Management", SUPPLIER_PAYMENTS),
    dealer_due_by_age("Dealer Due by Age", SUPPLIER_PAYMENTS),
    payment_by_supplier("Payment by Supplier", SUPPLIER_PAYMENTS),
    payment_by_bill("Payment by Bill", SUPPLIER_PAYMENTS),
    GRN_payment_approve("GRN Payment Approve", SUPPLIER_PAYMENTS),
    GRN_payment_done_search("GRN Payment done search", SUPPLIER_PAYMENTS),
    credit_dues_and_access("Credit Dues and Access", SUPPLIER_PAYMENTS),
    pharmacy_item_search("Pharmacy - Item Search", PHARMACY_REPORTS),
    pharmacy_generate_report("Pharmacy - Generate Report", PHARMACY_REPORTS),
    pharmacy_summary_views("Pharmacy - Summary Views", PHARMACY_REPORTS),
    pharmacy_analytics("Pharmacy - Analytics", PHARMACY_REPORTS),
    Goods_Receipt_With_Approval("Goods Receipt With Approval", GOODS_RECEIPT),
    Pharmacy_Order_Cancellation("Pharmacy Order Cancellation", PURCHASING),
    Optician_EMR("Optician - EMR Management", OPTICIAN),
    Optician_Patient_Management("Optician - Patient Management", OPTICIAN),
    Optician_Appointment_Management("Optician - Appointment Management", OPTICIAN),
    Optician_Stock_Management("Optician - Stock Management", OPTICIAN),
    Optician_Product_Catalog("Optician - Product Catalog", OPTICIAN),
    Optician_Repair_Management("Optician - Repair Management", OPTICIAN),
    Optician_Retail_Sale("Optician - Retail Sale", OPTICIAN),
    @Deprecated
    Appointments("Appointments", null),
    Channel_Booking("Channel Booking", DOCTOR_AND_CHANNEL),
    Cashier_Summaries("Cashier Summaries", CASHIER),
    Shift_End_Summary("Shift End Summary", CASHIER),
    Day_End_Summary("Day End Summary", CASHIER),
    Admit("Admit Patient", INPATIENT_ADMISSIONS),
    Inpatient_Appointments("Inpatient Appointments", INPATIENT_ADMISSIONS),
    Search_Admissions("Search Admissions", INPATIENT_ADMISSIONS),
    Investigation_Trace("Trace Investigations", LAB_AND_REPORTS),
    Manage_Shift_Fund_Bills("Manage Shift Fund Bills", CASHIER),
    Cashier_Drawer("Logged User Drawer", CASHIER),
    Financial_Transaction_Manager("Financial Transaction Manager", CASHIER),
    Channel_Booking_by_Dates("Channel Booking by Dates", DOCTOR_AND_CHANNEL),
    Channel_Scheduling("Channel Scheduling", DOCTOR_AND_CHANNEL),
    Goods_Receipt_Costing("Goods Receipt Costing", GOODS_RECEIPT),
    Report_Execution_Logs("Report Execution Logs", LAB_AND_REPORTS),
    Purchase_Orders_Finalize("Finalize Purchase Orders", PURCHASING),
    pharmacy_bill_search("Pharmacy Bill Search", PHARMACY_SALES),
    pharmacy_bill_search_new("Pharmacy Bill Search (New)", PHARMACY_SALES),
    pharmacy_disposal_issue("Pharmacy Disposal - Direct Issue", PHARMACY_STOCK),
    Patient_Deposit_Management("Patient Deposit Management", PAYMENTS_AND_REFUNDS),

    //for Inpatient Section
    Edit_Admission_Details("Edit Admission Details", INPATIENT_ADMISSIONS),
    Change_Patient_for_Admission("Change Patient for Admission", INPATIENT_ADMISSIONS),
    Appointment_Admission("Appointment Admission", INPATIENT_ADMISSIONS),
    Manage_Appointment("Manage Appointment", DOCTOR_AND_CHANNEL),
    Room_Reservations("Room Reservations", INPATIENT_ROOMS),
    Room_Occupancy("Room Occupancy", INPATIENT_ROOMS),
    Room_Vacancy("Room Vacancy", INPATIENT_ROOMS),
    Admit_Room("Admit Room", INPATIENT_ROOMS),
    Room_Change("Room Change", INPATIENT_ROOMS),
    Guardian_Room_Change("Guardian Room Change", INPATIENT_ROOMS),
    Add_Services_Investigations("Add Services and Investigations", INPATIENT_SERVICES),
    Add_Services_With_Payments("Add Services and Investigations with Payments", INPATIENT_SERVICES),
    Add_Outside_Charges("Add Outside Charges", INPATIENT_SERVICES),
    Add_Professional_Fee("Add Professional Fee", INPATIENT_SERVICES),
    Add_Estimated_Professional_Fee("Add Estimated Professional Fee", INPATIENT_SERVICES),
    Add_Timed_Services("Add Timed Services", INPATIENT_SERVICES),
    Interim_Bill("Interim Bill", INPATIENT_BILLING),
    Interim_Bill_Estimated("Interim Bill - Estimated Professional Fees", INPATIENT_BILLING),
    Interim_Bill_Search("Interim Bill Search", INPATIENT_BILLING),
    Search_Service_Bill("Search Service Bill", INPATIENT_BILLING),
    Search_Professional_Bill("Search Professional Bill", INPATIENT_BILLING),
    Search_Estimated_Professional_Bill("Search Estimated Professional Bill", INPATIENT_BILLING),
    Search_Provisional_Bill("Search Provisional Bill", INPATIENT_BILLING),
    Search_Final_Bill("Search Final Bill", INPATIENT_BILLING),
    Search_Final_Bill_By_Discharge_Date("Search Final Bill by Discharge Date", INPATIENT_BILLING),
    Request_Medicines_From_Pharmacy("Request Medicines from Pharmacy", INPATIENT_SERVICES),
    View_Pharmacy_Requests("View Pharmacy Requests", INPATIENT_SERVICES),
    Inward_Analytics("Inward Analytics", INPATIENT_ANALYTICS);

    private final String label;
    private final IconGroup iconGroup;

    Icon(String label, IconGroup iconGroup) {
        this.label = label;
        this.iconGroup = iconGroup;
    }

    public String getLabel() {
        return label;
    }

    public IconGroup getIconGroup() {
        return iconGroup;
    }

    /**
     * Returns the image filename for this icon.
     * Icons from Patient_Deposit_Management onwards use .svg extension.
     * Earlier icons use .png extension (legacy format).
     *
     * @return Image filename with extension (e.g., "Patient_Lookup.png" or "Patient_Deposit_Management.svg")
     */
    public String getImage() {
        if (this.ordinal() >= Patient_Deposit_Management.ordinal()) {
            return this.name() + ".svg";
        }
        return this.name() + ".png";
    }
}
