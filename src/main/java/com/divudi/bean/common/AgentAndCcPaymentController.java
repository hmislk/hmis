/*
 * To change this currentlate, choose Tools | currentlates
 * (94) 71 5812399 open the currentlate in the editor.
 */
package com.divudi.bean.common;

import com.divudi.core.util.JsfUtil;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.HistoryType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.core.entity.AgentHistory;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Payment;
import com.divudi.core.facade.AgentHistoryFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.service.BillService;
import com.divudi.service.PaymentService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class AgentAndCcPaymentController implements Serializable {
// old Name - AgentPaymentRecieveBillController

    private Bill current;
    @EJB
    private BillNumberGenerator billNumberGenerator;
    private boolean printPreview = false;
    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    private SessionController sessionController;
    @Inject
    InstitutionController institutionController;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    AgentHistoryFacade agentHistoryFacade;
    @EJB
    CashTransactionBean cashTransactionBean;
    @EJB
    BillService billService;
    @EJB
    PaymentService paymentService;

    @Inject
    private BillBeanController billBean;
    @Inject
    AgentAndCcApplicationController collectingCentreApplicationController;
    @Inject
    private PaymentSchemeController paymentSchemeController;

    private PatientEncounter patientEncounter;
    private BillItem currentBillItem;
    private List<BillItem> billItems;
    private int index;
    private PaymentMethodData paymentMethodData;
    String comment;
    double amount;
    boolean ccDepositSettlingStarted = false;
    
    private Institution collectingCentre;

    public void createAndAddBillItemToCcPaymentReceiptBill() {
        getCurrentBillItem().setNetValue(getCurrent().getNetTotal());
        getCurrentBillItem().setGrossValue(getCurrent().getNetTotal());
        getCurrentBillItem().setBillSession(null);
        getCurrentBillItem().setDiscount(0.0);
        getCurrentBillItem().setItem(null);
        getCurrentBillItem().setQty(1.0);
        getCurrentBillItem().setRate(getCurrent().getNetTotal());
        getBillItems().add(getCurrentBillItem());
        currentBillItem = null;
    }
    
    public List<Institution> getAgentInstitutions() {
        String j;
        j = "select i "
                + " from Institution i "
                + " where i.retired=:ret"
                + " and i.institutionType = :type"
                + " order by i.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("type", InstitutionType.Agency);
        return getInstitutionFacade().findByJpql(j, m);
    }

    public AgentAndCcPaymentController() {
    }

    private boolean errorCheck() {
        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Agency");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        return false;
    }

    private boolean errorCheckForCcPaymentReceiptBill() {
        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Collecting Centre");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if(getCurrent().getNetTotal()<=0){
            JsfUtil.addErrorMessage("Enter a value");
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        return false;
    }

    private boolean errorCheckForAgencyPaymentReceiptBill() {
        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select a Agency");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payement method");
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(getCurrent().getPaymentMethod(), paymentMethodData)) {
            return true;
        }

        return false;
    }

    private boolean errorCheckCreditNoteDebitNote() {
        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Agency");
            return true;
        }
        if (getAmount() < 0.0) {
            JsfUtil.addErrorMessage("Please Enter Correct Value");
            return true;
        }

        return false;
    }

    private void saveBill(BillType billType, BillNumberSuffix billNumberSuffix) {
        getCurrent().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.BilledBill, billNumberSuffix));
