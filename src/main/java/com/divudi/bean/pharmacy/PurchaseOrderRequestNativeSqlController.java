/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.PageMetadataRegistry;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.OptionScope;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.admin.ConfigOptionInfo;
import com.divudi.core.data.admin.PageMetadata;
import com.divudi.core.data.admin.PrivilegeInfo;
import com.divudi.core.data.dto.PharmacyPurchaseOrderRateDTO;
import com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.data.MessageType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.EmailFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.EmailManagerEjb;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.service.pharmacy.PurchaseOrderRequestNativeSqlService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * SessionScoped controller for the native-SQL Purchase Order Request page.
 * Native SQL: bill create/update, billitem+PBI writes (via the service).
 * JPA (unchanged): rate lookups, bill-number generation, email — see
 * docs/superpowers/specs/2026-08-07-po-request-native-design.md §3.
 * Related issue: #22727
 */
@Named
@SessionScoped
public class PurchaseOrderRequestNativeSqlController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(PurchaseOrderRequestNativeSqlController.class.getName());

    /** Smallest purchase rate a line may carry and still be finalizable. */
    private static final double MIN_PURCHASE_RATE = 0.00001;

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ConfigOptionController configOptionController;
    @Inject
    private EnumController enumController;
    @Inject
    private ItemController itemController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    private PharmacyCalculation pharmacyBillBean;
    @Inject
    private PageMetadataRegistry pageMetadataRegistry;

    @EJB
    private PurchaseOrderRequestNativeSqlService purchaseOrderRequestNativeSqlService;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private EmailFacade emailFacade;
    @EJB
    private EmailManagerEjb emailManagerEjb;

    private Bill currentBill;
    private BillItem currentBillItem;
    private List<BillItem> billItems;
    private List<BillItem> selectedBillItems;
    private boolean printPreview;
    private boolean itemHistoryVisible;
    private Long billId;
    private String emailRecipient;

    public String navigateToCreateNewPurchaseOrder() {
        resetBillValues();
        // getCurrentBill() rebuilds the draft with the configured default
        // payment method and consignment flag, so the entry path and the page's
        // "New Order" buttons produce an identically initialised bill.
        getCurrentBill();
        return "/pharmacy/pharmacy_purhcase_order_request_native?faces-redirect=true";
    }

    public String navigateToUpdatePurchaseOrder(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        resetBillValues();
        Bill bill = billFacade.find(billId);
        if (bill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return "";
        }
        if (bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_ORDER_PRE
                && bill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_ORDER) {
            JsfUtil.addErrorMessage("Bill is not a purchase order request");
            return "";
        }
        if (bill.isRetired() || bill.isCancelled()) {
            JsfUtil.addErrorMessage("Bill is retired or cancelled");
            return "";
        }
        if (bill.getDepartment() == null || sessionController.getLoggedUser() == null
                || !bill.getDepartment().equals(sessionController.getDepartment())) {
            JsfUtil.addErrorMessage("You are not authorized to view this purchase order request");
            return "";
        }
        this.billId = billId;
        currentBill = bill;
        billItems = loadBillItems(currentBill);
        return "/pharmacy/pharmacy_purhcase_order_request_native?faces-redirect=true";
    }

    private List<BillItem> loadBillItems(Bill bill) {
        String jpql = "select bi from BillItem bi where bi.retired=:ret and bi.bill=:bill order by bi.searialNo";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("bill", bill);
        List<BillItem> result = billItemFacade.findByJpql(jpql, m);
        return result != null ? result : new ArrayList<>();
    }

    public void resetBillValues() {
        currentBill = null;
        currentBillItem = new BillItem();
        billItems = new ArrayList<>();
        selectedBillItems = null;
        printPreview = false;
        itemHistoryVisible = false;
        billId = null;
    }

    /**
     * Authorization helper method to check Purchase Order privileges and
     * audit denied access
     *
     * @param action The action being attempted (SAVE, FINALIZE)
     * @param requiredPrivilege The specific privilege required
     * @return true if authorized, false if not
     */
    private boolean isAuthorized(String action, String requiredPrivilege) {
        if (webUserController == null || sessionController == null) {
            LOGGER.log(Level.SEVERE, "Authorization failed - missing controllers: action={0}, userId=null, billId={1}",
                    new Object[]{action, currentBill != null ? currentBill.getId() : "null"});
            return false;
        }

        if (!webUserController.hasPrivilege(requiredPrivilege)) {
            Long userId = sessionController.getLoggedUser() != null ? sessionController.getLoggedUser().getId() : null;
            Long billIdForLog = currentBill != null ? currentBill.getId() : null;

            LOGGER.log(Level.WARNING, "SECURITY: Unauthorized Purchase Order access attempt - action={0}, userId={1}, billId={2}, requiredPrivilege={3}",
                    new Object[]{action, userId, billIdForLog, requiredPrivilege});

            JsfUtil.addErrorMessage("You don't have permission to " + action.toLowerCase() + " purchase order requests.");
            return false;
        }

        return true;
    }

    public List<Item> getDealorItems() {
        List<Item> lst;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c.item "
                + " from ItemsDistributors c"
                + " where c.retired=false "
                + " and c.item.retired=false "
                + " and c.item.inactive=false "
                + " and c.institution=:ins "
                + " order by c.item.name";
        hm.put("ins", getCurrentBill().getToInstitution());
        lst = itemFacade.findByJpql(sql, hm, 200);

        // Filter by department type if set
        if (getCurrentBill().getDepartmentType() != null && lst != null) {
            lst = filterItemsByDepartmentType(lst, getCurrentBill().getDepartmentType());
        }

        return lst;
    }

    private List<Item> filterItemsByDepartmentType(List<Item> items, DepartmentType departmentType) {
        if (items == null || departmentType == null) {
            return items;
        }

        // Check if the department type is allowed for pharmacy transactions
        List<DepartmentType> allowedTypes = sessionController.getAvailableDepartmentTypesForPharmacyTransactions();
        if (allowedTypes == null || !allowedTypes.contains(departmentType)) {
            return new ArrayList<>(); // Return empty list if department type not allowed
        }

        // For now, return all items if department type is allowed
        // Additional item-specific filtering can be added here if needed
        return items;
    }

    public List<Item> completeItemForSelectedDepartmentType(String query) {
        // Get items from the ItemController
        List<Item> allItems;
        if (getCurrentBill().getDepartmentType() == null) {
            allItems = itemController.completeAmpAndAmppItemForLoggedDepartment(query);
        } else {
            allItems = itemController.completeAmpAndAmppItemForLoggedDepartment(query, getCurrentBill().getDepartmentType());
        }

        // Filter by department type if set
        if (getCurrentBill().getDepartmentType() != null && allItems != null) {
            return filterItemsByDepartmentType(allItems, getCurrentBill().getDepartmentType());
        }

        return allItems;
    }

    private void applyLastRatesToBillItem(BillItem billItem) {
        if (billItem == null || billItem.getItem() == null) {
            return;
        }

        List<Item> items = new ArrayList<>();
        items.add(billItem.getItem());

        Map<Long, Double> purchaseRates = fetchLastPurchaseRatesForItems(items);
        Map<Long, Double> retailRates = fetchLastRetailRatesForItems(items);

        applyLastRatesToBillItem(
                billItem,
                getRateForItem(purchaseRates, billItem.getItem()),
                getRateForItem(retailRates, billItem.getItem()));
    }

    private void applyLastRatesToBillItem(BillItem billItem, double purchaseRate, double retailRate) {
        PharmaceuticalBillItem pharmaceuticalBillItem = billItem.getPharmaceuticalBillItem();
        pharmaceuticalBillItem.setPurchaseRate(purchaseRate);
        pharmaceuticalBillItem.setRetailRate(retailRate);

        com.divudi.core.entity.BillItemFinanceDetails financeDetails = billItem.getBillItemFinanceDetails();
        financeDetails.setUnitsPerPack(getUnitsPerPack(billItem.getItem()));
        financeDetails.setLineGrossRate(BigDecimal.valueOf(purchaseRate));
        financeDetails.setLineNetRate(financeDetails.getLineGrossRate());
        financeDetails.setRetailSaleRate(BigDecimal.valueOf(retailRate));
    }

    public void addItem() {
        if (!isAuthorized("SAVE", "PurchaseOrderSave")) {
            return;
        }
        if (currentBillItem.getItem() == null) {
            JsfUtil.addErrorMessage("Please select and item from the list");
            return;
        }

        if (getCurrentBill().getDepartmentType() == null) {
            getCurrentBill().setDepartmentType(currentBillItem.getItem().getDepartmentType() != null
                    ? currentBillItem.getItem().getDepartmentType() : DepartmentType.Pharmacy);
        }

        DepartmentType itemDepartmentType = currentBillItem.getItem().getDepartmentType();
        if (itemDepartmentType != null && !itemDepartmentType.equals(getCurrentBill().getDepartmentType())) {
            JsfUtil.addErrorMessage("Cannot add items from different department types. "
                    + "Bill is set for " + getCurrentBill().getDepartmentType().getLabel()
                    + " items, but you are trying to add a " + itemDepartmentType.getLabel() + " item.");
            return;
        }

        List<DepartmentType> allowedTypes = sessionController.getAvailableDepartmentTypesForPharmacyTransactions();
        if (allowedTypes == null || !allowedTypes.contains(getCurrentBill().getDepartmentType())) {
            JsfUtil.addErrorMessage("Items are not allowed for the selected department type: " + getCurrentBill().getDepartmentType().getLabel());
            return;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Prevent Duplicate Items in Purchase Orders", false)) {
            for (BillItem existing : billItems) {
                if (existing != null && !existing.isRetired() && existing.getItem() != null
                        && existing.getItem().equals(currentBillItem.getItem())) {
                    JsfUtil.addErrorMessage("This item has already been added to the purchase order. Please update the quantity of the existing item instead of adding it again.");
                    return;
                }
            }
        }

        currentBillItem.setSearialNo(billItems.size());
        applyLastRatesToBillItem(currentBillItem);
        billItems.add(currentBillItem);
        calculateBillTotals();
        currentBillItem = new BillItem();
    }

    public void removeItem(BillItem bi) {
        if (!isAuthorized("SAVE", "PurchaseOrderSave")) {
            return;
        }
        if (currentBill == null || bi == null) {
            return;
        }
        if (currentBill.isChecked()) {
            JsfUtil.addErrorMessage("Cannot modify a finalized bill");
            return;
        }
        bi.setRetired(true);
        if (bi.getId() != null) {
            purchaseOrderRequestNativeSqlService.retireLine(bi.getId(), sessionController.getLoggedUser().getId());
        }
        billItems.remove(bi);
        calculateBillTotals();
        if (currentBill.getId() != null) {
            purchaseOrderRequestNativeSqlService.updateBillTotals(currentBill.getId(), currentBill.getNetTotal(), currentBill.getTotal());
        }
        persistSurvivingSerialNumbers();
        itemHistoryVisible = false;
    }

    public void removeSelected() {
        if (!isAuthorized("SAVE", "PurchaseOrderSave")) {
            return;
        }
        if (selectedBillItems == null) {
            return;
        }
        if (currentBill != null && currentBill.isChecked()) {
            JsfUtil.addErrorMessage("Cannot modify a finalized bill");
            return;
        }
        if (!saveRequestWithoutMessage()) {
            return;
        }
        for (BillItem b : selectedBillItems) {
            b.setBill(null);
            b.setRetired(true);
            b.setRetirer(sessionController.getLoggedUser());
            b.setRetiredAt(new Date());
            if (b.getId() != null) {
                purchaseOrderRequestNativeSqlService.retireLine(b.getId(), sessionController.getLoggedUser().getId());
            }
        }

        billItems.removeAll(selectedBillItems);
        calculateBillTotals();
        if (currentBill.getId() != null) {
            purchaseOrderRequestNativeSqlService.updateBillTotals(currentBill.getId(), currentBill.getNetTotal(), currentBill.getTotal());
        }
        persistSurvivingSerialNumbers();
        selectedBillItems = null;
    }

    /**
     * Persists the searialNo calculateBillTotals() just renumbered in memory,
     * for every surviving line that's already been saved (has an id). Called
     * after a removal, where calculateBillTotals() can shift a persisted
     * survivor's serial to fill the gap left by a retired line.
     */
    private void persistSurvivingSerialNumbers() {
        for (BillItem bi : billItems) {
            if (bi != null && !bi.isRetired() && bi.getId() != null) {
                purchaseOrderRequestNativeSqlService.updateBillItemSerialNo(bi.getId(), bi.getSearialNo());
            }
        }
    }

    public void onEdit(BillItem bi) {
        if (bi.getBillItemFinanceDetails() != null
                && configOptionController.getBooleanValueByKey("Pharmacy Purchase - Quantity Must Be Integer", true)) {
            java.math.BigDecimal qty = bi.getBillItemFinanceDetails().getQuantity();
            java.math.BigDecimal freeQty = bi.getBillItemFinanceDetails().getFreeQuantity();
            if (qty != null && qty.remainder(java.math.BigDecimal.ONE).compareTo(java.math.BigDecimal.ZERO) != 0) {
                bi.getBillItemFinanceDetails().setQuantity(java.math.BigDecimal.ZERO);
                recalculateLineValues(bi);
                JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for quantity. Decimal values are not allowed.");
                calculateBillTotals();
                return;
            }
            if (freeQty != null && freeQty.remainder(java.math.BigDecimal.ONE).compareTo(java.math.BigDecimal.ZERO) != 0) {
                bi.getBillItemFinanceDetails().setFreeQuantity(java.math.BigDecimal.ZERO);
                recalculateLineValues(bi);
                JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for free quantity. Decimal values are not allowed.");
                calculateBillTotals();
                return;
            }
        }
        recalculateLineValues(bi);
        calculateBillTotals();
    }

    /**
     * Recomputes the BillItem-level rate/value fields (rate, netRate, grossValue,
     * netValue) from the current BillItemFinanceDetails quantity/rate after an
     * in-place edit on the page. BillItemFinanceDetails and PharmaceuticalBillItem
     * fields are already correctly bound via two-way JSF EL on the qty/freeQty/rate
     * inputs (see pharmacy_purhcase_order_request_native.xhtml), so only the
     * BillItem-level fields — which calculateBillTotals() sums for the on-screen
     * total — need recalculating here. Mirrors the BillItem-level subset of legacy
     * PurchaseOrderRequestController.calculateLineValues() (no discount/tax/expense
     * in purchase order requests, so net == gross).
     */
    private void recalculateLineValues(BillItem bi) {
        if (bi == null || bi.getBillItemFinanceDetails() == null) {
            return;
        }
        java.math.BigDecimal qty = bi.getBillItemFinanceDetails().getQuantity();
        java.math.BigDecimal purchaseRate = bi.getBillItemFinanceDetails().getLineGrossRate();
        if (qty == null) {
            qty = java.math.BigDecimal.ZERO;
        }
        if (purchaseRate == null) {
            purchaseRate = java.math.BigDecimal.ZERO;
        }

        java.math.BigDecimal grossValue = purchaseRate.multiply(qty);
        java.math.BigDecimal netValue = grossValue;

        bi.setRate(purchaseRate.doubleValue());
        bi.setNetRate(purchaseRate.doubleValue());
        bi.setGrossValue(grossValue.doubleValue());
        bi.setNetValue(netValue.doubleValue());
    }

    /**
     * Recomputes the on-screen bill total AND renumbers the surviving lines'
     * serial numbers so they stay contiguous from 0.
     * <p>
     * Renumbering matters because the page's dataTable uses
     * {@code rowKey="#{bi.searialNo}"} (see
     * pharmacy_purhcase_order_request_native.xhtml) and both {@code addItem()}
     * and {@code generateBillComponentsForAllSupplierItems()} seed a new line's
     * serial from {@code billItems.size()}. After a removal the size shrinks
     * back over a serial already held by a survivor, so the next added line
     * would collide with it and PrimeFaces would bind edits to the wrong row.
     * Mirrors legacy PurchaseOrderRequestController.calculateBillTotals().
     */
    private void calculateBillTotals() {
        double total = 0.0;
        int serialNo = 0;
        for (BillItem bi : billItems) {
            if (bi == null || bi.isRetired()) {
                continue;
            }
            bi.setSearialNo(serialNo++);
            total += bi.getNetValue();
        }
        currentBill.setNetTotal(total);
        currentBill.setTotal(total);
    }

    /**
     * Total quantity (in atomic units) across all non-retired lines. Used as
     * the in-memory pre-check that a finalize is allowed at all, before any
     * native mutation touches the DB. Mirrors legacy
     * PurchaseOrderRequestController.calculateTotalBillItemsCount().
     */
    private double calculateTotalBillItemsCount(List<BillItem> items) {
        double total = 0d;
        if (items == null) {
            return total;
        }
        for (BillItem b : items) {
            if (b == null || b.isRetired()) {
                continue;
            }
            BigDecimal totalUnits = totalUnitsForLine(b);
            if (totalUnits.compareTo(BigDecimal.ZERO) > 0) {
                total += totalUnits.doubleValue();
            }
        }
        return total;
    }

    /**
     * True only when every non-retired line carries at least one atomic unit of
     * quantity and a usable purchase rate. Mirrors legacy
     * PurchaseOrderRequestController.allBillItemsValid().
     */
    private boolean allBillItemsValid(List<BillItem> items) {
        if (items == null || items.isEmpty()) {
            return false;
        }
        boolean sawLine = false;
        for (BillItem bi : items) {
            if (bi == null || bi.isRetired()) {
                continue;
            }
            sawLine = true;
            com.divudi.core.entity.BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
            if (fd == null) {
                return false;
            }
            // Require at least one atomic unit (e.g., tablet)
            if (totalUnitsForLine(bi).compareTo(BigDecimal.ONE) < 0) {
                return false;
            }
            BigDecimal rate = fd.getLineGrossRate();
            if (rate == null || rate.compareTo(BigDecimal.valueOf(MIN_PURCHASE_RATE)) < 0) {
                return false;
            }
        }
        return sawLine;
    }

    /**
     * Quantity + free quantity for a line, expressed in atomic units.
     * <p>
     * Legacy reads {@code BillItemFinanceDetails.quantityByUnits} /
     * {@code freeQuantityByUnits} directly, because legacy's
     * {@code calculateLineValues()} refreshes those two in-memory fields on
     * every edit. The native flow deliberately does NOT: {@code quantityByUnits}
     * is computed server-side inside
     * {@code PurchaseOrderRequestNativeSqlService.saveBillItemFinanceDetails()},
     * and the page binds only {@code quantity} / {@code freeQuantity} /
     * {@code lineGrossRate}. A freshly added, not-yet-saved line therefore has
     * a null {@code quantityByUnits}, so reading it directly would reject every
     * valid new order. Derive it here from the same inputs and the same formula
     * the service uses (pack quantity x unitsPerPack), so validation matches
     * what will actually be persisted.
     */
    private BigDecimal totalUnitsForLine(BillItem bi) {
        com.divudi.core.entity.BillItemFinanceDetails fd = bi.getBillItemFinanceDetails();
        if (fd == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal qty = fd.getQuantity() != null ? fd.getQuantity() : BigDecimal.ZERO;
        BigDecimal freeQty = fd.getFreeQuantity() != null ? fd.getFreeQuantity() : BigDecimal.ZERO;
        BigDecimal unitsPerPack = (fd.getUnitsPerPack() != null && fd.getUnitsPerPack().compareTo(BigDecimal.ZERO) > 0)
                ? fd.getUnitsPerPack() : BigDecimal.ONE;
        if (bi.getItem() instanceof Ampp) {
            return qty.multiply(unitsPerPack).add(freeQty.multiply(unitsPerPack));
        }
        return qty.add(freeQty);
    }

    /**
     * A duplicate-line removal can leave the surviving BillItem with a lazily
     * created all-zero PharmaceuticalBillItem while its BillItemFinanceDetails
     * still holds the real quantities; downstream approval/GRN reads the PBI,
     * so rebuild the derived line values from the finance details before
     * persisting (issue #21417).
     * <p>
     * The native flow rebuilds the persisted PBI qty/freeQty from the line's
     * BillItemFinanceDetails on every save (see
     * {@code toLineData()} -> {@code PurchaseOrderRequestNativeSqlService.saveLine()}),
     * so the PBI columns self-heal. What does NOT self-heal is the in-memory
     * BillItem's own rate/netValue fields, which {@code calculateBillTotals()}
     * sums for the on-screen and persisted bill total. Recalculate them here,
     * matching legacy's {@code calculateLineValues(b)} call.
     */
    private void resyncPharmaceuticalBillItemIfEmpty(BillItem b) {
        if (b == null || b.isRetired() || b.getBillItemFinanceDetails() == null) {
            return;
        }
        PharmaceuticalBillItem pbi = b.getPharmaceuticalBillItem();
        if (pbi == null || pbi.getQty() != 0 || pbi.getFreeQty() != 0) {
            return;
        }
        BigDecimal qtyByUnits = b.getBillItemFinanceDetails().getQuantity();
        BigDecimal freeQtyByUnits = b.getBillItemFinanceDetails().getFreeQuantity();
        boolean financeDetailsHaveQty = (qtyByUnits != null && qtyByUnits.compareTo(BigDecimal.ZERO) > 0)
                || (freeQtyByUnits != null && freeQtyByUnits.compareTo(BigDecimal.ZERO) > 0);
        if (financeDetailsHaveQty) {
            recalculateLineValues(b);
        }
    }

    public void generateBillComponentsForAllSupplierItems(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        if (billItems == null) {
            billItems = new ArrayList<>();
        }

        int serialStart = billItems.size();
        boolean preventDuplicates = configOptionApplicationController.getBooleanValueByKey("Prevent Duplicate Items in Purchase Orders", false);
        int skippedCount = 0;
        int skippedDepartmentTypeCount = 0;

        List<DepartmentType> allowedTypes = sessionController.getAvailableDepartmentTypesForPharmacyTransactions();

        List<Item> itemsToAdd = new ArrayList<>();
        for (Item i : items) {
            DepartmentType itemDepartmentType = i != null ? i.getDepartmentType() : null;
            DepartmentType billDepartmentType = getCurrentBill().getDepartmentType() != null
                    ? getCurrentBill().getDepartmentType()
                    : (itemDepartmentType != null ? itemDepartmentType : DepartmentType.Pharmacy);
            boolean departmentTypeMismatch = itemDepartmentType != null && !itemDepartmentType.equals(billDepartmentType);
            boolean departmentTypeNotAllowed = allowedTypes == null || !allowedTypes.contains(billDepartmentType);
            if (departmentTypeMismatch || departmentTypeNotAllowed) {
                skippedDepartmentTypeCount++;
                continue;
            }

            if (preventDuplicates) {
                boolean isDuplicate = false;
                for (BillItem existingItem : billItems) {
                    if (existingItem != null && !existingItem.isRetired()
                            && existingItem.getItem() != null
                            && existingItem.getItem().equals(i)) {
                        isDuplicate = true;
                        skippedCount++;
                        break;
                    }
                }
                if (isDuplicate) {
                    continue; // Skip this item as it already exists
                }
            }
            if (getCurrentBill().getDepartmentType() == null) {
                getCurrentBill().setDepartmentType(billDepartmentType);
            }
            itemsToAdd.add(i);
        }

        if (skippedDepartmentTypeCount > 0) {
            JsfUtil.addErrorMessage(skippedDepartmentTypeCount + " item(s) of a different or disallowed department type were skipped.");
        }

        Map<Long, Double> purchaseRatesByItemId = fetchLastPurchaseRatesForItems(itemsToAdd);
        Map<Long, Double> retailRatesByItemId = fetchLastRetailRatesForItems(itemsToAdd);

        for (Item i : itemsToAdd) {
            BillItem bi = new BillItem();
            bi.setItem(i);

            PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
            tmp.setBillItem(bi);
            bi.setPharmaceuticalBillItem(tmp);

            bi.setSearialNo(serialStart++);

            applyLastRatesToBillItem(
                    bi,
                    getRateForItem(purchaseRatesByItemId, i),
                    getRateForItem(retailRatesByItemId, i));

            billItems.add(bi);
        }

        if (preventDuplicates && skippedCount > 0) {
            JsfUtil.addErrorMessage(skippedCount + " duplicate item(s) were skipped. Items already in the purchase order were not added again.");
        }

        calculateBillTotals();
    }

    public void addAllSupplierItems() {
        if (getCurrentBill().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Please Select Dealor");
            return;
        }
        List<Item> allItems = pharmacyBillBean.getItemsForDealor(getCurrentBill().getToInstitution());
        generateBillComponentsForAllSupplierItems(allItems);
    }

    public void addAllSupplierItemsBelowRol() {
        if (getCurrentBill().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Please Select Dealer");
            return;
        }

        String jpql = "SELECT i FROM Item i WHERE i IN "
                + "(SELECT id.item FROM ItemsDistributors id WHERE id.institution = :supplier AND id.retired = false AND id.item.retired = false AND id.item.inactive = false) "
                + "AND i IN "
                + "(SELECT s.itemBatch.item FROM Stock s JOIN Reorder r ON r.item = s.itemBatch.item AND r.department = s.department "
                + "WHERE s.department = :department AND s.stock < r.rol GROUP BY s.itemBatch.item) "
                + "ORDER BY i.name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("supplier", getCurrentBill().getToInstitution());
        parameters.put("department", sessionController.getDepartment());
        List<Item> itemsBelowReorderLevel = itemFacade.findByJpql(jpql, parameters);

        if (itemsBelowReorderLevel == null || itemsBelowReorderLevel.isEmpty()) {
            JsfUtil.addErrorMessage("No items found below reorder level for the selected supplier and department.");
        } else {
            generateBillComponentsForAllSupplierItems(itemsBelowReorderLevel);
        }
    }

    // synchronized: a double-submit on the Save/Finalize button (Enter-key defaultCommand
    // racing a button click, or a resubmitted form) let two requests race through the same
    // in-memory billItems list before either had persisted, so both saw BillItem.id == null
    // and created every line twice, sharing one BillItemFinanceDetails (issue #21417).
    public synchronized void saveRequest() {
        if (!isAuthorized("SAVE", "PurchaseOrderSave")) {
            return;
        }
        boolean saved = saveRequestWithoutMessage();
        if (saved) {
            JsfUtil.addSuccessMessage("Request Saved");
        }
    }

    private boolean saveRequestWithoutMessage() {
        if (getCurrentBill().isChecked()) {
            JsfUtil.addErrorMessage("Cannot save a finalized bill");
            return false;
        }
        if (getCurrentBill().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Please select a supplier");
            return false;
        }

        Institution toInstitution = currentBill.getToInstitution();
        PaymentMethod paymentMethod = currentBill.getPaymentMethod();
        int creditDuration = currentBill.getCreditDuration();
        boolean consignment = currentBill.isConsignment();
        DepartmentType departmentType = currentBill.getDepartmentType();
        String comments = currentBill.getComments();

        if (currentBill.getId() == null) {
            String[] billNumbers = createAndAssignBillNumber();
            long newBillId = purchaseOrderRequestNativeSqlService.createDraftBill(
                    sessionController.getLoggedUser().getDepartment().getId(),
                    sessionController.getLoggedUser().getDepartment().getInstitution().getId(),
                    sessionController.getLoggedUser().getId(),
                    billNumbers[0],
                    billNumbers[1]);
            currentBill = billFacade.find(newBillId);
            // createDraftBill() only inserts department/institution/creater/deptId/insId --
            // toInstitution/paymentMethod/consignment/creditDuration/departmentType/comments
            // are not yet in the DB, so the reload above comes back with those fields
            // null/default. Re-apply the values the user actually entered before they're
            // used below.
            currentBill.setToInstitution(toInstitution);
            currentBill.setPaymentMethod(paymentMethod);
            currentBill.setCreditDuration(creditDuration);
            currentBill.setConsignment(consignment);
            currentBill.setDepartmentType(departmentType);
            currentBill.setComments(comments);
        }

        purchaseOrderRequestNativeSqlService.updateDraftBillHeader(
                currentBill.getId(),
                toInstitution.getId(),
                paymentMethod,
                creditDuration,
                consignment,
                departmentType,
                comments,
                sessionController.getLoggedUser().getId());

        for (BillItem bi : billItems) {
            resyncPharmaceuticalBillItemIfEmpty(bi);
            PurchaseOrderRequestLineData line = toLineData(bi);
            long billItemId = purchaseOrderRequestNativeSqlService.saveLine(currentBill.getId(), line);
            bi.setId(billItemId);
        }

        // Persist bill-level netTotal/total: calculateBillTotals() only mutates
        // the in-memory Bill, and nothing else in the native save path writes
        // these two columns back to the DB (unlike legacy's billFacade.edit()
        // full-entity merge). Recompute from ALL current lines (not just the
        // one being edited) since removeSelected()/finalizeRequest() also
        // route through this method.
        calculateBillTotals();
        purchaseOrderRequestNativeSqlService.updateBillTotals(currentBill.getId(), currentBill.getNetTotal(), currentBill.getTotal());

        return true;
    }

    public synchronized void finalizeRequest() {
        if (!isAuthorized("FINALIZE", "PurchaseOrderFinalize")) {
            return;
        }
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }
        if (currentBill.getToInstitution() == null) {
            JsfUtil.addErrorMessage("Please selectr a supplier");
            return;
        }
        if (currentBill.isChecked()) {
            JsfUtil.addErrorMessage("Cannot finalize an already finalized bill");
            return;
        }
        if (currentBill.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment method.");
            return;
        }
        if (billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please add bill items.");
            return;
        }
        // Every guard below runs against the IN-MEMORY line list, before any
        // native mutation reaches the DB. finalizeBill() sets checked=1 and
        // promotes BILLTYPEATOMIC, and retireZeroQtyLines() retires lines --
        // both commit immediately. Letting them run first and only then
        // discovering the bill had no real quantity left the bill permanently
        // checked=1 with every line retired, and the page disables Save and
        // Finalize once checked=true, so it could never be recovered.
        if (!allBillItemsValid(billItems)) {
            JsfUtil.addErrorMessage("Please ensure each item has quantity and purchase price.");
            return;
        }
        if (calculateTotalBillItemsCount(billItems) == 0) {
            JsfUtil.addErrorMessage("Please enter item quantities for the bill.");
            return;
        }

        // Validation passed -- safe to persist, then promote.
        if (!saveRequestWithoutMessage()) {
            return;
        }

        // Finalize and the per-line zero-qty sweep run as one EJB call so both
        // native writes share a single transaction (see finalizeAndRetireZeroQtyLines).
        purchaseOrderRequestNativeSqlService.finalizeAndRetireZeroQtyLines(currentBill.getId(), sessionController.getLoggedUser().getId());

        currentBill = billFacade.find(currentBill.getId());
        billItems = loadBillItems(currentBill);
        JsfUtil.addSuccessMessage("Request successfully finalized.");
        printPreview = true;
    }

    private PurchaseOrderRequestLineData toLineData(BillItem bi) {
        PurchaseOrderRequestLineData line = new PurchaseOrderRequestLineData();
        line.setBillItemId(bi.getId());
        line.setPharmaceuticalBillItemId(bi.getPharmaceuticalBillItem() != null ? bi.getPharmaceuticalBillItem().getId() : null);
        line.setItemId(bi.getItem().getId());
        line.setAmpp(bi.getItem() instanceof com.divudi.core.entity.pharmacy.Ampp);
        line.setQuantity(bi.getBillItemFinanceDetails().getQuantity());
        line.setFreeQuantity(bi.getBillItemFinanceDetails().getFreeQuantity());
        line.setPurchaseRate(bi.getBillItemFinanceDetails().getLineGrossRate());
        line.setRetailRate(bi.getBillItemFinanceDetails().getRetailSaleRate());
        line.setUnitsPerPack(bi.getBillItemFinanceDetails().getUnitsPerPack());
        line.setSerialNo(bi.getSearialNo());
        line.setCreaterId(sessionController.getLoggedUser().getId());
        return line;
    }

    private BigDecimal getUnitsPerPack(Item item) {
        if (item instanceof Ampp) {
            BigDecimal unitsPerPack = BigDecimal.valueOf(item.getDblValue());
            if (unitsPerPack.doubleValue() > 0) {
                return unitsPerPack;
            }
        }
        return BigDecimal.ONE;
    }

    private Map<Long, Double> fetchLastPurchaseRatesForItems(List<Item> items) {
        Map<Long, Double> ratesByItemId = fetchLastRatesForItems(items,
                "billItemFinanceDetails.lineGrossRate",
                "pharmaceuticalBillItem.itemBatch.purcahseRate",
                "purchase");

        Department dept = getDepartmentLookupScope().department;
        if (dept != null) {
            for (Item item : getItemsMissingRates(getUniqueItemsWithIds(items), ratesByItemId)) {
                double rate = pharmacyBean.getLastPurchaseRate(item, dept, true);
                if (rate > 0.0) {
                    ratesByItemId.put(item.getId(), rate);
                }
            }
        }

        return ratesByItemId;
    }

    private Map<Long, Double> fetchLastRetailRatesForItems(List<Item> items) {
        Map<Long, Double> ratesByItemId = fetchLastRatesForItems(items,
                "billItemFinanceDetails.retailSaleRate",
                "pharmaceuticalBillItem.itemBatch.retailsaleRate",
                "retail");

        Department dept = getDepartmentLookupScope().department;
        if (dept != null) {
            for (Item item : getItemsMissingRates(getUniqueItemsWithIds(items), ratesByItemId)) {
                double rate = pharmacyBean.getLastRetailRate(item, dept, true);
                if (rate > 0.0) {
                    ratesByItemId.put(item.getId(), rate);
                }
            }
        }

        return ratesByItemId;
    }

    private Map<Long, Double> fetchLastRatesForItems(List<Item> items, String financeRatePath, String itemBatchRatePath, String rateLabel) {
        List<Item> lookupItems = getUniqueItemsWithIds(items);
        Map<Long, Double> ratesByItemId = new HashMap<>();
        if (lookupItems.isEmpty()) {
            return ratesByItemId;
        }

        DepartmentLookupScope scope = getDepartmentLookupScope();

        mergeMissingRates(ratesByItemId, fetchScopedFinanceRatesForItems(lookupItems, financeRatePath, rateLabel, "department", scope.department));
        mergeMissingRates(ratesByItemId, fetchScopedFinanceRatesForItems(getItemsMissingRates(lookupItems, ratesByItemId), financeRatePath, rateLabel, "institution", scope.institution));
        mergeMissingRates(ratesByItemId, fetchScopedFinanceRatesForItems(getItemsMissingRates(lookupItems, ratesByItemId), financeRatePath, rateLabel, "global", null));

        mergeMissingItemBatchRates(ratesByItemId, lookupItems, itemBatchRatePath, rateLabel, "department", scope.department);
        mergeMissingItemBatchRates(ratesByItemId, lookupItems, itemBatchRatePath, rateLabel, "institution", scope.institution);
        mergeMissingItemBatchRates(ratesByItemId, lookupItems, itemBatchRatePath, rateLabel, "global", null);

        return ratesByItemId;
    }

    private List<Item> getUniqueItemsWithIds(List<Item> items) {
        Map<Long, Item> uniqueItems = new HashMap<>();
        if (items == null) {
            return new ArrayList<>();
        }
        for (Item item : items) {
            if (item != null && item.getId() != null && !uniqueItems.containsKey(item.getId())) {
                uniqueItems.put(item.getId(), item);
            }
        }
        return new ArrayList<>(uniqueItems.values());
    }

    private List<Item> getItemsMissingRates(List<Item> items, Map<Long, Double> ratesByItemId) {
        List<Item> missingItems = new ArrayList<>();
        if (items == null) {
            return missingItems;
        }
        for (Item item : items) {
            if (item != null && item.getId() != null && getRateForItem(ratesByItemId, item) <= 0.0) {
                missingItems.add(item);
            }
        }
        return missingItems;
    }

    private void mergeMissingRates(Map<Long, Double> ratesByItemId, Map<Long, Double> newRatesByItemId) {
        if (newRatesByItemId == null || newRatesByItemId.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, Double> rateEntry : newRatesByItemId.entrySet()) {
            if (rateEntry.getKey() != null && rateEntry.getValue() != null && rateEntry.getValue() > 0.0
                    && getRateByItemId(ratesByItemId, rateEntry.getKey()) <= 0.0) {
                ratesByItemId.put(rateEntry.getKey(), rateEntry.getValue());
            }
        }
    }

    private Map<Long, Double> fetchScopedFinanceRatesForItems(List<Item> items, String ratePath, String rateLabel, String scope, Object scopeValue) {
        Map<Long, Double> ratesByItemId = new HashMap<>();
        if (items == null || items.isEmpty() || (scopeValue == null && !"global".equals(scope))) {
            return ratesByItemId;
        }

        String rateExpression = "bi." + ratePath;

        String jpql = "SELECT new com.divudi.core.data.dto.PharmacyPurchaseOrderRateDTO("
                + "bi.item.id, " + rateExpression + ", bi.id) "
                + "FROM BillItem bi "
                + "WHERE bi.item.id IN :itemIds "
                + "AND bi.retired = false "
                + "AND bi.bill.cancelled = false "
                + "AND bi.billItemFinanceDetails IS NOT NULL "
                + "AND " + rateExpression + " IS NOT NULL "
                + "AND " + rateExpression + " > 0 "
                + "AND bi.bill.billType IN :billTypes "
                + getScopeCondition(scope, "bi")
                + "ORDER BY bi.id DESC";

        Map<String, Object> params = createRateLookupParameters(items);
        addScopeParameter(params, scope, scopeValue);
        return fetchRateDtos(jpql, params, rateLabel);
    }

    private void mergeMissingItemBatchRates(Map<Long, Double> ratesByItemId, List<Item> originalItems, String ratePath, String rateLabel, String scope, Object scopeValue) {
        List<Item> missingItems = getItemsMissingRates(originalItems, ratesByItemId);
        if (missingItems.isEmpty() || (scopeValue == null && !"global".equals(scope))) {
            return;
        }

        List<Item> batchItems = new ArrayList<>();
        Map<Long, Long> batchItemIdByOriginalItemId = new HashMap<>();
        for (Item item : missingItems) {
            Item batchItem = getItemForItemBatchRate(item);
            if (batchItem != null && batchItem.getId() != null) {
                batchItems.add(batchItem);
                batchItemIdByOriginalItemId.put(item.getId(), batchItem.getId());
            }
        }

        Map<Long, Double> batchRatesByBatchItemId = fetchScopedItemBatchRatesForItems(getUniqueItemsWithIds(batchItems), ratePath, rateLabel, scope, scopeValue);
        for (Item item : missingItems) {
            Long batchItemId = batchItemIdByOriginalItemId.get(item.getId());
            Double rate = batchRatesByBatchItemId.get(batchItemId);
            if (rate != null && rate > 0.0 && getRateForItem(ratesByItemId, item) <= 0.0) {
                ratesByItemId.put(item.getId(), rate);
            }
        }
    }

    private Map<Long, Double> fetchScopedItemBatchRatesForItems(List<Item> items, String ratePath, String rateLabel, String scope, Object scopeValue) {
        Map<Long, Double> ratesByItemId = new HashMap<>();
        if (items == null || items.isEmpty() || (scopeValue == null && !"global".equals(scope))) {
            return ratesByItemId;
        }

        String rateExpression = "bi." + ratePath;

        String jpql = "SELECT new com.divudi.core.data.dto.PharmacyPurchaseOrderRateDTO("
                + "bi.pharmaceuticalBillItem.itemBatch.item.id, " + rateExpression + ", bi.id) "
                + "FROM BillItem bi "
                + "WHERE bi.retired = false "
                + "AND bi.bill.cancelled = false "
                + "AND bi.pharmaceuticalBillItem IS NOT NULL "
                + "AND bi.pharmaceuticalBillItem.itemBatch IS NOT NULL "
                + "AND bi.pharmaceuticalBillItem.itemBatch.item.id IN :itemIds "
                + "AND " + rateExpression + " > 0 "
                + "AND bi.bill.billType IN :billTypes "
                + getScopeCondition(scope, "bi")
                + "ORDER BY bi.id DESC";

        Map<String, Object> params = createRateLookupParameters(items);
        addScopeParameter(params, scope, scopeValue);
        return fetchRateDtos(jpql, params, rateLabel);
    }

    private Map<String, Object> createRateLookupParameters(List<Item> items) {
        List<BillType> purchaseBillTypes = new ArrayList<>();
        purchaseBillTypes.add(BillType.PharmacyGrnBill);
        purchaseBillTypes.add(BillType.PharmacyPurchaseBill);

        Map<String, Object> params = new HashMap<>();
        params.put("itemIds", getItemIds(items));
        params.put("billTypes", purchaseBillTypes);
        return params;
    }

    private List<Long> getItemIds(List<Item> items) {
        List<Long> itemIds = new ArrayList<>();
        if (items == null) {
            return itemIds;
        }
        for (Item item : items) {
            if (item != null && item.getId() != null && !itemIds.contains(item.getId())) {
                itemIds.add(item.getId());
            }
        }
        return itemIds;
    }

    private String getScopeCondition(String scope, String alias) {
        if ("department".equals(scope)) {
            return "AND " + alias + ".bill.department = :department ";
        }
        if ("institution".equals(scope)) {
            return "AND " + alias + ".bill.department.institution = :institution ";
        }
        return "";
    }

    private void addScopeParameter(Map<String, Object> params, String scope, Object scopeValue) {
        if ("department".equals(scope)) {
            params.put("department", scopeValue);
        } else if ("institution".equals(scope)) {
            params.put("institution", scopeValue);
        }
    }

    private Map<Long, Double> fetchRateDtos(String jpql, Map<String, Object> params, String rateLabel) {
        Map<Long, Double> ratesByItemId = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<PharmacyPurchaseOrderRateDTO> results = (List<PharmacyPurchaseOrderRateDTO>) billItemFacade.findLightsByJpql(jpql, params);
            if (results == null) {
                return ratesByItemId;
            }
            for (PharmacyPurchaseOrderRateDTO result : results) {
                if (result != null && result.getItemId() != null && result.getRate() != null && result.getRate() > 0.0
                        && getRateByItemId(ratesByItemId, result.getItemId()) <= 0.0) {
                    ratesByItemId.put(result.getItemId(), result.getRate());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch last " + rateLabel + " rates for purchase order request", e);
        }
        return ratesByItemId;
    }

    private Item getItemForItemBatchRate(Item item) {
        if (item instanceof Ampp) {
            return ((Ampp) item).getAmp();
        }
        return item;
    }

    private double getRateForItem(Map<Long, Double> ratesByItemId, Item item) {
        if (item == null || item.getId() == null) {
            return 0.0;
        }
        return getRateByItemId(ratesByItemId, item.getId());
    }

    private double getRateByItemId(Map<Long, Double> ratesByItemId, Long itemId) {
        if (ratesByItemId == null || itemId == null) {
            return 0.0;
        }
        Double rate = ratesByItemId.get(itemId);
        if (rate == null || rate <= 0.0) {
            return 0.0;
        }
        return rate;
    }

    private DepartmentLookupScope getDepartmentLookupScope() {
        DepartmentLookupScope scope = new DepartmentLookupScope();
        if (sessionController != null && sessionController.getDepartment() != null) {
            scope.department = sessionController.getDepartment();
            scope.institution = sessionController.getDepartment().getInstitution();
        }
        return scope;
    }

    private static class DepartmentLookupScope implements Serializable {

        private static final long serialVersionUID = 1L;

        private Department department;
        private Institution institution;
    }

    /**
     * Computes the department-scoped and institution-scoped bill number
     * strings in a single invocation. Returns {@code String[]{deptId, insId}}.
     * <p>
     * Adapted from legacy's mutate-{@code currentBill}-directly style: at the
     * point this is called in the native flow, {@code createDraftBill()} has
     * not run yet, so there is no bill row to mutate. The exact same strategy
     * branching and the exact same deptId-to-insId fallback relationship
     * (insId normally reuses the just-computed deptId, per legacy
     * {@code PurchaseOrderRequestController.java:830-881}) are preserved.
     * Must be called exactly once per save — deptId and insId are NOT
     * independent draws from the sequence generator.
     */
    private String[] createAndAssignBillNumber() {
        // Check if bill number suffix is configured, if not set default "POR" for Purchase Order Requests
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_PRE, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            // Set default suffix for Purchase Order Requests if not configured
            configOptionApplicationController.setLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_PRE, "POR");
        }

        boolean stratDeptInsYear = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Requests - Prefix + Department Code + Institution Code + Year + Yearly Number and Yearly Number", false);
        boolean stratInsDeptYear = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Requests - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number", false);
        boolean stratInsYear = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Requests - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);
        boolean stratInsIdInsYear = configOptionApplicationController.getBooleanValueByKey("Institution Number Generation Strategy for Purchase Order Requests - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);

        String deptId;
        if (stratDeptInsYear) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
        } else if (stratInsDeptYear) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
        } else if (stratInsYear) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
        } else {
            deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
        }

        String insId;
        if (stratInsIdInsYear) {
            insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
        } else {
            insId = (deptId != null && !deptId.trim().isEmpty()) ? deptId : null;
        }

        return new String[]{deptId, insId};
    }

    public void prepareEmailDialog() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }

        // Set default email if available
        if (currentBill.getToInstitution() != null && currentBill.getToInstitution().getEmail() != null) {
            emailRecipient = currentBill.getToInstitution().getEmail();
        } else {
            emailRecipient = "";
        }
    }

    public void sendPurchaseOrderEmail() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No Bill");
            return;
        }

        if (emailRecipient == null || emailRecipient.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter recipient email");
            return;
        }

        String recipient = emailRecipient.trim();
        if (!CommonFunctions.isValidEmail(recipient)) {
            JsfUtil.addErrorMessage("Please enter a valid email address");
            return;
        }

        String body = generatePurchaseOrderHtml();
        if (body == null) {
            JsfUtil.addErrorMessage("Could not generate email body");
            return;
        }

        AppEmail email = new AppEmail();
        email.setCreatedAt(new Date());
        email.setCreater(sessionController.getLoggedUser());
        email.setReceipientEmail(recipient);
        email.setMessageSubject("Purchase Order Request");
        email.setMessageBody(body);
        email.setDepartment(sessionController.getLoggedUser().getDepartment());
        email.setInstitution(sessionController.getLoggedUser().getInstitution());
        email.setBill(currentBill);
        email.setMessageType(MessageType.Marketing);
        email.setSentSuccessfully(false);
        email.setPending(true);
        emailFacade.create(email);

        try {
            boolean success = emailManagerEjb.sendEmail(
                    java.util.Collections.singletonList(recipient),
                    body,
                    "Purchase Order Request",
                    true
            );
            email.setSentSuccessfully(success);
            email.setPending(!success);
            if (success) {
                email.setSentAt(new Date());
                JsfUtil.addSuccessMessage("Email Sent Successfully");
            } else {
                JsfUtil.addErrorMessage("Sending Email Failed");
            }
            emailFacade.edit(email);
        } catch (Exception ex) {
            JsfUtil.addErrorMessage("Sending Email Failed");
        }
    }

    /**
     * HTML-escapes free-text values (item/institution/person names, addresses,
     * phone numbers) before they are concatenated into the email body — those
     * fields are staff-editable and would otherwise let stray HTML/script
     * markup reach the outbound email unescaped.
     */
    private static String esc(String value) {
        return value != null ? org.apache.commons.text.StringEscapeUtils.escapeHtml4(value) : "";
    }

    private String generatePurchaseOrderHtml() {
        try {
            if (currentBill == null) {
                LOGGER.log(Level.SEVERE, "Current bill is null when generating purchase order HTML");
                return null;
            }

            StringBuilder html = new StringBuilder();
            html.append("<html><head><title>Purchase Order Request</title></head><body>");
            html.append("<div style='font-family: Arial, sans-serif; padding: 20px;'>");

            // Institution header
            if (currentBill.getCreater() != null && currentBill.getCreater().getInstitution() != null) {
                html.append("<div style='text-align: center; margin-bottom: 20px;'>");
                html.append("<h2>").append(esc(currentBill.getCreater().getInstitution().getName())).append("</h2>");
                if (currentBill.getCreater().getInstitution().getAddress() != null) {
                    html.append("<p>").append(esc(currentBill.getCreater().getInstitution().getAddress())).append("</p>");
                }
                if (currentBill.getCreater().getInstitution().getPhone() != null) {
                    html.append("<p>Phone: ").append(esc(currentBill.getCreater().getInstitution().getPhone())).append("</p>");
                }
                html.append("</div>");
            }

            html.append("<h3 style='text-align: center; text-decoration: underline;'>Purchase Order Request</h3>");

            // Order details
            html.append("<table style='width: 100%; margin-bottom: 20px;'>");
            html.append("<tr><td><strong>Order No:</strong></td><td>").append(esc(currentBill.getDeptId())).append("</td></tr>");
            if (currentBill.getDepartment() != null) {
                html.append("<tr><td><strong>Order Department:</strong></td><td>").append(esc(currentBill.getDepartment().getName())).append("</td></tr>");
            }
            if (currentBill.getToInstitution() != null) {
                html.append("<tr><td><strong>Supplier:</strong></td><td>").append(esc(currentBill.getToInstitution().getName())).append("</td></tr>");
                html.append("<tr><td><strong>Supplier Code:</strong></td><td>").append(esc(currentBill.getToInstitution().getCode())).append("</td></tr>");
                if (currentBill.getToInstitution().getPhone() != null) {
                    html.append("<tr><td><strong>Supplier Phone:</strong></td><td>").append(esc(currentBill.getToInstitution().getPhone())).append("</td></tr>");
                }
                if (currentBill.getToInstitution().getAddress() != null) {
                    html.append("<tr><td><strong>Supplier Address:</strong></td><td>").append(esc(currentBill.getToInstitution().getAddress())).append("</td></tr>");
                }
            }
            html.append("<tr><td><strong>Payment Method:</strong></td><td>").append(currentBill.getPaymentMethod() != null ? currentBill.getPaymentMethod().toString() : "").append("</td></tr>");
            html.append("<tr><td><strong>Consignment:</strong></td><td>").append(currentBill.isConsignment() ? "Yes" : "No").append("</td></tr>");
            html.append("</table>");

            // Items table
            html.append("<table border='1' style='width: 100%; border-collapse: collapse; margin-bottom: 20px;'>");
            html.append("<thead style='background-color: #f0f0f0;'>");
            html.append("<tr>");
            html.append("<th style='padding: 8px;'>Item Code</th>");
            html.append("<th style='padding: 8px;'>Item Name</th>");
            html.append("<th style='padding: 8px;'>Qty</th>");
            html.append("<th style='padding: 8px;'>Free Qty</th>");
            html.append("<th style='padding: 8px;'>Purchase Rate</th>");
            html.append("<th style='padding: 8px;'>Purchase Value</th>");
            html.append("</tr></thead><tbody>");

            if (billItems != null) {
                for (BillItem bi : billItems) {
                    if (bi != null && !bi.isRetired() && bi.getItem() != null) {
                        html.append("<tr>");
                        html.append("<td style='padding: 8px;'>").append(esc(bi.getItem().getCode())).append("</td>");
                        html.append("<td style='padding: 8px;'>").append(esc(bi.getItem().getName())).append("</td>");
                        html.append("<td style='padding: 8px; text-align: right;'>");
                        if (bi.getPharmaceuticalBillItem() != null) {
                            html.append(String.format("%,.0f", bi.getPharmaceuticalBillItem().getQty()));
                        }
                        html.append("</td>");
                        html.append("<td style='padding: 8px; text-align: right;'>");
                        if (bi.getPharmaceuticalBillItem() != null) {
                            html.append(String.format("%,.0f", bi.getPharmaceuticalBillItem().getFreeQty()));
                        }
                        html.append("</td>");
                        html.append("<td style='padding: 8px; text-align: right;'>");
                        if (bi.getPharmaceuticalBillItem() != null) {
                            html.append(String.format("%,.2f", bi.getPharmaceuticalBillItem().getPurchaseRate()));
                        }
                        html.append("</td>");
                        html.append("<td style='padding: 8px; text-align: right;'>").append(String.format("%,.2f", bi.getNetValue())).append("</td>");
                        html.append("</tr>");
                    }
                }
            }

            html.append("</tbody>");
            html.append("<tfoot style='font-weight: bold;'>");
            html.append("<tr>");
            html.append("<td colspan='5' style='padding: 8px; text-align: right;'>Net Total:</td>");
            html.append("<td style='padding: 8px; text-align: right;'>").append(String.format("%,.2f", currentBill.getNetTotal())).append("</td>");
            html.append("</tr></tfoot></table>");

            // Footer details
            html.append("<div style='margin-top: 20px;'>");
            if (currentBill.getCreater() != null && currentBill.getCreater().getWebUserPerson() != null) {
                html.append("<p><strong>Order Initiated By:</strong> ").append(esc(currentBill.getCreater().getWebUserPerson().getName())).append("</p>");
            }
            if (currentBill.getCheckedBy() != null) {
                html.append("<p><strong>Order Finalized By:</strong> ").append(esc(currentBill.getCheckedBy().getName())).append("</p>");
            }
            if (currentBill.getCheckeAt() != null) {
                html.append("<p><strong>Order Finalized At:</strong> ").append(CommonFunctions.formatDate(currentBill.getCheckeAt(), "dd/MM/yyyy HH:mm:ss")).append("</p>");
            }
            html.append("<p><strong>Generated At:</strong> ").append(CommonFunctions.formatDate(new Date(), "dd/MM/yyyy HH:mm:ss")).append("</p>");
            html.append("<p><strong>Total:</strong> ").append(String.format("%,.2f", currentBill.getNetTotal())).append("</p>");
            html.append("</div>");

            html.append("</div></body></html>");
            return html.toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating purchase order HTML", e);
            return null;
        }
    }

    // Getters/setters
    /**
     * Lazily rebuilds a fresh draft bill when the field is null.
     * <p>
     * The three "New Order" buttons on the page bind straight to
     * {@code resetBillValues()}, which nulls {@code currentBill} without
     * recreating it. Every writable header binding on the page
     * ({@code currentBill.toInstitution}, {@code .paymentMethod},
     * {@code .consignment}, {@code .comments}, {@code .departmentType},
     * {@code .creditDuration}) then resolves against null and fails on the next
     * form interaction. Reconstructing here — with the same config-driven
     * defaults legacy applies — makes "New Order" leave the page immediately
     * usable. Mirrors legacy PurchaseOrderRequestController.getCurrentBill().
     */
    public Bill getCurrentBill() {
        if (currentBill == null) {
            currentBill = new Bill();
            currentBill.setBillType(BillType.PharmacyOrder);
            currentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER);

            String strEnumValue = configOptionApplicationController.getEnumValueByKey("Pharmacy Purchase Order Default Payment Method");
            PaymentMethod pm = enumController.getEnumValue(PaymentMethod.class, strEnumValue);
            currentBill.setPaymentMethod(pm);

            boolean consignmentEnabled = configOptionApplicationController.getBooleanValueByKey(
                    "Consignment Option is checked in new Pharmacy Purchasing Bills", false);
            currentBill.setConsignment(consignmentEnabled);
        }
        return currentBill;
    }

    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
    }

    public BillItem getCurrentBillItem() {
        return currentBillItem;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public boolean isItemHistoryVisible() {
        return itemHistoryVisible;
    }

    public void setItemHistoryVisible(boolean itemHistoryVisible) {
        this.itemHistoryVisible = itemHistoryVisible;
    }

    public void displayItemDetails(BillItem bi) {
        pharmacyController.fillItemDetails(bi.getItem());
        itemHistoryVisible = true;
    }

    public void closeItemHistory() {
        itemHistoryVisible = false;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getEmailRecipient() {
        return emailRecipient;
    }

    public void setEmailRecipient(String emailRecipient) {
        this.emailRecipient = emailRecipient;
    }

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

        PageMetadata metadata = new PageMetadata(
                "pharmacy/pharmacy_purhcase_order_request_native",
                "Pharmacy Purchase Order Request (Native)",
                "Create and manage pharmacy purchase order requests to suppliers",
                "PurchaseOrderRequestNativeSqlController"
        );

        // Configuration Options - APPLICATION scope
        metadata.addConfigOption(new ConfigOptionInfo(
                "Pharmacy PO - Show Item History Above Items",
                "Controls whether item history panel appears above or below the items datatable",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Enable Consignment in Pharmacy Purchasing",
                "Shows or hides the consignment checkbox option in the purchase order panel",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Minimum Number of Characters to Search for Item",
                "Minimum characters required before item autocomplete search triggers",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Prevent Duplicate Items in Purchase Orders",
                "When enabled, prevents adding duplicate items to a purchase order",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Consignment Option is checked in new Pharmacy Purchasing Bills",
                "Sets the default checked state of the consignment checkbox when creating a new purchase order",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Pharmacy Purchase Order Default Payment Method",
                "Sets the default payment method when creating a new purchase order",
                OptionScope.APPLICATION
        ));

        // Bill Number Generation Strategies
        metadata.addConfigOption(new ConfigOptionInfo(
                "Bill Number Suffix for PHARMACY_ORDER_PRE",
                "Custom suffix for purchase order request bill numbers (default: POR)",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Bill Number Generation Strategy for Purchase Order Requests - Prefix + Department Code + Institution Code + Year + Yearly Number and Yearly Number",
                "Bill numbering format: Prefix-DeptCode-InstCode-Year-Number",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Bill Number Generation Strategy for Purchase Order Requests - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number",
                "Bill numbering format: Prefix-InstCode-DeptCode-Year-Number",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Bill Number Generation Strategy for Purchase Order Requests - Prefix + Institution Code + Year + Yearly Number and Yearly Number",
                "Bill numbering format: Prefix-InstCode-Year-Number (institution-wide)",
                OptionScope.APPLICATION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Institution Number Generation Strategy for Purchase Order Requests - Prefix + Institution Code + Year + Yearly Number and Yearly Number",
                "Controls separate institution-wide number generation for the insId field",
                OptionScope.APPLICATION
        ));

        // Configuration Options - INSTITUTION scope
        metadata.addConfigOption(new ConfigOptionInfo(
                "Pharmacy Purchase Order Request Print - A4 Paper Custom 1",
                "Uses A4 Paper Custom Format 1 for purchase order request printing",
                OptionScope.INSTITUTION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Pharmacy Purchase Order Request Print - A4 Paper Custom 2",
                "Uses A4 Paper Custom Format 2 for purchase order request printing",
                OptionScope.INSTITUTION
        ));
        metadata.addConfigOption(new ConfigOptionInfo(
                "Pharmacy Purchase - Quantity Must Be Integer",
                "Validates that quantity and free quantity values are whole numbers (no decimals)",
                OptionScope.INSTITUTION
        ));

        // Privileges
        metadata.addPrivilege(new PrivilegeInfo(
                "Admin",
                "Administrative access to configuration interface",
                "Controls visibility of the Config button"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "CreatePurchaseOrder",
                "Permission to create and manage purchase orders",
                "Controls access to the entire purchase order request page"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "PurchaseOrderSave",
                "Permission to save purchase order requests",
                "Controls the Save button and Remove Selected action"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "PurchaseOrderFinalize",
                "Permission to finalize purchase order requests",
                "Controls the Finalize button"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "Developers",
                "Developer-only features",
                "Controls visibility of item ID column in autocomplete"
        ));
        metadata.addPrivilege(new PrivilegeInfo(
                "ChangeReceiptPrintingPaperTypes",
                "Access to receipt printing configuration settings",
                "Controls visibility of the Settings button in print preview"
        ));

        pageMetadataRegistry.registerPage(metadata);
    }
}
