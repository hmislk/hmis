package com.divudi.service;

import com.divudi.bean.common.SessionController;
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
    StaffBean staffBean;
    @EJB
    CashbookService cashbookService;

    public List<Payment> createPayment(Bill bill, PaymentMethod pm, PaymentMethodData paymentMethodData, Department department, WebUser webUser, Patient patient) {
        CashBook cashbook = cashbookService.findAndSaveCashBookBySite(department.getSite(), department.getInstitution(), department);
        List<Payment> ps = new ArrayList<>();
        if (pm == PaymentMethod.MultiplePaymentMethods) {
            for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple().getMultiplePaymentMethodComponentDetails()) {
                Payment p = new Payment();
                p.setBill(bill);
                p.setInstitution(department.getInstitution());
                p.setDepartment(department);
                p.setCreatedAt(new Date());
                p.setCreater(webUser);
                p.setPaymentMethod(cd.getPaymentMethod());

                switch (cd.getPaymentMethod()) {
                    case Card:
                        p.setBank(cd.getPaymentMethodData().getCreditCard().getInstitution());
                        p.setCreditCardRefNo(cd.getPaymentMethodData().getCreditCard().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCreditCard().getTotalValue());
                        break;
                    case Cheque:
                        p.setBank(cd.getPaymentMethodData().getCheque().getInstitution());
                        p.setChequeDate(cd.getPaymentMethodData().getCheque().getDate());
                        p.setChequeRefNo(cd.getPaymentMethodData().getCheque().getNo());
                        p.setPaidValue(cd.getPaymentMethodData().getCheque().getTotalValue());
                        break;
                    case Cash:
                        p.setPaidValue(cd.getPaymentMethodData().getCash().getTotalValue());
                        break;
                    case ewallet:

                    case Agent:
                    case Credit:
                        p.setReferenceNo(cd.getPaymentMethodData().getCredit().getReferralNo());
                        p.setComments(cd.getPaymentMethodData().getCredit().getComment());
                    case PatientDeposit:
                        if (patient.getRunningBalance() != null) {
                            patient.setRunningBalance(patient.getRunningBalance() - cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        } else {
                            patient.setRunningBalance(0.0 - cd.getPaymentMethodData().getPatient_deposit().getTotalValue());
                        }
                        patientFacade.edit(patient);
                    case Slip:
                        p.setPaidValue(cd.getPaymentMethodData().getSlip().getTotalValue());
                        p.setBank(cd.getPaymentMethodData().getSlip().getInstitution());
                        p.setRealizedAt(cd.getPaymentMethodData().getSlip().getDate());
                    case OnCall:
                    case OnlineSettlement:
                    case Staff:
                        p.setPaidValue(cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                        if (cd.getPaymentMethodData().getStaffCredit().getToStaff() != null) {
                            staffBean.updateStaffCredit(cd.getPaymentMethodData().getStaffCredit().getToStaff(), cd.getPaymentMethodData().getStaffCredit().getTotalValue());
                            JsfUtil.addSuccessMessage("Staff Welfare Balance Updated");
                        }
                    case YouOweMe:
                    case MultiplePaymentMethods:
                }

                paymentFacade.create(p);
                cashbookService.writeCashBookEntryAtPaymentCreation(p, webUser, cashbook, department);
                ps.add(p);
            }
        } else {
            Payment p = new Payment();
            p.setBill(bill);
            p.setInstitution(department.getInstitution());
            p.setDepartment(department);
            p.setCreatedAt(new Date());
            p.setCreater(webUser);
            p.setPaymentMethod(pm);

            switch (pm) {
                case Card:
                    p.setBank(paymentMethodData.getCreditCard().getInstitution());
                    p.setCreditCardRefNo(paymentMethodData.getCreditCard().getNo());
                    p.setPaidValue(paymentMethodData.getCreditCard().getTotalValue());
                    break;
                case Cheque:
                    p.setBank(paymentMethodData.getCheque().getInstitution());
                    p.setChequeDate(paymentMethodData.getCheque().getDate());
                    p.setChequeRefNo(paymentMethodData.getCheque().getNo());
                    p.setPaidValue(paymentMethodData.getCheque().getTotalValue());
                    break;
                case Cash:
                    p.setPaidValue(paymentMethodData.getCash().getTotalValue());
                    break;
                case ewallet:

                case Agent:
                case Credit:
                    p.setReferenceNo(paymentMethodData.getCredit().getReferralNo());
                    p.setComments(paymentMethodData.getCredit().getComment());
                case PatientDeposit:
                case Slip:
                    p.setBank(paymentMethodData.getSlip().getInstitution());
                    p.setPaidValue(paymentMethodData.getSlip().getTotalValue());
                    p.setRealizedAt(paymentMethodData.getSlip().getDate());
                case OnCall:
                case OnlineSettlement:
                case Staff:
                case YouOweMe:
                case MultiplePaymentMethods:
            }

            p.setPaidValue(p.getBill().getNetTotal());
            paymentFacade.create(p);
            ps.add(p);
        }
        return ps;
    }

}
