/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillNumberSuffix;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.service.StaffService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItemFinanceDetails;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class PreReturnController implements Serializable {

    private Bill bill;
    private Bill returnBill;
    private boolean printPreview;
    String comment;
    ////////

    private List<BillItem> billItems;
    ///////
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private PharmaceuticalItemController pharmaceuticalItemController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    private SessionController sessionController;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillItemFacade billItemFacade;
    @Inject
    PageMetadataRegistry pageMetadataRegistry;

    @PostConstruct
    public void init() {
        registerPageMetadata();
    }

    /**
     * Register page metadata for the admin configuration interface
     */
    private void registerPageMetadata() {
        if (pageMetadataRegistry == null) {
            return;
        }

        PageMetadata metadata = new PageMetadata();
        metadata.setPagePath("pharmacy/pharmacy_bill_return_pre");
        metadata.setPageName("Pharmacy Pre Bill Return (Items Only)");
        metadata.setDescription("Return items from pharmacy pre bills without processing refund payments");
        metadata.setControllerClass("PreReturnController");

        // Configuration Options
        metadata.addConfigOption(new ConfigOptionInfo(
            "Display a link to navigate to original pharmacy retail sale bill at the time of return items",
            "Shows a button to view the original sale bill during the return process",
            "Line 227 (XHTML): View Sale Bill button visibility in print preview",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Retail Sale Return Bill is Five Five Custom 3 Paper",
            "Uses 5.5 inch custom format 3 for return bill printing",
            "Line 247 (XHTML): Print format selection for return bills",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Retail Sale Return Bill is POS Header Paper",
            "Uses POS header paper format for return bill printing",
            "Line 253 (XHTML): Print format selection for return bills",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Pharmacy Retail Sale Return Bill is POS Paper Custom 1 Paper",
            "Uses POS paper custom 1 format for return bill printing",
            "Line 259 (XHTML): Print format selection for return bills",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Sale Return Items only - Prefix + Department Code + Institution Code + Year + Yearly Number",
            "Bill number format: Prefix-DeptCode-InsCode-Year-Number (e.g., PHRET-PH-HOS-2025-001)",
            "Line 165 (Controller): Return bill department ID generation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Sale Return Items only - Prefix + Institution Code + Department Code + Year + Yearly Number",
            "Bill number format: Prefix-InsCode-DeptCode-Year-Number (e.g., PHRET-HOS-PH-2025-001)",
            "Line 168 (Controller): Return bill department ID generation",
            OptionScope.APPLICATION
        ));

        metadata.addConfigOption(new ConfigOptionInfo(
            "Bill Number Generation Strategy for Pharmacy Sale Return Items only - Prefix + Institution Code + Year + Yearly Number",
            "Bill number format: Prefix-InsCode-Year-Number (e.g., PHRET-HOS-2025-001) - institution-wide numbering",
            "Line 171 (Controller): Return bill department and institution ID generation",
            OptionScope.APPLICATION
        ));

        // Privileges
        metadata.addPrivilege(new PrivilegeInfo(
            "Admin",
            "Administrative access to system configuration and page settings",
            "Config button visibility (added via implementation)"
        ));

        metadata.addPrivilege(new PrivilegeInfo(
            "ChangeReceiptPrintingPaperTypes",
            "Ability to change receipt printing paper format settings",
            "Line 217 (XHTML): Settings button visibility for paper format configuration"
        ));

        // Register the metadata
        pageMetadataRegistry.registerPage(metadata);
    }

    public String navigateToReturnRetailSaleItemsOnly() {
        Bill tmpBill = bill;
        makeNull();
        bill=tmpBill;
        if (bill.getDepartment() == null) {
            JsfUtil.addErrorMessage("Programming Error. Contact System Administrator");
            return null;
        }
        if (!getSessionController().getDepartment().equals(bill.getDepartment())) {
            JsfUtil.addErrorMessage("You can't return another department's Issue. Please log to the billed department");
            return null;
        }
        generateBillComponent(BillType.PharmacyPre);
        return "/pharmacy/pharmacy_bill_return_pre?faces-redirect=true";
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Bill getReturnBill() {
        if (returnBill == null) {
            returnBill = new RefundBill();
//            returnBill.setBillType(BillType.PharmacyPre);

        }

        return returnBill;
    }

    public void setReturnBill(Bill returnBill) {
        this.returnBill = returnBill;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    @Inject
    private PharmacyCalculation pharmacyRecieveBean;

    public void onEdit(BillItem tmp) {
        //    PharmaceuticalBillItem tmp = (PharmaceuticalBillItem) event.getObject();

        if (tmp.getQty() > getPharmacyRecieveBean().calQty3(tmp.getReferanceBillItem())) {
            tmp.setQty(0.0);
            JsfUtil.addErrorMessage("You cant return over than ballanced Qty ");
        }

        calTotal();
        //   getPharmacyController().setPharmacyItem(tmp.getPharmaceuticalBillItem().getBillItem().getItem());
    }

    public void makeNull() {
        bill = null;
        returnBill = null;
        printPreview = false;
        billItems = null;

    }

    private void saveReturnBill() {

        getReturnBill().copy(getBill());
        getReturnBill().setBilledBill(getBill());
        double total = 0 - getReturnBill().getTotal();
        double netTotal = 0 - getReturnBill().getNetTotal();
        double discount = 0 - getReturnBill().getDiscount();

        getReturnBill().setBillType(BillType.PharmacyPre);
        getReturnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);
        getReturnBill().setTotal(total);
        getReturnBill().setNetTotal(netTotal);
        getReturnBill().setDiscount(discount);
        getReturnBill().setComments(comment);

        getReturnBill().setCreater(getSessionController().getLoggedUser());
        getReturnBill().setCreatedAt(Calendar.getInstance().getTime());

        getReturnBill().setDepartment(getSessionController().getDepartment());
        getReturnBill().setInstitution(getSessionController().getInstitution());
        
        // Handle Department ID generation
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Return Items only - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Return Items only - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Return Items only - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);
        } else {
            // Use existing method for backward compatibility
            deptId = getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), BillType.PharmacyPre, BillClassType.RefundBill, BillNumberSuffix.PHRET);
        }

        // Handle Institution ID generation (completely separate)
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Return Items only - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Return Items only - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            insId = deptId;
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Return Items only - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            insId = deptId;
        } else {
            insId = deptId;
        }

        getReturnBill().setInsId(insId);
        getReturnBill().setDeptId(deptId);
        //   getReturnBill().setInsId(getBill().getInsId());
        if (getReturnBill().getId() == null) {
            getBillFacade().create(getReturnBill());
        }

    }

    private void saveComponent() {
        for (BillItem i : getBillItems()) {
            i.getPharmaceuticalBillItem().setQtyInUnit(i.getQty());

            if (i.getPharmaceuticalBillItem().getQty() == 0.0) {
                continue;
            }

            i.setBill(getReturnBill());
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setQty(i.getPharmaceuticalBillItem().getQty());

            double value = i.getNetRate() * i.getQty();
            i.setGrossValue(0 - value);
            i.setNetValue(0 - value);

            PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);

            if (i.getId() == null) {
                getBillItemFacade().create(i);
            }

            if (tmpPh.getId() == null) {
                getPharmaceuticalBillItemFacade().create(tmpPh);
            }

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);

            //   getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            //System.err.println("STOCK " + i.getPharmaceuticalBillItem().getStock());
            getPharmacyBean().addToStock(i.getPharmaceuticalBillItem().getStock(), Math.abs(i.getPharmaceuticalBillItem().getQtyInUnit()), i.getPharmaceuticalBillItem(), getSessionController().getDepartment());

            //   i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem().setRemainingQty(i.getRemainingQty() - i.getQty());
            //   getPharmaceuticalBillItemFacade().edit(i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem());
            //      updateRemainingQty(i);
            getReturnBill().getBillItems().add(i);
        }

    }

    @EJB
    StaffService staffBean;

    public void settle() {
        // Check if credit has been partially or fully settled
        if (bill.getPaymentMethod() == PaymentMethod.Credit){
            if (bill != null && bill.getPaidAmount() > 0) {
                JsfUtil.addErrorMessage("Cannot return items for bills with partially or fully settled credit. Please contact the administrator.");
                return;
            }
        }
        
        if (getReturnBill().getTotal() == 0) {
            JsfUtil.addErrorMessage("Total is Zero cant' return");
            return;
        }
        if (getComment() == null || getComment().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }

        // Check if any items with quantity > 0 have refundsAllowed set to false
        for (BillItem bi : getBillItems()) {
            if (bi.getQty() > 0 && bi.getItem() != null && !bi.getItem().isRefundsAllowed()) {
                JsfUtil.addErrorMessage("Item '" + bi.getItem().getName() + "' is not allowed to be returned. Refunds are not permitted for this item.");
                return;
            }
        }

        saveReturnBill();
        saveComponent();
        getBill().getReturnPreBills().add(getReturnBill());
        getReturnBill().setReferenceBill(getBill());
        getBillFacade().edit(getReturnBill());

//        getBill().getReturnPreBills().add(getReturnBill());
        getBillFacade().edit(getBill());

        // Calculate and record financial details for the pre-return bill
        System.out.println("=== Calculating financial details for pre-return bill ===");
        calculateAndRecordCostingValues(getReturnBill());

//        if (getReturnBill().getPaymentMethod() == PaymentMethod.Credit) {
//            //   ////// // System.out.println("getBill().getPaymentMethod() = " + getBill().getPaymentMethod());
//            //   ////// // System.out.println("getBill().getToStaff() = " + getBill().getToStaff());
//            if (getBill().getToStaff() != null) {
//                //   ////// // System.out.println("getBill().getNetTotal() = " + getBill().getNetTotal());
//                staffBean.updateStaffCredit(getBill().getToStaff(), getReturnBill().getNetTotal());
//                JsfUtil.addSuccessMessage("Staff Credit Updated");
//                getReturnBill().setFromStaff(getBill().getToStaff());
//                getBillFacade().edit(getReturnBill());
//            }
//        }
        /// setOnlyReturnValue();
        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Returned");

    }

    public void fillReturningQty() {
        if (billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please add bill items");
            return;
        }
        for (BillItem bi : billItems) {
            bi.setQty(bi.getPharmaceuticalBillItem().getQty());
            onEdit(bi);
        }
    }

    private void calTotal() {
        double grossTotal = 0.0;
        double discountTotal = 0.0;
        double netTotal = 0.0;

        for (BillItem p : getBillItems()) {
            grossTotal += p.getNetRate() * p.getQty() + (p.getDiscountRate() * p.getQty());
            discountTotal += p.getDiscountRate() * p.getQty();
            netTotal += p.getNetRate() * p.getQty();
        }

        getReturnBill().setTotal(grossTotal);
        getReturnBill().setNetTotal(netTotal);
        getReturnBill().setDiscount(discountTotal);

        //  return grossTotal;
    }

    public void generateBillComponent(BillType billType) {
        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getBill())) {
            BillItem bi = new BillItem();
            bi.setBill(getReturnBill());
            bi.setReferenceBill(getBill());
            bi.setReferanceBillItem(i.getBillItem());
            bi.copy(i.getBillItem());
            bi.setQty(0.0);

            PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
            tmp.setBillItem(bi);
            tmp.copy(i);

            List<BillTypeAtomic> btas = new ArrayList<>();
            btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
            btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);
            
            double rFund = getPharmacyRecieveBean().getTotalQty(i.getBillItem(), btas);

            double tmpQty = (Math.abs(i.getQtyInUnit())) - Math.abs(rFund);

            if (tmpQty <= 0) {
                continue;
            }

            tmp.setQtyInUnit(tmpQty);

            bi.setPharmaceuticalBillItem(tmp);

            getBillItems().add(bi);
        }

    }

