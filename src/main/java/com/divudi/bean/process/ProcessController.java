package com.divudi.bean.process;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.process.ProcessStepActionType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.process.ProcessDefinition;
import com.divudi.core.entity.process.ProcessInstance;
import com.divudi.core.entity.process.ProcessStepActionDefinition;
import com.divudi.core.entity.process.ProcessStepDefinition;
import com.divudi.core.entity.process.ProcessStepInstance;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.ProcessService;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

/**
 *
 * @author ASUS
 */
@Named
@SessionScoped
public class ProcessController implements Serializable {

    @EJB
    ProcessService processService;

    @Inject
    ProcessDefinitionController processDefinitionController;
    @Inject
    SessionController sessionController;

    private String stage;

    /**
     * Creates a new instance of ProcessController
     */
    public ProcessController() {
    }

    private List<ProcessDefinition> processDefinitions;
    private List<ProcessStepInstance> processStepInstances;
    private List<ProcessInstance> processInstances;
    private ProcessInstance processInstance;
    private ProcessDefinition processDefinition;
    private ProcessStepDefinition processStepDefinition;
    private ProcessStepInstance processStepInstance;
    private List<ProcessStepActionDefinition> processStepActionDefinitions;
    Date fromDate;
    Date toDate;
    private Institution institution;
    private Department department;
    private Institution site;

    public String navigateToProcessIndex() {
        return "/process/index?faces-redirect=true";
    }

    public String navigateToStartNewProcessInstance() {
        resetVariablesToStartProcessInstance();
        stage = "initiate_process";
        return "/process/process?faces-redirect=true";
    }

    public String navigateToListProcesses() {
        return "/process/processes?faces-redirect=true";
    }

    public String listOngoingProcessInstances() {
        processInstances = processService.fetchProcessInstances(fromDate, toDate, institution, site, department, false, null, null, null);
        return null;
    }

    public String listCompletedProcessInstances() {
        processInstances = processService.fetchProcessInstances(fromDate, toDate, institution, site, department, true, null, null, null);
        return null;
    }

    public String listCancelledProcessInstances() {
        processInstances = processService.fetchProcessInstances(fromDate, toDate, institution, site, department, null, true, null, null);
        return null;
    }

    public String listRejectedProcessInstances() {
        processInstances = processService.fetchProcessInstances(fromDate, toDate, institution, site, department, null, true, true, null);
        return null;
    }

    public String listPausedProcessInstances() {
        processInstances = processService.fetchProcessInstances(fromDate, toDate, institution, site, department, null, true, null, null);
        return null;
    }


    public String navigateToViewProcessInstance() {
        if (processInstance == null) {
            JsfUtil.addErrorMessage("No Process Selected");
            return null;
        }
        processStepInstances = processService.fetchProcessStepInstances(processInstance);
        stage = "view_process";
        return null;
    }

    public String navigateToManageProcessInstance(ProcessInstance pi) {
        System.out.println("navigateToManageProcessInstance");
        if (pi == null) {
            JsfUtil.addErrorMessage("No Process Selected");
            return null;
        }
        processInstance=pi;
        processStepInstances = processService.fetchProcessStepInstances(processInstance);
        stage = "view_process";
        return "/process/process";
    }

    public String navigateToCompleteProcessStepInstance(ProcessStepInstance psi) {
        if (psi == null) {
            JsfUtil.addErrorMessage("No Process Step Selected");
            return null;
        }
        processStepInstance = psi;
        processInstance = processStepInstance.getProcessInstance();
        if (processInstance == null) {
            JsfUtil.addErrorMessage("No Process for Selected Process Step");
            return null;
        }
        processDefinition = processInstance.getProcessDefinition();
        if (processDefinition == null) {
            JsfUtil.addErrorMessage("No Process Definition for Selected Process");
            return null;
        }
        processStepDefinition = processStepInstance.getProcessStepDefinition();
        if (processStepDefinition == null) {
            JsfUtil.addErrorMessage("No Process Step Definition for Selected Process Step");
            return null;
        }
        processStepActionDefinitions = processService.fetchProcessStepActionDefinitions(processStepDefinition);
        if (processStepActionDefinitions == null || processStepActionDefinitions.isEmpty()) {
            JsfUtil.addErrorMessage("No Process Step Action Definitions for Selected Process Step Definition");
            return null;
        }

        stage = "initiate_step";
        return null;
    }

