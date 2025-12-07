package com.divudi.bean.lab;

import java.io.ByteArrayInputStream;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Damiya
 */
@Named
@RequestScoped
public class LabReportViewController {

    @Inject
    PatientReportUploadController patientReportUploadController;

    /**
     * Creates a new instance of LabReportViewController
     */
    public LabReportViewController() {
    }

    public boolean isPdf() {
        System.out.println("isPdf");
        if (patientReportUploadController.getReportUpload() == null || patientReportUploadController.getReportUpload().getFileType() == null) {
            return false;
        }
        System.out.println("patientReportUploadController.getReportUpload() = " + patientReportUploadController.getReportUpload().getId());
        return "application/pdf".equalsIgnoreCase(patientReportUploadController.getReportUpload().getFileType());
    }

    // Helper method to check if the uploaded file is an image
    public boolean isImage() {
        if (patientReportUploadController.getReportUpload() == null || patientReportUploadController.getReportUpload().getFileType() == null) {
            return false;
        }
        return patientReportUploadController.getReportUpload().getFileType().toLowerCase().startsWith("image/");
    }

    // StreamedContent getter for PDF
    public StreamedContent getPdfReportStream() {
        if (isPdf()) {
            ByteArrayInputStream input = new ByteArrayInputStream(patientReportUploadController.getReportUpload().getBaImage());
            System.out.println("patientReportUploadController.getReportUpload() = " + patientReportUploadController.getReportUpload().getId());
            return DefaultStreamedContent.builder()
                    .name(patientReportUploadController.getReportUpload().getFileName())
                    .contentType(patientReportUploadController.getReportUpload().getFileType())
                    .stream(() -> input)
                    .build();
        }
        return null;
    }

    // StreamedContent getter for image
    public StreamedContent getImageReportStream() {
        if (isImage()) {
            ByteArrayInputStream input = new ByteArrayInputStream(patientReportUploadController.getReportUpload().getBaImage());
            return DefaultStreamedContent.builder()
                    .name(patientReportUploadController.getReportUpload().getFileName())
                    .contentType(patientReportUploadController.getReportUpload().getFileType())
                    .stream(() -> input)
                    .build();
        }
        return null;
    }

}
