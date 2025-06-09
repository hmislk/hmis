/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.inward;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;

/**
 *
 * @author Buddhika
 */
@Entity
public class RoomFacilityCharge implements Serializable {

    @OneToMany(mappedBy = "roomFacilityCharge")
    private List<PatientRoom> patientRooms;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Double maintananceCharge = 0.0;
    private Double roomCharge = 0.0;
    private Double linenCharge = 0.0;
    private Double nursingCharge = 0.0;
    private Double moCharge = 0.0;
    private Double moChargeForAfterDuration = 0.0;
    private double adminstrationCharge;
    private double medicalCareCharge;
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
    boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;
    @ManyToOne
    private Room room;
    @ManyToOne
    private PaymentScheme paymentScheme;
    @ManyToOne
    private Institution company;
    @ManyToOne
    private Department department;
    @ManyToOne
    private TimedItemFee timedItemFee;
    @ManyToOne
    private RoomCategory roomCategory;

    public RoomCategory getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(RoomCategory roomCategory) {
        this.roomCategory = roomCategory;
    }

    public List<PatientRoom> getPatientRooms() {
        return patientRooms;
    }

    public void setPatientRooms(List<PatientRoom> patientRooms) {
        this.patientRooms = patientRooms;
    }

    public double getAdminstrationCharge() {
        return adminstrationCharge;
    }

    public void setAdminstrationCharge(double adminstrationCharge) {
        this.adminstrationCharge = adminstrationCharge;
    }

    public double getMedicalCareCharge() {
        return medicalCareCharge;
    }

    public void setMedicalCareCharge(double medicalCareCharge) {
        this.medicalCareCharge = medicalCareCharge;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public Institution getCompany() {
        return company;
    }

    public void setCompany(Institution company) {
        this.company = company;
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

        if (!(object instanceof RoomFacilityCharge)) {
            return false;
        }
        RoomFacilityCharge other = (RoomFacilityCharge) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.inward.RoomFacility[ id=" + id + " ]";
    }

    public TimedItemFee getTimedItemFee() {
        return timedItemFee;
    }

    public void setTimedItemFee(TimedItemFee timedItemFee) {
        this.timedItemFee = timedItemFee;
    }

    public Double getMaintananceCharge() {
        return maintananceCharge;
    }

    public void setMaintananceCharge(Double maintananceCharge) {
        this.maintananceCharge = maintananceCharge;
    }

    public Double getLinenCharge() {
        return linenCharge;
    }

    public void setLinenCharge(Double linenCharge) {
        this.linenCharge = linenCharge;
    }

    public Double getNursingCharge() {
        return nursingCharge;
    }

    public void setNursingCharge(Double nursingCharge) {
        this.nursingCharge = nursingCharge;
    }

    public Double getMoCharge() {
        return moCharge;
    }

    public void setMoCharge(Double moCharge) {
        this.moCharge = moCharge;
    }

    public Double getRoomCharge() {
        return roomCharge;
    }

    public void setRoomCharge(Double roomCharge) {
        this.roomCharge = roomCharge;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Double getMoChargeForAfterDuration() {
        return moChargeForAfterDuration;
    }

    public void setMoChargeForAfterDuration(Double moChargeForAfterDuration) {
        this.moChargeForAfterDuration = moChargeForAfterDuration;
    }
}
