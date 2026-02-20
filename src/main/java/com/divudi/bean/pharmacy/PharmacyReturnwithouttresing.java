/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.data.dataStructure.YearMonthDay;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.service.pharmacy.PharmacyCostingService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.IssueRateMargins;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.PreBill;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.UserStock;
import com.divudi.core.entity.pharmacy.UserStockContainer;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.StockHistoryFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

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
public class PharmacyReturnwithouttresing implements Serializable {

    String errorMessage = null;

    /**
     * Creates a new instance of PharmacySaleController
     */
    public PharmacyReturnwithouttresing() {
    }

    @PostConstruct
    public void init() {
        registerPageMetadata();
    }

    @Inject
    PaymentSchemeController PaymentSchemeController;

    @Inject
    SessionController sessionController;

    @Inject
    PageMetadataRegistry pageMetadataRegistry;
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
    private PharmacyCostingService pharmacyCostingService;
/////////////////////////
    Item selectedAlternative;
    private PreBill preBill;
    Bill printBill;
    Bill bill;
    Institution toInstitution;
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
        calculateBillItemForEditing(tmp);

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

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public String newSaleBillWithoutReduceStock() {
        clearBill();
        clearBillItem();
        billPreview = false;
        return "/pharmacy/pharmacy_return_withouttresing";
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
        if (toInstitution == null) {
            errorMessage = "Please select a department to issue items. Bill can NOT be settled until you select the department";
            JsfUtil.addErrorMessage("Intitution");
            return true;
        }
        return false;
    }

    private void savePreBillFinally() {
        getPreBill().setInsId(getBillNumberBean().institutionBillNumberGeneratorByPayment(getSessionController().getInstitution(), getPreBill(), BillType.PharmacyIssue, BillNumberSuffix.DI));

        getPreBill().setDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

        getPreBill().setCreatedAt(Calendar.getInstance().getTime());
        getPreBill().setCreater(getSessionController().getLoggedUser());

        getPreBill().setToInstitution(toInstitution);

        getPreBill().setDeptId(getBillNumberBean().institutionBillNumberGeneratorByPayment(getSessionController().getDepartment(), getPreBill(), BillType.PharmacyReturnWithoutTraising, BillNumberSuffix.PHDIRRET));

        getPreBill().setBillDate(new Date());
        getPreBill().setBillTime(new Date());
        getPreBill().setFromDepartment(getSessionController().getLoggedUser().getDepartment());
        getPreBill().setFromInstitution(getSessionController().getLoggedUser().getDepartment().getInstitution());

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
        for (BillItem b : getPreBill().getBillItems()) {

            if (onEdit(b)) {
                return true;
            }
        }

        return false;

    }

    public void settlePreBill() {
        editingQty = null;

        if (checkAllBillItem()) {// Before Settle Bill Current Bills Item Check Agian There is any otheruser change his qty
            return;
        }

        if (errorCheckForPreBill()) {
            return;
        }

        List<BillItem> tmpBillItems = getPreBill().getBillItems();
        getPreBill().setBillItems(null);

        savePreBillFinally();

        savePreBillItemsFinally(tmpBillItems);

        setPrintBill(getBillFacade().find(getPreBill().getId()));

        clearBill();
        clearBillItem();

        billPreview = true;
    }

    @EJB
    private CashTransactionBean cashTransactionBean;

    public void settleBill() {

        editingQty = null;
        //   ////System.out.println("editingQty = " + editingQty);
        errorMessage = null;
        //   ////System.out.println("errorMessage = " + errorMessage);
        if (toInstitution == null) {
            JsfUtil.addErrorMessage("Select a Supplier");
            return;
        }
        if (getPreBill().getComments() == null || getPreBill().getComments().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }

        if (getPreBill().getBillItems() == null || getPreBill().getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items");
            return;
        }

        if (checkAllBillItem()) {
            //   ////System.out.println("Check all bill Ietems");
            return;
        }

        if (errorCheckForSaleBill()) {
            //   ////System.out.println("Error for sale bill");
            return;
        }

        calculateAllRates();

        getPreBill().setPaidAmount(getPreBill().getTotal());
        //   ////System.out.println("getPreBill().getPaidAmount() = " + getPreBill().getPaidAmount());

        // Create financial details for proper cost accounting
        createFinanceDetailsForReturnItems();

        List<BillItem> tmpBillItems = getPreBill().getBillItems();
        getPreBill().setBillItems(null);

        savePreBillFinally();
        savePreBillItemsFinally(tmpBillItems);
        getPreBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETURN_WITHOUT_TREASING);
        getPreBill().setCompleted(true);
        getPreBill().setApproveAt(new Date());
        getPreBill().setApproveUser(getSessionController().getLoggedUser());
        getBillFacade().edit(getPreBill());

