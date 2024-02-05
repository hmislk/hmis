/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.data.BillType;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.PreBill;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
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
 * @author ruhunu
 */
@Stateless
public class PharmacyErrorCheckingEjb {

    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFacade billFacade;

    public List<BillItem> allBillItems(Item item, Date fromDate, Date toDate) {
        String sql;
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("i", item);
        sql = "select bi from BillItem bi where bi.bill.billTime between :fd and :td and b.item=:i";
        return getBillItemFacade().findByJpql(sql, m);
    }

    public double getTotalQty(BillType billType, Bill bill, Department department, Item item) {
        String sql = "Select abs(sum(p.pharmaceuticalBillItem.qty)) from BillItem p where"
                + "  type(p.bill)=:class and p.bill.createdAt is not null  and "
                + " p.item=:itm and p.bill.billType=:btp and p.bill.department=:dep ";

        HashMap hm = new HashMap();
        hm.put("itm", item);
        hm.put("btp", billType);
        hm.put("dep", department);
        hm.put("class", bill.getClass());

        double value = getBillFacade().findDoubleByJpql(sql, hm);

        //System.err.println(billType + " : " + value);
        return value;
    }

    public List<BillItem> getTotalBillItems(Department department, Item item) {
        String sql = "Select p from BillItem p where"
                + "  type(p.bill)!=:class and p.bill.createdAt is not null  and "
                + " p.item=:itm and p.bill.billType!=:btp and p.bill.department=:dep ";

        HashMap hm = new HashMap();
        hm.put("itm", item);
        hm.put("btp", BillType.PharmacyPre);
        hm.put("dep", department);
        hm.put("class", PreBill.class);

        return getBillItemFacade().findByJpql(sql, hm);

    }

    public double getTotalQtyByBillItem(BillType billType, Bill bill, Department department, Item item) {
        String sql = "Select abs(sum(p.qty)) from BillItem p where"
                + "  type(p.bill)=:class and p.bill.createdAt is not null  and "
                + " p.item=:itm and p.bill.billType=:btp and p.bill.department=:dep ";

        HashMap hm = new HashMap();
        hm.put("itm", item);
        hm.put("btp", billType);
        hm.put("dep", department);
        hm.put("class", bill.getClass());

        double value = getBillFacade().findDoubleByJpql(sql, hm);

        //System.err.println(billType + " : " + value);
        return value;
    }

    public List<BillItem> getPreSaleBillItems(BillType billType, Bill bill, Department department, Item item) {
//      String   sql = "select i  "
//                + " from BillItem i where i.bill.department=:dep"
//                + " and type(i.bill)=:class"
//                 + "  and i.bill.referenceBill.billType=:refType and "
//                 + " i.bill.referenceBill.cancelled=false"
//                + " and i.item=:itm and i.bill.billType=:btp ";        

        String sql = "Select p from BillItem p where"
                + "  type(p.bill)=:class and  "
                + " p.item=:itm and p.bill.retired=false "
                + " and p.bill.billType=:btp "
                + " and p.bill.department=:dep ";

        HashMap hm = new HashMap();
        hm.put("itm", item);
        hm.put("btp", billType);
        hm.put("dep", department);
        hm.put("class", bill.getClass());

        //newly Added
        //  hm.put("refType", BillType.PharmacySale);
        List<BillItem> list = getBillItemFacade().findByJpql(sql, hm);

        if (list == null) {
            list = new ArrayList<>();
        }

        return list;

    }

