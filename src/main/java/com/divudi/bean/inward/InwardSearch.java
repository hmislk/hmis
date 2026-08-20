/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.FinancialTransactionController;
import com.divudi.bean.common.*;
import com.divudi.bean.lab.LabTestHistoryController;

import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.dto.InwardBillReceiptDTO;
import com.divudi.core.data.EmailAttachment;
import com.divudi.core.data.MessageType;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.core.data.inward.SurgeryBillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.EjbApplication;
import com.divudi.core.entity.*;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.EncounterComponent;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.EncounterComponentFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.DrawerService;
import com.divudi.service.PaymentService;
import com.divudi.service.RequestService;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
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
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class InwardSearch implements Serializable {

    /**
     * EJBs
     */
    @EJB
    PatientEncounterFacade patientEncounterFacade;
    @EJB
    EjbApplication ejbApplication;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacede;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillComponentFacade billCommponentFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    PersonFacade personFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    PaymentService paymentService;
    @EJB
    DrawerService drawerService;
    @EJB
    private com.divudi.service.AuditService auditService;
    @EJB
    private com.divudi.core.facade.EmailFacade emailFacade;
    @EJB
    private com.divudi.ejb.EmailManagerEjb emailManagerEjb;

    /**
     * JSF Controllers
     */
    @Inject
    DrawerController drawerController;
    @Inject
    private BillBeanController billBean;
    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    BhtSummeryFinalizedController bhtSummeryFinalizedController;
    @Inject
    SessionController sessionController;
    @Inject
    FinancialTransactionController financialTransactionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    PatientInvestigationController patientInvestigationController;
    @Inject
    PatientDepositController patientDepositController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    LabTestHistoryController labTestHistoryController;
    @Inject
    InwardPaymentController inwardPaymentController;
    @Inject
    InwardDepositController inwardDepositController;

    /**
     * Properties
     */
    private Bill bill;
    private boolean printPreview = false;

    /**
     * Bill id to render on {@code /inward/inward_view_appointment_bill_receipt}
     * (Issue #22783 — appointment bill / appointment cancel bill view, routed
     * from {@code BillSearch.navigateToViewBillByAtomicBillType}). Appointment
     * bills never have {@code Bill.patientEncounter} populated, so they can't
     * use the regular {@code inward_reprint_bill_payment} template; the DTO
     * query behind {@link #getAppointmentBillReceipt()} handles that via
     * LEFT JOINs instead.
     */
    private Long appointmentReceiptBillId;
    private InwardBillReceiptDTO appointmentBillReceipt;
    @Temporal(TemporalType.TIME)
    private Date fromDate;
    @Temporal(TemporalType.TIME)
    private Date toDate;
    private String comment;
    WebUser user;
    ////////////////////
    List<BillEntry> billEntrys;
    List<BillItem> billItems;
    List<BillComponent> billComponents;
    List<BillFee> billFees;
    List<BillItem> refundingItems;
    private List<Bill> bills;
    private List<BillItem> tempbillItems;
    private List<Bill> finalBillVersions;
    /////////////////////

    PaymentMethod paymentMethod;

    ReportKeyWord reportKeyWord;

    private YearMonthDay yearMonthDay;
    Patient patient;
    Sex[] sex;
    private Admission admission;
    private PaymentMethodData paymentMethodData;
    private PaymentMethodData originalBillPaymentMethodData;
    boolean showOrginalBill;

    private boolean withProfessionalFee = false;
    private boolean showZeroInwardChargeCategoryTypes = false;
    private boolean changed = false;

    public PaymentMethodData loadCurrentBillPaymentMethodData(Bill bill) {
        System.out.println("loadCurrentBillPaymentMethodData");
        System.out.println("bill = " + bill);
        PaymentMethodData newPmd = new PaymentMethodData();
        if (bill == null || bill.getId() == null) {
            System.out.println("bill is null or not persisted, returning empty PaymentMethodData");
            return newPmd;
        }

        List<Payment> originalPayments = getBillBean().fetchBillPayments(bill);
        System.out.println("originalPayments = " + originalPayments);
        if (originalPayments == null || originalPayments.isEmpty()) {
            System.out.println("no original payments found, returning empty PaymentMethodData");
            return newPmd;
        }

        PaymentMethod firstMethod = null;
        boolean mixedMethods = false;

        for (Payment p : originalPayments) {
            PaymentMethod pm = p.getPaymentMethod();
            System.out.println("processing payment p = " + p);
            System.out.println("payment method pm = " + pm);
            if (pm == null) {
                continue;
            }
            if (firstMethod == null) {
                firstMethod = pm;
            } else if (firstMethod != pm) {
                mixedMethods = true;
            }
            populateComponentDetailFromPayment(newPmd, p, bill);
        }

        System.out.println("firstMethod = " + firstMethod);
        System.out.println("mixedMethods = " + mixedMethods);
        newPmd.setPaymentMethod(mixedMethods ? PaymentMethod.MultiplePaymentMethods : firstMethod);
        System.out.println("resolved paymentMethod = " + newPmd.getPaymentMethod());
        return newPmd;
    }

    private void populateComponentDetailFromPayment(PaymentMethodData pmd, Payment p, Bill bill) {
        PaymentMethod pm = p.getPaymentMethod();
        if (pm == null) {
            return;
        }
        double amount = Math.abs(p.getPaidValue());
        switch (pm) {
            case Cash:
                pmd.getCash().setTotalValue(pmd.getCash().getTotalValue() + amount);
                break;
            case Card:
                pmd.getCreditCard().setTotalValue(pmd.getCreditCard().getTotalValue() + amount);
                pmd.getCreditCard().setNo(p.getCreditCardRefNo());
                pmd.getCreditCard().setInstitution(p.getBank());
                pmd.getCreditCard().setComment(p.getComments());
                break;
            case Cheque:
                pmd.getCheque().setTotalValue(pmd.getCheque().getTotalValue() + amount);
                pmd.getCheque().setNo(p.getChequeRefNo());
                pmd.getCheque().setDate(p.getChequeDate());
                pmd.getCheque().setInstitution(p.getBank());
                pmd.getCheque().setComment(p.getComments());
                break;
            case Slip:
                pmd.getSlip().setTotalValue(pmd.getSlip().getTotalValue() + amount);
                pmd.getSlip().setReferenceNo(p.getReferenceNo());
                pmd.getSlip().setDate(p.getPaymentDate());
                pmd.getSlip().setInstitution(p.getBank());
                pmd.getSlip().setComment(p.getComments());
                break;
            case ewallet:
                pmd.getEwallet().setTotalValue(pmd.getEwallet().getTotalValue() + amount);
                pmd.getEwallet().setNo(p.getReferenceNo());
                pmd.getEwallet().setInstitution(p.getBank());
                pmd.getEwallet().setComment(p.getComments());
                break;
            case PatientDeposit:
                pmd.getPatient_deposit().setTotalValue(pmd.getPatient_deposit().getTotalValue() + amount);
                if (bill.getPatientEncounter() != null) {
                    pmd.getPatient_deposit().setPatient(bill.getPatientEncounter().getPatient());
                    PatientDeposit pd = patientDepositController.checkDepositOfThePatient(
                            bill.getPatientEncounter().getPatient(), sessionController.getDepartment());
                    if (pd != null && pd.getId() != null) {
                        pmd.getPatient_deposit().setPatientDepost(pd);
                    }
                }
                break;
            case OnlineSettlement:
                pmd.getOnlineSettlement().setTotalValue(pmd.getOnlineSettlement().getTotalValue() + amount);
                pmd.getOnlineSettlement().setReferenceNo(p.getReferenceNo());
                pmd.getOnlineSettlement().setDate(p.getPaymentDate());
                pmd.getOnlineSettlement().setInstitution(p.getBank());
                pmd.getOnlineSettlement().setComment(p.getComments());
                break;
            default:
                break;
        }
    }

    public void refillPaymentDetail() {
        if (originalBillPaymentMethodData == null) {
            return;
        }
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        paymentMethodData.setPaymentMethod(originalBillPaymentMethodData.getPaymentMethod());
        
        copyComponentDetail(originalBillPaymentMethodData.getCash(), paymentMethodData.getCash());
        copyComponentDetail(originalBillPaymentMethodData.getCreditCard(), paymentMethodData.getCreditCard());
        copyComponentDetail(originalBillPaymentMethodData.getCheque(), paymentMethodData.getCheque());
        copyComponentDetail(originalBillPaymentMethodData.getSlip(), paymentMethodData.getSlip());
        copyComponentDetail(originalBillPaymentMethodData.getEwallet(), paymentMethodData.getEwallet());
        copyComponentDetail(originalBillPaymentMethodData.getPatient_deposit(), paymentMethodData.getPatient_deposit());
        copyComponentDetail(originalBillPaymentMethodData.getOnlineSettlement(), paymentMethodData.getOnlineSettlement());
    }

    private void copyComponentDetail(ComponentDetail from, ComponentDetail to) {
        if (from == null || to == null) {
            return;
        }
        to.setTotalValue(from.getTotalValue());
        to.setNo(from.getNo());
        to.setReferenceNo(from.getReferenceNo());
        to.setReferralNo(from.getReferralNo());
        to.setComment(from.getComment());
        to.setDate(from.getDate());
        to.setInstitution(from.getInstitution());
        to.setPatient(from.getPatient());
        to.setPatientDepost(from.getPatientDepost());
        to.setToStaff(from.getToStaff());
        to.setCreditDuration(from.getCreditDuration());
    }

    private List<PaymentMethod> inwardDepositCancelationPaymentMethods;
    
    public String navigateToPaymentBillCancellation() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No bill is selected");
            return "";
        }
        if (bill.getCheckeAt() != null) {
            JsfUtil.addErrorMessage("This bill is already checked. A checked bill cannot be cancelled.");
            return "";
        }
        if (bill.isRefunded()) {
            JsfUtil.addErrorMessage("This bill has already been refunded and cannot be cancelled.");
            return "";
        }
        switch (bill.getBillTypeAtomic()) {
            case INWARD_PAYMENT:
                inwardDepositCancelationPaymentMethods = new ArrayList<>();
                getInwardDepositCancelationPaymentMethods().add(PaymentMethod.Cash);
                
                if(bill.getPaymentMethod() != PaymentMethod.Cash){
                    inwardDepositCancelationPaymentMethods.add(bill.getPaymentMethod());
                }
                originalBillPaymentMethodData = new PaymentMethodData();
                originalBillPaymentMethodData = loadCurrentBillPaymentMethodData(bill);
                
                paymentMethodData = new PaymentMethodData();
                
                refillPaymentDetail();
                
                return "inward_deposit_cancel_bill_payment?faces-redirect=true";
            case INWARD_PAYMENT_REFUND:
                return "inward_deposit_refund_cancel_bill_payment?faces-redirect=true";
            default:
                return "inward_cancel_bill_payment?faces-redirect=true";
        }
    }

    public String navigateToDepositBillCancellation() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No bill is selected");
            return "";
        }
        if (bill.getCheckeAt() != null) {
            JsfUtil.addErrorMessage("This bill is already checked. A checked bill cannot be cancelled.");
            return "";
        }
        if (bill.isRefunded()) {
            JsfUtil.addErrorMessage("This bill has already been refunded and cannot be cancelled.");
            return "";
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.INWARD_DEPOSIT) {
            JsfUtil.addErrorMessage("Selected bill is not an Inward Deposit bill.");
            return "";
        }
        inwardDepositCancelationPaymentMethods = new ArrayList<>();
        getInwardDepositCancelationPaymentMethods().add(PaymentMethod.Cash);
        if (bill.getPaymentMethod() != PaymentMethod.Cash) {
            inwardDepositCancelationPaymentMethods.add(bill.getPaymentMethod());
        }
        originalBillPaymentMethodData = new PaymentMethodData();
        originalBillPaymentMethodData = loadCurrentBillPaymentMethodData(bill);
        paymentMethodData = new PaymentMethodData();
        refillPaymentDetail();
        return "inward_cancel_bill_deposit?faces-redirect=true";
    }

    public String navigateToCancelInwardServiceBillFromReprint() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No bill is selected");
            return "";
        }
        if (bill.getCheckeAt() != null) {
            JsfUtil.addErrorMessage("This bill is already checked. A checked bill cannot be cancelled.");
            return "";
        }
        return "/inward/inward_cancel_bill_service?faces-redirect=true";
    }

    public String navigateToCancelInwardProfessionalBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No bill is selected");
            return "";
        }
        if (bill.getCheckeAt() != null) {
            JsfUtil.addErrorMessage("This bill is already checked. A checked bill cannot be cancelled.");
            return "";
        }
        return "/inward/inward_cancel_bill_professional?faces-redirect=true";
    }

    public String navigateToViewInwardDepositCancellationBill(Bill b) {
        if (b == null || b.getCancelledBill() == null) {
            JsfUtil.addErrorMessage("No cancellation bill found");
            return "";
        }
        bill = b;
        billItems = null;
        printPreview = true;
        return "/inward/inward_deposit_cancel_bill_payment?faces-redirect=true";
    }

    public String navigateToViewDepositBillCancellation(Bill b) {
        if (b == null || b.getCancelledBill() == null) {
            JsfUtil.addErrorMessage("No cancellation bill found");
            return "";
        }
        bill = b;
        billItems = null;
        printPreview = true;
        return "/inward/inward_cancel_bill_deposit?faces-redirect=true";
    }

    /**
     * Reprint navigation for a row in the Interim Bill's Payments tab, which
     * can hold Payment bills, Deposit bills, and either feature's Refund
     * bills all mixed together (issue #22826). Routes each to the correct
     * reprint page: Refund bills (either feature) go to the shared generic
     * refund reprint page; Deposit pay bills go to the new Deposit reprint
     * page; everything else (Payment pay bills) keeps the existing Payment
     * reprint page.
     */
    public String navigateToPaymentOrDepositReprint(Bill b) {
        if (b == null) {
            JsfUtil.addErrorMessage("No bill is selected");
            return "";
        }
        bill = b;
        billItems = null;
        printPreview = false;
        if (b instanceof RefundBill) {
            return "/inward/inward_reprint_bill_refund?faces-redirect=true";
        }
        if (b.getBillTypeAtomic() == BillTypeAtomic.INWARD_DEPOSIT) {
            return "/inward/inward_reprint_bill_deposit?faces-redirect=true";
        }
        return "/inward/inward_reprint_bill_payment?faces-redirect=true";
    }

    public void editBillDetails() {
        Bill editedBill = bill;
        if (bill == null) {
            JsfUtil.addErrorMessage("Bill Error !");
            return;
        }
        if (referredBy == null) {
            JsfUtil.addErrorMessage("Please Select Referring Doctor !");
            return;
        }
        editedBill.setReferredBy(referredBy);
        if (bill.getId() == null) {
            billFacade.create(editedBill);
        }
        billFacade.edit(editedBill);
        JsfUtil.addSuccessMessage("Saved");
        referredBy = null;
    }

    @Inject
    RequestController requestController;
    @Inject
    RequestService requestService;

    private Doctor referredBy;

    private Request currentRequest;

    public String navigateToCancelInpatientBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No bill is selected");
            return "";
        }

        if (bill.getCheckeAt() != null) {
            JsfUtil.addErrorMessage("This bill is already checked. A checked bill cannot be cancelled.");
            return "";
        }

        DepartmentType toBillDepartmentType = DepartmentType.Other;

        if (bill.getToDepartment() != null && bill.getToDepartment().getDepartmentType() != null) {
            toBillDepartmentType = bill.getToDepartment().getDepartmentType();
        }

        boolean allowType = configOptionApplicationController.getBooleanValueByKey("Inward Billing - Mandatory permission to cancel " + toBillDepartmentType.getLabel() + " type bills", false);

        if (configOptionApplicationController.getBooleanValueByKey("Mandatory permission to cancel bills.", false) && allowType) {
            currentRequest = requestService.findRequest(bill);

            if (currentRequest == null) {
                return requestController.navigateToCreateRequest(bill);
            } else {
                switch (currentRequest.getStatus()) {
                    case PENDING:
                        requestController.setCurrentRequest(currentRequest);
                        return "/common/request/request_status?faces-redirect=true";
                    case UNDER_REVIEW:
                        requestController.setCurrentRequest(currentRequest);
                        return "/common/request/request_status?faces-redirect=true";
                    case APPROVED:
                        requestController.getBills().add(currentRequest.getBill());
                        setComment(currentRequest.getRequestReason());
                        return "/inward/inward_cancel_bill_service?faces-redirect=true";
                    default:
                        return "";
                }
            }
        } else {
            return "/inward/inward_cancel_bill_service?faces-redirect=true";
        }
    }

    public void edit() {
        if (getBill() == null) {
            return;
        }

        if (getBill().getPatientEncounter() == null) {
            return;
        }

        patientEncounterFacade.edit(getBill().getPatientEncounter());

        if (getBill().getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
            getInwardBean().updateCreditDetail(getBill().getPatientEncounter(), getBill().getPatientEncounter().getFinalBill().getNetTotal());
        }
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

//    public void update() {
//
//        for (BillItem b : bill.getBillItems()) {
//            getBillItemFacede().edit(b);
//        }
//    }
    public Sex[] getSex() {
        return Sex.values();
    }

    public void updatePatiantDetails() {
        if (bill == null || bill.getPatient() == null || bill.getPatient().getPerson() == null) {
            JsfUtil.addErrorMessage("Error in Application. Can not update.");
            return;
        }
        personFacade.edit(getBill().getPatient().getPerson());
        JsfUtil.addSuccessMessage("Patient Details Updated.");
    }

    public String fillDataForInpatientsFinalBillHeader(String template, Bill bill) {

        if (isInvalidInput(template, bill)) {
            return "";
        }

        String output = template
                .replace("{ins_name}", String.valueOf(bill.getInstitution().getName()))
                .replace("{ins_address}", String.valueOf(bill.getInstitution().getAddress()))
                .replace("{ins_phone}", String.valueOf(bill.getInstitution().getPhone()))
                .replace("{ins_fax}", String.valueOf(bill.getInstitution().getFax()))
                .replace("{ins_email}", String.valueOf(bill.getInstitution().getEmail()))
                .replace("{ins_web}", String.valueOf(bill.getInstitution().getWeb()));

        return output;
    }

    private String formatDate(Date date, SessionController sessionController) {
        return date != null ? CommonFunctions.dateToString(date, sessionController.getApplicationPreference().getLongDateFormat()) : "";
    }

    private String formatTime(Date time, SessionController sessionController) {
        return time != null ? CommonFunctions.dateToString(time, sessionController.getApplicationPreference().getLongDateFormat()) : "";
    }

    private String getAdmissionType(PatientEncounter pe) {
        return pe.getAdmissionType() != null ? pe.getAdmissionType().getName() : "";
    }

    private String getInstitutionName(PatientEncounter pe) {
        return pe.getInstitution() != null ? pe.getInstitution().getName() : "";
    }

    private String getDepartmentName(PatientEncounter pe) {
        return pe.getDepartment() != null ? pe.getDepartment().getName() : "";
    }

    private boolean isInvalidInput(String template, Bill bill) {
        return template == null || template.trim().isEmpty()
                || bill == null || bill.getPatientEncounter() == null
                || bill.getPatientEncounter().getPatient() == null
                || bill.getPatientEncounter().getPatient().getPerson() == null;
    }

//    public void replace() {
//        for (BillItem b : bill.getBillItems()) {
//            b.setAdjustedValue(b.getGrossValue());
//            getBillItemFacede().edit(b);
//        }
//    }
    public void refreshFinalBillBackwordReferenceBills() {
        withProfessionalFee = true;
        if (bill == null) {
            return;
        }
        for (Bill b : bill.getBackwardReferenceBills()) {
            //   ////// // System.out.println("b = " + b);
        }
    }

    public String fromBhtFinalBillSearchToBillReprint() {
        refreshFinalBillBackwordReferenceBills();
        bhtSummeryFinalizedController.setPatientEncounter(bill.getPatientEncounter());
        return "/inward/inward_reprint_bill_final";
    }

    public void makeNull() {
        bill = null;
        printPreview = false;
        fromDate = null;
        toDate = null;
        comment = null;
        user = null;
        billEntrys = null;
        billItems = null;
        billComponents = null;
        billFees = null;
        refundingItems = null;
        bills = null;
        tempbillItems = null;
        sentEmailsForBill = null;
    }

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

    public String navigateToFinalBillForAdmission() {
        if (admission == null) {
            JsfUtil.addErrorMessage("No Admission Selected");
            return "";
        }

        finalBillVersions = null;
        List<Bill> versions = fetchFinalBillVersions(admission);

        if (versions.isEmpty()) {
            JsfUtil.addErrorMessage("No Final Bill Created");
            return "";
        }

        if (versions.size() == 1) {
            bill = versions.get(0);
            billItems = null;
            withProfessionalFee = false;
            return "/inward/inward_reprint_bill_final?faces-redirect=true";
        }

        return "/inward/inward_final_bill_list?faces-redirect=true";
    }

    public String navigateToManageFinalBills() {
        if (admission == null) {
            JsfUtil.addErrorMessage("No Admission Selected");
            return "";
        }

        finalBillVersions = null;
        List<Bill> versions = fetchFinalBillVersions(admission);

        if (versions.isEmpty()) {
            JsfUtil.addErrorMessage("No Final Bill Created");
            return "";
        }

        return "/inward/inward_final_bill_list?faces-redirect=true";
    }

    /**
     * Lazily fetches and caches all final-bill versions for the currently
     * selected {@link #admission}. Callers that switch admissions must reset
     * {@link #finalBillVersions} to null before calling this getter again.
     */
    public List<Bill> getFinalBillVersions() {
        if (finalBillVersions == null && admission != null) {
            finalBillVersions = fetchFinalBillVersions(admission);
        }
        return finalBillVersions;
    }

    private List<Bill> fetchFinalBillVersions(PatientEncounter admission) {
        // billTypeAtomic (not just billType) is required here — the cancellation-record
        // Bill created by createCancelBill() copies billType=InwardFinalBill from the
        // bill it cancels, so filtering on billType alone would list cancellation
        // records as if they were final bill versions.
        String jpql = "select b from Bill b where b.patientEncounter = :pe and b.billType = :billType"
                + " and b.billTypeAtomic = :atomic and b.retired = false order by b.finalBillVersionSerial asc";
        Map<String, Object> params = new HashMap<>();
        params.put("pe", admission);
        params.put("billType", BillType.InwardFinalBill);
        params.put("atomic", BillTypeAtomic.INWARD_FINAL_BILL);
        return getBillFacade().findByJpql(jpql, params);
    }

    public boolean hasAnyFinalBillVersion(PatientEncounter admission) {
        if (admission == null) {
            return false;
        }
        return !fetchFinalBillVersions(admission).isEmpty();
    }

    /**
     * User-facing action to mark {@code newConfirmed} as the confirmed final
     * bill version for its admission. Privilege checks are done in the XHTML
     * via {@code rendered}, not here.
     */
    public void setAsConfirmedFinalBill(Bill newConfirmed) {
        if (newConfirmed == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return;
        }
        if (newConfirmed.isCancelled()) {
            JsfUtil.addErrorMessage("Cannot confirm a cancelled bill");
            return;
        }
        if (!newConfirmed.isApprovedFinalBill()) {
            JsfUtil.addErrorMessage("Cannot confirm an unapproved bill");
            return;
        }
        setAsConfirmedFinalBillInternal(newConfirmed);
        JsfUtil.addSuccessMessage("Final Bill Version Confirmed");
        finalBillVersions = null;
    }

    /**
     * Navigates to the approval page for the given final bill version,
     * mirroring {@link #prepareEmailFinalBillVersion(Bill)}'s use of the
     * shared {@link #bill} session field for the target page.
     */
    public String navigateToApproveFinalBill(Bill b) {
        if (b == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return "";
        }
        bill = b;
        return "/inward/inward_final_bill_approve?faces-redirect=true";
    }

    /**
     * User-facing action to approve a final bill version. Approval is a
     * prerequisite for confirming a version ({@link #setAsConfirmedFinalBill})
     * and for emailing it ({@link #emailFinalBillVersionInternal}). Privilege
     * checks are done in the XHTML via {@code rendered}, not here.
     */
    public void approveFinalBillVersion(Bill b) {
        if (b == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return;
        }
        if (b.isApprovedFinalBill()) {
            JsfUtil.addErrorMessage("Bill already approved");
            return;
        }
        b.setApprovedFinalBill(true);
        b.setFinalBillApprover(sessionController.getLoggedUser());
        b.setFinalBillApprovedAt(new Date());
        getBillFacade().edit(b);

        auditService.logEncounterAudit(b.getPatientEncounter(), "Final Bill Version Approved",
                null, b.getId(), sessionController.getLoggedUser(),
                "Bill", b.getId());

        JsfUtil.addSuccessMessage("Final Bill Approved");
        finalBillVersions = null;
    }

    /**
     * Repoints {@code pe.finalBill} to {@code newConfirmed}, flips the
     * {@code confirmedFinalBill} flag on the old and new bills, and mirrors
     * the totals copy done in {@link BhtSummeryController#settle()}. Shared
     * by the explicit "set as confirmed" action and the cancel-triggered
     * auto-promotion of the next latest version.
     */
    private void setAsConfirmedFinalBillInternal(Bill newConfirmed) {
        PatientEncounter pe = newConfirmed.getPatientEncounter();

        Long previousFinalBillId = pe.getFinalBill() != null ? pe.getFinalBill().getId() : null;

        if (pe.getFinalBill() != null && !pe.getFinalBill().getId().equals(newConfirmed.getId())) {
            Bill oldConfirmed = pe.getFinalBill();
            oldConfirmed.setConfirmedFinalBill(false);
            getBillFacade().edit(oldConfirmed);
        }

        newConfirmed.setConfirmedFinalBill(true);
        getBillFacade().edit(newConfirmed);

        pe.setFinalBill(newConfirmed);
        pe.setGrantTotal(newConfirmed.getGrantTotal());
        pe.setDiscount(newConfirmed.getDiscount());
        pe.setNetTotal(newConfirmed.getNetTotal());
        getPatientEncounterFacade().edit(pe);

        auditService.logEncounterAudit(pe, "Final Bill Version Confirmed",
                previousFinalBillId, newConfirmed.getId(), sessionController.getLoggedUser(),
                "Bill", newConfirmed.getId());
    }

    /**
     * User-facing action to retire (soft delete) a final bill version.
     * Irreversible — there is no un-retire action. Privilege checks are done
     * in the XHTML via {@code rendered}, not here.
     */
    public void retireFinalBillVersion(Bill b) {
        if (b == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return;
        }
        if (b.isRetired()) {
            JsfUtil.addErrorMessage("Bill is already retired");
            return;
        }
        if (b.isConfirmedFinalBill()) {
            JsfUtil.addErrorMessage("Cannot retire the Confirmed Final Bill. Confirm a different version first.");
            return;
        }
        List<Bill> versions = fetchFinalBillVersions(b.getPatientEncounter());
        if (versions.size() <= 1) {
            JsfUtil.addErrorMessage("Cannot retire the only remaining final bill version");
            return;
        }

        b.setRetired(true);
        b.setRetirer(sessionController.getLoggedUser());
        b.setRetiredAt(new Date());
        getBillFacade().edit(b);

        auditService.logEncounterAudit(b.getPatientEncounter(), "Final Bill Version Retired",
                b.getId(), null, sessionController.getLoggedUser(),
                "Bill", b.getId());

        JsfUtil.addSuccessMessage("Final Bill Version Retired");
        finalBillVersions = null;
    }

    private String emailRecipient;
    private String emailSubject;
    private String emailBody;
    private List<EmailAttachment> pendingEmailAttachments;
    private List<AppEmail> sentEmailsForBill;

    public String getEmailRecipient() {
        return emailRecipient;
    }

    public void setEmailRecipient(String emailRecipient) {
        this.emailRecipient = emailRecipient;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    public String getEmailBody() {
        return emailBody;
    }

    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }

    public List<EmailAttachment> getPendingEmailAttachments() {
        return pendingEmailAttachments;
    }

    /**
     * Navigates from the Final Bill Versions list to the email review page
     * for {@code b}, prefilling recipient/subject/body so the cashier can
     * check and edit them — and attach extra documents — before anything is
     * actually sent. Replaces the old pattern of sending straight from a
     * "Recipient + Send" dialog with no review step.
     */
    public String prepareEmailFinalBillVersion(Bill b) {
        if (b == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return "";
        }
        bill = b;
        PatientEncounter pe = b.getPatientEncounter();
        emailRecipient = pe != null && pe.getPatient() != null && pe.getPatient().getPerson() != null
                ? pe.getPatient().getPerson().getEmail() : null;
        emailSubject = "Final Bill " + b.getDeptId();
        emailBody = "Please find attached the final bill " + b.getDeptId() + ".";
        pendingEmailAttachments = new ArrayList<>();
        return "/inward/inward_final_bill_email?faces-redirect=true";
    }

    // Attachments are held Base64-encoded in this SessionScoped bean until sent —
    // capped here (in addition to the fileUpload's client-side sizeLimit) so a
    // careless or malicious upload can't grow session memory unbounded.
    private static final int MAX_EMAIL_ATTACHMENTS = 5;
    private static final long MAX_EMAIL_ATTACHMENT_FILE_BYTES = 10_000_000L;
    private static final long MAX_EMAIL_ATTACHMENT_TOTAL_BYTES = 20_000_000L;

    /**
     * Adds a cashier-chosen file (e.g. a supporting document requested by the
     * credit company) to the attachment list for the email being composed.
     * Kept separate from the auto-generated final bill PDF, which is always
     * attached in addition to whatever is added here.
     */
    public void uploadEmailAttachment(FileUploadEvent event) {
        if (pendingEmailAttachments == null) {
            pendingEmailAttachments = new ArrayList<>();
        }
        UploadedFile file = event.getFile();
        if (pendingEmailAttachments.size() >= MAX_EMAIL_ATTACHMENTS) {
            JsfUtil.addErrorMessage("Cannot attach more than " + MAX_EMAIL_ATTACHMENTS + " documents");
            return;
        }
        if (file.getSize() > MAX_EMAIL_ATTACHMENT_FILE_BYTES) {
            JsfUtil.addErrorMessage(file.getFileName() + " exceeds the 10MB attachment size limit");
            return;
        }
        long attachedSoFar = 0;
        for (EmailAttachment existing : pendingEmailAttachments) {
            attachedSoFar += existing.getBase64Content() != null ? existing.getBase64Content().length() * 3L / 4 : 0;
        }
        if (attachedSoFar + file.getSize() > MAX_EMAIL_ATTACHMENT_TOTAL_BYTES) {
            JsfUtil.addErrorMessage("Total attachments cannot exceed 20MB");
            return;
        }
        try {
            EmailAttachment attachment = new EmailAttachment(
                    file.getFileName(),
                    file.getContentType(),
                    Base64.getEncoder().encodeToString(file.getContent()));
            pendingEmailAttachments.add(attachment);
            JsfUtil.addSuccessMessage("Attached " + file.getFileName());
        } catch (Exception ex) {
            JsfUtil.addErrorMessage("Failed to attach file");
            java.util.logging.Logger.getLogger(InwardSearch.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Final bill email attachment failed", ex);
        }
    }

    public void removeEmailAttachment(EmailAttachment attachment) {
        if (pendingEmailAttachments != null) {
            pendingEmailAttachments.remove(attachment);
        }
    }

    /**
     * Emails a one-page summary of the given final bill version (patient,
     * admission, and totals — not a full itemized reprint) as a PDF
     * attachment, plus any cashier-attached documents, and logs the send via
     * {@link AppEmail} so it shows up in the "Sent Emails" history on the
     * view/print screen.
     */
    public void emailFinalBillVersion(Bill b) {
        boolean sent = false;
        try {
            sent = emailFinalBillVersionInternal(b);
        } finally {
            // Tell the client whether the send succeeded so the dialog closes
            // only on success (see the Send button's oncomplete), mirroring
            // BhtSummeryController.addNewCreditCompany()'s creditCompanyAdded.
            if (PrimeFaces.current().isAjaxRequest()) {
                PrimeFaces.current().ajax().addCallbackParam("emailSent", sent);
            }
        }
    }

    private boolean emailFinalBillVersionInternal(Bill b) {
        if (b == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return false;
        }
        // InwardSearch.bill is shared session state set by many unrelated flows
        // (interim estimates, staff payment cancel, etc.), and this page is reachable
        // by direct URL — without this check, a stale/unrelated bill left over from
        // another flow could be sent out mislabeled as a Final Bill.
        // billTypeAtomic is required in addition to billType: createCancelBill()
        // copies billType=InwardFinalBill onto the cancellation record it creates
        // (see fetchFinalBillVersions()), so billType alone would let a cancellation
        // record through as if it were a real final bill.
        if (b.getBillType() != BillType.InwardFinalBill
                || b.getBillTypeAtomic() != BillTypeAtomic.INWARD_FINAL_BILL) {
            JsfUtil.addErrorMessage("Selected bill is not a Final Bill");
            return false;
        }
        if (!b.isApprovedFinalBill()) {
            JsfUtil.addErrorMessage("Cannot email an unapproved Final Bill");
            return false;
        }
        if (emailRecipient == null || emailRecipient.trim().isEmpty()) {
            JsfUtil.addErrorMessage("No recipient Email");
            return false;
        }
        if (!CommonFunctions.isValidEmail(emailRecipient)) {
            JsfUtil.addErrorMessage("Recipient Email is NOT valid");
            return false;
        }

        String subject = (emailSubject != null && !emailSubject.trim().isEmpty())
                ? emailSubject : "Final Bill " + b.getDeptId();
        String body = (emailBody != null && !emailBody.trim().isEmpty())
                ? emailBody : "Please find attached the final bill " + b.getDeptId() + ".";

        AppEmail email = new AppEmail();
        email.setCreatedAt(new Date());
        email.setCreater(sessionController.getLoggedUser());
        email.setReceipientEmail(emailRecipient);
        email.setMessageSubject(subject);
        email.setMessageBody(body);
        email.setDepartment(b.getDepartment());
        email.setInstitution(b.getInstitution());
        email.setBill(b);
        email.setPatientEncounter(b.getPatientEncounter());
        email.setMessageType(MessageType.InwardFinalBillEmail);
        email.setSentSuccessfully(false);
        email.setPending(true);
        emailFacade.create(email);

        boolean success = false;
        try {
            byte[] pdfBytes = buildFinalBillPdf(b);
            EmailAttachment attachment = new EmailAttachment(
                    "FinalBill_" + b.getFinalBillVersionSerial() + ".pdf",
                    "application/pdf",
                    Base64.getEncoder().encodeToString(pdfBytes));

            List<EmailAttachment> attachments = new ArrayList<>();
            attachments.add(attachment);
            if (pendingEmailAttachments != null) {
                attachments.addAll(pendingEmailAttachments);
            }

            success = emailManagerEjb.sendEmail(
                    Collections.singletonList(email.getReceipientEmail()),
                    email.getMessageBody(),
                    email.getMessageSubject(),
                    false,
                    attachments);

            if (success) {
                email.setSentAt(new Date());
                email.setSentSuccessfully(true);
                email.setPending(false);
                emailFacade.edit(email);
                JsfUtil.addSuccessMessage("Email Sent Successfully");
                pendingEmailAttachments = new ArrayList<>();
            } else {
                email.setPending(false);
                emailFacade.edit(email);
                JsfUtil.addErrorMessage("Sending Email Failed");
            }
        } catch (Exception ex) {
            email.setPending(false);
            emailFacade.edit(email);
            JsfUtil.addErrorMessage("Sending Email Failed");
            java.util.logging.Logger.getLogger(InwardSearch.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Final bill email failed", ex);
        }

        sentEmailsForBill = null;
        return success;
    }

    private byte[] buildFinalBillPdf(Bill b) throws Exception {
        String html = buildFinalBillHtml(b);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(out);
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            pdfDoc.setDefaultPageSize(com.itextpdf.kernel.geom.PageSize.A4);
            com.itextpdf.html2pdf.HtmlConverter.convertToPdf(html, pdfDoc, new com.itextpdf.html2pdf.ConverterProperties());
            return out.toByteArray();
        }
    }

    private String buildFinalBillHtml(Bill b) {
        PatientEncounter pe = b.getPatientEncounter();
        String patientName = pe != null && pe.getPatient() != null && pe.getPatient().getPerson() != null
                ? pe.getPatient().getPerson().getNameWithTitle() : "";
        String bhtNo = pe != null ? pe.getBhtNo() : "";
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00");

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:sans-serif;font-size:12px;'>");
        sb.append("<h2>Final Bill</h2>");
        sb.append("<table style='width:100%;margin-bottom:10px;'>");
        sb.append("<tr><td><b>Bill Number</b></td><td>").append(escapeHtml(b.getDeptId())).append("</td></tr>");
        sb.append("<tr><td><b>Version</b></td><td>").append(b.getFinalBillVersionSerial()).append("</td></tr>");
        sb.append("<tr><td><b>Patient</b></td><td>").append(escapeHtml(patientName)).append("</td></tr>");
        sb.append("<tr><td><b>BHT No</b></td><td>").append(escapeHtml(bhtNo)).append("</td></tr>");
        sb.append("</table>");
        sb.append("<table style='width:100%;border-collapse:collapse;' border='1' cellpadding='4'>");
        sb.append("<tr><th style='text-align:left;'>Description</th><th style='text-align:right;'>Amount</th></tr>");
        sb.append("<tr><td>Gross Total</td><td style='text-align:right;'>").append(df.format(b.getGrantTotal())).append("</td></tr>");
        sb.append("<tr><td>Discount</td><td style='text-align:right;'>").append(df.format(b.getDiscount())).append("</td></tr>");
        sb.append("<tr><td>Net Total</td><td style='text-align:right;'>").append(df.format(b.getNetTotal())).append("</td></tr>");
        sb.append("<tr><td>Claimable Total</td><td style='text-align:right;'>").append(df.format(b.getClaimableTotal())).append("</td></tr>");
        sb.append("</table>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public List<AppEmail> getSentEmailsForBill() {
        if (sentEmailsForBill == null && bill != null) {
            sentEmailsForBill = fetchSentEmailsForBill(bill);
        }
        return sentEmailsForBill;
    }

    private List<AppEmail> fetchSentEmailsForBill(Bill b) {
        String jpql = "select e from AppEmail e where e.bill = :bill order by e.createdAt desc";
        Map<String, Object> params = new HashMap<>();
        params.put("bill", b);
        return emailFacade.findByJpql(jpql, params);
    }

    public String navigateToProvisionalBillForAdmission() {
        if (admission == null) {
            JsfUtil.addErrorMessage("No Admission Selected");
            return "";
        }

        String jpql;
        Map temMap = new HashMap();
        jpql = "select b from Bill b where"
                + " b.billType = :billType and "
                + " b.retired=false ";

        jpql += " and  b.patientEncounter=:pe ";
        temMap.put("pe", admission);

        temMap.put("billType", BillType.InwardProvisionalBill);
        jpql += " order by b.id desc ";

        // bill = getBillFacade().findFirstByJpql(jpql, temMap, TemporalType.TIMESTAMP);
        bill = getBillFacade().findFirstByJpql(jpql, temMap);
        billItems = null;

        if (bill == null) {
            JsfUtil.addErrorMessage("No Provisional Bill Created");
            return "";
        }
        withProfessionalFee = false;

        return "/inward/inward_provisional_bill_edit?faces-redirect=true";
    }

    public String navigateDoctorPayment() {
        PatientEncounter pe = inwardPaymentController.getCurrent().getPatientEncounter();
        inwardPaymentController.makeNull();
        inwardPaymentController.getCurrent().setPatientEncounter(pe);
        inwardPaymentController.setPatient(pe.getPatient());
        
        inwardPaymentController.paymentListener();
        
        if (sessionController.getPaymentManagementAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                return "/inward/inward_bill_payment?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            return "/inward/inward_bill_payment?faces-redirect=true";
        }
    }

    public String navigateMakeDeposit() {
        PatientEncounter pe = inwardDepositController.getCurrent().getPatientEncounter();
        inwardDepositController.makeNull();
        inwardDepositController.getCurrent().setPatientEncounter(pe);
        inwardDepositController.setPatient(pe.getPatient());

        inwardDepositController.paymentListener();

        return inwardDepositController.navigateToInwardDeposit();
    }

    public boolean calculateRefundTotal() {
        Double d = 0.0;
        //billItems=null;
        tempbillItems = null;
        for (BillItem i : getRefundingItems()) {
            if (checkPaidIndividual(i)) {
                JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Refund Bill");
                return false;
            }

            if (!i.isRefunded()) {
                d = d + i.getNetValue();
                getTempbillItems().add(i);
            }

        }

        return true;
    }

    public String navigateToProfessionalFeeList() {
        return "/inward/inward_search_professional_estimate?faces-redirect=true";
    }

    public void dateChangeListen() {
        getBill().getPatient().getPerson().setDob(CommonFunctions.guessDob(yearMonthDay));
    }

    public Patient getPatient() {

        if (patient == null) {
            patient = new Patient();
            Person p = new Person();

            patient.setPerson(p);
        }
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public YearMonthDay getYearMonthDay() {
        if (yearMonthDay == null) {
            yearMonthDay = new YearMonthDay();
        }
        return yearMonthDay;
    }

    public void setYearMonthDay(YearMonthDay yearMonthDay) {
        this.yearMonthDay = yearMonthDay;
    }

    public String inwardReprintBillFinal() {

        return "inward/inward_reprint_bill_final";
    }

    public List<BillItem> getRefundingItems() {
        return refundingItems;
    }

    public void setRefundingItems(List<BillItem> refundingItems) {
        this.refundingItems = refundingItems;
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

    private boolean checkInvestigation(BillItem bit) {
        HashMap hm = new HashMap();
        String sql = "SELECT p FROM PatientInvestigation p where p.retired=false and p.billItem=:bi";
        hm.put("bi", bit);
        PatientInvestigation tmp = getPatientInvestigationFacade().findFirstByJpql(sql, hm);

        if (tmp.getDataEntered()) {
            return true;
        }

        return false;
    }

    public void calculateRefundBillFees(RefundBill rb) {
        double s = 0.0;
        double b = 0.0;
        double p = 0.0;
        for (BillItem bi : refundingItems) {
            HashMap hm = new HashMap();
            String sql = "select c from BillFee c where c.billItem=:b";
            hm.put("b", bi);
            List<BillFee> rbf = getBillFeeFacade().findByJpql(sql, hm);
            for (BillFee bf : rbf) {
                if (bf.getFee().getStaff() == null) {
                    p = p + bf.getFeeValue();
                } else {
                    s = s + bf.getFeeValue();
                }
            }

        }
        rb.setStaffFee(0 - s);
        rb.setPerformInstitutionFee(0 - p);
        getBillFacade().edit(rb);
    }

    private void recreateModel() {
        billFees = null;
        billComponents = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        comment = null;
        sentEmailsForBill = null;
    }

    private void cancelBillComponents(Bill can, BillItem bt) {
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

            if (bC.getId() == null) {
                getBillCommponentFacade().create(bC);
            }
        }

    }

    @EJB
    private EncounterComponentFacade encounterComponentFacade;

    private void retireEncounterComponents() {
        for (BillItem b : getBillItems()) {
            for (EncounterComponent nB : getBillBean().getEncounterBillComponents(b)) {
                nB.setRetired(true);
                nB.setRetiredAt(new Date());
                nB.setRetirer(getSessionController().getLoggedUser());
                getEncounterComponentFacade().edit(nB);
            }
        }
    }

    private boolean checkPaid() {
        HashMap hm = new HashMap();
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.bill=:b ";
        hm.put("b", getBill());
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql, hm);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private boolean checkPaidIndividual(BillItem bi) {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    @Inject
    private EnumController enumController;

    public boolean checkCancelBill(Bill originalBill) {
        List<PatientInvestigationStatus> availableStatus = enumController.getAvailableStatusforCancel();
        boolean canCancelBill = false;
        if (availableStatus.contains(originalBill.getStatus())) {
            canCancelBill = true;
        }
        return canCancelBill;
    }

    private boolean check() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }
        
        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (getBill().getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("You can't cancel Because this Bill has no BHT");
            return true;
        }
        
        if (getBill().getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("You can't cancel. Because this BHT is Already Discharged.");
            return true;
        }

        if (getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment Method.");
            return true;
        }
        
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    private boolean checkInvestigation() {
        String sql = "SELECT p FROM PatientInvestigation p where p.retired=false "
                + " and p.billItem.bill.id=" + getBill().getId();
        List<PatientInvestigation> tmp = getPatientInvestigationFacade().findByJpql(sql);

        for (PatientInvestigation p : tmp) {
            if (p.getDataEntered()) {
                return true;
            }
        }

        return false;
    }

    public void cancelBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            if (check()) {
                return;
            }

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                JsfUtil.addErrorMessage("Final Payment is Finalized You can't Cancel");
                return;
            }

            CancelledBill cb = createCancelBill();
            //Copy & paste
            if (webUserController.hasPrivilege("LabBillCancelling")) {

                if (cb.getId() == null) {
                    getBillFacade().create(cb);
                }
                cancelBillItems(cb);
                getBill().setCancelled(true);
                getBill().setCancelledBill(cb);
                getBillFacade().edit((BilledBill) getBill());
                JsfUtil.addSuccessMessage("Cancelled");

                printPreview = true;
            } else {
                getEjbApplication().getBillsToCancel().add(cb);
                JsfUtil.addSuccessMessage("Awaiting Cancellation");
            }

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }

    }

    public void cancelBillService() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            if (getBill().getCheckedBy() != null) {
                JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
                return;
            }

            if (getBill().isCancelled()) {
                JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
                return;
            }
            if (getBill().isRefunded()) {
                JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
                return;
            }

            if (getBill().getPatientEncounter() == null) {
                JsfUtil.addErrorMessage("You can't cancel Because this Bill has no BHT");
                return;
            }

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                JsfUtil.addErrorMessage("Final Payment is Finalized You can't Cancel");
                return;
            }

            if (getBill().getPatientEncounter().isDischarged()) {
                JsfUtil.addErrorMessage("Sorry, patient is discharged.");
                return;
            }

            if (checkPaid()) {
                JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
                return;
            }

            if (!configOptionApplicationController.getBooleanValueByKey("Enable the Special Privilege of Canceling Inward Service Bills", false)) {

                if (!checkCancelBill(getBill())) {
                    JsfUtil.addErrorMessage("This bill is processed in the Laboratory.");
                    return;
                }

                if (checkInvestigation()) {
                    JsfUtil.addErrorMessage("Lab Report was already Entered .you cant Cancel");
                    return;
                }
            } else {
                if (!getWebUserController().hasPrivilege("LabBillCancelSpecial")) {
                    JsfUtil.addErrorMessage("You have no privilege to cancel This Bill");
                }
            }

            CancelledBill cb = createCancelBill();
            //Copy & paste
            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);

            try {
                if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
                    for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsFromBill(getBill())) {
                        labTestHistoryController.addCancelHistory(pi, sessionController.getDepartment(), comment);
                    }
                }
            } catch (Exception e) {
            }

            //To null payment methord
            getBill().setPaymentMethod(null);
            cb.setPaymentMethod(null);

            getBillFacade().edit(cb);
            getBillFacade().edit((BilledBill) getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            getBillBean().updateBatchBill(getBill().getForwardReferenceBill());

            if (configOptionApplicationController.getBooleanValueByKey("Mandatory permission to cancel bills.", false)) {
                Request billRequest = requestService.findRequest(getBill());
                if (billRequest != null) {

                    requestController.getBills().add(getBill());
                    requestController.complteRequest(billRequest);
                }
            }

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }

    }

    public void cancelInwardProfessionalFeeBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            if (getBill().getCheckedBy() != null) {
                JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
                return;
            }

            if (getBill().isCancelled()) {
                JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
                return;
            }
            if (getBill().isRefunded()) {
                JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
                return;
            }

            if (getBill().getPatientEncounter() == null) {
                JsfUtil.addErrorMessage("You can't cancel Because this Bill has no BHT");
                return;
            }

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                JsfUtil.addErrorMessage("Final Payment is Finalized You can't Cancel");
                return;
            }

            if (getBill().getPatientEncounter().isDischarged()) {
                JsfUtil.addErrorMessage("Sorry, patient is discharged.");
                return;
            }

            if (checkPaid()) {
                JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
                return;
            }

            CancelledBill cb = createCancelBill();
            cb.setBillTypeAtomic(BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL_CANCELLATION);
            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);

            getBill().setPaymentMethod(null);
            cb.setPaymentMethod(null);

            getBillFacade().edit(cb);
            getBillFacade().edit((BilledBill) getBill());
            getBillBean().updateBatchBillExcludingCancelled(getBill().getForwardReferenceBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public void cancelTheatreProfessionalFeeBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            if (getBill().getCheckedBy() != null) {
                JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
                return;
            }

            if (getBill().isCancelled()) {
                JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
                return;
            }
            if (getBill().isRefunded()) {
                JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
                return;
            }

            if (getBill().getPatientEncounter() == null) {
                JsfUtil.addErrorMessage("You can't cancel Because this Bill has no BHT");
                return;
            }

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                JsfUtil.addErrorMessage("Final Payment is Finalized You can't Cancel");
                return;
            }

            if (getBill().getPatientEncounter().isDischarged()) {
                JsfUtil.addErrorMessage("Sorry, patient is discharged.");
                return;
            }

            if (checkPaid()) {
                JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
                return;
            }

            if (getBill().getPatient() == null && getBill().getPatientEncounter().getPatient() != null) {
                getBill().setPatient(getBill().getPatientEncounter().getPatient());
            }

            CancelledBill cb = createCancelBill();
            cb.setBillTypeAtomic(BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL_CANCELLATION);
            if (cb.getPatient() == null && getBill().getPatient() != null) {
                cb.setPatient(getBill().getPatient());
            }
            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);

            getBill().setPaymentMethod(null);
            cb.setPaymentMethod(null);

            getBillFacade().edit(cb);
            getBillFacade().edit((BilledBill) getBill());
            getBillBean().updateBatchBillExcludingCancelled(getBill().getForwardReferenceBill());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    @Inject
    private InwardBeanController inwardBean;
    @EJB
    CashTransactionBean cashTransactionBean;

    public void cancelBillPayment() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            if (check()) {
                return;
            }

            if (getBill().getCheckedBy() != null) {
                JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
                return;
            }

            double dbl = getInwardBean().getPaidValue(getBill().getPatientEncounter());

            if (dbl < getBill().getNetTotal()) {
                JsfUtil.addErrorMessage("This Bht has No Enough Vallue To Cancel");
            }

