/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

import com.divudi.entity.Item;

/**
 *
 * @author buddhika
 */
public class ItemLight {

    Long id;
    Integer orderNo;
    private Boolean isMasterItem;
    private Boolean hasReportFormat;
    String categoryName;
    Long categoryId;
    Double total = 0.0;
    Double totalForForeigner = 0.0;
    String institutionName;
    Long institutionId;
    String departmentName;
    Long departmentId;
    String specialityName;
    Long specialityId;
    String staffName;
    Long staffId;
    String dtype;
    String name;
    String code;
    String barcode;
    String printName;
    String shortName;
    String fullName;
    private String feeName;
    private Double feeValue = 0.0;
    private Double feeValueForeign = 0.0;

    public ItemLight(Long id, Integer orderNo, Boolean isMasterItem, Boolean hasReportFormat,
            String categoryName, Long categoryId, String institutionName, Long institutionId,
            String departmentName, Long departmentId, String specialityName, Long specialityId,
            String staffName, Long staffId, String name, String code, String barcode,
            String printName, String shortName, String fullName) {
        this.id = id;
        this.orderNo = orderNo;
        this.isMasterItem = isMasterItem;
        this.hasReportFormat = hasReportFormat;
        this.categoryName = categoryName;
        this.categoryId = categoryId;
        this.institutionName = institutionName;
        this.institutionId = institutionId;
        this.departmentName = departmentName;
        this.departmentId = departmentId;
        this.specialityName = specialityName;
        this.specialityId = specialityId;
        this.staffName = staffName;
        this.staffId = staffId;
        this.name = name;
        this.code = code;
        this.barcode = barcode;
        this.printName = printName;
        this.shortName = shortName;
        this.fullName = fullName;
    }

    public ItemLight(Long id, String departmentName, String name, String code, Double total) {
        this.id = id;
        this.departmentName = departmentName;
        this.name = name;
        this.code = code;
        this.total = total;
    }
    
    public ItemLight(Long id, String departmentName, String name, String code, Double total, Long departmentId) {
        this.id = id;
        this.departmentName = departmentName;
        this.name = name;
        this.code = code;
        this.total = total;
        this.departmentId = departmentId;
    }

    
    
    public ItemLight(Long id, Integer orderNo, Boolean isMasterItem, Boolean hasReportFormat,
            String categoryName, Long categoryId, String institutionName, Long institutionId,
            String departmentName, Long departmentId, String specialityName, Long specialityId,
            String staffName, Long staffId, String name, String code, String barcode,
            String printName, String shortName, String fullName, Double total) {
        this.id = id;
        this.orderNo = orderNo;
        this.isMasterItem = isMasterItem;
        this.hasReportFormat = hasReportFormat;
        this.categoryName = categoryName;
        this.categoryId = categoryId;
        this.institutionName = institutionName;
        this.institutionId = institutionId;
        this.departmentName = departmentName;
        this.departmentId = departmentId;
        this.specialityName = specialityName;
        this.specialityId = specialityId;
        this.staffName = staffName;
        this.staffId = staffId;
        this.name = name;
        this.code = code;
        this.barcode = barcode;
        this.printName = printName;
        this.shortName = shortName;
        this.fullName = fullName;
        this.total = total;
    }

    public Long getId() {
        return id;
    }

