/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.HistoryType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.PaymentType;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.EjbApplication;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillEntry;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BilledBillFacade;
import com.divudi.core.facade.CancelledBillFacade;
import com.divudi.core.facade.DrawerFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.RefundBillFacade;
import com.divudi.service.BillService;
import com.divudi.service.PaymentService;
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
 * @author Buddhika
 */
@Named
@SessionScoped
public class AgentPaymentReceiveSearchController implements Serializable {

    private boolean printPreview = false;
    @EJB
    private BillFeeFacade billFeeFacade;

    @EJB
    private BillService billService;

    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private CancelledBillFacade cancelledBillFacade;
    @EJB
    private BillItemFacade billItemFacede;
    @EJB
    private BilledBillFacade billedBillFacade;
    @EJB
    private BillComponentFacade billCommponentFacade;
    @EJB
    private RefundBillFacade refundBillFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private DrawerFacade drawerFacade;
    @EJB
    private PaymentService paymentService;
    @EJB
    private BillFacade billFacade;
    @EJB
    private EjbApplication ejbApplication;
    @EJB
    private PaymentFacade paymentFacade;

    @Inject
    private BillBeanController billBean;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private AgentAndCcPaymentController agentAndCcPaymentController;
    @Inject
    private AgentAndCcApplicationController agentAndCcApplicationController;
    @Inject
    private DrawerController drawerController;

    private BilledBill bill;
    private List<BillEntry> billEntrys;
    private List<BillItem> billItems;
    private List<BillComponent> billComponents;
    private List<BillFee> billFees;
    private PaymentMethod paymentMethod;
    private PaymentMethodData paymentMethodData;

    private List<BillItem> tempbillItems;
    private String comment;
    private WebUser user;
    private CancelledBill cancelledBill;

