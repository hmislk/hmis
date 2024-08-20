/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.store;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.dataStructure.StockReportRecord;
import com.divudi.data.table.String1Value3;

import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.RefundBill;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
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
    @Inject
    CommonController commonController;

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
    List<DepartmentCategoryRow> departmentCategoryRows;
    List<CaregoryRow> caregoryRows;
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
        //////System.out.println("sql = " + sql);
        //////System.out.println("m = " + m);
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
        Date startTime = new Date();

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
        transferItems = getBillItemFacade().findByJpql(sql, m);
        purchaseValue = 0.0;
        saleValue = 0.0;
        for (BillItem ts : transferItems) {
            purchaseValue = purchaseValue + (ts.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate() * ts.getPharmaceuticalBillItem().getQtyInUnit());
            saleValue = saleValue + (ts.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate() * ts.getPharmaceuticalBillItem().getQtyInUnit());
        }

        
    }

    public void fillDepartmentUnitIssueByBillStore() {
        Date startTime = new Date();

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

        transferBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
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
        Date startTime = new Date();

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

        transferItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
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
        Date startTime = new Date();

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
        transferBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
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
        Date startTime = new Date();

        listz = new ArrayList<>();
        List<Object[]> list;

        if (department == null) {
            list = getBillBeanController().fetchBilledDepartmentItemStore(getFromDate(), getToDate(), getSessionController().getDepartment());
        } else {
            list = getBillBeanController().fetchBilledDepartmentItemStore(getFromDate(), getToDate(), department);
        }

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

        transferItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        purchaseValue = 0.0;
        for (BillItem bi : transferItems) {
            purchaseValue += (bi.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate() * bi.getPharmaceuticalBillItem().getQtyInUnit());
        }

    }

    public void fillDepartmentTransfersIssueByBillItem() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt1", BillType.StoreTransferIssue);
