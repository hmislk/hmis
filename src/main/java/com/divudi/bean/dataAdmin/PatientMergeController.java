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
import java.util.LinkedHashMap;
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

    /**
     * Uses a correlated EXISTS subquery to find patients whose identifier
     * appears on at least one other non-retired record, then pairs them in
     * Java. This avoids an O(n²) Cartesian cross-join that times out on large
     * patient databases.
     */
    private List<PatientPair> runDeterministicScan(String field, String matchReason) {
        int cap = Math.min(Math.max(detMaxResults, 1), 500);
        String jpql;
        if ("phn".equals(field)) {
            jpql = "select p from Patient p "
                    + "where p.retired = false and p.phn is not null and p.phn <> '' "
                    + "and exists (select p2 from Patient p2 where p2.retired = false "
                    + "  and p2.phn = p.phn and p2.id <> p.id) "
                    + "order by p.phn, p.id";
        } else if ("code".equals(field)) {
            jpql = "select p from Patient p "
                    + "where p.retired = false and p.code is not null and p.code <> '' "
                    + "and exists (select p2 from Patient p2 where p2.retired = false "
                    + "  and p2.code = p.code and p2.id <> p.id) "
                    + "order by p.code, p.id";
        } else {
            jpql = "select p from Patient p "
                    + "where p.retired = false and p.person.nic is not null and p.person.nic <> '' "
                    + "and exists (select p2 from Patient p2 where p2.retired = false "
                    + "  and p2.person.nic = p.person.nic and p2.id <> p.id) "
                    + "order by p.person.nic, p.id";
        }
        // Fetch more patients than cap — one NIC value may appear many times
        List<Patient> patients = patientFacade.findByJpql(jpql, new HashMap<>(),
                javax.persistence.TemporalType.DATE, cap * 10);

        // Group by the shared field value, then generate ordered pairs
        LinkedHashMap<String, List<Patient>> groups = new LinkedHashMap<>();
        for (Patient p : patients) {
            String key = deterministicFieldValue(p, field);
            if (key != null) {
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            }
        }
        List<PatientPair> pairs = new ArrayList<>();
        outer:
        for (List<Patient> group : groups.values()) {
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    pairs.add(new PatientPair(group.get(i), group.get(j), matchReason));
                    if (pairs.size() >= cap) {
                        break outer;
                    }
                }
            }
        }
        return pairs;
    }

    private String deterministicFieldValue(Patient p, String field) {
        if ("phn".equals(field)) {
            return p.getPhn();
        }
        if ("code".equals(field)) {
            return p.getCode();
        }
        return p.getPerson() != null ? p.getPerson().getNic() : null;
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

    /**
     * Probabilistic duplicate detection using in-memory blocking.
     *
     * Instead of a Cartesian cross-join (O(n²) in SQL, unusable at 290 K+
     * patients), this method:
     * 1. Fetches lightweight (id, dob, name, phone, mobile) tuples via a
     *    simple SELECT — no cross-join, bounded by a hard row cap.
     * 2. Builds blocking groups in Java: patients sharing the same birth-year +
     *    name-first-letter, or the same 7-digit phone tail.
     * 3. Scores each candidate pair entirely in memory using Jaro-Winkler name
     *    similarity, DOB proximity, and phone tail matching.
     * 4. Loads full Patient entities only for the top-cap results, so database
     *    round-trips are proportional to the result set, not the patient count.
     */
    public void runProbabilisticScan() {
        int cap = Math.min(Math.max(probMaxPairs, 1), 1000);
        java.util.Set<String> alreadyMerged = loadAlreadyMergedPairKeys();

        // Step 1: fetch lightweight tuples — no cross-join, capped at 100 K rows
        String jpql = "select p.id, p.person.dob, p.person.name, p.person.phone, p.person.mobile "
                + "from Patient p where p.retired = false "
                + "and (p.person.dob is not null "
                + "  or p.person.phone is not null "
                + "  or p.person.mobile is not null)";
        List<Object[]> rows = patientFacade.findObjectsArrayByJpql(jpql, new HashMap<>(),
                javax.persistence.TemporalType.DATE, 100000);

        // Step 2: build blocking groups in Java
        Map<String, List<Object[]>> groups = new HashMap<>();
        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            Date dob = (Date) row[1];
            String name = row[2] != null ? row[2].toString() : null;
            String phone = cleanPhone(row[3]);
            String mobile = cleanPhone(row[4]);

            // DOB-year + name-first-letter block
            if (dob != null && name != null && !name.isEmpty()) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(dob);
                String key = "D" + cal.get(Calendar.YEAR) + "_" + Character.toUpperCase(name.charAt(0));
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
            }
            // Phone-tail blocks
            for (String num : new String[]{phone, mobile}) {
                if (num.length() >= 7) {
                    String key = "P" + num.substring(num.length() - 7);
                    groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
                }
            }
        }

        // Step 3: score candidate pairs in memory
        java.util.Set<String> seenPairs = new java.util.HashSet<>();
        // Each entry: [idA (Long), idB (Long), nameSim, dobScore, phoneScore, composite]
        List<Object[]> scoredResults = new ArrayList<>();

        for (List<Object[]> group : groups.values()) {
            // Skip degenerate groups: too small or far too large to contain real duplicates
            if (group.size() < 2 || group.size() > 100) {
                continue;
            }
            for (int i = 0; i < group.size(); i++) {
                for (int j = i + 1; j < group.size(); j++) {
                    long rawA = ((Number) group.get(i)[0]).longValue();
                    long rawB = ((Number) group.get(j)[0]).longValue();
                    if (rawA == rawB) {
                        continue;
                    }
                    long idA = Math.min(rawA, rawB);
                    long idB = Math.max(rawA, rawB);
                    String pairKey = idA + "_" + idB;
                    if (seenPairs.contains(pairKey) || alreadyMerged.contains(pairKey)) {
                        continue;
                    }
                    seenPairs.add(pairKey);

                    double[] scores = scoreTuples(group.get(i), group.get(j));
                    if (scores != null) {
                        scoredResults.add(new Object[]{idA, idB, scores[0], scores[1], scores[2], scores[3]});
                    }
                }
            }
        }

        // Step 4: sort and cap, then load full Patient entities for display
        scoredResults.sort((x, y) -> Double.compare((double) y[5], (double) x[5]));
        if (scoredResults.size() > cap) {
            scoredResults = scoredResults.subList(0, cap);
        }

        List<ScoredPatientPair> results = new ArrayList<>();
        for (Object[] s : scoredResults) {
            Patient a = patientFacade.find((Long) s[0]);
            Patient b = patientFacade.find((Long) s[1]);
            if (a != null && b != null) {
                results.add(new ScoredPatientPair(a, b, (double) s[2], (double) s[3], (double) s[4], (double) s[5]));
            }
        }
        probabilisticResults = results;
    }

    private String cleanPhone(Object raw) {
        return raw != null ? raw.toString().replaceAll("[^0-9]", "") : "";
    }

    /**
     * Scores a pair of lightweight patient tuples entirely in memory.
     * Returns null if the pair does not meet the minimum thresholds.
     * Each tuple: [id, dob, name, phone, mobile]
     */
    private double[] scoreTuples(Object[] rowA, Object[] rowB) {
        String nameA = rowA[2] != null ? rowA[2].toString().toLowerCase() : "";
        String nameB = rowB[2] != null ? rowB[2].toString().toLowerCase() : "";
        double nameSim = nameA.isEmpty() || nameB.isEmpty() ? 0.0 : JARO_WINKLER.apply(nameA, nameB);
        if (nameSim < probNameThreshold) {
            return null;
        }

        double dobScore = scoreDobDates((Date) rowA[1], (Date) rowB[1]);

        String[] phonesA = {cleanPhone(rowA[3]), cleanPhone(rowA[4])};
        String[] phonesB = {cleanPhone(rowB[3]), cleanPhone(rowB[4])};
        double phoneScore = scorePhoneArrays(phonesA, phonesB);

        double composite = nameSim * 0.40 + dobScore * 0.35 + phoneScore * 0.25;
        if (composite < 0.70) {
            return null;
        }
        return new double[]{nameSim, dobScore, phoneScore, composite};
    }

    private double scoreDobDates(Date da, Date db) {
        if (da == null || db == null) {
            return 0.0;
        }
        long diffDays = Math.abs(da.getTime() - db.getTime()) / (1000L * 60 * 60 * 24);
        if (diffDays == 0) {
            return 1.0;
        }
        if (diffDays <= 1) {
            return 0.8;
        }
        if (diffDays <= 3) {
            return 0.5;
        }
        return 0.0;
    }

    private double scorePhoneArrays(String[] numsA, String[] numsB) {
        for (String pA : numsA) {
            if (pA.isEmpty()) {
                continue;
            }
            for (String pB : numsB) {
                if (pB.isEmpty()) {
                    continue;
                }
                int len = Math.min(Math.min(pA.length(), pB.length()), 9);
                if (len < 7) {
                    continue;
                }
                if (pA.substring(pA.length() - len).equals(pB.substring(pB.length() - len))) {
                    return 1.0;
                }
            }
        }
        return 0.0;
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
