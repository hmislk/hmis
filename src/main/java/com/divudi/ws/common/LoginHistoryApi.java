package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Logins;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.LoginsFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.*;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * REST API for querying login history.
 * Provides endpoints to find recent logins filtered by department, user, or date range.
 */
@Path("logins")
@RequestScoped
public class LoginHistoryApi {

    @Context
    private HttpServletRequest requestContext;
    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private LoginsFacade loginsFacade;
    @EJB
    private DepartmentFacade departmentFacade;

    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    /**
     * Query login history with optional filters.
     *
     * GET /api/logins?departmentId=485&page=0&size=20
     * GET /api/logins?userId=60103&page=0&size=20
     * GET /api/logins/last-per-user?departmentId=485&size=20
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listLogins() {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);

            String departmentIdStr = value("departmentId");
            String userIdStr = value("userId");
            int page = Math.max(parseInt(value("page"), 0), 0);
            int size = Math.min(Math.max(parseInt(value("size"), 20), 1), 100);

            long offsetLong = (long) page * size;
            if (offsetLong > Integer.MAX_VALUE) {
                return errorResponse("page is too large", 400);
            }
            int offset = (int) offsetLong;

            Map<String, Object> m = new HashMap<>();
            StringBuilder jpql = new StringBuilder("select l from Logins l where l.logedAt is not null");

            if (departmentIdStr != null && !departmentIdStr.trim().isEmpty()) {
                Long deptId = parseLong(departmentIdStr, null);
                if (deptId == null) return errorResponse("Invalid departmentId", 400);
                Department dept = departmentFacade.find(deptId);
                if (dept == null) return errorResponse("Department not found", 404);
                jpql.append(" and l.department=:dept");
                m.put("dept", dept);
            }

            if (userIdStr != null && !userIdStr.trim().isEmpty()) {
                Long uid = parseLong(userIdStr, null);
                if (uid == null) return errorResponse("Invalid userId", 400);
                jpql.append(" and l.webUser.id=:uid");
                m.put("uid", uid);
            }

            jpql.append(" order by l.logedAt desc");

            List<Logins> logins = loginsFacade.findByJpql(jpql.toString(), m, size + offset);
            int from = Math.min(offset, logins.size());
            int to = Math.min(from + size, logins.size());

            List<Map<String, Object>> out = new ArrayList<>();
            for (Logins l : logins.subList(from, to)) {
                out.add(toLoginMap(l));
            }
            return successResponse(out);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
        }
    }

    /**
     * Get last login per unique user for a department.
     * Useful for finding "who last logged into Main Pharmacy".
     *
     * GET /api/logins/last-per-user?departmentId=485&size=20
     */
    @GET
    @Path("/last-per-user")
    @Produces(MediaType.APPLICATION_JSON)
    public Response lastLoginPerUser() {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);

            String departmentIdStr = value("departmentId");
            int size = Math.min(Math.max(parseInt(value("size"), 20), 1), 100);

            Map<String, Object> m = new HashMap<>();
            StringBuilder jpql = new StringBuilder(
                    "select l from Logins l where l.logedAt is not null"
                    + " and l.webUser.retired=false");

            if (departmentIdStr != null && !departmentIdStr.trim().isEmpty()) {
                Long deptId = parseLong(departmentIdStr, null);
                if (deptId == null) return errorResponse("Invalid departmentId", 400);
                Department dept = departmentFacade.find(deptId);
                if (dept == null) return errorResponse("Department not found", 404);
                jpql.append(" and l.department=:dept");
                m.put("dept", dept);
            }

            jpql.append(" order by l.logedAt desc");

            // Fetch more than needed to deduplicate by user
            List<Logins> logins = loginsFacade.findByJpql(jpql.toString(), m, size * 10);

            Set<Long> seenUsers = new LinkedHashSet<>();
            List<Map<String, Object>> out = new ArrayList<>();
            for (Logins l : logins) {
                if (l.getWebUser() == null) continue;
                Long uid = l.getWebUser().getId();
                if (seenUsers.contains(uid)) continue;
                seenUsers.add(uid);
                Map<String, Object> entry = new HashMap<>();
                entry.put("userId", uid);
                entry.put("userName", l.getWebUser().getName());
                entry.put("departmentId", l.getDepartment() != null ? l.getDepartment().getId() : null);
                entry.put("departmentName", l.getDepartment() != null ? l.getDepartment().getName() : null);
                entry.put("lastLogin", l.getLogedAt());
                out.add(entry);
                if (out.size() >= size) break;
            }
            return successResponse(out);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
        }
    }

    private Map<String, Object> toLoginMap(Logins l) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", l.getId());
        m.put("userId", l.getWebUser() != null ? l.getWebUser().getId() : null);
        m.put("userName", l.getWebUser() != null ? l.getWebUser().getName() : null);
        m.put("departmentId", l.getDepartment() != null ? l.getDepartment().getId() : null);
        m.put("departmentName", l.getDepartment() != null ? l.getDepartment().getName() : null);
        m.put("institutionId", l.getInstitution() != null ? l.getInstitution().getId() : null);
        m.put("logedAt", l.getLogedAt());
        m.put("logoutAt", l.getLogoutAt());
        m.put("ipAddress", l.getIpaddress());
        m.put("browser", l.getBrowser());
        return m;
    }

    private String value(String key) { return uriInfo.getQueryParameters().getFirst(key); }
    private int parseInt(String v, int d) { try { return v == null ? d : Integer.parseInt(v); } catch (Exception e) { return d; } }
    private Long parseLong(String v, Long d) { try { return v == null ? d : Long.parseLong(v); } catch (Exception e) { return d; } }

    private WebUser validateApiUser() {
        String key = requestContext.getHeader("Finance");
        if (key == null || key.trim().isEmpty()) return null;
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null || apiKey.getWebUser() == null || apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) return null;
        WebUser u = apiKey.getWebUser();
        if (u.isRetired() || !u.isActivated()) return null;
        return u;
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
