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
import java.util.HashMap;
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

    public Institution findDefaultInstitution() {
        Institution flagged = findFirstByJpql(
            "select i from Institution i where i.defaultInstitution=true and i.retired=false order by i.id asc");
        if (flagged != null) {
            return flagged;
        }
        return findFirstByJpql(
            "select i from Institution i where i.retired=false order by i.id asc");
    }

    public void clearDefaultInstitutionExceptFor(Long id) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        List<Institution> others = findByJpql(
            "select i from Institution i where i.defaultInstitution=true and i.id<>:id", params);
        for (Institution other : others) {
            other.setDefaultInstitution(false);
            edit(other);
        }
    }

    // ============ Collecting Centre / Agency financial field audit (#24603, #24614) ============
    //
    // A Collecting Centre's or Agency's balance changes only through
    // updateBalance, called alongside an AgentHistory row. Any other save
    // merges the whole Institution and may carry an out-of-date balance or
    // credit limit loaded with a page. The balance column is not updatable,
    // so such a balance is never saved; these overrides also put the stored
    // balance back into the entity and record an AuditEvent whenever a save
    // tries to change one of the financial fields, so the user, page and
    // caller are known. Credit-limit changes are saved as before.

    @Override
    public void edit(Institution entity) {
        protectBalanceAndAuditFinancialChanges(entity, "edit");
        super.edit(entity);
    }

    @Override
    public void editAndFlush(Institution entity) {
        protectBalanceAndAuditFinancialChanges(entity, "editAndFlush");
        super.editAndFlush(entity);
    }

    @Override
    public void editAndCommit(Institution entity) {
        protectBalanceAndAuditFinancialChanges(entity, "editAndCommit");
        super.editAndCommit(entity);
    }

    /**
     * Saves the other fields of a Collecting Centre after a balance update made
     * by AgentAndCcApplicationController (e.g. the credit-limit reset on a
     * deposit). The balance itself is written by updateBalance. Not audited
     * because each of those changes already has its own AgentHistory row.
     * Do not use it anywhere else.
     */
    public void editCcBalanceAndCommit(Institution collectingCentre) {
        super.editAndCommit(collectingCentre);
    }

    /**
     * The only way to change an Institution's balance (#24614). Writes just the
     * balance column, so it cannot overwrite any other field, and no
     * whole-entity save can overwrite it ({@code ballance} is not updatable).
     * The caller must write the matching AgentHistory row.
     */
    public void updateBalance(Institution institution, double newBalance) {
        if (institution == null || institution.getId() == null) {
            return;
        }
        getEntityManager().createQuery("update Institution i set i.ballance=:balance where i.id=:id")
                .setParameter("balance", newBalance)
                .setParameter("id", institution.getId())
                // Evict only this Institution below instead of invalidating every cached one.
                .setHint("eclipselink.cache-usage", "NoCache")
                .executeUpdate();
        institution.setBallance(newBalance);
        getEntityManager().getEntityManagerFactory().getCache().evict(Institution.class, institution.getId());
    }

    /**
     * Adds delta to the stored balance in a single UPDATE, so a balance loaded
     * earlier with a page cannot be written back over a newer one. The row
     * stays locked until the transaction commits, so concurrent changes are
     * applied one after the other. Returns {balanceBefore, balanceAfter} as
     * stored, for the matching AgentHistory row, or null if there is no
     * institution.
     */
    public double[] adjustBalance(Institution institution, double delta) {
        if (institution == null || institution.getId() == null) {
            return null;
        }
        getEntityManager().createQuery("update Institution i set i.ballance=coalesce(i.ballance, 0)+:delta where i.id=:id")
                .setParameter("delta", delta)
                .setParameter("id", institution.getId())
                .setHint("eclipselink.cache-usage", "NoCache")
                .executeUpdate();
        Double after = getEntityManager().createQuery(
                "select i.ballance from Institution i where i.id=:id", Double.class)
                .setParameter("id", institution.getId())
                .getSingleResult();
        double balanceAfter = after == null ? 0.0 : after;
        institution.setBallance(balanceAfter);
        getEntityManager().getEntityManagerFactory().getCache().evict(Institution.class, institution.getId());
        return new double[]{balanceAfter - delta, balanceAfter};
    }

    private void protectBalanceAndAuditFinancialChanges(Institution entity, String operation) {
        try {
            if (entity == null || entity.getId() == null
                    || (entity.getInstitutionType() != InstitutionType.CollectingCentre
                    && entity.getInstitutionType() != InstitutionType.Agency)) {
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
            if (changed.contains("ballance")) {
                // The column is not updatable, so this value would not be saved,
                // but it would still reach the shared cache and be shown as the
                // balance. Keep the stored one instead.
                entity.setBallance(stored[0] == null ? 0.0 : ((Number) stored[0]).doubleValue());
            }
            before.put("collectingCentreId", entity.getId());
            before.put("collectingCentreCode", entity.getCode());
            before.put("institutionType", entity.getInstitutionType().name());
            after.put("changedFields", changed);
            after.put("operation", operation);
            after.put("caller", findCaller());

            AuditEvent audit = new AuditEvent();
            audit.setEventDataTime(new Date());
            audit.setEntityType("Institution");
            audit.setObjectId(entity.getId());
            audit.setEventTrigger(changed.contains("ballance")
                    ? "Balance Change Outside Balance Update Not Saved"
                    : "Collecting Centre / Agency Credit Values Changed");
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
