/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.ConfigOptionApplicationController;

import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.bean.store.StoreIssueController;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.UserStock;
import com.divudi.core.entity.pharmacy.UserStockContainer;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillNumber;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BillNumberFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Institution;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.TemporalType;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;

import com.divudi.core.util.CommonFunctions;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import com.divudi.service.pharmacy.PharmacyCostingService;

/**
 *
 * @author Buddhika
 */
/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class PharmacyIssueController implements Serializable {

    String errorMessage = null;
    private static final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacyIssueController() {
    }

    @Inject
    PaymentSchemeController PaymentSchemeController;

    @Inject
    SessionController sessionController;

    @Inject
    StoreIssueController storeIssueController;

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
    PharmacyBean pharmacyBean;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    BillNumberGenerator billNumberBean;
    @EJB
    private BillNumberFacade billNumberFacade;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @EJB
    private PharmacyCostingService pharmacyCostingService;
    @Inject
    private PharmacyController pharmacyController;

    @EJB
    private CashTransactionBean cashTransactionBean;
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

    int activeIndex;

    private Patient newPatient;
    private Patient searchedPatient;
    private YearMonthDay yearMonthDay;
    boolean billPreview = false;

    Department toDepartment;

    /////////////////
    List<Stock> replaceableStocks;
    //List<BillItem> billItems;
    List<Item> itemsWithoutStocks;
    /////////////////////////
    double cashPaid;
    double netTotal;
    double balance;
    Double editingQty;
    String cashPaidStr;
    ///////////////////
    private UserStockContainer userStockContainer;
    PaymentMethodData paymentMethodData;

    public static class ConfigOptionInfo {

        private final String key;
        private final String defaultValue;

        public ConfigOptionInfo(String key, String defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public String getKey() {
            return key;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }

    public void makeNull() {
        selectedAlternative = null;
        preBill = null;
        toDepartment = null;
        printBill = null;
        bill = null;
        billItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        activeIndex = 0;
        newPatient = null;
        searchedPatient = null;
        yearMonthDay = null;
        billPreview = false;
        replaceableStocks = null;
        itemsWithoutStocks = null;
        paymentMethodData = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        editingQty = null;
        cashPaidStr = null;
    }

    public Double getEditingQty() {
        return editingQty;
    }

    public void setEditingQty(Double editingQty) {
        this.editingQty = editingQty;
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

        userStockController.updateUserStock(tmp.getTransUserStock(), 0);
    }

    //Check when edititng Qty
    //
    @Inject
    UserStockController userStockController;

    public boolean onEdit(BillItem tmp) {
        //Cheking Minus Value && Null
        if (tmp.getQty() <= 0 || tmp.getQty() == null) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);

            JsfUtil.addErrorMessage("Can not enter a minus value");
            return true;
        }

        Stock fetchStock = getStockFacade().find(tmp.getPharmaceuticalBillItem().getStock().getId());

        if (tmp.getQty() > fetchStock.getStock()) {
            setZeroToQty(tmp);
            onEditCalculation(tmp);

            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return true;
        }

        //Check Is There Any Other User using same Stock
        if (!userStockController.isStockAvailable(tmp.getPharmaceuticalBillItem().getStock(), tmp.getQty(), getSessionController().getLoggedUser())) {

            setZeroToQty(tmp);
            onEditCalculation(tmp);

            JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
            return true;
        }

        userStockController.updateUserStock(tmp.getTransUserStock(), tmp.getQty());

        onEditCalculation(tmp);

        return false;
    }

    private void onEditCalculation(BillItem tmp) {
        tmp.getPharmaceuticalBillItem().setQtyInUnit(0 - tmp.getQty());
        updateFinancialsForIssue(tmp);
        calTotal();
    }

    public void editQty(BillItem bi) {
        if (bi == null) {
            //////System.out.println("No Bill Item to Edit Qty");
            return;
        }
        if (editingQty == null) {
            //////System.out.println("Editing qty is null");
            return;
        }

        bi.setQty(editingQty);
        bi.getPharmaceuticalBillItem().setQtyInUnit(0 - editingQty);
        updateFinancialsForIssue(bi);
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

    public String newSaleBillWithoutReduceStock() {
        clearBill();
        clearBillItem();
        billPreview = false;
        return "/pharmacy/pharmacy_issue";
    }

    public void resetAll() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        clearBill();
        clearBillItem();
        billPreview = false;
    }

    public List<Item> completeIssueItems(String qry) {
        Map m = new HashMap<>();
        List<Item> items;
        String sql;
        sql = "select i from Item i where i.retired=false "
                + " and (i.name) like :n and type(i)=:t "
                + " and i.id not in(select ibs.id from Stock ibs where ibs.stock >:s and ibs.department=:d and (ibs.itemBatch.item.name) like :n ) order by i.name ";
        m.put("t", Amp.class);
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("n", "%" + qry + "%");
        double s = 0.0;
        m.put("s", s);
        items = getItemFacade().findByJpql(sql, m, 10);
        return items;
    }

    List<Stock> stockList;

    public List<Stock> completeAvailableStocks(String qry) {

        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n )  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        stockList = getStockFacade().findByJpql(sql, m, 20);
        itemsWithoutStocks = completeIssueItems(qry);
        //////System.out.println("selectedSaleitems = " + itemsWithoutStocks);
        return stockList;
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

    private boolean errorCheckForPreBill() {
        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No Items added to bill to sale");
            return true;
        }
        return false;
    }

    private boolean errorCheckForSaleBill() {
        if (toDepartment == null) {
            errorMessage = "Please select a department to issue items. Bill can NOT be settled until you select the department";
            JsfUtil.addErrorMessage("Department");
            return true;
        }
        if (preBill.getComments() == null || preBill.getComments().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please Add Comment");
            return true;
        }
        if (preBill.getInvoiceNumber() == null || preBill.getInvoiceNumber().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please Fill Invoice Number");
            return true;
        }
        return false;
    }

    private void savePreBillFinally() {

        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setToDepartment(toDepartment);
        getPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        // Handle Department ID generation (independent)
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Disposal Issue - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Disposal Issue - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        } else {
            // Check legacy disposal issue configuration options
            boolean billNumberGeberationStrategyForFromDept = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Disposal Issue - Separate Bill Numbers for Logged Department", false);
            boolean billNumberGeberationStrategyForToDept = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Disposal Issue - Separate Bill Numbers for Issuing Department", false);
            boolean billNumberGeberationStrategyForFromAndToDepts = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Disposal Issue - Separate Bill Numbers for Logged and Issuing Department Combination", false);

            if (billNumberGeberationStrategyForFromDept) {
                deptId = generateBillNumberForFromDepartment();
            } else if (billNumberGeberationStrategyForToDept) {
                deptId = generateBillNumberForToDepartment();
            } else if (billNumberGeberationStrategyForFromAndToDepts) {
                deptId = generateBillNumberForFromAndToDepartmentsCorrected();
            } else {
                // Use existing method for backward compatibility
                deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(
                        sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
            }
        }

        // Handle Institution ID generation (completely separate)
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Disposal Issue - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        } else {
            // Check if any of the new prefix strategies are enabled
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Disposal Issue - Prefix + Department Code + Institution Code + Year + Yearly Number", false) ||
                configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Disposal Issue - Prefix + Institution Code + Year + Yearly Number", false)) {
                insId = deptId; // Use same number as department to avoid consuming counter twice
            } else {
                // Check legacy disposal issue configuration options
                boolean billNumberGeberationStrategyForFromDept = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Disposal Issue - Separate Bill Numbers for Logged Department", false);
                boolean billNumberGeberationStrategyForToDept = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Disposal Issue - Separate Bill Numbers for Issuing Department", false);
                boolean billNumberGeberationStrategyForFromAndToDepts = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Disposal Issue - Separate Bill Numbers for Logged and Issuing Department Combination", false);

                if (billNumberGeberationStrategyForFromDept || billNumberGeberationStrategyForToDept || billNumberGeberationStrategyForFromAndToDepts) {
                    insId = deptId; // Use same number as department for legacy strategies
                } else {
                    // Generate separate institution number for backward compatibility
                    insId = generateInstitutionBillNumberForInstitution();
                }
            }
        }

        getPreBill().setDeptId(deptId);
        getPreBill().setInsId(insId);

        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());

        if (getPreBill().getId() == null) {
            getBillFacade().create(getPreBill());
        }

    }

    @Inject
    private BillBeanController billBean;

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

            if (tbi.getId() == null) {
                getBillItemFacade().create(tbi);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            tbi.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(tbi);

            double qtyL = tbi.getPharmaceuticalBillItem().getQtyInUnit() + tbi.getPharmaceuticalBillItem().getFreeQtyInUnit();

            //Deduct Stock
            boolean returnFlag = getPharmacyBean().deductFromStock(tbi.getPharmaceuticalBillItem().getStock(),
                    Math.abs(qtyL), tbi.getPharmaceuticalBillItem(), getPreBill().getDepartment());

            if (!returnFlag) {
                tbi.setTmpQty(0);
                getPharmaceuticalBillItemFacade().edit(tbi.getPharmaceuticalBillItem());
                getBillItemFacade().edit(tbi);
            }

            getPreBill().getBillItems().add(tbi);
        }

        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        calculateAllRates();

        getBillFacade().edit(getPreBill());
    }

    private boolean checkAllBillItem() {
        if (getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items");
            return true;
        }
        for (BillItem b : getPreBill().getBillItems()) {

            if (onEdit(b)) {
                return true;
            }
        }

        return false;

    }

