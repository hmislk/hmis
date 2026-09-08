package com.divudi.ws.investigation;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.investigation.CommonReportItemDTO;
import com.divudi.core.data.dto.investigation.CommonReportItemUpdateDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.investigation.ReportFormatApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Common report template endpoints — the counterpart of {@link InvestigationFormatApi},
 * which is scoped to one investigation and so cannot reach the rows shared by every
 * report of a format (patient-details block, signature block, footer).
 *
 * <p>These are the rows a hospital printing onto pre-printed stationery has to nudge,
 * and until now the only way to move them was direct SQL against the database
 * followed by a cache bounce (#23529). Writing through here goes via the facade, so
 * the EclipseLink L2 cache stays correct with no redeploy.</p>
 */
@Path("report-formats")
@RequestScoped
public class ReportFormatApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private ReportFormatApiService formatService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    // =========================================================================
    // Report formats
    // =========================================================================

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listFormats() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.listFormats());
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Common template items
    // =========================================================================

    @GET
    @Path("/{categoryId}/items")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listItems(@PathParam("categoryId") Long categoryId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.listItems(categoryId));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @GET
    @Path("/{categoryId}/items/{itemId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItem(@PathParam("categoryId") Long categoryId,
                            @PathParam("itemId") Long itemId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.getItem(categoryId, itemId));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @POST
    @Path("/{categoryId}/items")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createItem(@PathParam("categoryId") Long categoryId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            CommonReportItemUpdateDTO req = gson.fromJson(body, CommonReportItemUpdateDTO.class);
            CommonReportItemDTO dto = formatService.createItem(categoryId, req, user);
            return Response.status(201).entity(gson.toJson(successData(dto, 201))).build();
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/{categoryId}/items/{itemId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateItem(@PathParam("categoryId") Long categoryId,
                               @PathParam("itemId") Long itemId, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            CommonReportItemUpdateDTO req = gson.fromJson(body, CommonReportItemUpdateDTO.class);
            return successResponse(formatService.updateItem(categoryId, itemId, req, user));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    @DELETE
    @Path("/{categoryId}/items/{itemId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteItem(@PathParam("categoryId") Long categoryId,
                               @PathParam("itemId") Long itemId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            return successResponse(formatService.deleteItem(categoryId, itemId, user));
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
