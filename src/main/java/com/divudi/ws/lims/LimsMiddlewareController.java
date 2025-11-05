/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.lims;

import com.divudi.bean.common.SecurityController;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PatientSampleComponantFacade;
import com.divudi.core.facade.PatientSampleFacade;
import com.divudi.core.facade.WebUserFacade;
import java.util.HashMap;
import java.util.Map;
import com.divudi.core.data.LoginRequest;
import com.divudi.core.util.JsfUtil;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONObject;
import ca.uhn.hl7v2.model.*;
import com.divudi.core.util.HL7Utils;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.ws.rs.HeaderParam;
import ca.uhn.hl7v2.*;
import com.divudi.core.data.InvestigationItemType;
import com.divudi.core.data.lab.Priority;
import com.divudi.core.entity.lab.InvestigationItem;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.entity.lab.PatientSampleComponant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import ca.uhn.hl7v2.parser.Parser;
import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.common.DepartmentMachineController;
import com.divudi.bean.lab.MachineController;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.data.lab.SysMex;
import com.divudi.core.data.lab.SysMexTypeA;
import com.divudi.ejb.PatientReportBean;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.lab.DepartmentMachine;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.InvestigationItemValueFlag;
import com.divudi.core.entity.lab.Machine;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.entity.lab.PatientReportItemValue;
import com.divudi.core.facade.InvestigationItemValueFlagFacade;
import com.divudi.core.facade.PatientReportFacade;
import com.divudi.core.facade.PatientReportItemValueFacade;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.ws.rs.GET;

/**
 * REST Web Service
 *
 * @author Dr M H B Ariyaratne
 */
@Path("limsmw")
@RequestScoped
public class LimsMiddlewareController {
// to be taken as correct on 2024 09 04 08 31
    //FOR UNIT TESTING

    @EJB
    PatientSampleComponantFacade patientSampleComponantFacade;
    @EJB
    PatientSampleFacade patientSampleFacade;
    @EJB
    PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    PatientReportFacade prFacade;
    @EJB
    WebUserFacade webUserFacade;
    @EJB
    ItemFacade itemFacade;

    @EJB
    InvestigationItemValueFlagFacade iivfFacade;

    @Inject
    DepartmentMachineController departmentMachineController;
    @Inject
    DepartmentController departmentController;
    @Inject
    MachineController machineController;
    @Inject
    private PatientReportBean prBean;

    private WebUser loggedUser;
    private Department loggedDepartment;
    private Institution loggedInstitution;

    /**
     * Creates a new instance of LIMS
     */
    public LimsMiddlewareController() {
    }

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public Response testEndpoint() {
        return Response.ok("Hello, the path is correct!").build();
    }

    @POST
    @Path("/observation")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response receiveObservation(String observationJson) {
        try {
            // Parse the incoming JSON string to a JSONObject
            JSONObject observation = new JSONObject(observationJson);

            // Process the Observation JSON object
            return processD10Observation(observation);

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to process Observation").build();
        }
    }

    private Response processD10Observation(JSONObject observation) {
        String sampleId = observation.optString("sampleId");
        String analyzerName = observation.optString("analyzerName");
        String analyzerId = observation.optString("analyzerId");
        String departmentId = observation.optString("departmentId");
        String departmentAnalyzerId = observation.optString("departmentAnalyzerId");
        String observationValueCodingSystem = observation.optString("observationValueCodingSystem");
        String observationValueCode = observation.optString("observationValueCode");
        String observationUnitCodingSystem = observation.optString("observationUnitCodingSystem");
        String observationUnitCode = observation.optString("observationUnitCode");
        String observationValueStr = observation.optString("observationValue");
        Double observationValueDbl = null;
        String issuedDate = observation.optString("issuedDate");
        String password = observation.optString("password");
        String username = observation.optString("username");

        // Convert observationValueStr to Double
        try {
            observationValueDbl = Double.valueOf(observationValueStr);
        } catch (NumberFormatException e) {
            // // System.out.println("Invalid observation value: " + observationValueStr);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid observation value: " + observationValueStr)
                    .build();
        }

        Long sampleIdLong;
        Machine analyzer;
        DepartmentMachine departmentAnalyzer = null;
        Department department;
        WebUser wu;
        boolean resultAdded = false;

        // Find the user
        wu = findRequestSendingUser(username, password);
        if (wu == null) {
            // // System.out.println("Cannot find the user: " + username);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("User is not found for ID: " + username)
                    .build();
        }

        // Convert sample ID to Long
        try {
            sampleIdLong = Long.valueOf(sampleId);
        } catch (Exception e) {
            // // System.out.println("Cannot convert sample ID to Long: " + sampleId);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid sample ID: " + sampleId)
                    .build();
        }

        // Find the department
        department = departmentController.findDepartment(departmentId);
        if (department == null) {
            // // System.out.println("Cannot find the department: " + departmentId);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Department not found for ID: " + departmentId)
                    .build();
        }

        // Find the machine (analyzer)
        analyzer = machineController.findMachine(analyzerId);
        if (analyzer == null) {
            // // System.out.println("Cannot find the machine (analyzer): " + analyzerId);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Machine (analyzer) not found for ID: " + analyzerId)
                    .build();
        }

        // Find the department machine
        if (departmentAnalyzerId != null && !departmentAnalyzerId.isEmpty()) {
            departmentAnalyzer = departmentMachineController.findDepartmentMachine(departmentAnalyzerId);
        }
        if (departmentAnalyzer == null) {
            departmentAnalyzer = departmentMachineController.findDepartmentMachine(department, analyzer, true);
        }

        // Find the patient sample
        PatientSample ps = patientSampleFromId(sampleIdLong);
        if (ps == null) {
            // // System.out.println("Cannot find the patient sample: " + sampleId);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Patient sample not found for ID: " + sampleId)
                    .build();
        }

        // Get patient sample components
        List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
        if (pscs == null || pscs.isEmpty()) {
            // // System.out.println("Invalid Sample Components. Please inform developers. Resend results for sample ID: " + sampleIdLong);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Invalid sample components for sample ID: " + sampleIdLong)
                    .build();
        }

        // Get patient investigations
        List<PatientInvestigation> ptixs = getPatientInvestigations(pscs);
        if (ptixs == null || ptixs.isEmpty()) {
            // // System.out.println("Invalid Patient Investigations. Please inform developers. Resend results for sample ID: " + sampleIdLong);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Invalid patient investigations for sample ID: " + sampleIdLong)
                    .build();
        }

        for (PatientInvestigation patientInvestigation : ptixs) {

            Investigation ix = null;

            if (patientInvestigation.getInvestigation() == null) {
                continue;
            }

            ix = patientInvestigation.getInvestigation();

            if (ix.getReportedAs() != null) {
                if (ix.getReportedAs() instanceof Investigation) {
                    ix = (Investigation) ix.getReportedAs();
                }
            }

            // // System.out.println("Patient Investigation = " + patientInvestigation);
            List<PatientReport> patientReports = new ArrayList<>();

            // // System.out.println("ix = " + ix);
            // // System.out.println("pi.getInvestigation().getMachine() = " + patientInvestigation.getInvestigation().getMachine());
            if (ix.getMachine() != null && ix.getMachine().equals(analyzer)) {
                // // System.out.println("Match Machine");
                PatientReport unsavedPatientReport = getUnsavedPatientReport(patientInvestigation);
                // // System.out.println("unsavedPatientReport = " + unsavedPatientReport);
                if (unsavedPatientReport == null) {
                    unsavedPatientReport = createNewPatientReport(patientInvestigation, ix, departmentAnalyzer, wu);
                }
                // // System.out.println("unsavedPatientReport = " + unsavedPatientReport);
                patientReports.add(unsavedPatientReport);
            }
            // // System.out.println("patientReports = " + patientReports);

            if (patientReports.isEmpty()) {
                List<Item> temItems = getItemsForParentItem(ix);
                for (Item ti : temItems) {
                    // // System.out.println("ti = " + ti);
                    if (ti instanceof Investigation) {
                        Investigation tix = (Investigation) ti;
                        if (tix.getMachine() != null && tix.getMachine().equals(analyzer)) {
                            PatientReport tprs = getUnsavedPatientReport(patientInvestigation);
                            if (tprs == null) {
                                tprs = createNewPatientReport(patientInvestigation, tix);
                            }
                            patientReports.add(tprs);
                        }
                    }
                }
            }

            // // System.out.println("prs = " + patientReports);
            for (PatientReport tpr : patientReports) {
                // // System.out.println("tpr = " + tpr);

                for (PatientReportItemValue priv : tpr.getPatientReportItemValues()) {
                    // // System.out.println("priv = " + priv);
                    // // System.out.println("priv.getInvestigationItem().getValueCodeSystem() = " + priv.getInvestigationItem());
                    if (priv.getInvestigationItem() != null
                            && priv.getInvestigationItem().getValueCodeSystem() != null
                            && priv.getInvestigationItem().getValueCodeSystem().equals(observationValueCodingSystem)
                            && priv.getInvestigationItem().getValueCode() != null
                            && priv.getInvestigationItem().getValueCode().equals(observationValueCode)) {
                        priv.setStrValue(observationValueStr);
                        priv.setDoubleValue(observationValueDbl);
                        resultAdded = true;
                    }
                }
                tpr.setAutomated(true);
                tpr.setAutomatedAt(new Date());
                tpr.setAutomatedAnalyzer(analyzer);
                tpr.setAutomatedDepartment(department);
                tpr.setAutomatedInstitution(department.getInstitution());
                tpr.setAutomatedUser(wu);
                prFacade.edit(tpr);
            }
        }

        if (resultAdded) {
            return Response.status(Response.Status.CREATED)
                    .entity("Results Added Successfully for Sample ID : " + sampleIdLong)
                    .build();
        } else {
            return Response.status(Response.Status.EXPECTATION_FAILED)
                    .entity("Results Could NOT be Added for Sample ID : " + sampleIdLong)
                    .build();
        }
    }

