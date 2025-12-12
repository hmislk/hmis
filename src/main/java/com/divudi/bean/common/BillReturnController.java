package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.lab.LabTestHistoryController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.HistoryType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.lab.PatientInvestigationStatus;

import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.Staff;

import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.entity.lab.PatientSampleComponant;
import com.divudi.core.entity.lab.Sample;

import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PatientSampleComponantFacade;
import com.divudi.service.BillService;
import com.divudi.service.DrawerService;
import com.divudi.service.PaymentService;
import com.divudi.service.ProfessionalPaymentService;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author Damith Deshan
 */
@Named
@SessionScoped
public class BillReturnController implements Serializable, ControllerWithMultiplePayments {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    BillFacade billFacade;
    @EJB
    BillNumberGenerator billNumberGenerator;
    @EJB
    PaymentService paymentService;
    @EJB
    DrawerService drawerService;
    @EJB
    ProfessionalPaymentService professionalPaymentService;
    @EJB
    BillService billService;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    BillController billController;
    @Inject
    BillBeanController billBeanController;
    @Inject
    BillItemController billItemController;
    @Inject
    BillFeeController billFeeController;
    @Inject
    DrawerController drawerController;
    @Inject
    AgentAndCcApplicationController agentAndCcApplicationController;
    @Inject
    WebUserController webUserController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    PatientInvestigationController patientInvestigationController;
    @Inject
    LabTestHistoryController labTestHistoryController;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Class Variable">
    private Staff toStaff;
    private Bill originalBillToReturn;
    private List<BillItem> originalBillItemsAvailableToReturn;
    private List<BillItem> originalBillItemsToSelectedToReturn;

    private Bill newlyReturnedBill;
    private List<BillItem> newlyReturnedBillItems;
    private List<BillFee> newlyReturnedBillFees;
    private List<Payment> returningBillPayments;

    private PaymentMethod paymentMethod;
    private List<PaymentMethod> paymentMethods;
    private final AtomicBoolean returningStarted = new AtomicBoolean(false);

    private double refundingTotalAmount;
    private String refundComment;
    private boolean selectAll;

    private PaymentMethodData paymentMethodData;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigation Method">
    public String navigateToReturnOpdBill() {
        if (originalBillToReturn == null) {
            return null;
        }
        originalBillItemsAvailableToReturn = billBeanController.fetchBillItems(originalBillToReturn);
        returningStarted.set(false);
        paymentMethod = originalBillToReturn.getPaymentMethod();
        initializeReturnBillFromOriginalBill(originalBillToReturn);
        paymentMethods = paymentService.fetchAvailablePaymentMethodsForRefundsAndCancellations(originalBillToReturn);
        return "/opd/bill_return?faces-redirect=true";
    }

    public String navigateToReturnCCBill() {
        if (originalBillToReturn == null) {
            return null;
        }

        //System.out.println("Original Bill= " + originalBillToReturn);
        originalBillItemsAvailableToReturn = billBeanController.fetchBillItems(originalBillToReturn);
        //System.out.println("Bill Items Available To Return = " + originalBillItemsAvailableToReturn.size());
        returningStarted.set(false);
        paymentMethod = originalBillToReturn.getPaymentMethod();
        return "/collecting_centre/bill_return?faces-redirect=true";
    }

    public String navigateToOPDBillSearchFormRefundOpdBillView() {
        return "/opd/opd_bill_search?faces-redirect=true";
    }

    public String navigateToRefundBillViewFormOPDBillSearch() {
        return "/opd/bill_return_print?faces-redirect=true";
    }