//    private double calRemainingQty(PharmaceuticalBillItem i) {
//        if (i.getRemainingQty() == 0.0) {
////            if (i.getBillItem().getItem() instanceof Ampp) {
////                return (i.getQty()) * i.getBillItem().getItem().getDblValue();
////            } else {
////                return i.getQty();
////            }
//            return i.getQty();
//        } else {
//            return i.getRemainingQty();
//        }
//
//    }
    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public PharmaceuticalItemController getPharmaceuticalItemController() {
        return pharmaceuticalItemController;
    }

    public void setPharmaceuticalItemController(PharmaceuticalItemController pharmaceuticalItemController) {
        this.pharmaceuticalItemController = pharmaceuticalItemController;
    }

    public PharmacyController getPharmacyController() {
        return pharmacyController;
    }

    public void setPharmacyController(PharmacyController pharmacyController) {
        this.pharmacyController = pharmacyController;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PharmacyCalculation getPharmacyRecieveBean() {
        return pharmacyRecieveBean;
    }

    public void setPharmacyRecieveBean(PharmacyCalculation pharmacyRecieveBean) {
        this.pharmacyRecieveBean = pharmacyRecieveBean;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Calculates and records comprehensive financial details for pre-return bills.
     * This method ensures BillFinanceDetails and BillItemFinanceDetails are properly populated
     * for pharmacy pre-return transactions, maintaining consistency with full return processing.
     *
     * Key financial details calculated:
     * - Stock valuations (cost, purchase, retail, wholesale rates)
     * - Quantity tracking (including free quantities)
     * - Bill and line totals (net, gross)
     * - Individual item financial metrics
     *
     * This ensures pharmacy income reports have complete financial data for pre-returns
     * even before the actual refund is processed by the cashier.
     *
     * @param bill The pre-return bill to calculate financial details for
     */
    private void calculateAndRecordCostingValues(Bill bill) {
        System.out.println("=== calculateAndRecordCostingValues START (PreReturn) ===");
        System.out.println("Bill ID: " + (bill != null ? bill.getId() : "null"));
        System.out.println("Bill Type: " + (bill != null ? bill.getBillTypeAtomic() : "null"));

        if (bill == null || bill.getBillItems() == null || bill.getBillItems().isEmpty()) {
            System.out.println("Early return - no bill or bill items");
            return;
        }

        System.out.println("Number of bill items: " + bill.getBillItems().size());

        // Initialize bill finance details if not present
        if (bill.getBillFinanceDetails() == null) {
            BillFinanceDetails billFinanceDetails = new BillFinanceDetails();
            billFinanceDetails.setBill(bill);
            bill.setBillFinanceDetails(billFinanceDetails);
            System.out.println("Created new BillFinanceDetails for bill");
        } else {
            System.out.println("BillFinanceDetails already exists for bill");
        }

        // Initialize bill-level totals
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalPurchaseValue = BigDecimal.ZERO;
        BigDecimal totalRetailSaleValue = BigDecimal.ZERO;
        BigDecimal totalWholesaleValue = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalFreeQuantity = BigDecimal.ZERO;

        // Process each bill item
        int itemIndex = 0;
        for (BillItem billItem : bill.getBillItems()) {
            itemIndex++;
            System.out.println("--- Processing Bill Item " + itemIndex + " ---");
            System.out.println("BillItem ID: " + (billItem != null ? billItem.getId() : "null"));
            System.out.println("BillItem retired: " + (billItem != null ? billItem.isRetired() : "null"));
            System.out.println("BillItem qty: " + (billItem != null ? billItem.getQty() : "null"));

            if (billItem == null || billItem.isRetired()) {
                System.out.println("Skipping retired or null bill item");
                continue;
            }

            // Initialize bill item finance details if not present
            if (billItem.getBillItemFinanceDetails() == null) {
                BillItemFinanceDetails itemFinanceDetails = new BillItemFinanceDetails();
                itemFinanceDetails.setBillItem(billItem);
                billItem.setBillItemFinanceDetails(itemFinanceDetails);
                System.out.println("Created new BillItemFinanceDetails for item");
            } else {
                System.out.println("BillItemFinanceDetails already exists for item");
            }

            // Get pharmaceutical bill item for rate information
            PharmaceuticalBillItem pharmaItem = billItem.getPharmaceuticalBillItem();
            System.out.println("PharmaceuticalBillItem: " + (pharmaItem != null ? "exists" : "null"));
            if (pharmaItem == null) {
                System.out.println("Skipping - no pharmaceutical bill item");
                continue;
            }

            // Get quantities - for returns these will be negative
            BigDecimal qty = BigDecimal.valueOf(billItem.getQty());
            BigDecimal freeQty = BigDecimal.valueOf(pharmaItem.getFreeQty());
            BigDecimal totalQty = qty.add(freeQty);
            System.out.println("Quantities - qty: " + qty + ", freeQty: " + freeQty + ", totalQty: " + totalQty);

            // Get rates from pharmaceutical bill item
            BigDecimal retailRate = BigDecimal.valueOf(pharmaItem.getRetailRate());
            BigDecimal purchaseRate = BigDecimal.valueOf(pharmaItem.getPurchaseRate());
            BigDecimal wholesaleRate = BigDecimal.valueOf(pharmaItem.getWholesaleRate());

            System.out.println("Pharma rates - retail: " + retailRate + ", purchase: " + purchaseRate + ", wholesale: " + wholesaleRate);

            // Get cost rate from item batch (which is the actual cost for returns)
            BigDecimal costRate = purchaseRate; // default fallback
            if (pharmaItem.getItemBatch() != null) {
                Double batchCostRate = pharmaItem.getItemBatch().getCostRate();
                if (batchCostRate != null && batchCostRate > 0) {
                    costRate = BigDecimal.valueOf(batchCostRate);
                    System.out.println("Got costRate from itemBatch.getCostRate(): " + costRate);

                    // Also update the pharmaceutical bill item with this cost rate
                    pharmaItem.setCostRate(costRate.doubleValue());
                    pharmaItem.setPurchaseRate(costRate.doubleValue());
                    System.out.println("Updated PharmaceuticalBillItem costRate to: " + costRate);
                } else {
                    System.out.println("ItemBatch purcahseRate is zero, using purchaseRate as fallback");
                }
            } else {
                System.out.println("No itemBatch available, using purchaseRate as costRate fallback");
            }

            // Calculate values based on total quantity (including free quantities)
            BigDecimal itemRetailValue = retailRate.multiply(totalQty);
            BigDecimal itemPurchaseValue = purchaseRate.multiply(totalQty);
            BigDecimal itemCostValue = costRate.multiply(totalQty);
            BigDecimal itemWholesaleValue = wholesaleRate.multiply(totalQty);

            System.out.println("Calculated values - retail: " + itemRetailValue + ", purchase: " + itemPurchaseValue +
                             ", cost: " + itemCostValue + ", wholesale: " + itemWholesaleValue);

            // Set item-level finance details - enhanced with more comprehensive data
            BillItemFinanceDetails bifd = billItem.getBillItemFinanceDetails();
            System.out.println("Setting values in BillItemFinanceDetails...");

            // RATES (no signs - always positive rates)
            bifd.setLineNetRate(BigDecimal.valueOf(Math.abs(billItem.getNetRate())));
            bifd.setLineGrossRate(BigDecimal.valueOf(Math.abs(billItem.getRate())));
            bifd.setGrossRate(BigDecimal.valueOf(Math.abs(billItem.getRate())));
            bifd.setLineCostRate(costRate.abs()); // costRate from itemBatch (no sign)
            bifd.setCostRate(costRate.abs());
            bifd.setPurchaseRate(purchaseRate.abs());
            bifd.setRetailSaleRate(retailRate.abs());

            // BILL-LEVEL RATES (always 0 for now)
            bifd.setBillCostRate(BigDecimal.ZERO);

            // TOTAL RATES (lineCostRate + billCostRate)
            bifd.setTotalCostRate(bifd.getLineCostRate()); // since billCostRate = 0

            // TOTALS
            bifd.setGrossTotal(BigDecimal.valueOf(billItem.getGrossValue()));
            bifd.setLineGrossTotal(bifd.getGrossTotal()); // no bill-level discounts
            bifd.setLineNetTotal(BigDecimal.valueOf(billItem.getNetValue()));

            // COSTS (with signs - negative for returns as cost goes out)
            BigDecimal lineCost = costRate.multiply(qty.abs()).negate(); // Always negative for returns
            bifd.setLineCost(lineCost);
            bifd.setBillCost(BigDecimal.ZERO);
            bifd.setTotalCost(lineCost); // totalCost = lineCost + billCost

            // QUANTITIES
            bifd.setQuantity(qty);
            bifd.setFreeQuantity(freeQty);
            bifd.setTotalQuantity(totalQty);
            bifd.setQuantityByUnits(qty.abs()); // no packs, same as quantity but positive

            // VALUES AT RATES (positive - valuation of quantity)
            BigDecimal absQty = qty.abs(); // absolute quantity for valuation
            bifd.setValueAtCostRate(costRate.multiply(absQty));
            bifd.setValueAtPurchaseRate(purchaseRate.multiply(absQty));
            bifd.setValueAtRetailRate(retailRate.multiply(absQty));
            bifd.setValueAtWholesaleRate(wholesaleRate.multiply(absQty));

            System.out.println("Set all BillItemFinanceDetails fields successfully");
            System.out.println("Rates - lineNet: " + bifd.getLineNetRate() + ", lineGross: " + bifd.getLineGrossRate() +
                             ", lineCost: " + bifd.getLineCostRate());
            System.out.println("Costs - line: " + lineCost + ", bill: " + bifd.getBillCost() + ", total: " + bifd.getTotalCost());
            System.out.println("Values - cost: " + bifd.getValueAtCostRate() + ", purchase: " + bifd.getValueAtPurchaseRate() +
                             ", retail: " + bifd.getValueAtRetailRate());

            // Set PharmaceuticalBillItem values (positive valuations)
            System.out.println("Setting PharmaceuticalBillItem values...");
            BigDecimal absQtyForPBI = qty.abs(); // absolute quantity for PBI valuations
            pharmaItem.setCostValue(costRate.multiply(absQtyForPBI).doubleValue());
            pharmaItem.setPurchaseValue(purchaseRate.multiply(absQtyForPBI).doubleValue());
            pharmaItem.setRetailValue(retailRate.multiply(absQtyForPBI).doubleValue());

            System.out.println("PBI values - cost: " + pharmaItem.getCostValue() +
                             ", purchase: " + pharmaItem.getPurchaseValue() +
                             ", retail: " + pharmaItem.getRetailValue());

            // Save PharmaceuticalBillItem to ensure values are persisted
            if (pharmaItem.getId() == null) {
                System.out.println("PharmaceuticalBillItem is new - will be saved via cascade");
            } else {
                System.out.println("PharmaceuticalBillItem exists, saving explicitly");
                pharmaceuticalBillItemFacade.edit(pharmaItem);
            }

            // Aggregate values for bill level
            totalCostValue = totalCostValue.add(itemCostValue);
            totalPurchaseValue = totalPurchaseValue.add(itemPurchaseValue);
            totalRetailSaleValue = totalRetailSaleValue.add(itemRetailValue);
            totalWholesaleValue = totalWholesaleValue.add(itemWholesaleValue);
            totalQuantity = totalQuantity.add(qty);
            totalFreeQuantity = totalFreeQuantity.add(freeQty);

            System.out.println("Aggregated totals - cost: " + totalCostValue + ", purchase: " + totalPurchaseValue +
                             ", retail: " + totalRetailSaleValue + ", quantity: " + totalQuantity);

            // Save bill item finance details using JPA cascade persistence
            if (billItem.getBillItemFinanceDetails().getId() == null) {
                System.out.println("BillItemFinanceDetails is new (id == null) - will be saved via cascade");
            } else {
                System.out.println("BillItemFinanceDetails exists, calling billItemFacade.edit()");
                billItemFacade.edit(billItem);
            }
        }

        System.out.println("=== Finished processing all bill items ===");

        // Set bill-level finance details - enhanced with missing fields
        System.out.println("Setting BillFinanceDetails totals...");
        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        bfd.setTotalCostValue(totalCostValue);
        bfd.setTotalPurchaseValue(totalPurchaseValue);
        bfd.setTotalRetailSaleValue(totalRetailSaleValue);
        bfd.setTotalWholesaleValue(totalWholesaleValue);

        // Set missing quantity totals needed for pharmacy income reports
        bfd.setTotalQuantity(totalQuantity);
        bfd.setTotalFreeQuantity(totalFreeQuantity);

        // Set basic totals from bill for reporting consistency
        bfd.setNetTotal(BigDecimal.valueOf(bill.getNetTotal()));
        bfd.setGrossTotal(BigDecimal.valueOf(bill.getTotal()));

        System.out.println("Final BillFinanceDetails totals - cost: " + totalCostValue +
                         ", purchase: " + totalPurchaseValue + ", retail: " + totalRetailSaleValue +
                         ", quantity: " + totalQuantity + ", netTotal: " + bill.getNetTotal());

        // Save bill finance details
        if (bill.getBillFinanceDetails().getId() == null) {
            System.out.println("BillFinanceDetails is new (id == null) - will be saved via cascade");
        } else {
            System.out.println("BillFinanceDetails exists, calling billFacade.edit()");
            billFacade.edit(bill);
        }

        System.out.println("=== calculateAndRecordCostingValues COMPLETE (PreReturn) ===");
    }

}
