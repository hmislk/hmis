/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.clinical;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.clinical.PrescriptionTemplate;
import com.divudi.core.entity.pharmacy.MeasurementUnit;
import com.divudi.service.clinical.FavouriteMedicineApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for Favourite Medicine Management by Age
 * Provides comprehensive endpoints for AI agents to manage favourite medicine configurations
 * Supports entity management (VTM, ATM, VMP, AMP, Units, Categories) and favourite medicine CRUD operations
 *
 * @author Buddhika
 */
@Path("clinical/favourite_medicines")
@RequestScoped
public class FavouriteMedicineApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private FavouriteMedicineApiService favouriteMedicineService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public FavouriteMedicineApi() {
    }

    // =================== FAVOURITE MEDICINE CRUD OPERATIONS ===================

    /**
     * Create a new favourite medicine configuration
     * POST /api/clinical/favourite_medicines
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFavouriteMedicine(String jsonRequest) {
        try {
            // Validate API key
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Parse JSON request
            if (jsonRequest == null || jsonRequest.trim().isEmpty()) {
                return errorResponse("Request body is required", 400);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> requestData = gson.fromJson(jsonRequest, Map.class);

            // Create favourite medicine
            PrescriptionTemplate template = favouriteMedicineService.createFavouriteMedicine(user, requestData);

            // Convert to DTO for response
            Map<String, Object> response = convertTemplateToMap(template);

            return successResponse(response);

        } catch (IllegalArgumentException e) {
            return errorResponse("Invalid request: " + e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Search and list favourite medicines
     * GET /api/clinical/favourite_medicines
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchFavouriteMedicines() {
        try {
            // Validate API key
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Parse query parameters into search criteria
            Map<String, Object> searchCriteria = parseSearchParams();

            // Search favourite medicines
            List<PrescriptionTemplate> results = favouriteMedicineService.searchFavouriteMedicines(user, searchCriteria);

            // Convert to DTOs for response
            List<Map<String, Object>> responseData = new ArrayList<>();
            for (PrescriptionTemplate template : results) {
                responseData.add(convertTemplateToMap(template));
            }

            return successResponse(responseData);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get a specific favourite medicine by ID
     * GET /api/clinical/favourite_medicines/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFavouriteMedicine(@PathParam("id") Long id) {
        try {
            // Validate API key
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("ID parameter is required", 400);
            }

            // Get favourite medicine by ID
            PrescriptionTemplate template = favouriteMedicineService.getFavouriteMedicineById(user, id);

            // Convert to DTO for response
            Map<String, Object> response = convertTemplateToMap(template);

            return successResponse(response);

        } catch (IllegalArgumentException e) {
            return errorResponse("Invalid request: " + e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update an existing favourite medicine
     * PUT /api/clinical/favourite_medicines/{id}
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFavouriteMedicine(@PathParam("id") Long id, String jsonRequest) {
        try {
            // Validate API key
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("ID parameter is required", 400);
            }

            // Parse JSON request
            if (jsonRequest == null || jsonRequest.trim().isEmpty()) {
                return errorResponse("Request body is required", 400);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> updateData = gson.fromJson(jsonRequest, Map.class);

            // Update favourite medicine
            PrescriptionTemplate template = favouriteMedicineService.updateFavouriteMedicine(user, id, updateData);

            // Convert to DTO for response
            Map<String, Object> response = convertTemplateToMap(template);

            return successResponse(response);

        } catch (IllegalArgumentException e) {
            return errorResponse("Invalid request: " + e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Delete (soft delete) a favourite medicine
     * DELETE /api/clinical/favourite_medicines/{id}
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFavouriteMedicine(@PathParam("id") Long id) {
        try {
            // Validate API key
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("ID parameter is required", 400);
            }

            // Delete favourite medicine
            boolean deleted = favouriteMedicineService.deleteFavouriteMedicine(user, id);

            Map<String, Object> response = new HashMap<>();
            response.put("id", id);
            response.put("deleted", deleted);
            response.put("message", "Favourite medicine deleted successfully");

            return successResponse(response);

        } catch (IllegalArgumentException e) {
            return errorResponse("Invalid request: " + e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =================== ENTITY MANAGEMENT OPERATIONS ===================

    /**
     * Search VTMs (Virtual Therapeutic Moieties)
     * GET /api/clinical/favourite_medicines/entities/vtms?query=amoxicillin&limit=20
     */
    @GET
    @Path("/entities/vtms")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVtms() {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Get query parameters
            String query = uriInfo.getQueryParameters().getFirst("query");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            if (query == null || query.trim().isEmpty()) {
                return errorResponse("Query parameter is required", 400);
            }

            Integer limit = null;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitStr.trim());
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            // Search VTMs
            List<Item> vtms = favouriteMedicineService.searchVtms(query.trim(), limit);

            // Convert to DTOs
            List<Map<String, Object>> responseData = new ArrayList<>();
            for (Item vtm : vtms) {
                responseData.add(convertItemToMap(vtm));
            }

            return successResponse(responseData);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create a new VTM
     * POST /api/clinical/favourite_medicines/entities/vtms
     */
    @POST
    @Path("/entities/vtms")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVtm(String jsonRequest) {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // TODO: Implement in service layer
            return errorResponse("Not implemented yet", 501);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Search ATMs (Actual Therapeutic Moieties)
     * GET /api/clinical/favourite_medicines/entities/atms
     */
    @GET
    @Path("/entities/atms")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchAtms() {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // TODO: Implement in service layer
            return errorResponse("Not implemented yet", 501);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create a new ATM
     * POST /api/clinical/favourite_medicines/entities/atms
     */
    @POST
    @Path("/entities/atms")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAtm(String jsonRequest) {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // TODO: Implement in service layer
            return errorResponse("Not implemented yet", 501);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Search VMPs (Virtual Medicinal Products)
     * GET /api/clinical/favourite_medicines/entities/vmps?query=paracetamol&limit=20
     */
    @GET
    @Path("/entities/vmps")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchVmps() {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Get query parameters
            String query = uriInfo.getQueryParameters().getFirst("query");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            if (query == null || query.trim().isEmpty()) {
                return errorResponse("Query parameter is required", 400);
            }

            Integer limit = null;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitStr.trim());
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            // Search VMPs
            List<Item> vmps = favouriteMedicineService.searchVmps(query.trim(), limit);

            // Convert to DTOs
            List<Map<String, Object>> responseData = new ArrayList<>();
            for (Item vmp : vmps) {
                responseData.add(convertItemToMap(vmp));
            }

            return successResponse(responseData);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create a new VMP
     * POST /api/clinical/favourite_medicines/entities/vmps
     */
    @POST
    @Path("/entities/vmps")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVmp(String jsonRequest) {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // TODO: Implement in service layer
            return errorResponse("Not implemented yet", 501);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Search AMPs (Actual Medicinal Products)
     * GET /api/clinical/favourite_medicines/entities/amps?query=paracetamol&limit=20
     */
    @GET
    @Path("/entities/amps")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchAmps() {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Get query parameters
            String query = uriInfo.getQueryParameters().getFirst("query");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            if (query == null || query.trim().isEmpty()) {
                return errorResponse("Query parameter is required", 400);
            }

            Integer limit = null;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitStr.trim());
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            // Search AMPs
            List<Item> amps = favouriteMedicineService.searchAmps(query.trim(), limit);

            // Convert to DTOs
            List<Map<String, Object>> responseData = new ArrayList<>();
            for (Item amp : amps) {
                responseData.add(convertItemToMap(amp));
            }

            return successResponse(responseData);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create a new AMP
     * POST /api/clinical/favourite_medicines/entities/amps
     */
    @POST
    @Path("/entities/amps")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAmp(String jsonRequest) {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // TODO: Implement in service layer
            return errorResponse("Not implemented yet", 501);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Search measurement units
     * GET /api/clinical/favourite_medicines/entities/units?query=ml&unitType=DoseUnit&limit=20
     */
    @GET
    @Path("/entities/units")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchMeasurementUnits() {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Get query parameters
            String query = uriInfo.getQueryParameters().getFirst("query");
            String unitType = uriInfo.getQueryParameters().getFirst("unitType");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            if (query == null || query.trim().isEmpty()) {
                return errorResponse("Query parameter is required", 400);
            }

            Integer limit = null;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitStr.trim());
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            // Search measurement units
            List<MeasurementUnit> units = favouriteMedicineService.searchMeasurementUnits(query.trim(), unitType, limit);

            // Convert to DTOs
            List<Map<String, Object>> responseData = new ArrayList<>();
            for (MeasurementUnit unit : units) {
                responseData.add(convertMeasurementUnitToMap(unit));
            }

            return successResponse(responseData);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create a new measurement unit
     * POST /api/clinical/favourite_medicines/entities/units
     */
    @POST
    @Path("/entities/units")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMeasurementUnit(String jsonRequest) {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // TODO: Implement in service layer
            return errorResponse("Not implemented yet", 501);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Search categories
     * GET /api/clinical/favourite_medicines/entities/categories?query=suspension&limit=20
     */
    @GET
    @Path("/entities/categories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCategories() {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Get query parameters
            String query = uriInfo.getQueryParameters().getFirst("query");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            if (query == null || query.trim().isEmpty()) {
                return errorResponse("Query parameter is required", 400);
            }

            Integer limit = null;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitStr.trim());
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            // Search categories
            List<Category> categories = favouriteMedicineService.searchCategories(query.trim(), limit);

            // Convert to DTOs
            List<Map<String, Object>> responseData = new ArrayList<>();
            for (Category category : categories) {
                responseData.add(convertCategoryToMap(category));
            }

            return successResponse(responseData);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create a new category
     * POST /api/clinical/favourite_medicines/entities/categories
     */
    @POST
    @Path("/entities/categories")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCategory(String jsonRequest) {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // TODO: Implement in service layer
            return errorResponse("Not implemented yet", 501);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =================== AI HELPER OPERATIONS ===================

    /**
     * Bulk validate entities for AI agent workflows
     * POST /api/clinical/favourite_medicines/validate
     *
     * Request body example:
     * {
     *   "entities": [
     *     {"name": "amoxicillin", "type": "VTM"},
     *     {"name": "ml", "type": "UNIT"},
     *     {"name": "suspension", "type": "CATEGORY"}
     *   ]
     * }
     */
    @POST
    @Path("/validate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateEntities(String jsonRequest) {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Parse JSON request
            if (jsonRequest == null || jsonRequest.trim().isEmpty()) {
                return errorResponse("Request body is required", 400);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> requestData = gson.fromJson(jsonRequest, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entities = (List<Map<String, Object>>) requestData.get("entities");

            if (entities == null || entities.isEmpty()) {
                return errorResponse("Entities list is required", 400);
            }

            // Validate entities
            Map<String, Object> validationResult = favouriteMedicineService.validateEntities(entities);

            return successResponse(validationResult);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Auto-suggest similar entities
     * POST /api/clinical/favourite_medicines/suggest
     */
    @POST
    @Path("/suggest")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response suggestEntities(String jsonRequest) {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // TODO: Implement in service layer
            return errorResponse("Not implemented yet", 501);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Parse natural language instructions for AI agents
     * POST /api/clinical/favourite_medicines/parse
     */
    @POST
    @Path("/parse")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response parseInstructions(String jsonRequest) {
        try {
            WebUser user = validateApiKey();
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // TODO: Implement in service layer
            return errorResponse("Not implemented yet", 501);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =================== HELPER METHODS ===================

    /**
     * Validate API key and return associated user
     * Follows same pattern as PharmacySearchApi
     */
    private WebUser validateApiKey() {
        String key = requestContext.getHeader("Finance");
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) {
            return null;
        }

        WebUser user = apiKey.getWebUser();
        if (user == null) {
            return null;
        }

        if (user.isRetired()) {
            return null;
        }

        if (!user.isActivated()) {
            return null;
        }

        // Treat null expiry date as expired
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) {
            return null;
        }

        return user;
    }

    /**
     * Create error response following established pattern
     */
    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(gson.toJson(response)).build();
    }

    /**
     * Create success response following established pattern
     */
    private Response successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.status(200).entity(gson.toJson(response)).build();
    }

    /**
     * Convert PrescriptionTemplate to Map for JSON response
     */
    private Map<String, Object> convertTemplateToMap(PrescriptionTemplate template) {
        Map<String, Object> map = new HashMap<>();

        if (template == null) {
            return map;
        }

        map.put("id", template.getId());

        // Item information
        if (template.getItem() != null) {
            map.put("itemName", template.getItem().getName());
            map.put("itemId", template.getItem().getId());
            map.put("itemType", template.getItem().getMedicineType() != null ?
                   template.getItem().getMedicineType().toString() : null);
        }

        // Age range (convert days back to years)
        map.put("fromYears", favouriteMedicineService.convertDaysToYears(template.getFromDays()));
        map.put("toYears", favouriteMedicineService.convertDaysToYears(template.getToDays()));

        // Category
        if (template.getCategory() != null) {
            map.put("categoryName", template.getCategory().getName());
            map.put("categoryId", template.getCategory().getId());
        }

        // Dosage
        map.put("dose", template.getDose());
        if (template.getDoseUnit() != null) {
            map.put("doseUnitName", template.getDoseUnit().getName());
            map.put("doseUnitId", template.getDoseUnit().getId());
        }

        // Frequency
        if (template.getFrequencyUnit() != null) {
            map.put("frequencyUnitName", template.getFrequencyUnit().getName());
            map.put("frequencyUnitId", template.getFrequencyUnit().getId());
        }

        // Duration
        map.put("duration", template.getDuration());
        if (template.getDurationUnit() != null) {
            map.put("durationUnitName", template.getDurationUnit().getName());
            map.put("durationUnitId", template.getDurationUnit().getId());
        }

        // Issue
        map.put("issue", template.getIssue());
        if (template.getIssueUnit() != null) {
            map.put("issueUnitName", template.getIssueUnit().getName());
            map.put("issueUnitId", template.getIssueUnit().getId());
        }

        // Other properties
        map.put("orderNo", template.getOrderNo());
        map.put("indoor", template.isIndoor());
        map.put("sex", template.getSex() != null ? template.getSex().toString() : null);

        // For item
        if (template.getForItem() != null) {
            map.put("forItem", template.getForItem().getName());
        }

        // Audit information
        if (template.getCreater() != null) {
            map.put("createdBy", template.getCreater().getWebUserPerson() != null ?
                   template.getCreater().getWebUserPerson().getNameWithTitle() :
                   template.getCreater().getName());
        }

        if (template.getCreatedAt() != null) {
            map.put("createdAt", gson.toJson(template.getCreatedAt()));
        }

        return map;
    }

    /**
     * Parse search parameters from query string
     */
    private Map<String, Object> parseSearchParams() {
        Map<String, Object> searchCriteria = new HashMap<>();

        // Get query parameters
        String query = uriInfo.getQueryParameters().getFirst("query");
        if (query != null && !query.trim().isEmpty()) {
            searchCriteria.put("query", query.trim());
        }

        String itemType = uriInfo.getQueryParameters().getFirst("itemType");
        if (itemType != null && !itemType.trim().isEmpty()) {
            searchCriteria.put("itemType", itemType.trim());
        }

        String categoryName = uriInfo.getQueryParameters().getFirst("categoryName");
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            searchCriteria.put("categoryName", categoryName.trim());
        }

        // Age filters
        String ageYears = uriInfo.getQueryParameters().getFirst("ageYears");
        if (ageYears != null && !ageYears.trim().isEmpty()) {
            try {
                searchCriteria.put("ageYears", Double.parseDouble(ageYears.trim()));
            } catch (NumberFormatException e) {
                // Ignore invalid age format
            }
        }

        String fromYears = uriInfo.getQueryParameters().getFirst("fromYears");
        if (fromYears != null && !fromYears.trim().isEmpty()) {
            try {
                searchCriteria.put("fromYears", Double.parseDouble(fromYears.trim()));
            } catch (NumberFormatException e) {
                // Ignore invalid format
            }
        }

        String toYears = uriInfo.getQueryParameters().getFirst("toYears");
        if (toYears != null && !toYears.trim().isEmpty()) {
            try {
                searchCriteria.put("toYears", Double.parseDouble(toYears.trim()));
            } catch (NumberFormatException e) {
                // Ignore invalid format
            }
        }

        // Other filters
        String sex = uriInfo.getQueryParameters().getFirst("sex");
        if (sex != null && !sex.trim().isEmpty()) {
            searchCriteria.put("sex", sex.trim());
        }

        String indoor = uriInfo.getQueryParameters().getFirst("indoor");
        if (indoor != null && !indoor.trim().isEmpty()) {
            searchCriteria.put("indoor", Boolean.parseBoolean(indoor.trim()));
        }

        String forItemName = uriInfo.getQueryParameters().getFirst("forItemName");
        if (forItemName != null && !forItemName.trim().isEmpty()) {
            searchCriteria.put("forItemName", forItemName.trim());
        }

        // Pagination and ordering
        String limit = uriInfo.getQueryParameters().getFirst("limit");
        if (limit != null && !limit.trim().isEmpty()) {
            try {
                searchCriteria.put("limit", Integer.parseInt(limit.trim()));
            } catch (NumberFormatException e) {
                // Ignore invalid limit
            }
        }

        String offset = uriInfo.getQueryParameters().getFirst("offset");
        if (offset != null && !offset.trim().isEmpty()) {
            try {
                searchCriteria.put("offset", Integer.parseInt(offset.trim()));
            } catch (NumberFormatException e) {
                // Ignore invalid offset
            }
        }

        String orderBy = uriInfo.getQueryParameters().getFirst("orderBy");
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            searchCriteria.put("orderBy", orderBy.trim());
        }

        String orderDirection = uriInfo.getQueryParameters().getFirst("orderDirection");
        if (orderDirection != null && !orderDirection.trim().isEmpty()) {
            searchCriteria.put("orderDirection", orderDirection.trim());
        }

        return searchCriteria;
    }

    /**
     * Convert Item to Map for JSON response
     */
    private Map<String, Object> convertItemToMap(Item item) {
        Map<String, Object> map = new HashMap<>();

        if (item == null) {
            return map;
        }

        map.put("id", item.getId());
        map.put("name", item.getName());
        map.put("code", item.getCode());
        map.put("description", item.getDescreption());
        map.put("itemType", item.getItemType() != null ? item.getItemType().toString() : null);
        map.put("retired", item.isRetired());

        if (item.getCategory() != null) {
            map.put("categoryName", item.getCategory().getName());
            map.put("categoryId", item.getCategory().getId());
        }

        return map;
    }

    /**
     * Convert MeasurementUnit to Map for JSON response
     */
    private Map<String, Object> convertMeasurementUnitToMap(MeasurementUnit unit) {
        Map<String, Object> map = new HashMap<>();

        if (unit == null) {
            return map;
        }

        map.put("id", unit.getId());
        map.put("name", unit.getName());
        map.put("code", unit.getCode());
        map.put("description", unit.getDescription());
        // Determine unit type based on boolean flags
        String unitTypeStr = null;
        if (unit.isStrengthUnit()) unitTypeStr = "DoseUnit";
        else if (unit.isFrequencyUnit()) unitTypeStr = "FrequencyUnit";
        else if (unit.isDurationUnit()) unitTypeStr = "DurationUnit";
        else if (unit.isIssueUnit()) unitTypeStr = "IssueUnit";
        else if (unit.isPackUnit()) unitTypeStr = "PackUnit";

        map.put("unitType", unitTypeStr);
        map.put("retired", unit.isRetired());

        return map;
    }

    /**
     * Convert Category to Map for JSON response
     */
    private Map<String, Object> convertCategoryToMap(Category category) {
        Map<String, Object> map = new HashMap<>();

        if (category == null) {
            return map;
        }

        map.put("id", category.getId());
        map.put("name", category.getName());
        map.put("code", category.getCode());
        map.put("description", category.getDescription());
        map.put("retired", category.isRetired());

        return map;
    }
}