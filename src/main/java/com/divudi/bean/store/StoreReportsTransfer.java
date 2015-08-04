/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.store;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillType;
import com.divudi.data.dataStructure.StockReportRecord;
import com.divudi.data.table.String1Value3;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.BillItem;
import com.divudi.entity.Bill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.StockFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.inject.Inject;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.joda.time.Days;
import org.joda.time.LocalDate;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class StoreReportsTransfer implements Serializable {

    /**
     * Bean Variables
     */
    @Inject
    SessionController sessionController;
    
    Department fromDepartment;
    Department toDepartment;
    Department department;
    @Temporal(TemporalType.TIMESTAMP)
    Date fromDate;
    @Temporal(TemporalType.TIMESTAMP)
    Date toDate;

    Institution institution;
    List<Stock> stocks;
    double saleValue;
    double purchaseValue;
    double totalsValue;
    double discountsValue;
    double netTotalValues;

    List<BillItem> transferItems;
    List<Bill> transferBills;

    List<StockReportRecord> movementRecords;
    List<StockReportRecord> movementRecordsQty;
    List<String1Value3> listz;

    ArrayList<DepartmentBillRow> drows;
    /**
     * EJBs
     */
    @EJB
    StockFacade stockFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFacade BillFacade;
    @EJB
    StoreBean StoreBean;
    @Inject
    BillBeanController billBeanController;
    @EJB
    CommonFunctions commonFunctions;

    /**
     * Methods
     */
    public void fillFastMoving() {
        fillMoving(true);
        fillMovingQty(true);
    }

    public void fillSlowMoving() {
        fillMoving(false);
        fillMovingQty(false);
    }

    public void fillMovingWithStock() {
        String sql;
        Map m = new HashMap();
        m.put("i", institution);
        m.put("t1", BillType.StoreTransferIssue);
        m.put("t2", BillType.StorePre);
        m.put("fd", fromDate);
        m.put("td", toDate);
        BillItem bi = new BillItem();

        sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                + "SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty)) "
                + "FROM BillItem bi where bi.retired=false and  bi.bill.department.institution=:i and "
                + "(bi.bill.billType=:t1 or bi.bill.billType=:t2) and "
                + "bi.bill.billDate between :fd and :td group by bi.item "
                + "order by  SUM(bi.pharmaceuticalBillItem.qty) desc";
        List<Object[]> objs = getBillItemFacade().findAggregates(sql, m);
        movementRecordsQty = new ArrayList<>();
        for (Object[] obj : objs) {
            StockReportRecord r = new StockReportRecord();
            r.setItem((Item) obj[0]);
            r.setQty((Double) obj[1]);
            Days daysBetween = Days.daysBetween(LocalDate.fromDateFields(fromDate), LocalDate.fromDateFields(toDate));
            int ds = daysBetween.getDays();
            r.setPurchaseValue((Double) (r.getQty() / ds));
//            r.setRetailsaleValue((Double) obj[2]);
            r.setStockQty(getStoreBean().getStockQty(r.getItem(), institution));
            movementRecordsQty.add(r);
        }
    }

    public void fillMoving(boolean fast) {
        String sql;
        Map m = new HashMap();
//        m.put("r", StockReportRecord.class);
        m.put("d", department);
        m.put("t1", BillType.StoreTransferIssue);
        m.put("t2", BillType.StorePre);
        m.put("fd", fromDate);
        m.put("td", toDate);
        BillItem bi = new BillItem();

        if (!fast) {
            sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                    + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                    + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty))  "
                    + "FROM BillItem bi where bi.retired=false and bi.bill.department=:d and "
                    + "(bi.bill.billType=:t1 or bi.bill.billType=:t2) "
                    + "and bi.bill.billDate between :fd and :td "
                    + "group by bi.item "
                    + "order by "
                    + "SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate  * bi.pharmaceuticalBillItem.qty) "
                    + "desc";
        } else {
            sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                    + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                    + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty)) "
                    + "FROM BillItem bi where bi.retired=false and bi.bill.department=:d and "
                    + "(bi.bill.billType=:t1 or bi.bill.billType=:t2) and "
                    + "bi.bill.billDate between :fd and :td group by bi.item "
                    + "order by  SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.pharmaceuticalBillItem.qty) ";
        }
        ////System.out.println("sql = " + sql);
        ////System.out.println("m = " + m);
        List<Object[]> objs = getBillItemFacade().findAggregates(sql, m);
        movementRecords = new ArrayList<>();
        for (Object[] obj : objs) {
            StockReportRecord r = new StockReportRecord();
            r.setItem((Item) obj[0]);
            r.setQty((Double) obj[1]);
            r.setPurchaseValue((Double) obj[2]);
            r.setRetailsaleValue((Double) obj[3]);
            r.setStockQty(getStoreBean().getStockByPurchaseValue(r.getItem(), department));
            movementRecords.add(r);
        }
    }

    public void fillMovingQty(boolean fast) {
        String sql;
        Map m = new HashMap();
        m.put("d", department);
        m.put("t1", BillType.StoreTransferIssue);
        m.put("t2", BillType.StorePre);
        m.put("fd", fromDate);
        m.put("td", toDate);
        BillItem bi = new BillItem();
        if (!fast) {
            sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                    + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                    + "SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty)) "
                    + "FROM BillItem bi where bi.retired=false and  bi.bill.department=:d and "
                    + "(bi.bill.billType=:t1 or bi.bill.billType=:t2) and "
                    + "bi.bill.billDate between :fd and :td group by bi.item "
                    + "order by  SUM(bi.pharmaceuticalBillItem.qty) desc";
        } else {
            sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                    + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                    + "SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty) "
                    + "FROM BillItem bi where bi.retired=false and bi.bill.department=:d and "
                    + "(bi.bill.billType=:t1 or bi.bill.billType=:t2) "
                    + "and bi.bill.billDate between :fd and :td group by bi.item "
                    + "order by  SUM(bi.pharmaceuticalBillItem.qty) ";
        }
        List<Object[]> objs = getBillItemFacade().findAggregates(sql, m);
        movementRecordsQty = new ArrayList<>();
        for (Object[] obj : objs) {
            StockReportRecord r = new StockReportRecord();
            r.setItem((Item) obj[0]);
            r.setQty((Double) obj[1]);
            r.setPurchaseValue((Double) obj[3]);
            r.setRetailsaleValue((Double) obj[2]);
            r.setStockQty(getStoreBean().getStockByPurchaseValue(r.getItem(), department));
            movementRecordsQty.add(r);
        }
    }

    public void fillDepartmentTransfersReceive() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.StoreTransferReceive);
        if (fromDepartment != null && toDepartment != null) {
            m.put("fdept", fromDepartment);
            m.put("tdept", toDepartment);
            sql = "select bi from BillItem bi where bi.bill.fromDepartment=:fdept"
                    + " and bi.bill.department=:tdept and bi.bill.createdAt between :fd "
                    + "and :td and bi.bill.billType=:bt order by bi.id";
        } else if (fromDepartment == null && toDepartment != null) {
            m.put("tdept", toDepartment);
            sql = "select bi from BillItem bi where bi.bill.department=:tdept and bi.bill.createdAt "
                    + " between :fd and :td and bi.bill.billType=:bt order by bi.id";
        } else if (fromDepartment != null && toDepartment == null) {
            m.put("fdept", fromDepartment);
            sql = "select bi from BillItem bi where bi.bill.fromDepartment=:fdept and bi.bill.createdAt "
                    + " between :fd and :td and bi.bill.billType=:bt order by bi.id";
        } else {
            sql = "select bi from BillItem bi where bi.bill.createdAt "
                    + " between :fd and :td and bi.bill.billType=:bt order by bi.id";
        }
        transferItems = getBillItemFacade().findBySQL(sql, m);
        purchaseValue = 0.0;
        saleValue = 0.0;
        for (BillItem ts : transferItems) {
            purchaseValue = purchaseValue + (ts.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate() * ts.getPharmaceuticalBillItem().getQtyInUnit());
            saleValue = saleValue + (ts.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate() * ts.getPharmaceuticalBillItem().getQtyInUnit());
        }
    }

    public void fillDepartmentUnitIssueByBillStore() {
        Map m = new HashMap();
        String sql;

        sql = "select b from Bill b where "
                + " b.createdAt "
                + " between :fd and :td"
                + " and b.billType=:bt";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.StoreIssue);

        if (fromDepartment != null) {
            sql += " and b.fromDepartment=:fdept ";
            m.put("fdept", fromDepartment);
        }

        if (toDepartment != null) {
            sql += " and b.toDepartment=:tdept  ";
            m.put("tdept", toDepartment);
        }

        sql += " order by b.id";

        transferBills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;

        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }
    }

    public void fillDepartmentUnitIssueByBillItemStore() {
        Map m = new HashMap();
        String sql;

        sql = "select b from BillItem b where "
                + " b.bill.createdAt "
                + " between :fd and :td"
                + " and b.bill.billType=:bt";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.StoreIssue);

        if (fromDepartment != null) {
            sql += " and b.bill.fromDepartment=:fdept ";
            m.put("fdept", fromDepartment);
        }

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept  ";
            m.put("tdept", toDepartment);
        }

        sql += " order by b.bill.id";

        transferItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;

        for (BillItem b : transferItems) {
            totalsValue = totalsValue + (b.getRate());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetValue();
        }
    }

    public void fillFromDepartmentUnitIssueByBillStore() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.StoreIssue);
        m.put("fdept", fromDepartment);
