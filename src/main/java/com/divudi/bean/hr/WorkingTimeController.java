/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.BillController;
import com.divudi.bean.common.FeeController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.OpdPreBillController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.opd.OpdBillController;
import com.divudi.entity.hr.FingerPrintRecord;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.WorkingTime;
import com.divudi.facade.WorkingTimeFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.inward.AdmissionController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Doctor;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Payment;
import com.divudi.entity.Staff;
import com.divudi.entity.inward.Admission;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class WorkingTimeController implements Serializable {

    @EJB
    private WorkingTimeFacade ejbFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    BillNumberGenerator billNumberGenerator;

    private static final long serialVersionUID = 1L;

    @Inject
    SessionController sessionController;
    @Inject
    FingerPrintRecordController fingerPrintRecordController;
    @Inject
    StaffShiftController staffShiftController;
    @Inject
    OpdBillController opdBillController;
    @Inject
    OpdPreBillController opdPreBillController;
    @Inject
    private AdmissionController admissionController;
    @Inject
    BillController billController;
    @Inject
    ItemController itemController;
    @Inject
    FeeController feeController;

    List<WorkingTime> selectedItems;
    private WorkingTime current;
    private List<WorkingTime> items = null;
    String selectText = "";
    private Staff staff;
    private List<Admission> staffAdmissionsForPayments;
    private List<BillFee> staffBillFeesForPayment;
    private Integer staffBillFeesForPaymentCount;
    private Integer admissionCount;
    private List<WorkingTime> staffWorkingTimesForPayment;
    private List<WorkingTime> staffPaymentsCompleted;
    private WorkingTime workingTimeForPayment;
    private Double totalStaffWorkingPayment;
    private Double billFeeValue;
    private Double admissionFeeValue;
    private Double otherFeeValue;
    private Double shiftPaymentValue;
    private Double admissionRate;
    private boolean printPreview;
    Date fromDate;
    Date toDate;
    private PaymentMethod paymentMethod;

    public List<WorkingTime> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from WorkingTime c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public String selectStaffForOpdPayment() {
        if (staff == null) {
            JsfUtil.addErrorMessage("Select staff");
            return "";
        }
        workingTimeForPayment = findLastWorkingTime(staff);
        if (workingTimeForPayment == null) {
            JsfUtil.addErrorMessage("No Work Time Recorded");
            return "";
        }
        if (workingTimeForPayment.getProfessinoalPaymentBill() != null) {
            return toViewOpdPaymentsDone();
        }
        staffAdmissionsForPayments = admissionController.findAdmissions(staff, workingTimeForPayment.getStartRecord().getRecordTimeStamp(), workingTimeForPayment.getEndRecord().getRecordTimeStamp());
        staffBillFeesForPayment = billController.findBillFees(staff, workingTimeForPayment.getStartRecord().getRecordTimeStamp(), workingTimeForPayment.getEndRecord().getRecordTimeStamp());
        calculateStaffPayments();
        return "/opd/pay_doctor?faces-redirect=true";
    }

    public String toViewOpdPaymentsDone() {
        fillDoctorPaymentBills();
        return "/opd/pay_doctor_bills?faces-redirect=true";
    }

    public void fillDoctorPaymentBills() {
        String jpql = "select w "
                + " from WorkingTime w "
                + " where w.retired=:retw "
                + " and w.professinoalPaymentBill.retired=:retb "
                + " and w.professinoalPaymentBill.billDate between :fd and :td "
                + " and w.staffShift.staff=:staff";
        Map params = new HashMap();
        params.put("retw", false);
        params.put("retb", false);
        params.put("fd", getFromDate());
        params.put("td", getToDate());
        params.put("staff", staff);
        staffPaymentsCompleted = getFacade().findByJpql(jpql, params, TemporalType.DATE);
    }

    public void prepareForMakingPaymentForOpdDoctor() {
        staff = null;
        printPreview = false;
        workingTimeForPayment = null;
        staffAdmissionsForPayments = null;
        staffBillFeesForPayment = null;
    }

    public void settleStaffPayments() {
        Bill bill = new BilledBill();
        bill.setBillDate(Calendar.getInstance().getTime());
        bill.setBillTime(Calendar.getInstance().getTime());
        bill.setBillType(BillType.PaymentBill);
        bill.setBillTypeAtomic(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_OPD_SERVICES);
        bill.setCreatedAt(Calendar.getInstance().getTime());
        bill.setCreater(getSessionController().getLoggedUser());
        bill.setDepartment(getSessionController().getLoggedUser().getDepartment());
        String id = billNumberGenerator.departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.PaymentBill, BillClassType.BilledBill, BillNumberSuffix.PROPAY);
        bill.setDeptId(id);
        bill.setInsId(id);
        bill.setDiscount(0.0);
        bill.setDiscountPercent(0.0);
        bill.setInstitution(getSessionController().getLoggedUser().getInstitution());
        bill.setNetTotal(0 - totalStaffWorkingPayment);
        bill.setPaymentMethod(paymentMethod);
        bill.setStaff(staff);
        bill.setToStaff(staff);
        bill.setTotal(0 - totalStaffWorkingPayment);
        bill.setDepartment(sessionController.getDepartment());
        bill.setInstitution(sessionController.getInstitution());
        billController.save(bill);

        Payment paymentForProfessionalPayment = createPayment(bill, paymentMethod);

        createBillFeesAndSave(bill, paymentForProfessionalPayment, staffBillFeesForPayment);

        workingTimeForPayment.setProfessinoalPaymentBill(bill);
        save(workingTimeForPayment);
        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Paid");

    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        p.setPaidValue(bill.getNetTotal());
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

    public void createBillFeesAndSave(Bill b, Payment p, List<BillFee> payingBillFees) {
        int serviceFeeQty=0;
        int admissionQty=0;
//        if (payingBillFees != null) {
//            for (BillFee bf : payingBillFees) {
//                saveBillItemForPaymentBill(b, bf, p);
//                bf.setPaidValue(bf.getFeeValue());
//                bf.setSettleValue(bf.getFeeValue());
//                billFeeFacade.edit(bf);
//            }
//            serviceFeeQty = payingBillFees.size();
//        }
        
        BillItem admiddionFeeItem = new BillItem();
        admiddionFeeItem.setBill(b);
        admiddionFeeItem.setQty((double)admissionCount);
        admiddionFeeItem.setCreatedAt(new Date());
        admiddionFeeItem.setCreater(sessionController.getLoggedUser());
        admiddionFeeItem.setItem(itemController.findAndCreateItemByName("Doctor Payment for Admissions", sessionController.getDepartment()));
        billItemFacade.create(admiddionFeeItem);
        BillFee admissionFee = new BillFee();
        admissionFee.setBillItem(admiddionFeeItem);
        admissionFee.setFeeValue(admissionFeeValue);
        admissionFee.setFee(feeController.findFee("Doctor Payment for Admission Fee"));
        billFeeFacade.create(admissionFee);
        
        System.out.println("admiddionFeeItem = " + admiddionFeeItem);
        System.out.println("admiddionFeeItem.getItem = " + admiddionFeeItem.getItem());
        System.out.println("admissionFee = " + admissionFee);
        System.out.println("admissionFee.getFee = " + admissionFee.getFee());
        System.out.println("admissionFee.getFeeValue = " + admissionFee.getFeeValue());
        
        BillItem serviceBillItem = new BillItem();
        serviceBillItem.setBill(b);
        serviceBillItem.setQty((double)serviceFeeQty );
        serviceBillItem.setCreatedAt(new Date());
        serviceBillItem.setCreater(sessionController.getLoggedUser());
        serviceBillItem.setItem(itemController.findAndCreateItemByName("Doctor Payment for Services", sessionController.getDepartment()));
        billItemFacade.create(serviceBillItem);
        BillFee serviceFee = new BillFee();
        serviceFee.setBillItem(serviceBillItem);
        serviceFee.setFeeValue(billFeeValue);
        serviceFee.setFee(feeController.findFee("Doctor Payment for Services"));
        billFeeFacade.create(serviceFee);
        
        System.out.println("serviceBillItem = " + serviceBillItem);
        System.out.println("serviceBillItem.getItem = " + serviceBillItem.getItem());
        System.out.println("serviceFee = " + serviceFee);
        System.out.println("serviceFee.getFee = " + serviceFee.getFee());
        System.out.println("serviceFee.getFeeValue = " + serviceFee.getFeeValue());
        
        BillItem shiftFeeItem = new BillItem();
        shiftFeeItem.setBill(b);
        shiftFeeItem.setCreatedAt(new Date());
        shiftFeeItem.setCreater(sessionController.getLoggedUser());
        shiftFeeItem.setItem(itemController.findAndCreateItemByName("Doctor Payment for Shift", sessionController.getDepartment()));
        billItemFacade.create(shiftFeeItem);
        BillFee shiftFee = new BillFee();
        shiftFee.setBillItem(shiftFeeItem);
        shiftFee.setFeeValue(shiftPaymentValue);
        shiftFee.setFee(feeController.findFee("Doctor Payment for Shift"));
        billFeeFacade.create(shiftFee);
        
        System.out.println("shiftFeeItem = " + shiftFeeItem);
        System.out.println("shiftFeeItem.getItem = " + shiftFeeItem.getItem());
        System.out.println("shiftFee = " + shiftFee);
        System.out.println("shiftFee.getFee = " + shiftFee.getFee());
        System.out.println("shiftFee.getFeeValue = " + shiftFee.getFeeValue());
        
        BillItem otherFeeItem = new BillItem();
        otherFeeItem.setBill(b);
        otherFeeItem.setCreatedAt(new Date());
        otherFeeItem.setCreater(sessionController.getLoggedUser());
        otherFeeItem.setItem(itemController.findAndCreateItemByName("Doctor Payment for Other", sessionController.getDepartment()));
        billItemFacade.create(otherFeeItem);
        BillFee otherFee = new BillFee();
        otherFee.setBillItem(otherFeeItem);
        otherFee.setFeeValue(otherFeeValue);
        otherFee.setFee(feeController.findFee("Doctor Payment for Other"));
        billFeeFacade.create(otherFee);
        
        System.out.println("otherFeeItem = " + otherFeeItem);
        System.out.println("otherFeeItem.getItem = " + otherFeeItem.getItem());
        System.out.println("otherFee = " + otherFee);
        System.out.println("otherFee.getFee = " + otherFee.getFee());
        System.out.println("otherFee.getFeeValue = " + otherFee.getFeeValue());

        BillItem shiftPaymentItem;
        BillFee shiftPaymentFee;
        BillFee otherFeeFee;

        b.getBillItems().add(admiddionFeeItem);
        b.getBillItems().add(serviceBillItem);
        b.getBillItems().add(shiftFeeItem);
        b.getBillItems().add(otherFeeItem);
        
        System.out.println("b.getBillItems() = " + b.getBillItems());
          
        billController.save(b);

    }
    
    private void saveBillItemForPaymentBill(Bill b, BillFee bf, Payment p) {
        BillItem i = new BillItem();
        i.setReferanceBillItem(bf.getBillItem());
        i.setReferenceBill(bf.getBill());
        i.setPaidForBillFee(bf);
        i.setBill(b);
        i.setCreatedAt(Calendar.getInstance().getTime());
        i.setCreater(getSessionController().getLoggedUser());
        i.setDiscount(0.0);
        i.setGrossValue(bf.getFeeValue());
        i.setNetValue(bf.getFeeValue());
        i.setQty(1.0);
        i.setRate(bf.getFeeValue());
        billItemFacade.create(i);
        saveBillFee(i, p);
        b.getBillItems().add(i);
    }

    public void saveBillFee(BillItem bi, Payment p) {
        BillFee bf = new BillFee();
        bf.setCreatedAt(Calendar.getInstance().getTime());
        bf.setCreater(getSessionController().getLoggedUser());
        bf.setBillItem(bi);
        bf.setPatienEncounter(bi.getBill().getPatientEncounter());
        bf.setPatient(bi.getBill().getPatient());
        bf.setFeeValue(0 - bi.getNetValue());
        bf.setFeeGrossValue(0 - bi.getGrossValue());
        bf.setSettleValue(0 - bi.getNetValue());
        bf.setCreatedAt(new Date());
        bf.setDepartment(getSessionController().getDepartment());
        bf.setInstitution(getSessionController().getInstitution());
        bf.setBill(bi.getBill());

        if (bf.getId() == null) {
            billFeeFacade.create(bf);
        } else {
            billFeeFacade.edit(bf);
        }
        createBillFeePaymentAndPayment(bf, p);
    }

    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(bf.getSettleValue());
        bfp.setInstitution(getSessionController().getInstitution());
        bfp.setDepartment(getSessionController().getDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        billFeePaymentFacade.create(bfp);
    }

    public void calculateStaffPayments() {
        // Initialize Double objects to avoid NullPointerException due to auto-unboxing
        if (otherFeeValue == null) {
            otherFeeValue = 0.0;
        }
        if (shiftPaymentValue == null) {
            shiftPaymentValue = 0.0;
        }

        billFeeValue = 0.0; // Reset or initialize billFeeValue
        if (staffBillFeesForPayment != null) {
            for (BillFee bf : staffBillFeesForPayment) {
                // Check bf is not null and bf.getFeeValue() does not return null
                Double feeValue = bf != null ? bf.getFeeValue() : null;
                if (feeValue != null) {
                    billFeeValue += feeValue;
                }
            }
        }

        if (admissionRate == null) {
            admissionRate = 500.0; // Default value or take properly from admissions
        }

        // Initialize admissionFeeValue based on staffAdmissionsForPayments size and admissionRate
        admissionFeeValue = (staffAdmissionsForPayments != null) ? staffAdmissionsForPayments.size() * admissionRate : 0.0;

        // Calculate totalStaffWorkingPayment considering potential null values
        totalStaffWorkingPayment = (otherFeeValue != null ? otherFeeValue : 0.0)
                + (billFeeValue != null ? billFeeValue : 0.0)
                + (admissionFeeValue != null ? admissionFeeValue : 0.0)
                + (shiftPaymentValue != null ? shiftPaymentValue : 0.0);
    }

    public String navigateToMarkIn() {
        current = new WorkingTime();
        StaffShift staffShift = new StaffShift();
        current.setStaffShift(staffShift);
        FingerPrintRecord sr = new FingerPrintRecord();
        sr.setRecordTimeStamp(new Date());
        current.setStartRecord(sr);
        return "/opd/markIn?faces-redirect=true";
    }

    public String markIn() {
        fingerPrintRecordController.save(current.getStartRecord());
        staffShiftController.save(current.getStaffShift());
        save(current);
        JsfUtil.addSuccessMessage("Marked In");
        opdBillController.reloadCurrentlyWorkingStaff();
        opdPreBillController.reloadCurrentlyWorkingStaff();
        return navigateToListCurrentWorkTimes();
    }

    public String markOut() {
        FingerPrintRecord er = new FingerPrintRecord();
        er.setRecordTimeStamp(new Date());
        fingerPrintRecordController.save(er);
        current.setEndRecord(er);
        save(current);
        opdBillController.reloadCurrentlyWorkingStaff();
        opdPreBillController.reloadCurrentlyWorkingStaff();

        return navigateToListCurrentWorkTimes();
    }

    public String cancel() {
        current.setRetired(true);
        current.setRetiredAt(new Date());
        current.setRetirer(sessionController.getLoggedUser());
        save(current);
        return navigateToListWorkTimes();
    }

    public String navigateToViewWorkTime() {
        String j = "select w "
                + " from WorkingTime w "
                + " where w.retired=:ret ";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findByJpql(j, m);
        return "/opd/workTimes";
    }

    public void saveWorkTime() {
        save(current);
        JsfUtil.addSuccessMessage("Saved");
    }

    public String navigateToListCurrentWorkTimes() {
        items = findCurrentlyActiveWorkingTimes();
        return "/opd/marked_ins_current?faces-redirect=true";
    }

    public List<WorkingTime> findCurrentlyActiveWorkingTimes() {
        String j = "select w "
                + " from WorkingTime w "
                + " where w.retired=:ret "
                + " and w.endRecord is null";
        Map m = new HashMap();
        m.put("ret", false);
        return getFacade().findByJpql(j, m);
    }

    public WorkingTime findLastWorkingTime(Staff staff) {
        String j = "select w "
                + " from WorkingTime w "
                + " where w.retired=:ret "
                + " and w.endRecord is not null "
                + " and w.staffShift.staff=:staff "
                + " order by w.endRecord.id desc";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("staff", staff);
        return getFacade().findFirstByJpql(j, m);
    }

    public String navigateToListWorkTimes() {
        String j = "select w "
                + " from WorkingTime w "
                + " where w.retired=:ret "
                + " and w.endRecord is null";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findByJpql(j, m);
        return "/opd/workTimes?faces-redirect=true";
    }

    public String navigateToPay() {
        prepareForMakingPaymentForOpdDoctor();
        return "/opd/pay_doctor?faces-redirect=true";
    }

    public List<WorkingTime> completeWorkingTime(String qry) {
        List<WorkingTime> a = null;
        if (qry != null) {
            a = getFacade().findByJpql("select c from WorkingTime c where c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        }
        if (a == null) {
            a = new ArrayList<WorkingTime>();
        }
        return a;
    }

    public void prepareAdd() {
        current = new WorkingTime();
    }

    public void setSelectedItems(List<WorkingTime> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void save(WorkingTime t) {
        if (t.getId() != null) {
            getFacade().edit(t);
        } else {
            t.setCreatedAt(new Date());
            t.setCreater(getSessionController().getLoggedUser());
            getFacade().create(t);
        }
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public WorkingTimeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(WorkingTimeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public WorkingTimeController() {
    }

    public WorkingTime getCurrent() {
        if (current == null) {
            current = new WorkingTime();
        }
        return current;
    }

    public void setCurrent(WorkingTime current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private WorkingTimeFacade getFacade() {
        return ejbFacade;
    }

    public List<WorkingTime> getItems() {
        if (items == null) {
            String j;
            j = "select d from WorkingTime d where d.retired=false order by d.name";
            items = getFacade().findByJpql(j);
        }
        return items;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public AdmissionController getAdmissionController() {
        return admissionController;
    }

    public void setAdmissionController(AdmissionController admissionController) {
        this.admissionController = admissionController;
    }

    public List<Admission> getStaffAdmissionsForPayments() {
        return staffAdmissionsForPayments;
    }

    public void setStaffAdmissionsForPayments(List<Admission> staffAdmissionsForPayments) {
        this.staffAdmissionsForPayments = staffAdmissionsForPayments;
    }

    public List<BillFee> getStaffBillFeesForPayment() {
        return staffBillFeesForPayment;
    }

    public void setStaffBillFeesForPayment(List<BillFee> staffBillFeesForPayment) {
        this.staffBillFeesForPayment = staffBillFeesForPayment;
    }

    public List<WorkingTime> getStaffWorkingTimesForPayment() {
        return staffWorkingTimesForPayment;
    }

    public void setStaffWorkingTimesForPayment(List<WorkingTime> staffWorkingTimesForPayment) {
        this.staffWorkingTimesForPayment = staffWorkingTimesForPayment;
    }

    public Double getTotalStaffWorkingPayment() {
        return totalStaffWorkingPayment;
    }

    public void setTotalStaffWorkingPayment(Double totalStaffWorkingPayment) {
        this.totalStaffWorkingPayment = totalStaffWorkingPayment;
    }

    public Double getBillFeeValue() {

        return billFeeValue;
    }

    public void setBillFeeValue(Double billFeeValue) {
        this.billFeeValue = billFeeValue;
    }

    public Double getAdmissionFeeValue() {
        return admissionFeeValue;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public void setAdmissionFeeValue(Double admissionFeeValue) {
        this.admissionFeeValue = admissionFeeValue;
    }

    public Double getOtherFeeValue() {
        return otherFeeValue;
    }

    public void setOtherFeeValue(Double otherFeeValue) {
        this.otherFeeValue = otherFeeValue;
    }

    public Double getShiftPaymentValue() {
        return shiftPaymentValue;
    }

    public void setShiftPaymentValue(Double shiftPaymentValue) {
        this.shiftPaymentValue = shiftPaymentValue;
    }

    public Double getAdmissionRate() {
        return admissionRate;
    }

    public void setAdmissionRate(Double admissionRate) {
        this.admissionRate = admissionRate;
    }

    public WorkingTime getWorkingTimeForPayment() {
        return workingTimeForPayment;
    }

    public void setWorkingTimeForPayment(WorkingTime workingTimeForPayment) {
        this.workingTimeForPayment = workingTimeForPayment;
    }

    public Integer getStaffBillFeesForPaymentCount() {
        if (staffBillFeesForPayment != null) {
            staffBillFeesForPaymentCount = staffBillFeesForPayment.size();
        } else {
            staffBillFeesForPaymentCount = 0;
        }
        return staffBillFeesForPaymentCount;
    }

    public void setStaffBillFeesForPaymentCount(Integer staffBillFeesForPaymentCount) {
        this.staffBillFeesForPaymentCount = staffBillFeesForPaymentCount;
    }

    public Integer getAdmissionCount() {
        if (staffAdmissionsForPayments != null) {
            admissionCount = staffAdmissionsForPayments.size();
        } else {
            admissionCount = 0;
        }
        return admissionCount;
    }

    public void setAdmissionCount(Integer admissionCount) {
        this.admissionCount = admissionCount;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<WorkingTime> getStaffPaymentsCompleted() {
        return staffPaymentsCompleted;
    }

    public void setStaffPaymentsCompleted(List<WorkingTime> staffPaymentsCompleted) {
        this.staffPaymentsCompleted = staffPaymentsCompleted;
    }

    public PaymentMethod getPaymentMethod() {
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.Cash;
        }
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    /**
     *
     */
    @FacesConverter(forClass = WorkingTime.class)
    public static class WorkingTimeConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            WorkingTimeController controller = (WorkingTimeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "workingTimeController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof WorkingTime) {
                WorkingTime o = (WorkingTime) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + WorkingTime.class.getName());
            }
        }
    }

}
