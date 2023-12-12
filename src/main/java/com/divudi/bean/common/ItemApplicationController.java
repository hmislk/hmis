package com.divudi.bean.common;

import com.divudi.entity.Item;
import com.divudi.entity.Packege;
import com.divudi.entity.Service;
import com.divudi.entity.lab.Investigation;
import com.divudi.facade.ItemFacade;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@ApplicationScoped
public class ItemApplicationController {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    ItemFacade itemFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    private List<Item> items;
    private List<Item> investigationsAndServices;
    private List<Investigation> investigations;
    private List<Service> services;
    private List<Packege> packages;
    // </editor-fold>

    /**
     * Creates a new instance of ItemApplicationController
     */
    public ItemApplicationController() {
    }

    // <editor-fold defaultstate="collapsed" desc="Functions">
    public List<Item> fillAllItems() {
        String jpql = "select i "
                + " from Item i "
                + " where i.retired=:ret "
                + " order by i.name";
        Map m = new HashMap();
        m.put("ret", false);
        return itemFacade.findByJpql(jpql, m);
    }

    private <T extends Item> List<T> fillItems(Class<T> cls) {
        List<T> filteredItems = new ArrayList<>();
        for (Item item : getItems()) {
            if (cls.isInstance(item)) {
                filteredItems.add(cls.cast(item));
            }
        }
        return filteredItems;
    }

    private <T extends Item> List<T> fillItems(Collection<Class<? extends T>> classes) {
        List<T> filteredItems = new ArrayList<>();
        for (Item item : getItems()) {
            for (Class<? extends T> cls : classes) {
                if (cls.isInstance(item)) {
                    filteredItems.add(cls.cast(item));
                    break; // Breaks the inner loop once a match is found
                }
            }
        }
        return filteredItems;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Other">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public List<Item> getItems() {
        if (items == null) {
            items = fillAllItems();
        }
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<Investigation> getInvestigations() {
        if (investigations == null) {
            investigations = fillItems(Investigation.class);
        }
        return investigations;
    }

    public List<Service> getServices() {
        if (services == null) {
            services = fillItems(Service.class);
        }
        return services;
    }

    
    
    public List<Packege> getPackages() {
        if (packages == null) {
            packages = fillItems(Packege.class);
        }
        return packages;
    }
    
   
    

    // </editor-fold>

    public List<Item> getInvestigationsAndServices() {
        if(investigationsAndServices==null){
            investigationsAndServices = fillItems(Arrays.asList(Investigation.class, Service.class));
        }
        return investigationsAndServices;
    }

   
}