//        getCurrent().setDeptId(getBillNumberBean().departmentBillNumberGenerator(sessionController.getDepartment(), billType, BillClassType.BilledBill, billNumberSuffix));
        getCurrent().setDeptId(getBillNumberGenerator().departmentBillNumberGenerator(sessionController.getDepartment(), sessionController.getDepartment(), billType, BillClassType.BilledBill));
        getCurrent().setBillType(billType);

        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());

        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        getCurrent().setNetTotal(getCurrent().getNetTotal());

        current.setComments(comment);

        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String navigateToCcDeposit() {
        ccDepositSettlingStarted = false;
        return "/collecting_centre/collecting_centre_deposit_bill?faces-redirect=true";
    }

    public void collectingCentrePaymentRecieveSettleBill() {
        if (ccDepositSettlingStarted) {
            JsfUtil.addErrorMessage("Already Started");
            return;
        }
        ccDepositSettlingStarted = true;
        if (errorCheckForCcPaymentReceiptBill()) {
            ccDepositSettlingStarted = false;
            return;
        }


//        addPaymentMethordValueToTotal(current, getCurrent().getPaymentMethod());
        createAndAddBillItemToCcPaymentReceiptBill();
//        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getCurrent().setTotal(getCurrent().getNetTotal());

        String deptId;

        deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(
                getSessionController().getDepartment(), BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL);

        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);
        getCurrent().setBillType(BillType.CollectingCentrePaymentReceiveBill);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL);
        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        getCurrent().setNetTotal(getCurrent().getNetTotal());

        if (getCurrent().getId() == null) {
            getBillFacade().createAndFlush(getCurrent());
        } else {
            getBillFacade().editAndFlush(getCurrent());
        }
        saveBillItem();
        if (getCurrent() != null) {
            if (getCurrent().getFromInstitution() != null) {
                if (getCurrent().getFromInstitution().getInstitutionType() == InstitutionType.CollectingCentre) {
                    getCurrent().setCollectingCentre(getCurrent().getFromInstitution());
                }
            }
        }

        List<Payment> ps = paymentService.createPayment(current, paymentMethodData);
        collectingCentreApplicationController.updateCcBalance(
                current.getFromInstitution(),
                0,
                getCurrent().getNetTotal(),
                0,
                getCurrent().getNetTotal(),
                HistoryType.CollectingCentreDeposit,
                getCurrent());

        Institution ccToResetAllowedCredit = getCurrent().getFromInstitution();
        ccToResetAllowedCredit = institutionFacade.findWithoutCache(ccToResetAllowedCredit.getId());
        
        if ((getCurrent().getNetTotal() > (ccToResetAllowedCredit.getMaxCreditLimit() - ccToResetAllowedCredit.getStandardCreditLimit())) && (ccToResetAllowedCredit.getMaxCreditLimit() != ccToResetAllowedCredit.getStandardCreditLimit())) {
            ccToResetAllowedCredit.setAllowedCredit(getCurrent().getFromInstitution().getStandardCreditLimit());
            getInstitutionFacade().editAndCommit(ccToResetAllowedCredit);
        }
        JsfUtil.addSuccessMessage("Bill Saved");
        ccDepositSettlingStarted = false;
        printPreview = true;
    }

    public void updateBallance(Institution ins, double transactionValue, HistoryType historyType, Bill bill) {
        AgentHistory agentHistory = new AgentHistory();
        agentHistory.setCreatedAt(new Date());
        agentHistory.setCreater(getSessionController().getLoggedUser());
        agentHistory.setBill(bill);
        // agentHistory.setBillItem(billItem);
        //agentHistory.setBillSession(selectedBillSession);
        agentHistory.setBalanceBeforeTransaction(ins.getBallance());
        agentHistory.setTransactionValue(transactionValue);
        //agentHistory.setReferenceNumber(refNo);
        agentHistory.setHistoryType(historyType);
        agentHistoryFacade.create(agentHistory);

        ins.setBallance(ins.getBallance() + transactionValue);
        getInstitutionFacade().edit(ins);

    }

    public void agencyPaymentRecieveSettleBill() {
        if (errorCheckForAgencyPaymentReceiptBill()) {
            return;
        }
        addPaymentMethordValueToTotal(current, getCurrent().getPaymentMethod());

        if (getCurrent().getNetTotal() <= 0) {
            JsfUtil.addErrorMessage("Add Valid Amount");
            return;
        }

        createAndAddBillItemToCcPaymentReceiptBill();

        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getCurrent().setTotal(getCurrent().getNetTotal());

        String deptId;

        deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(
                getSessionController().getInstitution(),
                getSessionController().getDepartment(),
                BillType.AgentPaymentReceiveBill,
                BillClassType.BilledBill);

        getCurrent().setInsId(deptId);
        getCurrent().setDeptId(deptId);
        getCurrent().setBillType(BillType.AgentPaymentReceiveBill);
        getCurrent().setBillTypeAtomic(BillTypeAtomic.AGENCY_PAYMENT_RECEIVED);
        getCurrent().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getCurrent().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getCurrent().setBillDate(new Date());
        getCurrent().setBillTime(new Date());
        getCurrent().setCreatedAt(new Date());
        getCurrent().setCreater(getSessionController().getLoggedUser());

        getCurrent().setNetTotal(getCurrent().getNetTotal());

        current.setComments(comment);

        if (getCurrent().getId() == null) {
            getBillFacade().create(getCurrent());
        } else {
            getBillFacade().edit(getCurrent());
        }

        updateBallance(current.getFromInstitution(), current.getNetTotal(), HistoryType.AgentBalanceUpdateBill, current);
        saveBillItem();

        List<Payment> p = billService.createPayment(current, getCurrent().getPaymentMethod(), paymentMethodData);
//        drawerController.updateDrawerForIns(p); > Done during bill Service
//        collectingCentreApplicationController.updateCcBalance(
//                current.getFromInstitution(),
//                0,
//                getCurrent().getNetTotal(),
//                0,
//                getCurrent().getNetTotal(),
//                HistoryType.CollectingCentreDeposit,
//                getCurrent());
//        System.out.println(current.getFromInstitution().getBallance());
//        if ((getCurrent().getNetTotal() > (getCurrent().getFromInstitution().getMaxCreditLimit() - getCurrent().getFromInstitution().getStandardCreditLimit())) && (getCurrent().getFromInstitution().getMaxCreditLimit() != getCurrent().getFromInstitution().getStandardCreditLimit())) {
//            getCurrent().getFromInstitution().setAllowedCredit(getCurrent().getFromInstitution().getStandardCreditLimit());
//            getInstitutionFacade().edit(getCurrent().getFromInstitution());
//            collectingCentreApplicationController.updateCcBalance(
//                    current.getFromInstitution(),
//                    0,
//                    0,
//                    0,
//                    0,
//                    HistoryType.CollectingCentreBalanceUpdateBill,
//                    getCurrent(),
//                    "Agent Payment Allowed Credit Limit Reset");
//        }

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;
    }

    public void channellAgencyPaymentRecieveSettleBill() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        settleBill(BillType.AgentPaymentReceiveBill, HistoryType.ChannelDeposit, HistoryType.AgentBalanceUpdateBill, BillNumberSuffix.AGNPAY);

    }

    public void channelAgencyCreditNoteSettleBill() {
        if (errorCheckCreditNoteDebitNote()) {
            return;
        }
        getCurrent().setPaymentMethod(PaymentMethod.Slip);
        getCurrent().setNetTotal(getAmount());
        creditDebitNote(BillType.AgentCreditNoteBill, HistoryType.ChannelCreditNote, HistoryType.AgentBalanceUpdateBill, BillNumberSuffix.AGNCN);

    }

    public void channelAgencyDebitNoteSettleBill() {
        if (errorCheckCreditNoteDebitNote()) {
            return;
        }
        getCurrent().setPaymentMethod(PaymentMethod.Slip);
        getCurrent().setNetTotal(0 - getAmount());
        creditDebitNote(BillType.AgentDebitNoteBill, HistoryType.ChannelDebitNote, HistoryType.AgentBalanceUpdateBill, BillNumberSuffix.AGNDN);

    }

    public void collectingCenterCreditNoteSettleBill() {
        if (errorCheckCreditNoteDebitNote()) {
            return;
        }
        getCurrent().setPaymentMethod(PaymentMethod.Slip);
        getCurrent().setNetTotal(getAmount());
        getCurrent().setBillTypeAtomic(BillTypeAtomic.CC_CREDIT_NOTE);

        creditDebitNote(BillType.CollectingCentreCreditNoteBill,
                HistoryType.CollectingCentreCreditNote, HistoryType.CollectingCentreBalanceUpdateBill, BillNumberSuffix.CCCN);

        billFacade.edit(getCurrent());

        //        Institution collectingCentre,
//            double hospitalFee,
//            double collectingCentreFee,
//            double staffFee,
//            double transactionValue,
//            HistoryType historyType,
//            Bill bill
//
        collectingCentreApplicationController.updateCcBalance(
                getCurrent().getCollectingCentre(),
                0,
                0,
                0,
                getCurrent().getNetTotal(),
                HistoryType.CollectingCentreCreditNote,
                getCurrent());

    }

    public void collectingCenterDebitNoteSettleBill() {
        if (errorCheckCreditNoteDebitNote()) {
            return;
        }
        getCurrent().setPaymentMethod(PaymentMethod.Slip);
        getCurrent().setNetTotal(0 - getAmount());
        getCurrent().setBillTypeAtomic(BillTypeAtomic.CC_DEBIT_NOTE);

        creditDebitNote(BillType.CollectingCentreDebitNoteBill, HistoryType.CollectingCentreDebitNote, HistoryType.CollectingCentreBalanceUpdateBill, BillNumberSuffix.CCDN);

        billFacade.edit(getCurrent());

        //        Institution collectingCentre,
//            double hospitalFee,
//            double collectingCentreFee,
//            double staffFee,
//            double transactionValue,
//            HistoryType historyType,
//            Bill bill
//
        collectingCentreApplicationController.updateCcBalance(
                getCurrent().getCollectingCentre(),
                0,
                0,
                0,
                getCurrent().getNetTotal(),
                HistoryType.CollectingCentreDebitNote,
                getCurrent());

    }

    private void creditDebitNote(BillType billType, HistoryType historyType, HistoryType updatHistoryType, BillNumberSuffix billNumberSuffix) {

        settleBill(billType, historyType, updatHistoryType, billNumberSuffix);

    }

    public void collectingCenterPaymentReciptBill(BillType billType, HistoryType historyType, HistoryType updatHistoryType, BillNumberSuffix billNumberSuffix) {
        addPaymentMethordValueToTotal(current, getCurrent().getPaymentMethod());
        createAndAddBillItemToCcPaymentReceiptBill();
        if (errorCheck()) {
            return;
        }
        getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());

        getCurrent().setTotal(getCurrent().getNetTotal());

        saveBill(billType, billNumberSuffix);
        saveBillItem();
        billService.createPayment(current, getCurrent().getPaymentMethod(), paymentMethodData);
        //for channel agencyHistory Update
        createAgentHistory(getCurrent().getFromInstitution(), getCurrent().getNetTotal(), historyType, getCurrent());
        //for channel agencyHistory Update

        //Update Agent Max Credit Limit
        if ((getCurrent().getNetTotal() > (getCurrent().getFromInstitution().getMaxCreditLimit() - getCurrent().getFromInstitution().getStandardCreditLimit())) && (getCurrent().getFromInstitution().getMaxCreditLimit() != getCurrent().getFromInstitution().getStandardCreditLimit())) {
            institutionController.createAgentCreditLimitUpdateHistory(getCurrent().getFromInstitution(), getCurrent().getFromInstitution().getAllowedCredit(), getCurrent().getFromInstitution().getStandardCreditLimit(), updatHistoryType, "Agent Payment Allowed Credit Limit Update");
            getCurrent().getFromInstitution().setAllowedCredit(getCurrent().getFromInstitution().getStandardCreditLimit());
            getInstitutionFacade().edit(getCurrent().getFromInstitution());
        }
        //Update Agent Max Credit Limit

        ///////////////////
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    public void settleBill(BillType billType, HistoryType historyType, HistoryType updatHistoryType, BillNumberSuffix billNumberSuffix) {
//        addPaymentMethordValueToTotal(current, getCurrent().getPaymentMethod());
        createAndAddBillItemToCcPaymentReceiptBill();
        if (!billType.equals(BillType.AgentDebitNoteBill) && !billType.equals(BillType.AgentCreditNoteBill)
                && !billType.equals(BillType.CollectingCentreCreditNoteBill) && !billType.equals(BillType.CollectingCentreDebitNoteBill)) {
            if (errorCheck()) {
                return;
            }
            getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        }
        getCurrent().setCollectingCentre(getCurrent().getFromInstitution());
        getCurrent().setTotal(getCurrent().getNetTotal());

        saveBill(billType, billNumberSuffix);
        saveBillItem();
//        createPayment(current, getCurrent().getPaymentMethod());
        //for channel agencyHistory Update
//        createAgentHistory(getCurrent().getFromInstitution(), getCurrent().getNetTotal(), historyType, getCurrent());
        //for channel agencyHistory Update

        //Update Agent Max Credit Limit
        if ((getCurrent().getNetTotal() > (getCurrent().getFromInstitution().getMaxCreditLimit() - getCurrent().getFromInstitution().getStandardCreditLimit())) && (getCurrent().getFromInstitution().getMaxCreditLimit() != getCurrent().getFromInstitution().getStandardCreditLimit())) {
            collectingCentreApplicationController.updateCcBalance(
                    current.getFromInstitution(),
                    0,
                    0,
                    0,
                    0,
                    HistoryType.CollectingCentreBalanceUpdateBill,
                    getCurrent(),
                    "Agent Payment Allowed Credit Limit Reset");

            getCurrent().getFromInstitution().setAllowedCredit(getCurrent().getFromInstitution().getStandardCreditLimit());
            getInstitutionFacade().edit(getCurrent().getFromInstitution());
        }
        //Update Agent Max Credit Limit
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

    }

    @Deprecated
    public void addPaymentMethordValueToTotal(Bill b, PaymentMethod pm) {
        switch (pm) {
            case Card:
                b.setNetTotal(paymentMethodData.getCreditCard().getTotalValue());
                break;
            case Cheque:
                b.setNetTotal(paymentMethodData.getCheque().getTotalValue());
                break;
            case Cash:
                break;
            case ewallet:
                break;
            case Agent:
                break;
            case Credit:
                break;
            case PatientDeposit:
                break;
            case Slip:
                b.setNetTotal(paymentMethodData.getSlip().getTotalValue());
                break;
            case OnCall:
                break;
            case OnlineSettlement:
                break;
            case Staff:
                break;
            case YouOweMe:
                break;
            case MultiplePaymentMethods:
                break;
        }
    }

    private void saveBillItem() {
        for (BillItem tmp : getBillItems()) {
            tmp.setCreatedAt(new Date());
            tmp.setCreater(getSessionController().getLoggedUser());
            tmp.setBill(getCurrent());
            tmp.setNetValue(tmp.getNetValue());
            getBillItemFacade().create(tmp);
        }
    }

    public void recreateModel() {
        current = null;
        printPreview = false;
        currentBillItem = null;
        patientEncounter = null;
        paymentMethodData = null;
        billItems = null;
        comment = null;
        amount = 0.0;
        collectingCentre = null;
    }

    public void createAgentHistory(Institution ins, double transactionValue, HistoryType historyType, Bill bill) {
        AgentHistory agentHistory = new AgentHistory();
        agentHistory.setCreatedAt(new Date());
        agentHistory.setCreater(getSessionController().getLoggedUser());
        agentHistory.setBill(bill);
        agentHistory.setAgency(ins);
        agentHistory.setBalanceBeforeTransaction(ins.getBallance());
        agentHistory.setTransactionValue(transactionValue);
        agentHistory.setHistoryType(historyType);
        agentHistoryFacade.create(agentHistory);

        ins.setBallance(ins.getBallance() + transactionValue);
        getInstitutionFacade().edit(ins);

    }

    public String prepareNewBill() {
        recreateModel();
        return "";
    }

