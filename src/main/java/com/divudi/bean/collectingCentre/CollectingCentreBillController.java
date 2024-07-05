/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.collectingCentre;

import com.divudi.bean.channel.AgentReferenceBookController;
import com.divudi.bean.common.*;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.ItemLight;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.BillListWithTotals;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.ejb.BillEjb;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.StaffBean;
import com.divudi.entity.AgentHistory;
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
import com.divudi.entity.Staff;
import com.divudi.entity.UserPreference;
import com.divudi.entity.WebUser;
import com.divudi.entity.channel.AgentReferenceBook;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.AgentReferenceBookFacade;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.InstitutionType;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
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
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class CollectingCentreBillController implements Serializable, ControllerWithPatient {

    /**
     * EJBs
     */
    @EJB
    BillNumberGenerator billNumberGenerator;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    BillEjb billEjb;
    @EJB
    PaymentFacade PaymentFacade;
    @EJB
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    BillSessionFacade billSessionFacade;
    @EJB
    AgentHistoryFacade agentHistoryFacade;
    @EJB
    ItemFeeFacade itemFeeFacade;
    @EJB
    AgentReferenceBookFacade agentReferenceBookFacade;
    /**
     * Controllers
     */
    @Inject
    ItemController itemController;
    @Inject
    ItemApplicationController itemApplicationController;
    @Inject
    ItemMappingController itemMappingController;
    @Inject
    ApplicationController applicationController;
    @Inject
    ServiceSessionFunctions serviceSessionBean;
    @Inject
    CommonController commonController;
    @Inject
    SessionController sessionController;
    @Inject
    PaymentSchemeController paymentSchemeController;
    @Inject
    CollectingCentreBillController collectingCentreBillController;
    @Inject
    private EnumController enumController;
    @Inject
    DepartmentController departmentController;
    @EJB
    StaffBean staffBean;
    @Inject
    CategoryController categoryController;
    /**
     * Properties
     */
    private ItemLight itemLight;
    List<BillSession> billSessions;
    private static final long serialVersionUID = 1L;
    private boolean printPreview;
    private String patientTabId = "tabNewPt";
    //Interface Data
    private PaymentScheme paymentScheme;
    private Institution collectingCentre;
    private PaymentMethod paymentMethod = PaymentMethod.Agent;
//    private Patient newPatient;
    private Patient patient;
    private Doctor referredBy;
    private Institution referredByInstitution;
    String referralId;
    private List<String> referralIds;
    private Institution creditCompany;
    private Staff staff;
    Staff toStaff;
    private double total;
    private double discount;
    double vat;
    double vatPlusNetTotal;
    private double netTotal;
    private double cashPaid;
    private double cashBalance;
    double cashRemain = cashPaid;
    private BillItem currentBillItem;
    //Bill Items
    private List<BillComponent> lstBillComponents;
    private List<BillFee> lstBillFees;
    private List<BillItem> lstBillItems;
    private List<BillEntry> lstBillEntries;
    private Integer index;
    boolean fromOpdEncounter = false;
    String opdEncounterComments = "";
    int patientSearchTab = 0;
    String comment;
    double opdPaymentCredit;
    BilledBill opdBill;
    Date fromDate;
    Date toDate;
    Department department;
    Institution institution;
    Category category;
    private boolean patientDetailsEditable;
    //Print Last Bill
    Bill billPrint;
    List<Bill> billsPrint;
    private List<BillComponent> lstBillComponentsPrint;
    private List<BillFee> lstBillFeesPrint;
    private List<BillItem> lstBillItemsPrint;
    private List<BillEntry> lstBillEntriesPrint;
    BillType billType;
    private List<ItemLight> opdItems;
    private List<AgentReferenceBook> agentReferenceBooks;
    private List<CollectingCenterBookSummeryRow> bookSummeryRows;

    public List<AgentReferenceBook> getAgentReferenceBooks() {
        return agentReferenceBooks;
    }

    public void setAgentReferenceBooks(List<AgentReferenceBook> agentReferenceBooks) {
        this.agentReferenceBooks = agentReferenceBooks;
    }

    public void selectCollectingCentre() {
        if (collectingCentre == null) {
            JsfUtil.addErrorMessage("Please select a collecting centre");
            return;
        }
        fillAvailableAgentReferanceNumbers(collectingCentre);
        itemController.setCcInstitutionItems(itemController.fillItemsByInstitution(collectingCentre));
    }

    public void deselectCollectingCentre() {
        collectingCentre = null;
        itemController.setCcInstitutionItems(null);
    }

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
    private BillComponentFacade billComponentFacade;
    @EJB
    private BillFeeFacade billFeeFacade;

    List<Bill> bills;
    List<Bill> selectedBills;
    Double grosTotal;
    Bill bill;
    boolean foreigner = false;
    Date sessionDate;
    String strTenderedValue;
    private YearMonthDay yearMonthDay;
    private PaymentMethodData paymentMethodData;
    @EJB
    private CashTransactionBean cashTransactionBean;

    @Inject
    SearchController searchController;
    
    @Inject
    BillController billController;

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
//        vatPlusNetTotal = bt.getVat() + bt.getNetTotal();
//    }
//    public void clear() {
//        opdBill = new BilledBill();
//        printPreview = false;
//        opdPaymentCredit = 0.0;
//        comment = null;
//        searchController.createTableByKeywordToPayBills();
//    }
//    public void clearPharmacy() {
//        opdBill = new BilledBill();
//        printPreview = false;
//        opdPaymentCredit = 0.0;
//        comment = null;
//        searchController.createTablePharmacyCreditToPayBills();
//    }
    public void saveBillOPDCredit() {

        BilledBill temp = new BilledBill();

        if (opdPaymentCredit == 0) {
            JsfUtil.addErrorMessage("Please Select Correct Paid Amount");
            return;
        }
        if (opdPaymentCredit > opdBill.getBalance()) {
            JsfUtil.addErrorMessage("Please Enter Correct Paid Amount");
            return;
        }

        temp.setReferenceBill(opdBill);
        temp.setTotal(opdPaymentCredit);
        temp.setPaidAmount(opdPaymentCredit);
        temp.setNetTotal(opdPaymentCredit);

        temp.setDeptId(getBillNumberGenerator().departmentBillNumberGenerator(getSessionController().getDepartment(), getSessionController().getDepartment(), BillType.CashRecieveBill, BillClassType.BilledBill));
        temp.setInsId(getBillNumberGenerator().institutionBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), BillType.CashRecieveBill, BillClassType.BilledBill, BillNumberSuffix.NONE));
        temp.setBillType(BillType.CashRecieveBill);

        temp.setDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setToDepartment(getSessionController().getLoggedUser().getDepartment());

        temp.setComments(comment);

