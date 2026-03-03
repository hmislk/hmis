package com.divudi.core.data.dto;

import java.util.List;

/**
 * DTO for the AF15 Daily Stock Values Report.
 * Contains all components needed to reproduce the full report via API,
 * plus a balance check for AI agent discrepancy detection.
 *
 * Balance formula (verified against production data):
 *   expectedClosing = opening + sum(all signed stockValueAtRetailRate across all bundles)
 *   - Sales: negative (stock flowing out)
 *   - Purchases: positive (stock flowing in)
 *   - Transfers: mixed sign per bill type (ISSUE=negative, RECEIVE=positive)
 *   - Adjustments: mixed sign
 */
public class PharmacyF15ReportDTO {

    private String date;
    private Long departmentId;
    private String departmentName;

    private StockValueDTO openingStock;
    private BundleDTO sales;
    private BundleDTO purchases;
    private BundleDTO transfers;
    private BundleDTO adjustments;
    private StockValueDTO closingStock;
    private BalanceCheckDTO balanceCheck;
    /**
     * Informational section: pre-bill retail sales (PRE_TO_SETTLE_AT_CASHIER) and
     * their cancellations (CANCELLED_PRE) created within the report period.
     * Stock moved on these bills but financial settlement may be on a different day.
     * Use the stock values here to understand any closing-stock discrepancy.
     */
    private BundleDTO preBillStockMovements;

    // -------------------------------------------------------------------------
    // Inner DTOs
    // -------------------------------------------------------------------------

    public static class StockValueDTO {
        private double retailRate;
        private double costRate;

        public StockValueDTO() {}

        public StockValueDTO(double retailRate, double costRate) {
            this.retailRate = retailRate;
            this.costRate = costRate;
        }

        public double getRetailRate() { return retailRate; }
        public void setRetailRate(double retailRate) { this.retailRate = retailRate; }
        public double getCostRate() { return costRate; }
        public void setCostRate(double costRate) { this.costRate = costRate; }
    }

    /**
     * One row in a bundle (e.g. "PHARMACY_RETAIL_SALE - No Discount Scheme").
     * stockValueAtRetailRate is already signed:
     *   positive = stock flowed INTO this department
     *   negative = stock flowed OUT of this department
     */
    public static class BundleRowDTO {
        private String type;
        private double grossTotal;
        private double discount;
        private double serviceCharge;
        private double netTotal;
        private double stockValueAtCostRate;
        private double stockValueAtRetailRate;

        public BundleRowDTO() {}

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public double getGrossTotal() { return grossTotal; }
        public void setGrossTotal(double grossTotal) { this.grossTotal = grossTotal; }
        public double getDiscount() { return discount; }
        public void setDiscount(double discount) { this.discount = discount; }
        public double getServiceCharge() { return serviceCharge; }
        public void setServiceCharge(double serviceCharge) { this.serviceCharge = serviceCharge; }
        public double getNetTotal() { return netTotal; }
        public void setNetTotal(double netTotal) { this.netTotal = netTotal; }
        public double getStockValueAtCostRate() { return stockValueAtCostRate; }
        public void setStockValueAtCostRate(double stockValueAtCostRate) { this.stockValueAtCostRate = stockValueAtCostRate; }
        public double getStockValueAtRetailRate() { return stockValueAtRetailRate; }
        public void setStockValueAtRetailRate(double stockValueAtRetailRate) { this.stockValueAtRetailRate = stockValueAtRetailRate; }
    }

    public static class BundleDTO {
        private List<BundleRowDTO> rows;
        private BundleRowDTO totals;

        public BundleDTO() {}

        public List<BundleRowDTO> getRows() { return rows; }
        public void setRows(List<BundleRowDTO> rows) { this.rows = rows; }
        public BundleRowDTO getTotals() { return totals; }
        public void setTotals(BundleRowDTO totals) { this.totals = totals; }
    }

    public static class BalanceCheckDTO {
        private double expectedClosingAtRetailRate;
        private double actualClosingAtRetailRate;
        private double discrepancyAtRetailRate;
        private boolean balanced;
        private double toleranceUsed;
        private String formula;

        public BalanceCheckDTO() {}

        public double getExpectedClosingAtRetailRate() { return expectedClosingAtRetailRate; }
        public void setExpectedClosingAtRetailRate(double v) { this.expectedClosingAtRetailRate = v; }
        public double getActualClosingAtRetailRate() { return actualClosingAtRetailRate; }
        public void setActualClosingAtRetailRate(double v) { this.actualClosingAtRetailRate = v; }
        public double getDiscrepancyAtRetailRate() { return discrepancyAtRetailRate; }
        public void setDiscrepancyAtRetailRate(double v) { this.discrepancyAtRetailRate = v; }
        public boolean isBalanced() { return balanced; }
        public void setBalanced(boolean balanced) { this.balanced = balanced; }
        public double getToleranceUsed() { return toleranceUsed; }
        public void setToleranceUsed(double toleranceUsed) { this.toleranceUsed = toleranceUsed; }
        public String getFormula() { return formula; }
        public void setFormula(String formula) { this.formula = formula; }
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public StockValueDTO getOpeningStock() { return openingStock; }
    public void setOpeningStock(StockValueDTO openingStock) { this.openingStock = openingStock; }
    public BundleDTO getSales() { return sales; }
    public void setSales(BundleDTO sales) { this.sales = sales; }
    public BundleDTO getPurchases() { return purchases; }
    public void setPurchases(BundleDTO purchases) { this.purchases = purchases; }
    public BundleDTO getTransfers() { return transfers; }
    public void setTransfers(BundleDTO transfers) { this.transfers = transfers; }
    public BundleDTO getAdjustments() { return adjustments; }
    public void setAdjustments(BundleDTO adjustments) { this.adjustments = adjustments; }
    public StockValueDTO getClosingStock() { return closingStock; }
    public void setClosingStock(StockValueDTO closingStock) { this.closingStock = closingStock; }
    public BalanceCheckDTO getBalanceCheck() { return balanceCheck; }
    public void setBalanceCheck(BalanceCheckDTO balanceCheck) { this.balanceCheck = balanceCheck; }
    public BundleDTO getPreBillStockMovements() { return preBillStockMovements; }
    public void setPreBillStockMovements(BundleDTO preBillStockMovements) { this.preBillStockMovements = preBillStockMovements; }
}
