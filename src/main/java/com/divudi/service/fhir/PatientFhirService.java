/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.service.PatientService;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import org.hl7.fhir.r5.model.Bundle;
import org.hl7.fhir.r5.model.ContactPoint;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.HumanName;
import org.hl7.fhir.r5.model.Identifier;

/**
 * FHIR R5 Patient service — maps between HMIS Patient entities and FHIR R5 Patient resources.
 */
@Stateless
public class PatientFhirService {

    public static final FhirContext FHIR_CTX = FhirContext.forR5();

    @EJB
    private PatientFacade patientFacade;

    @EJB
    private PersonFacade personFacade;

    @EJB
    private PatientService patientService;

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public Patient getPatientById(Long id) {
        if (id == null) {
            return null;
        }
        return patientFacade.find(id);
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    public List<Patient> searchPatients(String name, String phone, String identifier, int maxResults) {
        // If name or phone only — single call suffices
        if (identifier == null || identifier.trim().isEmpty()) {
            return patientService.searchPatient(name, null, phone, null, null, null, maxResults);
        }

        // Determine which identifier field(s) to search
        // System-prefixed format: "urn:lk:nic|value" routes to one field (AND is safe)
        if (identifier.contains("|")) {
            String[] parts = identifier.split("\\|", 2);
            String system = parts[0];
            String value = parts[1];
            String nic = null, phn = null, mrn = null;
            if (system.contains("nic")) {
                nic = value;
            } else if (system.contains("phn")) {
                phn = value;
            } else if (system.contains("mrn")) {
                mrn = value;
            } else {
                nic = value; // unknown system — treat as NIC
            }
            return patientService.searchPatient(name, mrn, phone, nic, phn, null, maxResults);
        }

        // Plain value — must search NIC, PHN, MRN separately (OR semantics)
        // PatientService.searchPatient uses AND, so we run three searches and deduplicate by id
        String value = identifier.trim();
        Map<Long, Patient> seen = new LinkedHashMap<>();
        for (Patient p : patientService.searchPatient(name, null, phone, value, null, null, maxResults)) {
            if (p.getId() != null) seen.put(p.getId(), p);
        }
        for (Patient p : patientService.searchPatient(name, null, phone, null, value, null, maxResults)) {
            if (p.getId() != null) seen.putIfAbsent(p.getId(), p);
        }
        for (Patient p : patientService.searchPatient(name, value, phone, null, null, null, maxResults)) {
            if (p.getId() != null) seen.putIfAbsent(p.getId(), p);
        }
        List<Patient> results = new ArrayList<>(seen.values());
        return results.size() > maxResults ? results.subList(0, maxResults) : results;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    public Patient createPatient(org.hl7.fhir.r5.model.Patient fhirPt, WebUser creator) {
        Person person = new Person();
        mapFhirToPerson(fhirPt, person);
        person.setCreatedAt(new Date());
        person.setCreater(creator);
        personFacade.create(person);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setCreatedAt(new Date());
        patient.setCreater(creator);
        if (creator != null) {
            patient.setCreatedInstitution(creator.getInstitution());
        }

        // MRN from identifier or auto-generate
        String mrn = extractIdentifierValue(fhirPt, "urn:hmis:mrn");
        if (mrn == null || mrn.trim().isEmpty()) {
            mrn = generatePatientCode();
        }
        patient.setCode(mrn);

        // PHN
        String phn = extractIdentifierValue(fhirPt, "urn:lk:phn");
        if (phn != null && !phn.trim().isEmpty()) {
            patient.setPhn(phn);
        }

        patientFacade.createAndFlush(patient);
        return patient;
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    public Patient updatePatient(Long id, org.hl7.fhir.r5.model.Patient fhirPt, WebUser editor) {
        Patient patient = patientFacade.find(id);
        if (patient == null) {
            return null;
        }

        Person person = patient.getPerson();
        if (person == null) {
            person = new Person();
            patient.setPerson(person);
        }

        mapFhirToPerson(fhirPt, person);
//        person.setEditer(editor);
//        person.setEditedAt(new Date());
        personFacade.edit(person);

        // PHN update
        String phn = extractIdentifierValue(fhirPt, "urn:lk:phn");
        if (phn != null && !phn.trim().isEmpty()) {
            patient.setPhn(phn);
        }

        patient.setEditer(editor);
        patient.setEditedAt(new Date());
        patientFacade.editAndFlush(patient);
        return patient;
    }

    // -------------------------------------------------------------------------
    // Mapping: HMIS → FHIR
    // -------------------------------------------------------------------------

    public org.hl7.fhir.r5.model.Patient toFhirPatient(Patient p) {
        org.hl7.fhir.r5.model.Patient fhirPt = new org.hl7.fhir.r5.model.Patient();

        fhirPt.setId(p.getId() != null ? p.getId().toString() : null);
        fhirPt.setActive(!p.isRetired());

        // Identifiers
        if (p.getCode() != null && !p.getCode().trim().isEmpty()) {
            Identifier mrnId = fhirPt.addIdentifier();
            mrnId.setSystem("urn:hmis:mrn");
            mrnId.setValue(p.getCode());
        }
        if (p.getPhn() != null && !p.getPhn().trim().isEmpty()) {
            Identifier phnId = fhirPt.addIdentifier();
            phnId.setSystem("urn:lk:phn");
            phnId.setValue(p.getPhn());
        }

        Person person = p.getPerson();
        if (person == null) {
            return fhirPt;
        }

        // NIC
        if (person.getNic() != null && !person.getNic().trim().isEmpty()) {
            Identifier nicId = fhirPt.addIdentifier();
            nicId.setSystem("urn:lk:nic");
            nicId.setValue(person.getNic());
        }

        // Name
        HumanName name = fhirPt.addName();
        name.setUse(HumanName.NameUse.OFFICIAL);
        if (person.getName() != null) {
            name.setText(person.getName());
        }
        if (person.getFullName() != null) {
            name.addGiven(person.getFullName());
        }
        if (person.getLastName() != null) {
            name.setFamily(person.getLastName());
        }
        if (person.getTitle() != null) {
            name.addPrefix(person.getTitle().getLabel().trim());
        }

        // Birth date
        if (person.getDob() != null) {
            fhirPt.setBirthDate(person.getDob());
        }

        // Gender
        if (person.getSex() != null) {
            fhirPt.setGender(toFhirGender(person.getSex()));
        }

        // Telecom
        if (person.getPhone() != null && !person.getPhone().trim().isEmpty()) {
            ContactPoint phone = fhirPt.addTelecom();
            phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
            phone.setUse(ContactPoint.ContactPointUse.HOME);
            phone.setValue(person.getPhone());
        }
        if (person.getMobile() != null && !person.getMobile().trim().isEmpty()) {
            ContactPoint mobile = fhirPt.addTelecom();
            mobile.setSystem(ContactPoint.ContactPointSystem.PHONE);
            mobile.setUse(ContactPoint.ContactPointUse.MOBILE);
            mobile.setValue(person.getMobile());
        }
        if (person.getEmail() != null && !person.getEmail().trim().isEmpty()) {
            ContactPoint email = fhirPt.addTelecom();
            email.setSystem(ContactPoint.ContactPointSystem.EMAIL);
            email.setUse(ContactPoint.ContactPointUse.HOME);
            email.setValue(person.getEmail());
        }

        // Address
        if (person.getAddress() != null && !person.getAddress().trim().isEmpty()) {
            org.hl7.fhir.r5.model.Address addr = fhirPt.addAddress();
            addr.setText(person.getAddress());
        }

        return fhirPt;
    }

    public Bundle toSearchBundle(List<Patient> patients) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(patients.size());
        for (Patient p : patients) {
            bundle.addEntry().setResource(toFhirPatient(p));
        }
        return bundle;
    }

    // -------------------------------------------------------------------------
    // Mapping: FHIR → HMIS (partial — only non-null fields)
    // -------------------------------------------------------------------------

    private void mapFhirToPerson(org.hl7.fhir.r5.model.Patient fhirPt, Person person) {
        // Name
        if (!fhirPt.getName().isEmpty()) {
            HumanName name = fhirPt.getName().get(0);
            if (name.hasText()) {
                person.setName(name.getText());
            }
            if (name.hasFamily()) {
                person.setLastName(name.getFamily());
            }
            if (!name.getGiven().isEmpty()) {
                person.setFullName(name.getGiven().get(0).getValue());
            }
            if (!name.getPrefix().isEmpty()) {
                String prefix = name.getPrefix().get(0).getValue();
                person.setTitle(Title.getTitleEnum(prefix));
            }
        }

        // Birth date
        if (fhirPt.getBirthDate() != null) {
            person.setDob(fhirPt.getBirthDate());
        }

        // Gender
        if (fhirPt.getGender() != null) {
            person.setSex(fromFhirGender(fhirPt.getGender()));
        }

        // Telecom
        for (ContactPoint cp : fhirPt.getTelecom()) {
            if (ContactPoint.ContactPointSystem.PHONE.equals(cp.getSystem())) {
                if (ContactPoint.ContactPointUse.MOBILE.equals(cp.getUse())) {
                    if (cp.getValue() != null && !cp.getValue().trim().isEmpty()) {
                        person.setMobile(cp.getValue().trim());
                    }
                } else {
                    if (cp.getValue() != null && !cp.getValue().trim().isEmpty()) {
                        person.setPhone(cp.getValue().trim());
                    }
                }
            } else if (ContactPoint.ContactPointSystem.EMAIL.equals(cp.getSystem())) {
                if (cp.getValue() != null && !cp.getValue().trim().isEmpty()) {
                    person.setEmail(cp.getValue().trim());
                }
            }
        }

        // Address
        if (!fhirPt.getAddress().isEmpty()) {
            org.hl7.fhir.r5.model.Address addr = fhirPt.getAddress().get(0);
            if (addr.hasText()) {
                person.setAddress(addr.getText());
            }
        }

        // NIC from identifier
        String nic = extractIdentifierValue(fhirPt, "urn:lk:nic");
        if (nic != null && !nic.trim().isEmpty()) {
            person.setNic(nic);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    public String generatePatientCode() {
        long count = patientFacade.countByJpql("select count(p) FROM Patient p where p.code is not null");
        return String.valueOf(count + 1);
    }

    private String extractIdentifierValue(org.hl7.fhir.r5.model.Patient fhirPt, String system) {
        for (Identifier id : fhirPt.getIdentifier()) {
            if (system.equals(id.getSystem()) && id.getValue() != null) {
                return id.getValue();
            }
        }
        return null;
    }

    private Enumerations.AdministrativeGender toFhirGender(Sex sex) {
        if (sex == null) {
            return Enumerations.AdministrativeGender.UNKNOWN;
        }
        switch (sex) {
            case Male:
                return Enumerations.AdministrativeGender.MALE;
            case Female:
                return Enumerations.AdministrativeGender.FEMALE;
            case Other:
                return Enumerations.AdministrativeGender.OTHER;
            default:
                return Enumerations.AdministrativeGender.UNKNOWN;
        }
    }

    private Sex fromFhirGender(Enumerations.AdministrativeGender gender) {
        if (gender == null) {
            return Sex.Unknown;
        }
        switch (gender) {
            case MALE:
                return Sex.Male;
            case FEMALE:
                return Sex.Female;
            case OTHER:
                return Sex.Other;
            default:
                return Sex.Unknown;
        }
    }
}
