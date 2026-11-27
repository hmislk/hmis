package com.divudi.service.opd;

import com.divudi.bean.common.BillBeanController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Payment;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientDepositFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.service.PatientDepositService;
import com.divudi.service.StaffService;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Settles an OPD pre-bill in ONE container transaction (#26246): the settling
 * bill, its payments, the patient deposit and staff credit changes, the link from
 * the pre-bill, and the individual billed bills with their items and fees commit
 * together or not at all. The controller builds the request in memory, calls
 * this, and puts its own objects back if this rolls back.
 *
 * <p>The cash drawer is not touched here. The caller updates it after the commit,
 * as it does for every other settle, so a busy drawer cannot undo a saved
 * bill.</p>
 */
@Stateless
public class OpdPreSettleWriter {

    @Inject
    BillBeanController billBean;

    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    PatientFacade patientFacade;
    @EJB
    StaffService staffService;
    @EJB
    PatientDepositService patientDepositService;
    @EJB
    PatientDepositFacade patientDepositFacade;

    /**
     * Saves everything of the settle.
     *
     * @return the individual billed bills created under the settling bill
     * @throws OpdPreSettleException for a business error; any other failure
     * reaches the caller as an {@code EJBException}, and the transaction is
     * rolled back either way
     */
    public List<Bill> settle(OpdPreSettleRequest request) {
        Bill saleBill = request.getSaleBill();
        Bill preBill = request.getPreBill();
        if (saleBill == null || preBill == null) {
            throw new OpdPreSettleException("There is no bill to settle.");
        }
        if (saleBill.getId() == null) {
            billFacade.create(saleBill);
        }
        for (Payment p : request.getPayments()) {
            p.setBill(saleBill);
            paymentFacade.create(p);
        }
        for (Patient patient : request.getPatientsToSave()) {
            patientFacade.edit(patient);
        }
        for (OpdPreSettleRequest.DepositUse use : request.getDepositUses()) {
            takeFromDeposit(saleBill, use);
        }
        for (OpdPreSettleRequest.StaffCredit credit : request.getStaffCredits()) {
            staffService.updateStaffCredit(credit.getStaff(), credit.getValue());
        }
        saleBill.setPayments(request.getPayments());
        billFacade.edit(saleBill);

        preBill.setReferenceBill(saleBill);
        billFacade.edit(preBill);

        List<Bill> created = new ArrayList<>();
        for (Bill individualPreBill : request.getPreIndividualBills()) {
            created.add(saveIndividualBilledBill(request, individualPreBill));
        }
        billFacade.edit(preBill);
        return created;
    }

    /**
     * Takes the amount from the patient's deposit record, the one the deposit
     * receipt raises, and checks it again inside the transaction (#26260): the
     * balance plus the credit limit must cover the amount.
     */
    private void takeFromDeposit(Bill saleBill, OpdPreSettleRequest.DepositUse use) {
        PatientDeposit found = patientDepositService.getDepositOfThePatient(use.getPatient(), saleBill.getDepartment());
        // Hardening: lock the deposit record while it is checked and lowered.
        PatientDeposit pd = patientDepositFacade.findWithLock(found.getId());
        if (pd == null) {
            pd = found;
        }
        double balance = pd.getBalance() == null ? 0.0 : pd.getBalance();
        double creditLimit = use.getPatient().getCreditLimit() == null ? 0.0 : Math.abs(use.getPatient().getCreditLimit());
        if (use.getAmount() > balance + creditLimit) {
            throw new OpdPreSettleException("No Sufficient Patient Deposit");
        }
        patientDepositService.handleUtilization(saleBill, pd, use.getAmount());
    }

    private Bill saveIndividualBilledBill(OpdPreSettleRequest request, Bill individualPreBill) {
        Bill saleBill = request.getSaleBill();
        Bill out = new BilledBill();
        out.copy(individualPreBill);
        out.copyValue(individualPreBill);
        out.setBalance(0.0);
        out.setBillClassType(BillClassType.BilledBill);
        out.setBillType(BillType.OpdBill);
        out.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        out.setDepartment(request.getLoggedUser().getDepartment());
        out.setInstitution(request.getLoggedUser().getDepartment().getInstitution());
        out.setBillDate(new java.util.Date());
        out.setBillTime(new java.util.Date());
        out.setCreatedAt(Calendar.getInstance().getTime());
        out.setCreater(request.getLoggedUser());
        out.setReferenceBill(individualPreBill);
        out.setBackwardReferenceBill(saleBill);
        out.setInsId(individualPreBill.getInsId());
        out.setDeptId(individualPreBill.getDeptId());
        billFacade.create(out);

        individualPreBill.setReferenceBill(out);
        billFacade.edit(individualPreBill);

        for (BillItem tbi : individualPreBill.getBillItems()) {
            BillItem newBillItem = new BillItem();
            newBillItem.copy(tbi);
            newBillItem.setBill(out);
            newBillItem.setCreatedAt(Calendar.getInstance().getTime());
            newBillItem.setCreater(request.getLoggedUser());
            billItemFacade.create(newBillItem);
            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + tbi.getId();
            List<BillFee> preBillItemFees = billFeeFacade.findByJpql(sql);
            List<BillFee> newFees = new ArrayList<>();
            for (BillFee bfo : preBillItemFees) {
                BillFee nbf = new BillFee();
                nbf.copy(bfo);
                nbf.setCreatedAt(Calendar.getInstance().getTime());
                nbf.setCreater(request.getLoggedUser());
                nbf.setBillItem(newBillItem);
                nbf.setBill(out);
                billFeeFacade.create(nbf);
                newFees.add(nbf);
            }
            newBillItem.setBillFees(newFees);
            out.getBillItems().add(newBillItem);
            out.getBillFees().addAll(newFees);
        }

        out.setPaymentMethod(saleBill.getPaymentMethod());
        applyPaymentMethodData(out, request);
        billFacade.edit(out);
        return out;
    }

    /** Copies the payment details onto the bill; a method of its own so a test can replace it. */
    void applyPaymentMethodData(Bill bill, OpdPreSettleRequest request) {
        billBean.setPaymentMethodData(bill, request.getPreBillPaymentMethod(), request.getPaymentMethodData());
    }
}
