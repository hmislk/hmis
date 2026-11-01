package com.divudi.service.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import java.util.ArrayList;
import java.util.List;

/**
 * Guardian and NIC required-field rules for inward admissions, shared by the
 * places that must enforce the same configuration: the admit page
 * ({@code AdmissionController.errorCheck()}), "Mark as Verified" on a
 * Provisional Emergency admission, and Edit Admission Details
 * ({@code BhtEditController.saveCurrent()}). (Issue #25032)
 *
 * <p>
 * The rules only decide <em>what</em> is missing. The caller decides
 * <em>whether</em> the rules apply (for example, Provisional Emergency
 * admissions are exempt at admit time) and shows the returned messages.
 *
 * <p>
 * Every key read here keeps the default it had in
 * {@code AdmissionController.errorCheck()}; the new keys all default to
 * {@code false} so hospitals that do not set them see no change.
 */
public final class AdmissionGuardianRules {

    private AdmissionGuardianRules() {
    }

    /**
     * Runs the guardian per-field checks (behind {@code Guardian Details
     * Required in Patient Admission}) and then the "patient NIC, else guardian
     * NIC" rule (behind its own key). All problems are collected so the user
     * sees every missing field at once. (Issue #24564)
     *
     * @return the error messages, in display order; empty when nothing is
     * missing
     */
    public static List<String> validate(PatientEncounter admission, ConfigOptionApplicationController cfg) {
        List<String> errors = new ArrayList<>();
        if (admission == null || cfg == null) {
            return errors;
        }
        addGuardianFieldErrors(admission, cfg, errors);
        addPatientOrGuardianNicError(admission, cfg, errors);
        return errors;
    }

    private static void addGuardianFieldErrors(PatientEncounter admission,
            ConfigOptionApplicationController cfg, List<String> errors) {
        if (!cfg.getBooleanValueByKey("Guardian Details Required in Patient Admission")) {
            return;
        }
        Person guardian = admission.getGuardian();
        if (guardian == null) {
            errors.add("Guardian is Required");
            return;
        }
        // Legacy behaviour counts a single space as a value. Treating
        // whitespace-only as missing is a behaviour change, so it is opt-in.
        boolean trim = cfg.getBooleanValueByKey(
                "Inward Admission - Guardian Checks Treat Whitespace-Only Values as Missing", false);

        if (cfg.getBooleanValueByKey("Guardian Title is Required in Patient Admission", true)) {
            if (guardian.getTitle() == null) {
                errors.add("Guardian Title is Required");
            }
        }
        if (cfg.getBooleanValueByKey("Guardian Name is Required in Patient Admission", true)) {
            if (isMissing(guardian.getName(), trim)) {
                errors.add("Guardian Name is Required");
            }
        }
        if (cfg.getBooleanValueByKey("Guardian NIC is Required in Patient Admission", true)) {
            if (isMissing(guardian.getNic(), trim)) {
                errors.add("Guardian NIC is Required");
            }
        }
        if (cfg.getBooleanValueByKey("Guardian Address is Required in Patient Admission", true)) {
            if (isMissing(guardian.getAddress(), trim)) {
                errors.add("Guardian Address is Required");
            }
        }
        if (cfg.getBooleanValueByKey("Guardian Mobile Number is Required in Patient Admission", true)) {
            if (isMissing(guardian.getMobile(), trim)) {
                errors.add("Guardian Mobile Number is Required");
            }
        }
        if (cfg.getBooleanValueByKey("Guardian Home Phone Number is Required in Patient Admission", false)) {
            if (isMissing(guardian.getPhone(), trim)) {
                errors.add("Guardian Home Phone Number is Required");
            }
        }
        if (cfg.getBooleanValueByKey("Guardian Relationship is Required in Patient Admission", true)) {
            if (admission.getGuardianRelationshipToPatient() == null) {
                errors.add("Guardian Relationship is Required");
            }
        }
    }

    /**
     * Blocks the admission when neither the patient nor the guardian has a NIC.
     * Independent of {@code Guardian Details Required in Patient Admission}.
     * Always trims: this rule is new, so there is no legacy behaviour to keep.
     */
    private static void addPatientOrGuardianNicError(PatientEncounter admission,
            ConfigOptionApplicationController cfg, List<String> errors) {
        if (!cfg.getBooleanValueByKey("Inward Admission - Require Patient NIC or Guardian NIC", false)) {
            return;
        }
        Person patientPerson = admission.getPatient() == null ? null : admission.getPatient().getPerson();
        Person guardian = admission.getGuardian();
        boolean hasPatientNic = patientPerson != null && !isMissing(patientPerson.getNic(), true);
        boolean hasGuardianNic = guardian != null && !isMissing(guardian.getNic(), true);
        if (!hasPatientNic && !hasGuardianNic) {
            errors.add("Patient NIC or Guardian NIC is Required. Enter the patient's NIC, "
                    + "or the guardian's NIC if the patient has none.");
        }
    }

    private static boolean isMissing(String value, boolean trim) {
        if (value == null) {
            return true;
        }
        return trim ? value.trim().isEmpty() : value.isEmpty();
    }
}
