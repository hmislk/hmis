/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.inward.InpatientTimelineRow;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.AuditEventFacade;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.WebUserFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Inpatient Event History ("Patient Story", #22240) — merges the encounter's
 * bills and its encounter-linked audit events into one chronological timeline
 * shown from the Inpatient Dashboard.
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class InpatientEventHistoryController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private WebUserController webUserController;

    @EJB
    private BillFacade billFacade;
    @EJB
    private AuditEventFacade auditEventFacade;
    @EJB
    private WebUserFacade webUserFacade;

    private PatientEncounter patientEncounter;
    private List<InpatientTimelineRow> timelineRows;

    public String navigateToEventHistory(PatientEncounter pe) {
        if (pe == null) {
            JsfUtil.addErrorMessage("No admission selected");
            return "";
        }
        if (!webUserController.hasPrivilege("InwardEventHistoryView")) {
            JsfUtil.addErrorMessage("You are not authorized to view the event history");
            return "";
        }
        patientEncounter = pe;
        loadTimeline();
        return "/inward/admission_event_history?faces-redirect=true";
    }

    public void loadTimeline() {
        timelineRows = new ArrayList<>();
        if (patientEncounter == null || patientEncounter.getId() == null) {
            return;
        }

        String billJpql = "select b from Bill b "
                + " where b.retired=false "
                + " and b.patientEncounter=:pe "
                + " order by b.createdAt";
        Map<String, Object> billParams = new HashMap<>();
        billParams.put("pe", patientEncounter);
        List<Bill> bills = billFacade.findByJpql(billJpql, billParams);
        if (bills != null) {
            for (Bill b : bills) {
                StringBuilder description = new StringBuilder();
                if (b.getBillTypeAtomic() != null) {
                    description.append(b.getBillTypeAtomic().getLabel());
                } else if (b.getBillType() != null) {
                    description.append(b.getBillType().getLabel());
                } else {
                    description.append("Bill");
                }
                if (b.getDeptId() != null && !b.getDeptId().trim().isEmpty()) {
                    description.append(" — ").append(b.getDeptId());
                }
                if (b.isCancelled()) {
                    description.append(" (Cancelled)");
                }
                timelineRows.add(new InpatientTimelineRow(
                        b.getCreatedAt(),
                        "Billing",
                        description.toString(),
                        b.getCreater() != null ? b.getCreater().getName() : null,
                        b.getId(),
                        b.getNetTotal(),
                        null));
            }
        }

        String auditJpql = "select a from AuditEvent a "
                + " where a.patientEncounterId=:peid "
                + " order by a.eventDataTime";
        Map<String, Object> auditParams = new HashMap<>();
        auditParams.put("peid", patientEncounter.getId());
        List<AuditEvent> auditEvents = auditEventFacade.findByJpql(auditJpql, auditParams);
        if (auditEvents != null) {
            Map<Long, String> userNames = resolveUserNames(auditEvents);
            for (AuditEvent ae : auditEvents) {
                timelineRows.add(new InpatientTimelineRow(
                        ae.getEventDataTime(),
                        categorize(ae.getEventTrigger()),
                        ae.getEventTrigger(),
                        ae.getWebUserId() != null ? userNames.get(ae.getWebUserId()) : null,
                        null,
                        null,
                        ae));
            }
        }

        Collections.sort(timelineRows);
    }

    /**
     * AuditEvent stores only the webUserId (it lives in the audit persistence
     * unit); resolve display names from the main unit in one query.
     */
    private Map<Long, String> resolveUserNames(List<AuditEvent> auditEvents) {
        Map<Long, String> names = new HashMap<>();
        Set<Long> ids = new HashSet<>();
        for (AuditEvent ae : auditEvents) {
            if (ae.getWebUserId() != null) {
                ids.add(ae.getWebUserId());
            }
        }
        if (ids.isEmpty()) {
            return names;
        }
        String jpql = "select w from WebUser w where w.id in :ids";
        Map<String, Object> params = new HashMap<>();
        params.put("ids", new ArrayList<>(ids));
        List<WebUser> users = webUserFacade.findByJpql(jpql, params);
        if (users != null) {
            for (WebUser u : users) {
                names.put(u.getId(), u.getName());
            }
        }
        return names;
    }

    private String categorize(String trigger) {
        if (trigger == null) {
            return "Other";
        }
        String t = trigger.toLowerCase();
        if (t.contains("room") || t.contains("transfer") || t.contains("theatre")) {
            return "Room / Transfer";
        }
        if (t.contains("discharge")) {
            return "Discharge";
        }
        if (t.contains("credit") || t.contains("payment")) {
            return "Payment / Credit";
        }
        if (t.contains("discount") || t.contains("fee") || t.contains("settle")
                || t.contains("price")) {
            return "Financial";
        }
        if (t.contains("admission") || t.contains("bht") || t.contains("patient changed")) {
            return "Admission";
        }
        return "Other";
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public List<InpatientTimelineRow> getTimelineRows() {
        if (timelineRows == null) {
            timelineRows = new ArrayList<>();
        }
        return timelineRows;
    }

    public void setTimelineRows(List<InpatientTimelineRow> timelineRows) {
        this.timelineRows = timelineRows;
    }

}
