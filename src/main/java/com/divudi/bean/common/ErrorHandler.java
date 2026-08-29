package com.divudi.bean.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import com.divudi.core.util.CommonFunctions;

/**
 * Error Handler Bean
 * Created with assistance from ChatGPT by Dr M H B Ariyaratne
 */
@Named
@RequestScoped
public class ErrorHandler implements Serializable {
    
    private final LocalDateTime errorTime;
    
    public ErrorHandler() {
        this.errorTime = LocalDateTime.now();
    }

    public String getStatusCode() {
        Object code = FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().get("javax.servlet.error.status_code");
        return code != null ? code.toString() : "N/A";
    }

    public String getExceptionType() {
        Object type = FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().get("javax.servlet.error.exception_type");
        return type != null ? type.toString() : "N/A";
    }

    public String getMessage() {
        Object msg = FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().get("javax.servlet.error.message");
        return msg != null ? msg.toString() : "N/A";
    }

    public String getRequestURI() {
        Object uri = FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().get("javax.servlet.error.request_uri");
        return uri != null ? uri.toString() : "N/A";
    }

    public String getException() {
        Throwable t = (Throwable) FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().get("javax.servlet.error.exception");
        return t != null ? t.toString() : "N/A";
    }

    public String getStackTrace() {
        Throwable t = (Throwable) FacesContext.getCurrentInstance().getExternalContext()
                .getRequestMap().get("javax.servlet.error.exception");
        if (t == null) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder();
        appendStackTrace(t, sb);
        return sb.toString();
    }

    private void appendStackTrace(Throwable t, StringBuilder sb) {
        // Append the main exception class and message
        sb.append(t.getClass().getName())
          .append(": ")
          .append(CommonFunctions.escapeHtml(t.getMessage()))
          .append("\n");

        // Append stack trace elements
        for (StackTraceElement element : t.getStackTrace()) {
            sb.append("    at ")
              .append(CommonFunctions.escapeHtml(element.toString()))
              .append("\n");
        }

        // Append cause if present
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            sb.append("Caused by: ");
            appendStackTrace(cause, sb);
        }
    }
    
    public String getErrorTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return errorTime.format(formatter);
    }
    
    public String getErrorTimeWithTimezone() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return errorTime.format(formatter) + " (Server Time)";
    }

}
