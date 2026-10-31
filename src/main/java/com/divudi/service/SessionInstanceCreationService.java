package com.divudi.service;

import com.divudi.core.entity.ServiceSession;
import com.divudi.core.entity.channel.SessionInstance;
import com.divudi.core.facade.SessionInstanceFacade;
import com.divudi.ejb.ChannelBean;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.TemporalType;

/**
 * Finds or creates the SessionInstance of a ServiceSession for one date in its
 * own transaction (issue #24972).
 *
 * Callers hold SessionInstanceCreationLock for the ServiceSession around the
 * call. The new row is committed before the method returns, so the next lock
 * holder finds it instead of creating a second instance.
 */
@Stateless
public class SessionInstanceCreationService {

    @EJB
    private SessionInstanceFacade sessionInstanceFacade;
    @EJB
    private ChannelBean channelBean;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public SessionInstance findOrCreateInNewTransaction(ServiceSession ss, Date sessionDate) {
        String jpql = "select i "
                + " from SessionInstance i "
                + " where i.originatingSession=:os "
                + " and i.retired=:ret "
                + " and i.sessionDate=:sd";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("os", ss);
        m.put("sd", sessionDate);
        SessionInstance si = sessionInstanceFacade.findFirstByJpql(jpql, m, TemporalType.DATE);
        if (si != null) {
            return si;
        }
        si = channelBean.createSessionInstancesForServiceSession(ss, sessionDate);
        if (si.getId() == null) {
            sessionInstanceFacade.create(si);
        } else {
            sessionInstanceFacade.edit(si);
        }
        return si;
    }
}
