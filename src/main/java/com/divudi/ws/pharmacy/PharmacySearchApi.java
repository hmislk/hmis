/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.data.dto.search.DepartmentDTO;
import com.divudi.core.data.dto.search.ItemDTO;
import com.divudi.core.data.dto.search.StockSearchRequestDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.pharmacy.PharmacySearchApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for Pharmacy Search Operations
 * Provides search/lookup endpoints for stocks, departments, and items
 * Designed to work seamlessly with existing PharmacyAdjustmentApi
 *
 * @author Buddhika
 */
@Path("pharmacy_adjustments/search")
@RequestScoped
public class PharmacySearchApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private PharmacySearchApiService searchService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public PharmacySearchApi() {
    }

    /**
     * Search stocks with comprehensive filtering
     * GET /api/pharmacy_adjustments/search/stocks?query=Paracetamol&department=Main Pharmacy&minQuantity=10
     */
    @GET
    @Path("/stocks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchStocks() {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Parse query parameters into request DTO
            StockSearchRequestDTO request = parseStockSearchParams();

            // Perform search
            List<StockDTO> results = searchService.searchStocks(request);

            return successResponse(results);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Search departments by name
     * GET /api/pharmacy_adjustments/search/departments?query=Pharmacy&limit=20
     */
    @GET
    @Path("/departments")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchDepartments() {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Get query parameters
            String query = uriInfo.getQueryParameters().getFirst("query");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            if (query == null || query.trim().isEmpty()) {
                return errorResponse("Query parameter is required", 400);
            }

            Integer limit = null;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitStr.trim());
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            // Perform search
            List<DepartmentDTO> results = searchService.searchDepartments(query.trim(), limit);

            return successResponse(results);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Search items by name, code, or barcode
     * GET /api/pharmacy_adjustments/search/items?query=Para&limit=30
     */
    @GET
    @Path("/items")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchItems() {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Get query parameters
            String query = uriInfo.getQueryParameters().getFirst("query");
            String limitStr = uriInfo.getQueryParameters().getFirst("limit");

            if (query == null || query.trim().isEmpty()) {
                return errorResponse("Query parameter is required", 400);
            }

            Integer limit = null;
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                try {
                    limit = Integer.parseInt(limitStr.trim());
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid limit format", 400);
                }
            }

            // Perform search
            List<ItemDTO> results = searchService.searchItems(query.trim(), limit);

            return successResponse(results);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // Private helper methods

    /**
     * Parse stock search query parameters into request DTO
     */
    private StockSearchRequestDTO parseStockSearchParams() throws Exception {
        StockSearchRequestDTO request = new StockSearchRequestDTO();

        // Required parameters
        String query = uriInfo.getQueryParameters().getFirst("query");
        String department = uriInfo.getQueryParameters().getFirst("department");

        if (query == null || query.trim().isEmpty()) {
            throw new Exception("Query parameter is required");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new Exception("Department parameter is required");
        }

        request.setQuery(query.trim());
        request.setDepartment(department.trim());

        // Optional quantity filters
        parseDoubleParam("minQuantity", request::setMinQuantity);
        parseDoubleParam("maxQuantity", request::setMaxQuantity);

        // Optional rate filters
        parseDoubleParam("minRetailRate", request::setMinRetailRate);
        parseDoubleParam("maxRetailRate", request::setMaxRetailRate);
        parseDoubleParam("minCostRate", request::setMinCostRate);
        parseDoubleParam("maxCostRate", request::setMaxCostRate);
        parseDoubleParam("minPurchaseRate", request::setMinPurchaseRate);
        parseDoubleParam("maxPurchaseRate", request::setMaxPurchaseRate);

        // Optional date filters
        parseDateParam("expiryAfter", request::setExpiryAfter);
        parseDateParam("expiryBefore", request::setExpiryBefore);

        // Optional batch filter
        String batchNo = uriInfo.getQueryParameters().getFirst("batchNo");
        if (batchNo != null && !batchNo.trim().isEmpty()) {
            request.setBatchNo(batchNo.trim());
        }

        // Optional flags
        String includeZeroStockStr = uriInfo.getQueryParameters().getFirst("includeZeroStock");
        if (includeZeroStockStr != null && !includeZeroStockStr.trim().isEmpty()) {
            request.setIncludeZeroStock(Boolean.valueOf(includeZeroStockStr.trim()));
        }

        // Optional limit
        String limitStr = uriInfo.getQueryParameters().getFirst("limit");
        if (limitStr != null && !limitStr.trim().isEmpty()) {
            try {
                request.setLimit(Integer.parseInt(limitStr.trim()));
            } catch (NumberFormatException e) {
                throw new Exception("Invalid limit format");
            }
        }

        return request;
    }

    /**
     * Helper to parse double parameters
     */
    private void parseDoubleParam(String paramName, java.util.function.Consumer<Double> setter) throws Exception {
        String value = uriInfo.getQueryParameters().getFirst(paramName);
        if (value != null && !value.trim().isEmpty()) {
            try {
                setter.accept(Double.parseDouble(value.trim()));
            } catch (NumberFormatException e) {
                throw new Exception("Invalid " + paramName + " format");
            }
        }
    }

    /**
     * Helper to parse date parameters
     */
    private void parseDateParam(String paramName, java.util.function.Consumer<Date> setter) throws Exception {
        String value = uriInfo.getQueryParameters().getFirst(paramName);
        if (value != null && !value.trim().isEmpty()) {
            Date date = searchService.parseDate(value.trim());
            if (date != null) {
                setter.accept(date);
            }
        }
    }

    /**
     * Validate API key and return associated user
     * Follows same pattern as PharmacyAdjustmentApi
     */
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

        // Treat null expiry date as expired
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) {
            return null;
        }

        return user;
    }

    /**
     * Create error response following PharmacyAdjustmentApi pattern
     */
    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(gson.toJson(response)).build();
    }

    /**
     * Create success response following PharmacyAdjustmentApi pattern
     */
    private Response successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.status(200).entity(gson.toJson(response)).build();
    }
}