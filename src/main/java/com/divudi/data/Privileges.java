/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 *
 * @author www.divudi.com
 */
public enum Privileges {

    //Main Menu Privileges
    TheaterIssueBHT,
    Opd,
    Inward,
    Lab,
    Pharmacy,
    Payment,
    Hr,
    Reports,
    User,
    Admin,
    Channelling,
    Clinical,
    Store,
    Search,
    CashTransaction,
    //Submenu Privileges
    OpdBilling,
    OpdCollectingCentreBillingMenu,
    OpdCollectingCentreBilling,
    OpdCollectingCentreBillSearch,
    OpdPreBilling,
    OpdBillSearch,
    OpdBillItemSearch,
    OpdReprint,
    OpdCancel,
    OpdReturn,
    OpdReactivate,
    OpdBillSearchEdit,
    InwardAdmissions,
    InwardAdmissionsAdmission,
    InwardAdmissionsEditAdmission,
    InwardAdmissionsInwardAppoinment,
    InwardRoom,
    InwardRoomRoomOccupency,
    InwardRoomRoomChange,
    InwardRoomGurdianRoomChange,
    InwardRoomDischarge,
    InwardServicesAndItems,
    InwardServicesAndItemsAddServices,
    InwardServicesAndItemsAddOutSideCharges,
    InwardServicesAndItemsAddProfessionalFee,
    InwardServicesAndItemsAddTimedServices,
    InwardBilling,
    InwardBillingInterimBill,
    InwardBillingInterimBillSearch,
    InwardSearch,
    InwardSearchServiceBill,
    InwardSearchProfessionalBill,
    InwardSearchFinalBill,
    InwardReport,
    InwardFinalBillReportEdit,
    InwardAdministration,
    InwardAdditionalPrivilages,
    InwardBillSearch,
    InwardBillItemSearch,
    InwardBillReprint,
    InwardCancel,
    InwardReturn,
    InwardReactivate,
    InwardCheck,
    InwardUnCheck,
    InwardFinalBillCancel,
    InwardOutSideMarkAsUnPaid,
    ShowInwardFee,
    InwardPharmacyMenu,
    InwardPharmacyIssueRequest,
    InwardPharmacyIssueRequestSearch,
    InwardBillSettleWithoutCheck,
    LabBilling,
    LabBillCancelSpecial,
    LabBillRefundSpecial,
    LabCasheirBillSearch,
    LabCashier,
    LabBillSearchCashier,
    LabBillSearch,
    LabBillItemSearch,
    LabBillCancelling,
    CollectingCentreCancelling,
    LabBillReturning,
    LabBillReprint,
    LabBillRefunding,
    LabBillReactivating,
    LabSampleCollecting,
    LabSampleReceiving,
    LabReportFormatEditing,
    LabDataentry,
    LabAutherizing,
    LabDeAutherizing,
    LabRevertSample,
    LabPrinting,
    LabReprinting,
    LabReportEdit,
    LabReportPrint,
    AdminReportSearch,
    LabReportSearchByDepartment,
    LabSummeries,
    LabSummeriesLevel1,
    LabSummeriesLevel2,
    LabSummeriesLevel3,
    LabReportSearchOwn,
    LabReportSearchAll,
    LabReceive,
    LabEditPatient,
    LabInvestigationFee,
    LabAddInwardServices,
    LabReportSearchByLoggedInstitution,
    IncomeReport,
    LabReport,
    DuesAndAccess,
    CheckEnteredData,
    LabAdiministrator,
    LabReports,
    LabItems,
    LabItemFeeUpadate,
    LabItemFeeDelete,
    LabPatientDetailsEdit,
    LabLists,
    LabSetUp,
    LabInwardBilling,
    LabInwardSearchServiceBill,
    LabCollectingCentreBilling,
    LabCCBilling,
    LabCCBillingSearch,
    LabReportSearch,
    LabReporting,
    //dont remove
    LabSearchBillLoggedInstitution,
    PaymentBilling,
    PaymentBillSearch,
    PaymentBillReprint,
    PaymentBillCancel,
    PaymentBillRefund,
    PaymentBillReactivation,
    ReportsSearchCashCardOwn,
    ReportsSearchCreditOwn,
    ReportsItemOwn,
    ReportsSearchCashCardOther,
    ReportSearchCreditOther,
    ReportsItemOther,
    PharmacyOrderCreation,
    PharmacyOrderApproval,
    PharmacyOrderCancellation,
    PharmacySale,
    PharmacySaleWithoutStock,
    PharmacySaleReprint,
    PharmacySaleCancel,
    PharmacySaleReturn,
    //Wholesale
    PharmacySaleWh,
    PharmacySaleReprintWh,
    PharmacySaleCancelWh,
    PharmacySaleReturnWh,
    //end wholesale
    PharmacyInwardBilling,
    PharmacyInwardBillingCancel,
    PharmacyInwardBillingReturn,
    PharmacyGoodReceive,
    //Wholesale
    PharmacyGoodReceiveWh,
    //end Wholesale
    PharmacyGoodReceiveCancel,
    PharmacyGoodReceiveReturn,
    PharmacyGoodReceiveEdit,
    PharmacyPurchase,
    //Wholesale
    PharmacyPurchaseWh,
    //Whalesale
    PharmacyPurchaseReprint,
    PharmacyPurchaseCancellation,
    PharmacyPurchaseReturn,
    PharmacyStockAdjustment,
    PharmacyStockAdjustmentSingleItem,
    PharmacyReAddToStock,
    PharmacyStockIssue,
    PharmacyDealorPayment,
    PharmacySearch,
    PharmacyReports,
    PharmacyTransfer,
    PharmacySummery,
    PharmacyAdministration,
    PharmacySetReorderLevel,
    PharmacyReturnWithoutTraising,
    PharmacyBHTIssueAccept,
    //theater
    Theatre,
    TheatreAddSurgery,
    TheatreBilling,
    TheaterTransfer,
    TheaterTransferRequest,
    TheaterTransferIssue,
    TheaterTransferRecieve,
    TheaterTransferReport,
    TheaterReports,
    TheaterSummeries,
    TheaterIssue,
    TheaterIssuePharmacy,
    TheaterIssueStore,
    TheaterIssueStoreBhtBilling,
    TheaterIssueStoreBhtSearchBill,
    TheaterIssueStoreBhtSearchBillItem,
    TheaterIssueOpd,
    TheaterIssueOpdForCasheir,
    TheaterIssueOpdSearchPreBill,
    TheaterIssueOpdSearchPreBillForReturnItemOnly,
    TheaterIssueOpdSearchPreBillReturn,
    TheaterIssueOpdSearchPreBillAddToStock,
    ClinicalPatientSummery,
    ClinicalPatientDetails,
    ClinicalPatientPhoto,
    ClinicalVisitDetail,
    ClinicalVisitSummery,
    ClinicalHistory,
    ClinicalAdministration,
    ClinicalPatientDelete,
    ChannelAdd,
    ChannelCancel,
    ChannelRefund,
    ChannelReturn,
    ChannelView,
    ChannelDoctorPayments,
    ChannelDoctorPaymentCancel,
    ChannelViewHistory,
    ChannelCreateSessions,
    ChannelManageSessions,
    ChannelAdministration,
    ChannelAgencyReports,
    AdminManagingUsers,
    AdminInstitutions,
    AdminStaff,
    AdminItems,
    AdminPrices,
    AdminFilterWithoutDepartment,
    ChangeProfessionalFee,
    ChangeCollectingCentre,
    StoreIssue,
    StoreIssueInwardBilling,
    StoreIssueSearchBill,
    StoreIssueBillItems,
    StorePurchase,
    StorePurchaseOrder,
    StorePurchaseOrderApprove,
    StorePurchaseOrderApproveSearch,
    StorePurchaseGRNRecive,
    StorePurchaseGRNReturn,
    StorePurchasePurchase,
    StoreTransfer,
    StoreTransferRequest,
    StoreTransferIssue,
    StoreTransferRecive,
    StoreTransferReport,
    StoreAdjustment,
    StoreAdjustmentDepartmentStock,
    StoreAdjustmentStaffStock,
    StoreAdjustmentPurchaseRate,
    StoreAdjustmentSaleRate,
    StoreDealorPayment,
    StoreDealorPaymentDueSearch,
    StoreDealorPaymentDueByAge,
    StoreDealorPaymentPayment,
    StoreDealorPaymentPaymentGRN,
    StoreDealorPaymentPaymentGRNSelect,
    StoreDealorPaymentGRNDoneSearch,
    StoreSearch,
    StoreReports,
    StoreSummery,
    StoreAdministration,
    SearchGrand,
    CashTransactionCashIn,
    CashTransactionCashOut,
    CashTransactionListToCashRecieve,
    ChannellingChannelBooking,
    ChannellingFutureChannelBooking,
    ChannellingPastBooking,
    ChannellingBookedList,
    ChannellingDoctorLeave,
    ChannellingDoctorLeaveByDate,
    ChannellingDoctorLeaveByServiceSession,
    ChannellingChannelSheduling,
    ChannellingChannelShedulRemove,
    ChannellingChannelShedulName,
    ChannellingChannelShedulStartingNo,
    ChannellingChannelShedulRoomNo,
    ChannellingChannelShedulMaxRowNo,
    ChannellingChannelAgentFee,
    ChannellingDoctorSessionView,
    ChannellingPayment,
    ChannellingPaymentPayDoctor,
    ChannellingPaymentDueSearch,
    ChannellingPaymentDoneSearch,
    ChannellingApoinmentNumberCountEdit,
    ChannellingEditSerialNo,
    ChannellingEditPatientDetails,
    ChannellingPrintInPastBooking,
    ChannellingEditCreditLimitUserLevel,
    ChannellingEditCreditLimitAdminLevel,
    ChannelReports,
    ChannelSummery,
    ChannelManagement,
    ChannelAgencyAgencies,
    ChannelAgencyCreditLimitUpdate,
    ChannelAgencyCreditLimitUpdateBulk,
    ChannelAddChannelBookToAgency,
    ChannelManageSpecialities,
    ChannelManageConsultants,
    ChannelEditingAppoinmentCount,
    ChannelAddChannelingConsultantToInstitutions,
    ChannelFeeUpdate,
    ChannelCrdeitNote,
    ChannelCrdeitNoteSearch,
    ChannelDebitNote,
    ChannelDebitNoteSearch,
    ChannelCashCancelRestriction,
    ChannelBookingChange,
    ChannelBookingBokking,
    ChannelBookingReprint,
    ChannelBookingCancel,
    ChannelBookingRefund,
    ChannelBookingSettle,
    ChannelBookingSearch,
    ChannelBookingViews,
    ChannelBookingDocPay,
    ChannelBookingRestric,
    ChannelCashierTransaction,
    ChannelCashierTransactionIncome,
    ChannelCashierTransactionIncomeSearch,
    ChannelCashierTransactionExpencess,
    ChannelCashierTransactionExpencessSearch,
    ChannelActiveVat,
    MemberShip,
    MemberShipAdd,
    MemberShipEdit,
    MembershipReports,
    MembershipDiscountManagement,
    MembershipAdministration,
    MembershipSchemes,
    MemberShipInwardMemberShip,
    MemberShipInwardMemberShipSchemesDicounts,
    MemberShipInwardMemberShipInwardMemberShipReport,
    MemberShipOpdMemberShipDis,
    MemberShipOpdMemberShipDisByDepartment,
    MemberShipOpdMemberShipDisByCategory,
    MemberShipOpdMemberShipDisOpdMemberShipReport,
    MemberShipMemberDeActive,
    MemberShipMemberReActive,
    HrAdmin,
    HrReports,
    HrReportsLevel1,
    HrReportsLevel2,
    HrReportsLevel3,
    EmployeeHistoryReport,
    hrDeleteLateLeave,
    HrGenerateSalary,
    HrGenerateSalarySpecial,
    HrAdvanceSalary,
    HrPrintSalary,
    HrWorkingTime,
    HrRosterTable,
    HrUploadAttendance,
    HrAnalyseAttendenceByRoster,
    HrAnalyseAttendenceByStaff,
    HrForms,
    HrLeaveForms,
    HrAdditionalForms,
    HrEditRetiedDate,
    HrRemoveResignDate,
    Developers,
    //Cashier
    AllCashierSummery,
    //Administration
    SearchAll,
    ChangePreferece,
    SendBulkSMS,
    ClinicalAdministrationEditLetter,
    ClinicalPatientAdd,
    ClinicalPatientEdit,
    ClinicalPatientCommentsView,
    ClinicalPatientCommentsEdit,
    ClinicalPatientNameChange,
    ClinicalMembershipAdd,
    ClinicalMembershipEdit,
    ClinicalPatientPhoneNumberEdit,;

    public String getLabel() {
        switch (this) {
            case Opd:
                return "Online Settlement";
            case OpdCancel:
                return "OPD Bill Cancellation";
            case OpdReturn:
                return "OPD Bill Return";
            default:
                return this.toString();

        }
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
            case PharmacySale:
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
                return "Pharmacy";
            default:
                return this.toString();

        }
    }

}
