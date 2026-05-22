/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.lims;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.ItemType;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.Machine;
import com.divudi.core.facade.ItemFacade;
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

/**
 * REST API for Analyzer Test Item management (Items with itemType=AnalyzerTest linked to a Machine).
 * Closes #20968
 */
@Path("machines/{machineId}/tests")
@RequestScoped
public class AnalyzerTestApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private ItemFacade itemFacade;

    @EJB
    private MachineFacade machineFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * List analyzer tests for a machine.
     * GET /api/machines/{machineId}/tests?query=&size=
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@PathParam("machineId") Long machineId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Machine machine = machineFacade.find(machineId);
            if (machine == null || machine.isRetired()) {
                return errorResponse("Machine not found: " + machineId, 404);
            }

            String query = param("query");
            int size = intParam("size", 200, 1, 1000);

            StringBuilder jpql = new StringBuilder(
                    "select i from Item i where i.retired = false"
                    + " and i.itemType = :t and i.machine = :m");
            Map<String, Object> params = new HashMap<>();
            params.put("t", ItemType.AnalyzerTest);
            params.put("m", machine);
            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and (upper(i.name) like :q or upper(i.code) like :q)");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }
            jpql.append(" order by i.name");

            List<Item> items = itemFacade.findByJpql(jpql.toString(), params, size);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (items != null) {
                for (Item item : items) {
                    payload.add(toDto(item));
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
     * Get an analyzer test by ID.
     * GET /api/machines/{machineId}/tests/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("machineId") Long machineId, @PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Machine machine = machineFacade.find(machineId);
            if (machine == null || machine.isRetired()) {
                return errorResponse("Machine not found: " + machineId, 404);
            }

            Item item = itemFacade.find(id);
            if (item == null || item.isRetired()) {
                return errorResponse("Analyzer test not found: " + id, 404);
            }
            if (item.getMachine() == null || !item.getMachine().getId().equals(machineId)) {
                return errorResponse("Analyzer test " + id + " does not belong to machine " + machineId, 404);
            }

            return successResponse(toDto(item));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create an analyzer test for a machine.
     * POST /api/machines/{machineId}/tests
     * Body: { "name": "...", "code": "..." }
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@PathParam("machineId") Long machineId, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Machine machine = machineFacade.find(machineId);
            if (machine == null || machine.isRetired()) {
                return errorResponse("Machine not found: " + machineId, 404);
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

            String code = asString(body.get("code"));
            if (code == null || code.trim().isEmpty()) {
                return errorResponse("code is required", 400);
            }
            code = code.trim();

            Item existing = itemFacade.findFirstByJpql(
                    "select i from Item i where i.retired = false"
                    + " and i.itemType = :t and i.machine = :m and upper(i.code) = :c",
                    Map.of("t", ItemType.AnalyzerTest, "m", machine, "c", code.toUpperCase()));
            if (existing != null) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("status", "already_exists");
                payload.put("code", 409);
                payload.put("message", "An analyzer test with this code already exists for this machine.");
                payload.put("id", existing.getId());
                return Response.status(409).entity(gson.toJson(payload)).build();
            }

            Item item = new Item();
            item.setName(name);
            item.setCode(code);
            item.setMachine(machine);
            item.setItemType(ItemType.AnalyzerTest);
            item.setCreatedAt(new Date());
            item.setCreater(user);
            itemFacade.create(item);

            return Response.status(201).entity(gson.toJson(successData(toDto(item)))).build();

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update an analyzer test by ID.
     * PUT /api/machines/{machineId}/tests/{id}
     * Body: { "name": "...", "code": "..." }
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("machineId") Long machineId, @PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Machine machine = machineFacade.find(machineId);
            if (machine == null || machine.isRetired()) {
                return errorResponse("Machine not found: " + machineId, 404);
            }

            Item item = itemFacade.find(id);
            if (item == null || item.isRetired()) {
                return errorResponse("Analyzer test not found: " + id, 404);
            }
            if (item.getMachine() == null || !item.getMachine().getId().equals(machineId)) {
                return errorResponse("Analyzer test " + id + " does not belong to machine " + machineId, 404);
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
                item.setName(name.trim());
            }
            if (body.containsKey("code")) {
                String code = asString(body.get("code"));
                if (code == null || code.trim().isEmpty()) {
                    return errorResponse("code cannot be empty", 400);
                }
                code = code.trim();
                Item dup = itemFacade.findFirstByJpql(
                        "select i from Item i where i.retired = false"
                        + " and i.itemType = :t and i.machine = :m and upper(i.code) = :c and i.id <> :id",
                        Map.of("t", ItemType.AnalyzerTest, "m", machine, "c", code.toUpperCase(), "id", id));
                if (dup != null) {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("status", "already_exists");
                    payload.put("code", 409);
                    payload.put("message", "Another analyzer test with this code already exists for this machine.");
                    payload.put("id", dup.getId());
                    return Response.status(409).entity(gson.toJson(payload)).build();
                }
                item.setCode(code);
            }

            itemFacade.edit(item);
            return successResponse(toDto(item));

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Soft-retire an analyzer test by ID.
     * DELETE /api/machines/{machineId}/tests/{id}?retireComments=reason
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retire(@PathParam("machineId") Long machineId, @PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Machine machine = machineFacade.find(machineId);
            if (machine == null || machine.isRetired()) {
                return errorResponse("Machine not found: " + machineId, 404);
            }

            Item item = itemFacade.find(id);
            if (item == null) {
                return errorResponse("Analyzer test not found: " + id, 404);
            }
            if (item.isRetired()) {
                return errorResponse("Analyzer test is already retired: " + id, 400);
            }
            if (item.getMachine() == null || !item.getMachine().getId().equals(machineId)) {
                return errorResponse("Analyzer test " + id + " does not belong to machine " + machineId, 404);
            }

            item.setRetired(true);
            item.setRetiredAt(new Date());
            item.setRetirer(user);
            String retireComments = param("retireComments");
            if (retireComments != null && !retireComments.trim().isEmpty()) {
                item.setRetireComments(retireComments.trim());
            }
            itemFacade.edit(item);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", item.getId());
            resp.put("retired", true);
            return successResponse(resp);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Map<String, Object> toDto(Item i) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", i.getId());
        row.put("name", i.getName());
        row.put("code", i.getCode());
        row.put("retired", i.isRetired());
        if (i.getMachine() != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getMachine().getId());
            m.put("name", i.getMachine().getName());
            row.put("machine", m);
        } else {
            row.put("machine", null);
        }
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
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return response;
    }
}
