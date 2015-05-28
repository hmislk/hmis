package com.divudi.ejb;

import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
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
public class RevenueBean {

    /**
     * EJBs
     */
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFeeFacade billFeeFacade;

    public double findBillFeeRevenue(Date fromDate, Date toDate, BillType[] billTypes,
            Class[] billClasses, Department department, Institution institution,
            Category category, PaymentMethod[] paymentMethods,
            BillType[] billTypesToExculde,
            Class[] billCLassesToExclude) {
        //System.out.println("findBillFeeRevenue");
        double answer = 0.0;
        String sql;
        Map m = new HashMap();
        if (answer != 0.0) {
            BillFee bf = new BillFee();
            bf.getBill().getCreatedAt();
            bf.getBill().getBillType();
            bf.getBill().getDepartment();
            bf.getBill().getPaymentMethod();
            bf.getFee().getFeeType();
            bf.getFee().getDepartment();
            bf.getFee().getFeeType();
            bf.getBillItem().getItem().getCategory().getParentCategory();
            bf.getFeeValue();
        }
        sql = "Select sum(bf.feeValue) ";
        sql += " from BillFee bf ";
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

        if (billClasses != null) {
            List<Class> lbcs = Arrays.asList(billClasses);
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
            sql += " and bf.fee.department =:dept";
            m.put("dept", department);
        }

        if (institution != null) {
            sql += " and (bf.fee.institution =:ins or bf.fee.department.institution =:ins)";
            m.put("ins", institution);
        }

        if (category != null) {
            sql += " and (bf.billItem.item.category=:cat or "
                    + "bf.billItem.item.category.parentCategory=:cat)";
            m.put("cat", category);
        }
        //System.out.println("m = " + m);
        //System.out.println("sql = " + sql);
        answer = getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
        return answer;
    }

    public double findBillItemRevenue(Date fromDate, Date toDate, BillType[] billTypes,
            Class[] billCLasses, Department department, Institution institution,
            Category category, PaymentMethod[] paymentMethods,
            BillType[] billTypesToExculde,
            Class[] billCLassesToExclude) {
        //System.out.println("findBillItemRevenue" );
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
        //System.out.println("sql = " + sql);
        //System.out.println("m = " + m);
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
