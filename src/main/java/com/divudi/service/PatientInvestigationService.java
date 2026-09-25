package com.divudi.service;

import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.facade.PatientInvestigationFacade;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr M H B Ariyaratne buddhika.ari@gmail.com
 *
 */
@Stateless
public class PatientInvestigationService {

    @EJB
    PatientInvestigationFacade PatientInvestigationFacade;

    public List<PatientInvestigation> fetchPatientInvestigations(
            Date fromDate, Date toDate, SearchKeyword searchKeyword) {
        List<PatientInvestigation> patientInvestigations = new ArrayList<>();

        if (fromDate == null || toDate == null) {
            return patientInvestigations;
        }

        String jpql = "select pi "
                + " from PatientInvestigation pi "
                + " join pi.investigation i "
                + " join pi.billItem.bill b "
                + " join b.patient.person p "
                + " where b.createdAt between :fromDate and :toDate ";

        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        // Only apply searchKeyword filters if searchKeyword is not null
        if (searchKeyword != null) {
            if (searchKeyword.getItemDepartment() != null) {
                jpql += " and pi.investigation.department=:dep ";
                params.put("dep", searchKeyword.getItemDepartment());
            }

            if (searchKeyword.getPatientName() != null && !searchKeyword.getPatientName().trim().isEmpty()) {
                jpql += " and ((p.name) like :patientName)";
                params.put("patientName", "%" + searchKeyword.getPatientName().trim().toUpperCase() + "%");
            }

            if (searchKeyword.getBillNo() != null && !searchKeyword.getBillNo().trim().isEmpty()) {
                jpql += " and ((b.insId) like :billNo)";
                params.put("billNo", "%" + searchKeyword.getBillNo().trim().toUpperCase() + "%");
            }

            if (searchKeyword.getPatientPhone() != null && !searchKeyword.getPatientPhone().trim().isEmpty()) {
                jpql += " and ((p.phone) like :patientPhone)";
                params.put("patientPhone", "%" + searchKeyword.getPatientPhone().trim().toUpperCase() + "%");
            }

            if (searchKeyword.getItemName() != null && !searchKeyword.getItemName().trim().isEmpty()) {
                jpql += " and ((i.name) like :itm)";
                params.put("itm", "%" + searchKeyword.getItemName().trim().toUpperCase() + "%");
            }

            if (searchKeyword.getInvestigation()!= null) {
                jpql += " and i=:ix ";
                params.put("ix", searchKeyword.getInvestigation());
            }

            if (searchKeyword.getPatientEncounter() != null) {
                jpql += " and pi.encounter=:en";
                params.put("en", searchKeyword.getPatientEncounter());
            }

            if (searchKeyword.getPatientInvestigationStatus() != null) {
                jpql += " and pi.status=:pis ";
                params.put("pis", searchKeyword.getPatientInvestigationStatus());
            }
        }

        jpql += " order by pi.id desc";

        patientInvestigations = PatientInvestigationFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP, 50);
        return patientInvestigations;
    }

}
