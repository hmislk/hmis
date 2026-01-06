package com.divudi.ejb;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.facade.StockHistoryFacade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Singleton;

@Singleton
public class OptimizedPharmacyBean {

    @EJB
    private StockFacade stockFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private StockHistoryFacade stockHistoryFacade;
    @EJB
    private PharmacyBean pharmacyBean;

    public boolean deductFromStockOptimized(Stock stock, double qty, PharmaceuticalBillItem pbi, Department d) {
        if (stock == null || stock.getId() == null) {
            return false;
        }
        Stock s = stockFacade.findWithoutCache(stock.getId());
        if (s == null || s.getStock() < qty) {
            return false;
        }
        s.setStock(s.getStock() - qty);
        stockFacade.edit(s);
        if (pharmacyBean != null && pbi != null) {
            pharmacyBean.addToStockHistory(pbi, s, d);
        }
        return true;
    }

    public boolean deductFromStockBatch(List<StockDeductionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return true;
        }
        boolean valid = true;
        List<Stock> toEdit = new ArrayList<>();
        for (StockDeductionRequest r : requests) {
            if (r == null || r.getStock() == null || r.getStock().getId() == null) {
                valid = false;
                continue;
            }
            Stock s = stockFacade.findWithoutCache(r.getStock().getId());
            if (s == null || s.getStock() < r.getQty()) {
                valid = false;
                continue;
            }
            s.setStock(s.getStock() - r.getQty());
            toEdit.add(s);
            if (pharmacyBean != null && r.getBillItem() != null) {
                pharmacyBean.addToStockHistory(r.getBillItem(), s, r.getDepartment());
            }
        }
        if (!toEdit.isEmpty()) {
            stockFacade.batchEdit(toEdit);
        }
        return valid;
    }

    public StockQuantities calculateStockQuantitiesBatch(PharmaceuticalBillItem phItem, Department dept) {
        if (phItem == null || phItem.getBillItem() == null || dept == null) {
            return new StockQuantities();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("dept", dept);
        params.put("inst", dept.getInstitution());
        params.put("item", phItem.getBillItem().getItem());
        String jpql = "SELECT SUM(CASE WHEN s.department = :dept THEN s.stock ELSE 0 END), "
                + "SUM(CASE WHEN s.department.institution = :inst THEN s.stock ELSE 0 END), "
                + "SUM(s.stock) FROM Stock s WHERE s.itemBatch.item = :item";

        List<Object[]> res = stockFacade.findObjectsArrayByJpql(jpql, params, null);
        double deptStock = 0;
        double instStock = 0;
        double totalStock = 0;
        if (res != null && !res.isEmpty()) {
            Object[] row = res.get(0);
            deptStock = row[0] == null ? 0 : ((Number) row[0]).doubleValue();
            instStock = row[1] == null ? 0 : ((Number) row[1]).doubleValue();
            totalStock = row[2] == null ? 0 : ((Number) row[2]).doubleValue();
        }
        return new StockQuantities(deptStock, instStock, totalStock);
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public static class StockDeductionRequest {

        private Stock stock;
        private double qty;
        private PharmaceuticalBillItem billItem;
        private Department department;

        public StockDeductionRequest() {
        }

        public StockDeductionRequest(Stock stock, double qty, PharmaceuticalBillItem billItem, Department department) {
            this.stock = stock;
            this.qty = qty;
            this.billItem = billItem;
            this.department = department;
        }

        public Stock getStock() {
            return stock;
        }

        public void setStock(Stock stock) {
            this.stock = stock;
        }

        public double getQty() {
            return qty;
        }

        public void setQty(double qty) {
            this.qty = qty;
        }

        public PharmaceuticalBillItem getBillItem() {
            return billItem;
        }

        public void setBillItem(PharmaceuticalBillItem billItem) {
            this.billItem = billItem;
        }

        public Department getDepartment() {
            return department;
        }

        public void setDepartment(Department department) {
            this.department = department;
        }
    }

    public static class StockQuantities {
        private double departmentStock;
        private double institutionStock;
        private double totalStock;

        public StockQuantities() {
        }

        public StockQuantities(double departmentStock, double institutionStock, double totalStock) {
            this.departmentStock = departmentStock;
            this.institutionStock = institutionStock;
            this.totalStock = totalStock;
        }

        public double getDepartmentStock() {
            return departmentStock;
        }

        public void setDepartmentStock(double departmentStock) {
            this.departmentStock = departmentStock;
        }

        public double getInstitutionStock() {
            return institutionStock;
        }

        public void setInstitutionStock(double institutionStock) {
            this.institutionStock = institutionStock;
        }

        public double getTotalStock() {
            return totalStock;
        }

        public void setTotalStock(double totalStock) {
            this.totalStock = totalStock;
        }
    }
}