//            if (getBill().getPatientEncounter().isPaymentFinalized()) {
//                JsfUtil.addErrorMessage("Final Payment is Finalized You can't Cancel");
//                return;
//            }
            CancelledBill cb = createCancelDepositBill();
            //Copy & paste

            getBillFacade().create(cb);
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit((BilledBill) getBill());

            getBillBean().updateInwardDipositList(getBill().getPatientEncounter(), cb);

            List<Payment> payments = paymentService.createPayment(cb, paymentMethodData);
            paymentService.updateBalances(payments);

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                getInwardBean().updateFinalFill(getBill().getPatientEncounter());
                if (getBill().getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
                    getInwardBean().updateCreditDetail(getBill().getPatientEncounter(), getBill().getPatientEncounter().getFinalBill().getNetTotal());
                }

            }

            WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }

    }

    public void cancelDepositBillPayment() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            if (check()) {
                return;
            }

            if (getBill().getCheckedBy() != null) {
                JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
                return;
            }

            if (getBill().isRefunded()) {
                JsfUtil.addErrorMessage("This bill has already been refunded and cannot be cancelled.");
                return;
            }

            double dbl = getInwardBean().getPaidValue(getBill().getPatientEncounter());

            if (dbl < getBill().getNetTotal()) {
                JsfUtil.addErrorMessage("This Bht has No Enough Vallue To Cancel");
            }

