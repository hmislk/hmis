package com.divudi.bean.channel;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.PersonInstitutionType;
import com.divudi.ejb.BillNumberGenerator;

import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class ChannelStaffPaymentBillController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private BillFacade billFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    StaffFacade staffFacade;
    @EJB
    private ServiceSessionFacade serviceSessionFacade;
    /////////////////

    private CommonFunctions commonFunctions;
    @EJB
    BillNumberGenerator billNumberBean;
    //////////////////
    @Inject
    SessionController sessionController;
    @Inject
    CommonController commonController;
    /////////////////////
    private List<BillItem> billItems;
    List<Bill> selectedItems;
    private List<Bill> items = null;
    private List<BillFee> dueBillFeeReport;
    private List<BillFee> filteredBillFee;
    List<BillFee> dueBillFees;
    List<BillFee> payingBillFees;
    private List<BillFee> billFees;
    private List<ServiceSession> serviceSessions;
    private List<ServiceSession> serviceSessionList;
    /////////////////////    
    private Date fromDate;
    private Date toDate;
    private Date date;
    private Bill current;
    Staff currentStaff;
    Institution institution;
    double totalDue;
    double totalPaying;
    private Boolean printPreview = false;
    PaymentMethod paymentMethod;
    Speciality speciality;
    private ServiceSession selectedServiceSession;
    boolean considerDate = false;
    BillFee billFee;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public List<BillFee> getBillFees() {
        if (getCurrent() != null) {
            if (billFees == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getCurrent().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
                if (billFees == null) {
                    billFees = new ArrayList<BillFee>();
                }
            }
        }

        return billFees;
    }

    private void recreateModel() {
        billFees = null;
        billItems = null;
        printPreview = false;
        billItems = null;
        selectedItems = null;
        items = null;
        dueBillFeeReport = null;
        dueBillFees = null;
        payingBillFees = null;
        billFees = null;
        /////////////////////    
        fromDate = null;
        toDate = null;
        current = null;
        currentStaff = null;
        totalDue = 0.0;
        totalPaying = 0.0;
        printPreview = false;
        paymentMethod = null;
        speciality = null;
        serviceSessions = null;
    }

    public void makenull() {
        billFees = null;
        billItems = null;
        printPreview = false;
        selectedItems = null;
        items = null;
        dueBillFeeReport = null;
        dueBillFees = null;
        payingBillFees = null;
        billFees = null;
        /////////////////////    
        fromDate = null;
        toDate = null;
        current = null;
        currentStaff = null;
        totalDue = 0.0;
        totalPaying = 0.0;
        //printPreview = false;
        paymentMethod = null;
        speciality = null;
        serviceSessions = null;
        serviceSessionList = null;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
        currentStaff = null;
        dueBillFees = new ArrayList<BillFee>();
        payingBillFees = new ArrayList<BillFee>();
        totalPaying = 0.0;
        totalDue = 0.0;

    }

