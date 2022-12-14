/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.inward;

import com.divudi.entity.Category;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 *
 * @author Buddhika
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class TimedItemCategory extends Category implements Serializable {
    private static final long serialVersionUID = 1L;
   
}
