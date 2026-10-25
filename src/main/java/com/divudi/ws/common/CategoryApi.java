/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.common;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.CategoryType;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.DosageForm;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.PharmaceuticalCategory;
import com.divudi.core.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.core.entity.pharmacy.PharmaceuticalItemType;
import com.divudi.core.entity.pharmacy.StoreItemCategory;
import com.divudi.core.facade.CategoryFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
 * REST API for generic {@link Category} master-data lookup and creation.
 *
 * Category is the single-table-inheritance base for many typed lists
 * (DosageForm, PharmaceuticalItemCategory, RoomCategory, InvestigationCategory,
 * ...) but until now there was no way to search or create rows by
 * categoryType via API — a caller had to infer ids indirectly from other
 * resources' responses, and the one existing helper that creates categories
 * on the fly (CategoryController.findAndCreateCategoryByName, used by the
 * AMP/VMP bulk-Excel-import) always creates a plain, untyped Category row
 * even when it is explicitly being asked to create e.g. a dosage form.
 *
 * This API lets a caller search across Category by categoryType/name, and
 * create new rows of the pharmacy-relevant subtypes with the correct DTYPE
 * (and categoryType) stamped.
 *
 * @author Dr M H B Ariyaratne
 */
@Path("categories")
@RequestScoped
public class CategoryApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private CategoryFacade categoryFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    // categoryTypes this API can CREATE with the correct concrete subtype.
    // GET/search works for any categoryType (or none, to search across all of
    // them); POST is limited to these because each needs its own no-arg
    // subclass so the DTYPE/categoryType are stamped correctly.
    private static final Map<CategoryType, Class<? extends Category>> CREATABLE_TYPES = new LinkedHashMap<>();

    static {
        CREATABLE_TYPES.put(CategoryType.DOSAGE_FORM, DosageForm.class);
        CREATABLE_TYPES.put(CategoryType.PHARMACEUTICAL_CATEGORY, PharmaceuticalCategory.class);
        CREATABLE_TYPES.put(CategoryType.PHARMACEUTICAL_ITEM_CATEGORY, PharmaceuticalItemCategory.class);
        CREATABLE_TYPES.put(CategoryType.PHARMACEUTICAL_ITEM_TYPE, PharmaceuticalItemType.class);
        CREATABLE_TYPES.put(CategoryType.STORE_ITEM_CATEGORY, StoreItemCategory.class);
    }

    // =========================================================================
    // GET /api/categories?type=DOSAGE_FORM&query=Tablet&limit=50
    // =========================================================================

    /**
     * Search categories, optionally filtered by categoryType and/or a name
     * substring. type is any {@link CategoryType} enum name (case-insensitive);
     * omitting it searches across every category type.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String typeStr = param("type");
            String query = param("query");
            int limit = intParam("limit", 50, 1, 1000);

            CategoryType type = null;
            if (typeStr != null && !typeStr.trim().isEmpty()) {
                try {
                    type = CategoryType.valueOf(typeStr.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    return errorResponse("Invalid type: " + typeStr, 400);
                }
            }

            StringBuilder jpql = new StringBuilder("select c from Category c where c.retired=false");
            Map<String, Object> params = new HashMap<>();
            if (type != null) {
                jpql.append(" and c.categoryType=:type");
                params.put("type", type);
            }
            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and upper(c.name) like :q");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }
            jpql.append(" order by c.name");

            List<Category> rows = categoryFacade.findByJpql(jpql.toString(), params, limit);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (rows != null) {
                for (Category c : rows) {
                    payload.add(toDto(c));
                }
            }
            return successResponse(payload);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // POST /api/categories  {name, categoryType, description?, code?}
    // =========================================================================

    /**
     * Create a category of one of the pharmacy-relevant types (see
     * {@link #CREATABLE_TYPES}). Returns 409/already_exists when a non-retired
     * category with the same name already exists under the same categoryType.
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

            String name = asString(body.get("name"));
            if (name == null || name.trim().isEmpty()) {
                return errorResponse("name is required", 400);
            }
            name = name.trim();

            String typeStr = asString(body.get("categoryType"));
            if (typeStr == null || typeStr.trim().isEmpty()) {
                return errorResponse("categoryType is required. Supported: " + supportedTypesCsv(), 400);
            }
            CategoryType type;
            try {
                type = CategoryType.valueOf(typeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return errorResponse("Invalid categoryType: " + typeStr + ". Supported: " + supportedTypesCsv(), 400);
            }
            Class<? extends Category> targetClass = CREATABLE_TYPES.get(type);
            if (targetClass == null) {
                return errorResponse("categoryType " + type + " cannot be created via this API. Supported: " + supportedTypesCsv(), 400);
            }

            // Duplicate detection: same name + categoryType, not retired
            Map<String, Object> dupParams = new HashMap<>();
            dupParams.put("type", type);
            dupParams.put("n", name.toUpperCase());
            Category existing = categoryFacade.findFirstByJpql(
                    "select c from Category c where c.retired=false and c.categoryType=:type and upper(c.name)=:n",
                    dupParams);
            if (existing != null) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("status", "already_exists");
                payload.put("code", 409);
                payload.put("id", existing.getId());
                payload.put("name", existing.getName());
                return Response.status(409).entity(gson.toJson(payload)).build();
            }

            Category category;
            try {
                category = targetClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                return errorResponse("Could not instantiate categoryType " + type + ": " + e.getMessage(), 500);
            }
            category.setName(name);
            category.setCategoryType(type);
            String description = asString(body.get("description"));
            if (description != null && !description.trim().isEmpty()) {
                category.setDescription(description.trim());
            }
            String code = asString(body.get("code"));
            if (code != null && !code.trim().isEmpty()) {
                category.setCode(code.trim());
            }
            category.setCreater(user);
            category.setCreatedAt(new Date());
            categoryFacade.create(category);

            return Response.status(201).entity(gson.toJson(successData(toDto(category)))).build();

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // DTO / helpers
    // =========================================================================

    private Map<String, Object> toDto(Category c) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", c.getId());
        row.put("name", c.getName());
        row.put("code", c.getCode());
        row.put("description", c.getDescription());
        row.put("categoryType", c.getCategoryType() != null ? c.getCategoryType().name() : null);
        row.put("retired", c.isRetired());
        return row;
    }

    private String supportedTypesCsv() {
        StringBuilder sb = new StringBuilder();
        for (CategoryType t : CREATABLE_TYPES.keySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(t.name());
        }
        return sb.toString();
    }

    private String param(String name) {
        return uriInfo.getQueryParameters().getFirst(name);
    }

    private int intParam(String name, int defaultValue, int min, int max) {
        String v = param(name);
        if (v == null || v.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(v.trim());
            return Math.min(Math.max(parsed, min), max);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String asString(Object o) {
        if (o == null) {
            return null;
        }
        return o.toString();
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null || apiKey.isRetired()) {
            return null;
        }
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) {
            return null;
        }
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) {
            return null;
        }
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
