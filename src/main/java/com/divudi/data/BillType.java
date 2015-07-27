
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
    OpdBathcBillPre,
    SurgeryBill,
    LabBill,
    PaymentBill,//To Pay Professional Payments
    OpdBill,
    OpdPreBill,
    InwardPaymentBill,
    InwardFinalBill,
    InwardAppointmentBill,
    InwardBill,
    InwardProfessional,
    InwardProfessionalEstimates,
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
    PharmacyWholeSale,
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
    PharmacyWholesalePre,
    PharmacyOrder,
    PharmacyOrderApprove,
    PharmacyGrnBill,//Cash out
    PharmacyGrnReturn,
    PharmacyReturnWithoutTraising,
    GrnPayment,
    GrnPaymentPre,
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
    @Deprecated
    ChannelProPayment,
    ChannelAgencyPayment,
    ChannelAgencyCommission,
    gpBooking,
    gpSettling,
    Appointment,
    @Deprecated
    GrnPaymentBill,
    @Deprecated
    GrnPaymentReturn,
    @Deprecated
    GrnPaymentCancell,
    @Deprecated
    GrnPaymentCancellReturn,
    CashIn,
    CashOut,
    @Deprecated
    ChannelCredit,
    ClinicalOpdBooking,
    StorePurchase,
    StoreGrnBill,
    StoreAdjustment,
    StoreSale,
    StoreOrderApprove,
    StoreGrnReturn,
    StorePre,
    StoreOrder,
    StorePurchaseReturn,;

    public String getLabel() {
        switch (this) {
            case OpdBill:
                return "OPD Bill";
            case PaymentBill:
                return "Professional Payment Bill";
            case PettyCash:
                return "Petty Cash Payment Bill";
            case CashRecieveBill:
                return "Credit Company Payment Receive Bill";
            case AgentPaymentReceiveBill:
                return "Agent Payment Receive Bill";
            case InwardPaymentBill:
                return "Inward Payment Receive Bill";
            case PharmacyOrder:
                return "Purchase Order Request";
            case PharmacyOrderApprove:
                return "Purchase Order Aproval";
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
            case PharmacyWholeSale:
                return "Pharmacy WholeSale Bill";
            case PharmacyPre:
                return "Pharmacy Sale Bill (Pre)";
            case PharmacyAdjustment:
                return "Pharmacy Adjustment";
            case GrnPayment:
                return "Grn Payment";
            case GrnPaymentPre:
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
            case StoreOrder:
                return "Store Order Request";
            case ChannelPaid:
                return "Channel Settle";
            case StoreOrderApprove:
                return "Store Order Aproved";
            case StoreGrnBill:
                return "Store Good Receive Note";
            case StoreGrnReturn:
                return "Store Good Receive Note Return";
            case StorePurchase:
                return "Store Purchase";
            case StorePurchaseReturn:
                return "Store Purchase Return";
            case StoreSale:
                return "Store Sale Bill";
            case StorePre:
                return "Store Sale Bill (Pre)";
            case StoreAdjustment:
                return "Store Adjustment";
            case StoreTransferRequest:
                return "Store Transfer Request";
            case StoreTransferIssue:
                return "Store Transfer Issue";
            case PharmacyIssue:
                return "Pharmacy Issue";
            case PharmacyBhtPre:
                return "Pharmacy BHT Issue (Pre)";
            case OpdPreBill:
                return "OPD Bills To Pay";
            case OpdBathcBill:
                return "OPD Accepet Payment";

        }

        return "Other";
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
