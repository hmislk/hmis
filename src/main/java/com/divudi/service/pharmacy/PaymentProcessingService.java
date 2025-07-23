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
