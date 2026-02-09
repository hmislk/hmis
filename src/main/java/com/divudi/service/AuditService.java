package com.divudi.service;

import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.AuditEventFacade;
import com.google.gson.Gson;
import java.util.Date;
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

    @EJB
    AuditEventFacade auditEventFacade;

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

            auditEventFacade.create(audit);
        } catch (Exception e) {
            e.printStackTrace();
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

            auditEventFacade.create(audit);
        } catch (Exception e) {
            e.printStackTrace();
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

            auditEventFacade.create(audit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