//        getBillBean().setPaymentMethodData(temp, getPaymentMethod(), getPaymentMethodData());
        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setPaymentMethod(getPaymentMethod());
        temp.setCreatedAt(new Date());
        temp.setCreater(getSessionController().getLoggedUser());
        getFacade().create(temp);
        //create bill fee payments
        //create bill fee payments
        //create bill fee payments
        //create bill fee payments
        reminingCashPaid = opdPaymentCredit;

        Payment p = createPayment(temp, getPaymentMethod());

        String sql = "Select bi From BillItem bi where bi.retired=false and bi.bill.id=" + opdBill.getId();
        List<BillItem> billItems = getBillItemFacade().findByJpql(sql);

        for (BillItem bi : billItems) {
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();

            List<BillFee> billFees = getBillFeeFacade().findByJpql(sql);

            calculateBillfeePayments(billFees, p);
        }
        opdBill.setBalance(opdBill.getBalance() - opdPaymentCredit);
        opdBill.setCashPaid(calBillPaidValue(opdBill));
        opdBill.setNetTotal(calBillPaidValue(opdBill));
        getBillFacade().edit(opdBill);

        JsfUtil.addSuccessMessage("Paid");
        opdBill = temp;
        printPreview = true;

    }

    public void saveBillPharmacyCredit() {

        BilledBill temp = new BilledBill();

        if (opdPaymentCredit == 0) {
            JsfUtil.addErrorMessage("Please Select Correct Paid Amount");
            return;
        }
        if (opdPaymentCredit > (opdBill.getNetTotal() - opdBill.getPaidAmount())) {
            JsfUtil.addErrorMessage("Please Enter Correct Paid Amount");
            return;
        }

        temp.setReferenceBill(opdBill);
        temp.setTotal(opdPaymentCredit);
        temp.setPaidAmount(opdPaymentCredit);
        temp.setNetTotal(opdPaymentCredit);
        ////// // System.out.println("opdBill.getPaidAmount() = " + opdBill.getPaidAmount());
        ////// // System.out.println("opdPaymentCredit = " + opdPaymentCredit);
        opdBill.setPaidAmount(opdPaymentCredit + opdBill.getPaidAmount());
        ////// // System.out.println("opdBill.getPaidAmount() = " + opdBill.getPaidAmount());
        getBillFacade().edit(opdBill);

        temp.setDeptId(getBillNumberGenerator().departmentBillNumberGenerator(getSessionController().getDepartment(), getSessionController().getDepartment(), BillType.CashRecieveBill, BillClassType.BilledBill));
        temp.setInsId(getBillNumberGenerator().institutionBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), BillType.CashRecieveBill, BillClassType.BilledBill, BillNumberSuffix.NONE));
        temp.setBillType(BillType.CashRecieveBill);

        temp.setDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setToDepartment(getSessionController().getLoggedUser().getDepartment());

        temp.setComments(comment);

