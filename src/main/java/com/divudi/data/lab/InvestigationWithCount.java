/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.lab;

import com.divudi.entity.lab.Investigation;

/**
 *
 * @author buddhika
 */
public class InvestigationWithCount {
    Investigation investigation;
    Long iiCount;

    public InvestigationWithCount(Investigation investigation, Long iiCount) {
        this.investigation = investigation;
        this.iiCount = iiCount;
    }

    public InvestigationWithCount() {
    }

    
    
    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
    }

    public Long getIiCount() {
        return iiCount;
    }

    public void setIiCount(Long iiCount) {
        this.iiCount = iiCount;
    }
    
}
