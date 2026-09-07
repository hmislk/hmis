package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.dto.PharmacyReturnFromWardPendingReturnDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
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
import javax.persistence.TemporalType;
import org.primefaces.event.RowEditEvent;

/**
 * Pharmacy-side confirmation of medicines returned from a ward via a porter
 * for a {@link BillTypeAtomic#RETURN_MEDICINE_INWARD} return bill (#21471,
 * part of #21466). Mirrors the stock-movement pattern of
 * {@link WardPharmacyBhtIssueReceiveController} (#21467) in reverse: deducts
 * the porter's in-transit (staff) stock and credits the receiving pharmacy
 * department's stock, recording the confirmation as a
 * {@link BillTypeAtomic#ACCEPT_RETURN_MEDICINE_INWARD} bill linked back to
 * the return via {@link Bill#getBackwardReferenceBill()}.
 *
 * <p>Remaining-quantity tracking, auto-complete and force-complete (#21510)
 * mirror the equivalent BHT issue-request pattern in
 * {@link PharmacySaleBhtController} (#21505/#21507), reusing the generic
 * {@code Bill.fullyIssued}/{@code completed} fields and
 * {@code BillItem.remainingQty}.</p>
 */
@Named
@SessionScoped
public class PharmacyReturnFromWardReceiveController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private BillService billService;

    private Bill returnedBill;
    private Long returnedBillId;
    private Bill receivedBill;
    private List<PharmacyReturnFromWardPendingReturnDTO> pendingReturnBills;
    private boolean printPreview;
    private boolean settling;
    private boolean completed;

    public void makeNull() {
        returnedBill = null;
        returnedBillId = null;
        receivedBill = null;
        printPreview = false;
        completed = false;
    }

    public String navigateToPendingList() {
        makeNull();
        loadPendingReturnBills();
        return "/pharmacy/pharmacy_return_from_ward_receive_list?faces-redirect=true";
    }

    public void loadPendingReturnBills() {
        // DTO projection avoids loading full Bill entities (and their lazy
        // associations) for what is a read-only listing (#21471).
        String jpql = "SELECT new com.divudi.core.data.dto.PharmacyReturnFromWardPendingReturnDTO("
                + "b.id, b.deptId, COALESCE(fromDept.name, ''), COALESCE(createrPerson.name, ''), "
                + "toStaff.id, toStaffPerson.title, COALESCE(toStaffPerson.name, ''), b.createdAt) "
                + "FROM Bill b "
                + "LEFT JOIN b.fromDepartment fromDept "
                + "LEFT JOIN b.creater creater "
                + "LEFT JOIN creater.webUserPerson createrPerson "
                + "LEFT JOIN b.toStaff toStaff "
                + "LEFT JOIN toStaff.person toStaffPerson "
                + "WHERE b.billType = :bt AND b.billTypeAtomic = :bta "
                + "AND b.toDepartment = :dept "
                + "AND b.cancelled = false "
                + "AND (b.fullyIssued = false OR b.fullyIssued IS NULL) "
                + "AND (b.completed = false OR b.completed IS NULL) "
                + "ORDER BY b.createdAt DESC";
        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PharmacyIssue);
        params.put("bta", BillTypeAtomic.RETURN_MEDICINE_INWARD);
        params.put("dept", sessionController.getDepartment());
        List<PharmacyReturnFromWardPendingReturnDTO> candidates
                = (List<PharmacyReturnFromWardPendingReturnDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        // Defense for pre-#21510 data: returns fully accepted before this change
        // were never stamped with fullyIssued/completed, so the flag filter above
        // alone would resurface them. Drop anything already fully accepted.
        pendingReturnBills = filterOutFullyAccepted(candidates);
    }

    /**
     * Drops candidates whose every return line has already been fully accepted.
     * Bill-item-only variant of {@link #isFullyAccepted(Bill)} for the
     * pending-list filter above, so listing candidates do not need a full
     * {@link Bill} entity fetch just to check remaining quantities.
     *
     * <p>Outstanding quantities are resolved with two grouped queries covering
     * the whole candidate set rather than one query per bill plus one per bill
     * item, so the pending list stays a fixed number of queries however long
     * the return queue grows.</p>
     */
    private List<PharmacyReturnFromWardPendingReturnDTO> filterOutFullyAccepted(
            List<PharmacyReturnFromWardPendingReturnDTO> candidates) {
        List<PharmacyReturnFromWardPendingReturnDTO> remaining = new ArrayList<>();
        if (candidates == null || candidates.isEmpty()) {
            return remaining;
        }

        List<Long> billIds = new ArrayList<>();
        for (PharmacyReturnFromWardPendingReturnDTO dto : candidates) {
            if (dto.getId() != null) {
                billIds.add(dto.getId());
            }
        }

        // billId -> (returnItemId -> returned qty)
        Map<Long, Map<Long, Double>> returnedQtyByBill = new HashMap<>();
        List<Long> returnItemIds = new ArrayList<>();
        if (!billIds.isEmpty()) {
            String itemJpql = "SELECT bi.bill.id, bi.id, bi.qty FROM BillItem bi "
                    + "WHERE bi.bill.id IN :billIds";
            Map<String, Object> itemParams = new HashMap<>();
            itemParams.put("billIds", billIds);
            List<Object[]> itemRows = billItemFacade.findObjectsArrayByJpql(itemJpql, itemParams, TemporalType.TIMESTAMP);
            if (itemRows != null) {
                for (Object[] row : itemRows) {
                    Long billId = (Long) row[0];
                    Long itemId = (Long) row[1];
                    if (billId == null || itemId == null) {
                        continue;
                    }
                    double qty = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                    Map<Long, Double> lines = returnedQtyByBill.get(billId);
                    if (lines == null) {
                        lines = new HashMap<>();
                        returnedQtyByBill.put(billId, lines);
                    }
                    lines.put(itemId, qty);
                    returnItemIds.add(itemId);
                }
            }
        }

        // returnItemId -> qty already accepted, mirroring the filters in
        // getRemainingQuantityForReturnItem.
        Map<Long, Double> acceptedQtyByItem = new HashMap<>();
        if (!returnItemIds.isEmpty()) {
            String acceptedJpql = "SELECT bi.referanceBillItem.id, SUM(ABS(bi.qty)) FROM BillItem bi "
                    + "WHERE bi.referanceBillItem.id IN :refIds "
                    + "AND bi.bill.billTypeAtomic = :acceptBta "
                    + "AND (bi.bill.retired = false OR bi.bill.retired IS NULL) "
                    + "AND bi.bill.cancelled = false "
                    + "GROUP BY bi.referanceBillItem.id";
            Map<String, Object> acceptedParams = new HashMap<>();
            acceptedParams.put("refIds", returnItemIds);
            acceptedParams.put("acceptBta", BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD);
            List<Object[]> acceptedRows = billItemFacade.findObjectsArrayByJpql(acceptedJpql, acceptedParams, TemporalType.TIMESTAMP);
            if (acceptedRows != null) {
                for (Object[] row : acceptedRows) {
                    Long itemId = (Long) row[0];
                    if (itemId == null) {
                        continue;
                    }
                    acceptedQtyByItem.put(itemId, row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
                }
            }
        }

        for (PharmacyReturnFromWardPendingReturnDTO dto : candidates) {
            Map<Long, Double> lines = dto.getId() != null ? returnedQtyByBill.get(dto.getId()) : null;
            // A return with no lines at all counts as not fully accepted, as in
            // the per-bill check this replaced, so anomalous bills stay visible.
            if (lines == null || lines.isEmpty()) {
                remaining.add(dto);
                continue;
            }
            for (Map.Entry<Long, Double> line : lines.entrySet()) {
                Double accepted = acceptedQtyByItem.get(line.getKey());
                double outstanding = line.getValue() - (accepted != null ? accepted : 0.0);
                if (outstanding > 0.001) {
                    remaining.add(dto);
                    break;
                }
            }
        }
        return remaining;
    }

    public String navigateToReceive() {
        if (returnedBillId == null) {
            JsfUtil.addErrorMessage("No return selected.");
            return null;
        }
        returnedBill = billFacade.find(returnedBillId);
        if (returnedBill == null || !isWithinPendingScope(returnedBill)) {
            // This bean is @SessionScoped, so a department switch after the
            // pending list was rendered can leave a stale row pointing at
            // another department's return. Receiving it would credit the stock
            // to the current department instead (#21471).
            returnedBill = null;
            returnedBillId = null;
            JsfUtil.addErrorMessage("No return selected.");
            loadPendingReturnBills();
            return null;
        }
        if (isFullyAccepted(returnedBill)) {
            JsfUtil.addErrorMessage("This return has already been fully received.");
            loadPendingReturnBills();
            return null;
        }
        generateBillComponent();
        printPreview = false;
        completed = false;
        return "/pharmacy/pharmacy_return_from_ward_receive?faces-redirect=true";
    }

    /**
     * Whether a bill still satisfies the predicates of
     * {@link #loadPendingReturnBills()}, i.e. it really is an uncancelled
     * ward return addressed to the department currently in session.
     */
    private boolean isWithinPendingScope(Bill bill) {
        return bill.getBillType() == BillType.PharmacyIssue
                && bill.getBillTypeAtomic() == BillTypeAtomic.RETURN_MEDICINE_INWARD
                && !bill.isCancelled()
                && bill.getToDepartment() != null
                && sessionController.getDepartment() != null
                && bill.getToDepartment().equals(sessionController.getDepartment());
    }

    private void generateBillComponent() {
        receivedBill = new BilledBill();
        // PharmacyBhtPre (not PharmacyIssue) so PharmacyBean.resolveStockHistoryType
        // does not classify the addToStock credit below as a stock-decreasing
        // "Issue" in bin-card/stock-history reports. Mirrors the analogous
        // ACCEPT_ISSUED_MEDICINE_INWARD bill in WardPharmacyBhtIssueReceiveController (#21467).
        receivedBill.setBillType(BillType.PharmacyBhtPre);
        receivedBill.setBillTypeAtomic(BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD);
        receivedBill.setInstitution(sessionController.getInstitution());
        receivedBill.setDepartment(sessionController.getDepartment());
        receivedBill.setFromDepartment(returnedBill.getFromDepartment());
        receivedBill.setToDepartment(sessionController.getDepartment());
        receivedBill.setToStaff(returnedBill.getToStaff());
        receivedBill.setBackwardReferenceBill(returnedBill);
        receivedBill.setReferenceBill(returnedBill);
        receivedBill.setBillItems(new ArrayList<>());

        List<BillItem> returnedItems = billService.fetchBillItems(returnedBill);
        int serial = 1;
        for (BillItem returnedItem : returnedItems) {
            PharmaceuticalBillItem returnedPbi = returnedItem.getPharmaceuticalBillItem();
            if (returnedPbi == null) {
                continue;
            }
            // Only propose the quantity not yet accepted in a prior visit (#21510).
            double remaining = getRemainingQuantityForReturnItem(returnedItem);
            if (remaining <= 0.001) {
                continue;
            }

            BillItem newItem = new BillItem();
            newItem.setBill(receivedBill);
            newItem.setItem(returnedItem.getItem());
            newItem.setQty(remaining);
            newItem.setReferanceBillItem(returnedItem);
            newItem.setSearialNo(serial++);

            PharmaceuticalBillItem newPbi = new PharmaceuticalBillItem();
            newPbi.copy(returnedPbi);
            newPbi.setBillItem(newItem);
            newPbi.setQty(remaining);
            // The copy above inherits the return's stock binding. A receive
            // must only ever touch the porter's in-transit stock (deducted)
            // and the pharmacy department's stock (added, bound below).
            newPbi.setStock(null);
            newPbi.setStaffStock(null);
            newItem.setPharmaceuticalBillItem(newPbi);

            receivedBill.getBillItems().add(newItem);
        }
    }

    /**
     * Computes the quantity of a return line not yet accepted by the
     * pharmacy, netting off prior {@link BillTypeAtomic#ACCEPT_RETURN_MEDICINE_INWARD}
     * bill items that reference it. There is no cancellation/reversal
     * {@code BillTypeAtomic} for accepted returns, so unlike
     * {@code PharmacySaleBhtController#getRemainingQuantityForItem} only a
     * single reference hop is needed (#21510).
     */
    public double getRemainingQuantityForReturnItem(BillItem returnItem) {
        if (returnItem == null || returnItem.getId() == null) {
            return 0.0;
        }
        String jpql = "SELECT SUM(ABS(bi.qty)) FROM BillItem bi "
                + "WHERE bi.referanceBillItem.id = :refId "
                + "AND bi.bill.billTypeAtomic = :acceptBta "
                + "AND (bi.bill.retired = false OR bi.bill.retired IS NULL) "
                + "AND bi.bill.cancelled = false";
        Map<String, Object> params = new HashMap<>();
        params.put("refId", returnItem.getId());
        params.put("acceptBta", BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD);
        double alreadyAccepted = billItemFacade.findDoubleByJpql(jpql, params);
        return Math.max(0.0, returnItem.getQty() - alreadyAccepted);
    }

    /**
     * Whether every line of a return bill has been fully accepted by the
     * pharmacy. Mirrors {@code PharmacySaleBhtController#isFullyIssued}.
     */
    public boolean isFullyAccepted(Bill returnBill) {
        if (returnBill == null) {
            return false;
        }
        Bill freshBill = billFacade.findWithoutCache(returnBill.getId());
        if (freshBill == null || freshBill.getBillItems() == null || freshBill.getBillItems().isEmpty()) {
            return false;
        }
        for (BillItem item : freshBill.getBillItems()) {
            if (getRemainingQuantityForReturnItem(item) > 0.001) {
                return false;
            }
        }
        return true;
    }

    /**
     * Row-edit listener for the editable "Receiving Quantity" column,
     * clamping the entered quantity to the range
     * {@code [0, remaining-to-accept]}. Mirrors
     * {@code PharmacySaleBhtController#onEditing}.
     */
    public void onEditing(RowEditEvent event) {
        BillItem tmp = (BillItem) event.getObject();
        PharmaceuticalBillItem pbi = tmp.getPharmaceuticalBillItem();

        if (pbi.getQty() < 0) {
            pbi.setQty(0);
            tmp.setQty(0.0);
            JsfUtil.addErrorMessage("Can not enter a minus value");
            return;
        }

        if (tmp.getReferanceBillItem() != null) {
            double remaining = getRemainingQuantityForReturnItem(tmp.getReferanceBillItem());
            if (pbi.getQty() > remaining) {
                JsfUtil.addErrorMessage("Cannot receive " + pbi.getQty()
                        + " units of " + tmp.getItem().getName() + ". Only " + remaining + " units remaining to be accepted.");
                pbi.setQty(remaining);
                tmp.setQty(remaining);
                return;
            }
        }

        tmp.setQty(pbi.getQty());
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
        if (returnedBill == null || returnedBill.getId() == null) {
            JsfUtil.addErrorMessage("No return selected.");
            return;
        }
        if (isFullyAccepted(returnedBill)) {
            JsfUtil.addErrorMessage("This return has already been fully received.");
            return;
        }

        Staff porter = returnedBill.getToStaff();
        if (porter == null) {
            JsfUtil.addErrorMessage("This return has no carrying staff (porter) recorded - cannot receive. Please contact the returning ward.");
            return;
        }
        if (returnedBill.getPatientEncounter() != null
                && returnedBill.getPatientEncounter().isDischarged()) {
            JsfUtil.addErrorMessage("Sorry, patient is discharged.");
            return;
        }

        boolean forceComplete = completed && webUserController.hasPrivilege(Privileges.PharmacyReturnFromWardForceComplete.toString());

        List<BillItem> itemsToAccept = new ArrayList<>();
        if (receivedBill != null && receivedBill.getBillItems() != null) {
            for (BillItem bi : receivedBill.getBillItems()) {
                PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
                double requestedQty = pbi.getQty();
                if (requestedQty < 0.0) {
                    JsfUtil.addErrorMessage("Can not enter a minus value");
                    pbi.setQty(0);
                    bi.setQty(0.0);
                    continue;
                }
                if (requestedQty <= 0.0) {
                    continue;
                }
                if (pbi.getItemBatch() == null || pbi.getItemBatch().getId() == null) {
                    JsfUtil.addErrorMessage("Item " + (bi.getItem() != null ? bi.getItem().getName() : "?") + " has no batch - cannot receive.");
                    pbi.setQty(0);
                    bi.setQty(0.0);
                    continue;
                }

                // Recompute against the latest accepted total - the receivedBill's
                // requestedQty is a snapshot from when the page was loaded, and
                // another visit may have accepted some/all of this line since (#21510).
                double remaining = getRemainingQuantityForReturnItem(bi.getReferanceBillItem());
                if (remaining <= 0.0001) {
                    JsfUtil.addErrorMessage("Item " + (bi.getItem() != null ? bi.getItem().getName() : "?") + " has already been fully accepted - nothing received for this line.");
                    pbi.setQty(0);
                    bi.setQty(0.0);
                    continue;
                }

                String jpql = "select s from Stock s where s.itemBatch=:bc and s.staff=:stf";
                Map<String, Object> params = new HashMap<>();
                params.put("bc", pbi.getItemBatch());
                params.put("stf", porter);
                Stock porterStock = stockFacade.findFirstByJpql(jpql, params, true);
                double available = porterStock == null ? 0.0 : porterStock.getStock();

                double qty = Math.min(Math.min(requestedQty, available), remaining);
                if (qty <= 0.0001) {
                    JsfUtil.addErrorMessage("Insufficient in-transit (porter) stock for " + bi.getItem().getName() + " - nothing received for this line.");
                    pbi.setQty(0);
                    bi.setQty(0.0);
                    continue;
                }
                if (qty + 0.0001 < requestedQty) {
                    JsfUtil.addErrorMessage("Only " + qty + " of " + requestedQty + " units of " + bi.getItem().getName()
                            + " could be received (porter stock / remaining return quantity) - receiving the available quantity.");
                }

                pbi.setQty(qty);
                bi.setQty(qty);
                itemsToAccept.add(bi);
            }
        }

        if (itemsToAccept.isEmpty()) {
            if (!forceComplete) {
                JsfUtil.addErrorMessage("Nothing to receive.");
                return;
            }
        } else {
            String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD);
            receivedBill.setDeptId(deptId);
            receivedBill.setInsId(deptId);
            receivedBill.setCreatedAt(new Date());
            receivedBill.setCreater(sessionController.getLoggedUser());

            // Detach billItems before create() - CascadeType.ALL would otherwise
            // attempt to insert them here, before the per-item stock movements below.
            List<BillItem> items = receivedBill.getBillItems();
            receivedBill.setBillItems(new ArrayList<>());
            billFacade.create(receivedBill);
            receivedBill.setBillItems(items);

            for (BillItem bi : itemsToAccept) {
                PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
                double qty = Math.abs(pbi.getQty());

                // Persist the BillItem (cascading its PharmaceuticalBillItem) before
                // calling the stock movement methods - deductFromStock/addToStock
                // create StockHistory rows referencing the PBI, and that relationship
                // does not cascade from a still-transient BillItem.
                bi.setBill(receivedBill);
                bi.setCreatedAt(new Date());
                bi.setCreater(sessionController.getLoggedUser());
                billItemFacade.create(bi);

                if (pharmacyBean.deductFromStock(pbi, qty, porter)) {
                    Stock addedStock = pharmacyBean.addToStock(pbi, qty, sessionController.getDepartment());
                    pbi.setStock(addedStock);
                } else {
                    JsfUtil.addErrorMessage("Insufficient in-transit (porter) stock for " + bi.getItem().getName() + " - received as zero.");
                    pbi.setQty(0);
                    bi.setQty(0.0);
                }

                billItemFacade.edit(bi);
            }
        }

        // Update remainingQty on the original return items using DB-derived accepted total (#21510)
        Bill freshReturnedBill = billFacade.findWithoutCache(returnedBill.getId());
        for (BillItem returnItem : freshReturnedBill.getBillItems()) {
            BillItem freshReturnItem = billItemFacade.findWithoutCache(returnItem.getId());
            freshReturnItem.setRemainingQty(getRemainingQuantityForReturnItem(freshReturnItem));
            billItemFacade.editAndCommit(freshReturnItem);
        }

        // Auto-complete the return once everything has been accepted (#21510)
        if (!returnedBill.isFullyIssued() && isFullyAccepted(returnedBill)) {
            freshReturnedBill = billFacade.findWithoutCache(returnedBill.getId());
            freshReturnedBill.setFullyIssued(true);
            freshReturnedBill.setFullyIssuedAt(new Date());
            freshReturnedBill.setFullyIssuedBy(sessionController.getLoggedUser());
            billFacade.edit(freshReturnedBill);
            returnedBill.setFullyIssued(true);
            returnedBill.setFullyIssuedAt(freshReturnedBill.getFullyIssuedAt());
            returnedBill.setFullyIssuedBy(freshReturnedBill.getFullyIssuedBy());
        }

        // Manual force-complete (#21510)
        if (forceComplete) {
            freshReturnedBill = billFacade.findWithoutCache(returnedBill.getId());
            freshReturnedBill.setCompleted(true);
            freshReturnedBill.setCompletedAt(new Date());
            freshReturnedBill.setCompletedBy(sessionController.getLoggedUser());
            billFacade.edit(freshReturnedBill);
        }
        completed = false;

        if (itemsToAccept.isEmpty()) {
            // Force-completed with nothing left to receive - no accept bill was created.
            JsfUtil.addSuccessMessage("This return has been marked as completed.");
        } else {
            printPreview = true;
        }
    }

    public String navigateBackToPendingList() {
        makeNull();
        return "/pharmacy/pharmacy_return_from_ward_receive_list?faces-redirect=true";
    }

    public Bill getReturnedBill() {
        return returnedBill;
    }

    public void setReturnedBill(Bill returnedBill) {
        this.returnedBill = returnedBill;
    }

    public Long getReturnedBillId() {
        return returnedBillId;
    }

    public void setReturnedBillId(Long returnedBillId) {
        this.returnedBillId = returnedBillId;
    }

    public Bill getReceivedBill() {
        if (receivedBill == null) {
            receivedBill = new BilledBill();
        }
        return receivedBill;
    }

    public void setReceivedBill(Bill receivedBill) {
        this.receivedBill = receivedBill;
    }

    public List<PharmacyReturnFromWardPendingReturnDTO> getPendingReturnBills() {
        if (pendingReturnBills == null) {
            pendingReturnBills = new ArrayList<>();
        }
        return pendingReturnBills;
    }

    public void setPendingReturnBills(List<PharmacyReturnFromWardPendingReturnDTO> pendingReturnBills) {
        this.pendingReturnBills = pendingReturnBills;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
