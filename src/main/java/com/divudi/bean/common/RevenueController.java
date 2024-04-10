/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.StringsDoublesRow;
import com.divudi.ejb.RevenueBean;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Buddhika
 */
@Named(value = "revenueController")
@SessionScoped
public class RevenueController implements Serializable {

    /**
     * EJBs
     */
    @EJB
    RevenueBean revenueBean;
    /**
     * Managed Beans
     */
    @Inject
    InstitutionController institutionController;
    @Inject
    DepartmentController departmentController;
    @Inject
    CommonController commonController;
    /**
     * Properties
     */
    Date fromDate;
    Date toDate;
    Department department;
    Institution institution;
    List<StringsDoublesRow> rows;
    private int revenueReportIndex;
    double grandTotal = 0.0;
    double cashTotal = 0.0;
    double creditTotal = 0.0;
    double creditCardTotal = 0.0;
    double chequeTotal = 0.0;

    /**
     * Functions
     */
    public void fillRevenueSummery() {
        Date startTime = new Date();

        ////// // System.out.println("fillRevenueSummery ");
        rows = new ArrayList<>();
        List<Institution> institutions = getInstitutionController().getCompanies();
        ////// // System.out.println("institutions = " + institutions);
        StringsDoublesRow r = new StringsDoublesRow();
        grandTotal = 0.0;
        for (Institution ins : institutions) {
            ////// // System.out.println("ins = " + ins.getName());
            r = new StringsDoublesRow();
            r.setStr1(ins.getName());
            r.setBoldStr1(true);
            List<Department> depts = getDepartmentController().getInstitutionDepatrments(ins);
            double insTotal = 0.0;
            for (Department dept : depts) {
                ////// // System.out.println("dept = " + dept.getName());
                ////// // System.out.println("r.getStr1() = " + r.getStr1());
                double feeRevenue = getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, null, dept, null, null, null, null, null);
                Class[] billClassesToExclude = new Class[]{PreBill.class};
                BillType[] billTypes = new BillType[]{BillType.PharmacySale, BillType.StoreSale};
                double productRevenue = getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, null, dept, null, null, null, null, billClassesToExclude);
                r.setStr2(dept.getName());
                r.setDbl1(feeRevenue + productRevenue);
                ////// // System.out.println("feeRevenue + productRevenue = " + feeRevenue + productRevenue);
                insTotal = insTotal + feeRevenue + productRevenue;
                rows.add(r);
                r = new StringsDoublesRow();
            }
            r = new StringsDoublesRow();
            r.setStr2(ins.getName() + " subtotal");
            r.setDbl1(insTotal);
            r.setBoldStr2(true);
            r.setBoldDbl1(true);
            rows.add(r);
            r = new StringsDoublesRow();
            grandTotal += insTotal;
        }

        

    }

    public void fillRevenueReport() {
        Date startTime = new Date();
        
        ////// // System.out.println("fillRevenueReport");
        rows = new ArrayList<>();
        List<Institution> institutions = getInstitutionController().getCompanies();
        ////// // System.out.println("institutions = " + institutions);
        StringsDoublesRow r = new StringsDoublesRow();
        grandTotal = 0.0;
        cashTotal = 0.0;
        creditTotal = 0.0;
        creditCardTotal = 0.0;
        chequeTotal = 0.0;
        for (Institution ins : institutions) {
            double insTotal = 0.0;
            double insCash = 0.0;
            double insCredit = 0.0;
            double insCard = 0.0;
            double insCheque = 0.0;
            ////// // System.out.println("ins = " + ins.getName());
            r = new StringsDoublesRow();
            r.setStr1(ins.getName());
            r.setBoldStr1(true);
            List<Department> depts = getDepartmentController().getInstitutionDepatrments(ins);

            for (Department dept : depts) {
                double depTotal = 0.0;
                double depCash = 0.0;
                double depCredit = 0.0;
                double depCard = 0.0;
                double depCheque = 0.0;

                r.setStr2(dept.getName());
                Class[] billClassesToExclude = new Class[]{PreBill.class};
                BillType[] billTypes = new BillType[]{BillType.PharmacySale, BillType.StoreSale};
                //
                // Billed
                //
                r.setStr3("Billed");
                Class[] billedClasses = new Class[]{BilledBill.class};
                //Cash - Billed
                PaymentMethod[] cashs = new PaymentMethod[]{PaymentMethod.Cash};
                r.setDbl1(getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, billedClasses, dept, null, null, cashs, null, null)
                        + getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, billedClasses, dept, null, null, cashs, null, billClassesToExclude));
                depCash += r.getDbl1();

                //Credits - Billed
                PaymentMethod[] credits = new PaymentMethod[]{PaymentMethod.Credit};
                r.setDbl2(getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, billedClasses, dept, null, null, credits, null, null)
                        + getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, billedClasses, dept, null, null, credits, null, billClassesToExclude));
                depCredit += r.getDbl2();

                //Card - Billed
                PaymentMethod[] cards = new PaymentMethod[]{PaymentMethod.Card};
                r.setDbl3(getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, billedClasses, dept, null, null, cards, null, null)
                        + getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, billedClasses, dept, null, null, cards, null, billClassesToExclude));
                depCard += r.getDbl3();

                //Cheque - Billed
                PaymentMethod[] cheques = new PaymentMethod[]{PaymentMethod.Cheque};
                r.setDbl4(getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, billedClasses, dept, null, null, cheques, null, null)
                        + getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, billedClasses, dept, null, null, cheques, null, billClassesToExclude));
                depCheque += r.getDbl4();

                rows.add(r);
                r = new StringsDoublesRow();

                //
                // Cancelled
                //
                r.setStr3("Cancelled");
                Class[] cancelledClasses = new Class[]{CancelledBill.class};
                //Cash - Billed
                r.setDbl1(getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, cancelledClasses, dept, null, null, cashs, null, null)
                        + getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, cancelledClasses, dept, null, null, cashs, null, billClassesToExclude));
                depCash += r.getDbl1();

                //Credits - Billed
                r.setDbl2(getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, cancelledClasses, dept, null, null, credits, null, null)
                        + getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, cancelledClasses, dept, null, null, credits, null, billClassesToExclude));
                depCredit += r.getDbl2();

                //Card - Billed
                r.setDbl3(getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, cancelledClasses, dept, null, null, cards, null, null)
                        + getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, cancelledClasses, dept, null, null, cards, null, billClassesToExclude));
                depCard += r.getDbl3();

                //Cheque - Billed
                r.setDbl4(getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, cancelledClasses, dept, null, null, cheques, null, null)
                        + getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, cancelledClasses, dept, null, null, cheques, null, billClassesToExclude));
                depCheque += r.getDbl4();

                rows.add(r);
                r = new StringsDoublesRow();

                //
                // Refunded
                //
                r.setStr3("Refund");
                Class[] refundedClasses = new Class[]{RefundBill.class};

                //Cash - Billed
                r.setDbl1(getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, refundedClasses, dept, null, null, cashs, null, null)
                        + getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, refundedClasses, dept, null, null, cashs, null, billClassesToExclude));
                depCash += r.getDbl1();
                //Credits - Billed
                r.setDbl2(getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, refundedClasses, dept, null, null, credits, null, null)
                        + getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, refundedClasses, dept, null, null, credits, null, billClassesToExclude));
                depCredit += r.getDbl2();
                //Card - Billed
                r.setDbl3(getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, refundedClasses, dept, null, null, cards, null, null)
                        + getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, refundedClasses, dept, null, null, cards, null, billClassesToExclude));
                depCard += r.getDbl3();
                //Cheque - Billed
                r.setDbl4(getRevenueBean().findBillFeeRevenue(fromDate, toDate, null, refundedClasses, dept, null, null, cheques, null, null)
                        + getRevenueBean().findBillItemRevenue(fromDate, toDate, billTypes, refundedClasses, dept, null, null, cheques, null, billClassesToExclude));
                depCheque += r.getDbl4();
                rows.add(r);
                r = new StringsDoublesRow();
                //
                // Expenses
                //
                r.setStr3("Expenses");

                rows.add(r);
                r = new StringsDoublesRow();
                //TODO:

                //
                // Expenses
                //
                r.setStr3(dept.getName() + " Subtotal");
                r.setBoldStr3(true);
                r.setDbl1(depCash);
                r.setBoldDbl1(true);
                r.setDbl2(depCredit);
                r.setBoldDbl2(true);
                r.setDbl3(depCard);
                r.setBoldDbl3(true);
                r.setDbl4(depCheque);
                r.setBoldDbl4(true);

                rows.add(r);
                r = new StringsDoublesRow();

                insCash += depCash;
                insCredit += depCredit;
                insCard += depCard;
                insCheque += depCheque;
            }

            r = new StringsDoublesRow();
            r.setStr2(ins.getName() + " subtotal");
            r.setDbl1(insCash);
            r.setDbl2(insCredit);
            r.setDbl3(insCard);
            r.setDbl4(insCheque);

            r.setBoldStr2(true);
            r.setBoldDbl1(true);
            r.setBoldDbl2(true);
            r.setBoldDbl3(true);
            r.setBoldDbl4(true);

            rows.add(r);
            r = new StringsDoublesRow();

            cashTotal += insCash;
            creditTotal += insCredit;
            creditCardTotal += insCard;
            chequeTotal += insCheque;

        }

        grandTotal = cashTotal + creditTotal + creditCardTotal + chequeTotal;

        
        

    }

    /**
     * Constructors
     */
    public RevenueController() {
    }

    /**
     * Getters & Setters
     *
     * @return
     */
    public RevenueBean getRevenueBean() {
        return revenueBean;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth(toDate);
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = new Date();
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

    public List<StringsDoublesRow> getRows() {
        return rows;
    }

    public void setRows(List<StringsDoublesRow> rows) {
        this.rows = rows;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public DepartmentController getDepartmentController() {
        return departmentController;
    }

    public void setDepartmentController(DepartmentController departmentController) {
        this.departmentController = departmentController;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(double grandTotal) {
        this.grandTotal = grandTotal;
    }

    public double getCashTotal() {
        return cashTotal;
    }

    public void setCashTotal(double cashTotal) {
        this.cashTotal = cashTotal;
    }

    public double getCreditTotal() {
        return creditTotal;
    }

    public void setCreditTotal(double creditTotal) {
        this.creditTotal = creditTotal;
    }

    public double getCreditCardTotal() {
        return creditCardTotal;
    }

    public void setCreditCardTotal(double creditCardTotal) {
        this.creditCardTotal = creditCardTotal;
    }

    public double getChequeTotal() {
        return chequeTotal;
    }

    public void setChequeTotal(double chequeTotal) {
        this.chequeTotal = chequeTotal;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public int getRevenueReportIndex() {
        return revenueReportIndex;
    }

    public void setRevenueReportIndex(int revenueReportIndex) {
        this.revenueReportIndex = revenueReportIndex;
    }

    
}

