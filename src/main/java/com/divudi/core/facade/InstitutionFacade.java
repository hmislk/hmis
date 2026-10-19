/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.data.InstitutionType;
import com.divudi.core.entity.AuditEvent;
import com.divudi.core.entity.Institution;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author buddhika
 */
@Stateless
public class InstitutionFacade extends AbstractFacade<Institution> {

    private static final Logger LOGGER = Logger.getLogger(InstitutionFacade.class.getName());
    private static final double TOLERANCE = 0.005;

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @EJB
    private AuditEventFacade auditEventFacade;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public InstitutionFacade() {
        super(Institution.class);
    }

    // ===================== Collecting Centre financial field audit (#24603) =====================
    //
    // A Collecting Centre's balance is meant to change only through
    // AgentAndCcApplicationController, which writes an AgentHistory row for
    // every change. Any other save merges the whole Institution and can write
    // back an out-of-date balance or credit limit loaded with a page. These
    // overrides record an AuditEvent whenever such a save changes one of the
    // financial fields, so the user, page and caller of an unexpected change
    // are known. Saving is never blocked or altered.

    @Override
    public void edit(Institution entity) {
        auditCollectingCentreFinancialChanges(entity, "edit");
        super.edit(entity);
    }

    @Override
    public void editAndFlush(Institution entity) {
        auditCollectingCentreFinancialChanges(entity, "editAndFlush");
        super.editAndFlush(entity);
    }

    @Override
    public void editAndCommit(Institution entity) {
        auditCollectingCentreFinancialChanges(entity, "editAndCommit");
        super.editAndCommit(entity);
    }

    /**
     * Saves a Collecting Centre balance change made by
     * AgentAndCcApplicationController. Not audited here because each of
     * those changes already has its own AgentHistory row. Do not use it
     * anywhere else.
     */
    public void editCcBalanceAndCommit(Institution collectingCentre) {
        super.editAndCommit(collectingCentre);
    }

    private void auditCollectingCentreFinancialChanges(Institution entity, String operation) {
        try {
            if (entity == null || entity.getId() == null
                    || entity.getInstitutionType() != InstitutionType.CollectingCentre) {
                return;
            }
            // Scalar query: reads the stored values, not a cached entity.
            // FlushMode COMMIT keeps pending changes from being written
            // before they are compared.
            List<Object[]> rows = getEntityManager().createQuery(
                    "select i.ballance, i.allowedCredit, i.allowedCreditLimit, i.maxCreditLimit, i.standardCreditLimit "
                    + " from Institution i where i.id=:id", Object[].class)
                    .setParameter("id", entity.getId())
                    .setFlushMode(FlushModeType.COMMIT)
                    .getResultList();
            if (rows.isEmpty()) {
                return;
            }
            Object[] stored = rows.get(0);
            String[] fields = {"ballance", "allowedCredit", "allowedCreditLimit", "maxCreditLimit", "standardCreditLimit"};
            double[] saving = {entity.getBallance(), entity.getAllowedCredit(), entity.getAllowedCreditLimit(),
                entity.getMaxCreditLimit(), entity.getStandardCreditLimit()};

            Map<String, Object> before = new LinkedHashMap<>();
            Map<String, Object> after = new LinkedHashMap<>();
            List<String> changed = new ArrayList<>();
            for (int i = 0; i < fields.length; i++) {
                double storedValue = stored[i] == null ? 0.0 : ((Number) stored[i]).doubleValue();
                before.put(fields[i], storedValue);
                after.put(fields[i], saving[i]);
                if (Math.abs(storedValue - saving[i]) > TOLERANCE) {
                    changed.add(fields[i]);
                }
            }
            if (changed.isEmpty()) {
                return;
            }
            before.put("collectingCentreId", entity.getId());
            before.put("collectingCentreCode", entity.getCode());
            after.put("changedFields", changed);
            after.put("operation", operation);
            after.put("caller", findCaller());

            AuditEvent audit = new AuditEvent();
            audit.setEventDataTime(new Date());
            audit.setEntityType("Institution");
            audit.setObjectId(entity.getId());
            audit.setEventTrigger(changed.contains("ballance")
                    ? "Collecting Centre Balance Changed Outside Balance Update"
                    : "Collecting Centre Credit Values Changed");
            audit.setEventStatus(changed.contains("ballance") ? "WARNING" : "Completed");
            Gson gson = new Gson();
            audit.setBeforeJson(gson.toJson(before));
            audit.setAfterJson(gson.toJson(after));
            addRequestDetails(audit);
            auditEventFacade.create(audit);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not audit Collecting Centre financial field change", e);
        }
    }

    /**
     * Fills in the user, department, page and IP from the current JSF request,
     * when there is one (none for REST API calls and background jobs).
     */
    private void addRequestDetails(AuditEvent audit) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return;
        }
        try {
            audit.setWebUserId(context.getApplication().evaluateExpressionGet(
                    context, "#{sessionController.loggedUser.id}", Long.class));
            audit.setDepartmentId(context.getApplication().evaluateExpressionGet(
                    context, "#{sessionController.department.id}", Long.class));
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No logged user for Collecting Centre audit", e);
        }
        if (context.getViewRoot() != null) {
            audit.setUrl(context.getViewRoot().getViewId());
        }
        Object request = context.getExternalContext().getRequest();
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String ip = httpRequest.getHeader("X-Forwarded-For");
            audit.setIpAddress(ip == null ? httpRequest.getRemoteAddr() : ip);
        }
    }

    /**
     * The first application method outside this facade on the call stack,
     * e.g. "com.divudi.bean.lab.CollectingCentreController.saveSelected:493".
     */
    private String findCaller() {
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            String cls = e.getClassName();
            if (cls.startsWith("com.divudi.") && !cls.startsWith("com.divudi.core.facade.")
                    && !cls.contains("$")) {
                return cls + "." + e.getMethodName() + ":" + e.getLineNumber();
            }
        }
        return null;
    }

}
