/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.lims;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.Machine;
import com.divudi.core.facade.MachineFacade;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API for Machine (Analyzer) management.
 * Closes #20967
 */
@Path("machines")
@RequestScoped
public class MachineApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private MachineFacade machineFacade;

    private static final Logger logger = Logger.getLogger(MachineApi.class.getName());

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * List machines with optional name search.
     * GET /api/machines?query=&size=
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String query = param("query");
            int size = intParam("size", 200, 1, 1000);

            StringBuilder jpql = new StringBuilder(
                    "select m from Machine m where m.retired = false");
            Map<String, Object> params = new HashMap<>();
            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and upper(m.name) like :q");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }
            jpql.append(" order by m.name");

            List<Machine> machines = machineFacade.findByJpql(jpql.toString(), params, size);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (machines != null) {
                for (Machine m : machines) {
                    payload.add(toDto(m));
                }
            }
            return successResponse(payload);

        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid argument in list()", e);
            return errorResponse("Invalid request", 400);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in list()", e);
            return errorResponse("Internal server error", 500);
        }
    }

    /**
     * Get a machine by ID.
     * GET /api/machines/{id}
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
            Machine machine = machineFacade.find(id);
            if (machine == null || machine.isRetired()) {
                return errorResponse("Machine not found: " + id, 404);
            }
            return successResponse(toDto(machine));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in getById()", e);
            return errorResponse("Internal server error", 500);
        }
    }

    /**
     * Create a new machine.
     * POST /api/machines
     * Body: { "name": "...", "code": "...", "description": "..." }
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

            Machine existing = machineFacade.findFirstByJpql(
                    "select m from Machine m where m.retired = false and upper(m.name) = :n",
                    Map.of("n", name.toUpperCase()));
            if (existing != null) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("status", "already_exists");
                payload.put("code", 409);
                payload.put("message", "A machine with this name already exists.");
                payload.put("id", existing.getId());
                return Response.status(409).entity(gson.toJson(payload)).build();
            }

            Machine machine = new Machine();
            machine.setName(name);
            String code = asString(body.get("code"));
            if (code != null && !code.trim().isEmpty()) {
                machine.setCode(code.trim());
            }
            String description = asString(body.get("description"));
            if (description != null && !description.trim().isEmpty()) {
                machine.setDescription(description.trim());
            }
            machine.setCreatedAt(new Date());
            machine.setCreater(user);
            machineFacade.create(machine);

            return Response.status(201).entity(gson.toJson(successData(201, toDto(machine)))).build();

        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid argument in create()", e);
            return errorResponse("Invalid request", 400);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in create()", e);
            return errorResponse("Internal server error", 500);
        }
    }

    /**
     * Update a machine by ID.
     * PUT /api/machines/{id}
     * Body: { "name": "...", "code": "...", "description": "..." }
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

            Machine machine = machineFacade.find(id);
            if (machine == null || machine.isRetired()) {
                return errorResponse("Machine not found: " + id, 404);
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

            if (body.containsKey("name")) {
                String name = asString(body.get("name"));
                if (name == null || name.trim().isEmpty()) {
                    return errorResponse("name cannot be empty", 400);
                }
                name = name.trim();
                Machine dup = machineFacade.findFirstByJpql(
                        "select m from Machine m where m.retired = false and upper(m.name) = :n and m.id <> :id",
                        Map.of("n", name.toUpperCase(), "id", id));
                if (dup != null) {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("status", "already_exists");
                    payload.put("code", 409);
                    payload.put("message", "Another machine with this name already exists.");
                    payload.put("id", dup.getId());
                    return Response.status(409).entity(gson.toJson(payload)).build();
                }
                machine.setName(name);
            }
            if (body.containsKey("code")) {
                String code = asString(body.get("code"));
                machine.setCode(code != null ? code.trim() : null);
            }
            if (body.containsKey("description")) {
                String description = asString(body.get("description"));
                machine.setDescription(description != null ? description.trim() : null);
            }

            machineFacade.edit(machine);
            return successResponse(toDto(machine));

        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid argument in update()", e);
            return errorResponse("Invalid request", 400);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in update()", e);
            return errorResponse("Internal server error", 500);
        }
    }

    /**
     * Soft-retire a machine by ID.
     * DELETE /api/machines/{id}?retireComments=reason
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

            Machine machine = machineFacade.find(id);
            if (machine == null) {
                return errorResponse("Machine not found: " + id, 404);
            }
            if (machine.isRetired()) {
                return errorResponse("Machine is already retired: " + id, 400);
            }

            machine.setRetired(true);
            machine.setRetiredAt(new Date());
            machine.setRetirer(user);
            String retireComments = param("retireComments");
            if (retireComments != null && !retireComments.trim().isEmpty()) {
                machine.setRetireComments(retireComments.trim());
            }
            machineFacade.edit(machine);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", machine.getId());
            resp.put("retired", true);
            return successResponse(resp);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in retire()", e);
            return errorResponse("Internal server error", 500);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Map<String, Object> toDto(Machine m) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", m.getId());
        row.put("name", m.getName());
        row.put("code", m.getCode());
        row.put("description", m.getDescription());
        row.put("retired", m.isRetired());
        return row;
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
        return successData(200, data);
    }

    private Map<String, Object> successData(int code, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", code);
        response.put("data", data);
        return response;
    }
}
