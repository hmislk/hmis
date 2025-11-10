package com.divudi.bean.common;

import com.divudi.core.util.JsfUtil;
import com.divudi.bean.inward.AdmissionTypeController;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.FeeType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.QuickBookFormat;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.core.data.inward.AdmissionTypeEnum;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.data.table.String3Value2;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PatientRoomFacade;
import com.divudi.core.util.CommonFunctions;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Dushan
 */
@Named
@SessionScoped
public class QuickBookReportController implements Serializable {

    @EJB
    private CategoryFacade categoryFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private PatientRoomFacade patientRoomFacade;

    @Inject
    private BillBeanController billBeanController;
    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private AdmissionTypeController admissionTypeController;
    @Inject
    private InwardBeanController inwardBeanController;

    private List<QuickBookFormat> quickBookFormats;
    private List<Category> categorys;
    private List<Institution> creditCompanies;
    private List<Item> items;

    private Institution institution;
    private Department department;
    private Institution site;
    private Date toDate;
    private Date fromDate;
    private String report;
    private ReportKeyWord reportKeyWord;
    private double grantTot;

    private boolean withProfessionalFee;

    public QuickBookReportController() {
    }

    public String navigateToDailyReturnImportForQbReport() {
        report = "1";
        return "/reports/qb/report_qb_day_income_with_out_professional?faces-redirect=true";
    }

    public void createQBFormat() {
        if (errorCheck()) {
            return;
        }
        System.out.println("report = " + report);
        switch (report) {
            case "1":
                createQBFormatOpdDayIncome();
                break;
            case "2":
                createQBFormatPharmacyGRNAndPurchaseBills();
                break;
            case "3":
                createQBFormatStoreGRNAndPurchaseBills();
                break;
            case "4":
                createQBFormatInwardIncome();
                break;
            case "5":
                createQBFormatOpdDayCredit();
                break;
            case "6":
                createQBFormatPharmacyCredit();
                break;
            case "7":
                createQBFormatCafeRGRNAndPurchaseBills();
                break;
            case "8":
                createQBFormatOpdDayIncomeByDate();
                break;
            default:
                throw new AssertionError();
        }
    }

    public void createAllBilledItemReport() {
        items = fetchBilledItem(BillType.OpdBill, fromDate, toDate, true);
        for (Item i : items) {
            System.out.println("i.getName() = " + i.getName());
            System.out.println("i.getId() = " + i.getId());
            if (i.getName().length() > 30) {
                i.setTransName(i.getName().substring(0, 30));
            } else {
                i.setTransName(i.getName());
            }
            if (Investigation.class == i.getClass()) {
                i.getCategory().setDescription("RHD LAB INCOME:OPD:RHD OPD Sale");
            }
        }
        List<Item> is = fetchBilledItem(BillType.InwardBill, fromDate, toDate, false);
        for (Item i : is) {
            System.out.println("i.getName() = " + i.getName());
            System.out.println("i.getId() = " + i.getId());
            if (i.getName().length() > 30) {
                i.setTransName(i.getName().substring(0, 30));
            } else {
                i.setTransName(i.getName());
            }
            if (Investigation.class == i.getClass()) {
                i.getCategory().setDescription("RHD LAB INCOME:INWARD:RHD Inward Sale");
            }
        }
        items.addAll(is);

    }

    public List<Item> fetchBilledItem(List<BillType> billTypes, Date fd, Date td) {
        String sql;
        Map m = new HashMap();

        sql = "select distinct(bi.item) FROM BillItem bi "
                + " where bi.retired=false ";

        if (!billTypes.isEmpty()) {
            sql += " and  bi.bill.billType in :bts ";
            m.put("bts", billTypes);
        }

        sql += " and ((bi.bill.createdAt between :fromDate and :toDate) "
                + " or (bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate)) "
                + " order by bi.item.category.name, bi.item.name ";
        m.put("toDate", td);
        m.put("fromDate", fd);

        List<Item> tmp = getItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        System.out.println("tmp.size() = " + tmp.size());

        return tmp;

    }

    public List<Item> fetchBilledItem(BillType billType, Date fd, Date td, boolean opd) {
        String sql;
        Map m = new HashMap();

        sql = "select distinct(bi.item) FROM BillItem bi "
                + " where bi.retired=false ";

        if (billType != null) {
            sql += " and  bi.bill.billType=:bt ";
            m.put("bt", billType);
        }

        if (opd) {
            sql += " and bi.bill.createdAt between :fromDate and :toDate";
        } else {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        }

        sql += " order by bi.item.category.name, bi.item.name ";
        m.put("toDate", td);
        m.put("fromDate", fd);

        List<Item> tmp = getItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        System.out.println("tmp.size() = " + tmp.size());

        return tmp;

    }

    public List<Item> fetchBilledItemInwardInvestigationOnly(List<BillType> billTypes, Date fd, Date td) {
        String sql;
        Map m = new HashMap();

        sql = "select distinct(bi.item) FROM BillItem bi "
                + " where bi.retired=false "
                + " and bi.item.inactive!=true "
                + " and type(bi.item)=:typ ";

        if (!billTypes.isEmpty()) {
            sql += " and  bi.bill.billType in :bts ";
            m.put("bts", billTypes);
        }

        sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate "
                + " order by bi.item.category.name, bi.item.name ";
        m.put("toDate", td);
        m.put("fromDate", fd);
        m.put("typ", Investigation.class);

        List<Item> tmp = getItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        System.out.println("tmp.size() = " + tmp.size());

        return tmp;

    }

