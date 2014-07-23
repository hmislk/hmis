/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.clinical;

import com.divudi.data.SymanticHyrachi;
import com.divudi.entity.Item;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 *
 * @author buddhika
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ClinicalEntity extends Item implements Serializable {

    private static final long serialVersionUID = 1L;
    
  
    SymanticHyrachi symanticType;
    
    
    
    
    
    
}