    public ItemLight(Long id, String name, String code, Double total) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.total = total;
    }
    
    public ItemLight(Long id, String name, String code, Double total, String feeName, Double feeValue, Double feeValueForeign) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.total = total;
        this.feeName = feeName;
        this.feeValue = feeValue;
        this.feeValueForeign = feeValueForeign;
    }

    public ItemLight(Long id, String name, String code, String barcode,
            String printName, String shortName, String fullName,
            Boolean isMasterItem, Boolean hasReportFormat, Double total) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.barcode = barcode;
        this.printName = printName;
        this.shortName = shortName;
        this.fullName = fullName;
        this.isMasterItem = isMasterItem;
        this.hasReportFormat = hasReportFormat;
        this.total = total;
    }

    public ItemLight(Long id,
            String name,
            String code,
            String fullName,
            String departmentName,
            Double total) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.fullName = fullName;
        this.departmentName = departmentName;
        this.total = total;
    }

    public ItemLight(Item item) {
        this.id = item.getId();
        this.orderNo = item.getOrderNo();
        this.isMasterItem = item.isIsMasterItem();
        this.hasReportFormat = item.isHasReportFormat();
        this.categoryName = item.getCategory() != null ? item.getCategory().getName() : null;
        this.categoryId = item.getCategory() != null ? item.getCategory().getId() : null;
        this.institutionName = item.getInstitution() != null ? item.getInstitution().getName() : null;
        this.institutionId = item.getInstitution() != null ? item.getInstitution().getId() : null;
        this.departmentName = item.getDepartment() != null ? item.getDepartment().getName() : null;
        this.departmentId = item.getDepartment() != null ? item.getDepartment().getId() : null;
        this.specialityName = item.getSpeciality() != null ? item.getSpeciality().getName() : null;
        this.specialityId = item.getSpeciality() != null ? item.getSpeciality().getId() : null;
        this.staffName = item.getStaff() != null ? item.getStaff().getName() : null;
        this.staffId = item.getStaff() != null ? item.getStaff().getId() : null;
        this.dtype = item.getClazz(); // assuming getClazz() method exists in Item class
        this.name = item.getName();
        this.code = item.getCode();
        this.barcode = item.getBarcode();
        this.printName = item.getPrintName();
        this.shortName = item.getShortName();
        this.fullName = item.getFullName();
        // Add any other fields as needed
    }

    public ItemLight(Long id, Integer orderNo, Boolean isMasterItem, Boolean hasReportFormat, String categoryName, Long categoryId, String institutionName, Long institutionId, String departmentName, Long departmentId, String specialityName, Long specialityId, String staffName, Long staffId, String dtype, String name, String code, String barcode, String printingName, String shortName, String fullName) {
        this.id = id;
        this.orderNo = orderNo;
        this.isMasterItem = isMasterItem;
        this.hasReportFormat = hasReportFormat;
        this.categoryName = categoryName;
        this.categoryId = categoryId;
        this.institutionName = institutionName;
        this.institutionId = institutionId;
        this.departmentName = departmentName;
        this.departmentId = departmentId;
        this.specialityName = specialityName;
        this.specialityId = specialityId;
        this.staffName = staffName;
        this.staffId = staffId;
        this.dtype = dtype;
        this.name = name;
        this.code = code;
        this.barcode = barcode;
        this.printName = printName;
        this.shortName = shortName;
        this.fullName = fullName;
    }

    public ItemLight(Long id, Integer orderNo, Boolean isMasterItem, Boolean hasReportFormat,
            Long categoryId, Long institutionId, Long departmentId, Long specialityId,
            Long staffId, Long staffPersonId, String name, String code, String barcode,
            String printName, String shortName, String fullName, Double total) {
        this.id = id;
        this.orderNo = orderNo;
        this.isMasterItem = isMasterItem;
        this.hasReportFormat = hasReportFormat;
        this.categoryId = categoryId;
        // Note: As per your class, you don't have fields to directly store staffPersonId,
        // so you might need to adjust this part based on your actual requirements.
        this.institutionId = institutionId;
        this.departmentId = departmentId;
        this.specialityId = specialityId;
        this.staffId = staffId;
        this.name = name;
        this.code = code;
        this.barcode = barcode;
        this.printName = printName;
        this.shortName = shortName;
        this.fullName = fullName;
        this.total = total;
    }

    public ItemLight(Long id, String name, String code, String barcode,
            String printingName, String shortName, String fullName,
            Boolean isMasterItem, Boolean hasReportFormat,
            Long categoryId, String categoryName,
            Long departmentId, String departmentName,
            Long institutionId, String institutionName,
            Double total) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.barcode = barcode;
        this.printName = printName;
        this.shortName = shortName;
        this.fullName = fullName;
        this.isMasterItem = isMasterItem;
        this.hasReportFormat = hasReportFormat;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.total = total;
    }

    public ItemLight(Long id, Integer orderNo, Boolean isMasterItem, Boolean hasReportFormat,
            Long categoryId, Long institutionId, Long departmentId, Long specialityId,
            Long staffId, String name, String code, String barcode,
            String printName, String shortName, String fullName, Double total) {
        this.id = id;
        this.orderNo = orderNo;
        this.isMasterItem = isMasterItem;
        this.hasReportFormat = hasReportFormat;
        this.categoryId = categoryId;
        this.institutionId = institutionId;
        this.departmentId = departmentId;
        this.specialityId = specialityId;
        this.staffId = staffId;
        this.name = name;
        this.code = code;
        this.barcode = barcode;
        this.printName = printName;
        this.shortName = shortName;
        this.fullName = fullName;
        this.total = total;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public Boolean isIsMasterItem() {
        return isMasterItem;
    }

    public void setIsMasterItem(Boolean isMasterItem) {
        this.isMasterItem = isMasterItem;
    }

    public Boolean isHasReportFormat() {
        return hasReportFormat;
    }

    public void setHasReportFormat(Boolean hasReportFormat) {
        this.hasReportFormat = hasReportFormat;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getTotalForForeigner() {
        return totalForForeigner;
    }

    public void setTotalForForeigner(Double totalForForeigner) {
        this.totalForForeigner = totalForForeigner;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getSpecialityName() {
        return specialityName;
    }

    public void setSpecialityName(String specialityName) {
        this.specialityName = specialityName;
    }

    public Long getSpecialityId() {
        return specialityId;
    }

    public void setSpecialityId(Long specialityId) {
        this.specialityId = specialityId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getDtype() {
        return dtype;
    }

    public void setDtype(String dtype) {
        this.dtype = dtype;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getPrintName() {
        return printName;
    }

    public void setPrintName(String printName) {
        this.printName = printName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public double getVatPercentage() {
        return 0;
    }

    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();

        // Append name with null check
        if (name != null) {
            sb.append(name);
        }

        // Append department name with null check and tabs for spacing
        if (departmentName != null) {
            sb.append("\t - \t").append(departmentName);
        }

        // Append code with null check
        if (code != null) {
            sb.append("\t - \t").append(code);
        }

        // Append total with null check
        if (total != null) {
            sb.append("\t - \t").append(total);
        }

        return sb.toString();
    }

    public ItemLight() {

    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (!(object instanceof ItemLight)) {
            return false;
        }
        ItemLight other = (ItemLight) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        // Return a unique string representation for each ItemLight object
        return "ItemLight{" + "id=" + id + ", name=" + name + '}';
    }

    public String getFeeName() {
        return feeName;
    }

    public void setFeeName(String feeName) {
        this.feeName = feeName;
    }

    public Double getFeeValue() {
        return feeValue;
    }

    public void setFeeValue(Double feeValue) {
        this.feeValue = feeValue;
    }

    public Double getFeeValueForeign() {
        return feeValueForeign;
    }

    public void setFeeValueForeign(Double feeValueForeign) {
        this.feeValueForeign = feeValueForeign;
    }

}
