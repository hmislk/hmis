package com.divudi.bean.opd;

import com.divudi.bean.common.AuditEventController;
import com.divudi.bean.common.BillBean;
import com.divudi.bean.common.PriceMatrixController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.*;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.util.JsfUtil;
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
 * Controller for applying discounts to existing OPD Bills retroactively
 * This is for admin use only when there's a written request to change discount schemes
 *
 * @author HMIS Development Team
 */
@Named
@SessionScoped
public class AdminOpdBillDiscountController implements Serializable {

    private static final long serialVersionUID = 1L;

    // EJBs
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;

    // Injected Controllers
    @Inject
    private SessionController sessionController;
    @Inject
    private PaymentSchemeController paymentSchemeController;
    @Inject
    private PriceMatrixController priceMatrixController;
    @Inject
    private BillBean billBean;
    @Inject
    private AuditEventController auditEventController;

    // Instance Variables
    private Bill selectedBill;
    private PaymentScheme newPaymentScheme;
    private String adminComment;

    // Preview data
    private Bill previewBill;
    private List<BillItem> previewBillItems;
    private Map<String, Double> originalValues;
    private Map<String, Double> newValues;
    private boolean previewCalculated = false;

    /**
     * Navigation method to open the discount application page
     */
    public String navigateToApplyDiscount() {
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Please select a bill first");
            return "";
        }

        if (selectedBill.getBillTypeAtomic() != BillTypeAtomic.OPD_BILL_WITH_PAYMENT) {
            JsfUtil.addErrorMessage("This feature is only available for OPD Bills with Payment");
            return "";
        }

        if (selectedBill.isCancelled()) {
            JsfUtil.addErrorMessage("Cannot apply discount to a cancelled bill");
            return "";
        }

        if (selectedBill.isRefunded()) {
            JsfUtil.addErrorMessage("Cannot apply discount to a refunded bill");
            return "";
        }

        // Clear previous data
        clearData();

        // Store original values
        storeOriginalValues();

