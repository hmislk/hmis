package com.divudi.core.data;

import com.divudi.core.entity.AgentHistory;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillSession;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.Route;
import com.divudi.core.entity.Speciality;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.channel.SessionInstance;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.StockHistory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author Dr M H B Ariyaratne
 */
public class PharmacyRow implements Serializable {

    private Long id;
    private UUID uuid;

    private Item item;
    private ItemBatch itemBatch;
    private Double quantity;
    private Double purchaseValue;
    private Double saleValue;
    private Double costValue;
    private Double stockQty;

    private Long counter;
    private String rowType;

    private PaymentScheme paymentScheme;
    private AdmissionType admissionType;
    private StockHistory stockHistory;

    private Category category;
    private Bill bill;
    private Bill batchBill;
    private Bill referanceBill;
    private BillItem billItem;
    private PharmaceuticalBillItem pharmaceuticalBillItem;
    private BillFee billFee;
    private Payment payment;

    private BillType billType;
    private BillClassType billClassType;
    BillTypeAtomic billTypeAtomic;

    private boolean selected;

    private double retailValue;

    private double grossProfit;

    private Long categoryCount;
    private double categoryTotal;
    private double categoryHospitalFee;
    private double categoryDiscountAmount;
    private double categoryCollectingCentreFee;
    private double categoryDiscount;
    private double categoryProfessionalFee;
    private double categoryNetTotal;

    private Long itemCount;
    private double itemTotal;
    private double itemHospitalFee;
    private double itemDiscountAmount;
    private double itemCollectingCentreFee;
    private double itemDiscount;
    private double itemProfessionalFee;
    private double itemNetTotal;

    private List<Payment> payments;

    private String feeName;
    private String categoryName;
    private String toDepartmentName;
    private String itemName;
    private String paymentName;
    private double rowValue;
    private double rowValueIn;
    private double rowValueOut;
    private Long rowCountIn;
    private Long rowCountOut;
    private Long rowCount;

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
    private Institution creditCompany;
    private Department toDepartment;
    private Department itemDepartment;
    private BillSession billSession;
    private Department department;
    private Institution institution;
    private Institution site;
    private AgentHistory agentHistory;

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
    private Speciality speciality;
    private Staff staff;
    private Institution referringInstitution;
    private Staff referringStaff;

    private PatientInvestigation patientInvestigation;
    private Route route;
    private Institution collectingCentre;

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
    private double noneValue;
    private double opdCreditValue;
    private double inpatientCreditValue;

    private double grossTotal;
    private double discount;
    private double serviceCharge;
    private double tax;
    private double actualTotal;
    private double netTotal;
    private double paidTotal;

    private double hospitalTotal;
    private double staffTotal;
    private double ccTotal;

    private double purchaseRate;
    private double retailRate;
    private double costRate;

    private double qty;

    private long duration;


    private BigDecimal grossSaleRate = BigDecimal.ZERO;
    private BigDecimal discountRate = BigDecimal.ZERO;
    private BigDecimal marginRate = BigDecimal.ZERO;
    private BigDecimal netSaleRate = BigDecimal.ZERO;
    private BigDecimal grossSaleValue = BigDecimal.ZERO;
    private BigDecimal marginValue = BigDecimal.ZERO;
    private BigDecimal discountValue = BigDecimal.ZERO;
    private BigDecimal netSaleValue = BigDecimal.ZERO;


    public PharmacyRow() {
        this.uuid = UUID.randomUUID();
    }

    public PharmacyRow(StockHistory stockHistory) {
        this.stockHistory = stockHistory;
    }

    public PharmacyRow(Long id) {
        this.id = id;
    }

    public StockHistory getStockHistory() {
        return stockHistory;
    }

