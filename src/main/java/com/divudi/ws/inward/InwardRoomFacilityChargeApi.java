/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.inward;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.inward.TimedItemDurationUnit;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.Room;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.entity.inward.RoomFacilityTimedItem;
import com.divudi.core.entity.inward.TimedItem;
import com.divudi.core.entity.inward.TimedItemFee;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.RoomCategoryFacade;
import com.divudi.core.facade.RoomFacade;
import com.divudi.core.facade.RoomFacilityChargeFacade;
import com.divudi.core.facade.RoomFacilityTimedItemFacade;
import com.divudi.core.facade.TimedItemFacade;
import com.divudi.core.facade.TimedItemFeeFacade;
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
 * REST API for Inward Room Facility Charge management.
 * Backs the UI page /inward/inward_room_facility.xhtml (Manage Room Charges).
 *
 * @author Dr M H B Ariyaratne
 */
@Path("inward/room-facility-charges")
@RequestScoped
public class InwardRoomFacilityChargeApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private RoomFacilityChargeFacade roomFacilityChargeFacade;

    @EJB
    private RoomFacade roomFacade;

    @EJB
    private RoomCategoryFacade roomCategoryFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private TimedItemFeeFacade timedItemFeeFacade;

    @EJB
    private RoomFacilityTimedItemFacade roomFacilityTimedItemFacade;

    @EJB
    private TimedItemFacade timedItemFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    // =========================================================================
    // CRUD
    // =========================================================================

    /**
     * List room facility charges with optional filters.
     * GET /api/inward/room-facility-charges?query=&roomId=&roomCategoryId=&size=
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
            Long roomId = longParam("roomId");
            Long roomCategoryId = longParam("roomCategoryId");
            int size = intParam("size", 200, 1, 1000);

            StringBuilder jpql = new StringBuilder(
                    "select r from RoomFacilityCharge r where r.retired = false");
            Map<String, Object> params = new HashMap<>();
            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and upper(r.name) like :q");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }
            if (roomId != null) {
                jpql.append(" and r.room.id = :rid");
                params.put("rid", roomId);
            }
            if (roomCategoryId != null) {
                jpql.append(" and r.roomCategory.id = :rcid");
                params.put("rcid", roomCategoryId);
            }
            jpql.append(" order by r.name");

            List<RoomFacilityCharge> items = roomFacilityChargeFacade.findByJpql(jpql.toString(), params, size);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (items != null) {
                for (RoomFacilityCharge r : items) {
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
     * Fetch one room facility charge by ID.
     * GET /api/inward/room-facility-charges/{id}
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
            RoomFacilityCharge charge = roomFacilityChargeFacade.find(id);
            if (charge == null || charge.isRetired()) {
                return errorResponse("Room facility charge not found: " + id, 404);
            }
            return successResponse(toDto(charge));
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Create a new room facility charge.
     * POST /api/inward/room-facility-charges
     * Body: { "name" (required), "departmentId" (required), "roomId", "roomCategoryId",
     *         "roomCharge", "maintananceCharge", "linenCharge", "nursingCharge",
     *         "moCharge", "moChargeForAfterDuration", "adminstrationCharge", "medicalCareCharge",
     *         "timedItemFeeDurationHours", "timedItemFeeOverShootHours", "timedItemFeeDurationDaysForMoCharge",
     *         "timedItemFeeDurationUnit" (ONE_TIME | MINUTE | HOUR | DAY, defaults to HOUR) }
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

            Room room = null;
            Long roomId = asLong(body.get("roomId"));
            if (roomId != null) {
                room = roomFacade.find(roomId);
                if (room == null || room.isRetired()) {
                    return errorResponse("Room not found: " + roomId, 400);
                }
            }

            RoomCategory roomCategory = null;
            Long roomCategoryId = asLong(body.get("roomCategoryId"));
            if (roomCategoryId != null) {
                roomCategory = roomCategoryFacade.find(roomCategoryId);
                if (roomCategory == null || roomCategory.isRetired()) {
                    return errorResponse("RoomCategory not found: " + roomCategoryId, 400);
                }
            }

            Long departmentId = asLong(body.get("departmentId"));
            if (departmentId == null) {
                return errorResponse("departmentId is required", 400);
            }
            Department department = departmentFacade.find(departmentId);
            if (department == null || department.isRetired()) {
                return errorResponse("Department not found: " + departmentId, 400);
            }

            // Parse all charge fields before any DB writes to avoid orphaned TimedItemFee rows
            RoomFacilityCharge charge = new RoomFacilityCharge();
            charge.setName(name);
            charge.setRoom(room);
            charge.setRoomCategory(roomCategory);
            charge.setDepartment(department);
            applyChargeFields(body, charge);
            charge.setCreatedAt(new Date());
            charge.setCreater(user);

            // Build TimedItemFee (all parsing done above; now safe to persist).
            // Start at the same explicit default RoomFacilityChargeController writes
            // for a UI-created charge, so an omitted unit stores HOUR rather than
            // leaving the column null and relying on the read-time default.
            TimedItemFee timedItemFee = new TimedItemFee();
            timedItemFee.setDurationUnit(TimedItemDurationUnit.HOUR);
            Double durationHours = asDouble(body.get("timedItemFeeDurationHours"));
            if (durationHours != null) timedItemFee.setDurationHours(durationHours);
            Double overShootHours = asDouble(body.get("timedItemFeeOverShootHours"));
            if (overShootHours != null) timedItemFee.setOverShootHours(overShootHours);
            Long durationDaysForMoCharge = asLong(body.get("timedItemFeeDurationDaysForMoCharge"));
            if (durationDaysForMoCharge != null) timedItemFee.setDurationDaysForMoCharge(durationDaysForMoCharge);
            TimedItemDurationUnit durationUnit = asDurationUnit(body.get("timedItemFeeDurationUnit"));
            if (durationUnit != null) timedItemFee.setDurationUnit(durationUnit);
            String durationError = validateBlockDuration(timedItemFee);
            if (durationError != null) {
                return errorResponse(durationError, 400);
            }
            timedItemFeeFacade.create(timedItemFee);

            charge.setTimedItemFee(timedItemFee);
            roomFacilityChargeFacade.create(charge);

            return Response.status(201).entity(gson.toJson(successData(toDto(charge)))).build();

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Update a room facility charge by ID.
     * PUT /api/inward/room-facility-charges/{id}
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

            RoomFacilityCharge charge = roomFacilityChargeFacade.find(id);
            if (charge == null || charge.isRetired()) {
                return errorResponse("Room facility charge not found: " + id, 404);
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
                charge.setName(name.trim());
            }
            if (body.containsKey("roomId")) {
                Long roomId = asLong(body.get("roomId"));
                if (roomId == null) {
                    charge.setRoom(null);
                } else {
                    Room room = roomFacade.find(roomId);
                    if (room == null || room.isRetired()) {
                        return errorResponse("Room not found: " + roomId, 400);
                    }
                    charge.setRoom(room);
                }
            }
            if (body.containsKey("roomCategoryId")) {
                Long roomCategoryId = asLong(body.get("roomCategoryId"));
                if (roomCategoryId == null) {
                    charge.setRoomCategory(null);
                } else {
                    RoomCategory roomCategory = roomCategoryFacade.find(roomCategoryId);
                    if (roomCategory == null || roomCategory.isRetired()) {
                        return errorResponse("RoomCategory not found: " + roomCategoryId, 400);
                    }
                    charge.setRoomCategory(roomCategory);
                }
            }
            if (body.containsKey("departmentId")) {
                Long departmentId = asLong(body.get("departmentId"));
                if (departmentId == null) {
                    return errorResponse("departmentId cannot be null", 400);
                }
                Department department = departmentFacade.find(departmentId);
                if (department == null || department.isRetired()) {
                    return errorResponse("Department not found: " + departmentId, 400);
                }
                charge.setDepartment(department);
            }

            applyChargeFields(body, charge);

            // Update TimedItemFee if any timed fields are present.
            // Parse all timed values before any DB writes to avoid orphaned rows.
            if (body.containsKey("timedItemFeeDurationHours")
                    || body.containsKey("timedItemFeeOverShootHours")
                    || body.containsKey("timedItemFeeDurationDaysForMoCharge")
                    || body.containsKey("timedItemFeeDurationUnit")) {
                Double newDurationHours = body.containsKey("timedItemFeeDurationHours") ? asDouble(body.get("timedItemFeeDurationHours")) : null;
                Double newOverShootHours = body.containsKey("timedItemFeeOverShootHours") ? asDouble(body.get("timedItemFeeOverShootHours")) : null;
                Long newDurationDays = body.containsKey("timedItemFeeDurationDaysForMoCharge") ? asLong(body.get("timedItemFeeDurationDaysForMoCharge")) : null;
                TimedItemDurationUnit newDurationUnit = body.containsKey("timedItemFeeDurationUnit") ? asDurationUnit(body.get("timedItemFeeDurationUnit")) : null;
                // All parsing done; now safe to write
                TimedItemFee timedItemFee = charge.getTimedItemFee();
                boolean needCreate = timedItemFee == null;
                if (needCreate) {
                    timedItemFee = new TimedItemFee();
                    timedItemFee.setDurationUnit(TimedItemDurationUnit.HOUR);
                }
                if (newDurationHours != null) timedItemFee.setDurationHours(newDurationHours);
                if (newOverShootHours != null) timedItemFee.setOverShootHours(newOverShootHours);
                if (newDurationDays != null) timedItemFee.setDurationDaysForMoCharge(newDurationDays);
                if (newDurationUnit != null) timedItemFee.setDurationUnit(newDurationUnit);
                String durationError = validateBlockDuration(timedItemFee);
                if (durationError != null) {
                    return errorResponse(durationError, 400);
                }
                if (needCreate) {
                    timedItemFeeFacade.create(timedItemFee);
                    charge.setTimedItemFee(timedItemFee);
                } else {
                    timedItemFeeFacade.edit(timedItemFee);
                }
            }

            roomFacilityChargeFacade.edit(charge);
            return successResponse(toDto(charge));

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Soft-retire a room facility charge by ID.
     * DELETE /api/inward/room-facility-charges/{id}?retireComments=reason
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

            RoomFacilityCharge charge = roomFacilityChargeFacade.find(id);
            if (charge == null) {
                return errorResponse("Room facility charge not found: " + id, 404);
            }
            if (charge.isRetired()) {
                return errorResponse("Room facility charge is already retired: " + id, 400);
            }

            charge.setRetired(true);
            charge.setRetiredAt(new Date());
            charge.setRetirer(user);
            String retireComments = param("retireComments");
            if (retireComments != null && !retireComments.trim().isEmpty()) {
                charge.setRetireComments(retireComments.trim());
            }
            roomFacilityChargeFacade.edit(charge);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", charge.getId());
            resp.put("retired", true);
            return successResponse(resp);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Timed Items (RoomFacilityTimedItem) — issue #23147
    // =========================================================================

    /**
     * List active Timed Items attached to a room facility charge.
     * GET /api/inward/room-facility-charges/{id}/timed-items
     */
    @GET
    @Path("/{id}/timed-items")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listTimedItems(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            RoomFacilityCharge charge = roomFacilityChargeFacade.find(id);
            if (charge == null || charge.isRetired()) {
                return errorResponse("Room facility charge not found: " + id, 404);
            }

            Map<String, Object> params = new HashMap<>();
            params.put("rfc", charge);
            List<RoomFacilityTimedItem> links = roomFacilityTimedItemFacade.findByJpql(
                    "select r from RoomFacilityTimedItem r where r.roomFacilityCharge = :rfc and r.retired = false order by r.id",
                    params);

            List<Map<String, Object>> payload = new ArrayList<>();
            if (links != null) {
                for (RoomFacilityTimedItem link : links) {
                    payload.add(toTimedItemDto(link));
                }
            }
            return successResponse(payload);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Attach a Timed Item to a room facility charge.
     * POST /api/inward/room-facility-charges/{id}/timed-items
     * Body: { "timedItemId" (required) }
     */
    @POST
    @Path("/{id}/timed-items")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addTimedItem(@PathParam("id") Long id, String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            RoomFacilityCharge charge = roomFacilityChargeFacade.find(id);
            if (charge == null || charge.isRetired()) {
                return errorResponse("Room facility charge not found: " + id, 404);
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

            Long timedItemId = asLong(body.get("timedItemId"));
            if (timedItemId == null) {
                return errorResponse("timedItemId is required", 400);
            }
            TimedItem timedItem = timedItemFacade.find(timedItemId);
            if (timedItem == null || timedItem.isRetired()) {
                return errorResponse("Timed Item not found: " + timedItemId, 400);
            }

            Map<String, Object> params = new HashMap<>();
            params.put("rfc", charge);
            params.put("ti", timedItem);
            List<RoomFacilityTimedItem> existing = roomFacilityTimedItemFacade.findByJpql(
                    "select r from RoomFacilityTimedItem r where r.roomFacilityCharge = :rfc and r.timedItem = :ti and r.retired = false",
                    params);
            if (existing != null && !existing.isEmpty()) {
                return errorResponse("This Timed Item is already attached to this room facility charge", 409);
            }

            RoomFacilityTimedItem link = new RoomFacilityTimedItem();
            link.setRoomFacilityCharge(charge);
            link.setTimedItem(timedItem);
            link.setCreater(user);
            link.setCreatedAt(new Date());
            roomFacilityTimedItemFacade.create(link);

            return Response.status(201).entity(gson.toJson(successData(toTimedItemDto(link)))).build();

        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Soft-retire a Timed Item attachment.
     * DELETE /api/inward/room-facility-charges/{id}/timed-items/{linkId}?retireComments=reason
     */
    @DELETE
    @Path("/{id}/timed-items/{linkId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeTimedItem(@PathParam("id") Long id, @PathParam("linkId") Long linkId) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            RoomFacilityCharge charge = roomFacilityChargeFacade.find(id);
            if (charge == null || charge.isRetired()) {
                return errorResponse("Room facility charge not found: " + id, 404);
            }

            RoomFacilityTimedItem link = roomFacilityTimedItemFacade.find(linkId);
            if (link == null || link.getRoomFacilityCharge() == null
                    || !link.getRoomFacilityCharge().getId().equals(charge.getId())) {
                return errorResponse("Timed Item attachment not found: " + linkId, 404);
            }
            if (link.isRetired()) {
                return errorResponse("Timed Item attachment is already retired: " + linkId, 400);
            }

            link.setRetired(true);
            link.setRetiredAt(new Date());
            link.setRetirer(user);
            String retireComments = param("retireComments");
            if (retireComments != null && !retireComments.trim().isEmpty()) {
                link.setRetireComments(retireComments.trim());
            }
            roomFacilityTimedItemFacade.edit(link);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("id", link.getId());
            resp.put("retired", true);
            return successResponse(resp);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void applyChargeFields(Map<?, ?> body, RoomFacilityCharge charge) {
        if (body.containsKey("roomCharge")) {
            Double v = asDouble(body.get("roomCharge"));
            if (v != null) charge.setRoomCharge(v);
        }
        if (body.containsKey("maintananceCharge")) {
            Double v = asDouble(body.get("maintananceCharge"));
            if (v != null) charge.setMaintananceCharge(v);
        }
        if (body.containsKey("linenCharge")) {
            Double v = asDouble(body.get("linenCharge"));
            if (v != null) charge.setLinenCharge(v);
        }
        if (body.containsKey("nursingCharge")) {
            Double v = asDouble(body.get("nursingCharge"));
            if (v != null) charge.setNursingCharge(v);
        }
        if (body.containsKey("moCharge")) {
            Double v = asDouble(body.get("moCharge"));
            if (v != null) charge.setMoCharge(v);
        }
        if (body.containsKey("moChargeForAfterDuration")) {
            Double v = asDouble(body.get("moChargeForAfterDuration"));
            if (v != null) charge.setMoChargeForAfterDuration(v);
        }
        if (body.containsKey("adminstrationCharge")) {
            Double v = asDouble(body.get("adminstrationCharge"));
            if (v != null) charge.setAdminstrationCharge(v);
        }
        if (body.containsKey("medicalCareCharge")) {
            Double v = asDouble(body.get("medicalCareCharge"));
            if (v != null) charge.setMedicalCareCharge(v);
        }
    }

    private Map<String, Object> toDto(RoomFacilityCharge r) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", r.getId());
        row.put("name", r.getName());
        row.put("roomCharge", r.getRoomCharge());
        row.put("maintananceCharge", r.getMaintananceCharge());
        row.put("linenCharge", r.getLinenCharge());
        row.put("nursingCharge", r.getNursingCharge());
        row.put("moCharge", r.getMoCharge());
        row.put("moChargeForAfterDuration", r.getMoChargeForAfterDuration());
        row.put("adminstrationCharge", r.getAdminstrationCharge());
        row.put("medicalCareCharge", r.getMedicalCareCharge());
        row.put("retired", r.isRetired());
        if (r.getRoom() != null) {
            Map<String, Object> room = new LinkedHashMap<>();
            room.put("id", r.getRoom().getId());
            room.put("name", r.getRoom().getName());
            row.put("room", room);
        } else {
            row.put("room", null);
        }
        if (r.getRoomCategory() != null) {
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("id", r.getRoomCategory().getId());
            cat.put("name", r.getRoomCategory().getName());
            row.put("roomCategory", cat);
        } else {
            row.put("roomCategory", null);
        }
        if (r.getDepartment() != null) {
            Map<String, Object> dep = new LinkedHashMap<>();
            dep.put("id", r.getDepartment().getId());
            dep.put("name", r.getDepartment().getName());
            row.put("department", dep);
        } else {
            row.put("department", null);
        }
        if (r.getTimedItemFee() != null) {
            Map<String, Object> tif = new LinkedHashMap<>();
            tif.put("id", r.getTimedItemFee().getId());
            tif.put("durationHours", r.getTimedItemFee().getDurationHours());
            tif.put("overShootHours", r.getTimedItemFee().getOverShootHours());
            tif.put("durationDaysForMoCharge", r.getTimedItemFee().getDurationDaysForMoCharge());
            tif.put("durationUnit", r.getTimedItemFee().getDurationUnit().name());
            row.put("timedItemFee", tif);
        } else {
            row.put("timedItemFee", null);
        }
        return row;
    }

    private Map<String, Object> toTimedItemDto(RoomFacilityTimedItem link) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", link.getId());
        if (link.getTimedItem() != null) {
            Map<String, Object> ti = new LinkedHashMap<>();
            ti.put("id", link.getTimedItem().getId());
            ti.put("name", link.getTimedItem().getName());
            row.put("timedItem", ti);
        } else {
            row.put("timedItem", null);
        }
        row.put("createdAt", link.getCreatedAt());
        row.put("retired", link.isRetired());
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

    /**
     * A time-based block with no duration bills nothing, for every stay, forever —
     * {@code InwardBeanController.calCount()} returns 0 when the block length is 0.
     * The sibling timed-item fee endpoint already rejects that combination
     * (TimedItemFeeCreateRequestDTO.isValid); this keeps the two consistent rather
     * than letting a room be created that silently never charges.
     *
     * @return an error message, or null when the fee is billable
     */
    private String validateBlockDuration(TimedItemFee fee) {
        if (fee.isOneTime()) {
            return null;
        }
        if (fee.getDurationHours() <= 0) {
            return "timedItemFeeDurationHours must be greater than 0 for a "
                    + fee.getDurationUnit().name() + " block"
                    + " (use timedItemFeeDurationUnit=ONE_TIME for a flat charge).";
        }
        return null;
    }

    private TimedItemDurationUnit asDurationUnit(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return TimedItemDurationUnit.valueOf(s.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid timedItemFeeDurationUnit: '" + o
                    + "'. Expected one of ONE_TIME, MINUTE, HOUR, DAY.");
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
