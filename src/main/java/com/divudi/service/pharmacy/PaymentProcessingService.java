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
                Payment p = new Payment();
                p.setBill(bill);
                p.setInstitution(institution);
                p.setDepartment(department);
                p.setCreatedAt(new Date());
                p.setCreater(creater);
                p.setPaymentMethod(cd.getPaymentMethod());
                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
                        break;
                    case Cheque:
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        break;
                    default:
                        break;
                }
                paymentFacade.create(p);
                payments.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(institution);
            p.setDepartment(department);
            p.setCreatedAt(new Date());
            p.setCreater(creater);
            p.setPaymentMethod(method);
            switch (method) {
                case Card:
                    p.setBank(data.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(data.getCreditCard().getNo());
                    p.setPaidValue(data.getCreditCard().getTotalValue());
                    break;
                case Cheque:
                    p.setChequeDate(data.getCheque().getDate());
                    p.setChequeRefNo(data.getCheque().getNo());
                    p.setPaidValue(data.getCheque().getTotalValue());
                    break;
                case Cash:
                    p.setPaidValue(data.getCash().getTotalValue());
                    break;
                default:
                    break;
            }
            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);
            payments.add(p);
        }
        return payments;
    }
}
