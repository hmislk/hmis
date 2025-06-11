package com.divudi.service.pharmacy;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.Item;
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
     * Recalculate line-level financial values before adding a BillItem to a bill.
     */
    public void recalculateFinancialsBeforeAddingBillItem(BillItemFinanceDetails f) {
        if (f == null || f.getBillItem() == null) {
            return;
        }

        BigDecimal qty = Optional.ofNullable(f.getQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal freeQty = Optional.ofNullable(f.getFreeQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal lineGrossRate = Optional.ofNullable(f.getLineGrossRate()).orElse(BigDecimal.ZERO);
        BigDecimal lineDiscountRate = Optional.ofNullable(f.getLineDiscountRate()).orElse(BigDecimal.ZERO);
        BigDecimal retailRate = Optional.ofNullable(f.getRetailSaleRatePerUnit()).orElse(BigDecimal.ZERO);

        Item item = f.getBillItem().getItem();
        BigDecimal totalQty = qty.add(freeQty);

        BigDecimal unitsPerPack;
        BigDecimal qtyInUnits;
        BigDecimal freeQtyInUnits;
        BigDecimal totalQtyInUnits;
        if (item instanceof Ampp) {
            double dblVal = item.getDblValue();
            unitsPerPack = dblVal > 0.0 ? BigDecimal.valueOf(dblVal) : BigDecimal.ONE;
            qtyInUnits = qty.multiply(unitsPerPack);
            freeQtyInUnits = freeQty.multiply(unitsPerPack);
            totalQtyInUnits = totalQty.multiply(unitsPerPack);
        } else {
            unitsPerPack = BigDecimal.ONE;
            qtyInUnits = qty;
            freeQtyInUnits = freeQty;
            totalQtyInUnits = totalQty;
        }

        f.setUnitsPerPack(unitsPerPack);
        f.setQuantityByUnits(qtyInUnits);
        f.setFreeQuantityByUnits(freeQtyInUnits);
        f.setTotalQuantityByUnits(totalQtyInUnits);

        BigDecimal lineGrossTotal = lineGrossRate.multiply(qty);
        BigDecimal lineDiscountValue = lineDiscountRate.multiply(qty);
        BigDecimal lineNetTotal = lineGrossTotal.subtract(lineDiscountValue);
        BigDecimal lineCostRate = totalQtyInUnits.compareTo(BigDecimal.ZERO) > 0
                ? lineNetTotal.divide(totalQtyInUnits, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal retailValue = retailRate.multiply(totalQtyInUnits);

        f.setLineGrossRate(lineGrossRate);
        f.setLineNetRate(totalQty.compareTo(BigDecimal.ZERO) > 0
                ? lineNetTotal.divide(totalQty, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        f.setRetailSaleRate(retailRate.multiply(unitsPerPack));
        f.setLineDiscount(lineDiscountValue);
        f.setLineGrossTotal(lineGrossTotal);
        f.setLineNetTotal(lineNetTotal);
        f.setLineCost(lineNetTotal);
        f.setLineCostRate(lineCostRate);
        f.setTotalQuantity(totalQty);
    }

    /**
     * Distribute bill-level values (discounts, expenses, taxes) proportionally to items.
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

            BigDecimal lineCostRate = totalQty.compareTo(BigDecimal.ZERO) > 0
                    ? lineNetTotal.divide(totalQty, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal billCostRate = billCost.compareTo(BigDecimal.ZERO) > 0 && totalQty.compareTo(BigDecimal.ZERO) > 0
                    ? billCost.divide(totalQty, 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal totalCostRate = totalQty.compareTo(BigDecimal.ZERO) > 0
                    ? netTotal.divide(totalQty, 6, RoundingMode.HALF_UP)
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
                BigDecimal lineNetRate = totalQty.compareTo(BigDecimal.ZERO) > 0
                        ? lineNetTotal.divide(totalQty, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                f.setLineNetRate(lineNetRate);
            }

            f.setBillNetRate(BigDecimal.ZERO);
            f.setNetRate(f.getLineNetRate());
        }
    }
}
