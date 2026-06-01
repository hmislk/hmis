/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.sap;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.sap.SapInventorySyncResultDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.sap.SapIntegrationException;
import com.divudi.service.sap.SapInventorySyncService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST API for SAP S/4HANA Cloud inventory sync.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /api/sap/inventory/sync} — manually trigger a SAP MM goods-receipt sync</li>
 * </ul>
 *
 * <p>Query parameters (all optional):
 * <ul>
 *   <li>{@code fromDate} — start date yyyy-MM-dd (default: last sync timestamp or N days ago)</li>
 *   <li>{@code toDate}   — end date yyyy-MM-dd (default: today)</li>
 * </ul>
 *
 * <p>Auth: {@code Finance} header with a valid HMIS API key.
 */
@Path("sap/inventory")
@RequestScoped
public class SapInventoryApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private SapInventorySyncService sapInventorySyncService;

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * Manually trigger a SAP → HMIS inventory sync for a date range.
     * Fetches SAP MM goods-receipt material documents and matches them to HMIS items.
     *
     * @param fromDate optional start date (yyyy-MM-dd)
     * @param toDate   optional end date (yyyy-MM-dd)
     */
    @GET
    @Path("/sync")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sync(
            @QueryParam("fromDate") String fromDate,
            @QueryParam("toDate") String toDate) {

        WebUser user = validateApiKey(requestContext.getHeader("Finance"));
        if (user == null) {
            return errorResponse("Not a valid key", 401);
        }
        try {
            SapInventorySyncResultDTO result = sapInventorySyncService.sync(fromDate, toDate);
            return successResponse(result);
        } catch (SapIntegrationException e) {
            return errorResponse(e.getMessage(), 502);
        } catch (Exception e) {
            return errorResponse("Unexpected error: " + e.getMessage(), 500);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) return null;
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) return null;
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) return null;
        return user;
    }

    private Response errorResponse(String message, int code) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "error");
        body.put("code", code);
        body.put("message", message);
        return Response.status(code).entity(GSON.toJson(body)).build();
    }

    private Response successResponse(Object data) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "success");
        body.put("code", 200);
        body.put("data", data);
        return Response.status(200).entity(GSON.toJson(body)).build();
    }
}
