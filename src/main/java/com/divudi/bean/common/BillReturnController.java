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
import com.divudi.core.entity.Institution;
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
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author Damith Deshan
 */
@Named
@SessionScoped
public class BillReturnController implements Serializable, ControllerWithMultiplePayments {

    private static final Logger logger = Logger.getLogger(BillReturnController.class.getName());

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
    @Inject
    private com.divudi.bean.common.PatientDepositController patientDepositController;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Class Variable">
    private Staff toStaff;
    private Institution creditCompany;
    private Bill originalBillToReturn;
    private List<BillItem> originalBillItemsAvailableToReturn;
    private List<BillItem> originalBillItemsToSelectedToReturn;

    private Bill newlyReturnedBill;
    private List<BillItem> newlyReturnedBillItems;
    private List<BillFee> newlyReturnedBillFees;
    private List<Payment> returningBillPayments;
    private List<Payment> originalBillPayments;

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
        if(paymentMethod == PaymentMethod.Staff_Welfare){
            toStaff = originalBillToReturn.getToStaff();
        }

        // Set controller properties from original bill for proper return processing
        creditCompany = originalBillToReturn.getCreditCompany();
        paymentMethods = paymentService.fetchAvailablePaymentMethodsForRefundsAndCancellations(originalBillToReturn);

      

        // Check if this is an individual bill that references a batch bill (has payments)
        Bill billToFetchPaymentsFrom = originalBillToReturn;
        if (originalBillToReturn.getBackwardReferenceBill() != null) {
            billToFetchPaymentsFrom = originalBillToReturn.getBackwardReferenceBill();
        }

        originalBillPayments = billBeanController.fetchBillPayments(billToFetchPaymentsFrom);
      
        if (originalBillPayments != null && !originalBillPayments.isEmpty()) {
            initializePaymentDataFromOriginalPayments(originalBillPayments);
        }
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

