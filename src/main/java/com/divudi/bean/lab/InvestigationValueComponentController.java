/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.lab;

import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.InvestigationItemValidator;
import com.divudi.entity.lab.InvestigationValidateComponent;
import com.divudi.facade.InvestigationItemValidatorFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.validator.Validator;

/**
 *
 * @author pasan
 */
@Named(value = "investigationValueComponentController")
@SessionScoped
public class InvestigationValueComponentController implements Serializable {

    /**
     * Creates a new instance of InvestigationValueComponentController
     */
    public InvestigationValueComponentController() {
    }

    Investigation currentInvestigation;
    InvestigationValidateComponent current;
    InvestigationItemValidator currentValidator;
    private List<InvestigationItemValidator> investigationItemValidators;
    @EJB
    private InvestigationItemValidatorFacade investigationItemValidatorFacade;
    private String newValidatorName;

    public void addNewValidator() {
        currentValidator = new InvestigationItemValidator();
        currentValidator.setName(newValidatorName);
        currentValidator.setItem(currentInvestigation);
        getInvestigationItemValidatorFacade().create(currentValidator);
        listItemValidator();

    }

    public void listItemValidator() {

        investigationItemValidators = new ArrayList<>();
        String sql;
        sql = "select i from InvestigationItemValidator i where "
                + " i.retired=false ";
        investigationItemValidators = getInvestigationItemValidatorFacade().findBySQL(sql);

    }

    public Investigation getCurrentInvestigation() {
        if (currentInvestigation == null) {
            currentInvestigation = new Investigation();
            //current = null;
        }

        current = null;
        return currentInvestigation;
    }

    public InvestigationValidateComponent getCurrent() {

        if (current == null) {
            current = new InvestigationValidateComponent();
        }
        return current;
    }

    public void setCurrent(InvestigationValidateComponent current) {
        this.current = current;
    }

    public List<InvestigationItemValidator> getInvestigationItemValidators() {
        if (investigationItemValidators == null) {
            JsfUtil.addErrorMessage("No Validators");
        }
        return investigationItemValidators;
    }

    public void setInvestigationItemValidators(List<InvestigationItemValidator> investigationItemValidators) {
        this.investigationItemValidators = investigationItemValidators;
    }

    public InvestigationItemValidatorFacade getInvestigationItemValidatorFacade() {
        return investigationItemValidatorFacade;
    }

    public void setInvestigationItemValidatorFacade(InvestigationItemValidatorFacade investigationItemValidatorFacade) {
        this.investigationItemValidatorFacade = investigationItemValidatorFacade;
    }

    public String getNewValidatorName() {

        return newValidatorName;
    }

    public void setNewValidatorName(String newValidatorName) {
        this.newValidatorName = newValidatorName;
    }

    public InvestigationItemValidator getCurrentValidator() {
        
        
        return currentValidator;
    }

    public void setCurrentValidator(InvestigationItemValidator currentValidator) {
        this.currentValidator = currentValidator;
    }

    
}
