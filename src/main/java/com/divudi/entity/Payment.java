/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity;

import com.divudi.data.Denomination;
import com.divudi.data.PaymentHandover;
import com.divudi.data.PaymentMethod;
import com.divudi.entity.cashTransaction.CashBook;
import com.divudi.entity.cashTransaction.CashBookEntry;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import org.json.JSONArray;
import org.json.JSONObject;

@Entity
public class Payment implements Serializable {

    static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ManyToOne
    Bill bill;

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date paymentDate;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date writtenAt;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date toRealizeAt;

    @Enumerated(EnumType.STRING)
    PaymentMethod paymentMethod;

    boolean realized;
    @ManyToOne
    private Payment referancePayment;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date realizedAt;

    @ManyToOne
    WebUser realizer;

    @Lob
    String realizeComments;

    @ManyToOne
    Institution bank;

    @Lob
    String comments;

    @ManyToOne
    WebUser creater;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;

    boolean retired;

    @ManyToOne
    WebUser retirer;

    @ManyToOne
    private WebUser currentHolder;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;

    String retireComments;

    private String chequeRefNo;

    @Temporal(javax.persistence.TemporalType.DATE)
    private Date chequeDate;

    private String creditCardRefNo;

    double paidValue;

    private int creditDurationInDays;

    @Lob
    @Deprecated
    private String currencyDenominationsJson;

    @ManyToOne
    Institution institution;

    @ManyToOne
    Department department;

    @Transient
    @Deprecated
    private List<Denomination> currencyDenominations;

    @Transient
    @Deprecated
    private List<String> humanReadableDenominations;

    private String referenceNo;

    private boolean cashbookEntryStated;
    private boolean cashbookEntryCompleted;
    private boolean paymentRecordStated;
    private boolean paymentRecordCompleted;

    private boolean selectedForHandover;
    private boolean selectedForCashbookEntry;
    private boolean selectedForRecording;
    private boolean selectedForRecordingConfirmation;
    private boolean handingOverStarted;
    private boolean handingOverCompleted;

    @ManyToOne
    private Bill handoverShiftBill;
    @ManyToOne
    private Bill handoverShiftComponantBill;

    //Handover Creation
    @ManyToOne
    private Bill handoverCreatedBill;
    @ManyToOne
    private Bill handoverCreatedComponantBill;

    //Handover Accept
    @ManyToOne
    private Bill handoverAcceptBill;
    @ManyToOne
    private Bill handoverAcceptComponantBill;

    //Payment Record Creation
    @ManyToOne
    private Bill paymentRecordCreateBill;
    @ManyToOne
    private Bill paymentRecordCreateComponantBill;
    //Payment Record Accept
    @ManyToOne
    private Bill paymentRecordCompleteBill;
    @ManyToOne
    private Bill paymentRecordCompleteComponantBill;
    //Cash Book
    @ManyToOne
    private CashBookEntry cashbookEntry;
    @ManyToOne
    private CashBook cashbook;

    private boolean cancelled;
    @ManyToOne
    private WebUser cancelledBy;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date cancelledAt;
    @ManyToOne
    private Payment cancelledPayment;
    @ManyToOne
    private Bill cancelledBill;

    private PaymentHandover transientPaymentHandover;

    // New attributes for marking a cheque as paid
    @ManyToOne
    private WebUser chequePayer;  // User who marked the cheque as paid
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date chequePaidAt;  // Timestamp when the cheque was marked as paid
    private boolean chequePaid;  // Indicates if the cheque has been marked as paid
    @ManyToOne
    private Bill chequePaidBill;  // Reference to the Bill where the cheque was marked as paid

    // New attributes for marking a cheque as realized
    @ManyToOne
    private WebUser chequeRealizer;  // User who marked the cheque as realized
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date chequeRealizedAt;  // Timestamp when the cheque was marked as realized
    private boolean chequeRealized;  // Indicates if the cheque has been marked as realized
    @ManyToOne
    private Bill chequeRealizedBill;  // Reference to the Bill where the cheque was marked as realized

