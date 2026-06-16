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
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.InwardPriceAdjustment;
import com.divudi.core.facade.AdmissionTypeFacade;
import com.divudi.core.facade.CategoryFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.PriceMatrixFacade;
import com.divudi.service.AuditService;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for Inward Price Adjustment (margin) Matrix management.
 * Path: /api/price-matrix/inward
 *
 * Flat DTO format with departmentId/departmentName etc.
 * Audit events logged for every create/update/retire.
 *
 * @author Dr M H B Ariyaratne
 */
@Path("price-matrix/inward")
@RequestScoped
public class PriceMatrixInwardApi {

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

    @EJB
    private AdmissionTypeFacade admissionTypeFacade;

    @EJB
    private AuditService auditService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================================================================
    // CRUD
    // =========================================================================

    /**
     * List price adjustment entries with optional filters.
     * GET /api/price-matrix/inward?categoryId=&departmentId=&paymentMethod=&limit=
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Long departmentId    = longParam("departmentId");
            Long categoryId      = longParam("categoryId");
            String paymentMethodStr = param("paymentMethod");
            int limit = intParam("limit", 50, 1, 1000);

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
     * GET /api/price-matrix/inward/{id}
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
     * Create a new entry.
     * POST /api/price-matrix/inward
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

            // Required: departmentId
            Long departmentId = asLong(body.get("departmentId"));
            if (departmentId == null) {
                return errorResponse("departmentId is required", 400);
            }
            Department department = departmentFacade.find(departmentId);
            if (department == null || department.isRetired()) {
                return errorResponse("Department not found: " + departmentId, 400);
            }

            // Required: categoryId
            Long categoryId = asLong(body.get("categoryId"));
            if (categoryId == null) {
                return errorResponse("categoryId is required", 400);
            }
            Category category = categoryFacade.find(categoryId);
            if (category == null || category.isRetired()) {
                return errorResponse("Category not found: " + categoryId, 400);
            }

            // Required: margin
            Double margin = asDouble(body.get("margin"));
            if (margin == null) {
                return errorResponse("margin is required", 400);
            }
            if (margin.isNaN() || margin.isInfinite()) {
                return errorResponse("margin must be a finite number", 400);
            }

            // Optional: paymentMethod
            PaymentMethod paymentMethod = null;
            String paymentMethodStr = asString(body.get("paymentMethod"));
            if (paymentMethodStr != null && !paymentMethodStr.trim().isEmpty()) {
                try {
                    paymentMethod = PaymentMethod.valueOf(paymentMethodStr.trim());
                } catch (IllegalArgumentException e) {
                    return errorResponse("Invalid paymentMethod: " + paymentMethodStr, 400);
                }
            }

            // Optional: discountPercent (default 0)
            Double discountPercent = asDouble(body.get("discountPercent"));
            if (discountPercent == null) {
                discountPercent = 0.0;
            }
            if (discountPercent.isNaN() || discountPercent.isInfinite()) {
                return errorResponse("discountPercent must be a finite number", 400);
            }

            // Optional: fromPrice (default 0)
            Double fromPrice = asDouble(body.get("fromPrice"));
            if (fromPrice == null) {
                fromPrice = 0.0;
            }
            if (fromPrice.isNaN() || fromPrice.isInfinite()) {
                return errorResponse("fromPrice must be a finite number", 400);
            }

            // Optional: toPrice (default 9999999999)
            Double toPrice = asDouble(body.get("toPrice"));
            if (toPrice == null) {
                toPrice = 9999999999.0;
            }
            if (toPrice.isNaN() || toPrice.isInfinite()) {
                return errorResponse("toPrice must be a finite number", 400);
            }

            if (fromPrice >= toPrice) {
                return errorResponse("fromPrice must be less than toPrice", 400);
            }

            // Optional: admissionTypeId
            AdmissionType admissionType = null;
            Long admissionTypeId = asLong(body.get("admissionTypeId"));
            if (admissionTypeId != null) {
                admissionType = admissionTypeFacade.find(admissionTypeId);
                if (admissionType == null || admissionType.isRetired()) {
                    return errorResponse("Admission type not found: " + admissionTypeId, 400);
                }
            }

            // Optional: creditCompanyId
            Institution creditCompany = null;
            Long creditCompanyId = asLong(body.get("creditCompanyId"));
            if (creditCompanyId != null) {
                creditCompany = institutionFacade.find(creditCompanyId);
                if (creditCompany == null || creditCompany.isRetired()) {
                    return errorResponse("Credit company not found: " + creditCompanyId, 400);
                }
            }

            // Duplicate detection
            InwardPriceAdjustment existing = findDuplicate(
                    department, category, paymentMethod, fromPrice, toPrice,
                    creditCompany, admissionType);
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
            entry.setInstitution(department.getInstitution());
            entry.setCategory(category);
            entry.setPaymentMethod(paymentMethod);
            entry.setMargin(margin);
            entry.setDiscountPercent(discountPercent);
            entry.setFromPrice(fromPrice);
            entry.setToPrice(toPrice);
            entry.setAdmissionType(admissionType);
            entry.setCreditCompany(creditCompany);
            entry.setCreatedAt(new Date());
            entry.setCreater(user);
            priceMatrixFacade.create(entry);

            // Audit event
            auditService.logAudit(null, toDto(entry), user,
                    "InwardPriceAdjustment", "PRICE_MATRIX_CREATED", entry.getId());

            return Response.status(201).entity(gson.toJson(successData(toDto(entry)))).build();

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update an entry. Only supplied fields are updated.
     * PUT /api/price-matrix/inward/{id}
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

            // Snapshot before changes for audit
            Map<String, Object> beforeState = toDto(entry);

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
                    return errorResponse("departmentId cannot be null", 400);
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
                    return errorResponse("categoryId cannot be null", 400);
                } else {
                    Category c = categoryFacade.find(categoryId);
                    if (c == null || c.isRetired()) {
                        return errorResponse("Category not found: " + categoryId, 400);
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

            if (body.containsKey("margin")) {
                Double m = asDouble(body.get("margin"));
                if (m == null) return errorResponse("margin cannot be null", 400);
                if (m.isNaN() || m.isInfinite()) return errorResponse("margin must be a finite number", 400);
                entry.setMargin(m);
            }

            if (body.containsKey("discountPercent")) {
                Double dp = asDouble(body.get("discountPercent"));
                if (dp == null) return errorResponse("discountPercent cannot be null", 400);
                if (dp.isNaN() || dp.isInfinite()) return errorResponse("discountPercent must be a finite number", 400);
                entry.setDiscountPercent(dp);
            }

            if (body.containsKey("fromPrice")) {
                Double fp = asDouble(body.get("fromPrice"));
                if (fp == null) return errorResponse("fromPrice cannot be null", 400);
                if (fp.isNaN() || fp.isInfinite()) return errorResponse("fromPrice must be a finite number", 400);
                entry.setFromPrice(fp);
            }
            if (body.containsKey("toPrice")) {
                Double tp = asDouble(body.get("toPrice"));
                if (tp == null) return errorResponse("toPrice cannot be null", 400);
                if (tp.isNaN() || tp.isInfinite()) return errorResponse("toPrice must be a finite number", 400);
                entry.setToPrice(tp);
            }
            if (entry.getFromPrice() != null && entry.getToPrice() != null
                    && entry.getFromPrice() >= entry.getToPrice()) {
                return errorResponse("fromPrice must be less than toPrice", 400);
            }

            if (body.containsKey("admissionTypeId")) {
                Long atId = asLong(body.get("admissionTypeId"));
                if (atId == null) {
                    entry.setAdmissionType(null);
                } else {
                    AdmissionType at = admissionTypeFacade.find(atId);
                    if (at == null || at.isRetired()) {
                        return errorResponse("Admission type not found: " + atId, 400);
                    }
                    entry.setAdmissionType(at);
                }
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

            // Duplicate check (excluding self)
            InwardPriceAdjustment dup = findDuplicate(
                    entry.getDepartment(), entry.getCategory(), entry.getPaymentMethod(),
                    entry.getFromPrice(), entry.getToPrice(), entry.getCreditCompany(),
                    entry.getAdmissionType());
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

            // Audit event
            auditService.logAudit(beforeState, toDto(entry), user,
                    "InwardPriceAdjustment", "PRICE_MATRIX_UPDATED", entry.getId());

            return successResponse(toDto(entry));

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Soft-retire an entry.
     * DELETE /api/price-matrix/inward/{id}?retireComments=
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

            // Snapshot before changes for audit
            Map<String, Object> beforeState = toDto(pm);

            pm.setRetired(true);
            pm.setRetiredAt(new Date());
            pm.setRetirer(user);
            String retireComments = param("retireComments");
            if (retireComments != null && !retireComments.trim().isEmpty()) {
                pm.setRetireComments(retireComments.trim());
            }
            priceMatrixFacade.edit(pm);

            // Audit event
            auditService.logAudit(beforeState, toDto(pm), user,
                    "InwardPriceAdjustment", "PRICE_MATRIX_RETIRED", pm.getId());

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", pm.getId());
            resp.put("retired", true);
            return successResponse(resp);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // DTO conversion
    // =========================================================================

    private Map<String, Object> toDto(PriceMatrix pm) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", pm.getId());

        if (pm.getDepartment() != null) {
            row.put("departmentId", pm.getDepartment().getId());
            row.put("departmentName", pm.getDepartment().getName());
        } else {
            row.put("departmentId", null);
            row.put("departmentName", null);
        }

        if (pm.getCategory() != null) {
            row.put("categoryId", pm.getCategory().getId());
            row.put("categoryName", pm.getCategory().getName());
        } else {
            row.put("categoryId", null);
            row.put("categoryName", null);
        }

        row.put("paymentMethod", pm.getPaymentMethod() != null ? pm.getPaymentMethod().name() : null);
        row.put("margin", pm.getMargin());
        row.put("discountPercent", pm.getDiscountPercent());
        row.put("fromPrice", pm.getFromPrice());
        row.put("toPrice", pm.getToPrice());

        if (pm.getAdmissionType() != null) {
            row.put("admissionTypeId", pm.getAdmissionType().getId());
            row.put("admissionTypeName", pm.getAdmissionType().getName());
        } else {
            row.put("admissionTypeId", null);
            row.put("admissionTypeName", null);
        }

        if (pm.getCreditCompany() != null) {
            row.put("creditCompanyId", pm.getCreditCompany().getId());
            row.put("creditCompanyName", pm.getCreditCompany().getName());
        } else {
            row.put("creditCompanyId", null);
            row.put("creditCompanyName", null);
        }

        row.put("retired", pm.isRetired());
        if (pm.getCreatedAt() != null) {
            row.put("createdAt", pm.getCreatedAt().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime()
                    .format(DATE_FORMATTER));
        } else {
            row.put("createdAt", null);
        }
        return row;
    }

    // =========================================================================
    // Duplicate detection
    // =========================================================================

    private InwardPriceAdjustment findDuplicate(Department department, Category category,
            PaymentMethod paymentMethod, Double fromPrice, Double toPrice,
            Institution creditCompany, AdmissionType admissionType) {

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
        if (admissionType == null) {
            jpql.append(" and a.admissionType is null");
        } else {
            jpql.append(" and a.admissionType = :at");
            params.put("at", admissionType);
        }

        @SuppressWarnings("unchecked")
        List<InwardPriceAdjustment> list = (List<InwardPriceAdjustment>) (List<?>)
                priceMatrixFacade.findByJpql(jpql.toString(), params, 1);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    // =========================================================================
    // Param helpers
    // =========================================================================

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

    // =========================================================================
    // Auth + response helpers
    // =========================================================================

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
