package com.divudi.bean.channel;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CommonFunctions;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class ChannelAgentPaymentBillController1 implements Serializable {

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
    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    BillNumberGenerator billNumberBean;
    //////////////////
    @Inject
    SessionController sessionController;
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
                billFees = getBillFeeFacade().findBySQL(sql);
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
        printPreview = false;
        paymentMethod = null;
        speciality = null;
        serviceSessions = null;
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

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (speciality != null) {
                sql = "select p from Staff p where p.retired=false and (upper(p.person.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
            } else {
                sql = "select p from Staff p where p.retired=false and (upper(p.person.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            ////System.out.println(sql);
            suggestions = getStaffFacade().findBySQL(sql);
        }
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

            List<BillFee> tmp = getBillFeeFacade().findBySQL(sql, h, TemporalType.DATE);

            for (BillFee bf : tmp) {
                if (bf.getBill().getBillType() == BillType.ChannelCredit) {
                    bf.setFeeValue(0.0);
                }
            }

            dueBillFees = tmp;

        }

    }

    public void calculateDueFees() {

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.refunded=false"
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt "
                + " and b.staff=:stf ";

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

        hm.put("stf", getCurrentStaff());
        //hm.put("ins", sessionController.getInstitution());
        hm.put("bt", bts);
        hm.put("ftp", FeeType.Staff);
        hm.put("class", BilledBill.class);
        dueBillFees = billFeeFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }
    
    public void calculateDueFeesAgency() {

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
        dueBillFees = billFeeFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);

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
        totalPaying = 0;

        for (BillFee f : payingBillFees) {
            ////System.out.println("totalPaying before " + totalPaying);
            ////System.out.println("fee val is " + f.getFeeValue());
            ////System.out.println("paid val is " + f.getPaidValue());
            totalPaying = totalPaying + (f.getFeeValue() - f.getPaidValue());
            ////System.out.println("totalPaying after " + totalPaying);
        }
        ////System.out.println("total pay is " + totalPaying);
    }

    public List<ServiceSession> getServiceSessions() {
        serviceSessions = new ArrayList<ServiceSession>();
        String sql;

        if (currentStaff != null) {
            Calendar c = Calendar.getInstance();
            c.setTime(getDate());
            int wd = c.get(Calendar.DAY_OF_WEEK);
            sql = "Select s From ServiceSession s where s.retired=false and s.staff.id=" + getCurrentStaff().getId() + " and s.sessionWeekday=" + wd;
            serviceSessions = getServiceSessionFacade().findBySQL(sql);
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
        ////System.out.println("setting paying bill fees " + payingBillFees.size());
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

    private Bill createPaymentBill() {
        BilledBill tmp = new BilledBill();
        tmp.setBillDate(Calendar.getInstance().getTime());
        tmp.setBillTime(Calendar.getInstance().getTime());
        tmp.setBillType(BillType.PaymentBill);
        tmp.setCreatedAt(Calendar.getInstance().getTime());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setDepartment(getSessionController().getDepartment());

        tmp.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.PaymentBill, BillClassType.BilledBill, BillNumberSuffix.CHNPROPAY));
        tmp.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PaymentBill, BillClassType.BilledBill, BillNumberSuffix.CHNPROPAY));

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

    private boolean checkBillFeeValue() {
        for (BillFee f : payingBillFees) {
            if (f.getFeeValue() == 0.0) {
                return true;
            }
        }
        return false;
    }

    private boolean errorCheck() {
        if (currentStaff == null) {
            UtilityController.addErrorMessage("Please select a Staff Memeber");
            return true;
        }

        if (checkBillFeeValue()) {
            UtilityController.addErrorMessage("There is a Credit Bill");
            return true;
        }

        performCalculations();
        if (totalPaying == 0) {
            UtilityController.addErrorMessage("Please select payments to update");
            return true;
        }
        if (paymentMethod == null) {
            UtilityController.addErrorMessage("Please select a payment method");
            return true;
        }

        return false;
    }

    public void settleBill() {
        if (errorCheck()) {
            return;
        }
        calculateTotalPay();
        Bill b = createPaymentBill();
        current = b;
        getBillFacade().create(b);
        saveBillCompo(b);
        printPreview = true;
        UtilityController.addSuccessMessage("Successfully Paid");
        ////System.out.println("Paid");
    }

    private void saveBillCompo(Bill b) {
        for (BillFee bf : getPayingBillFees()) {
            saveBillItemForPaymentBill(b, bf);
//            saveBillFeeForPaymentBill(b,bf); No need to add fees for this bill
            bf.setPaidValue(bf.getFeeValue());
            getBillFeeFacade().edit(bf);
            ////System.out.println("marking as paid");
        }
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

    public ChannelAgentPaymentBillController1() {
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
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private BillFacade getFacade() {
        return billFacade;
    }

    public List<Bill> getItems() {
        items = getFacade().findAll("name", true);
        return items;
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
//        if (toDate == null) {
//            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
//        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        //  resetLists();
    }

    public Date getFromDate() {
        //Dont Remove Comments if u want ask Safrin
//        if (fromDate == null) {
//            fromDate = getCommonFunctions().getStartOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
//        }
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
        ////System.out.println("sql is " + sql);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        dueBillFeeReport = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        ////System.out.println(dueBillFeeReport.size());

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
            billItems = getBillItemFacade().findBySQL(sql);
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
        dueBillFees = new ArrayList<BillFee>();
        payingBillFees = new ArrayList<BillFee>();
        totalPaying = 0.0;
        totalDue = 0.0;
        printPreview = false;

        calculateDueFees();
        performCalculations();
    }

    public List<BillFee> getFilteredBillFee() {
        return filteredBillFee;
    }

    public void setFilteredBillFee(List<BillFee> filteredBillFee) {
        this.filteredBillFee = filteredBillFee;
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
            ChannelAgentPaymentBillController1 controller = (ChannelAgentPaymentBillController1) facesContext.getApplication().getELResolver().
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
                        + object.getClass().getName() + "; expected type: " + ChannelAgentPaymentBillController1.class.getName());
            }
        }
    }
}
