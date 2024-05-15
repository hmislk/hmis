/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.dataStructure.PharmacyStockRow;
import com.divudi.data.dataStructure.StockReportRecord;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
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
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.StockHistoryFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
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
@Named(value = "reportsStock")
@SessionScoped
public class ReportsStock implements Serializable {

    /**
     * Bean Variables
     */
    Department department;
    Staff staff;
    Institution institution;
    private Category category;
    List<Stock> stocks;
    double stockSaleValue;
    double stockPurchaseValue;
    List<PharmacyStockRow> pharmacyStockRows;
    List<StockReportRecord> records;
    Date fromDate;
    Date toDate;
    Date fromDateE;
    Date toDateE;
    double totalQty;
    double totalPurchaseRate;
    double totalPurchaseValue;
    double totalRetailSaleRate;
    double totalRetailSaleValue;
    Vmp vmp;
    BillType[] billTypes;
    ReportKeyWord reportKeyWord;
    private List<BillItem> billItems;
    /**
     * Managed Beans
     */
    @Inject
    DealerController dealerController;
    @Inject
    SessionController sessionController;
    @Inject
    CommonController commonController;
    /**
     * EJBs
     */
    @EJB
    StockFacade stockFacade;
    @EJB
    BillItemFacade billItemFacade;

    /**
     * Methods
     */
    public void fillDepartmentStocks() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s "
                + " where s.department=:d "
                + " and s.stock>0 "
                + " and (s.itemBatch.item.departmentType is null or s.itemBatch.item.departmentType =:depty) "
                //                + " and s.itemBatch.item.departmentType!=:depty1 "
                //                + " and s.itemBatch.item.departmentType!=:depty2 "
                + " order by s.itemBatch.item.name";
        m.put("d", department);
        m.put("depty", DepartmentType.Pharmacy);
//        m.put("depty1", DepartmentType.Store);
//        m.put("depty2", DepartmentType.Inventry);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }

    }

    public String fillDepartmentNonEmptyStocksByVmp() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        Map m = new HashMap();
        String sql;

        if (department == null) {
            sql = "select s from Stock s join TREAT(s.itemBatch.item as Amp) amp "
                    + "where s.stock>:z and amp.vmp=:vmp "
                    + "order by s.itemBatch.item.name";
            m.put("z", 0.0);
            m.put("vmp", vmp);
        } else {
            sql = "select s from Stock s join TREAT(s.itemBatch.item as Amp) amp "
                    + "where s.stock>:z and s.department=:d and amp.vmp=:vmp "
                    + "order by s.itemBatch.item.name";
            m.put("d", department);
            m.put("z", 0.0);
            m.put("vmp", vmp);
        }
        //System.err.println("");
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }

        return "pharmacy_report_department_stock_by_single_product";

    }

    public void fillDepartmentNonEmptyProductStocks() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        Map m = new HashMap();
        String sql;
        if (department == null) {
            sql = "select new com.divudi.data.dataStructure.PharmacyStockRow(vmp, sum(s.stock), "
                    + "sum(s.itemBatch.purcahseRate * s.stock), sum(s.itemBatch.retailsaleRate * s.stock))  "
                    + "from Stock s join s.itemBatch.item as amp join amp.vmp as vmp "
                    + "where s.stock>:z  "
                    + "group by vmp, vmp.name "
                    + "order by vmp.name";
            m.put("z", 0.0);
        } else {
            sql = "select new com.divudi.data.dataStructure.PharmacyStockRow(vmp, sum(s.stock), "
                    + "sum(s.itemBatch.purcahseRate * s.stock), sum(s.itemBatch.retailsaleRate * s.stock))  "
                    + "from Stock s join s.itemBatch.item as amp join amp.vmp as vmp "
                    + "where s.stock>:z and s.department=:d "
                    + "group by vmp, vmp.name "
                    + "order by vmp.name";
            m.put("d", department);
            m.put("z", 0.0);
        }
//        //////System.out.println("sql = " + sql);
//        //////System.out.println("m = " + m);
//        //////System.out.println("getStockFacade().findObjects(sql, m) = " + getStockFacade().findObjects(sql, m));
        List<PharmacyStockRow> lsts = (List) getStockFacade().findObjects(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (PharmacyStockRow r : lsts) {
            stockPurchaseValue += r.getPurchaseValue();
            stockSaleValue += r.getSaleValue();
        }
        pharmacyStockRows = lsts;

    }

    public void fillDepartmentNonEmptyItemStocks() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

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
        stockSaleValue = 0.0;
        for (PharmacyStockRow r : lsts) {
            stockPurchaseValue += r.getPurchaseValue();
            stockSaleValue += r.getSaleValue();

        }
        pharmacyStockRows = lsts;

    }

    public void fillDepartmentInventryStocks() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s from Stock s"
                + " where s.department=:d"
                + " and s.itemBatch.item.departmentType=:depty "
                + " order by s.itemBatch.item.name";

        m.put("depty", DepartmentType.Inventry);
        m.put("d", department);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
