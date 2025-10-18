/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.UserStockContainer;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.ItemBatch;
import java.math.BigDecimal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller for Direct Department Transfer Issues
 * Handles pharmacy transfers issued directly to departments without a prior request
 *
 * @author safrin
 */
@Named("transferIssueDirectController")
@SessionScoped
public class TransferIssueDirectController implements Serializable {

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillNumberGenerator billNumberBean;
    @Inject
    private UserStockController userStockController;
    @Inject
    private SessionController sessionController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    private Bill issuedBill;
    private boolean printPreview;

    private List<BillItem> billItems;
    private BillItem billItem;
    private Double qty;
    private Stock tmpStock;
    private StockDTO stockDto;
    UserStockContainer userStockContainer;

    public TransferIssueDirectController() {
    }

    /**
     * Navigation method for direct pharmacy issue
     */
    public String navigateToDirectPharmacyIssue() {
        createDirectIssueBillItems();
        getIssuedBill().setFromDepartment(getSessionController().getDepartment());
        return "/pharmacy/pharmacy_transfer_issue_direct_department?faces-redirect=true";
    }

    /**
     * Creates and initializes a new direct issue transaction
     */
    public void createDirectIssueBillItems() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        makeNull();
    }

    /**
     * Adds a new bill item to the direct issue
     */
    public void addNewBillItem() {
        billItem = new BillItem();
        if (getTmpStock() == null) {
            JsfUtil.addErrorMessage("Item?");
            return;
        }
        if (getQty() == null) {
            JsfUtil.addErrorMessage("Quantity?");
            return;
        }
        if (getQty() == 0.0) {
            JsfUtil.addErrorMessage("Quentity Zero?");
            return;
        }
        if (getQty() > getTmpStock().getStock()) {
            JsfUtil.addErrorMessage("No Sufficient Stocks?");
            return;
        }
        if (!userStockController.isStockAvailable(getTmpStock(), getQty(), getSessionController().getLoggedUser())) {
            JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
            return;
        }

        if (billItem.getBillItemFinanceDetails().getUnitsPerPack() == null || billItem.getBillItemFinanceDetails().getUnitsPerPack() == BigDecimal.ZERO) {
            if (billItem.getItem() instanceof Ampp) {
                billItem.getBillItemFinanceDetails().setUnitsPerPack(BigDecimal.valueOf(billItem.getItem().getDblValue()));
            } else {
                billItem.getBillItemFinanceDetails().setUnitsPerPack(BigDecimal.ONE);
            }
        }

        billItem.getPharmaceuticalBillItem().setQty(qty);
        billItem.getPharmaceuticalBillItem().setStock(getTmpStock());
        billItem.getPharmaceuticalBillItem().setItemBatch(getTmpStock().getItemBatch());

        billItem.getPharmaceuticalBillItem().setPurchaseRate(getTmpStock().getItemBatch().getPurcahseRate());
        billItem.getPharmaceuticalBillItem().setPurchaseRatePack(getTmpStock().getItemBatch().getPurcahseRate() * billItem.getBillItemFinanceDetails().getUnitsPerPack().doubleValue());

        billItem.getPharmaceuticalBillItem().setRetailRate(getTmpStock().getItemBatch().getRetailsaleRate());
        billItem.getPharmaceuticalBillItem().setRetailRatePack(getTmpStock().getItemBatch().getRetailsaleRate() * billItem.getBillItemFinanceDetails().getUnitsPerPack().doubleValue());

        billItem.getPharmaceuticalBillItem().setCostRate(getTmpStock().getItemBatch().getCostRate());
        billItem.getPharmaceuticalBillItem().setCostRatePack(getTmpStock().getItemBatch().getCostRate() * billItem.getBillItemFinanceDetails().getUnitsPerPack().doubleValue());

        billItem.getPharmaceuticalBillItem().setCostValue(billItem.getPharmaceuticalBillItem().getCostRate() * billItem.getPharmaceuticalBillItem().getQty());
        billItem.getPharmaceuticalBillItem().setRetailValue(billItem.getPharmaceuticalBillItem().getRetailRate() * billItem.getPharmaceuticalBillItem().getQty());
        billItem.getPharmaceuticalBillItem().setPurchaseValue(billItem.getPharmaceuticalBillItem().getPurchaseRate() * billItem.getPharmaceuticalBillItem().getQty());

        billItem.setItem(getTmpStock().getItemBatch().getItem());
        billItem.setQty(qty);

        billItem.setSearialNo(getBillItems().size());

        // Set the transfer rate based on configuration
        BigDecimal transferRate = determineTransferRate(getTmpStock().getItemBatch());
        billItem.getBillItemFinanceDetails().setLineGrossRate(transferRate.multiply(billItem.getBillItemFinanceDetails().getUnitsPerPack()));

        UserStockContainer usc = userStockController.saveUserStockContainer(getUserStockContainer(), getSessionController().getLoggedUser());

        billItem.setTransUserStock(userStockController.saveUserStock(billItem, getSessionController().getLoggedUser(), usc));

        getBillItems().add(billItem);

        calculateBillTotalsForTransferIssue(getIssuedBill());

        qty = null;
        stockDto = null;
        tmpStock = null;
    }

    /**
     * Settles the direct issue transaction
     */
    public void settleDirectIssue() {
        if (getIssuedBill().getToDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select Department to Issue");
            return;
        }
        if (getIssuedBill().getFromDepartment() == null) {
            getIssuedBill().setFromDepartment(sessionController.getDepartment());
        }
        if (Objects.equals(getIssuedBill().getFromDepartment().getId(),
                getIssuedBill().getToDepartment().getId())) {
            JsfUtil.addErrorMessage("You can't issue to the same department");
            return;
        }
        if (getIssuedBill().getToStaff() == null) {
            JsfUtil.addErrorMessage("Please Select Staff");
            return;
        }
        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items");
            return;
        }
        if (getIssuedBill().getComments() == null || getIssuedBill().getComments().isBlank()) {
            JsfUtil.addErrorMessage("Please add a comment");
            return;
        }
        boolean pharmacyTransferIsByPurchaseRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Purchase Rate", false);
        boolean pharmacyTransferIsByCostRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Cost Rate", false);
        boolean pharmacyTransferIsByRetailRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Retail Rate", true);
        if (!pharmacyTransferIsByPurchaseRate && !pharmacyTransferIsByCostRate && !pharmacyTransferIsByRetailRate) {
            pharmacyTransferIsByRetailRate = true;
        }
        for (BillItem bi : getBillItems()) {
            if (!userStockController.isStockAvailable(bi.getPharmaceuticalBillItem().getStock(), bi.getPharmaceuticalBillItem().getQty(), getSessionController().getLoggedUser())) {
                JsfUtil.addErrorMessage("No adequate stocks for " + bi.getItem().getName());
                return;
            }
        }

        if (getIssuedBill().getId() == null) {
            getBillFacade().create(getIssuedBill());
        } else {
            getBillFacade().edit(getIssuedBill());
        }
        if (getBillItems() == null || getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please Add Bill Items");
            return;
        }
        boolean stockWasNotSufficientToIssueFound = false;
        for (BillItem i : getBillItems()) {
            i.getPharmaceuticalBillItem().setQty(0 - i.getPharmaceuticalBillItem().getQty());
            if (i.getQty() == 0.0) {
                continue;
            }
            i.setBill(getIssuedBill());
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            //Checking User Stock Entity
            if (!userStockController.isStockAvailable(i.getPharmaceuticalBillItem().getStock(), i.getPharmaceuticalBillItem().getQty(), getSessionController().getLoggedUser())) {
                i.setQty(0.0);
                getBillItemFacade().edit(i);
                getIssuedBill().getBillItems().add(i);
                continue;
            }

            boolean returnFlag = pharmacyBean.deductFromStock(i.getPharmaceuticalBillItem().getStock(),
                    Math.abs(i.getPharmaceuticalBillItem().getQty()),
                    i.getPharmaceuticalBillItem(),
                    getSessionController().getDepartment());
            if (returnFlag) {
                Stock staffStock = pharmacyBean.addToStock(i.getPharmaceuticalBillItem(),
                        Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), getIssuedBill().getToStaff());
                i.getPharmaceuticalBillItem().setStaffStock(staffStock);
            } else {
                i.setQty(0.0);

            }
            getBillItemFacade().edit(i);
            getIssuedBill().getBillItems().add(i);
        }

        if (stockWasNotSufficientToIssueFound) {
            JsfUtil.addErrorMessage("The Current Stock was not sufficient to issue some items. Other items issued except those items. Please check the bill and issue the missing items separately in a new issue.");
            calculateBillTotalsForTransferIssue(getIssuedBill());
        }

        getIssuedBill().getBillItems().forEach(this::updateBillItemRateAndValueAndSaveForDirectIssue);

        // Handle Department ID generation
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Transfer Issue - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment().getInstitution(), BillTypeAtomic.PHARMACY_DIRECT_ISSUE);
        } else {
            deptId = getBillNumberBean().institutionBillNumberGenerator(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_DIRECT_ISSUE, BillClassType.BilledBill);
        }
        getIssuedBill().setDeptId(deptId);
        getIssuedBill().setInsId(deptId);

        getBillFacade().edit(getIssuedBill());
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());

        printPreview = true;

        JsfUtil.addSuccessMessage("Successfully Saved");
    }

    /**
     * Removes a bill item from the direct issue
     */
    public void removeBillItem(BillItem billItem) {
        getBillItems().remove(billItem);
        int serialNo = 0;
        for (BillItem b : getBillItems()) {
            b.setSearialNo(serialNo++);
        }
        calculateBillTotalsForTransferIssue(getIssuedBill());
    }

    /**
     * Displays item details by delegating to PharmacyController
     */
    public void displayItemDetails(BillItem tmp) {
        getPharmacyController().fillItemDetails(tmp.getItem());
    }

    /**
     * Resets all controller state
     */
    public void makeNull() {
        userStockController.retiredAllUserStockContainer(getSessionController().getLoggedUser());
        issuedBill = null;
        printPreview = false;
        billItems = null;
        userStockContainer = null;
        tmpStock = null;
        stockDto = null;
        qty = null;
    }

    // ------------------------------------------------------------------
    // Private Helper Methods
    // ------------------------------------------------------------------

    /**
     * Calculates and updates bill totals for transfer issue
     */
    private void calculateBillTotalsForTransferIssue(Bill bill) {
        if (bill == null || bill.getBillItems() == null) {
            return;
        }

        BigDecimal grossTotal = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal lineGrossTotal = BigDecimal.ZERO;
        BigDecimal lineNetTotal = BigDecimal.ZERO;

        int serialNo = 1;

        for (BillItem bi : bill.getBillItems()) {
            if (bi.isRetired()) {
                continue;
            }

            bi.setSearialNo(serialNo++);

            // For transfer issue: stock goes out so qty is negative
            double absQty = Math.abs(bi.getQty());
            bi.setQty(-absQty);

            // Revenue is positive (we receive money/value for stock going out)
            double netValue = absQty * bi.getRate();
            bi.setNetValue(netValue);

            grossTotal = grossTotal.add(BigDecimal.valueOf(netValue));
            netTotal = netTotal.add(BigDecimal.valueOf(netValue));
            lineGrossTotal = lineGrossTotal.add(BigDecimal.valueOf(netValue));
            lineNetTotal = lineNetTotal.add(BigDecimal.valueOf(netValue));
        }

        // Set bill totals as positive (revenue)
        bill.setTotal(grossTotal.doubleValue());
        bill.setNetTotal(netTotal.doubleValue());

        // Set bill finance details totals as positive (revenue)
        if (bill.getBillFinanceDetails() != null) {
            bill.getBillFinanceDetails().setGrossTotal(grossTotal);
            bill.getBillFinanceDetails().setLineGrossTotal(lineGrossTotal);
            bill.getBillFinanceDetails().setNetTotal(netTotal);
            bill.getBillFinanceDetails().setLineNetTotal(lineNetTotal);
        }

        getBillFacade().edit(bill);
    }

    /**
     * Updates bill item rate and value for direct issue and saves
     */
    private void updateBillItemRateAndValueAndSaveForDirectIssue(BillItem b) {
        updateBillItemRateAndValueForDirectIssue(b);
        getBillItemFacade().edit(b);
    }

    /**
     * Updates bill item rate and value for direct issue
     */
    private void updateBillItemRateAndValueForDirectIssue(BillItem b) {
        BillItemFinanceDetails f = b.getBillItemFinanceDetails();
        double rate = b.getBillItemFinanceDetails().getLineGrossRate().doubleValue();

        // Set BillItem.qty to negative for stock out (matching PharmaceuticalBillItem.qty sign)
        if (b.getQty() > 0) {
            b.setQty(0 - b.getQty());
        }

        b.setRate(rate);
        b.setNetRate(rate);
        // BillItem values should be positive (revenue) - use absolute quantity
        b.setNetValue(rate * Math.abs(b.getQty()));
        b.setGrossValue(rate * Math.abs(b.getQty()));

        BigDecimal qtyInUnits = BigDecimal.valueOf(b.getPharmaceuticalBillItem().getQty());
        BigDecimal qtyInPacks = BigDecimal.valueOf(b.getQty());
        BigDecimal rateBig = BigDecimal.valueOf(rate);

        // Set unitsPerPack from the PharmaceuticalBillItem/DTO before computing pack rates
        if (f.getUnitsPerPack() == null || f.getUnitsPerPack().compareTo(BigDecimal.ZERO) == 0) {
            if (b.getItem() instanceof Ampp) {
                f.setUnitsPerPack(BigDecimal.valueOf(b.getItem().getDblValue()));
            } else {
                f.setUnitsPerPack(BigDecimal.ONE);
            }
        }

        // BillItemFinanceDetails quantities should be in packs; negative for stock out
        BigDecimal absQtyInPacks = qtyInPacks.abs();
        f.setQuantity(BigDecimal.ZERO.subtract(absQtyInPacks));
        f.setTotalQuantity(BigDecimal.ZERO.subtract(absQtyInPacks));
        f.setLineGrossRate(rateBig);
        f.setLineNetRate(rateBig);
        // Calculate totals using pack quantities and pack rates (positive)
        f.setLineGrossTotal(rateBig.multiply(absQtyInPacks));
        f.setLineNetTotal(rateBig.multiply(absQtyInPacks));

        // Set cost rate to actual cost rate from ItemBatch
        if (b.getPharmaceuticalBillItem() != null && b.getPharmaceuticalBillItem().getItemBatch() != null) {
            ItemBatch batch = b.getPharmaceuticalBillItem().getItemBatch();
            BigDecimal costRate = BigDecimal.valueOf(batch.getCostRate());
            f.setCostRate(costRate);
            f.setLineCostRate(costRate);
            // Negative cost values for transfer out
            BigDecimal absQtyInUnits = qtyInUnits.abs();
            f.setLineCost(BigDecimal.ZERO.subtract(costRate.multiply(absQtyInUnits)));

            // Add cost rate fields for transfer issue
            f.setBillCostRate(BigDecimal.ZERO);
            f.setTotalCostRate(costRate);
            f.setTotalCost(BigDecimal.ZERO.subtract(costRate.multiply(absQtyInUnits)));
        }
    }

    /**
     * Determines the transfer rate based on configuration
     */
    private BigDecimal determineTransferRate(ItemBatch itemBatch) {
        if (itemBatch == null) {
            return BigDecimal.ZERO;
        }

        boolean pharmacyTransferIsByPurchaseRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Purchase Rate", false);
        boolean pharmacyTransferIsByCostRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Cost Rate", false);
        boolean pharmacyTransferIsByRetailRate = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Retail Rate", true);

        if (pharmacyTransferIsByPurchaseRate) {
            return BigDecimal.valueOf(itemBatch.getPurcahseRate());
        } else if (pharmacyTransferIsByCostRate) {
            return BigDecimal.valueOf(itemBatch.getCostRate());
        } else {
            return BigDecimal.valueOf(itemBatch.getRetailsaleRate());
        }
    }

    /**
     * Converts StockDTO to Stock entity
     */
    public Stock convertStockDtoToEntity(StockDTO stockDto) {
        if (stockDto == null || stockDto.getId() == null) {
            return null;
        }
        return stockFacade.find(stockDto.getId());
    }

    // ------------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------------

    public Bill getIssuedBill() {
        if (issuedBill == null) {
            issuedBill = new BilledBill();
            issuedBill.setBillType(BillType.PharmacyTransferIssue);
            issuedBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ISSUE);
        }
        return issuedBill;
    }

    public void setIssuedBill(Bill issuedBill) {
        this.issuedBill = issuedBill;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
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

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Stock getTmpStock() {
        return tmpStock;
    }

    public void setTmpStock(Stock tmpStock) {
        this.tmpStock = tmpStock;
    }

    public StockDTO getStockDto() {
        return stockDto;
    }

    public void setStockDto(StockDTO stockDto) {
        this.stockDto = stockDto;
        // Automatically convert DTO to entity
        if (stockDto != null) {
            this.tmpStock = convertStockDtoToEntity(stockDto);
        } else {
            this.tmpStock = null;
        }
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

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public PharmacyController getPharmacyController() {
        return pharmacyController;
    }
}
