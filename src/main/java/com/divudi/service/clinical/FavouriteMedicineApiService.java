/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.clinical;

import com.divudi.bean.clinical.FavouriteController;
import com.divudi.bean.common.ItemController;
import com.divudi.bean.pharmacy.MeasurementUnitController;
import com.divudi.bean.pharmacy.VmpController;
import com.divudi.core.data.ItemType;
import com.divudi.core.data.clinical.PrescriptionTemplateType;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.clinical.PrescriptionTemplate;
import com.divudi.core.entity.pharmacy.MeasurementUnit;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.MeasurementUnitFacade;
import com.divudi.core.facade.PrescriptionTemplateFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service layer for Favourite Medicine API operations
 * Contains business logic for entity management and favourite medicine operations
 * Integrates with existing facades and controllers
 *
 * @author Buddhika
 */
@Stateless
public class FavouriteMedicineApiService implements Serializable {

    // =================== FACADE INJECTIONS ===================

    @EJB
    private PrescriptionTemplateFacade prescriptionTemplateFacade;

    @EJB
    private ItemFacade itemFacade;

    @EJB
    private CategoryFacade categoryFacade;

    @EJB
    private MeasurementUnitFacade measurementUnitFacade;

    // =================== CONTROLLER INJECTIONS ===================

    @Inject
    private FavouriteController favouriteController;

    @Inject
    private ItemController itemController;

    @Inject
    private MeasurementUnitController measurementUnitController;

    @Inject
    private VmpController vmpController;

    // =================== FAVOURITE MEDICINE OPERATIONS ===================

    /**
     * Create a new favourite medicine configuration
     */
    public PrescriptionTemplate createFavouriteMedicine(WebUser user, Map<String, Object> requestData) {
        try {
            // Extract required fields
            String itemName = (String) requestData.get("itemName");
            String itemType = (String) requestData.get("itemType");
            Double fromYears = getDoubleValue(requestData.get("fromYears"));
            Double toYears = getDoubleValue(requestData.get("toYears"));

            if (itemName == null || itemType == null || fromYears == null || toYears == null) {
                throw new IllegalArgumentException("Required fields missing: itemName, itemType, fromYears, toYears");
            }

            if (fromYears < 0 || toYears < 0 || fromYears >= toYears) {
                throw new IllegalArgumentException("Invalid age range: fromYears must be less than toYears and both must be >= 0");
            }

            // Find or create the item based on type
            ItemType itemTypeEnum = parseItemType(itemType);
            Item item = findItemByNameAndType(itemName.trim(), itemTypeEnum);

            boolean createMissingEntities = getBooleanValue(requestData.get("createMissingEntities"));
            if (item == null && createMissingEntities) {
                throw new UnsupportedOperationException("Auto-creation of items not implemented yet");
            }

            if (item == null) {
                throw new IllegalArgumentException("Item not found: " + itemName + " (type: " + itemType + ")");
            }

            // Create the prescription template
            PrescriptionTemplate template = new PrescriptionTemplate();
            template.setType(PrescriptionTemplateType.FavouriteMedicine);
            template.setForWebUser(user);
            template.setItem(item);
            template.setForItem(item); // Default to same item

            // Set age range (convert years to days)
            template.setFromDays(convertYearsToDays(fromYears));
            template.setToDays(convertYearsToDays(toYears));

            // Set optional fields
            setOptionalFields(template, requestData);

            // Set order number
            Double orderNo = getDoubleValue(requestData.get("orderNo"));
            if (orderNo == null) {
                // Auto-generate order number
                orderNo = getNextOrderNumber(user);
            }
            template.setOrderNo(orderNo);

            // Set audit fields
            template.setCreatedAt(new Date());
            template.setCreater(user);

            // Save to database
            prescriptionTemplateFacade.create(template);

            return template;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create favourite medicine: " + e.getMessage(), e);
        }
    }

