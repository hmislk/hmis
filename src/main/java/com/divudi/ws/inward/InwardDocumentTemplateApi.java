package com.divudi.ws.inward;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.clinical.DocumentTemplateType;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.clinical.DocumentTemplate;
import com.divudi.core.facade.DocumentTemplateFacade;
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
 * REST API for inpatient document template management (InpatientDiagnosisCard,
 * InpatientLetter, and any future inpatient DocumentTemplateType values).
 *
 * All endpoints require the Finance API key header.
 *
 * @author Dr M H B Ariyaratne
 */
@Path("inward/document-templates")
@RequestScoped
public class InwardDocumentTemplateApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private DocumentTemplateFacade documentTemplateFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private static final List<DocumentTemplateType> INPATIENT_TYPES = Arrays.asList(
            DocumentTemplateType.InpatientDiagnosisCard,
            DocumentTemplateType.InpatientLetter
    );

    // =========================================================================
    // GET /api/inward/document-templates
    // =========================================================================

    /**
     * List inpatient document templates.
     * Optional query params: type (InpatientDiagnosisCard | InpatientLetter),
     * query (name search), size (max results, default 200).
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String typeParam = param("type");
            String query = param("query");
            int size = intParam("size", 200, 1, 1000);

            DocumentTemplateType filterType = null;
            if (typeParam != null && !typeParam.trim().isEmpty()) {
                try {
                    filterType = DocumentTemplateType.valueOf(typeParam.trim());
                    if (!INPATIENT_TYPES.contains(filterType)) {
                        return errorResponse("type must be one of: InpatientDiagnosisCard, InpatientLetter", 400);
                    }
                } catch (IllegalArgumentException e) {
                    return errorResponse("Unknown type: " + typeParam + ". Valid values: InpatientDiagnosisCard, InpatientLetter", 400);
                }
            }

            StringBuilder jpql = new StringBuilder(
                    "select t from DocumentTemplate t where t.retired = false");
            Map<String, Object> params = new HashMap<>();

            if (filterType != null) {
                jpql.append(" and t.type = :type");
                params.put("type", filterType);
            } else {
                jpql.append(" and t.type in :types");
                params.put("types", INPATIENT_TYPES);
            }

            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and upper(t.name) like :q");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }
            jpql.append(" order by t.type, t.name");

            List<DocumentTemplate> items = documentTemplateFacade.findByJpql(jpql.toString(), params, size);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (items != null) {
                for (DocumentTemplate t : items) {
                    payload.add(toDto(t, false));
                }
            }
            return successResponse(payload);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // GET /api/inward/document-templates/{id}
    // =========================================================================

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            DocumentTemplate t = documentTemplateFacade.find(id);
            if (t == null || t.isRetired()) {
                return errorResponse("Document template not found: " + id, 404);
            }
            if (!INPATIENT_TYPES.contains(t.getType())) {
                return errorResponse("Document template not found: " + id, 404);
            }
            return successResponse(toDto(t, true));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // POST /api/inward/document-templates
    // =========================================================================

    /**
     * Create a new inpatient document template.
     * Body: { "name": "...", "type": "InpatientLetter|InpatientDiagnosisCard",
     * "contents": "...", "defaultTemplate": false, "autoGenerate": false }
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

            Map<?, ?> body = parseBody(requestBody);
            if (body == null) return errorResponse("Request body is required", 400);

            String name = asString(body.get("name"));
            if (name == null || name.trim().isEmpty()) return errorResponse("name is required", 400);
            name = name.trim();

            String typeStr = asString(body.get("type"));
            if (typeStr == null || typeStr.trim().isEmpty()) return errorResponse("type is required", 400);
            DocumentTemplateType type;
            try {
                type = DocumentTemplateType.valueOf(typeStr.trim());
                if (!INPATIENT_TYPES.contains(type)) {
                    return errorResponse("type must be one of: InpatientDiagnosisCard, InpatientLetter", 400);
                }
            } catch (IllegalArgumentException e) {
                return errorResponse("Unknown type: " + typeStr + ". Valid values: InpatientDiagnosisCard, InpatientLetter", 400);
            }

            DocumentTemplate t = new DocumentTemplate();
            t.setName(name);
            t.setType(type);
            String contents = asString(body.get("contents"));
            if (contents != null) t.setContents(contents);
            if (body.containsKey("defaultTemplate")) t.setDefaultTemplate(Boolean.parseBoolean(asString(body.get("defaultTemplate"))));
            if (body.containsKey("autoGenerate")) t.setAutoGenerate(Boolean.parseBoolean(asString(body.get("autoGenerate"))));
            t.setCreater(user);
            t.setCreatedAt(new Date());
            documentTemplateFacade.create(t);

            return Response.status(201).entity(gson.toJson(successData(toDto(t, true)))).build();

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // PUT /api/inward/document-templates/{id}
    // =========================================================================

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

            DocumentTemplate t = documentTemplateFacade.find(id);
            if (t == null || t.isRetired()) return errorResponse("Document template not found: " + id, 404);
            if (!INPATIENT_TYPES.contains(t.getType())) return errorResponse("Document template not found: " + id, 404);

            Map<?, ?> body = parseBody(requestBody);
            if (body == null) return errorResponse("Request body is required", 400);

            if (body.containsKey("name")) {
                String name = asString(body.get("name"));
                if (name == null || name.trim().isEmpty()) return errorResponse("name cannot be empty", 400);
                t.setName(name.trim());
            }
            if (body.containsKey("type")) {
                String typeStr = asString(body.get("type"));
                try {
                    DocumentTemplateType newType = DocumentTemplateType.valueOf(typeStr.trim());
                    if (!INPATIENT_TYPES.contains(newType)) return errorResponse("type must be InpatientDiagnosisCard or InpatientLetter", 400);
                    t.setType(newType);
                } catch (IllegalArgumentException e) {
                    return errorResponse("Unknown type: " + typeStr, 400);
                }
            }
            if (body.containsKey("contents")) {
                t.setContents(asString(body.get("contents")));
            }
            if (body.containsKey("defaultTemplate")) {
                t.setDefaultTemplate(Boolean.parseBoolean(asString(body.get("defaultTemplate"))));
            }
            if (body.containsKey("autoGenerate")) {
                t.setAutoGenerate(Boolean.parseBoolean(asString(body.get("autoGenerate"))));
            }

            documentTemplateFacade.edit(t);
            return successResponse(toDto(t, true));

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // DELETE /api/inward/document-templates/{id}
    // =========================================================================

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retire(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            DocumentTemplate t = documentTemplateFacade.find(id);
            if (t == null) return errorResponse("Document template not found: " + id, 404);
            if (t.isRetired()) return errorResponse("Document template is already retired: " + id, 400);
            if (!INPATIENT_TYPES.contains(t.getType())) return errorResponse("Document template not found: " + id, 404);

            t.setRetired(true);
            documentTemplateFacade.edit(t);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", t.getId());
            resp.put("retired", true);
            return successResponse(resp);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Map<String, Object> toDto(DocumentTemplate t, boolean includeContents) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", t.getId());
        row.put("name", t.getName());
        row.put("type", t.getType() != null ? t.getType().name() : null);
        row.put("defaultTemplate", t.isDefaultTemplate());
        row.put("autoGenerate", t.isAutoGenerate());
        row.put("createdAt", t.getCreatedAt());
        if (includeContents) {
            row.put("contents", t.getContents());
        }
        return row;
    }

    private Map<?, ?> parseBody(String body) {
        if (body == null || body.trim().isEmpty()) return null;
        try {
            return gson.fromJson(body, Map.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    private String param(String name) {
        return uriInfo.getQueryParameters().getFirst(name);
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
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return response;
    }
}
