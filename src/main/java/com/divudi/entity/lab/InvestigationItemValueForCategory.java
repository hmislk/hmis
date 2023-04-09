/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.lab;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 *
 * @author Buddhika
 */
@Entity
public class InvestigationItemValueForCategory extends InvestigationItemValue implements Serializable {
    
    @ManyToOne
    private InvestigationItemValueCategory investigationItemValueCategory;

    public InvestigationItemValueCategory getInvestigationItemValueCategory() {
        return investigationItemValueCategory;
    }

    public void setInvestigationItemValueCategory(InvestigationItemValueCategory investigationItemValueCategory) {
        this.investigationItemValueCategory = investigationItemValueCategory;
    }
    
    
    
}
