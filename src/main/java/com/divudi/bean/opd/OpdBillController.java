package com.divudi.bean.opd;

import com.divudi.bean.cashTransaction.FinancialTransactionController;
import com.divudi.bean.common.*;
import com.divudi.bean.collectingCentre.CollectingCentreBillController;
import com.divudi.bean.hr.WorkingTimeController;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.FeeType;
import com.divudi.data.ItemLight;
import com.divudi.data.MessageType;
import com.divudi.data.OpdBillingStrategy;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.SmsSentResponse;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.BillListWithTotals;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillEjb;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.SmsManagerEjb;
import com.divudi.ejb.StaffBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillFeePayment;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.Payment;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.Sms;
import com.divudi.entity.Staff;
import com.divudi.entity.UserPreference;
import com.divudi.entity.WebUser;
import com.divudi.entity.hr.WorkingTime;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.OptionScope;
import com.divudi.entity.Token;
import com.divudi.facade.TokenFacade;
import com.divudi.java.CommonFunctions;
import com.divudi.light.common.BillLight;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
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
public class OpdBillController implements Serializable, ControllerWithPatient, ControllerWithMultiplePayments {

    private static final long serialVersionUID = 1L;

    /**
     * EJBs
     */
    @EJB
    private BillNumberGenerator billNumberGenerator;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private BillEjb billEjb;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    private CashTransactionBean cashTransactionBean;

    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private SmsFacade SmsFacade;
    @EJB
    private SmsManagerEjb smsManagerEjb;
    @EJB
    private TokenFacade tokenFacade;

    /**
     * Controllers
     */
    @Inject
    private BillController billController;
    @Inject
    private SessionController sessionController;
    @Inject
    private ItemController itemController;
    @Inject
    private ItemApplicationController itemApplicationController;
    @Inject
    private ItemMappingController itemMappingController;
    @Inject
    private CommonController commonController;
    @Inject
    private PaymentSchemeController paymentSchemeController;
    @Inject
    private ApplicationController applicationController;
    @Inject
    private EnumController enumController;
    @Inject
    private CollectingCentreBillController collectingCentreBillController;
    @Inject
    private PriceMatrixController priceMatrixController;
    @Inject
    private PatientController patientController;
    @Inject
    private AuditEventApplicationController auditEventApplicationController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private SearchController searchController;
    @Inject
    private AuditEventController auditEventController;
    @Inject
    private WorkingTimeController workingTimeController;
    @Inject
    private FinancialTransactionController financialTransactionController;
    @Inject
    private DepartmentController departmentController;
    @Inject
    ViewScopeDataTransferController viewScopeDataTransferController;

    @Inject
    OpdTokenController opdTokenController;

    @Inject
    ConfigOptionController configOptionController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    /**
     * Class Variables
     */
    private CommonFunctions commonFunctions;
    private ItemLight itemLight;
    private Long selectedItemLightId;
    private PaymentScheme paymentScheme;
    private PaymentMethod paymentMethod;
    private Patient patient;
    private Doctor referredBy;
    private Institution referredByInstitution;
    private String referralId;
    private Institution creditCompany;
    private Institution collectingCentre;
    private Staff staff;
    private Staff toStaff;
    private double total;
    private double discount;
    private double vat;
    private double netTotal;
    private double netPlusVat;
    private double cashPaid;
    private double cashBalance;
    private double cashRemain = cashPaid;
    private double remainMultiplePaymentBalance;
    private BillType billType;

    private Double grosTotal;
    private boolean foreigner = false;
    private Date sessionDate;
    private String strTenderedValue;
    private PaymentMethodData paymentMethodData;
    private Integer index;
    private boolean fromOpdEncounter = false;
    private String opdEncounterComments = "";
    private int patientSearchTab = 0;
    private String comment;
    private double opdPaymentCredit;
    private Date fromDate;
    private Date toDate;
    private Department department;
    private Institution institution;
    private Category category;
    private SearchKeyword searchKeyword;

    private Institution fromInstitution;
    private Institution toInstitution;
    private Department fromDepartment;
    private Department toDepartment;

    //Print Last Bill
    private Bill bill;
    private Bill batchBill;
    private Bill billPrint;

    private List<Bill> bills;
    private List<Bill> selectedBills;

    private BilledBill opdBill;
    private BillItem currentBillItem;

    private List<BillComponent> lstBillComponents;
    private List<BillComponent> lstBillComponentsPrint;

    private List<BillFee> lstBillFees;
    private List<BillFee> lstBillFeesPrint;

    private List<Payment> payments;

    private List<BillItem> lstBillItems;
    private List<BillItem> lstBillItemsPrint;

    private List<BillEntry> lstBillEntries;
    private List<Bill> billsPrint;

    private List<BillEntry> lstBillEntriesPrint;

    private List<BillLight> billLights;
    private BillLight billLight;

    private Long billId;
    private int opdSummaryIndex;
    private int opdAnalyticsIndex;

    private List<ItemLight> opdItems;
    private List<ItemLight> departmentOpdItems;
    private boolean patientDetailsEditable;

    private List<Staff> currentlyWorkingStaff;
    private Staff selectedCurrentlyWorkingStaff;
    List<BillSession> billSessions;
    private List<Department> opdItemDepartments;
    private Department selectedOpdItemDepartment;

    private boolean duplicatePrint;
    private Token token;

    private Double totalHospitalFee;
    private Double totalSaffFee;
    private boolean canChangeSpecialityAndDoctorInAddedBillItem;

    /**
     *
     * Navigation Methods
     *
     */
    public String navigateToSearchPatients() {
        patientController.clearSearchDetails();
        patientController.setSearchedPatients(null);
        return "/opd/patient_search?faces-redirect=true";
    }

    public String navigateToOpdAnalyticsIndex() {
        return "/opd/analytics/index?faces-redirect=true";
    }

    public String navigateToOpdOriginalBillPrint() {
        return "/opd/original_bill_reprint?faces-redirect=true";
    }

    public String navigateToOpdBatchBillList() {
        return "/opd/analytics/opd_batch_bill_search?faces-redirect=true";
    }

    public String navigateToSearchOpdBills() {
        batchBill = null;
        bills = null;
        return "/opd/opd_bill_search?faces-redirect=true";
    }

    public void fillOpdBillItems() {
        lstBillItems = new ArrayList<>();
        String jpql;
        Map m = new HashMap();
        jpql = "select i "
                + " from BillItem i"
                + " where i.retired=:ret"
                + " and i.bill.cancelled=:can"
                + " and i.bill.fromDepartment=:dep"
                + " and i.createdAt between :frm and :to";
        m.put("dep", fromDepartment);
        m.put("frm", fromDate);
        m.put("to", toDate);
        m.put("ret", false);
        m.put("can", false);
        lstBillItems = billItemFacade.findByJpql(jpql, m);
        if (lstBillItems == null) {
            return;
        }
        for (BillItem i : lstBillItems) {
            if (i.getBillFees() == null || i.getBillFees().isEmpty()) {
                i.setBillFees(billController.billFeesOfBillItem(i));
            }
        }
    }

    public void reloadCurrentlyWorkingStaff() {
        List<WorkingTime> wts = workingTimeController.findCurrentlyActiveWorkingTimes();
        currentlyWorkingStaff = new ArrayList<>();
        selectedCurrentlyWorkingStaff = null;
        if (wts == null) {
            return;
        }
        for (WorkingTime wt : wts) {
            if (wt.getStaffShift() != null && wt.getStaffShift().getStaff() != null) {
                currentlyWorkingStaff.add(wt.getStaffShift().getStaff());
                selectedCurrentlyWorkingStaff = wt.getStaffShift().getStaff();
            }
        }

    }

    public List<Item> completeOpdItems(String query) {
        UserPreference up = sessionController.getDepartmentPreference();
        switch (up.getOpdItemListingStrategy()) {
            case ALL_ITEMS:
                return itemController.completeServicesPlusInvestigationsAll(query);
            case ITEMS_MAPPED_TO_LOGGED_DEPARTMENT:
                return itemMappingController.completeItemByDepartment(query, sessionController.getDepartment());
            case ITEMS_MAPPED_TO_LOGGED_INSTITUTION:
                return itemMappingController.completeItemByInstitution(query, sessionController.getInstitution());
            case ITEMS_MAPPED_TO_SELECTED_DEPARTMENT:
                return itemMappingController.completeItemByDepartment(query, department);
            case ITEMS_MAPPED_TO_SELECTED_INSTITUTION:
                return itemMappingController.completeItemByInstitution(query, institution);
            case ITEMS_OF_LOGGED_DEPARTMENT:
                return itemController.completeItemsByDepartment(query, sessionController.getDepartment());
            case ITEMS_OF_LOGGED_INSTITUTION:
                return itemController.completeItemsByInstitution(query, sessionController.getInstitution());
            case ITEMS_OF_SELECTED_DEPARTMENT:
                return itemController.completeItemsByDepartment(query, department);
            case ITEMS_OF_SELECTED_INSTITUTIONS:
                return itemController.completeItemsByInstitution(query, institution);
            default:
                throw new AssertionError();
        }
    }

    public List<ItemLight> fillOpdItems() {
        UserPreference up = sessionController.getDepartmentPreference();
        List<ItemLight> temItems;
        switch (up.getOpdItemListingStrategy()) {
            case ALL_ITEMS:
                temItems = itemApplicationController.getInvestigationsAndServices();
                break;
            case ITEMS_MAPPED_TO_LOGGED_DEPARTMENT:
                temItems = itemMappingController.fillItemLightByDepartment(sessionController.getDepartment());
                break;
            case ITEMS_MAPPED_TO_LOGGED_INSTITUTION:
                temItems = itemMappingController.fillItemLightByInstitution(sessionController.getInstitution());
                break;
            case ITEMS_OF_LOGGED_DEPARTMENT:
                temItems = itemController.getDepartmentItems();
                break;
            case ITEMS_OF_LOGGED_INSTITUTION:
                temItems = itemController.getInstitutionItems();
                break;
            default:
                temItems = itemApplicationController.getInvestigationsAndServices();
                break;
        }
        boolean listItemsByDepartment = configOptionApplicationController.getBooleanValueByKey("List OPD Items by Department", false);
        if (listItemsByDepartment) {
            fillOpdItemDepartments(temItems);
        } else {
            opdItemDepartments = null;
        }
        if (getSelectedOpdItemDepartment() != null) {
            departmentOpdItems = filterItemLightesByDepartment(temItems, getSelectedOpdItemDepartment());
        }

        return temItems;
    }

    private List<ItemLight> filterItemLightesByDepartment(List<ItemLight> ils, Department dept) {
        boolean listItemsByDepartment = configOptionApplicationController.getBooleanValueByKey("List OPD Items by Department", false);
        if (!listItemsByDepartment || dept == null || dept.getId() == null) {
            return ils;
        }
        List<ItemLight> tils = new ArrayList<>();
        for (ItemLight il : ils) {
            if (il.getDepartmentId() != null && il.getDepartmentId().equals(dept.getId())) {
                tils.add(il);
            }
        }
        return tils;
    }

    public void departmentChanged() {
        if (selectedOpdItemDepartment == null) {
            departmentOpdItems = getOpdItems();
        } else {
            departmentOpdItems = filterItemLightesByDepartment(getOpdItems(), getSelectedOpdItemDepartment());
        }
    }

    public void fillOpdItemDepartments(List<ItemLight> itemLightsToAddDepartments) {
        opdItemDepartments = new ArrayList<>();
        Set<Long> uniqueDeptIds = new HashSet<>();
        for (ItemLight il : itemLightsToAddDepartments) {
            if (il.getDepartmentId() != null) {
                uniqueDeptIds.add(il.getDepartmentId());
            }
        }
        for (Long deptId : uniqueDeptIds) {
            Department d = departmentController.findDepartment(deptId);
            opdItemDepartments.add(d);
        }
    }

    public void searchDepartmentOpdBillLights() {
        Date startTime = new Date();
        billLights = searchController.listBillsLights(
                BillType.OpdBill,
                sessionController.getInstitution(),
                sessionController.getDepartment(),
                searchKeyword,
                getFromDate(),
                getToDate());

    }

    @Deprecated
    public String navigateToViewOpdBillByBillLight() {
        if (billLight == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }
        if (billLight.getId() == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }

        Bill tb = getFacade().find(billLight.getId());
        if (tb == null) {
            JsfUtil.addErrorMessage("No Bill");
            return null;
        }
        if (tb.getBillType() == null) {
            JsfUtil.addErrorMessage("No bill type");
            return null;
        }
        if (tb.getBillType() != BillType.OpdBill) {
            JsfUtil.addErrorMessage("Please Search Again and View Bill");
            bills = new ArrayList<>();
            return "";
        }

        Long batchBillId = null;

        if (tb.getBackwardReferenceBill() != null) {
            batchBillId = tb.getBackwardReferenceBill().getId();
        }
        if (batchBillId == null) {
            JsfUtil.addErrorMessage("No Batch Bill");
            return null;
        }
        batchBill = billFacade.find(batchBillId);
        String jpql;
        Map m = new HashMap();
        jpql = "select b "
                + " from Bill b"
                + " where b.backwardReferenceBill.id=:id";
        m.put("id", batchBillId);
        bills = getFacade().findByJpql(jpql, m);
        return "/opd/opd_batch_bill_print?faces-redirect=true;";
    }

    public String navigateToViewOpdBatchBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }
        if (bill.getId() == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }

        if (bill.getBillType() == null) {
            JsfUtil.addErrorMessage("No bill type");
            return null;
        }
        if (bill.getBillType() != BillType.OpdBill) {
            JsfUtil.addErrorMessage("Please Search Again and View Bill");
            bills = new ArrayList<>();
            return "";
        }

        Long batchBillId = null;

        if (bill.getBackwardReferenceBill() != null) {
            batchBillId = bill.getBackwardReferenceBill().getId();
        }
        if (batchBillId == null) {
            JsfUtil.addErrorMessage("No Batch Bill");
            return null;
        }
        batchBill = billFacade.find(batchBillId);
        String jpql;
        Map m = new HashMap();
        jpql = "select b "
                + " from Bill b"
                + " where b.backwardReferenceBill.id=:id";
        m.put("id", batchBillId);
        bills = getFacade().findByJpql(jpql, m);

        for (Bill b : bills) {
            getBillBean().checkBillItemFeesInitiated(b);
        }
        duplicatePrint = true;
        return "/opd/opd_batch_bill_print?faces-redirect=true;";
    }

    /**
     *
     * Getters & Setters
     *
     */
    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public double getCashRemain() {
        return cashRemain;
    }

    public void setCashRemain(double cashRemain) {
        this.cashRemain = cashRemain;
    }

    public String toAddNewCollectingCentre() {
        return "/admin/institutions/collecting_centre";
    }

//    public List<Bill> validBillsOfBatchBill(Bill batchBill) {
//        String j = "Select b from Bill b where b.backwardReferenceBill=:bb and b.cancelled=false";
//        Map m = new HashMap();
//        m.put("bb", batchBill);
//        return billFacade.findByJpql(j, m);
//    }
    public List<Bill> getSelectedBills() {
        return selectedBills;
    }

    public void setSelectedBills(List<Bill> selectedBills) {
        this.selectedBills = selectedBills;
    }

//    public void calculateSelectedBillTotals() {
//        BillListWithTotals bt = billEjb.calculateBillTotals(selectedBills);
//        grosTotal = bt.getGrossTotal();
//        netTotal = bt.getNetTotal();
//        discount = bt.getDiscount();
//        vat = bt.getVat();
//        netPlusVat = vat + netTotal;
//    }
    public void clear() {
        opdBill = new BilledBill();
        opdPaymentCredit = 0.0;
        comment = null;
        searchController.createTableByKeywordToPayBills();
    }

//    public void clearPharmacy() {
//        opdBill = new BilledBill();
//        opdPaymentCredit = 0.0;
//        comment = null;
//        searchController.createTablePharmacyCreditToPayBills();
//    }
//    public void saveBillOPDCredit() {
//
//        BilledBill temp = new BilledBill();
//
//        if (opdPaymentCredit == 0) {
//            JsfUtil.addErrorMessage("Please Select Correct Paid Amount");
//            return;
//        }
//        if (opdPaymentCredit > opdBill.getBalance()) {
//            JsfUtil.addErrorMessage("Please Enter Correct Paid Amount");
//            return;
//        }
//
//        temp.setReferenceBill(opdBill);
//        temp.setTotal(opdPaymentCredit);
//        temp.setPaidAmount(opdPaymentCredit);
//        temp.setNetTotal(opdPaymentCredit);
//
//        temp.setDeptId(getBillNumberGenerator().departmentBillNumberGenerator(getSessionController().getDepartment(), getSessionController().getDepartment(), BillType.CashRecieveBill, BillClassType.BilledBill));
//        temp.setInsId(getBillNumberGenerator().institutionBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), BillType.CashRecieveBill, BillClassType.BilledBill, BillNumberSuffix.NONE));
//        temp.setBillType(BillType.CashRecieveBill);
//
//        temp.setDepartment(getSessionController().getLoggedUser().getDepartment());
//        temp.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
//
//        temp.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
//        temp.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
//
//        temp.setToDepartment(getSessionController().getLoggedUser().getDepartment());
//
//        temp.setComments(comment);
//
//        getBillBean().setPaymentMethodData(temp, paymentMethod, getPaymentMethodData());
//
//        temp.setBillDate(new Date());
//        temp.setBillTime(new Date());
//        temp.setPaymentMethod(paymentMethod);
//        temp.setCreatedAt(new Date());
//        temp.setCreater(getSessionController().getLoggedUser());
//        getFacade().create(temp);
//        //create bill fee payments
//        //create bill fee payments
//        //create bill fee payments
//        //create bill fee payments
//        reminingCashPaid = opdPaymentCredit;
//
//        Payment p = createPayment(temp, paymentMethod);
//
//        String sql = "Select bi From BillItem bi where bi.retired=false and bi.bill.id=" + opdBill.getId();
//        List<BillItem> billItems = getBillItemFacade().findByJpql(sql);
//
//        for (BillItem bi : billItems) {
//            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
//
//            List<BillFee> billFees = getBillFeeFacade().findByJpql(sql);
//
//            calculateBillfeePayments(billFees, p);
//        }
//        opdBill.setBalance(opdBill.getBalance() - opdPaymentCredit);
//        opdBill.setCashPaid(calBillPaidValue(opdBill));
//        opdBill.setNetTotal(calBillPaidValue(opdBill));
//        getBillFacade().edit(opdBill);
//
//        JsfUtil.addSuccessMessage("Paid");
//        opdBill = temp;
//    }
//    public void saveBillPharmacyCredit() {
//
//        BilledBill temp = new BilledBill();
//
//        if (opdPaymentCredit == 0) {
//            JsfUtil.addErrorMessage("Please Select Correct Paid Amount");
//            return;
//        }
//        if (opdPaymentCredit > (opdBill.getNetTotal() - opdBill.getPaidAmount())) {
//            JsfUtil.addErrorMessage("Please Enter Correct Paid Amount");
//            return;
//        }
//
//        temp.setReferenceBill(opdBill);
//        temp.setTotal(opdPaymentCredit);
//        temp.setPaidAmount(opdPaymentCredit);
//        temp.setNetTotal(opdPaymentCredit);
//        ////// // System.out.println("opdBill.getPaidAmount() = " + opdBill.getPaidAmount());
//        ////// // System.out.println("opdPaymentCredit = " + opdPaymentCredit);
//        opdBill.setPaidAmount(opdPaymentCredit + opdBill.getPaidAmount());
//        ////// // System.out.println("opdBill.getPaidAmount() = " + opdBill.getPaidAmount());
//        getBillFacade().edit(opdBill);
//
//        temp.setDeptId(getBillNumberGenerator().departmentBillNumberGenerator(getSessionController().getDepartment(), getSessionController().getDepartment(), BillType.CashRecieveBill, BillClassType.BilledBill));
//        temp.setInsId(getBillNumberGenerator().institutionBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), BillType.CashRecieveBill, BillClassType.BilledBill, BillNumberSuffix.NONE));
//        temp.setBillType(BillType.CashRecieveBill);
//
//        temp.setDepartment(getSessionController().getLoggedUser().getDepartment());
//        temp.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
//
//        temp.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
//        temp.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
//
//        temp.setToDepartment(getSessionController().getLoggedUser().getDepartment());
//
//        temp.setComments(comment);
//
//        getBillBean().setPaymentMethodData(temp, paymentMethod, getPaymentMethodData());
//
//        temp.setBillDate(new Date());
//        temp.setBillTime(new Date());
//        temp.setPaymentMethod(paymentMethod);
//        temp.setCreatedAt(new Date());
//        temp.setCreater(getSessionController().getLoggedUser());
//        getFacade().create(temp);
//
//        JsfUtil.addSuccessMessage("Paid");
//        opdBill = temp;
//
//    }
    public BillNumberGenerator getBillNumberGenerator() {
        return billNumberGenerator;
    }

    public StaffBean getStaffBean() {
        return staffBean;
    }

    public void searchPatientListener() {
        //   createPaymentSchemeItems();
        calTotals();
    }

    public Institution getReferredByInstitution() {
        return referredByInstitution;
    }

    public void setReferredByInstitution(Institution referredByInstitution) {
        this.referredByInstitution = referredByInstitution;
    }

