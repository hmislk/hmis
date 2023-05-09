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

import org.json.JSONException;

import ca.uhn.hl7v2.model.*;
import com.divudi.bean.common.util.EncryptionUtils;
import com.divudi.bean.common.util.HL7Utils;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.ws.rs.HeaderParam;
import ca.uhn.hl7v2.*;
import ca.uhn.hl7v2.model.*;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.*;
import ca.uhn.hl7v2.model.v25.segment.*;
import com.divudi.data.InvestigationItemType;
import com.divudi.data.lab.Priority;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.PatientSample;
import com.divudi.entity.lab.PatientSampleComponant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ca.uhn.hl7v2.*;
import ca.uhn.hl7v2.model.*;
import ca.uhn.hl7v2.model.v25.datatype.*;
import ca.uhn.hl7v2.model.v25.message.*;
import ca.uhn.hl7v2.model.v25.segment.*;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.message.RSP_K11;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST Web Service
 *
 * @author Dr M H B Ariyaratne
 */
@Path("limsmw")
@RequestScoped
public class LimsMiddlewareController {

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
    WebUserFacade webUserFacade;
    @EJB
    ItemFacade itemFacade;

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
                    resultMessage = HL7Utils.sendACK_R22ForoulR22(receivedMessage);
                    break;
                case "QBP^Q11^QBP_Q11":
                    resultMessage = generateRSP_K11ForQBP_Q11(receivedMessage);
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
    
    public List<String> getSampleIdentifiers(String hl7Message) {
        List<String> sampleIdentifiers = new ArrayList<>();
        String[] segments = hl7Message.split("\\r");

        for (String segment : segments) {
            if (segment.startsWith("QPD")) {
                String[] fields = segment.split("\\|");
                if (fields.length >= 4) {
                    String sampleIdField = fields[3];
                    String[] sampleIds = sampleIdField.split("\\^");
                    sampleIdentifiers.addAll(Arrays.asList(sampleIds));
                }
            }
        }
        return sampleIdentifiers;
    }
    
    public static String generateRSP_K11ForQBP_Q11(String qbpMessage) {
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

    String rspMessage = "MSH|" + msh1 + "|"+ msh4 + "|" + msh5 + "|" + msh7 + "|"
            + msh9 + "|" + msh10 + "|" + msh11 + "|" + msh12 + "|" + msh15 + "|" + msh16 + "|\r"
            + "MSA|AA|" + qpd4 + "|\r"
            + "QAK|0|OK|" + qpd3 + "|\r";

    // extract sample IDs from QBP message
    String[] qpdFields = qpd3.split("\\^");
    String[] sampleIds = qpdFields[2].split("\\,");

    for (String sampleId : sampleIds) {
        rspMessage += "SPM||" + sampleId + "|||||||||||||||||||||||\r";
    }

    rspMessage += "ERR|||0|^^^^^^|200^No error^HL70357|\r";

    return rspMessage;
}


