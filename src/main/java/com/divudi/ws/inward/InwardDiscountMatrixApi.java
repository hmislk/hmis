/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.inward;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.ServiceCategory;
import com.divudi.core.entity.ServiceSubCategory;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.InwardDiscountMatrix;
import com.divudi.core.entity.lab.InvestigationCategory;
import com.divudi.core.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.PaymentSchemeFacade;
import com.divudi.core.facade.PriceMatrixFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for Inward Discount Matrix management.
 * Backs the two UI pages
 *   /inward/inward_discount_matrix_service_investigation.xhtml
 *   /inward/inward_discount_matrix_pharmacy.xhtml
 *
 * Single endpoint, scope-scoped (service|pharmacy) — scope controls which
 * category types are allowed.
 *
 * @author Dr M H B Ariyaratne
 */
@Path("inward-discount-matrix")
@RequestScoped
public class InwardDiscountMatrixApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private PriceMatrixFacade priceMatrixFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private CategoryFacade categoryFacade;

    @EJB
    private PaymentSchemeFacade paymentSchemeFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    // =========================================================================
    // Discount matrix CRUD
    // =========================================================================

    /**
     * List discount matrix entries with optional filters.
     *
     * GET /api/inward-discount-matrix?scope=service|pharmacy&...
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String scope = param("scope");
            if (scope != null) scope = scope.trim().toLowerCase();

            Long departmentId     = longParam("departmentId");
            Long categoryId       = longParam("categoryId");
            Long admissionTypeId  = longParam("admissionTypeId");
            Long paymentSchemeId  = longParam("paymentSchemeId");
            String paymentMethodStr = param("paymentMethod");
            int limit = intParam("limit", 200, 1, 1000);

            PaymentMethod paymentMethod = null;
            if (paymentMethodStr != null && !paymentMethodStr.trim().isEmpty()) {
                try {
                    paymentMethod = PaymentMethod.valueOf(paymentMethodStr.trim());
                } catch (IllegalArgumentException e) {
                    return errorResponse("Invalid paymentMethod: " + paymentMethodStr, 400);
                }
            }

            StringBuilder jpql = new StringBuilder(
                    "select a from InwardDiscountMatrix a where a.retired = false");
            Map<String, Object> params = new HashMap<>();

            if ("service".equals(scope)) {
                jpql.append(" and (type(a.category) = :svc"
                        + " or type(a.category) = :sub"
                        + " or type(a.category) = :inv"
                        + " or a.category is null)");
                params.put("svc", ServiceCategory.class);
                params.put("sub", ServiceSubCategory.class);
                params.put("inv", InvestigationCategory.class);
            } else if ("pharmacy".equals(scope)) {
                jpql.append(" and (type(a.category) = :pharm or a.category is null)");
                params.put("pharm", PharmaceuticalItemCategory.class);
            } else if (scope != null && !scope.isEmpty()) {
                return errorResponse("Invalid scope. Use 'service' or 'pharmacy'.", 400);
            }

            if (departmentId != null) {
                jpql.append(" and a.department.id = :did");
                params.put("did", departmentId);
            }
            if (categoryId != null) {
                jpql.append(" and a.category.id = :cid");
                params.put("cid", categoryId);
            }
            if (admissionTypeId != null) {
                jpql.append(" and a.admissionType.id = :aid");
                params.put("aid", admissionTypeId);
            }
            if (paymentSchemeId != null) {
                jpql.append(" and a.paymentScheme.id = :psid");
                params.put("psid", paymentSchemeId);
            }
            if (paymentMethod != null) {
                jpql.append(" and a.paymentMethod = :pm");
                params.put("pm", paymentMethod);
            }

            jpql.append(" order by a.paymentScheme.name, a.department.name, a.category.name");

            List<PriceMatrix> rows = priceMatrixFacade.findByJpql(jpql.toString(), params, limit);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (rows != null) {
                for (PriceMatrix pm : rows) {
                    payload.add(toDto(pm));
                }
            }
            return successResponse(payload);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Fetch one entry by id.
     * GET /api/inward-discount-matrix/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            PriceMatrix pm = priceMatrixFacade.find(id);
            if (pm == null || pm.isRetired() || !(pm instanceof InwardDiscountMatrix)) {
                return errorResponse("Discount matrix entry not found: " + id, 404);
            }
            return successResponse(toDto(pm));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create a new entry. Rejects duplicates with 409 + existing id.
     * POST /api/inward-discount-matrix
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Map<?, ?> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (body == null) {
                return errorResponse("Request body is required", 400);
            }

            String scope = asString(body.get("scope"));
            if (scope == null || scope.trim().isEmpty()) {
                return errorResponse("scope is required ('service' or 'pharmacy')", 400);
            }
            scope = scope.trim().toLowerCase();
            if (!"service".equals(scope) && !"pharmacy".equals(scope)) {
                return errorResponse("Invalid scope. Use 'service' or 'pharmacy'.", 400);
            }

            Long paymentSchemeId = asLong(body.get("paymentSchemeId"));
            if (paymentSchemeId == null) {
                return errorResponse("paymentSchemeId is required", 400);
            }
            PaymentScheme paymentScheme = paymentSchemeFacade.find(paymentSchemeId);
            if (paymentScheme == null || paymentScheme.isRetired()) {
                return errorResponse("PaymentScheme not found: " + paymentSchemeId, 400);
            }

            Double discountPercent = asDouble(body.get("discountPercent"));
            if (discountPercent == null) {
                return errorResponse("discountPercent is required", 400);
            }

            Department department = null;
            Long departmentId = asLong(body.get("departmentId"));
            if (departmentId != null) {
                department = departmentFacade.find(departmentId);
                if (department == null || department.isRetired()) {
                    return errorResponse("Department not found: " + departmentId, 400);
                }
            }

            Category category = null;
            Long categoryId = asLong(body.get("categoryId"));
            if (categoryId != null) {
                category = categoryFacade.find(categoryId);
                if (category == null || category.isRetired()) {
                    return errorResponse("Category not found: " + categoryId, 400);
                }
                String mismatch = validateCategoryForScope(category, scope);
                if (mismatch != null) {
                    return errorResponse(mismatch, 400);
                }
            }

            AdmissionType admissionType = null;
            Long admissionTypeId = asLong(body.get("admissionTypeId"));
            if (admissionTypeId != null) {
                Category c = categoryFacade.find(admissionTypeId);
                if (c == null || c.isRetired() || !(c instanceof AdmissionType)) {
                    return errorResponse("AdmissionType not found: " + admissionTypeId, 400);
                }
                admissionType = (AdmissionType) c;
            }

            PaymentMethod paymentMethod = null;
            String paymentMethodStr = asString(body.get("paymentMethod"));
            if (paymentMethodStr != null && !paymentMethodStr.trim().isEmpty()) {
                try {
                    paymentMethod = PaymentMethod.valueOf(paymentMethodStr.trim());
                } catch (IllegalArgumentException e) {
                    return errorResponse("Invalid paymentMethod: " + paymentMethodStr, 400);
                }
            }

            InwardDiscountMatrix existing = findDuplicate(
                    department, category, admissionType, paymentMethod, paymentScheme);
            if (existing != null) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("status", "already_exists");
                payload.put("code", 409);
                payload.put("message",
                        "An active discount matrix entry with the same combination already exists.");
                payload.put("id", existing.getId());
                return Response.status(409).entity(gson.toJson(payload)).build();
            }

            InwardDiscountMatrix entry = new InwardDiscountMatrix();
            entry.setDepartment(department);
            entry.setCategory(category);
            entry.setAdmissionType(admissionType);
            entry.setPaymentMethod(paymentMethod);
            entry.setPaymentScheme(paymentScheme);
            entry.setDiscountPercent(discountPercent);
            if (department != null) {
                entry.setInstitution(department.getInstitution());
            }
            entry.setCreatedAt(new Date());
            entry.setCreater(user);
            priceMatrixFacade.create(entry);

            return Response.status(201).entity(gson.toJson(successData(toDto(entry)))).build();

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update an entry. All fields optional; scope is required when categoryId is supplied.
     * PUT /api/inward-discount-matrix/{id}
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            PriceMatrix pm = priceMatrixFacade.find(id);
            if (pm == null || pm.isRetired() || !(pm instanceof InwardDiscountMatrix)) {
                return errorResponse("Discount matrix entry not found: " + id, 404);
            }
            InwardDiscountMatrix entry = (InwardDiscountMatrix) pm;

            Map<?, ?> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (body == null) {
                return errorResponse("Request body is required", 400);
            }

            if (body.containsKey("departmentId")) {
                Long departmentId = asLong(body.get("departmentId"));
                if (departmentId == null) {
                    entry.setDepartment(null);
                    entry.setInstitution(null);
                } else {
                    Department d = departmentFacade.find(departmentId);
                    if (d == null || d.isRetired()) {
                        return errorResponse("Department not found: " + departmentId, 400);
                    }
                    entry.setDepartment(d);
                    entry.setInstitution(d.getInstitution());
                }
            }

            if (body.containsKey("categoryId")) {
                Long categoryId = asLong(body.get("categoryId"));
                if (categoryId == null) {
                    entry.setCategory(null);
                } else {
                    Category c = categoryFacade.find(categoryId);
                    if (c == null || c.isRetired()) {
                        return errorResponse("Category not found: " + categoryId, 400);
                    }
                    String scope = asString(body.get("scope"));
                    if (scope != null && !scope.trim().isEmpty()) {
                        String mismatch = validateCategoryForScope(c, scope.trim().toLowerCase());
                        if (mismatch != null) {
                            return errorResponse(mismatch, 400);
                        }
                    }
                    entry.setCategory(c);
                }
            }

            if (body.containsKey("admissionTypeId")) {
                Long admissionTypeId = asLong(body.get("admissionTypeId"));
                if (admissionTypeId == null) {
                    entry.setAdmissionType(null);
                } else {
                    Category c = categoryFacade.find(admissionTypeId);
                    if (c == null || c.isRetired() || !(c instanceof AdmissionType)) {
                        return errorResponse("AdmissionType not found: " + admissionTypeId, 400);
                    }
                    entry.setAdmissionType((AdmissionType) c);
                }
            }

            if (body.containsKey("paymentSchemeId")) {
                Long paymentSchemeId = asLong(body.get("paymentSchemeId"));
                if (paymentSchemeId == null) {
                    return errorResponse("paymentSchemeId cannot be null", 400);
                }
                PaymentScheme ps = paymentSchemeFacade.find(paymentSchemeId);
                if (ps == null || ps.isRetired()) {
                    return errorResponse("PaymentScheme not found: " + paymentSchemeId, 400);
                }
                entry.setPaymentScheme(ps);
            }

            if (body.containsKey("paymentMethod")) {
                String pm2 = asString(body.get("paymentMethod"));
                if (pm2 == null || pm2.trim().isEmpty()) {
                    entry.setPaymentMethod(null);
                } else {
                    try {
                        entry.setPaymentMethod(PaymentMethod.valueOf(pm2.trim()));
                    } catch (IllegalArgumentException e) {
                        return errorResponse("Invalid paymentMethod: " + pm2, 400);
                    }
                }
            }

            if (body.containsKey("discountPercent")) {
                Double dp = asDouble(body.get("discountPercent"));
                if (dp == null) {
                    return errorResponse("discountPercent cannot be null", 400);
                }
                entry.setDiscountPercent(dp);
            }

            InwardDiscountMatrix dup = findDuplicate(
                    entry.getDepartment(), entry.getCategory(), entry.getAdmissionType(),
                    entry.getPaymentMethod(), entry.getPaymentScheme());
            if (dup != null && !dup.getId().equals(entry.getId())) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("status", "already_exists");
                payload.put("code", 409);
                payload.put("message",
                        "Another active discount matrix entry with the same combination already exists.");
                payload.put("id", dup.getId());
                return Response.status(409).entity(gson.toJson(payload)).build();
            }

            priceMatrixFacade.edit(entry);
            return successResponse(toDto(entry));

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Soft-retire an entry.
     * DELETE /api/inward-discount-matrix/{id}?retireComments=reason
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retire(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            PriceMatrix pm = priceMatrixFacade.find(id);
            if (pm == null || !(pm instanceof InwardDiscountMatrix)) {
                return errorResponse("Discount matrix entry not found: " + id, 404);
            }
            if (pm.isRetired()) {
                return errorResponse("Entry is already retired: " + id, 400);
            }
            pm.setRetired(true);
            pm.setRetiredAt(new Date());
            pm.setRetirer(user);
            String retireComments = param("retireComments");
            if (retireComments != null && !retireComments.trim().isEmpty()) {
                pm.setRetireComments(retireComments.trim());
            }
            priceMatrixFacade.edit(pm);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", pm.getId());
            resp.put("retired", true);
            return successResponse(resp);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Lookup endpoints (for name → id resolution)
    // =========================================================================

    /**
     * Search AdmissionType by name.
     * GET /api/inward-discount-matrix/admission-types/search?query=&limit=
     */
    @GET
    @Path("/admission-types/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchAdmissionTypes() {
        return searchCategoryByType(AdmissionType.class, "at");
    }

    /**
     * Search PharmaceuticalItemCategory by name.
     * GET /api/inward-discount-matrix/pharmaceutical-item-categories/search?query=&limit=
     */
    @GET
    @Path("/pharmaceutical-item-categories/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchPharmaceuticalItemCategories() {
        return searchCategoryByType(PharmaceuticalItemCategory.class, "pic");
    }

    private Response searchCategoryByType(Class<? extends Category> type, String paramKey) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            String query = param("query");
            int limit = intParam("limit", 30, 1, 200);

            StringBuilder jpql = new StringBuilder(
                    "select c from Category c where c.retired = false and type(c) = :")
                    .append(paramKey);
            Map<String, Object> params = new HashMap<>();
            params.put(paramKey, type);
            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and upper(c.name) like :q");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }
            jpql.append(" order by c.name");

            List<Category> results = categoryFacade.findByJpql(jpql.toString(), params, limit);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (results != null) {
                for (Category c : results) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", c.getId());
                    row.put("name", c.getName());
                    payload.add(row);
                }
            }
            return successResponse(payload);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Search PaymentScheme by name.
     * GET /api/inward-discount-matrix/payment-schemes/search?query=&limit=
     */
    @GET
    @Path("/payment-schemes/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchPaymentSchemes() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            String query = param("query");
            int limit = intParam("limit", 30, 1, 200);

            StringBuilder jpql = new StringBuilder(
                    "select p from PaymentScheme p where p.retired = false");
            Map<String, Object> params = new HashMap<>();
            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and upper(p.name) like :q");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }
            jpql.append(" order by p.name");

            List<PaymentScheme> results = paymentSchemeFacade.findByJpql(
                    jpql.toString(), params, limit);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (results != null) {
                for (PaymentScheme p : results) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", p.getId());
                    row.put("name", p.getName());
                    payload.add(row);
                }
            }
            return successResponse(payload);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * List all PaymentMethod enum values (no search — small set).
     * GET /api/inward-discount-matrix/payment-methods
     */
    @GET
    @Path("/payment-methods")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listPaymentMethods() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            List<Map<String, Object>> payload = new ArrayList<>();
            for (PaymentMethod pm : PaymentMethod.values()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", pm.name());
                row.put("label", pm.getLabel());
                payload.add(row);
            }
            return successResponse(payload);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private InwardDiscountMatrix findDuplicate(Department department, Category category,
            AdmissionType admissionType, PaymentMethod paymentMethod, PaymentScheme paymentScheme) {

        StringBuilder jpql = new StringBuilder(
                "select a from InwardDiscountMatrix a where a.retired = false");
        Map<String, Object> params = new HashMap<>();

        if (department == null) {
            jpql.append(" and a.department is null");
        } else {
            jpql.append(" and a.department = :dep");
            params.put("dep", department);
        }
        if (category == null) {
            jpql.append(" and a.category is null");
        } else {
            jpql.append(" and a.category = :cat");
            params.put("cat", category);
        }
        if (admissionType == null) {
            jpql.append(" and a.admissionType is null");
        } else {
            jpql.append(" and a.admissionType = :at");
            params.put("at", admissionType);
        }
        if (paymentMethod == null) {
            jpql.append(" and a.paymentMethod is null");
        } else {
            jpql.append(" and a.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }
        if (paymentScheme == null) {
            jpql.append(" and a.paymentScheme is null");
        } else {
            jpql.append(" and a.paymentScheme = :ps");
            params.put("ps", paymentScheme);
        }

        @SuppressWarnings("unchecked")
        List<InwardDiscountMatrix> list = (List<InwardDiscountMatrix>) (List<?>)
                priceMatrixFacade.findByJpql(jpql.toString(), params, 1);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    private String validateCategoryForScope(Category category, String scope) {
        if ("service".equals(scope)) {
            if (!(category instanceof ServiceCategory
                    || category instanceof ServiceSubCategory
                    || category instanceof InvestigationCategory)) {
                return "Category type does not match scope 'service'. "
                        + "Expected ServiceCategory, ServiceSubCategory, or InvestigationCategory.";
            }
        } else if ("pharmacy".equals(scope)) {
            if (!(category instanceof PharmaceuticalItemCategory)) {
                return "Category type does not match scope 'pharmacy'. "
                        + "Expected PharmaceuticalItemCategory.";
            }
        }
        return null;
    }

    private Map<String, Object> toDto(PriceMatrix pm) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", pm.getId());
        row.put("discountPercent", pm.getDiscountPercent());

        if (pm.getPaymentScheme() != null) {
            Map<String, Object> ps = new LinkedHashMap<>();
            ps.put("id", pm.getPaymentScheme().getId());
            ps.put("name", pm.getPaymentScheme().getName());
            row.put("paymentScheme", ps);
        } else {
            row.put("paymentScheme", null);
        }

        if (pm.getDepartment() != null) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("id", pm.getDepartment().getId());
            d.put("name", pm.getDepartment().getName());
            if (pm.getDepartment().getInstitution() != null) {
                d.put("institutionId", pm.getDepartment().getInstitution().getId());
                d.put("institutionName", pm.getDepartment().getInstitution().getName());
            }
            row.put("department", d);
        } else {
            row.put("department", null);
        }

        if (pm.getCategory() != null) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", pm.getCategory().getId());
            c.put("name", pm.getCategory().getName());
            c.put("type", pm.getCategory().getClass().getSimpleName());
            row.put("category", c);
        } else {
            row.put("category", null);
        }

        if (pm.getAdmissionType() != null) {
            Map<String, Object> at = new LinkedHashMap<>();
            at.put("id", pm.getAdmissionType().getId());
            at.put("name", pm.getAdmissionType().getName());
            row.put("admissionType", at);
        } else {
            row.put("admissionType", null);
        }

        row.put("paymentMethod", pm.getPaymentMethod() != null ? pm.getPaymentMethod().name() : null);
        row.put("retired", pm.isRetired());
        return row;
    }

    // -------- param helpers --------

    private String param(String name) {
        return uriInfo.getQueryParameters().getFirst(name);
    }

    private Long longParam(String name) {
        String v = param(name);
        if (v == null || v.trim().isEmpty()) return null;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int intParam(String name, int defaultValue, int min, int max) {
        String v = param(name);
        if (v == null || v.trim().isEmpty()) return defaultValue;
        try {
            int parsed = Integer.parseInt(v.trim());
            return Math.min(Math.max(parsed, min), max);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String asString(Object o) {
        if (o == null) return null;
        return o.toString();
    }

    private Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        try {
            if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double asDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).doubleValue();
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // -------- auth + response helpers --------

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
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return response;
    }

    // Unused helper kept in case future scopes add more types
    @SuppressWarnings("unused")
    private static List<String> scopeTypes() {
        return new ArrayList<>(Arrays.asList("service", "pharmacy"));
    }
}
