package com.divudi.ejb;

import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.BillListWithTotals;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Stateless
public class BillEjb {

    /**
     * EJBs
     */
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFeeFacade billFeeFacade;

    public BillListWithTotals findBillsAndTotals(Date fromDate, Date toDate, BillType[] billTypes,
            Class[] billClasses,
            Department department,
            Institution institution,
            PaymentMethod[] paymentMethods) {
        return findBillsAndTotals(fromDate, toDate, billTypes, billClasses, department, null, null, institution, null, null, paymentMethods, null, null);
    }
    public BillListWithTotals findBillsAndTotals(Date fromDate, Date toDate,
            Class[] billClasses,
            Department department,
            Institution institution) {
        return findBillsAndTotals(fromDate, toDate, null, billClasses, department, null, null, institution, null, null, null, null, billClasses);
    }
    

    public BillListWithTotals findBillsAndTotals(Date fromDate, Date toDate, BillType[] billTypes,
            Class[] billClasses,
            Department department, Department toDepartment, Department fromDepartment,
            Institution institution, Institution toInstitution, Institution fromInstitution,
            PaymentMethod[] paymentMethods,
            BillType[] billTypesToExculde,
            Class[] billCLassesToExclude) {
        System.out.println("findBillBills");
        String sql;
        Map m = new HashMap();

        sql = "Select b from Bill b ";
        sql += " where b.createdAt between :fd and :td ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        if (paymentMethods != null) {
            List<PaymentMethod> lpms = Arrays.asList(paymentMethods);
            sql += " and b.paymentMethod in :pms ";
            m.put("pms", lpms);
        }
        if (billTypes != null) {
            List<BillType> lbtps = Arrays.asList(billTypes);
            sql += " and b.billType in :btps ";
            m.put("btps", lbtps);
        }

        if (billClasses != null) {
            List<Class> lbcs = Arrays.asList(billClasses);
            sql += " and type(b) in :bcs ";
            m.put("bcs", lbcs);
        }
        if (billTypesToExculde != null) {
            List<BillType> lbtps = Arrays.asList(billTypesToExculde);
            sql += " and b.billType not in :btpse ";
            m.put("btpse", lbtps);
        }
        if (billCLassesToExclude != null) {
            List<Class> lbcs = Arrays.asList(billCLassesToExclude);
            sql += " and type(b) not in :bcse ";
            m.put("bcse", lbcs);
        }
        if (department != null) {
            sql += " and b.department =:dept ";
            m.put("dept", department);
        }
        if (toDepartment != null) {
            sql += " and b.toDepartment =:tdept ";
            m.put("tdept", toDepartment);
        }
        if (fromDepartment != null) {
            sql += " and b.fromDepartment =:fdept ";
            m.put("fdept", fromDepartment);
        }

        if (institution != null) {
            sql += " and (b.institution =:ins or b.department.institution =:ins) ";
            m.put("ins", institution);
        }
        if (toInstitution != null) {
            sql += " and (b.toInstitution =:tins or b.toDepartment.institution =:tins) ";
            m.put("tins", toInstitution);
        }

        if (fromInstitution != null) {
            sql += " and (b.fromInstitution =:fins or b.fromDepartment.institution =:fins) ";
            m.put("fins", fromInstitution);
        }

        System.out.println("m = " + m);
        System.out.println("sql = " + sql);
        BillListWithTotals r = new BillListWithTotals();
        r.setBills(getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP));
        System.out.println("r.getBills() = " + r.getBills());
        if (r.getBills() != null) {
            for (Bill b : r.getBills()) {
                r.setDiscount(r.getDiscount() + b.getDiscount());
                r.setNetTotal(r.getNetTotal() + b.getNetTotal());
                r.setGrossTotal(r.getGrossTotal() + b.getTotal());
            }
        } else {
            r.setBills(new ArrayList<Bill>());
            r.setDiscount(null);
            r.setNetTotal(null);
            r.setGrossTotal(null);
        }
        return r;
    }

    public double findBillItemRevenue(Date fromDate, Date toDate, BillType[] billTypes,
            Class[] billCLasses, Department department, Institution institution,
            Category category, PaymentMethod[] paymentMethods,
            BillType[] billTypesToExculde,
            Class[] billCLassesToExclude) {
        System.out.println("findBillItemRevenue");
        double answer = 0.0;
        String sql;
        Map m = new HashMap();
        if (answer != 0.0) {
            BillItem bf = new BillItem();
            bf.getBill().getCreatedAt();
            bf.getBill().getBillType();
            bf.getBill().getDepartment();
            bf.getBill().getPaymentMethod();
            bf.getItem().getDepartment();
            bf.getItem().getCategory().getParentCategory();
            bf.getNetValue();
        }
        sql = "Select sum(bf.netValue) ";
        sql += " from BillItem bf ";
        sql += " where bf.bill.createdAt between :fd and :td ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        if (paymentMethods != null) {
            List<PaymentMethod> lpms = Arrays.asList(paymentMethods);
            sql += " and bf.bill.paymentMethod in :pms ";
            m.put("pms", lpms);
        }
        if (billTypes != null) {
            List<BillType> lbtps = Arrays.asList(billTypes);
            sql += " and bf.bill.billType in :btps ";
            m.put("btps", lbtps);
        }
        if (billCLasses != null) {
            List<Class> lbcs = Arrays.asList(billCLasses);
            sql += " and type(bf.bill) in :bcs ";
            m.put("bcs", lbcs);
        }
        if (billTypesToExculde != null) {
            List<BillType> lbtps = Arrays.asList(billTypesToExculde);
            sql += " and bf.bill.billType not in :btpse ";
            m.put("btpse", lbtps);
        }
        if (billCLassesToExclude != null) {
            List<Class> lbcs = Arrays.asList(billCLassesToExclude);
            sql += " and type(bf.bill) not in :bcse ";
            m.put("bcse", lbcs);
        }

        if (department != null) {
            sql += " and bf.bill.department =:dept";
            m.put("dept", department);
        }

        if (institution != null) {
            sql += " and (bf.bill.institution =:ins or bf.bill.department.institution =:ins)";
            m.put("ins", institution);
        }

        if (category != null) {
            sql += " and (bf.item.category=:cat or "
                    + "bf.item.category.parentCategory=:cat)";
            m.put("cat", category);
        }
        System.out.println("sql = " + sql);
        System.out.println("m = " + m);
        answer = getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
        return answer;
    }

    /**
     * Bill Facade
     *
     * @return
     */
    public BillFacade getBillFacade() {
        return billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

}
