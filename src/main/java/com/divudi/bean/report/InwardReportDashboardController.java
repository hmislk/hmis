package com.divudi.bean.report;


import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 * @author Ishira Pahasara
 */
@Named
@SessionScoped
public class InwardReportDashboardController implements Serializable{

    private String reportTemplateFileIndexName;
    private int activeIndex = 0;
    public void InwardReportDashboardController(){
       
    }
    
    
    // Bed Occupency Dashboard Navigation
    public String navigateToBedOccupencyDashboard (){
      activeIndex=1;
      setReportTemplateFileIndexName("/reports/index.xhtml");
      return "/reports/dashboard/bedOccupencyDashboard?faces-redirect=true";
    }
    
    // OPD Revenue Dashboard Navigation
    public String navigateToopdRevenueDashboard (){
      activeIndex=2;
      setReportTemplateFileIndexName("/reports/index.xhtml");
      return "/reports/dashboard/opdRevenueDashboard?faces-redirect=true";
    }
    
    // Discount Dashboard Navigation
    public String navigateToDiscountDashboard (){
      activeIndex=3;
      setReportTemplateFileIndexName("/reports/index.xhtml");
      return "/reports/dashboard/discountDashboard?faces-redirect=true";
    }
    
    // HR Dashboard Navigation
    public String navigateToHRDashboard (){
      activeIndex=4;
      setReportTemplateFileIndexName("/reports/index.xhtml");
      return "/reports/dashboard/hrDashboard?faces-redirect=true";
    }   
    
    
    
    

    private void setReportTemplateFileIndexName(String reportTemplateFileIndexName) {
        this.reportTemplateFileIndexName = reportTemplateFileIndexName;
    }
    
    public int getActiveIndex() {
       return activeIndex ;
    }
    
    public void setActiveIndex(int activeIndex){
      this.activeIndex = activeIndex;
    }
    

   
}
