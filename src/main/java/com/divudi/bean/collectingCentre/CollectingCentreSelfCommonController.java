package com.divudi.bean.collectingCentre;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.DepartmentType;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
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

    @Inject
    private SessionController sessionController;

    public String navigateToCollectingCentreSelfBillingHome() {
        return "/collecting_centre/cc_self_index?faces-redirect=true";
    }

    public boolean isCollectingCentreDepartment() {
        return sessionController.getDepartment() != null
                && sessionController.getDepartment().getDepartmentType() == DepartmentType.CollectingCentre;
    }
}
