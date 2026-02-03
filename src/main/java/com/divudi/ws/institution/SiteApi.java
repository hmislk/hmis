/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.institution;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.search.SiteDTO;
import com.divudi.core.data.dto.site.SiteCreateRequestDTO;
import com.divudi.core.data.dto.site.SiteResponseDTO;
import com.divudi.core.data.dto.site.SiteUpdateRequestDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.institution.SiteApiService;
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
 * REST API for Site Management
 * Site is an Institution with institutionType = InstitutionType.Site
 * Provides endpoints for creating, updating, retiring, and searching sites
 * Follows patterns from PharmacySearchApi and PharmacyAdjustmentApi
 *
 * @author Buddhika
 */
@Path("sites")
@RequestScoped
public class SiteApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private SiteApiService siteService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public SiteApi() {
    }

    /**
     * Search sites by name or code
     * GET /api/sites/search?query=Main&limit=20
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchSites() {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Get query parameters
            String query = uriInfo.getQueryParameters().getFirst("query");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            if (query == null || query.trim().isEmpty()) {
                return errorResponse("Query parameter is required", 400);
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
            List<SiteDTO> results = siteService.searchSites(query.trim(), limit);

            return successResponse(results);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Get site by ID
     * GET /api/sites/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSiteById(@PathParam("id") Long id) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Site ID is required", 400);
            }

            // Get site
            SiteResponseDTO result = siteService.findSiteById(id);

            return successResponse(result);

        } catch (Exception e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("not a site")) {
                return errorResponse(e.getMessage(), 404);
            }
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create new site
     * POST /api/sites
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSite(String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Parse request body
            SiteCreateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, SiteCreateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            // Create site
            SiteResponseDTO response = siteService.createSite(request, user);
            return successResponse(response);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update existing site
     * PUT /api/sites/{id}
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSite(@PathParam("id") Long id, String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Site ID is required", 400);
            }

            // Parse request body
            SiteUpdateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, SiteUpdateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            // Update site
            SiteResponseDTO response = siteService.updateSite(id, request, user);
            return successResponse(response);

        } catch (Exception e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("not a site")) {
                return errorResponse(e.getMessage(), 404);
            }
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Retire site (soft delete)
     * DELETE /api/sites/{id}?retireComments=xxx
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireSite(@PathParam("id") Long id) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (id == null) {
                return errorResponse("Site ID is required", 400);
            }

            // Get retire comments from query parameter
            String retireComments = uriInfo.getQueryParameters().getFirst("retireComments");

            // Retire site
            SiteResponseDTO response = siteService.retireSite(id, retireComments, user);
            return successResponse(response);

        } catch (Exception e) {
            if (e.getMessage().contains("not found") || e.getMessage().contains("not a site")) {
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
