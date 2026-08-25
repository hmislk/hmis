/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.lab.LabTestHistoryController;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.facade.EmailFacade;
import com.divudi.core.facade.PatientReportFacade;
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
    private List<AppEmail> admissionEmails;

    private AppEmail selectedEmail;
    private PatientEncounter patientEncounter;
    private Institution creditCompanyFilter;
    private String patientSearchTerm;

    private String recipient;
    private String subject;
    private String body;
    private String output;

    private Date fromDate;
    private Date toDate;

    public void fillAllEmails() {
        // Every association below is walked via explicit LEFT JOIN. A plain
        // path expression (e.encounterCreditCompany.patientEncounter.bhtNo)
        // is an implicit INNER join, and since there is only one shared FROM
        // clause per query, an inner join anywhere in it drops rows where
        // that association is null - even when the WHERE clause only needs
        // it inside an OR branch. This bit us once already in
        // fillEmailsForAdmission (see the comment there).
        StringBuilder j = new StringBuilder("select e from AppEmail e "
                + "left join e.patientEncounter pe "
                + "left join pe.patient pep "
                + "left join pep.person pepp "
                + "left join e.encounterCreditCompany ecc "
                + "left join ecc.patientEncounter eccpe "
                + "left join eccpe.patient eccpep "
                + "left join eccpep.person eccpepp "
                + "where e.createdAt between :fd and :td");
        Map<String, Object> m = new HashMap<>();
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        if (creditCompanyFilter != null) {
            j.append(" and ecc.institution=:cc");
            m.put("cc", creditCompanyFilter);
        }
        if (patientSearchTerm != null && !patientSearchTerm.trim().isEmpty()) {
            j.append(" and (pe.bhtNo like :ps or eccpe.bhtNo like :ps or "
                    + "pepp.name like :ps or eccpepp.name like :ps)");
            m.put("ps", "%" + patientSearchTerm.trim() + "%");
        }
        emails = emailFacade.findByJpql(j.toString(), m, TemporalType.TIMESTAMP);
    }

    /**
     * Admission-scoped, unfiltered: every AppEmail tied to this admission,
     * either directly (patientEncounter) or via a credit-company send
     * (encounterCreditCompany.patientEncounter), with no date restriction.
     */
    public void fillEmailsForAdmission(PatientEncounter pe) {
        this.patientEncounter = pe;
        if (pe == null || pe.getId() == null) {
            admissionEmails = new ArrayList<>();
            return;
        }
        // Compare by id, not by entity reference - patientEncounter is
        // usually handed in as an Admission (a PatientEncounter subtype),
        // and JPQL entity-parameter equality across sub/supertype instances
        // in a joined-inheritance model is not reliable.
        // Both associations must be explicit LEFT JOINs: a plain path
        // expression (e.encounterCreditCompany.patientEncounter.id) is an
        // implicit INNER join, so when encounterCreditCompany is null on a
        // directly-sent email, that inner join drops the row entirely even
        // though the OR's other branch (e.patientEncounter.id) matches.
        String j = "select e from AppEmail e "
                + "left join e.patientEncounter pe "
                + "left join e.encounterCreditCompany ecc "
                + "left join ecc.patientEncounter eccpe "
                + "where pe.id=:peId or eccpe.id=:peId order by e.createdAt desc";
        Map<String, Object> m = new HashMap<>();
        m.put("peId", pe.getId());
        admissionEmails = emailFacade.findByJpql(j, m);
    }

    public String navigateToAdmissionSentEmails(PatientEncounter pe) {
        if (pe == null) {
            JsfUtil.addErrorMessage("No Admission Selected");
            return "";
        }
        fillEmailsForAdmission(pe);
        return "/inward/admission_sent_emails?faces-redirect=true";
    }

    public String navigateToEmailDetail() {
        if (selectedEmail == null) {
            JsfUtil.addErrorMessage("No Email Selected");
            return "";
        }
        return "/analytics/email_view?faces-redirect=true";
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

        boolean success = false;
        try {
            success = emailManager.sendEmail(recipients, body, subject, true);
        } catch (Exception e) {
            JsfUtil.addErrorMessage("An error occurred while sending the email: " + e.getMessage());
            return;
        }

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

    @EJB
    PatientReportFacade patientReportFacade;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    LabTestHistoryController labTestHistoryController;

    public void resendSelectedEmail() {
        if (selectedEmail == null) {
            JsfUtil.addErrorMessage("No Email selected");
            return;
        }

        List<String> recipients = new ArrayList<>();
        recipients.add(selectedEmail.getReceipientEmail());

        boolean success = emailManager.sendEmail(recipients, selectedEmail.getMessageBody(), selectedEmail.getMessageSubject(), true);

        if (success) {
            selectedEmail.setSentSuccessfully(success);
            selectedEmail.setSentAt(new Date());
            emailFacade.edit(selectedEmail);

            PatientReport courrentPr = selectedEmail.getPatientReport();
            courrentPr.setSendEmailComplete(true);
            patientReportFacade.edit(courrentPr);

            if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
                labTestHistoryController.addReportSentEmailHistory(selectedEmail.getPatientInvestigation(), selectedEmail.getPatientReport(), selectedEmail);
            }

            JsfUtil.addSuccessMessage("Email resent successfully");
        } else {
            if (configOptionApplicationController.getBooleanValueByKey("Lab Test History Enabled", false)) {
                labTestHistoryController.addResentFailureEmailHistory(selectedEmail.getPatientInvestigation(), selectedEmail.getPatientReport(), selectedEmail);
            }
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

    public List<AppEmail> getAdmissionEmails() {
        return admissionEmails;
    }

    public void setAdmissionEmails(List<AppEmail> admissionEmails) {
        this.admissionEmails = admissionEmails;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Institution getCreditCompanyFilter() {
        return creditCompanyFilter;
    }

    public void setCreditCompanyFilter(Institution creditCompanyFilter) {
        this.creditCompanyFilter = creditCompanyFilter;
    }

    public String getPatientSearchTerm() {
        return patientSearchTerm;
    }

    public void setPatientSearchTerm(String patientSearchTerm) {
        this.patientSearchTerm = patientSearchTerm;
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

    public EmailManagerEjb getEmailManager() {
        return emailManager;
    }

    public SessionController getSessionController() {
        return sessionController;
    }
}
