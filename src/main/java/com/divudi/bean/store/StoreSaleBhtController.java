/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.store;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.inward.InwardBeanController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.Sex;
import com.divudi.data.Title;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.data.inward.SurgeryBillType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.PreBill;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.entity.pharmacy.UserStock;
import com.divudi.entity.pharmacy.UserStockContainer;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PersonFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.StockHistoryFacade;
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
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;



@Named
@SessionScoped
public class StoreSaleBhtController implements Serializable {

    /**
     * Creates a new instance of PharmacySaleController
     */
    public StoreSaleBhtController() {
    }

    @Inject
    PaymentSchemeController PaymentSchemeController;

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
/////////////////////////
    Item selectedAlternative;
    private PreBill preBill;
    Bill printBill;
    Bill bill;
    BillItem billItem;
    //BillItem removingBillItem;
    BillItem editingBillItem;
    Double qty;
    Stock stock;
    private PatientEncounter patientEncounter;
    int activeIndex;
    boolean billPreview = false;
    /////////////////
    List<Stock> replaceableStocks;
    //List<BillItem> billItems;
    List<Item> itemsWithoutStocks;
    /////////////////////////
    private UserStockContainer userStockContainer;

    private Bill batchBill;
    @Inject
    private BillBeanController billBean;

    public void selectSurgeryBillListener() {
        patientEncounter = getBatchBill().getPatientEncounter();
    }

    public void makeNull() {
        selectedAlternative = null;
        preBill = null;
        printBill = null;
        bill = null;
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        activeIndex = 0;
        billPreview = false;
        replaceableStocks = null;
        itemsWithoutStocks = null;
    }

    public double getOldQty(BillItem bItem) {
        String sql = "Select b.qty From BillItem b where b.retired=false and b.bill=:b and b=:itm";
        HashMap hm = new HashMap();
        hm.put("b", getPreBill());
        hm.put("itm", bItem);
        return getBillItemFacade().findDoubleByJpql(sql, hm);
    }

    public void onEdit(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        onEdit(tmp);
    }

    private void setZeroToQty(BillItem tmp) {
        tmp.setQty(0.0);
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0.0f);