//        for (Stock ts : stocks) {
//            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
//            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
//        }
    }

    public void fillDepartmentStocksMinus() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

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
        hm.put("btp1", BillType.PharmacyGrnBill);
        hm.put("btp2", BillType.PharmacyPurchaseBill);
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
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

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
        Date startTime = new Date();

        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "select s "
                + " from Stock s "
                + " where s.stock > :st "
                + " and s.department=:d "
                + " and s.itemBatch.dateOfExpire "
                + " between :fd and :td "
                + " order by s.itemBatch.dateOfExpire";
        m.put("d", department);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("st", 0.0);
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }

    }

    public void addComment(Stock st) {
        if (st != null) {
            getStockFacade().edit(st);
            JsfUtil.addSuccessMessage("Edit Successful");
        } else {
            return;
        }
    }

    @EJB
    ItemFacade itemFacade;

    List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public void fillDepartmentNonmovingStocks() {
        Date startTime = new Date();

        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m = new HashMap();
        String sql;
        sql = "SELECT bi.item "
                + " FROM BillItem bi "
                + " WHERE  "
                + " bi.bill.department=:d "
                + " AND bi.bill.billType in :bts "
                + " AND bi.bill.billDate between :fd and :td ";
        m.put("d", department);
        m.put("bts", Arrays.asList(billTypes));
        m.put("fd", getFromDateE());
        m.put("td", getToDateE());
        if (category != null) {
            sql += " AND bi.item.category=:cat ";
            m.put("cat", category);
        }
        sql += " GROUP BY bi.item";

        //System.out.println("sql = " + sql);
        //System.out.println("m = " + m);
        Set<Item> bis = new HashSet<>(itemFacade.findByJpql(sql, m));

        sql = "SELECT s.itemBatch.item "
                + " FROM Stock s "
                + " WHERE s.department=:d "
                + " AND s.stock > 0 ";
        m = new HashMap();
        m.put("d", department);
        if (category != null) {
            sql += " AND s.itemBatch.item.category=:cat ";
            m.put("cat", category);
        }
        sql = sql + " GROUP BY s.itemBatch.item "
                + " ORDER BY s.itemBatch.item.name";

        Set<Item> sis = new HashSet<>(itemFacade.findByJpql(sql, m));

        sis.removeAll(bis);
        items = new ArrayList<>(sis);

        Collections.sort(items);

    }

    public void fillStaffStocks() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

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

    public void fillAllStaffStocks() {
//        Date startTime = new Date();
//        Date fromDate = null;
//        Date toDate = null;
//
//        Map m = new HashMap();
//        String sql;
//        sql = "select s from Stock s where s.stock!=:d "
//                + " order by s.staff.person.name, "
//                + " s.itemBatch.item.name ";
//        m.put("d", 0.0);
//        stocks = getStockFacade().findByJpql(sql, m);
//        stockPurchaseValue = 0.0;
//        stockSaleValue = 0.0;
//        for (Stock ts : stocks) {
//            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
//            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
//        }

        Map<String, Object> m = new HashMap<>();
        String sql = "select bi from BillItem bi"
                + " where bi.bill.billType = :bt"
                + " and bi.retired = :ret"
                + " and bi.bill.toStaff is not null"
                + " and bi.bill.fromDepartment is not null"
                + " and bi.bill.forwardReferenceBills is empty"
                + " order by bi.bill.toStaff.person.name";
        m.put("bt", BillType.PharmacyTransferIssue);
        m.put("ret", false);
        billItems = billItemFacade.findByJpql(sql, m);
    }

    public String fillDistributorStocks() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (department == null || institution == null) {
            JsfUtil.addErrorMessage("Please select a department && Dealor");
            return "";
        }
        Map m;
        String sql;
        records = new ArrayList<>();

        m = new HashMap();
        m.put("ins", institution);
        m.put("dep", department);
        m.put("st", 0.0);
        sql = "select s "
                + " from Stock s "
                + " where s.department=:dep "
                + " and s.stock > :st "
                + " and s.itemBatch.item.id in "
                + "(select item.id "
                + " from ItemsDistributors id join id.item as item "
                + " where id.retired=false "
                + " and id.institution=:ins)";
        stocks = getStockFacade().findByJpql(sql, m);
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
        }

        return "/pharmacy/pharmacy_report_supplier_stock_by_batch";
    }

    public void fillCategoryStocks() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

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
        m.put("st", 0.0);
        sql = "select s "
                + " from Stock s "
                + " where s.department=:dep "
                + " and s.stock > :st "
                + " and s.itemBatch.item.category=:cat "
                + " order by s.itemBatch.item.name";
        stocks = getStockFacade().findByJpql(sql, m);
        totalQty = 0.0;
        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;

        for (Stock ts : stocks) {
            stockPurchaseValue = stockPurchaseValue + (ts.getItemBatch().getPurcahseRate() * ts.getStock());
            stockSaleValue = stockSaleValue + (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
            totalQty += ts.getStock();
            totalPurchaseRate += ts.getItemBatch().getPurcahseRate();
            totalRetailSaleRate += ts.getItemBatch().getRetailsaleRate();
            totalPurchaseValue += ts.getItemBatch().getPurcahseRate() * ts.getStock();
            totalRetailSaleValue += ts.getItemBatch().getRetailsaleRate() * ts.getStock();
        }
        getReportKeyWord().setBool1(false);

    }

    public void fillCategoryStocksNew() {

        if (department == null || category == null) {
            JsfUtil.addErrorMessage("Please select a department && Category");
            return;
        }
        Map m;
        String sql = "";

        m = new HashMap();
        m.put("cat", category);
        m.put("dep", department);
        m.put("st", 0.0);
        if (getReportKeyWord().getString().equals("0")) {
            sql = "select s ";
        } else if (getReportKeyWord().getString().equals("1")) {
            sql = "select s.itemBatch.item, sum(s.stock) ";
        } else if (getReportKeyWord().getString().equals("2")) {
            sql = "select s.itemBatch.item, sum(s.stock), s.itemBatch.retailsaleRate ";
        }
        sql += " from Stock s "
                + " where s.department=:dep "
                + " and s.stock > :st "
                + " and s.itemBatch.item.category=:cat ";

        stockPurchaseValue = 0.0;
        stockSaleValue = 0.0;
        totalQty = 0.0;
        totalPurchaseRate = 0.0;
        totalRetailSaleRate = 0.0;
        totalPurchaseValue = 0.0;
        totalRetailSaleValue = 0.0;

        if (getReportKeyWord().getString().equals("0")) {
            sql += " order by s.itemBatch.item.name";
            stocks = getStockFacade().findByJpql(sql, m);

            for (Stock ts : stocks) {
                stockPurchaseValue += (ts.getItemBatch().getPurcahseRate() * ts.getStock());
                stockSaleValue += (ts.getItemBatch().getRetailsaleRate() * ts.getStock());
                totalQty += ts.getStock();
                totalPurchaseRate += ts.getItemBatch().getPurcahseRate();
                totalRetailSaleRate += ts.getItemBatch().getRetailsaleRate();
                totalPurchaseValue += ts.getItemBatch().getPurcahseRate() * ts.getStock();
                totalRetailSaleValue += ts.getItemBatch().getRetailsaleRate() * ts.getStock();
            }
            getReportKeyWord().setBool1(false);
        } else if (getReportKeyWord().getString().equals("1") || getReportKeyWord().getString().equals("2")) {
            if (getReportKeyWord().getString().equals("1")) {
                sql += " group by s.itemBatch.item ";
            }
            if (getReportKeyWord().getString().equals("2")) {
                sql += " group by s.itemBatch.item, s.itemBatch.retailsaleRate ";
            }
            sql += " order by s.itemBatch.item.name ";
            List<Object[]> objects = getStockFacade().findAggregates(sql, m);
            stocks = new ArrayList<>();
            for (Object[] ob : objects) {
                Item i = (Item) ob[0];
                double d = (double) ob[1];
                Stock s = new Stock();
                ItemBatch ib = new ItemBatch();
                if (getReportKeyWord().getString().equals("2")) {
                    double saleRate = (double) ob[2];
                    ib.setRetailsaleRate(saleRate);
                }
                ib.setItem(i);
                s.setItemBatch(ib);
                s.setStock(d);
                stocks.add(s);
                totalQty += d;
            }
            getReportKeyWord().setBool1(true);
        } else if (getReportKeyWord().getString().equals("3")) {
            stocks = new ArrayList<>();
            for (int i = 0; i < 70; i++) {
                Stock s = new Stock();
                s.setStock(1.0);
                stocks.add(s);
            }
        } else if (getReportKeyWord().getString().equals("4")) {
            stocks = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                Stock s = new Stock();
                s.setStock(1.0);
                stocks.add(s);
            }
        }

    }

    List<StockReportRecord> stockRecords;

    public List<StockReportRecord> getStockRecords() {
        return stockRecords;
    }

    public void setStockRecords(List<StockReportRecord> stockRecords) {
        this.stockRecords = stockRecords;
    }

    public void fillCategoryStocksByCatagory() {
        Map m;
        String sql;
        records = new ArrayList<>();

        m = new HashMap();
        m.put("dep", department);
        m.put("st", 0.0);
        sql = "select sum(s.stock * s.itemBatch.purcahseRate), "
                + " s.itemBatch.item.category "
                + " from Stock s "
                + " where s.department=:dep "
                + " and s.stock > :st "
                + " group by s.itemBatch.item.category "
                + " order by s.itemBatch.item.category.name";
        List<Object[]> objs = getStockFacade().findAggregates(sql, m);
        ////System.out.println("sql = " + sql);
        totalPurchaseValue = 0.0;
        stockRecords = new ArrayList<>();

        for (Object[] obj : objs) {
            Double sv = (Double) obj[0];
            Category c = (Category) obj[1];
            StockReportRecord r = new StockReportRecord();
            r.setCategory(c);
            r.setStockOnHand(sv);
            stockRecords.add(r);
            totalPurchaseValue += sv;
        }

    }

    public void fillAllDistributorStocks() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        Map m;
        String sql;
        records = new ArrayList<>();
        List<Institution> dealers = getDealerController().getItems();
        stockSaleValue = 0.0;
        stockPurchaseValue = 0.0;
        for (Institution i : dealers) {
            ////////System.out.println("i = " + i);
            m = new HashMap();
            m.put("ins", i);
            m.put("d", department);
            sql = "select sum(s.stock),sum(s.stock * s.itemBatch.purcahseRate),sum(s.stock * s.itemBatch.retailsaleRate)"
                    + " from Stock s where s.department=:d and s.itemBatch.item.id in (select item.id from ItemsDistributors id join id.item as item where id.retired=false and id.institution=:ins)"
                    + " ";
            Object[] objs = getStockFacade().findSingleAggregate(sql, m);

            if (objs[0] != null && (Double) objs[0] > 0) {
                StockReportRecord r = new StockReportRecord();
                ////////System.out.println("objs = " + objs);
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
        if (department == null) {
            department = sessionController.getDepartment();
        }
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
    public ReportsStock() {
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
        Date startTime = new Date();

        toDateE = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) - 3);
        fromDateE = c.getTime();
        fillDepartmentNonmovingStocks();

    }

    public void fillSixMonthsNonmoving() {
        Date startTime = new Date();

        toDateE = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, c.get(Calendar.MONTH) - 6);
        fromDateE = c.getTime();
        fillDepartmentNonmovingStocks();

    }

    public void fillOneYearNonmoving() {
        Date startTime = new Date();

        toDateE = Calendar.getInstance().getTime();
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, c.get(Calendar.YEAR) - 1);
        fromDateE = c.getTime();
        fillDepartmentNonmovingStocks();

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

    public double getTotalQty() {
        return totalQty;
    }

    public void setTotalQty(double totalQty) {
        this.totalQty = totalQty;
    }

    public double getTotalPurchaseRate() {
        return totalPurchaseRate;
    }

    public void setTotalPurchaseRate(double totalPurchaseRate) {
        this.totalPurchaseRate = totalPurchaseRate;
    }

    public double getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(double totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public double getTotalRetailSaleRate() {
        return totalRetailSaleRate;
    }

    public void setTotalRetailSaleRate(double totalRetailSaleRate) {
        this.totalRetailSaleRate = totalRetailSaleRate;
    }

    public double getTotalRetailSaleValue() {
        return totalRetailSaleValue;
    }

    public void setTotalRetailSaleValue(double totalRetailSaleValue) {
        this.totalRetailSaleValue = totalRetailSaleValue;
    }

    public List<PharmacyStockRow> getPharmacyStockRows() {
        return pharmacyStockRows;
    }

    public void setPharmacyStockRows(List<PharmacyStockRow> pharmacyStockRows) {
        this.pharmacyStockRows = pharmacyStockRows;
    }

    public Vmp getVmp() {
        return vmp;
    }

    public void setVmp(Vmp vmp) {
        this.vmp = vmp;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
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

    public BillType[] getBillTypes() {
        if (billTypes == null) {
            billTypes = new BillType[]{BillType.PharmacySale, BillType.PharmacyIssue, BillType.PharmacyPre, BillType.PharmacyWholesalePre};
        }
        return billTypes;
    }

    public void setBillTypes(BillType[] billTypes) {
        this.billTypes = billTypes;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
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

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBills(List<BillItem> billItems) {
        this.billItems = billItems;
    }

}