//        m.put("tdept", toDepartment);
        sql = "select b from Bill b where "
                + " b.fromDepartment=:fdept and "
                //                + " b.toDepartment=:tdept and "
                + " b.createdAt "
                + " between :fd and :td and "
                + " b.billType=:bt order by b.id";
        transferBills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }
    }

    public ArrayList<DepartmentBillRow> getDrows() {
        return drows;
    }

    public void setDrows(ArrayList<DepartmentBillRow> drows) {
        this.drows = drows;
    }

    public void createDepartmentIssueStore() {
        listz = new ArrayList<>();

        List<Object[]> list = getBillBeanController().fetchBilledDepartmentItemStore(getFromDate(), getToDate(),getSessionController().getDepartment());
        if (list == null) {
            return;
        }

        netTotalValues = 0;

        for (Object[] obj : list) {
            Department item = (Department) obj[0];
            Double dbl = (Double) obj[1];
            //double count = 0;

            String1Value3 newD = new String1Value3();
            newD.setString(item.getName());
            newD.setValue1(dbl);
            netTotalValues += dbl;
            newD.setSummery(false);
            listz.add(newD);

        }

//        netTotalValues = getBillBeanController().calNetTotalBilledDepartmentItemStore(fromDate, toDate, department);
    }

    public void fillAssetTransferlist() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.StoreTransferIssue);

        sql = "select bi from BillItem bi where "
                + " bi.bill.createdAt between :fd and :td "
                + " and  bi.bill.billType=:bt";

        if (fromDepartment != null) {
            m.put("fdept", fromDepartment);
            sql += " and bi.bill.department=:fdept ";
        }

        if (toDepartment != null) {
            m.put("tdept", toDepartment);
            sql += " and bi.bill.toDepartment=:tdept ";
        }

        sql += " order by bi.bill.toDepartment.name, bi.item.category.name, bi.item.name, bi.id";

        transferItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

        purchaseValue = 0.0;
        for (BillItem bi : transferItems) {
            purchaseValue += (bi.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate() * bi.getPharmaceuticalBillItem().getQtyInUnit());
        }

    }

    public void fillDepartmentTransfersIssueByBillItem() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
