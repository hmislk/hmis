/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.CreditBean;
import com.divudi.ejb.EjbApplication;
import com.divudi.core.entity.*;
import com.divudi.service.PaymentService;
import com.divudi.service.StaffService;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BilledBillFacade;
import com.divudi.core.facade.CancelledBillFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.RefundBillFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class CreditCompanyBillSearch implements Serializable {

    private boolean printPreview = false;
    @EJB
    BillFeeFacade billFeeFacade;
    String txtSearch;
    BilledBill bill;
    List<BillEntry> billEntrys;
    List<BillItem> billItems;
    List<BillComponent> billComponents;
    List<BillFee> billFees;
    PaymentMethod paymentMethod;
    PaymentScheme paymentScheme;
    List<Bill> bills;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    CancelledBillFacade cancelledBillFacade;
    @EJB
    private BillItemFacade billItemFacede;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    BilledBillFacade billedBillFacade;
    @EJB
    private BillComponentFacade billCommponentFacade;
    @EJB
    StaffService staffBean;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    PaymentService paymentService;
    @Inject
    CashBookEntryController cashBookEntryController;
    @EJB
    private RefundBillFacade refundBillFacade;
    @Inject
    SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    PatientDepositController patientDepositController;
    @EJB
    EjbApplication ejbApplication;
    private List<BillItem> tempbillItems;
    @Temporal(TemporalType.TIME)
    private Date fromDate;
    @Temporal(TemporalType.TIME)
    private Date toDate;
    private String comment;
    WebUser user;
    private PaymentMethodData paymentMethodData;

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

    public List<Bill> getUserBillsOwn() {
        List<Bill> userBills;
        if (getUser() == null) {
            userBills = new ArrayList<Bill>();
            //////// // System.out.println("user is null");
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), getSessionController().getInstitution(), BillType.OpdBill);
            //////// // System.out.println("user ok");
        }
        if (userBills == null) {
            userBills = new ArrayList<Bill>();
        }
        return userBills;
    }

    public List<Bill> getBillsOwn() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.CashRecieveBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.CashRecieveBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public String navigateToBillPreview(Bill b) {
        String page;
        switch (b.getBillTypeAtomic()) {
            case INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED:
                page = "/credit/inpatient_credit_company_bill_reprint?faces-redirect=true";
                break;
            case OPD_CREDIT_COMPANY_PAYMENT_RECEIVED:
                page = "/credit/credit_company_bill_reprint?faces-redirect=true";
                break;
            default:
                page = "credit_company_bill_reprint?faces-redirect=true";
                break;
        }
        return page;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
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

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void recreateModel() {

        billFees = null;
//        billFees
        billComponents = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        comment = null;
    }

    private void cancelBillComponents(CancelledBill can, BillItem bt) {
        for (BillComponent nB : getBillComponents()) {
            BillComponent bC = new BillComponent();
            bC.setCatId(nB.getCatId());
            bC.setDeptId(nB.getDeptId());
            bC.setInsId(nB.getInsId());
            bC.setDepartment(nB.getDepartment());
            bC.setDeptId(nB.getDeptId());
            bC.setInstitution(nB.getInstitution());
            bC.setItem(nB.getItem());
            bC.setName(nB.getName());
            bC.setPackege(nB.getPackege());
            bC.setSpeciality(nB.getSpeciality());
            bC.setStaff(nB.getStaff());

            bC.setBill(can);
            bC.setBillItem(bt);
            bC.setCreatedAt(new Date());
            bC.setCreater(getSessionController().getLoggedUser());
            getBillCommponentFacade().create(bC);
        }

    }

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

    private CancelledBill createCancelBill(BillNumberSuffix billSuffix) {
        CancelledBill cb = new CancelledBill();
        cb.copy(getBill());
        cb.invertAndAssignValuesFromOtherBill(getBill());

        cb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.CashRecieveBill, BillClassType.CancelledBill, billSuffix));
        cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.CashRecieveBill, BillClassType.CancelledBill, billSuffix));

        cb.setBilledBill(getBill());
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());

        cb.setPaymentScheme(paymentScheme);
        cb.setPaymentMethod(paymentMethod);
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        cb.setComments(comment);

        return cb;
    }

    private boolean errorCheck() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        // Check if this is a cancellation bill (bill created during cancellation process)
        if (isBillACancellationBill(getBill())) {
            JsfUtil.addErrorMessage("This is a Cancellation Bill and cannot be cancelled again.");
            return true;
        }

        if (checkPaid()) {
            JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
            return true;
        }
        if (getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment scheme.");
            return true;
        }
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    /**
     * Checks if the given bill is a cancellation bill (bill created during cancellation process).
     * Cancellation bills have specific BillTypeAtomic values and should not be allowed to be cancelled again.
     *
     * @param bill The bill to check
     * @return true if the bill is a cancellation bill, false otherwise
     */
    private boolean isBillACancellationBill(Bill bill) {
        if (bill == null || bill.getBillTypeAtomic() == null) {
            return false;
        }

        BillTypeAtomic billType = bill.getBillTypeAtomic();
        return billType == BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_CANCELLATION
            || billType == BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION
            || billType == BillTypeAtomic.PHARMACY_CREDIT_COMPANY_PAYMENT_CANCELLATION;
    }

    /**
     * Public method to check if the current bill is a cancellation bill.
     * Used by the UI to disable cancel button and show appropriate messages.
     *
     * @return true if the current bill is a cancellation bill, false otherwise
     */
    public boolean isBillACancellationBill() {
        return isBillACancellationBill(getBill());
    }

