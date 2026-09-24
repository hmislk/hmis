/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.inward;

import com.divudi.core.data.inward.InwardDiscountMatrixScope;
import com.divudi.core.entity.PriceMatrix;
import java.io.Serializable;
import javax.persistence.EnumType;
import javax.persistence.Entity;
import javax.persistence.Enumerated;

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
 * {@link #scope} records which InwardDiscountMatrixApi scope
 * (pharmacy or service/investigation) an entry belongs to. It is set on
 * EVERY row at create time -- for a row with a real category it mirrors
 * that category's type, and for a wildcard row (categoryId omitted,
 * applies to every category in the scope, where the category's type is
 * unavailable) it is the only signal of scope. Filtering on this enum
 * directly (rather than inferring scope from the category's Java type via
 * a JPQL type() discriminator check) avoids a JPQL/EclipseLink quirk where
 * type() does not reliably participate in an OR once the joined category
 * is null.
 *
 * @author Dr M H B Ariyaratne
 */
@Entity
public class InwardDiscountMatrix extends PriceMatrix implements Serializable {

    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    private InwardDiscountMatrixScope scope;

    public InwardDiscountMatrixScope getScope() {
        return scope;
    }

    public void setScope(InwardDiscountMatrixScope scope) {
        this.scope = scope;
    }

}
