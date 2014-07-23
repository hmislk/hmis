/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.data.dataStructure;

import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
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