//    public List<Payment> createPayment(Bill bill, PaymentMethod pm) {
//        List<Payment> ps = new ArrayList<>();
//        if (bill.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
//            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
//                Payment p = new Payment();
//                p.setBill(bill);
//                p.setInstitution(getSessionController().getInstitution());
//                p.setDepartment(getSessionController().getDepartment());
//                p.setCreatedAt(new Date());
//                p.setCreater(getSessionController().getLoggedUser());
//                p.setPaymentMethod(cd.getPaymentMethod());
//
//                switch (cd.getPaymentMethod()) {
//                    case Card:
//                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
//                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
//                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
//                        break;
//                    case Cheque:
//                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
//                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
//                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
//                        break;
//                    case Cash:
//                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
//                        break;
//                    case ewallet:
//                        break;
//                    case Agent:
//                        break;
//                    case Credit:
//                        break;
//                    case PatientDeposit:
//                        break;
//                    case Slip:
//                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
//                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
//                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
//                        break;
//                    case OnCall:
//                        break;
//                    case OnlineSettlement:
//                        break;
//                    case Staff:
//                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
//                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
//                            staffBean.updateStaffCredit(cd.getPaymentMethodData().getStaffCredit().getToStaff(), cd.getPaymentMethodData().getStaffCredit().getTotalValue());
//                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
//                        }
//                        break;
//                    case YouOweMe:
//                        break;
//                    case MultiplePaymentMethods:
//                        break;
//                }
//
//                paymentFacade.create(p);
//                ps.add(p);
//            }
//        } else {
//            Payment p = new Payment();
//            p.setBill(bill);
//            p.setInstitution(getSessionController().getInstitution());
//            p.setDepartment(sessionController.getDepartment());
//            p.setCreatedAt(new Date());
//            p.setCreater(getSessionController().getLoggedUser());
//            p.setPaymentMethod(pm);
//
//            switch (pm) {
//                case Card:
//                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
//                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
//                    break;
//                case Cheque:
//                    p.setChequeDate(paymentMethodData.getCheque().getDate());
//                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
//                    break;
//                case Cash:
//                    break;
//                case ewallet:
//                    break;
//                case Agent:
//                    break;
//                case Credit:
//                    break;
//                case PatientDeposit:
//                    break;
//                case Slip:
//                    p.setBank(paymentMethodData.getSlip().getInstitution());
//                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
//                case OnCall:
//                    break;
//                case OnlineSettlement:
//                    break;
//                case Staff:
//                    break;
//                case YouOweMe:
//                    break;
//                case MultiplePaymentMethods:
//                    break;
//            }
//
//            p.setPaidValue(p.getBill().getNetTotal());
//            paymentFacade.create(p);
//            cashBookEntryController.writeCashBookEntryAtPaymentCreation(p);
//            ps.add(p);
//        }
//        return ps;
//    }
    public Bill getCurrent() {
        if (current == null) {
            current = new BilledBill();
        }
        return current;
    }

    public void setCurrent(Bill current) {
        this.current = current;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
        }
        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<BillItem>();
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public AgentHistoryFacade getAgentHistoryFacade() {
        return agentHistoryFacade;
    }

    public void setAgentHistoryFacade(AgentHistoryFacade agentHistoryFacade) {
        this.agentHistoryFacade = agentHistoryFacade;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public BillNumberGenerator getBillNumberGenerator() {
        return billNumberGenerator;
    }

    public void setBillNumberGenerator(BillNumberGenerator billNumberGenerator) {
        this.billNumberGenerator = billNumberGenerator;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

}
