/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.data.lab;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author buddhika
 */
public class JsonInvestigationList {
    private List<JsonInvestigation> investigations;

    public List<JsonInvestigation> getInvestigations() {
        if(investigations==null){
            investigations = new ArrayList<>();
        }
        return investigations;
    }

    public void setInvestigations(List<JsonInvestigation> investigations) {
        this.investigations = investigations;
    }
    
    
}
