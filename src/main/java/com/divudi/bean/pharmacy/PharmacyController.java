/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillType;
import com.divudi.data.InstitutionType;
import com.divudi.data.dataStructure.DepartmentSale;
import com.divudi.data.dataStructure.DepartmentStock;
import com.divudi.data.dataStructure.InstitutionSale;
import com.divudi.data.dataStructure.InstitutionStock;
import com.divudi.data.dataStructure.ItemQuantityAndValues;
import com.divudi.data.dataStructure.ItemTransactionSummeryRow;
import com.divudi.data.dataStructure.StockAverage;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.entity.pharmacy.PharmaceuticalItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.AmppFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.PharmaceuticalItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class PharmacyController implements Serializable {

    private static final long serialVersionUID = 1L;
    /////
    @Inject
    private SessionController sessionController;
    @Inject
    AmpController ampController;
    //////////
    @EJB
    AmppFacade AmppFacade;
    @EJB
    PharmaceuticalItemFacade piFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private AmpFacade ampFacade;
    ///////////
    private Item pharmacyItem;
    // private double grantStock;
    private Date fromDate;
    private Date toDate;
    Department department;
    ////////
    //List<DepartmentStock> departmentStocks;
    private List<DepartmentSale> departmentSale;
    private List<BillItem> grns;
    private List<BillItem> pos;
    private List<BillItem> directPurchase;
    List<ItemTransactionSummeryRow> itemTransactionSummeryRows;
    double persentage;

    public void makeNull() {
        departmentSale = null;
//        departmentStocks = null;
        pos = null;
        grns = null;
        institutionSales = null;
        institutionStocks = null;
        institutionTransferIssue = null;
        directPurchase = null;

    }

    public List<Stock> completeAllStocks(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.department=:d and "
                + " (upper(i.itemBatch.item.name) like :n  or "
                + " upper(i.itemBatch.item.code) like :n  or  "
                + " upper(i.itemBatch.item.barcode) like :n ) "
                + " order by i.stock desc";
        items = getStockFacade().findBySQL(sql, m, 30);

        return items;
    }

    public List<Stock> completeStaffStocks(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.stock >:s and (upper(i.staff.code) like :n or upper(i.staff.person.name) like :n or upper(i.itemBatch.item.name) like :n ) order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        items = getStockFacade().findBySQL(sql, m, 20);

        return items;
    }

    public List<Department> getInstitutionDepatrments(Institution ins) {
        List<Department> d;
        HashMap hm = new HashMap();
        String sql = "Select d From Department d where d.retired=false and d.institution=:ins";
        hm.put("ins", ins);
        d = getDepartmentFacade().findBySQL(sql, hm);

        return d;
    }

    public void createAllItemTransactionSummery() {
        List<Amp> allAmps = ampController.getItems();
        Map<Long, ItemTransactionSummeryRow> m = new HashMap();

        for (Amp a : allAmps) {
            ItemTransactionSummeryRow r = new ItemTransactionSummeryRow();
            r.setItem(a);
            m.put(a.getId(), r);
        }

        BillType[] bts = new BillType[]{BillType.PharmacySale};
        BillType[] rbts = new BillType[]{BillType.PharmacyPre};
        List<ItemQuantityAndValues> rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, rbts);

        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                r.setRetailSaleQty(v.getQuantity());
                r.setRetailSaleVal(v.getValue());
            }
        }

        bts = new BillType[]{BillType.PharmacyWholeSale};
        rbts = new BillType[]{BillType.PharmacyWholesalePre};
        rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, rbts);
        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                r.setWholeSaleQty(v.getQuantity());
                r.setWholeSaleVal(v.getValue());
            }
        }

        bts = new BillType[]{BillType.PharmacyIssue};
        rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, rbts);
        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                r.setIssueQty(v.getQuantity());
                r.setIssueVal(v.getValue());
            }
        }

        bts = new BillType[]{BillType.PharmacyTransferIssue};
        rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, rbts);
        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                r.setTransferOutQty(v.getQuantity());
                r.setTransferOutVal(v.getValue());
            }
        }

        bts = new BillType[]{BillType.PharmacyBhtPre};
        rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, rbts);
        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                r.setBhtSaleQty(v.getQuantity());
                r.setBhtSaleVal(v.getValue());
            }
        }

        bts = new BillType[]{BillType.PharmacyPurchaseBill, BillType.PharmacyGrnBill};
        rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, null);
        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                r.setPurchaseQty(v.getQuantity());
                r.setPurchaseVal(v.getValue());
            }
        }

        bts = new BillType[]{BillType.PharmacyTransferReceive};
        rs = findPharmacyTrnasactionQuantityAndValues(fromDate,
                toDate, null, department, null, bts, rbts);
        for (ItemQuantityAndValues v : rs) {
            ItemTransactionSummeryRow r = m.get(v.getItem().getId());
            if (r != null) {
                r.setTransferInQty(v.getQuantity());
                r.setTransferInVal(v.getValue());
            }
        }

