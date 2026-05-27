/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.HistoryType;
import com.divudi.ejb.StockHistoryRecorder;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.pharmacy.StockHistory;
import com.divudi.core.entity.pharmacy.StockHistoryArchive;
import com.divudi.core.data.dto.PharmacyBinCardDTO;
import com.divudi.core.facade.StockHistoryArchiveFacade;
import com.divudi.core.facade.StockHistoryFacade;
import java.util.ArrayList;
import java.util.Comparator;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.Item;
import com.divudi.core.util.CommonFunctions;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author buddhika
 */
@Named
@SessionScoped
public class StockHistoryController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private StockHistoryFacade facade;
    @EJB
    private StockHistoryArchiveFacade archiveFacade;
    @EJB
    StockHistoryRecorder stockHistoryRecorder;
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Controllers">

    @Inject
    SessionController sessionController;
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private StockHistory current;
    private List<StockHistory> pharmacyStockHistories;
    private List<Date> pharmacyStockHistoryDays;
    private Date fromDate;
    private Date toDate;
    private Date historyDate;
    private Department department;
    private DepartmentType departmentType;

    private double totalStockSaleValue;
    private double totalStockPurchaseValue;
    private boolean includeArchived = false;
    private List<StockHistoryArchive> pharmacyStockHistoriesArchive;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    public StockHistoryController() {
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigation Methods">
    public String navigateToViewStockHistory() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }
        return "/analytics/pharmacy/stock_history?faces-redirect=true";
    }

    public String navigateToViewStockHistory(StockHistory stockHistory) {
        if (stockHistory == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }
        current = stockHistory;
        return "/analytics/pharmacy/stock_history?faces-redirect=true";
    }

    /**
     * Helper navigation method when only the id of the stock history is
     * available. This is used when bin card rows are represented by DTOs.
     */
    public String navigateToViewStockHistoryById(Long id) {
        if (id == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }
        current = facade.find(id);
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing found");
            return null;
        }
        return "/analytics/pharmacy/stock_history?faces-redirect=true";
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Functions">
    public void fillHistoryAvailableDays() {
        String jpql;
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ht", HistoryType.MonthlyRecord);
//        jpql = "select FUNC('Date',s.stockAt) from StockHistory s where s.historyType=:ht and s.stockAt between :fd and :td group by FUNC('Date',s.stockAt)";
        jpql = "select distinct(s.createdAt) from StockHistory s "
                + " where s.historyType=:ht"
                + " and s.createdAt between :fd and :td "
                + " order by s.id desc ";

//        List<StockHistory> historys=facade.findByJpql(jpql, m,TemporalType.TIMESTAMP);
//        for (StockHistory history : historys) {
//            //// // System.out.println("history.getStockAt() = " + history.getStockAt());
//            //// // System.out.println("history.getStockAt() = " + history.getCreatedAt());
//        }
        ////// // System.out.println("m = " + m);
        pharmacyStockHistoryDays = facade.findDateListByJpql(jpql, m, TemporalType.TIMESTAMP);
        if (includeArchived) {
            String archiveJpql = jpql.replace("from StockHistory s", "from StockHistoryArchive s");
            List<Date> archiveDays = archiveFacade.findDateListByJpql(archiveJpql, m, TemporalType.TIMESTAMP);
            List<Date> merged = new ArrayList<>(pharmacyStockHistoryDays);
            for (Date d : archiveDays) {
                if (!merged.contains(d)) {
                    merged.add(d);
                }
            }
            merged.sort(Comparator.reverseOrder());
            pharmacyStockHistoryDays = merged;
        }

    }

    public List<StockHistory> findStockHistories(Date fd, Date td, HistoryType ht, Department dep, Item i) {
        String jpql;
        Map m = new HashMap();
        m.put("fd", fd);
        m.put("td", td);

        jpql = "select s"
                + " from StockHistory s "
                + " where s.createdAt between :fd and :td ";
        if (ht != null) {
            jpql += " and s.historyType=:ht ";
            m.put("ht", ht);
        }
        if (dep != null) {
            jpql += " and s.department=:dep ";
            m.put("dep", dep);
        }
        if (i != null) {
            jpql += " and s.item=:i ";
            m.put("i", i);
        }

        jpql += " order by s.createdAt ";
        List<StockHistory> shxs = facade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        if (shxs != null) {
        }
        return shxs;
    }

    /**
     * Returns lightweight DTOs for the bin card view to reduce memory usage.
     * This method includes the stockQty and batchNo fields needed for hotfix compatibility.
     */
    public List<PharmacyBinCardDTO> findBinCardDTOs(Date fd, Date td, HistoryType ht, Department dep, Item i) {
        return findBinCardDTOs(fd, td, ht, dep, i, false);
    }

    public List<PharmacyBinCardDTO> findBinCardDTOs(Date fd, Date td, HistoryType ht, Department dep, Item i, boolean withArchive) {
        String baseJpql = buildBinCardJpql("StockHistory");
        Map<String, Object> m = buildBinCardParams(fd, td, ht, dep, i);
        String liveJpql = applyBinCardFilters(baseJpql, ht, dep, i);
        List<PharmacyBinCardDTO> results = new ArrayList<>(
                (List<PharmacyBinCardDTO>) facade.findLightsByJpql(liveJpql, m, TemporalType.TIMESTAMP));
        if (withArchive) {
            String archiveJpql = applyBinCardFilters(buildBinCardJpql("StockHistoryArchive"), ht, dep, i);
            results.addAll((List<PharmacyBinCardDTO>) archiveFacade.findLightsByJpql(archiveJpql, m, TemporalType.TIMESTAMP));
            results.sort(Comparator.comparing(PharmacyBinCardDTO::getCreatedAt));
        }
        return results;
    }

    private String buildBinCardJpql(String entity) {
        return "select new com.divudi.core.data.dto.PharmacyBinCardDTO("
                + "s.id, s.pbItem.billItem.bill.id, s.createdAt, "
                + "s.pbItem.billItem.bill.billType, "
                + "s.pbItem.billItem.bill.billTypeAtomic, "
                + "s.pbItem.billItem.item.name, "
                + "s.pbItem.qty, s.pbItem.freeQty, "
                + "s.pbItem.qtyPacks, s.pbItem.freeQtyPacks, "
                + "s.pbItem.billItem.item.dblValue, s.itemStock, "
                + "s.stockQty, s.pbItem.itemBatch.batchNo"
                + ") from " + entity + " s "
                + "where s.createdAt between :fd and :td "
                + "and s.pbItem is not null "
                + "and s.pbItem.billItem is not null "
                + "and s.pbItem.billItem.bill is not null "
                + "and s.pbItem.billItem.item is not null "
                + "and s.pbItem.itemBatch is not null ";
    }

    private Map<String, Object> buildBinCardParams(Date fd, Date td, HistoryType ht, Department dep, Item i) {
        Map<String, Object> m = new HashMap<>();
        m.put("fd", fd);
        m.put("td", td);
        if (ht != null) m.put("ht", ht);
        if (dep != null) m.put("dep", dep);
        if (i != null) m.put("i", i);
        return m;
    }

    private String applyBinCardFilters(String jpql, HistoryType ht, Department dep, Item i) {
        if (ht != null) jpql += " and s.historyType=:ht ";
        if (dep != null) jpql += " and s.department=:dep ";
        if (i != null) jpql += " and s.item=:i ";
        jpql += " order by s.createdAt";
        return jpql;
    }

    public void fillStockHistories(boolean withoutZeroStock) {
        Date startTime = new Date();

        String jpql;
        Map m = new HashMap();
        m.put("hd", historyDate);
        m.put("ht", HistoryType.MonthlyRecord);
//        if (department == null) {
//            jpql = "select s from StockHistory s where s.historyType=:ht and s.stockAt =:hd order by s.item.name";
//        } else {
//            m.put("d", department);
//            jpql = "select s from StockHistory s where s.historyType=:ht and s.department=:d and s.stockAt =:hd order by s.item.name";
//        }
        jpql = "select s from StockHistory s "
                + " where s.historyType=:ht "
                + " and s.createdAt=:hd ";

        if (withoutZeroStock) {
            jpql += " and s.stockQty>0 ";
        }

        if (departmentType != null) {
            if (departmentType == DepartmentType.Pharmacy) {
                jpql += " and (s.item.departmentType is null or s.item.departmentType =:depty) ";
            } else {
                jpql += " and s.item.departmentType=:depty ";
            }
            m.put("depty", departmentType);
        }

        if (department != null) {
            jpql += " and s.department=:d  ";
            m.put("d", department);
        }

        jpql += " order by s.item.name";

        pharmacyStockHistories = facade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        totalStockPurchaseValue = 0.0;
        totalStockSaleValue = 0.0;
        for (StockHistory psh : pharmacyStockHistories) {
            totalStockPurchaseValue += psh.getStockPurchaseValue();
            totalStockSaleValue += psh.getStockSaleValue();
        }
        pharmacyStockHistoriesArchive = null;
        if (includeArchived) {
            String archiveJpql = jpql.replace("from StockHistory s", "from StockHistoryArchive s");
            pharmacyStockHistoriesArchive = archiveFacade.findByJpql(archiveJpql, m, TemporalType.TIMESTAMP);
            for (StockHistoryArchive sha : pharmacyStockHistoriesArchive) {
                totalStockPurchaseValue += sha.getStockPurchaseValue();
                totalStockSaleValue += sha.getStockSaleValue();
            }
        }

    }

    public void fillStockHistoriesWithZero() {
        fillStockHistories(false);
    }

    public void fillStockHistoriesWithOutZero() {
        fillStockHistories(true);
    }

    public void recordHistory() {
        Date startTime = new Date();

        try {
            stockHistoryRecorder.myTimer();
            JsfUtil.addSuccessMessage("History Saved");
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Failed due to " + e.getMessage());
        }

    }

    public String viewPharmacyStockHistory() {
        getFromDate();
        getToDate();
        fillHistoryAvailableDays();
        return "/pharmacy/pharmacy_department_stock_history?faces-redirect=true";
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public Date getHistoryDate() {
        return historyDate;
    }

    public void setHistoryDate(Date historyDate) {
        this.historyDate = historyDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getFirstDayOfYear(new Date());
//            fillHistoryAvailableDays();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getLastDayOfYear(new Date());
//            fillHistoryAvailableDays();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<Date> getPharmacyStockHistoryDays() {
        return pharmacyStockHistoryDays;
    }

    public void setPharmacyStockHistoryDays(List<Date> pharmacyStockHistoryDays) {
        this.pharmacyStockHistoryDays = pharmacyStockHistoryDays;
    }

    public List<StockHistory> getPharmacyStockHistories() {
        return pharmacyStockHistories;
    }

    public void setPharmacyStockHistories(List<StockHistory> pharmacyStockHistories) {
        this.pharmacyStockHistories = pharmacyStockHistories;
    }

    public Department getDepartment() {
        if (department == null) {
            department = sessionController.getDepartment();
        }
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public double getTotalStockSaleValue() {
        return totalStockSaleValue;
    }

    public void setTotalStockSaleValue(double totalStockSaleValue) {
        this.totalStockSaleValue = totalStockSaleValue;
    }

    public double getTotalStockPurchaseValue() {
        return totalStockPurchaseValue;
    }

    public void setTotalStockPurchaseValue(double totalStockPurchaseValue) {
        this.totalStockPurchaseValue = totalStockPurchaseValue;
    }

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public boolean isIncludeArchived() { return includeArchived; }
    public void setIncludeArchived(boolean includeArchived) { this.includeArchived = includeArchived; }

    public List<StockHistoryArchive> getPharmacyStockHistoriesArchive() { return pharmacyStockHistoriesArchive; }

    public StockHistoryFacade getFacade() {
        return facade;
    }

    public StockHistory getCurrent() {
        return current;
    }

    public void setCurrent(StockHistory current) {
        this.current = current;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Inner Classes">
    @FacesConverter(forClass = StockHistory.class)
    public static class StockHistoryConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            try {
                StockHistoryController controller = (StockHistoryController) facesContext.getApplication().getELResolver()
                        .getValue(facesContext.getELContext(), null, "stockHistoryController");
                return controller.getFacade().find(getKey(value));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        Long getKey(String value) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                // Rethrow the exception to handle it in getAsObject
                throw e;
            }
        }

        String getStringKey(Long value) {
            if (value == null) {
                return null;
            }
            return value.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof StockHistory) {
                StockHistory stock = (StockHistory) object;
                return getStringKey(stock.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StockHistory.class.getName());
            }
        }
    }
    // </editor-fold>

}
