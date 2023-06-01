/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ws.lims;

import com.divudi.bean.common.SecurityController;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.InvestigationItemFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PatientSampleComponantFacade;
import com.divudi.facade.PatientSampleFacade;
import com.divudi.facade.WebUserFacade;
import java.util.HashMap;
import java.util.Map;
import com.divudi.data.LoginRequest;

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
import com.divudi.bean.common.util.HL7Utils;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.ws.rs.HeaderParam;
import ca.uhn.hl7v2.*;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.lab.Priority;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.PatientSample;
import com.divudi.entity.lab.PatientSampleComponant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import ca.uhn.hl7v2.parser.Parser;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.InvestigationItemValueType;
import com.divudi.data.lab.SysMex;
import com.divudi.data.lab.SysMexAdf1;
import com.divudi.data.lab.SysMexAdf2;
import com.divudi.data.lab.SysMexTypeA;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItemValueFlag;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.entity.lab.PatientReportItemValue;
import com.divudi.entity.lab.ReportItem;
import com.divudi.facade.InvestigationItemValueFlagFacade;
import com.divudi.facade.PatientReportFacade;
import com.divudi.facade.PatientReportItemValueFacade;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST Web Service
 *
 * @author Dr M H B Ariyaratne
 */
@Path("limsmw")
@RequestScoped
public class LimsMiddlewareController {

    //FOR UNIT TESTING
    @EJB
    InvestigationItemFacade investigationItemFacade;
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
    private PatientReportItemValueFacade ptRivFacade;
    @EJB
    InvestigationItemValueFlagFacade iivfFacade;

    private WebUser loggedUser;
    private Department loggedDepartment;
    private Institution loggedInstitution;

    /**
     * Creates a new instance of LIMS
     */
    public LimsMiddlewareController() {
    }