    public void setStockHistory(StockHistory stockHistory) {
        this.stockHistory = stockHistory;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemBatch getItemBatch() {
        return itemBatch;
    }

    public void setItemBatch(ItemBatch itemBatch) {
        this.itemBatch = itemBatch;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(Double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public Double getSaleValue() {
        return saleValue;
    }

    public void setSaleValue(Double saleValue) {
        this.saleValue = saleValue;
    }

    public PharmacyRow(Bill bill) {
        this();
        this.bill = bill;
        rowType = "Bill";
    }

    public PharmacyRow(BillItem billItem) {
        this();
        this.billItem = billItem;
        rowType = "BillItem";
    }

    public PharmacyRow(PharmaceuticalBillItem pbi) {
        this();
        this.pharmaceuticalBillItem = pbi;
        rowType = "PharmaceuticalBillItem";
    }

    public PharmacyRow(SessionInstance sessionInstance) {
        this();
        this.sessionInstance = sessionInstance;
    }

    public PharmacyRow(BillItem billItem, boolean withBill) {
        this();
        this.billItem = billItem;
        if (withBill) {
            rowType = "BillItemWithBill";
            this.bill = billItem.getBill();
        } else {
            rowType = "BillItem";
        }
    }

    public PharmacyRow(Payment payment) {
        this();
        this.payment = payment;
    }

    public PharmacyRow(BillFee billFee) {
        this();
        this.billFee = billFee;
    }

    public UUID getUUId() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PharmacyRow that = (PharmacyRow) o;

        return Objects.equals(this.uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

    @Override
    public String toString() {
        return "PharmacyRow{id=" + id + ", uuid=" + uuid + '}';
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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getPaymentName() {
        return paymentName;
    }

    public void setPaymentName(String paymentName) {
        this.paymentName = paymentName;
    }

    public double getRowValue() {
        return rowValue;
    }

    public void setRowValue(double rowValue) {
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

    public PharmacyRow(BillSession billSession) {
        this.billSession = billSession;
    }

    public void setBillSession(BillSession billSession) {
        this.billSession = billSession;
    }

    public PharmacyRow(String feeName, String categoryName, String toDepartmentName, String itemName, String paymentName, double rowValue, Long rowCount) {
        this.feeName = feeName;
        this.categoryName = categoryName;
        this.toDepartmentName = toDepartmentName;
        this.itemName = itemName;
        this.paymentName = paymentName;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public PharmacyRow(String categoryName, Long rowCount, double rowValue) {
        this.categoryName = categoryName;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public PharmacyRow(Category category, Long rowCount, double rowValue) {
        this.category = category;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public PharmacyRow(Department itemDept, Long rowCount, double rowValue) {
        this.itemDepartment = itemDept;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public PharmacyRow(Item item, Long rowCount, double rowValue) {
        this.item = item;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public PharmacyRow(String categoryName, double rowValue) {
        this.categoryName = categoryName;
        this.rowValue = rowValue;
    }

    public PharmacyRow(BillTypeAtomic billTypeAtomic, String categoryName, String toDepartmentName, double rowValue) {
        this.billTypeAtomic = billTypeAtomic;
        this.categoryName = categoryName;
        this.toDepartmentName = toDepartmentName;
        this.rowValue = rowValue;
    }

    public PharmacyRow(BillTypeAtomic billTypeAtomic, double rowValue) {
        this.billTypeAtomic = billTypeAtomic;
        this.rowValue = rowValue;
    }

    public PharmacyRow(BillTypeAtomic billTypeAtomic, Long rowCount, double rowValue) {
        this.rowValue = rowValue;
        this.rowCount = rowCount;
        this.billTypeAtomic = billTypeAtomic;
    }

    public PharmacyRow(double rowValue) {
        this.rowValue = rowValue;
    }

    public PharmacyRow(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
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

    public double getRowValueIn() {
        return rowValueIn;
    }

    public void setRowValueIn(double rowValueIn) {
        this.rowValueIn = rowValueIn;
    }

    public double getRowValueOut() {
        return rowValueOut;
    }

    public void setRowValueOut(double rowValueOut) {
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

    public PharmacyRow(Staff staff, Long long1, Long long2, Long long3, Long long4, Long long5, Long long6, Long long7) {
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

    public double getCategoryTotal() {
        return categoryTotal;
    }

    public void setCategoryTotal(double categoryTotal) {
        this.categoryTotal = categoryTotal;
    }

    public double getCategoryHospitalFee() {
        return categoryHospitalFee;
    }

    public void setCategoryHospitalFee(double categoryHospitalFee) {
        this.categoryHospitalFee = categoryHospitalFee;
    }

    public double getCategoryDiscountAmount() {
        return categoryDiscountAmount;
    }

    public void setCategoryDiscountAmount(double categoryDiscountAmount) {
        this.categoryDiscountAmount = categoryDiscountAmount;
    }

    public double getCategoryCollectingCentreFee() {
        return categoryCollectingCentreFee;
    }

    public void setCategoryCollectingCentreFee(double categoryCollectingCentreFee) {
        this.categoryCollectingCentreFee = categoryCollectingCentreFee;
    }

    public double getCategoryDiscount() {
        return categoryDiscount;
    }

    public void setCategoryDiscount(double categoryDiscount) {
        this.categoryDiscount = categoryDiscount;
    }

    public double getCategoryProfessionalFee() {
        return categoryProfessionalFee;
    }

    public void setCategoryProfessionalFee(double categoryProfessionalFee) {
        this.categoryProfessionalFee = categoryProfessionalFee;
    }

    public double getCategoryNetTotal() {
        return categoryNetTotal;
    }

    public void setCategoryNetTotal(double categoryNetTotal) {
        this.categoryNetTotal = categoryNetTotal;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }

    public double getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(double itemTotal) {
        this.itemTotal = itemTotal;
    }

    public double getItemHospitalFee() {
        return itemHospitalFee;
    }

    public void setItemHospitalFee(double itemHospitalFee) {
        this.itemHospitalFee = itemHospitalFee;
    }

    public double getItemDiscountAmount() {
        return itemDiscountAmount;
    }

    public void setItemDiscountAmount(double itemDiscountAmount) {
        this.itemDiscountAmount = itemDiscountAmount;
    }

    public double getItemCollectingCentreFee() {
        return itemCollectingCentreFee;
    }

    public void setItemCollectingCentreFee(double itemCollectingCentreFee) {
        this.itemCollectingCentreFee = itemCollectingCentreFee;
    }

    public double getItemDiscount() {
        return itemDiscount;
    }

    public void setItemDiscount(double itemDiscount) {
        this.itemDiscount = itemDiscount;
    }

    public double getItemProfessionalFee() {
        return itemProfessionalFee;
    }

    public void setItemProfessionalFee(double itemProfessionalFee) {
        this.itemProfessionalFee = itemProfessionalFee;
    }

    public double getItemNetTotal() {
        return itemNetTotal;
    }

    public void setItemNetTotal(double itemNetTotal) {
        this.itemNetTotal = itemNetTotal;
    }

    public String getRowType() {
        return rowType;
    }

    public void setRowType(String rowType) {
        this.rowType = rowType;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Long getCounter() {
        return counter;
    }

    public void setCounter(Long counter) {
        this.counter = counter;
    }

    public BillFee getBillFee() {
        return billFee;
    }

    public void setBillFee(BillFee billFee) {
        this.billFee = billFee;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public PharmacyRow(AgentHistory agentHistory) {
        this.agentHistory = agentHistory;
    }

    public double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getHospitalTotal() {
        return hospitalTotal;
    }

    public void setHospitalTotal(double hospitalTotal) {
        this.hospitalTotal = hospitalTotal;
    }

    public double getStaffTotal() {
        return staffTotal;
    }

    public void setStaffTotal(double staffTotal) {
        this.staffTotal = staffTotal;
    }

    public double getCcTotal() {
        return ccTotal;
    }

    public void setCcTotal(double ccTotal) {
        this.ccTotal = ccTotal;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public AgentHistory getAgentHistory() {
        return agentHistory;
    }

    public void setAgentHistory(AgentHistory agentHistory) {
        this.agentHistory = agentHistory;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public void setPatientInvestigation(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
    }

    public double getNoneValue() {
        return noneValue;
    }

    public void setNoneValue(double noneValue) {
        this.noneValue = noneValue;
    }

    public double getOpdCreditValue() {
        return opdCreditValue;
    }

    public void setOpdCreditValue(double opdCreditValue) {
        this.opdCreditValue = opdCreditValue;
    }

    public double getInpatientCreditValue() {
        return inpatientCreditValue;
    }

    public void setInpatientCreditValue(double inpatientCreditValue) {
        this.inpatientCreditValue = inpatientCreditValue;
    }

    public double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public double getActualTotal() {
        return actualTotal;
    }

    public void setActualTotal(double actualTotal) {
        this.actualTotal = actualTotal;
    }

    public List<Payment> getPayments() {
        if (payments == null) {
            payments = new ArrayList<>();
        }
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public Bill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(Bill batchBill) {
        this.batchBill = batchBill;
    }

    public Bill getReferanceBill() {
        return referanceBill;
    }

    public void setReferanceBill(Bill referanceBill) {
        this.referanceBill = referanceBill;
    }

    public double getPaidTotal() {
        return paidTotal;
    }

    public void setPaidTotal(double paidTotal) {
        this.paidTotal = paidTotal;
    }

    public PharmaceuticalBillItem getPharmaceuticalBillItem() {
        return pharmaceuticalBillItem;
    }

    public void setPharmaceuticalBillItem(PharmaceuticalBillItem pharmaceuticalBillItem) {
        this.pharmaceuticalBillItem = pharmaceuticalBillItem;
    }

    public double getRetailValue() {
        return retailValue;
    }

    public void setRetailValue(double retailValue) {
        this.retailValue = retailValue;
    }

    public double getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(double grossProfit) {
        this.grossProfit = grossProfit;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public Double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(double retailRate) {
        this.retailRate = retailRate;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public BigDecimal getGrossSaleRate() {
        return grossSaleRate;
    }

    public void setGrossSaleRate(BigDecimal grossSaleRate) {
        this.grossSaleRate = grossSaleRate;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public BigDecimal getMarginRate() {
        return marginRate;
    }

    public void setMarginRate(BigDecimal marginRate) {
        this.marginRate = marginRate;
    }

    public BigDecimal getNetSaleRate() {
        return netSaleRate;
    }

    public void setNetSaleRate(BigDecimal netSaleRate) {
        this.netSaleRate = netSaleRate;
    }

    public BigDecimal getGrossSaleValue() {
        return grossSaleValue;
    }

    public void setGrossSaleValue(BigDecimal grossSaleValue) {
        this.grossSaleValue = grossSaleValue;
    }

    public BigDecimal getMarginValue() {
        return marginValue;
    }

    public void setMarginValue(BigDecimal marginValue) {
        this.marginValue = marginValue;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getNetSaleValue() {
        return netSaleValue;
    }

    public void setNetSaleValue(BigDecimal netSaleValue) {
        this.netSaleValue = netSaleValue;
    }

    public Double getStockQty() {
        return stockQty;
    }

    public void setStockQty(Double stockQty) {
        this.stockQty = stockQty;
    }

    public Double getCostRate() {
        return costRate;
    }

    public void setCostRate(double costRate) {
        this.costRate = costRate;
    }

    public Double getCostValue() {
        return costValue;
    }

    public void setCostValue(Double costValue) {
        this.costValue = costValue;
    }
}
