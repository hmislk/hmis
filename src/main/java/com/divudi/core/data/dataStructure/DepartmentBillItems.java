/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import java.util.List;

/**
 *
 * @author safrin
 */
public class DepartmentBillItems {
    private Department department;
    private List<Item> items;


    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }


}
