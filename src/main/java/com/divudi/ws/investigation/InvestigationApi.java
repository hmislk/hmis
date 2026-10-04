package com.divudi.ws.investigation;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.investigation.InvestigationCreateRequestDTO;
import com.divudi.core.data.dto.investigation.InvestigationResponseDTO;
import com.divudi.core.data.dto.investigation.InvestigationSearchResultDTO;
import com.divudi.core.data.dto.investigation.InvestigationUpdateRequestDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.investigation.InvestigationApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.Date;
import com.divudi.service.inward.DiscountSetupService;
import javax.ejb.EJB;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("investigations")
@RequestScoped
public class InvestigationApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private InvestigationApiService investigationApiService;

    @EJB
    private DiscountSetupService discountSetupService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * Search investigations by name, code or print name, and active status.
     * GET /api/investigations/search?query=blood&limit=20
     *
     * Results are ordered by name then id. To list the whole master, page with
     * {@code offset}: GET /api/investigations/search?limit=100&offset=200&includeTotal=true.
     * {@code data} stays a plain array; {@code includeTotal=true} adds {@code totalCount},
     * {@code offset} and {@code limit} beside it. Without it the response is unchanged.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String query = uriInfo.getQueryParameters().getFirst("query");
            String inactiveStr = uriInfo.getQueryParameters().getFirst("inactive");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");
            String offsetStr = uriInfo.getQueryParameters().getFirst("offset");
            String includeTotalStr = uriInfo.getQueryParameters().getFirst("includeTotal");

            Boolean inactive = null;
            if (inactiveStr != null && !inactiveStr.trim().isEmpty()) {
                inactive = Boolean.parseBoolean(inactiveStr.trim());
            }

            int limit = 20;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                limit = Math.min(Math.max(Integer.parseInt(limitStr.trim()), 1), 100);
            }

            int offset = 0;
            if (offsetStr != null && !offsetStr.trim().isEmpty()) {
                try {
                    offset = Math.max(Integer.parseInt(offsetStr.trim()), 0);
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid offset format", 400);
                }
            }

            boolean includeTotal = false;
            if (includeTotalStr != null && !includeTotalStr.trim().isEmpty()) {
                String raw = includeTotalStr.trim().toLowerCase();
                if (!"true".equals(raw) && !"false".equals(raw)) {
                    return errorResponse("Invalid includeTotal value. Use true or false.", 400);
                }
                includeTotal = Boolean.parseBoolean(raw);
            }

            List<InvestigationSearchResultDTO> results =
                    investigationApiService.search(query, inactive, limit, offset);
            if (!includeTotal) {
                return successResponse(results);
            }

            // limit is echoed because it is clamped to 100: a caller that asked for more
            // must advance by what it actually got, not by what it asked for.
            Map<String, Object> body = successData(results);
            body.put("totalCount", investigationApiService.count(query, inactive));
            body.put("offset", offset);
            body.put("limit", limit);
            return Response.ok(gson.toJson(body)).build();
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            return successResponse(investigationApiService.findById(id));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            InvestigationCreateRequestDTO request = gson.fromJson(requestBody, InvestigationCreateRequestDTO.class);
            InvestigationResponseDTO dto = investigationApiService.create(request, user);
            return Response.status(201).entity(gson.toJson(successData(dto))).build();

        } catch (IllegalStateException ex) {
            try {
                Long existingId = Long.valueOf(ex.getMessage());
                InvestigationResponseDTO existing = investigationApiService.findById(existingId);
                Map<String, Object> map = successData(existing);
                map.put("status", "already_exists");
                map.put("id", existingId);
                return Response.status(409).entity(gson.toJson(map)).build();
            } catch (Exception e) {
                return errorResponse("An error occurred: " + e.getMessage(), 500);
            }
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            InvestigationUpdateRequestDTO request = gson.fromJson(requestBody, InvestigationUpdateRequestDTO.class);
            return successResponse(investigationApiService.update(id, request, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PATCH
    @Path("/{id}/activate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response activate(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            return successResponse(investigationApiService.setActive(id, false, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PATCH
    @Path("/{id}/deactivate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deactivate(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            return successResponse(investigationApiService.setActive(id, true, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Copy the deprecated investigationCategory into category for active
     * investigations whose category is blank (issue #24038). Dry run unless
     * {@code ?apply=true}: reports how many would change.
     * POST /api/investigations/copy-legacy-category[?apply=true]
     */
    @POST
    @Path("/copy-legacy-category")
    @Produces(MediaType.APPLICATION_JSON)
    public Response copyLegacyCategory() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            boolean apply = "true".equalsIgnoreCase(uriInfo.getQueryParameters().getFirst("apply"));
            Map<String, Object> data = new HashMap<>();
            data.put("apply", apply);
            data.put("investigationsWithOnlyLegacyCategory", discountSetupService.countInvestigationsUsingLegacyCategory());
            if (apply) {
                data.put("updated", discountSetupService.copyLegacyInvestigationCategories(user));
            }
            return successResponse(data);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null || apiKey.isRetired()) {
            return null;
        }

        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) {
            return null;
        }

        Date expiry = apiKey.getDateOfExpiary();
        if (expiry == null || expiry.before(new Date())) {
            return null;
        }

        return user;
    }

    private Response successResponse(Object data) {
        return Response.ok(gson.toJson(successData(data))).build();
    }

    private Response errorResponse(String message, int statusCode) {
        return Response.status(statusCode).entity(gson.toJson(errorData(message, statusCode))).build();
    }

    private Map<String, Object> successData(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "success");
        map.put("code", 200);
        map.put("timestamp", new Date());
        map.put("data", data);
        return map;
    }

    private Map<String, Object> errorData(String message, int statusCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "error");
        map.put("code", statusCode);
        map.put("message", message);
        map.put("timestamp", new Date());
        map.put("data", Collections.emptyMap());
        return map;
    }
}
