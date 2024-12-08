package com.divudi.service;

import com.divudi.entity.Category;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.ItemFee;
import com.divudi.facade.ItemFeeFacade;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author Dr M H B Ariyaratne, buddhika.ari@gmail.com
 *
 */
@Stateless
public class ItemFeeService {

    @EJB
    ItemFeeFacade itemFeeFacade;

    public List<ItemFee> fetchSiteFeesByItem(String itemCode, Institution site) {
        System.out.println("fetchSiteFeesByItem");
        System.out.println("itemCode = " + itemCode);
        System.out.println("site = " + site);
        String jpql = "select f "
                + " from ItemFee f "
                + " where f.retired=:ret ";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);

        jpql += " and f.item.code=:itemCode ";
        m.put("itemCode", itemCode);

        jpql += " and f.forInstitution=:site";
        m.put("site", site);

        System.out.println("jpql = " + jpql);
        System.out.println("m = " + m);
        List<ItemFee> fs = itemFeeFacade.findByJpql(jpql, m);
        return fs;
    }

}