    public String navigateToRefundCCBillViewFormCCBillSearch() {
        return "/opd/bill_return_print?faces-redirect=true";
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Method">
    public BillReturnController() {
    }

    public void makeNull() {

    }

    public void clear() {
        refundComment = null;
        refundingTotalAmount = 0.0;
        returningBillPayments = null;
        newlyReturnedBillFees = null;
        newlyReturnedBillItems = null;
        originalBillItemsToSelectedToReturn = null;
        selectAll = true;
    }

    public void selectAllItems() {
        originalBillItemsToSelectedToReturn = new ArrayList();
        for (BillItem selectedBillItemToReturn : originalBillItemsAvailableToReturn) {
            if (!selectedBillItemToReturn.isRefunded()) {
                originalBillItemsToSelectedToReturn.add(selectedBillItemToReturn);
            }
        }
        calculateRefundingAmount();
        selectAll = false;
    }

    public void unSelectAllItems() {
        originalBillItemsToSelectedToReturn = new ArrayList();
        refundingTotalAmount = 0.0;
        selectAll = true;
    }

    public boolean checkCanReturnBill(Bill bill) {
        List<BillItem> items = billBeanController.fetchBillItems(bill);
        boolean canReturn = false;
        for (BillItem bllItem : items) {
            if (!bllItem.isRefunded()) {
                canReturn = true;
            }
        }
        return canReturn;
    }

    public boolean checkDraverBalance(Drawer drawer, PaymentMethod paymentMethod) {
        boolean canReturn = false;

        switch (paymentMethod) {
            case Cash:
                if (drawer.getCashInHandValue() != null) {
                    if (drawer.getCashInHandValue() < refundingTotalAmount) {
                        canReturn = false;
                    } else {
                        canReturn = true;
                    }
                } else {
                    canReturn = false;
                }
                break;
            case Card:
                canReturn = true;
                break;
            case MultiplePaymentMethods:
                canReturn = true;
                break;
            case Staff:
                canReturn = true;
                break;
            case Credit:
                canReturn = true;
                break;
            case Staff_Welfare:
                canReturn = true;
                break;
            case Cheque:
                canReturn = true;
                break;
            case Slip:
                canReturn = true;
                break;
            case OnlineSettlement:
                canReturn = true;
                break;
            case PatientDeposit:
                canReturn = true;
                break;
            default:
                break;
        }
        if (!configOptionApplicationController.getBooleanValueByKey("Enable Drawer Manegment", true)) {
            canReturn = true;
        }
        return canReturn;
    }

    @EJB
    PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    PatientSampleComponantFacade patientSampleComponantFacade;

    public PatientInvestigation getPatientInvestigationsFromBillItem(BillItem billItem) {
        String j = "select pi from PatientInvestigation pi where pi.retired = :ret and pi.billItem =:billItem";

        Map m = new HashMap();
        m.put("billItem", billItem);
        m.put("ret", false);
        return patientInvestigationFacade.findFirstByJpql(j, m);
    }

    public String settleOpdReturnBill() {
        if (!returningStarted.compareAndSet(false, true)) {
            JsfUtil.addErrorMessage("Already Returning Started");
            return null;
        }
        if (originalBillToReturn == null) {
            JsfUtil.addErrorMessage("Already Returning Started");
            returningStarted.set(false);
            return null;
        }
        if (originalBillItemsToSelectedToReturn == null || originalBillItemsToSelectedToReturn.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing selected to return");
            returningStarted.set(false);
            return null;
        }

        if (refundComment == null || refundComment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter Refund Comment");
            returningStarted.set(false);
            return null;
        }

        if (!webUserController.hasPrivilege("OpdReturn")) {
            JsfUtil.addErrorMessage("You have no Privilege to Refund OPD Bills. Please Contact System Administrator.");
            returningStarted.set(false);
            return null;
        }

        Bill backward = originalBillToReturn.getBackwardReferenceBill();

        if (backward != null && backward.getPaymentMethod() == PaymentMethod.Credit) {
            List<BillItem> items = billService.checkCreditBillPaymentReciveFromCreditCompany(backward);
            if (items != null && !items.isEmpty()) {
                returningStarted.set(false);
                JsfUtil.addErrorMessage("This bill has been paid for by the credit company. Therefore, it cannot be Refund.");
                return null;
            }
        }

        for (BillItem bi : originalBillItemsToSelectedToReturn) {
            if (bi.getItem() instanceof Investigation) {
                PatientInvestigation pi = getPatientInvestigationsFromBillItem(bi);
                if (pi == null) {
                    returningStarted.set(false);
                    JsfUtil.addErrorMessage("Patient Investigation not found for this item.");
                    return null;
                }
                if (pi.getStatus() != PatientInvestigationStatus.ORDERED) {

                    String investigationjpql = "select psc from PatientSampleComponant psc "
                            + " where psc.patientInvestigation = :pi "
                            + " and psc.separated = :sept and psc.retired = :ret "
                            + " and psc.patientSample.sampleRejected = :rej";

                    Map params = new HashMap();
                    params.put("pi", pi);
                    params.put("sept", false);
                    params.put("ret", false);
                    params.put("rej", false);

                    PatientSampleComponant psc = patientSampleComponantFacade.findFirstByJpql(investigationjpql, params);

                    if (psc == null) {
                        //can Refund Item
                    }
                    
                    if (psc != null) {
                        String jpql = "select psc from PatientSampleComponant psc where "
                                + " psc.patientSample = :sample"
                                + " and psc.separated = :sept "
                                + " and psc.retired = :ret "
                                + " and psc.patientSample.sampleRejected = :rej";

                        Map params2 = new HashMap();
                        params2.put("sample", psc.getPatientSample());
                        params2.put("sept", false);
                        params2.put("ret", false);
                        params2.put("rej", false);

                        List<PatientSampleComponant> patientSampleComponants = patientSampleComponantFacade.findByJpql(jpql, params2);

                        if (patientSampleComponants == null || patientSampleComponants.isEmpty()) {
                            //can Refund Item
                        } else if (patientSampleComponants.size() > 1) {
                            returningStarted.set(false);
                            JsfUtil.addErrorMessage("This item can't be refunded. First separate this investigation sample.");
                            return null;
                        } else {
                            PatientSample currentPatientSample = patientSampleComponants.get(0).getPatientSample();
                            
                            if(currentPatientSample == null){
                                //can Refund Item
                            }else if (currentPatientSample.getStatus() == PatientInvestigationStatus.SAMPLE_SENT_TO_OUTLAB) {
                                returningStarted.set(false);
                                JsfUtil.addErrorMessage("This item can't be refunded. This investigation sample has been sent to an external lab.");
                                return null;
                            } else if (currentPatientSample.getStatus() == PatientInvestigationStatus.SAMPLE_SENT_TO_INTERNAL_LAB) {
                                returningStarted.set(false);
                                JsfUtil.addErrorMessage("This item can't be refunded. This investigation sample has been sent to the lab.");
                                return null;
                            } else if (currentPatientSample.getStatus() == PatientInvestigationStatus.SAMPLE_ACCEPTED) {
                                returningStarted.set(false);
                                JsfUtil.addErrorMessage("This item can't be refunded. This investigation sample is currently in the lab.");
                                return null;
                            }
                        }
                    }
                }
            }
        }

        calculateRefundingAmount();

        Drawer loggedUserDraver = drawerController.getUsersDrawer(sessionController.getLoggedUser());

        if (!drawerService.hasSufficientDrawerBalance(loggedUserDraver, paymentMethod, refundingTotalAmount)) {
            JsfUtil.addErrorMessage("Your Draver does not have enough Money");
            returningStarted.set(false);
            return null;
        }

        originalBillToReturn = billFacade.findWithoutCache(originalBillToReturn.getId());

        if (originalBillToReturn.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            returningStarted.set(false);
            return null;
        }

        for (BillItem bi : originalBillItemsToSelectedToReturn) {
            if (professionalPaymentService.isProfessionalFeePaid(originalBillToReturn, bi)) {
                JsfUtil.addErrorMessage("Staff or Outside Institute fees have already been paid for the " + bi.getItem().getName() + " procedure.");
                return null;
            }
        }

        //TO DO: Check weather selected items is refunded
        if (!checkCanReturnBill(originalBillToReturn)) {
            JsfUtil.addErrorMessage("All Items are Already Refunded");
            returningStarted.set(false);
            return null;
        }

        // fetch original bill now, checked alteady returned, cancelled, ,
        newlyReturnedBill = new RefundBill();
        newlyReturnedBill.copy(originalBillToReturn);
        newlyReturnedBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_REFUND);
        newlyReturnedBill.setComments(refundComment);
        newlyReturnedBill.setInstitution(sessionController.getInstitution());
        newlyReturnedBill.setDepartment(sessionController.getDepartment());
        newlyReturnedBill.setReferenceBill(originalBillToReturn);
        newlyReturnedBill.invertValueOfThisBill();

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.OPD_BILL_REFUND);
        newlyReturnedBill.setDeptId(deptId);
        newlyReturnedBill.setInsId(deptId);
        billController.save(newlyReturnedBill);

