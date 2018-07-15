/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import java.util.Properties;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 *
 * @author Buddhika
 */
@Stateless
public class EmailManagerEjb {

    final static String USERNAME = "arogyafirst@gmail.com";
    final static String PASSWORD = "arogya123@";
    static Session session = null;

    @SuppressWarnings("unused")
//    @Schedule(minute = "1", second = "1", dayOfMonth = "*", month = "*", year = "*", hour = "1", persistent = false)
    @Schedule(minute = "21", second = "59", dayOfMonth = "*", month = "*", year = "*", hour = "*", persistent = false)
//    @Schedule(minute = "59", second = "59", hour = "23", dayOfMonth = "Last", info = "2nd Scheduled Timer", persistent = false)
//    @Schedule(second="*/1", minute="*",hour="*", persistent=false)
    public void myTimer() {
        long userCount = 0;
        String errorMessage;
        try {

            sendEmail1("sunila.soft@gmail.com", "Arogya is OK", "Arogya is OK");
        } catch (Exception e) {
            errorMessage = e.getMessage();
            sendEmail1("Error in DE", errorMessage);
        }
        System.out.println("userCount = " + userCount);
    }

    public void sendEmail1(String messageHeading, String messageBody) {
        sendEmail1("buddhika.ari@gmail.com", messageHeading, messageBody);
    }

    public void sendEmail1(String toEmail, String messageHeading, String messageBody) {
        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        if (session == null) {
            session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(USERNAME, PASSWORD);
                }
            });
        }
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(toEmail));
            message.setSubject(messageHeading);
            message.setText(messageBody);
//            BodyPart msbp1 = new MimeBodyPart();
//            msbp1.setText("Final Lab report of patient");

//            MimeBodyPart msbp2 = new MimeBodyPart();
//            DataSource source = new FileDataSource("LabReport.pdf");
//            msbp2.setDataHandler(new DataHandler(source));
//            msbp2.setFileName("/Labreport.pdf");
//            Multipart multipart = new MimeMultipart();
//            multipart.addBodyPart(msbp1);
//            multipart.addBodyPart(msbp2);
//            message.setContent(multipart);
            Transport.send(message);

            System.out.println("Send Successfully");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

    }

}
