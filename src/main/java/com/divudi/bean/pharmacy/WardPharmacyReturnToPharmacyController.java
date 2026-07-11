package com.divudi.bean.pharmacy;

import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.CancelledBill;
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
    @Inject
    private WebUserController webUserController;

    private Bill returnBill;
    private List<BillItem> returnItems;
    private Stock tmpStock;
    private Double qty;
    private Staff porter;
    private Department toDepartment;
    private boolean printPreview;
    private boolean settling;
    private String comment;

    public String navigateToReturn() {
        printPreview = false;
        returnItems = new ArrayList<>();
        tmpStock = null;
        qty = null;
        porter = null;
        toDepartment = null;
        comment = null;
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
        double alreadyQueued = 0.0;
        for (BillItem queuedItem : getReturnItems()) {
            PharmaceuticalBillItem queuedPbi = queuedItem.getPharmaceuticalBillItem();
            if (queuedPbi.getItemBatch() != null && queuedPbi.getItemBatch().getId() != null
                    && queuedPbi.getItemBatch().getId().equals(tmpStock.getItemBatch().getId())) {
                alreadyQueued += queuedItem.getQty();
            }
        }
        if (qty + alreadyQueued > tmpStock.getStock()) {
            JsfUtil.addErrorMessage("Total queued quantity (" + (qty + alreadyQueued) + ") exceeds available ward stock (" + tmpStock.getStock() + ").");
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

    /**
     * Whether the pharmacy has started accepting this return - i.e. any
     * non-cancelled {@link BillTypeAtomic#ACCEPT_RETURN_MEDICINE_INWARD} bill
     * item references one of this return's items. Once true, the ward can no
     * longer cancel the return (#21516).
     */
    public boolean isAcceptanceStarted() {
        return hasNonCancelledAcceptance(returnBill);
    }

    private boolean hasNonCancelledAcceptance(Bill returnBill) {
        if (returnBill == null || returnBill.getId() == null) {
            return false;
        }
        String jpql = "SELECT COUNT(bi) FROM BillItem bi "
                + "WHERE bi.referanceBillItem.bill = :returnBill "
                + "AND bi.bill.billTypeAtomic = :acceptBta "
                + "AND (bi.bill.retired = false OR bi.bill.retired IS NULL) "
                + "AND bi.bill.cancelled = false";
        Map<String, Object> params = new HashMap<>();
        params.put("returnBill", returnBill);
        params.put("acceptBta", BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD);
        Long count = billItemFacade.findLongByJpql(jpql, params);
        return count != null && count > 0;
    }

    /**
     * Cancels this ward-to-pharmacy return, provided the pharmacy has not yet
     * started accepting it (#21516). Mirrors
     * {@code PharmacyBillSearch.cancelInwardPharmacyRequestBill} - flags the
     * bill as cancelled and records a {@link CancelledBill}, after reversing
     * the ward/porter stock movements made on settle (deducts from the
     * porter's staff stock and credits back to ward department stock).
     */
    public void cancelReturnBill() {
        if (!webUserController.hasPrivilege(Privileges.InwardPharmacyReturnCancel.name())) {
            JsfUtil.addErrorMessage("You do not have the privilege to cancel this return.");
            return;
        }
        if (returnBill == null || returnBill.getId() == null) {
            JsfUtil.addErrorMessage("No return bill found.");
            return;
        }
        if (returnBill.isCancelled()) {
            JsfUtil.addErrorMessage("This return has already been cancelled.");
            return;
        }
        if (comment == null || comment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Provide a comment to cancel the return.");
            return;
        }
        if (hasNonCancelledAcceptance(returnBill)) {
            JsfUtil.addErrorMessage("This return has already been accepted by the pharmacy and can no longer be cancelled.");
            return;
        }
        if (!porterStockCoversAllLines(returnBill)) {
            return;
        }

        reverseReturnStockMovements(returnBill);

        CancelledBill cb = new CancelledBill();
        cb.setBilledBill(returnBill);
        cb.copy(returnBill);
        cb.setReferenceBill(returnBill.getReferenceBill());
        cb.invertAndAssignValuesFromOtherBill(returnBill);
        cb.setBillItems(returnBill.getBillItems());
        cb.setBillTypeAtomic(BillTypeAtomic.RETURN_MEDICINE_INWARD_CANCELLATION);
        cb.setComments(comment);
        cb.setCreatedAt(new Date());
        cb.setCreater(sessionController.getLoggedUser());
        cb.setDepartment(sessionController.getDepartment());
        cb.setInstitution(sessionController.getInstitution());
        cb.setBalance(0.0);
        cb.setCompleted(true);

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.RETURN_MEDICINE_INWARD_CANCELLATION);
        cb.setDeptId(deptId);
        cb.setInsId(deptId);
        billFacade.create(cb);

        returnBill.setCancelled(true);
        returnBill.setCancelledBill(cb);
        billFacade.edit(returnBill);

        comment = null;
        JsfUtil.addSuccessMessage("Return to pharmacy cancelled.");
    }

    public void settle() {
        if (!webUserController.hasPrivilege(Privileges.InwardPharmacyReturnSubmit.name())) {
            JsfUtil.addErrorMessage("You do not have the privilege to submit this return.");
            return;
        }
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

    /**
     * Reverses the ward/porter stock movements made by {@link #doSettle()}
     * when a settled return is cancelled before pharmacy acceptance
     * (#21516): deducts the returned quantities from the porter's staff
     * stock and credits them back to ward department stock.
     */
    private void reverseReturnStockMovements(Bill bill) {
        Staff toStaff = bill.getToStaff();
        Department wardDept = bill.getFromDepartment();
        for (BillItem bi : bill.getBillItems()) {
            double lineQty = bi.getQty();
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            if (lineQty <= 0 || pbi == null || pbi.getItemBatch() == null) {
                continue;
            }
            pharmacyBean.deductFromStock(pbi, lineQty, toStaff);
            pharmacyBean.addToStock(pbi, lineQty, wardDept);
        }
    }

    /**
     * Validates that the porter still holds ENOUGH staff stock for EVERY
     * settled return line before cancellation reverses any stock, mirroring
     * {@link #wardStockCoversAllLines()}.
     */
    private boolean porterStockCoversAllLines(Bill bill) {
        Staff toStaff = bill.getToStaff();
        Map<Long, Double> requiredByBatch = new HashMap<>();
        Map<Long, BillItem> sampleByBatch = new HashMap<>();
        for (BillItem bi : bill.getBillItems()) {
            double lineQty = bi.getQty();
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            if (lineQty <= 0 || pbi == null || pbi.getItemBatch() == null || pbi.getItemBatch().getId() == null) {
                continue;
            }
            Long batchId = pbi.getItemBatch().getId();
            requiredByBatch.merge(batchId, lineQty, Double::sum);
            sampleByBatch.put(batchId, bi);
        }

        boolean allCovered = true;
        for (Map.Entry<Long, Double> e : requiredByBatch.entrySet()) {
            BillItem bi = sampleByBatch.get(e.getKey());
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            Stock porterStock = findStaffStock(pbi.getItemBatch(), toStaff);
            double available = porterStock == null ? 0.0 : porterStock.getStock();
            if (available + 0.0001 < e.getValue()) {
                JsfUtil.addErrorMessage("Cannot cancel: porter no longer holds enough stock of " + bi.getItem().getName()
                        + " batch " + pbi.getItemBatch().getBatchNo()
                        + " to reverse this return (need " + e.getValue() + ", available " + available + ").");
                allCovered = false;
            }
        }
        return allCovered;
    }

    private Stock findStaffStock(com.divudi.core.entity.pharmacy.ItemBatch itemBatch, Staff staff) {
        String jpql = "SELECT s FROM Stock s WHERE s.itemBatch = :batch AND s.staff = :staff";
        Map<String, Object> params = new HashMap<>();
        params.put("batch", itemBatch);
        params.put("staff", staff);
        return stockFacade.findFirstByJpql(jpql, params, true);
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
