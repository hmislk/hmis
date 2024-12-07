package com.divudi.service;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.PaymentMethod;
import static com.divudi.data.PaymentMethod.Agent;
import static com.divudi.data.PaymentMethod.Card;
import static com.divudi.data.PaymentMethod.Cash;
import static com.divudi.data.PaymentMethod.Cheque;
import static com.divudi.data.PaymentMethod.Credit;
import static com.divudi.data.PaymentMethod.OnCall;
import static com.divudi.data.PaymentMethod.OnlineSettlement;
import static com.divudi.data.PaymentMethod.PatientDeposit;
import static com.divudi.data.PaymentMethod.Slip;
import static com.divudi.data.PaymentMethod.Staff;
import static com.divudi.data.PaymentMethod.Staff_Welfare;
import static com.divudi.data.PaymentMethod.ewallet;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientDeposit;
import com.divudi.entity.Payment;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.CashBook;
import com.divudi.facade.BillFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.StaffFacade;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Stateless
public class PaymentService {

    @EJB
    PatientFacade patientFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    StaffFacade staffFacade;
    @EJB
    PaymentFacade paymentFacade;
    @EJB
    StaffService staffService;
    @EJB
    CashbookService cashbookService;
    @EJB
    DrawerService drawerService;
    @EJB
    BillService billService;
    @EJB
    PatientDepositService patientDepositService;

    /**
     * Creates payments for the given bill and updates relevant records.
     *
     * <p>
     * This method performs the following tasks:
     * <ul>
     * <li>Creates payments based on the provided bill and payment method
     * data.</li>
     * <li>DO NOT Update the balance of patient deposits.</li>
     * <li>DO NOT Update balances for credit companies.</li>
     * <li>DO NOT Update balances for collecting centres.</li>
     * <li>DO NOT Update Staff Credit.</li>
     * <li>Writes to the drawer for transaction recording.</li>
     * <li>Updates the cashbook if required by the specified options.</li>
     * </ul>
     *
     * @param bill The bill for which payments are being created.
     * @param paymentMethodData Additional data for processing the payment
     * method.
     * @return A list of created payments associated with the bill.
     */
    public List<Payment> createPayment(Bill bill, PaymentMethodData paymentMethodData) {
        return createPayment(bill, bill.getPaymentMethod(), paymentMethodData, bill.getDepartment(), bill.getCreater());
    }

    /**
     * Creates payments for the given Cancellation Bill and updates relevant
     * records.
     *
     * <p>
     * This method performs the following tasks:
     * <ul>
     * <li>Creates payments based on the provided cancellation bill using the
     * payment data in the original bill.</li>
     * <li>DO NOT Update the balance of patient deposits.</li>
     * <li>DO NOT Update balances for credit companies.</li>
     * <li>DO NOT Update Staff Credit.</li>
     * <li>Writes to the drawer for transaction recording.</li>
     * <li>Updates the cashbook if required by the specified options.</li>
     * </ul>
     *
     * @param cancellationBill The cancellation bill for which payments are
     * being created.
     * @return A list of created payments associated with the bill.
     */
    public List<Payment> createPaymentsForCancelling(Bill cancellationBill) {
        List<Payment> newPayments = new ArrayList<>();
        List<Payment> originalBillPayments = billService.fetchBillPayments(cancellationBill.getBilledBill());
        if (originalBillPayments != null) {
            for (Payment originalBillPayment : originalBillPayments) {
                Payment p = originalBillPayment.clonePaymentForNewBill();
                p.invertValues();
                p.setBill(cancellationBill);
                p.setInstitution(cancellationBill.getInstitution());
                p.setDepartment(cancellationBill.getDepartment());
                p.setCreatedAt(new Date());
                p.setCreater(cancellationBill.getCreater());
                paymentFacade.create(p);
                newPayments.add(p);
                cashbookService.writeCashBookEntryAtPaymentCreation(p);
                drawerService.updateDrawer(p);
            }
        }
        return newPayments;
    }

    private List<Payment> createPayment(Bill bill, PaymentMethod pm, PaymentMethodData paymentMethodData, Department department, WebUser webUser) {
        CashBook cashbook = cashbookService.findAndSaveCashBookBySite(department.getSite(), department.getInstitution(), department);
        List<Payment> payments = new ArrayList<>();
        Date currentDate = new Date();

        if (pm == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment payment = createPaymentFromComponentDetail(cd, bill, department, webUser, currentDate);
                if (payment != null) {
                    paymentFacade.create(payment);
                    cashbookService.writeCashBookEntryAtPaymentCreation(payment, webUser, cashbook, department);
                    drawerService.updateDrawer(payment);
                    payments.add(payment);
                }
            }
        } else {
            Payment payment = new Payment();
            payment.setBill(bill);
            payment.setInstitution(department.getInstitution());
            payment.setDepartment(department);
            payment.setCreatedAt(currentDate);
            payment.setCreater(webUser);
            payment.setPaymentMethod(pm);
            populatePaymentDetails(payment, pm, paymentMethodData);
            payment.setPaidValue(bill.getNetTotal());
            paymentFacade.create(payment);
            cashbookService.writeCashBookEntryAtPaymentCreation(payment);
            drawerService.updateDrawer(payment);

            payments.add(payment);
        }

