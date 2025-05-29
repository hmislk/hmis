package com.divudi.core.data.dataStructure;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;

import java.util.*;

public class InstitutionBillEncounter {
    private Institution institution;
    private String bhtNo;
    private Date dateOfDischarge;
    private Patient patient;
    private Double netTotal;
    private Double gopAmount;
    private Double paidByPatient;
    private Double patientDue;
    private Double patientExcess;
    private Double paidByCompany;
    private Double companyDue;
    private Double companyExcess;
    private PatientEncounter patientEncounter;
    private Double totalPaidByCompanies;
    private Double totalDue;

    public InstitutionBillEncounter() {
    }

    public static List<InstitutionBillEncounter> createInstitutionBillEncounter(Map<PatientEncounter, List<Bill>> patientEncounterBills, String dueType) {
        if (dueType == null || (!dueType.equalsIgnoreCase("due") && !dueType.equalsIgnoreCase("any")
                && !dueType.equalsIgnoreCase("excess") && !dueType.equalsIgnoreCase("settled"))) {
            return new ArrayList<>();
        }

        List<InstitutionBillEncounter> institutionBillEncounters = new ArrayList<>();

        for (Map.Entry<PatientEncounter, List<Bill>> entry : patientEncounterBills.entrySet()) {
            PatientEncounter patientEncounter = entry.getKey();
            List<Bill> bills = entry.getValue();

            double totalGopOfCompanies = 0.0;
            double totalPaidByCompanies = 0.0;

            for (Bill bill : bills) {
                if (bill.getCreditCompany() != null) {
                    totalGopOfCompanies += bill.getNetTotal();
                    totalPaidByCompanies += bill.getPaidAmount();
                }
            }

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
                    institutionBillEncounter.setPatientExcess(patientEncounter.getFinalBill().getNetTotal() -
                            totalGopOfCompanies - patientEncounter.getFinalBill().getSettledAmountByPatient() < 0 ?
                            patientEncounter.getFinalBill().getNetTotal() - totalGopOfCompanies - patientEncounter.getFinalBill().getSettledAmountByPatient() : 0.0);
                    institutionBillEncounter.setPaidByCompany(bill.getPaidAmount());
                    institutionBillEncounter.setCompanyDue(bill.getNetTotal() - bill.getPaidAmount());
                    institutionBillEncounter.setCompanyExcess(bill.getNetTotal() - bill.getPaidAmount() < 0 ? bill.getNetTotal() - bill.getPaidAmount() : 0.0);
                    institutionBillEncounter.setPatientEncounter(patientEncounter);
                    institutionBillEncounter.setTotalPaidByCompanies(totalPaidByCompanies);
                    institutionBillEncounter.setTotalDue(institutionBillEncounter.getNetTotal() -
                            (institutionBillEncounter.getPaidByPatient() + institutionBillEncounter.getTotalPaidByCompanies()));

                    if (dueType.equalsIgnoreCase("due")) {
                        if (institutionBillEncounter.getPatientDue() > 0 || institutionBillEncounter.getCompanyDue() > 0) {
                            institutionBillEncounters.add(institutionBillEncounter);
                        }
                    } else if (dueType.equalsIgnoreCase("excess")) {
                        if (institutionBillEncounter.getPatientExcess() < 0 || institutionBillEncounter.getCompanyExcess() < 0) {
                            institutionBillEncounters.add(institutionBillEncounter);
                        }
                    } else if (dueType.equalsIgnoreCase("settled")) {
                        if (institutionBillEncounter.getPatientDue() == 0 || institutionBillEncounter.getCompanyDue() == 0) {
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

    public static List<InstitutionBillEncounter> createInstitutionBillEncounter(Map<PatientEncounter, List<Bill>> patientEncounterBills, String dueType,
                                                                                String filterBy) {
        if (dueType == null || (!dueType.equalsIgnoreCase("due") && !dueType.equalsIgnoreCase("any")
                && !dueType.equalsIgnoreCase("excess") && !dueType.equalsIgnoreCase("settled"))) {
            return new ArrayList<>();
        }

        if (filterBy == null || (!filterBy.equalsIgnoreCase("patient") && !filterBy.equalsIgnoreCase("company")
                && !filterBy.equalsIgnoreCase("any") && !filterBy.equalsIgnoreCase("all"))) {
            return new ArrayList<>();
        }

        List<InstitutionBillEncounter> institutionBillEncounters = new ArrayList<>();

        for (Map.Entry<PatientEncounter, List<Bill>> entry : patientEncounterBills.entrySet()) {
            PatientEncounter patientEncounter = entry.getKey();
            List<Bill> bills = entry.getValue();

            double totalGopOfCompanies = 0.0;
            double totalPaidByCompanies = 0.0;

            for (Bill bill : bills) {
                if (bill.getCreditCompany() != null) {
                    totalGopOfCompanies += bill.getNetTotal();
                    totalPaidByCompanies += bill.getPaidAmount();
                }
            }

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
                            totalGopOfCompanies - patientEncounter.getFinalBill().getSettledAmountByPatient() > 0 ?
                            patientEncounter.getFinalBill().getNetTotal() - totalGopOfCompanies - patientEncounter.getFinalBill().getSettledAmountByPatient() : 0.0);
                    institutionBillEncounter.setPatientExcess(patientEncounter.getFinalBill().getNetTotal() -
                            totalGopOfCompanies - patientEncounter.getFinalBill().getSettledAmountByPatient() < 0 ?
                            patientEncounter.getFinalBill().getNetTotal() - totalGopOfCompanies - patientEncounter.getFinalBill().getSettledAmountByPatient() : 0.0);
                    institutionBillEncounter.setPaidByCompany(bill.getPaidAmount());
                    institutionBillEncounter.setCompanyDue(bill.getNetTotal() - bill.getPaidAmount() > 0 ?
                            bill.getNetTotal() - bill.getPaidAmount() : 0.0);
                    institutionBillEncounter.setCompanyExcess(bill.getNetTotal() - bill.getPaidAmount() < 0 ? bill.getNetTotal() - bill.getPaidAmount() : 0.0);
                    institutionBillEncounter.setPatientEncounter(patientEncounter);
                    institutionBillEncounter.setTotalPaidByCompanies(totalPaidByCompanies);
                    institutionBillEncounter.setTotalDue(institutionBillEncounter.getNetTotal() -
                            (institutionBillEncounter.getPaidByPatient() + institutionBillEncounter.getTotalPaidByCompanies()));

                    if (dueType.equalsIgnoreCase("due")) {
                        if (filterBy.equalsIgnoreCase("patient") && institutionBillEncounter.getPatientDue() > 0) {
                            institutionBillEncounters.add(institutionBillEncounter);
                        } else if (filterBy.equalsIgnoreCase("company") && institutionBillEncounter.getCompanyDue() > 0) {
                            institutionBillEncounters.add(institutionBillEncounter);
                        } else if (filterBy.equalsIgnoreCase("any")) {
                            if (institutionBillEncounter.getPatientDue() > 0 || institutionBillEncounter.getCompanyDue() > 0) {
                                institutionBillEncounters.add(institutionBillEncounter);
                            }
                        }
                    } else if (dueType.equalsIgnoreCase("excess")) {
                        if (filterBy.equalsIgnoreCase("patient") && institutionBillEncounter.getPatientExcess() < 0) {
                            institutionBillEncounters.add(institutionBillEncounter);
                        } else if (filterBy.equalsIgnoreCase("company") && institutionBillEncounter.getCompanyExcess() < 0) {
                            institutionBillEncounters.add(institutionBillEncounter);
                        } else if (filterBy.equalsIgnoreCase("any")) {
                            if (institutionBillEncounter.getPatientExcess() < 0 || institutionBillEncounter.getCompanyExcess() < 0) {
                                institutionBillEncounters.add(institutionBillEncounter);
                            }
                        }
                    } else if (dueType.equalsIgnoreCase("settled")) {
                        if (filterBy.equalsIgnoreCase("patient") && institutionBillEncounter.getPatientDue() == 0
                                && institutionBillEncounter.getPatientExcess() == 0) {
                            institutionBillEncounters.add(institutionBillEncounter);
                        } else if (filterBy.equalsIgnoreCase("company") && institutionBillEncounter.getCompanyDue() == 0
                                && institutionBillEncounter.getCompanyExcess() == 0) {
                            institutionBillEncounters.add(institutionBillEncounter);
                        } else if (filterBy.equalsIgnoreCase("any")) {
                            if (institutionBillEncounter.getPatientDue() == 0 || institutionBillEncounter.getCompanyDue() == 0) {
                                institutionBillEncounters.add(institutionBillEncounter);
                            }
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

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Double getTotalPaidByCompanies() {
        return totalPaidByCompanies;
    }

    public void setTotalPaidByCompanies(Double totalPaidByCompanies) {
        this.totalPaidByCompanies = totalPaidByCompanies;
    }

    public Double getTotalDue() {
        return totalDue;
    }

    public void setTotalDue(Double totalDue) {
        this.totalDue = totalDue;
    }

    public Double getCompanyExcess() {
        return companyExcess;
    }

    public void setCompanyExcess(Double companyExcess) {
        this.companyExcess = companyExcess;
    }

    public Double getPatientExcess() {
        return patientExcess;
    }

    public void setPatientExcess(Double patientExcess) {
        this.patientExcess = patientExcess;
    }
}
