package com.divudi.bean.common;

import com.divudi.entity.Patient;
import com.divudi.entity.channel.SessionInstance;
import com.divudi.bean.common.CommonController;
import com.divudi.entity.PaymentGatewayTransaction;
import com.divudi.facade.PaymentGatewayTransactionFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.inject.Inject;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

@Named
@SessionScoped
public class PaymentGatewayController implements Serializable {

    @EJB
    PaymentGatewayTransactionFacade paymentGatewayTransactionFacade;

    @Inject
    CommonController commonController;
    @Inject
    SessionController sessionController;

    private String merchantId = "TESTSETHMAHOSLKR"; // Actual Merchant ID
    private String apiUsername = "merchant.TESTSETHMAHOSLKR"; // Actual API Username
    private String apiPassword = "49de22fcd8ade9ecb3d81790f3ad152c"; // Actual API Password
    private String paymentStatus;
    private String sessionId;
    private String paymentUrl;
    private String orderAmount;
    private String orderId;
    private String successUrl;
    private String templateForOrderDescription;
    private SessionInstance selectedSessioninstance;
    private Patient patient;
    private String returnUrl;
    private String orderStatus;
    private PaymentGatewayTransaction newPaymentGatewayTransaction;

    private final String gatewayUrl = "https://cbcmpgs.gateway.mastercard.com/api/nvp/version/61";

    public void resetOrderStatus() {
        orderId=null;
        orderStatus = null;
        
    }

    public void generateTemplateForOrderDescription() {
        StringBuilder template = new StringBuilder();
        if (selectedSessioninstance == null) {
            templateForOrderDescription = "";
            return;
        }
        template.append("Doctor: ").append(selectedSessioninstance.getOriginatingSession().getStaff().getPerson().getNameWithTitle()).append("\n");
        template.append(" - Session: ").append(selectedSessioninstance.getName()).append("\n\n");
        templateForOrderDescription = template.toString();
    }

    public String createCheckoutSession() {
        generateTemplateForOrderDescription();
        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(gatewayUrl);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");

        newPaymentGatewayTransaction = new PaymentGatewayTransaction();
        newPaymentGatewayTransaction.setSessionInstance(selectedSessioninstance);
        newPaymentGatewayTransaction.setCreater(sessionController.getLoggedUser());
        newPaymentGatewayTransaction.setCreatedAt(new Date());
        paymentGatewayTransactionFacade.create(newPaymentGatewayTransaction);

        try {
            String requestBody = String.format(
                    "apiOperation=CREATE_CHECKOUT_SESSION&apiUsername=%s&apiPassword=%s&merchant=%s"
                    + "&order.id=%s&order.amount=%s&order.currency=%s&order.description=%s&interaction.operation=%s"
                    + "&interaction.returnUrl=%s&interaction.merchant.name=%s",
                    apiUsername, apiPassword, merchantId,
                    newPaymentGatewayTransaction.getIdStr(), orderAmount, "LKR", templateForOrderDescription.toString(), "PURCHASE",
                    commonController.getBaseUrl() + "faces/channel/patient_portal.xhtml", "Sethma");
            post.setEntity(new StringEntity(requestBody));
            HttpResponse response = client.execute(post);
            String responseString = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() == 200) {
                System.out.println("responseString = " + responseString);
                sessionId = extractSessionId(responseString);
                System.out.println("sessionId = " + sessionId);
                if (sessionId != null) {
                    return "/patient_portal_pay?faces-redirect=true";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String checkPaymentStatus() {
        String status = null;
        HttpClient client = HttpClients.createDefault();
        HttpPost post = new HttpPost(gatewayUrl);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        try {
            String requestBody = String.format(
                    "apiOperation=RETRIEVE_ORDER&apiPassword=%s"
                    + "&apiUsername=%s&merchant=%s&order.id=%s",
                    apiPassword, apiUsername, merchantId,
                    newPaymentGatewayTransaction.getIdStr());
            post.setEntity(new StringEntity(requestBody));
            HttpResponse response = client.execute(post);
            String responseString = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() == 200) {
                System.out.println("status = " + extractStatusCode(responseString));
                orderStatus = extractStatusCode(responseString);
                System.out.println("orderStatus = " + orderStatus);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    private String constructPaymentUrl(String sessionId) {
        return "https://cbcmpgs.gateway.mastercard.com/checkout/version/61/checkout.js?session.id=" + sessionId;
    }

    private String extractStatusCode(String response) {
        Map<String, String> responseMap = parseUrlEncodedResponse(response);
        return responseMap.get("result");
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

    public String getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(String orderAmount) {
        this.orderAmount = orderAmount;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }

    public String getTemplateForOrderDescription() {
        return templateForOrderDescription;
    }

    public void setTemplateForOrderDescription(String templateForOrderDescription) {
        this.templateForOrderDescription = templateForOrderDescription;
    }

    public SessionInstance getSelectedSessioninstance() {
        return selectedSessioninstance;
    }

    public void setSelectedSessioninstance(SessionInstance selectedSessioninstance) {
        this.selectedSessioninstance = selectedSessioninstance;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public PaymentGatewayTransaction getNewPaymentGatewayTransaction() {
        return newPaymentGatewayTransaction;
    }

    public void setNewPaymentGatewayTransaction(PaymentGatewayTransaction newPaymentGatewayTransaction) {
        this.newPaymentGatewayTransaction = newPaymentGatewayTransaction;
    }

}
