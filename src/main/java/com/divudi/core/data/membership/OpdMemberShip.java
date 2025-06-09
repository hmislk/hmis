/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.membership;

import com.divudi.core.entity.PriceMatrix;
import com.divudi.core.entity.membership.MembershipScheme;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author safrin
 */
public class OpdMemberShip {

    MembershipScheme membershipScheme;
    List<PriceMatrix> priceMatrixs;

    public MembershipScheme getMembershipScheme() {
        return membershipScheme;
    }

    public void setMembershipScheme(MembershipScheme membershipScheme) {
        this.membershipScheme = membershipScheme;
    }


    public List<PriceMatrix> getPriceMatrixs() {
        if (priceMatrixs == null) {
            priceMatrixs = new ArrayList<>();
        }
        return priceMatrixs;
    }

    public void setPriceMatrixs(List<PriceMatrix> priceMatrixs) {
        this.priceMatrixs = priceMatrixs;
    }

}
