/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.MessageType;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.entity.Bill;
import com.divudi.core.facade.EmailFacade;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.net.ssl.HttpsURLConnection;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Stateless
public class EmailManagerEjb {

    @EJB
    private EmailFacade emailFacade;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    @Schedule(second = "0", minute = "*/1", hour = "*", persistent = false)
    public void processPendingLabReportApprovalEmailQueue() {
        if (configOptionApplicationController == null || emailFacade == null) {
            return;
        }

        if (configOptionApplicationController.getBooleanValueByKey("Sending Email After Lab Report Approval Strategy - Do Not Sent Automatically", false)) {
            return;
        }

        // Static configuration of strategies
        Map<String, Integer> strategyMinutes = new LinkedHashMap<>();
        strategyMinutes.put("Sending Email After Lab Report Approval Strategy - Send after one minute", 1);
        strategyMinutes.put("Sending Email After Lab Report Approval Strategy - Send after two minutes", 2);
        strategyMinutes.put("Sending Email After Lab Report Approval Strategy - Send after 5 minutes", 5);
        strategyMinutes.put("Sending Email After Lab Report Approval Strategy - Send after 10 minutes", 10);
        strategyMinutes.put("Sending Email After Lab Report Approval Strategy - Send after 15 minutes", 15);
        strategyMinutes.put("Sending Email After Lab Report Approval Strategy - Send after 20 minutes", 20);
        strategyMinutes.put("Sending Email After Lab Report Approval Strategy - Send after half an hour", 30);
        strategyMinutes.put("Sending Email After Lab Report Approval Strategy - Send after one hour", 60);
        strategyMinutes.put("Sending Email After Lab Report Approval Strategy - Send after two hours", 120);

        int delayMinutes = strategyMinutes.entrySet().stream()
                .filter(e -> configOptionApplicationController.getBooleanValueByKey(e.getKey(), false))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(0);

        if (delayMinutes == 0) {
            return;
        }

        Calendar now = Calendar.getInstance();
        Calendar delayThreshold = (Calendar) now.clone();
        delayThreshold.add(Calendar.MINUTE, -delayMinutes);

        Calendar minCreatedAt = (Calendar) now.clone();
        minCreatedAt.add(Calendar.HOUR_OF_DAY, -24);

        String jpql = "Select e from AppEmail e where e.sentSuccessfully <> true and e.retired=false "
                + "and e.messageType = :messageType and e.createdAt between :from and :to";
        Map<String, Object> params = new HashMap<>();
        params.put("from", minCreatedAt.getTime());
        params.put("to", delayThreshold.getTime());
        params.put("messageType", MessageType.LabReport);

        List<AppEmail> emails = emailFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        for (AppEmail email : emails) {
            try {
                if (email.getPatientInvestigation() == null || email.getReceipientEmail() == null) {
                    continue;
                }

                Bill bill = email.getPatientInvestigation().getBillItem() != null
                        ? email.getPatientInvestigation().getBillItem().getBill()
                        : null;

                if (bill == null || bill.getBalance() > 0.99) {
                    continue;
                }

                boolean success = sendEmail(
                        Collections.singletonList(email.getReceipientEmail()),
                        email.getMessageBody(),
                        email.getMessageSubject(),
                        true
                );

                email.setSentSuccessfully(success);
                email.setPending(!success);
                if (success) {
                    email.setSentAt(new Date());
                }
                emailFacade.edit(email);
            } catch (Exception e) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                        "Failed to process Email ID: " + (email != null ? email.getId() : "unknown"), e);
            }
        }
    }

