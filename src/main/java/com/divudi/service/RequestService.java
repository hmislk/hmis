package com.divudi.service;

import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.RequestStatus;
import com.divudi.core.data.RequestType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Request;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.RequestFacade;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

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
            req.setCreator(user);
            req.setCreatedAt(new Date());

            requestFacade.create(req);
        } else {
            requestFacade.edit(req);
        }
    }

    public Request findRequest(Bill bill) {
        if (bill == null) {
            return null;
        }

        List<RequestStatus> availableStatus = new ArrayList<>();
        availableStatus.add(RequestStatus.PENDING);
        availableStatus.add(RequestStatus.UNDER_REVIEW);
        availableStatus.add(RequestStatus.APPROVED);

        String jpql = "Select q from Request q "
                + " where q.retired =:ret "
                + " and q.bill =:bill "
                + " and q.status in :status ";

        HashMap params = new HashMap();
        params.put("ret", false);
        params.put("bill", bill);
        params.put("status", availableStatus);

        Request req = requestFacade.findFirstByJpql(jpql, params);

        return req;
    }

    public List<Request> fillAllRequest(
            Date fromDate,
            Date toDate,
            String billNo,
            String bhtNo,
            String requestNo,
            RequestType requestType,
            RequestStatus requestStatus,
            DepartmentType departmentType) {
        
        HashMap params = new HashMap();

        String jpql = "Select q from Request q "
                + " where q.retired =:ret "
                + " and q.createdAt between :frm and :to"
                + " and q.department.departmentType =:deptType";

        if (requestNo != null && !requestNo.trim().equals("")) {
            jpql += " and q.requestNo like :reqNo ";
            params.put("reqNo", "%" + requestNo.trim() + "%");
        }

        if (billNo != null && !billNo.trim().equals("")) {
            jpql += " and  q.bill.deptId like :billNo ";
            params.put("billNo", "%" + billNo.trim() + "%");
        }

        if (bhtNo != null && !bhtNo.trim().equals("")) {
            jpql += " and  q.bill.patientEncounter.bhtNo like :bht ";
            params.put("bht", "%" + bhtNo.trim() + "%");
        }

        if (requestStatus != null) {
            jpql += " and q.status = :status ";
            params.put("status", requestStatus);
        }

        if (requestType != null) {
            jpql += " and q.requestType = :type ";
            params.put("type", requestType);
        }

        jpql += " order by q.id desc ";

        params.put("deptType", departmentType);
        params.put("ret", false);
        params.put("frm", fromDate);
        params.put("to", toDate);

        List<Request> req = requestFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        System.out.println("req = " + req);

        return req;
    }

}
