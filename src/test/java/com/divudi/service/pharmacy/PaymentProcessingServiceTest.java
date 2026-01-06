import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.*;
import com.divudi.service.pharmacy.PaymentProcessingService;
import com.divudi.core.facade.PaymentFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PaymentProcessingServiceTest {

    private static class DummyPaymentFacade extends PaymentFacade {
        List<Payment> created = new ArrayList<>();
        @Override
        protected EntityManager getEntityManager() { return null; }
        @Override
        public void create(Payment entity) { created.add(entity); }
    }

    private PaymentProcessingService service;
    private DummyPaymentFacade facade;
    private Bill bill;
    private Institution institution;
    private Department department;
    private WebUser user;

    @BeforeEach
    public void setUp() throws Exception {
        service = new PaymentProcessingService();
        facade = new DummyPaymentFacade();
        Field f = PaymentProcessingService.class.getDeclaredField("paymentFacade");
        f.setAccessible(true);
        f.set(service, facade);

        bill = new Bill();
        bill.setNetTotal(100.0);
        institution = new Institution();
        department = new Department();
        user = new WebUser();
    }

    @Test
    @DisplayName("Cash payment uses value from PaymentMethodData")
    public void testCashPaymentPaidValue() {
        PaymentMethodData data = new PaymentMethodData();
        data.getCash().setTotalValue(80.0);

        List<Payment> res = service.createPaymentsForBill(bill, PaymentMethod.Cash, data, institution, department, user);
        assertEquals(1, res.size());
        Payment p = res.get(0);
        assertEquals(80.0, p.getPaidValue(), 0.001);
    }

    @Test
    @DisplayName("Card payment populates fields correctly")
    public void testCardPayment() {
        PaymentMethodData data = new PaymentMethodData();
        Institution bank = new Institution();
        data.getCreditCard().setInstitution(bank);
        data.getCreditCard().setNo("CARD1");
        data.getCreditCard().setTotalValue(55.0);

        Payment p = service.createPaymentsForBill(bill, PaymentMethod.Card, data, institution, department, user).get(0);
        assertEquals(bank, p.getBank());
        assertEquals("CARD1", p.getCreditCardRefNo());
        assertEquals(55.0, p.getPaidValue(), 0.001);
    }

    @Test
    @DisplayName("Cheque payment populates fields correctly")
    public void testChequePayment() {
        PaymentMethodData data = new PaymentMethodData();
        Date d = new Date();
        data.getCheque().setDate(d);
        data.getCheque().setNo("CHQ1");
        data.getCheque().setTotalValue(40.0);

        Payment p = service.createPaymentsForBill(bill, PaymentMethod.Cheque, data, institution, department, user).get(0);
        assertEquals(d, p.getChequeDate());
        assertEquals("CHQ1", p.getChequeRefNo());
        assertEquals(40.0, p.getPaidValue(), 0.001);
    }

    @Test
    @DisplayName("Slip payment populates fields correctly")
    public void testSlipPayment() {
        PaymentMethodData data = new PaymentMethodData();
        Institution bank = new Institution();
        Date d = new Date();
        data.getSlip().setInstitution(bank);
        data.getSlip().setReferenceNo("SL1");
        data.getSlip().setComment("ok");
        data.getSlip().setDate(d);
        data.getSlip().setTotalValue(25.0);

        Payment p = service.createPaymentsForBill(bill, PaymentMethod.Slip, data, institution, department, user).get(0);
        assertEquals(bank, p.getBank());
        assertEquals("SL1", p.getReferenceNo());
        assertEquals(d, p.getChequeDate());
        assertEquals(25.0, p.getPaidValue(), 0.001);
    }

    @Test
    @DisplayName("Credit payment populates fields correctly")
    public void testCreditPayment() {
        PaymentMethodData data = new PaymentMethodData();
        Institution company = new Institution();
        data.getCredit().setInstitution(company);
        data.getCredit().setReferenceNo("CR1");
        data.getCredit().setReferralNo("POL");
        data.getCredit().setComment("cmt");
        data.getCredit().setTotalValue(90.0);

        Payment p = service.createPaymentsForBill(bill, PaymentMethod.Credit, data, institution, department, user).get(0);
        assertEquals(company, p.getCreditCompany());
        assertEquals("CR1", p.getReferenceNo());
        assertEquals("POL", p.getPolicyNo());
        assertEquals(90.0, p.getPaidValue(), 0.001);
    }

    @Test
    @DisplayName("Patient deposit payment uses value from PaymentMethodData")
    public void testPatientDepositPayment() {
        PaymentMethodData data = new PaymentMethodData();
        data.getPatient_deposit().setTotalValue(30.0);

        Payment p = service.createPaymentsForBill(bill, PaymentMethod.PatientDeposit, data, institution, department, user).get(0);
        assertEquals(30.0, p.getPaidValue(), 0.001);
    }

    @Test
    @DisplayName("Staff payment populates fields correctly")
    public void testStaffPayment() {
        PaymentMethodData data = new PaymentMethodData();
        data.getStaffCredit().setComment("stf");
        data.getStaffCredit().setTotalValue(12.0);

        Payment p = service.createPaymentsForBill(bill, PaymentMethod.Staff, data, institution, department, user).get(0);
        assertEquals("stf", p.getComments());
        assertEquals(12.0, p.getPaidValue(), 0.001);
    }

    @Test
    @DisplayName("Online settlement payment populates fields correctly")
    public void testOnlineSettlementPayment() {
        PaymentMethodData data = new PaymentMethodData();
        data.getOnlineSettlement().setComment("onl");
        data.getOnlineSettlement().setTotalValue(70.0);

        Payment p = service.createPaymentsForBill(bill, PaymentMethod.OnlineSettlement, data, institution, department, user).get(0);
        assertEquals("onl", p.getComments());
        assertEquals(70.0, p.getPaidValue(), 0.001);
    }

    @Test
    @DisplayName("IOU payment populates fields correctly")
    public void testIouPayment() {
        PaymentMethodData data = new PaymentMethodData();
        Date d = new Date();
        Staff staff = new Staff();
        data.getIou().setReferenceNo("IOU1");
        data.getIou().setDate(d);
        data.getIou().setToStaff(staff);
        data.getIou().setComment("iou");

        Payment p = service.createPaymentsForBill(bill, PaymentMethod.IOU, data, institution, department, user).get(0);
        assertEquals("IOU1", p.getReferenceNo());
        assertEquals(d, p.getChequeDate());
        assertEquals(staff, p.getToStaff());
        assertEquals("iou", p.getComments());
    }
}
