package com.divudi.service;

import com.divudi.core.entity.AuditEvent;
import com.divudi.core.facade.AuditEventFacade;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Stateless
public class AuditEventService {
    @EJB
    private AuditEventFacade auditEventFacade;

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
        }
    }
}
