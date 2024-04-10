/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.ServiceSessionBean;
import com.divudi.entity.AuditEvent;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.MedicalPackage;
import com.divudi.entity.Packege;
import com.divudi.entity.Patient;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.RefundBill;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class BillPackageMedicalController implements Serializable, ControllerWithPatient {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    private CommonController commonController;

    @Inject
    private AuditEventApplicationController auditEventApplicationController;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    private boolean printPreview;
    private String patientTabId = "tabNewPt";
    private YearMonthDay yearMonthDay;
    //Interface Data
    private PaymentScheme paymentScheme;
    private Patient patient;
    private Doctor referredBy;
    private Institution creditCompany;
    private Staff staff;
    private double total;
    private double discount;
    private double netTotal;
    private double cashPaid;
    private double cashBalance;
    double billedBillTotal;
    double canceledBillTotal;
    double refundedBillTotal;
    private BillItem currentBillItem;
    //Bill Items
    private List<BillComponent> lstBillComponents;
    private List<BillFee> lstBillFees;
    private List<BillItem> lstBillItems;
    private List<BillEntry> lstBillEntries;
    private Integer index;
    private boolean patientDetailsEditable;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @Inject
    private BillBeanController billBean;

    CommonFunctions commonFunctions;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    //Temprory Variable
    private Patient tmpPatient;
    List<Bill> bills;
    List<BillItem> billedBillItemLst;
    List<BillItem> canceledBillItemLst;
    List<BillItem> refundedBillItemLst;
    @Inject
    private BillSearch billSearch;
    PaymentMethodData paymentMethodData;

    // report
    @Temporal(TemporalType.TIMESTAMP)
    Date frmDate;
    @Temporal(TemporalType.TIMESTAMP)
    Date toDate;
    List<BillItem> billItems;
    Institution institution;
    Item ServiceItem;

    public void makeNull() {
        billItems = null;
        currentBillItem = null;
        total = 0.0;
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
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

    @EJB
    CashTransactionBean cashTransactionBean;
    @EJB
    BillSessionFacade billSessionFacade;

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void cancellAll() {
        Bill tmp = new CancelledBill();
        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());
        getBillFacade().create(tmp);

        Bill billedBill = null;
        for (Bill b : bills) {
            billedBill = b.getBackwardReferenceBill();
            getBillSearch().setBill((BilledBill) b);
            getBillSearch().setPaymentMethod(b.getPaymentMethod());
            getBillSearch().setComment("Batch Cancell");
            //////// // System.out.println("ggg : " + getBillSearch().getComment());
            getBillSearch().cancelOpdBill();
        }

        tmp.copy(billedBill);
        tmp.setBilledBill(billedBill);

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(tmp, getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);

    }

    public Title[] getTitle() {

        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public List<Bill> getBills() {
        if (bills == null) {
            bills = new ArrayList<>();
        }
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    private void savePatient() {
        if (getPatientTabId().equals("tabNewPt")) {
            getPatient().setCreater(getSessionController().getLoggedUser());
            getPatient().setCreatedAt(new Date());

            getPatient().getPerson().setCreater(getSessionController().getLoggedUser());
            getPatient().getPerson().setCreatedAt(new Date());

            getPersonFacade().create(getPatient().getPerson());
            getPatientFacade().create(getPatient());
            tmpPatient = getPatient();

        } else if (getPatientTabId().equals("tabSearchPt")) {
            tmpPatient = getPatient();
        }
    }

    private Patient savePatient(Patient p) {

        if (p == null) {
            return null;
        }
        if (p.getPerson() == null) {
            return null;
        }

        if (p.getPerson().getId() == null) {
            p.getPerson().setCreater(sessionController.getLoggedUser());
            p.getPerson().setCreatedAt(new Date());
            personFacade.create(p.getPerson());
        } else {
            personFacade.edit(p.getPerson());
        }

        if (p.getId() == null) {
            p.setCreater(sessionController.getLoggedUser());
            p.setCreatedAt(new Date());
            patientFacade.create(p);
        } else {
            patientFacade.edit(p);
        }

        return p;
    }

    public void putToBills() {
        bills = new ArrayList<>();
        Set<Department> billDepts = new HashSet<>();
        for (BillEntry e : lstBillEntries) {
            billDepts.add(e.getBillItem().getItem().getDepartment());

        }
        for (Department d : billDepts) {
            BilledBill myBill = new BilledBill();

            saveBill(d, myBill);

            List<BillEntry> tmp = new ArrayList<>();
            for (BillEntry e : lstBillEntries) {

                if (Objects.equals(e.getBillItem().getItem().getDepartment().getId(), d.getId())) {
                    getBillBean().saveBillItem(myBill, e, getSessionController().getLoggedUser());
                    // getBillBean().calculateBillItem(myBill, e);                
                    tmp.add(e);
                }
            }
            //////// // System.out.println("555");
            getBillBean().calculateBillItems(myBill, tmp);
            bills.add(myBill);
        }
    }

    public String navigateToMedicalPakageBillingFromMenu() {
        clearBillValues();
        setPatient(getPatient());

        return "/opd_bill_package_medical";
    }

    public void settleBill() {

        if (errorCheck()) {
            return;
        }
        savePatient(getPatient());
        if (getBillBean().calculateNumberOfBillsPerOrder(getLstBillEntries()) == 1) {
            BilledBill temp = new BilledBill();
            Bill b = saveBill(lstBillEntries.get(0).getBillItem().getItem().getDepartment(), temp);
            getBillBean().saveBillItems(b, getLstBillEntries(), getSessionController().getLoggedUser());
            getBillBean().calculateBillItems(b, getLstBillEntries());
            getBills().add(b);

        } else {
            //    //////// // System.out.println("11");
            putToBills();
            //   //////// // System.out.println("22");
        }

        saveBatchBill();
        saveBillItemSessions();

        clearBillItemValues();
        //////// // System.out.println("33");
        JsfUtil.addSuccessMessage("Bill Saved");
        printPreview = true;
    }

    @EJB
    ServiceSessionBean serviceSessionBean;

    public ServiceSessionBean getServiceSessionBean() {
        return serviceSessionBean;
    }

    public void setServiceSessionBean(ServiceSessionBean serviceSessionBean) {
        this.serviceSessionBean = serviceSessionBean;
    }

    private void saveBillItemSessions() {
        for (BillEntry be : lstBillEntries) {
            be.getBillItem().setBillSession(getServiceSessionBean().createBillSession(be.getBillItem()));

            if (be.getBillItem().getBillSession() != null) {
                getBillSessionFacade().create(be.getBillItem().getBillSession());

            }

        }
    }

    private void saveBatchBill() {
        Bill tmp = new BilledBill();
        tmp.setBillType(BillType.OpdBathcBill);
        tmp.setPaymentScheme(paymentScheme);
        tmp.setPaymentMethod(paymentMethod);
        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());
        getBillFacade().create(tmp);

        double dbl = 0;
        for (Bill b : bills) {
            b.setBackwardReferenceBill(tmp);
            dbl += b.getNetTotal();
            getBillFacade().edit(b);

            tmp.getForwardReferenceBills().add(b);
        }

        tmp.setNetTotal(dbl);
        getBillFacade().edit(tmp);

        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(tmp, getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);

    }

    PaymentMethod paymentMethod;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    private Bill saveBill(Department bt, BilledBill temp) {

        //getCurrent().setCashBalance(cashBalance); 
        //getCurrent().setCashPaid(cashPaid);
        //  temp.setBillType(bt);
        temp.setBillType(BillType.OpdBill);

        temp.setBillPackege(currentBillItem.getItem());

        temp.setDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setToDepartment(bt);
        temp.setToInstitution(bt.getInstitution());

        temp.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setStaff(staff);
        temp.setReferredBy(referredBy);
        //System.err.println("aaaa : " + getCreditCompany());
        temp.setCreditCompany(getCreditCompany());

        getBillBean().setPaymentMethodData(temp, getPaymentMethod(), getPaymentMethodData());

        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setPatient(tmpPatient);
//        temp.setPatientEncounter(patientEncounter);
        temp.setPaymentScheme(getPaymentScheme());
        temp.setPaymentMethod(paymentMethod);
        temp.setCreatedAt(new Date());
        temp.setCreater(getSessionController().getLoggedUser());
        temp.setDeptId(getBillNumberBean().departmentBillNumberGenerator(temp.getDepartment(), temp.getToDepartment(), temp.getBillType(), BillClassType.BilledBill));
        temp.setInsId(getBillNumberBean().institutionBillNumberGenerator(temp.getInstitution(), temp.getToDepartment(), temp.getBillType(), BillClassType.BilledBill, BillNumberSuffix.PACK));

        if (temp.getId() == null) {
            getFacade().create(temp);
        }
        return temp;

    }

    @Inject
    PaymentSchemeController paymentSchemeController;

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    private boolean errorCheck() {
        if (getPatientTabId().toString().equals("tabNewPt")) {

            if (getPatient().getPerson().getName() == null || getPatient().getPerson().getName().trim().equals("") || getPatient().getPerson().getSex() == null || getPatient().getPerson().getDob() == null) {
                JsfUtil.addErrorMessage("Can not bill without Patient Name, Age or Sex.");
                return true;
            }

            if (!com.divudi.java.CommonFunctions.checkAgeSex(getPatient().getPerson().getDob(), getPatient().getPerson().getSex(), getPatient().getPerson().getTitle())) {
                JsfUtil.addErrorMessage("Check Title,Age,Sex");
                return true;
            }

            if (getPatient().getPerson().getPhone().length() < 1) {
                JsfUtil.addErrorMessage("Phone Number is Required it should be fill");
                return true;
            }

        }
        if (getLstBillEntries().isEmpty()) {
            JsfUtil.addErrorMessage("No investigations are added to the bill to settle");
            return true;
        }

        if (getPaymentMethod() == null) {
            return true;
        }

        if (getPaymentSchemeController().errorCheckPaymentMethod(getPaymentMethod(), getPaymentMethodData())) {
            return true;
        }

//        if (paymentScheme.getPaymentMethod() == PaymentMethod.Cash) {
//            if (cashPaid == 0.0) {
//                JsfUtil.addErrorMessage("Please select tendered amount correctly");
//                return true;
//            }
//            if (cashPaid < getNetTotal()) {
//                JsfUtil.addErrorMessage("Please select tendered amount correctly");
//                return true;
//            }
//        }
        return false;
    }

    private void addEntry(BillItem bi) {
        if (bi == null) {
            JsfUtil.addErrorMessage("Nothing to add");
            return;
        }
        if (bi.getItem() == null) {
            JsfUtil.addErrorMessage("Please select an investigation");
            return;
        }

        BillEntry addingEntry = new BillEntry();
        addingEntry.setBillItem(bi);
        addingEntry.setLstBillComponents(getBillBean().billComponentsFromBillItem(bi));
        addingEntry.setLstBillFees(getBillBean().billFeefromBillItemMedicalPackage(bi, currentBillItem.getItem()));
        addingEntry.setLstBillSessions(getBillBean().billSessionsfromBillItem(bi));
        getLstBillEntries().add(addingEntry);
        bi.setRate(getBillBean().billItemRate(addingEntry));
        bi.setQty(1.0);
        bi.setNetValue(bi.getRate() * bi.getQty()); // Price == Rate as Qty is 1 here

        calTotals();
        if (bi.getNetValue() == 0.0) {
            JsfUtil.addErrorMessage("Please enter the rate");
            return;
        }
        //      clearBillItemValues();
        recreateBillItems();
    }

    public void addToBill() {
        if (getLstBillEntries().size() > 0) {
            JsfUtil.addErrorMessage("You can not add more than on package at a time create new bill");
            return;
        }

        List<Item> itemList = getBillBean().itemFromMedicalPackage(currentBillItem.getItem());
        setCreditCompany(getCurrentBillItem().getItem().getForInstitution());
        for (Item i : itemList) {
            if (i.getDepartment() == null) {
                JsfUtil.addErrorMessage("Under administration, add a Department for item " + i.getName());
                return;
            }

            BillItem tmp = new BillItem();
            tmp.setItem(i);
            addEntry(tmp);

        }

        //JsfUtil.addSuccessMessage("Item Added");
    }

    public void createBillItems(Item item) {
        String sql;
        Map m = new HashMap();
        sql = "select b from BillItem b"
                + " where b.bill.billType =:billType "
                + " and b.bill.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.bill.retired=false "
                + " and type(b.bill.billPackege)=:class ";

        if (getCurrentBillItem().getItem() != null) {
            sql += " and b.bill.billPackege=:item ";
            m.put("item", getCurrentBillItem().getItem());
        }

        if (institution != null) {
            sql += " and b.bill.billPackege.forInstitution=:ins ";
            m.put("ins", institution);
        }

        if (ServiceItem != null) {
            sql += " and b.item=:item ";
            m.put("item", ServiceItem);
        }

        m.put("class", item.getClass());
        m.put("billType", BillType.OpdBill);
        m.put("toDate", toDate);
        m.put("fromDate", frmDate);

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        total = 0.0;
        for (BillItem bi : billItems) {
            total += bi.getNetValue();
            ////// // System.out.println("total = " + total);
            ////// // System.out.println("bi.getNetValue() = " + bi.getNetValue());
        }
    }

    public List<BillItem> createBillItems(Item item, Bill bill) {
        String sql;
        Map m = new HashMap();
        sql = "select b from BillItem b"
                + " where b.bill.billType =:billType "
                + " and type(b.bill)=:class"
                + " and b.bill.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.bill.retired=false "
                + " and type(b.bill.billPackege)=:class ";

        if (getCurrentBillItem().getItem() != null) {
            sql += " and b.bill.billPackege=:item ";
            m.put("item", getCurrentBillItem().getItem());
        }

        if (institution != null) {
            sql += " and b.bill.billPackege.forInstitution=:ins ";
            m.put("ins", institution);
        }

        if (ServiceItem != null) {
            sql += " and b.item=:item ";
            m.put("item", ServiceItem);
        }

        m.put("class", item.getClass());
        m.put("billType", BillType.OpdBill);
        m.put("toDate", toDate);
        m.put("fromDate", frmDate);
        m.put("class", bill.getClass());

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        return billItems;
    }

    public void createBills(Item item) {
        bills = new ArrayList<>();

        bills.addAll(packageBills(item, false));
        bills.addAll(packageBills(item, true));
        total = 0.0;

        if (bills == null) {
            return;
        }

        for (Bill bi : bills) {
            total += bi.getNetTotal();
        }
    }

    public List<Bill> packageBills(Item item, boolean can) {
        String sql;
        Map m = new HashMap();
        sql = "select bill from Bill bill"
                + " where bill.billType =:billType "
                + " and bill.createdAt between :fromDate and :toDate "
                + " and bill.retired=false ";

        if (can) {
            if (getServiceItem() != null) {
                sql += " and bill.billedBill.billPackege=:item ";
                m.put("item", getServiceItem());
            }
            sql += " and type(bill.billedBill.billPackege)=:class ";
        } else {
            if (getServiceItem() != null) {
                sql += " and bill.billPackege=:item ";
                m.put("item", getServiceItem());
            }
            sql += " and type(bill.billPackege)=:class ";
        }

        if (institution != null) {
            sql += " and bill.creditCompany=:ins ";
            m.put("ins", institution);
        }

        m.put("class", item.getClass());
        m.put("billType", BillType.OpdBill);
        m.put("toDate", toDate);
        m.put("fromDate", frmDate);

        return billFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public double getTotal(List<BillItem> billItm) {
        double tot = 0.0;
        for (BillItem bi : billItm) {
            tot += bi.getNetValue();
        }

        return tot;
    }

    public void createMedicalPackageBillItems() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("createMedicalPackageBillItems()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        createBillItems(new MedicalPackage());

        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void createOtherPackageBillItems() {
        billedBillItemLst = createBillItems(new Packege(), new BilledBill());
        billedBillTotal = getTotal(billedBillItemLst);

        canceledBillItemLst = createBillItems(new Packege(), new CancelledBill());
        canceledBillTotal = getTotal(canceledBillItemLst);

        refundedBillItemLst = createBillItems(new Packege(), new RefundBill());
        refundedBillTotal = getTotal(refundedBillItemLst);

    }

    public void createOtherPackageBillItemsOld() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("createOtherPackageBillItemsOld()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

        createBillItems(new Packege());

        

    }

    public void createOtherPackageBills() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("createOtherPackageBills()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        createBills(new Packege());

        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
    }

    public void clearBillItemValues() {
        setCurrentBillItem(null);
        recreateBillItems();
    }

    private void recreateBillItems() {
        //Only remove Total and BillComponenbts,Fee and Sessions. NOT bill Entries
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;

        //billTotal = 0.0;
    }

    public String navigateToReportOpdPackage() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToReportOpdPackage()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_opd_package.xhtml?faces-redirect=true";
    }

    public String navigateToReportOpdPackageBill() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigateToReportOpdPackageBill()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_opd_package_bill.xhtml?faces-redirect=true";
    }

//    public String navigateToReportOpdPackageMedical() {
//        FacesContext context = FacesContext.getCurrentInstance();
//        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
//        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();
//
//        String url = request.getRequestURL().toString();
//
//        String ipAddress = request.getRemoteAddr();
//
//        AuditEvent auditEvent = new AuditEvent();
//        auditEvent.setEventStatus("Started");
//        long duration;
//        Date startTime = new Date();
//        auditEvent.setEventDataTime(startTime);
//        if (sessionController != null && sessionController.getDepartment() != null) {
//            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
//        }
//
//        if (sessionController != null && sessionController.getInstitution() != null) {
//            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
//        }
//        if (sessionController != null && sessionController.getLoggedUser() != null) {
//            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
//        }
//        auditEvent.setUrl(url);
//        auditEvent.setIpAddress(ipAddress);
//        auditEvent.setEventTrigger("navigateToReportOpdPackageMedical()");
//        auditEventApplicationController.logAuditEvent(auditEvent);
//
//        Date endTime = new Date();
//        duration = endTime.getTime() - startTime.getTime();
//        auditEvent.setEventDuration(duration);
//        auditEvent.setEventStatus("Completed");
//        auditEventApplicationController.logAuditEvent(auditEvent);
//        return "/reportCashier/report_opd_package_medical.xhtml?faces-redirect=true";
//    }

    public void calTotals() {
        double tot = 0.0;
        double dis = 0.0;

        for (BillEntry be : getLstBillEntries()) {
            BillItem bi = be.getBillItem();
            bi.setDiscount(0.0);
            bi.setGrossValue(0.0);
            bi.setNetValue(0.0);

            for (BillFee bf : be.getLstBillFees()) {
//                if (bf.getBillItem().getItem().isUserChangable() && bf.getBillItem().getItem().getDiscountAllowed() != true) {
                //////// // System.out.println("Total is " + tot);
                //    //////// // System.out.println("Bill Fee value is " + bf.getFeeValue());
                tot += bf.getFeeValue();
                //////// // System.out.println("After addition is " + tot);
                bf.getBillItem().setNetValue(bf.getBillItem().getNetValue() + bf.getFeeValue());
                bf.getBillItem().setGrossValue(bf.getBillItem().getGrossValue() + bf.getFeeValue());

            }
        }
        setDiscount(dis);
        setTotal(tot);
        setNetTotal(tot - dis);

    }

    public void feeChanged() {
        lstBillItems = null;
        getLstBillItems();
        calTotals();
    }

    public void clearBillValues() {
        setPatient(null);
        setPatient(null);
        setReferredBy(null);
        setCreditCompany(null);
        setYearMonthDay(null);
        setBills(null);
        paymentMethodData = null;
        setCurrentBillItem(null);
        setLstBillComponents(null);
        setLstBillEntries(null);
        setLstBillFees(null);
        setStaff(null);
        setPatientTabId("tabNewPt");
        lstBillEntries = new ArrayList<>();
        //   setForeigner(false);
        calTotals();

        setCashPaid(0.0);
        setDiscount(0.0);
        setCashBalance(0.0);
        printPreview = false;
    }

    public void prepareNewBill() {
        paymentMethodData = null;
        clearBillItemValues();
        clearBillValues();
        setPrintPreview(true);
        printPreview = false;

    }

    public void removeBillItem() {

        //TODO: Need to add Logic
        //////// // System.out.println(getIndex());
        if (getIndex() != null) {

            BillEntry temp = getLstBillEntries().get(getIndex());
            //////// // System.out.println("Removed Item:" + temp.getBillItem().getNetValue());
            recreateList(temp);
            calTotals();

        }
    }

    public void recreateList(BillEntry r) {
        List<BillEntry> temp = new ArrayList<>();
        for (BillEntry b : getLstBillEntries()) {
            if (b.getBillItem().getItem() != r.getBillItem().getItem()) {
                temp.add(b);
                //////// // System.out.println(b.getBillItem().getNetValue());
            }
        }
        lstBillEntries = temp;
        lstBillComponents = getBillBean().billComponentsFromBillEntries(lstBillEntries);
        lstBillFees = getBillBean().billFeesFromBillEntries(lstBillEntries);
    }

    public void onTabChange(TabChangeEvent event) {
        setPatientTabId(event.getTab().getId());

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

    public List<BillItem> getBilledBillItemLst() {
        return billedBillItemLst;
    }

    public void setBilledBillItemLst(List<BillItem> billedBillItemLst) {
        this.billedBillItemLst = billedBillItemLst;
    }

    public List<BillItem> getCanceledBillItemLst() {
        return canceledBillItemLst;
    }

    public void setCanceledBillItemLst(List<BillItem> canceledBillItemLst) {
        this.canceledBillItemLst = canceledBillItemLst;
    }

    public List<BillItem> getRefundedBillItemLst() {
        return refundedBillItemLst;
    }

    public void setRefundedBillItemLst(List<BillItem> refundedBillItemLst) {
        this.refundedBillItemLst = refundedBillItemLst;
    }

    public BillPackageMedicalController() {
    }

    private BillFacade getFacade() {
        return billFacade;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
        calTotals();
    }

    public String getPatientTabId() {
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
    }

    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
            patientDetailsEditable=true;
            Person p = new Person();

            patient.setPerson(p);
        }
        return patient;
    }

    public void setPatient(Patient newPatient) {
        this.patient = newPatient;
    }

    public Doctor getReferredBy() {

        return referredBy;
    }

    public void setReferredBy(Doctor referredBy) {
        this.referredBy = referredBy;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public List<BillComponent> getLstBillComponents() {
        if (lstBillComponents == null) {
            lstBillComponents = getBillBean().billComponentsFromBillEntries(getLstBillEntries());
        }

        return lstBillComponents;
    }

    public void setLstBillComponents(List<BillComponent> lstBillComponents) {
        this.lstBillComponents = lstBillComponents;
    }

    public List<BillFee> getLstBillFees() {
        if (lstBillFees == null) {
            lstBillFees = getBillBean().billFeesFromBillEntries(getLstBillEntries());
        }

        return lstBillFees;
    }

    public void setLstBillFees(List<BillFee> lstBillFees) {
        this.lstBillFees = lstBillFees;
    }

    public List<BillItem> getLstBillItems() {
        if (lstBillItems == null) {
            lstBillItems = new ArrayList<>();
        }
        return lstBillItems;
    }

    public void setLstBillItems(List<BillItem> lstBillItems) {
        this.lstBillItems = lstBillItems;
    }

    public List<BillEntry> getLstBillEntries() {
        if (lstBillEntries == null) {
            lstBillEntries = new ArrayList<>();
        }
        return lstBillEntries;
    }

    public void setLstBillEntries(List<BillEntry> lstBillEntries) {
        this.lstBillEntries = lstBillEntries;
    }

    public double getBilledBillTotal() {
        return billedBillTotal;
    }

    public void setBilledBillTotal(double billedBillTotal) {
        this.billedBillTotal = billedBillTotal;
    }

    public double getCanceledBillTotal() {
        return canceledBillTotal;
    }

    public void setCanceledBillTotal(double canceledBillTotal) {
        this.canceledBillTotal = canceledBillTotal;
    }

    public double getRefundedBillTotal() {
        return refundedBillTotal;
    }

    public void setRefundedBillTotal(double refundedBillTotal) {
        this.refundedBillTotal = refundedBillTotal;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(double cashPaid) {
        this.cashPaid = cashPaid;
        cashBalance = cashPaid - getNetTotal();
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
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

    public void dateChangeListen() {
        getPatient().getPerson().setDob(getCommonFunctions().guessDob(yearMonthDay));

    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;

    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;

    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    private Patient getTmpPatient() {
        return tmpPatient;
    }

    public void setTmpPatient(Patient tmpPatient) {
        this.tmpPatient = tmpPatient;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;

    }

    public BillSearch getBillSearch() {
        return billSearch;
    }

    public void setBillSearch(BillSearch billSearch) {
        this.billSearch = billSearch;
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

    public Date getFrmDate() {
        if (frmDate == null) {
            frmDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
        }
        return frmDate;
    }

    public void setFrmDate(Date frmDate) {
        this.frmDate = frmDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Item getServiceItem() {
        return ServiceItem;
    }

    public void setServiceItem(Item ServiceItem) {
        this.ServiceItem = ServiceItem;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

}