    // <editor-fold defaultstate="collapsed" desc="Helper Methods">
    /**
     * Initializes payment method data from the original bill's payment records.
     * This method populates payment method details (card numbers, reference numbers, etc.)
     * based on how the original bill was paid.
     *
     * @param originalPayments List of Payment objects from the original bill
     */
    private void initializePaymentDataFromOriginalPayments(List<Payment> originalPayments) {
        if (originalPayments == null || originalPayments.isEmpty()) {
            return;
        }

        // For single payment method
        if (originalPayments.size() == 1) {
            Payment originalPayment = originalPayments.get(0);
            paymentMethod = originalPayment.getPaymentMethod();


            // Initialize paymentMethodData based on payment method (using absolute values for UI display)
            // Note: Total value will be updated later when user selects items to refund
            switch (originalPayment.getPaymentMethod()) {
                case Cash:
                    getPaymentMethodData().getCash().setTotalValue(Math.abs(originalBillToReturn.getNetTotal()));
                    break;
                case Card:
                    getPaymentMethodData().getCreditCard().setInstitution(originalPayment.getBank());
                    getPaymentMethodData().getCreditCard().setNo(originalPayment.getCreditCardRefNo());
                    getPaymentMethodData().getCreditCard().setComment(originalPayment.getComments());
                    getPaymentMethodData().getCreditCard().setTotalValue(Math.abs(originalBillToReturn.getNetTotal()));
                    break;
                case Cheque:
                    getPaymentMethodData().getCheque().setInstitution(originalPayment.getBank());
                    getPaymentMethodData().getCheque().setDate(originalPayment.getChequeDate());
                    getPaymentMethodData().getCheque().setNo(originalPayment.getChequeRefNo());
                    getPaymentMethodData().getCheque().setComment(originalPayment.getComments());
                    getPaymentMethodData().getCheque().setTotalValue(Math.abs(originalBillToReturn.getNetTotal()));
                    break;
                case Slip:
                    getPaymentMethodData().getSlip().setInstitution(originalPayment.getBank());
                    getPaymentMethodData().getSlip().setDate(originalPayment.getPaymentDate());
                    getPaymentMethodData().getSlip().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getSlip().setComment(originalPayment.getComments());
                    getPaymentMethodData().getSlip().setTotalValue(Math.abs(originalBillToReturn.getNetTotal()));
                    break;
                case ewallet:
                    System.out.println("=== EWALLET CASE DEBUG ===");
                    System.out.println("Original Payment ID: " + originalPayment.getId());
                    System.out.println("Bank: " + (originalPayment.getBank() != null ? originalPayment.getBank().getName() + " (ID: " + originalPayment.getBank().getId() + ")" : "null"));
                    System.out.println("Institution: " + (originalPayment.getInstitution() != null ? originalPayment.getInstitution().getName() + " (ID: " + originalPayment.getInstitution().getId() + ")" : "null"));
                    System.out.println("ReferenceNo: " + originalPayment.getReferenceNo());
                    System.out.println("Comments: " + originalPayment.getComments());

                    Institution selectedInstitution = originalPayment.getBank() != null ? originalPayment.getBank() : originalPayment.getInstitution();
                    System.out.println("Selected Institution: " + (selectedInstitution != null ? selectedInstitution.getName() + " (ID: " + selectedInstitution.getId() + ")" : "null"));

                    getPaymentMethodData().getEwallet().setInstitution(selectedInstitution);
                    getPaymentMethodData().getEwallet().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getEwallet().setNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getEwallet().setReferralNo(originalPayment.getPolicyNo());
                    getPaymentMethodData().getEwallet().setTotalValue(Math.abs(originalBillToReturn.getNetTotal()));
                    getPaymentMethodData().getEwallet().setComment(originalPayment.getComments());

                    System.out.println("After setting - PaymentMethodData eWallet Institution: " +
                        (getPaymentMethodData().getEwallet().getInstitution() != null ?
                         getPaymentMethodData().getEwallet().getInstitution().getName() + " (ID: " + getPaymentMethodData().getEwallet().getInstitution().getId() + ")" :
                         "null"));
                    System.out.println("=== END EWALLET CASE DEBUG ===");
                    break;
                case PatientDeposit:
                    getPaymentMethodData().getPatient_deposit().setTotalValue(Math.abs(originalBillToReturn.getNetTotal()));
                    getPaymentMethodData().getPatient_deposit().setPatient(originalBillToReturn.getPatient());
                    getPaymentMethodData().getPatient_deposit().setComment(originalPayment.getComments());
                    // Load and set the PatientDeposit object for displaying balance
                    if (originalBillToReturn.getPatient() != null) {
                        com.divudi.core.entity.PatientDeposit pd = patientDepositController.getDepositOfThePatient(
                                originalBillToReturn.getPatient(),
                                sessionController.getDepartment()
                        );
                        if (pd != null && pd.getId() != null) {
                            getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                            getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                        }
                    }
                    break;
                case Credit:
                    getPaymentMethodData().getCredit().setInstitution(originalPayment.getCreditCompany());
                    getPaymentMethodData().getCredit().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getCredit().setReferralNo(originalPayment.getPolicyNo());
                    getPaymentMethodData().getCredit().setComment(originalPayment.getComments());
                    getPaymentMethodData().getCredit().setTotalValue(Math.abs(originalBillToReturn.getNetTotal()));
                    break;
                case Staff:
                    com.divudi.core.entity.Staff staffForCredit = originalPayment.getToStaff();
                    if (staffForCredit == null && originalBillToReturn != null) {
                        staffForCredit = originalBillToReturn.getToStaff();
                    }
                    getPaymentMethodData().getStaffCredit().setToStaff(staffForCredit);
                    getPaymentMethodData().getStaffCredit().setComment(originalPayment.getComments());
                    getPaymentMethodData().getStaffCredit().setTotalValue(Math.abs(originalBillToReturn.getNetTotal()));
                    break;
                case Staff_Welfare:
                    com.divudi.core.entity.Staff staffForWelfare = originalPayment.getToStaff();
                    if (staffForWelfare == null && originalBillToReturn != null) {
                        staffForWelfare = originalBillToReturn.getToStaff();
                    }
                    getPaymentMethodData().getStaffWelfare().setToStaff(staffForWelfare);
                    getPaymentMethodData().getStaffWelfare().setComment(originalPayment.getComments());
                    getPaymentMethodData().getStaffWelfare().setTotalValue(Math.abs(originalBillToReturn.getNetTotal()));
                    break;
                case OnlineSettlement:
                    getPaymentMethodData().getOnlineSettlement().setInstitution(originalPayment.getBank() != null ? originalPayment.getBank() : originalPayment.getInstitution());
                    getPaymentMethodData().getOnlineSettlement().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getOnlineSettlement().setDate(originalPayment.getPaymentDate());
                    getPaymentMethodData().getOnlineSettlement().setComment(originalPayment.getComments());
                    getPaymentMethodData().getOnlineSettlement().setTotalValue(Math.abs(originalBillToReturn.getNetTotal()));
                    break;
                default:
                    // For any other payment method, just set the total value
                    break;
            }
        } else {
            // Multiple payments - set to MultiplePaymentMethods
            paymentMethod = PaymentMethod.MultiplePaymentMethods;
            // Note: For multiple payments, the user would need to manually configure them
            // This is a complex scenario that may require additional UI handling
            System.out.println("Multiple payments detected - set to MultiplePaymentMethods");
        }
        System.out.println("=== END initializePaymentDataFromOriginalPayments DEBUG ===");
    }

