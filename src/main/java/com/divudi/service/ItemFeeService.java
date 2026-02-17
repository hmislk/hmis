package com.divudi.service;

import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.FeeValue;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.ItemFee;
import com.divudi.core.facade.FeeValueFacade;
import com.divudi.core.facade.ItemFeeFacade;
import java.util.Date;
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
    @EJB
    FeeValueFacade feeValueFacade;

    public List<ItemFee> fetchSiteFeesByItem(String itemCode, Institution site) {
        System.out.println("fetchSiteFeesByItem");
        System.out.println("itemCode = " + itemCode);
        String jpql = "select f "
                + " from ItemFee f "
                + " where f.retired=:ret ";
        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);

        jpql += " and f.item.code=:itemCode ";
        m.put("itemCode", itemCode);

        jpql += " and f.forInstitution=:site";
        m.put("site", site);

        List<ItemFee> fs = itemFeeFacade.findByJpql(jpql, m);
        return fs;
    }


     public void updateFeeValue(Item item, Department dept, Double feeValueForLocals, Double feeValueForForeigners) {
        FeeValue feeValue = getFeeValue(item, dept);
        if (feeValue == null) {
            feeValue = new FeeValue();
            feeValue.setItem(item);
            feeValue.setDepartment(dept);
            feeValue.setCreatedAt(new Date());
        }
        feeValue.setTotalValueForLocals(feeValueForLocals);
        feeValue.setTotalValueForForeigners(feeValueForForeigners);
        save(feeValue);
    }

    public void updateFeeValue(Item item, Institution ins, Double feeValueForLocals, Double feeValueForForeigners) {
        FeeValue feeValue = getFeeValue(item, ins);
        if (feeValue == null) {
            feeValue = new FeeValue();
            feeValue.setItem(item);
            feeValue.setInstitution(ins);
            feeValue.setCreatedAt(new Date());
        }
        feeValue.setTotalValueForLocals(feeValueForLocals);
        feeValue.setTotalValueForForeigners(feeValueForForeigners);
        save(feeValue);
    }


    public void save(FeeValue feeValue) {
        if (feeValue == null) {
            return;
        }
        if (feeValue.getId() != null) {
           feeValueFacade.edit(feeValue);
        } else {
            feeValue.setCreatedAt(new Date());
            feeValueFacade.create(feeValue);
        }
    }

    public FeeValue getFeeValue(Item item, Department department) {
        String jpql = "SELECT f FROM FeeValue f WHERE f.item = :item AND f.department = :department";
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        params.put("department", department);

        return feeValueFacade.findFirstByJpql(jpql, params);
    }

    public FeeValue getFeeValue(Item item, Institution institution) {
        String jpql = "SELECT f FROM FeeValue f WHERE f.item = :item AND f.institution = :institution";
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        params.put("institution", institution);
        return feeValueFacade.findFirstByJpql(jpql, params);
    }

    public FeeValue getFeeValue(Item item, Category category) {
        String jpql = "SELECT f FROM FeeValue f WHERE f.item = :item AND f.category = :category";
        Map<String, Object> params = new HashMap<>();
        params.put("item", item);
        params.put("category", category);

        return feeValueFacade.findFirstByJpql(jpql, params);
    }

    public FeeValue getFeeValue(Long itemId, Category category) {
        String jpql = "SELECT f FROM FeeValue f WHERE f.item.id = :iid AND f.category = :category";
        Map<String, Object> params = new HashMap<>();
        params.put("iid", itemId);
        params.put("category", category);

        return feeValueFacade.findFirstByJpql(jpql, params);
    }

}
