/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

/**
 *
 * @author Buddhika
 */
public enum DepartmentType {
    Pharmacy,
    Lab,
    Channelling,
    Store,
    Theatre,
    Kitchen,
    Opd,
    Inventry,
    Counter,
    Inward,
    Other;
    
    public String getLabel(){
        return this.toString();
    }
}
