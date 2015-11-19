/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

/**
 *
 * @author buddhika
 */
public enum PersonInstitutionType {
    Channelling,
    Opd,
    Inward;
    
    public String getLabel(){
        return this.toString();
    }
}
