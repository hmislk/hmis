package com.divudi.data;

/**
 * Enum for defining various icons with human-readable labels. Note: Image and
 * action paths are removed as per request.
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public enum Icon {
    Patient_Lookup("Patient Lookup"),
    Patient_Add_New("Add New Patient"),
    Channel_Booking("Channel Booking"),
    Search_Opd_Bill("Search OPD Bill"),
    Cashier_Summaries("Cashier Summaries"),
    Shift_End_Summary("Shift End Summary"),
    Day_End_Summary("Day End Summary"),
    Admit("Admit Patient"),
    Opd_Billing("OPD Billing"),
    Billing_For_Cashier("Billing for Cashier"),
    Collecting_Centre_Billing("Collecting Centre Billing"),
    Medical_Package_Billing("Medical Package Billing"),
    Accept_Payments("Accept Payments"),
    Accept_Payments_For_OPD_Bills("Accept Payments for OPD Bills"),
    Accept_Payments_For_OPD_Batch_Bills("Accept Payments for OPD Batch Bills"),
    Accept_Payments_For_Pharmacy_Bills("Accept Payments for Pharmacy Bills"),
    Scan_to_Pay("Scan to Pay"),
    Manage_Shift_Fund_Bills("Manage Shift Fund Bills"),
    Manage_Token("Manage Token"),
    Create_Purchase_Order("Create Purchase Order"),
    Auto_Order_P_Model("Auto Order (P Model)"),
    Auto_Order_Q_Model("Auto Order (Q Model)"),
    Direct_Purchase("Direct Purchase"),
    Purchase_Orders_Approval("Purchase Orders Approval"),
    Goods_Receipt("Goods Receipt"),
    Return_Received_Goods("Return Received Goods"),
    Return_without_Receipt("Return without Receipt");

    private final String label;

    Icon(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
