/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.inward;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillEntry;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.List;

/**
 * Everything {@link InwardServiceBillService} needs to turn a list of
 * {@link BillEntry} into inward service bills. Exists so the settle pipeline can
 * be shared between the hand-driven <i>Add Services / Investigations to BHT</i>
 * screen and the automatic admission charges (issue #23594) without either side
 * reaching into the other's page state.
 *
 * @author Buddhika
 */
public class InwardServiceBillRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The entries to bill. Each carries a BillItem and its BillFees. */
    private List<BillEntry> billEntries;

    private PatientEncounter patientEncounter;

    /**
     * The bill's {@code fromDepartment}, and the department the inward margin
     * matrix is looked up against. Callers resolve it the way
     * {@code BillBhtController.feeDepartment(PatientEncounter)} does.
     */
    private Department matrixDepartment;

    /** Payment method used for the inward margin matrix lookup. */
    private PaymentMethod marginPaymentMethod;

    /**
     * Payment method written onto the generated bills. Deliberately separate
     * from {@link #marginPaymentMethod}: the hand-driven path nulls its own
     * field before settling, and that behaviour is preserved.
     */
    private PaymentMethod billPaymentMethod;

    private PaymentScheme paymentScheme;

    private Doctor referredBy;

    /**
     * The surgery batch bill, when settling surgery services - passed straight
     * to {@code BillBeanController.setSurgeryData(...)}, which returns
     * immediately when it is null.
     */
    private Bill surgeryBatchBill;

    private WebUser loggedUser;

    /** {@code sessionController.getDepartment()} - drives bill numbering. */
    private Department loggedDepartment;

    /** {@code loggedUser.getDepartment()} - the bill's own department. */
    private Department creatingDepartment;

    /**
     * The list the generated bills are appended to, and the list the batch bill
     * is linked over.
     *
     * <p>This is a parameter rather than a fresh list because
     * {@code BillBhtController.settleBillSurgery()} does not reset its
     * {@code bills} field before settling, so the batch bill has always been
     * linked over the controller's whole accumulated list. The hand-driven path
     * passes {@code getBills()}; the automatic path passes a fresh list.</p>
     */
    private List<Bill> billCollector;

    /**
     * Whether to apply the inward price matrix to the generated fees. False for
     * automatic admission charges, whose price comes from the configuration and
     * must not be re-derived - which also suppresses the inward discount matrix,
     * since {@code InwardBeanController.setBillFeeMargin(...)} applies both.
     */
    private boolean applyInwardMargin = true;

    public List<BillEntry> getBillEntries() {
        return billEntries;
    }

    public void setBillEntries(List<BillEntry> billEntries) {
        this.billEntries = billEntries;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Department getMatrixDepartment() {
        return matrixDepartment;
    }

    public void setMatrixDepartment(Department matrixDepartment) {
        this.matrixDepartment = matrixDepartment;
    }

    public PaymentMethod getMarginPaymentMethod() {
        return marginPaymentMethod;
    }

    public void setMarginPaymentMethod(PaymentMethod marginPaymentMethod) {
        this.marginPaymentMethod = marginPaymentMethod;
    }

    public PaymentMethod getBillPaymentMethod() {
        return billPaymentMethod;
    }

    public void setBillPaymentMethod(PaymentMethod billPaymentMethod) {
        this.billPaymentMethod = billPaymentMethod;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public Doctor getReferredBy() {
        return referredBy;
    }

    public void setReferredBy(Doctor referredBy) {
        this.referredBy = referredBy;
    }

    public Bill getSurgeryBatchBill() {
        return surgeryBatchBill;
    }

    public void setSurgeryBatchBill(Bill surgeryBatchBill) {
        this.surgeryBatchBill = surgeryBatchBill;
    }

    public WebUser getLoggedUser() {
        return loggedUser;
    }

    public void setLoggedUser(WebUser loggedUser) {
        this.loggedUser = loggedUser;
    }

    public Department getLoggedDepartment() {
        return loggedDepartment;
    }

    public void setLoggedDepartment(Department loggedDepartment) {
        this.loggedDepartment = loggedDepartment;
    }

    public Department getCreatingDepartment() {
        return creatingDepartment;
    }

    public void setCreatingDepartment(Department creatingDepartment) {
        this.creatingDepartment = creatingDepartment;
    }

    public List<Bill> getBillCollector() {
        return billCollector;
    }

    public void setBillCollector(List<Bill> billCollector) {
        this.billCollector = billCollector;
    }

    public boolean isApplyInwardMargin() {
        return applyInwardMargin;
    }

    public void setApplyInwardMargin(boolean applyInwardMargin) {
        this.applyInwardMargin = applyInwardMargin;
    }
}
