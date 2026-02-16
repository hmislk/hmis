/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.adjustment.*;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.WebUserService;
import com.divudi.service.pharmacy.PharmacyAdjustmentApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;

/**
 * REST API for Pharmacy Stock Adjustments Provides endpoints for quantity,
 * retail rate, and expiry date adjustments
 *
 * @author Buddhika
 */
@Path("pharmacy_adjustments")
@RequestScoped
public class PharmacyAdjustmentApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private PharmacyAdjustmentApiService adjustmentService;

    @EJB
    WebUserService webUserService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    public PharmacyAdjustmentApi() {
    }

    /**
     * Adjust stock quantity for a single stock item Endpoint: POST
     * /api/pharmacy_adjustments/stock_quantity
     */
    @POST
    @Path("/stock_quantity")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response adjustStockQuantity(String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            

            // Parse request body
            StockQuantityAdjustmentDTO request;
            try {
                request = gson.fromJson(requestBody, StockQuantityAdjustmentDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }
            
            String privilege = "PharmacyAdjustmentDepartmentStockQTY";
            if (!webUserService.hasPrivilege(privilege,user, request.getDepartmentId())) {
                return errorResponse("Not authorized", 403);
            }
            

            // Process adjustment
            AdjustmentResponseDTO response = adjustmentService.adjustStockQuantity(request, user);
            return successResponse(response);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Adjust retail rate for a single stock item Endpoint: POST
     * /api/pharmacy_adjustments/retail_rate
     */
    @POST
    @Path("/retail_rate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response adjustRetailRate(String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Parse request body
            RetailRateAdjustmentDTO request;
            try {
                request = gson.fromJson(requestBody, RetailRateAdjustmentDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            String privilege = "PharmacyAdjustmentSaleRate";
            if (!webUserService.hasPrivilege(privilege, user, request.getDepartmentId())) {
                return errorResponse("Not authorized", 403);
            }

            // Process adjustment
            AdjustmentResponseDTO response = adjustmentService.adjustRetailRate(request, user);
            return successResponse(response);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Adjust expiry date for a single stock item Endpoint: POST
     * /api/pharmacy_adjustments/expiry_date
     */
    @POST
    @Path("/expiry_date")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response adjustExpiryDate(String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Parse request body
            ExpiryDateAdjustmentDTO request;
            try {
                request = gson.fromJson(requestBody, ExpiryDateAdjustmentDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            String privilege = "PharmacyAdjustmentExpiryDate";
            if (!webUserService.hasPrivilege(privilege, user, request.getDepartmentId())) {
                return errorResponse("Not authorized", 403);
            }

            // Process adjustment
            AdjustmentResponseDTO response = adjustmentService.adjustExpiryDate(request, user);
            return successResponse(response);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Adjust purchase rate for a single stock item Endpoint: POST
     * /api/pharmacy_adjustments/purchase_rate
     */
    @POST
    @Path("/purchase_rate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response adjustPurchaseRate(String requestBody) {
        try {
            // Validate API key
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Parse request body
            PurchaseRateAdjustmentDTO request;
            try {
                request = gson.fromJson(requestBody, PurchaseRateAdjustmentDTO.class);
            } catch (JsonSyntaxException e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }

            String privilege = "PharmacyAdjustmentPurchaseRate";
            if (!webUserService.hasPrivilege(privilege, user, request.getDepartmentId())) {
                return errorResponse("Not authorized", 403);
            }

            // Process adjustment
            AdjustmentResponseDTO response = adjustmentService.adjustPurchaseRate(request, user);
            return successResponse(response);

        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Validate API key and return associated user
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
     * Create error response following CostingData pattern
     */
    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(gson.toJson(response)).build();
    }

    /**
     * Create success response following CostingData pattern
     */
    private Response successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.status(200).entity(gson.toJson(response)).build();
    }
}
