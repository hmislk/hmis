/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.memberShip;

import com.divudi.entity.memberShip.MembershipScheme;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author safrin
 */
public class IpaMemberShip {

    private MembershipScheme membershipScheme;
    List<IpaPaymentMethod> ipaPaymentMethods;

    public List<IpaPaymentMethod> getIpaPaymentMethods() {
        if (ipaPaymentMethods == null) {
            ipaPaymentMethods = new ArrayList<>();
        }
        return ipaPaymentMethods;
    }

    public void setIpaPaymentMethods(List<IpaPaymentMethod> ipaPaymentMethods) {
        this.ipaPaymentMethods = ipaPaymentMethods;
    }

    public MembershipScheme getMembershipScheme() {
        return membershipScheme;
    }

    public void setMembershipScheme(MembershipScheme membershipScheme) {
        this.membershipScheme = membershipScheme;
    }

}
