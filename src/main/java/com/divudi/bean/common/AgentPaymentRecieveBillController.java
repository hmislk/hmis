/*
 * To change this currentlate, choose Tools | currentlates
 * (94) 71 5812399 open the currentlate in the editor.
 */
package com.divudi.bean.common;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.HistoryType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.WebUser;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.InstitutionFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
public class AgentPaymentRecieveBillController implements Serializable {

    private Bill current;
    private boolean printPreview = false;
    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    private SessionController sessionController;
    @Inject
    InstitutionController institutionController;
    @Inject
    CommonController commonController;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    AgentHistoryFacade agentHistoryFacade;
    
    @Inject
    private PaymentSchemeController paymentSchemeController;

    private PatientEncounter patientEncounter;
    private BillItem currentBillItem;
    private List<BillItem> billItems;
    private int index;
    private PaymentMethodData paymentMethodData;
    String comment;
    double amount;

    public void addToBill() {
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

    public AgentPaymentRecieveBillController() {
    }

    

    private boolean errorCheck() {
        if (getCurrent().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Agency");
            return true;
        }

        if (getCurrent().getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().errorCheckPaymentMethod(getCurrent().getPaymentMethod(), paymentMethodData)) {
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
        getCurrent().setDeptId(getBillNumberBean().departmentBillNumberGenerator(sessionController.getDepartment(), billType, BillClassType.BilledBill, billNumberSuffix));
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

    @Inject
    private BillBeanController billBean;
    @EJB
    CashTransactionBean cashTransactionBean;

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

    public void collectingCentrePaymentRecieveSettleBill() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        settleBill(BillType.CollectingCentrePaymentReceiveBill, HistoryType.CollectingCentreDeposit, HistoryType.CollectingCentreBalanceUpdateBill, BillNumberSuffix.CCPAY);

        
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
        creditDebitNote(BillType.CollectingCentreCreditNoteBill, HistoryType.CollectingCentreCreditNote, HistoryType.CollectingCentreBalanceUpdateBill, BillNumberSuffix.CCCN);

    }

    public void collectingCenterDebitNoteSettleBill() {
        if (errorCheckCreditNoteDebitNote()) {
            return;
        }
        getCurrent().setPaymentMethod(PaymentMethod.Slip);
        getCurrent().setNetTotal(0 - getAmount());
        creditDebitNote(BillType.CollectingCentreDebitNoteBill, HistoryType.CollectingCentreDebitNote, HistoryType.CollectingCentreBalanceUpdateBill, BillNumberSuffix.CCDN);

    }

    private void creditDebitNote(BillType billType, HistoryType historyType, HistoryType updatHistoryType, BillNumberSuffix billNumberSuffix) {

        settleBill(billType, historyType, updatHistoryType, billNumberSuffix);

    }

    public void settleBill(BillType billType, HistoryType historyType, HistoryType updatHistoryType, BillNumberSuffix billNumberSuffix) {
        addToBill();
        if (!billType.equals(BillType.AgentDebitNoteBill) && !billType.equals(BillType.AgentCreditNoteBill)
                && !billType.equals(BillType.CollectingCentreCreditNoteBill) && !billType.equals(BillType.CollectingCentreDebitNoteBill)) {
            if (errorCheck()) {
                return;
            }
            getBillBean().setPaymentMethodData(getCurrent(), getCurrent().getPaymentMethod(), getPaymentMethodData());
        }

        getCurrent().setTotal(getCurrent().getNetTotal());

        saveBill(billType, billNumberSuffix);
        saveBillItem();
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
        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(getCurrent(), getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);

        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;

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
    }

    public void createAgentHistory(Institution ins, double transactionValue, HistoryType historyType, Bill bill) {
        AgentHistory agentHistory = new AgentHistory();
        agentHistory.setCreatedAt(new Date());
        agentHistory.setCreater(getSessionController().getLoggedUser());
        agentHistory.setBill(bill);
        agentHistory.setBeforeBallance(ins.getBallance());
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

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

}
