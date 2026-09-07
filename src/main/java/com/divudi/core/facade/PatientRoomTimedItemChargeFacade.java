/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.inward.PatientRoomTimedItemCharge;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class PatientRoomTimedItemChargeFacade extends AbstractFacade<PatientRoomTimedItemCharge> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if (em == null) {
        }
        return em;
    }

    public PatientRoomTimedItemChargeFacade() {
        super(PatientRoomTimedItemCharge.class);
    }
}
