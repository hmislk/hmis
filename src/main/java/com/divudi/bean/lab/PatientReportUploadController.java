package com.divudi.bean.lab;

import com.divudi.data.UploadType;
import com.divudi.entity.Bill;
import com.divudi.entity.Category;
import com.divudi.entity.Upload;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.facade.UploadFacade;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Named;
import org.apache.commons.lang3.math.NumberUtils;
import javax.faces.context.FacesContext;
import javax.persistence.ManyToOne;
import org.primefaces.model.file.UploadedFile;


/**
 *
 * @author 
 * H.K. Damith Deshan
 * hkddrajapaksha@gmail.com
 * 
 */
@Named
@SessionScoped
public class PatientReportUploadController implements Serializable {

    @EJB
    private UploadFacade facade;

    private PatientInvestigation patientInvestigation;
    private Bill bill;
    private UploadedFile file;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private Category category;
    private UploadType uploadType;
    
    
    
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
