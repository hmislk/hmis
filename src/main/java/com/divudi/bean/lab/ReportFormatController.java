/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.lab;

import com.divudi.bean.clinical.PhotoCamBean;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UploadController;

import com.divudi.entity.Upload;
import com.divudi.entity.lab.ReportFormat;
import com.divudi.facade.ReportFormatFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class ReportFormatController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    UploadController uploadController;
    @EJB
    private ReportFormatFacade ejbFacade;
    List<ReportFormat> selectedItems;
    private ReportFormat current;
    private UploadedFile uploadedFile;
    private List<ReportFormat> items = null;
    private Upload upload;
    String selectText = "";

    public List<ReportFormat> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from ReportFormat c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new ReportFormat();
    }

    public void setSelectedItems(List<ReportFormat> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public ReportFormatFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ReportFormatFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ReportFormatController() {
    }

    public void uploadPhoto(FileUploadEvent event) {
        if (getUpload() == null || getUpload().getId() == null) {
            JsfUtil.addErrorMessage("Select Category");
            return;
        }
        byte[] fileBytes;
        try {
            uploadedFile = event.getFile();
            fileBytes = uploadedFile.getContent();
            getUpload().setBaImage(fileBytes);

            // Extracting the file extension and setting the file name
            String fileName = uploadedFile.getFileName();
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
            getUpload().setFileName("template_image" + getUpload().getId() + "." + extension);

            getUpload().setFileType(event.getFile().getContentType());
            uploadController.saveUpload(getUpload());
        } catch (Exception ex) {
            Logger.getLogger(PhotoCamBean.class.getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage("Error");
            return;
        }
    }
    
    public void removeUploadedFile(){
        if (getUpload() == null || getUpload().getId() == null) {
            JsfUtil.addErrorMessage("Select Category");
            return;
        }
        byte[] fileBytes;
        try {
            getUpload().setBaImage(null);
            uploadController.saveUpload(getUpload());
            
        } catch (Exception ex) {
            Logger.getLogger(PhotoCamBean.class.getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage("Error");
            return;
        }
    }

    public ReportFormat getCurrent() {
        if (current == null) {
            current = new ReportFormat();
        }
        return current;
    }

    public void setCurrent(ReportFormat current) {
        this.current = current;
        if (current != null && current.getId()!=null) {
            upload = uploadController.findUpload(current);
            if (upload == null) {
                upload = new Upload();
                upload.setCategory(current);
                upload.setCreater(sessionController.getLoggedUser());
                upload.setCreatedAt(new Date());
                uploadController.saveUpload(upload);
            }
        }else{
            upload = null;
        }
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private ReportFormatFacade getFacade() {
        return ejbFacade;
    }

    public StreamedContent getImage() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
            // So, we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.
            return DefaultStreamedContent.builder().build();
        } else if (getUpload() == null) {
            return DefaultStreamedContent.builder().build();
        } else {
            String imageType = getUpload().getFileType();
            if (imageType == null || imageType.trim().equals("")) {
                imageType = "image/png";
            }
            return DefaultStreamedContent.builder()
                    .contentType(imageType)
                    .stream(() -> new ByteArrayInputStream(getUpload().getBaImage()))
                    .build();
        }
    }

    public List<ReportFormat> getItems() {
        if (items == null) {
            String sql = "SELECT i FROM ReportFormat i where i.retired=false order by i.name";
            items = getEjbFacade().findByJpql(sql);
        }
        return items;
    }

    public void setItems(List<ReportFormat> items) {
        this.items = items;
    }

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
    }

    /**
     *
     */
}
