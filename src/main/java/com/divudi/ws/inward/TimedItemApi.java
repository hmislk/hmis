/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.inward;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.timeditem.TimedItemCreateRequestDTO;
import com.divudi.core.data.dto.timeditem.TimedItemFeeBulkRequestDTO;
import com.divudi.core.data.dto.timeditem.TimedItemFeeCreateRequestDTO;
import com.divudi.core.data.dto.timeditem.TimedItemFeeDTO;
import com.divudi.core.data.dto.timeditem.TimedItemFeeUpdateRequestDTO;
import com.divudi.core.data.dto.timeditem.TimedItemResponseDTO;
import com.divudi.core.data.dto.timeditem.TimedItemSearchPageDTO;
import com.divudi.core.data.dto.timeditem.TimedItemUpdateRequestDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.TimedItemCategory;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.service.inward.TimedItemApiService;
import com.divudi.service.inward.TimedItemFeeRuleException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javax.ejb.EJB;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for Timed Item management.
 * Manages TimedItem entities (room rent, oxygen, etc.) and their tiered fees (TimedItemFee).
 *
 * <p>Authentication: Finance header with a valid API key.
 *
 * @author Buddhika
 */
@Path("timed-items")
@RequestScoped
public class TimedItemApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private TimedItemApiService timedItemApiService;

    @EJB
    private CategoryFacade categoryFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public TimedItemApi() {
    }

    // =========================================================================
    // Timed Item CRUD
    // =========================================================================

    /**
     * Search timed items.
     * GET /api/timed-items/search?query=oxygen&departmentType=Inward&limit=30
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchTimedItems() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String query = uriInfo.getQueryParameters().getFirst("query");
            String departmentType = uriInfo.getQueryParameters().getFirst("departmentType");
            String inwardChargeType = uriInfo.getQueryParameters().getFirst("inwardChargeType");
            String inactiveStr = uriInfo.getQueryParameters().getFirst("inactive");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");
            String offsetStr = uriInfo.getQueryParameters().getFirst("offset");

            Boolean inactive = null;
            if (inactiveStr != null && !inactiveStr.trim().isEmpty()) {
                String raw = inactiveStr.trim().toLowerCase();
                if (!"true".equals(raw) && !"false".equals(raw)) {
                    return errorResponse("Invalid inactive value. Use true or false.", 400);
                }
                inactive = Boolean.parseBoolean(raw);
            }

            Boolean includeRetired = parseBooleanParam("includeRetired");
            if (includeRetired == null) {
                return errorResponse("Invalid includeRetired value. Use true or false.", 400);
            }

            Long categoryId;
            Long departmentId;
            Long institutionId;
            try {
                categoryId = parseLongParam("categoryId");
                departmentId = parseLongParam("departmentId");
                institutionId = parseLongParam("institutionId");
            } catch (NumberFormatException e) {
                return errorResponse(e.getMessage(), 400);
            }

            int limit = 30;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    int parsed = Integer.parseInt(limitStr.trim());
                    limit = Math.min(Math.max(parsed, 1), 100);
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            int offset = 0;
            if (offsetStr != null && !offsetStr.trim().isEmpty()) {
                try {
                    offset = Math.max(Integer.parseInt(offsetStr.trim()), 0);
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid offset format", 400);
                }
            }

            TimedItemSearchPageDTO results = timedItemApiService.searchTimedItems(
                    query, departmentType, inactive, categoryId, inwardChargeType,
                    departmentId, institutionId, includeRetired, limit, offset);
            return successResponse(results);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Invalid")) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + msg, 500);
        }
    }

    /**
     * Get a timed item by ID including its fees.
     * GET /api/timed-items/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTimedItemById(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Boolean includeRetired = parseBooleanParam("includeRetired");
            if (includeRetired == null) {
                return errorResponse("Invalid includeRetired value. Use true or false.", 400);
            }

            TimedItemResponseDTO result = timedItemApiService.findTimedItemById(id, includeRetired);
            return successResponse(result);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Create a new timed item.
     * POST /api/timed-items
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createTimedItem(String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            TimedItemCreateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, TimedItemCreateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            TimedItemResponseDTO response = timedItemApiService.createTimedItem(request, user);
            return successResponse(201, response);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update an existing timed item.
     * PUT /api/timed-items/{id}
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTimedItem(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            TimedItemUpdateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, TimedItemUpdateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            TimedItemResponseDTO response = timedItemApiService.updateTimedItem(id, request, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Retire a timed item (soft delete).
     * DELETE /api/timed-items/{id}?retireComments=reason
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireTimedItem(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String retireComments = uriInfo.getQueryParameters().getFirst("retireComments");
            TimedItemResponseDTO response = timedItemApiService.retireTimedItem(id, retireComments, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Un-retire a timed item, undoing DELETE /api/timed-items/{id}.
     * PATCH /api/timed-items/{id}/restore
     */
    @PATCH
    @Path("/{id}/restore")
    @Produces(MediaType.APPLICATION_JSON)
    public Response restoreTimedItem(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            TimedItemResponseDTO response = timedItemApiService.restoreTimedItem(id, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Activate a timed item (set inactive=false).
     * PATCH /api/timed-items/{id}/activate
     */
    @PATCH
    @Path("/{id}/activate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response activateTimedItem(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            TimedItemResponseDTO response = timedItemApiService.activateTimedItem(id, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Deactivate a timed item (set inactive=true).
     * PATCH /api/timed-items/{id}/deactivate
     */
    @PATCH
    @Path("/{id}/deactivate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deactivateTimedItem(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            TimedItemResponseDTO response = timedItemApiService.deactivateTimedItem(id, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    // =========================================================================
    // Fee Management
    // =========================================================================

    /**
     * List all fees for a timed item, ordered by sortOrder.
     * GET /api/timed-items/{id}/fees
     */
    @GET
    @Path("/{id}/fees")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listFees(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Boolean includeRetired = parseBooleanParam("includeRetired");
            if (includeRetired == null) {
                return errorResponse("Invalid includeRetired value. Use true or false.", 400);
            }

            List<TimedItemFeeDTO> fees = timedItemApiService.listFees(id, includeRetired);
            return successResponse(fees);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Add a fee to a timed item.
     * POST /api/timed-items/{id}/fees
     */
    @POST
    @Path("/{id}/fees")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addFee(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            TimedItemFeeCreateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, TimedItemFeeCreateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            TimedItemResponseDTO response = timedItemApiService.addFee(id, request, user);
            return successResponse(201, response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Update a fee.
     * PUT /api/timed-items/{id}/fees/{feeId}
     */
    @PUT
    @Path("/{id}/fees/{feeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFee(@PathParam("id") Long id, @PathParam("feeId") Long feeId, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            TimedItemFeeUpdateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, TimedItemFeeUpdateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            TimedItemResponseDTO response = timedItemApiService.updateFee(id, feeId, request, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Remove a fee (soft-delete).
     * DELETE /api/timed-items/{id}/fees/{feeId}
     */
    @DELETE
    @Path("/{id}/fees/{feeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeFee(@PathParam("id") Long id, @PathParam("feeId") Long feeId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            TimedItemResponseDTO response = timedItemApiService.removeFee(id, feeId, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Un-retire a fee, undoing DELETE /api/timed-items/{id}/fees/{feeId}.
     * PATCH /api/timed-items/{id}/fees/{feeId}/restore
     */
    @PATCH
    @Path("/{id}/fees/{feeId}/restore")
    @Produces(MediaType.APPLICATION_JSON)
    public Response restoreFee(@PathParam("id") Long id, @PathParam("feeId") Long feeId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            TimedItemResponseDTO response = timedItemApiService.restoreFee(id, feeId, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Replace the whole fee slot list in one atomic call.
     * PUT /api/timed-items/{id}/fees
     * Body: { "fees": [ { "id": 123, "name": "First hour", "fee": 500, "durationHours": 1,
     *                     "durationUnit": "HOUR", "sortOrder": 1 }, ... ] }
     * Fees present in the item but absent from the list are retired.
     */
    @PUT
    @Path("/{id}/fees")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response replaceFees(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            TimedItemFeeBulkRequestDTO request;
            try {
                request = gson.fromJson(requestBody, TimedItemFeeBulkRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            TimedItemResponseDTO response = timedItemApiService.replaceFees(id, request, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    // =========================================================================
    // Timed Item Category CRUD
    // =========================================================================

    /**
     * List all non-retired TimedItemCategory entries.
     * GET /api/timed-items/categories?query=&limit=50
     */
    @GET
    @Path("/categories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCategories() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            String query = uriInfo.getQueryParameters().getFirst("query");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");
            int limit = 50;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    limit = Math.min(Math.max(Integer.parseInt(limitStr.trim()), 1), 500);
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }
            StringBuilder jpql = new StringBuilder(
                    "select c from TimedItemCategory c where c.retired = false");
            Map<String, Object> params = new HashMap<>();
            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and upper(c.name) like :q");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }
            jpql.append(" order by c.name");
            List<Category> cats = categoryFacade.findByJpql(jpql.toString(), params, limit);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (cats != null) {
                for (Category c : cats) {
                    payload.add(toCategoryDto(c));
                }
            }
            return successResponse(payload);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get a single TimedItemCategory by id.
     * GET /api/timed-items/categories/{id}
     */
    @GET
    @Path("/categories/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCategory(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            Category c = categoryFacade.find(id);
            if (c == null || c.isRetired() || !(c instanceof TimedItemCategory)) {
                return errorResponse("Timed item category not found: " + id, 404);
            }
            return successResponse(toCategoryDto(c));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create a new TimedItemCategory.
     * POST /api/timed-items/categories
     * Body: { "name": "ICU Charges", "code": "ICU" }
     */
    @POST
    @Path("/categories")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCategory(String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            Map<?, ?> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (body == null) {
                return errorResponse("Request body is required", 400);
            }
            Object nameObj = body.get("name");
            if (nameObj == null || nameObj.toString().trim().isEmpty()) {
                return errorResponse("name is required", 400);
            }
            String name = nameObj.toString().trim();
            String code = body.get("code") != null ? body.get("code").toString().trim() : null;

            Map<String, Object> dupParams = new HashMap<>();
            dupParams.put("name", name.toUpperCase());
            List<Category> existing = categoryFacade.findByJpql(
                    "select c from TimedItemCategory c where upper(c.name) = :name and c.retired = false",
                    dupParams, 1);
            if (existing != null && !existing.isEmpty()) {
                Map<String, Object> conflict = new LinkedHashMap<>();
                conflict.put("status", "already_exists");
                conflict.put("id", existing.get(0).getId());
                conflict.put("name", existing.get(0).getName());
                return Response.status(409).entity(gson.toJson(conflict)).build();
            }

            TimedItemCategory cat = new TimedItemCategory();
            cat.setName(name);
            if (code != null && !code.isEmpty()) {
                cat.setCode(code);
            }
            cat.setCreatedAt(new Date());
            cat.setCreater(user);
            categoryFacade.create(cat);
            return successResponse(201, toCategoryDto(cat));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update an existing TimedItemCategory.
     * PUT /api/timed-items/categories/{id}
     * Body: { "name": "New Name", "code": "NEW" }
     */
    @PUT
    @Path("/categories/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCategory(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            Category c = categoryFacade.find(id);
            if (c == null || c.isRetired() || !(c instanceof TimedItemCategory)) {
                return errorResponse("Timed item category not found: " + id, 404);
            }
            Map<?, ?> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (body == null) {
                return errorResponse("Request body is required", 400);
            }
            if (body.containsKey("name")) {
                Object nameObj = body.get("name");
                if (nameObj == null || nameObj.toString().trim().isEmpty()) {
                    return errorResponse("name cannot be empty", 400);
                }
                c.setName(nameObj.toString().trim());
            }
            if (body.containsKey("code")) {
                Object codeObj = body.get("code");
                c.setCode(codeObj != null ? codeObj.toString().trim() : null);
            }
            categoryFacade.edit(c);
            return successResponse(toCategoryDto(c));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Soft-retire a TimedItemCategory.
     * DELETE /api/timed-items/categories/{id}?retireComments=reason
     */
    @DELETE
    @Path("/categories/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireCategory(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            Category c = categoryFacade.find(id);
            if (c == null || !(c instanceof TimedItemCategory)) {
                return errorResponse("Timed item category not found: " + id, 404);
            }
            if (c.isRetired()) {
                return errorResponse("Timed item category is already retired: " + id, 400);
            }
            c.setRetired(true);
            c.setRetiredAt(new Date());
            c.setRetirer(user);
            String retireComments = uriInfo.getQueryParameters().getFirst("retireComments");
            if (retireComments != null && !retireComments.trim().isEmpty()) {
                c.setRetireComments(retireComments.trim());
            }
            categoryFacade.edit(c);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", c.getId());
            resp.put("retired", true);
            return successResponse(resp);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    private Map<String, Object> toCategoryDto(Category c) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", c.getId());
        dto.put("name", c.getName());
        dto.put("code", c.getCode());
        dto.put("retired", c.isRetired());
        return dto;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Maps a service-layer exception onto an HTTP status.
     *
     * <p>"is retired" / "is not retired" are 404 and 409 rather than the 500 they used to
     * fall through to — a retired record is a real, findable state now that
     * includeRetired and restore exist, not an internal failure (issue #23236 §2).
     */
    private Response mapError(Exception e) {
        String msg = e.getMessage();
        // A broken rule is always the caller's payload, never a server fault — checked by
        // type so the status does not depend on how the message happens to be worded.
        if (e instanceof TimedItemFeeRuleException) {
            return errorResponse(msg, 400);
        }
        if (msg == null) {
            return errorResponse("An error occurred: Unknown error", 500);
        }
        if (msg.contains("not found")) {
            return errorResponse(msg, 404);
        }
        if (msg.contains("is retired")) {
            return errorResponse(msg + " — use ?includeRetired=true to read it, "
                    + "or PATCH the restore endpoint to bring it back.", 404);
        }
        if (msg.contains("is not retired") || msg.contains("is already retired")) {
            return errorResponse(msg, 409);
        }
        if (msg.contains("Invalid") || msg.contains("required") || msg.contains("does not belong")
                || msg.contains("must be") || msg.contains("Slot Order") || msg.contains("Duration must")) {
            return errorResponse(msg, 400);
        }
        return errorResponse("An error occurred: " + msg, 500);
    }

    /**
     * @return the flag, or null when the caller sent something that is neither true nor
     *         false — silently treating a typo as false would hide retired rows the
     *         caller explicitly asked for
     */
    private Boolean parseBooleanParam(String name) {
        String raw = uriInfo.getQueryParameters().getFirst(name);
        if (raw == null || raw.trim().isEmpty()) {
            return Boolean.FALSE;
        }
        String v = raw.trim().toLowerCase();
        if ("true".equals(v)) {
            return Boolean.TRUE;
        }
        if ("false".equals(v)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Long parseLongParam(String name) {
        String raw = uriInfo.getQueryParameters().getFirst(name);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid " + name + " format");
        }
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) {
            return null;
        }
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) {
            return null;
        }
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) {
            return null;
        }
        return user;
    }

    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(gson.toJson(response)).build();
    }

    private Response successResponse(Object data) {
        return successResponse(200, data);
    }

    private Response successResponse(int status, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", status);
        response.put("data", data);
        return Response.status(status).entity(gson.toJson(response)).build();
    }

    private Map<String, Object> successData(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return response;
    }
}
