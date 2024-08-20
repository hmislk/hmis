/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;
import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.data.dataStructure.YearMonthDay;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.PreBill;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.StockFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacyIssueControllerOld implements Serializable {

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacyIssueControllerOld() {
    }

    @Inject
    SessionController sessionController;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    StockFacade stockFacade;
    @EJB
    PharmacyBean pharmacyBean;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    Item selectedAlternative;
    private PreBill preBill;
    private Bill saleBill;
    Bill printBill;
    Bill bill;
    BillItem billItem;
    BillItem editingBillItem;
    Double qty;
    Stock stock;

    PaymentScheme paymentScheme;
    PaymentMethod paymentMethod;
    int activeIndex;

    private YearMonthDay yearMonthDay;
    private String patientTabId = "tabNewPt";
    private String strTenderedValue = "";
    boolean billPreview = false;
    /////////////////
    List<Stock> replaceableStocks;
    //List<BillItem> billItems;
    List<Item> itemsWithoutStocks;
    /////////////////////////
    private PaymentMethodData paymentMethodData;
    double cashPaid;
    double netTotal;
    double balance;
    Double editingQty;

    Department department;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Double getEditingQty() {
        return editingQty;
    }

    public void setEditingQty(Double editingQty) {
        this.editingQty = editingQty;
    }

    public void onTabChange(TabChangeEvent event) {
        setPatientTabId(event.getTab().getId());

    }

    public double getOldQty(BillItem bItem) {
        String sql = "Select b.qty From BillItem b where b.retired=false and b.bill=:b and b=:itm";
        HashMap hm = new HashMap();
        hm.put("b", getPreBill());
        hm.put("itm", bItem);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    private double getStockByBillItem(BillItem bItem) {
        String sql;
        Map m = new HashMap();
        m.put("dep", bItem.getBill().getDepartment());
        m.put("batch", bItem.getPharmaceuticalBillItem().getItemBatch());
        sql = "select i.stock from Stock i where "
                + " i.department=:dep and i.itemBatch=:batch ";
        return getStockFacade().findDoubleByJpql(sql, m);

    }

    public void onEdit(BillItem tmp) {
        if (tmp.getQty() == null) {
            return;
        }

        double oldQty = getOldQty(tmp);
        double newQty = tmp.getQty();

        //System.err.println("old " + oldQty);
        //System.err.println("new " + newQty);
        if (newQty > getStockByBillItem(tmp)) {
            tmp.setQty(oldQty);
            getBillItemFacade().edit(tmp);
            JsfUtil.addErrorMessage("No Sufficient Stocks");
            return;
        }

        if (oldQty > newQty) {
            double max = oldQty - newQty;
            //System.err.println("Max " + max);
            getPharmacyBean().addToStock(tmp.getPharmaceuticalBillItem().getStock(), Math.abs(max), tmp.getPharmaceuticalBillItem(), getSessionController().getDepartment());
        } else {
            double min = newQty - oldQty;
            //System.err.println("Min " + min);
            boolean returnFlag = getPharmacyBean().deductFromStock(tmp.getPharmaceuticalBillItem().getStock(), Math.abs(min), tmp.getPharmaceuticalBillItem(), getSessionController().getDepartment());
            if (!returnFlag) {
                tmp.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(tmp.getPharmaceuticalBillItem());
            }

        }
        tmp.setGrossValue(tmp.getQty() * tmp.getRate());

        getBillItemFacade().edit(tmp);

        calculateBillItemForEditing(tmp);

        calTotal();
    }

    public void editQty(BillItem bi) {
        if (bi == null) {
            //////// // System.out.println("No Bill Item to Edit Qty");
            return;
        }
        if (editingQty == null) {
            //////// // System.out.println("Editing qty is null");
            return;
        }

        bi.setQty(editingQty);
        calculateBillItemForEditing(bi);

        calTotal();
        editingQty = null;
    }

    public List<Stock> getReplaceableStocks() {
        return replaceableStocks;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public void setReplaceableStocks(List<Stock> replaceableStocks) {
        this.replaceableStocks = replaceableStocks;
    }

    public Item getSelectedAlternative() {
        return selectedAlternative;
    }

    public void setSelectedAlternative(Item selectedAlternative) {
        this.selectedAlternative = selectedAlternative;
    }

    public void selectReplaceableStocks() {
        if (selectedAlternative == null || !(selectedAlternative instanceof Amp)) {
            replaceableStocks = new ArrayList<>();
            return;
        }
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        Amp amp = (Amp) selectedAlternative;
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("s", d);
        m.put("vmp", amp.getVmp());
        sql = "select i from Stock i join treat(i.itemBatch.item as Amp) amp where i.stock >:s and i.department=:d and amp.vmp=:vmp order by i.itemBatch.item.name";
        replaceableStocks = getStockFacade().findByJpql(sql, m);
    }

    public List<Item> getItemsWithoutStocks() {
        return itemsWithoutStocks;
    }

    public void setItemsWithoutStocks(List<Item> itemsWithoutStocks) {
        this.itemsWithoutStocks = itemsWithoutStocks;
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
        sql = "select i from Stock i where i.stock >:s and i.department=:d and (i.itemBatch.item.name) like :n order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        items = getStockFacade().findByJpql(sql, m, 20);
        itemsWithoutStocks = completeRetailSaleItems(qry);
        //////// // System.out.println("selectedSaleitems = " + itemsWithoutStocks);
        return items;
    }

    public BillItem getBillItem() {
        if (billItem == null) {
            billItem = new BillItem();
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(billItem);
        }
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    private boolean errorCheckForPreBill() {
        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items added to bill to sale");
            return true;
        }
        return false;
    }

    @Inject
    private PaymentSchemeController paymentSchemeController;

    private boolean errorCheckForSaleBill() {

        if (getPaymentScheme() == null) {
            return true;
        }

//        if (getPaymentSchemeController().errorCheckPaymentMethod(getPaymentScheme().getPaymentMethod(), getPaymentMethodData())) {
//            return true;
//        }
//
//        if (getPaymentSchemeController().checkPaid(paymentScheme.getPaymentMethod(), getCashPaid(), getNetTotal())) {
//            return true;
//        }
        return false;
    }

    @Inject
    private BillBeanController billBean;

    private void savePreBill() {
        getPreBill().setBillDate(Calendar.getInstance().getTime());
        getPreBill().setBillTime(Calendar.getInstance().getTime());
        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());
        getPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyPre, BillClassType.PreBill, BillNumberSuffix.PHSAL));
        getPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PharmacyPre, BillClassType.PreBill, BillNumberSuffix.PHSAL));
        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
        getPreBill().setToDepartment(department);
        getPreBill().setToInstitution(department.getInstitution());
        getPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());
