/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.ItemBatchQty;
import com.divudi.data.StockQty;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.IssueRateMargins;
import com.divudi.entity.Item;
import com.divudi.entity.PreBill;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.entity.pharmacy.ItemBatch;
import com.divudi.entity.pharmacy.MeasurementUnit;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.entity.pharmacy.PharmaceuticalItemType;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.entity.pharmacy.StockHistory;
import com.divudi.entity.pharmacy.StoreItemCategory;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vmpp;
import com.divudi.entity.pharmacy.Vtm;
import com.divudi.entity.pharmacy.VirtualProductIngredient;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.AmppFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.IssueRateMarginsFacade;
import com.divudi.facade.ItemBatchFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemsDistributorsFacade;
import com.divudi.facade.MeasurementUnitFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.PharmaceuticalItemCategoryFacade;
import com.divudi.facade.PharmaceuticalItemTypeFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.StockHistoryFacade;
import com.divudi.facade.StoreItemCategoryFacade;
import com.divudi.facade.VmpFacade;
import com.divudi.facade.VmppFacade;
import com.divudi.facade.VtmFacade;
import com.divudi.facade.VirtualProductIngredientFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Singleton;

/**
 *
 * @author Buddhika
 */
@Singleton
public class PharmacyBean {

    @EJB
    PharmaceuticalItemCategoryFacade PharmaceuticalItemCategoryFacade;
    @EJB
    VmppFacade vmppFacade;
    @EJB
    StockFacade stockFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private AmpFacade ampFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private ItemsDistributorsFacade itemsDistributorsFacade;
    @EJB
    private CategoryFacade categoryFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    StockHistoryFacade stockHistoryFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    StoreItemCategoryFacade storeItemCategoryFacade;

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    @EJB
    IssueRateMarginsFacade issueRateMarginsFacade;

    public IssueRateMargins fetchIssueRateMargins(Department fromDepartment, Department toDepartment) {
        String sql;
        HashMap hm = new HashMap();
        sql = "select m from IssueRateMargins m "
                + " where m.retired=false "
                + " and m.fromDepartment=:frm "
                + " and m.toDepartment=:to";
        hm.put("frm", fromDepartment);
        hm.put("to", toDepartment);
        IssueRateMargins m = issueRateMarginsFacade.findFirstByJpql(sql, hm);
        if (m == null) {
            m = new IssueRateMargins();
            m.setCreatedAt(new Date());
            m.setFromDepartment(fromDepartment);
            m.setToDepartment(toDepartment);
            m.setAtPurchaseRate(true);
            m.setShowRates(true);
            m.setRateForConsumables(0.0);
            m.setRateForInventory(0.0);
            m.setRateForPharmaceuticals(0.0);
            issueRateMarginsFacade.create(m);
        }
        return m;
    }

    private Bill createPreBill(Bill bill, WebUser user, Department department, BillNumberSuffix billNumberSuffix) {
        Bill newPre = new PreBill();
        newPre.invertQty();
        newPre.copy(bill);
        newPre.setBilledBill(bill);
        newPre.setDeptId(getBillNumberBean().institutionBillNumberGenerator(department, bill.getBillType(), BillClassType.PreBill, billNumberSuffix));
        newPre.setInsId(getBillNumberBean().institutionBillNumberGenerator(department.getInstitution(), bill.getBillType(), BillClassType.PreBill, billNumberSuffix));
        newPre.setDepartment(department);
        newPre.setInstitution(department.getInstitution());
        newPre.invertValue(bill);
        newPre.setCreatedAt(new Date());
        newPre.setCreater(user);
        newPre.setComments("Re Add To Stock");
        newPre.setBackwardReferenceBill(bill);

        if (newPre.getId() == null) {
            getBillFacade().create(newPre);
        }
        return newPre;
    }

    private Bill createPreBillForIssueCancel(Bill bill, WebUser user, Department department, BillNumberSuffix billNumberSuffix) {
        Bill newPre = new PreBill();
        newPre.invertQty();
        newPre.copy(bill);
        newPre.setBilledBill(bill);
        newPre.setDeptId(getBillNumberBean().institutionBillNumberGenerator(department, bill.getBillType(), BillClassType.PreBill, billNumberSuffix));
        newPre.setInsId(getBillNumberBean().institutionBillNumberGenerator(department.getInstitution(), bill.getBillType(), BillClassType.PreBill, billNumberSuffix));
        newPre.setDepartment(department);
        newPre.setInstitution(department.getInstitution());
        newPre.invertValue(bill);
        newPre.setCreatedAt(new Date());
        newPre.setCreater(user);
        newPre.setComments("Readded To Stock For Issue Bills");
        newPre.setBackwardReferenceBill(bill);

        if (newPre.getId() == null) {
            getBillFacade().create(newPre);
        }
        return newPre;
    }

    private List<BillItem> savePreBillItems(Bill bill, Bill preBill, WebUser user, Department department) {

        List<BillItem> billItems = new ArrayList<>();

        //@Safrin
        if (bill == null) {
            return billItems;
        }

        //@Safrin
        if (bill.getBillItems() == null) {
            return billItems;
        }

        for (BillItem bItem : bill.getBillItems()) {
            BillItem newBillItem = new BillItem();
            newBillItem.copy(bItem);
            newBillItem.invertValue(bItem);
            newBillItem.setBill(preBill);
            newBillItem.setReferanceBillItem(bItem);
            newBillItem.setCreatedAt(new Date());
            newBillItem.setCreater(user);

            if (newBillItem.getId() == null) {
                getBillItemFacade().create(newBillItem);
            }

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(bItem.getPharmaceuticalBillItem());
            ph.invertValue(bItem.getPharmaceuticalBillItem());
            ph.setBillItem(newBillItem);

            if (ph.getId() == null) {
                getPharmaceuticalBillItemFacade().create(ph);
            }

            newBillItem.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(newBillItem);

            //System.err.println("QTY " + bItem.getQty());
            double qty = 0;
            if (bItem.getQty() != null) {
                qty = Math.abs(bItem.getQty());
            }

            addToStock(ph.getStock(), qty, ph, department);
            billItems.add(newBillItem);

        }

        return billItems;

    }

    public Bill reAddToStock(Bill bill, WebUser user, Department department, BillNumberSuffix billNumberSuffix) {

//        if (bill.getBillItems().isEmpty() || bill.isCancelled()) {
//            return null;
//        }
        //@Safrin
        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Bill Already Cancelled");
            return null;
        }
        Bill preBill = createPreBill(bill, user, department, billNumberSuffix);
        List<BillItem> list = savePreBillItems(bill, preBill, user, department);

        bill.setForwardReferenceBill(preBill);
        getBillFacade().edit(bill);

        preBill.setBillItems(list);
        getBillFacade().edit(preBill);

