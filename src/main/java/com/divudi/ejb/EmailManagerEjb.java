/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.facade.EmailFacade;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
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

    @SuppressWarnings("unused")
    @Schedule(second = "59", minute = "*/2", hour = "*", persistent = false)
    public void myTimer() {
//        sendReportApprovalEmails();

    }

    private void sendReportApprovalEmails() {
        String j = "Select e from AppEmail e where e.sentSuccessfully=:ret and e.retired=false";
        Map m = new HashMap();
        m.put("ret", false);
        List<AppEmail> emails = getEmailFacade().findByJpql(j,m);
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
