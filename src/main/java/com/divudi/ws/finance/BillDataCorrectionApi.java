package com.divudi.ws.finance;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.service.BillDataCorrectionService;
import com.divudi.ws.common.PATCH;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.util.LinkedHashMap;
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

@Path("bill_data_correction")
@RequestScoped
public class BillDataCorrectionApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private BillDataCorrectionService correctionService;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response correctBillData(String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            CorrectionRequest request;
            try {
                request = gson.fromJson(requestBody, CorrectionRequest.class);
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

            Map<String, Object> result = correctionService.correctData(
                    request.getTargetType(),
                    request.getTargetId(),
                    request.getFields(),
                    request.getAuditComment().trim(),
                    request.getApprovedBy().trim(),
                    user
            );

            return successResponse(result);
        } catch (IllegalArgumentException ex) {
            return errorResponse(ex.getMessage(), 400);
        } catch (IllegalStateException ex) {
            return errorResponse(ex.getMessage(), 500);
        } catch (Exception ex) {
            return errorResponse("An error occurred: " + ex.getMessage(), 500);
        }
    }

    @POST
    @Path("/create_bill_item")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMissingBillItem(String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            CreateBillItemRequest request;
            try {
                request = gson.fromJson(requestBody, CreateBillItemRequest.class);
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

            Map<String, Object> result = correctionService.createMissingBillItem(
                    request.getBillId(),
                    request.getItemId(),
                    request.getReferenceBillItemId(),
                    request.getQty(),
                    request.getRate(),
                    request.getNetRate(),
                    request.getGrossValue(),
                    request.getNetValue(),
                    request.getAuditComment().trim(),
                    request.getApprovedBy().trim(),
                    user
            );

            return successResponse(result);
        } catch (IllegalArgumentException ex) {
            return errorResponse(ex.getMessage(), 400);
        } catch (IllegalStateException ex) {
            return errorResponse(ex.getMessage(), 409);
        } catch (Exception ex) {
            return errorResponse("An error occurred: " + ex.getMessage(), 500);
        }
    }

    @POST
    @Path("/create_bill_fee")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMissingBillFee(String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            CreateBillFeeRequest request;
            try {
                request = gson.fromJson(requestBody, CreateBillFeeRequest.class);
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
            if (request.getFeeValue() == null) {
                return errorResponse("feeValue is required", 400);
            }
            if (request.getFeeGrossValue() == null) {
                return errorResponse("feeGrossValue is required", 400);
            }

            Map<String, Object> result = correctionService.createMissingBillFee(
                    request.getBillItemId(),
                    request.getReferenceBillFeeId(),
                    request.getFeeValue(),
                    request.getFeeGrossValue(),
                    request.getAuditComment().trim(),
                    request.getApprovedBy().trim(),
                    user
            );

            return successResponse(result);
        } catch (IllegalArgumentException ex) {
            return errorResponse(ex.getMessage(), 400);
        } catch (IllegalStateException ex) {
            return errorResponse(ex.getMessage(), 409);
        } catch (Exception ex) {
            return errorResponse("An error occurred: " + ex.getMessage(), 500);
        }
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null || apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new java.util.Date())) {
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

    public static class CorrectionRequest {

        private String targetType;
        private Long targetId;
        private Map<String, Object> fields;
        private String auditComment;
        private String approvedBy;

        public String getTargetType() {
            return targetType;
        }

        public void setTargetType(String targetType) {
            this.targetType = targetType;
        }

        public Long getTargetId() {
            return targetId;
        }

        public void setTargetId(Long targetId) {
            this.targetId = targetId;
        }

        public Map<String, Object> getFields() {
            return fields;
        }

        public void setFields(Map<String, Object> fields) {
            this.fields = fields;
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

    public static class CreateBillItemRequest {

        private Long billId;
        private Long itemId;
        private Long referenceBillItemId;
        private double qty;
        private double rate;
        private double netRate;
        private double grossValue;
        private double netValue;
        private String auditComment;
        private String approvedBy;

        public Long getBillId() {
            return billId;
        }

        public void setBillId(Long billId) {
            this.billId = billId;
        }

        public Long getItemId() {
            return itemId;
        }

        public void setItemId(Long itemId) {
            this.itemId = itemId;
        }

        public Long getReferenceBillItemId() {
            return referenceBillItemId;
        }

        public void setReferenceBillItemId(Long referenceBillItemId) {
            this.referenceBillItemId = referenceBillItemId;
        }

        public double getQty() {
            return qty;
        }

        public void setQty(double qty) {
            this.qty = qty;
        }

        public double getRate() {
            return rate;
        }

        public void setRate(double rate) {
            this.rate = rate;
        }

        public double getNetRate() {
            return netRate;
        }

        public void setNetRate(double netRate) {
            this.netRate = netRate;
        }

        public double getGrossValue() {
            return grossValue;
        }

        public void setGrossValue(double grossValue) {
            this.grossValue = grossValue;
        }

        public double getNetValue() {
            return netValue;
        }

        public void setNetValue(double netValue) {
            this.netValue = netValue;
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

    public static class CreateBillFeeRequest {

        private Long billItemId;
        private Long referenceBillFeeId;
        private Double feeValue;
        private Double feeGrossValue;
        private String auditComment;
        private String approvedBy;

        public Long getBillItemId() {
            return billItemId;
        }

        public void setBillItemId(Long billItemId) {
            this.billItemId = billItemId;
        }

        public Long getReferenceBillFeeId() {
            return referenceBillFeeId;
        }

        public void setReferenceBillFeeId(Long referenceBillFeeId) {
            this.referenceBillFeeId = referenceBillFeeId;
        }

        public Double getFeeValue() {
            return feeValue;
        }

        public void setFeeValue(Double feeValue) {
            this.feeValue = feeValue;
        }

        public Double getFeeGrossValue() {
            return feeGrossValue;
        }

        public void setFeeGrossValue(Double feeGrossValue) {
            this.feeGrossValue = feeGrossValue;
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
