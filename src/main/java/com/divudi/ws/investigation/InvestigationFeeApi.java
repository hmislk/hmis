/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.investigation;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.service.ItemFeeCreateRequestDTO;
import com.divudi.core.data.dto.service.ItemFeeUpdateRequestDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.investigation.InvestigationFeeApiService;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API for Investigation pricing/fees, mirroring {@code ServiceApi}'s
 * {@code /fees} sub-resource. See issue #22362.
 *
 * @author Buddhika
 */
@Path("investigations/{investigationId}/fees")
@RequestScoped
public class InvestigationFeeApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private InvestigationFeeApiService investigationFeeApiService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * List all non-retired fees for an investigation.
     * GET /api/investigations/{investigationId}/fees
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listFees(@PathParam("investigationId") Long investigationId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(investigationFeeApiService.listFees(investigationId));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    /**
     * Add a fee to an investigation.
     * POST /api/investigations/{investigationId}/fees
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addFee(@PathParam("investigationId") Long investigationId, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);

            ItemFeeCreateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, ItemFeeCreateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            return Response.status(201)
                    .entity(gson.toJson(successData(investigationFeeApiService.addFee(investigationId, request, user), 201)))
                    .build();
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
     * PUT /api/investigations/{investigationId}/fees/{feeId}
     */
    @PUT
    @Path("/{feeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFee(@PathParam("investigationId") Long investigationId,
                               @PathParam("feeId") Long feeId, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);

            ItemFeeUpdateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, ItemFeeUpdateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            return successResponse(investigationFeeApiService.updateFee(investigationId, feeId, request, user));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    /**
     * Remove a fee (soft-delete/retire).
     * DELETE /api/investigations/{investigationId}/fees/{feeId}
     */
    @DELETE
    @Path("/{feeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeFee(@PathParam("investigationId") Long investigationId, @PathParam("feeId") Long feeId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);

            return successResponse(investigationFeeApiService.removeFee(investigationId, feeId, user));
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
