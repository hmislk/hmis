package com.divudi.service.pharmacy;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import javax.ejb.Stateless;

@Stateless
public class PharmacyCostingService {

    /**
     * Recalculate line-level financial values before adding a BillItem to a
     * bill.
     *
     * @param billItemFinanceDetails
     */
    public void recalculateFinancialsBeforeAddingBillItem(BillItemFinanceDetails billItemFinanceDetails) {
        System.out.println("recalculateFinancialsBeforeAddingBillItem");
        if (billItemFinanceDetails == null || billItemFinanceDetails.getBillItem() == null) {
            return;
        }
        BillItem billItem = billItemFinanceDetails.getBillItem();
        if (billItem.getPharmaceuticalBillItem() == null) {
            return;
        }
        PharmaceuticalBillItem pbi = billItem.getPharmaceuticalBillItem();

        BigDecimal qty = Optional.ofNullable(billItemFinanceDetails.getQuantity()).orElse(BigDecimal.ZERO);
        System.out.println("qty = " + qty);
        BigDecimal freeQty = Optional.ofNullable(billItemFinanceDetails.getFreeQuantity()).orElse(BigDecimal.ZERO);
        System.out.println("freeQty = " + freeQty);
        BigDecimal lineGrossRate = Optional.ofNullable(billItemFinanceDetails.getLineGrossRate()).orElse(BigDecimal.ZERO);
        BigDecimal lineDiscountRate = Optional.ofNullable(billItemFinanceDetails.getLineDiscountRate()).orElse(BigDecimal.ZERO);
        BigDecimal retailRate = Optional.ofNullable(billItemFinanceDetails.getRetailSaleRate()).orElse(BigDecimal.ZERO);

        Item item = billItemFinanceDetails.getBillItem().getItem();
        BigDecimal totalQty = qty.add(freeQty);
        System.out.println("totalQty = " + totalQty);

        BigDecimal unitsPerPack;
        BigDecimal qtyInUnits;
        BigDecimal freeQtyInUnits;
        BigDecimal totalQtyInUnits;
        if (item instanceof Ampp) {
            System.out.println("Ampp");
            double dblVal = item.getDblValue();
            unitsPerPack = dblVal > 0.0 ? BigDecimal.valueOf(dblVal) : BigDecimal.ONE;
            System.out.println("unitsPerPack = " + unitsPerPack);
            qtyInUnits = qty.multiply(unitsPerPack);
            System.out.println("qtyInUnits = " + qtyInUnits);
            freeQtyInUnits = freeQty.multiply(unitsPerPack);
            System.out.println("freeQtyInUnits = " + freeQtyInUnits);
            totalQtyInUnits = totalQty.multiply(unitsPerPack);
            System.out.println("totalQtyInUnits = " + totalQtyInUnits);
        } else {
            System.out.println("Amp");
            unitsPerPack = BigDecimal.ONE;
            qtyInUnits = qty;
            System.out.println("qtyInUnits = " + qtyInUnits);
            freeQtyInUnits = freeQty;
            System.out.println("freeQtyInUnits = " + freeQtyInUnits);
            totalQtyInUnits = totalQty;
            System.out.println("totalQtyInUnits = " + totalQtyInUnits);
        }

        billItemFinanceDetails.setUnitsPerPack(unitsPerPack);
        billItemFinanceDetails.setQuantityByUnits(qtyInUnits);
        billItemFinanceDetails.setFreeQuantityByUnits(freeQtyInUnits);
        billItemFinanceDetails.setTotalQuantityByUnits(totalQtyInUnits);

        BigDecimal lineGrossTotal = lineGrossRate.multiply(qty);
        BigDecimal lineDiscountValue = lineDiscountRate.multiply(qty);
        BigDecimal lineNetTotal = lineGrossTotal.subtract(lineDiscountValue);
        BigDecimal lineCostRate = totalQtyInUnits.compareTo(BigDecimal.ZERO) > 0
                ? lineNetTotal.divide(totalQtyInUnits, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal retailValue = retailRate.multiply(totalQtyInUnits);

        billItemFinanceDetails.setLineGrossRate(lineGrossRate);
        billItemFinanceDetails.setLineNetRate(qty.compareTo(BigDecimal.ZERO) > 0
                ? lineNetTotal.divide(qty, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        billItemFinanceDetails.setRetailSaleRatePerUnit(
                unitsPerPack.compareTo(BigDecimal.ZERO) > 0
                ? retailRate.divide(unitsPerPack, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO
        );

        billItemFinanceDetails.setLineDiscount(lineDiscountValue);
        billItemFinanceDetails.setLineGrossTotal(lineGrossTotal);
        billItemFinanceDetails.setLineNetTotal(lineNetTotal);
        billItemFinanceDetails.setLineCost(lineNetTotal);
        billItemFinanceDetails.setLineCostRate(lineCostRate);
        billItemFinanceDetails.setTotalQuantity(totalQty);

        pbi.setRetailRate(billItemFinanceDetails.getRetailSaleRate().doubleValue());
        pbi.setRetailValue(retailValue.doubleValue());
    }

    /**
     * Distribute bill-level values (discounts, expenses, taxes) proportionally
     * to items.
     * @param billItems
     * @param bill
     */
    public void distributeProportionalBillValuesToItems(List<BillItem> billItems, Bill bill) {
        if (bill == null || bill.getBillFinanceDetails() == null || billItems == null || billItems.isEmpty()) {
            return;
        }

        BigDecimal totalBasis = BigDecimal.ZERO;
        Map<BillItem, BigDecimal> itemBases = new HashMap<>();
        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }
            BigDecimal qty = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal lineGrossRate = Optional.ofNullable(f.getLineGrossRate()).orElse(BigDecimal.ZERO);
            BigDecimal basis = lineGrossRate.multiply(qty.add(freeQty));
            itemBases.put(bi, basis);
            totalBasis = totalBasis.add(basis);
        }

        if (totalBasis.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        bill.getBillFinanceDetails().setBillDiscount(BigDecimal.valueOf(bill.getDiscount()));
        bill.getBillFinanceDetails().setBillTaxValue(BigDecimal.valueOf(bill.getTax()));
        bill.getBillFinanceDetails().setBillExpense(BigDecimal.valueOf(bill.getExpenseTotal()));

        BigDecimal billDiscountTotal = Optional.ofNullable(bill.getBillFinanceDetails().getBillDiscount()).orElse(BigDecimal.ZERO);
        BigDecimal billExpenseTotal = Optional.ofNullable(bill.getBillFinanceDetails().getBillExpense()).orElse(BigDecimal.ZERO);
        BigDecimal billTaxTotal = Optional.ofNullable(bill.getBillFinanceDetails().getBillTaxValue()).orElse(BigDecimal.ZERO);

        for (BillItem bi : billItems) {
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
            if (f == null) {
                continue;
            }
            BigDecimal basis = itemBases.get(bi);
            BigDecimal ratio = basis.divide(totalBasis, 12, RoundingMode.HALF_UP);

            BigDecimal lineDiscount = Optional.ofNullable(f.getLineDiscount()).orElse(BigDecimal.ZERO);
            BigDecimal lineExpense = Optional.ofNullable(f.getLineExpense()).orElse(BigDecimal.ZERO);
            BigDecimal lineTax = Optional.ofNullable(f.getLineTax()).orElse(BigDecimal.ZERO);
            BigDecimal lineNetTotal = Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO);
            BigDecimal lineGrossTotal = Optional.ofNullable(f.getLineGrossTotal()).orElse(BigDecimal.ZERO);
            BigDecimal lineGrossRate = Optional.ofNullable(f.getLineGrossRate()).orElse(BigDecimal.ZERO);

            BigDecimal billDiscount = billDiscountTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal billExpense = billExpenseTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal billTax = billTaxTotal.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

            f.setBillDiscount(billDiscount);
            f.setBillExpense(billExpense);
            f.setBillTax(billTax);

            BigDecimal totalDiscount = lineDiscount.add(billDiscount);
            BigDecimal totalExpense = lineExpense.add(billExpense);
            BigDecimal totalTax = lineTax.add(billTax);

            f.setTotalDiscount(totalDiscount);
            f.setTotalExpense(totalExpense);
            f.setTotalTax(totalTax);

            BigDecimal quantity = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
            BigDecimal totalQty = quantity.add(freeQty);
            f.setTotalQuantity(totalQty);

            BigDecimal billDiscountRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? billDiscount.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setBillDiscountRate(billDiscountRate);

            BigDecimal totalDiscountRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? totalDiscount.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setTotalDiscountRate(totalDiscountRate);

            BigDecimal netTotal = lineGrossTotal.subtract(totalDiscount).add(totalTax).add(totalExpense);
            f.setNetTotal(netTotal);
            f.setTotalCost(netTotal);

            BigDecimal billCost = netTotal.subtract(lineNetTotal);
            f.setBillCost(billCost);

            BigDecimal qtyUnits = Optional.ofNullable(f.getTotalQuantityByUnits())
                    .orElse(totalQty);

            BigDecimal lineCostRate = qtyUnits.compareTo(BigDecimal.ZERO) > 0
                    ? lineNetTotal.divide(qtyUnits, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal billCostRate = qtyUnits.compareTo(BigDecimal.ZERO) > 0
                    ? billCost.divide(qtyUnits, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal totalCostRate = qtyUnits.compareTo(BigDecimal.ZERO) > 0
                    ? netTotal.divide(qtyUnits, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            f.setLineCostRate(lineCostRate.setScale(4, RoundingMode.HALF_UP));
            f.setBillCostRate(billCostRate.setScale(4, RoundingMode.HALF_UP));
            f.setTotalCostRate(totalCostRate.setScale(4, RoundingMode.HALF_UP));

            f.setLineGrossRate(lineGrossRate);
            f.setBillGrossRate(BigDecimal.ZERO);
            f.setGrossRate(lineGrossRate);

            f.setLineGrossTotal(lineGrossTotal);
            f.setBillGrossTotal(BigDecimal.ZERO);
            f.setGrossTotal(lineGrossTotal);

            if (f.getLineNetRate() == null || f.getLineNetRate().compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal lineNetRate = quantity.compareTo(BigDecimal.ZERO) > 0
                        ? lineNetTotal.divide(quantity, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                f.setLineNetRate(lineNetRate);
            }

            BigDecimal billNetRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? billCost.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setBillNetRate(billNetRate);

            BigDecimal netRate = quantity.compareTo(BigDecimal.ZERO) > 0
                    ? netTotal.divide(quantity, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            f.setNetRate(netRate);
        }
    }

    public double calculateProfitMarginForPurchases(BillItem bi) {
        if (bi == null) {
            System.out.println("BillItem is null");
            return 0.0;
        }

        BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
        if (f == null) {
            System.out.println("BillItemFinanceDetails is null");
            return 0.0;
        }

        BigDecimal purchaseRate = f.getLineNetRate();
        BigDecimal retailRate = f.getRetailSaleRate();
        BigDecimal qty = f.getQuantity();
        BigDecimal freeQty = f.getFreeQuantity();

        if (purchaseRate == null || retailRate == null || qty == null || freeQty == null) {
            System.out.println("One or more required fields are null:");
            System.out.println("purchaseRate: " + purchaseRate);
            System.out.println("retailRate: " + retailRate);
            System.out.println("qty: " + qty);
            System.out.println("freeQty: " + freeQty);
            return 0.0;
        }

        BigDecimal totalQty = qty.add(freeQty);
        BigDecimal purchaseValue = purchaseRate.multiply(qty);
        BigDecimal retailValue = retailRate.multiply(totalQty);

        if (purchaseValue.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("Purchase value is zero, cannot divide");
            return 0.0;
        }

        BigDecimal margin = retailValue.subtract(purchaseValue)
                .divide(purchaseValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        System.out.println("Purchase Rate: " + purchaseRate);
        System.out.println("Retail Rate: " + retailRate);
        System.out.println("Quantity: " + qty);
        System.out.println("Free Quantity: " + freeQty);
        System.out.println("Total Quantity: " + totalQty);
        System.out.println("Purchase Value: " + purchaseValue);
        System.out.println("Retail Value: " + retailValue);
        System.out.println("Profit Margin: " + margin);

        return margin.doubleValue();
    }

    
    public void calculateBillTotalsFromItemsForPurchases(Bill bill, List<BillItem> billItems) {
        int serialNo = 0;

        // Only bill-level values provided by user
        BigDecimal billDiscount = BigDecimal.valueOf(bill.getDiscount());
        BigDecimal billExpense = BigDecimal.valueOf(bill.getExpenseTotal());
        BigDecimal billTax = BigDecimal.valueOf(bill.getTax());
        BigDecimal billCost = billDiscount.subtract(billExpense.add(billTax));

        // Initialize totals
        BigDecimal totalLineDiscounts = BigDecimal.ZERO;
        BigDecimal totalLineExpenses = BigDecimal.ZERO;
        BigDecimal totalLineCosts = BigDecimal.ZERO;
        BigDecimal totalTaxLines = BigDecimal.ZERO;

        BigDecimal totalFreeItemValue = BigDecimal.ZERO;
        BigDecimal totalPurchase = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;
        BigDecimal totalWholesale = BigDecimal.ZERO;

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalFreeQty = BigDecimal.ZERO;
        BigDecimal totalQtyAtomic = BigDecimal.ZERO;
        BigDecimal totalFreeQtyAtomic = BigDecimal.ZERO;

        BigDecimal grossTotal = BigDecimal.ZERO;
        BigDecimal lineGrossTotal = BigDecimal.ZERO;
        BigDecimal netTotal = BigDecimal.ZERO;
        BigDecimal lineNetTotal = BigDecimal.ZERO;

        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (BillItem bi : billItems) {
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            BillItemFinanceDetails f = bi.getBillItemFinanceDetails();

            if (bi.getItem() instanceof Ampp) {
                bi.setQty(pbi.getQtyPacks());
                bi.setRate(pbi.getPurchaseRatePack());
            } else if (bi.getItem() instanceof Amp) {
                bi.setQty(pbi.getQty());
                bi.setRate(pbi.getPurchaseRate());
            }

            bi.setSearialNo(serialNo++);
            double netValue = bi.getQty() * bi.getRate();
            bi.setNetValue(-netValue);

            if (f != null) {
                BigDecimal qty = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
                BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
                BigDecimal qtyTotal = qty.add(freeQty);

                BigDecimal costRate = Optional.ofNullable(f.getLineCostRate()).orElse(BigDecimal.ZERO);
                BigDecimal retailRate = Optional.ofNullable(f.getRetailSaleRate()).orElse(BigDecimal.ZERO);
                BigDecimal wholesaleRate = Optional.ofNullable(f.getWholesaleRate()).orElse(BigDecimal.ZERO);

                BigDecimal retailValue = retailRate.multiply(qtyTotal);
                BigDecimal wholesaleValue = wholesaleRate.multiply(qtyTotal);
                BigDecimal freeItemValue = costRate.multiply(freeQty);

                totalLineDiscounts = totalLineDiscounts.add(Optional.ofNullable(f.getLineDiscount()).orElse(BigDecimal.ZERO));
                totalLineExpenses = totalLineExpenses.add(Optional.ofNullable(f.getLineExpense()).orElse(BigDecimal.ZERO));
                totalTaxLines = totalTaxLines.add(Optional.ofNullable(f.getLineTax()).orElse(BigDecimal.ZERO));
                totalLineCosts = totalLineCosts.add(Optional.ofNullable(f.getLineCost()).orElse(BigDecimal.ZERO));

                totalFreeItemValue = totalFreeItemValue.add(freeItemValue);
                totalPurchase = totalPurchase.add(Optional.ofNullable(f.getGrossTotal()).orElse(BigDecimal.ZERO));
                totalRetail = totalRetail.add(retailValue);
                totalWholesale = totalWholesale.add(wholesaleValue);

                totalQty = totalQty.add(qty);
                totalFreeQty = totalFreeQty.add(freeQty);
                totalQtyAtomic = totalQtyAtomic.add(Optional.ofNullable(f.getQuantityByUnits()).orElse(BigDecimal.ZERO));
                totalFreeQtyAtomic = totalFreeQtyAtomic.add(Optional.ofNullable(f.getFreeQuantityByUnits()).orElse(BigDecimal.ZERO));

                grossTotal = grossTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));
                lineGrossTotal = lineGrossTotal.add(Optional.ofNullable(f.getLineGrossTotal()).orElse(BigDecimal.ZERO));
                netTotal = netTotal.add(Optional.ofNullable(f.getNetTotal()).orElse(BigDecimal.ZERO));
                lineNetTotal = lineNetTotal.add(Optional.ofNullable(f.getLineNetTotal()).orElse(BigDecimal.ZERO));

                totalDiscount = totalDiscount.add(Optional.ofNullable(f.getTotalDiscount()).orElse(BigDecimal.ZERO));
                totalExpense = totalExpense.add(Optional.ofNullable(f.getTotalExpense()).orElse(BigDecimal.ZERO));
                totalCost = totalCost.add(Optional.ofNullable(f.getTotalCost()).orElse(BigDecimal.ZERO));
                totalTax = totalTax.add(Optional.ofNullable(f.getTotalTax()).orElse(BigDecimal.ZERO));
            }
        }

        // Set legacy totals on Bill
        bill.setTotal(grossTotal.doubleValue());
        bill.setNetTotal(netTotal.doubleValue());
        bill.setSaleValue(totalRetail.doubleValue());

        // Ensure BillFinanceDetails is present
        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(bill);
            bill.setBillFinanceDetails(bfd);
        }

        // Set calculated values
        bfd.setBillDiscount(billDiscount);
        bfd.setBillExpense(billExpense);
        bfd.setBillTaxValue(billTax);
        bfd.setBillCostValue(billCost);

        bfd.setLineDiscount(totalLineDiscounts);
        bfd.setLineExpense(totalLineExpenses);
        bfd.setItemTaxValue(totalTaxLines);
        bfd.setLineCostValue(totalLineCosts);

        bfd.setTotalDiscount(totalDiscount);
        bfd.setTotalExpense(totalExpense);
        bfd.setTotalTaxValue(totalTax);
        bfd.setTotalCostValue(totalCost);

        bfd.setTotalOfFreeItemValues(totalFreeItemValue);
        bfd.setTotalPurchaseValue(totalPurchase);
        bfd.setTotalRetailSaleValue(totalRetail);
        bfd.setTotalWholesaleValue(totalWholesale);

        bfd.setTotalQuantity(totalQty);
        bfd.setTotalFreeQuantity(totalFreeQty);
        bfd.setTotalQuantityInAtomicUnitOfMeasurement(totalQtyAtomic);
        bfd.setTotalFreeQuantityInAtomicUnitOfMeasurement(totalFreeQtyAtomic);

        bfd.setGrossTotal(grossTotal);
        bfd.setLineGrossTotal(lineGrossTotal);
        bfd.setNetTotal(netTotal);
        bfd.setLineNetTotal(lineNetTotal);
    }
}
