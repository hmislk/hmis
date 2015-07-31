/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.data;

/**
 *
 * @author Buddhika
 */
public enum InstitutionType {
    Agency,
    CollectingCentre,
    CreditCompany,
    Bank,
    Lab,
    Hospital,
    Dealer,
    StoreDealor,
    Importer,
    Manufacturer,
    Company,
    branch;
    
    public String getLabel(){
        switch (this){
            case CollectingCentre: return "Collecting Centre";
            case CreditCompany: return "Credit Company";
            case StoreDealor: return "Store Dealor";
        }
        return this.toString();
    }

    
    
    
}
