package com.divudi.core.data.inward;

public enum InwardChargeType {

    AdmissionFee("Admission Fee", CalculationMethod.ADMISSION_FEE, true),
    AmbulanceCharges("Ambulance Charges", true),
    AdministrationCharge("Administration Fee", CalculationMethod.PATIENT_ROOM, true),//GOES WITH ROOM CHARGE
    BloodTransfusioncharges("Blood Transfusion Charges", true),
    CT("CT Scan", true),
    DressingCharges("Dressing Charges", true),
    Equipment("Equipment Charges", true),
    ECG_EEG("ECG/EEG", true),//"ECG/EEG/ECHO/EXECG"
    ETUCharges("ETU Charges", true),
    GeneralIssuing("General Issuing", CalculationMethod.STORE_BILL, true),
    HomeVisiting("Home Visiting", true),
    Immunization("Immunization", true),
    IntensiveCareManagement("Intensive Care Management", true),
    LarbourRoomCharges("Larbour Room Charges", true),
    LinenCharges("Linen Charges", CalculationMethod.PATIENT_ROOM, true),//GOES WITH PATIENT ROOM
    Laboratory("Laboratory Charges", true),
    MealCharges("Meal Charges", true),
    MedicalCareICU("Medical Care", CalculationMethod.PATIENT_ROOM, true),//Goes With Room
    MedicalServices("Medical Services", true),
    Medicine("Medicine", CalculationMethod.PHARMACY_BILL, true),//For BHT ISSUE
    CancelledReturnedMedicine("Cancelled/Returned Medicine", false),//Value of cancelled/returned medicine issues; NOT shown as its own final-bill row because it is already netted into the Medicine charge type's own total (its bill-type list includes the cancellation/return types) — showing it separately (issue #22674) double-counted it into the bill grand total. Kept allowToSetItems=false rather than deleted so the value/label are still available if a future print breakup needs it without touching bill totals.
    MedicinesAndSurgicalSupplies("Medicines and Surgical Supplies", true),//For Surgery Bill Medicines
    MOCharges("MO Charges", CalculationMethod.PATIENT_ROOM, true),//GOES WITH PATIENT ROOM
    MaintainCharges("Maintain Charges", CalculationMethod.PATIENT_ROOM, true),//GOES WITH PATIENT ROOM
    DoctorAndNurses("Assisting Charge", CalculationMethod.BILL_FEE, true),//Set Doctor && Nurse Fees
    NursingCharges("Nursing Care", CalculationMethod.PATIENT_ROOM, true),//GOES WITH PATIENT ROOM
    OxygenCharges("Oxygen Charges", true),
    OtherCharges("Other Charges", true),
    OperationTheatreCharges("Operation Theatre Charges", true),
    ProfessionalCharge("Professional Charge", CalculationMethod.BILL_FEE, true),//Only for Consultant Fees
    ReimbursementCharges("Reimbursement Charges", true),
    RoomCharges("Room Charges", CalculationMethod.PATIENT_ROOM, true),//GOES WITH PATIENT ROOM
    physiotherapy("Physiotherapy Charges", true),
    Scanning("Scanning Charges", true),
    TreatmentCharges("Treatment Charges", true),
    X_Ray("X-Ray", true),
    WardProcedures("Surgical Procedures", true),
    CardiacMonitoring("Holter Monitoring", true),
    Nebulisation("Nebulisation", true),
    Echo("Echo", true),
    SyringePump("Syringe Pump", true),
    OperationTheatreNursingCharges("Operation Theatre Nursing Charges", true),
    OperationTheatreMachineryCharges("Operation Theatre Machinery Charges", true),
    TheaterConsumbale("Theater Consumables & Drugs", true),
    VAT("VAT (18%)", true),
    AccessoryCharges("Endoscopy Charges", true),
    EyeLence("Opthalmic Technician & Lense Fee", true),
    @Deprecated
    RoomChargesVAT("RoomChargesVAT", true),
    @Deprecated
    ProfessionalChargeVAT("ProfessionalChargeVAT", true),
    @Deprecated
    LensFee("Lense Fee", true),
    @Deprecated
    PackageFee("PackageFee", true),
    HospitalSupportService("Hospital Support Service Charges", true),
    ExtraMedicine("Extra Medicine Charges", true),
    DialysisTreatment("Dialysis Treatment Charges", true),
    Dental("Dental Charges", true),
    Eye("Eye Charges", true),
    @Deprecated
    Investigations("Investigations", true),
    @Deprecated
    MedicalCare("MedicalCare", true),
    @Deprecated
    MedicalCareWard("MedicalCareWard", true),
    BabyCare("BabyCare Charges", true),
    LabourCharges("Labour Charges", true),
    @Deprecated
    ConsultantCharges("ConsultantCharges", true),
    // New enum values
    Andrology("Andrology", true),
    AudiogramTest("Audiogram Test", true),
    CathLabEOMachine("Cath Lab EO Machine", true),
    Channel("Channel", true),
    CSSDCharges("CSSD Charges", true),
    Dialysis("Dialysis", true),
    ECG("ECG", true),
    EEG("EEG", true),
    ExerciseECG("Exercise ECG", true),
    Fertility("Fertility", true),
    HolterMoniteringCharges("Holter Monitering Charges", true),
    LaboratoryInvestigation("Laboratory Investigation", true),
    MedicalService("Medical Service", true),
    MedicalServiceOPD("Medical Service OPD", true),
    MRIUnit("MRI Unit", true),
    OPD("OPD", true),
    Others("Others", true),
    Procedure("Procedure", true),
    Radiology("Radiology", true),
    ReportingCharges("Reporting Charges", true),
    WardProcedure("Ward Procedure", true),
    // Newly added charge categories
    ABIdentification("AB Identification", true),
    AirMattress("Air Mattress", true),
    AmbulatoryBPMonitoring("Ambulatory BP Monitoring", true),
    Audiogram("Audiogram Charges", true),
    BabyWarmerUse("Baby Warmer Charges", true),
    BakriBalloonPPH("Bakri Balloon for PPH", true),
    BloodBankCharges("Blood Bank Charges (DAT)", true),
    BloodDTCharges("Blood DT Charges", true),
    BloodGasProcedure("Blood Gas Procedure", true),
    BloodGrouping("Blood Grouping Charges", true),
    CataractSurgeryInstrumentCharges("Cataract Surgery Instrument Charges", true),
    CatheterRemoval("Catheter Removal Charges", true),
    CBSCharges("CBS Charges", true),
    Colonoscopy("Colonoscopy", true),
    CompositeDentalSplinting("Composite Dental Splinting", true),
    CarotidDoppler("Carotid Doppler Charges", true),
    CriticalCareCharges("Critical Care Charges", true),
    CryoTransfusion("Cryo Transfusion (One Unit)", true),
    CTReportingCharges("CT Reporting Charges", true),
    CTGCharges("CTG Charges", true),
    CystoscopyInstruments("Cystoscopy Instruments", true),
    DCShockProcedure("DC Shock Procedure", true),
    DisposableItems("Disposable Items", true),
    DopplerScan("Doppler Scan", true),
    EarWash("Ear Wash", true),
    EndoscopyCharges("Endoscopy Charges", true),
    EpiduralProcedure("Epidural Procedure", true),
    ETUProcedure("ETU Procedure", true),
    EveningCare("Evening Care", true),
    EyeIrrigation("Eye Irrigation", true),
    FNAC("FNAC Charges", true),
    FoleyInsertion("Foley Insertion Charges", true),
    ForeignBodyRemoval("Foreign Body in Ear/Nose/Throat Removal", true),
    HIOObservation("HIO Observation Charges", true),
    HolterMonitoring("Holter Monitoring", true),
    IDProcedure("I & D Charges", true),
    ICTubeRemoval("IC Tube Removal", true),
    IMInjection("IM Injection", true),
    InfusionPump("Infusion Pump", true),
    InstrumentCharges("Instrument Charges", true),
    IntubationProcedure("Intubation Procedure", true),
    IVCharges("IV Charges (with Cannula)", true),
    KneeAspiration("Knee Aspiration Procedure", true),
    LabourRoomCharges("Labour Room Charges", true),
    Laparoscopy("Laparoscopy", true),
    LargeSplint("Large Splint", true),
    LensCharges("Lens Charges", true),
    LewisPanel("Lewis Panel", true),
    LiquidNitrogen("Liquid Nitrogen", true),
    LumbarPuncture("Lumbar Puncture Procedure", true),
    MRIReporting("MRI Reporting", true),
    MRIScan("MRI Scan", true),
    Nebulization("Nebulization Charges", true),
    NebulizationWithOxygen("Nebulization With Oxygen", true),
    UltrasoundScan("Ultrasound Scan", true),
    VaccineHospitalFee("Vaccine Hospital Fee", true),
    WardCatheterization("Catheterization Charges", true),
    XRayReportingCharges("X-Ray Reporting Charges", true),
    IUCDInsertion("IUCD Insertion", true),
    IUCDInsertionRemoval("IUCD Insertion Removal", true),
    OPDDressingCharges("OPD Dressing Charges", true),
    FFPTransfusion("FFP Transfusion", true),
    KiwiVacuum("Kiwi (Vacuum)", true),
    MediumDressing("Medium Dressing", true),
    MorningCare("Morning Care", true),
    NebulizationInward("Nebulization Inward", true),
    NeckScan("Neck Scan", true),
    NerveConductionTest("Nerve Conduction Test", true),
    NGTubeInsertionProcedure("NG Tube Insertion Procedure", true),
    PEGTubeInsertionProcedure("PEG Tube Insertion Procedure", true),
    PeritonealFluidRemoval("Peritoneal Fluid Removal", true),
    PeritonealTappingDone("Peritoneal Tapping Done", true),
    PhotocromicOptic("Photocromic Optic", true),
    Platelets("Platelets", true),
    PlateletsTransfusion("Platelets Transfusion", true),
    POPBackSlab("POP Back Slab", true),
    POPCutting("POP Cutting", true),
    PPEKitCharges("PPE Kit Charges", true),
    RenalArterialDoppler("Renal Arterial Doppler", true),
    SerumLithiumLevel("Serum Lithium Level", true),
    Sigmoidoscopy("Sigmoidoscopy", true),
    Spectacles("Spectacles", true),
    STDIProcedure("STDI Procedure", true),
    SteamInhalation("Steam Inhalation", true),
    Suction("Suction", true),
    SutureRemovalSmall("Suture Removal (Small)", true),
    SuturingSmallWound("Suturing Small Wound", true),
    TVSScan("TVS Scan", true),
    UrineHCG("Urine HCG", true),
    UrineAlbuminProcedure("Urine Albumin Procedure", true),
    Venesection("Venesection", true),
    WardNGTubeInsertion("Ward NG Tube Insertion", true),
    WoundDressingCharges("Wound Dressing Charges", true),
    WoundToiletDone("Wound Toilet Done", true),
    ExtraLinenSet("Extra Linen Set", true),
    SpeechTherapy("Speech Therapy", true),
    PhototherapyPerHour("Phototherapy/hr", true),
    
