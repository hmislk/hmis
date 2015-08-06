package com.divudi.ejb;

import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.BillListWithTotals;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.RefundBill;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientInvestigationFacade;
import java.io.Serializable;
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
public class BillEjb implements Serializable {

    /**
     * EJBs
     */
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    PatientInvestigationFacade piFacade;

    public BillListWithTotals findBillsAndTotals(Date fromDate, Date toDate, BillType[] billTypes,
            Class[] billClasses,
            Department department,
            Institution institution,
            PaymentMethod[] paymentMethods) {
        return findBillsAndTotals(fromDate, toDate, billTypes, billClasses, department, null, null, institution, null, null, paymentMethods, null, null);
    }

    /**
     *
     * @param item
     * @param fromDate
     * @param toDate
     * @param billTypes
     * @param classes
     * @param allBilledInstitutions
     * @param billedInstitution
     * @param allBilledDepartments
     * @param billedDepartment
     * @param allItemInstitutions
     * @param itemInstitution
     * @param allItemDepartments
     * @param itemDepartment
     * @return
     */
    public long getBillItemCount(Item item, Date fromDate,
            Date toDate,
            BillType[] billTypes,
            Class[] classes,
            boolean allBilledInstitutions,
            Institution billedInstitution,
            boolean allBilledDepartments,
            Department billedDepartment,
            boolean allItemInstitutions,
            Institution itemInstitution,
            boolean allItemDepartments,
            Department itemDepartment
    ) {
        List<Class> arrayClasses = Arrays.asList(classes);
        List<BillType> arrayBillTypes = Arrays.asList(billTypes);
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi "
                + " where bi.bill.billType in :bts "
                + " and bi.item =:itm"
                + " and type(bi.bill) in :bcs "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("itm", item);
        temMap.put("bcs", arrayClasses);
        temMap.put("bts", arrayBillTypes);

        if (!allBilledInstitutions) {
            sql += " and (bi.bill.institution=:bins or bi.bill.department.institution=:bins ) ";
            temMap.put("bins", billedInstitution);
        }

        if (!allItemInstitutions) {
            sql += " and (bi.item.institution=:iins or bi.item.department.institution=:iins ) ";
            temMap.put("iins", itemInstitution);
        }

        if (!allBilledDepartments) {
            sql += " and (bi.bill.department=:bdep ) ";
            temMap.put("bdep", billedDepartment);
        }

        if (!allItemDepartments) {
            sql += " and (bi.item.department=:idep ) ";
            temMap.put("idep", itemDepartment);
        }
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);
    }

    
    public List<PatientInvestigation> getPatientInvestigations(Item item, Date fromDate,
            Date toDate,
            BillType[] billTypes,
            Class[] classes,
            boolean allBilledInstitutions,
            Institution billedInstitution,
            boolean allBilledDepartments,
            Department billedDepartment,
            boolean allItemInstitutions,
            Institution itemInstitution,
            boolean allItemDepartments,
            Department itemDepartment
    ) {
        List<Class> arrayClasses = Arrays.asList(classes);
        List<BillType> arrayBillTypes = Arrays.asList(billTypes);
        String sql;
        Map temMap = new HashMap();
        sql = "select pi FROM PatientInvestigation pi join pi.billItem bi "
                + " where bi.bill.billType in :bts "
                + " and bi.item =:itm"
                + " and type(bi.bill) in :bcs "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("itm", item);
        temMap.put("bcs", arrayClasses);
        temMap.put("bts", arrayBillTypes);

        if (!allBilledInstitutions) {
            sql += " and (bi.bill.institution=:bins or bi.bill.department.institution=:bins ) ";
            temMap.put("bins", billedInstitution);
        }

        if (!allItemInstitutions) {
            sql += " and (bi.item.institution=:iins or bi.item.department.institution=:iins ) ";
            temMap.put("iins", itemInstitution);
        }

        if (!allBilledDepartments) {
            sql += " and (bi.bill.department=:bdep ) ";
            temMap.put("bdep", billedDepartment);
        }

        if (!allItemDepartments) {
            sql += " and (bi.item.department=:idep ) ";
            temMap.put("idep", itemDepartment);
        }
        System.out.println("temMap = " + temMap);
        System.out.println("sql = " + sql);
        return piFacade.findBySQL(sql, temMap, TemporalType.TIMESTAMP);
    }

    
    public List<Item> getItemsInBills(Date fromDate,
            Date toDate,
            BillType[] billTypes,
            boolean allBilledInstitutions,
            Institution billedInstitution,
            boolean allBilledDepartments,
            Department billedDepartment,
            boolean allItemInstitutions,
            Institution itemInstitution,
            boolean allItemDepartments,
            Department itemDepartment,
            boolean allItemClasses,
            Class[] itemClasses
    ) {
        Class[] classes = new Class[]{BilledBill.class, CancelledBill.class, RefundBill.class};
        List<Class> arrayClasses = Arrays.asList(classes);
        List<BillType> arrayBillTypes = Arrays.asList(billTypes);
        String sql;
        Map temMap = new HashMap();
        sql = "select distinct(bi.item) FROM BillItem bi "
                + " where bi.bill.billType in :bts "
                + " and type(bi.bill) in :bcs "
                + " and bi.bill.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("bcs", arrayClasses);
        temMap.put("bts", arrayBillTypes);

        if (!allBilledInstitutions) {
            sql += " and (bi.bill.institution=:bins or bi.bill.department.institution=:bins ) ";
            temMap.put("bins", billedInstitution);
        }

        if (!allItemInstitutions) {
            sql += " and (bi.item.institution=:iins or bi.item.department.institution=:iins ) ";
            temMap.put("iins", itemInstitution);
        }

        if (!allBilledDepartments) {
            sql += " and (bi.bill.department=:bdep ) ";
            temMap.put("bdep", billedDepartment);
        }

        if (!allItemDepartments) {
            sql += " and (bi.item.department=:idep ) ";
            temMap.put("idep", itemDepartment);
        }
        
        if(!allItemClasses){
            List<Class> ics = Arrays.asList(itemClasses);
            sql += " and type(bi.item) in :ics ";
            temMap.put("ics", ics);
        }
        System.out.println("temMap = " + temMap);
        System.out.println("sql = " + sql);
        return itemFacade.findBySQL(sql, temMap, TemporalType.TIMESTAMP);
    }

    public BillListWithTotals findBillsAndTotals(Date fromDate, Date toDate, BillType[] billTypes,
            Class[] billClasses,
            Department department, Department toDepartment, Department fromDepartment,
            Institution institution, Institution toInstitution, Institution fromInstitution,
            PaymentMethod[] paymentMethods,
            BillType[] billTypesToExculde,
            Class[] billCLassesToExclude) {
        //System.out.println("findBillBills");
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

        //System.out.println("m = " + m);
        //System.out.println("sql = " + sql);
        //System.out.println("before r");
        BillListWithTotals r = new BillListWithTotals();
        //System.out.println("r = " + r);
        List<Bill> bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

        //System.out.println("r.getBills().size() = " + r.getBills().size());
        //System.out.println("r = " + r);
        r.setBills(bills);

        if (r.getBills() != null) {
            //System.out.println("bills not null");
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

    public BillListWithTotals calculateBillTotals(List<Bill> bills) {
        BillListWithTotals bt = new BillListWithTotals();
        //System.out.println("bills = " + bills);
        if (bills == null) {
            return bt;
        }
        bt.setGrossTotal(0.0);
        bt.setDiscount(0.0);
        bt.setNetTotal(0.0);
        for (Bill b : bills) {
            bt.setGrossTotal(bt.getGrossTotal() + b.getTotal());
            bt.setDiscount(bt.getDiscount() + b.getDiscount());
            bt.setNetTotal(bt.getNetTotal() + b.getNetTotal());
        }
        return bt;
    }

    public double findBillItemRevenue(Date fromDate, Date toDate, BillType[] billTypes,
            Class[] billCLasses, Department department, Institution institution,
            Category category, PaymentMethod[] paymentMethods,
            BillType[] billTypesToExculde,
            Class[] billCLassesToExclude) {
        //System.out.println("findBillItemRevenue");
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
