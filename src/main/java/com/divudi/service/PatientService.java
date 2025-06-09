package com.divudi.service;

import com.divudi.core.entity.Patient;
import com.divudi.core.facade.PatientFacade;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class PatientService {

    @EJB
    private PatientFacade patientFacade;

    // General search method with an optional maximum number of records
    public List<Patient> searchPatient(String searchName, String searchPatientCode, String searchPhone,
            String searchNic, String searchPhn, String searchMrn, int maxResults) {
        boolean atLeastOneCriteriaIsGiven = false;
        StringBuilder j = new StringBuilder();
        Map<String, Object> parameters = new HashMap<>();

        j.append("select p from Patient p where p.retired=false ");

        if (searchName != null && !searchName.trim().isEmpty()) {
            j.append(" and lower(p.person.name) like :name ");
            parameters.put("name", "%" + searchName.toLowerCase() + "%");
            atLeastOneCriteriaIsGiven = true;
        }

        if (searchPatientCode != null && !searchPatientCode.trim().isEmpty()) {
            j.append(" and p.code like :patientCode ");
            parameters.put("patientCode", "%" + searchPatientCode.toLowerCase() + "%");
            atLeastOneCriteriaIsGiven = true;
        }

        if (searchPhone != null && !searchPhone.trim().isEmpty()) {
            j.append(" and (p.person.phone = :phone or p.person.mobile = :phone) ");
            parameters.put("phone", searchPhone);
            atLeastOneCriteriaIsGiven = true;
        }

        if (searchNic != null && !searchNic.trim().isEmpty()) {
            j.append(" and p.person.nic = :nic ");
            parameters.put("nic", searchNic);
            atLeastOneCriteriaIsGiven = true;
        }

        if (searchPhn != null && !searchPhn.trim().isEmpty()) {
            j.append(" and p.phn = :phn ");
            parameters.put("phn", searchPhn);
            atLeastOneCriteriaIsGiven = true;
        }

        if (searchMrn != null && !searchMrn.trim().isEmpty()) {
            j.append(" and p.code = :mrn ");
            parameters.put("mrn", searchMrn);
            atLeastOneCriteriaIsGiven = true;
        }

        // Order by the latest record (assuming p.id represents the latest)
        j.append(" order by p.id desc");

        // Check if any criteria were provided
        if (!atLeastOneCriteriaIsGiven) {
            throw new IllegalArgumentException("At least one search criteria should be provided");
        }

        // Return results, limiting to maxResults if provided
        return patientFacade.findByJpql(j.toString(), parameters, maxResults);
    }

    // Method to search by NIC only
    public List<Patient> searchPatientsByNic(String nic) {
        String j = "select p from Patient p where p.retired = false and p.person.nic = :nic order by p.id desc";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("nic", nic);
        return patientFacade.findByJpql(j, parameters); // Return only one latest record
    }

    // Method to search by phone number only
    public List<Patient> searchPatientsByPhone(String phone) {
        String j = "select p from Patient p where p.retired = false and (p.person.phone = :phone or p.person.mobile = :phone) order by p.id desc";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("phone", phone);
        return patientFacade.findByJpql(j, parameters); // Return only one latest record
    }

    public List<Patient> searchPatientsByPhone(Long phoneNo) {
        String jpql;
        Map params = new HashMap();
        jpql = "select p from Patient p where p.retired = :ret and (p.patientMobileNumber = :pn or p.patientPhoneNumber = :pn)";
        params.put("ret", false);
        params.put("pn", phoneNo);
        List<Patient> patientsSavedWithPhoneNo = patientFacade.findByJpql(jpql, params);
        return patientsSavedWithPhoneNo;
    }

    /**
     * Method to find the first patient whose name contains the search string.
     *
     * @param patients List of patients to search within.
     * @param searchName Name or part of a name to search for.
     * @return The first matching patient, or null if no match is found.
     */
    public Patient findFirstMatchingPatientByName(List<Patient> patients, String searchName) {
        if (patients == null || searchName == null || searchName.trim().isEmpty()) {
            return null; // Return null if no patients or invalid search name
        }

        for (Patient patient : patients) {
            if (patient != null && patient.getPerson() != null && patient.getPerson().getName() != null) {
                String patientName = patient.getPerson().getName().toLowerCase();
                String searchLower = searchName.toLowerCase();
                if (patientName.contains(searchLower)) {
                    return patient; // Return the first matching patient
                }
            }
        }
        return null; // Return null if no match is found
    }

    public Patient reloadPatient(Patient p) {
        if (p == null) {
            return null;
        }
        if (p.getId() == null) {
            patientFacade.create(p);
            return p;
        }
        return patientFacade.findWithoutCache(p.getId());
    }
}
