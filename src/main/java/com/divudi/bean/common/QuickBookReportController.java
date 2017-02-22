/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.inward.AdmissionTypeController;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.QuickBookFormat;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.inward.AdmissionTypeEnum;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.data.table.String3Value2;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Speciality;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.lab.Investigation;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientRoomFacade;
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
 * @author archmage
 */
@Named(value = "quickBookReportController")
@SessionScoped
public class QuickBookReportController implements Serializable {

    /**
     * Creates a new instance of QuickBookReportController
     */
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

    @EJB
    private CommonFunctions commonFunctions;

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

    private Institution institution;
    private Date toDate;
    private Date fromDate;
    private String report;
    private ReportKeyWord reportKeyWord;
    private double grantTot;

    public QuickBookReportController() {
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
            default:
                throw new AssertionError();
        }
    }

    public void createQBFormatOpdDayIncome() {

        grantTot = 0.0;
        quickBookFormats = new ArrayList<>();
        List<QuickBookFormat> qbfs = new ArrayList<>();

        List<PaymentMethod> paymentMethods = Arrays.asList(PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card);
        qbfs.addAll(fetchOPdListWithProDayEndTable(paymentMethods, commonFunctions.getStartOfDay(fromDate), commonFunctions.getEndOfDay(fromDate), null));
        qbfs.addAll(createPharmacySale(BillType.PharmacySale, commonFunctions.getStartOfDay(fromDate), commonFunctions.getEndOfDay(fromDate)));
        qbfs.addAll(createInwardCollection(commonFunctions.getStartOfDay(fromDate), commonFunctions.getEndOfDay(fromDate)));
        qbfs.addAll(createAgencyAndCollectionCenterTotal(commonFunctions.getStartOfDay(fromDate), commonFunctions.getEndOfDay(fromDate)));

        QuickBookFormat qbf = new QuickBookFormat();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");

        qbf.setRowType("TRNS");
        qbf.setTrnsType("CASH SALE");
        qbf.setDate(sdf.format(fromDate));
        qbf.setAccnt("Cash in Hand");
        qbf.setName("Cash");
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

    public void createQBFormatOpdDayCredit() {

        quickBookFormats = new ArrayList<>();

        List<PaymentMethod> paymentMethods = Arrays.asList(PaymentMethod.Credit);
        for (Institution i : fetchCreditCompany(commonFunctions.getStartOfDay(fromDate), commonFunctions.getEndOfDay(fromDate), true, BillType.OpdBill)) {
            grantTot = 0.0;
            List<QuickBookFormat> qbfs = new ArrayList<>();
            System.out.println("i.getName() = " + i.getName());
            qbfs.addAll(fetchOPdListWithProDayEndTable(paymentMethods, commonFunctions.getStartOfDay(fromDate), commonFunctions.getEndOfDay(fromDate), i));
            QuickBookFormat qbf = new QuickBookFormat();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
            qbf.setRowType("TRNS");
            qbf.setTrnsType("CREDIT SALE");
            qbf.setDate(sdf.format(fromDate));
            qbf.setAccnt("RECEIVABLES:Debtors Control - OPD Credit Sale:" + i.getName());
            qbf.setName("Credit");
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
            for (PatientEncounter pe : fetchPatientEncounters(fromDate, toDate, PaymentMethod.Credit, null, null)) {
                System.out.println("pe.getBhtNo() = " + pe.getBhtNo());
                grantTot = 0.0;
                List<QuickBookFormat> qbfs = new ArrayList<>();
                qbfs.addAll(createInwardIncomes(fromDate, toDate, pe, null, PaymentMethod.Credit, null));
                if (grantTot != 0.0) {
                    QuickBookFormat qbf = new QuickBookFormat();
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
                    qbf.setRowType("TRNS");
                    qbf.setTrnsType("INVOICE");
                    qbf.setDate(sdf.format(fromDate));
                    qbf.setEditDate(true);
                    qbf.setAccnt("RECEIVABLES:Debetors Contol " + pe.getAdmissionType().getName());
                    qbf.setName(pe.getCreditCompany().getName());
                    qbf.setAmount(grantTot);
                    qbf.setMemo("");
                    qbf.setDocNum(pe.getFinalBill().getDeptId());
                    qbf.setCustFld1(pe.getBhtNo());
                    sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
                    qbf.setCustFld2(sdf.format(pe.getDateOfAdmission()));
                    qbf.setCustFld3(sdf.format(pe.getDateOfDischarge()));

                    quickBookFormats.add(qbf);
                    quickBookFormats.addAll(qbfs);
                    qbf = new QuickBookFormat();
                    qbf.setRowType("ENDTRNS");
                    quickBookFormats.add(qbf);
                }
            }
        } else {
            for (AdmissionType atype : getAdmissionTypeController().getItems()) {
                System.out.println("atype.getName() = " + atype.getName());
                grantTot = 0.0;
                List<QuickBookFormat> qbfs = new ArrayList<>();
                qbfs.addAll(createInwardIncomes(fromDate, toDate, null, atype, PaymentMethod.Cash, null));
                if (grantTot != 0.0) {
                    QuickBookFormat qbf = new QuickBookFormat();
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
                    qbf.setRowType("TRNS");
                    qbf.setTrnsType("INVOICE");
                    qbf.setDate(sdf.format(fromDate));
                    qbf.setEditDate(true);
                    qbf.setAccnt("RECEIVABLES:Debetors Contol " + atype.getName());
                    sdf = new SimpleDateFormat("MMM yyyy");
                    qbf.setName(atype.getName() + " Cash Customer-" + sdf.format(fromDate));
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

        for (Department d : getDepartmentrs(Arrays.asList(new BillType[]{BillType.PharmacyGrnBill, BillType.PharmacyGrnReturn, BillType.PharmacyPurchaseBill, BillType.PurchaseReturn}), getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate))) {
            System.out.println("d.getName() = " + d.getName());
            billsBilled.addAll(getBills(new BilledBill(), BillType.PharmacyGrnBill, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsBilledP.addAll(getBills(new BilledBill(), BillType.PharmacyPurchaseBill, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsCanceled.addAll(getBills(new CancelledBill(), BillType.PharmacyGrnBill, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsCanceledP.addAll(getBills(new CancelledBill(), BillType.PharmacyPurchaseBill, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsReturn.addAll(getBills(new BilledBill(), BillType.PharmacyGrnReturn, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsReturnP.addAll(getBills(new BilledBill(), BillType.PurchaseReturn, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsReturnCancel.addAll(getBills(new CancelledBill(), BillType.PharmacyGrnReturn, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsReturnCancelP.addAll(getBills(new CancelledBill(), BillType.PurchaseReturn, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
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
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getFromInstitution().getChequePrintingName());
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
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), "Cash GRN - Pharmacy", "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getFromInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
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
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill Refund");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getFromInstitution().getChequePrintingName() + " / " + b.getBilledBill().getDeptId());
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
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), "Cash GRN - Pharmacy", "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getFromInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
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
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill Refund");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getToInstitution().getChequePrintingName());
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
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), "Cash GRN - Pharmacy", "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getToInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
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
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getFromInstitution().getChequePrintingName());
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
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), "Cash GRN - Pharmacy", "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getFromInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
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

        for (Department d : getDepartmentrs(Arrays.asList(new BillType[]{BillType.StoreGrnBill, BillType.StoreGrnReturn, BillType.StorePurchase, BillType.PurchaseReturn}), getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate))) {
            System.out.println("d.getName() = " + d.getName());
            billsBilled.addAll(getBills(new BilledBill(), BillType.StoreGrnBill, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsBilledP.addAll(getBills(new BilledBill(), BillType.StorePurchase, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsCanceled.addAll(getBills(new CancelledBill(), BillType.StoreGrnBill, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsCanceledP.addAll(getBills(new CancelledBill(), BillType.StorePurchase, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsReturn.addAll(getBills(new BilledBill(), BillType.StoreGrnReturn, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsReturnP.addAll(getBills(new BilledBill(), BillType.PurchaseReturn, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsReturnCancel.addAll(getBills(new CancelledBill(), BillType.StoreGrnReturn, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
            billsReturnCancelP.addAll(getBills(new CancelledBill(), BillType.PurchaseReturn, d, getInstitution(), getCommonFunctions().getStartOfDay(fromDate), getCommonFunctions().getEndOfDay(fromDate)));
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
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getFromInstitution().getChequePrintingName());
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
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), "Cash GRN - Stores", "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getFromInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
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
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill Refund");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getFromInstitution().getChequePrintingName() + " / " + b.getBilledBill().getDeptId());
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
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), "Cash GRN - Stores", "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getFromInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
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
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill Refund");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getToInstitution().getChequePrintingName());
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
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), "Cash GRN - Stores", "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill Refund", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getToInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
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
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
            QuickBookFormat qbf = new QuickBookFormat();

            qbf.setRowType("SPL");
            qbf.setTrnsType("Bill");
            qbf.setDate(sdf.format(b.getCreatedAt()));
            qbf.setAccnt("INVENTORIES:" + b.getDepartment().getName());
            qbf.setName("");
            qbf.setInvItemType("");
            qbf.setInvItem("");
            qbf.setAmount(0 - b.getTotal());
            qbf.setDocNum(b.getInvoiceNumber());
            qbf.setPoNum(b.getDeptId());
            qbf.setQbClass(b.getDepartment().getName());
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf.setMemo(b.getPaymentMethod().toString() + " / " + sdf.format(b.getInvoiceDate()) + " / " + b.getFromInstitution().getChequePrintingName());
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
                qbf = new QuickBookFormat("SPL", "Bill", sdf.format(b.getCreatedAt()), bi.getItem().getPrintName(), "", "", "", (0 - bi.getNetValue()), b.getInvoiceNumber(), b.getDeptId(), bi.getItem().getName(), bi.getDescreption(), "", "", "", "", "");
                grantTot += bi.getNetValue();
                qbfs.add(qbf);
            }
            if (b.getPaymentMethod() == PaymentMethod.Cash) {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), "Cash GRN - Stores", "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
            } else {
                qbf = new QuickBookFormat("TRNS", "Bill", sdf.format(b.getCreatedAt()), "Accounts Payable:Trade Creditor-" + b.getDepartment().getName(), b.getFromInstitution().getChequePrintingName(), "", "", grantTot, b.getInvoiceNumber(), b.getDeptId(), b.getDepartment().getName(), "", "", "", "", "", "");
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

    public List<QuickBookFormat> fetchOPdListWithProDayEndTable(List<PaymentMethod> paymentMethods, Date fd, Date td, Institution creditCompany) {
        List<QuickBookFormat> qbfs = new ArrayList<>();
        Map temMap = new HashMap();
        String jpql;

        jpql = "select c, "
                + " i, "
                + " count(bi.bill), "
                + " sum(bf.feeValue),"
                + " bi.bill.billClassType "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c "
                + " where bi.bill.institution=:ins "
                + " and bi.bill.billType= :bTp  "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " and bi.bill.paymentMethod in :pms "
                + " and bf.fee.feeType!=:ft "
                + " and bi.bill.retired=false "
                + " and bi.retired=false "
                + " and bf.retired=false ";

        if (creditCompany != null) {
            jpql += " and bi.bill.creditCompany=:cd ";
            temMap.put("cd", creditCompany);

        } else {
            jpql += " and bf.department.institution=:ins ";
        }
        jpql += " group by i.name, bi.bill.billClassType "
                + " order by c.name, i.name, bf.fee.feeType ";

        temMap.put("ft", FeeType.Staff);
        temMap.put("toDate", td);
        temMap.put("fromDate", fd);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pms", paymentMethods);

        List<Object[]> lobjs = getBillFacade().findAggregates(jpql, temMap, TemporalType.TIMESTAMP);
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
            System.out.println("iName = " + i.getName());
            System.out.println("i.getClass() = " + i.getClass());
            System.out.println("bclass = " + bclass);
//            System.out.println("fValue = " + sum);
//            System.out.println("d = " + d);
            if (itemBefore == null) {
                System.err.println("if");
                qbf.setRowType("SPL");
                if (creditCompany != null) {
                    qbf.setTrnsType("CREDIT SALE");
                    qbf.setName("Credit");
                } else {
                    qbf.setTrnsType("CASH SALE");
                    qbf.setName("Cash");
                }

                if (Investigation.class == i.getClass()) {
                    qbf.setAccnt("RHD LAB INCOME:RHD Opd Sale");
                } else {
                    qbf.setAccnt(cat.getDescription());
//                    qbf.setAccnt("INCOME:" + cName);
                    qbf.setEditAccnt(true);
                }
                qbf.setInvItemType("SERV");
                if (i.getName().length() > 30) {
                    qbf.setInvItem(i.getName().substring(0, 30));
                } else {
                    qbf.setInvItem(i.getName());
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
                        qbf.setQbClass("VAT Control Account:Output");
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
                    qbf.setRowType("SPL");
                    if (creditCompany != null) {
                        qbf.setTrnsType("CREDIT SALE");
                        qbf.setName("Credit");
                    } else {
                        qbf.setTrnsType("CASH SALE");
                        qbf.setName("Cash");
                    }
                    if (Investigation.class == i.getClass()) {
                        qbf.setAccnt("RHD LAB INCOME:RHD Opd Sale");
                    } else {
                        qbf.setAccnt(cat.getDescription());
//                        qbf.setAccnt("INCOME:" + cName);
                        qbf.setEditAccnt(true);
                    }
                    qbf.setInvItemType("SERV");
                    if (i.getName().length() > 30) {
                        qbf.setInvItem(i.getName().substring(0, 30));
                    } else {
                        qbf.setInvItem(i.getName());
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
                            qbf.setQbClass("VAT Control Account:Output");
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

    public List<QuickBookFormat> createPharmacySale(BillType bt, Date fd, Date td) {

        List<QuickBookFormat> qbfs = new ArrayList<>();
        List<Object[]> list = getBillBeanController().fetchDepartmentSale(fd, td, getInstitution(), bt);

        for (Object[] obj : list) {

            Department department = ((Department) obj[0]);
            Double value = (Double) obj[1];

            if (value != null) {
                QuickBookFormat qbf = new QuickBookFormat();
                qbf.setRowType("SPL");
                qbf.setTrnsType("CASH SALE");
                qbf.setName("Cash");
                qbf.setAccnt("PHARMACY INCOME:Pharmacy Opd Sale");
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
                qbf.setTrnsType("CASH SALE");
                qbf.setName("Cash");
                if (admissionType.getAdmissionTypeEnum() == AdmissionTypeEnum.DayCase) {
                    qbf.setAccnt("Patient Deposit:OPD Patient Deposit");
                } else {
                    qbf.setAccnt("Patient Deposit:Indoor Patient Deposit");
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
        double cc = getBillBeanController().calBillTotal(BillType.CollectingCentrePaymentReceiveBill, fd, td, getInstitution());

        QuickBookFormat qbf = new QuickBookFormat();
        qbf.setRowType("SPL");
        qbf.setTrnsType("CASH SALE");
        qbf.setName("Cash");
        qbf.setAccnt("Channel Advance");
        qbf.setInvItemType("SERV");

        qbf.setQbClass("Channel");
        qbf.setInvItem("Channel Advance");
        qbf.setMemo("");
        qbf.setAmount(0 - agent);

        grantTot += agent;
        qbfs.add(qbf);

        qbf = new QuickBookFormat();
        qbf.setRowType("SPL");
        qbf.setTrnsType("CASH SALE");
        qbf.setName("Cash");
        qbf.setAccnt("Collecting Center Deposit");
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
        String sql;
        Map m = new HashMap();
        sql = "select bf.billItem.item.category, "
                + " bf.billItem.item, "
                + " count(bf.billItem.bill), "
                + " sum(bf.feeValue),"
                + " bf.billItem.bill.billClassType"
                + " from BillFee bf "
                + " where bf.retired=false "
                + " and bf.bill.patientEncounter.discharged=true "
                + " and bf.bill.patientEncounter.dateOfDischarge between :fd and :td "
                + " and bf.billItem.retired=false "
                + " and bf.fee.feeType!=:ftp "
                + " and bf.bill.billType=:billType ";

        if (pe != null) {
            sql += " and bf.bill.patientEncounter=:bhtno ";
            m.put("bhtno", pe);
        }

        if (admissionType != null) {
            sql = sql + " and bf.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bf.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (cc != null) {
            sql = sql + " and bf.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", cc);
        }

        sql = sql + " group by bf.billItem.item, bf.billItem.bill.billClassType "
                + " order by bf.billItem.item.category.name, bf.billItem.item.name";

        m.put("ftp", FeeType.Staff);
        m.put("billType", BillType.InwardBill);
        m.put("fd", getCommonFunctions().getStartOfDay(fd));
        m.put("td", getCommonFunctions().getEndOfDay(td));
        List<Object[]> results = getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

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
//            System.out.println("fValue = " + sum);
//            System.out.println("d = " + d);
            if (itemBefore == null) {
                System.err.println("if");

                if (paymentMethod == PaymentMethod.Credit) {
                    if (pe != null) {
                        qbf = new QuickBookFormat(cat.getName() + " I", pe.getCreditCompany().getName(), i.getName(), 0 - sum, s);
                    } else {
                        qbf = new QuickBookFormat(cat.getName() + " I", "No Bht", i.getName(), 0 - sum, s);
                    }
                } else {
                    qbf = new QuickBookFormat(cat.getName() + " I", i.getName(), i.getName(), 0 - sum, s);
                }
                if (Investigation.class == i.getClass()) {
                    qbf.setAccnt("RHD LAB INCOME:RHD Inward Sale");
                } else {
//                    qbf.setAccnt(cat.getName() + " I");
                    qbf.setAccnt(cat.getDescription() + " I");
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
                            qbf = new QuickBookFormat(cat.getName() + " I", pe.getCreditCompany().getName(), i.getName(), 0 - sum, s);
                        } else {
                            qbf = new QuickBookFormat(cat.getName() + " I", "No Bht", i.getName(), 0 - sum, s);
                        }
                    } else {
                        qbf = new QuickBookFormat(cat.getName() + " I", i.getName(), i.getName(), 0 - sum, s);
                    }
                    if (Investigation.class == i.getClass()) {
                        qbf.setAccnt("RHD LAB INCOME:RHD Inward Sale");
                    } else {
//                        qbf.setAccnt(cat.getName() + " I");
                        qbf.setAccnt(cat.getDescription() + " I");
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

        m.put("fd", getCommonFunctions().getStartOfDay(fd));
        m.put("td", getCommonFunctions().getEndOfDay(td));

        Object obj[] = getPatientRoomFacade().findAggregateModified(sql, m, TemporalType.TIMESTAMP);

        List<QuickBookFormat> qbfs = new ArrayList<>();
        if (obj != null) {
            System.out.println("obj.length = " + obj.length);
            QuickBookFormat qbf;
            if (paymentMethod == PaymentMethod.Credit) {
                if (pe != null) {
                    qbf = new QuickBookFormat();
                    qbf = new QuickBookFormat(InwardChargeType.RoomCharges.toString(), pe.getCreditCompany().getName(), InwardChargeType.RoomCharges.toString(), (double) obj[0], pe.getCurrentPatientRoom().getRoomFacilityCharge().getDepartment().getName());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat(InwardChargeType.MaintainCharges.toString(), pe.getCreditCompany().getName(), InwardChargeType.RoomCharges.toString(), (double) obj[1], pe.getCurrentPatientRoom().getRoomFacilityCharge().getDepartment().getName());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat(InwardChargeType.MOCharges.toString(), pe.getCreditCompany().getName(), InwardChargeType.RoomCharges.toString(), (double) obj[2], pe.getCurrentPatientRoom().getRoomFacilityCharge().getDepartment().getName());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat(InwardChargeType.NursingCharges.toString(), pe.getCreditCompany().getName(), InwardChargeType.RoomCharges.toString(), (double) obj[3], pe.getCurrentPatientRoom().getRoomFacilityCharge().getDepartment().getName());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat(InwardChargeType.LinenCharges.toString(), pe.getCreditCompany().getName(), InwardChargeType.RoomCharges.toString(), (double) obj[4], pe.getCurrentPatientRoom().getRoomFacilityCharge().getDepartment().getName());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat(InwardChargeType.AdministrationCharge.toString(), pe.getCreditCompany().getName(), InwardChargeType.RoomCharges.toString(), (double) obj[5], pe.getCurrentPatientRoom().getRoomFacilityCharge().getDepartment().getName());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                    qbf = new QuickBookFormat(InwardChargeType.MedicalCareICU.toString(), pe.getCreditCompany().getName(), InwardChargeType.RoomCharges.toString(), (double) obj[6], pe.getCurrentPatientRoom().getRoomFacilityCharge().getDepartment().getName());
                    if (qbf.getAmount() != 0.0) {
                        qbfs.add(qbf);
                    }
                }
            } else {
                qbf = new QuickBookFormat();
                qbf = new QuickBookFormat(InwardChargeType.RoomCharges.toString(), "??", InwardChargeType.RoomCharges.toString(), (double) obj[0], "Ward");
                if (qbf.getAmount() != 0.0) {
                    qbfs.add(qbf);
                }
                qbf = new QuickBookFormat(InwardChargeType.MaintainCharges.toString(), "??", InwardChargeType.RoomCharges.toString(), (double) obj[1], "Ward");
                if (qbf.getAmount() != 0.0) {
                    qbfs.add(qbf);
                }
                qbf = new QuickBookFormat(InwardChargeType.MOCharges.toString(), "??", InwardChargeType.RoomCharges.toString(), (double) obj[2], "Ward");
                if (qbf.getAmount() != 0.0) {
                    qbfs.add(qbf);
                }
                qbf = new QuickBookFormat(InwardChargeType.NursingCharges.toString(), "??", InwardChargeType.RoomCharges.toString(), (double) obj[3], "Ward");
                if (qbf.getAmount() != 0.0) {
                    qbfs.add(qbf);
                }
                qbf = new QuickBookFormat(InwardChargeType.LinenCharges.toString(), "??", InwardChargeType.RoomCharges.toString(), (double) obj[4], "Ward");
                if (qbf.getAmount() != 0.0) {
                    qbfs.add(qbf);
                }
                qbf = new QuickBookFormat(InwardChargeType.AdministrationCharge.toString(), "??", InwardChargeType.RoomCharges.toString(), (double) obj[5], "Ward");
                if (qbf.getAmount() != 0.0) {
                    qbfs.add(qbf);
                }
                qbf = new QuickBookFormat(InwardChargeType.MedicalCareICU.toString(), "??", InwardChargeType.RoomCharges.toString(), (double) obj[6], "Ward");
                if (qbf.getAmount() != 0.0) {
                    qbfs.add(qbf);
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
        String sql = "Select b.staff.speciality,"
                + " sum(b.feeValue) "
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

        sql += " group by b.staff.speciality "
                + " order by b.staff.speciality.name ";

        m.put("refType1", BillType.InwardBill);
        m.put("refType2", BillType.InwardProfessional);
        m.put("fd", getCommonFunctions().getStartOfDay(fd));
        m.put("td", getCommonFunctions().getEndOfDay(td));

        List<Object[]> objects = billFeeFacade.findAggregates(sql, m, TemporalType.TIMESTAMP);

        List<QuickBookFormat> qbfs = new ArrayList<>();
        for (Object[] o : objects) {
            Speciality sp = (Speciality) o[0];
            double value = (double) o[1];
            QuickBookFormat qbf = new QuickBookFormat(sp.getName(), sp.getName(), sp.getName(), value, sp.getName());
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
        String sql = "SELECT i.item,"
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

        sql += " group by i.item "
                + " order by i.item.name";

        m.put("fd", getCommonFunctions().getStartOfDay(fd));
        m.put("td", getCommonFunctions().getEndOfDay(td));

        List<Object[]> results = getBillFeeFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

        List<QuickBookFormat> qbfs = new ArrayList<>();
        for (Object[] obj : results) {
            Item item = (Item) obj[0];
            double value = (double) obj[1];
            QuickBookFormat qbf = new QuickBookFormat(item.getName(), item.getName(), item.getName(), value, item.getName());
            grantTot += value;
            qbfs.add(qbf);
        }
        System.out.println("qbfs.size(Time Service) = " + qbfs.size());
        return qbfs;
    }

    public List<QuickBookFormat> fetchInwardOtherService(Date fd, Date td, PatientEncounter pe, AdmissionType admissionType, PaymentMethod paymentMethod, Institution cc) {

        List<QuickBookFormat> qbfs = new ArrayList<>();
        double d = fetchAdmissionFeeValues(fd, td, pe, admissionType, paymentMethod, cc);
        grantTot += d;
        QuickBookFormat qbf = new QuickBookFormat(InwardChargeType.AdmissionFee.getLabel(), InwardChargeType.AdmissionFee.getLabel(), InwardChargeType.AdmissionFee.getLabel(), d, InwardChargeType.AdmissionFee.getLabel());
        if (qbf.getAmount() != 0.0) {
            qbfs.add(qbf);
        }
        d = fetchIssue(fd, td, pe, admissionType, paymentMethod, cc, BillType.PharmacyBhtPre);
        grantTot += d;
        qbf = new QuickBookFormat(InwardChargeType.Medicine.getLabel(), InwardChargeType.Medicine.getLabel(), InwardChargeType.Medicine.getLabel(), d, InwardChargeType.Medicine.getLabel());
        if (qbf.getAmount() != 0.0) {
            qbfs.add(qbf);
        }
        d = fetchIssue(fd, td, pe, admissionType, paymentMethod, cc, BillType.StoreBhtPre);
        grantTot += d;
        qbf = new QuickBookFormat(InwardChargeType.GeneralIssuing.getLabel(), InwardChargeType.GeneralIssuing.getLabel(), InwardChargeType.GeneralIssuing.getLabel(), d, InwardChargeType.GeneralIssuing.getLabel());
        if (qbf.getAmount() != 0.0) {
            qbfs.add(qbf);
        }
        d = fetchOutSideFee(fd, td, pe, admissionType, paymentMethod, cc);
        grantTot += d;
        qbf = new QuickBookFormat("Out Side Charges", "Out Side Charges", "Out Side Charges", d, "Out Side Charges");
        if (qbf.getAmount() != 0.0) {
            qbfs.add(qbf);
        }
        System.out.println("qbfs.size(Other Service) = " + qbfs.size());
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
        m.put("fd", getCommonFunctions().getStartOfDay(fd));
        m.put("td", getCommonFunctions().getEndOfDay(td));
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

        m.put("fd", getCommonFunctions().getStartOfDay(fd));
        m.put("td", getCommonFunctions().getEndOfDay(td));
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

        m.put("fd", getCommonFunctions().getStartOfDay(fd));
        m.put("td", getCommonFunctions().getEndOfDay(td));
        m.put("inwTp", InwardChargeType.AdmissionFee);
        m.put("btp", BillType.InwardFinalBill);

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
                + " and b.retired=false ";

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

        creditCompanies = getInstitutionFacade().findBySQL(jpql, temMap, TemporalType.TIMESTAMP);
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

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

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

        return getDepartmentFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<PatientEncounter> fetchPatientEncounters(Date fd, Date td, PaymentMethod paymentMethod, List<AdmissionType> admissionTypes, Institution cc) {
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

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (cc != null) {
            sql += " and b.patientEncounter.creditCompany=:cc ";
            m.put("cc", cc);
        }

        sql += " order by b.patientEncounter.bhtNo";

        m.put("fd", getCommonFunctions().getStartOfDay(fd));
        m.put("td", getCommonFunctions().getEndOfDay(td));
        m.put("billType", BillType.InwardFinalBill);

        pes = getPatientEncounterFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

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
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay();
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

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
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

}
