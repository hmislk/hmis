package com.divudi.service;

import com.divudi.core.entity.Request;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.RequestFacade;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

@Stateless
public class RequestService {

    @EJB
    RequestFacade requestFacade;

    public void save(Request req) {
        save(req, null);
    }

    public void save(Request req, WebUser user) {
        if (req == null) {
            return;
        }
        if (req.getId() == null) {
                req.setCreater(user);
                req.setCreatedAt(new Date());
            
            requestFacade.create(req);
        } else {
            requestFacade.edit(req);
        }
    }

}
