/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.institution;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.data.dto.institution.InstitutionCreateRequestDTO;
import com.divudi.core.data.dto.institution.InstitutionRelationshipUpdateDTO;
import com.divudi.core.data.dto.institution.InstitutionResponseDTO;
import com.divudi.core.data.dto.institution.InstitutionUpdateRequestDTO;
import com.divudi.core.data.dto.search.InstitutionDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.institution.InstitutionApiService;
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
 * REST API for Institution Management
 * Provides endpoints for creating, updating, retiring, and searching institutions
 * Follows patterns from PharmacySearchApi and PharmacyAdjustmentApi
 *
 * @author Buddhika
 */
@Path("institutions")
@RequestScoped
public class InstitutionApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private InstitutionApiService institutionService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public InstitutionApi() {
    }

    /**
     * Search institutions by name or code with optional type filtering
     * GET /api/institutions/search?query=Hospital&type=Hospital&limit=20
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchInstitutions() {
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
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            if (query == null || query.trim().isEmpty()) {
                return errorResponse("Query parameter is required", 400);
            }

            // Parse institution type if provided
            InstitutionType type = null;
            if (typeStr != null && !typeStr.trim().isEmpty()) {
                try {
                    type = InstitutionType.valueOf(typeStr.trim());
                } catch (IllegalArgumentException e) {
                    return errorResponse("Invalid institution type: " + typeStr, 400);
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
            List<InstitutionDTO> results = institutionService.searchInstitutions(query.trim(), type, limit);

            return successResponse(results);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get institution by ID
     * GET /api/institutions/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstitutionById(@PathParam("id") Long id) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Institution ID is required", 400);
            }

            // Get institution
            InstitutionResponseDTO result = institutionService.findInstitutionById(id);

            return successResponse(result);

        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return errorResponse(e.getMessage(), 404);
            }
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create new institution
     * POST /api/institutions
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createInstitution(String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Parse request body
            InstitutionCreateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, InstitutionCreateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            // Create institution
            InstitutionResponseDTO response = institutionService.createInstitution(request, user);
            return successResponse(response);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update existing institution
     * PUT /api/institutions/{id}
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateInstitution(@PathParam("id") Long id, String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Institution ID is required", 400);
            }

            // Parse request body
            InstitutionUpdateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, InstitutionUpdateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            // Update institution
            InstitutionResponseDTO response = institutionService.updateInstitution(id, request, user);
            return successResponse(response);

        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return errorResponse(e.getMessage(), 404);
            }
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Retire institution (soft delete)
     * DELETE /api/institutions/{id}?retireComments=xxx
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireInstitution(@PathParam("id") Long id) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Institution ID is required", 400);
            }

            // Get retire comments from query parameter
            String retireComments = uriInfo.getQueryParameters().getFirst("retireComments");

            // Retire institution
            InstitutionResponseDTO response = institutionService.retireInstitution(id, retireComments, user);
            return successResponse(response);

        } catch (Exception e) {
            if (e.getMessage().contains("not found")) {
                return errorResponse(e.getMessage(), 404);
            }
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Change parent institution relationship
     * PUT /api/institutions/{id}/relationship
     */
    @PUT
    @Path("/{id}/relationship")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeParentInstitution(@PathParam("id") Long id, String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Institution ID is required", 400);
            }

            // Parse request body
            InstitutionRelationshipUpdateDTO request;
            try {
                request = gson.fromJson(requestBody, InstitutionRelationshipUpdateDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            // Set institution ID from path parameter
            request.setInstitutionId(id);

            // Change parent institution
            InstitutionResponseDTO response = institutionService.changeParentInstitution(request, user);
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
