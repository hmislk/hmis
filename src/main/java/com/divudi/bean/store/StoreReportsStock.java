/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.store;


import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.pharmacy.DealerController;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.dataStructure.PharmacyStockRow;
import com.divudi.data.dataStructure.StockReportRecord;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.CreditBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.Staff;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.entity.pharmacy.StockHistory;
import com.divudi.facade.ItemBatchFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.StockHistoryFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class StoreReportsStock implements Serializable {

    /**
     * Bean Variables
     */
    Department department;
    Staff staff;
    Institution institution;
    private Category category;
    Item item;
    List<Stock> stocks;
    double stockSaleValue;
    double stockPurchaseValue;
    List<StockReportRecord> records;
    Bill grnbill;
    List<Bill> grnReturnbills;
    List<Bill> paymentbills;
    List<PharmacyStockRow> pharmacyStockRows;
    Date fromDate;
    Date toDate;
    Date fromDateE;
    Date toDateE;
    ReportKeyWord reportKeyWord;

    Stock selectedInventoryStock;

    /**
     * Managed Beans
     */
    @Inject
    DealerController dealerController;

    /**
     * EJBs
     */
    @EJB
    StockFacade stockFacade;
    @EJB
    ItemBatchFacade itemBatchFacade;

    /**
     * Methods
     */
    public void fillDepartmentNonEmptyItemStocks() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select new com.divudi.data.dataStructure.PharmacyStockRow"
                + "(s.itemBatch.item.code, "
                + "s.itemBatch.item.name, "
                + "sum(s.stock), "
                + "sum(s.itemBatch.purcahseRate * s.stock), "
                + "sum(s.itemBatch.retailsaleRate * s.stock))  "
                + "from Stock s where s.stock>:z and s.department=:d "
                + "group by s.itemBatch.item.name, s.itemBatch.item.code "
                + "order by s.itemBatch.item.name";
        m.put("d", department);
        m.put("z", 0.0);
        List<PharmacyStockRow> lsts = (List) getStockFacade().findObjects(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue += 0.0;
        for (PharmacyStockRow r : lsts) {
            stockPurchaseValue += r.getPurchaseValue();
            stockSaleValue += r.getSaleValue();

        }
        pharmacyStockRows = lsts;
    }

    public void fillDepartmentStocks() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s where s.department=:d"
                + " and s.itemBatch.item.departmentType=:depty";

        if (category != null) {
            sql += " and s.itemBatch.item.category=:cat ";
            m.put("cat", category);
        }

        if (item != null) {
            sql += " and s.itemBatch.item=:item ";
            m.put("item", item);
        }

        sql += " order by s.itemBatch.item.name,s.itemBatch.serialNo ";

        m.put("depty", DepartmentType.Store);
        m.put("d", department);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }
    }

    public void fillDepartmentStocksWithOutStockZero() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s where s.department=:d"
                + " and s.itemBatch.item.departmentType=:depty "
                + " and s.stock>0 ";

        if (category != null) {
            sql += " and s.itemBatch.item.category=:cat ";
            m.put("cat", category);
        }

        if (item != null) {
            sql += " and s.itemBatch.item=:item ";
            m.put("item", item);
        }

        sql += " order by s.itemBatch.item.name,s.itemBatch.serialNo ";

        m.put("depty", DepartmentType.Store);
        m.put("d", department);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }
    }

    public void fillInventoryAssets() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s where s.department=:d"
                + " and s.itemBatch.item.departmentType=:depty"
                + " order by s.itemBatch.item.name";

        m.put("depty", DepartmentType.Inventry);
        m.put("d", department);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }
    }

    public void fillDepartmentInventryStocks() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s where s.department.institution=:i"
                + " and s.itemBatch.item.departmentType=:depty "
                + " and s.itemBatch.lastPurchaseBillItem.parentBillItem is null ";
        if (item != null) {
            sql += " and s.itemBatch.item=:itm ";
            m.put("itm", item);
        }
        if (department != null) {
            sql += " and s.department=:d ";
            m.put("d", department);
        }
        if (getReportKeyWord().isBool1()) {
            sql += " and s.itemBatch.lastPurchaseBillItem.bill.createdAt between :fd and :td ";
            m.put("fd", fromDate);
            m.put("td", toDate);
        }
        if (!getReportKeyWord().getAddress().equals("") && getReportKeyWord().getAddress() != null) {
            sql += " and s.itemBatch.batchNo=:tag ";
            m.put("tag", getReportKeyWord().getAddress());
        }

        sql += " order by s.itemBatch.item.name";

        m.put("depty", DepartmentType.Inventry);
        m.put("i", institution);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }
    }

    public void fillDepartmentStocksMinus() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s where s.stock<0 and s.department=:d order by s.itemBatch.item.name";
        m.put("d", department);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }
    }

    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private StockHistoryFacade stockHistoryFacade;

    private PharmaceuticalBillItem getPreviousPharmacuticalBillByBatch(ItemBatch itemBatch, Department department, Date date) {
        String sql = "Select sh from PharmaceuticalBillItem sh where "
                + " sh.itemBatch=:itmB and sh.billItem.bill.department=:dep "
                + " and (sh.billItem.bill.billType=:btp1 or sh.billItem.bill.billType=:btp2 )"
                + "  and sh.billItem.createdAt between :fd and :td "
                + " order by sh.billItem.createdAt desc";
        HashMap hm = new HashMap();
        hm.put("itmB", itemBatch);
        Calendar cl = Calendar.getInstance();
        cl.set(Calendar.MONTH, 1);
        cl.set(Calendar.DAY_OF_MONTH, 26);
        hm.put("td", date);
        hm.put("fd", cl.getTime());
        hm.put("dep", department);
        hm.put("btp1", BillType.StoreGrnBill);
        hm.put("btp2", BillType.StorePurchase);
        return getPharmaceuticalBillItemFacade().findFirstByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    private StockHistory getPreviousStockHistoryByBatch(ItemBatch itemBatch, Department department, Date date) {
        String sql = "Select sh from StockHistory sh where sh.retired=false and"
                + " sh.itemBatch=:itmB and sh.department=:dep and sh.pbItem.billItem.createdAt<:dt "
                + " order by sh.pbItem.billItem.createdAt desc";
        HashMap hm = new HashMap();
        hm.put("itmB", itemBatch);
        hm.put("dt", date);
        hm.put("dep", department);
        return getStockHistoryFacade().findFirstByJpql(sql, hm, TemporalType.TIMESTAMP);
    }

    public void fillDepartmentStocksError() {
        Set<Stock> stockSet = new HashSet<>();
        String sql;
        Map temMap = new HashMap();

        sql = "select p from PharmaceuticalBillItem p where "
                + " p.billItem.bill.department=:dep "
                + " and p.billItem.createdAt>:date and "
                + "  p.stockHistory is not null order by p.stockHistory.id ";

        temMap.put("dep", department);
        temMap.put("date", date);

        List<PharmaceuticalBillItem> list = getPharmaceuticalBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        for (PharmaceuticalBillItem b : list) {
            StockHistory sh = getPreviousStockHistoryByBatch(b.getItemBatch(), b.getBillItem().getBill().getDepartment(), b.getBillItem().getCreatedAt());
            PharmaceuticalBillItem phi = getPreviousPharmacuticalBillByBatch(b.getStock().getItemBatch(), b.getBillItem().getBill().getDepartment(), b.getBillItem().getCreatedAt());

            double calculatedStk = 0;
            boolean flg = false;
            if (sh != null) {
                //   ////System.out.println("Previuos Stock " + sh.getStockQty());
                calculatedStk = (sh.getStockQty() + sh.getPbItem().getQtyInUnit() + sh.getPbItem().getFreeQtyInUnit());
                flg = true;
            } else if (phi != null) {
                calculatedStk = phi.getQtyInUnit() + phi.getFreeQtyInUnit();
                flg = true;
            }

            //   ////System.out.println("calculated History Qty " + calculatedStk);
            if (flg == true && b.getStockHistory().getStockQty() != calculatedStk) {
                stockSet.add(b.getStock());
                //   ////System.out.println("TRUE");
            }

            //   ////System.out.println("#########");
        }

        stocks = new ArrayList<>();
        for (Stock s : stockSet) {
            stocks.add(s);
        }

    }

    public void fillDepartmentStocksError2() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s where s.department=:d order by s.itemBatch.item.name";
        m.put("d", department);
        stocks = getStockFacade().findByJpql(sql, m);
        Set<Stock> tmpStockList = new HashSet<>();

        for (Stock st : stocks) {
            sql = "Select ph from PharmaceuticalBillItem ph where ph.stock=:st "
                    + " and ph.billItem.createdAt>:date  "
                    + " and ph.stockHistory is not null  "
                    + " order by ph.stockHistory.id ";

            m.clear();
            m.put("st", st);
            m.put("date", date);

            List<PharmaceuticalBillItem> phList = getPharmaceuticalBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

            PharmaceuticalBillItem previousPh = null;
            double calculatedStock = 0;

            for (PharmaceuticalBillItem ph : phList) {
                if (previousPh == null) {
                    previousPh = ph;
                    calculatedStock = ph.getStockHistory().getStockQty();
                    continue;
                }
                double preHistoryQty = 0;
                double curHistory = 0;

                if (previousPh.getStockHistory() != null) {
                    preHistoryQty = previousPh.getStockHistory().getStockQty();
                }

                if (ph.getStockHistory() != null) {
                    curHistory = ph.getStockHistory().getStockQty();
                }

                double calcualtedQty = preHistoryQty + previousPh.getQtyInUnit() + previousPh.getFreeQtyInUnit();

                switch (ph.getBillItem().getBill().getBillType()) {
                    case PharmacyGrnBill:
                    case PharmacyPurchaseBill:
                    case PharmacyTransferReceive:
                        if (ph.getBillItem().getBill() instanceof BilledBill) {
                            calculatedStock += Math.abs(ph.getQtyInUnit());
                            calculatedStock += Math.abs(ph.getFreeQtyInUnit());
                        } else if (ph.getBillItem().getBill() instanceof CancelledBill || ph.getBillItem().getBill() instanceof RefundBill) {
                            calculatedStock -= Math.abs(ph.getQtyInUnit());
                            calculatedStock -= Math.abs(ph.getFreeQtyInUnit());
                        }
                        break;
                    case PharmacyGrnReturn:
                    case PurchaseReturn:
                    case PharmacyTransferIssue:
                        if (ph.getBillItem().getBill() instanceof BilledBill) {
                            calculatedStock -= Math.abs(ph.getQtyInUnit());
                            calculatedStock -= Math.abs(ph.getFreeQtyInUnit());
                        } else if (ph.getBillItem().getBill() instanceof CancelledBill || ph.getBillItem().getBill() instanceof RefundBill) {
                            calculatedStock += Math.abs(ph.getQtyInUnit());
                            calculatedStock += Math.abs(ph.getFreeQtyInUnit());
                        }
                        break;
                    case PharmacyPre:
                        if (ph.getBillItem().getBill() instanceof PreBill) {
                            if (ph.getBillItem().getBill().getReferenceBill() == null) {
                                break;
                            }
                            calculatedStock -= Math.abs(ph.getQtyInUnit());

                        } else if (ph.getBillItem().getBill() instanceof CancelledBill || ph.getBillItem().getBill() instanceof RefundBill) {
                            calculatedStock += Math.abs(ph.getQtyInUnit());
                        }
                        break;
                    default:

                }

                if (calcualtedQty != curHistory) {
                    st.setCalculated(calculatedStock);
                    tmpStockList.add(st);
                } else {
                    //   ////System.out.println("Itm " + ph.getBillItem().getItem().getName());
                    //   ////System.out.println("Prv History Qty " + preHistoryQty);
                    //   ////System.out.println("Prv Qty " + previousPh.getQtyInUnit());
                    //   ////System.out.println("Prv Free Qty " + previousPh.getFreeQtyInUnit());
                    //   ////System.out.println("History " + curHistory);
                    //   ////System.out.println("######");
                }

                previousPh = ph;
            }

        }

        List<Stock> stk = new ArrayList<>();
        for (Stock st : tmpStockList) {
            stk.add(st);
        }

        stocks = stk;

    }

    private Date date;

    public void fillDepartmentExpiaryStocks() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s where s.department=:d and s.itemBatch.dateOfExpire between :fd and :td order by s.itemBatch.dateOfExpire";
        m.put("d", department);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
