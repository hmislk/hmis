package com.divudi.service.pharmacy;

import com.divudi.core.data.dto.adjustment.StockQuantityAdjustmentDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PharmacyAdjustmentApiServiceTest {

    private static class DummyBillFacade extends BillFacade {
        List<Bill> saved = new ArrayList<>();
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public void create(Bill entity) { saved.add(entity); }
        @Override public void edit(Bill entity) { /* no-op: entity already mutated in-memory */ }
    }

    private static class DummyBillItemFacade extends BillItemFacade {
        List<BillItem> saved = new ArrayList<>();
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public void create(BillItem entity) { saved.add(entity); }
    }

    private static class DummyStockFacade extends StockFacade {
        Stock stock;
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public Stock find(Object id) { return stock; }
    }

    private static class DummyDepartmentFacade extends DepartmentFacade {
        Department department;
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public Department find(Object id) { return department; }
    }

    private static class DummyItemBatchFacade extends ItemBatchFacade {
        @Override protected EntityManager getEntityManager() { return null; }
        @Override public void edit(ItemBatch entity) { /* no-op */ }
    }

    private static class DummyBillNumberGenerator extends BillNumberGenerator {
        @Override
        public String departmentBillNumberGeneratorYearly(Department dep, com.divudi.core.data.BillTypeAtomic billType) {
            return "TEST/ADJ/0001";
        }
    }

    private static class DummyPharmacyBean extends PharmacyBean {
        @Override
        public boolean resetStock(com.divudi.core.entity.pharmacy.PharmaceuticalBillItem ph, Stock stock, double qty, Department department) {
            stock.setStock(qty);
            return true;
        }
        @Override
        public void addToStockHistory(com.divudi.core.entity.pharmacy.PharmaceuticalBillItem phItem, Stock stock, Department d) {
            /* no-op */
        }
    }

    private PharmacyAdjustmentApiService service;
    private DummyBillFacade billFacade;
    private DummyBillItemFacade billItemFacade;
    private DummyStockFacade stockFacade;
    private DummyDepartmentFacade departmentFacade;
    private Stock stock;
    private WebUser user;

    @BeforeEach
    public void setUp() throws Exception {
        service = new PharmacyAdjustmentApiService();

        billFacade = new DummyBillFacade();
        billItemFacade = new DummyBillItemFacade();
        stockFacade = new DummyStockFacade();
        departmentFacade = new DummyDepartmentFacade();

        ItemBatch itemBatch = new ItemBatch();
        itemBatch.setItem(new Item());
        itemBatch.setRetailsaleRate(100.0);
        itemBatch.setPurcahseRate(60.0);
        itemBatch.setCostRate(55.0);

        stock = new Stock();
        stock.setItemBatch(itemBatch);
        stock.setStock(10.0);
        stockFacade.stock = stock;

        Department department = new Department();
        department.setInstitution(new com.divudi.core.entity.Institution());
        departmentFacade.department = department;

        user = new WebUser();

        setField("billFacade", billFacade);
        setField("billItemFacade", billItemFacade);
        setField("stockFacade", stockFacade);
        setField("departmentFacade", departmentFacade);
        setField("itemBatchFacade", new DummyItemBatchFacade());
        setField("billNumberGenerator", new DummyBillNumberGenerator());
        setField("pharmacyBean", new DummyPharmacyBean());
        setField("configOptionApplicationController", new com.divudi.bean.common.ConfigOptionApplicationController());
    }

    private void setField(String name, Object value) throws Exception {
        Field f = PharmacyAdjustmentApiService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    @Test
    @DisplayName("Stock quantity adjustment records the delta value, not the ending value, on the bill")
    public void testQuantityAdjustmentUsesDeltaValue() throws Exception {
        StockQuantityAdjustmentDTO request = new StockQuantityAdjustmentDTO();
        request.setStockId(1L);
        request.setNewQuantity(25.0); // was 10 -> +15 units
        request.setComment("test correction");
        request.setDepartmentId(1L);

        service.adjustStockQuantity(request, user);

        assertEquals(1, billFacade.saved.size());
        Bill bill = billFacade.saved.get(0);

        // delta = 15 units * 100.0 retail rate = 1500.0, NOT 25 * 100.0 = 2500.0
        assertEquals(1500.0, bill.getNetTotal(), 0.001, "Bill.netTotal must reflect the quantity delta, not the ending balance");
        assertEquals(1500.0, bill.getTotal(), 0.001);

        assertNotNull(bill.getBillFinanceDetails(), "BillFinanceDetails must be created so the F15 report can read it");
        assertEquals(0, java.math.BigDecimal.valueOf(1500.0).compareTo(bill.getBillFinanceDetails().getTotalRetailSaleValue()));
        assertEquals(0, java.math.BigDecimal.valueOf(15.0 * 55.0).compareTo(bill.getBillFinanceDetails().getTotalCostValue()));
    }

    @Test
    @DisplayName("Stock quantity decrease produces a negative delta value")
    public void testQuantityDecreaseIsNegative() throws Exception {
        StockQuantityAdjustmentDTO request = new StockQuantityAdjustmentDTO();
        request.setStockId(1L);
        request.setNewQuantity(4.0); // was 10 -> -6 units
        request.setComment("shrinkage correction");
        request.setDepartmentId(1L);

        service.adjustStockQuantity(request, user);

        Bill bill = billFacade.saved.get(0);
        assertEquals(-600.0, bill.getNetTotal(), 0.001);
        assertEquals(600.0, bill.getTotal(), 0.001, "Bill.total (gross) should be the absolute value of the change");
    }

    @Test
    @DisplayName("Retail rate adjustment records the rate-change value across current stock qty")
    public void testRetailRateAdjustmentPopulatesFinanceDetails() throws Exception {
        com.divudi.core.data.dto.adjustment.RetailRateAdjustmentDTO request =
                new com.divudi.core.data.dto.adjustment.RetailRateAdjustmentDTO();
        request.setStockId(1L);
        request.setNewRetailRate(120.0); // was 100.0, stock qty = 10 -> change = 10 * 20 = 200.0
        request.setComment("rate correction");
        request.setDepartmentId(1L);

        service.adjustRetailRate(request, user);

        assertEquals(1, billFacade.saved.size());
        Bill bill = billFacade.saved.get(0);
        assertEquals(200.0, bill.getNetTotal(), 0.001);
        assertEquals(200.0, bill.getTotal(), 0.001);
        assertNotNull(bill.getBillFinanceDetails());
        assertEquals(0, java.math.BigDecimal.valueOf(200.0).compareTo(bill.getBillFinanceDetails().getTotalRetailSaleValue()));
        assertEquals(0, java.math.BigDecimal.ZERO.compareTo(bill.getBillFinanceDetails().getTotalCostValue()));
    }

    @Test
    @DisplayName("Purchase rate adjustment records the rate-change value in totalPurchaseValue")
    public void testPurchaseRateAdjustmentPopulatesFinanceDetails() throws Exception {
        com.divudi.core.data.dto.adjustment.PurchaseRateAdjustmentDTO request =
                new com.divudi.core.data.dto.adjustment.PurchaseRateAdjustmentDTO();
        request.setStockId(1L);
        request.setNewPurchaseRate(70.0); // was 60.0, stock qty = 10 -> change = 10 * 10 = 100.0
        request.setComment("purchase rate correction");
        request.setDepartmentId(1L);

        service.adjustPurchaseRate(request, user);

        assertEquals(1, billFacade.saved.size());
        Bill bill = billFacade.saved.get(0);
        assertEquals(100.0, bill.getNetTotal(), 0.001);
        assertEquals(100.0, bill.getTotal(), 0.001);
        assertNotNull(bill.getBillFinanceDetails());
        assertEquals(0, java.math.BigDecimal.valueOf(100.0).compareTo(bill.getBillFinanceDetails().getTotalPurchaseValue()));
        assertEquals(0, java.math.BigDecimal.ZERO.compareTo(bill.getBillFinanceDetails().getTotalRetailSaleValue()));
    }
}
