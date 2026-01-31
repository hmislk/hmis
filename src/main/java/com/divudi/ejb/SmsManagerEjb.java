/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.MessageType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Sms;
import com.divudi.core.entity.channel.SessionInstance;
import com.divudi.core.facade.SessionInstanceFacade;
import com.divudi.core.facade.SmsFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.json.JSONObject;

/**
 *
 * @author Buddhika
 */
@Stateless
public class SmsManagerEjb {

    @EJB
    SmsFacade smsFacade;
    @EJB
    private SessionInstanceFacade sessionInstanceFacade;
    @EJB
    private ChannelBean channelBean;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private SessionController sessionController;

    private static final boolean doNotSendAnySms = false;

    // ChatGPT and CodeRabbitAI contributed method:
    // Processes pending lab report approval SMS messages based on configurable delay strategies
    @Schedule(second = "0", minute = "*/1", hour = "*", persistent = false)
    public void processPendingLabReportApprovalSmsQueue() {
        if (configOptionApplicationController == null || smsFacade == null) {
            return;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Sending SMS After Lab Report Approval Strategy - Do Not Sent Automatically", false)) {
            return;
        }

        // Ensure all strategy keys are registered with one default true
        configOptionApplicationController.getBooleanValueByKey("Sending SMS After Lab Report Approval Strategy - Send after one minute", false);
        configOptionApplicationController.getBooleanValueByKey("Sending SMS After Lab Report Approval Strategy - Send after two minutes", false);
        configOptionApplicationController.getBooleanValueByKey("Sending SMS After Lab Report Approval Strategy - Send after 5 minutes", false);
        configOptionApplicationController.getBooleanValueByKey("Sending SMS After Lab Report Approval Strategy - Send after 10 minutes", true); // Default true
        configOptionApplicationController.getBooleanValueByKey("Sending SMS After Lab Report Approval Strategy - Send after 15 minutes", false);
        configOptionApplicationController.getBooleanValueByKey("Sending SMS After Lab Report Approval Strategy - Send after 20 minutes", false);
        configOptionApplicationController.getBooleanValueByKey("Sending SMS After Lab Report Approval Strategy - Send after half an hour", false);
        configOptionApplicationController.getBooleanValueByKey("Sending SMS After Lab Report Approval Strategy - Send after one hour", false);
        configOptionApplicationController.getBooleanValueByKey("Sending SMS After Lab Report Approval Strategy - Send after two hours", false);

        Map<String, Integer> strategyMinutes = new LinkedHashMap<>();
        strategyMinutes.put("Sending SMS After Lab Report Approval Strategy - Send after one minute", 1);
        strategyMinutes.put("Sending SMS After Lab Report Approval Strategy - Send after two minutes", 2);
        strategyMinutes.put("Sending SMS After Lab Report Approval Strategy - Send after 5 minutes", 5);
        strategyMinutes.put("Sending SMS After Lab Report Approval Strategy - Send after 10 minutes", 10);
        strategyMinutes.put("Sending SMS After Lab Report Approval Strategy - Send after 15 minutes", 15);
        strategyMinutes.put("Sending SMS After Lab Report Approval Strategy - Send after 20 minutes", 20);
        strategyMinutes.put("Sending SMS After Lab Report Approval Strategy - Send after half an hour", 30);
        strategyMinutes.put("Sending SMS After Lab Report Approval Strategy - Send after one hour", 60);
        strategyMinutes.put("Sending SMS After Lab Report Approval Strategy - Send after two hours", 120);

        int delayMinutes = 0;
        for (Map.Entry<String, Integer> entry : strategyMinutes.entrySet()) {
            if (configOptionApplicationController.getBooleanValueByKey(entry.getKey(), false)) {
                delayMinutes = entry.getValue();
                break;
            }
        }

        if (delayMinutes == 0) {
            return;
        }

        Calendar now = Calendar.getInstance();

        Calendar delayThreshold = (Calendar) now.clone();
        delayThreshold.add(Calendar.MINUTE, -delayMinutes);

        Calendar minCreatedAt = (Calendar) now.clone();
        minCreatedAt.add(Calendar.HOUR_OF_DAY, -24);

        String jpql = "Select e from Sms e where e.pending=true and e.retired=false "
                + "and e.smsType = :smsType and e.createdAt between :from and :to";
        Map<String, Object> params = new HashMap<>();
        params.put("from", minCreatedAt.getTime());
        params.put("to", delayThreshold.getTime());
        params.put("smsType", MessageType.LabReport);

        List<Sms> smses = smsFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        for (Sms sms : smses) {
            try {
                // Skip if investigation is null
                if (sms.getPatientInvestigation() == null) {
                    continue;
                }

                Bill bill = sms.getPatientInvestigation().getBillItem() != null
                        ? sms.getPatientInvestigation().getBillItem().getBill()
                        : null;

                if (bill == null || bill.getBalance() > 0.99) {
                    continue;
                }

                boolean success = sendSms(sms);
                sms.setSentSuccessfully(success);
                sms.setPending(!success);
                if (success) {
                    sms.setSentAt(new Date());
                }
                smsFacade.edit(sms);
            } catch (Exception e) {
                Logger.getLogger(SmsManagerEjb.class.getName()).log(Level.SEVERE,
                        "Failed to process SMS ID: " + (sms != null ? sms.getId() : "unknown"), e);
            }
        }
    }

