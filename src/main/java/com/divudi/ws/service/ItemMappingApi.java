/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.service;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.ItemMapping;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.ItemMappingFacade;
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
import java.util.List;
import java.util.Map;

/**
 * REST API for ItemMapping — which items a department, an institution, or an
 * outside-charge site may bill.
 *
 * Mirrors the semantics of {@code ItemMappingController} (department/institution
 * mapping pages plus the outside-charge "Mode B" mapping added for #23250):
 * duplicate-detection before creating a mapping, and removal as a soft retire
 * rather than a hard delete. An outside-charge mapping is stored as an
 * institution mapping with {@code outsideChargeMapping=true} on the same
 * ItemMapping row — there is no separate site table.
 *
 * @author Buddhika
 */
@Path("item-mappings")
@RequestScoped
public class ItemMappingApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private ItemMappingFacade itemMappingFacade;

    @EJB
    private ItemFacade itemFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private InstitutionFacade institutionFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    // =========================================================================
    // GET /api/item-mappings/search
    // =========================================================================

    /**
     * List current mappings, filtered by target and/or item.
     * GET /api/item-mappings/search?departmentId=&institutionId=&outsideChargeSiteId=&itemId=&query=&limit=
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Long departmentId;
            Long institutionId;
            Long outsideChargeSiteId;
            Long itemId;
            try {
                departmentId = requireValidLongParam("departmentId");
                institutionId = requireValidLongParam("institutionId");
                outsideChargeSiteId = requireValidLongParam("outsideChargeSiteId");
                itemId = requireValidLongParam("itemId");
            } catch (NumberFormatException e) {
                return errorResponse(e.getMessage(), 400);
            }
            String query = uriInfo.getQueryParameters().getFirst("query");

            int given = 0;
            if (departmentId != null) {
                given++;
            }
            if (institutionId != null) {
                given++;
            }
            if (outsideChargeSiteId != null) {
                given++;
            }
            if (given > 1) {
                return errorResponse("Provide at most one of departmentId, institutionId, outsideChargeSiteId", 400);
            }

            int limit = 30;
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    limit = Math.min(Math.max(Integer.parseInt(limitStr.trim()), 1), 200);
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            Map<String, Object> params = new HashMap<>();
            StringBuilder jpql = new StringBuilder("select im from ItemMapping im where im.retired=false");

            if (departmentId != null) {
                jpql.append(" and im.department.id=:departmentId");
                params.put("departmentId", departmentId);
            } else if (institutionId != null) {
                jpql.append(" and im.institution.id=:institutionId and im.outsideChargeMapping=false");
                params.put("institutionId", institutionId);
            } else if (outsideChargeSiteId != null) {
                jpql.append(" and im.institution.id=:outsideChargeSiteId and im.outsideChargeMapping=true");
                params.put("outsideChargeSiteId", outsideChargeSiteId);
            }

            if (itemId != null) {
                jpql.append(" and im.item.id=:itemId");
                params.put("itemId", itemId);
            }

            if (query != null && !query.trim().isEmpty()) {
                jpql.append(" and upper(im.item.name) like :q");
                params.put("q", "%" + query.trim().toUpperCase() + "%");
            }

            jpql.append(" order by im.id desc");

            List<ItemMapping> mappings = itemMappingFacade.findByJpql(jpql.toString(), params, limit);
            List<Map<String, Object>> result = new ArrayList<>();
            for (ItemMapping im : mappings) {
                result.add(toMap(im));
            }
            return successResponse(result);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // POST /api/item-mappings
    // =========================================================================

    /**
     * Map one item to exactly one target (department, institution, or
     * outside-charge site). Body: {"itemId": 1, "departmentId": 2}
     * (or "institutionId", or "outsideChargeSiteId" instead of "departmentId").
     * Idempotent — mapping an already-mapped active pair returns the existing
     * mapping with status "already_exists"; mapping a previously soft-retired
     * pair reactivates that same row in place (status "reactivated") rather
     * than creating a duplicate — matching ItemMappingController's
     * addAllSelectedItemsTo...() reactivation behavior.
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

            Map<String, Object> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (body == null) {
                return errorResponse("Request body is required", 400);
            }

            MappingOutcome outcome = mapOneItem(body, user);
            if (outcome.errorMessage != null) {
                return errorResponse(outcome.errorMessage, 400);
            }

            if ("already_mapped".equals(outcome.result)) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "already_exists");
                response.put("code", 200);
                response.put("id", outcome.mapping.getId());
                response.put("data", toMap(outcome.mapping));
                return Response.status(200).entity(gson.toJson(response)).build();
            }

            if ("reactivated".equals(outcome.result)) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "reactivated");
                response.put("code", 200);
                response.put("id", outcome.mapping.getId());
                response.put("data", toMap(outcome.mapping));
                return Response.status(200).entity(gson.toJson(response)).build();
            }

            return Response.status(201).entity(gson.toJson(successData(toMap(outcome.mapping)))).build();

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // POST /api/item-mappings/bulk
    // =========================================================================

    /**
     * Map many items to one target in a single call.
     * Body: {"itemIds": [1,2,3], "departmentId": 2}
     * (or "institutionId", or "outsideChargeSiteId" instead of "departmentId").
     * Never fails the whole batch on one bad id — each itemId gets its own
     * outcome: created / reactivated / already_mapped / item_not_found / invalid_item_id.
     * A previously soft-retired mapping for the same item+target is reactivated
     * in place rather than duplicated, matching the single-item POST endpoint.
     */
    @POST
    @Path("/bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createBulk(String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Map<String, Object> body;
            try {
                body = gson.fromJson(requestBody, Map.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (body == null) {
                return errorResponse("Request body is required", 400);
            }

            Object itemIdsRaw = body.get("itemIds");
            if (!(itemIdsRaw instanceof List) || ((List<?>) itemIdsRaw).isEmpty()) {
                return errorResponse("itemIds must be a non-empty array", 400);
            }

            TargetResolution target = resolveTarget(body);
            if (target.errorMessage != null) {
                return errorResponse(target.errorMessage, 400);
            }

            List<Map<String, Object>> outcomes = new ArrayList<>();
            for (Object idObj : (List<?>) itemIdsRaw) {
                Long itemId = toLong(idObj);
                Map<String, Object> outcomeMap = new HashMap<>();
                if (itemId == null) {
                    outcomeMap.put("itemId", idObj);
                    outcomeMap.put("outcome", "invalid_item_id");
                    outcomes.add(outcomeMap);
                    continue;
                }

                Item item = itemFacade.find(itemId);
                if (item == null) {
                    outcomeMap.put("itemId", itemId);
                    outcomeMap.put("outcome", "item_not_found");
                    outcomes.add(outcomeMap);
                    continue;
                }

                ItemMapping existing = findExistingMapping(item, target);
                if (existing != null) {
                    if (existing.isRetired()) {
                        existing.setRetired(false);
                        itemMappingFacade.edit(existing);
                        outcomeMap.put("itemId", itemId);
                        outcomeMap.put("outcome", "reactivated");
                        outcomeMap.put("id", existing.getId());
                        outcomes.add(outcomeMap);
                        continue;
                    }
                    outcomeMap.put("itemId", itemId);
                    outcomeMap.put("outcome", "already_mapped");
                    outcomeMap.put("id", existing.getId());
                    outcomes.add(outcomeMap);
                    continue;
                }

                ItemMapping mapping = new ItemMapping();
                mapping.setItem(item);
                applyTarget(mapping, target);
                mapping.setCreater(user);
                mapping.setCreatedAt(new Date());
                itemMappingFacade.create(mapping);

                outcomeMap.put("itemId", itemId);
                outcomeMap.put("outcome", "created");
                outcomeMap.put("id", mapping.getId());
                outcomes.add(outcomeMap);
            }

            return successResponse(outcomes);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // DELETE /api/item-mappings/{id}
    // =========================================================================

    /**
     * Soft-retire one mapping (retired=true) — matches
     * removeSelectedItemMappingForDepartment()/...ForInstitution()/...ForOutsideChargeSite().
     * Never a hard delete.
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

            ItemMapping mapping = itemMappingFacade.find(id);
            if (mapping == null) {
                return errorResponse("Item mapping not found: " + id, 404);
            }
            if (mapping.isRetired()) {
                return successResponse(toMap(mapping));
            }

            mapping.setRetired(true);
            mapping.setRetirer(user);
            mapping.setRetiredAt(new Date());
            itemMappingFacade.edit(mapping);

            return successResponse(toMap(mapping));

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // =========================================================================
    // Shared helpers
    // =========================================================================

    private MappingOutcome mapOneItem(Map<String, Object> body, WebUser user) {
        MappingOutcome outcome = new MappingOutcome();

        Long itemId = toLong(body.get("itemId"));
        if (itemId == null) {
            outcome.errorMessage = "itemId is required";
            return outcome;
        }
        Item item = itemFacade.find(itemId);
        if (item == null) {
            outcome.errorMessage = "Item not found: " + itemId;
            return outcome;
        }

        TargetResolution target = resolveTarget(body);
        if (target.errorMessage != null) {
            outcome.errorMessage = target.errorMessage;
            return outcome;
        }

        ItemMapping existing = findExistingMapping(item, target);
        if (existing != null) {
            if (existing.isRetired()) {
                existing.setRetired(false);
                itemMappingFacade.edit(existing);
                outcome.result = "reactivated";
                outcome.mapping = existing;
                return outcome;
            }
            outcome.result = "already_mapped";
            outcome.mapping = existing;
            return outcome;
        }

        ItemMapping mapping = new ItemMapping();
        mapping.setItem(item);
        applyTarget(mapping, target);
        mapping.setCreater(user);
        mapping.setCreatedAt(new Date());
        itemMappingFacade.create(mapping);

        outcome.result = "created";
        outcome.mapping = mapping;
        return outcome;
    }

    /**
     * Resolves exactly one of departmentId/institutionId/outsideChargeSiteId
     * from the request body into the corresponding entity, validating that
     * only one target kind was given and that it exists.
     */
    private TargetResolution resolveTarget(Map<String, Object> body) {
        TargetResolution target = new TargetResolution();

        Long departmentId = toLong(body.get("departmentId"));
        Long institutionId = toLong(body.get("institutionId"));
        Long outsideChargeSiteId = toLong(body.get("outsideChargeSiteId"));

        int given = 0;
        if (departmentId != null) {
            given++;
        }
        if (institutionId != null) {
            given++;
        }
        if (outsideChargeSiteId != null) {
            given++;
        }
        if (given != 1) {
            target.errorMessage = "Provide exactly one of departmentId, institutionId, outsideChargeSiteId";
            return target;
        }

        if (departmentId != null) {
            Department department = departmentFacade.find(departmentId);
            if (department == null) {
                target.errorMessage = "Department not found: " + departmentId;
                return target;
            }
            target.department = department;
        } else if (institutionId != null) {
            Institution institution = institutionFacade.find(institutionId);
            if (institution == null) {
                target.errorMessage = "Institution not found: " + institutionId;
                return target;
            }
            target.institution = institution;
            target.outsideCharge = false;
        } else {
            Institution site = institutionFacade.find(outsideChargeSiteId);
            if (site == null) {
                target.errorMessage = "Outside-charge site (institution) not found: " + outsideChargeSiteId;
                return target;
            }
            target.institution = site;
            target.outsideCharge = true;
        }

        return target;
    }

    private void applyTarget(ItemMapping mapping, TargetResolution target) {
        if (target.department != null) {
            mapping.setDepartment(target.department);
        } else {
            mapping.setInstitution(target.institution);
            mapping.setOutsideChargeMapping(target.outsideCharge);
        }
    }

    /**
     * Duplicate-detection mirroring ItemMappingController's per-target JPQL:
     * department mappings match on department+item only; institution mappings
     * additionally require outsideChargeMapping=false; outside-charge mappings
     * additionally require outsideChargeMapping=true. Deliberately does NOT
     * filter on retired — the controller's findItemMapping()/addAllSelectedItemsTo...()
     * methods look up the row regardless of retired status so a previously
     * soft-retired mapping gets reactivated in place instead of duplicated;
     * see mapOneItem()/createBulk()'s isRetired() check on the result.
     */
    private ItemMapping findExistingMapping(Item item, TargetResolution target) {
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        String jpql;
        if (target.department != null) {
            jpql = "select im from ItemMapping im where im.department=:target and im.item=:item";
            params.put("target", target.department);
        } else if (!target.outsideCharge) {
            jpql = "select im from ItemMapping im where im.institution=:target and im.item=:item"
                    + " and im.outsideChargeMapping=false";
            params.put("target", target.institution);
        } else {
            jpql = "select im from ItemMapping im where im.institution=:target and im.item=:item"
                    + " and im.outsideChargeMapping=true";
            params.put("target", target.institution);
        }
        return itemMappingFacade.findFirstByJpql(jpql, params);
    }

    private Map<String, Object> toMap(ItemMapping im) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", im.getId());
        map.put("retired", im.isRetired());
        map.put("outsideChargeMapping", im.isOutsideChargeMapping());
        map.put("createdAt", im.getCreatedAt());

        if (im.getItem() != null) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", im.getItem().getId());
            item.put("name", im.getItem().getName());
            map.put("item", item);
        }
        if (im.getDepartment() != null) {
            Map<String, Object> department = new HashMap<>();
            department.put("id", im.getDepartment().getId());
            department.put("name", im.getDepartment().getName());
            map.put("department", department);
        }
        if (im.getInstitution() != null) {
            Map<String, Object> institution = new HashMap<>();
            institution.put("id", im.getInstitution().getId());
            institution.put("name", im.getInstitution().getName());
            map.put(im.isOutsideChargeMapping() ? "outsideChargeSite" : "institution", institution);
        }
        return map;
    }

    /**
     * Reads a query param as a Long, returning null when omitted/empty but
     * throwing NumberFormatException when present and unparseable — so a
     * typo like departmentId=bad is rejected with a 400 instead of silently
     * falling through as "no filter", which could also bypass the
     * mutually-exclusive-target check in search().
     */
    private Long requireValidLongParam(String name) {
        String raw = uriInfo.getQueryParameters().getFirst(name);
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid " + name + " format");
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            Number n = (Number) value;
            if (n.doubleValue() != Math.floor(n.doubleValue()) || Double.isInfinite(n.doubleValue())) {
                return null;
            }
            return n.longValue();
        }
        String s = value.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) {
            return null;
        }
        WebUser user = apiKey.getWebUser();
        if (user == null) {
            return null;
        }
        if (user.isRetired()) {
            return null;
        }
        if (!user.isActivated()) {
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

    private static class TargetResolution {
        Department department;
        Institution institution;
        boolean outsideCharge;
        String errorMessage;
    }

    private static class MappingOutcome {
        String result;
        ItemMapping mapping;
        String errorMessage;
    }
}