        return "/opd/admin_opd_bill_discount?faces-redirect=true";
    }

    private void storeOriginalValues() {
        originalValues = new HashMap<>();
        originalValues.put("billTotal", selectedBill.getTotal());
        originalValues.put("billDiscount", selectedBill.getDiscount());
        originalValues.put("billNetTotal", selectedBill.getNetTotal());

        // Store batch bill values if exists
        if (selectedBill.getBackwardReferenceBill() != null) {
            Bill batchBill = selectedBill.getBackwardReferenceBill();
            originalValues.put("batchBillTotal", batchBill.getTotal());
            originalValues.put("batchBillDiscount", batchBill.getDiscount());
            originalValues.put("batchBillNetTotal", batchBill.getNetTotal());
        }
    }

    /**
     * Calculate preview of what the bill will look like with new discount scheme
     */
    public void calculatePreview() {
        if (newPaymentScheme == null) {
            JsfUtil.addErrorMessage("Please select a discount scheme");
            return;
        }

        if (selectedBill == null) {
            JsfUtil.addErrorMessage("No bill selected");
            return;
        }

        try {
            // Create preview data structures
            newValues = new HashMap<>();

            // Fetch all bill items with their fees
            List<BillItem> billItems = getBillItems(selectedBill);

            double newBillTotal = 0.0;
            double newBillDiscount = 0.0;
            double newBillNetTotal = 0.0;

            // Calculate new values for each bill item
            for (BillItem bi : billItems) {
                double itemTotal = 0.0;
                double itemDiscount = 0.0;
                double itemNetTotal = 0.0;

                List<BillFee> billFees = getBillFees(bi);

                for (BillFee bf : billFees) {
                    Department department = null;
                    Item item = null;

                    if (bf.getBillItem() != null && bf.getBillItem().getItem() != null) {
                        department = bf.getBillItem().getItem().getDepartment();
                        item = bf.getBillItem().getItem();
                    }

                    // Get the price matrix for the new payment scheme
                    PriceMatrix priceMatrix = priceMatrixController.getPaymentSchemeDiscount(
                            selectedBill.getPaymentMethod(),
                            newPaymentScheme,
                            department,
                            item);

                    // Calculate new fee values (preview only, don't save)
                    BillFee previewFee = new BillFee();
                    copyBillFeeProperties(bf, previewFee);

                    billBean.setBillFees(previewFee, false, selectedBill.getPaymentMethod(),
                            newPaymentScheme, selectedBill.getCreditCompany(), priceMatrix);

                    itemTotal += previewFee.getFeeGrossValue();
                    itemDiscount += previewFee.getFeeDiscount();
                    itemNetTotal += previewFee.getFeeValue();
                }

                newBillTotal += itemTotal;
                newBillDiscount += itemDiscount;
                newBillNetTotal += itemNetTotal;
            }

            // Store new values
            newValues.put("billTotal", newBillTotal);
            newValues.put("billDiscount", newBillDiscount);
            newValues.put("billNetTotal", newBillNetTotal);

            // Calculate batch bill new values if exists
            if (selectedBill.getBackwardReferenceBill() != null) {
                Bill batchBill = selectedBill.getBackwardReferenceBill();

                // Calculate difference
                double billDifference = newBillNetTotal - selectedBill.getNetTotal();

                newValues.put("batchBillTotal", batchBill.getTotal() + (newBillTotal - selectedBill.getTotal()));
                newValues.put("batchBillDiscount", batchBill.getDiscount() + (newBillDiscount - selectedBill.getDiscount()));
                newValues.put("batchBillNetTotal", batchBill.getNetTotal() + billDifference);
            }

            previewCalculated = true;
            JsfUtil.addSuccessMessage("Preview calculated successfully");

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error calculating preview: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Apply the discount scheme to the bill
     */
    public String applyDiscountScheme() {
        if (newPaymentScheme == null) {
            JsfUtil.addErrorMessage("Please select a discount scheme");
            return "";
        }

        if (adminComment == null || adminComment.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Admin comment is mandatory. Please enter the details of the request.");
            return "";
        }

        if (!previewCalculated) {
            JsfUtil.addErrorMessage("Please calculate preview before applying the discount");
            return "";
        }

        // Create audit event
        AuditEvent auditEvent = auditEventController.createNewAuditEvent(
                "Admin Retroactive Discount Application"
        );

        try {
            // Store original values in audit
            Map<String, String> auditData = new HashMap<>();
            auditData.put("originalPaymentScheme", selectedBill.getPaymentScheme() != null ?
                    selectedBill.getPaymentScheme().getName() : "None");
            auditData.put("newPaymentScheme", newPaymentScheme.getName());
            auditData.put("originalBillTotal", String.format("%.2f", selectedBill.getTotal()));
            auditData.put("originalBillDiscount", String.format("%.2f", selectedBill.getDiscount()));
            auditData.put("originalBillNetTotal", String.format("%.2f", selectedBill.getNetTotal()));
            auditData.put("newBillTotal", String.format("%.2f", newValues.get("billTotal")));
            auditData.put("newBillDiscount", String.format("%.2f", newValues.get("billDiscount")));
            auditData.put("newBillNetTotal", String.format("%.2f", newValues.get("billNetTotal")));
            auditData.put("adminComment", adminComment);
            auditData.put("billId", String.valueOf(selectedBill.getId()));
            auditData.put("billDeptId", selectedBill.getDeptId());

            if (selectedBill.getBackwardReferenceBill() != null) {
                Bill batchBill = selectedBill.getBackwardReferenceBill();
                auditData.put("batchBillId", String.valueOf(batchBill.getId()));
                auditData.put("originalBatchBillTotal", String.format("%.2f", batchBill.getTotal()));
                auditData.put("originalBatchBillDiscount", String.format("%.2f", batchBill.getDiscount()));
                auditData.put("originalBatchBillNetTotal", String.format("%.2f", batchBill.getNetTotal()));
                auditData.put("newBatchBillTotal", String.format("%.2f", newValues.get("batchBillTotal")));
                auditData.put("newBatchBillDiscount", String.format("%.2f", newValues.get("batchBillDiscount")));
                auditData.put("newBatchBillNetTotal", String.format("%.2f", newValues.get("batchBillNetTotal")));
            }

            auditEvent.setDataMap(auditData);
            auditEvent.setComments(adminComment);

            // Apply the discount to all bill fees
            List<BillItem> billItems = getBillItems(selectedBill);

            double newBillTotal = 0.0;
            double newBillDiscount = 0.0;
            double newBillNetTotal = 0.0;

            for (BillItem bi : billItems) {
                double itemTotal = 0.0;
                double itemDiscount = 0.0;
                double itemNetTotal = 0.0;

                List<BillFee> billFees = getBillFees(bi);

                for (BillFee bf : billFees) {
                    Department department = null;
                    Item item = null;

                    if (bf.getBillItem() != null && bf.getBillItem().getItem() != null) {
                        department = bf.getBillItem().getItem().getDepartment();
                        item = bf.getBillItem().getItem();
                    }

                    PriceMatrix priceMatrix = priceMatrixController.getPaymentSchemeDiscount(
                            selectedBill.getPaymentMethod(),
                            newPaymentScheme,
                            department,
                            item);

                    billBean.setBillFees(bf, false, selectedBill.getPaymentMethod(),
                            newPaymentScheme, selectedBill.getCreditCompany(), priceMatrix);

                    // Save the bill fee
                    billFeeFacade.edit(bf);

                    itemTotal += bf.getFeeGrossValue();
                    itemDiscount += bf.getFeeDiscount();
                    itemNetTotal += bf.getFeeValue();
                }

                // Update bill item
                bi.setGrossValue(itemTotal);
                bi.setDiscount(itemDiscount);
                bi.setNetValue(itemNetTotal);
                billItemFacade.edit(bi);

                newBillTotal += itemTotal;
                newBillDiscount += itemDiscount;
                newBillNetTotal += itemNetTotal;
            }

            // Update the bill
            selectedBill.setPaymentScheme(newPaymentScheme);
            selectedBill.setTotal(newBillTotal);
            selectedBill.setDiscount(newBillDiscount);
            selectedBill.setNetTotal(newBillNetTotal);
            selectedBill.setComments((selectedBill.getComments() != null ? selectedBill.getComments() + "\n" : "")
                    + "Discount changed on " + new Date() + " - " + adminComment);
            billFacade.edit(selectedBill);

            // Update batch bill if exists
            if (selectedBill.getBackwardReferenceBill() != null) {
                Bill batchBill = selectedBill.getBackwardReferenceBill();
                batchBill.setPaymentScheme(newPaymentScheme);
                batchBill.setTotal(newValues.get("batchBillTotal"));
                batchBill.setDiscount(newValues.get("batchBillDiscount"));
                batchBill.setNetTotal(newValues.get("batchBillNetTotal"));
                billFacade.edit(batchBill);
            }

            auditEventController.completeAuditEvent(auditEvent);

            JsfUtil.addSuccessMessage("Discount scheme applied successfully");
            clearData();

            return "/opd/view/bill_admin?faces-redirect=true";

        } catch (Exception e) {
            auditEventController.failAuditEvent(auditEvent, "Error: " + e.getMessage());
            JsfUtil.addErrorMessage("Error applying discount: " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    private void copyBillFeeProperties(BillFee source, BillFee target) {
        target.setFee(source.getFee());
        target.setBillItem(source.getBillItem());
        target.setInstitution(source.getInstitution());
        target.setDepartment(source.getDepartment());
        target.setStaff(source.getStaff());
        target.setSpeciality(source.getSpeciality());
        target.setFeeGrossValue(source.getFeeGrossValue());
        target.setFeeDiscount(source.getFeeDiscount());
        target.setFeeValue(source.getFeeValue());
    }

    private List<BillItem> getBillItems(Bill bill) {
        String jpql = "SELECT bi FROM BillItem bi WHERE bi.bill = :bill AND bi.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("bill", bill);
        return billItemFacade.findByJpql(jpql, params);
    }

    private List<BillFee> getBillFees(BillItem billItem) {
        String jpql = "SELECT bf FROM BillFee bf WHERE bf.billItem = :billItem AND bf.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("billItem", billItem);
        return billFeeFacade.findByJpql(jpql, params);
    }

    public void clearData() {
        newPaymentScheme = null;
        adminComment = null;
        previewBill = null;
        previewBillItems = null;
        originalValues = null;
        newValues = null;
        previewCalculated = false;
    }

    // Getters and Setters
    public Bill getSelectedBill() {
        return selectedBill;
    }

    public void setSelectedBill(Bill selectedBill) {
        this.selectedBill = selectedBill;
    }

    public PaymentScheme getNewPaymentScheme() {
        return newPaymentScheme;
    }

    public void setNewPaymentScheme(PaymentScheme newPaymentScheme) {
        this.newPaymentScheme = newPaymentScheme;
    }

    public String getAdminComment() {
        return adminComment;
    }

    public void setAdminComment(String adminComment) {
        this.adminComment = adminComment;
    }

    public Map<String, Double> getOriginalValues() {
        return originalValues;
    }

    public Map<String, Double> getNewValues() {
        return newValues;
    }

    public boolean isPreviewCalculated() {
        return previewCalculated;
    }

    public PaymentSchemeController getPaymentSchemeController() {
        return paymentSchemeController;
    }
}
