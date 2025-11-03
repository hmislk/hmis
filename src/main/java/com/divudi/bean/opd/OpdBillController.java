package com.divudi.bean.opd;

import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.FinancialTransactionController;
import com.divudi.bean.common.*;
import com.divudi.bean.collectingCentre.CollectingCentreBillController;
import com.divudi.bean.hr.WorkingTimeController;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.ItemLight;
import com.divudi.core.data.MessageType;
import com.divudi.core.data.OpdBillingStrategy;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.data.dataStructure.BillListWithTotals;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.entity.*;
import com.divudi.ejb.BillEjb;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.SmsManagerEjb;
import com.divudi.service.StaffService;
import com.divudi.core.entity.hr.WorkingTime;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BillSessionFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.SmsFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.core.data.BillCategory;
import com.divudi.core.data.BillFeeBundleEntry;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BooleanMessage;
import com.divudi.core.data.OptionScope;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.TokenFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.light.common.BillLight;
import com.divudi.service.BillService;
import com.divudi.service.DepartmentResolver;
import com.divudi.service.DiscountSchemeValidationService;
import com.divudi.service.PaymentService;
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
    @EJB
    BillService billService;
    @EJB
    PaymentService paymentService;
    @EJB
    DiscountSchemeValidationService discountSchemeValidationService;
    @EJB
    DepartmentResolver departmentResolver;

    /**
     * Controllers
     */
    @Inject
    MembershipSchemeController membershipSchemeController;
    @Inject
    private BillController billController;
    @Inject
    private SessionController sessionController;
    @Inject
    private ItemController itemController;
    @Inject
    private ItemFeeManager itemFeeManager;
    @Inject
    private ItemApplicationController itemApplicationController;
    @Inject
    private ItemMappingController itemMappingController;
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
    @Inject
    CashBookEntryController cashBookEntryController;
    @Inject
    FeeValueController feeValueController;
    @Inject
    PatientDepositController patientDepositController;
    @Inject
    DrawerController drawerController;
    @Inject
    PatientInvestigationController patientInvestigationController;
    /**
     * Class Variables
     */
    private ItemLight itemLight;
    private Long selectedItemLightId;
    private PaymentScheme paymentScheme;
    private PaymentMethod paymentMethod;
    private Patient patient;
    private Doctor referredBy;
    private String referredByName;
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
    private String indication;
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
    private boolean billSettlingStarted;

    private List<Bill> bills;
    private List<Bill> selectedBills;

    private BilledBill opdBill;
    private BillItem currentBillItem;

    private List<BillComponent> lstBillComponents;
    private List<BillComponent> lstBillComponentsPrint;

    private List<BillFee> lstBillFees;
    private List<BillFee> lstBillFeesPrint;
    private List<BillFeeBundleEntry> billFeeBundleEntrys;

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
    private String localNumber;

    private String refNo;
    private double remainAmount;
    private Double currentBillItemQty;
    private PatientEncounter patientEncounter;

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

    public void changeTheSelectedFeeFromFeeBundle(BillFee ibf) {
        if (billFeeBundleEntrys == null) {
            return;
        }
        for (BillFeeBundleEntry bfbe : billFeeBundleEntrys) {
            for (BillFee bf : bfbe.getAvailableBillFees()) {
                if (bf.equals(ibf)) {
                    bfbe.setSelectedBillFee(bf);
                }
            }
        }
        calTotals();
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

    public String navigateToOpdBillItemList() {
        return "/opd/analytics/opd_bill_item_list?faces-redirect=true";
    }

    public String navigateToOpdBillList() {
        return "/opd/analytics/opd_bill_list?faces-redirect=true";
    }

    public String navigateToOpdCancellationBillList() {
        return "/opd/analytics/opd_cancellation_bill_list?faces-redirect=true";
    }

    public String navigateToSearchOpdBills() {
        batchBill = null;
        bills = null;
        searchController.setShowLoggedDepartmentOnly(true);
        return "/opd/opd_bill_search?faces-redirect=true";
    }

    @Deprecated
    public String navigateToSearchOpdPackageBills() {
        batchBill = null;
        bills = null;
        return "/opd/opd_package_bill_search?faces-redirect=true";
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
//                selectedCurrentlyWorkingStaff = wt.getStaffShift().getStaff();
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
            case SITE_FEE_ITEMS:
                temItems = itemFeeManager.fillItemLightsForSite(sessionController.getDepartment().getSite());
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
        return "/opd/opd_batch_bill_print?faces-redirect=true";
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

        switch (batchBill.getBillTypeAtomic()) {
            case OPD_BATCH_BILL_WITH_PAYMENT:
                billSearch.fetchPatientInvestigationsAllowBypassSampleProcess(batchBill);
                return "/opd/opd_batch_bill_print?faces-redirect=true";
            case PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT:
                return "/opd/opd_package_batch_bill_print?faces-redirect=true";
            default:
                return "";
        }

    }

    public String navigateToViewPackageBatchBill() {
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
        if (bill.getBillType() != BillType.OpdBathcBill) {
            JsfUtil.addErrorMessage("Please Search Again and View Bill");
            bills = new ArrayList<>();
            return "";
        }

        batchBill = billFacade.find(bill.getId());

        String jpql;
        Map m = new HashMap();
        jpql = "select b "
                + " from Bill b"
                + " where b.backwardReferenceBill=:batchBill";
        m.put("batchBill", batchBill);
        bills = getFacade().findByJpql(jpql, m);

        for (Bill b : bills) {
            getBillBean().checkBillItemFeesInitiated(b);
        }

        duplicatePrint = true;

        return "/opd/opd_package_batch_bill_print?faces-redirect=true";
    }

    public String navigateToViewOpdBatchBill(Bill bb) {
        if (bb == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }
        if (bb.getId() == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }

        if (bb.getBillType() == null) {
            JsfUtil.addErrorMessage("No bill type");
            return null;
        }
        batchBill = bb;
        Long batchBillId = bb.getId();

        batchBill = billFacade.find(batchBillId);
        String jpql;
        Map m = new HashMap();
        jpql = "select b "
                + " from Bill b"
                + " where b.backwardReferenceBill.id=:id";
        m.put("id", batchBillId);
        bills = getFacade().findByJpql(jpql, m);
        payments = billService.fetchBillPayments(batchBill);
        for (Bill b : bills) {
            getBillBean().checkBillItemFeesInitiated(b);
        }
        duplicatePrint = true;
        return "/opd/opd_batch_bill_print?faces-redirect=true";
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
        indication = null;
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
//        opdBill.setPaidAmount(opdPaymentCredit + opdBill.getPaidAmount());
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

    public StaffService getStaffBean() {
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

    public void changeBillDoctorByReferral() {
        if (referredBy == null) {
            JsfUtil.addErrorMessage("No referring doctor");
            return;
        }
        getCurrentlyWorkingStaff().add(referredBy);
        selectedCurrentlyWorkingStaff = referredBy;
    }

    public String getStrTenderedValue() {
        return strTenderedValue;
    }

    public void setStrTenderedValue(String strTenderedValue) {

        this.strTenderedValue = strTenderedValue;
        try {
            cashPaid = Double.parseDouble(strTenderedValue);
        } catch (NumberFormatException e) {
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
        if (configOptionApplicationController.getBooleanValueByKey("Save the Patient with Patient Status")) {
            if (patient != null) {
                foreigner = patient.getPerson().isForeigner();
            }
        }
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

    public void savePatient(Patient p) {
        setPatient(p);
        savePatient();
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
            getPatient().setHasAnAccount(false);
            getPatient().setCreditLimit(0.0);
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

    public void sendSmsOnOpdBillSettling(String smsMessage) {
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

//    public boolean createMultipleBillsFromOrderListOld() {
//        bills = new ArrayList<>();
//        Set<Department> billDepts = new HashSet<>();
//        for (BillEntry e : lstBillEntries) {
//            billDepts.add(e.getBillItem().getItem().getDepartment());
//            // Category is available as Category c=e.getBillItem().getItem().getCategory();
//        }
//
//        for (Department d : billDepts) {
//            Bill myBill = new BilledBill();
//            myBill = saveBill(d, myBill);
//            if (myBill == null) {
//                return false;
//            }
//            List<BillEntry> tmp = new ArrayList<>();
//            for (BillEntry e : lstBillEntries) {
//                if (Objects.equals(e.getBillItem().getItem().getDepartment().getId(), d.getId())) {
//                    BillItem bi = getBillBean().saveBillItem(myBill, e, getSessionController().getLoggedUser());
//                    myBill.getBillItems().add(bi);
//                    tmp.add(e);
//                }
//            }
//            if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
//                myBill.setCashPaid(cashPaid);
//            }
//            getBillFacade().edit(myBill);
//            getBillBean().calculateBillItemsForOpdBill(myBill, tmp, getBillFeeBundleEntrys());
//            createPaymentsForBills(myBill, tmp);
//            getBillBean().checkBillItemFeesInitiated(myBill);
//            getBills().add(myBill);
//        }
//
//        return true;
//    }
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
        Set<Department> billDepts = new HashSet<>();
        for (BillEntry e : lstBillEntries) {
            Department prformingDept = departmentResolver.resolvePerformingDepartment(sessionController.getDepartment(), e.getBillItem().getItem());
            billDepts.add(prformingDept);
        }
        for (Department d : billDepts) {
            Bill myBill = new BilledBill();
            myBill = saveBill(d, myBill);
            if (myBill == null) {
                return false;
            }
            List<BillEntry> tmp = new ArrayList<>();
            for (BillEntry e : lstBillEntries) {
                Department prformingDept = departmentResolver.resolvePerformingDepartment(sessionController.getDepartment(), e.getBillItem().getItem());
                if (Objects.equals(prformingDept.getId(), d.getId())) {
                    BillItem bi = getBillBean().saveBillItemForOpdBill(myBill, e, getSessionController().getLoggedUser(), getBillFeeBundleEntrys());
                    myBill.getBillItems().add(bi);
                    tmp.add(e);
                }
            }
            if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
                myBill.setCashPaid(cashPaid);
            }
            getBillFacade().edit(myBill);
            getBillBean().calculateBillItemsForOpdBill(myBill, tmp, getBillFeeBundleEntrys());
            getBillBean().checkBillItemFeesInitiated(myBill);
            getBills().add(myBill);
        }
        return true;
    }

    private boolean processBillsByDepartmentAndCategory() {
        Map<Department, Set<Category>> billDeptCats = new HashMap<>();
        // Collecting unique Departments and Categories
        for (BillEntry e : lstBillEntries) {
            Department dept = departmentResolver.resolvePerformingDepartment(sessionController.getDepartment(), e.getBillItem().getItem());
            Category cat = e.getBillItem().getItem().getCategory();
            billDeptCats.computeIfAbsent(dept, k -> new HashSet<>()).add(cat);
        }
        // Processing each Department and Category
        for (Map.Entry<Department, Set<Category>> entry : billDeptCats.entrySet()) {
            Department d = entry.getKey();
            for (Category c : entry.getValue()) {
                Bill newlyCreatedIndividualBill = new BilledBill();
                newlyCreatedIndividualBill = saveBill(d, c, newlyCreatedIndividualBill); // Saving the bill for each Department and Category
                if (newlyCreatedIndividualBill == null) {
                    return false;
                }
                List<BillEntry> tmp = new ArrayList<>();

                // Adding BillItems to the Bill
                for (BillEntry billEntry : lstBillEntries) {
                    Department dept = departmentResolver.resolvePerformingDepartment(sessionController.getDepartment(), billEntry.getBillItem().getItem());
                    if (Objects.equals(dept.getId(), d.getId())
                            && Objects.equals(billEntry.getBillItem().getItem().getCategory().getId(), c.getId())) {
                        BillItem bi = getBillBean().saveBillItem(newlyCreatedIndividualBill, billEntry, getSessionController().getLoggedUser());
                        newlyCreatedIndividualBill.getBillItems().add(bi);
                        tmp.add(billEntry);
                    }
                }

                // Handling partial payments if allowed
                if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
                    newlyCreatedIndividualBill.setCashPaid(cashPaid);
                }

                // Finalizing the Bill
                getBillFacade().edit(newlyCreatedIndividualBill);
                getBillBean().calculateBillItemsForOpdBill(newlyCreatedIndividualBill, tmp, getBillFeeBundleEntrys());
//              Payments are Created Only for Batch Bills
//              createPaymentsForBills(myBill, tmp);
                getBillBean().checkBillItemFeesInitiated(newlyCreatedIndividualBill);

                // Adding the finalized Bill to the list of Bills
                getBills().add(newlyCreatedIndividualBill);
            }
        }

        return true;
    }

    public void setPrintigBill() {
        billPrint = bill;
        billsPrint = bills;
        lstBillComponentsPrint = lstBillComponents;
        lstBillEntriesPrint = lstBillEntries;
        lstBillFeesPrint = lstBillFees;
        lstBillItemsPrint = lstBillItems;
    }

    public boolean validatePaymentMethodData() {
        boolean error = false;

        if (getPaymentMethod() == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(netTotal);
            if (getPaymentMethodData().getCreditCard().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("OPD Billing - CreditCard Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a Credit Card Comment..");
                error = true;
            }
        } else if (getPaymentMethod() == PaymentMethod.Cheque) {
            if (getPaymentMethodData().getCheque().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("OPD Billing - Cheque Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a Cheque Comment..");
                error = true;
            }
        } else if (getPaymentMethod() == PaymentMethod.ewallet) {
            if (getPaymentMethodData().getEwallet().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("OPD Billing - E-Wallet Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a E-Wallet Comment..");
                error = true;
            }
        } else if (getPaymentMethod() == PaymentMethod.Slip) {
            if (getPaymentMethodData().getSlip().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("OPD Billing - Slip Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a Slip Comment..");
                error = true;
            }
        } else if (getPaymentMethod() == PaymentMethod.Credit) {
            if (getPaymentMethodData().getCredit().getComment().trim().equals("") && configOptionApplicationController.getBooleanValueByKey("OPD Billing - Credit Comment is Mandatory", false)) {
                JsfUtil.addErrorMessage("Please Enter a Credit Comment..");
                error = true;
            }
        }
        return error;
    }

    public String settleOpdBill() {
        AuditEvent audirEvent = auditEventController.createNewAuditEvent("Settle OPD Bill");
        if (billSettlingStarted) {
            auditEventController.failAuditEvent(audirEvent, "Failed due to already started OPD Bill Settling Process.");
            return null;
        }
        billSettlingStarted = true;
        if (validatePaymentMethodData()) {
            auditEventController.failAuditEvent(audirEvent, "Execute Settle Bill Action Failed because of Payment Method Validation Error.");
            billSettlingStarted = false;
            return null;
        }
        BooleanMessage discountSchemeValidation = discountSchemeValidationService.validateDiscountScheme(paymentMethod, paymentScheme, getPaymentMethodData());
        if (!discountSchemeValidation.isFlag()) {
            auditEventController.failAuditEvent(audirEvent, "Execute Settle Bill Action Failed because of Discount Scheme Validation Error.");
            billSettlingStarted = false;
            JsfUtil.addErrorMessage(discountSchemeValidation.getMessage());
            return null;
        }

        if (!executeSettleBillActions()) {
            auditEventController.failAuditEvent(audirEvent, "Execute Settle Bill Action Failed because of errors in user inputs.");
            billSettlingStarted = false;
            return "";
        }
        boolean sendSmsAfterOpdBilling = configOptionApplicationController.getBooleanValueByKey("Send SMS after OPD Billing", false);
        String smsTempalteForTheSmsAfterOpdBilling = configOptionApplicationController.getLongTextValueByKey("SMS Tempalte for the Sms after OPD Billing");
        if (sendSmsAfterOpdBilling && smsTempalteForTheSmsAfterOpdBilling != null && !smsTempalteForTheSmsAfterOpdBilling.trim().equals("")) {
            sendSmsOnOpdBillSettling(smsTempalteForTheSmsAfterOpdBilling);
        }

        auditEventController.completeAuditEvent(audirEvent);
        billSettlingStarted = false;

        if (patientEncounter != null) {
            return "/inward/inward_service_batch_bill_print?faces-redirect=true";
        } else {
            billSearch.fetchPatientInvestigationsAllowBypassSampleProcess(getBatchBill());
            return "/opd/opd_batch_bill_print?faces-redirect=true";
        }
    }

    private boolean executeSettleBillActions() {
        if (errorCheck()) {
            return false;
        }
        savePatient();
        bills = new ArrayList<>();
        boolean oneOpdBillForAllDepartments = configOptionApplicationController.getBooleanValueByKey("One OPD Bill For All Departments and Categories", true);
        boolean oneOpdBillForEachDepartment = configOptionApplicationController.getBooleanValueByKey("One OPD Bill For Each Department", false);
        boolean oneOpdBillForEachCategory = configOptionApplicationController.getBooleanValueByKey("One OPD Bill For Each Category", false);
        boolean oneOpdBillForEachDepartmentAndCategoryCombination = configOptionApplicationController.getBooleanValueByKey("One OPD Bill For Each Department and Category Combination", false);

        BilledBill newBatchBill = new BilledBill();

        if (oneOpdBillForAllDepartments) {
            Bill newSingleBill = new BilledBill();
            newSingleBill = saveBill(sessionController.getDepartment(), newSingleBill);
            if (newSingleBill == null) {
                return false;
            }
            List<BillItem> list = new ArrayList<>();
            for (BillEntry billEntry : getLstBillEntries()) {
                list.add(getBillBean().saveBillItem(newSingleBill, billEntry, getSessionController().getLoggedUser()));
            }
            newSingleBill.setBillItems(list);
            newSingleBill.setBillTotal(newSingleBill.getNetTotal());
            if (patientEncounter != null) {
                newSingleBill.setIpOpOrCc("IP");
                newSingleBill.setPatientEncounter(patientEncounter);
            } else {
                newSingleBill.setIpOpOrCc("OP");
            }
            getBillFacade().edit(newSingleBill);
            getBillBean().calculateBillItemsForOpdBill(newSingleBill, getLstBillEntries(), getBillFeeBundleEntrys());
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
            processBillsByDepartment();
        } else if (oneOpdBillForEachCategory) {
            JsfUtil.addErrorMessage("Still Under Development");
            return false;
        } else {
            JsfUtil.addErrorMessage("Still Under Development");
            return false;
        }

        saveBatchBill();
        if (patientEncounter != null) {
            getBatchBill().setIpOpOrCc("IP");
            getBatchBill().setPatientEncounter(patientEncounter);
            getFacade().edit(getBatchBill());
        }
        createPaymentsForBills(getBatchBill(), getLstBillEntries());

        drawerController.updateDrawerForIns(payments);
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
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(getPatient(), sessionController.getDepartment());
            patientDepositController.updateBalance(getBatchBill(), pd);
        }
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            paymentService.updateBalances(payments);
        }

        if (getToken() != null) {
            getToken().setBill(getBatchBill());
            tokenFacade.edit(getToken());
            markToken(getBatchBill());
        }

        JsfUtil.addSuccessMessage("Bill Saved");
        setPrintigBill();
        checkBillValues();

        //billService.calculateBillBreakdownAsHospitalCcAndStaffTotalsByBillFees(getBills());
        billService.createBillItemFeeBreakdownFromBills(getBills());
        boolean generateBarcodesForSampleTubesAtBilling = configOptionApplicationController.getBooleanValueByKey("Need to Generate Barcodes for Sample Tubes at OPD Billing Automatically", false);
        if (generateBarcodesForSampleTubesAtBilling) {
            for (Bill b : getBills()) {
                patientInvestigationController.generateBarcodesForSelectedBill(b);
            }
        }
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

        if (billItemTotal != b.getTotal() || billItemDiscount != b.getDiscount() || billItemNetTotal != b.getNetTotal() || roundOff(billItemVatPlusNetValue) != roundOff(b.getVatPlusNetTotal())) {
            return true;
        }

        Double[] billFeeValues = billBean.fetchBillFeeValues(b);
        double billFeeTotal = billFeeValues[0];
        double billFeeDiscount = billFeeValues[1];
        double billFeeNetTotal = billFeeValues[2];
        double billFeeVatPlusNetValue = billFeeValues[3] + billFeeValues[2];

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
    StaffService staffBean;

    private void saveBillItemSessions() {
        for (BillEntry be : lstBillEntries) {
            BillSession newlyCreatedBillItemSession;
            newlyCreatedBillItemSession = getServiceSessionBean().createBillSession(be.getBillItem());
            be.getBillItem().setBillSession(newlyCreatedBillItemSession);
            if (be.getBillItem().getBillSession() != null) {
                getBillSessionFacade().create(be.getBillItem().getBillSession());
            }
            if (be.getBillItem().getBill().getSingleBillSession() == null) {
                be.getBillItem().getBill().setSingleBillSession(newlyCreatedBillItemSession);
                billFacade.edit(be.getBillItem().getBill());
            }
        }
    }

    private void saveBatchBill() {
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
        newBatchBill.setCreditCompany(creditCompany);
        newBatchBill.setComments(comment);
        newBatchBill.setIndication(indication);
        newBatchBill.setIpOpOrCc("OP");
        boolean billNumberByYear;
        String batchBillId; 
        billNumberByYear = configOptionApplicationController.getBooleanValueByKey("Bill Numbers are based on Year.", false);

        boolean opdBillNumberGenerateStrategyForFromDepartmentAndToDepartmentCombination
                
                = configOptionApplicationController.getBooleanValueByKey("OPD Bill Number Generation Strategy - Separate Bill Number for fromDepartment, toDepartment and BillTypes", false);
        
        boolean opdBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices
                = configOptionApplicationController.getBooleanValueByKey("OPD Bill Number Generation Strategy - Single Number for OPD and Inpatient Investigations and Services", false);

        
        if (opdBillNumberGenerateStrategyForFromDepartmentAndToDepartmentCombination) {
            batchBillId = getBillNumberGenerator().departmentBillNumberGeneratorYearlyByFromDepartmentAndToDepartment(null, department, BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
        } else if (opdBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices) {
            List<BillTypeAtomic> opdAndInpatientBills = BillTypeAtomic.findOpdAndInpatientServiceAndInvestigationBatchBillTypes();
            batchBillId = getBillNumberGenerator().departmentBatchBillNumberGeneratorYearlyForInpatientAndOpdServices(getSessionController().getDepartment(), opdAndInpatientBills);
        } else {
            if (billNumberByYear) {
                batchBillId = getBillNumberGenerator().departmentBillNumberGeneratorYearly(
                        getSessionController().getInstitution(),
                        getSessionController().getDepartment(),
                        BillType.OpdBathcBill,
                        BillClassType.BilledBill);
            } else {
                batchBillId = getBillNumberGenerator().departmentBillNumberGeneratorYearly(
                        getSessionController().getDepartment(),
                        BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
            }
        }
        newBatchBill.setInsId(batchBillId);
        newBatchBill.setDeptId(batchBillId);
        
        newBatchBill.setGrantTotal(total);
        newBatchBill.setTotal(total);
        newBatchBill.setDiscount(discount);
        newBatchBill.setBillTime(new Date());
        newBatchBill.setBillTotal(netTotal);
        newBatchBill.setBillDate(new Date());
        newBatchBill.setStaff(staff);
        newBatchBill.setToStaff(toStaff);
        newBatchBill.setCreatedAt(new Date());
        newBatchBill.setCreater(getSessionController().getLoggedUser());
        newBatchBill.setFromStaff(selectedCurrentlyWorkingStaff);

        getBillFacade().create(newBatchBill);

        double dbl = 0;
        double reminingCashPaid = cashPaid;
        int billCount = 1;
        for (Bill b : bills) {
            b.setBackwardReferenceBill(newBatchBill);
            if (billNumberByYear) {
                b.setDeptId(batchBillId + "/" + String.format("%02d", billCount));
            }
            billCount++;
            dbl += b.getNetTotal();

//            if (getSessionController().getDepartmentPreference().isPartialPaymentOfOpdBillsAllowed()) {
//                b.setCashPaid(reminingCashPaid);
//                if (reminingCashPaid > b.getTransSaleBillTotalMinusDiscount()) {
//                    b.setBalance(0.0);
//                    b.setNetTotal(b.getTransSaleBillTotalMinusDiscount());
//                } else {
//                    b.setBalance(b.getTotal() - b.getCashPaid());
//                    b.setNetTotal(reminingCashPaid);
//                }
//            }
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
        newBill.setIpOpOrCc("OP");
        newBill.setComments(comment);
        newBill.setIndication(indication);
        getBillBean().setPaymentMethodData(newBill, paymentMethod, getPaymentMethodData());
        newBill.setBillDate(new Date());
        newBill.setBillTime(new Date());
        newBill.setPatient(patient);

        if (localNumber != null) {
            newBill.setLocalNumber(localNumber);
        }
        if (paymentMethod == PaymentMethod.Credit) {
            String creditRefNo = paymentMethodData.getCredit().getReferenceNo();
            newBill.setReferenceNumber(creditRefNo);
        }
        if (paymentMethod == PaymentMethod.Card) {
            if (comment == null) {
                String cardComment = paymentMethodData.getCreditCard().getComment();
                newBill.setComments(cardComment);
            } else {
                newBill.setComments(comment);
            }
        }
        String deptId;

        
        boolean opdBillNumberGenerateStrategyForFromDepartmentAndToDepartmentCombination
                = configOptionApplicationController.getBooleanValueByKey("OPD Bill Number Generation Strategy - Separate Bill Number for fromDepartment, toDepartment and BillTypes", false);
        
        boolean opdBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices
                = configOptionApplicationController.getBooleanValueByKey("OPD Bill Number Generation Strategy - Single Number for OPD and Inpatient Investigations and Services", false);

        
        if (opdBillNumberGenerateStrategyForFromDepartmentAndToDepartmentCombination) {
            deptId = getBillNumberGenerator().departmentBillNumberGeneratorYearlyByFromDepartmentAndToDepartment(bt, sessionController.getDepartment(), BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        } else if (opdBillNumberGenerateStrategySingleNumberForOpdAndInpatientInvestigationsAndServices) {
            List<BillTypeAtomic> opdAndInpatientBills = BillTypeAtomic.findOpdAndInpatientServiceAndInvestigationIndividualBillTypes();
            deptId = getBillNumberGenerator().departmentBillNumberGeneratorYearly(sessionController.getDepartment(), opdAndInpatientBills);
        } else {
            deptId = getBillNumberGenerator().departmentBillNumberGeneratorYearly(bt, BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        }

//        newBill.setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(patient, getSessionController().getApplicationPreference().isMembershipExpires()));
        newBill.setPaymentScheme(getPaymentScheme());
        newBill.setPaymentMethod(paymentMethod);
        newBill.setCreatedAt(new Date());
        newBill.setCreater(getSessionController().getLoggedUser());

        //SETTING INS ID
        recurseCount = 0;

        newBill.setInsId(deptId);
        newBill.setDeptId(deptId);

        if (newBill.getId() == null) {
            getFacade().create(newBill);
        } else {
            getFacade().edit(newBill);
        }

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
        newBill.setIndication(indication);
        newBill.setIpOpOrCc("OP");

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
        if (getPatient().getPerson().getName() == null || getPatient().getPerson().getName().trim().isEmpty() || getPatient().getPerson().getSex() == null || getPatient().getPerson().getDob() == null) {
            JsfUtil.addErrorMessage("Can not bill without Patient Name, Age or Sex.");
            return true;
        }
        if (!Person.checkAgeSex(getPatient().getPerson().getDob(), getPatient().getPerson().getSex(), getPatient().getPerson().getTitle())) {
            JsfUtil.addErrorMessage("Mismatch in Title and Gender. Please Check the Title, Age and Sex");
            return true;
        }
        return false;
    }

    @Override
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
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffWelfare().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();
            }
            remainAmount = total - multiplePaymentMethodTotalValue;
            return total - multiplePaymentMethodTotalValue;

        }
        remainAmount = total;
        return total;
    }

    @Override
    public void recieveRemainAmountAutomatically() {
        remainAmount = calculatRemainForMultiplePaymentTotal();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            // Guard against empty component list
            if (paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
                return;
            }

            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
            switch (pm.getPaymentMethod()) {
                case Cash:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getCash().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCash().setTotalValue(remainAmount);
                    }
                    break;
                case Card:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getCreditCard().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCreditCard().setTotalValue(remainAmount);
                    }
                    break;
                case Cheque:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getCheque().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCheque().setTotalValue(remainAmount);
                    }
                    break;
                case Slip:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getSlip().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getSlip().setTotalValue(remainAmount);
                    }
                    break;
                case ewallet:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getEwallet().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getEwallet().setTotalValue(remainAmount);
                    }
                    break;
                case PatientDeposit:
                    if (patient != null) {
                        pm.getPaymentMethodData().getPatient_deposit().setPatient(patient);
                        PatientDeposit pd = patientDepositController.checkDepositOfThePatient(patient, sessionController.getDepartment());
                        pm.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                        // Only set if user hasn't already entered a value
                        if (pm.getPaymentMethodData().getPatient_deposit().getTotalValue() == 0.0) {
                            if (remainAmount >= pm.getPaymentMethodData().getPatient_deposit().getPatientDepost().getBalance()) {
                                pm.getPaymentMethodData().getPatient_deposit().setTotalValue(pm.getPaymentMethodData().getPatient_deposit().getPatientDepost().getBalance());
                            } else {
                                pm.getPaymentMethodData().getPatient_deposit().setTotalValue(remainAmount);
                            }
                        }
                    }

                    break;
                case Credit:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getCredit().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
                    }
                    break;
                case Staff:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getStaffCredit().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getStaffCredit().setTotalValue(remainAmount);
                    }
                    break;
                case Staff_Welfare:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getStaffWelfare().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getStaffWelfare().setTotalValue(remainAmount);
                    }
                    break;
                case OnlineSettlement:
                    // Only set if user hasn't already entered a value
                    if (pm.getPaymentMethodData().getOnlineSettlement().getTotalValue() == 0.0) {
                        pm.getPaymentMethodData().getOnlineSettlement().setTotalValue(remainAmount);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected value: " + pm.getPaymentMethod());
            }

        }
        listnerForPaymentMethodChange();

    }

    @Override
    public boolean isLastPaymentEntry(ComponentDetail cd) {
        if (cd == null ||
            paymentMethodData == null ||
            paymentMethodData.getPaymentMethodMultiple() == null ||
            paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null ||
            paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
            return false;
        }

        List<ComponentDetail> details = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails();
        int lastIndex = details.size() - 1;
        int currentIndex = details.indexOf(cd);
        return currentIndex != -1 && currentIndex == lastIndex;
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
            JsfUtil.addErrorMessage("Can not bill without a name for the Patient !");
            return true;
        }
        if (getPatient().getPerson().getSex() == null) {
            JsfUtil.addErrorMessage("Can not bill without sex for the Patient !");
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
//            if (!getPatient().getHasAnAccount()) {
//                JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
//                return true;
//            }
            double creditLimitAbsolute = 0.0;
//            if (getPatient().getCreditLimit() == null) {
//                creditLimitAbsolute = 0.0;
//            } else {
//                creditLimitAbsolute = Math.abs(getPatient().getCreditLimit());
//            }
//
            double runningBalance;
//            if (getPatient().getRunningBalance() != null) {
//                runningBalance = getPatient().getRunningBalance();
//            } else {
//                runningBalance = 0.0;
//            }
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(getPatient(), sessionController.getDepartment());
            runningBalance = pd.getBalance();
            double availableForPurchase = runningBalance + creditLimitAbsolute;

            if (netTotal > availableForPurchase) {
                JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                return true;
            }

        }

        if (paymentMethod == PaymentMethod.Credit) {
            ComponentDetail cd = getPaymentMethodData().getCredit();
            if (cd.getInstitution() == null) {
                JsfUtil.addErrorMessage("Please select Credit Company");
                return true;
            }
            creditCompany = cd.getInstitution();
        }

        syncStaffSelectionFromPaymentDetails();

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
                if (cd.getPaymentMethod().equals(PaymentMethod.PatientDeposit)) {
                    if (!getPatient().getHasAnAccount()) {
                        JsfUtil.addErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                        return true;
                    }
                    double creditLimitAbsolute = Math.abs(getPatient().getCreditLimit());
                    PatientDeposit pd = patientDepositController.checkDepositOfThePatient(patient, sessionController.getDepartment());
                    if (pd == null) {
                        JsfUtil.addErrorMessage("No Patient Deposit.");
                        return true;
                    }
                    double runningBalance = pd.getBalance();

                    double availableForPurchase = runningBalance + creditLimitAbsolute;

                    if (cd.getPaymentMethodData().getPatient_deposit().getTotalValue() > availableForPurchase) {

                        JsfUtil.addErrorMessage("No Sufficient Patient Deposit");
                        return true;
                    }

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
                } else if (cd.getPaymentMethod().equals(PaymentMethod.Staff_Welfare)) {
                    if (cd.getPaymentMethodData().getStaffWelfare().getTotalValue() == 0.0 || cd.getPaymentMethodData().getStaffWelfare().getToStaff() == null) {
                        JsfUtil.addErrorMessage("Please fill the Paying Amount and Staff Name");
                        return true;
                    }
                    Staff selectedStaff = cd.getPaymentMethodData().getStaffWelfare().getToStaff();
                    double proposedValue = cd.getPaymentMethodData().getStaffWelfare().getTotalValue();
                    double utilized = Math.abs(selectedStaff.getAnnualWelfareUtilized());
                    if (utilized + proposedValue > selectedStaff.getAnnualWelfareQualified()) {
                        JsfUtil.addErrorMessage("No enough credit.");
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
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getStaffWelfare().getTotalValue();
                multiplePaymentMethodTotalValue += cd.getPaymentMethodData().getOnlineSettlement().getTotalValue();
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

    private void syncStaffSelectionFromPaymentDetails() {
        if (paymentMethod == null) {
            return;
        }
        if (paymentMethodData == null) {
            return;
        }
        if (toStaff != null) {
            return;
        }
        if (paymentMethod != PaymentMethod.Staff && paymentMethod != PaymentMethod.Staff_Welfare) {
            return;
        }
        ComponentDetail staffComponent = paymentMethod == PaymentMethod.Staff
                ? paymentMethodData.getStaffCredit()
                : paymentMethodData.getStaffWelfare();
        if (staffComponent != null && staffComponent.getToStaff() != null) {
            setToStaff(staffComponent.getToStaff());
        }
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

        if (getCurrentBillItem().getItem().isRequestForQuentity()) {
            if (getCurrentBillItemQty() == null || getCurrentBillItemQty() == 0.0) {
                setCurrentBillItemQty(null);
                JsfUtil.addErrorMessage("Quentity is Missing ..! ");
                return;
            }
        } else {
            setCurrentBillItemQty(1.0);
        }

        for (BillEntry bi : lstBillEntries) {
            if (bi.getBillItem() != null && getCurrentBillItem() != null && getCurrentBillItem().getItem() != null && bi.getBillItem().getItem().equals(getCurrentBillItem().getItem())) {
                JsfUtil.addErrorMessage("Can't select same item " + getCurrentBillItem().getItem());
                setCurrentBillItem(null);
                setCurrentBillItemQty(null);
                return;
            }
        }

        BillItem bi = new BillItem();
        bi.copy(getCurrentBillItem());
        bi.setTmpQty(getCurrentBillItemQty());
        bi.setSessionDate(sessionDate);
        lastBillItem = bi;
        BillEntry addingEntry = new BillEntry();
        addingEntry.setBillItem(bi);
        addingEntry.setLstBillComponents(getBillBean().billComponentsFromBillItem(bi));

        List<BillFee> allBillFees;

        // Department-based billing functionality is now active and operational
        boolean addAllBillFees = configOptionApplicationController.getBooleanValueByKey("OPD Bill Fees are the same for all departments, institutions and sites for " + sessionController.getDepartment().getName(), true);
        boolean siteBasedBillFees = configOptionApplicationController.getBooleanValueByKey("OPD Bill Fees are based on the site for " + sessionController.getDepartment().getName(), false);
        boolean departmentBasedBillFees = configOptionApplicationController.getBooleanValueByKey("OPD Bill Fees are based on the department for " + sessionController.getDepartment().getName(), false);

        if (addAllBillFees) {
            allBillFees = getBillBean().billFeefromBillItem(bi);
        } else if (siteBasedBillFees) {
            allBillFees = getBillBean().forInstitutionBillFeesFromBillItem(bi, sessionController.getDepartment().getSite());
        } else if (departmentBasedBillFees) {
            allBillFees = getBillBean().forDepartmentBillFeefromBillItem(bi, sessionController.getDepartment());
        } else {
            allBillFees = getBillBean().billFeefromBillItem(bi);
        }

        List<BillFeeBundleEntry> billItemBillFeeBundleEntries = getBillBean().bundleFeesByName(allBillFees);

        addingEntry.setLstBillFees(allBillFees);

        getBillFeeBundleEntrys().addAll(billItemBillFeeBundleEntries);

        addStaffToBillFees(addingEntry.getLstBillFees());

        addingEntry.setLstBillSessions(getBillBean().billSessionsfromBillItem(bi));
        getLstBillEntries().add(addingEntry);

        bi.setRate(getBillBean().billItemRate(addingEntry));
        bi.setNetValue(bi.getRate() * bi.getQty());

        if (bi.getItem().isVatable()) {
            bi.setVat(bi.getNetValue() * bi.getItem().getVatPercentage() / 100);
        }

        bi.setVatPlusNetValue(bi.getNetValue() + bi.getVat());

        calTotals();

        // Previously the system blocked adding items with a zero value. This
        // restriction prevented recording fees that intentionally have no
        // charge. Issue #12544 requires allowing such entries, so the check for
        // a zero net value is removed. Items with a value of 0 are now
        // permitted and will be processed like any other item.
        clearBillItemValues();
        boolean clearItemAfterAddingToOpdBill = configOptionApplicationController.getBooleanValueByKey("Clear Item After Adding To Opd Bill", true);
        if (clearItemAfterAddingToOpdBill) {
            setItemLight(null);
        } else {
            setItemLight(itemLight);
        }
        setCurrentBillItemQty(null);
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
        currentBillItemQty = null;
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
        setBillFeeBundleEntrys(null);
        setStaff(null);
        setToStaff(null);
        setComment(null);
        setIndication(null);
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
        billSettlingStarted = false;
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
        setBillFeeBundleEntrys(null);
        setStaff(null);
        setToStaff(null);
        setComment(null);
        setIndication(null);
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

    public void calTotals() {
        if (paymentMethod == null) {
            return;
        }

        double billDiscount = 0.0;
        double billGross = 0.0;
        double billNet = 0.0;
        double billVat = 0.0;
        for (BillEntry be : getLstBillEntries()) {

            double entryGross = 0.0;
            double entryDis = 0.0;
            double entryNet = 0.0;
            double entryVat = 0.0;
            double entryVatPlusNet = 0.0;

            BillItem bi = be.getBillItem();

            for (BillFee bf : be.getLstBillFees()) {

                boolean needToAdd = billFeeIsThereAsSelectedInBillFeeBundle(bf);
                if (needToAdd) {

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

                }
            }

            bi.setDiscount(entryDis);
            bi.setGrossValue(entryGross);
            bi.setNetValue(entryNet);
            bi.setVat(entryVat);
            bi.setVatPlusNetValue(roundOff(entryVatPlusNet));

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

    private boolean billFeeIsThereAsSelectedInBillFeeBundle(BillFee bf) {
        if (bf == null) {
            return false;
        }
        if (bf.getFee() == null) {
            return false;
        }
        boolean found = false;
        for (BillFeeBundleEntry bfbe : getBillFeeBundleEntrys()) {
            if (bfbe.getSelectedBillFee().equals(bf)) {
                found = true;
            } else {
            }
        }
        return found;
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
        patientController.setQuickSearchPhoneNumber(null);
        if (sessionController.getOpdBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                clearBillItemValues();
                clearBillValues();
                paymentMethodData = null;
                paymentScheme = null;
                paymentMethod = PaymentMethod.Cash;
                patientEncounter = null;
                collectingCentreBillController.setCollectingCentre(null);
                if (sessionController.getOpdBillItemSearchByAutocomplete()) {
                    return "/opd/opd_bill_ac?faces-redirect=true";
                } else {
                    return "/opd/opd_bill?faces-redirect=true";
                }
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
            patientEncounter = null;
            collectingCentreBillController.setCollectingCentre(null);
            if (sessionController.getOpdBillItemSearchByAutocomplete()) {
                return "/opd/opd_bill_ac?faces-redirect=true";
            } else {
                return "/opd/opd_bill?faces-redirect=true";
            }
        }
    }

    public String navigateToNewInwardServiceBill() {
        if (sessionController.getInwardServiceBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                clearBillItemValues();
                clearBillValues();
                paymentMethodData = null;
                paymentScheme = null;
                paymentMethod = PaymentMethod.Cash;
                collectingCentreBillController.setCollectingCentre(null);
                patientEncounter = null;
                if (sessionController.getInwardServiceBillItemSearchByAutocomplete()) {
                    return "/inward/inward_service_bill_ac?faces-redirect=true";
                } else {
                    return "/inward/inward_service_bill?faces-redirect=true";
                }
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
            patientEncounter = null;
            if (sessionController.getInwardServiceBillItemSearchByAutocomplete()) {
                return "/inward/inward_service_bill_ac?faces-redirect=true";
            } else {
                return "/inward/inward_service_bill?faces-redirect=true";
            }
        }
    }

    public String navigateToNewOpdBillAutocomplete() {
        if (sessionController.getOpdBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                clearBillItemValues();
                clearBillValues();
                paymentMethodData = null;
                paymentScheme = null;
                paymentMethod = PaymentMethod.Cash;
                collectingCentreBillController.setCollectingCentre(null);
                return "/opd/opd_bill_ac?faces-redirect=true";
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
            return "/opd/opd_bill_ac?faces-redirect=true";
        }
    }

    public List<ItemLight> completeOpdItemsByWord(String query) {
        List<ItemLight> filteredItems = new ArrayList<>();
        Long defaultValue = 10l;
        Long maxResultsLong = configOptionApplicationController.getLongValueByKey("Number of Maximum Results for Item Search in Autocompletes", defaultValue);
        int maxResults = maxResultsLong.intValue();

        // Department-based billing functionality is now active and operational
        boolean addAllBillFees = configOptionApplicationController.getBooleanValueByKey("OPD Bill Fees are the same for all departments, institutions and sites for " + sessionController.getDepartment().getName(), true);
        boolean siteBasedBillFees = configOptionApplicationController.getBooleanValueByKey("OPD Bill Fees are based on the site for " + sessionController.getDepartment().getName(), false);
        boolean departmentBasedBillFees = configOptionApplicationController.getBooleanValueByKey("OPD Bill Fees are based on the department for " + sessionController.getDepartment().getName(), false);

        // Split the query into individual tokens (space-separated)
        String[] tokens = query.toLowerCase().split("\\s+");

        for (ItemLight opdItem : getOpdItems()) {
            boolean matchFound = true;

            // Check if all tokens match either the name or code
            for (String token : tokens) {
                if (!(opdItem.getName().toLowerCase().contains(token)
                        || opdItem.getCode().toLowerCase().contains(token))) {
                    matchFound = false;
                    break;
                }
            }

            if (matchFound) {
                if (siteBasedBillFees) {
                    FeeValue f = feeValueController.getSiteFeeValue(opdItem.getId(), sessionController.getLoggedSite());
                    if (f != null) {
                        opdItem.setTotal(f.getTotalValueForLocals());
                        opdItem.setTotalForForeigner(f.getTotalValueForForeigners());
                    }
                } else if (departmentBasedBillFees) {
                    FeeValue f = feeValueController.getDepartmentFeeValue(opdItem.getId(), sessionController.getDepartment());
                    if (f != null) {
                        opdItem.setTotal(f.getTotalValueForLocals());
                        opdItem.setTotalForForeigner(f.getTotalValueForForeigners());
                    }
                }
                filteredItems.add(opdItem);
            }

            // Limit the result set to maxResults
            if (filteredItems.size() >= maxResults) {
                break;
            }
        }
        return filteredItems;
    }

    public String navigateToNewOpdBillFromToken() {
        if (sessionController.getOpdBillingAfterShiftStart()) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                paymentMethodData = null;
                paymentScheme = null;
                paymentMethod = PaymentMethod.Cash;
                collectingCentreBillController.setCollectingCentre(null);
                if (getToken() != null) {
                    setPatient(token.getPatient());
                }
                if (sessionController.getOpdBillItemSearchByAutocomplete()) {
                    return "/opd/opd_bill_ac?faces-redirect=true";
                } else {
                    return "/opd/opd_bill?faces-redirect=true";
                }
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
            if (sessionController.getOpdBillItemSearchByAutocomplete()) {
                return "/opd/opd_bill_ac?faces-redirect=true";
            } else {
                return "/opd/opd_bill?faces-redirect=true";
            }
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
        if (bs.getBill().getBillTypeAtomic() != BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT) {
            if (bs.getBill().getPatient() == null) {
                return null;
            }

            patient = bs.getBill().getPatient();
        }

        if (bs.getSessionInstance().getStaff() == null) {
            return null;
        }
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
        if (sessionController.getOpdBillItemSearchByAutocomplete()) {
            return "/opd/opd_bill_ac?faces-redirect=true";
        } else {
            return "/opd/opd_bill?faces-redirect=true";
        }
    }

    public String toOpdBilling() {
        if (sessionController.getOpdBillItemSearchByAutocomplete()) {
            return "/opd/opd_bill_ac?faces-redirect=true";
        } else {
            return "/opd/opd_bill?faces-redirect=true";
        }
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
        if (getIndex() != null) {
            BillEntry temp = getLstBillEntries().get(getIndex());
            recreateList(temp);
            calTotals();
        }
    }

    public void recreateList(BillEntry r) {
        List<BillEntry> temp = new ArrayList<>();
        List<BillFeeBundleEntry> newListOfBillFeeBundleEntries = new ArrayList<>();
        for (BillEntry b : getLstBillEntries()) {
            if (b.getBillItem().getItem() != r.getBillItem().getItem()) {
                temp.add(b);
            }
        }
        for (BillFeeBundleEntry bfe : billFeeBundleEntrys) {
            if (bfe.getSelectedBillFee().getBillItem().getItem() != r.getBillItem().getItem()) {
                newListOfBillFeeBundleEntries.add(bfe);
            }
        }
        billFeeBundleEntrys = newListOfBillFeeBundleEntries;
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
                        p.setComments(cd.getPaymentMethodData().getCreditCard().getComment());
                        break;
                    case Cheque:
                        p.setBank(cd.getPaymentMethodData().getCheque().getInstitution());
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCheque().getComment());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCash().getComment());
                        break;
                    case ewallet:
                        p.setPolicyNo(cd.getPaymentMethodData().getEwallet().getReferralNo());
                        p.setReferenceNo(cd.getPaymentMethodData().getEwallet().getReferenceNo());
                        p.setCreditCompany(cd.getPaymentMethodData().getEwallet().getInstitution());
                        p.setPaidValue(cd.getPaymentMethodData().getEwallet().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getEwallet().getComment());
                        break;
                    case Agent:
//                        TODO:Add Details
                        break;
                    case Credit:
                        p.setPolicyNo(cd.getPaymentMethodData().getCredit().getReferralNo());
                        p.setReferenceNo(cd.getPaymentMethodData().getCredit().getReferenceNo());
                        p.setCreditCompany(cd.getPaymentMethodData().getCredit().getInstitution());
                        p.setPaidValue(cd.getPaymentMethodData().getCredit().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getCredit().getComment());
                        break;
                    case PatientDeposit:
                        if (getPatient().getRunningBalance() != null) {
                            getPatient().setRunningBalance(getPatient().getRunningBalance() - cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        } else {
                            getPatient().setRunningBalance(0.0 - cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        }
                        getPatientFacade().edit(getPatient());
                        p.setPaidValue(cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        break;
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                        p.setComments(cd.getPaymentMethodData().getSlip().getComment());
                        p.setReferenceNo(cd.getPaymentMethodData().getSlip().getReferenceNo());
                        p.setPaymentDate(cd.getPaymentMethodData().getSlip().getDate());
                        p.setChequeDate(cd.getPaymentMethodData().getSlip().getDate());

                        break;
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            staffBean.updateStaffCredit(cd.getPaymentMethodData().getStaffCredit().getToStaff(), cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Credit Updated");
                        }
                        break;
                    case Staff_Welfare:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffWelfare().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffWelfare().getToStaff() != null) {
                            staffBean.updateStaffWelfare(cd.getPaymentMethodData().getStaffWelfare().getToStaff(), cd.getPaymentMethodData().getStaffWelfare().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                        }
                        break;
                    case YouOweMe:
                    case MultiplePaymentMethods:
                }

                paymentFacade.create(p);
                cashBookEntryController.writeCashBookEntryAtPaymentCreation(p);
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
                    p.setComments(paymentMethodData.getCreditCard().getComment());
                    break;
                case Cheque:
                    p.setBank(paymentMethodData.getCheque().getInstitution());
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    p.setPaidValue(paymentMethodData.getCheque().getTotalValue());
                    p.setComments(paymentMethodData.getCheque().getComment());
                    break;
                case Cash:
                    p.setPaidValue(paymentMethodData.getCash().getTotalValue());
                    p.setComments(paymentMethodData.getCash().getComment());
                    break;
                case ewallet:
                    p.setBank(paymentMethodData.getEwallet().getInstitution());
                    p.setPolicyNo(paymentMethodData.getEwallet().getReferralNo());
                    p.setReferenceNo(paymentMethodData.getEwallet().getReferenceNo());
                    p.setCreditCompany(paymentMethodData.getEwallet().getInstitution());
                    p.setPaidValue(paymentMethodData.getEwallet().getTotalValue());
                    p.setComments(paymentMethodData.getEwallet().getComment());
                    break;

                case Agent:
                    break;
                case Credit:
                    p.setPolicyNo(paymentMethodData.getCredit().getReferralNo());
                    p.setComments(paymentMethodData.getCredit().getComment());
                    p.setReferenceNo(paymentMethodData.getCredit().getReferenceNo());
                    p.setCreditCompany(paymentMethodData.getCredit().getInstitution());
                    break;
                case PatientDeposit:
                    break;
                case Slip:
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setPaidValue(paymentMethodData.getSlip().getTotalValue());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                    p.setPaymentDate(paymentMethodData.getSlip().getDate());
                    p.setChequeDate(paymentMethodData.getSlip().getDate());
                    p.setComments(paymentMethodData.getSlip().getComment());
                    p.setReferenceNo(paymentMethodData.getSlip().getReferenceNo());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                    break;
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);
            cashBookEntryController.writeCashBookEntryAtPaymentCreation(p);
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

            if (bf.getId() == null) {
                continue;
            }

            if (bf.getId() == null) {
                continue;
            }

            if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdPreBillsAllowed() || getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
                if (Math.abs((bf.getFeeValue() - bf.getSettleValue())) > 0.1) {
                    if (reminingCashPaid >= (bf.getFeeValue() - bf.getSettleValue())) {
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
        if (bf.getId() != null) {
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
    }

    public double calBillPaidValue(Bill b) {
        String jpql = "select sum(bfp.amount) "
                + " from BillFeePayment bfp "
                + " where bfp.retired = :ret "
                + " and bfp.billFee.bill.id = :bid";

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("bid", b.getId());

        double d = billFeePaymentFacade.findDoubleByJpql(jpql, params);
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

    @Override
    public void listnerForPaymentMethodChange() {
        if (paymentMethod == PaymentMethod.PatientDeposit) {
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            getPaymentMethodData().getPatient_deposit().setTotalValue(netTotal);
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(patient, sessionController.getDepartment());
            if (pd != null && pd.getId() != null) {
                getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
            }
        } else if (paymentMethod == PaymentMethod.Card) {
            getPaymentMethodData().getCreditCard().setTotalValue(netTotal);
        } else if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            getPaymentMethodData().getPatient_deposit().setPatient(patient);
            getPaymentMethodData().getPatient_deposit().setTotalValue(calculatRemainForMultiplePaymentTotal());
            PatientDeposit pd = patientDepositController.checkDepositOfThePatient(patient, sessionController.getDepartment());

            if (pd != null && pd.getId() != null) {
                boolean hasPatientDeposit = false;
                for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    if (cd.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                        hasPatientDeposit = true;
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(patient);
                        cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

                    }
                }
            }

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
        selectPaymentSchemeAsPerPatientMembership();
    }

    private void selectPaymentSchemeAsPerPatientMembership() {
        if (patient == null) {
            return;
        }
        if (patient.getPerson().getMembershipScheme() == null) {
            paymentScheme = null;
        } else {
            paymentScheme = patient.getPerson().getMembershipScheme().getPaymentScheme();
        }
        listnerForPaymentMethodChange();
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
        hm.put("q", "%" + query.toUpperCase() + "%");
        hm.put("btp", BillType.InwardAppointmentBill);
        suggestions = getFacade().findByJpql(sql, hm);

        return suggestions;

    }

    public void selectPatientEncounter() {
        if (patientEncounter != null) {
            setPatient(patientEncounter.getPatient());
        } else {
            JsfUtil.addErrorMessage("Please select an encounter first.");
        }
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

    @Override
    public PaymentMethod getPaymentMethod() {
        if (!sessionController.getDepartmentPreference().isPartialPaymentOfOpdBillsAllowed()) {
            if (paymentMethod != PaymentMethod.Cash) {
                strTenderedValue = String.valueOf(netTotal);
            } else {
                strTenderedValue = String.valueOf(0.00);
            }
        }

        return paymentMethod;
    }

    @Override
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
        if (paymentMethodData != null || toStaff != null) {
            getPaymentMethodData().getStaffCredit().setToStaff(toStaff);
            getPaymentMethodData().getStaffWelfare().setToStaff(toStaff);
        }
        if (bill != null) {
            bill.setToStaff(toStaff);
        }
        if (batchBill != null) {
            batchBill.setToStaff(toStaff);
        }
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
        opdItems = null;
        itemApplicationController.reloadItems();
        itemController.reloadItems();
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

    public List<BillFeeBundleEntry> getBillFeeBundleEntrys() {
        if (billFeeBundleEntrys == null) {
            billFeeBundleEntrys = new ArrayList<>();
        }
        return billFeeBundleEntrys;
    }

    public void setBillFeeBundleEntrys(List<BillFeeBundleEntry> billFeeBundleEntrys) {
        this.billFeeBundleEntrys = billFeeBundleEntrys;
    }

    public boolean isBillSettlingStarted() {
        return billSettlingStarted;
    }

    public void setBillSettlingStarted(boolean billSettlingStarted) {
        this.billSettlingStarted = billSettlingStarted;
    }

    public String getLocalNumber() {
        return localNumber;
    }

    public void setLocalNumber(String localNumber) {
        this.localNumber = localNumber;
    }

    public String getRefNo() {
        if (refNo == null) {
            refNo = getPaymentMethodData().getCredit().getReferralNo();
        }
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public double getRemainAmount() {
        return remainAmount;
    }

    public void setRemainAmount(double remainAmount) {
        this.remainAmount = remainAmount;
    }

    public String getReferredByName() {
        return referredByName;
    }

    public void setReferredByName(String referredByName) {
        this.referredByName = referredByName;
    }

    public Double getCurrentBillItemQty() {
        return currentBillItemQty;
    }

    public void setCurrentBillItemQty(Double currentBillItemQty) {
        this.currentBillItemQty = currentBillItemQty;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public String getIndication() {
        return indication;
    }

    public void setIndication(String indication) {
        this.indication = indication;
    }

}
