package com.divudi.service;

import com.divudi.core.entity.AuditEvent;
import com.divudi.core.facade.AuditEventFacade;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author Dr M H Buddhika Ariyaratne
 */
@Stateless
public class AuditEventService {

    @EJB
    private AuditEventFacade auditEventFacade;

    /**
     * Save an audit event asynchronously. If the event has no ID, it will be
     * created. Otherwise, it will be updated.
     *
     * @param auditEvent The audit event to be saved.
     */
    @Asynchronous
    public void saveAuditEvent(AuditEvent auditEvent) {
        if (auditEvent == null) {
            return;
        }

        try {
            if (auditEvent.getId() == null) {
                auditEventFacade.create(auditEvent);
            } else {
                auditEventFacade.edit(auditEvent);
            }
        } catch (Exception e) {
            // Optional: Add proper logging here if needed
        }
    }
}
