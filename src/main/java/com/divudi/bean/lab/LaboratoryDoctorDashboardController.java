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
public class LaboratoryDoctorDashboardController implements Serializable {

    private static final long serialVersionUID = 1L;

    public LaboratoryDoctorDashboardController() {
        
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
    public String navigateToDoctorDashboard() {
        return "/lab/laboratory_doctor_dashboard?faces-redirect=true";
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

    public void searchPatientInvestigations() {
        items = new ArrayList();

        if (sampleId != null) {
            try {
                Long id = Long.valueOf(sampleId);
                searchPatientInvestigationsWithSampleId(id);
            } catch (NumberFormatException e) {
                searchPatientInvestigationsWithoutSampleId();
            }
        }
        
    }

    public void searchPatientInvestigationsWithSampleId(Long sampleID) {
        listingEntity = ListingEntity.PATIENT_INVESTIGATIONS;
        String jpql;
        Map<String, Object> params = new HashMap<>();

        // Query PatientSampleComponent to get PatientInvestigations
        jpql = "SELECT new com.divudi.core.data.dto.PharmacyIncomeBillDTO( psc.patientInvestigation.) "
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

        items = patientInvestigationFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        
    }

    public void searchPatientInvestigationsWithoutSampleId() {
        
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

        items = patientInvestigationFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        
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

        patientReports = patientReportFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

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