    //-------Main Functions
    public void createQBFormatOpdDayIncome() {

        grantTot = 0.0;
        quickBookFormats = new ArrayList<>();
        List<QuickBookFormat> qbfs = new ArrayList<>();

        List<PaymentMethod> paymentMethods = Arrays.asList(PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card, PaymentMethod.OnlineSettlement, PaymentMethod.MultiplePaymentMethods);

        if (withProfessionalFee) {
            qbfs.addAll(fetchOPdListDayEndTable(paymentMethods, CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate), null));
            qbfs.addAll(fetchOPdListProfessionalFeeDayEndTable(paymentMethods, CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate), null));
        } else {
            qbfs.addAll(fetchOPdListDayEndTable(paymentMethods, CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate), null));
        }

        qbfs.addAll(createPharmacySale(BillType.PharmacySale, CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
        qbfs.addAll(createInwardCollection(CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
        qbfs.addAll(createAgencyAndCollectionCenterTotal(CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));

        QuickBookFormat qbf = new QuickBookFormat();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");

        qbf.setRowType("TRNS");
        qbf.setTrnsType("Cash Sale");
        qbf.setDate(sdf.format(fromDate));
        qbf.setAccnt("Cash AR");
        qbf.setName("Cash AR");
        qbf.setAmount(grantTot);
        qbf.setMemo("Sales");
        sdf = new SimpleDateFormat("yyyyMMdd");
        qbf.setDocNum(sdf.format(fromDate));
        qbf.setEditQbClass(false);
        qbf.setEditAccnt(true);

        quickBookFormats.add(qbf);
        quickBookFormats.addAll(qbfs);
        qbf = new QuickBookFormat();
        qbf.setRowType("ENDTRNS");
        qbf.setEditQbClass(false);
        qbf.setEditAccnt(false);
        quickBookFormats.add(qbf);
    }

    public void createQBFormatOpdDayIncomeByDate() {

        grantTot = 0.0;
        quickBookFormats = new ArrayList<>();

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");

        List<Date> allDays = CommonFunctions.getDateList(fromDate, toDate);
        QuickBookFormat qbf;

        for (Date reportDate : allDays) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            List<PaymentMethod> paymentMethods = Arrays.asList(PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card, PaymentMethod.OnlineSettlement, PaymentMethod.MultiplePaymentMethods);
            qbfs.addAll(fetchOPdListDayEndTable(paymentMethods, CommonFunctions.getStartOfDay(reportDate), CommonFunctions.getEndOfDay(reportDate), null));
            qbfs.addAll(createPharmacySale(BillType.PharmacySale, CommonFunctions.getStartOfDay(reportDate), CommonFunctions.getEndOfDay(reportDate)));
            qbfs.addAll(createInwardCollection(CommonFunctions.getStartOfDay(reportDate), CommonFunctions.getEndOfDay(reportDate)));
            qbfs.addAll(createAgencyAndCollectionCenterTotal(CommonFunctions.getStartOfDay(reportDate), CommonFunctions.getEndOfDay(reportDate)));

            qbf = new QuickBookFormat();
            qbf.setRowType("TRNS");
            qbf.setTrnsType("Cash Sale");
            qbf.setDate(sdf.format(reportDate));
            qbf.setAccnt("Cash AR");
            qbf.setName("Cash AR");
            qbf.setAmount(grantTot);
            qbf.setMemo("Sales");
            sdf = new SimpleDateFormat("MM/dd/yy");
            qbf.setDocNum(sdf.format(reportDate));
            qbf.setEditQbClass(false);
            qbf.setEditAccnt(true);

            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);

            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            qbf.setEditQbClass(false);
            qbf.setEditAccnt(false);
            quickBookFormats.add(qbf);
        }

    }

    public void createQBFormatOpdDayCredit() {

        quickBookFormats = new ArrayList<>();

        List<PaymentMethod> paymentMethods = Arrays.asList(PaymentMethod.Credit);
        for (Institution i : fetchCreditCompany(CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate), true, BillType.OpdBill)) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            if (i != null) {
                System.out.println("****i.getName() = " + i.getName());
            } else {
                System.out.println("****i = " + i);
            }
            qbfs.addAll(fetchOPdListDayEndTable(paymentMethods, CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate), i));
            qbfs.addAll(fetchOPdDocPaymentTable(paymentMethods, CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate), i));
            if (qbfs.size() == 0) {
                System.out.println("qbfs.size() = " + qbfs.size());
                continue;
            }
            QuickBookFormat qbf = new QuickBookFormat();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            qbf.setRowType("TRNS");
            qbf.setTrnsType("INVOICE");
            qbf.setDate(sdf.format(fromDate));
            qbf.setAccnt("Accounts Receivable:Debtors Control - OPD Credit");
            qbf.setName("CREDIT COMPANY:" + i.getChequePrintingName());
            qbf.setAmount(grantTot);
            qbf.setMemo("Sales");
            sdf = new SimpleDateFormat("yyyyMMdd");
            qbf.setDocNum(sdf.format(fromDate));
            qbf.setEditQbClass(false);
            qbf.setEditAccnt(true);

            quickBookFormats.add(qbf);
            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            qbf.setEditQbClass(false);
            qbf.setEditAccnt(false);
            quickBookFormats.add(qbf);
        }

    }

    public void createQBFormatInwardIncome() {
        quickBookFormats = new ArrayList<>();
        if (reportKeyWord.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please Select BHT Type");
            return;
        }
        if (reportKeyWord.getPaymentMethod() == PaymentMethod.Credit) {
            for (PatientEncounter pe : fetchPatientEncounters(fromDate, toDate, PaymentMethod.Credit, null, getReportKeyWord().getAdmissionType(), null)) {
                System.out.println("pe.getBhtNo() = " + pe.getBhtNo());
                grantTot = 0.0;
                List<QuickBookFormat> qbfs = new ArrayList<>();
                qbfs.addAll(createInwardIncomes(fromDate, toDate, pe, null, PaymentMethod.Credit, null));
                if (grantTot != 0.0) {
                    QuickBookFormat qbf = new QuickBookFormat();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    qbf.setRowType("TRNS");
                    qbf.setTrnsType("INVOICE");
                    qbf.setDate(sdf.format(pe.getDateOfDischarge()));
                    qbf.setEditDate(true);
                    if (getReportKeyWord().getString().equals("0")) {
                        qbf.setAccnt("Accounts Receivable:Debtors Control - BHT");
                    }
                    if (getReportKeyWord().getString().equals("1")) {
                        qbf.setAccnt("Accounts Receivable:Debtors Control - GSB");
                    }
                    if (getReportKeyWord().getString().equals("2")) {
                        qbf.setAccnt("Accounts Receivable:Debtors Control - DS");
                    }
                    if (getReportKeyWord().isBool1()) {
                        qbf.setName("DIALYSIS CUSTOMER:" + pe.getCreditCompany().getChequePrintingName() + ":" + pe.getPatient().getPerson().getNameWithTitle());
                    } else {
                        qbf.setName("CREDIT COMPANY:" + pe.getCreditCompany().getChequePrintingName());
                    }
                    qbf.setAmount(grantTot);
                    qbf.setMemo("");
                    qbf.setDocNum(pe.getFinalBill().getInsId().substring(11));
                    qbf.setCustFld1(pe.getBhtNo());
                    sdf = new SimpleDateFormat("yyyy-MM-dd");
                    qbf.setCustFld2(sdf.format(pe.getDateOfAdmission()));
                    qbf.setCustFld3(sdf.format(pe.getDateOfDischarge()));
                    qbf.setCustFld4(pe.getPatient().getPerson().getNameWithTitle());
                    qbf.setCustFld5(pe.getPatient().getPerson().getAddress());
                    qbf.setCustFld6(pe.getPatient().getPerson().getPhone() + "/" + pe.getPatient().getPerson().getMobile());

                    quickBookFormats.add(qbf);
                    quickBookFormats.addAll(qbfs);
                    qbf = new QuickBookFormat();
                    qbf.setRowType("ENDTRNS");
                    quickBookFormats.add(qbf);
                }
            }

        } else {
            if (getReportKeyWord().getAdmissionType() != null) {
                grantTot = 0.0;
                List<QuickBookFormat> qbfs = new ArrayList<>();
                qbfs.addAll(createInwardIncomes(fromDate, toDate, null, getReportKeyWord().getAdmissionType(), PaymentMethod.Cash, null));
                if (grantTot != 0.0) {
                    QuickBookFormat qbf = new QuickBookFormat();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    qbf.setRowType("TRNS");
                    qbf.setTrnsType("INVOICE");
                    qbf.setDate(sdf.format(fromDate));
                    qbf.setEditDate(true);
                    if (getReportKeyWord().getString().equals("0")) {
                        qbf.setAccnt("Accounts Receivable:Debtors Control - BHT");
                    }
                    if (getReportKeyWord().getString().equals("1")) {
                        qbf.setAccnt("Accounts Receivable:Debtors Control - GSB");
                    }
                    if (getReportKeyWord().getString().equals("2")) {
                        qbf.setAccnt("Accounts Receivable:Debtors Control - DS");
                    }
                    sdf = new SimpleDateFormat("MMM yyyy");
                    if (getReportKeyWord().isBool1()) {
                        qbf.setName("DIALYSIS CUSTOMER:" + sdf.format(fromDate) + getReportKeyWord().getAdmissionType().getName());
                    } else {
                        qbf.setName(getReportKeyWord().getAdmissionType().getName().toUpperCase() + " Cash Customer:".toUpperCase() + getReportKeyWord().getAdmissionType().getName() + " Cash Customer-" + sdf.format(fromDate));
                    }
                    qbf.setAmount(grantTot);
                    qbf.setMemo("");
                    sdf = new SimpleDateFormat("yyyy-MMM");
                    qbf.setDocNum(sdf.format(fromDate));
                    qbf.setEditDocNum(true);

                    quickBookFormats.add(qbf);
                    quickBookFormats.addAll(qbfs);
                    qbf = new QuickBookFormat();
                    qbf.setRowType("ENDTRNS");
                    quickBookFormats.add(qbf);
                }

            } else {
                for (AdmissionType atype : getAdmissionTypeController().getItems()) {
                    System.out.println("atype.getName() = " + atype.getName());
                    grantTot = 0.0;
                    List<QuickBookFormat> qbfs = new ArrayList<>();
                    qbfs.addAll(createInwardIncomes(fromDate, toDate, null, atype, PaymentMethod.Cash, null));
                    if (grantTot != 0.0) {
                        QuickBookFormat qbf = new QuickBookFormat();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        qbf.setRowType("TRNS");
                        qbf.setTrnsType("INVOICE");
                        qbf.setDate(sdf.format(fromDate));
                        qbf.setEditDate(true);
                        if (getReportKeyWord().getString().equals("0")) {
                            qbf.setAccnt("Accounts Receivable:Debtors Control - BHT");
                        }
                        if (getReportKeyWord().getString().equals("1")) {
                            qbf.setAccnt("Accounts Receivable:Debtors Control - GSB");
                        }
                        if (getReportKeyWord().getString().equals("2")) {
                            qbf.setAccnt("Accounts Receivable:Debtors Control - DS");
                        }
                        sdf = new SimpleDateFormat("MMM yyyy");
                        if (getReportKeyWord().isBool1()) {
                            qbf.setName("DIALYSIS CUSTOMER:" + sdf.format(fromDate) + atype.getName());
                        } else {
                            qbf.setName(atype.getName().toUpperCase() + " Cash Customer:".toUpperCase() + atype.getName() + " Cash Customer-" + sdf.format(fromDate));
                        }
                        qbf.setAmount(grantTot);
                        qbf.setMemo("");
                        qbf.setDocNum("???");
                        qbf.setEditDocNum(true);

                        quickBookFormats.add(qbf);
                        quickBookFormats.addAll(qbfs);
                        qbf = new QuickBookFormat();
                        qbf.setRowType("ENDTRNS");
                        quickBookFormats.add(qbf);
                    }
                }
            }
        }

        System.out.println("quickBookFormats.size() = " + quickBookFormats.size());

    }

    public void createQBFormatPharmacyGRNAndPurchaseBills() {
        List<Bill> bills = new ArrayList<>();
        List<Bill> billsBilled = new ArrayList<>();
        List<Bill> billsCanceled = new ArrayList<>();
        List<Bill> billsReturn = new ArrayList<>();
        List<Bill> billsReturnCancel = new ArrayList<>();
        List<Bill> billsBilledP = new ArrayList<>();
        List<Bill> billsCanceledP = new ArrayList<>();
        List<Bill> billsReturnP = new ArrayList<>();
        List<Bill> billsReturnCancelP = new ArrayList<>();

        for (Department d : getDepartmentrs(Arrays.asList(new BillType[]{BillType.PharmacyGrnBill, BillType.PharmacyGrnReturn, BillType.PharmacyPurchaseBill, BillType.PurchaseReturn}), getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate))) {
            System.out.println("d.getName() = " + d.getName());
            billsBilled.addAll(getBills(new BilledBill(), BillType.PharmacyGrnBill, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsBilledP.addAll(getBills(new BilledBill(), BillType.PharmacyPurchaseBill, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsCanceled.addAll(getBills(new CancelledBill(), BillType.PharmacyGrnBill, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsCanceledP.addAll(getBills(new CancelledBill(), BillType.PharmacyPurchaseBill, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsReturn.addAll(getBills(new BilledBill(), BillType.PharmacyGrnReturn, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsReturnP.addAll(getBills(new BilledBill(), BillType.PurchaseReturn, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsReturnCancel.addAll(getBills(new CancelledBill(), BillType.PharmacyGrnReturn, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsReturnCancelP.addAll(getBills(new CancelledBill(), BillType.PurchaseReturn, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
        }

        System.out.println("billsBilled.size() = " + billsBilled.size());
        bills.addAll(billsBilled);
        System.out.println("billsBilledP.size() = " + billsBilledP.size());
        bills.addAll(billsBilledP);

        System.out.println("bills.size() = " + bills.size());

        quickBookFormats = new ArrayList<>();

        for (Bill b : bills) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(b.getNetTotal());
//            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            System.out.println("b.getInvoiceDate() = " + b.getInvoiceDate());
            System.out.println("b.getInsId() = " + b.getInsId());
            Date invoiceDate = b.getInvoiceDate() != null ? b.getInvoiceDate() : b.getCreatedAt();
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(invoiceDate) + " / " + b.getFromInstitution().getChequePrintingName());
            } else {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(invoiceDate));
            }
            qbf.setCustFld1("");
            qbf.setCustFld2("");
            qbf.setCustFld3("");
            qbf.setCustFld4("");
            qbf.setCustFld5("");
            grantTot += b.getNetTotal();
//            grantTot += b.getTotal();
            qbfs.add(qbf);
            System.out.println("b.getBillExpenses().size() = " + b.getBillExpenses().size());
            for (BillItem bi : b.getBillExpenses()) {
                System.err.println("expensess");
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", bi.getNetValue(), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), "Cash GRN - Pharmacy", "", "", (0 - grantTot), b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getFromInstitution().getChequePrintingName(), "", "", (0 - grantTot), b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            }
            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            quickBookFormats.add(qbf);
        }
        bills = new ArrayList<>();
        System.out.println("billsCanceled.size() = " + billsCanceled.size());
        bills.addAll(billsCanceled);
        System.out.println("billsCanceledP.size() = " + billsCanceledP.size());
        bills.addAll(billsCanceledP);
        System.out.println("bills.size() = " + bills.size());

        for (Bill b : bills) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill Refund");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(b.getNetTotal());
//            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            Date invoiceDate = b.getInvoiceDate() != null ? b.getInvoiceDate() : b.getCreatedAt();
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(invoiceDate) + " / " + b.getFromInstitution().getChequePrintingName() + " / " + b.getBilledBill().getDeptId());
            } else {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(invoiceDate) + " / " + b.getBilledBill().getDeptId());
            }
            qbf.setCustFld1("");
            qbf.setCustFld2("");
            qbf.setCustFld3("");
            qbf.setCustFld4("");
            qbf.setCustFld5("");
            grantTot += b.getNetTotal();
//            grantTot += b.getTotal();
            qbfs.add(qbf);
            System.out.println("b.getBillExpenses().size() = " + b.getBillExpenses().size());
            for (BillItem bi : b.getBillExpenses()) {
                System.err.println("expensess");
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", bi.getNetValue(), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), "Cash GRN - Pharmacy", "", "", (0 - grantTot), b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getFromInstitution().getChequePrintingName(), "", "", (0 - grantTot), b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            }
            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            quickBookFormats.add(qbf);
        }

        bills = new ArrayList<>();
        System.out.println("billsReturn.size() = " + billsReturn.size());
        bills.addAll(billsReturn);
        System.out.println("billsReturnP.size() = " + billsReturnP.size());
        bills.addAll(billsReturnP);
        System.out.println("bills.size() = " + bills.size());

        for (Bill b : bills) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill Refund");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getNetTotal());
//            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getDeptId());
//                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getToInstitution().getChequePrintingName() + " / " + b.getDeptId());
            } else {
                qbf.setMemo(b.getDeptId());
//                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getDeptId());
            }
            qbf.setCustFld1("");
            qbf.setCustFld2("");
            qbf.setCustFld3("");
            qbf.setCustFld4("");
            qbf.setCustFld5("");
            grantTot += b.getNetTotal();
//            grantTot += b.getTotal();
            qbfs.add(qbf);
            System.out.println("b.getBillExpenses().size() = " + b.getBillExpenses().size());
            for (BillItem bi : b.getBillExpenses()) {
                System.err.println("expensess");
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getToInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getToInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            }
            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            quickBookFormats.add(qbf);
        }

        bills = new ArrayList<>();
        System.out.println("billsReturnCancel.size() = " + billsReturnCancel.size());
        bills.addAll(billsReturnCancel);
        System.out.println("billsReturnCancelP.size() = " + billsReturnCancelP.size());
        bills.addAll(billsReturnCancelP);
        System.out.println("bills.size() = " + bills.size());

        for (Bill b : bills) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(b.getNetTotal());
//            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getDeptId());
//                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getFromInstitution().getChequePrintingName() + " / " + b.getDeptId());
            } else {
                qbf.setMemo(b.getDeptId());
//                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getDeptId());
            }
            qbf.setCustFld1("");
            qbf.setCustFld2("");
            qbf.setCustFld3("");
            qbf.setCustFld4("");
            qbf.setCustFld5("");
            grantTot += b.getNetTotal();
//            grantTot += b.getTotal();
            qbfs.add(qbf);
            System.out.println("b.getBillExpenses().size() = " + b.getBillExpenses().size());
            for (BillItem bi : b.getBillExpenses()) {
                System.err.println("expensess");
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", bi.getNetValue(), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getToInstitution().getChequePrintingName(), "", "", (0 - grantTot), b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getToInstitution().getChequePrintingName(), "", "", (0 - grantTot), b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            }
            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            quickBookFormats.add(qbf);
        }

        System.out.println("bills.size() = " + bills.size());
        System.out.println("quickBookFormats.size() = " + quickBookFormats.size());
    }

    public void createQBFormatStoreGRNAndPurchaseBills() {
        List<Bill> bills = new ArrayList<>();
        List<Bill> billsBilled = new ArrayList<>();
        List<Bill> billsCanceled = new ArrayList<>();
        List<Bill> billsReturn = new ArrayList<>();
        List<Bill> billsReturnCancel = new ArrayList<>();
        List<Bill> billsBilledP = new ArrayList<>();
        List<Bill> billsCanceledP = new ArrayList<>();
        List<Bill> billsReturnP = new ArrayList<>();
        List<Bill> billsReturnCancelP = new ArrayList<>();

        for (Department d : getDepartmentrs(Arrays.asList(new BillType[]{BillType.StoreGrnBill, BillType.StoreGrnReturn, BillType.StorePurchase, BillType.StorePurchaseReturn}), getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate))) {
            System.out.println("d.getName() = " + d.getName());
            billsBilled.addAll(getBills(new BilledBill(), BillType.StoreGrnBill, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsBilledP.addAll(getBills(new BilledBill(), BillType.StorePurchase, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsCanceled.addAll(getBills(new CancelledBill(), BillType.StoreGrnBill, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsCanceledP.addAll(getBills(new CancelledBill(), BillType.StorePurchase, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsReturn.addAll(getBills(new BilledBill(), BillType.StoreGrnReturn, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsReturnP.addAll(getBills(new BilledBill(), BillType.PurchaseReturn, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsReturnCancel.addAll(getBills(new CancelledBill(), BillType.StoreGrnReturn, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsReturnCancelP.addAll(getBills(new CancelledBill(), BillType.PurchaseReturn, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
        }

        System.out.println("billsBilled.size() = " + billsBilled.size());
        bills.addAll(billsBilled);
        System.out.println("billsBilledP.size() = " + billsBilledP.size());
        bills.addAll(billsBilledP);

        System.out.println("bills.size() = " + bills.size());

        quickBookFormats = new ArrayList<>();

        for (Bill b : bills) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getDeptId());
//            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getFromInstitution().getName());
            } else {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()));
            }
            qbf.setCustFld1("");
            qbf.setCustFld2("");
            qbf.setCustFld3("");
            qbf.setCustFld4("");
            qbf.setCustFld5("");
            grantTot += b.getTotal();
            qbfs.add(qbf);
            System.out.println("b.getBillExpenses().size() = " + b.getBillExpenses().size());
            for (BillItem bi : b.getBillExpenses()) {
                System.err.println("expensess");
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getDeptId(), b.getDeptId(), b.getDepartment().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getPrintingName(), "Cash GRN - Stores", "", "", grantTot, b.getDeptId(), b.getDeptId(), b.getDepartment().getName(), b.getInvoiceNumber(), "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getPrintingName(), b.getFromInstitution().getChequePrintingName(), "", "", grantTot, b.getDeptId(), b.getDeptId(), b.getDepartment().getName(), b.getInvoiceNumber(), "", "", "", "", "");
            }
            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            quickBookFormats.add(qbf);
        }
        bills = new ArrayList<>();
        System.out.println("billsCanceled.size() = " + billsCanceled.size());
        bills.addAll(billsCanceled);
        System.out.println("billsCanceledP.size() = " + billsCanceledP.size());
        bills.addAll(billsCanceledP);
        System.out.println("bills.size() = " + bills.size());

        for (Bill b : bills) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill Refund");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getDeptId());
//            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getFromInstitution().getName() + " / " + b.getBilledBill().getDeptId());
            } else {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getBilledBill().getDeptId());
            }
            qbf.setCustFld1("");
            qbf.setCustFld2("");
            qbf.setCustFld3("");
            qbf.setCustFld4("");
            qbf.setCustFld5("");
            grantTot += b.getTotal();
            qbfs.add(qbf);
            System.out.println("b.getBillExpenses().size() = " + b.getBillExpenses().size());
            for (BillItem bi : b.getBillExpenses()) {
                System.err.println("expensess");
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getDeptId(), b.getDeptId(), b.getDepartment().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getPrintingName(), "Cash GRN - Stores", "", "", grantTot, b.getDeptId(), b.getDeptId(), b.getDepartment().getName(), b.getInvoiceNumber(), "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getPrintingName(), b.getFromInstitution().getChequePrintingName(), "", "", grantTot, b.getDeptId(), b.getDeptId(), b.getDepartment().getName(), b.getInvoiceNumber(), "", "", "", "", "");
            }
            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            quickBookFormats.add(qbf);
        }

        bills = new ArrayList<>();
        System.out.println("billsReturn.size() = " + billsReturn.size());
        bills.addAll(billsReturn);
        System.out.println("billsReturnP.size() = " + billsReturnP.size());
        bills.addAll(billsReturnP);
        System.out.println("bills.size() = " + bills.size());

        for (Bill b : bills) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill Refund");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getDeptId());
//            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getToInstitution().getName());
            } else {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()));
            }
            qbf.setCustFld1("");
            qbf.setCustFld2("");
            qbf.setCustFld3("");
            qbf.setCustFld4("");
            qbf.setCustFld5("");
            grantTot += b.getTotal();
            qbfs.add(qbf);
            System.out.println("b.getBillExpenses().size() = " + b.getBillExpenses().size());
            for (BillItem bi : b.getBillExpenses()) {
                System.err.println("expensess");
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getDeptId(), b.getDeptId(), b.getDepartment().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getPrintingName(), "Cash GRN - Stores", "", "", grantTot, b.getDeptId(), b.getDeptId(), b.getDepartment().getName(), b.getInvoiceNumber(), "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getPrintingName(), b.getToInstitution().getChequePrintingName(), "", "", grantTot, b.getDeptId(), b.getDeptId(), b.getDepartment().getName(), b.getInvoiceNumber(), "", "", "", "", "");
            }
            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            quickBookFormats.add(qbf);
        }

        bills = new ArrayList<>();
        System.out.println("billsReturnCancel.size() = " + billsReturnCancel.size());
        bills.addAll(billsReturnCancel);
        System.out.println("billsReturnCancelP.size() = " + billsReturnCancelP.size());
        bills.addAll(billsReturnCancelP);
        System.out.println("bills.size() = " + bills.size());

        for (Bill b : bills) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getDeptId());
//            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            System.out.println("b.getInsId() = " + b.getInsId());
            System.out.println("b.getDeptId() = " + b.getDeptId());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getFromInstitution().getName());
            } else {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()));
            }
            qbf.setCustFld1("");
            qbf.setCustFld2("");
            qbf.setCustFld3("");
            qbf.setCustFld4("");
            qbf.setCustFld5("");
            grantTot += b.getTotal();
            qbfs.add(qbf);
            System.out.println("b.getBillExpenses().size() = " + b.getBillExpenses().size());
            for (BillItem bi : b.getBillExpenses()) {
                System.err.println("expensess");
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getDeptId(), b.getDeptId(), b.getDepartment().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getPrintingName(), "Cash GRN - Stores", "", "", grantTot, b.getDeptId(), b.getDeptId(), b.getDepartment().getName(), b.getInvoiceNumber(), "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getPrintingName(), b.getFromInstitution().getChequePrintingName(), "", "", grantTot, b.getDeptId(), b.getDeptId(), b.getDepartment().getName(), b.getInvoiceNumber(), "", "", "", "", "");
            }
            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            quickBookFormats.add(qbf);
        }

        System.out.println("bills.size() = " + bills.size());
        System.out.println("quickBookFormats.size() = " + quickBookFormats.size());

    }

    public void createQBFormatCafeRGRNAndPurchaseBills() {
        List<Bill> bills = new ArrayList<>();
        List<Bill> billsBilled = new ArrayList<>();
        List<Bill> billsCanceled = new ArrayList<>();
        List<Bill> billsReturn = new ArrayList<>();
        List<Bill> billsReturnCancel = new ArrayList<>();
        List<Bill> billsBilledP = new ArrayList<>();
        List<Bill> billsCanceledP = new ArrayList<>();
        List<Bill> billsReturnP = new ArrayList<>();
        List<Bill> billsReturnCancelP = new ArrayList<>();

        for (Department d : getDepartmentrs(Arrays.asList(new BillType[]{BillType.PharmacyGrnBill, BillType.PharmacyGrnReturn, BillType.PharmacyPurchaseBill, BillType.PurchaseReturn}), getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate))) {
            System.out.println("d.getName() = " + d.getName());
            billsBilled.addAll(getBills(new BilledBill(), BillType.PharmacyGrnBill, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsBilledP.addAll(getBills(new BilledBill(), BillType.PharmacyPurchaseBill, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsCanceled.addAll(getBills(new CancelledBill(), BillType.PharmacyGrnBill, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsCanceledP.addAll(getBills(new CancelledBill(), BillType.PharmacyPurchaseBill, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsReturn.addAll(getBills(new BilledBill(), BillType.PharmacyGrnReturn, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsReturnP.addAll(getBills(new BilledBill(), BillType.PurchaseReturn, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsReturnCancel.addAll(getBills(new CancelledBill(), BillType.PharmacyGrnReturn, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
            billsReturnCancelP.addAll(getBills(new CancelledBill(), BillType.PurchaseReturn, d, getInstitution(), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate)));
        }

        System.out.println("billsBilled.size() = " + billsBilled.size());
        bills.addAll(billsBilled);
        System.out.println("billsBilledP.size() = " + billsBilledP.size());
        bills.addAll(billsBilledP);

        System.out.println("bills.size() = " + bills.size());

        quickBookFormats = new ArrayList<>();

        for (Bill b : bills) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getNetTotal());
//            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            System.out.println("b.getInvoiceDate() = " + b.getInvoiceDate());
            System.out.println("b.getInsId() = " + b.getInsId());
            Date invoiceDate = b.getInvoiceDate() != null ? b.getInvoiceDate() : b.getCreatedAt();
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(invoiceDate) + " / " + b.getFromInstitution().getChequePrintingName());
            } else {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(invoiceDate));
            }
            qbf.setCustFld1("");
            qbf.setCustFld2("");
            qbf.setCustFld3("");
            qbf.setCustFld4("");
            qbf.setCustFld5("");
            grantTot += b.getNetTotal();
//            grantTot += b.getTotal();
            qbfs.add(qbf);
            System.out.println("b.getBillExpenses().size() = " + b.getBillExpenses().size());
            for (BillItem bi : b.getBillExpenses()) {
                System.err.println("expensess");
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), "Cash GRN - " + b.getDepartment().getName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getFromInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            }
            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            quickBookFormats.add(qbf);
        }
        bills = new ArrayList<>();
        System.out.println("billsCanceled.size() = " + billsCanceled.size());
        bills.addAll(billsCanceled);
        System.out.println("billsCanceledP.size() = " + billsCanceledP.size());
        bills.addAll(billsCanceledP);
        System.out.println("bills.size() = " + bills.size());

        for (Bill b : bills) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill Refund");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(b.getNetTotal());
//            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            Date invoiceDate = b.getInvoiceDate() != null ? b.getInvoiceDate() : b.getCreatedAt();
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(invoiceDate) + " / " + b.getFromInstitution().getChequePrintingName() + " / " + b.getBilledBill().getDeptId());
            } else {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(invoiceDate) + " / " + b.getBilledBill().getDeptId());
            }
            qbf.setCustFld1("");
            qbf.setCustFld2("");
            qbf.setCustFld3("");
            qbf.setCustFld4("");
            qbf.setCustFld5("");
            grantTot += b.getNetTotal();
//            grantTot += b.getTotal();
            qbfs.add(qbf);
            System.out.println("b.getBillExpenses().size() = " + b.getBillExpenses().size());
            for (BillItem bi : b.getBillExpenses()) {
                System.err.println("expensess");
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", bi.getNetValue(), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), "Cash GRN - Pharmacy", "", "", (0 - grantTot), b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getFromInstitution().getChequePrintingName(), "", "", (0 - grantTot), b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            }
            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            quickBookFormats.add(qbf);
        }

        bills = new ArrayList<>();
        System.out.println("billsReturn.size() = " + billsReturn.size());
        bills.addAll(billsReturn);
        System.out.println("billsReturnP.size() = " + billsReturnP.size());
        bills.addAll(billsReturnP);
        System.out.println("bills.size() = " + bills.size());

        for (Bill b : bills) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill Refund");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getNetTotal());
//            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getDeptId());
//                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getToInstitution().getChequePrintingName() + " / " + b.getDeptId());
            } else {
                qbf.setMemo(b.getDeptId());
//                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getDeptId());
            }
            qbf.setCustFld1("");
            qbf.setCustFld2("");
            qbf.setCustFld3("");
            qbf.setCustFld4("");
            qbf.setCustFld5("");
            grantTot += b.getNetTotal();
//            grantTot += b.getTotal();
            qbfs.add(qbf);
            System.out.println("b.getBillExpenses().size() = " + b.getBillExpenses().size());
            for (BillItem bi : b.getBillExpenses()) {
                System.err.println("expensess");
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getToInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getToInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            }
            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            quickBookFormats.add(qbf);
        }

        bills = new ArrayList<>();
        System.out.println("billsReturnCancel.size() = " + billsReturnCancel.size());
        bills.addAll(billsReturnCancel);
        System.out.println("billsReturnCancelP.size() = " + billsReturnCancelP.size());
        bills.addAll(billsReturnCancelP);
        System.out.println("bills.size() = " + bills.size());

        for (Bill b : bills) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(b.getNetTotal());
//            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getDeptId());
//                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getFromInstitution().getChequePrintingName() + " / " + b.getDeptId());
            } else {
                qbf.setMemo(b.getDeptId());
//                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getDeptId());
            }
            qbf.setCustFld1("");
            qbf.setCustFld2("");
            qbf.setCustFld3("");
            qbf.setCustFld4("");
            qbf.setCustFld5("");
            grantTot += b.getNetTotal();
//            grantTot += b.getTotal();
            qbfs.add(qbf);
            System.out.println("b.getBillExpenses().size() = " + b.getBillExpenses().size());
            for (BillItem bi : b.getBillExpenses()) {
                System.err.println("expensess");
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", bi.getNetValue(), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getToInstitution().getChequePrintingName(), "", "", (0 - grantTot), b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getToInstitution().getChequePrintingName(), "", "", (0 - grantTot), b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), b.getDeptId(), "", "", "", "", "");
            }
            quickBookFormats.add(qbf);

            quickBookFormats.addAll(qbfs);
            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            quickBookFormats.add(qbf);
        }

        System.out.println("bills.size() = " + bills.size());
        System.out.println("quickBookFormats.size() = " + quickBookFormats.size());
    }

    public void createQBFormatPharmacyCredit() {
        quickBookFormats = new ArrayList<>();

        List<PaymentMethod> paymentMethods = Arrays.asList(PaymentMethod.Credit);

        quickBookFormats.addAll(fetchPharmacySaleTable(Arrays.asList(new PaymentMethod[]{PaymentMethod.Credit}), CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate), institution, Arrays.asList(new BillType[]{BillType.PharmacySale, BillType.PharmacyWholeSale})));
    }

    //-------Main Functions
    public List<QuickBookFormat> fetchOPdListDayEndTable(List<PaymentMethod> paymentMethods, Date fd, Date td, Institution creditCompany) {
        List<QuickBookFormat> qbfs = new ArrayList<>();
        Map params = new HashMap();
        String jpql;

        jpql = "select c, "
                + " i, "
                + " count(bi.bill), "
                + " sum(bf.feeValue),"
                + " bi.bill.billClassType "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c "
                + " where bi.bill.billType= :bTp  "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " and bi.bill.paymentMethod in :pms "
                + " and bi.bill.retired=false "
                + " and bi.retired=false "
                + " and bf.retired=false ";

        if (!withProfessionalFee) {
            jpql += " and bf.fee.feeType!=:ft ";
            params.put("ft", FeeType.Staff);
        } else {
            jpql += " and bf.fee.feeType!=:ft ";
            params.put("ft", FeeType.Staff);
        }
        if (institution != null) {
            jpql += " and bi.bill.institution=:ins ";
            params.put("ins", institution);
        }
        if (department != null) {
            jpql += " and bi.bill.department=:dep ";
            params.put("dep", department);
        }
        if (site != null) {
            jpql += " and bi.bill.department.site=:site ";
            params.put("site", site);
        }

        if (creditCompany != null) {
            jpql += " and bi.bill.creditCompany=:cd ";
            params.put("cd", creditCompany);

        } else {
            // RHRHD/87626 set fee depart ment collecting center
//            jpql += " and bf.department.institution=:ins ";
        }
        jpql += " group by c, i, bi.bill.billClassType, bf.fee "
                + " order by c.name, i.name, bf.fee.feeType ";

        params.put("toDate", td);
        params.put("fromDate", fd);

        params.put("bTp", BillType.OpdBill);
        params.put("pms", paymentMethods);

        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);

        List<Object[]> lobjs = getBillFacade().findAggregates(jpql, params, TemporalType.TIMESTAMP);
        System.out.println("lobjs.size = " + lobjs.size());

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
        Item itemBefore = null;
        QuickBookFormat qbf = new QuickBookFormat();
        for (Object[] lobj : lobjs) {
            Category cat = (Category) lobj[0];
            String cName = cat.getName();
            Item i = (Item) lobj[1];
            long d = (long) lobj[2];
            double sum = (double) lobj[3];
            BillClassType bclass = (BillClassType) lobj[4];
//            System.out.println("iName = " + i.getName());
//            System.out.println("i.getClass() = " + i.getClass());
//            System.out.println("bclass = " + bclass);
//            System.out.println("fValue = " + sum);
//            System.out.println("d = " + d);
            if (itemBefore == null) {
//                System.err.println("if");
                qbf.setRowType("SPL");
                if (creditCompany != null) {
                    qbf.setTrnsType("INVOICE");
                    if (creditCompany != null) {
                        qbf.setName("CREDIT COMPANY:" + creditCompany.getChequePrintingName());
                    } else {
                        qbf.setName("Credit");
                    }
                } else {
                    qbf.setTrnsType("INVOICE");
                    qbf.setName("Cash AR");
                }

                if (Investigation.class == i.getClass()) {
                    qbf.setAccnt("RHD LAB INCOME:RHD OPD Sale");
                } else {
                    qbf.setAccnt(cat.getDescription());
//                    qbf.setAccnt("INCOME:" + cName);
                    qbf.setEditAccnt(true);
                }
                qbf.setInvItemType("SERV");
                if (i.getName().length() > 30) {
                    qbf.setInvItem(i.getCategory().getName() + ":" + i.getName().substring(0, 30));
                } else {
                    qbf.setInvItem(i.getCategory().getName() + ":" + i.getName());
                }
                qbf.setAmount(0 - sum);
                if (bclass == BillClassType.BilledBill) {
                    qbf.setCustFld5(String.valueOf(d));
                }
                if (bclass == BillClassType.CancelledBill) {
                    qbf.setCustFld5(String.valueOf(0 - d));
                }
                if (bclass == BillClassType.RefundBill) {
                    qbf.setCustFld5(String.valueOf(0 - d));
                }
                if (i.getDepartment() != null) {
                    if (i.getInwardChargeType() == InwardChargeType.VAT) {
                        qbf.setQbClass("HSS");
//                        qbf.setQbClass("VAT Control Account:Output");
                    } else {
                        qbf.setQbClass(i.getDepartment().getName());
                    }
                } else {
                    qbf.setQbClass("No Department");
                }
                qbf.setEditQbClass(false);
                qbf.setEditAccnt(false);
                qbf.setMemo(i.getName());
                grantTot += sum;
                itemBefore = i;
            } else {
//                System.err.println("Else");
                if (itemBefore == i) {
//                    System.err.println("Item Equal");
                    long l = 0l;
                    try {
                        l = Long.parseLong(qbf.getCustFld5());
                    } catch (Exception e) {
                    }
                    if (bclass == BillClassType.BilledBill) {
                        qbf.setCustFld5(String.valueOf(l + d));
                        qbf.setAmount(qbf.getAmount() - sum);
                    }
                    if (bclass == BillClassType.CancelledBill) {
                        qbf.setCustFld5(String.valueOf(l - d));
                        qbf.setAmount(qbf.getAmount() - sum);
                    }
                    if (bclass == BillClassType.RefundBill) {
                        qbf.setCustFld5(String.valueOf(l - d));
                        qbf.setAmount(qbf.getAmount() - sum);
                    }
                    grantTot += sum;
                } else {
//                    System.out.println("i.getName() = " + i.getName());
//                    System.out.println("itemBefore.getName() = " + itemBefore.getName());
                    itemBefore = i;
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat();
                    qbf.setRowType("SPL");
                    if (creditCompany != null) {
                        qbf.setTrnsType("INVOICE");
                        if (creditCompany != null) {
                            qbf.setName("CREDIT COMPANY:" + creditCompany.getChequePrintingName());
                        } else {
                            qbf.setName("Credit");
                        }
                    } else {
                        qbf.setTrnsType("INVOICE");
                        qbf.setName("Cash AR");
                    }
                    if (Investigation.class == i.getClass()) {
                        if (cat.getDescription() != null && !cat.getDescription().trim().equals("")) {
                            qbf.setAccnt(cat.getDescription());
                        } else {
                            qbf.setAccnt("RHD LAB INCOME:RHD OPD Sale");
                        }
                    } else {
                        qbf.setAccnt(cat.getDescription());
//                        qbf.setAccnt("INCOME:" + cName);
                        qbf.setEditAccnt(true);
                    }
                    qbf.setInvItemType("SERV");
                    if (i.getName().length() > 30) {
                        qbf.setInvItem(i.getCategory().getName() + ":" + i.getName().substring(0, 30));
                    } else {
                        qbf.setInvItem(i.getCategory().getName() + ":" + i.getName());
                    }
                    qbf.setAmount(0 - sum);
                    if (bclass == BillClassType.BilledBill) {
                        qbf.setCustFld5(String.valueOf(d));
                    }
                    if (bclass == BillClassType.CancelledBill) {
                        qbf.setCustFld5(String.valueOf(0 - d));
                    }
                    if (bclass == BillClassType.RefundBill) {
                        qbf.setCustFld5(String.valueOf(0 - d));
                    }
                    if (i.getDepartment() != null) {
                        if (i.getInwardChargeType() == InwardChargeType.VAT) {
                            qbf.setQbClass("HSS");
//                            qbf.setQbClass("VAT Control Account:Output");
                        } else {
                            qbf.setQbClass(i.getDepartment().getName());
                        }
                    } else {
                        qbf.setQbClass("No Department");
                    }
                    qbf.setEditQbClass(false);
                    qbf.setEditAccnt(false);
                    qbf.setMemo(i.getName());
                    grantTot += sum;
                }
            }

        }
        if (qbf.getAmount() != 0.0) {
            qbfs.add(qbf);
        }

        return qbfs;

    }

    public List<QuickBookFormat> fetchOPdListProfessionalFeeDayEndTable(List<PaymentMethod> paymentMethods, Date fd, Date td, Institution creditCompany) {
        List<QuickBookFormat> qbfs = new ArrayList<>();
        Map<String, Object> temMap = new HashMap<>();
        String jpql;

//        jpql = "select count(bi.bill), sum(bf.feeValue) "
//                + " from BillFee bf join bf.billItem bi "
//                + " where bi.bill.billType = :bTp "
//                + " and bi.bill.createdAt between :fromDate and :toDate "
//                + " and bi.bill.paymentMethod in :pms "
//                + " and bi.bill.retired = false "
//                + " and bi.retired = false "
//                + " and bf.retired = false "
//                + " and bf.fee.feeType = :ft ";
        jpql = "select count(bi.bill), sum(bf.feeValue) "
                + " from BillFee bf join bf.billItem bi "
                + " where bi.bill.billTypeAtomic in :btas "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " and bi.bill.paymentMethod in :pms "
                + " and bi.bill.retired = false "
                + " and bi.retired = false "
                + " and bf.retired = false "
                + " and bf.fee.feeType = :ft ";

        temMap.put("ft", FeeType.Staff);

        if (institution != null) {
            jpql += " and bi.bill.institution = :ins ";
            temMap.put("ins", institution);
        }
        if (department != null) {
            jpql += " and bi.bill.department = :dep ";
            temMap.put("dep", department);
        }
        if (site != null) {
            jpql += " and bi.bill.department.site = :site ";
            temMap.put("site", site);
        }

        if (creditCompany != null) {
            jpql += " and bi.bill.creditCompany = :cd ";
            temMap.put("cd", creditCompany);
        }

        // Removed the ORDER BY clause as it's unnecessary and causes unintended grouping
        // jpql += " order by c.name, i.name, bf.fee.feeType ";
        temMap.put("fromDate", fd);
        temMap.put("toDate", td);
//        temMap.put("bTp", BillType.OpdBill);

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        btas.add(BillTypeAtomic.OPD_BILL_REFUND);
        btas.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        btas.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);

        temMap.put("btas", btas);

        temMap.put("pms", paymentMethods);

        List<Object[]> lobjs = getBillFacade().findAggregates(jpql, temMap, TemporalType.TIMESTAMP);
        System.out.println("lobjs.size = " + (lobjs != null ? lobjs.size() : 0));

        if (lobjs != null && !lobjs.isEmpty()) {
            Object[] resultRow = lobjs.get(0);
            Number countResult = (Number) resultRow[0];
            Number sumResult = (Number) resultRow[1];

            Long proBillCount = countResult != null ? countResult.longValue() : 0L;
            Double proFeeValue = sumResult != null ? sumResult.doubleValue() : 0.0;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Item itemBefore = null;

            QuickBookFormat qbf = new QuickBookFormat();
            qbf.setRowType("SPL");
            qbf.setTrnsType("Cash Sale");
            qbf.setAccnt("ACCRUED CHARGES:Consultant Advance:Consultant Payment");
            qbf.setName("Cash AR");
            qbf.setInvItemType("SERV");
            qbf.setInvItem("Consultant Payment:OPD Professional Payments");
            qbf.setMemo("OPD Professional Payments");
            qbf.setAmount(0 - Math.abs(proFeeValue));
            qbf.setCustFld5(proBillCount.toString());

            qbfs.add(qbf);
            grantTot += proFeeValue;
        }

        return qbfs;
    }

    public List<QuickBookFormat> fetchOPdDocPaymentTable(List<PaymentMethod> paymentMethods, Date fd, Date td, Institution creditCompany) {
        List<QuickBookFormat> qbfs = new ArrayList<>();
        Map temMap = new HashMap();
        String jpql;

        jpql = "select sum(bf.feeValue) "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c "
                + " where bi.bill.institution=:ins "
                + " and bi.bill.billType= :bTp  "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " and bi.bill.paymentMethod in :pms "
                + " and bf.fee.feeType=:ft "
                + " and bi.bill.retired=false "
                + " and bi.retired=false "
                + " and bf.retired=false ";

        if (creditCompany != null) {
            jpql += " and bi.bill.creditCompany=:cd ";
            temMap.put("cd", creditCompany);
        } else {
            jpql += " and bf.department.institution=:ins ";
        }

        temMap.put("ft", FeeType.Staff);
        temMap.put("toDate", td);
        temMap.put("fromDate", fd);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pms", paymentMethods);

        double d = getBillFeeFacade().findDoubleByJpql(jpql, temMap, TemporalType.TIMESTAMP);
//        System.out.println("d = " + d);
//        List<Object[]> lobjs = getBillFacade().findAggregates(jpql, temMap, TemporalType.TIMESTAMP);
//        System.out.println("lobjs.size = " + lobjs.size());

//        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
//        Item itemBefore = null;
        QuickBookFormat qbf = new QuickBookFormat("ACCRUED CHARGES:Consultant Advance:OPD Credit Professional Fee",
                "CREDIT COMPANY:" + creditCompany.getChequePrintingName(), "ACCRUED CHARGES:Consultant Advance:Professional",
                0 - d, "OPD");

        qbf.setInvItem("ACCRUED CHARGES:Consultant Advance:Professional");

        grantTot += d;

        if (qbf.getAmount() != 0.0) {
            qbfs.add(qbf);
        }

        return qbfs;

    }

    public List<QuickBookFormat> createPharmacySale(BillType bt, Date fd, Date td) {

        List<QuickBookFormat> qbfs = new ArrayList<>();
        List<Object[]> list = getBillBeanController().fetchDepartmentSale(fd, td, getInstitution(), bt);

        for (Object[] obj : list) {

            Department department = ((Department) obj[0]);
            Double value = (Double) obj[1];

            if (value != null) {
                QuickBookFormat qbf = new QuickBookFormat();
                qbf.setRowType("SPL");
                qbf.setTrnsType("Cash Sale");
                qbf.setName("Cash AR");
                if (getReportKeyWord().isBool3()) {
                    qbf.setAccnt("OTHER RECEIVABLES:" + department.getName());
                } else {
                    qbf.setAccnt("PHARMACY SALES:Pharmacy OPD Sales");
                }
                qbf.setInvItemType("SERV");

                qbf.setAmount(0 - value);
                if (department != null) {
                    System.out.println("department.getPrintingName()) " + department.getName());
                    qbf.setQbClass(department.getName());
                    qbf.setInvItem(department.getName());
                    qbf.setMemo(department.getName());
                } else {
                    qbf.setQbClass("No Department");
                    qbf.setInvItem("No Department");
                }
                qbf.setEditQbClass(false);
                qbf.setEditAccnt(false);
                grantTot += value;
                qbfs.add(qbf);
            }
        }
        System.out.println("qbfs.size() = " + qbfs.size());
        return qbfs;

    }

    public List<QuickBookFormat> createInwardCollection(Date fd, Date td) {

        List<QuickBookFormat> qbfs = new ArrayList<>();
        List<Object[]> list = getBillBeanController().calInwardPaymentTotal(fd, td, getInstitution());

        for (Object[] obj : list) {
            AdmissionType admissionType = (AdmissionType) obj[0];
            PaymentMethod paymentMethod = (PaymentMethod) obj[1];
            double grantDbl = (Double) obj[2];

            String3Value2 newRow = new String3Value2();
            newRow.setString1(admissionType.getName() + " " + paymentMethod + " : ");
            newRow.setSummery(true);
            System.out.println("admissionType.getName()  paymentMethod = " + admissionType.getName() + " " + paymentMethod);

            if (paymentMethod != PaymentMethod.Credit) {
                QuickBookFormat qbf = new QuickBookFormat();
                qbf.setRowType("SPL");
                qbf.setTrnsType("Cash Sale");
                qbf.setName("CASH AR");
                if (admissionType.getAdmissionTypeEnum() == AdmissionTypeEnum.DayCase) {
                    qbf.setAccnt("ACCRUED CHARGES:Patient Deposits:OPD Patient Deposit");
                } else {
                    qbf.setAccnt("ACCRUED CHARGES:Patient Deposits:Indoor Patient Deposit");
                }
                qbf.setInvItemType("SERV");

                qbf.setQbClass(admissionType.getName());
                qbf.setInvItem(admissionType.getName());
                qbf.setMemo(paymentMethod.toString());
                qbf.setAmount(0 - grantDbl);
                qbf.setEditQbClass(true);
                qbf.setEditAccnt(false);

                grantTot += grantDbl;
                qbfs.add(qbf);
            }
        }
        return qbfs;
    }

    public List<QuickBookFormat> createAgencyAndCollectionCenterTotal(Date fd, Date td) {

        List<QuickBookFormat> qbfs = new ArrayList<>();

        double agent = getBillBeanController().calBillTotal(BillType.AgentPaymentReceiveBill, fd, td, getInstitution());
        List<BillTypeAtomic> ccCollection = new ArrayList<>();
        ccCollection.add(BillTypeAtomic.CC_PAYMENT_CANCELLATION_BILL);
        ccCollection.add(BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL);
        double cc = getBillBeanController().calBillTotal(ccCollection, fd, td, getInstitution(), getSite(), getDepartment());

        boolean skippedChannelling = true;
        QuickBookFormat qbf;
        if (!skippedChannelling) {
            qbf = new QuickBookFormat();
            qbf.setRowType("SPL");
            qbf.setTrnsType("Cash Sale");
            qbf.setName("CASH AR");
            qbf.setAccnt("ACCRUED CHARGES:Channel Advance");
            qbf.setInvItemType("SERV");

            qbf.setQbClass("Channel");
            qbf.setInvItem("Channel Advance");
            qbf.setMemo("");
            qbf.setAmount(0 - agent);

            grantTot += agent;
            qbfs.add(qbf);

        }

        qbf = new QuickBookFormat();
        qbf.setRowType("SPL");
        qbf.setTrnsType("Cash Sale");
        qbf.setName("CASH AR");
        qbf.setAccnt("ACCRUED CHARGES:Patient Deposits:Collecting Center Deposit");
        qbf.setInvItemType("SERV");

        qbf.setQbClass("RHD");
        qbf.setInvItem("Collecting Center Deposit");
        qbf.setMemo("");
        qbf.setAmount(0 - cc);
        qbf.setEditQbClass(false);
        qbf.setEditAccnt(false);

        grantTot += cc;
        qbfs.add(qbf);

        return qbfs;
    }

    public List<QuickBookFormat> createInwardIncomes(Date fd, Date td, PatientEncounter pe, AdmissionType admissionType, PaymentMethod paymentMethod, Institution cc) {
        List<QuickBookFormat> qbfs = new ArrayList<>();
        qbfs.addAll(fetchInwardOpdServiceWithoutPro(fd, td, pe, admissionType, paymentMethod, cc));
        qbfs.addAll(fetchInwardRoomValues(fd, td, pe, admissionType, paymentMethod, cc));
        qbfs.addAll(fetchInwardTimedService(fd, td, pe, admissionType, paymentMethod, cc));
        qbfs.addAll(fetchInwardDoctorPayment(fd, td, pe, admissionType, paymentMethod, cc));
        qbfs.addAll(fetchInwardOtherService(fd, td, pe, admissionType, paymentMethod, cc));
        System.out.println("qbfs.size(All) = " + qbfs.size());
        return qbfs;
    }

    public List<QuickBookFormat> fetchInwardOpdServiceWithoutPro(Date fd, Date td, PatientEncounter pe, AdmissionType admissionType, PaymentMethod paymentMethod, Institution cc) {
        String jpql;
        Map temMap = new HashMap();
        jpql = "select bf.billItem.item.category, "
                + " bf.billItem.item, "
                + " count(bf.billItem.bill), "
                + " sum(bf.feeValue),"
                + " bf.billItem.bill.billClassType"
                + " from BillFee bf "
                + " where bf.retired=false "
                + " and bf.bill.patientEncounter.discharged=true "
                + " and bf.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and bf.billItem.retired=false "
                + " and bf.bill.billType=:billType ";

        if (!withProfessionalFee) {
            jpql += " and bf.fee.feeType!=:ft ";
            temMap.put("ft", FeeType.Staff);
        } else {

        }

        if (pe != null) {
            jpql += " and bf.bill.patientEncounter=:bhtno ";
            temMap.put("bhtno", pe);
        }

        if (admissionType != null) {
            jpql = jpql + " and bf.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);

        }

        if (paymentMethod != null) {
            jpql = jpql + " and bf.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (cc != null) {
            jpql = jpql + " and bf.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", cc);
        }

        jpql = jpql + " group by bf.billItem.item, bf.billItem.bill.billClassType "
                + " order by bf.billItem.item.category.name, bf.billItem.item.name";

        temMap.put("billType", BillType.InwardBill);
        temMap.put("fd", CommonFunctions.getStartOfDay(fd));
        temMap.put("td", CommonFunctions.getEndOfDay(td));
        List<Object[]> results = getBillFacade().findAggregates(jpql, temMap, TemporalType.TIMESTAMP);

        System.out.println("results.size() = " + results.size());
        grantTot = 0.0;
        List<QuickBookFormat> qbfs = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
        Item itemBefore = null;
        QuickBookFormat qbf = new QuickBookFormat();
        for (Object[] lobj : results) {
            Category cat = (Category) lobj[0];
            String cName = cat.getName();
            Item i = (Item) lobj[1];
            long d = (long) lobj[2];
            double sum = (double) lobj[3];
            BillClassType bclass = (BillClassType) lobj[4];
            System.out.println("iName = " + i.getName());
            System.out.println("i.getClass() = " + i.getClass());
            System.out.println("bclass = " + bclass);
            String s = "";
            if (i.getInwardChargeType() == InwardChargeType.VAT) {
                s = "VAT Control Account:Output";
            } else {
                s = i.getDepartment().getName();
            }
            sdf = new SimpleDateFormat("MMM yyyy");
            String name = "";
            if (paymentMethod == PaymentMethod.Credit) {
                if (getReportKeyWord().isBool1()) {
                    name = "DIALYSIS CUSTOMER:" + pe.getCreditCompany().getChequePrintingName() + ":" + pe.getPatient().getPerson().getNameWithTitle();
                } else {
                    name = "CREDIT COMPANY:" + pe.getCreditCompany().getChequePrintingName();
                }
            } else {
                if (getReportKeyWord().isBool1()) {
                    name = "DIALYSIS CUSTOMER:" + sdf.format(fromDate) + getReportKeyWord().getAdmissionType().getName();
                } else {
                    name = getReportKeyWord().getAdmissionType().getName().toUpperCase() + " Cash Customer:".toUpperCase() + getReportKeyWord().getAdmissionType().getName() + " Cash Customer-" + sdf.format(fromDate);
                }
            }
            if (itemBefore == null) {
                System.err.println("if");

                if (paymentMethod == PaymentMethod.Credit) {
                    if (pe != null) {
                        qbf = new QuickBookFormat(cat.getName(), name, i.getName(), 0 - sum, s, "");
                    } else {
                        qbf = new QuickBookFormat(cat.getName(), "No Bht", i.getName(), 0 - sum, s, "");
                    }
                } else {
                    sdf = new SimpleDateFormat("MMM yyyy");
                    qbf = new QuickBookFormat(cat.getName(), name, i.getName(), 0 - sum, s, "");
                }
                if (Investigation.class == i.getClass()) {
                    qbf.setAccnt("RHD LAB INCOME:RHD Inward Sale");
                } else {
//                    qbf.setAccnt(cat.getName() + " I");
                    qbf.setAccnt(cat.getDescription());
                    qbf.setEditAccnt(true);
                }

                if (bclass == BillClassType.BilledBill) {
                    qbf.setCustFld5(String.valueOf(d));
                }
                if (bclass == BillClassType.CancelledBill) {
                    qbf.setCustFld5(String.valueOf(0 - d));
                }
                if (bclass == BillClassType.RefundBill) {
                    qbf.setCustFld5(String.valueOf(0 - d));
                }
                grantTot += sum;
                itemBefore = i;
            } else {
                System.err.println("Else");
                if (itemBefore == i) {
                    System.err.println("Item Equal");
                    long l = 0l;
                    try {
                        l = Long.parseLong(qbf.getCustFld5());
                    } catch (Exception e) {
                    }
                    if (bclass == BillClassType.BilledBill) {
                        qbf.setCustFld5(String.valueOf(l + d));
                        qbf.setAmount(qbf.getAmount() - sum);
                    }
                    if (bclass == BillClassType.CancelledBill) {
                        qbf.setCustFld5(String.valueOf(l - d));
                        qbf.setAmount(qbf.getAmount() - sum);
                    }
                    if (bclass == BillClassType.RefundBill) {
                        qbf.setCustFld5(String.valueOf(l - d));
                        qbf.setAmount(qbf.getAmount() - sum);
                    }
                    grantTot += sum;
                } else {
                    System.out.println("i.getName() = " + i.getName());
                    System.out.println("itemBefore.getName() = " + itemBefore.getName());
                    itemBefore = i;
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat();
                    if (paymentMethod == PaymentMethod.Credit) {
                        if (pe != null) {
                            qbf = new QuickBookFormat(cat.getName(), name, i.getName(), 0 - sum, s, "");
                        } else {
                            qbf = new QuickBookFormat(cat.getName(), "No Bht", i.getName(), 0 - sum, s, "");
                        }
                    } else {
                        sdf = new SimpleDateFormat("MMM yyyy");
                        qbf = new QuickBookFormat(cat.getName(), name, i.getName(), 0 - sum, s, "");
                    }
                    if (Investigation.class == i.getClass()) {
                        qbf.setAccnt("RHD LAB INCOME:RHD Inward Sale");
                    } else {
//                        qbf.setAccnt(cat.getName() + " I");
                        qbf.setAccnt(cat.getDescription());
                        qbf.setEditAccnt(true);
                    }
                    if (bclass == BillClassType.BilledBill) {
                        qbf.setCustFld5(String.valueOf(d));
                    }
                    if (bclass == BillClassType.CancelledBill) {
                        qbf.setCustFld5(String.valueOf(0 - d));
                    }
                    if (bclass == BillClassType.RefundBill) {
                        qbf.setCustFld5(String.valueOf(0 - d));
                    }
                    grantTot += sum;
                }
            }

        }
        if (qbf.getAmount() != 0.0) {
            qbfs.add(qbf);
        }
        System.out.println("qbfs.size(service) = " + qbfs.size());
        return qbfs;

    }

    public List<QuickBookFormat> fetchInwardRoomValues(Date fd, Date td, PatientEncounter pe, AdmissionType admissionType, PaymentMethod paymentMethod, Institution cc) {
        HashMap m = new HashMap();
        String sql = "SELECT "
                + " sum(pr.calculatedRoomCharge-pr.discountRoomCharge),"
                + " sum(pr.calculatedMaintainCharge-pr.discountMaintainCharge), "
                + " sum(pr.calculatedMoCharge-pr.discountMoCharge), "
                + " sum(pr.calculatedNursingCharge-pr.discountNursingCharge), "
                + " sum(pr.calculatedLinenCharge-pr.discountLinenCharge), "
                + " sum(pr.calculatedAdministrationCharge-pr.discountAdministrationCharge), "
                + " sum(pr.calculatedMedicalCareCharge-pr.discountMedicalCareCharge) "
                + " FROM PatientRoom pr "
                + " where pr.retired=false "
                + " and pr.patientEncounter.dateOfDischarge between :fd and :td "
                + " and pr.patientEncounter.paymentFinalized=true ";

        if (pe != null) {
            sql += " and pr.patientEncounter=:pe ";
            m.put("pe", pe);
        }

        if (admissionType != null) {
            sql += " and pr.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql += " and pr.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (cc != null) {
            sql += " and pr.patientEncounter.creditCompany=:cc ";
            m.put("cc", cc);
        }

        m.put("fd", CommonFunctions.getStartOfDay(fd));
        m.put("td", CommonFunctions.getEndOfDay(td));

        Object obj[] = getPatientRoomFacade().findAggregateModified(sql, m, TemporalType.TIMESTAMP);

        List<QuickBookFormat> qbfs = new ArrayList<>();
        if (obj != null) {
            System.out.println("obj.length = " + obj.length);
            QuickBookFormat qbf;
            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy");
            String name = "";
            if (paymentMethod == PaymentMethod.Credit) {
                if (getReportKeyWord().isBool1()) {
                    name = "DIALYSIS CUSTOMER:" + pe.getCreditCompany().getChequePrintingName() + ":" + pe.getPatient().getPerson().getNameWithTitle();
                } else {
                    name = "CREDIT COMPANY:" + pe.getCreditCompany().getChequePrintingName();
                }
            } else {
                if (getReportKeyWord().isBool1()) {
                    name = "DIALYSIS CUSTOMER:" + sdf.format(fromDate) + getReportKeyWord().getAdmissionType().getName();
                } else {
                    name = getReportKeyWord().getAdmissionType().getName().toUpperCase() + " Cash Customer:".toUpperCase() + getReportKeyWord().getAdmissionType().getName() + " Cash Customer-" + sdf.format(fromDate);
                }
            }
            if (paymentMethod == PaymentMethod.Credit) {
                if (pe != null) {
                    qbf = new QuickBookFormat();
                    if (getReportKeyWord().isBool1()) {
                        qbf = new QuickBookFormat("INCOME ACCOUNTS:Dialysis Unit", name, "Dialysis", 0 - ((double) obj[0] + (double) obj[1] + (double) obj[2] + (double) obj[3] + (double) obj[4] + (double) obj[5] + (double) obj[6]), "Dialysis");
                        if (qbf.getAmount() != 0.0) {
                            qbfs.add(qbf);
                        }
                    } else {
                        qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.RoomCharges.toString(), name, InwardChargeType.RoomCharges.toString(), 0 - (double) obj[0], "Ward");
                        qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                        if (qbf.getAmount() != 0.0) {
                            qbfs.add(qbf);
                        }
                        qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.MaintainCharges.toString(), name, InwardChargeType.MaintainCharges.toString(), 0 - (double) obj[1], "Ward");
                        qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                        if (qbf.getAmount() != 0.0) {
                            qbfs.add(qbf);
                        }
                        qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.MOCharges.toString(), name, InwardChargeType.MOCharges.toString(), 0 - (double) obj[2], "Ward");
                        qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                        if (qbf.getAmount() != 0.0) {
                            qbfs.add(qbf);
                        }
                        qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.NursingCharges.toString(), name, InwardChargeType.NursingCharges.toString(), 0 - (double) obj[3], "Ward");
                        qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                        if (qbf.getAmount() != 0.0) {
                            qbfs.add(qbf);
                        }
                        qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.LinenCharges.toString(), name, InwardChargeType.LinenCharges.toString(), 0 - (double) obj[4], "Linen");
                        qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                        if (qbf.getAmount() != 0.0) {
                            qbfs.add(qbf);
                        }
                        qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.AdministrationCharge.toString(), name, InwardChargeType.AdministrationCharge.toString(), 0 - (double) obj[5], "Ward");
                        qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                        if (qbf.getAmount() != 0.0) {
                            qbfs.add(qbf);
                        }
                        qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.MedicalCareICU.toString(), name, InwardChargeType.MedicalCareICU.toString(), 0 - (double) obj[6], "Ward");
                        qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                        if (qbf.getAmount() != 0.0) {
                            qbfs.add(qbf);
                        }
                    }

                }
            } else {
                qbf = new QuickBookFormat();
                if (getReportKeyWord().isBool1()) {
                    qbf = new QuickBookFormat("INCOME ACCOUNTS:Dialysis Unit", name, "Dialysis", 0 - ((double) obj[0] + (double) obj[1] + (double) obj[2] + (double) obj[3] + (double) obj[4] + (double) obj[5] + (double) obj[6]), "Dialysis");
                    qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                } else {
                    qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.RoomCharges.toString(), name, InwardChargeType.RoomCharges.toString(), 0 - (double) obj[0], "Ward");
                    qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.MaintainCharges.toString(), name, InwardChargeType.MaintainCharges.toString(), 0 - (double) obj[1], "Ward");
                    qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.MOCharges.toString(), name, InwardChargeType.MOCharges.toString(), 0 - (double) obj[2], "Ward");
                    qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.NursingCharges.toString(), name, InwardChargeType.NursingCharges.toString(), 0 - (double) obj[3], "Ward");
                    qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.LinenCharges.toString(), name, InwardChargeType.LinenCharges.toString(), 0 - (double) obj[4], "Linen");
                    qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.AdministrationCharge.toString(), name, InwardChargeType.AdministrationCharge.toString(), 0 - (double) obj[5], "Ward ");
                    qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.MOCharges.toString(), name, InwardChargeType.MedicalCareICU.toString(), 0 - (double) obj[6], "Ward");
                    qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                }
            }
            for (int i = 0; i < obj.length; i++) {
                grantTot += (double) obj[i];
            }

        } else {
            System.err.println("No Room data");
        }
        System.out.println("qbfs.size(room) = " + qbfs.size());
        return qbfs;

    }

    public List<QuickBookFormat> fetchInwardDoctorPayment(Date fd, Date td, PatientEncounter pe, AdmissionType admissionType, PaymentMethod paymentMethod, Institution cc) {
        HashMap m = new HashMap();
        String sql = "Select b.staff.speciality.description,"
                + " sum(b.feeValue), "
                + " b.staff.speciality "
                + " FROM BillFee b "
                + " where b.retired=false "
                + " and b.bill.patientEncounter.discharged=true "
                + " and b.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and(b.bill.billType=:refType1 "
                + " or b.bill.billType=:refType2 )";

        if (pe != null) {
            sql += " and b.bill.patientEncounter=:bhtno ";
            m.put("bhtno", pe);
        }

        if (admissionType != null) {
            sql += " and b.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql += " and b.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (cc != null) {
            sql += " and b.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", cc);
        }

        sql += " group by b.staff.speciality.description "
                + " order by b.staff.speciality.description ";

        m.put("refType1", BillType.InwardBill);
        m.put("refType2", BillType.InwardProfessional);
        m.put("fd", CommonFunctions.getStartOfDay(fd));
        m.put("td", CommonFunctions.getEndOfDay(td));

        List<Object[]> objects = billFeeFacade.findAggregates(sql, m, TemporalType.TIMESTAMP);

        List<QuickBookFormat> qbfs = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy");
        String name = "";
        if (paymentMethod == PaymentMethod.Credit) {
            if (getReportKeyWord().isBool1()) {
                name = "DIALYSIS CUSTOMER:" + pe.getCreditCompany().getChequePrintingName() + ":" + pe.getPatient().getPerson().getNameWithTitle();
            } else {
                name = "CREDIT COMPANY:" + pe.getCreditCompany().getChequePrintingName();
            }
        } else {
            if (getReportKeyWord().isBool1()) {
                name = "DIALYSIS CUSTOMER:" + sdf.format(fromDate) + getReportKeyWord().getAdmissionType().getName();
            } else {
                name = getReportKeyWord().getAdmissionType().getName().toUpperCase() + " Cash Customer:".toUpperCase() + getReportKeyWord().getAdmissionType().getName() + " Cash Customer-" + sdf.format(fromDate);
            }
        }
        for (Object[] o : objects) {
            String sp = (String) o[0];
            double value = (double) o[1];
            Speciality s = (Speciality) o[2];
            System.out.println("s.getNmane() = " + s.getName());
            System.out.println("s.getCreater().getWebUserPerson().getName() = " + s.getCreater().getWebUserPerson().getName());
            QuickBookFormat qbf;
            if (paymentMethod == PaymentMethod.Credit) {
                if (pe != null) {
                    qbf = new QuickBookFormat(sp, name, sp, 0 - value, "Ward");
                    qbf.setInvItem(sp);
                } else {
                    qbf = new QuickBookFormat(sp, "No BHT", sp, 0 - value, "Ward");
                    qbf.setInvItem(sp);
                }
            } else {
                qbf = new QuickBookFormat(sp, name, sp, 0 - value, "Ward");
                qbf.setInvItem(sp);
            }
            if (value != 0.0) {
                grantTot += value;
                qbfs.add(qbf);

            }
        }
        System.out.println("qbfs.size(Docpay) = " + qbfs.size());

        return qbfs;

    }

    public List<QuickBookFormat> fetchInwardTimedService(Date fd, Date td, PatientEncounter pe, AdmissionType admissionType, PaymentMethod paymentMethod, Institution cc) {
        HashMap m = new HashMap();
//        String sql = "SELECT i.item,"
//                + " sum(i.serviceValue-i.discount) "
//                + " FROM PatientItem i "
//                + " where i.retired=false "
//                + " and i.patientEncounter.dateOfDischarge between :fd and :td "
//                + " and i.patientEncounter.discharged=true ";
        String sql = "SELECT i.item.category,"
                + " i.item,"
                + " sum(i.serviceValue-i.discount) "
                + " FROM PatientItem i "
                + " where i.retired=false "
                + " and i.patientEncounter.dateOfDischarge between :fd and :td "
                + " and i.patientEncounter.discharged=true ";

        if (pe != null) {
            sql += " and i.patientEncounter=:bhtno ";
            m.put("bhtno", pe);
        }

        if (admissionType != null) {
            sql += " and i.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql += " and i.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (cc != null) {
            sql += " and i.patientEncounter.creditCompany=:cc ";
            m.put("cc", cc);
        }

        sql += " group by i.item.category, i.item "
                + " order by i.item.category.name, i.item.name ";

        m.put("fd", CommonFunctions.getStartOfDay(fd));
        m.put("td", CommonFunctions.getEndOfDay(td));

        List<Object[]> results = getBillFeeFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

        List<QuickBookFormat> qbfs = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy");
        String name = "";
        if (paymentMethod == PaymentMethod.Credit) {
            if (getReportKeyWord().isBool1()) {
                name = "DIALYSIS CUSTOMER:" + pe.getCreditCompany().getChequePrintingName() + ":" + pe.getPatient().getPerson().getNameWithTitle();
            } else {
                name = "CREDIT COMPANY:" + pe.getCreditCompany().getChequePrintingName();
            }
        } else {
            if (getReportKeyWord().isBool1()) {
                name = "DIALYSIS CUSTOMER:" + sdf.format(fromDate) + getReportKeyWord().getAdmissionType().getName();
            } else {
                name = getReportKeyWord().getAdmissionType().getName().toUpperCase() + " Cash Customer:".toUpperCase() + getReportKeyWord().getAdmissionType().getName() + " Cash Customer-" + sdf.format(fromDate);
            }
        }
        for (Object[] obj : results) {
            Category cat = (Category) obj[0];
            Item item = (Item) obj[1];
            double value = (double) obj[2];
            QuickBookFormat qbf;
            if (paymentMethod == PaymentMethod.Credit) {
                if (pe != null) {
                    qbf = new QuickBookFormat(cat.getDescription(), name, item.getName(), 0 - value, item.getDepartment().getName());
                    qbf.setInvItem(cat.getName() + ":" + qbf.getInvItem());
                } else {
                    qbf = new QuickBookFormat(cat.getDescription(), "No BHT", item.getName(), 0 - value, item.getDepartment().getName());
                    qbf.setInvItem(cat.getName() + ":" + qbf.getInvItem());
                }
            } else {
                qbf = new QuickBookFormat(cat.getDescription(), name, item.getName(), 0 - value, item.getDepartment().getName());
                qbf.setInvItem(cat.getName() + ":" + qbf.getInvItem());
            }
            grantTot += value;
            qbfs.add(qbf);
        }
        System.out.println("qbfs.size(Time Service) = " + qbfs.size());
        return qbfs;
    }

    public List<QuickBookFormat> fetchInwardOtherService(Date fd, Date td, PatientEncounter pe, AdmissionType admissionType, PaymentMethod paymentMethod, Institution cc) {

        List<QuickBookFormat> qbfs = new ArrayList<>();
        double d = fetchAdmissionFeeValues(fd, td, pe, admissionType, paymentMethod, cc) - fetchAdmissionFeeValuesDeduct(fd, td, pe, admissionType, paymentMethod, cc);
        grantTot += d;
        QuickBookFormat qbf;
//        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy");
//        String name = getReportKeyWord().getAdmissionType().getName() + " Cash Customer-" + sdf.format(fromDate);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy");
        String name = "";
        if (paymentMethod == PaymentMethod.Credit) {
            if (getReportKeyWord().isBool1()) {
                name = "DIALYSIS CUSTOMER:" + pe.getCreditCompany().getChequePrintingName() + ":" + pe.getPatient().getPerson().getNameWithTitle();
            } else {
                name = "CREDIT COMPANY:" + pe.getCreditCompany().getChequePrintingName();
            }
        } else {
            if (getReportKeyWord().isBool1()) {
                name = "DIALYSIS CUSTOMER:" + sdf.format(fromDate) + getReportKeyWord().getAdmissionType().getName();
            } else {
                name = getReportKeyWord().getAdmissionType().getName().toUpperCase() + " Cash Customer:".toUpperCase() + getReportKeyWord().getAdmissionType().getName() + " Cash Customer-" + sdf.format(fromDate);
            }
        }
        if (getReportKeyWord().isBool1()) {
            if (paymentMethod == PaymentMethod.Credit && pe != null) {
                qbf = new QuickBookFormat("INCOME ACCOUNTS:Dialysis Unit", name, InwardChargeType.AdmissionFee.getLabel(), 0 - d, "Dialysis");
                qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
            } else {
                qbf = new QuickBookFormat("INCOME ACCOUNTS:Dialysis Unit", name, InwardChargeType.AdmissionFee.getLabel(), 0 - d, "Dialysis");
                qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
            }

        } else if (getReportKeyWord().isBool2()) {
            if (paymentMethod == PaymentMethod.Credit && pe != null) {
                qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.AdmissionFee.getLabel() + " Green Sheet", pe.getCreditCompany().getChequePrintingName(), InwardChargeType.AdmissionFee.getLabel(), 0 - d, "Theatre");
                qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
            } else {
                qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.AdmissionFee.getLabel() + " Green Sheet", name, InwardChargeType.AdmissionFee.getLabel(), 0 - d, "Theatre");
                qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
            }
        } else {
            if (paymentMethod == PaymentMethod.Credit && pe != null) {
                qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.AdmissionFee.getLabel(), "CREDIT COMPANY:" + pe.getCreditCompany().getChequePrintingName(), InwardChargeType.AdmissionFee.getLabel(), 0 - d, "Ward");
                qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
            } else {
                qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.AdmissionFee.getLabel(), name, InwardChargeType.AdmissionFee.getLabel(), 0 - d, "Ward");
                qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
            }
        }
        if (qbf.getAmount() != 0.0) {
            qbfs.add(qbf);
        }
        d = fetchIssue(fd, td, pe, admissionType, paymentMethod, cc, BillType.PharmacyBhtPre);
        grantTot += d;
        if (getReportKeyWord().isBool2()) {
            if (paymentMethod == PaymentMethod.Credit && pe != null) {
                qbf = new QuickBookFormat("PHARMACY SALES:Green Sheet Sales", "CREDIT COMPANY:" + pe.getCreditCompany().getChequePrintingName(), InwardChargeType.Medicine.getLabel(), 0 - d, "Main Pharmacy");
                qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem() + " Green Sheet");
            } else {
                qbf = new QuickBookFormat("PHARMACY SALES:Green Sheet Sales", name, InwardChargeType.Medicine.getLabel(), 0 - d, "Main Pharmacy");
                qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem() + " Green Sheet");
            }
        } else if (getReportKeyWord().isBool1()) {
            if (paymentMethod == PaymentMethod.Credit && pe != null) {
                qbf = new QuickBookFormat("PHARMACY SALES:Dialysis Sales", name, InwardChargeType.Medicine.getLabel(), 0 - d, "Main Pharmacy");
                qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
            } else {
                qbf = new QuickBookFormat("PHARMACY SALES:Dialysis Sales", name, InwardChargeType.Medicine.getLabel(), 0 - d, "Main Pharmacy");
                qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
            }
        } else {
            if (paymentMethod == PaymentMethod.Credit && pe != null) {
                qbf = new QuickBookFormat("PHARMACY SALES:Inward Sales", "CREDIT COMPANY:" + pe.getCreditCompany().getChequePrintingName(), InwardChargeType.Medicine.getLabel(), 0 - d, "Main Pharmacy");
                qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
            } else {
                qbf = new QuickBookFormat("PHARMACY SALES:Inward Sales", name, InwardChargeType.Medicine.getLabel(), 0 - d, "Main Pharmacy");
                qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
            }
        }
        if (qbf.getAmount() != 0.0) {
            qbfs.add(qbf);
        }
        d = fetchIssue(fd, td, pe, admissionType, paymentMethod, cc, BillType.StoreBhtPre);
        grantTot += d;
        if (paymentMethod == PaymentMethod.Credit && pe != null) {
            qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.GeneralIssuing.getLabel(), "CREDIT COMPANY:" + pe.getCreditCompany().getChequePrintingName(), InwardChargeType.GeneralIssuing.getLabel(), 0 - d, InwardChargeType.GeneralIssuing.getLabel());
            qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
        } else {
            qbf = new QuickBookFormat("INCOME ACCOUNTS:" + InwardChargeType.GeneralIssuing.getLabel(), name, InwardChargeType.GeneralIssuing.getLabel(), 0 - d, InwardChargeType.GeneralIssuing.getLabel());
            qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());

        }
        if (qbf.getAmount() != 0.0) {
            qbfs.add(qbf);
        }
        d = fetchOutSideFee(fd, td, pe, admissionType, paymentMethod, cc);
        grantTot += d;
        if (paymentMethod == PaymentMethod.Credit && pe != null) {
            qbf = new QuickBookFormat("ACCRUED CHARGES:Indoor Patient Payable", "CREDIT COMPANY:" + pe.getCreditCompany().getChequePrintingName(), "Out Side Charges", 0 - d, "Ward");
            qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());
        } else {
            qbf = new QuickBookFormat("ACCRUED CHARGES:Indoor Patient Payable", name, "Out Side Charges", 0 - d, "Ward");
            qbf.setInvItem(qbf.getInvItem() + ":" + qbf.getInvItem());

        }
        if (qbf.getAmount() != 0.0) {
            qbfs.add(qbf);
        }
        System.out.println("qbfs.size(Other Service) = " + qbfs.size());
        return qbfs;
    }

    public List<QuickBookFormat> fetchPharmacySaleTable(List<PaymentMethod> paymentMethods, Date fd, Date td,
            Institution creditCompany, List<BillType> bts) {
        List<QuickBookFormat> qbfs = new ArrayList<>();
        Map temMap = new HashMap();
        String jpql;

        jpql = "select b "
                + " from Bill b "
                + " where b.institution=:ins "
                + " and b.billType in :bts  "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.paymentMethod in :pms "
                + " and b.retired=false "
                + " and b.toInstitution is not null"
                + " and b.toStaff is null "
                + " order by b.toInstitution.name ";

        temMap.put("toDate", td);
        temMap.put("fromDate", fd);
        temMap.put("ins", institution);
        temMap.put("bts", bts);
        temMap.put("pms", paymentMethods);

        List<Bill> bills = getBillFacade().findByJpql(jpql, temMap, TemporalType.TIMESTAMP);
        System.out.println("bills.size = " + bills.size());

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyyMMdd");
        for (Bill b : bills) {
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("TRNS");
            qbf.setTrnsType("INVOICE");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("Accounts Receivable:Debtors Control - OPD Credit");
            if (b.getToInstitution() != null) {
                qbf.setName("CREDIT COMPANY:" + b.getToInstitution().getChequePrintingName());
            } else {
                qbf.setName("CREDIT COMPANY:No Name");
            }
            qbf.setAmount(b.getNetTotal());
            qbf.setMemo("Sales");
            qbf.setDocNum(sdf2.format(fromDate));
            qbfs.add(qbf);

            qbf = new QuickBookFormat();
            qbf.setRowType("SPL");
            qbf.setTrnsType("INVOICE");
            qbf.setAccnt("PHARMACY SALES:Pharmacy OPD Sales");
            if (b.getToInstitution() != null) {
                qbf.setName("CREDIT COMPANY:" + b.getToInstitution().getChequePrintingName());
            } else {
                qbf.setName("CREDIT COMPANY:No Name");
            }
            qbf.setInvItemType("SERV");
            qbf.setInvItem("Drugs:Drugs OPD Credit");
            qbf.setAmount(0 - b.getNetTotal());
            qbf.setQbClass(b.getDepartment().getName());
            qbf.setMemo("Drugs");
            qbf.setEditQbClass(false);
            qbf.setEditAccnt(false);
            qbfs.add(qbf);

            qbf = new QuickBookFormat();
            qbf.setRowType("ENDTRNS");
            qbf.setEditQbClass(false);
            qbf.setEditAccnt(false);
            qbfs.add(qbf);

        }
        System.out.println("qbfs.size() = " + qbfs.size());
        return qbfs;

    }

    public double fetchOutSideFee(Date fd, Date td, PatientEncounter pe, AdmissionType admissionType, PaymentMethod paymentMethod, Institution cc) {
        HashMap m = new HashMap();

        String sql = "SELECT  sum(i.netTotal)"
                + " FROM Bill i "
                + " where i.retired=false"
                + " and i.billType=:btp "
                + " and i.patientEncounter.dateOfDischarge between :fd and :td "
                + " and i.patientEncounter.discharged=true ";
        if (pe != null) {
            sql += " and i.patientEncounter=:bhtno ";
            m.put("bhtno", pe);
        }

        if (admissionType != null) {
            sql += " and i.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql += " and i.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (cc != null) {
            sql += " and i.patientEncounter.creditCompany=:cc ";
            m.put("cc", cc);
        }
        m.put("fd", CommonFunctions.getStartOfDay(fd));
        m.put("td", CommonFunctions.getEndOfDay(td));
        m.put("btp", BillType.InwardOutSideBill);

        return billFeeFacade.findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double fetchIssue(Date fd, Date td, PatientEncounter pe, AdmissionType admissionType, PaymentMethod paymentMethod, Institution cc, BillType billType) {
        String sql;
        HashMap m = new HashMap();
        sql = "SELECT sum(b.netValue) "
                + " FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and b.bill.patientEncounter.discharged=true ";

        if (pe != null) {
            sql += " and b.bill.patientEncounter=:pe ";
            m.put("pe", pe);
        }

        if (admissionType != null) {
            sql += " and b.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql += " and b.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (cc != null) {
            sql += " and b.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", cc);
        }

        m.put("fd", CommonFunctions.getStartOfDay(fd));
        m.put("td", CommonFunctions.getEndOfDay(td));
        m.put("btp", billType);

        return billFeeFacade.findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double fetchAdmissionFeeValues(Date fd, Date td, PatientEncounter pe, AdmissionType admissionType, PaymentMethod paymentMethod, Institution cc) {
        HashMap m = new HashMap();
        String sql = "SELECT sum(i.netValue) "
                + " FROM BillItem i "
                + " where i.retired=false "
                + " and i.inwardChargeType=:inwTp "
                + " and i.bill.billType=:btp "
                + " and i.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and i.bill.patientEncounter.discharged=true ";

        if (pe != null) {
            sql += " and i.bill.patientEncounter=:bhtno "
                    + " and i.bill=:b ";
            m.put("bhtno", pe);
            m.put("b", getInwardBeanController().fetchFinalBill(pe));
        }
        if (admissionType != null) {
            sql += " and i.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql += " and i.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (cc != null) {
            sql += " and i.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", cc);
        }

        m.put("fd", CommonFunctions.getStartOfDay(fd));
        m.put("td", CommonFunctions.getEndOfDay(td));
        m.put("inwTp", InwardChargeType.AdmissionFee);
        m.put("btp", BillType.InwardFinalBill);

        return billFeeFacade.findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double fetchAdmissionFeeValuesDeduct(Date fd, Date td, PatientEncounter pe, AdmissionType admissionType, PaymentMethod paymentMethod, Institution cc) {
        HashMap m = new HashMap();
        String sql = "SELECT sum(i.netValue) "
                + " FROM BillItem i "
                + " where i.retired=false "
                + " and i.inwardChargeType=:inwTp "
                + " and i.bill.billType=:btp "
                + " and i.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and i.bill.patientEncounter.discharged=true ";

        if (pe != null) {
            sql += " and i.bill.patientEncounter=:bhtno ";
            m.put("bhtno", pe);
        }
        if (admissionType != null) {
            sql += " and i.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql += " and i.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (cc != null) {
            sql += " and i.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", cc);
        }

        m.put("fd", CommonFunctions.getStartOfDay(fd));
        m.put("td", CommonFunctions.getEndOfDay(td));
        m.put("inwTp", InwardChargeType.AdmissionFee);
        m.put("btp", BillType.InwardOutSideBill);

        return billFeeFacade.findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Institution> fetchCreditCompany(Date fd, Date td, boolean isOpd, BillType bt) {
        Map temMap = new HashMap();
        List<Institution> creditCompanies = new ArrayList<>();
        grantTot = 0.0;
        String jpql = " select distinct b.creditCompany "
                + " from Bill b "
                + " where b.institution=:ins "
                + " and b.billType= :bTp  "
                + " and b.retired=false"
                + " and b.creditCompany is not null ";

        if (isOpd) {
            jpql += " and b.createdAt between :fd and :td "
                    + " and b.paymentMethod=:pm ";
        } else {
            jpql += " and b.patientEncounter.dateOfDischarge between :fd and :td "
                    + " and b.patientEncounter.discharged=true "
                    + " and b.patientEncounter.paymentFinalized=true "
                    + " and b.patientEncounter.paymentMethod=:pm ";
        }

        temMap.put("td", td);
        temMap.put("fd", fd);
        temMap.put("ins", institution);
        temMap.put("bTp", bt);
//        temMap.put("bTp", BillType.InwardBill);
        temMap.put("pm", PaymentMethod.Credit);

        System.out.println("jpql = " + jpql);
        System.out.println("temMap = " + temMap);

        creditCompanies = getInstitutionFacade().findByJpql(jpql, temMap, TemporalType.TIMESTAMP);
        System.out.println("creditCompanies.size() = " + creditCompanies.size());
        return creditCompanies;
    }

    public List<Bill> getBills(Bill billClass, BillType billType, Department dep, Institution ins, Date fd, Date td) {
        String sql;
        Map temMap = new HashMap();

        sql = "SELECT b FROM Bill b WHERE type(b)=:bill"
                + " and b.retired=false "
                + " and b.billType = :btp "
                + " and b.department=:d "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " order by b.id  ";

        temMap.put("fromDate", fd);
        temMap.put("toDate", td);
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);
        temMap.put("d", dep);
        temMap.put("ins", ins);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<Department> getDepartmentrs(List<BillType> billTypes, Institution ins, Date fd, Date td) {
        String sql;
        Map temMap = new HashMap();

        sql = "SELECT distinct(b.department) FROM Bill b WHERE b.retired=false "
                + " and b.billType in :btps "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " order by b.department.name  ";

        temMap.put("fromDate", fd);
        temMap.put("toDate", td);
        temMap.put("btps", billTypes);
        temMap.put("ins", ins);

        return getDepartmentFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<PatientEncounter> fetchPatientEncounters(Date fd, Date td, PaymentMethod paymentMethod, List<AdmissionType> admissionTypes, AdmissionType admissionType, Institution cc) {
        List<PatientEncounter> pes = new ArrayList<>();
        String sql;
        Map m = new HashMap();
        sql = "select distinct(b.patientEncounter) "
                + " from Bill b "
                + " where b.retired=false "
                + " and b.patientEncounter.discharged=true "
                + " and b.patientEncounter.paymentFinalized=true "
                + " and b.patientEncounter.dateOfDischarge between :fd and :td "
                + " and b.billType=:billType ";

        if (admissionTypes != null) {
            sql += " and b.patientEncounter.admissionType in :ats ";
            m.put("ats", admissionTypes);
        }
        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (cc != null) {
            sql += " and b.patientEncounter.creditCompany=:cc ";
            m.put("cc", cc);
        }

        sql += " order by b.patientEncounter.bhtNo";

        m.put("fd", CommonFunctions.getStartOfDay(fd));
        m.put("td", CommonFunctions.getEndOfDay(td));
        m.put("billType", BillType.InwardFinalBill);

        pes = getPatientEncounterFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        System.out.println("pes.size() = " + pes.size());

        return pes;
    }

    private boolean errorCheck() {
        if (getInstitution() == null) {
            JsfUtil.addErrorMessage("Please Select Insitution");
            return true;
        }
        if (getReport() == null || getReport().equals("")) {
            JsfUtil.addErrorMessage("Please Select Report");
            return true;
        }
        return false;
    }

    public void listnerReportNameChange() {
        quickBookFormats = new ArrayList<>();
        items = new ArrayList<>();
        reportKeyWord = new ReportKeyWord();
    }

    public void listnerBool1Change() {
        if (getReportKeyWord().isBool1()) {
            getReportKeyWord().setBool2(false);
        }
    }

    public void listnerBool2Change() {
        if (getReportKeyWord().isBool2()) {
            getReportKeyWord().setBool1(false);
        }
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
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

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Institution getInstitution() {
        if (institution == null && getWebUserController().hasPrivilege("Developers")) {
            institution = getSessionController().getInstitution();
        }
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<QuickBookFormat> getQuickBookFormats() {
        return quickBookFormats;
    }

    public void setQuickBookFormats(List<QuickBookFormat> quickBookFormats) {
        this.quickBookFormats = quickBookFormats;
    }

    public double getGrantTot() {
        return grantTot;
    }

    public void setGrantTot(double grantTot) {
        this.grantTot = grantTot;
    }

    public List<Category> getCategorys() {
        return categorys;
    }

    public void setCategorys(List<Category> categorys) {
        this.categorys = categorys;
    }

    public List<Institution> getCreditCompanies() {
        return creditCompanies;
    }

    public void setCreditCompanies(List<Institution> creditCompanies) {
        this.creditCompanies = creditCompanies;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public CategoryFacade getCategoryFacade() {
        return categoryFacade;
    }

    public void setCategoryFacade(CategoryFacade categoryFacade) {
        this.categoryFacade = categoryFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public AdmissionTypeController getAdmissionTypeController() {
        return admissionTypeController;
    }

    public void setAdmissionTypeController(AdmissionTypeController admissionTypeController) {
        this.admissionTypeController = admissionTypeController;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public void setPatientRoomFacade(PatientRoomFacade patientRoomFacade) {
        this.patientRoomFacade = patientRoomFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public InwardBeanController getInwardBeanController() {
        return inwardBeanController;
    }

    public void setInwardBeanController(InwardBeanController inwardBeanController) {
        this.inwardBeanController = inwardBeanController;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public boolean isWithProfessionalFee() {
        return withProfessionalFee;
    }

    public void setWithProfessionalFee(boolean withProfessionalFee) {
        this.withProfessionalFee = withProfessionalFee;
    }

}
