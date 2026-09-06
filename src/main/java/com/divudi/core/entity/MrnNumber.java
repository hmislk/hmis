/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Internal counter entity used by MrnGenerator to track the last-issued
 * Medical Record Number (Patient.code) serial. Mirrors the BillNumber
 * counter-entity pattern.
 *
 * A null {@code year} represents the global, non-year-scoped counter used by
 * the SERIAL and INSTITUTION_SERIAL strategies. A non-null {@code year}
 * represents the yearly counter used by the YEAR_SERIAL and
 * YEAR_INSTITUTION_SERIAL strategies.
 */
@Entity
public class MrnNumber implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer year;

    private Long lastMrnNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Long getLastMrnNumber() {
        return lastMrnNumber;
    }

    public void setLastMrnNumber(Long lastMrnNumber) {
        this.lastMrnNumber = lastMrnNumber;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MrnNumber)) {
            return false;
        }
        MrnNumber other = (MrnNumber) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.MrnNumber[ id=" + id + " ]";
    }

}