        return preBill;
    }

    public Bill readdStockForIssueBills(PreBill bill, WebUser user, Department department, BillNumberSuffix billNumberSuffix) {
        if (bill.getBillItems().isEmpty() || bill.isCancelled()) {
            return null;
        }
        Bill preBill = createPreBillForIssueCancel(bill, user, department, billNumberSuffix);
        List<BillItem> list = savePreBillItems(bill, preBill, user, department);

        bill.setForwardReferenceBill(preBill);
        getBillFacade().edit(bill);

        preBill.setBillItems(list);
        getBillFacade().edit(preBill);

        return preBill;
    }

    public PharmaceuticalItemCategoryFacade getPharmaceuticalItemCategoryFacade() {
        return PharmaceuticalItemCategoryFacade;
    }

    public List<Stock> availableStocks(Item item, Department dept) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", dept);
        double d = 0.0;
        m.put("s", d);
        m.put("item", item);
        sql = "select i from Stock i where i.stock >:s and i.department=:d and i.itemBatch.item=:item order by i.itemBatch.dateOfExpire ";
        items = getStockFacade().findByJpql(sql, m);
        return items;
    }

    public List<Stock> getStocksFromIemBatch(Item item, Department dept) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", dept);
        double d = 0.0;
        m.put("s", d);
        m.put("item", item);
        sql = "select i from Stock i where i.stock >:s and i.department=:d and i.itemBatch.item=:item order by i.itemBatch.dateOfExpire ";
        items = getStockFacade().findByJpql(sql, m);
        return items;
    }

    public void setPharmaceuticalItemCategoryFacade(PharmaceuticalItemCategoryFacade PharmaceuticalItemCategoryFacade) {
        this.PharmaceuticalItemCategoryFacade = PharmaceuticalItemCategoryFacade;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public double getStockQty(ItemBatch batch, Department department) {
        //    //System.err.println("Item Batch "+batch);
        //     //System.err.println("Deprtment "+department);
        String sql;
        HashMap hm = new HashMap();
        sql = "select sum(s.stock) from Stock s where s.itemBatch=:batch "
                + " and s.department=:dep";
        hm.put("batch", batch);
        hm.put("dep", department);
        return getStockFacade().findDoubleByJpql(sql, hm);
    }

    public double getStockQty(ItemBatch batch, Staff staff) {
        String sql;
        HashMap hm = new HashMap();
        sql = "select sum(s.stock) from Stock s where s.itemBatch=:batch "
                + " and s.staff=:stf";
        hm.put("batch", batch);
        hm.put("stf", staff);
        double vl = getStockFacade().findAggregateDbl(sql);
        return vl;
    }

    public double getStockQty(ItemBatch batch, Institution institution) {
        String sql;
        sql = "select sum(s.stock) from Stock s where s.itemBatch.id = " + batch.getId() + " and s.department.institution.id = " + institution.getId();
        return getStockFacade().findAggregateDbl(sql);
    }

    public double getStockQty(Item item, Department department) {
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }
        String sql;
        Map m = new HashMap();
        m.put("d", department);
        m.put("i", item);
        sql = "select sum(s.stock) from Stock s where s.department=:d and s.itemBatch.item=:i";
        return getStockFacade().findDoubleByJpql(sql, m);

    }

    public double getStockQty(Item item, Institution institution) {
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }
        String sql;
        Map m = new HashMap();
        m.put("d", institution);
        m.put("i", item);
        sql = "select sum(s.stock) from Stock s where s.department.institution=:d and s.itemBatch.item=:i";
        return getStockFacade().findAggregateDbl(sql, m);
    }

    public double getStockByPurchaseValue(ItemBatch batch) {
        Map m = new HashMap<>();
        String sql;
        m.put("i", batch);
        sql = "Select sum(s.itemBatch.purcahseRate * s.stock) from Stock s where s.itemBatch=:i";
        return getItemBatchFacade().findDoubleByJpql(sql, m);
    }

    public double getStockByPurchaseValue(Item item) {
        Map m = new HashMap<>();
        String sql;
        m.put("i", item);
        sql = "Select sum(s.itemBatch.purcahseRate * s.stock) from Stock s where s.itemBatch.item=:i";
        return getItemBatchFacade().findDoubleByJpql(sql, m);
    }

    public double getStockByPurchaseValue(Item item, Department dept) {
        Map m = new HashMap<>();
        String sql;
        m.put("i", item);
        m.put("d", dept);
        sql = "Select sum(s.itemBatch.purcahseRate * s.stock) from Stock s where s.itemBatch.item=:i and s.department=:d";
        return getItemBatchFacade().findDoubleByJpql(sql, m);
    }

    public double getStockWithoutPurchaseValue(Item item, Department dept) {
        Map m = new HashMap<>();
        String sql;
        m.put("i", item);
        m.put("d", dept);
        sql = "Select sum(s.stock) from Stock s where s.itemBatch.item=:i and s.department=:d";
        return getItemBatchFacade().findDoubleByJpql(sql, m);
    }

    public double getStockByPurchaseValue(Item item, Institution ins) {
        Map m = new HashMap<>();
        String sql;
        m.put("i", item);
        m.put("ins", ins);
        sql = "Select sum(s.itemBatch.purcahseRate * s.stock) from Stock s where s.itemBatch.item=:i and s.department.institution=:ins";
        return getItemBatchFacade().findDoubleByJpql(sql, m);
    }

    public boolean resetStock(PharmaceuticalBillItem ph, Stock stock, double qty, Department department) {
        if (stock.getId() == null) {
            return false;
        }
        stock = getStockFacade().find(stock.getId());
        stock.setStock(qty);
        getStockFacade().edit(stock);
        addToStockHistory(ph, stock, department);
        return true;
    }

    public Stock addToStock(PharmaceuticalBillItem pharmaceuticalBillItem, double qty, Staff staff) {
        String sql;
        HashMap hm = new HashMap();
        sql = "Select s from Stock s where s.itemBatch=:bc and s.staff=:stf";
        hm.put("bc", pharmaceuticalBillItem.getItemBatch());
        hm.put("stf", staff);
        Stock s = getStockFacade().findFirstByJpql(sql, hm);
        if (s == null) {
            s = new Stock();
            s.setStaff(staff);
            s.setItemBatch(pharmaceuticalBillItem.getItemBatch());
        }
        if (s.getId() == null || s.getId() == 0) {
            s.setStock(s.getStock() + qty);
            getStockFacade().create(s);
        } else {
            s.setStock(s.getStock() + qty);
            getStockFacade().edit(s);
        }
        addToStockHistory(pharmaceuticalBillItem, s, staff);
        return s;
    }

    public Stock addToStock(PharmaceuticalBillItem pharmaceuticalBillItem, double qty, Department department) {
        String sql;
        HashMap hm = new HashMap();
        sql = "Select s from Stock s where s.itemBatch=:bch and s.department=:dep";
        hm.put("bch", pharmaceuticalBillItem.getItemBatch());
        hm.put("dep", department);
        Stock s = getStockFacade().findFirstByJpql(sql, hm);
        if (s == null || pharmaceuticalBillItem.getBillItem().getItem().getDepartmentType() == DepartmentType.Inventry) {
            s = new Stock();
            s.setDepartment(department);
            s.setCode(pharmaceuticalBillItem.getCode());
            s.setItemBatch(pharmaceuticalBillItem.getItemBatch());
        }
        if (s.getId() == null || s.getId() == 0) {
            s.setStock(s.getStock() + qty);
            s.setCode(pharmaceuticalBillItem.getCode());
            getStockFacade().create(s);
            addToStockHistoryInitial(pharmaceuticalBillItem, s, department);
        } else {
            s.setStock(s.getStock() + qty);
            getStockFacade().edit(s);
            addToStockHistory(pharmaceuticalBillItem, s, department);
        }
        return s;
    }

    public boolean deductFromStock(ItemBatch batch, double qty, Department department) {
        String sql;
        HashMap hm = new HashMap();
        sql = "Select s from Stock s where s.itemBatch=:bch and"
                + " s.department=:dep";
        hm.put("bch", batch);
        hm.put("dep", department);
        Stock s = getStockFacade().findFirstByJpql(sql, hm);
        if (s == null) {
            s = new Stock();
            s.setDepartment(department);
            s.setItemBatch(batch);
        }
        if (s.getStock() < qty) {
            return false;
        }
        s.setStock(s.getStock() - qty);
        if (s.getId() == null || s.getId() == 0) {
            getStockFacade().create(s);
        } else {
            getStockFacade().edit(s);
        }
        return true;
    }

    public boolean deductFromStock(PharmaceuticalBillItem pharmaceuticalBillItem, double qty, Staff staff) {
        String sql;
        HashMap hm = new HashMap();
        sql = "Select s from Stock s where s.itemBatch=:batch "
                + "and s.staff=:stf";
        hm.put("batch", pharmaceuticalBillItem.getItemBatch());
        hm.put("stf", staff);
        Stock s = getStockFacade().findFirstByJpql(sql, hm);
        if (s == null) {
            s = new Stock();
            s.setStaff(staff);
            s.setItemBatch(pharmaceuticalBillItem.getItemBatch());
        }
        if (s.getStock() < qty) {
            return false;
        }

        if (s.getId() == null || s.getId() == 0) {
            s.setStock(s.getStock() - qty);
            getStockFacade().create(s);
        } else {
            s.setStock(s.getStock() - qty);
            getStockFacade().edit(s);
        }
        addToStockHistory(pharmaceuticalBillItem, s, staff);
        return true;
    }

    public boolean deductFromStock(ItemBatch batch, double qty, Department department, boolean minusAllowed) {
        if (!minusAllowed) {
            return deductFromStock(batch, qty, department);
        }
        String sql;
        sql = "Select s from Stock s where s.itemBatch.id = " + batch.getId() + " and s.department.id = " + department.getId();
        Stock s = getStockFacade().findFirstByJpql(sql);
        if (s == null) {
            s = new Stock();
            s.setDepartment(department);
            s.setItemBatch(batch);
        }
        s.setStock(s.getStock() - qty);
        if (s.getId() == null || s.getId() == 0) {
            getStockFacade().create(s);
        } else {
            getStockFacade().edit(s);
        }
        return true;
    }

    public List<ItemBatchQty> deductFromStock(Item item, double qty, Department department) {
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }

        if (qty <= 0) {
            return new ArrayList<>();
        }
        String sql;
        Map m = new HashMap();
        m.put("i", item);
        m.put("d", department);
        sql = "select s from Stock s where s.itemBatch.item=:i "
                + " and s.department=:d order by s.itemBatch.dateOfExpire asc";
        List<Stock> stocks = getStockFacade().findByJpql(sql, m);
        List<ItemBatchQty> dl = new ArrayList<>();
        double toAddQty = qty;
        //System.err.println("QTY 1 : " + toAddQty);
        for (Stock s : stocks) {
            if (s.getStock() >= toAddQty) {
                deductFromStock(s.getItemBatch(), toAddQty, department);
                //System.err.println("QTY 2 : " + s.getStock());
                dl.add(new ItemBatchQty(s.getItemBatch(), toAddQty));
                break;
            } else {
                toAddQty = toAddQty - s.getStock();
                dl.add(new ItemBatchQty(s.getItemBatch(), s.getStock()));
                deductFromStock(s.getItemBatch(), s.getStock(), department);
            }
        }
        return dl;
    }

    public List<StockQty> getStockByQty(Item item, double qty, Department department) {
        System.out.println("getStockByQty");
        System.out.println("department = " + department);
        System.out.println("qty = " + qty);
        System.out.println("item = " + item);
        if (qty <= 0) {
            return new ArrayList<>();
        }
        String sql = "";
        Map m = new HashMap();

        m.put("d", department);
        m.put("q", 1.0);
        if (item instanceof Amp) {
            sql = "select s "
                    + " from Stock s "
                    + " where s.itemBatch.item=:amp "
                    + " and s.department=:d and s.stock >=:q "
                    + " and s.itemBatch.dateOfExpire > :doe "
                    + " order by s.itemBatch.dateOfExpire ";
            m.put("amp", item);
            m.put("doe", new Date());
        } else if (item instanceof Vmp) {
            List<Amp> amps = findAmpsForVmp((Vmp) item);
            sql = "select s "
                    + " from Stock s "
                    + " where s.itemBatch.item in :amps "
                    + " and s.itemBatch.dateOfExpire > :doe"
                    + " and s.department=:d and s.stock >=:q order by s.itemBatch.dateOfExpire ";
            m.put("amps", amps);
        } else {
            JsfUtil.addErrorMessage("Not supported yet");
            return new ArrayList<>();
        }
        System.out.println("m = " + m);
        List<Stock> stocks = getStockFacade().findByJpql(sql, m);
        List<StockQty> list = new ArrayList<>();
        double toAddQty = qty;
        for (Stock s : stocks) {
            if (s.getStock() >= toAddQty) {
                list.add(new StockQty(s, toAddQty));
                break;
            } else {
                toAddQty = toAddQty - s.getStock();
                list.add(new StockQty(s, s.getStock()));
            }
        }
        return list;
    }

    public List<StockQty> getStockByQty(Vmp item, double qty, Department department) {
        List<StockQty> stocks = new ArrayList<>();
        if (item == null) {
            return stocks;
        }
        List<Amp> amps = findAmpsForVmp(item);
        if (amps == null) {
            return stocks;
        }
        for (Amp a : amps) {
            List<StockQty> sq = getStockByQty(a, qty, department);

        }
        return stocks;
    }

    public List<Amp> findAmpsForVmp(Vmp vmp) {
        String jpql;
        Map m = new HashMap();
        m.put("vmp", vmp);
        m.put("ret", false);
        jpql = "select amp "
                + " from Amp amp "
                + " where amp.retired=:ret "
                + " and amp.vmp=:vmp";
        return ampFacade.findByJpql(jpql, m);
    }

    public List<StockQty> getStockByQty(Amp item, double qty, Department department) {
        if (qty <= 0) {
            return new ArrayList<>();
        }
        String sql;
        Map m = new HashMap();
        m.put("i", item);
        m.put("d", department);
        m.put("q", 1.0);
        sql = "select s from Stock s where s.itemBatch.item=:i "
                + " and s.department=:d and s.stock >=:q order by s.itemBatch.dateOfExpire ";
        List<Stock> stocks = getStockFacade().findByJpql(sql, m);
        List<StockQty> list = new ArrayList<>();
        double toAddQty = qty;
        for (Stock s : stocks) {
            if (s.getStock() >= toAddQty) {
                list.add(new StockQty(s, toAddQty));
                break;
            } else {
                toAddQty = toAddQty - s.getStock();
                list.add(new StockQty(s, s.getStock()));
                // //     deductFromStock(s.getItemBatch(), s.getStock(), department);
            }
        }
        return list;
    }

    public boolean deductFromStock(Stock stock, double qty, PharmaceuticalBillItem pbi, Department d) {
        if (stock == null) {
            return false;
        }

        if (stock.getId() == null) {
            return false;
        }

        if (stock.getStock() < qty) {
            return false;
        }
        stock = getStockFacade().find(stock.getId());
        stock.setStock(stock.getStock() - qty);
        getStockFacade().edit(stock);
        addToStockHistory(pbi, stock, d);
        return true;
    }

    @Deprecated
    public boolean deductFromStockWithoutHistory(Stock stock, double qty, PharmaceuticalBillItem pbi, Department d) {
        if (stock == null) {
            return false;
        }

        if (stock.getId() == null) {
            return false;
        }

        if (stock.getStock() < qty) {
            return false;
        }
        stock = getStockFacade().find(stock.getId());
        stock.setStock(stock.getStock() - qty);
        getStockFacade().edit(stock);
        return true;
    }

    public void addToStockHistory(PharmaceuticalBillItem phItem, Stock stock, Department d) {
        System.out.println("d = " + d);
        if (phItem == null) {
            return;
        }

        if (phItem.getBillItem() == null) {
            return;
        }

        if (phItem.getBillItem().getItem() == null) {
            return;
        }

        StockHistory sh;
        String sql;
        sql = "Select sh from StockHistory sh where sh.pbItem=:pbi";
        Map m = new HashMap();
        m.put("pbi", phItem);
        sh = getStockHistoryFacade().findFirstByJpql(sql, m);
        if (sh == null) {
            sh = new StockHistory();
        } else {
            return;
        }

        sh.setFromDate(new Date());
        sh.setPbItem(phItem);
        sh.setHxDate(Calendar.getInstance().get(Calendar.DATE));
        sh.setHxMonth(Calendar.getInstance().get(Calendar.MONTH));
        sh.setHxWeek(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        sh.setHxYear(Calendar.getInstance().get(Calendar.YEAR));

        sh.setStockAt(new Date());
        sh.setCreatedAt(new Date());
        sh.setDepartment(d);
        sh.setInstitution(d.getInstitution());

        Stock fetchedStock = getStockFacade().find(stock.getId());
        sh.setStockQty(fetchedStock.getStock());
        sh.setItem(phItem.getBillItem().getItem());
        sh.setItemBatch(fetchedStock.getItemBatch());
        sh.setItemStock(getStockQty(phItem.getBillItem().getItem(), d));
        if (sh.getId() == null) {
            getStockHistoryFacade().create(sh);
        } else {
            getStockHistoryFacade().edit(sh);
        }
        phItem.setStockHistory(sh);
        getPharmaceuticalBillItemFacade().edit(phItem);
    }

    public void addToStockHistoryInitial(PharmaceuticalBillItem phItem, Stock stock, Department d) {
        if (phItem == null) {
            return;
        }

        if (phItem.getBillItem() == null) {
            return;
        }

        if (phItem.getBillItem().getItem() == null) {
            return;
        }

        StockHistory sh;
        String sql;
        sql = "Select sh from StockHistory sh where sh.pbItem=:pbi";
        Map m = new HashMap();
        m.put("pbi", phItem);
        sh = getStockHistoryFacade().findFirstByJpql(sql, m);
        if (sh == null) {
            sh = new StockHistory();
        } else {
            return;
        }

        sh.setFromDate(new Date());
        sh.setPbItem(phItem);
        sh.setHxDate(Calendar.getInstance().get(Calendar.DATE));
        sh.setHxMonth(Calendar.getInstance().get(Calendar.MONTH));
        sh.setHxWeek(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        sh.setHxYear(Calendar.getInstance().get(Calendar.YEAR));

        sh.setStockAt(new Date());
        sh.setCreatedAt(new Date());
        sh.setDepartment(d);
        sh.setInstitution(d.getInstitution());

        Stock fetchedStock = getStockFacade().find(stock.getId());

        sh.setStockQty(0.0);
        sh.setItem(phItem.getBillItem().getItem());
        sh.setItemBatch(fetchedStock.getItemBatch());
        sh.setItemStock(getStockQty(phItem.getBillItem().getItem(), phItem.getBillItem().getBill().getDepartment()));

        if (sh.getId() == null) {
            getStockHistoryFacade().create(sh);
        } else {
            getStockHistoryFacade().edit(sh);
        }

        phItem.setStockHistory(sh);
        getPharmaceuticalBillItemFacade().edit(phItem);

    }

    public void addToStockHistory(PharmaceuticalBillItem phItem, Stock stock, Staff staff) {
        if (phItem == null) {
            return;
        }

        if (phItem.getBillItem() == null) {
            return;
        }

        if (phItem.getBillItem().getItem() == null) {
            return;
        }

        StockHistory sh;
        String sql;
        sql = "Select sh from StockHistory sh where sh.pbItem=:pbi";
        Map m = new HashMap();
        m.put("pbi", phItem);
        sh = getStockHistoryFacade().findFirstByJpql(sql, m);
        if (sh == null) {
            sh = new StockHistory();
        } else {
            return;
        }

        sh.setFromDate(Calendar.getInstance().getTime());
        sh.setPbItem(phItem);
        sh.setHxDate(Calendar.getInstance().get(Calendar.DATE));
        sh.setHxMonth(Calendar.getInstance().get(Calendar.MONTH));
        sh.setHxWeek(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        sh.setHxYear(Calendar.getInstance().get(Calendar.YEAR));

        sh.setStockAt(Calendar.getInstance().getTime());

        sh.setStaff(staff);
        Stock fetchedStock = getStockFacade().find(stock.getId());

        sh.setStockQty(fetchedStock.getStock());
        sh.setItemStock(getStockQty(phItem.getBillItem().getItem(), phItem.getBillItem().getBill().getDepartment()));
        sh.setItem(phItem.getBillItem().getItem());
        sh.setItemBatch(fetchedStock.getItemBatch());

        if (sh.getId() == null) {
            getStockHistoryFacade().create(sh);
        } else {
            getStockHistoryFacade().edit(sh);
        }

        phItem.setStockHistory(sh);
        getPharmaceuticalBillItemFacade().edit(phItem);

        //System.err.println("Histry Saved " + sh.getStockQty());
    }

    //
    public boolean addToStock(Stock stock, double qty, PharmaceuticalBillItem pbi, Department d) {
        if (stock == null) {
            return false;
        }
        if (stock.getStock() == null) {
            return false;
        }
        if (stock.getId() == null) {
            return false;
        }
        stock = getStockFacade().find(stock.getId());
        stock.setStock(stock.getStock() + qty);
        getStockFacade().edit(stock);
        addToStockHistory(pbi, stock, d);
        return true;
    }

    @Deprecated
    public boolean addToStockWithoutHistory(Stock stock, double qty, PharmaceuticalBillItem pbi, Department d) {
        if (stock == null) {
            return false;
        }
        if (stock.getStock() == null) {
            return false;
        }

        if (stock.getId() == null) {
            return false;
        }
        stock = getStockFacade().find(stock.getId());
        stock.setStock(stock.getStock() + qty);
        getStockFacade().edit(stock);
        return true;
    }

    //hhhh
//    public List<ItemBatchQty> deductFromStock(Item item, double qty, Staff staff, PharmaceuticalBillItem pbi, Department d) {
//        if (item instanceof Ampp) {
//            item = ((Ampp) item).getAmp();
//        }
//
//        String sql;
//        sql = "select s from Stock s where s.itemBatch.item.id = " + item.getId() + " and s.staff.id = " + staff.getId() + " order by s.itemBatch.dateOfExpire desc";
//        List<Stock> stocks = getStockFacade().findByJpql(sql);
//        List<ItemBatchQty> dl = new ArrayList<>();
//        double toAddQty = qty;
//        for (Stock s : stocks) {
//            if (toAddQty <= 0) {
//                break;
//            }
//            if (s.getStock() >= toAddQty) {
//                deductFromStock(s.getItemBatch(), toAddQty, staff);
//                dl.add(new ItemBatchQty(s.getItemBatch(), toAddQty));
//                break;
//            } else {
//                toAddQty = toAddQty - s.getStock();
//                dl.add(new ItemBatchQty(s.getItemBatch(), s.getStock()));
//                deductFromStock(s.getItemBatch(), s.getStock(), staff);
//            }
//        }
//        return dl;
//    }
//    public double getRetailRate(Item item, Department department) {
//
//        //////System.out.println("getting Retail rate");
//        double rate = getLastRetailRate(item, department);
//        if (item instanceof Ampp) {
//            return rate * item.getDblValue();
//        } else if (item instanceof Amp) {
//            return rate;
//        } else {
//            return 0.0;
//        }
//    }
    public double getWholesaleRate(Item item, Department department) {
        return 0.0;
    }

    public double getRetailRate(ItemBatch batch, Department department) {
        return 0.0;
    }

//    public double getRetailRate(Stock stock, PaymentScheme paymentScheme) {
//        if (stock == null) {
//            return 0.0;
//        }
//        if (paymentScheme == null) {
//            return stock.getItemBatch().getRetailsaleRate();
//        } else {
//            return stock.getItemBatch().getRetailsaleRate() * (1 + ((paymentScheme.getDiscountPercentForPharmacy()) / 100));
//        }
//    }
    public double getSaleRate(ItemBatch batch, Department department) {
        return 0.0;
    }

    public double getWholesaleRate(ItemBatch batch, Department department) {
        return 0.0;
    }

    public double getPurchaseRate(ItemBatch batch, Department department) {
        String sql;
        sql = "Select s from Stock s where s.itemBatch=:b and s.department=:d";
        Map m = new HashMap();
        m.put("b", batch);
        m.put("d", department);
        Stock s = getStockFacade().findFirstByJpql(sql, m);
        if (s == null) {
            return 10.0;
        } else {
            return s.getItemBatch().getPurcahseRate();
        }
    }

//    public double getPurchaseRate(Item item, Department department) {
//        //////System.out.println("getting purchase rate");
//        double rate = getLastPurchaseRate(item, department);
//        if (item instanceof Ampp) {
//            return rate * item.getDblValue();
//        } else if (item instanceof Amp) {
//            return rate;
//        } else {
//            return 0.0;
//        }
//
//    }
    public void reSetPurchaseRate(ItemBatch batch, Department department) {
    }

    public void reSetRetailRate(ItemBatch batch, Department department) {
    }

    public void setPurchaseRate(ItemBatch batch, Department department) {
    }

    public void setPurchaseRate(Item item, Department department) {
    }

    public void setRetailRate(Item item, Department department) {
    }

    public void setSaleRate(Item item, Department department) {
    }

    public void setWholesaleRate(Item item, Department department) {
    }

    public void setRetailRate(ItemBatch batch, Department department) {
    }

    public void setSaleRate(ItemBatch batch, Department department) {
    }

    public void setWholesaleRate(ItemBatch batch, Department department) {
    }

    public ItemBatch getItemBatch(Date doe, String batchNo, Item item) {
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }
        String sql;
        Map m = new HashMap();
        sql = "Select ib from ItemBatch ib where ib.item=:i and ib.dateOfExpire=:doe and ib.batchNo=:batchNo";
        m.put("i", item);
        m.put("batchNo", batchNo);
        m.put("doe", doe);
        ItemBatch ib = getItemBatchFacade().findFirstByJpql(sql, m);
        if (ib == null) {
            ib = new ItemBatch();
            ib.setDateOfExpire(doe);
            ib.setBatchNo(batchNo);
            ib.setItem(item);
            getItemBatchFacade().create(ib);
        }
        return ib;
    }

    public ItemBatch getItemBatch(Date doe, double salePrice, double purchasePrice, Item item) {
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }
        String sql;
        Map m = new HashMap();
        sql = "Select ib from ItemBatch ib where ib.item=:i and ib.dateOfExpire=:doe and ib.purcahseRate=:pr and ib.retailsaleRate=:rr";
        m.put("i", item);
        m.put("pr", purchasePrice);
        m.put("rr", salePrice);
        m.put("doe", doe);
        ItemBatch ib = getItemBatchFacade().findFirstByJpql(sql, m);
        if (ib == null) {
            ib = new ItemBatch();
            ib.setDateOfExpire(doe);
            ib.setPurcahseRate(purchasePrice);
            ib.setRetailsaleRate(salePrice);
            ib.setItem(item);
            getItemBatchFacade().create(ib);
        }
        return ib;
    }

    public VmppFacade getVmppFacade() {
        return vmppFacade;
    }

    public void setVmppFacade(VmppFacade vmppFacade) {
        this.vmppFacade = vmppFacade;
    }

    public Vmpp getVmpp(Ampp ampp, MeasurementUnit packUnit) {
        String sql;
        Map m = new HashMap();
        m.put("vmp", ampp.getAmp().getVmp());
        m.put("s", packUnit);
        m.put("d", ampp.getDblValue());
        sql = "select v from Vmpp v where v.retired=false and v.vmp =:vmp and v.dblValue =:d and v.packUnit=:s";
        Vmpp v = getVmppFacade().findFirstByJpql(sql, m);
        if (v == null) {
            v = new Vmpp();
            v.setVmp(ampp.getAmp().getVmp());
            v.setDblValue(ampp.getDblValue());
            v.setPackUnit(packUnit);
            try {
                v.setName(ampp.getAmp().getVmp().getName() + "(" + ampp.getDblValue() + " " + ampp.getVmpp().getPackUnit().getName() + ")");
            } catch (Exception e) {
                //System.err.println("Error : " + e.getMessage());
            }
            getVmppFacade().create(v);
        }
        return v;
    }

    public double getOrderingQty(Item item, Department department) {
//        double qty = 10;
//        if (item instanceof Ampp) {
//            qty /= item.getDblValue();
//        }
//        return (double) qty;
        return 0;
    }

    public double getMaximumPurchasePriceChange() {
        return 50.0;
    }

    public double getMaximumRetailPriceChange() {
        return 15.0;
    }

    public void setMaximumGrnPriceChange() {
    }

    public void recordPrice() {
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public ItemsDistributorsFacade getItemsDistributorsFacade() {
        return itemsDistributorsFacade;
    }

    public void setItemsDistributorsFacade(ItemsDistributorsFacade itemsDistributorsFacade) {
        this.itemsDistributorsFacade = itemsDistributorsFacade;
    }

    public CategoryFacade getCategoryFacade() {
        return categoryFacade;
    }

    public void setCategoryFacade(CategoryFacade categoryFacade) {
        this.categoryFacade = categoryFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PharmaceuticalItemCategory getPharmaceuticalCategoryByName(String name, boolean createNew) {
        if (name == null || name.trim().equals("")) {
            return null;
        }
        name = name.trim();
        PharmaceuticalItemCategory cat;
        Map m = new HashMap();

        m.put("name", name.toUpperCase());
        name = name.replaceAll("\'", "");
        name = name.replaceAll("\"", "");
        String j = "SELECT c FROM PharmaceuticalItemCategory c Where (c.name)=:name ";

        try {
            cat = getPharmaceuticalItemCategoryFacade().findFirstByJpql(j, m);
        } catch (Exception e) {
            //System.out.println("error = " + e.getMessage());
            //System.out.println("name = " + name);
            return null;
        }

        if (cat == null && createNew == true) {
            cat = new PharmaceuticalItemCategory();
            cat.setRetired(false);
            cat.setName(name);
            try {
                getPharmaceuticalItemCategoryFacade().create(cat);
            } catch (Exception e) {
                return null;
            }
        } else if (cat != null) {
            cat.setRetired(false);
            cat.setName(name);
            getPharmaceuticalItemCategoryFacade().edit(cat);
        }
        return cat;
    }

    @EJB
    PharmaceuticalItemTypeFacade pharmaceuticalItemTypeFacade;

    public PharmaceuticalItemType getPharmaceuticalItemTypeByName(String name, boolean createNew) {
        if (name == null || name.trim().equals("")) {
            return null;
        }
        name = name.trim();
        PharmaceuticalItemType cat = null;
        String j = "SELECT c FROM PharmaceuticalItemType c Where (c.name) = :n";
        Map m = new HashMap();
        m.put("n", name.trim().toUpperCase());
        try {
            cat = pharmaceuticalItemTypeFacade.findFirstByJpql(j, m);

        } catch (Exception e) {
            return null;
        }

        if (cat == null && createNew == true) {
            cat = new PharmaceuticalItemType();
            cat.setName(name);
            pharmaceuticalItemTypeFacade.create(cat);
        } else if (cat != null) {
            cat.setRetired(false);
            cat.setName(name);
            pharmaceuticalItemTypeFacade.edit(cat);
        }
        return cat;
    }

    public PharmaceuticalItemType getPharmaceuticalItemTypeByName(String name) {
        return getPharmaceuticalItemTypeByName(name, true);
    }

    public StoreItemCategory getStoreItemCategoryByName(String name, boolean createNew) {
        if (name == null || name.trim().equals("")) {
            return null;
        }
        name = name.trim();
        StoreItemCategory cat;
        cat = getStoreItemCategoryFacade().findFirstByJpql("SELECT c FROM StoreItemCategory c Where (c.name) = '" + name.toUpperCase() + "' ");
        if (cat == null && createNew == true) {
            cat = new StoreItemCategory();
            cat.setName(name);
            getStoreItemCategoryFacade().create(cat);
        } else if (cat != null) {
            cat.setRetired(false);
            cat.setName(name);
            getStoreItemCategoryFacade().edit(cat);
        }
        return cat;
    }

    public PharmaceuticalItemCategory getPharmaceuticalCategoryByName(String name) {
        return getPharmaceuticalCategoryByName(name, true);
    }

    public StoreItemCategory getStoreItemCategoryByName(String name) {
        return getStoreItemCategoryByName(name, true);
    }

    public MeasurementUnit getUnitByName(String name, boolean createNew) {
        if (name == null || name.trim().equals("")) {
            return null;
        }
        MeasurementUnit m;
        name = name.trim();
        String sql;
        Map map = new HashMap();
        sql = "SELECT c FROM MeasurementUnit c Where (c.name) =:n ";
        map.put("n", name.toUpperCase());
        m = getMeasurementUnitFacade().findFirstByJpql(sql, map);
        if (m == null && createNew == true) {
            m = new MeasurementUnit();
            m.setName(name);
            getMeasurementUnitFacade().create(m);
        } else if (m != null) {
            m.setName(name);
            m.setRetired(false);
            getMeasurementUnitFacade().edit(m);
        }
        return m;
    }

    public MeasurementUnit getUnitByName(String name) {
        return getUnitByName(name, true);
    }

    @EJB
    VmpFacade vmpFacade;
    @EJB
    VirtualProductIngredientFacade virtualProductIngredientFacade;

    public VirtualProductIngredientFacade getVirtualProductIngredientFacade() {
        return virtualProductIngredientFacade;
    }

    public void setVirtualProductIngredientFacade(VirtualProductIngredientFacade virtualProductIngredientFacade) {
        this.virtualProductIngredientFacade = virtualProductIngredientFacade;
    }

    public VmpFacade getVmpFacade() {
        return vmpFacade;
    }

    public void setVmpFacade(VmpFacade vmpFacade) {
        this.vmpFacade = vmpFacade;
    }

    @EJB
    AmppFacade amppFacade;

    public AmppFacade getAmppFacade() {
        return amppFacade;
    }

    public void setAmppFacade(AmppFacade amppFacade) {
        this.amppFacade = amppFacade;
    }

    public Vmpp getVmpp(Vmp vmp, double issueUnitsPerPack, MeasurementUnit packUnit) {
        if (vmp == null || packUnit == null || vmp.getCategory() == null) {
            //////System.out.println("vmp is " + vmp);
            //////System.out.println("pack unit is " + packUnit);
            //////System.out.println("vmp is " + vmp);
            if (vmp != null) {
                //////System.out.println("cat is " + vmp.getCategory());
            }
            return null;
        }
        String sql;
        Map m = new HashMap();
        m.put("v", vmp);
        m.put("u", packUnit);
        m.put("d", issueUnitsPerPack);
        sql = "select p from Vmpp p where p.vmp=:v and p.packUnit=:u and p.dblValue=:d";
        Vmpp vmpp = getVmppFacade().findFirstByJpql(sql, m);
        if (vmpp == null) {
            vmpp = new Vmpp();
            vmpp.setVmp(vmp);
            vmpp.setName(vmp.getName() + " " + vmp.getCategory().getName() + " (" + issueUnitsPerPack + " " + packUnit.getName() + ")");
            vmpp.setPackUnit(packUnit);
            vmpp.setCreatedAt(Calendar.getInstance().getTime());
            vmpp.setDblValue((double) issueUnitsPerPack);
            getVmppFacade().create(vmpp);
        } else {
            vmpp.setRetired(false);
            getVmppFacade().edit(vmpp);
        }
        return vmpp;
    }

    public Ampp getAmpp(Amp amp) {
        String sql = "select a from Ampp a where a.retired=false and a.amp.id=" + amp.getId();
        return getAmppFacade().findFirstByJpql(sql);
    }

    public Ampp getAmpp(Amp amp, double issueUnitsPerPack, MeasurementUnit unit) {
        Vmpp vmpp = getVmpp(amp.getVmp(), issueUnitsPerPack, unit);
        Ampp ampp;
        String sql;
        Map m = new HashMap();
        m.put("v", vmpp);
        m.put("a", amp);
        sql = "select p from Ampp p where p.vmpp=:v and p.amp=:a";
        ampp = getAmppFacade().findFirstByJpql(sql, m);
        if (ampp == null) {
            ampp = new Ampp();
            ampp.setAmp(amp);
            ampp.setName(amp.getName() + " " + issueUnitsPerPack + amp.getMeasurementUnit() + unit.getName());
            ampp.setDblValue((double) issueUnitsPerPack);
            ampp.setMeasurementUnit(unit);
            ampp.setVmpp(vmpp);
            getAmppFacade().create(ampp);
        } else {
            ampp.setRetired(false);
            getAmppFacade().edit(ampp);
        }
        return ampp;
    }

    public Vmp getVmp(Vtm vtm, double strength, MeasurementUnit strengthUnit, PharmaceuticalItemCategory cat) {
        String sql;
        String vmpName = "";

        if (vtm != null && vtm.getName() != null) {
            vmpName += vtm.getName();
        }

        if (strength < 0.00000001) {
            vmpName += " " + strength;
        }

        if (strengthUnit != null && strengthUnit.getName() != null) {
            vmpName += (vmpName.isEmpty() ? "" : " ") + strengthUnit.getName();
        }

        if (cat != null && cat.getName() != null) {
            vmpName += (vmpName.isEmpty() ? "" : " ") + cat.getName();
        }

        vmpName = vmpName.trim();

        Map m = new HashMap();
        m.put("n", vmpName);
        m.put("v", vtm);
        sql = "select v "
                + "from Vmp v "
                + "where v.name=:n "
                + " and v.vtm=:v";
        Vmp vmp = vmpFacade.findFirstByJpql(sql, m);
        if (vmp == null) {
            vmp = new Vmp();
            vmp.setName(vmpName);
            vmp.setVtm(vtm);
            vmp.setCreatedAt(Calendar.getInstance().getTime());
            getVmpFacade().create(vmp);
        }
        return vmp;
    }

    public Vtm getVtmByName(String name, boolean createNew) {
        if (name == null || name.trim().equals("")) {
            return null;
        }
        name = name.trim();
        Vtm vtm = null;
        Map m = new HashMap();
        m.put("n", name.toUpperCase());
        vtm = getVtmFacade().findFirstByJpql("SELECT c FROM Vtm c Where (c.name) =:n ", m);
        if (vtm == null && createNew) {
            vtm = new Vtm();
            vtm.setName(name);
            getVtmFacade().create(vtm);
        } else if (vtm != null) {
            vtm.setName(name);
            vtm.setRetired(false);
            getVtmFacade().edit(vtm);
        }
        return vtm;
    }

    public Vtm getVtmByName(String name) {
        return getVtmByName(name, true);
    }

    @EJB
    private VtmFacade VtmFacade;
    @EJB
    private MeasurementUnitFacade measurementUnitFacade;

    public MeasurementUnitFacade getMeasurementUnitFacade() {
        return measurementUnitFacade;
    }

    public void setMeasurementUnitFacade(MeasurementUnitFacade measurementUnitFacade) {
        this.measurementUnitFacade = measurementUnitFacade;
    }

    public VtmFacade getVtmFacade() {
        return VtmFacade;
    }

    public void setVtmFacade(VtmFacade VtmFacade) {
        this.VtmFacade = VtmFacade;
    }

//    public double getLastPurchaseRate(Item item) {
//        Map m = new HashMap();
//        String sql;
//        sql = "Select bi.pharmaceuticalBillItem.purchaseRate from BillItem bi where bi.retired=false and bi.bill.cancelled=false and bi.pharmaceuticalBillItem.itemBatch.item=:i and bi.bill.billType=:t order by bi.id desc";
//        m.put("i", item);
//        m.put("t", BillType.PharmacyGrnBill);
//        return getBillItemFacade().findDoubleByJpql(sql, m);
//    }
    public double getLastPurchaseRate(Item item, Department dept) {
        //////System.out.println("getting last purchase rate");
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }

        Map m = new HashMap();
        String sql;
        sql = "Select bi.pharmaceuticalBillItem.itemBatch from BillItem bi where "
                + " bi.retired=false and bi.bill.cancelled=false "
                + " and bi.bill.department=:d "
                + " and bi.pharmaceuticalBillItem.itemBatch.item=:i "
                + " and (bi.bill.billType=:t or bi.bill.billType=:t1) "
                + " order by bi.id desc";
        m.put("i", item);
        m.put("d", dept);
        m.put("t", BillType.PharmacyGrnBill);
        m.put("t1", BillType.PharmacyPurchaseBill);
        ItemBatch ii = getItemBatchFacade().findFirstByJpql(sql, m);
        // //System.err.println("d = " + ii.getPurcahseRate());
        if (ii != null) {
            return (double) ii.getPurcahseRate();
        } else {
            return 0.0f;
        }

    }

    public double getLastPurchaseRate(Item item, Institution ins) {
        //////System.out.println("getting last purchase rate");
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }

        Map m = new HashMap();
        String sql;
        sql = "Select bi.pharmaceuticalBillItem.itemBatch from BillItem bi where "
                + " bi.retired=false and bi.bill.cancelled=false "
                + " and bi.bill.department.institution=:ins "
                + " and bi.pharmaceuticalBillItem.itemBatch.item=:i "
                + " and (bi.bill.billType=:t or bi.bill.billType=:t1) "
                + " order by bi.id desc";
        m.put("i", item);
        m.put("ins", ins);
        m.put("t", BillType.PharmacyGrnBill);
        m.put("t1", BillType.PharmacyPurchaseBill);
        ItemBatch ii = getItemBatchFacade().findFirstByJpql(sql, m);
        // //System.err.println("d = " + ii.getPurcahseRate());
        if (ii != null) {
            return ii.getPurcahseRate();
        } else {
            return 0.0;
        }

    }

    public double getLastPurchaseRate(Item item, Department dept, boolean anyValueFromHirachi) {
        double d = getLastPurchaseRate(item, dept);
        if (d == 0) {
            d = getLastPurchaseRate(item, dept.getInstitution());
        }
        if (d == 0) {
            d = getLastPurchaseRate(item);
        }
        return d;
    }

    public double getLastPurchaseRate(Item item) {
        //////System.out.println("getting last purchase rate");
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }

        Map m = new HashMap();
        String sql;
        sql = "Select bi.pharmaceuticalBillItem.itemBatch from BillItem bi where "
                + " bi.retired=false and bi.bill.cancelled=false "
                + " and bi.pharmaceuticalBillItem.itemBatch.item=:i "
                + " and (bi.bill.billType=:t or bi.bill.billType=:t1) "
                + " order by bi.id desc";
        m.put("i", item);
        m.put("t", BillType.PharmacyGrnBill);
        m.put("t1", BillType.PharmacyPurchaseBill);
        ItemBatch ii = getItemBatchFacade().findFirstByJpql(sql, m);
        // //System.err.println("d = " + ii.getPurcahseRate());
        if (ii != null) {
            return ii.getPurcahseRate();
        } else {
            return 0.0;
        }

    }

    public double getLastRetailRate(Item item, Institution ins) {
        //////System.out.println("getting last purchase rate");
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }
        Map m = new HashMap();
        String sql;
        sql = "Select bi.pharmaceuticalBillItem.itemBatch from BillItem bi where "
                + " bi.retired=false and bi.bill.cancelled=false and bi.bill.department.institution=:ins "
                + " and bi.pharmaceuticalBillItem.itemBatch.item=:i"
                + " and (bi.bill.billType=:t or bi.bill.billType=:t1) "
                + " order by bi.id desc";
        m.put("i", item);
        m.put("ins", ins);
        m.put("t", BillType.PharmacyGrnBill);
        m.put("t1", BillType.PharmacyPurchaseBill);
        ItemBatch ii = getItemBatchFacade().findFirstByJpql(sql, m);
        // //System.err.println("d = " + ii.getPurcahseRate());
        if (ii != null) {
            return ii.getRetailsaleRate();
        } else {
            return 0.0;
        }

    }

    public double getLastRetailRate(Item item) {
        //////System.out.println("getting last purchase rate");
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }
        Map m = new HashMap();
        String sql;
        sql = "Select bi.pharmaceuticalBillItem.itemBatch from BillItem bi where "
                + " bi.retired=false and bi.bill.cancelled=false "
                + " and bi.pharmaceuticalBillItem.itemBatch.item=:i"
                + " and (bi.bill.billType=:t or bi.bill.billType=:t1) "
                + " order by bi.id desc";
        m.put("i", item);
        m.put("t", BillType.PharmacyGrnBill);
        m.put("t1", BillType.PharmacyPurchaseBill);
        ItemBatch ii = getItemBatchFacade().findFirstByJpql(sql, m);
        // //System.err.println("d = " + ii.getPurcahseRate());
        if (ii != null) {
            return ii.getRetailsaleRate();
        } else {
            return 0.0;
        }

    }

    public double getLastRetailRate(Item item, Department dept) {
        //////System.out.println("getting last purchase rate");
        if (item instanceof Ampp) {
            item = ((Ampp) item).getAmp();
        }
        Map m = new HashMap();
        String sql;
        sql = "Select bi.pharmaceuticalBillItem.itemBatch from BillItem bi where "
                + " bi.retired=false and bi.bill.cancelled=false and bi.bill.department=:d "
                + " and bi.pharmaceuticalBillItem.itemBatch.item=:i"
                + " and (bi.bill.billType=:t or bi.bill.billType=:t1) "
                + " order by bi.id desc";
        m.put("i", item);
        m.put("d", dept);
        m.put("t", BillType.PharmacyGrnBill);
        m.put("t1", BillType.PharmacyPurchaseBill);
        ItemBatch ii = getItemBatchFacade().findFirstByJpql(sql, m);
        // //System.err.println("d = " + ii.getPurcahseRate());
        if (ii != null) {
            return (double) ii.getRetailsaleRate();
        } else {
            return 0.0f;
        }

    }

    public double getLastRetailRate(Item item, Department dept, boolean anyValueFromHighrachy) {
        double d = getLastRetailRate(item, dept);
        if (d == 0) {
            getLastRetailRate(item, dept.getInstitution());
        }
        if (d == 0) {
            getLastRetailRate(item);
        }
        return d;
    }

    public StockHistoryFacade getStockHistoryFacade() {
        return stockHistoryFacade;
    }

    public void setStockHistoryFacade(StockHistoryFacade stockHistoryFacade) {
        this.stockHistoryFacade = stockHistoryFacade;
    }

    public StoreItemCategoryFacade getStoreItemCategoryFacade() {
        return storeItemCategoryFacade;
    }

    public void setStoreItemCategoryFacade(StoreItemCategoryFacade storeItemCategoryFacade) {
        this.storeItemCategoryFacade = storeItemCategoryFacade;
    }

}
