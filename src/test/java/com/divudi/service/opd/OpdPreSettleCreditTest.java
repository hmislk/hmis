package com.divudi.service.opd;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Institution;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import org.junit.Test;

public class OpdPreSettleCreditTest {

    @Test
    public void creditStoresCompanyAndReference() {
        PaymentMethodData data = new PaymentMethodData();
        Institution company = new Institution();
        data.getCredit().setInstitution(company);
        data.getCredit().setReferenceNo("R1");
        Bill bill = new Bill();
        OpdPreSettleCredit.apply(bill, PaymentMethod.Credit, data);
        assertSame(company, bill.getCreditCompany());
        assertEquals("R1", bill.getReferenceNumber());
    }

    @Test
    public void otherMethodsLeaveBillAlone() {
        PaymentMethodData data = new PaymentMethodData();
        data.getCredit().setInstitution(new Institution());
        Bill bill = new Bill();
        OpdPreSettleCredit.apply(bill, PaymentMethod.Cash, data);
        assertNull(bill.getCreditCompany());
        assertNull(bill.getReferenceNumber());
    }
}