        getStoreBean().updateUserStock(tmp.getTransUserStock(), 0);
    }

    //Check when edititng Qty
    //
    public boolean onEdit(BillItem tmp) {
        //Cheking Minus Value && Null
        if (tmp.getQty() <= 0 || tmp.getQty() == null) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);

            JsfUtil.addErrorMessage("Can not enter a minus value");
            return true;
        }

        if (tmp.getQty() > tmp.getPharmaceuticalBillItem().getStock().getStock()) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);

            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return true;
        }

        //Check Is There Any Other User using same Stock
        if (!getStoreBean().isStockAvailable(tmp.getPharmaceuticalBillItem().getStock(), tmp.getQty(), getSessionController().getLoggedUser())) {

            setZeroToQty(tmp);
            onEditCalculation(tmp);

            JsfUtil.addErrorMessage("Another User On Change Bill Item "
                    + " Qty value is resetted");
            return true;
        }

        getStoreBean().updateUserStock(tmp.getTransUserStock(), tmp.getQty());

        onEditCalculation(tmp);

        return false;
    }

    private void onEditCalculation(BillItem tmp) {
        tmp.setGrossValue(tmp.getQty() * tmp.getRate());
        tmp.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - tmp.getQty()));

        calculateBillItemForEditing(tmp);

        calTotal();

    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    public List<Stock> getReplaceableStocks() {
        return replaceableStocks;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        if (qty != null && qty <= 0) {
            JsfUtil.addErrorMessage("Can not enter a minus value");
            return;
        }
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

    public void newSaleBillWithoutReduceStock() {

        clearBill();
        clearBillItem();
        billPreview = false;

    }

    public void newSaleBill() {
        getStoreBean().retiredAllUserStockContainer(getSessionController().getLoggedUser());
        clearBill();
        clearBillItem();
        patientEncounter = null;
        billPreview = false;

    }

    public List<Item> completeRetailSaleItems(String qry) {
        Map m = new HashMap<>();
        List<Item> items;
        String sql;
        sql = "select i from Item i"
                + " where i.retired=false "
                + " and (i.name) like :n "
                + " and type(i)=:t and i.id not "
                + " in(select ibs.id from Stock ibs "
                + " where ibs.stock >:s "
                + " and ibs.department=:d "
                + " and (ibs.itemBatch.item.name) like :n )"
                + " order by i.name ";
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
        m.put("depTp", DepartmentType.Store);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i"
                    + " where i.stock >:s"
                    + " and i.department=:d "
                    + " and i.itemBatch.item.departmentType is null "
                    + " or i.itemBatch.item.departmentType!=:depTp "
                    + " and ((i.itemBatch.item.name) like :n "
                    + " or (i.itemBatch.item.code) like :n "
                    + " or (i.itemBatch.item.barcode) like :n )  "
                    + " order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i "
                    + " where i.stock >:s "
                    + " and i.department=:d"
                    + " and i.itemBatch.item.departmentType is null "
                    + " or i.itemBatch.item.departmentType!=:depTp "
                    + "  and ((i.itemBatch.item.name) like :n "
                    + " or (i.itemBatch.item.code) like :n)  "
                    + " order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        items = getStockFacade().findByJpql(sql, m, 20);
        itemsWithoutStocks = completeRetailSaleItems(qry);
//        System.out.println("Items  = " + items);
//        System.out.println("selectedSaleitems = " + itemsWithoutStocks);
        return items;
    }

    public List<Stock> completeAvailableStocksStore(String qry) {
        List<Stock> items;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("depTp", DepartmentType.Store);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i"
                    + " where i.stock >:s"
                    + " and i.department=:d "
                    + " and i.itemBatch.item.departmentType=:depTp "
                    + " and ((i.itemBatch.item.name) like :n "
                    + " or (i.itemBatch.item.code) like :n "
                    + " or (i.itemBatch.item.barcode) like :n )  "
                    + " order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i "
                    + " where i.stock >:s "
                    + " and i.department=:d"
                    + " and i.itemBatch.item.departmentType=:depTp "
                    + "  and ((i.itemBatch.item.name) like :n "
                    + " or (i.itemBatch.item.code) like :n)  "
                    + " order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        items = getStockFacade().findByJpql(sql, m, 20);
        itemsWithoutStocks = completeRetailSaleItems(qry);
//        System.out.println("Items = " + items);
//        System.out.println("selectedSaleitems = " + itemsWithoutStocks);
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

    private void savePreBillFinally(Patient pt, Department currentBhtDepartment, BillType billType, BillNumberSuffix billNumberSuffix) {
        getPreBill().setBillType(billType);
        getPreBill().setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), billType, BillClassType.PreBill, billNumberSuffix));
        getPreBill().setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), billType, BillClassType.PreBill, billNumberSuffix));

        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setPatient(pt);
        getPreBill().setPatientEncounter(getPatientEncounter());

        getPreBill().setToDepartment(null);
        getPreBill().setToInstitution(null);
        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());

        getPreBill().setFromDepartment(currentBhtDepartment);
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getBillBean().setSurgeryData(getPreBill(), getBatchBill(), SurgeryBillType.PharmacyItem);

        getBillFacade().create(getPreBill());

    }

    private void savePreBillItemsFinally(List<BillItem> list) {
        for (BillItem tbi : list) {
            if (onEdit(tbi)) {//If any issue in Stock Bill Item will not save & not include for total
                continue;
            }

            tbi.setInwardChargeType(InwardChargeType.Medicine);
            tbi.setBill(getPreBill());

            tbi.setCreatedAt(Calendar.getInstance().getTime());
            tbi.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem tmpPh = tbi.getPharmaceuticalBillItem();
            tbi.setPharmaceuticalBillItem(null);

            getBillItemFacade().create(tbi);
            getPharmaceuticalBillItemFacade().create(tmpPh);

            tbi.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(tbi);

            double qtyL = tbi.getPharmaceuticalBillItem().getQtyInUnit() + tbi.getPharmaceuticalBillItem().getFreeQtyInUnit();

            //Deduct Stock
            boolean returnFlag = getStoreBean().deductFromStock(tbi.getPharmaceuticalBillItem().getStock(),
                    Math.abs(qtyL), tbi.getPharmaceuticalBillItem(), getPreBill().getDepartment());

            if (!returnFlag) {
                tbi.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());
                getBillItemFacade().edit(tbi);
            }

            getPreBill().getBillItems().add(tbi);
        }

        getStoreBean().retiredAllUserStockContainer(getSessionController().getLoggedUser());

        calculateAllRates();

        getBillFacade().edit(getPreBill());
    }

    private boolean checkAllBillItem() {
        for (BillItem b : getPreBill().getBillItems()) {

            if (onEdit(b)) {
                return true;
            }
        }

        return false;

    }

    public void settleStoreBhtIssue() {
        settleBhtIssue(BillType.StoreBhtPre, BillNumberSuffix.STISSUE);
    }

    private void settleBhtIssue(BillType btp, BillNumberSuffix billNumberSuffix) {

        if (getPatientEncounter() == null || getPatientEncounter().getPatient() == null) {
            JsfUtil.addErrorMessage("Please Select a BHT");
            return;
        }
        
        if(getPatientEncounter().isDischarged()){
            JsfUtil.addErrorMessage("Sorry Patient is Discharged!!!");
            return;
        }

        if (checkAllBillItem()) {
            return;
        }

        Patient pt = getPatientEncounter().getPatient();
        getPreBill().setPaidAmount(0);

        List<BillItem> tmpBillItems = getPreBill().getBillItems();
        getPreBill().setBillItems(null);

        savePreBillFinally(pt, getPatientEncounter().getCurrentPatientRoom().getRoomFacilityCharge().getDepartment(), btp, billNumberSuffix);
        savePreBillItemsFinally(tmpBillItems);

        // Calculation Margin and Create Billfee 
        updateFee(getPreBill().getBillItems(),getPatientEncounter().getPaymentMethod());

        setPrintBill(getBillFacade().find(getPreBill().getId()));

        clearBill();
        clearBillItem();
        billPreview = true;

    }

    @EJB
    private BillFeeFacade billFeeFacade;
    @Inject
    private InwardBeanController inwardBean;

    public InwardBeanController getInwardBean() {
        return inwardBean;
    }

    public void setInwardBean(InwardBeanController inwardBean) {
        this.inwardBean = inwardBean;
    }

    @Inject
    PriceMatrixController priceMatrixController;

    public PriceMatrixController getPriceMatrixController() {
        return priceMatrixController;
    }

    public void setPriceMatrixController(PriceMatrixController priceMatrixController) {
        this.priceMatrixController = priceMatrixController;
    }

    public void updateFee(List<BillItem> billItems, PaymentMethod paymentMethod) {
        double total = 0;
        double netTotal = 0;
        for (BillItem bi : billItems) {
            double value = bi.getNetValue();
            BillFee issueFee = null;

            /////////////
            issueFee = getInwardBean().getIssueBillFee(bi, bi.getBill().getInstitution());
            issueFee.setBill(bi.getBill());
            issueFee.setBillItem(bi);
            issueFee.setFeeValue(Math.abs(value));

            if (issueFee.getId() != null) {
                getBillFeeFacade().edit(issueFee);
            }

            if (issueFee.getId() == null && issueFee.getFeeValue() != 0) {
                getBillFeeFacade().create(issueFee);
            }

            /////////////
            double rate = Math.abs(bi.getRate());
            PriceMatrix priceMatrix = getPriceMatrixController().fetchInwardMargin(bi, rate, bi.getBill().getFromDepartment(), paymentMethod);

            if (priceMatrix != null) {
                double margin = (Math.abs(bi.getNetValue()) * priceMatrix.getMargin()) / 100;
                issueFee.setFeeMargin(margin);
                getBillFeeFacade().edit(issueFee);
                bi.setMarginValue(margin);
            }

            bi.setAdjustedValue(Math.abs(issueFee.getTransNetValue()));
            getBillItemFacade().edit(bi);

            total += bi.getNetValue();
            netTotal += bi.getAdjustedValue();
        }

        getPreBill().setTotal(total);
        getPreBill().setNetTotal(netTotal);
        getBillFacade().edit(getPreBill());

    }

    private boolean checkItemBatch() {
        for (BillItem bItem : getPreBill().getBillItems()) {
            if (bItem.getPharmaceuticalBillItem().getStock().getId() == getBillItem().getPharmaceuticalBillItem().getStock().getId()) {
                return true;
            }
        }

        return false;
    }

    public void addBillItem() {

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

        if (checkItemBatch()) {
            JsfUtil.addErrorMessage("Already added this item batch");
            return;
        }
        //Checking User Stock Entity
        if (!getStoreBean().isStockAvailable(getStock(), getQty(), getSessionController().getLoggedUser())) {
            JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
            return;
        }

        billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - qty));
        billItem.getPharmaceuticalBillItem().setStock(stock);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        calculateBillItem();

        billItem.setInwardChargeType(InwardChargeType.Medicine);

        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setBill(getPreBill());

        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);

        //User Stock Container Save if New Bill
        getStoreBean().saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());
        UserStock us = getStoreBean().saveUserStock(billItem, getSessionController().getLoggedUser(), getUserStockContainer());

        billItem.setTransUserStock(us);

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
        int index = 0;
        for (BillItem b : getPreBill().getBillItems()) {
            if (b.isRetired()) {
                continue;
            }
            b.setSearialNo(index++);

            netTot = netTot + b.getNetValue();
            grossTot = grossTot + b.getGrossValue();
            discount = discount + b.getDiscount();
            getPreBill().setTotal(getPreBill().getTotal() + b.getNetValue());
        }
        getPreBill().setNetTotal(netTot);
        getPreBill().setTotal(grossTot);
        getPreBill().setGrantTotal(grossTot);
        getPreBill().setDiscount(discount);

    }

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    public void removeBillItem(BillItem b) {
        getStoreBean().removeUserStock(b.getTransUserStock(), getSessionController().getLoggedUser());
        getPreBill().getBillItems().remove(b.getSearialNo());

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
        billItem.getPharmaceuticalBillItem().setQtyInUnit((double) (0 - qty));

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
        //   //System.err.println("Rate "+bi.getRate());
//        bi.setDiscount(calculateBillItemDiscountRate(bi));
        //  //System.err.println("Discount "+bi.getDiscount());
        bi.setNetRate(bi.getRate() - bi.getDiscount());
        //  //System.err.println("Net "+bi.getNetRate());
    }

    private void clearBill() {
        preBill = null;
        userStockContainer = null;
    }

    private void clearBillItem() {
        billItem = null;
//        removingBillItem = null;
        editingBillItem = null;
        qty = null;
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

    public StoreBean getStoreBean() {
        return storeBean;
    }

    public void setStoreBean(StoreBean storeBean) {
        this.storeBean = storeBean;
    }

    public PreBill getPreBill() {
        if (preBill == null) {
            preBill = new PreBill();
            // preBill.setPaymentScheme(getPaymentSchemeController().getItems().get(0));
        }
        return preBill;
    }

    public void setPreBill(PreBill preBill) {
        this.preBill = preBill;
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

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
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

    public PaymentSchemeController getPaymentSchemeController() {
        return PaymentSchemeController;
    }

    public void setPaymentSchemeController(PaymentSchemeController PaymentSchemeController) {
        this.PaymentSchemeController = PaymentSchemeController;
    }

    public StockHistoryFacade getStockHistoryFacade() {
        return stockHistoryFacade;
    }

    public void setStockHistoryFacade(StockHistoryFacade stockHistoryFacade) {
        this.stockHistoryFacade = stockHistoryFacade;
    }

    public UserStockContainer getUserStockContainer() {
        if (userStockContainer == null) {
            userStockContainer = new UserStockContainer();
        }
        return userStockContainer;
    }

    public void setUserStockContainer(UserStockContainer userStockContainer) {
        this.userStockContainer = userStockContainer;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

}