//    public int getRecurseCount() {
//        return recurseCount;
//    }
//
//    public void setRecurseCount(int recurseCount) {
//        this.recurseCount = recurseCount;
//    }
    public boolean findByFilter(String property, String value) {
        String sql = "Select b From Bill b where b.retired=false and (b." + property + ") like '%" + value.toUpperCase() + " %'";
        Bill b = getBillFacade().findFirstByJpql(sql);
        //System.err.println("SQL " + sql);
        //System.err.println("Bill " + b);
        if (b != null) {
            return true;
        } else {
            return false;
        }
    }

    public void feeChangeListener(BillFee bf) {
        if (bf.getFeeGrossValue() == null) {
            bf.setFeeGrossValue(0.0);
//            return;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Disable increasing the fee value in OPD Billing", false)) {
            if (bf.getFeeValue() < bf.getFeeGrossValue()) {
                JsfUtil.addErrorMessage("Increasing the fee value is not allowed.");
                bf.setFeeGrossValue(bf.getFeeValue());
                return;
            }
        }
        if (configOptionApplicationController.getBooleanValueByKey("Disable decreasing the fee value in OPD Billing", false)) {

            if (bf.getFeeValue() > bf.getFeeGrossValue()) {
                bf.setFeeGrossValue(bf.getFeeValue());
                JsfUtil.addErrorMessage("Decreasing the fee value is not allowed.");
                return;
            }
        }
        lstBillItems = null;
        getLstBillItems();
        bf.setTmpChangedValue(bf.getFeeGrossValue());
        calTotals();
        JsfUtil.addSuccessMessage("Fee Changed Successfully");
    }

    public void changeBillDoctorByFee(BillFee bf) {
        if (bf == null) {
            return;
        }
        if (bf.getStaff() == null) {
            return;
        }
        getCurrentlyWorkingStaff().add(bf.getStaff());
        selectedCurrentlyWorkingStaff = bf.getStaff();
    }

    public String getStrTenderedValue() {
        return strTenderedValue;
    }

    public void setStrTenderedValue(String strTenderedValue) {

        this.strTenderedValue = strTenderedValue;
        try {
            cashPaid = Double.parseDouble(strTenderedValue);
        } catch (NumberFormatException e) {
            //////// // System.out.println("Error in converting tendered value. \n " + e.getMessage());
        }
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

//    public List<Bill> completeOpdCreditBill(String qry) {
//        List<Bill> a = null;
//        String sql;
//        HashMap hash = new HashMap();
//        if (qry != null) {
//            sql = "select c from BilledBill c "
//                    + " where abs(c.netTotal)-abs(c.paidAmount)>:val "
//                    + " and c.billType= :btp "
//                    + " and c.paymentMethod= :pm "
//                    + " and c.cancelledBill is null "
//                    + " and c.refundedBill is null "
//                    + " and c.retired=false "
//                    + " and ((c.insId) like :q or"
//                    + " (c.patient.person.name) like :q "
//                    + " or (c.creditCompany.name) like :q ) "
//                    + " order by c.creditCompany.name";
//            hash.put("btp", BillType.OpdBill);
//            hash.put("pm", PaymentMethod.Credit);
//            hash.put("val", 0.1);
//            hash.put("q", "%" + qry.toUpperCase() + "%");
//            a = getFacade().findByJpql(sql, hash);
//        }
//        if (a == null) {
//            a = new ArrayList<>();
//        }
//        return a;
//    }
//    public List<Bill> completePharmacyCreditBill(String qry) {
//        List<Bill> a = null;
//        String sql;
//        HashMap hash = new HashMap();
//        if (qry != null) {
//            sql = "select b from BilledBill b "
//                    + " where (abs(b.netTotal)-abs(b.paidAmount))>:val "
//                    + " and b.billType in :btps "
//                    + " and b.paymentMethod= :pm "
//                    + " and b.institution=:ins "
//                    //                    + " and b.department=:dep "
//                    + " and b.retired=false "
//                    + " and b.refunded=false "
//                    + " and b.cancelled=false "
//                    + " and b.toStaff is null "
//                    + " and ( (b.insId) like :q or "
//                    + " (b.deptId) like :q or "
//                    + " (b.toInstitution.name) like :q ) "
//                    + " order by b.deptId ";
//            hash.put("btps", Arrays.asList(new BillType[]{BillType.PharmacyWholeSale, BillType.PharmacySale}));
//            hash.put("pm", PaymentMethod.Credit);
//            hash.put("val", 0.1);
//            hash.put("q", "%" + qry.toUpperCase() + "%");
//            hash.put("ins", getSessionController().getInstitution());
////            hash.put("dep", getSessionController().getDepartment());
////            //// // System.out.println("hash = " + hash);
////            //// // System.out.println("sql = " + sql);
////            //// // System.out.println("getSessionController().getInstitution().getName() = " + getSessionController().getInstitution().getName());
////            //// // System.out.println("getSessionController().getDepartment().getName() = " + getSessionController().getDepartment().getName());
//            a = getFacade().findByJpql(sql, hash);
//        }
//        if (a == null) {
//            a = new ArrayList<>();
//        }
//        return a;
//    }
//    public List<Bill> completeBillFromDealor(String qry) {
//        List<Bill> a = null;
//        String sql;
//        HashMap hash = new HashMap();
//        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill, BillType.StoreGrnBill, BillType.StorePurchase};
//        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
//        if (qry != null) {
//            sql = "select c from BilledBill c "
//                    + "where  abs(c.netTotal)-abs(c.paidAmount)>:val "
//                    + " and c.billType in :bts "
//                    + " and c.createdAt is not null "
//                    + " and c.deptId is not null "
//                    + " and c.cancelledBill is null "
//                    + " and c.retired=false "
//                    + " and c.paymentMethod=:pm  "
//                    + " and (((c.deptId) like :q ) "
//                    + " or ((c.fromInstitution.name)  like :q ))"
//                    + " order by c.fromInstitution.name";
//            hash.put("bts", billTypesListBilled);
//            hash.put("pm", PaymentMethod.Credit);
//            hash.put("val", 0.1);
//            hash.put("q", "%" + qry.toUpperCase() + "%");
//            //     hash.put("pm", PaymentMethod.Credit);
//            a = getFacade().findByJpql(sql, hash, 20);
//        }
//        if (a == null) {
//            a = new ArrayList<>();
//        }
//        return a;
//    }
//    public List<Bill> completeBillFromDealorStore(String qry) {
//        List<Bill> a = null;
//        String sql;
//        HashMap hash = new HashMap();
//        if (qry != null) {
//            sql = "select c from BilledBill c "
//                    + "where  abs(c.netTotal)-abs(c.paidAmount)>:val "
//                    + " and (c.billType= :btp1 or c.billType= :btp2  )"
//                    + " and c.createdAt is not null "
//                    + " and c.deptId is not null "
//                    + " and c.cancelledBill is null "
//                    + " and c.retired=false "
//                    + " and c.paymentMethod=:pm  "
//                    + " and c.fromInstitution.institutionType=:insTp  "
//                    + " and (((c.deptId) like :q ) "
//                    + " or ((c.fromInstitution.name)  like :q ))"
//                    + " order by c.fromInstitution.name";
//            hash.put("btp1", BillType.PharmacyGrnBill);
//            hash.put("btp2", BillType.PharmacyPurchaseBill);
//            hash.put("pm", PaymentMethod.Credit);
//            hash.put("insTp", InstitutionType.StoreDealor);
//            hash.put("val", 0.1);
//            hash.put("q", "%" + qry.toUpperCase() + "%");
//            //     hash.put("pm", PaymentMethod.Credit);
//            a = getFacade().findByJpql(sql, hash, 10);
//        }
//        if (a == null) {
//            a = new ArrayList<>();
//        }
//        return a;
//    }
//    public List<Bill> completeSurgeryBills(String qry) {
//
//        String sql;
//        Map temMap = new HashMap();
//        sql = "select b from BilledBill b "
//                + " where b.billType = :billType "
//                + " and b.cancelled=false "
//                + " and b.retired=false "
//                + " and b.patientEncounter.discharged=false ";
//
//        sql += " and  (((b.patientEncounter.patient.person.name) like :q )";
//        sql += " or  ((b.patientEncounter.bhtNo) like :q )";
//        sql += " or  ((b.insId) like :q )";
//        sql += " or  ((b.procedure.item.name) like :q ))";
//        sql += " order by b.insId desc  ";
//
//        temMap.put("billType", BillType.SurgeryBill);
//        temMap.put("q", "%" + qry.toUpperCase() + "%");
//        List<Bill> tmps = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 20);
//
//        return tmps;
//    }
//    public List<Bill> getDealorBills(Institution institution, List<BillType> billTypes) {
//        String sql;
//        HashMap hash = new HashMap();
//
//        sql = "select c from BilledBill c where "
//                + " abs(c.netTotal)-abs(c.paidAmount)>:val"
//                + " and c.billType in :bts"
//                + " and c.createdAt is not null "
//                + " and c.deptId is not null "
//                + " and c.cancelled=false"
//                + " and c.retired=false"
//                + " and c.paymentMethod=:pm  "
//                + " and c.fromInstitution=:ins "
//                + " order by c.id ";
//        hash.put("bts", billTypes);
//        hash.put("pm", PaymentMethod.Credit);
//        hash.put("val", 0.1);
//        hash.put("ins", institution);
//        //     hash.put("pm", PaymentMethod.Credit);
//        List<Bill> bill = getFacade().findByJpql(sql, hash);
//
//        if (bill == null) {
//            bill = new ArrayList<>();
//        }
//
//        return bill;
//    }
    public List<Bill> getCreditBills(Institution institution) {
        String sql;
        HashMap hash = new HashMap();

        sql = "select c from BilledBill c  where"
                + " abs(c.netTotal)-abs(c.paidAmount)>:val "
                + " and c.billType= :btp"
                + " and c.createdAt is not null "
                + " and c.deptId is not null "
                + " and c.cancelled=false"
                + " and c.retired=false"
                + " and c.paymentMethod=:pm  "
                + " and c.creditCompany=:ins "
                + " order by c.id ";
        hash.put("btp", BillType.OpdBill);
        hash.put("pm", PaymentMethod.Credit);
        hash.put("val", 0.1);
        hash.put("ins", institution);
        //     hash.put("pm", PaymentMethod.Credit);
        List<Bill> bill = getFacade().findByJpql(sql, hash);

        if (bill == null) {
            bill = new ArrayList<>();
        }

        return bill;
    }

    public List<Bill> getCreditBillsPharmacy(Institution institution) {
        String sql;
        HashMap hash = new HashMap();

        sql = "select b from BilledBill b  where"
                + " (abs(b.netTotal)-abs(b.paidAmount))>:val "
                + " and b.billType in :btps"
                + " and b.createdAt is not null "
                + " and b.deptId is not null "
                + " and b.cancelled=false"
                + " and b.retired=false"
                + " and b.paymentMethod=:pm  "
                + " and b.toInstitution=:company "
                + " and b.institution=:ins "
                //                + " and b.department=:dep "
                + " order by b.id ";
        hash.put("btps", Arrays.asList(new BillType[]{BillType.PharmacyWholeSale, BillType.PharmacySale}));
        hash.put("pm", PaymentMethod.Credit);
        hash.put("val", 0.1);
        hash.put("company", institution);
        hash.put("ins", getSessionController().getInstitution());
//        hash.put("dep", getSessionController().getDepartment());
        //     hash.put("pm", PaymentMethod.Credit);
        List<Bill> bill = getFacade().findByJpql(sql, hash);

        if (bill == null) {
            bill = new ArrayList<>();
        }

        return bill;
    }

    public List<Bill> getCreditBills(Institution institution, Date fd, Date td) {
        String sql;
        HashMap m = new HashMap();

        sql = "select c from BilledBill c  where"
                + " abs(c.netTotal)-abs(c.paidAmount)>:val "
                + " and c.billType= :btp"
                + " and c.createdAt between :fd and :td "
                + " and c.deptId is not null "
                + " and c.cancelled=false"
                + " and c.retired=false"
                + " and c.paymentMethod=:pm  "
                + " and c.creditCompany=:ins "
                + " order by c.id ";
        m.put("btp", BillType.OpdBill);
        m.put("pm", PaymentMethod.Credit);
        m.put("val", 0.1);
        m.put("ins", institution);
        m.put("fd", fd);
        m.put("td", td);
        List<Bill> bill = getFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        if (bill == null) {
            bill = new ArrayList<>();
        }
        return bill;
    }

    public List<Bill> getBills(Date fromDate, Date toDate, BillType billType1, BillType billType2, Institution institution) {
        String sql;
        HashMap hm = new HashMap();
        sql = "Select b From Bill b where"
                + "  b.retired=false"
                + "  and b.createdAt between :frm and :to"
                + " and (b.fromInstitution=:ins "
                + " or b.toInstitution=:ins) "
                + " and (b.billType=:tp1"
                + " or b.billType=:tp2)";
        hm.put("frm", fromDate);
        hm.put("to", toDate);
        hm.put("ins", institution);
        hm.put("tp1", billType1);
        hm.put("tp2", billType2);
        return getBillFacade().findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public void getOpdBills() {
        Date startTime = new Date();

        BillType[] billTypes = {BillType.OpdBill};
        BillListWithTotals r = billEjb.findBillsAndTotals(fromDate, toDate, billTypes, null, department, institution, null);
        if (r == null) {
            r = new BillListWithTotals();
            bills = r.getBills();
            netTotal = r.getNetTotal();
            discount = r.getDiscount();
            grosTotal = r.getGrossTotal();
            vat = r.getVat();
            return;
        }
        if (r.getBills() != null) {
            bills = r.getBills();
        }
        if (r.getNetTotal() != null) {
            netTotal = r.getNetTotal();
        }
        if (r.getDiscount() != null) {
            discount = r.getDiscount();
        }
        if (r.getVat() != null) {
            vat = r.getVat();
        }
        if (r.getGrossTotal() != null) {
            grosTotal = r.getGrossTotal();
        }

    }

    public void onLineSettleBills() {
        Date startTime = new Date();

        BillType[] billTypes = {BillType.OpdBill, billType.InwardPaymentBill};
        PaymentMethod[] paymentMethods = {PaymentMethod.OnlineSettlement};
        BillListWithTotals r = billEjb.findBillsAndTotals(fromDate, toDate, billTypes, null, department, institution, paymentMethods);
        if (r == null) {
            r = new BillListWithTotals();
            bills = r.getBills();
            netTotal = r.getNetTotal();
            discount = r.getDiscount();
            grosTotal = r.getGrossTotal();
            vat = r.getVat();
            return;
        }
        if (r.getBills() != null) {
            bills = r.getBills();
        }
        if (r.getNetTotal() != null) {
            netTotal = r.getNetTotal();
        }
        if (r.getDiscount() != null) {
            discount = r.getDiscount();
        }
        if (r.getVat() != null) {
            vat = r.getVat();
        }
        if (r.getGrossTotal() != null) {
            grosTotal = r.getGrossTotal();
        }

    }

    public void getPharmacySaleBills() {
        Date startTime = new Date();

        BillType[] billTypes;
        if (billType == null) {
            billTypes = new BillType[]{BillType.PharmacySale, BillType.PharmacyWholeSale};
        } else {
            billTypes = new BillType[]{billType};
        }

        BillListWithTotals r = null;

        if (paymentMethod == null) {
            r = billEjb.findBillsAndTotals(fromDate, toDate, billTypes, null, department, institution, null);
        } else {
            PaymentMethod[] pms = new PaymentMethod[]{
                paymentMethod,};
            r = billEjb.findBillsAndTotals(fromDate, toDate, billTypes, null, department, institution, pms);
        }

        if (r == null) {
            r = new BillListWithTotals();
            bills = r.getBills();
            netTotal = r.getNetTotal();
            discount = r.getDiscount();
            vat = r.getVat();
            grosTotal = r.getGrossTotal();
            return;
        }
        if (r.getBills() != null) {
            bills = r.getBills();
        }
        if (r.getNetTotal() != null) {
            netTotal = r.getNetTotal();
        }
        if (r.getDiscount() != null) {
            discount = r.getDiscount();
        }
        if (r.getVat() != null) {
            vat = r.getVat();
        }
        if (r.getGrossTotal() != null) {
            grosTotal = r.getGrossTotal();
        }

    }

    public Double getGrosTotal() {
        return grosTotal;
    }

    public void setGrosTotal(Double grosTotal) {
        this.grosTotal = grosTotal;
    }

    public void getPharamacyWholeSaleCreditBills() {
        Date startTime = new Date();

        BillType[] billTypes = {BillType.PharmacyWholeSale};
        PaymentMethod[] paymentMethods = {PaymentMethod.Credit};
        BillListWithTotals r = billEjb.findBillsAndTotals(fromDate, toDate, billTypes, null, department, institution, paymentMethods);
        bills = r.getBills();
        netTotal = r.getNetTotal();
        discount = r.getDiscount();
        grosTotal = r.getGrossTotal();
        vat = r.getVat();

    }

    public void getPharmacyBills() {
        Date startTime = new Date();

        BillType[] billTypes = {BillType.PharmacySale};
        BillListWithTotals r = billEjb.findBillsAndTotals(fromDate, toDate, billTypes, null, department, institution, null);
        bills = r.getBills();
        netTotal = r.getNetTotal();
        discount = r.getDiscount();
        grosTotal = r.getGrossTotal();
        vat = r.getVat();

    }

    public void getPharmacyBillsBilled() {
        Date startTime = new Date();

        BillType[] billTypes = {BillType.PharmacyPre};
        BillListWithTotals r = billEjb.findBillsAndTotals(fromDate, toDate, billTypes, null, department, institution, null);
        bills = r.getBills();
        netTotal = r.getNetTotal();
        discount = r.getDiscount();
        grosTotal = r.getGrossTotal();
        vat = r.getVat();

    }

    public void getPharmacyWholeBills() {
        Date startTime = new Date();

        BillType[] billTypes = {BillType.PharmacySale};
        BillListWithTotals r = billEjb.findBillsAndTotals(fromDate, toDate, billTypes, null, department, institution, null);
        bills = r.getBills();
        netTotal = r.getNetTotal();
        discount = r.getDiscount();
        grosTotal = r.getGrossTotal();
        vat = r.getVat();

    }

    public BillEjb getBillEjb() {
        return billEjb;
    }

    public void setBillEjb(BillEjb billEjb) {
        this.billEjb = billEjb;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public SearchController getSearchController() {
        return searchController;
    }

    public void setSearchController(SearchController searchController) {
        this.searchController = searchController;
    }

    public MembershipSchemeController getMembershipSchemeController() {
        return membershipSchemeController;
    }

    public void setMembershipSchemeController(MembershipSchemeController membershipSchemeController) {
        this.membershipSchemeController = membershipSchemeController;
    }

    public Date getSessionDate() {
        if (sessionDate == null) {
            sessionDate = Calendar.getInstance().getTime();
        }
        return sessionDate;
    }

    public void setSessionDate(Date sessionDate) {
        this.sessionDate = sessionDate;
    }

    public boolean isForeigner() {
        return foreigner;
    }

    public void setForeigner(boolean foreigner) {
        this.foreigner = foreigner;
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

    public String changeTextCases(String nm, String tc) {
        if (tc == null) {
            return nm;
        }
        switch (tc.toUpperCase()) {
            case "UPPERCASE":
                return nm.toUpperCase();
            case "LOWERCASE":
                return nm.toLowerCase();
            case "CAPITALIZE":
                return capitalizeFirstLetter(nm);
            default:
                return nm;
        }
    }

    public String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        String[] words = str.split("\\s");
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return result.toString().trim();
    }

    private void savePatient() {
        if (getPatient().getId() == null) {
            if (getPatient().getPerson().getName() != null) {
                String updatedPatientName;
                updatedPatientName = changeTextCases(getPatient().getPerson().getName(), getSessionController().getApplicationPreference().getChangeTextCasesPatientName());
                getPatient().getPerson().setName(updatedPatientName);
            }
            getPatient().setPhn(applicationController.createNewPersonalHealthNumber(getSessionController().getInstitution()));
            getPatient().setCreatedInstitution(getSessionController().getInstitution());
            getPatient().setCreater(getSessionController().getLoggedUser());
            getPatient().setCreatedAt(new Date());
            if (getPatient().getPerson().getId() != null) {
//                getPatientFacade().edit(getPatient());
                getPersonFacade().edit(getPatient().getPerson());
            } else {
                getPatient().getPerson().setCreater(getSessionController().getLoggedUser());
                getPatient().getPerson().setCreatedAt(new Date());
//                getPatientFacade().create(getPatient());
                getPersonFacade().create(getPatient().getPerson());
            }
            try {
                getPatientFacade().create(getPatient());
            } catch (Exception e) {
                getPatientFacade().edit(getPatient());
            }
        } else {
            if (getPatient().getPerson().getId() != null) {
//                getPatientFacade().edit(getPatient());
                getPersonFacade().edit(getPatient().getPerson());
            } else {
                getPatient().getPerson().setCreater(getSessionController().getLoggedUser());
                getPatient().getPerson().setCreatedAt(new Date());
//                getPatientFacade().create(getPatient());
                getPersonFacade().create(getPatient().getPerson());
            }
        }
    }

    public void sendSmsOnOpdBillSettling(UserPreference ap, String smsMessage) {
        Sms s = new Sms();
        s.setPending(false);
        s.setBill(batchBill);
        s.setCreatedAt(new Date());
        s.setCreater(sessionController.getLoggedUser());
        s.setDepartment(sessionController.getLoggedUser().getDepartment());
        s.setInstitution(sessionController.getLoggedUser().getInstitution());
        if (getPatient().getPatientPhoneNumber() != null) {
            s.setReceipientNumber(getPatient().getPatientPhoneNumber().toString());
        } else {
            s.setReceipientNumber(getPatient().getPerson().getSmsNumber());
        }
        String messageBody = smsMessage;
        s.setSendingMessage(messageBody);
        s.setSmsType(MessageType.OpdBillSettle);
        getSmsFacade().create(s);

        Boolean sent = smsManagerEjb.sendSms(s);
        if (sent) {
            JsfUtil.addSuccessMessage("Sms send");
        } else {
            JsfUtil.addErrorMessage("Sending SMS Failed.");
        }
    }

    public boolean createMultipleBillsFromOrderListOld() {
        bills = new ArrayList<>();
        Set<Department> billDepts = new HashSet<>();
        for (BillEntry e : lstBillEntries) {
            billDepts.add(e.getBillItem().getItem().getDepartment());
            // Category is available as Category c=e.getBillItem().getItem().getCategory();
        }

        for (Department d : billDepts) {
            Bill myBill = new BilledBill();
            myBill = saveBill(d, myBill);
            if (myBill == null) {
                return false;
            }
            List<BillEntry> tmp = new ArrayList<>();
            for (BillEntry e : lstBillEntries) {
                if (Objects.equals(e.getBillItem().getItem().getDepartment().getId(), d.getId())) {
                    BillItem bi = getBillBean().saveBillItem(myBill, e, getSessionController().getLoggedUser());
                    myBill.getBillItems().add(bi);
                    tmp.add(e);
                }
            }
            if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
                myBill.setCashPaid(cashPaid);
            }
            getBillFacade().edit(myBill);
            getBillBean().calculateBillItems(myBill, tmp);
            createPaymentsForBills(myBill, tmp);
            getBillBean().checkBillItemFeesInitiated(myBill);
            getBills().add(myBill);
        }

        return true;
    }

    public boolean createMultipleBillsFromOrderList() {
        bills = new ArrayList<>();
        OpdBillingStrategy strategy = sessionController.getDepartmentPreference().getOpdBillingStrategy();
        if (strategy == OpdBillingStrategy.ONE_BILL_PER_DEPARTMENT) {
            // Process bills by each department
            return processBillsByDepartment();
        } else if (strategy == OpdBillingStrategy.ONE_BILL_PER_DEPARTMENT_AND_CATEGORY) {
            // Process bills by each department and each category
            return processBillsByDepartmentAndCategory();
        }

        return false; // Fallback case, should not reach here.
    }

    private boolean processBillsByDepartment() {
        System.out.println("processBillsByDepartment");
        Set<Department> billDepts = new HashSet<>();
        for (BillEntry e : lstBillEntries) {
            billDepts.add(e.getBillItem().getItem().getDepartment());
        }
        for (Department d : billDepts) {
            System.out.println("d = " + d);
            Bill myBill = new BilledBill();
            myBill = saveBill(d, myBill);
            if (myBill == null) {
                return false;
            }
            List<BillEntry> tmp = new ArrayList<>();
            for (BillEntry e : lstBillEntries) {
                if (Objects.equals(e.getBillItem().getItem().getDepartment().getId(), d.getId())) {
                    BillItem bi = getBillBean().saveBillItem(myBill, e, getSessionController().getLoggedUser());
                    myBill.getBillItems().add(bi);
                    tmp.add(e);
                }
            }
            if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
                myBill.setCashPaid(cashPaid);
            }
            getBillFacade().edit(myBill);
            getBillBean().calculateBillItems(myBill, tmp);
            createPaymentsForBills(myBill, tmp);
            getBillBean().checkBillItemFeesInitiated(myBill);
            getBills().add(myBill);
        }
        return true;
    }

    private boolean processBillsByDepartmentAndCategory() {
        Map<Department, Set<Category>> billDeptCats = new HashMap<>();
        // Collecting unique Departments and Categories
        for (BillEntry e : lstBillEntries) {
            Department dept = e.getBillItem().getItem().getDepartment();
            Category cat = e.getBillItem().getItem().getCategory();
            billDeptCats.computeIfAbsent(dept, k -> new HashSet<>()).add(cat);
        }
        // Processing each Department and Category
        for (Map.Entry<Department, Set<Category>> entry : billDeptCats.entrySet()) {
            Department d = entry.getKey();
            for (Category c : entry.getValue()) {
                Bill myBill = new BilledBill();
                myBill = saveBill(d, c, myBill); // Saving the bill for each Department and Category
                if (myBill == null) {
                    return false;
                }
                List<BillEntry> tmp = new ArrayList<>();

                // Adding BillItems to the Bill
                for (BillEntry e : lstBillEntries) {
                    if (Objects.equals(e.getBillItem().getItem().getDepartment().getId(), d.getId())
                            && Objects.equals(e.getBillItem().getItem().getCategory().getId(), c.getId())) {
                        BillItem bi = getBillBean().saveBillItem(myBill, e, getSessionController().getLoggedUser());
                        myBill.getBillItems().add(bi);
                        tmp.add(e);
                    }
                }

                // Handling partial payments if allowed
                if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
                    myBill.setCashPaid(cashPaid);
                }

                // Finalizing the Bill
                getBillFacade().edit(myBill);
                getBillBean().calculateBillItems(myBill, tmp);
                createPaymentsForBills(myBill, tmp);
                getBillBean().checkBillItemFeesInitiated(myBill);

                // Adding the finalized Bill to the list of Bills
                getBills().add(myBill);
            }
        }

        return true;
    }

    public void setPrintigBill() {
        ////// // System.out.println("In Print");
        billPrint = bill;
        billsPrint = bills;
        lstBillComponentsPrint = lstBillComponents;
        lstBillEntriesPrint = lstBillEntries;
        lstBillFeesPrint = lstBillFees;
        lstBillItemsPrint = lstBillItems;
        ////// // System.out.println("Out Print");
    }

    public String settleOpdBill() {
        String eventUuid = auditEventController.createAuditEvent("OPD Bill Controller - Settle OPD Bill");
        if (!executeSettleBillActions()) {
            auditEventController.updateAuditEvent(eventUuid);
            return "";
        }
        UserPreference ap = sessionController.getApplicationPreference();
        if (ap.getSmsTemplateForOpdBillSetting() != null && !ap.getSmsTemplateForOpdBillSetting().trim().equals("")) {
            sendSmsOnOpdBillSettling(ap, ap.getSmsTemplateForOpdBillSetting());
        }

        auditEventController.updateAuditEvent(eventUuid);
        return "/opd/opd_batch_bill_print?faces-redirect=true";
    }

    private boolean executeSettleBillActions() {
        if (errorCheck()) {
            return false;
        }
        savePatient();
//        int numberOfBillsForTheOrder = getBillBean().calculateNumberOfBillsPerOrder(getLstBillEntries());

        boolean oneOpdBillForAllDepartments = configOptionApplicationController.getBooleanValueByKey("One OPD Bill For All Departments and Categories", true);
        boolean oneOpdBillForEachDepartment = configOptionApplicationController.getBooleanValueByKey("One OPD Bill For Each Department", false);
        boolean oneOpdBillForEachCategory = configOptionApplicationController.getBooleanValueByKey("One OPD Bill For Each Category", false);
        boolean oneOpdBillForEachDepartmentAndCategoryCombination = configOptionApplicationController.getBooleanValueByKey("One OPD Bill For Each Department and Category Combination", false);

        BilledBill newBatchBill = new BilledBill();

        if (oneOpdBillForAllDepartments) {
            System.out.println("oneOpdBillForAllDepartments");
            Bill newSingleBill = null;
            newSingleBill = saveBill(sessionController.getDepartment(), newBatchBill);
            if (newSingleBill == null) {
                return false;
            }
            List<BillItem> list = new ArrayList<>();
            for (BillEntry billEntry : getLstBillEntries()) {
                list.add(getBillBean().saveBillItem(newSingleBill, billEntry, getSessionController().getLoggedUser()));
            }
            newSingleBill.setBillItems(list);
            newSingleBill.setBillTotal(newSingleBill.getNetTotal());
            getBillFacade().edit(newSingleBill);
            getBillBean().calculateBillItems(newSingleBill, getLstBillEntries());
            if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
                newSingleBill.setCashPaid(cashPaid);
                if (cashPaid >= newSingleBill.getTransSaleBillTotalMinusDiscount()) {
                    newSingleBill.setBalance(0.0);
                    newSingleBill.setNetTotal(newSingleBill.getTransSaleBillTotalMinusDiscount());
                } else {
                    newSingleBill.setBalance(newSingleBill.getTransSaleBillTotalMinusDiscount() - newSingleBill.getCashPaid());
                    newSingleBill.setNetTotal(newSingleBill.getCashPaid());
                }
            }
            newSingleBill.setVat(newSingleBill.getVat());
            newSingleBill.setVatPlusNetTotal(newSingleBill.getNetTotal() + newSingleBill.getVat());

            getBillFacade().edit(newSingleBill);
            getBillBean().checkBillItemFeesInitiated(newSingleBill);
            getBills().add(newSingleBill);
        } else if (oneOpdBillForEachDepartmentAndCategoryCombination) {
            processBillsByDepartmentAndCategory();
        } else if (oneOpdBillForEachDepartment) {
            System.out.println("oneOpdBillForEachDepartment");
            processBillsByDepartment();
        } else if (oneOpdBillForEachCategory) {
            JsfUtil.addErrorMessage("Still Under Development");
            return false;
        } else {
            JsfUtil.addErrorMessage("Still Under Development");
            return false;
        }

        saveBatchBill();
        createPaymentsForBills(getBatchBill(), getLstBillEntries());
        saveBillItemSessions();

        if (toStaff != null && getPaymentMethod() == PaymentMethod.Staff_Welfare) {
            staffBean.updateStaffWelfare(toStaff, netPlusVat);
            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
        } else if (toStaff != null && getPaymentMethod() == PaymentMethod.Staff) {
            staffBean.updateStaffCredit(toStaff, netPlusVat);
            JsfUtil.addSuccessMessage("Staff Credit Updated");
        }

        if (paymentMethod == PaymentMethod.PatientDeposit) {
            if (getPatient().getRunningBalance() != null) {
                getPatient().setRunningBalance(getPatient().getRunningBalance() - netTotal);
            } else {
                getPatient().setRunningBalance(0.0 - netTotal);
            }
            getPatientFacade().edit(getPatient());
        }

        if (getToken() != null) {
            getToken().setBill(getBatchBill());
            tokenFacade.edit(getToken());
            markToken(getBatchBill());
        }

        JsfUtil.addSuccessMessage("Bill Saved");
        setPrintigBill();
        checkBillValues();
        duplicatePrint = false;
        return true;
    }

    @Deprecated
    private boolean executeSettleBillActionsRetired() {
        if (errorCheck()) {
            return false;
        }
        savePatient();
        int numberOfBillsForTheOrder = getBillBean().calculateNumberOfBillsPerOrder(getLstBillEntries());

        boolean oneOpdBillForAllDepartments = configOptionApplicationController.getBooleanValueByKey("One OPD Bill For All Departments and Categories", true);
        boolean oneOpdBillForEachDepartment = configOptionApplicationController.getBooleanValueByKey("One OPD Bill For Each Department", false);
        boolean oneOpdBillForEachCategory = configOptionApplicationController.getBooleanValueByKey("One OPD Bill For Each Category", false);
        boolean oneOpdBillForEachDepartmentAndCategoryCombination = configOptionApplicationController.getBooleanValueByKey("One OPD Bill For Each Department and Category Combination", false);

        if (oneOpdBillForAllDepartments) {

        } else {

        }

        if (numberOfBillsForTheOrder == 1) {
            BilledBill temp = new BilledBill();
            Bill b = null;
            switch (sessionController.getDepartmentPreference().getOpdBillingStrategy()) {
                case ONE_BILL_PER_DEPARTMENT:
                    b = saveBill(lstBillEntries.get(0).getBillItem().getItem().getDepartment(), temp);
                    break;
                case ONE_BILL_PER_DEPARTMENT_AND_CATEGORY:
                    b = saveBill(
                            lstBillEntries.get(0).getBillItem().getItem().getDepartment(),
                            lstBillEntries.get(0).getBillItem().getItem().getCategory(),
                            temp);
                    break;
                case SINGLE_BILL_FOR_ALL_ORDERS:
                    b = saveBill(sessionController.getDepartment(), temp);
                    break;
            }

            if (b == null) {
                return false;
            }
            List<BillItem> list = new ArrayList<>();
            for (BillEntry billEntry : getLstBillEntries()) {
                list.add(getBillBean().saveBillItem(b, billEntry, getSessionController().getLoggedUser()));
            }
            b.setBillItems(list);
            b.setBillTotal(b.getNetTotal());
            getBillFacade().edit(b);
            getBillBean().calculateBillItems(b, getLstBillEntries());
            if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
                b.setCashPaid(cashPaid);
                if (cashPaid >= b.getTransSaleBillTotalMinusDiscount()) {
                    b.setBalance(0.0);
                    b.setNetTotal(b.getTransSaleBillTotalMinusDiscount());
                } else {
                    b.setBalance(b.getTransSaleBillTotalMinusDiscount() - b.getCashPaid());
                    b.setNetTotal(b.getCashPaid());
                }
            }
            b.setVat(b.getVat());
            b.setVatPlusNetTotal(b.getNetTotal() + b.getVat());

            getBillFacade().edit(b);
            getBillBean().checkBillItemFeesInitiated(b);
            getBills().add(b);
        } else {
            boolean result = createMultipleBillsFromOrderList();
            if (result == false) {
                return false;
            }
        }
        saveBatchBill();
        createPaymentsForBills(getBatchBill(), getLstBillEntries());
        saveBillItemSessions();

        if (toStaff != null && getPaymentMethod() == PaymentMethod.Staff_Welfare) {
            staffBean.updateStaffWelfare(toStaff, netPlusVat);
            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
        } else if (toStaff != null && getPaymentMethod() == PaymentMethod.Staff) {
            staffBean.updateStaffCredit(toStaff, netPlusVat);
            JsfUtil.addSuccessMessage("Staff Credit Updated");
        }

        if (paymentMethod == PaymentMethod.PatientDeposit) {
            if (getPatient().getRunningBalance() != null) {
                getPatient().setRunningBalance(getPatient().getRunningBalance() - netTotal);
            } else {
                getPatient().setRunningBalance(0.0 - netTotal);
            }
            getPatientFacade().edit(getPatient());
        }

        if (getToken() != null) {
            getToken().setBill(getBatchBill());
            tokenFacade.edit(getToken());
            markToken(getBatchBill());
        }

        JsfUtil.addSuccessMessage("Bill Saved");
        setPrintigBill();
        checkBillValues();
        duplicatePrint = false;
        return true;
    }

    public void markToken(Bill b) {
        Token t = getToken();
        if (t == null) {
            return;
        }
        t.setBill(b);
        t.setCalled(true);
        t.setCalledAt(new Date());
        t.setInProgress(false);
        t.setCompleted(false);
        opdTokenController.saveToken(t);
    }

    public boolean checkBillValues(Bill b) {
        if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
            return false;
        }

        Double[] billItemValues = billBean.fetchBillItemValues(b);
        double billItemTotal = billItemValues[0];
        double billItemDiscount = billItemValues[1];
        double billItemNetTotal = billItemValues[2];
        double billItemVatPlusNetValue = billItemValues[3] + billItemValues[2];

        //// // System.out.println("b.getVatPlusNetTotal() = " + b.getVatPlusNetTotal());
        //// // System.out.println("roundOff(b.getVatPlusNetTotal()) = " + roundOff(b.getVatPlusNetTotal()));
        //// // System.out.println("billItemVatPlusNetValue = " + billItemVatPlusNetValue);
        //// // System.out.println("roundOff(billItemVatPlusNetValue) = " + roundOff(billItemVatPlusNetValue));
        if (billItemTotal != b.getTotal() || billItemDiscount != b.getDiscount() || billItemNetTotal != b.getNetTotal() || roundOff(billItemVatPlusNetValue) != roundOff(b.getVatPlusNetTotal())) {
            return true;
        }

        Double[] billFeeValues = billBean.fetchBillFeeValues(b);
        double billFeeTotal = billFeeValues[0];
        double billFeeDiscount = billFeeValues[1];
        double billFeeNetTotal = billFeeValues[2];
        double billFeeVatPlusNetValue = billFeeValues[3] + billFeeValues[2];

        //// // System.out.println("b.getVatPlusNetTotal() = " + b.getVatPlusNetTotal());
        //// // System.out.println("billItemVatPlusNetValue = " + roundOff(billItemVatPlusNetValue));
        if (billFeeTotal != b.getTotal() || billFeeDiscount != b.getDiscount() || billFeeNetTotal != b.getNetTotal() || roundOff(billItemVatPlusNetValue) != roundOff(b.getVatPlusNetTotal())) {
            return true;
        }

        return false;
    }

    public void checkBillValues() {
        for (Bill b : getBills()) {
            boolean flag = checkBillValues(b);
            b.setTransError(flag);
        }
    }

    @EJB
    StaffBean staffBean;

    private void saveBillItemSessions() {
        for (BillEntry be : lstBillEntries) {
            be.getBillItem().setBillSession(getServiceSessionBean().createBillSession(be.getBillItem()));
            if (be.getBillItem().getBillSession() != null) {
                getBillSessionFacade().create(be.getBillItem().getBillSession());
            }
        }
    }

    private void saveBatchBill() {
        System.out.println("saveBatchBill");
        Bill newBatchBill = new BilledBill();
        newBatchBill.setBillType(BillType.OpdBathcBill);
        newBatchBill.setBillTypeAtomic(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
        newBatchBill.setPaymentScheme(paymentScheme);
        newBatchBill.setPaymentMethod(paymentMethod);
        newBatchBill.setInstitution(sessionController.getInstitution());
        newBatchBill.setDepartment(sessionController.getDepartment());
        newBatchBill.setFromInstitution(sessionController.getInstitution());
        newBatchBill.setFromDepartment(sessionController.getDepartment());
        newBatchBill.setPatient(patient);
        newBatchBill.setInsId(
                getBillNumberGenerator().institutionBillNumberGenerator(
                        getSessionController().getInstitution(),
                        BillType.OpdBathcBill,
                        BillClassType.BilledBill,
                        BillNumberSuffix.NONE));
        newBatchBill.setDeptId(getBillNumberGenerator().departmentBillNumberGenerator(
                getSessionController().getInstitution(),
                getSessionController().getDepartment(),
                BillType.OpdBathcBill,
                BillClassType.BilledBill));
        newBatchBill.setGrantTotal(total);
        newBatchBill.setTotal(total);
        newBatchBill.setDiscount(discount);
        newBatchBill.setBillTime(new Date());
        newBatchBill.setBillTotal(netTotal);
        newBatchBill.setBillDate(new Date());
        newBatchBill.setCreatedAt(new Date());
        newBatchBill.setCreater(getSessionController().getLoggedUser());
        newBatchBill.setFromStaff(selectedCurrentlyWorkingStaff);

        getBillFacade().create(newBatchBill);

        double dbl = 0;
        double reminingCashPaid = cashPaid;
        for (Bill b : bills) {
            System.out.println("b = " + b);
            b.setBackwardReferenceBill(newBatchBill);
            dbl += b.getNetTotal();

//            if (getSessionController().getDepartmentPreference().isPartialPaymentOfOpdBillsAllowed()) {
//                b.setCashPaid(reminingCashPaid);
//                System.out.println("reminingCashPaid ***= " + reminingCashPaid);
//                System.out.println("b.getTransSaleBillTotalMinusDiscount() **= " + b.getTransSaleBillTotalMinusDiscount());
//                if (reminingCashPaid > b.getTransSaleBillTotalMinusDiscount()) {
//                    b.setBalance(0.0);
//                    b.setNetTotal(b.getTransSaleBillTotalMinusDiscount());
//                } else {
//                    b.setBalance(b.getTotal() - b.getCashPaid());
//                    b.setNetTotal(reminingCashPaid);
//                }
//            }
            System.out.println("b.getNet = " + b.getNetTotal());
            reminingCashPaid = reminingCashPaid - b.getNetTotal();
            getBillFacade().edit(b);

            newBatchBill.getForwardReferenceBills().add(b);
        }

        newBatchBill.setNetTotal(dbl);
        if (reminingCashPaid < 0) {
            newBatchBill.setBalance(Math.abs(reminingCashPaid));
        }
        newBatchBill.setCashBalance(reminingCashPaid);
        newBatchBill.setCashPaid(cashPaid);
        getBillFacade().edit(newBatchBill);
        setBatchBill(newBatchBill);
    }

    @Inject
    private BillSearch billSearch;

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

    private Bill saveBill(Department bt, Bill newBill) {
        newBill.setBillType(BillType.OpdBill);
        newBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        newBill.setDepartment(getSessionController().getDepartment());
        newBill.setInstitution(getSessionController().getInstitution());
        newBill.setToDepartment(bt);
        newBill.setToInstitution(bt.getInstitution());

        newBill.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        newBill.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        newBill.setStaff(staff);
        newBill.setToStaff(toStaff);
        newBill.setFromStaff(selectedCurrentlyWorkingStaff);

        newBill.setReferredBy(referredBy);
        newBill.setReferenceNumber(referralId);
        newBill.setReferredByInstitution(referredByInstitution);

        newBill.setCreditCompany(creditCompany);
        newBill.setCollectingCentre(collectingCentre);
        newBill.setComments(comment);

        getBillBean().setPaymentMethodData(newBill, paymentMethod, getPaymentMethodData());

        newBill.setBillDate(new Date());
        newBill.setBillTime(new Date());
        newBill.setPatient(patient);

//        newBill.setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(patient, getSessionController().getApplicationPreference().isMembershipExpires()));
        newBill.setPaymentScheme(getPaymentScheme());
        newBill.setPaymentMethod(paymentMethod);
        newBill.setCreatedAt(new Date());
        newBill.setCreater(getSessionController().getLoggedUser());

        //SETTING INS ID
        recurseCount = 0;
        String insId = generateBillNumberInsId(newBill);

        if (insId.equals("")) {
            return null;
        }
        newBill.setInsId(insId);
        if (newBill.getId() == null) {
            getFacade().create(newBill);
        } else {
            getFacade().edit(newBill);
        }

        String deptId = getBillNumberGenerator().departmentBillNumberGenerator(newBill.getDepartment(), newBill.getToDepartment(), newBill.getBillType(), BillClassType.BilledBill);
        newBill.setDeptId(deptId);
        switch (sessionController.getDepartmentPreference().getOpdTokenNumberGenerationStrategy()) {
            case NO_TOKEN_GENERATION:
                newBill.setSessionId(null);
                break;
            case BILLS_BY_DEPARTMENT:
                newBill.setSessionId(getBillNumberGenerator().generateDailyBillNumberForOpd(newBill.getDepartment()));
                break;
            case BILLS_BY_DEPARTMENT_AND_CATEGORY:
                newBill.setSessionId(getBillNumberGenerator().generateDailyBillNumberForOpd(newBill.getDepartment(), newBill.getCategory()));
                break;
            case BILLS_BY_DEPARTMENT_CATEGORY_AND_FROMSTAFF:
                newBill.setSessionId(getBillNumberGenerator().generateDailyBillNumberForOpd(newBill.getDepartment(), newBill.getCategory(), newBill.getFromStaff()));
                break;
        }

        if (newBill.getId() == null) {
            getFacade().create(newBill);
        } else {
            getFacade().edit(newBill);
        }
        return newBill;

    }

    private Bill saveBill(Department bt, Category cat, Bill newBill) {
        newBill.setBillType(BillType.OpdBill);
        newBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);

//        newBill.setCategory(cat);
        newBill.setDepartment(getSessionController().getDepartment());
        newBill.setInstitution(getSessionController().getInstitution());
        newBill.setToDepartment(bt);
        newBill.setToInstitution(bt.getInstitution());

        newBill.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        newBill.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        newBill.setStaff(staff);
        newBill.setToStaff(toStaff);
        newBill.setFromStaff(selectedCurrentlyWorkingStaff);

        newBill.setReferredBy(referredBy);
        newBill.setReferenceNumber(referralId);
        newBill.setReferredByInstitution(referredByInstitution);

        newBill.setCreditCompany(creditCompany);
        newBill.setCollectingCentre(collectingCentre);
        newBill.setComments(comment);

        getBillBean().setPaymentMethodData(newBill, paymentMethod, getPaymentMethodData());

        newBill.setBillDate(new Date());
        newBill.setBillTime(new Date());
        newBill.setPatient(patient);

//        newBill.setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(patient, getSessionController().getApplicationPreference().isMembershipExpires()));
        newBill.setPaymentScheme(getPaymentScheme());
        newBill.setPaymentMethod(paymentMethod);
        newBill.setCreatedAt(new Date());
        newBill.setCreater(getSessionController().getLoggedUser());

        //SETTING INS ID
        recurseCount = 0;
        String insId = generateBillNumberInsId(newBill);

        if (insId.equals("")) {
            return null;
        }
        newBill.setInsId(insId);
        if (newBill.getId() == null) {
            getFacade().create(newBill);
        } else {
            getFacade().edit(newBill);
        }

        //Department ID (DEPT ID)
        String deptId = getBillNumberGenerator().departmentBillNumberGenerator(newBill.getDepartment(), newBill.getToDepartment(), newBill.getBillType(), BillClassType.BilledBill);
        newBill.setDeptId(deptId);

        newBill.setSessionId(getBillNumberGenerator().generateDailyBillNumberForOpd(newBill.getDepartment()));

        if (newBill.getId() == null) {
            getFacade().create(newBill);
        } else {
            getFacade().edit(newBill);
        }
        return newBill;

    }

    int recurseCount = 0;

    private String generateBillNumberInsId(Bill bill) {
        String insId = getBillNumberGenerator().institutionBillNumberGenerator(bill.getInstitution(), bill.getToDepartment(), bill.getBillType(), BillClassType.BilledBill, BillNumberSuffix.NONE);
        return insId;
    }

    private boolean checkPatientAgeSex() {
        if (getPatient().getPerson().getName() == null || getPatient().getPerson().getName().trim().equals("") || getPatient().getPerson().getSex() == null || getPatient().getPerson().getDob() == null) {
            JsfUtil.addErrorMessage("Can not bill without Patient Name, Age or Sex.");
            return true;
        }
        if (!com.divudi.java.CommonFunctions.checkAgeSex(getPatient().getPerson().getDob(), getPatient().getPerson().getSex(), getPatient().getPerson().getTitle())) {
            JsfUtil.addErrorMessage("Mismatch in Title and Gender. Please Check the Title, Age and Sex");
            return true;
        }
        return false;
    }

    public double calculatRemainForMultiplePaymentTotal() {

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();

            }
            return total - multiplePaymentMethodTotalValue;
        }
        return total;
    }

    public void recieveRemainAmountAutomatically() {
        double remainAmount = calculatRemainForMultiplePaymentTotal();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
            if (pm.getPaymentMethod() == PaymentMethod.Cash) {
                pm.getPaymentMethodData().getCash().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Card) {
                pm.getPaymentMethodData().getCreditCard().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Cheque) {
                pm.getPaymentMethodData().getCheque().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Slip) {
                pm.getPaymentMethodData().getSlip().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.ewallet) {
                pm.getPaymentMethodData().getEwallet().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                pm.getPaymentMethodData().getPatient_deposit().setPatient(patient);
                pm.getPaymentMethodData().getPatient_deposit().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Credit) {
                pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Staff) {
                pm.getPaymentMethodData().getStaffCredit().setTotalValue(remainAmount);
            }

        }
    }

    private boolean errorCheck() {

        if (getLstBillEntries().isEmpty()) {
            JsfUtil.addErrorMessage("No Items are added to the bill to settle");
            return true;
        }
        if (getPatient() == null) {
            JsfUtil.addErrorMessage("New Patient is NULL. Programming Error. Contact Developer.");
            return true;
        }
        if (getPatient().getPerson() == null) {
            JsfUtil.addErrorMessage("New Patient's Person is NULL. Programming Error. Contact Developer.");
            return true;
        }
        if (getPatient().getPerson().getName() == null
                || getPatient().getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Can not bill without a name for the new Patient !");
            return true;
        }
        if (getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Method");
            return true;
        }
        if (sessionController.getApplicationPreference().isNeedAreaForPatientRegistration()) {
            if (getPatient().getPerson().getArea() == null) {
                JsfUtil.addErrorMessage("Please Add Patient Area");
                return true;
            }
        }
        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Title And Gender To Save Patient", false)) {
            if (getPatient().getPerson().getTitle() == null) {
                JsfUtil.addErrorMessage("Please select title");
                return true;
            }
            if (getPatient().getPerson().getSex() == null) {
                JsfUtil.addErrorMessage("Please select gender");
                return true;
            }
        }
        if (configOptionApplicationController.getBooleanValueByKey("Need Patient Age to Save Patient", false)) {
            if (getPatient().getPerson().getDob() == null) {
                JsfUtil.addErrorMessage("Please select patient date of birth");
                return true;
            }
        }

        if (!sessionController.getDepartmentPreference().isOpdSettleWithoutPatientPhoneNumber()) {
            if (getPatient().getPerson().getPhone() == null) {
                JsfUtil.addErrorMessage("Please Enter a Phone Number");
                return true;
            }
            if (getPatient().getPerson().getPhone().trim().equals("")) {
                JsfUtil.addErrorMessage("Please Enter a Phone Number");
                return true;
            }
        }

        if (!sessionController.getDepartmentPreference().isOpdSettleWithoutPatientArea()) {
            if (getPatient().getPerson().getArea() == null) {
                JsfUtil.addErrorMessage("Please Select Pataient Area");
                return true;
            }
            if (getPatient().getPerson().getArea().getName().trim().equals("")) {
                JsfUtil.addErrorMessage("Please Select Patient Area");
                return true;
            }
        }

        if (!sessionController.getDepartmentPreference().isOpdSettleWithoutReferralDetails()) {
            if (referredBy == null && referredByInstitution == null) {
                JsfUtil.addErrorMessage("Please Select a Referring Doctor or a Referring Institute. It is Required for Investigations.");
                return true;
            }
        }

        if (!sessionController.getDepartmentPreference().getCanSettleOpdBillWithInvestigationsWithoutReferringDoctor()) {
            for (BillEntry be : getLstBillEntries()) {
                if (be.getBillItem().getItem() instanceof Investigation) {
                    if (referredBy == null && referredByInstitution == null) {
                        JsfUtil.addErrorMessage("Please Select a Referring Doctor or a Referring Institute. It is Required for Investigations.");
                        return true;
                    }
                }
            }
        }
