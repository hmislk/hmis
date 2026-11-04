package com.divudi.bean.channel;

import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ChannelPatientValidationRulesTest {

    @Test
    @DisplayName("Returns error when patient is null")
    void testValidatePatient_NullPatient() {
        String error = ChannelPatientValidationRules.validatePatientForSave(null);
        assertEquals("Please select a patient", error);
    }

    @Test
    @DisplayName("Returns error when patient has no person")
    void testValidatePatient_NullPerson() {
        Patient patient = new Patient() {
            @Override
            public Person getPerson() {
                return null;
            }
        };
        String error = ChannelPatientValidationRules.validatePatientForSave(patient);
        assertEquals("Please enter patient details", error);
    }

    @Test
    @DisplayName("Returns error when person name is null")
    void testValidatePatient_NullName() {
        Patient patient = new Patient();
        Person person = new Person();
        person.setName(null);
        person.setPhone("0112345678");
        patient.setPerson(person);

        String error = ChannelPatientValidationRules.validatePatientForSave(patient);
        assertEquals("Please enter a name", error);
    }

    @Test
    @DisplayName("Returns error when person name is empty or spaces")
    void testValidatePatient_EmptyName() {
        Patient patient = new Patient();
        Person person = new Person();
        person.setName("   ");
        person.setPhone("0112345678");
        patient.setPerson(person);

        String error = ChannelPatientValidationRules.validatePatientForSave(patient);
        assertEquals("Please enter a name", error);
    }

    @Test
    @DisplayName("Returns error when neither phone nor mobile is provided")
    void testValidatePatient_NoContactNumber() {
        Patient patient = new Patient();
        Person person = new Person();
        person.setName("John Doe");
        person.setPhone(null);
        person.setMobile(null);
        patient.setPerson(person);

        String error = ChannelPatientValidationRules.validatePatientForSave(patient);
        assertEquals("Please enter a phone or mobile number", error);
    }

    @Test
    @DisplayName("Returns error when phone and mobile are blank spaces")
    void testValidatePatient_BlankContactNumber() {
        Patient patient = new Patient();
        Person person = new Person();
        person.setName("John Doe");
        person.setPhone("  ");
        person.setMobile("");
        patient.setPerson(person);

        String error = ChannelPatientValidationRules.validatePatientForSave(patient);
        assertEquals("Please enter a phone or mobile number", error);
    }

    @Test
    @DisplayName("Passes validation when patient has valid name and phone")
    void testValidatePatient_ValidWithPhone() {
        Patient patient = new Patient();
        Person person = new Person();
        person.setName("Jane Doe");
        person.setPhone("0112345678");
        patient.setPerson(person);

        String error = ChannelPatientValidationRules.validatePatientForSave(patient);
        assertNull(error);
    }

    @Test
    @DisplayName("Passes validation when patient has valid name and mobile")
    void testValidatePatient_ValidWithMobile() {
        Patient patient = new Patient();
        Person person = new Person();
        person.setName("Jane Doe");
        person.setMobile("0771234567");
        patient.setPerson(person);

        String error = ChannelPatientValidationRules.validatePatientForSave(patient);
        assertNull(error);
    }

    @Test
    @DisplayName("Passes validation when patient has transient phone number")
    void testValidatePatient_ValidWithTransientPhone() {
        Patient patient = new Patient();
        Person person = new Person();
        person.setName("Kasun Perera");
        patient.setPerson(person);
        patient.setPhoneNumberStringTransient("0112345678");

        String error = ChannelPatientValidationRules.validatePatientForSave(patient);
        assertNull(error);
    }

    @Test
    @DisplayName("Passes validation when patient has transient mobile number")
    void testValidatePatient_ValidWithTransientMobile() {
        Patient patient = new Patient();
        Person person = new Person();
        person.setName("Kasun Perera");
        patient.setPerson(person);
        patient.setMobileNumberStringTransient("0771234567");

        String error = ChannelPatientValidationRules.validatePatientForSave(patient);
        assertNull(error);
    }

    @Test
    @DisplayName("Passes validation when patient has both phone and mobile")
    void testValidatePatient_ValidWithBoth() {
        Patient patient = new Patient();
        Person person = new Person();
        person.setName("Dr. Nimal Silva");
        person.setPhone("0112345678");
        person.setMobile("0771234567");
        patient.setPerson(person);

        String error = ChannelPatientValidationRules.validatePatientForSave(patient);
        assertNull(error);
    }
}
