package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO-specific bundle for Daily Return reports.
 */
public class DailyReturnBundleDTO implements Serializable {
    private String name;
    private String bundleType;
    private String description;
    private List<DailyReturnRowDTO> rows = new ArrayList<>();
    private List<DailyReturnBundleDTO> bundles = new ArrayList<>();
    
    private Double total = 0.0;
    private Double hospitalTotal = 0.0;
    private Double staffTotal = 0.0;
    private Double discount = 0.0;
    private Long count = 0L;

    public DailyReturnBundleDTO() {
    }

    public DailyReturnBundleDTO(String name, String bundleType) {
        this.name = name;
        this.bundleType = bundleType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBundleType() {
        return bundleType;
    }

    public void setBundleType(String bundleType) {
        this.bundleType = bundleType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DailyReturnRowDTO> getRows() {
        return rows;
    }

    public void setRows(List<DailyReturnRowDTO> rows) {
        this.rows = rows;
    }

    public List<DailyReturnBundleDTO> getBundles() {
        return bundles;
    }

    public void setBundles(List<DailyReturnBundleDTO> bundles) {
        this.bundles = bundles;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getHospitalTotal() {
        return hospitalTotal;
    }

    public void setHospitalTotal(Double hospitalTotal) {
        this.hospitalTotal = hospitalTotal;
    }

    public Double getStaffTotal() {
        return staffTotal;
    }

    public void setStaffTotal(Double staffTotal) {
        this.staffTotal = staffTotal;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}