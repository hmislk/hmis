package com.divudi.data;

import com.divudi.entity.Bill;
import com.divudi.entity.BillSession;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.channel.SessionInstance;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author buddhika
 */
public class ReportTemplateRow {

    private String uuid;

    private Category category;

    private Item item;

    private Long categoryCount;
    private Double categoryTotal;
    private Double categoryHospitalFee;
    private Double categoryDiscountAmount;
    private Double categoryCollectingCentreFee;
    private Double categoryDiscount;
    private Double categoryProfessionalFee;
    private Double categoryNetTotal;

    private Long itemCount;
    private Double itemTotal;
    private Double itemHospitalFee;
    private Double itemDiscountAmount;
    private Double itemCollectingCentreFee;
    private Double itemDiscount;
    private Double itemProfessionalFee;
    private Double itemNetTotal;

    private List<Bill> cashHnadovers;

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

    private Long long1;
    private Long long2;
    private Long long3;
    private Long long4;
    private Long long5;
    private Long long6;
    private Long long7;
    private Long long8;
    private Long long9;
    private Long long10;

    private ServiceType serviceType;
    private BillTypeAtomic billTypeAtomic;
    private Institution creditCompany;
    private Department toDepartment;
    private Department itemDepartment;
    private BillSession billSession;
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
    private SessionInstance sessionInstance;
    private Staff staff;
    private Institution referringInstitution;
    private Staff referringStaff;

    private List<BillTypeAtomic> btas;

    private double onCallValue;
    private double cashValue;
    private double cardValue;
    private double multiplePaymentMethodsValue;
    private double staffValue;
    private double creditValue;
    private double staffWelfareValue;
    private double voucherValue;
    private double iouValue;
    private double agentValue;
    private double chequeValue;
    private double slipValue;
    private double eWalletValue;
    private double patientDepositValue;
    private double patientPointsValue;
    private double onlineSettlementValue;

    private String rowType;

    public ReportTemplateRow(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
    }

    public ReportTemplateRow(Department department, Double rowValue) {
        this.rowValue = rowValue;
        this.department = department;
    }

    public ReportTemplateRow(Department department, Date date,
            double cashValue, double cardValue, double multiplePaymentMethodsValue,
            double staffValue, double creditValue, double staffWelfareValue,
            double voucherValue, double iouValue, double agentValue,
            double chequeValue, double slipValue, double eWalletValue,
            double patientDepositValue, double patientPointsValue, double onlineSettlementValue) {
        this.department = department;
        this.date = date;
        this.cashValue = cashValue;
        this.cardValue = cardValue;
        this.multiplePaymentMethodsValue = multiplePaymentMethodsValue;
        this.staffValue = staffValue;
        this.creditValue = creditValue;
        this.staffWelfareValue = staffWelfareValue;
        this.voucherValue = voucherValue;
        this.iouValue = iouValue;
        this.agentValue = agentValue;
        this.chequeValue = chequeValue;
        this.slipValue = slipValue;
        this.eWalletValue = eWalletValue;
        this.patientDepositValue = patientDepositValue;
        this.patientPointsValue = patientPointsValue;
        this.onlineSettlementValue = onlineSettlementValue;
    }

    public ReportTemplateRow(Department department, Date date, WebUser user,
            double cashValue, double cardValue, double multiplePaymentMethodsValue,
            double staffValue, double creditValue, double staffWelfareValue,
            double voucherValue, double iouValue, double agentValue,
            double chequeValue, double slipValue, double eWalletValue,
            double patientDepositValue, double patientPointsValue, double onlineSettlementValue) {
        this.department = department;
        this.date = date;
        this.cashValue = cashValue;
        this.user = user;
        this.cardValue = cardValue;
        this.multiplePaymentMethodsValue = multiplePaymentMethodsValue;
        this.staffValue = staffValue;
        this.creditValue = creditValue;
        this.staffWelfareValue = staffWelfareValue;
        this.voucherValue = voucherValue;
        this.iouValue = iouValue;
        this.agentValue = agentValue;
        this.chequeValue = chequeValue;
        this.slipValue = slipValue;
        this.eWalletValue = eWalletValue;
        this.patientDepositValue = patientDepositValue;
        this.patientPointsValue = patientPointsValue;
        this.onlineSettlementValue = onlineSettlementValue;
    }

    public ReportTemplateRow(Staff referringStaff, Institution referringInstitution, Long long1, Double rowValue) {
        this.rowValue = rowValue;
        this.long1 = long1;
        this.referringInstitution = referringInstitution;
        this.referringStaff = referringStaff;
    }

