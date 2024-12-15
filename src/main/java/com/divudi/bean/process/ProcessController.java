package com.divudi.bean.process;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.process.ProcessDefinition;
import com.divudi.entity.process.ProcessInstance;
import com.divudi.entity.process.ProcessStepActionDefinition;
import com.divudi.entity.process.ProcessStepDefinition;
import com.divudi.entity.process.ProcessStepInstance;
import com.divudi.service.ProcessService;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author ASUS
 */
@Named
@ViewScoped
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
    private ProcessInstance processInstance;
    private ProcessDefinition processDefinition;
    private ProcessStepDefinition processStepDefinition;
    private ProcessStepInstance processStepInstance;
    private List<ProcessStepActionDefinition> processStepActionDefinitions;

    public String navigateToProcessIndex() {
        return "/process/index?faces-redirect=true;";
    }

    public String navigateToStartNewProcessInstance() {
        resetVariablesToStartProcessInstance();
        stage = "initiate_process";
        return "/process/process?faces-redirect=true;";
    }

    public String startProcessInstance() {
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

        processStepInstance = new ProcessStepInstance();
        processStepInstance.setProcessInstance(processInstance);
        processStepInstance.setProcessStepDefinition(processStepDefinition);
        processStepInstance.setCreator(sessionController.getLoggedUser());
        processStepInstance.setCreatedAt(new Date());
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

        // Handle the specific action taken
        switch (actionDef.getActionType()) {
            case CANCEL_PROCESS:
                processService.cancelProcessInstance(processStepInstance, sessionController.getLoggedUser());
                JsfUtil.addSuccessMessage("Process has been cancelled.");
                break;
            case COMPLETE_PROCESS:
                processService.completeProcessInstance(processStepInstance, sessionController.getLoggedUser());
                JsfUtil.addSuccessMessage("Process has been completed.");
                break;
            case COMPLETE_STEP:
                processService.completeProcessStepInstance(processStepInstance, sessionController.getLoggedUser());
                JsfUtil.addSuccessMessage("Step has been completed.");
                break;
            case CUSTOM_ACTION:
                // Implement custom action logic here
                JsfUtil.addSuccessMessage("Custom action processed.");
                break;
            case ESCALATE_ISSUE:
                // Implement escalation logic
                JsfUtil.addSuccessMessage("Issue has been escalated.");
                break;
            case MOVE_TO_NEXT_STEP:
                processStepDefinition = processService.fetchTheFirstProcessStepDefinition(processDefinition);

                processStepInstance = new ProcessStepInstance();
                processStepInstance.setProcessInstance(processInstance);
                processStepInstance.setProcessStepDefinition(processStepDefinition);
                processStepInstance.setCreator(sessionController.getLoggedUser());
                processStepInstance.setCreatedAt(new Date());
                processService.save(processStepInstance);

                processStepActionDefinitions = processService.fetchProcessStepActionDefinitions(processStepDefinition);

                if (actionDef.isAllowsMultipleActions()) {

                    stage = "initiate_step";
                } else {
                    stage = "view_process";
                }
                JsfUtil.addSuccessMessage("Moved to the next step.");
                break;
            case MOVE_TO_SPECIFIED_STEP:
                // Logic to move to a specified step
                JsfUtil.addSuccessMessage("Moved to the specified step.");
                break;
            case PAUSE_PROCESS:
                processService.pauseProcessInstance(processStepInstance, sessionController.getLoggedUser());
                JsfUtil.addSuccessMessage("Process has been paused.");
                break;
            case REJECT_PROCESS:
                processService.rejectProcessInstance(processStepInstance, sessionController.getLoggedUser());
                JsfUtil.addSuccessMessage("Process has been rejected.");
                break;
            case RETURN_TO_PREVIOUS_STEP:
                // Logic to return to the previous step
                JsfUtil.addSuccessMessage("Returned to the previous step.");
                break;
            default:
                JsfUtil.addErrorMessage("Unknown action.");
                break;
        }

        return "some_navigation_case"; // Adjust this as per your JSF navigation rules
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

}
