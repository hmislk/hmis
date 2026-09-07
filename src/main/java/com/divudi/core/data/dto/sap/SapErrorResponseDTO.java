/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.sap;

/**
 * SAP S/4HANA Cloud OData error envelope (error path).
 * SAP wraps errors as: {"error":{"code":"...","message":{"value":"..."}}}
 */
public class SapErrorResponseDTO {

    private SapODataError error;

    public static class SapODataError {
        private String code;
        private SapODataMessage message;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public SapODataMessage getMessage() { return message; }
        public void setMessage(SapODataMessage message) { this.message = message; }
    }

    public static class SapODataMessage {
        private String value;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public SapErrorResponseDTO() {
    }

    public SapODataError getError() { return error; }
    public void setError(SapODataError error) { this.error = error; }

    public String extractMessage() {
        if (error == null) return "Unknown SAP error";
        if (error.getMessage() != null) {
            String val = error.getMessage().getValue();
            if (val != null && !val.trim().isEmpty()) return val;
        }
        return error.getCode() != null ? error.getCode() : "Unknown SAP error";
    }
}