//            if (getBill().getPatientEncounter().isPaymentFinalized()) {
//                JsfUtil.addErrorMessage("Final Payment is Finalized You can't Cancel");
//                return;
//            }
            CancelledBill cb = createCancelDepositBill();
            //Copy & paste

            getBillFacade().create(cb);
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit((BilledBill) getBill());

            getBillBean().updateInwardDipositList(getBill().getPatientEncounter(), cb);

            paymentService.createPayment(
                cb,
                cb.getPaymentMethod(), 
                paymentMethodData, 
                sessionController.getInstitution(), 
                sessionController.getDepartment(),
                sessionController.getLoggedUser());

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                getInwardBean().updateFinalFill(getBill().getPatientEncounter());
                if (getBill().getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
                    getInwardBean().updateCreditDetail(getBill().getPatientEncounter(), getBill().getPatientEncounter().getFinalBill().getNetTotal());
                }

            }

            WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }

    }

    public void cancelInwardDepositBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            if (check()) {
                return;
            }

            if (getBill().getCheckedBy() != null) {
                JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
                return;
            }

            if (getBill().isRefunded()) {
                JsfUtil.addErrorMessage("This bill has already been refunded and cannot be cancelled.");
                return;
            }

            double dbl = getInwardBean().getPaidValue(getBill().getPatientEncounter());

            if (dbl < getBill().getNetTotal()) {
                JsfUtil.addErrorMessage("This Bht has No Enough Vallue To Cancel");
                return;
            }

            CancelledBill cb = createCancelInwardDepositBill();

            getBillFacade().create(cb);
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit((BilledBill) getBill());

            getBillBean().updateInwardDipositList(getBill().getPatientEncounter(), cb);

            paymentService.createPayment(
                cb,
                cb.getPaymentMethod(),
                paymentMethodData,
                sessionController.getInstitution(),
                sessionController.getDepartment(),
                sessionController.getLoggedUser());

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                getInwardBean().updateFinalFill(getBill().getPatientEncounter());
                if (getBill().getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
                    getInwardBean().updateCreditDetail(getBill().getPatientEncounter(), getBill().getPatientEncounter().getFinalBill().getNetTotal());
                }

            }

            WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void cancelBillRefund() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            if (check()) {
                return;
            }

            if (getBill().getCheckedBy() != null) {
                JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
                return;
            }

            if (getBill().isCancelled()) {
                JsfUtil.addErrorMessage("This bill has already been cancelled.");
                return;
            }