    public double getTotalQtyPreDiduction(BillType billType, Bill bill, Department department, Item item) {
//        String sql = "Select abs(sum(p.qty)) from BillItem p where"
//                + "  type(p.bill)=:class and  "
//                + " p.item=:itm and (p.bill.retired!=false or p.retired!=false ) "
//                + " and p.bill.billType=:btp "
//                + " and p.bill.department=:dep ";

        String sql = "select abs(sum(i.qty))  "
                + " from BillItem i where i.bill.department=:dep"
                + " and type(i.bill)=:class"
                + "  and i.bill.referenceBill.billType=:refType and "
                + " i.bill.referenceBill.cancelled=false"
                + " and i.item=:itm and i.bill.billType=:btp ";

        HashMap hm = new HashMap();
        hm.put("itm", item);
        hm.put("btp", billType);
        hm.put("dep", department);
        hm.put("class", bill.getClass());

        //newly Added
        hm.put("refType", BillType.PharmacySale);

        double value = getBillFacade().findDoubleByJpql(sql, hm);

        //System.err.println(billType + " : Sale Duduction " + value);
        return value;
    }

    public double getTotalQtyPreAdd(BillType billType, Bill bill, Department department, Item item) {
        String sql = "Select abs(sum(p.pharmaceuticalBillItem.qty)) from BillItem p where"
                + "  type(p.bill)=:class and p.qty!=0.0 and"
                + " p.item=:itm and (p.bill.retired=true or p.retired=true  ) "
                + " and p.bill.billType=:btp and p.bill.department=:dep ";

        HashMap hm = new HashMap();
        hm.put("itm", item);
        hm.put("btp", billType);
        hm.put("dep", department);
        hm.put("class", bill.getClass());

        double value = getBillFacade().findDoubleByJpql(sql, hm);

        //System.err.println(billType + " : Re Add To Stock " + value);
        return value;
    }

    public List<BillItem> allBillItems(Item item, Department department) {
        List<BillItem> temp;
        temp = allBillItemsForBinCard(item, department);
        return temp;
    }

    public List<BillItem> allBillItemsForBinCard(Item item, Department department) {
        String sql;
        Map m = new HashMap();
        m.put("d", department);
        m.put("i", item);
        m.put("po", BillType.PharmacyOrder);
        m.put("poa", BillType.PharmacyOrderApprove);
        sql = "select bi from BillItem bi where bi.item = :i "
                + " and bi.bill.department = :d"
                + " and bi.bill.billType != :po and bi.bill.billType != :poa"
                + " order by bi.createdAt desc";
        return getBillItemFacade().findByJpql(sql, m);
    }

