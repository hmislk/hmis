package com.divudi.service;

import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.DrawerEntry;
import com.divudi.facade.DrawerEntryFacade;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Stateless
public class DrawerEntryService {

    @EJB
    DrawerEntryFacade ejbFacade;
    
    public void save(DrawerEntry drawerEntry) {
        save(drawerEntry,null);
    }

    public void save(DrawerEntry drawerEntry, WebUser user) {
        if (drawerEntry == null) {
            return;
        }
        if(drawerEntry.getId()==null){
            if(drawerEntry.getCreater()==null){
                drawerEntry.setCreater(user);
            }
            if(drawerEntry.getCreatedAt()==null){
                drawerEntry.setCreatedAt(new Date());
            }
            ejbFacade.create(drawerEntry);
        }else{
            ejbFacade.edit(drawerEntry);
        }
    }

}
