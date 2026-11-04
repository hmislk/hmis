package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.StockFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API to warm the EclipseLink shared (L2) entity cache after a deployment or restart.
 * GET /api/warmup/stocks loads every Stock with stock on hand, together with its ItemBatch and Item,
 * so the first pharmacy bills opened after a restart do not load each entity one by one.
 */
@Path("warmup")
@RequestScoped
public class WarmupApi {

    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @Inject
    private StockFacade stockFacade;

    /**
     * Load all Stock rows with a positive stock together with their ItemBatch and Item into the L2 cache.
     * GET /api/warmup/stocks
     */
    @GET
    @Path("/stocks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response warmUpStocks() {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            long start = System.currentTimeMillis();
            String jpql = "select s from Stock s"
                    + " join fetch s.itemBatch ib"
                    + " join fetch ib.item"
                    + " where s.retired=false and s.stock>0";
            List<Stock> stocks = stockFacade.findByJpql(jpql);
            int loaded = stocks == null ? 0 : stocks.size();

            Map<String, Object> data = new HashMap<>();
            data.put("stocksLoaded", loaded);
            data.put("timeTakenMs", System.currentTimeMillis() - start);
            return successResponse(data);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
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
        // Treat null expiry date as expired
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
