/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.ItemFee;
import com.divudi.entity.Service;
import java.util.List;

/**
 *
 * @author safrin
 */
public class ServiceFee {
    private Service service;
    private List<ItemFee> itemFees;

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public List<ItemFee> getItemFees() {
        return itemFees;
    }

    public void setItemFees(List<ItemFee> itemFees) {
        this.itemFees = itemFees;
    }
    
    
}
