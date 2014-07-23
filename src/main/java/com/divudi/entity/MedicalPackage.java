package com.divudi.entity;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * Dr. M H B Ariyaratne
 *
 * @author www.divudi.com
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class MedicalPackage extends Item implements Serializable {

    private static final long serialVersionUID = 1L;
}
