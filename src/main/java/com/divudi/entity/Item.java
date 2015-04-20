
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.SessionNumberType;
import com.divudi.data.SymanticType;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.entity.lab.InvestigationItem;
import com.divudi.entity.lab.Machine;
import com.divudi.entity.lab.ReportItem;
import com.divudi.entity.lab.WorksheetItem;
import com.divudi.entity.pharmacy.MeasurementUnit;
import com.divudi.entity.pharmacy.Vmp;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author buddhika
 */
@Entity
public class Item implements Serializable, Comparable<Item> {

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    List<InvestigationItem> reportItems;
    //

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    List<WorksheetItem> worksheetItems;

    static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    int orderNo;

    @ManyToOne
    Category category;
    Double total = 0.0;
    Boolean discountAllowed = false;
    @ManyToOne
    Institution institution;
    @ManyToOne
    Department department;
    @ManyToOne
    Speciality speciality;
    @ManyToOne
    Staff staff;
    @ManyToOne
    Institution forInstitution;
    @ManyToOne
    Department forDepartment;
    @Enumerated(EnumType.STRING)
    BillType forBillType;
    @ManyToOne
    Item billedAs;
    @ManyToOne
    Item reportedAs;
    String name;
    String sname;
    String tname;
    String code;
    String barcode;
    String printName;
    String shortName;
    String fullName;
    //Created Properties
    @ManyToOne
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    //Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;
    @ManyToOne
    Item parentItem;
    boolean userChangable;
    @Enumerated(EnumType.STRING)
    InwardChargeType inwardChargeType;
    private double dblValue = 0.0f;
    SessionNumberType sessionNumberType;
    boolean priceByBatch;
    @ManyToOne
    MeasurementUnit measurementUnit;
    @ManyToOne
    Category worksheet;
    @ManyToOne
    Category reportFormat;
    boolean billable;
    boolean formatable;
    boolean patientNotRequired;
    boolean chargesVisibleForInward;
    boolean requestForQuentity;
    boolean marginNotAllowed;
    @ManyToOne
    Institution manufacturer;
    @ManyToOne
    Institution importer;

    @Lob
    String descreption;
    @Lob
    String comments;

    @Enumerated(EnumType.STRING)
    SymanticType symanticType;
    @Enumerated(EnumType.STRING)
    private DepartmentType departmentType;
    @Transient
    private double transBillItemCount;
    @Transient
    double transCheckedCount;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date effectiveFrom;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date effectiveTo;
    private boolean scanFee;
    double profitMargin;

    //Matara Phrmacy Sale Autocomplete
    @ManyToOne
    private Vmp vmp;
    
    @ManyToOne
    private Machine machine;
    
    String creditNumbers;
    String cashNumbers;
    String agencyNumbers;
    String reserveName;
    String reserveNumbers;
    int maxTableRows;

    public String getCreditNumbers() {
        return creditNumbers;
    }

    public void setCreditNumbers(String creditNumbers) {
        this.creditNumbers = creditNumbers;
    }

    public String getCashNumbers() {
        return cashNumbers;
    }

    public void setCashNumbers(String cashNumbers) {
        this.cashNumbers = cashNumbers;
    }

        public String getAgencyNumbers() {
        return agencyNumbers;
    }

    public void setAgencyNumbers(String agencyNumbers) {
        this.agencyNumbers = agencyNumbers;
    }

    public String getReserveName() {
        return reserveName;
    }

    public void setReserveName(String reserveName) {
        this.reserveName = reserveName;
    }

    public int getMaxTableRows() {
        return maxTableRows;
    }

    public void setMaxTableRows(int maxTableRows) {
        this.maxTableRows = maxTableRows;
    }

    public Date getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Date effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public double getTransCheckedCount() {
        return transCheckedCount;
    }

    public void setTransCheckedCount(double transCheckedCount) {
        this.transCheckedCount = transCheckedCount;
    }

    public boolean isMarginNotAllowed() {
        return marginNotAllowed;
    }

    public void setMarginNotAllowed(boolean marginNotAllowed) {
        this.marginNotAllowed = marginNotAllowed;
    }

