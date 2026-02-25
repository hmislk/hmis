package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.HistoryType;
import com.divudi.core.data.dto.StockHistoryDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.facade.StockHistoryFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Stock history API for stock movement verification and audit trail access.
 */
@Path("stock_history")
@RequestScoped
public class StockHistoryApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private StockHistoryFacade stockHistoryFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStockHistory(
            @QueryParam("itemId") Long itemId,
            @QueryParam("departmentId") Long departmentId,
            @QueryParam("billId") Long billId,
            @QueryParam("fromDate") String fromDateStr,
            @QueryParam("toDate") String toDateStr,
            @QueryParam("historyType") String historyTypeStr,
            @QueryParam("limit") Integer limit) {

        try {
            String key = requestContext.getHeader("Finance");
            if (!isValidKey(key)) {
                return errorResponse("Not a valid key", 401);
            }

            Date fromDate = null;
            if (fromDateStr != null && !fromDateStr.trim().isEmpty()) {
                try {
                    fromDate = parseDate(fromDateStr.trim());
                } catch (DateTimeParseException e) {
                    return errorResponse("Invalid fromDate format. Expected: yyyy-MM-dd HH:mm:ss", 400);
                }
            }

            Date toDate = null;
            if (toDateStr != null && !toDateStr.trim().isEmpty()) {
                try {
                    toDate = parseDate(toDateStr.trim());
                } catch (DateTimeParseException e) {
                    return errorResponse("Invalid toDate format. Expected: yyyy-MM-dd HH:mm:ss", 400);
                }
            }

            HistoryType historyType = null;
            if (historyTypeStr != null && !historyTypeStr.trim().isEmpty()) {
                try {
                    historyType = HistoryType.valueOf(historyTypeStr.trim());
                } catch (IllegalArgumentException e) {
                    return errorResponse("Invalid historyType: " + historyTypeStr, 400);
                }
            }

            int maxResults = (limit != null && limit > 0) ? limit : 100;

            List<StockHistoryDTO> results = stockHistoryFacade.findStockHistoryDtos(
                    itemId,
                    departmentId,
                    billId,
                    fromDate,
                    toDate,
                    historyType,
                    maxResults
            );

            return successResponse(results);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    private Date parseDate(String dateStr) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, DATE_FORMATTER);
        return java.sql.Timestamp.valueOf(localDateTime);
    }

    private boolean isValidKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        ApiKey k = apiKeyController.findApiKey(key);
        if (k == null || k.getWebUser() == null) {
            return false;
        }
        if (k.getWebUser().isRetired() || !k.getWebUser().isActivated()) {
            return false;
        }
        if (k.getDateOfExpiary() == null || k.getDateOfExpiary().before(new Date())) {
            return false;
        }
        return true;
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
