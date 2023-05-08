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
import java.util.Base64;
import javax.ws.rs.HeaderParam;

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

//        System.out.println("requestBody = " + requestBody);

        try {
            JSONObject requestJson = new JSONObject(requestBody);

            String encodedCredentials = authorizationHeader.split(" ")[1];
            String decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials.getBytes()));
            String[] credentialsArray = decodedCredentials.split(":");
            String username = credentialsArray[0];
            String password = credentialsArray[1];

            String encryptedMessage = requestJson.getString("message");
            System.out.println("encryptedMessage = " + encryptedMessage.length());
            String receivedMessage = EncryptionUtils.decrypt(encryptedMessage);
            System.out.println("receivedMessage = " + receivedMessage.length());
            if (!isValidCredentials(username, password)) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            String resultMessage = "";

            if (thisIsOulR22Message(receivedMessage)) {
                System.out.println("this is a OulR22 message");
                try {
                    resultMessage = generateACKMessage(receivedMessage);
                } catch (Exception ex) {
                    resultMessage = ex.getMessage();
                }
            } else {
                System.out.println("other message than OulR22");
            }

            // Process the received message here and return the response message as a JSON
            JSONObject responseJson = new JSONObject();
            responseJson.put("result", resultMessage);

            return Response.ok(responseJson.toString()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    public String generateACKMessage(String oulR22Message) {
        // Parse the OUL^R22 message
        System.out.println("generateACKMessage");
        String[] segments = oulR22Message.split("\\r");
        String messageControlID = "";
        for (String segment : segments) {
            String[] fields = segment.split("\\|");
            if (fields[0].equals("MSH")) {
                messageControlID = fields[9];
            }
        }

        // Build the ACK^R22 message
        String ackMessage = "MSH|^~\\&|Sender|Receiver|Application|Facility|20220505120000||ACK^R22|" + messageControlID + "|P|2.5\r"
                + "MSA|AA|" + messageControlID + "\r";

        return ackMessage;
    }

    

    public boolean thisIsOulR22Message(String message) {
        try {
            String[] segments = message.split("\r");
            String messageType = "";
            String messageStructure = "";
            for (String segment : segments) {
                String[] fields = segment.split("\\|");
                if (fields[0].equals("MSH")) {
                    messageType = fields[8];
                } else if (fields[0].equals("MSA")) {
                    messageStructure = fields[2];
                }
            }
            return messageType.equals("OUL") && messageStructure.equals("R22");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

}
