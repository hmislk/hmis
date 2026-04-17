/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.pricing;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.pricing.CollectingCentreFeeCreateRequestDTO;
import com.divudi.core.data.dto.pricing.CollectingCentreFeeDTO;
import com.divudi.core.data.dto.service.ItemFeeUpdateRequestDTO;
import com.divudi.core.entity.WebUser;
import com.divudi.service.pricing.CollectingCentreFeesApiService;
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
 * REST API for Collecting Centre Item Fees management.
 *
 * Provides a collecting-centre-centric view of ItemFee records:
 * all fees where forInstitution is a CollectingCentre and forCategory is null.
 *
 * Complementary to ServiceApi (/api/services/{id}/fees) which is item-centric.
 *
 * All operations require a valid Finance API key header.
 */
@Path("pricing/collecting_centre_fees")
@RequestScoped
public class CollectingCentreFeesApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private CollectingCentreFeesApiService feesApiService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    // =========================================================================
    // GET /api/pricing/collecting_centre_fees?institutionId=X[&query=text&limit=50]
    // =========================================================================

    /**
     * List all active fees for a collecting centre.
     * Optionally filter by item name or code.
     *
     * @param institutionId required — ID of the collecting centre institution
     * @param query         optional — filter by item name or code (case-insensitive)
     * @param limit         optional — max results, default 100, max 500
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listFees() {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String institutionIdStr = uriInfo.getQueryParameters().getFirst("institutionId");
            String query = uriInfo.getQueryParameters().getFirst("query");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            if (institutionIdStr == null || institutionIdStr.trim().isEmpty()) {
                return errorResponse("institutionId parameter is required", 400);
            }

            Long institutionId;
            try {
                institutionId = Long.parseLong(institutionIdStr.trim());
            } catch (NumberFormatException e) {
                return errorResponse("Invalid institutionId format", 400);
            }

            int limit = 100;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    int parsed = Integer.parseInt(limitStr.trim());
                    limit = Math.min(Math.max(parsed, 1), 500);
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            List<CollectingCentreFeeDTO> fees = feesApiService.listFeesForCollectingCentre(
                    institutionId, query, limit);
            return successResponse(fees);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && msg.contains("not a collecting centre")) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    // =========================================================================
    // POST /api/pricing/collecting_centre_fees
    // =========================================================================

    /**
     * Add a new fee for a collecting centre and item.
     *
     * Body: {
     *   "collectingCentreId": 5,
     *   "itemId": 12,
     *   "name": "Collection Fee",
     *   "feeType": "OwnInstitution",
     *   "fee": 150.00,
     *   "ffee": 200.00,
     *   "departmentId": 3,
     *   "discountAllowed": false
     * }
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addFee(String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            CollectingCentreFeeCreateRequestDTO request;
            try {
                request = gson.fromJson(requestBody, CollectingCentreFeeCreateRequestDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            CollectingCentreFeeDTO result = feesApiService.addFee(request, user);
            return Response.status(201).entity(gson.toJson(successData(result))).build();

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    // =========================================================================
    // PUT /api/pricing/collecting_centre_fees/{feeId}
    // =========================================================================

    /**
     * Update an existing collecting centre fee.
     * Only non-null fields in the request body are applied.
     */
    @PUT
    @Path("/{feeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFee(@PathParam("feeId") Long feeId, String requestBody) {
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

            CollectingCentreFeeDTO result = feesApiService.updateFee(feeId, request, user);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && msg.contains("already retired")) {
                return errorResponse(msg, 409);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    // =========================================================================
    // DELETE /api/pricing/collecting_centre_fees/{feeId}
    // =========================================================================

    /**
     * Soft-retire a single collecting centre fee.
     *
     * @param feeId          required path param — the fee to retire
     * @param retireComments optional query param — reason for retirement
     */
    @DELETE
    @Path("/{feeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireFee(@PathParam("feeId") Long feeId) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String retireComments = uriInfo.getQueryParameters().getFirst("retireComments");
            CollectingCentreFeeDTO result = feesApiService.retireFee(feeId, retireComments, user);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && msg.contains("already retired")) {
                return errorResponse(msg, 409);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    // =========================================================================
    // DELETE /api/pricing/collecting_centre_fees?institutionId=X
    // =========================================================================

    /**
     * Soft-retire ALL active fees for the given collecting centre.
     * Returns a count of how many fees were retired.
     *
     * @param institutionId  required — ID of the collecting centre
     * @param retireComments optional — reason for retirement
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireAllFees() {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String institutionIdStr = uriInfo.getQueryParameters().getFirst("institutionId");
            String retireComments = uriInfo.getQueryParameters().getFirst("retireComments");

            if (institutionIdStr == null || institutionIdStr.trim().isEmpty()) {
                return errorResponse("institutionId parameter is required", 400);
            }

            Long institutionId;
            try {
                institutionId = Long.parseLong(institutionIdStr.trim());
            } catch (NumberFormatException e) {
                return errorResponse("Invalid institutionId format", 400);
            }

            int count = feesApiService.retireAllFeesForCollectingCentre(institutionId, retireComments, user);
            Map<String, Object> result = new HashMap<>();
            result.put("retiredCount", count);
            result.put("institutionId", institutionId);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && msg.contains("not a collecting centre")) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    // =========================================================================
    // POST /api/pricing/collecting_centre_fees/recalculate?institutionId=X
    // =========================================================================

    /**
     * Recalculate the total and totalForForeigner for all items that have
     * fees (active or retired) linked to the given collecting centre.
     * Call this after a bulk retirement to refresh item price totals.
     *
     * @param institutionId required — ID of the collecting centre
     */
    @POST
    @Path("/recalculate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response recalculate() {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String institutionIdStr = uriInfo.getQueryParameters().getFirst("institutionId");

            if (institutionIdStr == null || institutionIdStr.trim().isEmpty()) {
                return errorResponse("institutionId parameter is required", 400);
            }

            Long institutionId;
            try {
                institutionId = Long.parseLong(institutionIdStr.trim());
            } catch (NumberFormatException e) {
                return errorResponse("Invalid institutionId format", 400);
            }

            int count = feesApiService.recalculateItemTotalsForCollectingCentre(institutionId);
            Map<String, Object> result = new HashMap<>();
            result.put("itemsRecalculated", count);
            result.put("institutionId", institutionId);
            return successResponse(result);

        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("not found")) {
                return errorResponse(msg, 404);
            }
            if (msg != null && msg.contains("not a collecting centre")) {
                return errorResponse(msg, 400);
            }
            return errorResponse("An error occurred: " + (msg != null ? msg : "Unknown error"), 500);
        }
    }

    // =========================================================================
    // Helpers (copied verbatim from DepartmentApi)
    // =========================================================================

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        com.divudi.core.entity.ApiKey apiKey = apiKeyController.findApiKey(key);
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
