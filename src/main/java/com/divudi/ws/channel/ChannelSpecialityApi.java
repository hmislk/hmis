/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.channel;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.DoctorSpeciality;
import com.divudi.core.facade.DoctorSpecialityFacade;
import java.util.ArrayList;
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
import org.json.JSONObject;

/**
 * REST API for Doctor Speciality CRUD operations.
 *
 * GET    /api/channel/speciality          — list active specialities (supports ?query=&amp;page=&amp;size=)
 * POST   /api/channel/speciality          — create a new speciality
 * PUT    /api/channel/speciality/{id}     — update an existing speciality
 * DELETE /api/channel/speciality/{id}     — retire (soft delete) a speciality
 *
 * Authentication: Token header (same as /api/channel/consultant)
 */
@Path("channel/speciality")
@RequestScoped
public class ChannelSpecialityApi {

    @Context
    private UriInfo uriInfo;

    @EJB
    private DoctorSpecialityFacade doctorSpecialityFacade;

    @Inject
    private ApiKeyController apiKeyController;

    public ChannelSpecialityApi() {
    }

    /**
     * GET /api/channel/speciality
     *
     * Lists active DoctorSpeciality records.
     * Query params: query (name filter), page (0-based, default 0), size (default 20, max 200)
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listSpecialities(@Context HttpServletRequest requestContext) {
        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorMessageNotValidKey().toString()).build();
        }

        String query = uriInfo.getQueryParameters().getFirst("query");
        int page = Math.max(parseIntParam(uriInfo.getQueryParameters().getFirst("page"), 0), 0);
        int size = parseIntParam(uriInfo.getQueryParameters().getFirst("size"), 20);
        if (size < 1) {
            size = 20;
        }
        if (size > 200) {
            size = 200;
        }
        int offset = page * size;

        Map<String, Object> params = new HashMap<>();
        String jpql;
        if (query != null && !query.trim().isEmpty()) {
            jpql = "select d from DoctorSpeciality d where d.retired = false"
                    + " and upper(d.name) like :q order by d.name";
            params.put("q", "%" + query.trim().toUpperCase() + "%");
        } else {
            jpql = "select d from DoctorSpeciality d where d.retired = false order by d.name";
        }

        List<DoctorSpeciality> items = doctorSpecialityFacade.findByJpql(jpql, params, offset, offset + size - 1);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DoctorSpeciality ds : items) {
            result.add(toMap(ds));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("code", "200");
        response.put("message", "OK");
        response.put("data", result);
        return Response.ok(response).build();
    }

    /**
     * POST /api/channel/speciality
     *
     * Creates a new DoctorSpeciality.
     * Returns 200 with status "already_exists" if a non-retired speciality with the same name exists.
     *
     * Request body (JSON):
     *   { "name": "Consultant Anaesthetics", "code": "ANAES", "description": "Optional" }
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSpeciality(@Context HttpServletRequest requestContext, Map<String, Object> requestBody) {
        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorMessageNotValidKey().toString()).build();
        }

        String name = requestBody.get("name") != null ? requestBody.get("name").toString().trim() : "";
        if (name.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorResponse(400, "name is required").toString()).build();
        }

        String dupJpql = "select d from DoctorSpeciality d where d.retired = false and upper(d.name) = :n";
        Map<String, Object> dupParams = new HashMap<>();
        dupParams.put("n", name.toUpperCase());
        List<DoctorSpeciality> existing = doctorSpecialityFacade.findByJpql(dupJpql, dupParams);
        if (!existing.isEmpty()) {
            DoctorSpeciality dup = existing.get(0);
            Map<String, Object> dupResponse = new HashMap<>();
            dupResponse.put("status", "already_exists");
            dupResponse.put("id", dup.getId());
            dupResponse.put("name", dup.getName());
            return Response.ok(dupResponse).build();
        }

        DoctorSpeciality ds = new DoctorSpeciality();
        ds.setName(name);
        ds.setCreatedAt(new Date());
        if (requestBody.get("code") != null) {
            ds.setCode(requestBody.get("code").toString().trim());
        }
        if (requestBody.get("description") != null) {
            ds.setDescription(requestBody.get("description").toString().trim());
        }

        doctorSpecialityFacade.create(ds);

        Map<String, Object> data = toMap(ds);
        Map<String, Object> response = new HashMap<>();
        response.put("code", "201");
        response.put("message", "Created");
        response.put("data", data);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    /**
     * PUT /api/channel/speciality/{id}
     *
     * Updates an existing DoctorSpeciality by ID.
     * Only supplied fields (name, code, description) are changed.
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSpeciality(@Context HttpServletRequest requestContext,
            @PathParam("id") Long id,
            Map<String, Object> requestBody) {

        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorMessageNotValidKey().toString()).build();
        }

        DoctorSpeciality ds = doctorSpecialityFacade.find(id);
        if (ds == null || ds.isRetired()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse(404, "DoctorSpeciality not found for id: " + id).toString()).build();
        }

        if (requestBody.containsKey("name")) {
            Object v = requestBody.get("name");
            if (v != null && !v.toString().trim().isEmpty()) {
                ds.setName(v.toString().trim());
            }
        }
        if (requestBody.containsKey("code")) {
            Object v = requestBody.get("code");
            ds.setCode(v != null ? v.toString().trim() : null);
        }
        if (requestBody.containsKey("description")) {
            Object v = requestBody.get("description");
            ds.setDescription(v != null ? v.toString().trim() : null);
        }

        doctorSpecialityFacade.edit(ds);

        Map<String, Object> data = toMap(ds);
        Map<String, Object> response = new HashMap<>();
        response.put("code", "202");
        response.put("message", "Accepted");
        response.put("data", data);
        response.put("detailMessage", "DoctorSpeciality updated successfully");
        return Response.status(Response.Status.ACCEPTED).entity(response).build();
    }

    /**
     * DELETE /api/channel/speciality/{id}
     *
     * Soft-deletes (retires) a DoctorSpeciality by ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireSpeciality(@Context HttpServletRequest requestContext,
            @PathParam("id") Long id) {

        String key = requestContext.getHeader("Token");
        if (!isValidKey(key)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(errorMessageNotValidKey().toString()).build();
        }

        DoctorSpeciality ds = doctorSpecialityFacade.find(id);
        if (ds == null || ds.isRetired()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorResponse(404, "DoctorSpeciality not found for id: " + id).toString()).build();
        }

        ds.setRetired(true);
        ds.setRetiredAt(new Date());
        doctorSpecialityFacade.edit(ds);

        Map<String, Object> response = new HashMap<>();
        response.put("code", "200");
        response.put("message", "Retired");
        response.put("id", id);
        return Response.ok(response).build();
    }

    private Map<String, Object> toMap(DoctorSpeciality ds) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", ds.getId());
        m.put("name", ds.getName() != null ? ds.getName() : "");
        m.put("code", ds.getCode() != null ? ds.getCode() : "");
        m.put("description", ds.getDescription() != null ? ds.getDescription() : "");
        return m;
    }

    private boolean isValidKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        ApiKey k = apiKeyController.findApiKey(key);
        if (k == null) {
            return false;
        }
        if (k.getWebUser() == null) {
            return false;
        }
        if (k.getWebUser().isRetired()) {
            return false;
        }
        if (!k.getWebUser().isActivated()) {
            return false;
        }
        if (k.getDateOfExpiary().before(new Date())) {
            return false;
        }
        return true;
    }

    private JSONObject errorResponse(int httpStatusCode, String msg) {
        JSONObject j = new JSONObject();
        j.put("code", httpStatusCode);
        j.put("type", "Error");
        j.put("message", msg);
        return j;
    }

    private JSONObject errorMessageNotValidKey() {
        JSONObject j = new JSONObject();
        j.put("code", 401);
        j.put("type", "error");
        j.put("message", "Not a valid key.");
        return j;
    }

    private int parseIntParam(String s, int defaultVal) {
        if (s == null || s.trim().isEmpty()) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