    public String startProcessInstance() {
        System.out.println("startProcessInstance");
        if (processDefinition == null) {
            JsfUtil.addErrorMessage("Select a Process Definition");
            return null;
        }
        processInstance = new ProcessInstance();
        processInstance.setCreator(sessionController.getLoggedUser());
        processInstance.setCreatedAt(new Date());
        processInstance.setProcessDefinition(processDefinition);
        processService.save(processInstance);

        processStepDefinition = processService.fetchTheFirstProcessStepDefinition(processDefinition);
        System.out.println("processStepDefinition = " + processStepDefinition);

        processStepInstance = new ProcessStepInstance();
        processStepInstance.setProcessInstance(processInstance);
        processStepInstance.setProcessStepDefinition(processStepDefinition);
        processStepInstance.setCreator(sessionController.getLoggedUser());
        processStepInstance.setCreatedAt(new Date());
        processStepInstance.setStatus("To Initiate");
        processService.save(processStepInstance);

        processStepActionDefinitions = processService.fetchProcessStepActionDefinitions(processStepDefinition);
        stage = "initiate_step";
        return null;
    }

    public String completeProcessStepInstance() {
        if (processStepInstance == null) {
            JsfUtil.addErrorMessage("Select a Process Step Instance.");
            return null;
        }
        if (processStepInstance.getProcessStepActionDefinition() == null) {
            JsfUtil.addErrorMessage("Select an Action for the Process Step.");
            return null;
        }
        if (processStepInstance.getProcessStepDefinition() == null) {
            JsfUtil.addErrorMessage("Process Step Definition is missing.");
            return null;
        }
        if (processStepInstance.getProcessInstance() == null) {
            JsfUtil.addErrorMessage("No associated Process Instance found.");
            return null;
        }

        processInstance = processStepInstance.getProcessInstance();
        ProcessStepActionDefinition actionDef = processStepInstance.getProcessStepActionDefinition();
        ProcessStepInstance previousProcessStepInstance;
        // Handle the specific action taken
        switch (actionDef.getActionType()) {
            case CANCEL_PROCESS:
                processService.cancelProcessInstance(processStepInstance, sessionController.getLoggedUser());
                JsfUtil.addSuccessMessage("Process has been cancelled.");
                JsfUtil.addSuccessMessage("Moved to the next step.");
                return navigateToViewProcessInstance();

            case COMPLETE_PROCESS:
                processService.completeProcessInstance(processStepInstance, sessionController.getLoggedUser());
                JsfUtil.addSuccessMessage("Process has been completed.");
                JsfUtil.addSuccessMessage("Moved to the next step.");
                return navigateToViewProcessInstance();

            case COMPLETE_STEP:
                processService.completeProcessStepInstance(processStepInstance, sessionController.getLoggedUser());
                JsfUtil.addSuccessMessage("Step has been completed.");
                JsfUtil.addSuccessMessage("Moved to the next step.");
                return navigateToViewProcessInstance();

            case CUSTOM_ACTION:
                // Implement custom action logic here
                JsfUtil.addSuccessMessage("Custom action processed.");
                break;
            case ESCALATE_ISSUE:
                // Implement escalation logic
                JsfUtil.addSuccessMessage("Issue has been escalated.");
                break;
            case MOVE_TO_NEXT_STEP:
                processStepDefinition = processService.fetchNextProcessStepDefinition(processStepInstance);
                processService.completeProcessStepInstance(processStepInstance, sessionController.getLoggedUser());
                previousProcessStepInstance = processStepInstance;
                processStepInstance = new ProcessStepInstance();
                processStepInstance.setProcessInstance(processInstance);
                processStepInstance.setPrecedingStepInstance(previousProcessStepInstance);
                processStepInstance.setProcessStepDefinition(processStepDefinition);
                processStepInstance.setCreator(sessionController.getLoggedUser());
                processStepInstance.setCreatedAt(new Date());
                processService.save(processStepInstance);
                JsfUtil.addSuccessMessage("Moved to the next step.");
                return navigateToViewProcessInstance();
            case MOVE_TO_SPECIFIED_STEP:
                processService.completeProcessStepInstance(processStepInstance, sessionController.getLoggedUser());
                processStepDefinition = actionDef.getDirectedProcessStepDefinition();
                previousProcessStepInstance = processStepInstance;
                processStepInstance = new ProcessStepInstance();
                processStepInstance.setProcessInstance(processInstance);
                processStepInstance.setPrecedingStepInstance(previousProcessStepInstance);
                processStepInstance.setProcessStepDefinition(processStepDefinition);
                processStepInstance.setCreator(sessionController.getLoggedUser());
                processStepInstance.setCreatedAt(new Date());
                processService.save(processStepInstance);
                JsfUtil.addSuccessMessage("Moved to the specified step.");
                return navigateToViewProcessInstance();
            case PAUSE_PROCESS:
                processService.pauseProcessInstance(processStepInstance, sessionController.getLoggedUser());
                JsfUtil.addSuccessMessage("Process has been paused.");
                return navigateToViewProcessInstance();
            case REJECT_PROCESS:
                processService.rejectProcessInstance(processStepInstance, sessionController.getLoggedUser());
                JsfUtil.addSuccessMessage("Process has been rejected.");
                return navigateToViewProcessInstance();
            case RETURN_TO_PREVIOUS_STEP:
                processService.completeProcessStepInstance(processStepInstance, sessionController.getLoggedUser());
                processStepDefinition = processService.fetchPreviousProcessStepDefinition(processStepInstance);
                previousProcessStepInstance = processStepInstance;
                processStepInstance = new ProcessStepInstance();
                processStepInstance.setProcessInstance(processInstance);
                processStepInstance.setPrecedingStepInstance(previousProcessStepInstance);
                processStepInstance.setProcessStepDefinition(processStepDefinition);
                processStepInstance.setCreator(sessionController.getLoggedUser());
                processStepInstance.setCreatedAt(new Date());
                processService.save(processStepInstance);
                JsfUtil.addSuccessMessage("Returned to the previous step.");
                return navigateToViewProcessInstance();
            default:
                JsfUtil.addErrorMessage("Unknown action.");
                return navigateToViewProcessInstance();
        }

        return null; // Adjust this as per your JSF navigation rules
    }

