/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.xml.bind.annotation.XmlRootElement;


/**
 *
 * @author buddhika
 */
@Entity
@XmlRootElement
public class Speciality extends Category implements Serializable {
}
