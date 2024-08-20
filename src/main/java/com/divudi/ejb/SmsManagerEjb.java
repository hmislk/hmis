/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.MessageType;
import com.divudi.data.SmsSentResponse;
import com.divudi.entity.Sms;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.facade.EmailFacade;
import com.divudi.facade.SessionInstanceFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.facade.UserPreferenceFacade;
import com.divudi.java.CommonFunctions;
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
import java.util.ArrayList;
import java.util.Base64;
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
    private EmailFacade emailFacade;
    @EJB
    UserPreferenceFacade userPreferenceFacade;
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

    private static boolean doNotSendAnySms = false;

    // Schedule sendSmsToDoctorsBeforeSession to run every 30 minutes
    @SuppressWarnings("unused")
    @Schedule(second = "0", minute = "*/30", hour = "*", persistent = false)
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
        List<Sms> sentSmsList = new ArrayList<>();
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

        sentSmsList = smsFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
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
        String sessionTime = CommonController.getDateFormat(si.getStartingTime(), "HH:mm");
        String sessionDate = CommonController.getDateFormat(si.getSessionDate(), "dd MMMMM yyyy");
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
        if (sendSmsWithOAuth2) {
            return sendSmsByOauth2(sms);
        } else if (sendSmsWithBasicAuthentication) {
            return sendSmsByBasicAuthentication(sms);
        }
        return false;
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

        String smsPhoneNumberParameter = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Phone Number parameter");
        String smsMessageParameter = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - Message parameter");
        String smsUrl = configOptionApplicationController.getShortTextValueByKey("SMS Gateway with Basic Authentication - URL");

        m.put(smsUsernameParameter, smsUsername);
        m.put(smsPasswordParameter, smsPassword);
        if (smsUserAliasParameter != null && !smsUserAliasParameter.trim().equals("")) {
            m.put(smsUserAliasParameter, smsUserAlias);
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
            if (accessToken == null || accessToken.trim().equals("")) {
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
