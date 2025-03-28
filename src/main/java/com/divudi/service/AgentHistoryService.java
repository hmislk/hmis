package com.divudi.service;

import com.divudi.core.entity.AgentHistory;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.AgentHistoryFacade;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author M H B Ariyaratne
 *
 */
@Stateless
public class AgentHistoryService {

    @EJB
    AgentHistoryFacade agentHistoryFacade;

    public void save(AgentHistory ahx) {
        save(ahx, null);
    }

    public void save(AgentHistory ahx, WebUser user) {
        if (ahx == null) {
            return;
        }
        if (ahx.getId() == null) {
            if (ahx.getCreater() == null) {
                ahx.setCreater(user);
            }
            if (ahx.getCreatedAt() == null) {
                ahx.setCreatedAt(new Date());
            }
            agentHistoryFacade.create(ahx);
        } else {
            agentHistoryFacade.edit(ahx);
        }
    }

}
