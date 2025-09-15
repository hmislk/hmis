package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.util.JsfUtil;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Controller for managing Pharmacy Retail Sale configuration options
 * 
 * @author Claude Code Assistant
 */
@Named
@ViewScoped
public class PharmacyRetailConfigController implements Serializable {

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    
    @Inject
    private com.divudi.bean.common.ConfigOptionController configOptionController;
    
    @Inject
    private com.divudi.bean.common.SessionController sessionController;

    // Paper Type Settings
    private boolean posPaper;
    private boolean posPaperCustom1;
    private boolean fiveFivePaper;
    private boolean posHeaderPaper;
    private boolean custom1Paper;
    private boolean custom2Paper;
    private boolean custom3Paper;
    
    // Token Settings
    private boolean tokenFiveFivePaper;
    private boolean tokenPosPaper;
    private boolean tokenPosPaperCustom1;
    private boolean nativePrinters;
    
    // Feature Settings
    private boolean enableTokenSystem;
    private boolean tenderedAmount;
    private boolean showItemsSummary;
    
    // Cashier Specific Settings
    private boolean cashierBillPos;
    private boolean cashierBillCustom3;
    private boolean cashierTokenPos;
    private boolean posHeaderPaperGeneral;

    public PharmacyRetailConfigController() {
    }
    

    /**
     * Load current configuration values from the department-specific options
     */
    public void loadCurrentConfig() {
        // Paper Type Settings
        posPaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill Paper is POS Paper", true);
        posPaperCustom1 = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill Paper is POS Paper Custom 1", false);
        fiveFivePaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill Paper is FiveFive Paper without Blank Space for Header", true);
        posHeaderPaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill Paper is POS paper with header", true);
        custom1Paper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill Paper is Custom 1", true);
        custom2Paper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill Paper is Custom 2", true);
        custom3Paper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill Paper is Custom 3", true);
        
        // Token Settings
        tokenFiveFivePaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Token Paper is FiveFivePaper With Blank Space For Printed Heading", true);
        tokenPosPaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Token Paper is POS Paper", true);
        tokenPosPaperCustom1 = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Token Paper is POS Paper Custom 1", false);
        nativePrinters = configOptionController.getBooleanValueByKey("Pharmacy Bill Support for Native Printers", false);
        
        // Feature Settings
        enableTokenSystem = configOptionController.getBooleanValueByKey("Enable token system in sale for cashier", false);
        tenderedAmount = configOptionController.getBooleanValueByKey("Allow Tendered Amount for pharmacy sale for cashier", false);
        showItemsSummary = configOptionController.getBooleanValueByKey("Pharmacy Analytics - Show Single Items Summary", false);
        
        // Cashier Specific Settings
        cashierBillPos = configOptionController.getBooleanValueByKey("Pharmacy Sale for Cashier Bill is Pos paper", false);
        cashierBillCustom3 = configOptionController.getBooleanValueByKey("Pharmacy Sale for cashier Bill is FiveFiveCustom3", false);
        cashierTokenPos = configOptionController.getBooleanValueByKey("Pharmacy Sale for Cashier Token Bill is Pos paper", false);
        posHeaderPaperGeneral = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill is PosHeaderPaper", true);
    }

