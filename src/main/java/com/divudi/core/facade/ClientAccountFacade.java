package com.divudi.core.facade;

import com.divudi.core.entity.ClientAccount;
import com.divudi.core.entity.Person;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

@Stateless
public class ClientAccountFacade extends AbstractFacade<ClientAccount> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ClientAccountFacade() {
        super(ClientAccount.class);
    }

    public ClientAccount findByPerson(Long personId) {
        String jpql = "select c from ClientAccount c where c.retired=false and c.person.id=:personId";
        Map<String, Object> params = new HashMap<>();
        params.put("personId", personId);
        List<ClientAccount> results = findByJpql(jpql, params);
        if (results == null || results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    /**
     * Locks the Person row for the duration of the check-then-create so two
     * concurrent registration attempts for the same person can't both
     * succeed (design spec §3 concurrency note, option (a)). Returns null if
     * an active ClientAccount already exists for this person.
     */
    public ClientAccount createIfNoActiveAccount(Long personId, ClientAccount newAccount) {
        getEntityManager().find(Person.class, personId, LockModeType.PESSIMISTIC_WRITE);
        ClientAccount existing = findByPerson(personId);
        if (existing != null) {
            return null;
        }
        create(newAccount);
        return newAccount;
    }

    /**
     * Returns all active ClientAccounts whose verifiedPhone or verifiedEmail
     * exactly matches the given value. More than one account can match a
     * shared household phone number; callers must disambiguate via password
     * (login) or explicit selection (password reset).
     */
    public List<ClientAccount> findByVerifiedPhoneOrEmail(String phoneOrEmail) {
        String jpql = "select c from ClientAccount c where c.retired=false and (c.verifiedPhone=:v or c.verifiedEmail=:v)";
        Map<String, Object> params = new HashMap<>();
        params.put("v", phoneOrEmail);
        return findByJpql(jpql, params);
    }
}