//            if (getBill().getPatientEncounter().isPaymentFinalized()) {
//                JsfUtil.addErrorMessage("Final Payment is Finalized You can't Cancel");
//                return;
//            }
            RefundBill cb = createRefundCancelBill();
            //Copy & paste
            getBillFacade().create(cb);
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());

            List<Payment> payments = paymentService.createPayment(cb, paymentMethodData);
            paymentService.updateBalances(payments);

            getBillBean().updateInwardDipositList(getBill().getPatientEncounter(), cb);

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                getInwardBean().updateFinalFill(getBill().getPatientEncounter());
                if (getBill().getPatientEncounter().getPaymentMethod() == PaymentMethod.Credit) {
                    getInwardBean().updateCreditDetail(getBill().getPatientEncounter(), getBill().getPatientEncounter().getFinalBill().getNetTotal());
                }

            }

            WebUser wb = getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }

    }

    public boolean cancelBillPayment(Bill bill) {
        if (bill != null && bill.getId() != null && bill.getId() != 0) {

            if (check()) {
                return true;
            }

            CancelledBill cb = createCancelBill();
            //Copy & paste
            getBillFacade().create(cb);
            cancelBillItems(cb);
            bill.setCancelled(true);
            bill.setCancelledBill(cb);
            getBillFacade().edit((BilledBill) bill);

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return true;
        }

        return false;
    }

    public void cancelFinalBillPayment() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            long dayCount = CommonFunctions.getDayCount(getBill().getCreatedAt(), new Date());
            boolean disableTimeLimit = configOptionApplicationController.getBooleanValueByKey("Disable Time Limit on Final Bill Cancellation", false);
            boolean hasPrivilege = getWebUserController().hasPrivilege("InwardFinalBillCancel");

            // Skip time check if both conditions are true: time limit is disabled AND user has privilege
            if (!disableTimeLimit && Math.abs(dayCount) > 3 && !hasPrivilege) {
                JsfUtil.addErrorMessage("You can't cancel bills older than 3 days without special privileges.");
                return;
            }

            if (getBill().isCancelled()) {
                JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
                return;
            }
            if (getBill().isRefunded()) {
                JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
                return;
            }

            if (getBill().getPatientEncounter() == null) {
                JsfUtil.addErrorMessage("You can't cancel Because this Bill has no BHT");
                return;
            }

            if (getComment() == null || getComment().trim().equals("")) {
                JsfUtil.addErrorMessage("Please enter a comment");
                return;
            }

            boolean wasConfirmedFinalBill = getBill().isConfirmedFinalBill();

            CancelledBill cb = createCancelBill();
            //Copy & paste
            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBill().setConfirmedFinalBill(false);
            getBillFacade().edit((BilledBill) getBill());

            // Only the cancellation of the *confirmed* version affects the encounter's
            // totals/paymentFinalized/finalBill — cancelling an already-superseded,
            // non-confirmed version must leave the confirmed version's state untouched.
            if (wasConfirmedFinalBill) {
                List<Bill> remainingVersions = fetchFinalBillVersions(getBill().getPatientEncounter());
                List<Bill> nonCancelled = new ArrayList<>();
                if (remainingVersions != null) {
                    for (Bill v : remainingVersions) {
                        if (!v.isCancelled()) {
                            nonCancelled.add(v);
                        }
                    }
                }
                if (nonCancelled.isEmpty()) {
                    getBill().getPatientEncounter().setGrantTotal(0);
                    getBill().getPatientEncounter().setDiscount(0);
                    getBill().getPatientEncounter().setNetTotal(0);
                    getBill().getPatientEncounter().setAdjustedTotal(0);
                    getBill().getPatientEncounter().setPaymentFinalized(false);
                    getBill().getPatientEncounter().setCreditUsedAmount(0);
                    getBill().getPatientEncounter().setFinalBill(null);
                    getPatientEncounterFacade().edit(getBill().getPatientEncounter());
                } else {
                    Bill nextConfirmed = nonCancelled.get(0);
                    for (Bill v : nonCancelled) {
                        if (v.getFinalBillVersionSerial() != null
                                && (nextConfirmed.getFinalBillVersionSerial() == null
                                || v.getFinalBillVersionSerial() > nextConfirmed.getFinalBillVersionSerial())) {
                            nextConfirmed = v;
                        }
                    }
                    // nextConfirmed was loaded by a separate query, so its patientEncounter
                    // association is a distinct (though same-row) managed instance from
                    // getBill().getPatientEncounter(). Force identity before promoting so
                    // setAsConfirmedFinalBillInternal's edit() is the single, authoritative
                    // persist for this encounter — otherwise a second edit() on the stale
                    // getBill().getPatientEncounter() instance would silently clobber the
                    // finalBill repoint this call just made.
                    nextConfirmed.setPatientEncounter(getBill().getPatientEncounter());
                    setAsConfirmedFinalBillInternal(nextConfirmed);
                }
            } else {
                getPatientEncounterFacade().edit(getBill().getPatientEncounter());
            }

            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }

    }

    public void cancelProvisionalBillPayment() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            long dayCount = CommonFunctions.getDayCount(getBill().getCreatedAt(), new Date());
            boolean disableTimeLimit = configOptionApplicationController.getBooleanValueByKey("Disable Time Limit on Provisional Bill Cancellation", false);
            boolean hasPrivilege = true;

            // Skip time check if both conditions are true: time limit is disabled AND user has privilege
            if (!disableTimeLimit && Math.abs(dayCount) > 3 && !hasPrivilege) {
                JsfUtil.addErrorMessage("You can't cancel bills older than 3 days without special privileges.");
                return;
            }

            if (getBill().isCancelled()) {
                JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
                return;
            }
            if (getBill().isRefunded()) {
                JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
                return;
            }

            if (getBill().getPatientEncounter() == null) {
                JsfUtil.addErrorMessage("You can't cancel Because this Bill has no BHT");
                return;
            }

            if (getComment() == null || getComment().trim().equals("")) {
                JsfUtil.addErrorMessage("Please enter a comment");
                return;
            }

            CancelledBill cb = createCancelBill();
            cb.setBillTypeAtomic(BillTypeAtomic.INWARD_PROVISIONAL_BILL_CANCELLATION);
            //Copy & paste
            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit((BilledBill) getBill());

