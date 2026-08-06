package com.divudi.bean.pharmacy;

import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.MedicationAdministrationStatus;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.MedicationAdministrationRecordFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import com.divudi.service.BillService;
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
import org.primefaces.event.RowEditEvent;

/**
 * Ward-side return of unused ward-held pharmacy stock back to the originating
 * pharmacy via a porter (#21470, part of #21466).
 *
 * <p>Scoped to specific pharmacy receive bills (#22224): for the active
 * patient encounter (BHT), ward staff pick one of the encounter's
 * {@link BillTypeAtomic#ACCEPT_ISSUED_MEDICINE_INWARD} receive bills that
 * still has a returnable balance, then enter per-line return quantities
 * clamped to {@code [0, returnable]} where
 * {@code returnable = receivedQty - alreadyReturnedQty - administeredQty},
 * floored at 0 and capped by live ward department stock. On confirm the
 * quantities are deducted from ward department stock and credited to the
 * porter's staff stock (with stock history), and each return line records
 * {@code BillItem.referanceBillItem} pointing to the receive bill item for
 * traceability. The resulting bill is
 * {@link BillTypeAtomic#RETURN_MEDICINE_INWARD}. Pharmacy staff later receive
 * the goods from the porter (#21471,
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
    private MedicationAdministrationRecordFacade medicationAdministrationRecordFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillService billService;

    @Inject
    private SessionController sessionController;
    @Inject
    private DepartmentController departmentController;
    @Inject
    private WebUserController webUserController;

    private PatientEncounter patientEncounter;
    private List<Bill> receiveBills;
    private Bill selectedReceiveBill;
    private List<ReturnLine> returnLines;
    private Bill previewBill;
    private List<ReturnLine> previewLines;

    private Bill returnBill;
    private Staff porter;
    private Department toDepartment;
    private boolean printPreview;
    private boolean settling;
    private String comment;

    /**
     * One editable line of the scoped return, wrapping the transient return
     * {@link BillItem} (whose {@code referanceBillItem} points to the receive
     * bill item) together with the quantities that determine how much can
     * still be returned. Quantities are computed once when the line is built
     * to avoid re-running the netting queries on every EL evaluation.
     */
    public static class ReturnLine implements Serializable {

        private static final long serialVersionUID = 1L;

        private BillItem returnItem;
        private double receivedQty;
        private double alreadyReturnedQty;
        private double administeredQty;
        private double returnableQty;

        public BillItem getReturnItem() {
            return returnItem;
        }

        public void setReturnItem(BillItem returnItem) {
            this.returnItem = returnItem;
        }

        public double getReceivedQty() {
            return receivedQty;
        }

        public void setReceivedQty(double receivedQty) {
            this.receivedQty = receivedQty;
        }

        public double getAlreadyReturnedQty() {
            return alreadyReturnedQty;
        }

        public void setAlreadyReturnedQty(double alreadyReturnedQty) {
            this.alreadyReturnedQty = alreadyReturnedQty;
        }

        public double getAdministeredQty() {
            return administeredQty;
        }

        public void setAdministeredQty(double administeredQty) {
            this.administeredQty = administeredQty;
        }

        public double getReturnableQty() {
            return returnableQty;
        }

        public void setReturnableQty(double returnableQty) {
            this.returnableQty = returnableQty;
        }
    }

    public String navigateToReturn(PatientEncounter encounter) {
        if (encounter == null || encounter.getId() == null) {
            JsfUtil.addErrorMessage("Select an admission first.");
            return null;
        }
        patientEncounter = encounter;
        printPreview = false;
        selectedReceiveBill = null;
        returnLines = new ArrayList<>();
        previewBill = null;
        previewLines = new ArrayList<>();
        porter = null;
        toDepartment = null;
        comment = null;
        returnBill = new BilledBill();
        loadReceiveBills();
        return "/ward/ward_pharmacy_return_to_pharmacy?faces-redirect=true";
    }

    /**
     * Restarts the return flow for the same admission after a settle, from
     * the print-preview panel's "New Return" button.
     */
    public String navigateToNewReturn() {
        return navigateToReturn(patientEncounter);
    }

    /**
     * Loads the encounter's non-cancelled
     * {@link BillTypeAtomic#ACCEPT_ISSUED_MEDICINE_INWARD} receive bills that
     * still have a returnable balance on at least one line. Bills with
     * nothing left to return (fully administered and/or fully returned) are
     * dropped (#22224).
     */
    public void loadReceiveBills() {
        receiveBills = new ArrayList<>();
        if (patientEncounter == null || patientEncounter.getId() == null) {
            return;
        }
        String jpql = "SELECT b FROM Bill b WHERE b.billTypeAtomic = :bta "
                + "AND b.patientEncounter = :enc "
                + "AND b.cancelled = false "
                + "AND (b.retired = false OR b.retired IS NULL) "
                + "ORDER BY b.createdAt DESC";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.ACCEPT_ISSUED_MEDICINE_INWARD);
        params.put("enc", patientEncounter);
        List<Bill> candidates = billFacade.findByJpql(jpql, params);
        for (Bill b : candidates) {
            if (hasReturnableBalance(b)) {
                receiveBills.add(b);
            }
        }
    }

    private boolean hasReturnableBalance(Bill receiveBill) {
        for (BillItem receiveItem : billService.fetchBillItems(receiveBill)) {
            if (computeReturnableQty(receiveItem) > 0.001) {
                return true;
            }
        }
        return false;
    }

    /**
     * Quantity of a receive bill line already returned to the pharmacy -
     * a 1-hop netting over non-cancelled
     * {@link BillTypeAtomic#RETURN_MEDICINE_INWARD} bill items whose
     * {@code referanceBillItem} points to the receive bill item. Mirrors
     * {@code PharmacyReturnFromWardReceiveController#getRemainingQuantityForReturnItem}
     * (#21510).
     */
    public double getAlreadyReturnedQty(BillItem receiveItem) {
        if (receiveItem == null || receiveItem.getId() == null) {
            return 0.0;
        }
        String jpql = "SELECT SUM(ABS(bi.qty)) FROM BillItem bi "
                + "WHERE bi.referanceBillItem.id = :refId "
                + "AND bi.bill.billTypeAtomic = :returnBta "
                + "AND (bi.bill.retired = false OR bi.bill.retired IS NULL) "
                + "AND bi.bill.cancelled = false";
        Map<String, Object> params = new HashMap<>();
        params.put("refId", receiveItem.getId());
        params.put("returnBta", BillTypeAtomic.RETURN_MEDICINE_INWARD);
        return billItemFacade.findDoubleByJpql(jpql, params);
    }

    /**
     * Quantity already administered to the patient for the same
     * item/batch/encounter as the receive bill line, counted as soon as the
     * stage-1 {@code MedicationAdministrationRecord} exists (status GIVEN),
     * regardless of whether the stage-2 stock settlement has run (#22224).
     */
    public double getAdministeredQty(BillItem receiveItem) {
        if (receiveItem == null || receiveItem.getId() == null) {
            return 0.0;
        }
        PharmaceuticalBillItem receivePbi = receiveItem.getPharmaceuticalBillItem();
        if (receivePbi == null || receivePbi.getItemBatch() == null) {
            return 0.0;
        }
        PatientEncounter enc = receiveItem.getBill() != null
                ? receiveItem.getBill().getPatientEncounter()
                : patientEncounter;
        if (enc == null) {
            return 0.0;
        }
        String jpql = "SELECT SUM(m.qty) FROM MedicationAdministrationRecord m "
                + "WHERE m.patientEncounter = :enc "
                + "AND m.item = :item "
                + "AND m.itemBatch = :batch "
                + "AND m.status = :given "
                + "AND m.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("enc", enc);
        params.put("item", receiveItem.getItem());
        params.put("batch", receivePbi.getItemBatch());
        params.put("given", MedicationAdministrationStatus.GIVEN);
        return medicationAdministrationRecordFacade.findDoubleByJpql(jpql, params);
    }

    /**
     * How much of a receive bill line can still be returned:
     * {@code receivedQty - alreadyReturnedQty - administeredQty}, floored at
     * 0 and capped by live ward department stock as a backstop (#22224).
     */
    public double computeReturnableQty(BillItem receiveItem) {
        if (receiveItem == null) {
            return 0.0;
        }
        PharmaceuticalBillItem receivePbi = receiveItem.getPharmaceuticalBillItem();
        if (receivePbi == null || receivePbi.getItemBatch() == null) {
            return 0.0;
        }
        double received = Math.abs(receivePbi.getQty());
        double netReturnable = Math.max(0.0, received - getAlreadyReturnedQty(receiveItem) - getAdministeredQty(receiveItem));
        Stock wardStock = findWardStock(receivePbi.getItemBatch());
        double available = wardStock == null ? 0.0 : wardStock.getStock();
        return Math.min(netReturnable, available);
    }

    private List<ReturnLine> buildReturnLines(Bill receiveBill, boolean includeNonReturnable) {
        List<ReturnLine> lines = new ArrayList<>();
        if (receiveBill == null || receiveBill.getId() == null) {
            return lines;
        }
        for (BillItem receiveItem : billService.fetchBillItems(receiveBill)) {
            PharmaceuticalBillItem receivePbi = receiveItem.getPharmaceuticalBillItem();
            if (receivePbi == null || receivePbi.getItemBatch() == null) {
                continue;
            }
            double received = Math.abs(receivePbi.getQty());
            double alreadyReturned = getAlreadyReturnedQty(receiveItem);
            double administered = getAdministeredQty(receiveItem);
            double netReturnable = Math.max(0.0, received - alreadyReturned - administered);
            Stock wardStock = findWardStock(receivePbi.getItemBatch());
            double available = wardStock == null ? 0.0 : wardStock.getStock();
            double returnable = Math.min(netReturnable, available);
            if (!includeNonReturnable && returnable <= 0.001) {
                continue;
            }

            BillItem bi = new BillItem();
            bi.setItem(receiveItem.getItem());
            bi.setQty(0.0);
            bi.setPatientEncounter(receiveItem.getPatientEncounter());
            bi.setReferanceBillItem(receiveItem);

            PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
            pbi.copy(receivePbi);
            pbi.setBillItem(bi);
            pbi.setQty(0);
            // The copy above inherits the receive bill's stock binding. A
            // return must only ever touch ward department stock (deducted,
            // bound on settle) and the porter's staff stock (added).
            pbi.setStock(null);
            pbi.setStaffStock(null);
            bi.setPharmaceuticalBillItem(pbi);

            ReturnLine line = new ReturnLine();
            line.setReturnItem(bi);
            line.setReceivedQty(received);
            line.setAlreadyReturnedQty(alreadyReturned);
            line.setAdministeredQty(administered);
            line.setReturnableQty(returnable);
            lines.add(line);
        }
        return lines;
    }

    public void selectReceiveBill(Bill receiveBill) {
        selectedReceiveBill = receiveBill;
        returnLines = buildReturnLines(receiveBill, false);
        if (receiveBill != null && receiveBill.getFromDepartment() != null) {
            toDepartment = receiveBill.getFromDepartment();
        }
        if (returnLines.isEmpty()) {
            JsfUtil.addErrorMessage("Nothing left to return on this received bill.");
        }
    }

    public void previewReceiveBill(Bill receiveBill) {
        previewBill = receiveBill;
        previewLines = buildReturnLines(receiveBill, true);
    }

    /**
     * Row-edit listener for the editable "Return Quantity" column, clamping
     * the entered quantity to {@code [0, returnable]}. Mirrors
     * {@code PharmacyReturnFromWardReceiveController#onEditing}.
     */
    public void onEditing(RowEditEvent event) {
        Object rowObject = event.getObject();
        if (!(rowObject instanceof ReturnLine)) {
            return;
        }
        ReturnLine line = (ReturnLine) rowObject;
        BillItem bi = line.getReturnItem();
        PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();

        if (pbi.getQty() < 0) {
            pbi.setQty(0);
            bi.setQty(0.0);
            JsfUtil.addErrorMessage("Can not enter a minus value");
            return;
        }

        if (pbi.getQty() > line.getReturnableQty()) {
            JsfUtil.addErrorMessage("Cannot return " + pbi.getQty() + " units of " + bi.getItem().getName()
                    + ". Only " + line.getReturnableQty() + " units are returnable.");
            pbi.setQty(line.getReturnableQty());
            bi.setQty(line.getReturnableQty());
            return;
        }

        bi.setQty(pbi.getQty());
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
        if (selectedReceiveBill == null || selectedReceiveBill.getId() == null) {
            JsfUtil.addErrorMessage("Select a received bill to return against.");
            return;
        }
        if (getReturnLines().isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to return on the selected received bill.");
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
        if (selectedReceiveBill.getPatientEncounter() != null
                && selectedReceiveBill.getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Sorry, patient is discharged.");
            return;
        }

        List<BillItem> itemsToReturn = new ArrayList<>();
        for (ReturnLine line : getReturnLines()) {
            BillItem bi = line.getReturnItem();
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            double requestedQty = pbi.getQty();
            if (requestedQty < 0) {
                JsfUtil.addErrorMessage("Can not enter a minus value");
                pbi.setQty(0);
                bi.setQty(0.0);
                continue;
            }
            if (requestedQty <= 0) {
                continue;
            }
            // Recompute against the latest DB state - the line's returnable
            // quantity is a snapshot from when the bill was selected, and
            // another return/administration may have happened since.
            double freshReturnable = computeReturnableQty(bi.getReferanceBillItem());
            if (freshReturnable <= 0.0001) {
                JsfUtil.addErrorMessage("Item " + (bi.getItem() != null ? bi.getItem().getName() : "?")
                        + " no longer has a returnable balance - nothing returned for this line.");
                pbi.setQty(0);
                bi.setQty(0.0);
                continue;
            }
            double qtyToReturn = Math.min(requestedQty, freshReturnable);
            if (qtyToReturn + 0.0001 < requestedQty) {
                JsfUtil.addErrorMessage("Only " + qtyToReturn + " of " + requestedQty + " units of " + bi.getItem().getName()
                        + " can be returned now - returning the returnable quantity.");
            }
            pbi.setQty(qtyToReturn);
            bi.setQty(qtyToReturn);
            itemsToReturn.add(bi);
        }

        if (itemsToReturn.isEmpty()) {
            JsfUtil.addErrorMessage("Enter a return quantity for at least one item.");
            return;
        }
        if (!wardStockCoversAllLines(itemsToReturn)) {
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
        bill.setPatient(selectedReceiveBill.getPatient());
        bill.setPatientEncounter(selectedReceiveBill.getPatientEncounter());
        bill.setReferenceBill(selectedReceiveBill);
        bill.setCreatedAt(new Date());
        bill.setCreater(sessionController.getLoggedUser());

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(wardDept, BillTypeAtomic.RETURN_MEDICINE_INWARD);
        bill.setDeptId(deptId);
        bill.setInsId(deptId);

        billFacade.create(bill);

        int serial = 1;
        for (BillItem bi : itemsToReturn) {
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

        bill.setBillItems(itemsToReturn);
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
    private boolean wardStockCoversAllLines(List<BillItem> itemsToReturn) {
        Map<Long, Double> requiredByBatch = new HashMap<>();
        Map<Long, BillItem> sampleByBatch = new HashMap<>();
        for (BillItem bi : itemsToReturn) {
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
     * {@link #wardStockCoversAllLines(List)}.
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

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public List<Bill> getReceiveBills() {
        if (receiveBills == null) {
            receiveBills = new ArrayList<>();
        }
        return receiveBills;
    }

    public void setReceiveBills(List<Bill> receiveBills) {
        this.receiveBills = receiveBills;
    }

    public Bill getSelectedReceiveBill() {
        return selectedReceiveBill;
    }

    public void setSelectedReceiveBill(Bill selectedReceiveBill) {
        this.selectedReceiveBill = selectedReceiveBill;
    }

    public List<ReturnLine> getReturnLines() {
        if (returnLines == null) {
            returnLines = new ArrayList<>();
        }
        return returnLines;
    }

    public void setReturnLines(List<ReturnLine> returnLines) {
        this.returnLines = returnLines;
    }

    public Bill getPreviewBill() {
        return previewBill;
    }

    public void setPreviewBill(Bill previewBill) {
        this.previewBill = previewBill;
    }

    public List<ReturnLine> getPreviewLines() {
        if (previewLines == null) {
            previewLines = new ArrayList<>();
        }
        return previewLines;
    }

    public void setPreviewLines(List<ReturnLine> previewLines) {
        this.previewLines = previewLines;
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
