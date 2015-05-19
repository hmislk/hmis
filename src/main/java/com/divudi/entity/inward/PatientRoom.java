/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.inward;

import com.divudi.entity.PatientEncounter;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author Buddhika
 */
@Entity
public class PatientRoom implements Serializable {

    @OneToOne(mappedBy = "previousRoom")
    private PatientRoom nextRoom;

    @OneToOne(mappedBy = "referencePatientRoom")
    private PatientRoom duplicatePatientRoom;
    boolean discharged;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
//Main Properties
    private String name;
    private String tName;
    private String sName;
    private String description;
    private int orderNo;
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

    @ManyToOne
    private RoomFacilityCharge roomFacilityCharge;
    @ManyToOne
    RoomFacilityCharge printRoomFacilityCharge;
    @ManyToOne
    private PatientEncounter patientEncounter;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date admittedAt;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date printAdmittedAt;
    @ManyToOne
    private WebUser addmittedBy;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date dischargedAt;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date printDischargeAt;
    @ManyToOne
    private WebUser dischargedBy;

    @OneToOne
    private PatientRoom previousRoom;

    private double currentMaintananceCharge = 0.0;
    private double currentNursingCharge = 0.0;
    private double currentMoCharge = 0.0;
    private double currentMoChargeForAfterDuration = 0.0;
    private double currentRoomCharge;
    private double currentLinenCharge = 0.0;
    double currentAdministrationCharge;
    double currentMedicalCareCharge;
    //Calculated
    private double calculatedRoomCharge = 0;
    private double calculatedMaintainCharge = 0;
    private double calculatedMoCharge = 0;
    private double calculatedNursingCharge = 0;
    double calculatedLinenCharge = 0;
    double calculatedAdministrationCharge = 0;
    double calculatedMedicalCareCharge = 0;
//Discount
    private double discountRoomCharge;
    private double discountMaintainCharge;
    private double discountMoCharge;
    private double discountNursingCharge;
    double discountLinenCharge;
    double discountAdministrationCharge;
    double discountMedicalCareCharge;
    //Adjusted
    private double adjustedRoomCharge;
    private double adjustedMaintainCharge;
    private double adjustedMoCharge;
    private double ajdustedNursingCharge;
    double ajdustedLinenCharge;
    double ajdustedAdministrationCharge;
    double ajdustedMedicalCareCharge;
    ///////Added 
    double addedRoomCharge = 0.0;
    double addedMaintainCharge;
    double addedMoCharge;
    double addedNursingCharge;
    double addedLinenCharge;
    double addedAdministrationCharge;
    double addedMedicalCareCharge;

    @OneToOne
    private PatientRoom referencePatientRoom;
    @Transient
    private long tmpStayedTime;
    @Transient
    private double tmpTotalRoomCharge;

    public double getAddedRoomCharge() {
        return addedRoomCharge;
    }

    public void setAddedRoomCharge(double addedRoomCharge) {
        this.addedRoomCharge = addedRoomCharge;
    }

    public double getAddedMaintainCharge() {
        return addedMaintainCharge;
    }

    public void setAddedMaintainCharge(double addedMaintainCharge) {
        this.addedMaintainCharge = addedMaintainCharge;
    }

    public double getAddedMoCharge() {
        return addedMoCharge;
    }

    public void setAddedMoCharge(double addedMoCharge) {
        this.addedMoCharge = addedMoCharge;
    }

    public double getAddedNursingCharge() {
        return addedNursingCharge;
    }

    public void setAddedNursingCharge(double addedNursingCharge) {
        this.addedNursingCharge = addedNursingCharge;
    }

    public double getAddedAdministrationCharge() {
        return addedAdministrationCharge;
    }

    public void setAddedAdministrationCharge(double addedAdministrationCharge) {
        this.addedAdministrationCharge = addedAdministrationCharge;
    }

    public double getAddedMedicalCareCharge() {
        return addedMedicalCareCharge;
    }