    @POST
    @Path("/sysmex")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response receiveSysmexMessage(String message, @HeaderParam("Authorization") String authHeader) {
        // Decode the Authorization header to get the username and password
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            // Extract the encoded username and password
            String base64Credentials = authHeader.substring("Basic".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            // credentials = username:password
            final StringTokenizer tokenizer = new StringTokenizer(credentials, ":");
            String username = tokenizer.nextToken();
            String password = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";

            // Authenticate the user using the extracted username and password
            if (authenticate(username, password)) {
                // Process the message here
                // Assuming 'processMessage' is a method that handles your message
                String response = processSysmexRestMessage(message);
                return Response.ok().entity(response).build();
            }
        }
        return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
    }

    private String processSysmexRestMessage(String message) {
        SysMex sysmex = parseSysMexMessage(message);
        Long sampleId = sysmex.getSampleIdLong();
        PatientSample ps = patientSampleFromId(sampleId);
        String temMsgs = "";
        if (ps == null) {
            return "#{success=false|msg=Wrong Sample ID. Please resent results " + sampleId + "}";
        }
        List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
        if (pscs == null) {
            return "#{success=false|msg=Wrong Sample Components. Please inform developers. Please resent results " + sampleId + "}";
        }
        List<PatientInvestigation> ptixs = getPatientInvestigations(pscs);
        if (ptixs == null || ptixs.isEmpty()) {
            return "#{success=false|msg=Wrong Patient Investigations. Please inform developers. Please resent results " + sampleId + "}";
        }
        for (PatientInvestigation pi : ptixs) {
            List<PatientReport> prs = new ArrayList<>();
            if (pi.getInvestigation().getMachine() != null && pi.getInvestigation().getMachine().getName().toLowerCase().contains("sysmex")) {
                PatientReport tpr = createNewPatientReport(pi, pi.getInvestigation());
                prs.add(tpr);
            }
            List<Item> temItems = getItemsForParentItem(pi.getInvestigation());
            for (Item ti : temItems) {
                if (ti instanceof Investigation) {
                    Investigation tix = (Investigation) ti;
                    if (tix.getMachine() != null && tix.getMachine().getName().toLowerCase().contains("sysmex")) {
                        PatientReport tpr = createNewPatientReport(pi, tix);
                        prs.add(tpr);
                    }
                }
            }
            for (PatientReport tpr : prs) {

                for (PatientReportItemValue priv : tpr.getPatientReportItemValues()) {
                    if (priv.getInvestigationItem() != null && priv.getInvestigationItem().getTest() != null && priv.getInvestigationItem().getIxItemType() == InvestigationItemType.Value) {
                        String test = priv.getInvestigationItem().getTest().getCode().toUpperCase();
                        switch (test) {
                            case "WBC":
                                priv.setStrValue(sysmex.getWbc());
                                priv.setDoubleValue(Double.parseDouble(sysmex.getWbc()));
                                break;
                            case "NEUT%":
                                priv.setStrValue(sysmex.getNeutPercentage() + "");
                                priv.setDoubleValue(Double.parseDouble(sysmex.getNeutPercentage()));
                                break;
                            case "LYMPH%":
                                priv.setStrValue(sysmex.getLymphPercentage() + "");
                                priv.setDoubleValue(Double.parseDouble(sysmex.getLymphPercentage()));
                                break;
                            case "BASO%":
                                priv.setStrValue(sysmex.getBasoPercentage() + "");
                                priv.setDoubleValue(Double.parseDouble(sysmex.getBasoPercentage()));
                                break;
                            case "MONO%":
                                priv.setStrValue(sysmex.getMonoPercentage() + "");
                                priv.setDoubleValue(Double.parseDouble(sysmex.getMonoPercentage()));
                                break;
                            case "EO%":
                                priv.setStrValue(sysmex.getEoPercentage() + "");
                                priv.setDoubleValue(Double.parseDouble(sysmex.getEoPercentage()));
                                break;
                            case "RBC":
                                priv.setStrValue(sysmex.getRbc() + "");
                                priv.setDoubleValue(Double.parseDouble(sysmex.getRbc()));
                                break;
                            case "HGB":
                                priv.setStrValue(sysmex.getHgb() + "");
                                priv.setDoubleValue(Double.parseDouble(sysmex.getHgb()));
                                break;
                            case "HCT":
                                priv.setStrValue(sysmex.getHct() + "");
                                priv.setDoubleValue(Double.parseDouble(sysmex.getHct()));
                                break;
                            case "MCV":
                                priv.setStrValue(sysmex.getMcv() + "");
                                priv.setDoubleValue(Double.parseDouble(sysmex.getMcv()));
                                break;
                            case "MCH":
                                priv.setStrValue(sysmex.getMch() + "");
                                priv.setDoubleValue(Double.parseDouble(sysmex.getMch()));
                                break;
                            case "MCHC":
                                priv.setStrValue(sysmex.getMchc() + "");
                                priv.setDoubleValue(Double.parseDouble(sysmex.getMchc()));
                                break;
                            case "PLT":
                                priv.setStrValue(sysmex.getPlt() + "");
                                priv.setDoubleValue(Double.parseDouble(sysmex.getPlt()));
                                break;
                        }
                    }
                }
                tpr.setDataEntered(true);
                tpr.setDataEntryAt(new Date());
                tpr.setDataEntryComments("Initial Results were taken from Analyzer through Middleware");
                temMsgs += "Patient = " + tpr.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getNameWithTitle() + "\n";
                temMsgs += "Bill No = " + tpr.getPatientInvestigation().getBillItem().getBill().getInsId() + "\n";
                temMsgs += "Investigation = " + tpr.getPatientInvestigation().getInvestigation().getName() + "\n";
                prFacade.edit(tpr);
            }
        }
        return "#{success=true|msg=Data Added to LIMS \n" + temMsgs + "}";

    }

    private boolean authenticate(String username, String password) {
        return isValidCredentials(username, password);
    }

    @Path("/limsProcessAnalyzerMessage")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response limsProcessAnalyzerMessage(String requestBody, @HeaderParam("Authorization") String authorizationHeader) {

        try {
            JSONObject requestJson = new JSONObject(requestBody);

            String encodedCredentials = authorizationHeader.split(" ")[1];
            String decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials.getBytes()));
            String[] credentialsArray = decodedCredentials.split(":");
            String username = credentialsArray[0];
            String password = credentialsArray[1];

            String base64EncodedMessage = requestJson.getString("message");
            byte[] decodedMessageBytes = Base64.getDecoder().decode(base64EncodedMessage);
            String receivedMessage = new String(decodedMessageBytes, StandardCharsets.UTF_8);

            if (!isValidCredentials(username, password)) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            String resultMessage = "";

            String messageType = HL7Utils.findMessageType(receivedMessage);
            // // System.out.println("messageType = " + messageType);

            switch (messageType) {
                case "OUL^R22^OUL_R22":
                case "OUL^R22":
                    resultMessage = sendACK_R22ForoulR22(receivedMessage);
                    break;
                case "QBP^Q11^QBP_Q11":
//                    resultMessage = generateRSP_K11ForQBP_Q11(receivedMessage);
                    String tempUnitId = generateUniqueIDForK11FromQ11(receivedMessage);
                    // // System.out.println("tempUnitId = " + tempUnitId);
                    resultMessage = createK11FromQ11(receivedMessage,
                            tempUnitId,
                            "oHIMS",
                            loggedDepartment.getName(),
                            loggedInstitution.getName(),
                            "sample id");
                    break;
                case "OML^O33^OML_O33":
                case "ORL^O34ORL_O34":
                    break;
                case "ASTM":
                    msgFromSysmex(receivedMessage);
                    break;
                default:
                    System.err.println("messageType = " + messageType);
                    resultMessage = "Can not handle this message type > " + messageType;
            }

            // Encode the resultMessage using Base64
            String base64EncodedResultMessage = Base64.getEncoder().encodeToString(resultMessage.getBytes(StandardCharsets.UTF_8));

