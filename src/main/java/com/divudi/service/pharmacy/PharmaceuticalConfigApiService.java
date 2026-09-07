/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.pharmaceutical.CategoryConfigRequestDTO;
import com.divudi.core.data.dto.pharmaceutical.CategoryConfigResponseDTO;
import com.divudi.core.data.dto.pharmaceutical.MeasurementUnitRequestDTO;
import com.divudi.core.data.dto.pharmaceutical.MeasurementUnitResponseDTO;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.DosageForm;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.MeasurementUnit;
import com.divudi.core.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.core.facade.DosageFormFacade;
import com.divudi.core.facade.MeasurementUnitFacade;
import com.divudi.core.facade.PharmaceuticalItemCategoryFacade;
import com.divudi.core.util.CommonFunctions;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for Pharmaceutical Config API operations.
 * Manages categories, dosage forms, and measurement units.
 *
 * @author Buddhika
 */
@Stateless
public class PharmaceuticalConfigApiService implements Serializable {

    @EJB
    private PharmaceuticalItemCategoryFacade categoryFacade;
    @EJB
    private DosageFormFacade dosageFormFacade;
    @EJB
    private MeasurementUnitFacade measurementUnitFacade;

    // ==================== SEARCH ====================

    @SuppressWarnings("unchecked")
    public List<?> searchConfigs(String type, String query, Integer limit) throws Exception {
        if (query == null || query.trim().isEmpty()) {
            throw new Exception("Query parameter is required");
        }

        int resultLimit = (limit != null && limit > 0 && limit <= 100) ? limit : 20;
        String entityName = getEntityName(type);

        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");

        if ("units".equals(type)) {
            String jpql = "SELECT new com.divudi.core.data.dto.pharmaceutical.MeasurementUnitResponseDTO() "
                    + "FROM MeasurementUnit c "
                    + "WHERE c.retired = false "
                    + "AND (upper(c.name) LIKE :query OR upper(c.code) LIKE :query) "
                    + "ORDER BY c.name";
            // MeasurementUnitResponseDTO doesn't have a matching JPQL constructor, so use entity query
            return searchMeasurementUnits(query.trim(), resultLimit);
        }

        String jpql = "SELECT new com.divudi.core.data.dto.pharmaceutical.CategoryConfigResponseDTO("
                + "c.id, c.name, c.code, c.description, c.retired) "
                + "FROM " + entityName + " c "
                + "WHERE c.retired = false "
                + "AND (upper(c.name) LIKE :query OR upper(c.code) LIKE :query) "
                + "ORDER BY c.name";

        switch (type) {
            case "categories":
                return (List<CategoryConfigResponseDTO>) categoryFacade.findLightsByJpql(
                        jpql, params, javax.persistence.TemporalType.TIMESTAMP, resultLimit);
            case "dosage_forms":
                return (List<CategoryConfigResponseDTO>) dosageFormFacade.findLightsByJpql(
                        jpql, params, javax.persistence.TemporalType.TIMESTAMP, resultLimit);
            default:
                throw new Exception("Invalid config type: " + type);
        }
    }

    private List<MeasurementUnitResponseDTO> searchMeasurementUnits(String query, int limit) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", "%" + query.toUpperCase() + "%");

        String jpql = "SELECT c FROM MeasurementUnit c "
                + "WHERE c.retired = false "
                + "AND (upper(c.name) LIKE :query OR upper(c.code) LIKE :query) "
                + "ORDER BY c.name";

        List<MeasurementUnit> entities = measurementUnitFacade.findByJpql(
                jpql, params, javax.persistence.TemporalType.TIMESTAMP, limit);

