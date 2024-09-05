package com.divudi.ejb;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
//import lk.mobitel.esms.message.SMSManager;
//import lk.mobitel.esms.session.NullSessionException;
//import lk.mobitel.esms.session.SessionManager;
//import lk.mobitel.esms.test.ServiceTest; // Ensure this import matches your project structure
//import wsdl.SmsMessage;
//import wsdl.User;

/**
 *
 * @author L C J Samarasekara <lawan.chaamindu1234@gmail.com>
 */
@Stateless
public class eSmsManager {
    // Method to send an SMS using ESMSlib
//    public boolean sendSms(String userName, String password, String userAlias, String number, String message) {
//        try {
//            // Create a User object with the username and password
//            User user = new User();
//            user.setUsername(userName);
//            user.setPassword(password);
//
//            // Test the service
//            ServiceTest test = new ServiceTest();
//            String serviceTestResult = test.testService(user);
//            System.out.println("Service Test Result: " + serviceTestResult);
//            
//            // If the service test failed, stop execution
//            if (!"SUCCESS".equalsIgnoreCase(serviceTestResult)) {
//                System.out.println("Service test failed. Exiting...");
//                return false;
//            }
//
//            // Handle session login
//            SessionManager sm = SessionManager.getInstance();
//            sm.login(user);
//            Boolean isLogged = sm.isSession();
//            System.out.println("Login status: " + isLogged);
//
//            // Ensure the session is active before proceeding
//            if (!isLogged) {
//                System.out.println("Login failed. Exiting...");
//                return false;
//            }
//
//            // Create and configure the SMS message
//            SmsMessage msg = new SmsMessage();
//            msg.setMessage(message);
//            System.out.println("message = " + message);
//            msg.setSender(userAlias); // Set the sender alias
//            System.out.println("userAlias = " + userAlias);
//            msg.getRecipients().add(number); // Add the recipient's number
//            msg.setMessageType(1); // 1 for promotional messages, 0 for normal messages
//
//            // Send the SMS
//            SMSManager smsManager = new SMSManager();
//            int result = smsManager.sendMessage(msg);
//            System.out.println("Message Send Result: " + result);
//
//            // Retrieve delivery reports
//            try {
//                List<SmsMessage> deliveryReports = smsManager.getDeliveryReports(userAlias);
//                if (deliveryReports != null && !deliveryReports.isEmpty()) {
//                    System.out.println("Delivery reports retrieved:");
//                    for (SmsMessage report : deliveryReports) {
//                        System.out.println("Report: " + report.getMessage());
//                    }
//                } else {
//                    System.out.println("No delivery reports found.");
//                }
//            } catch (NullSessionException ex) {
////                LOGGER.log(Level.SEVERE, "Failed to retrieve delivery reports", ex);
//            }
//
//            // Logout from the session
//            sm.logout();
//            Boolean isLoggedOut = sm.isSession();
//            System.out.println("Logout status: " + isLoggedOut);
//
//            return true; // SMS sent successfully
//
//        } catch (NullSessionException ex) {
//           // LOGGER.log(Level.SEVERE, "Session error occurred", ex);
//            return false;
//        } catch (Exception ex) {
//           // LOGGER.log(Level.SEVERE, "An error occurred", ex);
//            return false;
//        }
//    }
}
