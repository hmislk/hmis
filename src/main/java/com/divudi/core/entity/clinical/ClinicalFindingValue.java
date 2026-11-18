/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.clinical;

import com.divudi.core.data.clinical.ClinicalFindingValueType;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientReport;
import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 *
 * @author Buddhika
 */
@Entity
public class ClinicalFindingValue implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    Person person;

    @ManyToOne
    private Patient patient;

    private double orderNo;

    @ManyToOne(cascade = CascadeType.REFRESH)
    PatientEncounter encounter;
    @ManyToOne
    ClinicalEntity clinicalFindingItem;

    double doubleValue;
    @Lob
    String lobValue;
    String stringValue;
    long longValue;
    byte[] imageValue;
    private String imageName;
    private String imageType;
    @ManyToOne
    Item itemValue;
    @ManyToOne
    Category categoryValue;
    private boolean retired;
    @ManyToOne
    private Prescription prescription;
    @ManyToOne
    private DocumentTemplate documentTemplate;
    @ManyToOne
    private PatientInvestigation patientInvestigation;
    @ManyToOne
    private PatientReport patientReport;

    @Enumerated(EnumType.STRING)
    private ClinicalFindingValueType clinicalFindingValueType;

    // Form inputs for the allergy entry panels; moved to the stored columns by applyAllergyInputs().
    @Transient
    private String allergyTextInput;
    @Transient
    private String allergyRemarkInput;

    /**
     * Allergy rows: Medicine keeps the medicine in itemValue and the remark in
     * stringValue (unchanged). Food, Plaster and Other keep the free text in
     * stringValue and the remark in lobValue.
     */
    public boolean isMedicineAllergy() {
        return clinicalFindingValueType == null
                || clinicalFindingValueType == ClinicalFindingValueType.PatientAllergy;
    }

    public String getAllergyTypeLabel() {
        if (clinicalFindingValueType == null) {
            return "Medicine";
        }
        switch (clinicalFindingValueType) {
            case PatientFoodAllergy:
                return "Food";
            case PatientPlasterAllergy:
                return "Plaster";
            case PatientOtherAllergy:
                return "Other";
            default:
                return "Medicine";
        }
    }

    public String getAllergyName() {
        if (isMedicineAllergy()) {
            return itemValue != null && itemValue.getName() != null ? itemValue.getName() : "";
        }
        return stringValue != null ? stringValue : "";
    }

    public String getAllergyRemark() {
        String r = isMedicineAllergy() ? stringValue : lobValue;
        // Some older medicine rows keep their remark in lobValue.
        if (isMedicineAllergy() && (r == null || r.trim().isEmpty())) {
            r = lobValue;
        }
        return r != null ? r : "";
    }

    /** For example "Food: Prawns" or "Medicine: Penicillin". */
    public String getAllergyTypeAndName() {
        return getAllergyTypeLabel() + ": " + getAllergyName();
    }

    public String getAllergyDisplay() {
        String r = getAllergyRemark();
        return getAllergyTypeAndName() + (r.isEmpty() ? "" : " - " + r);
    }

    public String getAllergyTextInput() {
        return allergyTextInput;
    }

    public void setAllergyTextInput(String allergyTextInput) {
        this.allergyTextInput = allergyTextInput;
    }

    public String getAllergyRemarkInput() {
        return allergyRemarkInput;
    }

    public void setAllergyRemarkInput(String allergyRemarkInput) {
        this.allergyRemarkInput = allergyRemarkInput;
    }

    /**
     * Checks the entry panel inputs. Returns an error message, or null when valid.
     */
    public String validateAllergyInputs() {
        if (isMedicineAllergy()) {
            if (itemValue == null) {
                return "Please select the allergy drug.";
            }
            return null;
        }
        if (allergyTextInput == null || allergyTextInput.trim().isEmpty()) {
            return "Please enter the " + getAllergyTypeLabel().toLowerCase() + " allergy.";
        }
        return null;
    }

    /**
     * Moves the entry panel inputs to the stored columns. Call after validateAllergyInputs().
     */
    public void applyAllergyInputs() {
        String remark = allergyRemarkInput != null && !allergyRemarkInput.trim().isEmpty()
                ? allergyRemarkInput.trim() : null;
        if (isMedicineAllergy()) {
            if (clinicalFindingValueType == null) {
                clinicalFindingValueType = ClinicalFindingValueType.PatientAllergy;
            }
            stringValue = remark;
        } else {
            itemValue = null;
            stringValue = allergyTextInput.trim();
            lobValue = remark;
        }
    }




    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public PatientEncounter getEncounter() {
        return encounter;
    }

    public void setEncounter(PatientEncounter encounter) {
        this.encounter = encounter;
    }

    public ClinicalEntity getClinicalFindingItem() {
        return clinicalFindingItem;
    }

    public void setClinicalFindingItem(ClinicalEntity clinicalFindingItem) {
        this.clinicalFindingItem = clinicalFindingItem;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public String getLobValue() {
        return lobValue;
    }

    public void setLobValue(String lobValue) {
        this.lobValue = lobValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    public byte[] getImageValue() {
        return imageValue;
    }

    public void setImageValue(byte[] imageValue) {
        this.imageValue = imageValue;
    }

    public Item getItemValue() {
        return itemValue;
    }

    public void setItemValue(Item itemValue) {
        this.itemValue = itemValue;
    }

    public Category getCategoryValue() {
        return categoryValue;
    }

    public void setCategoryValue(Category categoryValue) {
        this.categoryValue = categoryValue;
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

        if (!(object instanceof ClinicalFindingValue)) {
            return false;
        }
        ClinicalFindingValue other = (ClinicalFindingValue) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.clinical.ClinicalFindingValue[ id=" + id + " ]";
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public ClinicalFindingValueType getClinicalFindingValueType() {
        return clinicalFindingValueType;
    }

    public void setClinicalFindingValueType(ClinicalFindingValueType clinicalFindingValueType) {
        this.clinicalFindingValueType = clinicalFindingValueType;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public double getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(double orderNo) {
        this.orderNo = orderNo;
    }

    public Prescription getPrescription() {
        if (this.clinicalFindingValueType != null && this.clinicalFindingValueType == ClinicalFindingValueType.PatientMedicine) {
            if (this.prescription == null) {
                prescription = new Prescription();
            }
        }
        if (this.clinicalFindingValueType != null && this.clinicalFindingValueType == ClinicalFindingValueType.VisitMedicine) {
            if (this.prescription == null) {
                prescription = new Prescription();
            }
        }
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }

    public DocumentTemplate getDocumentTemplate() {
        return documentTemplate;
    }

    public void setDocumentTemplate(DocumentTemplate documentTemplate) {
        this.documentTemplate = documentTemplate;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public void setPatientInvestigation(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
    }

    public PatientReport getPatientReport() {
        return patientReport;
    }

    public void setPatientReport(PatientReport patientReport) {
        this.patientReport = patientReport;
    }



}