    public List<WorksheetItem> getWorksheetItems() {
        if (worksheetItems != null) {
            Collections.sort(worksheetItems, new ReportItemComparator());
        } else {
            worksheetItems = new ArrayList<>();
        }
        return worksheetItems;
    }

    public void setWorksheetItems(List<WorksheetItem> worksheetItems) {
        this.worksheetItems = worksheetItems;
    }

    public Item() {

    }

    public List<InvestigationItem> getReportItems() {
        if (reportItems != null) {
            Collections.sort(reportItems, new ReportItemComparator());
        } else {
            reportItems = new ArrayList<>();
        }
        return reportItems;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (!(object instanceof Item)) {
            return false;
        }
        Item other = (Item) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.Item[ id=" + id + " ]";
    }

    @Transient
    double hospitalFee;
    @Transient
    double professionalFee;
    @Transient
    double hospitalFfee;
    @Transient
    double professionalFfee;
    @Transient
    double taxFee;
    @Transient
    double taxFfee;
    @Transient
    double otherFee;
    @Transient
    double otherFfee;
    @Transient
    double totalFee;
    @Transient
    double totalFfee;
    @Transient
    List<ItemFee> itemFees;

    public List<ItemFee> getItemFees() {
        if (itemFees == null) {
            itemFees = new ArrayList<>();
        }
        return itemFees;
    }

    public void setItemFees(List<ItemFee> itemFees) {
        this.itemFees = itemFees;
    }

    public double getTaxFee() {
        return taxFee;
    }

    public void setTaxFee(double taxFee) {
        this.taxFee = taxFee;
    }

    public double getTaxFfee() {
        return taxFfee;
    }

    public void setTaxFfee(double taxFfee) {
        this.taxFfee = taxFfee;
    }

    public double getOtherFee() {
        return otherFee;
    }

    public void setOtherFee(double otherFee) {
        this.otherFee = otherFee;
    }

    public double getOtherFfee() {
        return otherFfee;
    }

    public void setOtherFfee(double otherFfee) {
        this.otherFfee = otherFfee;
    }

    public double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(double totalFee) {
        this.totalFee = totalFee;
    }

    public double getTotalFfee() {
        return totalFfee;
    }

    public void setTotalFfee(double totalFfee) {
        this.totalFfee = totalFfee;
    }

    @Transient
    ItemFee itemFee;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Boolean isDiscountAllowed() {
        return discountAllowed;
    }

    public Boolean getDiscountAllowed() {
        return discountAllowed;
    }

