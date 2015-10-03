/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.clinical;

import com.divudi.bean.common.SessionController;
import com.divudi.data.clinical.FavouriteItemType;
import com.divudi.entity.Item;
import com.divudi.entity.clinical.FavouriteItem;
import com.divudi.facade.FavouriteItemFacade;
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
@Named(value = "favouriteController")
@SessionScoped
public class FavouriteController implements Serializable {

    /**
     * EJBs
     */
    @EJB
    FavouriteItemFacade favouriteItemFacade;
    /**
     * Controllers
     */
    @Inject
    SessionController sessionController;
    /**
     * Properties
     */
    Item item;
    FavouriteItem current;
    FavouriteItem selected;
    List<FavouriteItem> items;

    /**
     * Methods
     */
    
    public void fillFavouriteMedicines(){
        fillFavouriteItems(FavouriteItemType.FavoutireMedicine);
    }
    
    public void fillFavouriteDisgnosis(){
        fillFavouriteItems(FavouriteItemType.FavouriteDiagnosis);
    }
    
    /**
     * 
     * @param type 
     */
    public void fillFavouriteItems(FavouriteItemType type) {
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

    public void addToFavouriteMedicine() {
        if (item == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }
        if (current == null) {
            JsfUtil.addErrorMessage("Favourite Item is not create by getter. Please contact vendor.");
            return;
        }
        current.setType(FavouriteItemType.FavoutireMedicine);
        current.setForItem(item);
        current.setForWebUser(sessionController.getLoggedUser());
        favouriteItemFacade.create(current);
        current=null;
        fillFavouriteItems(FavouriteItemType.FavoutireMedicine);
        JsfUtil.addSuccessMessage("Saved");

    }

    public void updateSelected(){
        updateSelected(selected);
    }
    
    public void updateSelected(FavouriteItem updatingItem){
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

    public List<FavouriteItem> getItems() {
        return items;
    }

    public void setItems(List<FavouriteItem> items) {
        this.items = items;
    }

    public FavouriteItem getCurrent() {
        if (current == null) {
            current = new FavouriteItem();
            current.setItem(item);
        }
        return current;
    }

    public void setCurrent(FavouriteItem current) {
        this.current = current;
    }

}
