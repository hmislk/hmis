/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.inward;

import com.divudi.core.entity.PriceMatrix;
import java.io.Serializable;
import javax.persistence.Entity;

/**
 * Inward Discount Matrix entry.
 *
 * Stores the discount percentage applicable for a combination of:
 * department, category, BHT type (paymentMethod), admission type, and
 * discount scheme (paymentScheme).
 *
 * All fields are inherited from PriceMatrix. The discriminator is the
 * concrete entity type, which keeps these records separate from
 * InwardPriceAdjustment (margin) records.
 *
 * @author Dr M H B Ariyaratne
 */
@Entity
public class InwardDiscountMatrix extends PriceMatrix implements Serializable {

    private static final long serialVersionUID = 1L;

}