    @Path("/limsProcessAnalyzerMessage")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response limsProcessAnalyzerMessage(String requestBody, @HeaderParam("Authorization") String authorizationHeader) {

        System.out.println("limsProcessAnalyzerMessage");

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
            System.out.println("receivedMessage = " + receivedMessage);

            if (!isValidCredentials(username, password)) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            String resultMessage = "";

            String messageType = HL7Utils.findMessageType(receivedMessage);
            System.out.println("messageType = " + messageType);

            switch (messageType) {
                case "OUL^R22^OUL_R22":
                case "OUL^R22":
                    resultMessage = sendACK_R22ForoulR22(receivedMessage);
                    break;
                case "QBP^Q11^QBP_Q11":
//                    resultMessage = generateRSP_K11ForQBP_Q11(receivedMessage);
                    String tempUnitId = generateUniqueIDForK11FromQ11(receivedMessage);
                    System.out.println("tempUnitId = " + tempUnitId);
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
            System.out.println("resultMessage = " + resultMessage);
            System.out.println("responseJson = " + responseJson);
            return Response.ok(responseJson.toString()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    public static String removeFirstFiveCharacters(String message) {
        if (message.length() <= 5) {
            return "";
        }
        return message.substring(5);
    }

    private String msgFromSysmex(String msg) {
        System.out.println("msgFromSysmex");
        System.out.println("msg = " + msg);
        System.out.println("msg = " + msg.length());
        SysMex sysMex = new SysMex();
        return extractDataFromSysMexTypeA(msg);
    }

    public String extractDataFromSysMexTypeA(String msg) {
        SysMexTypeA a = new SysMexTypeA();
        a.setInputString(msg);
        Long sampleId = a.getSampleId();
        System.out.println("sampleId = " + sampleId);
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
                                priv.setStrValue(a.getWbc() + "");
                                priv.setDoubleValue(a.getWbc());
                                break;
                            case "NEUT%":
                                priv.setStrValue(a.getNeutPercentage() + "");
                                break;
                            case "LYMPH%":
                                priv.setStrValue(a.getLymphPercentage() + "");
                                break;
                            case "BASO%":
                                priv.setStrValue(a.getBasoPercentage() + "");
                                break;
                            case "MONO%":
                                priv.setStrValue(a.getMonoPercentage() + "");
                                break;
                            case "EO%":
                                priv.setStrValue(a.getEoPercentage() + "");
                                break;
                            case "RBC":
                                priv.setStrValue(a.getRbc() + "");
                                break;
                            case "HGB":
                                priv.setStrValue(a.getHgb() + "");
                                break;
                            case "HCT":
                                priv.setStrValue(a.getHct() + "");
                                break;
                            case "MCV":
                                priv.setStrValue(a.getMcv() + "");
                                break;
                            case "MCH":
                                priv.setStrValue(a.getMch() + "");
                                break;
                            case "MCHC":
                                priv.setStrValue(a.getMchc() + "");
                                break;
                            case "PLT":
                                priv.setStrValue(a.getPlt() + "");
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
        System.out.println("Create K11 from Q11");

        // Extract Message Control ID from input message
        int startIndex = inputMessage.indexOf("QBP^Q11^QBP_Q11");
        System.out.println("startIndex = " + startIndex);
        if (startIndex == -1) {
            System.out.println("Couldn't find the string 'QBP^Q11^QBP_Q11' in the input message");
            return null;
        }
        int inputMessageControlIDIndex = inputMessage.indexOf("|", startIndex) + 1;
        System.out.println("inputMessageControlIDIndex = " + inputMessageControlIDIndex);
        int endIndex = inputMessage.indexOf("|", inputMessageControlIDIndex);
        System.out.println("endIndex = " + endIndex);
        String inputMessageControlID = inputMessage.substring(inputMessageControlIDIndex, endIndex);
        System.out.println("inputMessageControlID = " + inputMessageControlID);

        String responseMessageTemplate = "MSH|^~\\&|%s|%s|%s|%s||RSP^K11^RSP_K11|%s|P|2.5.1|||ER|NE||UNICODE UTF-8|||LAB27^IHE"
                + "\nMSA|AA|%s"
                + "\nQAK|%s|OK|WOS^Work Order Step^IHE_LABTF"
                + "\nQPD|WOS^Work Order Step^IHE_LABTF|%s|SPM01";

        System.out.println("responseMessageTemplate = " + responseMessageTemplate);

        return String.format(responseMessageTemplate, sendingApplication, sendingFacility, receivingApplication,
                receivingFacility, messageControlID, inputMessageControlID, messageControlID, messageControlID);
    }

    public String sendACK_R22ForoulR22(String oulR22Message) {
        System.out.println("sendACK_R22ForoulR22");
        boolean success;
        List<MyTestResult> myResults = getResultsFromOUL_R22Message(oulR22Message);
        System.out.println("myResults = " + myResults.size());
        success = addResultsFromMyResults(myResults);
        System.out.println("success = " + success);
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
        System.out.println("generateRSP_K11ForQBP_Q11");
        System.out.println("patients = " + patients);
        System.out.println("qbpMessage = " + qbpMessage);
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
        System.out.println("rspMessage = " + rspMessage);
        return rspMessage;
    }

    private String generateRSP_K11ForQBP_Q11(String qbpMessage) {
        System.out.println("generateRSP_K11ForQBP_Q11");
        List<MyPatient> mps = generateListOfPatientSpecimanTestDataFromQBPcQ11cQBP_Q11(qbpMessage);
        return generateRSP_K11ForQBP_Q11(qbpMessage, mps);
    }

    private boolean addResultsFromMyResults(List<MyTestResult> mrs) {
        boolean ok = true;
        for (MyTestResult mr : mrs) {
            boolean thisOk = addResultToReport(mr.getSampleId(), mr.getTestStr(), mr.getResult(), mr.getUnit(), mr.getError());
            System.out.println("mr.getSampleId() = " + mr.getSampleId());
            System.out.println("thisOk = " + thisOk);
            if (!thisOk) {
                ok = false;
            }
        }
        return ok;
    }

    private boolean addResultToReport(String sampleId, String testStr, String result, String unit, String error) {
        System.out.println("addResultToReport");
        boolean temFlag = false;
        Long sid;
        try {
            sid = Long.valueOf(sampleId);
        } catch (NumberFormatException e) {
            sid = 0l;
        }
        System.out.println("sid = " + sid);
        PatientSample ps = patientSampleFromId(sid);
        System.out.println("ps = " + ps);
        if (ps == null) {
            return temFlag;
        }

        List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
        System.out.println("pscs = " + pscs);
        if (pscs == null) {
            return temFlag;
        }
        List<PatientInvestigation> ptixs = getPatientInvestigations(pscs);
        System.out.println("ptixs = " + ptixs);
        if (ptixs == null || ptixs.isEmpty()) {
            return temFlag;
        }
        for (PatientInvestigation pi : ptixs) {
            System.out.println("pi = " + pi);
            List<PatientReport> prs = new ArrayList<>();
            PatientReport tpr;
            tpr = getUnapprovedPatientReport(pi);
            if (tpr == null) {
                tpr = createNewPatientReport(pi, pi.getInvestigation());
            }
            prs.add(tpr);

            for (PatientReport rtpr : prs) {
                System.out.println("rtpr = " + rtpr);
                for (PatientReportItemValue priv : rtpr.getPatientReportItemValues()) {
                    if (priv.getInvestigationItem() != null && priv.getInvestigationItem().getTest() != null
                            && priv.getInvestigationItem().getIxItemType() == InvestigationItemType.Value) {
                        String test;
                        test = priv.getInvestigationItem().getResultCode();
                        System.out.println("test = " + test);
                        if (test == null || test.trim().equals("")) {
                            test = priv.getInvestigationItem().getTest().getCode().toUpperCase();
                        }

                        if (test.toLowerCase().equals(testStr.toLowerCase())) {
                            if (ps.getInvestigationComponant() == null || priv.getInvestigationItem().getSampleComponent() == null) {
                                priv.setStrValue(result);
                                Double dbl = 0d;
                                try {
                                    dbl = Double.parseDouble(result);
                                } catch (Exception e) {
                                }
                                priv.setDoubleValue(dbl);
                                temFlag = true;
                            } else if (priv.getInvestigationItem().getSampleComponent().equals(ps.getInvestigationComponant())) {
                                priv.setStrValue(result);
                                Double dbl = 0d;
                                try {
                                    dbl = Double.parseDouble(result);
                                } catch (Exception e) {
                                }
                                priv.setDoubleValue(dbl);
                                temFlag = true;
                            } else {
                            }
                        }
                    }
                }
                rtpr.setDataEntered(true);
                rtpr.setDataEntryAt(new Date());
                rtpr.setDataEntryComments("Initial Results were taken from Analyzer through Middleware");
                prFacade.edit(rtpr);
            }
        }

        return temFlag;
    }

    public List<MyTestResult> getResultsFromOUL_R22Message(String message) {
        System.err.println("getResultsFromOUL_R22Message");
        System.out.println("message = " + message);
        System.out.println("message Length = " + message.length());
        System.out.println("message Type = " + HL7Utils.findMessageType(message));

        List<MyTestResult> results = new ArrayList<>();
        String[] segments = message.split("\\r");
        System.out.println("segments Length = " + segments.length);

        String sampleId = null;

        for (int i = 0; i < segments.length; i++) {
            if (segments[i].startsWith("SPM")) {
                String[] fields = segments[i].split("\\|");
                String temSampleId = fields[2];
                sampleId = extractNumber(temSampleId);
                System.out.println("Sample ID: " + sampleId);
            } else if (segments[i].startsWith("OBX")) {
                String[] fields = segments[i].split("\\|");

                if (fields.length > 14) { //Ensure there are enough fields before trying to access them
                    String[] testDetails = fields[3].split("\\^");
                    String testCode = testDetails[0];
                    System.out.println("testCode = " + testCode);
                    String result = fields[5];
                    System.out.println("result = " + result);
                    String unit = fields[6];
                    System.out.println("unit = " + unit);
                    String error = null;
                    if (fields.length > 15) {
                        error = fields[15];
                        System.out.println("error = " + error);
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
            addPatientReportItemValuesForReport(r);
            prFacade.edit(r);
        } else {
            UtilityController.addErrorMessage("No ptIx or Ix selected to add");
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

    public void addPatientReportItemValuesForReport(PatientReport ptReport) {
        String sql = "";
        Investigation temIx = (Investigation) ptReport.getItem();
        for (ReportItem ii : temIx.getReportItems()) {
            PatientReportItemValue val = null;
            if ((ii.getIxItemType() == InvestigationItemType.Value || ii.getIxItemType() == InvestigationItemType.Calculation || ii.getIxItemType() == InvestigationItemType.Flag || ii.getIxItemType() == InvestigationItemType.Template) && ii.isRetired() == false) {
                if (ptReport.getId() == null || ptReport.getId() == 0) {

                    val = new PatientReportItemValue();
                    if (ii.getIxItemValueType() == InvestigationItemValueType.Varchar) {
                        val.setStrValue(getDefaultVarcharValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                    } else if (ii.getIxItemValueType() == InvestigationItemValueType.Memo) {
                        val.setLobValue(getDefaultMemoValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                    } else if (ii.getIxItemValueType() == InvestigationItemValueType.Double) {
                        val.setDoubleValue(getDefaultDoubleValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                    } else if (ii.getIxItemValueType() == InvestigationItemValueType.Image) {
                        val.setBaImage(getDefaultImageValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                    } else {
                    }
                    val.setInvestigationItem((InvestigationItem) ii);
                    val.setPatient(ptReport.getPatientInvestigation().getPatient());
                    val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
                    val.setPatientReport(ptReport);
                    // ptReport.getPatientReportItemValues().add(val);
                    ////// // System.out.println("New value added to pr teport" + ptReport);

                } else {
                    sql = "select i from PatientReportItemValue i where i.patientReport=:ptRp"
                            + " and i.investigationItem=:inv ";
                    HashMap hm = new HashMap();
                    hm.put("ptRp", ptReport);
                    hm.put("inv", ii);
                    val = ptRivFacade.findFirstByJpql(sql, hm);
                    if (val == null) {
                        ////// // System.out.println("val is null");
                        val = new PatientReportItemValue();
                        if (ii.getIxItemValueType() == InvestigationItemValueType.Varchar) {
                            val.setStrValue(getDefaultVarcharValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                        } else if (ii.getIxItemValueType() == InvestigationItemValueType.Memo) {
                            val.setLobValue(getDefaultMemoValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                        } else if (ii.getIxItemValueType() == InvestigationItemValueType.Double) {
                            val.setDoubleValue(getDefaultDoubleValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                        } else if (ii.getIxItemValueType() == InvestigationItemValueType.Image) {
                            val.setBaImage(getDefaultImageValue((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                        } else {
                        }
                        val.setInvestigationItem((InvestigationItem) ii);
                        val.setPatient(ptReport.getPatientInvestigation().getPatient());
                        val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
                        val.setPatientReport(ptReport);
                        //ptReport.getPatientReportItemValues().add(val);
                        ////// // System.out.println("value added to pr teport" + ptReport);

                    }

                }
            } else if (ii.getIxItemType() == InvestigationItemType.DynamicLabel && ii.isRetired() == false) {
                if (ptReport.getId() == null || ptReport.getId() == 0) {

                    val = new PatientReportItemValue();
                    val.setStrValue(getPatientDynamicLabel((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                    val.setInvestigationItem((InvestigationItem) ii);
                    val.setPatient(ptReport.getPatientInvestigation().getPatient());
                    val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
                    val.setPatientReport(ptReport);
                    // ptReport.getPatientReportItemValues().add(val);
                    ////// // System.out.println("New value added to pr teport" + ptReport);

                } else {
                    sql = "select i from PatientReportItemValue i where i.patientReport.id = " + ptReport.getId() + " and i.investigationItem.id = " + ii.getId() + " and i.investigationItem.ixItemType = com.divudi.data.InvestigationItemType.Value";
                    val = ptRivFacade.findFirstByJpql(sql);
                    if (val == null) {
                        val = new PatientReportItemValue();
                        val.setStrValue(getPatientDynamicLabel((InvestigationItem) ii, ptReport.getPatientInvestigation().getPatient()));
                        val.setInvestigationItem((InvestigationItem) ii);
                        val.setPatient(ptReport.getPatientInvestigation().getPatient());
                        val.setPatientEncounter(ptReport.getPatientInvestigation().getEncounter());
                        val.setPatientReport(ptReport);
                        // ptReport.getPatientReportItemValues().add(val);
                        ////// // System.out.println("value added to pr teport" + ptReport);

                    }

                }
            }

            if (val != null) {

                ptRivFacade.create(val);

                ptReport.getPatientReportItemValues().add(val);
            }
        }
    }

    public String getPatientDynamicLabel(InvestigationItem ii, Patient p) {
        String dl;
        String sql;
        dl = ii.getName();

        long ageInDays = com.divudi.java.CommonFunctions.calculateAgeInDays(p.getPerson().getDob(), Calendar.getInstance().getTime());
        sql = "select f from InvestigationItemValueFlag f where  f.fromAge < " + ageInDays + " and f.toAge > " + ageInDays + " and f.investigationItemOfLabelType.id = " + ii.getId();
        List<InvestigationItemValueFlag> fs = iivfFacade.findBySQL(sql);
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
                + " and (r.approved = :a or r.approved is null) "
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
        System.out.println("generateListOfPatientSpecimanTestDataFromQBPcQ11cQBP_Q11");
        List<String> samplesIdentifiers = getSampleIdentifiersFromqBPcQ11cQBP_Q11(qBPcQ11cQBP_Q11);
        System.out.println("samplesIdentifiers = " + samplesIdentifiers);
        List<MyPatient> mps = new ArrayList<>();
        for (String sid : samplesIdentifiers) {

            PatientSample ps = patientSampleFromId(sid);
            if (ps == null) {
                System.out.println("No PS");
                continue;
            }

            if (ps.getPatient() == null || ps.getPatient().getPerson() == null) {
                System.out.println("No patient");
                continue;
            }

            List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
            if (pscs == null || pscs.isEmpty()) {
                System.out.println("PSCS NULL OR EMPTY");
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
        System.out.println("mps = " + mps.size());
        return mps;
    }

    public PatientSample patientSampleFromId(String id) {
        Long pid = 0l;
        try {
            pid = Long.valueOf(id);
        } catch (NumberFormatException e) {
            System.out.println("e = " + e);
        }
        return patientSampleFacade.find(pid);
    }

    public PatientSample patientSampleFromId(Long id) {
        return patientSampleFacade.find(id);
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
        System.out.println("login");
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
