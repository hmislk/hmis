package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.util.JsfUtil;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Controller for managing Pharmacy Purchase Order Request configuration options
 *
 * @author Claude Code Assistant
 */
@Named
@ViewScoped
public class PurchaseOrderConfigController implements Serializable {

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    @Inject
    private com.divudi.bean.common.ConfigOptionController configOptionController;

    @Inject
    private com.divudi.bean.common.SessionController sessionController;

    // Paper Type Settings for Purchase Order Request
    private boolean custom1Paper;
    private boolean custom2Paper;
    private boolean letterPaper;

    public PurchaseOrderConfigController() {
    }

    /**
     * Load current configuration values from the department-specific options
     */
    public void loadCurrentConfig() {
        // Purchase Order Request Paper Settings
        custom1Paper = configOptionController.getBooleanValueByKey("Pharmacy Purchase Order Request Print - A4 Paper Custom 1", true);
        custom2Paper = configOptionController.getBooleanValueByKey("Pharmacy Purchase Order Request Print - A4 Paper Custom 2", false);
        letterPaper = configOptionController.getBooleanValueByKey("Pharmacy Purchase Order Request Print - Letter Paper Format", true);
    }

    /**
     * Save all configuration changes
     */
    public void saveConfig() {
        try {
            // Purchase Order Request Paper Settings
            configOptionController.setBooleanValueByKey("Pharmacy Purchase Order Request Print - A4 Paper Custom 1", custom1Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Purchase Order Request Print - A4 Paper Custom 2", custom2Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Purchase Order Request Print - Letter Paper Format", letterPaper);

            JsfUtil.addSuccessMessage("Purchase Order configuration saved successfully");

            // Reload current values to ensure consistency
            loadCurrentConfig();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving configuration: " + e.getMessage());
        }
    }

    // Getters and Setters
    public boolean isCustom1Paper() {
        return custom1Paper;
    }

    public void setCustom1Paper(boolean custom1Paper) {
        this.custom1Paper = custom1Paper;
    }

    public boolean isCustom2Paper() {
        return custom2Paper;
    }

    public void setCustom2Paper(boolean custom2Paper) {
        this.custom2Paper = custom2Paper;
    }

    public boolean isLetterPaper() {
        return letterPaper;
    }

    public void setLetterPaper(boolean letterPaper) {
        this.letterPaper = letterPaper;
    }
    
    
}