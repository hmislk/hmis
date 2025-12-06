/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.store;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class StoreAdjustmentController implements Serializable {

    /**
     * Creates a new instance of PharmacySaleController
     */
    public StoreAdjustmentController() {
    }

    @Inject
    SessionController sessionController;
////////////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    StockFacade stockFacade;
    @EJB
    StoreBean storeBean;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    ItemBatchFacade itemBatchFacade;
/////////////////////////
//    Item selectedAlternative;
    private Bill deptAdjustmentPreBill;
    private Bill saleBill;
    Bill bill;
    BillItem billItem;
    BillItem removingBillItem;
    BillItem editingBillItem;

    Stock stock;

    String comment;

    private Double qty;
    private Double pr;
    private Double rsr;
    private Double wsr;

    private YearMonthDay yearMonthDay;

    List<BillItem> billItems;
    private boolean printPreview;

    public void makeNull() {
        printPreview = false;
        clearBill();
        clearBillItem();
        selectedItem = null;
        selectedItemStock = null;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public String newSaleBill() {
        clearBill();
        clearBillItem();
        return "";
    }

    public String navigateAdjustmentBillRePrint(Bill adjustmentbill) {
        bill = getBillFacade().find(adjustmentbill.getId());
        return "/store/store_reprint_adjustment?faces-redirect=true";
    }

    public List<Item> completeRetailSaleItems(String qry) {
        Map m = new HashMap<>();
        List<Item> items;
        String sql;
        sql = "select i from Item i where i.retired=false and (i.name) like :n and type(i)=:t and i.id not in(select ibs.id from Stock ibs where ibs.stock >:s and ibs.department=:d and (ibs.itemBatch.item.name) like :n ) order by i.name ";
        m.put("t", Amp.class);
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("n", "%" + qry + "%");
        double s = 0.0;
        m.put("s", s);
        items = getItemFacade().findByJpql(sql, m, 10);
        return items;
    }

    public List<Stock> completeAvailableStocks(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.stock >=:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n ) order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        items = getStockFacade().findByJpql(sql, m, 20);
        return items;
    }

    public List<Stock> completeAllStocks(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.department=:d and "
                + " ((i.itemBatch.item.name) like :n  or "
                + " (i.itemBatch.item.code) like :n  or  "
                + " (i.itemBatch.item.barcode) like :n ) ";
        items = getStockFacade().findByJpql(sql, m, 20);

        return items;
    }

    public List<Stock> completeStaffStocks(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.stock >=:s and ((i.staff.code) like :n or (i.staff.person.name) like :n or (i.itemBatch.item.name) like :n ) order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        items = getStockFacade().findByJpql(sql, m, 20);

        return items;
    }

    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(billItem);
            billItem.setPharmaceuticalBillItem(pbi);
        }

        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    private void saveAdjustmentBill(BillTypeAtomic billTypeAtomic) {
        getDeptAdjustmentPreBill().setBillDate(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setBillTime(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getDeptAdjustmentPreBill().setCreater(getSessionController().getLoggedUser());
        getDeptAdjustmentPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.StoreAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.StoreAdjustment, BillClassType.BilledBill, BillNumberSuffix.NONE));
        getDeptAdjustmentPreBill().setBillType(BillType.StoreAdjustment);
        getDeptAdjustmentPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setToDepartment(null);
        getDeptAdjustmentPreBill().setToInstitution(null);
        getDeptAdjustmentPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getDeptAdjustmentPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getDeptAdjustmentPreBill().setBillTypeAtomic(billTypeAtomic);
        getDeptAdjustmentPreBill().setComments(comment);
        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }
    }

    private PharmaceuticalBillItem saveDeptAdjustmentBillItems() {
        billItem = null;
        BillItem tbi = getBillItem();

        tbi.setPharmaceuticalBillItem(null);
        getBillItem().getPharmaceuticalBillItem().setStock(stock);

        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setQty((double) qty);

        //pharmaceutical Bill Item
        getBillItem().getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        getBillItem().getPharmaceuticalBillItem().setFreeQty(0.0f);
        getBillItem().getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());

        Stock fetchedStock = getStockFacade().find(stock.getId());

        //Adjustment Rates
        getBillItem().getPharmaceuticalBillItem().setBeforeAdjustmentValue(fetchedStock.getStock());
        getBillItem().getPharmaceuticalBillItem().setAfterAdjustmentValue(qty);
        getBillItem().getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());

        double stockQty = fetchedStock.getStock();
        double changingQty;

        changingQty = qty - stockQty;

        getBillItem().getPharmaceuticalBillItem().setQty(changingQty);

        //Rates
        //Values
        tbi.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * qty);
        tbi.setNetValue(qty * tbi.getNetRate());
        tbi.setDiscount(tbi.getGrossValue() - tbi.getNetValue());
        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setItem(getStock().getItemBatch().getItem());
        tbi.setBill(getDeptAdjustmentPreBill());
        tbi.setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());

        if (tbi.getId() == null) {
            getBillItemFacade().create(tbi);
        } else {
            getBillItemFacade().edit(tbi);
        }

        getDeptAdjustmentPreBill().getBillItems().add(tbi);
        getBillFacade().edit(getDeptAdjustmentPreBill());

        return getBillItem().getPharmaceuticalBillItem();

    }

    private PharmaceuticalBillItem savePurchaseRateAdjustmentBillItems() {
        billItem = new BillItem();

        getBillItem().setItem(getStock().getItemBatch().getItem());
        getBillItem().setRate(pr);

        getBillItem().setPharmaceuticalBillItem(null);
        ItemBatch ib = itemBatchFacade.find(getStock().getItemBatch().getId());
        getBillItem().getPharmaceuticalBillItem().setPurchaseRate(ib.getPurcahseRate());
        getBillItem().getPharmaceuticalBillItem().setRetailRate(ib.getRetailsaleRate());

        //pharmaceutical Bill Item
        getBillItem().getPharmaceuticalBillItem().setStock(stock);

        //Adjustment Rates
        getBillItem().getPharmaceuticalBillItem().setBeforeAdjustmentValue(ib.getPurcahseRate());
        getBillItem().getPharmaceuticalBillItem().setAfterAdjustmentValue(pr);
        getBillItem().getPharmaceuticalBillItem().setItemBatch(ib);

        //Values
        getBillItem().setGrossValue(getStock().getItemBatch().getRetailsaleRate() * getStock().getStock());
        getBillItem().setNetValue(getStock().getStock() * getBillItem().getNetRate());
        getBillItem().setDiscount(getBillItem().getGrossValue() - getBillItem().getNetValue());
        getBillItem().setInwardChargeType(InwardChargeType.Medicine);
        getBillItem().setItem(getStock().getItemBatch().getItem());
        getBillItem().setBill(getDeptAdjustmentPreBill());
        getBillItem().setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        getBillItem().setCreatedAt(Calendar.getInstance().getTime());
        getBillItem().setCreater(getSessionController().getLoggedUser());

        if (getBillItem().getId() == null) {
            getBillItemFacade().create(getBillItem());
        } else {
            getBillItemFacade().edit(getBillItem());
        }

        getDeptAdjustmentPreBill().getBillItems().add(getBillItem());
        getBillFacade().edit(getDeptAdjustmentPreBill());
        return getBillItem().getPharmaceuticalBillItem();
    }

    private void saveRetailSaleRateAdjustmentBillItems() {
        billItem = new BillItem();

        getBillItem().setItem(getStock().getItemBatch().getItem());
        getBillItem().setRate(rsr);

        getBillItem().setPharmaceuticalBillItem(null);
        ItemBatch itemBatch = itemBatchFacade.find(getStock().getItemBatch().getId());
        getBillItem().getPharmaceuticalBillItem().setPurchaseRate(itemBatch.getPurcahseRate());
        getBillItem().getPharmaceuticalBillItem().setRetailRate(itemBatch.getRetailsaleRate());

        //pharmaceutical Bill Item
        getBillItem().getPharmaceuticalBillItem().setStock(stock);

        //Adjustment Rates
        getBillItem().getPharmaceuticalBillItem().setBeforeAdjustmentValue(itemBatch.getPurcahseRate());
        getBillItem().getPharmaceuticalBillItem().setAfterAdjustmentValue(rsr);
        getBillItem().getPharmaceuticalBillItem().setItemBatch(itemBatch);

        //Values
        getBillItem().setGrossValue(getStock().getItemBatch().getRetailsaleRate() * getStock().getStock());
        getBillItem().setNetValue(getStock().getStock() * getBillItem().getNetRate());
        getBillItem().setDiscount(getBillItem().getGrossValue() - getBillItem().getNetValue());
        getBillItem().setInwardChargeType(InwardChargeType.Medicine);
        getBillItem().setItem(getStock().getItemBatch().getItem());
        getBillItem().setBill(getDeptAdjustmentPreBill());
        getBillItem().setSearialNo(getDeptAdjustmentPreBill().getBillItems().size() + 1);
        getBillItem().setCreatedAt(Calendar.getInstance().getTime());
        getBillItem().setCreater(getSessionController().getLoggedUser());

        if (getBillItem().getId() == null) {
            getBillItemFacade().create(getBillItem());
        } else {
            getBillItemFacade().edit(getBillItem());
        }

        getDeptAdjustmentPreBill().getBillItems().add(getBillItem());
        getBillFacade().edit(getDeptAdjustmentPreBill());
    }

    private boolean errorCheck() {
        if (getStock() == null) {
            JsfUtil.addErrorMessage("Please Select Stocke");
            return true;
        }

        if (getStock().getItemBatch() == null) {
            JsfUtil.addErrorMessage("Select Item Batch");
            return true;
        }

        return false;
    }