    public List<BillItem> allBillItemsByDate(Item item, Department department, Date fromDate, Date toDate) {
        String sql;
        Map m = new HashMap();
        m.put("d", department);
        m.put("i", item);
        m.put("fd", fromDate);
        m.put("td", toDate);
        sql = "select bi from BillItem bi where bi.item=:i and bi.bill.department=:d "
                + " and bi.createdAt between :fd and :td ";
        return getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public List<BillItem> allBillItemsByDateOnlyStock(Item item, Department department, Date fromDate, Date toDate) {
        String sql;
        BillType[] bArr = {BillType.PharmacyPurchaseBill,
            BillType.PharmacyGrnBill,
            BillType.PharmacyGrnReturn,
            BillType.PurchaseReturn,
            BillType.PharmacyPre,
            BillType.PharmacyWholesalePre,
            BillType.PharmacyTransferIssue,
            BillType.PharmacyTransferReceive,
            BillType.PharmacyAdjustment,
            BillType.PharmacyIssue,
            BillType.PharmacyBhtPre};
        List<BillType> billTypes = Arrays.asList(bArr);
        return allBillItemsByDateOnlyStock(item, department, fromDate, toDate, billTypes);
    }

    public List<BillItem> allBillItemsByDateOnlyStockStore(Item item, Department department, Date fromDate, Date toDate) {
        String sql;
        BillType[] bArr = {BillType.StorePurchase,
            BillType.StoreGrnBill,
            BillType.StoreGrnReturn,
            BillType.StorePurchaseReturn,
            BillType.StorePre,
            BillType.StoreTransferIssue,
            BillType.StoreTransferReceive,
            BillType.StoreAdjustment,
            BillType.StoreIssue,
            BillType.StoreBhtPre};
        List<BillType> billTypes = Arrays.asList(bArr);
        return allBillItemsByDateOnlyStock(item, department, fromDate, toDate, billTypes);
    }

    public List<BillItem> allBillItemsByDateOnlyStock(Item item, Department department, Date fromDate, Date toDate, List<BillType> billTypes) {
        String sql;
        Map m = new HashMap();
        m.put("d", department);
        m.put("i", item);
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bts", billTypes);

        sql = "select bi from BillItem bi "
                + " where bi.item=:i"
                + " and bi.bill.department=:d "
                + " and bi.createdAt between :fd and :td "
                + " and bi.bill.billType in :bts "
                + " order by bi.pharmaceuticalBillItem.stock.id,"
                + " bi.createdAt ";
        return getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public List<BillItem> allBillItems2(Item item, Department department) {
        String sql;
        Map m = new HashMap();
        m.put("d", department);
        m.put("i", item);
        m.put("btp1", BillType.PharmacyTransferRequest);
        m.put("btp2", BillType.PharmacyOrder);
        m.put("btp3", BillType.PharmacyOrderApprove);
        m.put("btp4", BillType.PharmacyAdjustment);
        m.put("btp5", BillType.PharmacySale);
        sql = "select bi from BillItem bi where bi.item=:i and bi.bill.department=:d and bi.bill.createdAt is not null "
                + " and (bi.bill.billType!=:btp1 and bi.bill.billType!=:btp2 "
                + "and bi.bill.billType!=:btp3 and bi.bill.billType!=:btp4 and bi.bill.billType!=:btp5)";
        return getBillItemFacade().findByJpql(sql, m);
    }

    public List<BillItem> allBillItemsWithCreatedAt(Item item, Department department) {
        String sql;
        Map m = new HashMap();
        m.put("d", department);
        m.put("i", item);
        sql = "select bi from BillItem bi where bi.item=:i and bi.bill.department=:d"
                + "  and bi.bill.createdAt is not null";
        return getBillItemFacade().findByJpql(sql, m);
    }

    public List<BillItem> allBillItems(Item item) {
        String sql;
        Map m = new HashMap();
        m.put("i", item);
        sql = "select bi from BillItem bi where bi.item=:i";
        return getBillItemFacade().findByJpql(sql, m);
    }

    public List<Bill> errPreBills(Department dept) {
        //////System.out.println("errrPreBills");
        String sql;

        Map m = new HashMap();
        m.put("d", dept);
        sql = "select pb from PreBill pb where pb.retired=false and pb.department=:d order by pb.id";
        //////System.out.println("m = " + m);
        //////System.out.println("sql = " + sql);
        List<Bill> pbs = getBillFacade().findByJpql(sql, m);
        List<Bill> epbs = new ArrayList<>();
        epbs = new ArrayList<Bill>();
        for (Bill pb : pbs) {

            boolean err1 = false;
            Bill bb = pb.getReferenceBill();

            if (bb == null) {
                //////System.out.println("bb is null");
                if (pb.getBillItems() != null) {
                    //////System.out.println("pb has bill Items = " + pb.getBillItems());
                    for (BillItem bi : pb.getBillItems()) {
                        //////System.out.println("bi = " + bi);
                        if (bi.isRetired() != false) {
                            //////System.out.println("bi is NOT retired ");
                            //System.err.println("err1");
                            err1 = true;
                        }
                    }
                }
            } else {
                if (bb.getBillItems() != null && pb.getBillItems() != null) {
                    if (bb.getNetTotal() != pb.getNetTotal()) {
                        //////System.out.println("bb.getBillItems().size() = " + bb.getNetTotal());
                        //////System.out.println("pb.getBillItems().size() = " + pb.getNetTotal());
                        err1 = true;
                        //System.err.println("err 2");
                    }
                }
            }
            if (err1 = true) {
                epbs.add(pb);
            }

        }
        return epbs;
    }

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

}
