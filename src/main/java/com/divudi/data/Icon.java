package com.divudi.data;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public enum Icon {
    Patient_Lookup("Patient_Lookup.png", "#{opdBillController.navigateToSearchPatients}"),
    Patient_Add_New("Patient_Add_New.png", "#{patientController.navigateToAddNewPatientForOpd}"),
    New_Opd_Bill("New_Opd_Bill.png", "#{opdBillController.navigateToNewOpdBill}"),
    New_Opd_Bill_For_Cashier("New_Opd_Bill_For_Cashier.png", "#{opdPreBillController.navigateToBillingForCashierFromMenu}"),
    Channel_Booking("Channel_Booking.png", "#{channelBookingController.navigateToChannelBookingFromMenu}"),
    Search_Opd_Bill("Search_Opd_Bill.png", "#{searchController.navigateToSearchOpdBillsOfLoggedDepartment}"),
    Cashier_Summeries("Cashier_Summeries.png", "#{cashierReportController.navigateToOpdSummeries}"),
    Shift_End_Summery("Shift_End_Summery.png", "#{cashierReportController.navigateToDayEndSummary}"),
    Day_End_Summery("Day_End_Summery.png", "#{cashierReportController.navigateToShiftEndSummary}"),
    Admit("Admit.png", "#{admissionController.navigateToAdmitFromMenu()}");

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
