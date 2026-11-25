package com.divudi.service.pharmacy;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.ejb.PharmacyBean;
import com.divudi.service.BillService;
import java.util.ArrayList;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Data-admin reversal of a pharmacy direct issue to a BHT, in one transaction
 * (#26109). The reversal bill, its bill items and pharmaceutical bill items are
 * saved first, then the stock goes back, then the original is marked cancelled.
 * Any failure throws {@link BhtIssueReversalException} or a runtime error and
 * rolls back everything, so no stock change stays.
 */
@Stateless
public class BhtIssueReversalService {

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillService billService;

    public static final String REVERSAL_OF_REVERSAL_MESSAGE = "This bill is a reversal and cannot be reversed";

    public static final String REQUEST_ISSUE_MESSAGE = "Request issues cannot be reversed here. Use the BHT issue cancel.";

    /**
     * True only for the direct issue types this reversal handles. Request
     * issues have a ward step and a request to reopen, which the BHT issue
     * cancel does.
     */
    public static boolean isReversibleDirectIssue(Bill bill) {
        if (bill == null) {
            return false;
        }
        BillTypeAtomic atomic = bill.getBillTypeAtomic();
        return atomic == BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE
                || atomic == BillTypeAtomic.DIRECT_ISSUE_INWARD_DISCHARGE_MEDICINE;
    }

    /**
     * True when the bill is itself the reversal of another bill: its reference
     * bill is the original, and the original points back to it as cancelled by.
     */
    public static boolean isReversal(Bill bill) {
        if (bill == null || bill.getReferenceBill() == null) {
            return false;
        }
        Bill cancelledBy = bill.getReferenceBill().getCancelledBill();
        if (cancelledBy == null) {
            return false;
        }
        if (cancelledBy == bill) {
            return true;
        }
        return cancelledBy.getId() != null && cancelledBy.getId().equals(bill.getId());
    }

    /**
     * Sets every money field of the reversal bill to the negated original, so
     * the interim bill and the lists net the reversal against the issue.
     */
    public static void applyNegatedBillValues(Bill reversal, Bill original) {
        double discountPercent = original.getDiscountPercent();
        reversal.copyValue(original);
        reversal.invertValueOfThisBill();
        reversal.setDiscountPercent(discountPercent);
        // invertValueOfThisBill() negates staffFee twice, so set it here.
        reversal.setStaffFee(0 - original.getStaffFee());
        reversal.setMargin(0 - original.getMargin());
        reversal.setServiceCharge(0 - original.getServiceCharge());
        reversal.setSaleValue(0 - original.getSaleValue());
        reversal.setFreeValue(0 - original.getFreeValue());
        reversal.setPerformInstitutionFee(0 - original.getPerformInstitutionFee());
        reversal.setBillerFee(0 - original.getBillerFee());
        reversal.setPaidAmount(0 - original.getPaidAmount());
        reversal.setBalance(0 - original.getBalance());
        reversal.setCashPaid(0 - original.getCashPaid());
        reversal.setCashBalance(0 - original.getCashBalance());
        reversal.setRefundAmount(0 - original.getRefundAmount());
        reversal.setExpenseTotal(0 - original.getExpenseTotal());
        reversal.setClaimableTotal(0 - original.getClaimableTotal());
        reversal.setTotalHospitalFee(0 - original.getTotalHospitalFee());
        reversal.setTotalCenterFee(0 - original.getTotalCenterFee());
        reversal.setTotalStaffFee(0 - original.getTotalStaffFee());
        if (original.hasBillFinanceDetails()) {
            reversal.getBillFinanceDetails().invertValue(original.getBillFinanceDetails());
        }
    }

    public Bill reverse(Long originalBillId, WebUser user, Institution institution, Department department, String deptId, String comments) {
        if (originalBillId == null) {
            throw new BhtIssueReversalException("No Bill Found");
        }
        // Read the original inside this transaction: merging the page's copy
        // could drop its items.
        Bill original = billFacade.findWithoutCache(originalBillId);
        if (original == null) {
            throw new BhtIssueReversalException("No Bill Found");
        }
        if (original.getBillType() != BillType.PharmacyBhtPre) {
            throw new BhtIssueReversalException("Invalid Bill Type for Reversal");
        }
        if (!isReversibleDirectIssue(original)) {
            throw new BhtIssueReversalException(REQUEST_ISSUE_MESSAGE);
        }
        if (isReversal(original)) {
            throw new BhtIssueReversalException(REVERSAL_OF_REVERSAL_MESSAGE);
        }
        if (original.isCancelled() || original.getCancelledBill() != null) {
            throw new BhtIssueReversalException("Bill is already cancelled. Cannot reverse cancelled bills");
        }
        if (original.getPatientEncounter() != null && original.getPatientEncounter().isPaymentFinalized()) {
            throw new BhtIssueReversalException("This Bill Already Discharged");
        }
        if (original.getCheckedBy() != null) {
            throw new BhtIssueReversalException("Checked Bill. Can not reverse");
        }
        if (original.checkActiveReturnBhtIssueBills()) {
            throw new BhtIssueReversalException("There are some return bills for this. Please cancel those bills first");
        }

        Bill reversalBill = new Bill();
        reversalBill.copy(original);
        // The reason typed on the reverse page; the original's stored comments are not changed.
        if (comments != null && !comments.trim().isEmpty()) {
            reversalBill.setComments(comments);
        }
        reversalBill.setCreatedAt(new Date());
        reversalBill.setCreater(user);
        reversalBill.setInstitution(institution);
        reversalBill.setDepartment(department);
        reversalBill.setBillDate(new Date());
        reversalBill.setBillTime(new Date());
        reversalBill.setDepartmentType(original.getDepartmentType());
        reversalBill.setReferenceBill(original);
        reversalBill.setBilledBill(original);
        reversalBill.setForwardReferenceBill(original.getForwardReferenceBill());
        reversalBill.setBillType(original.getBillType());
        reversalBill.setBillTypeAtomic(original.getBillTypeAtomic());
        reversalBill.setDeptId(deptId);
        reversalBill.setBillItems(new ArrayList<>());
        applyNegatedBillValues(reversalBill, original);

        ArrayList<PharmaceuticalBillItem> reversalPbis = new ArrayList<>();
        for (BillItem originalItem : original.getBillItems()) {
            BillItem reversalItem = new BillItem();
            reversalItem.copy(originalItem);
            reversalItem.setBill(reversalBill);
            reversalItem.setCreatedAt(new Date());
            reversalItem.setCreater(user);
            reversalItem.invertValue(originalItem);

            PharmaceuticalBillItem originalPbi = originalItem.getPharmaceuticalBillItem();
            if (originalPbi != null) {
                PharmaceuticalBillItem reversalPbi = new PharmaceuticalBillItem();
                reversalPbi.copy(originalPbi);
                reversalPbi.setBillItem(reversalItem);
                reversalPbi.setCreatedAt(new Date());
                reversalPbi.setCreater(user);
                reversalPbi.invertValue(originalPbi);
                reversalItem.setPharmaceuticalBillItem(reversalPbi);
                reversalPbis.add(reversalPbi);
            }
            reversalBill.getBillItems().add(reversalItem);
        }

        // New entities are saved with create(), in order: bill, items, pharmaceutical items.
        billFacade.create(reversalBill);
        for (BillItem reversalItem : reversalBill.getBillItems()) {
            billItemFacade.create(reversalItem);
        }
        for (PharmaceuticalBillItem reversalPbi : reversalPbis) {
            pharmaceuticalBillItemFacade.create(reversalPbi);
        }

        // Only now the stock goes back, with the saved pharmaceutical bill items.
        for (PharmaceuticalBillItem reversalPbi : reversalPbis) {
            if (reversalPbi.getQty() != 0 && reversalPbi.getStock() != null) {
                if (!pharmacyBean.addToStock(reversalPbi.getStock(), reversalPbi.getQty(), reversalPbi, department)) {
                    throw new BhtIssueReversalException("Stock could not be restored. Nothing was changed.");
                }
            }
        }

        original.setCancelled(true);
        original.setCancelledBill(reversalBill);
        billFacade.edit(original);
        return reversalBill;
    }
}
