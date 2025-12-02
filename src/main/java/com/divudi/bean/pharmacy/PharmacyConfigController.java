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
public class PharmacyConfigController implements Serializable {

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
    
    // GRN Return Settings
    private boolean grnReturnReceiptCustom1;
    private boolean grnReturnReceiptCustom2;

    // Transfer Receive Settings
    private boolean transferReceiveA4;
    private boolean transferReceiveTemplate;
    private boolean transferReceiveA4Detailed;
    private boolean transferReceiveA4Custom1;
    private boolean transferReceiveA4Custom2;
    private boolean transferReceiveCustom1;

    // Transfer Request Settings
    private boolean transferRequestA4;
    private boolean transferRequestCustom1;
    private boolean transferRequestCustom2;

    // Transfer Issue Settings
    private boolean transferIssueA4Paper;
    private boolean transferIssueA4PaperDetailed;
    private boolean transferIssuePosPaper;
    private boolean transferIssuePosHeaderPaper;
    private boolean transferIssueTemplate;
    private boolean transferIssueCustom1;

    // Direct Purchase Settings
    private boolean directPurchaseA4Paper;
    private boolean directPurchaseA4PaperCustom1;
    private boolean directPurchaseA4Details;
    private boolean directPurchaseCustom1;
    private boolean directPurchaseCustom2;
    private boolean directPurchaseCustomLetter;
    
    // Direct Purchase Return Settings
    private boolean directPurchaseReturnCustom1;
    private boolean directPurchaseReturnCustom2;

    // Disposal Issue Settings
    private boolean disposalIssueA4Paper;
    private boolean disposalIssueCustom1;
    private boolean disposalIssueCustom2;

    // Bill Return Issue Settings
    private boolean billReturnIssueA4Paper;
    private boolean billReturnIssueCustom1;
    private boolean billReturnIssueCustom2;
    
    // Disposal Cancel Settings
    private boolean disposalCancelA4Paper;
    private boolean disposalCancelCustom1;
    private boolean disposalCancelCustom2;

    // Cancel Bill Settings
    private boolean cancelBillPosPaper;
    private boolean cancelBillPosPaperCustom1;
    private boolean cancelBillFiveFivePaper;
    private boolean cancelBillPosHeaderPaper;
    private boolean cancelBillCustom3;

    // Retail Sale Return Items Only Settings (no payment)
    private boolean retailSaleReturnItemsBillFiveFiveCustom3;
    private boolean retailSaleReturnItemsBillPosHeaderPaper;

    // Retail Sale Return with Refund Payment Settings
    private boolean retailSaleReturnRefundBillA4Paper;
    private boolean retailSaleReturnRefundBillPosPaper;
    
    // Retail Sale Return Settings
    private boolean retailSaleReturnPosPaper;
    private boolean retailSaleReturnFiveFivePaper;
    private boolean retailSaleReturnPosHeaderPaper;
    private boolean retailSaleReturnFiveFiveCustom3;
    private boolean retailSaleReturnPosPaperCustom1;

    // Pharmacy Return Without Tresing Settings
    private boolean returnWithoutTresingPosPaper;
    private boolean returnWithoutTresingFiveFivePaper;
    private boolean returnWithoutTresingA4Paper;

    // Credit Settlement Cancellation Settings
    private boolean creditSettlementCancellationA4;
    private boolean creditSettlementCancellationPos;
    private boolean creditSettlementCancellationFiveFive;
    private boolean creditSettlementCancellationCustom1;
    private boolean creditSettlementCancellationCustom2;
    private boolean creditSettlementCancellationCustom3;
    private boolean creditSettlementCancellationPosHeader;
    private boolean creditSettlementCancellationPosCustom1;