//        System.out.println("m = " + m);
        itemTransactionSummeryRows = new ArrayList<>(m.values());

    }

    public List<ItemQuantityAndValues> findPharmacyTrnasactionQuantityAndValues(Date fromDate,
            Date toDate,
            Institution ins,
            Department department,
            Item item,
            BillType[] billTypes,
            BillType[] referenceBillTypes) {

        if (false) {
            BillItem bi = new BillItem();
            bi.getNetValue();
            bi.getPharmaceuticalBillItem().getQty();
            System.out.println("bi = " + bi.getDeptId());
        }

        String sql;
        Map m = new HashMap();
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        sql = "select new com.divudi.data.dataStructure.ItemQuantityAndValues(i.item, "
                + "sum(i.pharmaceuticalBillItem.qty), "
                + "sum(i.netValue)) "
                + " from BillItem i "
                + " where i.bill.createdAt between :frm and :to  ";
        if (department != null) {
            m.put("dep", department);
            sql += " and i.bill.department=:dep";
        }
        if (item != null) {
            m.put("item", department);
            sql += " and i.item=:item ";
        }

        if (billTypes != null) {
            List<BillType> bts = Arrays.asList(billTypes);
            m.put("bts", bts);
            sql += " and i.bill.billType in :bts ";
        }

        if (referenceBillTypes != null) {
            List<BillType> rbts = Arrays.asList(referenceBillTypes);
            m.put("rbts", rbts);
            sql += " and i.bill.referenceBill.billType in :rbts ";
        }

        if (ins != null) {
            m.put("ins", ins);
            sql += " and (i.bill.institution=:ins or i.bill.department.institution=:ins) ";
        }
        sql += " group by i.item";
        sql += " order by i.item.name";
//        System.out.println("m = " + m);
//        System.out.println("sql = " + sql);
        List<ItemQuantityAndValues> lst = getBillItemFacade().findItemQuantityAndValuesList(sql, m, TemporalType.DATE);
        return lst;

    }

    public void averageByDatePercentage() {
        Calendar frm = Calendar.getInstance();
        frm.setTime(fromDate);
        Calendar to = Calendar.getInstance();
        to.setTime(toDate);

        long lValue = to.getTimeInMillis() - frm.getTimeInMillis();
        double dayCount = 0;
        if (lValue != 0) {
            dayCount = lValue / (1000 * 60 * 60 * 24);
        }

        //System.err.println("Day Count " + dayCount);
        createStockAverageByPer(dayCount);

    }

    public void createStockAverageByPer(double dayCount) {

        stockAverages = new ArrayList<>();
        List<Item> items = getItemController().getDealorItem();
        List<Institution> insList = getCompany();
        for (Item i : items) {
            double itemStockTotal = 0;
            double itemAverageTotal = 0;
            StockAverage stockAverage = new StockAverage();
            stockAverage.setItem(i);
            stockAverage.setInstitutionStocks(new ArrayList<InstitutionStock>());

            for (Institution ins : insList) {
                double insStockTotal = 0;
                double insAverageTotal = 0;
                double insStock = 0.0;
                InstitutionStock newTable = new InstitutionStock();
                newTable.setInstitution(ins);
                newTable.setDepatmentStocks(new ArrayList<DepartmentStock>());
                List<Object[]> objs = calDepartmentStock(ins, i);
                double calPerStock = 0.0;
                for (Object[] obj : objs) {
//                    //System.err.println("Inside ");
                    DepartmentStock r = new DepartmentStock();
                    r.setDepartment((Department) obj[0]);
                    r.setStock((Double) obj[1]);

                    double qty = calDepartmentSaleQtyByPer(r.getDepartment(), i);
                    qty = 0 - qty;
                    if (qty != 0 && dayCount != 0) {
                        double avg = qty / dayCount;
                        calPerStock = (avg * persentage) / 100;
                        insStock = r.getStock();
                        r.setAverage(avg);
                    }

//                    ////System.out.println("calPerStock = " + calPerStock);
//                    ////System.out.println("insStockTotal = " + insStockTotal);
//                    ////System.out.println("insAverageTotal = " + insAverageTotal);
                    if ((insStock < calPerStock) && r.getStock() != 0) {
                        ////System.out.println("*insStock = " + insStock);
                        ////System.out.println("*calPerStock = " + calPerStock);
                        insStockTotal += r.getStock();
                        insAverageTotal += r.getAverage();
                        newTable.getDepatmentStocks().add(r);
                    }

                }

//                ////System.out.println("calPerStock = " + calPerStock);
//                ////System.out.println("insStockTotal = " + insStockTotal);
//                ////System.out.println("insAverageTotal = " + insAverageTotal);
                newTable.setInstitutionTotal(insStockTotal);
                newTable.setInstitutionAverage(insAverageTotal);

                if ((insStockTotal != 0 || insAverageTotal != 0) && insStock < calPerStock) {
                    stockAverage.getInstitutionStocks().add(newTable);
                    itemStockTotal += insStockTotal;
                    itemAverageTotal += insAverageTotal;
                }
            }

            if (itemAverageTotal != 0 || itemStockTotal != 0) {
                stockAverage.setItemAverageTotal(itemAverageTotal);
                stockAverage.setItemStockTotal(itemStockTotal);
                stockAverages.add(stockAverage);
            }

        }

    }

    public void averageByMonthByPercentage() {
        Calendar frm = Calendar.getInstance();
        frm.setTime(fromDate);
        Calendar to = Calendar.getInstance();
        to.setTime(toDate);

        long lValue = to.getTimeInMillis() - frm.getTimeInMillis();
        double monthCount = 0;
        if (lValue != 0) {
            monthCount = lValue / (1000 * 60 * 60 * 24 * 30);
        }

        //System.err.println("Month Count " + monthCount);
        createStockAverageByPer(Math.abs(monthCount));

    }

    public double calDepartmentSaleQtyByPer(Department department, Item itm) {

        if (itm instanceof Ampp) {
            itm = ((Ampp) pharmacyItem).getAmp();
        }

        String sql;
        Map m = new HashMap();
        m.put("itm", itm);
        m.put("dep", department);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyPre);
        m.put("refType", BillType.PharmacySale);
        sql = "select sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i where i.bill.department=:dep"
                + " and i.bill.referenceBill.billType=:refType "
                + " and i.item=:itm and i.bill.billType=:btp and "
                + " i.createdAt between :frm and :to  ";

        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void makeAllPharmaceuticalItemsToAllowDiscounts() {
        List<PharmaceuticalItem> pis = getPiFacade().findAll();
        for (PharmaceuticalItem pi : pis) {
            pi.setDiscountAllowed(true);
            getPiFacade().edit(pi);
        }
        JsfUtil.addSuccessMessage("All Pharmaceutical Items were made to allow discounts");

    }

    private double grantStock;

    public double getGrantStock() {
        return grantStock;

    }

    public double getTransferIssueValueByInstitution(Institution toIns, Item i) {
        //   List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("toIns", toIns);
        m.put("frmIns", getSessionController().getInstitution());
        m.put("i", i);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyTransferIssue);
        //   m.put("refType", BillType.PharmacySale);
        sql = "select sum(i.pharmaceuticalBillItem.stock.itemBatch.purcahseRate*i.pharmaceuticalBillItem.qty) "
                + "from BillItem i where i.bill.toInstitution=:toIns and i.bill.fromInstitution=:frmIns "
                + " and i.item=:i and i.bill.billType=:btp and i.createdAt between :frm and :to ";
        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double getTransferIssueValueByDepartmet(Department toDep, Item i) {
        //   List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("tDep", toDep);
        m.put("fDep", getSessionController().getDepartment());
        m.put("i", i);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyTransferIssue);
        //   m.put("refType", BillType.PharmacySale);
        sql = "select sum(i.pharmaceuticalBillItem.stock.itemBatch.purcahseRate*i.pharmaceuticalBillItem.qty) "
                + " from BillItem i where i.bill.toDepartment=:tDep and i.bill.fromDepartment=:fDep "
                + " and i.item=:i and i.bill.billType=:btp and i.createdAt between :frm and :to ";
        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double getTransferIssueQtyByDepartmet(Department tDep, Item i) {
        //   List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("tDep", tDep);
        m.put("fDep", getSessionController().getDepartment());
        m.put("i", i);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyTransferIssue);
        //   m.put("refType", BillType.PharmacySale);
        sql = "select sum(i.pharmaceuticalBillItem.qty) from BillItem i where i.bill.toDepartment=:tDep"
                + " and  i.bill.fromDepartment=:fDep"
                + " and i.item=:i and i.bill.billType=:btp and i.createdAt between :frm and :to ";
        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double getTransferIssueQtyByInstitution(Institution toIns, Item i) {
        //   List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("tIns", toIns);
        m.put("fIns", getSessionController().getInstitution());
        m.put("i", i);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyTransferIssue);
        //   m.put("refType", BillType.PharmacySale);
        sql = "select sum(i.pharmaceuticalBillItem.qty) from BillItem i where "
                + " i.bill.toInstitution=:tIns and i.bill.fromInstitution=:fIns "
                + " and i.item=:i and i.bill.billType=:btp and i.createdAt between :frm and :to ";
        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private double grantSaleQty;
    private double grantBhtIssueQty;
    private double grantSaleValue;
    private double grantBhtValue;

    private double grantWholeSaleQty;
    private double grantWholeSaleValue;

    private double grantTransferIssueQty;
    private double grantIssueQty;
    private double grantTransferIssueValue;
    private double grantIssueValue;

    @EJB
    private InstitutionFacade institutionFacade;

    private List<Institution> getCompany() {
        String sql;
        HashMap hm = new HashMap();
        hm.put("type", InstitutionType.Company);
        sql = "select c from Institution c where c.retired=false and c.institutionType=:type order by c.name";

        return getInstitutionFacade().findBySQL(sql, hm);
    }

    private List<InstitutionStock> institutionStocks;

    public List<Object[]> calDepartmentStock(Institution institution) {
        //   //System.err.println("Cal Department Stock");
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
        m.put("ins", institution);
        m.put("i", item);
        sql = "select i.department,sum(i.stock) from Stock i where "
                + " i.department.institution=:ins and i.itemBatch.item=:i"
                + " group by i.department"
                + " having sum(i.stock) > 0 ";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Object[]> calDepartmentStock(Institution institution, Item itm) {
        //   //System.err.println("Cal Department Stock");

        if (itm instanceof Ampp) {
            itm = ((Ampp) itm).getAmp();
        }

        String sql;
        Map m = new HashMap();
        m.put("ins", institution);
        m.put("i", itm);
        sql = "select i.department,sum(i.stock) from Stock i where "
                + " i.department.institution=:ins and i.itemBatch.item=:i"
                + " group by i.department"
                + " having sum(i.stock) > 0 ";

        return getBillItemFacade().findAggregates(sql, m);

    }

    public List<Object[]> calDepartmentTransferIssue(Institution institution) {
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
        m.put("curr", getSessionController().getDepartment());
        m.put("i", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyTransferIssue);
        //   m.put("refType", BillType.PharmacySale);
        sql = "select i.bill.toDepartment,"
                + " sum(i.pharmaceuticalBillItem.stock.itemBatch.purcahseRate*i.pharmaceuticalBillItem.qty),"
                + " sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i "
                + " where i.bill.toDepartment.institution=:ins "
                + " and i.bill.department=:curr "
                + " and i.item=:i"
                + " and i.bill.billType=:btp "
                + " and i.createdAt between :frm and :to"
                + " group by i.bill.toDepartment";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Object[]> calDepartmentIssue(Institution institution) {
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
//        m.put("curr", getSessionController().getDepartment());
        m.put("i", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyIssue);
        //   m.put("refType", BillType.PharmacySale);
//        sql = "select i.bill.toDepartment,"
//                + " sum(i.pharmaceuticalBillItem.stock.itemBatch.purcahseRate*i.pharmaceuticalBillItem.qty),"
//                + " sum(i.pharmaceuticalBillItem.qty) "
//                + " from BillItem i "
//                + " where i.bill.toDepartment.institution=:ins "
//                + " and i.bill.department=:curr "
//                + " and i.item=:i"
//                + " and i.bill.billType=:btp "
//                + " and i.createdAt between :frm and :to"
//                + " group by i.bill.toDepartment";

        sql = "select i.billItem.bill.toDepartment,"
                + " sum(i.stock.itemBatch.purcahseRate*i.qty),"
                + " sum(i.qty) "
                + " from PharmaceuticalBillItem i "
                + " where i.billItem.bill.toDepartment.institution=:ins "
                //                + " and i.billItem.bill.department=:curr "
                + " and i.billItem.item=:i"
                + " and i.billItem.bill.billType=:btp "
                + " and i.billItem.createdAt between :frm and :to"
                + " group by i.billItem.bill.toDepartment";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Object[]> calDepartmentTransferReceive(Institution institution) {
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
        m.put("dep", getSessionController().getDepartment());
        m.put("i", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyTransferReceive);
        //   m.put("refType", BillType.PharmacySale);
        sql = "select i.bill.fromDepartment,sum(i.pharmaceuticalBillItem.stock.itemBatch.purcahseRate*i.pharmaceuticalBillItem.qty),"
                + " sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i where i.bill.fromDepartment.institution=:ins and i.bill.department=:dep "
                + " and i.item=:i and i.bill.billType=:btp and i.createdAt between :frm and :to group by i.bill.fromDepartment";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Object[]> calDepartmentBhtIssue(Institution institution, BillType billType) {
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
        m.put("itm", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", billType);
        sql = "select i.bill.department,"
                + " sum(i.netValue),"
                + " sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i "
                + " where i.bill.department.institution=:ins"
                + " and i.item=:itm "
                + " and i.bill.billType=:btp "
                + " and i.createdAt between :frm and :to  "
                + " group by i.bill.department";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Object[]> calDepartmentSale(Institution institution) {
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
        m.put("itm", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyPre);
        m.put("refType", BillType.PharmacySale);
        sql = "select i.bill.department,"
                + " sum(i.netValue),"
                + " sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i "
                + " where i.bill.department.institution=:ins"
                + " and i.bill.referenceBill.billType=:refType "
                + " and i.item=:itm "
                + " and i.bill.billType=:btp "
                + " and i.createdAt between :frm and :to  "
                + " group by i.bill.department";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public List<Object[]> calDepartmentWholeSale(Institution institution) {
        Item item;

        if (pharmacyItem instanceof Ampp) {
            item = ((Ampp) pharmacyItem).getAmp();
        } else {
            item = pharmacyItem;
        }

        String sql;
        Map m = new HashMap();
        m.put("itm", item);
        m.put("ins", institution);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyWholesalePre);
        m.put("refType", BillType.PharmacyWholeSale);
        sql = "select i.bill.department,"
                + " sum(i.netValue),"
                + " sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i "
                + " where i.bill.department.institution=:ins"
                + " and i.bill.referenceBill.billType=:refType "
                + " and i.item=:itm "
                + " and i.bill.billType=:btp "
                + " and i.createdAt between :frm and :to  "
                + " group by i.bill.department";

        return getBillItemFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public double calDepartmentSaleQty(Department department, Item itm) {

        if (itm instanceof Ampp) {
            itm = ((Ampp) pharmacyItem).getAmp();
        }

        String sql;
        Map m = new HashMap();
        m.put("itm", itm);
        m.put("dep", department);
        m.put("frm", getFromDate());
        m.put("to", getToDate());
        m.put("btp", BillType.PharmacyPre);
        m.put("refType", BillType.PharmacySale);
        sql = "select sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i where i.bill.department=:dep"
                + " and i.bill.referenceBill.billType=:refType "
                + " and i.item=:itm and i.bill.billType=:btp and "
                + " i.createdAt between :frm and :to  ";

        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private Institution institution;
    private List<StockAverage> stockAverages;

    @Inject
    private ItemController itemController;

    public void averageByDate() {
        Calendar frm = Calendar.getInstance();
        frm.setTime(fromDate);
        Calendar to = Calendar.getInstance();
        to.setTime(toDate);

        long lValue = to.getTimeInMillis() - frm.getTimeInMillis();
        double dayCount = 0;
        if (lValue != 0) {
            dayCount = lValue / (1000 * 60 * 60 * 24);
        }

        System.err.println("Day Count " + dayCount);
        createStockAverage(dayCount);

    }

    public double getGrantWholeSaleQty() {
        return grantWholeSaleQty;
    }

    public void setGrantWholeSaleQty(double grantWholeSaleQty) {
        this.grantWholeSaleQty = grantWholeSaleQty;
    }

    public double getGrantWholeSaleValue() {
        return grantWholeSaleValue;
    }

    public void setGrantWholeSaleValue(double grantWholeSaleValue) {
        this.grantWholeSaleValue = grantWholeSaleValue;
    }

    public void averageByMonth() {
        Calendar frm = Calendar.getInstance();
        frm.setTime(fromDate);
        Calendar to = Calendar.getInstance();
        to.setTime(toDate);

        long lValue = to.getTimeInMillis() - frm.getTimeInMillis();
        double monthCount = 0;
        if (lValue != 0) {
            monthCount = lValue / (1000 * 60 * 60 * 24 * 30);
        }

        System.err.println("Month Count " + monthCount);
        createStockAverage(Math.abs(monthCount));

    }

    public void createStockAverage(double dayCount) {

        stockAverages = new ArrayList<>();
        List<Item> items = getItemController().getDealorItem();
        List<Institution> insList = getCompany();
        for (Item i : items) {
            double itemStockTotal = 0;
            double itemAverageTotal = 0;
            StockAverage stockAverage = new StockAverage();
            stockAverage.setItem(i);
            stockAverage.setInstitutionStocks(new ArrayList<InstitutionStock>());

            for (Institution ins : insList) {
                double insStockTotal = 0;
                double insAverageTotal = 0;
                InstitutionStock newTable = new InstitutionStock();
                newTable.setInstitution(ins);
                newTable.setDepatmentStocks(new ArrayList<DepartmentStock>());
                List<Object[]> objs = calDepartmentStock(ins, i);

                for (Object[] obj : objs) {
//                    System.err.println("Inside ");
                    DepartmentStock r = new DepartmentStock();
                    r.setDepartment((Department) obj[0]);
                    r.setStock((Double) obj[1]);

                    double qty = calDepartmentSaleQty(r.getDepartment(), i);
                    qty = 0 - qty;
                    if (qty != 0 && dayCount != 0) {
                        double avg = qty / dayCount;
                        r.setAverage(avg);
                    }

                    insStockTotal += r.getStock();
                    insAverageTotal += r.getAverage();
                    newTable.getDepatmentStocks().add(r);

                }

                newTable.setInstitutionTotal(insStockTotal);
                newTable.setInstitutionAverage(insAverageTotal);

                if (insStockTotal != 0 || insAverageTotal != 0) {
                    stockAverage.getInstitutionStocks().add(newTable);
                    itemStockTotal += insStockTotal;
                    itemAverageTotal += insAverageTotal;
                }
            }

            stockAverage.setItemAverageTotal(itemAverageTotal);
            stockAverage.setItemStockTotal(itemStockTotal);
            stockAverages.add(stockAverage);
        }

    }

    public void createInstitutionStock() {
        //   //System.err.println("Institution Stock");
        List<Institution> insList = getCompany();

        institutionStocks = new ArrayList<>();
        grantStock = 0;

        for (Institution ins : insList) {
            InstitutionStock newTable = new InstitutionStock();
            List<DepartmentStock> list = new ArrayList<>();
            double totalStock = 0;
            List<Object[]> objs = calDepartmentStock(ins);

            for (Object[] obj : objs) {
                DepartmentStock r = new DepartmentStock();
                r.setDepartment((Department) obj[0]);
                r.setStock((Double) obj[1]);
                list.add(r);

                //Total Institution Stock
                totalStock += r.getStock();
                grantStock += r.getStock();

            }

            if (totalStock != 0) {
                newTable.setDepatmentStocks(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionTotal(totalStock);

                institutionStocks.add(newTable);
            }
        }

    }

    public void createInstitutionSale() {
        List<Institution> insList = getCompany();

        institutionSales = new ArrayList<>();
        grantSaleQty = 0;
        grantSaleValue = 0;

        for (Institution ins : insList) {
            InstitutionSale newTable = new InstitutionSale();
            List<DepartmentSale> list = new ArrayList<>();
            double totalValue = 0;
            double totalQty = 0;
            List<Object[]> objs = calDepartmentSale(ins);

            for (Object[] obj : objs) {
                DepartmentSale r = new DepartmentSale();
                r.setDepartment((Department) obj[0]);
                r.setSaleValue((Double) obj[1]);
                r.setSaleQty((Double) obj[2]);
                list.add(r);
                //Total Institution Stock
                totalValue += r.getSaleValue();
                totalQty += r.getSaleQty();
                grantSaleValue += r.getSaleValue();
                grantSaleQty += r.getSaleQty();

            }

            if (totalQty != 0 || totalValue != 0) {
                newTable.setDepartmentSales(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionQty(totalQty);
                newTable.setInstitutionValue(totalValue);

                institutionSales.add(newTable);

            }
        }

    }

    public void createInstitutionWholeSale() {
        List<Institution> insList = getCompany();

        institutionWholeSales = new ArrayList<>();
        grantWholeSaleQty = 0;
        grantWholeSaleValue = 0;

        for (Institution ins : insList) {
            InstitutionSale newTable = new InstitutionSale();
            List<DepartmentSale> list = new ArrayList<>();
            double totalValue = 0;
            double totalQty = 0;
            List<Object[]> objs = calDepartmentWholeSale(ins);

            for (Object[] obj : objs) {
                DepartmentSale r = new DepartmentSale();
                r.setDepartment((Department) obj[0]);
                r.setSaleValue((Double) obj[1]);
                r.setSaleQty((Double) obj[2]);
                list.add(r);
                //Total Institution Stock
                totalValue += r.getSaleValue();
                totalQty += r.getSaleQty();
                grantWholeSaleValue += r.getSaleValue();
                grantWholeSaleQty += r.getSaleQty();

            }

            if (totalQty != 0 || totalValue != 0) {
                newTable.setDepartmentSales(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionQty(totalQty);
                newTable.setInstitutionValue(totalValue);

                institutionWholeSales.add(newTable);

            }
        }

    }

    public double getGrantBhtIssueQty() {
        return grantBhtIssueQty;
    }

    public void setGrantBhtIssueQty(double grantBhtIssueQty) {
        this.grantBhtIssueQty = grantBhtIssueQty;
    }

    public double getGrantBhtValue() {
        return grantBhtValue;
    }

    public void setGrantBhtValue(double grantBhtValue) {
        this.grantBhtValue = grantBhtValue;
    }

    public List<InstitutionSale> getInstitutionBhtIssue() {
        return institutionBhtIssue;
    }

    public void setInstitutionBhtIssue(List<InstitutionSale> institutionBhtIssue) {
        this.institutionBhtIssue = institutionBhtIssue;
    }

    public void createInstitutionBhtIssue() {
        List<Institution> insList = getCompany();

        institutionBhtIssue = new ArrayList<>();
        grantBhtIssueQty = 0;
        grantBhtValue = 0;

        for (Institution ins : insList) {
            InstitutionSale newTable = new InstitutionSale();
            List<DepartmentSale> list = new ArrayList<>();
            double totalValue = 0;
            double totalQty = 0;
            List<Object[]> objs = calDepartmentBhtIssue(ins, BillType.PharmacyBhtPre);

            for (Object[] obj : objs) {
                DepartmentSale r = new DepartmentSale();
                r.setDepartment((Department) obj[0]);
                r.setSaleValue((Double) obj[1]);
                r.setSaleQty((Double) obj[2]);
                list.add(r);
                //Total Institution Stock
                totalValue += r.getSaleValue();
                totalQty += r.getSaleQty();
                grantBhtValue += r.getSaleValue();
                grantBhtIssueQty += r.getSaleQty();

            }

            if (totalQty != 0 || totalValue != 0) {
                newTable.setDepartmentSales(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionQty(totalQty);
                newTable.setInstitutionValue(totalValue);

                institutionBhtIssue.add(newTable);

            }
        }

    }

    public List<InstitutionStock> getInstitutionStock() {
        return institutionStocks;
    }

    private List<InstitutionSale> institutionSales;
    private List<InstitutionSale> institutionWholeSales;
    private List<InstitutionSale> institutionBhtIssue;

    private List<InstitutionSale> institutionTransferIssue;
    private List<InstitutionSale> institutionIssue;

    public List<InstitutionSale> getInstitutionIssue() {
        return institutionIssue;
    }

    public void setInstitutionIssue(List<InstitutionSale> institutionIssue) {
        this.institutionIssue = institutionIssue;
    }

    public List<InstitutionSale> getInstitutionWholeSales() {
        return institutionWholeSales;
    }

    public void setInstitutionWholeSales(List<InstitutionSale> institutionWholeSales) {
        this.institutionWholeSales = institutionWholeSales;
    }

    private List<InstitutionSale> institutionTransferReceive;
    private double grantTransferReceiveQty = 0;
    private double grantTransferReceiveValue = 0;

    public void createInstitutionTransferIssue() {
        List<Institution> insList = getCompany();

        institutionTransferIssue = new ArrayList<>();
        grantTransferIssueQty = 0;
        grantTransferIssueValue = 0;

        for (Institution ins : insList) {
            InstitutionSale newTable = new InstitutionSale();
            List<DepartmentSale> list = new ArrayList<>();
            double totalValue = 0;
            double totalQty = 0;
            List<Object[]> objs = calDepartmentTransferIssue(ins);

            for (Object[] obj : objs) {
                DepartmentSale r = new DepartmentSale();
                r.setDepartment((Department) obj[0]);
                r.setSaleValue((Double) obj[1]);
                r.setSaleQty((Double) obj[2]);
                list.add(r);
                //Total Institution Stock
                totalValue += r.getSaleValue();
                totalQty += r.getSaleQty();
                grantTransferIssueValue += r.getSaleValue();
                grantTransferIssueQty += r.getSaleQty();

            }

            if (totalQty != 0 || totalValue != 0) {
                newTable.setDepartmentSales(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionQty(totalQty);
                newTable.setInstitutionValue(totalValue);

                institutionTransferIssue.add(newTable);

            }
        }

    }

    public double getGrantIssueQty() {
        return grantIssueQty;
    }

    public void setGrantIssueQty(double grantIssueQty) {
        this.grantIssueQty = grantIssueQty;
    }

    public double getGrantIssueValue() {
        return grantIssueValue;
    }

    public void setGrantIssueValue(double grantIssueValue) {
        this.grantIssueValue = grantIssueValue;
    }

    public void createInstitutionIssue() {
        List<Institution> insList = getCompany();

        institutionIssue = new ArrayList<>();
        grantIssueQty = 0;
        grantIssueValue = 0;

        for (Institution ins : insList) {
            InstitutionSale newTable = new InstitutionSale();
            List<DepartmentSale> list = new ArrayList<>();
            double totalValue = 0;
            double totalQty = 0;
            List<Object[]> objs = calDepartmentIssue(ins);

            for (Object[] obj : objs) {
                DepartmentSale r = new DepartmentSale();
                r.setDepartment((Department) obj[0]);
                r.setSaleValue((Double) obj[1]);
                r.setSaleQty((Double) obj[2]);
                list.add(r);
                //Total Institution Stock
                totalValue += r.getSaleValue();
                totalQty += r.getSaleQty();
                grantIssueValue += r.getSaleValue();
                grantIssueQty += r.getSaleQty();

            }

            if (totalQty != 0 || totalValue != 0) {
                newTable.setDepartmentSales(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionQty(totalQty);
                newTable.setInstitutionValue(totalValue);

                institutionIssue.add(newTable);

            }
        }

    }

    public void createInstitutionTransferReceive() {
        List<Institution> insList = getCompany();

        institutionTransferReceive = new ArrayList<>();
        grantTransferReceiveQty = 0;
        grantTransferReceiveValue = 0;

        for (Institution ins : insList) {
            InstitutionSale newTable = new InstitutionSale();
            List<DepartmentSale> list = new ArrayList<>();
            double totalValue = 0;
            double totalQty = 0;
            List<Object[]> objs = calDepartmentTransferReceive(ins);

            for (Object[] obj : objs) {
                DepartmentSale r = new DepartmentSale();
                r.setDepartment((Department) obj[0]);
                r.setSaleValue((Double) obj[1]);
                r.setSaleQty((Double) obj[2]);
                list.add(r);
                //Total Institution Stock
                totalValue += r.getSaleValue();
                totalQty += r.getSaleQty();
                grantTransferReceiveValue += r.getSaleValue();
                grantTransferReceiveQty += r.getSaleQty();

            }

            if (totalQty != 0 || totalValue != 0) {
                newTable.setDepartmentSales(list);
                newTable.setInstitution(ins);
                newTable.setInstitutionQty(totalQty);
                newTable.setInstitutionValue(totalValue);

                institutionTransferReceive.add(newTable);

            }
        }

    }

    public List<BillItem> getGrns() {
        return grns;
    }

    public void fillDetails() {
        createInstitutionSale();
        createInstitutionBhtIssue();
        createInstitutionStock();
        createInstitutionTransferIssue();
        createInstitutionIssue();
        createInstitutionTransferReceive();
        createGrnTable();
        createPoTable();
        createDirectPurchaseTable();
        createInstitutionIssue();
    }

    public void createTable() {
        createGrnTable();
        createPoTable();
        createDirectPurchaseTable();
        createInstitutionIssue();

    }

    public void createGrnTable() {

        // //System.err.println("Getting GRNS : ");
        String sql = "Select b From BillItem b where type(b.bill)=:class and b.bill.creater is not null "
                + " and b.bill.cancelled=false and b.retired=false and b.item=:i "
                + " and b.bill.billType=:btp and b.createdAt between :frm and :to order by b.id desc ";
        HashMap hm = new HashMap();
        hm.put("i", pharmacyItem);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("class", BilledBill.class);
        hm.put("btp", BillType.PharmacyGrnBill);

        grns = getBillItemFacade().findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

//    public void createPhrmacyIssueTable() {
//
//        // //System.err.println("Getting GRNS : ");
//        String sql = "Select b From BillItem b where type(b.bill)=:class and b.bill.creater is not null "
//                + " and b.bill.cancelled=false and b.retired=false and b.item=:i "
//                + " and b.bill.billType=:btp and b.createdAt between :frm and :to order by b.id desc ";
//        HashMap hm = new HashMap();
//        hm.put("i", pharmacyItem);
//        hm.put("frm", getFromDate());
//        hm.put("to", getToDate());
//        hm.put("class", BilledBill.class);
//        hm.put("btp", BillType.PharmacyIssue);
//
//        institutionIssue = getBillItemFacade().findBySQL(sql, hm, TemporalType.TIMESTAMP);
//
//    }
    public void createDirectPurchaseTable() {

        // //System.err.println("Getting GRNS : ");
        String sql = "Select b From BillItem b where type(b.bill)=:class and b.bill.creater is not null "
                + " and b.bill.cancelled=false and b.retired=false and b.item=:i "
                + " and b.bill.billType=:btp and b.createdAt between :frm and :to order by b.id desc ";
        HashMap hm = new HashMap();
        hm.put("i", pharmacyItem);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("btp", BillType.PharmacyPurchaseBill);
        hm.put("class", BilledBill.class);
        directPurchase = getBillItemFacade().findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    public List<BillItem> getPos() {
        return pos;
    }

    public void createPoTable() {

        // //System.err.println("Getting POS : ");
        String sql = "Select b From BillItem b where type(b.bill)=:class and b.bill.creater is not null "
                + " and b.bill.cancelled=false and b.retired=false and b.item=:i "
                + " and b.bill.billType=:btp and b.createdAt between :frm and :to order by b.id desc";
        HashMap hm = new HashMap();
        hm.put("i", pharmacyItem);
        hm.put("btp", BillType.PharmacyOrderApprove);
        hm.put("frm", getFromDate());
        hm.put("to", getToDate());
        hm.put("class", BilledBill.class);
        pos = getBillItemFacade().findBySQL(sql, hm, TemporalType.TIMESTAMP);

        for (BillItem t : pos) {
            //   t.setPharmaceuticalBillItem(getPoQty(t));
            t.setTotalGrnQty(getGrnQty(t));
        }

    }

//    private PharmaceuticalBillItem getPoQty(BillItem b) {
//        String sql = "Select b From PharmaceuticalBillItem b where b.billItem=:bt";
//
//        HashMap hm = new HashMap();
//        hm.put("bt", b);
//
//        return getPharmaceuticalBillItemFacade().findFirstBySQL(sql, hm);
//    }
    private double getGrnQty(BillItem b) {
        String sql = "Select sum(b.pharmaceuticalBillItem.qty) From BillItem b where b.retired=false and b.creater is not null"
                + " and b.bill.cancelled=false and b.bill.billType=:btp and "
                + " b.referanceBillItem=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyGrnBill);
        double value = getBillFacade().findDoubleByJpql(sql, hm);

//        if (pharmacyItem instanceof Ampp) {
//            value = value / pharmacyItem.getDblValue();
//        }
        return value;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public Item getPharmacyItem() {

        return pharmacyItem;
    }

    public List<Ampp> getAmpps() {
        if (pharmacyItem == null) {
            return new ArrayList<>();
        }
        String sql;
        Map m = new HashMap();
        sql = "select p from Ampp p where p.retired=false and p.amp=:a order by p.dblValue";
        m.put("a", pharmacyItem);
        List<Ampp> list = getAmppFacade().findBySQL(sql, m);

        if (list == null) {
            return new ArrayList<>();
        } else {
            return list;
        }
    }

    public void setPharmacyItem(Item pharmacyItem) {

        makeNull();

        this.pharmacyItem = pharmacyItem;
        createInstitutionSale();
        createInstitutionWholeSale();
        createInstitutionBhtIssue();
        createInstitutionStock();
        createInstitutionTransferIssue();
        createInstitutionIssue();
        createInstitutionTransferReceive();
    }

    public double findPharmacyMovement(Department department, Item itm, BillType[] bts, Date fd, Date td) {
        try {
            if (itm instanceof Ampp) {
                itm = ((Ampp) pharmacyItem).getAmp();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String sql;
        Map m = new HashMap();
        m.put("itm", itm);
        m.put("dep", department);
        m.put("frm", fd);
        m.put("to", td);
        List<BillType> bts1 = Arrays.asList(bts);
        m.put("bts", bts1);
        sql = "select sum(i.pharmaceuticalBillItem.qty) "
                + " from BillItem i "
                + " where i.bill.department=:dep"
                + " and i.item=:itm "
                + " and i.bill.billType in :bts "
                + " and i.createdAt between :frm and :to  ";
        return getBillItemFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public Date findFirstPharmacyMovementDate(Department department, Item itm, BillType[] bts, Date fd, Date td) {
        try {
            if (itm instanceof Ampp) {
                itm = ((Ampp) pharmacyItem).getAmp();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String sql;
        Map m = new HashMap();
        m.put("itm", itm);
        m.put("dep", department);
        m.put("frm", fd);
        m.put("to", td);
        List<BillType> bts1 = Arrays.asList(bts);
        m.put("bts", bts1);
        sql = "select i "
                + " from BillItem i "
                + " where i.bill.department=:dep"
                + " and i.item=:itm "
                + " and i.bill.billType in :bts "
                + " and i.bill.createdAt between :frm and :to  "
                + " order by i.id";
        BillItem d = getBillItemFacade().findFirstBySQL(sql, m, TemporalType.TIMESTAMP);
        if (d == null) {
            return fd;
        } else if (d.getBill() != null && d.getBill().getCreatedAt() != null) {
            return d.getBill().getCreatedAt();
        } else if (d.getCreatedAt() != null) {
            return d.getCreatedAt();
        } else {
            return fd;
        }
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Date getFromDate() {
        if (fromDate == null) {
//            Date date = ;
            fromDate = getCommonFunctions().getAddedDate(commonFunctions.getStartOfDay(new Date()), -30);
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        makeNull();
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = commonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        makeNull();
        this.toDate = toDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }

    public List<DepartmentSale> getDepartmentSale() {
        return departmentSale;
    }

    public void setDepartmentSale(List<DepartmentSale> departmentSale) {
        this.departmentSale = departmentSale;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public List<BillItem> getDirectPurchase() {
        return directPurchase;
    }

    public void setDirectPurchase(List<BillItem> directPurchase) {
        this.directPurchase = directPurchase;
    }

    public List<InstitutionStock> getInstitutionStocks() {
        return institutionStocks;
    }

    public void setInstitutionStocks(List<InstitutionStock> institutionStocks) {
        this.institutionStocks = institutionStocks;
    }

    public List<InstitutionSale> getInstitutionSales() {
        return institutionSales;
    }

    public void setInstitutionSales(List<InstitutionSale> institutionSales) {
        this.institutionSales = institutionSales;
    }

    public List<InstitutionSale> getInstitutionTransferIssue() {
        return institutionTransferIssue;
    }

    public void setInstitutionTransferIssue(List<InstitutionSale> institutionTransferIssue) {
        this.institutionTransferIssue = institutionTransferIssue;
    }

    public void setGrns(List<BillItem> grns) {
        this.grns = grns;
    }

    public void setPos(List<BillItem> pos) {
        this.pos = pos;
    }

    public void setGrantStock(double grantStock) {
        this.grantStock = grantStock;
    }

    public double getGrantSaleQty() {
        return grantSaleQty;
    }

    public void setGrantSaleQty(double grantSaleQty) {
        this.grantSaleQty = grantSaleQty;
    }

    public double getGrantSaleValue() {
        return grantSaleValue;
    }

    public void setGrantSaleValue(double grantSaleValue) {
        this.grantSaleValue = grantSaleValue;
    }

    public double getGrantTransferIssueQty() {
        return grantTransferIssueQty;
    }

    public void setGrantTransferIssueQty(double grantTransferIssueQty) {
        this.grantTransferIssueQty = grantTransferIssueQty;
    }

    public double getGrantTransferIssueValue() {
        return grantTransferIssueValue;
    }

    public void setGrantTransferIssueValue(double grantTransferIssueValue) {
        this.grantTransferIssueValue = grantTransferIssueValue;
    }

    public List<InstitutionSale> getInstitutionTransferReceive() {
        return institutionTransferReceive;
    }

    public void setInstitutionTransferReceive(List<InstitutionSale> institutionTransferReceive) {
        this.institutionTransferReceive = institutionTransferReceive;
    }

    public double getGrantTransferReceiveQty() {
        return grantTransferReceiveQty;
    }

    public void setGrantTransferReceiveQty(double grantTransferReceiveQty) {
        this.grantTransferReceiveQty = grantTransferReceiveQty;
    }

    public double getGrantTransferReceiveValue() {
        return grantTransferReceiveValue;
    }

    public void setGrantTransferReceiveValue(double grantTransferReceiveValue) {
        this.grantTransferReceiveValue = grantTransferReceiveValue;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<StockAverage> getStockAverages() {
        return stockAverages;
    }

    public void setStockAverages(List<StockAverage> stockAverages) {
        this.stockAverages = stockAverages;
    }

    public ItemController getItemController() {
        return itemController;
    }

    public void setItemController(ItemController itemController) {
        this.itemController = itemController;
    }

    public PharmaceuticalItemFacade getPiFacade() {
        return piFacade;
    }

    public void setPiFacade(PharmaceuticalItemFacade piFacade) {
        this.piFacade = piFacade;
    }

    public AmppFacade getAmppFacade() {
        return AmppFacade;
    }

    public void setAmppFacade(AmppFacade AmppFacade) {
        this.AmppFacade = AmppFacade;
    }

    public double getPersentage() {
        return persentage;
    }

    public void setPersentage(double persentage) {
        this.persentage = persentage;
    }

    public Department getDepartment() {
        if (department == null) {
            department = getSessionController().getDepartment();
        }
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<ItemTransactionSummeryRow> getItemTransactionSummeryRows() {
        return itemTransactionSummeryRows;
    }

    public void setItemTransactionSummeryRows(List<ItemTransactionSummeryRow> itemTransactionSummeryRows) {
        this.itemTransactionSummeryRows = itemTransactionSummeryRows;
    }

}
