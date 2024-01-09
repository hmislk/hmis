package com.divudi.bean.common;

import com.divudi.entity.Staff;
import com.divudi.entity.Upload;
import com.divudi.facade.UploadFacade;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
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
            Upload upload = findCategoryUploadById(id);
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

    private Upload findCategoryUploadById(String id) {
        Long lid = CommonController.convertStringToLong(id);
        String jpql = "select u "
                + " from Upload u "
                + " where u.retired=:ret "
                + " and u.category.id=:cid "
                + " order by u.id desc";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("cid", lid);
        return uploadFacade.findFirstByJpql(jpql, m);
    }

}
