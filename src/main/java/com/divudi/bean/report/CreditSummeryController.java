/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.report;

import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.CategoryWithItem;
import com.divudi.data.dataStructure.DailyCash;
import com.divudi.data.dataStructure.DailyCredit;
import com.divudi.data.dataStructure.ItemWithFee;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.RefundBill;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFacade;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@RequestScoped
public class CreditSummeryController implements Serializable {

    private Institution institution;
    private Date fromDate;
    private Date toDate;
    ///////////////
    private List<DailyCash> dailyCash;
    List<DailyCredit> dailyCredit;
    /////////////
    @EJB
    private CommonFunctions commonFunctions;
//    @Inject
//    private SessionController sessionController;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private CategoryFacade categoryFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillFacade billFacade;

    /**
     * Creates a new instance of CreditSummery
     *
     * @return
     */
    public void makeNull() {
        dailyCash = null;
        dailyCredit = null;
        //   categoryWithItem = null;
    }

    private List<Department> findDepartment() {

        String sql;
        Map temMap = new HashMap();
        sql = "select distinct(bi.item.department) "
                + " FROM BillItem bi "
                + " where  bi.bill.billType= :bTp "
                + " and bi.bill.creditCompany=:credit"
                + " and  bi.bill.createdAt between :fromDate and :toDate "
                + " and bi.bill.paymentMethod = :pm ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //  temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm", PaymentMethod.Credit);
        temMap.put("credit", getInstitution());

