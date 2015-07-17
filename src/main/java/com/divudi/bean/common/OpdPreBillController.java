/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.bean.memberShip.MembershipSchemeController;
import com.divudi.bean.memberShip.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.ejb.BillEjb;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.CommonFunctions;
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
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.PreBill;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.RefundBill;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.memberShip.MembershipScheme;
import com.divudi.facade.BatchBillFacade;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillFeePaymentFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
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
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class OpdPreBillController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    PaymentSchemeController paymentSchemeController;
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
    BillFeePaymentFacade billFeePaymentFacade;
    @Inject
    private EnumController enumController;
    @EJB
    BillEjb billEjb;
    private boolean printPreview;
    private String patientTabId = "tabNewPt";
    //Interface Data
    private PaymentScheme paymentScheme;
    private PaymentMethod paymentMethod;
    private Patient newPatient;
    private Patient searchedPatient;
    private Doctor referredBy;
    private Institution referredByInstitution;
    String referralId;
    private Institution creditCompany;
    private Staff staff;
    Staff toStaff;
    private double total;
    private double discount;
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

    //Print Last Bill
    Bill billPrint;
    List<Bill> billsPrint;
    private List<BillComponent> lstBillComponentsPrint;
    private List<BillFee> lstBillFeesPrint;
    private List<BillItem> lstBillItemsPrint;
    private List<BillEntry> lstBillEntriesPrint;
    
    List<BillFeePayment>billFeePayments;

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
    @EJB
    CommonFunctions commonFunctions;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    //Temprory Variable
    private Patient tmpPatient;
    List<Bill> bills;
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
    
    public void clear() {
        opdBill = new BilledBill();
        printPreview = false;
        opdPaymentCredit = 0.0;
        comment=null;
        searchController.createTableByKeywordToPayBills();
    }
    
    public void clearPharmacy() {
        opdBill = new BilledBill();
        printPreview = false;
        opdPaymentCredit = 0.0;
        comment=null;
        searchController.createTablePharmacyCreditToPayBills();
    }

    public void saveBillOPDCredit() {

        BilledBill temp = new BilledBill();

        if (opdPaymentCredit == 0) {
            UtilityController.addErrorMessage("Please Select Correct Paid Amount");
            return;
        }
        if (opdPaymentCredit > opdBill.getBalance()) {
            UtilityController.addErrorMessage("Please Enter Correct Paid Amount");
            return;
        }

        temp.setReferenceBill(opdBill);
        temp.setTotal(opdPaymentCredit);
        temp.setPaidAmount(opdPaymentCredit);
        temp.setNetTotal(opdPaymentCredit);

        opdBill.setBalance(opdBill.getBalance() - opdPaymentCredit);
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

        getBillBean().setPaymentMethodData(temp, paymentMethod, getPaymentMethodData());

        temp.setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setPaymentMethod(paymentMethod);
        temp.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setCreater(getSessionController().getLoggedUser());
        getFacade().create(temp);

        JsfUtil.addSuccessMessage("Paid");
        opdBill = temp;
        printPreview = true;

    }
    
    public void saveBillPharmacyCredit() {

        BilledBill temp = new BilledBill();

        if (opdPaymentCredit == 0) {
            UtilityController.addErrorMessage("Please Select Correct Paid Amount");
            return;
        }
        if (opdPaymentCredit > (opdBill.getNetTotal()-opdBill.getPaidAmount())) {
            UtilityController.addErrorMessage("Please Enter Correct Paid Amount");
            return;
        }

        temp.setReferenceBill(opdBill);
        temp.setTotal(opdPaymentCredit);
        temp.setPaidAmount(opdPaymentCredit);
        temp.setNetTotal(opdPaymentCredit);
        //System.out.println("opdBill.getPaidAmount() = " + opdBill.getPaidAmount());
        //System.out.println("opdPaymentCredit = " + opdPaymentCredit);
        opdBill.setPaidAmount(opdPaymentCredit+opdBill.getPaidAmount());
        //System.out.println("opdBill.getPaidAmount() = " + opdBill.getPaidAmount());
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

        getBillBean().setPaymentMethodData(temp, paymentMethod, getPaymentMethodData());

        temp.setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setPaymentMethod(paymentMethod);
        temp.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setCreater(getSessionController().getLoggedUser());
        getFacade().create(temp);

        JsfUtil.addSuccessMessage("Paid");
        opdBill = temp;
        printPreview = true;

    }
    
    public void createBillFeePayments(){
        billFeePayments=new ArrayList<>();
        String sql;
        Map m=new HashMap();
        
        sql=" select bfp from BillFeePayment bfp where "
                + " bfp.retired=false "
                + " and bfp.createdAt between :fd and :td "
                + " order by bfp.billFee.bill.deptId ";
        
        m.put("fd", fromDate);
        m.put("td", toDate);
        
        billFeePayments=getBillFeePaymentFacade().findBySQL(sql, m);
        
    }

    public BillNumberGenerator getBillNumberGenerator() {
        return billNumberGenerator;
    }

    public StaffBean getStaffBean() {
        return staffBean;
    }

    public void searchPatientListener() {
        System.err.println("1");
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
        String sql = "Select b From Bill b where b.retired=false and upper(b." + property + ") like '%" + value.toUpperCase() + " %'";
        Bill b = getBillFacade().findFirstBySQL(sql);
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

    public String getStrTenderedValue() {
        return strTenderedValue;
    }

    public void setStrTenderedValue(String strTenderedValue) {
        this.strTenderedValue = strTenderedValue;
        try {
            cashPaid = Double.parseDouble(strTenderedValue);
        } catch (NumberFormatException e) {
            ////System.out.println("Error in converting tendered value. \n " + e.getMessage());
        }
    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

  
   

    public BillEjb getBillEjb() {
        return billEjb;
    }

    public void setBillEjb(BillEjb billEjb) {
        this.billEjb = billEjb;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
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

    private void savePatient() {
        switch (getPatientTabId()) {
            case "tabNewPt":
                getNewPatient().setCreater(getSessionController().getLoggedUser());
                getNewPatient().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
                getNewPatient().getPerson().setCreater(getSessionController().getLoggedUser());
                getNewPatient().getPerson().setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
                getPersonFacade().create(getNewPatient().getPerson());
                getPatientFacade().create(getNewPatient());
                tmpPatient = getNewPatient();
                break;
            case "tabSearchPt":
                tmpPatient = getSearchedPatient();
                break;
        }
    }

    public boolean putToBills() {
        bills = new ArrayList<>();
        Set<Department> billDepts = new HashSet<>();
        for (BillEntry e : lstBillEntries) {
            billDepts.add(e.getBillItem().getItem().getDepartment());
        }

        for (Department d : billDepts) {
            PreBill myBill = new PreBill();
            myBill = saveBill(d, myBill);

            if (myBill == null) {
                return false;
            }

            List<BillEntry> tmp = new ArrayList<>();

            for (BillEntry e : lstBillEntries) {
                if (Objects.equals(e.getBillItem().getItem().getDepartment().getId(), d.getId())) {
                    BillItem bi = getBillBean().saveBillItem(myBill, e, getSessionController().getLoggedUser());
                    //getBillBean().calculateBillItem(myBill, e);
                    myBill.getBillItems().add(bi);
                    tmp.add(e);
                }
            }

//            if (getSessionController().getInstitutionPreference().isPartialPaymentOfOpdBillsAllowed()) {
//                myBill.setCashPaid(cashPaid);
//            }

            getBillFacade().edit(myBill);

            getBillBean().calculateBillItems(myBill, tmp);
            bills.add(myBill);
        }

        return true;
    }

    public void setPrintigBill() {
        //System.out.println("In Print");
        billPrint = bill;
        billsPrint = bills;
        lstBillComponentsPrint = lstBillComponents;
        lstBillEntriesPrint = lstBillEntries;
        lstBillFeesPrint = lstBillFees;
        lstBillItemsPrint = lstBillItems;
        //System.out.println("Out Print");
    }

    public void settleBill() {
        if (errorCheck()) {
            return;
        }

        savePatient();

        if (getBillBean().checkDepartment(getLstBillEntries()) == 1) {
            PreBill temp = new PreBill();
            PreBill b = saveBill(lstBillEntries.get(0).getBillItem().getItem().getDepartment(), temp);

            if (b == null) {
                return;
            }

            List<BillItem> list = new ArrayList<>();
            for (BillEntry billEntry : getLstBillEntries()) {
                list.add(getBillBean().saveBillItem(b, billEntry, getSessionController().getLoggedUser()));
            }

            b.setBillItems(list);

            getBillFacade().edit(b);
            getBillBean().calculateBillItems(b, getLstBillEntries());

//            if (getSessionController().getInstitutionPreference().isPartialPaymentOfOpdBillsAllowed()) {
//                b.setCashPaid(cashPaid);
//                if (cashPaid >= b.getTransSaleBillTotalMinusDiscount()) {
//                    b.setBalance(0.0);
//                    b.setNetTotal(b.getTransSaleBillTotalMinusDiscount());
//                } else {
//                    b.setBalance(b.getTransSaleBillTotalMinusDiscount() - b.getCashPaid());
//                    b.setNetTotal(b.getCashPaid());
//                }
//            }
            b.setBalance(b.getNetTotal());
            getBillFacade().edit(b);
            getBills().add(b);

        } else {
            boolean result = putToBills();
            if (result == false) {
                return;
            }
        }

        saveBatchBill();
        saveBillItemSessions();

//        if (toStaff != null && getPaymentMethod() == PaymentMethod.Credit) {
//            staffBean.updateStaffCredit(toStaff, netTotal);
//            UtilityController.addSuccessMessage("User Credit Updated");
//        }

        UtilityController.addSuccessMessage("Bill Saved");
        setPrintigBill();
        checkBillValues();
        printPreview = true;
    }

    public boolean checkBillValues(Bill b) {
        if (getSessionController().getInstitutionPreference().isPartialPaymentOfOpdBillsAllowed()) {
            return false;
        }

        Double[] billItemValues = billBean.fetchBillItemValues(b);
        double billItemTotal = billItemValues[0];
        double billItemDiscount = billItemValues[1];
        double billItemNetTotal = billItemValues[2];

        if (billItemTotal != b.getTotal() || billItemDiscount != b.getDiscount() || billItemNetTotal != b.getNetTotal()) {
            return true;
        }

        Double[] billFeeValues = billBean.fetchBillFeeValues(b);
        double billFeeTotal = billFeeValues[0];
        double billFeeDiscount = billFeeValues[1];
        double billFeeNetTotal = billFeeValues[2];

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

    @EJB
    StaffBean staffBean;

    private void saveBillItemSessions() {
        for (BillEntry be : lstBillEntries) {
            be.getBillItem().setBillSession(getServiceSessionBean().createBillSession(be.getBillItem()));
            if (be.getBillItem().getBillSession() != null) {
                System.err.println("IN");
                getBillSessionFacade().create(be.getBillItem().getBillSession());
            }
        }
    }
    @EJB
    private BatchBillFacade batchBillFacade;

    private void saveBatchBill() {
        PreBill tmp = new PreBill();
        tmp.setBillType(BillType.OpdBathcBillPre);
        tmp.setBillClassType(BillClassType.PreBill);
        tmp.setPatient(tmpPatient);
        tmp.setInstitution(getSessionController().getInstitution());
        tmp.setDepartment(getSessionController().getDepartment());
        tmp.setPaymentScheme(paymentScheme);
        tmp.setPaymentMethod(paymentMethod);
        tmp.setCreatedAt(new Date());
        tmp.setCreater(getSessionController().getLoggedUser());
        //Institution ID (INS ID)
        String insId = getBillNumberGenerator().institutionBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), tmp.getBillType(), tmp.getBillClassType(), BillNumberSuffix.NONE);
        tmp.setInsId(insId);

        //Department ID (DEPT ID)
        String deptId = getBillNumberGenerator().departmentBillNumberGenerator(getSessionController().getDepartment(), getSessionController().getDepartment(), tmp.getBillType(), tmp.getBillClassType());
        tmp.setDeptId(deptId);

        getBillFacade().create(tmp);

        double dbl = 0;
        double dblT = 0;
        double dblD = 0;
        double reminingCashPaid = cashPaid;
        for (Bill b : bills) {
            b.setBackwardReferenceBill(tmp);
            dbl += b.getNetTotal();
            dblT += b.getTotal();
            dblD += b.getDiscount();

//            if (getSessionController().getInstitutionPreference().isPartialPaymentOfOpdBillsAllowed()) {
//                b.setCashPaid(reminingCashPaid);
//
//                if (reminingCashPaid > b.getTransSaleBillTotalMinusDiscount()) {
//                    b.setBalance(0.0);
//                    b.setNetTotal(b.getTransSaleBillTotalMinusDiscount());
//                } else {
//                    b.setBalance(b.getTotal() - b.getCashPaid());
//                    b.setNetTotal(reminingCashPaid);
//                }
//            }
//            reminingCashPaid = reminingCashPaid - b.getNetTotal();

            getBillFacade().edit(b);

            tmp.getForwardReferenceBills().add(b);
        }

        tmp.setNetTotal(dbl);
        tmp.setDiscount(dblD);
        tmp.setTotal(dblT);
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
            ////System.out.println("ggg : " + getBillSearch().getComment());
            getBillSearch().cancelBill();
        }

        tmp.copy(billedBill);
        tmp.setBilledBill(billedBill);

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(tmp, getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
    }

    public void dateChangeListen() {
        getNewPatient().getPerson().setDob(getCommonFunctions().guessDob(yearMonthDay));

    }

    private PreBill saveBill(Department bt, PreBill temp) {
        temp.setBillType(BillType.OpdPreBill);
        temp.setDepartment(getSessionController().getDepartment());
        temp.setInstitution(getSessionController().getInstitution());
        temp.setToDepartment(bt);
        temp.setToInstitution(bt.getInstitution());

        temp.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setStaff(staff);
        temp.setToStaff(toStaff);
        temp.setReferredBy(referredBy);
        temp.setReferralNumber(referralId);
        temp.setReferredByInstitution(referredByInstitution);
        temp.setCreditCompany(creditCompany);
        temp.setComments(comment);

        getBillBean().setPaymentMethodData(temp, paymentMethod, getPaymentMethodData());

        temp.setBillDate(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setBillTime(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        temp.setPatient(tmpPatient);

        temp.setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(tmpPatient));

        temp.setPaymentScheme(getPaymentScheme());
        temp.setPaymentMethod(paymentMethod);
        temp.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
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
        String deptId = getBillNumberGenerator().departmentBillNumberGenerator(temp.getDepartment(), temp.getToDepartment(), temp.getBillType(), BillClassType.PreBill);
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
      
        //System.out.println("getBillNumberGenerator() = " + getBillNumberGenerator());
        //System.out.println("bill = " + bill);
        //System.out.println("bill.getInstitution() = " + bill.getInstitution());
        
        String insId = getBillNumberGenerator().institutionBillNumberGenerator(bill.getInstitution(), bill.getToDepartment(), bill.getBillType(), BillClassType.BilledBill, BillNumberSuffix.NONE);
//        try {
//            insId = getBillNumberGenerator().institutionBillNumberGenerator(bill, bill.getToDepartment(), BillClassType.BilledBill, BillNumberSuffix.NONE);
//        } catch (Exception e) {
//            if (recurseCount < 50) {
//                insId = generateBillNumberInsId(bill);
//                recurseCount++;
//            }
//        }

        return insId;
    }

    private boolean checkPatientAgeSex() {

        if (getPatientTabId().equals("tabNewPt")) {

            if (getNewPatient().getPerson().getName() == null || getNewPatient().getPerson().getName().trim().equals("") || getNewPatient().getPerson().getSex() == null || getNewPatient().getPerson().getDob() == null) {
                UtilityController.addErrorMessage("Can not bill without Patient Name, Age or Sex.");
                return true;
            }

            if (!com.divudi.java.CommonFunctions.checkAgeSex(getNewPatient().getPerson().getDob(), getNewPatient().getPerson().getSex(), getNewPatient().getPerson().getTitle())) {
                UtilityController.addErrorMessage("Check Title,Age,Sex");
                return true;
            }

            if (getNewPatient().getPerson().getPhone().length() < 1) {
                UtilityController.addErrorMessage("Phone Number is Required it should be fill");
                return true;
            }

        }

        return false;

    }

    private boolean institutionReferranceNumberExist() {
        String jpql;
        HashMap m = new HashMap();
        jpql = "Select b from Bill b where "
                + "b.retired = false and "
                + "upper(b.referralNumber) =:rid ";
        m.put("rid", referralId.toUpperCase());
        List<Bill> tempBills = getFacade().findBySQL(jpql, m);
        if (tempBills == null || tempBills.isEmpty()) {
            return false;
        }
        return true;
    }

    private boolean errorCheck() {

        if (getLstBillEntries().isEmpty()) {
            UtilityController.addErrorMessage("No Items added to the bill.");
            return true;
        }

        if (referredByInstitution != null && referredByInstitution.getInstitutionType() != InstitutionType.CollectingCentre) {
            if (referralId == null || referralId.trim().equals("")) {
                JsfUtil.addErrorMessage("Please Enter Referrance Number");
                return true;
            } else {

                if (institutionReferranceNumberExist()) {

                    JsfUtil.addErrorMessage("Alredy Entered");
                    return true;
                }

            }

        }

        if (!getLstBillEntries().get(0).getBillItem().getItem().isPatientNotRequired()) {
            if (getPatientTabId().equals("tabSearchPt")) {
                if (getSearchedPatient() == null) {
                    UtilityController.addErrorMessage("Plese Select Patient");
                    return true;
                }
            }

            if (getPatientTabId().equals("tabNewPt")) {
                if (getNewPatient().getPerson().getName() == null
                        || getNewPatient().getPerson().getName().trim().equals("")) {
                    UtilityController.addErrorMessage("Can not bill without Patient Name");
                    return true;
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
        }

        if (getPaymentMethod() == null) {
            UtilityController.addErrorMessage("Select Payment Method.");
            return true;
        }

        if (getPaymentSchemeController().errorCheckPaymentMethod(paymentMethod, getPaymentMethodData())) {
            return true;
        }

        if (paymentMethod != null && paymentMethod == PaymentMethod.Credit) {
            if (toStaff == null && creditCompany == null) {
                UtilityController.addErrorMessage("Please select Staff Member under welfare or credit company.");
                return true;
            }
            if (toStaff != null && creditCompany != null) {
                UtilityController.addErrorMessage("Both staff member and a company is selected. Please select either Staff Member under welfare or credit company.");
                return true;
            }
            if (toStaff != null) {
                if (toStaff.getAnnualWelfareUtilized() + netTotal > toStaff.getAnnualWelfareQualified()) {
                    UtilityController.addErrorMessage("No enough walfare credit.");
                    return true;
                }
            }
        }

        if ((getCreditCompany() != null || toStaff != null) && (paymentMethod != PaymentMethod.Credit && paymentMethod != PaymentMethod.Cheque && paymentMethod != PaymentMethod.Slip)) {
            UtilityController.addErrorMessage("Check Payment method");
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

    List<BillSession> billSessions;
    @EJB
    BillSessionFacade billSessionFacade;
    @Inject
    ServiceSessionFunctions serviceSessionBean;

    public List<BillSession> getBillSessions() {
        return billSessions;
    }

    public void fillBillSessions(SelectEvent event) {
        //System.out.println("event = " + event);
        //System.out.println("this = filling bill sessions");
        if (lastBillItem != null && lastBillItem.getItem() != null) {
            billSessions = getServiceSessionBean().getBillSessions(lastBillItem.getItem(), getSessionDate());
            //System.out.println("billSessions = " + billSessions);
        } else {
            //System.out.println("billSessions = " + billSessions);
            if (billSessions == null || !billSessions.isEmpty()) {
                //System.out.println("new array");
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

    public void addToBill() {

        if (getCurrentBillItem() == null) {
            UtilityController.addErrorMessage("Nothing to add");
            return;
        }
        if (getCurrentBillItem().getItem() == null) {
            UtilityController.addErrorMessage("Please select an Item");
            return;
        }
        if (getCurrentBillItem().getItem().getTotal() == 0.0) {
            UtilityController.addErrorMessage("Please corect item fee");
            return;
        }

        if (getCurrentBillItem().getItem().getDepartment() == null) {
            UtilityController.addErrorMessage("Please set Department to Item");
            return;
        }

        if (getCurrentBillItem().getItem().getCategory() == null) {
            UtilityController.addErrorMessage("Please set Category to Item");
            return;
        }

        getCurrentBillItem().setSessionDate(sessionDate);

//        New Session
        //   getCurrentBillItem().setBillSession(getServiceSessionBean().createBillSession(getCurrentBillItem()));
        lastBillItem = getCurrentBillItem();
        BillEntry addingEntry = new BillEntry();
        addingEntry.setBillItem(getCurrentBillItem());
        addingEntry.setLstBillComponents(getBillBean().billComponentsFromBillItem(getCurrentBillItem()));
        addingEntry.setLstBillFees(getBillBean().billFeefromBillItem(getCurrentBillItem()));
        addingEntry.setLstBillSessions(getBillBean().billSessionsfromBillItem(getCurrentBillItem()));
        getLstBillEntries().add(addingEntry);
        getCurrentBillItem().setRate(getBillBean().billItemRate(addingEntry));
        getCurrentBillItem().setQty(1.0);
        getCurrentBillItem().setNetValue(getCurrentBillItem().getRate() * getCurrentBillItem().getQty()); // Price == Rate as Qty is 1 here

        calTotals();

        if (getCurrentBillItem().getNetValue() == 0.0) {
            UtilityController.addErrorMessage("Please enter the rate");
            return;
        }
        clearBillItemValues();
        //UtilityController.addSuccessMessage("Item Added");
    }

    public void clearBillItemValues() {
        currentBillItem = null;

        recreateBillItems();
    }

    private void clearBillValues() {
        setNewPatient(null);
        setSearchedPatient(null);
        setReferredBy(null);
        setReferredByInstitution(null);
        setReferralId(null);
        setSessionDate(null);
        setCreditCompany(null);
        setYearMonthDay(null);
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

        patientTabId = "tabNewPt";

        fromOpdEncounter = false;
        opdEncounterComments = "";
        patientSearchTab = 0;
    }

    private void recreateBillItems() {
        //Only remove Total and BillComponenbts,Fee and Sessions. NOT bill Entries
        lstBillComponents = null;
        lstBillFees = null;
        lstBillItems = null;

        //billTotal = 0.0;
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
//     //   //System.out.println("calculating totals");
        if (paymentMethod == null) {
            return;
        }

        if (toStaff != null) {
            System.err.println("Inside");
            paymentScheme = null;
            creditCompany = null;
        }

        double billDiscount = 0.0;
        double billGross = 0.0;
        double billNet = 0.0;
        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getSearchedPatient());

        for (BillEntry be : getLstBillEntries()) {
            ////System.out.println("bill item entry");
            double entryGross = 0.0;
            double entryDis = 0.0;
            double entryNet = 0.0;
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

                //Membership Scheme
                if (membershipScheme != null) {
                    priceMatrix = getPriceMatrixController().getOpdMemberDisCount(paymentMethod, membershipScheme, department, category);
                    getBillBean().setBillFees(bf, isForeigner(), paymentMethod, membershipScheme, bi.getItem(), priceMatrix);
                    //System.out.println("priceMetrix = " + priceMatrix);

                } else {
                    //Payment  Scheme && Credit Company
                    priceMatrix = getPriceMatrixController().getPaymentSchemeDiscount(paymentMethod, paymentScheme, department, item);
                    getBillBean().setBillFees(bf, isForeigner(), paymentMethod, paymentScheme, getCreditCompany(), priceMatrix);
                }

                entryGross += bf.getFeeGrossValue();
                entryNet += bf.getFeeValue();
                entryDis += bf.getFeeDiscount();
                ////System.out.println("fee net is " + bf.getFeeValue());

            }

            bi.setDiscount(entryDis);
            bi.setGrossValue(entryGross);
            bi.setNetValue(entryNet);

            ////System.out.println("item is " + bi.getItem().getName());
            ////System.out.println("item gross is " + bi.getGrossValue());
            ////System.out.println("item net is " + bi.getNetValue());
            ////System.out.println("item dis is " + bi.getDiscount());
            billGross += bi.getGrossValue();
            billNet += bi.getNetValue();
            billDiscount += bi.getDiscount();
            //     billDis = billDis + entryDis;
        }
        setDiscount(billDiscount);
        setTotal(billGross);
        setNetTotal(billNet);

//        if (getSessionController().getInstitutionPreference().isPartialPaymentOfOpdBillsAllowed()) {
//            //System.out.println("cashPaid = " + cashPaid);
//            //System.out.println("billNet = " + billNet);
//            if (cashPaid >= billNet) {
//                //System.out.println("fully paid = ");
//                setDiscount(billDiscount);
//                setTotal(billGross);
//                setNetTotal(billNet);
//                setCashBalance(cashPaid - billNet - billDiscount);
//                //System.out.println("cashBalance = " + cashBalance);
//            } else {
//                //System.out.println("half paid = ");
//                setDiscount(billDiscount);
//                setTotal(billGross);
//                setNetTotal(cashPaid);
//                setCashBalance(billNet - cashPaid - billDiscount);
//                //System.out.println("cashBalance = " + cashBalance);
//            }
//            cashRemain = cashPaid;
//        }

        //      ////System.out.println("bill tot is " + billGross);
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
        paymentMethod = PaymentMethod.Cash;
    }

    public void makeNull() {
        clearBillItemValues();
        clearBillValues();
        paymentMethod = null;
        printPreview = false;

    }

    public void removeBillItem() {

        //TODO: Need to add Logic
        ////System.out.println(getIndex());
        if (getIndex() != null) {
            //  boolean remove;
            BillEntry temp = getLstBillEntries().get(getIndex());
            ////System.out.println("Removed Item:" + temp.getBillItem().getNetValue());
            recreateList(temp);
            // remove = getLstBillEntries().remove(getIndex());

            //  getLstBillEntries().remove(index);
            //////System.out.println("Is Removed:" + remove);
            calTotals();

        }

    }

    public void recreateList(BillEntry r) {
        List<BillEntry> temp = new ArrayList<>();
        for (BillEntry b : getLstBillEntries()) {
            if (b.getBillItem().getItem() != r.getBillItem().getItem()) {
                temp.add(b);
                ////System.out.println(b.getBillItem().getNetValue());
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
                setSearchedPatient(null);
            }
        }
        calTotals();
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

    public OpdPreBillController() {
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
        System.err.println("Change Listen 1 ");
        calTotals();
        System.err.println("Change Listen 2 ");
    }

    public String getPatientTabId() {
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
    }

    public Patient getNewPatient() {
        if (newPatient == null) {
            newPatient = new Patient();
            Person p = new Person();

            newPatient.setPerson(p);
        }
        return newPatient;
    }

    public void setNewPatient(Patient newPatient) {
        this.newPatient = newPatient;
    }

    public Patient getSearchedPatient() {
        return searchedPatient;
    }

    public void setSearchedPatient(Patient searchedPatient) {
        this.searchedPatient = searchedPatient;
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

    public BatchBillFacade getBatchBillFacade() {
        return batchBillFacade;
    }

    public void setBatchBillFacade(BatchBillFacade batchBillFacade) {
        this.batchBillFacade = batchBillFacade;
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
                + " and (upper(p.patient.person.name)  "
                + "like :q or upper(p.insId)  "
                + "like :q) order by p.insId";
        ////System.out.println(sql);
        hm.put("q", "%" + query.toUpperCase() + "%");
        hm.put("btp", BillType.InwardAppointmentBill);
        suggestions = getFacade().findBySQL(sql, hm);

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

    public List<BillFeePayment> getBillFeePayments() {
        if (billFeePayments==null) {
            billFeePayments=new ArrayList<>();
        }
        return billFeePayments;
    }

    public void setBillFeePayments(List<BillFeePayment> billFeePayments) {
        this.billFeePayments = billFeePayments;
    }

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
    }

//    /**
//     *
//     */
//    @FacesConverter("bill")
//    public static class BillControllerConverter implements Converter {
//
//        @Override
//        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
//            if (value == null || value.length() == 0) {
//                return null;
//            }
//            OpdPreBillController controller = (OpdPreBillController) facesContext.getApplication().getELResolver().
//                    getValue(facesContext.getELContext(), null, "billController");
//            return controller.getBillFacade().find(getKey(value));
//        }
//
//        java.lang.Long getKey(String value) {
//            java.lang.Long key;
//            key = Long.valueOf(value);
//            return key;
//        }
//
//        String getStringKey(java.lang.Long value) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(value);
//            return sb.toString();
//        }
//
//        @Override
//        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
//            if (object == null) {
//                return null;
//            }
//            if (object instanceof Bill) {
//                Bill o = (Bill) object;
//                return getStringKey(o.getId());
//            } else {
//                throw new IllegalArgumentException("object " + object + " is of type "
//                        + object.getClass().getName() + "; expected type: " + OpdPreBillController.class.getName());
//            }
//        }
//    }
//
//    @FacesConverter(forClass = Bill.class)
//    public static class BillConverter implements Converter {
//
//        @Override
//        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
//            if (value == null || value.length() == 0) {
//                return null;
//            }
//            OpdPreBillController controller = (OpdPreBillController) facesContext.getApplication().getELResolver().
//                    getValue(facesContext.getELContext(), null, "billController");
//            return controller.getBillFacade().find(getKey(value));
//        }
//
//        java.lang.Long getKey(String value) {
//            java.lang.Long key;
//            key = Long.valueOf(value);
//            return key;
//        }
//
//        String getStringKey(java.lang.Long value) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(value);
//            return sb.toString();
//        }
//
//        @Override
//        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
//            if (object == null) {
//                return null;
//            }
//            if (object instanceof Bill) {
//                Bill o = (Bill) object;
//                return getStringKey(o.getId());
//            } else {
//                throw new IllegalArgumentException("object " + object + " is of type "
//                        + object.getClass().getName() + "; expected type: " + OpdPreBillController.class.getName());
//            }
//        }
//    }
}
