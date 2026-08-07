/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
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

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ConfigOptionController configOptionController;
    @Inject
    private ItemController itemController;

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
        currentBill = new Bill();
        return "/pharmacy/pharmacy_purhcase_order_request_native?faces-redirect=true";
    }

    public String navigateToUpdatePurchaseOrder(Long billId) {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        resetBillValues();
        this.billId = billId;
        currentBill = billFacade.find(billId);
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return "";
        }
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

    private void resetBillValues() {
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
        if (currentBillItem.getItem() == null) {
            JsfUtil.addErrorMessage("Please select and item from the list");
            return;
        }

        if (currentBill.getDepartmentType() == null) {
            currentBill.setDepartmentType(currentBillItem.getItem().getDepartmentType() != null
                    ? currentBillItem.getItem().getDepartmentType() : DepartmentType.Pharmacy);
        }

        DepartmentType itemDepartmentType = currentBillItem.getItem().getDepartmentType();
        if (itemDepartmentType != null && !itemDepartmentType.equals(currentBill.getDepartmentType())) {
            JsfUtil.addErrorMessage("Cannot add items from different department types. "
                    + "Bill is set for " + currentBill.getDepartmentType().getLabel()
                    + " items, but you are trying to add a " + itemDepartmentType.getLabel() + " item.");
            return;
        }

        List<DepartmentType> allowedTypes = sessionController.getAvailableDepartmentTypesForPharmacyTransactions();
        if (allowedTypes == null || !allowedTypes.contains(currentBill.getDepartmentType())) {
            JsfUtil.addErrorMessage("Items are not allowed for the selected department type: " + currentBill.getDepartmentType().getLabel());
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
        bi.setRetired(true);
        if (bi.getId() != null) {
            purchaseOrderRequestNativeSqlService.retireLine(bi.getId(), sessionController.getLoggedUser().getId());
        }
        billItems.remove(bi);
        calculateBillTotals();
        itemHistoryVisible = false;
    }

    public void onEdit(BillItem bi) {
        if (configOptionController.getBooleanValueByKey("Pharmacy Purchase - Quantity Must Be Integer", true)) {
            java.math.BigDecimal qty = bi.getBillItemFinanceDetails().getQuantity();
            java.math.BigDecimal freeQty = bi.getBillItemFinanceDetails().getFreeQuantity();
            if (qty != null && qty.remainder(java.math.BigDecimal.ONE).compareTo(java.math.BigDecimal.ZERO) != 0) {
                bi.getBillItemFinanceDetails().setQuantity(java.math.BigDecimal.ZERO);
                JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for quantity. Decimal values are not allowed.");
                calculateBillTotals();
                return;
            }
            if (freeQty != null && freeQty.remainder(java.math.BigDecimal.ONE).compareTo(java.math.BigDecimal.ZERO) != 0) {
                bi.getBillItemFinanceDetails().setFreeQuantity(java.math.BigDecimal.ZERO);
                JsfUtil.addErrorMessage("Please enter only whole numbers (integers) for free quantity. Decimal values are not allowed.");
                calculateBillTotals();
                return;
            }
        }
        calculateBillTotals();
    }

    private void calculateBillTotals() {
        double total = 0.0;
        for (BillItem bi : billItems) {
            if (bi != null && !bi.isRetired()) {
                total += bi.getNetValue();
            }
        }
        currentBill.setNetTotal(total);
        currentBill.setTotal(total);
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

    private void createAndAssignBillNumber() {
        // Check if bill number suffix is configured, if not set default "POR" for Purchase Order Requests
        String billSuffix = configOptionApplicationController.getLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_PRE, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            // Set default suffix for Purchase Order Requests if not configured
            configOptionApplicationController.setLongTextValueByKey("Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_PRE, "POR");
        }

        boolean billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Requests - Prefix + Department Code + Institution Code + Year + Yearly Number and Yearly Number", false);
        boolean billNumberGenerationStrategyForDepartmentIdIsPrefixInsDeptYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Requests - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number", false);
        boolean billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Purchase Order Requests - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);
        boolean billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount = configOptionApplicationController.getBooleanValueByKey("Institution Number Generation Strategy for Purchase Order Requests - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);

        String billId = "";

        if (billNumberGenerationStrategyForDepartmentIdIsPrefixDeptInsYearCount) {
            if (getCurrentBill().getDeptId() == null || getCurrentBill().getDeptId().trim().equals("")) {
                billId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
                getCurrentBill().setDeptId(billId);
            }
        } else if (billNumberGenerationStrategyForDepartmentIdIsPrefixInsDeptYearCount) {
            if (getCurrentBill().getDeptId() == null || getCurrentBill().getDeptId().trim().equals("")) {
                billId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
                getCurrentBill().setDeptId(billId);
            }
        } else if (billNumberGenerationStrategyForDepartmentIdIsPrefixInsYearCount) {
            if (getCurrentBill().getDeptId() == null || getCurrentBill().getDeptId().trim().equals("")) {
                billId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
                getCurrentBill().setDeptId(billId);
            }
        } else {
            //Keep Legacy Method intact without any changes
            if (getCurrentBill().getDeptId() == null || getCurrentBill().getDeptId().trim().equals("")) {
                billId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
                getCurrentBill().setDeptId(billId);
            }
        }

        if (billNumberGenerationStrategyForInstitutionIdIsPrefixInsYearCount) {
            if (getCurrentBill().getInsId() == null || getCurrentBill().getInsId().trim().equals("")) {
                String insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
                getCurrentBill().setInsId(insId);
            }
        } else {
            //Keep Legacy Method intact without any changes
            if (getCurrentBill().getInsId() == null || getCurrentBill().getInsId().trim().equals("")) {
                if (billId != null && !billId.trim().isEmpty()) {
                    getCurrentBill().setInsId(billId);
                }
            }
        }
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
                html.append("<h2>").append(currentBill.getCreater().getInstitution().getName() != null ? currentBill.getCreater().getInstitution().getName() : "").append("</h2>");
                if (currentBill.getCreater().getInstitution().getAddress() != null) {
                    html.append("<p>").append(currentBill.getCreater().getInstitution().getAddress()).append("</p>");
                }
                if (currentBill.getCreater().getInstitution().getPhone() != null) {
                    html.append("<p>Phone: ").append(currentBill.getCreater().getInstitution().getPhone()).append("</p>");
                }
                html.append("</div>");
            }

            html.append("<h3 style='text-align: center; text-decoration: underline;'>Purchase Order Request</h3>");

            // Order details
            html.append("<table style='width: 100%; margin-bottom: 20px;'>");
            html.append("<tr><td><strong>Order No:</strong></td><td>").append(currentBill.getDeptId() != null ? currentBill.getDeptId() : "").append("</td></tr>");
            if (currentBill.getDepartment() != null) {
                html.append("<tr><td><strong>Order Department:</strong></td><td>").append(currentBill.getDepartment().getName() != null ? currentBill.getDepartment().getName() : "").append("</td></tr>");
            }
            if (currentBill.getToInstitution() != null) {
                html.append("<tr><td><strong>Supplier:</strong></td><td>").append(currentBill.getToInstitution().getName() != null ? currentBill.getToInstitution().getName() : "").append("</td></tr>");
                html.append("<tr><td><strong>Supplier Code:</strong></td><td>").append(currentBill.getToInstitution().getCode() != null ? currentBill.getToInstitution().getCode() : "").append("</td></tr>");
                if (currentBill.getToInstitution().getPhone() != null) {
                    html.append("<tr><td><strong>Supplier Phone:</strong></td><td>").append(currentBill.getToInstitution().getPhone()).append("</td></tr>");
                }
                if (currentBill.getToInstitution().getAddress() != null) {
                    html.append("<tr><td><strong>Supplier Address:</strong></td><td>").append(currentBill.getToInstitution().getAddress()).append("</td></tr>");
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
                        html.append("<td style='padding: 8px;'>").append(bi.getItem().getCode() != null ? bi.getItem().getCode() : "").append("</td>");
                        html.append("<td style='padding: 8px;'>").append(bi.getItem().getName() != null ? bi.getItem().getName() : "").append("</td>");
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
                html.append("<p><strong>Order Initiated By:</strong> ").append(currentBill.getCreater().getWebUserPerson().getName() != null ? currentBill.getCreater().getWebUserPerson().getName() : "").append("</p>");
            }
            if (currentBill.getCheckedBy() != null) {
                html.append("<p><strong>Order Finalized By:</strong> ").append(currentBill.getCheckedBy().getName() != null ? currentBill.getCheckedBy().getName() : "").append("</p>");
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
    public Bill getCurrentBill() {
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
}
