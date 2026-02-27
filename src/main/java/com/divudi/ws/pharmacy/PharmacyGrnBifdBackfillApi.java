package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.pharmacy.PharmacyGrnBifdBackfillService;
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
 * REST API for backfilling missing BillItemFinanceDetails (BIFD) and
 * BillFinanceDetails (BFD) on historical Pharmacy GRN bills.
 *
 * GRN bills saved before the BIFD system was introduced have null
 * billItemFinanceDetails on their BillItems. The GRN reprint shows blank
 * columns (Purchase Rate, Line Net Value, Sale Rate, Cost Value) for those
 * bills because the reprint template reads from BIFD. This endpoint
 * reconstructs BIFD/BFD from the PharmaceuticalBillItem data that was
 * correctly saved at GRN creation time.
 *
 * Endpoint : POST /api/pharmacy/backfill_grn_bifd
 * Auth     : Finance: &lt;api_key&gt; header
 *
 * Example request:
 * <pre>
 * {
 *   "fromDate": "2025-01-01",
 *   "toDate": "2026-02-26",
 *   "approvedBy": "Dr. Ariyaratne",
 *   "auditComment": "Backfill BIFD for GRN bills missing finance details",
 *   "departmentId": 485,
 *   "billTypeAtomics": ["PHARMACY_GRN"]
 * }
 * </pre>
 *
 * departmentId and billTypeAtomics are optional.
 * When omitted, all departments and all GRN bill types are processed.
 *
 * Example response:
 * <pre>
 * {
 *   "status": "success",
 *   "code": 200,
 *   "data": {
 *     "totalBillsFound": 150,
 *     "processedBills": 148,
 *     "skippedBills": 2,
 *     "processedItems": 620,
 *     "skippedItems": 4,
 *     "errors": []
 *   }
 * }
 * </pre>
 */
@Path("pharmacy/backfill_grn_bifd")
@RequestScoped
public class PharmacyGrnBifdBackfillApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private PharmacyGrnBifdBackfillService backfillService;

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Backfill BIFD/BFD for GRN bills missing finance details.
     *
     * POST /api/pharmacy/backfill_grn_bifd
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response backfillGrnBifd(String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            BackfillRequest request;
            try {
                request = GSON.fromJson(requestBody, BackfillRequest.class);
            } catch (JsonSyntaxException ex) {
                return errorResponse("Invalid JSON: " + ex.getMessage(), 400);
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
                return errorResponse("Invalid date format. Use yyyy-MM-dd", 400);
            }

            Map<String, Object> result = backfillService.backfillGrnBifds(
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
        return Response.ok(GSON.toJson(response), MediaType.APPLICATION_JSON).build();
    }

    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(GSON.toJson(response))
                .type(MediaType.APPLICATION_JSON).build();
    }

    // -------------------------------------------------------------------------
    // Request DTO
    // -------------------------------------------------------------------------

    public static class BackfillRequest {

        /** Required. Start date inclusive, format yyyy-MM-dd. */
        private String fromDate;

        /** Required. End date inclusive, format yyyy-MM-dd. */
        private String toDate;

        /** Required. Audit trail comment appended to bill.comments. */
        private String auditComment;

        /** Required. Name of the person approving this correction. */
        private String approvedBy;

        /**
         * Optional. Department ID to limit scope.
         * Null means all departments.
         */
        private Long departmentId;

        /**
         * Optional. List of BillTypeAtomic names to process.
         * Null or empty means all default GRN types:
         * PHARMACY_GRN, PHARMACY_GRN_CANCELLED, PHARMACY_GRN_REFUND,
         * PHARMACY_GRN_RETURN, PHARMACY_GRN_WHOLESALE,
         * PHARMACY_DIRECT_PURCHASE, PHARMACY_DIRECT_PURCHASE_CANCELLED.
         */
        private List<String> billTypeAtomics;

        public String getFromDate() { return fromDate; }
        public void setFromDate(String fromDate) { this.fromDate = fromDate; }

        public String getToDate() { return toDate; }
        public void setToDate(String toDate) { this.toDate = toDate; }

        public String getAuditComment() { return auditComment; }
        public void setAuditComment(String auditComment) { this.auditComment = auditComment; }

        public String getApprovedBy() { return approvedBy; }
        public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

        public List<String> getBillTypeAtomics() { return billTypeAtomics; }
        public void setBillTypeAtomics(List<String> billTypeAtomics) { this.billTypeAtomics = billTypeAtomics; }
    }
}