    @ManyToOne
    private Institution fromInstitution;
    @ManyToOne
    private Institution toInstitution;

    public Payment() {
        cashbookEntryStated = false;
        cashbookEntryCompleted = false;
        paymentRecordStated = false;
        paymentRecordCompleted = false;
        chequeRealized = false;
        chequePaid = false;
        paymentDate = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Deprecated
    public List<String> getHumanReadableDenominations() {
        List<String> humanReadableList = new ArrayList<>();
        deserializeDenominations();
        if (this.currencyDenominations != null) {
            for (Denomination denomination : this.currencyDenominations) {
                if (denomination.getCount() > 0) {
                    String valueFormatted = String.format("%.2f", denomination.getValue());
                    String countFormatted = String.format("%d", denomination.getCount());
                    String totalFormatted = String.format("%.2f", denomination.getValue() * denomination.getCount());
                    String humanReadable = "(" + valueFormatted + "x" + countFormatted + "=" + totalFormatted + ")";
                    humanReadableList.add(humanReadable);
                }
            }
        }
        return humanReadableList;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Date getWrittenAt() {
        return writtenAt;
    }

    public void setWrittenAt(Date writtenAt) {
        this.writtenAt = writtenAt;
    }

    public Date getToRealizeAt() {
        return toRealizeAt;
    }

    public void setToRealizeAt(Date toRealizeAt) {
        this.toRealizeAt = toRealizeAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isRealized() {
        return realized;
    }

    public void setRealized(boolean realized) {
        this.realized = realized;
    }

    public Date getRealizedAt() {
        return realizedAt;
    }

    public void setRealizedAt(Date realizedAt) {
        this.realizedAt = realizedAt;
    }

    public WebUser getRealizer() {
        return realizer;
    }

    public void setRealizer(WebUser realizer) {
        this.realizer = realizer;
    }

    public String getRealizeComments() {
        return realizeComments;
    }

    public void setRealizeComments(String realizeComments) {
        this.realizeComments = realizeComments;
    }

    public Institution getBank() {
        return bank;
    }

    public void setBank(Institution bank) {
        this.bank = bank;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (!(object instanceof Payment)) {
            return false;
        }
        Payment other = (Payment) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.Payment[ id=" + id + " ]";
    }

    // Can be use as a reference number for any payment method
    public String getChequeRefNo() {
        return chequeRefNo;
    }

    public void setChequeRefNo(String chequeRefNo) {
        this.chequeRefNo = chequeRefNo;
    }

    public Date getChequeDate() {
        return chequeDate;
    }

    public void setChequeDate(Date chequeDate) {
        this.chequeDate = chequeDate;
    }

    public String getCreditCardRefNo() {
        return creditCardRefNo;
    }

    public void setCreditCardRefNo(String creditCardRefNo) {
        this.creditCardRefNo = creditCardRefNo;
    }

    public double getPaidValue() {
        return paidValue;
    }

    public void setPaidValue(double paidValue) {
        this.paidValue = paidValue;
    }

    public int getCreditDurationInDays() {
        return creditDurationInDays;
    }

    public void setCreditDurationInDays(int creditDurationInDays) {
        this.creditDurationInDays = creditDurationInDays;
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

    public Payment copyAttributes() {
        Payment newPayment = new Payment();
        newPayment.setBill(this.bill);
        newPayment.setWrittenAt(this.writtenAt);
        newPayment.setToRealizeAt(this.toRealizeAt);
        newPayment.setPaymentMethod(this.paymentMethod);
        newPayment.setRealized(this.realized);
        newPayment.setRealizedAt(this.realizedAt);
        newPayment.setRealizer(this.realizer);
        newPayment.setRealizeComments(this.realizeComments);
        newPayment.setBank(this.bank);
        newPayment.setComments(this.comments);
        newPayment.setCurrencyDenominationsJson(this.currencyDenominationsJson);
        newPayment.setCreater(this.creater);
        newPayment.setCreatedAt(this.createdAt);
        newPayment.setRetired(this.retired);
        newPayment.setRetirer(this.retirer);
        newPayment.setRetiredAt(this.retiredAt);
        newPayment.setRetireComments(this.retireComments);
        newPayment.setChequeRefNo(this.chequeRefNo);
        newPayment.setChequeDate(this.chequeDate);
        newPayment.setCreditCardRefNo(this.creditCardRefNo);
        newPayment.setPaidValue(this.paidValue);
        newPayment.setInstitution(this.institution);
        newPayment.setDepartment(this.department);
        // Note: ID is not copied to ensure the uniqueness of each entity
        // newPayment.setId(this.id); // This line is intentionally commented out
        return newPayment;
    }

    public Payment clonePaymentForNewBill() {
        Payment newPayment = new Payment();
        newPayment.setWrittenAt(this.writtenAt);
        newPayment.setToRealizeAt(this.toRealizeAt);
        newPayment.setPaymentMethod(this.paymentMethod);
        newPayment.setRealized(this.realized);
        newPayment.setRealizedAt(this.realizedAt);
        newPayment.setRealizer(this.realizer);
        newPayment.setRealizeComments(this.realizeComments);
        newPayment.setBank(this.bank);
        newPayment.setComments(this.comments);
        newPayment.setChequeRefNo(this.chequeRefNo);
        newPayment.setChequeDate(this.chequeDate);
        newPayment.setCreditCardRefNo(this.creditCardRefNo);
        newPayment.setPaidValue(this.paidValue);
        newPayment.setInstitution(this.institution);
        newPayment.setDepartment(this.department);
        newPayment.setReferancePayment(this);
        // Note: ID is not copied to ensure the uniqueness of each entity
        // newPayment.setId(this.id); // This line is intentionally commented out
        return newPayment;
    }

    @Deprecated
    public void setCurrencyDenominationsJson(String currencyDenominationsJson) {
        this.currencyDenominationsJson = currencyDenominationsJson;
    }

    public void serializeDenominations() {
        if (this.currencyDenominations != null) {
            JSONArray jsonArray = new JSONArray();
            for (Denomination denomination : this.currencyDenominations) {
                if (denomination != null) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("value", denomination.getValue());
                    jsonObject.put("count", denomination.getCount());
                    jsonArray.put(jsonObject);
                }
            }
            this.currencyDenominationsJson = jsonArray.toString();
        } else {
            this.currencyDenominationsJson = "[]"; // Empty JSON array if currencyDenominations is null
        }
    }

    @Deprecated
    public void deserializeDenominations() {
        if (this.currencyDenominationsJson != null && !this.currencyDenominationsJson.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(this.currencyDenominationsJson);
                this.currencyDenominations = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (jsonObject.has("value") && jsonObject.has("count")) {
                        int value = jsonObject.getInt("value");
                        int count = jsonObject.getInt("count");
                        this.currencyDenominations.add(new Denomination(value, count));
                    }
                }
            } catch (Exception e) {
                this.currencyDenominations = new ArrayList<>(); // Initialize to an empty list on error
            }
        } else {
            this.currencyDenominations = new ArrayList<>(); // Initialize to an empty list if JSON is null or empty
        }
    }

    @Deprecated
    public String getCurrencyDenominationsJson() {
        return currencyDenominationsJson;
    }

    @Deprecated
    public List<Denomination> getCurrencyDenominations() {
        return currencyDenominations;
    }

    @Deprecated
    public void setCurrencyDenominations(List<Denomination> currencyDenominations) {
        this.currencyDenominations = currencyDenominations;
    }

    public String getReferenceNo() {

        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public boolean getCashbookEntryCompleted() {
        return cashbookEntryCompleted;
    }

    public void setCashbookEntryCompleted(boolean cashbookEntryCompleted) {
        this.cashbookEntryCompleted = cashbookEntryCompleted;
    }

    public Bill getHandoverCreatedBill() {
        return handoverCreatedBill;
    }

    public void setHandoverCreatedBill(Bill handoverCreatedBill) {
        this.handoverCreatedBill = handoverCreatedBill;
    }

    public Bill getHandoverAcceptBill() {
        return handoverAcceptBill;
    }

    public void setHandoverAcceptBill(Bill handoverAcceptBill) {
        this.handoverAcceptBill = handoverAcceptBill;
    }

    public CashBookEntry getCashbookEntry() {
        return cashbookEntry;
    }

    public void setCashbookEntry(CashBookEntry cashbookEntry) {
        this.cashbookEntry = cashbookEntry;
    }

    public CashBook getCashbook() {
        return cashbook;
    }

    public void setCashbook(CashBook cashbook) {
        this.cashbook = cashbook;
    }

    public boolean getCashbookEntryStated() {
        return cashbookEntryStated;
    }

    public void setCashbookEntryStated(boolean cashbookEntryStated) {
        this.cashbookEntryStated = cashbookEntryStated;
    }

    public Bill getPaymentRecordCreateBill() {
        return paymentRecordCreateBill;
    }

    public void setPaymentRecordCreateBill(Bill paymentRecordCreateBill) {
        this.paymentRecordCreateBill = paymentRecordCreateBill;
    }

    public Bill getPaymentRecordCompleteBill() {
        return paymentRecordCompleteBill;
    }

    public void setPaymentRecordCompleteBill(Bill paymentRecordCompleteBill) {
        this.paymentRecordCompleteBill = paymentRecordCompleteBill;
    }

    public boolean isPaymentRecordStated() {
        return paymentRecordStated;
    }

    public void setPaymentRecordStated(boolean paymentRecordStated) {
        this.paymentRecordStated = paymentRecordStated;
    }

    public boolean isPaymentRecordCompleted() {
        return paymentRecordCompleted;
    }

    public void setPaymentRecordCompleted(boolean paymentRecordCompleted) {
        this.paymentRecordCompleted = paymentRecordCompleted;
    }

    public Bill getHandoverShiftBill() {
        return handoverShiftBill;
    }

    public void setHandoverShiftBill(Bill handoverShiftBill) {
        this.handoverShiftBill = handoverShiftBill;
    }

    public Bill getHandoverShiftComponantBill() {
        return handoverShiftComponantBill;
    }

    public void setHandoverShiftComponantBill(Bill handoverShiftComponantBill) {
        this.handoverShiftComponantBill = handoverShiftComponantBill;
    }

    public Payment getReferancePayment() {
        return referancePayment;
    }

    public void setReferancePayment(Payment referancePayment) {
        this.referancePayment = referancePayment;
    }

    public Date getPaymentDate() {
        if (paymentDate == null) {
            if (this.getBill() != null) {
                paymentDate = this.getBill().getCreatedAt();
            }
        }
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Bill getHandoverCreatedComponantBill() {
        return handoverCreatedComponantBill;
    }

    public void setHandoverCreatedComponantBill(Bill handoverCreatedComponantBill) {
        this.handoverCreatedComponantBill = handoverCreatedComponantBill;
    }

    public Bill getHandoverAcceptComponantBill() {
        return handoverAcceptComponantBill;
    }

    public void setHandoverAcceptComponantBill(Bill handoverAcceptComponantBill) {
        this.handoverAcceptComponantBill = handoverAcceptComponantBill;
    }

    public Bill getPaymentRecordCreateComponantBill() {
        return paymentRecordCreateComponantBill;
    }

    public void setPaymentRecordCreateComponantBill(Bill paymentRecordCreateComponantBill) {
        this.paymentRecordCreateComponantBill = paymentRecordCreateComponantBill;
    }

    public Bill getPaymentRecordCompleteComponantBill() {
        return paymentRecordCompleteComponantBill;
    }

    public void setPaymentRecordCompleteComponantBill(Bill paymentRecordCompleteComponantBill) {
        this.paymentRecordCompleteComponantBill = paymentRecordCompleteComponantBill;
    }

    public boolean isSelectedForHandover() {
        return selectedForHandover;
    }

    public void setSelectedForHandover(boolean selectedForHandover) {
        this.selectedForHandover = selectedForHandover;
    }

    public boolean isSelectedForRecording() {
        return selectedForRecording;
    }

    public void setSelectedForRecording(boolean selectedForRecording) {
        this.selectedForRecording = selectedForRecording;
    }

    public boolean isSelectedForCashbookEntry() {
        return selectedForCashbookEntry;
    }

    public void setSelectedForCashbookEntry(boolean selectedForCashbookEntry) {
        this.selectedForCashbookEntry = selectedForCashbookEntry;
    }

    public boolean isSelectedForRecordingConfirmation() {
        return selectedForRecordingConfirmation;
    }

    public void setSelectedForRecordingConfirmation(boolean selectedForRecordingConfirmation) {
        this.selectedForRecordingConfirmation = selectedForRecordingConfirmation;
    }

    public WebUser getCurrentHolder() {
//        if (currentHolder == null) {
//            currentHolder = creater;
//        }
        return currentHolder;
    }

    public void setCurrentHolder(WebUser currentHolder) {
        this.currentHolder = currentHolder;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public WebUser getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(WebUser cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Payment getCancelledPayment() {
        return cancelledPayment;
    }

    public void setCancelledPayment(Payment cancelledPayment) {
        this.cancelledPayment = cancelledPayment;
    }

    public Bill getCancelledBill() {
        return cancelledBill;
    }

    public void setCancelledBill(Bill cancelledBill) {
        this.cancelledBill = cancelledBill;
    }

    public boolean isHandingOverStarted() {
        return handingOverStarted;
    }

    public void setHandingOverStarted(boolean handingOverStarted) {
        this.handingOverStarted = handingOverStarted;
    }

    public boolean isHandingOverCompleted() {
        return handingOverCompleted;
    }

    public void setHandingOverCompleted(boolean handingOverCompleted) {
        this.handingOverCompleted = handingOverCompleted;
    }

    public PaymentHandover getTransientPaymentHandover() {
        return transientPaymentHandover;
    }

    public void setTransientPaymentHandover(PaymentHandover transientPaymentHandover) {
        this.transientPaymentHandover = transientPaymentHandover;
    }

    public WebUser getChequePayer() {
        return chequePayer;
    }

    public void setChequePayer(WebUser chequePayer) {
        this.chequePayer = chequePayer;
    }

    public Date getChequePaidAt() {
        return chequePaidAt;
    }

    public void setChequePaidAt(Date chequePaidAt) {
        this.chequePaidAt = chequePaidAt;
    }

    public boolean isChequePaid() {
        return chequePaid;
    }

    public void setChequePaid(boolean chequePaid) {
        this.chequePaid = chequePaid;
    }

    
    
    public WebUser getChequeRealizer() {
        return chequeRealizer;
    }

    public void setChequeRealizer(WebUser chequeRealizer) {
        this.chequeRealizer = chequeRealizer;
    }

    public Date getChequeRealizedAt() {
        return chequeRealizedAt;
    }

    public void setChequeRealizedAt(Date chequeRealizedAt) {
        this.chequeRealizedAt = chequeRealizedAt;
    }

    public boolean isChequeRealized() {
        return chequeRealized;
    }

    public void setChequeRealized(boolean chequeRealized) {
        this.chequeRealized = chequeRealized;
    }

    public Bill getChequePaidBill() {
        return chequePaidBill;
    }

    public void setChequePaidBill(Bill chequePaidBill) {
        this.chequePaidBill = chequePaidBill;
    }

    public Bill getChequeRealizedBill() {
        return chequeRealizedBill;
    }

    public void setChequeRealizedBill(Bill chequeRealizedBill) {
        this.chequeRealizedBill = chequeRealizedBill;
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

}
