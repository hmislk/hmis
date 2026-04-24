/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.inward;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.Room;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.facade.RoomCategoryFacade;
import com.divudi.core.facade.RoomFacade;
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
 * REST API for Inward Room management.
 * Backs the UI page /inward/inward_room.xhtml.
 *
 * @author Dr M H B Ariyaratne
 */
@Path("inward/rooms")
@RequestScoped
public class InwardRoomApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private RoomFacade roomFacade;

    @EJB
    private RoomCategoryFacade roomCategoryFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    // =========================================================================
    // CRUD
    // =========================================================================

    /**
     * List rooms with optional filters.
     * GET /api/inward/rooms?query=&roomCategoryId=&size=
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
            Long roomCategoryId = longParam("roomCategoryId");
            int size = intParam("size", 200, 1, 1000);

            StringBuilder jpql = new StringBuilder(
                    "select r from Room r where r.retired = false");
            Map<String, Object> params = new HashMap<>();
            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and upper(r.name) like :q");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }
            if (roomCategoryId != null) {
                jpql.append(" and r.parentCategory.id = :rcid");
                params.put("rcid", roomCategoryId);
            }
            jpql.append(" order by r.name");

            List<Room> items = roomFacade.findByJpql(jpql.toString(), params, size);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (items != null) {
                for (Room r : items) {
                    payload.add(toDto(r));
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
     * Fetch one room by ID.
     * GET /api/inward/rooms/{id}
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
            Room room = roomFacade.find(id);
            if (room == null || room.isRetired()) {
                return errorResponse("Room not found: " + id, 404);
            }
            return successResponse(toDto(room));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create a new room.
     * POST /api/inward/rooms
     * Body: { "name": "...", "code": "...", "description": "...", "roomCategoryId": 1, "filled": false }
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

            Room existing = roomFacade.findFirstByJpql(
                    "select r from Room r where r.retired = false and upper(r.name) = :n",
                    Map.of("n", name.toUpperCase()));
            if (existing != null) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("status", "already_exists");
                payload.put("code", 409);
                payload.put("message", "A room with this name already exists.");
                payload.put("id", existing.getId());
                return Response.status(409).entity(gson.toJson(payload)).build();
            }

            RoomCategory roomCategory = null;
            Long roomCategoryId = asLong(body.get("roomCategoryId"));
            if (roomCategoryId != null) {
                roomCategory = roomCategoryFacade.find(roomCategoryId);
                if (roomCategory == null || roomCategory.isRetired()) {
                    return errorResponse("RoomCategory not found: " + roomCategoryId, 400);
                }
            }

            Room room = new Room();
            room.setName(name);
            String code = asString(body.get("code"));
            if (code != null && !code.trim().isEmpty()) {
                room.setCode(code.trim());
            }
            String description = asString(body.get("description"));
            if (description != null && !description.trim().isEmpty()) {
                room.setDescription(description.trim());
            }
            room.setParentCategory(roomCategory);
            if (body.containsKey("filled")) {
                room.setFilled(asBoolean(body.get("filled")));
            }
            room.setCreatedAt(new Date());
            room.setCreater(user);
            roomFacade.create(room);

            return Response.status(201).entity(gson.toJson(successData(toDto(room)))).build();

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update a room by ID.
     * PUT /api/inward/rooms/{id}
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

            Room room = roomFacade.find(id);
            if (room == null || room.isRetired()) {
                return errorResponse("Room not found: " + id, 404);
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
                Room dup = roomFacade.findFirstByJpql(
                        "select r from Room r where r.retired = false and upper(r.name) = :n and r.id <> :id",
                        Map.of("n", name.toUpperCase(), "id", id));
                if (dup != null) {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("status", "already_exists");
                    payload.put("code", 409);
                    payload.put("message", "Another room with this name already exists.");
                    payload.put("id", dup.getId());
                    return Response.status(409).entity(gson.toJson(payload)).build();
                }
                room.setName(name);
            }
            if (body.containsKey("code")) {
                String code = asString(body.get("code"));
                room.setCode(code != null ? code.trim() : null);
            }
            if (body.containsKey("description")) {
                String description = asString(body.get("description"));
                room.setDescription(description != null ? description.trim() : null);
            }
            if (body.containsKey("roomCategoryId")) {
                Long roomCategoryId = asLong(body.get("roomCategoryId"));
                if (roomCategoryId == null) {
                    room.setParentCategory(null);
                } else {
                    RoomCategory roomCategory = roomCategoryFacade.find(roomCategoryId);
                    if (roomCategory == null || roomCategory.isRetired()) {
                        return errorResponse("RoomCategory not found: " + roomCategoryId, 400);
                    }
                    room.setParentCategory(roomCategory);
                }
            }
            if (body.containsKey("filled")) {
                room.setFilled(asBoolean(body.get("filled")));
            }

            roomFacade.edit(room);
            return successResponse(toDto(room));

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Soft-retire a room by ID.
     * DELETE /api/inward/rooms/{id}?retireComments=reason
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

            Room room = roomFacade.find(id);
            if (room == null) {
                return errorResponse("Room not found: " + id, 404);
            }
            if (room.isRetired()) {
                return errorResponse("Room is already retired: " + id, 400);
            }

            room.setRetired(true);
            room.setRetiredAt(new Date());
            room.setRetirer(user);
            String retireComments = param("retireComments");
            if (retireComments != null && !retireComments.trim().isEmpty()) {
                room.setRetireComments(retireComments.trim());
            }
            roomFacade.edit(room);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", room.getId());
            resp.put("retired", true);
            return successResponse(resp);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Map<String, Object> toDto(Room r) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", r.getId());
        row.put("name", r.getName());
        row.put("code", r.getCode());
        row.put("description", r.getDescription());
        row.put("filled", r.isFilled());
        row.put("retired", r.isRetired());
        if (r.getParentCategory() != null) {
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("id", r.getParentCategory().getId());
            cat.put("name", r.getParentCategory().getName());
            row.put("roomCategory", cat);
        } else {
            row.put("roomCategory", null);
        }
        return row;
    }

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

    private boolean asBoolean(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        String s = o.toString().trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s);
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
