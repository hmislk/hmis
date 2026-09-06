/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.Institution;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author buddhika
 */
@Stateless
public class InstitutionFacade extends AbstractFacade<Institution> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public InstitutionFacade() {
        super(Institution.class);
    }

    public Institution findDefaultInstitution() {
        Institution flagged = findFirstByJpql(
            "select i from Institution i where i.defaultInstitution=true and i.retired=false order by i.id asc");
        if (flagged != null) {
            return flagged;
        }
        return findFirstByJpql(
            "select i from Institution i where i.retired=false order by i.id asc");
    }

    public void clearDefaultInstitutionExceptFor(Long id) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        List<Institution> others = findByJpql(
            "select i from Institution i where i.defaultInstitution=true and i.id<>:id", params);
        for (Institution other : others) {
            other.setDefaultInstitution(false);
            edit(other);
        }
    }

}
