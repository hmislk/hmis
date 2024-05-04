package com.divudi.entity.channel;

import com.divudi.data.ItemType;
import com.divudi.data.SessionNumberType;
import com.divudi.data.lab.Priority;
import com.divudi.entity.BillSession;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.ItemFee;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.SessionNumberGenerator;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Entity
public class SessionInstance implements Serializable {
    
    @ManyToOne
    private BillSession nextInLineBillSession;
    @ManyToOne
    private BillSession currentlyConsultingBillSession;
    @ManyToOne
    private BillSession lastCompletedBillSession;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Double total = 0.0;
    private Double totalForForeigner = 0.0;
    private Boolean discountAllowed = false;
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

    @Transient
    private boolean currentlyOngoing;

    private boolean started;
    @ManyToOne
    private WebUser startedBy;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date startedAt;

    private boolean completed;
    @ManyToOne
    private WebUser completedBy;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date completedAt;

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
//    
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;
    //Editer Properties
    @ManyToOne
    WebUser editer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date editedAt;

    private double dblValue = 0.0f;
    @Enumerated
    SessionNumberType sessionNumberType;

    @Lob
    String descreption;
    @Lob
    String comments;

    @Temporal(javax.persistence.TemporalType.DATE)
    Date effectiveFrom;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date effectiveTo;
    private boolean scanFee;
    double profitMargin;

    String creditNumbers;
    String cashNumbers;
    String agencyNumbers;
    String reserveName;
    String reserveNumbers;
    int maxTableRows;
    @Enumerated(EnumType.STRING)
    private ItemType itemType;
    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Transient
    double channelStaffFee;
    @Transient
    double channelHosFee;
    @Transient
    double channelAgentFee;
    @Transient
    double channelOnCallFee;

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

    @OneToOne
    ServiceSession afterSession;
    @ManyToOne
    SessionNumberGenerator sessionNumberGenerator;
    /////Newly Added
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date startingTime;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date endingTime;

    boolean refundable = false;
    int displayCount;
    double displayPercent;
    double duration;
    int roomNo;
    private int durationIncrementCount = 1;
    private boolean showAppointmentCount = true;
    private boolean oncallBookingsAllowed = true;
    private long advanceAppointmentPeriod = 10;
    private int advanceAPpointmentPeriodUnit = Calendar.DATE;
    private boolean showAppointmentTime = true;

    //Deactivate Properties(Doctor Leave)
    boolean deactivated;
    String deactivateComment;

    @ManyToOne(fetch = FetchType.EAGER)
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

    private Long bookedPatientCount;
    private Long paidPatientCount;
    private Long completedPatientCount;
    private Long remainingPatientCount;
    
    private boolean arrived;
    
    @ManyToOne
    private ArrivalRecord arrivalRecord;

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

    public ServiceSession getAfterSession() {
        return afterSession;
    }

