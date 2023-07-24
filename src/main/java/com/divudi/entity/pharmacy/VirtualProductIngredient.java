/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.pharmacy;

import com.divudi.entity.Category;
import com.divudi.entity.Item;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Represents an active ingredient within a Virtual Medicinal Product (VMP) in
 * the NHS UK dm+d (Dictionary of Medicines and Devices). This class stores and
 * manages information about the active ingredient and its strength or
 * concentration within the associated VMP, facilitating various healthcare
 * applications like customized prescribing, drug-allergy checking, and
 * pharmacovigilance activities.
 *
 * @author Dr M H B Ariyaratne
 * @version 1.0
 * @since 2023-04-05
 */
@Entity
public class VirtualProductIngredient implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    Vtm vtm;
    @ManyToOne
    Vmp vmp;
    @ManyToOne
    MeasurementUnit strengthUnit;
    double strength;
    //Retairing properties
    private boolean retired;
    @ManyToOne
    Category pharmaceuticalItemCategory;
    /**
     * A unique identifier for the ingredient substance. This identifier is used
     * to identify the ingredient substance within an Ingredient Substance file
     * and is taken from the set of SNOMED core terms. In cases where a new
     * ingredient term is created with no corresponding entry in the SNOMED CT
     * core terms, an entry from the UK extension to the SNOMED CT codes will be
     * used and may later be changed to a core term identifier.
     *
     * Note: 1. The identifier will not be deleted, although it may be marked as
     * no longer valid. 2. This identifier shall be used to identify a substance
     * that acts as an ingredient of a Virtual Medicinal Product, such as
     * quinine sulphate, amitriptyline hydrochloride, or amoxicillin sodium. 3.
     * Ingredients may be fully specified including salts, or may be more
     * loosely described without the salt if that is appropriate or if the only
     * information available.
     */
    @ManyToOne
    Item ingredientSubstance;
    /**
     * A unique identifier for the basis of strength substance. This identifier
     * is used to identify an ingredient substance within an Ingredient
     * Substance file that is different from the ingredient substance identified
     * above and serves as the substance upon which the pharmaceutical strength
     * is based.
     *
     * Example: Amoxicillin 250mg injection vials contain amoxicillin sodium but
     * the strength is expressed as the quantity of amoxicillin. Amoxicillin is
     * the "base" or basis of strength substance (BoSS) and therefore the
     * identifier for "amoxicillin" (substance, not VTM) would be put in this
     * field.
     *
     * Note: 1. The identifier will not be deleted, although it may be marked as
     * no longer valid. 2. This attribute is populated only if the basis of
     * pharmaceutical strength value is set to "2".
     */
    @ManyToOne
    Item basisOfStrengthSubstance;
    /**
     * Indicates whether the pharmaceutical strength (next attribute) is based
     * upon the ingredient substance or the "basis of strength substance".
     *
     * Values: 1 = Based on ingredient substance. The strength value is based on
     * the ingredient substance identified in the attribute ingredient substance
     * identifier. 2 = Based on base substance. The strength value is based on
     * the ingredient substance identified in the attribute basis of strength
     * substance identifier.
     *
     * Note: This attribute is mandatory when a value is present in the
     * attribute "pharmaceutical strength".
     */
    int basisOfPharmaceuticalStrength;

    public Category getPharmaceuticalItemCategory() {
        return pharmaceuticalItemCategory;
    }

    public void setPharmaceuticalItemCategory(Category pharmaceuticalItemCategory) {
        this.pharmaceuticalItemCategory = pharmaceuticalItemCategory;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public Vtm getVtm() {
        return vtm;
    }

    public void setVtm(Vtm vtm) {
        this.vtm = vtm;
    }

    public Vmp getVmp() {
        return vmp;
    }

    public void setVmp(Vmp vmp) {
        this.vmp = vmp;
    }

    public MeasurementUnit getStrengthUnit() {
        return strengthUnit;
    }

    public void setStrengthUnit(MeasurementUnit strengthUnit) {
        this.strengthUnit = strengthUnit;
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = strength;
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

        if (!(object instanceof VirtualProductIngredient)) {
            return false;
        }
        VirtualProductIngredient other = (VirtualProductIngredient) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.pharmacy.VtmsVmps[ id=" + id + " ]";
    }
}