    private boolean agencyDepositCanellationStarted = false;

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        // recreateModel();
        this.user = user;
        recreateModel();
    }

    public EjbApplication getEjbApplication() {
        return ejbApplication;
    }

    public void setEjbApplication(EjbApplication ejbApplication) {
        this.ejbApplication = ejbApplication;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BilledBillFacade getBilledBillFacade() {
        return billedBillFacade;
    }

    public void setBilledBillFacade(BilledBillFacade billedBillFacade) {
        this.billedBillFacade = billedBillFacade;
    }

    public CancelledBillFacade getCancelledBillFacade() {
        return cancelledBillFacade;
    }

    public void setCancelledBillFacade(CancelledBillFacade cancelledBillFacade) {
        this.cancelledBillFacade = cancelledBillFacade;
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void recreateModel() {

        billFees = null;
        billComponents = null;
        billItems = null;
        printPreview = false;
        tempbillItems = null;
        comment = null;
    }

//    private void cancelBillComponents(CancelledBill can, BillItem bt) {
//        for (BillComponent nB : getBillComponents()) {
//            BillComponent bC = new BillComponent();
//            bC.setCatId(nB.getCatId());
//            bC.setDeptId(nB.getDeptId());
//            bC.setInsId(nB.getInsId());
//            bC.setDepartment(nB.getDepartment());
//            bC.setDeptId(nB.getDeptId());
//            bC.setInstitution(nB.getInstitution());
//            bC.setItem(nB.getItem());
//            bC.setName(nB.getName());
//            bC.setPackege(nB.getPackege());
//            bC.setSpeciality(nB.getSpeciality());
//            bC.setStaff(nB.getStaff());
//
//            bC.setBill(can);
//            bC.setBillItem(bt);
//            bC.setCreatedAt(new Date());
//            bC.setCreater(getSessionController().getLoggedUser());
//            getBillCommponentFacade().create(bC);
//        }
//
//    }
    @Deprecated //Use the overloaded method with bill as a parameter
    private boolean checkPaid() {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.bill.id=" + getBill().getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private boolean checkPaid(Bill originalBill) {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.bill.id=" + originalBill.getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    @Deprecated //Use the overloaded method with bill as a param
    private CancelledBill createCancelBill(BillType billType, BillNumberSuffix billNumberSuffix) {
        CancelledBill cb = new CancelledBill();
        if (getBill() != null) {
            cb.copy(getBill());
            cb.invertAndAssignValuesFromOtherBill(getBill());

            cb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), billType, BillClassType.CancelledBill, billNumberSuffix));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.CancelledBill, billNumberSuffix));

        }

        cb.setBilledBill(getBill());
        cb.setPaymentMethod(paymentMethod);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

        return cb;
    }

    private CancelledBill createCancelBill(BillType billType, BillNumberSuffix billNumberSuffix, Bill originalBill) {
        CancelledBill cb = new CancelledBill();
        if (originalBill != null) {
            cb.copy(originalBill);
            cb.invertAndAssignValuesFromOtherBill(originalBill);
            String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.AGENCY_CREDIT_NOTE);
            cb.setDeptId(deptId);
            cb.setInsId(deptId);
        }
        cb.setBilledBill(getBill());
        cb.setPaymentMethod(paymentMethod);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

        return cb;
    }

    @Deprecated //Use the overloaded method with bill as a param
    private boolean errorCheck() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (checkPaid()) {
            JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
            return true;
        }

        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    public String navigateToCancelAgentBillView() {
        agencyDepositCanellationStarted = false;
        return "agency_deposit_bill_cancellation?faces-redirect=true";
    }

    private boolean errorCheck(Bill origianlBill) {
        if (origianlBill.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (origianlBill.isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method.");
            return true;
        }
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            JsfUtil.addErrorMessage("Multiple Payment Methods are NOT allowed.");
            return true;
        }
        if (paymentMethod.getPaymentType() == PaymentType.CREDIT) {
            JsfUtil.addErrorMessage("Credit Payment Methods are NOT allowed.");
            return true;
        }

//        if (checkPaid(origianlBill)) {
//            JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
//            return true;
//        }
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void cancelAgencyDepositBill() {
        System.out.println("cancelAgencyDepositBill");
        System.out.println("agencyDepositCanellationStarted = " + agencyDepositCanellationStarted);
        if (agencyDepositCanellationStarted) {
            JsfUtil.addErrorMessage("Already Started");
            printPreview = false;
            return;
        }
        agencyDepositCanellationStarted = true;
        System.out.println("getBill() = " + getBill());
        if (getBill() == null) {
            JsfUtil.addErrorMessage("No Bill to Calcel");
            printPreview = false;
            agencyDepositCanellationStarted = false;
            return;
        }
        Bill origianlBil = billBean.fetchBillBypassingCache(getBill().getId());
        System.out.println("origianlBil = " + origianlBil);
        if (origianlBil == null) {
            JsfUtil.addErrorMessage("No SUch Bill");
            printPreview = false;
            agencyDepositCanellationStarted = false;
            return;
        }
        boolean error = errorCheck(origianlBil);
        System.out.println("error = " + error);
        if (error) {
            JsfUtil.addErrorMessage("Error");
            printPreview = false;
            agencyDepositCanellationStarted = false;
            return;
        }
        System.out.println("origianlBil.isCancelled() = " + origianlBil.isCancelled());
        if (origianlBil.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            printPreview = false;
            agencyDepositCanellationStarted = false;
            return;
        }

        if (paymentMethod == PaymentMethod.Cash) {
            Drawer userDrawer = drawerFacade.find(sessionController.getLoggedUserDrawer().getId());
            if (userDrawer.getCashInHandValue() < bill.getNetTotal()) {
                if (configOptionApplicationController.getBooleanValueByKey("Enable Drawer Manegment", true)) {
                    JsfUtil.addErrorMessage("Drawer cash in hand value is not enough to cancel the bill");
                    printPreview = false;
                    agencyDepositCanellationStarted = false;
                    return;
                }
            }
        }

//        cancelBill(BillType.CollectingCentrePaymentReceiveBill, BillNumberSuffix.CCCAN, HistoryType.CollectingCentreDepositCancel, BillTypeAtomic.CC_PAYMENT_CANCELLATION_BILL);
        CancelledBill newlyCreatedCancelBill = generateCancelBillForCcDepositBill(origianlBil);

        cancelBillItems(newlyCreatedCancelBill, origianlBil);

        origianlBil.setCancelled(true);
        origianlBil.setCancelledBill(newlyCreatedCancelBill);
        getBillFacade().editAndCommit(origianlBil);

        Payment payments = createPayment(newlyCreatedCancelBill, paymentMethod);
        //drawerController.updateDrawerForOuts(payments);
        agentAndCcApplicationController.updateCcBalance(
                newlyCreatedCancelBill.getFromInstitution(),
                0,
                newlyCreatedCancelBill.getNetTotal(),
                0,
                newlyCreatedCancelBill.getNetTotal(),
                HistoryType.CollectingCentreDepositCancel,
                newlyCreatedCancelBill, comment);
        printPreview = true;
        agencyDepositCanellationStarted = false;
        JsfUtil.addSuccessMessage("Cancelled");
    }

    public void cancelCollectingCentreDepositBill() {
        if (agencyDepositCanellationStarted) {
            JsfUtil.addErrorMessage("Cancellation already Started");
            printPreview = false;
            return;
        }
        agencyDepositCanellationStarted = true;
        if (getBill() == null) {
            JsfUtil.addErrorMessage("No Bill to Cancel");
            printPreview = false;
            agencyDepositCanellationStarted = false;
            return;
        }
        if (getBill().getId() == null) {
            JsfUtil.addErrorMessage("Not a Saved Bill. Can NOT Cancel");
            printPreview = false;
            agencyDepositCanellationStarted = false;
            return;
        }
        Bill origianlBil = billBean.fetchBillBypassingCache(getBill().getId());
        System.out.println("origianlBil = " + origianlBil);
        if (origianlBil == null) {
            JsfUtil.addErrorMessage("No SUch Bill");
            printPreview = false;
            agencyDepositCanellationStarted = false;
            return;
        }
        boolean error = errorCheck(origianlBil);
        System.out.println("error = " + error);
        if (error) {
            JsfUtil.addErrorMessage("Error");
            printPreview = false;
            agencyDepositCanellationStarted = false;
            return;
        }
        cancelledBill = generateCancelBillForCcDepositBill(origianlBil);
        cancelBillItems(cancelledBill, origianlBil);

        origianlBil.setCancelled(true);
        origianlBil.setCancelledBill(cancelledBill);
        getBillFacade().editAndCommit(origianlBil);
        List<Payment> cancellationPayments = paymentService.createPayment(cancelledBill, getPaymentMethodData());
        agentAndCcApplicationController.updateCcBalance(
                cancelledBill.getFromInstitution(),
                0,
                cancelledBill.getNetTotal(),
                0,
                cancelledBill.getNetTotal(),
                HistoryType.CollectingCentreDepositCancel,
                cancelledBill, comment);
        printPreview = true;
        agencyDepositCanellationStarted = false;
        JsfUtil.addSuccessMessage("Cancelled");
    }

    public void channellAgencyCancelBill(Bill originalBill) {
        cancelBill(BillType.AgentPaymentReceiveBill, BillNumberSuffix.AGNCAN, HistoryType.ChannelDepositCancel, BillTypeAtomic.AGENCY_PAYMENT_CANCELLATION, originalBill);
    }

    public void channelCreditNoteCancelBill(Bill originalBill) {
        cancelBill(BillType.AgentCreditNoteBill, BillNumberSuffix.AGNCNCAN, HistoryType.ChannelCreditNoteCancel, BillTypeAtomic.AGENCY_CREDIT_NOTE_CANCELLATION, originalBill);
    }

    public void channelDebitNoteCancelBill(Bill originalBill) {
        cancelBill(BillType.AgentDebitNoteBill, BillNumberSuffix.AGNDNCAN, HistoryType.ChannelDebitNoteCancel, BillTypeAtomic.AGENCY_DEBIT_NOTE_CANCELLATION, originalBill);
    }

    public void collectingCenterCreditNoteCancelBill(Bill originalBill) {
        cancelBill(BillType.CollectingCentreCreditNoteBill, BillNumberSuffix.CCCNCAN, HistoryType.CollectingCentreCreditNoteCancel, BillTypeAtomic.CC_CREDIT_NOTE_CANCELLATION, originalBill);
        JsfUtil.addSuccessMessage("Cancelled Successfully");
    }

    public void collectingCenterDebitNoteCancelBill(Bill originalBill) {
        cancelBill(BillType.CollectingCentreDebitNoteBill, BillNumberSuffix.CCDNCAN, HistoryType.CollectingCentreDebitNoteCancel, BillTypeAtomic.CC_DEBIT_NOTE_CANCELLATION, originalBill);
    }

    public CancelledBill generateCancelBillForCcDepositBill(Bill originalCcDepositBill) {
        if (originalCcDepositBill == null) {
            return null;
        }
        CancelledBill cb = new CancelledBill();
        cb.copy(originalCcDepositBill);
        cb.invertAndAssignValuesFromOtherBill(originalCcDepositBill);
        String deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.CC_PAYMENT_CANCELLATION_BILL);
        cb.setDeptId(deptId);
        cb.setInsId(deptId);
        cb.setBillType(BillType.CollectingCentrePaymentMadeBill);
        cb.setBillTypeAtomic(BillTypeAtomic.CC_PAYMENT_CANCELLATION_BILL);
        cb.setBilledBill(getBill());
        cb.setPaymentMethod(paymentMethod);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);
        billFacade.create(cb);
        return cb;