//    public void settlePreBill() {
//        editingQty = null;
//
//        if (checkAllBillItem()) {// Before Settle Bill Current Bills Item Check Agian There is any otheruser change his qty
//            return;
//        }
//
//        if (errorCheckForPreBill()) {
//            return;
//        }
//
//        List<BillItem> tmpBillItems = getPreBill().getBillItems();
//        getPreBill().setBillItems(null);
//
//        savePreBillFinally();
//
//        savePreBillItemsFinally(tmpBillItems);
//
//        setPrintBill(getBillFacade().find(getPreBill().getId()));
//
//        clearBill();
//        clearBillItem();
//
//        billPreview = true;
//    }

    public void settleDisposalIssueBill() {
        editingQty = null;
        errorMessage = null;
        if (toDepartment == null) {
            JsfUtil.addErrorMessage("Please select a Department");
            return;
        }
        boolean canIssueToSameDept = configOptionApplicationController
            .getBooleanValueByKey("Disposal Issue can be done for the same department", false);
        if (!canIssueToSameDept) {
            if (Objects.equals(toDepartment, sessionController.getLoggedUser().getDepartment())) {
                JsfUtil.addErrorMessage("Cannot Issue to the Same Department");
                return;
            }
        }
        if (checkAllBillItem()) {
            return;
        }

        if (errorCheckForSaleBill()) {
            return;
        }

        getPreBill().setPaidAmount(getPreBill().getTotal());
        List<BillItem> tmpBillItems = getPreBill().getBillItems();
        getPreBill().setBillItems(null);
        getPreBill().setComments(getPreBill().getComments());
        getPreBill().setInvoiceNumber(getPreBill().getInvoiceNumber());

        savePreBillFinally();
        savePreBillItemsFinally(tmpBillItems);

        setPrintBill(getBillFacade().find(getPreBill().getId()));

        clearBill();
        clearBillItem();

        billPreview = true;

    }

    private boolean checkItemBatch() {
        for (BillItem bItem : getPreBill().getBillItems()) {
            if (Objects.equals(bItem.getPharmaceuticalBillItem().getStock().getId(), getBillItem().getPharmaceuticalBillItem().getStock().getId())) {
                return true;
            }
        }
        return false;
    }

    public void addBillItem() {
        errorMessage = null;

        editingQty = null;

        if (billItem == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }

        if (getToDepartment() == null) {
            errorMessage = "Please Select To Department";
            JsfUtil.addErrorMessage("Please Select To Department");
            return;
        }

        if (getStock() == null) {
            errorMessage = "Select an item. If the item is not listed, there is no stocks from that item. Check the department you are logged and the stock.";
            JsfUtil.addErrorMessage("Please Enter Item");
            return;
        }
        if (getQty() == null) {
            errorMessage = "Please enter a quentity";
            JsfUtil.addErrorMessage("Please enter a quentity");
            return;
        }

        Stock fetchStock = getStockFacade().find(getStock().getId());

        if (getQty() > fetchStock.getStock()) {
            errorMessage = "No sufficient stocks. Please enter a quentity which is qeual or less thatn the available stock quentity.";
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
        }

        if (checkItemBatch()) {
            JsfUtil.addErrorMessage("Already added this item batch");
            return;
        }
        //Checking User Stock Entity
        if (!userStockController.isStockAvailable(getStock(), getQty(), getSessionController().getLoggedUser())) {
            errorMessage = "Sorry. Another user is already billed that item so that there is no sufficient stocks for you. Please check.";
            JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
            return;
        }

//        if (CheckDateAfterOneMonthCurrentDateTime(getStock().getItemBatch().getDateOfExpire())) {
//            errorMessage = "This batch is Expire With in 31 Days.";
//            JsfUtil.addErrorMessage("This batch is Expire With in 31 Days.");
//            return;
//        }
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        billItem.getPharmaceuticalBillItem().setQty(0 - Math.abs(qty));
        billItem.getPharmaceuticalBillItem().setStock(stock);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());

        calculateBillItem();

        billItem.setInwardChargeType(InwardChargeType.Medicine);

        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setBill(getPreBill());

        billItem.setSearialNo(getPreBill().getBillItems().size() + 1);
        getPreBill().getBillItems().add(billItem);

        //User Stock Container Save if New Bill
        userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());
        UserStock us = userStockController.saveUserStock(billItem, getSessionController().getLoggedUser(), getUserStockContainer());
        billItem.setTransUserStock(us);

        calTotal();

        clearBillItem();
        setActiveIndex(1);
    }

    public void calTotal() {
        pharmacyCostingService.calculateBillTotalsFromItemsForDisposalIssue(getPreBill(), getPreBill().getBillItems());
        BillFinanceDetails bfd = getPreBill().getBillFinanceDetails();
        if (bfd != null) {
            double pv = Optional.ofNullable(bfd.getTotalPurchaseValue()).orElse(BigDecimal.ZERO).doubleValue();
            double rv = Optional.ofNullable(bfd.getTotalRetailSaleValue()).orElse(BigDecimal.ZERO).doubleValue();
            double cv = Optional.ofNullable(bfd.getTotalCostValue()).orElse(BigDecimal.ZERO).doubleValue();
            getPreBill().getStockBill().setStockValueAtPurchaseRates(pv);
            getPreBill().getStockBill().setStockValueAsSaleRate(rv);
            getPreBill().getStockBill().setStockValueAsCostRate(cv);
        }
        setNetTotal(getPreBill().getNetTotal());
    }

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    public void removeBillItem(BillItem b) {
        userStockController.removeUserStock(b.getTransUserStock(), getSessionController().getLoggedUser());
        getPreBill().getBillItems().remove(b.getSearialNo());

        calTotal();
    }

    private BigDecimal determineIssueRate(ItemBatch itemBatch) {
        if (itemBatch == null) {
            return BigDecimal.ZERO;
        }
        boolean issueByPurchase = configOptionApplicationController.getBooleanValueByKey("Pharmacy Disposal is by Purchase Rate", true);
        boolean issueByCost = configOptionApplicationController.getBooleanValueByKey("Pharmacy Disposal is by Cost Rate", false);
        boolean issueByRetail = configOptionApplicationController.getBooleanValueByKey("Pharmacy Disposal is by Retail Rate", false);

        if (issueByPurchase) {
            return BigDecimal.valueOf(itemBatch.getPurcahseRate());
        } else if (issueByCost) {
            return BigDecimal.valueOf(itemBatch.getCostRate());
        } else if (issueByRetail) {
            return BigDecimal.valueOf(itemBatch.getRetailsaleRate());
        } else {
            return BigDecimal.valueOf(itemBatch.getPurcahseRate());
        }
    }

    private void updateFinancialsForIssue(BillItem bi) {
        if (bi == null || bi.getPharmaceuticalBillItem() == null) {
            return;
        }
        PharmaceuticalBillItem ph = bi.getPharmaceuticalBillItem();
        BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
        Item item = bi.getItem();

        if (ph.getItemBatch() == null) {
            return;
        }

        BigDecimal qty = BigDecimal.valueOf(Math.abs(bi.getQty()));
        BigDecimal unitsPerPack = BigDecimal.ONE;
        if (item instanceof Amp || item instanceof com.divudi.core.entity.pharmacy.Ampp || item instanceof com.divudi.core.entity.pharmacy.Vmpp) {
            double d = item.getDblValue();
            if (d > 0) {
                unitsPerPack = BigDecimal.valueOf(d);
            }
        }

        fd.setUnitsPerPack(unitsPerPack);
        fd.setQuantity(qty);
        fd.setTotalQuantity(qty);

        BigDecimal baseIssueRate = determineIssueRate(ph.getItemBatch());
        BigDecimal grossRate = baseIssueRate.multiply(unitsPerPack);
        // For display purposes, the issue rate should be the base rate per unit, not multiplied by units per pack
        fd.setLineGrossRate(baseIssueRate);

        BigDecimal lineGrossTotal = grossRate.multiply(qty);
        fd.setLineGrossTotal(lineGrossTotal);
        fd.setGrossTotal(lineGrossTotal);
        // Transfer value should be the issue rate * quantity
        BigDecimal transferValue = baseIssueRate.multiply(qty);
        fd.setLineNetRate(baseIssueRate);
        fd.setLineNetTotal(transferValue);
        fd.setNetTotal(transferValue);

        BigDecimal qtyByUnits = qty.multiply(unitsPerPack);
        fd.setQuantityByUnits(qtyByUnits);
        fd.setTotalQuantityByUnits(qtyByUnits);

        BigDecimal costRateBase = BigDecimal.valueOf(ph.getItemBatch().getCostRate());
        BigDecimal retailRateBase = BigDecimal.valueOf(ph.getItemBatch().getRetailsaleRate());
        BigDecimal purchaseRateBase = BigDecimal.valueOf(ph.getItemBatch().getPurcahseRate());

        // These are per-pack rates for calculations
        BigDecimal costRate = costRateBase.multiply(unitsPerPack);
        BigDecimal retailRate = retailRateBase.multiply(unitsPerPack);
        BigDecimal purchaseRate = purchaseRateBase.multiply(unitsPerPack);

        fd.setLineCostRate(costRateBase);
        fd.setLineCost(costRate.multiply(qty));
        fd.setTotalCost(costRate.multiply(qty));
        fd.setTotalCostRate(costRateBase);
        fd.setRetailSaleRate(retailRateBase);

        // Values should be: base rate * quantity (not considering units per pack for display values)
        fd.setValueAtCostRate(costRateBase.multiply(qty));
        fd.setValueAtRetailRate(retailRateBase.multiply(qty));
        fd.setValueAtPurchaseRate(purchaseRateBase.multiply(qty));

        fd.setLineDiscount(BigDecimal.ZERO);
        fd.setLineExpense(BigDecimal.ZERO);
        fd.setLineTax(BigDecimal.ZERO);
        fd.setTotalDiscount(BigDecimal.ZERO);
        fd.setTotalExpense(BigDecimal.ZERO);
        fd.setTotalTax(BigDecimal.ZERO);
        fd.setTotalCost(BigDecimal.ZERO);
        fd.setFreeQuantity(BigDecimal.ZERO);
        fd.setFreeQuantityByUnits(BigDecimal.ZERO);

        pharmacyCostingService.recalculateFinancialsBeforeAddingBillItem(fd);

        BigDecimal qtyNeg = qty.negate();
        ph.setPurchaseRate(purchaseRateBase.doubleValue());
        ph.setPurchaseRatePack(purchaseRate.doubleValue());
        ph.setRetailRate(retailRateBase.doubleValue());
        ph.setRetailRatePack(retailRate.doubleValue());
        ph.setCostRate(costRateBase.doubleValue());
        ph.setCostRatePack(costRate.doubleValue());
        ph.setPurchaseValue(purchaseRateBase.multiply(qtyNeg).doubleValue());
        ph.setRetailValue(retailRateBase.multiply(qtyNeg).doubleValue());
        ph.setCostValue(costRateBase.multiply(qtyNeg).doubleValue());

        bi.setRate(baseIssueRate.doubleValue());
        bi.setNetRate(baseIssueRate.doubleValue());
        bi.setGrossValue(transferValue.doubleValue());
        bi.setNetValue(transferValue.doubleValue());
    }

    public void calculateBillItemListner(AjaxBehaviorEvent event) {
        calculateBillItem();
    }

    public void calculateBillItemAtQtyChange(AjaxBehaviorEvent event) {
        performBillItemCalculation();
        calTotal();
    }

    public void calculateBillItem() {
        performBillItemCalculation();
    }

    private void performBillItemCalculation() {
        if (stock == null || getPreBill() == null || billItem == null || billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem().getStock() == null) {
            billItem.getPharmaceuticalBillItem().setStock(stock);
        }
        if (getQty() == null) {
            qty = 0.0;
        }
        billItem.setItem(getStock().getItemBatch().getItem());
        billItem.setQty(qty);
        billItem.getPharmaceuticalBillItem().setDoe(getStock().getItemBatch().getDateOfExpire());
        billItem.getPharmaceuticalBillItem().setFreeQty(0.0f);
        billItem.getPharmaceuticalBillItem().setItemBatch(getStock().getItemBatch());
        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        billItem.getPharmaceuticalBillItem().setQty(0 - Math.abs(qty));
        updateFinancialsForIssue(billItem);
    }

    public void calculateBillItemForEditing(BillItem bi) {
        if (getPreBill() == null || bi == null || bi.getPharmaceuticalBillItem() == null || bi.getPharmaceuticalBillItem().getStock() == null) {
            return;
        }
        bi.getPharmaceuticalBillItem().setQtyInUnit(0 - bi.getQty());
        updateFinancialsForIssue(bi);
    }

    public void handleSelect(SelectEvent event) {
        getBillItem().getPharmaceuticalBillItem().setStock(stock);
        getBillItem().getPharmaceuticalBillItem().setItemBatch(stock.getItemBatch());
        getBillItem().setItem(stock.getItemBatch().getItem());
        // Initialize with quantity 1 if no quantity is set
        if (qty == null || qty == 0.0) {
            qty = 1.0;
        }
        getBillItem().setQty(qty);
        getBillItem().getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
        getBillItem().getPharmaceuticalBillItem().setQty(0 - Math.abs(qty));
        updateFinancialsForIssue(billItem);
    }

    public void paymentSchemeChanged(AjaxBehaviorEvent ajaxBehavior) {
        calculateAllRates();
    }

    public void calculateAllRates() {
        for (BillItem tbi : getPreBill().getBillItems()) {
            updateFinancialsForIssue(tbi);
        }
        calTotal();
    }

    public void calculateRateListner(AjaxBehaviorEvent event) {

    }

    @Deprecated
    public void calculateRates(BillItem bi) {
        // Deprecated method retained for backward compatibility
    }

    private void clearBill() {
        preBill = null;
        newPatient = null;
        searchedPatient = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;

        PreBill bill = new PreBill();

        storeIssueController.setPreBill(bill);

        userStockContainer = null;
        toDepartment = null;
    }

    private void clearBillItem() {
        billItem = null;
//        removingBillItem = null;
        editingBillItem = null;
        qty = null;
        stock = null;
        editingQty = null;

    }

    public boolean CheckDateAfterOneMonthCurrentDateTime(Date date) {
        Calendar calDateOfExpiry = Calendar.getInstance();
        calDateOfExpiry.setTime(CommonFunctions.getEndOfDay(date));
        Calendar cal = Calendar.getInstance();
        cal.setTime(CommonFunctions.getEndOfDay(new Date()));
        cal.add(Calendar.DATE, 31);
        if (cal.getTimeInMillis() <= calDateOfExpiry.getTimeInMillis()) {
            return false;
        } else {
            return true;
        }
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

    public Patient getNewPatient() {
        if (newPatient == null) {
            newPatient = new Patient();
            Person p = new Person();

            newPatient.setPerson(p);
        }
        return newPatient;
    }

    public void setNewPatient(Patient newPatient) {
        this.newPatient = newPatient;
    }

    public Patient getSearchedPatient() {
        return searchedPatient;
    }

    public void setSearchedPatient(Patient searchedPatient) {
        this.searchedPatient = searchedPatient;
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
            preBill.setBillType(BillType.PharmacyDisposalIssue);
            preBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
            //   preBill.setPaymentScheme(getPaymentSchemeController().getItems().get(0));
        }
        return preBill;
    }

    public void setPreBill(PreBill preBill) {
        this.preBill = preBill;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
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

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(double cashPaid) {
        this.cashPaid = cashPaid;
    }

    public String getCashPaidStr() {
        return cashPaidStr;
    }

    public void setCashPaidStr(String cashPaidStr) {
        this.cashPaidStr = cashPaidStr;
    }

    public List<ConfigOptionInfo> getConfigOptionsForDevelopers() {
        List<ConfigOptionInfo> list = new ArrayList<>();
        list.add(new ConfigOptionInfo("Pharmacy Issue is by Purchase Rate", "true"));
        list.add(new ConfigOptionInfo("Pharmacy Issue is by Cost Rate", "false"));
        list.add(new ConfigOptionInfo("Pharmacy Issue is by Retail Rate", "false"));
        return list;
    }

    public PaymentMethodData getPaymentMethodData() {
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public void displayItemDetails(BillItem tmp) {
        getPharmacyController().fillItemDetails(tmp.getItem());
    }

    public PharmacyController getPharmacyController() {
        return pharmacyController;
    }

    private String getLockKey(String strategy, Department fromDept, Department toDept, BillTypeAtomic billType) {
        StringBuilder key = new StringBuilder(strategy);
        if (fromDept != null) {
            key.append("-from-").append(fromDept.getId());
        }
        if (toDept != null) {
            key.append("-to-").append(toDept.getId());
        }
        key.append("-").append(billType.name());
        return key.toString();
    }

    public String generateBillNumberForFromDepartment() {
        Department fromDept = getPreBill().getFromDepartment();
        if (fromDept == null) {
            return "";
        }

        String lockKey = getLockKey("FROM_DEPT", fromDept, null, BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDirectBillNumber(fromDept, null, BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE, "FROM_DEPT");
        } finally {
            lock.unlock();
        }
    }

    public String generateBillNumberForToDepartment() {
        Department toDept = getPreBill().getToDepartment();
        if (toDept == null) {
            return "";
        }

        String lockKey = getLockKey("TO_DEPT", null, toDept, BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDirectBillNumber(toDept, null, BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE, "TO_DEPT");
        } finally {
            lock.unlock();
        }
    }

    public String generateBillNumberForFromAndToDepartmentsCorrected() {
        Department fromDept = getPreBill().getFromDepartment();
        Department toDept = getPreBill().getToDepartment();
        if (fromDept == null || toDept == null) {
            return "";
        }

        String lockKey = getLockKey("FROM_TO_DEPT", fromDept, toDept, BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateDirectBillNumber(fromDept, toDept, BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE, "FROM_TO_DEPT");
        } finally {
            lock.unlock();
        }
    }

    public String generateBillNumberByFromDepartmentAndToDepartment() {
        return generateBillNumberForFromAndToDepartmentsCorrected();
    }

    public String generateInstitutionBillNumber() {
        Department dept = getSessionController().getDepartment();
        if (dept == null) {
            return "";
        }

        String lockKey = getLockKey("INST_DEPT", dept, null, BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateInstitutionBillNumberByCount(dept, BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE, false);
        } finally {
            lock.unlock();
        }
    }

    public String generateInstitutionBillNumberForInstitution() {
        Institution inst = getSessionController().getInstitution();
        if (inst == null) {
            return "";
        }

        String lockKey = getLockKey("INST_ONLY", null, null, BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());

        lock.lock();
        try {
            return generateInstitutionBillNumberByCount(inst, BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE);
        } finally {
            lock.unlock();
        }
    }

    private String generateDirectBillNumber(Department primaryDept, Department secondaryDept, BillTypeAtomic billType, String strategy) {
        BillNumber billNumber = findOrCreateBillNumber(primaryDept, secondaryDept, billType, strategy);
        Long nextNumber = getNextBillNumberByCount(primaryDept, secondaryDept, billType, strategy);

        billNumber.setLastBillNumber(nextNumber);
        billNumberFacade.edit(billNumber);

        return formatBillNumber(primaryDept, secondaryDept, nextNumber, strategy);
    }

    private String generateInstitutionBillNumberByCount(Department dept, BillTypeAtomic billType, boolean isDepartmentLevel) {
        Long count = getInstitutionBillCount(dept, billType, isDepartmentLevel);
        count = count + 1;

        String institutionCode = "";
        if (configOptionApplicationController.getBooleanValueByKey("Add the Institution Code to the Bill Number Generator", true)) {
            institutionCode = dept.getInstitution().getInstitutionCode();
        }

        String deptCode = isDepartmentLevel ? dept.getDepartmentCode() : "";
        int year = Calendar.getInstance().get(Calendar.YEAR) % 100;

        StringBuilder result = new StringBuilder();
        if (!institutionCode.isEmpty()) {
            result.append(institutionCode);
        }
        if (!deptCode.isEmpty()) {
            result.append(deptCode).append("/");
        }
        result.append("DI/").append(String.format("%02d", year))
                .append("/").append(String.format("%06d", count));

        return result.toString();
    }

    private String generateInstitutionBillNumberByCount(Institution inst, BillTypeAtomic billType) {
        Long count = getInstitutionOnlyBillCount(inst, billType);
        count = count + 1;

        String institutionCode = "";
        if (configOptionApplicationController.getBooleanValueByKey("Add the Institution Code to the Bill Number Generator", true)) {
            institutionCode = inst.getInstitutionCode();
        }

        int year = Calendar.getInstance().get(Calendar.YEAR) % 100;

        StringBuilder result = new StringBuilder();
        if (!institutionCode.isEmpty()) {
            result.append(institutionCode);
        }
        result.append("DI/").append(String.format("%02d", year))
                .append("/").append(String.format("%06d", count));

        return result.toString();
    }

    private BillNumber findOrCreateBillNumber(Department primaryDept, Department secondaryDept, BillTypeAtomic billType, String strategy) {
        String jpql = "SELECT bn FROM BillNumber bn WHERE bn.retired = false AND bn.billTypeAtomic = :billType";
        Map<String, Object> params = new HashMap<>();
        params.put("billType", billType);

        if ("FROM_DEPT".equals(strategy)) {
            jpql += " AND bn.department = :dept AND bn.toDepartment IS NULL";
            params.put("dept", primaryDept);
        } else if ("TO_DEPT".equals(strategy)) {
            jpql += " AND bn.toDepartment = :toDept AND bn.department IS NULL";
            params.put("toDept", primaryDept);
        } else if ("FROM_TO_DEPT".equals(strategy)) {
            jpql += " AND bn.department = :dept AND bn.toDepartment = :toDept";
            params.put("dept", primaryDept);
            params.put("toDept", secondaryDept);
        }

        jpql += " AND bn.billYear = :year";
        params.put("year", Calendar.getInstance().get(Calendar.YEAR));

        List<BillNumber> results = billNumberFacade.findByJpql(jpql, params);

        if (!results.isEmpty()) {
            return results.get(0);
        }

        BillNumber newBillNumber = new BillNumber();
        newBillNumber.setBillTypeAtomic(billType);
        newBillNumber.setBillYear(Calendar.getInstance().get(Calendar.YEAR));
        newBillNumber.setLastBillNumber(0L);
        newBillNumber.setRetired(false);

        if ("FROM_DEPT".equals(strategy)) {
            newBillNumber.setDepartment(primaryDept);
            newBillNumber.setInstitution(primaryDept.getInstitution());
        } else if ("TO_DEPT".equals(strategy)) {
            newBillNumber.setToDepartment(primaryDept);
            newBillNumber.setInstitution(primaryDept.getInstitution());
        } else if ("FROM_TO_DEPT".equals(strategy)) {
            newBillNumber.setDepartment(primaryDept);
            newBillNumber.setToDepartment(secondaryDept);
            newBillNumber.setInstitution(primaryDept.getInstitution());
        }

        billNumberFacade.create(newBillNumber);
        return newBillNumber;
    }

    private Long getNextBillNumberByCount(Department primaryDept, Department secondaryDept, BillTypeAtomic billType, String strategy) {
        Calendar startOfYear = Calendar.getInstance();
        startOfYear.set(Calendar.DAY_OF_YEAR, 1);
        startOfYear.set(Calendar.HOUR_OF_DAY, 0);
        startOfYear.set(Calendar.MINUTE, 0);
        startOfYear.set(Calendar.SECOND, 0);
        startOfYear.set(Calendar.MILLISECOND, 0);

        Calendar endOfYear = Calendar.getInstance();
        endOfYear.set(Calendar.MONTH, 11);
        endOfYear.set(Calendar.DAY_OF_MONTH, 31);
        endOfYear.set(Calendar.HOUR_OF_DAY, 23);
        endOfYear.set(Calendar.MINUTE, 59);
        endOfYear.set(Calendar.SECOND, 59);
        endOfYear.set(Calendar.MILLISECOND, 999);

        String sql = "SELECT COUNT(b) FROM Bill b WHERE b.retired = false AND b.billTypeAtomic = :billType AND b.billDate BETWEEN :startDate AND :endDate";
        Map<String, Object> params = new HashMap<>();
        params.put("billType", billType);
        params.put("startDate", startOfYear.getTime());
        params.put("endDate", endOfYear.getTime());

        if ("FROM_DEPT".equals(strategy)) {
            sql += " AND b.fromDepartment = :fromDept";
            params.put("fromDept", primaryDept);
        } else if ("TO_DEPT".equals(strategy)) {
            sql += " AND b.toDepartment = :toDept";
            params.put("toDept", primaryDept);
        } else if ("FROM_TO_DEPT".equals(strategy)) {
            sql += " AND b.fromDepartment = :fromDept AND b.toDepartment = :toDept";
            params.put("fromDept", primaryDept);
            params.put("toDept", secondaryDept);
        }

        return billFacade.findAggregateLong(sql, params, TemporalType.DATE) + 1;
    }

    private Long getInstitutionBillCount(Department dept, BillTypeAtomic billType, boolean isDepartmentLevel) {
        Calendar startOfYear = Calendar.getInstance();
        startOfYear.set(Calendar.DAY_OF_YEAR, 1);
        startOfYear.set(Calendar.HOUR_OF_DAY, 0);
        startOfYear.set(Calendar.MINUTE, 0);
        startOfYear.set(Calendar.SECOND, 0);
        startOfYear.set(Calendar.MILLISECOND, 0);

        Calendar endOfYear = Calendar.getInstance();
        endOfYear.set(Calendar.MONTH, 11);
        endOfYear.set(Calendar.DAY_OF_MONTH, 31);
        endOfYear.set(Calendar.HOUR_OF_DAY, 23);
        endOfYear.set(Calendar.MINUTE, 59);
        endOfYear.set(Calendar.SECOND, 59);
        endOfYear.set(Calendar.MILLISECOND, 999);

        String sql = "SELECT COUNT(b) FROM Bill b WHERE b.retired = false AND b.billTypeAtomic = :billType AND b.billDate BETWEEN :startDate AND :endDate";
        Map<String, Object> params = new HashMap<>();
        params.put("billType", billType);
        params.put("startDate", startOfYear.getTime());
        params.put("endDate", endOfYear.getTime());

        if (isDepartmentLevel) {
            sql += " AND b.department = :dept";
            params.put("dept", dept);
        } else {
            sql += " AND b.institution = :inst";
            params.put("inst", dept.getInstitution());
        }

        return billFacade.findAggregateLong(sql, params, TemporalType.DATE);
    }

    private Long getInstitutionOnlyBillCount(Institution inst, BillTypeAtomic billType) {
        Calendar startOfYear = Calendar.getInstance();
        startOfYear.set(Calendar.DAY_OF_YEAR, 1);
        startOfYear.set(Calendar.HOUR_OF_DAY, 0);
        startOfYear.set(Calendar.MINUTE, 0);
        startOfYear.set(Calendar.SECOND, 0);
        startOfYear.set(Calendar.MILLISECOND, 0);

        Calendar endOfYear = Calendar.getInstance();
        endOfYear.set(Calendar.MONTH, 11);
        endOfYear.set(Calendar.DAY_OF_MONTH, 31);
        endOfYear.set(Calendar.HOUR_OF_DAY, 23);
        endOfYear.set(Calendar.MINUTE, 59);
        endOfYear.set(Calendar.SECOND, 59);
        endOfYear.set(Calendar.MILLISECOND, 999);

        String sql = "SELECT COUNT(b) FROM Bill b WHERE b.retired = false AND b.billTypeAtomic = :billType AND b.institution = :inst AND b.billDate BETWEEN :startDate AND :endDate";
        Map<String, Object> params = new HashMap<>();
        params.put("billType", billType);
        params.put("inst", inst);
        params.put("startDate", startOfYear.getTime());
        params.put("endDate", endOfYear.getTime());

        return billFacade.findAggregateLong(sql, params, TemporalType.DATE);
    }

    private String formatBillNumber(Department primaryDept, Department secondaryDept, Long billNumber, String strategy) {
        StringBuilder result = new StringBuilder();

        String institutionCode = "";
        if (configOptionApplicationController.getBooleanValueByKey("Add the Institution Code to the Bill Number Generator", true)) {
            institutionCode = primaryDept.getInstitution().getInstitutionCode();
        }

        if (!institutionCode.isEmpty()) {
            result.append(institutionCode);
        }

        if ("FROM_DEPT".equals(strategy) || "TO_DEPT".equals(strategy)) {
            result.append(primaryDept.getDepartmentCode());
        } else if ("FROM_TO_DEPT".equals(strategy)) {
            result.append(primaryDept.getDepartmentCode()).append("-").append(secondaryDept.getDepartmentCode());
        }

        int year = Calendar.getInstance().get(Calendar.YEAR) % 100;
        result.append("/DI/").append(String.format("%02d", year))
                .append("/").append(String.format("%06d", billNumber));

        return result.toString();
    }
}
