/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.data.ApplicationInstitution;
import com.divudi.data.SmsType;
import com.divudi.entity.Bill;
import com.divudi.entity.Sms;
import com.divudi.facade.SmsFacade;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author Dushan
 */
@Named(value = "smsController")
@SessionScoped
public class SmsController implements Serializable {
    
    @EJB
    SmsFacade smsFacade;
    
    @Inject
    SessionController sessionController;

    /**
     * Creates a new instance of SmsController
     */
    public SmsController() {
    }

    public void sendSmsToNumberList(String sendingNo, ApplicationInstitution ai, String msg,Bill b,SmsType smsType) {

        if (sendingNo.contains("077") || sendingNo.contains("076")
                || sendingNo.contains("071") || sendingNo.contains("070") 
                || sendingNo.contains("072")
                || sendingNo.contains("075") 
                || sendingNo.contains("078")) {
        } else {
            return;
        }

        if (ai == ApplicationInstitution.Ruhuna) {
            StringBuilder sb = new StringBuilder(sendingNo);
            sb.deleteCharAt(3);
            sendingNo = sb.toString();

            String url = "https://cpsolutions.dialog.lk/index.php/cbs/sms/send?destination=94";
            HttpResponse<String> stringResponse;
            String pw = "&q=14488825498722";

            String messageBody2 = msg;

            System.out.println("messageBody2 = " + messageBody2.length());

            final StringBuilder request = new StringBuilder(url);
            request.append(sendingNo.substring(1, 10));
            request.append(pw);

            try {
                System.out.println("pw = " + pw);
                System.out.println("sendingNo = " + sendingNo);
                System.out.println("sendingNo.substring(1, 10) = " + sendingNo.substring(1, 10));
                System.out.println("text = " + messageBody2);

                stringResponse = Unirest.post(request.toString()).field("message", messageBody2).asString();

            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            
            Sms sms = new Sms();
            sms.setPassword(pw);
            sms.setCreatedAt(new Date());
            sms.setCreater(getSessionController().getLoggedUser());
            sms.setBill(b);
            sms.setSmsType(smsType);
            sms.setSendingUrl(url);
            sms.setSendingMessage(messageBody2);
            getSmsFacade().create(sms);
        }
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public SmsFacade getSmsFacade() {
        return smsFacade;
    }

    public void setSmsFacade(SmsFacade smsFacade) {
        this.smsFacade = smsFacade;
    }
    
}
