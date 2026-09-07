package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.InstitutionType;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.InstitutionFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.util.BigDecimalUtil;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller to handle pharmacy donations. Structure based on
 * PharmacyDirectPurchaseController but simplified for donation flow.
 */
@Named
@SessionScoped
public class PharmacyDonationController implements Serializable {

    private BilledBill bill;
    private List<BillItem> billItems;
    private BillItem currentBillItem;
    private List<BillItem> billExpenses;
    private boolean printPreview;

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @Inject
    PharmacyCalculation pharmacyBillBean;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private InstitutionFacade institutionFacade;

    @Inject
    private SessionController sessionController;

    public String navigateToStartNewDonationBill() {
        prepareForNewDonationBill();
        return "/pharmacy/donation?faces-redirect=true";
    }

    public void prepareForNewDonationBill() {
        bill = null;
        billItems = null;
        billExpenses = null;
        currentBillItem = null;
        printPreview = false;
    }

    public void addItem() {
        if (currentBillItem == null) {
            return;
        }
        Item item = currentBillItem.getItem();
        BillItemFinanceDetails f = currentBillItem.getBillItemFinanceDetails();
        PharmaceuticalBillItem pbi = currentBillItem.getPharmaceuticalBillItem();

        if (item == null || f == null || pbi == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }

        if (BigDecimalUtil.isNullOrZero(f.getQuantity()) || BigDecimalUtil.isNegative(f.getQuantity())) {
            JsfUtil.addErrorMessage("Please enter quantity");
            return;
        }

        if (f.getLineCostRate() == null || BigDecimalUtil.isNegative(f.getLineCostRate())) {
            JsfUtil.addErrorMessage("Please enter cost rate");
            return;
        }

        BigDecimal qty = BigDecimalUtil.valueOrZero(f.getQuantity());
        BigDecimal unitsPerPack = BigDecimalUtil.valueOrZero(f.getUnitsPerPack());
        if (unitsPerPack.compareTo(BigDecimal.ZERO) <= 0) {
            unitsPerPack = BigDecimal.ONE;
            f.setUnitsPerPack(unitsPerPack);
        }

        BigDecimal totalQtyUnits;
        if (item instanceof Ampp) {
            totalQtyUnits = BigDecimalUtil.multiply(qty, unitsPerPack);
            f.setQuantityByUnits(totalQtyUnits);
        } else {
            totalQtyUnits = qty;
            f.setQuantityByUnits(qty);
        }

        BigDecimal packCostRate = f.getLineCostRate();
        BigDecimal unitCostRate;
        if (item instanceof Ampp) {
            unitCostRate = packCostRate.divide(unitsPerPack, 4, BigDecimal.ROUND_HALF_UP);
        } else {
            unitCostRate = packCostRate;
        }
        f.setLineCostRate(unitCostRate);
        f.setBillCostRate(unitCostRate);
        f.setTotalCostRate(unitCostRate);
        BigDecimal lineCost = unitCostRate.multiply(totalQtyUnits);
        f.setLineCost(lineCost);
        f.setValueAtCostRate(lineCost);

        BigDecimal lineExpense = BigDecimalUtil.valueOrZero(f.getLineExpense());
        BigDecimal lineTax = BigDecimalUtil.valueOrZero(f.getLineTax());
        BigDecimal net = lineCost.add(lineExpense).add(lineTax);
        f.setLineNetTotal(net);
        f.setLineNetRate(net.divide(totalQtyUnits, 4, BigDecimal.ROUND_HALF_UP));

        // Purchase & retail rates are kept only for batch creation
        BigDecimal linePurchaseRate = BigDecimalUtil.valueOrZero(f.getLineGrossRate());
        BigDecimal retailRate = BigDecimalUtil.valueOrZero(f.getRetailSaleRate());
        if (item instanceof Ampp) {
            BigDecimal unitPurchaseRate = linePurchaseRate.divide(unitsPerPack, 4, BigDecimal.ROUND_HALF_UP);
            BigDecimal unitRetailRate = retailRate.divide(unitsPerPack, 4, BigDecimal.ROUND_HALF_UP);
            f.setRetailSaleRatePerUnit(unitRetailRate);
            f.setGrossRate(unitPurchaseRate);
        } else {
            f.setRetailSaleRatePerUnit(retailRate);
            f.setGrossRate(linePurchaseRate);
        }

        pbi.setQty(totalQtyUnits.doubleValue());
        pbi.setPurchaseRate(f.getGrossRate().doubleValue());
        pbi.setRetailRate(f.getRetailSaleRatePerUnit().doubleValue());

        currentBillItem.setRate(unitCostRate.doubleValue());
        currentBillItem.setNetRate(f.getLineNetRate().doubleValue());
        currentBillItem.setNetValue(net.doubleValue());
        currentBillItem.setGrossValue(net.doubleValue());
        currentBillItem.setSearialNo(getBillItems().size());

        getBillItems().add(currentBillItem);
        currentBillItem = null;
        calculateBillTotalsFromItems();
    }

