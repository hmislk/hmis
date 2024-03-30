package com.divudi.entity;

import com.divudi.data.BillType;
import com.divudi.data.SessionNumberType;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Entity
@Deprecated
public class ServiceSessionInstance implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Double total = 0.0;
    private Double totalForForeigner = 0.0;
    private Boolean discountAllowed = false;
    @ManyToOne
    private Institution institution;
    @ManyToOne
    private Department department;
    @ManyToOne
    private Speciality speciality;
    @ManyToOne
    private Staff staff;
    @ManyToOne
    private Institution forInstitution;
    @ManyToOne
    private Department forDepartment;

    @Enumerated(EnumType.STRING)
    private BillType forBillType;

    private String name;
    private String sname;
    private String tname;
    private String code;
    private String barcode;
    private String printName;
    private String shortName;
    private String fullName;
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)

    private Date createdAt;
    private boolean retired;

    @ManyToOne
    private WebUser retirer;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;

    private String retireComments;
    //Editer Properties
    @ManyToOne
    private WebUser editer;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date editedAt;

    @Enumerated(EnumType.STRING)
    private InwardChargeType inwardChargeType;
    private double dblValue = 0.0f;
    private SessionNumberType sessionNumberType;
    private boolean priceByBatch;
    private boolean billable;
    private boolean vatable;
    private boolean formatable;
    private boolean patientNotRequired;
    private boolean chargesVisibleForInward;
    private boolean requestForQuentity;
    private boolean marginNotAllowed;
    private double vatPercentage;

    private boolean scanFee;
    private double profitMargin;

    private String creditNumbers;
    private String cashNumbers;
    private String agencyNumbers;
    private String reserveName;
    private String reserveNumbers;
    private int maxTableRows;

    @Transient
    private double channelStaffFee;
    @Transient
    private double channelHosFee;
    @Transient
    private double channelAgentFee;
    @Transient
    private double channelOnCallFee;

    @Transient
    private String transName;

    @Transient
    private double hospitalFee;
    @Transient
    private double professionalFee;
    @Transient
    private double hospitalFfee;
    @Transient
    private double professionalFfee;
    @Transient
    private double taxFee;
    @Transient
    private double taxFfee;
    @Transient
    private double otherFee;
    @Transient
    private double otherFfee;
    @Transient
    private double totalFee;
    @Transient
    private double totalFfee;
    @Transient
    private List<ItemFee> itemFees;

    @Transient
    private List<ItemFee> itemFeesActive;

    @Transient
    private String transCodeFromName;

    @Transient
    String dayString;
    @Transient
    String sessionText;

    Integer sessionWeekday;
    @Temporal(javax.persistence.TemporalType.DATE)
    Date sessionDate;
    @Temporal(javax.persistence.TemporalType.TIME)
    Date sessionTime;
    @Temporal(javax.persistence.TemporalType.TIME)
    Date sessionAt;
    int startingNo;
    int numberIncrement;
    int maxNo;

    boolean continueNumbers;

    @ManyToOne
    SessionNumberGenerator sessionNumberGenerator;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date startingTime;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date endingTime;

    boolean refundable = false;
    int displayCount;
    double displayPercent;
    double duration;
    int roomNo;
    boolean deactivated;
    String deactivateComment;

    @ManyToOne
    ServiceSession originatingSession;

    @Transient
    int transDisplayCountWithoutCancelRefund;
    @Transient
    int transCreditBillCount;

    @Transient
    int transRowNumber;
    @Transient
    Boolean arival;
    @Transient
    boolean serviceSessionCreateForOriginatingSession = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SessionNumberGenerator getSessionNumberGenerator() {
        return sessionNumberGenerator;
    }

    public void setSessionNumberGenerator(SessionNumberGenerator sessionNumberGenerator) {
        this.sessionNumberGenerator = sessionNumberGenerator;
    }

    public ServiceSession getOriginatingSession() {
        return originatingSession;
    }

    public void setOriginatingSession(ServiceSession originatingSession) {
        this.originatingSession = originatingSession;
    }

    public Integer getSessionWeekday() {
        return sessionWeekday;
    }

    public void setSessionWeekday(Integer sessionWeekday) {
        this.sessionWeekday = sessionWeekday;
    }

    public Date getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(Date sessionDate) {
        this.sessionDate = sessionDate;
    }

    public Date getSessionTime() {
        return sessionTime;
    }

    public void setSessionTime(Date sessionTime) {
        this.sessionTime = sessionTime;
    }

    public Date getSessionAt() {
        return sessionAt;
    }

    public void setSessionAt(Date sessionAt) {
        this.sessionAt = sessionAt;
    }

    public int getStartingNo() {
        return startingNo;
    }

    public void setStartingNo(int startingNo) {
        this.startingNo = startingNo;
    }

    public int getNumberIncrement() {
        return numberIncrement;
    }

    public void setNumberIncrement(int numberIncrement) {
        this.numberIncrement = numberIncrement;
    }

    public int getMaxNo() {
        return maxNo;
    }

    public void setMaxNo(int maxNo) {
        this.maxNo = maxNo;
    }

    public boolean isContinueNumbers() {
        return continueNumbers;
    }

    public void setContinueNumbers(boolean continueNumbers) {
        this.continueNumbers = continueNumbers;
    }

    public String getDayString() {
        if (sessionWeekday == null) {
            return "";
        }
        switch (sessionWeekday) {
            case 1:
                dayString = "Sunday";
                break;
            case 2:
                dayString = "Monday";
                break;
            case 3:
                dayString = "Tuesday";
                break;
            case 4:
                dayString = "Wednesday";
                break;
            case 5:
                dayString = "Thursday";
                break;
            case 6:
                dayString = "Friday";
                break;
            case 7:
                dayString = "Sutarday";
                break;

        }
        return dayString;
    }

    public Date getStartingTime() {
        return startingTime;
    }

    public Date getTransStartTime() {
        Calendar st = Calendar.getInstance();
        Calendar start = Calendar.getInstance();
        if (sessionAt == null || startingTime == null) {
            return null;
        }
        st.setTime(sessionAt);
        start.setTime(startingTime);
        st.set(Calendar.HOUR, start.get(Calendar.HOUR));
        st.set(Calendar.MINUTE, start.get(Calendar.MINUTE));
        st.set(Calendar.SECOND, start.get(Calendar.SECOND));
        st.set(Calendar.MILLISECOND, start.get(Calendar.MILLISECOND));
        return st.getTime();
    }

    public Date getTransEndTime() {
        Calendar st = Calendar.getInstance();
        Calendar ending = Calendar.getInstance();
//        //// // System.out.println("sessionAt = " + sessionAt);
        if (sessionAt == null || getEndingTime() == null) {
            return null;
        }
        st.setTime(sessionAt);
        ending.setTime(endingTime);
        st.set(Calendar.HOUR, ending.get(Calendar.HOUR));
        st.set(Calendar.MINUTE, ending.get(Calendar.MINUTE));
        st.set(Calendar.SECOND, ending.get(Calendar.SECOND));
        st.set(Calendar.MILLISECOND, ending.get(Calendar.MILLISECOND));
        return st.getTime();
    }

    public void setStartingTime(Date startingTime) {
        this.startingTime = startingTime;
    }

    public boolean isRefundable() {
        return refundable;
    }

    public void setRefundable(boolean refundable) {
        this.refundable = refundable;
    }

    public int getDisplayCount() {
        return displayCount;
    }

    public void setDisplayCount(int displayCount) {
        this.displayCount = displayCount;
    }

    public double getDisplayPercent() {
        return displayPercent;
    }

    public void setDisplayPercent(double displayPercent) {
        this.displayPercent = displayPercent;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public int getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(int roomNo) {
        this.roomNo = roomNo;
    }

    public Date getEndingTime() {
        if (endingTime == null) {
            if (startingTime == null) {
                endingTime = null;
            } else {
                Calendar e = Calendar.getInstance();
                e.setTime(startingTime);
                e.add(Calendar.HOUR, 2);
                endingTime = e.getTime();
            }
        }
        return endingTime;
    }

    public void setEndingTime(Date endingTime) {
        this.endingTime = endingTime;
    }

    public int getTransDisplayCountWithoutCancelRefund() {
        return transDisplayCountWithoutCancelRefund;
    }

    public void setTransDisplayCountWithoutCancelRefund(int transDisplayCountWithoutCancelRefund) {
        this.transDisplayCountWithoutCancelRefund = transDisplayCountWithoutCancelRefund;
    }

    public int getTransCreditBillCount() {
        return transCreditBillCount;
    }

    public void setTransCreditBillCount(int transCreditBillCount) {
        this.transCreditBillCount = transCreditBillCount;
    }

    public boolean isDeactivated() {
        return deactivated;
    }

    public void setDeactivated(boolean deactivated) {
        this.deactivated = deactivated;
    }

    public int getTransRowNumber() {
        return transRowNumber;
    }

    public void setTransRowNumber(int transRowNumber) {
        this.transRowNumber = transRowNumber;
    }

    public String getDeactivateComment() {
        return deactivateComment;
    }

    public void setDeactivateComment(String deactivateComment) {
        this.deactivateComment = deactivateComment;
    }

    public String getSessionText() {
        sessionText = "";
        ServiceSessionInstance ses = this;
        SimpleDateFormat dt1;
        if (!ses.deactivated) {
            dt1 = new SimpleDateFormat("E");
            sessionText += dt1.format(ses.getSessionDate());
            sessionText += " &nbsp;&nbsp;";
            dt1 = new SimpleDateFormat("MMM/dd");
            sessionText += dt1.format(ses.getSessionDate());
            sessionText += " &nbsp;&nbsp;";
            dt1 = new SimpleDateFormat("hh:mm a");
            sessionText += dt1.format(ses.getStartingTime());
            sessionText += " &nbsp;&nbsp;";
            sessionText += CommonFunctions.round(ses.totalFee);
            sessionText += " &nbsp;&nbsp;";
            sessionText += "<font color='green'>";
            sessionText += ses.getTransDisplayCountWithoutCancelRefund();
            sessionText += "</font>";
            sessionText += CommonFunctions.round(ses.totalFee);
            if (ses.getMaxNo() != 0) {

            }

        }
        return sessionText;
    }

    public void setSessionText(String sessionText) {
        this.sessionText = sessionText;
    }

    public Boolean getArival() {
        return arival;
    }

    public void setArival(Boolean arival) {
        this.arival = arival;
    }

    public boolean isServiceSessionCreateForOriginatingSession() {
        return serviceSessionCreateForOriginatingSession;
    }

    public void setServiceSessionCreateForOriginatingSession(boolean serviceSessionCreateForOriginatingSession) {
        this.serviceSessionCreateForOriginatingSession = serviceSessionCreateForOriginatingSession;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ServiceSessionInstance)) {
            return false;
        }
        ServiceSessionInstance other = (ServiceSessionInstance) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.ServiceSessionInstance[ id=" + id + " ]";
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
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

    public WebUser getEditer() {
        return editer;
    }

    public void setEditer(WebUser editer) {
        this.editer = editer;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }

    public InwardChargeType getInwardChargeType() {
        return inwardChargeType;
    }

    public void setInwardChargeType(InwardChargeType inwardChargeType) {
        this.inwardChargeType = inwardChargeType;
    }

    public SessionNumberType getSessionNumberType() {
        return sessionNumberType;
    }

    public void setSessionNumberType(SessionNumberType sessionNumberType) {
        this.sessionNumberType = sessionNumberType;
    }

    public boolean isPriceByBatch() {
        return priceByBatch;
    }

    public void setPriceByBatch(boolean priceByBatch) {
        this.priceByBatch = priceByBatch;
    }

    public boolean isBillable() {
        return billable;
    }

    public void setBillable(boolean billable) {
        this.billable = billable;
    }

    public boolean isVatable() {
        return vatable;
    }

    public void setVatable(boolean vatable) {
        this.vatable = vatable;
    }

    public boolean isFormatable() {
        return formatable;
    }

    public void setFormatable(boolean formatable) {
        this.formatable = formatable;
    }

    public boolean isPatientNotRequired() {
        return patientNotRequired;
    }

    public void setPatientNotRequired(boolean patientNotRequired) {
        this.patientNotRequired = patientNotRequired;
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

    public boolean isMarginNotAllowed() {
        return marginNotAllowed;
    }

    public void setMarginNotAllowed(boolean marginNotAllowed) {
        this.marginNotAllowed = marginNotAllowed;
    }

    public double getVatPercentage() {
        return vatPercentage;
    }

    public void setVatPercentage(double vatPercentage) {
        this.vatPercentage = vatPercentage;
    }

    public double getProfitMargin() {
        return profitMargin;
    }

    public void setProfitMargin(double profitMargin) {
        this.profitMargin = profitMargin;
    }

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

    public String getReserveNumbers() {
        return reserveNumbers;
    }

    public void setReserveNumbers(String reserveNumbers) {
        this.reserveNumbers = reserveNumbers;
    }

    public int getMaxTableRows() {
        return maxTableRows;
    }

    public void setMaxTableRows(int maxTableRows) {
        this.maxTableRows = maxTableRows;
    }

    public double getChannelStaffFee() {
        return channelStaffFee;
    }

    public void setChannelStaffFee(double channelStaffFee) {
        this.channelStaffFee = channelStaffFee;
    }

    public double getChannelHosFee() {
        return channelHosFee;
    }

    public void setChannelHosFee(double channelHosFee) {
        this.channelHosFee = channelHosFee;
    }

    public double getChannelAgentFee() {
        return channelAgentFee;
    }

    public void setChannelAgentFee(double channelAgentFee) {
        this.channelAgentFee = channelAgentFee;
    }

    public double getChannelOnCallFee() {
        return channelOnCallFee;
    }

    public void setChannelOnCallFee(double channelOnCallFee) {
        this.channelOnCallFee = channelOnCallFee;
    }

    public String getTransName() {
        return transName;
    }

    public void setTransName(String transName) {
        this.transName = transName;
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

    public List<ItemFee> getItemFees() {
        return itemFees;
    }

    public void setItemFees(List<ItemFee> itemFees) {
        this.itemFees = itemFees;
    }

    public Double getTotalForForeigner() {
        return totalForForeigner;
    }

    public void setTotalForForeigner(Double totalForForeigner) {
        this.totalForForeigner = totalForForeigner;
    }

    public double getDblValue() {
        return dblValue;
    }

    public void setDblValue(double dblValue) {
        this.dblValue = dblValue;
    }

    public boolean isScanFee() {
        return scanFee;
    }

    public void setScanFee(boolean scanFee) {
        this.scanFee = scanFee;
    }

    public List<ItemFee> getItemFeesActive() {
        return itemFeesActive;
    }

    public void setItemFeesActive(List<ItemFee> itemFeesActive) {
        this.itemFeesActive = itemFeesActive;
    }

    public String getTransCodeFromName() {
        return transCodeFromName;
    }

    public void setTransCodeFromName(String transCodeFromName) {
        this.transCodeFromName = transCodeFromName;
    }

}
