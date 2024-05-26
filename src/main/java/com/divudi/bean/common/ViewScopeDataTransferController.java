/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.entity.BillSession;
import com.divudi.entity.channel.SessionInstance;
import java.io.Serializable;
import java.util.Date;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
/**
 *
 * @author divudi_lk
 */
@Named
@SessionScoped
public class ViewScopeDataTransferController  implements Serializable {
    
    private String previousPage;
    private int manageFeeIndex=-1;
    
    private Date fromDate;
    private Date toDate;
    private Boolean needToFillBillSessions;
    private Boolean needToFillBillSessionDetails;
    private String sessionInstanceFilter;
    private BillSession selectedBillSession;
    private SessionInstance selectedSessionInstance;
       
        

    /**
     * Creates a new instance of ViewController
     */
    public ViewScopeDataTransferController() {
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

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Boolean getNeedToFillBillSessions() {
        return needToFillBillSessions;
    }

    public void setNeedToFillBillSessions(Boolean needToFillBillSessions) {
        this.needToFillBillSessions = needToFillBillSessions;
    }

    public Boolean getNeedToFillBillSessionDetails() {
        return needToFillBillSessionDetails;
    }

    public void setNeedToFillBillSessionDetails(Boolean needToFillBillSessionDetails) {
        this.needToFillBillSessionDetails = needToFillBillSessionDetails;
    }

    public String getSessionInstanceFilter() {
        return sessionInstanceFilter;
    }

    public void setSessionInstanceFilter(String sessionInstanceFilter) {
        this.sessionInstanceFilter = sessionInstanceFilter;
    }

    public BillSession getSelectedBillSession() {
        return selectedBillSession;
    }

    public void setSelectedBillSession(BillSession selectedBillSession) {
        this.selectedBillSession = selectedBillSession;
    }

    public SessionInstance getSelectedSessionInstance() {
        return selectedSessionInstance;
    }

    public void setSelectedSessionInstance(SessionInstance selectedSessionInstance) {
        this.selectedSessionInstance = selectedSessionInstance;
    }
    
    
    
    
}
