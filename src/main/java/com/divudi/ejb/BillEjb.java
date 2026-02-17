package com.divudi.ejb;

import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.BillListWithTotals;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
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

    public void save(Bill savingBill, WebUser user) {
        if (savingBill == null) {
            return;
        }
        if (savingBill.getId() == null) {
            savingBill.setCreatedAt(new Date());
            savingBill.setCreater(user);
            try {
                billFacade.create(savingBill);
            } catch (Exception e) {
                billFacade.edit(savingBill);
            }
        } else {
            billFacade.edit(savingBill);
        }
    }

    public BillListWithTotals findBillsAndTotals(Date fromDate, Date toDate, BillType[] billTypes,
            Class[] billClasses,
            Department department,
            Institution institution,
            PaymentMethod[] paymentMethods) {
        return findBillsAndTotals(fromDate, toDate, billTypes, billClasses, department, null, null, institution, null, null, null, paymentMethods, null, null, false, null);
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
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);
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
        return piFacade.findByJpql(sql, temMap, TemporalType.TIMESTAMP);
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

        if (!allItemClasses) {
            List<Class> ics = Arrays.asList(itemClasses);
            sql += " and type(bi.item) in :ics ";
            temMap.put("ics", ics);
        }
        return itemFacade.findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public BillListWithTotals findBillsAndTotals(Date fromDate, Date toDate, BillType[] billTypes,
            Class[] billClasses,
            Department department, Department toDepartment, Department fromDepartment,
            Institution institution, Institution toInstitution, Institution fromInstitution,
            Institution creditCompany,
            PaymentMethod[] paymentMethods,
            BillType[] billTypesToExculde,
            Class[] billCLassesToExclude,
            boolean isInward, AdmissionType admissionType) {
        ////// // System.out.println("findBillBills");
        String sql;
        Map m = new HashMap();

        sql = "Select b from Bill b "
                + " where b.retired=false ";
        if (isInward) {
            sql += " and b.patientEncounter.dateOfDischarge between :fd and :td ";
        } else {
            sql += " and b.createdAt between :fd and :td ";
        }
        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad ";
            m.put("ad", admissionType);
        }
        if (paymentMethods != null) {
            List<PaymentMethod> lpms = Arrays.asList(paymentMethods);
            if (isInward) {
                sql += " and b.patientEncounter.paymentMethod in :pms ";
            } else {
                sql += " and b.paymentMethod in :pms ";
            }
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
            sql += " and b.toInstitution=:tins ";
//            sql += " and (b.toInstitution=:tins or b.toDepartment.institution=:tins) ";
            m.put("tins", toInstitution);
        }

        if (fromInstitution != null) {
            sql += " and (b.fromInstitution =:fins or b.fromDepartment.institution =:fins) ";
            m.put("fins", fromInstitution);
        }

        if (creditCompany != null) {
            if (isInward) {
                sql += " and b.patientEncounter.creditCompany=:cc ";
            } else {
                sql += " and b.creditCompany=:cc ";
            }
            m.put("cc", creditCompany);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);
        ////// // System.out.println("before r");
        BillListWithTotals r = new BillListWithTotals();
        ////// // System.out.println("r = " + r);
        List<Bill> bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        ////// // System.out.println("r = " + r);
        r.setBills(bills);

        if (r.getBills() != null) {
            ////// // System.out.println("bills not null");
            for (Bill b : r.getBills()) {
                r.setDiscount(r.getDiscount() + b.getDiscount());
                r.setVat(r.getVat() + b.getVat());
                r.setNetTotal(r.getNetTotal() + b.getNetTotal());
                r.setGrossTotal(r.getGrossTotal() + b.getTotal());
                if (r.getSaleValueTotal() == null) {
                    r.setSaleValueTotal(0.0);
                }
                r.setSaleValueTotal(r.getSaleValueTotal() + b.getSaleValue());
            }
        } else {
            r.setBills(new ArrayList<>());
            r.setDiscount(null);
            r.setNetTotal(null);
            r.setGrossTotal(null);
            r.setVat(null);
        }
        return r;
    }

    public BillListWithTotals calculateBillTotals(List<Bill> bills) {
        BillListWithTotals bt = new BillListWithTotals();
        ////// // System.out.println("bills = " + bills);
        if (bills == null) {
            return bt;
        }
        bt.setGrossTotal(0.0);
        bt.setDiscount(0.0);
        bt.setVat(0.0);
        bt.setNetTotal(0.0);
        for (Bill b : bills) {
            bt.setGrossTotal(bt.getGrossTotal() + b.getTotal());
            bt.setDiscount(bt.getDiscount() + b.getDiscount());
            bt.setNetTotal(bt.getNetTotal() + b.getNetTotal());
            bt.setVat(bt.getVat() + b.getVat());
        }
        return bt;
    }

    public double findBillItemRevenue(Date fromDate, Date toDate, BillType[] billTypes,
            Class[] billCLasses, Department department, Institution institution,
            Category category, PaymentMethod[] paymentMethods,
            BillType[] billTypesToExculde,
            Class[] billCLassesToExclude) {
        ////// // System.out.println("findBillItemRevenue");
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
        ////// // System.out.println("sql = " + sql);
        ////// // System.out.println("m = " + m);
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

    /**
     * Updates expense costing for a bill item and recalculates associated bill totals
     * within a single transaction to ensure data consistency.
     * 
     * @param expense The BillItem expense to update
     * @param bill The Bill to update with recalculated totals
     * @param billItems List of BillItems for the bill
     * @param pharmacyCostingService Service for costing calculations
     */
    public void updateExpenseCosting(BillItem expense, Bill bill, List<BillItem> billItems, 
            com.divudi.service.pharmacy.PharmacyCostingService pharmacyCostingService) {
        if (expense == null || bill == null) {
            return;
        }
        
        try {
            // Update the expense item in the database
            billItemFacade.edit(expense);
            
            // Recalculate bill expense totals
            recalculateExpenseTotals(bill);
            
            // Recalculate entire bill totals with updated expense categorization
            calculateBillTotalsFromItems(bill, billItems, pharmacyCostingService);
            
            // Distribute proportional bill values (including expenses considered for costing) to line items
            if (pharmacyCostingService != null && billItems != null) {
                pharmacyCostingService.distributeProportionalBillValuesToItems(billItems, bill);
            }
            
            // Persist the updated bill
            if (bill.getId() != null) {
                billFacade.edit(bill);
            }
        } catch (Exception e) {
            // Transaction will be rolled back automatically by container
            throw new RuntimeException("Failed to update expense costing: " + e.getMessage(), e);
        }
    }
    
    /**
     * Recalculates expense totals for a bill
     */
    private void recalculateExpenseTotals(Bill bill) {
        if (bill == null) {
            return;
        }

        double billExpensesConsideredTotal = 0.0;
        double billExpensesNotConsideredTotal = 0.0;
        double billExpensesTotal = 0.0;

        // Calculate totals from bill-level expense BillItems
        if (bill.getBillExpenses() != null && !bill.getBillExpenses().isEmpty()) {
            for (BillItem expense : bill.getBillExpenses()) {
                billExpensesTotal += expense.getNetValue();
                if (expense.isConsideredForCosting()) {
                    billExpensesConsideredTotal += expense.getNetValue();
                } else {
                    billExpensesNotConsideredTotal += expense.getNetValue();
                }
            }
        }

        // Update the bill's expense totals
        bill.setExpenseTotal(billExpensesTotal);
        bill.setExpensesTotalConsideredForCosting(billExpensesConsideredTotal);
        bill.setExpensesTotalNotConsideredForCosting(billExpensesNotConsideredTotal);
        
        // Also update BillFinanceDetails if it exists
        if (bill.getBillFinanceDetails() != null) {
            bill.getBillFinanceDetails().setBillExpense(java.math.BigDecimal.valueOf(billExpensesTotal));
            bill.getBillFinanceDetails().setBillExpensesConsideredForCosting(java.math.BigDecimal.valueOf(billExpensesConsideredTotal));
            bill.getBillFinanceDetails().setBillExpensesNotConsideredForCosting(java.math.BigDecimal.valueOf(billExpensesNotConsideredTotal));
        }
    }
    
    /**
     * Placeholder for bill totals calculation - to be implemented based on existing logic
     */
    private void calculateBillTotalsFromItems(Bill bill, List<BillItem> billItems, 
            com.divudi.service.pharmacy.PharmacyCostingService pharmacyCostingService) {
        // This method should contain the logic from the controller's calculateBillTotalsFromItems method
        // For now, we'll leave it as a placeholder since the controller method may be complex
        if (pharmacyCostingService != null && billItems != null) {
            pharmacyCostingService.calculateBillTotalsFromItemsForPurchases(bill, billItems);
        }
    }

}
