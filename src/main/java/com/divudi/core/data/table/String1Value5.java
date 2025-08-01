/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.table;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author safrin
 */
public class String1Value5 {

    private String string;
    Institution institution;
    private double value1;
    List<PatientEncounter> value1PatientEncounters;
    List<Bill> value1Bills;
    private double value2;
    List<PatientEncounter> value2PatientEncounters;
    List<Bill> value2Bills;
    private double value3;
    List<PatientEncounter> value3PatientEncounters;
    List<Bill> value3Bills;
    private double value4;
    List<PatientEncounter> value4PatientEncounters;
    List<Bill> value4Bills;
    private double value5;

    public List<PatientEncounter> getValue1PatientEncounters() {
        if (value1PatientEncounters == null) {
            value1PatientEncounters = new ArrayList<>();
        }

        return value1PatientEncounters;
    }

    public void setValue1PatientEncounters(List<PatientEncounter> value1PatientEncounters) {

        this.value1PatientEncounters = value1PatientEncounters;
    }

    public List<PatientEncounter> getValue2PatientEncounters() {
        if (value2PatientEncounters == null) {
            value2PatientEncounters = new ArrayList<>();
        }

        return value2PatientEncounters;
    }

    public void setValue2PatientEncounters(List<PatientEncounter> value2PatientEncounters) {

        this.value2PatientEncounters = value2PatientEncounters;
    }

    public List<PatientEncounter> getValue3PatientEncounters() {
        if (value3PatientEncounters == null) {
            value3PatientEncounters = new ArrayList<>();
        }

        return value3PatientEncounters;
    }

    public void setValue3PatientEncounters(List<PatientEncounter> value3PatientEncounters) {

        this.value3PatientEncounters = value3PatientEncounters;
    }

    public List<PatientEncounter> getValue4PatientEncounters() {
        if (value4PatientEncounters == null) {
            value4PatientEncounters = new ArrayList<>();
        }

        return value4PatientEncounters;
    }

    public void setValue4PatientEncounters(List<PatientEncounter> value4PatientEncounters) {

        this.value4PatientEncounters = value4PatientEncounters;
    }

    public List<Bill> getValue1Bills() {
        if (value1Bills == null) {
            value1Bills = new ArrayList<>();
        }

        return value1Bills;
    }

    public void setValue1Bills(List<Bill> value1Bills) {
        this.value1Bills = value1Bills;
    }

    public List<Bill> getValue2Bills() {
        if (value2Bills == null) {
            value2Bills = new ArrayList<>();
        }

        return value2Bills;
    }

    public void setValue2Bills(List<Bill> value2Bills) {
        this.value2Bills = value2Bills;
    }

    public List<Bill> getValue3Bills() {
        if (value3Bills == null) {
            value3Bills = new ArrayList<>();
        }

        return value3Bills;
    }

    public void setValue3Bills(List<Bill> value3Bills) {
        this.value3Bills = value3Bills;
    }

    public List<Bill> getValue4Bills() {
        if (value4Bills == null) {
            value4Bills = new ArrayList<>();
        }

        return value4Bills;
    }

    public void setValue4Bills(List<Bill> value4Bills) {
        this.value4Bills = value4Bills;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public double getValue1() {
        return value1;
    }

    public void setValue1(double value1) {
        this.value1 = value1;
    }

    public double getValue2() {
        return value2;
    }

    public void setValue2(double value2) {
        this.value2 = value2;
    }

    public double getValue3() {
        return value3;
    }

    public void setValue3(double value3) {
        this.value3 = value3;
    }

    public double getValue4() {
        return value4;
    }

    public void setValue4(double value4) {
        this.value4 = value4;
    }

    public double getValue5() {
        return value5;
    }

    public void setValue5(double value5) {
        this.value5 = value5;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<PatientEncounter> getAllPatientEncountersByBills() {
        List<PatientEncounter> allPatientEncounters = new ArrayList<>();
        if (getValue1Bills() != null) {
            allPatientEncounters.addAll(getPatientEncountersFromBills(getValue1Bills()));
        }
        if (getValue2Bills() != null) {
            allPatientEncounters.addAll(getPatientEncountersFromBills(getValue2Bills()));
        }
        if (getValue3Bills() != null) {
            allPatientEncounters.addAll(getPatientEncountersFromBills(getValue3Bills()));
        }
        if (getValue4Bills() != null) {
            allPatientEncounters.addAll(getPatientEncountersFromBills(getValue4Bills()));
        }
        return allPatientEncounters.stream().distinct().collect(Collectors.toList());
    }

    private List<PatientEncounter> getPatientEncountersFromBills(List<Bill> bills) {
        return bills.stream()
                .map(Bill::getPatientEncounter)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}
