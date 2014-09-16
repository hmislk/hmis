
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Buddhika
 */
public enum BillType {

    //Dont Use in With BillType in Channel
    @Deprecated
    ChannelCashFlow(null),
    @Deprecated
    ChannelCreditFlow(null),
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
    //    @Deprecated Piumi requested issue 60 
    StoreTransferIssue,
    StoreTransferReceive,
    StoreTransferRequest,
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
    ChannelCash(ChannelCashFlow),
    ChannelPaid(ChannelCashFlow),
    ChannelAgent(ChannelCashFlow),
    ChannelOnCall(ChannelCreditFlow),
    ChannelStaff(ChannelCreditFlow),
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
    @Deprecated
    ChannelCredit,
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
            case ChannelAgent:
                return "Channel Agent";
            case ChannelCash:
                return "Channel Cash";
            case ChannelOnCall:
                return "Channel On Call";
            case ChannelStaff:
                return "Channel Staff";

        }

        return "";
    }

    private BillType parent = null;

    public BillType getParent() {
        return parent;
    }

    public void setParent(BillType parent) {
        this.parent = parent;
    }

    private BillType(BillType parent) {
        this.parent = parent;
        if (this.parent != null) {
            this.parent.addChild(this);
        }
    }

    private BillType() {
    }

    private final List<BillType> children = new ArrayList<>();

    public BillType[] children() {
        return children.toArray(new BillType[children.size()]);
    }

    public BillType[] allChildren() {
        List<BillType> list = new ArrayList<>();
        addChildren(this, list);
        return list.toArray(new BillType[list.size()]);
    }

    private static void addChildren(BillType root, List<BillType> list) {
        list.addAll(root.children);
        for (BillType child : root.children) {
            addChildren(child, list);
        }
    }

    private void addChild(BillType child) {
        this.children.add(child);
    }

    public boolean is(BillType other) {
        if (other == null) {
            return false;
        }

        for (BillType t = this; t != null; t = t.parent) {
            if (other == t) {
                return true;
            }
        }
        return false;
    }

}