    public void setAddedMedicalCareCharge(double addedMedicalCareCharge) {
        this.addedMedicalCareCharge = addedMedicalCareCharge;
    }

    public PatientRoom getNextRoom() {
        return nextRoom;
    }

    public void setNextRoom(PatientRoom nextRoom) {
        this.nextRoom = nextRoom;
    }

    public boolean isDischarged() {
        return discharged;
    }

    public void setDischarged(boolean discharged) {
        this.discharged = discharged;
    }

    public double getCalculatedLinenCharge() {
        return calculatedLinenCharge;
    }

    public void setCalculatedLinenCharge(double calculatedLinenCharge) {
        this.calculatedLinenCharge = calculatedLinenCharge;
    }

    public double getCalculatedAdministrationCharge() {
        return calculatedAdministrationCharge;
    }

    public void setCalculatedAdministrationCharge(double calculatedAdministrationCharge) {
        this.calculatedAdministrationCharge = calculatedAdministrationCharge;
    }

    public double getCalculatedMedicalCareCharge() {
        return calculatedMedicalCareCharge;
    }

    public void setCalculatedMedicalCareCharge(double calculatedMedicalCareCharge) {
        this.calculatedMedicalCareCharge = calculatedMedicalCareCharge;
    }

    public double getDiscountLinenCharge() {
        return discountLinenCharge;
    }

    public void setDiscountLinenCharge(double discountLinenCharge) {
        this.discountLinenCharge = discountLinenCharge;
    }

    public double getDiscountAdministrationCharge() {
        return discountAdministrationCharge;
    }

    public void setDiscountAdministrationCharge(double discountAdministrationCharge) {
        this.discountAdministrationCharge = discountAdministrationCharge;
    }

    public double getDiscountMedicalCareCharge() {
        return discountMedicalCareCharge;
    }

    public void setDiscountMedicalCareCharge(double discountMedicalCareCharge) {
        this.discountMedicalCareCharge = discountMedicalCareCharge;
    }

    public double getAjdustedLinenCharge() {
        return ajdustedLinenCharge;
    }

    public void setAjdustedLinenCharge(double ajdustedLinenCharge) {
        this.ajdustedLinenCharge = ajdustedLinenCharge;
    }

    public double getAjdustedAdministrationCharge() {
        return ajdustedAdministrationCharge;
    }

    public void setAjdustedAdministrationCharge(double ajdustedAdministrationCharge) {
        this.ajdustedAdministrationCharge = ajdustedAdministrationCharge;
    }

    public double getAjdustedMedicalCareCharge() {
        return ajdustedMedicalCareCharge;
    }

    public void setAjdustedMedicalCareCharge(double ajdustedMedicalCareCharge) {
        this.ajdustedMedicalCareCharge = ajdustedMedicalCareCharge;
    }

    public double getCurrentLinenCharge() {
        return currentLinenCharge;
    }

    public void setCurrentLinenCharge(double currentLinenCharge) {
        this.currentLinenCharge = currentLinenCharge;
    }

    public double getCurrentAdministrationCharge() {
        return currentAdministrationCharge;
    }

    public void setCurrentAdministrationCharge(double currentAdministrationCharge) {
        this.currentAdministrationCharge = currentAdministrationCharge;
    }

    public double getCurrentMedicalCareCharge() {
        return currentMedicalCareCharge;
    }

    public void setCurrentMedicalCareCharge(double currentMedicalCareCharge) {
        this.currentMedicalCareCharge = currentMedicalCareCharge;
    }

    public RoomFacilityCharge getPrintRoomFacilityCharge() {
        if (printRoomFacilityCharge == null) {
            printRoomFacilityCharge = roomFacilityCharge;
        }
        return printRoomFacilityCharge;
    }

    public void setPrintRoomFacilityCharge(RoomFacilityCharge printRoomFacilityCharge) {

        this.printRoomFacilityCharge = printRoomFacilityCharge;
    }