        return entities.stream().map(this::toMeasurementUnitResponse).collect(java.util.stream.Collectors.toList());
    }

    // ==================== GET BY ID ====================

    public Object findConfigById(String type, Long id) throws Exception {
        if (id == null) {
            throw new Exception("Config ID is required");
        }

        switch (type) {
            case "categories": {
                PharmaceuticalItemCategory item = categoryFacade.find(id);
                if (item == null) {
                    throw new Exception("Category not found with ID: " + id);
                }
                return toCategoryResponse(item);
            }
            case "dosage_forms": {
                DosageForm item = dosageFormFacade.find(id);
                if (item == null) {
                    throw new Exception("Dosage form not found with ID: " + id);
                }
                return toCategoryResponse(item);
            }
            case "units": {
                MeasurementUnit item = measurementUnitFacade.find(id);
                if (item == null) {
                    throw new Exception("Measurement unit not found with ID: " + id);
                }
                return toMeasurementUnitResponse(item);
            }
            default:
                throw new Exception("Invalid config type: " + type);
        }
    }

    // ==================== CREATE ====================

    public Object createConfig(String type, CategoryConfigRequestDTO request, WebUser user) throws Exception {
        if (user == null) {
            throw new Exception("User is required for creating configs");
        }
        validateConfigRequest(request);

        switch (type) {
            case "categories":
                return createCategory(request, user);
            case "dosage_forms":
                return createDosageForm(request, user);
            case "units":
                if (!(request instanceof MeasurementUnitRequestDTO)) {
                    throw new Exception("MeasurementUnit request DTO required for units");
                }
                return createMeasurementUnit((MeasurementUnitRequestDTO) request, user);
            default:
                throw new Exception("Invalid config type: " + type);
        }
    }

    private CategoryConfigResponseDTO createCategory(CategoryConfigRequestDTO request, WebUser user) {
        PharmaceuticalItemCategory item = new PharmaceuticalItemCategory();
        applyCategoryFields(item, request);
        setCategoryAuditForCreate(item, user);
        categoryFacade.createAndFlush(item);
        return toCategoryResponse(item);
    }

    private CategoryConfigResponseDTO createDosageForm(CategoryConfigRequestDTO request, WebUser user) {
        DosageForm item = new DosageForm();
        applyCategoryFields(item, request);
        setCategoryAuditForCreate(item, user);
        dosageFormFacade.createAndFlush(item);
        return toCategoryResponse(item);
    }

    private MeasurementUnitResponseDTO createMeasurementUnit(MeasurementUnitRequestDTO request, WebUser user) {
        MeasurementUnit item = new MeasurementUnit();
        applyCategoryFields(item, request);
        applyMeasurementUnitFields(item, request);
        setCategoryAuditForCreate(item, user);
        measurementUnitFacade.createAndFlush(item);
        return toMeasurementUnitResponse(item);
    }

    // ==================== UPDATE ====================

    public Object updateConfig(String type, Long id, CategoryConfigRequestDTO request, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Config ID is required");
        }
        if (user == null) {
            throw new Exception("User is required for updating configs");
        }

        switch (type) {
            case "categories": {
                PharmaceuticalItemCategory item = categoryFacade.find(id);
                if (item == null) {
                    throw new Exception("Category not found with ID: " + id);
                }
                applyCategoryFieldsIfProvided(item, request);
                categoryFacade.edit(item);
                return toCategoryResponse(item);
            }
            case "dosage_forms": {
                DosageForm item = dosageFormFacade.find(id);
                if (item == null) {
                    throw new Exception("Dosage form not found with ID: " + id);
                }
                applyCategoryFieldsIfProvided(item, request);
                dosageFormFacade.edit(item);
                return toCategoryResponse(item);
            }
            case "units": {
                MeasurementUnit item = measurementUnitFacade.find(id);
                if (item == null) {
                    throw new Exception("Measurement unit not found with ID: " + id);
                }
                applyCategoryFieldsIfProvided(item, request);
                if (request instanceof MeasurementUnitRequestDTO) {
                    applyMeasurementUnitFieldsIfProvided(item, (MeasurementUnitRequestDTO) request);
                }
                measurementUnitFacade.edit(item);
                return toMeasurementUnitResponse(item);
            }
            default:
                throw new Exception("Invalid config type: " + type);
        }
    }

    // ==================== RETIRE ====================

    public Object retireConfig(String type, Long id, String retireComments, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Config ID is required");
        }
        if (user == null) {
            throw new Exception("User is required for retiring configs");
        }

        Category item;
        switch (type) {
            case "categories":
                item = categoryFacade.find(id);
                break;
            case "dosage_forms":
                item = dosageFormFacade.find(id);
                break;
            case "units":
                item = measurementUnitFacade.find(id);
                break;
            default:
                throw new Exception("Invalid config type: " + type);
        }

        if (item == null) {
            throw new Exception(getDisplayName(type) + " not found with ID: " + id);
        }
        if (item.isRetired()) {
            throw new Exception(getDisplayName(type) + " is already retired");
        }

        item.setRetired(true);
        item.setRetirer(user);
        item.setRetiredAt(Calendar.getInstance().getTime());
        item.setRetireComments(retireComments);

        editCategoryByType(type, item);
        return findConfigById(type, id);
    }

    // ==================== UNRETIRE ====================

    public Object unretireConfig(String type, Long id, WebUser user) throws Exception {
        if (id == null) {
            throw new Exception("Config ID is required");
        }
        if (user == null) {
            throw new Exception("User is required for restoring configs");
        }

        Category item;
        switch (type) {
            case "categories":
                item = categoryFacade.find(id);
                break;
            case "dosage_forms":
                item = dosageFormFacade.find(id);
                break;
            case "units":
                item = measurementUnitFacade.find(id);
                break;
            default:
                throw new Exception("Invalid config type: " + type);
        }

        if (item == null) {
            throw new Exception(getDisplayName(type) + " not found with ID: " + id);
        }
        if (!item.isRetired()) {
            throw new Exception(getDisplayName(type) + " is not retired");
        }

        item.setRetired(false);
        item.setRetirer(null);
        item.setRetiredAt(null);
        item.setRetireComments(null);

        editCategoryByType(type, item);
        return findConfigById(type, id);
    }

    // ==================== HELPER METHODS ====================

    private void validateConfigRequest(CategoryConfigRequestDTO request) throws Exception {
        if (request == null) {
            throw new Exception("Request body is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new Exception("Name is required");
        }
    }

    private void applyCategoryFields(Category item, CategoryConfigRequestDTO request) {
        item.setName(request.getName().trim());
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            item.setCode(request.getCode().trim());
        } else {
            item.setCode(CommonFunctions.nameToCode(request.getName()));
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
    }

    private void applyCategoryFieldsIfProvided(Category item, CategoryConfigRequestDTO request) {
        if (request == null) {
            return;
        }
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            item.setName(request.getName().trim());
        }
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            item.setCode(request.getCode().trim());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription());
        }
    }

    private void applyMeasurementUnitFields(MeasurementUnit item, MeasurementUnitRequestDTO request) {
        if (request.getStrengthUnit() != null) {
            item.setStrengthUnit(request.getStrengthUnit());
        }
        if (request.getPackUnit() != null) {
            item.setPackUnit(request.getPackUnit());
        }
        if (request.getIssueUnit() != null) {
            item.setIssueUnit(request.getIssueUnit());
        }
        if (request.getDurationUnit() != null) {
            item.setDurationUnit(request.getDurationUnit());
        }
        if (request.getFrequencyUnit() != null) {
            item.setFrequencyUnit(request.getFrequencyUnit());
        }
        if (request.getDurationInHours() != null) {
            item.setDurationInHours(request.getDurationInHours());
        }
        if (request.getFrequencyInHours() != null) {
            item.setFrequencyInHours(request.getFrequencyInHours());
        }
    }

    private void applyMeasurementUnitFieldsIfProvided(MeasurementUnit item, MeasurementUnitRequestDTO request) {
        applyMeasurementUnitFields(item, request);
    }

    private void setCategoryAuditForCreate(Category item, WebUser user) {
        item.setCreater(user);
        item.setCreatedAt(Calendar.getInstance().getTime());
        item.setRetired(false);
    }

    private void editCategoryByType(String type, Category item) {
        switch (type) {
            case "categories":
                categoryFacade.edit((PharmaceuticalItemCategory) item);
                break;
            case "dosage_forms":
                dosageFormFacade.edit((DosageForm) item);
                break;
            case "units":
                measurementUnitFacade.edit((MeasurementUnit) item);
                break;
        }
    }

    private CategoryConfigResponseDTO toCategoryResponse(Category item) {
        return new CategoryConfigResponseDTO(
                item.getId(), item.getName(), item.getCode(),
                item.getDescription(), item.isRetired());
    }

    private MeasurementUnitResponseDTO toMeasurementUnitResponse(MeasurementUnit item) {
        MeasurementUnitResponseDTO dto = new MeasurementUnitResponseDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setCode(item.getCode());
        dto.setDescription(item.getDescription());
        dto.setRetired(item.isRetired());
        dto.setStrengthUnit(item.isStrengthUnit());
        dto.setPackUnit(item.isPackUnit());
        dto.setIssueUnit(item.isIssueUnit());
        dto.setDurationUnit(item.isDurationUnit());
        dto.setFrequencyUnit(item.isFrequencyUnit());
        dto.setDurationInHours(item.getDurationInHours());
        dto.setFrequencyInHours(item.getFrequencyInHours());
        return dto;
    }

    private String getEntityName(String type) throws Exception {
        switch (type) {
            case "categories":
                return "PharmaceuticalItemCategory";
            case "dosage_forms":
                return "DosageForm";
            case "units":
                return "MeasurementUnit";
            default:
                throw new Exception("Invalid config type: " + type);
        }
    }

    private String getDisplayName(String type) {
        switch (type) {
            case "categories":
                return "Category";
            case "dosage_forms":
                return "Dosage form";
            case "units":
                return "Measurement unit";
            default:
                return "Config";
        }
    }
}
