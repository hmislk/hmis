package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.pharmacy.PharmacyTransferDeptTypeBackfillService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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
 * REST API for backfilling the missing departmentType on historical pharmacy
 * transfer bills (issue / receive / cancellations / returns). See issues
 * #22056 (capture regression), #22057 / #22058 (data corrections) and #22067
 * (this endpoint).
 *
 * Endpoint: POST /api/pharmacy/backfill_transfer_department_type
 * Auth:     Finance: &lt;api_key&gt; header
 *
 * Dry-run by default: with "apply": false (or omitted) the response contains
 * the full resolution plan without writing anything. Re-run with
 * "apply": true to persist it.
 *
 * Example request:
 * <pre>
 * {
 *   "fromDate": "2026-05-01",
 *   "toDate": "2026-06-30",
 *   "departmentId": 485,
 *   "apply": false,
 *   "approvedBy": "Dr. Smith",
 *   "auditComment": "Backfill departmentType missing during #22056 regression window"
 * }
 * </pre>
 *
 * Example response data:
 * <pre>
 * {
 *   "apply": false,
 *   "totalCandidates": 50,
 *   "resolvedFromItems": 47,
 *   "resolvedFromBackwardReferenceBill": 2,
 *   "resolvedFromBilledBill": 1,
 *   "unresolved": 0,
 *   "appliedCount": 0,
 *   "unresolvedBillIds": [],
 *   "plan": [{"billId": 1, "deptId": "PH/TI/24/001", "billTypeAtomic": "PHARMACY_ISSUE",
 *             "resolvedDepartmentType": "Store", "source": "ITEMS"}],
 *   "errors": []
 * }
 * </pre>
 */
@Path("pharmacy/backfill_transfer_department_type")
@RequestScoped
public class PharmacyTransferDeptTypeBackfillApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private PharmacyTransferDeptTypeBackfillService backfillService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Backfill departmentType for historical transfer bills.
     *
     * POST /api/pharmacy/backfill_transfer_department_type
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response backfillTransferDepartmentType(String requestBody) {
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
                return errorResponse("Invalid date format. Use yyyy-MM-dd (e.g. 2026-05-30)", 400);
            }

            Map<String, Object> result = backfillService.backfillTransferDepartmentTypes(
                    request.getDepartmentId(),
                    fromDate,
                    toDate,
                    request.isApply(),
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

        /** Optional bill.department ID filter. Null means all departments. */
        private Long departmentId;

        /** Start date inclusive, format yyyy-MM-dd. Required. */
        private String fromDate;

        /** End date inclusive, format yyyy-MM-dd. Required. */
        private String toDate;

        /** False (default) = dry run returning the plan; true = persist. */
        private boolean apply;

        /** Mandatory audit trail comment. */
        private String auditComment;

        /** Mandatory approver name. */
        private String approvedBy;

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

        public boolean isApply() {
            return apply;
        }

        public void setApply(boolean apply) {
            this.apply = apply;
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
