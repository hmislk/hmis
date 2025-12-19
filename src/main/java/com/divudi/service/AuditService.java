package com.divudi.service;

import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.AuditEventFacade;
import com.google.gson.Gson;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

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

    public void logAudit(Object before, Object after, WebUser user, String entityType, String eventTrigger) {
        try {
            AuditEvent audit = new AuditEvent();
            audit.setEventDataTime(new Date());
            audit.setWebUserId(user.getId());
            audit.setEntityType(entityType);
            audit.setEventTrigger(eventTrigger);
            audit.setBeforeJson(before != null ? gson.toJson(before) : null);
            audit.setAfterJson(after != null ? gson.toJson(after) : null);
            auditEventFacade.create(audit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void logAudit(String before, String after, WebUser user, String entityType, String eventTrigger, Long objectId) {
        try {
            AuditEvent audit = new AuditEvent();
            audit.setEventDataTime(new Date());
            audit.setWebUserId(user.getId());
            audit.setEntityType(entityType);
            audit.setEventTrigger(eventTrigger);
            audit.setBeforeJson(before != null ? before : null);
            audit.setAfterJson(after != null ? after : null);
            audit.setObjectId(objectId);
            auditEventFacade.create(audit);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
