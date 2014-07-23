/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.memberShip;

import com.divudi.entity.PriceMatrix;
import com.divudi.entity.memberShip.MembershipScheme;
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
