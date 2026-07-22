/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.inward;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.AdmissionTypeFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.service.AuditService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API for viewing and resetting the BHT/OPD-card admission-number
 * sequence counter (AdmissionNumber) for a given AdmissionType (and
 * Institution, when institution-based numbering is enabled).
 *
 * Intended for use after staff have manually corrected a printed BHT/OPD-card
 * number, so the system's next auto-generated number can be realigned to
 * continue from the correction (issue #22336).
 *
 * @author Dr M H B Ariyaratne
 */
@Path("admission-numbers")
@RequestScoped
public class AdmissionNumberApi {

    @Context
    private HttpServletRequest requestContext;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private AdmissionTypeFacade admissionTypeFacade;

    @EJB
    private InstitutionFacade institutionFacade;

    @EJB
    private BillNumberGenerator billNumberGenerator;

    @EJB
    private AuditService auditService;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * View the current counter and what the next generated number would be.
     * GET /api/admission-numbers?admissionTypeId=&institutionId=
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCounter(@QueryParam("admissionTypeId") Long admissionTypeId,
            @QueryParam("institutionId") Long institutionId) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            if (admissionTypeId == null) {
                return errorResponse("admissionTypeId is required", 400);
            }
            AdmissionType admissionType = admissionTypeFacade.find(admissionTypeId);
            if (admissionType == null) {
                return errorResponse("Admission type not found: " + admissionTypeId, 404);
            }
            boolean institutionBased = configOptionApplicationController.getBooleanValueByKey(
                    "Generate Separate BHT Number Series for Each Institution");
            Institution institution = null;
            if (institutionId != null) {
                institution = institutionFacade.find(institutionId);
                if (institution == null) {
                    return errorResponse("Institution not found: " + institutionId, 404);
                }
            }
            Long nextNumber = billNumberGenerator.peekNextAdmissionNumber(admissionType, institution, institutionBased);
            Map<String, Object> data = new HashMap<>();
            data.put("admissionTypeId", admissionType.getId());
            data.put("admissionTypeName", admissionType.getName());
            data.put("institutionId", institution != null ? institution.getId() : null);
            data.put("institutionBased", institutionBased);
            data.put("lastAdmissionNumber", nextNumber - 1);
            data.put("nextAdmissionNumber", nextNumber);
            return successResponse(data);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    /**
     * Reset the counter to an explicit corrected value so the next generated
     * number is correction+1.
     * PUT /api/admission-numbers?admissionTypeId=&institutionId=
     * Body: { "lastAdmissionNumber": 1234 }
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetCounter(@QueryParam("admissionTypeId") Long admissionTypeId,
            @QueryParam("institutionId") Long institutionId,
            String requestBody) {
        try {
            String key = requestContext.getHeader("Finance");
            WebUser user = validateApiKey(key);
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }
            if (admissionTypeId == null) {
                return errorResponse("admissionTypeId is required", 400);
            }
            AdmissionType admissionType = admissionTypeFacade.find(admissionTypeId);
            if (admissionType == null) {
                return errorResponse("Admission type not found: " + admissionTypeId, 404);
            }
            JsonObject body;
            try {
                body = gson.fromJson(requestBody, JsonObject.class);
            } catch (Exception e) {
                return errorResponse("Invalid JSON format: " + e.getMessage(), 400);
            }
            if (body == null || !body.has("lastAdmissionNumber")) {
                return errorResponse("lastAdmissionNumber is required in the request body", 400);
            }
            Long newLastAdmissionNumber = body.get("lastAdmissionNumber").getAsLong();

            boolean institutionBased = configOptionApplicationController.getBooleanValueByKey(
                    "Generate Separate BHT Number Series for Each Institution");
            Institution institution = null;
            if (institutionId != null) {
                institution = institutionFacade.find(institutionId);
                if (institution == null) {
                    return errorResponse("Institution not found: " + institutionId, 404);
                }
            }

            Object[] result = billNumberGenerator.resetAdmissionNumber(admissionType, institution, institutionBased, newLastAdmissionNumber);
            Long previous = (Long) result[0];
            Long admissionNumberId = (Long) result[1];

            Map<String, Object> before = new HashMap<>();
            before.put("admissionTypeId", admissionType.getId());
            before.put("institutionId", institution != null ? institution.getId() : null);
            before.put("lastAdmissionNumber", previous);
            Map<String, Object> after = new HashMap<>();
            after.put("admissionTypeId", admissionType.getId());
            after.put("institutionId", institution != null ? institution.getId() : null);
            after.put("lastAdmissionNumber", newLastAdmissionNumber);
            auditService.logAudit(before, after, user, "AdmissionNumber", "Admission Number Counter Reset", admissionNumberId);

            Map<String, Object> data = new HashMap<>();
            data.put("admissionTypeId", admissionType.getId());
            data.put("admissionTypeName", admissionType.getName());
            data.put("institutionId", institution != null ? institution.getId() : null);
            data.put("institutionBased", institutionBased);
            data.put("previousLastAdmissionNumber", previous);
            data.put("lastAdmissionNumber", newLastAdmissionNumber);
            data.put("nextAdmissionNumber", newLastAdmissionNumber + 1);
            return successResponse(data);
        } catch (Exception e) {
            return errorResponse("An error occurred: " + e.getMessage(), 500);
        }
    }

    // Private helper methods

    /**
     * Validate API key and return associated user
     * Follows same pattern as DepartmentApi
     */
    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) {
            return null;
        }

        WebUser user = apiKey.getWebUser();
        if (user == null) {
            return null;
        }

        if (user.isRetired()) {
            return null;
        }

        if (!user.isActivated()) {
            return null;
        }

        // Treat null expiry date as expired
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) {
            return null;
        }

        return user;
    }

    /**
     * Create error response following DepartmentApi pattern
     */
    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(gson.toJson(response)).build();
    }

    /**
     * Create success response following DepartmentApi pattern
     */
    private Response successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return Response.status(200).entity(gson.toJson(response)).build();
    }
}
