package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.collectingCentre.CollectingCentreBillController;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.FeeType;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.dataStructure.BillListWithTotals;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.ejb.BillEjb;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.service.StaffService;
import com.divudi.entity.AuditEvent;
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
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Payment;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Person;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
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
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.opd.OpdBillController;
import com.divudi.data.BillTypeAtomic;
import static com.divudi.data.PaymentMethod.Agent;
import static com.divudi.data.PaymentMethod.Card;
import static com.divudi.data.PaymentMethod.Cash;
import static com.divudi.data.PaymentMethod.Cheque;
import static com.divudi.data.PaymentMethod.Credit;
import static com.divudi.data.PaymentMethod.MultiplePaymentMethods;
import static com.divudi.data.PaymentMethod.OnCall;
import static com.divudi.data.PaymentMethod.OnlineSettlement;
import static com.divudi.data.PaymentMethod.PatientDeposit;
import static com.divudi.data.PaymentMethod.Slip;
import static com.divudi.data.PaymentMethod.Staff;
import static com.divudi.data.PaymentMethod.YouOweMe;
import static com.divudi.data.PaymentMethod.ewallet;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.entity.FamilyMember;
import com.divudi.entity.PatientDeposit;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.java.CommonFunctions;
import com.divudi.light.common.BillLight;
import com.divudi.service.BillService;
import com.divudi.service.PaymentService;
import com.divudi.service.ProfessionalPaymentService;
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
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class BillController implements Serializable, ControllerWithMultiplePayments {

    private static final long serialVersionUID = 1L;

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
    PaymentFacade paymentFacade;
    @EJB
    BillItemFacade billItemFacede;
    @EJB
    BillService billService;
    @EJB
    StaffService staffService;
    @EJB
    PaymentService paymentService;
    @Inject
    private BillSearch billSearch;
    @Inject
    ProfessionalPaymentService professionalPaymentService;
    /**
     * Controllers
     */
    @Inject
    SessionController sessionController;
    @Inject
    CommonController commonController;
    @Inject
    PaymentSchemeController paymentSchemeController;
    @Inject
    ApplicationController applicationController;
    @Inject
    private EnumController enumController;
    @Inject
    CollectingCentreBillController collectingCentreBillController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private OpdBillController opdBillController;
    @Inject
    PatientDepositController patientDepositController;
    @Inject
    DrawerController drawerController;

    /**
     * Class Vairables
     */
    private boolean batchBillCancellationStarted = false;
    private boolean printPreview;
    private String patientTabId = "tabNewPt";
    //Interface Data
    private PaymentScheme paymentScheme;
    private PaymentMethod paymentMethod;
    private List<PaymentMethod> paymentMethods;
    private Patient newPatient;
    private Patient searchedPatient;
    private Doctor referredBy;
    private Institution referredByInstitution;
    String referralId;
    private Institution creditCompany;
    private Institution collectingCentre;
    private Staff staff;
    Staff toStaff;
    private double total;
    private double remainAmount;
    private Patient patient;
    private double discount;
    private double vat;
    private double netTotal;
    double netPlusVat;
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
    BillType billType;

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

    @Inject
    private BillBeanController billBean;
    @Inject
    private AuditEventApplicationController auditEventApplicationController;

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
    List<Bill> selectedBills;
    Double grosTotal;
    Bill bill;
    private Bill batchBill;
    boolean foreigner = false;
    Date sessionDate;
    String strTenderedValue;
    private YearMonthDay yearMonthDay;
    private PaymentMethodData paymentMethodData;
    @EJB
    private CashTransactionBean cashTransactionBean;

    @Inject
    SearchController searchController;

    public String toAddNewCollectingCentre() {
        return "/admin/institutions/collecting_centre";
    }

    public List<Bill> validBillsOfBatchBill(Bill batchBill) {
        String j = "Select b from Bill b where b.backwardReferenceBill=:bb and b.cancelled=false";
        Map m = new HashMap();
        m.put("bb", batchBill);
        return billFacade.findByJpql(j, m);
    }

    public List<Bill> getSelectedBills() {
        return selectedBills;
    }

    public void setSelectedBills(List<Bill> selectedBills) {
        this.selectedBills = selectedBills;
    }

    public void calculateSelectedBillTotals() {
        BillListWithTotals bt = billEjb.calculateBillTotals(selectedBills);
        grosTotal = bt.getGrossTotal();
        netTotal = bt.getNetTotal();
        discount = bt.getDiscount();
        vat = bt.getVat();
        netPlusVat = vat + netTotal;
    }

    public void clear() {
        opdBill = new BilledBill();
        printPreview = false;
        opdPaymentCredit = 0.0;
        comment = null;
        searchController.createTableByKeywordToPayBills();
    }

    public double getHospitalFee(BillItem i) {
        List<BillFee> billFees = billFeesOfBillItem(i);
        double hospitalFee = 0.0;
        for (BillFee billFee : billFees) {
            if (billFee.getFee() == null) {
                continue;
            }
            if (billFee.getFee().getFeeType() != FeeType.Staff) {
                hospitalFee += billFee.getFeeValue();
            }
        }
        return hospitalFee;
    }

    public double getStaffFee(BillItem i) {
        List<BillFee> billFees = billFeesOfBillItem(i);
        double hospitalFee = 0.0;
        for (BillFee billFee : billFees) {
            if (billFee.getFee() == null) {
                continue;
            }
            if (billFee.getFee().getFeeType() == FeeType.Staff) {
                hospitalFee += billFee.getFeeValue();
            }
        }
        return hospitalFee;
    }

    public void clearPharmacy() {
        opdBill = new BilledBill();
        printPreview = false;
        opdPaymentCredit = 0.0;
        comment = null;
        searchController.createTablePharmacyCreditToPayBills();
    }

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

        getBillBean().setPaymentMethodData(temp, paymentMethod, getPaymentMethodData());

        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setPaymentMethod(paymentMethod);
        temp.setCreatedAt(new Date());
        temp.setCreater(getSessionController().getLoggedUser());
        getFacade().create(temp);
        //create bill fee payments
        //create bill fee payments
        //create bill fee payments
        //create bill fee payments
        reminingCashPaid = opdPaymentCredit;

        Payment p = createPayment(temp, paymentMethod);

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

    public boolean hasRefunded(BillFee bf) {
        boolean refunded = false;
        if (bf == null) {
            return refunded;
        }
        String jpql = "select bf "
                + " from BillFee bf "
                + " where bf.retired=:ret "
                + " and bf.referenceBillFee=:bf "
                + " and bf.billItem.retired=:ret "
                + " and bf.bill.retired=:ret ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("bf", bf);
        BillFee rbf = billFeeFacade.findFirstByJpql(jpql, m);
        if (rbf != null) {
            refunded = true;
        }
        return refunded;
    }

    public boolean hasPaidToStaff(BillFee bf) {
        boolean paid = false;
        if (bf == null) {
            return paid;
        }
        if (bf.getPaidValue() > 0.0) {
            paid = true;
        }
        return paid;
    }

    public void save(Bill savingBill) {
        if (savingBill == null) {
            return;
        }
        if (savingBill.getId() == null) {
            savingBill.setCreatedAt(new Date());
            savingBill.setCreater(sessionController.getLoggedUser());
            try {
                getFacade().create(savingBill);
            } catch (Exception e) {
                getFacade().edit(savingBill);
            }
        } else {
            getFacade().edit(savingBill);
        }
    }

    public void saveBillItem(BillItem sb) {
        if (sb == null) {
            return;
        }
        if (sb.getId() == null) {
            sb.setCreatedAt(new Date());
            sb.setCreater(sessionController.getLoggedUser());
            getBillItemFacade().create(sb);
        } else {
            getBillItemFacade().edit(sb);
        }
    }

    public void saveBillFee(BillFee sb) {
        if (sb == null) {
            return;
        }
        if (sb.getId() == null) {
            sb.setCreatedAt(new Date());
            sb.setCreater(sessionController.getLoggedUser());
            getBillFeeFacade().create(sb);
        } else {
            getBillFeeFacade().edit(sb);
        }
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

        getBillBean().setPaymentMethodData(temp, paymentMethod, getPaymentMethodData());

        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setPaymentMethod(paymentMethod);
        temp.setCreatedAt(new Date());
        temp.setCreater(getSessionController().getLoggedUser());
        getFacade().create(temp);

        JsfUtil.addSuccessMessage("Paid");
        opdBill = temp;
        printPreview = true;

    }

    public BillNumberGenerator getBillNumberGenerator() {
        return billNumberGenerator;
    }

    public StaffService getStaffService() {
        return staffService;
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
            //////// // System.out.println("Error in converting tendered value. \n " + e.getMessage());
        }
    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public List<Bill> completeOpdCreditBill(String qry) {
        List<Bill> a = null;
        String sql;
        HashMap hash = new HashMap();
        if (qry != null) {
            sql = "select c from BilledBill c "
                    + " where abs(c.netTotal)-abs(c.paidAmount)>:val "
                    + " and c.billType= :btp "
                    + " and c.paymentMethod= :pm "
                    + " and c.cancelledBill is null "
                    + " and c.refundedBill is null "
                    + " and c.retired=false "
                    + " and ((c.insId) like :q or"
                    + " (c.patient.person.name) like :q "
                    + " or (c.creditCompany.name) like :q ) "
                    + " order by c.creditCompany.name";
            hash.put("btp", BillType.OpdBill);
            hash.put("pm", PaymentMethod.Credit);
            hash.put("val", 0.1);
            hash.put("q", "%" + qry.toUpperCase() + "%");
            a = getFacade().findByJpql(sql, hash);
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }
    
//    public List<Bill> completeOpdCreditBatchBill(String qry) {
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
//            hash.put("btp", BillType.OpdBathcBill);
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
//    
    public List<Bill> completeOpdCreditPackageBatchBill(String qry) {
        System.out.println("completeOpdCreditPackageBatchBill");
        List<Bill> a = null;
        String sql;
        HashMap hash = new HashMap();
        if (qry != null) {
            sql = "select c from BilledBill c "
                    + " where abs(c.netTotal)-abs(c.paidAmount)>:val "
                    + " and c.billTypeAtomic in :btas "
                    + " and c.paymentMethod= :pm "
                    + " and c.cancelledBill is null "
                    + " and c.refundedBill is null "
                    + " and c.retired=false "
                    + " and ((c.deptId) like :q or"
                    + " (c.patient.person.name) like :q "
                    + " or (c.creditCompany.name) like :q ) "
                    + " order by c.creditCompany.name";
            List<BillTypeAtomic> btas = new ArrayList<>();
            btas.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
            btas.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            hash.put("btas", btas);
            hash.put("pm", PaymentMethod.Credit);
            hash.put("val", 0.1);
            hash.put("q", "%" + qry.toUpperCase() + "%");
            System.out.println("qry = " + qry);
            System.out.println("hash = " + hash);
            a = getFacade().findByJpql(sql, hash);
            System.out.println("a = " + a);
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }
    
    public List<Bill> completeOpdCreditBatchBill(String qry) {
        System.out.println("completeOpdCreditBatchBill");
        List<Bill> a = null;
        String jpql;
        HashMap params = new HashMap();
        if (qry != null) {
            jpql = "select c from BilledBill c "
                    + " where abs(c.netTotal)-abs(c.paidAmount)>:val "
                    + " and c.billTypeAtomic in :btas "
                    + " and c.paymentMethod= :pm "
                    + " and c.cancelledBill is null "
                    + " and c.refundedBill is null "
                    + " and c.retired=false "
                    + " and ((c.deptId) like :q or"
                    + " (c.patient.person.name) like :q "
                    + " or (c.creditCompany.name) like :q ) "
                    + " order by c.creditCompany.name";
            List<BillTypeAtomic> btas = new ArrayList<>();
            btas.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
            btas.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            params.put("btas", btas);
            params.put("pm", PaymentMethod.Credit);
            params.put("val", 0.1);
            params.put("q", "%" + qry.toUpperCase() + "%");
            a = getFacade().findByJpql(jpql, params);
            System.out.println("jpql = " + jpql);
            System.out.println("params = " + params);
            System.out.println("a = " + a);
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    public List<Bill> completePharmacyCreditBill(String qry) {
        List<Bill> a = null;
        String sql;
        HashMap hash = new HashMap();
        if (qry != null) {
            sql = "select b from BilledBill b "
                    + " where (abs(b.netTotal)-abs(b.paidAmount))>:val "
                    + " and b.billType in :btps "
                    + " and b.paymentMethod= :pm "
                    + " and b.institution=:ins "
                    //                    + " and b.department=:dep "
                    + " and b.retired=false "
                    + " and b.refunded=false "
                    + " and b.cancelled=false "
                    + " and b.toStaff is null "
                    + " and ( (b.insId) like :q or "
                    + " (b.deptId) like :q or "
                    + " (b.toInstitution.name) like :q ) "
                    + " order by b.deptId ";
            hash.put("btps", Arrays.asList(new BillType[]{BillType.PharmacyWholeSale, BillType.PharmacySale}));
            hash.put("pm", PaymentMethod.Credit);
            hash.put("val", 0.1);
            hash.put("q", "%" + qry.toUpperCase() + "%");
            hash.put("ins", getSessionController().getInstitution());
//            hash.put("dep", getSessionController().getDepartment());
//            //// // System.out.println("hash = " + hash);
//            //// // System.out.println("sql = " + sql);
//            //// // System.out.println("getSessionController().getInstitution().getName() = " + getSessionController().getInstitution().getName());
//            //// // System.out.println("getSessionController().getDepartment().getName() = " + getSessionController().getDepartment().getName());
            a = getFacade().findByJpql(sql, hash);
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    public List<Bill> completeBillFromDealor(String qry) {
        List<Bill> a = null;
        String sql;
        HashMap hash = new HashMap();
        BillType[] billTypesArrayBilled = {BillType.PharmacyGrnBill, BillType.PharmacyPurchaseBill, BillType.StoreGrnBill, BillType.StorePurchase};
        List<BillType> billTypesListBilled = Arrays.asList(billTypesArrayBilled);
        if (qry != null) {
            sql = "select c from BilledBill c "
                    + "where  abs(c.netTotal)-abs(c.paidAmount)>:val "
                    + " and c.billType in :bts "
                    + " and c.createdAt is not null "
                    + " and c.deptId is not null "
                    + " and c.cancelledBill is null "
                    + " and c.retired=false "
                    + " and c.paymentMethod=:pm  "
                    + " and (((c.deptId) like :q ) "
                    + " or ((c.fromInstitution.name)  like :q ))"
                    + " order by c.fromInstitution.name";
            hash.put("bts", billTypesListBilled);
            hash.put("pm", PaymentMethod.Credit);
            hash.put("val", 0.1);
            hash.put("q", "%" + qry.toUpperCase() + "%");
            //     hash.put("pm", PaymentMethod.Credit);
            a = getFacade().findByJpql(sql, hash, 20);
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    public List<Bill> completeBillFromDealorStore(String qry) {
        List<Bill> a = null;
        String sql;
        HashMap hash = new HashMap();
        if (qry != null) {
            sql = "select c from BilledBill c "
                    + "where  abs(c.netTotal)-abs(c.paidAmount)>:val "
                    + " and (c.billType= :btp1 or c.billType= :btp2  )"
                    + " and c.createdAt is not null "
                    + " and c.deptId is not null "
                    + " and c.cancelledBill is null "
                    + " and c.retired=false "
                    + " and c.paymentMethod=:pm  "
                    + " and c.fromInstitution.institutionType=:insTp  "
                    + " and (((c.deptId) like :q ) "
                    + " or ((c.fromInstitution.name)  like :q ))"
                    + " order by c.fromInstitution.name";
            hash.put("btp1", BillType.PharmacyGrnBill);
            hash.put("btp2", BillType.PharmacyPurchaseBill);
            hash.put("pm", PaymentMethod.Credit);
            hash.put("insTp", InstitutionType.StoreDealor);
            hash.put("val", 0.1);
            hash.put("q", "%" + qry.toUpperCase() + "%");
            //     hash.put("pm", PaymentMethod.Credit);
            a = getFacade().findByJpql(sql, hash, 10);
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    public List<Bill> completeSurgeryBills(String qry) {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b "
                + " where b.billType = :billType "
                + " and b.cancelled=false "
                + " and b.retired=false "
                + " and b.patientEncounter.discharged=false ";

        sql += " and  (((b.patientEncounter.patient.person.name) like :q )";
        sql += " or  ((b.patientEncounter.bhtNo) like :q )";
        sql += " or  ((b.insId) like :q )";
        sql += " or  ((b.procedure.item.name) like :q ))";
        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.SurgeryBill);
        temMap.put("q", "%" + qry.toUpperCase() + "%");
        List<Bill> tmps = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 20);

        return tmps;
    }

    public List<Bill> fillPatientSurgeryBills(PatientEncounter pe) {
        String jpql;
        Map temMap = new HashMap();
        jpql = "select b from BilledBill b "
                + " where b.billType = :billType "
                + " and b.cancelled=false "
                + " and b.retired=false "
                + " and b.patientEncounter=:pe ";
        temMap.put("billType", BillType.SurgeryBill);
        temMap.put("pe", pe);
        List<Bill> tmps = getBillFacade().findByJpql(jpql, temMap, TemporalType.TIMESTAMP, 20);
        return tmps;
    }

    public List<Bill> getDealorBills(Institution institution, List<BillType> billTypes) {
        String sql;
        HashMap hash = new HashMap();

        sql = "select c from BilledBill c where "
                + " abs(c.netTotal)-abs(c.paidAmount)>:val"
                + " and c.billType in :bts"
                + " and c.createdAt is not null "
                + " and c.deptId is not null "
                + " and c.cancelled=false"
                + " and c.retired=false"
                + " and c.paymentMethod=:pm  "
                + " and c.fromInstitution=:ins "
                + " order by c.id ";
        hash.put("bts", billTypes);
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
        auditEvent.setEventTrigger("getOpdBills()");
        auditEventApplicationController.logAuditEvent(auditEvent);

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

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
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
        auditEvent.setEventTrigger("getPharamacyWholeSaleCreditBills()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        BillType[] billTypes = {BillType.PharmacyWholeSale};
        PaymentMethod[] paymentMethods = {PaymentMethod.Credit};
        BillListWithTotals r = billEjb.findBillsAndTotals(fromDate, toDate, billTypes, null, department, institution, paymentMethods);
        bills = r.getBills();
        netTotal = r.getNetTotal();
        discount = r.getDiscount();
        grosTotal = r.getGrossTotal();
        vat = r.getVat();
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void getPharmacyBills() {
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
        auditEvent.setEventTrigger("getPharmacyBills()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        BillType[] billTypes = {BillType.PharmacySale};
        BillListWithTotals r = billEjb.findBillsAndTotals(fromDate, toDate, billTypes, null, department, institution, null);
        bills = r.getBills();
        netTotal = r.getNetTotal();
        discount = r.getDiscount();
        grosTotal = r.getGrossTotal();
        vat = r.getVat();
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

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
        auditEvent.setEventTrigger("getPharmacyWholeBills()");
        auditEventApplicationController.logAuditEvent(auditEvent);
        BillType[] billTypes = {BillType.PharmacySale};
        BillListWithTotals r = billEjb.findBillsAndTotals(fromDate, toDate, billTypes, null, department, institution, null);
        bills = r.getBills();
        netTotal = r.getNetTotal();
        discount = r.getDiscount();
        grosTotal = r.getGrossTotal();
        vat = r.getVat();
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

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

    private void savePatient() {
        switch (getPatientTabId()) {
            case "tabNewPt":
                getNewPatient().setPhn(applicationController.createNewPersonalHealthNumber(getSessionController().getInstitution()));
                getNewPatient().setCreatedInstitution(getSessionController().getInstitution());
                getNewPatient().setCreater(getSessionController().getLoggedUser());
                getNewPatient().setCreatedAt(new Date());
                getNewPatient().getPerson().setCreater(getSessionController().getLoggedUser());
                getNewPatient().getPerson().setCreatedAt(new Date());
                try {
                    getPersonFacade().create(getNewPatient().getPerson());
                } catch (Exception e) {
                    getPersonFacade().edit(getNewPatient().getPerson());
                }
                try {
                    getPatientFacade().create(getNewPatient());
                } catch (Exception e) {
                    getPatientFacade().edit(getNewPatient());
                }
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

            if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
                myBill.setCashPaid(cashPaid);
            }

            getBillFacade().edit(myBill);

            getBillBean().calculateBillItems(myBill, tmp);
            createPaymentsForBills(myBill, tmp);

            bills.add(myBill);
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

    @Deprecated
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
                //for Create Bill Fee Payments
//                list.add(getBillBean().saveBillItem(b, billEntry, getSessionController().getLoggedUser(),p));
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

            createPaymentsForBills(b, getLstBillEntries());

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

        if (toStaff != null && getPaymentMethod() == PaymentMethod.Credit) {
            staffService.updateStaffCredit(toStaff, netPlusVat);
            JsfUtil.addSuccessMessage("User Credit Updated");
        }

        JsfUtil.addSuccessMessage("Bill Saved");
        setPrintigBill();
        checkBillValues();
        printPreview = true;

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
        tmp.setBillTypeAtomic(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
        tmp.setPaymentScheme(paymentScheme);
        tmp.setPaymentMethod(paymentMethod);
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

    public void cancellAll() {

        if (bills == null) {
            JsfUtil.addErrorMessage("No bills to cancel");
            return;
        }

        if (bills.isEmpty()) {
            JsfUtil.addErrorMessage("No bills to cancel");
            return;
        }

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

    public List<Bill> billsOfBatchBill(Bill batchBill) {
        String jpql;
        Map m = new HashMap();
        jpql = "select b "
                + " from Bill b"
                + " where b.backwardReferenceBill=:bb";
        m.put("bb", batchBill);
        return billFacade.findByJpql(jpql, m);
    }

    public List<BillItem> billItemsOfBill(Bill bill) {
        String jpql;
        Map m = new HashMap();
        jpql = "select bi "
                + " from BillItem bi"
                + " where bi.retired=:ret"
                + " and bi.bill=:b ";
        m.put("b", bill);
        m.put("ret", false);
        return billItemFacade.findByJpql(jpql, m);
    }

    public List<BillFee> billFeesOfBill(Bill bill) {
        String jpql;
        Map m = new HashMap();
        jpql = "select bf "
                + " from BillFee bf"
                + " where bf.retired=:ret"
                + " and bf.bill=:b ";
        m.put("b", bill);
        m.put("ret", false);
        return billFeeFacade.findByJpql(jpql, m);
    }

    public List<BillFee> findBillFees(Staff staff, Date fromDate, Date toDate) {
        List<BillFee> tmpFees;
        String jpql;
        List<BillTypeAtomic> btcs = new ArrayList<>();
        btcs.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        btcs.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        btcs.add(BillTypeAtomic.CC_BILL);
        Map<String, Object> m = new HashMap<>();

        jpql = "select bf "
                + " from BillFee bf"
                + " where bf.retired=:ret"
                + " and bf.staff=:staff "
                + " and bf.bill.cancelled=:can"
                + " and bf.billItem.refunded=:ret"
                + " and (bf.feeValue - bf.paidValue) > 0 "
                + " and bf.bill.billTypeAtomic in :btcs "
                + " and bf.fee.feeType=:ft";

        if (fromDate != null) {
            jpql += " and bf.bill.billTime >= :fromDate";
            m.put("fromDate", fromDate);
        }

        if (toDate != null) {
            jpql += " and bf.bill.billTime <= :toDate";
            m.put("toDate", toDate);
        }

        m.put("ret", false);
        m.put("can", false);
        m.put("btcs", btcs);
        m.put("staff", staff);
        m.put("ret", false);
        m.put("ft", FeeType.Staff);

        tmpFees = billFeeFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);

//        List<BillFee> removingBillFees = new ArrayList<>();
//        for (BillFee bf : tmpFees) {
//            m = new HashMap<>();
//            jpql = "SELECT bi FROM BillItem bi where "
//                    + " bi.retired=false"
//                    + " and bi.bill.cancelled=false "
//                    + " and type(bi.bill)=:class "
//                    + " and bi.referanceBillItem=:rbi";
//            m.put("class", RefundBill.class);
//            m.put("rbi", bf.getBillItem());
//            BillItem rbi = getBillItemFacade().findFirstByJpql(jpql, m);
//            if (rbi != null) {
//                removingBillFees.add(bf);
//            }
//        }
//        tmpFees.removeAll(removingBillFees);
        return tmpFees;
    }

    public List<BillFee> billFeesOfBillItem(BillItem billItem) {
        String jpql;
        Map m = new HashMap();
        jpql = "select bf "
                + " from BillFee bf"
                + " where bf.retired=:ret"
                + " and bf.billItem=:bi ";
        m.put("bi", billItem);
        m.put("ret", false);
        return billFeeFacade.findByJpql(jpql, m);
    }

    public String navigateToCancelOpdBatchBill() {
        if (batchBill == null) {
            JsfUtil.addErrorMessage("No Batch bill is selected");
            return "";
        }
        bills = billsOfBatchBill(batchBill);
        paymentMethod = null;
        patient = batchBill.getPatient();
        paymentMethods = billService.availablePaymentMethodsForCancellation(batchBill);
        comment = null;
        printPreview = false;
        batchBillCancellationStarted = false;
        return "/opd/batch_bill_cancel?faces-redirect=true;";
    }

    public String cancelOpdBatchBill() {
        batchBillCancellationStarted = true;
        if (getBatchBill() == null) {
            JsfUtil.addErrorMessage("No bill");
            batchBillCancellationStarted = false;
            return "";
        }
        if (getBatchBill().getId() == null) {
            JsfUtil.addErrorMessage("No Saved bill");
            batchBillCancellationStarted = false;
            return "";
        }
        if (!getWebUserController().hasPrivilege("OpdCancel")) {
            JsfUtil.addErrorMessage("You have no privilege to cancel OPD bills. Please contact System Administrator.");
            batchBillCancellationStarted = false;
            return "";
        }
        if (errorsPresentOnOpdBatchBillCancellation()) {
            batchBillCancellationStarted = false;
            return "";
        }
        if(professionalPaymentService.isProfessionalFeePaidForBatchBill(batchBill)){
            JsfUtil.addErrorMessage("Professional Fees or Outside Fees have already made this bill. Cancel them first and try again.");
            batchBillCancellationStarted = false;
            return "";
        }

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);

        Bill cancellationBatchBill = new CancelledBill();
        cancellationBatchBill.copy(batchBill);
        cancellationBatchBill.setDepartment(sessionController.getDepartment());
        cancellationBatchBill.setInstitution(sessionController.getInstitution());
        cancellationBatchBill.setFromDepartment(batchBill.getFromDepartment());
        cancellationBatchBill.setToDepartment(batchBill.getToDepartment());
        cancellationBatchBill.setFromInstitution(batchBill.getFromInstitution());
        cancellationBatchBill.setToInstitution(batchBill.getToInstitution());
        cancellationBatchBill.setBillType(BillType.OpdBathcBill);
        cancellationBatchBill.setBillTypeAtomic(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
        cancellationBatchBill.setDeptId(deptId);
        cancellationBatchBill.setInsId(deptId);
        cancellationBatchBill.setCreatedAt(new Date());
        cancellationBatchBill.setCreater(getSessionController().getLoggedUser());
        cancellationBatchBill.setTotal(0 - Math.abs(batchBill.getTotal()));
        cancellationBatchBill.setHospitalFee(0 - Math.abs(batchBill.getHospitalFee()));
        cancellationBatchBill.setCollctingCentreFee(0 - Math.abs(batchBill.getCollctingCentreFee()));
        cancellationBatchBill.setProfessionalFee(0 - Math.abs(batchBill.getProfessionalFee()));
        cancellationBatchBill.setGrantTotal(0 - Math.abs(batchBill.getGrantTotal()));
        cancellationBatchBill.setDiscount(0 - Math.abs(batchBill.getDiscount()));
        cancellationBatchBill.setNetTotal(0 - Math.abs(batchBill.getNetTotal()));
        if (paymentMethod != null) {
            cancellationBatchBill.setPaymentMethod(paymentMethod);
        }
        cancellationBatchBill.setBilledBill(batchBill);
        getBillFacade().create(cancellationBatchBill);

        batchBill.setCancelled(true);
        batchBill.setCancelledBill(cancellationBatchBill);
        getBillFacade().edit(batchBill);

        bills = billService.fetchIndividualBillsOfBatchBill(batchBill);

        for (Bill originalBill : bills) {
            cancelSingleBillWhenCancellingOpdBatchBill(originalBill, cancellationBatchBill);
        }

        if (cancellationBatchBill.getPaymentMethod() == PaymentMethod.PatientDeposit) {
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(cancellationBatchBill.getPatient(), sessionController.getDepartment());
            patientDepositController.updateBalance(cancellationBatchBill, pd);
        } else if (cancellationBatchBill.getPaymentMethod() == PaymentMethod.Credit) {
            if (cancellationBatchBill.getToStaff() != null) {
                staffService.updateStaffCredit(cancellationBatchBill.getToStaff(), 0 - Math.abs(cancellationBatchBill.getNetTotal() + getBill().getVat()));
                JsfUtil.addSuccessMessage("Staff Credit Updated");
                cancellationBatchBill.setFromStaff(cancellationBatchBill.getToStaff());
                getBillFacade().edit(cancellationBatchBill);
            }
        }

        List<Payment> cancelPayments = createPaymentForOpdBatchBillCancellation(cancellationBatchBill, paymentMethod);
        
        if (cancellationBatchBill.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
            paymentService.updateBalances(cancelPayments);
        }

        drawerController.updateDrawerForOuts(cancelPayments);

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cancellationBatchBill, getSessionController().getLoggedUser());
        opdBillController.setBills(bills);
        opdBillController.setBatchBill(batchBill);
        getSessionController().setLoggedUser(wb);

        printPreview = true;
        batchBillCancellationStarted = false;
        return "/opd/opd_batch_bill_print?faces-redirect=true";
    }
    
    public String cancelPackageBatchBill() {
        batchBillCancellationStarted = true;
        if (getBatchBill() == null) {
            JsfUtil.addErrorMessage("No bill");
            batchBillCancellationStarted = false;
            return "";
        }
        if (getBatchBill().getId() == null) {
            JsfUtil.addErrorMessage("No Saved bill");
            batchBillCancellationStarted = false;
            return "";
        }
        if (!getWebUserController().hasPrivilege("OpdCancel")) {
            JsfUtil.addErrorMessage("You have no privilege to cancel OPD bills. Please contact System Administrator.");
            batchBillCancellationStarted = false;
            return "";
        }
        if (errorsPresentOnOpdBatchBillCancellation()) {
            batchBillCancellationStarted = false;
            return "";
        }

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);

        Bill cancellationBatchBill = new CancelledBill();
        cancellationBatchBill.copy(batchBill);
        cancellationBatchBill.setDepartment(sessionController.getDepartment());
        cancellationBatchBill.setInstitution(sessionController.getInstitution());
        cancellationBatchBill.setFromDepartment(batchBill.getFromDepartment());
        cancellationBatchBill.setToDepartment(batchBill.getToDepartment());
        cancellationBatchBill.setFromInstitution(batchBill.getFromInstitution());
        cancellationBatchBill.setToInstitution(batchBill.getToInstitution());
        cancellationBatchBill.setBillType(BillType.OpdBathcBill);
        cancellationBatchBill.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
        cancellationBatchBill.setInsId(deptId);
        cancellationBatchBill.setDeptId(deptId);
        cancellationBatchBill.setCreatedAt(new Date());
        cancellationBatchBill.setCreater(getSessionController().getLoggedUser());
        cancellationBatchBill.setTotal(0 - Math.abs(batchBill.getTotal()));
        cancellationBatchBill.setHospitalFee(0 - Math.abs(batchBill.getHospitalFee()));
        cancellationBatchBill.setCollctingCentreFee(0 - Math.abs(batchBill.getCollctingCentreFee()));
        cancellationBatchBill.setProfessionalFee(0 - Math.abs(batchBill.getProfessionalFee()));
        cancellationBatchBill.setGrantTotal(0 - Math.abs(batchBill.getGrantTotal()));
        cancellationBatchBill.setDiscount(0 - Math.abs(batchBill.getDiscount()));
        cancellationBatchBill.setNetTotal(0 - Math.abs(batchBill.getNetTotal()));
        if (paymentMethod != null) {
            cancellationBatchBill.setPaymentMethod(paymentMethod);
        }
        cancellationBatchBill.setBilledBill(batchBill);
        getBillFacade().create(cancellationBatchBill);

        batchBill.setCancelled(true);
        batchBill.setCancelledBill(cancellationBatchBill);
        getBillFacade().edit(batchBill);

        bills = billService.fetchIndividualBillsOfBatchBill(batchBill);

        for (Bill originalBill : bills) {
            cancelSingleBillWhenCancellingPackageBatchBill(originalBill, cancellationBatchBill);
        }

        if (cancellationBatchBill.getPaymentMethod() == PaymentMethod.PatientDeposit) {
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(cancellationBatchBill.getPatient(), sessionController.getDepartment());
            patientDepositController.updateBalance(cancellationBatchBill, pd);
        } else if (cancellationBatchBill.getPaymentMethod() == PaymentMethod.Credit) {
            if (cancellationBatchBill.getToStaff() != null) {
                staffService.updateStaffCredit(cancellationBatchBill.getToStaff(), 0 - Math.abs(cancellationBatchBill.getNetTotal() + getBill().getVat()));
                JsfUtil.addSuccessMessage("Staff Credit Updated");
                cancellationBatchBill.setFromStaff(cancellationBatchBill.getToStaff());
                getBillFacade().edit(cancellationBatchBill);
            }
        }

        List<Payment> cancelPayments = createPaymentForOpdBatchBillCancellation(cancellationBatchBill, paymentMethod);

        drawerController.updateDrawerForOuts(cancelPayments);

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cancellationBatchBill, getSessionController().getLoggedUser());
        opdBillController.setBills(bills);
        opdBillController.setBatchBill(batchBill);
        getSessionController().setLoggedUser(wb);

        printPreview = true;
        batchBillCancellationStarted = false;
        return "/opd/opd_batch_bill_print?faces-redirect=true";
    }
    
    public void cancelSingleBillWhenCancellingPackageBatchBill(Bill originalBill, Bill cancellationBatchBill) {
        if (originalBill == null && originalBill == null) {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }
        String deptId = getBillNumberGenerator().departmentBillNumberGeneratorYearly(originalBill.getDepartment(), BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);

        CancelledBill individualCancelltionBill = new CancelledBill();
        individualCancelltionBill.copy(originalBill);
        individualCancelltionBill.invertAndAssignValuesFromOtherBill(originalBill);
        individualCancelltionBill.setBillType(BillType.OpdBill);
        individualCancelltionBill.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        individualCancelltionBill.setDeptId(deptId);
        individualCancelltionBill.setInsId(deptId);
        individualCancelltionBill.setPaymentMethod(cancellationBatchBill.getPaymentMethod());
        individualCancelltionBill.setBilledBill(cancellationBatchBill);
        individualCancelltionBill.setBillDate(new Date());
        individualCancelltionBill.setBillTime(new Date());
        individualCancelltionBill.setCreatedAt(new Date());
        individualCancelltionBill.setCreater(getSessionController().getLoggedUser());
        individualCancelltionBill.setDepartment(getSessionController().getDepartment());
        individualCancelltionBill.setInstitution(getSessionController().getInstitution());
        individualCancelltionBill.setForwardReferenceBill(cancellationBatchBill);
        individualCancelltionBill.setComments(comment);
        billService.saveBill(individualCancelltionBill);

        List<BillItem> list = createBillItemsForOpdBatchBillCancellation(originalBill, individualCancelltionBill);
        try {
            individualCancelltionBill.setBillItems(list);
        } catch (Exception e) {

        }
        billService.saveBill(individualCancelltionBill);

        originalBill.setCancelled(true);
        originalBill.setCancelledBill(individualCancelltionBill);
        billService.saveBill(originalBill);
    }


    private boolean errorsPresentOnOpdBatchBillCancellation() {
        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }
        batchBill = billService.reloadBill(batchBill);
        if (batchBill.isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled");
            return true;
        }
        if (batchBill.isRefunded()) {
            JsfUtil.addErrorMessage("Already Refunded");
            return true;
        }
        List<Bill> individualBills = billService.fetchIndividualBillsOfBatchBill(batchBill);
        if (individualBills == null) {
            JsfUtil.addErrorMessage("No Individual Bills");
            return true;
        }
        for (Bill individualBill : individualBills) {
            if (individualBill.isCancelled()) {
                JsfUtil.addErrorMessage("One individual bill of this batch bill is already Cancelled. Can not cancel Batch Bill !!! ");
                return true;
            }
            if (individualBill.isRefunded()) {
                JsfUtil.addErrorMessage("One individual bill of this batch bill is already Refunded. Can not cancel Batch Bill !!! ");
                return true;
            }
        }
        return false;
    }

    public void cancellAllBillsOfBatchBill() {
        if (batchBill == null) {
            JsfUtil.addErrorMessage("No Batch bill is selected");
            return;
        }
        bills = billsOfBatchBill(batchBill);
        if (bills == null) {
            JsfUtil.addErrorMessage("No bills to cancel");
            return;
        }

        if (bills.isEmpty()) {
            JsfUtil.addErrorMessage("No bills to cancel");
            return;
        }

        Bill cancellationBatchBill = new CancelledBill();
        cancellationBatchBill.setDepartment(sessionController.getDepartment());
        cancellationBatchBill.setInstitution(sessionController.getInstitution());
        cancellationBatchBill.setFromDepartment(batchBill.getFromDepartment());
        cancellationBatchBill.setToDepartment(batchBill.getToDepartment());
        cancellationBatchBill.setFromInstitution(batchBill.getFromInstitution());
        cancellationBatchBill.setToInstitution(batchBill.getToInstitution());

        cancellationBatchBill.setBillType(BillType.OpdBathcBill);
        cancellationBatchBill.setBillTypeAtomic(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
        String deptId = getBillNumberGenerator().generateBillNumber(cancellationBatchBill.getDepartment(), cancellationBatchBill.getToDepartment(), cancellationBatchBill.getBillType(), cancellationBatchBill.getBillClassType());
        String insId = getBillNumberGenerator().generateBillNumber(cancellationBatchBill.getInstitution(), cancellationBatchBill.getBillType(), cancellationBatchBill.getBillClassType());

        cancellationBatchBill.setInsId(deptId);
        cancellationBatchBill.setInsId(insId);
        cancellationBatchBill.setCreatedAt(new Date());
        cancellationBatchBill.setCreater(getSessionController().getLoggedUser());

        cancellationBatchBill.setTotal(0 - Math.abs(batchBill.getTotal()));
        cancellationBatchBill.setGrantTotal(0 - Math.abs(batchBill.getGrantTotal()));
        cancellationBatchBill.setDiscount(0 - Math.abs(batchBill.getDiscount()));
        cancellationBatchBill.setNetTotal(0 - Math.abs(batchBill.getNetTotal()));
        cancellationBatchBill.setPaymentMethod(paymentMethod);

        cancellationBatchBill.setForwardReferenceBill(batchBill);
        batchBill.setCancelled(true);
        batchBill.setCancelledBill(cancellationBatchBill);
        getBillFacade().edit(batchBill);

        getBillFacade().create(cancellationBatchBill);

        for (Bill originalBill : bills) {
            cancelSingleBillWhenCancellingOpdBatchBill(originalBill, cancellationBatchBill);
        }

        cancellationBatchBill.copy(batchBill);
        cancellationBatchBill.setBilledBill(batchBill);

        createPaymentForOpdBatchBillCancellation(cancellationBatchBill, batchBill.getPaymentMethod());

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(cancellationBatchBill, getSessionController().getLoggedUser());

        getSessionController().setLoggedUser(wb);
    }

    public void cancelSingleBillWhenCancellingOpdBatchBill(Bill originalBill, Bill cancellationBatchBill) {
        if (originalBill == null && originalBill == null) {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }
        String deptId = getBillNumberGenerator().departmentBillNumberGeneratorYearly(originalBill.getDepartment(), BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);

        CancelledBill individualCancelltionBill = new CancelledBill();
        individualCancelltionBill.copy(originalBill);
        individualCancelltionBill.invertAndAssignValuesFromOtherBill(originalBill);
        individualCancelltionBill.setBillType(BillType.OpdBill);
        individualCancelltionBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        individualCancelltionBill.setDeptId(deptId);
        individualCancelltionBill.setInsId(deptId);
        individualCancelltionBill.setPaymentMethod(cancellationBatchBill.getPaymentMethod());
        individualCancelltionBill.setBilledBill(cancellationBatchBill);
        individualCancelltionBill.setBillDate(new Date());
        individualCancelltionBill.setBillTime(new Date());
        individualCancelltionBill.setCreatedAt(new Date());
        individualCancelltionBill.setCreater(getSessionController().getLoggedUser());
        individualCancelltionBill.setDepartment(getSessionController().getDepartment());
        individualCancelltionBill.setInstitution(getSessionController().getInstitution());
        individualCancelltionBill.setForwardReferenceBill(cancellationBatchBill);
        individualCancelltionBill.setComments(comment);
        billService.saveBill(individualCancelltionBill);

        List<BillItem> list = createBillItemsForOpdBatchBillCancellation(originalBill, individualCancelltionBill);
        try {
            individualCancelltionBill.setBillItems(list);
        } catch (Exception e) {

        }
        billService.saveBill(individualCancelltionBill);

        originalBill.setCancelled(true);
        originalBill.setCancelledBill(individualCancelltionBill);
        billService.saveBill(originalBill);
    }

    public List<Payment> createPaymentForOpdBatchBillCancellation(Bill cancellationBatchBill, PaymentMethod pm) {
        List<Payment> ps = new ArrayList<>();
        if (paymentMethod == null) {
            List<Payment> originalBillPayments = billService.fetchBillPayments(cancellationBatchBill.getBilledBill());
            if (originalBillPayments != null) {
                for (Payment originalBillPayment : originalBillPayments) {
                    Payment p = originalBillPayment.clonePaymentForNewBill();
                    p.invertValues();
                    p.setBill(cancellationBatchBill);
                    p.setInstitution(getSessionController().getInstitution());
                    p.setDepartment(getSessionController().getDepartment());
                    p.setCreatedAt(new Date());
                    p.setCreater(getSessionController().getLoggedUser());
                    paymentFacade.create(p);
                    ps.add(p);
                }
            }
        } else if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(cancellationBatchBill);
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
                    case Slip:
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                    case YouOweMe:
                    case MultiplePaymentMethods:
                }
                p.setPaidValue(0 - Math.abs(p.getPaidValue()));
                paymentFacade.create(p);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(cancellationBatchBill);
            p.setInstitution(getSessionController().getInstitution());
            p.setDepartment(getSessionController().getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(getSessionController().getLoggedUser());
            p.setPaymentMethod(pm);
            p.setPaidValue(cancellationBatchBill.getNetTotal());

            switch (pm) {
                case Card:
                    p.setBank(getPaymentMethodData().getCreditCard().getInstitution());
                    p.setCreditCardRefNo(getPaymentMethodData().getCreditCard().getNo());
                    break;
                case Cheque:
                    p.setChequeDate(getPaymentMethodData().getCheque().getDate());
                    p.setChequeRefNo(getPaymentMethodData().getCheque().getNo());
                    break;
                case Cash:
                    break;
                case ewallet:

                case Agent:
                case Credit:
                case PatientDeposit:
                case Slip:
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(0 - Math.abs(p.getBill().getNetTotal()));
            paymentFacade.create(p);
            ps.add(p);
        }
        return ps;
    }

    /**
     * if (paymentMethod == PaymentMethod.MultiplePaymentMethods) { for
     * (ComponentDetail cd :
     * paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails())
     * { Payment p = new Payment(); p.setBill(cancellationBatchBill);
     * p.setInstitution(getSessionController().getInstitution());
     * p.setDepartment(getSessionController().getDepartment());
     * p.setCreatedAt(new Date());
     * p.setCreater(getSessionController().getLoggedUser());
     * p.setPaymentMethod(cd.getPaymentMethod());
     *
     * switch (cd.getPaymentMethod()) { case Card:
     * p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
     * p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
     * p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
     * break; case Cheque:
     * p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
     * p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
     * p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
     * break; case Cash:
     * p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
     * break; case ewallet:
     *
     * case Agent: case Credit: case PatientDeposit: case Slip: case OnCall:
     * case OnlineSettlement: case Staff: case YouOweMe: case
     * MultiplePaymentMethods: }
     *
     * paymentFacade.create(p); ps.add(p); } } else { Payment p = new Payment();
     * p.setBill(cancellationBatchBill);
     * p.setInstitution(getSessionController().getInstitution());
     * p.setDepartment(getSessionController().getDepartment());
     * p.setCreatedAt(new Date());
     * p.setCreater(getSessionController().getLoggedUser());
     * p.setPaymentMethod(pm);
     * p.setPaidValue(cancellationBatchBill.getNetTotal());
     *
     * switch (pm) { case Card:
     * p.setBank(getPaymentMethodData().getCreditCard().getInstitution());
     * p.setCreditCardRefNo(getPaymentMethodData().getCreditCard().getNo());
     * break; case Cheque:
     * p.setChequeDate(getPaymentMethodData().getCheque().getDate());
     * p.setChequeRefNo(getPaymentMethodData().getCheque().getNo()); break; case
     * Cash: break; case ewallet:
     *
     * case Agent: case Credit: case PatientDeposit: case Slip: case OnCall:
     * case OnlineSettlement: case Staff: case YouOweMe: case
     * MultiplePaymentMethods: }
     *
     * p.setPaidValue(p.getBill().getNetTotal()); paymentFacade.create(p);
     * ps.add(p); } return ps;
     */
    private List<BillItem> createBillItemsForOpdBatchBillCancellation(Bill originalBill, Bill cancellationBill) {
        List<BillItem> list = new ArrayList<>();
        for (BillItem originalBillItem : originalBill.getBillItems()) {
            BillItem newBillItem = new BillItem();
            newBillItem.setBill(cancellationBill);
            newBillItem.setItem(originalBillItem.getItem());
            newBillItem.setNetValue(0 - originalBillItem.getNetValue());
            newBillItem.setGrossValue(0 - originalBillItem.getGrossValue());
            newBillItem.setHospitalFee(0 - originalBillItem.getHospitalFee());
            newBillItem.setCollectingCentreFee(0 - originalBillItem.getCollectingCentreFee());
            newBillItem.setStaffFee(0 - originalBillItem.getStaffFee());
            newBillItem.setRate(0 - originalBillItem.getRate());
            newBillItem.setVat(0 - originalBillItem.getVat());
            newBillItem.setVatPlusNetValue(0 - originalBillItem.getVatPlusNetValue());
            newBillItem.setCatId(originalBillItem.getCatId());
            newBillItem.setDeptId(originalBillItem.getDeptId());
            newBillItem.setInsId(originalBillItem.getInsId());
            newBillItem.setDiscount(0 - originalBillItem.getDiscount());
            newBillItem.setQty(0 - originalBillItem.getQty());
            newBillItem.setRate(originalBillItem.getRate());
            newBillItem.setCreatedAt(new Date());
            newBillItem.setCreater(getSessionController().getLoggedUser());
            newBillItem.setPaidForBillFee(originalBillItem.getPaidForBillFee());
            newBillItem.setReferanceBillItem(originalBillItem);
            billItemFacede.create(newBillItem);

            cancelBillComponents(originalBill, cancellationBill, originalBillItem, newBillItem);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + originalBillItem.getId();
            List<BillFee> originalBillFees = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(originalBill, cancellationBill, originalBillItem, newBillItem, originalBillFees);

            list.add(newBillItem);

        }

        return list;
    }

    public void cancelBillFee(Bill originalBill, Bill cancellationBill, BillItem originalBillItem, BillItem cancellationBillItem, List<BillFee> originalBillFees) {
        for (BillFee originalBillFee : originalBillFees) {
            BillFee newBillFee = new BillFee();
            newBillFee.setFee(originalBillFee.getFee());
            newBillFee.setPatienEncounter(originalBillFee.getPatienEncounter());
            newBillFee.setPatient(originalBillFee.getPatient());
            newBillFee.setDepartment(originalBillFee.getDepartment());
            newBillFee.setInstitution(originalBillFee.getInstitution());
            newBillFee.setSpeciality(originalBillFee.getSpeciality());
            newBillFee.setStaff(originalBillFee.getStaff());
            newBillFee.setReferenceBillFee(originalBillFee);
            newBillFee.setBill(cancellationBill);
            newBillFee.setBillItem(originalBillItem);
            newBillFee.setFeeValue(0 - originalBillFee.getFeeValue());
            newBillFee.setFeeGrossValue(0 - originalBillFee.getFeeGrossValue());
            newBillFee.setFeeDiscount(0 - originalBillFee.getFeeDiscount());
            newBillFee.setSettleValue(0 - originalBillFee.getSettleValue());
            newBillFee.setFeeVat(0 - originalBillFee.getFeeVat());
            newBillFee.setFeeVatPlusValue(0 - originalBillFee.getFeeVatPlusValue());
            newBillFee.setCreatedAt(new Date());
            newBillFee.setCreater(getSessionController().getLoggedUser());
            getBillFeeFacade().create(newBillFee);
        }
    }

    private void cancelBillComponents(Bill originalBill, Bill cancellationBill, BillItem originalBillItem, BillItem newBillItem) {
        String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + originalBill.getId();
        List<BillComponent> billComponents = billComponentFacade.findByJpql(sql);
        if (billComponents == null) {
            billComponents = new ArrayList<>();
        }
        for (BillComponent originalBillComponent : billComponents) {
            BillComponent newBillComponent = new BillComponent();
            newBillComponent.setCatId(originalBillComponent.getCatId());
            newBillComponent.setDeptId(originalBillComponent.getDeptId());
            newBillComponent.setInsId(originalBillComponent.getInsId());
            newBillComponent.setDepartment(originalBillComponent.getDepartment());
            newBillComponent.setDeptId(originalBillComponent.getDeptId());
            newBillComponent.setInstitution(originalBillComponent.getInstitution());
            newBillComponent.setItem(originalBillComponent.getItem());
            newBillComponent.setName(originalBillComponent.getName());
            newBillComponent.setPackege(originalBillComponent.getPackege());
            newBillComponent.setSpeciality(originalBillComponent.getSpeciality());
            newBillComponent.setStaff(originalBillComponent.getStaff());
            newBillComponent.setBill(cancellationBill);
            newBillComponent.setBillItem(newBillItem);
            newBillComponent.setCreatedAt(new Date());
            newBillComponent.setCreater(getSessionController().getLoggedUser());
            billComponentFacade.create(newBillComponent);

        }

    }

    public void dateChangeListen() {
        getNewPatient().getPerson().setDob(getCommonFunctions().guessDob(yearMonthDay));

    }

    private Bill saveBill(Department bt, Bill temp) {
        temp.setBillType(BillType.OpdBill);
        temp.setDepartment(getSessionController().getDepartment());
        temp.setInstitution(getSessionController().getInstitution());
        temp.setToDepartment(bt);
        temp.setToInstitution(bt.getInstitution());

        temp.setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        temp.setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        temp.setStaff(staff);
        temp.setToStaff(toStaff);
        temp.setReferredBy(referredBy);
        temp.setReferenceNumber(referralId);
        temp.setReferredByInstitution(referredByInstitution);
        temp.setCreditCompany(creditCompany);
        temp.setCollectingCentre(collectingCentre);
        temp.setComments(comment);

        getBillBean().setPaymentMethodData(temp, paymentMethod, getPaymentMethodData());

        temp.setBillDate(new Date());
        temp.setBillTime(new Date());
        temp.setPatient(tmpPatient);

//        temp.setMembershipScheme(membershipSchemeController.fetchPatientMembershipScheme(tmpPatient, getSessionController().getApplicationPreference().isMembershipExpires()));
        temp.setPaymentScheme(getPaymentScheme());
        temp.setPaymentMethod(paymentMethod);
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

        ////// // System.out.println("getBillNumberGenerator() = " + getBillNumberGenerator());
        ////// // System.out.println("bill = " + bill);
        ////// // System.out.println("bill.getInstitution() = " + bill.getInstitution());
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
                JsfUtil.addErrorMessage("Can not bill without Patient Name, Age or Sex.");
                return true;
            }
            if (!com.divudi.java.CommonFunctions.checkAgeSex(getNewPatient().getPerson().getDob(), getNewPatient().getPerson().getSex(), getNewPatient().getPerson().getTitle())) {
                JsfUtil.addErrorMessage("Mismatch in Title and Gender. Please Check the Title, Age and Sex");
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
                + "(b.referenceNumber) =:rid ";
        m.put("rid", referralId.toUpperCase());
        List<Bill> tempBills = getFacade().findByJpql(jpql, m);
        if (tempBills == null || tempBills.isEmpty()) {
            return false;
        }
        return true;
    }

    private boolean errorCheck() {
        if (getLstBillEntries().isEmpty()) {
            JsfUtil.addErrorMessage("No Items are added to the bill to settle");
            return true;
        }

        if (getPatientTabId().equals("tabSearchPt")) {
            if (getSearchedPatient() == null) {
                JsfUtil.addErrorMessage("Plese Select Patient");
                return true;
            }
        }

        if (getPatientTabId().equals("tabNewPt")) {
            if (getNewPatient() == null) {
                JsfUtil.addErrorMessage("New Patient is NULL. Programming Error. Contact Developer.");
                return true;
            }
            if (getNewPatient().getPerson() == null) {
                JsfUtil.addErrorMessage("New Patient's Person is NULL. Programming Error. Contact Developer.");
                return true;
            }
            if (getNewPatient().getPerson().getName() == null
                    || getNewPatient().getPerson().getName().trim().equals("")) {
                JsfUtil.addErrorMessage("Can not bill without a name for the new Patient !");
                return true;
            }

            if (!sessionController.getApplicationPreference().isOpdSettleWithoutPatientPhoneNumber()) {
                if (getNewPatient().getPerson().getPhone() == null) {
                    JsfUtil.addErrorMessage("Please Enter a Phone Number");
                    return true;
                }
                if (getNewPatient().getPerson().getPhone().trim().equals("")) {
                    JsfUtil.addErrorMessage("Please Enter a Phone Number");
                    return true;
                }
            }

        }

//
//        if (!sessionController.getApplicationPreference().getCanSettleOpdBillWithoutReferringDoctor()) {
//            for (BillEntry be : getLstBillEntries()) {
//                if (be.getBillItem().getItem() instanceof Investigation) {
//                    if (referredBy == null && referredByInstitution == null) {
//                        JsfUtil.addErrorMessage("Please Select a Referring Doctor or a Referring Institute. It is Required for Investigations.");
//                        return true;
//                    }
//                }
//            }
//        }
//            if (getStrTenderedValue() == null) {
//                JsfUtil.addErrorMessage("Please Enter Tenderd Amount");
//                return true;
//            }
//            if (cashPaid < (vat + netTotal)) {
//                JsfUtil.addErrorMessage("Please Enter Correct Tenderd Amount");
//                return true;
//            }
//        if (referredByInstitution != null && referredByInstitution.getInstitutionType() != InstitutionType.CollectingCentre) {
//            if (referralId == null || referralId.trim().equals("")) {
//                JsfUtil.addErrorMessage("Please Enter Referrance Number");
//                return true;
//            } else if (institutionReferranceNumberExist()) {
//
//                JsfUtil.addErrorMessage("Alredy Entered");
//                return true;
//            }
//
//        }
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

        if (getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select Payment Scheme");
            return true;
        }

        if (getPaymentSchemeController().checkPaymentMethodError(paymentMethod, getPaymentMethodData())) {
            return true;
        }

        if (paymentMethod != null && paymentMethod == PaymentMethod.Credit) {
            if (toStaff == null && creditCompany == null && collectingCentre == null) {
                JsfUtil.addErrorMessage("Please select Staff Member under welfare or credit company or Collecting centre.");
                return true;
            }
            if (toStaff != null && creditCompany != null) {
                JsfUtil.addErrorMessage("Both staff member and a company is selected. Please select either Staff Member under welfare or credit company.");
                return true;
            }
            if (toStaff != null) {
                if (toStaff.getAnnualWelfareUtilized() + netTotal > toStaff.getAnnualWelfareQualified()) {
                    JsfUtil.addErrorMessage("No enough walfare credit.");
                    return true;
                }
            }
        }

//        if ((getCreditCompany() != null || toStaff != null) && (paymentMethod != PaymentMethod.Credit && paymentMethod != PaymentMethod.Cheque && paymentMethod != PaymentMethod.Slip)) {
//            JsfUtil.addErrorMessage("Check Payment method");
//            return true;
//        }
        if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {

            if (cashPaid == 0.0) {
                JsfUtil.addErrorMessage("Please enter the paid amount");
                return true;
            }

        }

//        if (getPaymentSchemeController().checkPaid(paymentScheme.getPaymentMethod(), getCashPaid(), getNetTotal())) {
//            return true;
//        }
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
        ////// // System.out.println("event = " + event);
        ////// // System.out.println("this = filling bill sessions");
        if (lastBillItem != null && lastBillItem.getItem() != null) {
            billSessions = getServiceSessionBean().getBillSessions(lastBillItem.getItem(), getSessionDate());
            ////// // System.out.println("billSessions = " + billSessions);
        } else ////// // System.out.println("billSessions = " + billSessions);
        if (billSessions == null || !billSessions.isEmpty()) {
            ////// // System.out.println("new array");
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

    public List<BillLight> listBillsLights(
            BillType bt,
            Institution ins,
            Department dep,
            Date fd,
            Date td) {
        if (bt == null) {
            return null;
        }
//        List<BillType> bts = new ArrayList<>();
//        bts.add(bt);
//        System.out.println("listBillsLights = ");
//        return listBillsLights(bts, ins, dep, fd, td);

        String sql;
        Map temMap = new HashMap();
        sql = "select new com.divudi.light.common.BillLight(bill.id, bill.insId, bill.createdAt, bill.institution.name, bill.toDepartment.name, bill.creater.name, bill.patient.person.name, bill.patient.person.phone, bill.total, bill.discount, bill.netTotal, bill.patient.id) "
                + " from Bill bill "
                + " where bill.retired=:ret ";

        sql += " and bill.createdAt between :fromDate and :toDate ";

        sql += " and bill.billType=:bt ";
        if (ins != null) {
            sql += " and bill.institution=:ins ";
            temMap.put("ins", ins);
        }
        if (dep != null) {
            sql += " and bill.department=:dep ";
            temMap.put("dep", dep);
        }

        sql += " order by bill.createdAt desc  ";

        temMap.put("ret", false);

        temMap.put("bt", bt);
        temMap.put("toDate", td);
        temMap.put("fromDate", fd);
        List<BillLight> lst = getBillFacade().findLightsByJpql(sql, temMap, TemporalType.TIMESTAMP);
        return lst;

    }

    public List<BillLight> listBillsLights(
            List<BillType> billTypes,
            Institution ins,
            Department dep,
            Date fd,
            Date td) {
        String sql;
        Map temMap = new HashMap();
        sql = "select new com.divudi.light.common.BillLight(bill.id, bill.insId, bill.createdAt, bill.institution.name, bill.toDepartment.name, bill.creater.name, bill.patient.person.name, bill.patient.person.phone, bill.total, bill.discount, bill.netTotal, bill.patient.id) "
                + " from Bill bill "
                + " where bill.retired=:ret ";

        sql += " and bill.createdAt between :fromDate and :toDate ";

        sql += " and bill.billType in :billTypes ";
        if (ins != null) {
            sql += " and bill.institution=:ins ";
            temMap.put("ins", ins);
        }
        if (dep != null) {
            sql += " and bill.department=:dep ";
            temMap.put("dep", dep);
        }

        sql += " order by bill.createdAt desc  ";

        temMap.put("ret", false);

        temMap.put("billTypes", billTypes);
        temMap.put("toDate", td);
        temMap.put("fromDate", fd);
        List<BillLight> lst = getBillFacade().findLightsByJpql(sql, temMap, TemporalType.TIMESTAMP);
        return lst;
    }

    public List<Bill> listBills(
            BillType bt,
            Institution ins,
            Department dep,
            Date fd,
            Date td) {
        if (bt == null) {
            return null;
        }
        List<BillType> bts = new ArrayList<>();
        bts.add(bt);
        return listBills(bts, ins, dep, fd, td);

    }

    @Deprecated
    public List<Bill> findUnpaidBillsOld(Date frmDate, Date toDate, List<BillType> billTypes, PaymentMethod pm, Double balanceGraterThan) {
        String jpql;
        HashMap hm;
        jpql = "Select b "
                + " From Bill b "
                + " where b.retired=:ret "
                + " and b.cancelled=:can "
                + " and b.createdAt between :frm and :to ";
        hm = new HashMap();
        hm.put("frm", frmDate);
        hm.put("ret", false);
        hm.put("can", false);
        hm.put("to", toDate);

        if (balanceGraterThan != null) {
            jpql += " and (abs(b.balance) > :val) ";
            hm.put("val", balanceGraterThan);
        }
        if (pm != null) {
            hm.put("pm", pm);
            jpql += " and b.paymentMethod=:pm ";
        }
        if (billTypes != null) {
            hm.put("bts", billTypes);
            jpql += " and b.billType in :bts";
        }
        return getBillFacade().findByJpql(jpql, hm, TemporalType.TIMESTAMP);

    }

    public List<Bill> findUnpaidBills(Date frmDate, Date toDate, List<BillTypeAtomic> billTypes, PaymentMethod pm, Double balanceGraterThan) {
        String jpql;
        HashMap hm;
        List<BillType> bts = null;
        jpql = "Select b "
                + " From Bill b "
                + " where b.retired=:ret "
                + " and b.cancelled=:can "
                + " and b.createdAt between :frm and :to ";
        hm = new HashMap();
        hm.put("frm", frmDate);
        hm.put("ret", false);
        hm.put("can", false);
        hm.put("to", toDate);

        if (balanceGraterThan != null) {
            jpql += " and (abs(b.balance) > :val) ";
            hm.put("val", balanceGraterThan);
        }
        if (pm != null) {
            hm.put("pm", pm);
            jpql += " and b.paymentMethod=:pm ";
        }
        if (billTypes != null) {
            hm.put("bts", billTypes);
            jpql += " and b.billTypeAtomic in :bts";
        }

        if (bts != null) {
            hm.put("bt", bts);
            jpql += " or b.billType in :bt";
        }

        return getBillFacade().findByJpql(jpql, hm, TemporalType.TIMESTAMP);

    }

    public void addMissingBillTypeAtomics() {
        String jpql = "select b "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.billTypeAtomic is null "
                + " and b.billType is not null ";
        Map m = new HashMap();
        m.put("ret", false);
        List<Bill> billsWithoutBillTypeAtomic = getFacade().findByJpql(jpql, m, 1000);
        for (Bill b : billsWithoutBillTypeAtomic) {
            b.setBillTypeAtomic(BillTypeAtomic.getBillTypeAtomic(b.getBillType(), b.getBillClassType()));
            getFacade().edit(b);
        }
    }

    public List<BillType> getBillTypesByAtomicBillTypes(List<BillTypeAtomic> ba) {
        List<BillType> bt = new ArrayList<>();
        if (ba == null) {
            return bt;
        }
        for (BillTypeAtomic billTypeAtomic : ba) {
            if (billTypeAtomic == billTypeAtomic.PHARMACY_GRN) {
                bt.add(billType.PharmacyGrnBill);
            } else if (billTypeAtomic == billTypeAtomic.PHARMACY_WHOLESALE_GRN_BILL) {
                bt.add(billType.PharmacyWholeSale);
            } else if (billTypeAtomic == billTypeAtomic.PHARMACY_DIRECT_PURCHASE) {
                bt.add(billType.PharmacyPurchaseBill);
            }
        }
        return bt;
    }

    public List<Bill> listBills(
            List<BillType> billTypes,
            Institution ins,
            Department dep,
            Date fd,
            Date td) {
        String sql;
        Map temMap = new HashMap();
        sql = "select bill "
                + " from Bill bill "
                + " where bill.retired=:ret ";

        sql += " and bill.createdAt between :fromDate and :toDate ";

        sql += " and bill.billType in :billTypes ";
        if (ins != null) {
            sql += " and bill.institution=:ins ";
            temMap.put("ins", ins);
        }
        if (dep != null) {
            sql += " and bill.department=:dep ";
            temMap.put("dep", dep);
        }

        sql += " order by bill.createdAt desc  ";

        temMap.put("ret", false);

        temMap.put("billTypes", billTypes);
        temMap.put("toDate", td);
        temMap.put("fromDate", fd);
        List<Bill> lst = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        return lst;
    }

    public Bill findBillbyID(Long id) {
        if (id == null) {
            return null;
        }
        Bill tb = getBillFacade().find(id);
        if (tb == null) {
            return null;
        }
        //System.out.println("tb = " + tb);
        return tb;
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

    @Deprecated
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
            JsfUtil.addErrorMessage("Please corect item fee");
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
        double qty = getCurrentBillItem().getQty();
        for (int i = 0; i < qty; i++) {
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
            addingEntry.setLstBillSessions(getBillBean().billSessionsfromBillItem(bi));
            getLstBillEntries().add(addingEntry);
            bi.setRate(getBillBean().billItemRate(addingEntry));
            bi.setQty(1.0);
            bi.setNetValue(bi.getRate() * bi.getQty()); // Price == Rate as Qty is 1 here

            if (bi.getItem().isVatable()) {
                bi.setVat(bi.getNetValue() * bi.getItem().getVatPercentage() / 100);
            }

            bi.setVatPlusNetValue(bi.getNetValue() + bi.getVat());

            calTotals();

            if (bi.getNetValue() == 0.0) {
                JsfUtil.addErrorMessage("Please enter the rate");
                return;
            }
        }
        clearBillItemValues();
        //JsfUtil.addSuccessMessage("Item Added");
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
        setCollectingCentre(null);
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

    private void clearBillValuesForMember() {
        setNewPatient(null);
        setReferredBy(null);
        setReferredByInstitution(null);
        setReferralId(null);
        setSessionDate(null);
        setCreditCompany(null);
        setCollectingCentre(null);
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

        patientTabId = "tabSearchPt";

        fromOpdEncounter = false;
        opdEncounterComments = "";
        patientSearchTab = 1;
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

    public void addMissingRefundStatusForCcRefunds() {
        String jpql = "select bi "
                + " from BillItem bi "
                + " where bi.retired=false "
                + " and bi.bill.billTypeAtomic=:bta";
        Map m = new HashMap();
        m.put("bta", BillTypeAtomic.CC_BILL_REFUND);
        List<BillItem> bis = billItemFacade.findByJpql(jpql, m, 1000);
        System.out.println("bis = " + bis);
        if (bis == null) {
            return;
        }
        for (BillItem bi : bis) {
            System.out.println("bi = " + bi);
            BillItem originalBilItem = bi.getReferanceBillItem();
            System.out.println("originalBilItem = " + originalBilItem);
            System.out.println("1 originalBilItem.isRefunded() = " + originalBilItem.isRefunded());
            originalBilItem.setRefunded(true);
            System.out.println("2 originalBilItem.isRefunded() = " + originalBilItem.isRefunded());
            billItemFacade.edit(originalBilItem);
            System.out.println("3 originalBilItem.isRefunded() = " + originalBilItem.isRefunded());
            billItemFacade.editAndCommit(originalBilItem);
            System.out.println("4 originalBilItem.isRefunded() = " + originalBilItem.isRefunded());
        }
    }

    public String navigateTomissingBillsForCcCancellation() {
        return "/dataAdmin/missing_cc_cancellation_bills?faces-redirect=true";
    }

    public void addMissingValuesForCcCancellationBillItems() {
        String jpql = "select bi "
                + " from BillItem bi "
                + " where bi.retired=false "
                + " and bi.bill.billTypeAtomic=:bta";
        Map m = new HashMap();
        m.put("bta", BillTypeAtomic.CC_BILL_CANCELLATION);
        List<BillItem> bis = billItemFacade.findByJpql(jpql, m, 10000);
        System.out.println("bis = " + bis);
        if (bis == null) {
            return;
        }
        for (BillItem bi : bis) {
            System.out.println("bi = " + bi);
            Institution cc = bi.getBill().getCollectingCentre();
            Double collectingCentreMargin = cc.getPercentage();
            Double netTotal = bi.getNetValue();
            System.out.println("netTotal = " + netTotal);
            Double ccFee = netTotal * collectingCentreMargin / 100;
            System.out.println("ccFee = " + ccFee);
            Double hosFee = netTotal - ccFee;
            System.out.println("hosFee = " + hosFee);
            if (bi.getHospitalFee() == 0.0) {
                bi.setHospitalFee(hosFee);
                billItemFacade.edit(bi);
            }
            if (bi.getCollectingCentreFee() == 0.0) {
                bi.setCollectingCentreFee(ccFee);
                billItemFacade.edit(bi);
            }

        }

    }

    public void calTotals() {
//     //   ////// // System.out.println("calculating totals");
        if (paymentMethod == null) {
            return;
        }

        if (toStaff != null) {
            paymentScheme = null;
            creditCompany = null;
        }

        double billDiscount = 0.0;
        double billGross = 0.0;
        double billNet = 0.0;
        double billVat = 0.0;

//        MembershipScheme membershipScheme = membershipSchemeController.fetchPatientMembershipScheme(getSearchedPatient(), getSessionController().getApplicationPreference().isMembershipExpires());
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

                //Payment  Scheme && Credit Company
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

        if (getSessionController().getApplicationPreference().isPartialPaymentOfOpdBillsAllowed()) {
            ////// // System.out.println("cashPaid = " + cashPaid);
            ////// // System.out.println("billNet = " + billNet);
            if (cashPaid >= (billNet + billVat)) {
                ////// // System.out.println("fully paid = ");
                setDiscount(billDiscount);
                setTotal(billGross);
                setNetTotal(billNet + billVat);
                setCashBalance(cashPaid - (billNet + billVat) - billDiscount);
                ////// // System.out.println("cashBalance = " + cashBalance);
            } else {
                ////// // System.out.println("half paid = ");
                setDiscount(billDiscount);
                setTotal(billGross);
                setNetTotal(cashPaid);
                setCashBalance((billNet + billVat) - cashPaid - billDiscount);
                ////// // System.out.println("cashBalance = " + cashBalance);
            }
            cashRemain = cashPaid;
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

    public void prepareNewBill() {
        clearBillItemValues();
        clearBillValues();
        setPrintPreview(true);
        printPreview = false;
        paymentMethodData = null;
        paymentScheme = null;

        paymentMethod = PaymentMethod.Cash;
        collectingCentreBillController.setCollectingCentre(null);
    }

    public String toOpdBilling() {
        return "/opd/opd_bill";
    }

    public String prepareNewBillForMember(FamilyMember familyMember) {
        clearBillItemValues();
        clearBillValuesForMember();
        setPrintPreview(true);
        printPreview = false;
        paymentMethodData = null;
        paymentMethod = PaymentMethod.Cash;

        collectingCentreBillController.setCollectingCentre(null);
        return "/opd/opd_bill?faces-redirect=true;";
    }

    public void makeNull() {
        clearBillItemValues();
        clearBillValues();
        paymentMethod = null;
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
                setSearchedPatient(null);
            }
        }
        calTotals();
    }

    public void createPaymentsForBills(Bill b, List<BillEntry> billEntrys) {
        Payment p = createPayment(b, b.getPaymentMethod());
        createBillFeePaymentsByPaymentsAndBillEntry(p, billEntrys);
    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment createdPayment = null;
        if (bill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return createdPayment;
        }
        if (bill.getId() == null) {
            JsfUtil.addErrorMessage("No Saved Bill");
            return createdPayment;
        }
        if (pm == null) {
            JsfUtil.addErrorMessage("No payment Method");
            return createdPayment;
        }
        if (bill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Atomic Bill Type");
            return createdPayment;
        }
        switch (bill.getBillTypeAtomic().getBillFinanceType()) {
            case CASH_IN:
                createdPayment = createPaymentForCashInBills(bill, pm);
                break;
            case CASH_OUT:
                createdPayment = createPaymentForCashOutBills(bill, pm);
                break;
            case NO_FINANCE_TRANSACTIONS:
                createdPayment = createPaymentForNoCashBills(bill, pm);
                break;
            default:
                throw new AssertionError();
        }
        return createdPayment;
    }

    public Payment createPaymentForCashOutBills(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        p.setPaidValue(0 - Math.abs(bill.getNetTotal()));
        setPaymentMethodData(p, pm);
        return p;
    }

    public Payment createPaymentForNoCashBills(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        p.setPaidValue(0);
        setPaymentMethodData(p, pm);
        return p;
    }

    public Payment createPaymentForCashInBills(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        p.setPaidValue(0 + Math.abs(bill.getNetTotal()));
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

    public String findBatchBillSessionID(String deptID) {
        String jpql = "select b from Bill b "
                + "where b.billType=:bt "
                + " and type(b)=:type "
                + " and b.deptId=:id"
                + " and b.retired=:ret"
                + " and b.deptId is not null "
                + " and b.cancelled=false";

        Map m = new HashMap();
        m.put("bt", BillType.OpdBathcBillPre);
        m.put("id", deptID);
        m.put("type", PreBill.class);
        m.put("ret", false);

        Bill b = billFacade.findFirstByJpql(jpql, m);
        //System.out.println("b = " + b);
        //System.out.println("b.getSessionId() = " + b.getSessionId());
        if ((b.getSessionId() == null) || ("".equals(b.getSessionId().trim()))) {
            return "*0*";
        } else {
            return b.getSessionId();
        }

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

    public BillController() {
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

    private Patient getTmpPatient() {
        return tmpPatient;
    }

    public void setTmpPatient(Patient tmpPatient) {
        this.tmpPatient = tmpPatient;
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

    public String navigateToBillContactNumbers() {
        return "/admin/bill_contact_numbers.xhtml";
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public OpdBillController getOpdBillController() {
        return opdBillController;
    }

    public void setOpdBillController(OpdBillController opdBillController) {
        this.opdBillController = opdBillController;
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
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

            }
            remainAmount = total - multiplePaymentMethodTotalValue;
            return total - multiplePaymentMethodTotalValue;

        }
        remainAmount = total;
        return total;
    }

    @Override
    public void recieveRemainAmountAutomatically() {
        //double remainAmount = calculatRemainForMultiplePaymentTotal();
        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);
            switch (pm.getPaymentMethod()) {
                case Cash:
                    pm.getPaymentMethodData().getCash().setTotalValue(remainAmount);
                    break;
                case Card:
                    pm.getPaymentMethodData().getCreditCard().setTotalValue(remainAmount);
                    break;
                case Cheque:
                    pm.getPaymentMethodData().getCheque().setTotalValue(remainAmount);
                    break;
                case Slip:
                    pm.getPaymentMethodData().getSlip().setTotalValue(remainAmount);
                    break;
                case ewallet:
                    pm.getPaymentMethodData().getEwallet().setTotalValue(remainAmount);
                    break;
                case PatientDeposit:
                    if (patient != null) {
                        pm.getPaymentMethodData().getPatient_deposit().setPatient(patient);
                    }
                    pm.getPaymentMethodData().getPatient_deposit().setTotalValue(remainAmount);
                    break;
                case Credit:
                    pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
                    break;
                case Staff:
                    pm.getPaymentMethodData().getStaffCredit().setTotalValue(remainAmount);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected value: " + pm.getPaymentMethod());
            }

        }
    }

    public double getRemainAmount() {
        return remainAmount;
    }

    public void setRemainAmount(double remainAmount) {
        this.remainAmount = remainAmount;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public boolean isBatchBillCancellationStarted() {
        return batchBillCancellationStarted;
    }

    public void setBatchBillCancellationStarted(boolean batchBillCancellationStarted) {
        this.batchBillCancellationStarted = batchBillCancellationStarted;
    }

    /**
     *
     */
    @FacesConverter(forClass = Bill.class)
    public static class BillConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            BillController controller = (BillController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "billController");
            return controller.getBillFacade().find(getKey(value));
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
            if (object instanceof Bill) {
                Bill o = (Bill) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + BillController.class.getName());
            }
        }
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public double getNetPlusVat() {
        return netPlusVat;
    }

    public void setNetPlusVat(double netPlusVat) {
        this.netPlusVat = netPlusVat;
    }

}
