package com.divudi.service.pharmacy;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PharmacyBatchChangeServiceTest {

    private static class DummyBillFacade extends BillFacade {
        Bill saved;
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public void create(Bill entity) { entity.setId(500L); saved = entity; }
        @Override public Bill findWithoutCache(Object id) { return saved; }
    }

    private static class DummyBillItemFacade extends BillItemFacade {
        List<BillItem> saved = new ArrayList<>();
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public void create(BillItem entity) { saved.add(entity); }
    }

    private static class DummyStockFacade extends StockFacade {
        Stock source;
        Stock existingTarget;
        List<Stock> created = new ArrayList<>();
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public Stock findWithoutCache(Object id) { return source; }
        @Override public Stock findWithLock(Object id) { return Long.valueOf(800L).equals(id) ? source : existingTarget; }
        @Override public Stock findFirstByJpql(String jpql, Map<String, Object> parameters, boolean withoutCache) { return existingTarget; }
        @Override public void create(Stock entity) { entity.setId(900L); created.add(entity); }
    }

    private static class DummyItemBatchFacade extends ItemBatchFacade {
        ItemBatch target;
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public ItemBatch findWithoutCache(Object id) { return target; }
        @Override public ItemBatch findWithLock(Object id) { return target; }
    }

    private static class DummyBillNumberGenerator extends BillNumberGenerator {
        @Override
        public String departmentBillNumberGeneratorYearly(Department dep, BillTypeAtomic billType) { return "TEST/ADJ/0001"; }
    }

    private static class DummyPharmacyBean extends PharmacyBean {
        @Override
        public boolean deductFromStock(Stock stock, double qty, PharmaceuticalBillItem pbi, Department d) {
            if (stock.getStock() < qty) {
                return false;
            }
            stock.setStock(stock.getStock() - qty);
            return true;
        }
        @Override
        public boolean addToStock(Stock stock, double qty, PharmaceuticalBillItem pbi, Department d) {
            if (stock != pbi.getStock()) {
                return false;
            }
            stock.setStock(stock.getStock() + qty);
            return true;
        }
    }

    private static class DummyConfig extends com.divudi.bean.common.ConfigOptionApplicationController {
        @Override
        public Boolean getBooleanValueByKey(String key, boolean defaultValue) { return defaultValue; }
    }

    private PharmacyBatchChangeService service;
    private DummyBillItemFacade billItemFacade;
    private DummyStockFacade stockFacade;
    private DummyItemBatchFacade itemBatchFacade;
    private Department department;
    private WebUser user;
    private Item item;
    private ItemBatch sourceBatch;
    private ItemBatch targetBatch;
    private Stock sourceStock;

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private ItemBatch batch(long id, Item it, double purchase, double retail) {
        ItemBatch b = new ItemBatch();
        b.setId(id);
        b.setItem(it);
        b.setBatchNo("B" + id);
        b.setPurcahseRate(purchase);
        b.setRetailsaleRate(retail);
        b.setCostRate(purchase);
        return b;
    }

    @BeforeEach
    public void setUp() throws Exception {
        service = new PharmacyBatchChangeService();
        billItemFacade = new DummyBillItemFacade();
        stockFacade = new DummyStockFacade();
        itemBatchFacade = new DummyItemBatchFacade();
        inject(service, "billFacade", new DummyBillFacade());
        inject(service, "billItemFacade", billItemFacade);
        inject(service, "stockFacade", stockFacade);
        inject(service, "itemBatchFacade", itemBatchFacade);
        inject(service, "pharmacyBean", new DummyPharmacyBean());
        inject(service, "billNumberGenerator", new DummyBillNumberGenerator());
        inject(service, "configOptionApplicationController", new DummyConfig());

        department = new Department();
        department.setId(1L);
        department.setInstitution(new com.divudi.core.entity.Institution());
        user = new WebUser();

        item = new Item();
        item.setId(10L);
        sourceBatch = batch(100L, item, 60.0, 100.0);
        targetBatch = batch(101L, item, 70.0, 120.0);
        sourceStock = new Stock();
        sourceStock.setId(800L);
        sourceStock.setItemBatch(sourceBatch);
        sourceStock.setDepartment(department);
        sourceStock.setStock(10.0);
        stockFacade.source = sourceStock;
        itemBatchFacade.target = targetBatch;
    }

    @Test
    public void partialMoveToBatchWithNoStockRowCreatesRowAndTwoLines() {
        Bill bill = service.changeBatch(800L, 101L, 4.0, "count correction", user, department);

        assertEquals(BillType.PharmacyAdjustmentBatchChange, bill.getBillType());
        assertEquals(BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT, bill.getBillTypeAtomic());
        assertEquals(1, stockFacade.created.size(), "department had no row for the target batch");
        assertEquals(6.0, sourceStock.getStock(), 0.0001);
        assertEquals(4.0, stockFacade.created.get(0).getStock(), 0.0001);

        assertEquals(2, billItemFacade.saved.size());
        PharmaceuticalBillItem from = billItemFacade.saved.get(0).getPharmaceuticalBillItem();
        PharmaceuticalBillItem to = billItemFacade.saved.get(1).getPharmaceuticalBillItem();
        assertEquals(-4.0, from.getQty(), 0.0001);
        assertEquals(10.0, from.getBeforeAdjustmentValue(), 0.0001);
        assertEquals(6.0, from.getAfterAdjustmentValue(), 0.0001);
        assertSame(sourceBatch, from.getItemBatch());
        assertEquals(4.0, to.getQty(), 0.0001);
        assertEquals(0.0, to.getBeforeAdjustmentValue(), 0.0001);
        assertEquals(4.0, to.getAfterAdjustmentValue(), 0.0001);
        assertSame(targetBatch, to.getItemBatch());

        // value difference = 4 * (target rate - source rate)
        assertEquals(0, new BigDecimal("80").compareTo(bill.getBillFinanceDetails().getTotalRetailSaleValue()));
        assertEquals(0, new BigDecimal("40").compareTo(bill.getBillFinanceDetails().getTotalPurchaseValue()));
        assertEquals(80.0, bill.getNetTotal(), 0.0001);
    }

    @Test
    public void fullMoveEmptiesSourceAndAddsToExistingTargetRow() {
        Stock existing = new Stock();
        existing.setId(901L);
        existing.setItemBatch(targetBatch);
        existing.setDepartment(department);
        existing.setStock(3.0);
        stockFacade.existingTarget = existing;

        service.changeBatch(800L, 101L, 10.0, "move all", user, department);

        assertEquals(0, stockFacade.created.size());
        assertEquals(0.0, sourceStock.getStock(), 0.0001);
        assertEquals(13.0, existing.getStock(), 0.0001);
    }

    @Test
    public void rejectsBadRequests() {
        assertThrows(PharmacyBatchChangeException.class, () -> service.changeBatch(800L, 101L, 0, "x", user, department));
        assertThrows(PharmacyBatchChangeException.class, () -> service.changeBatch(800L, 101L, 11.0, "x", user, department));
        assertThrows(PharmacyBatchChangeException.class, () -> service.changeBatch(800L, 101L, 2.0, "  ", user, department));
        assertThrows(PharmacyBatchChangeException.class, () -> service.changeBatch(800L, 101L, 2.5, "x", user, department), "fractions not allowed");
        itemBatchFacade.target = sourceBatch;
        assertThrows(PharmacyBatchChangeException.class, () -> service.changeBatch(800L, 100L, 2.0, "x", user, department), "same batch");
        Item other = new Item();
        other.setId(11L);
        itemBatchFacade.target = batch(102L, other, 1.0, 2.0);
        assertThrows(PharmacyBatchChangeException.class, () -> service.changeBatch(800L, 102L, 2.0, "x", user, department), "other item");
        itemBatchFacade.target = targetBatch;
        Department otherDept = new Department();
        otherDept.setId(2L);
        assertThrows(PharmacyBatchChangeException.class, () -> service.changeBatch(800L, 101L, 2.0, "x", user, otherDept), "other department");
        assertEquals(10.0, sourceStock.getStock(), 0.0001, "nothing moved");
    }
}
