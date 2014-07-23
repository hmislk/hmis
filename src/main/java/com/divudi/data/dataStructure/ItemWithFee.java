/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Item;

/**
 *
 * @author safrin
 */
public class ItemWithFee {

    private Item item;
    private double count;
    private double hospitalFee;
    private double proFee;
    private double total;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public double getProFee() {
        return proFee;
    }

    public void setProFee(double proFee) {
        this.proFee = proFee;
    }

    public double getTotal() {
        if (total == 0) {
            total = hospitalFee + proFee;
        }
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getCount() {
        return count;
    }

    public void setCount(double count) {
        this.count = count;
    }
}
