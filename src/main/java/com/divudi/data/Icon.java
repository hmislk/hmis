package com.divudi.data;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public enum Icon {
    Patient_Lookup("Patient_Lookup.png", "#{opdBillController.navigateToSearchPatients}"),
    Patient_Add_New("Patient_Add_New.png", "#{patientController.navigateToAddNewPatientForOpd}"),
    Channel_Booking("Channel_Booking.png", "#{channelBookingController.navigateToChannelBookingFromMenu}"),
    Search_Opd_Bill("Search_Opd_Bill.png", "#{searchController.navigateToSearchOpdBillsOfLoggedDepartment}"),
    Cashier_Summeries("Cashier_Summeries.png", "#{cashierReportController.navigateToOpdSummeries}"),
    Shift_End_Summery("Shift_End_Summery.png", "#{cashierReportController.navigateToDayEndSummary}"),
    Day_End_Summery("Day_End_Summery.png", "#{cashierReportController.navigateToShiftEndSummary}"),
    Admit("Admit.png", "#{admissionController.navigateToAdmitFromMenu()}"),
    Opd_Billing("New_Opd_Bill.png", "#{opdBillController.navigateToNewOpdBill}"),
    Billing_For_Cashier("New_Opd_Bill_For_Cashier.png", "#{opdPreBillController.navigateToBillingForCashierFromMenu}"),
    Collecting_Centre_Billing("OPD_Billing.png", "#{collectingCentreBillController.navigateToCollectingCenterBillingromMenu}"),
    Medical_Package_Billing("Admit.png", "#{billPackageController.navigateToNewOpdPackageBill}"),
    Accept_Payments("Admit.png", "/opd/patient_accept_payment"),
    Accept_Payments_For_OPD_Bills("Admit.png", "/opd_search_pre_bill"),
    Accept_Payments_For_OPD_Batch_Bills("Admit.png", "/opd_search_pre_batch_bill"),
    Accept_Payments_For_Pharmacy_Bills("Admit.png", "/pharmacy/pharmacy_search_pre_bill");

    public static Icon getPatient_Lookup() {
        return Patient_Lookup;
    }

    private final String image;
    private final String action;

    Icon(String image, String action) {
        this.image = image;
        this.action = action;
    }

    public String getImage() {
        return image;
    }

    public String getAction() {
        return action;
    }

}
