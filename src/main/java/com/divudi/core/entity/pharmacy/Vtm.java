/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.pharmacy;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;


/**
 *
 * @author buddhika
 */
@Entity
@Table(
    indexes = {
        @Index(name = "idx_vtm_name", columnList = "name")
    }
)
public class Vtm extends PharmaceuticalItem implements Serializable {

}
