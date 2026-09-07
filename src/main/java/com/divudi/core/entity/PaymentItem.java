/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.entity;

import java.io.Serializable;
import javax.persistence.Entity;

/**
 * Entity to represent different types of payments
 * Examples: Miscellaneous Payment for OPD Doctor, Shift Fee, etc.
 *
 * @author buddhika
 */
@Entity
public class PaymentItem extends Item implements Serializable {

}
