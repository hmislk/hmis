package com.divudi.service;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.entity.lab.PatientSampleComponant;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.facade.PatientReportFacade;
import com.divudi.core.facade.PatientSampleFacade;
import com.divudi.core.facade.PatientSampleComponantFacade;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Once a lab sample of an investigation on a bill is collected, the bill can
 * be neither cancelled nor refunded/returned. No privilege overrides this; the
 * only switch is the application configuration {@link #CONFIG_KEY}.
 */
@Stateless
public class LabSampleLockService {

    public static final String CONFIG_KEY = "Block cancellation and refund of OPD, package and collecting centre bills once a lab sample is collected";

    @EJB
    private BillService billService;
    @EJB
    private PatientSampleComponantFacade patientSampleComponantFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    private PatientReportFacade patientReportFacade;
    @EJB
    private PatientSampleFacade patientSampleFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private AuditService auditService;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    public boolean isLockEnabled() {
        return configOptionApplicationController.getBooleanValueByKey(CONFIG_KEY, true);
    }

    public boolean isCollected(PatientInvestigation pi) {
        if (pi == null) {
            return false;
        }
        boolean flagged = Boolean.TRUE.equals(pi.getSampleCollected()) || Boolean.TRUE.equals(pi.getCollected());
        if (!flagged) {
            return false;
        }
        List<PatientSample> samples = fetchActiveSamples(pi);
        if (samples == null || samples.isEmpty()) {
            return true;
        }
        for (PatientSample ps : samples) {
            if (!isRejected(ps)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRejected(PatientSample ps) {
        return Boolean.TRUE.equals(ps.getSampleRejected())
                || ps.getStatus() == PatientInvestigationStatus.SAMPLE_REJECTED;
    }

    /**
     * Non-retired, non-cancelled samples linked to the investigation.
     */
    List<PatientSample> fetchActiveSamples(PatientInvestigation pi) {
        String jpql = "select c "
                + " from PatientSampleComponant c "
                + " where c.retired=false "
                + " and c.patientInvestigation=:pi "
                + " and c.patientSample is not null ";
        Map<String, Object> params = new HashMap<>();
        params.put("pi", pi);
        List<PatientSampleComponant> components = patientSampleComponantFacade.findByJpql(jpql, params);
        List<PatientSample> result = new ArrayList<>();
        if (components == null) {
            return result;
        }
        for (PatientSampleComponant c : components) {
            PatientSample ps = c.getPatientSample();
            if (ps != null && !ps.isRetired() && !Boolean.TRUE.equals(ps.getCancelled()) && !result.contains(ps)) {
                result.add(ps);
            }
        }
        return result;
    }

    public List<PatientInvestigation> findCollectedInvestigations(Bill bill) {
        List<PatientInvestigation> collected = new ArrayList<>();
        if (bill == null) {
            return collected;
        }
        Map<Object, PatientInvestigation> candidates = new LinkedHashMap<>();
        addCandidates(candidates, fetchPatientInvestigationsOfBill(bill));
        addCandidates(candidates, fetchPatientInvestigationsOfBatchBill(bill));
        for (PatientInvestigation pi : candidates.values()) {
            if (isCandidate(pi) && isCollected(pi)) {
                collected.add(pi);
            }
        }
        return collected;
    }

    public List<PatientInvestigation> findCollectedInvestigations(List<BillItem> items) {
        List<PatientInvestigation> collected = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return collected;
        }
        List<PatientInvestigation> found = fetchPatientInvestigationsOfItems(items);
        if (found == null) {
            return collected;
        }
        for (PatientInvestigation pi : found) {
            if (isCandidate(pi) && isCollected(pi)) {
                collected.add(pi);
            }
        }
        return collected;
    }

    private void addCandidates(Map<Object, PatientInvestigation> map, List<PatientInvestigation> list) {
        if (list == null) {
            return;
        }
        for (PatientInvestigation pi : list) {
            Object key = pi.getId() != null ? pi.getId() : pi;
            map.put(key, pi);
        }
    }

    private boolean isCandidate(PatientInvestigation pi) {
        if (pi == null || pi.isRetired() || Boolean.TRUE.equals(pi.getCancelled())) {
            return false;
        }
        BillItem bi = pi.getBillItem();
        return bi == null || !(bi.isRefunded() || bi.isBillItemRefunded());
    }

    List<PatientInvestigation> fetchPatientInvestigationsOfBill(Bill bill) {
        return billService.fetchPatientInvestigations(bill);
    }

    List<PatientInvestigation> fetchPatientInvestigationsOfBatchBill(Bill bill) {
        return billService.fetchPatientInvestigationsOfBatchBill(bill);
    }

    List<PatientInvestigation> fetchPatientInvestigationsOfItems(List<BillItem> items) {
        String jpql = "select pi from PatientInvestigation pi "
                + " where pi.retired=false "
                + " and pi.billItem in :items ";
        Map<String, Object> params = new HashMap<>();
        params.put("items", items);
        return patientInvestigationFacade.findByJpql(jpql, params);
    }

    public String lockMessage(List<PatientInvestigation> collected, String action) {
        if (collected == null || collected.isEmpty()) {
            return null;
        }
        String act = (action == null || action.trim().isEmpty()) ? "cancel" : action.trim();
        StringBuilder names = new StringBuilder();
        int shown = 0;
        for (PatientInvestigation pi : collected) {
            if (shown >= 3) {
                break;
            }
            if (shown > 0) {
                names.append(", ");
            }
            String name = pi.getInvestigation() != null ? pi.getInvestigation().getName() : null;
            names.append(name == null ? "an investigation" : name);
            if (pi.getSampleCollectedAt() != null) {
                names.append(" (collected at ")
                        .append(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(pi.getSampleCollectedAt()))
                        .append(")");
            }
            shown++;
        }
        if (collected.size() > shown) {
            names.append(" and ").append(collected.size() - shown).append(" more");
        }
        return "Cannot " + act + " this bill because the laboratory has already collected the sample for " + names
                + ". Ask a laboratory user with the 'Lab Revert Sample' privilege to cancel the sample collection first (Lab > Sample Management).";
    }

    /**
     * @return null when cancellation is allowed (or the lock is disabled),
     * otherwise the message to show.
     */
    public String checkCancelBlocked(Bill bill) {
        if (bill == null || !isLockEnabled()) {
            return null;
        }
        return lockMessage(findCollectedInvestigations(bill), "cancel");
    }

    public String checkRefundBlocked(Bill bill) {
        if (bill == null || !isLockEnabled()) {
            return null;
        }
        return lockMessage(findCollectedInvestigations(bill), "refund");
    }

    public String checkReturnBlocked(List<BillItem> items) {
        return checkReturnBlocked(items, "return");
    }

    public String checkReturnBlocked(List<BillItem> items, String action) {
        if (items == null || items.isEmpty() || !isLockEnabled()) {
            return null;
        }
        return lockMessage(findCollectedInvestigations(items), action);
    }

    /**
     * True when the investigation has at least one active sample and every
     * active sample is rejected.
     */
    public boolean allSamplesRejected(PatientInvestigation pi) {
        if (pi == null) {
            return false;
        }
        List<PatientSample> samples = fetchActiveSamples(pi);
        if (samples == null || samples.isEmpty()) {
            return false;
        }
        for (PatientSample ps : samples) {
            if (!isRejected(ps)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Non-retired investigations linked to the sample through non-retired
     * sample components.
     */
    List<PatientInvestigation> fetchInvestigationsOfSample(PatientSample sample) {
        String jpql = "select distinct c.patientInvestigation "
                + " from PatientSampleComponant c "
                + " where c.retired=false "
                + " and c.patientSample=:ps "
                + " and c.patientInvestigation is not null ";
        Map<String, Object> params = new HashMap<>();
        params.put("ps", sample);
        List<PatientInvestigation> list = patientInvestigationFacade.findByJpql(jpql, params);
        List<PatientInvestigation> result = new ArrayList<>();
        if (list != null) {
            for (PatientInvestigation pi : list) {
                if (pi != null && !pi.isRetired() && !result.contains(pi)) {
                    result.add(pi);
                }
            }
        }
        return result;
    }

    /**
     * Non-retired reports of the investigation.
     */
    List<PatientReport> fetchReportsOfInvestigation(PatientInvestigation pi) {
        String jpql = "select r from PatientReport r "
                + " where r.retired=false "
                + " and r.patientInvestigation=:pi ";
        Map<String, Object> params = new HashMap<>();
        params.put("pi", pi);
        return patientReportFacade.findByJpql(jpql, params);
    }

    private String investigationName(PatientInvestigation pi) {
        String name = pi.getInvestigation() != null ? pi.getInvestigation().getName() : null;
        return name == null ? "an investigation" : name;
    }

    /**
     * Same checks as cancelSampleCollection but without a reason and without
     * any write; used to enable or disable the UI control.
     *
     * @return null when the sample collection can be cancelled, otherwise the
     * message to show.
     */
    public String checkCanCancelSampleCollection(PatientSample sample) {
        if (sample == null || sample.isRetired() || Boolean.TRUE.equals(sample.getCancelled())) {
            return "This sample is not available (retired or cancelled).";
        }
        if (!Boolean.TRUE.equals(sample.getSampleCollected())) {
            return "The sample collection cannot be cancelled: the sample is not collected.";
        }
        return checkReportsNotIssued(sample);
    }

    private String checkReportsNotIssued(PatientSample sample) {
        List<PatientInvestigation> investigations = fetchInvestigationsOfSample(sample);
        if (investigations == null) {
            return null;
        }
        for (PatientInvestigation pi : investigations) {
            boolean issued = Boolean.TRUE.equals(pi.getApproved()) || Boolean.TRUE.equals(pi.getPrinted());
            if (!issued) {
                List<PatientReport> reports = fetchReportsOfInvestigation(pi);
                if (reports != null) {
                    for (PatientReport r : reports) {
                        if (r != null && !r.isRetired()
                                && (Boolean.TRUE.equals(r.getApproved()) || Boolean.TRUE.equals(r.getPrinted()))) {
                            issued = true;
                            break;
                        }
                    }
                }
            }
            if (issued) {
                return "Cannot cancel the sample collection: the report for " + investigationName(pi)
                        + " has already been approved/printed.";
            }
        }
        return null;
    }

    public boolean canCancelSampleCollection(PatientSample sample) {
        return checkCanCancelSampleCollection(sample) == null;
    }

    /**
     * Reverts a collected sample to the "sample generated" state so the bill
     * can then be cancelled or refunded.
     *
     * @return null on success, otherwise the error message.
     */
    public String cancelSampleCollection(PatientSample sample, WebUser user, String reason) {
        String error = checkCanCancelSampleCollection(sample);
        if (error != null) {
            return error;
        }
        if (reason == null || reason.trim().isEmpty()) {
            return "Please enter a reason for cancelling the sample collection.";
        }
        String trimmedReason = reason.trim();
        Date now = new Date();
        List<PatientInvestigation> investigations = fetchInvestigationsOfSample(sample);
        if (investigations == null) {
            investigations = new ArrayList<>();
        }

        for (PatientInvestigation pi : investigations) {
            List<PatientReport> reports = fetchReportsOfInvestigation(pi);
            if (reports == null) {
                continue;
            }
            for (PatientReport r : reports) {
                if (r == null || r.isRetired()) {
                    continue;
                }
                r.setRetired(true);
                r.setRetirer(user);
                r.setRetiredAt(now);
                r.setRetireComments("Sample collection cancelled: " + trimmedReason);
                patientReportFacade.edit(r);
            }
        }

        sample.setSampleCollected(false);
        sample.setSampleCollecter(null);
        sample.setSampleCollectedAt(null);
        sample.setSampleSent(false);
        sample.setSampleSentBy(null);
        sample.setSampleSentAt(null);
        sample.setSampleReceivedAtLab(false);
        sample.setSampleReceiverAtLab(null);
        sample.setSampleReceivedAtLabAt(null);
        sample.setReadyTosentToAnalyzer(false);
        sample.setSentToAnalyzer(false);
        sample.setSentToAnalyzerBy(null);
        sample.setSentToAnalyzerAt(null);
        sample.setReceivedFromAnalyzer(false);
        sample.setReceivedFromAnalyzerBy(null);
        sample.setReceivedFromAnalyzerAt(null);
        sample.setStatus(PatientInvestigationStatus.SAMPLE_GENERATED);
        patientSampleFacade.edit(sample);

        Map<Object, Bill> bills = new LinkedHashMap<>();
        for (PatientInvestigation pi : investigations) {
            if (hasOtherCollectedSample(pi, sample)) {
                // Another sample of this investigation is still collected, so the
                // investigation itself stays collected.
                continue;
            }
            pi.setSampleCollected(false);
            pi.setCollected(false);
            pi.setReceived(false);
            pi.setDataEntered(false);
            pi.setSampleCollectedAt(null);
            pi.setSampleCollectedBy(null);
            pi.setStatus(PatientInvestigationStatus.SAMPLE_GENERATED);
            pi.setSampleReverted(true);
            pi.setSampleRevertedBy(user);
            pi.setSampleRevertedAt(now);
            patientInvestigationFacade.edit(pi);
            BillItem bi = pi.getBillItem();
            Bill b = bi != null ? bi.getBill() : null;
            if (b != null) {
                bills.put(b.getId() != null ? b.getId() : b, b);
            }
        }
        for (Bill b : bills.values()) {
            if (!findCollectedInvestigations(b).isEmpty()) {
                // Another investigation on this bill is still collected.
                continue;
            }
            b.setStatus(PatientInvestigationStatus.SAMPLE_GENERATED);
            billFacade.edit(b);
        }

        if (user != null) {
            Map<String, Object> after = new LinkedHashMap<>();
            after.put("sampleId", sample.getId());
            after.put("reason", trimmedReason);
            after.put("investigations", investigations.size());
            auditService.logAudit(null, after, user, "PatientSample", "Cancel Sample Collection", sample.getId());
        }
        return null;
    }

    boolean hasOtherCollectedSample(PatientInvestigation pi, PatientSample cancelled) {
        List<PatientSample> samples = fetchActiveSamples(pi);
        if (samples == null) {
            return false;
        }
        for (PatientSample ps : samples) {
            if (ps.equals(cancelled)) {
                continue;
            }
            if (Boolean.TRUE.equals(ps.getSampleCollected()) && !isRejected(ps)) {
                return true;
            }
        }
        return false;
    }
}
