/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.report.BookKeepingSummery;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.QuickBookFormat;
import com.divudi.data.inward.AdmissionTypeEnum;
import com.divudi.data.table.String1Value2;
import com.divudi.data.table.String3Value2;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Service;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.facade.BillFacade;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.ItemFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
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
    BillFacade billFacade;
    @EJB
    CommonFunctions commonFunctions;

    @Inject
    BillBeanController billBeanController;

    private List<QuickBookFormat> quickBookFormats;
    private List<Category> categorys;
    private List<Institution> creditCompanies;
    private Institution institution;
    private Date toDate;
    private Date fromDate;

    private double grantTot;

    public QuickBookReportController() {
    }

    public void createQBFormatOpdDayIncome() {

        grantTot = 0.0;
        quickBookFormats = new ArrayList<>();
        List<QuickBookFormat> qbfs = new ArrayList<>();

        List<PaymentMethod> paymentMethods = Arrays.asList(PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card);
        qbfs.addAll(fetchOPdListWithProDayEndTable(paymentMethods, commonFunctions.getStartOfDay(fromDate), commonFunctions.getEndOfDay(fromDate)));
        qbfs.addAll(createPharmacySale(BillType.PharmacySale, commonFunctions.getStartOfDay(fromDate), commonFunctions.getEndOfDay(fromDate)));
        qbfs.addAll(createInwardCollection(commonFunctions.getStartOfDay(fromDate), commonFunctions.getEndOfDay(fromDate)));
        qbfs.addAll(createAgencyAndCollectionCenterTotal(commonFunctions.getStartOfDay(fromDate), commonFunctions.getEndOfDay(fromDate)));

        QuickBookFormat qbf = new QuickBookFormat();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

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

    public void createQBFormatInwardIncome() {
        quickBookFormats = new ArrayList<>();
        for (Institution i : fetchCreditCompany(toDate, toDate)) {
            quickBookFormats.addAll(fetchINwardListWithCreditCompany(fromDate, toDate, i));
        }
        System.out.println("quickBookFormats.size() = " + quickBookFormats.size());

    }

    public List<QuickBookFormat> fetchOPdListWithProDayEndTable(List<PaymentMethod> paymentMethods, Date fd, Date td) {
        List<QuickBookFormat> qbfs = new ArrayList<>();
        Map temMap = new HashMap();
        String jpql;

        jpql = "select c.name, "
                + " i, "
                + " count(bi.bill), "
                + " sum(bf.feeValue) "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c "
                + " where bi.bill.institution=:ins "
                + " and bf.department.institution=:ins "
                + " and bi.bill.billType= :bTp  "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " and bi.bill.paymentMethod in :pms "
                + " and bf.fee.feeType!=:ft "
                + " and bi.bill.retired=false "
                + " and bi.retired=false "
                + " and bf.retired=false ";

//        if (creditCompany != null) {
//            jpql += " and bi.bill.creditCompany=:cd ";
//            temMap.put("cd", creditCompany);
//
////        }
        jpql += " group by  i.name,  bi.bill.billClassType "
                + " order by c.name, i.name, bf.fee.feeType";

        temMap.put("ft", FeeType.Staff);
        temMap.put("toDate", td);
        temMap.put("fromDate", fd);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pms", paymentMethods);

        List<Object[]> lobjs = getBillFacade().findAggregates(jpql, temMap, TemporalType.TIMESTAMP);
        System.out.println("lobjs.size = " + lobjs.size());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (Object[] lobj : lobjs) {
            QuickBookFormat qbf = new QuickBookFormat();
            System.out.println("lobj = " + lobj[0]);
            String cName = (String) lobj[0];
            Item i = (Item) lobj[1];
            double sum = (double) lobj[3];
//            System.out.println("cName = " + cName);
//            System.out.println("iName = " + i);
//            System.out.println("fValue = " + sum);
            qbf.setRowType("SPL");
            qbf.setTrnsType("CASH SALE");
            qbf.setName("Cash");
            qbf.setAccnt(cName);
            qbf.setInvItemType("SERV");
            qbf.setInvItem(i.getName());
            qbf.setAmount(0 - sum);
            if (i.getDepartment() != null) {
                System.out.println("si.getDepartment().getPrintingName()) " + i.getDepartment().getName());
                qbf.setQbClass(i.getDepartment().getName());
            } else {
                qbf.setQbClass("No Department");
            }
            qbf.setEditQbClass(false);
            qbf.setEditAccnt(false);
            qbf.setMemo(i.getName());
            grantTot += sum;
            qbfs.add(qbf);
        }

        return qbfs;

    }

    public List<QuickBookFormat> fetchINwardListWithCreditCompany(Date fd, Date td, Institution i) {
        Map temMap = new HashMap();
//        Bill b;
        grantTot = 0.0;
        String jpql = "select b.patientEncounter,"
                + " c.name, "
                + " i, "
                + " count(b), "
                + " sum(bf.feeValue) "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c join bi.bill b "
                + " where bi.bill.institution=:ins "
                + " and bi.bill.billType= :bTp  "
                + " and b.patientEncounter.dateOfDischarge between :fd and :td "
                + " and b.patientEncounter.discharged=true "
                + " and b.patientEncounter.paymentFinalized=true "
                + " and b.patientEncounter.paymentMethod=:pm "
                + " and bf.fee.feeType!=:ft "
                + " and bi.bill.retired=false "
                + " and bi.retired=false "
                + " and bf.retired=false ";

        if (i != null) {
            jpql += " and bi.bill.creditCompany=:cd ";
            temMap.put("cd", i);

        }

        jpql += " group by b.patientEncounter.bhtNo,c.name,i.name "
                + " order by c.name,i.name";
        temMap.put("ft", FeeType.Staff);
        temMap.put("td", td);
        temMap.put("fd", fd);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.InwardBill);
        temMap.put("pm", PaymentMethod.Credit);

        System.out.println("jpql = " + jpql);
        System.out.println("temMap = " + temMap);

        List<Object[]> lobjs = getBillFacade().findAggregates(jpql, temMap, TemporalType.TIMESTAMP);
        System.out.println("lobjs.size = " + lobjs.size());

        QuickBookFormat qbf;
        quickBookFormats = new ArrayList<>();
        List<QuickBookFormat> formats = new ArrayList<>();
        List<QuickBookFormat> qbfs = new ArrayList<>();
        SimpleDateFormat sdf;
        DecimalFormat df = new DecimalFormat("#.00");
        grantTot = 0.0;
        for (Object[] lobj : lobjs) {
            qbf = new QuickBookFormat();
            PatientEncounter pe = (PatientEncounter) lobj[0];
            String cat = (String) lobj[1];
            Item it = (Item) lobj[2];
            double sum = (double) lobj[4];
            System.out.println("bht = " + pe.getBhtNo());
            System.out.println("creditcom = " + cat);
            System.out.println("it = " + it.getName());
            System.out.println("sum = " + sum);

            qbf.setRowType("SPL");
            qbf.setTrnsType("INVOICE");
            qbf.setAccnt(cat);
            qbf.setName(i.getName());
            qbf.setInvItemType("SERV");
            qbf.setInvItem(i.getName());
            qbf.setAmount(0 - Double.parseDouble(df.format(sum)));
            if (it.getDepartment() != null) {
                System.out.println("si.getDepartment().getPrintingName()) " + it.getDepartment().getName());
                qbf.setQbClass(it.getDepartment().getName());
            } else {
                qbf.setQbClass("No Department");
            }

            qbf.setMemo(it.getName());
            qbf.setCustFld1(pe.getBhtNo());
            sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
            qbf.setCustFld2(sdf.format(pe.getDateOfAdmission()));
            qbf.setCustFld3(sdf.format(pe.getDateOfDischarge()));
            grantTot += sum;
            qbfs.add(qbf);
        }
        sdf = new SimpleDateFormat("yyyy-MM-dd");
        qbf = new QuickBookFormat();
        qbf.setRowType("TRNS");
        qbf.setTrnsType("INVOICE");
        qbf.setDate("????");
        qbf.setAccnt("Trade Debtors");
        qbf.setName(i.getName());
        qbf.setAmount(Double.parseDouble(df.format(grantTot)));
        qbf.setMemo("");
        qbf.setDocNum("????");

        formats.add(qbf);
        formats.addAll(qbfs);

        qbf = new QuickBookFormat();
        qbf.setRowType("ENDTRNS");
        formats.add(qbf);

        return formats;

    }

    public List<Institution> fetchCreditCompany(Date fd, Date td) {
        Map temMap = new HashMap();
        List<Institution> creditCompanies = new ArrayList<>();
        grantTot = 0.0;
        String jpql = " select distinct b.creditCompany "
                + " from BillFee bf join bf.billItem bi join bi.item i join i.category c join bi.bill b "
                + " where bi.bill.institution=:ins "
                + " and bi.bill.billType= :bTp  "
                + " and b.patientEncounter.dateOfDischarge between :fd and :td "
                + " and b.patientEncounter.discharged=true "
                + " and b.patientEncounter.paymentFinalized=true "
                + " and b.patientEncounter.paymentMethod=:pm "
                + " and bf.fee.feeType!=:ft "
                + " and bi.bill.retired=false "
                + " and bi.retired=false "
                + " and bf.retired=false ";

        temMap.put("ft", FeeType.Staff);
        temMap.put("td", td);
        temMap.put("fd", fd);
        temMap.put("ins", institution);
        temMap.put("bTp", BillType.InwardBill);
        temMap.put("pm", PaymentMethod.Credit);

        System.out.println("jpql = " + jpql);
        System.out.println("temMap = " + temMap);

        creditCompanies = getInstitutionFacade().findBySQL(jpql, temMap, TemporalType.DATE);
        System.out.println("creditCompanies.size() = " + creditCompanies.size());
        return creditCompanies;
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
                qbf.setAccnt("Pharmacy Opd Sale");
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

    private List<Bill> getBills(Bill billClass, BillType billType, Department dep, Institution ins, Date fd, Date td) {
        String sql;
        Map temMap = new HashMap();

        sql = "SELECT b FROM Bill b WHERE type(b)=:bill"
                + " and b.retired=false and "
                + " b.billType = :btp "
                + " and b.department=:d "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate ";

        if (institution != null) {
            sql += " and b.fromInstitution=:fIns ";
            temMap.put("fIns", institution);
        }

//        if (getReferenceInstitution() != null) {
//            sql += " and b.referenceInstitution=:ins ";
//            temMap.put("ins", getReferenceInstitution());
//        }
        sql += " order by b.id  ";

        temMap.put("fromDate", fd);
        temMap.put("toDate", td);
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);
        temMap.put("d", dep);
        temMap.put("ins", ins);

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Institution getInstitution() {
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

}
