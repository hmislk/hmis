package com.divudi.service;

import com.divudi.core.data.RequestStatus;
import com.divudi.core.data.RequestType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Request;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.RequestFacade;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

@Stateless
public class RequestService {

    @EJB
    RequestFacade requestFacade;

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
    
    public Request findRequest(Bill bill) {
        System.out.println("RequestService = " + "findRequest");
        String jpql;
        HashMap params = new HashMap();
        
        jpql = "Select q from Request q "
                + " where q.retired =:ret "
                + " and q.bill =:bill "
                + " and q.status =:status ";
        params.put("ret",false);
        params.put("bill",bill);
        params.put("status",RequestStatus.PENDING);
        
        Request req = requestFacade.findFirstByJpql(jpql, params);
        System.out.println("req = " + req);
        return req;
    }

}