        List<Bill> refundBillList = originalBillToReturn.getRefundBills();
        refundBillList.add(newlyReturnedBill);
        originalBillToReturn.setRefunded(true);
        originalBillToReturn.setRefundBills(refundBillList);

        billController.save(originalBillToReturn);
        double returningTotal = 0.0;
        double returningNetTotal = 0.0;
        double returningHospitalTotal = 0.0;
        double returningStaffTotal = 0.0;
        double returningReagentTotal = 0.0;
        double returningOtherTotal = 0.0;
        double returningDiscount = 0.0;

        newlyReturnedBillItems = new ArrayList<>();
        returningBillPayments = new ArrayList<>();
        newlyReturnedBillFees = new ArrayList<>();

        for (BillItem selectedBillItemToReturn : originalBillItemsToSelectedToReturn) {
            returningTotal += selectedBillItemToReturn.getGrossValue();
            returningNetTotal += selectedBillItemToReturn.getNetValue();
            returningHospitalTotal += selectedBillItemToReturn.getHospitalFee();
            returningStaffTotal += selectedBillItemToReturn.getStaffFee();
            returningReagentTotal += selectedBillItemToReturn.getReagentFee();
            returningOtherTotal += selectedBillItemToReturn.getOtherFee();
            returningDiscount += selectedBillItemToReturn.getDiscount();

            BillItem newlyCreatedReturningItem = new BillItem();
            newlyCreatedReturningItem.copy(selectedBillItemToReturn);
            newlyCreatedReturningItem.invertValue();
            newlyCreatedReturningItem.setBill(newlyReturnedBill);
            newlyCreatedReturningItem.setReferanceBillItem(selectedBillItemToReturn);
            billItemController.save(newlyCreatedReturningItem);
            newlyReturnedBillItems.add(newlyCreatedReturningItem);
            selectedBillItemToReturn.setRefunded(true);
            selectedBillItemToReturn.setReferanceBillItem(newlyCreatedReturningItem);
            billItemController.save(selectedBillItemToReturn);
            List<BillFee> originalBillFeesOfSelectedBillItem = billBeanController.fetchBillFees(selectedBillItemToReturn);

            try {
                if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
                    for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsFromBillItem(selectedBillItemToReturn)) {
                        labTestHistoryController.addRefundHistory(pi, sessionController.getDepartment(), refundComment);
                    }
                }
            } catch (Exception e) {
            }

            if (originalBillFeesOfSelectedBillItem != null) {
                for (BillFee origianlFee : originalBillFeesOfSelectedBillItem) {
                    BillFee newlyCreatedBillFeeToReturn = new BillFee();
                    newlyCreatedBillFeeToReturn.copy(origianlFee);
                    newlyCreatedBillFeeToReturn.invertValue();
                    newlyCreatedBillFeeToReturn.setBill(newlyReturnedBill);
                    newlyCreatedBillFeeToReturn.setBillItem(newlyCreatedReturningItem);
                    newlyCreatedBillFeeToReturn.setReferenceBillFee(origianlFee);
                    newlyCreatedBillFeeToReturn.setReferenceBillItem(selectedBillItemToReturn);
                    billFeeController.save(newlyCreatedBillFeeToReturn);
                    newlyReturnedBillFees.add(newlyCreatedBillFeeToReturn);

                    origianlFee.setReturned(true);
                    origianlFee.setReferenceBillFee(newlyCreatedBillFeeToReturn);
                    origianlFee.setReferenceBillItem(newlyCreatedReturningItem);
                    billFeeController.save(origianlFee);

                }
            }
        }

        newlyReturnedBill.setGrantTotal(0 - returningTotal);
        newlyReturnedBill.setNetTotal(0 - returningNetTotal);
        newlyReturnedBill.setTotal(0 - returningTotal);
        newlyReturnedBill.setHospitalFee(0 - returningHospitalTotal);
        newlyReturnedBill.setProfessionalFee(0 - returningStaffTotal);
        newlyReturnedBill.setDiscount(0 - returningDiscount);
        billController.save(newlyReturnedBill);

        returningBillPayments = paymentService.createPayment(newlyReturnedBill, getPaymentMethodData());