//        getPreBill().setPaymentMethod(paymentScheme.getPaymentMethod());
//        getPreBill().setPaymentScheme(paymentScheme);
//        getBillBean().setPaymentMethodData(getPreBill(), getPaymentScheme().getPaymentMethod(), paymentMethodData);
//        getBillFacade().edit(getPreBill());
        calculateAllRates();
    }

    private void saveSaleBill() {
        calculateAllRates();

        getSaleBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleBill().setInstitution(getSessionController().getLoggedUser().getInstitution());

        getSaleBill().setToDepartment(department);
        getSaleBill().setToInstitution(department.getInstitution());

        getSaleBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getSaleBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getSaleBill().setGrantTotal(getPreBill().getGrantTotal());
        getSaleBill().setDiscount(getPreBill().getDiscount());
        getSaleBill().setNetTotal(getPreBill().getNetTotal());
        getSaleBill().setTotal(getPreBill().getTotal());
//        getSaleBill().setRefBill(getPreBill());

        getSaleBill().setBillDate(new Date());
        getSaleBill().setBillTime(new Date());
        getSaleBill().setPaymentScheme(getPreBill().getPaymentScheme());
        getSaleBill().setReferenceBill(getPreBill());
        getSaleBill().setCreatedAt(Calendar.getInstance().getTime());
        getSaleBill().setCreater(getSessionController().getLoggedUser());
        getSaleBill().setInsId(getPreBill().getInsId());
        getSaleBill().setDeptId(getPreBill().getDeptId());

//        getBillBean().setPaymentMethodData(getSaleBill(), getPaymentScheme().getPaymentMethod(), paymentMethodData);
        if (getSaleBill().getId() == null) {
            getBillFacade().create(getSaleBill());
        }

        updatePreBill();
    }

    private void updatePreBill() {
        getPreBill().setReferenceBill(getSaleBill());
        getBillFacade().edit(getPreBill());
    }

    private void saveSaleBillItems() {
        for (BillItem tbi : getPreBill().getBillItems()) {
            BillItem newBil = new BillItem();

            newBil.copy(tbi);

            newBil.setBill(getSaleBill());
            newBil.setInwardChargeType(InwardChargeType.Medicine);
            newBil.setCreatedAt(Calendar.getInstance().getTime());
            newBil.setCreater(getSessionController().getLoggedUser());

            if (newBil.getId() == null) {
                getBillItemFacade().create(newBil);
            }

            PharmaceuticalBillItem newPhar = new PharmaceuticalBillItem();
            newPhar.copy(tbi.getPharmaceuticalBillItem());
            newPhar.setBillItem(newBil);

            if (newPhar.getId() == null) {
                getPharmaceuticalBillItemFacade().create(newPhar);
            }

            newBil.setPharmaceuticalBillItem(newPhar);
            getBillItemFacade().edit(newBil);
            getSaleBill().getBillItems().add(newBil);
        }
        getBillFacade().edit(getSaleBill());
    }

    private void savePreBillItems(BillItem tbi) {
        tbi.setInwardChargeType(InwardChargeType.Medicine);
        tbi.setBill(getPreBill());

        tbi.setCreatedAt(Calendar.getInstance().getTime());
        tbi.setCreater(getSessionController().getLoggedUser());
        PharmaceuticalBillItem tmpPharmacyItem = tbi.getPharmaceuticalBillItem();
        tmpPharmacyItem.setQty((double) (0 - tbi.getQty()));
        tbi.setPharmaceuticalBillItem(null);

        if (tbi.getId() == null) {
            getBillItemFacade().create(tbi);
        }

        if (tmpPharmacyItem.getId() == null) {
            getPharmaceuticalBillItemFacade().create(tmpPharmacyItem);
        }

        tbi.setPharmaceuticalBillItem(tmpPharmacyItem);

        tbi.setSearialNo(getPreBill().getBillItems().size() + 1);

        getBillItemFacade().edit(tbi);

        boolean returnFlag = getPharmacyBean().deductFromStock(tbi.getPharmaceuticalBillItem().getStock(), Math.abs(tbi.getPharmaceuticalBillItem().getQty()), tbi.getPharmaceuticalBillItem(), getSessionController().getDepartment());

        if (!returnFlag) {
            tbi.setTmpQty(0);
            getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());
            getBillItemFacade().edit(tbi);
        }

        getPreBill().getBillItems().add(tbi);

        getBillFacade().edit(getPreBill());
    }

    public void settlePreBill() {
        editingQty = null;
        if (errorCheckForPreBill()) {
            return;
        }
        savePreBill();
        setPrintBill(getBillFacade().find(getPreBill().getId()));

        clearBill();
        clearBillItem();

        billPreview = true;
    }

    public void settleBillWithPay() {
        editingQty = null;
        if (errorCheckForSaleBill()) {
            return;
        }
        getPreBill().setPaidAmount(getPreBill().getTotal());
        savePreBill();

        saveSaleBill();
        saveSaleBillItems();

        setPrintBill(getBillFacade().find(getSaleBill().getId()));

        clearBill();
        clearBillItem();
        billPreview = true;

    }

    public void addBillItem() {
        editingQty = null;

        if (billItem == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (getStock() == null) {
            JsfUtil.addErrorMessage("Item?");
            return;
        }
        if (getQty() == null) {
            JsfUtil.addErrorMessage("Quentity?");
            return;
        }
        if (getQty() > getStock().getStock()) {
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
        }

        billItem.getPharmaceuticalBillItem().setStock(stock);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        calculateBillItem();

        billItem.setInwardChargeType(InwardChargeType.Medicine);

        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setBill(getPreBill());

//        billItem.setSearialNo(getBillItems().size() + 1);
        //   getBillItems().add(billItem);
        //     getPreBill().getBillItems().add(billItem);
        if (getPreBill().getId() == null) {
            savePreBill();
        }

        savePreBillItems(billItem);

        calculateAllRates();

        calTotal();

        clearBillItem();
        setActiveIndex(1);
    }

    private void calTotal() {
        getPreBill().setTotal(0);
        double netTot = 0.0;
        double discount = 0.0;
        double grossTot = 0.0;
        for (BillItem b : getPreBill().getBillItems()) {
            netTot = netTot + b.getNetValue();
            grossTot = grossTot + b.getGrossValue();
            discount = discount + b.getDiscount();
            getPreBill().setTotal(getPreBill().getTotal() + b.getNetValue());
        }
        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
        getPreBill().setDiscount(discount);
        setNetTotal(getPreBill().getNetTotal());
    }

    public void removeBillItem(BillItem b) {

        getPharmacyBean().addToStock(b.getPharmaceuticalBillItem().getStock(), Math.abs(b.getQty()), b.getPharmaceuticalBillItem(), getSessionController().getDepartment());

        PharmaceuticalBillItem tmpPharmacy = b.getPharmaceuticalBillItem();
        b.setBill(null);
        b.setPharmaceuticalBillItem(null);
        getBillItemFacade().edit(b);

        tmpPharmacy.setBillItem(null);
        getPharmaceuticalBillItemFacade().edit(tmpPharmacy);

        getPreBill().getBillItems().remove(b);
        getBillFacade().edit(getPreBill());

        getBillItemFacade().remove(b);
        getPharmaceuticalBillItemFacade().remove(tmpPharmacy);

        calTotal();
    }

    public void calculateBillItemListner(AjaxBehaviorEvent event) {
        calculateBillItem();
    }

    public void calculateBillItem() {
        if (stock == null) {
            return;
        }
        if (getPreBill() == null) {
            return;
        }
        if (billItem == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            getBillItem().getPharmaceuticalBillItem().setStock(stock);
        }
        if (getQty() == null) {
            qty = 0.0;
        }

        //Bill Item
//        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setQty(qty);

        //pharmaceutical Bill Item
        billItem.getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        billItem.getPharmaceuticalBillItem().setQty((double) (double) qty);

        //Rates
        //Values
        billItem.setGrossValue(getStock().getItemBatch().getRetailsaleRate() * qty);
        billItem.setNetValue(qty * billItem.getNetRate());
        billItem.setDiscount(billItem.getGrossValue() - billItem.getNetValue());

    }

    public void calculateBillItemForEditing(BillItem bi) {
        //////// // System.out.println("calculateBillItemForEditing");
        //////// // System.out.println("bi = " + bi);
        if (getPreBill() == null || bi == null || bi.getPharmaceuticalBillItem() == null || bi.getPharmaceuticalBillItem().getStock() == null) {
            //////// // System.out.println("calculateItemForEditingFailedBecause of null");
            return;
        }
        //////// // System.out.println("bi.getQty() = " + bi.getQty());
        //////// // System.out.println("bi.getRate() = " + bi.getRate());
        bi.setGrossValue(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate() * bi.getQty());
        bi.setNetValue(bi.getQty() * bi.getNetRate());
        bi.setDiscount(bi.getGrossValue() - bi.getNetValue());
        //////// // System.out.println("bi.getNetValue() = " + bi.getNetValue());

    }

    public void handleSelect(SelectEvent event) {
        getBillItem().getPharmaceuticalBillItem().setStock(stock);
        calculateRates(billItem);
    }

    public void paymentSchemeChanged(AjaxBehaviorEvent ajaxBehavior) {
        calculateAllRates();
    }

    public void calculateAllRates() {
        //////// // System.out.println("calculating all rates");
        for (BillItem tbi : getPreBill().getBillItems()) {
            calculateRates(tbi);
            calculateBillItemForEditing(tbi);
        }
        calTotal();
    }

    public void calculateRateListner(AjaxBehaviorEvent event) {

    }

    public void calculateRates(BillItem bi) {
        //////// // System.out.println("calculating rates");
        if (bi.getPharmaceuticalBillItem().getStock() == null) {
            //////// // System.out.println("stock is null");
            return;
        }
        getBillItem();
        bi.setRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
        bi.setNetRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getPurcahseRate());
        bi.setDiscount(bi.getRate() - bi.getNetRate());
    }

    private void clearBill() {
        preBill = null;
        saleBill = null;
        patientTabId = "tabNewPt";
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        paymentMethodData = null;
    }

    private void clearBillItem() {
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        editingQty = null;
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

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
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

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public YearMonthDay getYearMonthDay() {
        if (yearMonthDay == null) {
            yearMonthDay = new YearMonthDay();
        }
        return yearMonthDay;
    }

    public void setYearMonthDay(YearMonthDay yearMonthDay) {
        this.yearMonthDay = yearMonthDay;
    }

    public PreBill getPreBill() {
        if (preBill == null) {
            preBill = new PreBill();
            preBill.setBillType(BillType.PharmacyPre);
        }
        return preBill;
    }

    public void setPreBill(PreBill preBill) {
        this.preBill = preBill;
    }

    public Bill getSaleBill() {
        if (saleBill == null) {
            saleBill = new BilledBill();
            saleBill.setBillType(BillType.PharmacySale);
        }
        return saleBill;
    }

    public void setSaleBill(Bill saleBill) {
        this.saleBill = saleBill;
    }

    public String getPatientTabId() {
        return patientTabId;
    }

    public void setPatientTabId(String patientTabId) {
        this.patientTabId = patientTabId;
    }

    public String getStrTenderedValue() {
        return strTenderedValue;
    }

    public void setStrTenderedValue(String strTenderedValue) {
        this.strTenderedValue = strTenderedValue;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(double cashPaid) {
        balance = cashPaid - netTotal;
        this.cashPaid = cashPaid;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        balance = cashPaid - netTotal;
        this.netTotal = netTotal;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isBillPreview() {
        return billPreview;
    }

    public void setBillPreview(boolean billPreview) {
        this.billPreview = billPreview;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public Bill getPrintBill() {
        return printBill;
    }

    public void setPrintBill(Bill printBill) {
        this.printBill = printBill;
    }

    public PaymentScheme getPaymentScheme() {
        //System.err.println("GEtting Paymen");
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        //System.err.println("Setting Pay");
        this.paymentScheme = paymentScheme;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController paymentSchemeController) {
        this.paymentSchemeController = paymentSchemeController;
    }

}
