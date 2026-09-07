package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.util.JsfUtil;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Controller for managing approval-requirement settings for pharmacy
 * GRN and Direct Purchase Returns (#21404).
 */
@Named
@ViewScoped
public class PharmacyReturnApprovalConfigController implements Serializable {

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private WebUserController webUserController;

    private boolean grnReturnApprovalRequired;
    private boolean directPurchaseReturnApprovalRequired;
    private boolean directPurchaseApprovalRequired;

    public PharmacyReturnApprovalConfigController() {
    }

    public void loadCurrentConfig() {
        grnReturnApprovalRequired = configOptionApplicationController.getBooleanValueByKey("GRN Return - Approval Required", true);
        directPurchaseReturnApprovalRequired = configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return - Approval Required", true);
        directPurchaseApprovalRequired = configOptionApplicationController.getBooleanValueByKey("Use Save Finalize Approve Workflow for Direct Purchase", false);
    }

    public void saveConfig() {
        // Server-side guard: the Config button is privilege-gated in the UI,
        // but the dialog form itself is always in the component tree, so the
        // action is invocable without it. Mirror the button's rendered check.
        if (!webUserController.hasPrivilege("PharmacyGrnApprove")
                && !webUserController.hasPrivilege("PharmacyDirectPurchaseApprove")) {
            JsfUtil.addErrorMessage("You do not have permission to modify procurement return configuration.");
            return;
        }
        try {
            configOptionApplicationController.setBooleanValueByKey("GRN Return - Approval Required", grnReturnApprovalRequired);
            configOptionApplicationController.setBooleanValueByKey("Direct Purchase Return - Approval Required", directPurchaseReturnApprovalRequired);
            configOptionApplicationController.setBooleanValueByKey("Use Save Finalize Approve Workflow for Direct Purchase", directPurchaseApprovalRequired);

            JsfUtil.addSuccessMessage("Procurement return configuration saved successfully");

            loadCurrentConfig();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving configuration: " + e.getMessage());
        }
    }

    public boolean isGrnReturnApprovalRequired() {
        return grnReturnApprovalRequired;
    }

    public void setGrnReturnApprovalRequired(boolean grnReturnApprovalRequired) {
        this.grnReturnApprovalRequired = grnReturnApprovalRequired;
    }

    public boolean isDirectPurchaseReturnApprovalRequired() {
        return directPurchaseReturnApprovalRequired;
    }

    public void setDirectPurchaseReturnApprovalRequired(boolean directPurchaseReturnApprovalRequired) {
        this.directPurchaseReturnApprovalRequired = directPurchaseReturnApprovalRequired;
    }

    public boolean isDirectPurchaseApprovalRequired() {
        return directPurchaseApprovalRequired;
    }

    public void setDirectPurchaseApprovalRequired(boolean directPurchaseApprovalRequired) {
        this.directPurchaseApprovalRequired = directPurchaseApprovalRequired;
    }
}
