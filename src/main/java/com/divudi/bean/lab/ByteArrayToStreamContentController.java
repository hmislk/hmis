package com.divudi.bean.lab;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Buddhika and Damith with the help of chatgpt
 *
 */
@Named(value = "byteArrayToStreamContentController")
@RequestScoped
public class ByteArrayToStreamContentController {

    /**
     * Creates a new instance of ByteArrayToStreamContentController
     */
    public ByteArrayToStreamContentController() {
    }

    public StreamedContent getImageStream(byte[] imageData, String contentType) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse()) {
            return DefaultStreamedContent.builder().build();
        }

        if (imageData == null || imageData.length == 0) {
            return DefaultStreamedContent.builder().build();
        }

        InputStream inputStream = new ByteArrayInputStream(imageData);
        return DefaultStreamedContent.builder()
                .stream(() -> inputStream)
                .contentType(contentType != null ? contentType : "image/png") // Adjust content type if necessary
                .build();
    }

}
