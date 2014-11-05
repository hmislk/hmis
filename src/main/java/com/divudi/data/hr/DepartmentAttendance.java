/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.hr;

import com.divudi.entity.Department;
import java.util.Date;

/**
 *
 * @author divudi
 */
public class DepartmentAttendance {

    Department department;
    Date date;
    Double present;

    public DepartmentAttendance(Department department, Date date, Double present) {
        this.department = department;
        this.date = date;
        this.present = present;

    }

    public DepartmentAttendance(Department department, Object object, Long present) {
        this.department = department;
        this.date = (Date) object;
        if (present != null) {
            this.present = present.doubleValue();
        }
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Double getPresent() {
        return present;
    }

    public void setPresent(Double present) {
        this.present = present;
    }

}
