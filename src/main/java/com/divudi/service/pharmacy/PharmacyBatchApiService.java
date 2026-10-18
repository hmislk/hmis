/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.ItemType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.adjustment.AdjustmentResponseDTO;
import com.divudi.core.data.dto.adjustment.StockQuantityAdjustmentDTO;
import com.divudi.core.data.dto.batch.*;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.*;
import com.divudi.core.facade.*;
import com.divudi.core.util.CommonFunctions;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.TemporalType;

/**
 * Service for Pharmacy Batch API operations Provides business logic for AMP
 * creation and batch creation with Stock entries
 *
 * @author Buddhika
 */
@Named
@RequestScoped
public class PharmacyBatchApiService implements Serializable {

    @EJB
    private AmpFacade ampFacade;

    @EJB
    private VmpFacade vmpFacade;

    @EJB
    private ItemBatchFacade itemBatchFacade;

    @EJB
    private StockFacade stockFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private CategoryFacade categoryFacade;

    @Inject
    private PharmacyAdjustmentApiService adjustmentApiService;

    private static final double RATE_TOLERANCE = 0.0001;

    /**
     * Search for AMP by name, create if not found
     */
    @Transactional
    public AmpResponseDTO searchOrCreateAmp(AmpSearchCreateRequestDTO request, WebUser user) throws Exception {
        validateAmpSearchCreateRequest(request);

        // Search for existing AMP by name (case-insensitive)
        String jpql = "SELECT a FROM Amp a WHERE LOWER(TRIM(a.name)) = LOWER(TRIM(:name)) AND a.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("name", request.getName());

        Amp existingAmp = ampFacade.findFirstByJpql(jpql, params);

        if (existingAmp != null) {
            // Return existing AMP
            return createAmpResponseDTO(existingAmp, false);
        } else {
            // Create new AMP
            Amp newAmp = createNewAmp(request, user);
            return createAmpResponseDTO(newAmp, true);
        }
    }