//
//        cb.setBillTypeAtomic(billTypeAtomic);
//        getCancelledBillFacade().create(cb);
//        cancelBillItems(cb);
//        getBill().setCancelled(true);
//        getBill().setCancelledBill(cb);
//        getBilledBillFacade().edit(getBill());
//        JsfUtil.addSuccessMessage("Cancelled");
//        createPayment(cb, paymentMethod);
//
//        //for channel agencyHistory Update
//        //getAgentPaymentRecieveBillController().createAgentHistory(cb.getFromInstitution(), cb.getNetTotal(), historyType, cb);
//        agentAndCcApplicationController.updateCcBalance(
//                cb.getFromInstitution(),
//                0,
//                cb.getNetTotal(),
//                0,
//                cb.getNetTotal(),
//                historyType,
//                cb, comment);
//        //for channel agencyHistory Update
//
//        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
//        getSessionController().setLoggedUser(wb);
//        printPreview = true;

    }

    public void cancelBill(BillType billType, BillNumberSuffix billNumberSuffix, HistoryType historyType, BillTypeAtomic billTypeAtomic, Bill originalBill) {
        if (originalBill != null && originalBill.getId() != null && originalBill.getId() != 0) {
            if (errorCheck(originalBill)) {
                return;
            }

            CancelledBill cb = createCancelBill(billType, billNumberSuffix);

            cb.setBillTypeAtomic(billTypeAtomic);
            getCancelledBillFacade().create(cb);
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBilledBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");
            createPayment(cb, paymentMethod);

            //for channel agencyHistory Update
            //getAgentPaymentRecieveBillController().createAgentHistory(cb.getFromInstitution(), cb.getNetTotal(), historyType, cb);
            agentAndCcApplicationController.updateCcBalance(
                    cb.getFromInstitution(),
                    0,
                    cb.getNetTotal(),
                    0,
                    cb.getNetTotal(),
                    historyType,
                    cb, comment);
            //for channel agencyHistory Update

            WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }

    }

    List<Bill> billsToApproveCancellation;
    List<Bill> billsApproving;
    private Bill billForCancel;

