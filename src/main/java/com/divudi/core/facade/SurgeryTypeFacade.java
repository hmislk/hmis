
package com.divudi.core.facade;

import com.divudi.core.entity.inward.SurgeryType;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Dhanesh
 */ 
@Stateless
public class SurgeryTypeFacade extends AbstractFacade<SurgeryType> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public SurgeryTypeFacade() {
        super(SurgeryType.class);
    }

}
