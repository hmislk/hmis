package com.divudi.core.data.inward;

import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Remembers the admission type timeline of each admission, so the history rows
 * are read once per admission instead of once per fee, room or charge (#17562).
 * The answers come from the same {@link AdmissionTypeTimeline} the per-call
 * lookup builds, so the type at every date is unchanged.
 * <p>
 * Not thread safe and not shared: hold one per request or per run, never in a
 * session or application field, or a later type change would not be seen.
 */
public class AdmissionTypeTimelineMemo {

    private final Function<PatientEncounter, AdmissionTypeTimeline> loader;
    private final Map<Long, AdmissionTypeTimeline> timelines = new HashMap<>();

    public AdmissionTypeTimelineMemo(Function<PatientEncounter, AdmissionTypeTimeline> loader) {
        this.loader = loader;
    }

    /**
     * The timeline of the admission. An admission not saved yet is never kept.
     * A kept timeline is used only while the admission still has the current
     * type it was built with; otherwise it is read again.
     */
    public AdmissionTypeTimeline timelineFor(PatientEncounter encounter) {
        if (encounter == null || encounter.getId() == null) {
            return loader.apply(encounter);
        }
        AdmissionTypeTimeline known = timelines.get(encounter.getId());
        if (known != null && known.getCurrentType() == encounter.getAdmissionType()) {
            return known;
        }
        AdmissionTypeTimeline loaded = loader.apply(encounter);
        timelines.put(encounter.getId(), loaded);
        return loaded;
    }

    public AdmissionType typeAt(PatientEncounter encounter, Date date) {
        return timelineFor(encounter).typeAt(date);
    }

    public AdmissionType firstType(PatientEncounter encounter) {
        return timelineFor(encounter).firstType();
    }

    /** Forgets everything, e.g. after an admission type change. */
    public void clear() {
        timelines.clear();
    }
}