//    public List<Staff> completeStaff(String query) {
//        List<Staff> suggestions;
//        String sql;
//        if (query == null) {
//            suggestions = new ArrayList<>();
//        } else {
//            if (speciality != null) {
//                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
//            } else {
//                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
//            }
//            //////// // System.out.println(sql);
//            suggestions = getStaffFacade().findByJpql(sql);
//        }
//        return suggestions;
//    }
    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions = new ArrayList<>();
        String sql;
        Map m = new HashMap();

        if (getSpeciality() != null) {
            if (getSessionController().getLoggedPreference().isShowOnlyMarkedDoctors()) {

                sql = " select pi.staff from PersonInstitution pi where pi.retired=false "
                        + " and pi.type=:typ "
                        + " and pi.institution=:ins "
                        + " and ((pi.staff.person.name) like '%" + query.toUpperCase() + "%'or  (pi.staff.code) like '%" + query.toUpperCase() + "%' )"
                        + " and pi.staff.speciality=:spe "
                        + " order by pi.staff.person.name ";

                m.put("ins", getSessionController().getInstitution());
                m.put("spe", getSpeciality());
                m.put("typ", PersonInstitutionType.Channelling);
            } else {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
            }
        } else {
            sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
        }
        suggestions = getStaffFacade().findByJpql(sql, m);

        return suggestions;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public double getTotalDue() {
        return totalDue;
    }

    public void setTotalDue(double totalDue) {
        this.totalDue = totalDue;
    }

    public double getTotalPaying() {
        return totalPaying;
    }

    public void setTotalPaying(double totalPaying) {
        this.totalPaying = totalPaying;
    }

    public void calculateDueFeesOld() {
        if (currentStaff == null || currentStaff.getId() == null || selectedServiceSession == null) {
            dueBillFees = new ArrayList<>();
        } else {
            String sql;
            HashMap h = new HashMap();

            sql = "select b from BillFee b where b.retired=false and (b.bill.billType=:btp or b.bill.billType=:btp2)"
                    + "and b.bill.id in(Select bs.bill.id From BillSession bs where bs.retired=false and bs.serviceSession.id=" + getSelectedServiceSession().getId() + " and bs.sessionDate= :ssDate) "
                    + "and b.bill.cancelled=false and b.bill.refunded=false  and (b.feeValue - b.paidValue) > 0 and b.staff.id = " + currentStaff.getId();
            h.put("btp", BillType.ChannelPaid);
            h.put("btp2", BillType.ChannelCredit);
            h.put("ssDate", getDate());

            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql, h, TemporalType.DATE);

            for (BillFee bf : tmp) {
                if (bf.getBill().getBillType() == BillType.ChannelCredit) {
                    bf.setFeeValue(0.0);
                }
            }

            dueBillFees = tmp;

        }

    }

    public void calculateDueFees() {
        Date startTime = new Date();

        if (getSpeciality() == null) {
            JsfUtil.addErrorMessage("Select Specility");
            return;
        }

        if (getCurrentStaff() == null) {
            JsfUtil.addErrorMessage("Select Doctor");
            return;
        }
        if (considerDate) {
            if (getToDate().getTime()>commonFunctions.getEndOfDay().getTime()) {
                JsfUtil.addErrorMessage("You Can't search after current Date");
                return;
            }
        }

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelPaid};
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt "
                + " and b.staff=:stf ";

        HashMap hm = new HashMap();
        if (getFromDate() != null && getToDate() != null && considerDate) {
            sql += " and b.bill.appointmentAt between :frm and  :to";
            hm.put("frm", getFromDate());
            hm.put("to", getToDate());
        }else{
            sql += " and b.bill.appointmentAt <= :nd";
            hm.put("nd", commonFunctions.getEndOfDay());
        }

        if (getSelectedServiceSession() != null) {
            sql += " and b.bill.singleBillSession.serviceSession.originatingSession=:ss";
            hm.put("ss", getSelectedServiceSession());
        }
        
        sql += " and b.bill.singleBillSession.absent=false "
                + " order by b.bill.singleBillSession.serviceSession.sessionDate,"
                + " b.bill.singleBillSession.serviceSession.sessionTime,"
                + " b.bill.singleBillSession.serialNo ";
        

        hm.put("stf", getCurrentStaff());
        //hm.put("ins", sessionController.getInstitution());
        hm.put("bt", bts);
        hm.put("ftp", FeeType.Staff);
        hm.put("class", BilledBill.class);
        dueBillFees = billFeeFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
        //// // System.out.println("dueBillFees.size() = " + dueBillFees.size());
        //// // System.out.println("hm = " + hm);
        //// // System.out.println("sql = " + sql);
        
        HashMap m = new HashMap();
        sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false  "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt "
                + " and b.staff=:stf ";

        if (getFromDate() != null && getToDate() != null && considerDate) {
            sql += " and b.bill.appointmentAt between :frm and  :to";
            m.put("frm", getFromDate());
            m.put("to", getToDate());
        }

        if (getSelectedServiceSession() != null) {
            sql += " and b.bill.singleBillSession.serviceSession.originatingSession=:ss";
            m.put("ss", getSelectedServiceSession());
        }
        
        sql += " and b.bill.singleBillSession.absent=true "
                + " and b.bill.singleBillSession.serviceSession.originatingSession.refundable=false "
                + " order by b.bill.singleBillSession.serviceSession.sessionDate,"
                + " b.bill.singleBillSession.serviceSession.sessionTime,"
                + " b.bill.singleBillSession.serialNo ";
        m.put("stf", getCurrentStaff());
        //hm.put("ins", sessionController.getInstitution());
        m.put("bt", bts);
        m.put("ftp", FeeType.Staff);
        m.put("class", BilledBill.class);
        List<BillFee>nonRefundableBillFees=new ArrayList<>();
        nonRefundableBillFees=billFeeFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        dueBillFees.addAll(nonRefundableBillFees);
        
        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Payment/pay doctor(/faces/channel/channel_payment_staff_bill.xhtml)");

    }

    public void calculateDueFeesAgency() {
        Date startTime = new Date();

        String sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false"
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType=:bt ";

        HashMap hm = new HashMap();
        if (getFromDate() != null && getToDate() != null) {
            sql += " and b.bill.appointmentAt between :frm and  :to";
            hm.put("frm", getFromDate());
            hm.put("to", getToDate());
        }

        if (getSelectedServiceSession() != null) {
            sql += " and b.serviceSession=:ss";
            hm.put("ss", getSelectedServiceSession());
        }

        if (getInstitution() != null) {
            sql += " and b.institution=:ins";
            hm.put("ins", getInstitution());
        }

        //hm.put("ins", sessionController.getInstitution());
        //hm.put("bt", bts);
        hm.put("ftp", FeeType.OtherInstitution);
        hm.put("class", BilledBill.class);
        hm.put("bt", BillType.ChannelAgent);
        dueBillFees = billFeeFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
        
        commonController.printReportDetails(fromDate, toDate, startTime, "Channeling/Payment/Pay agent(/faces/channel/channel_payment_bill_search.xhtml)");

    }

    public void calculateTotalDue() {
        if (dueBillFees != null) {
            totalDue = 0;
            for (BillFee f : dueBillFees) {
                totalDue = totalDue + f.getFeeValue() - f.getPaidValue();
            }
        }
    }

    public void performCalculations() {
        calculateTotalDue();
        calculateTotalPay();
    }

    public void calculateTotalPay() {
        System.out.println("payingBillFees = " + getPayingBillFees().size());
        totalPaying = 0;
        for (BillFee f : payingBillFees) {
            totalPaying = totalPaying + (f.getFeeValue() - f.getPaidValue());
        }
    }

    public List<ServiceSession> getServiceSessions() {
        serviceSessions = new ArrayList<ServiceSession>();
        String sql;

        if (currentStaff != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(getDate());
            int wd = c.get(Calendar.DAY_OF_WEEK);
            sql = "Select s From ServiceSession s where s.retired=false and s.staff.id=" + getCurrentStaff().getId() + " and s.sessionWeekday=" + wd;
            serviceSessions = getServiceSessionFacade().findByJpql(sql);
        }

        return serviceSessions;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public List<BillFee> getDueBillFees() {
        return dueBillFees;
    }

    public void setDueBillFees(List<BillFee> dueBillFees) {
        this.dueBillFees = dueBillFees;
    }

    public List<BillFee> getPayingBillFees() {
        return payingBillFees;
    }

    public void setPayingBillFees(List<BillFee> payingBillFees) {
        this.payingBillFees = payingBillFees;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Staff getCurrentStaff() {
        return currentStaff;

    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;

    }

    public void prepareAdd() {
        current = new BilledBill();
    }

    public void setSelectedItems(List<Bill> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public void fillSessions() {
        System.out.println("this = " + "working this");
        String sql;
        Map m = new HashMap();
        sql = "Select s From ServiceSession s "
                + " where s.retired=false "
                + " and type(s)=:class "
                + " and s.staff=:doc "
                + " and s.originatingSession is null "
                + " order by s.sessionWeekday,s.startingTime";
        m.put("doc", currentStaff);
        m.put("class", ServiceSession.class);
        serviceSessionList = getServiceSessionFacade().findByJpql(sql, m);
        System.out.println("serviceSessionList = " + serviceSessionList.size());
    }

    private Bill createPaymentBill() {
        BilledBill tmp = new BilledBill();
        tmp.setBillDate(Calendar.getInstance().getTime());
        tmp.setBillTime(Calendar.getInstance().getTime());
        tmp.setBillType(BillType.ChannelProPayment);
        tmp.setCreatedAt(Calendar.getInstance().getTime());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setDepartment(getSessionController().getDepartment());

        tmp.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.ChannelProPayment, BillClassType.BilledBill, BillNumberSuffix.CHNPROPAY));
        tmp.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.ChannelProPayment, BillClassType.BilledBill, BillNumberSuffix.CHNPROPAY));

        tmp.setDiscount(0.0);
        tmp.setDiscountPercent(0.0);

        tmp.setInstitution(getSessionController().getInstitution());
        tmp.setNetTotal(0 - totalPaying);
        tmp.setPaymentMethod(paymentMethod);
        tmp.setStaff(currentStaff);
        tmp.setToStaff(currentStaff);
        tmp.setTotal(0 - totalPaying);

        return tmp;
    }

    private Bill createPaymentBillAgent() {
        BilledBill tmp = new BilledBill();
        tmp.setBillDate(Calendar.getInstance().getTime());
        tmp.setBillTime(Calendar.getInstance().getTime());
        tmp.setBillType(BillType.ChannelAgencyCommission);
        tmp.setCreatedAt(Calendar.getInstance().getTime());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setDepartment(getSessionController().getDepartment());

        tmp.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.ChannelAgencyCommission, BillClassType.BilledBill, BillNumberSuffix.AGNPAY));
        tmp.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.ChannelAgencyCommission, BillClassType.BilledBill, BillNumberSuffix.AGNPAY));

        tmp.setDiscount(0.0);
        tmp.setDiscountPercent(0.0);

        tmp.setInstitution(getSessionController().getInstitution());
        tmp.setNetTotal(0 - totalPaying);
        tmp.setPaymentMethod(paymentMethod);
        tmp.setToInstitution(institution);
