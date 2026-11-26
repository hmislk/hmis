package com.divudi.service.inward;

import com.divudi.core.data.inward.AdmissionTypeTimeline;
import com.divudi.core.data.inward.AdmissionTypeTimelineMemo;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import java.io.Serializable;
import java.util.Date;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * Admission type timelines kept for one HTTP request only (#17562), so a click
 * such as Create Final Bill reads the history rows of an admission once, not
 * once per fee or room. It is request scoped on purpose: the next click reads
 * fresh rows, so a type change is always seen.
 * <p>
 * Callers that may run outside a request (no active request context) must use
 * {@link AdmissionTypeHistoryService} directly.
 */
@Named
@RequestScoped
public class AdmissionTypeTimelineRequestCache implements Serializable {

    @EJB
    private AdmissionTypeHistoryService admissionTypeHistoryService;

    private transient AdmissionTypeTimelineMemo memo;

    @PostConstruct
    void init() {
        memo = new AdmissionTypeTimelineMemo(admissionTypeHistoryService::timelineFor);
    }

    private AdmissionTypeTimelineMemo memo() {
        if (memo == null) {
            init();
        }
        return memo;
    }

    public AdmissionTypeTimeline timelineFor(PatientEncounter encounter) {
        return memo().timelineFor(encounter);
    }

    public AdmissionType typeAt(PatientEncounter encounter, Date date) {
        return memo().typeAt(encounter, date);
    }

    public AdmissionType firstType(PatientEncounter encounter) {
        return memo().firstType(encounter);
    }

    /** Call after an admission type change within the same request. */
    public void clear() {
        memo().clear();
    }
}
