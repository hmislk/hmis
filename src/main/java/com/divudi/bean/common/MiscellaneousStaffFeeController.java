package com.divudi.bean.common;

import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PaymentItem;
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
    @Inject
    private PaymentItemController paymentItemController;

    // Form fields
    private Staff selectedStaff;
    private PaymentItem selectedPaymentCategory;
    private double feeAmount;
    private String feeDescription;

    // Temporary storage for building multi-item bill
    private List<TempPaymentItem> tempPaymentItems;
    private double totalAmount;

    // Data lists for reports
    private List<BillFee> filteredMiscellaneousFees;
    private List<Bill> filteredMiscellaneousBills;

    // Report filter fields
    private Date fromDate;
    private Date toDate;
    private Staff filterStaff;

    /**
     * Inner class to hold temporary payment items before bill finalization
     */
    public static class TempPaymentItem implements Serializable {
        private PaymentItem paymentCategory;
        private double amount;
        private String description;

        public TempPaymentItem() {
        }

        public TempPaymentItem(PaymentItem paymentCategory, double amount, String description) {
            this.paymentCategory = paymentCategory;
            this.amount = amount;
            this.description = description;
        }

        public PaymentItem getPaymentCategory() {
            return paymentCategory;
        }

        public void setPaymentCategory(PaymentItem paymentCategory) {
            this.paymentCategory = paymentCategory;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    // Constants
    private static final String MISCELLANEOUS_FEE_ITEM_NAME = "Miscellaneous Staff Fee";
    private static final String MISCELLANEOUS_FEE_ITEM_CODE = "MISC_STAFF_FEE";

    /**
     * Creates a new MiscellaneousStaffFeeController
     */
    public MiscellaneousStaffFeeController() {
    }

    /**
     * Adds a payment item to the temporary list
     */
    public void addPaymentItem() {
        // Validation - Staff must be selected
        if (selectedStaff == null) {
            JsfUtil.addErrorMessage("Please select a staff member first");
            return;
        }

        if (selectedPaymentCategory == null) {
            JsfUtil.addErrorMessage("Please select a payment category");
            return;
        }
        if (feeAmount <= 0) {
            JsfUtil.addErrorMessage("Please enter a valid amount");
            return;
        }
        if (feeDescription == null || feeDescription.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a description for this payment");
            return;
        }

        // Add to temporary list
        TempPaymentItem tempItem = new TempPaymentItem(selectedPaymentCategory, feeAmount, feeDescription);
        getTempPaymentItems().add(tempItem);

        // Update total
        calculateTotal();

        // Clear current entry fields (but keep staff selected)
        selectedPaymentCategory = null;
        feeAmount = 0.0;
        feeDescription = null;

        JsfUtil.addSuccessMessage("Payment item added. Total: " + String.format("%.2f", totalAmount));
    }

    /**
     * Removes a payment item from the temporary list
     */
    public void removePaymentItem(TempPaymentItem item) {
        if (tempPaymentItems != null) {
            tempPaymentItems.remove(item);
            calculateTotal();
            JsfUtil.addSuccessMessage("Payment item removed");
        }
    }

    /**
     * Calculates the total amount from temporary payment items
     */
    private void calculateTotal() {
        totalAmount = 0.0;
        if (tempPaymentItems != null) {
            for (TempPaymentItem item : tempPaymentItems) {
                totalAmount += item.getAmount();
            }
        }
    }

    /**
     * Finalizes and saves the bill with all payment items.
     * Creates the complete Bill → BillItems → BillFees chain.
     */
    public void finalizeBill() {
        // Validation
        if (selectedStaff == null) {
            JsfUtil.addErrorMessage("Please select a staff member");
            return;
        }
        if (tempPaymentItems == null || tempPaymentItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please add at least one payment item");
            return;
        }

        try {
            // Step 1: Create the Bill
            BilledBill miscBill = createMiscellaneousBill();
            billFacade.create(miscBill);

            // Step 2: Create BillItems and BillFees for each temporary payment item
            for (TempPaymentItem tempItem : tempPaymentItems) {
                // Determine which item to use
                Item feeItem;
                if (tempItem.getPaymentCategory() != null) {
                    feeItem = tempItem.getPaymentCategory();
                } else {
                    feeItem = findOrCreateMiscellaneousFeeItem();
                }

                // Create the BillItem
                BillItem billItem = createBillItem(miscBill, feeItem, tempItem.getAmount(), tempItem.getDescription());
                billItemFacade.create(billItem);

                // Create the BillFee
                BillFee billFee = createBillFee(miscBill, billItem, tempItem.getAmount());
                billFeeFacade.create(billFee);
            }

            // Success
            JsfUtil.addSuccessMessage("Bill finalized successfully with " + tempPaymentItems.size() + " payment item(s) for " + selectedStaff.getPerson().getNameWithTitle());
            clearForm();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error finalizing bill: " + e.getMessage());
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
        bill.setTotal(totalAmount);
        bill.setNetTotal(totalAmount);
        bill.setGrantTotal(totalAmount);

        // Set description/comments (summary of items)
        bill.setComments("Miscellaneous fee with " + tempPaymentItems.size() + " item(s)");

        // Set flags
        bill.setCancelled(false);
        bill.setRefunded(false);
        bill.setRetired(false);

        return bill;
    }

    /**
     * Creates a bill item for the miscellaneous fee
     */
    private BillItem createBillItem(Bill bill, Item item, double amount, String description) {
        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(item);

        // Set financial values
        billItem.setGrossValue(amount);
        billItem.setNetValue(amount);
        billItem.setRate(amount);
        billItem.setQty(1.0);
        billItem.setDiscount(0.0);

        // Set staff fee
        billItem.setStaffFee(amount);

        // Set audit fields
        billItem.setCreatedAt(Calendar.getInstance().getTime());
        billItem.setCreater(sessionController.getLoggedUser());

        // Set description
        billItem.setDescreption(description);

        return billItem;
    }

    /**
     * Creates a bill fee for the staff member
     */
    private BillFee createBillFee(Bill bill, BillItem billItem, double amount) {
        BillFee billFee = new BillFee();
        billFee.setBill(bill);
        billFee.setBillItem(billItem);
        billFee.setStaff(selectedStaff);

        // Set fee values (unpaid initially)
        billFee.setFeeValue(amount);
        billFee.setFeeGrossValue(amount);
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
     * Filters miscellaneous fees based on date range and staff
     */
    public void filterMiscellaneousFees() {
        // Validation
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both From Date and To Date");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("From Date cannot be after To Date");
            return;
        }

        StringBuilder jpql = new StringBuilder("SELECT bf FROM BillFee bf ")
                .append("WHERE bf.retired = :ret ")
                .append("AND bf.bill.billTypeAtomic = :bta ")
                .append("AND bf.bill.department = :dept ")
                .append("AND bf.createdAt BETWEEN :fromDate AND :toDate ");

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("bta", BillTypeAtomic.MISCELLANEOUS_STAFF_FEE_BILL);
        params.put("dept", sessionController.getDepartment());
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (filterStaff != null) {
            jpql.append("AND bf.staff = :staff ");
            params.put("staff", filterStaff);
        }

        jpql.append("ORDER BY bf.createdAt DESC");

        filteredMiscellaneousFees = billFeeFacade.findByJpql(jpql.toString(), params);
        JsfUtil.addSuccessMessage("Found " + filteredMiscellaneousFees.size() + " fee record(s)");
    }

    /**
     * Filters miscellaneous bills based on date range and staff
     */
    public void filterMiscellaneousBills() {
        // Validation
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both From Date and To Date");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("From Date cannot be after To Date");
            return;
        }

        StringBuilder jpql = new StringBuilder("SELECT b FROM Bill b ")
                .append("WHERE b.retired = :ret ")
                .append("AND b.billTypeAtomic = :bta ")
                .append("AND b.department = :dept ")
                .append("AND b.createdAt BETWEEN :fromDate AND :toDate ");

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("bta", BillTypeAtomic.MISCELLANEOUS_STAFF_FEE_BILL);
        params.put("dept", sessionController.getDepartment());
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (filterStaff != null) {
            jpql.append("AND b.staff = :staff ");
            params.put("staff", filterStaff);
        }

        jpql.append("ORDER BY b.createdAt DESC");

        filteredMiscellaneousBills = billFacade.findByJpql(jpql.toString(), params);
        JsfUtil.addSuccessMessage("Found " + filteredMiscellaneousBills.size() + " bill(s)");
    }

    /**
     * Navigation methods for reports
     */
    public String navigateToMiscellaneousFeeReport() {
        clearReportFilters();
        return "/opd/professional_payments/miscellaneous_fee_report?faces-redirect=true";
    }

    public String navigateToMiscellaneousBillReport() {
        clearReportFilters();
        return "/opd/professional_payments/miscellaneous_bill_report?faces-redirect=true";
    }

    /**
     * Clears report filter fields
     */
    public void clearReportFilters() {
        fromDate = null;
        toDate = null;
        filterStaff = null;
        filteredMiscellaneousFees = null;
        filteredMiscellaneousBills = null;
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
     * Clears the form fields and temporary items
     */
    public void clearForm() {
        selectedStaff = null;
        selectedPaymentCategory = null;
        feeAmount = 0.0;
        feeDescription = null;
        tempPaymentItems = null;
        totalAmount = 0.0;
    }

    /**
     * Clears only the current payment item entry (keeps staff and existing items)
     */
    public void clearCurrentEntry() {
        selectedPaymentCategory = null;
        feeAmount = 0.0;
        feeDescription = null;
    }

    // Getters and Setters

    public List<TempPaymentItem> getTempPaymentItems() {
        if (tempPaymentItems == null) {
            tempPaymentItems = new ArrayList<>();
        }
        return tempPaymentItems;
    }

    public void setTempPaymentItems(List<TempPaymentItem> tempPaymentItems) {
        this.tempPaymentItems = tempPaymentItems;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Staff getSelectedStaff() {
        return selectedStaff;
    }

    public void setSelectedStaff(Staff selectedStaff) {
        this.selectedStaff = selectedStaff;
    }

    public PaymentItem getSelectedPaymentCategory() {
        return selectedPaymentCategory;
    }

    public void setSelectedPaymentCategory(PaymentItem selectedPaymentCategory) {
        this.selectedPaymentCategory = selectedPaymentCategory;
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

    public List<BillFee> getFilteredMiscellaneousFees() {
        return filteredMiscellaneousFees;
    }

    public void setFilteredMiscellaneousFees(List<BillFee> filteredMiscellaneousFees) {
        this.filteredMiscellaneousFees = filteredMiscellaneousFees;
    }

    public List<Bill> getFilteredMiscellaneousBills() {
        return filteredMiscellaneousBills;
    }

    public void setFilteredMiscellaneousBills(List<Bill> filteredMiscellaneousBills) {
        this.filteredMiscellaneousBills = filteredMiscellaneousBills;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Staff getFilterStaff() {
        return filterStaff;
    }

    public void setFilterStaff(Staff filterStaff) {
        this.filterStaff = filterStaff;
    }
}
