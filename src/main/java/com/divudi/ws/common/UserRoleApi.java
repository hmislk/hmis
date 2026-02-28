package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.dto.user.RolePrivilegeAssignmentRequestDTO;
import com.divudi.core.data.dto.user.UserRoleUpsertRequestDTO;
import com.divudi.core.entity.*;
import com.divudi.core.facade.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.util.*;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("user-roles")
@RequestScoped
public class UserRoleApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private WebUserRoleFacade roleFacade;
    @EJB
    private WebUserRolePrivilegeFacade rolePrivilegeFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;

    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRoles() {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        List<WebUserRole> roles = roleFacade.findByJpql("select r from WebUserRole r where r.retired=false order by r.name");
        List<Map<String, Object>> out = new ArrayList<>();
        for (WebUserRole r : roles) out.add(toRoleMap(r));
        return successResponse(out);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRole(@PathParam("id") Long id) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        WebUserRole r = roleFacade.find(id);
        if (r == null || r.isRetired()) return errorResponse("Role not found", 404);
        return successResponse(toRoleMap(r));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRole(String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            UserRoleUpsertRequestDTO req = gson.fromJson(body, UserRoleUpsertRequestDTO.class);
            if (req == null || req.getName() == null || req.getName().trim().isEmpty()) return errorResponse("name is required", 400);
            WebUserRole r = new WebUserRole();
            r.setName(req.getName().trim());
            r.setDescription(req.getDescription());
            r.setCreatedAt(new Date());
            r.setCreater(apiUser);
            roleFacade.create(r);
            return successResponse(toRoleMap(r));
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRole(@PathParam("id") Long id, String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            WebUserRole r = roleFacade.find(id);
            if (r == null || r.isRetired()) return errorResponse("Role not found", 404);
            UserRoleUpsertRequestDTO req = gson.fromJson(body, UserRoleUpsertRequestDTO.class);
            if (req == null) return errorResponse("Request body is required", 400);
            if (req.getName() != null) r.setName(req.getName());
            if (req.getDescription() != null) r.setDescription(req.getDescription());
            roleFacade.edit(r);
            return successResponse(toRoleMap(r));
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireRole(@PathParam("id") Long id) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
        WebUserRole r = roleFacade.find(id);
        if (r == null || r.isRetired()) return errorResponse("Role not found", 404);
        r.setRetired(true);
        r.setRetirer(apiUser);
        r.setRetiredAt(new Date());
        roleFacade.edit(r);
        return successResponse(toRoleMap(r));
    }

    @GET
    @Path("/{id}/privileges")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRolePrivileges(@PathParam("id") Long id) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        WebUserRole r = roleFacade.find(id);
        if (r == null || r.isRetired()) return errorResponse("Role not found", 404);
        List<WebUserRolePrivilege> ps = rolePrivilegeFacade.findByJpql("select p from WebUserRolePrivilege p where p.retired=false and p.webUserRole=:r", Collections.singletonMap("r", r));
        List<Map<String, Object>> out = new ArrayList<>();
        for (WebUserRolePrivilege p : ps) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("privilege", p.getPrivilege() != null ? p.getPrivilege().name() : null);
            m.put("departmentId", p.getDepartment() != null ? p.getDepartment().getId() : null);
            out.add(m);
        }
        return successResponse(out);
    }

    @POST
    @Path("/{id}/privileges")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignRolePrivileges(@PathParam("id") Long id, String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            WebUserRole r = roleFacade.find(id);
            if (r == null || r.isRetired()) return errorResponse("Role not found", 404);
            RolePrivilegeAssignmentRequestDTO req = gson.fromJson(body, RolePrivilegeAssignmentRequestDTO.class);
            if (req == null || req.getPrivileges() == null || req.getPrivileges().isEmpty()) return errorResponse("privileges are required", 400);
            Department d = req.getDepartmentId() != null ? departmentFacade.find(req.getDepartmentId()) : null;
            for (String pName : req.getPrivileges()) {
                Privileges p = Privileges.valueOf(pName);
                Map<String, Object> m = new HashMap<>();
                m.put("r", r);
                m.put("p", p);
                List<WebUserRolePrivilege> ex = rolePrivilegeFacade.findByJpql("select rp from WebUserRolePrivilege rp where rp.retired=false and rp.webUserRole=:r and rp.privilege=:p", m);
                if (!ex.isEmpty()) continue;
                WebUserRolePrivilege rp = new WebUserRolePrivilege();
                rp.setWebUserRole(r);
                rp.setPrivilege(p);
                rp.setDepartment(d);
                rp.setCreater(apiUser);
                rp.setCreatedAt(new Date());
                rolePrivilegeFacade.create(rp);
            }
            return listRolePrivileges(id);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), 400);
        }
    }

    private Map<String, Object> toRoleMap(WebUserRole r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("name", r.getName());
        m.put("description", r.getDescription());
        m.put("retired", r.isRetired());
        return m;
    }

    private WebUser validateApiUser() {
        String key = requestContext.getHeader("Finance");
        if (key == null || key.trim().isEmpty()) return null;
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null || apiKey.getWebUser() == null || apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) return null;
        WebUser u = apiKey.getWebUser();
        if (u.isRetired() || !u.isActivated()) return null;
        return u;
    }

    private boolean isAdmin(WebUser user) {
        Map<String, Object> m = new HashMap<>();
        m.put("u", user);
        m.put("p", Privileges.Admin);
        return !webUserPrivilegeFacade.findByJpql("select wp from WebUserPrivilege wp where wp.retired=false and wp.webUser=:u and wp.privilege=:p", m).isEmpty();
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