    public ReportTemplateRow(Staff referringStaff, Long long1, Double rowValue) {
        this.rowValue = rowValue;
        this.long1 = long1;
        this.referringStaff = referringStaff;
    }

    public ReportTemplateRow(Institution referringInstitution, Long long1, Double rowValue) {
        this.rowValue = rowValue;
        this.long1 = long1;
        this.referringInstitution = referringInstitution;
    }

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

    public BillSession getBillSession() {
        return billSession;
    }

    public ReportTemplateRow(BillSession billSession) {
        this.billSession = billSession;
    }

    public void setBillSession(BillSession billSession) {
        this.billSession = billSession;
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

    public SessionInstance getSessionInstance() {
        return sessionInstance;
    }

    public void setSessionInstance(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
    }

    public Long getLong1() {
        return long1;
    }

    public void setLong1(Long long1) {
        this.long1 = long1;
    }

    public Long getLong2() {
        return long2;
    }

    public void setLong2(Long long2) {
        this.long2 = long2;
    }

    public Long getLong3() {
        return long3;
    }

    public void setLong3(Long long3) {
        this.long3 = long3;
    }

    public Long getLong4() {
        return long4;
    }

    public void setLong4(Long long4) {
        this.long4 = long4;
    }

    public Long getLong5() {
        return long5;
    }

    public void setLong5(Long long5) {
        this.long5 = long5;
    }

    public Long getLong6() {
        return long6;
    }

    public void setLong6(Long long6) {
        this.long6 = long6;
    }

    public Long getLong7() {
        return long7;
    }

    public void setLong7(Long long7) {
        this.long7 = long7;
    }

    public Long getLong8() {
        return long8;
    }

    public void setLong8(Long long8) {
        this.long8 = long8;
    }

    public Long getLong9() {
        return long9;
    }

    public void setLong9(Long long9) {
        this.long9 = long9;
    }

    public Long getLong10() {
        return long10;
    }

    public void setLong10(Long long10) {
        this.long10 = long10;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public ReportTemplateRow(Staff staff, Long long1, Long long2, Long long3, Long long4, Long long5, Long long6, Long long7) {
        this.staff = staff;
        this.long1 = long1;
        this.long2 = long2;
        this.long3 = long3;
        this.long4 = long4;
        this.long5 = long5;
        this.long6 = long6;
        this.long7 = long7;
        this.staff = staff;
    }

    public Institution getReferringInstitution() {
        return referringInstitution;
    }

    public void setReferringInstitution(Institution referringInstitution) {
        this.referringInstitution = referringInstitution;
    }

    public Staff getReferringStaff() {
        return referringStaff;
    }

    public void setReferringStaff(Staff referringStaff) {
        this.referringStaff = referringStaff;
    }

    public String getUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public double getOnCallValue() {
        return onCallValue;
    }

    public void setOnCallValue(double onCallValue) {
        this.onCallValue = onCallValue;
    }

    public double getCashValue() {
        return cashValue;
    }

    public void setCashValue(double cashValue) {
        this.cashValue = cashValue;
    }

    public double getCardValue() {
        return cardValue;
    }

    public void setCardValue(double cardValue) {
        this.cardValue = cardValue;
    }

    public double getMultiplePaymentMethodsValue() {
        return multiplePaymentMethodsValue;
    }

    public void setMultiplePaymentMethodsValue(double multiplePaymentMethodsValue) {
        this.multiplePaymentMethodsValue = multiplePaymentMethodsValue;
    }

    public double getStaffValue() {
        return staffValue;
    }

    public void setStaffValue(double staffValue) {
        this.staffValue = staffValue;
    }

    public double getCreditValue() {
        return creditValue;
    }

    public void setCreditValue(double creditValue) {
        this.creditValue = creditValue;
    }

    public double getStaffWelfareValue() {
        return staffWelfareValue;
    }

    public void setStaffWelfareValue(double staffWelfareValue) {
        this.staffWelfareValue = staffWelfareValue;
    }

    public double getVoucherValue() {
        return voucherValue;
    }

    public void setVoucherValue(double voucherValue) {
        this.voucherValue = voucherValue;
    }

    public double getIouValue() {
        return iouValue;
    }

    public void setIouValue(double iouValue) {
        this.iouValue = iouValue;
    }

    public double getAgentValue() {
        return agentValue;
    }

    public void setAgentValue(double agentValue) {
        this.agentValue = agentValue;
    }

    public double getChequeValue() {
        return chequeValue;
    }

    public void setChequeValue(double chequeValue) {
        this.chequeValue = chequeValue;
    }

    public double getSlipValue() {
        return slipValue;
    }

    public void setSlipValue(double slipValue) {
        this.slipValue = slipValue;
    }

    public double getEwalletValue() {
        return eWalletValue;
    }

    public void setEwalletValue(double eWalletValue) {
        this.eWalletValue = eWalletValue;
    }

    public double getPatientDepositValue() {
        return patientDepositValue;
    }

    public void setPatientDepositValue(double patientDepositValue) {
        this.patientDepositValue = patientDepositValue;
    }

    public double getPatientPointsValue() {
        return patientPointsValue;
    }

    public void setPatientPointsValue(double patientPointsValue) {
        this.patientPointsValue = patientPointsValue;
    }

    public double getOnlineSettlementValue() {
        return onlineSettlementValue;
    }

    public void setOnlineSettlementValue(double onlineSettlementValue) {
        this.onlineSettlementValue = onlineSettlementValue;
    }

    public List<Bill> getCashHnadovers() {
        if (cashHnadovers == null) {
            cashHnadovers = new ArrayList<>();
        }
        return cashHnadovers;
    }

    public void setCashHnadovers(List<Bill> cashHnadovers) {
        this.cashHnadovers = cashHnadovers;
    }

    public double geteWalletValue() {
        return eWalletValue;
    }

    public void seteWalletValue(double eWalletValue) {
        this.eWalletValue = eWalletValue;
    }

    public Long getCategoryCount() {
        return categoryCount;
    }

    public void setCategoryCount(Long categoryCount) {
        this.categoryCount = categoryCount;
    }

    public Double getCategoryTotal() {
        return categoryTotal;
    }

    public void setCategoryTotal(Double categoryTotal) {
        this.categoryTotal = categoryTotal;
    }

    public Double getCategoryHospitalFee() {
        return categoryHospitalFee;
    }

    public void setCategoryHospitalFee(Double categoryHospitalFee) {
        this.categoryHospitalFee = categoryHospitalFee;
    }

    public Double getCategoryDiscountAmount() {
        return categoryDiscountAmount;
    }

    public void setCategoryDiscountAmount(Double categoryDiscountAmount) {
        this.categoryDiscountAmount = categoryDiscountAmount;
    }

    public Double getCategoryCollectingCentreFee() {
        return categoryCollectingCentreFee;
    }

    public void setCategoryCollectingCentreFee(Double categoryCollectingCentreFee) {
        this.categoryCollectingCentreFee = categoryCollectingCentreFee;
    }

    public Double getCategoryDiscount() {
        return categoryDiscount;
    }

    public void setCategoryDiscount(Double categoryDiscount) {
        this.categoryDiscount = categoryDiscount;
    }

    public Double getCategoryProfessionalFee() {
        return categoryProfessionalFee;
    }

    public void setCategoryProfessionalFee(Double categoryProfessionalFee) {
        this.categoryProfessionalFee = categoryProfessionalFee;
    }

    public Double getCategoryNetTotal() {
        return categoryNetTotal;
    }

    public void setCategoryNetTotal(Double categoryNetTotal) {
        this.categoryNetTotal = categoryNetTotal;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }

    public Double getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(Double itemTotal) {
        this.itemTotal = itemTotal;
    }

    public Double getItemHospitalFee() {
        return itemHospitalFee;
    }

    public void setItemHospitalFee(Double itemHospitalFee) {
        this.itemHospitalFee = itemHospitalFee;
    }

    public Double getItemDiscountAmount() {
        return itemDiscountAmount;
    }

    public void setItemDiscountAmount(Double itemDiscountAmount) {
        this.itemDiscountAmount = itemDiscountAmount;
    }

    public Double getItemCollectingCentreFee() {
        return itemCollectingCentreFee;
    }

    public void setItemCollectingCentreFee(Double itemCollectingCentreFee) {
        this.itemCollectingCentreFee = itemCollectingCentreFee;
    }

    public Double getItemDiscount() {
        return itemDiscount;
    }

    public void setItemDiscount(Double itemDiscount) {
        this.itemDiscount = itemDiscount;
    }

    public Double getItemProfessionalFee() {
        return itemProfessionalFee;
    }

    public void setItemProfessionalFee(Double itemProfessionalFee) {
        this.itemProfessionalFee = itemProfessionalFee;
    }

    public Double getItemNetTotal() {
        return itemNetTotal;
    }

    public void setItemNetTotal(Double itemNetTotal) {
        this.itemNetTotal = itemNetTotal;
    }

    public String getRowType() {
        return rowType;
    }

    public void setRowType(String rowType) {
        this.rowType = rowType;
    }

}
