/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.data.inward;

/**
 *
 * @author safrin
 */
public enum AdmissionTypeEnum {
    Admission("Inward Stay"),
    DayCase("Day Case");
    
    private final String label;

    public String getLabel() {
        return label;
    }

    AdmissionTypeEnum(String label) {
        this.label = label;
    }
    
    
}
