/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.pharmacy.PharmaceuticalItemApiService;
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
 * REST API for Pharmaceutical Item Management.
 * Provides endpoints for managing VTM, ATM, VMP, AMP, VMPP, and AMPP entities.
 * Supports search, create, update, retire/unretire, and activate/deactivate.
 *
 * @author Buddhika
 */
@Path("pharmaceutical_items")
@RequestScoped
public class PharmaceuticalItemApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private PharmaceuticalItemApiService itemService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public PharmaceuticalItemApi() {
    }

    /**
     * Search pharmaceutical items by name or code.
     * GET /api/pharmaceutical_items/{type}/search?query=&departmentType=&limit=
     */
    @GET
    @Path("/{type}/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchItems(@PathParam("type") String type) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String query = uriInfo.getQueryParameters().getFirst("query");
            String departmentType = uriInfo.getQueryParameters().getFirst("departmentType");
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

            List<?> results = itemService.searchItems(type, query.trim(), departmentType, limit);
            return successResponse(results);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Invalid item type")) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    /**
     * Get pharmaceutical item by ID.
     * GET /api/pharmaceutical_items/{type}/{id}
     */
    @GET
    @Path("/{type}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItemById(@PathParam("type") String type, @PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Item ID is required", 400);
            }

            Object result = itemService.findItemById(type, id);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && msg.contains("Invalid item type")) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    /**
     * Create new pharmaceutical item.
     * POST /api/pharmaceutical_items/{type}
     */
    @POST
    @Path("/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createItem(@PathParam("type") String type, String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (requestBody == null || requestBody.trim().isEmpty()) {
                return errorResponse("Request body is required", 400);
            }

            Object result = itemService.createItem(type, requestBody, user, gson);
            return successResponse(result);

        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("not found") || msg.contains("is required") || msg.contains("Invalid"))) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    /**
     * Update existing pharmaceutical item.
     * PUT /api/pharmaceutical_items/{type}/{id}
     */
    @PUT
    @Path("/{type}/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateItem(@PathParam("type") String type, @PathParam("id") Long id, String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Item ID is required", 400);
            }

            if (requestBody == null || requestBody.trim().isEmpty()) {
                return errorResponse("Request body is required", 400);
            }

            Object result = itemService.updateItem(type, id, requestBody, user, gson);
            return successResponse(result);

        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && (msg.contains("is required") || msg.contains("Invalid"))) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    /**
     * Retire pharmaceutical item (soft delete).
     * DELETE /api/pharmaceutical_items/{type}/{id}?retireComments=xxx
     */
    @DELETE
    @Path("/{type}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireItem(@PathParam("type") String type, @PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Item ID is required", 400);
            }

            String retireComments = uriInfo.getQueryParameters().getFirst("retireComments");
            Object result = itemService.retireItem(type, id, retireComments, user);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && msg.contains("already retired")) {
                return errorResponse(msg, 409);
            }
            if (msg != null && msg.contains("Invalid item type")) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    /**
     * Unretire (restore) a retired pharmaceutical item.
     * PUT /api/pharmaceutical_items/{type}/{id}/restore
     */
    @PUT
    @Path("/{type}/{id}/restore")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unretireItem(@PathParam("type") String type, @PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Item ID is required", 400);
            }

            Object result = itemService.unretireItem(type, id, user);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && msg.contains("not retired")) {
                return errorResponse(msg, 409);
            }
            if (msg != null && msg.contains("Invalid item type")) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    /**
     * Activate or deactivate a pharmaceutical item.
     * PUT /api/pharmaceutical_items/{type}/{id}/status?active=true/false
     */
    @PUT
    @Path("/{type}/{id}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setItemStatus(@PathParam("type") String type, @PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Item ID is required", 400);
            }

            String activeStr = uriInfo.getQueryParameters().getFirst("active");
            if (activeStr == null || activeStr.trim().isEmpty()) {
                return errorResponse("'active' query parameter is required (true/false)", 400);
            }

            boolean active = Boolean.parseBoolean(activeStr.trim());
            Object result = itemService.setItemActiveStatus(type, id, active, user);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && msg.contains("Invalid item type")) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private WebUser validateApiKey(String key) {
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
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.status(200).entity(gson.toJson(response)).build();
    }
}
