package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.Privileges;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.facade.WebUserPrivilegeFacade;
import com.divudi.core.facade.WebUserRolePrivilegeFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * REST API for Staff CRUD operations.
 * Auth: Finance header (same as UserManagementApi).
 */
@Path("staff")
@RequestScoped
public class StaffApi {

    @Context
    private HttpServletRequest requestContext;
    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private StaffFacade staffFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;
    @EJB
    private WebUserRolePrivilegeFacade webUserRolePrivilegeFacade;

    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    // -------------------------------------------------------------------------
    // GET /api/staff?query=&departmentId=&size=
    // -------------------------------------------------------------------------
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listStaff() {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);

            String q = value("query");
            int size = Math.min(Math.max(parseInt(value("size"), 50), 1), 200);
            String departmentIdStr = value("departmentId");

            Map<String, Object> params = new HashMap<>();
            String jpql;

            if (departmentIdStr != null && !departmentIdStr.trim().isEmpty()) {
                Long deptId = parseLong(departmentIdStr);
                if (deptId == null) return errorResponse("Invalid departmentId", 400);
                Department dept = departmentFacade.find(deptId);
                if (dept == null) return errorResponse("Department not found", 404);
                jpql = "select s from Staff s where s.retired=false and s.department=:dept";
                params.put("dept", dept);
                if (q != null && !q.trim().isEmpty()) {
                    jpql += " and (upper(s.person.name) like :q or upper(s.code) like :q)";
                    params.put("q", "%" + q.trim().toUpperCase() + "%");
                }
            } else {
                jpql = "select s from Staff s where s.retired=false";
                if (q != null && !q.trim().isEmpty()) {
                    jpql += " and (upper(s.person.name) like :q or upper(s.code) like :q)";
                    params.put("q", "%" + q.trim().toUpperCase() + "%");
                }
            }
            jpql += " order by s.person.name";

