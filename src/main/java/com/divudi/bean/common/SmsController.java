/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.data.ApplicationInstitution;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 *
 * @author Dushan
 */
@Named(value = "smsController")
@SessionScoped
public class SmsController implements Serializable {

    /**
     * Creates a new instance of SmsController
     */
    public SmsController() {
    }

    public void sendSmsToNumberList(String sendingNo, ApplicationInstitution ai, String msg) {

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
        }
    }

}
