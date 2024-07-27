package com.divudi.data;

import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.WebUser;
import java.util.Date;
import java.util.List;

/**
 *
 * @author buddhika
 */
public class ReportTemplateRow {

    private String feeName;
    private String categoryName;
    private String toDepartmentName;
    private String itemName;
    private String paymentName;
    private Double rowValue;
    private Double rowValueIn;
    private Double rowValueOut;
    private Long rowCountIn;
    private Long rowCountOut;
    private Long rowCount;
    private Long id;

    private Category category;

    private Item item;
    private ServiceType serviceType;
    private BillTypeAtomic billTypeAtomic;
    private Institution creditCompany;
    private Department toDepartment;
    private Department itemDepartment;

    private Department department;
    private Institution institution;

    private Date date;
    private Date fromDate;
    private Date toDate;
    private Institution fromInstitution;
    private Department fromDepartment;
    private Institution toInstitution;
    private WebUser user;
    private Long startId;
    private Long endId;
    
    private List<BillTypeAtomic> btas;
    
    

    public ReportTemplateRow(Category c) {
        this.category = c;
    }

    public ReportTemplateRow(Item item) {
        this.item = item;

    }

    public ReportTemplateRow(Department d) {
        this.department = d;
    }

    public void setFeeName(String feeName) {
        this.feeName = feeName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    
    
    public String getToDepartmentName() {
        return toDepartmentName;
    }

    public void setToDepartmentName(String toDepartmentName) {
        this.toDepartmentName = toDepartmentName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getPaymentName() {
        return paymentName;
    }

    public void setPaymentName(String paymentName) {
        this.paymentName = paymentName;
    }

    public Double getRowValue() {
        return rowValue;
    }

    public void setRowValue(Double rowValue) {
        this.rowValue = rowValue;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }

    public ReportTemplateRow(String feeName, String categoryName, String toDepartmentName, String itemName, String paymentName, Double rowValue, Long rowCount) {
        this.feeName = feeName;
        this.categoryName = categoryName;
        this.toDepartmentName = toDepartmentName;
        this.itemName = itemName;
        this.paymentName = paymentName;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public ReportTemplateRow(String categoryName, Long rowCount, Double rowValue) {
        this.categoryName = categoryName;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public ReportTemplateRow(Category category, Long rowCount, Double rowValue) {
        this.category = category;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public ReportTemplateRow(Department itemDept, Long rowCount, Double rowValue) {
        this.itemDepartment = itemDept;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public ReportTemplateRow(Item item, Long rowCount, Double rowValue) {
        this.item = item;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public ReportTemplateRow(String categoryName, Double rowValue) {
        this.categoryName = categoryName;
        this.rowValue = rowValue;
    }

    public ReportTemplateRow(BillTypeAtomic billTypeAtomic, String categoryName, String toDepartmentName, Double rowValue) {
        this.billTypeAtomic = billTypeAtomic;
        this.categoryName = categoryName;
        this.toDepartmentName = toDepartmentName;
        this.rowValue = rowValue;
    }

    public ReportTemplateRow(BillTypeAtomic billTypeAtomic, Double rowValue) {
        this.billTypeAtomic = billTypeAtomic;
        this.rowValue = rowValue;
    }

    public ReportTemplateRow(BillTypeAtomic billTypeAtomic, Long rowCount, Double rowValue) {
        this.rowValue = rowValue;
        this.rowCount = rowCount;
        this.billTypeAtomic = billTypeAtomic;
    }
    
    

    public ReportTemplateRow(Double rowValue) {
        this.rowValue = rowValue;
    }

    public ReportTemplateRow() {
    }

    public ReportTemplateRow(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    // Custom method to generate a unique key string, handling null values
    public String getCustomKey() {
        return (category != null ? category.getName() : "") + "|"
                + (creditCompany != null ? creditCompany.getName() : "") + "|"
                + (toDepartment != null ? toDepartment.getName() : "") + "|"
                + (serviceType != null ? serviceType.getLabel() : "") + "|"
                + (billTypeAtomic != null ? billTypeAtomic.getLabel() : "");
    }

    public String getFeeName() {
        return feeName;
    }

    public Double getRowValueIn() {
        return rowValueIn;
    }

    public void setRowValueIn(Double rowValueIn) {
        this.rowValueIn = rowValueIn;
    }

    public Double getRowValueOut() {
        return rowValueOut;
    }

    public void setRowValueOut(Double rowValueOut) {
        this.rowValueOut = rowValueOut;
    }

    public Long getRowCountIn() {
        return rowCountIn;
    }

    public void setRowCountIn(Long rowCountIn) {
        this.rowCountIn = rowCountIn;
    }

    public Long getRowCountOut() {
        return rowCountOut;
    }

    public void setRowCountOut(Long rowCountOut) {
        this.rowCountOut = rowCountOut;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Department getItemDepartment() {
        return itemDepartment;
    }

    public void setItemDepartment(Department itemDepartment) {
        this.itemDepartment = itemDepartment;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        this.user = user;
    }

    public Long getStartId() {
        return startId;
    }

    public void setStartId(Long startId) {
        this.startId = startId;
    }

    public Long getEndId() {
        return endId;
    }

    public void setEndId(Long endId) {
        this.endId = endId;
    }

    public List<BillTypeAtomic> getBtas() {
        return btas;
    }

    public void setBtas(List<BillTypeAtomic> btas) {
        this.btas = btas;
    }

}
