/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.inward;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.timeditem.TimedItemCreateRequestDTO;
import com.divudi.core.data.dto.timeditem.TimedItemFeeCreateRequestDTO;
import com.divudi.core.data.dto.timeditem.TimedItemFeeDTO;
import com.divudi.core.data.dto.timeditem.TimedItemFeeUpdateRequestDTO;
import com.divudi.core.data.dto.timeditem.TimedItemResponseDTO;
import com.divudi.core.data.dto.timeditem.TimedItemSearchResultDTO;
import com.divudi.core.data.dto.timeditem.TimedItemUpdateRequestDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.inward.TimedItemApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Date;
import java.util.HashMap;
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
            String inactiveStr = uriInfo.getQueryParameters().getFirst("inactive");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            Boolean inactive = null;
            if (inactiveStr != null && !inactiveStr.trim().isEmpty()) {
                inactive = Boolean.parseBoolean(inactiveStr.trim());
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

            List<TimedItemSearchResultDTO> results = timedItemApiService.searchTimedItems(
                    query, departmentType, inactive, limit);
            return successResponse(results);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
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

            TimedItemResponseDTO result = timedItemApiService.findTimedItemById(id);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
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
            return Response.status(201).entity(gson.toJson(successData(response))).build();

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
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
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
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
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
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
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
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
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

            List<TimedItemFeeDTO> fees = timedItemApiService.listFees(id);
            return successResponse(fees);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
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
            return Response.status(201).entity(gson.toJson(successData(response))).build();

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
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
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
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
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

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
        return Response.status(200).entity(gson.toJson(successData(data))).build();
    }

    private Map<String, Object> successData(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return response;
    }
}
