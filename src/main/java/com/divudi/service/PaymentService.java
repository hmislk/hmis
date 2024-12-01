package com.divudi.service;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.PaymentMethod;
import static com.divudi.data.PaymentMethod.Agent;
import static com.divudi.data.PaymentMethod.Card;
import static com.divudi.data.PaymentMethod.Cash;
import static com.divudi.data.PaymentMethod.Cheque;
import static com.divudi.data.PaymentMethod.Credit;
import static com.divudi.data.PaymentMethod.MultiplePaymentMethods;
import static com.divudi.data.PaymentMethod.OnCall;
import static com.divudi.data.PaymentMethod.OnlineSettlement;
import static com.divudi.data.PaymentMethod.PatientDeposit;
import static com.divudi.data.PaymentMethod.Slip;
import static com.divudi.data.PaymentMethod.Staff;
import static com.divudi.data.PaymentMethod.YouOweMe;
import static com.divudi.data.PaymentMethod.ewallet;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.Patient;
import com.divudi.entity.Payment;
import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.CashBook;
import com.divudi.facade.BillFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PaymentFacade;
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
    PaymentFacade paymentFacade;
    @EJB
    StaffService staffBean;
    @EJB
    CashbookService cashbookService;

    public List<Payment> createPayment(Bill bill, PaymentMethod pm, PaymentMethodData paymentMethodData, Department department, WebUser webUser, Patient patient) {
        CashBook cashbook = cashbookService.findAndSaveCashBookBySite(department.getSite(), department.getInstitution(), department);
        List<Payment> payments = new ArrayList<>();
        Date currentDate = new Date();

        if (pm == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment payment = createPaymentFromComponentDetail(cd, bill, department, webUser, patient, currentDate);
                if (payment != null) {
                    paymentFacade.create(payment);
                    cashbookService.writeCashBookEntryAtPaymentCreation(payment, webUser, cashbook, department);
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

            populatePaymentDetails(payment, pm, paymentMethodData, patient);

            paymentFacade.create(payment);
            cashbookService.writeCashBookEntryAtPaymentCreation(payment, webUser, cashbook, department);
            payments.add(payment);
        }

        return payments;
    }

    private Payment createPaymentFromComponentDetail(ComponentDetail cd, Bill bill, Department department, WebUser webUser, Patient patient, Date currentDate) {
        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setInstitution(department.getInstitution());
        payment.setDepartment(department);
        payment.setCreatedAt(currentDate);
        payment.setCreater(webUser);
        payment.setPaymentMethod(cd.getPaymentMethod());

        populatePaymentDetails(payment, cd.getPaymentMethod(), cd.getPaymentMethodData(), patient);

        return payment;
    }

    private void populatePaymentDetails(Payment payment, PaymentMethod paymentMethod, PaymentMethodData paymentMethodData, Patient patient) {
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
                payment.setPolicyNo(paymentMethodData.getEwallet().getReferralNo());
                payment.setComments(paymentMethodData.getEwallet().getComment());
                payment.setReferenceNo(paymentMethodData.getEwallet().getReferenceNo());
                payment.setCreditCompany(paymentMethodData.getEwallet().getInstitution());
                break;
            case Agent:
            case Credit:
                payment.setPolicyNo(paymentMethodData.getCredit().getReferralNo());
                payment.setComments(paymentMethodData.getCredit().getComment());
                payment.setReferenceNo(paymentMethodData.getCredit().getReferenceNo());
                payment.setCreditCompany(paymentMethodData.getCredit().getInstitution());
                break;
            case PatientDeposit:
                if (patient != null) {
                    double newBalance = (patient.getRunningBalance() != null ? patient.getRunningBalance() : 0.0)
                            - paymentMethodData.getPatient_deposit().getTotalValue();
                    patient.setRunningBalance(newBalance);
                    patientFacade.edit(patient);
                }
                break;
            case Slip:
                payment.setBank(paymentMethodData.getSlip().getInstitution());
                payment.setPaidValue(paymentMethodData.getSlip().getTotalValue());
                payment.setRealizedAt(paymentMethodData.getSlip().getDate());
                break;
            case OnCall:
            case OnlineSettlement:
            case Staff:
                payment.setPaidValue(paymentMethodData.getStaffCredit().getTotalValue());
                if (paymentMethodData.getStaffCredit().getToStaff() != null) {
                    staffBean.updateStaffCredit(paymentMethodData.getStaffCredit().getToStaff(), paymentMethodData.getStaffCredit().getTotalValue());
                    JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                }
                break;
            default:
                break;
        }
    }

}
