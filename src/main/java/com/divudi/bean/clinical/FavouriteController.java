/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.clinical;

import com.divudi.bean.common.SessionController;
import com.divudi.data.clinical.ItemUsageType;
import com.divudi.entity.Item;
import com.divudi.entity.clinical.ItemUsage;
import com.divudi.facade.ItemUsageFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author buddhika
 */
@Named
@SessionScoped
public class FavouriteController implements Serializable {

    /**
     * EJBs
     */
    @EJB
    ItemUsageFacade favouriteItemFacade;
    /**
     * Controllers
     */
    @Inject
    SessionController sessionController;
    /**
     * Properties
     */
    Item item;
    ItemUsage current;
    ItemUsage selected;
    List<ItemUsage> items;

    /**
     * Methods
     */
    
    public void fillFavouriteMedicines(){
        fillFavouriteItems(ItemUsageType.FavoutireMedicine);
    }
    
    public void fillFavouriteDisgnosis(){
        fillFavouriteItems(ItemUsageType.FavouriteDiagnosis);
    }
    
    /**
     * 
     * @param type 
     */
    public void fillFavouriteItems(ItemUsageType type) {
        String j;
        Map m = new HashMap();
        j = "select i "
                + " from FavouriteItem i "
                + " where i.retired=false "
                + " and i.forItem=:item "
                + " and i.forWebUser=:wu ";

        if (type != null) {
            m.put("t", type);
            j += " and i.type=:t ";
        }

        j += " order by i.orderNo";

        m.put("item", item);
        m.put("wu", sessionController.getLoggedUser());
        items = favouriteItemFacade.findBySQL(j, m);
    }

    public void prepareAddingFavouriteItem(){
        current = new ItemUsage();
    }
    
    public void addToFavouriteMedicine() {
        if (item == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }
        if (current == null) {
            JsfUtil.addErrorMessage("Favourite Item is not create by getter. Please contact vendor.");
            return;
        }
        current.setType(ItemUsageType.FavoutireMedicine);
        current.setForItem(item);
        current.setForWebUser(sessionController.getLoggedUser());
        favouriteItemFacade.create(current);
        current=null;
        fillFavouriteItems(ItemUsageType.FavoutireMedicine);
        JsfUtil.addSuccessMessage("Saved");

    }

    public void updateSelected(){
        updateSelected(selected);
    }
    
    public void updateSelected(ItemUsage updatingItem){
        if(updatingItem!=null){
            favouriteItemFacade.edit(updatingItem);
            JsfUtil.addSuccessMessage("Updated");
        }
    }
    
    
    /**
     * Creates a new instance of FavouriteController
     */
    public FavouriteController() {
    }

    /**
     * Getters And Setters
     */
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<ItemUsage> getItems() {
        return items;
    }

    public void setItems(List<ItemUsage> items) {
        this.items = items;
    }

    public ItemUsage getCurrent() {
        if (current == null) {
            current = new ItemUsage();
            current.setItem(item);
        }
        return current;
    }

    public void setCurrent(ItemUsage current) {
        this.current = current;
    }

}
