package com.divudi.core.data.inward;

public enum InwardChargeType {

    AdmissionFee("Admission Fee"),
    AmbulanceCharges("Ambulance Charges"),
    AdministrationCharge("Administration Fee"),//GOES WITH ROOM CHARGE
    BloodTransfusioncharges("Blood Transfusion Charges"),
    CT("CT Scan"),
    DressingCharges("Dressing Charges"),
    Equipment("Equipment Charges"),
    ECG_EEG("ECG/EEG"),//"ECG/EEG/ECHO/EXECG"
    ETUCharges("ETU Charges"),
    GeneralIssuing("General Issuing"),
    HomeVisiting("Home Visiting"),
    Immunization("Immunization"),
    IntensiveCareManagement("Intensive Care Management"),
    LarbourRoomCharges("Larbour Room Charges"),
    LinenCharges("Linen Charges"),//GOES WITH PATIENT ROOM
    Laboratory("Laboratory Charges"),
    MealCharges("Meal Charges"),
    MedicalCareICU("Medical Care"),//Goes With Room
    MedicalServices("Medical Services"),
    Medicine("Medicine"),//For BHT ISSUE
    MOCharges("MO Charges"),//GOES WITH PATIENT ROOM
    MaintainCharges("Maintain Charges"),//GOES WITH PATIENT ROOM
    DoctorAndNurses("Assisting Charge"),//Set Doctor && Nurse Fees
    NursingCharges("Nursing Care"),//GOES WITH PATIENT ROOM
    OxygenCharges("Oxygen Charges"),
    OtherCharges("Other Charges"),
    OperationTheatreCharges("Operation Theatre Charges"),
    ProfessionalCharge("Professional Charge"),//Only for Consultant Fees
    ReimbursementCharges("Reimbursement Charges"),
    RoomCharges("Room Charges"),//GOES WITH PATIENT ROOM
    physiotherapy("Physiotherapy Charges"),
    Scanning("Scanning Charges"),
    TreatmentCharges("Treatment Charges"),
    X_Ray("X-Ray"),
    WardProcedures("Surgical Procedures"),
    CardiacMonitoring("Holter Monitoring"),
    Nebulisation("Nebulisation"),
    Echo("Echo"),
    SyringePump("Syringe Pump"),
    OperationTheatreNursingCharges("Operation Theatre Nursing Charges"),
    OperationTheatreMachineryCharges("Operation Theatre Machinery Charges"),
    TheaterConsumbale("Theater Consumables & Drugs"),
    VAT("VAT (18%)"),
    AccessoryCharges("Endoscopy Charges"),
    EyeLence("Opthalmic Technician & Lense Fee"),
    HospitalSupportService("Hospital Support Service Charges"),
    ExtraMedicine("Extra Medicine Charges"),
    DialysisTreatment("Dialysis Treatment Charges"),
    Dental("Dental Charges"),
    Eye("Eye Charges"),
    @Deprecated
    Investigations("Investigations"),
    @Deprecated
    MedicalCare("MedicalCare"),
    BabyCare("BabyCare Charges"),
    LabourCharges("Labour Charges"),
    @Deprecated
    ConsultantCharges("ConsultantCharges"),
    // New enum values
    Andrology("Andrology"),
    AudiogramTest("Audiogram Test"),
    CathLabEOMachine("Cath Lab EO Machine"),
    Channel("Channel"),
    CSSDCharges("CSSD Charges"),
    Dialysis("Dialysis"),
    ECG("ECG"),
    EEG("EEG"),
    ExerciseECG("Exercise ECG"),
    Fertility("Fertility"),
    HolterMoniteringCharges("Holter Monitering Charges"),
    LaboratoryInvestigation("Laboratory Investigation"),
    MedicalService("Medical Service"),
    MedicalServiceOPD("Medical Service OPD"),
    MRIUnit("MRI Unit"),
    OPD("OPD"),
    Others("Others"),
    Procedure("Procedure"),
    Radiology("Radiology"),
    ReportingCharges("Reporting Charges"),
    WardProcedure("Ward Procedure"),
    // Newly added charge categories
    ABIdentification("AB Identification"),
    AirMattress("Air Mattress"),
    AmbulatoryBPMonitoring("Ambulatory BP Monitoring"),
    Audiogram("Audiogram Charges"),
    BabyWarmerUse("Baby Warmer Charges"),
    BakriBalloonPPH("Bakri Balloon for PPH"),
    BloodBankCharges("Blood Bank Charges (DAT)"),
    BloodDTCharges("Blood DT Charges"),
    BloodGasProcedure("Blood Gas Procedure"),
    BloodGrouping("Blood Grouping Charges"),
    CataractSurgeryInstrumentCharges("Cataract Surgery Instrument Charges"),
    CatheterRemoval("Catheter Removal Charges"),
    CBSCharges("CBS Charges"),
    Colonoscopy("Colonoscopy"),
    CompositeDentalSplinting("Composite Dental Splinting"),
    CarotidDoppler("Carotid Doppler Charges"),
    CriticalCareCharges("Critical Care Charges"),
    CryoTransfusion("Cryo Transfusion (One Unit)"),
    CTReportingCharges("CT Reporting Charges"),
    CTGCharges("CTG Charges"),
    CystoscopyInstruments("Cystoscopy Instruments"),
    DCShockProcedure("DC Shock Procedure"),
    DisposableItems("Disposable Items"),
    DopplerScan("Doppler Scan"),
    EarWash("Ear Wash"),
    EndoscopyCharges("Endoscopy Charges"),
    EpiduralProcedure("Epidural Procedure"),
    ETUProcedure("ETU Procedure"),
    EveningCare("Evening Care"),
    EyeIrrigation("Eye Irrigation"),
    FNAC("FNAC Charges"),
    FoleyInsertion("Foley Insertion Charges"),
    ForeignBodyRemoval("Foreign Body in Ear/Nose/Throat Removal"),
    HIOObservation("HIO Observation Charges"),
    HolterMonitoring("Holter Monitoring"),
    IDProcedure("I & D Charges"),
    ICTubeRemoval("IC Tube Removal"),
    IMInjection("IM Injection"),
    InfusionPump("Infusion Pump"),
    InstrumentCharges("Instrument Charges"),
    IntubationProcedure("Intubation Procedure"),
    IVCharges("IV Charges (with Cannula)"),
    KneeAspiration("Knee Aspiration Procedure"),
    LabourRoomCharges("Labour Room Charges"),
    Laparoscopy("Laparoscopy"),
    LargeSplint("Large Splint"),
    LensCharges("Lens Charges"),
    LewisPanel("Lewis Panel"),
    LiquidNitrogen("Liquid Nitrogen"),
    LumbarPuncture("Lumbar Puncture Procedure"),
    MRIReporting("MRI Reporting"),
    MRIScan("MRI Scan"),
    Nebulization("Nebulization Charges"),
    NebulizationWithOxygen("Nebulization With Oxygen"),
    UltrasoundScan("Ultrasound Scan"),
    VaccineHospitalFee("Vaccine Hospital Fee"),
    WardCatheterization("Catheterization Charges"),
    XRayReportingCharges("X-Ray Reporting Charges"),
    IUCDInsertion("IUCD Insertion"),
    IUCDInsertionRemoval("IUCD Insertion Removal"),
    OPDDressingCharges("OPD Dressing Charges"),
    FFPTransfusion("FFP Transfusion"),
    KiwiVacuum("Kiwi (Vacuum)"),
    MediumDressing("Medium Dressing"),
    MorningCare("Morning Care"),
    NebulizationInward("Nebulization Inward"),
    NeckScan("Neck Scan"),
    NerveConductionTest("Nerve Conduction Test"),
    NGTubeInsertionProcedure("NG Tube Insertion Procedure"),
    PEGTubeInsertionProcedure("PEG Tube Insertion Procedure"),
    PeritonealFluidRemoval("Peritoneal Fluid Removal"),
    PeritonealTappingDone("Peritoneal Tapping Done"),
    PhotocromicOptic("Photocromic Optic"),
    Platelets("Platelets"),
    PlateletsTransfusion("Platelets Transfusion"),
    POPBackSlab("POP Back Slab"),
    POPCutting("POP Cutting"),
    PPEKitCharges("PPE Kit Charges"),
    RenalArterialDoppler("Renal Arterial Doppler"),
    SerumLithiumLevel("Serum Lithium Level"),
    Sigmoidoscopy("Sigmoidoscopy"),
    Spectacles("Spectacles"),
    STDIProcedure("STDI Procedure"),
    SteamInhalation("Steam Inhalation"),
    Suction("Suction"),
    SutureRemovalSmall("Suture Removal (Small)"),
    SuturingSmallWound("Suturing Small Wound"),
    TVSScan("TVS Scan"),
    UrineHCG("Urine HCG"),
    UrineAlbuminProcedure("Urine Albumin Procedure"),
    Venesection("Venesection"),
    WardNGTubeInsertion("Ward NG Tube Insertion"),
    WoundDressingCharges("Wound Dressing Charges"),
    WoundToiletDone("Wound Toilet Done"),
    ExtraLinenSet("Extra Linen Set"),
    SpeechTherapy("Speech Therapy"),
    PhototherapyPerHour("Phototherapy/hr");

    private final String nameAsString;
    private String name;

    InwardChargeType(String nameAsString) {
        this.nameAsString = nameAsString;
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
}
