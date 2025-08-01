/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.data.dataStructure;

import com.divudi.core.data.hr.ExtraDutyType;

/**
 *
 * @author safrin
 */
public class ExtraDutyCount {
    private ExtraDutyType extraDutyType;
    private double count;

    public ExtraDutyType getExtraDutyType() {
        return extraDutyType;
    }

    public void setExtraDutyType(ExtraDutyType extraDutyType) {
        this.extraDutyType = extraDutyType;
    }

    public double getCount() {
        return count;
    }

    public void setCount(double count) {
        this.count = count;
    }
}
