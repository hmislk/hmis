package com.divudi.bean.clinical;

import com.divudi.entity.PatientEncounter;
import com.divudi.bean.common.util.JsfUtil;
import javax.inject.Named;
import java.io.Serializable;
import javax.enterprise.context.SessionScoped;

/**
 *
 * @author buddh
 */
@Named(value = "viewEncounterController")
@SessionScoped
public class ViewEncounterController implements Serializable {

    private PatientEncounter encounter;
    
    /**
     * Creates a new instance of ViewEncounterController
     */
    public ViewEncounterController() {
    }

    public String navigateToNewEncounter(){
        if(encounter==null){
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        String page = "/emr/opd_visit_view";
        return page;
    }

    public PatientEncounter getEncounter() {
        return encounter;
    }

    public void setEncounter(PatientEncounter encounter) {
        this.encounter = encounter;
    }
    
    
}