    // Schedule sendSmsToDoctorsBeforeSession to run every 30 minutes
    @SuppressWarnings("unused")
    @Schedule(second = "*", minute = "*/1", hour = "*", persistent = false)
    public void sendSmsToDoctorsBeforeSessionTimer() {
        if (doNotSendAnySms) {
            return;
        }
        if (configOptionApplicationController.getBooleanValueByKey("Send SMS for Doctor Reminder")) {
            if (configOptionApplicationController.getBooleanValueByKey("Check & Send SMS for Doctor Reminder")) {
                sendSmsToDoctorsBeforeSessionWithChecks();
            } else {
                sendSmsToDoctorsBeforeSession();
            }
        }
    }

    private void sendSmsToDoctorsBeforeSessionWithChecks() {
        if (doNotSendAnySms) {
            return;
        }

        Date fromTime = new Date();
        Calendar calf = Calendar.getInstance();
        calf.setTime(fromTime);
        calf.add(Calendar.MINUTE, 30);
        Date fromTime1 = calf.getTime();

        Calendar cal = Calendar.getInstance();
        cal.setTime(fromTime);
        cal.add(Calendar.MINUTE, 60);
        Date toTime = cal.getTime();

        List<SessionInstance> upcomingSessions;
        Map<String, Object> m = new HashMap<>();
        Map<String, TemporalType> temporalTypes = new HashMap<>();

        String jpql = "select i from SessionInstance i "
                + "where i.retired=:ret "
                + "and i.originatingSession.retired=:retos "
                + "and i.sessionDate=:sessionDate "
                + "and i.startingTime between :startingTime and :endingTime";

        m.put("ret", false);
        m.put("retos", false);
        m.put("sessionDate", new Date());
        temporalTypes.put("sessionDate", TemporalType.DATE);

        m.put("startingTime", fromTime1);
        m.put("endingTime", toTime);
        temporalTypes.put("startingTime", TemporalType.TIME);
        temporalTypes.put("endingTime", TemporalType.TIME);

        upcomingSessions = sessionInstanceFacade.findByJpql(jpql, m, temporalTypes);

        for (SessionInstance s : upcomingSessions) {
            if (s.getBookedPatientCount() != null && !s.isCancelled() && s.getBookedPatientCount() > 0) {
                boolean isSmsSentBefore = checkSmsToDoctors(s);
                if (!isSmsSentBefore) {
                    sendSmsToDoctors(s);
                }
            }
        }
    }

    public void sendSmsToDoctorsBeforeSession() {
        if (doNotSendAnySms) {
            return;
        }
        Date fromDate = CommonFunctions.getStartOfDay();
        Date toDate = CommonFunctions.getEndOfDay();
        List<SessionInstance> sessions = channelBean.listSessionInstances(fromDate, toDate, null, null, null);

        Date fromTime = new Date();
        Calendar calf = Calendar.getInstance();
        calf.setTime(fromTime);
        calf.add(Calendar.MINUTE, 30);
        Date fromTime1 = calf.getTime();

        Calendar cal = Calendar.getInstance();
        cal.setTime(fromTime);
        cal.add(Calendar.MINUTE, 60);
        Date toTime = cal.getTime();

        List<SessionInstance> upcomingSessions = sessions.stream()
                .filter(session -> {
                    Date sessionStartDateTime = getSessionStartDateTime(session);
                    return sessionStartDateTime != null && sessionStartDateTime.after(fromTime1) && sessionStartDateTime.before(toTime);
                })
                .collect(Collectors.toList());

        for (SessionInstance s : upcomingSessions) {
            if (s.getBookedPatientCount() != null && !s.isCancelled() && s.getBookedPatientCount() > 0) {
                sendSmsToDoctors(s);
            }
        }
    }

