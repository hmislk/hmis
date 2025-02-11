package com.divudi.bean.lab;

import com.divudi.entity.lab.PatientReportItemValue;
import com.divudi.facade.PatientReportItemValueFacade;
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
            return DefaultStreamedContent.builder().build();
        }

        // Retrieve image ID from request parameters
        String id = context.getExternalContext().getRequestParameterMap().get("id");

        if (id == null || id.isEmpty()) {
            return DefaultStreamedContent.builder().build();
        }

        Long imgId;
        try {
            imgId = Long.valueOf(id);
        } catch (NumberFormatException e) {
            return DefaultStreamedContent.builder().build();
        }

        PatientReportItemValue priv = null;
        priv = findPatientReportItemValueFromId(imgId);

        if (priv == null) {
            return DefaultStreamedContent.builder().build();
        }

        if (priv.getBaImage() == null) {
             return DefaultStreamedContent.builder().build();
        }

        // Fetch the byte array corresponding to the given ID
        byte[] imageData = priv.getBaImage();

        if (imageData == null || imageData.length == 0) {
            return DefaultStreamedContent.builder().build();
        }

        InputStream targetStream = new ByteArrayInputStream(imageData);
        return DefaultStreamedContent.builder()
                .stream(() -> targetStream)
                .contentType(priv.getFileType()) // Adjust content type as needed
                .build();
    }


    private PatientReportItemValue findPatientReportItemValueFromId(Long id) {
        return patientReportItemValueFacade.find(id);
    }
}
