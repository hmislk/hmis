/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.inward;

/**
 *
 * @author Buddhika
 */
public enum InwardChargeType {
    AdmissionFee,
    AmbulanceCharges,
    AdministrationCharge,//GOES WITH ROOM CHARGE
    BloodTransfusioncharges,
    CT,
    DressingCharges,
    Equipment,
    ECG_EEG,
    ETUCharges,
    GeneralIssuing,
    HomeVisiting,
    Immunization,
    IntensiveCareManagement,
    LarbourRoomCharges,
    LinenCharges,//GOES WITH PATIENT ROOM
    Laboratory,
    MealCharges,
    MedicalCareICU,//Goes With Room
    MedicalServices,
    Medicine,//For BHT ISSUE
    MOCharges,//GOES WITH PATIENT ROOM
    MaintainCharges,//GOES WITH PATIENT ROOM    
    DoctorAndNurses,//Set Doctor && Nurse Fees
    NursingCharges,//GOES WITH PATIENT ROOM
    OxygenCharges,
    OtherCharges,
    OperationTheatreCharges,
    ProfessionalCharge,//Only for Consultant Fees
    ReimbursementCharges,
    RoomCharges,//GOES WITH PATIENT ROOM
    physiotherapy,
    Scanning,
    TreatmentCharges,
    X_Ray,
    WardProcedures,
    CardiacMonitoring,
    Nebulisation,
    Echo,
    SyringePump,
    ExerciseECG,
    @Deprecated
    Investigations,
    @Deprecated
    MedicalCare,;

    public String getLabel() {
        switch (this) {
            case AdmissionFee:
                return "Admission Fee";
            case RoomCharges:
                return "Room Charges";
            case MOCharges:
                return "MO Charges";
            case MaintainCharges:
                return "Maintain Charges";
            case BloodTransfusioncharges:
                return "Blood Transfusion Charges";
            case LinenCharges:
                return "Linen Charges";
            case NursingCharges:
                return "Nursing Charges";
            case MealCharges:
                return "Meal Charges";
            case OperationTheatreCharges:
                return "Operation Theatre Charges";
            case LarbourRoomCharges:
                return "Larbour Room Charges";
            case ETUCharges:
                return "ETU Charges";
            case MedicalCareICU:
                return "Medical Care ICU";
            case TreatmentCharges:
                return "Treatment Charges";
            case IntensiveCareManagement:
                return "Intensive Care Management";
            case AmbulanceCharges:
                return "Ambulance Charges";
            case HomeVisiting:
                return "Home Visiting";
            case GeneralIssuing:
                return "General Issuing";
            case WardProcedures:
                return "Ward Procedures";
            case ReimbursementCharges:
                return "Reimbursement Charges";
            case OtherCharges:
                return "Other Charges";
            case ProfessionalCharge:
                return "Professional Charge";
            case DressingCharges:
                return "Dressing Charges";
            case OxygenCharges:
                return "Oxygen Charges";
            case Laboratory:
                return "Laboratory";
            case X_Ray:
                return "X-Ray";
            case CT:
                return "CT Scan";
            case Scanning:
                return "Scanning";
            case ECG_EEG:
                return "ECG/EEG";
            case MedicalServices:
                return "Medical Services";
            case DoctorAndNurses:
                return "Assisting Charge";
            case Equipment:
                return "Equipment";
            case AdministrationCharge:
                return "Administration Charge";
            case CardiacMonitoring:
                return "Cardiac Monitoring";
            case Nebulisation:
                return "Nebulisation";
            case Echo:
                return "Echo";
            case SyringePump:
                return "Syringe Pump";
            case ExerciseECG:
                return "Exercise E.C.G";

            default:
                return this.toString();
        }

    }
}
