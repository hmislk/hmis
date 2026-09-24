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
 * Most fields are inherited from PriceMatrix. The discriminator is the
 * concrete entity type, which keeps these records separate from
 * InwardPriceAdjustment (margin) records.
 *
 * {@link #scope} records which InwardDiscountMatrixApi scope ('service' or
 * 'pharmacy') an entry belongs to. It is normally inferable from the
 * runtime type of {@code category} (ServiceCategory/ServiceSubCategory/
 * InvestigationCategory vs PharmaceuticalItemCategory), but a wildcard row
 * (categoryId omitted, applies to every category in that scope) has a null
 * category and so cannot be classified that way. This field disambiguates
 * that case; it is set on create and never changes.
 *
 * @author Dr M H B Ariyaratne
 */
@Entity
public class InwardDiscountMatrix extends PriceMatrix implements Serializable {

    private static final long serialVersionUID = 1L;

    private String scope;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

}
