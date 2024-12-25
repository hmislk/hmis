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
    
    public String navigateToMembershipAnalyticsIndex(){
        return "/membership/analytics/index?faces-redirect=true";
    }

}
