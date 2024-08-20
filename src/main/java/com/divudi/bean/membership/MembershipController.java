/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.divudi.bean.membership;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 *
 * @author Senula Nanayakkara
 */
@Named
@SessionScoped
public class MembershipController implements Serializable {

    private int membershipAdminIndex;

    public MembershipController() {
    }

    public int getMembershipAdminIndex() {
        return membershipAdminIndex;
    }

    public void setMembershipAdminIndex(int membershipAdminIndex) {
        this.membershipAdminIndex = membershipAdminIndex;
    }

}