//        getBillBean().setPaymentMethodData(temp, getPaymentMethod(), getPaymentMethodData());
        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setPaymentMethod(getPaymentMethod());
        temp.setCreatedAt(new Date());
        temp.setCreater(getSessionController().getLoggedUser());
        getFacade().create(temp);

        JsfUtil.addSuccessMessage("Paid");
        opdBill = temp;
        printPreview = true;

    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

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
            return;
        }

        lstBillItems = null;
        getLstBillItems();
        bf.setTmpChangedValue(bf.getFeeGrossValue());
        calTotals();
    }

    public void fillAvailableAgentReferanceNumbers(Institution ins) {
        bookSummeryRows = new ArrayList<>();
        String sql;
        referralIds = new ArrayList<>();
        HashMap m = new HashMap();
        sql = "select a from AgentReferenceBook a "
                + " where a.retired=false "
                + " and a.institution=:ins"
                + " and a.deactivate=false "
                + " and a.fullyUtilized=false "
                + " order by a.id ";
        m.put("ins", ins);
        agentReferenceBooks = agentReferenceBookFacade.findByJpql(sql, m, 2);
        // Fetch all used reference numbers for this institution in one query
        Set<String> usedReferenceNumbers = fetchUsedReferenceNumbers(ins);

        if (agentReferenceBooks.isEmpty()) {
            ins.setAgentReferenceBooks(null);
            return;
        }
        ins.setAgentReferenceBooks(agentReferenceBooks);

        for (AgentReferenceBook a : agentReferenceBooks) {
            CollectingCenterBookSummeryRow row = new CollectingCenterBookSummeryRow();
            row.setBookName(a.getStrbookNumber());
            row.setBookNumber(0);

            boolean leavesRemain = false;
            int start = (int) a.getStartingReferenceNumber();
            int end = (int) a.getEndingReferenceNumber();

            for (int i = start; i <= end; i++) {
                String bookNo = a.getStrbookNumber();
                String leafNumber = String.format("%02d", i);
                String refNo = bookNo + leafNumber;

                if (!usedReferenceNumbers.contains(refNo)) {
                    leavesRemain = true;
                    referralIds.add(refNo);
                    row.setBookNumber(row.getBookNumber() + 1);
                }
            }
            if (!leavesRemain) {
                a.setFullyUtilized(true);
                agentReferenceBookFacade.edit(a);
            } else {
                bookSummeryRows.add(row);
            }
        }
    }

    public Set<String> fetchUsedReferenceNumbers(Institution ins) {
        String sql;
        Set<String> usedRefNumbers = new HashSet<>();
        Map m = new HashMap();
        // Adjust this query to fetch only the reference numbers
        sql = "select b.referenceNumber from Bill b where b.retired=false and b.institution=:ins";
        m.put("ins", ins);
        List<String> resultList = billFacade.findString(sql, m);
        usedRefNumbers.addAll(resultList);
        return usedRefNumbers;
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

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public Double getGrosTotal() {
        return grosTotal;
    }

    public void setGrosTotal(Double grosTotal) {
        this.grosTotal = grosTotal;
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

    private boolean checkPatientAgeSex() {
        if (getPatient().getPerson().getName() == null || getPatient().getPerson().getName().trim().equals("") || getPatient().getPerson().getSex() == null || getPatient().getPerson().getDob() == null) {
            JsfUtil.addErrorMessage("Can not bill without Patient Name, Age or Sex.");
            return true;
        }
        return false;
    }

    private void savePatient() {
        if (getPatient().getId() == null) {
            getPatient().setPhn(applicationController.createNewPersonalHealthNumber(getSessionController().getInstitution()));
            getPatient().setCreatedInstitution(getSessionController().getInstitution());
            getPatient().setCreater(getSessionController().getLoggedUser());
            getPatient().setCreatedAt(new Date());
            getPatient().getPerson().setCreater(getSessionController().getLoggedUser());
            getPatient().getPerson().setCreatedAt(new Date());
            try {
                getPersonFacade().create(getPatient().getPerson());
            } catch (Exception e) {
                getPersonFacade().edit(getPatient().getPerson());
            }
            try {
                getPatientFacade().create(getPatient());
            } catch (Exception e) {
                getPatientFacade().edit(getPatient());
            }
        } else {
            getPatientFacade().edit(getPatient());
        }

    }

//    private void savePatient() {
//        switch (getPatientTabId()) {
//            case "tabNewPt":
//                getNewPatient().setCreater(getSessionController().getLoggedUser());
//                getNewPatient().setCreatedAt(new Date());
//                getNewPatient().getPerson().setCreater(getSessionController().getLoggedUser());
//                getNewPatient().getPerson().setCreatedAt(new Date());
//                getPersonFacade().create(getNewPatient().getPerson());
//                getPatientFacade().create(getNewPatient());
//                tmpPatient = getNewPatient();
//                break;
//            case "tabSearchPt":
//                tmpPatient = getSearchedPatient();
//                break;
//        }
//    }
    public boolean putToBills() {
        bills = new ArrayList<>();
        Set<Department> billDepts = new HashSet<>();
        for (BillEntry e : lstBillEntries) {
            billDepts.add(e.getBillItem().getItem().getDepartment());
        }
        Bill temBill = new Bill();
        for (Department d : billDepts) {
            Bill myBill = new BilledBill();
            myBill = saveBill(d, myBill);

            if (myBill == null) {
                return false;
            }

            List<BillEntry> tmp = new ArrayList<>();

            for (BillEntry e : lstBillEntries) {
                if (Objects.equals(e.getBillItem().getItem().getDepartment().getId(), d.getId())) {
//                    BillItem bi = getBillBean().saveBillItem(myBill, e, getSessionController().getLoggedUser());
                    //for create Bill fee Payments
                    BillItem bi = getBillBean().saveBillItem(myBill, e, getSessionController().getLoggedUser());
                    //getBillBean().calculateBillItem(myBill, e);
                    myBill.getBillItems().add(bi);
                    tmp.add(e);
                }
            }

            myBill.setReferenceNumber(referralId);

            getBillFacade().edit(myBill);

            getBillBean().calculateBillItems(myBill, tmp);
            createPaymentsForBills(myBill, tmp);

            double feeTotalExceptCcfs = 0.0;
            for (BillEntry be : tmp) {
                for (BillFee bf : be.getLstBillFees()) {
                    if (bf.getFee().getFeeType() != FeeType.CollectingCentre) {
                        feeTotalExceptCcfs += bf.getFeeValue();
                    }
                }
            }
            updateBallance(collectingCentre, 0 - Math.abs(feeTotalExceptCcfs), HistoryType.CollectingCentreBalanceUpdateBill, myBill, referralId);
            AgentHistory ah = billSearch.fetchCCHistory(myBill);
            billSearch.createCollectingCenterfees(myBill);
            myBill.setTransCurrentCCBalance(ah.getBeforeBallance() + ah.getTransactionValue());

            bills.add(myBill);
            temBill = myBill;
        }

//        double feeTotalExceptCcfs = 0.0;
//        for (BillFee bf : lstBillFees) {
//            if (bf.getFee().getFeeType() != FeeType.CollectingCentre) {
//                feeTotalExceptCcfs += bf.getFeeValue();
//            }
//        }
//        updateBallance(collectingCentre, 0 - Math.abs(feeTotalExceptCcfs), HistoryType.CollectingCentreBalanceUpdateBill, temBill, referralId);
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

    public void settleBill() {
        Date startTime = new Date();
        if (errorCheck()) {
            return;
        }
        savePatient();
        if (getBillBean().calculateNumberOfBillsPerOrder(getLstBillEntries()) == 1) {
            BilledBill temp = new BilledBill();
            Bill b = saveBill(lstBillEntries.get(0).getBillItem().getItem().getDepartment(), temp);
            if (b == null) {
                return;
            }
            List<BillItem> list = new ArrayList<>();
            for (BillEntry billEntry : getLstBillEntries()) {
                list.add(getBillBean().saveBillItem(b, billEntry, getSessionController().getLoggedUser()));
            }
            b.setBillItems(list);
            b.setBillTotal(b.getNetTotal());
            getBillFacade().edit(b);
            getBillBean().calculateBillItems(b, getLstBillEntries());
            b.setBalance(0.0);
//            b.setNetTotal(b.getTransSaleBillTotalMinusDiscount());
            b.setReferenceNumber(referralId);

            createPaymentsForBills(b, getLstBillEntries());

            getBillFacade().edit(b);
            getBills().add(b);

            double feeTotalExceptCcfs = 0.0;
            for (BillFee bf : lstBillFees) {
                if (bf.getFee().getFeeType() != FeeType.CollectingCentre) {
                    feeTotalExceptCcfs += (bf.getFeeValue() + bf.getFeeVat());
                }
            }

            updateBallance(collectingCentre, 0 - Math.abs(feeTotalExceptCcfs), HistoryType.CollectingCentreBilling, b, b.getReferenceNumber());
            AgentHistory ah = billSearch.fetchCCHistory(b);
            billSearch.createCollectingCenterfees(b);
            b.setTransCurrentCCBalance(ah.getBeforeBallance() + ah.getTransactionValue());
        } else {
            boolean result = putToBills();
            if (result == false) {
                return;
            }
        }

        saveBatchBill();
        saveBillItemSessions();

        JsfUtil.addSuccessMessage("Bill Saved");
        setPrintigBill();
        checkBillValues();
        printPreview = true;

        
    }

    public void updateBallance(Institution ins, double transactionValue, HistoryType historyType, Bill bill, String refNo) {
        AgentHistory agentHistory = new AgentHistory();
        agentHistory.setCreatedAt(new Date());
        agentHistory.setCreater(getSessionController().getLoggedUser());
        agentHistory.setBill(bill);
        agentHistory.setBeforeBallance(ins.getBallance());
        agentHistory.setTransactionValue(transactionValue);
        agentHistory.setReferenceNumber(refNo);
        agentHistory.setHistoryType(historyType);
        agentHistoryFacade.create(agentHistory);
        ins.setBallance(ins.getBallance() + transactionValue);
        getInstitutionFacade().edit(ins);
    }

    public boolean checkBillValues(Bill b) {

        Double[] billItemValues = billBean.fetchBillItemValues(b);
        double billItemTotal = billItemValues[0];
        double billItemDiscount = billItemValues[1];
        double billItemNetTotal = billItemValues[2];

        //// // System.out.println("b.getTotal() = " + b.getTotal());
        //// // System.out.println("billItemTotal = " + billItemTotal);
        //// // System.out.println("b.getDiscount() = " + b.getDiscount());
        //// // System.out.println("billItemDiscount = " + billItemDiscount);
        //// // System.out.println("b.getNetTotal() = " + b.getNetTotal());
        //// // System.out.println("billItemNetTotal = " + billItemNetTotal);
        if (billItemTotal != b.getTotal() || billItemDiscount != b.getDiscount() || billItemNetTotal != b.getNetTotal()) {
            return true;
        }

        Double[] billFeeValues = billBean.fetchBillFeeValues(b);
        double billFeeTotal = billFeeValues[0];
        double billFeeDiscount = billFeeValues[1];
        double billFeeNetTotal = billFeeValues[2];

        //// // System.out.println("b.getTotal() = " + b.getTotal());
        //// // System.out.println("billFeeTotal = " + billFeeTotal);
        if (billFeeTotal != b.getTotal() || billFeeDiscount != b.getDiscount() || billFeeNetTotal != b.getNetTotal()) {
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
        tmp.setBillType(BillType.CollectingCentreBatchBill);
        tmp.setPaymentScheme(paymentScheme);
        tmp.setPaymentMethod(getPaymentMethod());
        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());
        getBillFacade().create(tmp);

        double dbl = 0;
        double reminingCashPaid = cashPaid;
        for (Bill b : bills) {
            b.setBackwardReferenceBill(tmp);
            dbl += b.getNetTotal();

            if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
                b.setCashPaid(reminingCashPaid);

                if (reminingCashPaid > b.getTransSaleBillTotalMinusDiscount()) {
                    b.setBalance(0.0);
                    b.setNetTotal(b.getTransSaleBillTotalMinusDiscount());
                } else {
                    b.setBalance(b.getTotal() - b.getCashPaid());
                    b.setNetTotal(reminingCashPaid);
                }
            }
            reminingCashPaid = reminingCashPaid - b.getNetTotal();

            getBillFacade().edit(b);

            tmp.getForwardReferenceBills().add(b);
        }

        tmp.setNetTotal(dbl);
        getBillFacade().edit(tmp);

        WebUser wb = getCashTransactionBean().saveBillCashInTransaction(tmp, getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
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

    public void dateChangeListen() {
        getPatient().getPerson().setDob(getCommonFunctions().guessDob(yearMonthDay));

    }

    private Bill saveBill(Department bt, Bill temp) {
        temp.setBillType(BillType.CollectingCentreBill);

        temp.setInstitution(collectingCentre);
        temp.setDepartment(departmentController.getDefaultDepatrment(collectingCentre));

        temp.setToDepartment(bt);
        temp.setToInstitution(bt.getInstitution());

        temp.setFromDepartment(temp.getDepartment());
        temp.setFromInstitution(temp.getInstitution());

        temp.setReferredBy(referredBy);
        temp.setReferenceNumber(referralId);
        temp.setReferredByInstitution(referredByInstitution);
        temp.setComments(comment);

//        getBillBean().setPaymentMethodData(temp, paymentMethod, getPaymentMethodData());
        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setPatient(getPatient());

        temp.setPaymentScheme(getPaymentScheme());
        temp.setPaymentMethod(getPaymentMethod());
        temp.setCreatedAt(new Date());
        temp.setCreater(getSessionController().getLoggedUser());

        //SETTING INS ID
        recurseCount = 0;
        String insId = generateBillNumberInsId(temp);

        if (insId.equals("")) {
            return null;
        }
        temp.setInsId(insId);
        if (temp.getId() == null) {
            getFacade().create(temp);
        } else {
            getFacade().edit(temp);
        }

        //Department ID (DEPT ID)
        String deptId = getBillNumberGenerator().departmentBillNumberGenerator(temp.getDepartment(), temp.getToDepartment(), temp.getBillType(), BillClassType.BilledBill);
        temp.setDeptId(deptId);

        if (temp.getId() == null) {
            getFacade().create(temp);
        } else {
            getFacade().edit(temp);
        }
        return temp;

    }

    int recurseCount = 0;

    private String generateBillNumberInsId(Bill bill) {
        String insId = getBillNumberGenerator().institutionBillNumberGenerator(bill.getInstitution(), bill.getToDepartment(), bill.getBillType(), BillClassType.BilledBill, BillNumberSuffix.NONE);
        return insId;
    }

    private boolean collectingCenterReferranceNumberAlreadyUsed(Institution ins) {
        String jpql;
        HashMap m = new HashMap();
        jpql = "Select b from Bill b"
                + " where b.retired = false "
                + " and b.billType=:bt "
                + " and b.institution=:ins "
                + " and (b.referenceNumber) =:rid ";
        m.put("rid", referralId.toUpperCase());
        m.put("bt", BillType.CollectingCentreBill);
        m.put("ins", ins);
        List<Bill> tempBills = getFacade().findByJpql(jpql, m);
//        Bill b = getFacade().findFirstByJpql(jpql, m);
//        //// // System.out.println(" Error find Number CheckTime 3 = " + new Date());
        if (tempBills == null || tempBills.isEmpty()) {
            return false;
        }
        return true;
    }

    @Inject
    AgentReferenceBookController agentReferenceBookController;

    private boolean errorCheck() {
        if (getPatient().getPerson().getName() == null
                || getPatient().getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please select a patient before billing");
            return true;
        }
        if (checkPatientAgeSex()) {
            return true;
        }
        if (collectingCentre == null) {
            JsfUtil.addErrorMessage("Please select a collecting centre");
            return true;
        }
        if (referralId == null || referralId.trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a referrance number");
            return true;
        }
//        else if (collectingCenterReferranceNumberAlreadyUsed(collectingCentre)) {
//            JsfUtil.addErrorMessage("Referral number alredy entered");
//            return true;
//        }

        if (getLstBillEntries().isEmpty()) {
            JsfUtil.addErrorMessage("Please Add tests before billing");
            return true;
        }

        double feeTotalExceptCcfs = 0.0;
        for (BillFee bf : lstBillFees) {
            if (bf.getFee().getFeeType() != FeeType.CollectingCentre) {
                feeTotalExceptCcfs += bf.getFeeValue();
            }
        }

        ///not wanted 
//        if ((collectingCentre.getBallance() - Math.abs(feeTotalExceptCcfs)) < 0 - collectingCentre.getStandardCreditLimit()) {
//            JsfUtil.addErrorMessage("This bill excees the Collecting Centre Limit");
//            return true;
//        }
        if (collectingCentre.getBallance() - feeTotalExceptCcfs < 0 - collectingCentre.getAllowedCredit()) {
            JsfUtil.addErrorMessage("Collecting Centre Balance is Not Enough");
            return true;
        }

//        if (agentReferenceBookController.numberHasBeenIssuedToTheAgent(getReferralId())) {
//            JsfUtil.addErrorMessage("Invaild Reference Number.");
//            return true;
//        }
        if (agentReferenceBookController.agentReferenceNumberIsAlredyUsed(getReferralId(), collectingCentre, BillType.CollectingCentreBill, PaymentMethod.Agent)) {
            JsfUtil.addErrorMessage("This Reference Number is alredy Used.");
            setReferralId("");
            return true;
        }

        if (!agentReferenceBookController.numberHasBeenIssuedToTheAgent(collectingCentre, getReferralId())) {
            JsfUtil.addErrorMessage("This Reference Number is Blocked Or This channel Book is Not Issued.");
            return true;
        }

        return false;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

    public List<BillSession> getBillSessions() {
        return billSessions;
    }

    public void fillBillSessions(SelectEvent event) {
        ////// // System.out.println("event = " + event);
        ////// // System.out.println("this = filling bill sessions");
        if (lastBillItem != null && lastBillItem.getItem() != null) {
            billSessions = getServiceSessionBean().getBillSessions(lastBillItem.getItem(), getSessionDate());
            ////// // System.out.println("billSessions = " + billSessions);
        } else ////// // System.out.println("billSessions = " + billSessions);
        {
            if (billSessions == null || !billSessions.isEmpty()) {
                ////// // System.out.println("new array");
                billSessions = new ArrayList<>();
            }
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

    public List<ItemLight> fillOpdItems() {
        UserPreference up = sessionController.getDepartmentPreference();
        switch (up.getCcItemListingStrategy()) {
            case ALL_ITEMS:
                return itemApplicationController.fillAllItems();
            case ITEMS_MAPPED_TO_SELECTED_DEPARTMENT:
            case ITEMS_MAPPED_TO_SELECTED_INSTITUTION:
                return itemMappingController.fillItemLightByInstitution(collectingCentre);
            case ITEMS_MAPPED_TO_LOGGED_DEPARTMENT:
                return itemMappingController.fillItemLightByDepartment(sessionController.getDepartment());
            case ITEMS_MAPPED_TO_LOGGED_INSTITUTION:
                return itemMappingController.fillItemLightByInstitution(sessionController.getInstitution());
            case ITEMS_OF_LOGGED_DEPARTMENT:
                return itemController.fillItemsByDepartment(sessionController.getDepartment());
            case ITEMS_OF_LOGGED_INSTITUTION:
                return itemController.getInstitutionItems();
            case ITEMS_OF_SELECTED_DEPARTMENT:
                return itemController.fillItemsByDepartment(departmentController.getDefaultDepatrment(collectingCentre));
            case ITEMS_OF_SELECTED_INSTITUTIONS:
                return itemController.fillItemsByInstitution(collectingCentre);
            default:
                return itemController.getAllItems();
        }
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
    
    public List<BillFee> getSelectedCollectionCenterItemFeeList(Institution collectingCentre, Item item){

        Map m = new HashMap();
        String jpql = "select f from ItemFee f "
                + " where f.institution=:ins"
                + " and f.institution.institutionType=:insType"
                + " and f.item=:item"
                + " and f.retired=:ret";
        
        m.put("insType", InstitutionType.CollectingCentre);
        m.put("ins", collectingCentre);
        m.put("item", item);
        m.put("ret", false);
        
        List<BillFee> billFees = getBillFeeFacade().findByJpql(jpql, m);
        
        if(billFees == null){
            billFees = new ArrayList<BillFee>();
        }
        
        System.out.println("getSelectedCollectionCenterItemFeeList - billFees = " + billFees);
        
        return billFees;
    }

    public void addToBill() {
        if (collectingCentre == null) {
            JsfUtil.addErrorMessage("Please Select Collecting Center");
            return;
        }
        if (getCurrentBillItem() == null) {
            JsfUtil.addErrorMessage("Nothing to add");
            return;
        }
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please select an Item");
            return;
        }
        if (getCurrentBillItem().getItem().getTotal() == 0.0) {
            JsfUtil.addErrorMessage("Please corect item fee");
            return;
        }

        if (getCurrentBillItem().getItem().getInstitution() == null) {
            getCurrentBillItem().getItem().setInstitution(collectingCentre);
            itemController.saveSelected(getCurrentBillItem().getItem());
        }

        if (getCurrentBillItem().getItem().getDepartment() == null) {
            Department dep = departmentController.getDefaultDepatrment(collectingCentre);
            getCurrentBillItem().getItem().setDepartment(dep);
            itemController.saveSelected(getCurrentBillItem().getItem());
            return;
        }

        if (getCurrentBillItem().getItem().getCategory() == null) {
            Category c = categoryController.findAndCreateCategoryByName("Other");
            getCurrentBillItem().getItem().setCategory(c);
            itemController.saveSelected(getCurrentBillItem().getItem());
            return;
        }
        
        BillItem bi = new BillItem();
        bi.copy(getCurrentBillItem());
        bi.setSessionDate(sessionDate);
        lastBillItem = bi;
        if(bi.getQty()==null || bi.getQty()<1){
            bi.setQty(1.0);
        }
        
        BillEntry addingEntry = new BillEntry();
        addingEntry.setBillItem(bi);
        addingEntry.setLstBillComponents(getBillBean().billComponentsFromBillItem(bi));
        System.out.println("collectingCentre = " + collectingCentre);
        System.out.println("getSelectedCollectionCenterItemFeeList(collectingCentre,getCurrentBillItem().getItem()) = " + getSelectedCollectionCenterItemFeeList(collectingCentre,getCurrentBillItem().getItem()).size());
        System.out.println("getCurrentBillItem().getItem().getName() = " + getCurrentBillItem().getItem().getName());
        if(getSelectedCollectionCenterItemFeeList(collectingCentre,getCurrentBillItem().getItem()).isEmpty()){
            addingEntry.setLstBillFees(getBillBean().billFeefromBillItem(bi));
            
            System.out.println("Collecting Center Item Fee Null");
        }else{
            addingEntry.setLstBillFees(getSelectedCollectionCenterItemFeeList(collectingCentre,getCurrentBillItem().getItem()));
            System.out.println("Collecting Center Item Fee");
        }
        
        addingEntry.setLstBillSessions(getBillBean().billSessionsfromBillItem(bi));
        getLstBillEntries().add(addingEntry);
        System.out.println("addingEntry = " + addingEntry);
        bi.setRate(getBillBean().billItemRate(addingEntry));
        bi.setQty(1.0);
       
        bi.setNetValue(bi.getRate() * bi.getQty()); // Price == Rate as Qty is 1 here
        calTotals();

        if (bi.getNetValue() == 0.0) {
            JsfUtil.addErrorMessage("Please enter the Fees");
            return;
        }
        
        clearBillItemValues();
        JsfUtil.addSuccessMessage("Item Added");
    }

    public void clearBillItemValues() {
        currentBillItem = null;
        recreateBillItems();
    }

    private void clearBillValues() {
        setPatient(null);
        setReferredBy(null);
        setReferredByInstitution(null);
        setReferralId(null);
        setSessionDate(null);
        setCreditCompany(null);
        setYearMonthDay(null);
        setBills(null);
        setPaymentScheme(null);
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

        patientTabId = "tabNewPt";

        fromOpdEncounter = false;
        opdEncounterComments = "";
        patientSearchTab = 0;
    }

    private void recreateBillItems() {
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;
    }

    @Inject
    PriceMatrixController priceMatrixController;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    @Inject
    MembershipSchemeController membershipSchemeController;

    public void calTotals() {

        double billDiscount = 0.0;
        double billGross = 0.0;
        double billNet = 0.0;
        double billVat = 0.0;

        for (BillEntry be : getLstBillEntries()) {
            //////// // System.out.println("bill item entry");
            double entryGross = 0.0;
            double entryDis = 0.0;
            double entryNet = 0.0;
            double entryVat = 0.0;
            BillItem bi = be.getBillItem();

            for (BillFee bf : be.getLstBillFees()) {
                Department dept = null;
                entryGross += bf.getFeeGrossValue();
                entryNet += bf.getFeeValue();
                entryDis += bf.getFeeDiscount();
                entryVat += bf.getFeeVat();
                //////// // System.out.println("fee net is " + bf.getFeeValue());

            }

            bi.setDiscount(entryDis);
            bi.setGrossValue(entryGross);
            bi.setNetValue(entryNet);
            bi.setVat(entryVat);
            bi.setVatPlusNetValue(entryVat + entryNet);
            //////// // System.out.println("item is " + bi.getItem().getName());
            //////// // System.out.println("item gross is " + bi.getGrossValue());
            //////// // System.out.println("item net is " + bi.getNetValue());
            //////// // System.out.println("item dis is " + bi.getDiscount());
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
        setVatPlusNetTotal(billVat + billNet);
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

    public void prepareNewBill() {
        clearBillItemValues();
        clearBillValues();
        setPrintPreview(true);
        printPreview = false;
        paymentMethodData = null;
        paymentScheme = null;
        paymentMethod = PaymentMethod.Agent;
        collectingCentre = null;
        opdItems = null;
    }

    public void prepareNewBillKeepingCollectingCenter() {
        clearBillItemValues();
        clearBillValues();
        setPrintPreview(true);
        printPreview = false;
        paymentMethodData = null;
        paymentScheme = null;
        paymentMethod = PaymentMethod.Agent;
    }

    public List<Item> completeOpdItems(String query) {
        UserPreference up = sessionController.getDepartmentPreference();
        switch (up.getCcItemListingStrategy()) {
            case ALL_ITEMS:
                return itemController.completeServicesPlusInvestigationsAll(query);
            case ITEMS_MAPPED_TO_LOGGED_DEPARTMENT:
                return itemMappingController.completeItemByDepartment(query, sessionController.getDepartment());
            case ITEMS_MAPPED_TO_LOGGED_INSTITUTION:
                return itemMappingController.completeItemByInstitution(query, sessionController.getInstitution());
            case ITEMS_MAPPED_TO_SELECTED_DEPARTMENT:
                return itemMappingController.completeItemByDepartment(query, collectingCentre);
            case ITEMS_MAPPED_TO_SELECTED_INSTITUTION:
                return itemMappingController.completeItemByInstitution(query, collectingCentre);
            case ITEMS_OF_LOGGED_DEPARTMENT:
                return itemController.completeItemsByDepartment(query, sessionController.getDepartment());
            case ITEMS_OF_LOGGED_INSTITUTION:
                return itemController.completeItemsByInstitution(query, sessionController.getInstitution());
            case ITEMS_OF_SELECTED_DEPARTMENT:
                return itemController.completeItemsByDepartment(query, collectingCentre);
            case ITEMS_OF_SELECTED_INSTITUTIONS:
                return itemController.completeItemsByInstitution(query, collectingCentre);
            default:
                throw new AssertionError();
        }
    }

    public void makeNull() {
        clearBillItemValues();
        clearBillValues();
        paymentMethod = PaymentMethod.Agent;
        printPreview = false;

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

    public void onTabChange(TabChangeEvent event) {
        setPatientTabId(event.getTab().getId());
        if (!getPatientTabId().equals("tabSearchPt")) {
            if (fromOpdEncounter == false) {
                setPatient(null);
            }
        }
        calTotals();
    }

    public void createPaymentsForBills(Bill b, List<BillEntry> billEntrys) {
        Payment p = createPayment(b, b.getPaymentMethod());
        createBillFeePaymentsByPaymentsAndBillEntry(p, billEntrys);
    }

    public String navigateToCollectingCenterBillingromMenu() {
        prepareNewBill();
        setPatient(getPatient());
        return "/collecting_centre/bill?faces-redirect=true";
    }

    public String navigateToCollectingCenterBillingromCollectingCenterBilling() {
        prepareNewBillKeepingCollectingCenter();
        setPatient(getPatient());
        return "/collecting_centre/bill?faces-redirect=true";
    }
    
    public String navigateToCollectingCenterBillFromBillSearch(Long id){
        bills = null;
        if(id == null){
            return "";
        }
        bill = billController.findBillbyID(id);
        
        System.out.println("bill = " + bill);
        
        if(bill == null){
            return "";
        }
        
        bill.setBillItems(getBillBean().fillBillItems(bill));
        
        bill.setBillFees(getBillBean().getBillFee(bill));
        
        System.out.println("bill.getBillItems = " + bill.getBillItems());
        System.out.println("bill.getBillFees = " + bill.getBillFees());
        bills.add(bill);
        
        return "/collecting_centre/bill_print?faces-redirect=true";
    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
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
            getPaymentFacade().create(p);
        }

    }

    double reminingCashPaid = 0.0;

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
        getBillFeePaymentFacade().create(bfp);
    }

    public double calBillPaidValue(Bill b) {
        String sql;

        sql = "select sum(bfp.amount) from BillFeePayment bfp where "
                + " bfp.retired=false "
                + " and bfp.billFee.bill.id=" + b.getId();

        double d = getBillFeePaymentFacade().findDoubleByJpql(sql);

        return d;
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

    public CollectingCentreBillController() {
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
//        if (paymentScheme == null) {
//            paymentScheme = new PaymentScheme();
//        }
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
        //    getEnumController().setPaymentScheme(paymentScheme);

    }

    public void changeListener() {
        calTotals();
    }

    public String getPatientTabId() {
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
    }

//    public Patient getNewPatient() {
//        if (newPatient == null) {
//            newPatient = new Patient();
//            Person p = new Person();
//
//            newPatient.setPerson(p);
//        }
//        return newPatient;
//    }
//
//    public void setNewPatient(Patient newPatient) {
//        this.newPatient = newPatient;
//    }
    @Override
    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
            patientDetailsEditable = true;
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

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getVat() {
        return vat;
    }

    public void setVat(double vat) {
        this.vat = vat;
    }

    public double getVatPlusNetTotal() {
        return vatPlusNetTotal;
    }

    public void setVatPlusNetTotal(double vatPlusNetTotal) {
        this.vatPlusNetTotal = vatPlusNetTotal;
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
        cashBalance = cashPaid - netTotal;
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
    BillItem lastBillItem;

    public BillItem getLastBillItem() {
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
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.Agent;
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

    public List<ItemLight> getOpdItems() {
        if (opdItems == null) {
            opdItems = fillOpdItems();
        }
        return opdItems;
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

    public PaymentFacade getPaymentFacade() {
        return PaymentFacade;
    }

    public void setPaymentFacade(PaymentFacade PaymentFacade) {
        this.PaymentFacade = PaymentFacade;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public double getReminingCashPaid() {
        return reminingCashPaid;
    }

    public void setReminingCashPaid(double reminingCashPaid) {
        this.reminingCashPaid = reminingCashPaid;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public List<String> getReferralIds() {
        return referralIds;
    }

    public void setReferralIds(List<String> referralIds) {
        this.referralIds = referralIds;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    public List<CollectingCenterBookSummeryRow> getBookSummeryRows() {
        return bookSummeryRows;
    }

    public void setBookSummeryRows(List<CollectingCenterBookSummeryRow> bookSummeryRows) {
        this.bookSummeryRows = bookSummeryRows;
    }

    public class CollectingCenterBookSummeryRow {

        private String bookName;
        private Integer bookNumber;

        public String getBookName() {
            return bookName;
        }

        public void setBookName(String bookName) {
            this.bookName = bookName;
        }

        public Integer getBookNumber() {
            return bookNumber;
        }

        public void setBookNumber(Integer bookNumber) {
            this.bookNumber = bookNumber;
        }

    }

}
