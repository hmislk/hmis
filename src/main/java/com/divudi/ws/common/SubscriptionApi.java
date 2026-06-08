/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.ApiKeyType;
import com.divudi.core.data.TriggerType;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.TriggerSubscription;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.TriggerSubscriptionFacade;
import com.divudi.core.facade.WebUserFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;

/**
 * REST API for managing notification trigger subscriptions
 * ({@link TriggerSubscription}).
 *
 * Mirrors the JSF page {@code admin/users/user_subscription.xhtml}: a
 * subscription links a {@link WebUser} to a {@link TriggerType} for a given
 * {@link Department}, or — when the department is {@code null} —
 * <b>application-wide</b> (matches every department across the whole
 * application; one HMIS application instance can host multiple institutions).
 *
 * All operations require a valid Finance API key header.
 */
@Path("subscriptions")
@RequestScoped
public class SubscriptionApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private TriggerSubscriptionFacade triggerSubscriptionFacade;

    @EJB
    private WebUserFacade webUserFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    // -------------------------------------------------------------------------
    // GET /api/subscriptions/trigger-types
    // -------------------------------------------------------------------------

    @GET
    @Path("/trigger-types")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listTriggerTypes() {
        try {
            if (validateApiKey(requestContext.getHeader("Finance")) == null) {
                return errorResponse("Not a valid key", 401);
            }
            List<Map<String, Object>> result = new ArrayList<>();
            for (TriggerType tt : TriggerType.getAlphabeticallySortedValues()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", tt.name());
                m.put("label", tt.getLabel());
                m.put("medium", tt.getMedium() != null ? tt.getMedium().name() : null);
                m.put("parent", tt.getParent() != null ? tt.getParent().name() : null);
                result.add(m);
            }
            return successResponse(result);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/subscriptions[?triggerType=X&userId=N&departmentId=N&applicationWide=true]
    // -------------------------------------------------------------------------

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            if (validateApiKey(requestContext.getHeader("Finance")) == null) {
                return errorResponse("Not a valid key", 401);
            }

            Map<String, Object> params = new HashMap<>();
            StringBuilder jpql = new StringBuilder(
                    "select i from TriggerSubscription i where i.retired=false and i.webUser is not null");

            String triggerTypeStr = param("triggerType");
            if (triggerTypeStr != null && !triggerTypeStr.trim().isEmpty()) {
                TriggerType tt = resolveTriggerType(triggerTypeStr);
                if (tt == null) {
                    return errorResponse("Unknown triggerType: " + triggerTypeStr, 400);
                }
                jpql.append(" and i.triggerType=:tt");
                params.put("tt", tt);
            }

            String userIdStr = param("userId");
            if (userIdStr != null && !userIdStr.trim().isEmpty()) {
                Long userId = parseLong(userIdStr);
                if (userId == null) {
                    return errorResponse("Invalid userId", 400);
                }
                WebUser u = webUserFacade.find(userId);
                if (u == null) {
                    return errorResponse("User not found with id: " + userId, 404);
                }
                jpql.append(" and i.webUser=:u");
                params.put("u", u);
            }

            boolean applicationWide = "true".equalsIgnoreCase(param("applicationWide"));
            String departmentIdStr = param("departmentId");
            if (applicationWide) {
                jpql.append(" and i.department is null");
            } else if (departmentIdStr != null && !departmentIdStr.trim().isEmpty()) {
                Long deptId = parseLong(departmentIdStr);
                if (deptId == null) {
                    return errorResponse("Invalid departmentId", 400);
                }
                Department dept = departmentFacade.find(deptId);
                if (dept == null) {
                    return errorResponse("Department not found with id: " + deptId, 404);
                }
                jpql.append(" and i.department=:dep");
                params.put("dep", dept);
            }

            jpql.append(" order by i.orderNumber");

            List<TriggerSubscription> subs = triggerSubscriptionFacade.findByJpql(jpql.toString(), params);
            List<Map<String, Object>> result = new ArrayList<>();
            for (TriggerSubscription s : subs) {
                result.add(toMap(s));
            }
            return successResponse(result);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/subscriptions
    // body: {userId, triggerType, departmentId?, applicationWide?}
    // -------------------------------------------------------------------------

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(String requestBody) {
        try {
            WebUser apiUser = validateApiKey(requestContext.getHeader("Finance"));
            if (apiUser == null) {
                return errorResponse("Not a valid key", 401);
            }

            Map<String, Object> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON: " + e.getMessage(), 400);
            }
            if (body == null) {
                return errorResponse("Request body is required", 400);
            }

            // triggerType (required)
            String triggerTypeStr = asString(body.get("triggerType"));
            if (triggerTypeStr == null || triggerTypeStr.trim().isEmpty()) {
                return errorResponse("triggerType is required", 400);
            }
            TriggerType tt = resolveTriggerType(triggerTypeStr);
            if (tt == null) {
                return errorResponse("Unknown triggerType: " + triggerTypeStr, 400);
            }

            // userId (required)
            Long userId = asLong(body.get("userId"));
            if (userId == null) {
                return errorResponse("userId is required", 400);
            }
            WebUser user = webUserFacade.find(userId);
            if (user == null || user.isRetired()) {
                return errorResponse("User not found with id: " + userId, 404);
            }

            // exactly one of departmentId / applicationWide
            boolean applicationWide = asBoolean(body.get("applicationWide"));
            Long departmentId = asLong(body.get("departmentId"));
            if (applicationWide && departmentId != null) {
                return errorResponse("Provide either departmentId or applicationWide, not both", 400);
            }
            if (!applicationWide && departmentId == null) {
                return errorResponse("Provide either departmentId or applicationWide:true", 400);
            }

            Department department = null;
            if (!applicationWide) {
                department = departmentFacade.find(departmentId);
                if (department == null) {
                    return errorResponse("Department not found with id: " + departmentId, 404);
                }
            }

            // Duplicate detection: same user + trigger + department (or both null).
            Map<String, Object> dupParams = new HashMap<>();
            dupParams.put("u", user);
            dupParams.put("tt", tt);
            String dupJpql = "select i from TriggerSubscription i where i.retired=false"
                    + " and i.webUser=:u and i.triggerType=:tt"
                    + (department == null ? " and i.department is null" : " and i.department=:dep");
            if (department != null) {
                dupParams.put("dep", department);
            }
            TriggerSubscription existing = triggerSubscriptionFacade.findFirstByJpql(dupJpql, dupParams);
            if (existing != null) {
                Map<String, Object> found = new LinkedHashMap<>();
                found.put("id", existing.getId());
                found.put("triggerType", existing.getTriggerType() != null ? existing.getTriggerType().name() : null);
                found.put("userId", existing.getWebUser() != null ? existing.getWebUser().getId() : null);
                found.put("departmentId", existing.getDepartment() != null ? existing.getDepartment().getId() : null);
                found.put("applicationWide", existing.getDepartment() == null);
                Map<String, Object> alreadyExists = new LinkedHashMap<>();
                alreadyExists.put("status", "already_exists");
                alreadyExists.put("code", 200);
                alreadyExists.put("data", found);
                return Response.ok(gson.toJson(alreadyExists)).build();
            }

            // Next order number for this user within the same department scope.
            double nextOrder = nextOrderNumber(user, department);

            TriggerSubscription ts = new TriggerSubscription();
            ts.setWebUser(user);
            ts.setTriggerType(tt);
            ts.setDepartment(department);
            ts.setOrderNumber(nextOrder);
            ts.setCreatedAt(new Date());
            ts.setCreater(apiUser.getId() != null ? apiUser : null);
            triggerSubscriptionFacade.create(ts);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("code", 201);
            response.put("data", toMap(ts));
            return Response.status(201).entity(gson.toJson(response)).build();
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/subscriptions/{id}
    // -------------------------------------------------------------------------

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        try {
            WebUser apiUser = validateApiKey(requestContext.getHeader("Finance"));
            if (apiUser == null) {
                return errorResponse("Not a valid key", 401);
            }

            TriggerSubscription ts = triggerSubscriptionFacade.find(id);
            if (ts == null || ts.isRetired()) {
                return errorResponse("Subscription not found with id: " + id, 404);
            }
            if (ts.getWebUser() == null) {
                return errorResponse("Subscription " + id + " is a role-based subscription and cannot be managed through this API", 400);
            }
            ts.setRetired(true);
            ts.setRetiredAt(new Date());
            ts.setRetirer(apiUser.getId() != null ? apiUser : null);
            triggerSubscriptionFacade.edit(ts);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", ts.getId());
            result.put("deleted", true);
            return successResponse(result);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private double nextOrderNumber(WebUser user, Department department) {
        Map<String, Object> params = new HashMap<>();
        params.put("u", user);
        String jpql = "select i from TriggerSubscription i where i.retired=false and i.webUser=:u"
                + (department == null ? " and i.department is null" : " and i.department=:dep");
        if (department != null) {
            params.put("dep", department);
        }
        List<TriggerSubscription> existing = triggerSubscriptionFacade.findByJpql(jpql, params);
        double maxOrder = 0.0;
        if (existing != null) {
            for (TriggerSubscription subscription : existing) {
                maxOrder = Math.max(maxOrder, subscription.getOrderNumber());
            }
        }
        return maxOrder + 1.0;
    }

    private TriggerType resolveTriggerType(String name) {
        if (name == null) return null;
        for (TriggerType tt : TriggerType.values()) {
            if (tt.name().equalsIgnoreCase(name.trim())) {
                return tt;
            }
        }
        return null;
    }

    private Map<String, Object> toMap(TriggerSubscription s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("triggerType", s.getTriggerType() != null ? s.getTriggerType().name() : null);
        m.put("triggerLabel", s.getTriggerType() != null ? s.getTriggerType().getLabel() : null);
        m.put("userId", s.getWebUser() != null ? s.getWebUser().getId() : null);
        m.put("userName", s.getWebUser() != null ? s.getWebUser().getName() : null);
        m.put("departmentId", s.getDepartment() != null ? s.getDepartment().getId() : null);
        m.put("departmentName", s.getDepartment() != null ? s.getDepartment().getName() : null);
        m.put("applicationWide", s.getDepartment() == null);
        m.put("orderNumber", s.getOrderNumber());
        return m;
    }

    private String param(String key) {
        return uriInfo.getQueryParameters().getFirst(key);
    }

    private Long parseLong(String value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Gson parses JSON numbers as Double; accept Number or numeric string.
    // Reject fractional values (e.g. 12.9) instead of silently truncating to 12.
    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d) || d % 1 != 0) {
                return null;
            }
            return ((Number) value).longValue();
        }
        return parseLong(value.toString());
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    // Accept a JSON boolean true or the string "true" (case-insensitive).
    private boolean asBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return "true".equalsIgnoreCase(value.toString().trim());
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) return null;
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) return null;
        if (apiKey.isRetired()) return null;
        if (apiKey.getKeyType() == ApiKeyType.Config) return new WebUser();
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) return null;
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
