/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.inward;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.itemrequest.ItemRequestCreateRequestDTO;
import com.divudi.core.data.dto.itemrequest.ItemRequestResponseDTO;
import com.divudi.core.data.dto.itemrequest.ItemRequestSearchResultDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.inward.ItemRequestApiService;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for external Item/Service Requests against a patient's active BHT
 * (issue #21793). External systems submit requests (meals, stock items) which
 * are routed to a target department's approval queue. Approval/rejection
 * happens only via the in-app JSF approval page — external systems poll
 * {@code GET /api/itemrequests/{id}} for status.
 *
 * @author Claude AI Assistant
 */
@Path("itemrequests")
@RequestScoped
public class ItemRequestApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private ItemRequestApiService itemRequestApiService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public ItemRequestApi() {
    }

    /**
     * Submit a new item/service request against a patient's active BHT.
     * POST /api/itemrequests
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createItemRequest(String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            ItemRequestCreateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, ItemRequestCreateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            ItemRequestResponseDTO response = itemRequestApiService.submitRequest(request, user);
            return Response.status(201).entity(gson.toJson(successData(response))).build();

        } catch (Exception e) {
            // Note: ItemRequestApiService is a @Stateless EJB — thrown RuntimeExceptions
            // (including IllegalArgumentException) are wrapped by the EJB container and do
            // not reach this catch block as their original type, so validation failures are
            // classified by message content here, matching the existing convention in
            // ServiceApi (com.divudi.ws.service.ServiceApi).
            return errorResponse(e.getMessage(), classifyErrorCode(e.getMessage()));
        }
    }

    /**
     * Get an item/service request by ID (for external systems to poll status).
     * GET /api/itemrequests/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItemRequestById(@PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            ItemRequestResponseDTO response = itemRequestApiService.getRequestById(id);
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
     * List/search item/service requests.
     * GET /api/itemrequests?targetDepartmentId=&status=&fromDate=&toDate=&limit=
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listItemRequests() {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String targetDepartmentIdStr = uriInfo.getQueryParameters().getFirst("targetDepartmentId");
            String status = uriInfo.getQueryParameters().getFirst("status");
            String fromDateStr = uriInfo.getQueryParameters().getFirst("fromDate");
            String toDateStr = uriInfo.getQueryParameters().getFirst("toDate");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            Long targetDepartmentId = null;
            if (targetDepartmentIdStr != null && !targetDepartmentIdStr.trim().isEmpty()) {
                try {
                    targetDepartmentId = Long.parseLong(targetDepartmentIdStr.trim());
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid targetDepartmentId format", 400);
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date fromDate = null;
            Date toDate = null;
            try {
                if (fromDateStr != null && !fromDateStr.trim().isEmpty()) {
                    fromDate = sdf.parse(fromDateStr.trim());
                }
                if (toDateStr != null && !toDateStr.trim().isEmpty()) {
                    toDate = sdf.parse(toDateStr.trim());
                }
            } catch (ParseException e) {
                return errorResponse("Invalid date format, expected yyyy-MM-dd", 400);
            }

            Integer limit = null;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitStr.trim());
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            List<ItemRequestSearchResultDTO> results = itemRequestApiService.listRequests(
                    targetDepartmentId, status, fromDate, toDate, limit);
            return successResponse(results);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Cancel a pending item/service request.
     * PUT /api/itemrequests/{id}/cancel
     * Body: {"reason": "..."}
     */
    @PUT
    @Path("/{id}/cancel")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelItemRequest(@PathParam("id") Long id, String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String reason = null;
            if (requestBody != null && !requestBody.trim().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, String> body = gson.fromJson(requestBody, Map.class);
                    if (body != null) {
                        reason = body.get("reason");
                    }
                } catch (JsonSyntaxException e) {
                    return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
                }
            }

            ItemRequestResponseDTO response = itemRequestApiService.cancelRequest(id, reason, user);
            return successResponse(response);

        } catch (Exception e) {
            // See createItemRequest() note: exception type does not survive the EJB
            // boundary, so classify by message content instead.
            return errorResponse(e.getMessage(), classifyErrorCode(e.getMessage()));
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Classifies an exception message from {@link ItemRequestApiService} into
     * an HTTP status code. The service is a {@code @Stateless} EJB, so thrown
     * RuntimeExceptions (IllegalArgumentException, IllegalStateException) are
     * wrapped by the container and never reach the REST layer as their
     * original type — only the message survives, hence matching on content
     * rather than exception type (same convention as {@code ServiceApi}).
     */
    private int classifyErrorCode(String message) {
        if (message == null) {
            return 500;
        }
        if (message.contains("not found")) {
            return 404;
        }
        if (message.contains("already") && (message.contains("cannot") || message.contains("cannot approve")
                || message.contains("cannot reject") || message.contains("cannot cancel"))) {
            return 409;
        }
        if (message.contains("required") || message.contains("must be") || message.contains("Invalid")
                || message.contains("No active BHT") || message.contains("Insufficient stock")
                || message.contains("discharged") || message.contains("settled")) {
            return 400;
        }
        return 500;
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
