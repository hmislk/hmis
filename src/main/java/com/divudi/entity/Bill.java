/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity;

import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.IdentifiableWithNameOrCode;
import com.divudi.data.PaymentMethod;
import com.divudi.data.inward.SurgeryBillType;
import com.divudi.entity.cashTransaction.CashTransaction;
import com.divudi.entity.membership.MembershipScheme;
import com.divudi.entity.pharmacy.StockVarientBillItem;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author buddhika
 */
@Entity
@Inheritance
public class Bill implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    
    static final long serialVersionUID = 1L;
    
    @ManyToOne
    private MembershipScheme membershipScheme;
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
    @OneToMany(mappedBy = "billedBill", fetch = FetchType.LAZY)
    private List<Bill> refundBills = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    protected BillClassType billClassType;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;
    @Transient
    boolean transError;

    
    

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BillFee> billFees = new ArrayList<>();
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("inwardChargeType, searialNo")
    private List<BillItem> billItems;

    @OneToMany(mappedBy = "expenseBill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("searialNo")
    private List<BillItem> billExpenses;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BillComponent> billComponents = new ArrayList<>();
    ////////////////////////////////////////////////   
    @Lob
    private String comments;
    // Bank Detail
    private String creditCardRefNo;
    private String chequeRefNo;

    private int creditDuration;
    @ManyToOne(fetch = FetchType.LAZY)
    private Institution bank;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date chequeDate;
    //Approve
    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser approveUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date approveAt;
    //Pharmacy
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date invoiceDate;
    //Enum
    @Enumerated(EnumType.STRING)
    private BillType billType;
    @Enumerated(EnumType.STRING)
    private BillTypeAtomic billTypeAtomic;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    @ManyToOne(fetch = FetchType.LAZY)
    private BillItem singleBillItem;
    @ManyToOne(fetch = FetchType.LAZY)
    private BillSession singleBillSession;
    String qutationNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    private Institution referredByInstitution;
    @Column
    private String referenceNumber; //referenceNumber

    //Values
    private double margin;

    private double total;
    private double netTotal;
    private double discount;
    private double vat;
    private double vatPlusNetTotal;

    @Transient
    private double absoluteNetTotal;

    private double discountPercent;

    private double billTotal;
    private double paidAmount;
    private double refundAmount;
    private double balance;
    private double serviceCharge;
    private Double tax = 0.0;
    private Double cashPaid = 0.0;
    private Double cashBalance = 0.0;
    private double saleValue = 0.0f;
    private double freeValue = 0.0f;
    private double performInstitutionFee;
    private double staffFee;
    private double billerFee;
    private double grantTotal;
    private double expenseTotal;
    //with minus tax and discount
    private double grnNetTotal;

    //Institution
    @ManyToOne(fetch = FetchType.LAZY)
    private Institution paymentSchemeInstitution;
    @ManyToOne(fetch = FetchType.LAZY)
    @Deprecated //Use fromInstitution
    private Institution collectingCentre;
    @ManyToOne(fetch = FetchType.LAZY)
    private Institution institution;
    @ManyToOne(fetch = FetchType.LAZY)
    private Institution fromInstitution;
    @ManyToOne(fetch = FetchType.LAZY)
    private Institution toInstitution;
    @ManyToOne(fetch = FetchType.LAZY)
    private Institution creditCompany;
    @ManyToOne(fetch = FetchType.LAZY)
    private Institution referenceInstitution;
    //Departments
    @ManyToOne(fetch = FetchType.LAZY)
    private Department referringDepartment;
    @ManyToOne(fetch = FetchType.LAZY)
    private Department department;
    @ManyToOne(fetch = FetchType.LAZY)
    private Department fromDepartment;
    @ManyToOne(fetch = FetchType.LAZY)
    private Department toDepartment;
    //Bill
    @ManyToOne(fetch = FetchType.LAZY)
    private Bill billedBill;
    @ManyToOne(fetch = FetchType.LAZY)
    private Bill cancelledBill;
    @ManyToOne(fetch = FetchType.LAZY)
    private Bill refundedBill;
    @ManyToOne(fetch = FetchType.LAZY)
    private Bill reactivatedBill;

    @ManyToOne(fetch = FetchType.LAZY)
    private Bill referenceBill;
    //Id's
    private String deptId;
    private String insId;
    private String catId;
    private String sessionId;
    @Deprecated
    private String bookingId;
    private String invoiceNumber;
    @Transient
    private int intInvoiceNumber;
    //Staff
    @ManyToOne(fetch = FetchType.LAZY)
    private Staff staff;
    @ManyToOne(fetch = FetchType.LAZY)
    private Staff fromStaff;
    @ManyToOne(fetch = FetchType.LAZY)
    private Staff toStaff;
    //Booleans
    private boolean paid;
    private boolean cancelled;
    private boolean refunded;
    private boolean reactivated;
    //Created Properties
    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Edited Properties
    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser editor;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date editedAt;
    //Checking Property
    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser checkedBy;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date checkeAt;
    //Retairing properties
    boolean retired;
    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;
    ////////////////
    @ManyToOne(fetch = FetchType.LAZY)
    private PaymentScheme paymentScheme;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date billDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date billTime;
    @Transient
    private String billClass;
    @ManyToOne(fetch = FetchType.LAZY)
    private Item billPackege;//BILLPACKEGE\\_ID
    @ManyToOne(fetch = FetchType.LAZY)
    private Person person;
    @ManyToOne(fetch = FetchType.LAZY)
    private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY)
    private Doctor referredBy;
    @ManyToOne(fetch = FetchType.LAZY)
    private PatientEncounter patientEncounter;
    @ManyToOne(fetch = FetchType.LAZY)
    private PatientEncounter procedure;
    @Transient
    private List<Bill> listOfBill;

    @Transient
    private List<BillItem> transActiveBillItem;

    @ManyToOne(fetch = FetchType.LAZY)
    private Bill forwardReferenceBill;

    @ManyToOne(fetch = FetchType.LAZY)
    private Bill backwardReferenceBill;
    private double hospitalFee;
    private double professionalFee;
    @Transient
    private double tmpReturnTotal;
    @Transient
    private boolean transBoolean;
    @Enumerated(EnumType.STRING)
    private SurgeryBillType surgeryBillType;
    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser toWebUser;
    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser fromWebUser;
    double claimableTotal;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date appointmentAt;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date paidAt;
    @ManyToOne(fetch = FetchType.LAZY)
    private Bill paidBill;
    double qty;

    //Sms Info
    private Boolean smsed = false;
    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser smsedUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date smsedAt;
    @OneToMany(mappedBy = "bill")
    private List<Sms> sentSmses;

    //Print Information
    private boolean printed;
    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser printedUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date printedAt;

        //Print Information
    private boolean duplicatedPrinted;
    @ManyToOne(fetch = FetchType.LAZY)
    private WebUser duplicatePrintedUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date duplicatePrintedAt;

    @Transient
    private double transTotalSaleValue;
    @Transient
    private double transTotalCCFee;
    @Transient
    private double transTotalWithOutCCFee;
    @Transient
    private double transCurrentCCBalance;
    @Transient
    private AgentHistory agentHistory;
    @Transient
    private double vatCalulatedAmount;
    @Transient
    private double vatPlusStaffFee;
    @Transient
    private double vatPlusHosFee;
    @Transient
    private boolean approvedAnyTest = false;
    @Transient
    private IdentifiableWithNameOrCode referredInstituteOrDoctor;
    @Transient
    private String billPrint;
    @Transient
    private String billTemplate;
    @Transient
    private String ageAtBilledDate;
    @Transient
    private Bill tmpRefBill;
    
    

    private void generateBillPrintFromBillTemplate() {
        billPrint = "";
        if (billTemplate == null) {
            return;
        }
        if (billTemplate.trim().equals("")) {
            return;
        }
        billPrint = billTemplate;

        if (this.getPatient() != null) {
            Patient tmpPt = this.getPatient();
            if (tmpPt.getPerson() != null) {
                Person tmpPerson = tmpPt.getPerson();
                billPrint = billPrint.replaceAll("\\{patient\\_name\\}", tmpPerson.getNameWithTitle() != null ? tmpPerson.getNameWithTitle() : "");
                billPrint = billPrint.replaceAll("\\{patient\\_age\\}", tmpPerson.getAgeAsString() != null ? tmpPerson.getAgeAsString() : "");
                billPrint = billPrint.replaceAll("\\{patient\\_sex\\}", tmpPerson.getSex() != null ? tmpPerson.getSex().name() : "");
                billPrint = billPrint.replaceAll("\\{patient\\_address\\}", tmpPerson.getAddress() != null ? tmpPerson.getAddress() : "");
                billPrint = billPrint.replaceAll("\\{patient\\_phone\\}", tmpPerson.getPhone() != null ? tmpPerson.getPhone() : "");

                billPrint = billPrint.replaceAll("\\{name\\}", tmpPerson.getNameWithTitle() != null ? tmpPerson.getNameWithTitle() : "");
                billPrint = billPrint.replaceAll("\\{age\\}", tmpPerson.getAgeAsString() != null ? tmpPerson.getAgeAsString() : "");
                billPrint = billPrint.replaceAll("\\{gender\\}", tmpPerson.getSex() != null ? tmpPerson.getSex().name() : "");
                billPrint = billPrint.replaceAll("\\{phone\\_number\\}", tmpPerson.getPhone() != null ? tmpPerson.getPhone() : "");
                billPrint = billPrint.replaceAll("\\{address\\}", tmpPerson.getAddress() != null ? tmpPerson.getAddress() : "");
            }
            billPrint = billPrint.replaceAll("\\{patient\\_phn\\_number\\}", tmpPt.getPhn() != null ? tmpPt.getPhn() : "");
            billPrint = billPrint.replaceAll("\\{patient\\_id\\}", String.valueOf(tmpPt.getId()));
            billPrint = billPrint.replaceAll("\\{patient\\_code\\}", tmpPt.getCode() != null ? tmpPt.getCode() : "");
            billPrint = billPrint.replaceAll("\\{patient\\_mrn\\}", tmpPt.getPhn() != null ? tmpPt.getPhn() : "");
        }

        if (this.getPatientEncounter() != null) {
            PatientEncounter encounter = this.getPatientEncounter();
            billPrint = billPrint.replaceAll("\\{admission\\_number\\}", encounter.getBhtNo() != null ? encounter.getBhtNo() : "");
            billPrint = billPrint.replaceAll("\\{admission\\_date\\}", encounter.getDateOfAdmission() != null ? encounter.getDateOfAdmission().toString() : "");
        }

        if (this.getPaymentMethod() != null) {
            billPrint = billPrint.replaceAll("\\{payment\\_method\\}", this.getPaymentMethod().getLabel() != null ? this.getPaymentMethod().getLabel() : "");
        }

        billPrint = billPrint.replaceAll("\\{id\\}", this.getIdStr());

        String tmpBarcode = "<img id=\"barcode" + this.getId() + "\"/>";
        billPrint = billPrint.replaceAll("\\{barcode\\}", tmpBarcode);

        billPrint = billPrint.replaceAll("\\{ins\\_id\\}", this.getInsId());
        billPrint = billPrint.replaceAll("\\{dept\\_id\\}", this.getDeptId());

        String doubleFormat = "#,##0.00";

        String shortDateFormat = "d M yy";
        String shortTimeFormat = "d M yy hh:mm a";
        String longDateFormat = "dd MMMM yyyy";
        String longTimeFormat = "hh:mm:ss a";
        String shortDateTimeFormat = "d M yy hh:mm a";

        DecimalFormat df = new DecimalFormat(doubleFormat);

        if (this.getBillItems() != null) {
            String itemCount = Integer.toString(this.getBillItems().size());

            billPrint = billPrint.replaceAll("\\{gross\\_total\\}", df.format(this.getTotal()));
            billPrint = billPrint.replaceAll("\\{discount\\}", df.format(this.getDiscount()));
            billPrint = billPrint.replaceAll("\\{net\\_total\\}", df.format(this.getNetTotal()));

            billPrint = billPrint.replaceAll("\\{discount\\_percent\\}", df.format(this.getDiscountPercent()));
            billPrint = billPrint.replaceAll("\\{cash\\_tendered\\}", df.format(this.getCashPaid()));
            billPrint = billPrint.replaceAll("\\{cash\\_balance\\}", df.format(this.getCashBalance()));
            billPrint = billPrint.replaceAll("\\{outstanding\\_balance\\}", df.format(this.getBalance()));
            billPrint = billPrint.replaceAll("\\{number\\_of\\_item\\_types\\}", itemCount);
            billPrint = billPrint.replaceAll("\\{count\\_of\\_items\\}", itemCount);

        }

        billPrint = billPrint.replaceAll("\\{institution\\_id\\}", Objects.toString(this.getInsId(), ""));
        billPrint = billPrint.replaceAll("\\{department\\_id\\}", Objects.toString(this.getDeptId(), ""));
        billPrint = billPrint.replaceAll("\\{id\\}", Objects.toString(this.getIdStr(), ""));
        billPrint = billPrint.replaceAll("\\{id\\_barcode\\}", Objects.toString(this.getIdStr(), ""));
        billPrint = billPrint.replaceAll("\\{Bill Payment Completed Details\\}", Objects.toString(this.getIdStr(), ""));

        if (this.getPaidAt() != null) {
            Date tmpPaidAt = this.getPaidAt();

            DateFormat shortDateFormatter = new SimpleDateFormat(shortDateFormat);
            DateFormat shortTimeFormatter = new SimpleDateFormat(shortTimeFormat);
            DateFormat longDateFormatter = new SimpleDateFormat(longDateFormat);
            DateFormat longTimeFormatter = new SimpleDateFormat(longTimeFormat);
            DateFormat shortDateTimeFormatter = new SimpleDateFormat(shortDateTimeFormat);

            billPrint = billPrint.replaceAll("\\{paid\\_date\\}", longDateFormatter.format(tmpPaidAt));
            billPrint = billPrint.replaceAll("\\{paid\\_time\\}", longDateFormatter.format(tmpPaidAt));
            billPrint = billPrint.replaceAll("\\{paid\\_date\\_time\\}", longDateFormatter.format(tmpPaidAt));

            billPrint = billPrint.replaceAll("\\{paid\\_date\\_short\\}", shortDateFormatter.format(tmpPaidAt));
            billPrint = billPrint.replaceAll("\\{paid\\_time\\_short\\}", shortTimeFormatter.format(tmpPaidAt));
            billPrint = billPrint.replaceAll("\\{paid\\_date\\_time\\_short\\}", shortDateTimeFormatter.format(tmpPaidAt));

            billPrint = billPrint.replaceAll("\\{paid\\_date\\_long\\}", longDateFormatter.format(tmpPaidAt));
            billPrint = billPrint.replaceAll("\\{paid\\_time\\_long\\}", longTimeFormatter.format(tmpPaidAt));
            billPrint = billPrint.replaceAll("\\{paid\\_date\\_time\\_long\\}", longDateFormatter.format(tmpPaidAt) + " " + longTimeFormatter.format(tmpPaidAt));
        }

        if (this.getCreatedAt() != null) {
            Date createdDateTimeTmp = this.getCreatedAt();

            DateFormat shortDateFormatter = new SimpleDateFormat(shortDateFormat);
            DateFormat shortTimeFormatter = new SimpleDateFormat(shortTimeFormat);
            DateFormat longDateFormatter = new SimpleDateFormat(longDateFormat);
            DateFormat longTimeFormatter = new SimpleDateFormat(longTimeFormat);
            DateFormat shortDateTimeFormatter = new SimpleDateFormat(shortDateTimeFormat);

            billPrint = billPrint.replaceAll("\\{bill\\_date\\}", longDateFormatter.format(createdDateTimeTmp));
            billPrint = billPrint.replaceAll("\\{bill\\_time\\}", longDateFormatter.format(createdDateTimeTmp));
            billPrint = billPrint.replaceAll("\\{bill\\_date\\_time\\}", longDateFormatter.format(createdDateTimeTmp));

            billPrint = billPrint.replaceAll("\\{bill\\_date\\_short\\}", shortDateFormatter.format(createdDateTimeTmp));
            billPrint = billPrint.replaceAll("\\{bill\\_time\\_short\\}", shortTimeFormatter.format(createdDateTimeTmp));
            billPrint = billPrint.replaceAll("\\{bill\\_date\\_time\\_short\\}", shortDateTimeFormatter.format(createdDateTimeTmp));

            billPrint = billPrint.replaceAll("\\{bill\\_date\\_long\\}", longDateFormatter.format(createdDateTimeTmp));
            billPrint = billPrint.replaceAll("\\{bill\\_time\\_long\\}", longTimeFormatter.format(createdDateTimeTmp));
            billPrint = billPrint.replaceAll("\\{bill\\_date\\_time\\_long\\}", longDateFormatter.format(createdDateTimeTmp) + " " + longTimeFormatter.format(createdDateTimeTmp));
        }

        if (this.getInstitution() != null) {
            billPrint = billPrint.replaceAll("\\{institution\\_name\\}", safeReplace(this.getInstitution().getName()));
            billPrint = billPrint.replaceAll("\\{institution\\_address\\}", safeReplace(this.getInstitution().getAddress()));
            billPrint = billPrint.replaceAll("\\{institution\\_phone\\}", safeReplace(this.getInstitution().getPhone()));
            billPrint = billPrint.replaceAll("\\{institution\\_email\\}", safeReplace(this.getInstitution().getEmail()));
            billPrint = billPrint.replaceAll("\\{institution\\_website\\}", safeReplace(this.getInstitution().getWeb()));
        }

        if (this.getDepartment() != null) {
            billPrint = billPrint.replaceAll("\\{department\\_name\\}", safeReplace(this.getDepartment().getName()));
            billPrint = billPrint.replaceAll("\\{department\\_address\\}", safeReplace(this.getDepartment().getAddress()));
            billPrint = billPrint.replaceAll("\\{department\\_phone\\}", safeReplace(this.getDepartment().getTelephone1()));
            billPrint = billPrint.replaceAll("\\{department\\_email\\}", safeReplace(this.getDepartment().getEmail()));
        }

        if (this.getFromInstitution() != null) {
            billPrint = billPrint.replaceAll("\\{from\\_institution\\_name\\}", safeReplace(this.getFromInstitution().getName()));
            billPrint = billPrint.replaceAll("\\{from\\_institution\\_address\\}", safeReplace(this.getFromInstitution().getAddress()));
            billPrint = billPrint.replaceAll("\\{from\\_institution\\_phone\\}", safeReplace(this.getFromInstitution().getPhone()));
            billPrint = billPrint.replaceAll("\\{from\\_institution\\_email\\}", safeReplace(this.getFromInstitution().getEmail()));
            billPrint = billPrint.replaceAll("\\{from\\_institution\\}", safeReplace(this.getFromInstitution().getName()));
        }

        if (this.getFromDepartment() != null) {
            billPrint = billPrint.replaceAll("\\{from\\_department\\_name\\}", safeReplace(this.getFromDepartment().getName()));
            billPrint = billPrint.replaceAll("\\{from\\_department\\_address\\}", safeReplace(this.getFromDepartment().getAddress()));
            billPrint = billPrint.replaceAll("\\{from\\_department\\_phone\\}", safeReplace(this.getFromDepartment().getTelephone1()));
            billPrint = billPrint.replaceAll("\\{from\\_department\\_email\\}", safeReplace(this.getFromDepartment().getEmail()));
            billPrint = billPrint.replaceAll("\\{from\\_department\\}", safeReplace(this.getDepartment().getName()));
        }

        if (this.getToInstitution() != null) {
            billPrint = billPrint.replaceAll("\\{to\\_institution\\_name\\}", safeReplace(this.getToInstitution().getName()));
            billPrint = billPrint.replaceAll("\\{to\\_institution\\_address\\}", safeReplace(this.getToInstitution().getAddress()));
            billPrint = billPrint.replaceAll("\\{to\\_institution\\_phone\\}", safeReplace(this.getToInstitution().getPhone()));
            billPrint = billPrint.replaceAll("\\{to\\_institution\\_email\\}", safeReplace(this.getToInstitution().getEmail()));
            billPrint = billPrint.replaceAll("\\{to\\_institution\\}", safeReplace(this.getToInstitution().getName()));
        }

        if (this.getToDepartment() != null) {
            billPrint = billPrint.replaceAll("\\{to\\_department\\_name\\}", safeReplace(this.getToDepartment().getName()));
            billPrint = billPrint.replaceAll("\\{to\\_department\\_address\\}", safeReplace(this.getToDepartment().getAddress()));
            billPrint = billPrint.replaceAll("\\{to\\_department\\_phone\\}", safeReplace(this.getToDepartment().getTelephone1()));
            billPrint = billPrint.replaceAll("\\{to\\_department\\_email\\}", safeReplace(this.getToDepartment().getEmail()));
            billPrint = billPrint.replaceAll("\\{to\\_department\\}", safeReplace(this.getToDepartment().getName()));
        }

        if (this.getCreater() != null) {
            billPrint = billPrint.replaceAll("\\{bill\\_raised\\_user\\_username\\}", safeReplace(this.getCreater().getName()));

            if (this.getCreater().getWebUserPerson() != null) {
                billPrint = billPrint.replaceAll("\\{bill\\_raised\\_user\\_name\\}", safeReplace(this.getCreater().getWebUserPerson().getName()));
                billPrint = billPrint.replaceAll("\\{Bill Raised Details\\}", safeReplace(this.getCreater().getWebUserPerson().getName()));
            }

            billPrint = billPrint.replaceAll("\\{cashier\\_user\\_name\\}", safeReplace(this.getCreater().getName()));

            if (this.getCreater().getWebUserPerson() != null) {
                billPrint = billPrint.replaceAll("\\{cashier\\_name\\}", safeReplace(this.getCreater().getWebUserPerson().getName()));
            }

            billPrint = billPrint.replaceAll("\\{cashier\\_code\\}", safeReplace(this.getCreater().getCode()));
        }

        billPrint = billPrint.replaceAll("\\{item\\_qty\\_rate\\_value\\_table\\}", convertBillItemsToItemQtyRateValueTable(billItems));
        billPrint = billPrint.replaceAll("\\{item\\_value\\_table\\}", convertBillItemsToItemValueTable(billItems));
        billPrint = billPrint.replaceAll("\\{item\\_rate\\_qty\\_newline\\_value\\_table\\}", safeReplace(this.getIdStr()));

    }

    
    
    private String safeReplace(String value) {
        return value != null ? value : "";
    }

    private String convertBillItemsToItemQtyRateValueTable(List<BillItem> bis) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        StringBuilder str = new StringBuilder();

        // Add the table start, thead and headers
        str.append("<table class='table table-borderless'>")
                .append("<thead>")
                .append("<tr>")
                .append("<th>Item</th>")
                .append("<th>Quantity</th>")
                .append("<th>Rate</th>")
                .append("<th>Value</th>")
                .append("</tr>")
                .append("</thead>")
                .append("<tbody>");

        // Add the table data
        for (BillItem bi : bis) {
            String tmpItem = bi.getItem().getName();
            Double qty = bi.getQty();
            Double rate = bi.getNetRate();
            Double value = bi.getNetValue();

            str.append("<tr>")
                    .append("<td>").append(tmpItem).append("</td>")
                    .append("<td>").append(df.format(qty)).append("</td>")
                    .append("<td>").append(df.format(rate)).append("</td>")
                    .append("<td>").append(df.format(value)).append("</td>")
                    .append("</tr>");
        }

        // Add the table end
        str.append("</tbody>")
                .append("</table>");

        return str.toString();
    }

    private String convertBillItemsToItemValueTable(List<BillItem> bis) {
        DecimalFormat df = new DecimalFormat("#,##0.00");
        StringBuilder str = new StringBuilder();

        // Add the table start, thead and headers
        str.append("<table class='table-sm'>")
                .append("<thead>")
                .append("<tr>")
                .append("<th>Item</th>")
                .append("<th>Value</th>")
                .append("</tr>")
                .append("</thead>")
                .append("<tbody>");

        // Add the table data
        for (BillItem bi : bis) {
            String tmpItem = bi.getItem().getName();
            Double qty = bi.getQty();
            Double rate = bi.getNetRate();
            Double value = bi.getNetValue();

            str.append("<tr>")
                    .append("<td>").append(tmpItem).append("</td>")
                    .append("<td>").append(df.format(value)).append("</td>")
                    .append("</tr>");
        }

        // Add the table end
        str.append("</tbody>")
                .append("</table>");

        return str.toString();
    }

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

    public double getVat() {
        return vat;
    }

    public void setVat(double vat) {
        this.vat = vat;
    }

    public void invertValue(Bill bill) {
        staffFee = 0 - bill.getStaffFee();
        performInstitutionFee = 0 - bill.getPerformInstitutionFee();
        billerFee = 0 - bill.getBillerFee();
        discount = 0 - bill.getDiscount();
        vat = 0 - bill.getVat();
        vatPlusNetTotal = 0 - bill.getVatPlusNetTotal();
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
        billTotal = 0 - bill.getBillTotal();
    }

    public void invertValue() {
        staffFee = 0 - getStaffFee();
        performInstitutionFee = 0 - getPerformInstitutionFee();
        billerFee = 0 - getBillerFee();
        discount = 0 - getDiscount();
        vat = 0 - getVat();
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
        vatPlusNetTotal = 0 - getVatPlusNetTotal();
        billTotal = 0 - getBillTotal();
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
        vat = bill.getVat();
        vatPlusNetTotal = bill.getVatPlusNetTotal();
        sessionId = bill.getSessionId();
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
        this.vat = bill.getVat();
        this.billTotal = bill.getBillTotal();
        this.vatPlusNetTotal = bill.getVatPlusNetTotal();
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
        return total - discount + vat;
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
        ////System.out.println("getting discount percent");
//        ////System.out.println("bill item"+getBillItems());
//        ////System.out.println(getBillItems().get(0).getPriceMatrix());
        if (!getBillItems().isEmpty() && getBillItems().get(0).getPriceMatrix() != null) {
            ////System.out.println("sys inside");
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

//    public void setBalance(double balance) {
//        this.balance = balance;
//    }
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
//        invoiceNumber = intInvoiceNumber + ""; change petty cash number to ruhunu hospital(Mr.Lahiru)
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

    public String getIdStr() {
        String formatted = String.format("%07d", id);
        return formatted;
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

    @Transient
    public List<BillFee> getBillFeesWIthoutZeroValue() {
        if (billFees == null) {
            return null;
        }
        List<BillFee> feesWithoutZeros = new ArrayList<>();
        if (billFees.isEmpty()) {
            return feesWithoutZeros;
        }
        for (BillFee bf : billFees) {
            if (bf.getFeeValue() > 0 || bf.getFeeDiscount() > 0) {
                feesWithoutZeros.add(bf);
            }
        }
        return feesWithoutZeros;
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

    @Deprecated
    public String getBookingId() {
        return bookingId;
    }

    @Deprecated
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

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

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
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

    public double getTransTotalCCFee() {
        return transTotalCCFee;
    }

    public void setTransTotalCCFee(double transTotalCCFee) {
        this.transTotalCCFee = transTotalCCFee;
    }

    public double getTransTotalWithOutCCFee() {
        return transTotalWithOutCCFee;
    }

    public void setTransTotalWithOutCCFee(double transTotalWithOutCCFee) {
        this.transTotalWithOutCCFee = transTotalWithOutCCFee;
    }

    public double getTransCurrentCCBalance() {
        return transCurrentCCBalance;
    }

    public void setTransCurrentCCBalance(double transCurrentCCBalance) {
        this.transCurrentCCBalance = transCurrentCCBalance;
    }

    public double getVatPlusNetTotal() {
        return vatPlusNetTotal;
    }

    public void setVatPlusNetTotal(double vatPlusNetTotal) {
        this.vatPlusNetTotal = vatPlusNetTotal;
    }

    public double getVatCalulatedAmount() {
        return vatCalulatedAmount;
    }

    public void setVatCalulatedAmount(double vatCalulatedAmount) {
        this.vatCalulatedAmount = vatCalulatedAmount;
    }

    public List<Bill> getRefundBills() {
        return refundBills;
    }

    public void setRefundBills(List<Bill> refundBills) {
        this.refundBills = refundBills;
    }

    public AgentHistory getAgentHistory() {
        return agentHistory;
    }

    public void setAgentHistory(AgentHistory agentHistory) {
        this.agentHistory = agentHistory;
    }

    public double getVatPlusStaffFee() {
        return vatPlusStaffFee;
    }

    public void setVatPlusStaffFee(double vatPlusStaffFee) {
        this.vatPlusStaffFee = vatPlusStaffFee;
    }

    public double getVatPlusHosFee() {
        return vatPlusHosFee;
    }

    public void setVatPlusHosFee(double vatPlusHosFee) {
        this.vatPlusHosFee = vatPlusHosFee;
    }

    public boolean isPrinted() {
        return printed;
    }

    public void setPrinted(boolean printed) {
        this.printed = printed;
    }

    public boolean isApprovedAnyTest() {
        return approvedAnyTest;
    }

    public void setApprovedAnyTest(boolean approvedAnyTest) {
        this.approvedAnyTest = approvedAnyTest;
    }

    public IdentifiableWithNameOrCode getReferredInstituteOrDoctor() {
        if (referenceInstitution != null) {
            referredInstituteOrDoctor = referenceInstitution;
        } else {
            referredInstituteOrDoctor = referredBy;
        }
        return referredInstituteOrDoctor;
    }

    public double getAbsoluteNetTotal() {
        absoluteNetTotal = Math.abs(netTotal);
        return absoluteNetTotal;
    }

    public String getBillPrint() {
        generateBillPrintFromBillTemplate();
        return billPrint;
    }

    public String getBillTemplate() {
        return billTemplate;
    }

    public void setBillTemplate(String billTemplate) {
        this.billTemplate = billTemplate;
    }

    public String getAgeAtBilledDate() {
        if (patient == null || patient.getPerson() == null) {
            ageAtBilledDate = "";
            return ageAtBilledDate;
        }

        Date ptDob = patient.getPerson().getDob();
        if (this.billDate == null) {
            billDate = new Date();
        }

        Calendar calDob = Calendar.getInstance();
        calDob.setTime(ptDob);
        Calendar calBill = Calendar.getInstance();
        calBill.setTime(billDate);

        int yearDiff = calBill.get(Calendar.YEAR) - calDob.get(Calendar.YEAR);
        int monthDiff = calBill.get(Calendar.MONTH) - calDob.get(Calendar.MONTH);
        int dayDiff = calBill.get(Calendar.DAY_OF_MONTH) - calDob.get(Calendar.DAY_OF_MONTH);

        if (dayDiff < 0) {
            monthDiff--;
            dayDiff += calBill.getActualMaximum(Calendar.DAY_OF_MONTH);
        }

        if (monthDiff < 0) {
            yearDiff--;
            monthDiff += 12;
        }

        if (yearDiff > 5) {
            ageAtBilledDate = yearDiff + " years";
        } else if (yearDiff >= 1) {
            ageAtBilledDate = yearDiff + " years and " + monthDiff + " months";
        } else {
            ageAtBilledDate = monthDiff * 30 + dayDiff + " days";
        }

        return ageAtBilledDate;
    }

    public int getCreditDuration() {
        return creditDuration;
    }

    public void setCreditDuration(int creditDuration) {
        this.creditDuration = creditDuration;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public WebUser getPrintedUser() {
        return printedUser;
    }

    public void setPrintedUser(WebUser printedUser) {
        this.printedUser = printedUser;
    }

    public Date getPrintedAt() {
        return printedAt;
    }

    public void setPrintedAt(Date printedAt) {
        this.printedAt = printedAt;
    }

    public boolean isDuplicatedPrinted() {
        return duplicatedPrinted;
    }

    public void setDuplicatedPrinted(boolean duplicatedPrinted) {
        this.duplicatedPrinted = duplicatedPrinted;
    }

    public WebUser getDuplicatePrintedUser() {
        return duplicatePrintedUser;
    }

    public void setDuplicatePrintedUser(WebUser duplicatePrintedUser) {
        this.duplicatePrintedUser = duplicatePrintedUser;
    }

    public Date getDuplicatePrintedAt() {
        return duplicatePrintedAt;
    }

    public void setDuplicatePrintedAt(Date duplicatePrintedAt) {
        this.duplicatePrintedAt = duplicatePrintedAt;
    }

}
