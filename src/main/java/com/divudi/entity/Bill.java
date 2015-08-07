/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.inward.SurgeryBillType;
import com.divudi.entity.cashTransaction.CashTransaction;
import com.divudi.entity.memberShip.MembershipScheme;
import com.divudi.entity.pharmacy.StockVarientBillItem;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.annotations.CacheType;

/**
 *
 * @author buddhika
 */
@Entity
@Table(name = "bill")
@NamedQueries({
    @NamedQuery(name = "Bill.findAll", query = "SELECT b FROM Bill b"),
    @NamedQuery(name = "Bill.findById", query = "SELECT b FROM Bill b WHERE b.id = :id")})
public class Bill implements Serializable {

    @ManyToOne
    MembershipScheme membershipScheme;
    @OneToOne
    private CashTransaction cashTransaction;
    @OneToMany(mappedBy = "bill", fetch = FetchType.LAZY)
    private List<StockVarientBillItem> stockVarientBillItems = new ArrayList<>();
    @OneToMany(mappedBy = "backwardReferenceBill", fetch = FetchType.LAZY)
    private List<Bill> forwardReferenceBills = new ArrayList<>();
    @OneToMany(mappedBy = "forwardReferenceBill", fetch = FetchType.LAZY)
    private List<Bill> backwardReferenceBills = new ArrayList<>();
    @OneToMany(mappedBy = "billedBill", fetch = FetchType.LAZY)
    private List<Bill> returnPreBills = new ArrayList<>();
    @OneToMany(mappedBy = "billedBill", fetch = FetchType.LAZY)
    private List<Bill> returnBhtIssueBills = new ArrayList<>();
    @OneToMany(mappedBy = "referenceBill", fetch = FetchType.LAZY)
    private List<Bill> returnCashBills = new ArrayList<>();
    @OneToMany(mappedBy = "referenceBill", fetch = FetchType.LAZY)
    private List<Bill> cashBillsPre = new ArrayList<>();
    @OneToMany(mappedBy = "referenceBill", fetch = FetchType.LAZY)
    private List<Bill> cashBillsOpdPre = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    BillClassType billClassType;

    @ManyToOne
    BatchBill batchBill;

    @ManyToOne
    private Category category;
    @Transient
    boolean transError;

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    ////////////////////////////////////////////////////
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Payment> payments = new ArrayList<>();
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<BillFee> billFees = new ArrayList<>();
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("inwardChargeType, searialNo")
    List<BillItem> billItems;

