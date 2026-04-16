/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.institution;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.config.DepartmentConfigDTO;
import com.divudi.core.data.dto.config.DepartmentConfigUpdateDTO;
import com.divudi.core.data.dto.department.DepartmentCreateRequestDTO;
import com.divudi.core.data.dto.department.DepartmentRelationshipUpdateDTO;
import com.divudi.core.data.dto.department.DepartmentResponseDTO;
import com.divudi.core.data.dto.department.DepartmentUpdateRequestDTO;
import com.divudi.core.data.dto.search.DepartmentDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.institution.ConfigOptionApiService;
import com.divudi.service.institution.DepartmentApiService;
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
 * REST API for Department Management
 * Provides endpoints for creating, updating, retiring, and searching departments
 * Includes department configuration management endpoints
 * Follows patterns from PharmacySearchApi and PharmacyAdjustmentApi
 *
 * @author Buddhika
 */
@Path("departments")
@RequestScoped
public class DepartmentApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private DepartmentApiService departmentService;

    @Inject
    private ConfigOptionApiService configOptionService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public DepartmentApi() {
    }

    /**
     * Search departments by name or code with optional type and institution filtering
     * GET /api/departments/search?query=Pharmacy&type=Pharmacy&institutionId=123&limit=20
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchDepartments() {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Get query parameters
            String query = uriInfo.getQueryParameters().getFirst("query");
            String typeStr = uriInfo.getQueryParameters().getFirst("type");
            String institutionIdStr = uriInfo.getQueryParameters().getFirst("institutionId");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            if (query == null || query.trim().isEmpty()) {
                return errorResponse("Query parameter is required", 400);
            }

            // Parse department type if provided
            DepartmentType type = null;
            if (typeStr != null && !typeStr.trim().isEmpty()) {
                try {
                    type = DepartmentType.valueOf(typeStr.trim());
                } catch (IllegalArgumentException e) {
                    return errorResponse("Invalid department type: " + typeStr, 400);
                }
            }

            // Parse institution ID if provided
            Long institutionId = null;
            if (institutionIdStr != null && !institutionIdStr.trim().isEmpty()) {
                try {
                    institutionId = Long.parseLong(institutionIdStr.trim());
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid institution ID format", 400);
                }
            }

            // Parse limit if provided
            Integer limit = null;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitStr.trim());
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            // Perform search
            List<DepartmentDTO> results = departmentService.searchDepartments(query.trim(), type, institutionId, limit);

            return successResponse(results);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get department by ID
     * GET /api/departments/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDepartmentById(@PathParam("id") Long id) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Department ID is required", 400);
            }

            // Get department
            DepartmentResponseDTO result = departmentService.findDepartmentById(id);

            return successResponse(result);

        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return errorResponse(e.getMessage(), 404);
            }
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create new department
     * POST /api/departments
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDepartment(String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Parse request body
            DepartmentCreateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, DepartmentCreateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            // Create department
            DepartmentResponseDTO response = departmentService.createDepartment(request, user);
            return successResponse(response);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update existing department
     * PUT /api/departments/{id}
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDepartment(@PathParam("id") Long id, String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Department ID is required", 400);
            }

            // Parse request body
            DepartmentUpdateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, DepartmentUpdateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            // Validate path id against payload id if present
            if (request.getId() != null && !request.getId().equals(id)) {
                return errorResponse("Path id and payload id mismatch", 400);
            }

            // Set id from path parameter
            request.setId(id);

            // Update department
            DepartmentResponseDTO response = departmentService.updateDepartment(id, request, user);
            return successResponse(response);

        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return errorResponse(e.getMessage(), 404);
            }
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Retire department (soft delete)
     * DELETE /api/departments/{id}?retireComments=xxx
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireDepartment(@PathParam("id") Long id) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Department ID is required", 400);
            }

            // Get retire comments from query parameter
            String retireComments = uriInfo.getQueryParameters().getFirst("retireComments");

            // Retire department
            DepartmentResponseDTO response = departmentService.retireDepartment(id, retireComments, user);
            return successResponse(response);

        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return errorResponse(e.getMessage(), 404);
            }
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Change department relationships (institution, site, super department)
     * PUT /api/departments/{id}/relationship
     */
    @PUT
    @Path("/{id}/relationship")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeDepartmentRelationships(@PathParam("id") Long id, String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Department ID is required", 400);
            }

            // Parse request body
            DepartmentRelationshipUpdateDTO request;
            try {
                request = gson.fromJson(requestBody, DepartmentRelationshipUpdateDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            // Set department ID from path parameter
            request.setDepartmentId(id);

            // Change department relationships
            DepartmentResponseDTO response = departmentService.changeDepartmentRelationships(request, user);
            return successResponse(response);

        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return errorResponse(e.getMessage(), 404);
            }
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get all configuration options for a department
     * GET /api/departments/{id}/config
     */
    @GET
    @Path("/{id}/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDepartmentConfig(@PathParam("id") Long id) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Department ID is required", 400);
            }

            // Get department config options
            List<DepartmentConfigDTO> results = configOptionService.getDepartmentConfigs(id);

            return successResponse(results);

        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return errorResponse(e.getMessage(), 404);
            }
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update department configuration option value
     * PUT /api/departments/{id}/config
     */
    @PUT
    @Path("/{id}/config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateDepartmentConfig(@PathParam("id") Long id, String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Department ID is required", 400);
            }

            // Parse request body
            DepartmentConfigUpdateDTO request;
            try {
                request = gson.fromJson(requestBody, DepartmentConfigUpdateDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            // Set department ID from path parameter
            request.setDepartmentId(id);

            // Validate required fields
            if (request.getConfigKey() == null || request.getConfigKey().trim().isEmpty() ||
                request.getConfigValue() == null) {
                return errorResponse("configKey and configValue are required", 400);
            }

            // Update department config option
            DepartmentConfigDTO response = configOptionService.updateDepartmentConfig(request, user);
            return successResponse(response);

        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return errorResponse(e.getMessage(), 404);
            }
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // Private helper methods

    /**
     * Validate API key and return associated user
     * Follows same pattern as PharmacyAdjustmentApi
     */
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

        // Treat null expiry date as expired
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) {
            return null;
        }

        return user;
    }

    /**
     * Create error response following PharmacyAdjustmentApi pattern
     */
    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(gson.toJson(response)).build();
    }

    /**
     * Create success response following PharmacyAdjustmentApi pattern
     */
    private Response successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.status(200).entity(gson.toJson(response)).build();
    }
}