        return payments;
    }

    private Payment createPaymentFromComponentDetail(ComponentDetail cd, Bill bill, Department department, WebUser webUser, Date currentDate) {
        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setInstitution(department.getInstitution());
        payment.setDepartment(department);
        payment.setCreatedAt(currentDate);
        payment.setCreater(webUser);
        payment.setPaymentMethod(cd.getPaymentMethod());
        populatePaymentDetails(payment, cd.getPaymentMethod(), cd.getPaymentMethodData());
        return payment;
    }

    private void populatePaymentDetails(Payment payment, PaymentMethod paymentMethod, PaymentMethodData paymentMethodData) {
        switch (paymentMethod) {
            case Card:
                payment.setBank(paymentMethodData.getCreditCard().getInstitution());
                payment.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                payment.setPaidValue(paymentMethodData.getCreditCard().getTotalValue());
                payment.setComments(paymentMethodData.getCreditCard().getComment());
                break;
            case Cheque:
                payment.setBank(paymentMethodData.getCheque().getInstitution());
                payment.setChequeDate(paymentMethodData.getCheque().getDate());
                payment.setChequeRefNo(paymentMethodData.getCheque().getNo());
                payment.setPaidValue(paymentMethodData.getCheque().getTotalValue());
                payment.setComments(paymentMethodData.getCheque().getComment());
                break;
            case Cash:
                payment.setPaidValue(paymentMethodData.getCash().getTotalValue());
                payment.setComments(paymentMethodData.getCash().getComment());
                break;
            case ewallet:
                payment.setPaidValue(paymentMethodData.getEwallet().getTotalValue());
                payment.setPolicyNo(paymentMethodData.getEwallet().getReferralNo());
                payment.setComments(paymentMethodData.getEwallet().getComment());
                payment.setReferenceNo(paymentMethodData.getEwallet().getReferenceNo());
                payment.setCreditCompany(paymentMethodData.getEwallet().getInstitution());
                break;
            case Agent:
                break;
            case Credit:
                payment.setPaidValue(paymentMethodData.getCredit().getTotalValue());
                payment.setPolicyNo(paymentMethodData.getCredit().getReferralNo());
                payment.setComments(paymentMethodData.getCredit().getComment());
                payment.setReferenceNo(paymentMethodData.getCredit().getReferenceNo());
                payment.setCreditCompany(paymentMethodData.getCredit().getInstitution());
                if (payment.getBill().getCreditCompany() == null) {
                    payment.getBill().setCreditCompany(payment.getCreditCompany());
                    if (payment.getBill().getId() == null) {
                        billFacade.create(payment.getBill());
                    } else {
                        billFacade.edit(payment.getBill());
                    }
                }
                break;
            case PatientDeposit:
                payment.setPaidValue(paymentMethodData.getPatient_deposit().getTotalValue());
                break;
            case Slip:
                payment.setPaidValue(paymentMethodData.getSlip().getTotalValue());
                payment.setComments(paymentMethodData.getSlip().getComment());
                payment.setBank(paymentMethodData.getSlip().getInstitution());
                payment.setReferenceNo(paymentMethodData.getSlip().getReferenceNo());
                payment.setRealizedAt(paymentMethodData.getSlip().getDate());
                break;
            case OnCall:
            case OnlineSettlement:
            case Staff:
                payment.setPaidValue(paymentMethodData.getStaffCredit().getTotalValue());
                payment.setComments(paymentMethodData.getStaffCredit().getComment());
                break;
            default:
                break;
        }
    }

    public void updateBalances(List<Payment> payments) {
        if (payments == null) {
            return;
        }
        for (Payment p : payments) {
            switch (p.getPaymentMethod()) {
                case Agent:
                case Card:
                case Cash:
                case Cheque:
                case IOU:
                case MultiplePaymentMethods:
                case None:
                case OnCall:
                case OnlineSettlement:
                case PatientPoints:
                case Slip:
                case Voucher:
                case ewallet:
                case YouOweMe:
                    break;
                case PatientDeposit:
                    updatePatientDeposits(p);
                    break;
                case Staff_Welfare:
                    updateStaffWelare(p);
                    break;
                case Staff:
                    updateStaffCredit(p);
                    break;
                case Credit:
                    updateCompanyCredit(p);
                    break;
                default:
                    continue;
            }
        }
    }

    private void updateCompanyCredit(Payment p) {
        //TODO: Add Logic Here
    }

    private void updateStaffCredit(Payment p) {
        if (p == null) {
            return;
        }
        if (p.getBill() == null) {
            return;
        }
        if (p.getBill().getToStaff() == null) {
            return;
        }
        Staff toStaff = p.getBill().getToStaff();
        staffService.updateStaffWelfare(toStaff, p.getPaidValue());
    }

    private void updateStaffWelare(Payment p) {
        if (p == null) {
            return;
        }
        if (p.getBill() == null) {
            return;
        }
        if (p.getBill().getToStaff() == null) {
            return;
        }
        Staff toStaff = p.getBill().getToStaff();
        staffService.updateStaffWelfare(toStaff, p.getPaidValue());
    }

    private void updatePatientDeposits(Payment p) {
        if (p == null) {
            return;
        }
        if (p.getBill() == null) {
            return;
        }
        if (p.getBill().getPatient() == null) {
            return;
        }
        Patient pt = patientFacade.findWithoutCache(p.getBill().getPatient().getId());
        if (pt.getRunningBalance() != null) {
            pt.setRunningBalance(pt.getRunningBalance() - p.getPaidValue());
        } else {
            pt.setRunningBalance(0.0 - p.getPaidValue());
        }
        patientFacade.editAndCommit(pt);
        PatientDeposit pd = patientDepositService.getDepositOfThePatient(pt, p.getDepartment());
        patientDepositService.updateBalance(p.getBill(), pd);

    }

}