    /**
     * Applies refund sign (negative values) to all payment method data.
     * This ensures that payment records for refunds/cancellations are stored with negative amounts.
     */
    private void applyRefundSignToPaymentData() {
        if (paymentMethodData == null) {
            return;
        }

        // Only apply negative sign to the currently selected payment method
        // This prevents accidentally processing payment data from the original bill's payment method
        if (paymentMethod == null) {
            return;
        }

        switch (paymentMethod) {
            case Cash:
                if (paymentMethodData.getCash() != null && paymentMethodData.getCash().getTotalValue() > 0) {
                    paymentMethodData.getCash().setTotalValue(-Math.abs(paymentMethodData.getCash().getTotalValue()));
                }
                break;
            case Card:
                if (paymentMethodData.getCreditCard() != null && paymentMethodData.getCreditCard().getTotalValue() > 0) {
                    paymentMethodData.getCreditCard().setTotalValue(-Math.abs(paymentMethodData.getCreditCard().getTotalValue()));
                }
                break;
            case Cheque:
                if (paymentMethodData.getCheque() != null && paymentMethodData.getCheque().getTotalValue() > 0) {
                    paymentMethodData.getCheque().setTotalValue(-Math.abs(paymentMethodData.getCheque().getTotalValue()));
                }
                break;
            case Slip:
                if (paymentMethodData.getSlip() != null && paymentMethodData.getSlip().getTotalValue() > 0) {
                    paymentMethodData.getSlip().setTotalValue(-Math.abs(paymentMethodData.getSlip().getTotalValue()));
                }
                break;
            case ewallet:
                if (paymentMethodData.getEwallet() != null && paymentMethodData.getEwallet().getTotalValue() > 0) {
                    paymentMethodData.getEwallet().setTotalValue(-Math.abs(paymentMethodData.getEwallet().getTotalValue()));
                }
                break;
            case PatientDeposit:
                if (paymentMethodData.getPatient_deposit() != null && paymentMethodData.getPatient_deposit().getTotalValue() > 0) {
                    paymentMethodData.getPatient_deposit().setTotalValue(-Math.abs(paymentMethodData.getPatient_deposit().getTotalValue()));
                }
                break;
            case Credit:
                if (paymentMethodData.getCredit() != null && paymentMethodData.getCredit().getTotalValue() > 0) {
                    paymentMethodData.getCredit().setTotalValue(-Math.abs(paymentMethodData.getCredit().getTotalValue()));
                }
                break;
            case Staff:
            case OnCall:
                if (paymentMethodData.getStaffCredit() != null && paymentMethodData.getStaffCredit().getTotalValue() > 0) {
                    paymentMethodData.getStaffCredit().setTotalValue(-Math.abs(paymentMethodData.getStaffCredit().getTotalValue()));
                }
                break;
            case Staff_Welfare:
                if (paymentMethodData.getStaffWelfare() != null && paymentMethodData.getStaffWelfare().getTotalValue() > 0) {
                    paymentMethodData.getStaffWelfare().setTotalValue(-Math.abs(paymentMethodData.getStaffWelfare().getTotalValue()));
                }
                break;
            case OnlineSettlement:
                if (paymentMethodData.getOnlineSettlement() != null && paymentMethodData.getOnlineSettlement().getTotalValue() > 0) {
                    paymentMethodData.getOnlineSettlement().setTotalValue(-Math.abs(paymentMethodData.getOnlineSettlement().getTotalValue()));
                }
                break;
            case MultiplePaymentMethods:
                // For multiple payment methods, apply refund sign to all component payment methods
                if (paymentMethodData.getPaymentMethodMultiple() != null
                        && paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() != null) {
                    for (com.divudi.core.entity.ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                        if (cd.getPaymentMethodData() != null) {
                            // Recursively apply refund sign to each component
                            PaymentMethod originalPaymentMethod = paymentMethod;
                            paymentMethod = cd.getPaymentMethod();
                            PaymentMethodData originalData = paymentMethodData;
                            paymentMethodData = cd.getPaymentMethodData();
                            applyRefundSignToPaymentData();
                            paymentMethodData = originalData;
                            paymentMethod = originalPaymentMethod;
                        }
                    }
                }
                break;
            default:
                // No action needed for other payment methods
                break;
        }
    }