    public String processQBPCarotQ11CatorQBP_Q11(String messageString) {
        
        List<String> samplesIdentifiers = getSampleIdentifiers(messageString);
        
        for(String sid:samplesIdentifiers){
            
        }
        
        
        try {
            // Parse the incoming QBP^WOS^QBP_Q11 message
            HapiContext context = new DefaultHapiContext();
            Parser parser = context.getGenericParser();
            QBP_Q11 qbpMessage = (QBP_Q11) parser.parse(messageString);

            // Create a new RSP^WOS^RSP_K11 message
            RSP_K11 rspMessage = new RSP_K11();

            // Set MSH segment fields
            MSH msh = rspMessage.getMSH();
            msh.getFieldSeparator().setValue("|");
            msh.getEncodingCharacters().setValue("^~\\&");
            msh.getDateTimeOfMessage().getTime().setValue(new Date());
            msh.getMessageType().getTriggerEvent().setValue("K11");
            msh.getMessageType().getMessageStructure().setValue("RSP_K11");
            msh.getMessageControlID().setValue(qbpMessage.getMSH().getMessageControlID().getValue());
//            msh.getProcessingID().setValue("P");
            msh.getVersionID().getVersionID().setValue("2.5.1");

            // Set QAK segment fields
            QAK qak = rspMessage.getQAK();
            qak.getQueryResponseStatus().setValue("OK");
            qak.getQueryTag().setValue(qbpMessage.getQPD().getQueryTag().getValue());

            // Get sample ID from QBP^WOS^QBP_Q11 message
            String sampleId = qbpMessage.getQPD().getMessageQueryName().getIdentifier().getValue();

            // Get the list of MySampleOrder objects for the sample ID
            List<MySampleOrder> sampleOrders = generateTestDetailsForSampleIDs(sampleId);

            // Add MySampleOrder data to the RSP_K11 message
            for (MySampleOrder order : sampleOrders) {

                
            }

            return parser.encode(rspMessage);
        } catch (HL7Exception ex) {
            Logger.getLogger(LimsMiddlewareController.class.getName()).log(Level.SEVERE, null, ex);
            return "Error " + ex.getMessage();
        }
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

    public List<PatientSampleComponant> getPatientSampleComponents(PatientSample ps) {
        String j = "select psc from PatientSampleComponant psc where psc.patientSample = :ps";
        Map m = new HashMap();
        m.put("ps", ps);
        return patientSampleComponantFacade.findByJpql(j, m);
    }

    public List<MySampleOrder> generateTestDetailsForSampleIDs(String strSampleId) {
        PatientSample ps = patientSampleFromId(strSampleId);
        List<PatientSampleComponant> pscs = getPatientSampleComponents(ps);
        return generateTestDetailsForSampleIDs(pscs, ps);
    }

    public List<MySampleOrder> generateTestDetailsForSampleIDs(List<PatientSampleComponant> limsPatientSampleComponants, PatientSample limsPatientSample) {
        Set<String> temsss = new HashSet<>();
        List<String> temss = new ArrayList<>();
        if (limsPatientSample == null) {
            return null;
        }
        List<MySampleOrder> os = new ArrayList<>();
        for (PatientSampleComponant c : limsPatientSampleComponants) {
            for (InvestigationItem tii : c.getPatientInvestigation().getInvestigation().getReportItems()) {
                if (tii.getIxItemType() == InvestigationItemType.Value) {
                    String sampleTypeName;
                    String samplePriority;
                    if (tii.getSample() != null) {
                        sampleTypeName = tii.getSample().getName();
                    }
                    if (tii.getItem().getPriority() != null) {
                        samplePriority = tii.getItem().getPriority().toString();
                    } else {
                        samplePriority = (Priority.Routeine).toString();
                    }
                    if (tii.getItem().isHasMoreThanOneComponant()) {
                        if (tii.getTest() != null && !tii.getTest().getName().trim().equals("")) {
                            if (tii.getSampleComponent().equals(limsPatientSample.getInvestigationComponant())) {
                                MySampleOrder o = new MySampleOrder();
                                o.patientName = c.getPatient().getPerson().getNameWithTitle();
                                o.patientId = c.getPatient().getId() + "";
                                o.sampleId = c.getIdStr();
                                o.testName = tii.getTest().getCode();
                                os.add(o);
                            }
                        }
                    } else {
                        if (tii.getTest() != null && !tii.getTest().getName().trim().equals("")) {
                            MySampleOrder o = new MySampleOrder();
                            o.patientName = c.getPatient().getPerson().getNameWithTitle();
                            o.patientId = c.getPatient().getId() + "";
                            o.sampleId = c.getIdStr();
                            o.testName = tii.getTest().getCode();
                            os.add(o);
                        }
                    }
                }
            }

        }
        return os;
    }

    private boolean isValidCredentials(String username, String password) {
        // TODO: add your logic to validate the username and password
        return true;
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

    private static class MySampleOrder {

        String patientName;
        String patientId;
        String sampleId;
        String testCode;
        String testName;

        public MySampleOrder() {
        }
    }

}
