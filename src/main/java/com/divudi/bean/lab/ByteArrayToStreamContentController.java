package com.divudi.bean.lab;

import com.divudi.core.entity.lab.PatientReportItemValue;
import com.divudi.core.facade.PatientReportItemValueFacade;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

@Named(value = "byteArrayToStreamContentController")
@RequestScoped
public class ByteArrayToStreamContentController {

    private byte[] currentImage;
    @EJB
    PatientReportItemValueFacade patientReportItemValueFacade;

    public void setCurrentImage(byte[] imageData) {
        this.currentImage = imageData;
    }

    public StreamedContent getStreamedImage() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return DefaultStreamedContent.builder().build();
        }

        if (currentImage == null || currentImage.length == 0) {
            return DefaultStreamedContent.builder().build();
        }

        InputStream inputStream = new ByteArrayInputStream(currentImage);
        return DefaultStreamedContent.builder()
                .stream(() -> inputStream)
                .contentType("image/BMP") // Adjust if necessary
                .build();
    }

    public StreamedContent getImageById() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return null; // No content needed during render phase
        }

        String id = context.getExternalContext().getRequestParameterMap().get("id");
        if (id == null || id.isEmpty()) {
            return null;
        }

        Long imgId;
        try {
            imgId = Long.valueOf(id);
        } catch (NumberFormatException e) {
            return null;
        }

        PatientReportItemValue priv = findPatientReportItemValueFromId(imgId);
        if (priv == null || priv.getBaImage() == null || priv.getBaImage().length == 0) {
            return null;
        }

        byte[] imageData = priv.getBaImage();
        InputStream targetStream = new ByteArrayInputStream(imageData);

        String fileType = priv.getFileType();
        if (fileType == null || fileType.isEmpty()) {
            fileType = "image/png"; // default to PNG if undefined
        } else {
            fileType = "image/" + fileType.toLowerCase();
        }

        return DefaultStreamedContent.builder()
                .stream(() -> targetStream)
                .contentType(fileType)
                .build();
    }

    private PatientReportItemValue findPatientReportItemValueFromId(Long id) {
        return patientReportItemValueFacade.find(id);
    }
}