    @OneToMany(mappedBy = "expenseBill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("searialNo")
    List<BillItem> billExpenses;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<BillComponent> billComponents = new ArrayList<>();
    ////////////////////////////////////////////////   
    @Lob
    String comments;
    // Bank Detail
    String creditCardRefNo;
    String chequeRefNo;
    @ManyToOne
    Institution bank;
    @Temporal(javax.persistence.TemporalType.DATE)
    Date chequeDate;
    //Approve
    @ManyToOne
    WebUser approveUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date approveAt;
    //Pharmacy
    @Temporal(javax.persistence.TemporalType.DATE)
    Date invoiceDate;
    //Enum
    @Enumerated(EnumType.STRING)
    BillType billType;
    @Enumerated(EnumType.STRING)
    PaymentMethod paymentMethod;
    @ManyToOne
    BillItem singleBillItem;
    @ManyToOne
    BillSession singleBillSession;
    String qutationNumber;
    @ManyToOne
    Institution referredByInstitution;
    @Column(name = "referralID")
    String referralNumber;

    //Values
    double total;
    double margin;
    double discount;
    double discountPercent;
    double netTotal;
    double billTotal;
    double paidAmount;
    double balance;
    double serviceCharge;
    Double tax = 0.0;
    Double cashPaid = 0.0;
    Double cashBalance = 0.0;
    double saleValue = 0.0f;
    double freeValue = 0.0f;
    double performInstitutionFee;
    double staffFee;
    double billerFee;
    double grantTotal;
    double expenseTotal;
    //with minus tax and discount
    double grnNetTotal;

    //Institution
    @ManyToOne
    Institution paymentSchemeInstitution;
    @ManyToOne
    Institution collectingCentre;
    @ManyToOne
    Institution institution;
    @ManyToOne
    Institution fromInstitution;
    @ManyToOne
    Institution toInstitution;
    @ManyToOne
    Institution creditCompany;
    @ManyToOne
    Institution referenceInstitution;
    //Departments
    @ManyToOne
    Department referringDepartment;
    @ManyToOne
    Department department;
    @ManyToOne
    Department fromDepartment;
    @ManyToOne
    Department toDepartment;
    //Bill
    @ManyToOne(fetch = FetchType.LAZY)
    Bill billedBill;
    @ManyToOne
    Bill cancelledBill;
    @ManyToOne
    Bill refundedBill;
    @ManyToOne
    Bill reactivatedBill;

    @ManyToOne
    Bill referenceBill;
    //Id's
    String deptId;
    String insId;
    String catId;
    String sessionId;
    String bookingId;
    String invoiceNumber;
    @Transient
    int intInvoiceNumber;
    //Staff
    @ManyToOne
    Staff staff;
    @ManyToOne
    Staff fromStaff;
    @ManyToOne
    Staff toStaff;
    //Booleans
    boolean cancelled;
    boolean refunded;
    boolean reactivated;
    //Created Properties
    @ManyToOne
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    //Edited Properties
    @ManyToOne
    private WebUser editor;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date editedAt;
    //Checking Property
    @ManyToOne
    private WebUser checkedBy;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date checkeAt;
    //Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;
    ////////////////
    @ManyToOne
    PaymentScheme paymentScheme;
    @Temporal(javax.persistence.TemporalType.DATE)
    Date billDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date billTime;
    @Transient
    String billClass;
    @ManyToOne
    Item billPackege;//BILLPACKEGE_ID
    @ManyToOne
    Person person;
    @ManyToOne
    Patient patient;
    @ManyToOne
    Doctor referredBy;
    @ManyToOne
    PatientEncounter patientEncounter;
    @ManyToOne
    private PatientEncounter procedure;
    @Transient
    List<Bill> listOfBill;

    @Transient
    private List<BillItem> transActiveBillItem;

    @ManyToOne
    private Bill forwardReferenceBill;

    @ManyToOne
    private Bill backwardReferenceBill;
    private double hospitalFee;
    private double professionalFee;
    @Transient
    private double tmpReturnTotal;
    @Transient
    private boolean transBoolean;
    @Enumerated(EnumType.STRING)
    private SurgeryBillType surgeryBillType;
    @ManyToOne
    private WebUser toWebUser;
    @ManyToOne
    private WebUser fromWebUser;
    double claimableTotal;

    //Denormalization
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date appointmentAt;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date paidAt;
    @ManyToOne
    Bill paidBill;
    double qty;
    @Transient
    double transTotalSaleValue;
    
    //Sms Info
    private Boolean smsed = false;
    @ManyToOne
    private WebUser smsedUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date smsedAt;
    @OneToMany(mappedBy = "bill")
    private List<Sms> sentSmses;

    public double getTransTotalSaleValue() {
        return transTotalSaleValue;
    }

    public void setTransTotalSaleValue(double transTotalSaleValue) {
        this.transTotalSaleValue = transTotalSaleValue;
    }
    
    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public void invertQty() {
        this.qty = 0 - qty;
    }

    public MembershipScheme getMembershipScheme() {
        return membershipScheme;
    }

    public void setMembershipScheme(MembershipScheme membershipScheme) {
        this.membershipScheme = membershipScheme;
    }

    public double getBillTotal() {
        return billTotal;
    }

    public void setBillTotal(double billTotal) {
        this.billTotal = billTotal;
    }
    
    

    private boolean paid;

    public Bill getPaidBill() {
        return paidBill;
    }

    public void setPaidBill(Bill paidBill) {
        this.paidBill = paidBill;
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
    }

    public WebUser getCheckedBy() {
        return checkedBy;
    }

    public void setCheckedBy(WebUser checkedBy) {
        this.checkedBy = checkedBy;
    }

    public Date getCheckeAt() {
        return checkeAt;
    }

    public void setCheckeAt(Date checkeAt) {
        this.checkeAt = checkeAt;
    }

    public double getAdjustedTotal() {
        return claimableTotal;
    }

    public void setAdjustedTotal(double dbl) {
        claimableTotal = dbl;
    }

    public BillItem getSingleBillItem() {
        return singleBillItem;
    }

    public void setSingleBillItem(BillItem singleBillItem) {
        this.singleBillItem = singleBillItem;
    }

    public double getClaimableTotal() {
        return claimableTotal;
    }

    public void setClaimableTotal(double claimableTotal) {
        this.claimableTotal = claimableTotal;
    }

    public void invertValue(Bill bill) {
        staffFee = 0 - bill.getStaffFee();
        performInstitutionFee = 0 - bill.getPerformInstitutionFee();
        billerFee = 0 - bill.getBillerFee();
        discount = 0 - bill.getDiscount();
        netTotal = 0 - bill.getNetTotal();
        total = 0 - bill.getTotal();
        discountPercent = 0 - bill.getDiscountPercent();
        paidAmount = 0 - bill.getPaidAmount();
        balance = 0 - bill.getBalance();
        cashPaid = 0 - bill.getCashPaid();
        cashBalance = 0 - bill.getCashBalance();
        saleValue = 0 - bill.getSaleValue();
        freeValue = 0 - bill.getFreeValue();
        grantTotal = 0 - bill.getGrantTotal();
        staffFee = 0 - bill.getStaffFee();
        hospitalFee = 0 - bill.getHospitalFee();
        margin = 0 - bill.getMargin();
        grnNetTotal = 0 - bill.getGrnNetTotal();

    }

    public void invertValue() {
        staffFee = 0 - getStaffFee();
        performInstitutionFee = 0 - getPerformInstitutionFee();
        billerFee = 0 - getBillerFee();
        discount = 0 - getDiscount();
        netTotal = 0 - getNetTotal();
        total = 0 - getTotal();
        discountPercent = 0 - getDiscountPercent();
        paidAmount = 0 - getPaidAmount();
        balance = 0 - getBalance();
        cashPaid = 0 - getCashPaid();
        cashBalance = 0 - getCashBalance();
        saleValue = 0 - getSaleValue();
        freeValue = 0 - getFreeValue();
        grantTotal = 0 - getGrantTotal();
        staffFee = 0 - getStaffFee();
        hospitalFee = 0 - getHospitalFee();
        grnNetTotal = 0 - getGrnNetTotal();
    }

    public void copy(Bill bill) {
        billType = bill.getBillType();
        membershipScheme = bill.getMembershipScheme();
        collectingCentre = bill.getCollectingCentre();
        catId = bill.getCatId();
        creditCompany = bill.getCreditCompany();
        staff = bill.getStaff();
        toStaff = bill.getToStaff();
        fromStaff = bill.getFromStaff();
        toDepartment = bill.getToDepartment();
        toInstitution = bill.getToInstitution();
        fromDepartment = bill.getFromDepartment();
        fromInstitution = bill.getFromInstitution();
        discountPercent = bill.getDiscountPercent();
        patient = bill.getPatient();
        patientEncounter = bill.getPatientEncounter();
        referredBy = bill.getReferredBy();
        referringDepartment = bill.getReferringDepartment();
        surgeryBillType = bill.getSurgeryBillType();
        comments = bill.getComments();
        paymentMethod = bill.getPaymentMethod();
        paymentScheme = bill.getPaymentScheme();
        bank = bill.getBank();
        chequeDate = bill.getChequeDate();
        referenceInstitution = bill.getReferenceInstitution();
        bookingId = bill.getBookingId();
        appointmentAt = bill.getAppointmentAt();
        referredByInstitution = bill.getReferredByInstitution();
        invoiceNumber = bill.getInvoiceNumber();
        //      referenceBill=bill.getReferenceBill();
    }

    public void copyValue(Bill bill) {
        this.grantTotal = (bill.getGrantTotal());
        this.discount = (bill.getDiscount());
        this.netTotal = (bill.getNetTotal());
        this.total = (bill.getTotal());
        this.staffFee = bill.getStaffFee();
        this.hospitalFee = bill.getHospitalFee();
        this.margin = bill.getMargin();

    }

    public List<BillComponent> getBillComponents() {
        return billComponents;
    }

    public void setBillComponents(List<BillComponent> billComponents) {
        this.billComponents = billComponents;
    }

    public boolean checkActiveForwardReference() {
        for (Bill b : getForwardReferenceBills()) {
            if (b.getCreater() != null && !b.isCancelled() && !b.isRetired()) {
                return true;
            }
        }
        return false;
    }

    public boolean checkActiveBackwardReference() {
        for (Bill b : getBackwardReferenceBills()) {
            if (b.getCreater() != null && !b.isCancelled() && !b.isRetired()) {
                return true;
            }
        }
        return false;
    }

    public Field getField(String name) {
        try {
            //System.err.println("ss : " + name);
            for (Field f : this.getClass().getFields()) {
                //System.err.println(f.getName());
            }
            return this.getClass().getField(name);
        } catch (NoSuchFieldException | SecurityException e) {
            //System.err.println("Ex no " + e.getMessage());
            return null;
        }
    }

    public double getTotal() {
        return total;
    }

    public double getTransSaleBillTotalMinusDiscount() {
        return total - discount;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<BillItem> getBillExpenses() {
        if (billExpenses == null) {
            billExpenses = new ArrayList<>();
        }
        return billExpenses;
    }

    public void setBillExpenses(List<BillItem> billExpenses) {
        this.billExpenses = billExpenses;
    }

    public double getExpenseTotal() {
        return expenseTotal;
    }

    public void setExpenseTotal(double expenseTotal) {
        this.expenseTotal = expenseTotal;
    }

    public double getDiscountPercentPharmacy() {
        //System.out.println("getting discount percent");
//        //System.out.println("bill item"+getBillItems());
//        //System.out.println(getBillItems().get(0).getPriceMatrix());
        if (!getBillItems().isEmpty() && getBillItems().get(0).getPriceMatrix() != null) {
            //System.out.println("sys inside");
            discountPercent = getBillItems().get(0).getPriceMatrix().getDiscountPercent();
        }

        return discountPercent;
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

    public double getBalance() {

        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public List<Bill> getListOfBill() {
        if (listOfBill == null) {
            listOfBill = new ArrayList<>();
        }
        return listOfBill;
    }

    public void setListOfBill(List<Bill> listOfBill) {
        this.listOfBill = listOfBill;
    }

    public int getIntInvoiceNumber() {
        return intInvoiceNumber;
    }

    public void setIntInvoiceNumber(int intInvoiceNumber) {
        this.intInvoiceNumber = intInvoiceNumber;
        invoiceNumber = intInvoiceNumber + "";
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Item getBillPackege() {
        return billPackege;
    }

    public void setBillPackege(Item billPackege) {
        this.billPackege = billPackege;
    }

    public String getBillClass() {
        return this.getClass().toString();
    }

    public void setBillClass(String billClass) {
        this.billClass = billClass;
    }

    public double getPerformInstitutionFee() {
        return performInstitutionFee;
    }

    public void setPerformInstitutionFee(double performInstitutionFee) {
        this.performInstitutionFee = performInstitutionFee;
    }

    public double getStaffFee() {
        return staffFee;
    }

    public void setStaffFee(double staffFee) {
        this.staffFee = staffFee;
    }

    public double getBillerFee() {
        return billerFee;
    }

    public void setBillerFee(double billerFee) {
        this.billerFee = billerFee;
    }

    public List<BillItem> getBillItems() {
        if (billItems == null) {
            billItems = new ArrayList<>();
        }

        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public Date getBillTime() {
        return billTime;
    }

    public void setBillTime(Date billTime) {
        this.billTime = billTime;
    }

    public Double getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(Double cashPaid) {
        this.cashPaid = cashPaid;

        cashBalance = netTotal - cashPaid;

    }

    public Double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(Double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
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

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Staff getFromStaff() {
        return fromStaff;
    }

    public void setFromStaff(Staff fromStaff) {
        this.fromStaff = fromStaff;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public Institution getPaymentSchemeInstitution() {
        return paymentSchemeInstitution;
    }

    public void setPaymentSchemeInstitution(Institution paymentSchemeInstitution) {
        this.paymentSchemeInstitution = paymentSchemeInstitution;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
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

        if (!(object instanceof Bill)) {
            return false;
        }
        Bill other = (Bill) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.Bill[ id=" + id + " ]";
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

    public Patient getPatient() {
        if (patientEncounter != null) {
            patient = patientEncounter.getPatient();
        }
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getReferredBy() {
        return referredBy;
    }

    public void setReferredBy(Doctor referredBy) {
        this.referredBy = referredBy;
    }

    public Department getReferringDepartment() {
        return referringDepartment;
    }

    public void setReferringDepartment(Department referringDepartment) {
        this.referringDepartment = referringDepartment;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public double getGrnNetTotal() {
        return grnNetTotal;
    }

    public void setGrnNetTotal(double grnNetTotal) {
        this.grnNetTotal = grnNetTotal;

    }

    public void calGrnNetTotal() {
        this.grnNetTotal = total + tax + discount;

    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getInsId() {
        return insId;
    }

    public void setInsId(String insId) {
        this.insId = insId;
    }

    public String getCatId() {
        return catId;
    }

    public void setCatId(String catId) {
        this.catId = catId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Bill getCancelledBill() {
        return cancelledBill;
    }

    public void setCancelledBill(Bill cancelledBill) {
        this.cancelledBill = cancelledBill;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isRefunded() {
        return refunded;
    }

    public void setRefunded(boolean refunded) {
        this.refunded = refunded;
    }

    public boolean isReactivated() {
        return reactivated;
    }

    public void setReactivated(boolean reactivated) {
        this.reactivated = reactivated;
    }

    public Bill getRefundedBill() {
        return refundedBill;
    }

    public void setRefundedBill(Bill refundedBill) {
        this.refundedBill = refundedBill;
    }

    public Bill getReactivatedBill() {
        return reactivatedBill;
    }

    public void setReactivatedBill(Bill reactivatedBill) {
        this.reactivatedBill = reactivatedBill;
    }

    public String getCreditCardRefNo() {
        return creditCardRefNo;
    }

    public void setCreditCardRefNo(String creditCardRefNo) {
        this.creditCardRefNo = creditCardRefNo;
    }

    public String getChequeRefNo() {
        return chequeRefNo;
    }

    public void setChequeRefNo(String chequeRefNo) {
        this.chequeRefNo = chequeRefNo;
    }

    public Institution getBank() {
        return bank;
    }

    public void setBank(Institution bank) {
        this.bank = bank;
    }

    public WebUser getApproveUser() {
        return approveUser;
    }

    public void setApproveUser(WebUser approveUser) {
        this.approveUser = approveUser;
    }

    public Date getApproveAt() {
        return approveAt;
    }

    public void setApproveAt(Date approveAt) {
        this.approveAt = approveAt;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Bill getReferenceBill() {
        return referenceBill;
    }

    public void setReferenceBill(Bill referenceBill) {
        this.referenceBill = referenceBill;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Date getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(Date invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public double getSaleValue() {
        return saleValue;
    }

    public void setSaleValue(double saleValue) {
        this.saleValue = saleValue;
    }

    public double getFreeValue() {
        return freeValue;
    }

    public void setFreeValue(double freeValue) {
        this.freeValue = freeValue;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public double getGrantTotal() {
        return grantTotal;
    }

    public void setGrantTotal(double grantTotal) {
        this.grantTotal = grantTotal;
    }

    public Date getChequeDate() {
        return chequeDate;
    }

    public void setChequeDate(Date chequeDate) {
        this.chequeDate = chequeDate;
    }

    public List<BillFee> getBillFees() {
        return billFees;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
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

    public Bill getBilledBill() {
        return billedBill;
    }

    public void setBilledBill(Bill billedBill) {
        this.billedBill = billedBill;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public BatchBill getBatchBill() {
        return batchBill;
    }

    public void setBatchBill(BatchBill batchBill) {
        this.batchBill = batchBill;
    }

    @Transient
    private Bill tmpRefBill;

    public Bill getTmpRefBill() {
        return tmpRefBill;
    }

    public void setTmpRefBill(Bill refBill) {
        this.tmpRefBill = refBill;
    }

    public double getTmpReturnTotal() {
        return tmpReturnTotal;
    }

    public void setTmpReturnTotal(double tmpReturnTotal) {
        this.tmpReturnTotal = tmpReturnTotal;
    }

    public Bill getForwardReferenceBill() {
        return forwardReferenceBill;
    }

    public void setForwardReferenceBill(Bill forwardReferenceBill) {
        this.forwardReferenceBill = forwardReferenceBill;
    }

    public Bill getBackwardReferenceBill() {
        return backwardReferenceBill;
    }

    public void setBackwardReferenceBill(Bill backwardReferenceBill) {
        this.backwardReferenceBill = backwardReferenceBill;
    }

//    public List<BillItem> getTransActiveBillItem() {
//        if (billItems != null) {
//            transActiveBillItem = new ArrayList<>();
//            for (BillItem b : billItems) {
//                if (!b.isRetired()) {
//                    transActiveBillItem.add(b);
//                }
//            }
//        } else {
//            transActiveBillItem = new ArrayList<>();
//        }
//        return transActiveBillItem;
//    }call me internet is dead slow
//
//    public void setTransActiveBillItem(List<BillItem> transActiveBillItem) {
//        this.transActiveBillItem = transActiveBillItem;
//    }
    public List<Bill> getForwardReferenceBills() {
        if (forwardReferenceBills == null) {
            forwardReferenceBills = new ArrayList<>();
        }
        return forwardReferenceBills;
    }

    public void setForwardReferenceBills(List<Bill> forwardReferenceBills) {
        this.forwardReferenceBills = forwardReferenceBills;
    }

    public List<Bill> getBackwardReferenceBills() {
        if (backwardReferenceBills == null) {
            backwardReferenceBills = new ArrayList<>();
        }
        return backwardReferenceBills;
    }

    public void setBackwardReferenceBills(List<Bill> backwardReferenceBills) {
        this.backwardReferenceBills = backwardReferenceBills;
    }

    public List<BillItem> getTransActiveBillItem() {
        return transActiveBillItem;
    }

    public void setTransActiveBillItem(List<BillItem> transActiveBillItem) {
        this.transActiveBillItem = transActiveBillItem;
    }

    public PatientEncounter getProcedure() {
        return procedure;
    }

    public void setProcedure(PatientEncounter procedure) {
        this.procedure = procedure;
    }

    public SurgeryBillType getSurgeryBillType() {
        return surgeryBillType;
    }

    public void setSurgeryBillType(SurgeryBillType surgeryBillType) {
        this.surgeryBillType = surgeryBillType;
    }

    public List<Bill> getReturnPreBills() {
        List<Bill> bills = new ArrayList<>();

        for (Bill b : returnPreBills) {
            if (b instanceof RefundBill && b.getBillType() == BillType.PharmacyPre) {

                bills.add(b);
            }
        }
        returnPreBills = bills;

        return returnPreBills;
    }

    public List<Bill> getReturnBhtIssueBills() {
        List<Bill> bills = new ArrayList<>();
//        System.err.println("Size " + returnBhtIssueBills.size());
        for (Bill b : returnBhtIssueBills) {
//            System.err.println("1 " + b);
//            System.err.println("2 " + b.getBillClass());
//            System.err.println("3 " + b.getBillType());
            if (b instanceof RefundBill && (b.getBillType() == BillType.PharmacyBhtPre || b.getBillType() == BillType.StoreBhtPre)) {
                bills.add(b);
            }
        }
        returnBhtIssueBills = bills;

        return returnBhtIssueBills;
    }

    public void setReturnPreBills(List<Bill> returnBills) {
        this.returnPreBills = returnBills;
    }

    public List<Bill> getReturnCashBills() {
        List<Bill> bills = new ArrayList<>();
        for (Bill b : returnCashBills) {
            if (b instanceof RefundBill
                    && b.getBillType() == BillType.PharmacySale
                    && b.getBilledBill() == null) {
                bills.add(b);
            }
        }
        returnCashBills = bills;

        return returnCashBills;
    }

    public boolean checkActiveReturnBhtIssueBills() {
        for (Bill b : getReturnBhtIssueBills()) {
            if (!b.isCancelled() && !b.isRetired()) {
                return true;
            }
        }
        return false;
    }

    public boolean checkActiveReturnCashBill() {
        for (Bill b : getReturnCashBills()) {
            if (!b.isCancelled() && !b.isRetired()) {
                return true;
            }
        }
        return false;
    }

    public void setReturnCashBills(List<Bill> returnCashBills) {
        this.returnCashBills = returnCashBills;
    }

    public List<Bill> getCashBillsPre() {
        List<Bill> bills = new ArrayList<>();
        for (Bill b : cashBillsPre) {
            if (b instanceof BilledBill && b.getBillType() == BillType.PharmacySale) {
                bills.add(b);
            }
        }
        cashBillsPre = bills;

        return cashBillsPre;
    }

    public List<Bill> getCashBillsOpdPre() {
        List<Bill> bills = new ArrayList<>();
        for (Bill b : cashBillsOpdPre) {
            if (b instanceof BilledBill && b.getBillType() == BillType.OpdBill) {
                bills.add(b);
            }
        }
        cashBillsOpdPre = bills;
        return cashBillsOpdPre;
    }

    public boolean checkActiveCashPreBill() {
        for (Bill b : getCashBillsPre()) {
            if (!b.isCancelled() && !b.isRetired()) {
                return true;
            }
        }
        return false;
    }

    public void setCashBillsPre(List<Bill> cashBillsPre) {
        this.cashBillsPre = cashBillsPre;
    }

    public void setReturnBhtIssueBills(List<Bill> returnBhtIssueBills) {
        this.returnBhtIssueBills = returnBhtIssueBills;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public WebUser getEditor() {
        return editor;
    }

    public void setEditor(WebUser editor) {
        this.editor = editor;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }

    public List<StockVarientBillItem> getStockVarientBillItems() {
        return stockVarientBillItems;
    }

    public void setStockVarientBillItems(List<StockVarientBillItem> stockVarientBillItems) {
        this.stockVarientBillItems = stockVarientBillItems;
    }

    public boolean isTransBoolean() {
        return transBoolean;
    }

    public boolean getTransBoolean() {
        return transBoolean;
    }

    public void setTransBoolean(boolean transBoolean) {
        this.transBoolean = transBoolean;
    }

    public double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public WebUser getFromWebUser() {
        return fromWebUser;
    }

    public void setFromWebUser(WebUser fromWebUser) {
        this.fromWebUser = fromWebUser;
    }

    public CashTransaction getCashTransaction() {
        return cashTransaction;
    }

    public void setCashTransaction(CashTransaction cashTransaction) {
        this.cashTransaction = cashTransaction;

    }

    public WebUser getToWebUser() {
        return toWebUser;
    }

    public void setToWebUser(WebUser toWebUser) {
        this.toWebUser = toWebUser;
    }

    public double getMargin() {
        return margin;
    }

    public void setMargin(double margin) {
        this.margin = margin;
    }

    public Institution getReferenceInstitution() {
        return referenceInstitution;
    }

    public void setReferenceInstitution(Institution referenceInstitution) {
        this.referenceInstitution = referenceInstitution;
    }

    public BillSession getSingleBillSession() {
        return singleBillSession;
    }

    public void setSingleBillSession(BillSession singleBillSession) {
        this.singleBillSession = singleBillSession;
    }

    public Date getAppointmentAt() {
        return appointmentAt;
    }

    public void setAppointmentAt(Date appointmentAt) {
        this.appointmentAt = appointmentAt;
    }

    public Date getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Date paidAt) {
        this.paidAt = paidAt;
    }

    public String getQutationNumber() {
        return qutationNumber;
    }

    public void setQutationNumber(String qutationNumber) {
        this.qutationNumber = qutationNumber;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public Institution getReferredByInstitution() {
        return referredByInstitution;
    }

    public void setReferredByInstitution(Institution referredByInstitution) {
        this.referredByInstitution = referredByInstitution;
    }

    public String getReferralNumber() {
        return referralNumber;
    }

    public void setReferralNumber(String referralNumber) {
        this.referralNumber = referralNumber;
    }

    public boolean isTransError() {
        return transError;
    }

    public void setTransError(boolean transError) {
        this.transError = transError;
    }

    public List<Sms> getSentSmses() {
        return sentSmses;
    }

    public void setSentSmses(List<Sms> sentSmses) {
        this.sentSmses = sentSmses;
    }

    public Boolean getSmsed() {
        return smsed;
    }

    public void setSmsed(Boolean smsed) {
        this.smsed = smsed;
    }

    public WebUser getSmsedUser() {
        return smsedUser;
    }

    public void setSmsedUser(WebUser smsedUser) {
        this.smsedUser = smsedUser;
    }

    public Date getSmsedAt() {
        return smsedAt;
    }

    public void setSmsedAt(Date smsedAt) {
        this.smsedAt = smsedAt;
    }

}
