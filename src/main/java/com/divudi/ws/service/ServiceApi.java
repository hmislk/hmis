/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.service;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.service.ItemFeeCreateRequestDTO;
import com.divudi.core.data.dto.service.ItemFeeDTO;
import com.divudi.core.data.dto.service.ItemFeeUpdateRequestDTO;
import com.divudi.core.data.dto.service.ServiceCategoryDTO;
import com.divudi.core.data.dto.service.ServiceCreateRequestDTO;
import com.divudi.core.data.dto.service.ServiceResponseDTO;
import com.divudi.core.data.dto.service.ServiceSearchResultDTO;
import com.divudi.core.data.dto.service.ServiceUpdateRequestDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.service.ServiceApiService;
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
 * REST API for Service Management.
 * Manages OPD Services, Inward Services, their fees, and service categories.
 * Follows the same pattern as InstitutionApi.
 *
 * @author Buddhika
 */
@Path("services")
@RequestScoped
public class ServiceApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private ServiceApiService serviceApiService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public ServiceApi() {
    }

    // =========================================================================
    // Service CRUD
    // =========================================================================

    /**
     * Search services by name, type, category, and active status.
     * GET /api/services/search?query=ward&serviceType=Inward&limit=30
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchServices() {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String query = uriInfo.getQueryParameters().getFirst("query");
            String serviceType = uriInfo.getQueryParameters().getFirst("serviceType");
            String categoryIdStr = uriInfo.getQueryParameters().getFirst("categoryId");
            String inactiveStr = uriInfo.getQueryParameters().getFirst("inactive");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            Long categoryId = null;
            if (categoryIdStr != null && !categoryIdStr.trim().isEmpty()) {
                try {
                    categoryId = Long.parseLong(categoryIdStr.trim());
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid categoryId format", 400);
                }
            }

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

            List<ServiceSearchResultDTO> results = serviceApiService.searchServices(
                    query, serviceType, categoryId, inactive, limit);
            return successResponse(results);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get service by ID including fees.
     * GET /api/services/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServiceById(@PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            ServiceResponseDTO result = serviceApiService.findServiceById(id);
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
     * Create a new Service or InwardService.
     * POST /api/services
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createService(String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            ServiceCreateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, ServiceCreateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            ServiceResponseDTO response = serviceApiService.createService(request, user);
            return Response.status(201).entity(gson.toJson(successData(response))).build();

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update an existing service.
     * PUT /api/services/{id}
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateService(@PathParam("id") Long id, String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            ServiceUpdateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, ServiceUpdateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            ServiceResponseDTO response = serviceApiService.updateService(id, request, user);
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
     * Retire a service (soft delete, sets retired=true permanently).
     * DELETE /api/services/{id}?retireComments=reason
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireService(@PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String retireComments = uriInfo.getQueryParameters().getFirst("retireComments");
            ServiceResponseDTO response = serviceApiService.retireService(id, retireComments, user);
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
     * Activate a service (set inactive=false).
     * PATCH /api/services/{id}/activate
     */
    @PATCH
    @Path("/{id}/activate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response activateService(@PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            ServiceResponseDTO response = serviceApiService.activateService(id, user);
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
     * Deactivate a service (set inactive=true).
     * PATCH /api/services/{id}/deactivate
     */
    @PATCH
    @Path("/{id}/deactivate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deactivateService(@PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            ServiceResponseDTO response = serviceApiService.deactivateService(id, user);
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
     * List all fees for a service.
     * GET /api/services/{id}/fees
     */
    @GET
    @Path("/{id}/fees")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listFees(@PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            List<ItemFeeDTO> fees = serviceApiService.listFees(id);
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
     * Add a fee to a service.
     * POST /api/services/{id}/fees
     */
    @POST
    @Path("/{id}/fees")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addFee(@PathParam("id") Long id, String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            ItemFeeCreateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, ItemFeeCreateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            ServiceResponseDTO response = serviceApiService.addFee(id, request, user);
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
     * PUT /api/services/{id}/fees/{feeId}
     */
    @PUT
    @Path("/{id}/fees/{feeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFee(@PathParam("id") Long id, @PathParam("feeId") Long feeId, String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            ItemFeeUpdateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, ItemFeeUpdateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            ServiceResponseDTO response = serviceApiService.updateFee(id, feeId, request, user);
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
     * DELETE /api/services/{id}/fees/{feeId}
     */
    @DELETE
    @Path("/{id}/fees/{feeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeFee(@PathParam("id") Long id, @PathParam("feeId") Long feeId) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            ServiceResponseDTO response = serviceApiService.removeFee(id, feeId, user);
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
    // Service Category CRUD
    // =========================================================================

    /**
     * Search service categories.
     * GET /api/services/categories/search?query=surgery&limit=20
     */
    @GET
    @Path("/categories/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCategories() {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String query = uriInfo.getQueryParameters().getFirst("query");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            int limit = 30;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    int parsed = Integer.parseInt(limitStr.trim());
                    limit = Math.min(Math.max(parsed, 1), 100);
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            List<ServiceCategoryDTO> results = serviceApiService.searchServiceCategories(query, limit);
            return successResponse(results);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get service category by ID.
     * GET /api/services/categories/{id}
     */
    @GET
    @Path("/categories/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCategoryById(@PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            ServiceCategoryDTO result = serviceApiService.findServiceCategoryById(id);
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
     * Create a new service category.
     * POST /api/services/categories
     * Body: {"name": "...", "code": "...", "description": "..."}
     */
    @POST
    @Path("/categories")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCategory(String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            @SuppressWarnings("unchecked")
            Map<String, String> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (body == null) {
                return errorResponse("Request body is required", 400);
            }

            String name = body.get("name");
            String code = body.get("code");
            String description = body.get("description");

            ServiceCategoryDTO response = serviceApiService.createServiceCategory(name, code, description, user);
            return Response.status(201).entity(gson.toJson(successData(response))).build();

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update a service category.
     * PUT /api/services/categories/{id}
     * Body: {"name": "...", "code": "...", "description": "..."}
     */
    @PUT
    @Path("/categories/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCategory(@PathParam("id") Long id, String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            @SuppressWarnings("unchecked")
            Map<String, String> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (body == null) {
                return errorResponse("Request body is required", 400);
            }

            String name = body.get("name");
            String code = body.get("code");
            String description = body.get("description");

            ServiceCategoryDTO response = serviceApiService.updateServiceCategory(id, name, code, description, user);
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
     * Retire a service category.
     * DELETE /api/services/categories/{id}?retireComments=reason
     */
    @DELETE
    @Path("/categories/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireCategory(@PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String retireComments = uriInfo.getQueryParameters().getFirst("retireComments");
            ServiceCategoryDTO response = serviceApiService.retireServiceCategory(id, retireComments, user);
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
