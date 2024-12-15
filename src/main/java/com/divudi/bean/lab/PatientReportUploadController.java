package com.divudi.bean.lab;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.UploadType;
import com.divudi.entity.Upload;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.facade.PatientReportFacade;
import com.divudi.facade.UploadFacade;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author H.K. Damith Deshan hkddrajapaksha@gmail.com
 *
 */
@Named
@SessionScoped
public class PatientReportUploadController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EGBs">
    @EJB
    private UploadFacade facade;
    @EJB
    PatientReportFacade patientReportFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    PatientReportController patientReportController;

    Upload reportUpload;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Variables">
    private PatientInvestigation patientInvestigation;
    private UploadedFile file;
    private UploadType uploadType;

    private static final long SIZE_LIMIT = 10240000; // 1 MB
    private static final String ALLOWED_FILE_TYPES_REGEX = "(\\.|\\/)(pdf|jpeg|jpg|png)$";

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Functional Methods">
    public void makeNull() {
        patientInvestigation = null;
        file = null;
        uploadType = null;
    }


    public String uploadReport() {
        InputStream in;
        if (file == null || "".equals(file.getFileName())) {
            return "";
        }
        if (file == null) {
            JsfUtil.addErrorMessage("Please select an image");
            return "";
        }
        if (patientReportController.getCurrentPatientReport() == null) {
            JsfUtil.addErrorMessage("Please select an image");
            return "";
        }
        if (patientReportController.getCurrentPatientReport().getId() == null) {
            patientReportFacade.create(patientReportController.getCurrentPatientReport());
        } else {
            patientReportFacade.edit(patientReportController.getCurrentPatientReport());
        }
        try {
            in = getFile().getInputStream();
            reportUpload = new Upload();
            reportUpload.setPatientReport(patientReportController.getCurrentPatientReport());
            reportUpload.setBaImage(IOUtils.toByteArray(in));
            reportUpload.setFileName(getFile().getFileName());
            reportUpload.setFileType(getFile().getContentType());
            facade.create(reportUpload);
            return "";
        } catch (IOException e) {
            //////System.out.println("Error " + e.getMessage());
            return "";
        }

    }

    public UploadFacade getFacade() {
        return facade;
    }

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

}
