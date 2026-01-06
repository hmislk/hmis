/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.ItemType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.batch.*;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.*;
import com.divudi.core.facade.*;
import com.divudi.core.util.CommonFunctions;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
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
     * Create new batch with Stock entry
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

        // Check for existing ItemBatch
        ItemBatch existingBatch = findExistingBatch(amp, batchNo, request.getExpiryDate());

        ItemBatch itemBatch;
        String message;

        if (existingBatch != null) {
            // Use existing batch
            itemBatch = existingBatch;
            message = "Used existing batch";
        } else {
            // Create new ItemBatch
            itemBatch = createNewItemBatch(amp, batchNo, request.getExpiryDate(),
                    request.getRetailRate(), purchaseRate, costRate,
                    request.getWholesaleRate());
            message = "Created new batch";
        }

        // Create Stock entry for department
        Stock stock = createStockEntry(itemBatch, department);

        // Build response
        AmpResponseDTO ampResponse = createAmpResponseDTO(amp, false);
        return new BatchCreateResponseDTO(
                itemBatch.getId(),
                stock.getId(),
                itemBatch.getBatchNo(),
                ampResponse,
                department.getName(),
                itemBatch.getRetailsaleRate(),
                itemBatch.getPurcahseRate(), // Keep intentional typo
                itemBatch.getCostRate(),
                itemBatch.getDateOfExpire(),
                message
        );
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

        // Future date validation
        if (request.getExpiryDate().before(new Date())) {
            throw new Exception("Expiry date must be in the future");
        }
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

        itemBatchFacade.create(ib);
        return ib;
    }

    private Stock createStockEntry(ItemBatch itemBatch, Department department) {
        Stock stock = new Stock();
        stock.setItemBatch(itemBatch);
        stock.setDepartment(department);
        stock.setStock(0.0); // Always start with 0 quantity
        stockFacade.create(stock);
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
