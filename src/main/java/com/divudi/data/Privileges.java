/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

public enum Privileges {

    // Main Menu Privileges
    TheaterIssueBHT("Theater Issue BHT"),
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
    Clinical("Clinical"),
    Store("Store"),
    Search("Search"),
    CashTransaction("Cash Transaction"),
    ChangeCreditLimitInCC("Change Credit Limit in Collecting Centre"),

    // Submenu Privileges
    OpdBilling("OPD Billing"),
    OpdCollectingCentreBillingMenu("OPD Collecting Centre Billing Menu"),
    OpdCollectingCentreBilling("OPD Collecting Centre Billing"),
    OpdCollectingCentreBillSearch("OPD Collecting Centre Bill Search"),
    OpdPreBilling("OPD Pre Billing"),
    OpdBillSearch("OPD Bill Search"),
    OpdBillItemSearch("OPD Bill Item Search"),
    OpdReprint("OPD Reprint"),
    OpdCancel("OPD Cancel"),
    OpdReturn("OPD Return"),
    OpdReactivate("OPD Reactivate"),
    OpdBillSearchEdit("OPD Bill Search Edit"),
    OpdLabReportSearch("OPD Lab Report Search"),
    OpdReprintOriginalBill("OPD Reprint Original Bill"),
    OpdAddNewRefferalDoctor("OPD Add New Referral Doctor"),
    OpdAddNewCollectingCentre("OPD Add New Collecting Centre"),
    
    AddCreditLimitInRegistration("Add Credit Limit in Registration"),
    
    InwardAdmissions("Inward Admissions"),
    InwardAdmissionsAdmission("Inward Admissions Admission"),
    InwardAdmissionsEditAdmission("Inward Admissions Edit Admission"),
    InwardAdmissionsInwardAppoinment("Inward Admissions Inward Appointment"),
    InwardRoom("Inward Room"),
    InwardRoomRoomOccupency("Inward Room Occupancy"),
    InwardRoomRoomChange("Inward Room Change"),
    InwardRoomGurdianRoomChange("Inward Guardian Room Change"),
    InwardRoomDischarge("Inward Room Discharge"),
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
    InwardSearchProfessionalBill("Inward Search Professional Bill"),
    InwardSearchFinalBill("Inward Search Final Bill"),
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
    InwardFinalBillCancel("Inward Final Bill Cancel"),
    InwardOutSideMarkAsUnPaid("Inward Outside Mark As Unpaid"),
    ShowInwardFee("Show Inward Fee"),
    InwardPharmacyMenu("Inward Pharmacy Menu"),
    InwardPharmacyIssueRequest("Inward Pharmacy Issue Request"),
    InwardPharmacyIssueRequestSearch("Inward Pharmacy Issue Request Search"),
    InwardBillSettleWithoutCheck("Inward Bill Settle Without Check"),

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
    LabSampleReceiving("Lab Sample Receiving"),
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

    ClinicalPatientSummery("Clinical Patient Summary"),
    ClinicalPatientDetails("Clinical Patient Details"),
    ClinicalPatientPhoto("Clinical Patient Photo"),
    ClinicalVisitDetail("Clinical Visit Detail"),
    ClinicalVisitSummery("Clinical Visit Summary"),
    ClinicalHistory("Clinical History"),
    ClinicalAdministration("Clinical Administration"),
    ClinicalPatientDelete("Clinical Patient Delete"),

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

    AdminManagingUsers("Admin Managing Users"),
    AdminInstitutions("Admin Institutions"),
    AdminStaff("Admin Staff"),
    AdminItems("Admin Items"),
    AdminPrices("Admin Prices"),
    AdminFilterWithoutDepartment("Admin Filter Without Department"),
    ChangeProfessionalFee("Change Professional Fee"),
    ChangeCollectingCentre("Change Collecting Centre"),

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

    SearchGrand("Search Grand"),

    CashTransactionCashIn("Cash Transaction Cash In"),
    CashTransactionCashOut("Cash Transaction Cash Out"),
    CashTransactionListToCashRecieve("Cash Transaction List to Cash Receive"),

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