//   Department Stock Adjustment
    private Item selectedItem;
    private List<Stock> selectedItemStock;

    public void fillselectedItemStocks() {
        List<Stock> items = new ArrayList<>();
        if (selectedItem == null) {
            selectedItemStock = items;
            return;
        }
        String sql;
        Map m = new HashMap();
        sql = "select i "
                + " from Stock i "
                + " where i.department=:d "
                + " and i.itemBatch.item=:amp "
                + " order by i.stock desc";
        m.put("d", sessionController.getDepartment());
        m.put("amp", selectedItem);

        items = getStockFacade().findByJpql(sql, m);

        if (items != null) {
            selectedItemStock = items;
        }
    }

    public List<Stock> completeStaffStocksInStore(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        m.put("dep", DepartmentType.Store);
        sql = "select i from Stock i where i.stock !=:s and "
                + "((i.staff.code) like :n or "
                + "(i.staff.person.name) like :n or "
                + "(i.itemBatch.item.name) like :n ) "
                + " and i.itemBatch.item.departmentType=:dep "
                + "order by i.itemBatch.item.name, i.itemBatch.dateOfExpire , i.stock desc";
        items = getStockFacade().findByJpql(sql, m, 20);

        return items;
    }

    public void adjustStaffStock() {
        if (errorCheck()) {
            return;
        }
        if (qty == null) {
            JsfUtil.addErrorMessage("Add Quantity..");
            return;
        }
        if ((comment == null) || (comment.trim().equals(""))) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        saveAdjustmentBill(BillTypeAtomic.STORE_STAFF_STOCK_ADJUSTMENT);

        getDeptAdjustmentPreBill().setStaff(getStock().getStaff());
        if (getDeptAdjustmentPreBill().getId() == null) {
            getBillFacade().create(getDeptAdjustmentPreBill());
        } else {
            getBillFacade().edit(getDeptAdjustmentPreBill());
        }

        PharmaceuticalBillItem ph = saveDeptAdjustmentBillItems();
        setBill(getBillFacade().find(getDeptAdjustmentPreBill().getId()));
        getStoreBean().resetStock(ph, stock, qty, getSessionController().getDepartment());
        printPreview = true;

        JsfUtil.addSuccessMessage("Staff Stock Adjustment Successfully..");

    }

    public void adjustDepartmentStock() {

        if (errorCheck()) {
            return;
        }

        if (qty == null) {
            JsfUtil.addErrorMessage("Add Quantity..");
            return;
        }
        if ((comment == null) || (comment.trim().equals(""))) {
            JsfUtil.addErrorMessage("Add the Comment..");
            return;
        }

        saveAdjustmentBill(BillTypeAtomic.STORE_DEPAERTMENT_STOCK_ADJUSTMENT);
        PharmaceuticalBillItem ph = saveDeptAdjustmentBillItems();

        setBill(getBillFacade().find(getDeptAdjustmentPreBill().getId()));
        getStoreBean().resetStock(ph, stock, qty, getSessionController().getDepartment());

        printPreview = true;
    }

    public void adjustPurchaseRate() {
        saveAdjustmentBill(BillTypeAtomic.STORE_PURCHASE_RATE_ADJUSTMENT);
        savePurchaseRateAdjustmentBillItems();
        getStock().getItemBatch().setPurcahseRate(pr);
        getItemBatchFacade().edit(getStock().getItemBatch());

        deptAdjustmentPreBill = billFacade.find(getDeptAdjustmentPreBill().getId());

        printPreview = true;
    }

    public void adjustRetailRate() {
        saveAdjustmentBill(BillTypeAtomic.STORE_SALE_RATE_ADJUSTMENT);
        saveRetailSaleRateAdjustmentBillItems();

        getStock().getItemBatch().setRetailsaleRate(rsr);
        getItemBatchFacade().edit(getStock().getItemBatch());

        bill = billFacade.find(getDeptAdjustmentPreBill().getId());

        printPreview = true;
    }

    private void clearBill() {
        deptAdjustmentPreBill = null;
        billItems = null;
        comment = "";
    }

    private void clearBillItem() {
        billItem = null;
        removingBillItem = null;
        editingBillItem = null;
        qty = null;
        pr = null;
        rsr = null;
        wsr = null;
        stock = null;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
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

    public BillItem getRemovingBillItem() {
        return removingBillItem;
    }

    public void setRemovingBillItem(BillItem removingBillItem) {
        this.removingBillItem = removingBillItem;
    }

    public BillItem getEditingBillItem() {
        return editingBillItem;
    }

    public void setEditingBillItem(BillItem editingBillItem) {
        this.editingBillItem = editingBillItem;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public StoreBean getStoreBean() {
        return storeBean;
    }

    public void setStoreBean(StoreBean storeBean) {
        this.storeBean = storeBean;
    }

    public Bill getDeptAdjustmentPreBill() {
        if (deptAdjustmentPreBill == null) {
            deptAdjustmentPreBill = new PreBill();
            deptAdjustmentPreBill.setBillType(BillType.StoreAdjustment);
        }
        return deptAdjustmentPreBill;
    }

    public void setDeptAdjustmentPreBill(Bill deptAdjustmentPreBill) {
        this.deptAdjustmentPreBill = deptAdjustmentPreBill;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
    }

    public Double getPr() {
        return pr;
    }

    public Double getRsr() {
        return rsr;
    }

    public Double getWsr() {
        return wsr;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public void setPr(Double pr) {
        this.pr = pr;
    }

    public void setRsr(Double rsr) {
        this.rsr = rsr;
    }

    public void setWsr(Double wsr) {
        this.wsr = wsr;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public Bill getSaleBill() {
        return saleBill;
    }

    public void setSaleBill(Bill saleBill) {
        this.saleBill = saleBill;
    }

    public YearMonthDay getYearMonthDay() {
        return yearMonthDay;
    }

    public void setYearMonthDay(YearMonthDay yearMonthDay) {
        this.yearMonthDay = yearMonthDay;
    }

    private List<Stock> stk;

    public void fillSelectStock() {
        List<Stock> items = new ArrayList<>();
        if (stock == null) {
            stk = items;
            return;
        }
        String sql;
        Map<String, Object> m = new HashMap<>();

        sql = "select i "
                + " from Stock i "
                + " where i.department=:d "
                + " and i.itemBatch.item.code=:stationary "
                + " order by i.stock desc";

        m.put("d", sessionController.getDepartment());
        m.put("stationary", stock.getItemBatch().getItem().getCode());  // Assuming stk contains the item

        items = getStockFacade().findByJpql(sql, m);

        if (items != null) {
            stk = items;
        }
    }

    public List<Stock> getStk() {
        return stk;
    }

    public void setStk(List<Stock> stk) {
        this.stk = stk;
    }

//    public void fillSelectStock() {
//        List<Stock> items = new ArrayList<>();
//        if (stock == null) {
//            stk = items;
//            return;
//        }
//        String sql;
//        Map<String, Object> m = new HashMap<>();
//
//        sql = "select i "
//                + " from Stock i "
//                + " where i.department=:d "
//                + " and i.itemBatch.item.code=:stationary "
//                + " order by i.stock desc";
//
//        m.put("d", sessionController.getDepartment());
//        m.put("stationary", stock.getItemBatch().getItem().getCode());  // Assuming stk contains the item
//
//        items = getStockFacade().findByJpql(sql, m);
//
//        if (items != null) {
//            stk = items;
//        }
//    }
//
//    public List<Stock> getStk() {
//        return stk;
//    }
//
//    public void setStk(List<Stock> stk) {
//        this.stk = stk;
//    }
//    public void fillSelectStock(){
//        List<Stock> items = new ArrayList<>();
//
//        String sql;
//        Map m = new HashMap();
//        sql = "select i "
//                + " from Stock i "
//                + " where i.department=:d "
//                + " order by i.stock desc";
//        m.put("d", sessionController.getDepartment());
//
//        items = getStockFacade().findByJpql(sql, m);
//
//
//    }
    public Item getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(Item selectedItem) {
        this.selectedItem = selectedItem;
    }

    public List<Stock> getSelectedItemStock() {
        return selectedItemStock;
    }

    public void setSelectedItemStock(List<Stock> selectedItemStock) {
        this.selectedItemStock = selectedItemStock;
    }
}
