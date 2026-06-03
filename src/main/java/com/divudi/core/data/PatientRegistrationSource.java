package com.divudi.core.data;

/**
 * Records how a Patient record was first entered into the system.
 *
 * <p>This is the identity-trust anchor for a patient: walk-in registrations are
 * identity-verified by staff in person, whereas kiosk/online registrations are
 * unverified. The value is set <b>once</b> at the moment the Patient record is
 * created and is never changed afterwards.</p>
 *
 * <p>Existing patients created before this feature carry a {@code null} value,
 * which is interpreted as "unrecorded / pre-feature".</p>
 *
 * Issue: hmislk/hmis#21181
 *
 * @author Dr M H B Ariyaratne
 */
public enum PatientRegistrationSource {

    WALK_IN("Walk-in"),                       // registered by counter/cashier staff during OPD or pharmacy visit
    INWARD_ADMISSION("Inward Admission"),     // new patient registered at time of inpatient admission
    NEWBORN("Newborn"),                       // baby registered via Baby Admission from mother's admission profile
    CALL_CENTRE("Call Centre"),               // registered by staff over the phone
    KIOSK("Kiosk"),                           // patient self-registered at a physical kiosk terminal
    ONLINE_SELF("Online / Self"),             // patient portal / mobile app (deprecated — legacy data migration only)
    THIRD_PARTY_AGENT("Third Party Agent"),   // external referral agent / partner submitted registration
    ON_ADMISSION_DEATH("On Admission Death"); // patient found dead, registered posthumously at time of admission

    private final String label;

    PatientRegistrationSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
