package com.divudi.service;

import com.divudi.core.entity.DefaultServiceDepartment;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Category;
import com.divudi.core.facade.DefaultServiceDepartmentFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Buddhika, Damith and ChatGPT
 * 
 */
@Stateless
public class DepartmentResolver {

    @EJB
    private DefaultServiceDepartmentFacade defaultServiceDepartmentFacade;

    public Department resolvePerformingDepartment(Department billingDept, Item item) {
        if (billingDept == null || item == null) {
            return null;
        }

        Category category = item.getCategory();

        // Priority 1: billingDept + item (no category)
        Department dept = findDepartment(billingDept, null, item);
        if (dept != null) {
            return dept;
        }

        // Priority 2: billingDept + category (no item)
        if (category != null) {
            dept = findDepartment(billingDept, category, null);
            if (dept != null) {
                return dept;
            }
        }

        // Priority 3: billingDept only
        dept = findDepartment(billingDept, null, null);
        if (dept != null) {
            return dept;
        }

        // Fallback: department of item
        return item.getDepartment();
    }

    private Department findDepartment(Department billingDept, Category category, Item item) {
        String jpql = "SELECT d FROM DefaultServiceDepartment d WHERE d.retired = false AND d.billingDepartment = :bd";
        Map<String, Object> params = new HashMap<>();
        params.put("bd", billingDept);

        if (category != null) {
            jpql += " AND d.category = :cat";
            params.put("cat", category);
        } else {
            jpql += " AND d.category IS NULL";
        }

        if (item != null) {
            jpql += " AND d.item = :itm";
            params.put("itm", item);
        } else {
            jpql += " AND d.item IS NULL";
        }

        DefaultServiceDepartment dsd = defaultServiceDepartmentFacade.findFirstByJpql(jpql, params);
        return dsd != null ? dsd.getPerformingDepartment() : null;
    }
}
