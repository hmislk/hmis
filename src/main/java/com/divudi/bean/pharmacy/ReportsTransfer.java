/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.data.BillType;
import com.divudi.data.dataStructure.StockReportRecord;
import com.divudi.data.table.String1Value3;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.BillItem;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.StockFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import org.joda.time.Days;
import org.joda.time.LocalDate;

/**
 *
 * @author Buddhika
 */
@Named(value = "reportsTransfer")
@SessionScoped
public class ReportsTransfer implements Serializable {

    /**
     * Bean Variables
     */
    Department fromDepartment;
    Department toDepartment;
    Department department;
    Date fromDate;
    Date toDate;
    BillType[] billTypes;

    Institution institution;
    List<Stock> stocks;
    List<ItemCount> itemCounts;
    double saleValue;
    double purchaseValue;
    double totalsValue;
    double discountsValue;
    double marginValue;
    double netTotalValues;

    List<BillItem> transferItems;
    List<Bill> transferBills;

    List<StockReportRecord> movementRecords;
    List<StockReportRecord> movementRecordsQty;
    @Inject
    BillBeanController billBeanController;

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
    PharmacyBean pharmacyBean;

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

    public BillBeanController getBillBeanController() {
        return billBeanController;
    }

    public void setBillBeanController(BillBeanController billBeanController) {
        this.billBeanController = billBeanController;
    }

