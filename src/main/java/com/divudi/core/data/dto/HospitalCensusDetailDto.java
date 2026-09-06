package com.divudi.core.data.dto;

import com.divudi.core.data.Sex;
import java.io.Serializable;
import java.util.Date;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class HospitalCensusDetailDto implements Serializable {

    private String area;
    private String mrn;
    private String name;
    private String ageSex;
    private Date doa;
    private String bedNo;
    private String status;
    private String consultant;
    private double deposited;
    private double balance;

    /**
     * Default no-arg constructor (required by JSF/CDI).
     */
    public HospitalCensusDetailDto() {
    }

    /**
     * JPQL projection constructor — used by the batch detail query in
     * InwardManagementReportController to avoid full entity hydration.
     *
     * @param area       Ward / department name
     * @param mrn        Patient hospital number (phn)
     * @param personName Patient display name
     * @param dob        Date of birth (used to compute age string)
     * @param sex        Sex enum (may be null)
     * @param doa        Date of admission
     * @param bedNo      Bed / room name
     * @param discharged Whether the encounter is marked discharged
     * @param consultant Referring doctor name (may be null)
     */
    public HospitalCensusDetailDto(
            String area,
            String mrn,
            String personName,
            Date dob,
            Sex sex,
            Date doa,
            String bedNo,
            Boolean discharged,
            String consultant) {
        this.area = area;
        this.mrn = mrn;
        this.name = personName;
        this.doa = doa;
        this.bedNo = bedNo;
        this.status = Boolean.TRUE.equals(discharged) ? "Discharged" : "Admitted";
        this.consultant = consultant != null ? consultant : "";
        this.ageSex = buildAgeSex(dob, sex);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static String buildAgeSex(Date dob, Sex sex) {
        String ageStr = "";
        if (dob != null) {
            try {
                Period p = new Period(
                        new LocalDate(dob),
                        LocalDate.now(),
                        PeriodType.yearMonthDay());
                ageStr = p.getYears() + "Y " + p.getMonths() + "M " + p.getDays() + "D";
            } catch (Exception ignored) {
                // keep empty on any date arithmetic failure
            }
        }
        String sexStr = sex != null ? sex.toString() : "";
        return ageStr + " / " + sexStr;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAgeSex() {
        return ageSex;
    }

    public void setAgeSex(String ageSex) {
        this.ageSex = ageSex;
    }

    public Date getDoa() {
        return doa;
    }

    public void setDoa(Date doa) {
        this.doa = doa;
    }

    public String getBedNo() {
        return bedNo;
    }

    public void setBedNo(String bedNo) {
        this.bedNo = bedNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConsultant() {
        return consultant;
    }

    public void setConsultant(String consultant) {
        this.consultant = consultant;
    }

    public double getDeposited() {
        return deposited;
    }

    public void setDeposited(double deposited) {
        this.deposited = deposited;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

}
