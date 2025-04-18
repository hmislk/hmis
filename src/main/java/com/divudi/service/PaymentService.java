package com.divudi.service;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillValidation;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.PaymentType;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.CashBook;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.facade.StaffFacade;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

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
    @EJB
    PatientService patientService;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;

    /**
     * Creates payments for the given bill and updates relevant records.
     *
     * <p>
     * This method performs the following tasks:
     * <ul>
     * <li>Creates payments based on the provided bill and payment method
     * data.</li>
     * <li>Updates the drawer.</li>
     * <li>Updates the cashbook if required by the specified options.</li>
     * <li>DO NOT Update the balance of patient deposits.</li>
     * <li>DO NOT Update balances for credit companies.</li>
     * <li>DO NOT Update balances for collecting centres.</li>
     * <li>DO NOT Update Staff Credit.</li>
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
//                payment.getBill().getNetTotal();
                payment.setPaidValue(payment.getBill().getNetTotal());
                payment.setComments(payment.getBill().getComments());
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
                payment.setPaymentDate(paymentMethodData.getSlip().getDate());
                payment.setChequeDate(paymentMethodData.getSlip().getDate());
                payment.setRealizedAt(paymentMethodData.getSlip().getDate());
                break;
            case OnCall:
            case OnlineSettlement:
            case Staff:
                payment.setPaidValue(paymentMethodData.getStaffCredit().getTotalValue());
                payment.setComments(paymentMethodData.getStaffCredit().getComment());
                break;
            case IOU:
                payment.setReferenceNo(paymentMethodData.getIou().getReferenceNo());
                payment.setChequeDate(paymentMethodData.getIou().getDate());
                payment.setToStaff(paymentMethodData.getIou().getToStaff());
                payment.setComments(paymentMethodData.getIou().getComment());
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
        patientDepositService.updateBalance(p, pd);

    }

    public List<PaymentMethod> fetchAvailablePaymentMethodsForRefundsAndCancellations(Bill originalBill) {
        Set<PaymentMethod> uniqueMethods = new LinkedHashSet<>();
        uniqueMethods.add(PaymentMethod.Cash);

        if (originalBill == null) {
            return new ArrayList<>(uniqueMethods);
        }

        if (originalBill.getPaymentMethod() == null) {
            return new ArrayList<>(uniqueMethods);
        }

        if (originalBill.getPaymentMethod() != PaymentMethod.MultiplePaymentMethods) {
            uniqueMethods.add(originalBill.getPaymentMethod());
            return new ArrayList<>(uniqueMethods);
        }

        List<Payment> originalBillPayments = billService.fetchBillPayments(originalBill);
        if (originalBillPayments == null) {
            originalBillPayments = billService.fetchBillPayments(originalBill.getBackwardReferenceBill());
        }
        if (originalBillPayments == null) {
            return new ArrayList<>(uniqueMethods);
        }

        for (Payment p : originalBillPayments) {
            uniqueMethods.add(p.getPaymentMethod());
        }

        return new ArrayList<>(uniqueMethods);
    }

    public BillValidation checkForErrorsInPaymentDetailsForInBills(PaymentMethod paymentMethod, PaymentMethodData paymentMethodData, Double netTotal, Patient patient, Department department) {
        BillValidation bv = new BillValidation();

        // Check for null payment method
        if (paymentMethod == null) {
            bv.setErrorMessage("Please select a payment method.");
            bv.setErrorPresent(true);
            return bv;
        }

        if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
            boolean creditPaymentFound = false;
            List<PaymentMethod> usedPaymentMethods = new ArrayList<>();

            // Iterate over each payment component in the multiple payment method
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                PaymentMethod pm = cd.getPaymentMethod();
                PaymentMethodData pmd = cd.getPaymentMethodData();

                // Check if any component is a Credit payment method
                if (pm.getPaymentType() == PaymentType.CREDIT) {
                    bv.setErrorMessage("Credit payments are not allowed inside Multiple Payment Methods.");
                    bv.setErrorPresent(true);
                    return bv;
                }

                // Ensure no duplicates of restricted methods
                if (!Arrays.asList(PaymentMethod.Slip, PaymentMethod.Cheque, PaymentMethod.ewallet).contains(pm)) {
                    if (usedPaymentMethods.contains(pm)) {
                        bv.setErrorMessage("Duplicate payment methods found in Multiple Payment Methods: " + pm.getLabel());
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    usedPaymentMethods.add(pm);
                }

                // Recursively call the validation method for each component
                BillValidation componentValidation = checkForErrorsInPaymentDetailsForInBills(pm, pmd, null, patient, department);
                if (componentValidation.isErrorPresent()) {
                    return componentValidation; // If any component has an error, return immediately with that error
                }
            }
        } else {
            switch (paymentMethod) {
                case Credit:
                    if (paymentMethodData.getCredit().getComment() == null
                            && configOptionApplicationController.getBooleanValueByKey("Package Billing - Credit Comment is Mandatory", false)) {
                        bv.setErrorMessage("Please enter a Credit comment.");
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    if (paymentMethodData.getCredit().getComment().trim().isEmpty()
                            && configOptionApplicationController.getBooleanValueByKey("Package Billing - Credit Comment is Mandatory", false)) {
                        bv.setErrorMessage("Please enter a Credit comment.");
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    if (paymentMethodData.getCredit().getInstitution() == null) {
                        bv.setErrorMessage("Please enter a Credit Company.");
                        bv.setErrorPresent(true);
                        return bv;
                    } else {
                        bv.setCompany(paymentMethodData.getCredit().getInstitution()); // Handling the case where institution is valid
                    }
                    break;

                case Card:
                    if (paymentMethodData.getCreditCard().getComment().trim().isEmpty()
                            && configOptionApplicationController.getBooleanValueByKey("Package Billing - CreditCard Comment is Mandatory", false)) {
                        bv.setErrorMessage("Please enter a Credit Card comment.");
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    if (paymentMethodData.getCreditCard().getInstitution() == null
                            || paymentMethodData.getCreditCard().getNo() == null) {
                        bv.setErrorMessage("Please Fill Credit Card Number and Bank.");
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    break;

                case Cheque:
                    if (paymentMethodData.getCheque().getComment().trim().isEmpty()
                            && configOptionApplicationController.getBooleanValueByKey("Package Billing - Cheque Comment is Mandatory", false)) {
                        bv.setErrorMessage("Please enter a Cheque comment.");
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    if (paymentMethodData.getCheque().getInstitution() == null
                            || paymentMethodData.getCheque().getNo() == null
                            || paymentMethodData.getCheque().getDate() == null) {
                        bv.setErrorMessage("Please select Cheque Number, Bank and Cheque Date.");
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    break;

                case ewallet:
                    if (paymentMethodData.getEwallet().getComment().trim().isEmpty()
                            && configOptionApplicationController.getBooleanValueByKey("Package Billing - E-Wallet Comment is Mandatory", false)) {
                        bv.setErrorMessage("Please enter an E-Wallet comment.");
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    if (paymentMethodData.getEwallet().getInstitution() == null
                            || paymentMethodData.getEwallet().getNo() == null) {
                        bv.setErrorMessage("Please Fill eWallet Reference Number and Bank.");
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    break;

                case Slip:
                    if (paymentMethodData.getSlip().getComment().trim().isEmpty()
                            && configOptionApplicationController.getBooleanValueByKey("Package Billing - Slip Comment is Mandatory", false)) {
                        bv.setErrorMessage("Please enter a Slip comment.");
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    if (paymentMethodData.getSlip().getInstitution() == null
                            || paymentMethodData.getSlip().getDate() == null) {
                        bv.setErrorMessage("Please Fill Memo, Bank and Slip Date.");
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    break;
                case PatientDeposit:
                    if (patient == null) {
                        bv.setErrorMessage("No Patient is selected. Can't proceed with Patient Deposits");
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    patient = patientService.reloadPatient(patient);
                    if (!patient.getHasAnAccount()) {
                        bv.setErrorMessage("Patient has not account. Can't proceed with Patient Deposits");
                        bv.setErrorPresent(true);
                        return bv;
                    }
                    double creditLimitAbsolute = Math.abs(patient.getCreditLimit());
                    PatientDeposit pd = patientDepositService.getDepositOfThePatient(patient, department);
                    double availableForPurchase = pd.getBalance() + creditLimitAbsolute;
                    double payhingThisTimeValue;
                    if (netTotal == null) {
                        payhingThisTimeValue = paymentMethodData.getPatient_deposit().getTotalValue();
                    } else {
                        payhingThisTimeValue = netTotal;
                    }

                    if (payhingThisTimeValue > availableForPurchase) {

                        bv.setErrorMessage("No Sufficient Patient Deposit");
                        bv.setErrorPresent(true);
                        return bv;
                    }

                default:
                    // No additional checks for other methods
                    break;
            }
        }

        return bv;
    }

}
