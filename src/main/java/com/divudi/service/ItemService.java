package com.divudi.service;

import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.facade.ItemFacade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Stateless
public class ItemService {

    @EJB
    ItemFacade itemFacade;

    public List<Item> fetchAmps() {
        List<Item> amps;
        String jpql;
        HashMap params = new HashMap();
        jpql = "select c "
                + " from Item c "
                + " where c.retired=:ret "
                + " and type(c)= :amp "
                + " order by c.name";
        params.put("amp", Amp.class);
        params.put("ret", false);
        amps = itemFacade.findByJpql(jpql, params);
        return amps;
    }

}
