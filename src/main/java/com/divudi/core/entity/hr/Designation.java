/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.hr;

import com.divudi.core.entity.Category;
import com.divudi.core.entity.Staff;
import java.io.Serializable;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author buddhika
 */
@Entity

public class Designation extends Category implements Serializable {

    private static final long serialVersionUID = 1L;
    @OneToMany(mappedBy = "designation")
    private List<Staff> staffs;



    @XmlTransient
    public List<Staff> getStaffs() {
        return staffs;
    }

    public void setStaffs(List<Staff> staffs) {
        this.staffs = staffs;
    }


}
