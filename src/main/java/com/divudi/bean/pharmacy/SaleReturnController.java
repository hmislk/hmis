/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.service.StaffService;
import com.divudi.service.PaymentService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.service.BillService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class SaleReturnController implements Serializable, com.divudi.bean.common.ControllerWithMultiplePayments {

    private Bill bill;
    private Bill returnBill;
    private Bill finalReturnBill;
    private boolean printPreview;
    private String returnBillcomment;
    private PaymentMethod returnPaymentMethod;
    private Staff toStaff;
    ////////

    private List<BillItem> billItems;
    ///////
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    BillService billService;
    @Inject
    private PharmaceuticalItemController pharmaceuticalItemController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    DrawerController drawerController;
    @Inject
    private SessionController sessionController;
    @Inject
    private com.divudi.bean.common.PatientDepositController patientDepositController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    StaffService staffBean;
    @EJB
    PaymentService paymentService;

    PaymentMethodData paymentMethodData;

    public String navigateToReturnItemsAndPaymentsForPharmacyRetailSale() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Please select a bill to return");
            return null;
        }
        if (bill.isCancelled()) {
            JsfUtil.addErrorMessage("Cancelled Bills CAN NOT BE returned");
            return null;
        }
        returnBill = null;
        finalReturnBill = null;
        printPreview = false;
        billItems = null;
        paymentMethodData = new PaymentMethodData();

        // Copy patient from original bill to return bill BEFORE generating components
        getReturnBill().setPatient(bill.getPatient());

        generateBillComponent();

        List<Payment> originalPayments;
        Bill paymentBill = null;

        //PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER
        if (bill.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("Programming Error. Inform the system administrator.");
            return null;
        } else if (bill.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_RETAIL_SALE) {
            paymentBill = bill;
            bill.getReferenceBill();
        } else if (bill.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE) {
            paymentBill = bill.getReferenceBill();
            if (paymentBill.getBillTypeAtomic() == null) {
                JsfUtil.addErrorMessage("Programming Error. Inform the system administrator.");
                return null;
            }
            if (paymentBill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_RETAIL_SALE) {
                JsfUtil.addErrorMessage("Programming Error. Inform the system administrator.");
                return null;
            }
        } else if (bill.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER) {
            paymentBill = bill.getReferenceBill();
            if (paymentBill.getBillTypeAtomic() == null) {
                JsfUtil.addErrorMessage("Programming Error. Inform the system administrator.");
                return null;
            }
            if (paymentBill.getBillTypeAtomic() != BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER) {
                JsfUtil.addErrorMessage("Programming Error. Inform the system administrator.");
                return null;
            }
        } else if (bill.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER) {
            paymentBill = bill;
        } else {
            JsfUtil.addErrorMessage("Programming Error. Not a suitable bill type atomic. Inform the system administrator.");
            return null;
        }

        // Fetch and initialize payment data from original bill
        originalPayments = billService.fetchBillPayments(paymentBill);

        if (originalPayments != null && !originalPayments.isEmpty()) {
            // Initialize payment method data based on original payments
            initializeRefundPaymentFromOriginalPayments(originalPayments);
        } else {
            // Fallback: just set payment method enum if no payment details found
            returnPaymentMethod = bill.getPaymentMethod();
        }

        return "/pharmacy/pharmacy_bill_return_retail?faces-redirect=true";
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Bill getReturnBill() {
        if (returnBill == null) {
            returnBill = new RefundBill();
        }
        return returnBill;
    }

    public void setReturnBill(Bill returnBill) {
        this.returnBill = returnBill;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    @Inject
    private PharmacyCalculation pharmacyRecieveBean;

    public void onEdit(BillItem tmp) {
        //    PharmaceuticalBillItem tmp = (PharmaceuticalBillItem) event.getObject();
        double remainingQty = getPharmacyRecieveBean().calQty3(tmp.getReferanceBillItem());
        if (tmp.getQty() > remainingQty) {
            tmp.setQty(0.0);
            JsfUtil.addErrorMessage("You cant return over than the remaining quanty to return. The returning qtantity was set to Remaining Quantity.");
        }

        calTotal();
        getPharmacyController().setPharmacyItem(tmp.getPharmaceuticalBillItem().getBillItem().getItem());
    }

    public void makeNull() {
        bill = null;
        returnBill = null;
        finalReturnBill = null;
        printPreview = false;
        billItems = null;

    }

    public String getReturnBillcomment() {
        return returnBillcomment;
    }

    public void setReturnBillcomment(String returnBillcomment) {
        this.returnBillcomment = returnBillcomment;
    }

    private void savePreReturnBill() {
        //   getReturnBill().setReferenceBill(getBill());

        getReturnBill().copy(getBill());

        getReturnBill().setPaymentMethod(returnPaymentMethod);
        getReturnBill().setBillType(BillType.PharmacyPre);
        getReturnBill().setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL);

        getReturnBill().setBilledBill(getBill());

        getReturnBill().setTotal(0 - Math.abs(getReturnBill().getTotal()));
        getReturnBill().setNetTotal(0 - Math.abs(getReturnBill().getNetTotal()));
        getReturnBill().setDiscount(0 - Math.abs(getReturnBill().getDiscount()));

        getReturnBill().setCreater(getSessionController().getLoggedUser());
        getReturnBill().setCreatedAt(Calendar.getInstance().getTime());

        getReturnBill().setInstitution(getSessionController().getInstitution());
        getReturnBill().setDepartment(getSessionController().getDepartment());

        // Handle Department ID generation
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund Pre Bill - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL);
        } else {
            // Use existing method for backward compatibility
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL);
        }

        // Handle Institution ID generation (completely separate)
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL);
        } else {
            if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
                insId = deptId; // Use same number as department
            } else {
                // Use existing method for backward compatibility
                insId = deptId;
            }
        }

        getReturnBill().setInsId(insId);
        getReturnBill().setDeptId(deptId);

        if (getReturnBill().getId() == null) {
            getBillFacade().create(getReturnBill());
        } else {
            getBillFacade().edit(getReturnBill());
        }

    }

    private Bill saveSaleFinalReturnBill() {
        RefundBill finalRefundBill = new RefundBill();
        finalRefundBill.copy(getReturnBill());
        finalRefundBill.setPaymentMethod(returnPaymentMethod);
        finalRefundBill.setBillType(BillType.PharmacySale);
        finalRefundBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);

        finalRefundBill.setReferenceBill(getReturnBill());
        finalRefundBill.setTotal(getReturnBill().getTotal());
        finalRefundBill.setNetTotal(getReturnBill().getNetTotal());
        finalRefundBill.setDiscount(getReturnBill().getDiscount());

        finalRefundBill.setCreater(getSessionController().getLoggedUser());
        finalRefundBill.setCreatedAt(Calendar.getInstance().getTime());

        finalRefundBill.setInstitution(getSessionController().getInstitution());
        finalRefundBill.setDepartment(getSessionController().getDepartment());
        finalRefundBill.setComments(returnBillcomment);

        // Handle Department ID generation
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
        } else {
            // Use existing method for backward compatibility
            deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
        }

        // Handle Institution ID generation (completely separate)
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund - Prefix + Institution Code + Department Code + Year + Yearly Number", false)) {
            insId = deptId;
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            insId = deptId;
        } else {
            insId = deptId;
        }

        finalRefundBill.setInsId(insId);
        finalRefundBill.setDeptId(deptId);
        finalRefundBill.setBillTime(new Date());

        if (finalRefundBill.getId() == null) {
            getBillFacade().create(finalRefundBill);
        }

        getReturnBill().setReferenceBill(finalRefundBill);
        getBillFacade().edit(getReturnBill());

        return finalRefundBill;

    }

    private void savePreReturnBillComponents() {
        for (BillItem returningBillItem : getBillItems()) {
            returningBillItem.getPharmaceuticalBillItem().setQty(returningBillItem.getQty());
            if (returningBillItem.getPharmaceuticalBillItem().getQty() == 0.0) {
                continue;
            }
            returningBillItem.setBill(getReturnBill());
            returningBillItem.setCreatedAt(Calendar.getInstance().getTime());
            returningBillItem.setCreater(getSessionController().getLoggedUser());
            //   i.getBillItem().setQty(i.getPharmaceuticalBillItem().getQty());
            double grossValue = returningBillItem.getRate() * returningBillItem.getQty();
            double netValue = returningBillItem.getNetRate() * returningBillItem.getQty();
            double discountValue = returningBillItem.getDiscountRate() * returningBillItem.getQty();
            returningBillItem.setGrossValue(0 - grossValue);
            returningBillItem.setNetValue(0 - netValue);
            returningBillItem.setDiscount(discountValue);

            PharmaceuticalBillItem tmpPh = returningBillItem.getPharmaceuticalBillItem();
            tmpPh.setBillItem(returningBillItem);
            returningBillItem.setPharmaceuticalBillItem(tmpPh);

            if (returningBillItem.getId() == null) {
                getBillItemFacade().create(returningBillItem);
            } else {
                getBillItemFacade().edit(returningBillItem);
            }
            getPharmacyBean().addToStock(tmpPh.getStock(), Math.abs(tmpPh.getQty()), tmpPh, getSessionController().getDepartment());
        }
        returnBill = billService.reloadBill(returnBill);
        updateReturnTotal();

    }

    private void saveSaleComponent(Bill finalReturnBill) {
        for (BillItem returnBillItem : getReturnBill().getBillItems()) {
            BillItem finalReturnBillItem = new BillItem();
            finalReturnBillItem.copy(returnBillItem);
            finalReturnBillItem.setBill(finalReturnBill);
            finalReturnBillItem.setCreatedAt(Calendar.getInstance().getTime());
            finalReturnBillItem.setCreater(getSessionController().getLoggedUser());

            PharmaceuticalBillItem finalReturnPbi = new PharmaceuticalBillItem();
            finalReturnPbi.copy(returnBillItem.getPharmaceuticalBillItem());
            finalReturnPbi.setBillItem(finalReturnBillItem);
            finalReturnBillItem.setPharmaceuticalBillItem(finalReturnPbi);

            if (finalReturnBillItem.getId() == null) {
                getBillItemFacade().create(finalReturnBillItem);
            } else {
                getBillItemFacade().edit(finalReturnBillItem);
            }
        }
        getBillFacade().edit(finalReturnBill);
    }

    public void saveBillFee(BillItem bi) {
        BillFee bf = new BillFee();
        bf.setCreatedAt(Calendar.getInstance().getTime());
        bf.setCreater(getSessionController().getLoggedUser());
        bf.setBillItem(bi);
        bf.setPatienEncounter(bi.getBill().getPatientEncounter());
        bf.setPatient(bi.getBill().getPatient());
        bf.setFeeValue(bi.getNetValue());
        bf.setFeeGrossValue(bi.getGrossValue());
        bf.setSettleValue(bi.getNetValue());
        bf.setCreatedAt(new Date());
        bf.setDepartment(getSessionController().getDepartment());
        bf.setInstitution(getSessionController().getInstitution());
        bf.setBill(bi.getBill());

        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }
    }

    private void updateReturnTotal() {
        double tot = 0;
        double discount = 0;
        double netTotal = 0;
        for (BillItem b : getReturnBill().getBillItems()) {
            tot += b.getGrossValue();
            discount += b.getDiscount();
            netTotal += b.getNetValue();
        }

        getReturnBill().setTotal(tot);
        getReturnBill().setDiscount(discount);
        getReturnBill().setNetTotal(netTotal);

        getBillFacade().edit(getReturnBill());
    }