//        m.put("bt1", BillType.StoreTransferIssue);
        m.put("bt1", BillType.StoreIssue);

        sql = " select bi from BillItem bi where "
                + "  bi.bill.createdAt between :fd  and :td "
                + " and (bi.bill.billType=:bt1)  ";

        if (fromDepartment != null) {
            m.put("fdept", fromDepartment);
            sql += " and bi.bill.department=:fdept ";
        }

        if (toDepartment != null) {
            m.put("tdept", toDepartment);
            sql += " and bi.bill.toDepartment=:tdept ";
        }

        sql += " order by bi.bill.toDepartment.name, "
                + " bi.item.category.name, "
                + " bi.item.name, "
                + " bi.id";

        //System.out.println("sql = " + sql);
        //System.out.println("m = " + m);
        transferItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        purchaseValue = 0.0;
        saleValue = 0.0;
        Map<Item, ItemBillRow> ibrs = new HashMap<>();
        Department dept = null;
        Category cat = null;
        Item item = null;
        drows = new ArrayList<>();
        DepartmentBillRow dbr = null;
        CategoryBillRow cbr = null;
        ItemBillRow ibr = null;

        //System.out.println("transferItems = " + transferItems);
        //System.out.println("transferItems.size() = " + transferItems.size());
        for (BillItem ts : transferItems) {
            //System.out.println("ts = " + ts);

            if (dept != null && dept.equals(ts.getBill().getToDepartment())) {
                //System.out.println("old dept");

                if (cat != null && cat.equals(ts.getItem().getCategory())) {
                    //System.out.println("old cat");

                    if (item != null && item.equals(ts.getItem())) {
                        //System.out.println("old item");

                    } else {
                        //System.out.println("new item");

                        item = ts.getItem();

                        ibr = new ItemBillRow();
                        ibr.setItem(ts.getItem());

                        cbr.getItemBillRows().add(ibr);
                    }
                } else {
                    //System.out.println("new cat");

                    cbr = new CategoryBillRow();
                    ibr = new ItemBillRow();

                    cbr.setCategory(ts.getItem().getCategory());
                    ibr.setItem(ts.getItem());

                    cat = ts.getItem().getCategory();
                    item = ts.getItem();

                    cbr.getItemBillRows().add(ibr);
                    dbr.getCategoryBillRows().add(cbr);
                }

            } else {

                //System.out.println("new dept");
                dbr = new DepartmentBillRow();
                cbr = new CategoryBillRow();
                ibr = new ItemBillRow();

                cbr.setCategory(ts.getItem().getCategory());
                ibr.setItem(ts.getItem());
                dbr.setDepartment(ts.getBill().getToDepartment());

                cat = ts.getItem().getCategory();
                item = ts.getItem();
                dept = ts.getBill().getToDepartment();

                dbr.getCategoryBillRows().add(cbr);
                cbr.getItemBillRows().add(ibr);
                drows.add(dbr);
                //System.out.println("drows = " + drows);
            }

            ibr.getBill().setNetTotal(ibr.getBill().getNetTotal() + ts.getNetValue());
            ibr.getBill().setGrantTotal(ibr.getBill().getGrantTotal() + ts.getQty());

            cbr.getBill().setNetTotal(cbr.getBill().getNetTotal() + ts.getNetValue());
            cbr.getBill().setGrantTotal(cbr.getBill().getGrantTotal() + ts.getQty());

            dbr.getBill().setNetTotal(dbr.getBill().getNetTotal() + ts.getNetValue());
            dbr.getBill().setGrantTotal(dbr.getBill().getGrantTotal() + ts.getQty());

//            purchaseValue += ts.getNetValue();
        }

        purchaseValue = getBillBeanController().calNetTotalBilledDepartmentItemStore(fromDate, toDate, department);
    }

    public void fillDepartmentTransfersIssueByBill() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.StoreTransferIssue);
        if (fromDepartment != null && toDepartment != null) {
            m.put("fdept", fromDepartment);
            m.put("tdept", toDepartment);
            sql = "select b from Bill b where b.department=:fdept"
                    + " and b.toDepartment=:tdept "
                    + " and b.createdAt between :fd "
                    + " and :td and b.billType=:bt"
                    + " order by b.id";
        } else if (fromDepartment == null && toDepartment != null) {
            m.put("tdept", toDepartment);
            sql = "select b from Bill b where"
                    + " b.toDepartment=:tdept and b.createdAt "
                    + " between :fd and :td and "
                    + " b.billType=:bt order by b.id";
        } else if (fromDepartment != null && toDepartment == null) {
            m.put("fdept", fromDepartment);
            sql = "select b from Bill b where "
                    + " b.department=:fdept and b.createdAt "
                    + " between :fd and :td and"
                    + "  b.billType=:bt order by b.id";
        } else {
            sql = "select b from Bill b where b.createdAt "
                    + " between :fd and :td and b.billType=:bt order by b.id";
        }
        transferBills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }
    }

    public void fillDepartmentTransfersRecieveByBill() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.StoreTransferReceive);
        if (fromDepartment != null && toDepartment != null) {
            m.put("fdept", fromDepartment);
            m.put("tdept", toDepartment);
            sql = "select b from Bill b where b.fromDepartment=:fdept"
                    + " and b.department=:tdept and b.createdAt between :fd "
                    + "and :td and b.billType=:bt order by b.id";
        } else if (fromDepartment == null && toDepartment != null) {
            m.put("tdept", toDepartment);
            sql = "select b from Bill b where b.department=:tdept and b.createdAt "
                    + " between :fd and :td and b.billType=:bt order by b.id";
        } else if (fromDepartment != null && toDepartment == null) {
            m.put("fdept", fromDepartment);
            sql = "select b from Bill b where b.fromDepartment=:fdept and b.createdAt "
                    + " between :fd and :td and b.billType=:bt order by b.id";
        } else {
            sql = "select b from Bill b where b.createdAt "
                    + " between :fd and :td and b.billType=:bt order by b.id";
        }
        transferBills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }
    }

    /**
     * Getters & Setters
     *
     * @return
     */
    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
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
    public StoreReportsTransfer() {
    }

    public double getSaleValue() {
        return saleValue;
    }

    public void setSaleValue(double saleValue) {
        this.saleValue = saleValue;
    }

    public double getStockPurchaseValue() {
        return purchaseValue;
    }

    public void setStockPurchaseValue(double stockPurchaseValue) {
        this.purchaseValue = stockPurchaseValue;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public List<BillItem> getTransferItems() {
        return transferItems;
    }

    public void setTransferItems(List<BillItem> transferItems) {
        this.transferItems = transferItems;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.java.CommonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public List<StockReportRecord> getMovementRecords() {
        return movementRecords;
    }

    public void setMovementRecords(List<StockReportRecord> movementRecords) {
        this.movementRecords = movementRecords;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public StoreBean getStoreBean() {
        return StoreBean;
    }

    public void setStoreBean(StoreBean StoreBean) {
        this.StoreBean = StoreBean;
    }

    public List<StockReportRecord> getMovementRecordsQty() {
        return movementRecordsQty;
    }

    public void setMovementRecordsQty(List<StockReportRecord> movementRecordsQty) {
        this.movementRecordsQty = movementRecordsQty;
    }

    public List<Bill> getTransferBills() {
        return transferBills;
    }

    public void setTransferBills(List<Bill> transferBills) {
        this.transferBills = transferBills;
    }

    public BillFacade getBillFacade() {
        return BillFacade;
    }

    public void setBillFacade(BillFacade BillFacade) {
        this.BillFacade = BillFacade;
    }

    public double getTotalsValue() {
        return totalsValue;
    }

    public void setTotalsValue(double totalsValue) {
        this.totalsValue = totalsValue;
    }

    public double getDiscountsValue() {
        return discountsValue;
    }

    public void setDiscountsValue(double discountsValue) {
        this.discountsValue = discountsValue;
    }

    public double getNetTotalValues() {
        return netTotalValues;
    }

    public void setNetTotalValues(double netTotalValues) {
        this.netTotalValues = netTotalValues;
    }

    public List<String1Value3> getListz() {
        return listz;
    }

    public void setListz(List<String1Value3> listz) {
        this.listz = listz;
    }

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public class DepartmentBillRow {

        Department department;
        Bill bill;
        List<ItemBillRow> itemBillRows;
        List<CategoryBillRow> categoryBillRows;

        public Department getDepartment() {
            return department;
        }

        public void setDepartment(Department department) {
            this.department = department;
        }

        public Bill getBill() {
            if (bill == null) {
                bill = new Bill();
            }
            return bill;
        }

        public void setBill(Bill bill) {
            this.bill = bill;
        }

        public List<ItemBillRow> getItemBillRows() {
            if (itemBillRows == null) {
                itemBillRows = new ArrayList<>();
            }
            return itemBillRows;
        }

        public void setItemBillRows(List<ItemBillRow> itemBillRows) {
            this.itemBillRows = itemBillRows;
        }

        public List<CategoryBillRow> getCategoryBillRows() {
            if (categoryBillRows == null) {
                categoryBillRows = new ArrayList<>();
            }
            return categoryBillRows;
        }

        public void setCategoryBillRows(List<CategoryBillRow> categoryBillRows) {
            this.categoryBillRows = categoryBillRows;
        }

        public DepartmentBillRow() {
        }

        public DepartmentBillRow(Department department, List<ItemBillRow> itemBillRows) {
            this.department = department;
            this.itemBillRows = itemBillRows;
        }

        public DepartmentBillRow(Department department, List<ItemBillRow> itemBillRows, Double netTotal) {
            this.department = department;
            this.itemBillRows = itemBillRows;
            bill = new Bill();
            bill.setNetTotal(netTotal);
        }

    }

    public class CategoryBillRow {

        Category category;
        List<ItemBillRow> itemBillRows;
        Bill bill;

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public List<ItemBillRow> getItemBillRows() {
            if (itemBillRows == null) {
                itemBillRows = new ArrayList<>();
            }
            return itemBillRows;
        }

        public void setItemBillRows(List<ItemBillRow> itemBillRows) {
            this.itemBillRows = itemBillRows;
        }

        public Bill getBill() {
            if (bill == null) {
                bill = new Bill();
            }
            return bill;
        }

        public void setBill(Bill bill) {
            this.bill = bill;
        }

    }

    public class ItemBillRow {

        Item item;
        Bill bill;

        public ItemBillRow() {
        }

        public ItemBillRow(Item item, Double netTotal) {
            this.item = item;
            bill = new Bill();
            bill.setNetTotal(netTotal);
        }

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        public Bill getBill() {
            if (bill == null) {
                bill = new Bill();
            }
            return bill;
        }

        public void setBill(Bill bill) {
            this.bill = bill;
        }

    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

}