//        Payment returningPayment = new Payment();
//        returningPayment.setBill(newlyReturnedBill);
//        returningPayment.setPaymentMethod(paymentMethod);
//        returningPayment.setInstitution(sessionController.getInstitution());
//        returningPayment.setDepartment(sessionController.getDepartment());
//        returningPayment.setPaidValue(newlyReturnedBill.getNetTotal());
//        paymentController.save(returningPayment);
//        returningBillPayments.add(returningPayment);
        paymentService.updateBalances(returningBillPayments);

//        if (paymentMethod == PaymentMethod.PatientDeposit) {
//            PatientDeposit pd = patientDepositController.getDepositOfThePatient(newlyReturnedBill.getPatient(), sessionController.getDepartment());
//            patientDepositController.updateBalance(newlyReturnedBill, pd);
//        } else if (paymentMethod == PaymentMethod.Staff_Welfare) {
//            staffBean.updateStaffWelfare(newlyReturnedBill.getToStaff(), -Math.abs(newlyReturnedBill.getNetTotal()));
//            System.out.println("updated = ");
//        }
        // drawer Update
//        drawerController.updateDrawerForOuts(returningPayment);
        returningStarted.set(false);
        return "/opd/bill_return_print?faces-redirect=true";

    }

    public void calculateRefundingAmount() {
        refundingTotalAmount = 0.0;
        for (BillItem selectedBillItemToReturn : originalBillItemsToSelectedToReturn) {
            refundingTotalAmount += selectedBillItemToReturn.getNetValue();
        }
        if (originalBillItemsToSelectedToReturn.size() == 0) {
            selectAll = true;
        } else {
            selectAll = false;
        }
    }

    public String settleCCReturnBill() {
        if (!returningStarted.compareAndSet(false, true)) {
            JsfUtil.addErrorMessage("Already Returning Started");
            return null;
        }
        if (originalBillToReturn == null) {
            JsfUtil.addErrorMessage("Already Returning Started");
            returningStarted.set(false);
            return null;
        }
        if (originalBillItemsToSelectedToReturn == null || originalBillItemsToSelectedToReturn.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing selected to return");
            returningStarted.set(false);
            return null;
        }

        if (refundComment == null || refundComment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Enter Refund Comment");
            returningStarted.set(false);
            return null;
        }
        
        for (BillItem bi : originalBillItemsToSelectedToReturn) {
            if (bi.getItem() instanceof Investigation) {
                PatientInvestigation pi = getPatientInvestigationsFromBillItem(bi);
                if (pi == null) {
                    returningStarted.set(false);
                    JsfUtil.addErrorMessage("Patient Investigation not found for this item.");
                    return null;
                }
                if (pi.getStatus() != PatientInvestigationStatus.ORDERED) {

                    String investigationjpql = "select psc from PatientSampleComponant psc "
                            + " where psc.patientInvestigation = :pi "
                            + " and psc.separated = :sept and psc.retired = :ret "
                            + " and psc.patientSample.sampleRejected = :rej";

                    Map params = new HashMap();
                    params.put("pi", pi);
                    params.put("sept", false);
                    params.put("ret", false);
                    params.put("rej", false);

                    PatientSampleComponant psc = patientSampleComponantFacade.findFirstByJpql(investigationjpql, params);

                    if (psc == null) {
                        //can Refund Item
                    }
                    
                    if (psc != null) {
                        String jpql = "select psc from PatientSampleComponant psc where "
                                + " psc.patientSample = :sample"
                                + " and psc.separated = :sept "
                                + " and psc.retired = :ret "
                                + " and psc.patientSample.sampleRejected = :rej";

                        Map params2 = new HashMap();
                        params2.put("sample", psc.getPatientSample());
                        params2.put("sept", false);
                        params2.put("ret", false);
                        params2.put("rej", false);

                        List<PatientSampleComponant> patientSampleComponants = patientSampleComponantFacade.findByJpql(jpql, params2);

                        if (patientSampleComponants == null || patientSampleComponants.isEmpty()) {
                            //can Refund Item
                        } else if (patientSampleComponants.size() > 1) {
                            returningStarted.set(false);
                            JsfUtil.addErrorMessage("This item can't be refunded. First separate this investigation sample.");
                            return null;
                        } else {
                            PatientSample currentPatientSample = patientSampleComponants.get(0).getPatientSample();
                            
                            if(currentPatientSample == null){
                                //can Refund Item
                            }else if (currentPatientSample.getStatus() == PatientInvestigationStatus.SAMPLE_SENT_TO_OUTLAB) {
                                returningStarted.set(false);
                                JsfUtil.addErrorMessage("This item can't be refunded. This investigation sample has been sent to an external lab.");
                                return null;
                            } else if (currentPatientSample.getStatus() == PatientInvestigationStatus.SAMPLE_SENT_TO_INTERNAL_LAB) {
                                returningStarted.set(false);
                                JsfUtil.addErrorMessage("This item can't be refunded. This investigation sample has been sent to the lab.");
                                return null;
                            } else if (currentPatientSample.getStatus() == PatientInvestigationStatus.SAMPLE_ACCEPTED) {
                                returningStarted.set(false);
                                JsfUtil.addErrorMessage("This item can't be refunded. This investigation sample is currently in the lab.");
                                return null;
                            }
                        }
                    }
                }
            }
        }

        calculateRefundingAmount();

        originalBillToReturn = billFacade.findWithoutCache(originalBillToReturn.getId());
        if (originalBillToReturn.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            returningStarted.set(false);
            return null;
        }
        //TO DO: Check weather selected items is refunded
        if (!checkCanReturnBill(originalBillToReturn)) {
            JsfUtil.addErrorMessage("All Items are Already Refunded");
            returningStarted.set(false);
            return null;
        }

        // fetch original bill now, checked alteady returned, cancelled, ,
        newlyReturnedBill = new RefundBill();
        newlyReturnedBill.copy(originalBillToReturn);
        newlyReturnedBill.setBillTypeAtomic(BillTypeAtomic.CC_BILL_REFUND);
        newlyReturnedBill.setComments(refundComment);
        newlyReturnedBill.setInstitution(sessionController.getInstitution());
        newlyReturnedBill.setDepartment(sessionController.getDepartment());
        newlyReturnedBill.setReferenceBill(originalBillToReturn);
        newlyReturnedBill.setBillDate(new Date());
        newlyReturnedBill.setBillTime(new Date());
//        newlyReturnedBill.invertValueOfThisBill();

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.CC_BILL_REFUND);
        newlyReturnedBill.setDeptId(deptId);
        newlyReturnedBill.setInsId(deptId);
        billController.save(newlyReturnedBill);

        List<Bill> refundBillList = originalBillToReturn.getRefundBills();
        refundBillList.add(newlyReturnedBill);
        originalBillToReturn.setRefunded(true);
        originalBillToReturn.setRefundBills(refundBillList);

        billController.save(originalBillToReturn);
        double returningTotal = 0.0;
        double returningNetTotal = 0.0;
        double returningHospitalTotal = 0.0;
        double returningCCTotal = 0.0;
        double returningStaffTotal = 0.0;
        double returningDiscount = 0.0;

        newlyReturnedBillItems = new ArrayList<>();
        returningBillPayments = new ArrayList<>();
        newlyReturnedBillFees = new ArrayList<>();

        for (BillItem selectedBillItemToReturn : originalBillItemsToSelectedToReturn) {

            returningTotal += selectedBillItemToReturn.getGrossValue();
            returningNetTotal += selectedBillItemToReturn.getNetValue();
            returningHospitalTotal += selectedBillItemToReturn.getHospitalFee();
            returningCCTotal += selectedBillItemToReturn.getCollectingCentreFee();
            returningStaffTotal += selectedBillItemToReturn.getStaffFee();
            returningDiscount += selectedBillItemToReturn.getDiscount();

            BillItem newlyCreatedReturningItem = new BillItem();
            newlyCreatedReturningItem.copy(selectedBillItemToReturn);
            newlyCreatedReturningItem.invertValue();
            newlyCreatedReturningItem.setBill(newlyReturnedBill);
            newlyCreatedReturningItem.setReferanceBillItem(selectedBillItemToReturn);
            billItemController.save(newlyCreatedReturningItem);
            newlyReturnedBillItems.add(newlyCreatedReturningItem);
            selectedBillItemToReturn.setRefunded(true);
            selectedBillItemToReturn.setReferanceBillItem(newlyCreatedReturningItem);
            billItemController.save(selectedBillItemToReturn);
            List<BillFee> originalBillFeesOfSelectedBillItem = billBeanController.fetchBillFees(selectedBillItemToReturn);

            try {
                if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
                    for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsFromBillItem(selectedBillItemToReturn)) {
                        labTestHistoryController.addRefundHistory(pi, sessionController.getDepartment(), refundComment);
                    }
                }
            } catch (Exception e) {
            }

            if (originalBillFeesOfSelectedBillItem != null) {
                for (BillFee origianlFee : originalBillFeesOfSelectedBillItem) {
                    BillFee newlyCreatedBillFeeToReturn = new BillFee();
                    newlyCreatedBillFeeToReturn.copy(origianlFee);
                    newlyCreatedBillFeeToReturn.invertValue();
                    newlyCreatedBillFeeToReturn.setBill(newlyReturnedBill);
                    newlyCreatedBillFeeToReturn.setBillItem(newlyCreatedReturningItem);
                    newlyCreatedBillFeeToReturn.setReferenceBillFee(origianlFee);
                    newlyCreatedBillFeeToReturn.setReferenceBillItem(selectedBillItemToReturn);
                    billFeeController.save(newlyCreatedBillFeeToReturn);
                    newlyReturnedBillFees.add(newlyCreatedBillFeeToReturn);

                    origianlFee.setReturned(true);
                    origianlFee.setReferenceBillFee(newlyCreatedBillFeeToReturn);
                    origianlFee.setReferenceBillItem(newlyCreatedReturningItem);
                    billFeeController.save(origianlFee);

                }
            }
        }

