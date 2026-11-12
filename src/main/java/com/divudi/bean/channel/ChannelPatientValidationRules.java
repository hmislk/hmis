package com.divudi.bean.channel;

import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;

/**
 * Container-free patient validation rules shared by the channel booking by-date
 * and by-month controllers when saving patient details (#25087).
 * Kept separate from JSF context so it can be thoroughly unit tested.
 */
public final class ChannelPatientValidationRules {

    private ChannelPatientValidationRules() {
    }

    /**
     * Validates that a patient has the minimum required fields before saving
     * details in channel booking screens.
     *
     * @param patient the patient to validate, may be null
     * @return the error message to display, or null if validation passes
     */
    public static String validatePatientForSave(Patient patient) {
        if (patient == null) {
            return "Please select a patient";
        }
        Person person = patient.getPerson();
        if (person == null) {
            return "Please enter patient details";
        }
        String name = person.getName();
        if (name == null || name.trim().isEmpty()) {
            return "Please enter a name";
        }
        String phone = person.getPhone();
        String mobile = person.getMobile();
        String transPhone = patient.getPhoneNumberStringTransient();
        String transMobile = patient.getMobileNumberStringTransient();

        boolean hasPhone = (phone != null && !phone.trim().isEmpty())
                || (transPhone != null && !transPhone.trim().isEmpty());
        boolean hasMobile = (mobile != null && !mobile.trim().isEmpty())
                || (transMobile != null && !transMobile.trim().isEmpty());

        if (!hasPhone && !hasMobile) {
            return "Please enter a phone or mobile number";
        }
        return null;
    }
}

