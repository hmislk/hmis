package com.divudi.bean.collectingCentre;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.report.ReportController;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.entity.Institution;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;

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
    @Inject
    private ReportController reportController;

    public String navigateToCollectingCentreSelfBillingHome() {
        return "/collecting_centre/cc_self_index?faces-redirect=true";
    }

    public String navigateToCollectingCentreSelfStatement() {
        reportController.setFromDate(CommonFunctions.getStartOfMonth());
        reportController.setToDate(CommonFunctions.getEndOfDay());
        reportController.setInvoiceNumber(null);
        reportController.setAgentHistories(null);
        restrictStatementToOwnCollectingCentre();
        return "/collecting_centre/cc_self_statement?faces-redirect=true";
    }

    /**
     * Reuses the admin Collection Centre Statement query, but always pins it
     * to the logged-in Collecting Centre so a centre can never see another
     * centre's balance history.
     */
    public void processCollectingCentreSelfStatement() {
        if (!restrictStatementToOwnCollectingCentre()) {
            reportController.setAgentHistories(null);
            return;
        }
        if (reportController.getFromDate() == null || reportController.getToDate() == null) {
            JsfUtil.addErrorMessage("Please select From and To dates");
            return;
        }
        reportController.processCollectingCentreStatementReportNew();
    }

    private boolean restrictStatementToOwnCollectingCentre() {
        Institution collectingCentre = sessionController.getInstitution();
        reportController.setInstitution(null);
        reportController.setCollectingCentre(collectingCentre);
        if (collectingCentre == null || !isCollectingCentreDepartment()) {
            JsfUtil.addErrorMessage("Collecting centre not found for the logged department");
            return false;
        }
        return true;
    }

    public boolean isCollectingCentreDepartment() {
        return sessionController.getDepartment() != null
                && sessionController.getDepartment().getDepartmentType() == DepartmentType.CollectingCentre;
    }
}