    /**
     * Save all configuration changes
     */
    public void saveConfig() {
        try {
            // Paper Type Settings
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is POS Paper", posPaper);
            configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Bill Paper is POS Paper Custom 1", posPaperCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is FiveFive Paper without Blank Space for Header", fiveFivePaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is POS paper with header", posHeaderPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is Custom 1", custom1Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is Custom 2", custom2Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is Custom 3", custom3Paper);
            
            // Token Settings
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Token Paper is FiveFivePaper With Blank Space For Printed Heading", tokenFiveFivePaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Token Paper is POS Paper", tokenPosPaper);
            configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Token Paper is POS Paper Custom 1",tokenPosPaperCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy Bill Support for Native Printers", nativePrinters);
            
            // Feature Settings
            configOptionController.setBooleanValueByKey("Enable token system in sale for cashier", enableTokenSystem);
            configOptionController.setBooleanValueByKey("Allow Tendered Amount for pharmacy sale for cashier", tenderedAmount);
            configOptionController.setBooleanValueByKey("Pharmacy Analytics - Show Single Items Summary", showItemsSummary);
            
            // Cashier Specific Settings
            configOptionController.setBooleanValueByKey("Pharmacy Sale for Cashier Bill is Pos paper", cashierBillPos);
            configOptionController.setBooleanValueByKey("Pharmacy Sale for cashier Bill is FiveFiveCustom3", cashierBillCustom3);
            configOptionController.setBooleanValueByKey("Pharmacy Sale for Cashier Token Bill is Pos paper", cashierTokenPos);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill is PosHeaderPaper", posHeaderPaperGeneral);

            JsfUtil.addSuccessMessage("Configuration saved successfully");
            
            // Reload current values to ensure consistency
            loadCurrentConfig();
            
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving configuration: " + e.getMessage());
        }
    }

    // Getters and Setters
    public boolean isPosPaper() {
        return posPaper;
    }

    public void setPosPaper(boolean posPaper) {
        this.posPaper = posPaper;
    }

    public boolean isFiveFivePaper() {
        return fiveFivePaper;
    }

    public void setFiveFivePaper(boolean fiveFivePaper) {
        this.fiveFivePaper = fiveFivePaper;
    }

    public boolean isPosHeaderPaper() {
        return posHeaderPaper;
    }

    public void setPosHeaderPaper(boolean posHeaderPaper) {
        this.posHeaderPaper = posHeaderPaper;
    }

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

    public boolean isCustom3Paper() {
        return custom3Paper;
    }

    public void setCustom3Paper(boolean custom3Paper) {
        this.custom3Paper = custom3Paper;
    }

    public boolean isTokenFiveFivePaper() {
        return tokenFiveFivePaper;
    }

    public void setTokenFiveFivePaper(boolean tokenFiveFivePaper) {
        this.tokenFiveFivePaper = tokenFiveFivePaper;
    }

    public boolean isTokenPosPaper() {
        return tokenPosPaper;
    }

    public void setTokenPosPaper(boolean tokenPosPaper) {
        this.tokenPosPaper = tokenPosPaper;
    }

    public boolean isNativePrinters() {
        return nativePrinters;
    }

    public void setNativePrinters(boolean nativePrinters) {
        this.nativePrinters = nativePrinters;
    }

    public boolean isEnableTokenSystem() {
        return enableTokenSystem;
    }

    public void setEnableTokenSystem(boolean enableTokenSystem) {
        this.enableTokenSystem = enableTokenSystem;
    }

    public boolean isTenderedAmount() {
        return tenderedAmount;
    }

    public void setTenderedAmount(boolean tenderedAmount) {
        this.tenderedAmount = tenderedAmount;
    }

    public boolean isShowItemsSummary() {
        return showItemsSummary;
    }

    public void setShowItemsSummary(boolean showItemsSummary) {
        this.showItemsSummary = showItemsSummary;
    }

    public boolean isCashierBillPos() {
        return cashierBillPos;
    }

    public void setCashierBillPos(boolean cashierBillPos) {
        this.cashierBillPos = cashierBillPos;
    }

    public boolean isCashierBillCustom3() {
        return cashierBillCustom3;
    }

    public void setCashierBillCustom3(boolean cashierBillCustom3) {
        this.cashierBillCustom3 = cashierBillCustom3;
    }

    public boolean isCashierTokenPos() {
        return cashierTokenPos;
    }

    public void setCashierTokenPos(boolean cashierTokenPos) {
        this.cashierTokenPos = cashierTokenPos;
    }

    public boolean isPosHeaderPaperGeneral() {
        return posHeaderPaperGeneral;
    }

    public void setPosHeaderPaperGeneral(boolean posHeaderPaperGeneral) {
        this.posHeaderPaperGeneral = posHeaderPaperGeneral;
    }

    public boolean isPosPaperCustom1() {
        return posPaperCustom1;
    }

    public void setPosPaperCustom1(boolean posPaperCustom1) {
        this.posPaperCustom1 = posPaperCustom1;
    }

    public boolean isTokenPosPaperCustom1() {
        return tokenPosPaperCustom1;
    }

    public void setTokenPosPaperCustom1(boolean tokenPosPaperCustom1) {
        this.tokenPosPaperCustom1 = tokenPosPaperCustom1;
    }
    
    
}