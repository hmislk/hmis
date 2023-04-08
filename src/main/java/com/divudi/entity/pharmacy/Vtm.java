/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.pharmacy;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Inheritance;


/**
 *
 * @author buddhika
 */
@Entity
@Inheritance
public class Vtm extends PharmaceuticalItem implements Serializable {
 
}
