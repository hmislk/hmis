/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.entity.AppEmail;
import com.divudi.core.facade.EmailFacade;
import com.divudi.ejb.EmailManagerEjb;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.util.CommonFunctions;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.*;

@Named
@SessionScoped
public class EmailController implements Serializable {

    @EJB
    private EmailFacade emailFacade;

    @Inject
    private EmailManagerEjb emailManager;

    @Inject
    private SessionController sessionController;

    private List<AppEmail> emails;
    private List<AppEmail> failedEmails;

    private AppEmail selectedEmail;

    private String recipient;
    private String subject;
    private String body;
    private String output;

    private Date fromDate;
    private Date toDate;

    public void fillAllEmails() {
        String j = "select e from AppEmail e where e.createdAt between :fd and :td";
        Map<String, Object> m = new HashMap<>();
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        emails = emailFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
    }

    public void fillFailedEmails() {
        String j = "select e from AppEmail e where e.sentSuccessfully <> :suc and e.createdAt between :fd and :td";
        Map<String, Object> m = new HashMap<>();
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("suc", true);
        failedEmails = emailFacade.findByJpql(j, m, TemporalType.TIMESTAMP);
    }

    public void sendManualEmail() {
        if (recipient == null || recipient.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Recipient email is required.");
            return;
        }

        List<String> recipients = new ArrayList<>();
        recipients.add(recipient.trim());

        boolean success = emailManager.sendEmail(recipients, body, subject, true);

        if (success) {
            JsfUtil.addSuccessMessage("Email sent successfully");

            AppEmail e = new AppEmail();
            e.setReceipientEmail(recipient.trim());
            e.setMessageSubject(subject);
            e.setMessageBody(body);
            e.setSentSuccessfully(true);
            e.setCreatedAt(new Date());
            e.setCreater(sessionController.getLoggedUser());
            emailFacade.create(e);

            // Clear form
            recipient = null;
            subject = null;
            body = null;
        } else {
            JsfUtil.addErrorMessage("Failed to send email");
        }
    }

    public void resendSelectedEmail() {
        if (selectedEmail == null) {
            JsfUtil.addErrorMessage("No Email selected");
            return;
        }

        List<String> recipients = new ArrayList<>();
        recipients.add(selectedEmail.getReceipientEmail());

        boolean success = emailManager.sendEmail(recipients, selectedEmail.getMessageBody(), selectedEmail.getMessageSubject(), true);

        selectedEmail.setSentSuccessfully(success);
        selectedEmail.setSentAt(new Date());
        emailFacade.edit(selectedEmail);

        if (success) {
            JsfUtil.addSuccessMessage("Email resent successfully");
        } else {
            JsfUtil.addErrorMessage("Failed to resend email");
        }
    }

    public String navigateToEmailList() {
        return "/analytics/email_list?faces-redirect=true";
    }

    public String navigateToFailedEmailList() {
        return "/analytics/email_failed_list?faces-redirect=true";
    }

    public String navigateToSendEmail() {
        return "/analytics/email_send?faces-redirect=true";
    }

    // ---------------- Getters & Setters ---------------- //
    public List<AppEmail> getEmails() {
        return emails;
    }

    public void setEmails(List<AppEmail> emails) {
        this.emails = emails;
    }

    public List<AppEmail> getFailedEmails() {
        return failedEmails;
    }

    public void setFailedEmails(List<AppEmail> failedEmails) {
        this.failedEmails = failedEmails;
    }

    public AppEmail getSelectedEmail() {
        return selectedEmail;
    }

    public void setSelectedEmail(AppEmail selectedEmail) {
        this.selectedEmail = selectedEmail;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public EmailFacade getEmailFacade() {
        return emailFacade;
    }

    public void setEmailFacade(EmailFacade emailFacade) {
        this.emailFacade = emailFacade;
    }

    public EmailManagerEjb getEmailManager() {
        return emailManager;
    }

    public void setEmailManager(EmailManagerEjb emailManager) {
        this.emailManager = emailManager;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }
}
