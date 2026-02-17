package com.divudi.service;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.process.ProcessDefinition;
import com.divudi.core.entity.process.ProcessInstance;
import com.divudi.core.entity.process.ProcessStepActionDefinition;
import com.divudi.core.entity.process.ProcessStepDefinition;
import com.divudi.core.entity.process.ProcessStepInstance;
import com.divudi.core.facade.ProcessInstanceFacade;
import com.divudi.core.facade.ProcessStepActionDefinitionFacade;
import com.divudi.core.facade.ProcessStepDefinitionFacade;
import com.divudi.core.facade.ProcessStepInstanceFacade;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr M H B Ariyaratne buddhika.ari@gmail.com
 *
 */
@Stateless
public class ProcessService {

    @EJB
    ProcessStepDefinitionFacade processStepDefinitionFacade;
    @EJB
    ProcessStepActionDefinitionFacade processStepActionDefinitionFacade;
    @EJB
    ProcessInstanceFacade processInstanceFacade;
    @EJB
    ProcessStepInstanceFacade processStepInstanceFacade;

    /**
     * Save or update a ProcessInstance.
     *
     * @param processInstance The ProcessInstance to save or update.
     * @return true if save or update is successful, false otherwise.
     */
    public boolean save(ProcessInstance processInstance) {
        if (processInstance == null) {
            return false;
        }
        try {
            if (processInstance.getId() == null) {
                processInstanceFacade.create(processInstance);
            } else {
                processInstanceFacade.edit(processInstance);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Save or update a ProcessStepInstance.
     *
     * @param processStepInstance The ProcessStepInstance to save or update.
     * @return true if save or update is successful, false otherwise.
     */
    public boolean save(ProcessStepInstance processStepInstance) {
        if (processStepInstance == null) {
            return false;
        }
        try {
            if (processStepInstance.getId() == null) {
                processStepInstanceFacade.create(processStepInstance);
            } else {
                processStepInstanceFacade.edit(processStepInstance);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fetches a list of active (non-retired) ProcessStepActionDefinition
     * entities associated with a given ProcessStepDefinition.
     *
     * @param processStepDefinition The ProcessStepDefinition for which to
     * retrieve the action definitions.
     * @return A list of ProcessStepActionDefinition entities or an empty list
     * if no active action definitions are found.
     */
    public List<ProcessStepActionDefinition> fetchProcessStepActionDefinitions(ProcessStepDefinition processStepDefinition) {
        if (processStepDefinition == null || processStepDefinition.getId() == null) {
            return new ArrayList<>(); // Return an empty list if the definition is null or not saved yet.
        }
        String jpql = "SELECT a FROM ProcessStepActionDefinition a WHERE a.processStepDefinition = :processStepDefinition AND a.retired = :retired ORDER BY a.sequenceOrder";
        Map<String, Object> params = new HashMap<>();
        params.put("processStepDefinition", processStepDefinition);
        params.put("retired", false); // Only fetch non-retired action definitions
        return processStepActionDefinitionFacade.findByJpql(jpql, params);
    }

    /**
     * Fetches the next ProcessStepDefinition based on the current
     * ProcessStepInstance. Assumes that there is a sequenced order or linked
     * list-like structure to determine the next step.
     *
     * @param processStepInstance The current ProcessStepInstance from which to
     * find the next step.
     * @return The next ProcessStepDefinition or null if there is no next step
     * or input is invalid.
     */
    public ProcessStepDefinition fetchNextProcessStepDefinition(ProcessStepInstance processStepInstance) {
        if (processStepInstance == null || processStepInstance.getProcessStepDefinition() == null) {
            return null; // No valid current step to determine the next step from.
        }

        Double currentSequence = processStepInstance.getProcessStepDefinition().getSequenceOrder();
        if (currentSequence == null) {
            return null; // Current step does not have a sequence order.
        }

        String jpql = "SELECT psd FROM ProcessStepDefinition psd WHERE psd.processDefinition = :processDefinition AND psd.sequenceOrder > :currentSequence ORDER BY psd.sequenceOrder ASC";
        Map params = new HashMap(); // Using raw type for the Map
        params.put("processDefinition", processStepInstance.getProcessStepDefinition().getProcessDefinition());
        params.put("currentSequence", currentSequence);
        List<ProcessStepDefinition> nextSteps = processStepDefinitionFacade.findByJpql(jpql, params);

        if (nextSteps.isEmpty()) {
            return null; // No subsequent steps found.
        }

        return nextSteps.get(0); // Return the next step in sequence.
    }

    /**
     * Fetches the previous ProcessStepDefinition based on the current
     * ProcessStepInstance. Assumes that there is a sequenced order or linked
     * list-like structure to determine the previous step.
     *
     * @param processStepInstance The current ProcessStepInstance from which to
     * find the previous step.
     * @return The previous ProcessStepDefinition or null if there is no
     * previous step or input is invalid.
     */
    public ProcessStepDefinition fetchPreviousProcessStepDefinition(ProcessStepInstance processStepInstance) {
        if (processStepInstance == null || processStepInstance.getProcessStepDefinition() == null) {
            return null; // No valid current step to determine the previous step from.
        }

        Double currentSequence = processStepInstance.getProcessStepDefinition().getSequenceOrder();
        if (currentSequence == null) {
            return null; // Current step does not have a sequence order.
        }

        String jpql = "SELECT psd FROM ProcessStepDefinition psd WHERE psd.processDefinition = :processDefinition AND psd.sequenceOrder < :currentSequence ORDER BY psd.sequenceOrder DESC";
        Map params = new HashMap(); // Using raw type for the Map
        params.put("processDefinition", processStepInstance.getProcessStepDefinition().getProcessDefinition());
        params.put("currentSequence", currentSequence);
        List<ProcessStepDefinition> previousSteps = processStepDefinitionFacade.findByJpql(jpql, params);

        if (previousSteps.isEmpty()) {
            return null; // No preceding steps found.
        }

        return previousSteps.get(0); // Return the closest preceding step in sequence.
    }

    public ProcessStepDefinition fetchTheFirstProcessStepDefinition(ProcessDefinition processDefinition) {
        // JPQL query to fetch the first process step associated with a given process definition
        String jpql = "SELECT ps FROM ProcessStepDefinition ps WHERE ps.processDefinition = :processDef AND ps.retired = :retired ORDER BY ps.sequenceOrder ASC";
        Map<String, Object> params = new HashMap<>();
        params.put("processDef", processDefinition);
        params.put("retired", false);
        // Fetch the first result from the query
        ProcessStepDefinition psd = processStepDefinitionFacade.findFirstByJpql(jpql, params);
        return psd;
    }

    public void cancelProcessInstance(ProcessStepInstance processStepInstance, WebUser user) {
        if (processStepInstance != null && processStepInstance.getProcessInstance() != null && user != null) {
            ProcessInstance processInstance = processStepInstance.getProcessInstance();
            processInstance.setStatus("Cancelled");
            processInstance.setCancelled(true);
            processInstance.setCancelledBy(user);
            processInstance.setCancelledAt(new Date());
            save(processInstance);
            completeProcessStepInstance(processStepInstance, user);
        }
    }

    public void completeProcessInstance(ProcessStepInstance processStepInstance, WebUser user) {
        if (processStepInstance != null && processStepInstance.getProcessInstance() != null && user != null) {
            ProcessInstance processInstance = processStepInstance.getProcessInstance();
            processInstance.setStatus("Completed");
            processInstance.setCompleted(true);
            processInstance.setCompletedBy(user);
            processInstance.setCompletedAt(new Date());
            save(processInstance);
            completeProcessStepInstance(processStepInstance, user);

        }
    }

    public void rejectProcessInstance(ProcessStepInstance processStepInstance, WebUser user) {
        if (processStepInstance != null && processStepInstance.getProcessInstance() != null && user != null) {
            ProcessInstance processInstance = processStepInstance.getProcessInstance();
            processInstance.setRejected(true);
            processInstance.setStatus("Rejected");
            processInstance.setRejectedBy(user);
            processInstance.setRejectedAt(new Date());
            save(processInstance);
            completeProcessStepInstance(processStepInstance, user);
        }
    }

    public void pauseProcessInstance(ProcessStepInstance processStepInstance, WebUser user) {
        if (processStepInstance != null && processStepInstance.getProcessInstance() != null && user != null) {
            ProcessInstance processInstance = processStepInstance.getProcessInstance();
            processInstance.setStatus("Paused");
            processInstance.setPaused(true);
            processInstance.setPausedBy(user);
            processInstance.setPausedAt(new Date());
            save(processInstance);
            completeProcessStepInstance(processStepInstance, user);
        }
    }

    // Helper method to mark a ProcessStepInstance as completed
    public void completeProcessStepInstance(ProcessStepInstance processStepInstance, WebUser user) {
        processStepInstance.setCompleted(true);
        processStepInstance.setCompletedBy(user);
        processStepInstance.setCompletedAt(new Date());
        processStepInstance.setStatus("Completed");
        save(processStepInstance);
    }

    /**
     * Fetches all ProcessStepInstance entities associated with a given
     * ProcessInstance.
     *
     * @param processInstance The ProcessInstance for which to find the step
     * instances.
     * @return A list of ProcessStepInstance entities or an empty list if no
     * instances are found.
     */
    public List<ProcessStepInstance> fetchProcessStepInstances(ProcessInstance processInstance) {
        if (processInstance == null || processInstance.getId() == null) {
            return new ArrayList<>(); // Return an empty list if the instance is null or not saved yet.
        }
        String jpql = "SELECT psi FROM ProcessStepInstance psi WHERE psi.processInstance = :processInstance ORDER BY psi.createdAt";
        Map params = new HashMap(); // Using raw type for the Map
        params.put("processInstance", processInstance);
        List<ProcessStepInstance> stepInstances = processStepInstanceFacade.findByJpql(jpql, params);
        return stepInstances;
    }

    public List<ProcessInstance> fetchProcessInstances(Date fromDate, Date toDate, Institution institution, Institution site, Department department, Boolean completed, Boolean cancelled, Boolean rejected, Boolean paused) {
        String jpql = "SELECT pi FROM ProcessInstance pi WHERE pi.retired = :ret";
        Map params = new HashMap(); // Using raw type for the Map
        params.put("ret", false);

        if (fromDate != null) {
            jpql += " AND pi.createdAt >= :fromDate";
            params.put("fromDate", fromDate);
        }
        if (toDate != null) {
            jpql += " AND pi.createdAt <= :toDate";
            params.put("toDate", toDate);
        }
        if (institution != null) {
            jpql += " AND pi.institution = :institution";
            params.put("institution", institution);
        }
        if (site != null) {
            jpql += " AND pi.department.site = :site";
            params.put("site", site);
        }
        if (department != null) {
            jpql += " AND pi.department = :department";
            params.put("department", department);
        }
        if (completed != null) {
            jpql += " AND pi.completed = :completed";
            params.put("completed", completed);
        }
        if (cancelled != null) {
            jpql += " AND pi.cancelled = :cancelled";
            params.put("cancelled", cancelled);
        }
        if (rejected != null) {
            jpql += " AND pi.rejected = :rejected";
            params.put("rejected", rejected);
        }
        if (paused != null) {
            jpql += " AND pi.paused = :paused";
            params.put("paused", paused);
        }

        jpql += " ORDER BY pi.createdAt DESC"; // Ordering by createdAt date in descending order

        List<ProcessInstance> processInstances = processInstanceFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        return processInstances;
    }

}