    public void setAfterSession(ServiceSession afterSession) {
        this.afterSession = afterSession;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        SessionInstance ses = this;
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
    private Boolean printFeesForBills;

    @Transient
    private List<ItemFee> itemFeesActive;

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

    public String getDescreption() {
        return descreption;
    }

    public void setDescreption(String descreption) {
        this.descreption = descreption;
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

    public double getProfitMargin() {
        return profitMargin;
    }

    public void setProfitMargin(double profitMargin) {
        this.profitMargin = profitMargin;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
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

    public String getReserveNumbers() {
        return reserveNumbers;
    }

    public void setReserveNumbers(String reserveNumbers) {
        this.reserveNumbers = reserveNumbers;
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

    public void setChannelStaffFee(double channelStaffFee) {
        this.channelStaffFee = channelStaffFee;
    }

    public void setChannelHosFee(double channelHosFee) {
        this.channelHosFee = channelHosFee;
    }

    public void setChannelAgentFee(double channelAgentFee) {
        this.channelAgentFee = channelAgentFee;
    }

    public void setChannelOnCallFee(double channelOnCallFee) {
        this.channelOnCallFee = channelOnCallFee;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Double getTotalForForeigner() {
        return totalForForeigner;
    }

    public void setTotalForForeigner(Double totalForForeigner) {
        this.totalForForeigner = totalForForeigner;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.SessionInstance[ id=" + id + " ]";
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof SessionInstance)) {
            return false;
        }
        SessionInstance other = (SessionInstance) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    public int getDurationIncrementCount() {
        return durationIncrementCount;
    }

    public void setDurationIncrementCount(int durationIncrementCount) {
        this.durationIncrementCount = durationIncrementCount;
    }

    public boolean isShowAppointmentCount() {
        return showAppointmentCount;
    }

    public void setShowAppointmentCount(boolean showAppointmentCount) {
        this.showAppointmentCount = showAppointmentCount;
    }

    public boolean isOncallBookingsAllowed() {
        return oncallBookingsAllowed;
    }

    public void setOncallBookingsAllowed(boolean oncallBookingsAllowed) {
        this.oncallBookingsAllowed = oncallBookingsAllowed;
    }

    public long getAdvanceAppointmentPeriod() {
        return advanceAppointmentPeriod;
    }

    public void setAdvanceAppointmentPeriod(long advanceAppointmentPeriod) {
        this.advanceAppointmentPeriod = advanceAppointmentPeriod;
    }

    public int getAdvanceAPpointmentPeriodUnit() {
        return advanceAPpointmentPeriodUnit;
    }

    public void setAdvanceAPpointmentPeriodUnit(int advanceAPpointmentPeriodUnit) {
        this.advanceAPpointmentPeriodUnit = advanceAPpointmentPeriodUnit;
    }

    public boolean isShowAppointmentTime() {
        return showAppointmentTime;
    }

    public void setShowAppointmentTime(boolean showAppointmentTime) {
        this.showAppointmentTime = showAppointmentTime;
    }

    public Boolean getPrintFeesForBills() {
        return printFeesForBills;
    }

    public void setPrintFeesForBills(Boolean printFeesForBills) {
        this.printFeesForBills = printFeesForBills;
    }

    public List<ItemFee> getItemFeesActive() {
        return itemFeesActive;
    }

    public void setItemFeesActive(List<ItemFee> itemFeesActive) {
        this.itemFeesActive = itemFeesActive;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public double getChannelStaffFee() {
        return channelStaffFee;
    }

    public double getChannelHosFee() {
        return channelHosFee;
    }

    public double getChannelAgentFee() {
        return channelAgentFee;
    }

    public double getChannelOnCallFee() {
        return channelOnCallFee;
    }

    public boolean isCurrentlyOngoing() {
        return started && !completed;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public WebUser getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(WebUser startedBy) {
        this.startedBy = startedBy;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public WebUser getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(WebUser completedBy) {
        this.completedBy = completedBy;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public Long getBookedPatientCount() {
        return bookedPatientCount;
    }

    public void setBookedPatientCount(Long bookedPatientCount) {
        this.bookedPatientCount = bookedPatientCount;
    }

    public Long getPaidPatientCount() {
        return paidPatientCount;
    }

    public void setPaidPatientCount(Long paidPatientCount) {
        this.paidPatientCount = paidPatientCount;
    }

    public Long getCompletedPatientCount() {
        return completedPatientCount;
    }

    public void setCompletedPatientCount(Long completedPatientCount) {
        this.completedPatientCount = completedPatientCount;
    }

    public Long getRemainingPatientCount() {
        return remainingPatientCount;
    }

    public void setRemainingPatientCount(Long remainingPatientCount) {
        this.remainingPatientCount = remainingPatientCount;
    }

    public BillSession getNextInLineBillSession() {
        return nextInLineBillSession;
    }

    public void setNextInLineBillSession(BillSession nextInLineBillSession) {
        this.nextInLineBillSession = nextInLineBillSession;
    }

    public BillSession getCurrentlyConsultingBillSession() {
        return currentlyConsultingBillSession;
    }

    public void setCurrentlyConsultingBillSession(BillSession currentlyConsultingBillSession) {
        this.currentlyConsultingBillSession = currentlyConsultingBillSession;
    }

    public BillSession getLastCompletedBillSession() {
        return lastCompletedBillSession;
    }

    public void setLastCompletedBillSession(BillSession lastCompletedBillSession) {
        this.lastCompletedBillSession = lastCompletedBillSession;
    }

    public boolean isArrived() {
        return arrived;
    }

    public void setArrived(boolean arrived) {
        this.arrived = arrived;
    }

    public ArrivalRecord getArrivalRecord() {
        return arrivalRecord;
    }

    public void setArrivalRecord(ArrivalRecord arrivalRecord) {
        this.arrivalRecord = arrivalRecord;
    }
    
    

}
