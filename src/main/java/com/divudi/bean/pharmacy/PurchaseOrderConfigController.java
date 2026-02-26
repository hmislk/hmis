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
    
    //Paper Type Settings for Purchase Order Approval
    private boolean a4custom1paper;
    private boolean a4custom2paper;
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
        
        // Purchase Order Approval Paper Settings
        a4custom1paper = configOptionController.getBooleanValueByKey("Pharmacy Purchase Order Approval Print - A4 Custom 1 Paper Format", true);
        a4custom2paper = configOptionController.getBooleanValueByKey("Pharmacy Purchase Order Approval Print - A4 Custom 2 Paper Format", false);
        letterPaper = configOptionController.getBooleanValueByKey("Pharmacy Purchase Order Approval Print - Letter Paper Format", false);
    }

    /**
     * Save all configuration changes
     */
    public void saveConfig() {
        try {
            // Purchase Order Request Paper Settings
            configOptionController.setBooleanValueByKey("Pharmacy Purchase Order Request Print - A4 Paper Custom 1", custom1Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Purchase Order Request Print - A4 Paper Custom 2", custom2Paper);
            
            // Purchase Order Approval Paper Settings
            configOptionController.setBooleanValueByKey("Pharmacy Purchase Order Approval Print - A4 Custom 1 Paper Format", a4custom1paper);
            configOptionController.setBooleanValueByKey("Pharmacy Purchase Order Approval Print - A4 Custom 2 Paper Format", a4custom2paper);
            configOptionController.setBooleanValueByKey("Pharmacy Purchase Order Approval Print - Letter Paper Format", letterPaper);

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

    public boolean isA4custom1paper() {
        return a4custom1paper;
    }

    public void setA4custom1paper(boolean a4custom1paper) {
        this.a4custom1paper = a4custom1paper;
    }

    public boolean isA4custom2paper() {
        return a4custom2paper;
    }

    public void setA4custom2paper(boolean a4custom2paper) {
        this.a4custom2paper = a4custom2paper;
    }
    
    
}