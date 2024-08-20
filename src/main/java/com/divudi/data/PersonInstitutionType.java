/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 * @author buddhika
 */
public enum PersonInstitutionType {

    Channelling,
    Opd,
    Inward;

    public String getLabel() {
        return this.toString();
    }
}
