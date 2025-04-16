/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.ItemFee;
import com.divudi.core.entity.Service;
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
