/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.data.BillType;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.WebUser;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.bean.SessionScoped;
import javax.inject.Named;
import javax.faces.view.ViewScoped;

/**
 *
 * @author Buddhika
 */
@Named(value = "commonReportController")
@SessionScoped
public class CommonReportController {

    List<Class<?>> availableClasses ;

    Date fromDate;
    Date toDate;
    Institution institution;
    Department department;
    WebUser webUser;
    List<BillType> billTypes;
    List<billSummeryRow> billSummeryRows;
    List<Class<?>> selectedClasses ;

    public List<Class<?>> getAvailableClasses() {
        if(availableClasses==null){
            availableClasses= new ArrayList<>();
            
            
        }
        return availableClasses;
    }

    public void setAvailableClasses(List<Class<?>> availableClasses) {
        this.availableClasses = availableClasses;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public List<BillType> getBillTypes() {
        return billTypes;
    }

    public void setBillTypes(List<BillType> billTypes) {
        this.billTypes = billTypes;
    }

    public List<billSummeryRow> getBillSummeryRows() {
        return billSummeryRows;
    }

    public void setBillSummeryRows(List<billSummeryRow> billSummeryRows) {
        this.billSummeryRows = billSummeryRows;
    }

    public List<Class<?>> getSelectedClasses() {
        return selectedClasses;
    }

    public void setSelectedClasses(List<Class<?>> selectedClasses) {
        this.selectedClasses = selectedClasses;
    }

    /**
     * Creates a new instance of CommonReportController
     */
    public CommonReportController() {
    }

    public void findBillsByWeekday() {
        String jpql;
        Map m = new HashMap();
        jpql = "select sum(b.netValue), count(b), func('',b.createdAt) "
                + " from Bill b "
                + " where b.billType in :bts "
                + " and b.createdAt between :fd and :td "
                + " and ";
    }

    public class billSummeryRow {

        long count;
        double value;
        String name;
        int number;

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

    }

}
