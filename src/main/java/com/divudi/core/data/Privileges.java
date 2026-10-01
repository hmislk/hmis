/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 *
 */
package com.divudi.core.data;

import com.divudi.bean.lab.LaborataryReportController;

public enum Privileges {

    //<editor-fold defaultstate="collapsed" desc="Main">
    Opd("OPD"),
    Inward("Inward"),
    Lab("Lab"),
    Pharmacy("Pharmacy"),
    Payment("Payment"),
    Hr("HR"),
    Reports("Reports"),
    User("User"),
    Admin("Admin"),
    Channelling("Channelling"),
    Clinic("Clinics"),
    Clinical("Clinical"),
    Store("Store"),
    Search("Search"),
    CashTransaction("Cash Transaction"),
    //</editor-fold>

    //cashier menu in opd module
    Cashier("OPD cashier"),
    ScanBillsFromCashier("Scan Bills From Cashier"),
    AcceptPaymentForOpdBatchBills("Accept Payment for opd Batch Bills"),
    RefundFromCashier("Refund in cashier"),
    RefundOpdBillsFromCashier("Refund opd Bills From Cashier"),
    RefundPharmacyBillsFromCashier("Rufund Pharmacy Bills From Cashier"),
    AcceptPaymentForPharmacyBills("Accept payment For Pharmacy Bill(Cashier)"),
    //<editor-fold defaultstate="collapsed" desc="OPD">
    // Submenu Privileges
    OpdBilling("OPD Billing"),
    OpdOrdering("OPD Ordering without Financial Details"),
    OpdCollectingCentreBillingMenu("OPD Collecting Centre Billing Menu"),
    OpdCollectingCentreBilling("OPD Collecting Centre Billing"),
    OpdCollectingCentreBillSearch("OPD Collecting Centre Bill Search"),
    OpdPreBilling("OPD Pre Billing"),
    OpdBillSearch("OPD Bill Search"),
    OpdBillItemSearch("OPD Bill Item Search"),
    OpdReprint("OPD Reprint"),
    OpdCancel("OPD Cancel"),
    OpdIndividualCancel("OPD Individual Cancel"),
    OpdReturn("OPD Return"),
    OpdReactivate("OPD Reactivate"),
    OpdBillSearchEdit("OPD Bill Search Edit"),
    OpdLabReportSearch("OPD Lab Report Search"),
    OpdReprintOriginalBill("OPD Reprint Original Bill"),
    OpdAddNewRefferalDoctor("OPD Add New Referral Doctor"),
    OpdAddNewCollectingCentre("OPD Add New Collecting Centre"),
    ChangeProfessionalFee("Change Professional Fee"),
    OpdPackageBillCancel("OPD Package Bill Cancel"),
    OpdEditPatientDetails("OPD Edit Patient Details"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Inpatient">
    AddCreditLimitInRegistration("Add Credit Limit in Registration"),
    InwardAdmissions("Inward Admissions"),
    InwardAdmissionsAdmission("Inward Admissions Admission"),
    InwardAdmissionsEditAdmission("Inward Admissions Edit Admission"),
    InwardAdmissionsInwardAppoinment("Inward Admissions Inward Appointment"),
    InwardRoom("Inward Room"),
    InwardRoomRoomOccupency("Inward Room Occupancy"),
    InwardRoomRoomChange("Inward Room Change"),
    
    // IP Appointment
    InwardAppointmentMenu("IP Appointment Menu"),
    AddInwardAppointment("Add IP Appointment"),
    InwardAppointmentAdmission("IP Appointment to Admit"),
    InwardAppointmentUpdate("IP Appointment Update"),
    InwardAppointmentCancel("IP Appointment Cancel"),
    
    WatingRoomAdmitPatient("Wating Room Admit Patient"),
    InwardRoomGurdianRoomChange("Inward Guardian Room Change"),
    InwardRoomDischarge("Inward Room Discharge"),
    InwardRoomTransferInitiate("Inward Room Transfer Initiate"),
    InwardRoomPatientAccept("Inward Room Patient Accept"),
    InwardServicesAndItems("Inward Services and Items"),
    InwardServicesAndItemsAddServices("Inward Add Services"),
    InwardServicesAndItemsAddOutSideCharges("Inward Add Outside Charges"),
    InwardServicesAndItemsAddProfessionalFee("Inward Add Professional Fee"),
    InwardServicesAndItemsAddTimedServices("Inward Add Timed Services"),
    InwardBilling("Inward Billing"),
    InwardBillingInterimBill("Inward Interim Bill"),
    InwardBillingInterimBillSearch("Inward Interim Bill Search"),
    InwardSearch("Inward Search"),
    InwardSearchServiceBill("Inward Search Service Bill"),
    InwardSearchServiceBillUnrestrictedAccess("Inward Search Service Bill Without Restricted"),
    InwardSearchProfessionalBill("Inward Search Professional Bill"),
    InwardSearchFinalBill("Inward Search Final Bill"),
    InwardSettleFinalBillUnrestricted("Inward Settle Final Bill Without Restricted"),
    InwardReport("Inward Report"),
    InwardFinalBillReportEdit("Inward Final Bill Report Edit"),
    InwardAdministration("Inward Administration"),
    InwardAdditionalPrivilages("Inward Additional Privileges"),
    InwardBillSearch("Inward Bill Search"),
    InwardBillItemSearch("Inward Bill Item Search"),
    InwardBillReprint("Inward Bill Reprint"),
    InwardCancel("Inward Cancel"),
    InwardReturn("Inward Return"),
    InwardReactivate("Inward Reactivate"),
    InwardCheck("Inward Check"),
    InwardUnCheck("Inward Uncheck"),
    InwardAdmissionCancel("Inward Admission Cancel"),
    InwardFinalBillCancel("Inward Final Bill Cancel"),
    InwardOutSideMarkAsUnPaid("Inward Outside Mark As Unpaid"),
    ShowInwardFee("Show Inward Fee"),
    InwardPharmacyMenu("Inward Pharmacy Menu"),
    InwardPharmacyIssueRequest("Inward Pharmacy Issue Request"),
    InwardPharmacyIssueRequestCancel("Inward Pharmacy Issue Request Cancel"),
    InwardPharmacyIssueRequestSearch("Inward Pharmacy Issue Request Search"),
    InwardBillSettleWithoutCheck("Inward Bill Settle Without Check"),
    TheaterIssueBHT("Theater Issue BHT"),
    InpatientClinicalAssessment("Inpatient Clinical Assessment"),
    InpatientClinicalDischarge("Inpatient Clinical Discharge"),
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Nurse">
    NursingWorkBench("Nursing Work Bench"),
    ShowDrugCharges("Show Drug Charges"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Finance">
    PettyCashBillCancellationApprove("Petty Cash Bill Cancellation Approval"),
    PettyCashBillApprove("Petty Cash Bill Approval"),
    AllCashierSummery("All Cashier Summary"),
    CashTransactionCashIn("Cash Transaction Cash In"),
    CashTransactionCashOut("Cash Transaction Cash Out"),
    CashTransactionListToCashRecieve("Cash Transaction List to Cash Receive"),
    ShiftHandoverAcceptAsCashier("Shift Handover Accept As Cashier"),
    ShiftHandoverAcceptAsMainCashier("Shift Handover Accept As Main Cashier"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Lab">

    LabBilling("Lab Billing"),
    LabBillCancelSpecial("Lab Bill Cancel Special"),
    LabBillRefundSpecial("Lab Bill Refund Special"),
    LabCasheirBillSearch("Lab Cashier Bill Search"),
    LabCashier("Lab Cashier"),
    LabBillSearchCashier("Lab Bill Search Cashier"),
    LabBillSearch("Lab Bill Search"),
    LabBillItemSearch("Lab Bill Item Search"),
    LabBillCancelling("Lab Bill Cancelling"),
    CollectingCentreCancelling("Collecting Centre Cancelling"),
    LabBillReturning("Lab Bill Returning"),
    LabBillReprint("Lab Bill Reprint"),
    LabBillRefunding("Lab Bill Refunding"),
    LabBillReactivating("Lab Bill Reactivating"),
    LabSampleCollecting("Lab Sample Collecting"),
    LabSampleSending("Lab Sample Sending"),
    OutLabSampleSending("Out Lab Sample Sending"),
    LabSampleReceiving("Lab Sample Receiving"),
    LabSampleRejecting("Lab Sample Rejecting"),
    LabSampleSeparate("Lab Sample Separate"),
    LabSampleRetrieving("Receiving the Sent Sample"),
    LabReportFormatEditing("Lab Report Format Editing"),
    LabDataentry("Lab Data Entry"),
    LabAutherizing("Lab Authorizing"),
    LabDeAutherizing("Lab Deauthorizing"),
    LabRevertSample("Lab Revert Sample"),
    LabPrinting("Lab Printing"),
    LabReprinting("Lab Reprinting"),
    LabReportEdit("Lab Report Edit"),
    LabReportPrint("Lab Report Print"),
    AdminReportSearch("Admin Report Search"),
    LabReportSearchByDepartment("Lab Report Search by Department"),
    LabSummeries("Lab Summaries"),
    LabSummeriesLevel1("Lab Summaries Level 1"),
    LabSummeriesLevel2("Lab Summaries Level 2"),
    LabSummeriesLevel3("Lab Summaries Level 3"),
    LabReportSearchOwn("Lab Report Search Own"),
    LabReportSearchAll("Lab Report Search All"),
    LabReceive("Lab Receive"),
    LabEditPatient("Lab Edit Patient"),
    LabInvestigationFee("Lab Investigation Fee"),
    LabAddInwardServices("Lab Add Inward Services"),
    LabReportSearchByLoggedInstitution("Lab Report Search by Logged Institution"),
    LabReportSearchByLoggedDepartment("Lab Report Search by Logged Department"),
    IncomeReport("Income Report"),
    LabReport("Lab Report"),
    DuesAndAccess("Dues and Access"),
    CheckEnteredData("Check Entered Data"),
    LabAdiministrator("Lab Administrator"),
    LabReports("Lab Reports"),
    LabItems("Lab Items"),
    LabItemFeeUpadate("Lab Item Fee Update"),
    LabItemFeeDelete("Lab Item Fee Delete"),
    LabPatientDetailsEdit("Lab Patient Details Edit"),
    LabLists("Lab Lists"),
    LabSetUp("Lab Setup"),
    LabInwardBilling("Lab Inward Billing"),
    LabInwardSearchServiceBill("Lab Inward Search Service Bill"),
    LabCollectingCentreBilling("Lab Collecting Centre Billing"),
    LabCCBilling("Lab CC Billing"),
    LabCCBillingSearch("Lab CC Billing Search"),
    LabReportSearch("Lab Report Search"),
    LabReporting("Lab Reporting"),
    // Don't remove
    LabSearchBillLoggedInstitution("Lab Search Bill Logged Institution"),
    DashBoardMenu("DashBoard Menu"),
    DashBoardBillSearch("DashBoard Bill Search"),
    DashBoardWorksheet("DashBoard WorkSheet"),
    DashBoardSampleSearch("DashBoard Sample Search"),
    DashBoardInvestigationSearch("DashBoard Investigation Search"),
    DashBoardReportSearch("DashBoard Report Search"),
    DashBoardPatientReportSearch("DashBoard Patient Report Search"),
    AccessLabTestHistory("Access Lab Test History"),
    
    DoctorDashBoardMenu("Doctor DashBoard Menu"),
    
    
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Pharmacy">

    PaymentBilling("Payment Billing"),
    PaymentBillSearch("Payment Bill Search"),
    PaymentBillReprint("Payment Bill Reprint"),
    PaymentBillCancel("Payment Bill Cancel"),
    PaymentBillRefund("Payment Bill Refund"),
    PaymentBillReactivation("Payment Bill Reactivation"),
    ReportsSearchCashCardOwn("Reports Search Cash/Card Own"),
    ReportsSearchCreditOwn("Reports Search Credit Own"),
    ReportsItemOwn("Reports Item Own"),
    ReportsSearchCashCardOther("Reports Search Cash/Card Other"),
    ReportSearchCreditOther("Report Search Credit Other"),
    ReportsItemOther("Reports Item Other"),
    PharmacyOrderCreation("Pharmacy Order Creation"),
    PharmacyOrderApproval("Pharmacy Order Approval"),
    PharmacyOrderCancellation("Pharmacy Order Cancellation"),
    PharmacySaleWithoutStock("Pharmacy Sale Without Stock"),
    PharmacySaleReprint("Pharmacy Sale Reprint"),
    PrintOriginalPharmacyBillFromReprint("Print Original Pharmacy Bill From Reprint"),
    PharmacySaleCancel("Pharmacy Sale Cancel"),
    PharmacySaleReturn("Pharmacy Sale Return"),
    // Wholesale
    PharmacySaleWh("Pharmacy Sale Wholesale"),
    PharmacySaleReprintWh("Pharmacy Sale Reprint Wholesale"),
    PharmacySaleCancelWh("Pharmacy Sale Cancel Wholesale"),
    PharmacySaleReturnWh("Pharmacy Sale Return Wholesale"),
    // End wholesale
    PharmacyInwardBilling("Pharmacy Inward Billing"),
    PharmacyInwardBillingCancel("Pharmacy Inward Billing Cancel"),
    PharmacyInwardBillingReturn("Pharmacy Inward Billing Return"),
    PharmacyGoodReceive("Pharmacy Good Receive"),
    // Wholesale
    PharmacyGoodReceiveWh("Pharmacy Good Receive Wholesale"),
    // End Wholesale
    PharmacyGoodReceiveCancel("Pharmacy Good Receive Cancel"),
    PharmacyGoodReceiveReturn("Pharmacy Good Receive Return"),
    PharmacyGoodReceiveEdit("Pharmacy Good Receive Edit"),
    PharmacyPurchase("Pharmacy Purchase"),
    // Wholesale
    PharmacyPurchaseWh("Pharmacy Purchase Wholesale"),
    PharmacyTokenManagement("Pharmacy Token Management"),
    PharmacyDonation("Pharmacy Donation"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Theatre">

    // Theater
    Theatre("Theater"),
    TheatreAddSurgery("Theater Add Surgery"),
    TheatreBilling("Theater Billing"),
    TheaterTransfer("Theater Transfer"),
    TheaterTransferRequest("Theater Transfer Request"),
    TheaterTransferIssue("Theater Transfer Issue"),
    TheaterTransferRecieve("Theater Transfer Receive"),
    TheaterTransferReport("Theater Transfer Report"),
    TheaterReports("Theater Reports"),
    TheaterSummeries("Theater Summaries"),
    TheaterIssue("Theater Issue"),
    TheaterIssuePharmacy("Theater Issue Pharmacy"),
    TheaterIssueStore("Theater Issue Store"),
    TheaterIssueStoreBhtBilling("Theater Issue Store BHT Billing"),
    TheaterIssueStoreBhtSearchBill("Theater Issue Store BHT Search Bill"),
    TheaterIssueStoreBhtSearchBillItem("Theater Issue Store BHT Search Bill Item"),
    TheaterIssueOpd("Theater Issue OPD"),
    TheaterIssueOpdForCasheir("Theater Issue OPD for Cashier"),
    TheaterIssueOpdSearchPreBill("Theater Issue OPD Search Pre Bill"),
    TheaterIssueOpdSearchPreBillForReturnItemOnly("Theater Issue OPD Search Pre Bill for Return Item Only"),
    TheaterIssueOpdSearchPreBillReturn("Theater Issue OPD Search Pre Bill Return"),
    TheaterIssueOpdSearchPreBillAddToStock("Theater Issue OPD Search Pre Bill Add to Stock"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Stores">
    StoreIssue("Store Issue"),
    StoreIssueInwardBilling("Store Issue Inward Billing"),
    StoreIssueSearchBill("Store Issue Search Bill"),
    StoreIssueBillItems("Store Issue Bill Items"),
    StorePurchase("Store Purchase"),
    StorePurchaseOrder("Store Purchase Order"),
    StorePurchaseOrderApprove("Store Purchase Order Approve"),
    StorePurchaseOrderApproveSearch("Store Purchase Order Approve Search"),
    StorePurchaseGRNRecive("Store Purchase GRN Receive"),
    StorePurchaseGRNReturn("Store Purchase GRN Return"),
    StorePurchasePurchase("Store Purchase"),
    StoreTransfer("Store Transfer"),
    StoreTransferRequest("Store Transfer Request"),
    StoreTransferIssue("Store Transfer Issue"),
    StoreTransferRecive("Store Transfer Receive"),
    StoreTransferReport("Store Transfer Report"),
    StoreAdjustment("Store Adjustment"),
    StoreAdjustmentDepartmentStock("Store Adjustment Department Stock"),
    StoreAdjustmentStaffStock("Store Adjustment Staff Stock"),
    StoreAdjustmentPurchaseRate("Store Adjustment Purchase Rate"),
    StoreAdjustmentSaleRate("Store Adjustment Sale Rate"),
    StoreDealorPayment("Store Dealer Payment"),
    StoreDealorPaymentDueSearch("Store Dealer Payment Due Search"),
    StoreDealorPaymentDueByAge("Store Dealer Payment Due by Age"),
    StoreDealorPaymentPayment("Store Dealer Payment Payment"),
    StoreDealorPaymentPaymentGRN("Store Dealer Payment Payment GRN"),
    StoreDealorPaymentPaymentGRNSelect("Store Dealer Payment Payment GRN Select"),
    StoreDealorPaymentGRNDoneSearch("Store Dealer Payment GRN Done Search"),
    StoreSearch("Store Search"),
    StoreReports("Store Reports"),
    StoreSummery("Store Summary"),
    StoreAdministration("Store Administration"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Channel">
    ChannelAdd("Channel Add"),
    ChannelCancel("Channel Cancel"),
    ChannelRefund("Channel Refund"),
    ChannelReturn("Channel Return"),
    ChannelView("Channel View"),
    ChannelDoctorPayments("Channel Doctor Payments"),
    ChannelDoctorPaymentCancel("Channel Doctor Payment Cancel"),
    ChannelViewHistory("Channel View History"),
    ChannelCreateSessions("Channel Create Sessions"),
    ChannelCreateSpecialSessions("Channel Create Special Sessions"),
    ChannelManageSessions("Channel Manage Sessions"),
    ChannelAdministration("Channel Administration"),
    ChannelAgencyReports("Channel Agency Reports"),
    ChannellingChannelBooking("Channelling Channel Booking"),
    ChannellingFutureChannelBooking("Channelling Future Channel Booking"),
    ChannellingPastBooking("Channelling Past Booking"),
    ChannellingBookedList("Channelling Booked List"),
    ChannellingDoctorLeave("Channelling Doctor Leave"),
    ChannellingDoctorLeaveByDate("Channelling Doctor Leave by Date"),
    ChannellingDoctorLeaveByServiceSession("Channelling Doctor Leave by Service Session"),
    ChannellingChannelSheduling("Channelling Channel Scheduling"),
    ChannellingSpecialChannelSheduling("Channelling Special Channel Scheduling"),
    ChannellingChannelShedulRemove("Channelling Channel Schedule Remove"),
    ChannellingChannelShedulName("Channelling Channel Schedule Name"),
    ChannellingChannelShedulStartingNo("Channelling Channel Schedule Starting No"),
    ChannellingChannelShedulRoomNo("Channelling Channel Schedule Room No"),
    ChannellingChannelShedulMaxRowNo("Channelling Channel Schedule Max Row No"),
    ChannellingChannelAgentFee("Channelling Channel Agent Fee"),
    ChannellingDoctorSessionView("Channelling Doctor Session View"),
    ChannellingPayment("Channelling Payment"),
    ChannellingPaymentPayDoctor("Channelling Payment Pay Doctor"),
    ChannellingPaymentDueSearch("Channelling Payment Due Search"),
    ChannellingPaymentDoneSearch("Channelling Payment Done Search"),
    ChannellingApoinmentNumberCountEdit("Channelling Appointment Number Count Edit"),
    ChannellingEditSerialNo("Channelling Edit Serial No"),
    ChannellingEditPatientDetails("Channelling Edit Patient Details"),
    ChannellingPrintInPastBooking("Channelling Print in Past Booking"),
    ChannellingEditCreditLimitUserLevel("Channelling Edit Credit Limit User Level"),
    ChannellingEditCreditLimitAdminLevel("Channelling Edit Credit Limit Admin Level"),
    ChannellingReprintOriginalBill("Channelling Reprint Original Bill"),
    ChannellingPastBookingPatientAttend("Channelling Past Booking Patient Attend"),
    ChannelReports("Channel Reports"),
    ChannelSummery("Channel Summary"),
    ChannelManagement("Channel Management"),
    ChannelAgencyAgencies("Channel Agency Agencies"),
    ChannelAgencyCreditLimitUpdate("Channel Agency Credit Limit Update"),
    ChannelAgencyCreditLimitUpdateBulk("Channel Agency Credit Limit Update Bulk"),
    ChannelAddChannelBookToAgency("Channel Add Channel Book to Agency"),
    ChannelManageSpecialities("Channel Manage Specialities"),
    ChannelManageConsultants("Channel Manage Consultants"),
    ChannelEditingAppoinmentCount("Channel Editing Appointment Count"),
    ChannelAddChannelingConsultantToInstitutions("Channel Add Channelling Consultant to Institutions"),
    ChannelFeeUpdate("Channel Fee Update"),
    ChannelCrdeitNote("Channel Credit Note"),
    ChannelCrdeitNoteSearch("Channel Credit Note Search"),
    ChannelDebitNote("Channel Debit Note"),
    ChannelDebitNoteSearch("Channel Debit Note Search"),
    ChannelCashCancelRestriction("Channel Cash Cancel Restriction"),
    ChannelBookingChange("Channel Booking Change"),
    ChannelBookingBokking("Channel Booking Booking"),
    ChannelBookingReprint("Channel Booking Reprint"),
    ChannelBookingCancel("Channel Booking Cancel"),
    ChannelBookingRefund("Channel Booking Refund"),
    ChannelBookingSettle("Channel Booking Settle"),
    ChannelBookingSearch("Channel Booking Search"),
    ChannelBookingViews("Channel Booking Views"),
    ChannelBookingDocPay("Channel Booking Doc Pay"),
    ChannelBookingRestric("Channel Booking Restrict"),
    ChannelCashierTransaction("Channel Cashier Transaction"),
    ChannelCashierTransactionIncome("Channel Cashier Transaction Income"),
    ChannelCashierTransactionIncomeSearch("Channel Cashier Transaction Income Search"),
    ChannelCashierTransactionExpencess("Channel Cashier Transaction Expenses"),
    ChannelCashierTransactionExpencessSearch("Channel Cashier Transaction Expenses Search"),
    ChannelActiveVat("Channel Active VAT"),
    
    ChannelSessionMultipleDeletion("Delete Multiple Channel Sessions"),
    ChannelSessionHolidayMark("Channel Sessions Holidays Mark"),
    ChannelSessionManagement("Channel Session Management"),
    ChannelSheduleManagement("Channel Shedule Management"),
    ChannelBookingByMonth("Channel Booking by Month"),
    ChannelDoctorCard("Doctor Card"),
    ChannelPatientPortal("Patient Portal"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Clinis">
    ClinicSession("Clinic Session"),
    ClinicCalendar("Clinic Calendar"),
    ClinicQueue("Clinic Queue"),
    ClinicDisplay("Clinic Display"),
    ClinicSchedule("Clinic Schedule"),
    ClinicReports("Clinic Reports"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Memberships">
    MemberShip("Membership"),
    MemberShipAdd("Membership Add"),
    MemberShipSearch("Membership Search"),
    MemberShipEdit("Membership Edit"),
    MembershipReports("Membership Reports"),
    MembershipDiscountManagement("Membership Discount Management"),
    MembershipAdministration("Membership Administration"),
    MembershipSchemes("Membership Schemes"),
    MemberShipInwardMemberShip("Inward Membership"),
    MemberShipInwardMemberShipSchemesDicounts("Inward Membership Schemes Discounts"),
    MemberShipInwardMemberShipInwardMemberShipReport("Inward Membership Report"),
    MemberShipOpdMemberShipDis("OPD Membership Discount"),
    MemberShipOpdMemberShipDisByDepartment("OPD Membership Discount by Department"),
    MemberShipOpdMemberShipDisByCategory("OPD Membership Discount by Category"),
    MemberShipOpdMemberShipDisOpdMemberShipReport("OPD Membership Report"),
    MemberShipMemberDeActive("Membership Deactivate"),
    MemberShipMemberReActive("Membership Reactivate"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="HR">
    HrAdmin("HR Admin"),
    HrReports("HR Reports"),
    HrReportsLevel1("HR Reports Level 1"),
    HrReportsLevel2("HR Reports Level 2"),
    HrReportsLevel3("HR Reports Level 3"),
    EmployeeHistoryReport("Employee History Report"),
    hrDeleteLateLeave("HR Delete Late Leave"),
    HrGenerateSalary("HR Generate Salary"),
    HrGenerateSalarySpecial("HR Generate Salary Special"),
    HrAdvanceSalary("HR Advance Salary"),
    HrPrintSalary("HR Print Salary"),
    HrWorkingTime("HR Working Time"),
    HrRosterTable("HR Roster Table"),
    HrUploadAttendance("HR Upload Attendance"),
    HrAnalyseAttendenceByRoster("HR Analyse Attendance by Roster"),
    HrAnalyseAttendenceByStaff("HR Analyse Attendance by Staff"),
    HrForms("HR Forms"),
    HrLeaveForms("HR Leave Forms"),
    HrAdditionalForms("HR Additional Forms"),
    HrEditRetiedDate("HR Edit Retired Date"),
    HrRemoveResignDate("HR Remove Resign Date"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Clinical">
    ClinicalPatientSummery("Clinical Patient Summary"),
    ClinicalPatientDetails("Clinical Patient Details"),
    ClinicalPatientPhoto("Clinical Patient Photo"),
    ClinicalVisitDetail("Clinical Visit Detail"),
    ClinicalVisitSummery("Clinical Visit Summary"),
    ClinicalHistory("Clinical History"),
    ClinicalAdministration("Clinical Administration"),
    ClinicalPatientDelete("Clinical Patient Delete"),
    ClinicalAdministrationEditLetter("Clinical Administration Edit Letter"),
    ClinicalPatientAdd("Clinical Patient Add"),
    ClinicalPatientEdit("Clinical Patient Edit"),
    ClinicalPatientCommentsView("Clinical Patient Comments View"),
    ClinicalPatientCommentsEdit("Clinical Patient Comments Edit"),
    ClinicalPatientNameChange("Clinical Patient Name Change"),
    ClinicalMembershipAdd("Clinical Membership Add"),
    ClinicalMembershipEdit("Clinical Membership Edit"),
    ClinicalPatientPhoneNumberEdit("Clinical Patient Phone Number Edit"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Pharmacy">
    // Pharmacy Disbursement
    PharmacyDisburesementMenu("Pharmacy Disbursement Menu"),
    PharmacyDisbursementRequest("Pharmacy Disbursement Request"),
    PharmacyDisbursementFinalizeRequest("Pharmacy Disbursement Finalize Request"),
    PharmacyDisbursementIssurForRequest("Pharmacy Disbursement Issue for Request"),
    PharmacyDisbursementDirectIssue("Pharmacy Disbursement Direct Issue"),
    PharmacyDisbursementRecieve("Pharmacy Disbursement Receive"),
    PharmacyDisbursementReports("Pharmacy Disbursement Reports"),
    PharmacyDisbursementRequestApproval("Pharmacy Disbursement Request Approval"),
    // Pharmacy Inpatient medication management
    InpatientMedicationManagementMenue("Inpatient Medication Management Menu"),
    PharmacyDirectIssueToBht("Pharmacy Direct Issue to BHT"),
    PharmacyDirectIssueToTheaterCases("Pharmacy Direct Issue to Theater Cases"),
    PharmacyBhtIssueRequest("Pharmacy BHT Issue Request"),
    PharmacySearchInpatientDirectIssuesbyBill("Pharmacy Search Inpatient Direct Issues by Bill"),
    PharmacySearchInpatientDirectIssuesbyItem("Pharmacy Search Inpatient Direct Issues by Item"),
    PharmacySearchInpatientDirectIssueReturnsbyBill("Pharmacy Search Inpatient Direct Issue Returns by Bill"),
    PharmacysSearchInpatientDirectIssueReturnsbyItem("Pharmacy Search Inpatient Direct Issue Returns by Item"),
    // Pharmacy Retail Transactions
    PharmacyRetailTransactionMenue("Pharmacy Retail Transaction Menu"),
    PharmacyRetailTransaction("Pharmacy Retail Transaction"),
    PharmacySale("Pharmacy Sale"),
    PharmacySaleQuick("Pharmacy Sale - Quick"),
    PharmacySaleForCashier("Pharmacy Sale for Cashier"),
    PharmacySaleForCashierQuick("Pharmacy Sale for Cashier - Quick"),
    PharmacySaleWithOutStock("Pharmacy Sale without Stock"),
    PharmacySearchSaleBill("Pharmacy Search Sale Bill"),
    PharmacySearchSalePreBill("Pharmacy Search Sale Pre-Bill"),
    PharmacySearchSaleBillItems("Pharmacy Search Sale Bill Items"),
    PharmacyReturnItemsOnly("Pharmacy Return Items Only"),
    PharmacyReturnItemsAndPayments("Pharmacy Return Items and Payments"),
    PharmacySearchReturnBill("Pharmacy Search Return Bill"),
    PharmacyAddToStock("Pharmacy Add to Stock"),
    // Pharmacy Wholesale Transaction
    PharmacyWholeSaleTransactionMenue("Pharmacy Wholesale Transaction Menu"),
    PharmacyWholeSaleTransaction("Pharmacy Wholesale Transaction"),
    PharmacyWholesaleSale("Pharmacy Wholesale Sale"),
    PharmacyWholesaleSaleForCashier("Pharmacy Wholesale Sale for Cashier"),
    PharmacyWholesaleSearchSaleBill("Pharmacy Wholesale Search Sale Bill"),
    PharmacyWholesaleSearchSaleBillToPay("Pharmacy Wholesale Search Sale Bill to Pay"),
    PharmacyWholesaleSearchSaleBillItems("Pharmacy Wholesale Search Sale Bill Items"),
    PharmacyWholesaleReturnItemsOnly("Pharmacy Wholesale Return Items Only"),
    PharmacyWholesaleWholeSaleAddToStock("Pharmacy Wholesale Add to Stock"),
    PharmacyWholeSalePurchase("Pharmacy Wholesale Purchase"),
    PharmacySearchReturnBillItems("Pharmacy Search Return Bill Items"),
    // Pharmacy Disposal
    PharmacyDisposalMenue("Pharmacy Disposal Menu"),
    PharmacyDisposalIssue("Pharmacy Disposal Issue"),
    PharmacyDisposalSearchIssueBill("Pharmacy Disposal Search Issue Bill"),
    PharmacyDisposalSearchIssueBillItems("Pharmacy Disposal Search Issue Bill Items"),
    PharmacyDisposalSearchIssueReturnBill("Pharmacy Disposal Search Issue Return Bill"),
    PharmacyDisposalUnitIssueMargin("Pharmacy Disposal Unit Issue Margin"),
    // Pharmacy Adjustment
    PharmacyAdjustmentMenue("Pharmacy Adjustment Menu"),
    PharmacyAdjustmentDepartmentStockQTY("Pharmacy Adjustment Department Stock Quantity"),
    PharmacyAdjustmentDepartmentStockBySingleItemQTY("Pharmacy Adjustment Department Stock by Single Item Quantity"),
    PharmacyAdjustmentStaffStockAdjustment("Pharmacy Adjustment Staff Stock Adjustment"),
    PharmacyAdjustmentPurchaseRate("Pharmacy Adjustment Purchase Rate"),
    PharmacyAdjustmentCostRate("Pharmacy Adjustment Cost Rate"),
    PharmacyAdjustmentSaleRate("Pharmacy Adjustment Sale Rate"),
    PharmacyAdjustmentWholeSaleRate("Pharmacy Adjustment Wholesale Rate"),
    PharmacyAdjustmentExpiryDate("Pharmacy Adjustment Expiry Date"),
    PharmacyAdjustmentSearchAdjustmentBills("Pharmacy Adjustment Search Adjustment Bills"),
    PharmacyAdjustmentTransferAllStock("Pharmacy Adjustment Transfer All Stock"),
    PharmacyAdjustmentCreateBatch("Pharmacy Adjustment Create Batch"),
    PharmacyPhysicalCountApprove("Pharmacy Physical Count Approve"),
    PharmacyStockTakeApprove("Pharmacy Stock Take Approve"),
    // Pharmacy Dealer Payments
    PharmacyDealerPaymentMenue("Pharmacy Dealer Payment Menu"),
    PharmacyDealerDueSearch("Pharmacy Dealer Due Search"),
    PharmacyDealerDueByAge("Pharmacy Dealer Due by Age"),
    PharmacyPayment("Pharmacy Payment"),
    PharmacyGRNPaymentApprove("Pharmacy GRN Payment Approve"),
    PharmacyGRNPaymentDoneSearch("Pharmacy GRN Payment Done Search"),
    PharmacyCreditDueAndAccess("Pharmacy Credit Due and Access"),
    // Whalesale
    PharmacyWholesaleMenue("Pharmacy Wholesale Menu"),
    PharmacyPurchaseReprint("Pharmacy Purchase Reprint"),
    PharmacyPurchaseCancellation("Pharmacy Purchase Cancellation"),
    PharmacyPurchaseReturn("Pharmacy Purchase Return"),
    PharmacyStockAdjustment("Pharmacy Stock Adjustment"),
    PharmacyStockAdjustmentSingleItem("Pharmacy Stock Adjustment Single Item"),
    PharmacyReAddToStock("Pharmacy Re-Add to Stock"),
    PharmacyStockIssue("Pharmacy Stock Issue"),
    PharmacyDealorPayment("Pharmacy Dealer Payment"),
    PharmacySearch("Pharmacy Search"),
    PharmacyReports("Pharmacy Reports"),
    PharmacyTransfer("Pharmacy Transfer"),
    PharmacyTransferViewRates("Pharmacy Transfer View Rates"),
    NursingIPBillingViewRates("Nursing IP Billing View Rates"),
    IPRequestViewRates("IP Request View Rates"),
    StockRequestViewRates("Stock Request View Rates"),
    ConsumptionViewRates("Consumption View Rates"),
    StockTransactionViewRates("Stock Transaction View Rates"),
    DiscardViewRates("Discard View Rates"),
    PharmacySummery("Pharmacy Summary"),
    PharmacyAdministration("Pharmacy Administration"),
    PharmacySetReorderLevel("Pharmacy Set Reorder Level"),
    PharmacyReturnWithoutTraising("Pharmacy Return without Traising"),
    PharmacyBHTIssueAccept("Pharmacy BHT Issue Accept"),
    // Pharmacy Procurement
    PharmacyProcurementMenu("Pharmacy Procurement Menu"),
    CreatePurchaseOrder("Create Purchase Order"),
    AutoOrderPModel("Auto Order P Model"),
    AutoOrderQModal("Auto Order Q Model"),
    DirectPurchase("Direct Purchase"),
    PurchaseOrdersApprovel("Purchase Orders Approval"),
    TransferReciveApproval("Transfer Receive Approval"),
    GoodsRecipt("Goods Receipt"),
    ReturnReceviedGoods("Return Received Goods"),
    CreateGrnReturn("Create GRN Return"),
    FinalizeGrnReturn("Finalize GRN Return"),
    ApproveGrnReturn("Approve GRN Return"),
    CreateDisposalReturn("Create Disposal Return"),
    FinalizeDisposalReturn("Finalize Disposal Return"),
    ApproveDisposalReturn("Approve Disposal Return"),
    ViewDisposalReturn("View Disposal Return"),
    CreateDirectPurchaseReturn("Create Direct Purchase Return"),
    FinalizeDirectPurchaseReturn("Finalize Direct Purchase Return"),
    ApproveDirectPurchaseReturn("Approve Direct Purchase Return"),
    ReturnWithoutRecipt("Return without Receipt"),
    PharmacyReturnWithoutReceiptBill("Pharmacy Return Without Receipt Bill"),
    PharmacyGrnSave("Pharmacy GRN Save"),
    PharmacyGrnFinalize("Pharmacy GRN Finalize"),
    PharmacyGrnApprove("Pharmacy GRN Approve"),
    PharmacyItemSearch("Pharmacy Item Search"),
    PharmacyGenarateReports("Pharmacy Generate Reports"),
    PharmacySummaryViews("Pharmacy Summary Views"),
    PrintOriginalPoBillFromReprint("Print Original PO Bill From Reprint"),
    PrintOriginalGrnBillFromReprint("Print Original GRN Bill From Reprint"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Optician">
    // Optician
    Optician("Optician"),
    OpticianPatientManagement("Optician Patient Management"),
    OpticianAppointmentManagement("Optician Appointment Management"),
    OpticianEmr("Optician EMR"),
    OpticianStockManagement("Optician Stock Management"),
    OpticianProductCatalog("Optician Product Catalog"),
    OpticianRepairManagement("Optician Repair Management"),
    @Deprecated
    Ophthalmology("Ophthalmology"),
    @Deprecated
    OphthalmologyPatientManagement("Ophthalmology Patient Management"),
    @Deprecated
    OphthalmologyAppointmentManagement("Ophthalmology Appointment Management"),
    @Deprecated
    OphthalmologyEmr("Ophthalmology EMR"),
    @Deprecated
    OphthalmologyStockManagement("Ophthalmology Stock Management"),
    @Deprecated
    OphthalmologyProductCatalog("Ophthalmology Product Catalog"),
    @Deprecated
    OphthalmologyRepairManagement("Ophthalmology Repair Management"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Courier">
    Courier("Courier"),
    CourierCollectSamples("Courier Collect Samples"),
    CourierHandoverSamplesToLab("Courier Handover Samples to Lab"),
    CourierViewReports("Courier View Reports"),
    CourierPrintReports("Courier Print Reports"),
    CourierViewStatistics("Courier View Statistics"),
    CourierViewBillReports("Courier View Bill Reports"),
    CourierViewPaymentReports("Courier View Payment Reports"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Collecting Centre">
    CollectingCentreManageMenu("Collecting Centre Manage Menu"),
    CollectingCentreBilling("Collecting Centre Billing"),
    CCPaymentReceive("CC Payment Receive"),
    SearchCCPaymentReceive("Search CC Payment Receive"),
    IssueReferenceBook("CC Issue Reference Book"),
    SearchIssuedReferenceBook("Search CC Reference Book"),
    ChangeCreditLimitInCC("Change CC Credit Limit"),
    PayCollectingCentre("Pay Collecting Centre"),
    CollectingCentreCreditDebitNoteMenu("CC Credit/Debit Note Menu"),
    CollectingCentreCreditNote("CC Credit Note"),
    CollectingCentreDebitNote("CC Debit Note"),
    CollectingCentreReports("CC Reports"),
    ChangeCollectingCentre("Change Collecting Centre"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="User">
    UserMenu("User Menu"),
    ChangeMyPassword("Change My Password"),
    ChangeMyTheme("Change My Theme"),
    ChangePreferece("Change My Preference"),
    ChangeMyApiKeys("Change My Api Keys"),
    ChangeReceiptPrintingPaperTypes("Change Receipt Printing Paper Types"),
    MyFinanacialTransactionManager("My Finanacial Transaction Manager"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Admin">
    SearchGrand("Search Grand"),
    AdminManagingUsers("Admin Managing Users"),
    AdminInstitutions("Admin Institutions"),
    AdminStaff("Admin Staff"),
    AdminItems("Admin Items"),
    AdminPrices("Admin Prices"),
    ManageCreditCompany("Manage Credit Company"),
    AdminFilterWithoutDepartment("Admin Filter Without Department"),
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Approval">
    BillCancelRequestApproval("Bill Cancel Request Approval"),
    ItemRefundRequestApproval("Item Refund Request Approval"),
    DrawerAdjustmentRequestApproval("Drawer Adjustment Request Approval"),
    DrawerAdjustmentDirect("Drawer Adjustment Direct (No Approval)"),
    PettyCashCancellationApproval("Petty-Cash Cancellation Approval"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Float Transfer">
    IssueFundTransfer("Issue Float Transfer"),
    ReceiveFundTransfer("Receive Float Transfer"),
    DeclineFundTransfer("Decline Float Transfer"),
    RequestFundTransfer("Request Float Transfer"),
    ProcessFundTransferRequest("Process Float Transfer Request"),
    CancelOwnFundTransfer("Cancel Own Float Transfer"),
    CancelOthersFundTransfer("Cancel Others Float Transfer"),
    ViewFundTransferReports("View Float Transfer Reports"),
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Developers">
    Developers("Developers"),
    // Administration
    SearchAll("Search All"),
    SendBulkSMS("Send Bulk SMS"),
    SuperAdmin("Supper Admin"),
    Reactivate("Reactivate"),
    EditData("Edit Data"),
    DeleteData("Delete Data"),
    BillCancel("Bill Cancel"),
    BillRefund("Bill Refund"), //</editor-fold>
    AiChat("AI Chat"),
    // <editor-fold defaultstate="collapsed" desc="Synced from development - missing privileges">
    AdminInactivePatients("Manage Inactive Patients"),
    AdminPatientRelationships("Manage Patient Relationships"),
    ArchiveOldItemBatch("Archive Old ItemBatch Records"),
    ArchiveOldStockHistory("Archive Old StockHistory Records"),
    CashierHandoverStatusReport("Cashier Handover Status Report"),
    ClientPortalCreateAccount("Create Client Portal Account"),
    ClinicalPatientBlacklist("Clinical Patient Blacklist"),
    ClinicalPatientPseudonymise("Clinical Patient Pseudonymise"),
    CollectingCentreReprintOriginalBill("Collecting Centre Reprint Original Bill"),
    InpatientDashboardPanelAdmission("Inpatient Dashboard - Admission Panel"),
    InpatientDashboardPanelBilling("Inpatient Dashboard - Billing Panel"),
    InpatientDashboardPanelClinicalData("Inpatient Dashboard - Clinical Data Panel"),
    InpatientDashboardPanelDocuments("Inpatient Dashboard - Documents Panel"),
    InpatientDashboardPanelOperationTheatre("Inpatient Dashboard - Operation Theatre Panel"),
    InpatientDashboardPanelPackage("Inpatient Dashboard - Package Panel"),
    InpatientDashboardPanelPharmaceuticals("Inpatient Dashboard - Pharmaceuticals Panel"),
    InpatientDashboardPanelReports("Inpatient Dashboard - Reports Panel"),
    InpatientDashboardPanelRoomManagement("Inpatient Dashboard - Room Management Panel"),
    InpatientDashboardPanelServices("Inpatient Dashboard - Services Panel"),
    InpatientLetter("Inpatient Letter"),
    InwardAddChargesAfterNursingDischarge("Inward Add Charges After Nursing Discharge"),
    InwardClinicalNotesView("Inward Clinical Notes View"),
    InwardDiagnosisCardView("Inward Diagnosis Card View"),
    InwardDischargeMedicationsView("Inward Discharge Medications View"),
    InwardDoctorPaymentAccess("Inward Doctor Payment Access"),
    InwardDocumentUpload("Inward Document Upload"),
    InwardEditPatientDetailsFromAdmission("Inward Edit Patient Details From Admission"),
    InwardEditPaymentDetails("Inward Edit Payment Details"),
    InwardEventHistoryView("Inward Event History View"),
    InwardFinalBillApprove("Inward Final Bill Approve"),
    InwardFinalBillCreateVersion("Inward Final Bill Create New Version"),
    InwardFinalBillEmail("Inward Final Bill Email"),
    InwardFinalBillRetire("Inward Final Bill Retire"),
    InwardFinalBillSetConfirmed("Inward Final Bill Set As Confirmed"),
    InwardFinalBillViewUnapproved("Inward Final Bill View / Print Unapproved"),
    InwardFormFill("Inward Form Fill"),
    InwardFormTemplateAdmin("Inward Form Template Admin"),
    InwardHoldProfessionalPayments("Hold Professional Payments"),
    InwardImagesView("Inward Images View"),
    InwardInvestigationsView("Inward Investigations View"),
    InwardLaboratory("Inward Laboratory"),
    InwardLaboratoryBarcodeGeneration("Inward Laboratory Barcode Generation"),
    InwardLaboratoryReportSearch("Inward Laboratory Report Search"),
    InwardLaboratorySampleManagement("Inward Laboratory Sample Management"),
    InwardMakeDepositAccess("Inward Make Deposit Access"),
    InwardManageAllergies("Inward Manage Allergies"),
    InwardNursingDischarge("Inward Nursing Discharge"),
    InwardPackageAdministration("Inward Package Administration"),
    InwardPackageAdmission("Inward Package Admission"),
    InwardPackageChange("Inward Package Change"),
    InwardPatientHistoryView("Inward Patient History View"),
    InwardPayProfessionalFeesWhileOnHold("Pay Professional Fees While On Hold"),
    InwardPharmacyBhtReceive("Inward Pharmacy BHT Receive"),
    InwardPharmacyReturnCancel("Inward Pharmacy Return Cancel"),
    InwardPharmacyReturnSubmit("Inward Pharmacy Return Submit"),
    InwardPhysicalDischarge("Inward Physical Discharge"),
    InwardPostDischargeReports("Inward Post-Discharge Reports"),
    InwardPostFinalPaymentAccess("Inward Post Final Payment Access"),
    InwardProcessCancelAfterNursingDischarge("Inward Process Cancel After Nursing Discharge"),
    InwardProcessReturnAfterNursingDischarge("Inward Process Return After Nursing Discharge"),
    InwardReportLabBillSummary("Inward Report - Lab Bill Summary"),
    InwardReportLabResultSummary("Inward Report - Lab Result Summary"),
    InwardReportPaymentsAndCancellations("Inward Report - Payments & Cancellations"),
    InwardReportPharmacyAndServiceSummary("Inward Report - Pharmacy & Services Summary"),
    InwardReportPharmacyIssueSummary("Inward Report - Pharmacy Issue Summary"),
    InwardReportPharmacyIssueSummaryLegacy("Inward Report - Pharmacy Issue Summary (Legacy)"),
    InwardReportServiceBills("Inward Report - All Inward Service Bills"),
    InwardReportServiceSummary("Inward Report - Service Summary"),
    InwardSaveProvisionalFinalBill("Inward Save Provisional Final Bill"),
    InwardSearchAdmissionsByAdmittedDepartmentAnyInstitute("Inward Search Admissions By Admitted Department - Any Institute"),
    InwardSearchAdmissionsByAdmittedDepartmentLoggedDepartment("Inward Search Admissions By Admitted Department - Logged Department"),
    InwardSearchAdmissionsByAdmittedDepartmentLoggedInstitute("Inward Search Admissions By Admitted Department - Logged Institute"),
    InwardSearchAdmissionsByCurrentDepartmentAnyInstitute("Inward Search Admissions By Current Department - Any Institute"),
    InwardSearchAdmissionsByCurrentDepartmentLoggedDepartment("Inward Search Admissions By Current Department - Logged Department"),
    InwardSearchAdmissionsByCurrentDepartmentLoggedInstitute("Inward Search Admissions By Current Department - Logged Institute"),
    InwardSearchAdmissionsGeneralSearch("Inward Search Admissions - General Search (Date Range)"),
    InwardSendEmail("Inward Send Email"),
    InwardServiceItemRequestApproval("Inward Service/Item Request Approval"),
    InwardServiceItemRequestRejection("Inward Service/Item Request Rejection"),
    InwardSettleFinalBill("Inward Settle Final Bill"),
    InwardSurgeryAdd("Inward Surgery Add"),
    InwardSurgeryManage("Inward Surgery Manage"),
    InwardSurgeryValidate("Inward Surgery Validate"),
    InwardSurgeryValidationRevert("Inward Surgery Validation Revert"),
    InwardWardMedicationsView("Inward Ward Medications View"),
    IPBillingViewDiscount("IP Billing View Discount"),
    LabAnalyticsAllIncomeSummary("Lab Analytics - Laboratory All Income Summary"),
    LabAnalyticsAvgTurnAroundTime("Lab Analytics - Average Turn Around Time"),
    LabAnalyticsBillItemList("Lab Analytics - Bill Item List"),
    LabAnalyticsBillList("Lab Analytics - Bill List"),
    LabAnalyticsBillWiseTurnAroundTime("Lab Analytics - Bill-vice turn-around time"),
    LabAnalyticsByBilledDepartment("Lab Analytics - By Billed Department"),
    LabAnalyticsByBilledInstitution("Lab Analytics - By Billed Institution"),
    LabAnalyticsByOrderingInstitution("Lab Analytics - By Ordering Institution"),
    LabAnalyticsByReportedDepartment("Lab Analytics - By Reported Department"),
    LabAnalyticsByReportedInstitution("Lab Analytics - By Reported Institution"),
    LabAnalyticsCancelledBillSearch("Lab Analytics - Bills Cancelled after Approving Reports"),
    LabAnalyticsCancelledLabBillList("Lab Analytics - List of Cancelled Lab Bills"),
    LabAnalyticsCardIncomeReport("Lab Analytics - Laboratary Card Income Report"),
    LabAnalyticsClientList("Lab Analytics - Client List"),
    LabAnalyticsCollectionCentreCount("Lab Analytics - Report by Collection Centre Count"),
    LabAnalyticsCollectionCentreCountSummary("Lab Analytics - Report by Collection Centre Count(Summary)"),
    LabAnalyticsCollectionCentreDetail("Lab Analytics - Report by Collection Centre(Detail)"),
    LabAnalyticsCollectionCentreSummary("Lab Analytics - Report by Collection Centre(Summary)"),
    LabAnalyticsDailyInwardFeeSummary("Lab Analytics - Daily Inward Fee Summary"),
    LabAnalyticsDailyInwardFeeSummaryWithCounts("Lab Analytics - Daily Inward Fee Summary with Counts"),
    LabAnalyticsDailyLabSummaryByDepartment("Lab Analytics - Daily Lab Summmary By Department"),
    LabAnalyticsDailyLabSummaryByDepartmentDto("Lab Analytics - Daily Lab Summmary By Department (DTO)"),
    LabAnalyticsDailyOpdFeeSummary("Lab Analytics - Daily OPD Fee Summary"),
    LabAnalyticsDailyOpdFeeSummaryWithCounts("Lab Analytics - Daily OPD Fee Summary with Counts"),
    LabAnalyticsDailySummary("Lab Analytics - Daily Summary"),
    LabAnalyticsDailySummaryByBillTypes("Lab Analytics - Daily Summary By Bill Types"),
    LabAnalyticsDailySummaryInwardOpd("Lab Analytics - Daily Summary Inward and Opd"),
    LabAnalyticsDailySummaryInwardOpdByDate("Lab Analytics - Daily Summary Inward and Opd by Date"),
    LabAnalyticsDailySummaryInwardOpdCount("Lab Analytics - Daily Summary Inward and Opd Count"),
    LabAnalyticsIncomeReport("Lab Analytics - Laboratary Income Report"),
    LabAnalyticsIncomeSummary("Lab Analytics - Income Summary"),
    LabAnalyticsInvestigationList("Lab Analytics - Investigation List"),
    LabAnalyticsInvestigationSummaryFeeType("Lab Analytics - Investigation Summary Fee Type"),
    LabAnalyticsInvestigationSummaryFeeTypeWithCredit("Lab Analytics - Investigation Summary Fee Type With Credit"),
    LabAnalyticsInvestigationSummaryInward("Lab Analytics - Investigation Summary Inward"),
    LabAnalyticsInvestigationSummaryInwardByDate("Lab Analytics - Investigation Summary Inward by Date"),
    LabAnalyticsInvestigationSummaryRegentFee("Lab Analytics - Investigation Summary Regent Fee"),
    LabAnalyticsInvestigationSummaryRegentFeeByPayMethod("Lab Analytics - Investigation Summary Regent Fee By Payment Method"),
    LabAnalyticsInvestigationSummaryRegentFeeWithCredit("Lab Analytics - Investigation Summary Regent Fee With Credit"),
    LabAnalyticsInwardSummaryByAddedDate("Lab Analytics - Inward Lab Summary by Added Date"),
    LabAnalyticsInwardSummaryByAddedDateWithMargin("Lab Analytics - Inward Lab Summary by Added Date With Margin"),
    LabAnalyticsLaboratorySummary("Lab Analytics - Laboratory Summary"),
    LabAnalyticsOpdBillItemsForCreditCompanies("Lab Analytics - OPD Bill Items For Credit Companies"),
    LabAnalyticsOrderReport("Lab Analytics - Laboratory Order Report"),
    LabAnalyticsPriceList("Lab Analytics - Price List"),
    LabAnalyticsReferringDoctorDetail("Lab Analytics - Report by Referring Doctor(Details)"),
    LabAnalyticsReferringDoctorSummary("Lab Analytics - Report by Referring Doctor(Summary)"),
    LabAnalyticsReportSummaryByDay("Lab Analytics - Report Summary by day"),
    LabAnalyticsReportSummaryByMonthCashCredit("Lab Analytics - Report Summary by Month With Cash and Credit"),
    LabAnalyticsReportSummaryDepartment("Lab Analytics - Report Summary Department"),
    LabAnalyticsSampleList("Lab Analytics - Sample List"),
    LabAnalyticsSampleListDto("Lab Analytics - Sample List (DTO)"),
    LabAnalyticsTestResults("Lab Analytics - Test Results"),
    LabAnalyticsTestResultsSingle("Lab Analytics - Test Results - Single"),
    LabAnalyticsTestWiseCountReport("Lab Analytics - Test Wise Count Report"),
    LabAnalyticsTestWiseCountReportDto("Lab Analytics - Test Wise Count Report - DTO"),
    LabAnalyticsTestWiseReagentCostReport("Lab Analytics - Test Wise Reagent Cost Report"),
    MergePatients("Merge Patients"),
    NursingWorkBenchPanelClinicalData("Nursing Workbench - Clinical Data Panel"),
    NursingWorkBenchPanelEdit("Nursing Workbench - Edit Panel"),
    NursingWorkBenchPanelOperationTheatre("Nursing Workbench - Operation Theatre Panel"),
    NursingWorkBenchPanelPayments("Nursing Workbench - Payments Panel"),
    NursingWorkBenchPanelPharmaceuticals("Nursing Workbench - Pharmaceuticals Panel"),
    NursingWorkBenchPanelReports("Nursing Workbench - Reports Panel"),
    NursingWorkBenchPanelRoomManagement("Nursing Workbench - Room Management Panel"),
    NursingWorkBenchPanelService("Nursing Workbench - Service Panel"),
    PettyCashEditFinancialYear("Petty Cash Edit Financial Year"),
    PharmacyAnalyticsAfterStockTakingReport("Pharmacy Analytics - After Stock Taking Report"),
    PharmacyAnalyticsAllCashierReport("Pharmacy Analytics - All Cashier Report"),
    PharmacyAnalyticsAllCashierSummary("Pharmacy Analytics - All Cashier Summary"),
    PharmacyAnalyticsAllDepartmentSaleSummary("Pharmacy Analytics - All Department Sale Summary"),
    PharmacyAnalyticsAllItemMovementSummary("Pharmacy Analytics - All Item Movement Summary"),
    PharmacyAnalyticsAllItemsSummary("Pharmacy Analytics - All Items Summary"),
    PharmacyAnalyticsBatchBinCard("Pharmacy Analytics - Batch Bin Card"),
    PharmacyAnalyticsBatchStock("Pharmacy Analytics - Batch Stock"),
    PharmacyAnalyticsBeforeStockTakingReport("Pharmacy Analytics - Before Stock Taking Report"),
    PharmacyAnalyticsBHTIssueByBill("Pharmacy Analytics - BHT Issue - By Bill"),
    PharmacyAnalyticsBHTIssueByBillItem("Pharmacy Analytics - BHT Issue - By Bill Item"),
    PharmacyAnalyticsBHTIssueByItem("Pharmacy Analytics - BHT Issue - By Item"),
    PharmacyAnalyticsBHTIssueStaff("Pharmacy Analytics - BHT Issue - Staff"),
    PharmacyAnalyticsBHTIssueWithMarginReport("Pharmacy Analytics - BHT Issue With Margin Report"),
    PharmacyAnalyticsBillTypes("Pharmacy Analytics - Bill Types"),
    PharmacyAnalyticsBinCard("Pharmacy Analytics - Bin Card"),
    PharmacyAnalyticsCashierDetailedReportByDepartment("Pharmacy Analytics - Cashier Detailed Report by Department"),
    PharmacyAnalyticsCashierReport("Pharmacy Analytics - Cashier Report"),
    PharmacyAnalyticsCashierSummary("Pharmacy Analytics - Cashier Summary"),
    PharmacyAnalyticsCashInOutReport("Pharmacy Analytics - Cash In/Out Report"),
    PharmacyAnalyticsCategoryStockReport("Pharmacy Analytics - Category Stock Report"),
    PharmacyAnalyticsCategoryStockSummary("Pharmacy Analytics - Category Stock Summary"),
    PharmacyAnalyticsDailyStockValuesF15("Pharmacy Analytics - Daily Stock Values (F-15)"),
    PharmacyAnalyticsDepartmentStockByBatch("Pharmacy Analytics - Department Stock By Batch"),
    PharmacyAnalyticsDepartmentStockByBatchToUpload("Pharmacy Analytics - Department Stock By Batch to Upload"),
    PharmacyAnalyticsDepartmentViceStock("Pharmacy Analytics - Department Vice Stock"),
    PharmacyAnalyticsExpiringStock("Pharmacy Analytics - Expiring Stock"),
    PharmacyAnalyticsExpiryAdjustments("Pharmacy Analytics - Expiry adjustments"),
    PharmacyAnalyticsF15DrillDownLevel1("Pharmacy Analytics - F-15 Drill-Down (Level 1)"),
    PharmacyAnalyticsF9B("Pharmacy Analytics - F 9B"),
    PharmacyAnalyticsFastMoving("Pharmacy Analytics - Fast Moving"),
    PharmacyAnalyticsGRNBillItemReport("Pharmacy Analytics - GRN Bill Item Report"),
    PharmacyAnalyticsGRNPaymentSummary("Pharmacy Analytics - GRN Payment Summary"),
    PharmacyAnalyticsGRNPaymentSummaryBySupplier("Pharmacy Analytics - GRN Payment Summary By Supplier"),
    PharmacyAnalyticsGRNPurchaseItemsBySupplier("Pharmacy Analytics - GRN Purchase Items by Supplier"),
    PharmacyAnalyticsGRNRegistry("Pharmacy Analytics - GRN Registry"),
    PharmacyAnalyticsGRNReturnList("Pharmacy Analytics - GRN Return List"),
    PharmacyAnalyticsGRNSummary("Pharmacy Analytics - GRN Summary"),
    PharmacyAnalyticsGRNSummaryBySupplier("Pharmacy Analytics - GRN Summary By Supplier"),
    PharmacyAnalyticsIncomeSummaryCategory("Pharmacy Analytics - Income Summary Category"),
    PharmacyAnalyticsInstitutionItemMovement("Pharmacy Analytics - Institution Item Movement"),
    PharmacyAnalyticsItemBinCard("Pharmacy Analytics - Item Bin Card"),
    PharmacyAnalyticsItemsAMPList("Pharmacy Analytics - Items (AMP) List"),
    PharmacyAnalyticsItemStock("Pharmacy Analytics - Item Stock"),
    PharmacyAnalyticsItemsWithDistributor("Pharmacy Analytics - Items With Distributor"),
    PharmacyAnalyticsItemsWithMultipleDistributorItemsOnly("Pharmacy Analytics - Items With Multiple Distributor(Items Only)"),
    PharmacyAnalyticsItemsWithoutDistributor("Pharmacy Analytics - Items Without Distributor"),
    PharmacyAnalyticsItemsWithSuppliersAndPrices("Pharmacy Analytics - Items With Suppliers and Prices"),
    PharmacyAnalyticsItemWiseAdjustments("Pharmacy Analytics - Item-wise adjustments"),
    PharmacyAnalyticsItemWiseProcurement("Pharmacy Analytics - Item-wise Procurement"),
    PharmacyAnalyticsItemWithMultipleDistributor("Pharmacy Analytics - Item With Multiple Distributor"),
    PharmacyAnalyticsListOfPharmacyBills("Pharmacy Analytics - List of Pharmacy Bills"),
    PharmacyAnalyticsMedicineVTMATMVMPAMPVMPPAndAMPPList("Pharmacy Analytics - Medicine (VTM,ATM,VMP,AMP,VMPP and AMPP) List"),
    PharmacyAnalyticsMovementOutBySaleIssueAndConsumptionWithCurrentStockReport("Pharmacy Analytics - Movement Out by Sale, Issue, and Consumption with Current Stock Report"),
    PharmacyAnalyticsMovementReportStockByDate("Pharmacy Analytics - Movement Report Stock By Date"),
    PharmacyAnalyticsMovementReportStockByDateByBatch("Pharmacy Analytics - Movement Report Stock By Date - By Batch"),
    PharmacyAnalyticsNonMoving("Pharmacy Analytics - Non Moving"),
    PharmacyAnalyticsOrderingRequirementReport("Pharmacy Analytics - Ordering Requirement Report"),
    PharmacyAnalyticsPharmacyAllReport("Pharmacy Analytics - Pharmacy All Report"),
    PharmacyAnalyticsPharmacyDirectPurchaseReport("Pharmacy Analytics - Pharmacy Direct purchase Report"),
    PharmacyAnalyticsPharmacyGRNAndPurchaseReport("Pharmacy Analytics - Pharmacy GRN and purchase Report"),
    PharmacyAnalyticsPharmacyGRNReport("Pharmacy Analytics - Pharmacy GRN Report"),
    PharmacyAnalyticsPharmacyIncomeAndCost("Pharmacy Analytics - Pharmacy Income and Cost"),
    PharmacyAnalyticsPharmacyIncomeReport("Pharmacy Analytics - Pharmacy Income Report"),
    PharmacyAnalyticsPharmacyProcurementReport("Pharmacy Analytics - Pharmacy Procurement Report"),
    PharmacyAnalyticsPharmacyReturnWithoutTraising("Pharmacy Analytics - Pharmacy Return Without Traising"),
    PharmacyAnalyticsPharmacySaleReport("Pharmacy Analytics - Pharmacy Sale Report"),
    PharmacyAnalyticsPharmacySaleSummary("Pharmacy Analytics - Pharmacy Sale Summary"),
    PharmacyAnalyticsPharmacySaleSummaryDate("Pharmacy Analytics - Pharmacy Sale Summary Date"),
    PharmacyAnalyticsPharmacyWholesaleCreditBills("Pharmacy Analytics - Pharmacy Wholesale Credit Bills"),
    PharmacyAnalyticsPharmacyWholesaleReport("Pharmacy Analytics - Pharmacy Wholesale Report"),
    PharmacyAnalyticsPresciptionList("Pharmacy Analytics - Presciption List"),
    PharmacyAnalyticsPrescriptionReport("Pharmacy Analytics - Prescription Report"),
    PharmacyAnalyticsPrescriptionSummary("Pharmacy Analytics - Prescription Summary"),
    PharmacyAnalyticsProcurementBillItemList("Pharmacy Analytics - Procurement Bill Item List"),
    PharmacyAnalyticsPurcharseBillWithSupplier("Pharmacy Analytics - Purcharse Bill with Supplier"),
    PharmacyAnalyticsPurchaseAndGRNSummaryCreditCash("Pharmacy Analytics - Purchase and GRN Summary (Credit / Cash )"),
    PharmacyAnalyticsPurchaseBillsByDepartment("Pharmacy Analytics - Purchase Bills by Department"),
    PharmacyAnalyticsPurchaseOrdersNotApproved("Pharmacy Analytics - Purchase Orders Not Approved"),
    PharmacyAnalyticsPurchaseOrderSummary("Pharmacy Analytics - Purchase Order Summary"),
    PharmacyAnalyticsPurchaseSummaryBySupplier("Pharmacy Analytics - Purchase Summary By Supplier"),
    PharmacyAnalyticsPurchaseSummaryBySupplierCreditCash("Pharmacy Analytics - Purchase Summary By Supplier (Credit / Cash)"),
    PharmacyAnalyticsPurchaseSummaryCreditCash("Pharmacy Analytics - Purchase Summary (Credit / Cash )"),
    PharmacyAnalyticsReorderAnalysis("Pharmacy Analytics - Reorder Analysis"),
    PharmacyAnalyticsReportTransferIssuedNotRecieved("Pharmacy Analytics - Report Transfer Issued not Recieved"),
    PharmacyAnalyticsRetailSaleBillList("Pharmacy Analytics - Retail Sale Bill List"),
    PharmacyAnalyticsROLAndROQManagement("Pharmacy Analytics - ROL and ROQ Management"),
    PharmacyAnalyticsSaleDetailByBill("Pharmacy Analytics - Sale Detail - By Bill"),
    PharmacyAnalyticsSaleDetailByBillItems("Pharmacy Analytics - Sale Detail - By Bill Items"),
    PharmacyAnalyticsSaleDetailByDiscountScheme("Pharmacy Analytics - Sale Detail - By Discount Scheme"),
    PharmacyAnalyticsSaleDetailByPaymentMethod("Pharmacy Analytics - Sale Detail - By Payment Method"),
    PharmacyAnalyticsSaleReport("Pharmacy Analytics - Sale Report"),
    PharmacyAnalyticsSaleSummaryByBillType("Pharmacy Analytics - Sale Summary - By Bill Type"),
    PharmacyAnalyticsSaleSummaryByDiscountSchemeSummary("Pharmacy Analytics - Sale Summary By Discount Scheme Summary"),
    PharmacyAnalyticsSaleSummaryByPaymentMethod("Pharmacy Analytics - Sale Summary - By Payment Method"),
    PharmacyAnalyticsSaleSummaryByPaymentMethodByBill("Pharmacy Analytics - Sale Summary - By Payment Method (By Bill)"),
    PharmacyAnalyticsShortExpiryByAMPPeriod("Pharmacy Analytics - Short Expiry (by AMP Period)"),
    PharmacyAnalyticsSingleItemSummary("Pharmacy Analytics - Single Item Summary"),
    PharmacyAnalyticsSlowMoving("Pharmacy Analytics - Slow Moving"),
    PharmacyAnalyticsStaffStock("Pharmacy Analytics - Staff Stock"),
    PharmacyAnalyticsStaffStockReport("Pharmacy Analytics - Staff Stock Report"),
    PharmacyAnalyticsStockHistory("Pharmacy Analytics - Stock History"),
    PharmacyAnalyticsStockMovementTimelineGraphical("Pharmacy Analytics - Stock Movement Timeline (Graphical)"),
    PharmacyAnalyticsStockOverviewReport("Pharmacy Analytics - Stock Overview Report"),
    PharmacyAnalyticsStockReportByBatchForExport("Pharmacy Analytics - Stock Report by Batch for Export"),
    PharmacyAnalyticsStockReportByItem("Pharmacy Analytics - Stock Report by Item"),
    PharmacyAnalyticsStockReportByItemOrderByVMP("Pharmacy Analytics - Stock Report by Item - Order by VMP"),
    PharmacyAnalyticsStockReportByProduct("Pharmacy Analytics - Stock Report by Product"),
    PharmacyAnalyticsStockReportOfSingleProduct("Pharmacy Analytics - Stock Report of Single Product"),
    PharmacyAnalyticsStockReportWithSuppliers("Pharmacy Analytics - Stock Report (with Suppliers)"),
    PharmacyAnalyticsStockSummaryWithSuppliers("Pharmacy Analytics - Stock Summary (with Suppliers)"),
    PharmacyAnalyticsStockTakingReportNew("Pharmacy Analytics - Stock Taking Report(New)"),
    PharmacyAnalyticsStockWithMovement("Pharmacy Analytics - Stock With Movement"),
    PharmacyAnalyticsSuppliersExpiringStocks("Pharmacy Analytics - Suppliers Expiring Stocks"),
    PharmacyAnalyticsSuppliersStockSummary("Pharmacy Analytics - Suppliers Stock Summary"),
    PharmacyAnalyticsSupplierStockReport("Pharmacy Analytics - Supplier Stock Report"),
    PharmacyAnalyticsTransferIssueByBill("Pharmacy Analytics - Transfer Issue by Bill"),
    PharmacyAnalyticsTransferIssueByBillItem("Pharmacy Analytics - Transfer Issue By Bill Item"),
    PharmacyAnalyticsTransferIssueSummary("Pharmacy Analytics - Transfer Issue Summary"),
    PharmacyAnalyticsTransferIssueSummaryReportByDate("Pharmacy Analytics - Transfer Issue Summary Report By Date"),
    PharmacyAnalyticsTransferReceiveByBill("Pharmacy Analytics - Transfer Receive by Bill"),
    PharmacyAnalyticsTransferReceiveByBillItem("Pharmacy Analytics - Transfer Receive By Bill Item"),
    PharmacyAnalyticsTransferReceiveSummary("Pharmacy Analytics - Transfer Receive Summary"),
    PharmacyAnalyticsTransferReceiveVsBHTIssueQuntityTotalsByItem("Pharmacy Analytics - Transfer Receive Vs BHT Issue Quntity Totals By Item"),
    PharmacyAnalyticsTransferReportSummary("Pharmacy Analytics - Transfer Report Summary"),
    PharmacyAnalyticsUnitIssueByBill("Pharmacy Analytics - Unit Issue by bill"),
    PharmacyAnalyticsUnitIssueByDepartment("Pharmacy Analytics - Unit Issue by Department"),
    PharmacyAnalyticsUnitIssueByItem("Pharmacy Analytics - Unit Issue by Item"),
    PharmacyAnalyticsUnitIssueByItemBatch("Pharmacy Analytics - Unit Issue by Item (Batch)"),
    PharmacyAnalyticsZeroStockItemReport("Pharmacy Analytics - Zero Stock Item Report"),
    PharmacyBhtRequestForceComplete("Pharmacy BHT Request Force Complete"),
    PharmacyDirectPurchaseApprove("Pharmacy Direct Purchase Approve"),
    PharmacyDirectPurchaseFinalize("Pharmacy Direct Purchase Finalize"),
    PharmacyDirectPurchaseSave("Pharmacy Direct Purchase Save"),
    PharmacyDiscardCategoryManage("Pharmacy Issue Category Manage"),
    PharmacyDischargeMedicineIssue("Pharmacy Discharge Medicine Issue"),
    PharmacyDisposalIssueApprove("Pharmacy Disposal Issue Approve"),
    PharmacyDisposalIssueCancel("Pharmacy Disposal Issue Cancel"),
    PharmacyDisposalIssueFinalize("Pharmacy Disposal Issue Finalize"),
    PharmacyExpiryDateAdjustmentApproval("Pharmacy Expiry Date Adjustment Approval"),
    PharmacyGrnCancel("Pharmacy GRN Cancel"),
    PharmacyGrnReturnCancel("Pharmacy GRN Return Cancel"),
    PharmacyIssueForRequestApprove("Pharmacy Issue For Request Approve"),
    PharmacyIssueForRequestFinalize("Pharmacy Issue For Request Finalize"),
    PharmacyIssueForRequestSave("Pharmacy Issue For Request Save"),
    PharmacyItemNameEdit("Pharmacy Item Name Edit"),
    PharmacyPriceAdjustmentApproval("Pharmacy Price Adjustment Approval"),
    PharmacyReceiveApprove("Pharmacy Receive Approve"),
    PharmacyReceiveFinalize("Pharmacy Receive Finalize"),
    PharmacyReceiveSave("Pharmacy Receive Save"),
    PharmacyRetailSaleReturnApproval("Pharmacy Retail Sale Return Approval"),
    PharmacyReturnFromWardForceComplete("Pharmacy Return From Ward Force Complete"),
    PharmacySearchReturnBillCancel("Pharmacy Search Return Bill Cancel"),
    PharmacyStockAdjustmentApproval("Pharmacy Stock Adjustment Approval"),
    PharmacyTransferIssueCancel("Pharmacy Transfer Issue Cancel"),
    PharmacyTransferReceiveCancel("Pharmacy Transfer Receive Cancel"),
    PurchaseOrderFinalize("Purchase Order Finalize"),
    PurchaseOrderSave("Purchase Order Save"),
    ReportsActiveShiftsReport("Reports Page - 8. Active Shifts"),
    ReportsAdditionalFormReportVerification("Reports Page - 2. Additinal Form Report Verification"),
    ReportsAdmissionCategoryWiseAdmission("Reports Page - 4. Admission Category Wise Admission"),
    ReportsAdmissionCountConsultationWise("Reports Page - 12. Admission Count(Consultant Wise)"),
    ReportsAdmissionCountPaymentTypeWise("Reports Page - 13. Admission Count(Payment Type Wise)"),
    ReportsAdmissionDischargeReport("Reports Page - 1. Admission and Discharge"),
    ReportsAdmissionReport("Reports Page - 6. Admission Reports"),
    ReportsAfterStockTaking("Reports Page - 11. After Stock Taking"),
    ReportsAllCashierHandovers("Reports Page - 5. Shifts"),
    ReportsAllCashierSummary("Reports Page - 1. All Cashier Summary"),
    ReportsAllDepartmentSaleReport("Reports Page - 18. All Department Sale Report"),
    ReportsAllStaffSalarySummary("Reports Page - 1. All Staff Salary Summary"),
    ReportsAllUsersDrawerHistory("Reports Page - 26. Drawer History"),
    ReportsAnnualTestStatistics("Reports Page - Annual Test Statistics"),
    ReportsAssetAmcExpiryReport("Reports Page - 9. Asset AMC Expiry"),
    ReportsAssetAmcReport("Reports Page - 11. AMC"),
    ReportsAssetDisposalReportSaleDisposalWriteOff("Reports Page - 15. Asset Disposal Report (Sale Disposal, Write-off)"),
    ReportsAssetGrnReport("Reports Page - 6. Asset GRN Report"),
    ReportsAssetRegister("Reports Page - 1. Asset Register"),
    ReportsAssetTransferReport("Reports Page - 7. Asset Transfer Report"),
    ReportsAssetWarentyExpireReport("Reports Page - 5. Asset Warranty Expire Report"),
    ReportsAssetWarrantyExpiry("Reports Page - 10. Asset Warranty Expiry"),
    ReportsAttendanceReport("Reports Page - 1. Attandance Report"),
    ReportsBatchWiseStockReport("Reports Page - 7. Batch Wise Stock"),
    ReportsBeforeStockTaking("Reports Page - 10. Before Stock Taking"),
    ReportsBillExpenses("Reports Page - 8. Bill Expenses"),
    ReportsBillsByItemCategory("Reports Page - Bills by Category"),
    ReportsBillWiseItemMovementReport("Reports Page - 18. Bill Wise Item Movement"),
    ReportsCafeConsumption("Reports Page - 4. Café Consumption"),
    ReportsCafeDiscount("Reports Page - 1. Café Discount"),
    ReportsCafeExpiry("Reports Page - 3. Café Expiry"),
    ReportsCafeInwardPatientSale("Reports Page - 5. Cafe Inward Patient Sale"),
    ReportsCafeSale("Reports Page - 2. Café Sale"),
    ReportsCashierDetails("Reports Page - 3. Cashier Details"),
    ReportsCashierSummary("Reports Page - 2. Cashier Summary"),
    ReportsCCBalanceReport("Reports Page - 2. Collection Center Balance"),
    ReportsCCBillItemListReport("Reports Page - 12. Collction Centre Bill Item List"),
    ReportsCCBillWiseDetailReport("Reports Page - 4. Collection Center Bill Wise Detail"),
    ReportsCCBookReport("Reports Page - 10. Collction Centre Book"),
    ReportsCCBookWiseDetail("Reports Page - 11. Collction Centre Book Wise Detail"),
    ReportsCCCurrentBalanceReport("Reports Page - 2. Collection Center Current Balance"),
    ReportsCCInvestigationListReport("Reports Page - 12. Collction Centre Investigation List"),
    ReportsCCReceiptReport("Reports Page - 3. Collection Center Receipt"),
    ReportsCCReportsPrint("Reports Page - 1. Collection Center Reports Print"),
    ReportsCCRouteAnalysisReport("Reports Page - 9. Route Analysis"),
    ReportsCCStatementReport("Reports Page - 6. Collection Center Statement"),
    ReportsCCWiseInvoiceListReport("Reports Page - 5. Collection Center Wise Invoice List"),
    ReportsCCWiseSummaryReport("Reports Page - 7. Collection Center Wise Summary"),
    ReportsClosingStockReport("Reports Page - 1. Closing Stock"),
    ReportsCollectionCenterStatement("Reports Page - Collection center statement"),
    ReportsCollectionCenterWiseIncome("Reports Page - 8. Collection Center Wise Income"),
    ReportsConsumption("Reports Page - 2. Consumption (Legacy)"),
    ReportsConsumptionDto("Reports Page - 2. Consumption"),
    ReportsCostOfGoodsSold("Reports Page - 4. Cost Of Good Sold"),
    ReportsCourierLabReportsPrint("Reports Page - Courier Lab Reports Print"),
    ReportsCreditInvoiceDispatch("Reports Page - 15. Credit Invoice Dispatch"),
    ReportsDailyReturn("Reports Page - Daily return"),
    ReportsDailyReturnDto("Reports Page - Daily return – Fast"),
    ReportsDailyReturnImportForQbReport("Reports Page - 1. QB Import Reports"),
    ReportsDashboard("Reports Page - Dashboard"),
    ReportsDebtorAgeAnlysis("Reports Page - 14. Debtor Age Anlysis"),
    ReportsDebtorAnalysis("Reports Page - 24. Debtor Analysis"),
    ReportsDebtorBalanceReport("Reports Page - 12. Debtor Balance Report"),
    ReportsDebtorSettlement("Reports Page - 11. Debtor Settlement"),
    ReportsDebtorSettlementFinancial("Reports Page - 19. Debtor Settlement"),
    ReportsDepartmentReports("Reports Page - 1. Department Report"),
    ReportsDepartmentRevenueReport("Reports Page - 27. Department Revenue"),
    ReportsDiscount("Reports Page - 6. Discount"),
    ReportsDrawerAdjustments("Reports Page - 26 A. Drawer Adjustments"),
    ReportsDrawerHistory("Reports Page - 25. Drawer History By User"),
    ReportsDueAge("Reports Page - 31. Due Age"),
    ReportsDueAgeCreditCompany("Reports Page - 32. Due Age (Credit Company)"),
    ReportsDueAgeDetail("Reports Page - 33. Due Age Detail"),
    ReportsDueSearch("Reports Page - 29. Due Search"),
    ReportsDueSearchCreditCompany("Reports Page - 30. Due Search (Credit Company)"),
    ReportsDurationServiceReport("Reports Page - 5. Duration Service Report"),
    ReportsEmployeeAssetIssue("Reports Page - 3. Employee Asset Issue"),
    ReportsEmployeeDetails("Reports Page - 2. Employee Detail"),
    ReportsEmployeeEndofProbation("Reports Page - 4. Employee End of Probation"),
    ReportsEmployeeToRetired("Reports Page - 3. Employee To Retired Details"),
    ReportsEmployeeWorkedDayReport("Reports Page - 2. Employee Worked Day Report"),
    ReportsEmployeeWorkedDayReportSalaryCycle("Reports Page - 3. Employee Worked Day Report(Salary Cycle)"),
    ReportsEnteredShiftReport("Reports Page - 2. Entered Shift Report"),
    ReportsEPF("Reports Page - 16. EPF"),
    ReportsEpfEtfUploadReport("Reports Page - 18. EPF/ETF Upload Report"),
    ReportsETF("Reports Page - 17. ETF"),
    ReportsExcessAge("Reports Page - 37. Excess Age"),
    ReportsExcessAgeCreditCompany("Reports Page - 35. Excess Age (Credit Company)"),
    ReportsExcessSearch("Reports Page - 36. Excess Search"),
    ReportsExcessSearchCreditCompany("Reports Page - 34. Excess Search (Credit Company)"),
    ReportsExpiryItem("Reports Page - 13. Expiry Item Report"),
    ReportsExternalLaboratoryWorkloadReport("Reports Page - External Laboratory Workload"),
    ReportsFingerAnalysisReportBySalaryCycle("Reports Page - 7. Finger Analysis Report by Salary Cycle"),
    ReportsFingerPrintApprove("Reports Page - 4. Fingerprint Approve"),
    ReportsFingerprintHistory("Reports Page - 2. Fingerprint history"),
    ReportsFingerPrintRecordByLogged("Reports Page - 1. Fingerprint Record by Logged"),
    ReportsFingerPrintRecordByVerified("Reports Page - 2. Fingerprint Record by Verified"),
    ReportsFingerPrintRecordNoShiftSettled("Reports Page - 3. Fingerprint Record by No Shift Settled"),
    ReportsFixedAssetIssue("Reports Page - 4. Fixed Asset Issue"),
    ReportsGoodInTransit("Reports Page - 5. Good in Transit"),
    ReportsGrn("Reports Page - 9. GRN"),
    ReportsGrnReport("Reports Page - 6. GRN Report"),
    ReportsGrnReturnVarianceReport("Reports Page - 14. GRN Return Variance Report"),
    ReportsGrnSummaryReport("Reports Page - 15. GRN Summary Report"),
    ReportsHandoverStatusReport("Reports Page - 6. Handovers"),
    ReportsHeadCountReport("Reports Page - 1. Head Count"),
    ReportsHolidayReport("Reports Page - 6. Holiday Report"),
    ReportsIncomeBreakdownByCategory("Reports Page - Income Breakdown by Category"),
    ReportsInvestigationMonthEndDetails("Reports Page - Investigation Month End Details"),
    ReportsInvestigationMonthEndSummery("Reports Page - Investigation Month End Summary"),
    ReportsInvoiceAndReciptReportSerialWise("Reports Page - 9. Invoice and Receipt Report (Serial Wise)"),
    ReportsInwardService("Reports Page - 6. Inward Service"),
    ReportsIouConversionBillReport("Reports Page - 9. IOU Conversion Bills"),
    ReportsIouConversionPaymentReport("Reports Page - 10. IOU Conversion Payments"),
    ReportsIpIncomeCategoryWiseReport("Reports Page - 3. IP Income Category Wise"),
    ReportsIpServiceReport("Reports Page - 5. Service Reports"),
    ReportsIpUnsettledInvoices("Reports Page - 2. IP Unsettled Invoices"),
    ReportsItemLoacationHistory("Reports Page - 8. Item Loacation History"),
    ReportsLabBillItemList("Reports Page - Lab Bill Item List"),
    ReportsLabInvetigationWiseReport("Reports Page - Investigation Wise Research"),
    ReportsLaboratoryWorkloadReport("Reports Page - Laboratory Workload"),
    ReportsLabOrganismAntibioticSensitivityReport("Reports Page - Organism Antibiotic Sensitivity"),
    ReportsLabPeakHourStatistics("Reports Page - Peak Hour Statistics"),
    ReportsLabRegisterReport("Reports Page - Lab Register"),
    ReportsLabReportsTestCount("Reports Page - Test Count Report"),
    ReportsLateInAndEarlyOut("Reports Page - 2. Late In and Early Out"),
    ReportsLateLeaveDetails("Reports Page - 3. Late Leave(Detail)"),
    ReportsLeaveForm("Reports Page - 1. Leave Form"),
    ReportsLeaveReport("Reports Page - 1. Leave Report"),
    ReportsLeaveReportSummery("Reports Page - 2. Leave Report Summary"),
    ReportsLeaveSummeryReport("Reports Page - 4.Leave Summary Report"),
    ReportsListAllDrawers("Reports Page - 4. All Drawers"),
    ReportsManagementAdmissionCountReport("Reports Page - 7. Referring Doctor Wise Revenue"),
    ReportsManagementHospitalCensusReport("Reports Page - 14. Hospital Census"),
    ReportsModalityDowntime("Reports Page - 14. Modality Down Time"),
    ReportsMonthEndEmployeeNoPayReportByMinutes("Reports Page - 5. Month End Employee(No Pay) Report-By Minute"),
    ReportsMonthEndEmployeeSummery("Reports Page - 6. Month End Employee Summary"),
    ReportsMonthendEmployeeWorkingTimeAndOvertime("Reports Page - 4. Month End Employee Working Time + Over Time Report"),
    ReportsNopayandSalaryAllowanceReport("Reports Page - 7. No Pay and Salary Allowance Report"),
    ReportsOnlineFormStatus("Reports Page - 3. Online Form Status"),
    ReportsOpdAndInwardDueReport("Reports Page - 13. OPD and Inward Due"),
    ReportsOpdWeeklyReport("Reports Page - 16. OPD Weekly Report"),
    ReportsOtRoomWiseSergeryCount("Reports Page - 8. OT Room Wise Surgery Count"),
    ReportsOutsidePayment("Reports Page - 7. Outside Payments"),
    ReportsPackageReport("Reports Page - 23. Package Report"),
    ReportsPatientJourney("Reports Page - 1. Patient Journey"),
    ReportsPatientLedger("Reports Page - 2. Patient Ledger"),
    ReportsPaymentSettlement("Reports Page - 28. Payment Settlement"),
    ReportsPettyCashPayment("Reports Page - 16. Petty Cash Payment"),
    ReportsPharmacyDepartmentWiseSaleReport("Reports Page - 6. Pharmacy Department Wise Sale Report"),
    ReportsPharmacySaleReport("Reports Page - 10. Pharmacy Sale (OP/IP)"),
    ReportsPoStatusReport("Reports Page - 2. PO Status Report"),
    ReportsPretentiveMaintainanceReport("Reports Page - 13. Preventive Maintenance"),
    ReportsProfessionalFeePayment("Reports Page - 5. Professional Fees Payment"),
    ReportsProfessionalFeePayments("Reports Page - Professional Fee Payments"),
    ReportsProfessionalFees("Reports Page - Professional Fees"),
    ReportsProfessionalPayments("Reports Page - Professional Payments"),
    ReportsProfitMatrixReport("Reports Page - 22. Profit Matrix"),
    ReportsPurchaseRateMovement("Reports Page - 16. Purchase Rate movement"),
    ReportsReferringDoctorWiseRevenueDto("Reports Page - 7a. Referring Doctor Wise Revenue (DTO - Fast)"),
    ReportsReportQbItemList("Reports Page - 2. Item Import Report"),
    ReportsRoomChange("Reports Page - 3. Room Change"),
    ReportsROOMOCCUPANCY("Reports Page - 15. ROOM OCCUPANCY"),
    ReportsRoomOccupancyReport("Reports Page - 1. Room Occupancy"),
    ReportsRosterTimeAndVerifyTime("Reports Page - 3. Roaster Table and Verify Time Report"),
    ReportsSampleCarrierReport("Reports Page - Sample Carrier"),
    ReportsServiceCategoryWiseBillDetail("Reports Page - 4. Service Category Wise Bill Details"),
    ReportsServiceCategoryWiseBillDetailOpd("Reports Page - 4.1. Service Category Wise Bill Details OPD"),
    ReportsShiftEndCash("Reports Page - 7. Shift End Cash"),
    ReportsShiftStartAndEnd("Reports Page - 12. Shift End Summary"),
    ReportsSlowFastNoneMovement("Reports Page - 8. Slow/Fast/None Movement Report"),
    ReportsSpecialityDoctorWiseIncome("Reports Page - 17. Speciality/Doctor Wise Income Report"),
    ReportsSpecialityWiseDemograhicData("Reports Page - 3. Speciality Wise Demographic Data"),
    ReportsStaffDetail("Reports Page - 5. Staff Detail"),
    ReportsStaffGratuity("Reports Page - 21. Staff Garduity"),
    ReportsStaffOverTimeReport("Reports Page - 6. Staff Over Time"),
    ReportsStaffPayrollAccountant("Reports Page - 3. Staff Payroll(Accountant)"),
    ReportsStaffPayrollByDepartmentByRoster("Reports Page - 4. Staff Payroll(By Department, By Roster)"),
    ReportsStaffPayrollReport("Reports Page - 2. Staff Payroll"),
    ReportsStaffPayrollSelectedStaff("Reports Page - 5. Staff Payroll(Selected Staff)"),
    ReportsStaffPaysheetComponentList("Reports Page - 8. Staff Paysheet Component List"),
    ReportsStaffSalaryBankWise("Reports Page - 9. Staff Salary Bank Wise"),
    ReportsStaffSalaryComponentBankWiseReport("Reports Page - 14. Staff Salary Component(Bank Wise)"),
    ReportsStaffSalaryComponentBetweenToSalaryCycles("Reports Page - 15. Staff Salary Component(Between To Salary Cycles)"),
    ReportsStaffSalaryComponentReport("Reports Page - 13. Staff Salary Component"),
    ReportsStaffSalaryGenerateOrDeleteDetailReport("Reports Page - 20. Staff Salary Generate Or Delete Detail Report"),
    ReportsStaffSalaryGenerateOrNotReport("Reports Page - 19. Staff Salary Generate Or Not Report"),
    ReportsStaffSalaryPaymentToBank("Reports Page - 10. Staff Salary Payment To Bank"),
    ReportsStaffSalaryPaymentToBankPayPast("Reports Page - 12. Staff Salary Payment To Bank(Pay Fast)"),
    ReportsStaffSalaryPaymentToBankSlip("Reports Page - 11. Staff Salary Payment To Bank(Slip)"),
    ReportsStaffShiftDetailsByStaff("Reports Page - 3. Staff Shift Detail By Report"),
    ReportsStaffShiftHistory("Reports Page - 1. Staff Shift History"),
    ReportsStaffShiftReport("Reports Page - 1. Staff Shift Report"),
    ReportsStaffWelfareBills("Reports Page - 21. Staff Welfare"),
    ReportsStockLedger("Reports Page - 12. Stock Leger (Legacy)"),
    ReportsStockLedgerDto("Reports Page - 12. Stock Ledger"),
    ReportsStockTransferReport("Reports Page - 3. Stock Transfers"),
    ReportsSugeryStatus("Reports Page - 3. Surgery Status"),
    ReportsSurgeryCostEstimation("Reports Page - 4. Surgery Cost Estimation"),
    ReportsSurgeryCountDoctorWise("Reports Page - 10. Surgery Count(Doctor Wise)"),
    ReportsSurgeryCountTypeWise("Reports Page - 11. Surgery Count(Type)"),
    ReportsSurgerySurvey("Reports Page - 2. Surgery Survey"),
    ReportsSurgeryWiseCount("Reports Page - 9. Surgery Count(Surgery Wise)"),
    ReportsTestWiseCountReport("Reports Page - 8. Collection Center Test Wise Count"),
    ReportsTestWiseCountReports("Reports Page - Test Wise Count"),
    ReportsTheatreService("Reports Page - 7. Theatre Service"),
    ReportsTotalCashierSummary("Reports Page - Total Cashier Summary"),
    ReportsTurnAroundTimeDetails("Reports Page - Turn Around Time Details"),
    ReportsTurnAroundTimeHourly("Reports Page - Turn Around Time(Hourly)"),
    ReportsVerifiedReport("Reports Page - 4. Verified Report"),
    ReportsWhtReport("Reports Page - 17. WHT"),
    ReportsWorkOrderReport("Reports Page - 12. Work Order"),
    RequestManager("Request Manager"),
    SettleHandoverProofMissing("Settle Handover Proof Missing"),
    SettleNonCashPayments("Settle Non-Cash Payments"),
    ShowServiceCharges("Show Service Charges"),
    ShowTimeServiceCharges("Show Time Service Charges"),
    TheatreAcceptPatient("Theatre Accept Patient"),
    TheatreReturnPatient("Theatre Return Patient"),
    TheatreSendPatient("Theatre Send Patient"),
    ViewAllShiftShortageBills("View All Shift Shortage Bills"),
    WardAcceptTheatreReturn("Ward Accept Theatre Return"),
    // </editor-fold>
    ;

    private final String label;

    Privileges(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isFromDatabase() {
        switch (this) {
            case Opd:
            case Pharmacy:
            case Lab:
            case Channelling:
            case Admin:
            case Theatre:
            case Store:
            case Hr:
            case Inward:
            case Payment:
            case Reports:
            case Clinical:
            case MemberShip:
            case Search:
            case User:
                return false;
            default:
                return true;
        }
    }

    public String getCategory() {
        switch (this) {
            case Opd:
            case OpdCancel:
            case OpdPackageBillCancel:
            case OpdReturn:
            case OpdBilling:
            case OpdOrdering:
            case OpdReprint:
            case OpdBillSearch:
            case OpdPreBilling:
            case OpdReactivate:
            case OpdBillItemSearch:
            case OpdBillSearchEdit:
            case OpdEditPatientDetails:
            case OpdCollectingCentreBilling:
            case OpdCollectingCentreBillSearch:
            case OpdCollectingCentreBillingMenu:
                return "OPD";
            case Lab:
            case LabItems:
            case LabLists:
            case LabSetUp:
            case LabReport:
            case LabBilling:
            case LabCashier:
            case LabReceive:
            case LabReports:
            case LabPrinting:
            case LabCCBilling:
            case LabDataentry:
            case LabReporting:
            case LabSummeries:
            case LabBillSearch:
            case LabReportEdit:
            case LabReprinting:
            case LabAutherizing:
            case LabBillReprint:
            case LabEditPatient:
            case LabReportPrint:
            case LabReportSearch:
            case LabRevertSample:
            case LabBillRefunding:
            case LabBillReturning:
            case LabDeAutherizing:
            case LabInwardBilling:
            case LabItemFeeDelete:
            case LabAdiministrator:
            case LabBillCancelling:
            case LabBillItemSearch:
            case LabItemFeeUpadate:
            case LabCCBillingSearch:
            case LabReportSearchAll:
            case LabReportSearchOwn:
            case LabSampleReceiving:
            case LabSummeriesLevel1:
            case LabSummeriesLevel2:
            case LabSummeriesLevel3:
            case LabBillReactivating:
            case LabInvestigationFee:
            case LabSampleCollecting:
            case OutLabSampleSending:
            case LabAddInwardServices:
            case LabBillCancelSpecial:
            case LabBillRefundSpecial:
            case LabBillSearchCashier:
            case LabCasheirBillSearch:
            case LabReportFormatEditing:
            case LabCollectingCentreBilling:
            case LabInwardSearchServiceBill:
            case LabReportSearchByDepartment:
            case LabSearchBillLoggedInstitution:
            case LabReportSearchByLoggedInstitution:
            case AccessLabTestHistory:
            case DoctorDashBoardMenu:
                return "Lab";
            case Pharmacy:
            case PharmacySaleWh:
            case PharmacySearch:
            case PharmacyReports:
            case PharmacySummery:
            case PharmacyPurchase:
            case PharmacyTransfer:
            case PharmacyTransferViewRates:
            case NursingIPBillingViewRates:
            case IPRequestViewRates:
            case StockRequestViewRates:
            case ConsumptionViewRates:
            case StockTransactionViewRates:
            case DiscardViewRates:
            case PharmacyPurchaseWh:
            case PharmacySaleCancel:
            case PharmacySaleReturn:
            case PharmacyStockIssue:
            case PharmacyGoodReceive:
            case PharmacySaleReprint:
            case PharmacyReAddToStock:
            case PharmacySaleCancelWh:
            case PharmacySaleReturnWh:
            case PharmacyDealorPayment:
            case PharmacyGoodReceiveWh:
            case PharmacyInwardBilling:
            case PharmacyOrderApproval:
            case PharmacyOrderCreation:
            case PharmacySaleReprintWh:
            case PharmacyAdministration:
            case PharmacyBHTIssueAccept:
            case PharmacyPurchaseReturn:
            case PharmacyGoodReceiveEdit:
            case PharmacyPurchaseReprint:
            case PharmacySetReorderLevel:
            case PharmacyStockAdjustment:
            case PharmacyGoodReceiveCancel:
            case PharmacyGoodReceiveReturn:
            case PharmacyOrderCancellation:
            case PharmacyInwardBillingCancel:
            case PharmacyInwardBillingReturn:
            case PharmacyPurchaseCancellation:
            case PharmacyReturnWithoutTraising:
            case PharmacyStockAdjustmentSingleItem:

            // Inpatient medication management
            case InpatientMedicationManagementMenue:
            case PharmacyDirectIssueToBht:
            case PharmacyDirectIssueToTheaterCases:
            case PharmacyBhtIssueRequest:
            case PharmacySearchInpatientDirectIssuesbyBill:
            case PharmacySearchInpatientDirectIssuesbyItem:
            case PharmacySearchInpatientDirectIssueReturnsbyBill:
            case PharmacysSearchInpatientDirectIssueReturnsbyItem:
            // Procurement
            case PharmacyProcurementMenu:
            case CreatePurchaseOrder:
            case AutoOrderPModel:
            case AutoOrderQModal:
            case DirectPurchase:
            case PurchaseOrdersApprovel:
            case GoodsRecipt:
            case ReturnReceviedGoods:
            case CreateGrnReturn:
            case FinalizeGrnReturn:
            case ApproveGrnReturn:
            case CreateDisposalReturn:
            case FinalizeDisposalReturn:
            case ApproveDisposalReturn:
            case ViewDisposalReturn:
            case CreateDirectPurchaseReturn:
            case FinalizeDirectPurchaseReturn:
            case ApproveDirectPurchaseReturn:
            case ReturnWithoutRecipt:
            case PharmacyReturnWithoutReceiptBill:
            // Disbursement
            case PharmacyDisburesementMenu:
            case PharmacyDisbursementRequest:
            case PharmacyDisbursementIssurForRequest:
            case PharmacyDisbursementDirectIssue:
            case PharmacyDisbursementRecieve:
            case PharmacyDisbursementReports:

            // Retail Transactions
            case PharmacyRetailTransaction:
            case PharmacySale:
            case PharmacySaleQuick:
            case PharmacySaleForCashierQuick:
            case PharmacySaleForCashier:
            case PharmacySaleWithOutStock:
            case PharmacySearchSaleBill:
            case PharmacySearchSalePreBill:
            case PharmacySearchSaleBillItems:
            case PharmacyReturnItemsOnly:
            case PharmacyReturnItemsAndPayments:
            case PharmacySearchReturnBill:
            case PharmacyAddToStock:
            case PharmacyDonation:

            // Wholesale Transaction
            case PharmacyWholeSaleTransaction:
            case PharmacyWholesaleSale:
            case PharmacyWholesaleSaleForCashier:
            case PharmacyWholesaleSearchSaleBill:
            case PharmacyWholesaleSearchSaleBillToPay:
            case PharmacyWholesaleSearchSaleBillItems:
            case PharmacyWholesaleReturnItemsOnly:
            case PharmacyWholesaleWholeSaleAddToStock:
            case PharmacyWholeSalePurchase:

            // Disposal
            case PharmacyDisposalIssue:
            case PharmacyDisposalSearchIssueBill:
            case PharmacyDisposalSearchIssueBillItems:
            case PharmacyDisposalSearchIssueReturnBill:
            case PharmacyDisposalUnitIssueMargin:

            // Pharmacy Adjustment
            case PharmacyAdjustmentDepartmentStockQTY:
            case PharmacyAdjustmentDepartmentStockBySingleItemQTY:
            case PharmacyAdjustmentStaffStockAdjustment:
            case PharmacyAdjustmentPurchaseRate:
            case PharmacyAdjustmentCostRate:
            case PharmacyAdjustmentSaleRate:
            case PharmacyAdjustmentWholeSaleRate:
            case PharmacyAdjustmentExpiryDate:
            case PharmacyAdjustmentSearchAdjustmentBills:
            case PharmacyAdjustmentTransferAllStock:
            case PharmacyAdjustmentCreateBatch:
            case PharmacyPhysicalCountApprove:
            case PharmacyStockTakeApprove:

            // Pharmacy Dealer Payments
            case PharmacyDealerDueSearch:
            case PharmacyDealerDueByAge:
            case PharmacyPayment:
            case PharmacyGRNPaymentApprove:
            case PharmacyGRNPaymentDoneSearch:
            case PharmacyCreditDueAndAccess:

            case PharmacyItemSearch:
            case PharmacyGenarateReports:
            case PharmacySummaryViews:
            case PharmacyGrnSave:
            case PharmacyGrnFinalize:
            case PharmacyGrnApprove:
            case PrintOriginalPoBillFromReprint:
            case PrintOriginalGrnBillFromReprint:    

                return "Pharmacy";

            case Clinic:
            case ClinicCalendar:
            case ClinicDisplay:
            case ClinicQueue:
            case ClinicReports:
            case ClinicSchedule:
            case ClinicSession:
                return "Clinics";

            // Collecting Centre Privileges
            case CollectingCentreManageMenu:
            case CollectingCentreBilling:
            case CCPaymentReceive:
            case SearchCCPaymentReceive:
            case IssueReferenceBook:
            case SearchIssuedReferenceBook:
            case ChangeCreditLimitInCC:
            case PayCollectingCentre:
            case CollectingCentreCreditDebitNoteMenu:
            case CollectingCentreCreditNote:
            case CollectingCentreDebitNote:
            case CollectingCentreReports:
            case ChangeCollectingCentre:
                return "Collecting Centre";
            
            // Approval Privileges
            case BillCancelRequestApproval:
            case ItemRefundRequestApproval:
            case DrawerAdjustmentRequestApproval:
            case DrawerAdjustmentDirect:
            case PettyCashCancellationApproval:
                return "Approval";

            case IssueFundTransfer:
            case ReceiveFundTransfer:
            case DeclineFundTransfer:
            case RequestFundTransfer:
            case ProcessFundTransferRequest:
            case CancelOwnFundTransfer:
            case CancelOthersFundTransfer:
            case ViewFundTransferReports:
                return "Float Transfer";

            case NursingWorkBench:
            case ShowDrugCharges:
                return "Nursing Work Bench";
                
            case WatingRoomAdmitPatient:
            case InwardAppointmentMenu:
            case AddInwardAppointment:
            case InwardAppointmentAdmission:
            case InwardAppointmentUpdate:
            case InwardAppointmentCancel:
            case InpatientClinicalAssessment:
            case InpatientClinicalDischarge:
                return "Inward";

            default:
                return this.toString();
        }
    }
}