    private Date getSessionStartDateTime(SessionInstance session) {
        Calendar sessionDateTimeCal = Calendar.getInstance();
        sessionDateTimeCal.setTime(session.getSessionDate());

        Calendar sessionTimeCal = Calendar.getInstance();
        sessionTimeCal.setTime(session.getSessionTime());

        sessionDateTimeCal.set(Calendar.HOUR_OF_DAY, sessionTimeCal.get(Calendar.HOUR_OF_DAY));
        sessionDateTimeCal.set(Calendar.MINUTE, sessionTimeCal.get(Calendar.MINUTE));
        sessionDateTimeCal.set(Calendar.SECOND, sessionTimeCal.get(Calendar.SECOND));

        return sessionDateTimeCal.getTime();
    }

    private void sendSmsToDoctors(SessionInstance session) {
        if (doNotSendAnySms) {
            return;
        }
        Sms e = new Sms();
        e.setCreatedAt(new Date());
        if (session.getStaff().getPerson().getMobile() == null || session.getStaff().getPerson().getMobile().isEmpty()) {
            e.setReceipientNumber(session.getStaff().getPerson().getPhone());
        } else {
            e.setReceipientNumber(session.getStaff().getPerson().getMobile());
        }
        e.setSendingMessage(createDoctorRemiderSms(session));
        e.setPending(false);
        e.setSmsType(MessageType.ChannelDoctorReminder);
        getSmsFacade().create(e);
        Boolean sent = sendSms(e);
        e.setSentSuccessfully(sent);
        getSmsFacade().edit(e);
    }

    private boolean checkSmsToDoctors(SessionInstance session) {
        if (doNotSendAnySms) {
            return true;
        }
        Sms ec = new Sms();
        ec.setCreatedAt(new Date());
        if (session.getStaff().getPerson().getMobile() == null || session.getStaff().getPerson().getMobile().isEmpty()) {
            ec.setReceipientNumber(session.getStaff().getPerson().getPhone());
        } else {
            ec.setReceipientNumber(session.getStaff().getPerson().getMobile());
        }
        ec.setSendingMessage(createDoctorRemiderSms(session));
        ec.setPending(false);
        ec.setSmsType(MessageType.ChannelDoctorReminder);

        String jpql = "select s "
                + "from Sms s "
                + "where s.retired = :ret "
                + "and s.sendingMessage = :sm "
                + "and s.receipientNumber = :rn "
                + "and s.smsType = :st "
                + "and s.createdAt <= :bd "
                + "order by s.id";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("sm", ec.getSendingMessage());
        params.put("st", ec.getSmsType());
        params.put("rn", ec.getReceipientNumber());
        params.put("bd", ec.getCreatedAt());

        List<Sms> sentSmsList = smsFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return !sentSmsList.isEmpty();
    }

    private String createDoctorRemiderSms(SessionInstance s) {
        String template = configOptionApplicationController.getLongTextValueByKey("Template for SMS sent on Doctor Reminder");
        if (template == null || template.isEmpty()) {
            template = "Dear {doctor}, Your consultation session on today {appointment_date} at {appointment_time} has {booked} appointments ( {paid} paid). {ins_name}";
        }
        return createSmsForCDoctorRemider(s, template);
    }

    public String createSmsForCDoctorRemider(SessionInstance si, String template) {
        if (si == null) {
            return "";
        }
        String s;
        String sessionTime = CommonFunctions.getDateFormat(si.getStartingTime(), "HH:mm");
        String sessionDate = CommonFunctions.getDateFormat(si.getSessionDate(), "dd MMMMM yyyy");
        String doc = si.getStaff().getPerson().getNameWithTitle();
        String booked = si.getBookedPatientCount().toString();
        String paid = si.getPaidPatientCount().toString();

        String insName = si.getInstitution().getName();

        s = template.replace("{doctor}", doc)
                .replace("{appointment_time}", sessionTime)
                .replace("{appointment_date}", sessionDate)
                .replace("{booked}", booked)
                .replace("{paid}", paid)
                .replace("{ins_name}", insName);

        return s;
    }

