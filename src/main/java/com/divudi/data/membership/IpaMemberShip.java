/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.membership;

import com.divudi.entity.membership.MembershipScheme;
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
