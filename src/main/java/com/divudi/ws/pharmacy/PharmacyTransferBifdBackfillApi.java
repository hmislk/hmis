package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.pharmacy.PharmacyTransferBifdBackfillService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
 * BillFinanceDetails (BFD) on historical pharmacy stock transfer bills
 * (issue and receive), issue #24721.
 *
 * Transfers saved before finance details existed show 0 in the Stock Transfer
 * Report. This endpoint rebuilds BIFD/BFD from the PharmaceuticalBillItem data
 * saved at transfer time. Run month by month and use dryRun first.
 *
 * Endpoint : POST /api/pharmacy/backfill_transfer_bifd
 * Auth     : Finance: &lt;api_key&gt; header
 *
 * Example request:
 * <pre>
 * {
 *   "fromDate": "2025-01-01",
 *   "toDate": "2025-01-31",
 *   "approvedBy": "Pharmacy Manager",
 *   "auditComment": "Backfill finance details for historical stock transfers",
 *   "departmentId": 1,
 *   "billTypeAtomics": ["PHARMACY_ISSUE", "PHARMACY_RECEIVE"],
 *   "dryRun": true
 * }
 * </pre>
 *
 * departmentId, billTypeAtomics and dryRun are optional (defaults: all
 * departments, both transfer types, dryRun=false).
 *
 * Example response:
 * <pre>
 * {
 *   "status": "success",
 *   "code": 200,
 *   "data": {
 *     "totalBillsFound": 120,
 *     "processedBills": 118,
 *     "skippedBills": 2,
 *     "createdBifds": 410,
 *     "filledBifds": 0,
 *     "createdBfds": 95,
 *     "filledBfds": 0,
 *     "skippedItems": 1,
 *     "costFallbackItems": 300,
 *     "totalPurchaseValue": 1250000.0,
 *     "totalRetailValue": 1500000.0,
 *     "totalCostValue": 1250000.0,
 *     "dryRun": true,
 *     "errors": []
 *   }
 * }
 * </pre>
 */
@Path("pharmacy/backfill_transfer_bifd")
@RequestScoped
public class PharmacyTransferBifdBackfillApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private PharmacyTransferBifdBackfillService backfillService;

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * Backfill BIFD/BFD for stock transfer bills missing finance details.
     *
     * POST /api/pharmacy/backfill_transfer_bifd
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response backfillTransferBifd(String requestBody) {
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
                // LocalDate.parse is strict: rejects 2025-02-30 and trailing text
                fromDate = Date.from(LocalDate.parse(request.getFromDate().trim())
                        .atStartOfDay(ZoneId.systemDefault()).toInstant());
                toDate = Date.from(LocalDate.parse(request.getToDate().trim())
                        .atStartOfDay(ZoneId.systemDefault()).toInstant());
            } catch (DateTimeParseException ex) {
                return errorResponse("Invalid date format. Use yyyy-MM-dd", 400);
            }
            if (fromDate.after(toDate)) {
                return errorResponse("fromDate must not be after toDate", 400);
            }

            List<BillTypeAtomic> types = new ArrayList<>();
            if (request.getBillTypeAtomics() != null) {
                for (String name : request.getBillTypeAtomics()) {
                    BillTypeAtomic bta = null;
                    if (name != null) {
                        try {
                            bta = BillTypeAtomic.valueOf(name.trim());
                        } catch (IllegalArgumentException ex) {
                            bta = null;
                        }
                    }
                    if (bta == null || !PharmacyTransferBifdBackfillService.SUPPORTED_TYPES.contains(bta)) {
                        return errorResponse("Unknown or unsupported billTypeAtomic: " + name
                                + ". Supported: " + PharmacyTransferBifdBackfillService.SUPPORTED_TYPES, 400);
                    }
                    types.add(bta);
                }
            }

            boolean dryRun = Boolean.TRUE.equals(request.getDryRun());

            Map<String, Object> result = backfillService.backfillTransferBifds(
                    types,
                    request.getDepartmentId(),
                    fromDate,
                    toDate,
                    request.getAuditComment().trim(),
                    request.getApprovedBy().trim(),
                    user,
                    dryRun);

            return successResponse(result);

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
        if (apiKey == null || apiKey.isRetired() || apiKey.getDateOfExpiary() == null
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

        /** Optional. Department ID to limit scope. Null means all departments. */
        private Long departmentId;

        /**
         * Optional. BillTypeAtomic names to process. Null or empty means both:
         * PHARMACY_ISSUE, PHARMACY_RECEIVE.
         */
        private List<String> billTypeAtomics;

        /** Optional. True = compute and report only, persist nothing. Default false. */
        private Boolean dryRun;

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

        public Boolean getDryRun() { return dryRun; }
        public void setDryRun(Boolean dryRun) { this.dryRun = dryRun; }
    }
}
