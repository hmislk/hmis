package com.divudi.bean.common;

import com.divudi.core.data.UploadType;
import com.divudi.core.entity.Upload;
import com.divudi.core.facade.UploadFacade;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 * Streams PVC card background images (front/back) independently of the
 * {@code @ViewScoped} PvcCardLayoutController. PrimeFaces's dynamic image
 * streaming issues a separate HTTP resource request for the image bytes,
 * and that secondary request cannot reliably re-resolve a
 * {@code cc.attrs.*} composite-component-attribute expression, nor
 * reactivate a {@code @ViewScoped} bean's context. This mirrors the
 * already-working {@link UploadViewController#getCategoryUploadById()}
 * pattern: {@code @RequestScoped}, identifying value read from the raw
 * request parameter map (baked into the resource URL once via
 * {@code f:param} during the initial render), not via EL/cc.attrs
 * navigation.
 */
@Named(value = "pvcCardBackgroundViewController")
@RequestScoped
public class PvcCardBackgroundViewController {

    @EJB
    UploadFacade uploadFacade;

    public StreamedContent getStream() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return new DefaultStreamedContent();
        }
        String side = context.getExternalContext().getRequestParameterMap().get("side");
        UploadType type = "back".equals(side) ? UploadType.PVC_Card_Back_Background : UploadType.PVC_Card_Front_Background;
        Upload upload = findUploadByType(type);
        if (upload == null || upload.getBaImage() == null) {
            return new DefaultStreamedContent();
        }
        byte[] bytes = upload.getBaImage();
        String contentType = upload.getFileType() != null ? upload.getFileType() : "image/png";
        InputStream targetStream = new ByteArrayInputStream(bytes);
        return DefaultStreamedContent.builder()
                .contentType(contentType)
                .stream(() -> targetStream)
                .build();
    }

    private Upload findUploadByType(UploadType type) {
        String jpql = "select u from Upload u where u.retired=:ret and u.uploadType=:ut order by u.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("ut", type);
        return uploadFacade.findFirstByJpql(jpql, m);
    }
}
