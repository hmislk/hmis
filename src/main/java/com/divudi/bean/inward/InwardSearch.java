/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.common.WebUserController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.inward.SurgeryBillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.EjbApplication;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Person;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.inward.EncounterComponent;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.EncounterComponentFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

    private CommonFunctions commonFunctions;

    /**
     * JSF Controllers
     */
    @Inject
    private BillBeanController billBean;
    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    BhtSummeryFinalizedController bhtSummeryFinalizedController;
    @Inject
    SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    PatientInvestigationController patientInvestigationController;
    @Inject
    SearchController searchController;
    @EJB
    PersonFacade personFacade;
    /**
     * Properties
     */
    private Bill bill;
    private boolean printPreview = false;
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
    /////////////////////

    PaymentMethod paymentMethod;

    ReportKeyWord reportKeyWord;

    private YearMonthDay yearMonthDay;
    Patient patient;
    Sex[] sex;
    private Admission admission;

    private boolean withProfessionalFee = false;

    public boolean showProfessionalFee() {
        if (withProfessionalFee == true) {
            withProfessionalFee = false;
        } else {
            withProfessionalFee = true;
        }
        return withProfessionalFee;
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

        String jpql;
        Map temMap = new HashMap();
        jpql = "select b from Bill b where"
                + " b.billType = :billType and "
                + " b.retired=false ";

        jpql += " and  b.patientEncounter=:pe ";
        temMap.put("pe", admission);

        temMap.put("billType", BillType.InwardFinalBill);
        jpql += " order by b.id desc ";

        // bill = getBillFacade().findFirstByJpql(jpql, temMap, TemporalType.TIMESTAMP);
        bill = getBillFacade().findFirstByJpql(jpql, temMap);

        if (bill == null) {
            JsfUtil.addErrorMessage("No Final Bill Created");
            return "";
        }
        withProfessionalFee = false;

        return "/inward/inward_reprint_bill_final?faces-redirect=true";
    }

    public String navigateDoctorPayment() {
        return "/inward/inward_bill_payment?faces-redirect=true";
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

            if (i.isRefunded() == null) {
                d = d + i.getNetValue();
                getTempbillItems().add(i);
            }

        }

        return true;
    }
    
    public String navigateToProfessionalFeeList(){
        return "/inward/inward_search_professional_estimate?faces-redirect=true";
    }

    public void dateChangeListen() {
        getBill().getPatient().getPerson().setDob(getCommonFunctions().guessDob(yearMonthDay));
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
            JsfUtil.addErrorMessage("U cant cancel Because this Bill has no BHT");
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
                JsfUtil.addErrorMessage("U cant cancel Because this Bill has no BHT");
                return;
            }

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                JsfUtil.addErrorMessage("Final Payment is Finalized You can't Cancel");
                return;
            }

            if (checkPaid()) {
                JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
                return;
            }

            if (checkInvestigation()) {
                JsfUtil.addErrorMessage("Lab Report was already Entered .you cant Cancel");
                return;
            }

            if (!getWebUserController().hasPrivilege("LabBillCancelSpecial")) {

                ////// // System.out.println("patientInvestigationController.sampledForAnyItemInTheBill(bill) = " + patientInvestigationController.sampledForAnyItemInTheBill(bill));
                if (patientInvestigationController.sampledForAnyItemInTheBill(getBill())) {
                    JsfUtil.addErrorMessage("Sample Already collected can't cancel");
                    return;
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
            //To null payment methord
            getBill().setPaymentMethod(null);
            cb.setPaymentMethod(null);

            getBillFacade().edit(cb);
            getBillFacade().edit((BilledBill) getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            getBillBean().updateBatchBill(getBill().getForwardReferenceBill());

            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
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
            CancelledBill cb = createCancelBill();
            //Copy & paste

            getBillFacade().create(cb);
            cancelBillItems(cb);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit((BilledBill) getBill());

            getBillBean().updateInwardDipositList(getBill().getPatientEncounter(), cb);

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

            long dayCount = getCommonFunctions().getDayCount(getBill().getCreatedAt(), new Date());

            if (Math.abs(dayCount) > 3 && !getWebUserController().hasPrivilege("InwardFinalBillCancel")) {
                JsfUtil.addErrorMessage("You can't Cancell Two days Old Bill Sory .com");
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
                JsfUtil.addErrorMessage("U cant cancel Because this Bill has no BHT");
                return;
            }

            if (getComment() == null || getComment().trim().equals("")) {
                JsfUtil.addErrorMessage("Please enter a comment");
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

            getBill().getPatientEncounter().setGrantTotal(0);
            getBill().getPatientEncounter().setDiscount(0);
            getBill().getPatientEncounter().setNetTotal(0);
            getBill().getPatientEncounter().setAdjustedTotal(0);
            getBill().getPatientEncounter().setPaymentFinalized(false);
            getBill().getPatientEncounter().setCreditUsedAmount(0);
            getPatientEncounterFacade().edit(getBill().getPatientEncounter());

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
        cb.invertValue(getBill());
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

        return cb;
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

        cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), getBill().getBillType(), BillClassType.RefundBill, BillNumberSuffix.INWREFCAN));
        cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getBillType(), BillClassType.RefundBill, BillNumberSuffix.INWREFCAN));

        cb.invertValue(getBill());
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
                JsfUtil.addErrorMessage("U cant cancel Because this Bill has no BHT");
                return;
            }

            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                JsfUtil.addErrorMessage("Final Payment is Finalized You can't Cancel");
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
            if (getBill().getSurgeryBillType() != null) {
                retireEncounterComponents();
            }

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

    public void cancelPaymentBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (errorCheck()) {
                return;
            }
            CancelledBill cb = createCancelBill();
            //Copy & paste

            getBillFacade().create(cb);
            cancelBillItemsPayment(cb);
            cancelPaymentItems(bill);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
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
        HashMap hm = new HashMap();
        String sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill=:b ";
        hm.put("b", getBill());
        billItems = getBillItemFacede().findByJpql(sql, hm);
        if (billItems == null) {
            billItems = new ArrayList<>();
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
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        bills = null;
        this.toDate = toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        bills = null;
        this.fromDate = fromDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
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

}