//            getBill().getPatientEncounter().setGrantTotal(0);
//            getBill().getPatientEncounter().setDiscount(0);
//            getBill().getPatientEncounter().setNetTotal(0);
//            getBill().getPatientEncounter().setAdjustedTotal(0);
//            getBill().getPatientEncounter().setPaymentFinalized(false);
//            getBill().getPatientEncounter().setCreditUsedAmount(0);
//            getPatientEncounterFacade().edit(getBill().getPatientEncounter());
            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }

    }

    private boolean checkBathcReferenceBill() {
        String sql = "select b from BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.forwardReferenceBill=:bil "
                + " and b.surgeryBillType!=:sbt ";
        HashMap hm = new HashMap();
        hm.put("sbt", SurgeryBillType.TimedService);
        hm.put("bil", getBill());
        ////// // System.out.println("getBillFacade().findFirstByJpql(sql, hm) = " + getBillFacade().findFirstByJpql(sql, hm));
        Bill b = getBillFacade().findFirstByJpql(sql, hm);
        if (b == null && checkBathcReferenceBillTimeService()) {
            return false;
        } else {
            if (b != null) {
            }
            return true;
        }
    }

    public String getRowStyleClass(BillItem bip) {
        if (bip.getNetValue() != 0) {
            return "non-zero-value-row";
        }
        return null; // Return null for rows with netValue equal to 0
    }

    public boolean checkBathcReferenceBillTimeService() {
        String sql = "select b from BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.forwardReferenceBill=:bil "
                + " and b.surgeryBillType=:sbt ";
        HashMap hm = new HashMap();
        hm.put("sbt", SurgeryBillType.TimedService);
        hm.put("bil", getBill());

        List<Bill> bs = getBillFacade().findByJpql(sql, hm);
        ////// // System.out.println("bs = " + bs);
        for (Bill b : bs) {
            List<EncounterComponent> enc = getBillBean().getEncounterComponents(b);
            ////// // System.out.println("enc = " + enc);
            for (EncounterComponent e : enc) {
                ////// // System.out.println("e = " + e);
                ////// // System.out.println("e.getBillFee().getPatientItem().isRetired() = " + e.getBillFee().getPatientItem().isRetired());
                if (!e.getBillFee().getPatientItem().isRetired()) {
                    return false;
                }
            }

        }

        return true;
    }

    public void cancelSurgeryBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

