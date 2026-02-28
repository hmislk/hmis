package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.common.SecurityController;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.dto.user.DepartmentAssignmentRequestDTO;
import com.divudi.core.data.dto.user.PasswordChangeRequestDTO;
import com.divudi.core.data.dto.user.PrivilegeAssignmentRequestDTO;
import com.divudi.core.data.dto.user.UserUpsertRequestDTO;
import com.divudi.core.entity.*;
import com.divudi.core.facade.*;
import com.divudi.core.light.common.WebUserLight;
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
import javax.ws.rs.core.UriInfo;

@Path("users")
@RequestScoped
public class UserManagementApi {

    @Context
    private HttpServletRequest requestContext;
    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;
    @Inject
    private SecurityController securityController;

    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;
    @EJB
    private WebUserRolePrivilegeFacade webUserRolePrivilegeFacade;
    @EJB
    private WebUserDepartmentFacade webUserDepartmentFacade;
    @EJB
    private WebUserRoleFacade webUserRoleFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private PersonFacade personFacade;

    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers() {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);

            String q = value("query");
            int page = parseInt(value("page"), 0);
            int size = Math.min(Math.max(parseInt(value("size"), 20), 1), 100);

            Map<String, Object> m = new HashMap<>();
            String jpql = "select w from WebUser w where w.retired=false ";
            if (q != null && !q.trim().isEmpty()) {
                jpql += " and (upper(w.name) like :q or upper(w.code) like :q or upper(w.webUserPerson.name) like :q) ";
                m.put("q", "%" + q.trim().toUpperCase() + "%");
            }
            jpql += " order by w.name";
            List<WebUser> users = webUserFacade.findByJpql(jpql, m, size + (page * size));
            int from = Math.min(page * size, users.size());
            int to = Math.min(from + size, users.size());
            List<WebUserLight> out = new ArrayList<>();
            for (WebUser w : users.subList(from, to)) {
                out.add(new WebUserLight(w.getName(), w.getWebUserPerson() != null ? w.getWebUserPerson().getName() : null, w.getId(), w.getCode(), w.getStaff() != null && w.getStaff().getPerson() != null ? w.getStaff().getPerson().getNameWithTitle() : null));
            }
            return successResponse(out);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), 500);
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("id") Long id) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        WebUser u = webUserFacade.find(id);
        if (u == null || u.isRetired()) return errorResponse("User not found", 404);
        return successResponse(toUserMap(u));
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchUsers() {
        return listUsers();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUser(String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            UserUpsertRequestDTO req = gson.fromJson(body, UserUpsertRequestDTO.class);
            if (req == null || req.getName() == null || req.getName().trim().isEmpty() || req.getPassword() == null || req.getPassword().trim().isEmpty()) {
                return errorResponse("name and password are required", 400);
            }
            if (!webUserFacade.findByJpql("select w from WebUser w where w.retired=false and lower(w.name)=:n", Collections.singletonMap("n", req.getName().toLowerCase())).isEmpty()) {
                return errorResponse("User name already exists", 400);
            }
            WebUser u = new WebUser();
            Person p = new Person();
            p.setName(req.getPersonName() != null ? req.getPersonName() : req.getName());
            p.setMobile(req.getPersonMobile());
            p.setCreatedAt(new Date());
            p.setCreater(apiUser);
            personFacade.create(p);
            u.setWebUserPerson(p);
            applyUserChanges(u, req, apiUser, true);
            webUserFacade.create(u);
            return successResponse(toUserMap(u));
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), 500);
        }
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUser(@PathParam("id") Long id, String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            WebUser u = webUserFacade.find(id);
            if (u == null || u.isRetired()) return errorResponse("User not found", 404);
            UserUpsertRequestDTO req = gson.fromJson(body, UserUpsertRequestDTO.class);
            if (req == null) return errorResponse("Request body is required", 400);
            applyUserChanges(u, req, apiUser, false);
            webUserFacade.edit(u);
            return successResponse(toUserMap(u));
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), 500);
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireUser(@PathParam("id") Long id) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
        WebUser u = webUserFacade.find(id);
        if (u == null || u.isRetired()) return errorResponse("User not found", 404);
        u.setRetired(true);
        u.setRetirer(apiUser);
        u.setRetiredAt(new Date());
        u.setRetireComments(value("retireComments"));
        webUserFacade.edit(u);
        return successResponse(toUserMap(u));
    }

    @POST
    @Path("/{id}/reset-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetPassword(@PathParam("id") Long id, String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            WebUser u = webUserFacade.find(id);
            if (u == null || u.isRetired()) return errorResponse("User not found", 404);
            PasswordChangeRequestDTO req = gson.fromJson(body, PasswordChangeRequestDTO.class);
            if (req == null || req.getNewPassword() == null || req.getNewPassword().trim().isEmpty()) return errorResponse("newPassword is required", 400);
            u.setWebUserPassword(securityController.hashAndCheck(req.getNewPassword()));
            u.setNeedToResetPassword(true);
            u.setLastPasswordResetAt(new Date());
            webUserFacade.edit(u);
            return successResponse("Password reset successful");
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        }
    }

    @POST
    @Path("/{id}/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeOwnPassword(@PathParam("id") Long id, String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!apiUser.getId().equals(id) && !isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            WebUser u = webUserFacade.find(id);
            if (u == null || u.isRetired()) return errorResponse("User not found", 404);
            PasswordChangeRequestDTO req = gson.fromJson(body, PasswordChangeRequestDTO.class);
            if (req == null || req.getNewPassword() == null || req.getNewPassword().trim().isEmpty()) return errorResponse("newPassword is required", 400);
            if (apiUser.getId().equals(id) && (req.getCurrentPassword() == null || !securityController.matchPassword(req.getCurrentPassword(), u.getWebUserPassword()))) {
                return errorResponse("Current password is invalid", 400);
            }
            u.setWebUserPassword(securityController.hashAndCheck(req.getNewPassword()));
            u.setNeedToResetPassword(false);
            u.setLastPasswordResetAt(new Date());
            webUserFacade.edit(u);
            return successResponse("Password changed successfully");
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        }
    }

    @GET
    @Path("/{id}/privileges")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUserPrivileges(@PathParam("id") Long id) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        WebUser u = webUserFacade.find(id);
        if (u == null || u.isRetired()) return errorResponse("User not found", 404);
        List<WebUserPrivilege> ps = webUserPrivilegeFacade.findByJpql("select p from WebUserPrivilege p where p.retired=false and p.webUser=:u order by p.privilege", Collections.singletonMap("u", u));
        List<Map<String, Object>> out = new ArrayList<>();
        for (WebUserPrivilege p : ps) {
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
    public Response assignUserPrivileges(@PathParam("id") Long id, String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            WebUser u = webUserFacade.find(id);
            if (u == null || u.isRetired()) return errorResponse("User not found", 404);
            PrivilegeAssignmentRequestDTO req = gson.fromJson(body, PrivilegeAssignmentRequestDTO.class);
            if (req == null || req.getPrivileges() == null || req.getPrivileges().isEmpty()) return errorResponse("privileges are required", 400);
            Department d = req.getDepartmentId() != null ? departmentFacade.find(req.getDepartmentId()) : null;
            for (String pName : req.getPrivileges()) {
                Privileges p = Privileges.valueOf(pName);
                Map<String, Object> m = new HashMap<>();
                m.put("u", u);
                m.put("p", p);
                List<WebUserPrivilege> ex = webUserPrivilegeFacade.findByJpql("select wp from WebUserPrivilege wp where wp.retired=false and wp.webUser=:u and wp.privilege=:p", m);
                if (!ex.isEmpty()) continue;
                WebUserPrivilege wp = new WebUserPrivilege();
                wp.setWebUser(u);
                wp.setPrivilege(p);
                wp.setDepartment(d);
                wp.setCreater(apiUser);
                wp.setCreatedAt(new Date());
                webUserPrivilegeFacade.create(wp);
            }
            return listUserPrivileges(id);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), 400);
        }
    }

    @DELETE
    @Path("/{id}/privileges/{privilegeId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeUserPrivilege(@PathParam("id") Long id, @PathParam("privilegeId") Long privilegeId) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
        WebUserPrivilege p = webUserPrivilegeFacade.find(privilegeId);
        if (p == null || p.isRetired() || p.getWebUser() == null || !p.getWebUser().getId().equals(id)) return errorResponse("Privilege assignment not found", 404);
        p.setRetired(true);
        p.setRetirer(apiUser);
        p.setRetiredAt(new Date());
        webUserPrivilegeFacade.edit(p);
        return successResponse("Privilege revoked");
    }

    @GET
    @Path("/{id}/departments")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUserDepartments(@PathParam("id") Long id) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        WebUser u = webUserFacade.find(id);
        if (u == null || u.isRetired()) return errorResponse("User not found", 404);
        List<WebUserDepartment> ds = webUserDepartmentFacade.findByJpql("select d from WebUserDepartment d where d.retired=false and d.webUser=:u", Collections.singletonMap("u", u));
        List<Map<String, Object>> out = new ArrayList<>();
        for (WebUserDepartment d : ds) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("departmentId", d.getDepartment() != null ? d.getDepartment().getId() : null);
            m.put("departmentName", d.getDepartment() != null ? d.getDepartment().getName() : null);
            out.add(m);
        }
        return successResponse(out);
    }

    @POST
    @Path("/{id}/departments")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignUserDepartments(@PathParam("id") Long id, String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            WebUser u = webUserFacade.find(id);
            if (u == null || u.isRetired()) return errorResponse("User not found", 404);
            DepartmentAssignmentRequestDTO req = gson.fromJson(body, DepartmentAssignmentRequestDTO.class);
            if (req == null || req.getDepartmentIds() == null || req.getDepartmentIds().isEmpty()) return errorResponse("departmentIds are required", 400);
            for (Long did : req.getDepartmentIds()) {
                Department d = departmentFacade.find(did);
                if (d == null) continue;
                Map<String, Object> m = new HashMap<>();
                m.put("u", u);
                m.put("d", d);
                List<WebUserDepartment> ex = webUserDepartmentFacade.findByJpql("select ud from WebUserDepartment ud where ud.retired=false and ud.webUser=:u and ud.department=:d", m);
                if (!ex.isEmpty()) continue;
                WebUserDepartment ud = new WebUserDepartment();
                ud.setWebUser(u);
                ud.setDepartment(d);
                ud.setCreater(apiUser);
                ud.setCreatedAt(new Date());
                webUserDepartmentFacade.create(ud);
            }
            return listUserDepartments(id);
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        }
    }

    private void applyUserChanges(WebUser u, UserUpsertRequestDTO req, WebUser actor, boolean create) {
        if (req.getName() != null) u.setName(req.getName().trim());
        if (req.getCode() != null) u.setCode(req.getCode());
        if (req.getEmail() != null) u.setEmail(req.getEmail());
        if (req.getTelNo() != null) u.setTelNo(req.getTelNo());
        if (req.getInstitutionId() != null) u.setInstitution(institutionFacade.find(req.getInstitutionId()));
        if (req.getSiteId() != null) u.setSite(institutionFacade.find(req.getSiteId()));
        if (req.getDepartmentId() != null) u.setDepartment(departmentFacade.find(req.getDepartmentId()));
        if (req.getRoleId() != null) u.setRole(webUserRoleFacade.find(req.getRoleId()));
        if (req.getActivated() != null) u.setActivated(req.getActivated());
        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) u.setWebUserPassword(securityController.hashAndCheck(req.getPassword()));
        if (u.getWebUserPerson() != null) {
            if (req.getPersonName() != null) u.getWebUserPerson().setName(req.getPersonName());
            if (req.getPersonMobile() != null) u.getWebUserPerson().setMobile(req.getPersonMobile());
            personFacade.edit(u.getWebUserPerson());
        }
        if (create) {
            u.setCreatedAt(new Date());
            u.setCreater(actor);
            u.setActivated(u.isActivated());
            if (u.isActivated()) {
                u.setActivatedAt(new Date());
                u.setActivator(actor);
            }
        }
    }

    private Map<String, Object> toUserMap(WebUser u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("code", u.getCode());
        m.put("email", u.getEmail());
        m.put("telNo", u.getTelNo());
        m.put("activated", u.isActivated());
        m.put("retired", u.isRetired());
        m.put("institutionId", u.getInstitution() != null ? u.getInstitution().getId() : null);
        m.put("departmentId", u.getDepartment() != null ? u.getDepartment().getId() : null);
        m.put("siteId", u.getSite() != null ? u.getSite().getId() : null);
        m.put("roleId", u.getRole() != null ? u.getRole().getId() : null);
        m.put("personName", u.getWebUserPerson() != null ? u.getWebUserPerson().getName() : null);
        return m;
    }

    private String value(String key) { return uriInfo.getQueryParameters().getFirst(key); }

    private int parseInt(String v, int d) { try { return v == null ? d : Integer.parseInt(v); } catch (Exception e) { return d; } }

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
        if (!webUserPrivilegeFacade.findByJpql("select wp from WebUserPrivilege wp where wp.retired=false and wp.webUser=:u and wp.privilege=:p", m).isEmpty()) {
            return true;
        }
        if (user.getRole() == null) return false;
        Map<String, Object> r = new HashMap<>();
        r.put("r", user.getRole());
        r.put("p", Privileges.Admin);
        return !webUserRolePrivilegeFacade.findByJpql("select rp from WebUserRolePrivilege rp where rp.retired=false and rp.webUserRole=:r and rp.privilege=:p", r).isEmpty();
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
