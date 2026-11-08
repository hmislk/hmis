package com.divudi.service.fhir;

import com.divudi.core.data.Title;
import com.divudi.core.entity.Person;
import org.hl7.fhir.r5.model.HumanName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PatientFhirServiceTest {

    private PatientFhirService service;

    @BeforeEach
    public void setUp() {
        service = new PatientFhirService();
    }

    @Test
    public void mapFhirToPerson_withTextOnly_setsPersonName() {
        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();
        HumanName name = fhirPt.addName();
        name.setText("John Doe");

        Person person = new Person();
        service.mapFhirToPerson(fhirPt, person);

        assertEquals("John Doe", person.getName());
        assertNull(person.getFullName());
        assertNull(person.getLastName());
    }

    @Test
    public void mapFhirToPerson_withFamilyAndGiven_synthesizesPersonName() {
        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();
        HumanName name = fhirPt.addName();
        name.setFamily("Doe");
        name.addGiven("John");

        Person person = new Person();
        service.mapFhirToPerson(fhirPt, person);

        assertEquals("John Doe", person.getName());
        assertEquals("John", person.getFullName());
        assertEquals("Doe", person.getLastName());
    }

    @Test
    public void mapFhirToPerson_withMultipleGivenNamesAndFamily_synthesizesCompositeName() {
        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();
        HumanName name = fhirPt.addName();
        name.setFamily("Smith");
        name.addGiven("John");
        name.addGiven("Robert");

        Person person = new Person();
        service.mapFhirToPerson(fhirPt, person);

        assertEquals("John Robert Smith", person.getName());
        assertEquals("John Robert", person.getFullName());
        assertEquals("Smith", person.getLastName());
    }

    @Test
    public void mapFhirToPerson_withFamilyOnly_synthesizesFamilyName() {
        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();
        HumanName name = fhirPt.addName();
        name.setFamily("Perera");

        Person person = new Person();
        service.mapFhirToPerson(fhirPt, person);

        assertEquals("Perera", person.getName());
        assertNull(person.getFullName());
        assertEquals("Perera", person.getLastName());
    }

    @Test
    public void mapFhirToPerson_withGivenOnly_synthesizesGivenName() {
        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();
        HumanName name = fhirPt.addName();
        name.addGiven("Kamal");

        Person person = new Person();
        service.mapFhirToPerson(fhirPt, person);

        assertEquals("Kamal", person.getName());
        assertEquals("Kamal", person.getFullName());
        assertNull(person.getLastName());
    }

    @Test
    public void mapFhirToPerson_withTextAndFamilyGiven_prefersExplicitText() {
        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();
        HumanName name = fhirPt.addName();
        name.setText("Custom Explicit Name");
        name.setFamily("Doe");
        name.addGiven("John");

        Person person = new Person();
        service.mapFhirToPerson(fhirPt, person);

        assertEquals("Custom Explicit Name", person.getName());
        assertEquals("John", person.getFullName());
        assertEquals("Doe", person.getLastName());
    }

    @Test
    public void mapFhirToPerson_withWhitespaceText_fallsBackToSynthesis() {
        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();
        HumanName name = fhirPt.addName();
        name.setText("   ");
        name.setFamily("Doe");
        name.addGiven("John");

        Person person = new Person();
        service.mapFhirToPerson(fhirPt, person);

        assertEquals("John Doe", person.getName());
        assertEquals("John", person.getFullName());
        assertEquals("Doe", person.getLastName());
    }

    @Test
    public void mapFhirToPerson_withPrefix_mapsTitle() {
        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();
        HumanName name = fhirPt.addName();
        name.addPrefix("Mr.");
        name.setFamily("Perera");
        name.addGiven("Sunil");

        Person person = new Person();
        service.mapFhirToPerson(fhirPt, person);

        assertEquals(Title.Mr, person.getTitle());
        assertEquals("Sunil Perera", person.getName());
    }

    @Test
    public void mapFhirToPerson_putUpdate_updatesFamilyAndPreservesExistingGivenName() {
        Person person = new Person();
        person.setName("John Doe");
        person.setFullName("John");
        person.setLastName("Doe");

        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();
        HumanName name = fhirPt.addName();
        name.setFamily("Smith");

        service.mapFhirToPerson(fhirPt, person);

        assertEquals("John Smith", person.getName());
        assertEquals("John", person.getFullName());
        assertEquals("Smith", person.getLastName());
    }

    @Test
    public void mapFhirToPerson_putUpdate_updatesGivenAndPreservesExistingFamilyName() {
        Person person = new Person();
        person.setName("John Doe");
        person.setFullName("John");
        person.setLastName("Doe");

        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();
        HumanName name = fhirPt.addName();
        name.addGiven("Jane");

        service.mapFhirToPerson(fhirPt, person);

        assertEquals("Jane Doe", person.getName());
        assertEquals("Jane", person.getFullName());
        assertEquals("Doe", person.getLastName());
    }

    @Test
    public void mapFhirToPerson_putFamilyOnly_keepsStoredNameWhenGivenUnknown() {
        Person person = new Person();
        person.setName("Kamal Perera");

        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();
        fhirPt.addName().setFamily("Silva");

        service.mapFhirToPerson(fhirPt, person);

        assertEquals("Kamal Perera", person.getName());
        assertEquals("Silva", person.getLastName());
    }

    @Test
    public void mapFhirToPerson_putGivenOnly_keepsStoredNameWhenFamilyUnknown() {
        Person person = new Person();
        person.setName("Kamal Perera");

        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();
        fhirPt.addName().addGiven("Nimal");

        service.mapFhirToPerson(fhirPt, person);

        assertEquals("Kamal Perera", person.getName());
        assertEquals("Nimal", person.getFullName());
    }

    @Test
    public void mapFhirToPerson_emptyNameArrayDoesNotOverwriteExistingName() {
        Person person = new Person();
        person.setName("John Doe");
        person.setFullName("John");
        person.setLastName("Doe");

        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();

        service.mapFhirToPerson(fhirPt, person);

        assertEquals("John Doe", person.getName());
        assertEquals("John", person.getFullName());
        assertEquals("Doe", person.getLastName());
    }

    @Test
    public void toFhirPatient_mapsPersonFieldsToFhirPatient() {
        Person person = new Person();
        person.setName("John Robert Doe");
        person.setFullName("John Robert");
        person.setLastName("Doe");
        person.setTitle(Title.Mr);

        com.divudi.core.entity.Patient patient = new com.divudi.core.entity.Patient();
        patient.setPerson(person);
        patient.setCode("MRN12345");

        org.hl7.fhir.r5.model.Patient fhirPt = service.toFhirPatient(patient);

        assertNotNull(fhirPt);
        assertEquals(1, fhirPt.getName().size());
        HumanName hn = fhirPt.getName().get(0);
        assertEquals("John Robert Doe", hn.getText());
        assertEquals("Doe", hn.getFamily());
        assertEquals(2, hn.getGiven().size());
        assertEquals("John", hn.getGiven().get(0).getValue());
        assertEquals("Robert", hn.getGiven().get(1).getValue());
        assertEquals("Mr.", hn.getPrefix().get(0).getValue());
    }

    @Test
    public void toFhirPatient_fallbackTextWhenPersonNameIsNull() {
        Person person = new Person();
        person.setName(null);
        person.setFullName("Kamal");
        person.setLastName("Perera");

        com.divudi.core.entity.Patient patient = new com.divudi.core.entity.Patient();
        patient.setPerson(person);

        org.hl7.fhir.r5.model.Patient fhirPt = service.toFhirPatient(patient);

        assertNotNull(fhirPt);
        HumanName hn = fhirPt.getName().get(0);
        assertEquals("Kamal Perera", hn.getText());
        assertEquals("Perera", hn.getFamily());
        assertEquals("Kamal", hn.getGiven().get(0).getValue());
    }
}
