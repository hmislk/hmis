/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.facade;

import com.divudi.data.inward.RoomFacility;
import com.divudi.entity.inward.RoomFacilityCharge;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author Buddhika
 */
@Stateless
public class RoomFacilityChargeFacade extends AbstractFacade<RoomFacilityCharge> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public RoomFacilityChargeFacade() {
        super(RoomFacilityCharge.class);
    }
    
     public RoomFacility[] roomFacilityBySql(String sql) {
        Query q = getEntityManager().createQuery(sql);
        return (RoomFacility[]) q.getSingleResult();
    }
    
}
