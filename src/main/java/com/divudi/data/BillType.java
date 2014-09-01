
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

/**
 *
 * @author Buddhika
 */
public enum BillType {

    OpdBathcBill,
    SurgeryBill,
    LabBill,
    PaymentBill,
    OpdBill,
    InwardPaymentBill,
    InwardFinalBill,
    InwardAppointmentBill,
    InwardBill,
    InwardProfessional,
    InwardIntrimBill,
    @Deprecated
    InwardAdditionalBill,
    InwardOutSideBill,
    AdmissionBill,
    CashRecieveBill,
    PettyCash,
    AgentPaymentReceiveBill,
    @Deprecated
    PharmacyBill, //Cash In
    PharmacySale,
    @Deprecated
    SandryGrn,
    PharmacyIssue,
    @Deprecated
    PharmacyBhtIssue,
    PharmacyBhtPre,
    @Deprecated
    StoreBhtIssue,
    StoreBhtPre,
    StoreIssue,
    @Deprecated
    StoreTransferIssue,
    PharmacyPre,
    PharmacyOrder,
    PharmacyOrderApprove,
    PharmacyGrnBill,//Cash out
    PharmacyGrnReturn,
    GrnPayment,
    PharmacyPurchaseBill, //Cash out
    PurchaseReturn,// Purchase Return
    PharmacyTransferRequest,
    PharmacyTransferIssue,
    PharmacyDirectIssue,
    PharmacyTransferReceive,
    PharmacyDirectReceive,
    PharmacyAdjustment,
    DrawerAdjustment,
    PharmacyMajorAdjustment,
    ChannelPaid,
    ChannelCredit,
    ChannelProPayment,
    gpBooking,
    gpSettling,
    Appointment,
    GrnPaymentBill,
    GrnPaymentReturn,
    GrnPaymentCancell,
    GrnPaymentCancellReturn,
    CashIn,
    CashOut,
    ClinicalOpdBooking;

    public String getLabel() {
        switch (this) {
            case OpdBill:
                return "OPD ";
            case PaymentBill:
                return "Professional Pay ";
            case PettyCash:
                return "Petty Cash ";
            case CashRecieveBill:
                return "Credit Company Payment ";
            case AgentPaymentReceiveBill:
                return "Agent Payment";
            case InwardPaymentBill:
                return "Inward Payment";
            case PharmacyOrder:
                return "Purchase Order Request";
            case PharmacyOrderApprove:
                return "Purchase Order Aproved";
            case PharmacyGrnBill:
                return "Good Receive Note";
            case PharmacyGrnReturn:
                return "Good Receive Note Return";
            case PharmacyPurchaseBill:
                return "Pharmacy Purchase";
            case PurchaseReturn:
                return "Pharmacy Purchase Return";
            case PharmacySale:
                return "Pharmacy Sale Bill";
            case PharmacyPre:
                return "Pharmacy Pre Bill";
            case PharmacyAdjustment:
                return "Pharmacy Adjustment Bill";
            case GrnPayment:
                return "Grn Payment";
            case PharmacyTransferRequest:
                return "Pharmacy Transfer Request";
            case PharmacyTransferIssue:
                return "Pharmacy Transfer Issue";
            case PharmacyTransferReceive:
                return "Pharmacy Transfer Receive";
            case CashIn:
                return "Cash In Transaction";
            case CashOut:
                return "Cash Out Transaction";

        }

        return "";
    }
}