    public void fillMovingWithStock() {
        String sql;
        Map m = new HashMap();
        m.put("i", institution);
        m.put("t1", BillType.PharmacyTransferIssue);
        m.put("t2", BillType.PharmacyPre);
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
            r.setStockQty(getPharmacyBean().getStockQty(r.getItem(), institution));
            movementRecordsQty.add(r);
        }
    }

    public void fillMoving(boolean fast) {
        String sql;
        Map m = new HashMap();
//        m.put("r", StockReportRecord.class);
        m.put("d", department);
//        m.put("t1", BillType.PharmacyTransferIssue);
//        m.put("t2", BillType.PharmacyPre);
        m.put("fd", fromDate);
        m.put("td", toDate);
        List<BillType> bts = Arrays.asList(billTypes);
        m.put("bt", bts);
        BillItem bi = new BillItem();

        if (!fast) {
            sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                    + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                    + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty))  "
                    + "FROM BillItem bi where bi.retired=false and bi.bill.department=:d and "
                    + "bi.bill.billDate between :fd and :td "
                    + "and bi.bill.billType in :bt "
                    + "group by bi.item "
                    + "order by "
                    + "SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate  * bi.pharmaceuticalBillItem.qty) "
                    + "desc";
        } else {
            sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                    + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                    + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty)) "
                    + "FROM BillItem bi where bi.retired=false and bi.bill.department=:d and "
                    + "bi.bill.billType in :bt and "
                    + "bi.bill.billDate between :fd and :td group by bi.item "
                    + "order by  SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.pharmaceuticalBillItem.qty) ";
        }
        //System.out.println("sql = " + sql);
        //System.out.println("m = " + m);
        List<Object[]> objs = getBillItemFacade().findAggregates(sql, m);
        movementRecords = new ArrayList<>();
        for (Object[] obj : objs) {
            StockReportRecord r = new StockReportRecord();
            r.setItem((Item) obj[0]);
            r.setQty((Double) obj[1]);
            r.setPurchaseValue((Double) obj[2]);
            r.setRetailsaleValue((Double) obj[3]);
            r.setStockQty(getPharmacyBean().getStockByPurchaseValue(r.getItem(), department));
            movementRecords.add(r);
        }
    }

    public void fillMovingQty(boolean fast) {
        String sql;
        Map m = new HashMap();

        List<BillType> bts = Arrays.asList(billTypes);

        m.put("d", department);
//        m.put("t1", BillType.PharmacyTransferIssue);
//        m.put("t2", BillType.PharmacyPre);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", bts);

        BillItem bi = new BillItem();
        if (!fast) {
            sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                    + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                    + "SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty)) "
                    + "FROM BillItem bi where bi.retired=false and  bi.bill.department=:d and "
                    + "bi.billType in :bt"
                    + "bi.bill.billDate between :fd and :td group by bi.item "
                    + "order by  SUM(bi.pharmaceuticalBillItem.qty) desc";
        } else {
            sql = "select bi.item, abs(SUM(bi.pharmaceuticalBillItem.qty)), "
                    + "abs(SUM(bi.pharmaceuticalBillItem.stock.itemBatch.purcahseRate * bi.pharmaceuticalBillItem.qty)), "
                    + "SUM(bi.pharmaceuticalBillItem.stock.itemBatch.retailsaleRate * bi.qty) "
                    + "FROM BillItem bi where bi.retired=false and bi.bill.department=:d and "
                    + "(bi.bill.billType in :bt) "
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
            r.setStockQty(getPharmacyBean().getStockByPurchaseValue(r.getItem(), department));
            movementRecordsQty.add(r);
        }
    }

    public void fillDepartmentTransfersReceive() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyTransferReceive);
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

    public void fillDepartmentTransfersIssueByBillItem() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyTransferIssue);
        if (fromDepartment != null && toDepartment != null) {
            m.put("fdept", fromDepartment);
            m.put("tdept", toDepartment);
            sql = "select bi from BillItem bi where bi.bill.department=:fdept"
                    + " and bi.bill.toDepartment=:tdept and bi.bill.createdAt between :fd "
                    + "and :td and bi.bill.billType=:bt order by bi.id";
        } else if (fromDepartment == null && toDepartment != null) {
            m.put("tdept", toDepartment);
            sql = "select bi from BillItem bi where bi.bill.toDepartment=:tdept and bi.bill.createdAt "
                    + " between :fd and :td and bi.bill.billType=:bt order by bi.id";
        } else if (fromDepartment != null && toDepartment == null) {
            m.put("fdept", fromDepartment);
            sql = "select bi from BillItem bi where bi.bill.department=:fdept and bi.bill.createdAt "
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

    public void fillDepartmentTransfersIssueByBill() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyTransferIssue);
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
        transferBills = getBillFacade().findBySQL(sql, m);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }
    }

    public void fillDepartmentBHTIssueByBill() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyBhtPre);
        m.put("fdept", fromDepartment);
        sql = "select b from Bill b where "
                + " b.department=:fdept and b.createdAt "
                + " between :fd and :td and"
                + "  b.billType=:bt order by b.id";
        transferBills = getBillFacade().findBySQL(sql, m);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }
    }

    List<String1Value3> listz;

    public List<String1Value3> getListz() {

        return listz;
    }

    public void setListz(List<String1Value3> listz) {
        this.listz = listz;
    }

    public void createDepartmentIssue() {
        listz = new ArrayList<>();

        List<Object[]> list = getBillBeanController().fetchBilledDepartmentItem(getFromDate(), getToDate(), getFromDepartment());
        if (list == null) {
            return;
        }

        for (Object[] obj : list) {
            Department item = (Department) obj[0];
            Double dbl = (Double) obj[1];
            //double count = 0;

            String1Value3 newD = new String1Value3();
            newD.setString(item.getName());
            newD.setValue1(dbl);
            newD.setSummery(false);
            listz.add(newD);

        }

        netTotalValues = getBillBeanController().calNetTotalBilledDepartmentItem(fromDate, toDate, department);

    }

    public void fillDepartmentUnitIssueByBill() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyIssue);
        m.put("fdept", fromDepartment);
        m.put("tdept", toDepartment);
        sql = "select b from Bill b where "
                + " b.fromDepartment=:fdept and "
                + " b.toDepartment=:tdept and "
                + " b.createdAt "
                + " between :fd and :td and "
                + " b.billType=:bt order by b.id";
        transferBills = getBillFacade().findBySQL(sql, m);
        totalsValue = 0.0;
        discountsValue = 0.0;
        netTotalValues = 0.0;
        for (Bill b : transferBills) {
            totalsValue = totalsValue + (b.getTotal());
            discountsValue = discountsValue + b.getDiscount();
            netTotalValues = netTotalValues + b.getNetTotal();
        }
    }

    private List<Object[]> fetchBillItem() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyIssue);
        m.put("fdept", fromDepartment);

        sql = "select b.item,"
                + " sum(b.grossValue),"
                + " sum(b.marginValue),"
                + " sum(b.discount),"
                + " sum(b.netValue)"
                + " from BillItem b "
                + " where b.bill.fromDepartment=:fdept ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt"
                + " group by b.item "
                + " order by b.item.name";

        return getBillFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);
    }

    private Object[] fetchBill() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyIssue);
        m.put("fdept", fromDepartment);

        sql = "select sum(b.total),"
                + " sum(b.margin),"
                + " sum(b.discount),"
                + " sum(b.netTotal)"
                + " from Bill b "
                + " where b.fromDepartment=:fdept ";

        if (toDepartment != null) {
            sql += " and b.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        sql += " and b.createdAt between :fd and :td"
                + " and b.billType=:bt";

        return getBillFacade().findAggregate(sql, m, TemporalType.TIMESTAMP);
    }

    public void fillItemCounts() {

        List<Object[]> list = fetchBillItem();

        if (list == null) {
            return;
        }

        itemCounts = new ArrayList<>();
        totalsValue = 0;
        marginValue = 0;
        netTotalValues = 0;
        for (Object[] obj : list) {
            ItemCount row = new ItemCount();
            row.setItem((Item) obj[0]);
            row.setGross((Double) obj[1]);
            row.setMargin((Double) obj[2]);
            row.setDiscount((Double) obj[3]);
            row.setNet((Double) obj[4]);

            Double pre = calCount(row.getItem(), new PreBill());
            Double preCancel = calCountCan(row.getItem(), new PreBill());
            Double returned = calCountReturn(row.getItem(), new RefundBill());
            System.err.println("PRE " + pre);
            System.err.println("PRE CAN " + preCancel);
            System.err.println("Return " + returned);
//            long retturnedCancel = calCountCan(row.getItem(), new RefundBill());

            row.setCount(pre - (preCancel + returned));

            totalsValue += row.getGross();
            marginValue += row.getMargin();
            discountsValue += row.getDiscount();
            netTotalValues += row.getNet();

            itemCounts.add(row);
        }

        Object[] listBill = fetchBill();

        billTotal = (Double) listBill[0];
        billMargin = (Double) listBill[1];
        billDiscount = (Double) listBill[2];
        billNetTotal = (Double) listBill[3];

    }

    double billTotal;
    double billMargin;
    double billDiscount;
    double billNetTotal;

    private Double calCount(Item item, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", BillType.PharmacyIssue);
        m.put("fdept", fromDepartment);

        sql = "select sum(b.qty)"
                + " from BillItem b "
                + " where b.bill.fromDepartment=:fdept "
                + " and b.bill.billedBill is null "
                + " and type(b.bill)=:class "
                + " and b.item=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Double calCountReturn(Item item, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", BillType.PharmacyIssue);
        m.put("fdept", fromDepartment);

        sql = "select sum(b.qty)"
                + " from BillItem b "
                + " where b.bill.fromDepartment=:fdept "
                + " and b.bill.billedBill is not null "
                + " and type(b.bill)=:class "
                + " and b.item=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Double calCountCan(Item item, Bill bill) {

        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("class", bill.getClass());
        m.put("itm", item);
        m.put("bt", BillType.PharmacyIssue);
        m.put("fdept", fromDepartment);

        sql = "select sum(b.qty)"
                + " from BillItem b "
                + " where b.bill.fromDepartment=:fdept "
                + " and b.bill.billedBill is not null "
                + " and type(b.bill)=:class "
                + " and b.item=:itm ";

        if (toDepartment != null) {
            sql += " and b.bill.toDepartment=:tdept ";
            m.put("tdept", toDepartment);
        }

        sql += " and b.bill.createdAt between :fd and :td"
                + " and b.bill.billType=:bt";

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void fillDepartmentTransfersRecieveByBill() {
        Map m = new HashMap();
        String sql;
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.PharmacyTransferIssue);
        if (fromDepartment != null && toDepartment != null) {
            m.put("fdept", fromDepartment);
            m.put("tdept", toDepartment);
            sql = "select b from Bill b where b.department=:fdept"
                    + " and b.toDepartment=:tdept and b.createdAt between :fd "
                    + "and :td and b.billType=:bt order by b.id";
        } else if (fromDepartment == null && toDepartment != null) {
            m.put("tdept", toDepartment);
            sql = "select b from Bill b where b.toDepartment=:tdept and b.createdAt "
                    + " between :fd and :td and b.billType=:bt order by b.id";
        } else if (fromDepartment != null && toDepartment == null) {
            m.put("fdept", fromDepartment);
            sql = "select b from Bill b where b.department=:fdept and b.createdAt "
                    + " between :fd and :td and b.billType=:bt order by b.id";
        } else {
            sql = "select b from Bill b where b.createdAt "
                    + " between :fd and :td and b.billType=:bt order by b.id";
        }
        transferBills = getBillFacade().findBySQL(sql, m);
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
    public ReportsTransfer() {
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
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
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

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
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

    public BillType[] getBillTypes() {
        if (billTypes == null) {
            billTypes = new BillType[]{BillType.PharmacySale, BillType.PharmacyIssue};
        }
        return billTypes;
    }

    public void setBillTypes(BillType[] billTypes) {
        this.billTypes = billTypes;
    }

    public class ItemCount {

        Item item;
        double count;
        double gross;
        double margin;
        double discount;
        double net;

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }

        public double getCount() {
            return count;
        }

        public void setCount(double count) {
            this.count = count;
        }

        public double getGross() {
            return gross;
        }

        public void setGross(double gross) {
            this.gross = gross;
        }

        public double getMargin() {
            return margin;
        }

        public void setMargin(double margin) {
            this.margin = margin;
        }

        public double getNet() {
            return net;
        }

        public void setNet(double net) {
            this.net = net;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

    }

    public List<ItemCount> getItemCounts() {
        return itemCounts;
    }

    public void setItemCounts(List<ItemCount> itemCounts) {
        this.itemCounts = itemCounts;
    }

    public double getMarginValue() {
        return marginValue;
    }

    public void setMarginValue(double marginValue) {
        this.marginValue = marginValue;
    }

    public double getBillTotal() {
        return billTotal;
    }

    public void setBillTotal(double billTotal) {
        this.billTotal = billTotal;
    }

    public double getBillMargin() {
        return billMargin;
    }

    public void setBillMargin(double billMargin) {
        this.billMargin = billMargin;
    }

    public double getBillDiscount() {
        return billDiscount;
    }

    public void setBillDiscount(double billDiscount) {
        this.billDiscount = billDiscount;
    }

    public double getBillNetTotal() {
        return billNetTotal;
    }

    public void setBillNetTotal(double billNetTotal) {
        this.billNetTotal = billNetTotal;
    }

}
