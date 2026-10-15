package com.divudi.bean.collectingCentre;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UserNotificationController;
import com.divudi.bean.common.WebUserController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.bean.lab.PatientReportController;
import com.divudi.bean.lab.PatientReportUploadController;
import com.divudi.bean.report.ReportController;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.lab.CollectingCentreBillingType;
import com.divudi.core.data.lab.ListingEntity;
import com.divudi.core.data.lab.SearchDateType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Upload;
import com.divudi.core.entity.UserNotification;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientReportFacade;
import com.divudi.core.facade.PatientSampleFacade;
import com.divudi.core.facade.UserNotificationFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backs the self-service home page landed on by a Collecting Centre
 * department's users (department type Collecting Centre), as opposed to the
 * admin-side collecting_centre/index.xhtml pages used by hospital staff to
 * manage collecting centres on their behalf.
 */
@Named
@SessionScoped
public class CollectingCentreSelfCommonController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;
    @Inject
    private ReportController reportController;
    @Inject
    private PatientInvestigationController patientInvestigationController;
    @Inject
    private PatientReportController patientReportController;
    @Inject
    private PatientReportUploadController patientReportUploadController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private UserNotificationController userNotificationController;

    @EJB
    private BillFacade billFacade;
    @EJB
    private PatientSampleFacade patientSampleFacade;
    @EJB
    private PatientReportFacade patientReportFacade;
    @EJB
    private UserNotificationFacade userNotificationFacade;

    private PatientReport printingReport;
    private Privileges unauthorizedPrivilege;
    private UserNotification viewingNotification;

    // <editor-fold defaultstate="collapsed" desc="Privileges">
    public boolean hasSelfPrivilege(Privileges privilege) {
        return webUserController.hasPrivilege(privilege.name());
    }

    /**
     * Opens the self-service "not authorized" page naming the missing
     * privilege. Every self-service navigation method returns this when the
     * user lacks the privilege, so the dashboard can show all its buttons.
     */
    public String navigateToSelfUnauthorized(Privileges privilege) {
        unauthorizedPrivilege = privilege;
        return "/collecting_centre/cc_self_unauthorized?faces-redirect=true";
    }

    /**
     * For in-page actions (no navigation): adds an error and returns false
     * when the user lacks the privilege.
     */
    public boolean checkSelfPrivilege(Privileges privilege) {
        if (hasSelfPrivilege(privilege)) {
            return true;
        }
        JsfUtil.addErrorMessage("You are not authorized to use " + privilege.getLabel());
        return false;
    }

    public Privileges getUnauthorizedPrivilege() {
        return unauthorizedPrivilege;
    }
    // </editor-fold>

    public String navigateToCollectingCentreSelfBillingHome() {
        if (!hasSelfPrivilege(Privileges.CcSelfBillingMenu)) {
            return navigateToSelfUnauthorized(Privileges.CcSelfBillingMenu);
        }
        return "/collecting_centre/cc_self_index?faces-redirect=true";
    }

    public String navigateToCollectingCentreSelfStatement() {
        if (!hasSelfPrivilege(Privileges.CcSelfStatement)) {
            return navigateToSelfUnauthorized(Privileges.CcSelfStatement);
        }
        reportController.setFromDate(CommonFunctions.getStartOfMonth());
        reportController.setToDate(CommonFunctions.getEndOfDay());
        reportController.setInvoiceNumber(null);
        reportController.setAgentHistories(null);
        restrictStatementToOwnCollectingCentre();
        return "/collecting_centre/cc_self_statement?faces-redirect=true";
    }

    /**
     * Reuses the admin Collection Centre Statement query, but always pins it
     * to the logged-in Collecting Centre so a centre can never see another
     * centre's balance history.
     */
    public void processCollectingCentreSelfStatement() {
        if (!checkSelfPrivilege(Privileges.CcSelfStatement)) {
            reportController.setAgentHistories(null);
            return;
        }
        if (!restrictStatementToOwnCollectingCentre()) {
            reportController.setAgentHistories(null);
            return;
        }
        // has*Date, not get*Date: the getters silently default a cleared date to today
        if (!reportController.hasFromDate() || !reportController.hasToDate()) {
            reportController.setAgentHistories(null);
            JsfUtil.addErrorMessage("Please select From and To dates");
            return;
        }
        reportController.processCollectingCentreStatementReportNew();
    }

    /**
     * Re-processes the current criteria before exporting, so the PDF never
     * carries rows from an earlier Process run under the new criteria.
     */
    public void exportCollectingCentreSelfStatementToPDF() {
        processCollectingCentreSelfStatement();
        if (reportController.getAgentHistories() == null) {
            return;
        }
        reportController.exportCollectionCenterStatementReportToPDF();
    }

    private boolean restrictStatementToOwnCollectingCentre() {
        Institution collectingCentre = sessionController.getInstitution();
        reportController.setInstitution(null);
        reportController.setCollectingCentre(collectingCentre);
        if (collectingCentre == null || !isCollectingCentreDepartment()) {
            JsfUtil.addErrorMessage("Collecting centre not found for the logged department");
            return false;
        }
        return true;
    }

    // <editor-fold defaultstate="collapsed" desc="Sample Management">
    /**
     * Opens the self-service Sample Management page. Reuses the lab Sample
     * Management logic of PatientInvestigationController, but every search
     * is pinned to the logged-in Collecting Centre and every action first
     * checks the bills/samples/reports belong to it.
     */
    public String navigateToCollectingCentreSelfSampleManagement() {
        if (!hasSelfPrivilege(Privileges.CcSelfSampleManagement)) {
            return navigateToSelfUnauthorized(Privileges.CcSelfSampleManagement);
        }
        patientInvestigationController.makeNull();
        patientInvestigationController.setSelectedPatientSamples(null);
        setSelfSampleManagementDefaults();
        restrictSampleManagementToOwnCollectingCentre();
        return "/collecting_centre/cc_self_sample_management?faces-redirect=true";
    }

    public String navigateToSelfSampleManagementFromBill(Bill bill) {
        List<Bill> bills = new ArrayList<>();
        if (bill != null) {
            bills.add(bill);
        }
        return navigateToSelfSampleManagementFromBills(bills);
    }

    /**
     * Opens the self-service Sample Management page listing only the given
     * bills, e.g. from the bill print page right after settling or from a
     * bill opened through Search Bills. Bills of other centres are refused.
     */
    public String navigateToSelfSampleManagementFromBills(List<Bill> bills) {
        if (!hasSelfPrivilege(Privileges.CcSelfSampleManagement)) {
            return navigateToSelfUnauthorized(Privileges.CcSelfSampleManagement);
        }
        if (bills == null || bills.isEmpty()) {
            JsfUtil.addErrorMessage("No bill selected");
            return null;
        }
        List<Bill> ownBills = new ArrayList<>();
        for (Bill b : bills) {
            Bill ownBill = fetchOwnBill(b);
            if (ownBill == null) {
                return null;
            }
            ownBills.add(ownBill);
        }
        patientInvestigationController.makeNull();
        patientInvestigationController.setSelectedPatientSamples(null);
        setSelfSampleManagementDefaults();
        restrictSampleManagementToOwnCollectingCentre();
        patientInvestigationController.setListingEntity(ListingEntity.BILLS);
        patientInvestigationController.setBills(ownBills);
        return "/collecting_centre/cc_self_sample_management?faces-redirect=true";
    }

    public String navigateBackToCollectingCentreSelfSampleManagement() {
        printingReport = null;
        if (!hasSelfPrivilege(Privileges.CcSelfSampleManagement)) {
            return navigateToSelfUnauthorized(Privileges.CcSelfSampleManagement);
        }
        return "/collecting_centre/cc_self_sample_management?faces-redirect=true";
    }

    public void searchSelfBills() {
        if (!checkSelfPrivilege(Privileges.CcSelfSampleManagement)) {
            patientInvestigationController.setBills(null);
            return;
        }
        if (!restrictSampleManagementToOwnCollectingCentre()) {
            patientInvestigationController.setBills(null);
            return;
        }
        patientInvestigationController.searchBills();
    }

    public void searchSelfSamples() {
        if (!checkSelfPrivilege(Privileges.CcSelfSampleManagement)) {
            patientInvestigationController.setPatientSamples(null);
            return;
        }
        if (!restrictSampleManagementToOwnCollectingCentre()) {
            patientInvestigationController.setPatientSamples(null);
            return;
        }
        patientInvestigationController.setSelectedPatientSamples(null);
        patientInvestigationController.searchPatientSamples();
    }

    /**
     * Lists the centre's patient investigations; the page shows every report
     * of each investigation in its Action column.
     */
    public void searchSelfReports() {
        if (!checkSelfPrivilege(Privileges.CcSelfSampleManagement)) {
            patientInvestigationController.setItems(null);
            return;
        }
        if (!restrictSampleManagementToOwnCollectingCentre()) {
            patientInvestigationController.setItems(null);
            return;
        }
        patientInvestigationController.searchPatientInvestigations();
    }

    public List<PatientReport> findSelfReportsOfInvestigation(PatientInvestigation investigation) {
        if (investigation == null || investigation.getId() == null) {
            return new ArrayList<>();
        }
        String jpql = "SELECT r "
                + " FROM PatientReport r "
                + " WHERE r.retired = :ret "
                + " AND r.patientInvestigation.id = :piId "
                + " ORDER BY r.id";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("piId", investigation.getId());
        return patientReportFacade.findByJpql(jpql, params);
    }

    public void clearSelfSampleManagementFilters() {
        patientInvestigationController.setPatientName(null);
        patientInvestigationController.setSampleId(null);
        patientInvestigationController.setPatientInvestigationStatus(null);
        patientInvestigationController.setSampleTransportedToLabByStaff(null);
        patientInvestigationController.setSampleRejectionComment(null);
        setSelfSampleManagementDefaults();
    }

    private void setSelfSampleManagementDefaults() {
        patientInvestigationController.setFromDate(CommonFunctions.getStartOfDay());
        patientInvestigationController.setToDate(CommonFunctions.getEndOfDay());
        patientInvestigationController.setInvestigationName(null);
        patientInvestigationController.setCollectingCentreBillingType(CollectingCentreBillingType.AGENT_BILLING);
    }

    public void generateSelfBarcodes(Bill bill) {
        if (!checkSelfPrivilege(Privileges.CcSelfSampleBarcodePrint)) {
            return;
        }
        Bill ownBill = fetchOwnBill(bill);
        if (ownBill == null) {
            return;
        }
        if (!isSelfBill(ownBill)) {
            JsfUtil.addErrorMessage("This is a Hospital Billing bill and barcodes cannot be generated here");
            return;
        }
        patientInvestigationController.generateBarcodesForSelectedBill(ownBill);
    }

    public void listSelfSamplesOfBill(Bill bill) {
        Bill ownBill = fetchOwnBill(bill);
        if (ownBill == null) {
            return;
        }
        patientInvestigationController.setSelectedPatientSamples(null);
        patientInvestigationController.navigateToSamplesFromSelectedBill(ownBill);
    }

    /**
     * Shows the barcode sticker of only the clicked sample. Hospital Billing
     * samples are refused, like every other barcode action here.
     */
    public void viewSelfSampleBarcode(PatientSample sample) {
        if (!checkSelfPrivilege(Privileges.CcSelfSampleBarcodePrint)) {
            return;
        }
        if (!restrictSampleManagementToOwnCollectingCentre()) {
            return;
        }
        PatientSample fetched = (sample == null || sample.getId() == null) ? null : patientSampleFacade.find(sample.getId());
        if (fetched == null || !isOwnBill(fetched.getBill())) {
            JsfUtil.addErrorMessage("This sample does not belong to your collecting centre");
            return;
        }
        if (!isSelfBill(fetched.getBill())) {
            JsfUtil.addErrorMessage("This is a Hospital Billing sample and its barcode cannot be printed here");
            return;
        }
        patientInvestigationController.navigateToThePatientSample(fetched);
    }

    public void listSelfReportsOfBill(Bill bill) {
        Bill ownBill = fetchOwnBill(bill);
        if (ownBill == null) {
            return;
        }
        patientInvestigationController.navigateToInvestigationsFromSelectedBill(ownBill);
    }

    public void collectSelfSamples() {
        if (!checkSelfPrivilege(Privileges.CcSelfSampleCollect)) {
            return;
        }
        if (!areSelectedSamplesOwn()) {
            return;
        }
        patientInvestigationController.collectSamples();
    }

    public void sendSelfSamples() {
        if (!checkSelfPrivilege(Privileges.CcSelfSampleSend)) {
            return;
        }
        if (!areSelectedSamplesOwn()) {
            return;
        }
        patientInvestigationController.sendSamplesToLab(true);
    }

    public void rejectSelfSamples() {
        if (!checkSelfPrivilege(Privileges.CcSelfSampleReject)) {
            return;
        }
        if (!areSelectedSamplesOwn()) {
            return;
        }
        if (!areSelectedSamplesCollected()) {
            return;
        }
        if (!areSelectedSamplesNotSent()) {
            return;
        }
        patientInvestigationController.rejectSamples();
    }

    /**
     * Only a collected sample can be rejected; one that only has a barcode
     * has not been taken yet. Reads the collected flag from the database,
     * not the page copy.
     */
    private boolean areSelectedSamplesCollected() {
        for (PatientSample ps : patientInvestigationController.getSelectedPatientSamples()) {
            PatientSample fetched = patientSampleFacade.find(ps.getId());
            if (fetched == null || !Boolean.TRUE.equals(fetched.getSampleCollected())) {
                JsfUtil.addErrorMessage("Sample " + ps.getId() + " has not been collected yet and cannot be rejected");
                return false;
            }
        }
        return true;
    }

    /**
     * Once a sample is sent to the lab, the centre can no longer reject it.
     * Reads the sent flag from the database, not the page copy.
     */
    private boolean areSelectedSamplesNotSent() {
        for (PatientSample ps : patientInvestigationController.getSelectedPatientSamples()) {
            PatientSample fetched = patientSampleFacade.find(ps.getId());
            if (fetched != null && Boolean.TRUE.equals(fetched.getSampleSent())) {
                JsfUtil.addErrorMessage("Sample " + ps.getId() + " has already been sent and cannot be rejected");
                return false;
            }
        }
        return true;
    }

    public void reGenerateSelfSamples() {
        if (!checkSelfPrivilege(Privileges.CcSelfSampleRegenerate)) {
            return;
        }
        if (!areSelectedSamplesOwn()) {
            return;
        }
        patientInvestigationController.reGenerateSampleForRejectSamples();
        
       patientInvestigationController.setListingEntity(ListingEntity.PATIENT_SAMPLES);
    }

    /**
     * Reuses the courier report print flow (it loads the report or its
     * upload), but swaps the courier pages for the self-service ones, which
     * have no hospital menu. Only approved reports of this centre print.
     */
    public String navigateToSelfReportPrint(PatientReport report) {
        printingReport = null;
        if (!restrictSampleManagementToOwnCollectingCentre()) {
            return null;
        }
        if (report == null || report.getId() == null) {
            JsfUtil.addErrorMessage("No report selected");
            return null;
        }
        PatientReport fetched = patientReportFacade.find(report.getId());
        if (fetched == null || !isOwnReport(fetched)) {
            JsfUtil.addErrorMessage("This report does not belong to your collecting centre");
            return null;
        }
        if (!Boolean.TRUE.equals(fetched.getApproved())) {
            JsfUtil.addErrorMessage("Only approved reports can be printed");
            return null;
        }
        String outcome = patientReportController.navigateToPrintPatientReportForCourier(fetched);
        if (outcome == null || outcome.isEmpty()) {
            return null;
        }
        printingReport = fetched;
        if (outcome.startsWith("/collecting_centre/courier/upload_patient_report_print")) {
            return "/collecting_centre/cc_self_upload_report_print?faces-redirect=true";
        }
        return "/collecting_centre/cc_self_report_print?faces-redirect=true";
    }

    /**
     * Guards the self-service report print page, which can be reached without
     * navigateToSelfReportPrint(): the report on PatientReportController must
     * be the approved, own-centre report that was opened from this section.
     */
    public boolean isSelfReportPrintAllowed() {
        PatientReport current = patientReportController.getCurrentPatientReport();
        return isPrintingReportValid()
                && current != null
                && printingReport.getId().equals(current.getId());
    }

    public boolean isSelfUploadReportPrintAllowed() {
        Upload upload = patientReportUploadController.getReportUpload();
        return isPrintingReportValid()
                && upload != null
                && upload.getPatientInvestigation() != null
                && printingReport.getPatientInvestigation() != null
                && upload.getPatientInvestigation().getId().equals(printingReport.getPatientInvestigation().getId());
    }

    private boolean isPrintingReportValid() {
        return isCollectingCentreDepartment()
                && printingReport != null
                && printingReport.getId() != null
                && Boolean.TRUE.equals(printingReport.getApproved())
                && isOwnReport(printingReport);
    }

    /**
     * Pins the shared lab search to this Collecting Centre. Filters in
     * PatientInvestigationController only narrow a search, so pinning the
     * collection centre is enough to hide other centres' records.
     */
    private boolean restrictSampleManagementToOwnCollectingCentre() {
        Institution collectingCentre = sessionController.getInstitution();
        patientInvestigationController.setCollectionCenter(collectingCentre);
        patientInvestigationController.setSearchDateType(SearchDateType.ORDERED_DATE);
        if (collectingCentre == null || !isCollectingCentreDepartment()) {
            JsfUtil.addErrorMessage("Collecting centre not found for the logged department");
            return false;
        }
        return true;
    }

    private Bill fetchOwnBill(Bill bill) {
        if (!restrictSampleManagementToOwnCollectingCentre()) {
            return null;
        }
        if (bill == null || bill.getId() == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return null;
        }
        Bill fetched = billFacade.find(bill.getId());
        if (fetched == null || !isOwnBill(fetched)) {
            JsfUtil.addErrorMessage("This bill does not belong to your collecting centre");
            return null;
        }
        return fetched;
    }

    private boolean areSelectedSamplesOwn() {
        if (!restrictSampleManagementToOwnCollectingCentre()) {
            return false;
        }
        List<PatientSample> selected = patientInvestigationController.getSelectedPatientSamples();
        if (selected == null || selected.isEmpty()) {
            JsfUtil.addErrorMessage("No samples selected");
            return false;
        }
        for (PatientSample ps : selected) {
            PatientSample fetched = ps.getId() == null ? null : patientSampleFacade.find(ps.getId());
            if (fetched == null || !isOwnBill(fetched.getBill())) {
                JsfUtil.addErrorMessage("Sample " + ps.getId() + " does not belong to your collecting centre");
                return false;
            }
            if (!isSelfBill(fetched.getBill())) {
                JsfUtil.addErrorMessage("Sample " + ps.getId() + " is a Hospital Billing sample and cannot be updated here");
                return false;
            }
        }
        return true;
    }

    /**
     * A sample billed by the centre itself (Self Billing) can be updated
     * here; one billed at the hospital for the centre (Hospital Billing) is
     * view only.
     */
    public boolean isSelfBilledSample(PatientSample sample) {
        return sample != null && isSelfBill(sample.getBill());
    }

    public boolean isSelfBill(Bill bill) {
        return bill != null
                && bill.getDepartment() != null
                && bill.getDepartment().getDepartmentType() == DepartmentType.CollectingCentre;
    }

    private boolean isOwnReport(PatientReport report) {
        return report.getPatientInvestigation() != null
                && report.getPatientInvestigation().getBillItem() != null
                && isOwnBill(report.getPatientInvestigation().getBillItem().getBill());
    }

    private boolean isOwnBill(Bill bill) {
        Institution collectingCentre = sessionController.getInstitution();
        return collectingCentre != null
                && bill != null
                && (collectingCentre.equals(bill.getFromInstitution())
                || collectingCentre.equals(bill.getCollectingCentre()));
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Notifications">
    /**
     * Opens one of the logged user's notifications in the dashboard popup and
     * marks it seen. A notification can only be removed once it has been seen.
     */
    public void viewSelfNotification(UserNotification un) {
        if (!isOwnNotification(un)) {
            JsfUtil.addErrorMessage("Invalid notification");
            return;
        }
        viewingNotification = un;
        if (!un.isSeen()) {
            un.setSeen(true);
            userNotificationFacade.edit(un);
        }
    }

    /**
     * Removes (retires) one of the logged user's notifications. CC
     * notifications are not tied to a bill, room or encounter department, so
     * the general UserNotificationController remove does not apply here.
     */
    public void removeSelfNotification(UserNotification un) {
        if (!isOwnNotification(un)) {
            JsfUtil.addErrorMessage("Nothing to remove");
            return;
        }
        if (!un.isSeen()) {
            JsfUtil.addErrorMessage("View the notification before removing it");
            return;
        }
        un.setRetired(true);
        un.setRetiredAt(new Date());
        un.setRetirer(sessionController.getLoggedUser());
        un.setRetireComments("Removed from collecting centre self billing");
        userNotificationFacade.edit(un);
        if (un.equals(viewingNotification)) {
            viewingNotification = null;
        }
        userNotificationController.filterNotificationsByCriteria();
        JsfUtil.addSuccessMessage("Notification removed");
    }

    private boolean isOwnNotification(UserNotification un) {
        return un != null
                && un.getId() != null
                && un.getWebUser() != null
                && un.getWebUser().equals(sessionController.getLoggedUser());
    }

    public UserNotification getViewingNotification() {
        return viewingNotification;
    }
    // </editor-fold>

    public boolean isCollectingCentreDepartment() {
        return sessionController.getDepartment() != null
                && sessionController.getDepartment().getDepartmentType() == DepartmentType.CollectingCentre;
    }
}
