/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.webservice;

import java.util.Set;
import javax.ws.rs.core.Application;

/**
 *
 * @author safrin
 */
@javax.ws.rs.ApplicationPath("webresources")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        // following code can be used to customize Jersey 1.x JSON provider:
        try {
            Class jacksonProvider = Class.forName("org.codehaus.jackson.jaxrs.JacksonJsonProvider");
            resources.add(jacksonProvider);
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        addRestResourceClasses(resources);
        return resources;
    }

    /**
     * Do not modify addRestResourceClasses() method.
     * It is automatically populated with
     * all resources defined in the project.
     * If required, comment out calling this method in getClasses().
     */
    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(com.divudi.webservice.AmendmentFormFacadeREST.class);
        resources.add(com.divudi.webservice.AreaFacadeREST.class);
        resources.add(com.divudi.webservice.BankAccountFacadeREST.class);
        resources.add(com.divudi.webservice.BasicSalaryFacadeREST.class);
        resources.add(com.divudi.webservice.FingerPrintRecordFacadeREST.class);
        resources.add(com.divudi.webservice.FormFacadeREST.class);
        resources.add(com.divudi.webservice.GradeFacadeREST.class);
        resources.add(com.divudi.webservice.HrmVariablesFacadeREST.class);
        resources.add(com.divudi.webservice.LeaveFormFacadeREST.class);
        resources.add(com.divudi.webservice.LoanFacadeREST.class);
        resources.add(com.divudi.webservice.MembershipSchemeFacadeREST.class);
        resources.add(com.divudi.webservice.PaysheetComponentFacadeREST.class);
        resources.add(com.divudi.webservice.PhDateFacadeREST.class);
        resources.add(com.divudi.webservice.SalaryCycleFacadeREST.class);
        resources.add(com.divudi.webservice.SalaryHoldFacadeREST.class);
        resources.add(com.divudi.webservice.ShiftFacadeREST.class);
        resources.add(com.divudi.webservice.ShiftPreferenceFacadeREST.class);
        resources.add(com.divudi.webservice.StaffBasicsFacadeREST.class);
        resources.add(com.divudi.webservice.StaffCategoryFacadeREST.class);
        resources.add(com.divudi.webservice.StaffEmployeeStatusFacadeREST.class);
        resources.add(com.divudi.webservice.StaffEmploymentFacadeREST.class);
        resources.add(com.divudi.webservice.StaffGradeFacadeREST.class);
        resources.add(com.divudi.webservice.StaffLeaveEntitleFacadeREST.class);
        resources.add(com.divudi.webservice.StaffLeaveFacadeREST.class);
        resources.add(com.divudi.webservice.StaffPaysheetComponentFacadeREST.class);
        resources.add(com.divudi.webservice.StaffSalaryComponantFacadeREST.class);
        resources.add(com.divudi.webservice.StaffSalaryFacadeREST.class);
        resources.add(com.divudi.webservice.StaffShiftExtraFacadeREST.class);
        resources.add(com.divudi.webservice.StaffShiftFacadeREST.class);
        resources.add(com.divudi.webservice.StaffShiftHistoryFacadeREST.class);
        resources.add(com.divudi.webservice.StaffShiftReplaceFacadeREST.class);
        resources.add(com.divudi.webservice.StaffStaffCategoryFacadeREST.class);
        resources.add(com.divudi.webservice.StaffWorkDayFacadeREST.class);
        resources.add(com.divudi.webservice.StaffWorkingDepartmentFacadeREST.class);
        resources.add(com.divudi.webservice.WebUserFacadeREST.class);
        resources.add(com.divudi.webservice.WebUserRoleFacadeREST.class);
        resources.add(com.divudi.webservice.WorkingTimeFacadeREST.class);
    }
    
}