    /**
     * Transfer controller properties (staff, credit company) to payment method data
     * This ensures payment details are properly set in payment data before creating payments
     */
    private void transferPaymentDataFromControllerProperties() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }

        // Transfer staff data for staff-related payment methods
        if (toStaff != null) {
            switch (paymentMethod) {
                case Staff_Welfare:
                    paymentMethodData.getStaffWelfare().setToStaff(toStaff);
                    if (paymentMethodData.getStaffWelfare().getTotalValue() == 0) {
                        paymentMethodData.getStaffWelfare().setTotalValue(Math.abs(refundingTotalAmount));
                    }
                    break;
                case Staff:
                case OnCall:
                    paymentMethodData.getStaffCredit().setToStaff(toStaff);
                    if (paymentMethodData.getStaffCredit().getTotalValue() == 0) {
                        paymentMethodData.getStaffCredit().setTotalValue(Math.abs(refundingTotalAmount));
                    }
                    break;
                default:
                    break;
            }
        }

        // Transfer credit company for credit payment method
        if (paymentMethod == PaymentMethod.Credit && creditCompany != null) {
            paymentMethodData.getCredit().setInstitution(creditCompany);
            if (paymentMethodData.getCredit().getTotalValue() == 0) {
                paymentMethodData.getCredit().setTotalValue(Math.abs(refundingTotalAmount));
            }
        }

        // Debug logging
        if (paymentMethod == PaymentMethod.Credit && creditCompany != null) {
            logger.fine("transferPaymentDataFromControllerProperties - Credit Company: " + creditCompany.getName());
            logger.fine("Credit Institution set to: " +
                (paymentMethodData.getCredit().getInstitution() != null ?
                paymentMethodData.getCredit().getInstitution().getName() : "null"));
        }
        if (toStaff != null && (paymentMethod == PaymentMethod.Staff_Welfare || paymentMethod == PaymentMethod.Staff)) {
            logger.fine("transferPaymentDataFromControllerProperties - Staff: " + toStaff.getPerson().getName());
        }
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
        newlyReturnedBill.setPaymentMethod(paymentMethod);
        newlyReturnedBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_REFUND);
        newlyReturnedBill.setPaymentMethod(paymentMethod);
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

        // Update batch bill balance for credit payment method returns
        updateBatchBillFinancialFieldsForIndividualReturn(originalBillToReturn, newlyReturnedBill);

        // Apply refund sign to payment data
        applyRefundSignToPaymentData();

        // Transfer controller properties to payment method data before creating payments
        transferPaymentDataFromControllerProperties();

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

        // Update payment method data with calculated amount for partial returns
        updatePaymentMethodDataWithRefundingAmount();

        if (originalBillItemsToSelectedToReturn.size() == 0) {
            selectAll = true;
        } else {
            selectAll = false;
        }
    }

    /**
     * Updates payment method data with calculated refunding amount when items are selected.
     * This ensures the payment form shows the correct amount for partial returns.
     */
    private void updatePaymentMethodDataWithRefundingAmount() {
        if (paymentMethodData == null || refundingTotalAmount == 0.0) {
            return;
        }

        // Update the total value for the selected payment method
        // Use absolute value because negatives are applied later in applyRefundSignToPaymentData()
        double absoluteAmount = Math.abs(refundingTotalAmount);

        switch (paymentMethod) {
            case Cash:
                paymentMethodData.getCash().setTotalValue(absoluteAmount);
                break;
            case Card:
                paymentMethodData.getCreditCard().setTotalValue(absoluteAmount);
                break;
            case Cheque:
                paymentMethodData.getCheque().setTotalValue(absoluteAmount);
                break;
            case Slip:
                paymentMethodData.getSlip().setTotalValue(absoluteAmount);
                break;
            case ewallet:
                paymentMethodData.getEwallet().setTotalValue(absoluteAmount);
                break;
            case PatientDeposit:
                paymentMethodData.getPatient_deposit().setTotalValue(absoluteAmount);
                break;
            case Credit:
                paymentMethodData.getCredit().setTotalValue(absoluteAmount);
                break;
            case Staff:
                paymentMethodData.getStaffCredit().setTotalValue(absoluteAmount);
                break;
            case Staff_Welfare:
                paymentMethodData.getStaffWelfare().setTotalValue(absoluteAmount);
                break;
            case OnlineSettlement:
                paymentMethodData.getOnlineSettlement().setTotalValue(absoluteAmount);
                break;
            default:
                break;
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

    public List<Payment> getOriginalBillPayments() {
        return originalBillPayments;
    }

    public void setOriginalBillPayments(List<Payment> originalBillPayments) {
        this.originalBillPayments = originalBillPayments;
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

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    /**
     * Updates the batch bill's financial tracking fields when individual OPD bill items
     * are returned within a credit payment batch.
     *
     * <p>This method ensures accurate credit balance tracking for partial item returns by
     * reducing the batch bill's balance, paidAmount, and increasing refundAmount proportionally
     * to the returned items' net total value.</p>
     *
     * <p><b>Healthcare Domain Context:</b> When OPD bills are paid using Credit payment
     * method, the net total becomes the "due amount" (stored in balance field). Credit
     * companies settle these dues periodically. Accurate balance tracking is critical
     * for credit company settlement reports and financial reconciliation.</p>
     *
     * <p><b>Pattern:</b> Mirrors pharmacy implementation and individual bill cancellation
     * patterns for consistent balance management across all return scenarios.</p>
     *
     * @param originalBillToReturn The original individual OPD bill being partially/fully returned
     * @param newlyReturnedBill The return bill created for the returned items
     * @throws IllegalArgumentException if bills are null
     * @throws IllegalStateException if financial data is invalid
     * @see <a href="https://github.com/hmislk/hmis/issues/17138">GitHub Issue #17138</a>
     */
    private void updateBatchBillFinancialFieldsForIndividualReturn(Bill originalBillToReturn, Bill newlyReturnedBill) {
        // Validate inputs
        if (originalBillToReturn == null) {
            throw new IllegalArgumentException("Original bill to return cannot be null");
        }

        if (newlyReturnedBill == null) {
            throw new IllegalArgumentException("Newly returned bill cannot be null");
        }

        Bill batchBill = originalBillToReturn.getBackwardReferenceBill();

        // Not all individual bills have batch bills (e.g., direct OPD bills)
        if (batchBill == null) {
            return;
        }

        // Only update balance for Credit payment method bills
        if (batchBill.getPaymentMethod() != PaymentMethod.Credit) {
            return;
        }

        // Validate numeric fields
        if (newlyReturnedBill.getNetTotal() == 0.0) {
            throw new IllegalStateException("Return bill net total is invalid");
        }

        // Refresh batch bill from database to ensure latest data and trigger optimistic locking
        batchBill = billFacade.find(batchBill.getId());

        if (batchBill == null) {
            throw new IllegalStateException("Batch bill not found in database");
        }

        // Calculate refund amount (always positive) - return bills have negative values
        double refundAmount = Math.abs(newlyReturnedBill.getNetTotal());

        // Validate refund amount doesn't exceed original batch bill total
        if (refundAmount > Math.abs(batchBill.getNetTotal())) {
            throw new IllegalStateException(
                String.format("CRITICAL: Refund amount (%.2f) exceeds batch bill total (%.2f). " +
                             "Batch Bill: %s, Original Bill: %s",
                             refundAmount, Math.abs(batchBill.getNetTotal()),
                             batchBill.getInsId(), originalBillToReturn.getInsId())
            );
        }

        // Store old values for audit trail
        double oldBalance = batchBill.getBalance();
        double oldPaidAmount = batchBill.getPaidAmount();
        double oldRefundAmount = batchBill.getRefundAmount();

        // Update refundAmount - add the return amount
        batchBill.setRefundAmount(batchBill.getRefundAmount() + refundAmount);

        // Update paidAmount - deduct the return amount (only if payment exists)
        if (batchBill.getPaidAmount() > 0) {
            batchBill.setPaidAmount(Math.max(0d, batchBill.getPaidAmount() - refundAmount));
        }

        // Update balance (due amount) - deduct the return amount (only if balance > 0)
        if (batchBill.getBalance() > 0) {
            batchBill.setBalance(Math.max(0d, batchBill.getBalance() - refundAmount));
        }

        try {
            // Save the updated bill
            billFacade.edit(batchBill);

            System.out.println("=== OPD Return - Batch Bill Balance Updated ===");
            System.out.println("Batch Bill ID: " + batchBill.getInsId());
            System.out.println("Original Bill: " + originalBillToReturn.getInsId());
            System.out.println("Return Bill: " + newlyReturnedBill.getInsId());
            System.out.println("Refund Amount: " + refundAmount);
            System.out.println("Old Balance: " + oldBalance + " → New Balance: " + batchBill.getBalance());
            System.out.println("Old Paid: " + oldPaidAmount + " → New Paid: " + batchBill.getPaidAmount());
            System.out.println("Old Refund: " + oldRefundAmount + " → New Refund: " + batchBill.getRefundAmount());

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error updating batch bill balance: " + e.getMessage());
            System.err.println("Failed to update batch bill balance: " + e.getMessage());
            e.printStackTrace();
            // Don't re-throw to prevent return process from failing completely
            // The individual bill return should still succeed
        }
    }

    /**
     * Called when user changes payment method in bill return form.
     * Resets paymentMethodData to prevent using old payment method data.
     */
    public void onPaymentMethodChange() {
        // Reset payment method data to prevent using old payment method data
        paymentMethodData = new PaymentMethodData();

        // Clear controller properties that should only be set for specific payment methods
        // This prevents accidentally using staff/company from original bill when changing to different payment method
        if (paymentMethod != PaymentMethod.Staff_Welfare && paymentMethod != PaymentMethod.Staff && paymentMethod != PaymentMethod.OnCall) {
            toStaff = null;
        }
        if (paymentMethod != PaymentMethod.Credit) {
            creditCompany = null;
        }

        // Initialize basic payment data based on newly selected payment method
        if (paymentMethod != null && originalBillToReturn != null) {
            double netTotal = Math.abs(refundingTotalAmount > 0 ? refundingTotalAmount : originalBillToReturn.getNetTotal());

            switch (paymentMethod) {
                case Cash:
                    paymentMethodData.getCash().setTotalValue(netTotal);
                    break;
                case Card:
                    paymentMethodData.getCreditCard().setTotalValue(netTotal);
                    break;
                case Cheque:
                    paymentMethodData.getCheque().setTotalValue(netTotal);
                    break;
                case Slip:
                    paymentMethodData.getSlip().setTotalValue(netTotal);
                    break;
                case ewallet:
                    paymentMethodData.getEwallet().setTotalValue(netTotal);
                    break;
                case Staff_Welfare:
                    paymentMethodData.getStaffWelfare().setTotalValue(netTotal);
                    if (toStaff != null) {
                        paymentMethodData.getStaffWelfare().setToStaff(toStaff);
                    }
                    break;
                case Staff:
                case OnCall:
                    paymentMethodData.getStaffCredit().setTotalValue(netTotal);
                    if (toStaff != null) {
                        paymentMethodData.getStaffCredit().setToStaff(toStaff);
                    }
                    break;
                case Credit:
                    paymentMethodData.getCredit().setTotalValue(netTotal);
                    if (creditCompany != null) {
                        paymentMethodData.getCredit().setInstitution(creditCompany);
                    }
                    break;
                case PatientDeposit:
                    paymentMethodData.getPatient_deposit().setTotalValue(netTotal);
                    if (originalBillToReturn.getPatient() != null) {
                        paymentMethodData.getPatient_deposit().setPatient(originalBillToReturn.getPatient());
                    }
                    break;
                case MultiplePaymentMethods:
                    // For multiple payments, clear the component details
                    paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().clear();
                    break;
                default:
                    // For other payment methods, just initialize with net total
                    break;
            }
        }
    }

}