    MemberShip("Membership"),
    MemberShipAdd("Membership Add"),
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
    Developers("Developers"),

    // Cashier
    AllCashierSummery("All Cashier Summary"),

    // Administration
    SearchAll("Search All"),
    ChangePreferece("Change Preference"),
    SendBulkSMS("Send Bulk SMS"),
    ClinicalAdministrationEditLetter("Clinical Administration Edit Letter"),
    ClinicalPatientAdd("Clinical Patient Add"),
    ClinicalPatientEdit("Clinical Patient Edit"),
    ClinicalPatientCommentsView("Clinical Patient Comments View"),
    ClinicalPatientCommentsEdit("Clinical Patient Comments Edit"),
    ClinicalPatientNameChange("Clinical Patient Name Change"),
    ClinicalMembershipAdd("Clinical Membership Add"),
    ClinicalMembershipEdit("Clinical Membership Edit"),
    ClinicalPatientPhoneNumberEdit("Clinical Patient Phone Number Edit"),

    // Pharmacy Disbursement
    PharmacyDisburesementMenu("Pharmacy Disbursement Menu"),
    PharmacyDisbursementRequest("Pharmacy Disbursement Request"),
    PharmacyDisbursementIssurForRequest("Pharmacy Disbursement Issue for Request"),
    PharmacyDisbursementDirectIssue("Pharmacy Disbursement Direct Issue"),
    PharmacyDisbursementRecieve("Pharmacy Disbursement Receive"),
    PharmacyDisbursementReports("Pharmacy Disbursement Reports"),

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
    PharmacySaleForCashier("Pharmacy Sale for Cashier"),
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
    PharmacyAdjustmentSaleRate("Pharmacy Adjustment Sale Rate"),
    PharmacyAdjustmentWholeSaleRate("Pharmacy Adjustment Wholesale Rate"),
    PharmacyAdjustmentExpiryDate("Pharmacy Adjustment Expiry Date"),
    PharmacyAdjustmentSearchAdjustmentBills("Pharmacy Adjustment Search Adjustment Bills"),
    PharmacyAdjustmentTransferAllStock("Pharmacy Adjustment Transfer All Stock"),

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
    ReturnWithoutRecipt("Return without Receipt"),

    PharmacyItemSearch("Pharmacy Item Search"),
    PharmacyGenarateReports("Pharmacy Generate Reports"),
    PharmacySummaryViews("Pharmacy Summary Views"),

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

    // New privileges for couriers
    CourierCollectSamples("Courier Collect Samples"),
    CourierHandoverSamplesToLab("Courier Handover Samples to Lab"),
    CourierViewReports("Courier View Reports"),
    CourierPrintReports("Courier Print Reports"),
    CourierViewStatistics("Courier View Statistics"),
    CourierViewBillReports("Courier View Bill Reports"),
    CourierViewPaymentReports("Courier View Payment Reports");

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
            case OpdReturn:
            case OpdBilling:
            case OpdReprint:
            case OpdBillSearch:
            case OpdPreBilling:
            case OpdReactivate:
            case OpdBillItemSearch:
            case OpdBillSearchEdit:
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
                return "Lab";
            case Pharmacy:
            case PharmacySaleWh:
            case PharmacySearch:
            case PharmacyReports:
            case PharmacySummery:
            case PharmacyPurchase:
            case PharmacyTransfer:
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
            case ReturnWithoutRecipt:
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
            case PharmacySaleForCashier:
            case PharmacySaleWithOutStock:
            case PharmacySearchSaleBill:
            case PharmacySearchSalePreBill:
            case PharmacySearchSaleBillItems:
            case PharmacyReturnItemsOnly:
            case PharmacyReturnItemsAndPayments:
            case PharmacySearchReturnBill:
            case PharmacyAddToStock:

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
            case PharmacyAdjustmentSaleRate:
            case PharmacyAdjustmentWholeSaleRate:
            case PharmacyAdjustmentExpiryDate:
            case PharmacyAdjustmentSearchAdjustmentBills:
            case PharmacyAdjustmentTransferAllStock:

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

                return "Pharmacy";
            default:
                return this.toString();
        }
    }
}