    /**
     * Search favourite medicines with filtering
     */
    public List<PrescriptionTemplate> searchFavouriteMedicines(WebUser user, Map<String, Object> searchCriteria) {
        try {
            String jpql = "SELECT p FROM PrescriptionTemplate p " +
                         "WHERE p.retired = false AND p.forWebUser = :user " +
                         "AND p.type = :type";

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("user", user);
            parameters.put("type", PrescriptionTemplateType.FavouriteMedicine);

            // Add filters based on search criteria
            String query = (String) searchCriteria.get("query");
            if (query != null && !query.trim().isEmpty()) {
                jpql += " AND (UPPER(p.item.name) LIKE :query OR UPPER(p.item.code) LIKE :query)";
                parameters.put("query", "%" + query.trim().toUpperCase() + "%");
            }

            String itemType = (String) searchCriteria.get("itemType");
            if (itemType != null && !itemType.trim().isEmpty()) {
                try {
                    ItemType itemTypeEnum = parseItemType(itemType);
                    jpql += " AND p.item.itemType = :itemType";
                    parameters.put("itemType", itemTypeEnum);
                } catch (IllegalArgumentException e) {
                    // Invalid item type, ignore filter
                }
            }

            String categoryName = (String) searchCriteria.get("categoryName");
            if (categoryName != null && !categoryName.trim().isEmpty()) {
                jpql += " AND UPPER(p.category.name) = :categoryName";
                parameters.put("categoryName", categoryName.trim().toUpperCase());
            }

            // Age range filters
            Double ageYears = getDoubleValue(searchCriteria.get("ageYears"));
            if (ageYears != null) {
                Double ageDays = convertYearsToDays(ageYears);
                jpql += " AND (p.fromDays <= :ageDays AND p.toDays >= :ageDays)";
                parameters.put("ageDays", ageDays);
            }

            Double fromYears = getDoubleValue(searchCriteria.get("fromYears"));
            if (fromYears != null) {
                Double fromDays = convertYearsToDays(fromYears);
                jpql += " AND p.toDays >= :fromDays";
                parameters.put("fromDays", fromDays);
            }

            Double toYears = getDoubleValue(searchCriteria.get("toYears"));
            if (toYears != null) {
                Double toDays = convertYearsToDays(toYears);
                jpql += " AND p.fromDays <= :toDays";
                parameters.put("toDays", toDays);
            }

            // Other filters
            String sex = (String) searchCriteria.get("sex");
            if (sex != null && !sex.trim().isEmpty()) {
                jpql += " AND (p.sex IS NULL OR UPPER(p.sex) = :sex)";
                parameters.put("sex", sex.trim().toUpperCase());
            }

            Boolean indoor = getBooleanValue(searchCriteria.get("indoor"));
            if (indoor != null) {
                jpql += " AND p.indoor = :indoor";
                parameters.put("indoor", indoor);
            }

            // Ordering
            String orderBy = (String) searchCriteria.get("orderBy");
            String orderDirection = (String) searchCriteria.get("orderDirection");

            if (orderBy == null || orderBy.trim().isEmpty()) {
                orderBy = "orderNo";
            }
            if (orderDirection == null || orderDirection.trim().isEmpty()) {
                orderDirection = "ASC";
            }

            switch (orderBy.toLowerCase()) {
                case "orderno":
                    jpql += " ORDER BY p.orderNo " + orderDirection;
                    break;
                case "itemname":
                    jpql += " ORDER BY p.item.name " + orderDirection;
                    break;
                case "fromyears":
                    jpql += " ORDER BY p.fromDays " + orderDirection;
                    break;
                case "toyears":
                    jpql += " ORDER BY p.toDays " + orderDirection;
                    break;
                case "categoryname":
                    jpql += " ORDER BY p.category.name " + orderDirection;
                    break;
                default:
                    jpql += " ORDER BY p.orderNo ASC";
            }

            // Apply limit and offset
            Integer limit = (Integer) searchCriteria.get("limit");
            Integer offset = (Integer) searchCriteria.get("offset");

            if (limit != null && limit > 0) {
                if (offset != null && offset > 0) {
                    return prescriptionTemplateFacade.findByJpql(jpql, parameters, offset, limit);
                } else {
                    return prescriptionTemplateFacade.findByJpql(jpql, parameters, limit);
                }
            } else {
                return prescriptionTemplateFacade.findByJpql(jpql, parameters);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to search favourite medicines: " + e.getMessage(), e);
        }
    }

    /**
     * Get a specific favourite medicine by ID
     */
    public PrescriptionTemplate getFavouriteMedicineById(WebUser user, Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        String jpql = "SELECT p FROM PrescriptionTemplate p " +
                     "WHERE p.id = :id AND p.retired = false " +
                     "AND p.forWebUser = :user AND p.type = :type";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        parameters.put("user", user);
        parameters.put("type", PrescriptionTemplateType.FavouriteMedicine);

        PrescriptionTemplate template = prescriptionTemplateFacade.findFirstByJpql(jpql, parameters);

        if (template == null) {
            throw new IllegalArgumentException("Favourite medicine not found or access denied");
        }

        return template;
    }

    /**
     * Update an existing favourite medicine
     */
    public PrescriptionTemplate updateFavouriteMedicine(WebUser user, Long id, Map<String, Object> updateData) {
        try {
            PrescriptionTemplate template = getFavouriteMedicineById(user, id);

            // Update age range if provided
            Double fromYears = getDoubleValue(updateData.get("fromYears"));
            Double toYears = getDoubleValue(updateData.get("toYears"));

            if (fromYears != null) {
                if (fromYears < 0) {
                    throw new IllegalArgumentException("fromYears must be >= 0");
                }
                template.setFromDays(convertYearsToDays(fromYears));
            }

            if (toYears != null) {
                if (toYears < 0) {
                    throw new IllegalArgumentException("toYears must be >= 0");
                }
                template.setToDays(convertYearsToDays(toYears));
            }

            // Validate age range after updates
            if (template.getFromDays() != null && template.getToDays() != null) {
                if (template.getFromDays() >= template.getToDays()) {
                    throw new IllegalArgumentException("fromYears must be less than toYears");
                }
            }

            // Update other optional fields
            setOptionalFields(template, updateData);

            // Update order number if provided
            Double orderNo = getDoubleValue(updateData.get("orderNo"));
            if (orderNo != null) {
                template.setOrderNo(orderNo);
            }

            // Save changes
            prescriptionTemplateFacade.edit(template);
            return template;

        } catch (Exception e) {
            throw new RuntimeException("Failed to update favourite medicine: " + e.getMessage(), e);
        }
    }

    /**
     * Delete (soft delete) a favourite medicine
     */
    public boolean deleteFavouriteMedicine(WebUser user, Long id) {
        try {
            PrescriptionTemplate template = getFavouriteMedicineById(user, id);

            // Soft delete by setting retired flag
            template.setRetired(true);

            prescriptionTemplateFacade.edit(template);
            return true;

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete favourite medicine: " + e.getMessage(), e);
        }
    }

    // =================== VTM OPERATIONS ===================

    /**
     * Search VTMs by name with optional limit
     */
    public List<Item> searchVtms(String query, Integer limit) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String jpql = "SELECT i FROM Item i WHERE i.retired = false " +
                     "AND i.itemType = :itemType " +
                     "AND (UPPER(i.name) LIKE :query OR UPPER(i.code) LIKE :query) " +
                     "ORDER BY i.name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("itemType", ItemType.Vtm);
        parameters.put("query", "%" + query.trim().toUpperCase() + "%");

        if (limit != null && limit > 0) {
            return itemFacade.findByJpql(jpql, parameters, limit);
        } else {
            return itemFacade.findByJpql(jpql, parameters);
        }
    }

