package com.divudi.entity;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

/**
 * Dr. M H B Ariyaratne
 *
 * @author www.divudi.com
 */
@Entity
public class Packege extends Item implements Serializable {
    @OneToMany(mappedBy = "packege")
    private List<PackageFee> packageFees;

    public List<PackageFee> getPackageFees() {
        return packageFees;
    }

    public void setPackageFees(List<PackageFee> packageFees) {
        this.packageFees = packageFees;
    }

    
    
}
