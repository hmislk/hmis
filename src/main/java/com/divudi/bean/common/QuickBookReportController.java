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
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.Category;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Service;
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

        List<PaymentMethod> paymentMethods = Arrays.asList(PaymentMethod.Cash, PaymentMethod.Cheque, PaymentMethod.Slip, PaymentMethod.Card);
        fetchOPdListWithProDayEndTable(paymentMethods, commonFunctions.getStartOfDay(fromDate), commonFunctions.getEndOfDay(fromDate));
    }

    public void createQBFormatInwardIncome() {
        quickBookFormats=new ArrayList<>();
        for (Institution i : fetchCreditCompany(toDate, toDate)) {
            quickBookFormats.addAll(fetchINwardListWithCreditCompany(fromDate, toDate, i));
        }

    }

    public void fetchOPdListWithProDayEndTable(List<PaymentMethod> paymentMethods, Date fd, Date td) {
        Map temMap = new HashMap();
        grantTot = 0.0;
        String jpql = "select c.name, "
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
        QuickBookFormat qbf = new QuickBookFormat();
        quickBookFormats = new ArrayList<>();
        List<QuickBookFormat> qbfs = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        DecimalFormat df = new DecimalFormat("#.00");
        for (Object[] lobj : lobjs) {
            qbf = new QuickBookFormat();
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
            qbf.setAmount(0 - Double.parseDouble(df.format(sum)));
            if (i.getDepartment() != null) {
                System.out.println("si.getDepartment().getPrintingName()) " + i.getDepartment().getName());
                qbf.setQbClass(i.getDepartment().getName());
            } else {
                qbf.setQbClass("No Department");
            }

            qbf.setMemo(i.getName());
            grantTot += sum;
            qbfs.add(qbf);
        }
        qbf = new QuickBookFormat();

        qbf.setRowType("TRNS");
        qbf.setTrnsType("CASH SALE");
        qbf.setDate(sdf.format(fromDate));
        qbf.setAccnt("Cash in Hand/Bank C/A");
        qbf.setName("Cash");
        qbf.setAmount(Double.parseDouble(df.format(grantTot)));
        qbf.setMemo("Sales");
        qbf.setDocNum(sdf.format(fromDate));

        quickBookFormats.add(qbf);
        quickBookFormats.addAll(qbfs);
        qbf = new QuickBookFormat();
        qbf.setRowType("ENDTRNS");
        quickBookFormats.add(qbf);

    }

    public List<QuickBookFormat> fetchINwardListWithCreditCompany(Date fd, Date td, Institution i) {
        Map temMap = new HashMap();
//        Bill b;
        grantTot = 0.0;
        String jpql = "select b.patientEncounter,"
                + "c.name,"
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
        return creditCompanies;
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

}