//            if (check()) {
//                return;
//            }
            if (checkBathcReferenceBill()) {
                JsfUtil.addErrorMessage("There is some bills refering this Surgery .Cancel those bills first");
                return;
            }
            if (getBill().getPatientEncounter() != null && getBill().getPatientEncounter().isDischarged()) {
                JsfUtil.addErrorMessage("Sorry, patient is discharged.");
                return;
            }

            CancelledBill cb = createCancelBill();
            //Copy & paste
            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
//            only cancell the sergery bill
//            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit((BilledBill) getBill());

            JsfUtil.addSuccessMessage("Cancelled");

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }

    }

    private CancelledBill createCancelBill() {
        CancelledBill cb = new CancelledBill();
        cb.copy(getBill());
        cb.invertAndAssignValuesFromOtherBill(getBill());
        cb.setBilledBill(getBill());

        ////////////
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setComments(comment);
        cb.setPaymentMethod(paymentMethod);
        //TODO: Find null Point Exception

        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        cb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getBill().getBillType(), BillClassType.CancelledBill, BillNumberSuffix.INWCAN));
        cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getBillType(), BillClassType.CancelledBill, BillNumberSuffix.INWCAN));
//        cb.setBillType(BillType.InwardProfessional);
        cb.setBillTypeAtomic(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
        return cb;
    }

    private CancelledBill createCancelDepositBill() {
        CancelledBill cb = new CancelledBill();
        cb.copy(getBill());
        cb.invertAndAssignValuesFromOtherBill(getBill());
        cb.setBilledBill(getBill());
        if (getBill().getPatient() != null) {
            cb.setPatient(getBill().getPatient());
        } else if (getBill().getPatientEncounter() != null) {
            cb.setPatient(getBill().getPatientEncounter().getPatient());
        }

        ////////////
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setComments(comment);
        cb.setPaymentMethod(paymentMethod);
        //TODO: Find null Point Exception

        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

//        cb.setBillType(BillType.InwardProfessional);
        cb.setBillTypeAtomic(BillTypeAtomic.INWARD_PAYMENT_CANCELLATION);

        AdmissionType admissionTypeForBillNumber = getBill().getPatientEncounter() != null
                ? getBill().getPatientEncounter().getAdmissionType() : null;
        boolean uniqueSerialPerAdmissionType = admissionTypeForBillNumber != null
                && configOptionApplicationController.getBooleanValueByKey(
                        "Bill Number Generation Strategy - Unique Serial Per Admission Type for Inward Payments", false);
        if (uniqueSerialPerAdmissionType) {
            cb.setDeptId(getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), cb.getBillTypeAtomic(), admissionTypeForBillNumber));
            cb.setInsId(getBillNumberBean().institutionBillNumberGeneratorYearly(getSessionController().getInstitution(), cb.getBillTypeAtomic(), admissionTypeForBillNumber));
        } else {
            cb.setDeptId(getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), cb.getBillTypeAtomic()));
            cb.setInsId(getBillNumberBean().institutionBillNumberGeneratorYearly(getSessionController().getInstitution(), cb.getBillTypeAtomic()));
        }
        return cb;
    }

    private CancelledBill createCancelInwardDepositBill() {
        CancelledBill cb = new CancelledBill();
        cb.copy(getBill());
        cb.invertAndAssignValuesFromOtherBill(getBill());
        cb.setBilledBill(getBill());
        if (getBill().getPatient() != null) {
            cb.setPatient(getBill().getPatient());
        } else if (getBill().getPatientEncounter() != null) {
            cb.setPatient(getBill().getPatientEncounter().getPatient());
        }

        ////////////
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setComments(comment);
        cb.setPaymentMethod(paymentMethod);
        //TODO: Find null Point Exception

        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        cb.setBillTypeAtomic(BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION);

        AdmissionType admissionTypeForBillNumber = getBill().getPatientEncounter() != null
                ? getBill().getPatientEncounter().getAdmissionType() : null;
        boolean uniqueSerialPerAdmissionType = admissionTypeForBillNumber != null
                && configOptionApplicationController.getBooleanValueByKey(
                        "Bill Number Generation Strategy - Unique Serial Per Admission Type for Inward Payments", false);
        if (uniqueSerialPerAdmissionType) {
            cb.setDeptId(getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), cb.getBillTypeAtomic(), admissionTypeForBillNumber));
            cb.setInsId(getBillNumberBean().institutionBillNumberGeneratorYearly(getSessionController().getInstitution(), cb.getBillTypeAtomic(), admissionTypeForBillNumber));
        } else {
            cb.setDeptId(getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), cb.getBillTypeAtomic()));
            cb.setInsId(getBillNumberBean().institutionBillNumberGeneratorYearly(getSessionController().getInstitution(), cb.getBillTypeAtomic()));
        }

        return cb;
    }

    public void listnerForPaymentMethodChange(Bill b) {
        if (getPaymentMethod() == PaymentMethod.PatientDeposit) {
            getPaymentMethodData().getPatient_deposit().setPatient(b.getPatientEncounter().getPatient());
            getPaymentMethodData().getPatient_deposit().setTotalValue(b.getTotal());
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(b.getPatientEncounter().getPatient(), sessionController.getDepartment());
            if (pd != null && pd.getId() != null) {
                getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
            }
        } else if (getPaymentMethod() == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(b.getTotal());
        } else if (getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            getPaymentMethodData().getPatient_deposit().setPatient(b.getPatientEncounter().getPatient());
//            getPaymentMethodData().getPatient_deposit().setTotalValue(calculatRemainForMultiplePaymentTotal());
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(b.getPatientEncounter().getPatient(), sessionController.getDepartment());

            if (pd != null && pd.getId() != null) {
                boolean hasPatientDeposit = false;
                for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                        hasPatientDeposit = true;
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(b.getPatientEncounter().getPatient());
                        cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

                    }
                }
            }

        }
    }

    private RefundBill createRefundCancelBill() {
        RefundBill cb = new RefundBill();
        cb.invertQty();
        cb.copy(getBill());
        cb.setRefundedBill(getBill());
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setPaymentMethod(getPaymentMethod());
        cb.setComments(comment);
        //TODO: Find null Point Exception

        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        BillTypeAtomic cancelAtomic = getBill().getBillTypeAtomic() == BillTypeAtomic.INWARD_DEPOSIT_REFUND
                ? BillTypeAtomic.INWARD_DEPOSIT_REFUND_CANCELLATION
                : BillTypeAtomic.INWARD_PAYMENT_REFUND_CANCELLATION;
        cb.setBillTypeAtomic(cancelAtomic);

        AdmissionType admissionTypeForBillNumber = getBill().getPatientEncounter() != null
                ? getBill().getPatientEncounter().getAdmissionType() : null;
        boolean uniqueSerialPerAdmissionType = admissionTypeForBillNumber != null
                && configOptionApplicationController.getBooleanValueByKey(
                        "Bill Number Generation Strategy - Unique Serial Per Admission Type for Inward Payments", false);
        if (uniqueSerialPerAdmissionType) {
            cb.setDeptId(getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), cancelAtomic, admissionTypeForBillNumber));
            cb.setInsId(getBillNumberBean().institutionBillNumberGeneratorYearly(getSessionController().getInstitution(), cancelAtomic, admissionTypeForBillNumber));
        } else {
            cb.setDeptId(getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), cancelAtomic));
            cb.setInsId(getBillNumberBean().institutionBillNumberGeneratorYearly(getSessionController().getInstitution(), cancelAtomic));
        }

        cb.invertAndAssignValuesFromOtherBill(getBill());
        return cb;
    }

    public void cancelProfessional() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            if (getBill().getCheckedBy() != null) {
                JsfUtil.addErrorMessage("Checked Bill. Can not cancel");
                return;
            }

            if (getBill().isCancelled()) {
                JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
                return;
            }
            if (getBill().isRefunded()) {
                JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
                return;
            }

            if (getBill().getPatientEncounter() == null) {
                JsfUtil.addErrorMessage("You can't cancel Because this Bill has no BHT");
                return;
            }

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                JsfUtil.addErrorMessage("Final Payment is Finalized You can't Cancel");
                return;
            }

            if (getBill().getPatientEncounter().isDischarged()) {
                JsfUtil.addErrorMessage("Sorry, patient is discharged.");
                return;
            }

            if (checkPaid()) {
                JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
                return;
            }

            CancelledBill cb = createCancelBill();

            //Copy & paste
            if (cb.getId() == null) {
                getBillFacade().create(cb);
            }
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit((BilledBill) getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            WebUser wb = getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }

    }

    List<Bill> billsToApproveCancellation;
    List<Bill> billsApproving;
    private CancelledBill billForCancel;

    public void approveCancellation() {

        if (billsApproving == null) {
            JsfUtil.addErrorMessage("Select Bill to Approve Cancell");
            return;
        }
        for (Bill b : billsApproving) {

            b.setApproveUser(getSessionController().getCurrent());
            b.setApproveAt(Calendar.getInstance().getTime());

            if (b.getId() == null) {
                getBillFacade().create(b);
            }

            cancelBillItems(b);
            b.getBilledBill().setCancelled(true);
            b.getBilledBill().setCancelledBill(b);

            getBillFacade().edit((BilledBill) getBill());

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

            if (b.getId() == null) {
                getBillItemFacede().create(b);
            }

            cancelBillComponents(can, b);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);

            cancelBillFee(can, b, tmp);

        }
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

    public List<Payment> createPayment(Bill bill, PaymentMethod pm) {
        List<Payment> pays = new ArrayList<>();
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, pm);
        pays.add(p);
        return pays;
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

    public void cancelPaymentBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (errorCheck()) {
                return;
            }
            if (paymentMethod == PaymentMethod.Cash) {
                Drawer userDrawer = drawerService.getUsersDrawer(sessionController.getLoggedUser());
                double drawerBalance = userDrawer.getCashInHandValue();
                double paymentAmount = getBill().getNetTotal();
                if (configOptionApplicationController.getBooleanValueByKey("Enable Drawer Manegment", true)) {
                    if (drawerBalance < paymentAmount) {
                        JsfUtil.addErrorMessage("Not enough cash in your drawer to make this payment");
                        return;
                    }
                }
            }
            CancelledBill cb = createCancelBill();
            //Copy & paste

            getBillFacade().create(cb);
            cancelBillItemsPayment(cb);
            cancelPaymentItems(bill);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            List<Payment> cancelPayment = createPayment(cb, cb.getPaymentMethod());
            drawerController.updateDrawerForIns(cancelPayment);
            JsfUtil.addSuccessMessage("Cancelled");

            WebUser wb = getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }
    }

    private void cancelPaymentItems(Bill pb) {
        List<BillItem> pbis;
        pbis = getBillItemFacede().findByJpql("SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + pb.getId());
        for (BillItem pbi : pbis) {
            if (pbi.getPaidForBillFee() != null) {
                pbi.getPaidForBillFee().setPaidValue(0.0);
                getBillFeeFacade().edit(pbi.getPaidForBillFee());
            }
        }
    }

    private void cancelBillFee(Bill can, BillItem bt, List<BillFee> tmp) {
        if (tmp == null) {
            return;
        }
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.copy(nB);
            bf.invertValue(nB);

            bf.setBill(can);
            bf.setBillItem(bt);
            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            if (bf.getId() == null) {
                getBillFeeFacade().create(bf);
            }
        }
    }

    private void cancelBillItemsPayment(Bill can) {
        for (BillItem nB : getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);
            b.setItem(nB.getItem());

            b.setNetValue(0 - nB.getNetValue());
            b.setGrossValue(0 - nB.getGrossValue());
            b.setRate(0 - nB.getRate());

            b.setCatId(nB.getCatId());
            b.setDeptId(nB.getDeptId());
            b.setInsId(nB.getInsId());
            b.setDiscount(nB.getDiscount());
            b.setQty(1.0);
            b.setRate(nB.getRate());

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            b.setPaidForBillFee(nB.getPaidForBillFee());

            ////// // System.out.println("nB.getPaidForBillFee() = " + nB.getPaidForBillFee());
            getBillItemFacede().create(b);

            cancelBillComponents(can, b);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);

            cancelBillFee(can, b, tmp);

        }
    }

    private void cancelBillFee(Bill can, BillItem bt) {
        for (BillFee nB : getBillFees()) {
            BillFee bf = new BillFee();
            bf.copy(nB);
            bf.invertValue(nB);

            bf.setBill(can);
            bf.setBillItem(bt);
            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            if (bf.getId() == null) {
                getBillFeeFacade().create(bf);
            }
        }
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public Bill getBill() {
        //recreateModel();
        if (bill == null) {
            bill = new BilledBill();
        }
        return bill;
    }

    public Long getAppointmentReceiptBillId() {
        return appointmentReceiptBillId;
    }

    public void setAppointmentReceiptBillId(Long appointmentReceiptBillId) {
        this.appointmentReceiptBillId = appointmentReceiptBillId;
        this.appointmentBillReceipt = null;
    }

    /**
     * DTO for {@code /inward/inward_view_appointment_bill_receipt} — null
     * until {@link #appointmentReceiptBillId} is set by the BillSearch
     * routing case. Lazily loaded and cached for the lifetime of this bean
     * (the composite receipt templates dereference it many times per
     * render); the cache is cleared by {@link #setAppointmentReceiptBillId}.
     */
    public InwardBillReceiptDTO getAppointmentBillReceipt() {
        if (appointmentReceiptBillId == null) {
            return null;
        }
        if (appointmentBillReceipt == null) {
            appointmentBillReceipt = billFacade.findInwardBillReceiptDTO(appointmentReceiptBillId);
        }
        return appointmentBillReceipt;
    }

    public void markAsChecked() {
        Bill b = bill;
        if (b == null) {
            return;
        }

        if (b.getPatientEncounter() == null) {
            return;
        }

        if (b.getPatientEncounter().isPaymentFinalized()) {
            return;
        }

        b.setCheckeAt(new Date());
        b.setCheckedBy(getSessionController().getLoggedUser());

        getBillFacade().edit(b);

        JsfUtil.addSuccessMessage("Successfully Cheked");
    }

    public void markAsUnChecked() {
        if (bill == null) {
            return;
        }

        if (bill.getPatientEncounter() == null) {
            return;
        }

        if (bill.getPatientEncounter().isPaymentFinalized()) {
            return;
        }

        bill.setCheckeAt(null);
        bill.setCheckedBy(null);

        getBillFacade().edit(bill);

        JsfUtil.addErrorMessage("Successfully Cheked");
    }

    public void updateBillComments() {
        if (bill == null || bill.getId() == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return;
        }
        getBillFacade().edit(bill);
        JsfUtil.addSuccessMessage("Comment Updated");
    }

    public void selectBillItem(BillItem billItem) {
        makeNull();
        BillItem tmp = billItemFacede.find(billItem.getId());
        bill = tmp.getBill();
    }

    public void setBill(Bill bill) {
        recreateModel();
        if (bill == null) {
            return;
        }
        this.bill = billFacade.find(bill.getId());
        paymentMethod = bill.getPaymentMethod();

    }

    public void setBillActionListener(String id) {
        setBill(bill);
    }

    public List<BillEntry> getBillEntrys() {
        return billEntrys;
    }

    public void setBillEntrys(List<BillEntry> billEntrys) {
        this.billEntrys = billEntrys;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            HashMap hm = new HashMap();
            String sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill=:b ";
            hm.put("b", getBill());
            billItems = getBillItemFacede().findByJpql(sql, hm);
            if (billItems == null) {
                billItems = new ArrayList<>();
            }
        }

        return billItems;
    }

    public List<BillComponent> getBillComponents() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billComponents = getBillCommponentFacade().findByJpql(sql);
            if (billComponents == null) {
                billComponents = new ArrayList<>();
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
                    billFees = new ArrayList<>();
                }
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
    public InwardSearch() {
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

    public double calTot() {
        if (getBillFees() == null) {
            return 0.0;
        }
        double tot = 0.0;
        for (BillFee f : getBillFees()) {
            //////// // System.out.println("Tot" + f.getFeeValue());
            tot += f.getFeeValue();
        }

        return tot;
    }

    public void changeIsMade() {
        changed = true;
    }

    public void refreshBill() {
        changed = false;
    }

    public void updateTotal() {

        double grantTotal = 0.0;

        for (BillItem bi : bill.getBillItems()) {
            grantTotal += bi.getAdjustedValue();
        }

        bill.setGrantTotal(grantTotal);
        bill.setNetTotal(grantTotal - bill.getDiscount());
        changed = true;

    }

    public void updateProTotal(BillItem bi) {

        double totalDr = 0.0;

        for (BillFee bf : bi.getProFees()) {
            if (bf.getFeeAdjusted() != 0) {
                totalDr += bf.getFeeAdjusted();
            } else {
                totalDr += bf.getFeeValue();
            }
        }

        bi.setAdjustedValue(totalDr);

        updateTotal();
    }

    public void saveProvisionalBill(Bill b) {
        b.setEditor(sessionController.getLoggedUser());
        b.setEditedAt(new Date());
        billFacade.edit(b);
        for (BillItem bi : b.getBillItems()) {
            billItemFacede.edit(bi);
            if (bi.getProFees() != null && !bi.getProFees().isEmpty()) {
                for (BillFee bf : bi.getProFees()) {
                    billFeeFacade.edit(bf);
                }
            }
        }
        JsfUtil.addSuccessMessage("Provisional Bill Saved");
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public CancelledBill getBillForCancel() {
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
        bills = null;
        this.toDate = toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        bills = null;
        this.fromDate = fromDate;
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

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
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

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    public EncounterComponentFacade getEncounterComponentFacade() {
        return encounterComponentFacade;
    }

    public void setEncounterComponentFacade(EncounterComponentFacade encounterComponentFacade) {
        this.encounterComponentFacade = encounterComponentFacade;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public Admission getAdmission() {
        return admission;
    }

    public void setAdmission(Admission admission) {
        this.admission = admission;
    }

    public boolean isWithProfessionalFee() {
        return withProfessionalFee;
    }

    public void setWithProfessionalFee(boolean withProfessionalFee) {
        this.withProfessionalFee = withProfessionalFee;
    }

    public boolean isShowOrginalBill() {
        return showOrginalBill;
    }

    public void setShowOrginalBill(boolean showOrginalBill) {
        this.showOrginalBill = showOrginalBill;
    }

    public boolean isShowZeroInwardChargeCategoryTypes() {
        return showZeroInwardChargeCategoryTypes;
    }

    public void setShowZeroInwardChargeCategoryTypes(boolean showZeroInwardChargeCategoryTypes) {
        this.showZeroInwardChargeCategoryTypes = showZeroInwardChargeCategoryTypes;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public Doctor getReferredBy() {
        return referredBy;
    }

    public void setReferredBy(Doctor referredBy) {
        this.referredBy = referredBy;
    }

    public Request getCurrentRequest() {
        return currentRequest;
    }

    public void setCurrentRequest(Request currentRequest) {
        this.currentRequest = currentRequest;
    }

    public List<PaymentMethod> getInwardDepositCancelationPaymentMethods() {
        if(inwardDepositCancelationPaymentMethods == null){
            inwardDepositCancelationPaymentMethods = new ArrayList<>();
        }
        return inwardDepositCancelationPaymentMethods;
    }

    public void setInwardDepositCancelationPaymentMethods(List<PaymentMethod> inwardDepositCancelationPaymentMethods) {
        this.inwardDepositCancelationPaymentMethods = inwardDepositCancelationPaymentMethods;
    }

    public PaymentMethodData getOriginalBillPaymentMethodData() {
        return originalBillPaymentMethodData;
    }

    public void setOriginalBillPaymentMethodData(PaymentMethodData originalBillPaymentMethodData) {
        this.originalBillPaymentMethodData = originalBillPaymentMethodData;
    }

}