    /**
     * Retrieves all values of the ProcessStepActionType enum.
     *
     * @return An array of ProcessStepActionType, which contains all enum
     * values.
     */
    public ProcessStepActionType[] getProcessStepActionTypes() {
        return ProcessStepActionType.values();
    }

    public void resetVariablesToStartProcessInstance() {
        processInstance = null;
    }

    public List<ProcessDefinition> getProcessDefinitions() {
        if (processDefinitions == null) {
            processDefinitions = processDefinitionController.getItems();
        }
        return processDefinitions;
    }

    public void setProcessDefinitions(List<ProcessDefinition> processDefinitions) {
        this.processDefinitions = processDefinitions;
    }

    public ProcessInstance getProcessInstance() {
        return processInstance;
    }

    public void setProcessInstance(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    public ProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    public void setProcessDefinition(ProcessDefinition processDefinition) {
        this.processDefinition = processDefinition;
    }

    public String getStage() {
        if (stage == null || stage.trim().equals("")) {
            stage = "initiate_process";
        }
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public ProcessStepDefinition getProcessStepDefinition() {
        return processStepDefinition;
    }

    public void setProcessStepDefinition(ProcessStepDefinition processStepDefinition) {
        this.processStepDefinition = processStepDefinition;
    }

    public ProcessStepInstance getProcessStepInstance() {
        return processStepInstance;
    }

    public void setProcessStepInstance(ProcessStepInstance processStepInstance) {
        this.processStepInstance = processStepInstance;
    }

    public List<ProcessStepActionDefinition> getProcessStepActionDefinitions() {
        return processStepActionDefinitions;
    }

    public void setProcessStepActionDefinitions(List<ProcessStepActionDefinition> processStepActionDefinitions) {
        this.processStepActionDefinitions = processStepActionDefinitions;
    }

    public List<ProcessStepInstance> getProcessStepInstances() {
        return processStepInstances;
    }

    public void setProcessStepInstances(List<ProcessStepInstance> processStepInstances) {
        this.processStepInstances = processStepInstances;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public List<ProcessInstance> getProcessInstances() {
        if (processInstances == null) {
            processInstances = new ArrayList<>();
        }
        return processInstances;
    }

    public void setProcessInstances(List<ProcessInstance> processInstances) {
        this.processInstances = processInstances;
    }

}
