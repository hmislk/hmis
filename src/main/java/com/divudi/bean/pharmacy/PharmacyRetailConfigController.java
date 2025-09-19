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

    // Settle Payment Settings
    private boolean settlePaymentPosPaper;
    private boolean settlePaymentPosPaperCustom1;
    private boolean settlePaymentCustom1;
    private boolean settlePaymentCustom2;
    private boolean settlePaymentCustom3;
    private boolean settlePaymentPosHeaderPaper;

    // GRN Settings
    private boolean grnReceiptA4;
    private boolean grnReceiptCustom1;
    private boolean grnReceiptCustom2;

    // Transfer Receive Settings
    private boolean transferReceiveA4;
    private boolean transferReceiveTemplate;
    private boolean transferReceiveA4Detailed;
    private boolean transferReceiveA4Custom1;
    private boolean transferReceiveA4Custom2;

    // Transfer Request Settings
    private boolean transferRequestA4;
    private boolean transferRequestCustom1;
    private boolean transferRequestCustom2;

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

        // Settle Payment Settings
        settlePaymentPosPaper = configOptionController.getBooleanValueByKey("Pharmacy accept payment for sale for cashier bill with Items is PosPaper", true);
        settlePaymentPosPaperCustom1 = configOptionController.getBooleanValueByKey("Pharmacy accept payment for sale for cashier bill with Items is PosPaper Custom 1", false);
        settlePaymentCustom1 = configOptionController.getBooleanValueByKey("Pharmacy accept payment for sale for cashier Bill is Custom 1", true);
        settlePaymentCustom2 = configOptionController.getBooleanValueByKey("Pharmacy accept payment for sale for cashier Bill is Custom 2", true);
        settlePaymentCustom3 = configOptionController.getBooleanValueByKey("Pharmacy accept payment for sale for cashier Bill is Custom 3", true);
        settlePaymentPosHeaderPaper = configOptionController.getBooleanValueByKey("Pharmacy accept payment for sale for cashier Bill is PosHeaderPaper", true);

        // GRN Settings
        grnReceiptA4 = configOptionController.getBooleanValueByKey("GRN Receipt Paper is A4", true);
        grnReceiptCustom1 = configOptionController.getBooleanValueByKey("GRN Receipt Paper is Custom 1", true);
        grnReceiptCustom2 = configOptionController.getBooleanValueByKey("GRN Receipt Paper is Custom 2", true);

        // Transfer Receive Settings
        transferReceiveA4 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4", true);
        transferReceiveTemplate = configOptionController.getBooleanValueByKey("Pharmacy Transfer Receive Bill is Template", false);
        transferReceiveA4Detailed = configOptionController.getBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4 Detailed", true);
        transferReceiveA4Custom1 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4 Custom 1", true);
        transferReceiveA4Custom2 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4 Custom 2", true);

        // Transfer Request Settings
        transferRequestA4 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Request Receipt is A4", true);
        transferRequestCustom1 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Request Receipt is Custom 1", true);
        transferRequestCustom2 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Request Receipt is Custom 2", true);
    }

    /**
     * Save all configuration changes
     */
    public void saveConfig() {
        try {
            // Paper Type Settings
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is POS Paper", posPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is POS Paper Custom 1", posPaperCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is FiveFive Paper without Blank Space for Header", fiveFivePaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is POS paper with header", posHeaderPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is Custom 1", custom1Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is Custom 2", custom2Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is Custom 3", custom3Paper);
            
            // Token Settings
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Token Paper is FiveFivePaper With Blank Space For Printed Heading", tokenFiveFivePaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Token Paper is POS Paper", tokenPosPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Token Paper is POS Paper Custom 1", tokenPosPaperCustom1);
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

            // Settle Payment Settings
            configOptionController.setBooleanValueByKey("Pharmacy accept payment for sale for cashier bill with Items is PosPaper", settlePaymentPosPaper);
            configOptionController.setBooleanValueByKey("Pharmacy accept payment for sale for cashier bill with Items is PosPaper Custom 1", settlePaymentPosPaperCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy accept payment for sale for cashier Bill is Custom 1", settlePaymentCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy accept payment for sale for cashier Bill is Custom 2", settlePaymentCustom2);
            configOptionController.setBooleanValueByKey("Pharmacy accept payment for sale for cashier Bill is Custom 3", settlePaymentCustom3);
            configOptionController.setBooleanValueByKey("Pharmacy accept payment for sale for cashier Bill is PosHeaderPaper", settlePaymentPosHeaderPaper);

            // GRN Settings
            configOptionController.setBooleanValueByKey("GRN Receipt Paper is A4", grnReceiptA4);
            configOptionController.setBooleanValueByKey("GRN Receipt Paper is Custom 1", grnReceiptCustom1);
            configOptionController.setBooleanValueByKey("GRN Receipt Paper is Custom 2", grnReceiptCustom2);

            // Transfer Receive Settings
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4", transferReceiveA4);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Receive Bill is Template", transferReceiveTemplate);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4 Detailed", transferReceiveA4Detailed);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4 Custom 1", transferReceiveA4Custom1);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4 Custom 2", transferReceiveA4Custom2);

            // Transfer Request Settings
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Request Receipt is A4", transferRequestA4);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Request Receipt is Custom 1", transferRequestCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Request Receipt is Custom 2", transferRequestCustom2);

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

    public boolean isSettlePaymentPosPaper() {
        return settlePaymentPosPaper;
    }

    public void setSettlePaymentPosPaper(boolean settlePaymentPosPaper) {
        this.settlePaymentPosPaper = settlePaymentPosPaper;
    }

    public boolean isSettlePaymentCustom1() {
        return settlePaymentCustom1;
    }

    public void setSettlePaymentCustom1(boolean settlePaymentCustom1) {
        this.settlePaymentCustom1 = settlePaymentCustom1;
    }

    public boolean isSettlePaymentCustom2() {
        return settlePaymentCustom2;
    }

    public void setSettlePaymentCustom2(boolean settlePaymentCustom2) {
        this.settlePaymentCustom2 = settlePaymentCustom2;
    }

    public boolean isSettlePaymentCustom3() {
        return settlePaymentCustom3;
    }

    public void setSettlePaymentCustom3(boolean settlePaymentCustom3) {
        this.settlePaymentCustom3 = settlePaymentCustom3;
    }

    public boolean isSettlePaymentPosHeaderPaper() {
        return settlePaymentPosHeaderPaper;
    }

    public void setSettlePaymentPosHeaderPaper(boolean settlePaymentPosHeaderPaper) {
        this.settlePaymentPosHeaderPaper = settlePaymentPosHeaderPaper;
    }

    public boolean isGrnReceiptA4() {
        return grnReceiptA4;
    }

    public void setGrnReceiptA4(boolean grnReceiptA4) {
        this.grnReceiptA4 = grnReceiptA4;
    }

    public boolean isGrnReceiptCustom1() {
        return grnReceiptCustom1;
    }

    public void setGrnReceiptCustom1(boolean grnReceiptCustom1) {
        this.grnReceiptCustom1 = grnReceiptCustom1;
    }
    
     public boolean isGrnReceiptCustom2() {
        return grnReceiptCustom2;
    }

    public void setGrnReceiptCustom2(boolean grnReceiptCustom2) {
        this.grnReceiptCustom2 = grnReceiptCustom2;
    }
    

    public boolean isTransferReceiveA4() {
        return transferReceiveA4;
    }

    public void setTransferReceiveA4(boolean transferReceiveA4) {
        this.transferReceiveA4 = transferReceiveA4;
    }

    public boolean isTransferReceiveTemplate() {
        return transferReceiveTemplate;
    }

    public void setTransferReceiveTemplate(boolean transferReceiveTemplate) {
        this.transferReceiveTemplate = transferReceiveTemplate;
    }

    public boolean isTransferReceiveA4Detailed() {
        return transferReceiveA4Detailed;
    }

    public void setTransferReceiveA4Detailed(boolean transferReceiveA4Detailed) {
        this.transferReceiveA4Detailed = transferReceiveA4Detailed;
    }

    public boolean isTransferReceiveA4Custom1() {
        return transferReceiveA4Custom1;
    }

    public void setTransferReceiveA4Custom1(boolean transferReceiveA4Custom1) {
        this.transferReceiveA4Custom1 = transferReceiveA4Custom1;
    }

    public boolean isTransferReceiveA4Custom2() {
        return transferReceiveA4Custom2;
    }

    public void setTransferReceiveA4Custom2(boolean transferReceiveA4Custom2) {
        this.transferReceiveA4Custom2 = transferReceiveA4Custom2;
    }

    public boolean isTransferRequestA4() {
        return transferRequestA4;
    }

    public void setTransferRequestA4(boolean transferRequestA4) {
        this.transferRequestA4 = transferRequestA4;
    }

    public boolean isTransferRequestCustom1() {
        return transferRequestCustom1;
    }

    public void setTransferRequestCustom1(boolean transferRequestCustom1) {
        this.transferRequestCustom1 = transferRequestCustom1;
    }

    public boolean isTransferRequestCustom2() {
        return transferRequestCustom2;
    }

    public void setTransferRequestCustom2(boolean transferRequestCustom2) {
        this.transferRequestCustom2 = transferRequestCustom2;
    }

    public boolean isSettlePaymentPosPaperCustom1() {
        return settlePaymentPosPaperCustom1;
    }

    public void setSettlePaymentPosPaperCustom1(boolean settlePaymentPosPaperCustom1) {
        this.settlePaymentPosPaperCustom1 = settlePaymentPosPaperCustom1;
    }
    
    

}