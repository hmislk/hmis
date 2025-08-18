/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.facade;

import com.divudi.core.entity.ConfigOption;
import java.util.Map;
import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Sniper 619
 */
@Stateless
@PermitAll
public class ConfigOptionFacade extends AbstractFacade<ConfigOption> {
    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ConfigOptionFacade() {
        super(ConfigOption.class);
    }

    @PermitAll
    public ConfigOption findFirstByJpqlWithLock(String jpql, Map<String, Object> params) {
        javax.persistence.TypedQuery<ConfigOption> qry = getEntityManager().createQuery(jpql, ConfigOption.class);
        for (Map.Entry<String, Object> e : params.entrySet()) {
            qry.setParameter(e.getKey(), e.getValue());
        }
        qry.setMaxResults(1);
        qry.setLockMode(javax.persistence.LockModeType.PESSIMISTIC_WRITE);
        try {
            return qry.getSingleResult();
        } catch (javax.persistence.NoResultException ex) {
            return null;
        }
    }

    @PermitAll
    public synchronized ConfigOption createOptionIfNotExists(String key, com.divudi.core.data.OptionScope scope, 
            com.divudi.core.entity.Institution institution, com.divudi.core.entity.Department department, 
            com.divudi.core.entity.WebUser webUser, com.divudi.core.data.OptionValueType valueType, String value) {
        
        StringBuilder jpql = new StringBuilder("SELECT o FROM ConfigOption o WHERE o.retired=false AND o.optionKey=:key AND o.scope=:scope");
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("key", key);
        params.put("scope", scope);
        
        if (institution != null) {
            jpql.append(" AND o.institution = :institution");
            params.put("institution", institution);
        } else {
            jpql.append(" AND o.institution IS NULL");
        }
        if (department != null) {
            jpql.append(" AND o.department = :department");
            params.put("department", department);
        } else {
            jpql.append(" AND o.department IS NULL");
        }
        if (webUser != null) {
            jpql.append(" AND o.webUser = :webUser");
            params.put("webUser", webUser);
        } else {
            jpql.append(" AND o.webUser IS NULL");
        }
        
        ConfigOption existing = findFirstByJpql(jpql.toString(), params);
        if (existing != null) {
            return existing;
        }
        
        ConfigOption newOption = new ConfigOption();
        newOption.setCreatedAt(new java.util.Date());
        newOption.setOptionKey(key);
        newOption.setScope(scope);
        newOption.setInstitution(institution);
        newOption.setDepartment(department);
        newOption.setWebUser(webUser);
        newOption.setValueType(valueType);
        newOption.setOptionValue(value);
        
        try {
            create(newOption);
            return newOption;
        } catch (Exception e) {
            existing = findFirstByJpql(jpql.toString(), params);
            if (existing != null) {
                return existing;
            }
            throw e;
        }
    }

}
