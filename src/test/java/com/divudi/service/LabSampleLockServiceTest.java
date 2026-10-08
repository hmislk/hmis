package com.divudi.service;

import com.divudi.core.data.lab.PatientInvestigationStatus;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.entity.lab.PatientSample;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LabSampleLockServiceTest {

    private static class StubService extends LabSampleLockService {

        List<PatientSample> samples = new ArrayList<>();
        List<PatientInvestigation> sampleInvestigations = new ArrayList<>();
        List<PatientReport> reports = new ArrayList<>();

        @Override
        List<PatientInvestigation> fetchInvestigationsOfSample(PatientSample sample) {
            return sampleInvestigations;
        }

        @Override
        List<PatientReport> fetchReportsOfInvestigation(PatientInvestigation pi) {
            return reports;
        }

        @Override
        List<PatientSample> fetchActiveSamples(PatientInvestigation pi) {
            return samples;
        }
    }

    private PatientInvestigation pi(boolean collected) {
        PatientInvestigation pi = new PatientInvestigation();
        pi.setSampleCollected(collected);
        return pi;
    }

    private PatientSample sample(boolean rejected) {
        PatientSample ps = new PatientSample();
        ps.setSampleRejected(rejected);
        return ps;
    }

    @Test
    public void notCollectedIsNotLocked() {
        StubService s = new StubService();
        assertFalse(s.isCollected(pi(false)));
        assertFalse(s.isCollected(null));
    }

    @Test
    public void collectedWithoutSampleRowsIsLocked() {
        StubService s = new StubService();
        assertTrue(s.isCollected(pi(true)));
    }

    @Test
    public void legacyCollectedFlagIsLocked() {
        StubService s = new StubService();
        PatientInvestigation p = pi(false);
        p.setCollected(true);
        assertTrue(s.isCollected(p));
    }

    @Test
    public void collectedButAllSamplesRejectedIsNotLocked() {
        StubService s = new StubService();
        s.samples = Arrays.asList(sample(true), sample(true));
        assertFalse(s.isCollected(pi(true)));
    }

    @Test
    public void rejectedByStatusIsNotLocked() {
        StubService s = new StubService();
        PatientSample ps = sample(false);
        ps.setStatus(PatientInvestigationStatus.SAMPLE_REJECTED);
        s.samples = Collections.singletonList(ps);
        assertFalse(s.isCollected(pi(true)));
    }

    @Test
    public void oneRejectedOneActiveIsLocked() {
        StubService s = new StubService();
        s.samples = Arrays.asList(sample(true), sample(false));
        assertTrue(s.isCollected(pi(true)));
    }

    @Test
    public void lockMessageFormatting() throws Exception {
        StubService s = new StubService();
        Investigation inv = new Investigation();
        inv.setName("FBC");
        PatientInvestigation p = pi(true);
        p.setInvestigation(inv);
        Date at = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse("2026-10-08 09:30");
        p.setSampleCollectedAt(at);

        String msg = s.lockMessage(Collections.singletonList(p), "cancel");
        assertTrue(msg.startsWith("Cannot cancel this bill because the laboratory has already collected the sample for FBC"));
        assertTrue(msg.contains("2026-10-08 09:30"));
        assertTrue(msg.contains("'Lab Revert Sample' privilege"));

        assertTrue(s.lockMessage(Collections.singletonList(p), "refund").startsWith("Cannot refund this bill"));
        assertNull(s.lockMessage(new ArrayList<>(), "cancel"));
    }

    @Test
    public void lockMessageListsAtMostThreeNamesAndIsNullSafe() {
        StubService s = new StubService();
        List<PatientInvestigation> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            PatientInvestigation p = pi(true);
            Investigation inv = new Investigation();
            inv.setName("T" + i);
            p.setInvestigation(inv);
            list.add(p);
        }
        String msg = s.lockMessage(list, "return");
        assertTrue(msg.contains("T1"));
        assertTrue(msg.contains("T3"));
        assertFalse(msg.contains("T4"));
        assertTrue(msg.contains("and 2 more"));

        String nullSafe = s.lockMessage(Collections.singletonList(pi(true)), null);
        assertTrue(nullSafe.startsWith("Cannot cancel this bill because the laboratory has already collected the sample for an investigation"));
    }

    private PatientSample collectedSample() {
        PatientSample ps = new PatientSample();
        ps.setSampleCollected(true);
        return ps;
    }

    @Test
    public void cancelCollectionNotCollectedIsRejected() {
        StubService s = new StubService();
        assertNotNull(s.checkCanCancelSampleCollection(new PatientSample()));
        assertNotNull(s.checkCanCancelSampleCollection(null));
        assertFalse(s.canCancelSampleCollection(new PatientSample()));
    }

    @Test
    public void cancelCollectionCancelledOrRetiredSampleIsRejected() {
        StubService s = new StubService();
        PatientSample cancelled = collectedSample();
        cancelled.setCancelled(true);
        assertNotNull(s.checkCanCancelSampleCollection(cancelled));
        PatientSample retired = collectedSample();
        retired.setRetired(true);
        assertNotNull(s.checkCanCancelSampleCollection(retired));
    }

    @Test
    public void cancelCollectionBlankReasonIsRejected() {
        StubService s = new StubService();
        assertEquals("Please enter a reason for cancelling the sample collection.",
                s.cancelSampleCollection(collectedSample(), null, "  "));
        assertEquals("Please enter a reason for cancelling the sample collection.",
                s.cancelSampleCollection(collectedSample(), null, null));
    }

    @Test
    public void cancelCollectionBlockedByApprovedInvestigation() {
        StubService s = new StubService();
        Investigation inv = new Investigation();
        inv.setName("FBC");
        PatientInvestigation p = pi(true);
        p.setInvestigation(inv);
        p.setApproved(true);
        s.sampleInvestigations.add(p);
        String msg = s.checkCanCancelSampleCollection(collectedSample());
        assertEquals("Cannot cancel the sample collection: the report for FBC has already been approved/printed.", msg);
    }

    @Test
    public void cancelCollectionBlockedByPrintedReport() {
        StubService s = new StubService();
        s.sampleInvestigations.add(pi(true));
        PatientReport r = new PatientReport();
        r.setPrinted(true);
        s.reports.add(r);
        String msg = s.checkCanCancelSampleCollection(collectedSample());
        assertNotNull(msg);
        assertTrue(msg.startsWith("Cannot cancel the sample collection: the report for an investigation"));
    }

    @Test
    public void cancelCollectionAllowedWhenNothingIssued() {
        StubService s = new StubService();
        s.sampleInvestigations.add(pi(true));
        s.reports.add(new PatientReport());
        assertNull(s.checkCanCancelSampleCollection(collectedSample()));
        assertTrue(s.canCancelSampleCollection(collectedSample()));
    }

    @Test
    public void allSamplesRejectedLogic() {
        StubService s = new StubService();
        assertFalse(s.allSamplesRejected(null));
        assertFalse(s.allSamplesRejected(pi(true)));
        s.samples = Arrays.asList(sample(true), sample(true));
        assertTrue(s.allSamplesRejected(pi(true)));
        s.samples = Arrays.asList(sample(true), sample(false));
        assertFalse(s.allSamplesRejected(pi(true)));
    }
}