//            ts.getItemBatch().getDateOfExpire()
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }
    }

    public void fillDepartmentNonmovingStocks() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s where s.department=:d and s.stock > 0 and s.itemBatch.item not in (select bi.item FROM BillItem bi where  bi.bill.department=:d and (bi.bill.billType=:t1 or bi.bill.billType=:t2) and bi.bill.billDate between :fd and :td group by bi.item having SUM(bi.qty) > 0 ) order by s.itemBatch.dateOfExpire";
        m.put("d", department);
        m.put("t1", BillType.StoreTransferIssue);
        m.put("t2", BillType.StorePre);
        m.put("fd", getFromDateE());
        m.put("td", getToDateE());
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
//            ts.getItemBatch().getDateOfExpire()
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }
    }

    public void fillStaffStocks() {
        if (staff == null) {
            JsfUtil.addErrorMessage("Please select a staff member");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s where s.staff=:d order by s.itemBatch.item.name";
        m.put("d", staff);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }
    }

    public void fillDistributorStocks() {
        if (department == null || institution == null) {
            JsfUtil.addErrorMessage("Please select a department && Dealor");
            return;
        }
        Map m;
        String sql;
        records = new ArrayList<>();

        m = new HashMap();
        m.put("ins", institution);
        m.put("dep", department);
        sql = "select s from Stock s where s.department=:dep and s.itemBatch.item.id in "
                + "(select item.id from ItemsDistributors id join id.item as item where id.retired=false and id.institution=:ins)";
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;

        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }

    }

    public void fillCategoryStocks() {
        if (department == null || category == null) {
            JsfUtil.addErrorMessage("Please select a department && Category");
            return;
        }
        Map m;
        String sql;
        records = new ArrayList<>();

        m = new HashMap();
        m.put("cat", category);
        m.put("dep", department);
        m.put("depty", DepartmentType.Store);//Before Add show items Without code

        sql = "select s from Stock s where s.department=:dep "
                + " and s.itemBatch.item.category=:cat "
                + " and s.itemBatch.item.departmentType=:depty "
                + " order by s.itemBatch.item.name";
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;

        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }

    }

    public void fillAllDistributorStocks() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m;
        String sql;
        records = new ArrayList<>();
        List<Institution> dealers = getDealerController().getItems();
        for (Institution i : dealers) {
            //////System.out.println("i = " + i);
            m = new HashMap();
            m.put("ins", i);
            sql = "select sum(s.stock),sum(s.stock * s.itemBatch.purcahseRate),sum(s.stock * s.itemBatch.retailsaleRate)"
                    + " from Stock s where s.department=:d and s.itemBatch.item.id in (select item.id from ItemsDistributors id join id.item as item where id.retired=false and id.institution=:ins)";
            Object[] objs = getStockFacade().findSingleAggregate(sql, m);
            if (objs[0] != null && (Double) objs[0] > 0) {
                StockReportRecord r = new StockReportRecord();
                //////System.out.println("objs = " + objs);
                r.setInstitution(i);
                r.setQty((Double) objs[0]);
                r.setPurchaseValue((Double) objs[1]);
                r.setRetailsaleValue((Double) objs[2]);
                records.add(r);
                stockPurchaseValue = stockPurchaseValue + r.getPurchaseValue();
                stockSaleValue = stockSaleValue + r.getRetailsaleValue();
            }
        }

    }

    /**
     * Getters & Setters
     *
     * @return
     */
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

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    /**
     * Constructor
     */
    public StoreReportsStock() {
    }

    public double getStockSaleValue() {
        return stockSaleValue;
    }

    public void setStockSaleValue(double stockSaleValue) {
        this.stockSaleValue = stockSaleValue;
    }

    public double getStockPurchaseValue() {
        return stockPurchaseValue;
    }

    public void setStockPurchaseValue(double stockPurchaseValue) {
        this.stockPurchaseValue = stockPurchaseValue;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public List<StockReportRecord> getRecords() {
        return records;
    }

    public void setRecords(List<StockReportRecord> records) {
        this.records = records;
    }

    public DealerController getDealerController() {
        return dealerController;
    }

    public void setDealerController(DealerController dealerController) {
        this.dealerController = dealerController;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = Calendar.getInstance().getTime();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.MONTH, c.get(Calendar.MONTH) + 3);
            toDate = c.getTime();
        }
        return toDate;
    }

    public void fillThreeMonthsExpiary() {
        fromDate = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) + 3);
        toDate = c.getTime();
        fillDepartmentExpiaryStocks();;
    }

    public void fillSixMonthsExpiary() {
        fromDate = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) + 6);
        toDate = c.getTime();
        fillDepartmentExpiaryStocks();;
    }

    public void fillOneYearExpiary() {
        fromDate = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) + 1);
        toDate = c.getTime();
        fillDepartmentExpiaryStocks();;
    }

    public void fillThreeMonthsNonmoving() {
        toDateE = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) - 3);
        fromDateE = c.getTime();
        fillDepartmentNonmovingStocks();
    }

    public void fillSixMonthsNonmoving() {
        toDateE = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) - 6);
        fromDateE = c.getTime();
        fillDepartmentNonmovingStocks();
    }

    public void fillOneYearNonmoving() {
        toDateE = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) - 1);
        fromDateE = c.getTime();
        fillDepartmentNonmovingStocks();
    }

    public void updateTagSerialNo() {
        if (selectedInventoryStock == null) {
            JsfUtil.addErrorMessage("Nothing To Update");
            return;
        }
        if (selectedInventoryStock.getItemBatch() == null) {
            JsfUtil.addErrorMessage("Nothing To Update.Item Batch Null.");
            return;
        }
        if (selectedInventoryStock.getItemBatch().getBatchNo() == null || selectedInventoryStock.getItemBatch().getBatchNo().equals("")) {
            JsfUtil.addErrorMessage("Please Enter Tag Serial");
            return;
        }
        getItemBatchFacade().edit(selectedInventoryStock.getItemBatch());
        JsfUtil.addSuccessMessage("Updated");
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDateE() {
        if (fromDateE == null) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.MONTH, c.get(Calendar.MONTH) - 3);
            fromDateE = c.getTime();
        }
        return fromDateE;
    }

    public void makeBillsNull() {
        grnbill = null;
        grnReturnbills = null;
        paymentbills = null;
    }

    public Bill getGrnbill() {
        return grnbill;
    }

    public void setGrnbill(Bill grnbill) {
        this.grnbill = grnbill;
    }

    public List<Bill> getGrnReturnbills() {
        return grnReturnbills;
    }

    public void setGrnReturnbills(List<Bill> grnReturnbills) {
        this.grnReturnbills = grnReturnbills;
    }

    public List<Bill> getPaymentbills() {
        return paymentbills;
    }

    public void setPaymentbills(List<Bill> paymentbills) {
        this.paymentbills = paymentbills;
    }

    public CreditBean getCreditBean() {
        return creditBean;
    }

    public void setCreditBean(CreditBean creditBean) {
        this.creditBean = creditBean;
    }

    public void setFromDateE(Date fromDateE) {
        this.fromDateE = fromDateE;
    }

    public Date getToDateE() {
        if (toDateE == null) {
            toDateE = Calendar.getInstance().getTime();
        }
        return toDateE;
    }

    public void setToDateE(Date toDateE) {
        this.toDateE = toDateE;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public StockHistoryFacade getStockHistoryFacade() {
        return stockHistoryFacade;
    }

    public void setStockHistoryFacade(StockHistoryFacade stockHistoryFacade) {
        this.stockHistoryFacade = stockHistoryFacade;
    }

    public Stock getSelectedInventoryStock() {
        return selectedInventoryStock;
    }

    public List<PharmacyStockRow> getPharmacyStockRows() {
        return pharmacyStockRows;
    }

    public void setPharmacyStockRows(List<PharmacyStockRow> pharmacyStockRows) {
        this.pharmacyStockRows = pharmacyStockRows;
    }

    boolean paginator = true;
    int rows = 20;

    public boolean isPaginator() {
        return paginator;
    }

    public void setPaginator(boolean paginator) {
        this.paginator = paginator;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public void prepareForPrint() {
        paginator = false;
        rows = getStocks().size();
    }

    public void prepareForView() {
        paginator = true;
        rows = 20;
    }

    @EJB
    CreditBean creditBean;

    private void fetchGrnReturnAndPayments() {
        if (selectedInventoryStock == null) {
            return;
        }

        if (selectedInventoryStock.getItemBatch() == null) {
            return;
        }

        if (selectedInventoryStock.getItemBatch().getLastPurchaseBillItem() == null) {
            return;
        }

        BillType[] billTypesArrayReturn = {BillType.PharmacyGrnReturn, BillType.PurchaseReturn, BillType.StoreGrnReturn, BillType.StorePurchaseReturn};
        List<BillType> billTypesListReturn = Arrays.asList(billTypesArrayReturn);

        grnbill = selectedInventoryStock.getItemBatch().getLastPurchaseBillItem().getBill();

        grnReturnbills = creditBean.getGrnReturnBills(grnbill, billTypesListReturn);
        paymentbills = creditBean.getPaidBills(grnbill, BillType.GrnPayment);
    }

    public void setSelectedInventoryStock(Stock selectedInventoryStock) {
        makeBillsNull();
        this.selectedInventoryStock = selectedInventoryStock;
        fetchGrnReturnAndPayments();
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
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

}
