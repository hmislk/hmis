package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.dto.PharmacyF15ReportDTO;
import com.divudi.core.entity.ApiKey;
import com.divudi.service.pharmacy.PharmacyF15ReportApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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
 * REST API for the AF15 Daily Stock Values Report.
 *
 * Endpoint: GET /api/pharmacy_f15_report
 * Auth:     Finance: <api_key>  header
 * Params:   date=yyyy-MM-dd  &  departmentId=<long>
 *
 * Returns the full F15 report in JSON, including:
 *   - Opening stock (retail + cost rate)
 *   - Sales, purchases, transfers, adjustments (all rows + totals)
 *   - Closing stock (retail + cost rate)
 *   - Balance check with discrepancy calculation
 *
 * The report uses the same service methods as the JSF page
 * (PharmacyDailyStockReportOptimizedController), so any fix to the
 * underlying PharmacyService automatically applies here too.
 */
@Path("pharmacy_f15_report")
@RequestScoped
public class PharmacyF15ReportApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private PharmacyF15ReportApiService f15ReportService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeSpecialFloatingPointValues()
            .create();

    private static final SimpleDateFormat DATE_PARAM_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public PharmacyF15ReportApi() {
    }

    /**
     * Generate the full AF15 Daily Stock Values Report.
     *
     * Example:
     *   GET /api/pharmacy_f15_report?date=2026-02-18&departmentId=485
     *   Header: Finance: bdc3775d-f07a-4c15-855d-9e201fa4af84
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateF15Report(
            @QueryParam("date") String dateStr,
            @QueryParam("departmentId") Long departmentId) {

        // Validate API key
        String key = requestContext.getHeader("Finance");
        if (!isValidKey(key)) {
            return errorResponse("Not a valid key", 401);
        }

        // Validate required parameters
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return errorResponse("Parameter 'date' is required (format: yyyy-MM-dd)", 400);
        }
        if (departmentId == null) {
            return errorResponse("Parameter 'departmentId' is required", 400);
        }

        // Parse date
        Date date;
        try {
            synchronized (DATE_PARAM_FORMAT) {
                date = DATE_PARAM_FORMAT.parse(dateStr.trim());
            }
        } catch (ParseException e) {
            return errorResponse("Invalid date format. Use yyyy-MM-dd (e.g. 2026-02-18)", 400);
        }

        // Generate report
        PharmacyF15ReportDTO report;
        try {
            report = f15ReportService.generateReport(date, departmentId);
        } catch (Exception e) {
            return errorResponse("Error generating report: " + e.getMessage(), 500);
        }

        if (report == null) {
            return errorResponse("Department not found with ID: " + departmentId, 404);
        }

        return successResponse(report);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
