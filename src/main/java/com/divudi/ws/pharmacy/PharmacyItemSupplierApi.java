package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.pharmacy.BulkItemSupplierResultDto;
import com.divudi.core.data.dto.pharmacy.ItemSupplierDto;
import com.divudi.core.data.dto.pharmacy.ItemSupplierRequestDto;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.pharmacy.PharmacyItemSupplierApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

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
 * REST API for managing Pharmacy Item Supplier links (ItemsDistributors).
 * Allows listing active links, linking items to Dealer suppliers, bulk linking,
 * and retiring links with full audit attribution.
 */
@Path("pharmacy/item_suppliers")
@RequestScoped
public class PharmacyItemSupplierApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private PharmacyItemSupplierApiService service;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            // Gson's own Long reader rounds 200.00000000000000001 to 200 and can truncate fractions.
            .registerTypeAdapter(Long.class, (JsonDeserializer<Long>) (json, type, context) -> {
                try {
                    return parseExactLong(json);
                } catch (NumberFormatException | ArithmeticException e) {
                    throw new JsonSyntaxException("Invalid ID: " + json, e);
                }
            })
            .create();

    private static final int MAX_BULK_SIZE = 500;

    public PharmacyItemSupplierApi() {
    }

    /**
     * List active item-supplier links, optionally filtered by supplierId or itemId.
     * GET /api/pharmacy/item_suppliers?supplierId=&itemId=&limit=&offset=
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchLinks() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            Long supplierId = parseLongParam("supplierId");
            Long itemId = parseLongParam("itemId");
            Integer limit = parseIntParam("limit");
            Integer offset = parseIntParam("offset");

            List<ItemSupplierDto> results = service.searchLinks(supplierId, itemId, limit, offset);
            return successResponse(results);

        } catch (NumberFormatException e) {
            return errorResponse("Invalid numeric query parameter: " + e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), 500);
        }
    }

    /**
     * Get a single item-supplier link by ID.
     * GET /api/pharmacy/item_suppliers/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLinkById(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            ItemSupplierDto link = service.getLinkById(id);
            if (link == null) {
                return errorResponse("Item supplier link not found: " + id, 404);
            }
            return successResponse(link);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), 500);
        }
    }

    /**
     * Link an item to a Dealer supplier.
     * POST /api/pharmacy/item_suppliers
     * Body: { "supplierId": 1, "itemId": 2, "orderNo": 0 }
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLink(String requestBody) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (requestBody == null || requestBody.trim().isEmpty()) {
                return errorResponse("Request body is required", 400);
            }

            ItemSupplierRequestDto requestDto;
            try {
                requestDto = gson.fromJson(requestBody, ItemSupplierRequestDto.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            PharmacyItemSupplierApiService.CreateItemSupplierResult result = service.createLink(requestDto, user);

            if (result.getStatus() == 201) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("code", 201);
                response.put("data", result.getData());
                return Response.status(201).entity(gson.toJson(response)).build();
            } else if (result.getStatus() == 409) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("code", 409);
                response.put("message", result.getMessage());
                response.put("data", result.getData());
                return Response.status(409).entity(gson.toJson(response)).build();
            } else {
                return errorResponse(result.getMessage(), result.getStatus());
            }

        } catch (Exception e) {
            return errorResponse("An error occurred: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), 500);
        }
    }

    /**
     * Create multiple item-supplier links in bulk with per-row outcomes.
     * POST /api/pharmacy/item_suppliers/bulk
     * Body can be:
     *   - JSON array: [ { "supplierId": 1, "itemId": 2 }, ... ]
     *   - JSON object with links/items: { "links": [ { "supplierId": 1, "itemId": 2 }, ... ] }
     *   - JSON object with itemIds: { "supplierId": 1, "itemIds": [ 2, 3, 4 ] }
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

            if (requestBody == null || requestBody.trim().isEmpty()) {
                return errorResponse("Request body is required", 400);
            }

            List<ItemSupplierRequestDto> requests = parseBulkRequests(requestBody);
            if (requests.isEmpty()) {
                return errorResponse("No valid supplier-item entries found in request body", 400);
            }
            if (requests.size() > MAX_BULK_SIZE) {
                return errorResponse("Too many entries; maximum is " + MAX_BULK_SIZE + " per call", 400);
            }

            List<BulkItemSupplierResultDto> results = service.createBulk(requests, user);
            return successResponse(results);

        } catch (JsonSyntaxException | NumberFormatException | ArithmeticException | IllegalStateException
                | UnsupportedOperationException | ClassCastException e) {
            return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), 500);
        }
    }

    /**
     * Soft-retire an item-supplier link.
     * DELETE /api/pharmacy/item_suppliers/{id}?retireComments=xxx
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response retireLink(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String retireComments = uriInfo != null && uriInfo.getQueryParameters() != null
                    ? uriInfo.getQueryParameters().getFirst("retireComments")
                    : null;

            PharmacyItemSupplierApiService.RetireItemSupplierResult result = service.retireLink(id, retireComments, user);

            if (result.getStatus() == 200) {
                return successResponse(result.getData());
            } else {
                return errorResponse(result.getMessage(), result.getStatus());
            }

        } catch (Exception e) {
            return errorResponse("An error occurred: " + (e.getMessage() != null ? e.getMessage() : "Unknown error"), 500);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    List<ItemSupplierRequestDto> parseBulkRequests(String json) {
        List<ItemSupplierRequestDto> list = new ArrayList<>();
        JsonElement element = JsonParser.parseString(json);

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                list.add(parseBulkRow(item));
            }
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("links") && obj.get("links").isJsonArray()) {
                for (JsonElement item : obj.getAsJsonArray("links")) {
                    list.add(parseBulkRow(item));
                }
            } else if (obj.has("items") && obj.get("items").isJsonArray()) {
                for (JsonElement item : obj.getAsJsonArray("items")) {
                    list.add(parseBulkRow(item));
                }
            } else if (obj.has("supplierId") && obj.has("itemIds") && obj.get("itemIds").isJsonArray()) {
                Long supplierId = parseExactLong(obj.get("supplierId"));
                for (JsonElement itemIdElem : obj.getAsJsonArray("itemIds")) {
                    if (itemIdElem.isJsonPrimitive()) {
                        Long itemId = parseExactLong(itemIdElem);
                        list.add(new ItemSupplierRequestDto(supplierId, itemId));
                    } else {
                        list.add(new ItemSupplierRequestDto(supplierId, null));
                    }
                }
            }
        }

        return list;
    }

    private ItemSupplierRequestDto parseBulkRow(JsonElement row) {
        if (!row.isJsonObject()) {
            return new ItemSupplierRequestDto();
        }
        return gson.fromJson(row, ItemSupplierRequestDto.class);
    }

    private static Long parseExactLong(JsonElement elem) {
        if (elem == null || elem.isJsonNull()) {
            return null;
        }
        if (!elem.isJsonPrimitive()) {
            throw new JsonSyntaxException("Expected primitive numeric ID");
        }
        JsonPrimitive prim = elem.getAsJsonPrimitive();
        if (prim.isNumber()) {
            return prim.getAsBigDecimal().longValueExact();
        }
        if (prim.isString()) {
            return new java.math.BigDecimal(prim.getAsString().trim()).longValueExact();
        }
        throw new JsonSyntaxException("Invalid ID type: " + prim);
    }

    private Long parseLongParam(String paramName) {
        if (uriInfo == null || uriInfo.getQueryParameters() == null) {
            return null;
        }
        String val = uriInfo.getQueryParameters().getFirst(paramName);
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        return Long.parseLong(val.trim());
    }

    private Integer parseIntParam(String paramName) {
        if (uriInfo == null || uriInfo.getQueryParameters() == null) {
            return null;
        }
        String val = uriInfo.getQueryParameters().getFirst(paramName);
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        return Integer.parseInt(val.trim());
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
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.status(200).entity(gson.toJson(response)).build();
    }
}
