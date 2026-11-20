package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.common.MaintenanceBannerCache;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.MaintenanceWindow;
import com.divudi.core.entity.WebUser;
import com.divudi.service.MaintenanceWindowException;
import com.divudi.service.MaintenanceWindowService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST API for maintenance windows shown as a banner (#25838).
 * GET needs any valid Finance API key. POST and DELETE also need the key's
 * owner to hold the ManageMaintenanceWindows privilege.
 */
@Path("maintenance_windows")
@RequestScoped
public class MaintenanceWindowApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;
    @Inject
    private MaintenanceBannerCache maintenanceBannerCache;

    @EJB
    private MaintenanceWindowService maintenanceWindowService;

    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    /** Request body for POST. */
    static class CreateRequest {

        String title;
        String message;
        Date startAt;
        Date endAt;
        Date showFromAt;
        List<Long> institutionIds;
        List<Long> departmentIds;
        String reference;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            if (validateApiKey(requestContext.getHeader("Finance")) == null) {
                return errorResponse("Not a valid key", 401);
            }
            List<Map<String, Object>> out = new ArrayList<>();
            for (MaintenanceWindow w : maintenanceWindowService.listUpcomingAndActive(new Date())) {
                out.add(toMap(w));
            }
            return successResponse(out);
        } catch (Exception e) {
            return errorResponse("An error occurred", 500);
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
            if (!maintenanceWindowService.userMayManage(user)) {
                return errorResponse("The key owner is not allowed to manage maintenance windows", 403);
            }
            CreateRequest r;
            try {
                r = gson.fromJson(requestBody, CreateRequest.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON body (dates use yyyy-MM-dd HH:mm:ss)", 400);
            }
            if (r == null) {
                return errorResponse("Request body is required", 400);
            }
            MaintenanceWindow w = new MaintenanceWindow();
            w.setTitle(r.title);
            w.setMessage(r.message);
            w.setStartAt(r.startAt);
            w.setEndAt(r.endAt);
            w.setShowFromAt(r.showFromAt);
            w.setExternalReference(r.reference);
            MaintenanceWindow saved = maintenanceWindowService.create(w, r.institutionIds, r.departmentIds, user, "api");
            maintenanceBannerCache.invalidate();
            return successResponse(toMap(saved));
        } catch (MaintenanceWindowException e) {
            return errorResponse(e.getMessage(), e.getStatus());
        } catch (Exception e) {
            return errorResponse("An error occurred", 500);
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancel(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            if (!maintenanceWindowService.userMayManage(user)) {
                return errorResponse("The key owner is not allowed to manage maintenance windows", 403);
            }
            MaintenanceWindow w = maintenanceWindowService.cancel(id, user);
            maintenanceBannerCache.invalidate();
            return successResponse(toMap(w));
        } catch (MaintenanceWindowException e) {
            return errorResponse(e.getMessage(), e.getStatus());
        } catch (Exception e) {
            return errorResponse("An error occurred", 500);
        }
    }

    private Map<String, Object> toMap(MaintenanceWindow w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", w.getId());
        m.put("title", w.getTitle());
        m.put("message", w.getMessage());
        m.put("startAt", w.getStartAt());
        m.put("endAt", w.getEndAt());
        m.put("showFromAt", w.getShowFromAt());
        List<Long> institutionIds = new ArrayList<>();
        for (Institution i : w.getInstitutions()) {
            institutionIds.add(i.getId());
        }
        List<Long> departmentIds = new ArrayList<>();
        for (Department d : w.getDepartments()) {
            departmentIds.add(d.getId());
        }
        m.put("institutionIds", institutionIds);
        m.put("departmentIds", departmentIds);
        m.put("source", w.getSource());
        m.put("reference", w.getExternalReference());
        m.put("retired", w.isRetired());
        return m;
    }

    /**
     * Validate API key and return associated user (same rules as DepartmentApi).
     */
    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) {
            return null;
        }
        if (apiKey.isRetired()) {
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
        // Treat null expiry date as expired
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
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.status(200).entity(gson.toJson(response)).build();
    }
}