// Print the original values
        System.out.println("Original returningTotal: " + returningTotal);
        System.out.println("Original returningNetTotal: " + returningNetTotal);
        System.out.println("Original returningHospitalTotal: " + returningHospitalTotal);
        System.out.println("Original returningCCTotal: " + returningCCTotal);
        System.out.println("Original returningStaffTotal: " + returningStaffTotal);
        System.out.println("Original returningDiscount: " + returningDiscount);

// Convert all values to negative absolute amounts
        returningTotal = -Math.abs(returningTotal);
        returningNetTotal = -Math.abs(returningNetTotal);
        returningHospitalTotal = -Math.abs(returningHospitalTotal);
        returningCCTotal = -Math.abs(returningCCTotal);
        returningStaffTotal = -Math.abs(returningStaffTotal);
        returningDiscount = -Math.abs(returningDiscount);
// Print the adjusted values

// Print the adjusted values
        System.out.println("Adjusted returningTotal: " + returningTotal);
        System.out.println("Adjusted returningNetTotal: " + returningNetTotal);
        System.out.println("Adjusted returningHospitalTotal: " + returningHospitalTotal);
        System.out.println("Adjusted returningCCTotal: " + returningCCTotal);
        System.out.println("Adjusted returningStaffTotal: " + returningStaffTotal);
        System.out.println("Adjusted returningDiscount: " + returningDiscount);

