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
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.ServiceCategory;
import com.divudi.core.entity.ServiceSubCategory;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.InwardPriceAdjustment;
import com.divudi.core.entity.lab.InvestigationCategory;
import com.divudi.core.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for Inward Price Adjustment (margin) Matrix management.
 * Backs the UI pages:
 *   /inward/inward_price_adjustment_service.xhtml
 *   /inward/inward_price_adjustment_investigation.xhtml
 *   /inward/inward_price_adjustment_pharmacy.xhtml
 *
 * Scope controls which category types are permitted (service|pharmacy).
 * Optional creditCompanyId creates a credit-company-specific margin override row.
 *
 * @author Dr M H B Ariyaratne
 */
@Path("inward-price-adjustment")
@RequestScoped
public class InwardPriceAdjustmentApi {

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
    private InstitutionFacade institutionFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    // =========================================================================
    // CRUD
    // =========================================================================

    /**
     * List price adjustment entries with optional filters.
     * GET /api/inward-price-adjustment?scope=service|pharmacy&...
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

            Long departmentId    = longParam("departmentId");
            Long categoryId      = longParam("categoryId");
            Long creditCompanyId = longParam("creditCompanyId");
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
                    "select a from InwardPriceAdjustment a where a.retired = false");
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
            if (paymentMethod != null) {
                jpql.append(" and a.paymentMethod = :pm");
                params.put("pm", paymentMethod);
            }
            if (creditCompanyId != null) {
                jpql.append(" and a.creditCompany.id = :ccid");
                params.put("ccid", creditCompanyId);
            }

            jpql.append(" order by a.department.name, a.category.name, a.fromPrice");

            List<PriceMatrix> rows = priceMatrixFacade.findByJpql(jpql.toString(), params, limit);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (rows != null) {
                for (PriceMatrix pm : rows) {
                    payload.add(toDto(pm));
                }
            }
            return successResponse(payload);

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Fetch one entry by id.
     * GET /api/inward-price-adjustment/{id}
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
            if (pm == null || pm.isRetired() || !(pm instanceof InwardPriceAdjustment)) {
                return errorResponse("Price adjustment entry not found: " + id, 404);
            }
            return successResponse(toDto(pm));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create a new entry. Rejects duplicates with 409 + existing id.
     * POST /api/inward-price-adjustment
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

            Double fromPrice = asDouble(body.get("fromPrice"));
            Double toPrice   = asDouble(body.get("toPrice"));
            Double margin    = asDouble(body.get("margin"));
            if (fromPrice == null || toPrice == null || margin == null) {
                return errorResponse("fromPrice, toPrice, and margin are required", 400);
            }
            if (fromPrice >= toPrice) {
                return errorResponse("fromPrice must be less than toPrice", 400);
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

            PaymentMethod paymentMethod = null;
            String paymentMethodStr = asString(body.get("paymentMethod"));
            if (paymentMethodStr != null && !paymentMethodStr.trim().isEmpty()) {
                try {
                    paymentMethod = PaymentMethod.valueOf(paymentMethodStr.trim());
                } catch (IllegalArgumentException e) {
                    return errorResponse("Invalid paymentMethod: " + paymentMethodStr, 400);
                }
            }

            Institution creditCompany = null;
            Long creditCompanyId = asLong(body.get("creditCompanyId"));
            if (creditCompanyId != null) {
                creditCompany = institutionFacade.find(creditCompanyId);
                if (creditCompany == null || creditCompany.isRetired()) {
                    return errorResponse("Credit company not found: " + creditCompanyId, 400);
                }
            }

            InwardPriceAdjustment existing = findDuplicate(
                    department, category, paymentMethod, fromPrice, toPrice, creditCompany);
            if (existing != null) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("status", "already_exists");
                payload.put("code", 409);
                payload.put("message",
                        "An active price adjustment entry with the same combination already exists.");
                payload.put("id", existing.getId());
                return Response.status(409).entity(gson.toJson(payload)).build();
            }

            InwardPriceAdjustment entry = new InwardPriceAdjustment();
            entry.setDepartment(department);
            entry.setCategory(category);
            entry.setPaymentMethod(paymentMethod);
            entry.setFromPrice(fromPrice);
            entry.setToPrice(toPrice);
            entry.setMargin(margin);
            entry.setCreditCompany(creditCompany);
            if (department != null) {
                entry.setInstitution(department.getInstitution());
            }
            entry.setCreatedAt(new Date());
            entry.setCreater(user);
            priceMatrixFacade.create(entry);

            return Response.status(201).entity(gson.toJson(successData(toDto(entry)))).build();

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update an entry.
     * PUT /api/inward-price-adjustment/{id}
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
            if (pm == null || pm.isRetired() || !(pm instanceof InwardPriceAdjustment)) {
                return errorResponse("Price adjustment entry not found: " + id, 404);
            }
            InwardPriceAdjustment entry = (InwardPriceAdjustment) pm;

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
                    if (scope == null || scope.trim().isEmpty()) {
                        return errorResponse("scope is required when categoryId is supplied", 400);
                    }
                    scope = scope.trim().toLowerCase();
                    String mismatch = validateCategoryForScope(c, scope);
                    if (mismatch != null) {
                        return errorResponse(mismatch, 400);
                    }
                    entry.setCategory(c);
                }
            }

            if (body.containsKey("paymentMethod")) {
                String pmStr = asString(body.get("paymentMethod"));
                if (pmStr == null || pmStr.trim().isEmpty()) {
                    entry.setPaymentMethod(null);
                } else {
                    try {
                        entry.setPaymentMethod(PaymentMethod.valueOf(pmStr.trim()));
                    } catch (IllegalArgumentException e) {
                        return errorResponse("Invalid paymentMethod: " + pmStr, 400);
                    }
                }
            }

            if (body.containsKey("fromPrice")) {
                Double fp = asDouble(body.get("fromPrice"));
                if (fp == null) return errorResponse("fromPrice cannot be null", 400);
                entry.setFromPrice(fp);
            }
            if (body.containsKey("toPrice")) {
                Double tp = asDouble(body.get("toPrice"));
                if (tp == null) return errorResponse("toPrice cannot be null", 400);
                entry.setToPrice(tp);
            }
            if (entry.getFromPrice() != null && entry.getToPrice() != null
                    && entry.getFromPrice() >= entry.getToPrice()) {
                return errorResponse("fromPrice must be less than toPrice", 400);
            }

            if (body.containsKey("margin")) {
                Double m = asDouble(body.get("margin"));
                if (m == null) return errorResponse("margin cannot be null", 400);
                entry.setMargin(m);
            }

            if (body.containsKey("creditCompanyId")) {
                Long ccId = asLong(body.get("creditCompanyId"));
                if (ccId == null) {
                    entry.setCreditCompany(null);
                } else {
                    Institution cc = institutionFacade.find(ccId);
                    if (cc == null || cc.isRetired()) {
                        return errorResponse("Credit company not found: " + ccId, 400);
                    }
                    entry.setCreditCompany(cc);
                }
            }

            InwardPriceAdjustment dup = findDuplicate(
                    entry.getDepartment(), entry.getCategory(), entry.getPaymentMethod(),
                    entry.getFromPrice(), entry.getToPrice(), entry.getCreditCompany());
            if (dup != null && !dup.getId().equals(entry.getId())) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("status", "already_exists");
                payload.put("code", 409);
                payload.put("message",
                        "Another active price adjustment entry with the same combination already exists.");
                payload.put("id", dup.getId());
                return Response.status(409).entity(gson.toJson(payload)).build();
            }

            priceMatrixFacade.edit(entry);
            return successResponse(toDto(entry));

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Soft-retire an entry.
     * DELETE /api/inward-price-adjustment/{id}
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
            if (pm == null || !(pm instanceof InwardPriceAdjustment)) {
                return errorResponse("Price adjustment entry not found: " + id, 404);
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
    // Lookup endpoints
    // =========================================================================

    /**
     * Search categories by scope.
     * GET /api/inward-price-adjustment/categories/search?scope=service|pharmacy&query=&limit=
     */
    @GET
    @Path("/categories/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCategories() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            String scope = param("scope");
            if (scope == null) scope = "service";
            scope = scope.trim().toLowerCase();

            String query = param("query");
            int limit = intParam("limit", 30, 1, 200);

            StringBuilder jpql = new StringBuilder("select c from Category c where c.retired = false");
            Map<String, Object> params = new HashMap<>();

            if ("service".equals(scope)) {
                jpql.append(" and (type(c) = :svc or type(c) = :sub or type(c) = :inv)");
                params.put("svc", ServiceCategory.class);
                params.put("sub", ServiceSubCategory.class);
                params.put("inv", InvestigationCategory.class);
            } else if ("pharmacy".equals(scope)) {
                jpql.append(" and type(c) = :pharm");
                params.put("pharm", PharmaceuticalItemCategory.class);
            } else {
                return errorResponse("Invalid scope. Use 'service' or 'pharmacy'.", 400);
            }

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
                    row.put("type", c.getClass().getSimpleName());
                    payload.add(row);
                }
            }
            return successResponse(payload);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Search departments by name.
     * GET /api/inward-price-adjustment/departments/search?query=&limit=
     */
    @GET
    @Path("/departments/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchDepartments() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            String query = param("query");
            int limit = intParam("limit", 30, 1, 200);

            StringBuilder jpql = new StringBuilder("select d from Department d where d.retired = false");
            Map<String, Object> params = new HashMap<>();
            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and upper(d.name) like :q");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }
            jpql.append(" order by d.name");

            List<Department> results = departmentFacade.findByJpql(jpql.toString(), params, limit);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (results != null) {
                for (Department d : results) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", d.getId());
                    row.put("name", d.getName());
                    if (d.getInstitution() != null) {
                        row.put("institutionId", d.getInstitution().getId());
                        row.put("institutionName", d.getInstitution().getName());
                    }
                    payload.add(row);
                }
            }
            return successResponse(payload);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * List all PaymentMethod enum values.
     * GET /api/inward-price-adjustment/payment-methods
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

    /**
     * Search credit companies (institutions) by name.
     * GET /api/inward-price-adjustment/credit-companies/search?query=&limit=
     */
    @GET
    @Path("/credit-companies/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCreditCompanies() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            String query = param("query");
            int limit = intParam("limit", 30, 1, 200);
            StringBuilder jpql = new StringBuilder(
                    "select i from Institution i where i.retired = false");
            Map<String, Object> params = new HashMap<>();
            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and upper(i.name) like :q");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }
            jpql.append(" order by i.name");
            List<Institution> results = institutionFacade.findByJpql(jpql.toString(), params, limit);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (results != null) {
                for (Institution inst : results) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", inst.getId());
                    row.put("name", inst.getName());
                    payload.add(row);
                }
            }
            return successResponse(payload);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private InwardPriceAdjustment findDuplicate(Department department, Category category,
            PaymentMethod paymentMethod, Double fromPrice, Double toPrice, Institution creditCompany) {

        StringBuilder jpql = new StringBuilder(
                "select a from InwardPriceAdjustment a where a.retired = false");
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
        if (paymentMethod == null) {
            jpql.append(" and a.paymentMethod is null");
        } else {
            jpql.append(" and a.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }
        if (fromPrice != null) {
            jpql.append(" and a.fromPrice = :fp");
            params.put("fp", fromPrice);
        }
        if (toPrice != null) {
            jpql.append(" and a.toPrice = :tp");
            params.put("tp", toPrice);
        }
        if (creditCompany == null) {
            jpql.append(" and a.creditCompany is null");
        } else {
            jpql.append(" and a.creditCompany = :cc");
            params.put("cc", creditCompany);
        }

        @SuppressWarnings("unchecked")
        List<InwardPriceAdjustment> list = (List<InwardPriceAdjustment>) (List<?>)
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
        row.put("fromPrice", pm.getFromPrice());
        row.put("toPrice", pm.getToPrice());
        row.put("margin", pm.getMargin());

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

        row.put("paymentMethod", pm.getPaymentMethod() != null ? pm.getPaymentMethod().name() : null);

        if (pm.getCreditCompany() != null) {
            Map<String, Object> cc = new LinkedHashMap<>();
            cc.put("id", pm.getCreditCompany().getId());
            cc.put("name", pm.getCreditCompany().getName());
            row.put("creditCompany", cc);
        } else {
            row.put("creditCompany", null);
        }

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
            throw new IllegalArgumentException("Invalid numeric value for '" + name + "': '" + v + "'");
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
            throw new IllegalArgumentException("Invalid numeric id: '" + o + "'");
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
            throw new IllegalArgumentException("Invalid numeric value: '" + o + "'");
        }
    }

    // -------- auth + response helpers --------

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
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return response;
    }
}