//        m.put("bt1", BillType.StoreIssue);

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

        ////System.out.println("sql = " + sql);
        ////System.out.println("m = " + m);
        transferItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
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
        ////System.out.println("transferItems = " + transferItems);

        for (BillItem ts : transferItems) {
            purchaseValue += ts.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate() * ts.getPharmaceuticalBillItem().getQty();
            saleValue += ts.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate() * ts.getPharmaceuticalBillItem().getQty();

            if (dept != null && dept.equals(ts.getBill().getToDepartment())) {
                ////System.out.println("old dept");

                if (cat != null && cat.equals(ts.getItem().getCategory())) {
                    ////System.out.println("old cat");

                    if (item != null && item.equals(ts.getItem())) {
                        ////System.out.println("old item");

                    } else {
                        ////System.out.println("new item");

                        item = ts.getItem();

                        ibr = new ItemBillRow();
                        ibr.setItem(ts.getItem());

                        cbr.getItemBillRows().add(ibr);
                    }
                } else {
                    ////System.out.println("new cat");

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

                ////System.out.println("new dept");
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
                ////System.out.println("drows = " + drows);
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

    public void fillDepartmentIssueByBillItem() {
        Date startTime = new Date();

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

        ////System.out.println("sql = " + sql);
        ////System.out.println("m = " + m);
        transferItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
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
        ////System.out.println("transferItems = " + transferItems);

        for (BillItem ts : transferItems) {
            purchaseValue += ts.getPharmaceuticalBillItem().getItemBatch().getPurcahseRate() * ts.getPharmaceuticalBillItem().getQty();
            saleValue += ts.getPharmaceuticalBillItem().getItemBatch().getRetailsaleRate() * ts.getPharmaceuticalBillItem().getQty();

            if (dept != null && dept.equals(ts.getBill().getToDepartment())) {
                ////System.out.println("old dept");

                if (cat != null && cat.equals(ts.getItem().getCategory())) {
                    ////System.out.println("old cat");

                    if (item != null && item.equals(ts.getItem())) {
                        ////System.out.println("old item");

                    } else {
                        ////System.out.println("new item");

                        item = ts.getItem();

                        ibr = new ItemBillRow();
                        ibr.setItem(ts.getItem());

                        cbr.getItemBillRows().add(ibr);
                    }
                } else {
                    ////System.out.println("new cat");

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

                ////System.out.println("new dept");
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
                ////System.out.println("drows = " + drows);
            }

            ibr.getBill().setNetTotal(ibr.getBill().getNetTotal() + ts.getNetValue());

            cbr.getBill().setNetTotal(cbr.getBill().getNetTotal() + ts.getNetValue());

            dbr.getBill().setNetTotal(dbr.getBill().getNetTotal() + ts.getNetValue());
            if (ts.getBill() instanceof RefundBill) {
                ibr.getBill().setGrantTotal(ibr.getBill().getGrantTotal() - ts.getQty());
                cbr.getBill().setGrantTotal(cbr.getBill().getGrantTotal() - ts.getQty());
                dbr.getBill().setGrantTotal(dbr.getBill().getGrantTotal() - ts.getQty());
            } else {
                ibr.getBill().setGrantTotal(ibr.getBill().getGrantTotal() + ts.getQty());
                cbr.getBill().setGrantTotal(cbr.getBill().getGrantTotal() + ts.getQty());
                dbr.getBill().setGrantTotal(dbr.getBill().getGrantTotal() + ts.getQty());

            }

//            purchaseValue += ts.getNetValue();
        }

        purchaseValue = getBillBeanController().calNetTotalBilledDepartmentItemStore(fromDate, toDate, department);

        
    }

    public void fillDepartmentTransfersIssueByBill() {
        Date startTime = new Date();

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
        transferBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
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
        Date startTime = new Date();

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
        transferBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }

        
    }

    public void createStoreIssueReport() {
        Date startTime = new Date();
        departmentCategoryRows = new ArrayList<>();
        List<Object[]> objects = fetchBillItemDetails(fromDepartment, null, fromDate, toDate, BillType.StoreIssue);
        Department lastDepartment = null;
        Category lastCategory = null;
        Item lastItem = null;
        DepartmentCategoryRow dcr = new DepartmentCategoryRow();
        CaregoryRow cr = new CaregoryRow();
        ItemRow ir = new ItemRow();
        for (Object[] ob : objects) {
            Department d = (Department) ob[0];
            //System.out.println("d.getName() = " + d.getName());
            Category c = (Category) ob[1];
            //System.out.println("c.getName() = " + c.getName());
            Item i = (Item) ob[2];
            //System.out.println("i.getName() = " + i.getName());
            BillClassType type = (BillClassType) ob[3];
            double count = (double) ob[4];
            double unitValue = (double) ob[5];
            double total = (double) ob[6];
            if (lastDepartment == null) {
                lastDepartment = d;
                dcr.setD(d);
                if (lastCategory == null) {
                    lastCategory = c;
                    cr.setC(c);
                    if (lastItem == null) {
                        lastItem = i;
                        ir.setI(i);
                        ir = createItemRow(ir, type, count, unitValue, total);
                    } else {
                        if (lastItem == i) {
                            ir = createItemRow(ir, type, count, unitValue, total);
                        } else {
                            cr.getItemRows().add(ir);
                            ir = new ItemRow();
                            lastItem = i;
                            ir.setI(i);
                            ir = createItemRow(ir, type, count, unitValue, total);
                        }
                    }
                } else {
                    if (lastCategory == c) {
                        if (lastItem == null) {
                            lastItem = i;
                            ir.setI(i);
                            ir = createItemRow(ir, type, count, unitValue, total);
                        } else {
                            if (lastItem == i) {
                                ir = createItemRow(ir, type, count, unitValue, total);
                            } else {
                                cr.getItemRows().add(ir);
                                ir = new ItemRow();
                                lastItem = i;
                                ir.setI(i);
                                ir = createItemRow(ir, type, count, unitValue, total);
                            }
                        }
                    } else {
                        lastCategory = c;
                        cr.getItemRows().add(ir);
                        dcr.getCaregoryRows().add(cr);
                        cr = new CaregoryRow();
                        cr.setC(c);
                        if (lastItem == null) {
                            lastItem = i;
                            ir.setI(i);
                            ir = createItemRow(ir, type, count, unitValue, total);
                        } else {
                            if (lastItem == i) {
                                ir = createItemRow(ir, type, count, unitValue, total);
                            } else {
                                cr.getItemRows().add(ir);
                                ir = new ItemRow();
                                lastItem = i;
                                ir.setI(i);
                                ir = createItemRow(ir, type, count, unitValue, total);
                            }
                        }
                    }
                }
            } else {
                if (lastDepartment == d) {
                    if (lastCategory == c) {
                        if (lastItem == null) {
                            lastItem = i;
                            ir.setI(i);
                            ir = createItemRow(ir, type, count, unitValue, total);
                        } else {
                            if (lastItem == i) {
                                ir = createItemRow(ir, type, count, unitValue, total);
                            } else {
                                cr.getItemRows().add(ir);
                                ir = new ItemRow();
                                lastItem = i;
                                ir.setI(i);
                                ir = createItemRow(ir, type, count, unitValue, total);
                            }
                        }
                    } else {
                        lastCategory = c;
                        cr.getItemRows().add(ir);
                        dcr.getCaregoryRows().add(cr);
                        cr = new CaregoryRow();
                        cr.setC(c);
                        if (lastItem == null) {
                            lastItem = i;
                            ir.setI(i);
                            ir = createItemRow(ir, type, count, unitValue, total);
                        } else {
                            if (lastItem == i) {
                                ir = createItemRow(ir, type, count, unitValue, total);
                            } else {
                                cr.getItemRows().add(ir);
                                ir = new ItemRow();
                                lastItem = i;
                                ir.setI(i);
                                ir = createItemRow(ir, type, count, unitValue, total);
                            }
                        }
                    }
                } else {
                    cr.getItemRows().add(ir);
                    dcr.getCaregoryRows().add(cr);
                    departmentCategoryRows.add(dcr);
                    cr = new CaregoryRow();
                    dcr = new DepartmentCategoryRow();
                    dcr.setD(d);
                    lastDepartment = d;
                    if (lastCategory == c) {
                        if (lastItem == null) {
                            lastItem = i;
                            ir.setI(i);
                            ir = createItemRow(ir, type, count, unitValue, total);
                        } else {
                            if (lastItem == i) {
                                ir = createItemRow(ir, type, count, unitValue, total);
                            } else {
                                cr.getItemRows().add(ir);
                                ir = new ItemRow();
                                lastItem = i;
                                ir.setI(i);
                                ir = createItemRow(ir, type, count, unitValue, total);
                            }
                        }
                    } else {
                        lastCategory = c;
                        cr.getItemRows().add(ir);
                        dcr.getCaregoryRows().add(cr);
                        cr = new CaregoryRow();
                        cr.setC(c);
                        if (lastItem == null) {
                            lastItem = i;
                            ir.setI(i);
                            ir = createItemRow(ir, type, count, unitValue, total);
                        } else {
                            if (lastItem == i) {
                                ir = createItemRow(ir, type, count, unitValue, total);
                            } else {
                                cr.getItemRows().add(ir);
                                ir = new ItemRow();
                                lastItem = i;
                                ir.setI(i);
                                ir = createItemRow(ir, type, count, unitValue, total);
                            }
                        }
                    }
                }
            }
        }
        cr.getItemRows().add(ir);
        dcr.getCaregoryRows().add(cr);
        departmentCategoryRows.add(dcr);

        
    }

    public void createStoreIssueCategoryReport() {
        if (fromDepartment == null) {
            JsfUtil.addErrorMessage("Please Select Issued Department");
            return;
        }
        Date startTime = new Date();
        caregoryRows = new ArrayList<>();
        saleValue = 0.0;
        List<Object[]> objects = fetchBillItemDetails(fromDepartment, fromDate, toDate, BillType.StoreIssue, toDepartment);
        Category lastCategory = null;
        Item lastItem = null;
        DepartmentCategoryRow dcr = new DepartmentCategoryRow();
        CaregoryRow cr = new CaregoryRow();
        ItemRow ir = new ItemRow();
        for (Object[] ob : objects) {
            Category c = (Category) ob[0];
            //System.out.println("c.getName() = " + c.getName());
            Item i = (Item) ob[1];
            BillClassType type = (BillClassType) ob[2];
            double count = (double) ob[3];
//            //System.out.println("count = " + count);
            double unitValue = (double) ob[4];
//            //System.out.println("unitValue = " + unitValue);
            double total = (double) ob[5];
            if (lastCategory == null) {
                lastCategory = c;
                cr.setC(c);
                if (lastItem == null) {
                    lastItem = i;
                    ir.setI(i);
                    ir = createItemRow(ir, type, count, unitValue, total);
//                    cr.setTotal(cr.getTotal() + ir.getValue());
                } else {
                    if (lastItem == i) {
                        ir = createItemRow(ir, type, count, unitValue, total);
//                        cr.setTotal(cr.getTotal() + ir.getValue());
                    } else {
                        cr.getItemRows().add(ir);
                        ir = new ItemRow();
                        lastItem = i;
                        ir.setI(i);
                        ir = createItemRow(ir, type, count, unitValue, total);
//                        cr.setTotal(cr.getTotal() + ir.getValue());
                    }
                }
            } else {
                if (lastCategory == c) {
                    if (lastItem == null) {
                        lastItem = i;
                        ir.setI(i);
                        ir = createItemRow(ir, type, count, unitValue, total);
//                        cr.setTotal(cr.getTotal() + ir.getValue());
                    } else {
                        if (lastItem == i) {
                            ir = createItemRow(ir, type, count, unitValue, total);
//                            //System.out.println("ir.getValue() = " + ir.getValue());
//                            cr.setTotal(cr.getTotal() + ir.getValue());
                        } else {
                            cr.getItemRows().add(ir);
                            cr.setTotal(cr.getTotal() + ir.getValue());
                            ir = new ItemRow();
                            lastItem = i;
                            ir.setI(i);
                            ir = createItemRow(ir, type, count, unitValue, total);
//                            cr.setTotal(cr.getTotal() + ir.getValue());
                        }
                    }
                } else {
                    lastCategory = c;
                    cr.getItemRows().add(ir);
                    cr.setTotal(cr.getTotal() + ir.getValue());
                    saleValue += cr.getTotal();
                    caregoryRows.add(cr);
                    ir = new ItemRow();
                    cr = new CaregoryRow();
                    cr.setC(c);
                    if (lastItem == null) {
                        lastItem = i;
                        ir.setI(i);
                        ir = createItemRow(ir, type, count, unitValue, total);
//                        cr.setTotal(cr.getTotal() + ir.getValue());
                    } else {
                        if (lastItem == i) {
                            ir = createItemRow(ir, type, count, unitValue, total);
//                            cr.setTotal(cr.getTotal() + ir.getValue());
                        } else {
                            lastItem = i;
                            ir.setI(i);
                            ir = createItemRow(ir, type, count, unitValue, total);
//                            cr.setTotal(cr.getTotal() + ir.getValue());
                        }
                    }
                }
            }
        }
        cr.getItemRows().add(ir);
        cr.setTotal(cr.getTotal() + ir.getValue());
        saleValue += cr.getTotal();
        caregoryRows.add(cr);

        
    }

    private ItemRow createItemRow(ItemRow ir, BillClassType bct, double count, double unitValue, double total) {
        if (bct == BillClassType.PreBill) {
            ir.setQty(ir.getQty() + count);
            ir.setUnitValue(unitValue);
            ir.setValue(ir.getValue() + total);
        }
        if (bct == BillClassType.RefundBill) {
            ir.setQty(ir.getQty() - count);
            ir.setUnitValue(unitValue);
            ir.setValue(ir.getValue() + total);
        }
        return ir;
    }

    public List<Object[]> fetchBillItemDetails(Department fdep, Department tdep, Date fd, Date td, BillType bt) {
        Map m = new HashMap();
        String sql;
        List<Object[]> objects = new ArrayList<>();

        sql = "select bi.bill.toDepartment, "
                + " bi.item.category, "
                + " bi.item, "
                + " bi.bill.billClassType, "
                + " sum(bi.qty), "
                + " bi.Rate,"
                + " sum(bi.netValue) "
                + " from BillItem bi where "
                + " bi.bill.createdAt between :fd and :td"
                + " and bi.bill.billType=:bt";

        if (fdep != null) {
            sql += " and bi.bill.fromDepartment=:fdept ";
            m.put("fdept", fdep);
        }

        if (tdep != null) {
            sql += " and bi.bill.toDepartment=:tdept  ";
            m.put("tdept", tdep);
        }

        sql += " group by bi.bill.toDepartment,"
                + " bi.item.category,"
                + " bi.item,"
                + " bi.bill.billClassType "
                + " order by bi.bill.toDepartment.name, "
                + " bi.item.category.name, "
                + " bi.item.name, "
                + " bi.bill.billClassType ";

        m.put("fd", fd);
        m.put("td", td);
        m.put("bt", bt);

        objects = getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
        return objects;

    }

    public List<Object[]> fetchBillItemDetails(Department fdep, Date fd, Date td, BillType bt, Department tdep) {
        Map m = new HashMap();
        String sql;
        List<Object[]> objects = new ArrayList<>();

        sql = "select bi.item.category, "
                + " bi.item, "
                + " bi.bill.billClassType, "
                + " sum(bi.qty), "
                + " bi.Rate,"
                + " sum(bi.netValue) "
                + " from BillItem bi where "
                + " bi.bill.createdAt between :fd and :td"
                + " and bi.bill.billType=:bt";

        if (fdep != null) {
            sql += " and bi.bill.fromDepartment=:fdept ";
            m.put("fdept", fdep);
        }

        if (tdep != null) {
            sql += " and bi.bill.toDepartment=:tdept ";
            m.put("tdept", tdep);
        }

        sql += " group by bi.item.category,"
                + " bi.item,"
                + " bi.bill.billClassType "
                + " order by bi.item.category.name, "
                + " bi.item.name, "
                + " bi.bill.billClassType ";

        m.put("fd", fd);
        m.put("td", td);
        m.put("bt", bt);

        objects = getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
        return objects;

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

    public List<DepartmentCategoryRow> getDepartmentCategoryRows() {
        return departmentCategoryRows;
    }

    public void setDepartmentCategoryRows(List<DepartmentCategoryRow> departmentCategoryRows) {
        this.departmentCategoryRows = departmentCategoryRows;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public List<CaregoryRow> getCaregoryRows() {
        return caregoryRows;
    }

    public void setCaregoryRows(List<CaregoryRow> caregoryRows) {
        this.caregoryRows = caregoryRows;
    }

    public class DepartmentCategoryRow {

        Department d;
        List<CaregoryRow> caregoryRows;

        public Department getD() {
            return d;
        }

        public void setD(Department d) {
            this.d = d;
        }

        public List<CaregoryRow> getCaregoryRows() {
            if (caregoryRows == null) {
                caregoryRows = new ArrayList<>();
            }
            return caregoryRows;
        }

        public void setCaregoryRows(List<CaregoryRow> caregoryRows) {
            this.caregoryRows = caregoryRows;
        }
    }

    public class CaregoryRow {

        Category c;
        List<ItemRow> itemRows;
        double total;

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        public Category getC() {
            return c;
        }

        public void setC(Category c) {
            this.c = c;
        }

        public List<ItemRow> getItemRows() {
            if (itemRows == null) {
                itemRows = new ArrayList<>();
            }
            return itemRows;
        }

        public void setItemRows(List<ItemRow> itemRows) {
            this.itemRows = itemRows;
        }

    }

    public class ItemRow {

        Item i;
        double qty;
        double unitValue;
        double value;

        public Item getI() {
            return i;
        }

        public void setI(Item i) {
            this.i = i;
        }

        public double getQty() {
            return qty;
        }

        public void setQty(double qty) {
            this.qty = qty;
        }

        public double getUnitValue() {
            return unitValue;
        }

        public void setUnitValue(double unitValue) {
            this.unitValue = unitValue;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }
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
