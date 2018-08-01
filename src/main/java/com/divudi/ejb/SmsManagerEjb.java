/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import com.divudi.entity.AppEmail;
import com.divudi.entity.Sms;
import com.divudi.facade.EmailFacade;
import com.divudi.facade.SmsFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.File;
import java.util.Date;
import java.util.List;
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
import lk.mobitel.esms.session.*;
import lk.mobitel.esms.message.*;
import lk.mobitel.esms.ws.*;

/**
 *
 * @author Buddhika
 */
@Stateless
public class SmsManagerEjb {

    @EJB
    private EmailFacade emailFacade;
    @EJB
    SmsFacade smsFacade;

    @SuppressWarnings("unused")
    @Schedule(second = "19", minute = "*/5", hour = "*", persistent = false)
    public void myTimer() {
        sendSmsAwaitingToSendInDatabase();
    }

    private void sendSmsAwaitingToSendInDatabase() {
        String j = "Select e from Sms e where e.sentSuccessfully=false and e.retired=false";
        List<Sms> smses = getSmsFacade().findBySQL(j);
//        if (false) {
//            Sms e = new Sms();
//            e.getSentSuccessfully();
//            e.getInstitution();
//        }
        for (Sms e : smses) {
            e.setSentSuccessfully(Boolean.TRUE);
            getSmsFacade().edit(e);

            sendSms(e.getReceipientNumber(), e.getSendingMessage(),
                    e.getInstitution().getSmsSendingUsername(),
                    e.getInstitution().getSmsSendingPassword(),
                    e.getInstitution().getSmsSendingAlias());
            e.setSentSuccessfully(true);
            e.setSentAt(new Date());
            getSmsFacade().edit(e);
        }

    }

    public void sendSms(String number, String message, String username, String password, String sendingAlias) {
        //create user object
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);

        //initiate session
        SessionManager sm = SessionManager.getInstance();
        System.out.println("Logging in.....");
        if (!sm.isSession()) {
            sm.login(user);
        }
        System.out.println("Logged in!");

        //create alias obj
        Alias alias = new Alias();
        alias.setAlias(sendingAlias);

        //create SmsMessage object 
        SmsMessage msg = new SmsMessage();
        msg.setMessage(message);
        System.out.println("Message length: " + msg.getMessage().length());
        msg.setSender(alias);
        msg.getRecipients().add(number);

        //send sms
        try {
            SMSManager smsMgr = new SMSManager();
            System.out.println("Sending......");
            int ret = smsMgr.sendMessage(msg);
            System.out.println("Sent! " + ret);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public SmsFacade getSmsFacade() {
        return smsFacade;
    }

}
