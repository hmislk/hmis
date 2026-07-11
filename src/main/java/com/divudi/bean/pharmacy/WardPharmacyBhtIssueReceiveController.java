package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.Privileges;
import com.divudi.core.data.BillTypeAtomic;
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

/**
 * Ward-side confirmation of medicines received from the porter for a
 * {@link BillTypeAtomic#ISSUE_MEDICINE_ON_REQUEST_INWARD} issue bill
 * (#21467). Mirrors the stock-movement pattern of
 * {@link TransferReceiveController}: deducts the porter's in-transit stock
 * and credits the ward department's stock, recording the confirmation as a
 * {@link BillTypeAtomic#ACCEPT_ISSUED_MEDICINE_INWARD} bill linked back to
 * the issue via {@link Bill#getBackwardReferenceBill()}.
 */
@Named
@SessionScoped
public class WardPharmacyBhtIssueReceiveController implements Serializable {

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

    private Bill issuedBill;
    private Bill receivedBill;
    private List<Bill> pendingIssueBills;
    private boolean printPreview;
    private boolean settling;

    public void makeNull() {
        issuedBill = null;
        receivedBill = null;
        printPreview = false;
    }

    public String navigateToPendingList() {
        makeNull();
        loadPendingIssueBills();
        return "/ward/ward_pharmacy_bht_issue_receive_list?faces-redirect=true";
    }

