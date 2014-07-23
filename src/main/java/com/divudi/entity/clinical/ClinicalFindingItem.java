/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.clinical;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Inheritance;

/**
 *
 * @author Buddhika
 */
@Entity
@Inheritance
public class ClinicalFindingItem extends ClinicalEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    
    
    
}
