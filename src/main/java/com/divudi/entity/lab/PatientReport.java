/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.lab;

//import ch.lambdaj.Lambda;
import com.divudi.data.InvestigationItemType;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.Transient;
//import org.hamcrest.Matchers;

/**
 *
 * @author Buddhika
 */
@Entity
public class PatientReport implements Serializable {

    @OneToMany(mappedBy = "patientReport", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PatientReportItemValue> patientReportItemValues;

//    @Transient
//    private List<PatientReportItemValue> patientReportItemOfValueType;
//
//    @Transient
//    private List<PatientReportItemValue> patientReportItemOfFlagType;
//
//    @Transient
//    private List<PatientReportItemValue> patientReportItemOfCalculationType;
//
//    @Transient
//    private List<PatientReportItemValue> patientReportItemOfDynamicLabelType;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    private Item item;
    @ManyToOne
    private PatientInvestigation patientInvestigation;
    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retairing properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;
    //DataEntry
    private Boolean dataEntered = false;
    @ManyToOne
    private WebUser dataEntryUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date dataEntryAt;
    private String dataEntryComments;
    @ManyToOne
    private Department dataEntryDepartment;
    @ManyToOne
    private Institution dataEntryInstitution;
    //Approve
    private Boolean approved = false;
    @ManyToOne
    private WebUser approveUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date approveAt;
    private String approveComments;
    @ManyToOne
    private Department approveDepartment;
    @ManyToOne
    private Institution approveInstitution;
    //Printing
    private Boolean printed = false;
    @ManyToOne
    private WebUser printingUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date printingAt;
    private String printingComments;
    @ManyToOne
    private Department printingDepartment;
    @ManyToOne
    private Institution printingInstitution;
    //Cancellation
    private Boolean cancelled = false;
    @ManyToOne
    private WebUser cancelledUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date cancelledAt;
    private String cancellComments;
    @ManyToOne
    private Department cancellDepartment;
    @ManyToOne
    private Institution cancellInstitution;
    //Return
    private Boolean returned = false;
    @ManyToOne
    private WebUser returnedUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date returnedAt;
    private String returnComments;
    @ManyToOne
    private Department returnDepartment;
    @ManyToOne
    private Institution returnInstitution;
    @Transient
    Boolean transHasAbst;
    @Transient
    Investigation transInvestigation;

    public Investigation getTransInvestigation() {
        if (item instanceof Investigation) {
            transInvestigation=(Investigation) item;
        }else{
            transInvestigation = null;
        }
        return transInvestigation;
    }

//    public List<PatientReportItemValue> getPatientReportItemOfValueType() {
//        if (patientReportItemOfValueType == null) {
//            patientReportItemOfValueType = Lambda.filter(Lambda.having(Lambda.on(PatientReportItemValue.class).getInvestigationItem().getIxItemType(), 
//                    Matchers.equalTo(InvestigationItemType.Value)), getPatientReportItemValues());
//        }
//        return patientReportItemOfValueType;
//    }
//    public List<PatientReportItemValue> getPatientReportItemOfFlagType() {
//        if (patientReportItemOfFlagType == null) {
//            patientReportItemOfFlagType = Lambda.filter(Lambda.having(Lambda.on(PatientReportItemValue.class).getInvestigationItem().getIxItemType(), Matchers.equalTo(InvestigationItemType.Flag)), getPatientReportItemValues());
//        }
//        return patientReportItemOfFlagType;
//    }
//    public List<PatientReportItemValue> getPatientReportItemOfCalculationType() {
//        if (patientReportItemOfCalculationType == null) {
//            patientReportItemOfCalculationType = Lambda.filter(Lambda.having(Lambda.on(PatientReportItemValue.class).getInvestigationItem().getIxItemType(), Matchers.equalTo(InvestigationItemType.Calculation)), getPatientReportItemValues());
//        }
//        return patientReportItemOfCalculationType;
//    }
//    public List<PatientReportItemValue> getPatientReportItemOfDynamicLabelType() {
//        if (patientReportItemOfDynamicLabelType == null) {
//            patientReportItemOfDynamicLabelType = Lambda.filter(Lambda.having(Lambda.on(PatientReportItemValue.class).getInvestigationItem().getIxItemType(), Matchers.equalTo(InvestigationItemType.DynamicLabel)), getPatientReportItemValues());
//        }
//        return patientReportItemOfDynamicLabelType;
//    }
    @Transient
    boolean filteredAndSorted = false;

    public void sortValues() {
        if (patientReportItemValues != null) {
            Collections.sort(patientReportItemValues, new PatientReportItemValueComparator());
//            patientReportItemOfCalculationType = null;
//            patientReportItemOfDynamicLabelType = null;
//            patientReportItemOfFlagType = null;
//            patientReportItemOfValueType = null;
            filteredAndSorted = true;
        }
    }

    public List<PatientReportItemValue> getPatientReportItemValues() {
        if (patientReportItemValues != null) {
            if (filteredAndSorted == false) {
                Collections.sort(patientReportItemValues, new PatientReportItemValueComparator());
//                patientReportItemOfCalculationType = null;
//                patientReportItemOfDynamicLabelType = null;
//                patientReportItemOfFlagType = null;
//                patientReportItemOfValueType = null;
                filteredAndSorted = true;
            }
        } else {
            patientReportItemValues = new ArrayList<>();
        }
        return patientReportItemValues;
    }

    public void setPatientReportItemValues(List<PatientReportItemValue> patientReportItemValues) {
        this.patientReportItemValues = patientReportItemValues;
    }

    public Boolean getDataEntered() {
        return dataEntered;
    }

    public void setDataEntered(Boolean dataEntered) {
        this.dataEntered = dataEntered;
    }

    public WebUser getDataEntryUser() {
        return dataEntryUser;
    }

    public void setDataEntryUser(WebUser dataEntryUser) {
        this.dataEntryUser = dataEntryUser;
    }

    public Date getDataEntryAt() {
        return dataEntryAt;
    }

    public void setDataEntryAt(Date dataEntryAt) {
        this.dataEntryAt = dataEntryAt;
    }

    public String getDataEntryComments() {
        return dataEntryComments;
    }

    public void setDataEntryComments(String dataEntryComments) {
        this.dataEntryComments = dataEntryComments;
    }

    public Department getDataEntryDepartment() {
        return dataEntryDepartment;
    }

    public void setDataEntryDepartment(Department dataEntryDepartment) {
        this.dataEntryDepartment = dataEntryDepartment;
    }

    public Institution getDataEntryInstitution() {
        return dataEntryInstitution;
    }

    public void setDataEntryInstitution(Institution dataEntryInstitution) {
        this.dataEntryInstitution = dataEntryInstitution;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
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

    public String getApproveComments() {
        return approveComments;
    }

    public void setApproveComments(String approveComments) {
        this.approveComments = approveComments;
    }

    public Department getApproveDepartment() {
        return approveDepartment;
    }

    public void setApproveDepartment(Department approveDepartment) {
        this.approveDepartment = approveDepartment;
    }

    public Institution getApproveInstitution() {
        return approveInstitution;
    }

    public void setApproveInstitution(Institution approveInstitution) {
        this.approveInstitution = approveInstitution;
    }

    public Boolean getPrinted() {
        return printed;
    }

    public void setPrinted(Boolean printed) {
        this.printed = printed;
    }

    public WebUser getPrintingUser() {
        return printingUser;
    }

    public void setPrintingUser(WebUser printingUser) {
        this.printingUser = printingUser;
    }

    public Date getPrintingAt() {
        return printingAt;
    }

    public void setPrintingAt(Date printingAt) {
        this.printingAt = printingAt;
    }

    public String getPrintingComments() {
        return printingComments;
    }

    public void setPrintingComments(String printingComments) {
        this.printingComments = printingComments;
    }

    public Department getPrintingDepartment() {
        return printingDepartment;
    }

    public void setPrintingDepartment(Department printingDepartment) {
        this.printingDepartment = printingDepartment;
    }

    public Institution getPrintingInstitution() {
        return printingInstitution;
    }

    public void setPrintingInstitution(Institution printingInstitution) {
        this.printingInstitution = printingInstitution;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public WebUser getCancelledUser() {
        return cancelledUser;
    }

    public void setCancelledUser(WebUser cancelledUser) {
        this.cancelledUser = cancelledUser;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellComments() {
        return cancellComments;
    }

    public void setCancellComments(String cancellComments) {
        this.cancellComments = cancellComments;
    }

    public Department getCancellDepartment() {
        return cancellDepartment;
    }

    public void setCancellDepartment(Department cancellDepartment) {
        this.cancellDepartment = cancellDepartment;
    }

    public Institution getCancellInstitution() {
        return cancellInstitution;
    }

    public void setCancellInstitution(Institution cancellInstitution) {
        this.cancellInstitution = cancellInstitution;
    }

    public Boolean getReturned() {
        return returned;
    }

    public void setReturned(Boolean returned) {
        this.returned = returned;
    }

    public WebUser getReturnedUser() {
        return returnedUser;
    }

    public void setReturnedUser(WebUser returnedUser) {
        this.returnedUser = returnedUser;
    }

    public Date getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(Date returnedAt) {
        this.returnedAt = returnedAt;
    }

    public String getReturnComments() {
        return returnComments;
    }

    public void setReturnComments(String returnComments) {
        this.returnComments = returnComments;
    }

    public Department getReturnDepartment() {
        return returnDepartment;
    }

    public void setReturnDepartment(Department returnDepartment) {
        this.returnDepartment = returnDepartment;
    }

    public Institution getReturnInstitution() {
        return returnInstitution;
    }

    public void setReturnInstitution(Institution returnInstitution) {
        this.returnInstitution = returnInstitution;
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

        if (!(object instanceof PatientReport)) {
            return false;
        }
        PatientReport other = (PatientReport) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "com.divudi.entity.lab.PatientReport[ id=" + id + " ]";
    }

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public void setPatientInvestigation(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
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

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }
    
    //AntiBiotic List
    public boolean getTransHasAbst() {

        transHasAbst = false;
        for (PatientReportItemValue priva : this.patientReportItemValues) {
            if (priva.strValue != null) {
                if (priva.strValue.equals("SENSITIVE") || priva.strValue.equals("Resistant") || priva.strValue.equals("Intermediate")) {
                    transHasAbst = true;
                    return transHasAbst;
                }
            }
        }

        return transHasAbst;
    }

    public void setTransHasAbst(Boolean transHasAbst) {
        this.transHasAbst = transHasAbst;
    }

    static class PatientReportItemValueComparator implements Comparator<PatientReportItemValue> {

        @Override
        public int compare(PatientReportItemValue o1, PatientReportItemValue o2) {
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            if (o1.getInvestigationItem() == null || o1.getInvestigationItem().getCssTop() == null) {
                return 1;
            }
            if (o2.getInvestigationItem() == null || o2.getInvestigationItem().getCssTop() == null) {
                return -1;
            }
            if (o1.getInvestigationItem().getCssTop().equalsIgnoreCase(o2.getInvestigationItem().getCssTop())) {
                return o1.getInvestigationItem().getName().compareTo(o2.getInvestigationItem().getName());  //To change body of generated methods, choose Tools | Templates.
            }
            return o1.getInvestigationItem().getCssTop().compareTo(o2.getInvestigationItem().getCssTop());  //To change body of generated methods, choose Tools | Templates.
        }
    }

}