//    @SuppressWarnings("unused")
//    @Schedule(second = "59", minute = "*/2", hour = "*", persistent = false)
//    public void myTimer() {
////        sendReportApprovalEmails();
//
//    }
    private boolean sendEmailViaRestGateway(String subject, String body, List<String> recipients, boolean isHtml) {
        String messengerServiceURL = configOptionApplicationController.getShortTextValueByKey("Email Gateway - URL", "");

        if (messengerServiceURL == null || messengerServiceURL.trim().isEmpty()) {
            Logger.getLogger(EmailManagerEjb.class.getName()).log(Level.SEVERE, "Email Gateway URL is not configured.");
            return false;
        }

        HttpURLConnection connection = null;
        try {
            JSONObject payload = buildEmailJsonPayload(subject, body, recipients, isHtml);

            URL url = new URL(messengerServiceURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.toString().getBytes("UTF-8"));
                os.flush();
            }

            int responseCode = connection.getResponseCode();

            // Always consume the response body to allow connection reuse
            try (InputStream is = connection.getInputStream()) {
                byte[] buffer = new byte[1024];
                while (is.read(buffer) != -1) {
                    // Consume response - code will be added later in seperate issue
                }
            } catch (Exception ex) {
                try (InputStream es = connection.getErrorStream()) {
                    if (es != null) {
                        byte[] buffer = new byte[1024];
                        while (es.read(buffer) != -1) {
                            // Consume response - code will be added later in seperate issue
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            if (responseCode == 200) {
                return true;
            } else {
                Logger.getLogger(EmailManagerEjb.class.getName()).log(Level.SEVERE, "Email sending failed with response code: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            Logger.getLogger(EmailManagerEjb.class.getName()).log(Level.SEVERE, "Exception in sendEmailViaRestGateway: " + e.getMessage(), e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private JSONObject buildEmailJsonPayload(String subject, String body, List<String> recipients, boolean isHtml) {
        final String username = configOptionApplicationController.getShortTextValueByKey("Email Gateway - Username", "");
        final String password = configOptionApplicationController.getShortTextValueByKey("Email Gateway - Password", "");
        final String smtpHost = configOptionApplicationController.getShortTextValueByKey("Email Gateway - SMTP Host", "");
        final String replyTo = configOptionApplicationController.getShortTextValueByKey("Email Gateway - Reply To Email", "");

        JSONObject payload = new JSONObject();
        payload.put("subject", subject);
        payload.put("body", body);
        payload.put("isHtml", isHtml);
        payload.put("replyTo", replyTo);

        JSONArray recipientArray = new JSONArray();
        for (String recipient : recipients) {
            recipientArray.put(recipient);
        }
        payload.put("recipients", recipientArray);

        JSONObject smtpConfig = new JSONObject();
        smtpConfig.put("username", username);
        smtpConfig.put("password", password);
        smtpConfig.put("smtpHost", smtpHost);
        smtpConfig.put("smtpPort", configOptionApplicationController.getIntegerValueByKey("Email Gateway - SMTP Port", 587));
        smtpConfig.put("smtpAuth", configOptionApplicationController.getBooleanValueByKey("Email Gateway - SMTP Auth Enabled", true));
        smtpConfig.put("smtpStarttlsEnable", configOptionApplicationController.getBooleanValueByKey("Email Gateway - StartTLS Enabled", true));
        smtpConfig.put("smtpSslEnable", configOptionApplicationController.getBooleanValueByKey("Email Gateway - SSL Enabled", false));

        payload.put("smtpConfig", smtpConfig);

        return payload;
    }

    private void sendReportApprovalEmails() {
        String j = "Select e from AppEmail e where e.sentSuccessfully=:ret and e.retired=false";
        Map m = new HashMap();
        m.put("ret", false);
        List<AppEmail> emails = getEmailFacade().findByJpql(j, m);
        for (AppEmail e : emails) {
            e.setSentSuccessfully(Boolean.TRUE);
            getEmailFacade().edit(e);

            sendEmail(e.getInstitution().getEmailSendingUsername(),
                    e.getInstitution().getEmailSendingPassword(),
                    e.getSenderEmail(),
                    e.getReceipientEmail(),
                    e.getMessageSubject(),
                    e.getMessageBody(),
                    e.getAttachment1());
        }

    }

    public boolean sendEmail(
            final String senderUsername,
            final String senderPassword,
            String sendingEmail,
            String receipientEmail,
            String subject,
            String messageHtml,
            String attachmentFile1Path) {

        try {
            List<String> recipients = new ArrayList<>();
            recipients.add(receipientEmail);

            // Optional: Warn if attachment path is not null
            if (attachmentFile1Path != null && !attachmentFile1Path.trim().isEmpty()) {
                Logger.getLogger(EmailManagerEjb.class.getName()).log(
                        Level.WARNING,
                        "Attachments are not supported in REST-based email API. Skipping attachment: " + attachmentFile1Path
                );
            }

            return sendEmailViaRestGateway(subject, messageHtml, recipients, true);
        } catch (Exception e) {
            Logger.getLogger(EmailManagerEjb.class.getName()).log(Level.SEVERE, "Legacy sendEmail() failed: " + e.getMessage(), e);
            return false;
        }
    }

    @Deprecated // Will be remove soon
    public boolean sendEmail(
            final String senderUsername,
            final String senderPassword,
            String sendingEmail,
            String receipientEmail,
            String subject,
            String messageHtml,
            String attachmentFile1Path,
            boolean legacyMethod) {
        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
//        Authenticator auth = new SMTPAuthenticator();
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderUsername, senderPassword);
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sendingEmail));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(receipientEmail));
            message.setSubject(subject);

            Multipart multipart = new MimeMultipart();

            BodyPart msbp1 = new MimeBodyPart();
            msbp1.setContent(messageHtml, "text/html; charset=utf-8");
            multipart.addBodyPart(msbp1);

            if (attachmentFile1Path != null) {
                File f = new File(attachmentFile1Path);
                if (f.exists() && !f.isDirectory()) {
                    MimeBodyPart msbp2 = new MimeBodyPart();
                    DataSource source = new FileDataSource(attachmentFile1Path);
                    msbp2.setDataHandler(new DataHandler(source));
                    msbp2.setFileName(attachmentFile1Path);
                    multipart.addBodyPart(msbp2);
                }
            }

            message.setContent(multipart);

            Transport.send(message);
            return true;

        } catch (Exception e) {
            return false;
        }

    }

    // Latest Email Sender
    public boolean sendEmail(final List<String> recipients, final String body, final String subject, final boolean isHtml) {
        try {
            return sendEmailViaRestGateway(subject, body, recipients, isHtml);
        } catch (Exception e) {
            Logger.getLogger(EmailManagerEjb.class.getName()).log(Level.SEVERE, "Failed to send email: " + e.getMessage(), e);
            return false;
        }
    }

    @Deprecated // Will be remove soon
    public boolean sendEmail(final List<String> recipients, final String body, final String subject, final boolean isHtml, boolean legacyMethod) {
        final String username = configOptionApplicationController.getShortTextValueByKey("Email Gateway - Username", "");
        final String password = configOptionApplicationController.getShortTextValueByKey("Email Gateway - Password", "");
        final String smtpHost = configOptionApplicationController.getShortTextValueByKey("Email Gateway - SMTP Host", "");
        final String messengerServiceURL = configOptionApplicationController.getShortTextValueByKey("Email Gateway - URL", "");

        try {
            JSONObject payload = new JSONObject();
            payload.put("subject", subject);
            payload.put("body", body);
            payload.put("isHtml", isHtml);

            JSONArray recipientArray = new JSONArray();
            for (String recipient : recipients) {
                recipientArray.put(recipient);
            }
            payload.put("recipients", recipientArray);

            JSONObject smtpConfig = new JSONObject();
            smtpConfig.put("username", username);
            smtpConfig.put("password", password);
            smtpConfig.put("smtpHost", smtpHost);
            smtpConfig.put("smtpPort", 587);
            smtpConfig.put("smtpAuth", true);
            smtpConfig.put("smtpStarttlsEnable", true);
            smtpConfig.put("smtpSslEnable", false);

            payload.put("smtpConfig", smtpConfig);

            URL url = new URL(messengerServiceURL);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.toString().getBytes());
                os.flush();
            }

            return connection.getResponseCode() == 200;
        } catch (Exception e) {
            Logger.getLogger(EmailManagerEjb.class.getName()).log(
                    Level.SEVERE, "Failed to send email: " + e.getMessage(), e);
            return false;
        }
    }

    public EmailFacade getEmailFacade() {
        return emailFacade;
    }

}
