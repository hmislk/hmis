package com.divudi.bean.common;

import com.divudi.core.data.PvcCardLayout;
import com.divudi.core.data.PvcCardSlot;
import com.divudi.core.data.UploadType;
import com.divudi.core.entity.Upload;
import com.divudi.core.facade.UploadFacade;
import com.divudi.core.util.JsfUtil;
import org.apache.commons.io.IOUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Named
@ViewScoped
public class PvcCardLayoutController implements Serializable {

    public static final String KEY_FRONT_LAYOUT = "PVC Card Front Layout";
    public static final String KEY_BACK_LAYOUT = "PVC Card Back Layout";

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private SessionController sessionController;
    @EJB
    private UploadFacade uploadFacade;

    private PvcCardLayout front;
    private PvcCardLayout back;
    private String frontUrlInput;
    private String backUrlInput;

    @PostConstruct
    public void init() {
        front = PvcCardLayout.fromJson(configOptionApplicationController.getLongTextValueByKeyReadOnly(KEY_FRONT_LAYOUT, ""));
        back = PvcCardLayout.fromJson(configOptionApplicationController.getLongTextValueByKeyReadOnly(KEY_BACK_LAYOUT, ""));
        frontUrlInput = backgroundUrl(UploadType.PVC_Card_Front_Background);
        backUrlInput = backgroundUrl(UploadType.PVC_Card_Back_Background);
    }

    public PvcCardLayout getFront() {
        return front;
    }

    public PvcCardLayout getBack() {
        return back;
    }

    public void saveFrontLayout() {
        if (!isValid(front)) {
            return;
        }
        configOptionApplicationController.setLongTextValueByKey(KEY_FRONT_LAYOUT, front.toJson());
        JsfUtil.addSuccessMessage("Front layout saved");
    }

    public void saveBackLayout() {
        if (!isValid(back)) {
            return;
        }
        configOptionApplicationController.setLongTextValueByKey(KEY_BACK_LAYOUT, back.toJson());
        JsfUtil.addSuccessMessage("Back layout saved");
    }

    private boolean isValid(PvcCardLayout layout) {
        if (layout.getWidthMm() <= 0 || layout.getHeightMm() <= 0) {
            JsfUtil.addErrorMessage("Card width and height must be greater than zero");
            return false;
        }
        if (layout.getMarginMm() < 0) {
            JsfUtil.addErrorMessage("Margin cannot be negative");
            return false;
        }
        for (Map.Entry<String, PvcCardSlot> entry : layout.getSlots().entrySet()) {
            PvcCardSlot slot = entry.getValue();
            if (!slot.isVisible()) {
                continue;
            }
            if (slot.getFontSizePt() <= 0) {
                JsfUtil.addErrorMessage("Font size for '" + entry.getKey() + "' must be greater than zero");
                return false;
            }
            if (slot.getLeftMm() < 0 || slot.getLeftMm() > layout.getWidthMm()
                    || slot.getTopMm() < 0 || slot.getTopMm() > layout.getHeightMm()) {
                JsfUtil.addErrorMessage("Position for '" + entry.getKey() + "' is outside the card bounds");
                return false;
            }
        }
        return true;
    }

    public String getFrontBackgroundExternalUrl() {
        return backgroundUrl(UploadType.PVC_Card_Front_Background);
    }

    public String getBackBackgroundExternalUrl() {
        return backgroundUrl(UploadType.PVC_Card_Back_Background);
    }

    public StreamedContent getFrontBackgroundStream() {
        return backgroundStream(UploadType.PVC_Card_Front_Background);
    }

    public StreamedContent getBackBackgroundStream() {
        return backgroundStream(UploadType.PVC_Card_Back_Background);
    }

    public String getFrontUrlInput() {
        return frontUrlInput;
    }

    public void setFrontUrlInput(String frontUrlInput) {
        this.frontUrlInput = frontUrlInput;
    }

    public String getBackUrlInput() {
        return backUrlInput;
    }

    public void setBackUrlInput(String backUrlInput) {
        this.backUrlInput = backUrlInput;
    }

    public void saveFrontBackgroundUrl() {
        saveBackgroundUrl(UploadType.PVC_Card_Front_Background, frontUrlInput);
    }

    public void saveBackBackgroundUrl() {
        saveBackgroundUrl(UploadType.PVC_Card_Back_Background, backUrlInput);
    }

    public void uploadFrontBackground(FileUploadEvent event) {
        saveBackgroundUpload(UploadType.PVC_Card_Front_Background, event);
    }

    public void uploadBackBackground(FileUploadEvent event) {
        saveBackgroundUpload(UploadType.PVC_Card_Back_Background, event);
    }

    private String backgroundUrl(UploadType type) {
        Upload upload = findUploadByType(type);
        if (upload == null || upload.getFileUrl() == null) {
            return "";
        }
        return upload.getFileUrl();
    }

    private StreamedContent backgroundStream(UploadType type) {
        Upload upload = findUploadByType(type);
        if (upload == null || upload.getBaImage() == null) {
            return null;
        }
        byte[] bytes = upload.getBaImage();
        String contentType = upload.getFileType() != null ? upload.getFileType() : "image/png";
        return DefaultStreamedContent.builder()
                .contentType(contentType)
                .stream(() -> new ByteArrayInputStream(bytes))
                .build();
    }

    private void saveBackgroundUpload(UploadType type, FileUploadEvent event) {
        try {
            Upload upload = existingOrNewUpload(type);
            try (InputStream input = event.getFile().getInputStream()) {
                byte[] bytes = IOUtils.toByteArray(input);
                upload.setBaImage(bytes);
            }
            upload.setFileName(event.getFile().getFileName());
            upload.setFileType(event.getFile().getContentType());
            upload.setFileUrl(null);
            persist(upload);
            JsfUtil.addSuccessMessage("Background image uploaded");
        } catch (IOException ex) {
            JsfUtil.addErrorMessage("Upload failed");
        }
    }

    private void saveBackgroundUrl(UploadType type, String url) {
        Upload upload = existingOrNewUpload(type);
        upload.setFileUrl(url);
        upload.setBaImage(null);
        persist(upload);
        JsfUtil.addSuccessMessage("Background URL saved");
    }

    private Upload existingOrNewUpload(UploadType type) {
        Upload upload = findUploadByType(type);
        if (upload == null) {
            upload = new Upload();
            upload.setUploadType(type);
            upload.setCreater(sessionController.getLoggedUser());
            upload.setCreatedAt(new Date());
        }
        return upload;
    }

    private void persist(Upload upload) {
        if (upload.getId() == null) {
            uploadFacade.create(upload);
        } else {
            uploadFacade.edit(upload);
        }
    }

    private Upload findUploadByType(UploadType type) {
        String jpql = "select u from Upload u where u.retired=:ret and u.uploadType=:ut order by u.id desc";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("ut", type);
        return uploadFacade.findFirstByJpql(jpql, m);
    }
}
