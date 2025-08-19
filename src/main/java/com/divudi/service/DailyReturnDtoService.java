package com.divudi.service;

import com.divudi.core.data.dto.DailyReturnDTO;
import com.divudi.core.data.dto.DailyReturnItemDTO;
import com.divudi.core.facade.BillFacade;
import java.util.*;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * Service executing direct DTO queries for Daily Return reports.
 */
@Stateless
public class DailyReturnDtoService {

    @EJB
    private BillFacade billFacade;

    public List<DailyReturnDTO> fetchDailyReturnByPaymentMethod(Date fromDate, Date toDate) {
        String jpql = "select new com.divudi.core.data.dto.DailyReturnDTO(b.paymentMethod, sum(b.netTotal)) "
                + "from Bill b "
                + "where b.retired=false "
                + "and b.createdAt between :fd and :td "
                + "group by b.paymentMethod";
        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        return (List<DailyReturnDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public List<DailyReturnItemDTO> fetchDailyReturnItems(Date fromDate, Date toDate) {
        String jpql = "select new com.divudi.core.data.dto.DailyReturnItemDTO(b.department.name, sum(b.netTotal)) "
                + "from Bill b "
                + "where b.retired=false "
                + "and b.createdAt between :fd and :td "
                + "group by b.department.name";
        Map<String, Object> params = new HashMap<>();
        params.put("fd", fromDate);
        params.put("td", toDate);
        return (List<DailyReturnItemDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }
}
