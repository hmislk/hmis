/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.lab;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import org.eclipse.persistence.annotations.CascadeOnDelete;

/**
 *
 * @author Buddhika
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class InvestigationItem extends ReportItem implements Serializable {

    private static final long serialVersionUID = 1L;
    
    
    @OneToMany(mappedBy = "investigationItem", fetch= FetchType.LAZY, cascade = {CascadeType.MERGE,CascadeType.PERSIST,CascadeType.PERSIST,CascadeType.REFRESH})
    List<InvestigationItemValue> investigationItemValues;

    public List<InvestigationItemValue> getInvestigationItemValues() {
        return investigationItemValues;
    }

    public void setInvestigationItemValues(List<InvestigationItemValue> investigationItemValues) {
        this.investigationItemValues = investigationItemValues;
    }
    
    
    
    
    
}
