/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.CommonFunctionsController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.DepartmentType;
import com.divudi.data.HistoryType;
import com.divudi.ejb.StockHistoryRecorder;
import com.divudi.entity.Department;
import com.divudi.entity.pharmacy.StockHistory;
import com.divudi.facade.StockHistoryFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Item;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
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

    @EJB
    StockHistoryFacade facade;
    @Inject
    CommonController commonController;

    List<StockHistory> pharmacyStockHistories;
    List<Date> pharmacyStockHistoryDays;
    Date fromDate;
    Date toDate;
    Date historyDate;
    Department department;
    DepartmentType departmentType;

    double totalStockSaleValue;
    double totalStockPurchaseValue;

    public void fillHistoryAvailableDays() {
        Date startTime = new Date();

        String jpql;
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ht", HistoryType.MonthlyRecord);
//        jpql = "select FUNC('Date',s.stockAt) from StockHistory s where s.historyType=:ht and s.stockAt between :fd and :td group by FUNC('Date',s.stockAt)";
        jpql = "select distinct(s.createdAt) from StockHistory s "
                + " where s.historyType=:ht"
                + " and s.createdAt between :fd and :td "
                + " order by s.createdAt desc ";

//        List<StockHistory> historys=facade.findByJpql(jpql, m,TemporalType.TIMESTAMP);
//        for (StockHistory history : historys) {
//            //// // System.out.println("history.getStockAt() = " + history.getStockAt());
//            //// // System.out.println("history.getStockAt() = " + history.getCreatedAt());
//        }
        ////// // System.out.println("m = " + m);
        pharmacyStockHistoryDays = facade.findDateListByJpql(jpql, m, TemporalType.TIMESTAMP);
        for (Date d : pharmacyStockHistoryDays) {
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Reports/Stock Reports/Stock history(Display Available Days)(/faces/pharmacy/pharmacy_department_stock_history.xhtml)");
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
        System.out.println("m = " + m);
        System.out.println("jpql = " + jpql);
        List<StockHistory> shxs = facade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        if (shxs != null) {
            System.out.println("shxs = " + shxs);
        }
        return shxs;
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

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Reports/Stock Reports/Stock history(Display History)(/faces/pharmacy/pharmacy_department_stock_history.xhtml)");
    }

    public void fillStockHistoriesWithZero() {
        fillStockHistories(false);
    }

    public void fillStockHistoriesWithOutZero() {
        fillStockHistories(true);
    }

    public Date getHistoryDate() {
        return historyDate;
    }

    public void setHistoryDate(Date historyDate) {
        this.historyDate = historyDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctionsController.getFirstDayOfYear(new Date());
//            fillHistoryAvailableDays();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctionsController.getLastDayOfYear(new Date());
//            fillHistoryAvailableDays();
        }
        return toDate;
    }

    public String viewPharmacyStockHistory() {
        getFromDate();
        getToDate();
        fillHistoryAvailableDays();
        return "/pharmacy/pharmacy_department_stock_history?faces-redirect=true";
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    @EJB
    StockHistoryRecorder stockHistoryRecorder;

    public void recordHistory() {
        Date startTime = new Date();

        try {
            stockHistoryRecorder.myTimer();
            JsfUtil.addSuccessMessage("History Saved");
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Failed due to " + e.getMessage());
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Reports/Stock Reports/Stock history(Record History Now)(/faces/pharmacy/pharmacy_department_stock_history.xhtml)");
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

    /**
     * Creates a new instance of StockHistoryController
     */
    public StockHistoryController() {
    }

    @Inject
    SessionController sessionController;

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

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

}
