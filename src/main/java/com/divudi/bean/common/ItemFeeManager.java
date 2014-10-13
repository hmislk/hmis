/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author pasan
 */
@Named
@SessionScoped
public class ItemFeeManager implements Serializable {

    /**
     * Creates a new instance of ItemFeeManager
     */
    public ItemFeeManager() {
    }

    Item item;
    ItemFee itemFee;
    List<ItemFee> itemFees;

    @EJB
    ItemFeeFacade itemFeeFacade;
    @EJB
    ItemFacade itemFacade;
    @EJB
    DepartmentFacade departmentFacade;
    
    @Inject
    SessionController sessionController;
    
    List<Department> departments;

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }
    
    
    
    public void fillDepartments(){
        System.out.println("fill dept");
        String jpql;
        Map m = new HashMap();
        m.put("ins", getItemFee().getInstitution());
        jpql = "select d from Department d where d.retired=false and d.institution=:ins order by d.name";
        System.out.println("m = " + m);
        System.out.println("jpql = " + jpql);
        departments = departmentFacade.findBySQL(jpql, m);
    }
    
    public List<Department> compelteDepartments(String qry){
         String jpql;
         if(qry==null){
             return new ArrayList<>();
         }
        Map m = new HashMap();
        m.put("ins", getItemFee().getInstitution());
        m.put("name", "%" + qry.toUpperCase() + "%" );
        jpql = "select d from Department d where d.retired=false and d.institution=:ins and d.name like :name order by d.name";
        return departmentFacade.findBySQL(jpql, m);
    }
    
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemFee getItemFee() {
        if (itemFee == null) {
            itemFee = new ItemFee();
        }
        return itemFee;
    }

    public void setItemFee(ItemFee itemFee) {
        this.itemFee = itemFee;
    }

    public List<ItemFee> getItemFees() {
        return itemFees;
    }

    public void setItemFees(List<ItemFee> itemFees) {
        this.itemFees = itemFees;
    }

    public void fillFees() {
        String jpql;
        Map m = new HashMap();
        jpql = "select f from ItemFee f where f.retired=false and f.item=:i";
        m.put("i", item);
        itemFees = itemFeeFacade.findBySQL(jpql, m);
    }
    
    public void addNewFee(){
        if(item == null){
            JsfUtil.addErrorMessage("Select Item ?");
            return;
        }
        getItemFee().setCreatedAt(new Date());
        getItemFee().setCreater(sessionController.getLoggedUser());
        itemFeeFacade.create(itemFee);
        
        getItemFee().setItem(item);
        item.getItemFees().add(itemFee);
        itemFeeFacade.edit(itemFee);
        
        JsfUtil.addSuccessMessage("New Fee Added");
        fillFees();
        updateTotal();
        itemFee = new ItemFee();
    }

    public void updateFee(ItemFee f){
        System.out.println("f = " + f);
        System.out.println("f.getFee() = " + f.getFee());
        itemFeeFacade.edit(f);
        updateTotal();
    }
    
    
    public void updateTotal(){
        if(item==null){
            return;
        }
        double t = 0.0;
        for(ItemFee f:itemFees){
            t+=f.getFee();
        }
        getItem().setTotal(t);
        itemFacade.edit(item);
    }
}
