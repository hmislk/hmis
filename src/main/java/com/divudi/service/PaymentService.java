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
import static com.divudi.data.PaymentMethod.ewallet;
import com.divudi.data.dataStructure.ComponentDetail;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.entity.Bill;
import com.divudi.entity.Department;
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
    @EJB
    DrawerService drawerService;

    /**
     * Creates payments for the given bill and updates relevant records.
     *
     * <p>
     * This method performs the following tasks:
     * <ul>
     * <li>Creates payments based on the provided bill and payment method
     * data.</li>
     * <li>Updates the balance of patient deposits, if applicable.</li>
     * <li>Adjusts balances for credit companies, if required.</li>
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
    
    public List<Payment> createPayment(Bill bill, PaymentMethod pm, PaymentMethodData paymentMethodData, Department department, WebUser webUser) {
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
                
                break;
            case Slip:
                payment.setPaidValue(paymentMethodData.getSlip().getTotalValue());
                payment.setComments(paymentMethodData.getCreditCard().getComment());
                payment.setBank(paymentMethodData.getSlip().getInstitution());
                payment.setReferenceNo(paymentMethodData.getCredit().getReferenceNo());
                payment.setRealizedAt(paymentMethodData.getSlip().getDate());
                
                break;
            case OnCall:
            case OnlineSettlement:
            case Staff:
                payment.setPaidValue(paymentMethodData.getStaffCredit().getTotalValue());
                payment.setComments(paymentMethodData.getCreditCard().getComment());
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
