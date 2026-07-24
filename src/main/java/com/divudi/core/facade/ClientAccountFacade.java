package com.divudi.core.facade;

import com.divudi.core.entity.ClientAccount;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
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
}
