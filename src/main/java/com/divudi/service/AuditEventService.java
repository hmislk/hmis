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

    private static final Logger logger = LogManager.getLogger(AuditEventService.class);

    @EJB
    private AuditEventFacade auditEventFacade;

    @Asynchronous
    public void saveAuditEvent(AuditEvent auditEvent) {
        if (auditEvent == null) {
            logger.warn("Attempted to save a null AuditEvent");
            return;
        }

        try {
            if (auditEvent.getId() == null) {
                auditEventFacade.create(auditEvent);
                logger.info("Created new AuditEvent: {}", auditEvent);
            } else {
                auditEventFacade.edit(auditEvent);
                logger.info("Updated existing AuditEvent with ID {}: {}", auditEvent.getId(), auditEvent);
            }
        } catch (Exception e) {
            logger.error("Failed to save AuditEvent", e);
        }
    }
}
