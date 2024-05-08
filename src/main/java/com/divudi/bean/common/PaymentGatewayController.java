package com.divudi.bean.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

@Named
@SessionScoped
public class PaymentGatewayController implements Serializable {

    private String merchantId = "TESTSETHMAHOSLKR"; // Actual Merchant ID
    private String apiUsername = "merchant.TESTSETHMAHOSLKR"; // Actual API Username
    private String apiPassword = "49de22fcd8ade9ecb3d81790f3ad152c"; // Actual API Password
    private String paymentStatus;
    private String sessionId;
    private String paymentUrl;

    private final String gatewayUrl = "https://cbcmpgs.gateway.mastercard.com/api/nvp/version/61";

    public String createCheckoutSession() {
        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(gatewayUrl);

        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        try {
            String requestBody = String.format(
                    "apiOperation=CREATE_CHECKOUT_SESSION&apiUsername=%s&apiPassword=%s&merchant=%s"
                    + "&order.id=%s&order.amount=%s&order.currency=%s&order.description=%s&interaction.operation=%s"
                    + "&interaction.returnUrl=%s&interaction.merchant.name=%s",
                    apiUsername, apiPassword, merchantId,
                    "12345", "100.00", "LKR", "Medical services at Sethma Hospital", "PURCHASE",
                    "http://localhost:8080/returnPage", "Sethma Hospital");

            post.setEntity(new StringEntity(requestBody));
            HttpResponse response = client.execute(post);
            String responseString = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() == 200) {
                sessionId = extractSessionId(responseString);
                if (sessionId != null) {
                    return "/pay?faces-redirect=true"; // Use JSF navigation to redirect to the /pay page
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Stay on the same page if the session creation fails
    }

    private String constructPaymentUrl(String sessionId) {
        return "https://cbcmpgs.gateway.mastercard.com/checkout/version/61/checkout.js?session.id=" + sessionId;
    }

    private String extractSessionId(String response) {
        Map<String, String> responseMap = parseUrlEncodedResponse(response);
        return responseMap.get("session.id");
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
