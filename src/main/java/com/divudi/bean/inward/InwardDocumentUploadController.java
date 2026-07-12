package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.UploadType;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Upload;
import com.divudi.core.facade.UploadFacade;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

@Named
@SessionScoped
public class InwardDocumentUploadController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private UploadFacade uploadFacade;
    @EJB
    private com.divudi.core.facade.EmailFacade emailFacade;
    @EJB
    private com.divudi.ejb.EmailManagerEjb emailManagerEjb;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private SessionController sessionController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Variables">
    private PatientEncounter currentEncounter;
    private List<Upload> documents;
    private UploadType selectedUploadType;
    private String comments;
    private UploadedFile file;
    private Upload selectedDocument;
    private String emailRecipient;

    private static final long SIZE_LIMIT = 10240000;
    private static final String ALLOWED_FILE_TYPES_REGEX = "(?i)\\.(pdf|jpeg|jpg|png)$";
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Navigation">
    public String navigateToDocumentsFromAdmissionProfile(PatientEncounter pe) {
        currentEncounter = pe;
        loadDocuments();
        selectedUploadType = null;
        comments = null;
        file = null;
        return "/inward/admission_documents?faces-redirect=true";
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Functional Methods">
    public void loadDocuments() {
        if (currentEncounter == null) {
            documents = new ArrayList<>();
            return;
        }
        String jpql = "select u from Upload u "
                + "where u.retired = :ret "
                + "and u.patientEncounter = :pe "
                + "order by u.createdAt desc";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("pe", currentEncounter);
        documents = uploadFacade.findByJpql(jpql, params);
    }

    public void uploadDocument() {
        if (file == null || file.getSize() == 0) {
            JsfUtil.addErrorMessage("Please select a file.");
            return;
        }
        if (selectedUploadType == null) {
            JsfUtil.addErrorMessage("Please select a document type.");
            return;
        }
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No admission selected.");
            return;
        }
        String fileName = file.getFileName();
        if (!fileName.matches(".*" + ALLOWED_FILE_TYPES_REGEX)) {
            JsfUtil.addErrorMessage("Invalid file type. Only PDF, JPEG, JPG, and PNG are allowed.");
            return;
        }
        if (file.getSize() > SIZE_LIMIT) {
            JsfUtil.addErrorMessage("File size exceeds the maximum limit of 10 MB.");
            return;
        }
        try {
            byte[] fileContent;
            try (InputStream in = file.getInputStream()) {
                fileContent = IOUtils.toByteArray(in);
            }
            String detectedContentType = detectContentType(fileContent, fileName);
            if (detectedContentType == null) {
                JsfUtil.addErrorMessage("Invalid file type. Only PDF, JPEG, JPG, and PNG are allowed.");
                return;
            }
            Upload upload = new Upload();
            upload.setUploadType(selectedUploadType);
            upload.setPatientEncounter(currentEncounter);
            upload.setBaImage(fileContent);
            upload.setFileName(fileName);
            upload.setFileType(detectedContentType);
            upload.setComments(comments);
            upload.setCreatedAt(new Date());
            upload.setCreater(sessionController.getLoggedUser());
            upload.setRetired(false);
            uploadFacade.create(upload);
            JsfUtil.addSuccessMessage("Document uploaded successfully.");
            file = null;
            selectedUploadType = null;
            comments = null;
            loadDocuments();
        } catch (IOException e) {
            JsfUtil.addErrorMessage("Error uploading file: " + e.getMessage());
        }
    }

    public void handleFileUpload(FileUploadEvent event) {
        UploadedFile uploadedFile = event.getFile();
        if (uploadedFile == null || uploadedFile.getSize() == 0) {
            JsfUtil.addErrorMessage("Please select a file.");
            return;
        }
        if (uploadedFile.getSize() > SIZE_LIMIT) {
            JsfUtil.addErrorMessage("File size exceeds the maximum limit of 10 MB.");
            return;
        }
        if (selectedUploadType == null) {
            JsfUtil.addErrorMessage("Please select a document type.");
            return;
        }
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No admission selected.");
            return;
        }
        String fileName = uploadedFile.getFileName();
        if (!fileName.matches(".*" + ALLOWED_FILE_TYPES_REGEX)) {
            JsfUtil.addErrorMessage("Invalid file type. Only PDF, JPEG, JPG, and PNG are allowed.");
            return;
        }
        try {
            byte[] fileContent;
            try (InputStream in = uploadedFile.getInputStream()) {
                fileContent = IOUtils.toByteArray(in);
            }
            String detectedContentType = detectContentType(fileContent, fileName);
            if (detectedContentType == null) {
                JsfUtil.addErrorMessage("Invalid file type. Only PDF, JPEG, JPG, and PNG are allowed.");
                return;
            }
            Upload upload = new Upload();
            upload.setUploadType(selectedUploadType);
            upload.setPatientEncounter(currentEncounter);
            upload.setBaImage(fileContent);
            upload.setFileName(fileName);
            upload.setFileType(detectedContentType);
            upload.setComments(comments);
            upload.setCreatedAt(new Date());
            upload.setCreater(sessionController.getLoggedUser());
            upload.setRetired(false);
            uploadFacade.create(upload);
            JsfUtil.addSuccessMessage("Document uploaded successfully.");
            selectedUploadType = null;
            comments = null;
            loadDocuments();
        } catch (IOException e) {
            JsfUtil.addErrorMessage("Error uploading file: " + e.getMessage());
        }
    }

    public void viewDocument(Upload doc) {
        selectedDocument = doc;
    }

    public void removeDocument(Upload u) {
        if (u == null) {
            return;
        }
        u.setRetired(true);
        u.setRetirer(sessionController.getLoggedUser());
        u.setRetiredAt(new Date());
        uploadFacade.edit(u);
        loadDocuments();
        JsfUtil.addSuccessMessage("Document removed.");
    }

    public StreamedContent getDownloadStream(Upload u) {
        if (u == null || u.getId() == null) {
            return null;
        }
        Upload persisted = uploadFacade.find(u.getId());
        if (persisted == null || persisted.isRetired() || persisted.getBaImage() == null) {
            return null;
        }
        byte[] data = persisted.getBaImage();
        return DefaultStreamedContent.builder()
                .name(persisted.getFileName())
                .contentType(persisted.getFileType())
                .stream(() -> new ByteArrayInputStream(data))
                .build();
    }

    public void prepareEmailDialog(Upload doc) {
        if (doc == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        selectedDocument = doc;
        emailRecipient = resolveDefaultEmailRecipient();
    }

    private String resolveDefaultEmailRecipient() {
        if (currentEncounter != null && currentEncounter.getPatient() != null
                && currentEncounter.getPatient().getPerson() != null
                && currentEncounter.getPatient().getPerson().getEmail() != null
                && !currentEncounter.getPatient().getPerson().getEmail().trim().isEmpty()) {
            return currentEncounter.getPatient().getPerson().getEmail().trim();
        }
        if (currentEncounter != null && currentEncounter.getGuardian() != null
                && currentEncounter.getGuardian().getEmail() != null
                && !currentEncounter.getGuardian().getEmail().trim().isEmpty()) {
            return currentEncounter.getGuardian().getEmail().trim();
        }
        return "";
    }

    public void sendDocumentEmail() {
        if (selectedDocument == null) {
            JsfUtil.addErrorMessage("No document selected");
            return;
        }
        if (emailRecipient == null || emailRecipient.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter recipient email");
            return;
        }
        String recipient = emailRecipient.trim();
        if (!com.divudi.core.util.CommonFunctions.isValidEmail(recipient)) {
            JsfUtil.addErrorMessage("Please enter a valid email address");
            return;
        }

        Upload persisted = selectedDocument.getId() != null ? uploadFacade.find(selectedDocument.getId()) : null;
        if (persisted == null || persisted.isRetired() || persisted.getBaImage() == null || persisted.getBaImage().length == 0) {
            JsfUtil.addErrorMessage("Document content is not available to attach");
            return;
        }

        String subject = "Document: " + (selectedDocument.getFileName() != null ? selectedDocument.getFileName() : "Attachment");
        String body = buildDocumentEmailHtml();

        com.divudi.core.data.EmailAttachment attachment = new com.divudi.core.data.EmailAttachment(
                persisted.getFileName(),
                persisted.getFileType(),
                java.util.Base64.getEncoder().encodeToString(persisted.getBaImage()));

        com.divudi.core.entity.AppEmail email = new com.divudi.core.entity.AppEmail();
        email.setCreatedAt(new Date());
        email.setCreater(sessionController.getLoggedUser());
        email.setReceipientEmail(recipient);
        email.setMessageSubject(subject);
        email.setMessageBody(body);
        email.setDepartment(sessionController.getLoggedUser().getDepartment());
        email.setInstitution(sessionController.getLoggedUser().getInstitution());
        email.setPatientEncounter(currentEncounter);
        email.setMessageType(com.divudi.core.data.MessageType.InpatientDocumentUpload);
        email.setAttachment1(persisted.getFileName());
        email.setSentSuccessfully(false);
        email.setPending(true);
        emailFacade.create(email);

        try {
            boolean success = emailManagerEjb.sendEmail(
                    java.util.Collections.singletonList(recipient),
                    body,
                    subject,
                    true,
                    java.util.Collections.singletonList(attachment)
            );
            email.setSentSuccessfully(success);
            email.setPending(!success);
            if (success) {
                email.setSentAt(new Date());
                JsfUtil.addSuccessMessage("Email Sent Successfully");
            } else {
                JsfUtil.addErrorMessage("Sending Email Failed");
            }
            emailFacade.edit(email);
        } catch (Exception ex) {
            JsfUtil.addErrorMessage("Sending Email Failed");
        }
    }

    private String buildDocumentEmailHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<p>Please find the details of the document below.</p>");
        sb.append("<table border=\"0\" cellpadding=\"4\">");
        sb.append("<tr><td><b>Document Type</b></td><td>").append(escapeHtml(selectedDocument.getUploadType() != null ? selectedDocument.getUploadType().getLabel() : "")).append("</td></tr>");
        sb.append("<tr><td><b>File Name</b></td><td>").append(escapeHtml(selectedDocument.getFileName())).append("</td></tr>");
        if (selectedDocument.getComments() != null && !selectedDocument.getComments().trim().isEmpty()) {
            sb.append("<tr><td><b>Comments</b></td><td>").append(escapeHtml(selectedDocument.getComments())).append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p><i>The document is attached to this email.</i></p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String detectContentType(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length < 4) {
            return null;
        }
        // PDF: %PDF
        if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
            return "application/pdf";
        }
        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        // PNG: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "image/png";
        }
        return null;
    }

    public List<UploadType> getInwardUploadTypes() {
        List<UploadType> types = new ArrayList<>();
        types.add(UploadType.Inward_Consent_Form);
        types.add(UploadType.Inward_Insurance_Document);
        types.add(UploadType.Inward_Referral_Letter);
        types.add(UploadType.Inward_GOP);
        types.add(UploadType.Inward_Other);
        return types;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public PatientEncounter getCurrentEncounter() {
        return currentEncounter;
    }

    public void setCurrentEncounter(PatientEncounter currentEncounter) {
        this.currentEncounter = currentEncounter;
    }

    public List<Upload> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Upload> documents) {
        this.documents = documents;
    }

    public UploadType getSelectedUploadType() {
        return selectedUploadType;
    }

    public void setSelectedUploadType(UploadType selectedUploadType) {
        this.selectedUploadType = selectedUploadType;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Upload getSelectedDocument() {
        return selectedDocument;
    }

    public void setSelectedDocument(Upload selectedDocument) {
        this.selectedDocument = selectedDocument;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public String getEmailRecipient() {
        return emailRecipient;
    }

    public void setEmailRecipient(String emailRecipient) {
        this.emailRecipient = emailRecipient;
    }

    // </editor-fold>
}
