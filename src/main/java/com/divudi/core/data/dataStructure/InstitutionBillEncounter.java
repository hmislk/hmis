package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class InstitutionBillEncounter {
    private Institution institution;
    private String bhtNo;
    private Date dateOfDischarge;
    private Patient patient;
    private Double netTotal;
    private Double gopAmount;
    private Double paidByPatient;
    private Double patientDue;
    private Double paidByCompany;
    private Double companyDue;

    public InstitutionBillEncounter() {
    }

    public static List<InstitutionBillEncounter> createInstitutionBillEncounter(Map<PatientEncounter, List<Bill>> patientEncounterBills, boolean onlyDue) {
        List<InstitutionBillEncounter> institutionBillEncounters = new ArrayList<>();

        for (Map.Entry<PatientEncounter, List<Bill>> entry : patientEncounterBills.entrySet()) {
            PatientEncounter patientEncounter = entry.getKey();
            List<Bill> bills = entry.getValue();

            double totalGopOfCompanies = bills.stream()
                    .filter(bill -> bill.getCreditCompany() != null)
                    .mapToDouble(Bill::getNetTotal)
                    .sum();

            for (Bill bill : bills) {
                if (bill.getCreditCompany() != null) {
                    InstitutionBillEncounter institutionBillEncounter = new InstitutionBillEncounter();

                    institutionBillEncounter.setInstitution(bill.getCreditCompany());
                    institutionBillEncounter.setBhtNo(patientEncounter.getBhtNo());
                    institutionBillEncounter.setDateOfDischarge(patientEncounter.getDateOfDischarge());
                    institutionBillEncounter.setPatient(patientEncounter.getPatient());
                    institutionBillEncounter.setNetTotal(patientEncounter.getFinalBill().getNetTotal());
                    institutionBillEncounter.setGopAmount(bill.getNetTotal());
                    institutionBillEncounter.setPaidByPatient(patientEncounter.getFinalBill().getSettledAmountByPatient());
                    institutionBillEncounter.setPatientDue(patientEncounter.getFinalBill().getNetTotal() -
                            totalGopOfCompanies - patientEncounter.getFinalBill().getSettledAmountByPatient());
                    institutionBillEncounter.setPaidByCompany(bill.getPaidAmount());
                    institutionBillEncounter.setCompanyDue(bill.getNetTotal() - bill.getPaidAmount());

                    if (onlyDue) {
                        if (institutionBillEncounter.getPatientDue() > 0 || institutionBillEncounter.getCompanyDue() > 0) {
                            institutionBillEncounters.add(institutionBillEncounter);
                        }
                    } else {
                        institutionBillEncounters.add(institutionBillEncounter);
                    }
                }
            }
        }

        return institutionBillEncounters;
    }

    public static Map<Institution, List<InstitutionBillEncounter>> createInstitutionBillEncounterMap(List<InstitutionBillEncounter> institutionBillEncounters) {
        return institutionBillEncounters.stream()
                .collect(java.util.stream.Collectors.groupingBy(InstitutionBillEncounter::getInstitution));
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public Date getDateOfDischarge() {
        return dateOfDischarge;
    }

    public void setDateOfDischarge(Date dateOfDischarge) {
        this.dateOfDischarge = dateOfDischarge;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Double getGopAmount() {
        return gopAmount;
    }

    public void setGopAmount(Double gopAmount) {
        this.gopAmount = gopAmount;
    }

    public Double getPaidByPatient() {
        return paidByPatient;
    }

    public void setPaidByPatient(Double paidByPatient) {
        this.paidByPatient = paidByPatient;
    }

    public Double getPatientDue() {
        return patientDue;
    }

    public void setPatientDue(Double patientDue) {
        this.patientDue = patientDue;
    }

    public Double getPaidByCompany() {
        return paidByCompany;
    }

    public void setPaidByCompany(Double paidByCompany) {
        this.paidByCompany = paidByCompany;
    }

    public Double getCompanyDue() {
        return companyDue;
    }

    public void setCompanyDue(Double companyDue) {
        this.companyDue = companyDue;
    }
}
