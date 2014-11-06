/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.dataStructure;

import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.lab.Investigation;

/**
 *
 * @author buddhika
 */
public class ItemInstitutionCollectingCentreCountRow {

    Institution institution;
    Institution collectingCentre;
    Item item;
    Investigation investigation;
    Long count;
    Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
    

    public ItemInstitutionCollectingCentreCountRow() {
    }

    public ItemInstitutionCollectingCentreCountRow(Item item, Long count, Institution institution) {
        this.institution = institution;
        this.collectingCentre = null;
        this.item = item;
        this.count = count;
    }

    public ItemInstitutionCollectingCentreCountRow(Item item, Long count, Institution institution, Institution collectingCentre) {
        this.institution = institution;
        this.collectingCentre = collectingCentre;
        this.item = item;
        this.count = count;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

}
