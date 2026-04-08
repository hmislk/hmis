/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.clinical;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.ApiKeyType;
import com.divudi.core.data.SymanticType;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Vocabulary;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.clinical.ClinicalEntity;
import com.divudi.core.facade.ClinicalEntityFacade;
import com.divudi.core.facade.VocabularyFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;

/**
 * REST API for EMR Clinical Metadata management.
 *
 * Covers all types in the Clinical Metadata section of /emr/admin/index.xhtml:
 * symptoms, signs, diagnosis, procedures, plans, vocabularies, and clinical
 * entities (race, religion, blood_group, civil_status, employment, relationship).
 *
 * All operations require a valid Finance API key header.
 */
@Path("clinical/metadata")
@RequestScoped
public class ClinicalMetadataApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private ClinicalEntityFacade clinicalEntityFacade;

    @EJB
    private VocabularyFacade vocabularyFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    // -------------------------------------------------------------------------
    // GET /api/clinical/metadata?type=X[&query=text&page=0&size=20]
    // -------------------------------------------------------------------------

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            String key = requestContext.getHeader("Finance");
            if (validateApiKey(key) == null) {
                return errorResponse("Not a valid key", 401);
            }

            String typeStr = param("type");
            if (typeStr == null || typeStr.trim().isEmpty()) {
                return errorResponse("type parameter is required", 400);
            }

            String query = param("query");
            int page = Math.max(parseInt(param("page"), 0), 0);
            int size = Math.min(Math.max(parseInt(param("size"), 20), 1), 200);
            int offset = page * size;

            if ("vocabulary".equalsIgnoreCase(typeStr)) {
                return listVocabularies(query, offset, size);
            }

            SymanticType st = resolveSymanticType(typeStr);
            if (st == null) {
                return errorResponse("Unknown type: " + typeStr, 400);
            }

            return listClinicalEntities(st, typeStr.toLowerCase(), query, offset, size);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    private Response listClinicalEntities(SymanticType st, String typeLabel, String query, int offset, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("t", st);
        String jpql;
        if (query != null && !query.trim().isEmpty()) {
            jpql = "select c from ClinicalEntity c where c.retired=false and c.symanticType=:t"
                    + " and upper(c.name) like :n order by c.name";
            params.put("n", "%" + query.trim().toUpperCase() + "%");
        } else {
            jpql = "select c from ClinicalEntity c where c.retired=false and c.symanticType=:t order by c.name";
        }
        List<ClinicalEntity> entities = clinicalEntityFacade.findByJpql(jpql, params, offset, offset + size - 1);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ClinicalEntity e : entities) {
            result.add(toMap(e, typeLabel));
        }
        return successResponse(result);
    }

    private Response listVocabularies(String query, int offset, int size) {
        Map<String, Object> params = new HashMap<>();
        String jpql;
        if (query != null && !query.trim().isEmpty()) {
            jpql = "select v from Vocabulary v where v.retired=false and upper(v.name) like :n order by v.name";
            params.put("n", "%" + query.trim().toUpperCase() + "%");
        } else {
            jpql = "select v from Vocabulary v where v.retired=false order by v.name";
        }
        List<Vocabulary> vocabs = vocabularyFacade.findByJpql(jpql, params, offset, offset + size - 1);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Vocabulary v : vocabs) {
            result.add(toMap(v));
        }
        return successResponse(result);
    }

    // -------------------------------------------------------------------------
    // POST /api/clinical/metadata?type=X   body: {name, code, description}
    // -------------------------------------------------------------------------

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            if (validateApiKey(key) == null) {
                return errorResponse("Not a valid key", 401);
            }

            String typeStr = param("type");
            if (typeStr == null || typeStr.trim().isEmpty()) {
                return errorResponse("type parameter is required", 400);
            }

            Map<String, String> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON: " + e.getMessage(), 400);
            }
            if (body == null) {
                return errorResponse("Request body is required", 400);
            }

            String name = body.get("name");
            if (name == null || name.trim().isEmpty()) {
                return errorResponse("name is required", 400);
            }
            name = name.trim();
            String code = body.get("code");
            String description = body.get("description");

            if ("vocabulary".equalsIgnoreCase(typeStr)) {
                return createVocabulary(name, code, description);
            }

            SymanticType st = resolveSymanticType(typeStr);
            if (st == null) {
                return errorResponse("Unknown type: " + typeStr, 400);
            }

            return createClinicalEntity(st, typeStr.toLowerCase(), name, code, description);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    private Response createClinicalEntity(SymanticType st, String typeLabel, String name, String code, String description) {
        // Duplicate check
        Map<String, Object> params = new HashMap<>();
        params.put("t", st);
        params.put("n", name.toUpperCase());
        ClinicalEntity existing = clinicalEntityFacade.findFirstByJpql(
                "select c from ClinicalEntity c where c.retired=false and c.symanticType=:t and upper(c.name)=:n",
                params);
        if (existing != null) {
            Map<String, Object> found = new HashMap<>();
            found.put("status", "already_exists");
            found.put("id", existing.getId());
            found.put("name", existing.getName());
            found.put("type", typeLabel);
            return Response.ok(gson.toJson(found)).build();
        }

        ClinicalEntity entity = new ClinicalEntity();
        entity.setName(name);
        entity.setCode(code);
        entity.setDescreption(description);
        entity.setSymanticType(st);
        entity.setCreatedAt(new Date());
        entity.setRetired(false);
        clinicalEntityFacade.create(entity);

        return successResponse(toMap(entity, typeLabel));
    }

    private Response createVocabulary(String name, String code, String description) {
        // Duplicate check
        Map<String, Object> params = new HashMap<>();
        params.put("n", name.toUpperCase());
        Vocabulary existing = vocabularyFacade.findFirstByJpql(
                "select v from Vocabulary v where v.retired=false and upper(v.name)=:n",
                params);
        if (existing != null) {
            Map<String, Object> found = new HashMap<>();
            found.put("status", "already_exists");
            found.put("id", existing.getId());
            found.put("name", existing.getName());
            found.put("type", "vocabulary");
            return Response.ok(gson.toJson(found)).build();
        }

        Vocabulary vocab = new Vocabulary();
        vocab.setName(name);
        vocab.setCode(code);
        vocab.setDescription(description);
        vocab.setRetired(false);
        vocabularyFacade.create(vocab);

        return successResponse(toMap(vocab));
    }

    // -------------------------------------------------------------------------
    // PUT /api/clinical/metadata/{id}   body: {name, code, description}
    // -------------------------------------------------------------------------

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") Long id, String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            if (validateApiKey(key) == null) {
                return errorResponse("Not a valid key", 401);
            }

            String typeStr = param("type");
            if (typeStr == null || typeStr.trim().isEmpty()) {
                return errorResponse("type parameter is required", 400);
            }

            Map<String, String> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON: " + e.getMessage(), 400);
            }
            if (body == null) {
                return errorResponse("Request body is required", 400);
            }

            if ("vocabulary".equalsIgnoreCase(typeStr)) {
                Vocabulary vocab = vocabularyFacade.find(id);
                if (vocab == null || vocab.isRetired()) {
                    return errorResponse("Record not found with id: " + id, 404);
                }
                if (body.containsKey("name") && body.get("name") != null) {
                    vocab.setName(body.get("name").trim());
                }
                if (body.containsKey("code")) {
                    vocab.setCode(body.get("code"));
                }
                if (body.containsKey("description")) {
                    vocab.setDescription(body.get("description"));
                }
                vocabularyFacade.edit(vocab);
                return successResponse(toMap(vocab));
            }

            SymanticType st = resolveSymanticType(typeStr);
            if (st == null) {
                return errorResponse("Unknown type: " + typeStr, 400);
            }

            ClinicalEntity ce = clinicalEntityFacade.find(id);
            if (ce != null && !ce.isRetired() && ce.getSymanticType() == st) {
                if (body.containsKey("name") && body.get("name") != null) {
                    ce.setName(body.get("name").trim());
                }
                if (body.containsKey("code")) {
                    ce.setCode(body.get("code"));
                }
                if (body.containsKey("description")) {
                    ce.setDescreption(body.get("description"));
                }
                clinicalEntityFacade.edit(ce);
                return successResponse(toMap(ce, typeLabel(ce.getSymanticType())));
            }

            return errorResponse("Record not found with id: " + id, 404);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/clinical/metadata/{id}
    // -------------------------------------------------------------------------

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        try {
            String key = requestContext.getHeader("Finance");
            if (validateApiKey(key) == null) {
                return errorResponse("Not a valid key", 401);
            }

            ClinicalEntity ce = clinicalEntityFacade.find(id);
            if (ce != null && !ce.isRetired()) {
                ce.setRetired(true);
                ce.setRetiredAt(new Date());
                clinicalEntityFacade.edit(ce);
                Map<String, Object> result = new HashMap<>();
                result.put("id", ce.getId());
                result.put("deleted", true);
                return successResponse(result);
            }

            Vocabulary vocab = vocabularyFacade.find(id);
            if (vocab != null && !vocab.isRetired()) {
                vocab.setRetired(true);
                vocab.setRetiredAt(new Date());
                vocabularyFacade.edit(vocab);
                Map<String, Object> result = new HashMap<>();
                result.put("id", vocab.getId());
                result.put("deleted", true);
                return successResponse(result);
            }

            return errorResponse("Record not found with id: " + id, 404);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SymanticType resolveSymanticType(String type) {
        if (type == null) return null;
        switch (type.toLowerCase()) {
            case "symptom":     return SymanticType.Symptom;
            case "sign":        return SymanticType.Sign;
            case "diagnosis":   return SymanticType.Disease_or_Syndrome;
            case "procedure":   return SymanticType.Therapeutic_Procedure;
            case "plan":        return SymanticType.Preventive_Procedure;
            case "race":        return SymanticType.Race;
            case "religion":    return SymanticType.Religion;
            case "employment":  return SymanticType.Employment;
            case "blood_group": return SymanticType.Blood_Group;
            case "civil_status":return SymanticType.Civil_status;
            case "relationship":return SymanticType.Relationships;
            default:            return null;
        }
    }

    private String typeLabel(SymanticType st) {
        if (st == null) return "unknown";
        switch (st) {
            case Symptom:                return "symptom";
            case Sign:                   return "sign";
            case Disease_or_Syndrome:    return "diagnosis";
            case Therapeutic_Procedure:  return "procedure";
            case Preventive_Procedure:   return "plan";
            case Race:                   return "race";
            case Religion:               return "religion";
            case Employment:             return "employment";
            case Blood_Group:            return "blood_group";
            case Civil_status:           return "civil_status";
            case Relationships:          return "relationship";
            default:                     return st.name().toLowerCase();
        }
    }

    private Map<String, Object> toMap(ClinicalEntity e, String typeLabel) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("name", e.getName());
        m.put("code", e.getCode());
        m.put("description", e.getDescreption());
        m.put("type", typeLabel);
        return m;
    }

    private Map<String, Object> toMap(Vocabulary v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("name", v.getName());
        m.put("code", v.getCode());
        m.put("description", v.getDescription());
        m.put("type", "vocabulary");
        return m;
    }

    private String param(String key) {
        return uriInfo.getQueryParameters().getFirst(key);
    }

    private int parseInt(String value, int defaultVal) {
        if (value == null) return defaultVal;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) return null;
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) return null;
        if (apiKey.isRetired()) return null;
        if (apiKey.getKeyType() == ApiKeyType.Config) return new WebUser();
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) return null;
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
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.status(200).entity(gson.toJson(response)).build();
    }
}
