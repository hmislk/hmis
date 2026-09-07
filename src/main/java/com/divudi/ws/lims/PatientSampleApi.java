/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.lims;

import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.facade.PatientSampleFacade;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API for PatientSample search and retrieval.
 * Closes #20969
 */
@Path("patient-samples")
@RequestScoped
public class PatientSampleApi {

    @Context
    private HttpServletRequest requestContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private ApiKeyController apiKeyController;

    @EJB
    private PatientSampleFacade patientSampleFacade;

    private static final Logger logger = Logger.getLogger(PatientSampleApi.class.getName());

    private static final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * Search patient samples.
     * GET /api/patient-samples/search?sampleId=&patientName=&fromDate=&toDate=&billNumber=&size=
     * Dates are accepted as yyyy-MM-dd.
     */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search() {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            String sampleIdStr = param("sampleId");
            String patientName = param("patientName");
            String fromDateStr = param("fromDate");
            String toDateStr = param("toDate");
            String billNumber = param("billNumber");
            int size = intParam("size", 200, 1, 1000);

            StringBuilder jpql = new StringBuilder(
                    "select ps from PatientSample ps where ps.retired = false");
            Map<String, Object> params = new HashMap<>();

            if (sampleIdStr != null && !sampleIdStr.trim().isEmpty()) {
                try {
                    Long sampleId = Long.parseLong(sampleIdStr.trim());
                    jpql.append(" and ps.sampleId = :sid");
                    params.put("sid", sampleId);
                } catch (NumberFormatException e) {
                    return errorResponse("Invalid sampleId: must be a number", 400);
                }
            }

            if (patientName != null && !patientName.trim().isEmpty()) {
                jpql.append(" and upper(ps.patient.person.name) like :pname");
                params.put("pname", "%" + patientName.trim().toUpperCase() + "%");
            }

            if (billNumber != null && !billNumber.trim().isEmpty()) {
                jpql.append(" and ps.bill.deptId = :bnum");
                params.put("bnum", billNumber.trim());
            }

            if (fromDateStr != null && !fromDateStr.trim().isEmpty()) {
                Date fromDate = parseDate(fromDateStr.trim());
                if (fromDate == null) {
                    return errorResponse("Invalid fromDate format; expected yyyy-MM-dd", 400);
                }
                jpql.append(" and ps.createdAt >= :fromDate");
                params.put("fromDate", fromDate);
            }

            if (toDateStr != null && !toDateStr.trim().isEmpty()) {
                Date toDate = parseDate(toDateStr.trim());
                if (toDate == null) {
                    return errorResponse("Invalid toDate format; expected yyyy-MM-dd", 400);
                }
                // Include the entire to-date day
                jpql.append(" and ps.createdAt < :toDate");
                params.put("toDate", new Date(toDate.getTime() + 86400000L));
            }

            jpql.append(" order by ps.createdAt desc");

            List<PatientSample> samples = patientSampleFacade.findByJpql(jpql.toString(), params, size);
            List<Map<String, Object>> payload = new ArrayList<>();
            if (samples != null) {
                for (PatientSample ps : samples) {
                    payload.add(toDto(ps, false));
                }
            }
            return successResponse(payload);

        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid argument in search()", e);
            return errorResponse("Invalid request", 400);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in search()", e);
            return errorResponse("Internal server error", 500);
        }
    }

    /**
     * Get a patient sample by ID including linked investigations and automation status.
     * GET /api/patient-samples/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") Long id) {
        try {
            WebUser user = validateApiKey(requestContext.getHeader("Finance"));
            if (user == null) {
                return errorResponse("Not a valid key", 401);
            }

            PatientSample ps = patientSampleFacade.find(id);
            if (ps == null || ps.isRetired()) {
                return errorResponse("PatientSample not found: " + id, 404);
            }
            return successResponse(toDto(ps, true));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in getById()", e);
            return errorResponse("Internal server error", 500);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Map<String, Object> toDto(PatientSample ps, boolean detailed) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", ps.getId());
        row.put("sampleId", ps.getSampleId());
        row.put("status", ps.getStatus() != null ? ps.getStatus().name() : null);
        row.put("createdAt", ps.getCreatedAt());
        row.put("retired", ps.isRetired());
        row.put("cancelled", ps.getCancelled());

        if (ps.getPatient() != null) {
            Map<String, Object> patient = new LinkedHashMap<>();
            patient.put("id", ps.getPatient().getId());
            patient.put("phn", ps.getPatient().getPhn());
            if (ps.getPatient().getPerson() != null) {
                patient.put("name", ps.getPatient().getPerson().getName());
            }
            row.put("patient", patient);
        } else {
            row.put("patient", null);
        }

        if (ps.getBill() != null) {
            Map<String, Object> bill = new LinkedHashMap<>();
            bill.put("id", ps.getBill().getId());
            bill.put("billNumber", ps.getBill().getDeptId());
            row.put("bill", bill);
        } else {
            row.put("bill", null);
        }

        if (ps.getMachine() != null) {
            Map<String, Object> machine = new LinkedHashMap<>();
            machine.put("id", ps.getMachine().getId());
            machine.put("name", ps.getMachine().getName());
            row.put("machine", machine);
        } else {
            row.put("machine", null);
        }

        if (ps.getTest() != null) {
            Map<String, Object> test = new LinkedHashMap<>();
            test.put("id", ps.getTest().getId());
            test.put("name", ps.getTest().getName());
            test.put("code", ps.getTest().getCode());
            row.put("test", test);
        } else {
            row.put("test", null);
        }

        if (ps.getInvestigationComponant() != null) {
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("id", ps.getInvestigationComponant().getId());
            comp.put("name", ps.getInvestigationComponant().getName());
            row.put("investigationComponent", comp);
        } else {
            row.put("investigationComponent", null);
        }

        if (detailed) {
            row.put("sampleCollected", ps.getSampleCollected());
            row.put("sampleCollectedAt", ps.getSampleCollectedAt());
            row.put("sampleReceivedAtLab", ps.getSampleReceivedAtLab());
            row.put("sampleReceivedAtLabAt", ps.getSampleReceivedAtLabAt());
            row.put("sentToAnalyzer", ps.getSentToAnalyzer());
            row.put("sentToAnalyzerAt", ps.getSentToAnalyzerAt());
            row.put("receivedFromAnalyzer", ps.getReceivedFromAnalyzer());
            row.put("receivedFromAnalyzerAt", ps.getReceivedFromAnalyzerAt());
            row.put("sampleRejected", ps.getSampleRejected());
            row.put("sampleRejectionComment", ps.getSampleRejectionComment());
            row.put("readyToSentToAnalyzer", ps.getReadyTosentToAnalyzer());
            row.put("sampleRequestType", ps.getSampleRequestType() != null ? ps.getSampleRequestType().name() : null);
            row.put("outsourced", ps.getOutsourced());
            if (ps.getPatientInvestigation() != null) {
                Map<String, Object> pi = new LinkedHashMap<>();
                pi.put("id", ps.getPatientInvestigation().getId());
                row.put("patientInvestigation", pi);
            } else {
                row.put("patientInvestigation", null);
            }
        }

        return row;
    }

    private Date parseDate(String s) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            return sdf.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    private String param(String name) {
        return uriInfo.getQueryParameters().getFirst(name);
    }

    private int intParam(String name, int defaultValue, int min, int max) {
        String v = param(name);
        if (v == null || v.trim().isEmpty()) return defaultValue;
        try {
            int parsed = Integer.parseInt(v.trim());
            return Math.min(Math.max(parsed, min), max);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private WebUser validateApiKey(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null || apiKey.isRetired()) return null;
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) return null;
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) return null;
        return user;
    }

    private Response errorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("code", code);
        response.put("message", message);
        return Response.status(code).entity(gson.toJson(response)).build();
    }

    private Response successResponse(Object data) {
        return Response.status(200).entity(gson.toJson(successData(data))).build();
    }

    private Map<String, Object> successData(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("code", 200);
        response.put("data", data);
        return response;
    }
}
