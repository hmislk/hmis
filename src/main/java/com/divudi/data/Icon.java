package com.divudi.data;

/**
 * Enum for defining various icons with human-readable labels. Note: Image and
 * action paths are removed as per request.
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com>
 */
public enum Icon {
//for OPD Section
    Patient_Lookup("Patient Lookup"),
    Patient_Add_New("Add New Patient"),
    
    Opd_Billing("OPD Billing"),
    Billing_For_Cashier("Billing for Cashier"),
    Collecting_Centre_Billing("Collecting Centre Billing"),
    Medical_Package_Billing("Medical Package Billing"),
    
    Scan_to_Pay("Scan to Pay"),
    Accept_Payments("Accept Payments"),
    Accept_Payments_For_OPD_Bills("Accept Payments for OPD Bills"),
    Accept_Payments_For_OPD_Batch_Bills("Accept Payments for OPD Batch Bills"),
    Accept_Payments_For_Pharmacy_Bills("Accept Payments for Pharmacy Bills"),
    Refunds("Refunds"),
    RefundsforOPDBills("Refunds for OPD Bills"),
    RefundsforPharmacyBills("Refunds for Pharmacy Bills"),
    
    Search_Opd_Bill("Search OPD Bill"),
    Search_Opd_Bill_Item("Search OPD Bill Item"),
    Search_Opd_Payment("Search OPD Payment"),
    Search_Collecting_Centre_Bill("Search Collecting Centre Bill"),
    Search_Bill_to_Pay("Search Bill to Pay"),
    Search_Credit_Paid_Bill("Search Credit Paid Bill"),
    Search_Credit_Paid_Bill_with_OPD_Bill("Search Credit Paid Bill with OPD Bill"),

    Doctor_Mark_in("Doctor Mark in"),
    Doctor_Mark_out("Doctor Mark out"),
    Doctor_Working_Times("Doctor Working Times"),
    
    Manage_Token("Manage Token"),
    
    Lab_Report_Print("Lab Report Print"),
    
    OPD_Summaries ("OPD Summaries"),
    
    OPD_Analytics ("OPD Analytics"),
    
//for Pharmacy
    Channel_Booking("Channel Booking"),
    Cashier_Summaries("Cashier Summaries"),
    Shift_End_Summary("Shift End Summary"),
    Day_End_Summary("Day End Summary"),
    Admit("Admit Patient"),
    
    
    
    Manage_Shift_Fund_Bills("Manage Shift Fund Bills"),
    
    Create_Purchase_Order("Create Purchase Order"),
    Auto_Order_P_Model("Auto Order (P Model)"),
    Auto_Order_Q_Model("Auto Order (Q Model)"),
    Direct_Purchase("Direct Purchase"),
    Purchase_Orders_Approval("Purchase Orders Approval"),
    Goods_Receipt("Goods Receipt"),
    Return_Received_Goods("Return Received Goods"),
    Return_without_Receipt("Return without Receipt"),
    ;

    private final String label;

    Icon(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
