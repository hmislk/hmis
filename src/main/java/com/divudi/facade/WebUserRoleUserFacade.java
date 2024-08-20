
package com.divudi.facade;

import com.divudi.entity.WebUserRoleUser;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author L C J Samarasekara
 */

@Stateless
public class WebUserRoleUserFacade extends AbstractFacade<WebUserRoleUser> {
    @PersistenceContext(unitName = "hmisPU")
     private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }
    
     public WebUserRoleUserFacade() {
        super(WebUserRoleUser.class);
    }
}
