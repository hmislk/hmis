package com.divudi.service.inward;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientDepositFacade;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.entity.Payment;
import java.util.List;
import com.divudi.core.facade.BilledBillFacade;
import com.divudi.service.PaymentService;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Saves an inward payment as one transaction (#25610): the payment bill, its
 * bill item, the Payment rows and the drawer update are all saved, or none are.
 * Before this, the controller saved the bill first and the Payment afterwards
 * in separate transactions, so a failure left a live payment bill with no
 * Payment and no drawer entry.
 */
@Stateless
public class InwardPaymentService {

    @EJB
    private BilledBillFacade billedBillFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private PatientDepositFacade patientDepositFacade;
    @EJB
    private PaymentService paymentService;

    /**
     * @param bill the prepared payment bill (numbers, dates, type already set);
     * must be new (no id yet)
     */
    public Bill savePaymentBillWithPayments(BilledBill bill, PaymentMethodData paymentMethodData,
            Institution institution, Department department, WebUser webUser) {
        return savePaymentBillWithPayments(bill, paymentMethodData, institution, department, webUser, true);
    }

    /**
     * @param updateDrawer true to also write the drawer (deposits, inward
     * payments); false to create the Payment rows only (post-final payments)
     */
    public Bill savePaymentBillWithPayments(BilledBill bill, PaymentMethodData paymentMethodData,
            Institution institution, Department department, WebUser webUser, boolean updateDrawer) {
        if (bill == null) {
            throw new InwardPaymentException("No payment bill to save.");
        }
        if (bill.getId() != null) {
            throw new InwardPaymentException("This payment bill is already saved.");
        }
        // Flush so the bill has its id before the Payment rows point to it.
        billedBillFacade.createAndFlush(bill);

        BillItem item = new BillItem();
        item.setBill(bill);
        item.setGrossValue(bill.getTotal());
        item.setNetValue(bill.getTotal());
        item.setCreatedAt(new Date());
        item.setCreater(webUser);
        billItemFacade.create(item);

        List<Payment> payments = updateDrawer
                ? paymentService.createPaymentAndUpdateDrawer(
                        bill, bill.getPaymentMethod(), paymentMethodData, institution, department, webUser)
                : paymentService.createPayment(
                        bill, bill.getPaymentMethod(), paymentMethodData, institution, department, webUser);
        if (payments == null || payments.isEmpty()) {
            // Rolls back the bill and bill item too: never save a payment bill
            // with no Payment behind it.
            throw new InwardPaymentException("No payment was recorded. Please check the payment details.");
        }
        return bill;
    }

    /**
     * Saves an inward deposit/payment refund as one transaction (#25641): the
     * refund bill, its bill item and fee, the "refunded" mark on the original
     * bill, the Payment rows and the balance updates. Mirrors what
     * InwardRefundController did in separate steps (no drawer update).
     *
     * @param bill the prepared refund bill (numbers, negative totals, type
     * set); must be new (no id yet)
     */
    public Bill saveRefundBillWithPayments(Bill bill, Bill originalBill, PaymentMethodData paymentMethodData,
            WebUser webUser) {
        if (bill == null || originalBill == null) {
            throw new InwardPaymentException("No refund bill to save.");
        }
        if (bill.getId() != null) {
            throw new InwardPaymentException("This refund bill is already saved.");
        }
        billFacade.createAndFlush(bill);

        BillItem item = new BillItem();
        item.setBill(bill);
        item.setGrossValue(0 - bill.getTotal());
        item.setNetValue(0 - bill.getTotal());
        item.setCreatedAt(new Date());
        item.setCreater(webUser);
        billItemFacade.create(item);

        BillFee fee = new BillFee();
        fee.setBill(bill);
        fee.setBillItem(item);
        fee.setCreatedAt(new Date());
        fee.setCreater(webUser);
        fee.setFeeGrossValue(0 - bill.getTotal());
        fee.setFeeValue(0 - bill.getTotal());
        billFeeFacade.create(fee);

        originalBill.setRefunded(true);
        originalBill.setRefundedBill(bill);
        billFacade.edit(originalBill);

        List<Payment> payments = paymentService.createPayment(bill, paymentMethodData);
        if (payments == null || payments.isEmpty()) {
            throw new InwardPaymentException("No payment was recorded. Please check the payment details.");
        }
        paymentService.updateBalances(payments);
        return bill;
    }

    /**
     * After a rolled-back save, reloads the PatientDeposit objects held in the
     * payment data: PatientDepositService lowered their balance in memory, but
     * the rollback restored only the rows.
     */
    public void reloadPatientDeposits(PaymentMethodData paymentMethodData) {
        if (paymentMethodData == null) {
            return;
        }
        reloadPatientDeposit(paymentMethodData.getPatient_deposit());
        ComponentDetail multiple = paymentMethodData.getPaymentMethodMultiple();
        if (multiple != null && multiple.getMultiplePaymentMethodComponentDetails() != null) {
            for (ComponentDetail cd : multiple.getMultiplePaymentMethodComponentDetails()) {
                if (cd != null && cd.getPaymentMethodData() != null) {
                    reloadPatientDeposit(cd.getPaymentMethodData().getPatient_deposit());
                }
            }
        }
    }

    private void reloadPatientDeposit(ComponentDetail detail) {
        if (detail == null || detail.getPatientDepost() == null || detail.getPatientDepost().getId() == null) {
            return;
        }
        detail.setPatientDepost(patientDepositFacade.find(detail.getPatientDepost().getId()));
    }
}
