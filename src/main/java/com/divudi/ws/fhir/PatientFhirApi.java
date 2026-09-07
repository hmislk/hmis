/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import com.divudi.bean.common.ApiKeyController;
import com.divudi.core.entity.ApiKey;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.WebUser;
import com.divudi.service.fhir.PatientFhirService;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.hl7.fhir.r5.model.Bundle;

/**
 * FHIR R5 Patient REST endpoints.
 *
 * Authentication: {@code FHIR} request header containing a valid API key.
 *
 * Endpoints:
 * <ul>
 *   <li>GET  /api/fhir/Patient/{id}         — get patient by internal DB id</li>
 *   <li>GET  /api/fhir/Patient              — search by name/phone/identifier</li>
 *   <li>POST /api/fhir/Patient              — create new patient</li>
 *   <li>PUT  /api/fhir/Patient/{id}         — partial update</li>
 * </ul>
 */
@Path("fhir/Patient")
@RequestScoped
public class PatientFhirApi {

    private static final String FHIR_JSON = "application/fhir+json";

    @Context
    private HttpServletRequest requestContext;

    @EJB
    private PatientFhirService patientFhirService;

    @Inject
    private ApiKeyController apiKeyController;

    // -------------------------------------------------------------------------
    // GET /api/fhir/Patient/{id}
    // -------------------------------------------------------------------------

    @GET
    @Path("/{id}")
    @Produces(FHIR_JSON)
    public Response getById(@PathParam("id") String idStr) {
        WebUser user = validateApiKey();
        if (user == null) {
            return unauthorizedResponse();
        }

        Long id;
        try {
            id = Long.valueOf(idStr);
        } catch (NumberFormatException e) {
            return errorResponse("Invalid patient id: " + idStr, 400);
        }

        Patient patient = patientFhirService.getPatientById(id);
        if (patient == null) {
            return errorResponse("Patient not found: " + id, 404);
        }

        org.hl7.fhir.r5.model.Patient fhirPt = patientFhirService.toFhirPatient(patient);
        String json = PatientFhirService.FHIR_CTX.newJsonParser().setPrettyPrint(true).encodeResourceToString(fhirPt);
        return Response.ok(json, FHIR_JSON).build();
    }

    // -------------------------------------------------------------------------
    // GET /api/fhir/Patient?name=&phone=&identifier=
    // -------------------------------------------------------------------------

    @GET
    @Produces(FHIR_JSON)
    public Response search(
            @QueryParam("name") String name,
            @QueryParam("phone") String phone,
            @QueryParam("identifier") String identifier) {

        WebUser user = validateApiKey();
        if (user == null) {
            return unauthorizedResponse();
        }

        boolean noParams = (name == null || name.trim().isEmpty())
                && (phone == null || phone.trim().isEmpty())
                && (identifier == null || identifier.trim().isEmpty());

        if (noParams) {
            return errorResponse("At least one search parameter (name, phone, identifier) is required", 400);
        }

        List<Patient> patients;
        try {
            patients = patientFhirService.searchPatients(name, phone, identifier, 50);
        } catch (IllegalArgumentException e) {
            return errorResponse(e.getMessage(), 400);
        }

        Bundle bundle = patientFhirService.toSearchBundle(patients);
        String json = PatientFhirService.FHIR_CTX.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
        return Response.ok(json, FHIR_JSON).build();
    }

    // -------------------------------------------------------------------------
    // POST /api/fhir/Patient
    // -------------------------------------------------------------------------

    @POST
    @Consumes(FHIR_JSON)
    @Produces(FHIR_JSON)
    public Response create(String body) {
        WebUser user = validateApiKey();
        if (user == null) {
            return unauthorizedResponse();
        }

        if (body == null || body.trim().isEmpty()) {
            return errorResponse("Request body is required", 400);
        }

        org.hl7.fhir.r5.model.Patient fhirPt;
        try {
            fhirPt = PatientFhirService.FHIR_CTX.newJsonParser().parseResource(org.hl7.fhir.r5.model.Patient.class, body);
        } catch (DataFormatException e) {
            return errorResponse("Invalid FHIR Patient JSON: " + e.getMessage(), 400);
        }

        Patient created = patientFhirService.createPatient(fhirPt, user);
        org.hl7.fhir.r5.model.Patient responseResource = patientFhirService.toFhirPatient(created);
        String json = PatientFhirService.FHIR_CTX.newJsonParser().setPrettyPrint(true).encodeResourceToString(responseResource);
        return Response.status(Response.Status.CREATED).entity(json).type(FHIR_JSON).build();
    }

    // -------------------------------------------------------------------------
    // PUT /api/fhir/Patient/{id}
    // -------------------------------------------------------------------------

    @PUT
    @Path("/{id}")
    @Consumes(FHIR_JSON)
    @Produces(FHIR_JSON)
    public Response update(@PathParam("id") String idStr, String body) {
        WebUser user = validateApiKey();
        if (user == null) {
            return unauthorizedResponse();
        }

        Long id;
        try {
            id = Long.valueOf(idStr);
        } catch (NumberFormatException e) {
            return errorResponse("Invalid patient id: " + idStr, 400);
        }

        if (body == null || body.trim().isEmpty()) {
            return errorResponse("Request body is required", 400);
        }

        org.hl7.fhir.r5.model.Patient fhirPt;
        try {
            fhirPt = PatientFhirService.FHIR_CTX.newJsonParser().parseResource(org.hl7.fhir.r5.model.Patient.class, body);
        } catch (DataFormatException e) {
            return errorResponse("Invalid FHIR Patient JSON: " + e.getMessage(), 400);
        }

        Patient updated = patientFhirService.updatePatient(id, fhirPt, user);
        if (updated == null) {
            return errorResponse("Patient not found: " + id, 404);
        }

        org.hl7.fhir.r5.model.Patient responseResource = patientFhirService.toFhirPatient(updated);
        String json = PatientFhirService.FHIR_CTX.newJsonParser().setPrettyPrint(true).encodeResourceToString(responseResource);
        return Response.ok(json, FHIR_JSON).build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private WebUser validateApiKey() {
        String key = requestContext.getHeader("FHIR");
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        ApiKey apiKey = apiKeyController.findApiKey(key);
        if (apiKey == null) {
            return null;
        }
        WebUser user = apiKey.getWebUser();
        if (user == null || user.isRetired() || !user.isActivated()) {
            return null;
        }
        if (apiKey.getDateOfExpiary() == null || apiKey.getDateOfExpiary().before(new Date())) {
            return null;
        }
        return user;
    }

    private Response unauthorizedResponse() {
        String body = javax.json.Json.createObjectBuilder()
                .add("status", "error")
                .add("code", 401)
                .add("message", "Invalid or missing FHIR API key")
                .build().toString();
        return Response.status(401).entity(body).type(javax.ws.rs.core.MediaType.APPLICATION_JSON).build();
    }

    private Response errorResponse(String message, int code) {
        String body = javax.json.Json.createObjectBuilder()
                .add("status", "error")
                .add("code", code)
                .add("message", message)
                .build().toString();
        return Response.status(code).entity(body).type(javax.ws.rs.core.MediaType.APPLICATION_JSON).build();
    }
}
