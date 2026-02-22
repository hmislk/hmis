package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.pharmacy.PharmacyBfdBackfillService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST API for backfilling missing BillFinanceDetails (BFD) on historical
 * PHARMACY_STOCK_ADJUSTMENT and PHARMACY_RETAIL_RATE_ADJUSTMENT bills.
 *
 * Endpoint: POST /api/pharmacy/backfill_bfd
 * Auth:     Finance: <api_key>  header
 *
 * These bill types were saved without BFD records (or with bill.total = 0)
 * before the 2026-02-23 fix. This endpoint creates / corrects BFD values
 * derived from the PharmaceuticalBillItem records that were correctly saved.
 *
 * Example request:
 * <pre>
 * {
 *   "billTypeAtomics": ["PHARMACY_STOCK_ADJUSTMENT", "PHARMACY_RETAIL_RATE_ADJUSTMENT"],
 *   "departmentId": 485,
 *   "fromDate": "2025-01-01",
 *   "toDate": "2026-02-22",
 *   "approvedBy": "Dr. Smith",
 *   "auditComment": "Backfill BFDs missing before 2026-02-23 fix"
 * }
 * </pre>
 *
 * Example response:
 * <pre>
 * {
 *   "status": "success",
 *   "code": 200,
 *   "data": {
 *     "backfilledBills": 12,
 *     "skipped": 2,
 *     "errors": []
 *   }
 * }
 * </pre>
 */
@Path("pharmacy/backfill_bfd")
@RequestScoped
public class PharmacyBfdBackfillApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private PharmacyBfdBackfillService backfillService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Backfill BillFinanceDetails for historical adjustment bills.
     *
     * POST /api/pharmacy/backfill_bfd
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response backfillBfd(String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            BackfillRequest request;
            try {
                request = gson.fromJson(requestBody, BackfillRequest.class);
            } catch (JsonSyntaxException ex) {
                return errorResponse("Invalid JSON format: " + ex.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }
            if (request.getFromDate() == null || request.getFromDate().trim().isEmpty()) {
                return errorResponse("fromDate is required (format: yyyy-MM-dd)", 400);
            }
            if (request.getToDate() == null || request.getToDate().trim().isEmpty()) {
                return errorResponse("toDate is required (format: yyyy-MM-dd)", 400);
            }
            if (request.getAuditComment() == null || request.getAuditComment().trim().isEmpty()) {
                return errorResponse("auditComment is required", 400);
            }
            if (request.getApprovedBy() == null || request.getApprovedBy().trim().isEmpty()) {
                return errorResponse("approvedBy is required", 400);
            }

            Date fromDate;
            Date toDate;
            try {
                synchronized (DATE_FORMAT) {
                    fromDate = DATE_FORMAT.parse(request.getFromDate().trim());
                    toDate = DATE_FORMAT.parse(request.getToDate().trim());
                }
            } catch (ParseException ex) {
                return errorResponse("Invalid date format. Use yyyy-MM-dd (e.g. 2026-02-22)", 400);
            }

            Map<String, Object> result = backfillService.backfillAdjustmentBfds(
                    request.getBillTypeAtomics(),
                    request.getDepartmentId(),
                    fromDate,
                    toDate,
                    request.getAuditComment().trim(),
                    request.getApprovedBy().trim(),
                    user);

            return successResponse(result);

        } catch (IllegalArgumentException ex) {
            return errorResponse(ex.getMessage(), 400);
        } catch (Exception ex) {
            return errorResponse("An error occurred: " + ex.getMessage(), 500);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null || apiKey.getDateOfExpiary() == null
                || apiKey.getDateOfExpiary().before(new Date())) {
            return null;
        }
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) {
            return null;
        }
        return user;
    }

    private Response successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.ok(gson.toJson(response), MediaType.APPLICATION_JSON).build();
    }

    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(gson.toJson(response))
                .type(MediaType.APPLICATION_JSON).build();
    }

    // -------------------------------------------------------------------------
    // Request DTO
    // -------------------------------------------------------------------------

    public static class BackfillRequest {

        /** Optional list of bill type atomics to backfill. Defaults to both adjustment types. */
        private List<String> billTypeAtomics;

        /** Optional department ID filter. Null means all departments. */
        private Long departmentId;

        /** Start date inclusive, format yyyy-MM-dd. Required. */
        private String fromDate;

        /** End date inclusive, format yyyy-MM-dd. Required. */
        private String toDate;

        /** Mandatory audit trail comment. */
        private String auditComment;

        /** Mandatory approver name. */
        private String approvedBy;

        public List<String> getBillTypeAtomics() {
            return billTypeAtomics;
        }

        public void setBillTypeAtomics(List<String> billTypeAtomics) {
            this.billTypeAtomics = billTypeAtomics;
        }

        public Long getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(Long departmentId) {
            this.departmentId = departmentId;
        }

        public String getFromDate() {
            return fromDate;
        }

        public void setFromDate(String fromDate) {
            this.fromDate = fromDate;
        }

        public String getToDate() {
            return toDate;
        }

        public void setToDate(String toDate) {
            this.toDate = toDate;
        }

        public String getAuditComment() {
            return auditComment;
        }

        public void setAuditComment(String auditComment) {
            this.auditComment = auditComment;
        }

        public String getApprovedBy() {
            return approvedBy;
        }

        public void setApprovedBy(String approvedBy) {
            this.approvedBy = approvedBy;
        }
    }
}
