/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.facade;

import com.divudi.core.entity.Consultant;
import com.divudi.core.entity.Person;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author buddhika
 */
@Stateless
public class ConsultantFacade extends AbstractFacade<Consultant> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        if(em == null){}return em;
    }

    public ConsultantFacade() {
        super(Consultant.class);
    }

    /**
     * Persists a new Person and its associated Consultant in a single EJB transaction.
     * If either write fails the whole transaction rolls back, preventing orphaned Person records.
     */
    public void createConsultantWithPerson(Person person, Consultant consultant) {
        em.persist(person);
        consultant.setPerson(person);
        em.persist(consultant);
    }

    /**
     * Merges an existing Person and its associated Consultant in a single EJB transaction.
     */
    public void updateConsultantWithPerson(Person person, Consultant consultant) {
        em.merge(person);
        em.merge(consultant);
    }

}
