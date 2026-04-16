package com.divudi.bean.lab;

import com.divudi.bean.common.EnumController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.lab.TestHistoryType;
import com.divudi.core.entity.AppEmail;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Sms;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.lab.LabTestHistory;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.entity.lab.PatientSampleComponant;
import com.divudi.core.facade.lab.LabTestHistoryFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.LabTestHistoryService;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

import javax.faces.convert.Converter;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari@gmail.com> and H.K. Damith Deshan
 * <hkddrajapaksha@gmail.com>
 *
 */
@Named(value = "labTestHistoryController")
@SessionScoped
public class LabTestHistoryController implements Serializable {

    public LabTestHistoryController() {
    }

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private LabTestHistoryFacade labTestHistoryFacade;
    @EJB
    private LabTestHistoryService labTestHistoryService;
    @Inject
    EnumController enumController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private SessionController sessionController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Variables">
    private static final long serialVersionUID = 1L;
    private LabTestHistory current;
    private List<LabTestHistory> items = null;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Navigation Method">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Function">
    // <editor-fold defaultstate="collapsed" desc="Billing">
    public void addBillingHistory(PatientInvestigation patientInvestigation, Department department) {
        labTestHistoryService.addBillingHistory(patientInvestigation, department, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Cancel">
    public void addCancelHistory(PatientInvestigation patientInvestigation, Department department, String comment) {
        labTestHistoryService.addCancelHistory(patientInvestigation, department, comment, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Refund">
    public void addRefundHistory(PatientInvestigation patientInvestigation, Department department, String comment) {
        labTestHistoryService.addRefundHistory(patientInvestigation, department, comment, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Sample History">
    public void addBarcodeGenerateHistory(PatientInvestigation patientInvestigation, PatientSample patientSample) {
        labTestHistoryService.addBarcodeGenerateHistory(patientInvestigation, patientSample, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addBarcodeViewHistory(PatientInvestigation patientInvestigation, PatientSample patientSample) {
        labTestHistoryService.addBarcodeViewHistory(patientInvestigation, patientSample, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addPrintBarcodeHistory(PatientInvestigation patientInvestigation, PatientSample patientSample) {
        labTestHistoryService.addPrintBarcodeHistory(patientInvestigation, patientSample, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSampleSeparateAndCreateHistory(PatientInvestigation patientInvestigation, PatientSample patientSample) {
        labTestHistoryService.addSampleSeparateAndCreateHistory(patientInvestigation, patientSample, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSampleSeparate(PatientInvestigation patientInvestigation, PatientSample patientSample, String separateReason) {
        labTestHistoryService.addSampleSeparate(patientInvestigation, patientSample, separateReason, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSampleReGenerateHistory(PatientInvestigation patientInvestigation, PatientSample patientSample) {
        labTestHistoryService.addSampleReGenerateHistory(patientInvestigation, patientSample, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSampleCollectHistory(PatientInvestigation patientInvestigation, PatientSample patientSample) {
        labTestHistoryService.addSampleCollectHistory(patientInvestigation, patientSample, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSampleReCollectHistory(PatientInvestigation patientInvestigation, PatientSample patientSample) {
        labTestHistoryService.addSampleReCollectHistory(patientInvestigation, patientSample, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSampleSentHistory(PatientInvestigation patientInvestigation, PatientSample patientSample, Staff sampleTransporter) {
        labTestHistoryService.addSampleSentHistory(patientInvestigation, patientSample, sampleTransporter, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSampleOutLabSentHistory(PatientInvestigation patientInvestigation, PatientSample patientSample, Staff sampleTransporter, Department fromDepartment, Department toDepartment) {
        labTestHistoryService.addSampleOutLabSentHistory(patientInvestigation, patientSample, sampleTransporter, fromDepartment, toDepartment, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSampleInternalLabSentHistory(PatientInvestigation patientInvestigation, PatientSample patientSample, Staff sampleTransporter, Department fromDepartment, Department toDepartment) {
        labTestHistoryService.addSampleInternalLabSentHistory(patientInvestigation, patientSample, sampleTransporter, fromDepartment, toDepartment, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSampleRetrievingHistory(PatientInvestigation patientInvestigation, PatientSample patientSample, String comment) {
        labTestHistoryService.addSampleRetrievingHistory(patientInvestigation, patientSample, comment, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSampleReceiveHistory(PatientInvestigation patientInvestigation, PatientSample patientSample) {
        labTestHistoryService.addSampleReceiveHistory(patientInvestigation, patientSample, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSampleRejectHistory(PatientInvestigation patientInvestigation, PatientSample patientSample, String comment) {
        labTestHistoryService.addSampleRejectHistory(patientInvestigation, patientSample, comment, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSampleReCollectRequestHistory(PatientInvestigation patientInvestigation, PatientSample patientSample) {
        labTestHistoryService.addSampleReCollectRequestHistory(patientInvestigation, patientSample, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addBypassBarcodeGeneratAndReportCreateHistory(PatientInvestigation patientInvestigation, PatientReport patientReport) {
        labTestHistoryService.addBypassBarcodeGeneratAndReportCreateHistory(patientInvestigation, patientReport, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Report History">
    public void addCreateReportHistory(PatientInvestigation patientInvestigation, PatientReport patientReport) {
        labTestHistoryService.addCreateReportHistory(patientInvestigation, patientReport, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addDataEnterHistory(PatientInvestigation patientInvestigation, PatientReport patientReport) {
        labTestHistoryService.addDataEnterHistory(patientInvestigation, patientReport, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addCalculateHistory(PatientInvestigation patientInvestigation, PatientReport patientReport) {
        labTestHistoryService.addCalculateHistory(patientInvestigation, patientReport, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addApprovalHistory(PatientInvestigation patientInvestigation, PatientReport patientReport) {
        labTestHistoryService.addApprovalHistory(patientInvestigation, patientReport, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addApprovalCancelHistory(PatientInvestigation patientInvestigation, PatientReport patientReport) {
        labTestHistoryService.addApprovalCancelHistory(patientInvestigation, patientReport, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addReportRemoveHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, String reason) {
        labTestHistoryService.addReportRemoveHistory(patientInvestigation, patientReport, reason, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }
    
    public void addParientDetailsEditHistory(PatientInvestigation patientInvestigation, PatientReport patientReport) {
        labTestHistoryService.addParientDetailsEditHistory(patientInvestigation, patientReport, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }
    
    public void addReCalculateDynamicLabelHistory(PatientInvestigation patientInvestigation, PatientReport patientReport) {
        labTestHistoryService.addReCalculateDynamicLabelHistory(patientInvestigation, patientReport, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Report Sent">
    // <editor-fold defaultstate="collapsed" desc="SMS">
    public void addReportCreateSentSMSHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, Sms sms) {
        labTestHistoryService.addReportCreateSentSMSHistory(patientInvestigation, patientReport, sms);
    }

    public void addReportSentSMSToPatientHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, Sms sms) {
        labTestHistoryService.addReportSentSMSToPatientHistory(patientInvestigation, patientReport, sms);
    }

    public void addReportCreateSentManualSMSHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, Sms sms) {
        labTestHistoryService.addReportCreateSentManualSMSHistory(patientInvestigation, patientReport, sms, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addReportSentManualSMSHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, Sms sms) {
        labTestHistoryService.addReportSentManualSMSHistory(patientInvestigation, patientReport, sms, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addSentSMSFailureHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, Sms sms, String failureReason) {
        labTestHistoryService.addSentSMSFailureHistory(patientInvestigation, patientReport, sms, failureReason);
    }

    public void addResentFailureSMSHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, Sms sms) {
        labTestHistoryService.addResentFailureSMSHistory(patientInvestigation, patientReport, sms, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Email">
    public void addReportCreateEmailHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, AppEmail email) {
        labTestHistoryService.addReportCreateEmailHistory(patientInvestigation, patientReport, email);
    }

    public void addReportSentEmailHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, AppEmail email) {
        labTestHistoryService.addReportSentEmailHistory(patientInvestigation, patientReport, email);
    }

    public void addSentEmailFailureHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, AppEmail email, String failureReason) {
        labTestHistoryService.addSentEmailFailureHistory(patientInvestigation, patientReport, email, failureReason);
    }

    public void addResentFailureEmailHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, AppEmail email) {
        labTestHistoryService.addResentFailureEmailHistory(patientInvestigation, patientReport, email, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }
    // </editor-fold>
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Report View & Print">
    public void addReportViewHistory(PatientInvestigation patientInvestigation, PatientReport patientReport) {
        labTestHistoryService.addReportViewHistory(patientInvestigation, patientReport, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addReportPrintHistory(PatientInvestigation patientInvestigation, PatientReport patientReport) {
        labTestHistoryService.addReportPrintHistory(patientInvestigation, patientReport, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Report Issue">
    public void addReportIssueToPatientHistory(PatientInvestigation patientInvestigation, PatientReport patientReport) {
        labTestHistoryService.addReportIssueToPatientHistory(patientInvestigation, patientReport, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addReportIssuetoStaffHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, Staff issueToStaff) {
        labTestHistoryService.addReportIssuetoStaffHistory(patientInvestigation, patientReport, issueToStaff, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }

    public void addExportPDFReportHistory(PatientInvestigation patientInvestigation, PatientReport patientReport) {
        labTestHistoryService.addExportPDFReportHistory(patientInvestigation, patientReport, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Data Recive from Analyzer">
    public void addDataReciveFromAnalyzerHistory(PatientInvestigation patientInvestigation, PatientReport patientReport, Category analyzer, String analyzerMessage) {
        labTestHistoryService.addDataReciveFromAnalyzerHistory(patientInvestigation, patientReport, analyzer, analyzerMessage, sessionController.getInstitution(), sessionController.getDepartment(), sessionController.getLoggedUser());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Query Methods">
    public List<LabTestHistoryLight> getCreatedLabTestHistoryByInvestigation(PatientInvestigation patientInvestigation) {
        if (patientInvestigation == null) {
            return null;
        }

        String jpql = "SELECT new com.divudi.bean.lab.LabTestHistoryLight(his.id, his.testHistoryType, his.createdAt, inst.name, dept.name, his.staff, his.createdBy, his.comment) "
                + " FROM LabTestHistory his "
                + " LEFT JOIN his.institution inst"
                + " LEFT JOIN his.department dept"
                + " WHERE his.retired=:retired "
                + " AND his.patientInvestigation =:patientInvestigation"
                + " AND his.testHistoryType =:type"
                + " order by his.createdAt asc";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("patientInvestigation", patientInvestigation);
        params.put("type", TestHistoryType.ORDERED);
        List<LabTestHistoryLight> labHistory = (List<LabTestHistoryLight>) getLabTestHistoryFacade().findLightsByJpql(jpql, params);
        return labHistory;
    }

    public List<LabTestHistoryLight> getCanceledLabTestHistoryByInvestigation(PatientInvestigation patientInvestigation) {
        if (patientInvestigation == null) {
            return null;
        }

        String jpql = "SELECT new com.divudi.bean.lab.LabTestHistoryLight(his.id, his.testHistoryType, his.createdAt, inst.name, dept.name, his.staff, his.createdBy, his.comment) "
                + " FROM LabTestHistory his "
                + " LEFT JOIN his.institution inst"
                + " LEFT JOIN his.department dept"
                + " WHERE his.retired=:retired "
                + " AND his.patientInvestigation =:patientInvestigation"
                + " AND his.testHistoryType =:type"
                + " order by his.createdAt asc";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("patientInvestigation", patientInvestigation);
        params.put("type", TestHistoryType.CANCELED);
        List<LabTestHistoryLight> labHistory = (List<LabTestHistoryLight>) getLabTestHistoryFacade().findLightsByJpql(jpql, params);
        return labHistory;
    }

    public List<LabTestHistoryLight> getRefundedLabTestHistoryByInvestigation(PatientInvestigation patientInvestigation) {
        if (patientInvestigation == null) {
            return null;
        }

        String jpql = "SELECT new com.divudi.bean.lab.LabTestHistoryLight(his.id, his.testHistoryType, his.createdAt, inst.name, dept.name, his.staff, his.createdBy, his.comment) "
                + " FROM LabTestHistory his "
                + " LEFT JOIN his.institution inst"
                + " LEFT JOIN his.department dept"
                + " WHERE his.retired=:retired "
                + " AND his.patientInvestigation =:patientInvestigation"
                + " AND his.testHistoryType =:type"
                + " order by his.createdAt asc";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("patientInvestigation", patientInvestigation);
        params.put("type", TestHistoryType.REFUNDED);
        List<LabTestHistoryLight> labHistory = (List<LabTestHistoryLight>) getLabTestHistoryFacade().findLightsByJpql(jpql, params);
        return labHistory;
    }

    public List<LabTestHistoryLight> getReportLabTestHistoryByInvestigation(PatientInvestigation patientInvestigation) {
        if (patientInvestigation == null) {
            return null;
        }

        String jpql = "SELECT new com.divudi.bean.lab.LabTestHistoryLight(his.id, his.testHistoryType, his.createdAt, inst.name, dept.name, his.staff, his.createdBy, his.comment) "
                + " FROM LabTestHistory his "
                + " LEFT JOIN his.institution inst"
                + " LEFT JOIN his.department dept"
                + " WHERE his.retired=:retired "
                + " AND his.patientInvestigation=:patientInvestigation"
                + " AND his.testHistoryType In :types"
                + " order by his.createdAt asc";
        Map<String, Object> params = new HashMap<>();

        List<TestHistoryType> reportedTypes = new ArrayList<>();
        reportedTypes.add(TestHistoryType.REPORT_CREATED);
        reportedTypes.add(TestHistoryType.DATA_ENTERED);
        reportedTypes.add(TestHistoryType.REPORT_UPLOADED);
        reportedTypes.add(TestHistoryType.REMOVED_UPLOADED_REPORT);
        reportedTypes.add(TestHistoryType.REPORT_CALCULATED);
        reportedTypes.add(TestHistoryType.REPORT_APPROVED);
        reportedTypes.add(TestHistoryType.REPORT_APPROVED_CANCEL);
        reportedTypes.add(TestHistoryType.REPORT_VIEWED);
        reportedTypes.add(TestHistoryType.REPORT_PRINTED);
        reportedTypes.add(TestHistoryType.SENT_SMS_MANUAL);
        reportedTypes.add(TestHistoryType.SENT_SMS_AUTO);
        reportedTypes.add(TestHistoryType.SENT_EMAIL);

        params.put("retired", false);
        params.put("patientInvestigation", patientInvestigation);
        params.put("types", reportedTypes);
        List<LabTestHistoryLight> labHistory = (List<LabTestHistoryLight>) getLabTestHistoryFacade().findLightsByJpql(jpql, params);
        return labHistory;
    }

    public List<LabTestHistoryLight> getLabTestHistoryByInvestigation(PatientInvestigation patientInvestigation, PatientSample patientSample) {

        if (patientInvestigation == null) {
            return null;
        }
        if (patientSample == null) {
            return null;
        }

        String jpql = "SELECT new com.divudi.bean.lab.LabTestHistoryLight(his.id, his.testHistoryType, his.createdAt, inst.name, dept.name, his.staff, his.createdBy, his.comment) "
                + " FROM LabTestHistory his "
                + " LEFT JOIN his.institution inst"
                + " LEFT JOIN his.department dept"
                + " WHERE his.retired=:retired "
                + " AND his.patientInvestigation=:patientInvestigation"
                + " AND his.patientSample =:ps"
                + " order by his.createdAt asc";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);
        params.put("patientInvestigation", patientInvestigation);
        params.put("ps", patientSample);
        List<LabTestHistoryLight> labHistory = (List<LabTestHistoryLight>) getLabTestHistoryFacade().findLightsByJpql(jpql, params);
        return labHistory;
    }

    public Long getLabTestHistoryCountByInvestigation(PatientInvestigation patientInvestigation, String labTestHistory) {
        TestHistoryType historyType = enumController.getLabTestHistory(labTestHistory);

        if (historyType == null || patientInvestigation == null) {
            return 0L;
        }

        String jpql = "SELECT COUNT(his.id) "
                + "FROM LabTestHistory his "
                + "WHERE his.retired = :retired "
                + "AND his.testHistoryType = :type "
                + "AND his.patientInvestigation = :patientInvestigation";

        Map<String, Object> params = new HashMap<>();
        params.put("type", historyType);
        params.put("retired", false);
        params.put("patientInvestigation", patientInvestigation);

        return getLabTestHistoryFacade().findLongByJpql(jpql, params);
    }

    public List<LabTestHistoryLight> getReportLabTestHistorys(PatientReport report, List<TestHistoryType> reportedTypes) {
        if (report == null) {
            return null;
        }
        if (reportedTypes == null || reportedTypes.isEmpty()) {
            return null;
        }

        String jpql = "SELECT new com.divudi.bean.lab.LabTestHistoryLight(his.id, his.testHistoryType, his.createdAt, inst.name, dept.name, his.staff, his.createdBy, his.comment) "
                + " FROM LabTestHistory his "
                + " LEFT JOIN his.institution inst"
                + " LEFT JOIN his.department dept"
                + " WHERE his.retired=:retired "
                + " AND his.patientReport=:pReport"
                + " AND his.testHistoryType In :types"
                + " order by his.createdAt asc";
        Map<String, Object> params = new HashMap<>();

        params.put("retired", false);
        params.put("pReport", report);
        params.put("types", reportedTypes);
        List<LabTestHistoryLight> labHistory = (List<LabTestHistoryLight>) labTestHistoryFacade.findLightsByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        return labHistory;
    }

    //  All Report Data (All in One)
    public List<LabTestHistoryLight> getAllPatientReportHistorys(PatientReport report) {
        List<TestHistoryType> types = new ArrayList<>();
        types.add(TestHistoryType.BYPASS_BARCODE_GENERAT_AND_REPORT_CREATED);
        types.add(TestHistoryType.RESULT_RECEIVED_FROM_ANALYZER);
        types.add(TestHistoryType.REPORT_CREATED);
        types.add(TestHistoryType.DATA_ENTERED);
        types.add(TestHistoryType.REPORT_CALCULATED);
        types.add(TestHistoryType.REPORT_APPROVED);
        types.add(TestHistoryType.REPORT_APPROVED_CANCEL);
        types.add(TestHistoryType.REPORT_REMOVE);
        types.add(TestHistoryType.REPORT_VIEWED);
        types.add(TestHistoryType.CREATE_SMS_AUTO);
        types.add(TestHistoryType.SENT_SMS_AUTO);
        types.add(TestHistoryType.SENT_SMS_FAIL);
        types.add(TestHistoryType.RESENT_FAIL_SMS);
        types.add(TestHistoryType.CREATE_SMS_MANUAL);
        types.add(TestHistoryType.SENT_SMS_MANUAL);
        types.add(TestHistoryType.CREATE_EMAIL);
        types.add(TestHistoryType.SENT_EMAIL);
        types.add(TestHistoryType.SENT_EMAIL_FAIL);
        types.add(TestHistoryType.RESENT_EMAIL);
        types.add(TestHistoryType.REPORT_PRINTED);
        types.add(TestHistoryType.REPORT_EXPORT_AS_PDF);
        types.add(TestHistoryType.REPORT_ISSUE_STAFF);
        types.add(TestHistoryType.REPORT_ISSUE_PATIENT);
        types.add(TestHistoryType.RECALCULATE_DYNAMICLABEL);
        types.add(TestHistoryType.PATIENT_DETAILS_CHANGE);

        return getReportLabTestHistorys(report, types);
    }
    // </editor-fold>

    @FacesConverter(forClass = LabTestHistory.class)
    public static class LabTestHistoryConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            LabTestHistoryController controller = (LabTestHistoryController) fc.getApplication()
                    .getELResolver().getValue(fc.getELContext(), null, "labTestHistoryController");
            return controller.getLabTestHistoryFacade().find(Long.valueOf(value));
        }

        @Override
        public String getAsString(FacesContext fc, UIComponent uic, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof LabTestHistory) {
                LabTestHistory lth = (LabTestHistory) object;
                return lth.getId() == null ? "" : lth.getId().toString();
            } else {
                throw new IllegalArgumentException("Unexpected object type");
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public LabTestHistory getCurrent() {
        if (current == null) {
            current = new LabTestHistory();
        }
        return current;
    }

    public void setCurrent(LabTestHistory current) {
        this.current = current;
    }

    public List<LabTestHistory> getItems() {
        if (items == null) {
            items = getLabTestHistoryFacade().findAll();
        }
        return items;
    }

    public LabTestHistoryFacade getLabTestHistoryFacade() {
        return labTestHistoryFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void save() {
        if (current == null) {
            return;
        }
        try {
            if (current.getId() != null) {
                getLabTestHistoryFacade().edit(current);
            } else {
                current.setInstitution(sessionController.getInstitution());
                current.setDepartment(sessionController.getDepartment());
                current.setCreatedAt(new Date());
                current.setCreatedBy(getSessionController().getLoggedUser());
                getLabTestHistoryFacade().create(current);
            }

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving: " + e.getMessage());
        }
    }
    // </editor-fold>

}
