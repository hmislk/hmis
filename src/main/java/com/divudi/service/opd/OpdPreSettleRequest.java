package com.divudi.service.opd;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything one OPD pre-bill settle needs, built in memory by the controller
 * before the transaction starts (#26246). It also keeps the steps that put the
 * controller's objects back if the transaction rolls back, so the cashier can
 * settle again. Free of CDI so it can be unit tested.
 */
public class OpdPreSettleRequest {

    /** A staff credit to add in the same transaction. */
    public static final class StaffCredit {

        private final Staff staff;
        private final double value;

        public StaffCredit(Staff staff, double value) {
            this.staff = staff;
            this.value = value;
        }

        public Staff getStaff() {
            return staff;
        }

        public double getValue() {
            return value;
        }
    }

    private Bill saleBill;
    private Bill preBill;
    private List<Bill> preIndividualBills = new ArrayList<>();
    private List<Payment> payments = new ArrayList<>();
    private List<Patient> patientsToSave = new ArrayList<>();
    private List<StaffCredit> staffCredits = new ArrayList<>();
    private WebUser loggedUser;
    private PaymentMethod preBillPaymentMethod;
    private PaymentMethodData paymentMethodData;
    private final List<Runnable> undo = new ArrayList<>();

    /** Adds a step to run, in reverse order, when the settle is rolled back. */
    public void addUndo(Runnable step) {
        undo.add(step);
    }

    /**
     * Queues a staff credit for the writer and records the staff member's
     * current credit, so {@link #restore()} puts it back if the settle fails.
     */
    public void addStaffCredit(Staff staff, double value) {
        if (staff == null) {
            return;
        }
        final double before = staff.getCurrentCreditValue();
        addUndo(() -> staff.setCurrentCreditValue(before));
        staffCredits.add(new StaffCredit(staff, value));
    }

    /** Puts the caller's objects back as they were before the settle started. */
    public void restore() {
        for (int i = undo.size() - 1; i >= 0; i--) {
            undo.get(i).run();
        }
    }

    /**
     * Records what the settle changes on the caller's bills, so {@link #restore()}
     * can undo it: the sale bill's id and payments, and the link from the pre-bill
     * and each individual pre-bill to the bill that settles it. Call before the
     * settle.
     */
    public void captureBillState() {
        if (saleBill != null) {
            final Long id = saleBill.getId();
            final List<Payment> heldPayments = saleBill.getPayments();
            addUndo(() -> {
                saleBill.setId(id);
                saleBill.setPayments(heldPayments);
            });
        }
        if (preBill != null) {
            final Bill ref = preBill.getReferenceBill();
            final Bill held = preBill;
            addUndo(() -> held.setReferenceBill(ref));
        }
        for (Bill individual : preIndividualBills) {
            final Bill ref = individual.getReferenceBill();
            addUndo(() -> individual.setReferenceBill(ref));
        }
    }

    public Bill getSaleBill() {
        return saleBill;
    }

    public void setSaleBill(Bill saleBill) {
        this.saleBill = saleBill;
    }

    public Bill getPreBill() {
        return preBill;
    }

    public void setPreBill(Bill preBill) {
        this.preBill = preBill;
    }

    public List<Bill> getPreIndividualBills() {
        return preIndividualBills;
    }

    public void setPreIndividualBills(List<Bill> preIndividualBills) {
        this.preIndividualBills = preIndividualBills == null ? new ArrayList<>() : preIndividualBills;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments == null ? new ArrayList<>() : payments;
    }

    public List<Patient> getPatientsToSave() {
        return patientsToSave;
    }

    public List<StaffCredit> getStaffCredits() {
        return staffCredits;
    }

    public WebUser getLoggedUser() {
        return loggedUser;
    }

    public void setLoggedUser(WebUser loggedUser) {
        this.loggedUser = loggedUser;
    }

    public PaymentMethod getPreBillPaymentMethod() {
        return preBillPaymentMethod;
    }

    public void setPreBillPaymentMethod(PaymentMethod preBillPaymentMethod) {
        this.preBillPaymentMethod = preBillPaymentMethod;
    }

    public PaymentMethodData getPaymentMethodData() {
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }
}
