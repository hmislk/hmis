/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import java.io.Serializable;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
/**
 *
 * @author divudi_lk
 */
@Named
@SessionScoped
public class ViewController  implements Serializable {
    
    private String previousPage;
    private int manageFeeIndex=-1;

    /**
     * Creates a new instance of ViewController
     */
    public ViewController() {
    }

    public String getPreviousPage() {
        return previousPage;
    }

    public void setPreviousPage(String previousPage) {
        this.previousPage = previousPage;
    }
    
    public void makeAdminFeesAsPreviousPage(){
        previousPage = "/admin_fees";
    }
    
    public void makeInvestigationAsPreviousPage(){
        previousPage = "/lab/investigation";
    }
    
    public void makeBulkFeesAsPreviousPage(){
        previousPage = "/dataAdmin/manage_item_fees_bulk";
    }
    
    public String backToPreviousPage(){
        if(previousPage==null||previousPage.trim().equals("")){
            previousPage = "/index";
        }
        return previousPage;
    }

    public int getManageFeeIndex() {
        return manageFeeIndex;
    }

    public void setManageFeeIndex(int manageFeeIndex) {
        this.manageFeeIndex = manageFeeIndex;
    }
    
    
}