    //Depaetment Type Medicine Types
    Opd_Medicine("OPD Medicine", false),
    Etu_Medicine("ETU Medicine", false),
    Pharmacy_Medicine("Pharmacy Medicine", false),
    Inward_Medicine("Inward Medicine", false),
    Theatre_Medicine("Theatre Medicine", false),
    Store_Medicine("Store Medicine", false),
    Inventry_Medicine("Inventry Medicine", false),
    Other_Medicine("Other Medicine", false);
    ;

    private final String nameAsString;
    private String name;
    private CalculationMethod calculationMethod;
    private boolean allowToSetItems;

    InwardChargeType(String nameAsString, boolean allowToSetItems) {
        this.nameAsString = nameAsString;
        this.allowToSetItems = allowToSetItems;
    }

    InwardChargeType(String nameAsString, CalculationMethod calculationMethod, boolean allowToSetItems) {
        this.nameAsString = nameAsString;
        this.calculationMethod = calculationMethod;
        this.allowToSetItems = allowToSetItems;
    }

    public CalculationMethod getCalculationMethod() {
        return calculationMethod != null ? calculationMethod : CalculationMethod.BILL_ITEM;
    }

    @Override
    public String toString() {
        return nameAsString;
    }

    public String getLabel() {
        return toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAllowToSetItems() {
        return allowToSetItems;
    }

    public void setAllowToSetItems(boolean allowToSetItems) {
        this.allowToSetItems = allowToSetItems;
    }
}
