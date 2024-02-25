/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.WorksheetItem;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.WorksheetItemFacade;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class LabWorksheetController implements Serializable {

    /**
     * Creates a new instance of LabWorksheetController
     */
    public LabWorksheetController() {
    }
    @EJB
    InvestigationFacade ixFacade;
    @EJB
    WorksheetItemFacade wiFacade;
    WorksheetItem current;
    Investigation investigation;

    public InvestigationFacade getIxFacade() {
        return ixFacade;
    }

    public void setIxFacade(InvestigationFacade ixFacade) {
        this.ixFacade = ixFacade;
    }

    public WorksheetItemFacade getWiFacade() {
        return wiFacade;
    }

    public void setWiFacade(WorksheetItemFacade wiFacade) {
        this.wiFacade = wiFacade;
    }

    public WorksheetItem getCurrent() {
        if(current==null){
            current = new WorksheetItem();
        }
        current.setItem(investigation);
        return current;
    }

    public void setCurrent(WorksheetItem current) {
        this.current = current;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        current=null;
        this.investigation = investigation;
    }


    public void addWorksheetItem(){
        if(current==null){
            JsfUtil.addErrorMessage("No worksheet");
            return;
        }
        if(investigation==null){
            JsfUtil.addErrorMessage("No Ix");
            return;
        }
        getInvestigation().getWorksheetItems().add(current);
        getIxFacade().edit(investigation);
        JsfUtil.addSuccessMessage("Added");
        current=null;
    }
    

    
    
        public void removeWorksheetItem(){
        if(current==null){
            JsfUtil.addErrorMessage("No worksheet");
            return;
        }
        if(investigation==null){
            JsfUtil.addErrorMessage("No Ix");
            return;
        }
        getInvestigation().getWorksheetItems().remove(current);
        getIxFacade().edit(investigation);
        JsfUtil.addSuccessMessage("Removed");
        current=null;
    }

    
    
    public void saveIx(){
    for(WorksheetItem wsi:investigation.getWorksheetItems()){
        getWiFacade().edit(wsi);
    }
    getIxFacade().edit(investigation);
    JsfUtil.addSuccessMessage("Updated");
}








}
