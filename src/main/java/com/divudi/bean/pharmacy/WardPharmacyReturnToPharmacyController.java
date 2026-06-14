package com.divudi.bean.pharmacy;

import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Ward-side return of unused ward-held pharmacy stock back to the originating
 * pharmacy via a porter (#21470, part of #21466).
 *
 * <p>Mirrors {@code PharmacySaleBhtController#transferIssuedStockToPorter}
 * (#21467) but in reverse: ward staff free-select ward-held stock
 * items/batches/quantities, pick a destination pharmacy department and a
 * porter, and on confirm the quantities are deducted from ward department
 * stock and credited to the porter's staff stock (with stock history). The
 * resulting bill is {@link BillTypeAtomic#RETURN_MEDICINE_INWARD}. Pharmacy
 * staff later receive the goods from the porter (#21471,
 * {@link BillTypeAtomic#ACCEPT_RETURN_MEDICINE_INWARD}).</p>
 */
@Named
@SessionScoped
public class WardPharmacyReturnToPharmacyController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillNumberGenerator billNumberBean;

    @Inject
    private SessionController sessionController;
    @Inject
    private DepartmentController departmentController;

    private Bill returnBill;
    private List<BillItem> returnItems;
    private Stock tmpStock;
    private Double qty;
    private Staff porter;
    private Department toDepartment;
    private boolean printPreview;
    private boolean settling;

    public String navigateToReturn() {
        printPreview = false;
        returnItems = new ArrayList<>();
        tmpStock = null;
        qty = null;
        porter = null;
        toDepartment = null;
        returnBill = new BilledBill();
        return "/ward/ward_pharmacy_return_to_pharmacy?faces-redirect=true";
    }

    public void addItem() {
        if (tmpStock == null || tmpStock.getId() == null) {
            JsfUtil.addErrorMessage("Select an item.");
            return;
        }
        if (qty == null || qty <= 0) {
            JsfUtil.addErrorMessage("Enter a quantity greater than zero.");
            return;
        }
        if (qty > tmpStock.getStock()) {
            JsfUtil.addErrorMessage("Quantity exceeds available ward stock (" + tmpStock.getStock() + ").");
            return;
        }

        BillItem bi = new BillItem();
        bi.setItem(tmpStock.getItemBatch().getItem());
        bi.setQty(qty);

        PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
        pbi.setBillItem(bi);
        pbi.setItemBatch(tmpStock.getItemBatch());
        pbi.setStock(tmpStock);
        pbi.setQty(qty);
        bi.setPharmaceuticalBillItem(pbi);

        getReturnItems().add(bi);

        tmpStock = null;
        qty = null;
    }

    public void removeItem(BillItem item) {
        getReturnItems().remove(item);
    }

    public void settle() {
        if (settling) {
            return;
        }
        settling = true;
        try {
            doSettle();
        } finally {
            settling = false;
        }
    }

    private void doSettle() {
        printPreview = false;
        if (getReturnItems().isEmpty()) {
            JsfUtil.addErrorMessage("Add at least one item to return.");
            return;
        }
        if (porter == null) {
            JsfUtil.addErrorMessage("Select the porter who will carry the medicines to the pharmacy.");
            return;
        }
        if (toDepartment == null) {
            JsfUtil.addErrorMessage("Select the destination pharmacy department.");
            return;
        }
        if (!wardStockCoversAllLines()) {
            return;
        }

        Department wardDept = sessionController.getDepartment();

        BilledBill bill = new BilledBill();
        bill.setBillType(BillType.PharmacyIssue);
        bill.setBillTypeAtomic(BillTypeAtomic.RETURN_MEDICINE_INWARD);
        bill.setInstitution(sessionController.getInstitution());
        bill.setDepartment(wardDept);
        bill.setFromDepartment(wardDept);
        bill.setToDepartment(toDepartment);
        bill.setToStaff(porter);
        bill.setCreatedAt(new Date());
        bill.setCreater(sessionController.getLoggedUser());

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(wardDept, BillTypeAtomic.RETURN_MEDICINE_INWARD);
        bill.setDeptId(deptId);
        bill.setInsId(deptId);

        billFacade.create(bill);

        int serial = 1;
        for (BillItem bi : getReturnItems()) {
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            double lineQty = bi.getQty();

            bi.setBill(bill);
            bi.setSearialNo(serial++);
            bi.setCreatedAt(new Date());
            bi.setCreater(sessionController.getLoggedUser());
            billItemFacade.create(bi);

            Stock wardStock = findWardStock(pbi.getItemBatch());
            if (wardStock != null && pharmacyBean.deductFromStock(wardStock, lineQty, pbi, wardDept)) {
                Stock porterStock = pharmacyBean.addToStock(pbi, lineQty, porter);
                pbi.setStaffStock(porterStock);
                pbi.setStock(wardStock);
                billItemFacade.edit(bi);
            } else {
                JsfUtil.addErrorMessage("Insufficient ward stock for " + bi.getItem().getName()
                        + " batch " + pbi.getItemBatch().getBatchNo() + " - this line was skipped.");
                bi.setQty(0.0);
                pbi.setQty(0.0);
                billItemFacade.edit(bi);
            }
        }

        bill.setBillItems(getReturnItems());
        returnBill = bill;
        printPreview = true;
        JsfUtil.addSuccessMessage("Stock returned to pharmacy via porter.");
    }

    private Stock findWardStock(com.divudi.core.entity.pharmacy.ItemBatch itemBatch) {
        String jpql = "SELECT s FROM Stock s WHERE s.itemBatch = :batch AND s.department = :dept";
        Map<String, Object> params = new HashMap<>();
        params.put("batch", itemBatch);
        params.put("dept", sessionController.getDepartment());
        return stockFacade.findFirstByJpql(jpql, params, true);
    }

    /**
     * Validates that ward stock covers EVERY return line before any data is
     * written, mirroring
     * {@code MedicationAdministrationStockSettlementController#wardStockCoversAllLines}.
     */
    private boolean wardStockCoversAllLines() {
        Map<Long, Double> requiredByBatch = new HashMap<>();
        Map<Long, BillItem> sampleByBatch = new HashMap<>();
        for (BillItem bi : getReturnItems()) {
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            if (pbi.getItemBatch() == null || pbi.getItemBatch().getId() == null) {
                JsfUtil.addErrorMessage("Item " + (bi.getItem() != null ? bi.getItem().getName() : "?") + " has no batch - cannot return.");
                return false;
            }
            Long batchId = pbi.getItemBatch().getId();
            requiredByBatch.merge(batchId, bi.getQty(), Double::sum);
            sampleByBatch.put(batchId, bi);
        }

        boolean allCovered = true;
        for (Map.Entry<Long, Double> e : requiredByBatch.entrySet()) {
            BillItem bi = sampleByBatch.get(e.getKey());
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            Stock wardStock = findWardStock(pbi.getItemBatch());
            double available = wardStock == null ? 0.0 : wardStock.getStock();
            if (available + 0.0001 < e.getValue()) {
                JsfUtil.addErrorMessage("Insufficient ward stock for " + bi.getItem().getName()
                        + " batch " + pbi.getItemBatch().getBatchNo()
                        + ": need " + e.getValue() + " but only " + available + " available. Nothing was returned.");
                allCovered = false;
            }
        }
        return allCovered;
    }

    public List<Department> getPharmacies() {
        return departmentController.getPharmacies();
    }

    public Bill getReturnBill() {
        if (returnBill == null) {
            returnBill = new BilledBill();
        }
        return returnBill;
    }

    public void setReturnBill(Bill returnBill) {
        this.returnBill = returnBill;
    }

    public List<BillItem> getReturnItems() {
        if (returnItems == null) {
            returnItems = new ArrayList<>();
        }
        return returnItems;
    }

    public void setReturnItems(List<BillItem> returnItems) {
        this.returnItems = returnItems;
    }

    public Stock getTmpStock() {
        return tmpStock;
    }

    public void setTmpStock(Stock tmpStock) {
        this.tmpStock = tmpStock;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Staff getPorter() {
        return porter;
    }

    public void setPorter(Staff porter) {
        this.porter = porter;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

}
