package com.divudi.service;

import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.facade.ItemFacade;

import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;

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
