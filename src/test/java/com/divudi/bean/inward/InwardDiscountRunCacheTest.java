package com.divudi.bean.inward;

import com.divudi.bean.common.PriceMatrixController;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.EncounterCreditCompany;
import com.divudi.core.entity.Fee;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.data.FeeType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.EncounterCreditCompanyFacade;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Issue #17562: the discount percent and credit company are looked up once per
 * run and reused; the fee discounts must equal those of the per-fee lookups.
 */
class InwardDiscountRunCacheTest {

    private static class CountingMatrix extends PriceMatrixController {

        final AtomicInteger calls = new AtomicInteger();

        @Override
        public double getInwardDiscountPercent(PaymentMethod bhtType, PaymentScheme scheme,
                AdmissionType admissionType, Department department, Item item, Institution creditCompany) {
            calls.incrementAndGet();
            // a rule that depends on every part of the key
            double pct = 0;
            if (item != null) {
                pct += item.getId() % 7;
            }
            if (admissionType != null) {
                pct += admissionType.getId();
            }
            if (creditCompany != null) {
                pct += 10;
            }
            return pct;
        }
    }

    private static class CountingCreditFacade extends EncounterCreditCompanyFacade {

        final AtomicInteger calls = new AtomicInteger();
        final Institution company;

        CountingCreditFacade(Institution company) {
            this.company = company;
        }

        @Override
        public List<EncounterCreditCompany> findByJpql(String jpql, Map<String, Object> parameters, int maxRecords) {
            calls.incrementAndGet();
            List<EncounterCreditCompany> l = new ArrayList<>();
            if (company != null) {
                EncounterCreditCompany e = new EncounterCreditCompany();
                e.setInstitution(company);
                l.add(e);
            }
            return l;
        }
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = InwardBeanController.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Item item(long id) {
        Item i = new Item();
        i.setId(id);
        i.setDiscountAllowed(true);
        return i;
    }

    private static BillFee fee(Item item, double gross, double qty) {
        Fee f = new Fee();
        f.setFeeType(FeeType.OwnInstitution);
        f.setDiscountAllowed(true);
        BillFee bf = new BillFee();
        bf.setFee(f);
        BillItem bi = new BillItem();
        bi.setItem(item);
        bi.setQty(qty);
        bf.setBillItem(bi);
        bf.setFeeGrossValue(gross);
        bf.setFeeUnitGrossValue(gross / qty);
        return bf;
    }

    @Test
    void cachedRunGivesSameDiscountsWithFarFewerLookups() throws Exception {
        Institution cc = new Institution();
        cc.setId(5L);
        AdmissionType type = new AdmissionType();
        type.setId(3L);
        PatientEncounter enc = new PatientEncounter();
        enc.setPaymentMethod(PaymentMethod.Credit);

        // 3 items, 4 fees each, including a return-style negative quantity row
        Item[] items = {item(1), item(2), item(3)};
        List<BillFee[]> pairs = new ArrayList<>();
        for (int k = 0; k < 12; k++) {
            Item it = items[k % 3];
            double gross = (k % 4 == 3) ? -400.0 : 1000.0 + k;
            double qty = (k % 4 == 3) ? 1 : 2;
            pairs.add(new BillFee[]{fee(it, gross, qty), fee(it, gross, qty)});
        }

        CountingMatrix plainMatrix = new CountingMatrix();
        CountingCreditFacade plainCredit = new CountingCreditFacade(cc);
        InwardBeanController plain = new InwardBeanController();
        inject(plain, "priceMatrixController", plainMatrix);
        inject(plain, "encounterCreditCompanyFacade", plainCredit);

        CountingMatrix cachedMatrix = new CountingMatrix();
        CountingCreditFacade cachedCredit = new CountingCreditFacade(cc);
        InwardBeanController cached = new InwardBeanController();
        inject(cached, "priceMatrixController", cachedMatrix);
        inject(cached, "encounterCreditCompanyFacade", cachedCredit);
        InwardBeanController.DiscountRunCache cache = new InwardBeanController.DiscountRunCache();

        for (BillFee[] pair : pairs) {
            Item it = pair[0].getBillItem().getItem();
            plain.applyInwardDiscountToBillFee(pair[0], it, enc, type);
            cached.applyInwardDiscountToBillFee(pair[1], it, enc, type, cache);
            assertEquals(pair[0].getFeeDiscount(), pair[1].getFeeDiscount(), 0.0);
            assertEquals(pair[0].getFeeUnitDiscount(), pair[1].getFeeUnitDiscount(), 0.0);
        }
        assertEquals(12, plainMatrix.calls.get());
        assertEquals(3, cachedMatrix.calls.get());
        assertEquals(12, plainCredit.calls.get());
        assertEquals(1, cachedCredit.calls.get());
    }

    @Test
    void differentKeyPartsAreLookedUpSeparately() {
        Item a = item(1);
        Item b = item(2);
        AdmissionType t1 = new AdmissionType();
        t1.setId(1L);
        AdmissionType t2 = new AdmissionType();
        t2.setId(2L);
        InwardBeanController.DiscountRunCache cache = new InwardBeanController.DiscountRunCache();
        AtomicInteger loads = new AtomicInteger();
        cache.percent(InwardBeanController.discountPercentKey(PaymentMethod.Credit, null, t1, null, a, null), () -> loads.incrementAndGet());
        cache.percent(InwardBeanController.discountPercentKey(PaymentMethod.Credit, null, t1, null, a, null), () -> loads.incrementAndGet());
        cache.percent(InwardBeanController.discountPercentKey(PaymentMethod.Credit, null, t2, null, a, null), () -> loads.incrementAndGet());
        cache.percent(InwardBeanController.discountPercentKey(PaymentMethod.Credit, null, t1, null, b, null), () -> loads.incrementAndGet());
        assertEquals(3, loads.get());
    }

    @Test
    void zeroPercentIsRememberedToo() {
        InwardBeanController.DiscountRunCache cache = new InwardBeanController.DiscountRunCache();
        AtomicInteger loads = new AtomicInteger();
        List<Object> key = InwardBeanController.discountPercentKey(PaymentMethod.Cash, null, null, null, item(9), null);
        assertNotNull(key);
        cache.percent(key, () -> {
            loads.incrementAndGet();
            return 0.0;
        });
        assertEquals(0.0, cache.percent(key, () -> {
            loads.incrementAndGet();
            return 99.0;
        }), 0.0);
        assertEquals(1, loads.get());
    }

    @Test
    void unsavedEntityIsNeverMemoised() {
        Item unsaved = new Item();
        assertNull(InwardBeanController.discountPercentKey(PaymentMethod.Cash, null, null, null, unsaved, null));
        InwardBeanController.DiscountRunCache cache = new InwardBeanController.DiscountRunCache();
        AtomicInteger loads = new AtomicInteger();
        cache.percent(null, () -> loads.incrementAndGet());
        cache.percent(null, () -> loads.incrementAndGet());
        assertEquals(2, loads.get());
    }
}
