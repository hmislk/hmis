package com.divudi.bean.lab;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.InvestigationReportType;
import com.divudi.data.UploadType;
import com.divudi.entity.Bill;
import com.divudi.entity.Category;
import com.divudi.entity.Patient;
import com.divudi.entity.Upload;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.facade.UploadFacade;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Named;
import org.apache.commons.lang3.math.NumberUtils;
import javax.faces.context.FacesContext;
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
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Variables">
    private PatientInvestigation patientInvestigation;
    private Bill bill;
    private UploadedFile file;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private Category category;
    private UploadType uploadType;
    private Patient patient;
    private PatientReport currentReport;
    
    private static final long SIZE_LIMIT = 1024000; // 1 MB
    private static final String ALLOWED_FILE_TYPES_REGEX = "(\\.|\\/)(pdf|jpeg|jpg|png)$";
    
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Functional Methods">
    
    public void makeNull() {
        patientInvestigation = null;
        bill = null;
        file = null;
        fileName = null;
        fileType = null;
        fileUrl = null;
        category = null;
        uploadType = null;
    }
        
    public void uploadReport(){
        fileErrorCheck();
 
    }
    
//    public void createReport(){
//        if (patientInvestigation.getInvestigation() == null) {
//            JsfUtil.addErrorMessage("No Investigation for Patient Report");
//            return null;
//        } else {
//            ix = (Investigation) pi.getInvestigation();
//        }
//        if (ix.getReportedAs() != null) {
//            ix = (Investigation) pi.getInvestigation().getReportedAs();
//        }
//
//        currentReportInvestigation = ix;
//        currentPtIx = pi;
//        PatientReport newlyCreatedReport = null;
//        if (ix.getReportType() == InvestigationReportType.Microbiology) {
//            createNewMicrobiologyReport(pi, ix);
//        } else {
//            newlyCreatedReport = createNewPatientReport(pi, ix);
//        }
//        if (newlyCreatedReport == null) {
//            JsfUtil.addErrorMessage("Error");
//            return null;
//        }
//        currentPatientReport = newlyCreatedReport;
//    }
    
    public boolean fileErrorCheck(){
        System.out.println("file = " + file);
        System.out.println("getFile = " + getFile());
        if(file == null){
            JsfUtil.addErrorMessage("No File Found");
            return true;
        }else if(file.getContent().length > 0){
            JsfUtil.addErrorMessage("File is Empty");
            return true;
        }else if(file.getSize() > SIZE_LIMIT){
            JsfUtil.addErrorMessage("The file exceeds the maximum allowed size of 1 MB.");
            return true;
        }else if(file.getFileName() != null){
            JsfUtil.addErrorMessage("File Name is Missing");
            return true;
        }else if(file.getContent().length > 0){
            Pattern pattern = Pattern.compile(ALLOWED_FILE_TYPES_REGEX, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(fileName);
            if(matcher.find()){
                return false;
            }else{
                JsfUtil.addErrorMessage("File Type is Worng");
                return true;
            }
        }else{
            return false;
        }
    }
    
    
    
    
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Navigation Method">
    public String navigateToUploadPatientReport(PatientInvestigation pi) {
        setPatientInvestigation(pi);
        setBill(pi.getBillItem().getBill());
        
        
        return "/lab/upload_patient_report?faces-redirect=true";
    }

    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public UploadFacade getFacade() {
        return facade;
    }

    public void setFacade(UploadFacade facade) {
        this.facade = facade;
    }

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public void setPatientInvestigation(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public UploadType getUploadType() {
        return uploadType;
    }

    public void setUploadType(UploadType uploadType) {
        this.uploadType = uploadType;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PatientReport getCurrentReport() {
        return currentReport;
    }

    public void setCurrentReport(PatientReport currentReport) {
        this.currentReport = currentReport;
    }

    // </editor-fold>
    
    @FacesConverter(forClass = Upload.class)
    public static class PatientReportControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PatientReportUploadController controller = (PatientReportUploadController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "PatientReportUploadController");
            return controller.getFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            if (NumberUtils.isNumber(value)) {
                key = Long.valueOf(value);
            } else {
                key = 0l;
            }
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof PatientReport) {
                PatientReport o = (PatientReport) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PatientReportUploadController.class.getName());
            }
        }
    }

}
