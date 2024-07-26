/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity;

import com.divudi.data.Denomination;
import com.divudi.data.PaymentMethod;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    Date writtenAt;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date toRealizeAt;

    @Enumerated(EnumType.STRING)
    PaymentMethod paymentMethod;

    boolean realized;

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
    private String currencyDenominationsJson;

    @ManyToOne
    Institution institution;

    @ManyToOne
    Department department;

    @Transient
    private List<Denomination> currencyDenominations;

    @Transient
    private List<String> humanReadableDenominations;

    private String referenceNo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

        // Copying attributes
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
                System.out.println("Error deserializing currency denominations: " + e.getMessage());
                this.currencyDenominations = new ArrayList<>(); // Initialize to an empty list on error
            }
        } else {
            this.currencyDenominations = new ArrayList<>(); // Initialize to an empty list if JSON is null or empty
        }
    }

    public String getCurrencyDenominationsJson() {
        return currencyDenominationsJson;
    }

    public List<Denomination> getCurrencyDenominations() {
        return currencyDenominations;
    }

    public void setCurrencyDenominations(List<Denomination> currencyDenominations) {
        this.currencyDenominations = currencyDenominations;
    }

    public String getReferenceNo() {
        
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

   

}
