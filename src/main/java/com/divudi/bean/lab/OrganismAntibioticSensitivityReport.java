package com.divudi.bean.lab;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
import com.divudi.core.data.InvestigationItemType;
import com.divudi.core.data.Title;
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

    /**
     * Upper bound on the rows one Process run may load, so a wide date range
     * cannot exhaust the heap of this session-scoped bean.
     */
    private static final int MAX_REPORT_ROWS = 10000;

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
    private Item antibiotic;
    private String sensitivity;

    private List<SensitivityRow> reportData;

    public OrganismAntibioticSensitivityReport() {
    }

    public void process() {
        reportData = new ArrayList<>();

        if (fromDate == null || toDate == null) {
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

        // Only the displayed columns are selected. PatientReportItemValue
        // reaches PatientReport -> PatientInvestigation, which maps 56 to-one
        // associations with none declared LAZY, so selecting the entity would
        // drag that whole graph in for every antibiotic result.
        jpql.append("select ")
                .append("patient.phn, ")
                .append("(select max(ps.sampleId) from PatientSampleComponant psc ")
                .append("  join psc.patientSample ps ")
                .append("  where psc.patientInvestigation = pi and psc.retired = false), ")
                .append("inv.name, specimen.name, b.ipOpOrCc, ")
                .append("person.title, person.name, dept.name, ")
                .append("ii.name, priv.strValue, pr.approveComments ")
                .append("from PatientReportItemValue priv ")
                .append("join priv.patientReport pr ")
                .append("join pr.patientInvestigation pi ")
                .append("join pi.billItem bi ")
                .append("join bi.bill b ")
                .append("join priv.investigationItem ii ")
                .append("left join pi.investigation inv ")
                .append("left join inv.sample specimen ")
                .append("left join b.patient patient ")
                .append("left join patient.person person ")
                .append("left join b.department dept ")
                .append("where priv.retired = false ")
                .append("and pr.retired = false ")
                .append("and pi.retired = false ")
                .append("and bi.retired = false ")
                .append("and b.retired = false ")
                .append("and b.createdAt between :fromDate and :toDate ")
                .append("and ii.ixItemType = :itemType ");

        Map<String, Object> parameters = new HashMap<>();

        parameters.put("fromDate", getFromDate());
        parameters.put("toDate", getToDate());
        parameters.put("itemType", InvestigationItemType.Antibiotic);

        if (institution != null) {
            jpql.append("and dept.institution = :institution ");
            parameters.put("institution", institution);
        }

        if (site != null) {
            jpql.append("and dept.site = :site ");
            parameters.put("site", site);
        }

        if (department != null) {
            jpql.append("and dept = :department ");
            parameters.put("department", department);
        }

        if (patient != null
                && patient.getPhn() != null
                && !patient.getPhn().trim().isEmpty()) {

            jpql.append("and upper(patient.phn) like :phn ");

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
            jpql.append("and ii = :antibiotic ");
            parameters.put("antibiotic", antibiotic);
        }

        if (visitType != null
                && !visitType.trim().isEmpty()
                && !"All".equalsIgnoreCase(visitType)) {

            jpql.append("and b.ipOpOrCc = :visitType ");
            parameters.put("visitType", visitType);
        }

        // Sensitivity is held in strValue; PatientReportItemValue.value is
        // @Transient and cannot be queried.
        if (sensitivity != null && !sensitivity.trim().isEmpty()) {
            jpql.append("and priv.strValue = :sensitivity ");
            parameters.put("sensitivity", sensitivity);
        }

        if (referringDoctor != null) {
            jpql.append("and b.referredBy = :referringDoctor ");
            parameters.put("referringDoctor", referringDoctor);
        }

        jpql.append("order by pi.id desc");

        try {
            // One row past the cap separates a truncated result from one that
            // is exactly MAX_REPORT_ROWS long.
            List<Object[]> rows = patientReportItemValueFacade
                    .findObjectsArrayByJpql(
                            jpql.toString(),
                            parameters,
                            TemporalType.TIMESTAMP,
                            MAX_REPORT_ROWS + 1
                    );

            if (rows == null) {
                rows = new ArrayList<>();
            }

            boolean truncated = rows.size() > MAX_REPORT_ROWS;

            if (truncated) {
                rows = rows.subList(0, MAX_REPORT_ROWS);
            }

            for (Object[] columns : rows) {
                reportData.add(createReportRow(columns));
            }

            if (reportData.isEmpty()) {
                JsfUtil.addErrorMessage(
                        "No antibiotic sensitivity records were found."
                );
            } else if (truncated) {
                JsfUtil.addErrorMessage(
                        "Showing the first " + MAX_REPORT_ROWS
                        + " records only. Narrow the date range or filters"
                        + " to see the rest."
                );
            } else {
                JsfUtil.addSuccessMessage(
                        reportData.size() + " records found."
                );
            }

        } catch (Exception e) {
            // Detail stays in the server log; a query fragment in the growl
            // would leak schema information to the user.
            e.printStackTrace();

            reportData = new ArrayList<>();

            JsfUtil.addErrorMessage(
                    "Error generating the report. Please check the server log."
            );
        }
    }

    /**
     * Builds a report row from one projected tuple. The column order must match
     * the select clause in process().
     */
    private SensitivityRow createReportRow(Object[] columns) {

        SensitivityRow row = new SensitivityRow();

        if (columns == null) {
            return row;
        }

        row.setMrn(safeString((String) columns[0]));

        Long sampleId = (Long) columns[1];
        row.setSampleId(sampleId == null ? "" : String.valueOf(sampleId));

        row.setInvestigationName(safeString((String) columns[2]));
        row.setSpecimen(safeString((String) columns[3]));
        row.setVisitType(safeString((String) columns[4]));

        Title title = (Title) columns[5];
        String name = safeString((String) columns[6]);

        row.setPatientName(
                title == null ? name : (title.getLabel() + " " + name).trim()
        );

        row.setPatientLocation(safeString((String) columns[7]));
        row.setAntibiotic(safeString((String) columns[8]));
        row.setSensitivity(safeString((String) columns[9]));
        row.setRemarks(safeString((String) columns[10]));

        return row;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    public void clear() {
        institution = null;
        site = null;
        department = null;
        patient = null;
        investigation = null;
        referringDoctor = null;

        visitType = null;
        antibiotic = null;
        sensitivity = null;

        reportData = new ArrayList<>();

        fromDate = CommonFunctions.getStartOfDay(new Date());
        toDate = CommonFunctions.getEndOfDay(new Date());
    }

    public Date getCurrentDate() {
        return new Date();
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

    public List<SensitivityRow> getReportData() {
        if (reportData == null) {
            reportData = new ArrayList<>();
        }
        return reportData;
    }

    public void setReportData(List<SensitivityRow> reportData) {
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

    /**
     * One antibiotic sensitivity result, flattened for display.
     */
    public static class SensitivityRow implements Serializable {

        private static final long serialVersionUID = 1L;

        private String mrn = "";
        private String sampleId = "";
        private String investigationName = "";
        private String specimen = "";
        private String visitType = "";
        private String patientName = "";
        private String patientLocation = "";
        private String antibiotic = "";
        private String sensitivity = "";
        private String remarks = "";

        public String getMrn() {
            return mrn;
        }

        public void setMrn(String mrn) {
            this.mrn = mrn;
        }

        public String getSampleId() {
            return sampleId;
        }

        public void setSampleId(String sampleId) {
            this.sampleId = sampleId;
        }

        public String getInvestigationName() {
            return investigationName;
        }

        public void setInvestigationName(String investigationName) {
            this.investigationName = investigationName;
        }

        public String getSpecimen() {
            return specimen;
        }

        public void setSpecimen(String specimen) {
            this.specimen = specimen;
        }

        public String getVisitType() {
            return visitType;
        }

        public void setVisitType(String visitType) {
            this.visitType = visitType;
        }

        public String getPatientName() {
            return patientName;
        }

        public void setPatientName(String patientName) {
            this.patientName = patientName;
        }

        public String getPatientLocation() {
            return patientLocation;
        }

        public void setPatientLocation(String patientLocation) {
            this.patientLocation = patientLocation;
        }

        public String getAntibiotic() {
            return antibiotic;
        }

        public void setAntibiotic(String antibiotic) {
            this.antibiotic = antibiotic;
        }

        public String getSensitivity() {
            return sensitivity;
        }

        public void setSensitivity(String sensitivity) {
            this.sensitivity = sensitivity;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
    }
}
