
package com.divudi.core.facade;

import com.divudi.core.entity.TriggerSubscription;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author L C J Samarasekara
 */

@Stateless
public class TriggerSubscriptionFacade extends AbstractFacade<TriggerSubscription> {
    @PersistenceContext(unitName = "hmisPU")
     private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

     public TriggerSubscriptionFacade() {
        super(TriggerSubscription.class);
    }
}
