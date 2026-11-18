/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.clinical;

/**
 *
 * @author buddhika
 */
public enum ClinicalFindingValueType {
    PatientDiagnosis,
    PatientAllergy,
    PatientFoodAllergy,
    PatientPlasterAllergy,
    PatientOtherAllergy,
    PatientMedicine,
    PatientImage,
    PatientDiagnosticImage,
    PatientProcedure,
    VisitDiagnosis,
    VisitMedicine,
    VisitHistory,
    VisitExamination,
    VisitImage,
    VisitInvestigation,
    VisitDiagnosticImage,
    VisitDocument,
    VisitProcedure,
    VisitPrescription,
    VisitDischargeMedicine,
    PlanOfAction,
    VisitInvestigationResult;

    /**
     * All kinds of patient allergy. PatientAllergy is the medicine allergy
     * (kept under its original name so existing rows stay valid).
     */
    public static java.util.List<ClinicalFindingValueType> patientAllergyTypes() {
        return java.util.Arrays.asList(PatientAllergy, PatientFoodAllergy,
                PatientPlasterAllergy, PatientOtherAllergy);
    }

    public boolean isPatientAllergyType() {
        return this == PatientAllergy || this == PatientFoodAllergy
                || this == PatientPlasterAllergy || this == PatientOtherAllergy;
    }
}
