package com.divudi.bean.common;

import com.divudi.entity.Patient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.inject.Named;
import java.io.Serializable;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@RequestScoped
public class ImageController implements Serializable {

    @Inject
    PatientController patientController;

    public ImageController() {
    }

    public StreamedContent getPhotoById() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            // So, we're rendering the view. Return a stub StreamedContent so that it will generate right URL.
            return new DefaultStreamedContent();
        } else {
            // So, browser is requesting the image. Get ID value from actual request param.
            String id = context.getExternalContext().getRequestParameterMap().get("id");
            Patient pt = patientController.findPatientById(id);
            if (pt != null) {
                String imageType = pt.getFileType();
                if (imageType == null || imageType.trim().equals("")) {
                    imageType = "image/png";
                }
                return DefaultStreamedContent.builder()
                        .contentType(imageType)
                        .name(pt.getFileName())
                        .stream(() -> new ByteArrayInputStream(pt.getBaImage()))
                        .build();
            } else {
                return new DefaultStreamedContent();
            }
        }
    }

    public StreamedContent getPatientImage(Patient patient) throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
            // Return a stub StreamedContent so that it will generate the correct URL.
            return DefaultStreamedContent.builder().build();
        } else {
            // Streaming the actual image content.
            if (patient == null || patient.getBaImage() == null) {
                return DefaultStreamedContent.builder().build();
            } else {
                String imageType = patient.getFileType();
                if (imageType == null || imageType.trim().equals("")) {
                    imageType = "image/png";
                }
                return DefaultStreamedContent.builder()
                        .contentType(imageType)
                        .stream(() -> new ByteArrayInputStream(patient.getBaImage()))
                        .build();
            }
        }
    }

}
