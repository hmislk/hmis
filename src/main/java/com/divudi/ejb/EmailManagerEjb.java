/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.ejb;

import com.divudi.entity.AppEmail;
import com.divudi.facade.EmailFacade;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 *
 * @author Buddhika
 */
@Stateless
public class EmailManagerEjb {

    @EJB
    private EmailFacade emailFacade;

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

        } catch (MessagingException e) {
            return false;
        } catch (Exception e) {
            return false;
        }

    }

    public EmailFacade getEmailFacade() {
        return emailFacade;
    }

}