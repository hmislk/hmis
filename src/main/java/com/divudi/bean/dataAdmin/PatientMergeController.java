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
import org.apache.commons.text.similarity.JaroWinklerSimilarity;

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

    // <editor-fold defaultstate="collapsed" desc="Probabilistic scan state">
    private int probMaxPairs = 100;
    private double probNameThreshold = 0.92;
    private List<ScoredPatientPair> probabilisticResults = new ArrayList<>();
    private static final JaroWinklerSimilarity JARO_WINKLER = new JaroWinklerSimilarity();
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="History state">
    private Date historyFromDate;
    private Date historyToDate;
    private PatientMergeStatus historyStatus;
    private String historyMergedByUsername;
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
        List<Object[]> rows = patientFacade.findPatientPairsByJpql(jpql, new HashMap<>(), cap);
        List<PatientPair> pairs = new ArrayList<>();
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

    // <editor-fold defaultstate="collapsed" desc="Probabilistic Scan">
    public void runProbabilisticScan() {
        int cap = Math.min(Math.max(probMaxPairs, 1), 1000);
        // Blocking: same birth year + same first letter of name
        String jpql = "select p1, p2 from Patient p1, Patient p2 "
                + "where p1.id < p2.id "
                + "and p1.retired = false and p2.retired = false "
                + "and p1.person.dob is not null and p2.person.dob is not null "
                + "and year(p1.person.dob) = year(p2.person.dob) "
                + "and p1.person.name is not null and p2.person.name is not null "
                + "and substring(upper(p1.person.name),1,1) = substring(upper(p2.person.name),1,1)";
        List<Object[]> rows = patientFacade.findObjectsArrayByJpql(jpql, new HashMap<>(), javax.persistence.TemporalType.DATE);

        // Collect already-merged pair IDs to exclude
        java.util.Set<String> alreadyMerged = loadAlreadyMergedPairKeys();

        List<ScoredPatientPair> results = new ArrayList<>();
        int checked = 0;
        for (Object[] row : rows) {
            if (checked >= cap) break;
            Patient a = (Patient) row[0];
            Patient b = (Patient) row[1];
            String key = Math.min(a.getId(), b.getId()) + "_" + Math.max(a.getId(), b.getId());
            if (alreadyMerged.contains(key)) continue;
            checked++;
            ScoredPatientPair scored = score(a, b);
            if (scored != null) {
                results.add(scored);
            }
        }
        results.sort((x, y) -> Double.compare(y.getCompositeScore(), x.getCompositeScore()));
        probabilisticResults = results;
    }

    private java.util.Set<String> loadAlreadyMergedPairKeys() {
        String jpql = "select mr.primaryPatient.id, mr.secondaryPatient.id from PatientMergeRecord mr";
        List<Object[]> rows = patientMergeRecordFacade.findObjectsArrayByJpql(jpql, new HashMap<>(), javax.persistence.TemporalType.DATE);
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (Object[] row : rows) {
            Long idA = (Long) row[0];
            Long idB = (Long) row[1];
            keys.add(Math.min(idA, idB) + "_" + Math.max(idA, idB));
        }
        return keys;
    }

    private ScoredPatientPair score(Patient a, Patient b) {
        String nameA = a.getPerson() != null && a.getPerson().getName() != null ? a.getPerson().getName().toLowerCase() : "";
        String nameB = b.getPerson() != null && b.getPerson().getName() != null ? b.getPerson().getName().toLowerCase() : "";
        double nameSim = nameA.isEmpty() || nameB.isEmpty() ? 0.0 : JARO_WINKLER.apply(nameA, nameB);
        if (nameSim < probNameThreshold) return null;

        double dobScore = scoreDob(a, b);
        double phoneScore = scorePhone(a, b);
        double composite = nameSim * 0.40 + dobScore * 0.35 + phoneScore * 0.25;
        if (composite < 0.80) return null;

        return new ScoredPatientPair(a, b, nameSim, dobScore, phoneScore, composite);
    }

    private double scoreDob(Patient a, Patient b) {
        if (a.getPerson() == null || b.getPerson() == null) return 0.0;
        Date da = a.getPerson().getDob();
        Date db = b.getPerson().getDob();
        if (da == null || db == null) return 0.0;
        long diffDays = Math.abs(da.getTime() - db.getTime()) / (1000L * 60 * 60 * 24);
        if (diffDays == 0) return 1.0;
        if (diffDays <= 1) return 0.8;
        if (diffDays <= 3) return 0.5;
        return 0.0;
    }

    private double scorePhone(Patient a, Patient b) {
        String pA = bestPhone(a);
        String pB = bestPhone(b);
        if (pA.isEmpty() || pB.isEmpty()) return 0.0;
        int len = Math.min(pA.length(), 9);
        String tailA = pA.substring(Math.max(0, pA.length() - len));
        String tailB = pB.substring(Math.max(0, pB.length() - len));
        return tailA.equals(tailB) ? 1.0 : 0.0;
    }

    private String bestPhone(Patient p) {
        if (p.getPerson() == null) return "";
        String phone = p.getPerson().getPhone() != null ? p.getPerson().getPhone().replaceAll("[^0-9]", "") : "";
        String mobile = p.getPerson().getMobile() != null ? p.getPerson().getMobile().replaceAll("[^0-9]", "") : "";
        return phone.length() >= mobile.length() ? phone : mobile;
    }

    public void mergeProbabilisticPair(ScoredPatientPair pair) {
        if (pair.getSelectedPrimary() == null) {
            JsfUtil.addErrorMessage("Please select the primary patient.");
            return;
        }
        Patient pri = pair.getSelectedPrimary();
        Patient sec = pri.getId().equals(pair.getPatientA().getId())
                ? pair.getPatientB() : pair.getPatientA();
        try {
            patientMergeService.merge(pri, sec, PatientMergeType.PROBABILISTIC,
                    String.format("Probabilistic scan — score %.0f%% (name %.0f%%, DOB %.0f%%, phone %.0f%%)",
                            pair.getCompositeScore() * 100,
                            pair.getNameScore() * 100,
                            pair.getDobScore() * 100,
                            pair.getPhoneScore() * 100),
                    sessionController.getLoggedUser());
            probabilisticResults.remove(pair);
            JsfUtil.addSuccessMessage("Patients merged.");
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Merge failed: " + e.getMessage());
        }
    }

    public void dismissProbabilisticPair(ScoredPatientPair pair) {
        probabilisticResults.remove(pair);
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
        if (historyMergedByUsername != null && !historyMergedByUsername.trim().isEmpty()) {
            jpql += " and lower(mr.mergedBy.username) like :mergedBy";
            params.put("mergedBy", "%" + historyMergedByUsername.trim().toLowerCase() + "%");
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

    // <editor-fold defaultstate="collapsed" desc="Inner class — ScoredPatientPair">
    public static class ScoredPatientPair implements Serializable {
        private Patient patientA;
        private Patient patientB;
        private double nameScore;
        private double dobScore;
        private double phoneScore;
        private double compositeScore;
        private Patient selectedPrimary;

        public ScoredPatientPair(Patient a, Patient b, double nameScore, double dobScore,
                double phoneScore, double compositeScore) {
            this.patientA = a;
            this.patientB = b;
            this.nameScore = nameScore;
            this.dobScore = dobScore;
            this.phoneScore = phoneScore;
            this.compositeScore = compositeScore;
            this.selectedPrimary = a;
        }

        public Patient getPatientA() { return patientA; }
        public Patient getPatientB() { return patientB; }
        public double getNameScore() { return nameScore; }
        public double getDobScore() { return dobScore; }
        public double getPhoneScore() { return phoneScore; }
        public double getCompositeScore() { return compositeScore; }
        public Patient getSelectedPrimary() { return selectedPrimary; }
        public void setSelectedPrimary(Patient selectedPrimary) { this.selectedPrimary = selectedPrimary; }
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

    public String getHistoryMergedByUsername() { return historyMergedByUsername; }
    public void setHistoryMergedByUsername(String historyMergedByUsername) { this.historyMergedByUsername = historyMergedByUsername; }

    public List<PatientMergeRecord> getHistoryResults() { return historyResults; }

    public PatientMergeRecord getSelectedMergeRecord() { return selectedMergeRecord; }
    public void setSelectedMergeRecord(PatientMergeRecord selectedMergeRecord) { this.selectedMergeRecord = selectedMergeRecord; }

    public PatientMergeStatus[] getMergeStatusValues() { return PatientMergeStatus.values(); }

    public int getProbMaxPairs() { return probMaxPairs; }
    public void setProbMaxPairs(int probMaxPairs) { this.probMaxPairs = probMaxPairs; }

    public double getProbNameThreshold() { return probNameThreshold; }
    public void setProbNameThreshold(double probNameThreshold) { this.probNameThreshold = probNameThreshold; }

    public List<ScoredPatientPair> getProbabilisticResults() { return probabilisticResults; }
    // </editor-fold>
}
