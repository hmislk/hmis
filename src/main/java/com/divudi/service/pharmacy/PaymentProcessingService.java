package com.divudi.service.pharmacy;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.ComponentDetail;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.PaymentFacade;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Updated payment field population for various methods (refs #14152)
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class PaymentProcessingService {

    @EJB
    private PaymentFacade paymentFacade;

    private Payment initPayment(Bill bill,
            Institution institution,
            Department department,
            WebUser creater,
            PaymentMethod method) {
        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setInstitution(institution);
        payment.setDepartment(department);
        payment.setCreatedAt(new Date());
        payment.setCreater(creater);
        payment.setPaymentMethod(method);
        return payment;
    }

    private void populatePaymentDetails(Payment payment, PaymentMethodData data, PaymentMethod method) {
        switch (method) {
            case Card:
                payment.setBank(data.getCreditCard().getInstitution());
                payment.setCreditCardRefNo(data.getCreditCard().getNo());
                payment.setPaidValue(data.getCreditCard().getTotalValue());
                break;
            case Cheque:
                payment.setChequeDate(data.getCheque().getDate());
                payment.setChequeRefNo(data.getCheque().getNo());
                payment.setPaidValue(data.getCheque().getTotalValue());
                break;
            case Cash:
                payment.setPaidValue(data.getCash().getTotalValue());
                break;
            case ewallet:
                payment.setPaidValue(data.getEwallet().getTotalValue());
                payment.setPolicyNo(data.getEwallet().getReferralNo());
                payment.setComments(data.getEwallet().getComment());
                payment.setReferenceNo(data.getEwallet().getReferenceNo());
                payment.setCreditCompany(data.getEwallet().getInstitution());
                break;
            case Credit:
                payment.setPaidValue(data.getCredit().getTotalValue());
                payment.setPolicyNo(data.getCredit().getReferralNo());
                payment.setComments(data.getCredit().getComment());
                payment.setReferenceNo(data.getCredit().getReferenceNo());
                payment.setCreditCompany(data.getCredit().getInstitution());
                break;
            case PatientDeposit:
                payment.setPaidValue(data.getPatient_deposit().getTotalValue());
                break;
            case Slip:
                payment.setPaidValue(data.getSlip().getTotalValue());
                payment.setComments(data.getSlip().getComment());
                payment.setBank(data.getSlip().getInstitution());
                payment.setReferenceNo(data.getSlip().getReferenceNo());
                payment.setRealizedAt(data.getSlip().getDate());
                payment.setPaymentDate(data.getSlip().getDate());
                payment.setChequeDate(data.getSlip().getDate());
                break;
            case OnCall:
            case Staff:
                payment.setPaidValue(data.getStaffCredit().getTotalValue());
                payment.setComments(data.getStaffCredit().getComment());
                break;
            case OnlineSettlement:
                payment.setPaidValue(data.getOnlineSettlement().getTotalValue());
                payment.setComments(data.getOnlineSettlement().getComment());
                break;
            case IOU:
                payment.setReferenceNo(data.getIou().getReferenceNo());
                payment.setChequeDate(data.getIou().getDate());
                payment.setToStaff(data.getIou().getToStaff());
                payment.setComments(data.getIou().getComment());
                break;
            default:
                payment.setPaidValue(payment.getBill().getNetTotal());
                break;
        }
    }

    public List<Payment> createPaymentsForBill(Bill bill,
            PaymentMethod method,
            PaymentMethodData data,
            Institution institution,
            Department department,
            WebUser creater) {
        return createMultiplePayments(bill, method, data, institution, department, creater);
    }

    public List<Payment> createMultiplePayments(Bill bill,
            PaymentMethod method,
            PaymentMethodData data,
            Institution institution,
            Department department,
            WebUser creater) {
        List<Payment> payments = new ArrayList<>();
        if (method == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : data.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = initPayment(bill, institution, department, creater, cd.getPaymentMethod());
                populatePaymentDetails(p, cd.getPaymentMethodData(), cd.getPaymentMethod());
                paymentFacade.create(p);
                payments.add(p);
            }
        } else {
            Payment p = initPayment(bill, institution, department, creater, method);
            populatePaymentDetails(p, data, method);
            paymentFacade.create(p);
            payments.add(p);
        }
        return payments;
    }
}