//        System.out.println("sessionController.getDepartmentPreference().isOpdSettleWithoutCashTendered() = " + sessionController.getDepartmentPreference().isOpdSettleWithoutCashTendered());
//        System.out.println("getStrTenderedValue() = " + getStrTenderedValue());
        if (!sessionController.getDepartmentPreference().isOpdSettleWithoutCashTendered()) {
            if (getPaymentMethod() == PaymentMethod.Cash) {
                if (getStrTenderedValue() == null) {
                    JsfUtil.addErrorMessage("Please Enter Tenderd Amount");
                    return true;
                }
                if (cashPaid < (vat + netTotal)) {
                    JsfUtil.addErrorMessage("Please Enter Correct Tenderd Amount");
                    return true;
                }
            }

        }

        boolean checkAge = false;
        for (BillEntry be : getLstBillEntries()) {
            if (be.getBillItem().getItem().getDepartment().getDepartmentType() == DepartmentType.Lab) {
                //  //System.err.println("ttttt");
                checkAge = true;
                break;
            }
        }

        if (checkAge && checkPatientAgeSex()) {
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(paymentMethod, getPaymentMethodData())) {
            return true;
        }

        if (paymentMethod == PaymentMethod.PatientDeposit) {
            if (!getPatient().getHasAnAccount()) {
                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                return true;
            }
            double creditLimitAbsolute = Math.abs(getPatient().getCreditLimit());
            double runningBalance;
            if (getPatient().getRunningBalance() != null) {
                runningBalance = getPatient().getRunningBalance();
            } else {
                runningBalance = 0.0;
            }
            double availableForPurchase = runningBalance + creditLimitAbsolute;

            if (netTotal > availableForPurchase) {
                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.Credit) {
            if (creditCompany == null && collectingCentre == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare or credit company or Collecting centre.");
                return true;
            }
        }

        if (paymentMethod == PaymentMethod.Staff) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member.");
                return true;
            }

            if (toStaff.getCurrentCreditValue() + netTotal > toStaff.getCreditLimitQualified()) {
                JsfUtil.addErrorMessage("No enough Credit.");
                return true;
            }
        }

        if (paymentMethod == PaymentMethod.Staff_Welfare) {
            if (toStaff == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare.");
                return true;
            }
            if (Math.abs(toStaff.getAnnualWelfareUtilized()) + netTotal > toStaff.getAnnualWelfareQualified()) {
                JsfUtil.addErrorMessage("No enough credit.");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            if (getPaymentMethodData() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            if (getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null) {
                JsfUtil.addErrorMessage("No Details on multiple payment methods given");
                return true;
            }
            double multiplePaymentMethodTotalValue = 0.0;
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                if (paymentSchemeController.checkPaymentMethodError(cd.getPaymentMethod(), cd.getPaymentMethodData())) {
                    return true;
                }
                if (cd.getPaymentMethod().equals(PaymentMethod.Staff)) {
                    if (cd.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0 || cd.getPaymentMethodData().getStaffCredit().getToStaff() == null) {
                        JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                        return true;
                    }
                    if (cd.getPaymentMethodData().getStaffCredit().getToStaff().getCurrentCreditValue() + cd.getPaymentMethodData().getStaffCredit().getTotalValue() > cd.getPaymentMethodData().getStaffCredit().getToStaff().getCreditLimitQualified()) {
                        JsfUtil.addErrorMessage("No enough Credit.");
                        return true;
                    }
                }
                //TODO - filter only relavant value
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCash().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCreditCard().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getCheque().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getEwallet().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getPatient_deposit().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getSlip().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffCredit().getTotalValue();
            }
            double differenceOfBillTotalAndPaymentValue = netTotal - multiplePaymentMethodTotalValue;
            differenceOfBillTotalAndPaymentValue = Math.abs(differenceOfBillTotalAndPaymentValue);
            if (differenceOfBillTotalAndPaymentValue > 1.0) {
                JsfUtil.addErrorMessage("Mismatch in differences of multiple payment method total and bill total");
                return true;
            }
            if (cashPaid == 0.0) {
                setCashPaid(multiplePaymentMethodTotalValue);
            }

        }

        if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
            if (cashPaid == 0.0) {
                JsfUtil.addErrorMessage("Please enter the paid amount");
                return true;
            }

        }
        return false;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    @EJB
    BillSessionFacade billSessionFacade;
    @Inject
    ServiceSessionFunctions serviceSessionBean;

    public List<BillSession> getBillSessions() {
        return billSessions;
    }

    public void fillBillSessions() {
        if (lastBillItem != null && lastBillItem.getItem() != null) {
            billSessions = getServiceSessionBean().getBillSessions(lastBillItem.getItem(), getSessionDate());
        } else if (billSessions == null || !billSessions.isEmpty()) {
            billSessions = new ArrayList<>();
        }
    }

    public void fillBillSessionsLstner() {
        if (lastBillItem != null && lastBillItem.getItem() != null) {
            billSessions = getServiceSessionBean().getBillSessions(lastBillItem.getItem(), getSessionDate());
        } else if (billSessions == null || !billSessions.isEmpty()) {
            billSessions = new ArrayList<>();
        }
    }

    public ServiceSessionFunctions getServiceSessionBean() {
        return serviceSessionBean;
    }

    public void setServiceSessionBean(ServiceSessionFunctions serviceSessionBean) {
        this.serviceSessionBean = serviceSessionBean;
    }

    public BillSessionFacade getBillSessionFacade() {
        return billSessionFacade;
    }

    public void setBillSessionFacade(BillSessionFacade billSessionFacade) {
        this.billSessionFacade = billSessionFacade;
    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    public void addToBill() {
        if (getCurrentBillItem() == null) {
            JsfUtil.addErrorMessage("Nothing to add");
            return;
        }
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please select an Item");
            return;
        }
        if (getCurrentBillItem().getItem().getTotal() == 0.0) {
            JsfUtil.addErrorMessage("Please correct item fee");
            return;
        }

        if (getCurrentBillItem().getItem().getDepartment() == null) {
            JsfUtil.addErrorMessage("Please set Department to Item");
            return;
        }

        if (getCurrentBillItem().getItem().getCategory() == null) {
            JsfUtil.addErrorMessage("Please set Category to Item");
            return;
        }
        if (getCurrentBillItem().getItem().getPriority() != null) {
            getCurrentBillItem().setPriority(getCurrentBillItem().getItem().getPriority());
        }
        if (getCurrentBillItem().getQty() == null) {
            getCurrentBillItem().setQty(1.0);
        }
        for (BillEntry bi : lstBillEntries) {
            if (bi.getBillItem() != null && getCurrentBillItem() != null && getCurrentBillItem().getItem() != null && bi.getBillItem().getItem().equals(getCurrentBillItem().getItem())) {
                JsfUtil.addErrorMessage("Can't select same item " + getCurrentBillItem().getItem());
                return;
            }
        }
        BillItem bi = new BillItem();
        bi.copy(getCurrentBillItem());
        bi.setSessionDate(sessionDate);
//        New Session
        //   getCurrentBillItem().setBillSession(getServiceSessionBean().createBillSession(getCurrentBillItem()));
//        New Session
        //   getCurrentBillItem().setBillSession(getServiceSessionBean().createBillSession(getCurrentBillItem()));
        lastBillItem = bi;
        BillEntry addingEntry = new BillEntry();
        addingEntry.setBillItem(bi);
        addingEntry.setLstBillComponents(getBillBean().billComponentsFromBillItem(bi));
        addingEntry.setLstBillFees(getBillBean().billFeefromBillItem(bi));

        addStaffToBillFees(addingEntry.getLstBillFees());

        addingEntry.setLstBillSessions(getBillBean().billSessionsfromBillItem(bi));
        getLstBillEntries().add(addingEntry);
        bi.setRate(getBillBean().billItemRate(addingEntry));
//            bi.setQty(1.0);
        bi.setNetValue(bi.getRate() * bi.getQty());

        if (bi.getItem().isVatable()) {
            bi.setVat(bi.getNetValue() * bi.getItem().getVatPercentage() / 100);
        }

        bi.setVatPlusNetValue(bi.getNetValue() + bi.getVat());

        calTotals();

        if (bi.getNetValue() == 0.0) {
            JsfUtil.addErrorMessage("Please enter the rate");
            return;
        }

        clearBillItemValues();
        boolean clearItemAfterAddingToOpdBill = configOptionApplicationController.getBooleanValueByKey("Clear Item After Adding To Opd Bill", true);
        if (clearItemAfterAddingToOpdBill) {
            setItemLight(null);
        } else {
            setItemLight(itemLight);
        }
        JsfUtil.addSuccessMessage("Added");
    }

    private void addStaffToBillFees(List<BillFee> tmpBfs) {
        if (tmpBfs == null) {
            return;
        }
        if (tmpBfs.isEmpty()) {
            return;
        }
        if (getCurrentlyWorkingStaff().isEmpty()) {
            return;
        }

        for (BillFee bf : tmpBfs) {
            if (bf.getFee() == null) {
                continue;
            }
            if (bf.getFee().getFeeType() == null) {
                continue;
            }

            if (bf.getFee().getFeeType() == FeeType.Staff) {
                if (bf.getFee().getStaff() == null) {
                    if (bf.getFee().getSpeciality() != null) {
                        boolean staffSet = false;
                        for (Staff s : currentlyWorkingStaff) {
                            if (bf.getFee().getSpeciality().equals(s.getSpeciality())) {
                                bf.setStaff(s);
                                staffSet = true;
                                break;
                            }
                        }
                        if (!staffSet) {
                        }
                    } else {
                        bf.setStaff(getSelectedCurrentlyWorkingStaff());
                    }
                } else {
                }
            }
        }
    }

    public void clearBillItemValues() {
        currentBillItem = null;
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;
    }

    private void clearBillValues() {
        setPatient(null);
        setReferredBy(null);
        payments = null;
//        setReferredByInstitution(null);
        setReferralId(null);
        setSessionDate(null);
        setCreditCompany(null);
        setCollectingCentre(null);
        setBills(null);
        setPaymentScheme(null);
        paymentMethod = PaymentMethod.Cash;
        paymentMethodData = null;
        currentBillItem = null;
        setLstBillComponents(null);
        setLstBillEntries(null);
        setLstBillFees(null);
        setStaff(null);
        setToStaff(null);
        setComment(null);
        lstBillEntries = new ArrayList<>();
        setForeigner(false);
        setSessionDate(Calendar.getInstance().getTime());
        calTotals();

        setCashPaid(0.0);
        setDiscount(0.0);
        setCashBalance(0.0);

        setStrTenderedValue("");
        currentlyWorkingStaff = null;
        fromOpdEncounter = false;
        opdEncounterComments = "";
        patientSearchTab = 0;
        token = null;
    }

    private void clearBillValuesForMember() {
        setPatient(null);
        setReferredBy(null);
//        setReferredByInstitution(null);
        setReferralId(null);
        setSessionDate(null);
        setCreditCompany(null);
        setCollectingCentre(null);
        setBills(null);
        setPaymentScheme(null);
        paymentMethod = PaymentMethod.Cash;
        paymentMethodData = null;
        currentBillItem = null;
        setLstBillComponents(null);
        setLstBillEntries(null);
        setLstBillFees(null);
        setStaff(null);
        setToStaff(null);
        setComment(null);
        lstBillEntries = new ArrayList<>();
        setForeigner(false);
        setSessionDate(Calendar.getInstance().getTime());
        calTotals();

        setCashPaid(0.0);
        setDiscount(0.0);
        setCashBalance(0.0);
        setStrTenderedValue("");
        fromOpdEncounter = false;
        opdEncounterComments = "";
        patientSearchTab = 1;
    }

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    @Inject
    MembershipSchemeController membershipSchemeController;

    public void calTotals() {
        if (paymentMethod == null) {
            return;
        }

//        if (toStaff != null) {
//            paymentScheme = null;
//            creditCompany = null;
//        }
        double billDiscount = 0.0;
        double billGross = 0.0;
        double billNet = 0.0;
        double billVat = 0.0;

//        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
        for (BillEntry be : getLstBillEntries()) {
            //////// // System.out.println("bill item entry");
            double entryGross = 0.0;
            double entryDis = 0.0;
            double entryNet = 0.0;
            double entryVat = 0.0;
            double entryVatPlusNet = 0.0;

            BillItem bi = be.getBillItem();

            for (BillFee bf : be.getLstBillFees()) {
                Department department = null;
                Item item = null;
                PriceMatrix priceMatrix;
                Category category = null;

                if (bf.getBillItem() != null && bf.getBillItem().getItem() != null) {
                    department = bf.getBillItem().getItem().getDepartment();

                    item = bf.getBillItem().getItem();
                }

                priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(paymentMethod, paymentScheme, department, item);
                getBillBean().setBillFees(bf, isForeigner(), paymentMethod, paymentScheme, getCreditCompany(), priceMatrix);

                if (bf.getBillItem().getItem().isVatable()) {
                    if (!(bf.getFee().getFeeType() == FeeType.CollectingCentre && collectingCentreBillController.getCollectingCentre() != null)) {
                        bf.setFeeVat(bf.getFeeValue() * bf.getBillItem().getItem().getVatPercentage() / 100);
                        bf.setFeeVat(roundOff(bf.getFeeVat()));
                    }
                }
                bf.setFeeVatPlusValue(bf.getFeeValue() + bf.getFeeVat());

                entryGross += bf.getFeeGrossValue();
                entryNet += bf.getFeeValue();
                entryDis += bf.getFeeDiscount();
                entryVat += bf.getFeeVat();
                entryVatPlusNet += bf.getFeeVatPlusValue();

                //////// // System.out.println("fee net is " + bf.getFeeValue());
            }

            bi.setDiscount(entryDis);
            bi.setGrossValue(entryGross);
            bi.setNetValue(entryNet);
            bi.setVat(entryVat);
            bi.setVatPlusNetValue(roundOff(entryVatPlusNet));

            //// // System.out.println("item is = " + bi.getItem().getName());
            //// // System.out.println("item gross is = " + bi.getGrossValue());
            billGross += bi.getGrossValue();
            billNet += bi.getNetValue();
            billDiscount += bi.getDiscount();
            billVat += bi.getVat();
            //     billDis = billDis + entryDis;
        }
        setDiscount(billDiscount);
        setTotal(billGross);
        setNetTotal(billNet);
        setVat(billVat);
        setNetPlusVat(getVat() + getNetTotal());

        if (getSessionController() != null) {
            if (getSessionController().getApplicationPreference() != null) {

            }
        }

    }

    public void feeChanged() {
        lstBillItems = null;
        getLstBillItems();
        calTotals();
        //  feeChanged = false;

    }

    public void markAsForeigner() {
        setForeigner(true);
        calTotals();
    }

    public void markAsLocal() {
        setForeigner(false);
        calTotals();
    }

    long startIdForVat;
    BillType billTypeForVat;
    String txtBillNoForVat;

    public long getStartIdForVat() {
        return startIdForVat;
    }

    public void setStartIdForVat(long startIdForVat) {
        this.startIdForVat = startIdForVat;
    }

    public BillType getBillTypeForVat() {
        return billTypeForVat;
    }

    public void setBillTypeForVat(BillType billTypeForVat) {
        this.billTypeForVat = billTypeForVat;
    }

    public String getTxtBillNoForVat() {
        return txtBillNoForVat;
    }

    public void setTxtBillNoForVat(String txtBillNoForVat) {
        this.txtBillNoForVat = txtBillNoForVat;
    }

    public void addVatToOldBills() {
        String j = "select b from Bill b "
                + "where b.billType=:bt "
                + " and b.id > :id ";
        Map m = new HashMap();
        m.put("bt", billTypeForVat);
        m.put("id", startIdForVat);
        List<Bill> bs = getFacade().findByJpql(j, m, 1000);
        txtBillNoForVat = "";
        for (Bill b : bs) {
            if (b.getVatPlusNetTotal() == 0.00) {
                b.setVatPlusNetTotal(b.getNetTotal());
                getFacade().edit(b);
                startIdForVat = b.getId();
                txtBillNoForVat = txtBillNoForVat + "\n" + "Ind Id = " + b.getInsId();
            }
        }
    }

    public void searchOpdPayments() {
        billType = BillType.OpdBill;
        String j = "select b from Payment b"
                + " where b.createdAt between :fd and :td "
                + " and b.retired=false"
                + " and b.bill.billType = :bt ";
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", billType);

        if (institution != null) {
            j += " and b.institution=:ins ";
            m.put("ins", institution);
        }

        if (department != null) {
            j += " and b.department=:dep ";
            m.put("dep", department);
        }

        j += " order by b.createdAt desc  ";
        payments = getPaymentFacade().findByJpql(j, m);
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public void clearOpdBillSearchData() {
        institution = null;
        department = null;

    }

    public String navigateToNewOpdBill() {
        Boolean opdBillingAfterShiftStart = sessionController.getApplicationPreference().isOpdBillingAftershiftStart();
        if (opdBillingAfterShiftStart) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                clearBillItemValues();
                clearBillValues();
                paymentMethodData = null;
                paymentScheme = null;
                paymentMethod = PaymentMethod.Cash;
                collectingCentreBillController.setCollectingCentre(null);
                return "/opd/opd_bill?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            clearBillItemValues();
            clearBillValues();
            paymentMethodData = null;
            paymentScheme = null;
            paymentMethod = PaymentMethod.Cash;
            collectingCentreBillController.setCollectingCentre(null);
            return "/opd/opd_bill?faces-redirect=true";
        }
    }

    public String navigateToNewOpdBillFromToken() {
        Boolean opdBillingAfterShiftStart = sessionController.getApplicationPreference().isOpdBillingAftershiftStart();
        if (opdBillingAfterShiftStart) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                paymentMethodData = null;
                paymentScheme = null;
                paymentMethod = PaymentMethod.Cash;
                collectingCentreBillController.setCollectingCentre(null);
                if (getToken() != null) {
                    setPatient(token.getPatient());
                }
                return "/opd/opd_bill?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            paymentMethodData = null;
            paymentScheme = null;
            paymentMethod = PaymentMethod.Cash;
            collectingCentreBillController.setCollectingCentre(null);
            if (getToken() != null) {
                setPatient(token.getPatient());
            }
            return "/opd/opd_bill?faces-redirect=true";
        }
    }

    public String navigateToNewOpdBill(Patient pt) {
        String navigateLink = navigateToNewOpdBill();
        patient = pt;
        return navigateLink;
    }

    public String navigateToNewOpdBillFromChannelling() {
        String navigateLink = navigateToNewOpdBill();
        BillSession bs = viewScopeDataTransferController.getSelectedBillSession();
        if (bs == null) {
            return null;
        }
        if (bs.getBill().getPatient() == null) {
            return null;
        }
        if (bs.getSessionInstance().getStaff() == null) {
            return null;
        }
        patient = bs.getBill().getPatient();
        Staff channellingDoc = bs.getSessionInstance().getStaff();
        getCurrentlyWorkingStaff().add(channellingDoc);
        setSelectedCurrentlyWorkingStaff(channellingDoc);
        if (channellingDoc.getDepartment() != null) {
            setSelectedOpdItemDepartment(channellingDoc.getDepartment());
            departmentChanged();
        }
        return navigateLink;
    }

    public String navigateToNewOpdBillWithPaymentScheme(Patient pt, PaymentScheme ps) {
        navigateToNewOpdBill();
        patient = pt;
        paymentScheme = ps;
        return "/opd/opd_bill?faces-redirect=true";
    }

    public String toOpdBilling() {
        return "/opd/opd_bill?faces-redirect=true";
    }

    public void prepareNewBillForMember() {
        clearBillItemValues();
        clearBillValuesForMember();
        paymentMethodData = null;
        paymentScheme = null;
        paymentMethod = PaymentMethod.Cash;
        collectingCentreBillController.setCollectingCentre(null);
    }

    public void makeNull() {
        clearBillItemValues();
        clearBillValues();
        paymentMethod = null;
    }

    public void removeBillItem() {

        //TODO: Need to add Logic
        //////// // System.out.println(getIndex());
        if (getIndex() != null) {
            //  boolean remove;
            BillEntry temp = getLstBillEntries().get(getIndex());
            //////// // System.out.println("Removed Item:" + temp.getBillItem().getNetValue());
            recreateList(temp);
            // remove = getLstBillEntries().remove(getIndex());

            //  getLstBillEntries().remove(index);
            ////////// // System.out.println("Is Removed:" + remove);
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

    public void createPaymentsForBills(Bill b, List<BillEntry> billEntrys) {
        List<Payment> ps = createPayment(b, b.getPaymentMethod());
        payments = ps;
        createBillFeePaymentsByPaymentsAndBillEntry(ps.get(0), billEntrys);
    }

    public List<Payment> createPayment(Bill bill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
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

                    case Agent:
                    case Credit:
                    case PatientDeposit:
                        if (getPatient().getRunningBalance() != null) {
                            getPatient().setRunningBalance(getPatient().getRunningBalance() - netTotal);
                        } else {
                            getPatient().setRunningBalance(0.0 - netTotal);
                        }
                        getPatientFacade().edit(getPatient());
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            staffBean.updateStaffCredit(cd.getPaymentMethodData().getStaffCredit().getToStaff(), cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                        }
                    case YouOweMe:
                    case MultiplePaymentMethods:
                }

                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(getSessionController().getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(pm);

            switch (pm) {
                case Card:
                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                    p.setPaidValue(paymentMethodData.getCreditCard().getTotalValue());
                    break;
                case Cheque:
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    p.setPaidValue(paymentMethodData.getCheque().getTotalValue());
                    break;
                case Cash:
                    p.setPaidValue(paymentMethodData.getCash().getTotalValue());
                    break;
                case ewallet:

                case Agent:
                case Credit:
                case PatientDeposit:
                case Slip:
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setPaidValue(paymentMethodData.getSlip().getTotalValue());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);

            ps.add(p);
        }
        return ps;
    }

    private SmsManagerEjb getSmsManagerEjb() {
        return smsManagerEjb;
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
        } else {
            paymentFacade.edit(p);
        }
    }

    private double reminingCashPaid = 0.0;

    public void createBillFeePaymentsByPaymentsAndBillEntry(Payment p, List<BillEntry> billEntrys) {
        double dbl = 0;
        double pid = 0;
        reminingCashPaid = cashPaid;
        for (BillEntry be : billEntrys) {
            if ((reminingCashPaid != 0.0) || !getSessionController().getApplicationPreference().isPartialPaymentOfOpdPreBillsAllowed()) {
                calculateBillfeePayments(be.getLstBillFees(), p);
            }
        }
    }

    public void calculateBillfeePayments(List<BillFee> billFees, Payment p) {
        for (BillFee bf : billFees) {

            if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdPreBillsAllowed() || getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
                if (Math.abs((bf.getFeeValue() - bf.getSettleValue())) > 0.1) {
                    if (reminingCashPaid >= (bf.getFeeValue() - bf.getSettleValue())) {
                        //// // System.out.println("In If reminingCashPaid = " + reminingCashPaid);
                        //// // System.out.println("bf.getPaidValue() = " + bf.getSettleValue());
                        double d = (bf.getFeeValue() - bf.getSettleValue());
                        bf.setSettleValue(bf.getFeeValue());
                        setBillFeePaymentAndPayment(d, bf, p);
                        getBillFeeFacade().edit(bf);
                        reminingCashPaid -= d;
                    } else {
                        bf.setSettleValue(bf.getSettleValue() + reminingCashPaid);
                        setBillFeePaymentAndPayment(reminingCashPaid, bf, p);
                        getBillFeeFacade().edit(bf);
                        reminingCashPaid = 0.0;
                    }
                }
            } else {
                bf.setSettleValue(bf.getFeeValue());
                setBillFeePaymentAndPayment(bf.getFeeValue(), bf, p);
                getBillFeeFacade().edit(bf);
            }
        }
    }

    public void setBillFeePaymentAndPayment(double amount, BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(amount);
        bfp.setInstitution(bf.getBillItem().getItem().getInstitution());
        bfp.setDepartment(bf.getBillItem().getItem().getDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        billFeePaymentFacade.create(bfp);
    }

    public double calBillPaidValue(Bill b) {
        String sql;

        sql = "select sum(bfp.amount) from BillFeePayment bfp where "
                + " bfp.retired=false "
                + " and bfp.billFee.bill.id=" + b.getId();

        double d = billFeePaymentFacade.findDoubleByJpql(sql);

        return d;
    }

    private double roundOff(double d) {
        DecimalFormat newFormat = new DecimalFormat("#.##");
        try {
            return Double.valueOf(newFormat.format(d));
        } catch (Exception e) {
            return 0;
        }
    }

    public void updateReferingDoctor() {
        if (bill.getReferredBy() == null) {
            JsfUtil.addErrorMessage("Empty Doctor");
            return;
        }
        billFacade.edit(bill);
        JsfUtil.addSuccessMessage("Ref Doctor Updated");
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

    public OpdBillController() {
    }

    private BillFacade getFacade() {
        return billFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PaymentScheme getPaymentScheme() {
//        if (paymentScheme == null) {
//            paymentScheme = new PaymentScheme();
//        }
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
        //    getEnumController().setPaymentScheme(paymentScheme);

    }

    public void listnerForPaymentMethodChange() {
        if (paymentMethod == PaymentMethod.PatientDeposit) {
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            getPaymentMethodData().getPatient_deposit().setTotalValue(netTotal);
        }
        if (paymentMethod == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(netTotal);
        }
        calTotals();
    }

    @Override
    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
            Person p = new Person();
            patientDetailsEditable = true;

            patient.setPerson(p);
        }
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
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

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getDiscount() {
        return discount;
    }

    public double getVat() {
        return vat;
    }

    public void setVat(double vat) {
        this.vat = vat;
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
//        cashBalance = cashPaid - getNetTotal();
    }

    public double getCashBalance() {
        cashBalance = cashPaid - (netTotal + vat);
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
            currentBillItem.setQty(1.0);
        }

        return currentBillItem;
    }
    BillItem lastBillItem;

    public BillItem getLastBillItem() {
        if (lastBillItem == null) {
            if (getCurrentBillItem() != null) {
                lastBillItem = getCurrentBillItem();
            }
        }
        return lastBillItem;
    }

    public void setLastBillItem(BillItem lastBillItem) {
        this.lastBillItem = lastBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
        lastBillItem = currentBillItem;
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

    public EnumController getEnumController() {
        return enumController;
    }

    public void setEnumController(EnumController enumController) {
        this.enumController = enumController;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    public List<Bill> completeAppointmentBill(String query) {
        List<Bill> suggestions;
        String sql;
        HashMap hm = new HashMap();

        sql = "select p from BilledBill p where p.retired=false and "
                + "p.cancelled=false and p.refunded=false and p.billType=:btp "
                + " and ((p.patient.person.name)  "
                + "like :q or (p.insId)  "
                + "like :q) order by p.insId";
        //////// // System.out.println(sql);
        hm.put("q", "%" + query.toUpperCase() + "%");
        hm.put("btp", BillType.InwardAppointmentBill);
        suggestions = getFacade().findByJpql(sql, hm);

        return suggestions;

    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public PaymentMethod getPaymentMethod() {
        if (!sessionController.getDepartmentPreference().isPartialPaymentOfOpdBillsAllowed()) {
            if (paymentMethod != paymentMethod.Cash) {
                strTenderedValue = String.valueOf(netTotal);
            }
        }

        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isFromOpdEncounter() {
        return fromOpdEncounter;
    }

    public void setFromOpdEncounter(boolean fromOpdEncounter) {
        this.fromOpdEncounter = fromOpdEncounter;
    }

    public String getOpdEncounterComments() {
        return opdEncounterComments;
    }

    public void setOpdEncounterComments(String opdEncounterComments) {
        this.opdEncounterComments = opdEncounterComments;
    }

    public int getPatientSearchTab() {
        return patientSearchTab;
    }

    public void setPatientSearchTab(int patientSearchTab) {
        this.patientSearchTab = patientSearchTab;
    }

    public Bill getBill() {
        if (bill == null) {
            bill = new Bill();
            Bill b = new Bill();

            bill.setBilledBill(b);
        }
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public Bill getBillPrint() {
        return billPrint;
    }

    public void setBillPrint(Bill billPrint) {
        this.billPrint = billPrint;
    }

    public List<BillComponent> getLstBillComponentsPrint() {
        return lstBillComponentsPrint;
    }

    public void setLstBillComponentsPrint(List<BillComponent> lstBillComponentsPrint) {
        this.lstBillComponentsPrint = lstBillComponentsPrint;
    }

    public List<BillFee> getLstBillFeesPrint() {
        return lstBillFeesPrint;
    }

    public void setLstBillFeesPrint(List<BillFee> lstBillFeesPrint) {
        this.lstBillFeesPrint = lstBillFeesPrint;
    }

    public List<BillItem> getLstBillItemsPrint() {
        return lstBillItemsPrint;
    }

    public void setLstBillItemsPrint(List<BillItem> lstBillItemsPrint) {
        this.lstBillItemsPrint = lstBillItemsPrint;
    }

    public List<BillEntry> getLstBillEntriesPrint() {
        return lstBillEntriesPrint;
    }

    public void setLstBillEntriesPrint(List<BillEntry> lstBillEntriesPrint) {
        this.lstBillEntriesPrint = lstBillEntriesPrint;
    }

    public int getRecurseCount() {
        return recurseCount;
    }

    public void setRecurseCount(int recurseCount) {
        this.recurseCount = recurseCount;
    }

    public List<Bill> getBillsPrint() {
        return billsPrint;
    }

    public void setBillsPrint(List<Bill> billsPrint) {
        this.billsPrint = billsPrint;
    }

    public String getReferralId() {
        return referralId;
    }

    public void setReferralId(String referralId) {
        this.referralId = referralId;
    }

    public double getOpdPaymentCredit() {
        return opdPaymentCredit;
    }

    public void setOpdPaymentCredit(double opdPaymentCredit) {
        this.opdPaymentCredit = opdPaymentCredit;
    }

    public BilledBill getOpdBill() {
        return opdBill;
    }

    public void setOpdBill(BilledBill opdBill) {
        this.opdBill = opdBill;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public String navigateToBillContactNumbers() {
        return "/admin/bill_contact_numbers.xhtml";
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public List<BillLight> getBillLights() {
        return billLights;
    }

    public void setBillLights(List<BillLight> billLights) {
        this.billLights = billLights;
    }

    public BillLight getBillLight() {
        return billLight;
    }

    public void setBillLight(BillLight billLight) {
        this.billLight = billLight;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public double getNetPlusVat() {
        return netPlusVat;
    }

    public void setNetPlusVat(double netPlusVat) {
        this.netPlusVat = netPlusVat;
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }

    public double getReminingCashPaid() {
        return reminingCashPaid;
    }

    public void setReminingCashPaid(double reminingCashPaid) {
        this.reminingCashPaid = reminingCashPaid;
    }

    public SmsFacade getSmsFacade() {
        return SmsFacade;
    }

    public void setSmsFacade(SmsFacade SmsFacade) {
        this.SmsFacade = SmsFacade;
    }

    public int getOpdSummaryIndex() {
        return opdSummaryIndex;
    }

    public void setOpdSummaryIndex(int opdSummaryIndex) {
        this.opdSummaryIndex = opdSummaryIndex;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public int getOpdAnalyticsIndex() {
        return opdAnalyticsIndex;
    }

    public void setOpdAnalyticsIndex(int opdAnalyticsIndex) {
        this.opdAnalyticsIndex = opdAnalyticsIndex;
    }

    public List<ItemLight> getOpdItems() {
        if (opdItems == null) {
            opdItems = fillOpdItems();
        }
        return opdItems;
    }

    public String navigateToOpdBillPayments() {
        bills = null;
        return "/opd/analytics/opd_bill_payments";
    }

    // This is the setter for selectedItemLightId
    public void setSelectedItemLightId(Long id) {
        this.selectedItemLightId = id;
        if (id != null) {
            // Now use this ID to find the corresponding Item or ItemLight
            Item item = itemController.findItem(id);
            this.itemLight = new ItemLight(item);
            getCurrentBillItem().setItem(item);// Assuming you have such a constructor or method
            // Now itemLight is set to the corresponding ItemLight object
        } else {
            this.itemLight = null;
        }
    }

    public Long getSelectedItemLightId() {
        if (getCurrentBillItem().getItem() != null) {
            selectedItemLightId = getCurrentBillItem().getItem().getId();
        }
        return selectedItemLightId;
    }

    public ItemLight getItemLight() {
        if (getCurrentBillItem().getItem() != null) {
            itemLight = new ItemLight(getCurrentBillItem().getItem());
        }
        return itemLight;
    }

    public void setItemLight(ItemLight itemLight) {
        this.itemLight = itemLight;
        if (itemLight != null) {
            getCurrentBillItem().setItem(itemController.findItem(itemLight.getId()));
        }
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    public List<Payment> getPayments() {
        if (payments == null) {
            payments = new ArrayList<>();
        }
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public List<Staff> getCurrentlyWorkingStaff() {
        if (currentlyWorkingStaff == null) {
            reloadCurrentlyWorkingStaff();
        }
        return currentlyWorkingStaff;
    }

    public void setCurrentlyWorkingStaff(List<Staff> currentlyWorkingStaff) {
        this.currentlyWorkingStaff = currentlyWorkingStaff;
    }

    public Staff getSelectedCurrentlyWorkingStaff() {
        return selectedCurrentlyWorkingStaff;
    }

    public void setSelectedCurrentlyWorkingStaff(Staff selectedCurrentlyWorkingStaff) {
        this.selectedCurrentlyWorkingStaff = selectedCurrentlyWorkingStaff;
    }

    public boolean isDuplicatePrint() {
        return duplicatePrint;
    }

    public void setDuplicatePrint(boolean duplicatePrint) {
        this.duplicatePrint = duplicatePrint;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public Double getTotalHospitalFee() {
        return totalHospitalFee;
    }

    public void setTotalHospitalFee(Double totalHospitalFee) {
        this.totalHospitalFee = totalHospitalFee;
    }

    public Double getTotalSaffFee() {
        return totalSaffFee;
    }

    public void setTotalSaffFee(Double totalSaffFee) {
        this.totalSaffFee = totalSaffFee;
    }

    public boolean isCanChangeSpecialityAndDoctorInAddedBillItem() {
        return canChangeSpecialityAndDoctorInAddedBillItem;
    }

    public void setCanChangeSpecialityAndDoctorInAddedBillItem(boolean canChangeSpecialityAndDoctorInAddedBillItem) {
        boolean config = configOptionController.getBooleanValueByKey("Allow To Change Doctor Speciality And Doctor Added Bill Items in Opd Bill", OptionScope.DEPARTMENT, null, null, null);
        this.canChangeSpecialityAndDoctorInAddedBillItem = config;
    }

    public List<Department> getOpdItemDepartments() {
        if (opdItemDepartments == null) {
            getOpdItems();
        }
        return opdItemDepartments;
    }

    public void setOpdItemDepartments(List<Department> opdItemDepartments) {
        this.opdItemDepartments = opdItemDepartments;
    }

    public Department getSelectedOpdItemDepartment() {
        if (selectedOpdItemDepartment == null) {
            if (opdItemDepartments != null && !opdItemDepartments.isEmpty()) {
                selectedOpdItemDepartment = opdItemDepartments.get(0);
            }
        }
        return selectedOpdItemDepartment;
    }

    public void setSelectedOpdItemDepartment(Department selectedOpdItemDepartment) {
        this.selectedOpdItemDepartment = selectedOpdItemDepartment;
    }

    public void fillDepartmentOpdItems() {
        departmentOpdItems = null;
        itemApplicationController.reloadItems();
        getDepartmentOpdItems();
    }

    public List<ItemLight> getDepartmentOpdItems() {
        if (departmentOpdItems == null) {
            getOpdItems();
            departmentOpdItems = filterItemLightesByDepartment(getOpdItems(), getSelectedOpdItemDepartment());
        }
        return departmentOpdItems;
    }

    public void setDepartmentOpdItems(List<ItemLight> departmentOpdItems) {
        this.departmentOpdItems = departmentOpdItems;
    }

}
