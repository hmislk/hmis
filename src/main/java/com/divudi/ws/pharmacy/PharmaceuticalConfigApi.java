/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.pharmaceutical.CategoryConfigRequestDTO;
import com.divudi.core.data.dto.pharmaceutical.MeasurementUnitRequestDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.pharmacy.PharmaceuticalConfigApiService;
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
 * REST API for Pharmaceutical Configuration Management.
 * Provides endpoints for managing categories, dosage forms, and measurement units.
 *
 * @author Buddhika
 */
@Path("pharmaceutical_config")
@RequestScoped
public class PharmaceuticalConfigApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private PharmaceuticalConfigApiService configService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public PharmaceuticalConfigApi() {
    }

    /**
     * Search config entities by name or code.
     * GET /api/pharmaceutical_config/{type}/search?query=&limit=
     */
    @GET
    @Path("/{type}/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchConfigs(@PathParam("type") String type) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

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

            List<?> results = configService.searchConfigs(type, query.trim(), limit);
            return successResponse(results);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Invalid config type")) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    /**
     * Get config entity by ID.
     * GET /api/pharmaceutical_config/{type}/{id}
     */
    @GET
    @Path("/{type}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfigById(@PathParam("type") String type, @PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Config ID is required", 400);
            }

            Object result = configService.findConfigById(type, id);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && msg.contains("Invalid config type")) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    /**
     * Create new config entity.
     * POST /api/pharmaceutical_config/{type}
     */
    @POST
    @Path("/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createConfig(@PathParam("type") String type, String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (requestBody == null || requestBody.trim().isEmpty()) {
                return errorResponse("Request body is required", 400);
            }

            CategoryConfigRequestDTO request;
            try {
                if ("units".equals(type)) {
                    request = gson.fromJson(requestBody, MeasurementUnitRequestDTO.class);
                } else {
                    request = gson.fromJson(requestBody, CategoryConfigRequestDTO.class);
                }
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            Object result = configService.createConfig(type, request, user);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("is required") || msg.contains("Invalid"))) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    /**
     * Update existing config entity.
     * PUT /api/pharmaceutical_config/{type}/{id}
     */
    @PUT
    @Path("/{type}/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateConfig(@PathParam("type") String type, @PathParam("id") Long id, String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Config ID is required", 400);
            }

            if (requestBody == null || requestBody.trim().isEmpty()) {
                return errorResponse("Request body is required", 400);
            }

            CategoryConfigRequestDTO request;
            try {
                if ("units".equals(type)) {
                    request = gson.fromJson(requestBody, MeasurementUnitRequestDTO.class);
                } else {
                    request = gson.fromJson(requestBody, CategoryConfigRequestDTO.class);
                }
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            Object result = configService.updateConfig(type, id, request, user);
            return successResponse(result);

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
     * Retire config entity (soft delete).
     * DELETE /api/pharmaceutical_config/{type}/{id}?retireComments=xxx
     */
    @DELETE
    @Path("/{type}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireConfig(@PathParam("type") String type, @PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Config ID is required", 400);
            }

            String retireComments = uriInfo.getQueryParameters().getFirst("retireComments");
            Object result = configService.retireConfig(type, id, retireComments, user);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && msg.contains("already retired")) {
                return errorResponse(msg, 409);
            }
            if (msg != null && msg.contains("Invalid config type")) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    /**
     * Unretire (restore) a retired config entity.
     * PUT /api/pharmaceutical_config/{type}/{id}/restore
     */
    @PUT
    @Path("/{type}/{id}/restore")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unretireConfig(@PathParam("type") String type, @PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Config ID is required", 400);
            }

            Object result = configService.unretireConfig(type, id, user);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && msg.contains("not retired")) {
                return errorResponse(msg, 409);
            }
            if (msg != null && msg.contains("Invalid config type")) {
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