        setPrintBill(getBillFacade().find(getPreBill().getId()));

        clearBill();
        clearBillItem();

        billPreview = true;

    }

    /**
     * Creates BillItemFinanceDetails and BillFinanceDetails for return without tracing.
     * Follows cost accounting sign conventions for proper inventory valuation.
     */
    private void createFinanceDetailsForReturnItems() {
        if (getPreBill() == null || getPreBill().getBillItems() == null) {
            return;
        }

        // Step 1: Create BIFD for each item
        for (BillItem billItem : getPreBill().getBillItems()) {
            if (!billItem.isRetired()) {
                createBillItemFinanceDetails(billItem);
            }
        }

        // Step 2: Create BFD at bill level
        createBillFinanceDetails();
    }

    /**
     * Creates BillItemFinanceDetails for a single return item.
     * Applies negative cost accounting for stock-out transactions.
     */
    private void createBillItemFinanceDetails(BillItem billItem) {
        PharmaceuticalBillItem phi = billItem.getPharmaceuticalBillItem();
        if (phi == null || phi.getStock() == null || phi.getStock().getItemBatch() == null) {
            return;
        }

        // Initialize BIFD (auto-creates if null)
        billItem.initializeBillItemFinanceDetails();
        BillItemFinanceDetails bifd = billItem.getBillItemFinanceDetails();

        // Set audit information
        bifd.setCreatedAt(new Date());
        bifd.setCreatedBy(getSessionController().getLoggedUser());

        // Calculate units per pack using PharmacyCostingService
        pharmacyCostingService.calculateUnitsPerPack(bifd);

        // Set quantities from PharmaceuticalBillItem (negative for returns)
        pharmacyCostingService.addBillItemFinanceDetailQuantitiesFromPharmaceuticalBillItem(phi, bifd);

        // Extract and set rates from ItemBatch (positive values)
        ItemBatch batch = phi.getStock().getItemBatch();
        bifd.setCostRate(BigDecimal.valueOf(batch.getCostRate() != null ? batch.getCostRate() : batch.getPurcahseRate()));
        bifd.setPurchaseRate(BigDecimal.valueOf(batch.getPurcahseRate()));
        bifd.setRetailSaleRate(BigDecimal.valueOf(batch.getRetailsaleRate()));

        // Set line rates (using purchase rate for return pricing)
        bifd.setLineGrossRate(bifd.getPurchaseRate());

        // Apply comprehensive line total calculation
        calculateLineTotal(billItem);
    }

    /**
     * Creates and populates BillFinanceDetails with aggregated values.
     */
    private void createBillFinanceDetails() {
        // Initialize BFD if not exists
        if (getPreBill().getBillFinanceDetails() == null) {
            BillFinanceDetails bfd = new BillFinanceDetails();
            bfd.setBill(getPreBill());
            getPreBill().setBillFinanceDetails(bfd);
        }

        BillFinanceDetails bfd = getPreBill().getBillFinanceDetails();

        // Aggregate totals from all bill items
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue = BigDecimal.ZERO;
        BigDecimal totalRetailValue = BigDecimal.ZERO;

        for (BillItem billItem : getPreBill().getBillItems()) {
            if (billItem.isRetired() || billItem.getBillItemFinanceDetails() == null) {
                continue;
            }

            BillItemFinanceDetails bifd = billItem.getBillItemFinanceDetails();

            // Aggregate revenue (positive)
            if (bifd.getGrossTotal() != null) {
                totalGross = totalGross.add(bifd.getGrossTotal());
            }

            // Aggregate stock valuations (negative for returns)
            if (bifd.getValueAtCostRate() != null) {
                totalCostValue = totalCostValue.add(bifd.getValueAtCostRate());
            }
            if (bifd.getValueAtPurchaseRate() != null) {
                totalPurchaseValue = totalPurchaseValue.add(bifd.getValueAtPurchaseRate());
            }
            if (bifd.getValueAtRetailRate() != null) {
                totalRetailValue = totalRetailValue.add(bifd.getValueAtRetailRate());
            }
        }

        // Set aggregated values in BFD
        bfd.setGrossTotal(totalGross);
        bfd.setNetTotal(totalGross);
        bfd.setTotalCostValue(totalCostValue);
        bfd.setTotalPurchaseValue(totalPurchaseValue);
        bfd.setTotalRetailSaleValue(totalRetailValue);
    }

    /**
     * Comprehensive line total calculation for return without tracing items.
     * Follows DirectPurchaseReturnWorkflowController pattern for consistency.
     */
    private void calculateLineTotal(BillItem bi) {
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }

        BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
        PharmaceuticalBillItem phi = bi.getPharmaceuticalBillItem();

        // Get current quantities and rates
        BigDecimal qty = fd.getQuantity();
        BigDecimal freeQty = fd.getFreeQuantity();
        BigDecimal rate = fd.getLineGrossRate();

        // Null safety
        if (qty == null) qty = BigDecimal.ZERO;
        if (freeQty == null) freeQty = BigDecimal.ZERO;
        if (rate == null) rate = BigDecimal.ZERO;

        // Use absolute values for calculation (quantities are already negative)
        qty = qty.abs();
        freeQty = freeQty.abs();

        // Calculate total quantity and line total
        BigDecimal totalQty = qty.add(freeQty);
        BigDecimal lineTotal = totalQty.multiply(rate);

        // Set total quantity (negative for returns - stock moving out)
        fd.setTotalQuantity(totalQty.abs().negate());

        // Set financial totals (positive for refund amounts)
        fd.setLineGrossTotal(lineTotal);
        fd.setLineNetTotal(lineTotal);

        // Calculate line net rate
        if (totalQty.compareTo(BigDecimal.ZERO) > 0) {
            fd.setLineNetRate(lineTotal.divide(totalQty, 4, RoundingMode.HALF_UP));
        } else {
            fd.setLineNetRate(BigDecimal.ZERO);
        }

        // Set bill-level rates and totals in BIFD
        fd.setGrossRate(rate);
        fd.setNetRate(fd.getLineNetRate());
        fd.setGrossTotal(lineTotal);
        fd.setNetTotal(lineTotal);

        // Set BillItem legacy fields (matching BIFD values)
        bi.setRate(rate.doubleValue());
        bi.setQty(qty.doubleValue());
        bi.setNetRate(fd.getLineNetRate() != null ? fd.getLineNetRate().doubleValue() : 0.0);
        bi.setGrossValue(lineTotal.doubleValue());
        bi.setNetValue(lineTotal.doubleValue());

        // Calculate unit-based quantities
        boolean isAmpp = bi.getItem() instanceof Ampp;
        BigDecimal unitsPerPack = fd.getUnitsPerPack() != null ? fd.getUnitsPerPack() : BigDecimal.ONE;
        BigDecimal qtyByUnits = isAmpp ? qty.multiply(unitsPerPack) : qty;
        BigDecimal freeQtyByUnits = isAmpp ? freeQty.multiply(unitsPerPack) : freeQty;
        BigDecimal totalQtyByUnits = qtyByUnits.add(freeQtyByUnits);

        // Update BIFD quantities (negative for returns - stock moving out)
        fd.setQuantity(qty.abs().negate());
        fd.setFreeQuantity(freeQty.abs().negate());
        fd.setQuantityByUnits(qtyByUnits.abs().negate());
        fd.setFreeQuantityByUnits(freeQtyByUnits.abs().negate());
        fd.setTotalQuantityByUnits(totalQtyByUnits.abs().negate());

        // Set cost rate fields for consistency
        if (fd.getCostRate() != null) {
            fd.setLineCostRate(fd.getCostRate());
            fd.setBillCostRate(fd.getCostRate());
            fd.setTotalCostRate(fd.getCostRate());

            // Calculate cost values (negative for returns)
            BigDecimal totalCostValue = fd.getCostRate().multiply(totalQtyByUnits.abs());
            fd.setLineCost(totalCostValue.negate());
            fd.setBillCost(totalCostValue.negate());
            fd.setTotalCost(totalCostValue.negate());
        }

        // Set BIFD value fields for aggregation by createBillFinanceDetails()
        // Calculate stock valuation values (NEGATIVE for returns)
        if (fd.getCostRate() != null) {
            BigDecimal costValue = fd.getCostRate().multiply(totalQtyByUnits.abs()).negate();
            fd.setValueAtCostRate(costValue);
        }
        if (fd.getPurchaseRate() != null) {
            BigDecimal purchaseValue = fd.getPurchaseRate().multiply(totalQtyByUnits.abs()).negate();
            fd.setValueAtPurchaseRate(purchaseValue);
        }
        if (fd.getRetailSaleRate() != null) {
            BigDecimal retailValue = fd.getRetailSaleRate().multiply(totalQtyByUnits.abs()).negate();
            fd.setValueAtRetailRate(retailValue);
        }

        // Set PharmaceuticalBillItem stock values (negative for returns)
        if (phi != null) {
            double totalQtyInUnits = totalQtyByUnits.doubleValue();
            if (fd.getPurchaseRate() != null) {
                phi.setPurchaseValue(-Math.abs(totalQtyInUnits * fd.getPurchaseRate().doubleValue()));
            }
            if (fd.getCostRate() != null) {
                phi.setCostValue(-Math.abs(totalQtyInUnits * fd.getCostRate().doubleValue()));
                phi.setCostRate(fd.getCostRate().doubleValue());
            }
            if (fd.getRetailSaleRate() != null) {
                phi.setRetailValue(-Math.abs(totalQtyInUnits * fd.getRetailSaleRate().doubleValue()));
                phi.setRetailRate(fd.getRetailSaleRate().doubleValue());
            }
        }


        // Set zero-value fields (no discounts, taxes, or expenses on returns without tracing)
        fd.setLineDiscount(BigDecimal.ZERO);
        fd.setLineTax(BigDecimal.ZERO);
        fd.setLineExpense(BigDecimal.ZERO);
        fd.setBillDiscount(BigDecimal.ZERO);
        fd.setBillTax(BigDecimal.ZERO);
        fd.setBillExpense(BigDecimal.ZERO);
        fd.setTotalDiscount(BigDecimal.ZERO);
        fd.setTotalTax(BigDecimal.ZERO);
        fd.setTotalExpense(BigDecimal.ZERO);
    }

    private boolean checkItemBatch() {
        if (getPreBill() == null || getPreBill().getBillItems() == null || getBillItem() == null
                || getBillItem().getPharmaceuticalBillItem() == null
                || getBillItem().getPharmaceuticalBillItem().getStock() == null) {
            return false;
        }

        Long targetStockId = getBillItem().getPharmaceuticalBillItem().getStock().getId();

        for (BillItem bItem : getPreBill().getBillItems()) {
            if (bItem == null || bItem.getPharmaceuticalBillItem() == null
                    || bItem.getPharmaceuticalBillItem().getStock() == null) {
                continue;
            }
            if (Objects.equals(bItem.getPharmaceuticalBillItem().getStock().getId(), targetStockId)) {
                return true;
            }
        }

        return false;
    }