//    public void approveCancellation() {
//
//        if (billsApproving == null) {
//            JsfUtil.addErrorMessage("Select Bill to Approve Cancell");
//            return;
//        }
//        for (Bill b : billsApproving) {
//
//            b.setApproveUser(getSessionController().getCurrent());
//            b.setApproveAt(Calendar.getInstance().getTime());
//            getBillFacade().create(b);
//
//            cancelBillItems(b);
//            b.getBilledBill().setCancelled(true);
//            b.getBilledBill().setCancelledBill(b);
//
//            getBilledBillFacade().edit(getBill());
//
//            ejbApplication.getBillsToCancel().remove(b);
//
//            JsfUtil.addSuccessMessage("Cancelled");
//
//        }
//
//        billForCancel = null;
//    }
    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, pm);
        return p;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);

        p.setPaidValue(p.getBill().getNetTotal());

        if (p.getId() == null) {
            paymentFacade.create(p);
        }

    }

    public List<Bill> getBillsToApproveCancellation() {
        //////// // System.out.println("1");
        billsToApproveCancellation = ejbApplication.getBillsToCancel();
        return billsToApproveCancellation;
    }

    public void setBillsToApproveCancellation(List<Bill> billsToApproveCancellation) {
        this.billsToApproveCancellation = billsToApproveCancellation;
    }

    public List<Bill> getBillsApproving() {
        return billsApproving;
    }

    public void setBillsApproving(List<Bill> billsApproving) {
        this.billsApproving = billsApproving;
    }

    private void cancelBillItems(CancelledBill cancelBill, Bill originalBill) {
        List<BillItem> bis = billBean.fetchBillItems(originalBill);
        if (bis == null) {
            return;
        }
        for (BillItem originalBillItem : bis) {
            BillItem newBillItemForCancelBill = new BillItem();
            newBillItemForCancelBill.setBill(cancelBill);
            newBillItemForCancelBill.copy(originalBillItem);
            newBillItemForCancelBill.invertValue();
//            newBillItemForCancelBill.setNetValue(-originalBillItem.getNetValue());
            newBillItemForCancelBill.setReferenceBill(originalBillItem.getReferenceBill());
            newBillItemForCancelBill.setCatId(originalBillItem.getCatId());
            newBillItemForCancelBill.setDeptId(originalBillItem.getDeptId());
            newBillItemForCancelBill.setInsId(originalBillItem.getInsId());
            newBillItemForCancelBill.setCreatedAt(new Date());
            newBillItemForCancelBill.setCreater(getSessionController().getLoggedUser());
            newBillItemForCancelBill.setReferanceBillItem(originalBillItem);
            getBillItemFacede().create(newBillItemForCancelBill);

        }
    }

    @Deprecated
    private void cancelBillItems(Bill can) {
        for (BillItem nB : getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.setNetValue(-nB.getNetValue());
            b.setReferenceBill(nB.getReferenceBill());
            b.setCatId(nB.getCatId());
            b.setDeptId(nB.getDeptId());
            b.setInsId(nB.getInsId());
            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            getBillItemFacede().create(b);

        }
    }

    public BilledBill getBill() {
        //recreateModel();
        return bill;
    }

    public void setBill(BilledBill bill) {
        recreateModel();
        this.bill = bill;
        paymentMethod = bill.getPaymentMethod();
    }

    public List<BillEntry> getBillEntrys() {
        return billEntrys;
    }

    public void setBillEntrys(List<BillEntry> billEntrys) {
        this.billEntrys = billEntrys;
    }

    public List<BillItem> getBillItems() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billItems = getBillItemFacede().findByJpql(sql);
            //////// // System.out.println("sql for bill item search is " + sql);
            //////// // System.out.println("results for bill item search is " + billItems);
            if (billItems == null) {
                billItems = new ArrayList<BillItem>();
            }
        }

        return billItems;
    }

    public List<BillComponent> getBillComponents() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billComponents = getBillCommponentFacade().findByJpql(sql);
            if (billComponents == null) {
                billComponents = new ArrayList<BillComponent>();
            }
        }
        return billComponents;
    }

    public List<BillFee> getBillFees() {
        if (getBill() != null) {
            if (billFees == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
                if (billFees == null) {
                    billFees = new ArrayList<BillFee>();
                }
            }
        }

        return billFees;
    }

    public List<BillFee> getPayingBillFees() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billFees = getBillFeeFacade().findByJpql(sql);
            if (billFees == null) {
                billFees = new ArrayList<BillFee>();
            }

        }

        return billFees;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public void setBillComponents(List<BillComponent> billComponents) {
        this.billComponents = billComponents;
    }

    /**
     * Creates a new instance of BillSearch
     */
    public AgentPaymentReceiveSearchController() {
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillItemFacade getBillItemFacede() {
        return billItemFacede;
    }

    public void setBillItemFacede(BillItemFacade billItemFacede) {
        this.billItemFacede = billItemFacede;
    }

    public BillComponentFacade getBillCommponentFacade() {
        return billCommponentFacade;
    }

    public void setBillCommponentFacade(BillComponentFacade billCommponentFacade) {
        this.billCommponentFacade = billCommponentFacade;
    }

    public RefundBillFacade getRefundBillFacade() {
        return refundBillFacade;
    }

    public void setRefundBillFacade(RefundBillFacade refundBillFacade) {
        this.refundBillFacade = refundBillFacade;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public Bill getBillForCancel() {
        return billForCancel;
    }

    public void setBillForCancel(CancelledBill billForCancel) {
        this.billForCancel = billForCancel;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<BillItem> getTempbillItems() {
        if (tempbillItems == null) {
            tempbillItems = new ArrayList<BillItem>();
        }
        return tempbillItems;
    }

    public void setTempbillItems(List<BillItem> tempbillItems) {
        this.tempbillItems = tempbillItems;
    }

    public void resetLists() {
        recreateModel();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public AgentAndCcPaymentController getAgentAndCcPaymentController() {
        return agentAndCcPaymentController;
    }

    public void setAgentAndCcPaymentController(AgentAndCcPaymentController agentAndCcPaymentController) {
        this.agentAndCcPaymentController = agentAndCcPaymentController;
    }

    public CancelledBill getCancelledBill() {
        return cancelledBill;
    }

    public void setCancelledBill(CancelledBill cancelledBill) {
        this.cancelledBill = cancelledBill;
    }

}
