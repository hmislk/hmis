/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ws.finance.clinical;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.divudi.bean.common.ApiKeyController;
import com.divudi.bean.common.AuthenticateController;
import com.divudi.bean.common.CommonController;

import com.divudi.entity.ApiKey;
import com.divudi.entity.Patient;
import com.divudi.facade.PatientFacade;
import com.divudi.java.CommonFunctions;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.r5.model.Address;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Identifier;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * REST Web Service
 *
 * @author Archmage-Dushan
 */
@Path("fhir")
@RequestScoped
public class Fhir {

    @Context
    private UriInfo context;

    @EJB
    private PatientFacade patientFacade;


    private CommonFunctions commonFunctions;

    @Inject
    private CommonController commonController;
    @Inject
    AuthenticateController authenticateController;
    @Inject
    ApiKeyController apiKeyController;

    /**
     * Creates a new instance of Api
     */
    public Fhir() {
    }

    private JSONObject errorMessage() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 400);
        jSONObjectOut.put("type", "error");
        String e = "Parameter name is not recognized.";
        jSONObjectOut.put("message", "Parameter name is not recognized.");
        return jSONObjectOut;
    }

    private JSONObject errorMessageNoData() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 400);
        jSONObjectOut.put("type", "error");
        String e = "No Data.";
        jSONObjectOut.put("message", e);
        return jSONObjectOut;
    }

    private JSONObject errorMessageNotValidKey() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 401);
        jSONObjectOut.put("type", "error");
        String e = "Not a valid key.";
        jSONObjectOut.put("message", e);
        return jSONObjectOut;
    }

    private JSONObject errorMessageNotValidPathParameter() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 401);
        jSONObjectOut.put("type", "error");
        String e = "Not a valid path parameter.";
        jSONObjectOut.put("message", e);
        return jSONObjectOut;
    }

    private JSONObject successMessage() {
        JSONObject jSONObjectOut = new JSONObject();
        jSONObjectOut.put("code", 200);
        jSONObjectOut.put("type", "success");
        return jSONObjectOut;
    }

    private boolean isValidKey(String key) {
        if (key == null || key.trim().equals("")) {
            return false;
        }
        ApiKey k = apiKeyController.findApiKey(key);
        if (k == null) {
            return false;
        }
        if (k.getWebUser() == null) {
            return false;
        }
        if (k.getWebUser().isRetired()) {
            return false;
        }
        if (!k.getWebUser().isActivated()) {
            return false;
        }
        if (k.getDateOfExpiary().before(new Date())) {
            return false;
        }
        return true;
    }

    @GET
    @Path("/patient/{phn}")
    @Produces("application/json")
    public String getInvoice(@Context HttpServletRequest requestContext,
            @PathParam("phn") String phn) {
        JSONArray array;
        JSONObject jSONObjectOut = new JSONObject();
        String key = requestContext.getHeader("Clinical");
        if (!isValidKey(key)) {
            jSONObjectOut = errorMessageNotValidKey();
            String json = jSONObjectOut.toString();
            return json;
        }

        Patient pt = findPatientByPHN(phn);
        if (pt == null) {
            return errorMessageNoData().toString();
        }

        Long ptId;
        try {
            ptId = Long.valueOf(phn);
        } catch (Exception e) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }
        if (ptId == null || ptId < 1) {
            jSONObjectOut = errorMessageNotValidPathParameter();
            String json = jSONObjectOut.toString();
            return json;
        }

        jSONObjectOut.put("status", successMessage());
        FhirContext ctx = FhirContext.forDstu3();

        org.hl7.fhir.r5.model.Patient patient = new org.hl7.fhir.r5.model.Patient();
        patient.setActive(!pt.isRetired());
        patient.setBirthDate(pt.getPerson().getDob());

        Address add = new Address();
        add.setCountry("Sri Lanka");
        if (pt.getPerson().getArea().getName() != null) {
            add.setCity(pt.getPerson().getArea().getName());
        }
        add.setText(pt.getPerson().getAddress());
        patient.addAddress(add);
        Identifier id = patient.addIdentifier();
        id.setSystem("https://health.gov.lk/phn");
        id.setValue(pt.getPhn());

// Add a name
        HumanName name = patient.addName();
        name.setUse(HumanName.NameUse.OFFICIAL);
        name.setFamily(pt.getPerson().getLastName());
        name.addGiven(pt.getPerson().getFullName());
        name.addPrefix(pt.getPerson().getTitle().getLabel());
        IParser parser = ctx.newXmlParser();
        String json = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
        return json;
    }

    private Patient findPatientByPHN(String phn) {
        String jpql = "select p "
                + " from Patient p "
                + " where p.phn=:phn";
        Map m = new HashMap();
        m.put("phn", m);
        return patientFacade.findFirstByJpql(jpql, m);

    }

}
