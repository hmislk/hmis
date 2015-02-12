/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.lab;

import com.divudi.data.InvestigationReportType;
import com.divudi.data.SymanticType;
import com.divudi.entity.Item;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;

/**
 *
 * @author buddhika
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Investigation extends Item implements Serializable {

    static final long serialVersionUID = 1L;
    //Main Properties
    @ManyToOne
    InvestigationCategory investigationCategory;
    @ManyToOne
    InvestigationTube investigationTube;
    @ManyToOne
    Sample sample;
    Double SampleVolume;
    
    public InvestigationCategory getInvestigationCategory() {
        return investigationCategory;
    }

    public void setInvestigationCategory(InvestigationCategory investigationCategory) {
        this.investigationCategory = investigationCategory;
    }

    public InvestigationTube getInvestigationTube() {
        return investigationTube;
    }

    public void setInvestigationTube(InvestigationTube investigationTube) {
        this.investigationTube = investigationTube;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public Double getSampleVolume() {
        return SampleVolume;
    }

    public void setSampleVolume(Double SampleVolume) {
        this.SampleVolume = SampleVolume;
    }

    @Enumerated(EnumType.STRING)
    InvestigationReportType reportType;

    public InvestigationReportType getReportType() {
        return reportType;
    }

    public void setReportType(InvestigationReportType reportType) {
        this.reportType = reportType;
    }

    @Override
    public SymanticType getSymanticType() {
        return SymanticType.Laboratory_Procedure;
    }
}