//    private void updateRemainingQty(PharmacyItemData pharmacyItemData) {
//        String sql = "Select p from PharmaceuticalBillItem p where p.billItem.id=" + pharmacyItemData.getPoBillItem().getId();
//        PharmaceuticalBillItem po = getPharmaceuticalBillItemFacade().findFirstByJpql(sql);
//        po.setRemainingQty(po.getRemainingQty() + pharmacyItemData.getPharmaceuticalBillItem().getQty());
//
//        //System.err.println("Added Remaini Qty " + pharmacyItemData.getPharmaceuticalBillItem().getQty());
//        //System.err.println("Final Remaini Qty " + po.getRemainingQty());
//        getPharmaceuticalBillItemFacade().edit(po);
//
//    }
    @EJB
    CashTransactionBean cashTransactionBean;

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    /**
     * Validates that the absolute value of total payments equals the absolute
     * value of the refund bill's netTotal. This is called during settlement to
     * ensure that the payment amounts match the refund amount.
     *
     * @return true if validation fails (there's an error), false if validation
     * passes
     */
    private boolean validatePaymentRefundMatch() {
        if (getReturnBill() == null) {
            JsfUtil.addErrorMessage("Return bill is not initialized");
            return true;
        }

        // Calculate total of payments that will be created
        double totalPayments = 0.0;

        if (returnPaymentMethod == PaymentMethod.MultiplePaymentMethods) {
            // For multiple payment methods, calculate the sum from payment method data
            if (getPaymentMethodData() != null
                    && getPaymentMethodData().getPaymentMethodMultiple() != null
                    && getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() != null) {
                // Calculate the sum of all payment components
                for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                    totalPayments += multiplePaymentComponentValue(cd);
                }
            }
        } else {
            // For single payment methods, use the bill's netTotal as the payment amount
            // (This is how paymentService.createPayment works for single payment methods)
            totalPayments = getReturnBill().getNetTotal();
        }

        // Get absolute values for comparison (both should be positive for comparison)
        double absRefundTotal = Math.abs(getReturnBill().getNetTotal());
        double absPaymentTotal = Math.abs(totalPayments);

        // Allow a tolerance of 1.0 for rounding differences
        double difference = Math.abs(absRefundTotal - absPaymentTotal);

        if (difference > 1.0) {
            JsfUtil.addErrorMessage("Payment total does not match refund total. "
                    + "Refund Amount: " + String.format("%.2f", absRefundTotal)
                    + ", Payment Total: " + String.format("%.2f", absPaymentTotal)
                    + ". Please edit payment methods to match the refund amount.");
            return true;
        }

        return false;
    }

    public void settle() {
        if (getReturnBill().getTotal() == 0) {
            JsfUtil.addErrorMessage("Total is Zero cant' return");
            return;
        }

        if (getReturnBillcomment() == null || getReturnBillcomment().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return;
        }

        // Check if any items with quantity > 0 have refundsAllowed set to false
        for (BillItem bi : getBillItems()) {
            if (bi.getQty() > 0 && bi.getItem() != null && !bi.getItem().isRefundsAllowed()) {
                JsfUtil.addErrorMessage("Item '" + bi.getItem().getName() + "' is not allowed to be returned. Refunds are not permitted for this item.");
                return;
            }
        }

        if (returnPaymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method to return");
            return;
        }
//        if (returnPaymentMethod == PaymentMethod.MultiplePaymentMethods) {
//            JsfUtil.addErrorMessage("Multiple Payment Methods NOT allowed. Please select another payment method to return");
//            return;
//        }

        // Validate that payment total matches refund total
        if (validatePaymentRefundMatch()) {
            return; // Validation failed, error message already displayed
        }

        savePreReturnBill();
        savePreReturnBillComponents();

        getBill().getReturnPreBills().add(getReturnBill());

        // Update original bills (both PRE and SALE)
        updateOriginalBillsForReturn();

        getBillFacade().edit(getBill());

        finalReturnBill = saveSaleFinalReturnBill();
        saveSaleComponent(finalReturnBill);
        applyRefundSignToPaymentData();
        List<Payment> payments = paymentService.createPayment(finalReturnBill, getPaymentMethodData());
        // Update patient deposit balances and create history records
        paymentService.updateBalances(payments);

        getReturnBill().setReferenceBill(getBill());
        getReturnBill().getReturnCashBills().add(finalReturnBill);
        getReturnBill().setCreditCompany(getFinalReturnBill().getCreditCompany());
        getBillFacade().edit(getReturnBill());
//        getBillFacade().edit(getFinalReturnBill());

        finalReturnBill = billService.reloadBill(finalReturnBill);
        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Returned");
        returnBillcomment = null;

    }

    /**
     * Updates the original bill's financial tracking fields (refundAmount,
     * paidAmount, balance). This method validates bill types and updates both
     * the PHARMACY_RETAIL_SALE_PRE bill and its related PHARMACY_RETAIL_SALE
     * bill.
     *
     * @throws RuntimeException if bill type validation fails
     */
    private void updateOriginalBillsForReturn() {
        Bill originalBill = null;
        Bill salePreBill;
        Bill saleBill;
        if (getBill() == null) {
            throw new RuntimeException("Original bill is null. Cannot update financial tracking fields.");
        } else {
            originalBill = getBill();
        }

        if (null == originalBill.getBillTypeAtomic()) {
            JsfUtil.addErrorMessage("Data Flow Error");
            return;
        } else {
            switch (originalBill.getBillTypeAtomic()) {
                case PHARMACY_RETAIL_SALE:
                    saleBill = originalBill;
                    salePreBill = originalBill.getReferenceBill();
                    break;
                case PHARMACY_RETAIL_SALE_PRE:
                    saleBill = originalBill.getReferenceBill();
                    salePreBill = originalBill;
                    break;
                case PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER:
                    saleBill = originalBill;
                    salePreBill = originalBill.getReferenceBill();
                    break;
                case PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER:
                    saleBill = originalBill.getReferenceBill();
                    salePreBill = originalBill;
                    break;
                default:
                    JsfUtil.addErrorMessage("Data Flow Error");
                    return;
            }
        }

        // Calculate the absolute value of the return amount
        double returnAmount = Math.abs(getReturnBill().getNetTotal());

        updateBillFinancialFields(salePreBill, returnAmount);
        updateBillFinancialFields(saleBill, returnAmount);
    }

    /**
     * Helper method to update a bill's financial tracking fields. Updates
     * refundAmount, paidAmount, and balance (if applicable).
     *
     * @param billToUpdate The bill to update
     * @param returnAmount The absolute value of the return amount
     */
    private void updateBillFinancialFields(Bill billToUpdate, double returnAmount) {
        // Update refundAmount - add the return amount
        double currentRefundAmount = billToUpdate.getRefundAmount();
        billToUpdate.setRefundAmount(currentRefundAmount + returnAmount);

        // Update paidAmount - deduct the return amount only when a payment exists
        double currentPaidAmount = billToUpdate.getPaidAmount();
        if (currentPaidAmount > 0) {
            double updatedPaidAmount = currentPaidAmount - returnAmount;
            billToUpdate.setPaidAmount(Math.max(0d, updatedPaidAmount));
        }

        // Update balance for credit bills (only if balance > 0)
        double currentBalance = billToUpdate.getBalance();
        if (currentBalance > 0) {
            billToUpdate.setBalance(Math.max(0d, currentBalance - returnAmount));
        }

        // Save the updated bill
        getBillFacade().edit(billToUpdate);
    }

    private void applyRefundSignToPaymentData() {
        PaymentMethodData data = getPaymentMethodData();
        if (data == null) {
            return;
        }

        PaymentMethod paymentMethod = null;
        if (getFinalReturnBill() != null) {
            paymentMethod = getFinalReturnBill().getPaymentMethod();
        }
        if (paymentMethod == null) {
            paymentMethod = getReturnPaymentMethod();
        }

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            ComponentDetail multiple = data.getPaymentMethodMultiple();
            if (multiple != null && multiple.getMultiplePaymentMethodComponentDetails() != null) {
                for (ComponentDetail component : multiple.getMultiplePaymentMethodComponentDetails()) {
                    if (component == null) {
                        continue;
                    }
                    negateComponentTotal(component);
                    negatePaymentMethodData(component.getPaymentMethodData());
                }
            }
        } else {
            negatePaymentMethodData(data);
        }
    }

    private void negatePaymentMethodData(PaymentMethodData paymentMethodData) {
        if (paymentMethodData == null) {
            return;
        }

        negateComponentTotal(paymentMethodData.getCash());
        negateComponentTotal(paymentMethodData.getCreditCard());
        negateComponentTotal(paymentMethodData.getCheque());
        negateComponentTotal(paymentMethodData.getSlip());
        negateComponentTotal(paymentMethodData.getEwallet());
        negateComponentTotal(paymentMethodData.getPatient_deposit());
        negateComponentTotal(paymentMethodData.getCredit());
        negateComponentTotal(paymentMethodData.getStaffCredit());
        negateComponentTotal(paymentMethodData.getStaffWelfare());
        negateComponentTotal(paymentMethodData.getOnlineSettlement());
        negateComponentTotal(paymentMethodData.getIou());
    }

    private void negateComponentTotal(ComponentDetail componentDetail) {
        if (componentDetail == null) {
            return;
        }

        componentDetail.setTotalValue(0 - Math.abs(componentDetail.getTotalValue()));
    }

    public void fillReturningQty() {
        if (billItems == null || billItems.isEmpty()) {
            JsfUtil.addErrorMessage("Please add bill items");
            return;
        }
        for (BillItem bi : billItems) {
            bi.setQty(bi.getPharmaceuticalBillItem().getQty());
            onEdit(bi);
        }
    }

    /**
     * Initializes refund payment data from the original bill's payment records.
     * This method populates payment method details (card numbers, reference
     * numbers, etc.) based on how the original sale was paid.
     *
     * @param originalPayments List of Payment objects from the original sale
     * bill
     */
    private void initializeRefundPaymentFromOriginalPayments(List<Payment> originalPayments) {
        if (originalPayments == null || originalPayments.isEmpty()) {
            return;
        }

        // If single payment method
        if (originalPayments.size() == 1) {
            Payment originalPayment = originalPayments.get(0);
            returnPaymentMethod = originalPayment.getPaymentMethod();

            // Initialize paymentMethodData based on payment method (using absolute values for UI display)
            switch (originalPayment.getPaymentMethod()) {
                case Cash:
                    getPaymentMethodData().getCash().setTotalValue(Math.abs(getReturnBill().getNetTotal()));
                    break;
                case Card:
                    getPaymentMethodData().getCreditCard().setInstitution(originalPayment.getBank());
                    getPaymentMethodData().getCreditCard().setNo(originalPayment.getCreditCardRefNo());
                    getPaymentMethodData().getCreditCard().setTotalValue(Math.abs(getReturnBill().getNetTotal()));
                    break;
                case Cheque:
                    getPaymentMethodData().getCheque().setDate(originalPayment.getChequeDate());
                    getPaymentMethodData().getCheque().setNo(originalPayment.getChequeRefNo());
                    getPaymentMethodData().getCheque().setTotalValue(Math.abs(getReturnBill().getNetTotal()));
                    break;
                case Slip:
                    getPaymentMethodData().getSlip().setTotalValue(Math.abs(getReturnBill().getNetTotal()));
                    break;
                case ewallet:
                    getPaymentMethodData().getEwallet().setInstitution(originalPayment.getCreditCompany());
                    getPaymentMethodData().getEwallet().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getEwallet().setReferralNo(originalPayment.getPolicyNo());
                    getPaymentMethodData().getEwallet().setTotalValue(Math.abs(getReturnBill().getNetTotal()));
                    getPaymentMethodData().getEwallet().setComment(originalPayment.getComments());
                    break;
                case PatientDeposit:
                    getPaymentMethodData().getPatient_deposit().setTotalValue(Math.abs(getReturnBill().getNetTotal()));
                    getPaymentMethodData().getPatient_deposit().setPatient(getReturnBill().getPatient());
                    getPaymentMethodData().getPatient_deposit().setComment(originalPayment.getComments());
                    // Load and set the PatientDeposit object for displaying balance
                    if (getReturnBill().getPatient() != null) {
                        com.divudi.core.entity.PatientDeposit pd = patientDepositController.getDepositOfThePatient(
                                getReturnBill().getPatient(),
                                sessionController.getDepartment()
                        );
                        if (pd != null && pd.getId() != null) {
                            getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                            getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                        }
                    }
                    break;
                case Credit:
                    getPaymentMethodData().getCredit().setInstitution(originalPayment.getCreditCompany());
                    getPaymentMethodData().getCredit().setReferenceNo(originalPayment.getReferenceNo());
                    getPaymentMethodData().getCredit().setReferralNo(originalPayment.getPolicyNo());
                    getPaymentMethodData().getCredit().setTotalValue(Math.abs(getReturnBill().getNetTotal()));
                    getPaymentMethodData().getCredit().setComment(originalPayment.getComments());
                    break;
                case Staff:
                    Staff staffForCredit = originalPayment.getToStaff();
                    if (staffForCredit == null && getBill() != null) {
                        staffForCredit = getBill().getToStaff();
                    }
                    getPaymentMethodData().getStaffCredit().setToStaff(staffForCredit);
                    getPaymentMethodData().getStaffCredit().setTotalValue(Math.abs(getReturnBill().getNetTotal()));
                    getPaymentMethodData().getStaffCredit().setComment(originalPayment.getComments());
                    break;
                case Staff_Welfare:
                    Staff staffForWelfare = originalPayment.getToStaff();
                    if (staffForWelfare == null && getBill() != null) {
                        staffForWelfare = getBill().getToStaff();
                    }
                    getPaymentMethodData().getStaffWelfare().setToStaff(staffForWelfare);
                    getPaymentMethodData().getStaffWelfare().setTotalValue(Math.abs(getReturnBill().getNetTotal()));
                    getPaymentMethodData().getStaffWelfare().setComment(originalPayment.getComments());
                    break;
                default:
                    // For other payment methods, just set the total value
                    break;
            }
        } // If multiple payment methods
        else {
            returnPaymentMethod = PaymentMethod.MultiplePaymentMethods;

            // Clear any existing multiple payment details
            getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().clear();

            for (Payment originalPayment : originalPayments) {
                ComponentDetail cd = new ComponentDetail();
                cd.setPaymentMethod(originalPayment.getPaymentMethod());

                // Set payment details based on method - use absolute value for UI display
                double refundAmount = Math.abs(originalPayment.getPaidValue());

                switch (originalPayment.getPaymentMethod()) {
                    case Cash:
                        cd.getPaymentMethodData().getCash().setTotalValue(refundAmount);
                        break;
                    case Card:
                        cd.getPaymentMethodData().getCreditCard().setInstitution(originalPayment.getBank());
                        cd.getPaymentMethodData().getCreditCard().setNo(originalPayment.getCreditCardRefNo());
                        cd.getPaymentMethodData().getCreditCard().setTotalValue(refundAmount);
                        break;
                    case Cheque:
                        cd.getPaymentMethodData().getCheque().setDate(originalPayment.getChequeDate());
                        cd.getPaymentMethodData().getCheque().setNo(originalPayment.getChequeRefNo());
                        cd.getPaymentMethodData().getCheque().setTotalValue(refundAmount);
                        break;
                    case Slip:
                        cd.getPaymentMethodData().getSlip().setTotalValue(refundAmount);
                        break;
                    case ewallet:
                        cd.getPaymentMethodData().getEwallet().setInstitution(originalPayment.getCreditCompany());
                        cd.getPaymentMethodData().getEwallet().setReferenceNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getEwallet().setReferralNo(originalPayment.getPolicyNo());
                        cd.getPaymentMethodData().getEwallet().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getEwallet().setComment(originalPayment.getComments());
                        break;
                    case PatientDeposit:
                        cd.getPaymentMethodData().getPatient_deposit().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getPatient_deposit().setPatient(getReturnBill().getPatient());
                        cd.getPaymentMethodData().getPatient_deposit().setComment(originalPayment.getComments());
                        // Load and set the PatientDeposit object for displaying balance
                        if (getReturnBill().getPatient() != null) {
                            com.divudi.core.entity.PatientDeposit pd = patientDepositController.getDepositOfThePatient(
                                    getReturnBill().getPatient(),
                                    sessionController.getDepartment()
                            );
                            if (pd != null && pd.getId() != null) {
                                cd.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                                cd.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                            }
                        }
                        break;
                    case Credit:
                        cd.getPaymentMethodData().getCredit().setInstitution(originalPayment.getCreditCompany());
                        cd.getPaymentMethodData().getCredit().setReferenceNo(originalPayment.getReferenceNo());
                        cd.getPaymentMethodData().getCredit().setReferralNo(originalPayment.getPolicyNo());
                        cd.getPaymentMethodData().getCredit().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getCredit().setComment(originalPayment.getComments());
                        break;
                    case Staff:
                        Staff staffForCredit = originalPayment.getToStaff();
                        if (staffForCredit == null && getBill() != null) {
                            staffForCredit = getBill().getToStaff();
                        }
                        cd.getPaymentMethodData().getStaffCredit().setToStaff(staffForCredit);
                        cd.getPaymentMethodData().getStaffCredit().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getStaffCredit().setComment(originalPayment.getComments());
                        break;
                    case Staff_Welfare:
                        Staff staffForWelfare = originalPayment.getToStaff();
                        if (staffForWelfare == null && getBill() != null) {
                            staffForWelfare = getBill().getToStaff();
                        }
                        cd.getPaymentMethodData().getStaffWelfare().setToStaff(staffForWelfare);
                        cd.getPaymentMethodData().getStaffWelfare().setTotalValue(refundAmount);
                        cd.getPaymentMethodData().getStaffWelfare().setComment(originalPayment.getComments());
                        break;
                    default:
                        // For other payment methods
                        break;
                }

                getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().add(cd);
            }
        }
    }

    private void calTotal() {
        double grossTotal = 0.0;
        double discount = 0;

        for (BillItem p : getBillItems()) {
            grossTotal += p.getNetRate() * p.getQty();
            discount += p.getDiscountRate() * p.getQty();

        }
        getReturnBill().setDiscount(discount);
        getReturnBill().setTotal(grossTotal);
        getReturnBill().setNetTotal(grossTotal - discount);

        //  return grossTotal;
    }

    public void generateBillComponent() {
        for (PharmaceuticalBillItem i : getPharmaceuticalBillItemFacade().getPharmaceuticalBillItems(getBill())) {
            BillItem bi = new BillItem();
            bi.setBill(getReturnBill());
            bi.setReferenceBill(getBill());
            bi.setReferanceBillItem(i.getBillItem());
            bi.copy(i.getBillItem());
            bi.setQty(0.0);

            PharmaceuticalBillItem tmp = new PharmaceuticalBillItem();
            tmp.setBillItem(bi);
            tmp.copy(i);

            List<BillTypeAtomic> btas = new ArrayList<>();
            btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
            btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY);

            double rFund = getPharmacyRecieveBean().getTotalQty(i.getBillItem(), btas);

            //  double rCacnelled = getPharmacyRecieveBean().getTotalQty(i.getBillItem(), BillType.PharmacySale, new CancelledBill());
            //System.err.println("Refund " + rFund);
//                //System.err.println("Cancelled "+rCacnelled);
//                //System.err.println("Net "+(rBilled-rCacnelled));
            tmp.setQty(Math.abs(i.getQty()) - Math.abs(rFund));

            bi.setPharmaceuticalBillItem(tmp);

            getBillItems().add(bi);

        }

    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public PharmaceuticalItemController getPharmaceuticalItemController() {
        return pharmaceuticalItemController;
    }

    public void setPharmaceuticalItemController(PharmaceuticalItemController pharmaceuticalItemController) {
        this.pharmaceuticalItemController = pharmaceuticalItemController;
    }

    public PharmacyController getPharmacyController() {
        return pharmacyController;
    }

    public void setPharmacyController(PharmacyController pharmacyController) {
        this.pharmacyController = pharmacyController;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public PharmacyCalculation getPharmacyRecieveBean() {
        return pharmacyRecieveBean;
    }

    public void setPharmacyRecieveBean(PharmacyCalculation pharmacyRecieveBean) {
        this.pharmacyRecieveBean = pharmacyRecieveBean;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public StaffService getStaffBean() {
        return staffBean;
    }

    public void setStaffBean(StaffService staffBean) {
        this.staffBean = staffBean;
    }

    public PaymentMethod getReturnPaymentMethod() {
        return returnPaymentMethod;
    }

    public void setReturnPaymentMethod(PaymentMethod returnPaymentMethod) {
        this.returnPaymentMethod = returnPaymentMethod;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public Bill getFinalReturnBill() {
        return finalReturnBill;
    }

    public void setFinalReturnBill(Bill finalReturnBill) {
        this.finalReturnBill = finalReturnBill;
    }

    /**
     * Retrieves the original bill's payments for display on the return page.
     * This allows users to see how the original bill was paid before processing
     * the refund.
     *
     * @return List of Payment objects from the original sale bill, or empty
     * list if not available
     */
    public List<Payment> getOriginalBillPayments() {
        if (bill != null && bill.getPayments() != null && !bill.getPayments().isEmpty()) {
            return bill.getPayments();
        }
        return new ArrayList<>();
    }

    /**
     * Calculates the remaining amount for multiple payment method total.
     * Required by ControllerWithMultiplePayments interface.
     *
     * @return The remaining amount (bill total - sum of all payment method
     * values)
     */
    private double componentTotal(ComponentDetail detail) {
        if (detail == null) {
            return 0.0;
        }
        return detail.getTotalValue();
    }

    private double multiplePaymentComponentValue(ComponentDetail componentDetail) {
        if (componentDetail == null) {
            return 0.0;
        }

        PaymentMethodData data = componentDetail.getPaymentMethodData();
        if (data == null) {
            return 0.0;
        }

        double total = 0.0;
        total += componentTotal(data.getCash());
        total += componentTotal(data.getCreditCard());
        total += componentTotal(data.getCheque());
        total += componentTotal(data.getSlip());
        total += componentTotal(data.getEwallet());
        total += componentTotal(data.getPatient_deposit());
        total += componentTotal(data.getCredit());
        total += componentTotal(data.getStaffCredit());
        total += componentTotal(data.getStaffWelfare());
        total += componentTotal(data.getOnlineSettlement());
        total += componentTotal(data.getIou());

        if (total == 0.0) {
            total = Math.abs(componentDetail.getTotalValue());
        }

        return total;
    }

    @Override
    public double calculatRemainForMultiplePaymentTotal() {
        double total = getReturnBill().getNetTotal();
        double multiplePaymentMethodTotalValue = 0;

        if (returnPaymentMethod == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : getPaymentMethodData().getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                multiplePaymentMethodTotalValue += multiplePaymentComponentValue(cd);
            }
        }

        return total - multiplePaymentMethodTotalValue;
    }

    /**
     * Automatically receives the remaining amount for the last payment method
     * in multiple payments. Required by ControllerWithMultiplePayments
     * interface.
     */
    @Override
    public void recieveRemainAmountAutomatically() {
        double remainAmount = calculatRemainForMultiplePaymentTotal();

        if (returnPaymentMethod == PaymentMethod.MultiplePaymentMethods) {
            int arrSize = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
            if (arrSize == 0) {
                return;
            }

            ComponentDetail pm = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(arrSize - 1);

            if (pm.getPaymentMethod() == PaymentMethod.Cash) {
                pm.getPaymentMethodData().getCash().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Card) {
                pm.getPaymentMethodData().getCreditCard().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Cheque) {
                pm.getPaymentMethodData().getCheque().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.Slip) {
                pm.getPaymentMethodData().getSlip().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.ewallet) {
                pm.getPaymentMethodData().getEwallet().setTotalValue(remainAmount);
            } else if (pm.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                if (getReturnBill().getPatient() == null || getReturnBill().getPatient().getId() == null) {
                    pm.getPaymentMethodData().getPatient_deposit().setTotalValue(0.0);
                    return;
                }
                pm.getPaymentMethodData().getPatient_deposit().setPatient(getReturnBill().getPatient());
                com.divudi.core.entity.PatientDeposit pd = patientDepositController.getDepositOfThePatient(
                        getReturnBill().getPatient(),
                        sessionController.getDepartment()
                );
                if (pd != null && pd.getId() != null) {
                    pm.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
                    pm.getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);
                    double availableBalance = pd.getBalance();
                    if (availableBalance >= remainAmount) {
                        pm.getPaymentMethodData().getPatient_deposit().setTotalValue(remainAmount);
                    } else {
                        pm.getPaymentMethodData().getPatient_deposit().setTotalValue(availableBalance);
                    }
                } else {
                    pm.getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(false);
                    pm.getPaymentMethodData().getPatient_deposit().setTotalValue(0.0);
                }
            } else if (pm.getPaymentMethod() == PaymentMethod.Credit) {
                pm.getPaymentMethodData().getCredit().setTotalValue(remainAmount);
            }
        }
    }

    /**
     * Checks if the given ComponentDetail is the last payment entry in the
     * multiple payment methods list. This is used to determine which payment
     * fields should be editable in the UI.
     *
     * @param cd The ComponentDetail to check
     * @return true if cd is the last entry, false otherwise
     */
    public boolean isLastPaymentEntry(ComponentDetail cd) {
        if (paymentMethodData == null
                || paymentMethodData.getPaymentMethodMultiple() == null
                || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails() == null
                || paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().isEmpty()) {
            return false;
        }

        int size = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().size();
        ComponentDetail lastEntry = paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails().get(size - 1);
        return cd == lastEntry;
    }

}
