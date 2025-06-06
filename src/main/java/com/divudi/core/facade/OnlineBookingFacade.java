
package com.divudi.core.facade;

import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.OnlineBooking;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Chinthaka Prasad
 */
@Stateless
public class OnlineBookingFacade extends AbstractFacade<OnlineBooking>{
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public OnlineBookingFacade() {
        super(OnlineBooking.class);
    }
}
