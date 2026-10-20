package com.divudi.bean.common;

import com.divudi.core.data.UploadType;
import com.divudi.core.entity.Upload;
import com.divudi.core.facade.UploadFacade;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;

import com.divudi.core.util.CommonFunctions;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named(value = "uploadViewController")
@RequestScoped
public class UploadViewController {

    @EJB
    UploadFacade uploadFacade;

    /**
     * Creates a new instance of UploadViewController
     */
    public UploadViewController() {
    }

    public StreamedContent getCategoryUploadById() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return new DefaultStreamedContent();
        } else {
            String id = context.getExternalContext().getRequestParameterMap().get("id");
            String page = context.getExternalContext().getRequestParameterMap().get("page");
            Upload upload = findCategoryUploadById(id, CommonFunctions.convertStringToLongOrZero(page).intValue());
            if (upload != null) {
                byte[] imgArr = upload.getBaImage();
                if (imgArr == null) {
                    return new DefaultStreamedContent();
                }

                InputStream targetStream = new ByteArrayInputStream(imgArr);
                String fileType = upload.getFileType() != null ? upload.getFileType() : "application/pdf";
                String fileName = upload.getFileName() != null ? upload.getFileName() : id + ".png";

                return DefaultStreamedContent.builder()
                        .contentType(fileType)
                        .name(fileName)
                        .stream(() -> targetStream)
                        .build();
            } else {
                return new DefaultStreamedContent();
            }
        }
    }

    /**
     * Background of a report format for a page. Page 1, and any page without
     * its own background, resolves to the format's ordinary background exactly
     * as before pages existed; page-specific rows are kept apart by upload type.
     */
    private Upload findCategoryUploadById(String id, int page) {
        Long lid = CommonFunctions.convertStringToLongOrZero(id);
        Map m = new HashMap();
        m.put("ret", false);
        m.put("cid", lid);
        if (page > 1) {
            String pageJpql = "select u "
                    + " from Upload u "
                    + " where u.retired=:ret "
                    + " and u.category.id=:cid "
                    + " and u.uploadType=:pb "
                    + " and u.comments=:pg "
                    + " order by u.id desc";
            Map pm = new HashMap(m);
            pm.put("pb", UploadType.Lab_Report_Page_Background);
            pm.put("pg", "page=" + page);
            Upload pageUpload = uploadFacade.findFirstByJpql(pageJpql, pm);
            if (pageUpload != null
                    && (pageUpload.getBaImage() != null
                    || (pageUpload.getFileUrl() != null && !pageUpload.getFileUrl().trim().isEmpty()))) {
                return pageUpload;
            }
        }
        String jpql = "select u "
                + " from Upload u "
                + " where u.retired=:ret "
                + " and u.category.id=:cid "
                + " and (u.uploadType is null or u.uploadType<>:pb) "
                + " order by u.id desc";
        m.put("pb", UploadType.Lab_Report_Page_Background);
        return uploadFacade.findFirstByJpql(jpql, m);
    }

    public String getReportTemplate(String id) {
        return getReportTemplate(id, 1);
    }

    public String getReportTemplate(String id, int page) {
        Upload u = findCategoryUploadById(id, page);

        String url = "";
        if (u == null) {

        } else {
            url = u.getFileUrl();
        }
        return url;
    }

}
