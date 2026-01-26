package com.divudi.bean.common;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Staff;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.StaffFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.ejb.BillNumberGenerator;
import java.io.Serializable;
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
 * Controller for managing miscellaneous staff fees that are not related to patient bills.
 * These fees can include administrative fees, bonuses, allowances, meeting fees, etc.
 *
 * @author HMIS Development Team
 */
@Named
@SessionScoped
public class MiscellaneousStaffFeeController implements Serializable {

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private StaffFacade staffFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillNumberGenerator billNumberBean;

    @Inject
    private SessionController sessionController;
    @Inject
    private ItemController itemController;

    // Form fields
    private Staff selectedStaff;
    private double feeAmount;
    private String feeDescription;

    // Data lists
    private List<BillFee> recentMiscellaneousFees;

    // Constants
    private static final String MISCELLANEOUS_FEE_ITEM_NAME = "Miscellaneous Staff Fee";
    private static final String MISCELLANEOUS_FEE_ITEM_CODE = "MISC_STAFF_FEE";

    /**
     * Creates a new MiscellaneousStaffFeeController
     */
    public MiscellaneousStaffFeeController() {
    }

    /**
     * Saves a new miscellaneous fee for a staff member.
     * Creates the complete Bill → BillItem → BillFee chain.
     */
    public void saveNewMiscellaneousFee() {
        // Validation
        if (selectedStaff == null) {
            JsfUtil.addErrorMessage("Please select a staff member");
            return;
        }
        if (feeAmount <= 0) {
            JsfUtil.addErrorMessage("Please enter a valid amount");
            return;
        }
        if (feeDescription == null || feeDescription.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a description for this fee");
            return;
        }

        try {
            // Step 1: Create or get the miscellaneous fee Item
            Item miscFeeItem = findOrCreateMiscellaneousFeeItem();

            // Step 2: Create the Bill
            BilledBill miscBill = createMiscellaneousBill();
            billFacade.create(miscBill);

            // Step 3: Create the BillItem
            BillItem billItem = createBillItem(miscBill, miscFeeItem);
            billItemFacade.create(billItem);

            // Step 4: Create the BillFee
            BillFee billFee = createBillFee(miscBill, billItem);
            billFeeFacade.create(billFee);

            // Success
            JsfUtil.addSuccessMessage("Miscellaneous fee added successfully for " + selectedStaff.getPerson().getNameWithTitle());
            clearForm();
            loadRecentMiscellaneousFees();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving miscellaneous fee: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Finds existing miscellaneous fee item or creates a new one if it doesn't exist
     */
    private Item findOrCreateMiscellaneousFeeItem() {
        // Try to find existing item
        String jpql = "SELECT i FROM Item i WHERE i.retired = :ret AND i.code = :code";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("code", MISCELLANEOUS_FEE_ITEM_CODE);

        Item item = itemFacade.findFirstByJpql(jpql, params);

        if (item == null) {
            // Create new item
            item = new Item();
            item.setName(MISCELLANEOUS_FEE_ITEM_NAME);
            item.setCode(MISCELLANEOUS_FEE_ITEM_CODE);
            item.setCreatedAt(new Date());
            item.setCreater(sessionController.getLoggedUser());
            item.setDepartment(sessionController.getDepartment());
            item.setInstitution(sessionController.getInstitution());
            item.setRetired(false);
            itemFacade.create(item);
        }

        return item;
    }

    /**
     * Creates a new miscellaneous bill with proper bill numbering
     */
    private BilledBill createMiscellaneousBill() {
        BilledBill bill = new BilledBill();
        bill.setBillType(BillType.PaymentBill);
        bill.setBillTypeAtomic(BillTypeAtomic.MISCELLANEOUS_STAFF_FEE_BILL);
        bill.setBillClassType(BillClassType.BilledBill);

        // Set dates
        bill.setBillDate(Calendar.getInstance().getTime());
        bill.setCreatedAt(Calendar.getInstance().getTime());

        // Set user and department info
        bill.setCreater(sessionController.getLoggedUser());
        bill.setDepartment(sessionController.getDepartment());
        bill.setInstitution(sessionController.getInstitution());

        // Generate bill number using yearly numbering
        String billNumber = billNumberBean.departmentBillNumberGeneratorYearly(
            sessionController.getDepartment(),
            BillTypeAtomic.MISCELLANEOUS_STAFF_FEE_BILL
        );
        bill.setDeptId(billNumber);
        bill.setInsId(billNumber);

        // Set staff and financial values
        bill.setStaff(selectedStaff);
        bill.setToStaff(selectedStaff);
        bill.setTotal(feeAmount);
        bill.setNetTotal(feeAmount);
        bill.setGrantTotal(feeAmount);

        // Set description/comments
        bill.setComments(feeDescription);

        // Set flags
        bill.setCancelled(false);
        bill.setRefunded(false);
        bill.setRetired(false);

        return bill;
    }

    /**
     * Creates a bill item for the miscellaneous fee
     */
    private BillItem createBillItem(Bill bill, Item item) {
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(item);

        // Set financial values
        billItem.setGrossValue(feeAmount);
        billItem.setNetValue(feeAmount);
        billItem.setRate(feeAmount);
        billItem.setQty(1.0);
        billItem.setDiscount(0.0);

        // Set staff fee
        billItem.setStaffFee(feeAmount);

        // Set audit fields
        billItem.setCreatedAt(Calendar.getInstance().getTime());
        billItem.setCreater(sessionController.getLoggedUser());

        // Set description
        billItem.setDescreption(feeDescription);

        return billItem;
    }

    /**
     * Creates a bill fee for the staff member
     */
    private BillFee createBillFee(Bill bill, BillItem billItem) {
        BillFee billFee = new BillFee();
        billFee.setBill(bill);
        billFee.setBillItem(billItem);
        billFee.setStaff(selectedStaff);

        // Set fee values (unpaid initially)
        billFee.setFeeValue(feeAmount);
        billFee.setFeeGrossValue(feeAmount);
        billFee.setPaidValue(0.0);  // Unpaid
        billFee.setSettleValue(0.0);

        // Set organizational context
        billFee.setInstitution(sessionController.getInstitution());
        billFee.setDepartment(sessionController.getDepartment());
        if (selectedStaff.getSpeciality() != null) {
            billFee.setSpeciality(selectedStaff.getSpeciality());
        }

        // Set audit fields
        billFee.setCreatedAt(new Date());
        billFee.setCreater(sessionController.getLoggedUser());
        billFee.setRetired(false);

        return billFee;
    }

    /**
     * Loads recent miscellaneous fees for display
     */
    public void loadRecentMiscellaneousFees() {
        String jpql = "SELECT bf FROM BillFee bf "
                + "WHERE bf.retired = :ret "
                + "AND bf.bill.billTypeAtomic = :bta "
                + "AND bf.bill.department = :dept "
                + "ORDER BY bf.createdAt DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("bta", BillTypeAtomic.MISCELLANEOUS_STAFF_FEE_BILL);
        params.put("dept", sessionController.getDepartment());

        recentMiscellaneousFees = billFeeFacade.findByJpql(jpql, params, 100);
    }

    /**
     * Autocomplete method for staff selection
     */
    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return suggestions;
        }

        String jpql = "SELECT s FROM Staff s "
                + "WHERE s.retired = :ret "
                + "AND (LOWER(s.person.name) LIKE :q "
                + "OR LOWER(s.code) LIKE :q) "
                + "ORDER BY s.person.name";

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("q", "%" + query.toLowerCase() + "%");

        suggestions = staffFacade.findByJpql(jpql, params, 20);
        return suggestions;
    }

    /**
     * Clears the form fields
     */
    public void clearForm() {
        selectedStaff = null;
        feeAmount = 0.0;
        feeDescription = null;
    }

    // Getters and Setters

    public Staff getSelectedStaff() {
        return selectedStaff;
    }

    public void setSelectedStaff(Staff selectedStaff) {
        this.selectedStaff = selectedStaff;
    }

    public double getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(double feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getFeeDescription() {
        return feeDescription;
    }

    public void setFeeDescription(String feeDescription) {
        this.feeDescription = feeDescription;
    }

    public List<BillFee> getRecentMiscellaneousFees() {
        if (recentMiscellaneousFees == null) {
            loadRecentMiscellaneousFees();
        }
        return recentMiscellaneousFees;
    }

    public void setRecentMiscellaneousFees(List<BillFee> recentMiscellaneousFees) {
        this.recentMiscellaneousFees = recentMiscellaneousFees;
    }
}
