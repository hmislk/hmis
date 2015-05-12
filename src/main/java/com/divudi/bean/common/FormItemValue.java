/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.entity.Category;
import com.divudi.entity.Item;
import com.divudi.entity.lab.*;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Person;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

/**
 *
 * @author Buddhika
 *
 *
 * Table creation sql for MySQL
 *
 *
 *
 *
 */
@Entity
public class FormItemValue implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    Patient patient;
    @ManyToOne
    Person person;
    @ManyToOne
    PatientEncounter patientEncounter;
    @ManyToOne
    ReportItem reportItem;
    @ManyToOne
    PatientReport patientReport;
    String strValue;
    @Lob
    private String lobValue;
    @Lob
    byte[] baImage;
    String fileName;
    String fileType;
    Double doubleValue;
    Long longValue;
    @ManyToOne
    Item item;
    @ManyToOne
    Category category;
    @ManyToOne
    Person referringPerson;

    public FormItemValue() {
    }

    public String getStrValue() {
        if (strValue != null) {
            strValue = strValue.trim();
        }
        return strValue;
    }

    public void setStrValue(String strValue) {
        this.strValue = strValue;
    }

    public String getLobValue() {
        return lobValue;
    }

    public void setLobValue(String lobValue) {
        this.lobValue = lobValue;
    }

    public byte[] getBaImage() {
        return baImage;
    }

    public void setBaImage(byte[] baImage) {
        this.baImage = baImage;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Patient getPatient() {
        ////System.out.println("");
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public ReportItem getReportItem() {
        return reportItem;
    }

    public void setReportItem(ReportItem reportItem) {
        this.reportItem = reportItem;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Person getReferringPerson() {
        return referringPerson;
    }

    public void setReferringPerson(Person referringPerson) {
        this.referringPerson = referringPerson;
    }

    public PatientReport getPatientReport() {
        return patientReport;
    }

    public void setPatientReport(PatientReport patientReport) {
        this.patientReport = patientReport;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (!(object instanceof FormItemValue)) {
            return false;
        }
        FormItemValue other = (FormItemValue) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        String s = "";
        if (this.doubleValue != null) {
            s = s + this.doubleValue;
        }
        if (this.lobValue != null) {
            s = s + this.lobValue;
        }
        if (this.longValue != null) {
            s = s + this.longValue;
        }
        if (this.strValue != null) {
            s = s + this.strValue;
        }
        return s;
    }
}
