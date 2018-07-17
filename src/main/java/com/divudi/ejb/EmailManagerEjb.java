/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.PersonInstitutionType;
import com.divudi.entity.AppEmail;
import com.divudi.entity.Department;
import com.divudi.entity.FeeChange;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Staff;
import com.divudi.entity.channel.ArrivalRecord;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.entity.pharmacy.StockHistory;
import com.divudi.facade.AmpFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.EmailFacade;
import com.divudi.facade.FeeChangeFacade;
import com.divudi.facade.FingerPrintRecordFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PharmaceuticalItemFacade;
import com.divudi.facade.ServiceSessionFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.facade.StockFacade;
import com.divudi.facade.StockHistoryFacade;
import com.divudi.facade.util.JsfUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
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
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Stateless
public class EmailManagerEjb {

    @EJB
    private EmailFacade emailFacade;
    
    
    
    @SuppressWarnings("unused")
    @Schedule(second = "59", minute = "*/5", hour = "*", persistent = false)
    public void myTimer() {
        System.err.println("Timer ticked " + new Date());
        sendReportApprovalEmails();

    }

    private void sendReportApprovalEmails() {
        String j = "Select e from AppEmail e where e.sentSuccessfully=false and e.retired=false";
        List<AppEmail> emails = getEmailFacade().findBySQL(j);
//        if (false) {
//            AppEmail e = new AppEmail();
//            e.getSentSuccessfully();
//            e.getInstitution();
//        }
        for(AppEmail e:emails){
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
                MimeBodyPart msbp2 = new MimeBodyPart();
                DataSource source = new FileDataSource(attachmentFile1Path);
                msbp2.setDataHandler(new DataHandler(source));
                msbp2.setFileName(attachmentFile1Path);
                multipart.addBodyPart(msbp2);
            }

            message.setContent(multipart);

            Transport.send(message);
            System.err.println("Email send successfully");
            return true;

        } catch (MessagingException e) {
            JsfUtil.addErrorMessage("Error. " + e.getMessage());
            return false;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error. " + e.getMessage());
            return false;
        }

    }

    public EmailFacade getEmailFacade() {
        return emailFacade;
    }
    
    
    

}