    /**
     * Create new batch with Stock entry.
     * <p>
     * An existing ItemBatch with the same item, batch number and expiry is
     * reused with its own rates (ratesDiffer flags a mismatch with the
     * request), and an existing Stock row for that batch in the department is
     * reused instead of creating a duplicate (#24334). When initialQuantity is
     * given, the stock is set through the same audited path as
     * /pharmacy_adjustments/stock_quantity (#24336). Privilege checks for
     * allowPastExpiry and initialQuantity are done by the REST resource.
     */
    @Transactional
    public BatchCreateResponseDTO createBatch(BatchCreateRequestDTO request, WebUser user) throws Exception {
        validateBatchCreateRequest(request);

        // Load and validate entities
        Amp amp = loadAndValidateAmp(request.getItemId());
        Department department = loadAndValidateDepartment(request.getDepartmentId());

        // Apply rate defaults
        Double purchaseRate = request.getPurchaseRate();
        if (purchaseRate == null) {
            purchaseRate = request.getRetailRate() * 0.85;
        }

        Double costRate = request.getCostRate();
        if (costRate == null) {
            costRate = purchaseRate;
        }

        // Auto-generate batch number if not provided
        String batchNo = request.getBatchNo();
        if (batchNo == null || batchNo.trim().isEmpty()) {
            batchNo = "B" + System.currentTimeMillis();
        } else {
            batchNo = batchNo.trim();
        }

        ItemBatch existingBatch = findExistingBatch(amp, batchNo, request.getExpiryDate());
        boolean batchCreated = existingBatch == null;
        ItemBatch itemBatch = batchCreated
                ? createNewItemBatch(amp, batchNo, request.getExpiryDate(),
                        request.getRetailRate(), purchaseRate, costRate,
                        request.getWholesaleRate())
                : existingBatch;

        Stock existingStock = findDepartmentStock(itemBatch, department);
        boolean stockCreated = existingStock == null;
        Stock stock = stockCreated ? createStockEntry(itemBatch, department) : existingStock;

        boolean ratesDiffer = !batchCreated
                && (ratesDiffer(request.getRetailRate(), itemBatch.getRetailsaleRate())
                || ratesDiffer(purchaseRate, itemBatch.getPurcahseRate())
                || ratesDiffer(costRate, itemBatch.getCostRate())
                || (request.getWholesaleRate() != null
                && ratesDiffer(request.getWholesaleRate(), itemBatch.getWholesaleRate())));

        StringBuilder message = new StringBuilder(batchCreated ? "Created new batch" : "Used existing batch");
        if (ratesDiffer) {
            message.append(" (existing rates kept; they differ from the request)");
        }
        message.append(stockCreated ? "; created department stock" : "; reused existing department stock");

        AdjustmentResponseDTO adjustment = null;
        if (request.getInitialQuantity() != null && stock.getStock() == null) {
            // Legacy rows can hold a null quantity; the adjustment path needs a number to start from.
            stock.setStock(0.0);
            stockFacade.edit(stock);
        }
        if (request.getInitialQuantity() != null
                && Double.compare(request.getInitialQuantity(), currentQuantity(stock)) != 0) {
            String comment = request.getComment();
            if (comment == null || comment.trim().isEmpty()) {
                comment = "Initial quantity on batch creation";
            }
            adjustment = adjustmentApiService.adjustStockQuantity(
                    new StockQuantityAdjustmentDTO(stock.getId(), request.getInitialQuantity(), comment, department.getId()),
                    user);
            message.append("; quantity set to ").append(request.getInitialQuantity());
        }

        AmpResponseDTO ampResponse = createAmpResponseDTO(amp, false);
        BatchCreateResponseDTO response = new BatchCreateResponseDTO(
                itemBatch.getId(),
                stock.getId(),
                itemBatch.getBatchNo(),
                ampResponse,
                department.getName(),
                itemBatch.getRetailsaleRate(),
                itemBatch.getPurcahseRate(), // Keep intentional typo
                itemBatch.getCostRate(),
                itemBatch.getDateOfExpire(),
                message.toString()
        );
        response.setBatchCreated(batchCreated);
        response.setStockCreated(stockCreated);
        response.setRatesDiffer(ratesDiffer);
        if (ratesDiffer) {
            response.setRequestedRetailRate(request.getRetailRate());
            response.setRequestedPurchaseRate(purchaseRate);
            response.setRequestedCostRate(costRate);
            response.setRequestedWholesaleRate(request.getWholesaleRate());
        }
        response.setWholesaleRate(itemBatch.getWholesaleRate());
        response.setQuantity(currentQuantity(stock));
        if (adjustment != null) {
            response.setAdjustmentBillId(adjustment.getBillId());
            response.setAdjustmentBillNumber(adjustment.getBillNumber());
            response.setQuantity(adjustment.getAfterValue());
        }
        return response;
    }

    /**
     * Returns the department's Stock row for the batch, creating one with
     * zero quantity only when the department has none.
     */
    public Stock findOrCreateDepartmentStock(ItemBatch itemBatch, Department department) {
        Stock stock = findDepartmentStock(itemBatch, department);
        return stock != null ? stock : createStockEntry(itemBatch, department);
    }

    /**
     * Returns the non-retired ItemBatch with this item, batch number and
     * expiry, creating one with the given rates when none exists. An existing
     * batch keeps its own rates.
     */
    public ItemBatch findOrCreateItemBatch(Amp amp, String batchNo, Date expiryDate,
            Double retailRate, Double purchaseRate, Double costRate) {
        ItemBatch existing = findExistingBatch(amp, batchNo, expiryDate);
        return existing != null ? existing
                : createNewItemBatch(amp, batchNo, expiryDate, retailRate, purchaseRate, costRate, null);
    }

