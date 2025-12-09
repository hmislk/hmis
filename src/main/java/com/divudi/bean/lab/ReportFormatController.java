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

import com.divudi.core.entity.Upload;
import com.divudi.core.entity.lab.ReportFormat;
import com.divudi.core.facade.ReportFormatFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.lab.PatientReport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private String fileUrl;
    private String viewImageType;

    public String navigateToReportTemplateImageUpload() {
        viewImageType = "UPLOADIMAGE";
        makeNull();
        return "/admin/lims/report_template_image_upload?faces-redirect=true";
    }

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

    public void makeNull() {
        current = null;
        fileUrl = null;
    }

    public void removeUploadedFile() {
        if (getUpload() == null || getUpload().getId() == null) {
            JsfUtil.addErrorMessage("Select Category");
            return;
        }
        try {
            getUpload().setBaImage(null);
            uploadController.saveUpload(getUpload());
            JsfUtil.addSuccessMessage("Removed Successfully");
        } catch (Exception ex) {
            Logger.getLogger(PhotoCamBean.class.getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage("Error");
            return;
        }
    }

    public void removeUrlFile() {
        System.out.println("URL");
        getUpload().setFileUrl(null);
        uploadController.saveUpload(getUpload());
        JsfUtil.addSuccessMessage("Removed Successfully");

    }

    public void saveFileUrl() {
        if (getUpload() == null || getUpload().getId() == null) {
            JsfUtil.addErrorMessage("Select Category");
            return;
        }
        getUpload().setFileUrl(fileUrl);
        uploadController.saveUpload(getUpload());
        JsfUtil.addSuccessMessage("Save Successfullt");
    }

    public ReportFormat getCurrent() {
        if (current == null) {
            current = new ReportFormat();
        }
        return current;
    }

    public ReportFormat getValidReportFormat() {
        String jpql = "select f "
                + " from ReportFormat f"
                + " where f.retired=:ret"
                + " order by f.id desc";
        Map params = new HashMap();
        params.put("ret", false);
        ReportFormat r = getFacade().findFirstByJpql(jpql, params);
        if (r == null) {
            r = new ReportFormat();
            r.setName("Common Report Format");
            r.setCreatedAt(new Date());
            r.setCreater(sessionController.getLoggedUser());
            getFacade().create(r);
        }
        return r;
    }

    public void setCurrent(ReportFormat current) {
        this.current = current;
        if (current != null && current.getId() != null) {
            upload = uploadController.findUpload(current);
            if (upload == null) {
                upload = new Upload();
                upload.setCategory(current);
                upload.setCreater(sessionController.getLoggedUser());
                upload.setCreatedAt(new Date());
                uploadController.saveUpload(upload);
            }
        } else {
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

    public List<ReportFormat> fillParentReportFormats() {

        List<ReportFormat> formats = new ArrayList<>();
        Map params = new HashMap();
        String jpql = "SELECT i "
                + " FROM ReportFormat i "
                + " where i.retired=false "
                + " and i.parentCategory=null ";

        jpql += " order by i.name";

        formats = ejbFacade.findByJpql(jpql, params);
        return formats;
    }

    public List<ReportFormat> fillReportFormatsForLoggedDepartmentSite(PatientReport patientReport) {
        List<ReportFormat> formats = new ArrayList<>();
        Map params = new HashMap();
        String jpql = "SELECT i "
                + " FROM ReportFormat i "
                + " WHERE i.retired = false "
                + " AND i.parentCategory IS NOT NULL "
                + " AND i.parentCategory =:rf ";

        if (sessionController.getDepartment() != null && sessionController.getDepartment().getSite() != null) {
            jpql += " AND i.institution = :site";
            params.put("site", sessionController.getDepartment().getSite());
        }

        ReportFormat  parentCategory = ejbFacade.find(patientReport.getPatientInvestigation().getInvestigation().getReportFormat().getId());
        params.put("rf", parentCategory  );

        jpql += " ORDER BY i.orderNo ASC";

        try {
            formats = ejbFacade.findByJpql(jpql, params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return formats;
    }

    public StreamedContent getImage() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
            return DefaultStreamedContent.builder().contentType("image/png").stream(() -> new ByteArrayInputStream(new byte[0])).build();
        } else if (getUpload() == null || getUpload().getBaImage() == null) {
            // Return a placeholder StreamedContent when no image is available
            return DefaultStreamedContent.builder().contentType("image/png").stream(() -> new ByteArrayInputStream(new byte[0])).build();
        } else {
            String imageType = getUpload().getFileType();
            if (imageType == null || imageType.trim().isEmpty()) {
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

    private List<ReportFormat> parentFormat;

    public List<ReportFormat> getParentFormat() {
        if (parentFormat == null) {
            String sql = "SELECT i FROM ReportFormat i where i.retired=false and i.parentCategory=null order by i.name";
            parentFormat = getEjbFacade().findByJpql(sql);
        }
        return parentFormat;
    }

    public void setParentFormat(List<ReportFormat> parentFormat) {
        this.parentFormat = parentFormat;
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

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getViewImageType() {
        return viewImageType;
    }

    public void setViewImageType(String viewImageType) {
        this.viewImageType = viewImageType;
    }

    /**
     *
     */
}
