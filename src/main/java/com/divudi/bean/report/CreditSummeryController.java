/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.report;

import com.divudi.bean.common.BillController;
import com.divudi.bean.common.CommonController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.CategoryWithItem;
import com.divudi.data.dataStructure.DailyCash;
import com.divudi.data.dataStructure.DailyCredit;
import com.divudi.data.dataStructure.ItemWithFee;
import com.divudi.data.table.String1Value2;

import com.divudi.ejb.CreditBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
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
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
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
    Item item;
    boolean withFooter;
    double total;
    private double totalVat;
    private double totalVatCalculatedValue;
    String vatRegNo;
    String invoceNo;
    double opdCreditVatTotal;
    ///////////////
    private List<DailyCash> dailyCash;
    List<DailyCredit> dailyCredit;
    List<DailyCash> dailyCashSummery;
    List<Bill> creditBills;
    List<String1Value2> vatTableOpdCredit;
    /////////////

    private CommonFunctions commonFunctions;
    @EJB
    CreditBean creditBean;
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

    @Inject
    BillController billController;
    @Inject
    CommonController commonController;

    /**
     * Creates a new instance of CreditSummery
     *
     * @return
     */
    public void makeNull() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        dailyCash = null;
        dailyCredit = null;
        //   categoryWithItem = null;

        
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    private List<Department> findDepartment() {

        String sql;
        Map temMap = new HashMap();
        sql = "select distinct(bi.item.department) "
                + " FROM BillItem bi "
                + " where  bi.bill.billType= :bTp "
                + " and  bi.bill.createdAt between :fromDate and :toDate "
                + " and bi.bill.paymentMethod = :pm ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //  temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm", PaymentMethod.Credit);

        if (item != null) {
            sql += " and bi.item=:it ";
            temMap.put("it", item);
        }

        if (getInstitution() != null) {
            sql += " and bi.bill.creditCompany=:credit";
            temMap.put("credit", getInstitution());
        }

        List<Department> tmp = getDepartmentFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
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
                + " and bi.bill.paymentMethod = :pm ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //   temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", d);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm", PaymentMethod.Credit);

        if (item != null) {
            sql += " and bi.item=:it ";
            temMap.put("it", item);
        }

        if (getInstitution() != null) {
            sql += " and bi.bill.creditCompany=:credit ";
            temMap.put("credit", getInstitution());
        }

        List<Category> tmp = getCategoryFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
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
                + "and  bi.bill.paymentMethod = :pm ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //     temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", dep);
        temMap.put("cat", d);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm", PaymentMethod.Credit);

        if (item != null) {
            sql += " and bi.item=:it ";
            temMap.put("it", item);
        }

        if (getInstitution() != null) {
            sql += " and bi.bill.creditCompany=:credit ";
            temMap.put("credit", getInstitution());
        }

        List<Item> tmp = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    private long billItemForCount(Bill bill, Item i) {

        Map temMap = new HashMap();
        String sql;

        sql = "select count(bi) FROM BillItem bi where bi.item=:itm "
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

        if (item != null) {
            sql += " and bi.item=:it ";
            temMap.put("it", item);
        }

        if (getInstitution() != null) {
            sql += " and bi.bill.creditCompany=:credit ";
            temMap.put("credit", getInstitution());
        }

        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getFee(ItemWithFee i, FeeType feeType) {
        String sql = "SELECT sum(bf.feeValue) FROM BillFee bf WHERE "
                + " bf.bill.billType=:bTp and bf.fee.feeType=:ftp "
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.billItem.item=:itm "
                + " and bf.bill.paymentMethod = :pm ";

        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //   temMap.put("ins", getSessionController().getInstitution());
        temMap.put("itm", i.getItem());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp", feeType);
        temMap.put("pm", PaymentMethod.Credit);

        if (item != null) {
            sql += " and bf.billItem.item=:it ";
            temMap.put("it", item);
        }

        if (getInstitution() != null) {
            sql += " and bf.bill.creditCompany=:credit ";
            temMap.put("credit", getInstitution());
        }

        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getFeeWithOut(ItemWithFee i, FeeType feeType) {
        String sql = "SELECT sum(bf.feeValue) FROM BillFee bf WHERE "
                + " bf.bill.billType=:bTp "
                + " and bf.fee.feeType!=:ftp "
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.billItem.item=:itm"
                + " and bf.bill.paymentMethod = :pm ";

        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //   temMap.put("ins", getSessionController().getInstitution());
        temMap.put("itm", i.getItem());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp", feeType);
        temMap.put("pm", PaymentMethod.Credit);

        if (item != null) {
            sql += " and bf.billItem.item=:it ";
            temMap.put("it", item);
        }

        if (getInstitution() != null) {
            sql += " and bf.bill.creditCompany=:credit ";
            temMap.put("credit", getInstitution());
        }

        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getFeeVatTotal() {
        String sql = "SELECT sum(bf.feeValue) FROM BillFee bf WHERE "
                + " bf.bill.billType=:bTp "
                + " and bf.fee.feeType is not null "
                + " and bf.bill.createdAt between :fromDate and :toDate "
                + " and bf.bill.paymentMethod = :pm ";

        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //   temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm", PaymentMethod.Credit);

        if (item != null) {
            sql += " and bf.billItem.item=:it ";
            temMap.put("it", item);
        }

        if (getInstitution() != null) {
            sql += " and bf.bill.creditCompany=:credit ";
            temMap.put("credit", getInstitution());
        }

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
        for (DailyCash d : getDailyCashSummery()) {
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

    public double getDepartmentTotalByBillWithVat() {
        double tmp = 0.0;
        for (DailyCredit d : getDailyCreditByBill()) {
            tmp += d.getVatTotal() + d.getNetTotal();
        }

        return tmp;
    }

    public List<DailyCash> getDailyCashSummery() {
        return dailyCashSummery;
    }

    public void setDailyCashSummery(List<DailyCash> dailyCashSummery) {
        this.dailyCashSummery = dailyCashSummery;
    }

    public void createDailyCashTable() {
        Date startTime = new Date();

        dailyCashSummery = new ArrayList<>();
        if (!getDailyCredit().isEmpty()) {
            dailyCashSummery.addAll(getDailyCredit());
        }

        opdCreditVatTotal = getFeeVatTotal();
        createVatTables();

        

    }

    public List<DailyCash> getDailyCredit() {
        // //////// // System.out.println("Starting : ");
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
//                        iwf.setHospitalFee(getFee(iwf, FeeType.OwnInstitution));
                        iwf.setHospitalFee(getFeeWithOut(iwf, FeeType.Staff));
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
        List<Bill> list = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

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

    private double getVatTotal(Department dep) {
        String sql;
        Map temMap = new HashMap();
        if (dep == null) {
            return 0;
        }
        sql = "select sum(bi.vat) FROM Bill bi where bi.toDepartment=:dep "
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
        // //////// // System.out.println("Starting : ");
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
                d.setVatTotal(getVatTotal((d.getDepartment())));
            }

        }

        return dailyCredit;
    }

    public void createCreditDueTable() {
        Date startTime = new Date();

        creditBills = new ArrayList<>();
        creditBills = billController.getCreditBills(institution, fromDate, toDate);
        total = 0.0;
        List<Bill> bills=new ArrayList<>();
        for (Bill b : creditBills) {
            b.setVatCalulatedAmount(0.0);
            double refAmount = 0.0;
            Object[] objects = getCreditBean().getRefundAmounts(b);
            if (objects != null) {
                double vat = 0.0;
                double net = 0.0;
                if (objects[0] != null) {
                    net = (double) objects[0];
                }
                if (objects[1] != null) {
                    vat = (double) objects[1];
                }
                refAmount = net + vat;
                if ((b.getNetTotal() + b.getVat()) - (Math.abs(getCreditBean().getPaidAmount(b, BillType.CashRecieveBill)) -refAmount) == 0.0) {
                    bills.add(b);
                    continue;
                }
                b.setNetTotal(b.getNetTotal() - net);
                b.setVat(b.getVat() - vat);
            }

            for (BillItem bi : b.getBillItems()) {
                if (bi.getVat() > 0) {
                    b.setVatCalulatedAmount(b.getVatCalulatedAmount() + bi.getNetValue());
                }
            }
            total += b.getNetTotal();
            totalVat += b.getVat();
            totalVatCalculatedValue += b.getVatCalulatedAmount();
        }
        creditBills.removeAll(bills);

        

    }

    private void createVatTables() {
        vatTableOpdCredit = new ArrayList<>();
        String1Value2 dd = new String1Value2();

        dd = new String1Value2();
        dd.setString("Credit VAT Collection");
        dd.setValue1(opdCreditVatTotal);
        vatTableOpdCredit.add(dd);

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
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }

        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
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

    public List<Bill> getCreditBills() {
        return creditBills;
    }

    public void setCreditBills(List<Bill> creditBills) {
        this.creditBills = creditBills;
    }

    public boolean isWithFooter() {
        return withFooter;
    }

    public void setWithFooter(boolean withFooter) {
        this.withFooter = withFooter;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public double getTotalVat() {
        return totalVat;
    }

    public void setTotalVat(double totalVat) {
        this.totalVat = totalVat;
    }

    public double getTotalVatCalculatedValue() {
        return totalVatCalculatedValue;
    }

    public void setTotalVatCalculatedValue(double totalVatCalculatedValue) {
        this.totalVatCalculatedValue = totalVatCalculatedValue;
    }

    public String getVatRegNo() {
        return vatRegNo;
    }

    public void setVatRegNo(String vatRegNo) {
        this.vatRegNo = vatRegNo;
    }

    public String getInvoceNo() {
        return invoceNo;
    }

    public void setInvoceNo(String invoceNo) {
        this.invoceNo = invoceNo;
    }

    public double getOpdCreditVatTotal() {
        return opdCreditVatTotal;
    }

    public void setOpdCreditVatTotal(double opdCreditVatTotal) {
        this.opdCreditVatTotal = opdCreditVatTotal;
    }

    public List<String1Value2> getVatTableOpdCredit() {
        return vatTableOpdCredit;
    }

    public void setVatTableOpdCredit(List<String1Value2> vatTableOpdCredit) {
        this.vatTableOpdCredit = vatTableOpdCredit;
    }

    public CreditBean getCreditBean() {
        return creditBean;
    }

    public void setCreditBean(CreditBean creditBean) {
        this.creditBean = creditBean;
    }

}
