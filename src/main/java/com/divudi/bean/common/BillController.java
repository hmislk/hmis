package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.collectingCentre.CollectingCentreBillController;
import com.divudi.bean.lab.LabTestHistoryController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.bean.membership.MembershipSchemeController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.data.dataStructure.BillListWithTotals;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.ejb.BillEjb;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.service.StaffService;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillEntry;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillSession;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
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
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.opd.OpdBillController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.entity.FamilyMember;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.Request;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.light.common.BillLight;
import com.divudi.service.BillService;
import com.divudi.service.PaymentService;
import com.divudi.service.ProfessionalPaymentService;
import com.divudi.service.RequestService;
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
    PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    PatientInvestigationFacade patientInvestigationFacade;
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
    BillService billService;
    @EJB
    StaffService staffService;
    @EJB
    PaymentService paymentService;
    @EJB
    ProfessionalPaymentService professionalPaymentService;
    /**
     * Controllers
     */
    @Inject
    SessionController sessionController;
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
    @Inject
    private BillSearch billSearch;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    PatientInvestigationController patientInvestigationController;
    @Inject
    LabTestHistoryController labTestHistoryController;

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
    @EJB
    private StaffService staffBean;

    @Inject
    SearchController searchController;

    private Long billIdToAssignBillItems;
    private String output;

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
        Map params = new HashMap();
        params.put("ret", false);
        params.put("bf", bf);
        BillFee rbf = billFeeFacade.findFirstByJpql(jpql, params);
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
            JsfUtil.addSuccessMessage("Saved");
        } else {
            getFacade().edit(savingBill);
            JsfUtil.addSuccessMessage("Updated");
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

    public void fixReferancesInPharmacyGrnReturns() {
        output = ""; // Reset log
        String jpql = "SELECT b FROM Bill b "
                + "WHERE b.retired = :ret "
                + "AND b.createdAt BETWEEN :fd AND :td "
                + "AND b.billTypeAtomic IN :btas";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("fd", getFromDate());
        m.put("td", getToDate());

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.PHARMACY_GRN_RETURN);
        btas.add(BillTypeAtomic.PHARMACY_GRN_REFUND);
        m.put("btas", btas);

        List<Bill> allGrnReturns = billFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        int fixedCount = 0;

        for (Bill grnReturnBill : allGrnReturns) {
            Bill grnBill = grnReturnBill.getBilledBill();

            if (grnBill == null) {
                output += "Skipped: GRN Return #" + grnReturnBill.getDeptId() + " has no billed bill.\n";
                continue;
            }

            if (grnBill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_GRN) {
                output += "Skipped: GRN Return #" + grnReturnBill.getDeptId() + " not linked to PHARMACY_GRN.\n";
                continue;
            }

            Bill wronglyAssignedReferenceBill = grnBill.getReferenceBill();
            if (wronglyAssignedReferenceBill == null) {
                output += "Skipped: GRN #" + grnBill.getDeptId() + " has no reference bill.\n";
                continue;
            }

            if (wronglyAssignedReferenceBill.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_ORDER) {
                output += "Already correct: GRN #" + grnBill.getDeptId() + " references PO correctly.\n";
                continue;
            }

            billService.reloadBill(grnReturnBill);
            List<BillItem> returnItems = grnReturnBill.getBillItems();

            if (returnItems == null || returnItems.isEmpty()) {
                output += "Skipped: GRN Return #" + grnReturnBill.getDeptId() + " has no bill items.\n";
                continue;
            }

            Bill correctReferenceOfPoBill = null;
            for (BillItem returnItem : returnItems) {
                BillItem refItem = returnItem.getReferanceBillItem();
                if (refItem == null || refItem.getBill() == null) {
                    continue;
                }
                if (refItem.getBill().getBillTypeAtomic() == BillTypeAtomic.PHARMACY_GRN) {
                    correctReferenceOfPoBill = refItem.getBill();
                    break;
                }
            }

            if (correctReferenceOfPoBill != null) {
                grnReturnBill.setReferenceBill(correctReferenceOfPoBill);
                billFacade.edit(grnReturnBill);

                grnBill.setReferenceBill(correctReferenceOfPoBill);
                billFacade.edit(grnBill);

                output += "Fixed: GRN Return #" + grnReturnBill.getDeptId() + " reference updated to PO #" + correctReferenceOfPoBill.getDeptId() + ".\n";
                output += "Fixed: GRN #" + grnBill.getDeptId() + " reference updated to PO #" + correctReferenceOfPoBill.getDeptId() + ".\n";
                fixedCount++;
            } else {
                output += "Failed to find correct PO for GRN Return #" + grnReturnBill.getDeptId() + ".\n";
            }
        }

        output += "\nCorrection completed. Total GRN Returns fixed: " + fixedCount + ".";
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
                    + " where ((c.balance IS NOT NULL AND abs(c.balance) > :val) "
                    + " OR (c.balance IS NULL AND (abs(c.netTotal) + abs(c.vat) - abs(c.paidAmount)) > :val)) "
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
            btas.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
            btas.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
            btas.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            hash.put("btas", btas);
            hash.put("pm", PaymentMethod.Credit);
            hash.put("val", 1.0);
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
                    + " where ((c.balance IS NOT NULL AND abs(c.balance) > :val) "
                    + " OR (c.balance IS NULL AND (abs(c.netTotal) + abs(c.vat) - abs(c.paidAmount)) > :val)) "
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
            btas.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            btas.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
            btas.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            params.put("btas", btas);
            params.put("pm", PaymentMethod.Credit);
            params.put("val", 1.0);
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

    public List<Bill> completeInwardCreditCompanyPaymentBill(String qry) {
        System.out.println("completeInwardCreditCompanyPaymentBill");
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
            btas.add(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
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
                    + " where ((b.balance IS NOT NULL AND abs(b.balance) > :val) "
                    + " OR (b.balance IS NULL AND (abs(b.netTotal) + abs(b.vat) - abs(b.paidAmount)) > :val)) "
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
            hash.put("btps", Arrays.asList(new BillType[]{BillType.PharmacyWholeSale, BillType.PharmacySale, BillType.PharmacyPre}));
            hash.put("pm", PaymentMethod.Credit);
            hash.put("val", 1.0);
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
        sql += " or  (b.procedure is not null and b.procedure.item is not null and ((b.procedure.item.name) like :q) )";
        sql += " or  ((b.patientEncounter.patient.phn) like :q ))";
        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.SurgeryBill);
        temMap.put("q","%" + qry.toUpperCase() + "%");

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
        AuditEvent auditEvent = sessionController.createNewAuditEvent("getOpdBills()", "Started");
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
        sessionController.completeAuditEvent(auditEvent);
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

    public boolean checkCancelBill(Bill originalBill) {
        List<PatientInvestigationStatus> availableStatus = enumController.getAvailableStatusforCancel();
        boolean canCancelBill = false;
        if (availableStatus.contains(originalBill.getStatus())) {
            canCancelBill = true;
        }
        return canCancelBill;
    }

    @Inject
    RequestController requestController;
    @Inject
    RequestService requestService;

    private Request currentRequest;

    public String navigateToCancelOpdBatchBill() {
        if (batchBill == null) {
            JsfUtil.addErrorMessage("No Batch bill is selected");
            return "";
        }

        if (configOptionApplicationController.getBooleanValueByKey("Mandatory permission to cancel bills.", false)) {
            currentRequest = requestService.findRequest(batchBill);

            if (currentRequest == null) {
                return requestController.navigateToCreateRequest(batchBill);
            } else {
                switch (currentRequest.getStatus()) {
                    case PENDING:
                        requestController.setCurrentRequest(currentRequest);
                        return "/common/request/request_status?faces-redirect=true";
                    case UNDER_REVIEW:
                        requestController.setCurrentRequest(currentRequest);
                        return "/common/request/request_status?faces-redirect=true";
                    case APPROVED:

                        bills = billsOfBatchBill(batchBill);
                        paymentMethod = null;
                        patient = batchBill.getPatient();
                        paymentMethods = billService.availablePaymentMethodsForCancellation(batchBill);
                        comment = currentRequest.getRequestReason();
                        printPreview = false;
                        batchBillCancellationStarted = false;

                        return "/opd/batch_bill_cancel?faces-redirect=true";
                    default:
                        return "";
                }
            }
        } else {
            bills = billsOfBatchBill(batchBill);
            paymentMethod = null;
            patient = batchBill.getPatient();
            paymentMethods = billService.availablePaymentMethodsForCancellation(batchBill);
            comment = null;
            printPreview = false;
            batchBillCancellationStarted = false;
            return "/opd/batch_bill_cancel?faces-redirect=true";
        }
    }

    private List<Bill> cancelSingleBills = new ArrayList<>();

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

        if (getBatchBill().getPaymentMethod() == PaymentMethod.Credit) {
            List<BillItem> items = billService.checkCreditBillPaymentReciveFromCreditCompany(getBatchBill());

            if (items != null && !items.isEmpty()) {
                batchBillCancellationStarted = false;
                JsfUtil.addErrorMessage("This bill has been paid for by the credit company. Therefore, it cannot be canceled.");
                return "";
            }
        }

        if (!configOptionApplicationController.getBooleanValueByKey("Enable the Special Privilege of Canceling OPD Bills", false)) {
            for (Bill bill : billBean.validBillsOfBatchBill(getBatchBill())) {
                if (!checkCancelBill(bill)) {
                    JsfUtil.addErrorMessage("This bill is processed in the Laboratory.");
                    if (getWebUserController().hasPrivilege("BillCancel")) {
                        JsfUtil.addErrorMessage("You have Special privilege to cancel This Bill");
                    } else {
                        JsfUtil.addErrorMessage("You have no Privilege to Cancel OPD Bills. Please Contact System Administrator.");
                        batchBillCancellationStarted = false;
                        return "";
                    }
                } else {
                    if (!getWebUserController().hasPrivilege("OpdCancel")) {
                        JsfUtil.addErrorMessage("You have no Privilege to Cancel OPD Bills. Please Contact System Administrator.");
                        batchBillCancellationStarted = false;
                        return "";
                    }
                }
            }
        } else {
            if (!getWebUserController().hasPrivilege("OpdCancel")) {
                JsfUtil.addErrorMessage("You have no Privilege to Cancel OPD Bills. Please Contact System Administrator.");
                batchBillCancellationStarted = false;
                return "";
            }
        }

        if (errorsPresentOnOpdBatchBillCancellation()) {
            batchBillCancellationStarted = false;
            return "";
        }
        if (professionalPaymentService.isProfessionalFeePaidForBatchBill(batchBill)) {
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
        cancellationBatchBill.setReferenceBill(batchBill);
        cancellationBatchBill.setComments(comment);
        if (paymentMethod != null) {
            cancellationBatchBill.setPaymentMethod(paymentMethod);
        }
        cancellationBatchBill.setBilledBill(batchBill);
        getBillFacade().create(cancellationBatchBill);

        batchBill.setCancelled(true);
        batchBill.setCancelledBill(cancellationBatchBill);
        getBillFacade().edit(batchBill);

        bills = billService.fetchIndividualBillsOfBatchBill(batchBill);

        cancelSingleBills = new ArrayList();

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
        } else if (cancellationBatchBill.getPaymentMethod() == PaymentMethod.Staff_Welfare) {
            if (cancellationBatchBill.getToStaff() != null) {
                staffService.updateStaffWelfare(cancellationBatchBill.getToStaff(), 0 - Math.abs(cancellationBatchBill.getNetTotal() + getBill().getVat()));
                JsfUtil.addSuccessMessage("Staff Welfare Updated");
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

        opdBillController.setBills(cancelSingleBills);
        opdBillController.setBatchBill(cancellationBatchBill);
        getSessionController().setLoggedUser(wb);

        if (configOptionApplicationController.getBooleanValueByKey("Mandatory permission to cancel bills.", false)) {
            Request billRequest = requestService.findRequest(batchBill);
            if (billRequest != null) {
                requestController.setBills(bills);
                requestController.complteRequest(billRequest);
            } else {
                JsfUtil.addErrorMessage("Related approval request not found to complete.");
            }
        }

        printPreview = true;
        batchBillCancellationStarted = false;
        return "/opd/opd_cancel_batch_bill_print?faces-redirect=true";
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

    public Bill getBillById(Long id) {
        if (id == null) {
            return null;
        }
        return billFacade.find(id);
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
        individualCancelltionBill.setReferenceBill(originalBill);
        individualCancelltionBill.setComments(comment);
        billService.saveBill(individualCancelltionBill);
        cancelSingleBills.add(individualCancelltionBill);

        List<BillItem> list = createBillItemsForOpdBatchBillCancellation(originalBill, individualCancelltionBill);

        try {
            if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
                for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsFromBill(originalBill)) {
                    labTestHistoryController.addCancelHistory(pi, sessionController.getDepartment(), comment);
                }
            }
        } catch (Exception e) {
            System.out.println("Error = " + e);
        }

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
                    p.setReferancePayment(originalBillPayment);
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
                        p.setComments(cd.getPaymentMethodData().getCreditCard().getComment());
                        break;
                    case Cheque:
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
                        p.setPaidValue(cd.getPaymentMethodData().getEwallet().getTotalValue());
                        p.setComments(cd.getPaymentMethodData().getEwallet().getComment());
                        break;
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
                    p.setComments(getPaymentMethodData().getCreditCard().getComment());
                    break;
                case Cheque:
                    p.setChequeDate(getPaymentMethodData().getCheque().getDate());
                    p.setChequeRefNo(getPaymentMethodData().getCheque().getNo());
                    p.setComments(getPaymentMethodData().getCheque().getComment());
                    break;
                case Cash:
                    p.setComments(getPaymentMethodData().getCash().getComment());
                    break;
                case ewallet:
                    p.setComments(getPaymentMethodData().getEwallet().getComment());
                    break;

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
            billItemFacade.create(newBillItem);

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
        getNewPatient().getPerson().setDob(CommonFunctions.guessDob(yearMonthDay));

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
            if (getNewPatient().getPerson().getName() == null || getNewPatient().getPerson().getName().trim().isEmpty() || getNewPatient().getPerson().getSex() == null || getNewPatient().getPerson().getDob() == null) {
                JsfUtil.addErrorMessage("Can not bill without Patient Name, Age or Sex.");
                return true;
            }
            if (!Person.checkAgeSex(getNewPatient().getPerson().getDob(), getNewPatient().getPerson().getSex(), getNewPatient().getPerson().getTitle())) {
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
        sql = "select new com.divudi.core.light.common.BillLight(bill.id, bill.insId, bill.createdAt, bill.institution.name, bill.toDepartment.name, bill.creater.name, bill.patient.person.name, bill.patient.person.phone, bill.total, bill.discount, bill.netTotal, bill.patient.id) "
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
        sql = "select new com.divudi.core.light.common.BillLight(bill.id, bill.insId, bill.createdAt, bill.institution.name, bill.toDepartment.name, bill.creater.name, bill.patient.person.name, bill.patient.person.phone, bill.total, bill.discount, bill.netTotal, bill.patient.id) "
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
        return findUnpaidBills(frmDate, toDate, billTypes, pm, balanceGraterThan, null);
    }

    public List<Bill> findUnpaidBills(Date frmDate, Date toDate, List<BillTypeAtomic> billTypes,
            PaymentMethod pm, Double balanceGraterThan, Boolean omitPaymentGeneratedBills) {
        String jpql = "SELECT b FROM Bill b WHERE b.retired = :ret "
                + " AND b.cancelled = :can " //Cancelled bills are no longer listed here
                + " AND b.createdAt BETWEEN :frm AND :to";

        HashMap<String, Object> params = new HashMap<>();
        params.put("frm", frmDate);
        params.put("to", toDate);
        params.put("ret", false);
        params.put("can", false);

        // Add filters dynamically
        if (balanceGraterThan != null) {
            jpql += " AND ABS(b.balance) > :val";
            params.put("val", balanceGraterThan);
        }
        if (pm != null) {
            jpql += " AND b.paymentMethod = :pm";
            params.put("pm", pm);
        }
        if (billTypes != null && !billTypes.isEmpty()) {
            jpql += " AND (b.billTypeAtomic IN :bts)";
            params.put("bts", billTypes);
        }
        if (omitPaymentGeneratedBills != null && omitPaymentGeneratedBills) {
            jpql += " AND (b.paymentGenerated = false OR b.paymentGenerated = 0 OR b.paymentGenerated IS NULL)";
        }

        // Logging
        System.out.println("JPQL Query: " + jpql);
        System.out.println("Parameters: " + params);

        return getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public List<Bill> findPaidBills(Date frmDate, Date toDate, List<BillTypeAtomic> billTypes, PaymentMethod pm, Double balanceGraterThan) {
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
            jpql += " and (abs(b.balance) < :val) ";
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

    /**
     * Entry point method to fix totals in
     * PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS bills. This is a temporary
     * data correction method and should be removed after affected bills are
     * corrected.
     *
     * ChatGPT contributed - 2025-04
     */
    public void fixTotalInPHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS() {
        fixTotalsInBillsByAtomicType(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
    }

    /**
     * Entry point method to fix totals in
     * PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY bills. This is a temporary data
     * correction method and should be removed after affected bills are
     * corrected.
     *
     * ChatGPT contributed - 2025-04
     */
    public void fixTotalInPHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY() {
        fixTotalsInBillsByAtomicType(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);
    }

    /**
     * Recalculates and fixes the gross total in bills of the given atomic bill
     * type. This method updates BillItem gross values (grossRate * quantity)
     * and reassigns the bill's total only if net total is already accurate
     * (difference < 1).
     *
     * This is a temporary cleanup utility and should be removed once all
     * corrupted data is corrected.
     *
     * @param billTypeAtomic the atomic bill type to filter and process
     */
    public void fixTotalsInBillsByAtomicType(BillTypeAtomic billTypeAtomic) {
        System.out.println("Starting temporary fix for bill type: " + billTypeAtomic);

        String jpql = "SELECT b FROM Bill b WHERE b.retired = false AND b.billTypeAtomic = :bta";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", billTypeAtomic);

        List<Bill> bills = getFacade().findByJpql(jpql, params, 1000);

        if (bills == null || bills.isEmpty()) {
            System.out.println("No matching bills found for type: " + billTypeAtomic);
            return;
        }

        System.out.println("Found " + bills.size() + " bills to process.");

        for (Bill b : bills) {
            System.out.println("Processing bill ID: " + b.getId());

            double total = 0.0;
            double netTotal = 0.0;

            for (BillItem bi : b.getBillItems()) {
                double grossRate = bi.getRate(); // 'rate' field stores gross rate
                double qty = bi.getQty();
                double grossValue = grossRate * qty;
                bi.setGrossValue(grossValue);
                billItemFacade.edit(bi);

                total += grossValue;
                netTotal += bi.getNetValue();
            }

            System.out.println("Calculated Gross Total: " + total + " | Existing: " + b.getTotal());
            System.out.println("Calculated Net Total: " + netTotal + " | Existing: " + b.getNetTotal());

            if (Math.abs(b.getNetTotal() - netTotal) >= 1.0) {
                System.out.println("Skipping bill ID: " + b.getId() + ". Net total mismatch suggests deeper issue.");
            } else {
                System.out.println("Updating gross total for bill ID: " + b.getId());
                b.setTotal(total);
                getFacade().edit(b);
            }
        }

        System.out.println("Completed processing for bill type: " + billTypeAtomic);
    }

    /**
     * Temporary admin method to correct BillItem rate, netRate, grossValue, and
     * netValue for PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY bills. These values
     * should be negative to reflect a return transaction.
     *
     * Note: This method does not alter Bill-level totals as net total is
     * already negative.
     *
     * ChatGPT contributed - 2025-04
     */
    public void correctBillItemRatesInPHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY() {
        System.out.println("Starting BillItem correction for PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY");

        String jpql = "SELECT b FROM Bill b WHERE b.retired = false AND b.billTypeAtomic = :bta";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);

        List<Bill> bills = getFacade().findByJpql(jpql, params, 1000);
        if (bills == null || bills.isEmpty()) {
            System.out.println("No matching bills found.");
            return;
        }

        System.out.println("Number of bills found: " + bills.size());

        for (Bill bill : bills) {
            System.out.println("Processing bill ID: " + bill.getId());
            for (BillItem bi : bill.getBillItems()) {
                boolean updated = false;

                if (bi.getRate() > 0) {
                    bi.setRate(-bi.getRate());
                    updated = true;
                }

                if (bi.getNetRate() > 0) {
                    bi.setNetRate(-bi.getNetRate());
                    updated = true;
                }

                if (bi.getGrossValue() > 0) {
                    bi.setGrossValue(-bi.getGrossValue());
                    updated = true;
                }

                if (bi.getNetValue() > 0) {
                    bi.setNetValue(-bi.getNetValue());
                    updated = true;
                }

                if (updated) {
                    billItemFacade.edit(bi);
                    System.out.println("Updated BillItem ID: " + bi.getId());
                } else {
                    System.out.println("No update needed for BillItem ID: " + bi.getId());
                }
            }
        }

        System.out.println("Completed correction of BillItem values.");
    }

    /**
     * Temporary admin method to correct BillItem rate, netRate, grossValue, and
     * netValue for DIRECT_ISSUE_INWARD_MEDICINE_RETURN bills. These values
     * should be negative to reflect a return transaction.
     *
     * PharmaceuticalBillItem values (retailRate, retailValue, purchaseRate,
     * purchaseValue) are also updated to ensure negative direction for returns.
     *
     * Note: This method does not alter Bill-level totals.
     *
     * ChatGPT contributed - 2025-04
     */
    public void correctBillItemRatesInDIRECT_ISSUE_INWARD_MEDICINE_RETURN() {
        System.out.println("Starting BillItem correction for DIRECT_ISSUE_INWARD_MEDICINE_RETURN");

        String jpql = "SELECT b FROM Bill b WHERE b.retired = false AND b.billTypeAtomic = :bta";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);

        List<Bill> bills = getFacade().findByJpql(jpql, params, 1000);
        if (bills == null || bills.isEmpty()) {
            System.out.println("No matching bills found.");
            return;
        }

        System.out.println("Number of bills found: " + bills.size());

        for (Bill bill : bills) {
            System.out.println("Processing bill ID: " + bill.getId());
            for (BillItem bi : bill.getBillItems()) {
                boolean updated = false;

                if (bi.getRate() > 0) {
                    bi.setRate(-bi.getRate());
                    updated = true;
                }

                if (bi.getNetRate() > 0) {
                    bi.setNetRate(-bi.getNetRate());
                    updated = true;
                }

                if (bi.getGrossValue() > 0) {
                    bi.setGrossValue(-bi.getGrossValue());
                    updated = true;
                }

                if (bi.getNetValue() > 0) {
                    bi.setNetValue(-bi.getNetValue());
                    updated = true;
                }

                PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
                if (pbi != null) {
                    boolean pbiUpdated = false;

                    if (pbi.getRetailRate() > 0) {
                        pbi.setRetailRate(-pbi.getRetailRate());
                        pbiUpdated = true;
                    }

                    if (pbi.getRetailValue() > 0) {
                        pbi.setRetailValue(-pbi.getRetailValue());
                        pbiUpdated = true;
                    } else if (pbi.getRetailValue() == 0) {
                        double value = Math.abs(pbi.getRetailRate() * pbi.getQty());
                        pbi.setRetailValue(-value);
                        pbiUpdated = true;
                    }

                    if (pbi.getPurchaseRate() > 0) {
                        pbi.setPurchaseRate(-pbi.getPurchaseRate());
                        pbiUpdated = true;
                    }

                    if (pbi.getPurchaseValue() == 0) {
                        double value = Math.abs(pbi.getPurchaseRate() * pbi.getQty());
                        pbi.setPurchaseValue(-value);
                        pbiUpdated = true;
                    }

                    if (pbiUpdated) {
                        pharmaceuticalBillItemFacade.edit(pbi);
                    }
                }

                if (updated) {
                    billItemFacade.edit(bi);
                    System.out.println("Updated BillItem ID: " + bi.getId());
                } else {
                    System.out.println("No update needed for BillItem ID: " + bi.getId());
                }
            }
        }

        System.out.println("Completed correction of BillItem and PharmaceuticalBillItem values.");
    }

    public void addMissingBillTypeAtomics() {
        System.out.println("addMissingBillTypeAtomics");
        String jpql = "select b "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.billTypeAtomic is null "
                + " and b.billType is not null ";
        Map m = new HashMap();
        m.put("ret", false);
        List<Bill> billsWithoutBillTypeAtomic = getFacade().findByJpql(jpql, m, 1000);
        if (billsWithoutBillTypeAtomic == null) {
            System.out.println("No Bills");
            return;
        }
        System.out.println("billsWithoutBillTypeAtomic = " + billsWithoutBillTypeAtomic.size());
        for (Bill b : billsWithoutBillTypeAtomic) {
            if (b.getBillTypeAtomic() != null) {
                continue;
            }
            b.setBillTypeAtomic(BillTypeAtomic.getBillTypeAtomic(b.getBillType(), b.getBillClassType()));
            getFacade().edit(b);
        }
    }

    /**
     * Administrative method to batch-correct all PharmaceuticalBillItems that
     * were wrongly assigned due to cancellation logic in
     * PHARMACY_RETAIL_SALE_CANCELLED bills.
     *
     * This method scans all non-retired bills of type
     * PHARMACY_RETAIL_SALE_CANCELLED, and for each BillItem in those bills,
     * processes only the second set of PharmaceuticalBillItems, skipping the
     * first set which is known to be incorrect.
     *
     * ChatGPT contributed - 2025-04, updated 2025-04
     */
    public void convertPharmaceuticalBillItemReferanceFromErronouslyRecordedPharmacyRetailSaleCancellationPreBillToPharmacyRetailSalePreBillForAllBills() {
        if (getFromDate() == null || getToDate() == null) {
            System.out.println("FromDate or ToDate is null. Aborting process.");
            return;
        }

        BillTypeAtomic billTypeAtomic = BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED;

        String jpql = "SELECT b FROM Bill b WHERE b.retired = false AND b.billTypeAtomic = :bta "
                + "AND b.createdAt BETWEEN :fd AND :td";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", billTypeAtomic);
        params.put("fd", getFromDate());
        params.put("td", getToDate());

        List<Bill> pharmacyRetailSaleCancellationBills = getFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);

        if (pharmacyRetailSaleCancellationBills == null || pharmacyRetailSaleCancellationBills.isEmpty()) {
            System.out.println("No matching bills found for type: " + billTypeAtomic);
            return;
        }

        System.out.println("Found " + pharmacyRetailSaleCancellationBills.size() + " cancelled bills to process...");

        for (Bill cancelledBill : pharmacyRetailSaleCancellationBills) {
            if (cancelledBill == null) {
                continue;
            }

            billService.reloadBill(cancelledBill);

            List<PharmaceuticalBillItem> pbis = billService.fetchPharmaceuticalBillItems(cancelledBill);
            List<BillItem> bis = cancelledBill.getBillItems();

            Long billId = cancelledBill.getId() != null ? cancelledBill.getId() : -1L;

            if (pbis == null || pbis.isEmpty()) {
                System.out.println("Cancelled bill ID " + billId + " has no pharmaceutical bill items.");
                continue;
            }

            if (bis == null || bis.isEmpty()) {
                System.out.println("Cancelled bill ID " + billId + " has no bill items.");
                continue;
            }

            int numberOfBillItems = bis.size();
            int numberOfPharmaceuticalBillItems = pbis.size();

            if (numberOfPharmaceuticalBillItems != (numberOfBillItems * 2)) {
                System.out.println("Cancelled bill ID " + billId
                        + " skipped: Expected PBI count = " + (numberOfBillItems * 2)
                        + ", Found = " + numberOfPharmaceuticalBillItems);
                continue;
            }

            // Skip the first half of PharmaceuticalBillItems
            for (int i = numberOfBillItems; i < numberOfPharmaceuticalBillItems; i++) {
                PharmaceuticalBillItem pbi = pbis.get(i);
                if (pbi == null) {
                    continue;
                }

                BillItem bi = pbi.getBillItem();
                if (bi != null && hasMoreThanOnePbi(bi)) {
                    convertPharmaceuticalBillItemReferenceFromErroneouslyRecordedPharmacyRetailSaleCancellationPreBillToPharmacyRetailSalePreBill(pbi);
                }
            }
        }

        System.out.println("Completed batch correction of wrongly assigned PharmaceuticalBillItems.");
    }

    /**
     * Checks whether a BillItem has more than one associated
     * PharmaceuticalBillItem.
     *
     * @param bi The BillItem to check
     * @return true if more than one PharmaceuticalBillItem exists for the given
     * BillItem; false otherwise
     */
    private boolean hasMoreThanOnePbi(BillItem bi) {
        if (bi == null || bi.getId() == null) {
            return false;
        }

        String jpql = "SELECT COUNT(pbi) FROM PharmaceuticalBillItem pbi WHERE pbi.billItem.id = :bid";
        Map<String, Object> params = new HashMap<>();
        params.put("bid", bi.getId());

        Long count = pharmaceuticalBillItemFacade.countByJpql(jpql, params);
        return count != null && count > 1;
    }

    public void createNewBillItemAndAssignPatientInvestigation(PatientInvestigation ptix) {
        if (billIdToAssignBillItems == null) {
            JsfUtil.addErrorMessage("No ID for a Bill to assign newly created Bill Items");
            return;
        }
        if (ptix == null) {
            JsfUtil.addErrorMessage("No patient investigation");
            return;
        }
        if (ptix.getBillItem() == null) {
            JsfUtil.addErrorMessage("No Bill Item for Patient investigation");
            return;
        }
        Bill selectedBillToAssignNewlyCreatedBillItems;
        BillItem originalBillItemToDuplicate = ptix.getBillItem();
        BillItem duplicateBillItem = new BillItem();

        selectedBillToAssignNewlyCreatedBillItems = billService.fetchBillById(billIdToAssignBillItems);
        if (selectedBillToAssignNewlyCreatedBillItems == null) {
            JsfUtil.addErrorMessage("No Bill Found for the ID given for a Bill to assign newly created Bill Items");
            return;
        }

        try {
            duplicateBillItem.copy(originalBillItemToDuplicate);
            duplicateBillItem.setBill(selectedBillToAssignNewlyCreatedBillItems);
            duplicateBillItem.setCreatedAt(new Date());
            duplicateBillItem.setCreater(getSessionController().getLoggedUser());
            billItemFacade.create(duplicateBillItem);

            duplicateBillItem.setPatientInvestigation(ptix);
            billItemFacade.edit(duplicateBillItem);

            ptix.setBillItem(duplicateBillItem);
            patientInvestigationFacade.edit(ptix);

            // Can NOT Recalculate bill totals after adding the new item. Bill Totals are correc. We are fixing Bill Items. Have have to double check bill totlas are equal to the total of the bill item values
            // calTotals();
            JsfUtil.addSuccessMessage("New Bill Item Created and References Assigned");
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error creating bill item: " + e.getMessage());
        }

        billService.createBillItemFeesAndAssignToNewBillItem(originalBillItemToDuplicate, duplicateBillItem);

        JsfUtil.addSuccessMessage("New Bill Item Created and Referances Assigned");
    }

    public void convertPharmaceuticalBillItemReferenceFromErroneouslyRecordedPharmacyRetailSaleCancellationPreBillToPharmacyRetailSalePreBill(PharmaceuticalBillItem pbi) {
        Bill originalBill = null;
        Bill cancelledBill = null;
        BillItem originalBillItemNowWonglyAssignedToCancelledBill = null;
        BillItem billItemNeededToCreateForCancellationBill = null;
        PharmaceuticalBillItem pbiFromOriginalBillItem = null;
        PharmaceuticalBillItem pbiFromCancelledBill = null;

        if (pbi == null) {
            System.out.println("No Selected Pharmaceutical Bill Item - returning");
            return;
        } else {
            pbiFromCancelledBill = pbi;
            System.out.println("Selected Pharmaceutical Bill Item ID: " + pbiFromCancelledBill.getId());
        }

        if (pbiFromCancelledBill.getBillItem() == null) {
            System.out.println("Cancelled BillItem is null - returning");
            return;
        } else {
            originalBillItemNowWonglyAssignedToCancelledBill = pbi.getBillItem();
            System.out.println("Original BillItem ID: " + originalBillItemNowWonglyAssignedToCancelledBill.getId());
        }

        if (originalBillItemNowWonglyAssignedToCancelledBill.getBill() == null) {
            System.out.println("Cancelled Bill is null - returning");
            return;
        } else {
            cancelledBill = pbi.getBillItem().getBill();
            System.out.println("Cancelled Bill ID: " + cancelledBill.getId());
        }

        Long cancelledBillId = originalBillItemNowWonglyAssignedToCancelledBill.getId();
        Long pbiFromCancelledBillId = pbiFromCancelledBill.getId();

        // Sampbple cancelledtion bill id is 542424
        // Sample Correct PBI ID is 542428
        // Sample Wrong PBI ID is 542417
        System.out.println("Cancelled BillItem ID: " + cancelledBillId);
        System.out.println("Selected PBI ID: " + pbiFromCancelledBillId);

        if (pbiFromCancelledBillId < cancelledBillId) {
            System.out.println("Clicking on the wrong Pharmaceutical Bill Item. Select the other one - returning");
            return;
        }

        if (cancelledBill.getBillTypeAtomic() == null) {
            System.out.println("Cancelled BillTypeAtomic is null - returning");
            return;
        }

        if (cancelledBill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED) {
            System.out.println("Cancelled bill type is not PHARMACY_RETAIL_SALE_CANCELLED - returning");
            return;
        }

        if (cancelledBill.getBilledBill() == null) {
            System.out.println("Cancelled bill has no billed bill - returning");
            return;
        } else {
            originalBill = cancelledBill.getBilledBill();
            System.out.println("Original Bill ID: " + originalBill.getId());
        }

        if (originalBillItemNowWonglyAssignedToCancelledBill.getPharmaceuticalBillItem() == null) {
            System.out.println("Original BillItem has no PBI - returning");
            return;
        } else {
            pbiFromOriginalBillItem = originalBillItemNowWonglyAssignedToCancelledBill.getPharmaceuticalBillItem();
            System.out.println("Original PBI ID: " + pbiFromOriginalBillItem.getId());
        }

        System.out.println("Creating new BillItem for cancelled bill");
        billItemNeededToCreateForCancellationBill = new BillItem();
        billItemNeededToCreateForCancellationBill.copy(originalBillItemNowWonglyAssignedToCancelledBill);
        billItemNeededToCreateForCancellationBill.setBill(cancelledBill);
        billItemNeededToCreateForCancellationBill.setReferanceBillItem(originalBillItemNowWonglyAssignedToCancelledBill);
        billItemFacade.create(billItemNeededToCreateForCancellationBill);
        System.out.println("Created new BillItem with ID: " + billItemNeededToCreateForCancellationBill.getId());

        billItemNeededToCreateForCancellationBill.setPharmaceuticalBillItem(null);
        billItemFacade.edit(billItemNeededToCreateForCancellationBill);
        System.out.println("Linked new BillItem to Cancelled PBI ID: " + pbiFromCancelledBill.getId());

        originalBillItemNowWonglyAssignedToCancelledBill.setBill(originalBill);
        originalBillItemNowWonglyAssignedToCancelledBill.setPharmaceuticalBillItem(null);
        billItemFacade.edit(originalBillItemNowWonglyAssignedToCancelledBill);
        System.out.println("Reassigned original BillItem to Original Bill ID: " + originalBill.getId());

        pbiFromCancelledBill.setBillItem(billItemNeededToCreateForCancellationBill);
        pharmaceuticalBillItemFacade.edit(pbiFromCancelledBill);

        pbiFromOriginalBillItem.setBillItem(originalBillItemNowWonglyAssignedToCancelledBill);
        pharmaceuticalBillItemFacade.edit(pbiFromOriginalBillItem);

        billItemNeededToCreateForCancellationBill.setPharmaceuticalBillItem(pbiFromCancelledBill);
        billItemFacade.edit(billItemNeededToCreateForCancellationBill);

        originalBillItemNowWonglyAssignedToCancelledBill.setPharmaceuticalBillItem(pbiFromOriginalBillItem);
        billItemFacade.edit(originalBillItemNowWonglyAssignedToCancelledBill);

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
        return "/opd/opd_bill?faces-redirect=true";
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

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
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
        String jpql = "select sum(bfp.amount) "
                + " from BillFeePayment bfp "
                + " where bfp.retired = :ret "
                + " and bfp.billFee.bill.id = :bid";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("bid", b.getId());
        double d = getBillFeePaymentFacade().findDoubleByJpql(jpql, params);
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
                case OnlineSettlement:
                    pm.getPaymentMethodData().getOnlineSettlement().setTotalValue(remainAmount);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected value: " + pm.getPaymentMethod());
            }

        }
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

    public Long getBillIdToAssignBillItems() {
        return billIdToAssignBillItems;
    }

    public void setBillIdToAssignBillItems(Long billIdToAssignBillItems) {
        this.billIdToAssignBillItems = billIdToAssignBillItems;
    }

    public List<Bill> getCancelSingleBills() {
        if (cancelSingleBills == null) {
            cancelSingleBills = new ArrayList<>();
        }
        return cancelSingleBills;
    }

    public void setCancelSingleBills(List<Bill> cancelSingleBills) {
        this.cancelSingleBills = cancelSingleBills;
    }

    public Request getCurrentRequest() {
        return currentRequest;
    }

    public void setCurrentRequest(Request currentRequest) {
        this.currentRequest = currentRequest;
    }

    /**
     *
     */
    @FacesConverter(forClass = Bill.class)
    public static class BillConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
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

    public double getNetPlusVat() {
        return netPlusVat;
    }

    public void setNetPlusVat(double netPlusVat) {
        this.netPlusVat = netPlusVat;
    }

    public String navigateToBillById(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("Bill ID is required");
            return null;
        }

        Bill foundBill = billFacade.find(billId);
        if (foundBill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }

        return billSearch.navigateToViewBillByAtomicBillTypeByBillId(billId);
    }

}
