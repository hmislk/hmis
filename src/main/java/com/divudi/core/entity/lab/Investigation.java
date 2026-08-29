/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.lab;

import com.divudi.core.data.InvestigationReportType;
import com.divudi.core.data.SymanticType;
import com.divudi.core.entity.Item;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;

/**
 *
 * @author buddhika
 */
@Entity
public class Investigation extends Item implements Serializable {

    static final long serialVersionUID = 1L;
    //Main Properties
    @ManyToOne
    @Deprecated
    InvestigationCategory investigationCategory;
    @ManyToOne
    InvestigationTube investigationTube;
    @ManyToOne
    Sample sample;
    Double SampleVolume;
    /**
     * If true, bypasses sample collection workflow and allows direct result
     * entry immediately after billing.
     */
    boolean bypassSampleWorkflow;

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

    public boolean isBypassSampleWorkflow() {
        return bypassSampleWorkflow;
    }

    public void setBypassSampleWorkflow(boolean bypassSampleWorkflow) {
        this.bypassSampleWorkflow = bypassSampleWorkflow;
    }

    @Enumerated(EnumType.STRING)
    InvestigationReportType reportType;

    public InvestigationReportType getReportType() {
        if (reportType == null) {
            reportType = InvestigationReportType.General;
        }
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
