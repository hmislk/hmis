package com.divudi.core.data;

import java.io.Serializable;

/**
 * A file attachment for outbound emails sent through the messenger REST
 * gateway. Content is carried as a base64 string because the gateway
 * contract is JSON.
 */
public class EmailAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileName;
    private String contentType;
    private String base64Content;

    public EmailAttachment() {
    }

    public EmailAttachment(String fileName, String contentType, String base64Content) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.base64Content = base64Content;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBase64Content() {
        return base64Content;
    }

    public void setBase64Content(String base64Content) {
        this.base64Content = base64Content;
    }
}