    /**
     * Create a new VTM
     */
    public Item createVtm(WebUser user, String name, String code, String description) {
        // TODO: Implement VTM creation with validation
        // Check for duplicates, set proper fields, save to database

        throw new UnsupportedOperationException("Not implemented yet");
    }

    // =================== ATM OPERATIONS ===================

    /**
     * Search ATMs by name with optional limit
     */
    public List<Item> searchAtms(String query, Integer limit) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String jpql = "SELECT i FROM Item i WHERE i.retired = false " +
                     "AND i.itemType = :itemType " +
                     "AND (UPPER(i.name) LIKE :query OR UPPER(i.code) LIKE :query) " +
                     "ORDER BY i.name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("itemType", ItemType.Atm);
        parameters.put("query", "%" + query.trim().toUpperCase() + "%");

        if (limit != null && limit > 0) {
            return itemFacade.findByJpql(jpql, parameters, limit);
        } else {
            return itemFacade.findByJpql(jpql, parameters);
        }
    }

    /**
     * Create a new ATM
     */
    public Item createAtm(WebUser user, String name, String code, String description) {
        // TODO: Implement ATM creation

        throw new UnsupportedOperationException("Not implemented yet");
    }

    // =================== VMP OPERATIONS ===================

    /**
     * Search VMPs by name with optional limit
     */
    public List<Item> searchVmps(String query, Integer limit) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String jpql = "SELECT i FROM Item i WHERE i.retired = false " +
                     "AND i.itemType = :itemType " +
                     "AND (UPPER(i.name) LIKE :query OR UPPER(i.code) LIKE :query) " +
                     "ORDER BY i.name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("itemType", ItemType.Vmp);
        parameters.put("query", "%" + query.trim().toUpperCase() + "%");

        if (limit != null && limit > 0) {
            return itemFacade.findByJpql(jpql, parameters, limit);
        } else {
            return itemFacade.findByJpql(jpql, parameters);
        }
    }

    /**
     * Create a new VMP
     */
    public Item createVmp(WebUser user, Map<String, Object> vmpData) {
        // TODO: Implement VMP creation with category, units, etc.

        throw new UnsupportedOperationException("Not implemented yet");
    }

    // =================== AMP OPERATIONS ===================

    /**
     * Search AMPs by name with optional limit
     */
    public List<Item> searchAmps(String query, Integer limit) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String jpql = "SELECT i FROM Item i WHERE i.retired = false " +
                     "AND i.itemType = :itemType " +
                     "AND (UPPER(i.name) LIKE :query OR UPPER(i.code) LIKE :query) " +
                     "ORDER BY i.name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("itemType", ItemType.Amp);
        parameters.put("query", "%" + query.trim().toUpperCase() + "%");

        if (limit != null && limit > 0) {
            return itemFacade.findByJpql(jpql, parameters, limit);
        } else {
            return itemFacade.findByJpql(jpql, parameters);
        }
    }

    /**
     * Create a new AMP
     */
    public Item createAmp(WebUser user, Map<String, Object> ampData) {
        // TODO: Implement AMP creation

        throw new UnsupportedOperationException("Not implemented yet");
    }

    // =================== MEASUREMENT UNIT OPERATIONS ===================

    /**
     * Search measurement units by name and type
     */
    public List<MeasurementUnit> searchMeasurementUnits(String query, String unitType, Integer limit) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String jpql = "SELECT m FROM MeasurementUnit m WHERE m.retired = false " +
                     "AND (UPPER(m.name) LIKE :query OR UPPER(m.code) LIKE :query)";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("query", "%" + query.trim().toUpperCase() + "%");

        // Add unit type filter if specified
        if (unitType != null && !unitType.trim().isEmpty()) {
            switch (unitType.trim().toLowerCase()) {
                case "doseunit":
                    jpql += " AND m.strengthUnit = true";
                    break;
                case "frequencyunit":
                    jpql += " AND m.frequencyUnit = true";
                    break;
                case "durationunit":
                    jpql += " AND m.durationUnit = true";
                    break;
                case "issueunit":
                    jpql += " AND m.issueUnit = true";
                    break;
                case "packunit":
                    jpql += " AND m.packUnit = true";
                    break;
                // If invalid unit type, ignore the filter
            }
        }

        jpql += " ORDER BY m.name";

        if (limit != null && limit > 0) {
            return measurementUnitFacade.findByJpql(jpql, parameters, limit);
        } else {
            return measurementUnitFacade.findByJpql(jpql, parameters);
        }
    }

    /**
     * Create a new measurement unit
     */
    public MeasurementUnit createMeasurementUnit(WebUser user, String name, String code, String unitType) {
        // TODO: Implement measurement unit creation

        throw new UnsupportedOperationException("Not implemented yet");
    }

    // =================== CATEGORY OPERATIONS ===================

    /**
     * Search categories by name
     */
    public List<Category> searchCategories(String query, Integer limit) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String jpql = "SELECT c FROM Category c WHERE c.retired = false " +
                     "AND (UPPER(c.name) LIKE :query OR UPPER(c.code) LIKE :query) " +
                     "ORDER BY c.name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("query", "%" + query.trim().toUpperCase() + "%");

        if (limit != null && limit > 0) {
            return categoryFacade.findByJpql(jpql, parameters, limit);
        } else {
            return categoryFacade.findByJpql(jpql, parameters);
        }
    }

    /**
     * Create a new category
     */
    public Category createCategory(WebUser user, String name, String code, String description) {
        // TODO: Implement category creation

        throw new UnsupportedOperationException("Not implemented yet");
    }

    // =================== AI HELPER OPERATIONS ===================

    /**
     * Bulk validate entities for AI workflows
     */
    public Map<String, Object> validateEntities(List<Map<String, Object>> entitiesToValidate) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> validationResults = new ArrayList<>();

        int totalValidated = 0;
        int totalValid = 0;
        int totalInvalid = 0;

        if (entitiesToValidate != null) {
            for (Map<String, Object> entityRequest : entitiesToValidate) {
                String name = (String) entityRequest.get("name");
                String type = (String) entityRequest.get("type");

                Map<String, Object> validationResult = validateSingleEntity(name, type);
                validationResults.add(validationResult);

                totalValidated++;
                if ((Boolean) validationResult.get("exists")) {
                    totalValid++;
                } else {
                    totalInvalid++;
                }
            }
        }

        result.put("results", validationResults);
        result.put("totalValidated", totalValidated);
        result.put("totalValid", totalValid);
        result.put("totalInvalid", totalInvalid);

        return result;
    }

    /**
     * Validate a single entity
     */
    private Map<String, Object> validateSingleEntity(String name, String type) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("type", type);
        result.put("exists", false);
        result.put("entityId", null);
        result.put("message", "");
        result.put("suggestions", new ArrayList<>());
        result.put("canCreate", false);

        if (name == null || name.trim().isEmpty()) {
            result.put("message", "Name cannot be empty");
            return result;
        }

        if (type == null || type.trim().isEmpty()) {
            result.put("message", "Type cannot be empty");
            return result;
        }

        try {
            switch (type.toUpperCase()) {
                case "VTM":
                    Item vtm = findItemByNameAndType(name.trim(), ItemType.Vtm);
                    if (vtm != null) {
                        result.put("exists", true);
                        result.put("entityId", vtm.getId());
                        result.put("message", "VTM found");
                    } else {
                        result.put("message", "VTM not found");
                        result.put("canCreate", true);
                        // Add suggestions for similar VTMs
                        List<Item> similarVtms = searchVtms(name.trim(), 5);
                        List<String> suggestions = new ArrayList<>();
                        for (Item item : similarVtms) {
                            suggestions.add(item.getName());
                        }
                        result.put("suggestions", suggestions);
                    }
                    break;

                case "ATM":
                    Item atm = findItemByNameAndType(name.trim(), ItemType.Atm);
                    if (atm != null) {
                        result.put("exists", true);
                        result.put("entityId", atm.getId());
                        result.put("message", "ATM found");
                    } else {
                        result.put("message", "ATM not found");
                        result.put("canCreate", true);
                    }
                    break;

                case "VMP":
                    Item vmp = findItemByNameAndType(name.trim(), ItemType.Vmp);
                    if (vmp != null) {
                        result.put("exists", true);
                        result.put("entityId", vmp.getId());
                        result.put("message", "VMP found");
                    } else {
                        result.put("message", "VMP not found");
                        result.put("canCreate", true);
                    }
                    break;

                case "AMP":
                    Item amp = findItemByNameAndType(name.trim(), ItemType.Amp);
                    if (amp != null) {
                        result.put("exists", true);
                        result.put("entityId", amp.getId());
                        result.put("message", "AMP found");
                    } else {
                        result.put("message", "AMP not found");
                        result.put("canCreate", true);
                    }
                    break;

                case "UNIT":
                    MeasurementUnit unit = findMeasurementUnitByName(name.trim());
                    if (unit != null) {
                        result.put("exists", true);
                        result.put("entityId", unit.getId());
                        result.put("message", "Measurement unit found");
                    } else {
                        result.put("message", "Measurement unit not found");
                        result.put("canCreate", true);
                        // Add suggestions for similar units
                        List<MeasurementUnit> similarUnits = searchMeasurementUnits(name.trim(), null, 5);
                        List<String> suggestions = new ArrayList<>();
                        for (MeasurementUnit u : similarUnits) {
                            suggestions.add(u.getName());
                        }
                        result.put("suggestions", suggestions);
                    }
                    break;

                case "CATEGORY":
                    Category category = findCategoryByName(name.trim());
                    if (category != null) {
                        result.put("exists", true);
                        result.put("entityId", category.getId());
                        result.put("message", "Category found");
                    } else {
                        result.put("message", "Category not found");
                        result.put("canCreate", true);
                        // Add suggestions for similar categories
                        List<Category> similarCategories = searchCategories(name.trim(), 5);
                        List<String> suggestions = new ArrayList<>();
                        for (Category c : similarCategories) {
                            suggestions.add(c.getName());
                        }
                        result.put("suggestions", suggestions);
                    }
                    break;

                default:
                    result.put("message", "Invalid entity type. Must be one of: VTM, ATM, VMP, AMP, UNIT, CATEGORY");
                    break;
            }
        } catch (Exception e) {
            result.put("message", "Validation error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Auto-suggest similar entities
     */
    public List<Map<String, Object>> suggestSimilarEntities(String entityType, String partialName) {
        // TODO: Implement suggestion logic using fuzzy matching

        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Parse natural language instructions
     */
    public Map<String, Object> parseNaturalLanguage(String instruction) {
        // TODO: Implement natural language parsing for AI agents
        // Extract medicine name, dosage, frequency, duration, age range

        throw new UnsupportedOperationException("Not implemented yet");
    }

    // =================== UTILITY METHODS ===================

    /**
     * Convert years to days for age range storage
     */
    public Double convertYearsToDays(Double years) {
        if (years == null) {
            return null;
        }
        return years * 365.25; // Account for leap years
    }

    /**
     * Convert days to years for API responses
     */
    public Double convertDaysToYears(Double days) {
        if (days == null) {
            return null;
        }
        return days / 365.25; // Account for leap years
    }

    /**
     * Find entity by name and type with case-insensitive search
     */
    public Item findItemByNameAndType(String name, ItemType itemType) {
        if (name == null || name.trim().isEmpty() || itemType == null) {
            return null;
        }

        String jpql = "SELECT i FROM Item i WHERE i.retired = false " +
                     "AND i.itemType = :itemType " +
                     "AND UPPER(i.name) = :name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("itemType", itemType);
        parameters.put("name", name.trim().toUpperCase());

        return itemFacade.findFirstByJpql(jpql, parameters);
    }

    /**
     * Find measurement unit by name
     */
    public MeasurementUnit findMeasurementUnitByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String jpql = "SELECT m FROM MeasurementUnit m WHERE m.retired = false " +
                     "AND UPPER(m.name) = :name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", name.trim().toUpperCase());

        return measurementUnitFacade.findFirstByJpql(jpql, parameters);
    }

    /**
     * Find category by name
     */
    public Category findCategoryByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String jpql = "SELECT c FROM Category c WHERE c.retired = false " +
                     "AND UPPER(c.name) = :name";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", name.trim().toUpperCase());

        return categoryFacade.findFirstByJpql(jpql, parameters);
    }

    /**
     * Parse item type string to enum
     */
    private ItemType parseItemType(String itemType) {
        if (itemType == null || itemType.trim().isEmpty()) {
            throw new IllegalArgumentException("Item type cannot be null or empty");
        }

        switch (itemType.trim().toLowerCase()) {
            case "vtm":
                return ItemType.Vtm;
            case "atm":
                return ItemType.Atm;
            case "vmp":
                return ItemType.Vmp;
            case "amp":
                return ItemType.Amp;
            default:
                throw new IllegalArgumentException("Invalid item type: " + itemType + ". Must be one of: Vtm, Atm, Vmp, Amp");
        }
    }

    /**
     * Get double value from object, handling various types
     */
    private Double getDoubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Get boolean value from object
     */
    private boolean getBooleanValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    /**
     * Set optional fields on prescription template from request data
     */
    private void setOptionalFields(PrescriptionTemplate template, Map<String, Object> requestData) {
        // Category
        String categoryName = (String) requestData.get("categoryName");
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            Category category = findCategoryByName(categoryName.trim());
            if (category != null) {
                template.setCategory(category);
            }
        }

        // Dose and dose unit
        Double dose = getDoubleValue(requestData.get("dose"));
        if (dose != null) {
            template.setDose(dose);
        }

        String doseUnitName = (String) requestData.get("doseUnitName");
        if (doseUnitName != null && !doseUnitName.trim().isEmpty()) {
            MeasurementUnit doseUnit = findMeasurementUnitByName(doseUnitName.trim());
            if (doseUnit != null) {
                template.setDoseUnit(doseUnit);
            }
        }

        // Frequency unit
        String frequencyUnitName = (String) requestData.get("frequencyUnitName");
        if (frequencyUnitName != null && !frequencyUnitName.trim().isEmpty()) {
            MeasurementUnit frequencyUnit = findMeasurementUnitByName(frequencyUnitName.trim());
            if (frequencyUnit != null) {
                template.setFrequencyUnit(frequencyUnit);
            }
        }

        // Duration and duration unit
        Double duration = getDoubleValue(requestData.get("duration"));
        if (duration != null) {
            template.setDuration(duration);
        }

        String durationUnitName = (String) requestData.get("durationUnitName");
        if (durationUnitName != null && !durationUnitName.trim().isEmpty()) {
            MeasurementUnit durationUnit = findMeasurementUnitByName(durationUnitName.trim());
            if (durationUnit != null) {
                template.setDurationUnit(durationUnit);
            }
        }

        // Issue and issue unit
        Double issue = getDoubleValue(requestData.get("issue"));
        if (issue != null) {
            template.setIssue(issue);
        }

        String issueUnitName = (String) requestData.get("issueUnitName");
        if (issueUnitName != null && !issueUnitName.trim().isEmpty()) {
            MeasurementUnit issueUnit = findMeasurementUnitByName(issueUnitName.trim());
            if (issueUnit != null) {
                template.setIssueUnit(issueUnit);
            }
        }

        // Other fields
        Boolean indoor = getBooleanValue(requestData.get("indoor"));
        template.setIndoor(indoor);

        String sex = (String) requestData.get("sex");
        if (sex != null && !sex.trim().isEmpty()) {
            // TODO: Set sex enum if needed
        }

        String forItemName = (String) requestData.get("forItemName");
        if (forItemName != null && !forItemName.trim().isEmpty()) {
            // TODO: Find and set forItem if different from main item
        }
    }

    /**
     * Get next order number for user's favourite medicines
     */
    private Double getNextOrderNumber(WebUser user) {
        String jpql = "SELECT MAX(p.orderNo) FROM PrescriptionTemplate p " +
                     "WHERE p.retired = false AND p.forWebUser = :user " +
                     "AND p.type = :type";

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("user", user);
        parameters.put("type", PrescriptionTemplateType.FavouriteMedicine);

        Double maxOrder = prescriptionTemplateFacade.findDoubleByJpql(jpql, parameters);
        return (maxOrder != null ? maxOrder + 1.0 : 1.0);
    }
}