    public void loadPendingIssueBills() {
        String jpql = "SELECT b FROM Bill b WHERE b.billType = :bt AND b.billTypeAtomic = :bta "
                + "AND b.patientEncounter.currentPatientRoom.roomFacilityCharge.department = :dept "
                + "AND b.cancelled = false "
                + "AND NOT EXISTS (SELECT r FROM Bill r WHERE r.backwardReferenceBill = b "
                + "AND r.billTypeAtomic = :acceptBta AND r.cancelled = false) "
                + "ORDER BY b.createdAt DESC";
        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PharmacyBhtPre);
        params.put("bta", BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD);
        params.put("dept", sessionController.getDepartment());
        params.put("acceptBta", BillTypeAtomic.ACCEPT_ISSUED_MEDICINE_INWARD);
        pendingIssueBills = billFacade.findByJpql(jpql, params);
    }

    public String navigateToReceive() {
        if (issuedBill == null || issuedBill.getId() == null) {
            JsfUtil.addErrorMessage("No issue selected.");
            return null;
        }
        if (isAlreadyReceived(issuedBill)) {
            JsfUtil.addErrorMessage("This issue has already been received.");
            loadPendingIssueBills();
            return null;
        }
        generateBillComponent();
        printPreview = false;
        return "/ward/ward_pharmacy_bht_issue_receive?faces-redirect=true";
    }

    private void generateBillComponent() {
        receivedBill = new BilledBill();
        receivedBill.setBillType(BillType.PharmacyBhtPre);
        receivedBill.setBillTypeAtomic(BillTypeAtomic.ACCEPT_ISSUED_MEDICINE_INWARD);
        receivedBill.setInstitution(sessionController.getInstitution());
        receivedBill.setDepartment(sessionController.getDepartment());
        receivedBill.setFromInstitution(issuedBill.getInstitution());
        receivedBill.setFromDepartment(issuedBill.getDepartment());
        receivedBill.setToStaff(issuedBill.getToStaff());
        receivedBill.setPatient(issuedBill.getPatient());
        receivedBill.setPatientEncounter(issuedBill.getPatientEncounter());
        receivedBill.setBackwardReferenceBill(issuedBill);
        receivedBill.setReferenceBill(issuedBill);
        receivedBill.setBillItems(new ArrayList<>());

        List<BillItem> issuedItems = billService.fetchBillItems(issuedBill);
        int serial = 1;
        for (BillItem issuedItem : issuedItems) {
            PharmaceuticalBillItem issuedPbi = issuedItem.getPharmaceuticalBillItem();
            if (issuedPbi == null) {
                continue;
            }
            double qty = Math.abs(issuedPbi.getQty());
            if (qty <= 0.0) {
                continue;
            }

            BillItem newItem = new BillItem();
            newItem.setBill(receivedBill);
            newItem.setItem(issuedItem.getItem());
            newItem.setQty(qty);
            newItem.setPatientEncounter(issuedItem.getPatientEncounter());
            newItem.setReferanceBillItem(issuedItem);
            newItem.setSearialNo(serial++);

            PharmaceuticalBillItem newPbi = new PharmaceuticalBillItem();
            newPbi.copy(issuedPbi);
            newPbi.setBillItem(newItem);
            newPbi.setQty(qty);
            // The copy above inherits the issuing department's stock binding.
            // A receive must only ever touch the porter's in-transit stock
            // (deducted) and the ward department's stock (added, bound below).
            newPbi.setStock(null);
            newPbi.setStaffStock(null);
            newItem.setPharmaceuticalBillItem(newPbi);

            receivedBill.getBillItems().add(newItem);
        }
    }

    public void settle() {
        if (!webUserController.hasPrivilege(Privileges.InwardPharmacyBhtReceive.name())) {
            JsfUtil.addErrorMessage("You do not have the privilege to receive this issue.");
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
        if (receivedBill == null || receivedBill.getBillItems() == null || receivedBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Nothing to receive.");
            return;
        }
        if (isAlreadyReceived(issuedBill)) {
            JsfUtil.addErrorMessage("This issue has already been received.");
            return;
        }

        Staff porter = issuedBill.getToStaff();
        if (porter == null) {
            JsfUtil.addErrorMessage("This issue has no carrying staff (porter) recorded - cannot receive. Please contact the issuing pharmacy.");
            return;
        }

        if (!porterStockCoversAllLines(porter)) {
            return;
        }

        String deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.ACCEPT_ISSUED_MEDICINE_INWARD);
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

        for (BillItem bi : receivedBill.getBillItems()) {
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

        issuedBill.getForwardReferenceBills().add(receivedBill);
        billFacade.edit(issuedBill);

        printPreview = true;
    }

    /**
     * Validates that the porter holds enough in-transit stock to cover EVERY
     * line being received, before any data is written, mirroring
     * {@code TransferReceiveController#porterStockCoversAllLines} (#21266).
     */
    private boolean porterStockCoversAllLines(Staff porter) {
        Map<Long, Double> requiredUnitsByBatch = new HashMap<>();
        Map<Long, BillItem> sampleItemByBatch = new HashMap<>();
        for (BillItem bi : receivedBill.getBillItems()) {
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            double qty = Math.abs(pbi.getQty());
            if (qty <= 0.0) {
                continue;
            }
            if (pbi.getItemBatch() == null || pbi.getItemBatch().getId() == null) {
                JsfUtil.addErrorMessage("Item " + (bi.getItem() != null ? bi.getItem().getName() : "?") + " has no batch - cannot receive.");
                return false;
            }
            Long batchId = pbi.getItemBatch().getId();
            requiredUnitsByBatch.merge(batchId, qty, Double::sum);
            sampleItemByBatch.put(batchId, bi);
        }

        boolean allCovered = true;
        for (Map.Entry<Long, Double> e : requiredUnitsByBatch.entrySet()) {
            BillItem bi = sampleItemByBatch.get(e.getKey());
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            String jpql = "select s from Stock s where s.itemBatch=:bc and s.staff=:stf";
            Map<String, Object> params = new HashMap<>();
            params.put("bc", pbi.getItemBatch());
            params.put("stf", porter);
            Stock porterStock = stockFacade.findFirstByJpql(jpql, params, true);
            double available = porterStock == null ? 0.0 : porterStock.getStock();
            if (available + 0.0001 < e.getValue()) {
                JsfUtil.addErrorMessage("Insufficient in-transit (porter) stock for " + bi.getItem().getName()
                        + ": receiving " + e.getValue() + " but porter holds " + available
                        + ". Nothing was received.");
                allCovered = false;
            }
        }
        return allCovered;
    }

    public boolean isAlreadyReceived(Bill bill) {
        if (bill == null || bill.getId() == null) {
            return false;
        }
        String jpql = "SELECT count(r) FROM Bill r WHERE r.backwardReferenceBill.id = :issueId "
                + "AND r.billTypeAtomic = :acceptBta AND r.cancelled = false";
        Map<String, Object> params = new HashMap<>();
        params.put("issueId", bill.getId());
        params.put("acceptBta", BillTypeAtomic.ACCEPT_ISSUED_MEDICINE_INWARD);
        return billFacade.findLongByJpql(jpql, params) > 0;
    }

    public String navigateBackToPendingList() {
        makeNull();
        return "/ward/ward_pharmacy_bht_issue_receive_list?faces-redirect=true";
    }

    public Bill getIssuedBill() {
        return issuedBill;
    }

    public void setIssuedBill(Bill issuedBill) {
        this.issuedBill = issuedBill;
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

    public List<Bill> getPendingIssueBills() {
        if (pendingIssueBills == null) {
            pendingIssueBills = new ArrayList<>();
        }
        return pendingIssueBills;
    }

    public void setPendingIssueBills(List<Bill> pendingIssueBills) {
        this.pendingIssueBills = pendingIssueBills;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }
}