            List<Staff> staffList = staffFacade.findByJpql(jpql, params, size);
            List<Map<String, Object>> out = new ArrayList<>();
            for (Staff s : staffList) {
                out.add(toStaffMap(s, false));
            }
            return successResponse(out);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/staff/{id}
    // -------------------------------------------------------------------------
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStaff(@PathParam("id") Long id) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        Staff s = staffFacade.find(id);
        if (s == null || s.isRetired()) return errorResponse("Staff not found", 404);
        return successResponse(toStaffMap(s, true));
    }

    // -------------------------------------------------------------------------
    // POST /api/staff
    // Body: {name, code, designation (string label), departmentId, institutionId}
    // -------------------------------------------------------------------------
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createStaff(String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);

            Map req = gson.fromJson(body, Map.class);
            if (req == null) return errorResponse("Request body is required", 400);

            String name = req.containsKey("name") ? (String) req.get("name") : null;
            if (name == null || name.trim().isEmpty()) return errorResponse("name is required", 400);
            name = name.trim();

            String code = req.containsKey("code") ? (String) req.get("code") : null;
            String designationLabel = req.containsKey("designation") ? (String) req.get("designation") : null;

            Long departmentId = parseDoubleAsLong(req.get("departmentId"));
            Long institutionId = parseDoubleAsLong(req.get("institutionId"));

            Department dept = (departmentId != null) ? departmentFacade.find(departmentId) : null;
            if (departmentId != null && dept == null) return errorResponse("Department not found", 404);

            Institution inst = (institutionId != null) ? institutionFacade.find(institutionId) : null;
            if (institutionId != null && inst == null) return errorResponse("Institution not found", 404);

            // Create a Person for this staff member
            Person person = new Person();
            person.setName(name);
            person.setCreatedAt(new Date());
            person.setCreater(apiUser);
            personFacade.create(person);

            // Create the Staff record
            Staff staff = new Staff();
            staff.setPerson(person);
            if (code != null && !code.trim().isEmpty()) staff.setCode(code.trim());
            if (designationLabel != null && !designationLabel.trim().isEmpty()) {
                staff.setDescription(designationLabel.trim());
            }
            if (dept != null) staff.setDepartment(dept);
            if (inst != null) staff.setInstitution(inst);
            staff.setCreatedAt(new Date());
            staff.setCreater(apiUser);
            staffFacade.create(staff);

            return successResponse(toStaffMap(staff, true));
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/staff/{id}
    // Partial update — only supplied fields change (name, code, designation)
    // -------------------------------------------------------------------------
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateStaff(@PathParam("id") Long id, String body) {
        try {
            WebUser apiUser = validateApiUser();
            if (apiUser == null) return errorResponse("Not a valid key", 401);
            if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);

            Staff staff = staffFacade.find(id);
            if (staff == null || staff.isRetired()) return errorResponse("Staff not found", 404);

            Map req = gson.fromJson(body, Map.class);
            if (req == null) return errorResponse("Request body is required", 400);

            if (req.containsKey("name")) {
                String name = (String) req.get("name");
                if (name == null || name.trim().isEmpty()) return errorResponse("name cannot be empty", 400);
                if (staff.getPerson() != null) {
                    staff.getPerson().setName(name.trim());
                    personFacade.edit(staff.getPerson());
                }
            }
            if (req.containsKey("code")) {
                String code = (String) req.get("code");
                staff.setCode(code != null ? code.trim() : null);
            }
            if (req.containsKey("designation")) {
                String designation = (String) req.get("designation");
                staff.setDescription(designation != null ? designation.trim() : null);
            }
            if (req.containsKey("departmentId")) {
                Long departmentId = parseDoubleAsLong(req.get("departmentId"));
                if (departmentId != null) {
                    Department dept = departmentFacade.find(departmentId);
                    if (dept == null) return errorResponse("Department not found", 404);
                    staff.setDepartment(dept);
                } else {
                    staff.setDepartment(null);
                }
            }
            if (req.containsKey("institutionId")) {
                Long institutionId = parseDoubleAsLong(req.get("institutionId"));
                if (institutionId != null) {
                    Institution inst = institutionFacade.find(institutionId);
                    if (inst == null) return errorResponse("Institution not found", 404);
                    staff.setInstitution(inst);
                } else {
                    staff.setInstitution(null);
                }
            }

            staffFacade.edit(staff);
            return successResponse(toStaffMap(staff, true));
        } catch (JsonSyntaxException e) {
            return errorResponse("Invalid JSON format", 400);
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/staff/{id}?retireComments=reason  — soft-retire
    // -------------------------------------------------------------------------
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireStaff(@PathParam("id") Long id) {
        WebUser apiUser = validateApiUser();
        if (apiUser == null) return errorResponse("Not a valid key", 401);
        if (!isAdmin(apiUser)) return errorResponse("Insufficient privileges", 403);
        Staff staff = staffFacade.find(id);
        if (staff == null || staff.isRetired()) return errorResponse("Staff not found", 404);
        staff.setRetired(true);
        staff.setRetirer(apiUser);
        staff.setRetiredAt(new Date());
        staff.setRetireComments(value("retireComments"));
        staffFacade.edit(staff);
        return successResponse(toStaffMap(staff, false));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<String, Object> toStaffMap(Staff s, boolean full) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("code", s.getCodeRaw());
        m.put("designation", s.getDescription());
        m.put("retired", s.isRetired());
        m.put("personId", s.getPerson() != null ? s.getPerson().getId() : null);
        m.put("name", s.getPerson() != null ? s.getPerson().getName() : null);
        m.put("departmentId", s.getDepartment() != null ? s.getDepartment().getId() : null);
        m.put("departmentName", s.getDepartment() != null ? s.getDepartment().getName() : null);
        m.put("institutionId", s.getInstitution() != null ? s.getInstitution().getId() : null);
        if (full) {
            m.put("createdAt", s.getCreatedAt());
            m.put("retiredAt", s.getRetiredAt());
            m.put("retireComments", s.getRetireComments());
        }
        return m;
    }

    private String value(String key) {
        return uriInfo.getQueryParameters().getFirst(key);
    }

    private int parseInt(String v, int d) {
        try { return v == null ? d : Integer.parseInt(v.trim()); } catch (Exception e) { return d; }
    }

    private Long parseLong(String v) {
        try { return v == null ? null : Long.parseLong(v.trim()); } catch (Exception e) { return null; }
    }

    private Long parseDoubleAsLong(Object v) {
        if (v == null) return null;
        try {
            if (v instanceof Number) return ((Number) v).longValue();
            return Long.parseLong(v.toString().trim());
        } catch (Exception e) { return null; }
    }

    private WebUser validateApiUser() {
        String key = requestContext.getHeader("Finance");
        if (key == null || key.trim().isEmpty()) return null;
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null || apiKey.getWebUser() == null
                || apiKey.getDateOfExpiary() == null
                || apiKey.getDateOfExpiary().before(new Date())) return null;
        WebUser u = apiKey.getWebUser();
        if (u.isRetired() || !u.isActivated()) return null;
        return u;
    }

    private boolean isAdmin(WebUser user) {
        Map<String, Object> m = new HashMap<>();
        m.put("u", user);
        m.put("p", Privileges.Admin);
        if (!webUserPrivilegeFacade.findByJpql(
                "select wp from WebUserPrivilege wp where wp.retired=false and wp.webUser=:u and wp.privilege=:p", m).isEmpty()) {
            return true;
        }
        if (user.getRole() == null) return false;
        Map<String, Object> r = new HashMap<>();
        r.put("r", user.getRole());
        r.put("p", Privileges.Admin);
        return !webUserRolePrivilegeFacade.findByJpql(
                "select rp from WebUserRolePrivilege rp where rp.retired=false and rp.webUserRole=:r and rp.privilege=:p", r).isEmpty();
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
