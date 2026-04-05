package com.divudi.ejb;

import com.divudi.core.data.lab.TestHistoryType;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Sms;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.LabTestHistory;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.entity.lab.PatientSampleComponant;
import com.divudi.core.facade.lab.LabTestHistoryFacade;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Stateless EJB service for recording lab test history events. Can be used
 * from both web (session) contexts and background/timer contexts.
 *
 * @author H.K. Damith Deshan
 */
@Stateless
public class LabTestHistoryService {

    @EJB
    private LabTestHistoryFacade labTestHistoryFacade;

    // <editor-fold defaultstate="collapsed" desc="Billing">
    public void addBillingHistory(PatientInvestigation pi, Department billingDepartment, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.ORDERED, billingDepartment, null, pi, null, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Cancel">
    public void addCancelHistory(PatientInvestigation pi, Department billingDepartment, String comment, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.CANCELED, billingDepartment, null, pi, null, null, null, null, null, null, null, null, comment, institution, department, createdBy);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Refund">
    public void addRefundHistory(PatientInvestigation pi, Department billingDepartment, String comment, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.REFUNDED, billingDepartment, null, pi, null, null, null, null, null, null, null, null, comment, institution, department, createdBy);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Sample History">
    public void addBarcodeGenerateHistory(PatientInvestigation pi, PatientSample ps, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.BARCODE_GENERATED, null, null, pi, null, ps, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addBarcodeViewHistory(PatientInvestigation pi, PatientSample ps, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.VIEW_BARCODE, null, null, pi, null, ps, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addPrintBarcodeHistory(PatientInvestigation pi, PatientSample ps, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.PRINT_BARCODE, null, null, pi, null, ps, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addSampleSeparateAndCreateHistory(PatientInvestigation pi, PatientSample ps, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.SEPARATE_AND_BARCODE_GENERATED, null, null, pi, null, ps, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addSampleSeparate(PatientInvestigation pi, PatientSample ps, String separateReason, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.SAMPLE_SEPARATE, null, null, pi, null, ps, null, null, null, null, null, null, separateReason, institution, department, createdBy);
    }

    public void addSampleReGenerateHistory(PatientInvestigation pi, PatientSample ps, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.BARCODE_REGENERATED, null, null, pi, null, ps, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addSampleCollectHistory(PatientInvestigation pi, PatientSample ps, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.SAMPLE_COLLECTED, null, null, pi, null, ps, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addSampleReCollectHistory(PatientInvestigation pi, PatientSample ps, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.SAMPLE_RECOLLECTED, null, null, pi, null, ps, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addSampleSentHistory(PatientInvestigation pi, PatientSample ps, Staff sampleTransporter, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.SAMPLE_SENT, null, null, pi, null, ps, sampleTransporter, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addSampleOutLabSentHistory(PatientInvestigation pi, PatientSample ps, Staff sampleTransporter, Department fromDepartment, Department toDepartment, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.SAMPLE_SENT_OUT_LAB, fromDepartment, toDepartment, pi, null, ps, sampleTransporter, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addSampleInternalLabSentHistory(PatientInvestigation pi, PatientSample ps, Staff sampleTransporter, Department fromDepartment, Department toDepartment, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.SAMPLE_SENT_INTERNAL_LAB, fromDepartment, toDepartment, pi, null, ps, sampleTransporter, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addSampleRetrievingHistory(PatientInvestigation pi, PatientSample ps, String comment, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.SAMPLE_RETRIEVING, null, null, pi, null, ps, null, null, null, null, null, null, comment, institution, department, createdBy);
    }

    public void addSampleReceiveHistory(PatientInvestigation pi, PatientSample ps, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.SAMPLE_RECEIVED, null, null, pi, null, ps, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addSampleRejectHistory(PatientInvestigation pi, PatientSample ps, String comment, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.SAMPLE_REJECTED, null, null, pi, null, ps, null, null, null, null, null, null, comment, institution, department, createdBy);
    }

    public void addSampleReCollectRequestHistory(PatientInvestigation pi, PatientSample ps, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.SAMPLE_RECOLLECT_REQUEST, null, null, pi, null, ps, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addBypassBarcodeGeneratAndReportCreateHistory(PatientInvestigation pi, PatientReport pr, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.BYPASS_BARCODE_GENERAT_AND_REPORT_CREATED, null, null, pi, pr, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Report History">
    public void addCreateReportHistory(PatientInvestigation pi, PatientReport pr, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.REPORT_CREATED, null, null, pi, pr, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addDataEnterHistory(PatientInvestigation pi, PatientReport pr, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.DATA_ENTERED, null, null, pi, pr, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addCalculateHistory(PatientInvestigation pi, PatientReport pr, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.REPORT_CALCULATED, null, null, pi, pr, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addApprovalHistory(PatientInvestigation pi, PatientReport pr, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.REPORT_APPROVED, null, null, pi, pr, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addApprovalCancelHistory(PatientInvestigation pi, PatientReport pr, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.REPORT_APPROVED_CANCEL, null, null, pi, pr, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addReportRemoveHistory(PatientInvestigation pi, PatientReport pr, String reason, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.REPORT_REMOVE, null, null, pi, pr, null, null, null, null, null, null, null, reason, institution, department, createdBy);
    }
    
    public void addParientDetailsEditHistory(PatientInvestigation pi, PatientReport pr, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.PATIENT_DETAILS_CHANGE, null, null, pi, pr, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }
    
    public void addReCalculateDynamicLabelHistory(PatientInvestigation pi, PatientReport pr, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.RECALCULATE_DYNAMICLABEL, null, null, pi, pr, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Report Sent - SMS">
    public void addReportCreateSentSMSHistory(PatientInvestigation pi, PatientReport pr, Sms sms) {
        addNewHistoryWithOutUser(TestHistoryType.CREATE_SMS_AUTO, null, null, pi, pr, null, null, sms, null, null, null, null, null);
    }

    public void addReportSentSMSToPatientHistory(PatientInvestigation pi, PatientReport pr, Sms sms) {
        addNewHistoryWithOutUser(TestHistoryType.SENT_SMS_AUTO, null, null, pi, pr, null, null, sms, null, null, null, null, null);
    }

    public void addReportCreateSentManualSMSHistory(PatientInvestigation pi, PatientReport pr, Sms sms, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.CREATE_SMS_MANUAL, null, null, pi, pr, null, null, sms, null, null, null, null, null, institution, department, createdBy);
    }

    public void addReportSentManualSMSHistory(PatientInvestigation pi, PatientReport pr, Sms sms, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.SENT_SMS_MANUAL, null, null, pi, pr, null, null, sms, null, null, null, null, null, institution, department, createdBy);
    }

    public void addSentSMSFailureHistory(PatientInvestigation pi, PatientReport pr, Sms sms, String failureReason) {
        addNewHistoryWithOutUser(TestHistoryType.SENT_SMS_FAIL, null, null, pi, pr, null, null, sms, null, null, null, null, failureReason);
    }

    public void addResentFailureSMSHistory(PatientInvestigation pi, PatientReport pr, Sms sms, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.RESENT_FAIL_SMS, null, null, pi, pr, null, null, sms, null, null, null, null, null, institution, department, createdBy);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Report Sent - Email">
    public void addReportCreateEmailHistory(PatientInvestigation pi, PatientReport pr, AppEmail email) {
        addNewHistoryWithOutUser(TestHistoryType.CREATE_EMAIL, null, null, pi, pr, null, null, null, email, null, null, null, null);
    }

    public void addReportSentEmailHistory(PatientInvestigation pi, PatientReport pr, AppEmail email) {
        addNewHistoryWithOutUser(TestHistoryType.SENT_EMAIL, null, null, pi, pr, null, null, null, email, null, null, null, null);
    }

    public void addSentEmailFailureHistory(PatientInvestigation pi, PatientReport pr, AppEmail email, String failureReason) {
        addNewHistoryWithOutUser(TestHistoryType.SENT_EMAIL_FAIL, null, null, pi, pr, null, null, null, email, null, null, null, failureReason);
    }

    public void addResentFailureEmailHistory(PatientInvestigation pi, PatientReport pr, AppEmail email, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.RESENT_EMAIL, null, null, pi, pr, null, null, null, email, null, null, null, null, institution, department, createdBy);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Report View & Print">
    public void addReportViewHistory(PatientInvestigation pi, PatientReport pr, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.REPORT_VIEWED, null, null, pi, pr, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addReportPrintHistory(PatientInvestigation pi, PatientReport pr, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.REPORT_PRINTED, null, null, pi, pr, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Report Issue">
    public void addReportIssueToPatientHistory(PatientInvestigation pi, PatientReport pr, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.REPORT_ISSUE_PATIENT, null, null, pi, pr, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addReportIssuetoStaffHistory(PatientInvestigation pi, PatientReport pr, Staff issueToStaff, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.REPORT_ISSUE_STAFF, null, null, pi, pr, null, issueToStaff, null, null, null, null, null, null, institution, department, createdBy);
    }

    public void addExportPDFReportHistory(PatientInvestigation pi, PatientReport pr, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.REPORT_EXPORT_AS_PDF, null, null, pi, pr, null, null, null, null, null, null, null, null, institution, department, createdBy);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Data Receive from Analyzer">
    public void addDataReciveFromAnalyzerHistory(PatientInvestigation pi, PatientReport pr, Category analyzer, String analyzerMessage, Institution institution, Department department, WebUser createdBy) {
        addNewHistoryWithUser(TestHistoryType.RESULT_RECEIVED_FROM_ANALYZER, null, null, pi, pr, null, null, null, null, null, analyzer, analyzerMessage, null, institution, department, createdBy);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Core Methods">
    public void addNewHistoryWithUser(
            TestHistoryType testHistoryType,
            Department fromDepartment,
            Department toDepartment,
            PatientInvestigation patientInvestigation,
            PatientReport patientReport,
            PatientSample patientSample,
            Staff staff,
            Sms sms,
            AppEmail email,
            PatientSampleComponant sampleComponant,
            Category analyzer,
            String analyzerMessage,
            String comment,
            Institution institution,
            Department department,
            WebUser createdBy
    ) {
        LabTestHistory history = new LabTestHistory();
        history.setTestHistoryType(testHistoryType);
        history.setFromDepartment(fromDepartment);
        history.setToDepartment(toDepartment);
        history.setPatientInvestigation(patientInvestigation);
        history.setPatientReport(patientReport);
        history.setPatientSample(patientSample);
        history.setStaff(staff);
        history.setSms(sms);
        history.setEmail(email);
        history.setSampleComponant(sampleComponant);
        history.setAnalyzer(analyzer);
        history.setAnalyzerReceiveMessage(analyzerMessage);
        history.setComment(comment);
        history.setInstitution(institution);
        history.setDepartment(department);
        history.setCreatedBy(createdBy);
        history.setCreatedAt(new Date());
        try {
            labTestHistoryFacade.create(history);
        } catch (Exception e) {
            Logger.getLogger(LabTestHistoryService.class.getName()).log(Level.SEVERE, "Failed to save lab test history: " + e.getMessage(), e);
        }
    }

    public void addNewHistoryWithOutUser(
            TestHistoryType testHistoryType,
            Department fromDepartment,
            Department toDepartment,
            PatientInvestigation patientInvestigation,
            PatientReport patientReport,
            PatientSample patientSample,
            Staff staff,
            Sms sms,
            AppEmail email,
            PatientSampleComponant sampleComponant,
            Category analyzer,
            String analyzerMessage,
            String comment
    ) {
        LabTestHistory history = new LabTestHistory();
        history.setTestHistoryType(testHistoryType);
        history.setFromDepartment(fromDepartment);
        history.setToDepartment(toDepartment);
        history.setPatientInvestigation(patientInvestigation);
        history.setPatientReport(patientReport);
        history.setPatientSample(patientSample);
        history.setStaff(staff);
        history.setSms(sms);
        history.setEmail(email);
        history.setSampleComponant(sampleComponant);
        history.setAnalyzer(analyzer);
        history.setAnalyzerReceiveMessage(analyzerMessage);
        history.setComment(comment);
        history.setCreatedAt(new Date());
        try {
            labTestHistoryFacade.create(history);
        } catch (Exception e) {
            Logger.getLogger(LabTestHistoryService.class.getName()).log(Level.SEVERE, "Failed to save lab test history: " + e.getMessage(), e);
        }
    }
    // </editor-fold>
}
