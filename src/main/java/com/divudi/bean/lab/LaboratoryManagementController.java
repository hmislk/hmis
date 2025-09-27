package com.divudi.bean.lab;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.ReportTimerController;
import com.divudi.core.data.reports.CommonReports;
import com.divudi.core.data.PatientReportLight;
import com.divudi.core.data.ReportType;
import com.divudi.core.data.lab.BillBarcode;
import com.divudi.core.data.lab.ListingEntity;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.data.lab.PatientInvestigationWrapper;
import com.divudi.core.data.lab.PatientSampleWrapper;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Route;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.Upload;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PatientReportFacade;
import com.divudi.core.facade.PatientSampleComponantFacade;
import com.divudi.core.facade.PatientSampleFacade;
import com.divudi.core.facade.UploadFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class LaboratoryManagementController implements Serializable {

    private static final long serialVersionUID = 1L;

    public LaboratoryManagementController() {
        activeIndex = 1;
        listingEntity = ListingEntity.BILLS; // Set default view
    }

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private BillFacade billFacade;
    @EJB
    private PatientSampleFacade patientSampleFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    private PatientSampleComponantFacade patientSampleComponantFacade;
    @EJB
    PatientReportFacade patientReportFacade;
    @EJB
    UploadFacade uploadFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    PatientReportController patientReportController;
    @Inject
    PatientReportUploadController patientReportUploadController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    SessionController sessionController;
    @Inject
    PatientInvestigationController patientInvestigationController;
    @Inject
    ReportTimerController reportTimerController;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Variables">
    private ListingEntity listingEntity;

    private int activeIndex = 0;
    private Date fromDate;
    private Date toDate;
    private String patientName;
    private String type;
    private Staff referringDoctor;
    private PatientInvestigationStatus patientInvestigationStatus;
    private Route route;
    private Institution collectionCenter;
    private Institution orderedInstitution;
    private Department orderedDepartment;
    private Institution performingInstitution;
    private Department performingDepartment;
    private String billNo;
    private String bhtNo;
    private List<PatientInvestigationStatus> availableStatus;
    private boolean selectAll = false;
    private String sampleId;
    private String sampleRejectionComment;
    private List<PatientSample> patientSamples;
    private List<PatientSample> selectedPatientSamples;
    private Staff sampleTransportedToLabByStaff;
    private List<BillBarcode> billBarcodes;
    private List<BillBarcode> selectedBillBarcodes;
    private Bill currentBill;
    private boolean printIndividualBarcodes;
    private List<Bill> bills = null;
    private List<PatientInvestigation> items;
    private String investigationName;
    private String filteringStatus;
    private String comment;
    private Department sampleSendingDepartment;
    private Department sampleReceiveFromDepartment;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Navigation Method">
    public String navigateToLaboratoryManagementDashboard() {
        activeIndex = 1;
        listingEntity = ListingEntity.BILLS;
        if (configOptionApplicationController.getBooleanValueByKey("Only bills that have been sent to the Log department should be displayed.", false)) {
            performingInstitution = sessionController.getInstitution();
            performingDepartment = sessionController.getDepartment();
        }
        patientInvestigationStatus = PatientInvestigationStatus.ORDERED;
        return "/lab/laboratory_management_dashboard?faces-redirect=true";
    }

    public void navigateToLaboratoryBills() {
        activeIndex = 1;
        listingEntity = ListingEntity.BILLS;
        if (configOptionApplicationController.getBooleanValueByKey("Only bills that have been sent to the Log department should be displayed.", false)) {
            performingInstitution = sessionController.getInstitution();
            performingDepartment = sessionController.getDepartment();
        }
        patientInvestigationStatus = PatientInvestigationStatus.ORDERED;
    }

    public void navigateToInvestigation() {
        activeIndex = 2;
        listingEntity = ListingEntity.PATIENT_INVESTIGATIONS;
        performingInstitution = null;
        performingDepartment = null;
        patientInvestigationStatus = null;
    }

    public void navigateToSamples() {
        activeIndex = 3;
        performingInstitution = null;
        performingDepartment = null;
        patientInvestigationStatus = null;
        selectAll = false;
        listingEntity = ListingEntity.PATIENT_SAMPLES;
    }

    public void navigateToReport() {
        activeIndex = 4;
        listingEntity = ListingEntity.PATIENT_REPORTS;
        performingInstitution = null;
        performingDepartment = null;
        patientInvestigationStatus = null;
    }

    public void navigateToReportPrint() {
        activeIndex = 5;
        listingEntity = ListingEntity.REPORT_PRINT;
        performingInstitution = null;
        performingDepartment = null;
        patientInvestigationStatus = null;
    }

    public String navigateToOtherPatientReport(Bill bill) {
        activeIndex = 4;
        listingEntity = ListingEntity.PATIENT_REPORTS;
        navigateToPatientReportsFromSelectedBill(bill);
        return "/lab/laboratory_management_dashboard?faces-redirect=true";
    }

    public String navigateToOtherPatientInvestigations(Bill bill) {
        activeIndex = 4;
        listingEntity = ListingEntity.PATIENT_INVESTIGATIONS;
        navigateToInvestigationsFromSelectedBill(bill);
        return "/lab/laboratory_management_dashboard?faces-redirect=true";
    }

    public String navigateToEditReport(Long patientReportID) {
        PatientReport currentPatientReport = patientReportFacade.find(patientReportID);

        if (null == currentPatientReport.getReportType()) {
            patientReportController.setCurrentPatientReport(currentPatientReport);
            if (currentPatientReport.getPatientInvestigation().getInvestigation().isBypassSampleWorkflow()) {
                return "/lab/patient_report_without_sample_sending_process?faces-redirect=true";
            } else {
                return "/lab/patient_report?faces-redirect=true";
            }
        } else {
            switch (currentPatientReport.getReportType()) {
                case GENARATE:
                    patientReportController.setCurrentPatientReport(currentPatientReport);
                    patientReportController.fillReportFormats(currentPatientReport);
                    if (currentPatientReport.getPatientInvestigation().getInvestigation().isBypassSampleWorkflow()) {
                        return "/lab/patient_report_without_sample_sending_process?faces-redirect=true";
                    } else {
                        return "/lab/patient_report?faces-redirect=true";
                    }
                case UPLOAD:

                    Upload u = patientReportController.loadUpload(currentPatientReport);

                    if (u != null) {
                        patientReportUploadController.setReportUpload(u);
                    } else {
                        patientReportUploadController.setReportUpload(null);
                    }

                    patientReportUploadController.setPatientInvestigation(currentPatientReport.getPatientInvestigation());
                    return "/lab/upload_patient_report?faces-redirect=true";
                default:
                    return "";
            }
        }
    }

    public String navigateToPrintReport(Long patientReportID) {
        if (patientReportID == null) {
            JsfUtil.addErrorMessage("No Select Patient Report");
            return "";
        }

        PatientReport currentPatientReport = patientReportFacade.find(patientReportID);

        if (currentPatientReport.getReportType() == null) {
            patientReportController.setCurrentPatientReport(currentPatientReport);
            return "/lab/patient_report_print?faces-redirect=true";
        } else {
            switch (currentPatientReport.getReportType()) {
                case GENARATE:
                    patientReportController.setCurrentPatientReport(currentPatientReport);
                    return "/lab/patient_report_print?faces-redirect=true";
                case UPLOAD:
                    Upload currentReportUpload = patientReportController.loadUpload(currentPatientReport);
                    patientReportUploadController.setReportUpload(currentReportUpload);
                    return "/lab/upload_patient_report_print?faces-redirect=true";
                default:
                    return "";
            }
        }
    }

    public String navigateToPrintReportfromPrintPanel(Long patientReportID) {
        if (patientReportID == null) {
            JsfUtil.addErrorMessage("No Select Patient Report");
            return "";
        }

        PatientReport currentPatientReport = patientReportFacade.find(patientReportID);

        if (currentPatientReport.getReportType() == null) {
            patientReportController.setCurrentPatientReport(currentPatientReport);
            return "/lab/report_print?faces-redirect=true";
        } else {
            switch (currentPatientReport.getReportType()) {
                case GENARATE:
                    patientReportController.setCurrentPatientReport(currentPatientReport);
                    return "/lab/report_print?faces-redirect=true";
                case UPLOAD:
                    Upload currentReportUpload = patientReportController.loadUpload(currentPatientReport);
                    patientReportUploadController.setReportUpload(currentReportUpload);
                    return "/lab/upload_patient_report_print?faces-redirect=true";
                default:
                    return "";
            }
        }
    }

    public String navigateToLaboratoryAdministration() {
        return "/admin/lims/index?faces-redirect=true";
    }

    public void navigateToInvestigationsFromSelectedBill(Bill bill) {
        items = new ArrayList<>();
        listingEntity = ListingEntity.PATIENT_INVESTIGATIONS;
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "SELECT i "
                + " FROM PatientInvestigation i "
                + " WHERE i.retired = :ret "
                + " and i.billItem.bill =:bill"
                + " ORDER BY i.id DESC";

        params.put("ret", false);
        params.put("bill", bill);

        items = patientInvestigationFacade.findByJpql(jpql, params);
    }

    public void navigateToPatientReportsFromSelectedInvestigation(PatientInvestigation patientInvestigation) {
        items = new ArrayList<>();
        if (patientInvestigation == null || patientInvestigation.getId() == null) {
            return;
        }
        PatientInvestigation pi = patientInvestigationFacade.find(patientInvestigation.getId());
        items.add(pi);
        listingEntity = ListingEntity.REPORT_PRINT;

    }

    public void navigateToPatientReportsFromSelectedBill(Bill bill) {
        items = new ArrayList<>();
        listingEntity = ListingEntity.PATIENT_REPORTS;
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "SELECT i "
                + " FROM PatientInvestigation i "
                + " WHERE i.retired =:ret "
                + " and i.billItem.bill =:bill"
                + " ORDER BY i.id DESC";

        params.put("ret", false);
        params.put("bill", bill);

        items = patientInvestigationFacade.findByJpql(jpql, params);
    }

    public void navigateToPatientReportsFromSelectedPatientInvestigation(PatientInvestigation patientInvestigation) {
        items = new ArrayList<>();
        listingEntity = ListingEntity.PATIENT_REPORTS;

        items.add(patientInvestigation);
    }

    public void navigateToPatientReportsPrintFromSelectedBill(Bill bill) {
        items = new ArrayList<>();
        listingEntity = ListingEntity.REPORT_PRINT;
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "SELECT i "
                + " FROM PatientInvestigation i "
                + " WHERE i.retired =:ret "
                + " and i.billItem.bill =:bill"
                + " ORDER BY i.id DESC";

        params.put("ret", false);
        params.put("bill", bill);

        items = patientInvestigationFacade.findByJpql(jpql, params);
    }

    public String navigateToBackFormPatientReportEditingView() {
        if (configOptionApplicationController.getBooleanValueByKey("The system uses the Laboratory Dashboard as its default interface", false)) {
            listingEntity = ListingEntity.PATIENT_REPORTS;
            return "/lab/laboratory_management_dashboard?faces-redirect=true";
        } else {
            patientInvestigationController.setListingEntity(ListingEntity.PATIENT_REPORTS);
            return "/lab/generate_barcode_p?faces-redirect=true";
        }
    }

    public String navigateToBackFormPatientReportPrintView() {
        if (configOptionApplicationController.getBooleanValueByKey("The system uses the Laboratory Dashboard as its default interface", false)) {
            listingEntity = ListingEntity.PATIENT_REPORTS;
            return "/lab/laboratory_management_dashboard?faces-redirect=true";
        } else {
            patientInvestigationController.setListingEntity(ListingEntity.PATIENT_REPORTS);
            return "/lab/generate_barcode_p?faces-redirect=true";
        }
    }

    public String navigateToBackFormPatientReportPrintingView() {
        if (configOptionApplicationController.getBooleanValueByKey("The system uses the Laboratory Dashboard as its default interface", false)) {
            listingEntity = ListingEntity.REPORT_PRINT;
            return "/lab/laboratory_management_dashboard?faces-redirect=true";
        } else {
            patientInvestigationController.setListingEntity(ListingEntity.REPORT_PRINT);
            return "/lab/generate_barcode_p?faces-redirect=true";
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Function">
    public void makeNull() {
        this.bills = null;
        this.items = null;
        this.investigationName = null;
        this.filteringStatus = null;
        this.selectedBillBarcodes = null;
        this.currentBill = null;
        this.availableStatus = null;
        this.selectAll = false;
        this.sampleId = null;
        this.sampleRejectionComment = null;
        this.patientSamples = null;
        this.route = null;
        this.collectionCenter = null;
        this.orderedInstitution = null;
        this.orderedDepartment = null;
        this.performingInstitution = null;
        this.performingDepartment = null;
        this.billNo = null;
        this.bhtNo = null;
        this.patientName = null;
        this.selectedPatientSamples = null;
        this.sampleTransportedToLabByStaff = null;
        this.billBarcodes = null;
        this.type = null;
        this.referringDoctor = null;
        this.sampleSendingDepartment = null;
    }

    public void searchLabBills() {
        reportTimerController.trackReportExecution(() -> {
            listingEntity = ListingEntity.BILLS;
            String jpql;
            bills = new ArrayList();
            Map<String, Object> params = new HashMap<>();
            jpql = "SELECT pi.billItem.bill "
                    + " FROM PatientInvestigation pi"
                    + " WHERE pi.billItem.bill.retired = :ret"
                    + " AND pi.billItem.bill.createdAt BETWEEN :fd AND :td";

        if (billNo != null && !billNo.trim().isEmpty()) {
            jpql += " AND pi.billItem.bill.deptId LIKE :billNo";
            params.put("billNo", "%" + getBillNo().trim() + "%");
        }

        if (bhtNo != null && !bhtNo.trim().isEmpty()) {
            jpql += " AND pi.billItem.bill.patientEncounter is not null AND pi.billItem.bill.patientEncounter.bhtNo LIKE :bht";
            params.put("bht", "%" + getBhtNo().trim() + "%");
        }

        if (orderedInstitution != null) {
            jpql += " AND pi.billItem.bill.institution = :orderedInstitution";
            params.put("orderedInstitution", getOrderedInstitution());
        }

        if (orderedDepartment != null) {
            jpql += " AND pi.billItem.bill.department = :orderedDepartment";
            params.put("orderedDepartment", getOrderedDepartment());
        }

        if (performingInstitution != null) {
            jpql += " AND pi.billItem.bill.toInstitution = :performingInstitution";
            params.put("performingInstitution", performingInstitution);
        }

        if (performingDepartment != null) {
            jpql += " AND pi.billItem.bill.toDepartment = :performingDepartment";
            params.put("performingDepartment", performingDepartment);
        }

        if (collectionCenter != null) {
            jpql += " AND (pi.billItem.bill.collectingCentre = :collectionCenter OR pi.billItem.bill.fromInstitution = :collectionCenter)";
            params.put("collectionCenter", getCollectionCenter());
        }

        if (route != null) {
            jpql += " AND (pi.billItem.bill.collectingCentre.route = :route OR pi.billItem.bill.fromInstitution.route = :route)";
            params.put("route", getRoute());
        }

        if (patientName != null && !patientName.trim().isEmpty()) {
            jpql += " AND pi.billItem.bill.patient.person.name LIKE :patientName";
            params.put("patientName", "%" + getPatientName().trim() + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            jpql += " AND pi.billItem.bill.ipOpOrCc = :type";
            params.put("type", getType().trim());
        }

        if (referringDoctor != null) {
            jpql += " AND pi.billItem.bill.referredBy = :referringDoctor";
            params.put("referringDoctor", getReferringDoctor());
        }

        if (patientInvestigationStatus != null) {
            jpql += " AND pi.billItem.bill.status = :status";
            params.put("status", patientInvestigationStatus);
        }

        jpql += " GROUP BY pi.billItem.bill ";

        jpql += " ORDER BY pi.billItem.bill.id DESC";

        params.put("ret", false);
        params.put("fd", getFromDate());
        params.put("td", getToDate());

        bills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.searchLabBills", sessionController.getLoggedUser());
    }

    public void generateBarcodesForSelectedBill(Bill billForBarcode) {
        selectedBillBarcodes = new ArrayList<>();
        billBarcodes = new ArrayList<>();
        setCurrentBill(billForBarcode);
        if (billForBarcode == null) {
            JsfUtil.addErrorMessage("No Bills Seelcted");
            return;
        }

        if (billForBarcode.isCancelled()) {
            JsfUtil.addErrorMessage("This Bill is Already Cancel");
            return;
        }
        reportTimerController.trackReportExecution(() -> {
            BillBarcode bb = new BillBarcode(billForBarcode);
            List<PatientSampleWrapper> psws = new ArrayList<>();
            List<PatientSample> pss = patientInvestigationController.prepareSampleCollectionByBillsForPhlebotomyRoom(billForBarcode, sessionController.getLoggedUser());
            StringBuilder sampleIDs = new StringBuilder();
            if (pss != null) {
                for (PatientSample ps : pss) {
                    PatientSampleWrapper ptsw = new PatientSampleWrapper(ps);
                    psws.add(ptsw);
                    if (!sampleIDs.toString().contains(ps.getIdStr())) {
                        sampleIDs.append(ps.getIdStr()).append(" ");
                    }
                }
            }

            for (PatientInvestigationWrapper piw : bb.getPatientInvestigationWrappers()) {
                if (billForBarcode.getStatus() == PatientInvestigationStatus.ORDERED) {
                    piw.getPatientInvestigation().setBarcodeGenerated(true);
                    piw.getPatientInvestigation().setBarcodeGeneratedAt(new Date());

                }

                String[] idsToAdd = sampleIDs.toString().trim().split("\\s+");
                String existingSampleIds = piw.getPatientInvestigation().getSampleIds();
                for (String id : idsToAdd) {
                    if (!existingSampleIds.contains(id)) {
                        existingSampleIds += " " + id;
                    }
                }
                if (billForBarcode.getStatus() == PatientInvestigationStatus.ORDERED) {
                    piw.getPatientInvestigation().setBarcodeGeneratedBy(sessionController.getLoggedUser());
                    piw.getPatientInvestigation().setStatus(PatientInvestigationStatus.SAMPLE_GENERATED);
                }
                piw.getPatientInvestigation().setSampleIds(existingSampleIds.trim());

                patientInvestigationFacade.edit(piw.getPatientInvestigation());
            }
            if (billForBarcode.getStatus() == PatientInvestigationStatus.ORDERED) {
                billForBarcode.setStatus(PatientInvestigationStatus.SAMPLE_GENERATED);
            }

            billFacade.edit(billForBarcode);
            bb.setPatientSampleWrappers(psws);

            billBarcodes.add(bb);
            selectedBillBarcodes = billBarcodes;
            listingEntity = ListingEntity.VIEW_BARCODE;
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.generateBarcodesForSelectedBill", sessionController.getLoggedUser());
    }

    public void searchPatientSamples() {
        reportTimerController.trackReportExecution(() -> {
        listingEntity = ListingEntity.PATIENT_SAMPLES;
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "SELECT ps FROM PatientSample ps JOIN ps.bill b WHERE ps.retired = :ret";

        jpql += " AND b.createdAt BETWEEN :fd AND :td";
        params.put("fd", getFromDate());
        params.put("td", getToDate());

        if (billNo != null && !billNo.trim().isEmpty()) {
            jpql += " AND b.deptId LIKE :billNo";
            params.put("billNo", "%" + getBillNo().trim() + "%");
        }

        if (bhtNo != null && !bhtNo.trim().isEmpty()) {
            jpql += " AND b.patientEncounter is not null AND b.patientEncounter.bhtNo LIKE :bht";
            params.put("bht", "%" + getBhtNo().trim() + "%");
        }

        if (orderedInstitution != null) {
            jpql += " AND b.institution = :orderedInstitution";
            params.put("orderedInstitution", getOrderedInstitution());
        }

        if (orderedDepartment != null) {
            jpql += " AND b.department = :orderedDepartment";
            params.put("orderedDepartment", getOrderedDepartment());
        }

        if (performingInstitution != null) {
            jpql += " AND ps.institution = :performingInstitution";
            params.put("performingInstitution", getPerformingInstitution());
        }

        if (performingDepartment != null) {
            jpql += " AND ps.department = :performingDepartment";
            params.put("performingDepartment", getPerformingDepartment());
        }

        if (collectionCenter != null) {
            jpql += " AND (b.collectingCentre = :collectionCenter OR b.fromInstitution = :collectionCenter)";
            params.put("collectionCenter", getCollectionCenter());
        }

        if (route != null) {
            jpql += " AND (b.collectingCentre.route = :route OR b.fromInstitution.route = :route)";
            params.put("route", getRoute());
        }

        if (patientName != null && !patientName.trim().isEmpty()) {
            jpql += " AND b.patient.person.name LIKE :patientName";
            params.put("patientName", "%" + getPatientName().trim() + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            jpql += " AND b.ipOpOrCc = :type";
            params.put("type", getType().trim());
        }

        if (patientInvestigationStatus != null) {
            jpql += " AND ps.status = :status";
            params.put("status", getPatientInvestigationStatus());
        }

        if (sampleId != null) {
            jpql += " AND (ps.sampleId like :smpid or ps.id like :smpId) ";
            params.put("smpid", "%" + String.valueOf(sampleId) + "%");
            params.put("smpId", "%" + String.valueOf(sampleId) + "%");
        }

        jpql += " ORDER BY ps.id DESC";

        params.put("ret", false);

        patientSamples = patientSampleFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        if (patientSamples == null) {
            patientSamples = new ArrayList();
        }
        selectAll = false;
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.searchPatientSamples", sessionController.getLoggedUser());
    }

    public void fetchSamples(List<PatientInvestigationStatus> availableStatus) {
        reportTimerController.trackReportExecution(() -> {
        listingEntity = ListingEntity.PATIENT_SAMPLES;
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "SELECT ps FROM PatientSample ps JOIN ps.bill b WHERE ps.retired = :ret";

        jpql += " AND b.createdAt BETWEEN :fd AND :td";
        params.put("fd", getFromDate());
        params.put("td", getToDate());

        if (orderedInstitution != null) {
            jpql += " AND b.institution = :orderedInstitution";
            params.put("orderedInstitution", getOrderedInstitution());
        }

        if (orderedDepartment != null) {
            jpql += " AND b.department = :orderedDepartment";
            params.put("orderedDepartment", getOrderedDepartment());
        }

        if (performingInstitution != null) {
            jpql += " AND ps.institution = :performingInstitution";
            params.put("performingInstitution", getPerformingInstitution());
        }

        if (performingDepartment != null) {
            jpql += " AND ps.department = :performingDepartment";
            params.put("performingDepartment", getPerformingDepartment());
        }

        if (availableStatus != null) {
            jpql += " AND ps.status in :status";
            params.put("status", availableStatus);
        }

        jpql += " ORDER BY ps.id DESC";

        params.put("ret", false);

        patientSamples = patientSampleFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        if (patientSamples == null) {
            patientSamples = new ArrayList();
        }
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.fetchSamples", sessionController.getLoggedUser());
    }

    public void nonCollectedSampleList() {
        reportTimerController.trackReportExecution(() -> {
        selectedPatientSamples = new ArrayList();

        String jpql = "SELECT ps FROM PatientSample ps "
                + "WHERE ps.retired = :ret "
                + "AND ps.bill.createdAt BETWEEN :fd AND :td "
                + "AND ps.department = :department "
                + "AND ps.status = :status "
                + "ORDER BY ps.id DESC";

        
        Map<String, Object> params = new HashMap<>();
        params.put("fd", getFromDate());
        params.put("td", getToDate());
        params.put("department", sessionController.getDepartment());
        params.put("ret", false);
        params.put("status", PatientInvestigationStatus.SAMPLE_GENERATED);

        patientSamples = patientSampleFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        if (patientSamples == null) {
            patientSamples = new ArrayList<>();
        }

        selectAll = false;
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.nonCollectedSampleList", sessionController.getLoggedUser());
    }

    public void pendingSendSampleList() {
        reportTimerController.trackReportExecution(() -> {
        selectedPatientSamples = new ArrayList<>();

        String jpql = "SELECT ps FROM PatientSample ps "
                + "WHERE ps.retired = :ret "
                + "AND ps.bill.createdAt BETWEEN :fd AND :td "
                + "AND ps.department = :department "
                + "AND ps.status = :status "
                + "AND ps.createdAt >= :fromDate "
                + "ORDER BY ps.id DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("fd", getFromDate());
        params.put("td", getToDate());
        params.put("department", sessionController.getDepartment());
        params.put("ret", false);
        params.put("status", PatientInvestigationStatus.SAMPLE_COLLECTED);

        int hours = configOptionApplicationController.getIntegerValueByKey(
                "Limit pending sample listings to last hours", 24);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -hours);
        params.put("fromDate", cal.getTime());

        patientSamples = patientSampleFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        if (patientSamples == null) {
            patientSamples = new ArrayList<>();
        }

        selectAll = false;
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.pendingSendSampleList", sessionController.getLoggedUser());
    }

    public void nonReceivedSampleList() {
        reportTimerController.trackReportExecution(() -> {
        if (sampleReceiveFromDepartment == null) {
            JsfUtil.addErrorMessage("Please Select Sample from Department.");
            patientSamples = new ArrayList<>();
            return;
        }

        selectedPatientSamples = new ArrayList<>();

        String jpql = "SELECT ps FROM PatientSample ps "
                + "WHERE ps.retired = :ret "
                + "AND ps.sampleSentToDepartment = :toDepartment "
                + "AND ps.bill.createdAt BETWEEN :fd AND :td "
                + "AND ps.department = :fromDepartment "
                + "AND ps.status = :status "
                + "AND ps.createdAt >= :fromDate "
                + "ORDER BY ps.id DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("fd", getFromDate());
        params.put("td", getToDate());
        params.put("toDepartment", sessionController.getDepartment());
        params.put("fromDepartment", sampleReceiveFromDepartment);
        params.put("ret", false);
        params.put("status", PatientInvestigationStatus.SAMPLE_SENT);

        int hours = configOptionApplicationController.getIntegerValueByKey(
                "Limit pending sample listings to last hours", 24);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -hours);
        params.put("fromDate", cal.getTime());

        patientSamples = patientSampleFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        if (patientSamples == null) {
            patientSamples = new ArrayList<>();
        }

        selectAll = false;
        sampleReceiveFromDepartment = null;
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.nonReceivedSampleList", sessionController.getLoggedUser());
    }

    public void selectAllSamples() {
        reportTimerController.trackReportExecution(() -> {
        if (patientSamples == null) {
            JsfUtil.addErrorMessage("No samples selected");
            return;
        }
        selectedPatientSamples = new ArrayList();
        for (PatientSample ps : patientSamples) {
            selectedPatientSamples.add(ps);
        }
        selectAll = true;
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.selectAllSamples", sessionController.getLoggedUser());
    }

    public void unSelectAllSamples() {
        reportTimerController.trackReportExecution(() -> {
        selectedPatientSamples = new ArrayList();
        selectAll = false;
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.unSelectAllSamples", sessionController.getLoggedUser());
    }

    public void collectSamples() {
        reportTimerController.trackReportExecution(() -> {
        if (selectedPatientSamples == null || selectedPatientSamples.isEmpty()) {
            JsfUtil.addErrorMessage("No samples selected");
            return;
        }

        List<PatientSample> canCollectSamples = new ArrayList<>();
        
        for (PatientSample ps : selectedPatientSamples) {
            if (ps.getBill().isCancelled()) {
                JsfUtil.addErrorMessage("This Bill is Already Cancel");
                return;
            }
            if (ps.getStatus() == PatientInvestigationStatus.SAMPLE_COLLECTED) {
                JsfUtil.addErrorMessage("There are samples already colleted. Please unselect and click Collect again");
                return;
            }
            if (ps.getStatus() == PatientInvestigationStatus.SAMPLE_GENERATED) {
                canCollectSamples.add(ps);
            }
        }
        
        if (canCollectSamples.isEmpty()) {
            JsfUtil.addErrorMessage("There are no suitable samples to send from the selected samples.");
            return;
        }

        listingEntity = ListingEntity.PATIENT_SAMPLES;

        Map<Long, PatientInvestigation> collectedPtixs = new HashMap<>();
        Map<Long, Bill> collectedBills = new HashMap<>();

        // Update sample collection details and gather associated patient investigations
        for (PatientSample ps : canCollectSamples) {
            ps.setDepartment(sessionController.getDepartment());
            ps.setSampleCollected(true);
            ps.setSampleCollectedAt(new Date());
            ps.setSampleCollectedDepartment(sessionController.getDepartment());
            ps.setSampleCollectedInstitution(sessionController.getInstitution());
            ps.setSampleCollecter(sessionController.getLoggedUser());
            ps.setStatus(PatientInvestigationStatus.SAMPLE_COLLECTED);
            patientSampleFacade.edit(ps);

            // Retrieve and store PatientInvestigations by unique ID to avoid duplicates
            for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsBySample(ps)) {
                collectedPtixs.putIfAbsent(pi.getId(), pi);
            }
        }

        // Update patient investigations and collect associated bills
        for (PatientInvestigation tptix : collectedPtixs.values()) {
            tptix.setSampleCollected(true);
            tptix.setSampleCollectedAt(new Date());
            tptix.setSampleCollectedBy(sessionController.getLoggedUser());
            tptix.setStatus(PatientInvestigationStatus.SAMPLE_COLLECTED);
            patientInvestigationFacade.edit(tptix);
            collectedBills.putIfAbsent(tptix.getBillItem().getBill().getId(), tptix.getBillItem().getBill());
        }

        // Update bills status
        for (Bill tb : collectedBills.values()) {
            tb.setStatus(PatientInvestigationStatus.SAMPLE_COLLECTED);
            billFacade.edit(tb);
        }

        JsfUtil.addSuccessMessage("Selected Samples Collected");
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.collectSamples", sessionController.getLoggedUser());
    }

    public void sendSamplesToLab() {
        reportTimerController.trackReportExecution(() -> {
        if (sampleTransportedToLabByStaff == null) {
            JsfUtil.addErrorMessage("The transport worker is not included.");
            return;
        }
        if (sampleSendingDepartment == null) {
            JsfUtil.addErrorMessage("The sending Department is Empty.");
            return;
        }

        if (selectedPatientSamples == null || selectedPatientSamples.isEmpty()) {
            JsfUtil.addErrorMessage("No samples selected");
            return;
        }

        List<PatientSample> canSentSamples = new ArrayList<>();
        
        for (PatientSample ps : selectedPatientSamples) {
            if (ps.getBill().isCancelled()) {
                JsfUtil.addErrorMessage("This Bill is Already Cancel");
                return;
            }
            if (ps.getStatus() != PatientInvestigationStatus.SAMPLE_COLLECTED) {
                JsfUtil.addErrorMessage("There are samples which are yet to collect. Please select them and click the sent to lab button again");
                return;
            }
            if (ps.getStatus() == PatientInvestigationStatus.SAMPLE_COLLECTED) {
                canSentSamples.add(ps);
            }
        }
        
        if (canSentSamples.isEmpty()) {
            JsfUtil.addErrorMessage("There are no suitable samples to send from the selected samples.");
            return;
        }

        listingEntity = ListingEntity.PATIENT_SAMPLES;

        Map<Long, PatientInvestigation> samplePtixs = new HashMap<>();
        Map<Long, Bill> sampleBills = new HashMap<>();

        // Process each selected patient sample
        for (PatientSample ps : canSentSamples) {
            ps.setSampleTransportedToLabByStaff(sampleTransportedToLabByStaff);
            ps.setSampleSent(true);
            ps.setDepartment(sessionController.getDepartment());
            ps.setSampleSentBy(sessionController.getLoggedUser());
            ps.setSampleSentAt(new Date());
            ps.setSampleSentToInstitution(sampleSendingDepartment.getInstitution());
            ps.setSampleSentToDepartment(sampleSendingDepartment);
            ps.setStatus(PatientInvestigationStatus.SAMPLE_SENT);
            patientSampleFacade.edit(ps);

            // Retrieve and store PatientInvestigations by unique ID to avoid duplicates
            for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsBySample(ps)) {
                samplePtixs.putIfAbsent(pi.getId(), pi);
            }
        }

        // Update PatientInvestigations and store associated Bills by unique ID to avoid duplicates
        for (PatientInvestigation tptix : samplePtixs.values()) {
            tptix.setSampleSent(true);
            tptix.setSampleTransportedToLabByStaff(sampleTransportedToLabByStaff);
            tptix.setSampleSentAt(new Date());
            tptix.setSampleSentBy(sessionController.getLoggedUser());
            tptix.setStatus(PatientInvestigationStatus.SAMPLE_SENT);
            patientInvestigationFacade.edit(tptix);
            sampleBills.putIfAbsent(tptix.getBillItem().getBill().getId(), tptix.getBillItem().getBill());
        }

        // Update Bills
        for (Bill tb : sampleBills.values()) {
            tb.setStatus(PatientInvestigationStatus.SAMPLE_SENT);
            billFacade.edit(tb);
        }
        sampleTransportedToLabByStaff = null;
        JsfUtil.addSuccessMessage("Selected Samples Sent to Lab");
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.sendSamplesToLab", sessionController.getLoggedUser());
    }

    public void receiveSamplesAtLab() {
        reportTimerController.trackReportExecution(() -> {
        if (selectedPatientSamples == null || selectedPatientSamples.isEmpty()) {
            JsfUtil.addErrorMessage("No samples selected");
            return;
        }
        
        List<PatientSample> canReciveSamples = new ArrayList<>();

        for (PatientSample ps : selectedPatientSamples) {
            if (ps.getBill().isCancelled()) {
                JsfUtil.addErrorMessage("This Bill is Already Cancel");
                return;
            }
            if (ps.getStatus() == PatientInvestigationStatus.SAMPLE_SENT) {
                canReciveSamples.add(ps);
            }
        }
        
        if (canReciveSamples.isEmpty()) {
            JsfUtil.addErrorMessage("There are no suitable samples to send from the selected samples.");
            return;
        }

        listingEntity = ListingEntity.PATIENT_SAMPLES;

        Map<Long, PatientInvestigation> receivedPtixs = new HashMap<>();
        Map<Long, Bill> receivedBills = new HashMap<>();

        // Update sample details and collect associated patient investigations
        for (PatientSample ps : canReciveSamples) {
            ps.setSampleReceivedAtLab(true);
            ps.setSampleReceiverAtLab(sessionController.getLoggedUser());
            ps.setSampleReceivedAtLabDepartment(sessionController.getDepartment());
            ps.setSampleReceivedAtLabInstitution(sessionController.getInstitution());
            ps.setSampleReceivedAtLabAt(new Date());
            ps.setStatus(PatientInvestigationStatus.SAMPLE_ACCEPTED);
            patientSampleFacade.edit(ps);

            // Retrieve and store PatientInvestigations by unique ID to avoid duplicates
            for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsBySample(ps)) {
                receivedPtixs.putIfAbsent(pi.getId(), pi);
            }
        }

        // Update patient investigations and collect associated bills
        for (PatientInvestigation tptix : receivedPtixs.values()) {
            tptix.setSampleAccepted(true);
            tptix.setSampleAcceptedAt(new Date());
            tptix.setSampleAcceptedBy(sessionController.getLoggedUser());
            tptix.setReceived(true);
            tptix.setReceivedAt(new Date());
            tptix.setReceiveDepartment(sessionController.getDepartment());
            tptix.setReceiveInstitution(sessionController.getInstitution());
            tptix.setStatus(PatientInvestigationStatus.SAMPLE_ACCEPTED);
            patientInvestigationFacade.edit(tptix);
            receivedBills.putIfAbsent(tptix.getBillItem().getBill().getId(), tptix.getBillItem().getBill());
        }

        // Update bills status
        for (Bill tb : receivedBills.values()) {
            tb.setStatus(PatientInvestigationStatus.SAMPLE_ACCEPTED);
            billFacade.edit(tb);
        }

        JsfUtil.addSuccessMessage("Selected Samples Are Received at Lab");
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.receiveSamplesAtLab", sessionController.getLoggedUser());
    }

    public void rejectSamples() {
        reportTimerController.trackReportExecution(() -> {
        if (selectedPatientSamples == null || selectedPatientSamples.isEmpty()) {
            JsfUtil.addErrorMessage("No samples selected");
            return;
        }
        listingEntity = ListingEntity.PATIENT_SAMPLES;

        Map<Long, PatientInvestigation> rejectedPtixs = new HashMap<>();
        Map<Long, Bill> affectedBills = new HashMap<>();

        // Update sample rejection details and gather associated patient investigations
        for (PatientSample ps : selectedPatientSamples) {
            ps.setSampleReceivedAtLabComments(sampleRejectionComment);
            ps.setSampleRejected(true);
            ps.setSampleRejectedAt(new Date());
            ps.setSampleRejectedBy(sessionController.getLoggedUser());
            ps.setStatus(PatientInvestigationStatus.SAMPLE_REJECTED);
            patientSampleFacade.edit(ps);
            sampleRejectionComment = "";

            // Retrieve and store PatientInvestigations by unique ID to avoid duplicates
            for (PatientInvestigation pi : patientInvestigationController.getPatientInvestigationsBySample(ps)) {
                rejectedPtixs.putIfAbsent(pi.getId(), pi);
            }
        }

        // Update patient investigations and gather associated bills
        for (PatientInvestigation tptix : rejectedPtixs.values()) {
            tptix.setSampleRejected(true);
            tptix.setSampleRejectedAt(new Date());
            tptix.setSampleRejectedBy(sessionController.getLoggedUser());
            tptix.setStatus(PatientInvestigationStatus.SAMPLE_REJECTED);
            patientInvestigationFacade.edit(tptix);
            affectedBills.putIfAbsent(tptix.getBillItem().getBill().getId(), tptix.getBillItem().getBill());
        }

        // Update bills status accordingly
        for (Bill tb : affectedBills.values()) {
            tb.setStatus(PatientInvestigationStatus.SAMPLE_REJECTED);
            billFacade.edit(tb);
        }

        JsfUtil.addSuccessMessage("Selected Samples Are Rejected");
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.rejectSamples", sessionController.getLoggedUser());
    }

    public void navigateToSamplesFromSelectedBill(Bill bill) {
        reportTimerController.trackReportExecution(() -> {
        patientSamples = new ArrayList<>();
        listingEntity = ListingEntity.PATIENT_SAMPLES;
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "SELECT ps "
                + "FROM PatientSample ps "
                + "JOIN ps.bill b "
                + "WHERE ps.retired = :ret "
                + " and ps.bill =:bill ";

        jpql += " ORDER BY ps.id DESC";

        params.put("ret", false);
        params.put("bill", bill);

        patientSamples = patientSampleFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        selectAll = false;
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.navigateToSamplesFromSelectedBill", sessionController.getLoggedUser());

    }

    public void searchPatientInvestigations() {
        reportTimerController.trackReportExecution(() -> {
        items = new ArrayList();

        if (sampleId != null) {
            try {
                Long id = Long.valueOf(sampleId);
                searchPatientInvestigationsWithSampleId(id);
            } catch (NumberFormatException e) {
                searchPatientInvestigationsWithoutSampleId();
            }
        }
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.searchPatientInvestigations", sessionController.getLoggedUser());
    }

    public void searchPatientInvestigationsWithSampleId(Long sampleID) {
        reportTimerController.trackReportExecution(() -> {
        listingEntity = ListingEntity.PATIENT_INVESTIGATIONS;
        String jpql;
        Map<String, Object> params = new HashMap<>();

        // Query PatientSampleComponent to get PatientInvestigations
        jpql = "SELECT i "
                + "FROM PatientSampleComponant psc "
                + " join psc.patientInvestigation i "
                + " WHERE psc.retired = :ret "
                + " AND psc.patientSample.id LIKE :sampleId "
                + " AND i.billItem.bill.createdAt BETWEEN :fd AND :td "
                + " AND i.retired = :ret ";

        params.put("ret", false);
        params.put("sampleId", "%" + sampleID + "%");
        params.put("fd", getFromDate());
        params.put("td", getToDate());

        if (billNo != null && !billNo.trim().isEmpty()) {
            jpql += " AND i.billItem.bill.deptId LIKE :billNo";
            params.put("billNo", "%" + getBillNo().trim() + "%");
        }

        if (bhtNo != null && !bhtNo.trim().isEmpty()) {
            jpql += " AND i.billItem.bill.patientEncounter is not null AND i.billItem.bill.patientEncounter.bhtNo LIKE :bht";
            params.put("bht", "%" + getBhtNo().trim() + "%");
        }

        if (orderedInstitution != null) {
            jpql += " AND i.billItem.bill.institution = :orderedInstitution ";
            params.put("orderedInstitution", getOrderedInstitution());
        }

        if (orderedDepartment != null) {
            jpql += " AND i.billItem.bill.department = :orderedDepartment ";
            params.put("orderedDepartment", getOrderedDepartment());
        }

        if (performingInstitution != null) {
            jpql += " AND i.performInstitution = :performingInstitution ";
            params.put("performingInstitution", getPerformingInstitution());
        }

        if (performingDepartment != null) {
            jpql += " AND i.performDepartment = :performingDepartment ";
            params.put("performingDepartment", getPerformingDepartment());
        }

        if (collectionCenter != null) {
            jpql += " AND (i.billItem.bill.collectingCentre = :collectionCenter OR i.billItem.bill.fromInstitution = :collectionCenter) ";
            params.put("collectionCenter", getCollectionCenter());
        }

        if (route != null) {
            jpql += " AND (i.billItem.bill.collectingCentre.route = :route OR i.billItem.bill.fromInstitution.route = :route) ";
            params.put("route", getRoute());
        }

        if (patientName != null && !patientName.trim().isEmpty()) {
            jpql += " AND i.billItem.bill.patient.person.name LIKE :patientName ";
            params.put("patientName", "%" + getPatientName().trim() + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            jpql += " AND i.billItem.bill.ipOpOrCc = :tp ";
            params.put("tp", getType().trim());
        }

        if (referringDoctor != null) {
            jpql += " AND i.billItem.bill.referringDoctor = :referringDoctor ";
            params.put("referringDoctor", getReferringDoctor());
        }

        if (investigationName != null && !investigationName.trim().isEmpty()) {
            jpql += " AND i.billItem.item.name like :investigation ";
            params.put("investigation", "%" + investigationName.trim() + "%");
        }

        if (patientInvestigationStatus != null) {
            jpql += " AND i.status = :patientInvestigationStatus ";
            params.put("patientInvestigationStatus", getPatientInvestigationStatus());
        }

        jpql += " ORDER BY i.id DESC";

        params.put("ret", false);

        items = patientInvestigationFacade.findByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.searchPatientInvestigationsWithSampleId", sessionController.getLoggedUser());
    }

    public void searchPatientInvestigationsWithoutSampleId() {
        reportTimerController.trackReportExecution(() -> {
        listingEntity = ListingEntity.PATIENT_INVESTIGATIONS;
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "SELECT i "
                + " FROM PatientInvestigation i "
                + " WHERE i.retired = :ret "
                + " AND i.billItem.bill.createdAt BETWEEN :fd AND :td ";
        params.put("fd", getFromDate());
        params.put("td", getToDate());

        if (billNo != null && !billNo.trim().isEmpty()) {
            jpql += " AND i.billItem.bill.deptId LIKE :billNo";
            params.put("billNo", "%" + getBillNo().trim() + "%");
        }

        if (bhtNo != null && !bhtNo.trim().isEmpty()) {
            jpql += " AND i.billItem.bill.patientEncounter is not null AND i.billItem.bill.patientEncounter.bhtNo LIKE :bht";
            params.put("bht", "%" + getBhtNo().trim() + "%");
        }

        if (orderedInstitution != null) {
            jpql += " AND i.billItem.bill.institution = :orderedInstitution ";
            params.put("orderedInstitution", getOrderedInstitution());
        }

        if (orderedDepartment != null) {
            jpql += " AND i.billItem.bill.department = :orderedDepartment ";
            params.put("orderedDepartment", getOrderedDepartment());
        }

        if (performingInstitution != null) {
            jpql += " AND i.performInstitution = :peformingInstitution ";
            params.put("peformingInstitution", getPerformingInstitution());
        }

        if (performingDepartment != null) {
            jpql += " AND i.performDepartment = :peformingDepartment ";
            params.put("peformingDepartment", getPerformingDepartment());
        }

        if (collectionCenter != null) {
            jpql += " AND (i.billItem.bill.collectingCentre = :collectionCenter OR i.billItem.bill.fromInstitution = :collectionCenter) ";
            params.put("collectionCenter", getCollectionCenter());
        }

        if (route != null) {
            jpql += " AND (i.billItem.bill.collectingCentre.route = :route OR i.billItem.bill.fromInstitution.route = :route) ";
            params.put("route", getRoute());
        }

        if (patientName != null && !patientName.trim().isEmpty()) {
            jpql += " AND i.billItem.bill.patient.person.name LIKE :patientName ";
            params.put("patientName", "%" + getPatientName().trim() + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            jpql += " AND i.billItem.bill.ipOpOrCc = :tp ";
            params.put("tp", getType().trim());
        }

        if (referringDoctor != null) {
            jpql += " AND i.billItem.bill.referringDoctor = :referringDoctor ";
            params.put("referringDoctor", getReferringDoctor());
        }

        if (investigationName != null && !investigationName.trim().isEmpty()) {
            jpql += " AND i.billItem.item.name like :investigation ";
            params.put("investigation", "%" + investigationName.trim() + "%");
        }

        if (patientInvestigationStatus != null) {
            jpql += " AND i.status = :patientInvestigationStatus ";
            params.put("patientInvestigationStatus", getPatientInvestigationStatus());
        }

        jpql += " ORDER BY i.id DESC";

        params.put("ret", false);

        items = patientInvestigationFacade.findByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.searchPatientInvestigationsWithoutSampleId", sessionController.getLoggedUser());
    }

    public List<Long> getPatientSampleComponentsByInvestigation(PatientInvestigation patientInvestigation) {

        List<Long> sampleIds = new ArrayList();
        String jpql = "SELECT psc.patientSample.id "
                + " FROM PatientSampleComponant psc "
                + " WHERE psc.retired=:retired "
                + " AND psc.patientInvestigation=:patientInvestigation";
        Map<String, Object> params = new HashMap<>();
        params.put("retired", false);  // Assuming you want only non-retired records
        params.put("patientInvestigation", patientInvestigation);
        sampleIds = patientSampleComponantFacade.findLongList(jpql, params);
        return sampleIds;
    }

    public List<PatientReportLight> patientReports(PatientInvestigation pi) {
        String jpql = "SELECT new com.divudi.core.data.PatientReportLight("
                + " r.id, r.approved, r.printed, r.reportType, r.qrCodeContentsLink)"
                + " from PatientReport r "
                + " where r.patientInvestigation=:pi"
                + " and r.retired = :ret ";
        Map params = new HashMap();
        params.put("pi", pi);
        params.put("ret", false);
        return patientReportFacade.findLightsByJpql(jpql, params);
    }

    public boolean hasPatientReports(PatientInvestigation pi) {
        String jpql = "SELECT r"
                + " from PatientReport r "
                + " where r.patientInvestigation=:pi"
                + " and r.retired = :ret ";
        Map params = new HashMap();
        params.put("pi", pi);
        params.put("ret", false);
        PatientReport pr = patientReportFacade.findFirstByJpql(jpql, params);

        if (pr == null) {
            return false;
        } else {
            return true;
        }
    }

    public void removePatientReport(Long patientReportID) {
        reportTimerController.trackReportExecution(() -> {
        PatientReport currentPatientReport = patientReportFacade.find(patientReportID);

        if (currentPatientReport == null) {
            JsfUtil.addErrorMessage("No Patient Report");
            return;
        }
        if (comment == null || comment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Add Comment");
            return;
        }

        currentPatientReport.setRetireComments(comment);
        currentPatientReport.setRetired(Boolean.TRUE);
        currentPatientReport.setRetiredAt(Calendar.getInstance().getTime());
        currentPatientReport.setRetirer(sessionController.getLoggedUser());

        if (currentPatientReport.getReportType() == ReportType.UPLOAD) {
            Upload currentReportUpload = patientReportUploadController.loadUploads(currentPatientReport);

            if (currentReportUpload != null) {
                currentReportUpload.setRetireComments(comment);
                currentReportUpload.setRetired(true);
                currentReportUpload.setRetiredAt(new Date());
                currentReportUpload.setRetirer(sessionController.getLoggedUser());
                uploadFacade.edit(currentReportUpload);
            }
        }
        patientReportFacade.edit(currentPatientReport);
        comment = null;
        JsfUtil.addSuccessMessage("Successfully Removed");
        searchPatientReports();
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.removePatientReport", sessionController.getLoggedUser());
    }

    public void searchPatientReports() {
        reportTimerController.trackReportExecution(() -> {
        if (filteringStatus == null) {
            searchPatientInvestigations();
        } else if (filteringStatus.equalsIgnoreCase("Processing")) {
            searchProcessingPatientReports();
        } else {
            searchPendingAndApprovedPatientReports();
        }
        listingEntity = ListingEntity.PATIENT_REPORTS;
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.searchPatientReports", sessionController.getLoggedUser());

    }

    public void searchProcessingPatientReports() {
        reportTimerController.trackReportExecution(() -> {
        searchPatientInvestigations();

        List<PatientInvestigation> processingList = new ArrayList<>();

        for (PatientInvestigation pi : items) {
            if (!hasPatientReports(pi)) {
                processingList.add(pi);
            }
        }
        setItems(processingList);
        listingEntity = ListingEntity.PATIENT_REPORTS;
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.searchProcessingPatientReports", sessionController.getLoggedUser());
    }

    public void searchPatientReportPrint() {
        reportTimerController.trackReportExecution(() -> {
        if (filteringStatus == null) {
            searchPatientInvestigations();
        } else if (filteringStatus.equalsIgnoreCase("Processing")) {
            searchProcessingPatientReports();
        } else {
            searchPendingAndApprovedPatientReports();
        }
        listingEntity = ListingEntity.REPORT_PRINT;
        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.searchPatientReportPrint", sessionController.getLoggedUser());
    }

    public void searchPendingAndApprovedPatientReports() {
        reportTimerController.trackReportExecution(() -> {
        listingEntity = ListingEntity.PATIENT_REPORTS;
        List<PatientReport> patientReports = new ArrayList<>();
        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "SELECT r "
                + " FROM PatientReport r "
                + " WHERE r.retired = :ret "
                + " AND r.patientInvestigation.billItem.bill.createdAt BETWEEN :fd AND :td ";

        params.put("fd", getFromDate());
        params.put("td", getToDate());

        if (orderedInstitution != null) {
            jpql += " AND r.patientInvestigation.billItem.bill.institution = :orderedInstitution ";
            params.put("orderedInstitution", getOrderedInstitution());
        }

        if (orderedDepartment != null) {
            jpql += " AND r.patientInvestigation.billItem.bill.department = :orderedDepartment ";
            params.put("orderedDepartment", getOrderedDepartment());
        }

        if (performingInstitution != null) {
            jpql += " AND r.approveInstitution = :performingInstitution ";
            params.put("performingInstitution", getPerformingInstitution());
        }

        if (performingDepartment != null) {
            jpql += " AND r.approveDepartment = :performingDepartment ";
            params.put("performingDepartment", getPerformingDepartment());
        }

        if (collectionCenter != null) {
            jpql += " AND (r.patientInvestigation.billItem.bill.collectingCentre = :collectionCenter OR r.patientInvestigation.billItem.bill.fromInstitution = :collectionCenter) ";
            params.put("collectionCenter", getCollectionCenter());
        }

        if (route != null) {
            jpql += " AND (r.patientInvestigation.billItem.bill.collectingCentre.route = :route OR r.patientInvestigation.billItem.bill.fromInstitution.route = :route) ";
            params.put("route", getRoute());
        }

        if (patientName != null && !patientName.trim().isEmpty()) {
            jpql += " AND r.patientInvestigation.billItem.bill.patient.person.name LIKE :patientName ";
            params.put("patientName", "%" + getPatientName().trim() + "%");
        }

        if (type != null && !type.trim().isEmpty()) {
            jpql += " AND r.patientInvestigation.billItem.bill.ipOpOrCc = :tp ";
            params.put("tp", getType().trim());
        }

        if (referringDoctor != null) {
            jpql += " AND r.patientInvestigation.billItem.bill.referringDoctor = :referringDoctor ";
            params.put("referringDoctor", getReferringDoctor());
        }

        if (investigationName != null && !investigationName.trim().isEmpty()) {
            jpql += " AND r.patientInvestigation.billItem.item.name like :investigation ";
            params.put("investigation", "%" + investigationName.trim() + "%");
        }

        if (patientInvestigationStatus != null) {
            jpql += " AND r.status = :patientReportStatus ";
            params.put("patientReportStatus", patientInvestigationStatus);
        }

        if ("Pending".equals(filteringStatus)) {
            jpql += " AND r.approved = :approved ";
            params.put("approved", false);
        }

        if ("Approved".equals(filteringStatus)) {
            jpql += " AND r.approved = :approved ";
            params.put("approved", true);
        }

        jpql += " group by r, r.patientInvestigation ";
        jpql += " ORDER BY r.id DESC";
        params.put("ret", false);

        patientReports = patientReportFacade.findByJpqlWithoutCache(jpql, params, TemporalType.TIMESTAMP);

        items = new ArrayList();

        for (PatientReport pr : patientReports) {
            if (!items.contains(pr.getPatientInvestigation())) {
                items.add(pr.getPatientInvestigation());
            }
        }

        }, CommonReports.LAB_DASHBOARD, "LaboratoryManagementController.searchPendingAndApprovedPatientReports", sessionController.getLoggedUser());
    }

    public Department getDefaultSampleSendingDepartment() {
        if (configOptionApplicationController.getBooleanValueByKey("Set the default sample department as the parent department (Super Department) of the current department.", false)) {
            if (sessionController.getDepartment().getSuperDepartment() != null) {
                return sessionController.getDepartment().getSuperDepartment();
            } else {
                return sessionController.getDepartment();
            }
        }
        return sampleSendingDepartment;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getter & Setter">
    public ListingEntity getListingEntity() {
        return listingEntity;
    }

    public void setListingEntity(ListingEntity listingEntity) {
        this.listingEntity = listingEntity;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Staff getReferringDoctor() {
        return referringDoctor;
    }

    public void setReferringDoctor(Staff referringDoctor) {
        this.referringDoctor = referringDoctor;
    }

    public PatientInvestigationStatus getPatientInvestigationStatus() {
        return patientInvestigationStatus;
    }

    public void setPatientInvestigationStatus(PatientInvestigationStatus patientInvestigationStatus) {
        this.patientInvestigationStatus = patientInvestigationStatus;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Institution getCollectionCenter() {
        return collectionCenter;
    }

    public void setCollectionCenter(Institution collectionCenter) {
        this.collectionCenter = collectionCenter;
    }

    public Institution getOrderedInstitution() {
        return orderedInstitution;
    }

    public void setOrderedInstitution(Institution orderedInstitution) {
        this.orderedInstitution = orderedInstitution;
    }

    public Department getOrderedDepartment() {
        return orderedDepartment;
    }

    public void setOrderedDepartment(Department orderedDepartment) {
        this.orderedDepartment = orderedDepartment;
    }

    public Institution getPerformingInstitution() {
        return performingInstitution;
    }

    public void setPerformingInstitution(Institution performingInstitution) {
        this.performingInstitution = performingInstitution;
    }

    public Department getPerformingDepartment() {
        return performingDepartment;
    }

    public void setPerformingDepartment(Department performingDepartment) {
        this.performingDepartment = performingDepartment;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public List<BillBarcode> getBillBarcodes() {
        return billBarcodes;
    }

    public void setBillBarcodes(List<BillBarcode> billBarcodes) {
        this.billBarcodes = billBarcodes;
    }

    public List<BillBarcode> getSelectedBillBarcodes() {
        return selectedBillBarcodes;
    }

    public void setSelectedBillBarcodes(List<BillBarcode> selectedBillBarcodes) {
        this.selectedBillBarcodes = selectedBillBarcodes;
    }

    public Bill getCurrentBill() {
        return currentBill;
    }

    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
    }

    public boolean isPrintIndividualBarcodes() {
        return printIndividualBarcodes;
    }

    public void setPrintIndividualBarcodes(boolean printIndividualBarcodes) {
        this.printIndividualBarcodes = printIndividualBarcodes;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public List<PatientSample> getPatientSamples() {
        return patientSamples;
    }

    public void setPatientSamples(List<PatientSample> patientSamples) {
        this.patientSamples = patientSamples;
    }

    public List<PatientInvestigationStatus> getAvailableStatus() {
        return availableStatus;
    }

    public void setAvailableStatus(List<PatientInvestigationStatus> availableStatus) {
        this.availableStatus = availableStatus;
    }

    public List<PatientSample> getSelectedPatientSamples() {
        return selectedPatientSamples;
    }

    public void setSelectedPatientSamples(List<PatientSample> selectedPatientSamples) {
        this.selectedPatientSamples = selectedPatientSamples;
    }

    public boolean isSelectAll() {
        return selectAll;
    }

    public void setSelectAll(boolean selectAll) {
        this.selectAll = selectAll;
    }

    public String getSampleRejectionComment() {
        return sampleRejectionComment;
    }

    public void setSampleRejectionComment(String sampleRejectionComment) {
        this.sampleRejectionComment = sampleRejectionComment;
    }

    public Staff getSampleTransportedToLabByStaff() {
        return sampleTransportedToLabByStaff;
    }

    public void setSampleTransportedToLabByStaff(Staff sampleTransportedToLabByStaff) {
        this.sampleTransportedToLabByStaff = sampleTransportedToLabByStaff;
    }

    public List<PatientInvestigation> getItems() {
        return items;
    }

    public void setItems(List<PatientInvestigation> items) {
        this.items = items;
    }

    public String getInvestigationName() {
        return investigationName;
    }

    public void setInvestigationName(String investigationName) {
        this.investigationName = investigationName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getFilteringStatus() {
        return filteringStatus;
    }

    public void setFilteringStatus(String filteringStatus) {
        this.filteringStatus = filteringStatus;
    }

    public Department getSampleSendingDepartment() {
        return getDefaultSampleSendingDepartment();
    }

    public void setSampleSendingDepartment(Department sampleSendingDepartment) {
        this.sampleSendingDepartment = sampleSendingDepartment;
    }

// </editor-fold>
    public Department getSampleReceiveFromDepartment() {
        return sampleReceiveFromDepartment;
    }

    public void setSampleReceiveFromDepartment(Department sampleReceiveFromDepartment) {
        this.sampleReceiveFromDepartment = sampleReceiveFromDepartment;
    }
}
