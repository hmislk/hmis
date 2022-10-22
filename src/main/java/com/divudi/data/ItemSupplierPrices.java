/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Vmp;

/**
 *
 * @author Niluka
 */
public class ItemSupplierPrices {
    Item item;
    Amp amp;
    Vmp vmp;
    Institution supplier;
    double pp;
    double sp;
    double wp;

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Amp getAmp() {
        return amp;
    }

    public void setAmp(Amp amp) {
        this.amp = amp;
    }

    public Vmp getVmp() {
        return vmp;
    }

    public void setVmp(Vmp vmp) {
        this.vmp = vmp;
    }

    public Institution getSupplier() {
        return supplier;
    }

    public void setSupplier(Institution supplier) {
        this.supplier = supplier;
    }

    public double getPp() {
        return pp;
    }

    public void setPp(double pp) {
        this.pp = pp;
    }

    public double getSp() {
        return sp;
    }

    public void setSp(double sp) {
        this.sp = sp;
    }

    public double getWp() {
        return wp;
    }

    public void setWp(double wp) {
        this.wp = wp;
    }
    
    
    
    
}