//    @EJB
//    IssueRateMarginsFacade issueRateMarginsFacade;
    public void addBillItem() {
        errorMessage = null;

        editingQty = null;

        if (billItem == null) {
            return;
        }
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }



        //IssueRateMargins issueRateMargins = pharmacyBean.fetchIssueRateMargins(sessionController.getDepartment(), getToDepartment());
//        if (issueRateMargins == null) {
//            JsfUtil.addErrorMessage("Set Issue Margin");
//            return;
//        }
        if (getStock() == null) {
            errorMessage = "Select an item. If the item is not listed, there is no stocks from that item. Check the department you are logged and the stock.";
            JsfUtil.addErrorMessage("Item?");
            return;
        }
        if (getQty() == null) {
            errorMessage = "Please enter a quentity";
            JsfUtil.addErrorMessage("Quentity?");
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

        billItem.getPharmaceuticalBillItem().setQtyInUnit(0 - qty);
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

        calculateAllRates();

        calTotal();

        clearBillItem();
        setActiveIndex(1);
    }

    public void calTotal() {
        getPreBill().setTotal(0);
        double netTot = 0.0;
//        double discount = 0.0;
        double grossTot = 0.0;
        double retailValue = 0.0;
        //double margin = 0;
        int index = 0;
        for (BillItem b : getPreBill().getBillItems()) {
            if (b.isRetired()) {
                continue;
            }
            b.setSearialNo(index++);

            netTot = netTot + b.getNetValue();
            grossTot = grossTot + b.getGrossValue();
//            discount = discount + b.getDiscount();
            retailValue = retailValue + b.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate() * b.getPharmaceuticalBillItem().getQty();
            //margin += b.getMarginValue();

        }

        netTot = netTot + getPreBill().getServiceCharge();

        getPreBill().setTotal(grossTot);

        getPreBill().setNetTotal(netTot - Math.abs(getPreBill().getDiscount()));

        //getPreBill().setMargin(margin);
//        getPreBill().setDiscount(discount);
        getPreBill().getPharmacyBill().setSaleValue(retailValue);
        getPreBill().getPharmacyBill().setPurchaseValue(netTot);
        setNetTotal(getPreBill().getNetTotal());

    }

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    public void removeBillItem(BillItem b) {
        userStockController.removeUserStock(b.getTransUserStock(), getSessionController().getLoggedUser());
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
        billItem.getPharmaceuticalBillItem().setQty(0 - qty);

        //Rates
        //Values
        billItem.setGrossValue(billItem.getRate() * qty);
        billItem.setDiscount(0);
        //billItem.setMarginValue(billItem.getMarginRate() * qty);
        billItem.setNetValue(billItem.getNetRate() * qty);

    }

    public void calculateBillItemForEditing(BillItem bi) {
        //////System.out.println("calculateBillItemForEditing");
        //////System.out.println("bi = " + bi);
        if (getPreBill() == null || bi == null || bi.getPharmaceuticalBillItem() == null || bi.getPharmaceuticalBillItem().getStock() == null) {
            //////System.out.println("calculateItemForEditingFailedBecause of null");
            return;
        }

        bi.setGrossValue(bi.getQty() * bi.getRate());
        //bi.setMarginValue(bi.getQty() * bi.getMarginRate());
        bi.setNetValue(bi.getQty() * bi.getNetRate());

    }

    public void handleSelect(SelectEvent event) {
        System.out.println("handleSelect method called!");
        if (event == null || event.getObject() == null) {
            return;
        }

        // Get the selected stock from the event
        stock = (Stock) event.getObject();
        System.out.println("Selected stock: " + stock.getItemBatch().getItem().getName());

        // Set the stock and itemBatch for proper rate display
        getBillItem().getPharmaceuticalBillItem().setStock(stock);
        getBillItem().getPharmaceuticalBillItem().setItemBatch(stock.getItemBatch());

        // Set the item for the bill item
        getBillItem().setItem(stock.getItemBatch().getItem());

        // Calculate rates first to set purchase rate
        calculateRates(billItem);

        // If quantity is set, calculate the value immediately
        if (qty != null && qty > 0) {
            calculateBillItem();
        }
    }

    public void paymentSchemeChanged(AjaxBehaviorEvent ajaxBehavior) {
        calculateAllRates();
    }

    public void calculateAllRates() {
        //////System.out.println("calculating all rates");
        for (BillItem tbi : getPreBill().getBillItems()) {
            calculateRates(tbi);
            calculateBillItemForEditing(tbi);
        }
        calTotal();
    }

    public void calculateRateListner(AjaxBehaviorEvent event) {

    }

    public void calculateRates(BillItem bi) {
        //   ////System.out.println("calculating rates");
        if (bi.getPharmaceuticalBillItem().getStock() == null) {
            ////System.out.println("stock is unavailable");
            return;
        }

        //IssueRateMargins issueRateMargins = pharmacyBean.fetchIssueRateMargins(sessionController.getDepartment(), getToDepartment());
//        if (issueRateMargins == null) {
//            JsfUtil.addErrorMessage("Please select to department");
//            return;
//        }
        //if (issueRateMargins.isAtPurchaseRate()) {
        bi.setRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getPurcahseRate());
        //} else {
        //bi.setRate(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate());
        //}

        //bi.setMarginRate(calculateBillItemAdditionToPurchaseRate(bi, issueRateMargins));
        bi.setDiscount(0.0);
        //bi.setNetRate(bi.getRate() + bi.getMarginRate());
        bi.setNetRate(bi.getRate());
    }

    public double calculateBillItemAdditionToPurchaseRate(BillItem bi, IssueRateMargins issueRateMargins) {
        //////System.out.println("bill item discount rate");
        //////System.out.println("getPaymentScheme() = " + getPaymentScheme());
        if (bi == null) {
            //////System.out.println("bi is null");
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem() == null) {
            //////System.out.println("pi is null");
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem().getStock() == null) {
            //////System.out.println("stock is null");
            return 0.0;
        }
        if (bi.getPharmaceuticalBillItem().getStock().getItemBatch() == null) {
            //////System.out.println("batch is null");
            return 0.0;
        }
        bi.setItem(bi.getPharmaceuticalBillItem().getStock().getItemBatch().getItem());
        double tr = bi.getRate();
        //  //System.err.println("tr = " + tr);
        double tdp;
        if (getToDepartment() != null) {
            tdp = issueRateMargins.getRateForPharmaceuticals();
        } else {
            tdp = 0;
        }
        double dr;
        dr = (tr * tdp) / 100;

//        if (bi.getItem().isDiscountAllowed()) {
        return dr;
//        } else {
//            return 0;
//        }
    }

    private void clearBill() {
        preBill = null;
        newPatient = null;
        searchedPatient = null;
        cashPaid = 0;
        netTotal = 0;
        balance = 0;
        userStockContainer = null;
        toDepartment = null;
        toInstitution = null;
    }

    private void clearBillItem() {
        billItem = null;
//        removingBillItem = null;
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
            preBill.setBillType(BillType.PharmacyReturnWithoutTraising);
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

    /**
     * Register page metadata for the admin configuration interface
     */
    private void registerPageMetadata() {
        if (pageMetadataRegistry == null) {
            return;
        }

        PageMetadata metadata = new PageMetadata();
        metadata.setPagePath("pharmacy/pharmacy_return_withouttresing");
        metadata.setPageName("Pharmacy Return Without Tracing");
        metadata.setDescription("Return pharmacy items without tracing the original issue");
        metadata.setControllerClass("PharmacyReturnwithouttresing");

        // Column visibility configuration options
        metadata.addConfigOption(new ConfigOptionInfo(
            "ui.pharmacy_return_withouttresing.columns.visibility",
            "JSON configuration for column visibility settings on the pharmacy return without tracing page",
            OptionScope.USER
        ));

        // Register individual column configurations for UI components
        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Return Without Tracing Item Visible",
            "Controls visibility of item column in pharmacy return without tracing table",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Return Without Tracing Qty Visible",
            "Controls visibility of quantity column in pharmacy return without tracing table",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Return Without Tracing Return Rate Visible",
            "Controls visibility of return rate column in pharmacy return without tracing table",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Return Without Tracing Return Value Visible",
            "Controls visibility of return value column in pharmacy return without tracing table",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Return Without Tracing Retail Rate Visible",
            "Controls visibility of retail rate column in pharmacy return without tracing table",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Return Without Tracing Retail Value Visible",
            "Controls visibility of retail value column in pharmacy return without tracing table",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Return Without Tracing Expiry Visible",
            "Controls visibility of expiry date column in pharmacy return without tracing table",
            OptionScope.APPLICATION
        ));

        // Bill Number Suffix Configuration for PHARMACY_RETURN_WITHOUT_TREASING
        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Suffix for PHARMACY_RETURN_WITHOUT_TREASING",
            "Custom suffix to append to pharmacy return without tracing bill numbers (used by BillNumberGenerator.institutionBillNumberGeneratorByPayment)",
            OptionScope.APPLICATION
        ));

        pageMetadataRegistry.registerPage(metadata);
    }

    public PaymentMethodData getPaymentMethodData() {
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

}