    /**
     * Search AMPs by name
     */
    public List<AmpResponseDTO> searchAmpByName(String name, Integer limit) throws Exception {
        if (name == null || name.trim().isEmpty()) {
            throw new Exception("Search name is required");
        }

        if (limit == null || limit <= 0) {
            limit = 30;
        } else if (limit > 50) {
            limit = 50; // Maximum limit for performance
        }

        String jpql = "SELECT NEW com.divudi.core.data.dto.batch.AmpResponseDTO(a.id, a.name, a.code, a.vmp.name, a.category.name) "
                + "FROM Amp a WHERE LOWER(a.name) LIKE LOWER(:name) AND a.retired = false ORDER BY a.name";

        Map<String, Object> params = new HashMap<>();
        params.put("name", "%" + name.trim() + "%");

        return (List<AmpResponseDTO>) ampFacade.findLightsByJpql(jpql, params, TemporalType.DATE, limit);
    }

    // Private helper methods
    private void validateAmpSearchCreateRequest(AmpSearchCreateRequestDTO request) throws Exception {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new Exception("AMP name is required");
        }
    }

    private void validateBatchCreateRequest(BatchCreateRequestDTO request) throws Exception {
        if (request.getItemId() == null) {
            throw new Exception("Item ID is required");
        }
        if (request.getExpiryDate() == null) {
            throw new Exception("Expiry date is required");
        }
        if (request.getRetailRate() == null || request.getRetailRate() <= 0) {
            throw new Exception("Retail rate is required and must be positive");
        }
        if (request.getDepartmentId() == null) {
            throw new Exception("Department ID is required");
        }

        // Optional validations with positive values
        if (request.getPurchaseRate() != null && request.getPurchaseRate() <= 0) {
            throw new Exception("Purchase rate must be positive when provided");
        }
        if (request.getCostRate() != null && request.getCostRate() <= 0) {
            throw new Exception("Cost rate must be positive when provided");
        }
        if (request.getWholesaleRate() != null && request.getWholesaleRate() <= 0) {
            throw new Exception("Wholesale rate must be positive when provided");
        }

        // Expiry date validation (DATE-ONLY, ignores time component)
        Date today = truncateToDate(new Date());
        Date expiry = truncateToDate(request.getExpiryDate());

        if (expiry.before(today) && !Boolean.TRUE.equals(request.getAllowPastExpiry())) {
            throw new Exception("Expiry date must be today or a future date (set allowPastExpiry=true to record expired stock)");
        }
        if (request.getInitialQuantity() != null
                && (!Double.isFinite(request.getInitialQuantity()) || request.getInitialQuantity() < 0)) {
            throw new Exception("Initial quantity must be a number zero or above");
        }
    }

    private Date truncateToDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Amp loadAndValidateAmp(Long itemId) throws Exception {
        Amp amp = ampFacade.find(itemId);
        if (amp == null) {
            throw new Exception("AMP not found with ID: " + itemId);
        }
        if (amp.isRetired()) {
            throw new Exception("AMP is retired and cannot be used");
        }
        return amp;
    }

    private Department loadAndValidateDepartment(Long departmentId) throws Exception {
        Department department = departmentFacade.find(departmentId);
        if (department == null) {
            throw new Exception("Department not found with ID: " + departmentId);
        }
        if (department.isRetired()) {
            throw new Exception("Department is retired and cannot be used");
        }
        return department;
    }

    private Amp createNewAmp(AmpSearchCreateRequestDTO request, WebUser user) throws Exception {
        Amp amp = new Amp();
        amp.setName(request.getName().trim());
        amp.setCode(CommonFunctions.nameToCode(request.getName())); // Convert to lowercase, replace spaces with _
        amp.setItemType(ItemType.Amp);
        amp.setDepartmentType(DepartmentType.Pharmacy);
        amp.setRetired(false);

        // Set audit fields
        amp.setCreatedAt(Calendar.getInstance().getTime());
        amp.setCreater(user);

        // Get VMP (use any available if not specified)
        Vmp vmp = getVmpForAmp(request.getGenericName());
        amp.setVmp(vmp);

        // Inherit category and dosage form from VMP
        if (vmp != null) {
            if (request.getCategoryId() != null) {
                Category category = categoryFacade.find(request.getCategoryId());
                amp.setCategory(category);
            } else if (vmp.getCategory() != null) {
                amp.setCategory(vmp.getCategory());
            }

            amp.setDosageForm(vmp.getDosageForm());
        }

        ampFacade.create(amp);
        return amp;
    }

    private Vmp getVmpForAmp(String genericName) throws Exception {
        if (genericName != null && !genericName.trim().isEmpty()) {
            // Search for VMP by name
            String jpql = "SELECT v FROM Vmp v WHERE LOWER(TRIM(v.name)) = LOWER(TRIM(:name)) AND v.retired = false";
            Map<String, Object> params = new HashMap<>();
            params.put("name", genericName.trim());
            Vmp vmp = vmpFacade.findFirstByJpql(jpql, params);
            if (vmp != null) {
                return vmp;
            }
        }

        // Use any available VMP
        String jpql = "SELECT v FROM Vmp v WHERE v.retired = false ORDER BY v.id";
        List<Vmp> vmps = vmpFacade.findByJpql(jpql, 1);
        if (vmps.isEmpty()) {
            throw new Exception("No VMP available in the system");
        }
        return vmps.get(0);
    }

    private ItemBatch findExistingBatch(Amp amp, String batchNo, Date expiryDate) {
        String jpql = "SELECT ib FROM ItemBatch ib WHERE ib.item = :item AND ib.batchNo = :batchNo AND ib.dateOfExpire = :expiryDate AND ib.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("item", amp);
        params.put("batchNo", batchNo);
        params.put("expiryDate", expiryDate);

        return itemBatchFacade.findFirstByJpql(jpql, params);
    }

    private ItemBatch createNewItemBatch(Amp amp, String batchNo, Date expiryDate,
            Double retailRate, Double purchaseRate, Double costRate, Double wholesaleRate) {
        ItemBatch ib = new ItemBatch();
        ib.setItem(amp);
        ib.setBatchNo(batchNo);
        ib.setDateOfExpire(expiryDate);
        ib.setPurcahseRate(purchaseRate); // CRITICAL: Keep intentional typo
        ib.setRetailsaleRate(retailRate);
        ib.setCostRate(costRate);

        if (wholesaleRate != null && wholesaleRate > 0) {
            ib.setWholesaleRate(wholesaleRate);
        }

        itemBatchFacade.createAndFlush(ib); // flush so getId() below is guaranteed populated
        return ib;
    }

    private Stock findDepartmentStock(ItemBatch itemBatch, Department department) {
        String jpql = "SELECT s FROM Stock s WHERE s.itemBatch = :batch AND s.department = :department AND s.retired = false ORDER BY s.id";
        Map<String, Object> params = new HashMap<>();
        params.put("batch", itemBatch);
        params.put("department", department);
        return stockFacade.findFirstByJpql(jpql, params);
    }

    private double currentQuantity(Stock stock) {
        return stock.getStock() == null ? 0.0 : stock.getStock();
    }

    private boolean ratesDiffer(Double requested, Double actual) {
        if (requested == null || actual == null) {
            return requested != null || actual != null;
        }
        return Math.abs(requested - actual) > RATE_TOLERANCE;
    }

    private Stock createStockEntry(ItemBatch itemBatch, Department department) {
        Stock stock = new Stock();
        stock.setItemBatch(itemBatch);
        stock.setDepartment(department);
        stock.setStock(0.0); // Always start with 0 quantity
        stockFacade.createAndFlush(stock); // flush so getId() below is guaranteed populated
        return stock;
    }

    private AmpResponseDTO createAmpResponseDTO(Amp amp, Boolean created) {
        String genericName = (amp.getVmp() != null) ? amp.getVmp().getName() : null;
        String categoryName = (amp.getCategory() != null) ? amp.getCategory().getName() : null;

        return new AmpResponseDTO(
                amp.getId(),
                amp.getName(),
                amp.getCode(),
                genericName,
                categoryName,
                created
        );
    }
}