//        tmp.setStaff(currentStaff);
        //tmp.setToStaff(currentStaff);
        tmp.setTotal(0 - totalPaying);

        return tmp;
    }

    private boolean checkBillFeeValue() {
        for (BillFee f : payingBillFees) {
            if (f.getFeeValue() == 0.0) {
                return true;
            }
        }
        return false;
    }

    private boolean errorCheck() {
        System.out.println("this working error check");
        if (currentStaff == null) {
            JsfUtil.addErrorMessage("Please select a Staff Memeber");
            return true;
        }

        if (checkBillFeeValue()) {
            JsfUtil.addErrorMessage("There is a Credit Bill");
            return true;
        }

        performCalculations();
        if (totalPaying == 0) {
            JsfUtil.addErrorMessage("Total Paying Amount is zero. Please select payments to update");
            return true;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return true;
        }

        return false;
    }

    private boolean errorCheckForAgency() {
//        if (currentStaff == null) {
//            JsfUtil.addErrorMessage("Please select a Staff Memeber");
//            return true;
//        }

        if (checkBillFeeValue()) {
            JsfUtil.addErrorMessage("There is a Credit Bill");
            return true;
        }

        performCalculations();
        if (totalPaying == 0) {
            JsfUtil.addErrorMessage("Please select payments to update");
            return true;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return true;
        }

        return false;
    }

    public void settleBill() {
        System.out.println("this = " + payingBillFees.size());
        System.out.println("working settle bill");
        if (errorCheck()) {
            return;
        }
        calculateTotalPay();
        Bill b = createPaymentBill();
        current = b;
        getBillFacade().create(b);
        saveBillCompo(b);
        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Paid");
        //////// // System.out.println("Paid");
    }

    public void settleBillAgent() {
        if (errorCheckForAgency()) {
            return;
        }
        calculateTotalPay();
        Bill b = createPaymentBillAgent();
        current = b;
        getBillFacade().create(b);
        saveBillCompo(b);
        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Paid");
        //////// // System.out.println("Paid");
    }

    private void saveBillCompo(Bill b) {
        System.out.println("save bill true");
        System.out.println("b = " + b);
        for (BillFee bf : getPayingBillFees()) {
            saveBillItemForPaymentBill(b, bf);
//            saveBillFeeForPaymentBill(b,bf); No need to add fees for this bill
            bf.setPaidValue(bf.getFeeValue());
            getBillFeeFacade().edit(bf);
            //////// // System.out.println("marking as paid");
        }
        System.out.println("save bill true end");
    }

    private void saveBillItemForPaymentBill(Bill b, BillFee bf) {
        BillItem i = new BillItem();
        i.setReferanceBillItem(bf.getBillItem());
        i.setReferenceBill(bf.getBill());
        i.setPaidForBillFee(bf);
        i.setBill(b);
        i.setCreatedAt(Calendar.getInstance().getTime());
        i.setCreater(getSessionController().getLoggedUser());
        i.setDiscount(0.0);
        i.setGrossValue(bf.getFeeValue());
//        if (bf.getBillItem() != null && bf.getBillItem().getItem() != null) {
//            i.setItem(bf.getBillItem().getItem());
//        }
        i.setNetValue(bf.getFeeValue());
        i.setQty(1.0);
        i.setRate(bf.getFeeValue());
        getBillItemFacade().create(i);
        b.getBillItems().add(i);
    }

    public BillFacade getEjbFacade() {
        return billFacade;
    }

    public void setEjbFacade(BillFacade ejbFacade) {
        this.billFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ChannelStaffPaymentBillController() {
    }

    public Bill getCurrent() {
        if (current == null) {
            current = new BilledBill();
        }
        return current;
    }

    public void setCurrent(Bill current) {
        currentStaff = null;
        dueBillFees = new ArrayList<BillFee>();
        payingBillFees = new ArrayList<BillFee>();
        totalPaying = 0.0;
        totalDue = 0.0;
        recreateModel();
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
//        getItems();
        current = null;
        getCurrent();
    }

    private BillFacade getFacade() {
        return billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public Boolean getPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(Boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Date getToDate() {
        //Dont Remove Comments if u want ask Safrin
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        //  resetLists();
    }

    public Date getFromDate() {
        //Dont Remove Comments if u want ask Safrin
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        //  resetLists();
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<BillFee> getDueBillFeeReport() {
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillFee b where b.retired=false and b.bill.cancelled=false and (b.feeValue - b.paidValue) > 0 and b.bill.institution.id=" + getSessionController().getInstitution().getId() + " and b.bill.billDate between :fromDate and :toDate order by b.staff.id  ";
        //////// // System.out.println("sql is " + sql);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        dueBillFeeReport = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        //////// // System.out.println(dueBillFeeReport.size());

        if (dueBillFeeReport == null) {
            dueBillFeeReport = new ArrayList<>();
        }

        return dueBillFeeReport;
    }

//     sql = "select b from BillFee b where b.retired=false and (b.bill.billType=:btp or b.bill.billType=:btp2)"
//                    + "and b.bill.id in(Select bs.bill.id From BillSession bs where bs.retired=false and bs.serviceSession.id=" + getSelectedServiceSession().getId() + " and bs.sessionDate= :ssDate) "
//                    + "and b.bill.cancelled=false and b.bill.refunded=false  and (b.feeValue - b.paidValue) > 0 and b.staff.id = " + currentStaff.getId();
    public void setDueBillFeeReport(List<BillFee> dueBillFeeReport) {
        this.dueBillFeeReport = dueBillFeeReport;
    }

    public List<BillItem> getBillItems() {
        if (getCurrent() != null) {
            String sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id = " + current.getId();
            billItems = getBillItemFacade().findByJpql(sql);
            if (billItems == null) {
                billItems = new ArrayList<BillItem>();
            }
        }

        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public void setItems(List<Bill> items) {
        this.items = items;

    }

    public Date getDate() {
        if (date == null) {
            date = new Date();
        }
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<ServiceSession> getServiceSession() {
        return serviceSessions;
    }

    public void setServiceSession(List<ServiceSession> serviceSession) {
        this.serviceSessions = serviceSession;
    }

    public List<ServiceSession> getServiceSessionList() {
        return serviceSessionList;
    }

    public void setServiceSessionList(List<ServiceSession> serviceSessionList) {
        this.serviceSessionList = serviceSessionList;
    }

    public ServiceSessionFacade getServiceSessionFacade() {
        return serviceSessionFacade;
    }

    public void setServiceSessionFacade(ServiceSessionFacade serviceSessionFacade) {
        this.serviceSessionFacade = serviceSessionFacade;

    }

    public ServiceSession getSelectedServiceSession() {
        return selectedServiceSession;
    }

    public void setSelectedServiceSession(ServiceSession selectedServiceSession) {
        this.selectedServiceSession = selectedServiceSession;
//        dueBillFees = new ArrayList<BillFee>();
//        payingBillFees = new ArrayList<BillFee>();
//        totalPaying = 0.0;
//        totalDue = 0.0;
//        printPreview = false;
//
//        calculateDueFees();
//        performCalculations();
    }

    public List<BillFee> getFilteredBillFee() {
        return filteredBillFee;
    }

    public void setFilteredBillFee(List<BillFee> filteredBillFee) {
        this.filteredBillFee = filteredBillFee;
    }

    public boolean isConsiderDate() {
        return considerDate;
    }

    public void setConsiderDate(boolean considerDate) {
        this.considerDate = considerDate;
    }

    public BillFee getBillFee() {
        return billFee;
    }

    public void setBillFee(BillFee billFee) {
        if (billFee != null) {
            setSpeciality(billFee.getSpeciality());
            setCurrentStaff(billFee.getStaff());
            calculateDueFees();
        }
        this.billFee = billFee;
    }

    /**
     *
     */
    @FacesConverter(forClass = BilledBill.class)
    public static class StaffPaymentBillControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ChannelStaffPaymentBillController controller = (ChannelStaffPaymentBillController) facesContext.getApplication().getELResolver().
                    //getValue(facesContext.getELContext(), null, "billController");
                    getValue(facesContext.getELContext(), null, "staffPaymentBillController");
            return controller.billFacade.find(getKey(value));
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
            if (object instanceof BilledBill) {
                BilledBill o = (BilledBill) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ChannelStaffPaymentBillController.class.getName());
            }
        }
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }
    
    
}
