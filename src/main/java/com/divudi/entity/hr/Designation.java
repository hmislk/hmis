/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.entity.*;
import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;

/**
 *
 * @author buddhika
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Designation extends Category implements Serializable {
   
    private static final long serialVersionUID = 1L;
    @OneToMany(mappedBy = "designation")
    private List<Staff> staffs;

    
    
    public List<Staff> getStaffs() {
        return staffs;
    }

    public void setStaffs(List<Staff> staffs) {
        this.staffs = staffs;
    }

  
}
