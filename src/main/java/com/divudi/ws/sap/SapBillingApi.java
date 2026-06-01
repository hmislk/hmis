/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.sap;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.sap.SapIntegrationException;
import com.divudi.service.sap.SapOutboundService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST API for SAP S/4HANA Cloud billing outbound integration.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /api/sap/billing/push/{billId}} — push a finalized HMIS bill to SAP FI</li>
 *   <li>{@code GET  /api/sap/billing/status/{billId}} — check whether a bill has been pushed</li>
 * </ul>
 *
 * <p>Auth: {@code Finance} header with a valid HMIS API key.
 */
@Path("sap/billing")
@RequestScoped
public class SapBillingApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private SapOutboundService sapOutboundService;

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * Push a finalized HMIS bill to SAP S/4HANA Cloud Finance as a journal entry.
     *
     * @param billId HMIS Bill ID
     * @return 200 with SAP document number on success, 4xx/5xx on failure
     */
    @POST
    @Path("/push/{billId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pushBill(@PathParam("billId") Long billId) {
        WebUser user = validateApiKey(requestContext.getHeader("Finance"));
        if (user == null) {
            return errorResponse("Not a valid key", 401);
        }
        if (billId == null || billId <= 0) {
            return errorResponse("Invalid bill ID", 400);
        }
        try {
            Map<String, Object> result = sapOutboundService.pushBillToSap(billId);
            return successResponse(result);
        } catch (SapIntegrationException e) {
            return errorResponse(e.getMessage(), 502);
        } catch (Exception e) {
            return errorResponse("Unexpected error: " + e.getMessage(), 500);
        }
    }

    /**
     * Check whether a bill has already been pushed to SAP.
     * Returns the stored push record if found, or a 404 if not yet pushed.
     */
    @GET
    @Path("/status/{billId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response pushStatus(@PathParam("billId") Long billId) {
        WebUser user = validateApiKey(requestContext.getHeader("Finance"));
        if (user == null) {
            return errorResponse("Not a valid key", 401);
        }
        if (billId == null || billId <= 0) {
            return errorResponse("Invalid bill ID", 400);
        }
        try {
            Map<String, Object> status = sapOutboundService.getPushStatus(billId);
            if (status == null) {
                return errorResponse("Bill " + billId + " has not been pushed to SAP", 404);
            }
            return successResponse(status);
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