    public void setDiscountAllowed(Boolean discountAllowed) {
        this.discountAllowed = discountAllowed;
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

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Institution getForInstitution() {
        return forInstitution;
    }

    public void setForInstitution(Institution forInstitution) {
        this.forInstitution = forInstitution;
    }

    public Department getForDepartment() {
        return forDepartment;
    }

    public void setForDepartment(Department forDepartment) {
        this.forDepartment = forDepartment;
    }

    public BillType getForBillType() {
        return forBillType;
    }

    public void setForBillType(BillType forBillType) {
        this.forBillType = forBillType;
    }

    public Item getBilledAs() {
        return billedAs;
    }

    public void setBilledAs(Item billedAs) {
        this.billedAs = billedAs;
    }

    public Item getReportedAs() {
        return reportedAs;
    }

    public void setReportedAs(Item reportedAs) {
        this.reportedAs = reportedAs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSname() {
        return sname;
    }

    public void setSname(String sname) {
        this.sname = sname;
    }

    public String getTname() {
        return tname;
    }

    public void setTname(String tname) {
        this.tname = tname;
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

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    public Item getParentItem() {
        return parentItem;
    }

    public void setParentItem(Item parentItem) {
        this.parentItem = parentItem;
    }

    public boolean isUserChangable() {
        return userChangable;
    }

    public void setUserChangable(boolean userChangable) {
        this.userChangable = userChangable;
    }

    public InwardChargeType getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(InwardChargeType inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public boolean isPriceByBatch() {
        return priceByBatch;
    }

    public void setPriceByBatch(boolean priceByBatch) {
        this.priceByBatch = priceByBatch;
    }

    public MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
    }

    public void setMeasurementUnit(MeasurementUnit measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

    public Category getWorksheet() {
        return worksheet;
    }

    public void setWorksheet(Category worksheet) {
        this.worksheet = worksheet;
    }

    public Category getReportFormat() {
        return reportFormat;
    }

    public void setReportFormat(Category reportFormat) {
        this.reportFormat = reportFormat;
    }

    public boolean isBillable() {
        return billable;
    }

    public void setBillable(boolean billable) {
        this.billable = billable;
    }

    public boolean isFormatable() {
        return formatable;
    }

    public void setFormatable(boolean formatable) {
        this.formatable = formatable;
    }

    public String getDescreption() {
        return descreption;
    }

    public void setDescreption(String descreption) {
        this.descreption = descreption;
    }

    public SymanticType getSymanticType() {
        return symanticType;
    }

    public void setSymanticType(SymanticType symanticType) {
        this.symanticType = symanticType;
    }

    public boolean isPatientNotRequired() {
        return patientNotRequired;
    }

    public void setPatientNotRequired(boolean patientNotRequired) {
        this.patientNotRequired = patientNotRequired;
    }

    public SessionNumberType getSessionNumberType() {
        return sessionNumberType;
    }

    public void setSessionNumberType(SessionNumberType sessionNumberType) {
        this.sessionNumberType = sessionNumberType;
    }

    public double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public double getProfessionalFee() {
        return professionalFee;
    }

    public void setProfessionalFee(double professionalFee) {
        this.professionalFee = professionalFee;
    }

    public double getHospitalFfee() {
        return hospitalFfee;
    }

    public void setHospitalFfee(double hospitalFfee) {
        this.hospitalFfee = hospitalFfee;
    }

    public double getProfessionalFfee() {
        return professionalFfee;
    }

    public void setProfessionalFfee(double professionalFfee) {
        this.professionalFfee = professionalFfee;
    }

    public ItemFee getItemFee() {
        return itemFee;
    }

    public void setItemFee(ItemFee itemFee) {
        this.itemFee = itemFee;
    }

    public double getDblValue() {
        return dblValue;
    }

    public void setDblValue(double dblValue) {
        this.dblValue = dblValue;
    }

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public double getProfitMargin() {
        return profitMargin;
    }

    public void setProfitMargin(double profitMargin) {
        this.profitMargin = profitMargin;
    }


    public double getTransBillItemCount() {
        return transBillItemCount;
    }

    public void setTransBillItemCount(double transBillItemCount) {
        this.transBillItemCount = transBillItemCount;
    }

    public boolean isChargesVisibleForInward() {
        return chargesVisibleForInward;
    }

    public void setChargesVisibleForInward(boolean chargesVisibleForInward) {
        this.chargesVisibleForInward = chargesVisibleForInward;
    }

    public boolean isRequestForQuentity() {
        return requestForQuentity;
    }

    public void setRequestForQuentity(boolean requestForQuentity) {
        this.requestForQuentity = requestForQuentity;
    }

    public Institution getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Institution manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Institution getImporter() {
        return importer;
    }

    public void setImporter(Institution importer) {
        this.importer = importer;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Vmp getVmp() {
        return vmp;
    }

    public void setVmp(Vmp vmp) {
        this.vmp = vmp;
    }

    public Date getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Date effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public boolean isScanFee() {
        return scanFee;
    }

    public void setScanFee(boolean scanFee) {
        this.scanFee = scanFee;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public String getReserveNumbers() {
        return reserveNumbers;
    }

    public void setReserveNumbers(String reserveNumbers) {
        this.reserveNumbers = reserveNumbers;
    }

    @Override
    public int compareTo(Item o) {
        if(o==null){
            return 0;
        }
        
        if(o.getName()==null){
            return 0;
        }
        
        if(this==null){
            return 0;
        }
        
        if(this.getName()==null){
            return 0;
        }
        
        return this.name.compareTo(o.name);
    }

   

    static class ReportItemComparator implements Comparator<ReportItem> {

        @Override
        public int compare(ReportItem o1, ReportItem o2) {
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            if (o1.getCssTop() == null) {
                return 1;
            }
            if (o2.getCssTop() == null) {
                return -1;
            }
            return o1.getCssTop().compareTo(o2.getCssTop());  //To change body of generated methods, choose Tools | Templates.
        }
    }

}
