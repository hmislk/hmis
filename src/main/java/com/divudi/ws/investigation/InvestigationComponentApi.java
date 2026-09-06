package com.divudi.ws.investigation;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.investigation.InvestigationComponentDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.investigation.InvestigationComponentApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Path("investigations/{investigationId}/components")
@RequestScoped
public class InvestigationComponentApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private InvestigationComponentApiService componentService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listComponents(@PathParam("investigationId") Long investigationId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(componentService.listComponents(investigationId));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createComponent(@PathParam("investigationId") Long investigationId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            InvestigationComponentDTO req = gson.fromJson(body, InvestigationComponentDTO.class);
            InvestigationComponentDTO dto = componentService.createComponent(investigationId, req, user);
            return Response.status(201).entity(gson.toJson(successData(dto, 201))).build();
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/{componentId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateComponent(@PathParam("investigationId") Long investigationId,
                                     @PathParam("componentId") Long componentId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            InvestigationComponentDTO req = gson.fromJson(body, InvestigationComponentDTO.class);
            return successResponse(componentService.updateComponent(investigationId, componentId, req, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @DELETE
    @Path("/{componentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteComponent(@PathParam("investigationId") Long investigationId,
                                     @PathParam("componentId") Long componentId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(componentService.deleteComponent(investigationId, componentId, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
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
        return successData(data, 200);
    }

    private Map<String, Object> successData(Object data, int statusCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "success");
        map.put("code", statusCode);
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
