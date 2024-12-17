package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.UploadType;
import com.divudi.entity.Upload;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.facade.PatientReportFacade;
import com.divudi.facade.UploadFacade;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 * - *
 * - * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com - *
 */
@Named
@SessionScoped
public class PatientReportUploadController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private UploadFacade facade;
    @EJB
    private PatientReportFacade patientReportFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private PatientReportController patientReportController;
    @Inject
    SessionController sessionController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Variables">
    private PatientInvestigation patientInvestigation;
    private org.primefaces.model.file.UploadedFile file; // Ensure correct import
    private UploadType uploadType;

    private static final long SIZE_LIMIT = 10240000; // 10 MB
    private static final String ALLOWED_FILE_TYPES_REGEX = "(?i)\\.(pdf|jpeg|jpg|png)$"; // Case-insensitive

    private Upload reportUpload; // To store the uploaded file's information
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Functional Methods">
    public void makeNull() {
        patientInvestigation = null;
        file = null;
        uploadType = null;
        reportUpload = null;
    }

    public String uploadReport() {
        if (file == null || file.getSize() == 0) {
            JsfUtil.addErrorMessage("Please select a file to upload.");
            return "";
        }

        // Validate file size
        if (file.getSize() > SIZE_LIMIT) {
            JsfUtil.addErrorMessage("File size exceeds the maximum limit of 10 MB.");
            return "";
        }

        // Validate file type
        String fileName = file.getFileName();
        if (!fileName.matches(".*" + ALLOWED_FILE_TYPES_REGEX)) {
            JsfUtil.addErrorMessage("Invalid file type. Only PDF, JPEG, JPG, and PNG are allowed.");
            return "";
        }

        if (patientReportController.getCurrentPatientReport() == null) {
            JsfUtil.addErrorMessage("Please select a patient report.");
            return "";
        }

        try {
            // Persist the patient report if it's new
            if (patientReportController.getCurrentPatientReport().getId() == null) {
                patientReportFacade.create(patientReportController.getCurrentPatientReport());
            } else {
                patientReportFacade.edit(patientReportController.getCurrentPatientReport());
            }

            // Read the file input stream
            InputStream in = file.getInputStream();
            byte[] fileContent = IOUtils.toByteArray(in);

            // Create and persist the Upload entity
            reportUpload = new Upload();
            reportUpload.setUploadType(UploadType.Lab_Report);
            reportUpload.setPatientReport(patientReportController.getCurrentPatientReport());
            reportUpload.setBaImage(fileContent);
            reportUpload.setFileName(fileName);
            reportUpload.setFileType(file.getContentType());
            facade.create(reportUpload);

            JsfUtil.addSuccessMessage("File Uploaded Successfully.");
            file = null;
            return ""; // Stay on the same page or navigate as needed
        } catch (IOException e) {
            JsfUtil.addErrorMessage("Error uploading file: " + e.getMessage());
            return "";
        }
    }

    public void reportApproval() {
        System.out.println("reportApproval");  
        if (patientReportController.getCurrentPatientReport() == null) {
            System.out.println("Approval-1");
            JsfUtil.addErrorMessage("Please select a patient report.");
            return;
        }
        if (reportUpload.getBaImage() == null) {
            System.out.println("Approval-2");
            JsfUtil.addErrorMessage("Please select a patient report.");
            return;
        }
        patientReportController.getCurrentPatientReport().setApproveAt(new Date());
        patientReportController.getCurrentPatientReport().setApproveUser(sessionController.getLoggedUser());
        patientReportController.getCurrentPatientReport().setApproved(Boolean.TRUE);
        patientReportFacade.edit(patientReportController.getCurrentPatientReport());
        System.out.println("Report Approved.");  
        JsfUtil.addSuccessMessage("Report Approved.");
    }

    public void reportApprovalCancel() {
        System.out.println("reportApprovalCancel");  
        if (patientReportController.getCurrentPatientReport() == null) {
            System.out.println("Cancel-1");
            JsfUtil.addErrorMessage("Please select a patient report.");
            return;
        }
        patientReportController.getCurrentPatientReport().setApproveAt(null);
        patientReportController.getCurrentPatientReport().setApproveUser(null);
        patientReportController.getCurrentPatientReport().setApproved(null);
        patientReportFacade.edit(patientReportController.getCurrentPatientReport());
        System.out.println("Cancel Approved.");  
        JsfUtil.addSuccessMessage("Cancel Approved.");
    }
    
    public void removeUploadedFile() {
        System.out.println("removeUploadedFile");  
        if (reportUpload.getBaImage() == null) {
            System.out.println("Remove-1");
            JsfUtil.addErrorMessage("Please select a patient report.");
            return;
        }
        reportUpload.setBaImage(null);
        reportUpload.setFileName(null);
        reportUpload.setFileType(null);
        facade.edit(reportUpload);
        System.out.println("Remove Successfully.");  
        JsfUtil.addSuccessMessage("Remove Successfully.");
    }

    public List<Upload> loadUploads(PatientReport pr) {
        String jpql = "select u "
                + " from Upload u "
                + " where u.retired=:ret"
                + " and u.patientReport=:pr"
                + " and u.patientReport.retired=:prr"
                + " and u.uploadType=:ut";

        Map params = new HashMap<>();
        params.put("ret", false);
        params.put("pr", pr);
        params.put("ut", UploadType.Lab_Report);
        params.put("prr", false);

        return facade.findByJpql(jpql, params);
    }

    // Helper method to check if the uploaded file is a PDF
    public boolean isPdf() {
        if (reportUpload == null || reportUpload.getFileType() == null) {
            return false;
        }
        return "application/pdf".equalsIgnoreCase(reportUpload.getFileType());
    }

    // Helper method to check if the uploaded file is an image
    public boolean isImage() {
        if (reportUpload == null || reportUpload.getFileType() == null) {
            return false;
        }
        return reportUpload.getFileType().toLowerCase().startsWith("image/");
    }

    // StreamedContent getter for PDF
    public StreamedContent getPdfReportStream() {
        if (isPdf()) {
            ByteArrayInputStream input = new ByteArrayInputStream(reportUpload.getBaImage());
            return DefaultStreamedContent.builder()
                    .name(reportUpload.getFileName())
                    .contentType(reportUpload.getFileType())
                    .stream(() -> input)
                    .build();
        }
        return null;
    }

    // StreamedContent getter for image
    public StreamedContent getImageReportStream() {
        if (isImage()) {
            ByteArrayInputStream input = new ByteArrayInputStream(reportUpload.getBaImage());
            return DefaultStreamedContent.builder()
                    .name(reportUpload.getFileName())
                    .contentType(reportUpload.getFileType())
                    .stream(() -> input)
                    .build();
        }
        return null;
    }

    // Getters and Setters
    public UploadFacade getFacade() {
        return facade;
    }

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public void setPatientInvestigation(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
    }

    public org.primefaces.model.file.UploadedFile getFile() {
        return file;
    }

    public void setFile(org.primefaces.model.file.UploadedFile file) {
        this.file = file;
    }

    public Upload getReportUpload() {
        return reportUpload;
    }

    public void setReportUpload(Upload reportUpload) {
        this.reportUpload = reportUpload;
    }

    public PatientReportController getPatientReportController() {
        return patientReportController;
    }

    public void setPatientReportController(PatientReportController patientReportController) {
        this.patientReportController = patientReportController;
    }
    // </editor-fold>
}
