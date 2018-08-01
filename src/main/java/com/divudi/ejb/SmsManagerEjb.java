/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.ejb;

import com.divudi.entity.Sms;
import com.divudi.facade.EmailFacade;
import com.divudi.facade.SmsFacade;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.swing.JOptionPane;

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

    public String executePost(String targetURL, Map<String, Object> parameters) {
        HttpURLConnection connection = null;
        if (parameters != null && !parameters.isEmpty()) {
            targetURL += "?";
        }
        Set s = parameters.entrySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            Map.Entry m = (Map.Entry) it.next();
            Object pVal = m.getValue();
            String pPara = (String) m.getKey();
            targetURL += pPara + "=" + pVal.toString() + "&";
        }
        if (parameters != null && !parameters.isEmpty()) {
            targetURL += "last=true";
        }
        System.out.println("targetURL = " + targetURL);
        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(false);
            connection.setDoOutput(true);
            //Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
//            wr.writeBytes(urlParameters);
            wr.close();

            //Get Response  
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void sendSms(String number, String message, String username, String password, String sendingAlias) {

        System.out.println("number = " + number);
        System.out.println("message = " + message);
        System.out.println("username = " + username);
        System.out.println("password = " + password);
        System.out.println("sendingAlias = " + sendingAlias);

        Map m = new HashMap();
        m.put("userName", username);
        m.put("password", password);
        m.put("userAlias", sendingAlias);
        m.put("number", number);
        m.put("message", message);

        String res = executePost("http://localhost:8080/sms/faces/index.xhtml", m);
        if (res == null) {
            System.out.println("Error in sending sms");
        } else if (res.toUpperCase().contains("200")) {
            System.out.println("sms sent");
        } else {
            System.out.println("Error in sending sms");
        }

    }

    public SmsFacade getSmsFacade() {
        return smsFacade;
    }

}
