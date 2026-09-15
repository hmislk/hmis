/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.SecurityController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.MessageType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Sms;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.SmsFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.SmsManagerEjb;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Backs the "Send SMS" dialog on the Purchase Order pages: builds a
 * permanent, tamper-proof public link to the PO (viewable without login by
 * the supplier) and sends it via SMS. Related issue: #23811.
 */
@Named
@SessionScoped
public class PurchaseOrderSmsController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;
    @Inject
    private SecurityController securityController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    @EJB
    private BillFacade billFacade;
    @EJB
    private SmsFacade smsFacade;
    @EJB
    private SmsManagerEjb smsManagerEjb;

    private Bill bill;
    private String smsNumber;
    private String smsMessage;
    private String supplierName;

    public void prepareSmsDialog(Long billId) {
        bill = null;
        smsNumber = "";
        smsMessage = "";
        supplierName = "";

        if (billId == null) {
            JsfUtil.addErrorMessage("No Purchase Order selected");
            return;
        }

        Bill b = billFacade.find(billId);
        if (b == null) {
            JsfUtil.addErrorMessage("Purchase Order not found");
            return;
        }
        // The public link page only serves approved POs, so don't send a link that can't open
        if (b.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_ORDER_APPROVAL) {
            JsfUtil.addErrorMessage("Only approved purchase orders can be sent by SMS");
            return;
        }

        bill = b;
        Institution toInstitution = bill.getToInstitution();
        supplierName = toInstitution != null && toInstitution.getName() != null ? toInstitution.getName() : "";

        if (toInstitution != null) {
            String mobile = toInstitution.getMobile() != null ? toInstitution.getMobile().trim() : "";
            if (!mobile.isEmpty()) {
                smsNumber = mobile;
            } else {
                String phone = toInstitution.getPhone() != null ? toInstitution.getPhone().trim() : "";
                smsNumber = phone;
            }
        } else {
            smsNumber = "";
        }

        smsMessage = buildMessage(bill);
    }

    public void sendSms() {
        if (bill == null) {
            JsfUtil.addErrorMessage("No Purchase Order selected");
            return;
        }

        String number = smsNumber != null ? smsNumber.replaceAll("\\s+", "") : "";
        if (number.isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a mobile number");
            return;
        }
        if (!number.matches("\\+?[0-9]{9,15}")) {
            JsfUtil.addErrorMessage("Please enter a valid mobile number");
            return;
        }

        if (smsMessage == null || smsMessage.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a message");
            return;
        }

        Sms sms = new Sms();
        sms.setPending(true);
        sms.setCreatedAt(new Date());
        sms.setCreater(sessionController.getLoggedUser());
        sms.setReceipientNumber(number);
        sms.setSendingMessage(smsMessage);
        sms.setSmsType(MessageType.PurchaseOrderSms);
        sms.setBill(bill);
        sms.setInstitution(sessionController.getInstitution());
        sms.setDepartment(sessionController.getDepartment());
        smsFacade.create(sms);

        boolean success = smsManagerEjb.sendSms(sms);

        sms.setSentSuccessfully(success);
        sms.setPending(!success);
        sms.setSendingFailed(!success);
        if (success) {
            sms.setSentAt(new Date());
        }
        smsFacade.edit(sms);

        if (success) {
            JsfUtil.addSuccessMessage("SMS sent to " + number);
        } else {
            JsfUtil.addErrorMessage("SMS could not be sent. Please check the SMS gateway configuration.");
        }
    }

    public String buildPublicLink(Bill b) {
        if (b == null || b.getId() == null) {
            return "";
        }
        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.YEAR, 10);

        String token = securityController.createBillToken(b.getId(), expiry.getTime(), securityController.obtainHmacSigningKey(sessionController));
        String encodedToken;
        try {
            encodedToken = URLEncoder.encode(token, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            encodedToken = token;
        }
        return CommonFunctions.getBaseUrl() + "faces/requests/po.xhtml?id=" + encodedToken;
    }

    private String buildMessage(Bill b) {
        String template = configOptionApplicationController.getLongTextValueByKey(
                "Pharmacy PO - SMS Template",
                "Purchase Order {po_no} from {institution} - value {net_total}. View online: {link}");
        if (template == null || template.trim().isEmpty()) {
            template = "Purchase Order {po_no} from {institution} - value {net_total}. View online: {link}";
        }

        String poNo = b.getDeptId() != null ? b.getDeptId() : (b.getInsId() != null ? b.getInsId() : "");

        String institutionName = "";
        if (b.getInstitution() != null && b.getInstitution().getName() != null) {
            institutionName = b.getInstitution().getName();
        } else {
            Department department = b.getDepartment();
            if (department != null && department.getInstitution() != null && department.getInstitution().getName() != null) {
                institutionName = department.getInstitution().getName();
            }
        }

        Institution toInstitution = b.getToInstitution();
        String supplier = toInstitution != null && toInstitution.getName() != null ? toInstitution.getName() : "";

        DecimalFormat df = new DecimalFormat("#,##0.00");
        String netTotal = df.format(b.getNetTotal());

        String link = buildPublicLink(b);

        return template.replace("{po_no}", poNo)
                .replace("{institution}", institutionName)
                .replace("{supplier}", supplier)
                .replace("{net_total}", netTotal)
                .replace("{link}", link != null ? link : "");
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public void setSmsNumber(String smsNumber) {
        this.smsNumber = smsNumber;
    }

    public String getSmsMessage() {
        return smsMessage;
    }

    public void setSmsMessage(String smsMessage) {
        this.smsMessage = smsMessage;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }
}