// Assign the adjusted values to newlyReturnedBill
        newlyReturnedBill.setGrantTotal(returningTotal);
        newlyReturnedBill.setNetTotal(returningNetTotal);
        newlyReturnedBill.setTotal(returningTotal);
        newlyReturnedBill.setHospitalFee(returningHospitalTotal);
        newlyReturnedBill.setCollctingCentreFee(returningCCTotal);
        newlyReturnedBill.setProfessionalFee(returningStaffTotal);
        newlyReturnedBill.setDiscount(returningDiscount);
// Print the values before setting

// Print the values before setting
        System.out.println("Setting TotalHospitalFee: " + returningHospitalTotal);
        System.out.println("Setting TotalCenterFee: " + returningCCTotal);

// Assign the values
        newlyReturnedBill.setTotalHospitalFee(returningHospitalTotal);
        newlyReturnedBill.setTotalCenterFee(returningCCTotal);
        newlyReturnedBill.setTotalStaffFee(returningStaffTotal);

        billController.save(newlyReturnedBill);

        agentAndCcApplicationController.updateCcBalance(
                originalBillToReturn.getCollectingCentre(),
                newlyReturnedBill.getHospitalFee(),
                newlyReturnedBill.getCollctingCentreFee(),
                newlyReturnedBill.getProfessionalFee(),
                newlyReturnedBill.getNetTotal(),
                HistoryType.CollectingCentreBillingRefund,
                newlyReturnedBill);

        returningStarted.set(false);
        return "/collecting_centre/cc_bill_return_print?faces-redirect=true";

    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public Bill getOriginalBillToReturn() {
        return originalBillToReturn;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setOriginalBillToReturn(Bill originalBillToReturn) {
        this.originalBillToReturn = originalBillToReturn;
    }

    public List<BillItem> getOriginalBillItemsAvailableToReturn() {
        return originalBillItemsAvailableToReturn;
    }

    public void setOriginalBillItemsAvailableToReturn(List<BillItem> originalBillItemsAvailableToReturn) {
        this.originalBillItemsAvailableToReturn = originalBillItemsAvailableToReturn;
    }

    public List<BillItem> getOriginalBillItemsToSelectedToReturn() {
        return originalBillItemsToSelectedToReturn;
    }

    public void setOriginalBillItemsToSelectedToReturn(List<BillItem> originalBillItemsToSelectedToReturn) {
        this.originalBillItemsToSelectedToReturn = originalBillItemsToSelectedToReturn;
    }

    public Bill getNewlyReturnedBill() {
        return newlyReturnedBill;
    }

    public void setNewlyReturnedBill(Bill newlyReturnedBill) {
        this.newlyReturnedBill = newlyReturnedBill;
    }

    public List<BillItem> getNewlyReturnedBillItems() {
        return newlyReturnedBillItems;
    }

    public void setNewlyReturnedBillItems(List<BillItem> newlyReturnedBillItems) {
        this.newlyReturnedBillItems = newlyReturnedBillItems;
    }

    public List<BillFee> getNewlyReturnedBillFees() {
        return newlyReturnedBillFees;
    }

    public void setNewlyReturnedBillFees(List<BillFee> newlyReturnedBillFees) {
        this.newlyReturnedBillFees = newlyReturnedBillFees;
    }

    public List<Payment> getReturningBillPayments() {
        return returningBillPayments;
    }

    public void setReturningBillPayments(List<Payment> returningBillPayments) {
        this.returningBillPayments = returningBillPayments;
    }

    public boolean isReturningStarted() {
        return returningStarted.get();
    }

    public void setReturningStarted(boolean returningStarted) {
        this.returningStarted.set(returningStarted);
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getRefundingTotalAmount() {
        return refundingTotalAmount;
    }

    public void setRefundingTotalAmount(double refundingTotalAmount) {
        this.refundingTotalAmount = refundingTotalAmount;
    }

    public String getRefundComment() {
        return refundComment;
    }

    public void setRefundComment(String refundComment) {
        this.refundComment = refundComment;
    }

    public boolean isSelectAll() {
        return selectAll;
    }

    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    // </editor-fold>
    @Override
    public double calculatRemainForMultiplePaymentTotal() {
        throw new UnsupportedOperationException("Multiple Payments Not supported in Returns and Refunds.");
    }

    @Override
    public void recieveRemainAmountAutomatically() {
        throw new UnsupportedOperationException("Multiple Payments Not supported in Returns and Refunds");
    }

    @Override
    public boolean isLastPaymentEntry(ComponentDetail cd) {
        throw new UnsupportedOperationException("Multiple Payments Not supported in Returns and Refunds");
    }

    @Override
    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        throw new UnsupportedOperationException("Multiple Payments Not supported in Returns and Refunds");
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }
    
    private void initializeReturnBillFromOriginalBill(Bill originalBillToReturn) {
        switch (originalBillToReturn.getPaymentMethod()) {
            case Cheque:
                getPaymentMethodData().getCheque().setInstitution(originalBillToReturn.getBank());
                getPaymentMethodData().getCheque().setDate(originalBillToReturn.getChequeDate());
                getPaymentMethodData().getCheque().setNo(originalBillToReturn.getChequeRefNo());
                getPaymentMethodData().getCheque().setComment(originalBillToReturn.getComments());
                getPaymentMethodData().getCheque().setTotalValue(originalBillToReturn.getNetTotal());
                break;
            case Card:
                getPaymentMethodData().getCreditCard().setInstitution(originalBillToReturn.getBank());
                getPaymentMethodData().getCreditCard().setNo(originalBillToReturn.getCreditCardRefNo());
                getPaymentMethodData().getCreditCard().setComment(originalBillToReturn.getComments());
                break;
            case Slip:
                getPaymentMethodData().getSlip().setInstitution(originalBillToReturn.getBank());
                getPaymentMethodData().getSlip().setDate(originalBillToReturn.getChequeDate());
                getPaymentMethodData().getSlip().setComment(originalBillToReturn.getComments());
//                getPaymentMethodData().getSlip().setReferenceNo(originalBillToReturn.getReferenceNumber());
                break;
            default:
                break;
        }
    }
    
    
}
