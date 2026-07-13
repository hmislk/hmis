package com.divudi.ws.investigation;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.investigation.InvestigationCreateRequestDTO;
import com.divudi.core.data.dto.investigation.InvestigationResponseDTO;
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
import java.util.HashMap;
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

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

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

            Boolean inactive = null;
            if (inactiveStr != null && !inactiveStr.trim().isEmpty()) {
                inactive = Boolean.parseBoolean(inactiveStr.trim());
            }

            int limit = 20;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                limit = Math.min(Math.max(Integer.parseInt(limitStr.trim()), 1), 100);
            }

            return successResponse(investigationApiService.search(query, inactive, limit));
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
