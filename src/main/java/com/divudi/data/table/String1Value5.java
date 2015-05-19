/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data.table;

import com.divudi.entity.Institution;
import com.divudi.entity.PatientEncounter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author safrin
 */
public class String1Value5 {

    private String string;
    Institution institution;
    private double value1;
    List<PatientEncounter> value1PatientEncounters;
    private double value2;
    List<PatientEncounter> value2PatientEncounters;
    private double value3;
    List<PatientEncounter> value3PatientEncounters;
    private double value4;
    List<PatientEncounter> value4PatientEncounters;
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

}
