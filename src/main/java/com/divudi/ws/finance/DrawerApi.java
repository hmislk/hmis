package com.divudi.ws.finance;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Privileges;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.facade.WebUserPrivilegeFacade;
import com.divudi.service.DrawerService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
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
 * REST API to read and reset a cashier's drawer balance per payment method,
 * for QA/E2E test setup (issue #24433). Mirrors the "Adjust Drawer Balance ->
 * Admin" UI flow in DrawerAdjustmentController, going through the same
 * DrawerEntry/Bill audit trail via DrawerService.
 *
 * @author Dr M H B Ariyaratne
 */
@Path("drawer")
@RequestScoped
public class DrawerApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private DrawerService drawerService;

    @EJB
    private WebUserFacade webUserFacade;

    @EJB
    private WebUserPrivilegeFacade webUserPrivilegeFacade;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    @GET
    @Path("/{webUserId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDrawerBalance(@PathParam("webUserId") Long webUserId,
            @QueryParam("paymentMethod") String paymentMethodParam) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser caller = validateApiKey(key);
            if (caller == null) {
                return errorResponse("Not a valid key", 401);
            }

            WebUser targetUser = webUserFacade.find(webUserId);
            if (targetUser == null) {
                return errorResponse("User not found", 404);
            }

            // Never auto-create a Drawer as a side effect of a read (D6) -
            // a user who was never adjusted is reported as all-zero balances.
            Drawer drawer = drawerService.findUsersDrawerWithoutCreate(targetUser);

            if (paymentMethodParam != null && paymentMethodParam.trim().isEmpty()) {
                return errorResponse("paymentMethod cannot be blank; omit it to read every supported payment method", 400);
            }

            if (paymentMethodParam != null) {
                PaymentMethod pm;
                try {
                    pm = PaymentMethod.valueOf(paymentMethodParam.trim());
                } catch (IllegalArgumentException ex) {
                    return errorResponse("Invalid paymentMethod: " + paymentMethodParam, 400);
                }
                if (!drawerService.getAdjustablePaymentMethods().contains(pm)) {
                    return errorResponse("Unsupported paymentMethod: " + paymentMethodParam, 400);
                }
                Double balance = drawerService.getDrawerInHandValue(drawer, pm);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("webUserId", webUserId);
                data.put("paymentMethod", pm.name());
                data.put("balance", balance != null ? balance : 0.0);
                return successResponse(data);
            }

            Map<String, Double> balances = new LinkedHashMap<>();
            for (PaymentMethod pm : drawerService.getAdjustablePaymentMethods()) {
                Double balance = drawerService.getDrawerInHandValue(drawer, pm);
                balances.put(pm.name(), balance != null ? balance : 0.0);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("webUserId", webUserId);
            data.put("balances", balances);
            return successResponse(data);
        } catch (IllegalArgumentException ex) {
            return errorResponse(ex.getMessage(), 400);
        } catch (Exception ex) {
            return errorResponse("An error occurred: " + ex.getMessage(), 500);
        }
    }

    @POST
    @Path("/adjust")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response adjustDrawerBalance(String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser actor = validateApiKey(key);
            if (actor == null) {
                return errorResponse("Not a valid key", 401);
            }

            if (!hasDrawerAdjustmentDirectPrivilege(actor)) {
                return errorResponse("Insufficient privileges", 403);
            }

            DrawerAdjustRequest request;
            try {
                request = gson.fromJson(requestBody, DrawerAdjustRequest.class);
            } catch (JsonSyntaxException ex) {
                return errorResponse("Invalid JSON format: " + ex.getMessage(), 400);
            }

            if (request == null || request.getWebUserId() == null) {
                return errorResponse("webUserId is required", 400);
            }

            WebUser targetUser = webUserFacade.find(request.getWebUserId());
            if (targetUser == null) {
                return errorResponse("User not found", 404);
            }
            if (targetUser.getDepartment() == null || targetUser.getInstitution() == null) {
                // Checked here, not just in DrawerService, because an IllegalArgumentException
                // thrown from inside a @Stateless EJB method is wrapped as EJBException by the
                // container and would otherwise surface as a generic 500 instead of a 400.
                return errorResponse("Target user has no department/institution set; cannot generate an adjustment bill number", 400);
            }

            PaymentMethod paymentMethod = null;
            String paymentMethodParam = request.getPaymentMethod();
            if (paymentMethodParam != null && paymentMethodParam.trim().isEmpty()) {
                return errorResponse("paymentMethod cannot be blank; omit it to reset every supported payment method", 400);
            }
            if (paymentMethodParam != null) {
                try {
                    paymentMethod = PaymentMethod.valueOf(paymentMethodParam.trim());
                } catch (IllegalArgumentException ex) {
                    return errorResponse("Invalid paymentMethod: " + paymentMethodParam, 400);
                }
                if (!drawerService.getAdjustablePaymentMethods().contains(paymentMethod)) {
                    return errorResponse("Unsupported paymentMethod: " + paymentMethodParam, 400);
                }
            }

            if (request.getTargetBalance() != null && !Double.isFinite(request.getTargetBalance())) {
                return errorResponse("targetBalance must be a finite number", 400);
            }
            double targetBalance = request.getTargetBalance() != null ? request.getTargetBalance() : 0.0;

            List<DrawerService.DrawerBalanceSnapshot> snapshots = drawerService.resetDrawerBalance(
                    targetUser, paymentMethod, targetBalance, request.getComment(), actor);

            List<Map<String, Object>> adjustments = new ArrayList<>();
            for (DrawerService.DrawerBalanceSnapshot snapshot : snapshots) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("paymentMethod", snapshot.getPaymentMethod().name());
                row.put("before", snapshot.getBefore());
                row.put("after", snapshot.getAfter());
                row.put("changed", snapshot.isChanged());
                adjustments.add(row);
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("webUserId", request.getWebUserId());
            data.put("adjustedBy", actor.getName());
            data.put("adjustments", adjustments);

            return successResponse(data);
        } catch (IllegalArgumentException ex) {
            return errorResponse(ex.getMessage(), 400);
        } catch (Exception ex) {
            return errorResponse("An error occurred: " + ex.getMessage(), 500);
        }
    }

    /**
     * Stateless equivalent of DrawerAdjustmentController's
     * webUserController.hasPrivilege("DrawerAdjustmentDirect") check. That
     * check (via SessionController.fillUserPrivileges) is strictly scoped to
     * the logged-in user's current department and only looks at direct
     * WebUserPrivilege grants (no role-based fallback) - this mirrors both
     * of those constraints exactly, using the API key's resolved user's own
     * department as the stand-in for "current department" (there is no
     * session here).
     */
    private boolean hasDrawerAdjustmentDirectPrivilege(WebUser user) {
        if (user.getDepartment() == null) {
            return false;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("u", user);
        m.put("p", Privileges.DrawerAdjustmentDirect);
        m.put("d", user.getDepartment());
        return !webUserPrivilegeFacade.findByJpql(
                "select wp from WebUserPrivilege wp where wp.retired=false and wp.webUser=:u and wp.privilege=:p and wp.department=:d", m).isEmpty();
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null || apiKey.isRetired() || apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new java.util.Date())) {
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

    public static class DrawerAdjustRequest {

        private Long webUserId;
        private String paymentMethod;
        private Double targetBalance;
        private String comment;

        public Long getWebUserId() {
            return webUserId;
        }

        public void setWebUserId(Long webUserId) {
            this.webUserId = webUserId;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public Double getTargetBalance() {
            return targetBalance;
        }

        public void setTargetBalance(Double targetBalance) {
            this.targetBalance = targetBalance;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}
