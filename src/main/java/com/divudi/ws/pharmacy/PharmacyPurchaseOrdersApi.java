package com.divudi.ws.pharmacy;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.Privileges;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.WebUserPrivilege;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.WebUserPrivilegeFacade;
import com.divudi.service.pharmacy.PharmacyPoCancellationException;
import com.divudi.service.pharmacy.PharmacyPurchaseOrderApprovalCancellationService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Pharmacy Purchase Order approval status and cancellation, backing issue
 * #23944. Two endpoints:
 * <ul>
 * <li>{@code GET /status} - resolves a PO *request* bill (by number or id)
 * and reports its approval history and any GRNs raised against the live
 * approval, so a caller can decide whether cancelling the approval is
 * possible.</li>
 * <li>{@code POST /approvals/{approvalBillId}/cancel} - cancels a Pharmacy
 * Purchase Order Approval bill via the shared
 * {@link PharmacyPurchaseOrderApprovalCancellationService}, the same
 * service the JSF UI's {@code PharmacyBillSearch.pharmacyPoCancel()} calls.</li>
 * </ul>
 *
 * @author Buddhika
 */
@Path("pharmacy_purchase_orders")
@RequestScoped
public class PharmacyPurchaseOrdersApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private BillFacade billFacade;

    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;

    @EJB
    private PharmacyPurchaseOrderApprovalCancellationService cancellationService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus(@QueryParam("number") String number, @QueryParam("billId") Long billId) {
        try {
            WebUser apiUser = validateApiKey(requestContext.getHeader("Finance"));
            if (apiUser == null) {
                return errorResponse("Not a valid key", 401);
            }

            Bill request = resolveRequestBill(number, billId);
            if (request == null) {
                return errorResponse("No Pharmacy Purchase Order request bill found", 404);
            }

            Map<String, Object> requestJson = new LinkedHashMap<>();
            requestJson.put("id", request.getId());
            requestJson.put("billNumber", request.getDeptId());
            requestJson.put("finalized", request.isCompleted());
            requestJson.put("cancelled", request.isCancelled());

            // FROM BilledBill (not Bill): the CancelledBill created when an approval is
            // cancelled is itself given billTypeAtomic=PHARMACY_ORDER_APPROVAL_CANCELLED
            // and referenceBill=request (mirroring the original approval's own values,
            // same as every other cancellation contra-bill in this codebase) - confirmed
            // live that querying FROM Bill double-lists it as a phantom third "approval"
            // alongside the real one it cancels. BilledBill's single-table-inheritance
            // discriminator excludes CancelledBill rows even though they share the table.
            //
            // A cancelled approval keeps billTypeAtomic=PHARMACY_ORDER_APPROVAL (only
            // `cancelled`/`cancelledBill` change - confirmed live against bill 1323535
            // after cancelling it) - PHARMACY_ORDER_APPROVAL_CANCELLED is only ever set
            // on the CancelledBill itself, which FROM BilledBill now excludes, so a
            // single type is enough here.
            String approvalsJpql = "SELECT a FROM BilledBill a WHERE a.referenceBill.id = :requestId "
                    + "AND a.billTypeAtomic = :type ORDER BY a.createdAt";
            Map<String, Object> approvalsParams = new HashMap<>();
            approvalsParams.put("requestId", request.getId());
            approvalsParams.put("type", BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
            List<Bill> approvalBills = billFacade.findByJpql(approvalsJpql, approvalsParams);

            List<Map<String, Object>> approvals = new ArrayList<>();
            Bill liveApproval = null;
            for (Bill approval : approvalBills) {
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("id", approval.getId());
                a.put("billNumber", approval.getDeptId());
                a.put("cancelled", approval.isCancelled());
                if (approval.isCancelled()) {
                    Bill cancelledBill = approval.getCancelledBill();
                    a.put("cancelledBillId", cancelledBill != null ? cancelledBill.getId() : null);
                    a.put("cancelledBillNumber", cancelledBill != null ? cancelledBill.getDeptId() : null);
                }
                approvals.add(a);
            }

            // The request's own referenceBill points forward at its currently-live
            // (non-cancelled) approval while one exists - see
            // PharmacyPurchaseOrderApprovalCancellationService.cancelApproval() for why
            // that link is only cleared on the request bill, never on the approval.
            if (request.getReferenceBill() != null && !request.getReferenceBill().isCancelled()) {
                liveApproval = request.getReferenceBill();
            }

            List<Map<String, Object>> grns = new ArrayList<>();
            if (liveApproval != null) {
                // NOTE: does NOT reuse PharmacyBillSearch.checkGrn()'s BillType.PharmacyOrder
                // filter - live-testing this endpoint against real local data showed that
                // filter always matches the PO *request* bill itself (BillType.PharmacyOrder
                // is the request's own BillType per BillTypeAtomic.PHARMACY_ORDER, and a live
                // approval's request has request.referenceBill = approval), never an actual
                // GRN. Real GRN bills use BillType.PharmacyGrnBill/PharmacyGrnBillImport (see
                // GrnController.java/GrnCostingController.java) - see the fuller writeup in
                // PharmacyPurchaseOrderApprovalCancellationService.checkGrnBlocksCancellation().
                String grnJpql = "Select b From BilledBill b where b.retired=false and b.creater is not null"
                        + " and b.cancelled=false and b.billType IN :btps and "
                        + " b.referenceBill=:ref and b.referenceBill.cancelled=false ";
                Map<String, Object> grnParams = new HashMap<>();
                grnParams.put("ref", liveApproval);
                grnParams.put("btps", Arrays.asList(BillType.PharmacyGrnBill, BillType.PharmacyGrnBillImport));
                List<Bill> grnBills = billFacade.findByJpql(grnJpql, grnParams);
                for (Bill grnBill : grnBills) {
                    Map<String, Object> g = new LinkedHashMap<>();
                    g.put("id", grnBill.getId());
                    g.put("billNumber", grnBill.getDeptId());
                    g.put("cancelled", grnBill.isCancelled());
                    grns.add(g);
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("request", requestJson);
            result.put("approvals", approvals);
            result.put("grns", grns);

            return successResponse(result);
        } catch (Exception ex) {
            return errorResponse("An error occurred: " + ex.getMessage(), 500);
        }
    }

    @POST
    @Path("/approvals/{approvalBillId}/cancel")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cancelApproval(@PathParam("approvalBillId") Long approvalBillId, String requestBody) {
        try {
            WebUser apiUser = validateApiKey(requestContext.getHeader("Finance"));
            if (apiUser == null) {
                return errorResponse("Not a valid key", 401);
            }

            // Loaded (and its department used for the privilege check) before
            // validating the request body - the department-scoped privilege check
            // needs the target bill either way, and UI-parity requires it (see below).
            Bill approval = billFacade.find(approvalBillId);
            if (approval == null) {
                return errorResponse("No Bill to cancel", 404);
            }

            // Scoped to the approval's own department, matching the UI: WebUserController
            // .hasPrivilege() there is implicitly department-scoped because it reads
            // getSessionController().getUserPrivileges(), which is filtered to the
            // logged-in user's currently-selected department. An unscoped check here
            // would let a user with PharmacyOrderCancellation in ANY department cancel
            // an approval in a completely different one via the API (#23988 review).
            if (!hasPrivilege(apiUser, "PharmacyOrderCancellation", approval.getDepartment())) {
                return errorResponse("API user does not have the PharmacyOrderCancellation privilege", 403);
            }

            CancelApprovalRequest request;
            try {
                request = gson.fromJson(requestBody, CancelApprovalRequest.class);
            } catch (JsonSyntaxException ex) {
                return errorResponse("Invalid JSON format: " + ex.getMessage(), 400);
            }

            if (request == null) {
                return errorResponse("Request body is required", 400);
            }
            if (request.getAuditComment() == null || request.getAuditComment().trim().isEmpty()) {
                return errorResponse("auditComment is required", 400);
            }
            if (request.getApprovedBy() == null || request.getApprovedBy().trim().isEmpty()) {
                return errorResponse("approvedBy is required", 400);
            }

            Long requestBillId = approval.getReferenceBill() != null
                    ? approval.getReferenceBill().getId() : null;

            // approvedBy has no dedicated column on the cancellation bill (unlike
            // BillDataCorrectionService, which keeps it in a separate audit log) -
            // folded into the stored comment so who authorized the cancellation isn't
            // silently discarded despite being a mandatory field (#23988 review).
            String comment = request.getAuditComment().trim()
                    + " (approved by: " + request.getApprovedBy().trim() + ")";

            CancelledBill cancelledBill;
            try {
                cancelledBill = cancellationService.cancelApproval(approvalBillId, comment, apiUser);
            } catch (PharmacyPoCancellationException ex) {
                return errorResponse(ex.getMessage(), statusForReason(ex.getReason()));
            }

            // A request can carry more than one un-cancelled approval on record (an
            // earlier, superseded one alongside the current live one) - confirmed via
            // live-testing against real local data. The cancelled bill only becomes
            // "pending re-approval" when the request's CURRENT link actually pointed
            // at the approval just cancelled; the shared service only clears that link
            // in that case, so re-reading it here tells us which happened.
            Bill requestAfterCancel = requestBillId != null ? billFacade.find(requestBillId) : null;
            boolean pendingApproval = requestAfterCancel != null && requestAfterCancel.getReferenceBill() == null;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("cancelledBillId", cancelledBill.getId());
            result.put("cancelledBillNumber", cancelledBill.getDeptId());
            result.put("requestBillId", requestBillId);
            result.put("pendingApproval", pendingApproval);

            return successResponse(result);
        } catch (Exception ex) {
            return errorResponse("An error occurred: " + ex.getMessage(), 500);
        }
    }

    private int statusForReason(PharmacyPoCancellationException.Reason reason) {
        switch (reason) {
            case NOT_FOUND:
                return 404;
            case NOT_APPROVAL_TYPE:
            case ALREADY_CANCELLED:
            case CONFIG_DISABLED:
            case GRN_EXISTS:
                return 409;
            default:
                return 400;
        }
    }

    /**
     * Resolves the PO request bill either by numeric id or by its deptId bill
     * number, mirroring {@code CostingData.getBillByNumberQuery()}'s
     * deptId-equality lookup (the recommended pattern for bill numbers that
     * may contain '/', per developer_docs/api/using-apis/API_BILL_NUMBER_SOLUTION.md).
     */
    private Bill resolveRequestBill(String number, Long billId) {
        if (billId != null) {
            Bill b = billFacade.find(billId);
            // Reject anything that isn't actually a PO request - otherwise passing
            // e.g. an approval's id here silently succeeds and reports it as the
            // "request", with an empty approvals list (#23988 review).
            return b != null && b.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_ORDER ? b : null;
        }
        if (number == null || number.trim().isEmpty()) {
            return null;
        }
        String jpql = "SELECT b FROM Bill b WHERE b.deptId = :deptId AND b.billTypeAtomic = :bta ORDER BY b.id DESC";
        Map<String, Object> params = new HashMap<>();
        params.put("deptId", number.trim());
        params.put("bta", BillTypeAtomic.PHARMACY_ORDER);
        List<Bill> bills = billFacade.findByJpql(jpql, params, 1);
        return bills == null || bills.isEmpty() ? null : bills.get(0);
    }

    /**
     * Checks whether the given (already API-key-resolved) WebUser has the
     * named privilege **in the given department**. Mirrors
     * {@code WebUserController.checkPrivilege(WebUser, String, Department)} -
     * scoped to a department, like the UI's own check effectively is (its
     * {@code hasPrivilege(String)} reads {@code getSessionController()
     * .getUserPrivileges()}, which is filtered to the logged-in user's
     * currently-selected department). An unscoped check would let a user
     * with the privilege in one department cancel an approval in a totally
     * different one via the API (#23988 review). {@code WebUserController}
     * itself is {@code @SessionScoped} and cannot be injected into this
     * {@code @RequestScoped} REST resource, so the query is run directly.
     */
    private boolean hasPrivilege(WebUser user, String privilegeName, Department department) {
        if (user == null || department == null) {
            return false;
        }
        Privileges privilege;
        try {
            privilege = Privileges.valueOf(privilegeName);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        String jpql = "SELECT w FROM WebUserPrivilege w WHERE w.webUser = :user "
                + "AND w.department = :department AND w.privilege = :privilege AND w.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("department", department);
        params.put("privilege", privilege);
        WebUserPrivilege wup = webUserPrivilegeFacade.findFirstByJpql(jpql, params);
        return wup != null;
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null || apiKey.isRetired() || apiKey.getDateOfExpiary() == null
                || apiKey.getDateOfExpiary().before(new java.util.Date())) {
            return null;
        }
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) {
            return null;
        }
        return user;
    }

    private Response successResponse(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.ok(gson.toJson(response), MediaType.APPLICATION_JSON).build();
    }

    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(gson.toJson(response)).type(MediaType.APPLICATION_JSON).build();
    }

    public static class CancelApprovalRequest {

        private String auditComment;
        private String approvedBy;

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