    private void calculateBillTotalsFromItems() {
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        if (billItems != null) {
            for (BillItem bi : billItems) {
                BillItemFinanceDetails f = bi.getBillItemFinanceDetails();
                if (f != null) {
                    totalCost = totalCost.add(BigDecimalUtil.valueOrZero(f.getLineCost()));
                    totalExpense = totalExpense.add(BigDecimalUtil.valueOrZero(f.getLineExpense()));
                    totalTax = totalTax.add(BigDecimalUtil.valueOrZero(f.getLineTax()));
                }
            }
        }

        if (billExpenses != null) {
            for (BillItem e : billExpenses) {
                totalExpense = totalExpense.add(BigDecimal.valueOf(e.getNetValue()));
            }
        }

        BigDecimal net = totalCost.add(totalExpense).add(totalTax);
        getBill().setTotal(net.doubleValue());
        getBill().setNetTotal(net.doubleValue());
        
        // Populate BillFinanceDetails for the receipt template
        if (getBill().getBillFinanceDetails() == null) {
            getBill().setBillFinanceDetails(new BillFinanceDetails());
        }
        getBill().getBillFinanceDetails().setTotalCostValue(totalCost);
        getBill().getBillFinanceDetails().setTotalExpense(totalExpense);
        getBill().getBillFinanceDetails().setTotalTaxValue(totalTax);
        getBill().getBillFinanceDetails().setNetTotal(net);
    }

    public void settleDonationBillFinally() {
        if (getBill().getFromInstitution() == null) {
            JsfUtil.addErrorMessage("Select Donator");
            return;
        }
        if (getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("No items to settle");
            return;
        }

        saveBill();

        for (BillItem i : getBillItems()) {
            PharmaceuticalBillItem pbi = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);
            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            i.setBill(getBill());
            getBillItemFacade().create(i);

            if (pbi.getId() == null) {
                getPharmaceuticalBillItemFacade().create(pbi);
            } else {
                getPharmaceuticalBillItemFacade().edit(pbi);
            }
            i.setPharmaceuticalBillItem(pbi);
            getBillItemFacade().edit(i);

            ItemBatch itemBatch = getPharmacyBillBean().saveItemBatchWithCosting(i);
            double addingQty = BigDecimalUtil.valueOrZero(i.getBillItemFinanceDetails().getQuantityByUnits()).doubleValue();
            pbi.setItemBatch(itemBatch);
            Stock stock = getPharmacyBean().addToStockForCosting(i, Math.abs(addingQty), getSessionController().getDepartment());
            pbi.setStock(stock);
            getPharmaceuticalBillItemFacade().edit(pbi);

            getBill().getBillItems().add(i);
        }

        getBillFacade().edit(getBill());
        JsfUtil.addSuccessMessage("Donation Successfully Completed.");
        printPreview = true;
    }

    public List<Institution> completeDonator(String query) {
        String jpql = "select i from Institution i where i.retired=false and i.institutionType=:type and lower(i.name) like :q";
        Map<String, Object> m = new HashMap<>();
        m.put("type", InstitutionType.NonProfit);
        m.put("q", "%" + query.toLowerCase() + "%");
        return institutionFacade.findByJpql(jpql, m);
    }

    public BilledBill getBill() {
        if (bill == null) {
            bill = new BilledBill();
            bill.setBillType(BillType.PharmacyDonationBill);
            bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_DONATION_BILL);
            bill.setInstitution(getSessionController().getInstitution());
            bill.setDepartment(getSessionController().getDepartment());
            bill.setCreatedAt(new Date());
            bill.setCreater(getSessionController().getLoggedUser());
        }
        return bill;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public BillItem getCurrentBillItem() {
        if (currentBillItem == null) {
            currentBillItem = new BillItem();
            currentBillItem.setBill(getBill());
            currentBillItem.setPharmaceuticalBillItem(new PharmaceuticalBillItem());
            currentBillItem.setBillItemFinanceDetails(new BillItemFinanceDetails());
        }
        return currentBillItem;
    }

    // Getters and setters
    public void setCurrentBillItem(BillItem currentBillItem) {
        this.currentBillItem = currentBillItem;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public void saveBill() {
        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_DONATION_BILL);
        getBill().setDeptId(deptId);
        getBill().setInsId(deptId);
        if (getBill().getId() == null) {
            billFacade.create(getBill());
        } else {
            billFacade.edit(getBill());
        }
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public PharmacyCalculation getPharmacyBillBean() {
        return pharmacyBillBean;
    }

    public SessionController getSessionController() {
        return sessionController;
    }
}

