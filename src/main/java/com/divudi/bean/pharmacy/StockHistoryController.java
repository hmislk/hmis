/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.CommonFunctionsController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.HistoryType;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.StockHistoryRecorder;
import com.divudi.entity.Department;
import com.divudi.entity.pharmacy.StockHistory;
import com.divudi.facade.StockHistoryFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;

/**
 *
 * @author buddhika
 */
@Named
@SessionScoped
public class StockHistoryController implements Serializable {

    @EJB
    StockHistoryFacade facade;

    List<StockHistory> pharmacyStockHistories;
    List<Date> pharmacyStockHistoryDays;
    Date fromDate;
    Date toDate;
    Date historyDate;
    Department department;
    
    

    public void fillHistoryAvailableDays() {
        
        String jpql;
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("ht", HistoryType.MonthlyRecord);
        jpql = "select FUNC('Date',s.stockAt) from StockHistory s where s.historyType=:ht and s.stockAt between :fd and :td group by FUNC('Date',s.stockAt)";
        //System.out.println("m = " + m);
        pharmacyStockHistoryDays = facade.findDateListBySQL(jpql, m);
    }

    public void fillStockHistories() {
        String jpql;
        Map m = new HashMap();
        m.put("hd", historyDate);
        m.put("ht", HistoryType.MonthlyRecord);
        if (department == null) {
            jpql = "select s from StockHistory s where s.historyType=:ht and s.stockAt =:hd order by s.item.name";
        } else {
            m.put("d", department);
            jpql = "select s from StockHistory s where s.historyType=:ht and s.department=:d and s.stockAt =:hd order by s.item.name";
        }
        //System.out.println("m = " + m);
        //System.out.println("jpql = " + jpql);
        pharmacyStockHistories = facade.findBySQL(jpql, m);
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
    
    public String viewPharmacyStockHistory(){
        getFromDate();
        getToDate();
        fillHistoryAvailableDays();
        return "/pharmacy/pharmacy_department_stock_history";
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }
    
    @EJB
    StockHistoryRecorder stockHistoryRecorder;
    
    public void recordHistory(){
        try{
            stockHistoryRecorder.myTimer();
            JsfUtil.addSuccessMessage("History Saved");
        }catch (Exception e){
            JsfUtil.addErrorMessage("Failed due to " + e.getMessage());
        }
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
        if(department==null){
            department=sessionController.getDepartment();
        }
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    
    
}
