package com.divudi.core.data;

import com.divudi.core.data.dataStructure.InvestigationDetails;
import com.divudi.core.data.dto.PharmacyIncomeCostBillDTO;
import com.divudi.core.data.dto.PharmacyIncomeBillDTO;
import com.divudi.core.data.dto.PharmacyIncomeBillItemDTO;
import com.divudi.core.data.dto.OpdIncomeReportDTO;
import com.divudi.core.data.dto.LabIncomeReportDTO;
import com.divudi.core.entity.*;
import com.divudi.core.entity.channel.SessionInstance;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author buddhika
 */
public class IncomeRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String uuid;
    private Long counter;
    String rowType;

    private PaymentScheme paymentScheme;
    private AdmissionType admissionType;

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

    // Fields populated from PharmacyIncomeCostBillDTO
    private Long billId;
    private String billNo;
    private String patientName;
    private String bhtNo;
    private Date createdAt;
    private String deptId;
    private Double itemValue;

    private boolean selected;

    private Item item;

    private double retailValue;
    private double purchaseValue;
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
    private PatientEncounter patientEncounter;

    private PaymentMethod paymentMethod;

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
    private double otherValue;

    private double grossTotal;
    private double discount;
    private double margin;
    private double serviceCharge;
    private double tax;
    private double actualTotal;
    private double netTotal;
    private double paidTotal;
    private double total;

    private double hospitalTotal;
    private double staffTotal;
    private double ccTotal;

    private double totalBillsRefund;
    private double totalBillsCancel;
    private double totalBillsDiscount;

    private double qty;
    private double retailRate;
    private double purchaseRate;
    private double netRate;

    private double totalRetailSaleValue;
    private double totalPurchaseValue;

    private long duration;

    private BillFinanceDetails billFinanceDetails;

    private UUID id;

    // Constructor to generate a new UUID when an object is created
    public IncomeRow() {
        this.id = UUID.randomUUID();
    }

    public IncomeRow(Bill bill) {
        this();
        this.bill = bill;
        rowType = "Bill";
    }

    public IncomeRow(BillItem billItem) {
        this();
        this.billItem = billItem;
        rowType = "BillItem";
    }

    public IncomeRow(PharmaceuticalBillItem pbi) {
        this();
        this.pharmaceuticalBillItem = pbi;
        rowType = "PharmaceuticalBillItem";
    }

    public IncomeRow(SessionInstance sessionInstance) {
        this();
        this.sessionInstance = sessionInstance;
    }

    public IncomeRow(BillItem billItem, boolean withBill) {
        this();
        this.billItem = billItem;
        if (withBill) {
            rowType = "BillItemWithBill";
            this.bill = billItem.getBill();
        } else {
            rowType = "BillItem";
        }
    }

    public IncomeRow(Payment payment) {
        this();
        this.payment = payment;
    }

    public IncomeRow(BillFee billFee) {
        this();
        this.billFee = billFee;
    }

    public IncomeRow(InvestigationDetails dto) {
        this();
        if (dto != null) {
            this.billId = dto.getId();
            this.billNo = dto.getBillNumber();
            this.createdAt = dto.getBillDate();
            this.patientName = dto.getPatientName();
            this.itemValue = dto.getItemAmount();
        }
    }

    public IncomeRow(PharmacyIncomeCostBillDTO dto) {
        this();
        if (dto != null) {
            this.billId = dto.getId();
            this.billNo = dto.getBillNo();
            this.billTypeAtomic = dto.getBillTypeAtomic();
            this.patientName = dto.getPatientName();
            this.bhtNo = dto.getBhtNo();
            this.createdAt = dto.getCreatedAt();
            if (dto.getRetailValue() != null) {
                this.retailValue = dto.getRetailValue().doubleValue();
            }
            if (dto.getPurchaseValue() != null) {
                this.purchaseValue = dto.getPurchaseValue().doubleValue();
            }
            this.grossProfit = this.retailValue - this.purchaseValue;
        }
    }

    public IncomeRow(PharmacyIncomeBillDTO dto) {
        this();

        Bill bill = new Bill();
        bill.setId(dto.getBillId());
        bill.setDeptId(dto.getDeptId());
        if (dto.getPatientName() != null) {
            Patient patient = new Patient();
            Person person = new Person();
            person.setName(dto.getPatientName());
            patient.setPerson(person);
            bill.setPatient(patient);
        }
        bill.setBillTypeAtomic(dto.getBillTypeAtomic());
        bill.setCreatedAt(dto.getCreatedAt());
        bill.setNetTotal(dto.getNetTotal());
        bill.setPaymentMethod(dto.getPaymentMethod());
        bill.setTotal(dto.getTotal());
        bill.setPatientEncounter(dto.getPatientEncounter());
        bill.setDiscount(dto.getDiscount() != null ? dto.getDiscount() : 0.0);
        bill.setMargin(dto.getMargin() != null ? dto.getMargin() : 0.0);
        bill.setServiceCharge(dto.getServiceCharge() != null ? dto.getServiceCharge() : 0.0);
        bill.setPaymentScheme(dto.getPaymentScheme());

        BillFinanceDetails billFinanceDetails = new BillFinanceDetails();
        billFinanceDetails.setTotalRetailSaleValue(dto.getTotalRetailSaleValue());
        billFinanceDetails.setTotalPurchaseValue(dto.getTotalPurchaseValue());
        bill.setBillFinanceDetails(billFinanceDetails);

        this.bill = bill;
    }

    public IncomeRow(OpdIncomeReportDTO dto) {
        this();

        Bill bill = new Bill();
        bill.setId(dto.getBillId());
        bill.setDeptId(dto.getDeptId());
        if (dto.getPatientName() != null) {
            Patient patient = new Patient();
            Person person = new Person();
            person.setName(dto.getPatientName());
            patient.setPerson(person);
            bill.setPatient(patient);
        }
        bill.setBillTypeAtomic(dto.getBillTypeAtomic());
        bill.setCreatedAt(dto.getCreatedAt());
        bill.setNetTotal(dto.getNetTotal());
        bill.setPaymentMethod(dto.getPaymentMethod());
        bill.setTotal(dto.getTotal());
        bill.setPatientEncounter(dto.getPatientEncounter());
        bill.setDiscount(dto.getDiscount() != null ? dto.getDiscount() : 0.0);
        bill.setMargin(dto.getMargin() != null ? dto.getMargin() : 0.0);
        bill.setServiceCharge(dto.getServiceCharge() != null ? dto.getServiceCharge() : 0.0);
        bill.setPaymentScheme(dto.getPaymentScheme());

        this.bill = bill;
    }

    public IncomeRow(LabIncomeReportDTO dto) {
        this();

        System.out.println("DEBUG: Creating IncomeRow from LabIncomeReportDTO");
        System.out.println("  - DTO billId: " + dto.getBillId());
        System.out.println("  - DTO billNumber: " + dto.getBillNumber());
        System.out.println("  - DTO billTypeAtomic: " + dto.getBillTypeAtomic());
        System.out.println("  - DTO paymentMethod: " + dto.getPaymentMethod());

        Bill bill = new Bill();
        bill.setId(dto.getBillId());
        bill.setDeptId(dto.getBillNumber());
        if (dto.getPatientName() != null) {
            Patient patient = new Patient();
            Person person = new Person();
            person.setName(dto.getPatientName());
            patient.setPerson(person);
            bill.setPatient(patient);
        }
        bill.setBillTypeAtomic(dto.getBillTypeAtomic());
        bill.setCreatedAt(dto.getBillDate());
        bill.setNetTotal(dto.getNetTotal() != null ? dto.getNetTotal().doubleValue() : 0.0);
        bill.setPaymentMethod(dto.getPaymentMethod());
        bill.setTotal(dto.getTotal() != null ? dto.getTotal().doubleValue() : 0.0);
        bill.setPatientEncounter(dto.getPatientEncounter());
        bill.setDiscount(dto.getDiscount() != null ? dto.getDiscount() : 0.0);
        bill.setServiceCharge(dto.getServiceCharge() != null ? dto.getServiceCharge() : 0.0);
        bill.setPaymentScheme(dto.getPaymentScheme());

        this.bill = bill;
        System.out.println("  - Created Bill with id: " + bill.getId() + ", paymentMethod: " + bill.getPaymentMethod());
    }

    public IncomeRow(PharmacyIncomeBillItemDTO dto) {
        this();

        Bill bill = new Bill();
        BillItem billItem = new BillItem();

        bill.setId(dto.getBillId());
        billItem.setId(dto.getBillItemId());
        bill.setDeptId(dto.getDeptId());

        Patient patient = new Patient();
        Person person = new Person();
        person.setName(dto.getPatientName());
        patient.setPerson(person);
        bill.setPatient(patient);

        bill.setBillTypeAtomic(dto.getBillTypeAtomic());
        bill.setCreatedAt(dto.getCreatedAt());
        bill.setNetTotal(dto.getNetTotal());
        bill.setPaymentMethod(dto.getPaymentMethod());
        bill.setTotal(dto.getTotal());
        bill.setPatientEncounter(dto.getPatientEncounter());

        PharmaceuticalBillItem pharmaceuticalBillItem = new PharmaceuticalBillItem();
        pharmaceuticalBillItem.setQty(dto.getQty() != null ? dto.getQty() : 0.0);

        ItemBatch itemBatch = new ItemBatch();
        itemBatch.setCostRate(dto.getCostRate() != null ? dto.getCostRate() : 0.0);
        pharmaceuticalBillItem.setItemBatch(itemBatch);

        pharmaceuticalBillItem.setRetailRate(dto.getRetailRate() != null ? dto.getRetailRate() : 0.0);
        pharmaceuticalBillItem.setPurchaseRate(dto.getPurchaseRate() != null ? dto.getPurchaseRate() : 0.0);

        billItem.setNetRate(dto.getNetRate() != null ? dto.getNetRate() : 0.0);
        Item item = new Item();
        item.setName(dto.getItemName());
        billItem.setItem(item);

        billItem.setBill(bill);
        pharmaceuticalBillItem.setBillItem(billItem);
        billItem.setPharmaceuticalBillItem(pharmaceuticalBillItem);

        this.pharmaceuticalBillItem = pharmaceuticalBillItem;
    }

    // Getter for UUID (optional, depending on use case)
    public UUID getId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        return id;
    }

    // Override equals() using UUID field
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IncomeRow that = (IncomeRow) o;
        return Objects.equals(getId(), that.id);
    }

    // Override hashCode() using UUID field
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    // Override toString() for better readability (optional)
    @Override
    public String toString() {
        return "ReportTemplateRow{id=" + getId() + '}';
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

    public IncomeRow(BillSession billSession) {
        this.billSession = billSession;
    }

    public void setBillSession(BillSession billSession) {
        this.billSession = billSession;
    }

    public IncomeRow(String feeName, String categoryName, String toDepartmentName, String itemName, String paymentName, double rowValue, Long rowCount) {
        this.feeName = feeName;
        this.categoryName = categoryName;
        this.toDepartmentName = toDepartmentName;
        this.itemName = itemName;
        this.paymentName = paymentName;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public IncomeRow(String categoryName, Long rowCount, double rowValue) {
        this.categoryName = categoryName;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public IncomeRow(Category category, Long rowCount, double rowValue) {
        this.category = category;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public IncomeRow(Department itemDept, Long rowCount, double rowValue) {
        this.itemDepartment = itemDept;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public IncomeRow(Item item, Long rowCount, double rowValue) {
        this.item = item;
        this.rowValue = rowValue;
        this.rowCount = rowCount;
    }

    public IncomeRow(String categoryName, double rowValue) {
        this.categoryName = categoryName;
        this.rowValue = rowValue;
    }

    public IncomeRow(BillTypeAtomic billTypeAtomic, String categoryName, String toDepartmentName, double rowValue) {
        this.billTypeAtomic = billTypeAtomic;
        this.categoryName = categoryName;
        this.toDepartmentName = toDepartmentName;
        this.rowValue = rowValue;
    }

    public IncomeRow(BillTypeAtomic billTypeAtomic, double rowValue) {
        this.billTypeAtomic = billTypeAtomic;
        this.rowValue = rowValue;
    }

    public IncomeRow(BillTypeAtomic billTypeAtomic, Long rowCount, double rowValue) {
        this.rowValue = rowValue;
        this.rowCount = rowCount;
        this.billTypeAtomic = billTypeAtomic;
    }

    public IncomeRow(double rowValue) {
        this.rowValue = rowValue;
    }

    public IncomeRow(BillTypeAtomic billTypeAtomic) {
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

    public IncomeRow(Staff staff, Long long1, Long long2, Long long3, Long long4, Long long5, Long long6, Long long7) {
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

    public IncomeRow(AgentHistory agentHistory) {
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

    public double getMargin() {
        return margin;
    }

    public void setMargin(double margin) {
        this.margin = margin;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
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

    public double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(double purchaseValue) {
        this.purchaseValue = purchaseValue;
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

    public double getTotalBillsRefund() {
        return totalBillsRefund;
    }

    public void setTotalBillsRefund(double totalBillsRefund) {
        this.totalBillsRefund = totalBillsRefund;
    }

    public double getTotalBillsCancel() {
        return totalBillsCancel;
    }

    public void setTotalBillsCancel(double totalBillsCancel) {
        this.totalBillsCancel = totalBillsCancel;
    }

    public double getTotalBillsDiscount() {
        return totalBillsDiscount;
    }

    public void setTotalBillsDiscount(double totalBillsDiscount) {
        this.totalBillsDiscount = totalBillsDiscount;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public BillFinanceDetails getBillFinanceDetails() {
        return billFinanceDetails;
    }

    public void setBillFinanceDetails(BillFinanceDetails billFinanceDetails) {
        this.billFinanceDetails = billFinanceDetails;
    }

    public double getPurchaseRate() {
        return purchaseRate;
    }

    public void setPurchaseRate(double purchaseRate) {
        this.purchaseRate = purchaseRate;
    }

    public double getRetailRate() {
        return retailRate;
    }

    public void setRetailRate(double retailRate) {
        this.retailRate = retailRate;
    }

    public double getNetRate() {
        return netRate;
    }

    public void setNetRate(double netRate) {
        this.netRate = netRate;
    }

    public double getTotalRetailSaleValue() {
        return totalRetailSaleValue;
    }

    public void setTotalRetailSaleValue(double totalRetailSaleValue) {
        this.totalRetailSaleValue = totalRetailSaleValue;
    }

    public double getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(double totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public Double getItemValue() {
        return itemValue;
    }

    public void setItemValue(Double itemValue) {
        this.itemValue = itemValue;
    }

    public double getOtherValue() {
        return otherValue;
    }

    public void setOtherValue(double otherValue) {
        this.otherValue = otherValue;
    }
}
