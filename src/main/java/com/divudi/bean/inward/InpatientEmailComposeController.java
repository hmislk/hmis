/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.EmailAttachment;
import com.divudi.core.data.EmailRecipientCandidate;
import com.divudi.core.data.MessageType;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.EncounterCreditCompany;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.Upload;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.entity.clinical.DocumentTemplate;
import com.divudi.core.entity.inward.PatientFormEntry;
import com.divudi.core.facade.DoctorFacade;
import com.divudi.core.facade.DocumentTemplateFacade;
import com.divudi.core.facade.ClinicalFindingValueFacade;
import com.divudi.core.facade.EmailFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.PatientFormEntryFacade;
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.facade.UploadFacade;
import com.divudi.core.data.clinical.DocumentTemplateType;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.EmailManagerEjb;
import org.apache.commons.io.IOUtils;
import org.primefaces.model.file.UploadedFile;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generalized inpatient "Send Email" compose page: reachable from the
 * Inpatient Dashboard (blank recipients) and from a credit-company row on
 * Edit Admission (recipient prefilled from that company's contact). Adds
 * free-form To/CC/BCC recipient search, saved-document attachments, and
 * template selection on top of the single-recipient flow this replaces.
 */
@Named
@SessionScoped
public class InpatientEmailComposeController implements Serializable {

    private static final long MANUAL_ATTACHMENT_SIZE_LIMIT = 10240000;
    private static final String MANUAL_ATTACHMENT_ALLOWED_TYPES_REGEX = "(?i)\\.(pdf|jpeg|jpg|png)$";

    @Inject
    private SessionController sessionController;
    @Inject
    private InwardDocumentUploadController inwardDocumentUploadController;
    @Inject
    private InpatientClinicalDataController inpatientClinicalDataController;
    @Inject
    private InwardFormController inwardFormController;

    @EJB
    private EmailFacade emailFacade;
    @EJB
    private EmailManagerEjb emailManagerEjb;
    @EJB
    private UploadFacade uploadFacade;
    @EJB
    private ClinicalFindingValueFacade clinicalFindingValueFacade;
    @EJB
    private PatientFormEntryFacade patientFormEntryFacade;
    @EJB
    private DocumentTemplateFacade documentTemplateFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    @EJB
    private DoctorFacade doctorFacade;
    @EJB
    private StaffFacade staffFacade;

    private PatientEncounter patientEncounter;
    private EncounterCreditCompany currentEncounterCreditCompany;

    private List<EmailRecipientCandidate> toRecipients;
    private List<EmailRecipientCandidate> ccRecipients;
    private List<EmailRecipientCandidate> bccRecipients;

    private String subject;
    private String emailBody;
    private DocumentTemplate selectedTemplate;
    private List<DocumentTemplate> emailTemplates;

    private List<Upload> encounterUploads;
    private List<ClinicalFindingValue> encounterLetters;
    private List<PatientFormEntry> encounterForms;

    private List<Upload> selectedUploads;
    private List<ClinicalFindingValue> selectedLetters;
    private List<PatientFormEntry> selectedForms;

    private UploadedFile manualAttachment;

    public String startComposeForAdmission(PatientEncounter e) {
        if (e == null) {
            JsfUtil.addErrorMessage("No Admission Selected");
            return "";
        }
        resetState();
        patientEncounter = e;
        loadAttachableDocuments();
        loadEmailTemplates();
        return "/inward/inpatient_send_email?faces-redirect=true";
    }

    public String startComposeForCreditCompany(EncounterCreditCompany ecc) {
        if (ecc == null || ecc.getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("No Admission to Email");
            return "";
        }
        resetState();
        patientEncounter = ecc.getPatientEncounter();
        currentEncounterCreditCompany = ecc;
        if (ecc.getInstitution() != null && ecc.getInstitution().getEmail() != null
                && !ecc.getInstitution().getEmail().trim().isEmpty()) {
            toRecipients.add(new EmailRecipientCandidate(
                    ecc.getInstitution().getName(), ecc.getInstitution().getEmail().trim(), "Institution"));
        }
        loadAttachableDocuments();
        loadEmailTemplates();
        return "/inward/inpatient_send_email?faces-redirect=true";
    }

    private void resetState() {
        patientEncounter = null;
        currentEncounterCreditCompany = null;
        toRecipients = new ArrayList<>();
        ccRecipients = new ArrayList<>();
        bccRecipients = new ArrayList<>();
        subject = null;
        emailBody = null;
        selectedTemplate = null;
        selectedUploads = new ArrayList<>();
        selectedLetters = new ArrayList<>();
        selectedForms = new ArrayList<>();
        manualAttachment = null;
    }

    private void loadAttachableDocuments() {
        if (patientEncounter == null) {
            encounterUploads = new ArrayList<>();
            encounterLetters = new ArrayList<>();
            encounterForms = new ArrayList<>();
            return;
        }
        Map<String, Object> m = new HashMap<>();
        m.put("pe", patientEncounter);
        m.put("ret", false);

        encounterUploads = uploadFacade.findByJpql(
                "select u from Upload u where u.patientEncounter=:pe and u.retired=:ret order by u.createdAt desc", m);

        encounterLetters = clinicalFindingValueFacade.findByJpql(
                "select c from ClinicalFindingValue c where c.encounter=:pe and c.documentTemplate is not null "
                        + "and c.retired=:ret order by c.id desc", m);

        encounterForms = patientFormEntryFacade.findByJpql(
                "select f from PatientFormEntry f where f.patientEncounter=:pe and f.retired=:ret order by f.createdAt desc", m);
    }

    private void loadEmailTemplates() {
        Map<String, Object> m = new HashMap<>();
        m.put("type", DocumentTemplateType.InpatientEmail);
        m.put("ret", false);
        emailTemplates = documentTemplateFacade.findByJpql(
                "select t from DocumentTemplate t where t.type=:type and t.retired=:ret order by t.name", m);
    }

    public List<EmailRecipientCandidate> completeRecipientCandidates(String query) {
        List<EmailRecipientCandidate> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }
        String q = "%" + query.toUpperCase() + "%";
        Map<String, Object> m = new HashMap<>();
        m.put("q", q);

        List<Institution> institutions = institutionFacade.findByJpql(
                "select i from Institution i where i.retired=false and i.email is not null and i.email <> '' "
                        + "and upper(i.name) like :q order by i.name", m, 15);
        for (Institution i : institutions) {
            results.add(new EmailRecipientCandidate(i.getName(), i.getEmail().trim(), "Institution"));
        }

        List<Doctor> doctors = doctorFacade.findByJpql(
                "select d from Doctor d where d.retired=false and d.person.email is not null and d.person.email <> '' "
                        + "and upper(d.person.name) like :q order by d.person.name", m, 15);
        for (Doctor d : doctors) {
            results.add(new EmailRecipientCandidate(d.getPerson().getNameWithTitle(), d.getPerson().getEmail().trim(), "Doctor"));
        }

        List<Staff> staffMembers = staffFacade.findByJpql(
                "select s from Staff s where s.retired=false and s.person.email is not null and s.person.email <> '' "
                        + "and upper(s.person.name) like :q order by s.person.name", m, 15);
        for (Staff s : staffMembers) {
            results.add(new EmailRecipientCandidate(s.getPerson().getNameWithTitle(), s.getPerson().getEmail().trim(), "Staff"));
        }

        return results;
    }

    public void generateFromTemplate() {
        if (selectedTemplate == null) {
            JsfUtil.addErrorMessage("Please select a template");
            return;
        }
        subject = replacePlaceholders(selectedTemplate.getSubject());
        emailBody = replacePlaceholders(selectedTemplate.getContents());
    }

    private String replacePlaceholders(String text) {
        if (text == null) {
            return "";
        }
        String patientName = "N/A";
        String patientNic = "N/A";
        String bht = "N/A";
        String admissionDate = "N/A";
        String hospitalName = sessionController.getInstitution() != null ? sessionController.getInstitution().getName() : "";
        String wardName = sessionController.getDepartment() != null ? sessionController.getDepartment().getName() : "";

        if (patientEncounter != null) {
            if (patientEncounter.getPatient() != null && patientEncounter.getPatient().getPerson() != null) {
                if (patientEncounter.getPatient().getPerson().getNameWithTitle() != null) {
                    patientName = patientEncounter.getPatient().getPerson().getNameWithTitle();
                }
                if (patientEncounter.getPatient().getPerson().getNic() != null && !patientEncounter.getPatient().getPerson().getNic().isEmpty()) {
                    patientNic = patientEncounter.getPatient().getPerson().getNic();
                }
            }
            if (patientEncounter.getBhtNo() != null && !patientEncounter.getBhtNo().isEmpty()) {
                bht = patientEncounter.getBhtNo();
            }
            if (patientEncounter.getDateOfAdmission() != null) {
                admissionDate = org.apache.commons.lang3.time.DateFormatUtils.format(patientEncounter.getDateOfAdmission(), "yyyy-MM-dd HH:mm:ss");
            }
        }

        String creditCompany = "N/A";
        String creditCompanyAddress = "N/A";
        String policyNumber = "N/A";
        String referenceNumber = "N/A";
        String creditLimit = "N/A";
        if (currentEncounterCreditCompany != null) {
            if (currentEncounterCreditCompany.getInstitution() != null && currentEncounterCreditCompany.getInstitution().getName() != null) {
                creditCompany = currentEncounterCreditCompany.getInstitution().getName();
            }
            if (currentEncounterCreditCompany.getInstitution() != null
                    && currentEncounterCreditCompany.getInstitution().getAddress() != null
                    && !currentEncounterCreditCompany.getInstitution().getAddress().trim().isEmpty()) {
                creditCompanyAddress = currentEncounterCreditCompany.getInstitution().getAddress();
            }
            if (currentEncounterCreditCompany.getPolicyNo() != null && !currentEncounterCreditCompany.getPolicyNo().isEmpty()) {
                policyNumber = currentEncounterCreditCompany.getPolicyNo();
            }
            if (currentEncounterCreditCompany.getReferanceNo() != null && !currentEncounterCreditCompany.getReferanceNo().isEmpty()) {
                referenceNumber = currentEncounterCreditCompany.getReferanceNo();
            }
            creditLimit = String.format("%.2f", currentEncounterCreditCompany.getCreditLimit());
        }

        return text
                .replace("{patient_name}", patientName)
                .replace("{patient_nic}", patientNic)
                .replace("{bht}", bht)
                .replace("{admition_date}", admissionDate)
                .replace("{doa}", admissionDate)
                .replace("{hospital_name}", hospitalName)
                .replace("{institution}", hospitalName)
                .replace("{ward_name}", wardName)
                .replace("{department}", wardName)
                .replace("{credit_company}", creditCompany)
                .replace("{credit_company_address}", creditCompanyAddress)
                .replace("{policy_no}", policyNumber)
                .replace("{policy_number}", policyNumber)
                .replace("{reference_no}", referenceNumber)
                .replace("{reference_number}", referenceNumber)
                .replace("{credit_limit}", creditLimit)
                .replace("{letter_date}", org.apache.commons.lang3.time.DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
    }

    public String sendEmail() {
        if (toRecipients == null || toRecipients.isEmpty()) {
            JsfUtil.addErrorMessage("Please add at least one To recipient");
            return "";
        }
        if (subject == null || subject.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Email Subject Missing");
            return "";
        }
        if (emailBody == null || emailBody.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Message is Missing");
            return "";
        }
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("BHT is Missing");
            return "";
        }

        List<String> to = cleanEmailList(toRecipients);
        List<String> cc = cleanEmailList(ccRecipients);
        List<String> bcc = cleanEmailList(bccRecipients);

        List<EmailAttachment> attachments = new ArrayList<>();
        List<String> attachmentNames = new ArrayList<>();

        if (selectedUploads != null) {
            for (Upload u : selectedUploads) {
                EmailAttachment a = inwardDocumentUploadController.toEmailAttachment(u);
                if (a != null) {
                    attachments.add(a);
                    attachmentNames.add(a.getFileName());
                }
            }
        }
        if (selectedLetters != null) {
            for (ClinicalFindingValue c : selectedLetters) {
                EmailAttachment a = inpatientClinicalDataController.toEmailAttachment(c);
                if (a != null) {
                    attachments.add(a);
                    attachmentNames.add(a.getFileName());
                }
            }
        }
        if (selectedForms != null) {
            for (PatientFormEntry f : selectedForms) {
                EmailAttachment a = inwardFormController.toEmailAttachment(f);
                if (a != null) {
                    attachments.add(a);
                    attachmentNames.add(a.getFileName());
                }
            }
        }
        if (manualAttachment != null && manualAttachment.getSize() > 0) {
            String manualFileName = manualAttachment.getFileName();
            if (manualFileName == null || !manualFileName.matches(".*" + MANUAL_ATTACHMENT_ALLOWED_TYPES_REGEX)) {
                JsfUtil.addErrorMessage("Invalid attached file type. Only PDF, JPEG, JPG, and PNG are allowed.");
                return "";
            }
            if (manualAttachment.getSize() > MANUAL_ATTACHMENT_SIZE_LIMIT) {
                JsfUtil.addErrorMessage("Attached file size exceeds the maximum limit of 10 MB.");
                return "";
            }
            try (InputStream in = manualAttachment.getInputStream()) {
                byte[] content = IOUtils.toByteArray(in);
                EmailAttachment a = new EmailAttachment(
                        manualAttachment.getFileName(),
                        manualAttachment.getContentType(),
                        java.util.Base64.getEncoder().encodeToString(content));
                attachments.add(a);
                attachmentNames.add(a.getFileName());
            } catch (Exception ex) {
                JsfUtil.addErrorMessage("Failed to read the attached file");
                return "";
            }
        }

        AppEmail email = new AppEmail();
        email.setCreatedAt(new Date());
        email.setCreater(sessionController.getLoggedUser());
        email.setReceipientEmail(String.join("; ", to));
        email.setCcEmails(cc.isEmpty() ? null : String.join("; ", cc));
        email.setBccEmails(bcc.isEmpty() ? null : String.join("; ", bcc));
        email.setAttachmentNames(attachmentNames.isEmpty() ? null : String.join("; ", attachmentNames));
        email.setMessageSubject(subject);
        email.setMessageBody(emailBody);
        email.setDepartment(sessionController.getLoggedUser().getDepartment());
        email.setInstitution(sessionController.getLoggedUser().getInstitution());
        email.setPatientEncounter(patientEncounter);
        email.setEncounterCreditCompany(currentEncounterCreditCompany);
        email.setMessageType(MessageType.InpatientComposedEmail);
        email.setSentSuccessfully(false);
        email.setPending(true);
        emailFacade.create(email);

        try {
            boolean success = emailManagerEjb.sendEmail(to, cc, bcc, emailBody, subject, true, attachments);
            email.setSentSuccessfully(success);
            email.setPending(!success);
            if (success) {
                email.setSentAt(new Date());
                emailFacade.edit(email);
                JsfUtil.addSuccessMessage("Email Sent Successfully");
                return "/inward/admission_profile?faces-redirect=true";
            } else {
                emailFacade.edit(email);
                JsfUtil.addErrorMessage("Sending Email Failed");
                return "";
            }
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(InpatientEmailComposeController.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Failed to send inpatient composed email", ex);
            emailFacade.edit(email);
            JsfUtil.addErrorMessage("Sending Email Failed");
            return "";
        }
    }

    private List<String> cleanEmailList(List<EmailRecipientCandidate> candidates) {
        List<String> emails = new ArrayList<>();
        if (candidates == null) {
            return emails;
        }
        for (EmailRecipientCandidate c : candidates) {
            if (c != null && c.getEmail() != null && !c.getEmail().trim().isEmpty()) {
                emails.add(c.getEmail().trim());
            }
        }
        return emails;
    }

    // ---------------- Getters & Setters ---------------- //

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public EncounterCreditCompany getCurrentEncounterCreditCompany() {
        return currentEncounterCreditCompany;
    }

    public List<EmailRecipientCandidate> getToRecipients() {
        return toRecipients;
    }

    public void setToRecipients(List<EmailRecipientCandidate> toRecipients) {
        this.toRecipients = toRecipients;
    }

    public List<EmailRecipientCandidate> getCcRecipients() {
        return ccRecipients;
    }

    public void setCcRecipients(List<EmailRecipientCandidate> ccRecipients) {
        this.ccRecipients = ccRecipients;
    }

    public List<EmailRecipientCandidate> getBccRecipients() {
        return bccRecipients;
    }

    public void setBccRecipients(List<EmailRecipientCandidate> bccRecipients) {
        this.bccRecipients = bccRecipients;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getEmailBody() {
        return emailBody;
    }

    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
    }

    public DocumentTemplate getSelectedTemplate() {
        return selectedTemplate;
    }

    public void setSelectedTemplate(DocumentTemplate selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }

    public List<DocumentTemplate> getEmailTemplates() {
        return emailTemplates;
    }

    public List<Upload> getEncounterUploads() {
        return encounterUploads;
    }

    public List<ClinicalFindingValue> getEncounterLetters() {
        return encounterLetters;
    }

    public List<PatientFormEntry> getEncounterForms() {
        return encounterForms;
    }

    public List<Upload> getSelectedUploads() {
        return selectedUploads;
    }

    public void setSelectedUploads(List<Upload> selectedUploads) {
        this.selectedUploads = selectedUploads;
    }

    public List<ClinicalFindingValue> getSelectedLetters() {
        return selectedLetters;
    }

    public void setSelectedLetters(List<ClinicalFindingValue> selectedLetters) {
        this.selectedLetters = selectedLetters;
    }

    public List<PatientFormEntry> getSelectedForms() {
        return selectedForms;
    }

    public void setSelectedForms(List<PatientFormEntry> selectedForms) {
        this.selectedForms = selectedForms;
    }

    public UploadedFile getManualAttachment() {
        return manualAttachment;
    }

    public void setManualAttachment(UploadedFile manualAttachment) {
        this.manualAttachment = manualAttachment;
    }
}
