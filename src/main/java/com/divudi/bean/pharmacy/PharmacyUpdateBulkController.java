/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.UtilityController;
import com.divudi.data.DepartmentType;
import com.divudi.entity.Category;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.facade.AmpFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Sniper
 */
@Named("pharmacyUpdateBulkController")
@SessionScoped
public class PharmacyUpdateBulkController implements Serializable {
    
    @EJB
    AmpFacade ampFacade;
    List<Amp> amps;
    Category category;
    Category updateCategory;

    public PharmacyUpdateBulkController() {
    }
    
    public void calerAll(){
        amps=new ArrayList<>();
        category=null;
        updateCategory=null;
    }

    public void fillPharmacyItems() {
        
        if (category==null) {
            UtilityController.addErrorMessage("Select Category....");
            return;
        }
        
        String sql;
        Map m = new HashMap();
        
        sql = "select c from Amp c where "
                + " c.retired=false and"
                + " (c.departmentType is null"
                + " or c.departmentType!=:dep) "
                + " and c.category=:cat "
                + " order by c.name ";

        m.put("dep", DepartmentType.Store);
        m.put("cat", category);
        
        amps = getAmpFacade().findBySQL(sql, m);
    }
    
    public void fillPharmacyDiscountDisAllowedItems() {
        
        String sql;
        Map m = new HashMap();
        
        sql = "select c from Amp c where "
                + " c.retired=false "
                + " and (c.departmentType is null or c.departmentType!=:dep) "
                + " and c.discountAllowed=false "
                + " order by c.name ";

        m.put("dep", DepartmentType.Store);
        
        amps = getAmpFacade().findBySQL(sql, m);
    }
    
    public void updatePharmacyItemCategory(){
        if(amps==null || amps.isEmpty()){
            UtilityController.addErrorMessage("Nothing To Update.");
            return;
        }
        
        if(updateCategory==null){
            UtilityController.addErrorMessage("Select Update Category...");
            return;
        }
        
        if(category.equals(updateCategory)){
            UtilityController.addErrorMessage("Nothing To Update.Same Category...");
            return;
        }
        
        //System.out.println("Size = " + amps.size());
        
        for (Amp a : amps) {
            //System.out.println("**********************************");
            //System.out.println("Name = " + a.getName());
            //System.out.println("Categery Name before = " + a.getCategory().getName());
            a.setCategory(updateCategory);
            getAmpFacade().edit(a);
            //System.out.println("Categery Name After = " + a.getCategory().getName());
            //System.out.println("**********************************");
        }
        
        UtilityController.addSuccessMessage("Updated...");
        
    }
    
    public void updatePharmacyItemDiscountAllowed(){
        if(amps==null || amps.isEmpty()){
            UtilityController.addErrorMessage("Nothing To Update.");
            return;
        }
        
        //System.out.println("Size = " + amps.size());
        
        for (Amp a : amps) {
            //System.out.println("**********************************");
            //System.out.println("Name = " + a.getName());
            //System.out.println("Discount Allowd before = " + a.getDiscountAllowed());
            a.setDiscountAllowed(Boolean.TRUE);
            getAmpFacade().edit(a);
            //System.out.println("Discount Allowd After = " + a.getDiscountAllowed());
            //System.out.println("**********************************");
        }
        
        UtilityController.addSuccessMessage("Updated...");
        
    }

    public AmpFacade getAmpFacade() {
        return ampFacade;
    }

    public void setAmpFacade(AmpFacade ampFacade) {
        this.ampFacade = ampFacade;
    }

    public List<Amp> getAmps() {
        return amps;
    }

    public void setAmps(List<Amp> amps) {
        this.amps = amps;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Category getUpdateCategory() {
        return updateCategory;
    }

    public void setUpdateCategory(Category updateCategory) {
        this.updateCategory = updateCategory;
    }
    
}