    public String executeJsonPost(String targetURL, JSONObject jsonPayload) {
        if (doNotSendAnySms) {
            return null;
        }

        HttpURLConnection connection = null;

        try {
            // Open connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            // Write JSON payload to the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.toString().getBytes("UTF-8");
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = connection.getResponseCode();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } catch (IOException e) {
            Logger.getLogger(SmsManagerEjb.class.getName()).log(Level.SEVERE, "HTTP request failed", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String executePost(String targetURL, Map<String, String> parameters) {
        if (doNotSendAnySms) {
            return null;
        }
        HttpURLConnection connection = null;
        StringBuilder urlParameters = new StringBuilder();

        if (parameters != null && !parameters.isEmpty()) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                try {
                    urlParameters.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                            .append("=")
                            .append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                            .append("&");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(SmsManagerEjb.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            urlParameters.setLength(urlParameters.length() - 1);
            targetURL += "?" + urlParameters.toString();
        }

        try {
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);

            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(targetURL);
                wr.flush();
            }

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line).append('\r');
            }
            rd.close();
            return response.toString();
        } catch (IOException e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String executePost(String targetURL, JSONObject jsonPayload, String accessToken) {
        if (doNotSendAnySms) {
            return null;
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("X-API-VERSION", "v1");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line).append('\r');
            }
            rd.close();
            return response.toString();
        } catch (IOException e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public SmsFacade getSmsFacade() {
        return smsFacade;
    }

    public boolean sendSms(Sms sms) {
        if (doNotSendAnySms) {
            return false;
        }
        boolean sendSmsWithOAuth2 = configOptionApplicationController.getBooleanValueByKey("SMS Sent Using OAuth 2.0 Supported SMS Gateway", false);
        boolean sendSmsWithBasicAuthentication = configOptionApplicationController.getBooleanValueByKey("SMS Sent Using Basic Authentication Supported SMS Gateway", false);
        boolean sendSmsWithBasicAuthenticationWithJson = configOptionApplicationController.getBooleanValueByKey("SMS Sent Using Basic Authentication Supported SMS Gateway with JQON", false);
        boolean sendSmsWithBasicAuthenticationWithSimpleJson = configOptionApplicationController.getBooleanValueByKey("SMS Sent Using Basic Authentication Supported SMS Gateway with Simple JSON", false);
        boolean sendSmsViaESms = configOptionApplicationController.getBooleanValueByKey("SMS Sent Using E -SMS Supported SMS Gateway", false);
        if (sendSmsWithOAuth2) {
            return sendSmsByOauth2(sms);
        } else if (sendSmsWithBasicAuthentication) {
            return sendSmsByBasicAuthentication(sms);
        } else if (sendSmsWithBasicAuthenticationWithJson) {
            return sendSmsWithJson(sms);
        } else if (sendSmsViaESms) {
            return sendSmsByESms(sms);
        } else if (sendSmsWithBasicAuthenticationWithSimpleJson) {
            return sendSmsWithSimpleJson(sms);
        }
        return false;
    }

    public boolean sendSmsByESms(Sms sms) {
        if (doNotSendAnySms) {
            return false;
        }
        String smsUsername = configOptionApplicationController.getShortTextValueByKey("SMS Gateway via E-SMS - Username");
        String smsPassword = configOptionApplicationController.getShortTextValueByKey("SMS Gateway via E-SMS - Password");
        String smsUserAlias = configOptionApplicationController.getShortTextValueByKey("SMS Gateway via E-SMS - User Alias");
        String smsUrl = configOptionApplicationController.getShortTextValueByKey("SMS Gateway via E-SMS - URL");

        // Create an instance of eSmsManager
        eSmsManager smsManager = new eSmsManager();

        boolean send = false; // smsManager.sendSms(smsUsername,smsPassword,smsUserAlias,sms.getReceipientNumber(),sms.getSendingMessage());

        if (send) {
            sms.setSentSuccessfully(true);
            saveSms(sms);
            return send;
        } else {
            sms.setSentSuccessfully(false);
            saveSms(sms);
            return send;
        }
    }

    public boolean sendSmsWithJson(Sms sms) {
        if (doNotSendAnySms) {
            return false;
        }

        // Prepare JSON payload using JSONObject
        JSONObject payload = new JSONObject();
        payload.put("username", configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Username"));
        payload.put("password", configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Password"));
        payload.put("userAlias", configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - User Alias"));
        payload.put("number", sms.getReceipientNumber());
        payload.put("message", sms.getSendingMessage());
        payload.put("promo", configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Additional parameter 1 value"));

        String smsUrl = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - URL");

        // Execute POST request with JSON payload
        String response = executeJsonPost(smsUrl, payload);

        if (response == null) {
            sms.setSentSuccessfully(false);
            sms.setReceivedMessage(response);
            saveSms(sms);
            return false;
        } else if (response.toUpperCase().contains("OK")) {
            sms.setSentSuccessfully(true);
            sms.setReceivedMessage(response);
            saveSms(sms);
            return true;
        } else {
            sms.setSentSuccessfully(false);
            sms.setReceivedMessage(response);
            saveSms(sms);
            return false;
        }
    }
    
    public boolean sendSmsWithSimpleJson(Sms sms) {
        if (doNotSendAnySms) {
            return false;
        }

        try {
            // Prepare JSON payload
            JSONObject payload = new JSONObject();
            payload.put("username", configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Simple JSON - Username"));
            payload.put("password", configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Simple JSON - Password"));
            payload.put("from", configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Simple JSON - User Alias"));
            payload.put("to", sms.getReceipientNumber()); // For multiple, use comma-separated
            payload.put("text", sms.getSendingMessage());
            payload.put("mesageType", Integer.parseInt(configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Simple JSON - Additional parameter 1 value (Only Numbers)"))); // 0 or 1

            String smsUrl = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Simple JSON - URL");

            // Send POST request
            URL url = new URL(smsUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            StringBuilder response = new StringBuilder();
            int responseCode;
            try {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = payload.toString().getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                // Read the response
                responseCode = conn.getResponseCode();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
            } finally {
                conn.disconnect();
            }

            sms.setReceivedMessage(response.toString());

            if (responseCode == 200 && response.toString().toUpperCase().contains("200")) {
                sms.setSentSuccessfully(true);
            } else {
                sms.setSentSuccessfully(false);
            }

            saveSms(sms);
            return sms.getSentSuccessfully();

        } catch (Exception e) {
            sms.setSentSuccessfully(false);
            sms.setReceivedMessage("Exception: " + e.getMessage());
            saveSms(sms);
            return false;
        }
    }

    public boolean sendSmsByBasicAuthentication(Sms sms) {
        if (doNotSendAnySms) {
            return false;
        }
        Map<String, String> m = new HashMap<>();
        String smsUsernameParameter = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Username parameter");
        String smsUsername = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Username");

        String smsPasswordParameter = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Password parameter");
        String smsPassword = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Password");

        String smsUserAliasParameter = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - User Alias parameter");
        String smsUserAlias = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - User Alias");

        String smsAdditionalParameter1 = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Additional parameter 1");
        String smsAdditionalParameter1Value = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Additional parameter 1 value");

        String smsPhoneNumberParameter = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Phone Number parameter");
        String smsMessageParameter = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Message parameter");
        String smsUrl = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - URL");

        m.put(smsUsernameParameter, smsUsername);
        m.put(smsPasswordParameter, smsPassword);
        if (smsUserAliasParameter != null && !smsUserAliasParameter.trim().isEmpty()) {
            m.put(smsUserAliasParameter, smsUserAlias);
        }
        if (smsAdditionalParameter1 != null && !smsAdditionalParameter1.trim().isEmpty()) {
            m.put(smsAdditionalParameter1, smsAdditionalParameter1Value);
        }
        m.put(smsPhoneNumberParameter, sms.getReceipientNumber());
        m.put(smsMessageParameter, sms.getSendingMessage());

        String res = executePost(smsUrl, m);

        if (res == null) {
            sms.setSentSuccessfully(false);
            sms.setReceivedMessage(res);
            saveSms(sms);
            return false;
        } else if (res.toUpperCase().contains("OK")) {
            sms.setSentSuccessfully(true);
            sms.setReceivedMessage(res);
            saveSms(sms);
            return true;
        } else {
            sms.setSentSuccessfully(false);
            sms.setReceivedMessage(res);
            saveSms(sms);
            return false;
        }
    }

    public boolean sendSmsByOauth2(Sms sms) {
        if (doNotSendAnySms) {
            return false;
        }
        try {
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put(configOptionApplicationController.getShortTextValueByKey("OAuth2 SMS Gateway - Parameter 1 Name", "campaignName"), configOptionApplicationController.getShortTextValueByKey("OAuth2 SMS Gateway - Parameter 1 Value", "Test"));
            jsonPayload.put(configOptionApplicationController.getShortTextValueByKey("OAuth2 SMS Gateway - Parameter 2 Name", "mask"), configOptionApplicationController.getShortTextValueByKey("OAuth2 SMS Gateway - Parameter 2 Value", "Test"));
            jsonPayload.put(configOptionApplicationController.getShortTextValueByKey("OAuth2 SMS Gateway - Parameter Name for SMS Numbers", "numbers"), sms.getReceipientNumber());
            jsonPayload.put(configOptionApplicationController.getShortTextValueByKey("OAuth2 SMS Gateway - Parameter Name for SMS Text", "content"), sms.getSendingMessage());

            String loginUrl = configOptionApplicationController.getShortTextValueByKey("OAuth2 SMS Gateway - Login URL", "https://bsms.hutch.lk/api/login");
            String refreshTokenUrl = configOptionApplicationController.getShortTextValueByKey("OAuth2 SMS Gateway - Refresh Token URL", "https://bsms.hutch.lk/api/login/api/token/accessToken");
            String smsGatewayUrl = configOptionApplicationController.getShortTextValueByKey("OAuth2 SMS Gateway - URL", "https://bsms.hutch.lk/api/login/api/sendsms");
            String userName = configOptionApplicationController.getShortTextValueByKey("OAuth2 SMS Gateway - Username");
            String password = configOptionApplicationController.getShortTextValueByKey("OAuth2 SMS Gateway - Password");
            URL url = new URL(smsGatewayUrl);

            String accessToken = configOptionApplicationController.getShortTextValueByKey("OAuth2 SMS Gateway - Access Token");
            if (accessToken == null || accessToken.trim().isEmpty()) {
                accessToken = getNewAccessToken(userName, password, loginUrl);
            }

            long expiresIn = getExpiryFromJWT(accessToken) - Instant.now().getEpochSecond();
            if (expiresIn < 3) {
                accessToken = getAccessToken(loginUrl, accessToken, accessToken, loginUrl, refreshTokenUrl);
                configOptionApplicationController.saveShortTextOption("OAuth2 SMS Gateway - Access Token", accessToken);
            }

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("X-API-VERSION", "v1");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                sms.setReceivedMessage(response.toString());
            } catch (IOException e) {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(errorStream, "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            saveSms(sms);
            return true;
        } catch (Exception e) {
            sms.setReceivedMessage(sms.getReceivedMessage() + e.getMessage());
            saveSms(sms);
            return false;
        }
    }

    public void saveSms(Sms savingSms) {
        if (savingSms == null) {
            return;
        }
        if (savingSms.getId() == null) {
            smsFacade.create(savingSms);
        } else {
            smsFacade.edit(savingSms);
        }
    }

    private static final String LOGIN_URL = "https://bsms.hutch.lk/api/login";
    private static final String REFRESH_TOKEN_URL = "https://bsms.hutch.lk/api/login/api/token/accessToken";

    public static String getAccessToken(String username, String password, String refreshToken, String loginUrl, String refreshTokenUrl) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return getNewAccessToken(username, password, loginUrl);
        } else {
            return refreshAccessToken(refreshToken, refreshTokenUrl);
        }
    }

    private static String getNewAccessToken(String username, String password, String loginUrl) {
        try {
            URL url = new URL(loginUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("X-API-VERSION", "v1");

            JSONObject credentials = new JSONObject();
            credentials.put("username", username);
            credentials.put("password", password);

            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = credentials.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            return extractTokenFromResponse(conn);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String refreshAccessToken(String refreshToken, String refreshTokenUrl) {
        try {
            URL url = new URL(refreshTokenUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("X-API-VERSION", "v1");
            conn.setRequestProperty("Authorization", "Bearer " + refreshToken);

            return extractTokenFromResponse(conn);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String extractTokenFromResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                JSONObject responseJson = new JSONObject(response.toString());
                return responseJson.getString("accessToken");
            }
        } else {
            return null;
        }
    }

    public static long getExpiryFromJWT(String jwtToken) {
        try {
            String[] splitToken = jwtToken.split("\\.");
            String base64EncodedBody = splitToken[1];
            String body = new String(Base64.getUrlDecoder().decode(base64EncodedBody));
            JSONObject jsonBody = new JSONObject(body);
            return jsonBody.getLong("exp");
        } catch (Exception e) {
            e.printStackTrace();
            return Long.MIN_VALUE;
        }
    }

    public SessionInstanceFacade getSessionInstanceFacade() {
        return sessionInstanceFacade;
    }

    public void setSessionInstanceFacade(SessionInstanceFacade sessionInstanceFacade) {
        this.sessionInstanceFacade = sessionInstanceFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }
}
