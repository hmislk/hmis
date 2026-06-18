package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.PharmaceuticalItem;
import com.divudi.core.facade.AmpFacade;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.PharmaceuticalItemFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("pharmacy/items")
@RequestScoped
public class PharmacyItemApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private PharmaceuticalItemFacade pharmaceuticalItemFacade;
    @EJB
    private CategoryFacade categoryFacade;
    @EJB
    private AmpFacade ampFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private DepartmentFacade departmentFacade;

    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);

            String query = param("query");
            int size = intParam("size", 50, 1, 200);
            String institutionId = param("institutionId");
            String departmentId = param("departmentId");

            Map<String, Object> params = new HashMap<>();
            StringBuilder jpql = new StringBuilder("select p from PharmaceuticalItem p where p.retired=false and TYPE(p) = PharmaceuticalItem");
            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and (upper(p.name) like :q or upper(p.code) like :q)");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }
            if (institutionId != null && !institutionId.trim().isEmpty()) {
                Long parsedInstitutionId = parseLong(institutionId, null);
                if (parsedInstitutionId == null) return errorResponse("Invalid institutionId", 400);
                Institution ins = institutionFacade.find(parsedInstitutionId);
                if (ins == null) return errorResponse("Institution not found: " + institutionId, 404);
                jpql.append(" and p.institution=:ins");
                params.put("ins", ins);
            }
            if (departmentId != null && !departmentId.trim().isEmpty()) {
                Long parsedDepartmentId = parseLong(departmentId, null);
                if (parsedDepartmentId == null) return errorResponse("Invalid departmentId", 400);
                Department dept = departmentFacade.find(parsedDepartmentId);
                if (dept == null) return errorResponse("Department not found: " + departmentId, 404);
                jpql.append(" and p.department=:dept");
                params.put("dept", dept);
            }
            jpql.append(" order by p.name");

            List<PharmaceuticalItem> items = pharmaceuticalItemFacade.findByJpql(jpql.toString(), params, size);
            return successResponse(toDtoList(items));
        } catch (Exception e) {
            return errorResponse("Internal server error", 500);
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("id") Long id) {
        WebUser user = validateApiKey(requestContext.getHeader("Finance"));
        if (user == null) return errorResponse("Not a valid key", 401);
        PharmaceuticalItem item = pharmaceuticalItemFacade.find(id);
        if (item == null || item.isRetired() || item.getClass() != PharmaceuticalItem.class) return errorResponse("Pharmacy item not found", 404);
        return successResponse(toDto(item));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            PharmacyItemRequest req = gson.fromJson(body, PharmacyItemRequest.class);
            if (req == null) return errorResponse("Request body is required", 400);
            String name = req.getName() != null ? req.getName().trim() : null;
            if (name == null || name.isEmpty()) return errorResponse("name is required", 400);

            ResolvedRefs refs = resolveRefs(req);
            PharmaceuticalItem existing = findDuplicate(name, refs.institution, refs.department, null);
            if (existing != null) {
                Map<String, Object> already = new LinkedHashMap<>();
                already.put("status", "already_exists");
                already.put("id", existing.getId());
                already.put("name", existing.getName());
                return Response.status(200).entity(gson.toJson(already)).build();
            }

            PharmaceuticalItem item = new PharmaceuticalItem();
            item.setName(name);
            item.setCreatedAt(new Date());
            item.setCreater(user);
            applyRequest(item, req, refs, false);
            pharmaceuticalItemFacade.create(item);
            return Response.status(201).entity(gson.toJson(successData(toDto(item), 201))).build();
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
    public Response update(@PathParam("id") Long id, String body) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) return errorResponse("Not a valid key", 401);
            PharmaceuticalItem item = pharmaceuticalItemFacade.find(id);
            if (item == null || item.isRetired() || item.getClass() != PharmaceuticalItem.class) return errorResponse("Pharmacy item not found", 404);
            PharmacyItemRequest req = gson.fromJson(body, PharmacyItemRequest.class);
            if (req == null) return errorResponse("Request body is required", 400);

            ResolvedRefs refs = resolveRefs(req);
            String name = req.getName() != null ? req.getName().trim() : item.getName();
            if (name == null || name.trim().isEmpty()) return errorResponse("name cannot be empty", 400);
            PharmaceuticalItem existing = findDuplicate(name, refs.institution != null ? refs.institution : item.getInstitution(),
                    refs.department != null ? refs.department : item.getDepartment(), id);
            if (existing != null) return errorResponse("Pharmacy item name already exists in this institution/department scope", 400);

            item.setName(name);
            item.setEditedAt(new Date());
            item.setEditer(user);
            applyRequest(item, req, refs, true);
            pharmaceuticalItemFacade.edit(item);
            return successResponse(toDto(item));
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
    public Response retire(@PathParam("id") Long id) {
        WebUser user = validateApiKey(requestContext.getHeader("Finance"));
        if (user == null) return errorResponse("Not a valid key", 401);
        PharmaceuticalItem item = pharmaceuticalItemFacade.find(id);
        if (item == null || item.isRetired() || item.getClass() != PharmaceuticalItem.class) return errorResponse("Pharmacy item not found", 404);
        item.setRetired(true);
        item.setRetiredAt(new Date());
        item.setRetirer(user);
        item.setRetireComments(param("retireComments"));
        pharmaceuticalItemFacade.edit(item);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", item.getId());
        payload.put("retired", true);
        return successResponse(payload);
    }

    private void applyRequest(PharmaceuticalItem item, PharmacyItemRequest req, ResolvedRefs refs, boolean update) {
        if (!update || req.getCode() != null) item.setCode(req.getCode());
        if (!update || req.getCategoryId() != null) item.setCategory(refs.category);
        if (!update || req.getDosageFormId() != null) item.setDosageForm(refs.dosageForm);
        if (!update || req.getAmpId() != null) item.setAmp(refs.amp);
        if (!update || req.getInstitutionId() != null) item.setInstitution(refs.institution);
        if (!update || req.getDepartmentId() != null) item.setDepartment(refs.department);
        if (req.getRetailRate() != null) item.setTotal(req.getRetailRate());
        if (req.getAllowFractions() != null) item.setAllowFractions(req.getAllowFractions());
        if (req.getDiscountAllowed() != null) item.setDiscountAllowed(req.getDiscountAllowed());
    }

    private ResolvedRefs resolveRefs(PharmacyItemRequest req) {
        ResolvedRefs refs = new ResolvedRefs();
        if (req.getCategoryId() != null) {
            refs.category = categoryFacade.find(req.getCategoryId());
            if (refs.category == null) throw new IllegalArgumentException("Category not found: " + req.getCategoryId());
        }
        if (req.getDosageFormId() != null) {
            refs.dosageForm = categoryFacade.find(req.getDosageFormId());
            if (refs.dosageForm == null) throw new IllegalArgumentException("Dosage form not found: " + req.getDosageFormId());
        }
        if (req.getAmpId() != null) {
            refs.amp = ampFacade.find(req.getAmpId());
            if (refs.amp == null) throw new IllegalArgumentException("AMP not found: " + req.getAmpId());
        }
        if (req.getInstitutionId() != null) {
            refs.institution = institutionFacade.find(req.getInstitutionId());
            if (refs.institution == null) throw new IllegalArgumentException("Institution not found: " + req.getInstitutionId());
        }
        if (req.getDepartmentId() != null) {
            refs.department = departmentFacade.find(req.getDepartmentId());
            if (refs.department == null) throw new IllegalArgumentException("Department not found: " + req.getDepartmentId());
        }
        return refs;
    }

    private PharmaceuticalItem findDuplicate(String name, Institution institution, Department department, Long excludeId) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", name.trim().toUpperCase());
        params.put("institution", institution);
        params.put("department", department);
        String jpql = "select p from PharmaceuticalItem p where p.retired=false and TYPE(p) = PharmaceuticalItem and upper(p.name)=:name "
                + "and ((:institution is null and p.institution is null) or p.institution=:institution) "
                + "and ((:department is null and p.department is null) or p.department=:department)";
        if (excludeId != null) {
            jpql += " and p.id<>:excludeId";
            params.put("excludeId", excludeId);
        }
        return pharmaceuticalItemFacade.findFirstByJpql(jpql, params);
    }

    private List<Map<String, Object>> toDtoList(List<PharmaceuticalItem> items) {
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        if (items != null) {
            for (PharmaceuticalItem item : items) {
                out.add(toDto(item));
            }
        }
        return out;
    }

    private Map<String, Object> toDto(PharmaceuticalItem item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", item.getId());
        row.put("name", item.getName());
        row.put("code", item.getCode());
        row.put("categoryId", item.getCategory() != null ? item.getCategory().getId() : null);
        row.put("categoryName", item.getCategory() != null ? item.getCategory().getName() : null);
        row.put("dosageFormId", item.getDosageForm() != null ? item.getDosageForm().getId() : null);
        row.put("dosageFormName", item.getDosageForm() != null ? item.getDosageForm().getName() : null);
        row.put("ampId", item.getAmp() != null ? item.getAmp().getId() : null);
        row.put("ampName", item.getAmp() != null ? item.getAmp().getName() : null);
        row.put("institutionId", item.getInstitution() != null ? item.getInstitution().getId() : null);
        row.put("institutionName", item.getInstitution() != null ? item.getInstitution().getName() : null);
        row.put("departmentId", item.getDepartment() != null ? item.getDepartment().getId() : null);
        row.put("departmentName", item.getDepartment() != null ? item.getDepartment().getName() : null);
        row.put("retailRate", item.getTotal());
        row.put("allowFractions", item.isAllowFractions());
        row.put("discountAllowed", item.getDiscountAllowed());
        row.put("retired", item.isRetired());
        row.put("inactive", item.isInactive());
        return row;
    }

    private String param(String name) {
        return uriInfo.getQueryParameters().getFirst(name);
    }

    private int intParam(String name, int defaultValue, int min, int max) {
        String v = param(name);
        if (v == null || v.trim().isEmpty()) return defaultValue;
        try {
            return Math.min(Math.max(Integer.parseInt(v.trim()), min), max);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Long parseLong(String value, Long defaultValue) {
        try {
            return value == null ? defaultValue : Long.parseLong(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null || apiKey.isRetired()) return null;
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) return null;
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) return null;
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
        return Response.status(200).entity(gson.toJson(successData(data))).build();
    }

    private Map<String, Object> successData(Object data) {
        return successData(data, 200);
    }

    private Map<String, Object> successData(Object data, int code) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("code", code);
        response.put("data", data);
        return response;
    }

    private static class ResolvedRefs {
        private Category category;
        private Category dosageForm;
        private Amp amp;
        private Institution institution;
        private Department department;
    }

    private static class PharmacyItemRequest {
        private String name;
        private String code;
        private Long categoryId;
        private Long dosageFormId;
        private Long ampId;
        private Long institutionId;
        private Long departmentId;
        private Double retailRate;
        private Boolean allowFractions;
        private Boolean discountAllowed;

        public String getName() { return name; }
        public String getCode() { return code; }
        public Long getCategoryId() { return categoryId; }
        public Long getDosageFormId() { return dosageFormId; }
        public Long getAmpId() { return ampId; }
        public Long getInstitutionId() { return institutionId; }
        public Long getDepartmentId() { return departmentId; }
        public Double getRetailRate() { return retailRate; }
        public Boolean getAllowFractions() { return allowFractions; }
        public Boolean getDiscountAllowed() { return discountAllowed; }
    }
}