    public PharmacyConfigController() {
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
        settlePaymentPosPaperCustom1 = configOptionController.getBooleanValueByKey("Pharmacy accept payment for sale for cashier bill with Items is PosPaper Custom 1", true);
        settlePaymentCustom1 = configOptionController.getBooleanValueByKey("Pharmacy accept payment for sale for cashier Bill is Custom 1", true);
        settlePaymentCustom2 = configOptionController.getBooleanValueByKey("Pharmacy accept payment for sale for cashier Bill is Custom 2", true);
        settlePaymentCustom3 = configOptionController.getBooleanValueByKey("Pharmacy accept payment for sale for cashier Bill is Custom 3", true);
        settlePaymentPosHeaderPaper = configOptionController.getBooleanValueByKey("Pharmacy accept payment for sale for cashier Bill is PosHeaderPaper", true);

        // GRN Settings
        grnReceiptA4 = configOptionController.getBooleanValueByKey("GRN Receipt Paper is A4", true);
        grnReceiptCustom1 = configOptionController.getBooleanValueByKey("GRN Receipt Paper is Custom 1", true);
        grnReceiptCustom2 = configOptionController.getBooleanValueByKey("GRN Receipt Paper is Custom 2", true);
        
        // GRN Return Settings
        grnReturnReceiptCustom1 = configOptionController.getBooleanValueByKey("GRN Return Receipt Paper is Custom 1", false);
        grnReturnReceiptCustom2 = configOptionController.getBooleanValueByKey("GRN Return Receipt Paper is Custom 2", true);

        // Transfer Receive Settings
        transferReceiveA4 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4", true);
        transferReceiveTemplate = configOptionController.getBooleanValueByKey("Pharmacy Transfer Receive Bill is Template", false);
        transferReceiveA4Detailed = configOptionController.getBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4 Detailed", true);
        transferReceiveA4Custom1 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4 Custom 1", true);
        transferReceiveA4Custom2 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4 Custom 2", true);
        transferReceiveCustom1 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Receive Receipt is Letter Paper Custom 1", true);

        // Transfer Request Settings
        transferRequestA4 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Request Receipt is A4", true);
        transferRequestCustom1 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Request Receipt is Custom 1", true);
        transferRequestCustom2 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Request Receipt is Custom 2", true);

        // Transfer Issue Settings
        transferIssueA4Paper = configOptionController.getBooleanValueByKey("Pharmacy Transfer Issue A4 Paper", true);
        transferIssueA4PaperDetailed = configOptionController.getBooleanValueByKey("Pharmacy Transfer Issue A4 Paper Detailed", false);
        transferIssuePosPaper = configOptionController.getBooleanValueByKey("Pharmacy Transfer Issue POS Paper", false);
        transferIssuePosHeaderPaper = configOptionController.getBooleanValueByKey("Pharmacy Transfer Issue Bill is PosHeaderPaper", false);
        transferIssueTemplate = configOptionController.getBooleanValueByKey("Pharmacy Transfer Issue Bill is Template", false);
        transferIssueCustom1 = configOptionController.getBooleanValueByKey("Pharmacy Transfer Issue Bill is Letter Paper Custom 1", false);
        
        // Direct Purchase Settings
        directPurchaseA4Paper = configOptionController.getBooleanValueByKey("Direct Purchase Bill Print - A4", true);
        directPurchaseA4PaperCustom1 = configOptionController.getBooleanValueByKey("Direct Purchase Bill Print - A4 (Custom 1)", true);
        directPurchaseA4Details = configOptionController.getBooleanValueByKey("Direct Purchase Bill Print - A4 Details", false);
        directPurchaseCustom1 = configOptionController.getBooleanValueByKey("Direct Purchase Bill Print - Custom 1", false);
        directPurchaseCustom2 = configOptionController.getBooleanValueByKey("Direct Purchase Bill Print - Custom 2", false);
        directPurchaseCustomLetter = configOptionController.getBooleanValueByKey("Direct Purchase Bill Print - Custom Letter Format", false);

        // Direct Purchase Return Settings
        directPurchaseReturnCustom1 = configOptionController.getBooleanValueByKey("Direct Purchase Return Receipt Paper is Custom 1", false);
        directPurchaseReturnCustom2 = configOptionController.getBooleanValueByKey("Direct Purchase Return Receipt Paper is Custom 2", true);
        
        // Disposal Issue Settings
        disposalIssueA4Paper = configOptionController.getBooleanValueByKey("Pharmacy Disposal Issue Receipt is A4 Paper", true);
        disposalIssueCustom1 = configOptionController.getBooleanValueByKey("Pharmacy Disposal Issue Receipt is Custom 1", false);
        disposalIssueCustom2 = configOptionController.getBooleanValueByKey("Pharmacy Disposal Issue Receipt is Custom 2", false);

        // Bill Return Issue Settings
        billReturnIssueA4Paper = configOptionController.getBooleanValueByKey("Pharmacy Bill Return Issue Receipt is A4 Paper", true);
        billReturnIssueCustom1 = configOptionController.getBooleanValueByKey("Pharmacy Bill Return Issue Receipt is Custom 1", false);
        billReturnIssueCustom2 = configOptionController.getBooleanValueByKey("Pharmacy Bill Return Issue Receipt is Custom 2", false);
        
        // Disposal Cancel Settings
        disposalCancelA4Paper = configOptionController.getBooleanValueByKey("Pharmacy Disposal Cancel Receipt is A4 Paper", true);
        disposalCancelCustom1 = configOptionController.getBooleanValueByKey("Pharmacy Disposal Cancel Receipt is Custom 1", false);
        disposalCancelCustom2 = configOptionController.getBooleanValueByKey("Pharmacy Disposal Cancel Receipt is Custom 2", false);

        // Cancel Bill Settings
        cancelBillPosPaper = configOptionController.getBooleanValueByKey("Pharmacy Cancel Bill Paper is POS Paper", true);
        cancelBillPosPaperCustom1 = configOptionController.getBooleanValueByKey("Pharmacy Cancel Bill Paper is POS Paper Custom 1", false);
        cancelBillFiveFivePaper = configOptionController.getBooleanValueByKey("Pharmacy Cancel Bill Paper is FiveFive Paper", true);
        cancelBillPosHeaderPaper = configOptionController.getBooleanValueByKey("Pharmacy Cancel Bill Paper is POS Header Paper", true);
        cancelBillCustom3 = configOptionController.getBooleanValueByKey("Pharmacy Cancel Bill Paper is Custom 3", true);

        // Retail Sale Return Items Only Settings (no payment)
        retailSaleReturnItemsBillFiveFiveCustom3 = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Return Items Bill is FiveFiveCustom3", true);
        retailSaleReturnItemsBillPosHeaderPaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Return Items Bill is PosHeaderPaper", false);

        // Retail Sale Return with Refund Payment Settings
        retailSaleReturnRefundBillA4Paper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Return Refund Bill is A4 Paper", true);
        retailSaleReturnRefundBillPosPaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Return Refund Bill is POS Paper", false);
        
        // Retail Sale Return Settings
        retailSaleReturnPosPaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Return Bill is POS Paper", true);
        retailSaleReturnFiveFivePaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Return Bill is Five Five Paper", true);
        retailSaleReturnPosHeaderPaper = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Return Bill is POS Header Paper", true);
        retailSaleReturnFiveFiveCustom3 = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Return Bill is Five Five Custom 3 Paper", false);
        retailSaleReturnPosPaperCustom1 = configOptionController.getBooleanValueByKey("Pharmacy Retail Sale Return Bill is POS Paper Custom 1 Paper", false);

        // Pharmacy Return Without Tresing Settings
        returnWithoutTresingPosPaper = configOptionController.getBooleanValueByKey("Pharmacy Return Without Tresing Bill is POS Paper", true);
        returnWithoutTresingFiveFivePaper = configOptionController.getBooleanValueByKey("Pharmacy Return Without Tresing Bill is FiveFive Paper", false);
        returnWithoutTresingA4Paper = configOptionController.getBooleanValueByKey("Pharmacy Return Without Tresing Bill is A4 Paper", true);

        // Credit Settlement Cancellation Settings
        creditSettlementCancellationA4 = configOptionController.getBooleanValueByKey("Credit Settlement Cancellation Receipt is A4 Paper", true);
        creditSettlementCancellationPos = configOptionController.getBooleanValueByKey("Credit Settlement Cancellation Receipt is POS Paper", false);
        creditSettlementCancellationFiveFive = configOptionController.getBooleanValueByKey("Credit Settlement Cancellation Receipt is FiveFive Paper", true);
        creditSettlementCancellationCustom1 = configOptionController.getBooleanValueByKey("Credit Settlement Cancellation Receipt is Custom 1", false);
        creditSettlementCancellationCustom2 = configOptionController.getBooleanValueByKey("Credit Settlement Cancellation Receipt is Custom 2", false);
        creditSettlementCancellationCustom3 = configOptionController.getBooleanValueByKey("Credit Settlement Cancellation Receipt is Custom 3", false);
        creditSettlementCancellationPosHeader = configOptionController.getBooleanValueByKey("Credit Settlement Cancellation Receipt is POS Header Paper", false);
        creditSettlementCancellationPosCustom1 = configOptionController.getBooleanValueByKey("Credit Settlement Cancellation Receipt is POS Custom 1", false);

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
            
            // GRN Return Settings
            configOptionController.setBooleanValueByKey("GRN Return Receipt Paper is Custom 1", grnReturnReceiptCustom1);
            configOptionController.setBooleanValueByKey("GRN Return Receipt Paper is Custom 2", grnReturnReceiptCustom2);

            // Transfer Receive Settings
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4", transferReceiveA4);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Receive Bill is Template", transferReceiveTemplate);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4 Detailed", transferReceiveA4Detailed);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4 Custom 1", transferReceiveA4Custom1);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Receive Receipt is A4 Custom 2", transferReceiveA4Custom2);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Receive Receipt is Letter Paper Custom 1", transferReceiveCustom1);

            // Transfer Request Settings
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Request Receipt is A4", transferRequestA4);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Request Receipt is Custom 1", transferRequestCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Request Receipt is Custom 2", transferRequestCustom2);

            // Transfer Issue Settings
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Issue A4 Paper", transferIssueA4Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Issue A4 Paper Detailed", transferIssueA4PaperDetailed);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Issue POS Paper", transferIssuePosPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Issue Bill is PosHeaderPaper", transferIssuePosHeaderPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Issue Bill is Template", transferIssueTemplate);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Issue Bill is Letter Paper Custom 1", transferIssueCustom1);

            // Direct Purchase Settings
            configOptionController.setBooleanValueByKey("Direct Purchase Bill Print - A4", directPurchaseA4Paper);
            configOptionController.setBooleanValueByKey("Direct Purchase Bill Print - A4 (Custom 1)", directPurchaseA4PaperCustom1);
            configOptionController.setBooleanValueByKey("Direct Purchase Bill Print - A4 Details", directPurchaseA4Details);
            configOptionController.setBooleanValueByKey("Direct Purchase Bill Print - Custom 1", directPurchaseCustom1);
            configOptionController.setBooleanValueByKey("Direct Purchase Bill Print - Custom 2", directPurchaseCustom2);
            configOptionController.setBooleanValueByKey("Direct Purchase Bill Print - Custom Letter Format", directPurchaseCustomLetter);

            // Direct Purchase Return Settings
            configOptionController.setBooleanValueByKey("Direct Purchase Return Receipt Paper is Custom 1", directPurchaseReturnCustom1);
            configOptionController.setBooleanValueByKey("Direct Purchase Return Receipt Paper is Custom 2", directPurchaseReturnCustom2);
            
            // Disposal Issue Settings
            configOptionController.setBooleanValueByKey("Pharmacy Disposal Issue Receipt is A4 Paper", disposalIssueA4Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Disposal Issue Receipt is Custom 1", disposalIssueCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy Disposal Issue Receipt is Custom 2", disposalIssueCustom2);

            // Bill Return Issue Settings
            configOptionController.setBooleanValueByKey("Pharmacy Bill Return Issue Receipt is A4 Paper", billReturnIssueA4Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Bill Return Issue Receipt is Custom 1", billReturnIssueCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy Bill Return Issue Receipt is Custom 2", billReturnIssueCustom2);
            
            // Disposal Cancel Settings
            configOptionController.setBooleanValueByKey("Pharmacy Disposal Cancel Receipt is A4 Paper", disposalCancelA4Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Disposal Cancel Receipt is Custom 1", disposalCancelCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy Disposal Cancel Receipt is Custom 2", disposalCancelCustom2);

            // Cancel Bill Settings
            configOptionController.setBooleanValueByKey("Pharmacy Cancel Bill Paper is POS Paper", cancelBillPosPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Cancel Bill Paper is POS Paper Custom 1", cancelBillPosPaperCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy Cancel Bill Paper is FiveFive Paper", cancelBillFiveFivePaper);
            configOptionController.setBooleanValueByKey("Pharmacy Cancel Bill Paper is POS Header Paper", cancelBillPosHeaderPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Cancel Bill Paper is Custom 3", cancelBillCustom3);
            
            // Retail Sale Return Settings
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Return Bill is POS Paper", retailSaleReturnPosPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Return Bill is Five Five Paper", retailSaleReturnFiveFivePaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Return Bill is POS Header Paper", retailSaleReturnPosHeaderPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Return Bill is Five Five Custom 3 Paper", retailSaleReturnFiveFiveCustom3);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Return Bill is POS Paper Custom 1 Paper", retailSaleReturnPosPaperCustom1);

            // Credit Settlement Cancellation Settings
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is A4 Paper", creditSettlementCancellationA4);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is POS Paper", creditSettlementCancellationPos);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is FiveFive Paper", creditSettlementCancellationFiveFive);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is Custom 1", creditSettlementCancellationCustom1);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is Custom 2", creditSettlementCancellationCustom2);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is Custom 3", creditSettlementCancellationCustom3);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is POS Header Paper", creditSettlementCancellationPosHeader);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is POS Custom 1", creditSettlementCancellationPosCustom1);

            JsfUtil.addSuccessMessage("Configuration saved successfully");

            // Reload current values to ensure consistency
            loadCurrentConfig();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving configuration: " + e.getMessage());
        }
    }

    /**
     * Save Transfer Issue configuration changes specifically
     */
    public void saveTransferIssueConfig() {
        try {
            // Transfer Issue Settings
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Issue A4 Paper", transferIssueA4Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Issue A4 Paper Detailed", transferIssueA4PaperDetailed);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Issue POS Paper", transferIssuePosPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Issue Bill is PosHeaderPaper", transferIssuePosHeaderPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Issue Bill is Template", transferIssueTemplate);
            configOptionController.setBooleanValueByKey("Pharmacy Transfer Issue Bill is Letter Paper Custom 1", transferIssueCustom1);

            JsfUtil.addSuccessMessage("Transfer Issue configuration saved successfully");

            // Reload current values to ensure consistency
            loadCurrentConfig();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Transfer Issue configuration: " + e.getMessage());
        }
    }

    /**
     * Save Direct Purchase configuration changes specifically
     */
    public void saveDirectPurchaseConfig() {
        try {
            // Direct Purchase Settings
            configOptionController.setBooleanValueByKey("Direct Purchase Bill Print - A4", directPurchaseA4Paper);
            configOptionController.setBooleanValueByKey("Direct Purchase Bill Print - A4 (Custom 1)", directPurchaseA4PaperCustom1);
            configOptionController.setBooleanValueByKey("Direct Purchase Bill Print - A4 Details", directPurchaseA4Details);
            configOptionController.setBooleanValueByKey("Direct Purchase Bill Print - Custom 1", directPurchaseCustom1);
            configOptionController.setBooleanValueByKey("Direct Purchase Bill Print - Custom 2", directPurchaseCustom2);
            configOptionController.setBooleanValueByKey("Direct Purchase Bill Print - Custom Letter Format", directPurchaseCustomLetter);

            JsfUtil.addSuccessMessage("Direct Purchase configuration saved successfully");

            // Reload current values to ensure consistency
            loadCurrentConfig();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Direct Purchase configuration: " + e.getMessage());
        }
    }

    /**
     * Save Disposal Issue configuration changes specifically
     */
    public void saveDisposalIssueConfig() {
        try {
            // Disposal Issue Settings
            configOptionController.setBooleanValueByKey("Pharmacy DIsposal Issue Receipt is A4 Paper", disposalIssueA4Paper);
            configOptionController.setBooleanValueByKey("Pharmacy DIsposal Issue Receipt is Custom 1", disposalIssueCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy DIsposal Issue Receipt is Custom 2", disposalIssueCustom2);

            JsfUtil.addSuccessMessage("Disposal Issue configuration saved successfully");

            // Reload current values to ensure consistency
            loadCurrentConfig();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Disposal Issue configuration: " + e.getMessage());
        }
    }

    /**
     * Save Bill Return Issue configuration changes specifically
     */
    public void saveBillReturnIssueConfig() {
        try {
            // Bill Return Issue Settings
            configOptionController.setBooleanValueByKey("Pharmacy Bill Return Issue Receipt is A4 Paper", billReturnIssueA4Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Bill Return Issue Receipt is Custom 1", billReturnIssueCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy Bill Return Issue Receipt is Custom 2", billReturnIssueCustom2);

            JsfUtil.addSuccessMessage("Bill Return Issue configuration saved successfully");

            // Reload current values to ensure consistency
            loadCurrentConfig();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Bill Return Issue configuration: " + e.getMessage());
        }
    }

    /**
     * Save Pharmacy Retail Sale configuration changes specifically
     */
    public void saveRetailSaleConfig() {
        try {
            // Pharmacy Retail Sale Paper Type Settings
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is POS Paper", posPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is POS Paper Custom 1", posPaperCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is FiveFive Paper without Blank Space for Header", fiveFivePaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is POS paper with header", posHeaderPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is Custom 1", custom1Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is Custom 2", custom2Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill Paper is Custom 3", custom3Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Bill is PosHeaderPaper", posHeaderPaperGeneral);

            JsfUtil.addSuccessMessage("Pharmacy Retail Sale configuration saved successfully");

            // Reload current values to ensure consistency
            loadCurrentConfig();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Pharmacy Retail Sale configuration: " + e.getMessage());
        }
    }

    /**
     * Save Pharmacy Cancel Bill configuration changes specifically
     */
    public void saveCancelBillConfig() {
        try {
            // Cancel Bill Settings
            configOptionController.setBooleanValueByKey("Pharmacy Cancel Bill Paper is POS Paper", cancelBillPosPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Cancel Bill Paper is POS Paper Custom 1", cancelBillPosPaperCustom1);
            configOptionController.setBooleanValueByKey("Pharmacy Cancel Bill Paper is FiveFive Paper", cancelBillFiveFivePaper);
            configOptionController.setBooleanValueByKey("Pharmacy Cancel Bill Paper is POS Header Paper", cancelBillPosHeaderPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Cancel Bill Paper is Custom 3", cancelBillCustom3);

            JsfUtil.addSuccessMessage("Pharmacy Cancel Bill configuration saved successfully");

            // Reload current values to ensure consistency
            loadCurrentConfig();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Pharmacy Cancel Bill configuration: " + e.getMessage());
        }
    }

    /**
     * Save Pharmacy Retail Sale Return Items Bill configuration changes specifically (no payment)
     */
    public void saveRetailSaleReturnItemsBillConfig() {
        try {
            // Retail Sale Return Items Bill Settings
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Return Items Bill is FiveFiveCustom3", retailSaleReturnItemsBillFiveFiveCustom3);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Return Items Bill is PosHeaderPaper", retailSaleReturnItemsBillPosHeaderPaper);

            JsfUtil.addSuccessMessage("Return Items Bill configuration saved successfully");

            // Reload current values to ensure consistency
            loadCurrentConfig();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Return Items Bill configuration: " + e.getMessage());
        }
    }

    /**
     * Save Pharmacy Retail Sale Return Refund Bill configuration changes specifically (with payment)
     */
    public void saveRetailSaleReturnRefundBillConfig() {
        try {
            // Retail Sale Return Refund Bill Settings
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Return Refund Bill is A4 Paper", retailSaleReturnRefundBillA4Paper);
            configOptionController.setBooleanValueByKey("Pharmacy Retail Sale Return Refund Bill is POS Paper", retailSaleReturnRefundBillPosPaper);

            JsfUtil.addSuccessMessage("Return Refund Bill configuration saved successfully");

            // Reload current values to ensure consistency
            loadCurrentConfig();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Return Refund Bill configuration: " + e.getMessage());
        }
    }

    /**
     * Save Pharmacy Return Without Tresing configuration changes specifically
     */
    public void saveReturnWithoutTresingConfig() {
        try {
            // Pharmacy Return Without Tresing Settings
            configOptionController.setBooleanValueByKey("Pharmacy Return Without Tresing Bill is POS Paper", returnWithoutTresingPosPaper);
            configOptionController.setBooleanValueByKey("Pharmacy Return Without Tresing Bill is FiveFive Paper", returnWithoutTresingFiveFivePaper);
            configOptionController.setBooleanValueByKey("Pharmacy Return Without Tresing Bill is A4 Paper", returnWithoutTresingA4Paper);

            JsfUtil.addSuccessMessage("Return Without Tresing configuration saved successfully");

            // Reload current values to ensure consistency
            loadCurrentConfig();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Return Without Tresing configuration: " + e.getMessage());
        }
    }

    /**
     * Save Credit Settlement Cancellation configuration changes specifically
     */
    public void saveCreditSettlementCancellationConfig() {
        try {
            // Credit Settlement Cancellation Settings
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is A4 Paper", creditSettlementCancellationA4);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is POS Paper", creditSettlementCancellationPos);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is FiveFive Paper", creditSettlementCancellationFiveFive);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is Custom 1", creditSettlementCancellationCustom1);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is Custom 2", creditSettlementCancellationCustom2);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is Custom 3", creditSettlementCancellationCustom3);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is POS Header Paper", creditSettlementCancellationPosHeader);
            configOptionController.setBooleanValueByKey("Credit Settlement Cancellation Receipt is POS Custom 1", creditSettlementCancellationPosCustom1);

            JsfUtil.addSuccessMessage("Credit Settlement Cancellation configuration saved successfully");

            // Reload current values to ensure consistency
            loadCurrentConfig();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving Credit Settlement Cancellation configuration: " + e.getMessage());
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

    // Transfer Issue Getters and Setters
    public boolean isTransferIssueA4Paper() {
        return transferIssueA4Paper;
    }

    public void setTransferIssueA4Paper(boolean transferIssueA4Paper) {
        this.transferIssueA4Paper = transferIssueA4Paper;
    }

    public boolean isTransferIssueA4PaperDetailed() {
        return transferIssueA4PaperDetailed;
    }

    public void setTransferIssueA4PaperDetailed(boolean transferIssueA4PaperDetailed) {
        this.transferIssueA4PaperDetailed = transferIssueA4PaperDetailed;
    }

    public boolean isTransferIssuePosPaper() {
        return transferIssuePosPaper;
    }

    public void setTransferIssuePosPaper(boolean transferIssuePosPaper) {
        this.transferIssuePosPaper = transferIssuePosPaper;
    }

    public boolean isTransferIssuePosHeaderPaper() {
        return transferIssuePosHeaderPaper;
    }

    public void setTransferIssuePosHeaderPaper(boolean transferIssuePosHeaderPaper) {
        this.transferIssuePosHeaderPaper = transferIssuePosHeaderPaper;
    }

    public boolean isTransferIssueTemplate() {
        return transferIssueTemplate;
    }

    public void setTransferIssueTemplate(boolean transferIssueTemplate) {
        this.transferIssueTemplate = transferIssueTemplate;
    }

    // Direct Purchase Getters and Setters
    public boolean isDirectPurchaseA4Paper() {
        return directPurchaseA4Paper;
    }

    public void setDirectPurchaseA4Paper(boolean directPurchaseA4Paper) {
        this.directPurchaseA4Paper = directPurchaseA4Paper;
    }

    public boolean isDirectPurchaseA4Details() {
        return directPurchaseA4Details;
    }

    public void setDirectPurchaseA4Details(boolean directPurchaseA4Details) {
        this.directPurchaseA4Details = directPurchaseA4Details;
    }

    public boolean isDirectPurchaseCustom1() {
        return directPurchaseCustom1;
    }

    public void setDirectPurchaseCustom1(boolean directPurchaseCustom1) {
        this.directPurchaseCustom1 = directPurchaseCustom1;
    }

    public boolean isDirectPurchaseCustom2() {
        return directPurchaseCustom2;
    }

    public void setDirectPurchaseCustom2(boolean directPurchaseCustom2) {
        this.directPurchaseCustom2 = directPurchaseCustom2;
    }

    public boolean isTransferIssueCustom1() {
        return transferIssueCustom1;
    }

    public void setTransferIssueCustom1(boolean transferIssueCustom1) {
        this.transferIssueCustom1 = transferIssueCustom1;
    }

    public boolean isDirectPurchaseCustomLetter() {
        return directPurchaseCustomLetter;
    }

    public void setDirectPurchaseCustomLetter(boolean directPurchaseCustomLetter) {
        this.directPurchaseCustomLetter = directPurchaseCustomLetter;
    }

    public boolean isTransferReceiveCustom1() {
        return transferReceiveCustom1;
    }

    public void setTransferReceiveCustom1(boolean transferReceiveCustom1) {
        this.transferReceiveCustom1 = transferReceiveCustom1;
    }

    // Disposal Issue Getters and Setters
    public boolean isDisposalIssueA4Paper() {
        return disposalIssueA4Paper;
    }

    public void setDisposalIssueA4Paper(boolean disposalIssueA4Paper) {
        this.disposalIssueA4Paper = disposalIssueA4Paper;
    }

    public boolean isDisposalIssueCustom1() {
        return disposalIssueCustom1;
    }

    public void setDisposalIssueCustom1(boolean disposalIssueCustom1) {
        this.disposalIssueCustom1 = disposalIssueCustom1;
    }

    public boolean isDisposalIssueCustom2() {
        return disposalIssueCustom2;
    }

    public void setDisposalIssueCustom2(boolean disposalIssueCustom2) {
        this.disposalIssueCustom2 = disposalIssueCustom2;
    }

    // Bill Return Issue Getters and Setters
    public boolean isBillReturnIssueA4Paper() {
        return billReturnIssueA4Paper;
    }

    public void setBillReturnIssueA4Paper(boolean billReturnIssueA4Paper) {
        this.billReturnIssueA4Paper = billReturnIssueA4Paper;
    }

    public boolean isBillReturnIssueCustom1() {
        return billReturnIssueCustom1;
    }

    public void setBillReturnIssueCustom1(boolean billReturnIssueCustom1) {
        this.billReturnIssueCustom1 = billReturnIssueCustom1;
    }

    public boolean isBillReturnIssueCustom2() {
        return billReturnIssueCustom2;
    }

    public void setBillReturnIssueCustom2(boolean billReturnIssueCustom2) {
        this.billReturnIssueCustom2 = billReturnIssueCustom2;
    }

    public boolean isGrnReturnReceiptCustom1() {
        return grnReturnReceiptCustom1;
    }

    public void setGrnReturnReceiptCustom1(boolean grnReturnReceiptCustom1) {
        this.grnReturnReceiptCustom1 = grnReturnReceiptCustom1;
    }

    public boolean isGrnReturnReceiptCustom2() {
        return grnReturnReceiptCustom2;
    }

    public void setGrnReturnReceiptCustom2(boolean grnReturnReceiptCustom2) {
        this.grnReturnReceiptCustom2 = grnReturnReceiptCustom2;
    }

    // Cancel Bill Getters and Setters
    public boolean isCancelBillPosPaper() {
        return cancelBillPosPaper;
    }

    public void setCancelBillPosPaper(boolean cancelBillPosPaper) {
        this.cancelBillPosPaper = cancelBillPosPaper;
    }

    public boolean isCancelBillPosPaperCustom1() {
        return cancelBillPosPaperCustom1;
    }

    public void setCancelBillPosPaperCustom1(boolean cancelBillPosPaperCustom1) {
        this.cancelBillPosPaperCustom1 = cancelBillPosPaperCustom1;
    }

    public boolean isCancelBillFiveFivePaper() {
        return cancelBillFiveFivePaper;
    }

    public void setCancelBillFiveFivePaper(boolean cancelBillFiveFivePaper) {
        this.cancelBillFiveFivePaper = cancelBillFiveFivePaper;
    }

    public boolean isCancelBillPosHeaderPaper() {
        return cancelBillPosHeaderPaper;
    }

    public void setCancelBillPosHeaderPaper(boolean cancelBillPosHeaderPaper) {
        this.cancelBillPosHeaderPaper = cancelBillPosHeaderPaper;
    }

    public boolean isCancelBillCustom3() {
        return cancelBillCustom3;
    }

    public void setCancelBillCustom3(boolean cancelBillCustom3) {
        this.cancelBillCustom3 = cancelBillCustom3;
    }

    public boolean isDirectPurchaseReturnCustom1() {
        return directPurchaseReturnCustom1;
    }

    public void setDirectPurchaseReturnCustom1(boolean directPurchaseReturnCustom1) {
        this.directPurchaseReturnCustom1 = directPurchaseReturnCustom1;
    }

    public boolean isDirectPurchaseReturnCustom2() {
        return directPurchaseReturnCustom2;
    }

    public void setDirectPurchaseReturnCustom2(boolean directPurchaseReturnCustom2) {
        this.directPurchaseReturnCustom2 = directPurchaseReturnCustom2;
    }

    // Retail Sale Return Items Bill Getters and Setters
    public boolean isRetailSaleReturnItemsBillFiveFiveCustom3() {
        return retailSaleReturnItemsBillFiveFiveCustom3;
    }

    public void setRetailSaleReturnItemsBillFiveFiveCustom3(boolean retailSaleReturnItemsBillFiveFiveCustom3) {
        this.retailSaleReturnItemsBillFiveFiveCustom3 = retailSaleReturnItemsBillFiveFiveCustom3;
    }

    public boolean isRetailSaleReturnItemsBillPosHeaderPaper() {
        return retailSaleReturnItemsBillPosHeaderPaper;
    }

    public void setRetailSaleReturnItemsBillPosHeaderPaper(boolean retailSaleReturnItemsBillPosHeaderPaper) {
        this.retailSaleReturnItemsBillPosHeaderPaper = retailSaleReturnItemsBillPosHeaderPaper;
    }

    // Retail Sale Return Refund Bill Getters and Setters
    public boolean isRetailSaleReturnRefundBillA4Paper() {
        return retailSaleReturnRefundBillA4Paper;
    }

    public void setRetailSaleReturnRefundBillA4Paper(boolean retailSaleReturnRefundBillA4Paper) {
        this.retailSaleReturnRefundBillA4Paper = retailSaleReturnRefundBillA4Paper;
    }

    public boolean isRetailSaleReturnRefundBillPosPaper() {
        return retailSaleReturnRefundBillPosPaper;
    }

    public void setRetailSaleReturnRefundBillPosPaper(boolean retailSaleReturnRefundBillPosPaper) {
        this.retailSaleReturnRefundBillPosPaper = retailSaleReturnRefundBillPosPaper;
    }

    public boolean isDisposalCancelA4Paper() {
        return disposalCancelA4Paper;
    }

    public void setDisposalCancelA4Paper(boolean disposalCancelA4Paper) {
        this.disposalCancelA4Paper = disposalCancelA4Paper;
    }

    public boolean isDisposalCancelCustom1() {
        return disposalCancelCustom1;
    }

    public void setDisposalCancelCustom1(boolean disposalCancelCustom1) {
        this.disposalCancelCustom1 = disposalCancelCustom1;
    }

    public boolean isDisposalCancelCustom2() {
        return disposalCancelCustom2;
    }

    public void setDisposalCancelCustom2(boolean disposalCancelCustom2) {
        this.disposalCancelCustom2 = disposalCancelCustom2;
    }

    public boolean isDirectPurchaseA4PaperCustom1() {
        return directPurchaseA4PaperCustom1;
    }

    public void setDirectPurchaseA4PaperCustom1(boolean directPurchaseA4PaperCustom1) {
        this.directPurchaseA4PaperCustom1 = directPurchaseA4PaperCustom1;
    }

    public boolean isRetailSaleReturnPosPaper() {
        return retailSaleReturnPosPaper;
    }

    public void setRetailSaleReturnPosPaper(boolean retailSaleReturnPosPaper) {
        this.retailSaleReturnPosPaper = retailSaleReturnPosPaper;
    }

    public boolean isRetailSaleReturnFiveFivePaper() {
        return retailSaleReturnFiveFivePaper;
    }

    public void setRetailSaleReturnFiveFivePaper(boolean retailSaleReturnFiveFivePaper) {
        this.retailSaleReturnFiveFivePaper = retailSaleReturnFiveFivePaper;
    }

    public boolean isRetailSaleReturnPosHeaderPaper() {
        return retailSaleReturnPosHeaderPaper;
    }

    public void setRetailSaleReturnPosHeaderPaper(boolean retailSaleReturnPosHeaderPaper) {
        this.retailSaleReturnPosHeaderPaper = retailSaleReturnPosHeaderPaper;
    }

    public boolean isRetailSaleReturnFiveFiveCustom3() {
        return retailSaleReturnFiveFiveCustom3;
    }

    public void setRetailSaleReturnFiveFiveCustom3(boolean retailSaleReturnFiveFiveCustom3) {
        this.retailSaleReturnFiveFiveCustom3 = retailSaleReturnFiveFiveCustom3;
    }

    public boolean isRetailSaleReturnPosPaperCustom1() {
        return retailSaleReturnPosPaperCustom1;
    }

    public void setRetailSaleReturnPosPaperCustom1(boolean retailSaleReturnPosPaperCustom1) {
        this.retailSaleReturnPosPaperCustom1 = retailSaleReturnPosPaperCustom1;
    }

    // Pharmacy Return Without Tresing Getters and Setters
    public boolean isReturnWithoutTresingPosPaper() {
        return returnWithoutTresingPosPaper;
    }

    public void setReturnWithoutTresingPosPaper(boolean returnWithoutTresingPosPaper) {
        this.returnWithoutTresingPosPaper = returnWithoutTresingPosPaper;
    }

    public boolean isReturnWithoutTresingFiveFivePaper() {
        return returnWithoutTresingFiveFivePaper;
    }

    public void setReturnWithoutTresingFiveFivePaper(boolean returnWithoutTresingFiveFivePaper) {
        this.returnWithoutTresingFiveFivePaper = returnWithoutTresingFiveFivePaper;
    }

    public boolean isReturnWithoutTresingA4Paper() {
        return returnWithoutTresingA4Paper;
    }

    public void setReturnWithoutTresingA4Paper(boolean returnWithoutTresingA4Paper) {
        this.returnWithoutTresingA4Paper = returnWithoutTresingA4Paper;
    }

    // Credit Settlement Cancellation Getters and Setters
    public boolean isCreditSettlementCancellationA4() {
        return creditSettlementCancellationA4;
    }

    public void setCreditSettlementCancellationA4(boolean creditSettlementCancellationA4) {
        this.creditSettlementCancellationA4 = creditSettlementCancellationA4;
    }

    public boolean isCreditSettlementCancellationPos() {
        return creditSettlementCancellationPos;
    }

    public void setCreditSettlementCancellationPos(boolean creditSettlementCancellationPos) {
        this.creditSettlementCancellationPos = creditSettlementCancellationPos;
    }

    public boolean isCreditSettlementCancellationCustom1() {
        return creditSettlementCancellationCustom1;
    }

    public void setCreditSettlementCancellationCustom1(boolean creditSettlementCancellationCustom1) {
        this.creditSettlementCancellationCustom1 = creditSettlementCancellationCustom1;
    }

    public boolean isCreditSettlementCancellationCustom2() {
        return creditSettlementCancellationCustom2;
    }

    public void setCreditSettlementCancellationCustom2(boolean creditSettlementCancellationCustom2) {
        this.creditSettlementCancellationCustom2 = creditSettlementCancellationCustom2;
    }

    public boolean isCreditSettlementCancellationPosHeader() {
        return creditSettlementCancellationPosHeader;
    }

    public void setCreditSettlementCancellationPosHeader(boolean creditSettlementCancellationPosHeader) {
        this.creditSettlementCancellationPosHeader = creditSettlementCancellationPosHeader;
    }

    public boolean isCreditSettlementCancellationFiveFive() {
        return creditSettlementCancellationFiveFive;
    }

    public void setCreditSettlementCancellationFiveFive(boolean creditSettlementCancellationFiveFive) {
        this.creditSettlementCancellationFiveFive = creditSettlementCancellationFiveFive;
    }

    public boolean isCreditSettlementCancellationCustom3() {
        return creditSettlementCancellationCustom3;
    }

    public void setCreditSettlementCancellationCustom3(boolean creditSettlementCancellationCustom3) {
        this.creditSettlementCancellationCustom3 = creditSettlementCancellationCustom3;
    }

    public boolean isCreditSettlementCancellationPosCustom1() {
        return creditSettlementCancellationPosCustom1;
    }

    public void setCreditSettlementCancellationPosCustom1(boolean creditSettlementCancellationPosCustom1) {
        this.creditSettlementCancellationPosCustom1 = creditSettlementCancellationPosCustom1;
    }

}