package com.divudi.entity;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * Dr. M H B Ariyaratne
 *
 * @author www.divudi.com
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Packege extends Item implements Serializable {
    @OneToMany(mappedBy = "packege")
     List<PackageFee> packageFees;
  

     static final long serialVersionUID = 1L;
    
}
