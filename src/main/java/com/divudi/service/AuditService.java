package com.divudi.service;

import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.WebUser;
import com.google.gson.Gson;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 *
 */
@Stateless
public class AuditService {

    private static final Logger LOGGER = Logger.getLogger(AuditService.class.getName());

    @EJB
    AuditEventService auditEventService;

    private final Gson gson = new Gson();

    /**
     * Get the client IP address from the current HTTP request
     */
    private String getClientIpAddress() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            if (context != null && context.getExternalContext() != null) {
                HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
                String ipAddr = request.getHeader("X-Forwarded-For");
                return (ipAddr == null) ? request.getRemoteAddr() : ipAddr;
            }
        } catch (Exception e) {
            // Fallback if we can't get the IP address
            return "Unknown";
        }
        return "Unknown";
    }

    public void logAudit(Object before, Object after, WebUser user, String entityType, String eventTrigger) {
        try {
            AuditEvent audit = new AuditEvent();
            audit.setEventDataTime(new Date());
            audit.setWebUserId(user.getId());
            audit.setEntityType(entityType);
            audit.setEventTrigger(eventTrigger);
            audit.setBeforeJson(before != null ? gson.toJson(before) : null);
            audit.setAfterJson(after != null ? gson.toJson(after) : null);

            // Set missing audit fields
            audit.setEventStatus("Completed");
            audit.setIpAddress(getClientIpAddress());

            auditEventService.saveAuditEvent(audit);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to record audit event: " + eventTrigger, e);
        }
    }

    public void logAudit(Object before, Object after, WebUser user, String entityType, String eventTrigger, Long objectId) {
        try {
            AuditEvent audit = new AuditEvent();
            audit.setEventDataTime(new Date());
            audit.setWebUserId(user.getId());
            audit.setEntityType(entityType);
            audit.setEventTrigger(eventTrigger);
            audit.setObjectId(objectId);
            audit.setBeforeJson(before != null ? gson.toJson(before) : null);
            audit.setAfterJson(after != null ? gson.toJson(after) : null);

            // Set missing audit fields
            audit.setEventStatus("Completed");
            audit.setIpAddress(getClientIpAddress());

            auditEventService.saveAuditEvent(audit);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to record audit event: " + eventTrigger, e);
        }
    }

    /**
     * Enhanced audit logging with custom event status
     * @param before Object state before change
     * @param after Object state after change
     * @param user User performing the action
     * @param entityType Type of entity being audited
     * @param eventTrigger Action description
     * @param objectId ID of the object being audited
     * @param eventStatus Status of the operation (e.g., "Completed", "Failed", "Cancelled")
     */
    public void logAudit(Object before, Object after, WebUser user, String entityType, String eventTrigger, Long objectId, String eventStatus) {
        try {
            AuditEvent audit = new AuditEvent();
            audit.setEventDataTime(new Date());
            audit.setWebUserId(user.getId());
            audit.setEntityType(entityType);
            audit.setEventTrigger(eventTrigger);
            audit.setObjectId(objectId);
            audit.setBeforeJson(before != null ? gson.toJson(before) : null);
            audit.setAfterJson(after != null ? gson.toJson(after) : null);

            // Set audit fields with custom status
            audit.setEventStatus(eventStatus != null ? eventStatus : "Completed");
            audit.setIpAddress(getClientIpAddress());

            auditEventService.saveAuditEvent(audit);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to record audit event: " + eventTrigger, e);
        }
    }

    /**
     * Records an audit event linked to a PatientEncounter so all events of an
     * admission can be fetched with
     * {@code select a from AuditEvent a where a.patientEncounterId=:peid}.
     * entityType defaults to "PatientEncounter" and objectId to the encounter id.
     */
    public void logEncounterAudit(PatientEncounter pe, String eventTrigger,
            Object before, Object after, WebUser user) {
        logEncounterAudit(pe, eventTrigger, before, after, user, "PatientEncounter",
                pe != null ? pe.getId() : null, null, null);
    }

    public void logEncounterAudit(PatientEncounter pe, String eventTrigger,
            Object before, Object after, WebUser user, String entityType, Long objectId) {
        logEncounterAudit(pe, eventTrigger, before, after, user, entityType, objectId, null, null);
    }

    /**
     * Full form for callers that audit a related entity (e.g. PatientRoom, Bill)
     * while still linking the event to the encounter, optionally recording the
     * acting institution/department (session data the EJB cannot reach itself).
     */
    public void logEncounterAudit(PatientEncounter pe, String eventTrigger,
            Object before, Object after, WebUser user, String entityType, Long objectId,
            Long institutionId, Long departmentId) {
        try {
            AuditEvent audit = new AuditEvent();
            audit.setEventDataTime(new Date());
            if (user != null) {
                audit.setWebUserId(user.getId());
            }
            audit.setEntityType(entityType);
            audit.setEventTrigger(eventTrigger);
            audit.setObjectId(objectId);
            if (pe != null) {
                audit.setPatientEncounterId(pe.getId());
                audit.setUrl("BHT: " + pe.getBhtNo());
            }
            audit.setInstitutionId(institutionId);
            audit.setDepartmentId(departmentId);
            audit.setBeforeJson(before != null ? gson.toJson(before) : null);
            audit.setAfterJson(after != null ? gson.toJson(after) : null);
            audit.setEventStatus("Completed");
            audit.setIpAddress(getClientIpAddress());
            auditEventService.saveAuditEvent(audit);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to record audit event: " + eventTrigger, e);
        }
    }
}
