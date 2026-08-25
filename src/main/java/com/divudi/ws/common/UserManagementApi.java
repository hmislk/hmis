package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.common.SecurityController;
import com.divudi.core.data.LoginPage;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.dto.user.BulkPrivilegeAssignmentRequestDTO;
import com.divudi.core.data.dto.user.DepartmentAssignmentRequestDTO;
import com.divudi.core.data.dto.user.PasswordChangeRequestDTO;
import com.divudi.core.data.dto.user.PrivilegeCategoryAssignmentRequestDTO;
import com.divudi.core.data.dto.user.PrivilegeAssignmentRequestDTO;
import com.divudi.core.data.dto.user.UserUpsertRequestDTO;
import com.divudi.core.entity.*;
import com.divudi.core.facade.*;
import com.divudi.core.light.common.WebUserLight;
import com.divudi.service.UserRoleApplicationService;
import com.divudi.service.UserRoleApplicationService.RoleAspect;
import com.divudi.service.UserRoleApplicationService.RoleApplicationResult;
import com.divudi.service.UserRoleApplicationService.RoleOperation;
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
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private UserIconFacade userIconFacade;
    @EJB
    private TriggerSubscriptionFacade triggerSubscriptionFacade;
    @EJB
    private WebUserDefaultLoginPageFacade webUserDefaultLoginPageFacade;
    @EJB
    private UserRoleApplicationService userRoleApplicationService;

    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUsers() {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);

            String q = value("query");
            int page = Math.max(parseInt(value("page"), 0), 0);
            int size = Math.min(Math.max(parseInt(value("size"), 20), 1), 100);

            long offsetLong = (long) page * size;
            if (offsetLong > Integer.MAX_VALUE) {
                return errorResponse("page is too large", 400);
            }
            int offset = (int) offsetLong;

            String departmentIdStr = value("departmentId");

            Map<String, Object> m = new HashMap<>();
            String jpql;
            if (departmentIdStr != null && !departmentIdStr.trim().isEmpty()) {
                Long deptId = parseLong(departmentIdStr, null);
                if (deptId == null) return errorResponse("Invalid departmentId", 400);
                Department dept = departmentFacade.find(deptId);
                if (dept == null) return errorResponse("Department not found", 404);
                jpql = "select distinct w from WebUser w join WebUserDepartment wud on wud.webUser=w"
                        + " where w.retired=false and wud.retired=false and wud.department=:dept";
                m.put("dept", dept);
                if (q != null && !q.trim().isEmpty()) {
                    jpql += " and (upper(w.name) like :q or upper(w.code) like :q or upper(w.webUserPerson.name) like :q)";
                    m.put("q", "%" + q.trim().toUpperCase() + "%");
                }
            } else {
                jpql = "select w from WebUser w where w.retired=false";
                if (q != null && !q.trim().isEmpty()) {
                    jpql += " and (upper(w.name) like :q or upper(w.code) like :q or upper(w.webUserPerson.name) like :q)";
                    m.put("q", "%" + q.trim().toUpperCase() + "%");
                }
            }
            jpql += " order by w.name";
            List<WebUser> users = webUserFacade.findByJpql(jpql, m, size + offset);
            int from = Math.min(offset, users.size());
            int to = Math.min(from + size, users.size());
            List<WebUserLight> out = new ArrayList<>();
            for (WebUser w : users.subList(from, to)) {
                WebUserLight light = new WebUserLight(w.getName(), w.getWebUserPerson() != null ? w.getWebUserPerson().getName() : null, w.getId(), w.getCode(), w.getStaff() != null && w.getStaff().getPerson() != null ? w.getStaff().getPerson().getNameWithTitle() : null);
                light.setLoginPage(w.getLoginPage() != null ? w.getLoginPage().name() : null);
                out.add(light);
            }
            return successResponse(out);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
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
            if (req == null || req.getPassword() == null || req.getPassword().trim().isEmpty()) {
                return errorResponse("name and password are required", 400);
            }
            String normalizedName = req.getName() == null ? null : req.getName().trim();
            if (normalizedName == null || normalizedName.isEmpty()) {
                return errorResponse("name is required", 400);
            }
            req.setName(normalizedName);
            if (!webUserFacade.findByJpql("select w from WebUser w where w.retired=false and lower(w.name)=:n", Collections.singletonMap("n", normalizedName.toLowerCase())).isEmpty()) {
                return errorResponse("User name already exists", 400);
            }
            Staff linkedStaff = null;
            if (req.getStaffId() != null) {
                linkedStaff = staffFacade.find(req.getStaffId());
                if (linkedStaff == null || linkedStaff.isRetired()) return errorResponse("Staff not found: " + req.getStaffId(), 404);
            }
            WebUser u = new WebUser();
            Person p = new Person();
            p.setName(req.getPersonName() != null ? req.getPersonName() : normalizedName);
            p.setMobile(req.getPersonMobile());
            p.setCreatedAt(new Date());
            p.setCreater(apiUser);
            personFacade.create(p);
            u.setWebUserPerson(p);
            if (linkedStaff != null) {
                u.setStaff(linkedStaff);
            }
            applyUserChanges(u, req, apiUser, true);
            webUserFacade.create(u);
            return successResponse(toUserMap(u));
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
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
            if (req.getName() != null) {
                String normalizedName = req.getName().trim();
                if (normalizedName.isEmpty()) return errorResponse("name cannot be empty", 400);
                Map<String, Object> dupCheck = new HashMap<>();
                dupCheck.put("n", normalizedName.toLowerCase());
                dupCheck.put("id", id);
                if (!webUserFacade.findByJpql("select w from WebUser w where w.retired=false and lower(w.name)=:n and w.id<>:id", dupCheck).isEmpty()) {
                    return errorResponse("User name already exists", 400);
                }
                req.setName(normalizedName);
            }
            applyUserChanges(u, req, apiUser, false);
            webUserFacade.edit(u);
            return successResponse(toUserMap(u));
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
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

    /**
     * POST /api/users/{id}/force-password-reset
     * Flags the user for a mandatory password reset on next login (respected by the
     * "Allow admin to force password change" config option), without requiring the
     * caller to supply or know a new password value. Distinct from /reset-password,
     * which sets an actual new password. Audit-logged via ApiKey attribution, unlike
     * a direct database update.
     */
    @POST
    @Path("/{id}/force-password-reset")
    @Produces(MediaType.APPLICATION_JSON)
    public Response forcePasswordReset(@PathParam("id") Long id) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
        WebUser u = webUserFacade.find(id);
        if (u == null || u.isRetired()) return errorResponse("User not found", 404);
        u.setNeedToResetPassword(true);
        webUserFacade.edit(u);
        return successResponse(toUserMap(u));
    }

    /**
     * GET /api/users/password-status
     * Optional query params: from, to (yyyy-MM-dd) — filters the list to users whose
     * lastPasswordResetAt falls within the (inclusive) range. Omitting both returns
     * every active user, including those who have never reset their password
     * (lastPasswordResetAt: null). A 'from' bound always excludes never-reset users
     * (they haven't reset since anything). A 'to'-only bound (no 'from') includes
     * never-reset users - they are, by definition, at least as overdue as any date.
     */
    @GET
    @Path("/password-status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listPasswordStatus() {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);

        String fromStr = value("from");
        String toStr = value("to");
        Date from = parseDateParam(fromStr);
        if (fromStr != null && !fromStr.trim().isEmpty() && from == null) {
            return errorResponse("Invalid 'from' date, expected yyyy-MM-dd", 400);
        }
        Date to = parseDateParam(toStr);
        if (toStr != null && !toStr.trim().isEmpty() && to == null) {
            return errorResponse("Invalid 'to' date, expected yyyy-MM-dd", 400);
        }
        if (to != null) {
            to = new Date(to.getTime() + 24L * 60 * 60 * 1000 - 1); // make 'to' inclusive of the whole day
        }

        List<WebUser> users = webUserFacade.findByJpql("select w from WebUser w where w.retired=false order by w.name");
        List<Map<String, Object>> out = new ArrayList<>();
        for (WebUser u : users) {
            Date lastReset = u.getLastPasswordResetAtRaw();
            if (from != null && (lastReset == null || lastReset.before(from))) continue;
            if (to != null && lastReset != null && lastReset.after(to)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getId());
            m.put("name", u.getName());
            m.put("personName", u.getWebUserPerson() != null ? u.getWebUserPerson().getName() : null);
            m.put("lastPasswordResetAt", lastReset);
            m.put("needToResetPassword", u.isNeedToResetPassword());
            out.add(m);
        }
        return successResponse(out);
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
            m.put("privilege", p.getPrivilege() != null ? canonicalPrivilegeName(p.getPrivilege()) : null);
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
            if (req.getDepartmentId() == null) return errorResponse("departmentId is required for privileges to take effect", 400);
            Department d = departmentFacade.find(req.getDepartmentId());
            if (d == null) return errorResponse("Department not found: " + req.getDepartmentId(), 404);
            for (String pName : req.getPrivileges()) {
                Privileges p;
                try {
                    p = Privileges.valueOf(pName);
                } catch (IllegalArgumentException e) {
                    return errorResponse("Invalid privilege: " + pName, 400);
                }
                Map<String, Object> m = new HashMap<>();
                m.put("u", u);
                m.put("p", p);
                m.put("d", d);
                List<WebUserPrivilege> ex = webUserPrivilegeFacade.findByJpql(
                        "select wp from WebUserPrivilege wp where wp.retired=false and wp.webUser=:u and wp.privilege=:p and wp.department=:d",
                        m);
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
            return errorResponse("Internal server error", 500);
        }
    }

    @POST
    @Path("/{id}/departments/{departmentId}/privileges/category")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignUserPrivilegeCategories(@PathParam("id") Long id, @PathParam("departmentId") Long departmentId, String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            WebUser u = webUserFacade.find(id);
            if (u == null || u.isRetired()) return errorResponse("User not found", 404);
            Department d = departmentFacade.find(departmentId);
            if (d == null) return errorResponse("Department not found: " + departmentId, 404);
            PrivilegeCategoryAssignmentRequestDTO req = gson.fromJson(body, PrivilegeCategoryAssignmentRequestDTO.class);
            if (req == null || req.getCategories() == null || req.getCategories().isEmpty()) return errorResponse("categories are required", 400);

            Set<String> requestedCategories = new LinkedHashSet<>();
            for (String category : req.getCategories()) {
                if (category == null || category.trim().isEmpty()) return errorResponse("categories cannot contain blank values", 400);
                requestedCategories.add(category.trim());
            }

            List<Privileges> matchingPrivileges = new ArrayList<>();
            Set<String> matchedCategories = new HashSet<>();
            for (Privileges p : Privileges.values()) {
                String category = p.getCategory();
                if (category != null && requestedCategories.contains(category)) {
                    matchingPrivileges.add(p);
                    matchedCategories.add(category);
                }
            }
            Set<String> missingCategories = new LinkedHashSet<>(requestedCategories);
            missingCategories.removeAll(matchedCategories);
            if (!missingCategories.isEmpty()) return errorResponse("Unknown privilege categories: " + missingCategories, 400);

            int added = 0;
            int skipped = 0;
            for (Privileges p : matchingPrivileges) {
                Map<String, Object> m = new HashMap<>();
                m.put("u", u);
                m.put("p", p);
                m.put("d", d);
                List<WebUserPrivilege> ex = webUserPrivilegeFacade.findByJpql(
                        "select wp from WebUserPrivilege wp where wp.retired=false and wp.webUser=:u and wp.privilege=:p and wp.department=:d",
                        m);
                if (!ex.isEmpty()) {
                    skipped++;
                    continue;
                }
                WebUserPrivilege wp = new WebUserPrivilege();
                wp.setWebUser(u);
                wp.setPrivilege(p);
                wp.setDepartment(d);
                wp.setCreater(apiUser);
                wp.setCreatedAt(new Date());
                webUserPrivilegeFacade.create(wp);
                added++;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", u.getId());
            result.put("departmentId", d.getId());
            result.put("categories", requestedCategories);
            result.put("privilegesMatched", matchingPrivileges.size());
            result.put("privilegesAdded", added);
            result.put("privilegesSkipped", skipped);
            return successResponse(result);
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
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
            List<Long> invalidDepartmentIds = new ArrayList<>();
            for (Long did : req.getDepartmentIds()) {
                Department d = departmentFacade.find(did);
                if (d == null) {
                    invalidDepartmentIds.add(did);
                    continue;
                }
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
            if (!invalidDepartmentIds.isEmpty()) {
                return errorResponse("Invalid departmentIds: " + invalidDepartmentIds, 400);
            }
            return listUserDepartments(id);
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        }
    }

    @DELETE
    @Path("/{id}/departments/{assignmentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeUserDepartment(@PathParam("id") Long id, @PathParam("assignmentId") Long assignmentId) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
        WebUser u = webUserFacade.find(id);
        if (u == null || u.isRetired()) return errorResponse("User not found", 404);
        WebUserDepartment wud = webUserDepartmentFacade.find(assignmentId);
        if (wud == null || wud.isRetired() || wud.getWebUser() == null || !wud.getWebUser().getId().equals(id)) {
            return errorResponse("Department assignment not found", 404);
        }
        wud.setRetired(true);
        wud.setRetirer(apiUser);
        wud.setRetiredAt(new Date());
        webUserDepartmentFacade.edit(wud);
        return successResponse("Department assignment revoked");
    }

    @DELETE
    @Path("/{id}/departments/{departmentId}/privileges")
    @Produces(MediaType.APPLICATION_JSON)
    public Response revokeAllDepartmentPrivileges(@PathParam("id") Long id, @PathParam("departmentId") Long departmentId) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
        WebUser u = webUserFacade.find(id);
        if (u == null || u.isRetired()) return errorResponse("User not found", 404);
        Department dept = departmentFacade.find(departmentId);
        if (dept == null) return errorResponse("Department not found", 404);
        Map<String, Object> m = new HashMap<>();
        m.put("u", u);
        m.put("d", dept);
        m.put("retirer", apiUser);
        m.put("retiredAt", new Date());
        int revoked = webUserPrivilegeFacade.updateByJpql(
                "update WebUserPrivilege wp set wp.retired=true, wp.retirer=:retirer, wp.retiredAt=:retiredAt "
                + "where wp.retired=false and wp.webUser=:u and wp.department=:d", m);
        Map<String, Object> result = new HashMap<>();
        result.put("privilegesRevoked", revoked);
        return successResponse(result);
    }

    @POST
    @Path("/{id}/departments/{departmentId}/privileges/all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignAllDepartmentPrivileges(@PathParam("id") Long id, @PathParam("departmentId") Long departmentId) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
        WebUser u = webUserFacade.find(id);
        if (u == null || u.isRetired()) return errorResponse("User not found", 404);
        Department dept = departmentFacade.find(departmentId);
        if (dept == null) return errorResponse("Department not found", 404);
        Map<String, Object> existing = new HashMap<>();
        existing.put("u", u);
        existing.put("d", dept);
        List<WebUserPrivilege> currentPrivileges = webUserPrivilegeFacade.findByJpql(
                "select wp from WebUserPrivilege wp where wp.retired=false and wp.webUser=:u and wp.department=:d", existing);
        Set<Privileges> alreadyAssigned = new HashSet<>();
        for (WebUserPrivilege wp : currentPrivileges) {
            if (wp.getPrivilege() != null) alreadyAssigned.add(wp.getPrivilege());
        }
        int added = 0;
        int skipped = 0;
        for (Privileges p : Privileges.values()) {
            if (alreadyAssigned.contains(p)) {
                skipped++;
                continue;
            }
            WebUserPrivilege wp = new WebUserPrivilege();
            wp.setWebUser(u);
            wp.setPrivilege(p);
            wp.setDepartment(dept);
            wp.setCreater(apiUser);
            wp.setCreatedAt(new Date());
            webUserPrivilegeFacade.create(wp);
            added++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("privilegesAdded", added);
        result.put("privilegesSkipped", skipped);
        return successResponse(result);
    }

    /**
     * POST /api/users/{id}/privileges/all
     * Body: {"departmentIds": [481, 485]}  — optional; if omitted, uses all loggable departments.
     * Assigns every Privileges enum value across the specified (or all loggable) departments.
     * Returns {privilegesAdded, privilegesSkipped, departments: [{departmentId, added, skipped}]}.
     */
    @POST
    @Path("/{id}/privileges/all")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignAllPrivilegesMultiDept(@PathParam("id") Long id, String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            WebUser u = webUserFacade.find(id);
            if (u == null || u.isRetired()) return errorResponse("User not found", 404);

            // Parse optional departmentIds from body
            List<Long> requestedDeptIds = new ArrayList<>();
            if (body != null && !body.trim().isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> req = gson.fromJson(body, Map.class);
                    if (req != null && req.containsKey("departmentIds")) {
                        Object deptIdsObj = req.get("departmentIds");
                        if (deptIdsObj instanceof List) {
                            List<?> rawList = (List<?>) deptIdsObj;
                            for (Object item : rawList) {
                                if (item instanceof Number) {
                                    requestedDeptIds.add(((Number) item).longValue());
                                } else {
                                    return errorResponse("departmentIds must be an array of integers, got: " + item, 400);
                                }
                            }
                        }
                    }
                } catch (JsonSyntaxException e) {
                    return errorResponse("Invalid JSON format", 400);
                }
            }

            // Determine target departments
            List<Department> targetDepts = new ArrayList<>();
            if (!requestedDeptIds.isEmpty()) {
                for (Long deptId : requestedDeptIds) {
                    Department dept = departmentFacade.find(deptId);
                    if (dept == null) return errorResponse("Department not found: " + deptId, 404);
                    targetDepts.add(dept);
                }
            } else {
                List<WebUserDepartment> userDepts = webUserDepartmentFacade.findByJpql(
                        "select d from WebUserDepartment d where d.retired=false and d.webUser=:u",
                        Collections.singletonMap("u", u));
                for (WebUserDepartment wud : userDepts) {
                    if (wud.getDepartment() != null) targetDepts.add(wud.getDepartment());
                }
            }

            int totalAdded = 0;
            int totalSkipped = 0;
            List<Map<String, Object>> deptResults = new ArrayList<>();

            for (Department dept : targetDepts) {
                Map<String, Object> existingParams = new HashMap<>();
                existingParams.put("u", u);
                existingParams.put("d", dept);
                List<WebUserPrivilege> currentPrivileges = webUserPrivilegeFacade.findByJpql(
                        "select wp from WebUserPrivilege wp where wp.retired=false and wp.webUser=:u and wp.department=:d",
                        existingParams);
                Set<Privileges> alreadyAssigned = new HashSet<>();
                for (WebUserPrivilege wp : currentPrivileges) {
                    if (wp.getPrivilege() != null) alreadyAssigned.add(wp.getPrivilege());
                }
                int deptAdded = 0;
                int deptSkipped = 0;
                for (Privileges p : Privileges.values()) {
                    if (alreadyAssigned.contains(p)) {
                        deptSkipped++;
                        continue;
                    }
                    WebUserPrivilege wp = new WebUserPrivilege();
                    wp.setWebUser(u);
                    wp.setPrivilege(p);
                    wp.setDepartment(dept);
                    wp.setCreater(apiUser);
                    wp.setCreatedAt(new Date());
                    webUserPrivilegeFacade.create(wp);
                    deptAdded++;
                }
                totalAdded += deptAdded;
                totalSkipped += deptSkipped;
                Map<String, Object> deptResult = new LinkedHashMap<>();
                deptResult.put("departmentId", dept.getId());
                deptResult.put("departmentName", dept.getName());
                deptResult.put("added", deptAdded);
                deptResult.put("skipped", deptSkipped);
                deptResults.add(deptResult);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", u.getId());
            result.put("privilegesAdded", totalAdded);
            result.put("privilegesSkipped", totalSkipped);
            result.put("departments", deptResults);
            return successResponse(result);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
        }
    }

    /**
     * PUT /api/users/{id}/staff
     * Body: {"staffId": 12345}
     * Links an existing (non-retired) Staff to a WebUser.
     */
    @PUT
    @Path("/{id}/staff")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response linkStaffToUser(@PathParam("id") Long id, String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            WebUser u = webUserFacade.find(id);
            if (u == null || u.isRetired()) return errorResponse("User not found", 404);
            if (body == null || body.trim().isEmpty()) return errorResponse("Request body is required", 400);

            @SuppressWarnings("unchecked")
            Map<String, Object> req = gson.fromJson(body, Map.class);
            if (req == null || !req.containsKey("staffId")) return errorResponse("staffId is required", 400);
            Object staffIdObj = req.get("staffId");
            Long staffId;
            try {
                staffId = ((Number) staffIdObj).longValue();
            } catch (Exception e) {
                return errorResponse("staffId must be a numeric ID", 400);
            }
            Staff staff = staffFacade.find(staffId);
            if (staff == null || staff.isRetired()) return errorResponse("Staff not found: " + staffId, 404);

            u.setStaff(staff);
            webUserFacade.edit(u);
            return successResponse(toUserMap(u));
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
        }
    }

    @GET
    @Path("/privileges/available")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAvailablePrivileges() {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        // Deduplicate by lowercase name; first occurrence wins so the canonical name
        // matches whatever MySQL has already stored (case-insensitive collation means
        // PharmacySaleWithoutStock and PharmacySaleWithOutStock occupy the same row).
        Map<String, String> canonicals = buildCanonicalPrivilegeMap();
        Set<String> seenLower = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        for (Privileges p : Privileges.values()) {
            String lower = p.name().toLowerCase();
            if (seenLower.add(lower)) {
                out.add(canonicals.get(lower));
            }
        }
        return successResponse(out);
    }

    @POST
    @Path("/bulk-privileges")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bulkAssignPrivileges(String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            BulkPrivilegeAssignmentRequestDTO req = gson.fromJson(body, BulkPrivilegeAssignmentRequestDTO.class);
            if (req == null || req.getUserIds() == null || req.getUserIds().isEmpty()) {
                return errorResponse("userIds are required", 400);
            }
            if (req.getPrivileges() == null || req.getPrivileges().isEmpty()) {
                return errorResponse("privileges are required", 400);
            }
            final int MAX_USERS = 500;
            final int MAX_PRIVILEGES = 100;
            if (req.getUserIds().size() > MAX_USERS) {
                return errorResponse("Too many userIds. Max allowed: " + MAX_USERS, 400);
            }
            if (req.getPrivileges().size() > MAX_PRIVILEGES) {
                return errorResponse("Too many privileges. Max allowed: " + MAX_PRIVILEGES, 400);
            }
            // Deduplicate inputs
            req.setUserIds(new ArrayList<>(new LinkedHashSet<>(req.getUserIds())));
            req.setPrivileges(new ArrayList<>(new LinkedHashSet<>(req.getPrivileges())));
            // Validate and resolve privilege names up front, guarding against null/blank
            List<Privileges> privileges = new ArrayList<>();
            for (String pName : req.getPrivileges()) {
                if (pName == null || pName.trim().isEmpty()) {
                    return errorResponse("privileges list contains a null or blank entry", 400);
                }
                try {
                    privileges.add(Privileges.valueOf(pName.trim()));
                } catch (IllegalArgumentException e) {
                    return errorResponse("Invalid privilege: " + pName, 400);
                }
            }
            // Guard against null elements in userIds
            for (Long userId : req.getUserIds()) {
                if (userId == null) {
                    return errorResponse("userIds cannot contain null", 400);
                }
            }
            Department fixedDept = req.getDepartmentId() != null ? departmentFacade.find(req.getDepartmentId()) : null;
            if (req.getDepartmentId() != null && fixedDept == null) {
                return errorResponse("Department not found: " + req.getDepartmentId(), 404);
            }

            List<Map<String, Object>> summary = new ArrayList<>();
            List<Map<String, Object>> skippedUsers = new ArrayList<>();
            for (Long userId : req.getUserIds()) {
                WebUser u = webUserFacade.find(userId);
                if (u == null || u.isRetired()) {
                    Map<String, Object> skipped = new HashMap<>();
                    skipped.put("userId", userId);
                    skipped.put("reason", u == null ? "not_found" : "retired");
                    skippedUsers.add(skipped);
                    continue;
                }

                // Determine target departments: fixed dept or all of the user's loggable departments
                List<Department> targetDepts = new ArrayList<>();
                if (fixedDept != null) {
                    targetDepts.add(fixedDept);
                } else {
                    List<WebUserDepartment> userDepts = webUserDepartmentFacade.findByJpql(
                            "select d from WebUserDepartment d where d.retired=false and d.webUser=:u",
                            Collections.singletonMap("u", u));
                    for (WebUserDepartment wud : userDepts) {
                        if (wud.getDepartment() != null) targetDepts.add(wud.getDepartment());
                    }
                }

                int added = 0;
                int skipped = 0;
                for (Department dept : targetDepts) {
                    for (Privileges p : privileges) {
                        Map<String, Object> check = new HashMap<>();
                        check.put("u", u);
                        check.put("p", p);
                        check.put("d", dept);
                        List<WebUserPrivilege> ex = webUserPrivilegeFacade.findByJpql(
                                "select wp from WebUserPrivilege wp where wp.retired=false and wp.webUser=:u and wp.privilege=:p and wp.department=:d",
                                check);
                        if (!ex.isEmpty()) {
                            skipped++;
                            continue;
                        }
                        WebUserPrivilege wp = new WebUserPrivilege();
                        wp.setWebUser(u);
                        wp.setPrivilege(p);
                        wp.setDepartment(dept);
                        wp.setCreater(apiUser);
                        wp.setCreatedAt(new Date());
                        webUserPrivilegeFacade.create(wp);
                        added++;
                    }
                }
                Map<String, Object> entry = new HashMap<>();
                entry.put("userId", u.getId());
                entry.put("userName", u.getName());
                entry.put("privilegesAdded", added);
                entry.put("privilegesSkipped", skipped);
                summary.add(entry);
            }
            Map<String, Object> result = new HashMap<>();
            result.put("processed", summary);
            result.put("skippedUsers", skippedUsers);
            return successResponse(result);
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
        }
    }

    // ── Role-template operations (issue #22023) ─────────────────────────────
    // Roles are admin-time templates; these endpoints stamp/reset user-level
    // records (privileges/icons/subscriptions/login page) from a role
    // template via UserRoleApplicationService — same engine used by the UI
    // and the AI assistant.

    /**
     * POST /api/users/{id}/role/reset
     * Body: {"roleId": optional long, "departmentIds": [long,...] required,
     * "aspects": optional ["PRIVILEGES","ICONS","SUBSCRIPTIONS","LOGIN_PAGE"] default ["PRIVILEGES"],
     * "updateUserRole": optional bool default true, "preview": optional bool default false}
     * roleId omitted/null uses the target user's own WebUser.role (400 if the user has no role).
     */
    @POST
    @Path("/{id}/role/reset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetUserRole(@PathParam("id") Long id, String body) {
        return applyRoleOperation(RoleOperation.RESET, id, body, false);
    }

    /**
     * POST /api/users/{id}/role/expand
     * Body: {"roleId": required long, "departmentIds": [long,...] required,
     * "aspects": optional default ["PRIVILEGES"], "preview": optional bool default false}
     */
    @POST
    @Path("/{id}/role/expand")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response expandUserRole(@PathParam("id") Long id, String body) {
        return applyRoleOperation(RoleOperation.EXPAND, id, body, true);
    }

    /**
     * POST /api/users/{id}/role/narrow
     * Body: {"roleId": required long, "departmentIds": [long,...] required,
     * "aspects": optional default ["PRIVILEGES"], "preview": optional bool default false}
     */
    @POST
    @Path("/{id}/role/narrow")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response narrowUserRole(@PathParam("id") Long id, String body) {
        return applyRoleOperation(RoleOperation.NARROW, id, body, true);
    }

    private Response applyRoleOperation(RoleOperation op, Long id, String body, boolean roleRequired) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            WebUser u = webUserFacade.find(id);
            if (u == null || u.isRetired()) return errorResponse("User not found", 404);

            Map<String, Object> req = parseJsonBody(body);
            WebUserRole role;
            Object roleIdObj = req.get("roleId");
            if (roleIdObj != null) {
                role = findRoleOrThrow(toLong(roleIdObj, "roleId"));
            } else if (roleRequired) {
                throw new ApiValidationException("roleId is required", 400);
            } else {
                role = u.getRole();
                if (role == null) {
                    return errorResponse("User has no default role; specify roleId explicitly", 400);
                }
            }

            List<Department> departments = resolveDepartments(extractLongList(req, "departmentIds", true));
            Set<RoleAspect> aspects = parseAspects(extractStringList(req, "aspects", Collections.singletonList("PRIVILEGES")));
            boolean updateUserRole = toBoolean(req.get("updateUserRole"), true);
            boolean preview = toBoolean(req.get("preview"), false);

            if (preview) {
                Map<RoleAspect, Long> counts = userRoleApplicationService.previewCounts(op, u, role, departments, aspects);
                return successResponse(previewResponse(op, u, role, departments, aspects, counts));
            }

            RoleApplicationResult result = userRoleApplicationService.apply(op, u, role, departments, aspects, updateUserRole, apiUser);
            if (!result.isSuccess()) return errorResponse(result.getErrorMessage(), 400);
            return successResponse(resultResponse(op, result));
        } catch (ApiValidationException e) {
            return errorResponse(e.getMessage(), e.getCode());
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
        }
    }

    /**
     * POST /api/users/bulk/role-operations
     * Body: {"action": "RESET"|"EXPAND"|"NARROW", "userIds": [long] optional,
     * "filter": {"roleId": long optional, "departmentId": long optional} optional,
     * "roleId": optional (target template role), "departmentIds": [long] required,
     * "aspects": optional default ["PRIVILEGES"], "updateUserRole": optional bool default true,
     * "preview": optional bool default false, "confirm": optional bool default false}
     * Explicit userIds wins over filter. Safety gate: preview=false and confirm=false is
     * rejected — callers must preview first, then repeat with confirm=true to apply.
     */
    @POST
    @Path("/bulk/role-operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bulkRoleOperations(String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);

            Map<String, Object> req = parseJsonBody(body);
            Object actionObj = req.get("action");
            if (!(actionObj instanceof String) || ((String) actionObj).trim().isEmpty()) {
                return errorResponse("action is required", 400);
            }
            RoleOperation op;
            try {
                op = RoleOperation.valueOf(((String) actionObj).trim());
            } catch (IllegalArgumentException e) {
                return errorResponse("Invalid action: " + actionObj + ". Valid values: RESET, EXPAND, NARROW", 400);
            }

            Long roleId = req.get("roleId") != null ? toLong(req.get("roleId"), "roleId") : null;
            WebUserRole role = roleId != null ? findRoleOrThrow(roleId) : null;

            List<Department> departments = resolveDepartments(extractLongList(req, "departmentIds", true));
            Set<RoleAspect> aspects = parseAspects(extractStringList(req, "aspects", Collections.singletonList("PRIVILEGES")));
            boolean updateUserRole = toBoolean(req.get("updateUserRole"), true);
            boolean preview = toBoolean(req.get("preview"), false);
            boolean confirm = toBoolean(req.get("confirm"), false);

            // Resolve target users: explicit userIds wins over filter.
            List<Long> userIds = extractLongList(req, "userIds", false);
            List<WebUser> users;
            List<Long> missingUserIds = new ArrayList<>();
            if (userIds != null && !userIds.isEmpty()) {
                users = new ArrayList<>();
                for (Long uid : userIds) {
                    WebUser candidate = webUserFacade.find(uid);
                    if (candidate == null || candidate.isRetired()) {
                        missingUserIds.add(uid);
                    } else {
                        users.add(candidate);
                    }
                }
            } else {
                users = resolveUsersByFilter(req.get("filter"));
            }

            final int MAX_BULK_USERS = 500;
            if (users.size() > MAX_BULK_USERS) {
                return errorResponse("Too many users resolved (" + users.size() + "). Narrow the filter; max allowed: " + MAX_BULK_USERS, 400);
            }

            if (!preview && !confirm) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("status", "error");
                m.put("code", 400);
                m.put("message", "Safety gate: call again with preview=true to review impact, then repeat with confirm=true to apply.");
                m.put("resolvedUserCount", users.size());
                if (!missingUserIds.isEmpty()) m.put("missingUserIds", missingUserIds);
                return Response.status(400).entity(gson.toJson(m)).build();
            }

            if (preview) {
                Map<RoleAspect, Long> totals = new EnumMap<>(RoleAspect.class);
                for (RoleAspect a : aspects) totals.put(a, 0L);
                int previewedCount = Math.min(users.size(), 200);
                for (int i = 0; i < previewedCount; i++) {
                    WebUser targetUser = users.get(i);
                    WebUserRole effectiveRole = role != null ? role : targetUser.getRole();
                    if (effectiveRole == null) continue;
                    Map<RoleAspect, Long> counts = userRoleApplicationService.previewCounts(op, targetUser, effectiveRole, departments, aspects);
                    for (RoleAspect a : aspects) {
                        totals.merge(a, counts.getOrDefault(a, 0L), Long::sum);
                    }
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("preview", true);
                m.put("action", op.name());
                m.put("userCount", users.size());
                m.put("previewedUserCount", previewedCount);
                Map<String, Long> aspectTotals = new LinkedHashMap<>();
                for (RoleAspect a : aspects) aspectTotals.put(a.name(), totals.get(a));
                m.put("previewCounts", aspectTotals);
                if (!missingUserIds.isEmpty()) m.put("missingUserIds", missingUserIds);
                return successResponse(m);
            }

            // confirm == true: apply for real
            List<RoleApplicationResult> results = userRoleApplicationService.applyBulk(op, users, role, departments, aspects, updateUserRole, apiUser);
            List<Map<String, Object>> perUser = new ArrayList<>();
            int succeeded = 0;
            int failed = 0;
            int totalAdded = 0;
            int totalRetired = 0;
            for (RoleApplicationResult r : results) {
                perUser.add(resultResponse(op, r));
                if (r.isSuccess()) {
                    succeeded++;
                    totalAdded += r.getTotalAdded();
                    totalRetired += r.getTotalRetired();
                } else {
                    failed++;
                }
            }
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("action", op.name());
            summary.put("usersProcessed", results.size());
            summary.put("succeeded", succeeded);
            summary.put("failed", failed);
            summary.put("totalAdded", totalAdded);
            summary.put("totalRetired", totalRetired);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("preview", false);
            out.put("summary", summary);
            out.put("results", perUser);
            if (!missingUserIds.isEmpty()) out.put("missingUserIds", missingUserIds);
            return successResponse(out);
        } catch (ApiValidationException e) {
            return errorResponse(e.getMessage(), e.getCode());
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
        }
    }

    @SuppressWarnings("unchecked")
    private List<WebUser> resolveUsersByFilter(Object filterObj) {
        Map<String, Object> filter = (filterObj instanceof Map) ? (Map<String, Object>) filterObj : null;
        if (filter == null || filter.isEmpty()) {
            throw new ApiValidationException("Either userIds or filter is required", 400);
        }
        Long filterRoleId = filter.get("roleId") != null ? toLong(filter.get("roleId"), "filter.roleId") : null;
        Long filterDeptId = filter.get("departmentId") != null ? toLong(filter.get("departmentId"), "filter.departmentId") : null;
        WebUserRole filterRole = filterRoleId != null ? findRoleOrThrow(filterRoleId) : null;
        Department filterDept = null;
        if (filterDeptId != null) {
            filterDept = departmentFacade.find(filterDeptId);
            if (filterDept == null) {
                throw new ApiValidationException("filter.departmentId not found: " + filterDeptId, 404);
            }
        }
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select distinct w from WebUser w");
        if (filterDept != null) {
            jpql.append(" join WebUserDepartment wud on wud.webUser=w");
        }
        jpql.append(" where w.retired=false");
        if (filterRole != null) {
            jpql.append(" and w.role=:role");
            params.put("role", filterRole);
        }
        if (filterDept != null) {
            jpql.append(" and wud.retired=false and wud.department=:dept");
            params.put("dept", filterDept);
        }
        jpql.append(" order by w.name");
        return webUserFacade.findByJpql(jpql.toString(), params);
    }

    /**
     * GET /api/users/roles
     * Active roles with template summary: id, name, description, template login page,
     * and counts of active role-level privileges / template icons / template subscriptions.
     */
    @GET
    @Path("/roles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listUserRolesWithTemplateCounts() {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        List<WebUserRole> roles = webUserRoleFacade.findByJpql("select r from WebUserRole r where r.retired=false order by r.name");
        List<Map<String, Object>> out = new ArrayList<>();
        for (WebUserRole role : roles) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", role.getId());
            m.put("name", role.getName());
            m.put("description", role.getDescription());
            m.put("loginPage", role.getLoginPage() != null ? role.getLoginPage().name() : null);
            Map<String, Object> rp = kv("role", role);
            long privilegeCount = webUserRolePrivilegeFacade.findLongByJpql(
                    "select count(p) from WebUserRolePrivilege p where p.retired=false and p.webUserRole=:role", rp);
            long iconCount = userIconFacade.findLongByJpql(
                    "select count(u) from UserIcon u where u.retired=false and u.webUserRole=:role and u.webUser is null", rp);
            long subscriptionCount = triggerSubscriptionFacade.findLongByJpql(
                    "select count(t) from TriggerSubscription t where t.retired=false and t.webUserRole=:role and t.webUser is null", rp);
            m.put("privilegeCount", privilegeCount);
            m.put("iconCount", iconCount);
            m.put("subscriptionCount", subscriptionCount);
            out.add(m);
        }
        return successResponse(out);
    }

    /**
     * PUT /api/users/{id}/login-page
     * Body: {"departmentId": required long, "loginPage": required string (LoginPage enum name)}
     * Upserts the active WebUserDefaultLoginPage row for user+department: retires the old
     * active row (if any) and creates a new one.
     */
    @PUT
    @Path("/{id}/login-page")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setUserLoginPage(@PathParam("id") Long id, String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
            WebUser u = webUserFacade.find(id);
            if (u == null || u.isRetired()) return errorResponse("User not found", 404);

            Map<String, Object> req = parseJsonBody(body);
            Long departmentId = req.get("departmentId") != null ? toLong(req.get("departmentId"), "departmentId") : null;
            if (departmentId == null) return errorResponse("departmentId is required", 400);
            Department dept = departmentFacade.find(departmentId);
            if (dept == null) return errorResponse("Department not found: " + departmentId, 404);

            Object loginPageObj = req.get("loginPage");
            if (!(loginPageObj instanceof String) || ((String) loginPageObj).trim().isEmpty()) {
                return errorResponse("loginPage is required", 400);
            }
            LoginPage loginPage;
            try {
                loginPage = LoginPage.valueOf(((String) loginPageObj).trim());
            } catch (IllegalArgumentException e) {
                return errorResponse("Invalid loginPage: " + loginPageObj, 400);
            }

            Date now = new Date();
            WebUserDefaultLoginPage active = webUserDefaultLoginPageFacade.findFirstByJpql(
                    "select w from WebUserDefaultLoginPage w where w.retired=false and w.webUser=:u and w.department=:d order by w.id desc",
                    kv("u", u, "d", dept));
            if (active != null) {
                active.setRetired(true);
                active.setRetirer(apiUser);
                active.setRetiredAt(now);
                webUserDefaultLoginPageFacade.edit(active);
            }
            WebUserDefaultLoginPage fresh = new WebUserDefaultLoginPage();
            fresh.setWebUser(u);
            fresh.setDepartment(dept);
            fresh.setLoginPage(loginPage);
            fresh.setCreater(apiUser);
            fresh.setCreatedAt(now);
            webUserDefaultLoginPageFacade.create(fresh);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", fresh.getId());
            m.put("userId", u.getId());
            m.put("departmentId", dept.getId());
            m.put("loginPage", fresh.getLoginPage().name());
            return successResponse(m);
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
        }
    }

    /**
     * DELETE /api/users/{id}/login-page/{departmentId}
     * Retires the active WebUserDefaultLoginPage row for this user+department, if any.
     */
    @DELETE
    @Path("/{id}/login-page/{departmentId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUserLoginPage(@PathParam("id") Long id, @PathParam("departmentId") Long departmentId) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
        WebUser u = webUserFacade.find(id);
        if (u == null || u.isRetired()) return errorResponse("User not found", 404);
        Department dept = departmentFacade.find(departmentId);
        if (dept == null) return errorResponse("Department not found: " + departmentId, 404);

        WebUserDefaultLoginPage active = webUserDefaultLoginPageFacade.findFirstByJpql(
                "select w from WebUserDefaultLoginPage w where w.retired=false and w.webUser=:u and w.department=:d order by w.id desc",
                kv("u", u, "d", dept));
        if (active == null) return errorResponse("No active login page override for this user/department", 404);
        active.setRetired(true);
        active.setRetirer(apiUser);
        active.setRetiredAt(new Date());
        webUserDefaultLoginPageFacade.edit(active);
        return successResponse("Login page override retired");
    }

    // ── Role-template helpers ────────────────────────────────────────────────

    private Map<String, Object> previewResponse(RoleOperation op, WebUser user, WebUserRole role, List<Department> departments,
            Set<RoleAspect> aspects, Map<RoleAspect, Long> counts) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("preview", true);
        m.put("operation", op.name());
        m.put("userId", user.getId());
        m.put("roleId", role.getId());
        m.put("roleName", role.getName());
        m.put("departmentIds", departmentIdsOf(departments));
        Map<String, Long> aspectCounts = new LinkedHashMap<>();
        for (RoleAspect a : aspects) aspectCounts.put(a.name(), counts.getOrDefault(a, 0L));
        m.put("previewCounts", aspectCounts);
        return m;
    }

    private Map<String, Object> resultResponse(RoleOperation op, RoleApplicationResult result) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("preview", false);
        m.put("operation", op.name());
        m.put("userId", result.getUser() != null ? result.getUser().getId() : null);
        m.put("userName", result.getUser() != null ? result.getUser().getName() : null);
        m.put("success", result.isSuccess());
        if (!result.isSuccess()) {
            m.put("errorMessage", result.getErrorMessage());
        }
        Map<String, Integer> added = new LinkedHashMap<>();
        for (Map.Entry<RoleAspect, Integer> e : result.getAdded().entrySet()) added.put(e.getKey().name(), e.getValue());
        Map<String, Integer> retired = new LinkedHashMap<>();
        for (Map.Entry<RoleAspect, Integer> e : result.getRetired().entrySet()) retired.put(e.getKey().name(), e.getValue());
        m.put("added", added);
        m.put("retired", retired);
        m.put("totalAdded", result.getTotalAdded());
        m.put("totalRetired", result.getTotalRetired());
        return m;
    }

    private List<Long> departmentIdsOf(List<Department> departments) {
        List<Long> ids = new ArrayList<>();
        for (Department d : departments) ids.add(d.getId());
        return ids;
    }

    private WebUserRole findRoleOrThrow(Long roleId) {
        WebUserRole role = roleId != null ? webUserRoleFacade.find(roleId) : null;
        if (role == null || role.isRetired()) {
            throw new ApiValidationException("Role not found: " + roleId, 404);
        }
        return role;
    }

    private List<Department> resolveDepartments(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ApiValidationException("departmentIds is required and must be non-empty", 400);
        }
        List<Department> depts = new ArrayList<>();
        List<Long> invalid = new ArrayList<>();
        for (Long deptId : ids) {
            Department d = departmentFacade.find(deptId);
            if (d == null) {
                invalid.add(deptId);
            } else {
                depts.add(d);
            }
        }
        if (!invalid.isEmpty()) {
            throw new ApiValidationException("Department(s) not found: " + invalid, 404);
        }
        return depts;
    }

    private Set<RoleAspect> parseAspects(List<String> names) {
        Set<RoleAspect> set = new LinkedHashSet<>();
        for (String n : names) {
            try {
                set.add(RoleAspect.valueOf(n == null ? null : n.trim()));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new ApiValidationException(
                        "Invalid aspect: " + n + ". Valid values: PRIVILEGES, ICONS, SUBSCRIPTIONS, LOGIN_PAGE", 400);
            }
        }
        if (set.isEmpty()) {
            throw new ApiValidationException("aspects must contain at least one value", 400);
        }
        return set;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> parsed = gson.fromJson(body, Map.class);
        return parsed != null ? parsed : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Long> extractLongList(Map<String, Object> req, String key, boolean required) {
        Object obj = req.get(key);
        if (obj == null) {
            if (required) throw new ApiValidationException(key + " is required", 400);
            return null;
        }
        if (!(obj instanceof List)) {
            throw new ApiValidationException(key + " must be an array", 400);
        }
        List<Long> out = new ArrayList<>();
        for (Object item : (List<Object>) obj) {
            if (item instanceof Number) {
                out.add(((Number) item).longValue());
            } else {
                throw new ApiValidationException(key + " must be an array of integers, got: " + item, 400);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractStringList(Map<String, Object> req, String key, List<String> defaultVal) {
        Object obj = req.get(key);
        if (obj == null) return defaultVal;
        if (!(obj instanceof List)) {
            throw new ApiValidationException(key + " must be an array", 400);
        }
        List<String> out = new ArrayList<>();
        for (Object item : (List<Object>) obj) {
            if (item instanceof String) {
                out.add((String) item);
            } else {
                throw new ApiValidationException(key + " must be an array of strings, got: " + item, 400);
            }
        }
        return out;
    }

    private Long toLong(Object obj, String field) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        if (obj instanceof String) {
            try {
                return Long.parseLong(((String) obj).trim());
            } catch (NumberFormatException e) {
                throw new ApiValidationException(field + " must be numeric", 400);
            }
        }
        throw new ApiValidationException(field + " must be numeric", 400);
    }

    private boolean toBoolean(Object obj, boolean defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof Boolean) return (Boolean) obj;
        if (obj instanceof String) return Boolean.parseBoolean((String) obj);
        return defaultVal;
    }

    private Map<String, Object> kv(Object... pairs) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((String) pairs[i], pairs[i + 1]);
        }
        return m;
    }

    /** Lightweight validation exception carrying the intended HTTP status code (400/404). */
    private static class ApiValidationException extends RuntimeException {
        private final int code;

        ApiValidationException(String message, int code) {
            super(message);
            this.code = code;
        }

        int getCode() {
            return code;
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
        if (req.getLoginPage() != null) {
            String loginPage = req.getLoginPage().trim();
            if (loginPage.isEmpty()) {
                u.setLoginPage(null);
            } else {
                try {
                    u.setLoginPage(LoginPage.valueOf(loginPage));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid loginPage: " + req.getLoginPage());
                }
            }
        }
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
        m.put("loginPage", u.getLoginPage() != null ? u.getLoginPage().name() : null);
        m.put("institutionId", u.getInstitution() != null ? u.getInstitution().getId() : null);
        m.put("departmentId", u.getDepartment() != null ? u.getDepartment().getId() : null);
        m.put("siteId", u.getSite() != null ? u.getSite().getId() : null);
        m.put("roleId", u.getRole() != null ? u.getRole().getId() : null);
        m.put("personName", u.getWebUserPerson() != null ? u.getWebUserPerson().getName() : null);
        m.put("needToResetPassword", u.isNeedToResetPassword());
        return m;
    }

    private String value(String key) { return uriInfo.getQueryParameters().getFirst(key); }

    private int parseInt(String v, int d) { try { return v == null ? d : Integer.parseInt(v); } catch (Exception e) { return d; } }

    private Long parseLong(String v, Long d) { try { return v == null ? d : Long.parseLong(v); } catch (Exception e) { return d; } }

    private Date parseDateParam(String v) {
        if (v == null || v.trim().isEmpty()) return null;
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(v.trim());
        } catch (java.text.ParseException e) {
            return null;
        }
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

    /** Build a lowercase→first-occurrence-name map so case-insensitive duplicates resolve consistently. */
    private static Map<String, String> buildCanonicalPrivilegeMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Privileges p : Privileges.values()) {
            map.putIfAbsent(p.name().toLowerCase(), p.name());
        }
        return map;
    }

    /** Return the canonical API name for a privilege, normalising case-insensitive duplicates. */
    private String canonicalPrivilegeName(Privileges p) {
        return buildCanonicalPrivilegeMap().getOrDefault(p.name().toLowerCase(), p.name());
    }
}
