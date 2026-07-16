package com.divudi.bean.lab;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
import com.divudi.core.data.InvestigationItemType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.PatientReportItemValue;
import com.divudi.core.facade.PatientReportItemValueFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.Item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Rashmika
 */

@Named(value = "organismAntibioticSensitivityReport")
@SessionScoped
public class OrganismAntibioticSensitivityReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PatientReportItemValueFacade patientReportItemValueFacade;

    private Date fromDate;
    private Date toDate;

    private Institution institution;
    private Institution site;
    private Department department;
    private Patient patient;
    private Investigation investigation;
    private Doctor referringDoctor;

    private String visitType;
    private Item organism;
    private Item antibiotic;
    private String sensitivity;

    private List<PatientReportItemValue> reportData;

    public OrganismAntibioticSensitivityReport() {
    }

    public void process() {
        reportData = new ArrayList<>();

        if (getFromDate() == null || getToDate() == null) {
            JsfUtil.addErrorMessage("Please select the from date and to date.");
            return;
        }

        if (getFromDate().after(getToDate())) {
            JsfUtil.addErrorMessage(
                    "The from date cannot be later than the to date."
            );
            return;
        }

        StringBuilder jpql = new StringBuilder();

        jpql.append("select priv ")
                .append("from PatientReportItemValue priv ")
                .append("join priv.patientReport pr ")
                .append("join pr.patientInvestigation pi ")
                .append("join pi.billItem bi ")
                .append("join bi.bill b ")
                .append("where priv.retired = false ")
                .append("and pr.retired = false ")
                .append("and pi.retired = false ")
                .append("and bi.retired = false ")
                .append("and b.retired = false ")
                .append("and b.createdAt between :fromDate and :toDate ")
                .append("and priv.investigationItem.ixItemType = :itemType ");

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("fromDate", getFromDate());
        parameters.put("toDate", getToDate());
        parameters.put("itemType", InvestigationItemType.Antibiotic);

        if (institution != null) {
            jpql.append("and b.department.institution = :institution ");
            parameters.put("institution", institution);
        }

        if (site != null) {
            jpql.append("and b.department.site = :site ");
            parameters.put("site", site);
        }

        if (department != null) {
            jpql.append("and b.department = :department ");
            parameters.put("department", department);
        }

        if (patient != null
                && patient.getPhn() != null
                && !patient.getPhn().trim().isEmpty()) {

            jpql.append("and upper(b.patient.phn) like :phn ");

            parameters.put(
                    "phn",
                    "%" + patient.getPhn().trim().toUpperCase() + "%"
            );
        }

        if (investigation != null) {
            jpql.append("and pi.investigation = :investigation ");
            parameters.put("investigation", investigation);
        }

        if (antibiotic != null) {
            jpql.append("and priv.investigationItem = :antibiotic ");
            parameters.put("antibiotic", antibiotic);
        }

        if (visitType != null
                && !visitType.trim().isEmpty()
                && !"All".equalsIgnoreCase(visitType)) {

            jpql.append("and b.ipOpOrCc = :visitType ");
            parameters.put("visitType", visitType);
        }
        
        if (visitType != null
                && !visitType.trim().isEmpty()
                && !"All".equalsIgnoreCase(visitType)) {

            jpql.append("and b.ipOpOrCc = :visitType ");
            parameters.put("visitType", visitType);
        }

        if (organism != null) {
            jpql.append("and pi.organism = :organism ");
            parameters.put("organism", organism);
        }

        if (sensitivity != null && !sensitivity.trim().isEmpty()) {
            jpql.append("and priv.value = :sensitivity ");
            parameters.put("sensitivity", sensitivity);
        }

        if (referringDoctor != null) {
            jpql.append("and b.referredBy = :referringDoctor ");
            parameters.put("referringDoctor", referringDoctor);
        }

        jpql.append("order by pi.id desc");

        jpql.append("order by pi.id desc");

        reportData = patientReportItemValueFacade.findByJpql(
                jpql.toString(),
                parameters,
                TemporalType.TIMESTAMP
        );

        if (reportData == null) {
            reportData = new ArrayList<>();
        }

        if (reportData.isEmpty()) {
            JsfUtil.addErrorMessage(
                    "No antibiotic sensitivity records were found."
            );
        } else {
            JsfUtil.addSuccessMessage(
                    reportData.size() + " records found."
            );
        }
    }

    public void clear() {
        institution = null;
        site = null;
        department = null;
        patient = null;
        investigation = null;
        referringDoctor = null;

        visitType = null;
        organism = null;
        antibiotic = null;
        sensitivity = null;

        reportData = new ArrayList<>();

        fromDate = CommonFunctions.getStartOfDay(new Date());
        toDate = CommonFunctions.getEndOfDay(new Date());
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<PatientReportItemValue> getReportData() {
        if (reportData == null) {
            reportData = new ArrayList<>();
        }
        return reportData;
    }

    public void setReportData(
            List<PatientReportItemValue> reportData) {
        this.reportData = reportData;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
        }
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
    }

    public Doctor getReferringDoctor() {
        return referringDoctor;
    }

    public void setReferringDoctor(Doctor referringDoctor) {
        this.referringDoctor = referringDoctor;
    }

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public Item getOrganism() {
        return organism;
    }

    public void setOrganism(Item organism) {
        this.organism = organism;
    }

    public Item getAntibiotic() {
        return antibiotic;
    }

    public void setAntibiotic(Item antibiotic) {
        this.antibiotic = antibiotic;
    }

    public String getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(String sensitivity) {
        this.sensitivity = sensitivity;
    }
}
