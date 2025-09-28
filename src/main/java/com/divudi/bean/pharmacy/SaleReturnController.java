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
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.PharmacyCalculation;
import com.divudi.service.StaffService;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillFeePayment;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillFeePaymentFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
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
public class SaleReturnController implements Serializable {

    private Bill bill;
    private Bill returnBill;
    private boolean printPreview;
    private String returnBillcomment;
    private PaymentMethod returnPaymentMethod;
    ////////

    private List<BillItem> billItems;
    ///////
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @Inject
    private PharmaceuticalItemController pharmaceuticalItemController;
    @Inject
    private PharmacyController pharmacyController;
    @Inject
    DrawerController drawerController;
    @Inject
    private SessionController sessionController;
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
    BillFeePaymentFacade billFeePaymentFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    StaffService staffBean;

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
        makeNull();
        this.bill = bill;
        generateBillComponent();
        returnPaymentMethod = bill.getPaymentMethod();
    }

    public Bill getReturnBill() {
        if (returnBill == null) {
            returnBill = new RefundBill();
            //     returnBill.setBillType(BillType.PharmacySale);

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
            tmp.setQty(remainingQty);
            JsfUtil.addErrorMessage("You cant return over than the remaining quanty to return. The returning qtantity was set to Remaining Quantity.");
        }

        calTotal();
        getPharmacyController().setPharmacyItem(tmp.getPharmaceuticalBillItem().getBillItem().getItem());
    }

    public void makeNull() {
        bill = null;
        returnBill = null;
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
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL);
        } else {
            // Use existing method for backward compatibility
            deptId = getBillNumberBean().departmentBillNumberGeneratorYearly(getSessionController().getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL);
        }

        // Handle Institution ID generation
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund Pre Bill - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    getSessionController().getDepartment(), getSessionController().getInstitution(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund Pre Bill - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = getBillNumberBean().institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    getSessionController().getInstitution(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS_PREBILL);
        } else {
            // Use existing method for backward compatibility
            insId = deptId;
        }

        getReturnBill().setInsId(insId);
        getReturnBill().setDeptId(deptId);

        if (getReturnBill().getId() == null) {
            getBillFacade().create(getReturnBill());
        }

    }

    private Bill saveSaleReturnBill() {
        RefundBill refundBill = new RefundBill();
        refundBill.copy(getReturnBill());
        refundBill.setPaymentMethod(returnPaymentMethod);
        refundBill.setBillType(BillType.PharmacySale);
        refundBill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);

        refundBill.setReferenceBill(getReturnBill());
        refundBill.setTotal(getReturnBill().getTotal());
        refundBill.setNetTotal(getReturnBill().getNetTotal());
        refundBill.setDiscount(getReturnBill().getDiscount());

        refundBill.setCreater(getSessionController().getLoggedUser());
        refundBill.setCreatedAt(Calendar.getInstance().getTime());

        refundBill.setInstitution(getSessionController().getInstitution());
        refundBill.setDepartment(getSessionController().getDepartment());
        refundBill.setComments(returnBillcomment);

        // Handle Department ID generation
        String deptId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund - Prefix + Institution Code + Year + Yearly Number", false)) {
            deptId = billNumberBean.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
        } else {
            // Use existing method for backward compatibility
            deptId = billNumberBean.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
        }

        // Handle Institution ID generation
        String insId;
        if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund - Prefix + Department Code + Institution Code + Year + Yearly Number", false)) {
            insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(
                    sessionController.getDepartment(), sessionController.getInstitution(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
        } else if (configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Pharmacy Sale Refund - Prefix + Institution Code + Year + Yearly Number", false)) {
            insId = billNumberBean.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(
                    sessionController.getInstitution(), BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
        } else {
            // Use existing method for backward compatibility
            insId = deptId;
        }

        refundBill.setInsId(insId);
        refundBill.setDeptId(deptId);
        refundBill.setBillTime(new Date());

        if (refundBill.getId() == null) {
            getBillFacade().create(refundBill);
        }

        updatePreReturnBill(refundBill);

        return refundBill;

    }

    public void updatePreReturnBill(Bill ref) {
        getReturnBill().setReferenceBill(ref);
        getBillFacade().edit(getReturnBill());
    }

    private void savePreComponent() {
        for (BillItem i : getBillItems()) {
            i.getPharmaceuticalBillItem().setQty(i.getQty());
            if (i.getPharmaceuticalBillItem().getQty() == 0.0) {
                continue;
            }

            i.setCreatedAt(Calendar.getInstance().getTime());
            i.setCreater(getSessionController().getLoggedUser());
            //   i.getBillItem().setQty(i.getPharmaceuticalBillItem().getQty());
            double grossValue = i.getRate() * i.getQty();
            double netValue = i.getNetRate() * i.getQty();
            double discountValue = i.getDiscountRate() * i.getQty();
            i.setGrossValue(0 - grossValue);
            i.setNetValue(0 - netValue);
            i.setDiscount(discountValue);

            PharmaceuticalBillItem tmpPh = i.getPharmaceuticalBillItem();
            i.setPharmaceuticalBillItem(null);
            getBillItemFacade().create(i);

            tmpPh.setBillItem(i);
            getPharmaceuticalBillItemFacade().create(tmpPh);

            i.setPharmaceuticalBillItem(tmpPh);
            getBillItemFacade().edit(i);

            //   getPharmaceuticalBillItemFacade().edit(i.getPharmaceuticalBillItem());
            getPharmacyBean().addToStock(tmpPh.getStock(), Math.abs(tmpPh.getQty()), tmpPh, getSessionController().getDepartment());

            //   i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem().setRemainingQty(i.getRemainingQty() - i.getQty());
            //   getPharmaceuticalBillItemFacade().edit(i.getBillItem().getTmpReferenceBillItem().getPharmaceuticalBillItem());
            //      updateRemainingQty(i);
            getReturnBill().getBillItems().add(i);
        }

        updateReturnTotal();

    }

    private void saveSaleComponent(Bill bill) {
        for (BillItem i : getReturnBill().getBillItems()) {
            BillItem b = new BillItem();
            b.copy(i);
            b.setBill(bill);
            b.setCreatedAt(Calendar.getInstance().getTime());
            b.setCreater(getSessionController().getLoggedUser());

            getBillItemFacade().create(b);

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(i.getPharmaceuticalBillItem());
            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().create(ph);

            b.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(b);

            bill.getBillItems().add(b);
        }

        getBillFacade().edit(bill);

    }

    private void saveSaleComponent(Bill bill, Payment p) {
        for (BillItem i : getReturnBill().getBillItems()) {
            BillItem b = new BillItem();
            b.copy(i);
            b.setBill(bill);
            b.setCreatedAt(Calendar.getInstance().getTime());
            b.setCreater(getSessionController().getLoggedUser());

            getBillItemFacade().create(b);

            PharmaceuticalBillItem ph = new PharmaceuticalBillItem();
            ph.copy(i.getPharmaceuticalBillItem());
            ph.setBillItem(b);
            getPharmaceuticalBillItemFacade().create(ph);

            b.setPharmaceuticalBillItem(ph);
            getBillItemFacade().edit(b);
            //save bill Fees for refund billItems
            saveBillFee(b, p);

            bill.getBillItems().add(b);
        }

        getBillFacade().edit(bill);

    }

    public void saveBillFee(BillItem bi, Payment p) {
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
        createBillFeePaymentAndPayment(bf, p);
    }

    public void createBillFeePaymentAndPayment(BillFee bf, Payment p) {
        BillFeePayment bfp = new BillFeePayment();
        bfp.setBillFee(bf);
        bfp.setAmount(bf.getSettleValue());
        bfp.setInstitution(getSessionController().getInstitution());
        bfp.setDepartment(getSessionController().getDepartment());
        bfp.setCreater(getSessionController().getLoggedUser());
        bfp.setCreatedAt(new Date());
        bfp.setPayment(p);
        getBillFeePaymentFacade().create(bfp);
    }

    public Payment createPayment(Bill bill, PaymentMethod pm) {
        Payment p = new Payment();
        p.setBill(bill);
        setPaymentMethodData(p, pm);
        return p;
    }

    public void setPaymentMethodData(Payment p, PaymentMethod pm) {

        p.setInstitution(getSessionController().getInstitution());
        p.setDepartment(getSessionController().getDepartment());
        p.setCreatedAt(new Date());
        p.setCreater(getSessionController().getLoggedUser());
        p.setPaymentMethod(pm);

        p.setPaidValue(p.getBill().getNetTotal());

        if (p.getId() == null) {
            getPaymentFacade().create(p);
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
        if (returnPaymentMethod == PaymentMethod.MultiplePaymentMethods) {
            JsfUtil.addErrorMessage("Multiple Payment Methods NOT allowed. Please select another payment method to return");
            return;
        }

        savePreReturnBill();
        savePreComponent();
//        getReturnBill().setTotal(getReturnBill().getNetTotal()+getReturnBill().getDiscount());

        getBill().getReturnPreBills().add(getReturnBill());
        getBillFacade().edit(getBill());

        Bill b = saveSaleReturnBill();
//        saveSaleComponent(b);
        //saveSaleComponent and billfees and billFeePayment
        Payment p = createPayment(b, getReturnPaymentMethod());
        drawerController.updateDrawerForOuts(p);
        saveSaleComponent(b, p);
        getReturnBill().setReferenceBill(getBill());
        getReturnBill().getReturnCashBills().add(b);
        getBillFacade().edit(getReturnBill());

        printPreview = true;
        JsfUtil.addSuccessMessage("Successfully Returned");
        returnBillcomment = null;
        if (getBill().getPaymentMethod() == PaymentMethod.Credit) {
            //   ////// // System.out.println("getBill().getPaymentMethod() = " + getBill().getPaymentMethod());
            //   ////// // System.out.println("getBill().getToStaff() = " + getBill().getToStaff());
            if (getBill().getToStaff() != null) {
                //   ////// // System.out.println("getBill().getNetTotal() = " + getBill().getNetTotal());
                getStaffBean().updateStaffCredit(getBill().getToStaff(), 0 - getBill().getNetTotal());
                JsfUtil.addSuccessMessage("Staff Credit Updated");
                getReturnBill().setFromStaff(getBill().getToStaff());
                getBillFacade().edit(getReturnBill());
            }
        }
        if (getBill().getPaymentMethod() == PaymentMethod.Staff_Welfare) {
            if (getBill().getToStaff() != null) {
                getStaffBean().updateStaffWelfare(getBill().getToStaff(), getReturnBill().getNetTotal());
                JsfUtil.addSuccessMessage("Staff Welfare Updated");
                getReturnBill().setFromStaff(getBill().getToStaff());
                getBillFacade().edit(getReturnBill());
            }
        }

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

            double rFund = getPharmacyRecieveBean().getTotalQty(i.getBillItem(), BillType.PharmacyPre, new RefundBill());
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

    public BillFeePaymentFacade getBillFeePaymentFacade() {
        return billFeePaymentFacade;
    }

    public void setBillFeePaymentFacade(BillFeePaymentFacade billFeePaymentFacade) {
        this.billFeePaymentFacade = billFeePaymentFacade;
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

}
