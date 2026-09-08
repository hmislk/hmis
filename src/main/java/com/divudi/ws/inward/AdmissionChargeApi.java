/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.inward;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.admissioncharge.AdmissionChargeItemCreateRequestDTO;
import com.divudi.core.data.dto.admissioncharge.AdmissionChargeItemDTO;
import com.divudi.core.data.dto.admissioncharge.AdmissionChargeItemPageDTO;
import com.divudi.core.data.dto.admissioncharge.AdmissionChargeItemUpdateRequestDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.inward.AdmissionChargeApiService;
import com.divudi.service.inward.AdmissionChargeValidationException;
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
import java.util.Map;

/**
 * REST API for Admission Charge Item management (issue #23594).
 *
 * <p>Manages {@code AdmissionChargeItem} rows - the routine charges billed
 * automatically on every matching admission by
 * {@code AdmissionChargeApplicationBean}. See
 * {@code developer_docs/api/using-apis/API_ADMISSION_CHARGES.md} for the
 * two-dimension resolution rule, the configuration trap, and the
 * double-charge trap with {@code AdmissionType.admissionFee}.
 *
 * <p>Authentication: Finance header with a valid API key.
 *
 * @author Buddhika
 */
@Path("admission-charges")
@RequestScoped
public class AdmissionChargeApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private AdmissionChargeApiService admissionChargeApiService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public AdmissionChargeApi() {
    }

    // =========================================================================
    // Search / Get
    // =========================================================================

    /**
     * Search admission charge items.
     * GET /api/admission-charges/search?itemId=&admissionTypeId=&paymentMethod=&includeRetired=&limit=&offset=
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

            Long itemId;
            Long admissionTypeId;
            try {
                itemId = parseLongParam("itemId");
                admissionTypeId = parseLongParam("admissionTypeId");
            } catch (NumberFormatException e) {
                return errorResponse(e.getMessage(), 400);
            }
            String paymentMethod = uriInfo.getQueryParameters().getFirst("paymentMethod");

            Boolean includeRetired = parseBooleanParam("includeRetired");
            if (includeRetired == null) {
                return errorResponse("Invalid includeRetired value. Use true or false.", 400);
            }

            int limit = 30;
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    limit = Math.min(Math.max(Integer.parseInt(limitStr.trim()), 1), 100);
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            int offset = 0;
            String offsetStr = uriInfo.getQueryParameters().getFirst("offset");
            if (offsetStr != null && !offsetStr.trim().isEmpty()) {
                try {
                    offset = Math.max(Integer.parseInt(offsetStr.trim()), 0);
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid offset format", 400);
                }
            }

            AdmissionChargeItemPageDTO result = admissionChargeApiService.search(
                    itemId, admissionTypeId, paymentMethod, includeRetired, limit, offset);
            return successResponse(result);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Get an admission charge item by ID.
     * GET /api/admission-charges/{id}?includeRetired=
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Boolean includeRetired = parseBooleanParam("includeRetired");
            if (includeRetired == null) {
                return errorResponse("Invalid includeRetired value. Use true or false.", 400);
            }

            AdmissionChargeItemDTO result = admissionChargeApiService.findById(id, includeRetired);
            return successResponse(result);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    // =========================================================================
    // Create / Update
    // =========================================================================

    /**
     * Create a new admission charge item.
     * POST /api/admission-charges
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            AdmissionChargeItemCreateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, AdmissionChargeItemCreateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            AdmissionChargeItemDTO response = admissionChargeApiService.create(request, user);
            return successResponse(201, response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Update an existing admission charge item.
     * PUT /api/admission-charges/{id}
     */
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

            AdmissionChargeItemUpdateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, AdmissionChargeItemUpdateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            AdmissionChargeItemDTO response = admissionChargeApiService.update(id, request, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    // =========================================================================
    // Retire / Restore
    // =========================================================================

    /**
     * Retire an admission charge item (soft delete).
     * DELETE /api/admission-charges/{id}?retireComments=reason
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retire(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String retireComments = uriInfo.getQueryParameters().getFirst("retireComments");
            AdmissionChargeItemDTO response = admissionChargeApiService.retire(id, retireComments, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    /**
     * Un-retire an admission charge item, undoing DELETE /api/admission-charges/{id}.
     * PATCH /api/admission-charges/{id}/restore
     */
    @PATCH
    @Path("/{id}/restore")
    @Produces(MediaType.APPLICATION_JSON)
    public Response restore(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            AdmissionChargeItemDTO response = admissionChargeApiService.restore(id, user);
            return successResponse(response);

        } catch (Exception e) {
            return mapError(e);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Maps a service-layer exception onto an HTTP status. A broken validation
     * rule is always the caller's payload, never a server fault - checked by
     * type so the status does not depend on how the message happens to be
     * worded.
     */
    private Response mapError(Exception e) {
        String msg = e.getMessage();
        if (e instanceof AdmissionChargeValidationException) {
            return errorResponse(msg, 400);
        }
        if (msg == null) {
            return errorResponse("An error occurred: Unknown error", 500);
        }
        if (msg.contains("not found")) {
            return errorResponse(msg, 404);
        }
        if (msg.contains("is retired")) {
            return errorResponse(msg + " — use ?includeRetired=true to read it, "
                    + "or PATCH the restore endpoint to bring it back.", 404);
        }
        if (msg.contains("is not retired")) {
            return errorResponse(msg, 409);
        }
        return errorResponse("An error occurred: " + msg, 500);
    }

    /**
     * @return the flag, or null when the caller sent something that is neither true nor
     *         false - silently treating a typo as false would hide retired rows the
     *         caller explicitly asked for
     */
    private Boolean parseBooleanParam(String name) {
        String raw = uriInfo.getQueryParameters().getFirst(name);
        if (raw == null || raw.trim().isEmpty()) {
            return Boolean.FALSE;
        }
        String v = raw.trim().toLowerCase();
        if ("true".equals(v)) {
            return Boolean.TRUE;
        }
        if ("false".equals(v)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private Long parseLongParam(String name) {
        String raw = uriInfo.getQueryParameters().getFirst(name);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid " + name + " format");
        }
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) {
            return null;
        }
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) {
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
        return successResponse(200, data);
    }

    private Response successResponse(int status, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", status);
        response.put("data", data);
        return Response.status(status).entity(gson.toJson(response)).build();
    }
}