        List<Department> tmp = getDepartmentFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        return tmp;
    }

    private List<Category> findCategory(Department d) {
        String sql;
        Map temMap = new HashMap();
        if (d == null) {
            return new ArrayList<>();
        }
        sql = "select distinct(bi.item.category) FROM BillItem bi where bi.bill.billType= :bTp "
                + " and bi.item.department=:dep and  bi.bill.createdAt between :fromDate and :toDate "
                + " and bi.bill.paymentMethod = :pm and bi.bill.creditCompany=:credit";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //   temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", d);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm", PaymentMethod.Credit);
        temMap.put("credit", getInstitution());

        List<Category> tmp = getCategoryFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        return tmp;

    }

    private List<Item> findItem(Category d, Department dep) {
        String sql;
        Map temMap = new HashMap();
        if (d == null) {
            return new ArrayList<>();
        }
        sql = "select distinct(bi.item) FROM BillItem bi where bi.item.department=:dep "
                + " and  bi.bill.billType= :bTp  "
                + " and bi.item.category=:cat and  bi.bill.createdAt between :fromDate and :toDate "
                + "and  bi.bill.paymentMethod = :pm and bi.bill.creditCompany=:credit ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //     temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", dep);
        temMap.put("cat", d);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm", PaymentMethod.Credit);
        temMap.put("credit", getInstitution());
        List<Item> tmp = getItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    private long billItemForCount(Bill bill, Item i) {

        Map temMap = new HashMap();
        String sql;

        sql = "select count(bi) FROM BillItem bi where bi.item=:itm and bi.bill.creditCompany=:credit"
                + " and bi.bill.paymentMethod = :pm "
                + "and bi.bill.billType=:btp and type(bi.bill)=:billClass "
                + "and bi.bill.createdAt between :fromDate and :toDate ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //  temMap.put("ins", getSessionController().getInstitution());
        temMap.put("itm", i);
        temMap.put("pm", PaymentMethod.Credit);
        temMap.put("billClass", bill.getClass());
        temMap.put("btp", BillType.OpdBill);
        temMap.put("credit", getInstitution());

        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getFee(ItemWithFee i, FeeType feeType) {
        String sql = "SELECT sum(bf.feeValue) FROM BillFee bf WHERE "
                + " bf.bill.billType=:bTp and bf.fee.feeType=:ftp "
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + "  and bf.billItem.item=:itm"
                + " and bf.bill.paymentMethod = :pm and bf.bill.creditCompany=:credit ";

        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //   temMap.put("ins", getSessionController().getInstitution());
        temMap.put("itm", i.getItem());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp", feeType);
        temMap.put("pm", PaymentMethod.Credit);
        temMap.put("credit", getInstitution());
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long getCount(ItemWithFee i) {
        long billed, cancelled, refunded;
        billed = cancelled = refunded = 0l;

        billed = billItemForCount(new BilledBill(), i.getItem());
        cancelled = billItemForCount(new CancelledBill(), i.getItem());
        refunded = billItemForCount(new RefundBill(), i.getItem());

        return billed - (cancelled + refunded);

    }

    public double getDepartmentTotal() {
        double tmp = 0.0;
        for (DailyCash d : getDailyCash()) {
            tmp += d.getDepartmentTotal();
        }

        return tmp;
    }

    public double getDepartmentTotalByBill() {
        double tmp = 0.0;
        for (DailyCredit d : getDailyCreditByBill()) {
            tmp += d.getDiscountTotal() + d.getNetTotal();
        }

        return tmp;
    }
    

    public List<DailyCash> getDailyCredit() {
        // ////System.out.println("Starting : ");
        if (dailyCash == null) {
            dailyCash = new ArrayList<>();

            for (Department d : findDepartment()) {
                DailyCash tmp = new DailyCash();
                tmp.setDepartment(d);
                dailyCash.add(tmp);
            }

            for (DailyCash d : dailyCash) {
                List<CategoryWithItem> tmpCatList = new ArrayList<>();

                for (Category cat : findCategory(d.getDepartment())) {
                    CategoryWithItem n = new CategoryWithItem();
                    n.setCategory(cat);

                    List<ItemWithFee> tmpItemList = new ArrayList<>();

                    for (Item i : findItem(cat, d.getDepartment())) {
                        ItemWithFee iwf = new ItemWithFee();
                        iwf.setItem(i);
                        iwf.setCount(getCount(iwf));
                        // setCount(iwf);
                        iwf.setHospitalFee(getFee(iwf, FeeType.OwnInstitution));
                        iwf.setProFee(getFee(iwf, FeeType.Staff));

                        tmpItemList.add(iwf);

                    }

                    n.setItemWithFees(tmpItemList);
                    tmpCatList.add(n);
                }

                d.setCategoryWitmItems(tmpCatList);

            }

        }

        return dailyCash;
    }

    private List<Bill> findBills(Department dep) {
        String sql;
        Map temMap = new HashMap();
        if (dep == null) {
            return new ArrayList<>();
        }
        sql = "select bi FROM Bill bi "
                + " where bi.toDepartment=:dep "
                + " and  bi.billType= :bTp  "
                + " and  bi.createdAt between :fromDate and :toDate "
                + "and  bi.paymentMethod = :pm "
                + "and bi.creditCompany=:credit ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //     temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", dep);
        // temMap.put("cat", d);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm", PaymentMethod.Credit);
        temMap.put("credit", getInstitution());
        List<Bill> list = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        System.err.println(list);
        return list;
        //     return tmp;
    }

    private double getDiscount(Department dep) {
        String sql;
        Map temMap = new HashMap();
        if (dep == null) {
            return 0;
        }
        sql = "select sum(bi.discount) FROM Bill bi where bi.toDepartment=:dep "
                + " and  bi.billType= :bTp  "
                + " and  bi.createdAt between :fromDate and :toDate "
                + "and  bi.paymentMethod = :pm and bi.creditCompany=:credit ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //     temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", dep);
        // temMap.put("cat", d);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm", PaymentMethod.Credit);
        temMap.put("credit", getInstitution());
        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

        //     return tmp;
    }

    private double getNetTotal(Department dep) {
        String sql;
        Map temMap = new HashMap();
        if (dep == null) {
            return 0;
        }
        sql = "select sum(bi.netTotal) FROM Bill bi where bi.toDepartment=:dep "
                + " and  bi.billType= :bTp  "
                + " and  bi.createdAt between :fromDate and :toDate "
                + "and  bi.paymentMethod = :pm and bi.creditCompany=:credit ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //     temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", dep);
        // temMap.put("cat", d);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm", PaymentMethod.Credit);
        temMap.put("credit", getInstitution());
        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

        //     return tmp;
    }

    public List<DailyCredit> getDailyCreditByBill() {
        // ////System.out.println("Starting : ");
        if (dailyCredit == null) {
            dailyCredit = new ArrayList<>();

            for (Department d : findDepartment()) {
                DailyCredit tmp = new DailyCredit();
                tmp.setDepartment(d);
                dailyCredit.add(tmp);
            }

            for (DailyCredit d : dailyCredit) {
                d.setBills(findBills(d.getDepartment()));
                d.setDiscountTotal(getDiscount(d.getDepartment()));
                d.setNetTotal(getNetTotal((d.getDepartment())));
            }

        }

        return dailyCredit;
    }

    public CreditSummeryController() {
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }

        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<DailyCash> getDailyCash() {
        return dailyCash;
    }

    public void setDailyCash(List<DailyCash> dailyCash) {
        this.dailyCash = dailyCash;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
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

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

}
