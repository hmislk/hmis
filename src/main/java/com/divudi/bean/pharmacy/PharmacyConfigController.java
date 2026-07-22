package com.divudi.bean.pharmacy;

import com.divudi.core.util.JsfUtil;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Controller for managing Pharmacy printer/paper-format configuration options,
 * backed by department-specific ConfigOptionController settings.
 */
@Named
@ViewScoped
public class PharmacyConfigController implements Serializable {

    @Inject
    private com.divudi.bean.common.ConfigOptionController configOptionController;

    // Retail Sale Bill Paper Settings
    private boolean retailSalePosPaper;
    private boolean retailSaleWithItemsPaper;
    private boolean retailSalePrabodhaPaper;
    private boolean retailSaleFiveFivePaper;
    private boolean retailSalePosHeaderPaper;
    private boolean retailSaleCustom3Paper;

    /**
     * Load current configuration values from the department-specific options
     */
    public void loadCurrentConfig() {
        retailSalePosPaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill is PosPaper", true);
        retailSaleWithItemsPaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill with Items is PosPaper", true);
        retailSalePrabodhaPaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill is PosPaper(prabodha)", true);
        retailSaleFiveFivePaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill is FiveFivePaper", true);
        retailSalePosHeaderPaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill is PosHeaderPaper", true);
        retailSaleCustom3Paper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill is FiveFiveCustom3", true);
    }

    /**
     * Save Pharmacy Retail Sale configuration changes
     */
    public void saveRetailSaleConfig() {
        try {
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill is PosPaper", retailSalePosPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill with Items is PosPaper", retailSaleWithItemsPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill is PosPaper(prabodha)", retailSalePrabodhaPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill is FiveFivePaper", retailSaleFiveFivePaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill is PosHeaderPaper", retailSalePosHeaderPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill is FiveFiveCustom3", retailSaleCustom3Paper);

            JsfUtil.addSuccessMessage("Pharmacy Retail Sale printer configuration saved successfully");

            loadCurrentConfig();
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Pharmacy Retail Sale printer configuration: " + e.getMessage());
        }
    }

    public boolean isRetailSalePosPaper() {
        return retailSalePosPaper;
    }

    public void setRetailSalePosPaper(boolean retailSalePosPaper) {
        this.retailSalePosPaper = retailSalePosPaper;
    }

    public boolean isRetailSaleWithItemsPaper() {
        return retailSaleWithItemsPaper;
    }

    public void setRetailSaleWithItemsPaper(boolean retailSaleWithItemsPaper) {
        this.retailSaleWithItemsPaper = retailSaleWithItemsPaper;
    }

    public boolean isRetailSalePrabodhaPaper() {
        return retailSalePrabodhaPaper;
    }

    public void setRetailSalePrabodhaPaper(boolean retailSalePrabodhaPaper) {
        this.retailSalePrabodhaPaper = retailSalePrabodhaPaper;
    }

    public boolean isRetailSaleFiveFivePaper() {
        return retailSaleFiveFivePaper;
    }

    public void setRetailSaleFiveFivePaper(boolean retailSaleFiveFivePaper) {
        this.retailSaleFiveFivePaper = retailSaleFiveFivePaper;
    }

    public boolean isRetailSalePosHeaderPaper() {
        return retailSalePosHeaderPaper;
    }

    public void setRetailSalePosHeaderPaper(boolean retailSalePosHeaderPaper) {
        this.retailSalePosHeaderPaper = retailSalePosHeaderPaper;
    }

    public boolean isRetailSaleCustom3Paper() {
        return retailSaleCustom3Paper;
    }

    public void setRetailSaleCustom3Paper(boolean retailSaleCustom3Paper) {
        this.retailSaleCustom3Paper = retailSaleCustom3Paper;
    }
}
