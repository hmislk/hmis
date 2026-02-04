/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.data.dto;

import com.divudi.core.data.ItemCount;
import java.io.Serializable;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 *
 * @author dhanesh
 */
@Entity
public class MonthlySurgeryCountDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id; 
    private Month month;
    private List<ItemCount> itemcounts;
    private Double total;

    public MonthlySurgeryCountDTO() {
    }  

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MonthlySurgeryCountDTO)) {
            return false;
        }
        MonthlySurgeryCountDTO other = (MonthlySurgeryCountDTO) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.data.dto.MonthlySurgeryCountDTO[ id=" + id + " ]";
    }

    public Month getMonth() {
        return month;
    }

    public void setMonth(Month month) {
        this.month = month;
    }

    public List<ItemCount> getItemcounts() {
        if(itemcounts == null){
            itemcounts = new ArrayList<>();
        }
        return itemcounts;
    }

    public void setItemcounts(List<ItemCount> itemcounts) {
        this.itemcounts = itemcounts;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
    
    
    
}
