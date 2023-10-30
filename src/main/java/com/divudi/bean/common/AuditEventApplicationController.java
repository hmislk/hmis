package com.divudi.bean.common;

import com.divudi.entity.AuditEvent;
import com.divudi.facade.AuditEventFacade;
import java.util.ArrayList;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.ejb.EJB;

/**
 *
 * @author Senula Nanayakkara
 */
@Named
@ApplicationScoped
public class AuditEventApplicationController {

    @EJB
    AuditEventFacade auditEventFacade;
    ArrayList<AuditEvent> auditEvents = new ArrayList<>();

    public AuditEventApplicationController() {
    }

    private final BlockingQueue<AuditEvent> eventQueue = new ArrayBlockingQueue<>(100);
    private ExecutorService executorService;

    @PostConstruct
    public void initialize() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this::processEventQueue);
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }

    public void logAuditEvent(AuditEvent event) {
        try {
            eventQueue.put(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void saveAutitEvent(AuditEvent auditEvent) {
        if (auditEvent == null) {
            return;
        }
        if (auditEvent.getId() == null) {
            auditEventFacade.create(auditEvent);
        }else{
            auditEventFacade.edit(auditEvent);
        }
    }

    private void processEventQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                AuditEvent event = eventQueue.take();
                saveOrUpdateEvent(event);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void saveOrUpdateEvent(AuditEvent auditEvent) {
        if (auditEvent == null) {
            return;
        }

        if (auditEvent.getId() == null) {
            auditEventFacade.create(auditEvent);
        } else {
            auditEventFacade.edit(auditEvent);
        }
    }
}
