package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
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

    private boolean grnReturnApprovalRequired;
    private boolean directPurchaseReturnApprovalRequired;

    public PharmacyReturnApprovalConfigController() {
    }

    public void loadCurrentConfig() {
        grnReturnApprovalRequired = configOptionApplicationController.getBooleanValueByKey("GRN Return - Approval Required", true);
        directPurchaseReturnApprovalRequired = configOptionApplicationController.getBooleanValueByKey("Direct Purchase Return - Approval Required", true);
    }

    public void saveConfig() {
        try {
            configOptionApplicationController.setBooleanValueByKey("GRN Return - Approval Required", grnReturnApprovalRequired);
            configOptionApplicationController.setBooleanValueByKey("Direct Purchase Return - Approval Required", directPurchaseReturnApprovalRequired);

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
}
