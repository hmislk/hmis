package com.divudi.bean.collectingCentre;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.io.Serializable;

/**
 * Backs the self-service home page landed on by a Collecting Centre
 * department's users (department type Collecting Centre), as opposed to the
 * admin-side collecting_centre/index.xhtml pages used by hospital staff to
 * manage collecting centres on their behalf.
 */
@Named
@SessionScoped
public class CollectingCentreSelfCommonController implements Serializable {

    private static final long serialVersionUID = 1L;

    public String navigateToCollectingCentreSelfBillingHome() {
        return "/collecting_centre/collecting_centre_self_billing_home?faces-redirect=true";
    }
}
