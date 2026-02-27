package com.divudi.core.data.lab;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com> and H.K. Damith Deshan
 * <hkddrajapaksha@gmail.com>
 *
 */
/**
 * Enum representing different stages of laboratory test history. ChatGPT
 * contributed.
 */
public enum TestHistoryType {
    ORDERED,
    CANCELED,
    REFUNDED,
    SAMPLE_SEPARATE,
    SEPARATE_AND_BARCODE_GENERATED,
    BARCODE_GENERATED,
    VIEW_BARCODE,
    PRINT_BARCODE,
    BARCODE_REGENERATED,
    BARCODE_PRINTED,
    BARCODE_PRINTED_INDIVIDUAL,
    SAMPLE_COLLECTED,
    SAMPLE_RECOLLECT_REQUEST,
    SAMPLE_RECOLLECTED,
    SAMPLE_ACCEPTED,
    SAMPLE_SENT,
    SAMPLE_SENT_OUT_LAB,
    SAMPLE_SENT_INTERNAL_LAB,
    SAMPLE_RETRIEVING,
    SAMPLE_REJECTED,
    SAMPLE_RECEIVED,
    SAMPLE_TRANSFERRED,
    BYPASS_BARCODE_GENERAT_AND_REPORT_CREATED,
    SAMPLE_PUT_TO_ANALYZER,
    RESULT_RECEIVED_FROM_ANALYZER,
    REPORT_CREATED,
    DATA_ENTERED,
    REPORT_UPLOADED,
    REMOVED_UPLOADED_REPORT,
    REPORT_CALCULATED,
    REPORT_APPROVED,
    REPORT_APPROVED_CANCEL,
    REPORT_VIEWED,
    REPORT_PRINTED,
    REPORT_EXPORT_AS_PDF,
    REPORT_REMOVE,
    REPORT_ISSUE_PATIENT,
    REPORT_ISSUE_STAFF,
    SENT_SMS_MANUAL,
    SENT_SMS_AUTO,
    SENT_EMAIL;
    
    
    // <editor-fold defaultstate="collapsed" desc="Extra Need to Add Enum">
    
//    1. Bill Request (Cancel/Refund)
//    2. Request Cancel   
//    3. Request Approvel
//    4. Request Reject
//    5. Request Approvel Cancel
    
    // </editor-fold>

    public String getLabel() {
        switch (this) {
            case ORDERED:
                return "Ordered";
            case CANCELED:
                return "Cancelled";
            case REFUNDED:
                return "Refunded";
            case BARCODE_GENERATED:
                return "Barcode Generated";
            case VIEW_BARCODE:
                return "View Barcode";
            case PRINT_BARCODE:
                return "Print Barcode";
            case SEPARATE_AND_BARCODE_GENERATED:
                return "Separate and Barcode Generated";
            case SAMPLE_SEPARATE:
                return "Sample Separate";
            case BARCODE_REGENERATED:
                return "Barcode Regenerated";
            case BARCODE_PRINTED:
                return "Barcode Printed";
            case BARCODE_PRINTED_INDIVIDUAL:
                return "Barcode Printed (Individual)";
            case SAMPLE_COLLECTED:
                return "Sample Collected";
            case SAMPLE_RECOLLECT_REQUEST:
                return "Sample Recollect Request";
            case SAMPLE_RECOLLECTED:
                return "Sample Recollected";
            case SAMPLE_ACCEPTED:
                return "Sample Accepted";
            case SAMPLE_SENT:
                return "Sample Sent";
            case SAMPLE_RETRIEVING:
                return "Receiving the sent sample";
            case SAMPLE_SENT_OUT_LAB:
                return "Sample Sent to External Lab";
            case SAMPLE_SENT_INTERNAL_LAB:
                return "Sample Sent to Internal Lab";
            case SAMPLE_REJECTED:
                return "Sample Rejected";
            case SAMPLE_RECEIVED:
                return "Sample Received";
            case SAMPLE_TRANSFERRED:
                return "Sample Transferred";
            case SAMPLE_PUT_TO_ANALYZER:
                return "Sample Put to Analyzer";
            case RESULT_RECEIVED_FROM_ANALYZER:
                return "Result Received from Analyzer";
            case BYPASS_BARCODE_GENERAT_AND_REPORT_CREATED:
                return "Sample Process and Report Created";
            case REPORT_CREATED:
                return "Report Created";
            case DATA_ENTERED:
                return "Data Entered";
            case REPORT_UPLOADED:
                return "Report Uploaded";
            case REMOVED_UPLOADED_REPORT:
                return "Uploaded Report Removed";
            case REPORT_CALCULATED:
                return "Flags Generate";
            case REPORT_APPROVED:
                return "Report Approved";
            case REPORT_APPROVED_CANCEL:
                return "Report Approval Canceled";
            case REPORT_REMOVE:
                return "Delete Report";
            case REPORT_VIEWED:
                return "Report Viewed";
            case REPORT_PRINTED:
                return "Report Printed";
            case REPORT_EXPORT_AS_PDF:
                return "Report Export as PDF";
            case REPORT_ISSUE_PATIENT:
                return "Report Issue to Patient";
            case REPORT_ISSUE_STAFF:
                return "Report Issue to Staff";    
            case SENT_SMS_MANUAL:
                return "SMS Sent (Manual)";
            case SENT_SMS_AUTO:
                return "SMS Sent (Auto)";
            case SENT_EMAIL:
                return "Email Sent";
            default:
                return this.toString();
        }
    }

}