            // Return the response message as a JSON
            JSONObject responseJson = new JSONObject();
            responseJson.put("result", base64EncodedResultMessage);
            // // System.out.println("resultMessage = " + resultMessage);
            // // System.out.println("responseJson = " + responseJson);
            return Response.ok(responseJson.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @Deprecated
    private String msgFromSysmex(String msg) {
        return extractDataFromSysMexTypeA(msg);
    }

    public static SysMex parseSysMexMessage(String message) {
//        Correcte Method
        // // System.out.println("parseSysMexMessage");
        SysMex sysMex = new SysMex();
        // Normalize line endings to \n
        message = message.replaceAll("\r\n|\r|\n", "\n");
        // Then split
        String[] lines = message.split("\n(?=\\w\\|)");
        // // System.out.println("lines.length = " + lines.length);
        for (String line : lines) {
            if (line.startsWith("O|")) {
                String[] parts = line.split("\\|");
                if (parts.length > 3) {
                    // The sample ID part is in parts[3], we need to trim it and split by caret
                    String sampleIdPart = parts[3].trim();
                    // // System.out.println("sampleIdPart = " + sampleIdPart);
                    String[] sampleIdParts = sampleIdPart.split("\\^");
                    // // System.out.println("sampleIdParts = " + sampleIdParts);
                    // The actual sample ID is in the third position after splitting by "^"
                    if (sampleIdParts.length > 2) {
                        String sampleIdStr = sampleIdParts[2].trim();
                        // // System.out.println("sampleIdStr = " + sampleIdStr);
                        try {
                            long sampleId = Long.parseLong(sampleIdStr);
                            sysMex.setSampleId(sampleIdStr);
                            sysMex.setSampleIdLong(sampleId);
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            } else if (line.startsWith("R|")) {
                // // System.out.println("line = " + line);
                String[] parts = line.split("\\|");
                // // System.out.println("parts = " + parts);
                // Use a regex to match one or more '^' characters
                String[] codeParts = parts[2].split("\\^+");
                // // System.out.println("codeParts = " + codeParts);
                // The test code should be the second element after splitting, if it exists
                String testCode = codeParts.length > 1 ? codeParts[1].trim() : "";
                // // System.out.println("testCode = " + testCode);
                String value = parts[3].trim();
                Double dblValue = 0.0;
                // // System.out.println("value = " + value);
                try {
                    dblValue = Double.parseDouble(value);
                } catch (Exception e) {
                    dblValue = 0.0;

                }

                switch (testCode) {
                    case "WBC":
                        Double wbc = dblValue * 1000;
                        sysMex.setWbc(wbc + "");
                        break;
                    case "RBC":
                        sysMex.setRbc(value);
                        break;
                    case "HGB":
                        sysMex.setHgb(value);
                        break;
                    case "HCT":
                        sysMex.setHct(value);
                        break;
                    case "MCV":
                        sysMex.setMcv(value);
                        break;
                    case "MCH":
                        sysMex.setMch(value);
                        break;
                    case "MCHC":
                        sysMex.setMchc(value);
                        break;
                    case "PLT":
                        Double plt = dblValue * 1000;
                        sysMex.setPlt(plt + "");
                        break;
                    case "NEUT%":
                        sysMex.setNeutPercentage(value);
                        break;
                    case "LYMPH%":
                        sysMex.setLymphPercentage(value);
                        break;
                    case "MONO%":
                        sysMex.setMonoPercentage(value);
                        break;
                    case "EO%":
                        sysMex.setEoPercentage(value);
                        break;
                    case "BASO%":
                        sysMex.setBasoPercentage(value);
                        break;
                    case "NEUT#":
                        sysMex.setNeutAbsolute(testCode);
                        break;
                    case "LYMPH#":
                        sysMex.setLymphAbsolute(value);
                        break;
                    case "MONO#":
                        sysMex.setMonoAbsolute(value);
                        break;
                    case "EO#":
                        sysMex.setEoAbsolute(value);
                        break;
                    case "BASO#":
                        sysMex.setBasoAbsolute(value);
                        break;
                    case "RDW-SD":
                        sysMex.setRdwSd(value);
                        break;
                    case "RDW-CV":
                        sysMex.setRdwCv(value);
                        break;
                    case "PDW":
                        sysMex.setPdw(value);
                        break;
                    case "MPV":
                        sysMex.setMpv(value);
                        break;
                    case "P-LCR":
                        sysMex.setpLcr(value);
                        break;
                    case "PCT":
                        sysMex.setPct(value);
                        break;
                }
            }
        }

        return sysMex;
    }

    @Deprecated
    public String extractDataFromSysMexTypeA(String msg) {
        SysMexTypeA a = new SysMexTypeA();
        a.setInputString(msg);
        Long sampleId = a.getSampleId();
        // // System.out.println("sampleId = " + sampleId);
        PatientSample ps = patientSampleFromId(sampleId);
        // // System.out.println("ps = " + ps);
        String temMsgs = "";
        if (ps == null) {
            return "#{success=false|msg=Wrong Sample ID. Please resent results " + sampleId + "}";
        }
        List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
        if (pscs == null) {
            return "#{success=false|msg=Wrong Sample Components. Please inform developers. Please resent results " + sampleId + "}";
        }
        List<PatientInvestigation> ptixs = getPatientInvestigations(pscs);
        if (ptixs == null || ptixs.isEmpty()) {
            return "#{success=false|msg=Wrong Patient Investigations. Please inform developers. Please resent results " + sampleId + "}";
        }
        for (PatientInvestigation pi : ptixs) {
            List<PatientReport> prs = new ArrayList<>();
            if (pi.getInvestigation().getMachine() != null && pi.getInvestigation().getMachine().getName().toLowerCase().contains("sysmex")) {
                PatientReport tpr = createNewPatientReport(pi, pi.getInvestigation());
                prs.add(tpr);
            }
            List<Item> temItems = getItemsForParentItem(pi.getInvestigation());
            for (Item ti : temItems) {
                if (ti instanceof Investigation) {
                    Investigation tix = (Investigation) ti;
                    if (tix.getMachine() != null && tix.getMachine().getName().toLowerCase().contains("sysmex")) {
                        PatientReport tpr = createNewPatientReport(pi, tix);
                        prs.add(tpr);
                    }
                }
            }
            for (PatientReport tpr : prs) {

                for (PatientReportItemValue priv : tpr.getPatientReportItemValues()) {
                    if (priv.getInvestigationItem() != null && priv.getInvestigationItem().getTest() != null && priv.getInvestigationItem().getIxItemType() == InvestigationItemType.Value) {
                        String test = priv.getInvestigationItem().getTest().getCode().toUpperCase();
                        switch (test) {
                            case "WBC":
                                priv.setStrValue(a.getWbc() + "");
                                priv.setDoubleValue(a.getWbc());
                                break;
                            case "NEUT%":
                                priv.setStrValue(a.getNeutPercentage() + "");
                                priv.setDoubleValue(a.getNeutPercentage());
                                break;
                            case "LYMPH%":
                                priv.setStrValue(a.getLymphPercentage() + "");
                                priv.setDoubleValue(a.getLymphPercentage());
                                break;
                            case "BASO%":
                                priv.setStrValue(a.getBasoPercentage() + "");
                                priv.setDoubleValue(a.getBasoPercentage());
                                break;
                            case "MONO%":
                                priv.setStrValue(a.getMonoPercentage() + "");
                                priv.setDoubleValue(a.getMonoPercentage());
                                break;
                            case "EO%":
                                priv.setStrValue(a.getEoPercentage() + "");
                                priv.setDoubleValue(a.getEoPercentage());
                                break;
                            case "RBC":
                                priv.setStrValue(a.getRbc() + "");
                                priv.setDoubleValue(a.getRbc());
                                break;
                            case "HGB":
                                priv.setStrValue(a.getHgb() + "");
                                priv.setDoubleValue(a.getHgb());
                                break;
                            case "HCT":
                                priv.setStrValue(a.getHct() + "");
                                priv.setDoubleValue(a.getHct());
                                break;
                            case "MCV":
                                priv.setStrValue(a.getMcv() + "");
                                priv.setDoubleValue(a.getMcv());
                                break;
                            case "MCH":
                                priv.setStrValue(a.getMch() + "");
                                priv.setDoubleValue(a.getMch());
                                break;
                            case "MCHC":
                                priv.setStrValue(a.getMchc() + "");
                                priv.setDoubleValue(a.getMchc());
                                break;
                            case "PLT":
                                priv.setStrValue(a.getPlt() + "");
                                priv.setDoubleValue(a.getPlt());
                                break;
                        }
                    }
                }
                tpr.setDataEntered(true);
                tpr.setDataEntryAt(new Date());
                tpr.setDataEntryComments("Initial Results were taken from Analyzer through Middleware");
                temMsgs += "Patient = " + tpr.getPatientInvestigation().getBillItem().getBill().getPatient().getPerson().getNameWithTitle() + "\n";
                temMsgs += "Bill No = " + tpr.getPatientInvestigation().getBillItem().getBill().getInsId() + "\n";
                temMsgs += "Investigation = " + tpr.getPatientInvestigation().getInvestigation().getName() + "\n";
                prFacade.edit(tpr);
            }
        }
        return "#{success=true|msg=Data Added to LIMS \n" + temMsgs + "}";
    }

    public List<Item> getItemsForParentItem(Item i) {
        String sql;
        Map m = new HashMap();
        m.put("it", i);
        sql = "select c.childItem from ItemForItem c where c.retired=false and c.parentItem=:it order by c.childItem.name ";
        return itemFacade.findByJpql(sql, m);
    }

    public static String extractSampleIDFromK11(String uniqueID) {
        // The sample ID is the part of the unique ID before the colon
        String[] parts = uniqueID.split(":");

        // Return the first part (sample ID)
        return parts[0];
    }

    public static String generateUniqueIDForK11FromQ11(String inputMessage) {
        String[] segments = inputMessage.split("\\|");
        String sampleID = "";

        // Find the sampleID in the QPD segment
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].equals("WOS^Work Order Step^IHE_LABTF")) {
                sampleID = segments[i + 1];
                break;
            }
        }

        // Create a timestamp
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = now.format(formatter);

        // Return the unique ID
        return sampleID + ":" + timestamp;
    }

    public static String createK11FromQ11(String inputMessage, String messageControlID, String sendingApplication,
            String sendingFacility, String receivingApplication, String receivingFacility) {
        // // System.out.println("Create K11 from Q11");

        // Extract Message Control ID from input message
        int startIndex = inputMessage.indexOf("QBP^Q11^QBP_Q11");
        // // System.out.println("startIndex = " + startIndex);
        if (startIndex == -1) {
            // // System.out.println("Couldn't find the string 'QBP^Q11^QBP_Q11' in the input message");
            return null;
        }
        int inputMessageControlIDIndex = inputMessage.indexOf("|", startIndex) + 1;
        // // System.out.println("inputMessageControlIDIndex = " + inputMessageControlIDIndex);
        int endIndex = inputMessage.indexOf("|", inputMessageControlIDIndex);
        // // System.out.println("endIndex = " + endIndex);
        String inputMessageControlID = inputMessage.substring(inputMessageControlIDIndex, endIndex);
        // // System.out.println("inputMessageControlID = " + inputMessageControlID);

        String responseMessageTemplate = "MSH|^~\\&|%s|%s|%s|%s||RSP^K11^RSP_K11|%s|P|2.5.1|||ER|NE||UNICODE UTF-8|||LAB27^IHE"
                + "\nMSA|AA|%s"
                + "\nQAK|%s|OK|WOS^Work Order Step^IHE_LABTF"
                + "\nQPD|WOS^Work Order Step^IHE_LABTF|%s|SPM01";

        return String.format(responseMessageTemplate, sendingApplication, sendingFacility, receivingApplication,
                receivingFacility, messageControlID, inputMessageControlID, messageControlID, messageControlID);
    }

    public String sendACK_R22ForoulR22(String oulR22Message) {
        // // System.out.println("Formulating a ACK_R22 For received oulR22");
        boolean success;
        List<MyTestResult> myResults = getResultsFromOUL_R22Message(oulR22Message);
        // // System.out.println("Number of Results in the Message " + myResults.size());
        success = addResultsFromMyResults(myResults);
        // // System.out.println("success = " + success);
        HapiContext hapiContext = new DefaultHapiContext();
        try {
            Parser parser = hapiContext.getGenericParser();
            Message oulR22 = parser.parse(oulR22Message);
            Message ackR22 = oulR22.generateACK();
            return ackR22.toString();
        } catch (HL7Exception | IOException ex) {
            Logger.getLogger(HL7Utils.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
    }

    public List<String> getSampleIdentifiersFromqBPcQ11cQBP_Q11(String hl7Message) {
        List<String> sampleIdentifiers = new ArrayList<>();
        String[] segments = hl7Message.split("\r|\n");  // split by either \r or \n
//earlier it was like below
// String[] segments = hl7Message.split("\\r");
        for (String segment : segments) {
            if (segment.startsWith("QPD")) {
                String[] fields = segment.split("\\|");
                if (fields.length >= 4) {
                    String sampleIdField = fields[3];
                    // Add the sample ID directly to the list, no need to split
                    sampleIdentifiers.add(sampleIdField);
                }
            }
        }
        return sampleIdentifiers;
    }

    private String generateRSP_K11ForQBP_Q11(String qbpMessage, List<MyPatient> patients) {
        // // System.out.println("generateRSP_K11ForQBP_Q11");
        // // System.out.println("patients = " + patients);
        // // System.out.println("qbpMessage = " + qbpMessage);
        String[] segments = qbpMessage.split("\\|");

        String msh1 = segments[0];
        String msh4 = segments[3];
        String msh5 = segments[4];
        String msh7 = segments[6];
        String msh9 = "RSP^K11^RSP_K11";
        String msh10 = segments[9];
        String msh11 = segments[10];
        String msh12 = segments[11];
        String msh15 = segments[14];
        String msh16 = segments[15];

        String qpd3 = segments[25];
        String qpd4 = segments[26];

        String rspMessage = "MSH|" + msh1 + "|" + msh4 + "|" + msh5 + "|" + msh7 + "|"
                + msh9 + "|" + msh10 + "|" + msh11 + "|" + msh12 + "|" + msh15 + "|" + msh16 + "|\r"
                + "MSA|AA|" + qpd4 + "|\r"
                + "QAK|0|OK|" + qpd3 + "|\r";

        // Add patient, specimen, and test details
        for (MyPatient patient : patients) {
            String patientName = patient.getPatientName();
            String patientId = patient.getPatientId();

            rspMessage += "PID|||" + patientId + "||" + patientName + "|\r";

            for (MySpeciman specimen : patient.getMySpecimans()) {
                String specimenName = specimen.getSpecimanName();

                for (MySampleTests test : specimen.getMySampleTests()) {
                    String sampleId = test.getSampleId();
                    String testCode = test.getTestCode();
                    String testName = test.getTestName();

                    rspMessage += "SPM||" + sampleId + "|" + specimenName + "|||||||||||||||||||||||\r";
                    rspMessage += "ORC|NW|||" + sampleId + "|\r";
                    rspMessage += "OBR|||" + testCode + "^" + testName + "|||\r";
                }
            }
        }

        rspMessage += "ERR|||0|^^^^^^|200^No error^HL70357|\r";
        // // System.out.println("rspMessage = " + rspMessage);
        return rspMessage;
    }

    private String generateRSP_K11ForQBP_Q11(String qbpMessage) {
        // // System.out.println("generateRSP_K11ForQBP_Q11");
        List<MyPatient> mps = generateListOfPatientSpecimanTestDataFromQBPcQ11cQBP_Q11(qbpMessage);
        return generateRSP_K11ForQBP_Q11(qbpMessage, mps);
    }

    private boolean addResultsFromMyResults(List<MyTestResult> mrs) {
        // // System.out.println("Adding Results extracted from Messge to LIMS");
        boolean ok = true;
        for (MyTestResult mr : mrs) {
            boolean thisOk = addResultToReport(mr.getSampleId(), mr.getTestStr(), mr.getResult(), mr.getUnit(), mr.getError());
            // // System.out.println("mr.getSampleId() = " + mr.getSampleId());
            // // System.out.println("thisOk = " + thisOk);
            if (!thisOk) {
                ok = false;
            }
        }
        return ok;
    }

    public boolean addResultToReport(String sampleId, String testCodeFromUploadedDataBundle, String result, String unit, String error) {
        System.out.println("addResultToReport");
        System.out.println("testStr = " + testCodeFromUploadedDataBundle);
        System.out.println("result = " + result);
        boolean temFlag = false;
        Long sid;
        try {
            sid = Long.valueOf(sampleId);
        } catch (NumberFormatException e) {
            sid = 0l;
        }
        // System.out.println("Sample ID = " + sid);
        PatientSample ps = patientSampleFromId(sid);
        // System.out.println("Patient Sample = " + ps);
        if (ps == null) {
            return temFlag;
        }

        List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
        // // System.out.println("Patient Sample Component = " + pscs);
        if (pscs == null) {
            return temFlag;
        }
        List<PatientInvestigation> ptixs = getPatientInvestigations(pscs);
        // // System.out.println("Patient Investigations = " + ptixs);
        if (ptixs == null || ptixs.isEmpty()) {
            return temFlag;
        }
        for (PatientInvestigation pi : ptixs) {
            // // System.out.println("Patient Investigation = " + pi.getInvestigation());

            Investigation ix = pi.getInvestigation();

            if (ix.getReportedAs() != null) {
                if (ix.getReportedAs() instanceof Investigation) {
                    ix = (Investigation) ix.getReportedAs();
                }
            }

            List<PatientReport> prs = new ArrayList<>();
            PatientReport tpr;
            tpr = getUnapprovedPatientReport(pi);
            // // System.out.println("Previous Unapproved Report = " + tpr);
            if (tpr == null) {
                tpr = createNewPatientReport(pi, ix);
                // // System.out.println("new Report Created tpr = " + tpr);
            }
            prs.add(tpr);

            for (PatientReport rtpr : prs) {
                boolean valueToSave = false;
                // // System.out.println("Patient Report = " + rtpr);
                for (PatientReportItemValue priv : rtpr.getPatientReportItemValues()) {
                    System.out.println("Patient Report Item Value = " + priv);
                    System.out.println("priv.getInvestigationItem()  = " + priv.getInvestigationItem());

                    if (priv.getInvestigationItem() != null && priv.getInvestigationItem().getTest() != null
                            && priv.getInvestigationItem().getIxItemType() == InvestigationItemType.ReportImage) {

                        System.out.println("image found");

                        System.out.println("priv.getInvestigationItem().getTest() = " + priv.getInvestigationItem().getTest());
                        String testCodeFromDatabase;
                        testCodeFromDatabase = priv.getInvestigationItem().getResultCode();
                        System.out.println("Test Result Code from LIMS = " + testCodeFromDatabase);
                        if (testCodeFromDatabase == null || testCodeFromDatabase.trim().equals("")) {
                            testCodeFromDatabase = priv.getInvestigationItem().getTest().getCode().toUpperCase();
                            System.out.println("Test Code from Test = " + testCodeFromDatabase);
                        }
                        System.out.println("Test Name from Data Bundle = " + testCodeFromUploadedDataBundle);
                        if (testCodeFromDatabase.equalsIgnoreCase(testCodeFromUploadedDataBundle)) {
                            System.out.println("data bundle and componant are the same");
                            System.out.println("ps.getInvestigationComponant() = " + ps.getInvestigationComponant());
                            System.out.println("priv.getInvestigationItem().getSampleComponent() = " + priv.getInvestigationItem().getSampleComponent());
                            for (PatientSampleComponant patientSampleComponant : pscs) {
                                Item investigationComponentFromPatientSampleComponant = patientSampleComponant.getInvestigationComponant();
                                Item investigationComponentFromPatientReportItem = priv.getInvestigationItem().getSampleComponent();
                                if (investigationComponentFromPatientSampleComponant != null
                                        && investigationComponentFromPatientReportItem != null
                                        && investigationComponentFromPatientSampleComponant.equals(investigationComponentFromPatientReportItem)) {

                                    System.out.println("0 result = " + result);
                                    priv.setLobValue(result);
                                    priv.setBaImage(base64TextToByteArray(result));

                                    String fileType = extractImageTypeFromResult(result);
                                    System.out.println("fileType = " + fileType);
                                    priv.setFileType(fileType);

                                    String fileName = testCodeFromDatabase + sid;
                                    if (!fileName.toLowerCase().matches(".*\\.(bmp|png|jpg|jpeg|jpe)$")) {
                                        fileName += "." + fileType.toLowerCase();
                                    }
                                    priv.setFileName(fileName);

                                    if (priv.getId() == null) {
                                        patientReportItemValueFacade.create(priv);
                                    } else {
                                        patientReportItemValueFacade.edit(priv);
                                    }
                                    temFlag = true;
                                    valueToSave = true;
                                }
                            }

                        }

                    }

                    if (priv.getInvestigationItem() != null && priv.getInvestigationItem().getTest() != null
                            && priv.getInvestigationItem().getIxItemType() == InvestigationItemType.Value) {
                        // // System.out.println("priv.getInvestigationItem().getTest() = " + priv.getInvestigationItem().getTest());
                        String testCodeFromDatabase;
                        testCodeFromDatabase = priv.getInvestigationItem().getResultCode();
                        // // System.out.println("Test Result Code from LIMS = " + testCodeFromDatabase);
                        if (testCodeFromDatabase == null || testCodeFromDatabase.trim().equals("")) {
                            testCodeFromDatabase = priv.getInvestigationItem().getTest().getCode().toUpperCase();
                            // // System.out.println("Test Code from Test = " + testCodeFromDatabase);
                        }
                        // // System.out.println("Test Name from Data Bundle = " + testCodeFromUploadedDataBundle);
                        if (testCodeFromDatabase.equalsIgnoreCase(testCodeFromUploadedDataBundle)) {
                            // // System.out.println("data bundle and componant are the same");
                            // // System.out.println("ps.getInvestigationComponant() = " + ps.getInvestigationComponant());
                            // // System.out.println("priv.getInvestigationItem().getSampleComponent() = " + priv.getInvestigationItem().getSampleComponent());
                            for (PatientSampleComponant patientSampleComponant : pscs) {
                                Item investigationComponentFromPatientSampleComponant = patientSampleComponant.getInvestigationComponant();
                                Item investigationComponentFromPatientReportItem = priv.getInvestigationItem().getSampleComponent();
                                if (investigationComponentFromPatientSampleComponant != null
                                        && investigationComponentFromPatientReportItem != null
                                        && investigationComponentFromPatientSampleComponant.equals(investigationComponentFromPatientReportItem)) {

                                    // // System.out.println("0 result = " + result);
                                    priv.setStrValue(result);

                                    Double dbl = 0d;
                                    try {
                                        dbl = Double.parseDouble(result);
                                        // // System.out.println("0 dbl = " + dbl);
                                    } catch (Exception e) {
                                    }
                                    priv.setDoubleValue(dbl);
                                    // // System.out.println("0 priv.getDoubleValue() = " + priv.getDoubleValue());
                                    if (priv.getId() == null) {
                                        // // System.out.println("0 new priv created = " + dbl);
                                        patientReportItemValueFacade.create(priv);
                                    } else {
                                        // // System.out.println("0 new priv Updates = " + dbl);
                                        patientReportItemValueFacade.edit(priv);
                                    }
                                    temFlag = true;
                                    valueToSave = true;

                                }
                            }

                            if (valueToSave == false) {
                                if (ps.getInvestigationComponant() == null || priv.getInvestigationItem().getSampleComponent() == null) {
                                    // // System.out.println("1 result = " + result);
                                    priv.setStrValue(result);
                                    Double dbl = 0d;
                                    try {
                                        dbl = Double.parseDouble(result);
                                    } catch (Exception e) {
                                    }
                                    priv.setDoubleValue(dbl);
                                    // // System.out.println("1 priv.getDoubleValue() = " + priv.getDoubleValue());
                                    // // System.out.println("priv = " + priv.getId());
                                    // // System.out.println("priv double value " + priv.getDoubleValue());
                                    // // System.out.println("priv Str value = " + priv.getStrValue());
                                    if (priv.getId() == null) {
                                        patientReportItemValueFacade.create(priv);
                                        // // System.out.println("1 new priv created = " + dbl);
                                    } else {
                                        // // System.out.println("1 new priv Updates = " + dbl);
                                        patientReportItemValueFacade.edit(priv);
                                    }
                                    valueToSave = true;
                                    temFlag = true;
                                } else if (priv.getInvestigationItem().getSampleComponent().equals(ps.getInvestigationComponant())) {
                                    // // System.out.println("2 result = " + result);
                                    priv.setStrValue(result);

                                    Double dbl = 0d;
                                    try {
                                        dbl = Double.parseDouble(result);
                                        // // System.out.println("2 dbl = " + dbl);
                                    } catch (Exception e) {
                                    }
                                    priv.setDoubleValue(dbl);
                                    // // System.out.println("2 priv.getDoubleValue() = " + priv.getDoubleValue());
                                    if (priv.getId() == null) {
                                        // // System.out.println("2 new priv created = " + dbl);
                                        patientReportItemValueFacade.create(priv);
                                    } else {
                                        // // System.out.println("2 new priv Updates = " + dbl);
                                        patientReportItemValueFacade.edit(priv);
                                    }
                                    temFlag = true;
                                    valueToSave = true;
                                } else {
                                    // // System.out.println("3 result = " + result);
                                    priv.setStrValue(result);

                                    Double dbl = 0d;
                                    try {
                                        dbl = Double.parseDouble(result);
                                        // // System.out.println("3 dbl = " + dbl);
                                    } catch (Exception e) {
                                    }
                                    priv.setDoubleValue(dbl);
                                    // // System.out.println("3 priv.getDoubleValue() = " + priv.getDoubleValue());
                                    if (priv.getId() == null) {
                                        // // System.out.println("3 new priv created = " + dbl);
                                        patientReportItemValueFacade.create(priv);
                                    } else {
                                        // // System.out.println("3 new priv Updates = " + dbl);
                                        patientReportItemValueFacade.edit(priv);
                                    }
                                    temFlag = true;
                                    valueToSave = true;
                                }
                            }

                        }
                    }

                }
                if (valueToSave) {
                    rtpr.setDataEntered(true);
                    rtpr.setDataEntryAt(new Date());
                    rtpr.setDataEntryComments("Initial Results were taken from Analyzer through Middleware");
                    prFacade.edit(rtpr);
                }
            }

            pi.setStatus(PatientInvestigationStatus.REPORT_CREATED);
            pi.setPerformed(true);
//            pi.setPerformDepartment(sessionController.getDepartment());
//            pi.setPerformInstitution(sessionController.getInstitution());
            pi.setPerformedAt(new Date());
//            pi.setPerformedUser(sessionController.getLoggedUser());
            patientInvestigationFacade.edit(pi);

            if (pi.getBillItem() == null) {
                continue;
            }
            if (pi.getBillItem().getBill() == null) {
                continue;
            }
            Bill b = pi.getBillItem().getBill();
            b.setStatus(PatientInvestigationStatus.REPORT_CREATED);
            billFacade.edit(b);
        }

        return temFlag;
    }

    public byte[] base64TextToByteArray(String base64Text) {
        if (base64Text == null || base64Text.isEmpty()) {
            return new byte[0];
        }
        // Remove the prefix regardless of image type
        String cleanedBase64 = base64Text.replaceFirst("^\\^Image\\^[A-Z]+\\^Base64\\^", "");
        return java.util.Base64.getDecoder().decode(cleanedBase64);
    }

    public boolean addResultToReportPrevious(String sampleId, String testStr, String result, String unit, String error) {
        // // System.out.println("Adding Individual Result To Report");
        // // System.out.println("testStr = " + testStr);
        // // System.out.println("result = " + result);
        boolean temFlag = false;
        Long sid;
        try {
            sid = Long.valueOf(sampleId);
        } catch (NumberFormatException e) {
            sid = 0l;
        }
        // // System.out.println("Sample ID = " + sid);
        PatientSample ps = patientSampleFromId(sid);
        // // System.out.println("Patient Sample = " + ps);
        if (ps == null) {
            return temFlag;
        }

        List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
        // // System.out.println("Patient Sample Component = " + pscs);
        if (pscs == null) {
            return temFlag;
        }
        List<PatientInvestigation> ptixs = getPatientInvestigations(pscs);
        // // System.out.println("Patient Investigations = " + ptixs);
        if (ptixs == null || ptixs.isEmpty()) {
            return temFlag;
        }
        for (PatientInvestigation pi : ptixs) {
            // // System.out.println("Patient Investigation = " + pi);

            Investigation ix = pi.getInvestigation();

            if (ix.getReportedAs() != null) {
                if (ix.getReportedAs() instanceof Investigation) {
                    ix = (Investigation) ix.getReportedAs();
                }
            }

            List<PatientReport> prs = new ArrayList<>();
            PatientReport tpr;
            tpr = getUnapprovedPatientReport(pi);
            // // System.out.println("Previous Unapproved Report = " + tpr);
            if (tpr == null) {
                tpr = createNewPatientReport(pi, ix);
            }
            prs.add(tpr);

            for (PatientReport rtpr : prs) {
                boolean valueToSave = false;
                // // System.out.println("Patient Report = " + rtpr);
                for (PatientReportItemValue priv : rtpr.getPatientReportItemValues()) {
                    // // System.out.println("Patient Report Item Value = " + priv);
                    if (priv.getInvestigationItem() != null && priv.getInvestigationItem().getTest() != null
                            && priv.getInvestigationItem().getIxItemType() == InvestigationItemType.Value) {
                        String test;
                        test = priv.getInvestigationItem().getResultCode();
                        // // System.out.println("Test Result Code from LIMS = " + test);
                        if (test == null || test.trim().equals("")) {
                            test = priv.getInvestigationItem().getTest().getCode().toUpperCase();
                            // // System.out.println("Test Code from Test = " + test);
                        }
                        // // System.out.println("Test Name from LIMS = " + test);
                        // // System.out.println("Test Name from HL7 Msg = " + testStr);
                        if (test.equalsIgnoreCase(testStr)) {
                            // // System.out.println("ps.getInvestigationComponant() = " + ps.getInvestigationComponant());
                            // // System.out.println("priv.getInvestigationItem().getSampleComponent() = " + priv.getInvestigationItem().getSampleComponent());
                            if (ps.getInvestigationComponant() == null || priv.getInvestigationItem().getSampleComponent() == null) {
                                // // System.out.println("1 result = " + result);
                                priv.setStrValue(result);
                                Double dbl = 0d;
                                try {
                                    dbl = Double.parseDouble(result);
                                } catch (Exception e) {
                                }
                                priv.setDoubleValue(dbl);
                                // // System.out.println("1 priv.getDoubleValue() = " + priv.getDoubleValue());
                                // // System.out.println("priv = " + priv.getId());
                                // // System.out.println("priv double value " + priv.getDoubleValue());
                                // // System.out.println("priv Str value = " + priv.getStrValue());
                                if (priv.getId() == null) {
                                    patientReportItemValueFacade.create(priv);
                                    // // System.out.println("1 new priv created = " + dbl);
                                } else {
                                    // // System.out.println("1 new priv Updates = " + dbl);
                                    patientReportItemValueFacade.edit(priv);
                                }
                                valueToSave = true;
                                temFlag = true;
                            } else if (priv.getInvestigationItem().getSampleComponent().equals(ps.getInvestigationComponant())) {
                                // // System.out.println("2 result = " + result);
                                priv.setStrValue(result);

                                Double dbl = 0d;
                                try {
                                    dbl = Double.parseDouble(result);
                                    // // System.out.println("2 dbl = " + dbl);
                                } catch (Exception e) {
                                }
                                priv.setDoubleValue(dbl);
                                // // System.out.println("2 priv.getDoubleValue() = " + priv.getDoubleValue());
                                if (priv.getId() == null) {
                                    // // System.out.println("2 new priv created = " + dbl);
                                    patientReportItemValueFacade.create(priv);
                                } else {
                                    // // System.out.println("2 new priv Updates = " + dbl);
                                    patientReportItemValueFacade.edit(priv);
                                }
                                temFlag = true;
                                valueToSave = true;
                            } else {
                                // // System.out.println("Else");
                            }

                        }
                    }

                }
                if (valueToSave) {
                    rtpr.setDataEntered(true);
                    rtpr.setDataEntryAt(new Date());
                    rtpr.setDataEntryComments("Initial Results were taken from Analyzer through Middleware");
                    prFacade.edit(rtpr);
                }
            }
        }

        return temFlag;
    }

    @EJB
    PatientReportItemValueFacade patientReportItemValueFacade;

    public List<MyTestResult> getResultsFromOUL_R22Message(String message) {
        System.err.println("getResultsFromOUL_R22Message");
//        // // System.out.println("message = " + message);
//        // // System.out.println("message Length = " + message.length());
//        // // System.out.println("message Type = " + HL7Utils.findMessageType(message));

        List<MyTestResult> results = new ArrayList<>();
        String[] segments = message.split("\\r");
//        // // System.out.println("segments Length = " + segments.length);

        String sampleId = null;

        for (int i = 0; i < segments.length; i++) {
            if (segments[i].startsWith("SPM")) {
                String[] fields = segments[i].split("\\|");
                String temSampleId = fields[2];
                sampleId = extractNumber(temSampleId);
                // // System.out.println("Sample ID from HL7 Message = " + sampleId);
            } else if (segments[i].startsWith("OBX")) {
                String[] fields = segments[i].split("\\|");

                if (fields.length > 14) { //Ensure there are enough fields before trying to access them
                    String[] testDetails = fields[3].split("\\^");
                    String testCode = testDetails[0];
                    // // System.out.println("Test Code from HL7 Message = " + testCode);
                    String result = fields[5];
                    // // System.out.println("Results extracted from HL7 Message = " + result);
                    String unit = fields[6];
                    // // System.out.println("Unit extracted from HL7 Message= " + unit);
                    String error = null;
                    if (fields.length > 15) {
                        error = fields[15];
                        // // System.out.println("Error in Extracing the message. Field Length is more than 15." + error);
                    }

                    MyTestResult testResult = new MyTestResult(sampleId, testCode, result, unit, error);
                    results.add(testResult);
                }
            }
        }
        return results;
    }

    public static String extractNumber(String str) {
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(str);
        StringBuilder number = new StringBuilder();
        while (matcher.find()) {
            number.append(matcher.group());
        }
        return number.toString();
    }

    private String extractImageTypeFromResult(String result) {
        System.out.println("Received result = " + result);
        if (result == null || result.isEmpty()) {
            System.out.println("Result is null or empty. Defaulting to BMP.");
            return "BMP";
        }
        Pattern pattern = Pattern.compile("^\\^Image\\^([A-Z]+)\\^Base64\\^");
        Matcher matcher = pattern.matcher(result);
        if (matcher.find()) {
            String type = matcher.group(1);
            System.out.println("Extracted image type = " + type);
            return type;
        } else {
            System.out.println("No image type pattern matched. Defaulting to BMP.");
        }
        return "BMP";
    }

    public class MyTestResult {

        private String sampleId;
        private String testStr;
        private String result;
        private String unit;
        private String error;

        public MyTestResult(String sampleId, String testStr, String result, String unit, String error) {
            this.sampleId = sampleId;
            this.testStr = testStr;
            this.result = result;
            this.unit = unit;
            this.error = error;
        }

        // getters and setters for each attribute
        public String getSampleId() {
            return sampleId;
        }

        public void setSampleId(String sampleId) {
            this.sampleId = sampleId;
        }

        public String getTestStr() {
            return testStr;
        }

        public void setTestStr(String testStr) {
            this.testStr = testStr;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    public PatientReport createNewPatientReport(PatientInvestigation pi, Investigation ix) {
        //System.err.println("creating a new patient report");
        PatientReport r = null;
        if (pi != null && pi.getId() != null && ix != null) {
            r = new PatientReport();
            r.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            r.setCreater(loggedUser);
            r.setItem(ix);
            r.setDataEntryDepartment(loggedDepartment);
            r.setDataEntryInstitution(loggedInstitution);
            if (r.getTransInvestigation() != null) {
                r.setReportFormat(r.getTransInvestigation().getReportFormat());
            }
            prFacade.create(r);
            r.setPatientInvestigation(pi);
            prBean.addPatientReportItemValuesForReport(r);
            prFacade.edit(r);
        } else {
            JsfUtil.addErrorMessage("No ptIx or Ix selected to add");
        }
        return r;
    }

    public PatientReport createNewPatientReport(PatientInvestigation pi, Investigation ix, DepartmentMachine deptAnalyzer, WebUser u) {
//        // // System.out.println("createNewPatientReport = ");
        PatientReport r = null;
        if (pi != null && pi.getId() != null && ix != null) {
            r = new PatientReport();
            r.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            r.setCreater(loggedUser);
            r.setItem(ix);
            r.setDataEntryDepartment(loggedDepartment);
            r.setDataEntryInstitution(loggedInstitution);
            r.setAutomated(Boolean.TRUE);
            r.setAutomatedAnalyzer(deptAnalyzer.getMachine());
            r.setAutomatedAt(new Date());
            r.setAutomatedDepartment(deptAnalyzer.getDepartment());
            r.setAutomatedInstitution(deptAnalyzer.getDepartment().getInstitution());
            r.setAutomatedUser(u);
            if (r.getTransInvestigation() != null) {
                r.setReportFormat(r.getTransInvestigation().getReportFormat());
            }
            prFacade.create(r);
            r.setPatientInvestigation(pi);
            prBean.addPatientReportItemValuesForReport(r);
            prFacade.edit(r);
        } else {
            JsfUtil.addErrorMessage("No ptIx or Ix selected to add");
        }
        return r;
    }

    public Double getDefaultDoubleValue(InvestigationItem item, Patient patient) {
        //TODO: Create Logic
        return 0.0;
    }

    public String getDefaultVarcharValue(InvestigationItem item, Patient patient) {
        //TODO: Create Logic
        return "";
    }

    public String getDefaultMemoValue(InvestigationItem item, Patient patient) {
        //TODO: Create Logic
        return "";
    }

    public byte[] getDefaultImageValue(InvestigationItem item, Patient patient) {
        //TODO: Create Logic
        return null;
    }

//    public void addPatientReportItemValuesForReport(PatientReport ptReport) {
//        String sql = "";
//        Investigation temIx = (Investigation) ptReport.getItem();
//        for (ReportItem ii : temIx.getReportItems()) {
//            PatientReportItemValue val = null;
//            if ((ii.getIxItemType() == InvestigationItemType.Value || ii.getIxItemType() == InvestigationItemType.Calculation || ii.getIxItemType() == InvestigationItemType.Flag || ii.getIxItemType() == InvestigationItemType.Template) && ii.isRetired() == false) {
//                if (ptReport.getId() == null || ptReport.getId() == 0) {
//
//                    val = new PatientReportItemValue();
//                    if (ii.getIxItemValueType() == InvestigationItemValueType.Varchar) {
//                        val.setStrValue(getDefaultVarcharValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
//                    } else if (ii.getIxItemValueType() == InvestigationItemValueType.Memo) {
//                        val.setLobValue(getDefaultMemoValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
//                    } else if (ii.getIxItemValueType() == InvestigationItemValueType.Double) {
//                        val.setDoubleValue(getDefaultDoubleValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
//                    } else if (ii.getIxItemValueType() == InvestigationItemValueType.Image) {
//                        val.setBaImage(getDefaultImageValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
//                    } else {
//                    }
//                    val.setInvestigationItem((InvestigationItem) ii);
//                    val.setPatient(ptReport.getPatientInvestigation().getPatient());
//                    val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
//                    val.setPatientReport(ptReport);
//                    // ptReport.getPatientReportItemValues().add(val);
//                    ////// // // // System.out.println("New value added to pr teport" + ptReport);
//
//                } else {
//                    sql = "select i from PatientReportItemValue i where i.patientReport=:ptRp"
//                            + " and i.investigationItem=:inv ";
//                    HashMap hm = new HashMap();
//                    hm.put("ptRp", ptReport);
//                    hm.put("inv", ii);
//                    val = ptRivFacade.findFirstByJpql(sql, hm);
//                    if (val == null) {
//                        ////// // // // System.out.println("val is null");
//                        val = new PatientReportItemValue();
//                        if (ii.getIxItemValueType() == InvestigationItemValueType.Varchar) {
//                            val.setStrValue(getDefaultVarcharValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
//                        } else if (ii.getIxItemValueType() == InvestigationItemValueType.Memo) {
//                            val.setLobValue(getDefaultMemoValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
//                        } else if (ii.getIxItemValueType() == InvestigationItemValueType.Double) {
//                            val.setDoubleValue(getDefaultDoubleValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
//                        } else if (ii.getIxItemValueType() == InvestigationItemValueType.Image) {
//                            val.setBaImage(getDefaultImageValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
//                        } else {
//                        }
//                        val.setInvestigationItem((InvestigationItem) ii);
//                        val.setPatient(ptReport.getPatientInvestigation().getPatient());
//                        val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
//                        val.setPatientReport(ptReport);
//                        //ptReport.getPatientReportItemValues().add(val);
//                        ////// // // // System.out.println("value added to pr teport" + ptReport);
//
//                    }
//
//                }
//            } else if (ii.getIxItemType() == InvestigationItemType.DynamicLabel && ii.isRetired() == false) {
//                if (ptReport.getId() == null || ptReport.getId() == 0) {
//
//                    val = new PatientReportItemValue();
//                    val.setStrValue(getPatientDynamicLabel((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
//                    val.setInvestigationItem((InvestigationItem) ii);
//                    val.setPatient(ptReport.getPatientInvestigation().getPatient());
//                    val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
//                    val.setPatientReport(ptReport);
//                    // ptReport.getPatientReportItemValues().add(val);
//                    ////// // // // System.out.println("New value added to pr teport" + ptReport);
//
//                } else {
//                    sql = "select i from PatientReportItemValue i where i.patientReport.id = " + ptReport.getId() + " and i.investigationItem.id = " + ii.getId() + " and i.investigationItem.ixItemType = com.divudi.core.data.InvestigationItemType.Value";
//                    val = ptRivFacade.findFirstByJpql(sql);
//                    if (val == null) {
//                        val = new PatientReportItemValue();
//                        val.setStrValue(getPatientDynamicLabel((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
//                        val.setInvestigationItem((InvestigationItem) ii);
//                        val.setPatient(ptReport.getPatientInvestigation().getPatient());
//                        val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
//                        val.setPatientReport(ptReport);
//                        // ptReport.getPatientReportItemValues().add(val);
//                        ////// // // // System.out.println("value added to pr teport" + ptReport);
//
//                    }
//
//                }
//            }
//
//            if (val != null) {
//
//                ptRivFacade.create(val);
//
//                ptReport.getPatientReportItemValues().add(val);
//            }
//        }
//    }
    public String getPatientDynamicLabel(InvestigationItem ii, Patient p) {
        String dl;
        String sql;
        dl = ii.getName();

        long ageInDays = com.divudi.core.util.CommonFunctions.calculateAgeInDays(p.getPerson().getDob(), Calendar.getInstance().getTime());
        sql = "select f from InvestigationItemValueFlag f where  f.fromAge < " + ageInDays + " and f.toAge > " + ageInDays + " and f.investigationItemOfLabelType.id = " + ii.getId();
        List<InvestigationItemValueFlag> fs = iivfFacade.findByJpql(sql);
        for (InvestigationItemValueFlag f : fs) {
            if (f.getSex() == p.getPerson().getSex()) {
                dl = f.getFlagMessage();
            }
        }
        return dl;
    }

    public PatientReport getUnapprovedPatientReport(PatientInvestigation pi) {
        String j = "select r from PatientReport r "
                + " where r.patientInvestigation = :pi "
                + " and r.retired != true "
                + " and (r.approved = :a or r.approved is null) "
                + " order by r.id desc";

        Map m = new HashMap();
        m.put("pi", pi);
        m.put("a", false);
        PatientReport r = prFacade.findFirstByJpql(j, m);
        return r;
    }

    public PatientReport getUnsavedPatientReport(PatientInvestigation pi) {
        //Retired Reports are not excluded
        String j = "select r from PatientReport r "
                + " where r.patientInvestigation = :pi"
                + " and r.retired=false "
                + " and (r.dataEntered = :a or r.dataEntered is null) "
                + " order by r.id desc";
        Map m = new HashMap();
        m.put("pi", pi);
        m.put("a", false);
        PatientReport r = prFacade.findFirstByJpql(j, m);
        return r;
    }

    private List<PatientInvestigation> getPatientInvestigations(List<PatientSampleComponant> pscs) {
        Set<PatientInvestigation> ptixhs = new HashSet<>();
        for (PatientSampleComponant psc : pscs) {
            ptixhs.add(psc.getPatientInvestigation());
        }
        List<PatientInvestigation> ptixs = new ArrayList<>(ptixhs);
        return ptixs;
    }

    private List<MyPatient> generateListOfPatientSpecimanTestDataFromQBPcQ11cQBP_Q11(String qBPcQ11cQBP_Q11) {
        List<String> samplesIdentifiers = getSampleIdentifiersFromqBPcQ11cQBP_Q11(qBPcQ11cQBP_Q11);
        List<MyPatient> mps = new ArrayList<>();
        for (String sid : samplesIdentifiers) {

            PatientSample ps = patientSampleFromId(sid);
            if (ps == null) {
                continue;
            }

            if (ps.getPatient() == null || ps.getPatient().getPerson() == null) {
                // // System.out.println("No patient");
                continue;
            }

            List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
            if (pscs == null || pscs.isEmpty()) {
                // // System.out.println("PSCS NULL OR EMPTY");
                continue;
            }

            MyPatient p = new MyPatient();
            MySpeciman s = new MySpeciman();
            MySampleTests t;

            p.setPatientId(ps.getPatient().getId() + "");
            p.setPatientName(ps.getPatient().getPerson().getNameWithTitle());

            for (PatientSampleComponant c : pscs) {
                for (InvestigationItem tii : c.getPatientInvestigation().getInvestigation().getReportItems()) {
                    if (tii.getIxItemType() == InvestigationItemType.Value) {
                        String sampleTypeName;
                        String samplePriority;
                        if (tii.getSample() != null) {
                            sampleTypeName = tii.getSample().getName();
                        } else {
                            sampleTypeName = "serum";
                        }
                        if (tii.getItem().getPriority() != null) {
                            samplePriority = tii.getItem().getPriority().toString();
                        } else {
                            samplePriority = (Priority.Routeine).toString();
                        }
                        MySpeciman ms = new MySpeciman();
                        ms.setSpecimanName(sampleTypeName);
                        if (tii.getItem().isHasMoreThanOneComponant()) {
                            if (tii.getTest() != null && !tii.getTest().getName().trim().equals("")) {
                                if (tii.getSampleComponent().equals(ps.getInvestigationComponant())) {
                                    t = new MySampleTests();
                                    t.setTestCode(tii.getTest().getCode());
                                    t.setTestName(tii.getTest().getName());
                                    t.setSampleId(sid);
                                    ms.getMySampleTests().add(t);
                                }
                            }
                        } else {
                            if (tii.getTest() != null && !tii.getTest().getName().trim().equals("")) {
                                t = new MySampleTests();
                                t.setTestCode(tii.getTest().getCode());
                                t.setTestName(tii.getTest().getName());
                                t.setSampleId(sid);
                                ms.getMySampleTests().add(t);
                            }
                        }
                        p.getMySpecimans().add(s);
                    }
                }
            }
            mps.add(p);
        }
        // // System.out.println("mps = " + mps.size());
        return mps;
    }

    public PatientSample patientSampleFromId(String id) {
        Long pid = 0l;
        try {
            pid = Long.valueOf(id);
        } catch (NumberFormatException e) {
            // // System.out.println("e = " + e);
        }
        return patientSampleFacade.find(pid);
    }

    public PatientSample patientSampleFromId(Long id) {
        PatientSample ps = patientSampleFacade.find(id);
        if (ps != null) {
            return ps;
        }
        String j = "Select ps "
                + " from PatientSample ps "
                + " where ps.sampleId=:sid ";
        Map m = new HashMap<>();
        m.put("sid", id);
        return patientSampleFacade.findFirstByJpql(j, m);
    }

    public List<PatientSampleComponant> getPatientSampleComponents(PatientSample ps) {
        String j = "select psc from PatientSampleComponant psc where psc.patientSample = :ps";
        Map m = new HashMap();
        m.put("ps", ps);
        return patientSampleComponantFacade.findByJpql(j, m);
    }

    private boolean isValidCredentials(String username, String password) {
        WebUser requestSendingUser = findRequestSendingUser(username, password);

        if (requestSendingUser != null) {
            loggedUser = requestSendingUser;
            loggedDepartment = loggedUser.getDepartment();
            loggedInstitution = loggedUser.getInstitution();
            return true;
        } else {
            loggedUser = null;
            loggedDepartment = null;
            loggedInstitution = null;
            return false;
        }

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginRequest request) {
        // // System.out.println("login");
        String username = request.getUsername();
        String password = request.getPassword();

        // Validate the username and password, such as checking them against a database or LDAP directory
        WebUser requestSendingUser = findRequestSendingUser(username, password);

        if (requestSendingUser != null) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    public WebUser findRequestSendingUser(String temUserName, String temPassword) {
        if (temUserName == null) {
            return null;
        }
        if (temPassword == null) {
            return null;
        }
        String temSQL;

        temSQL = "SELECT u "
                + " FROM WebUser u "
                + " WHERE u.retired=:ret"
                + " and u.name=:n";
        Map m = new HashMap();

        m.put("n", temUserName.trim().toLowerCase());
        m.put("ret", false);
        WebUser u = webUserFacade.findFirstByJpql(temSQL, m);

        if (u == null) {
            return null;
        }

        if (SecurityController.matchPassword(temPassword, u.getWebUserPassword())) {
            return u;
        }
        return null;
    }

    public WebUser getLoggedUser() {
        return loggedUser;
    }

    public void setLoggedUser(WebUser loggedUser) {
        this.loggedUser = loggedUser;
    }

    public Department getLoggedDepartment() {
        return loggedDepartment;
    }

    public void setLoggedDepartment(Department loggedDepartment) {
        this.loggedDepartment = loggedDepartment;
    }

    public Institution getLoggedInstitution() {
        return loggedInstitution;
    }

    public void setLoggedInstitution(Institution loggedInstitution) {
        this.loggedInstitution = loggedInstitution;
    }

    public List<String> generateTestCodesForAnalyzer(String sampleId, String sendingAnalyzerName) {
        System.out.println("sendingAnalyzerName = " + sendingAnalyzerName);
        System.out.println("generateTestCodesForAnalyzer");
        PatientSample ps = patientSampleFromId(sampleId);
        System.out.println("ps = " + ps);
        if (ps == null) {
            System.out.println("No PS");
            return null;
        }

        List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
        System.out.println("pscs = " + pscs);
        if (pscs == null || pscs.isEmpty()) {
            System.out.println("PSCS NULL OR EMPTY");
            return null;
        }

        List<String> tests = new ArrayList<>();

        for (PatientSampleComponant c : pscs) {
            // // System.out.println("c = " + c);
            Investigation myIx = c.getPatientInvestigation().getInvestigation();

            if (myIx == null) {
                return null;
            }

            if (myIx.getReportedAs() != null) {
                if (myIx.getReportedAs() instanceof Investigation) {
                    myIx = (Investigation) myIx.getReportedAs();
                }
            }

            for (InvestigationItem tii : myIx.getReportItems()) {
                System.out.println("tii = " + tii.getName());
                if (tii.getIxItemType() == InvestigationItemType.Value) {
                    System.out.println("value");
                    String sampleTypeName;
                    String samplePriority;

                    if (tii.getItem() == null) {
                        System.out.println("tii is NULL " + tii);
                        continue;
                    }

                    if (tii.getSample() != null) {
                        sampleTypeName = tii.getSample().getName();
                    } else {
                        sampleTypeName = "serum";
                    }
                    if (tii.getItem().getPriority() != null) {
                        samplePriority = tii.getItem().getPriority().toString();
                    } else {
                        samplePriority = (Priority.Routeine).toString();
                    }
                    MySpeciman ms = new MySpeciman();
                    ms.setSpecimanName(sampleTypeName);
                    if (tii.getItem().isHasMoreThanOneComponant()) {
                        System.out.println("more than one componant");
                        System.out.println("tii = " + tii.getName());
                        System.out.println("tii.getTest() = " + tii.getTest());
                        if (tii.getTest() != null && !tii.getTest().getName().trim().equals("")) {
                            System.out.println("tii.getSampleComponent() = " + tii.getSampleComponent());
                            System.out.println("c.getInvestigationComponant() = " + c.getInvestigationComponant());
                            if (tii.getSampleComponent().equals(c.getInvestigationComponant())) {
                                System.out.println("going to check analyzer equal");
                                if (tii.getMachine().getName().equalsIgnoreCase(sendingAnalyzerName)) {
                                    System.out.println("tii.getTest().getCode() = " + tii.getTest().getCode());
                                    System.out.println("c.getInvestigationComponant().getName() = " + c.getInvestigationComponant().getName());
                                    tests.add(tii.getTest().getCode());
                                    updateStatusToPeformed(c);
                                    System.out.println("1. tii.getTest().getCode() = " + tii.getTest().getCode());
                                }
//                                tests.add(tii.getTest().getName());

                            }
                        }
                    } else {
                        System.out.println("one componant");
                        System.out.println("tti = " + tii.getName());
                        System.out.println("tii.getTest() = " + tii.getTest());
                        if (tii.getTest() != null && !tii.getTest().getName().trim().equals("")) {
                            System.out.println("going to check analyzer equal");
                            if (tii.getMachine().getName().equalsIgnoreCase(sendingAnalyzerName)) {
                                tests.add(tii.getTest().getCode());
                                updateStatusToPeformed(c);
                                System.out.println("1. tii.getTest().getCode() = " + tii.getTest().getCode());
                            }

                        }
                    }
                }
            }
        }
        System.out.println("tests = " + tests);
        Set<String> uniqueTests = new HashSet<>(tests);
        List<String> uniqueTestList = new ArrayList<>(uniqueTests);
        return uniqueTestList;
    }

    public void updateStatusToPeformed(PatientSampleComponant psc) {
        if (psc == null) {
            return;
        }
        if (psc.getPatientInvestigation() == null) {
            return;
        }
        PatientInvestigation pi = psc.getPatientInvestigation();
        pi.setStatus(PatientInvestigationStatus.SAMPLE_INTERFACED);
        pi.setPerformed(true);
//            pi.setPerformDepartment(sessionController.getDepartment());
//            pi.setPerformInstitution(sessionController.getInstitution());
        pi.setPerformedAt(new Date());
//            pi.setPerformedUser(sessionController.getLoggedUser());
        patientInvestigationFacade.edit(pi);
        if (pi.getBillItem() == null) {
            return;
        }
        if (pi.getBillItem().getBill() == null) {
            return;
        }
        Bill b = pi.getBillItem().getBill();
        b.setStatus(PatientInvestigationStatus.SAMPLE_INTERFACED);
        billFacade.edit(b);
        if (psc.getSample() == null) {
            return;
        }
        PatientSample ps = psc.getPatientSample();
        ps.setStatus(PatientInvestigationStatus.SAMPLE_INTERFACED);
        ps.setSentToAnalyzer(Boolean.TRUE);
        ps.setSentToAnalyzerAt(new Date());
        //TODO: Add other fields
        patientSampleFacade.edit(ps);

    }

    public List<String> generateTestCodesForAnalyzer(String sampleId) {
        // // System.out.println("generateTestCodesForAnalyzer");
        PatientSample ps = patientSampleFromId(sampleId);
        // // System.out.println("ps = " + ps);
        if (ps == null) {
            // // System.out.println("No PS");
            return null;
        }

        List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
        // // System.out.println("pscs = " + pscs);
        if (pscs == null || pscs.isEmpty()) {
            // // System.out.println("PSCS NULL OR EMPTY");
            return null;
        }

        List<String> tests = new ArrayList<>();

        for (PatientSampleComponant c : pscs) {
            // // System.out.println("c = " + c);
            Investigation myIx = c.getPatientInvestigation().getInvestigation();

            if (myIx == null) {
                return null;
            }

            if (myIx.getReportedAs() != null) {
                if (myIx.getReportedAs() instanceof Investigation) {
                    myIx = (Investigation) myIx.getReportedAs();
                }
            }

            for (InvestigationItem tii : myIx.getReportItems()) {
                // // System.out.println("tii = " + tii);
                if (tii.getIxItemType() == InvestigationItemType.Value) {
                    String sampleTypeName;
                    String samplePriority;
                    if (tii.getSample() != null) {
                        sampleTypeName = tii.getSample().getName();
                    } else {
                        sampleTypeName = "serum";
                    }
                    if (tii.getItem().getPriority() != null) {
                        samplePriority = tii.getItem().getPriority().toString();
                    } else {
                        samplePriority = (Priority.Routeine).toString();
                    }
                    MySpeciman ms = new MySpeciman();
                    ms.setSpecimanName(sampleTypeName);
                    if (tii.getItem().isHasMoreThanOneComponant()) {
                        // // System.out.println("tti = " + tii);
                        if (tii.getTest() != null && !tii.getTest().getName().trim().equals("")) {
                            if (tii.getSampleComponent().equals(ps.getInvestigationComponant())) {

                                tests.add(tii.getTest().getCode());
                                // // System.out.println("1. tii.getTest().getCode() = " + tii.getTest().getCode());
                                tests.add(tii.getTest().getName());

                            }
                        }
                    } else {
                        if (tii.getTest() != null && !tii.getTest().getName().trim().equals("")) {
                            tests.add(tii.getTest().getCode());
                            // // System.out.println("1. tii.getTest().getCode() = " + tii.getTest().getCode());

                        }
                    }
                }
            }
        }
        // // System.out.println("tests = " + tests);
        return tests;
    }

    private static class MySampleTests {

        private String sampleId;
        private String testCode;
        private String testName;

        public MySampleTests() {
        }

        public String getSampleId() {
            return sampleId;
        }

        public void setSampleId(String sampleId) {
            this.sampleId = sampleId;
        }

        public String getTestCode() {
            return testCode;
        }

        public void setTestCode(String testCode) {
            this.testCode = testCode;
        }

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
        }

    }

    private static class MyPatient {

        private String patientName;
        private String patientId;
        private List<MySpeciman> mySpecimans;

        public String getPatientName() {
            return patientName;
        }

        public void setPatientName(String patientName) {
            this.patientName = patientName;
        }

        public String getPatientId() {
            return patientId;
        }

        public void setPatientId(String patientId) {
            this.patientId = patientId;
        }

        public List<MySpeciman> getMySpecimans() {
            if (mySpecimans == null) {
                mySpecimans = new ArrayList<>();
            }
            return mySpecimans;
        }

        public void setMySpecimans(List<MySpeciman> mySpecimans) {
            this.mySpecimans = mySpecimans;
        }

    }

    private static class MySpeciman {

        private String specimanName;
        private List<MySampleTests> mySampleTests;

        public String getSpecimanName() {
            return specimanName;
        }

        public void setSpecimanName(String specimanName) {
            this.specimanName = specimanName;
        }

        public List<MySampleTests> getMySampleTests() {
            if (mySampleTests == null) {
                mySampleTests = new ArrayList<>();
            }
            return mySampleTests;
        }

        public void setMySampleTests(List<MySampleTests> mySampleTests) {
            this.mySampleTests = mySampleTests;
        }

    }

}
