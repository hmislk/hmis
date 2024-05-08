package com.divudi.bean.common;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

/**
 * PaymentGatewayController manages payment sessions and interactions with a
 * payment gateway.
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@RequestScoped
public class PaymentGatewayController {

    private String merchantId = "TESTSETHMAHOSLKR"; // Sample merchant ID, replace with real one
    private String apiUsername = "merchant.TESTSETHMAHOSLKR"; // Sample API username
    private String apiPassword = "49de22fcd8ade9ecb3d81790f3ad152c"; // Sample API password
    private String paymentStatus;
    private String sessionId; // This will hold the session ID across requests
    private String paymentUrl; // To store the URL for redirection

    private final String gatewayUrl = "https://cbcmpgs.gateway.mastercard.com/api/nvp/version/61";

    public String createCheckoutSession() {
        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(gatewayUrl);

        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        try {
            String requestBody = String.format(
                    "apiOperation=CREATE_CHECKOUT_SESSION&apiUsername=%s&apiPassword=%s&merchant=%s"
                    + "&order.id=%s&order.amount=%s&order.currency=%s&interaction.operation=%s"
                    + "&interaction.returnUrl=%s&interaction.merchant.name=%s",
                    apiUsername, apiPassword, merchantId,
                    "12345", "100.00", "LKR", "PURCHASE",
                    "http://localhost:8080/sethma1", "Sethma");

            post.setEntity(new StringEntity(requestBody));
            HttpResponse response = client.execute(post);
            String responseString = EntityUtils.toString(response.getEntity());

            System.out.println("responseString = " + responseString);

            if (response.getStatusLine().getStatusCode() == 200) {
                sessionId = extractSessionId(responseString);
                if (sessionId != null) {
                    paymentUrl = constructPaymentUrl(sessionId); // Store the URL in the bean
                    FacesContext.getCurrentInstance().getExternalContext().redirect(paymentUrl); // Redirect directly
                    return null; // JSF navigation should be null to stay on the same page
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "error"; // Return to an error page or similar
    }

    private String extractSessionId(String response) {
        Map<String, String> responseMap = parseUrlEncodedResponse(response);
        if (responseMap.containsKey("session.id")) {
            return responseMap.get("session.id");
        }
        return null; // Return null if the session ID is not found
    }

    private Map<String, String> parseUrlEncodedResponse(String response) {
        Map<String, String> responseMap = new HashMap<>();
        String[] pairs = response.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx != -1) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                responseMap.put(key, value);
            }
        }
        return responseMap;
    }

    private String constructPaymentUrl(String sessionId) {
        // Update this URL to the actual URL provided by the payment gateway for redirecting the user
        return "https://cbcmpgs.gateway.mastercard.com/api/rest/version/61/session/" + sessionId;
    }

    // Getters and Setters
    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

}
