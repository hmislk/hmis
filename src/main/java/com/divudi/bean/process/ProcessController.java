package com.divudi.bean.process;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.process.ProcessDefinition;
import com.divudi.entity.process.ProcessInstance;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;

/**
 *
 * @author ASUS
 */
@Named
@ViewScoped
public class ProcessController implements Serializable {

    @Inject
    ProcessDefinitionController processDefinitionController;
    @Inject
    SessionController sessionController;
    
    private String stage ;

    /**
     * Creates a new instance of ProcessController
     */
    public ProcessController() {
    }

    private List<ProcessDefinition> processDefinitions;
    private ProcessInstance processInstance;
    private ProcessDefinition processDefinition;

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
        
        return null;
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
        if(stage==null || stage.trim().equals("")){
            stage = "initiate_process";
        }
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    
}