//    if (getTabId().equals("tabBht")) {
//            savePayments();
//        }
//    private void savePayments() {
//        for (BillItem b : getBillItems()) {
//            Bill bil = saveBhtPaymentBill(b);
//            saveBhtBillItem(bil);
//        }
//    }
//      private Bill saveBhtPaymentBill(BillItem b) {
//        Bill tmp = new BilledBill();
//        tmp.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getSessionController().getDepartment()));
//        tmp.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment()));
//
//        tmp.setBillType(BillType.InwardPaymentBill);
//        tmp.setPatientEncounter(b.getPatientEncounter());
//        tmp.setPatient(b.getPatientEncounter().getPatient());
//        tmp.setPaymentScheme(getCurrent().getPaymentScheme());
//        tmp.setNetTotal(b.getNetValue());
//        tmp.setCreatedAt(new Date());
//        tmp.setCreater(getSessionController().getLoggedUser());
//        getBillFacade().create(tmp);
//
//        return tmp;
//    }
//
//       private void saveBhtBillItem(Bill b) {
//        BillItem temBi = new BillItem();
//        temBi.setBill(b);
//        temBi.setNetValue(b.getNetTotal());
//        temBi.setCreatedAt(new Date());
//        temBi.setCreater(getSessionController().getLoggedUser());
//        getBillItemFacade().create(temBi);
//    }
    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void cancelBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (errorCheck()) {
                return;
            }

            CancelledBill cb = createCancelBill(BillNumberSuffix.CRDCAN);

            //Copy & paste
            //  if (webUserController.hasPrivilege("LabBillCancelling")) {
            if (true) {
                cb.setBillTypeAtomic(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_CANCELLATION);
                getCancelledBillFacade().create(cb);
                cancelBillItems(cb);
                getBill().setCancelled(true);
                getBill().setCancelledBill(cb);
                getBilledBillFacade().edit(getBill());
                JsfUtil.addSuccessMessage("Cancelled");
                WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
                getSessionController().setLoggedUser(wb);
                paymentService.createPaymentsForCancelling(cb);
//                createPayment(cb, paymentMethod);
                printPreview = true;
            } else {
                getEjbApplication().getBillsToCancel().add(cb);
                JsfUtil.addSuccessMessage("Awaiting Cancellation");
            }

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }

    }

    public void cancelCreditCompanyPaymentBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (errorCheck()) {
                return;
            }

            CancelledBill cb = createCancelBill(BillNumberSuffix.INWCCPAYCAN);

            //Copy & paste
            //  if (webUserController.hasPrivilege("LabBillCancelling")) {
            if (true) {
                cb.setBillTypeAtomic(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION);
                getCancelledBillFacade().create(cb);
                cancelBillItems(cb);
                getBill().setCancelled(true);
                getBill().setCancelledBill(cb);
                getBilledBillFacade().edit(getBill());
                JsfUtil.addSuccessMessage("Cancelled");
                WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
                for(BillItem cancellingBillItem : cb.getBillItems()){
                    getBillBean().updateInwardDipositList(cancellingBillItem.getPatientEncounter(), cb);
                }
                getSessionController().setLoggedUser(wb);
                paymentService.createPayment(cb, getPaymentMethodData());
                //createPayment(cb, paymentMethod);
                printPreview = true;
                paymentMethodData = null;
            } else {
                getEjbApplication().getBillsToCancel().add(cb);
                JsfUtil.addSuccessMessage("Awaiting Cancellation");
            }

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }

    }

    /**
     * @deprecated This method will be removed in the next iteration.
     * Pharmacy credit company payment cancellations are now handled through the unified OPD credit
     * cancellation methods, as pharmacy credit bills are being consolidated with OPD credit bills.
     * The separate Pharmacy Credit Settle bill type (BillTypeAtomic.PHARMACY_CREDIT_COMPANY_PAYMENT_RECEIVED)
     * and its cancellation type (BillTypeAtomic.PHARMACY_CREDIT_COMPANY_PAYMENT_CANCELLATION) are being deprecated
     * in favor of the unified OPD Credit Settle bill type.
     */
    @Deprecated
    public void cancelPharmacyCreditCompanyPaymentBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (errorCheck()) {
                return;
            }

            CancelledBill cb = createCancelBill(BillNumberSuffix.PHACCPAYCAN);

            //Copy & paste
            //  if (webUserController.hasPrivilege("LabBillCancelling")) {
            if (true) {
                cb.setBillTypeAtomic(BillTypeAtomic.PHARMACY_CREDIT_COMPANY_PAYMENT_CANCELLATION);
                getCancelledBillFacade().create(cb);
                cancelBillItems(cb);
                getBill().setCancelled(true);
                getBill().setCancelledBill(cb);
                getBilledBillFacade().edit(getBill());
                JsfUtil.addSuccessMessage("Cancelled");
                WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
                getSessionController().setLoggedUser(wb);
                paymentService.createPayment(cb, getPaymentMethodData());
                printPreview = true;
                paymentMethodData = null;
            } else {
                getEjbApplication().getBillsToCancel().add(cb);
                JsfUtil.addSuccessMessage("Awaiting Cancellation");
            }

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }

    }

    public void listnerForPaymentMethodChange(Bill b) {
        if (getPaymentMethod() == PaymentMethod.PatientDeposit) {
            getPaymentMethodData().getPatient_deposit().setPatient(b.getPatientEncounter().getPatient());
            getPaymentMethodData().getPatient_deposit().setTotalValue(b.getTotal());
            com.divudi.core.entity.PatientDeposit pd = patientDepositController.checkDepositOfThePatient(b.getPatientEncounter().getPatient(), sessionController.getDepartment());
            if (pd != null && pd.getId() != null) {
                getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
            }
        } else if (getPaymentMethod() == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(b.getTotal());
            System.out.println("this = " + this);
        } else if (getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            getPaymentMethodData().getPatient_deposit().setPatient(b.getPatientEncounter().getPatient());
//            getPaymentMethodData().getPatient_deposit().setTotalValue(calculatRemainForMultiplePaymentTotal());
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(b.getPatientEncounter().getPatient(), sessionController.getDepartment());

            if (pd != null && pd.getId() != null) {
                System.out.println("pd = " + pd);
                boolean hasPatientDeposit = false;
                for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    System.out.println("cd = " + cd);
                    if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                        System.out.println("cd = " + cd);
                        hasPatientDeposit = true;
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(b.getPatientEncounter().getPatient());
                        cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

                    }
                }
            }

        }
    }

    List<Bill> billsToApproveCancellation;
    List<Bill> billsApproving;
    private Bill billForCancel;

    public List<Payment> createPayment(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        if (bill.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(bill);
                p.setInstitution(getSessionController().getInstitution());
                p.setDepartment(getSessionController().getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(getSessionController().getLoggedUser());
                p.setPaymentMethod(cd.getPaymentMethod());

                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
                        break;
                    case Cheque:
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
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
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                        break;
                    case OnCall:
                        break;
                    case OnlineSettlement:
                        break;
                    case Staff:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            staffBean.updateStaffCredit(cd.getPaymentMethodData().getStaffCredit().getToStaff(), cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                        }
                        break;
                    case YouOweMe:
                        break;
                    case MultiplePaymentMethods:
                        break;
                }

                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(sessionController.getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(pm);

            switch (pm) {
                case Card:
                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                    break;
                case Cheque:
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
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
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
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

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);
            cashBookEntryController.writeCashBookEntryAtPaymentCreation(p);
            ps.add(p);
        }
        return ps;
    }

    public void approveCancellation() {

        if (billsApproving == null) {
            JsfUtil.addErrorMessage("Select Bill to Approve Cancell");
            return;
        }
        for (Bill b : billsApproving) {

            b.setApproveUser(getSessionController().getCurrent());
            b.setApproveAt(Calendar.getInstance().getTime());
            getBillFacade().create(b);

            cancelBillItems(b);
            b.getBilledBill().setCancelled(true);
            b.getBilledBill().setCancelledBill(b);

            getBilledBillFacade().edit(getBill());

            ejbApplication.getBillsToCancel().remove(b);

            JsfUtil.addSuccessMessage("Cancelled");

        }

        billForCancel = null;
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

    private void cancelBillItems(Bill can) {
        for (BillItem nB : getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.copy(nB);
            b.invertValue(nB);

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            getBillItemFacede().create(b);

            if (b.getReferenceBill() != null) {
                updateReferenceBill(b);
            }

            if (b.getPatientEncounter() != null) {
                updateReferenceBht(b);
            }
        }
    }

    @EJB
    private CreditBean creditBean;

    private void updateReferenceBill(BillItem cancellationBillItem) {
        Bill referenceBill = cancellationBillItem.getReferenceBill();

        // The cancellation bill item has negative netValue (inverted from original)
        // The amount that was originally added during settlement is the absolute value
        double originalSettlementAmount = Math.abs(cancellationBillItem.getNetValue());

        // Reverse the settlement by subtracting the original amount from paidAmount
        double currentPaidAmount = referenceBill.getPaidAmount();
        referenceBill.setPaidAmount(currentPaidAmount - originalSettlementAmount);

        // Recalculate settled amounts (will exclude the cancelled settlement)
        double settledCreditValueByCompanies = getCreditBean().getSettledAmountByCompany(referenceBill);
        double settledCreditValueByPatient = getCreditBean().getSettledAmountByPatient(referenceBill);

        // Update the settled amount fields
        referenceBill.setSettledAmountByPatient(settledCreditValueByPatient);
        referenceBill.setSettledAmountBySponsor(settledCreditValueByCompanies);

        // CRITICAL FIX: Update balance field to stay synchronized with paid amount changes
        // Calculate the accurate current balance using the same formula used in settlement process
        double totalSettlement = settledCreditValueByCompanies + settledCreditValueByPatient;
        double netTotal = Math.abs(referenceBill.getNetTotal() + referenceBill.getVat());
        double refundAmount = Math.abs(getCreditBean().getRefundAmount(referenceBill));
        double currentBalance = netTotal - (totalSettlement + refundAmount);
        referenceBill.setBalance(Math.max(0, currentBalance)); // Ensure balance doesn't go negative

        getBillFacade().edit(referenceBill);

    }

    private void updateReferenceBht(BillItem cancellationBillItem) {
        PatientEncounter encounter = cancellationBillItem.getPatientEncounter();

        // The cancellation bill item has negative netValue (inverted from original)
        // The amount that was originally added during settlement is the absolute value
        double originalSettlementAmount = Math.abs(cancellationBillItem.getNetValue());

        // Reverse the settlement by subtracting the original amount from creditPaidAmount
        double currentPaidAmount = encounter.getCreditPaidAmount();
        encounter.setCreditPaidAmount(currentPaidAmount - originalSettlementAmount);

        getPatientEncounterFacade().edit(encounter);

    }

    @Inject
    private BillBeanController billBean;

    public List<Bill> getBills() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<Bill>();
            }
        }
        return bills;
    }

    public List<Bill> getRequests() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            }
            if (bills == null) {
                bills = new ArrayList<Bill>();
            }
        }
        return bills;
    }
    @EJB
    private BillFacade billFacade;

    public List<Bill> getInstitutionPaymentBills() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            if (bills == null) {
                if (txtSearch == null || txtSearch.trim().equals("")) {
                    sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType=:type and b.createdAt between :fromDate and :toDate order by b.id";
                    temMap.put("toDate", getToDate());
                    temMap.put("fromDate", getFromDate());
                    temMap.put("type", BillType.PaymentBill);
                    bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

                } else {
                    sql = "select b from BilledBill b where b.retired=false and b.billType=:type and b.createdAt between :fromDate and :toDate and ((b.staff.person.name) like '%" + txtSearch.toUpperCase() + "%'  or (b.staff.person.phone) like '%" + txtSearch.toUpperCase() + "%'  or (b.insId) like '%" + txtSearch.toUpperCase() + "%') order by b.id desc  ";
                    temMap.put("toDate", getToDate());
                    temMap.put("fromDate", getFromDate());
                    temMap.put("type", BillType.PaymentBill);
                    bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);
                }
                if (bills == null) {
                    bills = new ArrayList<Bill>();
                }
            }
        }
        return bills;

    }

    public List<Bill> getUserBills() {
        List<Bill> userBills;
        //////// // System.out.println("getting user bills");
        if (getUser() == null) {
            userBills = new ArrayList<Bill>();
            //////// // System.out.println("user is null");
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), BillType.OpdBill);
            //////// // System.out.println("user ok");
        }
        if (userBills == null) {
            userBills = new ArrayList<Bill>();
        }
        return userBills;
    }

    public List<Bill> getOpdBills() {
        if (txtSearch == null || txtSearch.trim().equals("")) {
            bills = getBillBean().billsForTheDay(fromDate, toDate, BillType.OpdBill);
        } else {
            bills = getBillBean().billsFromSearch(txtSearch, fromDate, toDate, BillType.OpdBill);
        }

        if (bills == null) {
            bills = new ArrayList<Bill>();
        }
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
        recreateModel();
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
    public CreditCompanyBillSearch() {
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

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        resetLists();
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        resetLists();
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

    public CreditBean getCreditBean() {
        return creditBean;
    }

    public void setCreditBean(CreditBean creditBean) {
        this.creditBean = creditBean;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
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
}
