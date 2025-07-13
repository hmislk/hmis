package com.divudi.bean.lab;

import java.io.ByteArrayInputStream;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Damiya
 */
@Named
@RequestScoped
public class LabReportViewController {
    private static final Logger LOG = Logger.getLogger(LabReportViewController.class.getName());


    @Inject
    PatientReportUploadController patientReportUploadController;

    /**
     * Creates a new instance of LabReportViewController
     */
    public LabReportViewController() {
    }

    public boolean isPdf() {
        LOG.log(Level.INFO, "isPdf");
        if (patientReportUploadController.getReportUpload() == null || patientReportUploadController.getReportUpload().getFileType() == null) {
            return false;
        }
        LOG.log(Level.INFO, "patientReportUploadController.getReportUpload() = " + patientReportUploadController.getReportUpload().getId());
        LOG.log(Level.INFO, "patientReportUploadController.getReportUpload() = " + patientReportUploadController.getReportUpload().getIdStr());
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
        LOG.log(Level.INFO, "getPdfReportStream");
        if (isPdf()) {
            ByteArrayInputStream input = new ByteArrayInputStream(patientReportUploadController.getReportUpload().getBaImage());
            LOG.log(Level.INFO, "patientReportUploadController.getReportUpload() = " + patientReportUploadController.getReportUpload().getId());
            LOG.log(Level.INFO, "patientReportUploadController.getReportUpload() = " + patientReportUploadController.getReportUpload().getIdStr());
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
