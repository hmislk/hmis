/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.entity.hr;

import com.divudi.entity.Form;
import com.divudi.entity.Staff;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class HrForm extends Form implements Serializable{
    
    private Staff staff;
    private Staff approved;
    @Temporal(TemporalType.TIMESTAMP)
    private Date approvedAt;

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Staff getApproved() {
        return approved;
    }

    public void setApproved(Staff approved) {
        this.approved = approved;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    
    
}
