/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.inward;

import com.divudi.core.data.inward.AdmissionTypeEnum;
import com.divudi.core.entity.Category;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;

/**
 *
 * @author www.divudi.com
 */
@Entity
public class AdmissionType extends Category implements Serializable {

    private static final long serialVersionUID = 1L;
    //Admission Fee
    private double admissionFee=0.0;
    private boolean inwardPackage;
    boolean roomChargesAllowed;
    @Enumerated(EnumType.STRING)
    private AdmissionTypeEnum admissionTypeEnum;
    @ManyToOne
    RoomFacilityCharge roomFacilityCharge;
    int additionToCount;
    private boolean generateSeparateAdmissionNumber;
    private boolean allowToCalculateMargin = true;

    public int getAdditionToCount() {
        return additionToCount;
    }

    public void setAdditionToCount(int additionToCount) {
        this.additionToCount = additionToCount;
    }

    public RoomFacilityCharge getRoomFacilityCharge() {
        return roomFacilityCharge;
    }

    public void setRoomFacilityCharge(RoomFacilityCharge roomFacilityCharge) {
        this.roomFacilityCharge = roomFacilityCharge;
    }

    public boolean isRoomChargesAllowed() {
        return roomChargesAllowed;
    }

    public void setRoomChargesAllowed(boolean roomChargesAllowed) {
        this.roomChargesAllowed = roomChargesAllowed;
    }

    public double getAdmissionFee() {
        return admissionFee;
    }

    public void setAdmissionFee(double admissionFee) {
        this.admissionFee = admissionFee;
    }

    public boolean isInwardPackage() {
        return inwardPackage;
    }

    public void setInwardPackage(boolean inwardPackage) {
        this.inwardPackage = inwardPackage;
    }

    public AdmissionTypeEnum getAdmissionTypeEnum() {
        return admissionTypeEnum;
    }

    public void setAdmissionTypeEnum(AdmissionTypeEnum admissionTypeEnum) {
        this.admissionTypeEnum = admissionTypeEnum;
    }

    public boolean isGenerateSeparateAdmissionNumber() {
        return generateSeparateAdmissionNumber;
    }

    public void setGenerateSeparateAdmissionNumber(boolean generateSeparateAdmissionNumber) {
        this.generateSeparateAdmissionNumber = generateSeparateAdmissionNumber;
    }

    public boolean isAllowToCalculateMargin() {
        return allowToCalculateMargin;
    }

    public void setAllowToCalculateMargin(boolean allowToCalculateMargin) {
        this.allowToCalculateMargin = allowToCalculateMargin;
    }
    
}
