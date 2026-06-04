package com.divudi.bean.dataAdmin;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.PatientMergeStatus;
import com.divudi.core.data.PatientMergeType;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientMergeRecord;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PatientMergeRecordFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.patient.PatientMergeService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@SessionScoped
public class PatientMergeController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private PatientMergeRecordFacade patientMergeRecordFacade;
    @EJB
    private PatientMergeService patientMergeService;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private SessionController sessionController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Manual merge state">
    private Patient primaryPatient;
    private Patient secondaryPatient;
    private String mergeReason;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Deterministic scan state">
    private int detMaxResults = 50;
    private List<PatientPair> deterministicResults = new ArrayList<>();
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="History state">
    private Date historyFromDate;
    private Date historyToDate;
    private PatientMergeStatus historyStatus;
    private List<PatientMergeRecord> historyResults = new ArrayList<>();
    private PatientMergeRecord selectedMergeRecord;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Navigation">
    public String navigateToPatientDataManagement() {
        initHistoryDates();
        primaryPatient = null;
        secondaryPatient = null;
        mergeReason = null;
        deterministicResults = new ArrayList<>();
        historyResults = new ArrayList<>();
        return "/dataAdmin/patient_data_management?faces-redirect=true";
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Manual Merge">
    public void executeMerge() {
        if (primaryPatient == null || secondaryPatient == null) {
            JsfUtil.addErrorMessage("Please select both a primary and a secondary patient.");
            return;
        }
        if (primaryPatient.getId().equals(secondaryPatient.getId())) {
            JsfUtil.addErrorMessage("Primary and secondary patients must be different.");
            return;
        }
        try {
            patientMergeService.merge(primaryPatient, secondaryPatient,
                    PatientMergeType.MANUAL, mergeReason, sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Patients merged successfully.");
            primaryPatient = null;
            secondaryPatient = null;
            mergeReason = null;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Merge failed: " + e.getMessage());
        }
    }

    public List<Patient> completePrimaryPatient(String query) {
        return searchPatients(query);
    }

    public List<Patient> completeSecondaryPatient(String query) {
        return searchPatients(query);
    }

    private List<Patient> searchPatients(String query) {
        if (query == null || query.trim().length() < 2) {
            return new ArrayList<>();
        }
        String jpql = "select p from Patient p where p.retired = false "
                + "and (lower(p.person.name) like :q or p.phn like :q or p.person.nic like :q or p.code like :q) "
                + "order by p.person.name";
        Map<String, Object> params = new HashMap<>();
        params.put("q", "%" + query.toLowerCase() + "%");
        return patientFacade.findByJpql(jpql, params, javax.persistence.TemporalType.DATE, 20);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Deterministic Scan">
    public void scanByNic() {
        deterministicResults = runDeterministicScan("nic", "NIC");
    }

    public void scanByPhn() {
        deterministicResults = runDeterministicScan("phn", "PHN");
    }

    public void scanByMrn() {
        deterministicResults = runDeterministicScan("code", "MRN");
    }

    public void scanAll() {
        List<PatientPair> all = new ArrayList<>();
        all.addAll(runDeterministicScan("nic", "NIC"));
        all.addAll(runDeterministicScan("phn", "PHN"));
        all.addAll(runDeterministicScan("code", "MRN"));
        // De-duplicate by patient ID pair
        List<PatientPair> deduped = new ArrayList<>();
        for (PatientPair np : all) {
            boolean found = false;
            for (PatientPair ex : deduped) {
                if (ex.getPatientA().getId().equals(np.getPatientA().getId())
                        && ex.getPatientB().getId().equals(np.getPatientB().getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                deduped.add(np);
            }
        }
        deterministicResults = deduped;
    }

    private List<PatientPair> runDeterministicScan(String field, String matchReason) {
        int cap = Math.min(Math.max(detMaxResults, 1), 500);
        String jpql;
        if ("phn".equals(field) || "code".equals(field)) {
            jpql = "select p1, p2 from Patient p1, Patient p2 "
                    + "where p1.id < p2.id "
                    + "and p1.retired = false and p2.retired = false "
                    + "and p1." + field + " is not null and p1." + field + " <> '' "
                    + "and p1." + field + " = p2." + field;
        } else {
            jpql = "select p1, p2 from Patient p1, Patient p2 "
                    + "where p1.id < p2.id "
                    + "and p1.retired = false and p2.retired = false "
                    + "and p1.person." + field + " is not null and p1.person." + field + " <> '' "
                    + "and p1.person." + field + " = p2.person." + field;
        }
        List<Object[]> allRows = patientFacade.findObjectsArrayByJpql(jpql, new HashMap<>(), javax.persistence.TemporalType.DATE);
        List<PatientPair> pairs = new ArrayList<>();
        int limit = Math.min(allRows.size(), cap);
        List<Object[]> rows = allRows.subList(0, limit);
        for (Object[] row : rows) {
            PatientPair pair = new PatientPair((Patient) row[0], (Patient) row[1], matchReason);
            pairs.add(pair);
        }
        return pairs;
    }

    public void mergeDeterministicPair(PatientPair pair) {
        if (pair.getSelectedPrimary() == null) {
            JsfUtil.addErrorMessage("Please select the primary patient.");
            return;
        }
        Patient pri = pair.getSelectedPrimary();
        Patient sec = pri.getId().equals(pair.getPatientA().getId())
                ? pair.getPatientB() : pair.getPatientA();
        try {
            patientMergeService.merge(pri, sec, PatientMergeType.DETERMINISTIC,
                    "Deterministic scan — matched on " + pair.getMatchReason(),
                    sessionController.getLoggedUser());
            deterministicResults.remove(pair);
            JsfUtil.addSuccessMessage("Patients merged.");
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Merge failed: " + e.getMessage());
        }
    }

    public void dismissDeterministicPair(PatientPair pair) {
        deterministicResults.remove(pair);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="History & Unmerge">
    public void searchHistory() {
        String jpql = "select mr from PatientMergeRecord mr where mr.mergeDate >= :from and mr.mergeDate <= :to";
        Map<String, Object> params = new HashMap<>();
        params.put("from", historyFromDate != null ? historyFromDate : defaultFrom());
        params.put("to", historyToDate != null ? historyToDate : new Date());
        if (historyStatus != null) {
            jpql += " and mr.status = :status";
            params.put("status", historyStatus);
        }
        jpql += " order by mr.mergeDate desc";
        historyResults = patientMergeRecordFacade.findByJpql(jpql, params);
    }

    public void executeUnmerge() {
        if (selectedMergeRecord == null) {
            JsfUtil.addErrorMessage("No merge record selected.");
            return;
        }
        try {
            patientMergeService.unmerge(selectedMergeRecord, sessionController.getLoggedUser());
            JsfUtil.addSuccessMessage("Merge reversed successfully.");
            searchHistory();
        } catch (IllegalStateException e) {
            JsfUtil.addErrorMessage(e.getMessage());
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Unmerge failed: " + e.getMessage());
        }
    }

    private void initHistoryDates() {
        historyToDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        historyFromDate = cal.getTime();
    }

    private Date defaultFrom() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        return cal.getTime();
    }

    public int getAffectedRecordCount(PatientMergeRecord mr) {
        String jpql = "select count(ar) from PatientMergeAffectedRecord ar where ar.mergeRecord = :mr";
        Map<String, Object> p = new HashMap<>();
        p.put("mr", mr);
        return (int) patientMergeRecordFacade.findLongByJpql(jpql, p);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Inner class — PatientPair">
    public static class PatientPair implements Serializable {
        private Patient patientA;
        private Patient patientB;
        private String matchReason;
        private Patient selectedPrimary;

        public PatientPair(Patient a, Patient b, String matchReason) {
            this.patientA = a;
            this.patientB = b;
            this.matchReason = matchReason;
            this.selectedPrimary = a; // default: earlier-registered (lower id) is primary
        }

        public Patient getPatientA() { return patientA; }
        public Patient getPatientB() { return patientB; }
        public String getMatchReason() { return matchReason; }
        public Patient getSelectedPrimary() { return selectedPrimary; }
        public void setSelectedPrimary(Patient selectedPrimary) { this.selectedPrimary = selectedPrimary; }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public Patient getPrimaryPatient() { return primaryPatient; }
    public void setPrimaryPatient(Patient primaryPatient) { this.primaryPatient = primaryPatient; }

    public Patient getSecondaryPatient() { return secondaryPatient; }
    public void setSecondaryPatient(Patient secondaryPatient) { this.secondaryPatient = secondaryPatient; }

    public String getMergeReason() { return mergeReason; }
    public void setMergeReason(String mergeReason) { this.mergeReason = mergeReason; }

    public int getDetMaxResults() { return detMaxResults; }
    public void setDetMaxResults(int detMaxResults) { this.detMaxResults = detMaxResults; }

    public List<PatientPair> getDeterministicResults() { return deterministicResults; }

    public Date getHistoryFromDate() { return historyFromDate; }
    public void setHistoryFromDate(Date historyFromDate) { this.historyFromDate = historyFromDate; }

    public Date getHistoryToDate() { return historyToDate; }
    public void setHistoryToDate(Date historyToDate) { this.historyToDate = historyToDate; }

    public PatientMergeStatus getHistoryStatus() { return historyStatus; }
    public void setHistoryStatus(PatientMergeStatus historyStatus) { this.historyStatus = historyStatus; }

    public List<PatientMergeRecord> getHistoryResults() { return historyResults; }

    public PatientMergeRecord getSelectedMergeRecord() { return selectedMergeRecord; }
    public void setSelectedMergeRecord(PatientMergeRecord selectedMergeRecord) { this.selectedMergeRecord = selectedMergeRecord; }

    public PatientMergeStatus[] getMergeStatusValues() { return PatientMergeStatus.values(); }
    // </editor-fold>
}
