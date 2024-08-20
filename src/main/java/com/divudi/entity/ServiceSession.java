/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity;

import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author Buddhika
 */
@Entity
public class ServiceSession extends Item implements Serializable {

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
    @OneToOne(mappedBy = "afterSession")
    ServiceSession beforeSession;
    /////Newly Added
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date startingTime;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date endingTime;
    
    private Integer numberOfDaysForAutomaticInstanceCreation;

    boolean refundable = false;
    int displayCount;
    double displayPercent;
    double duration;
    int roomNo;
//    int durationIncrementCount = 1;
//    boolean showAppointmentCount = true;
//    boolean oncallBookingsAllowed = true;
//    long advanceAppointmentPeriod = 10;
//    int advanceAPpointmentPeriodUnit = Calendar.DATE;
//    boolean showAppointmentTime = true;

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
    boolean serviceSessionCreateForOriginatingSession=false;

    //new Adittions
    private int recervedNumbers;
    private boolean paidAppointmentsOnly;
    private boolean excludeFromPatientPortal;
    private boolean canChangePatient;

    @Lob
    private String activities;
    @Lob
    private String actions;
    @Lob
    private String notificationRoles;
    @Lob
    private String dataEntryForms;

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

    public ServiceSession getBeforeSession() {
        return beforeSession;
    }

    public void setBeforeSession(ServiceSession beforeSession) {
        this.beforeSession = beforeSession;
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

        if (!(object instanceof ServiceSession)) {
            return false;
        }
        ServiceSession other = (ServiceSession) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.ServiceSession[ id=" + id + " ]";
    }

    public String getDayString() {
        if (sessionWeekday==null) {
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
        ServiceSession ses = this;
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

    public Integer getNumberOfDaysForAutomaticInstanceCreation() {
        return numberOfDaysForAutomaticInstanceCreation;
    }

    public void setNumberOfDaysForAutomaticInstanceCreation(Integer numberOfDaysForAutomaticInstanceCreation) {
        this.numberOfDaysForAutomaticInstanceCreation = numberOfDaysForAutomaticInstanceCreation;
    }

    public int getRecervedNumbers() {
        return recervedNumbers;
    }

    public void setRecervedNumbers(int recervedNumbers) {
        this.recervedNumbers = recervedNumbers;
    }

    public boolean isPaidAppointmentsOnly() {
        return paidAppointmentsOnly;
    }

    public void setPaidAppointmentsOnly(boolean paidAppointmentsOnly) {
        this.paidAppointmentsOnly = paidAppointmentsOnly;
    }

    public boolean isCanChangePatient() {
        return canChangePatient;
    }

    public void setCanChangePatient(boolean canChangePatient) {
        this.canChangePatient = canChangePatient;
    }

    public String getActivities() {
        return activities;
    }

    public void setActivities(String activities) {
        this.activities = activities;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public String getNotificationRoles() {
        return notificationRoles;
    }

    public void setNotificationRoles(String notificationRoles) {
        this.notificationRoles = notificationRoles;
    }

    public String getDataEntryForms() {
        return dataEntryForms;
    }

    public void setDataEntryForms(String dataEntryForms) {
        this.dataEntryForms = dataEntryForms;
    }

    public boolean isExcludeFromPatientPortal() {
        return excludeFromPatientPortal;
    }

    public void setExcludeFromPatientPortal(boolean excludeFromPatientPortal) {
        this.excludeFromPatientPortal = excludeFromPatientPortal;
    }

   
    
    
    
    

}
