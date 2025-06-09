/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.util.JsfUtil;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Buddhika
 */
@Stateless
public class PatientRoomFacade extends AbstractFacade<PatientRoom> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){
            JsfUtil.addErrorMessage("null em");
        }
        if(em == null){}return em;
    }

    public PatientRoomFacade() {
        super(PatientRoom.class);
    }

}
