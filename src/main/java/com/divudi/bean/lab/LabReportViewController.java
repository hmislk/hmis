/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.lab;

import com.divudi.entity.Upload;
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
    PatientReportUploadController patientReportUploadController ;

    private Upload reportUpload;
    
    /**
     * Creates a new instance of LabReportViewController
     */
    public LabReportViewController() {
    }
    
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
    
}
