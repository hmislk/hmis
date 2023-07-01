package com.divudi.bean.web;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class WebTemplateController implements Serializable {

    /**
     * Creates a new instance of WebTemplateController
     */
    public WebTemplateController() {
    }
    
    public String navigateToListWebTemplates(){
        
        return "/webcontent/templates";
    }
    
    public String navigateToAddNewTemplate(){
        return "/webcontent/template";
    }
    
}