    public Date getPrintAdmittedAt() {
        if (printAdmittedAt == null) {
            printAdmittedAt = admittedAt;
        }
        return printAdmittedAt;
    }

    public void setPrintAdmittedAt(Date printAdmittedAt) {
        this.printAdmittedAt = printAdmittedAt;
    }

    public Date getPrintDischargeAt() {
        if (printDischargeAt == null) {
            printDischargeAt = dischargedAt;
        }
        return printDischargeAt;
    }

    public void setPrintDischargeAt(Date printDischargeAt) {
        this.printDischargeAt = printDischargeAt;

    }

    public String getPatientRoomClass() {
        return this.getClass().toString();
    }

    public long getTmpStayedTime() {
        return tmpStayedTime;
    }

    public void setTmpStayedTime(long tmpStayedTime) {
        this.tmpStayedTime = tmpStayedTime;
    }

    public double getTmpTotalRoomCharge() {
        return tmpTotalRoomCharge;
    }

    public void setTmpTotalRoomCharge(double tmpTotalRoomCharge) {
        this.tmpTotalRoomCharge = tmpTotalRoomCharge;
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

        if (!(object instanceof PatientRoom)) {
            return false;
        }
        PatientRoom other = (PatientRoom) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.inward.PatientRoom[ id=" + id + " ]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
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

    public RoomFacilityCharge getRoomFacilityCharge() {
        return roomFacilityCharge;
    }

    public void setRoomFacilityCharge(RoomFacilityCharge roomFacilityCharge) {
        this.roomFacilityCharge = roomFacilityCharge;
        printRoomFacilityCharge = roomFacilityCharge;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Date getAdmittedAt() {
        return admittedAt;
    }

    public void setAdmittedAt(Date admittedAt) {
        this.admittedAt = admittedAt;
        printAdmittedAt = admittedAt;
    }

    public WebUser getAddmittedBy() {
        return addmittedBy;
    }

    public void setAddmittedBy(WebUser addmittedBy) {
        this.addmittedBy = addmittedBy;
    }

    public Date getDischargedAt() {
        return dischargedAt;
    }

    public void setDischargedAt(Date dischargedAt) {
        this.dischargedAt = dischargedAt;
        printDischargeAt = dischargedAt;
    }

    public WebUser getDischargedBy() {
        return dischargedBy;
    }

    public void setDischargedBy(WebUser dischargedBy) {
        this.dischargedBy = dischargedBy;
    }

    public PatientRoom getPreviousRoom() {
        return previousRoom;
    }

    public void setPreviousRoom(PatientRoom previousRoom) {
        this.previousRoom = previousRoom;
    }

    public String gettName() {
        return tName;
    }

    public void settName(String tName) {
        this.tName = tName;
    }

    public String getsName() {
        return sName;
    }

    public void setsName(String sName) {
        this.sName = sName;
    }

    public double getAddedLinenCharge() {

        return addedLinenCharge;
    }

    public void setAddedLinenCharge(double addedLinenCharge) {
        this.addedLinenCharge = addedLinenCharge;
    }

    public double getCurrentMaintananceCharge() {
        return currentMaintananceCharge;
    }

    public void setCurrentMaintananceCharge(double currentMaintananceCharge) {
        this.currentMaintananceCharge = currentMaintananceCharge;
    }

//    public double getCurrentLinenCharge() {
//        return currentLinenCharge;
//    }
//
//    public void setCurrentLinenCharge(double currentLinenCharge) {
//        this.currentLinenCharge = currentLinenCharge;
//    }
    public double getCurrentNursingCharge() {
        return currentNursingCharge;
    }

    public void setCurrentNursingCharge(double currentNursingCharge) {
        this.currentNursingCharge = currentNursingCharge;
    }

    public double getCurrentMoCharge() {
        return currentMoCharge;
    }

    public void setCurrentMoCharge(double currentMoCharge) {
        this.currentMoCharge = currentMoCharge;
    }

    public double getCurrentRoomCharge() {
        return currentRoomCharge;
    }

    public void setCurrentRoomCharge(double currentRoomCharge) {
        this.currentRoomCharge = currentRoomCharge;
    }

    public PatientRoom getReferencePatientRoom() {
        return referencePatientRoom;
    }

    public void setReferencePatientRoom(PatientRoom referencePatientRoom) {
        this.referencePatientRoom = referencePatientRoom;
    }

    public PatientRoom getDuplicatePatientRoom() {
        return duplicatePatientRoom;
    }

    public void setDuplicatePatientRoom(PatientRoom duplicatePatientRoom) {
        this.duplicatePatientRoom = duplicatePatientRoom;
    }

    public double getCalculatedRoomCharge() {
        return calculatedRoomCharge;
    }

    public void setCalculatedRoomCharge(double calculatedRoomCharge) {
        this.calculatedRoomCharge = calculatedRoomCharge;
    }

    public double getCalculatedMaintainCharge() {
        return calculatedMaintainCharge;
    }

    public void setCalculatedMaintainCharge(double calculatedMaintainCharge) {
        this.calculatedMaintainCharge = calculatedMaintainCharge;
    }

    public double getCalculatedMoCharge() {
        return calculatedMoCharge;
    }

    public void setCalculatedMoCharge(double calculatedMoCharge) {
        this.calculatedMoCharge = calculatedMoCharge;
    }

    public double getCalculatedNursingCharge() {
        return calculatedNursingCharge;
    }

    public void setCalculatedNursingCharge(double calculatedNursingCharge) {
        this.calculatedNursingCharge = calculatedNursingCharge;
    }

    public double getDiscountRoomCharge() {
        return discountRoomCharge;
    }

    public void setDiscountRoomCharge(double discountRoomCharge) {
        this.discountRoomCharge = discountRoomCharge;
    }

    public double getDiscountMaintainCharge() {
        return discountMaintainCharge;
    }

    public void setDiscountMaintainCharge(double discountMaintainCharge) {
        this.discountMaintainCharge = discountMaintainCharge;
    }

    public double getDiscountMoCharge() {
        return discountMoCharge;
    }

    public void setDiscountMoCharge(double discountMoCharge) {
        this.discountMoCharge = discountMoCharge;
    }

    public double getDiscountNursingCharge() {
        return discountNursingCharge;
    }

    public void setDiscountNursingCharge(double discountNursingCharge) {
        this.discountNursingCharge = discountNursingCharge;
    }

    public double getAdjustedRoomCharge() {        
        return adjustedRoomCharge;
    }

    public void setAdjustedRoomCharge(double adjustedRoomCharge) {
        this.adjustedRoomCharge = adjustedRoomCharge;
    }

    public double getAdjustedMaintainCharge() {       
        return adjustedMaintainCharge;
    }

    public void setAdjustedMaintainCharge(double adjustedMaintainCharge) {
        this.adjustedMaintainCharge = adjustedMaintainCharge;
    }

    public double getAdjustedMoCharge() {
      
        return adjustedMoCharge;
    }

    public void setAdjustedMoCharge(double adjustedMoCharge) {
        this.adjustedMoCharge = adjustedMoCharge;
    }

    public double getAjdustedNursingCharge() {        
        return ajdustedNursingCharge;
    }

    public void setAjdustedNursingCharge(double ajdustedNursingCharge) {
        this.ajdustedNursingCharge = ajdustedNursingCharge;
    }

    public double getCurrentMoChargeForAfterDuration() {
        return currentMoChargeForAfterDuration;
    }

    public void setCurrentMoChargeForAfterDuration(double currentMoChargeForAfterDuration) {
        this.currentMoChargeForAfterDuration = currentMoChargeForAfterDuration;
    }
}
