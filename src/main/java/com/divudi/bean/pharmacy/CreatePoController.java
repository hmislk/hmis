/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ConfigOptionController;
import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.NotificationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.ItemSupplierDto;
import com.divudi.core.data.dto.PurchaseOrderItemDto;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.service.BillService;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Optimized Purchase Order Controller using DTOs for improved performance
 * Parallel implementation to PurchaseOrderRequestController
 *
 * Performance Optimizations:
 * - Pre-calculates stock and usage data when loading items
 * - Uses DTOs for display to avoid per-row entity method calls
 * - Maintains entity-based save logic for data integrity
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class CreatePoController implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(CreatePoController.class.getName());
    private static final double MIN_PURCHASE_RATE = 0.00001;

    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillService billService;

    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private EnumController enumController;
    @Inject
    private ConfigOptionController configOptionController;
    @Inject
    private PharmacyCalculation pharmacyBillBean;
    @Inject
    private NotificationController notificationController;

    // Entity-based properties (for save operations)
    private Bill currentBill;
    private BillItem currentBillItem;
    private List<BillItem> billItems;
    private List<BillItem> selectedBillItems;

    // DTO-based properties (for display - performance optimization)
    private List<PurchaseOrderItemDto> itemDtos;
    private List<PurchaseOrderItemDto> selectedItemDtos;
    private List<ItemSupplierDto> dealerItemDtos;
    private ItemSupplierDto selectedDealerItem;

    // UI state
    private boolean printPreview;
    private double totalBillItemsCount;

    public CreatePoController() {
    }

    /**
     * Reset all values for a new purchase order
     */
    public void resetBillValues() {
        currentBill = null;
        currentBillItem = null;
        billItems = null;
        itemDtos = null;
        selectedItemDtos = null;
        dealerItemDtos = null;
        selectedDealerItem = null;
        printPreview = false;
    }

    /**
     * Navigate to create new PO page
     */
    public String navigateToCreateNewPurchaseOrder() {
        resetBillValues();
        getCurrentBill();
        return "/pharmacy/create_po?faces-redirect=true";
    }

    /**
     * Get current bill - creates new if doesn't exist
     */
    public Bill getCurrentBill() {
        if (currentBill == null) {
            currentBill = new BilledBill();
            currentBill.setBillType(BillType.PharmacyOrder);
            currentBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER);

            // Set default payment method from config
            String key = "Pharmacy Purchase Order Default Payment Method";
            String strEnumValue = configOptionApplicationController.getEnumValueByKey(key);
            PaymentMethod pm = enumController.getEnumValue(PaymentMethod.class, strEnumValue);
            currentBill.setPaymentMethod(pm);

            // Set consignment flag from config
            boolean consignmentEnabled = configOptionApplicationController.getBooleanValueByKey(
                    "Consignment Option is checked in new Pharmacy Purchasing Bills", false);
            currentBill.setConsignment(consignmentEnabled);
        }
        return currentBill;
    }

    /**
     * Get bill items - creates empty list if doesn't exist
     */
    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        return billItems;
    }

    /**
     * Get item DTOs for display (performance optimized)
     */
    public List<PurchaseOrderItemDto> getItemDtos() {
        if (itemDtos == null) {
            loadItemDtosFromEntities();
        }
        return itemDtos;
    }

    /**
     * Load DTOs from current bill items with pre-calculated stock and usage
     * This is the key performance optimization - stock and usage are calculated
     * once when loading instead of per-row in the UI
     */
    private void loadItemDtosFromEntities() {
        itemDtos = new ArrayList<>();
        if (getBillItems().isEmpty()) {
            return;
        }

        // Build list of item IDs for batch stock/usage query
        List<Long> itemIds = new ArrayList<>();
        for (BillItem bi : getBillItems()) {
            if (bi != null && !bi.isRetired() && bi.getItem() != null) {
                itemIds.add(bi.getItem().getId());
            }
        }

        if (itemIds.isEmpty()) {
            return;
        }

        // Batch query for stock and usage data (performance optimization)
        Map<Long, Double> stockMap = fetchStockDataForItems(itemIds);
        Map<Long, Double> usageMap = fetchUsageDataForItems(itemIds);

        // Create DTOs with pre-calculated data
        int serialNo = 0;
        for (BillItem bi : getBillItems()) {
            if (bi == null || bi.isRetired() || bi.getItem() == null) {
                continue;
            }

            PurchaseOrderItemDto dto = new PurchaseOrderItemDto();
            dto.setBillItemId(bi.getId());
            dto.setSerialNo(serialNo++);
            dto.setItemId(bi.getItem().getId());
            dto.setItemName(bi.getItem().getName());
            dto.setItemCode(bi.getItem().getCode());

            // Financial details
            if (bi.getBillItemFinanceDetails() != null) {
                dto.setQuantity(bi.getBillItemFinanceDetails().getQuantity());
                dto.setFreeQuantity(bi.getBillItemFinanceDetails().getFreeQuantity());
                dto.setPurchaseRate(bi.getBillItemFinanceDetails().getLineGrossRate());
                dto.setRetailRate(bi.getBillItemFinanceDetails().getRetailSaleRate());
                dto.setLineTotal(bi.getBillItemFinanceDetails().getLineNetTotal());
                dto.setUnitsPerPack(bi.getBillItemFinanceDetails().getUnitsPerPack());
            }

            // Pre-calculated stock and usage (from batch query)
            Long itemId = bi.getItem().getId();
            dto.setStockInUnits(stockMap.getOrDefault(itemId, 0.0));
            dto.setUsageCount(usageMap.getOrDefault(itemId, 0.0));

            itemDtos.add(dto);
        }
    }

    /**
     * Batch query for stock data - prevents N+1 query problem
     */
    private Map<Long, Double> fetchStockDataForItems(List<Long> itemIds) {
        Map<Long, Double> stockMap = new HashMap<>();

        String jpql = "SELECT i.id, COALESCE(SUM(s.stock), 0.0) "
                + "FROM Item i "
                + "LEFT JOIN Stock s ON s.itemBatch.item.id = i.id "
                + "WHERE i.id IN :itemIds "
                + "AND i.retired = false "
                + "GROUP BY i.id";

        Map<String, Object> params = new HashMap<>();
        params.put("itemIds", itemIds);

        @SuppressWarnings("unchecked")
        List<Object[]> results = (List<Object[]>) itemFacade.findLightsByJpql(jpql, params);

        if (results != null) {
            for (Object[] row : results) {
                Long itemId = (Long) row[0];
                Double stock = (Double) row[1];
                stockMap.put(itemId, stock != null ? stock : 0.0);
            }
        }

        return stockMap;
    }

    /**
     * Batch query for usage data - prevents N+1 query problem
     */
    private Map<Long, Double> fetchUsageDataForItems(List<Long> itemIds) {
        Map<Long, Double> usageMap = new HashMap<>();

        // Get from and to dates from pharmacy controller or default to last 30 days
        Date toDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date fromDate = cal.getTime();

        String jpql = "SELECT i.id, COALESCE(SUM(ABS(pbi.qty)), 0.0) "
                + "FROM Item i "
                + "LEFT JOIN BillItem bi ON bi.item.id = i.id "
                + "LEFT JOIN PharmaceuticalBillItem pbi ON pbi.billItem.id = bi.id "
                + "WHERE i.id IN :itemIds "
                + "AND bi.retired = false "
                + "AND bi.bill.billType IN ('PharmacyBhtPre', 'PharmacyPre') "
                + "AND bi.bill.createdAt BETWEEN :fromDate AND :toDate "
                + "GROUP BY i.id";

        Map<String, Object> params = new HashMap<>();
        params.put("itemIds", itemIds);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = (List<Object[]>) itemFacade.findLightsByJpql(jpql, params);

        if (results != null) {
            for (Object[] row : results) {
                Long itemId = (Long) row[0];
                Double usage = (Double) row[1];
                usageMap.put(itemId, usage != null ? usage : 0.0);
            }
        }

        return usageMap;
    }

    /**
     * Load dealer items as DTOs with pre-calculated stock
     * Called when supplier is selected
     */
    public void loadDealerItems() {
        dealerItemDtos = new ArrayList<>();

        if (getCurrentBill().getToInstitution() == null) {
            return;
        }

        String jpql = "SELECT NEW com.divudi.core.data.dto.ItemSupplierDto("
                + "i.id, i.name, i.code, "
                + "COALESCE(SUM(s.stock), 0.0), "
                + "i.dblValue) "
                + "FROM ItemsDistributors id "
                + "JOIN id.item i "
                + "LEFT JOIN Stock s ON s.itemBatch.item.id = i.id AND s.department = :department "
                + "WHERE id.institution = :supplier "
                + "AND id.retired = false "
                + "AND i.retired = false "
                + "GROUP BY i.id, i.name, i.code, i.dblValue "
                + "ORDER BY i.name";

        Map<String, Object> params = new HashMap<>();
        params.put("supplier", getCurrentBill().getToInstitution());
        params.put("department", sessionController.getDepartment());

        dealerItemDtos = (List<ItemSupplierDto>) itemFacade.findLightsByJpql(jpql, params, null, 500);

        // Batch load usage data for these items
        if (!dealerItemDtos.isEmpty()) {
            List<Long> itemIds = new ArrayList<>();
            for (ItemSupplierDto dto : dealerItemDtos) {
                itemIds.add(dto.getItemId());
            }
            Map<Long, Double> usageMap = fetchUsageDataForItems(itemIds);

            // Set usage data in DTOs
            for (ItemSupplierDto dto : dealerItemDtos) {
                dto.setUsageCount(usageMap.getOrDefault(dto.getItemId(), 0.0));
            }
        }
    }

    /**
     * Get current bill item - creates new if doesn't exist
     */
    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
        }
        return currentBillItem;
    }

    /**
     * Add item to purchase order (for any item autocomplete)
     */
    public void addItem() {
        if (getCurrentBillItem().getItem() == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }

        // Check for duplicates if config enabled
        if (configOptionApplicationController.getBooleanValueByKey(
                "Prevent Duplicate Items in Purchase Orders", false)) {
            for (BillItem existingItem : getBillItems()) {
                if (existingItem != null && !existingItem.isRetired()
                        && existingItem.getItem() != null
                        && existingItem.getItem().getId().equals(getCurrentBillItem().getItem().getId())) {
                    JsfUtil.addErrorMessage("This item has already been added. Please update the existing entry.");
                    return;
                }
            }
        }

        Item item = getCurrentBillItem().getItem();

        // Create bill item entity
        BillItem bi = new BillItem();
        bi.setItem(item);
        bi.setSearialNo(getBillItems().size());

        // Create pharmaceutical bill item
        PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
        pbi.setBillItem(bi);
        bi.setPharmaceuticalBillItem(pbi);

        // Set last purchase and retail rates
        pbi.setPurchaseRate(pharmacyBean.getLastPurchaseRate(item, sessionController.getDepartment()));
        pbi.setRetailRate(pharmacyBean.getLastRetailRate(item, sessionController.getDepartment()));

        // Set units per pack
        if (item instanceof Ampp) {
            BigDecimal unitsPerPack = BigDecimal.valueOf(item.getDblValue());
            if (unitsPerPack == null || unitsPerPack.doubleValue() <= 0) {
                unitsPerPack = BigDecimal.ONE;
            }
            bi.getBillItemFinanceDetails().setUnitsPerPack(unitsPerPack);
        } else {
            bi.getBillItemFinanceDetails().setUnitsPerPack(BigDecimal.ONE);
        }

        // Set purchase rate in finance details
        bi.getBillItemFinanceDetails().setLineGrossRate(BigDecimal.valueOf(pbi.getPurchaseRate()));
        bi.getBillItemFinanceDetails().setLineNetRate(bi.getBillItemFinanceDetails().getLineGrossRate());

        // Add to bill items
        getBillItems().add(bi);

        // Refresh DTOs
        itemDtos = null;

        // Clear current bill item
        currentBillItem = null;

        calculateBillTotals();
    }

    /**
     * Add selected dealer item to purchase order
     */
    public void addDealerItem() {
        if (selectedDealerItem == null) {
            JsfUtil.addErrorMessage("Please select an item from the list");
            return;
        }

        // Check for duplicates if config enabled
        if (configOptionApplicationController.getBooleanValueByKey(
                "Prevent Duplicate Items in Purchase Orders", false)) {
            for (BillItem existingItem : getBillItems()) {
                if (existingItem != null && !existingItem.isRetired()
                        && existingItem.getItem() != null
                        && existingItem.getItem().getId().equals(selectedDealerItem.getItemId())) {
                    JsfUtil.addErrorMessage("This item has already been added. Please update the existing entry.");
                    return;
                }
            }
        }

        // Load full item entity for save operation
        Item item = itemFacade.find(selectedDealerItem.getItemId());
        if (item == null) {
            JsfUtil.addErrorMessage("Item not found");
            return;
        }

        // Create bill item entity (required for persistence)
        BillItem bi = new BillItem();
        bi.setItem(item);
        bi.setSearialNo(getBillItems().size());

        // Create pharmaceutical bill item
        PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
        pbi.setBillItem(bi);
        bi.setPharmaceuticalBillItem(pbi);

        // Set last purchase and retail rates
        pbi.setPurchaseRate(pharmacyBean.getLastPurchaseRate(item, sessionController.getDepartment()));
        pbi.setRetailRate(pharmacyBean.getLastRetailRate(item, sessionController.getDepartment()));

        // Set units per pack
        if (item instanceof Ampp) {
            BigDecimal unitsPerPack = BigDecimal.valueOf(item.getDblValue());
            if (unitsPerPack == null || unitsPerPack.doubleValue() <= 0) {
                unitsPerPack = BigDecimal.ONE;
            }
            bi.getBillItemFinanceDetails().setUnitsPerPack(unitsPerPack);
        } else {
            bi.getBillItemFinanceDetails().setUnitsPerPack(BigDecimal.ONE);
        }

        // Set purchase rate in finance details
        bi.getBillItemFinanceDetails().setLineGrossRate(BigDecimal.valueOf(pbi.getPurchaseRate()));
        bi.getBillItemFinanceDetails().setLineNetRate(bi.getBillItemFinanceDetails().getLineGrossRate());

        // Add to bill items
        getBillItems().add(bi);

        // Refresh DTOs
        itemDtos = null;

        // Clear selection
        selectedDealerItem = null;

        calculateBillTotals();
    }

    /**
     * Add all supplier items to PO
     */
    public void addAllSupplierItems() {
        if (getCurrentBill().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Please select supplier");
            return;
        }

        // Load dealer items if not already loaded
        if (dealerItemDtos == null || dealerItemDtos.isEmpty()) {
            loadDealerItems();
        }

        int addedCount = 0;
        boolean preventDuplicates = configOptionApplicationController.getBooleanValueByKey(
                "Prevent Duplicate Items in Purchase Orders", false);

        for (ItemSupplierDto dto : dealerItemDtos) {
            // Check for duplicates
            if (preventDuplicates) {
                boolean exists = false;
                for (BillItem existingItem : getBillItems()) {
                    if (existingItem != null && !existingItem.isRetired()
                            && existingItem.getItem() != null
                            && existingItem.getItem().getId().equals(dto.getItemId())) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    continue;
                }
            }

            // Load item entity
            Item item = itemFacade.find(dto.getItemId());
            if (item == null) {
                continue;
            }

            // Create bill item
            BillItem bi = new BillItem();
            bi.setItem(item);
            bi.setSearialNo(getBillItems().size());

            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(bi);
            bi.setPharmaceuticalBillItem(pbi);

            pbi.setPurchaseRate(pharmacyBean.getLastPurchaseRate(item, sessionController.getDepartment()));
            pbi.setRetailRate(pharmacyBean.getLastRetailRate(item, sessionController.getDepartment()));

            getBillItems().add(bi);
            addedCount++;
        }

        if (addedCount > 0) {
            JsfUtil.addSuccessMessage(addedCount + " items added");
            itemDtos = null; // Refresh DTOs
            calculateBillTotals();
        } else {
            JsfUtil.addErrorMessage("No new items to add");
        }
    }

    /**
     * Add supplier items below reorder level
     */
    public void addAllSupplierItemsBelowRol() {
        if (getCurrentBill().getToInstitution() == null) {
            JsfUtil.addErrorMessage("Please select supplier");
            return;
        }

        String jpql = "SELECT i FROM Item i WHERE i IN "
                + "(SELECT id.item FROM ItemsDistributors id WHERE id.institution = :supplier "
                + "AND id.retired = false AND id.item.retired = false) "
                + "AND i IN "
                + "(SELECT s.itemBatch.item FROM Stock s JOIN Reorder r ON r.item = s.itemBatch.item "
                + "AND r.department = s.department "
                + "WHERE s.department = :department AND s.stock < r.rol GROUP BY s.itemBatch.item) "
                + "ORDER BY i.name";

        Map<String, Object> params = new HashMap<>();
        params.put("supplier", getCurrentBill().getToInstitution());
        params.put("department", sessionController.getDepartment());

        List<Item> itemsBelowRol = itemFacade.findByJpql(jpql, params);

        if (itemsBelowRol == null || itemsBelowRol.isEmpty()) {
            JsfUtil.addErrorMessage("No items found below reorder level");
            return;
        }

        int addedCount = 0;
        boolean preventDuplicates = configOptionApplicationController.getBooleanValueByKey(
                "Prevent Duplicate Items in Purchase Orders", false);

        for (Item item : itemsBelowRol) {
            // Check for duplicates
            if (preventDuplicates) {
                boolean exists = false;
                for (BillItem existingItem : getBillItems()) {
                    if (existingItem != null && !existingItem.isRetired()
                            && existingItem.getItem() != null
                            && existingItem.getItem().getId().equals(item.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    continue;
                }
            }

            BillItem bi = new BillItem();
            bi.setItem(item);
            bi.setSearialNo(getBillItems().size());

            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.setBillItem(bi);
            bi.setPharmaceuticalBillItem(pbi);

            pbi.setPurchaseRate(pharmacyBean.getLastPurchaseRate(item, sessionController.getDepartment()));
            pbi.setRetailRate(pharmacyBean.getLastRetailRate(item, sessionController.getDepartment()));

            getBillItems().add(bi);
            addedCount++;
        }

        if (addedCount > 0) {
            JsfUtil.addSuccessMessage(addedCount + " items below ROL added");
            itemDtos = null; // Refresh DTOs
            calculateBillTotals();
        } else {
            JsfUtil.addErrorMessage("No new items to add");
        }
    }

    /**
     * On edit of item quantity or rate in DTO
     */
    public void onEditDto(PurchaseOrderItemDto dto) {
        if (dto == null) {
            return;
        }

        // Validate integer quantities if config enabled
        if (configOptionController.getBooleanValueByKey(
                "Pharmacy Purchase - Quantity Must Be Integer", true)) {
            if (dto.getQuantity() != null
                    && dto.getQuantity().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                dto.setQuantity(BigDecimal.ZERO);
                JsfUtil.addErrorMessage("Please enter only whole numbers for quantity");
                return;
            }
            if (dto.getFreeQuantity() != null
                    && dto.getFreeQuantity().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                dto.setFreeQuantity(BigDecimal.ZERO);
                JsfUtil.addErrorMessage("Please enter only whole numbers for free quantity");
                return;
            }
        }

        // Calculate line total in DTO
        dto.calculateLineTotal();

        // Update corresponding bill item entity
        syncDtoToEntity(dto);

        calculateBillTotals();
    }

    /**
     * Sync DTO changes back to entity (for persistence)
     */
    private void syncDtoToEntity(PurchaseOrderItemDto dto) {
        BillItem bi = findBillItemById(dto.getBillItemId());
        if (bi == null) {
            return;
        }

        // Update bill item finance details from DTO
        if (bi.getBillItemFinanceDetails() != null) {
            bi.getBillItemFinanceDetails().setQuantity(dto.getQuantity());
            bi.getBillItemFinanceDetails().setFreeQuantity(dto.getFreeQuantity());
            bi.getBillItemFinanceDetails().setLineGrossRate(dto.getPurchaseRate());
            bi.getBillItemFinanceDetails().setRetailSaleRate(dto.getRetailRate());

            // Recalculate line values using existing logic
            calculateLineValues(bi);
        }
    }

    /**
     * Find bill item by ID
     */
    private BillItem findBillItemById(Long billItemId) {
        if (billItemId == null) {
            return null;
        }
        for (BillItem bi : getBillItems()) {
            if (bi != null && billItemId.equals(bi.getId())) {
                return bi;
            }
        }
        return null;
    }

    /**
     * Calculate line values (reused from original controller)
     */
    private void calculateLineValues(BillItem lineBillItem) {
        BigDecimal bdQty = lineBillItem.getBillItemFinanceDetails().getQuantity();
        BigDecimal bdFreeQty = lineBillItem.getBillItemFinanceDetails().getFreeQuantity();
        BigDecimal bdPurchaseRate = lineBillItem.getBillItemFinanceDetails().getLineGrossRate();
        BigDecimal bdRetailRate = lineBillItem.getBillItemFinanceDetails().getRetailSaleRate();
        BigDecimal bdUnitsPerPack = lineBillItem.getBillItemFinanceDetails().getUnitsPerPack();

        // Null safety
        if (bdQty == null) bdQty = BigDecimal.ZERO;
        if (bdFreeQty == null) bdFreeQty = BigDecimal.ZERO;
        if (bdPurchaseRate == null) bdPurchaseRate = BigDecimal.ZERO;
        if (bdRetailRate == null) bdRetailRate = BigDecimal.ZERO;
        if (bdUnitsPerPack == null || bdUnitsPerPack.doubleValue() <= 0) bdUnitsPerPack = BigDecimal.ONE;

        // Calculate values
        BigDecimal bdGrossValue = bdPurchaseRate.multiply(bdQty);
        BigDecimal bdNetValue = bdGrossValue;
        BigDecimal bdRetailValue = bdRetailRate.multiply(bdQty.add(bdFreeQty));
        BigDecimal bdPurchaseValue = bdPurchaseRate.multiply(bdQty.add(bdFreeQty));

        // Set finance details
        lineBillItem.getBillItemFinanceDetails().setLineNetRate(bdPurchaseRate);
        lineBillItem.getBillItemFinanceDetails().setLineGrossRate(bdPurchaseRate);
        lineBillItem.getBillItemFinanceDetails().setGrossRate(bdPurchaseRate);

        lineBillItem.getBillItemFinanceDetails().setLineNetTotal(bdNetValue);
        lineBillItem.getBillItemFinanceDetails().setLineGrossTotal(bdGrossValue);
        lineBillItem.getBillItemFinanceDetails().setGrossTotal(bdGrossValue);

        lineBillItem.getBillItemFinanceDetails().setQuantity(bdQty);
        lineBillItem.getBillItemFinanceDetails().setFreeQuantity(bdFreeQty);

        // Quantity by units
        BigDecimal quantityByUnits = (lineBillItem.getItem() instanceof Ampp)
                ? bdQty.multiply(bdUnitsPerPack) : bdQty;
        lineBillItem.getBillItemFinanceDetails().setQuantityByUnits(quantityByUnits);

        BigDecimal freeQuantityByUnits = (lineBillItem.getItem() instanceof Ampp)
                ? bdFreeQty.multiply(bdUnitsPerPack) : bdFreeQty;
        lineBillItem.getBillItemFinanceDetails().setFreeQuantityByUnits(freeQuantityByUnits);

        lineBillItem.getBillItemFinanceDetails().setValueAtPurchaseRate(bdPurchaseValue);
        lineBillItem.getBillItemFinanceDetails().setValueAtRetailRate(bdRetailValue);

        // Update pharmaceutical bill item
        if (lineBillItem.getPharmaceuticalBillItem() != null) {
            double quantity = bdQty.doubleValue();
            double freeQuantity = bdFreeQty.doubleValue();

            if (lineBillItem.getItem() instanceof Ampp) {
                double unitsPerPack = bdUnitsPerPack.doubleValue();
                lineBillItem.getPharmaceuticalBillItem().setQty(quantity * unitsPerPack);
                lineBillItem.getPharmaceuticalBillItem().setFreeQty(freeQuantity * unitsPerPack);
                lineBillItem.getPharmaceuticalBillItem().setPurchaseRate(bdPurchaseRate.doubleValue() / unitsPerPack);
                lineBillItem.getPharmaceuticalBillItem().setRetailRate(bdRetailRate.doubleValue() / unitsPerPack);
            } else {
                lineBillItem.getPharmaceuticalBillItem().setQty(quantity);
                lineBillItem.getPharmaceuticalBillItem().setFreeQty(freeQuantity);
                lineBillItem.getPharmaceuticalBillItem().setPurchaseRate(bdPurchaseRate.doubleValue());
                lineBillItem.getPharmaceuticalBillItem().setRetailRate(bdRetailRate.doubleValue());
            }
        }

        // Update bill item values
        lineBillItem.setQty(bdQty.doubleValue());
        lineBillItem.setRate(bdPurchaseRate.doubleValue());
        lineBillItem.setNetRate(bdPurchaseRate.doubleValue());
        lineBillItem.setNetValue(bdNetValue.doubleValue());
        lineBillItem.setGrossValue(bdGrossValue.doubleValue());
    }

    /**
     * Calculate bill totals
     */
    public void calculateBillTotals() {
        BigDecimal billNetTotal = BigDecimal.ZERO;
        BigDecimal billGrossTotal = BigDecimal.ZERO;

        int serialNo = 0;
        for (BillItem handlingBillItem : getBillItems()) {
            if (handlingBillItem == null || handlingBillItem.isRetired()) {
                continue;
            }

            handlingBillItem.setSearialNo(serialNo++);

            if (handlingBillItem.getBillItemFinanceDetails() != null) {
                BigDecimal lineNetTotal = handlingBillItem.getBillItemFinanceDetails().getLineNetTotal();
                BigDecimal lineGrossTotal = handlingBillItem.getBillItemFinanceDetails().getLineGrossTotal();

                if (lineNetTotal != null) {
                    billNetTotal = billNetTotal.add(lineNetTotal);
                }
                if (lineGrossTotal != null) {
                    billGrossTotal = billGrossTotal.add(lineGrossTotal);
                }
            }
        }

        getCurrentBill().setTotal(billGrossTotal.doubleValue());
        getCurrentBill().setNetTotal(billNetTotal.doubleValue());

        if (getCurrentBill().getBillFinanceDetails() != null) {
            getCurrentBill().getBillFinanceDetails().setNetTotal(billNetTotal);
            getCurrentBill().getBillFinanceDetails().setGrossTotal(billGrossTotal);
        }
    }

    /**
     * Remove item from PO
     */
    public void removeItemDto(PurchaseOrderItemDto dto) {
        if (dto == null) {
            return;
        }

        // Find and remove from bill items
        BillItem toRemove = findBillItemById(dto.getBillItemId());
        if (toRemove != null) {
            toRemove.setRetired(true);
            toRemove.setRetirer(sessionController.getLoggedUser());
            toRemove.setRetiredAt(new Date());

            if (toRemove.getId() != null) {
                billItemFacade.edit(toRemove);
            }

            getBillItems().remove(toRemove);
        }

        // Refresh DTOs
        itemDtos = null;
        calculateBillTotals();
    }

    /**
     * Remove selected items
     */
    public void removeSelected() {
        if (selectedItemDtos == null || selectedItemDtos.isEmpty()) {
            return;
        }

        for (PurchaseOrderItemDto dto : selectedItemDtos) {
            removeItemDto(dto);
        }

        selectedItemDtos = null;
        itemDtos = null;
        calculateBillTotals();
    }

    /**
     * Save purchase order request
     */
    public void saveRequest() {
        if (getCurrentBill().isChecked()) {
            JsfUtil.addErrorMessage("Cannot save a finalized bill");
            return;
        }

        saveBill();
        saveBillComponents();

        JsfUtil.addSuccessMessage("Purchase order saved");
    }

    /**
     * Save bill header
     */
    private void saveBill() {
        if (getCurrentBill().getId() == null) {
            getCurrentBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER_PRE);
            getCurrentBill().setDepartment(sessionController.getLoggedUser().getDepartment());
            getCurrentBill().setInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
            getCurrentBill().setFromDepartment(sessionController.getLoggedUser().getDepartment());
            getCurrentBill().setFromInstitution(sessionController.getLoggedUser().getDepartment().getInstitution());
            createAndAssignBillNumber();
            getCurrentBill().setCreater(sessionController.getLoggedUser());
            getCurrentBill().setCreatedAt(Calendar.getInstance().getTime());
            billFacade.create(getCurrentBill());
        } else {
            getCurrentBill().setEditedAt(Calendar.getInstance().getTime());
            getCurrentBill().setEditor(sessionController.getLoggedUser());
            billFacade.edit(getCurrentBill());
        }
    }

    /**
     * Generate and assign bill number
     */
    private void createAndAssignBillNumber() {
        String billSuffix = configOptionApplicationController.getLongTextValueByKey(
                "Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_PRE, "");
        if (billSuffix == null || billSuffix.trim().isEmpty()) {
            configOptionApplicationController.setLongTextValueByKey(
                    "Bill Number Suffix for " + BillTypeAtomic.PHARMACY_ORDER_PRE, "POR");
        }

        boolean deptInst = configOptionApplicationController.getBooleanValueByKey(
                "Bill Number Generation Strategy for Purchase Order Requests - Prefix + Department Code + Institution Code + Year + Yearly Number and Yearly Number", false);

        if (getCurrentBill().getDeptId() == null || getCurrentBill().getDeptId().trim().equals("")) {
            String billId = billNumberBean.departmentBillNumberGeneratorYearly(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_ORDER_PRE);
            getCurrentBill().setDeptId(billId);
        }

        if (getCurrentBill().getInsId() == null || getCurrentBill().getInsId().trim().equals("")) {
            getCurrentBill().setInsId(getCurrentBill().getDeptId());
        }
    }

    /**
     * Save bill components
     */
    private void saveBillComponents() {
        for (BillItem bi : getBillItems()) {
            bi.setBill(getCurrentBill());
            if (bi.getId() == null) {
                billItemFacade.create(bi);
            } else {
                billItemFacade.edit(bi);
            }
        }
    }

    /**
     * Finalize purchase order
     */
    public void finalizeRequest() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No bill to finalize");
            return;
        }
        if (getCurrentBill().isChecked()) {
            JsfUtil.addErrorMessage("Bill already finalized");
            return;
        }
        if (getCurrentBill().getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select payment method");
            return;
        }
        if (getBillItems() == null || getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Please add items");
            return;
        }

        // Save first if not saved
        if (currentBill.getId() == null) {
            saveBill();
            saveBillComponents();
        }

        // Finalize bill
        getCurrentBill().setEditedAt(new Date());
        getCurrentBill().setEditor(sessionController.getLoggedUser());
        getCurrentBill().setChecked(true);
        getCurrentBill().setCheckeAt(new Date());
        getCurrentBill().setCheckedBy(sessionController.getLoggedUser());
        getCurrentBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER);
        billFacade.edit(getCurrentBill());

        // Finalize bill items
        for (BillItem bi : getBillItems()) {
            if (bi.getId() == null) {
                billItemFacade.create(bi);
            } else {
                billItemFacade.edit(bi);
            }
        }

        notificationController.createNotification(getCurrentBill());

        JsfUtil.addSuccessMessage("Purchase order finalized");
        printPreview = true;
    }

    // Getters and Setters
    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
    }

    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public void setItemDtos(List<PurchaseOrderItemDto> itemDtos) {
        this.itemDtos = itemDtos;
    }

    public List<PurchaseOrderItemDto> getSelectedItemDtos() {
        return selectedItemDtos;
    }

    public void setSelectedItemDtos(List<PurchaseOrderItemDto> selectedItemDtos) {
        this.selectedItemDtos = selectedItemDtos;
    }

    public List<ItemSupplierDto> getDealerItemDtos() {
        if (dealerItemDtos == null) {
            loadDealerItems();
        }
        return dealerItemDtos;
    }

    public void setDealerItemDtos(List<ItemSupplierDto> dealerItemDtos) {
        this.dealerItemDtos = dealerItemDtos;
    }

    public ItemSupplierDto getSelectedDealerItem() {
        return selectedDealerItem;
    }

    public void setSelectedDealerItem(ItemSupplierDto selectedDealerItem) {
        this.selectedDealerItem = selectedDealerItem;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<BillItem> getSelectedBillItems() {
        return selectedBillItems;
    }

    public void setSelectedBillItems(List<BillItem> selectedBillItems) {
        this.selectedBillItems = selectedBillItems;
    }
}
