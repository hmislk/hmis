package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * One row of the Ordering Requirement Report (issue #22466).
 *
 * Assembled in Java by PharmacyOrderingRequirementService - this DTO is not
 * produced by a constructor JPQL query, because every figure on it depends on
 * the day-by-day balance walk rather than on any single aggregate.
 *
 * @author Buddhika
 */
public class OrderingRequirementRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DECISION_URGENT_ORDER = "Urgent Order";
    public static final String DECISION_ORDER = "Order";
    public static final String DECISION_NO_ORDER = "No Order";

    private Long itemId;
    private String itemName;
    private String code;

    /**
     * Name of the institution the item was most recently purchased from, across
     * all time rather than just the report window - a buyer wants to know who
     * they last bought from even if that was before the selected period.
     * Filled by a separate batched lookup, not by the main aggregate.
     */
    private String lastSupplier;

    /** Current stock in scope, from the Stock table. */
    private double currentBalance;
    /** Consumption over the whole window, net of returns, positive. */
    private double consumption;
    /** Consumption divided by the days in the window, scaled to a mean month. */
    private double avgMonthlyConsumption;
    private int windowDays;

    /** Months of cover the current balance provides at the adjusted average. */
    private Double stockCover;
    private double targetStock;
    private double quantityToOrder;
    private double lastPurchaseRate;
    private double estimatedCost;
    private String decision;

    /**
     * True when the item had no inbound bill in the window, so lastPurchaseRate
     * (and therefore estimatedCost) could not be established. Flagged on the
     * page rather than silently shown as zero.
     */
    private boolean purchaseRateUnknown;

    public OrderingRequirementRowDto() {
    }

    public OrderingRequirementRowDto(Long itemId, String itemName, String code) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.code = code;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLastSupplier() {
        return lastSupplier;
    }

    public void setLastSupplier(String lastSupplier) {
        this.lastSupplier = lastSupplier;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(double currentBalance) {
        this.currentBalance = currentBalance;
    }

    public double getConsumption() {
        return consumption;
    }

    public void setConsumption(double consumption) {
        this.consumption = consumption;
    }

    public double getAvgMonthlyConsumption() {
        return avgMonthlyConsumption;
    }

    public void setAvgMonthlyConsumption(double avgMonthlyConsumption) {
        this.avgMonthlyConsumption = avgMonthlyConsumption;
    }

    public int getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(int windowDays) {
        this.windowDays = windowDays;
    }

    public Double getStockCover() {
        return stockCover;
    }

    public void setStockCover(Double stockCover) {
        this.stockCover = stockCover;
    }

    public double getTargetStock() {
        return targetStock;
    }

    public void setTargetStock(double targetStock) {
        this.targetStock = targetStock;
    }

    public double getQuantityToOrder() {
        return quantityToOrder;
    }

    public void setQuantityToOrder(double quantityToOrder) {
        this.quantityToOrder = quantityToOrder;
    }

    public double getLastPurchaseRate() {
        return lastPurchaseRate;
    }

    public void setLastPurchaseRate(double lastPurchaseRate) {
        this.lastPurchaseRate = lastPurchaseRate;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public boolean isPurchaseRateUnknown() {
        return purchaseRateUnknown;
    }

    public void setPurchaseRateUnknown(boolean purchaseRateUnknown) {
        this.purchaseRateUnknown = purchaseRateUnknown;
    }

    /**
     * Stock cover formatted for display - "-" when there was no consumption in
     * the window, so the ratio is undefined rather than infinite.
     */
    public String getStockCoverDisplay() {
        if (stockCover == null) {
            return "-";
        }
        return String.format("%.1f", stockCover);
    }

    /** True when this row was decided Urgent Order - drives the red badge. */
    public boolean isUrgent() {
        return DECISION_URGENT_ORDER.equals(decision);
    }

    /** True when this row was decided Order - drives the amber badge. */
    public boolean isOrder() {
        return DECISION_ORDER.equals(decision);
    }
}
